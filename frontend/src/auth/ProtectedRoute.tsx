import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useSessionStore } from "./session-store";

type Props = {
  children: ReactNode;
};

export function ProtectedRoute({ children }: Props) {
  const status = useSessionStore((s) => s.status);
  const location = useLocation();

  if (status === "refreshing") {
    return <div className="p-6 text-sm text-slate-600">세션 갱신 중...</div>;
  }

  if (status !== "authenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
