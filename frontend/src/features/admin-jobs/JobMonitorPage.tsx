import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronUp, Loader2, Search, Square } from "lucide-react";
import { cancelAdminJob, fetchAdminJobs } from "./api";
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
  const [pendingCancelJobId, setPendingCancelJobId] = useState<string | null>(null);
  const [showScroll, setShowScroll] = useState(false);
  const size = 20;
  const queryClient = useQueryClient();

  useEffect(() => {
    const handleScroll = () => {
      setShowScroll(window.scrollY > 300);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

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
  const cancelMutation = useMutation({
    mutationFn: (jobId: string) => cancelAdminJob(jobId),
    onMutate: (jobId) => {
      setPendingCancelJobId(jobId);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-jobs"] });
    },
    onSettled: () => {
      setPendingCancelJobId(null);
    },
  });
  const onSearch = () => {
    setFilters({
      requestId: filterDraft.requestId.trim(),
      jobType: filterDraft.jobType,
      status: filterDraft.status,
    });
    setPage(0);
    setSearchTick((prev) => prev + 1);
  };

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <div className="workbench-shell relative mx-auto flex w-full max-w-[calc(100vw-96px)] flex-col gap-6 p-6 md:p-8 transition-colors duration-200">
      <section className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] p-4 shadow-sm">
        <h2 className="mb-3 text-lg font-medium text-[var(--text-primary)]">Job 목록</h2>
        <div className="mb-4 grid grid-cols-1 gap-2 md:grid-cols-5">
          <input
            className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
            placeholder="requestId 검색"
            value={filterDraft.requestId}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, requestId: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === "Enter") onSearch();
            }}
          />
          <select
            className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
            value={filterDraft.jobType}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, jobType: e.target.value as JobType | "" }))}
          >
            <option value="">작업유형 전체</option>
            <option value="BULK_INSERT">BULK_INSERT</option>
            <option value="EXPORT">EXPORT</option>
          </select>
          <select
            className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
            value={filterDraft.status}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, status: e.target.value as JobStatus | "" }))}
          >
            <option value="">상태 전체</option>
            <option value="QUEUED">QUEUED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="CANCEL_REQUESTED">CANCEL_REQUESTED</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="PARTIAL_SUCCESS">PARTIAL_SUCCESS</option>
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

        <div className="overflow-auto rounded border border-[var(--border-main)] bg-[var(--bg-app)]">
          <table className="w-full min-w-[1480px] table-fixed text-sm text-[var(--text-secondary)]">
            <thead className="bg-[var(--bg-hover)] text-left text-[var(--text-primary)]">
              <tr>
                <th className="w-[240px] px-3 py-2 border-r border-[var(--border-main)]/30">Job ID</th>
                <th className="w-[180px] px-3 py-2 border-r border-[var(--border-main)]/30">Request ID</th>
                <th className="w-[110px] px-3 py-2 border-r border-[var(--border-main)]/30">Type</th>
                <th className="w-[150px] px-3 py-2 border-r border-[var(--border-main)]/30">Status</th>
                <th className="w-[90px] px-3 py-2 border-r border-[var(--border-main)]/30">Progress</th>
                <th className="w-[110px] px-3 py-2 border-r border-[var(--border-main)]/30">Download</th>
                <th className="w-[360px] px-3 py-2 border-r border-[var(--border-main)]/30">Error</th>
                <th className="w-[130px] px-3 py-2 border-r border-[var(--border-main)]/30">Control</th>
                <th className="w-[160px] px-3 py-2 border-r border-[var(--border-main)]/30">Created</th>
                <th className="w-[160px] px-3 py-2">Updated</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.jobId} className="border-t border-[var(--border-main)]/30 hover:bg-[var(--bg-hover)] transition-colors">
                  <td className="px-3 py-2 font-mono text-xs break-all border-r border-[var(--border-main)]/30">{row.jobId}</td>
                  <td className="px-3 py-2 font-mono text-xs break-all border-r border-[var(--border-main)]/30">{row.requestId}</td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{row.jobType}</td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{row.status}</td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{row.progressPercent}%</td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">
                    {row.downloadUrl ? (
                      <a className="text-sky-400 hover:underline" href={row.downloadUrl} target="_blank" rel="noreferrer">
                        다운로드
                      </a>
                    ) : (
                      "-"
                    )}
                  </td>
                  <td className="px-3 py-2 text-rose-400 break-words border-r border-[var(--border-main)]/30">{toJobErrorLabel(row.errorMessage)}</td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">
                    {canCancel(row.status) ? (
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 rounded border border-rose-500/50 bg-rose-900/30 px-2.5 py-1.5 text-xs font-medium text-rose-200 hover:bg-rose-900/50 disabled:cursor-not-allowed disabled:opacity-50"
                        onClick={() => cancelMutation.mutate(row.jobId)}
                        disabled={pendingCancelJobId === row.jobId}
                      >
                        {pendingCancelJobId === row.jobId ? <Loader2 className="animate-spin" size={14} /> : <Square size={14} />}
                        {pendingCancelJobId === row.jobId ? "정지 요청 중" : "정지"}
                      </button>
                    ) : (
                      <span className="text-[var(--text-secondary)]/50">-</span>
                    )}
                  </td>
                  <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{formatDateTime(row.createdAt)}</td>
                  <td className="px-3 py-2">{formatDateTime(row.updatedAt)}</td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td className="px-3 py-6 text-center text-[var(--text-secondary)]" colSpan={10}>
                    조회된 작업이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex items-center justify-between text-[var(--text-secondary)]">
          <p className="text-sm">
            총 {pageInfo?.totalElements?.toLocaleString() ?? 0}건
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded border border-[var(--border-main)] bg-[var(--bg-app)] px-3 py-1.5 text-sm hover:bg-[var(--bg-hover)] disabled:opacity-40"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              이전
            </button>
            <span className="text-sm">
              {page + 1} / {Math.max(1, pageInfo?.totalPages ?? 1)}
            </span>
            <button
              type="button"
              className="rounded border border-[var(--border-main)] bg-[var(--bg-app)] px-3 py-1.5 text-sm hover:bg-[var(--bg-hover)] disabled:opacity-40"
              disabled={pageInfo == null || page + 1 >= pageInfo.totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </div>
      </section>

      {showScroll && (
        <button
          className="fixed bottom-8 right-8 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-sky-600 text-white shadow-lg transition hover:bg-sky-500 hover:-translate-y-1"
          onClick={scrollToTop}
          title="위로 가기"
        >
          <ChevronUp size={24} />
        </button>
      )}
    </div>
  );
}

function canCancel(status: JobStatus) {
  return status === "QUEUED" || status === "RUNNING" || status === "CANCEL_REQUESTED";
}

function toJobErrorLabel(errorMessage?: string) {
  if (!errorMessage) return "-";
  const code = errorMessage.trim();
  if (code === "JOB_CANCEL_REQUESTED") return "정지 요청이 접수되었습니다.";
  if (code === "JOB_CANCELLED") return "사용자 요청으로 작업이 중단되었습니다.";
  if (code === "EXPORT_EXECUTOR_REJECTED") return "내보내기 처리 큐가 가득 찼습니다.";
  if (code === "BULK_EXECUTOR_REJECTED") return "대량등록 처리 큐가 가득 찼습니다.";
  if (code.startsWith("JOB_INTERRUPTED_BY_SERVER_RESTART:QUEUED")) {
    return "서버 재시작으로 대기 중 작업이 중단되었습니다.";
  }
  if (code.startsWith("JOB_INTERRUPTED_BY_SERVER_RESTART:RUNNING")) {
    return "서버 재시작으로 진행 중 작업이 중단되었습니다.";
  }
  return errorMessage;
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}
