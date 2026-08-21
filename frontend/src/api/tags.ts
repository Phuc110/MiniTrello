import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { TagDto } from "@/types";

export const tagApi = {
  listByWorkspace: (workspaceId: string) =>
    apiClient.get<ApiEnvelope<TagDto[]>>(`/workspaces/${workspaceId}/tags`).then((r) => r.data.data),

  create: (workspaceId: string, payload: { name: string; color: string }) =>
    apiClient.post<ApiEnvelope<TagDto>>(`/workspaces/${workspaceId}/tags`, payload).then((r) => r.data.data),

  remove: (tagId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/tags/${tagId}`),
};
