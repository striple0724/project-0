import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useSessionStore } from "./session-store";

type Props = {
  children: ReactNode;
  requiredRole?: string;
};

export function ProtectedRoute({ children, requiredRole }: Props) {
  const status = useSessionStore((s) => s.status);
  const userRoles = useSessionStore((s) => s.user?.roles) ?? [];
  const location = useLocation();

  if (status === "refreshing") {
    return null;
  }

  if (status !== "authenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (requiredRole && !userRoles.includes(requiredRole)) {
    return <Navigate to="/workbench" replace />;
  }

  return <>{children}</>;
}
