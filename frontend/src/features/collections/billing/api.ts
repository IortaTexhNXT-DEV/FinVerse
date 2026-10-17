import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import { toQuery } from '../plans/api';

/** Statements of account API client (BRCLXN.058/060): /api/v1/collections/billing/statements. */

export type StatementStatus = 'GENERATED' | 'SENT' | 'CANCELLED';

export interface StatementLine {
  lineNo: number;
  kind: 'CURRENT' | 'ARREARS';
  invoiceNo?: string;
  policyYear: number;
  installmentSeq: number;
  coverageFrom: string;
  coverageTo: string;
  dueDate: string;
  amount: number;
  paid: number;
  balance: number;
}

export interface Statement {
  id: number;
  soaNo: string;
  planId: number;
  cycleSeq: number;
  arn: string;
  clientCode: string;
  assuredName: string;
  currency: string;
  frequency: string;
  cycleFrom: string;
  cycleTo: string;
  dueDate: string;
  total: number;
  paid: number;
  balance: number;
  status: StatementStatus;
  templateVersion?: string;
  sentAt?: string;
  sentTo?: string;
  cancelReason?: string;
  generatedBy: string;
  createdAt: string;
  lines: StatementLine[];
}

export interface SendStatement {
  to: string[];
  cc: string[];
  subject: string;
  body: string;
  passwordHint?: string;
}

const BASE = '/collections/billing/statements';

export const billingApi = {
  statements: (companyId: number, status: readonly StatementStatus[], q: string, page: number) =>
    api.get<PageResponse<Statement>>(`${BASE}${toQuery({ companyId, status, q, page, size: 20 })}`),
  statement: (id: number) => api.get<Statement>(`${BASE}/${id}`),
  forPlan: (planId: number) => api.get<Statement[]>(`${BASE}/by-plan/${planId}`),
  generate: (planId: number, cycleSeq: number) => api.post<Statement>(BASE, { planId, cycleSeq }),
  generateDue: (companyId: number, from: string, to: string) =>
    api.post<Statement[]>(`${BASE}/generate-due`, { companyId, from, to }),
  document: (id: number) => api.getFile(`${BASE}/${id}/document`),
  recipient: (id: number) => api.get<{ email?: string }>(`${BASE}/${id}/recipient`),
  send: (id: number, mail: SendStatement) => api.post<Statement>(`${BASE}/${id}/send`, mail),
  cancel: (id: number, reason: string) => api.post<Statement>(`${BASE}/${id}/cancel`, { reason }),
};
