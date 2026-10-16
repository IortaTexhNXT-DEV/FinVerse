import { api, toQuery } from './client';
import type { JobRun, SystemParameter } from './system';

/** A group profile a user holds, with where it came from and since when. */
export interface HeldGroupProfile {
  code: string;
  name: string;
  active: boolean;
  /** Who gave it: UIDM-ISC over the provisioning interface, or the user of an earlier change. */
  source?: string;
  since?: string;
  requestNo?: string;
}

/** A permission a user holds, with the active group profiles that grant it. */
export interface EffectivePermission {
  permission: string;
  /** Functional area; absent when the permission is not classified. */
  area?: string | null;
  groupProfiles: string[];
}

/** One logged change of a user's access. */
export interface AccessChange {
  id: number;
  occurredAt: string;
  activity: string;
  attribute: string;
  fromValue?: string;
  toValue?: string;
  requestNo?: string;
  doneBy: string;
  approvedBy?: string;
}

/** The state of the connection with UIDM-ISC. */
export interface IdentityStatus {
  lastCall?: string;
  lastSuccess?: string;
  errorsLast24Hours: number;
  refusedOpen: number;
  mismatchesOpen: number;
  lastSyncAt?: string;
  lastSyncStatus?: string;
  lastSyncMessage?: string;
}

export type MismatchKind = 'NOT_IN_DIRECTORY' | 'STATUS' | 'GROUP_PROFILES';

/** A difference between a user and the UIDM-ISC directory found by the reconciliation. */
export interface ReconciliationMismatch {
  id: number;
  runDate: string;
  username: string;
  windowsId?: string;
  kind: MismatchKind;
  detail: string;
  foundAt: string;
  resolvedBy?: string;
  resolvedAt?: string;
  resolutionRemark?: string;
}

/** A user whose access breaches an active separation-of-duties rule. */
export interface SodViolation {
  ruleCode: string;
  kind: 'PROFILES' | 'PERMISSIONS';
  first: string;
  second: string;
  description: string;
  username: string;
  fullName: string;
  groupProfiles: string;
  foundAt?: string;
}

/** A certification extract taken. */
export interface CertificationLogEntry {
  fileName: string;
  format: 'CSV' | 'XLSX';
  rows: number;
  sha256: string;
  takenBy: string;
  takenAt: string;
}

const USERS = '/admin/users';
const IDENTITY = '/admin/identity';
const SETTINGS = '/nbadmin/uam-settings';

/** The User Access screens: user detail, identity monitoring, oversight and settings. */
export const userAccessApi = {
  groupProfilesOf: (id: number) =>
    api.get<HeldGroupProfile[]>(`${USERS}/${String(id)}/group-profiles`),
  permissionsOf: (id: number) =>
    api.get<EffectivePermission[]>(`${USERS}/${String(id)}/permissions`),
  accessHistory: (id: number) => api.get<AccessChange[]>(`${USERS}/${String(id)}/access-history`),
  endSessions: (username: string) =>
    api.post<{ ended: number }>(`/admin/sessions/users/${encodeURIComponent(username)}/end`),
  identityStatus: () => api.get<IdentityStatus>(`${IDENTITY}/status`),
  synchroniseNow: () => api.post<JobRun>(`${IDENTITY}/reconciliation/run`),
  mismatches: (open: boolean) =>
    api.get<ReconciliationMismatch[]>(`${IDENTITY}/reconciliation${toQuery({ open })}`),
  resolveMismatch: (id: number, remark: string) =>
    api.post<undefined>(`${IDENTITY}/reconciliation/${String(id)}/resolve`, { remark }),
  dismissEvent: (id: number, remark: string) =>
    api.post<undefined>(`${IDENTITY}/events/${String(id)}/dismiss`, { remark }),
  sodViolations: () => api.get<SodViolation[]>('/nbadmin/sod-rules/violations'),
  takeExtract: (format: 'CSV' | 'XLSX') =>
    api.download('/nbadmin/certification/extract', { format }),
  extracts: () => api.get<CertificationLogEntry[]>('/nbadmin/certification/extracts'),
  settings: () => api.get<SystemParameter[]>(SETTINGS),
  changeSetting: (key: string, value: string) =>
    api.put<SystemParameter>(`${SETTINGS}/${key}`, { value }),
  approveSetting: (key: string) => api.post<SystemParameter>(`${SETTINGS}/${key}/approve`),
  rejectSetting: (key: string, reason?: string) =>
    api.post<SystemParameter>(`${SETTINGS}/${key}/reject`, { reason }),
};
