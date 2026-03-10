import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchClients } from "./api";
import { Search, X } from "lucide-react";
import type { Client } from "./types";

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (client: Client) => void;
}

export function ClientSearchModal({ isOpen, onClose, onSelect }: Props) {
  const [searchTerm, setSearchName] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["clients", "search", searchTerm, page],
    queryFn: () => fetchClients({ name: searchTerm || undefined, page, size: 10 }),
    enabled: isOpen,
    placeholderData: (prev) => prev,
  });

  if (!isOpen) return null;

  const clients = data?.data ?? [];
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-[var(--bg-app)]/80 p-4 backdrop-blur-sm">
      <div className="flex h-full max-h-[600px] w-full max-w-2xl flex-col rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--border-main)]/50 p-4">
          <h3 className="text-lg font-semibold text-[var(--text-primary)]">고객사 검색</h3>
          <button className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        {/* Search Input */}
        <div className="border-b border-[var(--border-main)]/50 p-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-secondary)]" size={18} />
            <input
              className="w-full rounded border border-[var(--border-main)] bg-[var(--bg-input)] py-2.5 pl-10 pr-4 text-[var(--text-primary)] outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
              placeholder="고객사명으로 검색"
              value={searchTerm}
              onChange={(e) => {
                setSearchName(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        {/* List */}
        <div className="flex-1 overflow-auto p-4">
          {isLoading && <div className="py-8 text-center text-[var(--text-secondary)]">검색 중...</div>}
          {!isLoading && clients.length === 0 && (
            <div className="py-8 text-center text-[var(--text-secondary)]">일치하는 고객사가 없습니다.</div>
          )}
          <ul className="flex flex-col gap-2">
            {clients.map((client) => (
              <li key={client.id}>
                <button
                  className="flex w-full items-center justify-between rounded border border-[var(--border-main)]/50 bg-[var(--bg-app)] p-3 text-left transition hover:border-sky-500/50 hover:bg-[var(--bg-hover)]"
                  onClick={() => onSelect(client)}
                >
                  <div>
                    <span className="font-medium text-[var(--text-primary)]">{client.name}</span>
                    <span className="ml-2 text-sm text-[var(--text-secondary)]">({client.bizNo || "사업자번호 없음"})</span>
                  </div>
                  <div className="flex gap-2">
                    <span className="rounded bg-[var(--bg-hover)] px-2 py-0.5 text-xs text-[var(--text-secondary)]">{client.type}</span>
                    <span className="rounded bg-[var(--bg-hover)] px-2 py-0.5 text-xs text-[var(--text-secondary)]">{client.status}</span>
                  </div>
                </button>
              </li>
            ))}
          </ul>
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
