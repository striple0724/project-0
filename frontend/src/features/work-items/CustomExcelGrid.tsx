import React, { useState, useRef, useEffect, KeyboardEvent, ClipboardEvent } from "react";
import { createPortal } from "react-dom";
import { useVirtualizer } from "@tanstack/react-virtual";
import { CalendarDays } from "lucide-react";
import type { WorkItem } from "./types";

interface ColumnDef {
  field: keyof WorkItem | "select" | "history" | "delete";
  headerName: string;
  width: number;
  editable?: boolean;
  type?: "text" | "select" | "date" | "checkbox" | "action";
  options?: string[];
}

interface CustomExcelGridProps {
  data: WorkItem[];
  columns: ColumnDef[];
  selectedIds: number[];
  deletedIds?: number[];
  onSelectionChange: (ids: number[]) => void;
  onCellValueChanged: (rowId: number, field: keyof WorkItem, newValue: any) => void;
  onHistoryClick?: (row: WorkItem) => void;
  onDeleteClick?: (row: WorkItem) => void;
  dirtyMap?: Record<number, Partial<WorkItem>>;
  fetchNextPage?: () => void;
  hasNextPage?: boolean;
  isFetchingNextPage?: boolean;
  copyWithHeader?: boolean;
}

export function CustomExcelGrid({
  data,
  columns,
  selectedIds,
  deletedIds = [],
  onSelectionChange,
  onCellValueChanged,
  onHistoryClick,
  onDeleteClick,
  dirtyMap = {},
  fetchNextPage,
  hasNextPage,
  isFetchingNextPage,
  copyWithHeader = false,
}: CustomExcelGridProps) {
  const toDisplayDateTime = (value: string): string => {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, "0");
    const dd = String(date.getDate()).padStart(2, "0");
    const hh = String(date.getHours()).padStart(2, "0");
    const mi = String(date.getMinutes()).padStart(2, "0");
    const ss = String(date.getSeconds()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
  };

  const normalizeDateInput = (value: string): string => {
    const digits = value.replace(/\D/g, "").slice(0, 8);
    if (digits.length <= 4) return digits;
    if (digits.length <= 6) return `${digits.slice(0, 4)}-${digits.slice(4)}`;
    return `${digits.slice(0, 4)}-${digits.slice(4, 6)}-${digits.slice(6)}`;
  };

  const parentRef = useRef<HTMLDivElement>(null);
  const [viewportWidth, setViewportWidth] = useState(0);

  const rowVirtualizer = useVirtualizer({
    count: data.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 40,
    overscan: 5,
  });

  const [focusedCell, setFocusedCell] = useState<[number, number] | null>(null);
  const [editingCell, setEditingCell] = useState<[number, number] | null>(null);
  const [selectionRange, setSelectionRange] = useState<{ start: [number, number]; end: [number, number] } | null>(null);
  const [selectedCells, setSelectedCells] = useState<Set<string>>(new Set());
  const [selectedColumns, setSelectedColumns] = useState<Set<number>>(new Set());
  const [isDragging, setIsDragging] = useState(false);
  const [editValue, setEditValue] = useState<string>("");
  const [openDatePickerOnEdit, setOpenDatePickerOnEdit] = useState(false);
  const [memoEditorRect, setMemoEditorRect] = useState<{ top: number; left: number; width: number; height: number } | null>(null);
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    row: number;
    col: number;
    anchorRect: { top: number; left: number; width: number; height: number };
  } | null>(null);

  const inputRef = useRef<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(null);
  const nativeDatePickerRef = useRef<HTMLInputElement>(null);
  const pointerRef = useRef<{ x: number; y: number } | null>(null);
  const dragModeRef = useRef<"none" | "shift" | "normal">("none");
  const dragAnchorRef = useRef<[number, number] | null>(null);
  const focusedCellRef = useRef<[number, number] | null>(null);
  const keyboardAnchorRef = useRef<[number, number] | null>(null);

  useEffect(() => {
    if (editingCell && inputRef.current) {
      inputRef.current.focus();
    }
  }, [editingCell]);

  useEffect(() => {
    focusedCellRef.current = focusedCell;
  }, [focusedCell]);

  useEffect(() => {
    const container = parentRef.current;
    if (!container) return;

    const resize = () => setViewportWidth(container.clientWidth);
    resize();

    const observer = new ResizeObserver(() => resize());
    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!editingCell || !openDatePickerOnEdit) return;
    const [, c] = editingCell;
    const col = columns[c];
    if (col.type !== "date") {
      setOpenDatePickerOnEdit(false);
      return;
    }
    const picker = nativeDatePickerRef.current;
    if (!picker) return;
    try {
      picker.showPicker?.();
    } catch {
      picker.focus();
    } finally {
      setOpenDatePickerOnEdit(false);
    }
  }, [editingCell, openDatePickerOnEdit, columns]);

  const resolveSelectionSpec = () => {
    let minR = 0;
    let maxR = 0;
    let minC = 0;
    let maxC = 0;
    let useCellMask = false;
    let cellMask = new Set<string>();

    if (selectionRange) {
      minR = Math.min(selectionRange.start[0], selectionRange.end[0]);
      maxR = Math.max(selectionRange.start[0], selectionRange.end[0]);
      minC = Math.min(selectionRange.start[1], selectionRange.end[1]);
      maxC = Math.max(selectionRange.start[1], selectionRange.end[1]);
    } else if (selectedColumns.size > 0) {
      const cols = Array.from(selectedColumns).sort((a, b) => a - b);
      minR = 0;
      maxR = Math.max(data.length - 1, 0);
      minC = cols[0];
      maxC = cols[cols.length - 1];
    } else if (selectedCells.size > 0) {
      const coords = Array.from(selectedCells).map((key) => key.split(":").map(Number) as [number, number]);
      minR = Math.min(...coords.map((v) => v[0]));
      maxR = Math.max(...coords.map((v) => v[0]));
      minC = Math.min(...coords.map((v) => v[1]));
      maxC = Math.max(...coords.map((v) => v[1]));
      useCellMask = true;
      cellMask = selectedCells;
    } else if (focusedCell) {
      minR = focusedCell[0];
      maxR = focusedCell[0];
      minC = focusedCell[1];
      maxC = focusedCell[1];
    } else {
      return null;
    }

    return { minR, maxR, minC, maxC, useCellMask, cellMask };
  };

  const buildCopyText = (includeHeader: boolean) => {
    const spec = resolveSelectionSpec();
    if (!spec) return "";
    const { minR, maxR, minC, maxC, useCellMask, cellMask } = spec;

    const lines: string[] = [];
    if (includeHeader) {
      const headerValues: string[] = [];
      for (let c = minC; c <= maxC; c++) {
        if (c < 0 || c >= columns.length) continue;
        const col = columns[c];
        headerValues.push(col.field === "select" || col.field === "history" || col.field === "delete" ? "" : col.headerName);
      }
      lines.push(headerValues.join("\t"));
    }

    for (let r = minR; r <= maxR; r++) {
      if (r < 0 || r >= data.length) continue;
      const row = data[r];
      const values: string[] = [];
      for (let c = minC; c <= maxC; c++) {
        if (c < 0 || c >= columns.length) continue;
        const col = columns[c];
        if (col.field === "select" || col.field === "history" || col.field === "delete") {
          values.push("");
          continue;
        }
        if (useCellMask && !cellMask.has(`${r}:${c}`)) {
          values.push("");
          continue;
        }
        values.push(String(row[col.field as keyof WorkItem] ?? ""));
      }
      lines.push(values.join("\t"));
    }

    return lines.join("\n");
  };

  const moveFocus = (rowDelta: number, colDelta: number, shiftKey = false) => {
    const base = focusedCellRef.current ?? [0, 0];
    const nextRow = Math.max(0, Math.min(data.length - 1, base[0] + rowDelta));
    let nextCol = base[1] + colDelta;
    if (nextCol < 0) nextCol = 0;
    if (nextCol > columns.length - 1) nextCol = columns.length - 1;

    const nextCell: [number, number] = [nextRow, nextCol];
    if (shiftKey) {
      const anchor = keyboardAnchorRef.current ?? selectionRange?.start ?? base;
      keyboardAnchorRef.current = anchor;
      setSelectionRange({ start: anchor, end: nextCell });
    } else {
      keyboardAnchorRef.current = null;
      setSelectionRange({ start: nextCell, end: nextCell });
      setSelectedCells(new Set());
      setSelectedColumns(new Set());
    }
    setFocusedCell(nextCell);
    scrollCellIntoView(nextRow, nextCol);
  };

  const startEditing = (
    rowIdx: number,
    colIdx: number,
    options?: {
      initialValue?: string;
      openDatePicker?: boolean;
      anchorEl?: HTMLElement | null;
      anchorRect?: { top: number; left: number; width: number; height: number };
    }
  ) => {
    const col = columns[colIdx];
    if (!col.editable || col.field === "history" || col.field === "delete") return;

    const row = data[rowIdx];
    const val = String(row[col.field as keyof WorkItem] ?? "");
    setEditValue(options?.initialValue ?? val);
    setEditingCell([rowIdx, colIdx]);
    setOpenDatePickerOnEdit(Boolean(options?.openDatePicker && col.type === "date"));

    if (col.field === "memo") {
      const rect = options?.anchorRect ?? options?.anchorEl?.getBoundingClientRect();
      const topBase = rect?.top ?? 120;
      const leftBase = rect?.left ?? 120;
      const top = Math.max(16, Math.min(topBase, window.innerHeight - 240));
      const left = Math.max(16, Math.min(leftBase, window.innerWidth - 520));
      const width = Math.max(360, Math.min(rect?.width ?? 520, window.innerWidth - left - 16));
      setMemoEditorRect({ top, left, width, height: 200 });
    } else {
      setMemoEditorRect(null);
    }
  };

  const finishEditing = (nextValue?: string) => {
    if (!editingCell) return;
    const [r, c] = editingCell;
    const col = columns[c];
    const row = data[r];
    const valueToSave = nextValue ?? editValue;
    if (col.field !== "select" && col.field !== "history" && col.field !== "delete" && col.editable) {
      onCellValueChanged(row.id, col.field as keyof WorkItem, valueToSave);
    }
    setEditingCell(null);
    setOpenDatePickerOnEdit(false);
    setMemoEditorRect(null);
    if (parentRef.current) parentRef.current.focus();
  };

  const clearSelectedCells = () => {
    const spec = resolveSelectionSpec();
    if (!spec) return;
    const { minR, maxR, minC, maxC, useCellMask, cellMask } = spec;

    for (let r = minR; r <= maxR; r++) {
      if (r < 0 || r >= data.length) continue;
      if (deletedIds.includes(data[r].id)) continue;
      for (let c = minC; c <= maxC; c++) {
        if (c < 0 || c >= columns.length) continue;
        if (useCellMask && !cellMask.has(`${r}:${c}`)) continue;
        const col = columns[c];
        if (col.editable && col.field !== "select" && col.field !== "history" && col.field !== "delete") {
          onCellValueChanged(data[r].id, col.field as keyof WorkItem, "");
        }
      }
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const isArrowKey =
      e.key === "ArrowDown" || e.key === "ArrowUp" || e.key === "ArrowRight" || e.key === "ArrowLeft";
    if (!focusedCell) {
      if (isArrowKey || e.key === "Tab" || e.key === "Enter") {
        e.preventDefault();
        setFocusedCell([0, 0]);
        setSelectionRange({ start: [0, 0], end: [0, 0] });
        setSelectedCells(new Set());
        setSelectedColumns(new Set());
        scrollCellIntoView(0, 0);
      }
      return;
    }
    const focusedRowId = data[focusedCell[0]]?.id;
    const focusedRowDeleted = focusedRowId != null && deletedIds.includes(focusedRowId);

    if (editingCell) {
      if (e.key === "Enter") {
        e.preventDefault();
        finishEditing();
        moveFocus(1, 0);
      } else if (e.key === "Escape") {
        setEditingCell(null);
        if (parentRef.current) parentRef.current.focus();
      } else if (e.key === "Tab") {
        e.preventDefault();
        finishEditing();
        moveFocus(0, e.shiftKey ? -1 : 1);
      }
      return;
    }

    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "c") {
      e.preventDefault();
      const text = buildCopyText(e.shiftKey ? true : copyWithHeader);
      if (!text) return;
      void navigator.clipboard?.writeText(text).catch(() => undefined);
      return;
    }

    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "x") {
      e.preventDefault();
      const text = buildCopyText(e.shiftKey ? true : copyWithHeader);
      if (!text) return;
      void navigator.clipboard?.writeText(text).catch(() => undefined);
      clearSelectedCells();
      return;
    }

    if (focusedRowDeleted) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      moveFocus(1, 0, e.shiftKey);
    }
    else if (e.key === "ArrowUp") {
      e.preventDefault();
      moveFocus(-1, 0, e.shiftKey);
    }
    else if (e.key === "ArrowRight") {
      e.preventDefault();
      moveFocus(0, 1, e.shiftKey);
    }
    else if (e.key === "ArrowLeft") {
      e.preventDefault();
      moveFocus(0, -1, e.shiftKey);
    }
    else if (e.key === "F2") {
      e.preventDefault();
      startEditing(focusedCell[0], focusedCell[1]);
    }
    else if (e.key === "Tab") {
      e.preventDefault();
      moveFocus(0, e.shiftKey ? -1 : 1);
    } else if (e.key === "Enter") {
      e.preventDefault();
      startEditing(focusedCell[0], focusedCell[1]);
    } else if (e.key === "Delete" || e.key === "Backspace") {
      clearSelectedCells();
    } else if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
      startEditing(focusedCell[0], focusedCell[1], { initialValue: e.key });
      e.preventDefault();
    }
  };

  const handleMouseDown = (event: React.MouseEvent<HTMLDivElement>, rIdx: number, cIdx: number) => {
    if (event.button !== 0) return;
    event.preventDefault();
    pointerRef.current = { x: event.clientX, y: event.clientY };

    if (editingCell) {
      const [er, ec] = editingCell;
      if (er === rIdx && ec === cIdx) {
        return;
      }
      finishEditing();
    }
    if (event.shiftKey) {
      dragModeRef.current = "shift";
      const anchor: [number, number] = focusedCellRef.current ?? [rIdx, cIdx];
      dragAnchorRef.current = anchor;
      setFocusedCell([rIdx, cIdx]);
      setSelectionRange({ start: anchor, end: [rIdx, cIdx] });
      setSelectedCells(new Set());
      setSelectedColumns(new Set());
      setIsDragging(true);
      return;
    }

    if (event.ctrlKey || event.metaKey) {
      dragModeRef.current = "none";
      dragAnchorRef.current = null;
      keyboardAnchorRef.current = null;
      setFocusedCell([rIdx, cIdx]);
      const key = `${rIdx}:${cIdx}`;
      setSelectedCells((prev) => {
        const next = new Set(prev);
        if (next.has(key)) next.delete(key);
        else next.add(key);
        return next;
      });
      setSelectedColumns(new Set());
      setSelectionRange(null);
      setIsDragging(false);
      return;
    }

    dragModeRef.current = "normal";
    dragAnchorRef.current = [rIdx, cIdx];
    keyboardAnchorRef.current = null;
    setFocusedCell([rIdx, cIdx]);
    setSelectedCells(new Set());
    setSelectedColumns(new Set());
    setSelectionRange({ start: [rIdx, cIdx], end: [rIdx, cIdx] });
    setIsDragging(true);
  };

  const handleMouseEnter = (rIdx: number, cIdx: number) => {
    if (isDragging) {
      setSelectionRange((prev) => {
        const anchor = dragAnchorRef.current ?? prev?.start ?? focusedCellRef.current ?? [rIdx, cIdx];
        return { start: anchor, end: [rIdx, cIdx] };
      });
      setFocusedCell([rIdx, cIdx]);
    }
  };

  const handleCellContextMenu = (event: React.MouseEvent<HTMLDivElement>, rIdx: number, cIdx: number) => {
    event.preventDefault();
    const hasExistingSelection =
      selectedColumns.size > 0 ||
      selectedCells.size > 0 ||
      (selectionRange != null &&
        (selectionRange.start[0] !== selectionRange.end[0] || selectionRange.start[1] !== selectionRange.end[1]));

    if (!hasExistingSelection) {
      setFocusedCell([rIdx, cIdx]);
      setSelectedCells(new Set());
      setSelectedColumns(new Set());
      setSelectionRange({ start: [rIdx, cIdx], end: [rIdx, cIdx] });
    }
    const rect = event.currentTarget.getBoundingClientRect();
    setContextMenu({
      x: event.clientX,
      y: event.clientY,
      row: rIdx,
      col: cIdx,
      anchorRect: { top: rect.top, left: rect.left, width: rect.width, height: rect.height },
    });
  };

  useEffect(() => {
    const handleGlobalMouseUp = () => {
      setIsDragging(false);
      pointerRef.current = null;
      dragModeRef.current = "none";
      dragAnchorRef.current = null;
    };
    window.addEventListener("mouseup", handleGlobalMouseUp);
    return () => window.removeEventListener("mouseup", handleGlobalMouseUp);
  }, []);

  const resolvePointerCell = (x: number, y: number): [number, number] | null => {
    const target = document.elementFromPoint(x, y);
    const cell = target instanceof HTMLElement ? (target.closest("[data-grid-cell='1']") as HTMLElement | null) : null;
    if (cell) {
      const row = Number(cell.dataset.row);
      const col = Number(cell.dataset.col);
      if (!Number.isNaN(row) && !Number.isNaN(col)) {
        return [row, col];
      }
    }

    const element = parentRef.current;
    if (!element || data.length === 0) return null;
    const rect = element.getBoundingClientRect();
    const localX = x - rect.left + element.scrollLeft;
    const localY = y - rect.top + element.scrollTop - 40;

    let colIndex = 0;
    let widthAcc = 0;
    for (let i = 0; i < columns.length; i++) {
      widthAcc += getColumnWidth(columns[i]);
      if (localX <= widthAcc) {
        colIndex = i;
        break;
      }
      colIndex = i;
    }

    const estimatedRow = Math.floor(localY / 40);
    const rowIndex = Math.max(0, Math.min(data.length - 1, estimatedRow));
    return [rowIndex, Math.max(0, Math.min(columns.length - 1, colIndex))];
  };

  const updateDragSelectionAtPointer = (x: number, y: number) => {
    const resolved = resolvePointerCell(x, y);
    if (!resolved) return;
    const [row, col] = resolved;
    setSelectionRange((prev) => {
      const anchor: [number, number] = dragAnchorRef.current ?? prev?.start ?? focusedCellRef.current ?? [row, col];
      return { start: anchor, end: [row, col] };
    });
    setFocusedCell([row, col]);
  };

  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (event: MouseEvent) => {
      if (event.shiftKey && dragModeRef.current !== "shift") {
        dragModeRef.current = "shift";
      } else if (!event.shiftKey && dragModeRef.current === "shift") {
        dragModeRef.current = "normal";
      }
      pointerRef.current = { x: event.clientX, y: event.clientY };
      updateDragSelectionAtPointer(event.clientX, event.clientY);
    };

    window.addEventListener("mousemove", handleMouseMove);
    return () => window.removeEventListener("mousemove", handleMouseMove);
  }, [isDragging]);

  useEffect(() => {
    if (!isDragging) return;
    const container = parentRef.current;
    if (!container) return;

    let rafId: number | null = null;
    const edge = 28;
    const step = 18;

    const tick = () => {
      const pointer = pointerRef.current;
      const element = parentRef.current;
      if (pointer && element && dragModeRef.current !== "none") {
        const rect = element.getBoundingClientRect();
        let dx = 0;
        let dy = 0;

        if (pointer.y < rect.top + edge) dy = -step;
        else if (pointer.y > rect.bottom - edge) dy = step;
        if (pointer.x < rect.left + edge) dx = -step;
        else if (pointer.x > rect.right - edge) dx = step;

        const maxLeft = Math.max(0, element.scrollWidth - element.clientWidth);
        const maxTop = Math.max(0, element.scrollHeight - element.clientHeight);
        const nextLeft = Math.max(0, Math.min(maxLeft, element.scrollLeft + dx));
        const nextTop = Math.max(0, Math.min(maxTop, element.scrollTop + dy));

        if (nextLeft !== element.scrollLeft) {
          element.scrollLeft = nextLeft;
        }
        if (nextTop !== element.scrollTop) {
          element.scrollTop = nextTop;
        }
        updateDragSelectionAtPointer(pointer.x, pointer.y);
      }
      rafId = window.requestAnimationFrame(tick);
    };

    rafId = window.requestAnimationFrame(tick);
    return () => {
      if (rafId != null) window.cancelAnimationFrame(rafId);
    };
  }, [isDragging, data.length, columns.length]);

  useEffect(() => {
    if (!contextMenu) return;
    const close = () => setContextMenu(null);
    window.addEventListener("click", close);
    window.addEventListener("scroll", close, true);
    window.addEventListener("resize", close);
    return () => {
      window.removeEventListener("click", close);
      window.removeEventListener("scroll", close, true);
      window.removeEventListener("resize", close);
    };
  }, [contextMenu]);

  const handlePaste = (e: ClipboardEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!focusedCell) return;
    if (deletedIds.includes(data[focusedCell[0]]?.id)) return;

    const pasteData = e.clipboardData.getData("text");
    const rows = pasteData.split(/\r?\n/).map((row) => row.split("\t"));

    const startRow = focusedCell[0];
    const startCol = focusedCell[1];

    for (let r = 0; r < rows.length; r++) {
      const targetRowIdx = startRow + r;
      if (targetRowIdx >= data.length) break;

      for (let c = 0; c < rows[r].length; c++) {
        const targetColIdx = startCol + c;
        if (targetColIdx >= columns.length) break;

        const col = columns[targetColIdx];
        if (col.editable && col.field !== "select" && col.field !== "history" && col.field !== "delete") {
          const val = rows[r][c].trim();
          onCellValueChanged(data[targetRowIdx].id, col.field as keyof WorkItem, val);
        }
      }
    }
  };

  const handleCopy = (e: ClipboardEvent<HTMLDivElement>) => {
    const text = buildCopyText(copyWithHeader);
    if (!text) return;
    e.preventDefault();
    e.clipboardData.setData("text/plain", text);
  };

  const copySelectionToClipboard = async () => {
    const text = buildCopyText(copyWithHeader);
    if (!text) return;
    await navigator.clipboard?.writeText(text);
  };

  const cutSelectionToClipboard = async () => {
    const text = buildCopyText(copyWithHeader);
    if (!text) return;
    await navigator.clipboard?.writeText(text);
    clearSelectedCells();
  };

  const pasteFromClipboard = async () => {
    if (!focusedCell) return;
    if (deletedIds.includes(data[focusedCell[0]]?.id)) return;

    const pasteData = await navigator.clipboard?.readText();
    if (!pasteData) return;
    const rows = pasteData.split(/\r?\n/).map((row) => row.split("\t"));
    const startRow = focusedCell[0];
    const startCol = focusedCell[1];

    for (let r = 0; r < rows.length; r++) {
      const targetRowIdx = startRow + r;
      if (targetRowIdx >= data.length) break;

      for (let c = 0; c < rows[r].length; c++) {
        const targetColIdx = startCol + c;
        if (targetColIdx >= columns.length) break;
        const col = columns[targetColIdx];
        if (col.editable && col.field !== "select" && col.field !== "history" && col.field !== "delete") {
          onCellValueChanged(data[targetRowIdx].id, col.field as keyof WorkItem, rows[r][c].trim());
        }
      }
    }
  };

  const isCellSelected = (r: number, c: number) => {
    if (selectedColumns.has(c)) return true;
    if (selectedCells.has(`${r}:${c}`)) return true;
    if (!selectionRange) return false;
    const minR = Math.min(selectionRange.start[0], selectionRange.end[0]);
    const maxR = Math.max(selectionRange.start[0], selectionRange.end[0]);
    const minC = Math.min(selectionRange.start[1], selectionRange.end[1]);
    const maxC = Math.max(selectionRange.start[1], selectionRange.end[1]);
    return r >= minR && r <= maxR && c >= minC && c <= maxC;
  };

  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    if (!hasNextPage || isFetchingNextPage || !fetchNextPage) return;
    const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;
    if (scrollHeight - scrollTop - clientHeight < 150) {
      fetchNextPage();
    }
  };

  const handleHeaderMouseDown = (event: React.MouseEvent<HTMLDivElement>, cIdx: number) => {
    if (event.ctrlKey || event.metaKey) {
      setSelectedColumns((prev) => {
        const next = new Set(prev);
        if (next.has(cIdx)) next.delete(cIdx);
        else next.add(cIdx);
        return next;
      });
      setSelectionRange(null);
      setSelectedCells(new Set());
      return;
    }

    setSelectedColumns(new Set([cIdx]));
    setSelectionRange(null);
    setSelectedCells(new Set());
    setFocusedCell([0, cIdx]);
  };

  const memoColumnIndex = columns.findIndex((col) => col.field === "memo");
  const memoBaseWidth = memoColumnIndex >= 0 ? columns[memoColumnIndex].width : 0;
  const baseTotalWidth = columns.reduce((acc, c) => acc + c.width, 0);
  const fixedWidthWithoutMemo = baseTotalWidth - memoBaseWidth;
  const memoEffectiveWidth =
    memoColumnIndex >= 0 ? Math.max(memoBaseWidth, viewportWidth - fixedWidthWithoutMemo) : memoBaseWidth;
  const totalWidth = memoColumnIndex >= 0 ? fixedWidthWithoutMemo + memoEffectiveWidth : baseTotalWidth;
  const getColumnWidth = (col: ColumnDef) => (col.field === "memo" ? memoEffectiveWidth : col.width);
  const scrollCellIntoView = (rowIdx: number, colIdx: number) => {
    const element = parentRef.current;
    if (!element) return;
    const rowHeight = 40;
    const headerHeight = 40;

    // Keep focused row visible below sticky header while navigating with keyboard.
    const rowTop = headerHeight + rowIdx * rowHeight;
    const rowBottom = rowTop + rowHeight;
    const viewTop = element.scrollTop + headerHeight;
    const viewBottom = element.scrollTop + element.clientHeight;
    if (rowTop < viewTop) {
      element.scrollTop = Math.max(0, rowTop - headerHeight);
    } else if (rowBottom > viewBottom) {
      element.scrollTop = Math.max(0, rowBottom - element.clientHeight);
    }

    let left = 0;
    for (let i = 0; i < colIdx; i++) {
      left += getColumnWidth(columns[i]);
    }
    const width = getColumnWidth(columns[colIdx]);
    const right = left + width;

    const viewLeft = element.scrollLeft;
    const viewRight = viewLeft + element.clientWidth;
    if (left < viewLeft) {
      element.scrollLeft = left;
    } else if (right > viewRight) {
      element.scrollLeft = right - element.clientWidth;
    }
  };

  return (
    <div
      ref={parentRef}
      className="w-full h-[585px] overflow-auto border border-[var(--border-main)] bg-[var(--bg-app)] rounded outline-none select-none"
      tabIndex={0}
      onKeyDown={handleKeyDown}
      onPaste={handlePaste}
      onCopy={handleCopy}
      onScroll={handleScroll}
    >
      <div style={{ width: totalWidth, height: rowVirtualizer.getTotalSize(), position: "relative" }}>
        <div className="sticky top-0 z-20 flex border-b border-[var(--border-main)] bg-[var(--bg-app)] text-[var(--text-secondary)] text-sm font-semibold">
          {columns.map((col, idx) => {
            const stickyClass =
              col.field === "delete"
                ? "sticky right-0 bg-[var(--bg-app)] z-30 shadow-[-4px_0_10px_-4px_rgba(0,0,0,0.3)]"
                : col.field === "history"
                  ? "sticky right-[60px] bg-[var(--bg-app)] z-30 shadow-[-4px_0_10px_-4px_rgba(0,0,0,0.3)]"
                  : "";

            return (
              <div
                key={idx}
                className={`px-3 py-2 border-r border-[var(--border-main)] flex items-center ${stickyClass}`}
                style={{ width: getColumnWidth(col), minWidth: getColumnWidth(col) }}
                onMouseDown={(e) => handleHeaderMouseDown(e, idx)}
              >
                {col.field === "select" ? (
                  <div className="flex w-full items-center justify-center">
                    <input
                      type="checkbox"
                      checked={data.length > 0 && selectedIds.length === data.length}
                      onChange={(e) => onSelectionChange(e.target.checked ? data.map((d) => d.id) : [])}
                    />
                  </div>
                ) : (
                  col.headerName
                )}
              </div>
            );
          })}
        </div>

        {rowVirtualizer.getVirtualItems().map((virtualRow) => {
          const row = data[virtualRow.index];
          const isRowSelected = selectedIds.includes(row.id);
          const isRowDeleted = deletedIds.includes(row.id);

          return (
            <div
              key={virtualRow.key}
              className="group absolute top-0 left-0 flex text-sm text-[var(--text-primary)] border-b border-[var(--border-main)]/50 hover:bg-[var(--bg-hover)] transition-colors"
              style={{
                width: "100%",
                height: `${virtualRow.size}px`,
                transform: `translateY(${virtualRow.start + 40}px)`,
              }}
            >
              {columns.map((col, cIdx) => {
                const isFocused = focusedCell?.[0] === virtualRow.index && focusedCell?.[1] === cIdx;
                const isEditing = editingCell?.[0] === virtualRow.index && editingCell?.[1] === cIdx;
                const isSelected = isCellSelected(virtualRow.index, cIdx);
                const isDirty =
                  col.field !== "select" && col.field !== "history" && col.field !== "delete" &&
                  dirtyMap[row.id]?.[col.field as keyof WorkItem] !== undefined;

                let cellContent: React.ReactNode;

                if (col.field === "select") {
                  cellContent = (
                    <div className="flex items-center justify-center w-full h-full">
                      <input
                        type="checkbox"
                        checked={isRowSelected}
                        disabled={isRowDeleted}
                        onChange={(e) => {
                          if (e.target.checked) onSelectionChange([...selectedIds, row.id]);
                          else onSelectionChange(selectedIds.filter((id) => id !== row.id));
                        }}
                      />
                    </div>
                  );
                } else if (col.type === "action" && col.field === "history") {
                  cellContent = (
                    <div className="flex items-center justify-center w-full h-full">
                      <button
                        type="button"
                        onClick={() => onHistoryClick?.(row)}
                        className={`px-2 py-0.5 text-xs font-black rounded border transition-all shadow-sm ${
                          row.hasAudit
                            ? "bg-[var(--btn-history-bg)] border-[var(--btn-history-border)] text-[var(--btn-history-text)] hover:bg-[var(--accent)] hover:text-white"
                            : "bg-[var(--bg-hover)] border-[var(--border-main)] text-[var(--text-secondary)] opacity-40"
                        }`}
                        title="이력 보기"
                      >
                        이력
                      </button>
                    </div>
                  );
                } else if (col.type === "action" && col.field === "delete") {
                  cellContent = (
                    <div className="flex items-center justify-center w-full h-full">
                      <button
                        type="button"
                        disabled={isRowDeleted}
                        onClick={() => onDeleteClick?.(row)}
                        className={`px-2 py-0.5 text-xs font-black rounded border transition-all shadow-sm ${
                          isRowDeleted
                            ? "bg-[var(--bg-hover)] border-[var(--border-main)] text-[var(--text-secondary)] opacity-20 cursor-not-allowed"
                            : "bg-[var(--btn-delete-bg)] border-[var(--btn-delete-border)] text-[var(--btn-delete-text)] hover:bg-rose-600 hover:text-white"
                        }`}
                        title="삭제 표시"
                      >
                        삭제
                      </button>
                    </div>
                  );
                } else if (isEditing) {
                  if (col.type === "select") {
                    cellContent = (
                      <select
                        ref={inputRef as React.RefObject<HTMLSelectElement>}
                        className="w-full h-full bg-[var(--bg-input)] text-[var(--text-primary)] outline-none px-2"
                        value={editValue}
                        onMouseDown={(e) => e.stopPropagation()}
                        onChange={(e) => setEditValue(e.target.value)}
                        onBlur={() => finishEditing()}
                      >
                        {col.options?.map((o) => (
                          <option key={o} value={o}>{o}</option>
                        ))}
                      </select>
                    );
                  } else if (col.field === "memo") {
                    cellContent = (
                      <div className="w-full h-full px-3 py-2 text-[var(--text-secondary)]">메모 편집 중...</div>
                    );
                  } else if (col.type === "date") {
                    const openNativePicker = () => {
                      const picker = nativeDatePickerRef.current;
                      if (!picker) return;
                      try {
                        picker.showPicker?.();
                      } catch {
                        picker.focus();
                      }
                    };

                    cellContent = (
                      <div className="relative flex h-full w-full items-center gap-1.5 bg-[var(--bg-input)] px-2">
                        <input
                          ref={inputRef as React.RefObject<HTMLInputElement>}
                          type="text"
                          className="h-full w-full bg-transparent text-[var(--text-primary)] outline-none"
                          value={editValue}
                          maxLength={10}
                          inputMode="numeric"
                          placeholder="YYYY-MM-DD"
                          onChange={(e) => setEditValue(normalizeDateInput(e.target.value))}
                          onBlur={() => finishEditing()}
                        />
                        <input
                          ref={nativeDatePickerRef}
                          type="date"
                          tabIndex={-1}
                          aria-hidden="true"
                          className="pointer-events-none absolute h-0 w-0 opacity-0"
                          value={/^\d{4}-\d{2}-\d{2}$/.test(editValue) ? editValue : ""}
                          onChange={(e) => {
                            const selected = e.target.value;
                            setEditValue(selected);
                            finishEditing(selected);
                          }}
                        />
                        <button
                          type="button"
                          className="inline-flex h-5 w-5 items-center justify-center rounded border border-[var(--border-main)] bg-[var(--bg-hover)] text-[var(--btn-icon-text)] hover:bg-[var(--accent)] hover:text-white shrink-0 transition-all shadow-sm"
                          onMouseDown={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            openNativePicker();
                          }}
                          title="달력 열기"
                          aria-label="달력 열기"
                        >
                          <CalendarDays size={12} />
                        </button>
                      </div>
                    );
                  } else {
                    cellContent = (
                      <input
                        ref={inputRef as React.RefObject<HTMLInputElement>}
                        type="text"
                        className="w-full h-full bg-[var(--bg-input)] text-[var(--text-primary)] outline-none px-2"
                        value={editValue}
                        onChange={(e) => setEditValue(e.target.value)}
                        onBlur={() => finishEditing()}
                      />
                    );
                  }
                } else {
                  const val = String(row[col.field as keyof WorkItem] ?? "");
                  if (col.field === "dueDate") {
                    cellContent = (
                      <div className="w-full h-full px-3 py-2 truncate flex items-center gap-1.5">
                        <span className="truncate flex-1">{val}</span>
                        <button
                          type="button"
                          className="inline-flex h-5 w-5 items-center justify-center rounded border border-[var(--border-main)] bg-[var(--bg-hover)] text-[var(--btn-icon-text)] hover:bg-[var(--accent)] hover:text-white shrink-0 transition-all shadow-sm"
                          onMouseDown={(e) => e.stopPropagation()}
                          onClick={(e) => {
                            e.stopPropagation();
                            if (isRowDeleted) return;
                            setFocusedCell([virtualRow.index, cIdx]);
                            startEditing(virtualRow.index, cIdx, { openDatePicker: true });
                          }}
                          title="마감일 편집"
                          aria-label="마감일 편집"
                        >
                          <CalendarDays size={12} />
                        </button>
                      </div>
                    );
                  } else if (col.field === "updatedAt") {
                    cellContent = <div className="w-full h-full px-3 py-2 truncate flex items-center">{toDisplayDateTime(val)}</div>;
                  } else {
                    cellContent = <div className="w-full h-full px-3 py-2 truncate flex items-center">{val}</div>;
                  }
                }

                const stickyClass =
                  col.field === "delete"
                    ? "sticky right-0 bg-[var(--bg-app)] group-hover:bg-[var(--bg-hover)] z-10"
                    : col.field === "history"
                      ? "sticky right-[60px] bg-[var(--bg-app)] group-hover:bg-[var(--bg-hover)] z-10"
                      : "";

                return (
                  <div
                    key={cIdx}
                    data-grid-cell="1"
                    data-row={virtualRow.index}
                    data-col={cIdx}
                    onMouseDown={(e) => handleMouseDown(e, virtualRow.index, cIdx)}
                    onMouseEnter={() => handleMouseEnter(virtualRow.index, cIdx)}
                    onContextMenu={(e) => handleCellContextMenu(e, virtualRow.index, cIdx)}
                    onDoubleClick={(e) => {
                      if (isRowDeleted) return;
                      startEditing(virtualRow.index, cIdx, { anchorEl: e.currentTarget });
                    }}
                    className={`border-r border-[var(--border-main)]/50 relative ${stickyClass} ${
                      isSelected ? "bg-blue-900/40" : isDirty ? "bg-amber-900/20 text-amber-200" : ""
                    } ${isFocused ? "ring-2 ring-inset ring-blue-500 z-20" : ""} ${
                      isRowDeleted ? "line-through decoration-2 decoration-rose-400 opacity-60" : ""
                    } ${isEditing && col.field === "memo" ? "z-[900]" : ""}`}
                    style={{ width: getColumnWidth(col), minWidth: getColumnWidth(col) }}
                  >
                    {cellContent}
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
      {editingCell && columns[editingCell[1]]?.field === "memo" && memoEditorRect &&
        createPortal(
          <textarea
            ref={inputRef as React.RefObject<HTMLTextAreaElement>}
            className="fixed z-[2147483647] rounded-lg border-2 border-[var(--accent)] bg-[var(--bg-card)] px-3 py-2 text-sm text-[var(--text-primary)] shadow-2xl outline-none ring-4 ring-[var(--accent)]/10 resize"
            style={{
              top: memoEditorRect.top,
              left: memoEditorRect.left,
              width: memoEditorRect.width,
              minHeight: memoEditorRect.height,
            }}
            value={editValue}
            onMouseDown={(e) => e.stopPropagation()}
            onChange={(e) => setEditValue(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") e.stopPropagation();
              if (e.key === "Escape") {
                e.preventDefault();
                setEditingCell(null);
                setMemoEditorRect(null);
              }
            }}
            onBlur={() => finishEditing()}
          />,
          document.body
        )}
      {contextMenu &&
        createPortal(
          <div
            className="fixed z-[2147483646] min-w-44 rounded-lg border border-[var(--border-main)] bg-[var(--bg-card)] p-1 shadow-2xl"
            style={{
              left: Math.max(8, Math.min(contextMenu.x, window.innerWidth - 190)),
              top: Math.max(8, Math.min(contextMenu.y, window.innerHeight - 230)),
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="block w-full rounded px-3 py-2 text-left text-sm text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
              onClick={() => {
                void copySelectionToClipboard().catch(() => undefined);
                setContextMenu(null);
              }}
            >
              복사 (Ctrl+C)
            </button>
            <button
              type="button"
              className="block w-full rounded px-3 py-2 text-left text-sm text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
              onClick={() => {
                void cutSelectionToClipboard().catch(() => undefined);
                setContextMenu(null);
              }}
            >
              잘라내기 (Ctrl+X)
            </button>
            <button
              type="button"
              className="block w-full rounded px-3 py-2 text-left text-sm text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
              onClick={() => {
                void pasteFromClipboard().catch(() => undefined);
                setContextMenu(null);
              }}
            >
              붙여넣기 (Ctrl+V)
            </button>
            <button
              type="button"
              className="block w-full rounded px-3 py-2 text-left text-sm text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
              onClick={() => {
                const { row, col, anchorRect } = contextMenu;
                if (!deletedIds.includes(data[row]?.id)) {
                  startEditing(row, col, { anchorRect });
                }
                setContextMenu(null);
              }}
            >
              셀 편집 (F2)
            </button>
            <button
              type="button"
              className="block w-full rounded px-3 py-2 text-left text-sm text-[var(--text-primary)] hover:bg-[var(--bg-hover)]"
              onClick={() => {
                clearSelectedCells();
                setContextMenu(null);
              }}
            >
              값 비우기
            </button>
            {onDeleteClick && (
              <button
                type="button"
                className="block w-full rounded px-3 py-2 text-left text-sm text-rose-500 hover:bg-rose-100/10"
                onClick={() => {
                  const row = data[contextMenu.row];
                  if (row && !deletedIds.includes(row.id)) {
                    onDeleteClick(row);
                  }
                  setContextMenu(null);
                }}
              >
                행 삭제 표시
              </button>
            )}
          </div>,
          document.body
        )}
    </div>
  );
}
