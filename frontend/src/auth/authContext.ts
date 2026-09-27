import { createContext, useContext } from 'react';
import type { PasswordChangeReason } from '@/api/auth';
import type { UserProfile } from '@/api/types';

/** Why a session is signed out by the web client (the user's own sign-out when absent). */
export type SignOutReason = 'IDLE_TIMEOUT' | 'EXPIRED';

export interface AuthState {
  user: UserProfile | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: (reason?: SignOutReason) => void;
  can: (permission: string) => boolean;
  /** Set while the password must be changed before working (UAM-NFR-36; FR-UA-005). */
  passwordChange: PasswordChangeReason | null;
  /** Called once the due password change is done. */
  passwordChanged: () => void;
}

export const AuthContext = createContext<AuthState | null>(null);

/** Authenticated user and permission checks (see AuthProvider). */
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
