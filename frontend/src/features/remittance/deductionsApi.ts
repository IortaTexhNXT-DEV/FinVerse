import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import { toQuery } from './api';

/** Remittance deductions on insurer confirmation (ACSL 2.9.2): /api/v1/remittance/deductions. */

export type DeductionStage = 'DRAFT' | 'FOR_CONFIRMATION' | 'CONFIRMED' | 'APPLIED' | 'CANCELLED';

export interface Deduction {
  id: number;
  deductionNo: string;
  insurerCode: string;
  currency: string;
  sourceType: string;
  sourceRef: string;
  invoiceNo?: string;
  amount: number;
  appliedAmount: number;
  remaining: number;
  confirmationRef?: string;
  confirmationDate?: string;
  remarks?: string;
  stage: DeductionStage;
  createdBy: string;
  createdAt: string;
  submittedBy?: string;
  confirmedBy?: string;
  confirmedAt?: string;
}

/** The part of a deduction a batch consumed. */
export interface DeductionApplication {
  id: number;
  deductionId: number;
  deductionNo: string;
  batchId: number;
  batchNo: string;
  sendCycle: number;
  amount: number;
  journalBatchNo?: string;
  reversed: boolean;
  createdAt: string;
}

export interface DeductionInput {
  companyId: number;
  insurerCode: string;
  currency: string;
  sourceType: string;
  sourceRef: string;
  invoiceNo?: string;
  amount: number;
  confirmationRef?: string;
  confirmationDate?: string;
  remarks?: string;
}

const BASE = '/remittance/deductions';

export const deductionsApi = {
  list: (
    companyId: number,
    stages: readonly DeductionStage[],
    filters: { q: string; insurer: string },
    page: number,
  ) =>
    api.get<PageResponse<Deduction>>(
      `${BASE}${toQuery({
        companyId,
        stage: stages,
        q: filters.q || undefined,
        insurer: filters.insurer || undefined,
        page,
        size: 20,
      })}`,
    ),
  get: (id: number) => api.get<Deduction>(`${BASE}/${id}`),
  applications: (id: number) => api.get<DeductionApplication[]>(`${BASE}/${id}/applications`),
  ofBatch: (batchId: number) => api.get<DeductionApplication[]>(`${BASE}/by-batch/${batchId}`),
  pending: (companyId: number, insurer: string, currency: string) =>
    api.get<Deduction[]>(`${BASE}/pending${toQuery({ companyId, insurer, currency })}`),
  create: (body: DeductionInput) => api.post<Deduction>(BASE, body),
  update: (id: number, body: DeductionInput) => api.put<Deduction>(`${BASE}/${id}`, body),
  submit: (id: number, comment?: string) =>
    api.post<Deduction>(`${BASE}/${id}/submit`, { comment }),
  confirm: (id: number, comment?: string) =>
    api.post<Deduction>(`${BASE}/${id}/confirm`, { comment }),
};
