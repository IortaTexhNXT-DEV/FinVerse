import { api, toQuery } from './client';

/** An item waiting for the current user's approval (any module). */
export interface PendingApproval {
  module: string;
  type: string;
  reference: string;
  description?: string;
  amount?: number;
  currency?: string;
  submittedBy?: string;
  submittedAt?: string;
  companyId?: number;
  link?: string;
}

export interface ApprovalCounts {
  total: number;
  byModule: Record<string, number>;
}

export const approvalsApi = {
  inbox: (companyId?: number) =>
    api.get<PendingApproval[]>(`/approvals/inbox${toQuery({ companyId })}`),
  counts: (companyId?: number) =>
    api.get<ApprovalCounts>(`/approvals/counts${toQuery({ companyId })}`),
};
