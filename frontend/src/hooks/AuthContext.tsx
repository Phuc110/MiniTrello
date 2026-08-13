import { createContext, useCallback, useEffect, useState, type ReactNode } from "react";
import toast from "react-hot-toast";
import { authApi, type LoginPayload, type RegisterPayload } from "@/api/auth";
import { setAccessToken } from "@/api/axiosClient";
import type { User } from "@/types";

interface AuthContextValue {
  user: User | null;
  /** True while we're attempting the initial silent-refresh bootstrap on page load — distinct from `user === null`, which could just mean "logged out." */
  isBootstrapping: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  // On first load, the browser may already hold a valid httpOnly refresh
  // cookie from a previous session (access tokens live only in memory and
  // don't survive a page reload). Attempt a silent refresh so the person
  // doesn't have to log in again just because they hit F5.
  useEffect(() => {
    authApi
      .refresh()
      .then((res) => {
        setAccessToken(res.accessToken);
        setUser(res.user);
      })
      .catch(() => {
        // No valid session — this is the normal "not logged in" case, not an error.
      })
      .finally(() => setIsBootstrapping(false));
  }, []);

  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
      toast.error("Your session expired. Please log in again.");
    };
    window.addEventListener("auth:session-expired", handleExpired);
    return () => window.removeEventListener("auth:session-expired", handleExpired);
  }, []);

  const login = useCallback(async (payload: LoginPayload) => {
    const res = await authApi.login(payload);
    setAccessToken(res.accessToken);
    setUser(res.user);
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const res = await authApi.register(payload);
    setAccessToken(res.accessToken);
    setUser(res.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, isBootstrapping, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
