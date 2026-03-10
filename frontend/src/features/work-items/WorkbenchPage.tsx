import { useCallback, useEffect, useMemo, useRef, useState, type ChangeEvent } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, Save, MoreVertical, Search, Loader2, X, RotateCcw } from "lucide-react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { CustomExcelGrid } from "./CustomExcelGrid";
import { ClientSearchModal } from "../clients/ClientSearchModal";
import { AuditHistoryModal } from "./AuditHistoryModal";
import {
  createWorkItem,
  deleteWorkItem,
  downloadExportCsvByUrl,
  fetchExportJobStatus,
  fetchWorkbenchTracking,
  fetchWorkItems,
  patchWorkItem,
  submitBulkCsvFile,
  submitExportJob,
  submitSelectedExportJob,
  type WorkItemFilters,
} from "./api";
import type { WorkItem, WorkStatus, WorkType } from "./types";
import { useSessionStore } from "../../auth/session-store";
import { localLogout } from "../../auth/local-auth-api";

const statuses: WorkStatus[] = ["TODO", "IN_PROGRESS", "DONE", "HOLD"];
const workTypes: WorkType[] = ["FILING", "BOOKKEEPING", "REVIEW", "ETC"];

const createSchema = z.object({
  clientId: z.preprocess(
    (value) => (value === undefined || value === null || value === "" ? Number.NaN : Number(value)),
    z
      .union([z.number(), z.nan()])
      .refine((value) => Number.isInteger(value) && value > 0, "고객사를 선택해주세요.")
  ),
  type: z.enum(["FILING", "BOOKKEEPING", "REVIEW", "ETC"]),
  status: z.enum(["TODO", "IN_PROGRESS", "DONE", "HOLD"]),
  assignee: z.string().min(1),
  dueDate: z.string().min(1),
  memo: z.string().optional(),
});

type CreateForm = z.infer<typeof createSchema>;
type CreateFormInput = z.input<typeof createSchema>;

type Props = {
  embedded?: boolean;
};

type FilterDraft = {
  clientName: string;
  status: WorkStatus | "";
  assignee: string;
  dueDateFrom: Date | null;
  dueDateTo: Date | null;
};

type WorkbenchPersistedState = {
  filters?: Partial<WorkItemFilters>;
  filterDraft?: {
    clientName: string;
    status: WorkStatus | "";
    assignee: string;
    dueDateFrom: string | null;
    dueDateTo: string | null;
  };
  exportTask?: {
    jobId: string | null;
    mode: "FULL" | "SELECTED";
    status: "IDLE" | "QUEUED" | "RUNNING" | "DONE" | "FAILED";
    message: string | null;
    inProgress: boolean;
  };
  bulkTask?: {
    jobId: string | null;
    status: "IDLE" | "QUEUED" | "RUNNING" | "DONE" | "FAILED";
    message: string | null;
    inProgress: boolean;
  };
};

const WORKBENCH_STATE_KEY = "workbench-ui-state-v1";
const WORKBENCH_SERVER_INSTANCE_KEY = "workbench-server-instance-key";

const BULK_MESSAGE = {
  QUEUED: "대량등록 작업 접수 완료",
  RUNNING: "대량등록 작업 진행 중...",
  DONE: "대량등록 작업 완료",
  FAILED: "대량등록 작업 실패",
  UPLOADING: "대량등록 파일 업로드 중...",
} as const;

const EXPORT_MESSAGE = {
  SUBMITTING: "내보내기 작업 제출 중...",
  QUEUED: "CSV 생성 대기 중...",
  RUNNING: "CSV 생성 진행 중...",
  DOWNLOADING: "CSV 파일 다운로드 중...",
  DONE: "CSV 다운로드 완료",
  FAILED: "CSV 생성 작업 실패",
  DOWNLOAD_URL_MISSING: "다운로드 URL을 찾을 수 없습니다.",
} as const;

function toBulkErrorMessage(codeOrMessage: string | null | undefined): string {
  if (!codeOrMessage) return BULK_MESSAGE.FAILED;
  const code = codeOrMessage.trim();
  const table: Record<string, string> = {
    CSV_EMPTY_FILE: "CSV 파일이 비어 있습니다.",
    CSV_INVALID_HEADER: "CSV 헤더가 올바르지 않습니다.",
    CSV_INVALID_COLUMN_COUNT: "CSV 컬럼 수가 올바르지 않습니다.",
    CSV_EMPTY_REQUEST_ID: "CSV requestId 값이 비어 있습니다.",
    CSV_MIXED_REQUEST_ID: "CSV requestId 값이 행마다 다릅니다.",
    CSV_INVALID_CLIENT_ID: "CSV clientId 값이 올바르지 않습니다.",
    CSV_INVALID_WORK_TYPE: "CSV type 값이 올바르지 않습니다.",
    CSV_EMPTY_ASSIGNEE: "CSV assignee 값이 비어 있습니다.",
    CSV_INVALID_DUEDATE: "CSV dueDate 값이 올바르지 않습니다.",
    CLIENT_NOT_FOUND: "존재하지 않는 clientId가 포함되어 있습니다.",
    BULK_EXECUTOR_REJECTED: "서버 처리 큐가 가득 찼습니다. 잠시 후 다시 시도해주세요.",
    BULK_FILE_UPLOAD_FAILED: "CSV 업로드 파일 처리 중 오류가 발생했습니다.",
    UPLOAD_SIZE_LIMIT_EXCEEDED: "파일 용량이 서버 제한을 초과했습니다.",
  };
  return table[code] ?? codeOrMessage;
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function isValidDateString(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00`);
  return !Number.isNaN(date.getTime()) && toIsoDate(date) === value;
}

function parseStoredDate(value: string | null | undefined): Date | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function loadWorkbenchState(): WorkbenchPersistedState {
  try {
    const raw = window.sessionStorage.getItem(WORKBENCH_STATE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as WorkbenchPersistedState;
    return parsed ?? {};
  } catch {
    return {};
  }
}

export function WorkbenchPage({ embedded = false }: Props) {
  const navigate = useNavigate();
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const clearSession = useSessionStore((s) => s.clearSession);
  const persistedInitial = useMemo(() => loadWorkbenchState(), []);

  // Set defaultTo as the last day of the current month
  const today = new Date();
  const defaultToDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  const defaultTo = toIsoDate(defaultToDate);

  // Set defaultFrom as 1st day of 3 months ago
  const defaultFromDate = new Date(today.getFullYear(), today.getMonth() - 3, 1);
  const defaultFrom = toIsoDate(defaultFromDate);

  const [filters, setFilters] = useState<WorkItemFilters>(() => ({
    page: persistedInitial.filters?.page ?? 0,
    size: persistedInitial.filters?.size ?? 50,
    includeTotal: persistedInitial.filters?.includeTotal ?? true,
    clientName: persistedInitial.filters?.clientName,
    status: persistedInitial.filters?.status as WorkStatus | "" | undefined,
    assignee: persistedInitial.filters?.assignee,
    dueDateFrom: persistedInitial.filters?.dueDateFrom ?? defaultFrom,
    dueDateTo: persistedInitial.filters?.dueDateTo ?? defaultTo,
    sort: persistedInitial.filters?.sort,
  }));
  const [searchTick, setSearchTick] = useState(0);
  const [filterDraft, setFilterDraft] = useState<FilterDraft>(() => ({
    clientName: persistedInitial.filterDraft?.clientName ?? "",
    status: persistedInitial.filterDraft?.status ?? "",
    assignee: persistedInitial.filterDraft?.assignee ?? "",
    dueDateFrom: parseStoredDate(persistedInitial.filterDraft?.dueDateFrom) ?? defaultFromDate,
    dueDateTo: parseStoredDate(persistedInitial.filterDraft?.dueDateTo) ?? defaultToDate,
  }));
  const [filterError, setFilterError] = useState<string | null>(null);
  const [lastKnownTotals, setLastKnownTotals] = useState<{ totalElements: number; totalPages: number } | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isActionMenuOpen, setIsActionMenuOpen] = useState(false);
  const [isClientSearchModalOpen, setIsClientSearchModalOpen] = useState(false);
  const [selectedClientName, setSelectedClientName] = useState<string>("");
  const [historyModalWorkItem, setHistoryModalWorkItem] = useState<WorkItem | null>(null);
  const [gridRows, setGridRows] = useState<WorkItem[]>([]);
  const [dirtyMap, setDirtyMap] = useState<Record<number, Partial<WorkItem>>>({});
  const [deletedIds, setDeletedIds] = useState<number[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [copyWithHeader, setCopyWithHeader] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isBulkUploading, setIsBulkUploading] = useState(persistedInitial.bulkTask?.inProgress ?? false);
  const [isCsvDownloading, setIsCsvDownloading] = useState(persistedInitial.exportTask?.inProgress ?? false);
  const [bulkJobId, setBulkJobId] = useState<string | null>(persistedInitial.bulkTask?.jobId ?? null);
  const [bulkJobStatus, setBulkJobStatus] = useState<"IDLE" | "QUEUED" | "RUNNING" | "DONE" | "FAILED">(
    persistedInitial.bulkTask?.status ?? "IDLE"
  );
  const [bulkJobMessage, setBulkJobMessage] = useState<string | null>(persistedInitial.bulkTask?.message ?? null);
  const [exportJobId, setExportJobId] = useState<string | null>(persistedInitial.exportTask?.jobId ?? null);
  const [exportMode, setExportMode] = useState<"FULL" | "SELECTED">(persistedInitial.exportTask?.mode ?? "FULL");
  const [exportJobStatus, setExportJobStatus] = useState<"IDLE" | "QUEUED" | "RUNNING" | "DONE" | "FAILED">(
    persistedInitial.exportTask?.status ?? "IDLE"
  );
  const [exportJobMessage, setExportJobMessage] = useState<string | null>(persistedInitial.exportTask?.message ?? null);
  const [actionMenuPosition, setActionMenuPosition] = useState<{ top: number; left: number } | null>(null);
  const [undoStack, setUndoStack] = useState<Array<{
    gridRows: WorkItem[];
    dirtyMap: Record<number, Partial<WorkItem>>;
    deletedIds: number[];
  }>>([]);
  const toastTimerRef = useRef<number | null>(null);
  const exportPollTimerRef = useRef<number | null>(null);
  const exportPollingRef = useRef(false);

  const actionMenuRef = useRef<HTMLDivElement>(null);
  const actionMenuPortalRef = useRef<HTMLDivElement>(null);
  const bulkUploadInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const {
    register: registerWork,
    handleSubmit: handleSubmitWork,
    reset: resetWork,
    setValue: setWorkValue,
    formState: { errors: createErrors },
  } = useForm<CreateFormInput>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      clientId: undefined,
      type: "FILING",
      status: "TODO",
      assignee: "kim",
      dueDate: toIsoDate(today),
      memo: "",
    },
  });

  const workItemsQuery = useInfiniteQuery({
    queryKey: ["work-items", filters, searchTick],
    queryFn: ({ pageParam = 0 }) => fetchWorkItems({ ...filters, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (lastPage: { page: { number: number; totalPages: number } }) => {
      if (lastPage.page.number < lastPage.page.totalPages - 1) {
        return lastPage.page.number + 1;
      }
      return undefined;
    },
  });

  const trackingQuery = useQuery({
    queryKey: ["workbench-tracking"],
    queryFn: fetchWorkbenchTracking,
    refetchInterval: 3000,
  });

  const createMutation = useMutation({
    mutationFn: createWorkItem,
    onSuccess: async () => {
      setFilters((prev) => ({ ...prev, page: 0, includeTotal: true }));
      await queryClient.invalidateQueries({ queryKey: ["work-items"] });
      resetWork({
        clientId: undefined,
        type: "FILING",
        status: "TODO",
        assignee: "kim",
        dueDate: toIsoDate(today),
        memo: "",
      });
      setSelectedClientName("");
      setErrorMessage(null);
      setIsCreateModalOpen(false);
    },
    onError: (error: unknown) => {
      let message = "업무 생성에 실패했습니다. 입력값을 확인해주세요.";
      if (
        error != null &&
        typeof error === "object" &&
        "response" in error &&
        error.response != null &&
        typeof error.response === "object" &&
        "data" in error.response &&
        error.response.data != null &&
        typeof error.response.data === "object" &&
        "detail" in error.response.data &&
        typeof (error.response.data as { detail: unknown }).detail === "string"
      ) {
        message = (error.response.data as { detail: string }).detail;
      }
      setErrorMessage(message);
    },
  });

  const bulkMutation = useMutation({
    mutationFn: (file: File) => submitBulkCsvFile(file),
    onSuccess: async (result) => {
      setBulkJobId(result.data.jobId);
      setBulkJobStatus("QUEUED");
      setBulkJobMessage(BULK_MESSAGE.QUEUED);
      setToastMessage(BULK_MESSAGE.QUEUED);
      await queryClient.invalidateQueries({ queryKey: ["workbench-tracking"] });
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : "CSV 업로드 처리 중 오류가 발생했습니다.";
      setBulkJobStatus("FAILED");
      setBulkJobMessage(`${BULK_MESSAGE.FAILED}: ${message}`);
      setToastMessage(`${BULK_MESSAGE.FAILED}: ${message}`);
    },
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      for (const workItemId of deletedIds) {
        await deleteWorkItem(workItemId);
      }

      for (const [id, patch] of Object.entries(dirtyMap)) {
        const workItemId = Number(id);
        const row = gridRows.find((item) => item.id === workItemId);
        if (!row) continue;
        await patchWorkItem(workItemId, row.version, {
          status: patch.status as WorkStatus | undefined,
          type: patch.type as WorkType | undefined,
          assignee: patch.assignee,
          dueDate: patch.dueDate,
          memo: patch.memo,
        });
      }
    },
    onSuccess: async () => {
      setDirtyMap({});
      setDeletedIds([]);
      setUndoStack([]);
      await queryClient.invalidateQueries({ queryKey: ["work-items"] });
      setToastMessage("저장 완료");
    },
  });

  const pushUndoSnapshot = () => {
    setUndoStack((prev) => {
      const snapshot = {
        gridRows: gridRows.map((row) => ({ ...row })),
        dirtyMap: Object.fromEntries(
          Object.entries(dirtyMap).map(([key, value]) => [Number(key), { ...value }])
        ) as Record<number, Partial<WorkItem>>,
        deletedIds: [...deletedIds],
      };
      const next = [...prev, snapshot];
      if (next.length > 50) next.shift();
      return next;
    });
  };

  const onUndo = useCallback(() => {
    if (undoStack.length === 0) return;
    setUndoStack((prev) => {
      if (prev.length === 0) return prev;
      const last = prev[prev.length - 1];
      setGridRows(last.gridRows);
      setDirtyMap(last.dirtyMap);
      setDeletedIds(last.deletedIds);
      return prev.slice(0, -1);
    });
  }, [undoStack]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((!event.ctrlKey && !event.metaKey) || event.shiftKey || event.altKey) return;
      if (event.key.toLowerCase() !== "z") return;

      const target = event.target as HTMLElement | null;
      if (target) {
        const tag = target.tagName.toLowerCase();
        if (tag === "input" || tag === "textarea" || target.isContentEditable) {
          return;
        }
      }

      if (undoStack.length === 0) return;
      event.preventDefault();
      onUndo();
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onUndo, undoStack.length]);

  useEffect(() => {
    if (workItemsQuery.data) {
      setGridRows(workItemsQuery.data.pages.flatMap((page: { data: WorkItem[] }) => page.data));
    } else {
      setGridRows([]);
    }
  }, [workItemsQuery.data]);

  useEffect(() => {
    const onDocumentClick = (event: MouseEvent) => {
      const target = event.target as Node;
      const inTrigger = !!actionMenuRef.current?.contains(target);
      const inPortal = !!actionMenuPortalRef.current?.contains(target);
      if (!inTrigger && !inPortal) {
        setIsActionMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", onDocumentClick);
    return () => document.removeEventListener("mousedown", onDocumentClick);
  }, []);

  useEffect(() => {
    if (!isActionMenuOpen) return;
    const updatePosition = () => {
      if (!actionMenuRef.current) return;
      const rect = actionMenuRef.current.getBoundingClientRect();
      setActionMenuPosition({
        top: rect.bottom + window.scrollY + 6,
        left: rect.right + window.scrollX,
      });
    };
    updatePosition();
    window.addEventListener("scroll", updatePosition, true);
    window.addEventListener("resize", updatePosition);
    return () => {
      window.removeEventListener("scroll", updatePosition, true);
      window.removeEventListener("resize", updatePosition);
    };
  }, [isActionMenuOpen]);

  useEffect(() => {
    if (!toastMessage) return;
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
    toastTimerRef.current = window.setTimeout(() => {
      setToastMessage(null);
      toastTimerRef.current = null;
    }, 2200);
    return () => {
      if (toastTimerRef.current) {
        window.clearTimeout(toastTimerRef.current);
        toastTimerRef.current = null;
      }
    };
  }, [toastMessage]);

  useEffect(() => {
    return () => {
      if (exportPollTimerRef.current != null) {
        window.clearInterval(exportPollTimerRef.current);
        exportPollTimerRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    const currentServerKey = trackingQuery.data?.serverInstanceKey;
    if (!currentServerKey) return;

    const previousServerKey = window.sessionStorage.getItem(WORKBENCH_SERVER_INSTANCE_KEY);
    if (!previousServerKey) {
      window.sessionStorage.setItem(WORKBENCH_SERVER_INSTANCE_KEY, currentServerKey);
      return;
    }
    if (previousServerKey === currentServerKey) return;

    const defaultFilters: WorkItemFilters = {
      page: 0,
      size: 50,
      includeTotal: true,
      clientName: undefined,
      status: undefined,
      assignee: undefined,
      dueDateFrom: defaultFrom,
      dueDateTo: defaultTo,
      sort: undefined,
    };

    window.sessionStorage.removeItem(WORKBENCH_STATE_KEY);
    window.sessionStorage.setItem(WORKBENCH_SERVER_INSTANCE_KEY, currentServerKey);

    setFilters(defaultFilters);
    setFilterDraft({
      clientName: "",
      status: "",
      assignee: "",
      dueDateFrom: defaultFromDate,
      dueDateTo: defaultToDate,
    });
    setFilterError(null);
    setUndoStack([]);
    setDirtyMap({});
    setDeletedIds([]);
    setSelectedIds([]);
    setBulkJobId(null);
    setBulkJobStatus("IDLE");
    setBulkJobMessage(null);
    setIsBulkUploading(false);
    setExportJobId(null);
    setExportMode("FULL");
    setExportJobStatus("IDLE");
    setExportJobMessage(null);
    setIsCsvDownloading(false);
    setSearchTick((prev) => prev + 1);
  }, [trackingQuery.data?.serverInstanceKey]);

  useEffect(() => {
    const payload: WorkbenchPersistedState = {
      filters: {
        page: filters.page,
        size: filters.size,
        includeTotal: filters.includeTotal,
        clientName: filters.clientName,
        status: filters.status,
        assignee: filters.assignee,
        dueDateFrom: filters.dueDateFrom,
        dueDateTo: filters.dueDateTo,
        sort: filters.sort,
      },
      filterDraft: {
        clientName: filterDraft.clientName,
        status: filterDraft.status,
        assignee: filterDraft.assignee,
        dueDateFrom: filterDraft.dueDateFrom ? toIsoDate(filterDraft.dueDateFrom) : null,
        dueDateTo: filterDraft.dueDateTo ? toIsoDate(filterDraft.dueDateTo) : null,
      },
      exportTask: {
        jobId: exportJobId,
        mode: exportMode,
        status: exportJobStatus,
        message: exportJobMessage,
        inProgress: isCsvDownloading,
      },
      bulkTask: {
        jobId: bulkJobId,
        status: bulkJobStatus,
        message: bulkJobMessage,
        inProgress: isBulkUploading,
      },
    };
    window.sessionStorage.setItem(WORKBENCH_STATE_KEY, JSON.stringify(payload));
  }, [
    filters,
    filterDraft,
    exportJobId,
    exportMode,
    exportJobStatus,
    exportJobMessage,
    isCsvDownloading,
    bulkJobId,
    bulkJobStatus,
    bulkJobMessage,
    isBulkUploading,
  ]);

  const columns = useMemo<any[]>(
    () => [
      { field: "select", headerName: "✔", width: 50 },
      { field: "id", headerName: "ID", width: 90, editable: false },
      { field: "clientName", headerName: "업체명", width: 200, editable: false },
      { field: "type", headerName: "업무유형", width: 140, editable: true, type: "select", options: workTypes },
      { field: "status", headerName: "상태", width: 140, editable: true, type: "select", options: statuses },
      { field: "assignee", headerName: "담당자", width: 140, editable: true, type: "text" },
      { field: "dueDate", headerName: "마감일", width: 150, editable: true, type: "date" },
      { field: "memo", headerName: "메모", width: 250, editable: true, type: "text" },
      { field: "updatedAt", headerName: "수정시각", width: 190, editable: false },
      { field: "history", headerName: "이력", width: 60, type: "action" },
      { field: "delete", headerName: "삭제", width: 60, type: "action" },
    ],
    []
  );

  const pageInfo = workItemsQuery.data?.pages[workItemsQuery.data.pages.length - 1]?.page;

  useEffect(() => {
    if (!pageInfo) return;
    if (pageInfo.totalElements >= 0 && pageInfo.totalPages >= 0) {
      setLastKnownTotals({
        totalElements: pageInfo.totalElements,
        totalPages: pageInfo.totalPages,
      });
    }
  }, [pageInfo]);

  const totalElements =
    pageInfo && pageInfo.totalElements >= 0 ? pageInfo.totalElements : (lastKnownTotals?.totalElements ?? 0);

  const tracking = trackingQuery.data?.data;

  useEffect(() => {
    if (!bulkJobId || !tracking?.recentJobs) return;
    const matched = tracking.recentJobs.find((job) => job.jobId === bulkJobId);
    if (!matched) return;
    setBulkJobStatus(matched.status);
    if (matched.status === "RUNNING") {
      setBulkJobMessage(BULK_MESSAGE.RUNNING);
      setIsBulkUploading(true);
    } else if (matched.status === "DONE") {
      setBulkJobMessage(BULK_MESSAGE.DONE);
      setIsBulkUploading(false);
    } else if (matched.status === "FAILED") {
      const reason = toBulkErrorMessage(matched.errorMessage);
      setBulkJobMessage(`${BULK_MESSAGE.FAILED}: ${reason}`);
      setIsBulkUploading(false);
    } else {
      setBulkJobMessage(BULK_MESSAGE.QUEUED);
      setIsBulkUploading(true);
    }
  }, [bulkJobId, tracking]);

  useEffect(() => {
    if (!isCsvDownloading || !exportJobId) {
      if (exportPollTimerRef.current != null) {
        window.clearInterval(exportPollTimerRef.current);
        exportPollTimerRef.current = null;
      }
      return;
    }

    const pollStatus = async () => {
      if (exportPollingRef.current) return;
      exportPollingRef.current = true;
      try {
        const response = await fetchExportJobStatus(exportJobId);
        const status = response.data.status;
        setExportJobStatus(status);

        if (status === "QUEUED") {
          setExportJobMessage(EXPORT_MESSAGE.QUEUED);
          return;
        }
        if (status === "RUNNING") {
          setExportJobMessage(EXPORT_MESSAGE.RUNNING);
          return;
        }

        if (exportPollTimerRef.current != null) {
          window.clearInterval(exportPollTimerRef.current);
          exportPollTimerRef.current = null;
        }

        if (status === "FAILED") {
          const reason = response.data.errorMessage?.trim();
          const failMessage = reason ? `${EXPORT_MESSAGE.FAILED}: ${reason}` : EXPORT_MESSAGE.FAILED;
          setExportJobMessage(failMessage);
          setToastMessage(failMessage);
          setIsCsvDownloading(false);
          return;
        }

        const downloadUrl = response.data.downloadUrl;
        if (!downloadUrl) {
          setExportJobStatus("FAILED");
          setExportJobMessage(EXPORT_MESSAGE.DOWNLOAD_URL_MISSING);
          setToastMessage(EXPORT_MESSAGE.DOWNLOAD_URL_MISSING);
          setIsCsvDownloading(false);
          return;
        }

        setExportJobMessage(EXPORT_MESSAGE.DOWNLOADING);
        const blob = await downloadExportCsvByUrl(downloadUrl);
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download =
          exportMode === "SELECTED"
            ? `work-items-selected-${toIsoDate(new Date())}.csv`
            : `work-items-all-${toIsoDate(new Date())}.csv`;
        a.click();
        URL.revokeObjectURL(url);

        setExportJobStatus("DONE");
        setExportJobMessage(EXPORT_MESSAGE.DONE);
        setToastMessage(EXPORT_MESSAGE.DONE);
        setIsCsvDownloading(false);
      } catch (error) {
        if (exportPollTimerRef.current != null) {
          window.clearInterval(exportPollTimerRef.current);
          exportPollTimerRef.current = null;
        }
        const message = error instanceof Error ? error.message : "CSV 내보내기 상태 조회에 실패했습니다.";
        setExportJobStatus("FAILED");
        setExportJobMessage(message);
        setToastMessage(message);
        setIsCsvDownloading(false);
      } finally {
        exportPollingRef.current = false;
      }
    };

    void pollStatus();
    exportPollTimerRef.current = window.setInterval(() => {
      void pollStatus();
    }, 2000);

    return () => {
      if (exportPollTimerRef.current != null) {
        window.clearInterval(exportPollTimerRef.current);
        exportPollTimerRef.current = null;
      }
    };
  }, [isCsvDownloading, exportJobId, exportMode]);

  const onSearch = () => {
    if (!filterDraft.dueDateFrom || !filterDraft.dueDateTo) {
      setFilterError("시작일과 종료일을 모두 선택해주세요.");
      return;
    }

    const from = new Date(filterDraft.dueDateFrom);
    from.setHours(0, 0, 0, 0);
    const to = new Date(filterDraft.dueDateTo);
    to.setHours(0, 0, 0, 0);

    if (from.getTime() > to.getTime()) {
      setFilterError("시작일은 종료일보다 클 수 없습니다.");
      return;
    }

    setFilterError(null);
    setFilters((prev) => ({
      ...prev,
      clientName: filterDraft.clientName || undefined,
      status: filterDraft.status || undefined,
      assignee: filterDraft.assignee || undefined,
      dueDateFrom: toIsoDate(filterDraft.dueDateFrom as Date),
      dueDateTo: toIsoDate(filterDraft.dueDateTo as Date),
      page: 0,
      includeTotal: true,
    }));
    setUndoStack([]);
    setSearchTick((prev) => prev + 1);
  };

  const onCellValueChanged = (rowId: number, field: keyof WorkItem, newValue: any) => {
    if (field === "dueDate" && !isValidDateString(String(newValue))) return;
    const currentValue = gridRows.find((item) => item.id === rowId)?.[field];
    if (String(currentValue ?? "") === String(newValue ?? "")) return;
    pushUndoSnapshot();

    setGridRows((prev) => prev.map((item) => (item.id === rowId ? { ...item, [field]: newValue } : item)));
    setDirtyMap((prev) => {
      const before = prev[rowId] ?? {};
      return { ...prev, [rowId]: { ...before, [field]: newValue } };
    });
  };

  const onDeleteRows = () => {
    if (selectedIds.length === 0) return;
    markRowsDeleted(selectedIds);
    setSelectedIds([]);
  };

  const markRowsDeleted = (targetIds: number[]) => {
    const newTargets = targetIds.filter((id) => !deletedIds.includes(id));
    if (newTargets.length === 0) return;
    pushUndoSnapshot();
    setDeletedIds((prev) => Array.from(new Set([...prev, ...newTargets])));
    setDirtyMap((prev) => {
      const next = { ...prev };
      newTargets.forEach((id) => delete next[id]);
      return next;
    });
  };

  const onSaveChanges = () => {
    if (deletedIds.length === 0 && Object.keys(dirtyMap).length === 0) {
      setToastMessage("저장할 변경사항이 없습니다.");
      return;
    }
    saveMutation.mutate();
  };

  const hasUnsavedChanges = deletedIds.length > 0 || Object.keys(dirtyMap).length > 0;

  useEffect(() => {
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!hasUnsavedChanges) return;
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [hasUnsavedChanges]);

  useEffect(() => {
    const onRouteLinkClick = (event: MouseEvent) => {
      if (!hasUnsavedChanges) return;
      const target = event.target as HTMLElement | null;
      if (!target) return;
      const anchor = target.closest("a[href]") as HTMLAnchorElement | null;
      if (!anchor || anchor.target === "_blank") return;

      const current = window.location.pathname + window.location.search;
      const next = anchor.pathname + anchor.search;
      if (next === current) return;

      const proceed = window.confirm("저장되지 않은 변경사항이 있습니다. 저장하지 않고 이동하시겠습니까?");
      if (!proceed) {
        event.preventDefault();
        event.stopPropagation();
      }
    };

    document.addEventListener("click", onRouteLinkClick, true);
    return () => document.removeEventListener("click", onRouteLinkClick, true);
  }, [hasUnsavedChanges]);

  const onDownloadBulkTemplate = () => {
    const a = document.createElement("a");
    a.href = "/templates/work-items-bulk-template.csv";
    a.download = "work-items-bulk-template.csv";
    a.click();
    setIsActionMenuOpen(false);
  };

  const onClickBulkUpload = () => {
    setIsActionMenuOpen(false);
    bulkUploadInputRef.current?.click();
  };

  const onBulkFileSelected = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.currentTarget.value = "";
    if (!file) return;
    setIsBulkUploading(true);
    setBulkJobStatus("RUNNING");
    setBulkJobMessage(BULK_MESSAGE.UPLOADING);
    try {
      await bulkMutation.mutateAsync(file);
    } finally {
      setIsBulkUploading(false);
    }
  };
  const startExportJob = async (mode: "FULL" | "SELECTED", selectedWorkItemIds: number[] = []) => {
    if (isCsvDownloading) return;

    setIsCsvDownloading(true);
    setExportMode(mode);
    setExportJobStatus("QUEUED");
    setExportJobMessage(EXPORT_MESSAGE.SUBMITTING);
    setExportJobId(null);

    try {
      const accepted =
        mode === "SELECTED"
          ? await submitSelectedExportJob(selectedWorkItemIds)
          : await submitExportJob(filters);
      setExportJobId(accepted.data.jobId);
      setExportJobStatus("QUEUED");
      setExportJobMessage(EXPORT_MESSAGE.QUEUED);
      setToastMessage("내보내기 작업 접수 완료");
    } catch (error) {
      const message = error instanceof Error ? error.message : "전체 CSV 다운로드에 실패했습니다.";
      setExportJobStatus("FAILED");
      setExportJobMessage(message);
      setToastMessage(message);
      setIsCsvDownloading(false);
    } finally {
      setIsActionMenuOpen(false);
    }
  };

  const onDownloadCsvSelected = async () => {
    const selectedWorkItemIds = gridRows
      .filter((item) => selectedIds.includes(item.id) && !deletedIds.includes(item.id))
      .map((row) => row.id);
    if (selectedWorkItemIds.length === 0) {
      setToastMessage("선택된 항목이 없습니다.");
      setIsActionMenuOpen(false);
      return;
    }
    await startExportJob("SELECTED", selectedWorkItemIds);
  };

  const onDownloadCsvAllByFilters = async () => {
    await startExportJob("FULL");
  };

  return (
    <div className="workbench-shell mx-auto flex w-full max-w-[1600px] flex-col gap-6 p-6">
      {!embedded && (
        <div className="flex flex-col items-start gap-1 mb-2">
          <div className="flex w-full items-center justify-between">
            <h1 className="text-2xl font-semibold text-slate-100">Tax Workbench</h1>
            <div className="flex items-center gap-2">
              {authMode === "local" ? (
                <button
                  className="rounded border border-slate-700 bg-slate-900 px-3 py-1 text-sm text-slate-200"
                  type="button"
                  onClick={() => {
                    clearSession();
                    navigate("/login", { replace: true });
                    void localLogout().catch(() => undefined);
                  }}
                >
                  로그아웃(Local)
                </button>
              ) : (
                <button
                  className="rounded border border-slate-700 bg-slate-900 px-3 py-1 text-sm text-slate-200"
                  type="button"
                  onClick={() => {
                    clearSession();
                    navigate("/login", { replace: true });
                  }}
                >
                  로그아웃(Mock)
                </button>
              )}
            </div>
          </div>
        </div>
      )}
      {toastMessage && (
        <div className="fixed right-6 top-6 z-[90] rounded-lg border border-emerald-500/50 bg-emerald-900/90 px-4 py-2 text-sm font-medium text-emerald-100 shadow-xl">
          {toastMessage}
        </div>
      )}
      {(isBulkUploading || isCsvDownloading) && (
        <div className="fixed right-6 top-20 z-[90] flex items-center gap-2 rounded-lg border border-sky-500/50 bg-slate-900/95 px-4 py-2 text-sm font-medium text-sky-100 shadow-xl">
          <Loader2 className="animate-spin" size={16} />
          <div className="flex flex-col leading-tight">
            {isBulkUploading && <span>{bulkJobMessage ?? "대량등록 파일 업로드 진행 중..."}</span>}
            {isCsvDownloading && <span>{exportJobMessage ?? "CSV 내보내기 진행 중..."}</span>}
          </div>
        </div>
      )}
      <input
        ref={bulkUploadInputRef}
        className="hidden"
        type="file"
        accept=".csv,text/csv"
        onChange={onBulkFileSelected}
      />

      <section className="rounded-xl border border-slate-800 bg-[#0f172a] p-5 shadow-lg">
        <h2 className="mb-3 text-lg font-medium text-slate-200">조회 조건</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-6">
          <input
            className="rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            placeholder="업체명"
            value={filterDraft.clientName}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, clientName: e.target.value }))}
            onKeyDown={(e) => e.key === "Enter" && onSearch()}
          />
          <select
            className="rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            value={filterDraft.status}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, status: e.target.value as WorkStatus | "" }))}
          >
            <option value="">상태 전체</option>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <input
            className="rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            placeholder="담당자"
            value={filterDraft.assignee}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, assignee: e.target.value }))}
            onKeyDown={(e) => e.key === "Enter" && onSearch()}
          />
          <div className="flex w-full items-center gap-2 md:col-span-2">
            <DatePicker
              selected={filterDraft.dueDateFrom}
              onChange={(date: Date | null) => setFilterDraft((prev) => ({ ...prev, dueDateFrom: date }))}
              selectsStart
              startDate={filterDraft.dueDateFrom}
              endDate={filterDraft.dueDateTo}
              dateFormat="yyyy-MM-dd"
              className="w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
              placeholderText="시작일"
            />
            <span className="text-slate-400">~</span>
            <DatePicker
              selected={filterDraft.dueDateTo}
              onChange={(date: Date | null) => setFilterDraft((prev) => ({ ...prev, dueDateTo: date }))}
              selectsEnd
              startDate={filterDraft.dueDateFrom}
              endDate={filterDraft.dueDateTo}
              minDate={filterDraft.dueDateFrom || undefined}
              dateFormat="yyyy-MM-dd"
              className="w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
              placeholderText="종료일"
            />
          </div>
          <button
            className="flex h-[42px] w-12 shrink-0 items-center justify-center gap-2 rounded bg-sky-500 font-semibold text-slate-950 transition active:scale-[0.98] active:brightness-90 disabled:opacity-70 disabled:cursor-not-allowed"
            onClick={onSearch}
            type="button"
            title="조회"
            disabled={workItemsQuery.isFetching}
          >
            {workItemsQuery.isFetching ? <Loader2 className="animate-spin" size={18} /> : <Search size={18} />}
          </button>
        </div>
        {filterError && <p className="mt-2 text-sm text-rose-400">{filterError}</p>}
      </section>

      <section className="rounded-xl border border-slate-800 bg-[#0f172a] p-5 shadow-lg relative z-0">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3 relative z-10">
          <h2 className="text-lg font-medium text-slate-200">작업 현황 (Work Items)</h2>
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-md border border-amber-500/40 bg-amber-900/30 px-2 py-1 text-xs font-medium text-amber-100">
              보류(HOLD) 건수 {(tracking?.pendingApprovalCount ?? 0).toLocaleString()}
            </span>
            <span className="rounded-md border border-cyan-500/40 bg-cyan-900/30 px-2 py-1 text-xs font-medium text-cyan-100">
              Bulk 진행중 {(tracking?.bulkAsyncCount ?? 0).toLocaleString()}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button
              className="group flex h-9 w-9 items-center justify-center rounded border border-sky-500/50 bg-sky-900/40 text-sky-100 transition hover:bg-sky-900/60 active:scale-[0.98]"
              type="button"
              title="업무 추가"
              onClick={() => {
                resetWork({
                  clientId: undefined,
                  type: "FILING",
                  status: "TODO",
                  assignee: "kim",
                  dueDate: toIsoDate(today),
                  memo: "",
                });
                setSelectedClientName("");
                setIsCreateModalOpen(true);
              }}
            >
              <Plus size={18} />
            </button>
            <button
              className="group flex h-9 w-9 items-center justify-center rounded border border-rose-500/50 bg-rose-900/40 text-rose-100 transition hover:bg-rose-900/60 active:scale-[0.98]"
              type="button"
              title="선택 삭제"
              onClick={onDeleteRows}
            >
              <Trash2 size={18} />
            </button>
            <button
              className="group flex h-9 w-9 items-center justify-center rounded border border-emerald-500/50 bg-emerald-900/40 text-emerald-100 transition hover:bg-emerald-900/60 active:scale-[0.98] disabled:opacity-50 disabled:hover:bg-emerald-900/40"
              type="button"
              title="변경사항 저장"
              onClick={onSaveChanges}
              disabled={saveMutation.isPending}
            >
              <Save size={18} />
            </button>
            <button
              className="group flex h-9 w-9 items-center justify-center rounded border border-amber-500/50 bg-amber-900/40 text-amber-100 transition hover:bg-amber-900/60 active:scale-[0.98] disabled:opacity-50"
              type="button"
              title="실행 취소"
              onClick={onUndo}
              disabled={undoStack.length === 0 || saveMutation.isPending}
            >
              <RotateCcw size={18} />
            </button>

            <div className="relative z-[1100]" ref={actionMenuRef}>
              <button
                className="group flex h-9 w-9 items-center justify-center rounded border border-slate-500/50 bg-slate-800/80 text-slate-100 transition hover:bg-slate-700 active:scale-[0.98]"
                type="button"
                title="기타 옵션"
                onClick={() => setIsActionMenuOpen((prev) => !prev)}
              >
                <MoreVertical size={18} />
              </button>
            </div>
          </div>
        </div>

        <div className="mb-3 flex items-center justify-between gap-3 text-sm text-slate-400">
          <p>대량등록: {bulkJobId ?? "-"} {bulkJobStatus !== "IDLE" ? `(${bulkJobStatus})` : ""}</p>
          <p>내보내기: {exportJobId ?? "-"} {exportJobStatus !== "IDLE" ? `(${exportJobStatus})` : ""}</p>
        </div>

        <div className="mb-3 rounded border border-slate-800 bg-slate-900/40 p-3 text-xs text-slate-300">
          <div className="flex flex-wrap items-center gap-4">
            <label className="inline-flex items-center gap-1 cursor-pointer">
              <input
                type="checkbox"
                checked={copyWithHeader}
                onChange={(e) => setCopyWithHeader(e.target.checked)}
              />
              <span>복사 시 헤더 포함</span>
            </label>
            {trackingQuery.isFetching && (
              <span className="inline-flex items-center gap-1.5 text-slate-400">
                <Loader2 className="animate-spin" size={14} />
                Sync
              </span>
            )}
          </div>
        </div>

        <CustomExcelGrid
          data={gridRows}
          columns={columns}
          selectedIds={selectedIds}
          deletedIds={deletedIds}
          onSelectionChange={setSelectedIds}
          onCellValueChanged={onCellValueChanged}
          onHistoryClick={(row) => setHistoryModalWorkItem(row)}
          onDeleteClick={(row) => markRowsDeleted([row.id])}
          dirtyMap={dirtyMap}
          fetchNextPage={workItemsQuery.fetchNextPage}
          hasNextPage={workItemsQuery.hasNextPage}
          isFetchingNextPage={workItemsQuery.isFetchingNextPage}
          copyWithHeader={copyWithHeader}
        />
        {workItemsQuery.isError && (
          <p className="mt-3 text-sm text-rose-600">업무 목록 조회에 실패했습니다. 잠시 후 다시 시도해주세요.</p>
        )}
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          <div className="text-sm text-slate-600">
            총 {totalElements.toLocaleString()}건 중 {gridRows.length}건 로드됨
            {workItemsQuery.isFetchingNextPage && <span className="ml-2 text-sky-500 animate-pulse">데이터 불러오는 중...</span>}
          </div>
        </div>
      </section>

      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
          <div className="w-full max-w-3xl rounded-xl border border-slate-700 bg-slate-900 p-5 shadow-2xl">
            <div className="mb-4 flex items-center justify-between gap-3">
              <h3 className="text-lg font-semibold text-slate-100">업무 생성</h3>
              <button
                className="inline-flex h-8 w-8 items-center justify-center rounded border border-slate-600 text-slate-200 hover:bg-slate-800"
                type="button"
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setSelectedClientName("");
                  setErrorMessage(null);
                }}
                title="닫기"
                aria-label="닫기"
              >
                <X size={16} />
              </button>
            </div>

                        <form
              className="grid grid-cols-1 gap-4 md:grid-cols-2"
              onSubmit={handleSubmitWork((values) => {
                setErrorMessage(null);
                createMutation.mutate(values as CreateForm);
              })}
            >
              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-sm text-slate-400">고객사</label>
                <input type="hidden" {...registerWork("clientId")} />
                <button
                  type="button"
                  onClick={() => setIsClientSearchModalOpen(true)}
                  className="w-full text-left rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition hover:border-sky-500 focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                >
                  {selectedClientName || "고객사 선택 (클릭)"}
                </button>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">업무 유형</label>
                <select
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                  {...registerWork("type")}
                >
                  <option value="FILING">FILING</option>
                  <option value="BOOKKEEPING">BOOKKEEPING</option>
                  <option value="REVIEW">REVIEW</option>
                  <option value="ETC">ETC</option>
                </select>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">상태</label>
                <select
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                  {...registerWork("status")}
                >
                  {statuses.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">담당자</label>
                <input
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                  placeholder="예: kim"
                  {...registerWork("assignee")}
                />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">마감일</label>
                <input
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                  type="date"
                  {...registerWork("dueDate")}
                />
              </div>

              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-sm text-slate-400">메모</label>
                <input
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none transition focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                  placeholder="메모를 입력하세요"
                  {...registerWork("memo")}
                />
              </div>

              <div className="col-span-1 mt-4 flex items-center justify-end gap-3 border-t border-slate-800 pt-4 md:col-span-2">
                <button
                  className="rounded px-4 py-2 text-sm text-slate-300 transition hover:text-slate-100"
                  type="button"
                  onClick={() => {
                    setIsCreateModalOpen(false);
                    setSelectedClientName("");
                    setErrorMessage(null);
                  }}
                >
                  취소
                </button>
                <button
                  className="rounded border border-sky-500/50 bg-sky-600 px-6 py-2 text-sm font-medium text-white transition hover:bg-sky-500 disabled:opacity-60"
                  type="submit"
                  disabled={createMutation.isPending}
                >
                  {createMutation.isPending ? "저장 중..." : "저장"}
                </button>
              </div>

              {(errorMessage || createErrors.clientId || createErrors.assignee || createErrors.dueDate) && (
                <div className="md:col-span-2 flex flex-wrap items-center gap-3 text-sm text-rose-400">
                  {errorMessage && <span>{errorMessage}</span>}
                  {createErrors.clientId && <span>{createErrors.clientId.message}</span>}
                  {createErrors.assignee && <span>{createErrors.assignee.message ?? "담당자를 입력해주세요."}</span>}
                  {createErrors.dueDate && <span>{createErrors.dueDate.message ?? "마감일을 입력해주세요."}</span>}
                </div>
              )}
            </form>
          </div>
        </div>
      )}

      <ClientSearchModal
        isOpen={isClientSearchModalOpen}
        onClose={() => setIsClientSearchModalOpen(false)}
        onSelect={(client) => {
          setSelectedClientName(client.name);
          setWorkValue("clientId", client.id);
          setIsClientSearchModalOpen(false);
        }}
      />

      <AuditHistoryModal
        workItem={historyModalWorkItem}
        isOpen={!!historyModalWorkItem}
        onClose={() => setHistoryModalWorkItem(null)}
      />
      {isActionMenuOpen && actionMenuPosition && createPortal(
        <div
          ref={actionMenuPortalRef}
          className="fixed z-[9999] min-w-52 -translate-x-full rounded-lg border border-slate-700 bg-slate-900 p-1 shadow-2xl"
          style={{ top: actionMenuPosition.top, left: actionMenuPosition.left }}
        >
          <button
            className="block w-full rounded px-3 py-2 text-left text-sm text-slate-200 hover:bg-slate-800"
            type="button"
            onClick={onDownloadBulkTemplate}
          >
            템플릿 다운로드
          </button>
          <button
            className="block w-full rounded px-3 py-2 text-left text-sm text-slate-200 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            onClick={onClickBulkUpload}
            disabled={isBulkUploading || isCsvDownloading}
          >
            파일 업로드 (CSV)
          </button>
          <button
            className="block w-full rounded px-3 py-2 text-left text-sm text-slate-200 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            onClick={() => void onDownloadCsvAllByFilters()}
            disabled={isBulkUploading || isCsvDownloading}
          >
            CSV 다운로드 (전체 조회조건)
          </button>
          <button
            className="block w-full rounded px-3 py-2 text-left text-sm text-slate-200 hover:bg-slate-800"
            type="button"
            onClick={onDownloadCsvSelected}
          >
            CSV 다운로드 (선택 항목)
          </button>
        </div>,
        document.body
      )}
    </div>
  );
}



















