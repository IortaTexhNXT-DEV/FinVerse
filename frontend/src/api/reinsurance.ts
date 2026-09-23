import { api, toQuery } from './client';
import type { RecordStatus } from './types';

/** Reinsurance API: treaties, cessions, facultative placements, claim shares, statements. */

export type TreatyType = 'QUOTA_SHARE' | 'SURPLUS' | 'XOL';
export type RiLayer = 'RETENTION' | 'QUOTA_SHARE' | 'SURPLUS' | 'FAC' | 'XOL';
export type FacStatus = 'PROVISIONAL' | 'PENDING_APPROVAL' | 'PLACED' | 'CLOSED';
export type SoaStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'SETTLED';

export interface ParticipantInput {
  /** Client-side row key (ignored by the server). */
  key?: string;
  reinsurerCode: string;
  sharePct: number;
  commissionPct?: number;
  profitCommissionPct?: number;
  premiumReservePct?: number;
}

export interface LayerInput {
  key?: string;
  priority: number;
  limit: number;
  minDepositPremium?: number;
  reinstatements?: number;
}

export interface TreatyInput {
  companyId: number;
  code: string;
  name: string;
  treatyType: TreatyType;
  businessLine: string;
  uwYear: number;
  periodFrom: string;
  periodTo: string;
  currency: string;
  quotaSharePct?: number;
  treatyLimit?: number;
  retentionLimit?: number;
  lines?: number;
  levyPct?: number;
  reserveInterestPct?: number;
  lossReservePct?: number;
  brokerCode?: string;
  participants: ParticipantInput[];
  layers: LayerInput[];
}

export interface Treaty extends Omit<TreatyInput, 'participants' | 'layers'> {
  id: number;
  statementFrequency: string;
  brokerName?: string;
  participants: (ParticipantInput & { lineNo: number; reinsurerName: string })[];
  layers: (LayerInput & { layerNo: number })[];
  recordStatus: RecordStatus;
  createdBy: string;
  updatedBy?: string;
  authorizedBy?: string;
}

export interface CessionLine {
  riskLineNo: number;
  riskDescription: string;
  riskSi: number;
  layer: RiLayer;
  treatyId?: number;
  placementId?: number;
  reinsurerCode?: string;
  sharePct: number;
  sumInsured: number;
  premium: number;
  commissionPct: number;
  commission: number;
  postingRef?: string;
}

export interface Cession {
  id: number;
  cessionNo: string;
  policyId: number;
  policyNo: string;
  endorsementNo: number;
  documentNo: string;
  kind: string;
  basis: 'FULL' | 'PRO_RATA' | 'NONE';
  businessLine: string;
  productCode: string;
  uwYear: number;
  treatyYear: number;
  riDate: string;
  currency: string;
  exchangeRate: number;
  sharePct: number;
  ourSi: number;
  ourPremium: number;
  treatyPremium: number;
  facPremium: number;
  retention: number;
  lines: CessionLine[];
  createdBy: string;
}

export interface AllocationPreviewRow {
  policyId: number;
  policyNo: string;
  endorsementNo: number;
  documentNo: string;
  kind: string;
  businessLine: string;
  approvalDate: string;
  currency: string;
  basis?: string;
  ourSi: number;
  ourPremium: number;
  retention: number;
  quotaShare: number;
  surplus: number;
  fac: number;
  message?: string;
}

export interface AllocationRunResult {
  jobRunId: number;
  status: string;
  ceded: number;
  failed: number;
  premiumCeded: number;
  messages: string[];
}

export interface FacParticipantLine {
  reinsurerCode: string;
  sharePct: number;
  commissionPct?: number;
}

export interface FacPlacement {
  id: number;
  placementNo: string;
  cessionNo: string;
  policyId: number;
  policyNo: string;
  endorsementNo: number;
  businessLine: string;
  currency: string;
  riDate: string;
  riskLineNo: number;
  riskDescription: string;
  riskSi: number;
  riskPremium: number;
  facPct: number;
  facSi: number;
  facPremium: number;
  placedSi: number;
  placedPremium: number;
  placementPct: number;
  commission: number;
  status: FacStatus;
  submittedBy?: string;
  placedBy?: string;
  placedOn?: string;
  closedOn?: string;
  remarks?: string;
  participants: (FacParticipantLine & {
    lineNo: number;
    reinsurerName: string;
    sumInsured: number;
    premium: number;
    commission: number;
  })[];
}

export interface ClaimShare {
  layer: RiLayer;
  treatyId?: number;
  layerNo?: number;
  placementId?: number;
  reinsurerCode: string;
  sharePct: number;
  baseAmount: number;
}

export interface ClaimMovement {
  id: number;
  reference: string;
  claimId: number;
  claimNo: string;
  policyId: number;
  businessLine: string;
  lossDate: string;
  movementDate: string;
  movementType: 'RESERVE_CHANGE' | 'PAYMENT' | 'RECOVERY';
  currency: string;
  amount: number;
  baseAmount: number;
  ceded: number;
  netRetained: number;
  shares: ClaimShare[];
}

export interface SoaLine {
  label: string;
  income: number;
  outgo: number;
}

export interface SoaLayout {
  lines: SoaLine[];
  incomeSubtotal: number;
  outgoSubtotal: number;
  balance: number;
  balanceLabel: string;
  balanceOnIncome: boolean;
  total: number;
  amountInWords: string;
}

export interface Soa {
  id: number;
  soaNo: string;
  treatyCode: string;
  treatyName: string;
  treatyType: TreatyType;
  businessLine: string;
  reinsurerCode: string;
  reinsurerName: string;
  year: number;
  quarter: number;
  periodFrom: string;
  periodTo: string;
  statementDate: string;
  currency: string;
  balance: number;
  status: SoaStatus;
  layout: SoaLayout;
  preparedBy: string;
  approvedBy?: string;
  settlementDate?: string;
  settlementBatchNo?: string;
  bankAccountCode?: string;
}

export interface SoaGenerateInput {
  companyId: number;
  treatyCode: string;
  reinsurerCode?: string;
  year: number;
  quarter: number;
  statementDate?: string;
}

const BASE = '/reinsurance';

export const reinsuranceApi = {
  treaties: (companyId: number) => api.get<Treaty[]>(`${BASE}/treaties${toQuery({ companyId })}`),
  createTreaty: (body: TreatyInput) => api.post<Treaty>(`${BASE}/treaties`, body),
  updateTreaty: (id: number, body: TreatyInput) => api.put<Treaty>(`${BASE}/treaties/${id}`, body),
  authorizeTreaty: (id: number) => api.post<Treaty>(`${BASE}/treaties/${id}/authorize`),

  preview: (companyId: number, from: string, to: string) =>
    api.get<AllocationPreviewRow[]>(
      `${BASE}/allocation/preview${toQuery({ companyId, from, to })}`,
    ),
  run: (companyId: number, fromDate: string, toDate: string) =>
    api.post<AllocationRunResult>(`${BASE}/allocation/runs`, { companyId, fromDate, toDate }),
  policyCessions: (companyId: number, policyNo: string) =>
    api.get<Cession[]>(`${BASE}/cessions${toQuery({ companyId, policyNo })}`),
  cedePolicy: (policyId: number) => api.post<Cession[]>(`${BASE}/cessions/policies/${policyId}`),

  placements: (companyId: number, status?: FacStatus) =>
    api.get<FacPlacement[]>(`${BASE}/fac-placements${toQuery({ companyId, status })}`),
  assignPlacement: (id: number, participants: FacParticipantLine[], remarks?: string) =>
    api.put<FacPlacement>(`${BASE}/fac-placements/${id}/participants`, { participants, remarks }),
  submitPlacement: (id: number) => api.post<FacPlacement>(`${BASE}/fac-placements/${id}/submit`),
  approvePlacement: (id: number, date?: string) =>
    api.post<FacPlacement>(`${BASE}/fac-placements/${id}/approve`, { date }),
  rejectPlacement: (id: number) => api.post<FacPlacement>(`${BASE}/fac-placements/${id}/reject`),
  closePlacement: (id: number, date?: string) =>
    api.post<FacPlacement>(`${BASE}/fac-placements/${id}/close`, { date }),

  claimMovements: (companyId: number, from: string, to: string) =>
    api.get<ClaimMovement[]>(`${BASE}/claims/movements${toQuery({ companyId, from, to })}`),
  catchUp: (companyId: number, fromDate: string, toDate: string) =>
    api.post<{ processed: number }>(`${BASE}/claims/catch-up`, { companyId, fromDate, toDate }),

  statements: (companyId: number) => api.get<Soa[]>(`${BASE}/soas${toQuery({ companyId })}`),
  generateStatements: (body: SoaGenerateInput) => api.post<Soa[]>(`${BASE}/soas`, body),
  approveStatement: (id: number) => api.post<Soa>(`${BASE}/soas/${id}/approve`),
  settleStatement: (id: number, settlementDate: string, bankAccountCode: string) =>
    api.post<Soa>(`${BASE}/soas/${id}/settle`, { settlementDate, bankAccountCode }),
};
