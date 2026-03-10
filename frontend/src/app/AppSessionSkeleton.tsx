import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProviderShell } from "../auth/AuthProviderShell";
import { ProtectedRoute } from "../auth/ProtectedRoute";
import { useSessionStore } from "../auth/session-store";
import { LoginPage } from "../features/auth/LoginPage";
import { MainLayout } from "../features/main/MainLayout";
import { WorkbenchPage } from "../features/work-items/WorkbenchPage";
import { UserAdminPage } from "../features/users/UserAdminPage";
import { MenuAdminPage } from "../features/menus/MenuAdminPage";
import { ClientAdminPage } from "../features/clients/ClientAdminPage";
import { JobMonitorPage } from "../features/admin-jobs/JobMonitorPage";

const queryClient = new QueryClient();

export function AppSessionSkeleton() {
  return (
    <AuthProviderShell>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<HomeRoute />} />
            <Route path="/login" element={<LoginRoute />} />
            <Route
              element={
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              }
            >
              <Route path="dashboard" element={<Navigate to="/workbench" replace />} />
              <Route path="workbench" element={<WorkbenchPage embedded />} />
              <Route path="clients" element={<ClientAdminPage />} />
              <Route
                path="admin/jobs"
                element={
                  <ProtectedRoute requiredRole="ADMIN">
                    <JobMonitorPage />
                  </ProtectedRoute>
                }
              />
              <Route path="admin/users" element={<UserAdminPage />} />
              <Route path="admin/menus" element={<MenuAdminPage />} />
            </Route>
            <Route path="*" element={<FallbackRoute />} />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </AuthProviderShell>
  );
}

function HomeRoute() {
  const status = useSessionStore((s) => s.status);
  if (status === "refreshing") {
    return null;
  }
  if (status === "authenticated") {
    return <Navigate to="/workbench" replace />;
  }
  return <Navigate to="/login" replace />;
}

function LoginRoute() {
  const status = useSessionStore((s) => s.status);
  if (status === "refreshing") {
    return null;
  }
  if (status === "authenticated") {
    return <Navigate to="/workbench" replace />;
  }
  return <LoginPage />;
}

function FallbackRoute() {
  const status = useSessionStore((s) => s.status);
  if (status === "authenticated") {
    return <Navigate to="/workbench" replace />;
  }
  return <Navigate to="/login" replace />;
}
