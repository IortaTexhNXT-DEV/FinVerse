import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type { Disposition, UnappliedItem } from './cashieringQueueTypes';

/**
 * Requests Cashiering receives from other modules (wave C1-C): collector disposition requests from
 * Collections (BRCLXN.030-033), refund validations from Payment Requests (MKT 1.11.0) and payment
 * reversals from ACSL (ACSL 2.6.0-2.6.1), under /api/v1/cashiering.
 */

export type CollectorRequestStatus = 'QUEUED' | 'ACCEPTED' | 'REJECTED' | 'APPLIED';
export type ValidationStatus = 'OPEN' | 'CONFIRMED' | 'REJECTED';
export type ReversalStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface CollectorRequest {
  id: number;
  requestNo: string;
  unappliedId: number;
  unappliedRef?: string;
  currency?: string;
  balance?: number;
  stage?: string;
  payorName?: string;
  action: 'APPLY_TO_INVOICE' | 'REFUND' | 'RECLASS' | 'TRANSFER';
  invoiceNo?: string;
  amount?: number;
  requestedBy: string;
  requestedAt: string;
  source: string;
  sourceRef: string;
  remarks?: string;
  status: CollectorRequestStatus;
  dispositionId?: number;
  decidedBy?: string;
  decidedAt?: string;
  decisionNote?: string;
}

export interface RefundValidation {
  id: number;
  taskNo: string;
  sourceModule: string;
  sourceRef: string;
  invoiceNo?: string;
  arNo?: string;
  clientCode?: string;
  currency: string;
  amount: number;
  requestedBy: string;
  requestedAt: string;
  requestRemarks?: string;
  status: ValidationStatus;
  unappliedId?: number;
  newArNo?: string;
  decidedBy?: string;
  decidedAt?: string;
  resultRemarks?: string;
}

export interface PaymentReversal {
  id: number;
  requestNo: string;
  sourceModule: string;
  sourceRef: string;
  invoiceNo: string;
  receiptNo: string;
  currency?: string;
  amount?: number;
  valueDate: string;
  reason?: string;
  requestedBy: string;
  requestedAt: string;
  status: ReversalStatus;
  reversedAmount?: number;
  unappliedId?: number;
  decidedBy?: string;
  decidedAt?: string;
  decisionNote?: string;
}

export interface RequestCounts {
  collectorRequests: number;
  refundValidations: number;
  paymentReversals: number;
}

export interface AcceptBody {
  dispositionType?: string;
  amount?: number;
  targetClientCode?: string;
  targetUnit?: string;
  payeeName?: string;
  remarks?: string;
  submit: boolean;
}

const BASE = '/cashiering';

function query(params: Record<string, string | number | readonly string[] | undefined>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    const values = Array.isArray(value) ? (value as readonly string[]) : [value];
    values.filter((v) => v !== undefined && v !== '').forEach((v) => search.append(key, String(v)));
  });
  return `?${search.toString()}`;
}

export const requestsApi = {
  counts: (companyId: number) =>
    api.get<RequestCounts>(`${BASE}/requests/counts${query({ companyId })}`),
  collectorRequests: (
    companyId: number,
    status: readonly CollectorRequestStatus[],
    q: string,
    page: number,
  ) =>
    api.get<PageResponse<CollectorRequest>>(
      `${BASE}/collector-requests${query({ companyId, status, q, page })}`,
    ),
  accept: (id: number, body: AcceptBody) =>
    api.post<Disposition>(`${BASE}/collector-requests/${id}/accept`, body),
  rejectRequest: (id: number, reason: string) =>
    api.post<CollectorRequest>(`${BASE}/collector-requests/${id}/reject`, { reason }),
  validations: (companyId: number, status: readonly ValidationStatus[], page: number) =>
    api.get<PageResponse<RefundValidation>>(
      `${BASE}/refund-validations${query({ companyId, status, page })}`,
    ),
  candidates: (id: number) =>
    api.get<UnappliedItem[]>(`${BASE}/refund-validations/${id}/candidates`),
  confirm: (id: number, unappliedId: number, newArNo?: string, remarks?: string) =>
    api.post<RefundValidation>(`${BASE}/refund-validations/${id}/confirm`, {
      unappliedId,
      newArNo,
      remarks,
    }),
  rejectValidation: (id: number, reason: string) =>
    api.post<RefundValidation>(`${BASE}/refund-validations/${id}/reject`, { reason }),
  reversals: (companyId: number, status: readonly ReversalStatus[], page: number) =>
    api.get<PageResponse<PaymentReversal>>(
      `${BASE}/payment-reversals${query({ companyId, status, page })}`,
    ),
  approveReversal: (id: number) =>
    api.post<PaymentReversal>(`${BASE}/payment-reversals/${id}/approve`),
  rejectReversal: (id: number, reason: string) =>
    api.post<PaymentReversal>(`${BASE}/payment-reversals/${id}/reject`, { reason }),
};

/** The default cashiering disposition type of a collector action. */
export const DEFAULT_TYPE: Record<CollectorRequest['action'], string> = {
  APPLY_TO_INVOICE: 'APPLY_OTHER_INVOICE',
  REFUND: 'REFUND',
  RECLASS: 'RECLASS',
  TRANSFER: 'TRANSFER_UNIT',
};

export interface AcceptForm {
  dispositionType: string;
  amount: string;
  targetClientCode: string;
  targetUnit: string;
  payeeName: string;
  remarks: string;
  submit: boolean;
}

/** Field errors of the acceptance form: the reclass client and the transfer unit are needed. */
export function acceptErrors(
  request: Pick<CollectorRequest, 'action' | 'balance'>,
  form: AcceptForm,
): Partial<Record<keyof AcceptForm, string>> {
  const errors: Partial<Record<keyof AcceptForm, string>> = {};
  if (request.action === 'RECLASS' && form.targetClientCode.trim() === '') {
    errors.targetClientCode = 'Enter the client to reclass to';
  }
  if (request.action === 'TRANSFER' && form.targetUnit.trim() === '') {
    errors.targetUnit = 'Enter the marketing unit to transfer to';
  }
  const amount = form.amount.trim();
  if (amount !== '') {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0 || value > (request.balance ?? Infinity)) {
      errors.amount = 'Enter an amount above zero and at most the unapplied balance';
    }
  }
  return errors;
}

/** The acceptance body of the form. */
export function acceptBody(form: AcceptForm): AcceptBody {
  const text = (v: string) => v.trim() || undefined;
  return {
    dispositionType: text(form.dispositionType),
    amount: form.amount.trim() === '' ? undefined : Number(form.amount),
    targetClientCode: text(form.targetClientCode),
    targetUnit: text(form.targetUnit),
    payeeName: text(form.payeeName),
    remarks: text(form.remarks),
    submit: form.submit,
  };
}
