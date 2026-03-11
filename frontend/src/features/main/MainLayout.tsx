import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Sun, Moon } from "lucide-react";
import { useSessionStore } from "../../auth/session-store";
import { localLogout } from "../../auth/local-auth-api";
import { useThemeStore } from "./theme-store";

export function MainLayout() {
  const navigate = useNavigate();
  const themeMode = useThemeStore((s) => s.mode);
  const toggleTheme = useThemeStore((s) => s.toggleNext);

  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const clearSession = useSessionStore((s) => s.clearSession);
  const userName = useSessionStore((s) => s.user?.name ?? "User");
  const roles = useSessionStore((s) => s.user?.roles) ?? [];
  const isAdmin = roles.includes("ADMIN");
  const menus = [
    { to: "/workbench", label: "Workbench" },
    { to: "/clients", label: "고객사 관리" },
    ...(isAdmin ? [{ to: "/admin/jobs", label: "작업 관제" }] : []),
  ];

  const signOut = () => {
    if (authMode === "local") {
      void localLogout().catch(() => undefined);
    }
    clearSession();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-[var(--bg-app)] text-[var(--text-primary)] transition-colors duration-200">
      <header className="border-b border-[var(--border-main)] bg-[var(--bg-app)] px-4 py-3 md:px-6 shadow-md">
        <div className="grid w-full grid-cols-[1fr_auto_1fr] items-center gap-3">
          <div>
            <p className="text-sm font-black uppercase tracking-[0.2em] text-[var(--logo-text)] drop-shadow-sm">Tax Workbench</p>
          </div>

          <nav className="flex items-center justify-center gap-2 overflow-x-auto">
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

          <div className="flex items-center justify-self-end gap-3">
            <button
              type="button"
              onClick={toggleTheme}
              className="flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border-main)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-[var(--accent)] transition-colors"
              title={`테마 전환 (현재: ${themeMode})`}
            >
              {themeMode === "light" && <Sun size={18} className="text-orange-500" />}
              {themeMode === "bright-navy" && <Sun size={18} className="text-blue-400" />}
              {themeMode === "dark-navy" && <Moon size={18} className="text-indigo-400" />}
            </button>
            <span className="hidden text-sm text-[var(--text-secondary)] md:inline">{userName}</span>
            <button
              className="rounded-lg border border-[var(--border-main)] bg-[var(--bg-card)] px-3 py-1.5 text-sm text-[var(--text-primary)] hover:border-sky-400 transition-colors"
              type="button"
              onClick={signOut}
            >
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 bg-gradient-to-b from-[var(--bg-app)] to-[var(--bg-card)]">
        <Outlet />
      </main>
    </div>
  );
}


