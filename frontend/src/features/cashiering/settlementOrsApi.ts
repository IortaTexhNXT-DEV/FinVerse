import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * The commission and incentive ORs of insurer settlements issued once Disbursement approves the
 * payment request (FRS.CSH.07.01.01).
 */

export type SettlementOrStatus = 'PENDING' | 'ISSUED' | 'CANCELLED' | 'FAILED';

export interface SettlementOr {
  id: number;
  sourceModule: string;
  sourceRef: string;
  awaitRef: string;
  orType: string;
  payeeName?: string;
  currency: string;
  amount: number;
  status: SettlementOrStatus;
  receiptNo?: string;
  message?: string;
  createdAt: string;
  issuedAt?: string;
}

export const SETTLEMENT_OR_STATUS_LABELS: Record<SettlementOrStatus, string> = {
  PENDING: 'Waiting for Disbursement approval',
  ISSUED: 'Issued',
  CANCELLED: 'Payment request cancelled',
  FAILED: 'Not issued',
};

/** The batch of a settlement OR (the source reference is batch number and OR type). */
export function batchOf(or: Pick<SettlementOr, 'sourceRef'>): string {
  const colon = or.sourceRef.lastIndexOf(':');
  return colon > 0 ? or.sourceRef.slice(0, colon) : or.sourceRef;
}

/** Only an OR that could not be issued is issued again. */
export function canIssueAgain(or: Pick<SettlementOr, 'status'>): boolean {
  return or.status === 'FAILED';
}

export const settlementOrsApi = {
  list: (companyId: number, page: number) =>
    api.get<PageResponse<SettlementOr>>(
      `/cashiering/settlement-ors${toQuery({ companyId, page, size: 20 })}`,
    ),
  issueAgain: (id: number) => api.post<SettlementOr>(`/cashiering/settlement-ors/${id}/issue`),
};
