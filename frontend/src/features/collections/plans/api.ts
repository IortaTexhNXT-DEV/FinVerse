import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Collections plans API client (BRCLXN.053/054/055): installment plans, installments due and
 * promises to pay, under /api/v1/collections.
 */

export type PlanStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type PlanSource = 'POLICY_YEARS' | 'GENERATED' | 'MANUAL';
export type InstallmentStatus = 'NOT_DUE' | 'DUE' | 'OVERDUE' | 'PARTIAL' | 'PAID';
export type PromiseStatus = 'OPEN' | 'KEPT' | 'PARTIALLY_KEPT' | 'BROKEN' | 'CANCELLED';

export interface Installment {
  id: number;
  seq: number;
  policyYear: number;
  invoiceNo?: string;
  dueDate: string;
  cycleFrom: string;
  cycleTo: string;
  amount: number;
  paidAmount: number;
  balance: number;
  status: InstallmentStatus;
  overdueSince?: string;
  paidOn?: string;
}

export interface Plan {
  id: number;
  planNo: string;
  arn: string;
  invoiceNo?: string;
  clientCode: string;
  assuredName: string;
  currency: string;
  frequency: string;
  source: PlanSource;
  firstDue: string;
  installmentCount: number;
  total: number;
  paidTotal: number;
  balance: number;
  status: PlanStatus;
  remarks?: string;
  cancelReason?: string;
  refreshedAt?: string;
  createdBy: string;
  createdAt: string;
  installments: Installment[];
}

export interface DueInstallment {
  planId: number;
  planNo: string;
  arn: string;
  clientCode: string;
  assuredName: string;
  currency: string;
  installment: Installment;
}

export interface PaymentPromise {
  id: number;
  invoiceNo: string;
  arn: string;
  clientCode: string;
  assuredName: string;
  installmentId?: number;
  promisedOn: string;
  promisedDate: string;
  promisedAmount: number;
  currency: string;
  remarks?: string;
  status: PromiseStatus;
  evaluatedAt?: string;
  actualPaid?: number;
  actualDate?: string;
  closingNote?: string;
  bulkRef?: string;
  recordedBy: string;
  createdAt: string;
}

/** A new plan: over the policy years of an account, or splitting one invoice. */
export type NewPlan =
  | { kind: 'POLICY_YEARS'; companyId: number; arn: string; frequency: string; remarks?: string }
  | {
      kind: 'GENERATED';
      companyId: number;
      invoiceNo: string;
      frequency: string;
      firstDue: string;
      count: number;
      remarks?: string;
    };

export interface PromiseInput {
  companyId: number;
  invoiceNo: string;
  promisedOn?: string;
  promisedDate: string;
  amount?: number;
  installmentId?: number;
  remarks?: string;
}

export interface BulkPromiseInput {
  companyId: number;
  invoiceNos: string[];
  promisedOn?: string;
  promisedDate: string;
  amount?: number;
  remarks?: string;
}

/** Outcome of a bulk action on one invoice. */
export interface ItemResult {
  reference: string;
  ok: boolean;
  message: string;
}

type QueryValue = string | number | boolean | null | undefined | readonly string[];

/** A query string; arrays repeat their key (?status=A&status=B), empty values are left out. */
export function toQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    const values = Array.isArray(value) ? (value as readonly string[]) : [value];
    values
      .filter((v) => v !== undefined && v !== null && v !== '')
      .forEach((v) => search.append(key, String(v)));
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

const PLANS = '/collections/plans';
const PROMISES = '/collections/promises';

export const plansApi = {
  plans: (companyId: number, status: readonly PlanStatus[], q: string, page: number) =>
    api.get<PageResponse<Plan>>(`${PLANS}${toQuery({ companyId, status, q, page, size: 20 })}`),
  plan: (id: number) => api.get<Plan>(`${PLANS}/${id}`),
  create: (input: NewPlan) => {
    const { kind, ...body } = input;
    return api.post<Plan>(
      `${PLANS}/${kind === 'POLICY_YEARS' ? 'policy-years' : 'generated'}`,
      body,
    );
  },
  refresh: (id: number) => api.post<Plan>(`${PLANS}/${id}/refresh`),
  cancel: (id: number, reason: string) => api.post<Plan>(`${PLANS}/${id}/cancel`, { reason }),
  due: (companyId: number, overdueOnly: boolean, page: number) =>
    api.get<PageResponse<DueInstallment>>(
      `${PLANS}/installments/due${toQuery({ companyId, overdueOnly, page, size: 20 })}`,
    ),
  promises: (companyId: number, status: readonly PromiseStatus[], q: string, page: number) =>
    api.get<PageResponse<PaymentPromise>>(
      `${PROMISES}${toQuery({ companyId, status, q, page, size: 20 })}`,
    ),
  promisesOf: (invoiceNo: string) =>
    api.get<PaymentPromise[]>(`${PROMISES}/by-invoice/${encodeURIComponent(invoiceNo)}`),
  recordPromise: (input: PromiseInput) => api.post<PaymentPromise>(PROMISES, input),
  cancelPromise: (id: number, reason: string) =>
    api.post<PaymentPromise>(`${PROMISES}/${id}/cancel`, { reason }),
  bulkPromise: (input: BulkPromiseInput) =>
    api.post<ItemResult[]>('/collections/bulk/promises', input),
};
