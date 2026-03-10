import { httpClient } from "../../api/http-client";
import axios from "axios";
import type {
  ExportJobStatus,
  JobAccepted,
  PagedAuditLogs,
  PagedWorkItems,
  WorkStatus,
  WorkType,
  WorkbenchTrackingResponse,
} from "./types";

export interface WorkItemFilters {
  clientName?: string;
  status?: WorkStatus | "";
  assignee?: string;
  dueDateFrom?: string;
  dueDateTo?: string;
  page?: number;
  size?: number;
  includeTotal?: boolean;
  sort?: string[];
}

export async function fetchWorkItems(filters: WorkItemFilters): Promise<PagedWorkItems> {
  const response = await httpClient.get<PagedWorkItems>("/api/v1/work-items", {
    params: {
      clientName: filters.clientName || undefined,
      status: filters.status || undefined,
      assignee: filters.assignee || undefined,
      dueDateFrom: filters.dueDateFrom || undefined,
      dueDateTo: filters.dueDateTo || undefined,
      page: filters.page ?? 0,
      size: filters.size ?? 50,
      includeTotal: filters.includeTotal ?? true,
      sort: filters.sort ?? ["updatedAt,desc"],
    },
  });
  return response.data;
}

export async function createWorkItem(payload: {
  clientId: number;
  type: WorkType;
  status: WorkStatus;
  assignee: string;
  dueDate: string;
  memo?: string;
}) {
  await httpClient.post("/api/v1/work-items", payload);
}

export async function createClient(payload: {
  name: string;
  bizNo?: string;
  type: "INDIVIDUAL" | "CORPORATE";
  status: "ACTIVE" | "INACTIVE";
  tier: "BASIC" | "PREMIUM" | "VIP";
}) {
  const response = await httpClient.post<{ data: { id: number } }>("/api/v1/clients", payload);
  return response.data.data;
}

export async function submitBulkJob(
  requestId: string,
  items: Array<{
    clientId: number;
    type: WorkType;
    assignee: string;
    dueDate: string;
    tags?: string[];
    memo?: string;
  }>
): Promise<JobAccepted> {
  const response = await httpClient.post<JobAccepted>("/api/v1/work-items/bulk", {
    requestId,
    items,
  }, {
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
    },
  });
  return response.data;
}

export async function submitBulkCsvFile(file: File, requestId?: string): Promise<JobAccepted> {
  const formData = new FormData();
  formData.append("file", file);
  if (requestId && requestId.trim().length > 0) {
    formData.append("requestId", requestId.trim());
  }

  try {
    const response = await httpClient.post<JobAccepted>("/api/v1/work-items/bulk-file", formData, {
      timeout: 0,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data as { detail?: string; code?: string } | undefined;
      const code = data?.code;
      if (code === "CSV_EMPTY_FILE") {
        throw new Error("업로드한 CSV 파일이 비어 있습니다.");
      }
      if (code === "CSV_INVALID_HEADER") {
        throw new Error("CSV 헤더가 올바르지 않습니다. 템플릿 헤더(requestId,clientId,type,assignee,dueDate,tags,memo)를 사용해주세요.");
      }
      if (code === "UPLOAD_SIZE_LIMIT_EXCEEDED" || status === 413) {
        throw new Error("업로드 파일 용량이 서버 제한을 초과했습니다. 파일을 분할하거나 제한값을 확인해주세요.");
      }
      if (code === "BULK_FILE_UPLOAD_FAILED") {
        throw new Error("CSV 임시 저장 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
      }
      if (code === "MULTIPART_REQUEST_INVALID") {
        throw new Error("파일 업로드 요청 형식이 올바르지 않습니다. 브라우저를 새로고침 후 다시 시도해주세요.");
      }
      if (code === "MULTIPART_PART_MISSING") {
        throw new Error("업로드 파일이 전송되지 않았습니다. 파일을 다시 선택 후 시도해주세요.");
      }
      if (data?.detail) {
        throw new Error(data.detail);
      }
    }
    throw error;
  }
}

export async function submitExportJob(filters: WorkItemFilters): Promise<JobAccepted> {
  const response = await httpClient.post<JobAccepted>("/api/v1/exports", {
    clientName: filters.clientName || undefined,
    status: filters.status || undefined,
    assignee: filters.assignee || undefined,
    dueDateFrom: filters.dueDateFrom || undefined,
    dueDateTo: filters.dueDateTo || undefined,
  });
  return response.data;
}

export async function submitSelectedExportJob(workItemIds: number[]): Promise<JobAccepted> {
  const response = await httpClient.post<JobAccepted>("/api/v1/exports/selected", {
    workItemIds,
  });
  return response.data;
}

export async function exportWorkItemsCsv(filters: WorkItemFilters): Promise<Blob> {
  try {
    const response = await httpClient.get<Blob>("/api/v1/work-items:export", {
      params: {
        clientName: filters.clientName || undefined,
        status: filters.status || undefined,
        assignee: filters.assignee || undefined,
        dueDateFrom: filters.dueDateFrom || undefined,
        dueDateTo: filters.dueDateTo || undefined,
      },
      responseType: "blob",
      timeout: 0,
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const statusCode = error.response?.status;
      const raw = error.response?.data;
      if (raw instanceof Blob) {
        try {
          const text = await raw.text();
          const parsed = JSON.parse(text) as { detail?: string; message?: string };
          const detail = parsed.detail ?? parsed.message;
          if (detail) {
            throw new Error(detail);
          }
        } catch {
          // ignore parse errors
        }
      }
      if (statusCode === 413) {
        throw new Error("다운로드 용량 제한을 초과했습니다. 조회 조건을 좁히거나 관리자에게 제한 설정을 확인하세요.");
      }
    }
    throw error;
  }
}

export async function fetchExportJobStatus(jobId: string): Promise<ExportJobStatus> {
  const response = await httpClient.get<ExportJobStatus>(`/api/v1/exports/${jobId}`);
  return response.data;
}

export async function downloadExportCsvByUrl(downloadUrl: string): Promise<Blob> {
  const isAbsolute = /^https?:\/\//i.test(downloadUrl);
  const target = isAbsolute
    ? downloadUrl
    : downloadUrl.startsWith("/")
      ? downloadUrl
      : `/${downloadUrl}`;

  const response = await (isAbsolute ? axios : httpClient).get<Blob>(target, {
    responseType: "blob",
    timeout: 0,
    withCredentials: true,
  });
  return response.data;
}

export async function fetchWorkbenchTracking(): Promise<WorkbenchTrackingResponse> {
  const response = await httpClient.get<WorkbenchTrackingResponse>("/api/v1/workbench/tracking");
  const serverInstanceKeyHeader = response.headers["x-server-instance-key"];
  if (typeof serverInstanceKeyHeader === "string" && serverInstanceKeyHeader.trim().length > 0) {
    return { ...response.data, serverInstanceKey: serverInstanceKeyHeader };
  }
  return response.data;
}

export async function patchWorkItem(
  workItemId: number,
  version: number,
  payload: {
    status?: WorkStatus;
    type?: WorkType;
    dueDate?: string;
    assignee?: string;
    memo?: string;
    tags?: string[];
  }
) {
  const response = await httpClient.patch(`/api/v1/work-items/${workItemId}`, payload, {
    headers: { "If-Match": `"${version}"` },
  });
  return response.data;
}

export async function deleteWorkItem(workItemId: number) {
  await httpClient.delete(`/api/v1/work-items/${workItemId}`);
}

export async function fetchAuditLogs(workItemId: number, page = 0, size = 20): Promise<PagedAuditLogs> {
  const response = await httpClient.get<PagedAuditLogs>(`/api/v1/work-items/${workItemId}/audit-logs`, {
    params: { page, size },
  });
  return response.data;
}
