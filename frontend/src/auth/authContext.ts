import { createContext, useContext } from 'react';
import type { UserProfile } from '@/api/types';

export interface AuthState {
  user: UserProfile | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  can: (permission: string) => boolean;
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
