import type { PageResponse } from './types';
import { api, toQuery } from './client';

/**
 * The Legacy Inquiry (BRD-13): read-only search of the legacy archive with its documents, the
 * Excel export and the access log reviewed by Compliance. Every search, view, download and export
 * is logged with the reason given.
 */

export interface ArchiveRecord {
  id: number;
  sourceSystem: string;
  recordType: string;
  legacyKey: string;
  clientKey?: string;
  clientName?: string;
  policyNo?: string;
  invoiceNo?: string;
  receiptNo?: string;
  claimNo?: string;
  documentDate: string;
  currency?: string;
  amount?: number;
  status?: string;
  documentCount: number;
}

export interface LegacyDocument {
  id: number;
  fileName: string;
  description?: string;
  sizeBytes: number;
  sha256: string;
}

export interface ArchiveRecordDetail {
  record: ArchiveRecord;
  periodFrom?: string;
  periodTo?: string;
  details: Record<string, string>;
  loadedAt: string;
  documents: LegacyDocument[];
}

export interface ArchiveCriteria {
  client?: string;
  policyNo?: string;
  invoiceNo?: string;
  receiptNo?: string;
  claimNo?: string;
  recordType?: string;
  sourceSystem?: string;
  from?: string;
  to?: string;
}

export interface AccessReason {
  reasonCode: string;
  reasonText: string;
}

export interface InquirySettings {
  reasonRequired: boolean;
  exportMaxRows: number;
  links: { system: string; url: string }[];
}

export interface AccessLogEntry {
  id: number;
  username: string;
  accessedAt: string;
  sourceAddress?: string;
  action: 'SEARCH' | 'VIEW' | 'DOWNLOAD' | 'EXPORT';
  criteria?: string;
  recordKeys?: string;
  resultCount: number;
  reasonCode?: string;
  reasonText?: string;
}

export interface AccessLogFilter {
  username?: string;
  action?: string;
  from?: string;
  to?: string;
}

const BASE = '/legacy-inquiry';

export const legacyInquiryApi = {
  settings: () => api.get<InquirySettings>(`${BASE}/settings`),
  search: (companyId: number, criteria: ArchiveCriteria, reason: AccessReason, page: number) =>
    api.get<PageResponse<ArchiveRecord>>(
      `${BASE}/records${toQuery({ companyId, ...criteria, ...reason, page, size: 25 })}`,
    ),
  view: (id: number, reason: AccessReason) =>
    api.get<ArchiveRecordDetail>(`${BASE}/records/${String(id)}${toQuery({ ...reason })}`),
  document: (id: number, documentId: number, reason: AccessReason) =>
    api.getFile(
      `${BASE}/records/${String(id)}/documents/${String(documentId)}${toQuery({ ...reason })}`,
    ),
  export: (companyId: number, criteria: ArchiveCriteria, reason: AccessReason) =>
    api.getFile(`${BASE}/export${toQuery({ companyId, ...criteria, ...reason })}`),
  accessLog: (companyId: number, filter: AccessLogFilter, page: number) =>
    api.get<PageResponse<AccessLogEntry>>(
      `${BASE}/access-log${toQuery({ companyId, ...filter, page, size: 50 })}`,
    ),
};
