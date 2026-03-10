import type { ReactNode } from "react";
import { useEffect, useRef } from "react";
import { useSessionStore } from "./session-store";
import { fetchMe, toSessionUser } from "./local-auth-api";

type Props = {
  children: ReactNode;
};

function MockAuthBootstrap({ children }: Props) {
  const status = useSessionStore((s) => s.status);
  const clearSession = useSessionStore((s) => s.clearSession);

  useEffect(() => {
    // In mock mode, start from anonymous so protected routes redirect to /login.
    if (status === "refreshing") {
      clearSession();
    }
  }, [status, clearSession]);

  return <>{children}</>;
}

function LocalAuthBootstrap({ children }: Props) {
  const status = useSessionStore((s) => s.status);
  const setAuthenticated = useSessionStore((s) => s.setAuthenticated);
  const clearSession = useSessionStore((s) => s.clearSession);
  const refreshAttemptRef = useRef(0);

  useEffect(() => {
    let mounted = true;
    const attempt = ++refreshAttemptRef.current;
    let watchdogTimer: number | null = null;

    const load = async () => {
      // Fail-safe: avoid indefinite loading on root/login when /auth/me is stalled.
      watchdogTimer = window.setTimeout(() => {
        if (!mounted || refreshAttemptRef.current !== attempt) return;
        if (useSessionStore.getState().status === "refreshing") {
          clearSession();
        }
      }, 8000);

      try {
        const me = await fetchMe();
        if (!mounted || refreshAttemptRef.current !== attempt) return;
        if (useSessionStore.getState().status !== "refreshing") return;

        setAuthenticated({
          user: toSessionUser(me.data),
          tokens: {
            accessToken: "SESSION",
            expiresAt: Math.floor(Date.now() / 1000) + 1800,
          },
        });
      } catch {
        if (!mounted || refreshAttemptRef.current !== attempt) return;
        if (useSessionStore.getState().status === "refreshing") {
          clearSession();
        }
      } finally {
        if (watchdogTimer != null) {
          window.clearTimeout(watchdogTimer);
          watchdogTimer = null;
        }
      }
    };

    if (status === "refreshing") {
      void load();
    }

    return () => {
      mounted = false;
      if (watchdogTimer != null) {
        window.clearTimeout(watchdogTimer);
      }
    };
  }, [status, setAuthenticated, clearSession]);

  return <>{children}</>;
}

export function AuthProviderShell({ children }: Props) {
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  if (authMode === "mock") {
    return <MockAuthBootstrap>{children}</MockAuthBootstrap>;
  }
  return <LocalAuthBootstrap>{children}</LocalAuthBootstrap>;
}
