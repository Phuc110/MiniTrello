import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Workspace } from "@/types";

export const workspaceApi = {
  list: () => apiClient.get<ApiEnvelope<Workspace[]>>("/workspaces").then((r) => r.data.data),

  create: (name: string) =>
    apiClient.post<ApiEnvelope<Workspace>>("/workspaces", { name }).then((r) => r.data.data),

  getById: (id: string) =>
    apiClient.get<ApiEnvelope<Workspace>>(`/workspaces/${id}`).then((r) => r.data.data),

  remove: (id: string) => apiClient.delete<void>(`/workspaces/${id}`),
};

