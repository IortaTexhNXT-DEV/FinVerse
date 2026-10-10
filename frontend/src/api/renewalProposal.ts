import { api, toQuery } from './client';

/** Proposals, TSU requests, KYC monitoring and risk codes of the renewal accounts. */
export interface ProposalView {
  proposalNo: string;
  kind: string;
  fileName: string;
  attachmentId: number | null;
  signatories: string | null;
  status: string;
  messageNo: string | null;
  generatedBy: string;
  generatedAt: string;
}

export interface TsuQuote {
  insurerCode: string;
  premium: number | null;
  terms: string | null;
  selected: boolean;
}

export interface TsuView {
  requestNo: string;
  renewalRef: string;
  clientName: string;
  riskCode: string | null;
  insurerCode: string | null;
  accountOfficer: string | null;
  status: string;
  remarks: string | null;
  decisionRemarks: string | null;
  tsuOfficer: string | null;
  proposalAttachmentId: number | null;
  quotes: TsuQuote[];
}

export interface KycMonitoringView {
  status: string | null;
  kycReviewDue: string | null;
  history: {
    status: string;
    activity: string | null;
    remarks: string | null;
    created_by: string;
    created_at: string;
  }[];
}

export interface KycAccount {
  renewalRef: string;
  clientName: string;
  kycReviewDue: string | null;
  status: string;
  accountOfficer: string | null;
  expiryDate: string;
}

export interface RiskCodeView {
  id: number;
  riskCode: string;
  description: string | null;
  lineCode: string | null;
  renewable: boolean;
  effectiveDate: string;
  remarks: string | null;
  status: string;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface RiskCodeEntry {
  riskCode: string;
  description: string;
  lineCode: string | null;
  renewable: boolean;
  effectiveDate: string;
  remarks: string | null;
}

const R = '/renewal';
const co = (companyId: number) => toQuery({ companyId });
const c = (ref: string) => `${R}/candidates/${encodeURIComponent(ref)}`;
const t = (no: string) => `${R}/tsu-requests/${encodeURIComponent(no)}`;

export const renewalProposalApi = {
  proposals: (companyId: number, ref: string) =>
    api.get<{ requiredSignatories: number; proposals: ProposalView[] }>(
      `${c(ref)}/proposals${co(companyId)}`,
    ),
  generate: (companyId: number, ref: string, kind: string, signatories: string[]) =>
    api.post<ProposalView>(`${c(ref)}/proposals${co(companyId)}`, { kind, signatories }),
  send: (companyId: number, ref: string, proposalNo: string, to: string[], cc: string[]) =>
    api.post<{ message: string }>(
      `${c(ref)}/proposals/${encodeURIComponent(proposalNo)}/send${co(companyId)}`,
      { to, cc },
    ),
  users: (permission: string) =>
    api.get<{ username: string; displayName: string }[]>(
      `${R}/proposal-users${toQuery({ permission })}`,
    ),
  tsuRequests: (companyId: number, ref: string) =>
    api.get<TsuView[]>(`${c(ref)}/tsu-requests${co(companyId)}`),
  tsuQueue: (companyId: number, status: string) =>
    api.get<TsuView[]>(`${R}/tsu-requests${toQuery({ companyId, status })}`),
  createTsu: (companyId: number, ref: string, remarks: string) =>
    api.post<TsuView>(`${c(ref)}/tsu-requests${co(companyId)}`, { remarks }),
  resubmitTsu: (companyId: number, no: string) =>
    api.post<unknown>(`${t(no)}/submit${co(companyId)}`, {}),
  decideTsu: (companyId: number, no: string, decision: string, remarks: string) =>
    api.post<unknown>(`${t(no)}/decision${co(companyId)}`, { decision, remarks }),
  assignTsu: (companyId: number, no: string, officer: string) =>
    api.post<unknown>(`${t(no)}/assign${co(companyId)}`, { officer }),
  quoteTsu: (
    companyId: number,
    no: string,
    quote: { insurerCode: string; premium: number | null; terms: string },
  ) => api.post<unknown>(`${t(no)}/quotes${co(companyId)}`, quote),
  selectTsu: (companyId: number, no: string, insurers: string[]) =>
    api.post<unknown>(`${t(no)}/selection${co(companyId)}`, { insurers }),
  completeTsu: (companyId: number, no: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<unknown>(`${t(no)}/complete${co(companyId)}`, form);
  },
  kyc: (companyId: number, ref: string) =>
    api.get<KycMonitoringView>(`${c(ref)}/kyc${co(companyId)}`),
  recordKyc: (
    companyId: number,
    ref: string,
    body: { status: string; activity: string; remarks: string },
  ) => api.post<unknown>(`${c(ref)}/kyc${co(companyId)}`, body),
  kycDashboard: (companyId: number) =>
    api.get<{ due: number; upcoming: number; completed: number; follow_up: number }>(
      `${R}/kyc/dashboard${co(companyId)}`,
    ),
  kycAccounts: (companyId: number, status: string) =>
    api.get<KycAccount[]>(`${R}/kyc/accounts${toQuery({ companyId, status })}`),
  refreshKyc: (companyId: number) =>
    api.post<{ changed: number }>(`${R}/kyc/refresh${co(companyId)}`, {}),
  riskCodes: (companyId: number, search?: string, renewable?: boolean) =>
    api.get<RiskCodeView[]>(
      `${R}/setup/risk-code-maintenance${toQuery({ companyId, search, renewable })}`,
    ),
  createRiskCode: (companyId: number, entry: RiskCodeEntry) =>
    api.post<RiskCodeView>(`${R}/setup/risk-code-maintenance${co(companyId)}`, entry),
  updateRiskCode: (companyId: number, id: number, entry: RiskCodeEntry) =>
    api.put<RiskCodeView>(`${R}/setup/risk-code-maintenance/${String(id)}${co(companyId)}`, entry),
};

/** Labels of the KYC statuses. */
export const KYC_STATUS: Record<string, string> = {
  KYC_DUE: 'KYC Due',
  KYC_UPCOMING: 'KYC Upcoming',
  KYC_NOT_REQUIRED: 'KYC Not Required',
  PENDING_FOLLOW_UP: 'Pending Follow-Up',
  KYC_COMPLETED: 'KYC Completed',
};

/** Labels of the TSU request statuses. */
export const TSU_STATUS: Record<string, string> = {
  DRAFT: 'Draft',
  PENDING_TL_APPROVAL: 'Pending Team Lead Approval',
  RETURNED_FOR_REVISION: 'Returned for Revision',
  REJECTED: 'Rejected',
  APPROVED: 'Approved',
  FOR_TSU_PROCESSING: 'For TSU Processing',
  COMPLETED: 'Completed',
};
