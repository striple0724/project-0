import React, { useState, useRef, useEffect, KeyboardEvent, ClipboardEvent } from "react";
import { useVirtualizer } from "@tanstack/react-virtual";
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
  const parentRef = useRef<HTMLDivElement>(null);

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

  const inputRef = useRef<HTMLInputElement | HTMLSelectElement>(null);

  useEffect(() => {
    if (editingCell && inputRef.current) {
      inputRef.current.focus();
    }
  }, [editingCell]);

  const buildCopyText = (includeHeader: boolean) => {
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
      return "";
    }

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
    setFocusedCell((prev) => {
      if (!prev) return [0, 0];
      const nextRow = Math.max(0, Math.min(data.length - 1, prev[0] + rowDelta));
      let nextCol = prev[1] + colDelta;
      if (nextCol < 0) nextCol = 0;
      if (nextCol > columns.length - 1) nextCol = columns.length - 1;

      const nextCell: [number, number] = [nextRow, nextCol];
      if (shiftKey && selectionRange) {
        setSelectionRange({ start: selectionRange.start, end: nextCell });
      } else {
        setSelectionRange({ start: nextCell, end: nextCell });
        setSelectedCells(new Set());
        setSelectedColumns(new Set());
      }
      return nextCell;
    });
  };

  const startEditing = (rowIdx: number, colIdx: number, initialValue?: string) => {
    const col = columns[colIdx];
    if (!col.editable || col.field === "history" || col.field === "delete") return;

    const row = data[rowIdx];
    const val = String(row[col.field as keyof WorkItem] ?? "");
    setEditValue(initialValue ?? val);
    setEditingCell([rowIdx, colIdx]);
  };

  const finishEditing = () => {
    if (!editingCell) return;
    const [r, c] = editingCell;
    const col = columns[c];
    const row = data[r];
    if (col.field !== "select" && col.field !== "history" && col.field !== "delete" && col.editable) {
      onCellValueChanged(row.id, col.field as keyof WorkItem, editValue);
    }
    setEditingCell(null);
    if (parentRef.current) parentRef.current.focus();
  };

  const clearSelectedCells = () => {
    if (!selectionRange) return;
    const minR = Math.min(selectionRange.start[0], selectionRange.end[0]);
    const maxR = Math.max(selectionRange.start[0], selectionRange.end[0]);
    const minC = Math.min(selectionRange.start[1], selectionRange.end[1]);
    const maxC = Math.max(selectionRange.start[1], selectionRange.end[1]);

    for (let r = minR; r <= maxR; r++) {
      for (let c = minC; c <= maxC; c++) {
        const col = columns[c];
        if (col.editable && col.field !== "select" && col.field !== "history" && col.field !== "delete") {
          onCellValueChanged(data[r].id, col.field as keyof WorkItem, "");
        }
      }
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (!focusedCell) return;
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

    if (focusedRowDeleted) return;

    if (e.key === "ArrowDown") moveFocus(1, 0, e.shiftKey);
    else if (e.key === "ArrowUp") moveFocus(-1, 0, e.shiftKey);
    else if (e.key === "ArrowRight") moveFocus(0, 1, e.shiftKey);
    else if (e.key === "ArrowLeft") moveFocus(0, -1, e.shiftKey);
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
      startEditing(focusedCell[0], focusedCell[1], e.key);
      e.preventDefault();
    }
  };

  const handleMouseDown = (event: React.MouseEvent<HTMLDivElement>, rIdx: number, cIdx: number) => {
    if (editingCell) {
      const [er, ec] = editingCell;
      if (er === rIdx && ec === cIdx) {
        return;
      }
      finishEditing();
    }
    setFocusedCell([rIdx, cIdx]);

    if (event.ctrlKey || event.metaKey) {
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

    setSelectedCells(new Set());
    setSelectedColumns(new Set());
    setSelectionRange({ start: [rIdx, cIdx], end: [rIdx, cIdx] });
    setIsDragging(true);
  };

  const handleMouseEnter = (rIdx: number, cIdx: number) => {
    if (isDragging && selectionRange) {
      setSelectionRange({ start: selectionRange.start, end: [rIdx, cIdx] });
    }
  };

  useEffect(() => {
    const handleGlobalMouseUp = () => setIsDragging(false);
    window.addEventListener("mouseup", handleGlobalMouseUp);
    return () => window.removeEventListener("mouseup", handleGlobalMouseUp);
  }, []);

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

  const totalWidth = columns.reduce((acc, c) => acc + c.width, 0);

  return (
    <div
      ref={parentRef}
      className="w-full h-[500px] overflow-auto border border-slate-800 bg-[#061022] rounded outline-none select-none"
      tabIndex={0}
      onKeyDown={handleKeyDown}
      onPaste={handlePaste}
      onCopy={handleCopy}
      onScroll={handleScroll}
    >
      <div style={{ width: totalWidth, height: rowVirtualizer.getTotalSize(), position: "relative" }}>
        <div className="sticky top-0 z-20 flex border-b border-slate-800 bg-[#0a152d] text-slate-300 text-sm font-semibold">
          {columns.map((col, idx) => {
            const stickyClass =
              col.field === "delete"
                ? "sticky right-0 bg-[#0a152d] z-30 shadow-[-4px_0_10px_-4px_rgba(0,0,0,0.3)]"
                : col.field === "history"
                  ? "sticky right-[60px] bg-[#0a152d] z-30 shadow-[-4px_0_10px_-4px_rgba(0,0,0,0.3)]"
                  : "";

            return (
              <div
                key={idx}
                className={`px-3 py-2 border-r border-slate-800 flex items-center ${col.field === "memo" ? "flex-1" : ""} ${stickyClass}`}
                style={{ width: col.field === "memo" ? "auto" : col.width, minWidth: col.field === "memo" ? 200 : col.width }}
                onMouseDown={(e) => handleHeaderMouseDown(e, idx)}
              >
                {col.field === "select" ? (
                  <input
                    type="checkbox"
                    checked={data.length > 0 && selectedIds.length === data.length}
                    onChange={(e) => onSelectionChange(e.target.checked ? data.map((d) => d.id) : [])}
                  />
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
              className="group absolute top-0 left-0 flex text-sm text-slate-200 border-b border-slate-800/50 hover:bg-[#0a152d] transition-colors"
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
                        className={`px-2 py-0.5 text-xs rounded border transition ${
                          row.hasAudit
                            ? "bg-sky-900/50 border-sky-500/50 text-sky-200 hover:bg-sky-500 hover:text-white"
                            : "bg-slate-800/50 border-slate-700/50 text-slate-500 hover:text-slate-300"
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
                        className={`px-2 py-0.5 text-xs rounded border transition ${
                          isRowDeleted
                            ? "bg-slate-800/40 border-slate-700/40 text-slate-500 cursor-not-allowed"
                            : "bg-rose-900/50 border-rose-500/50 text-rose-200 hover:bg-rose-500 hover:text-white"
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
                        className="w-full h-full bg-[#0a152d] text-white outline-none px-2"
                        value={editValue}
                        onMouseDown={(e) => e.stopPropagation()}
                        onChange={(e) => setEditValue(e.target.value)}
                        onBlur={finishEditing}
                      >
                        {col.options?.map((o) => (
                          <option key={o} value={o}>{o}</option>
                        ))}
                      </select>
                    );
                  } else {
                    cellContent = (
                      <input
                        ref={inputRef as React.RefObject<HTMLInputElement>}
                        type={col.type === "date" ? "date" : "text"}
                        className="w-full h-full bg-[#0a152d] text-white outline-none px-2"
                        value={editValue}
                        onChange={(e) => setEditValue(e.target.value)}
                        onBlur={finishEditing}
                      />
                    );
                  }
                } else {
                  const val = String(row[col.field as keyof WorkItem] ?? "");
                  cellContent = <div className="w-full h-full px-3 py-2 truncate flex items-center">{val}</div>;
                }

                const stickyClass =
                  col.field === "delete"
                    ? "sticky right-0 bg-[#061022] group-hover:bg-[#0a152d] z-10"
                    : col.field === "history"
                      ? "sticky right-[60px] bg-[#061022] group-hover:bg-[#0a152d] z-10"
                      : "";

                return (
                  <div
                    key={cIdx}
                    onMouseDown={(e) => handleMouseDown(e, virtualRow.index, cIdx)}
                    onMouseEnter={() => handleMouseEnter(virtualRow.index, cIdx)}
                    onDoubleClick={() => {
                      if (isRowDeleted) return;
                      startEditing(virtualRow.index, cIdx);
                    }}
                    className={`border-r border-slate-800/50 relative ${col.field === "memo" ? "flex-1" : ""} ${stickyClass} ${
                      isSelected ? "bg-blue-900/40" : isDirty ? "bg-amber-900/20 text-amber-200" : ""
                    } ${isFocused ? "ring-2 ring-inset ring-blue-500 z-20" : ""} ${
                      isRowDeleted ? "line-through decoration-2 decoration-rose-400 opacity-60" : ""
                    }`}
                    style={{ width: col.field === "memo" ? "auto" : col.width, minWidth: col.field === "memo" ? 200 : col.width }}
                  >
                    {cellContent}
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
