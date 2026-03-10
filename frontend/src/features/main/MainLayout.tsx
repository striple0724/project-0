import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useSessionStore } from "../../auth/session-store";
import { localLogout } from "../../auth/local-auth-api";

export function MainLayout() {
  const navigate = useNavigate();
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const clearSession = useSessionStore((s) => s.clearSession);
  const userName = useSessionStore((s) => s.user?.name ?? "User");
  const roles = useSessionStore((s) => s.user?.roles) ?? [];
  const isAdmin = roles.includes("ADMIN");
  const menus = [
    { to: "/workbench", label: "Workbench" },
    { to: "/clients", label: "고객사 관리" },
    ...(isAdmin ? [{ to: "/admin/jobs", label: "작업 모니터링" }] : []),
  ];

  const signOut = () => {
    if (authMode === "local") {
      void localLogout().catch(() => undefined);
    }
    clearSession();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-[#1e293b] bg-[#061022] px-4 py-3 md:px-6 shadow-md">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-sky-400">Tax Workbench</p>
          </div>

          <nav className="flex items-center gap-2 overflow-x-auto">
            {menus.map((menu) => (
              <NavLink
                key={menu.to}
                to={menu.to}
                className={({ isActive }) =>
                  `twb-menu-chip ${isActive ? "twb-menu-chip--active" : "twb-menu-chip--idle"}`
                }
              >
                {menu.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <span className="hidden text-sm text-slate-300 md:inline">{userName}</span>
            <button
              className="rounded-lg border border-slate-500 bg-slate-900/40 px-3 py-1.5 text-sm text-slate-100 hover:border-slate-300"
              type="button"
              onClick={signOut}
            >
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 bg-gradient-to-b from-slate-950 via-[#071326] to-slate-950">
        <Outlet />
      </main>
    </div>
  );
}

