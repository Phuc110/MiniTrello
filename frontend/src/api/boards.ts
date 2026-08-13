import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Board, BoardListColumn } from "@/types";

export const boardApi = {
  list: (projectId: string) =>
    apiClient.get<ApiEnvelope<Board[]>>(`/projects/${projectId}/boards`).then((r) => r.data.data),

  create: (projectId: string, name: string) =>
    apiClient.post<ApiEnvelope<Board>>(`/projects/${projectId}/boards`, { name }).then((r) => r.data.data),

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
};
