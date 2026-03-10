import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAuditLogs } from "./api";
import { X, History } from "lucide-react";
import type { WorkItem } from "./types";

interface Props {
  workItem: WorkItem | null;
  isOpen: boolean;
  onClose: () => void;
}

export function AuditHistoryModal({ workItem, isOpen, onClose }: Props) {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["audit-logs", workItem?.id, page],
    queryFn: () => fetchAuditLogs(workItem!.id, page, 20),
    enabled: isOpen && !!workItem,
    placeholderData: (prev) => prev,
  });

  if (!isOpen || !workItem) return null;

  const logs = data?.data ?? [];
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-[var(--bg-app)]/80 p-4 backdrop-blur-sm">
      <div className="flex h-full max-h-[600px] w-full max-w-3xl flex-col rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--border-main)]/50 p-4">
          <div className="flex items-center gap-2">
            <History className="text-sky-400" size={20} />
            <h3 className="text-lg font-semibold text-[var(--text-primary)]">
              업무 이력 (ID: {workItem.id})
            </h3>
          </div>
          <button className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        {/* List */}
        <div className="flex-1 overflow-auto p-4">
          {isLoading && <div className="py-8 text-center text-[var(--text-secondary)]">이력 불러오는 중...</div>}
          {!isLoading && logs.length === 0 && (
            <div className="py-8 text-center text-[var(--text-secondary)]">변경 이력이 없습니다.</div>
          )}
          
          <div className="relative border-l border-[var(--border-main)]/50 ml-3">
            {logs.map((log) => (
              <div key={log.auditId || log.id} className="mb-6 ml-6 relative">
                {/* Timeline dot */}
                <div className="absolute -left-[31px] top-1 h-3 w-3 rounded-full border-2 border-[var(--bg-card)] bg-sky-500"></div>
                
                <div className="rounded-lg border border-[var(--border-main)]/50 bg-[var(--bg-app)] p-3 shadow-sm">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold text-[var(--text-secondary)] bg-[var(--bg-hover)] px-2 py-0.5 rounded">
                      {log.field.toUpperCase()}
                    </span>
                    <span className="text-xs text-[var(--text-secondary)]/70">{new Date(log.changedAt).toLocaleString()}</span>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4 text-sm mt-2 bg-[var(--bg-card)]/50 p-2 rounded border border-[var(--border-main)]/30">
                    <div className="flex flex-col">
                      <span className="text-xs text-[var(--text-secondary)]/70 mb-1">변경 전</span>
                      <span className="text-rose-400 line-through decoration-rose-900/50">{log.before || log.beforeValue || "-"}</span>
                    </div>
                    <div className="flex flex-col">
                      <span className="text-xs text-[var(--text-secondary)]/70 mb-1">변경 후</span>
                      <span className="text-emerald-400">{log.after || log.afterValue || "-"}</span>
                    </div>
                  </div>
                  
                  <div className="mt-2 text-right text-xs text-[var(--text-secondary)]/70">
                    수정자: <span className="text-[var(--text-secondary)]">{log.changedBy}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 border-t border-[var(--border-main)]/50 p-4">
            <button
              className="rounded border border-[var(--border-main)] bg-[var(--bg-app)] px-3 py-1 text-sm text-[var(--text-primary)] disabled:opacity-50"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              이전
            </button>
            <span className="text-sm text-[var(--text-secondary)]">
              {page + 1} / {totalPages}
            </span>
            <button
              className="rounded border border-[var(--border-main)] bg-[var(--bg-app)] px-3 py-1 text-sm text-[var(--text-primary)] disabled:opacity-50"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              다음
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
