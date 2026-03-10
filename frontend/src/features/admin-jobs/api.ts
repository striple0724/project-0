import { httpClient } from "../../api/http-client";
import type { JobMonitorPageResponse, JobStatus, JobType } from "./types";

export async function fetchAdminJobs(params: {
  requestId?: string;
  jobType?: JobType | "";
  status?: JobStatus | "";
  page?: number;
  size?: number;
}): Promise<JobMonitorPageResponse> {
  const response = await httpClient.get<JobMonitorPageResponse>("/api/v1/admin/jobs", {
    params: {
      requestId: params.requestId || undefined,
      jobType: params.jobType || undefined,
      status: params.status || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return response.data;
}
