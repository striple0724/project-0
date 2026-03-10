export type WorkStatus = "TODO" | "IN_PROGRESS" | "DONE" | "HOLD";
export type WorkType = "FILING" | "BOOKKEEPING" | "REVIEW" | "ETC";

export interface WorkItem {
  id: number;
  clientId: number;
  clientName: string;
  bizNo?: string;
  type: WorkType;
  status: WorkStatus;
  assignee: string;
  dueDate: string;
  tags?: string[];
  memo?: string;
  version: number;
  updatedAt: string;
  hasAudit?: boolean;
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

export interface AuditLog {
  auditId?: number;
  id?: number;
  workItemId: number;
  field: string;
  before?: string;
  after?: string;
  beforeValue?: string;
  afterValue?: string;
  changedBy: string;
  changedAt: string;
}

export interface PagedAuditLogs {
  data: AuditLog[];
  page: PageInfo;
}

export interface JobAccepted {
  data: {
    requestId: string;
    jobId: string;
    jobType: "BULK_INSERT" | "EXPORT";
    status: "QUEUED" | "RUNNING" | "PARTIAL_SUCCESS" | "DONE" | "FAILED";
  };
}

export interface ExportJobStatus {
  data: {
    requestId: string;
    jobId: string;
    jobType: "BULK_INSERT" | "EXPORT";
    status: "QUEUED" | "RUNNING" | "PARTIAL_SUCCESS" | "DONE" | "FAILED";
    errorMessage?: string;
    downloadUrl?: string;
    expiresAt?: string;
  };
}

export interface WorkbenchTrackingJob {
  jobId: string;
  requestId: string;
  jobType: "BULK_INSERT" | "EXPORT";
  status: "QUEUED" | "RUNNING" | "PARTIAL_SUCCESS" | "DONE" | "FAILED";
  progressPercent: number;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkbenchTrackingResponse {
  serverInstanceKey?: string;
  data: {
    pendingApprovalCount: number;
    asyncJobCount: number;
    bulkAsyncCount: number;
    exportAsyncCount: number;
    recentJobs: WorkbenchTrackingJob[];
  };
}

export interface BulkCsvValidationResult {
  data: {
    totalRows: number;
    validRows: number;
    invalidRows: number;
    uniqueClientIdCount: number;
    missingClientIdCount: number;
    missingClientIds: number[];
    malformedRows: number;
  };
}
