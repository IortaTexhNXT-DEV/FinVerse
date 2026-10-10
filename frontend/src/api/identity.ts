import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Status of an account in the Enterprise SSO platform. */
export type DirectoryStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'DISABLED' | 'DEACTIVATED';

/** What a provisioning event asks for. */
export type IdentityEventType = 'JOINER' | 'MOVER' | 'LEAVER' | 'REHIRE' | 'STATUS';

/** Outcome of a provisioning event. */
export type IdentityEventStatus = 'APPLIED' | 'NO_CHANGE' | 'REFUSED' | 'FAILED';

/** An account of the Enterprise SSO directory (and of its simulator). */
export interface DirectoryAccount {
  windowsId: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName?: string;
  adGroup?: string;
  status?: DirectoryStatus;
  teamLeaderName?: string;
  teamHeadName?: string;
  sectionHeadName?: string;
  unitHeadName?: string;
  unitSegment?: string;
  department?: string;
  location?: string;
  uidmRequestNo?: string;
}

/** A provisioning event received with its outcome. */
export interface IdentityEvent {
  id: number;
  receivedAt: string;
  source: string;
  sourceLabel: string;
  eventType: IdentityEventType;
  windowsId?: string;
  userId?: string;
  uidmRequestNo?: string;
  adStatus?: DirectoryStatus;
  status: IdentityEventStatus;
  username?: string;
  message?: string;
  processedAt?: string;
  processedBy?: string;
  attempts: number;
  reprocessable: boolean;
}

/** The directory details of a user and the latest synchronisation. */
export interface DirectoryProfile extends Omit<DirectoryAccount, 'userId' | 'email' | 'status'> {
  username: string;
  adEmail?: string;
  adStatus?: DirectoryStatus;
  adSyncAt?: string;
  lastEventType?: IdentityEventType;
  lastEventAt?: string;
  lastEventSource?: string;
  syncStatus: 'SYNCED' | 'FAILED';
  syncMessage?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

/** How users are provisioned on this deployment. */
export interface IdentitySettings {
  provisioning: boolean;
  directoryName: string;
  simulator: boolean;
}

/** Filters of the events. */
export interface IdentityEventFilter {
  status?: string;
  windowsId?: string;
  from?: string;
  to?: string;
  page?: number;
}

const BASE = '/admin/identity';

/** Identity synchronisation for the System Administrator. */
export const identityApi = {
  settings: () => api.get<IdentitySettings>(`${BASE}/settings`),
  events: (filter: IdentityEventFilter) =>
    api.get<PageResponse<IdentityEvent>>(`${BASE}/events${toQuery({ ...filter, size: 25 })}`),
  reprocess: (id: number) => api.post<IdentityEvent>(`${BASE}/events/${id}/reprocess`),
  synchronise: (username: string) =>
    api.post<IdentityEvent>(`${BASE}/users/${encodeURIComponent(username)}/sync`),
  profile: (username: string) =>
    api.get<DirectoryProfile | undefined>(`${BASE}/users/${encodeURIComponent(username)}/profile`),
  lookup: (windowsId: string) =>
    api.get<DirectoryAccount>(`${BASE}/directory${toQuery({ windowsId })}`),
  createFromDirectory: (windowsId: string) =>
    api.post<IdentityEvent>(`${BASE}/users/from-directory`, { windowsId }),
  simulatorAccounts: () => api.get<DirectoryAccount[]>(`${BASE}/simulator/accounts`),
  addSimulatorAccount: (account: DirectoryAccount) =>
    api.post<DirectoryAccount>(`${BASE}/simulator/accounts`, account),
  changeSimulatorAccount: (account: DirectoryAccount) =>
    api.put<DirectoryAccount>(
      `${BASE}/simulator/accounts${toQuery({ windowsId: account.windowsId })}`,
      account,
    ),
  sendSimulatorEvent: (windowsId: string, type: IdentityEventType) =>
    api.post<IdentityEvent>(`${BASE}/simulator/accounts/events/${type}${toQuery({ windowsId })}`),
};

/** Event types in words. */
export const EVENT_TYPE_LABELS: Record<IdentityEventType, string> = {
  JOINER: 'Joiner',
  MOVER: 'Mover',
  LEAVER: 'Leaver',
  REHIRE: 'Rehire',
  STATUS: 'Status change',
};

/** Outcomes in words. */
export const EVENT_STATUS_LABELS: Record<IdentityEventStatus, string> = {
  APPLIED: 'Applied',
  NO_CHANGE: 'No change',
  REFUSED: 'Refused',
  FAILED: 'Failed',
};

/** AD statuses in words. */
export const DIRECTORY_STATUS_LABELS: Record<DirectoryStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  LOCKED: 'Locked',
  DISABLED: 'Disabled',
  DEACTIVATED: 'Deactivated',
};
