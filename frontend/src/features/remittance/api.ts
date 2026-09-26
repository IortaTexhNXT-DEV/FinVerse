import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type { Amounts, Settlement } from './amounts';

/** Remittance API client (RMTID.001-039, MKTID.001-009): /api/v1/remittance. */

export type RemittanceType = 'WITH_INCENTIVES' | 'NORMAL_PHP' | 'NORMAL_USD' | 'SPECIAL';
export type BatchStage =
  | 'REVIEW_IN_PROCESS'
  | 'ON_HOLD'
  | 'FOR_APPROVAL'
  | 'APPROVED'
  | 'PARTIALLY_REMITTED'
  | 'FULLY_REMITTED'
  | 'OR_RECEIVED'
  | 'RETURNED';
export type ExtractionTag = 'EXTRACTED' | 'UNEXTRACTED_NOT_DUE' | 'UNEXTRACTED_DUE' | 'RETURNED';
export type HoldStage =
  | 'DRAFT'
  | 'FOR_APPROVAL'
  | 'ACTIVE'
  | 'EXTENSION_FOR_APPROVAL'
  | 'CANCEL_FOR_APPROVAL'
  | 'RELEASED'
  | 'REJECTED'
  | 'CANCELLED';
export type SpecialStage =
  | 'REQUESTED'
  | 'FOR_APPROVAL'
  | 'IN_PROCESS_REMITTANCE'
  | 'PUSHED_TO_DISBURSEMENT'
  | 'REJECTED'
  | 'RETURNED';
export type DocumentKind = 'SCHEDULE_PDF' | 'SCHEDULE_XLSX' | 'PAYMENT_REQUEST_PDF';

export type { Amounts, Settlement } from './amounts';

export interface ExtractionRun {
  id: number;
  runNo: string;
  trigger: string;
  insurerCode?: string;
  type?: RemittanceType;
  invoiceNo?: string;
  businessDate: string;
  startedAt: string;
  endedAt?: string;
  status: 'RUNNING' | 'SUCCEEDED' | 'FAILED';
  examined: number;
  extracted: number;
  dueNotExtracted: number;
  notDue: number;
  batches: number;
  message?: string;
  createdBy: string;
}

export interface InvoiceTag {
  invoiceNo: string;
  insurerCode: string;
  type?: RemittanceType;
  tag: ExtractionTag;
  reasons?: string;
  remarks?: string;
  paidAr: number;
  dtipBalance: number;
  remittable: number;
  batchNo?: string;
  taggedAt: string;
}

export interface BatchSummary {
  id: number;
  batchNo: string;
  insurerCode: string;
  type: RemittanceType;
  currency: string;
  stage: BatchStage;
  processor?: string;
  lineCount: number;
  totals: Amounts;
  createdAt: string;
  submittedAt?: string;
  approvedAt?: string;
  dvNo?: string;
  specialRequestNo?: string;
}

export interface Exclusion {
  excluded: boolean;
  reason?: string;
  comment?: string;
  excludedBy?: string;
  excludedAt?: string;
  restoredBy?: string;
  restoredAt?: string;
}

export interface InsurerOr {
  orNo: string;
  orDate: string;
  amount: number;
  status: 'MATCHED' | 'AMOUNT_MISMATCH';
  runNo: string;
}

export interface BatchLine {
  invoiceNo: string;
  arn: string;
  endorsementNo?: string;
  policyNo?: string;
  clientCode: string;
  assuredName: string;
  riskCode?: string;
  inceptionDate: string;
  bookingDate: string;
  lastPaidOn?: string;
  basicPremium: number;
  cpc2Code?: string;
  cpc2Rate?: number;
  amounts: Amounts;
  exclusion?: Exclusion;
  insurerOr?: InsurerOr;
  journalBatchNo?: string;
  remittedStatus?: string;
}

export interface Batch {
  summary: BatchSummary;
  insurerName: string;
  submittedBy?: string;
  approvedBy?: string;
  returnReason?: string;
  disbursement: { requestNo?: string; status?: string; amount?: number };
  receipts: {
    commissionOrNo?: string;
    commissionOrStatus?: string;
    incentiveOrNo?: string;
    incentiveOrStatus?: string;
    message?: string;
  };
  settlement: Settlement;
  scheduleSentAt?: string;
  extractFileId?: number;
  lines: BatchLine[];
}

export interface Preview {
  totals: Amounts;
  lineCount: number;
  excludedCount: number;
  lines: BatchLine[];
  problems: string[];
}

export interface AccountHit {
  batchId: number;
  batchNo: string;
  stage: BatchStage;
  line: BatchLine;
}

export interface DtipRow {
  invoiceNo: string;
  arn: string;
  insurerCode: string;
  assuredName: string;
  currency: string;
  inceptionDate: string;
  paymentStatus: string;
  remittanceStatus: string;
  hold: boolean;
  pendingNegativeAdjustment: boolean;
  writtenOff: boolean;
  lockOwner?: string;
  paidAr: number;
  dtipDue: number;
  dtipRemitted: number;
  dtipBalance: number;
  tag?: ExtractionTag;
  reasons?: string;
}

export interface IncentiveRule {
  id: number;
  insurerCode: string;
  productLine?: string;
  segment?: string;
  rate: number;
  windowDays: number;
  basis: 'INCEPTION' | 'BOOKING';
  effectiveFrom: string;
  effectiveTo?: string;
  active: boolean;
  description?: string;
}

export type IncentiveRuleInput = Omit<IncentiveRule, 'id'> & { companyId: number };

export interface FeedRun {
  id: number;
  runNo: string;
  feedCode: string;
  fileName?: string;
  status: string;
  read: number;
  accepted: number;
  duplicates: number;
  failed: number;
  message?: string;
  errorDetail?: string;
  startedAt: string;
  createdBy: string;
}

export interface FeedRecord {
  key: string;
  status: string;
  reference?: string;
  message?: string;
}

export interface OrUpload {
  run: FeedRun;
  updated: AccountHit[];
  records: FeedRecord[];
  matched: number;
  mismatched: number;
}

export interface Hold {
  id: number;
  requestNo: string;
  invoiceNo: string;
  arn: string;
  insurerCode: string;
  clientCode: string;
  assuredName: string;
  reasonCode: string;
  remarks?: string;
  holdUntil: string;
  requestedUntil?: string;
  extensionCount: number;
  stage: HoldStage;
  source: string;
  requestedBy: string;
  approvedBy?: string;
  assignedProcessor?: string;
  releasedAt?: string;
  releaseNote?: string;
  createdAt: string;
}

export interface HoldInput {
  companyId: number;
  invoiceNo: string;
  reasonCode: string;
  remarks?: string;
  holdUntil: string;
  submit: boolean;
}

export interface Special {
  id: number;
  requestNo: string;
  invoiceNo: string;
  arn: string;
  insurerCode: string;
  clientCode: string;
  assuredName: string;
  policyNo?: string;
  segment?: string;
  conditionCode: string;
  remarks?: string;
  stage: SpecialStage;
  source: string;
  requestedBy: string;
  validationNote?: string;
  approvedBy?: string;
  rejectedReason?: string;
  batchNo?: string;
  pushedAt?: string;
  createdAt: string;
}

export interface EodRequest {
  invoiceNo: string;
  requestedOn: string;
  requestedBy: string;
  runNo?: string;
  tag?: ExtractionTag;
  processedAt?: string;
}

const BASE = '/remittance';
const enc = encodeURIComponent;

type QueryValue = string | number | boolean | null | undefined | readonly string[];

/** A query string; arrays repeat their key (?stage=A&stage=B), empty values are left out. */
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

function uploadFile<T>(path: string, file: File): Promise<T> {
  const form = new FormData();
  form.append('file', file);
  return api.upload<T>(path, form);
}

export const remittanceApi = {
  runs: (companyId: number, page: number) =>
    api.get<PageResponse<ExtractionRun>>(`${BASE}/runs${toQuery({ companyId, page, size: 20 })}`),
  tags: (runId: number, page: number) =>
    api.get<PageResponse<InvoiceTag>>(`${BASE}/runs/${runId}/tags${toQuery({ page, size: 50 })}`),
  extract: (body: {
    companyId: number;
    insurerCode?: string;
    type?: RemittanceType;
    invoiceNo?: string;
  }) => api.post<ExtractionRun>(`${BASE}/runs`, body),
  queueEod: (companyId: number, invoiceNo: string) =>
    api.post<EodRequest>(`${BASE}/eod-requests`, { companyId, invoiceNo }),
  eodRequests: (companyId: number) =>
    api.get<EodRequest[]>(`${BASE}/eod-requests${toQuery({ companyId })}`),
  accounts: (companyId: number, q: string, page: number) =>
    api.get<PageResponse<AccountHit>>(
      `${BASE}/accounts${toQuery({ companyId, q: q || undefined, page, size: 20 })}`,
    ),
  dtip: (companyId: number, q: string, insurer: string, status: string, page: number) =>
    api.get<PageResponse<DtipRow>>(
      `${BASE}/dtip${toQuery({
        companyId,
        q: q || undefined,
        insurer: insurer || undefined,
        status: status || undefined,
        page,
        size: 20,
      })}`,
    ),
  incentiveRules: (companyId: number) =>
    api.get<IncentiveRule[]>(`${BASE}/incentive-rules${toQuery({ companyId })}`),
  saveIncentiveRule: (id: number | undefined, body: IncentiveRuleInput) =>
    id === undefined
      ? api.post<IncentiveRule>(`${BASE}/incentive-rules`, body)
      : api.put<IncentiveRule>(`${BASE}/incentive-rules/${id}`, body),
  uploadInsurerOr: (file: File) => uploadFile<OrUpload>(`${BASE}/insurer-or/upload`, file),
  insurerOrRuns: (page: number) =>
    api.get<PageResponse<FeedRun>>(`${BASE}/insurer-or/runs${toQuery({ page, size: 10 })}`),
  insurerOrRun: (id: number) => api.get<OrUpload>(`${BASE}/insurer-or/runs/${id}`),
  batches: (
    companyId: number,
    stages: readonly BatchStage[],
    filters: { q: string; insurer: string; type: string },
    page: number,
  ) =>
    api.get<PageResponse<BatchSummary>>(
      `${BASE}/batches${toQuery({
        companyId,
        stage: stages,
        q: filters.q || undefined,
        insurer: filters.insurer || undefined,
        type: filters.type || undefined,
        page,
        size: 20,
      })}`,
    ),
  batch: (id: number) => api.get<Batch>(`${BASE}/batches/${id}`),
  preview: (id: number) => api.get<Preview>(`${BASE}/batches/${id}/preview`),
  exclude: (id: number, invoiceNos: string[], reasonCode: string, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/exclude`, { invoiceNos, reasonCode, comment }),
  restore: (id: number, invoiceNo: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/restore`, { invoiceNo }),
  submit: (id: number, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/submit`, { comment }),
  approve: (id: number, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/approve`, { comment }),
  returnBatch: (id: number, reasonCode: string, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/return`, { reasonCode, comment }),
  assign: (id: number, username: string) =>
    api.post<Batch>(`${BASE}/batches/${id}/assign`, { username }),
  document: (id: number, kind: DocumentKind) =>
    api.getFile(`${BASE}/batches/${id}/documents/${kind}`),
  sendSchedule: (id: number, body: { to: string[]; subject: string; body: string }) =>
    api.post<{ messageId: number }>(`${BASE}/batches/${id}/send-schedule`, body),
  holds: (companyId: number, stages: readonly HoldStage[], q: string, page: number) =>
    api.get<PageResponse<Hold>>(
      `${BASE}/holds${toQuery({ companyId, stage: stages, q: q || undefined, page, size: 20 })}`,
    ),
  hold: (id: number) => api.get<Hold>(`${BASE}/holds/${id}`),
  createHold: (body: HoldInput) => api.post<Hold>(`${BASE}/holds`, body),
  holdAction: (id: number, action: string, body?: unknown) =>
    api.post<Hold>(`${BASE}/holds/${id}/${action}`, body),
  uploadHolds: (file: File) => uploadFile<FeedRun>(`${BASE}/holds/upload`, file),
  specials: (companyId: number, stages: readonly SpecialStage[], q: string, page: number) =>
    api.get<PageResponse<Special>>(
      `${BASE}/special${toQuery({ companyId, stage: stages, q: q || undefined, page, size: 20 })}`,
    ),
  special: (id: number) => api.get<Special>(`${BASE}/special/${id}`),
  createSpecial: (body: {
    companyId: number;
    invoiceNo: string;
    conditionCode: string;
    remarks?: string;
  }) => api.post<Special>(`${BASE}/special`, body),
  approveSpecial: (id: number, comment?: string) =>
    api.post<Special>(`${BASE}/special/${id}/approve`, { comment }),
  rejectSpecial: (id: number, reasonCode: string, comment?: string) =>
    api.post<Special>(`${BASE}/special/${id}/reject`, { reasonCode, comment }),
  uploadSpecials: (file: File) => uploadFile<FeedRun>(`${BASE}/special/upload`, file),
  invoiceLink: (invoiceNo: string) => `/operations/invoices/${enc(invoiceNo)}`,
};
