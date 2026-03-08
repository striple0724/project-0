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
    <section className="rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 text-lg font-medium">사용자 관리 (TB_USER CRUD)</h2>
      {message && <p className="mb-3 text-sm text-slate-700">{message}</p>}

      <div className="mb-4 overflow-auto rounded border border-slate-200">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left">
            <tr>
              <th className="px-3 py-2">SEQ</th>
              <th className="px-3 py-2">ID</th>
              <th className="px-3 py-2">NAME</th>
              <th className="px-3 py-2">EMAIL</th>
              <th className="px-3 py-2">MOBILE_NO</th>
              <th className="px-3 py-2">USE_YN</th>
            </tr>
          </thead>
          <tbody>
            {sortedUsers.map((user) => (
              <tr
                key={user.seq}
                className={`cursor-pointer border-t border-slate-100 ${selected?.seq === user.seq ? "bg-slate-100" : "bg-white"}`}
                onClick={() => onSelect(user)}
              >
                <td className="px-3 py-2">{user.seq}</td>
                <td className="px-3 py-2">{user.id}</td>
                <td className="px-3 py-2">{user.name}</td>
                <td className="px-3 py-2">{user.email}</td>
                <td className="px-3 py-2">{user.mobileNo}</td>
                <td className="px-3 py-2">{user.useYn}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <form
          className="rounded border border-slate-200 p-3"
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate(createForm);
          }}
        >
          <h3 className="mb-2 font-medium">사용자 생성</h3>
          <div className="grid grid-cols-1 gap-2">
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="ID (max 20)"
              value={createForm.id}
              onChange={(e) => setCreateForm((f) => ({ ...f, id: e.target.value }))}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="NAME (max 20)"
              value={createForm.name}
              onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="비밀번호"
              type="password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="EMAIL (max 20)"
              value={createForm.email}
              onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="MOBILE_NO (max 20)"
              value={createForm.mobileNo}
              onChange={(e) => setCreateForm((f) => ({ ...f, mobileNo: e.target.value }))}
            />
            <select
              className="rounded border border-slate-300 px-3 py-2"
              value={createForm.useYn}
              onChange={(e) => setCreateForm((f) => ({ ...f, useYn: e.target.value }))}
            >
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="CRT_BY"
              value={createForm.crtBy}
              onChange={(e) => setCreateForm((f) => ({ ...f, crtBy: e.target.value }))}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="CRT_IP"
              value={createForm.crtIp}
              onChange={(e) => setCreateForm((f) => ({ ...f, crtIp: e.target.value }))}
            />
            <button className="rounded bg-blue-600 px-4 py-2 text-white" type="submit">
              생성
            </button>
          </div>
        </form>

        <form
          className="rounded border border-slate-200 p-3"
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
          <h3 className="mb-2 font-medium">사용자 수정/삭제</h3>
          <div className="grid grid-cols-1 gap-2">
            <input className="rounded border border-slate-300 px-3 py-2" value={selected?.id ?? ""} readOnly placeholder="ID" />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="NAME"
              value={updateName}
              onChange={(e) => setUpdateName(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="EMAIL"
              value={updateEmail}
              onChange={(e) => setUpdateEmail(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="MOBILE_NO"
              value={updateMobileNo}
              onChange={(e) => setUpdateMobileNo(e.target.value)}
              disabled={!selected}
            />
            <input
              className="rounded border border-slate-300 px-3 py-2"
              placeholder="비밀번호 변경(선택)"
              type="password"
              value={updatePassword}
              onChange={(e) => setUpdatePassword(e.target.value)}
              disabled={!selected}
            />
            <select
              className="rounded border border-slate-300 px-3 py-2"
              value={updateUseYn}
              onChange={(e) => setUpdateUseYn(e.target.value)}
              disabled={!selected}
            >
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
            <div className="flex gap-2">
              <button className="rounded bg-emerald-600 px-4 py-2 text-white disabled:opacity-50" type="submit" disabled={!selected}>
                수정
              </button>
              <button
                className="rounded bg-rose-600 px-4 py-2 text-white disabled:opacity-50"
                type="button"
                disabled={!selected}
                onClick={() => selected && deleteMutation.mutate(selected.seq)}
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
