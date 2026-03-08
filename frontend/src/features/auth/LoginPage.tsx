import { useState } from "react";
import { useSessionStore } from "../../auth/session-store";
import { localLogin, toSessionUser } from "../../auth/local-auth-api";

export function LoginPage() {
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  const setAuthenticated = useSessionStore((s) => s.setAuthenticated);
  const [userId, setUserId] = useState("admin");
  const [password, setPassword] = useState("admin1234");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const signInMock = () => {
    setAuthenticated({
      user: {
        sub: "local-user",
        name: "Local User",
        email: "local@example.com",
        roles: ["USER"],
      },
      tokens: {
        accessToken: "local-token",
        expiresAt: Math.floor(Date.now() / 1000) + 3600,
      },
    });
    window.location.href = "/dashboard";
  };

  const signInLocal = async () => {
    setSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await localLogin(userId, password);
      setAuthenticated({
        user: toSessionUser(response.data),
        tokens: {
          accessToken: "SESSION",
          expiresAt: Math.floor(Date.now() / 1000) + 1800,
        },
      });
      window.location.href = "/dashboard";
    } catch {
      setErrorMessage("로그인에 실패했습니다. ID/비밀번호를 확인하세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-b from-[#081328] via-[#0b1f3a] to-[#081328] p-6">
      <div className="pointer-events-none absolute -left-24 top-10 h-72 w-72 rounded-full bg-sky-500/20 blur-3xl" />
      <div className="pointer-events-none absolute -right-20 bottom-0 h-80 w-80 rounded-full bg-indigo-400/20 blur-3xl" />

      <div className="relative w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900/80 p-7 shadow-[0_20px_60px_-30px_rgba(20,100,220,0.75)] backdrop-blur">
        <div className="mb-6">
          <p className="text-xs uppercase tracking-[0.22em] text-slate-400">Tax Workbench</p>
          <h1 className="mt-2 text-3xl font-semibold text-white">Sign In</h1>
          <p className="mt-2 text-sm text-slate-300">보안 세션 기반으로 대시보드에 접속합니다.</p>
        </div>

        {authMode === "local" && (
          <form
            className="flex flex-col gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              void signInLocal();
            }}
          >
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">User ID</label>
            <input
              className="rounded-xl border border-slate-600 bg-slate-950/80 px-3 py-2.5 text-slate-100 outline-none ring-sky-400 transition focus:ring"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="사용자 ID"
            />

            <label className="mt-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Password</label>
            <input
              className="rounded-xl border border-slate-600 bg-slate-950/80 px-3 py-2.5 text-slate-100 outline-none ring-sky-400 transition focus:ring"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              type="password"
            />

            {errorMessage && <p className="mt-1 text-sm text-rose-400">{errorMessage}</p>}

            <button
              className="mt-2 rounded-xl bg-sky-500 px-4 py-2.5 font-semibold text-slate-950 transition hover:bg-sky-400 disabled:opacity-60"
              type="submit"
              disabled={submitting}
            >
              {submitting ? "로그인 중..." : "로그인"}
            </button>
          </form>
        )}

        {authMode === "mock" && (
          <button
            className="w-full rounded-xl bg-sky-500 px-4 py-2.5 font-semibold text-slate-950 transition hover:bg-sky-400"
            onClick={signInMock}
            type="button"
          >
            Mock 로그인
          </button>
        )}
      </div>
    </div>
  );
}
