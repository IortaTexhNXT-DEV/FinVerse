import { api, toQuery } from './client';

/**
 * The opening-balance adjustments of the year-end cut-over (FY2027 true-ups, BRD-13): prepared by
 * the Comptrollership GL lead, approved by the Head of Comptrollership, posted by the load of the
 * adjustment batch, reconciled and signed.
 */

export type TrueUpStatus =
  'PREPARED' | 'FOR_APPROVAL' | 'APPROVED' | 'POSTED' | 'RECONCILED' | 'SIGNED';

export interface TrueUp {
  reference: string;
  trueupNo: string;
  asOf: string;
  status: TrueUpStatus;
  batchNo?: string;
  tbBatchNo?: string;
  journalsPosted: number;
  itemsAdjusted: number;
  preparedBy: string;
  preparedAt: string;
  approvedBy?: string;
  approvedAt?: string;
  postedAt?: string;
  signedBy?: string;
  signedAt?: string;
  remarks?: string;
}

export interface TrueUpInput {
  trueupNo: string;
  asOf: string;
  batchNo: string;
  tbBatchNo?: string;
}

const BASE = '/migration/trueups';

export const migrationGlApi = {
  trueups: (companyId: number) => api.get<TrueUp[]>(`${BASE}${toQuery({ companyId })}`),
  prepare: (companyId: number, input: TrueUpInput) =>
    api.post<TrueUp>(`${BASE}${toQuery({ companyId })}`, input),
  submit: (reference: string, note: string) =>
    api.post<TrueUp>(`${BASE}/${reference}/submit`, { note }),
  decide: (reference: string, approve: boolean, note: string) =>
    api.post<TrueUp>(`${BASE}/${reference}/decide`, { approve, note }),
  reconcile: (reference: string) => api.post<TrueUp>(`${BASE}/${reference}/reconcile`),
  sign: (reference: string) => api.post<TrueUp>(`${BASE}/${reference}/sign`),
};
