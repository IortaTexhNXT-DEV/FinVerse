import { api } from './client';

/** One reference-data cache of the platform. */
export interface CacheInfo {
  name: string;
  /** Time to live, ISO-8601 duration (PT15M). */
  ttl: string;
  /** VALKEY: shared by every instance; IN_MEMORY: this instance only. */
  store: 'VALKEY' | 'IN_MEMORY';
  readOnlyTransactionsOnly: boolean;
  invalidatedBy: string[];
}

/** A record class of the document storage: retention and archiving of its files. */
export interface RecordClass {
  code: string;
  name: string;
  bucketClass: string;
  retentionRecordType?: string | null;
  /** Fallback retention, ISO-8601 period (P10Y). */
  retentionPeriod: string;
  legalHold: boolean;
  archiveToEcm: boolean;
  active: boolean;
  description?: string | null;
}

/** New settings of a record class. */
export interface RecordClassSettings {
  retentionRecordType?: string | null;
  retentionPeriod: string;
  legalHold: boolean;
  archiveToEcm: boolean;
  active: boolean;
}

export type HoldAction = 'PLACE' | 'RELEASE';

/** A request to place or release the legal hold of a file. */
export interface LegalHoldRequest {
  id: number;
  storedFileId: number;
  action: HoldAction;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionNote?: string | null;
  fileName?: string | null;
  documentType?: string | null;
}

/** A stored file as the records officers see it (no content). */
export interface HoldFile {
  id: number;
  fileName: string;
  documentType?: string | null;
  recordClass: string;
  legalHold: boolean;
  legalHoldReason?: string | null;
  retentionUntil?: string | null;
  createdBy: string;
  createdAt: string;
}

/** Progress of the copy of the file content of one area into the file store. */
export interface ContentMigrationCount {
  table: string;
  total: number;
  moved: number;
  remaining: number;
}

/** Support functions of the platform (System Administrator, records officers). */
export const supportAdminApi = {
  caches: () => api.get<CacheInfo[]>('/admin/caches'),
  clearCache: (name: string) => api.post<undefined>(`/admin/caches/${name}/clear`),
  clearAllCaches: () => api.post<undefined>('/admin/caches/clear'),
  recordClasses: () => api.get<RecordClass[]>('/files/record-classes'),
  changeRecordClass: (code: string, settings: RecordClassSettings) =>
    api.put<RecordClass>(`/files/record-classes/${code}`, settings),
  pendingHolds: () => api.get<LegalHoldRequest[]>('/files/legal-hold-requests'),
  holdHistory: (fileId: number) =>
    api.get<LegalHoldRequest[]>(`/files/${String(fileId)}/legal-hold-requests`),
  holdStatus: (fileId: number) => api.get<HoldFile>(`/files/${String(fileId)}/hold-status`),
  requestHold: (fileId: number, action: HoldAction, reason: string) =>
    api.post<LegalHoldRequest>(`/files/${String(fileId)}/legal-hold-requests`, { action, reason }),
  approveHold: (id: number, reason: string) =>
    api.post<LegalHoldRequest>(`/files/legal-hold-requests/${String(id)}/approve`, { reason }),
  rejectHold: (id: number, reason: string) =>
    api.post<LegalHoldRequest>(`/files/legal-hold-requests/${String(id)}/reject`, { reason }),
  contentMigration: () => api.get<ContentMigrationCount[]>('/files/content-migration'),
};
