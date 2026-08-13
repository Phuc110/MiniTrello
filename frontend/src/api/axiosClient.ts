import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";

/**
 * The API envelope every backend response follows (see the Java
 * ApiResponse<T> record) — kept in sync with the backend contract.
 */
export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: { field: string | null; message: string }[];
  timestamp: string;
  path: string;
}

let inMemoryAccessToken: string | null = null;

export function setAccessToken(token: string | null) {
  inMemoryAccessToken = token;
}

export function getAccessToken() {
  return inMemoryAccessToken;
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  // Refresh token travels as an httpOnly cookie (see AuthController) —
  // this is what makes the browser actually send it.
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  if (inMemoryAccessToken) {
    config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
  }
  return config;
});

/**
 * Single-flight refresh: if multiple requests 401 at the same moment
 * (e.g. a page fires 4 parallel queries right as the access token
 * expires), we only want ONE call to /auth/refresh — not four racing
 * refresh calls that could each rotate the refresh token and invalidate
 * each other (see the backend's reuse-detection logic, which would
 * otherwise treat this exact scenario as token theft and log everyone
 * out).
 */
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post<ApiEnvelope<{ accessToken: string }>>("/auth/refresh")
      .then((res) => {
        const token = res.data.data.accessToken;
        setAccessToken(token);
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthEndpoint = original?.url?.includes("/auth/login") || original?.url?.includes("/auth/register");

    if (error.response?.status === 401 && original && !original._retried && !isAuthEndpoint) {
      original._retried = true;
      try {
        const newToken = await refreshAccessToken();
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      } catch (refreshError) {
        setAccessToken(null);
        window.dispatchEvent(new CustomEvent("auth:session-expired"));
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);
