import { api, toQuery } from './client';
import type { LoginResponse, PageResponse, UserProfile } from './types';

/** Why a signed-in session ended (UAM-NFR-35; TOKEN_REUSED: a replaced renewal token came back). */
export type SessionEndReason =
  'LOGOUT' | 'IDLE_TIMEOUT' | 'EXPIRED' | 'ADMIN_ENDED' | 'LOCKED' | 'TOKEN_REUSED';

/** How users sign in on this installation (read before anyone is signed in). */
export interface SignInOptions {
  mode: 'LOCAL' | 'DIRECTORY' | 'OIDC' | 'SAML';
  singleSignOn: boolean;
  providerLabel?: string;
  /** The password form is the main sign-in (otherwise only for the break-glass administrators). */
  passwordSignIn: boolean;
  passwordReset: boolean;
  /** Kind of environment (local, sit, uat, training, preprod, production). */
  environment?: string;
}

/** A sign-in session of the session log (FR-UA-004). */
export interface UserSessionEntry {
  sessionId: string;
  username: string;
  issuedAt: string;
  lastSeenAt: string;
  expiresAt: string;
  endedAt?: string;
  endReason?: SessionEndReason;
  open: boolean;
}

/** The password rules in force and the signed-in user's password dates (UAM-NFR-36). */
export interface PasswordStatus {
  authMode: 'LOCAL' | 'DIRECTORY' | 'OIDC' | 'SAML';
  historyCount: number;
  minAgeDays: number;
  maxAgeDays: number;
  passwordChangedAt?: string;
  passwordExpiresAt?: string;
  changeDue: boolean;
  changeReason?: PasswordChangeReason;
}

/** Why the password must be changed before working: set by an administrator, or too old. */
export type PasswordChangeReason = 'RESET' | 'EXPIRED';

export interface ContactDetails {
  email: string;
  mobileNo: string;
}

/** Sign-in, own profile, own password and the session log (BRD-11; FR-UA-001 to 005). */
export const authApi = {
  passwordStatus: () => api.get<PasswordStatus>('/auth/password-status'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<undefined>('/auth/change-password', { currentPassword, newPassword }),
  updateContact: (details: ContactDetails) => api.put<UserProfile>('/auth/me', details),
  mySessions: (page = 0, size = 10) =>
    api.get<PageResponse<UserSessionEntry>>(`/auth/sessions${toQuery({ page, size })}`),
  /** "Forgot password?": the answer is the same for every user ID. */
  requestReset: (userId: string) => api.post<undefined>('/auth/password-reset/request', { userId }),
  signInOptions: () => api.get<SignInOptions>('/auth/sign-in-options'),
  /** Address of the identity provider's sign-in page. */
  startSingleSignOn: () => api.get<{ redirectUrl: string }>('/auth/sso/start'),
  /** Exchanges the one-time ticket of the identity provider's answer for the session. */
  completeSingleSignOn: (ticket: string) =>
    api.post<LoginResponse>('/auth/sso/complete', { ticket }),
  checkReset: (token: string) =>
    api.post<{ expiresAt: string }>('/auth/password-reset/check', { token }),
  confirmReset: (token: string, newPassword: string) =>
    api.post<undefined>('/auth/password-reset/confirm', { token, newPassword }),
  sessions: (params: { username?: string; open?: boolean; page?: number; size?: number }) =>
    api.get<PageResponse<UserSessionEntry>>(`/admin/sessions${toQuery(params)}`),
  online: () => api.get<string[]>('/admin/sessions/online'),
  endSession: (sessionId: string) =>
    api.post<UserSessionEntry>(`/admin/sessions/${encodeURIComponent(sessionId)}/end`),
};
