import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, onUnauthorized, tokenStore } from '@/api/client';
import type { LoginResponse, UserProfile } from '@/api/types';
import { AuthContext } from './authContext';

/** Holds the authenticated user and exposes permission checks for menus and actions. */
export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(() => tokenStore.get() !== null);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    onUnauthorized(logout);
    if (tokenStore.get() === null) {
      return;
    }
    api
      .get<UserProfile>('/auth/me')
      .then(setUser)
      .catch(logout)
      .finally(() => {
        setLoading(false);
      });
  }, [logout]);

  const login = useCallback(async (username: string, password: string) => {
    const result = await api.post<LoginResponse>('/auth/login', { username, password });
    tokenStore.set(result.accessToken);
    setUser(result.user);
  }, []);

  const can = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user],
  );

  const value = useMemo(
    () => ({ user, loading, login, logout, can }),
    [user, loading, login, logout, can],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
