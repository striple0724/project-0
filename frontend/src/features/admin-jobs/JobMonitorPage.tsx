import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Search } from "lucide-react";
import { fetchAdminJobs } from "./api";
import type { JobStatus, JobType } from "./types";

type JobFilterDraft = {
  requestId: string;
  jobType: JobType | "";
  status: JobStatus | "";
};

export function JobMonitorPage() {
  const [filters, setFilters] = useState<JobFilterDraft>({
    requestId: "",
    jobType: "",
    status: "",
  });
  const [filterDraft, setFilterDraft] = useState<JobFilterDraft>({
    requestId: "",
    jobType: "",
    status: "",
  });
  const [page, setPage] = useState(0);
  const [searchTick, setSearchTick] = useState(0);
  const size = 20;

  const jobsQuery = useQuery({
    queryKey: ["admin-jobs", filters.requestId, filters.jobType, filters.status, page, size, searchTick],
    queryFn: () =>
      fetchAdminJobs({
        requestId: filters.requestId,
        jobType: filters.jobType,
        status: filters.status,
        page,
        size,
      }),
    refetchInterval: 5000,
  });

  const pageInfo = jobsQuery.data?.page;
  const rows = useMemo(() => jobsQuery.data?.data ?? [], [jobsQuery.data]);
  const onSearch = () => {
    setFilters({
      requestId: filterDraft.requestId.trim(),
      jobType: filterDraft.jobType,
      status: filterDraft.status,
    });
    setPage(0);
    setSearchTick((prev) => prev + 1);
  };

  return (
    <div className="workbench-shell mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8">
      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h1 className="text-2xl font-semibold">작업 모니터링</h1>
        <p className="mt-2 text-sm text-slate-600">Job 목록과 처리 상태를 조회합니다.</p>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-lg font-medium">Job 목록</h2>
        <div className="mb-4 grid grid-cols-1 gap-2 md:grid-cols-5">
          <input
            className="rounded border border-slate-300 px-3 py-2"
            placeholder="requestId 검색"
            value={filterDraft.requestId}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, requestId: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === "Enter") onSearch();
            }}
          />
          <select
            className="rounded border border-slate-300 px-3 py-2"
            value={filterDraft.jobType}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, jobType: e.target.value as JobType | "" }))}
          >
            <option value="">작업유형 전체</option>
            <option value="BULK_INSERT">BULK_INSERT</option>
            <option value="EXPORT">EXPORT</option>
          </select>
          <select
            className="rounded border border-slate-300 px-3 py-2"
            value={filterDraft.status}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, status: e.target.value as JobStatus | "" }))}
          >
            <option value="">상태 전체</option>
            <option value="QUEUED">QUEUED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="DONE">DONE</option>
            <option value="FAILED">FAILED</option>
          </select>
          <button
            type="button"
            className="flex h-[42px] w-12 shrink-0 items-center justify-center gap-2 rounded bg-sky-500 font-semibold text-slate-950 transition active:scale-[0.98] active:brightness-90 disabled:opacity-70 disabled:cursor-not-allowed"
            onClick={onSearch}
            title="조회"
            disabled={jobsQuery.isFetching}
          >
            {jobsQuery.isFetching ? <Loader2 className="animate-spin" size={18} /> : <Search size={18} />}
          </button>
        </div>

        <div className="overflow-auto rounded border border-slate-200">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-left">
              <tr>
                <th className="px-3 py-2">Job ID</th>
                <th className="px-3 py-2">Request ID</th>
                <th className="px-3 py-2">Type</th>
                <th className="px-3 py-2">Status</th>
                <th className="px-3 py-2">Progress</th>
                <th className="px-3 py-2">Download</th>
                <th className="px-3 py-2">Error</th>
                <th className="px-3 py-2">Created</th>
                <th className="px-3 py-2">Updated</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.jobId} className="border-t border-slate-100">
                  <td className="px-3 py-2 font-mono text-xs">{row.jobId}</td>
                  <td className="px-3 py-2 font-mono text-xs">{row.requestId}</td>
                  <td className="px-3 py-2">{row.jobType}</td>
                  <td className="px-3 py-2">{row.status}</td>
                  <td className="px-3 py-2">{row.progressPercent}%</td>
                  <td className="px-3 py-2">
                    {row.downloadUrl ? (
                      <a className="text-blue-600 hover:underline" href={row.downloadUrl} target="_blank" rel="noreferrer">
                        다운로드
                      </a>
                    ) : (
                      "-"
                    )}
                  </td>
                  <td className="px-3 py-2 text-rose-700">{row.errorMessage ?? "-"}</td>
                  <td className="px-3 py-2">{formatDateTime(row.createdAt)}</td>
                  <td className="px-3 py-2">{formatDateTime(row.updatedAt)}</td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td className="px-3 py-6 text-center text-slate-500" colSpan={9}>
                    조회된 작업이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex items-center justify-between">
          <p className="text-sm text-slate-600">
            총 {pageInfo?.totalElements?.toLocaleString() ?? 0}건
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              이전
            </button>
            <span className="text-sm text-slate-700">
              {page + 1} / {Math.max(1, pageInfo?.totalPages ?? 1)}
            </span>
            <button
              type="button"
              className="rounded border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40"
              disabled={pageInfo == null || page + 1 >= pageInfo.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}
