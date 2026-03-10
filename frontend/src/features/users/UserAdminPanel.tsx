import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createUser, deleteUser, fetchUsers, updateUser } from "./api";
import type { UserAccount } from "./types";

type Props = {
  loginUserName: string;
};

const initialCreateForm = {
  id: "",
  name: "",
  password: "",
  email: "",
  mobileNo: "",
  useYn: "Y",
  crtBy: "admin",
  crtIp: "127.0.0.1",
};

export function UserAdminPanel({ loginUserName }: Props) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<UserAccount | null>(null);
  const [createForm, setCreateForm] = useState(initialCreateForm);
  const [updateName, setUpdateName] = useState("");
  const [updateUseYn, setUpdateUseYn] = useState("Y");
  const [updatePassword, setUpdatePassword] = useState("");
  const [updateEmail, setUpdateEmail] = useState("");
  const [updateMobileNo, setUpdateMobileNo] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ["users"],
    queryFn: fetchUsers,
  });

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: async () => {
      setMessage("사용자를 생성했습니다.");
      setCreateForm(initialCreateForm);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: () => setMessage("사용자 생성에 실패했습니다."),
  });

  const updateMutation = useMutation({
    mutationFn: ({
      seq,
      payload,
    }: {
      seq: number;
      payload: { name: string; password?: string; email: string; mobileNo: string; useYn: string; amnBy: string; amnIp: string };
    }) =>
      updateUser(seq, payload),
    onSuccess: async () => {
      setMessage("사용자를 수정했습니다.");
      setUpdatePassword("");
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: () => setMessage("사용자 수정에 실패했습니다."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: async () => {
      setMessage("사용자를 삭제했습니다.");
      setSelected(null);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: () => setMessage("사용자 삭제에 실패했습니다."),
  });

  const sortedUsers = useMemo(() => {
    return [...(usersQuery.data ?? [])].sort((a, b) => a.seq - b.seq);
  }, [usersQuery.data]);

  const onSelect = (user: UserAccount) => {
    setSelected(user);
    setUpdateName(user.name);
    setUpdateEmail(user.email);
    setUpdateMobileNo(user.mobileNo);
    setUpdateUseYn(user.useYn);
    setUpdatePassword("");
  };

  return (
    <section className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] p-4 shadow-sm transition-colors duration-200">
      <h2 className="mb-3 text-lg font-medium text-[var(--text-primary)]">사용자 관리 (TB_USER CRUD)</h2>
      {message && <p className="mb-3 text-sm text-[var(--text-secondary)]">{message}</p>}

      <div className="mb-4 overflow-auto rounded border border-[var(--border-main)] bg-[var(--bg-app)]">
        <table className="min-w-full text-sm text-[var(--text-secondary)]">
          <thead className="bg-[var(--bg-hover)] text-left text-[var(--text-primary)]">
            <tr>
              <th className="px-3 py-2 border-r border-[var(--border-main)]/30">SEQ</th>
              <th className="px-3 py-2 border-r border-[var(--border-main)]/30">ID</th>
              <th className="px-3 py-2 border-r border-[var(--border-main)]/30">NAME</th>
              <th className="px-3 py-2 border-r border-[var(--border-main)]/30">EMAIL</th>
              <th className="px-3 py-2 border-r border-[var(--border-main)]/30">MOBILE_NO</th>
              <th className="px-3 py-2">USE_YN</th>
            </tr>
          </thead>
          <tbody>
            {sortedUsers.map((user) => (
              <tr
                key={user.seq}
                className={`cursor-pointer border-t border-[var(--border-main)]/30 hover:bg-[var(--bg-hover)]/50 ${selected?.seq === user.seq ? "bg-sky-900/20" : ""}`}
                onClick={() => onSelect(user)}
              >
                <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{user.seq}</td>
                <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{user.id}</td>
                <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{user.name}</td>
                <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{user.email}</td>
                <td className="px-3 py-2 border-r border-[var(--border-main)]/30">{user.mobileNo}</td>
                <td className="px-3 py-2">{user.useYn}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <form
          className="rounded border border-[var(--border-main)] p-3 bg-[var(--bg-app)]/50"
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate(createForm);
          }}
        >
          <h3 className="mb-2 font-medium text-[var(--text-primary)]">사용자 생성</h3>
          <div className="grid grid-cols-1 gap-2">
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="ID (max 20)"
              value={createForm.id}
              onChange={(e) => setCreateForm((f) => ({ ...f, id: e.target.value }))}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="NAME (max 20)"
              value={createForm.name}
              onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="비밀번호"
              type="password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="EMAIL (max 20)"
              value={createForm.email}
              onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="MOBILE_NO (max 20)"
              value={createForm.mobileNo}
              onChange={(e) => setCreateForm((f) => ({ ...f, mobileNo: e.target.value }))}
            />
            <select
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              value={createForm.useYn}
              onChange={(e) => setCreateForm((f) => ({ ...f, useYn: e.target.value }))}
            >
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="CRT_BY"
              value={createForm.crtBy}
              onChange={(e) => setCreateForm((f) => ({ ...f, crtBy: e.target.value }))}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="CRT_IP"
              value={createForm.crtIp}
              onChange={(e) => setCreateForm((f) => ({ ...f, crtIp: e.target.value }))}
            />
            <button className="rounded bg-sky-600 px-4 py-2 text-white hover:bg-sky-500 transition shadow-sm" type="submit">
              생성
            </button>
          </div>
        </form>

        <form
          className="rounded border border-[var(--border-main)] p-3 bg-[var(--bg-app)]/50"
          onSubmit={(e) => {
            e.preventDefault();
            if (!selected) return;
            updateMutation.mutate({
              seq: selected.seq,
              payload: {
                name: updateName,
                email: updateEmail,
                mobileNo: updateMobileNo,
                useYn: updateUseYn,
                amnBy: loginUserName || "admin",
                amnIp: "127.0.0.1",
                password: updatePassword || undefined,
              },
            });
          }}
        >
          <h3 className="mb-2 font-medium text-[var(--text-primary)]">사용자 수정/삭제</h3>
          <div className="grid grid-cols-1 gap-2">
            <input className="rounded border border-[var(--border-main)] bg-[var(--bg-hover)] px-3 py-2 text-[var(--text-secondary)] outline-none" value={selected?.id ?? ""} readOnly placeholder="ID" />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="NAME"
              value={updateName}
              onChange={(e) => setUpdateName(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="EMAIL"
              value={updateEmail}
              onChange={(e) => setUpdateEmail(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="MOBILE_NO"
              value={updateMobileNo}
              onChange={(e) => setUpdateMobileNo(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              placeholder="비밀번호 변경(선택)"
              type="password"
              value={updatePassword}
              onChange={(e) => setUpdatePassword(e.target.value)}
              disabled={!selected}
            />
            <select
              className="rounded border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2 text-[var(--text-primary)] outline-none focus:border-sky-500"
              value={updateUseYn}
              onChange={(e) => setUpdateUseYn(e.target.value)}
              disabled={!selected}
            >
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
            <div className="flex gap-2">
              <button className="rounded bg-emerald-600 px-4 py-2 text-white disabled:opacity-50 hover:bg-emerald-500 transition" type="submit" disabled={!selected}>
                수정
              </button>
              <button
                className="rounded bg-rose-600 px-4 py-2 text-white disabled:opacity-50 hover:bg-rose-500 transition"
                type="button"
                disabled={!selected}
                onClick={() => {
                  if (confirm(`정말 ${selected?.name} 사용자를 삭제하시겠습니까?`)) {
                    selected && deleteMutation.mutate(selected.seq);
                  }
                }}
              >
                삭제
              </button>
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}
