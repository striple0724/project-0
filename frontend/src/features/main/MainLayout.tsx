import { NavLink, Outlet } from "react-router-dom";
import { useSessionStore } from "../../auth/session-store";
import { localLogout } from "../../auth/local-auth-api";

const menus = [
  { to: "/dashboard", label: "Dashboard", caption: "운영 현황" },
  { to: "/workbench", label: "Workbench", caption: "업무 관리" },
];

export function MainLayout() {
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const clearSession = useSessionStore((s) => s.clearSession);
  const userName = useSessionStore((s) => s.user?.name ?? "User");

  const signOut = () => {
    if (authMode === "local") {
      void localLogout()
        .catch(() => undefined)
        .finally(() => {
          clearSession();
          window.location.href = "/login";
        });
      return;
    }
    clearSession();
    window.location.href = "/login";
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="flex min-h-screen">
        <aside className="hidden w-72 flex-col border-r border-slate-800 bg-gradient-to-b from-slate-900 via-slate-950 to-[#0a1a33] p-6 md:flex">
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-slate-400">Tax Workbench</p>
            <h1 className="mt-2 text-2xl font-semibold text-slate-100">Main Menu</h1>
          </div>

          <nav className="mt-8 flex flex-col gap-2">
            {menus.map((menu) => (
              <NavLink
                key={menu.to}
                to={menu.to}
                className={({ isActive }) =>
                  `rounded-xl border px-4 py-3 transition ${
                    isActive
                      ? "border-sky-400/60 bg-sky-500/15 text-white"
                      : "border-slate-700 bg-slate-900/50 text-slate-300 hover:border-slate-500 hover:text-white"
                  }`
                }
              >
                <p className="text-sm font-semibold">{menu.label}</p>
                <p className="mt-0.5 text-xs text-slate-400">{menu.caption}</p>
              </NavLink>
            ))}
          </nav>

          <div className="mt-auto rounded-xl border border-slate-700 bg-slate-900/60 p-4">
            <p className="text-xs text-slate-400">Signed in as</p>
            <p className="mt-1 text-sm font-medium text-slate-100">{userName}</p>
          </div>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="border-b border-slate-800 bg-[#0b1f3a] px-4 py-3 md:px-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-slate-400 md:hidden">Tax Workbench</p>
                <p className="text-sm text-slate-300">신뢰 기반 세무 업무 대시보드</p>
              </div>
              <button
                className="rounded-lg border border-slate-500 bg-slate-900/40 px-3 py-1.5 text-sm text-slate-100 hover:border-slate-300"
                type="button"
                onClick={signOut}
              >
                로그아웃
              </button>
            </div>
            <div className="mt-3 flex gap-2 overflow-x-auto md:hidden">
              {menus.map((menu) => (
                <NavLink
                  key={menu.to}
                  to={menu.to}
                  className={({ isActive }) =>
                    `rounded-lg border px-3 py-1.5 text-sm ${
                      isActive
                        ? "border-sky-400/60 bg-sky-500/15 text-white"
                        : "border-slate-700 bg-slate-900/50 text-slate-300"
                    }`
                  }
                >
                  {menu.label}
                </NavLink>
              ))}
            </div>
          </header>

          <main className="flex-1 bg-gradient-to-b from-slate-950 via-[#071326] to-slate-950">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
