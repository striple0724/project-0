import type { ReactNode } from "react";
import { useEffect } from "react";
import { useSessionStore } from "./session-store";
import { fetchMe, toSessionUser } from "./local-auth-api";

type Props = {
  children: ReactNode;
};

function MockAuthBootstrap({ children }: Props) {
  return <>{children}</>;
}

function LocalAuthBootstrap({ children }: Props) {
  const status = useSessionStore((s) => s.status);
  const setAuthenticated = useSessionStore((s) => s.setAuthenticated);
  const setRefreshing = useSessionStore((s) => s.setRefreshing);
  const clearSession = useSessionStore((s) => s.clearSession);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setRefreshing();
      try {
        const me = await fetchMe();
        if (!mounted) return;
        setAuthenticated({
          user: toSessionUser(me.data),
          tokens: {
            accessToken: "SESSION",
            expiresAt: Math.floor(Date.now() / 1000) + 1800,
          },
        });
      } catch {
        if (!mounted) return;
        clearSession();
      }
    };
    if (status === "refreshing") {
      void load();
    }
    return () => {
      mounted = false;
    };
  }, [status, setAuthenticated, setRefreshing, clearSession]);

  return <>{children}</>;
}

export function AuthProviderShell({ children }: Props) {
  const authMode = (import.meta.env.VITE_AUTH_MODE as string | undefined) ?? "local";
  if (authMode === "mock") {
    return <MockAuthBootstrap>{children}</MockAuthBootstrap>;
  }
  return <LocalAuthBootstrap>{children}</LocalAuthBootstrap>;
}
