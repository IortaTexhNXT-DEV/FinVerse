import { api } from './client';
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

/** The second factor of the local test accounts: the sign-in step and My Profile. */
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
};
