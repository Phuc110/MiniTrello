import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { Notification } from "@/types";

export const notificationApi = {
  listAll: () =>
    apiClient.get<ApiEnvelope<Notification[]>>("/notifications").then((r) => r.data.data),

  listUnread: () =>
    apiClient.get<ApiEnvelope<Notification[]>>("/notifications/unread").then((r) => r.data.data),

  markRead: (id: string) =>
    apiClient
      .patch<ApiEnvelope<Notification>>(`/notifications/${id}/read`)
      .then((r) => r.data.data),

  markAllRead: () =>
    apiClient.patch<ApiEnvelope<void>>("/notifications/read-all").then((r) => r.data.data),
};
