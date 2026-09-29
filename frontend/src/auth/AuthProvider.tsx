import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '@/api/auth';
import type { PasswordChangeReason } from '@/api/auth';
import { api, onUnauthorized, tokenStore } from '@/api/client';
import type { LoginResponse, UserProfile } from '@/api/types';
import { tabSession } from '@/session/tabSession';
import type { SharedSession } from '@/session/tabSync';
import { AuthContext } from './authContext';
import type { SignOutReason } from './authContext';
import { useServerKeepAlive } from './useServerKeepAlive';
import { loadUserDirectory, resetUserDirectory } from '@/api/users';
import { modulesApi } from '@/api/modules';
import type { ModulesInUse } from '@/api/modules';
import { mayUse } from '@/navigation/productModules';

const ALL_MODULES_ON: ModulesInUse = { switchedOff: [], inactivePermissions: [] };

/** The product modules switched off; all modules count as on when the server cannot say. */
async function modulesInUse(): Promise<ModulesInUse> {
  try {
    return await modulesApi.inUse();
  } catch {
    return ALL_MODULES_ON;
  }
}

/** How long a new tab waits for an open tab to share its session (BRNB.082). */
const HANDSHAKE_MS = 400;

/** Key of the devices remembered for the second factor, by user (only when the policy allows). */
const DEVICE_KEY = 'brokerverse.mfaDevices';

function devices(): Record<string, string> {
  try {
    return JSON.parse(localStorage.getItem(DEVICE_KEY) ?? '{}') as Record<string, string>;
  } catch {
    return {};
  }
}

/** The token of this device for a user, when it was remembered for the second factor. */
function rememberedDevice(username: string): string | undefined {
  return devices()[username.trim().toLowerCase()];
}

function rememberDevice(username: string, token: string | undefined): void {
  if (token === undefined) {
    return;
  }
  try {
    localStorage.setItem(
      DEVICE_KEY,
      JSON.stringify({ ...devices(), [username.trim().toLowerCase()]: token }),
    );
  } catch {
    // Storage unavailable: the code is asked again next time.
  }
}

/** The password change due for a profile: a reset one at once, an expired one from the server. */
async function dueChange(profile: UserProfile): Promise<PasswordChangeReason | null> {
  if (profile.mustChangePassword === true) {
    return 'RESET';
  }
  try {
    const status = await authApi.passwordStatus();
    return status.changeDue ? (status.changeReason ?? 'EXPIRED') : null;
  } catch {
    return null;
  }
}

/**
 * Holds the authenticated user and exposes permission checks for menus and actions. The session
 * is shared with the other tabs of the browser: a new tab takes the session of an open tab, and
 * signing in or out in one tab does the same in all of them (BRNB.082). A password set by an
 * administrator or older than the maximum age must be changed before the home page opens
 * (UAM-NFR-36); the sign-out tells the server why the session ends (UAM-NFR-35).
 */
export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [passwordChange, setPasswordChange] = useState<PasswordChangeReason | null>(null);
  const [modules, setModules] = useState<ModulesInUse>(ALL_MODULES_ON);

  const signOutHere = useCallback(() => {
    tokenStore.clear();
    resetUserDirectory();
    setUser(null);
    setPasswordChange(null);
  }, []);

  const logout = useCallback(
    (reason?: SignOutReason) => {
      if (tokenStore.get() !== null) {
        // Revoke the token on the server (every instance refuses it from now on). The request
        // reads the token before signOutHere clears it; a failure (already expired) changes
        // nothing here.
        const query = reason === undefined ? '' : `?reason=${reason}`;
        api.post(`/auth/logout${query}`).catch(() => undefined);
      }
      signOutHere();
      tabSession().announceLogout();
    },
    [signOutHere],
  );

  const loadProfile = useCallback(async () => {
    try {
      const profile = await api.get<UserProfile>('/auth/me');
      setPasswordChange(await dueChange(profile));
      setModules(await modulesInUse());
      setUser(profile);
      void loadUserDirectory();
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

  useServerKeepAlive(user !== null);

  const completeSignIn = useCallback((result: LoginResponse) => {
    if (result.accessToken === undefined || result.user === undefined) {
      return;
    }
    tokenStore.set(result.accessToken, result.expiresAt, result.accessTokenExpiresAt);
    rememberDevice(result.user.username, result.deviceToken);
    setPasswordChange(
      result.mustChangePassword === true ? (result.passwordChangeReason ?? 'RESET') : null,
    );
    setModules(await modulesInUse());
    setUser(result.user);
    void loadUserDirectory();
    tabSession().announceLogin({ token: result.accessToken, expiresAt: result.expiresAt });
  }, []);

  const login = useCallback(
    async (username: string, password: string) => {
      const result = await api.post<LoginResponse>('/auth/login', {
        username,
        password,
        deviceToken: rememberedDevice(username),
      });
      completeSignIn(result);
      return result;
    },
    [completeSignIn],
  );

  const passwordChanged = useCallback(() => {
    setPasswordChange(null);
    setUser((current) => (current === null ? null : { ...current, mustChangePassword: false }));
  }, []);

  // Permissions of switched-off product modules grant nothing; "MODULE_OFF:<code>" asks whether a
  // product module is switched off (menus and widgets of the module).
  const can = useCallback(
    (permission: string) =>
      user !== null && mayUse(permission, modules, (p) => user.permissions.includes(p)),
    [user, modules],
  );

  const value = useMemo(
    () => ({ user, loading, login, completeSignIn, logout, can, passwordChange, passwordChanged }),
    [user, loading, login, completeSignIn, logout, can, passwordChange, passwordChanged],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
