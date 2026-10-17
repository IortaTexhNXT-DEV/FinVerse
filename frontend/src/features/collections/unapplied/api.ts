import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import { toQuery } from '../plans/api';

/**
 * Collector view of unapplied payments (BRCLXN.030-048): /api/v1/collections/unapplied. The
 * items are Cashiering's, read through the Operations port; Collections keeps the collector
 * dispositions and the requests sent to Cashiering.
 */

export type CashieringTab = 'UNAPPLIED' | 'MONITORING' | 'FOR_APPROVAL' | 'FOR_REVERSAL' | 'DONE';
export type CashieringAction = 'APPLY_TO_INVOICE' | 'REFUND' | 'RECLASS' | 'TRANSFER' | 'NONE';
export type RequestStatus = 'SENT' | 'DEFERRED' | 'ACCEPTED' | 'REJECTED' | 'APPLIED';

export interface AccountFacts {
  assuredName?: string;
  prBalance?: number;
  inceptionDate?: string;
  segment?: string;
  salesUnit?: string;
  unitHead?: string;
  aoUsername?: string;
  insurerCode?: string;
  invoiceCategory?: string;
  handler?: string;
}

export interface UnappliedRow {
  unappliedRef: string;
  paymentDate: string;
  ageDays: number;
  paymentFileName?: string;
  transactionNo?: string;
  currency: string;
  amount: number;
  balance: number;
  paymentType?: string;
  payor?: string;
  bankCode?: string;
  checkNo?: string;
  reference?: string;
  clientCode?: string;
  invoiceNo?: string;
  salesUnit?: string;
  cashieringTab: CashieringTab;
  cashieringStatus?: string;
  account?: AccountFacts;
  dispositionCode?: string;
  dispositionInvoiceNo?: string;
  dispositionBy?: string;
  dispositionAt?: string;
}

export interface CollectorDisposition {
  id: number;
  unappliedRef: string;
  dispositionCode: string;
  cashieringAction: CashieringAction;
  invoiceNo?: string;
  amount?: number;
  remarks?: string;
  requestId?: number;
  createdBy: string;
  createdAt: string;
}

export interface CashieringRequest {
  id: number;
  unappliedRef: string;
  action: Exclude<CashieringAction, 'NONE'>;
  invoiceNo?: string;
  amount?: number;
  requestedBy: string;
  requestedAt: string;
  sourceRef: string;
  status: RequestStatus;
  cashieringRef?: string;
  statusMessage?: string;
  statusAt?: string;
  payor?: string;
  currency?: string;
  paidAmount?: number;
  fileRunNo?: string;
}

export interface UnappliedDetail {
  row: UnappliedRow;
  dispositions: CollectorDisposition[];
  requests: CashieringRequest[];
}

export interface DispositionRule {
  code: string;
  label: string;
  requiresInvoice: boolean;
  cashieringAction: CashieringAction;
}

export interface DispositionRules {
  rules: DispositionRule[];
  invoicePattern: string;
}

export interface HistoryEvent {
  at?: string;
  event: string;
  description: string;
  amount?: number;
  by: string;
  reference?: string;
}

export interface UnappliedFilters {
  q?: string;
  tab?: CashieringTab;
  segment?: string;
  disposition?: string;
  clientCode?: string;
  salesUnit?: string;
  ageMin?: number;
  ageMax?: number;
}

export interface DispositionDraft {
  dispositionCode: string;
  invoiceNo?: string;
  amount?: number;
  remarks?: string;
}

const BASE = '/collections/unapplied';

export const unappliedApi = {
  list: (companyId: number, filters: UnappliedFilters, page: number) =>
    api.get<PageResponse<UnappliedRow>>(
      `${BASE}${toQuery({ companyId, ...filters, page, size: 20 })}`,
    ),
  detail: (companyId: number, ref: string) =>
    api.get<UnappliedDetail>(`${BASE}/${encodeURIComponent(ref)}${toQuery({ companyId })}`),
  history: (companyId: number, ref: string) =>
    api.get<HistoryEvent[]>(`${BASE}/${encodeURIComponent(ref)}/history${toQuery({ companyId })}`),
  rules: () => api.get<DispositionRules>(`${BASE}/disposition-rules`),
  dispose: (companyId: number, ref: string, draft: DispositionDraft) =>
    api.post<CollectorDisposition>(`${BASE}/${encodeURIComponent(ref)}/dispositions`, {
      companyId,
      ...draft,
    }),
  requests: (companyId: number, status: readonly RequestStatus[], q: string, page: number) =>
    api.get<PageResponse<CashieringRequest>>(
      `${BASE}/requests${toQuery({ companyId, status, q, page, size: 20 })}`,
    ),
  refresh: (id: number) => api.post<CashieringRequest>(`${BASE}/requests/${id}/refresh`),
};
