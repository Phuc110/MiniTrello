import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import { queryClient } from "@/lib/queryClient";
import { AuthProvider } from "@/hooks/AuthContext";
import { ProtectedRoute } from "@/components/layout/ProtectedRoute";
import { AppLayout } from "@/components/layout/AppLayout";
import { LoginPage } from "@/features/auth/LoginPage";
import { RegisterPage } from "@/features/auth/RegisterPage";
import { WorkspaceListPage } from "@/features/workspace/WorkspaceListPage";
import { ProjectListPage } from "@/features/project/ProjectListPage";
import { BoardPage } from "@/features/board/BoardPage";

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/workspaces" element={<WorkspaceListPage />} />
                <Route path="/workspaces/:workspaceId/projects" element={<ProjectListPage />} />
                <Route path="/projects/:projectId" element={<BoardPage />} />
              </Route>
            </Route>

            <Route path="/" element={<Navigate to="/workspaces" replace />} />
            <Route path="*" element={<Navigate to="/workspaces" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
