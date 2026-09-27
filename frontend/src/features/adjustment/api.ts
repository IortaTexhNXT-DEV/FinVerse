import { api, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Adjustment API (BRD-2 ADJID.001-028, MKTID.008): endorsement requests, recompute, workflow
 * actions, posting batches, slips and minimal balance write-offs.
 */

export const REQUEST_ENTITY = 'EndorsementRequest';

export type RequestStage =
  | 'DRAFT'
  | 'FOR_VALIDATION'
  | 'FOR_APPROVAL'
  | 'FOR_POSTING'
  | 'AWAITING_REAPPLICATION'
  | 'POSTED'
  | 'RETURNED'
  | 'CANCELLED';
export type RequestClass = 'FINANCIAL' | 'NON_FINANCIAL' | 'INTERNAL';
export type RefundBasis = 'PRO_RATA' | 'SHORT_PERIOD';

export interface AmountsInput {
  basic?: number;
  dst?: number;
  premiumTaxVat?: number;
  lgt?: number;
  fst?: number;
  other?: number;
  commission?: number;
  vatOnCommission?: number;
}

export interface RequestInput {
  invoiceNos: string[];
  endorsementType: string;
  requestType?: string;
  reasonCode?: string;
  endorsementRef?: string;
  effectiveDate: string;
  refundBasis?: RefundBasis;
  sumInsuredChange?: number;
  ratePercent?: number;
  newPeriodFrom?: string;
  newPeriodTo?: string;
  description: string;
  instructions?: string;
  amounts?: AmountsInput;
  duplicateOverride?: string;
  baselineOverride?: string;
}

export interface RequestSummary {
  id: number;
  requestNo: string;
  stage: RequestStage;
  requestClass: RequestClass;
  endorsementType: string;
  requestType?: string;
  invoiceNo: string;
  arn: string;
  assuredName: string;
  insurerCode: string;
  currency: string;
  effectiveDate: string;
  negative: boolean;
  quotationRequired: boolean;
  duplicateOverride: boolean;
  batchNo?: string;
  createdBy: string;
  createdAt: string;
  agingDays: number;
}

export interface ChangeView {
  component: string;
  before: number;
  delta: number;
  after: number;
}

export interface ShareView {
  insurerCode: string;
  sharePct: number;
  lead: boolean;
  premiumDelta: number;
  commissionDelta: number;
  vatDelta: number;
}

export interface RequestTerms {
  endorsementType: string;
  requestType?: string;
  reasonCode?: string;
  endorsementRef?: string;
  effectiveDate: string;
  refundBasis: RefundBasis;
  sumInsuredChange?: number;
  ratePercent?: number;
  newPeriodFrom?: string;
  newPeriodTo?: string;
  description: string;
  instructions?: string;
}

export interface EndorsementRequest {
  id: number;
  requestNo: string;
  stage: RequestStage;
  requestClass: RequestClass;
  computation: string;
  invoice: {
    invoiceNo: string;
    arn: string;
    policyNo?: string;
    clientCode: string;
    assuredName: string;
    insurerCode: string;
    currency: string;
    segment?: string;
    aoUsername?: string;
    productLine?: string;
  };
  terms: RequestTerms;
  amounts: AmountsInput;
  control: {
    needsApproval: boolean;
    negative: boolean;
    duplicateOverride?: string;
    baselineOverride?: string;
    quotationRequired: boolean;
    quotationRef?: string;
    returnReason?: string;
    returnComment?: string;
    slipNo?: string;
  };
  trail: {
    submittedBy?: string;
    submittedAt?: string;
    validatedBy?: string;
    validatedAt?: string;
    approvedBy?: string;
    approvedAt?: string;
    postedBy?: string;
    postedAt?: string;
    completedAt?: string;
  };
  outcome: {
    batchNo?: string;
    endorsementNo?: string;
    newInvoiceNo?: string;
    serviceInvoices?: string;
    arInsurerAmount?: number;
    excessAmount?: number;
    unappliedRef?: string;
  };
  changes: ChangeView[];
  shares: ShareView[];
  journals: string[];
  createdBy: string;
  createdAt: string;
  agingDays: number;
}

export interface Recompute {
  requestClass: RequestClass;
  computation: string;
  negative: boolean;
  needsApproval: boolean;
  changes: ChangeView[];
  shares: ShareView[];
  premiumChange: number;
  commissionChange: number;
  vatChange: number;
  settlement: {
    netApplied?: number;
    remitted?: number;
    reapplication: boolean;
    arInsurer: number;
  };
  serviceInvoice: { action: 'ISSUE' | 'CREDIT' | 'NONE'; commission: number; vat: number };
  baseline?: {
    originalPremium: number;
    adjustedBefore: number;
    adjustedAfter: number;
    limitPercent: number;
    exceeded: boolean;
  };
  quotationRequired: boolean;
  duplicates: string[];
}

export interface GlLine {
  batchNo: string;
  accountCode: string;
  accountName: string;
  side: 'DEBIT' | 'CREDIT';
  amount: number;
  partyCode?: string;
  narration?: string;
}

export interface PostingBatch {
  batchNo: string;
  postedCount: number;
  pendingCount: number;
  failedCount: number;
  remarks?: string;
  createdBy: string;
  createdAt: string;
  lines: {
    requestId: number;
    requestNo: string;
    invoiceNo: string;
    outcome: 'POSTED' | 'AWAITING_REAPPLICATION' | 'FAILED';
    message?: string;
  }[];
}

export interface WriteOff {
  invoiceNo: string;
  arn: string;
  clientCode: string;
  currency: string;
  balance: number;
  action: 'WRITE_OFF' | 'CREDIT';
  fileRef: string;
  journalBatchNo?: string;
  createdBy: string;
  createdAt: string;
}

export type StageCounts = Partial<Record<RequestStage, number>>;

const BASE = '/adjustment';

const comment = (text?: string) => ({ comment: text });

export const adjustmentApi = {
  requests: (companyId: number, stage: RequestStage | undefined, q: string, page: number) =>
    api.get<PageResponse<RequestSummary>>(
      `${BASE}/requests${toQuery({ companyId, stage, q, page, size: 20 })}`,
    ),
  counts: (companyId: number) =>
    api.get<StageCounts>(`${BASE}/requests/counts${toQuery({ companyId })}`),
  get: (id: number) => api.get<EndorsementRequest>(`${BASE}/requests/${String(id)}`),
  recompute: (id: number) => api.get<Recompute>(`${BASE}/requests/${String(id)}/recompute`),
  journal: (id: number) => api.get<GlLine[]>(`${BASE}/requests/${String(id)}/journal`),
  forInvoice: (invoiceNo: string) =>
    api.get<RequestSummary[]>(`${BASE}/invoices/${encodeURIComponent(invoiceNo)}/requests`),
  preview: (input: RequestInput) => api.post<Recompute>(`${BASE}/requests/preview`, input),
  create: (input: RequestInput) => api.post<EndorsementRequest[]>(`${BASE}/requests`, input),
  update: (id: number, input: RequestInput) =>
    api.put<EndorsementRequest>(`${BASE}/requests/${String(id)}`, input),
  submit: (id: number, text?: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/submit`, comment(text)),
  resubmit: (id: number, text?: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/resubmit`, comment(text)),
  validate: (id: number, text?: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/validate`, comment(text)),
  approve: (id: number, text?: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/approve`, comment(text)),
  post: (id: number, text?: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/post`, comment(text)),
  reapply: (id: number) => api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/reapply`),
  linkQuotation: (id: number, quotationRef: string) =>
    api.post<EndorsementRequest>(`${BASE}/requests/${String(id)}/quotation`, { quotationRef }),
  endorsementSlip: (id: number): Promise<DownloadedFile> =>
    api.getFile(`${BASE}/requests/${String(id)}/endorsement-slip`),
  validationSlip: (id: number): Promise<DownloadedFile> =>
    api.getFile(`${BASE}/requests/${String(id)}/validation-slip`),
  postBatch: (companyId: number, ids: number[], remarks?: string) =>
    api.post<PostingBatch>(`${BASE}/batches`, { companyId, ids, remarks }),
  returnRequests: (ids: number[], reasonCode: string, text?: string) =>
    api.post<{ returned: number }>(`${BASE}/batches/return`, {
      ids,
      reasonCode,
      comment: text,
    }),
  batches: (companyId: number, page: number) =>
    api.get<PageResponse<PostingBatch>>(`${BASE}/batches${toQuery({ companyId, page, size: 20 })}`),
  batch: (batchNo: string) =>
    api.get<PostingBatch>(`${BASE}/batches/${encodeURIComponent(batchNo)}`),
  writeOffs: (companyId: number, page: number) =>
    api.get<PageResponse<WriteOff>>(`${BASE}/write-offs${toQuery({ companyId, page, size: 20 })}`),
};
