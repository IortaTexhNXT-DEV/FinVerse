import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type { TemplateField } from '../cases/api';

/**
 * Suspicious transaction reports API client (BRD-10, SNSRP-705, 706): the STR of a case, the
 * register, the extraction of committee-approved STRs and the AMLC filing reference.
 */

export type StrStatus = 'DRAFT' | 'FOR_APPROVAL' | 'APPROVED' | 'EXTRACTED' | 'FILED';

export interface StrRow {
  id: number;
  strNo: string;
  caseId: number;
  subjectCode: string;
  subjectName: string;
  status: StrStatus;
  reasonCodes: string[];
  committeeDecidedAt?: string | null;
  readyAt?: string | null;
  extractionId?: number | null;
  extractedAt?: string | null;
  amlcReference?: string | null;
  filedOn?: string | null;
  createdAt: string;
  createdBy: string;
}

export interface StrTransaction {
  reference: string;
  date: string;
  amount: number | string;
  currency: string;
  type: string;
  description?: string | null;
}

export interface StrDetail {
  str: StrRow;
  subjectSnapshot: string;
  templateVersionId?: number | null;
  fields: TemplateField[];
  values: Record<string, string | null>;
  transactions: StrTransaction[];
  gaps: Record<string, string>;
}

export interface StrExtraction {
  id: number;
  batchNo: string;
  periodFrom: string;
  periodTo: string;
  strCount: number;
  fileName: string;
  sha256: string;
  reExtraction: boolean;
  reason?: string | null;
  extractedBy: string;
  extractedAt: string;
}

export interface ExtractRequest {
  companyId: number;
  from: string;
  to: string;
  reason?: string;
}

export interface StrSave {
  values: Record<string, string | null>;
  reasonCodes: string[];
  transactions: StrTransaction[];
}

const BASE = '/screening';

export const strApi = {
  register: (params: { companyId: number; status?: string; page: number }) =>
    api.get<PageResponse<StrRow>>(`${BASE}/str${toQuery({ ...params })}`),
  ofCase: (caseId: number) => api.get<StrDetail | undefined>(`${BASE}/cases/${caseId}/str`),
  prepare: (caseId: number) => api.post<StrDetail>(`${BASE}/cases/${caseId}/str`),
  save: (id: number, request: StrSave) => api.put<StrDetail>(`${BASE}/str/${id}`, request),
  ready: (id: number) => api.post<StrDetail>(`${BASE}/str/${id}/ready`),
  document: (id: number, format: 'PDF' | 'DOCX') =>
    api.getFile(`${BASE}/str/${id}/document${toQuery({ format })}`),
  preview: (request: ExtractRequest) =>
    api.post<StrRow[]>(`${BASE}/str/extractions/preview`, request),
  extract: (request: ExtractRequest) => api.post<StrExtraction>(`${BASE}/str/extractions`, request),
  extractions: (companyId: number, page: number) =>
    api.get<PageResponse<StrExtraction>>(`${BASE}/str/extractions${toQuery({ companyId, page })}`),
  file: (id: number) => api.getFile(`${BASE}/str/extractions/${id}/file`),
  filing: (id: number, request: { reference: string; filedOn: string }) =>
    api.post<StrRow>(`${BASE}/str/${id}/filing`, request),
};
