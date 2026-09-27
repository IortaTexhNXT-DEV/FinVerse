import { api, toQuery } from './client';
import type { PageResponse } from './types';

/**
 * Placement (placement module): workbench, payment gate, CLPC billing and payment reports,
 * placement slips, hold cover, insurer returns, cancel and reactivate placement.
 */

export type WorkbenchTab =
  | 'FOR_PLACEMENT'
  | 'AWAITING_PAYMENT'
  | 'RETURNED'
  | 'CANCELLED'
  | 'HOLD_COVER_EXPIRING'
  | 'BOOKED';

export interface WorkbenchRow {
  accountId: number;
  arn: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  status: string;
  productCode: string;
  lineCode: string;
  department?: string;
  marketSegment?: string;
  insurerCode?: string;
  insurerBranch?: string;
  grossPremium?: number;
  directPayment: boolean;
  paymentStatus: string;
  slipId?: number;
  slipNo?: string;
  slipStatus?: string;
  holdCoverStatus?: string;
  holdCoverExpiry?: string;
}

export interface WorkbenchCounts {
  awaitingPayment: number;
  readyForPlacement: number;
  placed: number;
  returnedByInsurer: number;
  holdCoverExpiring: number;
  placementCancelled: number;
  policyIssued: number;
  booked: number;
}

export interface Unmet {
  code: string;
  message: string;
}

export interface Readiness {
  arn: string;
  clientName: string;
  insurerCode?: string;
  insurerBranch?: string;
  status: string;
  ready: boolean;
  unmet: Unmet[];
}

export interface SlipAccount {
  accountId: number;
  arn: string;
}

export interface Slip {
  id: number;
  slipNo: string;
  versionNo: number;
  displayNo: string;
  insurerCode: string;
  branchCode: string;
  status: 'GENERATED' | 'SENT' | 'SUPERSEDED';
  templateVersion: string;
  recipients?: string;
  sentAt?: string;
  sendCount: number;
  createdAt: string;
  createdBy: string;
  accounts: SlipAccount[];
}

export interface SlipEmail {
  to: string[];
  cc: string[];
  subject: string;
  body: string;
  protect: boolean;
}

export interface ItemResult {
  arn: string;
  ok: boolean;
  message: string;
}

export interface Evidence {
  id: number;
  kind: string;
  source: string;
  reference: string;
  amount?: number;
  paidOn?: string;
  channel?: string;
  remarks?: string;
  attachmentId?: number;
  gateOpened: boolean;
  createdAt: string;
  createdBy: string;
}

export interface Gate {
  accountId: number;
  arn: string;
  status: string;
  marketSegment?: string;
  lineCode: string;
  paymentStatus: string;
  paymentSource?: string;
  directPayment: boolean;
  rule: 'PAYMENT_MATCHED' | 'CLIENT_CONFIRMATION' | 'DIRECT_PAYMENT';
  ruleDescription: string;
  open: boolean;
  evidence: Evidence[];
}

export interface GateRule {
  marketSegment?: string;
  lineCode?: string;
  rule: string;
  priority: number;
  description: string;
}

export interface HoldCover {
  id: number;
  arn: string;
  insurerCode: string;
  status: string;
  startDate: string;
  expiryDate: string;
  insurerRef?: string;
  confirmedOn?: string;
  alertedOn?: string;
  createdAt: string;
  createdBy: string;
}

export interface InsurerReturn {
  id: number;
  arn: string;
  insurerCode?: string;
  slipNo?: string;
  reasonCode: string;
  remarks?: string;
  resolution?: string;
  resolvedAt?: string;
  createdAt: string;
  createdBy: string;
}

export interface PlacementView {
  gate: Gate;
  slips: Slip[];
  holdCovers: HoldCover[];
  returns: InsurerReturn[];
}

export interface BillingItem {
  lineNo: number;
  accountId: number;
  arn: string;
  pnNumbers?: string;
  loanApplicationNo?: string;
  bookingDate?: string;
  borrower: string;
  originatingUnit?: string;
  premium: number;
  bdoiLocation?: string;
  amortised: boolean;
  paymentStatus: string;
}

export interface BillingBatch {
  id: number;
  batchNo: string;
  billingDate: string;
  status: string;
  itemCount: number;
  totalPremium: number;
  createdAt: string;
  createdBy: string;
  items: BillingItem[];
}

export interface BillingCandidate {
  accountId: number;
  arn: string;
  clientName: string;
  pnNumbers: string[];
  loanApplicationNo?: string;
  mortgageeBank?: string;
  periodFrom?: string;
  grossPremium?: number;
}

export type MatchStatus = 'MATCHED' | 'UNPAID' | 'UNMATCHED' | 'AMBIGUOUS';

export interface ReportLine {
  id: number;
  rowNo: number;
  reference: string;
  paid: boolean;
  amount?: number;
  paidOn?: string;
  matchStatus: MatchStatus;
  accountId?: number;
  arn?: string;
  candidates?: string;
  message?: string;
  manuallyMatched: boolean;
  applied: boolean;
  applyMessage?: string;
}

export interface PaymentReport {
  id: number;
  reportNo: string;
  kind: 'CLPC' | 'REFERENCE';
  batchId?: number;
  fileName: string;
  status: 'REVIEW' | 'CONFIRMED' | 'DISCARDED';
  matched: number;
  unpaid: number;
  unmatched: number;
  ambiguous: number;
  confirmedBy?: string;
  confirmedAt?: string;
  createdAt: string;
  createdBy: string;
  lines: ReportLine[];
}

const pageOf = (page = 0, size = 20) => ({ page, size });

export const placementApi = {
  workbench: (companyId: number, tab: WorkbenchTab, text?: string, page = 0) =>
    api.get<PageResponse<WorkbenchRow>>(
      `/placement/workbench${toQuery({ companyId, tab, text, ...pageOf(page) })}`,
    ),
  counts: (companyId: number) =>
    api.get<WorkbenchCounts>(`/placement/workbench/counts${toQuery({ companyId })}`),
  readiness: (companyId: number, arns: string[]) => {
    const params = new URLSearchParams({ companyId: String(companyId) });
    arns.forEach((arn) => params.append('arn', arn));
    return api.get<Readiness[]>(`/placement/readiness?${params.toString()}`);
  },
  account: (arn: string) => api.get<PlacementView>(`/placement/accounts/${arn}`),
  insurerReturn: (arn: string, reasonCode: string, remarks?: string) =>
    api.post<InsurerReturn>(`/placement/accounts/${arn}/insurer-return`, { reasonCode, remarks }),
  resubmit: (arn: string, comment?: string) =>
    api.post<Gate>(`/placement/accounts/${arn}/resubmit`, { comment }),
  cancel: (arns: string[], reasonCode: string, comment?: string) =>
    api.post<ItemResult[]>('/placement/accounts/cancel', { arns, reasonCode, comment }),
  reactivate: (arns: string[], comment?: string) =>
    api.post<ItemResult[]>('/placement/accounts/reactivate', { arns, comment }),
  requestHoldCover: (arn: string, startDate?: string) =>
    api.post<HoldCover>(`/placement/accounts/${arn}/hold-cover`, { startDate }),
  confirmHoldCover: (
    arn: string,
    body: { reference: string; insurerCode?: string; confirmedOn?: string; expiryDate?: string },
  ) => api.post<HoldCover>(`/placement/accounts/${arn}/hold-cover/confirm`, body),
  declineHoldCover: (arn: string, reference?: string) =>
    api.post<HoldCover>(`/placement/accounts/${arn}/hold-cover/decline`, { comment: reference }),
  slips: (companyId: number, status?: string, page = 0) =>
    api.get<PageResponse<Slip>>(
      `/placement/slips${toQuery({ companyId, status, ...pageOf(page) })}`,
    ),
  generateSlips: (companyId: number, arns: string[]) =>
    api.post<Slip[]>('/placement/slips/generate', { companyId, arns }),
  regenerateSlip: (id: number) => api.post<Slip>(`/placement/slips/${id}/regenerate`),
  slipDraft: (id: number) => api.get<SlipEmail>(`/placement/slips/${id}/email-draft`),
  sendSlip: (id: number, email: SlipEmail) => api.post<Slip>(`/placement/slips/${id}/send`, email),
  sendSlips: (companyId: number, arns: string[]) =>
    api.post<ItemResult[]>('/placement/slips/send', { companyId, arns }),
  slipFile: (id: number, format: 'pdf' | 'xlsx') =>
    api.getFile(`/placement/slips/${id}/files/${format}`),
  gateRules: () => api.get<GateRule[]>('/placement/gate/rules'),
  confirmClient: (arn: string, channel: string, remarks?: string) =>
    api.post<Gate>(`/placement/gate/${arn}/client-confirmation`, { channel, remarks }),
  confirmDirect: (arn: string, remarks?: string) =>
    api.post<Gate>(`/placement/gate/${arn}/direct-payment`, { comment: remarks }),
  candidates: (companyId: number) =>
    api.get<BillingCandidate[]>(`/placement/billing/candidates${toQuery({ companyId })}`),
  batches: (companyId: number, page = 0) =>
    api.get<PageResponse<BillingBatch>>(
      `/placement/billing/batches${toQuery({ companyId, ...pageOf(page) })}`,
    ),
  createBatch: (companyId: number, arns: string[]) =>
    api.post<BillingBatch>('/placement/billing/batches', { companyId, arns }),
  batchFile: (id: number, format: 'XLSX' | 'ODS') =>
    api.getFile(`/placement/billing/batches/${id}/file${toQuery({ format })}`),
  uploadReport: (companyId: number, kind: string, file: File, batchId?: number) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    form.append('kind', kind);
    if (batchId !== undefined) {
      form.append('batchId', String(batchId));
    }
    form.append('file', file);
    return api.upload<PaymentReport>('/placement/billing/reports', form);
  },
  reports: (companyId: number, page = 0) =>
    api.get<PageResponse<PaymentReport>>(
      `/placement/billing/reports${toQuery({ companyId, ...pageOf(page) })}`,
    ),
  report: (id: number) => api.get<PaymentReport>(`/placement/billing/reports/${id}`),
  matchLine: (id: number, lineId: number, arn: string) =>
    api.post<PaymentReport>(`/placement/billing/reports/${id}/lines/${lineId}/match`, { arn }),
  confirmReport: (id: number) =>
    api.post<PaymentReport>(`/placement/billing/reports/${id}/confirm`),
  discardReport: (id: number) =>
    api.post<PaymentReport>(`/placement/billing/reports/${id}/discard`),
};
