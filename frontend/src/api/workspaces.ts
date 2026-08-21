import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Workspace } from "@/types";

export interface WorkspaceMember {
  userId: string;
  email: string;
  fullName: string;
}

export const workspaceApi = {
  list: () => apiClient.get<ApiEnvelope<Workspace[]>>("/workspaces").then((r) => r.data.data),

  create: (name: string) =>
    apiClient.post<ApiEnvelope<Workspace>>("/workspaces", { name }).then((r) => r.data.data),

  getById: (id: string) =>
    apiClient.get<ApiEnvelope<Workspace>>(`/workspaces/${id}`).then((r) => r.data.data),

  remove: (id: string) => apiClient.delete<void>(`/workspaces/${id}`),

  listMembers: (workspaceId: string) =>
    apiClient.get<ApiEnvelope<WorkspaceMember[]>>(`/workspaces/${workspaceId}/members`).then((r) => r.data.data),

  inviteMember: (workspaceId: string, email: string) =>
    apiClient
      .post<ApiEnvelope<WorkspaceMember>>(`/workspaces/${workspaceId}/members`, { email })
      .then((r) => r.data.data),

  removeMember: (workspaceId: string, userId: string) =>
    apiClient.delete<void>(`/workspaces/${workspaceId}/members/${userId}`),
};
