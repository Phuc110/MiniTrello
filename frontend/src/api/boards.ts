import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Board, BoardListColumn } from "@/types";

export const boardApi = {
  listByWorkspace: (workspaceId: string) =>
    apiClient.get<ApiEnvelope<Board[]>>(`/workspaces/${workspaceId}/boards`).then((r) => r.data.data),

  create: (workspaceId: string, name: string) =>
    apiClient
      .post<ApiEnvelope<Board>>(`/workspaces/${workspaceId}/boards`, { name })
      .then((r) => r.data.data),

  getById: (boardId: string) =>
    apiClient.get<ApiEnvelope<Board>>(`/boards/${boardId}`).then((r) => r.data.data),

  remove: (boardId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/boards/${boardId}`),

  listLists: (boardId: string) =>
    apiClient.get<ApiEnvelope<BoardListColumn[]>>(`/boards/${boardId}/lists`).then((r) => r.data.data),

  createList: (boardId: string, name: string) =>
    apiClient
      .post<ApiEnvelope<BoardListColumn>>(`/boards/${boardId}/lists`, { name })
      .then((r) => r.data.data),

  moveList: (boardListId: string, payload: { prevListId: string | null; nextListId: string | null }) =>
    apiClient
      .patch<ApiEnvelope<BoardListColumn>>(`/board-lists/${boardListId}/position`, payload)
      .then((r) => r.data.data),

  removeList: (boardListId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/board-lists/${boardListId}`),
};
