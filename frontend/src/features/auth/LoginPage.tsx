import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSessionStore } from "../../auth/session-store";
import { localLogin, toSessionUser } from "../../auth/local-auth-api";

export function LoginPage() {
  const navigate = useNavigate();
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
    navigate("/dashboard");
  };

  const signInLocal = async () => {
    const normalizedUserId = userId.trim();
    const normalizedPassword = password.trim();
    if (!normalizedUserId || !normalizedPassword) {
      setErrorMessage("ID/비밀번호를 입력하세요.");
      return;
    }

    setSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await localLogin(normalizedUserId, normalizedPassword);
      setAuthenticated({
        user: toSessionUser(response.data),
        tokens: {
          accessToken: "SESSION",
          expiresAt: Math.floor(Date.now() / 1000) + 1800,
        },
      });
      navigate("/dashboard");
    } catch {
      setErrorMessage("로그인에 실패했습니다. ID/비밀번호를 확인하세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[var(--bg-app)] p-6 transition-colors duration-200">
      <div className="relative w-full max-w-md flex flex-col items-center">
        <div className="relative w-full rounded-2xl border border-[var(--border-main)] bg-[var(--bg-card)] px-8 pb-8 pt-8 shadow-2xl backdrop-blur flex flex-col items-center">
        <div className="w-full mb-6 text-center">
          <p className="text-xs uppercase tracking-[0.22em] text-[var(--text-secondary)]">Tax Workbench</p>
          <h1 className="mt-2 text-3xl font-semibold text-[var(--text-primary)]">Sign In</h1>
        </div>

        {authMode === "local" && (
          <form
            className="flex w-full flex-col gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              void signInLocal();
            }}
          >
            <label className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">User ID</label>
            <input
              className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2.5 text-[var(--text-primary)] outline-none ring-sky-400 transition focus:ring"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="사용자 ID"
              autoComplete="username"
            />

            <label className="mt-1 text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">Password</label>
            <input
              className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-input)] px-3 py-2.5 text-[var(--text-primary)] outline-none ring-sky-400 transition focus:ring"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              type="password"
              autoComplete="current-password"
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
    </div>
  );
}
