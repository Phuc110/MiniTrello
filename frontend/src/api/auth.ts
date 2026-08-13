import { apiClient, type ApiEnvelope } from "./axiosClient";
import type { AuthResponse } from "@/types";

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
}

export const authApi = {
  login: (payload: LoginPayload) =>
    apiClient.post<ApiEnvelope<AuthResponse>>("/auth/login", payload).then((r) => r.data.data),

  register: (payload: RegisterPayload) =>
    apiClient.post<ApiEnvelope<AuthResponse>>("/auth/register", payload).then((r) => r.data.data),

  refresh: () =>
    apiClient.post<ApiEnvelope<AuthResponse>>("/auth/refresh").then((r) => r.data.data),

  logout: () => apiClient.post<ApiEnvelope<void>>("/auth/logout").then((r) => r.data),
};
