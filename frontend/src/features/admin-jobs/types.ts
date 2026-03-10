export type JobType = "BULK_INSERT" | "EXPORT";
export type JobStatus = "QUEUED" | "RUNNING" | "CANCEL_REQUESTED" | "CANCELLED" | "DONE" | "FAILED";

export interface JobMonitorItem {
  jobId: string;
  requestId: string;
  jobType: JobType;
  status: JobStatus;
  progressPercent: number;
  downloadUrl?: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  expiresAt?: string;
}

export interface JobMonitorPageResponse {
  data: JobMonitorItem[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}
