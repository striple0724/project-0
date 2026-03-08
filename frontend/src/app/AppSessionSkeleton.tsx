import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProviderShell } from "../auth/AuthProviderShell";
import { ProtectedRoute } from "../auth/ProtectedRoute";
import { LoginPage } from "../features/auth/LoginPage";
import { DashboardPage } from "../features/main/DashboardPage";
import { MainLayout } from "../features/main/MainLayout";
import { WorkbenchPage } from "../features/work-items/WorkbenchPage";

const queryClient = new QueryClient();

export function AppSessionSkeleton() {
  return (
    <AuthProviderShell>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="workbench" element={<WorkbenchPage embedded />} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </AuthProviderShell>
  );
}
