import { api, toQuery } from './client';
import type { LoginResponse } from './types';

/** An enrolment of an authenticator app in progress. */
export interface MfaEnrolment {
  /** The secret in Base32, in groups of four, to type into the app. */
  secret: string;
  otpauthUri: string;
  /** QR code as an SVG data: URI. */
  qrCode: string;
  issuer: string;
}

/** A confirmed enrolment: the recovery codes (shown once) and, at sign-in, the open session. */
export interface MfaEnrolled {
  recoveryCodes: string[];
  signIn?: LoginResponse;
}

/** The signed-in user's second factor. */
export interface MyMfaStatus {
  enrolled: boolean;
  enrolledAt?: string;
  lastUsedAt?: string;
  recoveryCodesLeft: number;
  required: boolean;
  available: boolean;
  rememberDeviceDays: number;
}

/** A user and the state of the second factor (administrators). */
export interface UserSecondFactor {
  username: string;
  fullName: string;
  enabled: boolean;
  privileged: boolean;
  enrolled: boolean;
  enrolledAt?: string;
  lastUsedAt?: string;
  resetPending: boolean;
}

export type MfaResetStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';

/** A request to reset a user's second factor (four eyes). */
export interface MfaResetRequest {
  id: number;
  username: string;
  reason: string;
  status: MfaResetStatus;
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionNote?: string;
}

/** The second factor: the sign-in step, My Profile and the administrators' reset. */
export const mfaApi = {
  verify: (challenge: string, code: string, rememberDevice: boolean) =>
    api.post<LoginResponse>('/auth/mfa/verify', { challenge, code, rememberDevice }),
  startAtSignIn: (challenge: string) =>
    api.post<MfaEnrolment>('/auth/mfa/enrolment/start', { challenge }),
  confirmAtSignIn: (challenge: string, code: string) =>
    api.post<MfaEnrolled>('/auth/mfa/enrolment/confirm', { challenge, code }),
  me: () => api.get<MyMfaStatus>('/auth/mfa/me'),
  startMine: () => api.post<MfaEnrolment>('/auth/mfa/me/enrolment/start'),
  confirmMine: (code: string) => api.post<MfaEnrolled>('/auth/mfa/me/enrolment/confirm', { code }),
  newRecoveryCodes: (code: string) => api.post<string[]>('/auth/mfa/me/recovery-codes', { code }),
  users: () => api.get<UserSecondFactor[]>('/admin/mfa/users'),
  resetRequests: (status?: MfaResetStatus) =>
    api.get<MfaResetRequest[]>(`/admin/mfa/reset-requests${toQuery({ status })}`),
  requestReset: (username: string, reason: string) =>
    api.post<MfaResetRequest>('/admin/mfa/reset-requests', { username, reason }),
  approveReset: (id: number) =>
    api.post<MfaResetRequest>(`/admin/mfa/reset-requests/${id}/approve`),
  rejectReset: (id: number, reason: string) =>
    api.post<MfaResetRequest>(`/admin/mfa/reset-requests/${id}/reject`, { reason }),
};
