import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, onUnauthorized, tokenStore } from '@/api/client';
import type { LoginResponse, UserProfile } from '@/api/types';
import { tabSession } from '@/session/tabSession';
import type { SharedSession } from '@/session/tabSync';
import { AuthContext } from './authContext';

/** How long a new tab waits for an open tab to share its session (BRNB.082). */
const HANDSHAKE_MS = 400;

/**
 * Holds the authenticated user and exposes permission checks for menus and actions. The session
 * is shared with the other tabs of the browser: a new tab takes the session of an open tab, and
 * signing in or out in one tab does the same in all of them (BRNB.082).
 */
export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const signOutHere = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const logout = useCallback(() => {
    if (tokenStore.get() !== null) {
      // Revoke the token on the server (every instance refuses it from now on). The request reads
      // the token before signOutHere clears it; a failure (already expired) changes nothing here.
      api.post('/auth/logout').catch(() => undefined);
    }
    signOutHere();
    tabSession().announceLogout();
  }, [signOutHere]);

  const loadProfile = useCallback(async () => {
    try {
      setUser(await api.get<UserProfile>('/auth/me'));
    } catch {
      signOutHere();
    }
  }, [signOutHere]);

  useEffect(() => {
    const sync = tabSession();
    const adopt = (session: SharedSession) => {
      tokenStore.set(session.token, session.expiresAt);
      void loadProfile();
    };
    const unsubscribe = sync.subscribe((message) => {
      if (message.type === 'logout') {
        signOutHere();
      } else if (message.type === 'login' && tokenStore.get() === null) {
        adopt(message.session);
      }
    });
    onUnauthorized(logout);
    const start = async () => {
      if (tokenStore.get() === null) {
        const shared = await sync.requestSession(HANDSHAKE_MS);
        if (shared !== null) {
          tokenStore.set(shared.token, shared.expiresAt);
        }
      }
      if (tokenStore.get() !== null) {
        await loadProfile();
      }
      setLoading(false);
    };
    void start();
    return unsubscribe;
  }, [logout, loadProfile, signOutHere]);

  const login = useCallback(async (username: string, password: string) => {
    const result = await api.post<LoginResponse>('/auth/login', { username, password });
    tokenStore.set(result.accessToken, result.expiresAt);
    setUser(result.user);
    tabSession().announceLogin({ token: result.accessToken, expiresAt: result.expiresAt });
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
