import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Claims API: notification (FNOL), reserves, settlements, recoveries, LPOs, movements. */

export type ClaimStatus =
  'REGISTERED' | 'OPEN' | 'PARTIALLY_SETTLED' | 'CLOSED' | 'REOPENED' | 'REJECTED' | 'WITHDRAWN';
export type ClaimPartyRole = 'CLAIMANT' | 'THIRD_PARTY' | 'GARAGE' | 'SURVEYOR' | 'ADJUSTER';
export type CostType = 'LOSS' | 'EXPENSE';
export type EstimateSide = 'PAYMENT' | 'RECOVERY';
export type DocumentStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
export type SettlementType = 'PARTIAL' | 'FINAL';
export type RecoveryType = 'SALVAGE' | 'SUBROGATION';
export type LpoCover = 'OD' | 'TP';

export interface ClaimPartyInput {
  role: ClaimPartyRole;
  partyCode: string;
}

export interface ClaimParty extends ClaimPartyInput {
  partyName: string;
  partyType: string;
}

export interface ClaimInput {
  companyId: number;
  policyNo: string;
  riskLineNo?: number;
  lossDate: string;
  reportedDate: string;
  natureOfLoss: string;
  causeOfLoss: string;
  lossLocation: string;
  description: string;
  currency: string;
  claimantCode?: string;
  parties: ClaimPartyInput[];
  initialLossReserve?: number;
  initialExpenseReserve?: number;
}

export interface ClaimTotals {
  estimateLoss: number;
  estimateExpense: number;
  estimateRecovery: number;
  paidLoss: number;
  paidExpense: number;
  recovered: number;
  outstandingLoss: number;
  outstandingExpense: number;
  recoveryOutstanding: number;
  ourEstimate: number;
  ourPaid: number;
  ourOutstanding: number;
  ourRecovered: number;
}

export interface Claim {
  id: number;
  claimNo: string;
  companyId: number;
  branchId: number;
  status: ClaimStatus;
  statusReason?: string;
  closedOn?: string;
  policyId: number;
  policyNo: string;
  productCode: string;
  productName: string;
  businessLine: string;
  customerCode: string;
  customerName: string;
  insuredName: string;
  sharePct: number;
  coinsuranceLeader: boolean;
  coinsurerCode?: string;
  riskDescription?: string;
  sumInsured: number;
  lossDate: string;
  reportedDate: string;
  natureOfLoss: string;
  causeOfLoss: string;
  lossLocation: string;
  description: string;
  currency: string;
  claimantCode: string;
  claimantName: string;
  totals: ClaimTotals;
  parties: ClaimParty[];
  createdBy: string;
  createdAt: string;
}

export interface ClaimFilters {
  companyId: number;
  branchId?: number;
  status?: string;
  businessLine?: string;
  q?: string;
  lossFrom?: string;
  lossTo?: string;
  page?: number;
  size?: number;
}

export interface PolicyCover {
  policyId: number;
  policyNo: string;
  status: string;
  productCode: string;
  productName: string;
  businessLine: string;
  customerCode: string;
  customerName: string;
  insuredName: string;
  periodFrom: string;
  periodTo: string;
  currency: string;
  sharePct: number;
  coinsurerCode?: string;
  coinsuranceLeader: boolean;
  inForce: boolean;
  risks: { lineNo: number; description: string; sumInsured: number }[];
}

export interface Approval {
  status: DocumentStatus;
  submittedBy: string;
  submittedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  approvalDate?: string;
  rejectionReason?: string;
  authorityAmount: number;
}

export interface ReserveInput {
  side: EstimateSide;
  costType: CostType;
  newEstimate: number;
  reason: string;
}

export interface ReserveChange extends ReserveInput {
  id: number;
  claimId: number;
  changeNo: number;
  previousEstimate?: number;
  changeAmount?: number;
  ourChange?: number;
  systemGenerated: boolean;
  journalBatchNo?: string;
  approval: Approval;
}

export interface SettlementInput {
  payeeCode: string;
  costType: CostType;
  settlementType: SettlementType;
  assessedAmount: number;
  deductible?: number;
  excess?: number;
  narration: string;
}

export interface Settlement {
  id: number;
  claimId: number;
  settlementNo: string;
  payeeCode: string;
  payeeName: string;
  costType: CostType;
  settlementType: SettlementType;
  assessedAmount: number;
  deductible: number;
  excessAmount: number;
  netAmount: number;
  ourAmount?: number;
  payableAmount?: number;
  coinsurerAmount?: number;
  exchangeRate?: number;
  narration: string;
  journalBatchNo?: string;
  coinsuranceBatchNo?: string;
  approval: Approval;
}

export interface RecoveryInput {
  recoveryType: RecoveryType;
  fromPartyCode?: string;
  bankAccountCode: string;
  amount: number;
  narration: string;
}

export interface Recovery extends RecoveryInput {
  id: number;
  claimId: number;
  recoveryNo: string;
  fromPartyName?: string;
  ourAmount?: number;
  coinsurerAmount?: number;
  journalBatchNo?: string;
  coinsuranceBatchNo?: string;
  approval: Approval;
}

export interface LpoInput {
  garageCode: string;
  cover: LpoCover;
  issueDate?: string;
  gross: number;
  discount?: number;
  description: string;
}

export interface Lpo {
  id: number;
  claimId: number;
  claimNo: string;
  lpoNo: string;
  garageCode: string;
  garageName: string;
  coverType: LpoCover;
  issueDate: string;
  grossAmount: number;
  discountAmount: number;
  netAmount: number;
  currency: string;
  description: string;
  status: 'ISSUED' | 'CANCELLED';
  cancelReason?: string;
}

export interface Movement {
  id: number;
  movementDate: string;
  kind: 'ESTIMATE' | 'PAID';
  side: EstimateSide;
  costType: CostType;
  estimateType: number;
  amount100: number;
  amount: number;
  baseAmount: number;
  currency: string;
  reference: string;
  sourceType: 'RESERVE' | 'SETTLEMENT' | 'RECOVERY';
  journalBatchNo?: string;
  narration: string;
}

export interface ClaimDecisionInput {
  reason: string;
  accountingDate?: string;
}

/** Documents a checker approves: the URL segment of each kind. */
export type ClaimDocumentKind = 'reserves' | 'settlements' | 'recoveries';

const BASE = '/claims';

export const claimsApi = {
  claims: (f: ClaimFilters) => api.get<PageResponse<Claim>>(`${BASE}${toQuery({ ...f })}`),
  claim: (id: number) => api.get<Claim>(`${BASE}/${id}`),
  policyCover: (companyId: number, policyNo: string, lossDate?: string) =>
    api.get<PolicyCover>(`${BASE}/policy-cover${toQuery({ companyId, policyNo, lossDate })}`),
  register: (body: ClaimInput) => api.post<Claim>(BASE, body),
  addParty: (id: number, body: ClaimPartyInput) => api.post<Claim>(`${BASE}/${id}/parties`, body),
  close: (id: number, body: ClaimDecisionInput) => api.post<Claim>(`${BASE}/${id}/close`, body),
  reopen: (id: number, reason: string) => api.post<Claim>(`${BASE}/${id}/reopen`, { reason }),
  repudiate: (id: number, body: ClaimDecisionInput) =>
    api.post<Claim>(`${BASE}/${id}/repudiate`, body),
  withdraw: (id: number, body: ClaimDecisionInput) =>
    api.post<Claim>(`${BASE}/${id}/withdraw`, body),
  movements: (id: number) => api.get<Movement[]>(`${BASE}/${id}/movements`),

  reserves: (id: number) => api.get<ReserveChange[]>(`${BASE}/${id}/reserves`),
  requestReserve: (id: number, body: ReserveInput) =>
    api.post<ReserveChange>(`${BASE}/${id}/reserves`, body),
  settlements: (id: number) => api.get<Settlement[]>(`${BASE}/${id}/settlements`),
  createSettlement: (id: number, body: SettlementInput) =>
    api.post<Settlement>(`${BASE}/${id}/settlements`, body),
  recoveries: (id: number) => api.get<Recovery[]>(`${BASE}/${id}/recoveries`),
  createRecovery: (id: number, body: RecoveryInput) =>
    api.post<Recovery>(`${BASE}/${id}/recoveries`, body),
  approve: (kind: ClaimDocumentKind, documentId: number, accountingDate?: string) =>
    api.post<unknown>(`${BASE}/${kind}/${documentId}/approve`, { accountingDate }),
  reject: (kind: ClaimDocumentKind, documentId: number, reason: string) =>
    api.post<unknown>(`${BASE}/${kind}/${documentId}/reject`, { reason }),

  lpos: (id: number) => api.get<Lpo[]>(`${BASE}/${id}/lpos`),
  lpoRegister: (companyId: number) => api.get<Lpo[]>(`${BASE}/lpos${toQuery({ companyId })}`),
  issueLpo: (id: number, body: LpoInput) => api.post<Lpo>(`${BASE}/${id}/lpos`, body),
  cancelLpo: (lpoId: number, reason: string) =>
    api.post<Lpo>(`${BASE}/lpos/${lpoId}/cancel`, { reason }),
};
