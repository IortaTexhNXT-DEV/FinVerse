import { api, toQuery } from '@/api/client';
import type { BulkJob, BulkRow, BulkRowStatus } from '@/api/bulk';
import type { GlAccount, GlAccountRequest, Journal } from '@/api/gl';
import type { PageResponse } from '@/api/types';

/** Negative balance control of an account (FRBS 2.5.4). */
export type NegativeBalancePolicy = 'ALLOW' | 'WARN' | 'BLOCK';

/** Account with the BRD-5 controls. */
export type FrbsAccount = GlAccount & { negativeBalancePolicy?: NegativeBalancePolicy };

/** Account request with the negative balance control. */
export type FrbsAccountRequest = GlAccountRequest & {
  negativeBalancePolicy?: NegativeBalancePolicy;
};

/** Journal with assignment, automatic reversal date and correction links (FRBS 2.5.1 / 2.8.1). */
export type FrbsJournal = Journal & {
  assignedTo?: string;
  assignedBy?: string;
  reverseOn?: string;
  correctsBatchId?: number;
  relatedInvoiceNo?: string;
  rootInvoiceNo?: string;
};

/** Numbering scheme of the children of a parent account (FRBS 2.3.2). */
export interface CoaNumbering {
  id: number;
  parentCode: string;
  separator: string;
  width: number;
  active: boolean;
  example: string;
}

/** Outcome of one journal of a bulk posting (FRBS 2.5.6). */
export interface BulkPostOutcome {
  id: number;
  batchNo?: string;
  posted: boolean;
  message: string;
}

/** Chart of accounts upload, numbering, assignment and bulk posting (BRD-5 FRBS sections B and D). */
export const glPlatformApi = {
  uploadTemplate: () => api.getFile('/coa/uploads/template'),
  uploadChart: (companyId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<BulkJob>(`/coa/uploads${toQuery({ companyId })}`, form);
  },
  uploads: (companyId: number) =>
    api.get<PageResponse<BulkJob>>(`/coa/uploads${toQuery({ companyId, size: 10 })}`),
  uploadRows: (id: number, status?: BulkRowStatus) =>
    api.get<PageResponse<BulkRow>>(`/coa/uploads/${id}/rows${toQuery({ status, size: 200 })}`),
  commitUpload: (id: number) => api.post<BulkJob>(`/coa/uploads/${id}/commit`),
  cancelUpload: (id: number) => api.post<BulkJob>(`/coa/uploads/${id}/cancel`),
  uploadReport: (id: number) => api.getFile(`/coa/uploads/${id}/report`),

  numbering: (companyId: number) =>
    api.get<CoaNumbering[]>(`/coa/numbering${toQuery({ companyId })}`),
  saveNumbering: (body: {
    companyId: number;
    parentCode: string;
    separator: string;
    width: number;
    active: boolean;
  }) => api.put<CoaNumbering>('/coa/numbering', body),
  nextCode: (companyId: number, parentCode: string) =>
    api.get<{ code: string }>(`/coa/accounts/next-code${toQuery({ companyId, parentCode })}`),
  lookup: (companyId: number, key: string) =>
    api.get<FrbsAccount>(`/coa/accounts/lookup${toQuery({ companyId, key })}`),

  assignees: () => api.get<string[]>('/journals/assignees'),
  assign: (ids: number[], assignee: string) =>
    api.post<FrbsJournal[]>('/journals/assign', { ids, assignee: assignee || undefined }),
  bulkApprove: (ids: number[]) => api.post<BulkPostOutcome[]>('/journals/bulk-approve', { ids }),
  warnings: (id: number) => api.get<string[]>(`/journals/${id}/warnings`),
};
