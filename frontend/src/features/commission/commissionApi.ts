import { api, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Commission Receivables API (CMRID.001-015, MKTID.012): direct payment lists and accounts,
 * billings to the insurers with their answers and collection, incentive schemes and runs, BIR
 * certificates and estimated premium items.
 */

export const BILLING_ENTITY = 'DpBilling';
export const CERTIFICATE_ENTITY = 'BirCertificate';

export type DpTag =
  | 'DP_FOR_CONFIRMATION'
  | 'DP_FOR_BILLING'
  | 'BILLED'
  | 'APPROVED'
  | 'REJECTED'
  | 'COLLECTED'
  | 'PR_REVERSED'
  | 'EXCLUDED';
export type Sanitation = 'VALID' | 'DUPLICATE' | 'INVALID' | 'INCOMPLETE';
export type BillingStage =
  | 'DP_FOR_BILLING'
  | 'AWAITING_INSURER'
  | 'APPROVED'
  | 'COLLECTED'
  | 'CLOSED'
  | 'RETURNED_TO_COLLECTION'
  | 'CANCELLED';
export type SchemeType = 'NO_TOUCH' | 'TOP_UP' | 'MOTOR_MANIA' | 'OTHER';
export type Calculation = 'TARGET_TIERED' | 'FIXED_PER_POLICY';
export type PeriodType = 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL' | 'CUSTOM';
export type Beneficiary = 'BDOI' | 'BRANCH';
export type RunStatus = 'COMPUTED' | 'POSTED' | 'CANCELLED';

export interface Amounts {
  premium?: number;
  commission?: number;
  commissionVat?: number;
  wtax?: number;
  net?: number;
}

export interface DpList {
  id: number;
  listNo: string;
  source: 'BRANCH' | 'HEAD_OFFICE' | 'COLLECTION_FEED';
  branchCode?: string;
  submissionDate: string;
  fileName?: string;
  runNo?: string;
  itemCount: number;
  validCount: number;
  excludedCount: number;
  createdAt: string;
  createdBy: string;
}

export interface Submission {
  branchCode: string;
  branchName: string;
  lists: number;
  accounts: number;
  lastSubmission?: string;
  received: boolean;
}

export interface RuleResult {
  rule: string;
  passed: boolean;
  message?: string;
}

export interface DpProgress {
  remarks?: string;
  confirmedBy?: string;
  confirmedAt?: string;
  feedbackReason?: string;
  feedbackComment?: string;
  respondedAt?: string;
  returnedRef?: string;
  orNo?: string;
  collectedAmount?: number;
  collectedOn?: string;
  prReversedAt?: string;
  reinstatedCount: number;
}

export interface DpItem {
  id: number;
  listId: number;
  invoiceNo: string;
  policyNo?: string;
  insurerCode?: string;
  clientCode?: string;
  assuredName?: string;
  branchCode?: string;
  submittedPremium?: number;
  amounts: Amounts;
  tag: DpTag;
  sanitation: Sanitation;
  rules: RuleResult[];
  billingId?: number;
  feedback: DpProgress;
}

export interface DpBilling {
  id: number;
  billingNo: string;
  insurerCode: string;
  handler?: string;
  stage: BillingStage;
  itemCount: number;
  amounts: Amounts;
  fileId?: number;
  fileName?: string;
  sentAt?: string;
  slaDue?: string;
  overdue: boolean;
  respondedAt?: string;
  recipients?: string;
  orNo?: string;
  orStatus?: string;
  createdAt: string;
}

export interface Answer {
  invoiceNo: string;
  approved: boolean;
  reason?: string;
  comment?: string;
}

export interface CommissionSettings {
  feedbackWorkingDays: number;
  collectionBank?: string;
  prReversalPosting: boolean;
}

export interface IncentiveTier {
  minProduction?: number;
  ratePercent?: number;
  multiplier?: number;
  minBasicPremium?: number;
  fixedAmount?: number;
}

export interface SchemeTerms {
  name: string;
  schemeType: SchemeType;
  calculation: Calculation;
  periodType: PeriodType;
  beneficiary: Beneficiary;
  insurerCode?: string;
  segments: string[];
  productLines: string[];
  effectiveFrom?: string;
  effectiveTo?: string;
  active: boolean;
  description?: string;
  tiers: IncentiveTier[];
}

export interface Scheme {
  id: number;
  code: string;
  terms: SchemeTerms;
}

export interface RunTotals {
  eligibleCount: number;
  eligibleProduction: number;
  excludedCount: number;
  excludedAmount: number;
  tierApplied?: string;
  incentive: number;
  passOn: number;
}

export interface IncentiveRun {
  id: number;
  runNo: string;
  schemeId: number;
  periodFrom: string;
  periodTo: string;
  status: RunStatus;
  totals: RunTotals;
  postedAt?: string;
  postedBy?: string;
  journalRefs?: string;
  createdAt: string;
  createdBy: string;
}

export interface RunLine {
  invoiceNo: string;
  insurerCode: string;
  salesUnit?: string;
  segment?: string;
  productLine?: string;
  basicPremium: number;
  grossPremium: number;
  exclusionReason?: string;
  incentive: number;
}

export interface OrLink {
  orNo: string;
  amount: number;
}

export interface CertificateInput {
  companyId?: number;
  insurerCode?: string;
  form: string;
  number: string;
  periodFrom: string;
  periodTo: string;
  taxWithheld: number;
  receipts: OrLink[];
}

export interface Certificate {
  id: number;
  submissionNo: string;
  insurerCode: string;
  certificate: CertificateInput;
  stage: 'SUBMITTED' | 'ACKNOWLEDGED' | 'REJECTED';
  rejectReason?: string;
  submittedCount: number;
  decidedAt?: string;
  decidedBy?: string;
  createdAt: string;
  createdBy: string;
}

export interface EstimatedItem {
  invoiceNo: string;
  arn?: string;
  insurerCode?: string;
  assuredName?: string;
  bookingDate?: string;
  grossPremium?: number;
  estimated: boolean;
}

export interface ItemFilter {
  tag: DpTag[];
  sanitation?: Sanitation;
  insurer?: string;
  listId?: number;
  q?: string;
}

const BASE = '/commission';
const DP = `${BASE}/dp`;
const INC = `${BASE}/incentives`;
const PAGE = 20;
const id = (n: number) => String(n);

function itemQuery(companyId: number, filter: ItemFilter, page: number): string {
  const tags = filter.tag.map((t) => `&tag=${t}`).join('');
  return `${toQuery({
    companyId,
    sanitation: filter.sanitation,
    insurer: filter.insurer,
    listId: filter.listId,
    q: filter.q,
    page,
  })}${tags}`;
}

export const commissionApi = {
  lists: (companyId: number, page: number) =>
    api.get<PageResponse<DpList>>(`${DP}/lists${toQuery({ companyId, page, size: PAGE })}`),
  uploadList: (companyId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<DpList>(`${DP}/lists${toQuery({ companyId })}`, form);
  },
  pull: (companyId: number) => api.post<DpList[]>(`${DP}/lists/pull${toQuery({ companyId })}`),
  submissions: (companyId: number, from: string, to: string) =>
    api.get<Submission[]>(`${DP}/submissions${toQuery({ companyId, from, to })}`),
  items: (companyId: number, filter: ItemFilter, page: number) =>
    api.get<PageResponse<DpItem>>(`${DP}/items${itemQuery(companyId, filter, page)}`),
  counts: (companyId: number) =>
    api.get<Partial<Record<DpTag, number>>>(`${DP}/items/counts${toQuery({ companyId })}`),
  confirm: (ids: number[]) => api.post<DpItem[]>(`${DP}/items/confirm`, { ids }),
  exclude: (ids: number[], reason: string) =>
    api.post<DpItem[]>(`${DP}/items/exclude`, { ids, reason }),
  revalidate: (itemId: number) => api.post<DpItem>(`${DP}/items/${id(itemId)}/revalidate`),
  reverse: (itemId: number) => api.post<DpItem>(`${DP}/items/${id(itemId)}/reverse`),
  reinstate: (itemId: number, reasonCode: string, comment: string) =>
    api.post<DpItem>(`${DP}/items/${id(itemId)}/reinstate`, { reasonCode, comment }),
  prepare: (companyId: number, ids: number[]) =>
    api.post<DpBilling[]>(`${DP}/billings`, { companyId, ids }),
  billings: (companyId: number, filter: { stage?: string; insurer?: string }, page: number) =>
    api.get<PageResponse<DpBilling>>(
      `${DP}/billings${toQuery({ companyId, ...filter, page, size: PAGE })}`,
    ),
  billing: (billingId: number) => api.get<DpBilling>(`${DP}/billings/${id(billingId)}`),
  billingItems: (billingId: number) => api.get<DpItem[]>(`${DP}/billings/${id(billingId)}/items`),
  billingFile: (billingId: number): Promise<DownloadedFile> =>
    api.getFile(`${DP}/billings/${id(billingId)}/file`),
  send: (billingId: number, to: string[], cc: string[]) =>
    api.post<DpBilling>(`${DP}/billings/${id(billingId)}/send`, { to, cc }),
  cancel: (billingId: number, comment: string) =>
    api.post<DpBilling>(`${DP}/billings/${id(billingId)}/cancel`, { comment }),
  answer: (billingId: number, answers: Answer[]) =>
    api.post<DpBilling>(`${DP}/billings/${id(billingId)}/answers`, { answers }),
  collect: (
    billingId: number,
    input: { receiptDate?: string; bankAccount?: string; certificateRef?: string },
  ) => api.post<DpBilling>(`${DP}/billings/${id(billingId)}/collect`, input),
  uploadResponses: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<{ runNo: string; status: string; message: string }>(`${DP}/responses`, form);
  },
  schemes: (companyId: number) => api.get<Scheme[]>(`${INC}/schemes${toQuery({ companyId })}`),
  createScheme: (companyId: number, code: string, terms: SchemeTerms) =>
    api.post<Scheme>(`${INC}/schemes`, { companyId, code, terms }),
  updateScheme: (schemeId: number, terms: SchemeTerms) =>
    api.put<Scheme>(`${INC}/schemes/${id(schemeId)}`, terms),
  runs: (companyId: number, schemeId: number | undefined, page: number) =>
    api.get<PageResponse<IncentiveRun>>(
      `${INC}/runs${toQuery({ companyId, schemeId, page, size: PAGE })}`,
    ),
  runLines: (runId: number, page: number) =>
    api.get<PageResponse<RunLine>>(`${INC}/runs/${id(runId)}/lines${toQuery({ page })}`),
  compute: (schemeId: number, from: string, to: string) =>
    api.post<IncentiveRun>(`${INC}/runs`, { schemeId, from, to }),
  postRun: (runId: number) => api.post<IncentiveRun>(`${INC}/runs/${id(runId)}/post`),
  cancelRun: (runId: number) => api.post<IncentiveRun>(`${INC}/runs/${id(runId)}/cancel`),
  certificates: (companyId: number, stage: string | undefined, page: number) =>
    api.get<PageResponse<Certificate>>(
      `${BASE}/certificates${toQuery({ companyId, stage, page, size: PAGE })}`,
    ),
  certificate: (certId: number) => api.get<Certificate>(`${BASE}/certificates/${id(certId)}`),
  receipts: (companyId: number, insurer: string) =>
    api.get<OrLink[]>(`${BASE}/certificates/receipts${toQuery({ companyId, insurer })}`),
  submitCertificate: (input: CertificateInput) =>
    api.post<Certificate>(`${BASE}/certificates`, input),
  resubmitCertificate: (certId: number, input: CertificateInput) =>
    api.put<Certificate>(`${BASE}/certificates/${id(certId)}`, input),
  acknowledge: (certId: number, comment: string) =>
    api.post<Certificate>(`${BASE}/certificates/${id(certId)}/acknowledge`, { comment }),
  reject: (certId: number, reason: string) =>
    api.post<Certificate>(`${BASE}/certificates/${id(certId)}/reject`, { reason }),
  estimated: (companyId: number, page: number) =>
    api.get<PageResponse<EstimatedItem>>(
      `${BASE}/estimated${toQuery({ companyId, page, size: PAGE })}`,
    ),
  flagEstimated: (invoiceNo: string, estimated: boolean, reason: string) =>
    api.post<EstimatedItem>(`${BASE}/estimated`, { invoiceNo, estimated, reason }),
  settings: () => api.get<CommissionSettings>(`${BASE}/settings`),
};
