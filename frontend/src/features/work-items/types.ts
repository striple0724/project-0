export type WorkStatus = "TODO" | "IN_PROGRESS" | "DONE" | "HOLD";
export type WorkType = "FILING" | "BOOKKEEPING" | "REVIEW" | "ETC";

export interface WorkItem {
  id: number;
  clientId: number;
  clientName: string;
  type: WorkType;
  status: WorkStatus;
  assignee: string;
  dueDate: string;
  memo?: string;
  version: number;
  updatedAt: string;
}

export interface PageInfo {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PagedWorkItems {
  data: WorkItem[];
  page: PageInfo;
}

export interface JobAccepted {
  data: {
    requestId: string;
    jobId: string;
    jobType: "BULK_INSERT" | "EXPORT";
    status: "QUEUED" | "RUNNING" | "DONE" | "FAILED";
  };
}

export interface ExportJobStatus {
  data: {
    requestId: string;
    jobId: string;
    jobType: "BULK_INSERT" | "EXPORT";
    status: "QUEUED" | "RUNNING" | "DONE" | "FAILED";
    downloadUrl?: string;
    expiresAt?: string;
  };
}
