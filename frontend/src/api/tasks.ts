import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Priority, Task } from "@/types";

export const taskApi = {
  myTasks: () =>
    apiClient.get<ApiEnvelope<Task[]>>("/tasks/my-tasks").then((r) => r.data.data),

  listForList: (boardListId: string) =>
    apiClient.get<ApiEnvelope<Task[]>>(`/board-lists/${boardListId}/tasks`).then((r) => r.data.data),

  create: (
    boardListId: string,
    payload: { title: string; description?: string; priority?: Priority; dueDate?: string | null },
  ) =>
    apiClient
      .post<ApiEnvelope<Task>>(`/board-lists/${boardListId}/tasks`, payload)
      .then((r) => r.data.data),

  update: (
    taskId: string,
    payload: { title: string; description?: string; priority: Priority; dueDate?: string | null },
  ) => apiClient.put<ApiEnvelope<Task>>(`/tasks/${taskId}`, payload).then((r) => r.data.data),

  remove: (taskId: string) => apiClient.delete<ApiEnvelope<void>>(`/tasks/${taskId}`),

  move: (
    taskId: string,
    payload: { targetBoardListId: string; prevTaskId: string | null; nextTaskId: string | null },
  ) => apiClient.patch<ApiEnvelope<Task>>(`/tasks/${taskId}/position`, payload).then((r) => r.data.data),

  assign: (taskId: string, userId: string) =>
    apiClient.post<ApiEnvelope<void>>(`/tasks/${taskId}/assignees/${userId}`),

  unassign: (taskId: string, userId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/tasks/${taskId}/assignees/${userId}`),

  addTag: (taskId: string, tagId: string) =>
    apiClient.post<ApiEnvelope<void>>(`/tasks/${taskId}/tags/${tagId}`),

  removeTag: (taskId: string, tagId: string) =>
    apiClient.delete<ApiEnvelope<void>>(`/tasks/${taskId}/tags/${tagId}`),
};
