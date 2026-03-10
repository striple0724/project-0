import { useSessionStore } from "../../auth/session-store";
import { UserAdminPanel } from "./UserAdminPanel";

export function UserAdminPage() {
  const loginUserName = useSessionStore((s) => s.user?.name ?? "admin");

  return (
    <div className="workbench-shell mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8 transition-colors duration-200">
      <section className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] p-4 shadow-sm">
        <h1 className="text-2xl font-semibold text-[var(--text-primary)]">사용자 관리</h1>
        <p className="mt-2 text-sm text-[var(--text-secondary)]">워크벤치에서 분리된 관리자 화면입니다.</p>
      </section>
      <UserAdminPanel loginUserName={loginUserName} />
    </div>
  );
}
