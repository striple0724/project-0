import { httpClient } from "../../api/http-client";
import type { ExportJobStatus, JobAccepted, PagedWorkItems, WorkStatus, WorkType } from "./types";

export interface WorkItemFilters {
  clientName?: string;
  status?: WorkStatus | "";
  assignee?: string;
  page?: number;
  size?: number;
}

export async function fetchWorkItems(filters: WorkItemFilters): Promise<PagedWorkItems> {
  const response = await httpClient.get<PagedWorkItems>("/api/v1/work-items", {
    params: {
      clientName: filters.clientName || undefined,
      status: filters.status || undefined,
      assignee: filters.assignee || undefined,
      page: filters.page ?? 0,
      size: filters.size ?? 50,
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

export async function submitBulkJob(requestId: string): Promise<JobAccepted> {
  const response = await httpClient.post<JobAccepted>("/api/v1/work-items:bulk", {
    requestId,
    items: [
      {
        clientId: 11,
        type: "FILING",
        assignee: "kim",
        dueDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10),
      },
    ],
  });
  return response.data;
}

export async function submitExportJob(filters: WorkItemFilters): Promise<JobAccepted> {
  const response = await httpClient.post<JobAccepted>("/api/v1/exports", {
    clientName: filters.clientName || undefined,
    status: filters.status || undefined,
    assignee: filters.assignee || undefined,
  });
  return response.data;
}

export async function fetchExportJobStatus(jobId: string): Promise<ExportJobStatus> {
  const response = await httpClient.get<ExportJobStatus>(`/api/v1/exports/${jobId}`);
  return response.data;
}
