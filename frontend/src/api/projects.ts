import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { PageResponse, Project, ProjectMember, ProjectRole } from "@/types";

export const projectApi = {
  search: (workspaceId: string, params: { name?: string; page?: number; size?: number }) =>
    apiClient
      .get<ApiEnvelope<PageResponse<Project>>>(`/workspaces/${workspaceId}/projects`, { params })
      .then((r) => r.data.data),

  create: (workspaceId: string, payload: { name: string; description?: string }) =>
    apiClient
      .post<ApiEnvelope<Project>>(`/workspaces/${workspaceId}/projects`, payload)
      .then((r) => r.data.data),

  getById: (projectId: string) =>
    apiClient.get<ApiEnvelope<Project>>(`/projects/${projectId}`).then((r) => r.data.data),

  update: (projectId: string, payload: { name: string; description?: string }) =>
    apiClient.put<ApiEnvelope<Project>>(`/projects/${projectId}`, payload).then((r) => r.data.data),

  remove: (projectId: string) => apiClient.delete<ApiEnvelope<void>>(`/projects/${projectId}`),

  listMembers: (projectId: string) =>
    apiClient.get<ApiEnvelope<ProjectMember[]>>(`/projects/${projectId}/members`).then((r) => r.data.data),

  inviteMember: (projectId: string, payload: { email: string; role: ProjectRole }) =>
    apiClient
      .post<ApiEnvelope<ProjectMember>>(`/projects/${projectId}/members`, payload)
      .then((r) => r.data.data),

  removeMember: (projectId: string, userId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/projects/${projectId}/members/${userId}`),
};
