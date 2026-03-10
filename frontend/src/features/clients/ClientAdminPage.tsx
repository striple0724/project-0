import { useState, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { fetchClients, createClient, updateClient, deleteClient } from "./api";
import { Search, ChevronUp, Loader2 } from "lucide-react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import type { ClientFilters, Client, ClientStatus, ClientType, ClientTier } from "./types";

const clientSchema = z.object({
  name: z.string().min(1, "고객사명은 필수입니다."),
  bizNo: z.string().optional(),
  type: z.enum(["INDIVIDUAL", "CORPORATE"]),
  status: z.enum(["ACTIVE", "INACTIVE"]),
  tier: z.enum(["BASIC", "PREMIUM", "VIP"]),
});

type ClientForm = z.infer<typeof clientSchema>;
type ClientFormInput = z.input<typeof clientSchema>;

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function ClientAdminPage() {
  const queryClient = useQueryClient();

  // Date Defaults: From (3 months ago 1st), To (Today)
  const today = new Date();
  const defaultToDate = new Date(today);
  const defaultFromDate = new Date(today.getFullYear(), today.getMonth() - 3, 1);

  const [filters, setFilters] = useState<ClientFilters>({
    page: 0,
    size: 25,
    createdAtFrom: toIsoDate(defaultFromDate),
    createdAtTo: toIsoDate(defaultToDate),
  });

  const [filterDraft, setFilterDraft] = useState<{
    name: string;
    type: ClientType | "";
    status: ClientStatus | "";
    tier: ClientTier | "";
    createdAtFrom: Date | null;
    createdAtTo: Date | null;
  }>({
    name: "",
    type: "",
    status: "",
    tier: "",
    createdAtFrom: defaultFromDate,
    createdAtTo: defaultToDate,
  });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showScroll, setShowScroll] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 300) {
        setShowScroll(true);
      } else {
        setShowScroll(false);
      }
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["clients", filters],
    queryFn: () => fetchClients(filters),
    placeholderData: (prev) => prev,
  });

  const { register, handleSubmit, reset } = useForm<ClientFormInput>({
    resolver: zodResolver(clientSchema),
    defaultValues: {
      name: "",
      bizNo: "",
      type: "CORPORATE",
      status: "ACTIVE",
      tier: "BASIC",
    },
  });

  const saveMutation = useMutation({
    mutationFn: async (values: ClientForm) => {
      if (editingId) {
        return updateClient(editingId, values);
      }
      return createClient(values);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
      closeModal();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteClient,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
  });

  const openCreateModal = () => {
    setEditingId(null);
    reset({
      name: "",
      bizNo: "",
      type: "CORPORATE",
      status: "ACTIVE",
      tier: "BASIC",
    });
    setIsModalOpen(true);
  };

  const openEditModal = (client: Client) => {
    setEditingId(client.id);
    reset({
      name: client.name,
      bizNo: client.bizNo ?? "",
      type: client.type,
      status: client.status,
      tier: client.tier,
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
  };

  const formatBizNo = (value: string) => {
    const cleaned = value.replace(/\D/g, "").slice(0, 10);
    if (cleaned.length <= 3) return cleaned;
    if (cleaned.length <= 5) return `${cleaned.slice(0, 3)}-${cleaned.slice(3)}`;
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 5)}-${cleaned.slice(5)}`;
  };

  const handleBizNoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatBizNo(e.target.value);
    e.target.value = formatted;
    return formatted;
  };

  const onSearch = () => {
    setFilters({
      name: filterDraft.name || undefined,
      type: filterDraft.type || undefined,
      status: filterDraft.status || undefined,
      tier: filterDraft.tier || undefined,
      createdAtFrom: filterDraft.createdAtFrom ? toIsoDate(filterDraft.createdAtFrom) : undefined,
      createdAtTo: filterDraft.createdAtTo ? toIsoDate(filterDraft.createdAtTo) : undefined,
      page: 0,
      size: 25,
    });
  };

  const clients = data?.data ?? [];
  const pageInfo = data?.page;

  return (
    <div className="mx-auto flex max-w-7xl flex-col gap-6 p-6 relative">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-100">고객사 관리</h1>
          <p className="mt-1 text-sm text-slate-400">워크벤치에서 사용할 고객사 마스터 데이터를 관리합니다.</p>
        </div>
        <button
          className="rounded border border-sky-500/50 bg-sky-900/40 px-4 py-2 text-sm font-medium text-sky-100 transition hover:bg-sky-900/60 active:scale-[0.98]"
          onClick={openCreateModal}
        >
          + 새 고객사 등록
        </button>
      </div>

      <section className="rounded-xl border border-slate-800 bg-[#0f172a] p-5 shadow-lg">
        <h2 className="mb-3 text-lg font-medium text-slate-200">조회 조건</h2>
        <div className="flex flex-wrap gap-3">
          <input
            className="flex-1 min-w-[150px] rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            placeholder="고객사명"
            value={filterDraft.name}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, name: e.target.value }))}
            onKeyDown={(e) => e.key === "Enter" && onSearch()}
          />
          <select
            className="w-32 rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            value={filterDraft.type}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, type: e.target.value as ClientType | "" }))}
          >
            <option value="">유형 전체</option>
            <option value="CORPORATE">법인</option>
            <option value="INDIVIDUAL">개인</option>
          </select>
          <select
            className="w-32 rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            value={filterDraft.tier}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, tier: e.target.value as ClientTier | "" }))}
          >
            <option value="">등급 전체</option>
            <option value="BASIC">BASIC</option>
            <option value="PREMIUM">PREMIUM</option>
            <option value="VIP">VIP</option>
          </select>
          <select
            className="w-32 rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
            value={filterDraft.status}
            onChange={(e) => setFilterDraft((prev) => ({ ...prev, status: e.target.value as ClientStatus | "" }))}
          >
            <option value="">상태 전체</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>
          
          <div className="flex items-center gap-2">
            <DatePicker
              selected={filterDraft.createdAtFrom}
              onChange={(date: Date | null) => setFilterDraft((prev) => ({ ...prev, createdAtFrom: date }))}
              selectsStart
              startDate={filterDraft.createdAtFrom}
              endDate={filterDraft.createdAtTo}
              dateFormat="yyyy-MM-dd"
              className="w-32 rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
              placeholderText="등록일(From)"
            />
            <span className="text-slate-400">~</span>
            <DatePicker
              selected={filterDraft.createdAtTo}
              onChange={(date: Date | null) => setFilterDraft((prev) => ({ ...prev, createdAtTo: date }))}
              selectsEnd
              startDate={filterDraft.createdAtFrom}
              endDate={filterDraft.createdAtTo}
              minDate={filterDraft.createdAtFrom || undefined}
              dateFormat="yyyy-MM-dd"
              className="w-32 rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-200 outline-none focus:border-sky-500"
              placeholderText="등록일(To)"
            />
          </div>

          <button
            className="flex h-[42px] w-12 shrink-0 items-center justify-center gap-2 rounded bg-sky-500 font-semibold text-slate-950 transition active:scale-[0.98] active:brightness-90 disabled:opacity-70 disabled:cursor-not-allowed"
            onClick={onSearch}
            type="button"
            title="조회"
            disabled={isFetching}
          >
            {isFetching ? <Loader2 className="animate-spin" size={18} /> : <Search size={18} />}
          </button>
        </div>
      </section>

      <section className="rounded-xl border border-slate-800 bg-[#0f172a] p-5 shadow-lg flex-1 flex flex-col min-h-0">
        <div className="w-full flex-1 overflow-auto rounded border border-slate-800 bg-[#061022]">
          <table className="w-full text-left text-sm text-slate-300 border-collapse">
            <thead className="sticky top-0 z-10 bg-[#0a152d] text-slate-200 border-b border-slate-800 shadow-sm">
              <tr>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">ID</th>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">고객사명</th>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">사업자번호</th>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">유형</th>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">등급</th>
                <th className="px-4 py-3 font-semibold border-r border-slate-800">상태</th>
                <th className="px-4 py-3 font-semibold text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/50">
              {isLoading && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500">
                    로딩 중...
                  </td>
                </tr>
              )}
              {!isLoading && clients.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500">
                    등록된 고객사가 없습니다.
                  </td>
                </tr>
              )}
              {clients.map((client) => (
                <tr key={client.id} className="hover:bg-[#0a152d] transition-colors group">
                  <td className="px-4 py-3 border-r border-slate-800/50">{client.id}</td>
                  <td className="px-4 py-3 border-r border-slate-800/50 font-medium text-slate-200">{client.name}</td>
                  <td className="px-4 py-3 border-r border-slate-800/50">{client.bizNo || "-"}</td>
                  <td className="px-4 py-3 border-r border-slate-800/50">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${client.type === 'CORPORATE' ? 'bg-indigo-900/40 text-indigo-200 border border-indigo-700/50' : 'bg-orange-900/40 text-orange-200 border border-orange-700/50'}`}>
                      {client.type}
                    </span>
                  </td>
                  <td className="px-4 py-3 border-r border-slate-800/50">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${client.tier === 'VIP' ? 'bg-amber-900/40 text-amber-200 border border-amber-700/50' : 'bg-slate-800 text-slate-300 border border-slate-700'}`}>
                      {client.tier}
                    </span>
                  </td>
                  <td className="px-4 py-3 border-r border-slate-800/50">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${client.status === 'ACTIVE' ? 'bg-emerald-900/40 text-emerald-200 border border-emerald-700/50' : 'bg-rose-900/40 text-rose-200 border border-rose-700/50'}`}>
                      {client.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      className="text-sky-400 hover:text-sky-300 mr-3 transition"
                      onClick={() => openEditModal(client)}
                    >
                      수정
                    </button>
                    <button
                      className="text-rose-400 hover:text-rose-300 transition"
                      onClick={() => {
                        if (confirm(`정말 ${client.name} 고객사를 삭제하시겠습니까?`)) {
                          deleteMutation.mutate(client.id);
                        }
                      }}
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {pageInfo && pageInfo.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between border-t border-slate-800/50 pt-4">
            <div className="text-sm text-slate-500">
              총 {pageInfo.totalElements}건
            </div>
            <div className="flex items-center gap-1">
              <button
                className="rounded border border-slate-700 bg-slate-800 px-3 py-1 text-sm text-slate-300 transition hover:bg-slate-700 disabled:opacity-40"
                disabled={pageInfo.number === 0}
                onClick={() => setFilters((prev) => ({ ...prev, page: Math.max(0, (prev.page ?? 0) - 1) }))}
              >
                이전
              </button>
              <span className="flex items-center px-3 text-sm text-slate-400">
                {pageInfo.number + 1} / {pageInfo.totalPages}
              </span>
              <button
                className="rounded border border-slate-700 bg-slate-800 px-3 py-1 text-sm text-slate-300 transition hover:bg-slate-700 disabled:opacity-40"
                disabled={pageInfo.number >= pageInfo.totalPages - 1}
                onClick={() => setFilters((prev) => ({ ...prev, page: Math.min(pageInfo.totalPages - 1, (prev.page ?? 0) + 1) }))}
              >
                다음
              </button>

              <div className="ml-4 flex items-center gap-2">
                <input
                  type="number"
                  min={1}
                  max={pageInfo.totalPages}
                  className="w-16 rounded border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-200 outline-none focus:border-sky-500 text-center hide-spin-button"
                  placeholder="이동"
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      const targetPage = Number((e.target as HTMLInputElement).value) - 1;
                      if (!isNaN(targetPage) && targetPage >= 0 && targetPage < pageInfo.totalPages) {
                        setFilters((prev) => ({ ...prev, page: targetPage }));
                      }
                    }
                  }}
                />
                <span className="text-sm text-slate-500">페이지로</span>
              </div>
            </div>
          </div>
        )}
      </section>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
          <div className="w-full max-xl rounded-xl border border-slate-700 bg-slate-900 p-6 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <h3 className="text-xl font-semibold text-slate-100">
                {editingId ? "고객사 수정" : "새 고객사 등록"}
              </h3>
              <button
                className="text-slate-400 hover:text-slate-200"
                onClick={closeModal}
              >
                ✕
              </button>
            </div>

            <form
              className="grid grid-cols-1 gap-4 md:grid-cols-2"
              onSubmit={handleSubmit((values) => {
                saveMutation.mutate(values as ClientForm);
              })}
            >
              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">고객사명</label>
                <input
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
                  placeholder="예: (주)택스컴퍼니"
                  {...register("name")}
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">사업자번호</label>
                <input
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
                  placeholder="000-00-00000"
                  {...register("bizNo", {
                    onChange: handleBizNoChange,
                  })}
                  maxLength={12}
                />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">유형 (Type)</label>
                <select
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
                  {...register("type")}
                >
                  <option value="CORPORATE">CORPORATE (법인)</option>
                  <option value="INDIVIDUAL">INDIVIDUAL (개인)</option>
                </select>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-sm text-slate-400">등급 (Tier)</label>
                <select
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
                  {...register("tier")}
                >
                  <option value="BASIC">BASIC</option>
                  <option value="PREMIUM">PREMIUM</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>

              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-sm text-slate-400">상태 (Status)</label>
                <select
                  className="rounded border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-200 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500 transition"
                  {...register("status")}
                >
                  <option value="ACTIVE">ACTIVE (활성)</option>
                  <option value="INACTIVE">INACTIVE (비활성)</option>
                </select>
              </div>

              <div className="col-span-1 flex items-center justify-end gap-3 md:col-span-2 mt-4 pt-4 border-t border-slate-800">
                <button
                  className="rounded px-4 py-2 text-sm text-slate-300 hover:text-slate-100 transition"
                  type="button"
                  onClick={closeModal}
                >
                  취소
                </button>
                <button
                  className="rounded border border-sky-500/50 bg-sky-600 px-6 py-2 text-sm font-medium text-white transition hover:bg-sky-500 disabled:opacity-60"
                  type="submit"
                  disabled={saveMutation.isPending}
                >
                  {saveMutation.isPending ? "저장 중..." : "저장"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
