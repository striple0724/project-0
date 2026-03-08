import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AgGridReact } from "ag-grid-react";
import type { ColDef } from "ag-grid-community";
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";
import {
  createWorkItem,
  fetchExportJobStatus,
  fetchWorkItems,
  submitBulkJob,
  submitExportJob,
  type WorkItemFilters,
} from "./api";
import type { WorkItem, WorkStatus } from "./types";
import { useSessionStore } from "../../auth/session-store";
import { localLogout } from "../../auth/local-auth-api";
import { UserAdminPanel } from "../users/UserAdminPanel";
import { MenuAdminPanel } from "../menus/MenuAdminPanel";

const statuses: WorkStatus[] = ["TODO", "IN_PROGRESS", "DONE", "HOLD"];

const createSchema = z.object({
  clientId: z.coerce.number().int().positive(),
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

export function WorkbenchPage({ embedded = false }: Props) {
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const clearSession = useSessionStore((s) => s.clearSession);
  const loginUserName = useSessionStore((s) => s.user?.name ?? "admin");
  const [filters, setFilters] = useState<WorkItemFilters>({ page: 0, size: 50 });
  const [lastJobId, setLastJobId] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { register, handleSubmit, reset } = useForm<CreateFormInput>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      clientId: 11,
      type: "FILING",
      status: "TODO",
      assignee: "kim",
      dueDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10),
      memo: "",
    },
  });

  const workItemsQuery = useQuery({
    queryKey: ["work-items", filters],
    queryFn: () => fetchWorkItems(filters),
  });

  const exportStatusQuery = useQuery({
    queryKey: ["export-job", lastJobId],
    queryFn: () => fetchExportJobStatus(lastJobId as string),
    enabled: !!lastJobId,
    refetchInterval: (query) => {
      const status = query.state.data?.data.status;
      return status === "DONE" || status === "FAILED" ? false : 1500;
    },
  });

  const createMutation = useMutation({
    mutationFn: createWorkItem,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["work-items"] });
      reset();
    },
  });

  const bulkMutation = useMutation({
    mutationFn: () => submitBulkJob(`bulk-${Date.now()}`),
  });

  const exportMutation = useMutation({
    mutationFn: () => submitExportJob(filters),
    onSuccess: (data) => setLastJobId(data.data.jobId),
  });

  const columns = useMemo<ColDef<WorkItem>[]>(
    () => [
      { field: "id", headerName: "ID", width: 90 },
      { field: "clientName", headerName: "업체명", flex: 1.2 },
      { field: "status", headerName: "상태", width: 130 },
      { field: "assignee", headerName: "담당자", width: 130 },
      { field: "dueDate", headerName: "마감일", width: 130 },
      { field: "updatedAt", headerName: "수정시각", flex: 1.3 },
    ],
    []
  );

  return (
    <div className="mx-auto flex max-w-7xl flex-col gap-6 p-6">
      {!embedded && (
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Tax Workbench</h1>
          <div className="flex items-center gap-2">
            {authMode === "local" ? (
              <button
                className="rounded border border-slate-300 px-3 py-1 text-sm"
                type="button"
                onClick={() => {
                  void localLogout()
                    .catch(() => undefined)
                    .finally(() => {
                      clearSession();
                      window.location.href = "/login";
                    });
                }}
              >
                로그아웃(Local)
              </button>
            ) : (
              <button
                className="rounded border border-slate-300 px-3 py-1 text-sm"
                type="button"
                onClick={() => {
                  clearSession();
                  window.location.href = "/login";
                }}
              >
                로그아웃(Mock)
              </button>
            )}
          </div>
        </div>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-lg font-medium">조회 조건</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            className="rounded border border-slate-300 px-3 py-2"
            placeholder="업체명"
            onChange={(e) => setFilters((f) => ({ ...f, clientName: e.target.value, page: 0 }))}
          />
          <select
            className="rounded border border-slate-300 px-3 py-2"
            onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value as WorkStatus | "", page: 0 }))}
            defaultValue=""
          >
            <option value="">상태 전체</option>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <input
            className="rounded border border-slate-300 px-3 py-2"
            placeholder="담당자"
            onChange={(e) => setFilters((f) => ({ ...f, assignee: e.target.value, page: 0 }))}
          />
          <button
            className="rounded bg-slate-900 px-4 py-2 text-white"
            onClick={() => workItemsQuery.refetch()}
            type="button"
          >
            조회
          </button>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-lg font-medium">업무 생성 (폼 저장)</h2>
        <form
          className="grid grid-cols-1 gap-3 md:grid-cols-3"
          onSubmit={handleSubmit((values) => {
            const parsed: CreateForm = createSchema.parse(values);
            createMutation.mutate(parsed);
          })}
        >
          <input className="rounded border border-slate-300 px-3 py-2" placeholder="clientId" {...register("clientId")} />
          <select className="rounded border border-slate-300 px-3 py-2" {...register("type")}>
            <option value="FILING">FILING</option>
            <option value="BOOKKEEPING">BOOKKEEPING</option>
            <option value="REVIEW">REVIEW</option>
            <option value="ETC">ETC</option>
          </select>
          <select className="rounded border border-slate-300 px-3 py-2" {...register("status")}>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <input className="rounded border border-slate-300 px-3 py-2" placeholder="assignee" {...register("assignee")} />
          <input className="rounded border border-slate-300 px-3 py-2" type="date" {...register("dueDate")} />
          <input className="rounded border border-slate-300 px-3 py-2" placeholder="memo" {...register("memo")} />
          <button className="rounded bg-blue-600 px-4 py-2 text-white" type="submit">
            저장
          </button>
        </form>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center gap-3">
          <h2 className="text-lg font-medium">대량 처리</h2>
          <button className="rounded bg-emerald-600 px-3 py-2 text-white" type="button" onClick={() => bulkMutation.mutate()}>
            Bulk Job 제출
          </button>
          <button className="rounded bg-indigo-600 px-3 py-2 text-white" type="button" onClick={() => exportMutation.mutate()}>
            Export Job 제출
          </button>
        </div>
        <p className="text-sm text-slate-600">
          Bulk: {bulkMutation.data?.data.jobId ?? "-"} / Export: {exportMutation.data?.data.jobId ?? "-"}
        </p>
        <p className="text-sm text-slate-600">
          Export 상태: {exportStatusQuery.data?.data.status ?? "-"}
          {exportStatusQuery.data?.data.downloadUrl ? ` / URL: ${exportStatusQuery.data.data.downloadUrl}` : ""}
        </p>
      </section>

      <UserAdminPanel loginUserName={loginUserName} />
      <MenuAdminPanel loginUserName={loginUserName} />

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-lg font-medium">업무 그리드</h2>
        <div className="ag-theme-quartz" style={{ height: 420, width: "100%" }}>
          <AgGridReact<WorkItem>
            columnDefs={columns}
            rowData={workItemsQuery.data?.data ?? []}
            rowSelection={{ mode: "multiRow" }}
            animateRows
          />
        </div>
      </section>
    </div>
  );
}





