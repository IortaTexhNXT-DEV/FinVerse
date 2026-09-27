import { api, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * ACSL API (BRD-5 ACSL 2.2-2.16): cases, correction entries, insurer SOA uploads with their
 * reconciliation, and the GL-SL reconciliation.
 */

export const CASE_ENTITY = 'AcslCase';
export const CORRECTION_ENTITY = 'AcslCorrection';

export type CaseType =
  'INVESTIGATION' | 'ANALYSIS_REQUEST' | 'REFUND_APPLICATION' | 'PAYMENT_REVERSAL';
export type CaseStage =
  'RECEIVED' | 'ASSIGNED' | 'INVESTIGATING' | 'RESULT_PROVIDED' | 'CORRECTION';
export type CaseOutcome = 'CONFIRMED' | 'REJECTED' | 'NO_ACTION' | 'CORRECTION';
export type CorrectionStage =
  'ASSIGNED' | 'DRAFT' | 'FOR_REVIEW' | 'FOR_APPROVAL' | 'POSTED' | 'CANCELLED';
export type Side = 'DEBIT' | 'CREDIT';
export type LineOrigin = 'REVERSAL' | 'REPOST' | 'MANUAL';
export type ReconBucket =
  'OUTSTANDING' | 'FOR_REMITTANCE' | 'REMITTED' | 'CANCELLED' | 'DIRECT_BILLED' | 'NOT_FOUND';
export type SlSource = 'PARTY_LEDGER' | 'OPEN_ITEMS' | 'OPS_LEDGER';

export interface CaseAccount {
  invoiceNo?: string;
  rootInvoiceNo?: string;
  insurerCode?: string;
  clientCode?: string;
  arNo?: string;
  currency?: string;
  amount?: number;
}

export interface AcslCase {
  id: number;
  caseNo: string;
  type: CaseType;
  stage: CaseStage;
  account: CaseAccount;
  subject: string;
  details?: string;
  requesterModule?: string;
  requesterRef?: string;
  requestedBy: string;
  findings?: string;
  outcome?: CaseOutcome;
  resultRemarks?: string;
  resultBy?: string;
  resultAt?: string;
  correctionId?: number;
  reversalRef?: string;
  reversalStatus?: string;
  reversalMessage?: string;
  createdAt: string;
}

export interface CorrectionLine {
  lineNo?: number;
  accountCode: string;
  side: Side;
  amount: number;
  partyCode?: string;
  invoiceNo?: string;
  component?: string;
  costCenter?: string;
  businessLine?: string;
  narration?: string;
  origin?: LineOrigin;
  originalBatchNo?: string;
  originalLineNo?: number;
}

export interface CorrectionSummary {
  id: number;
  correctionNo: string;
  kind: string;
  stage: CorrectionStage;
  invoiceNo?: string;
  description: string;
  currency: string;
  journalBatchNo?: string;
  createdBy: string;
  createdAt: string;
}

export interface Correction extends CorrectionSummary {
  caseId?: number;
  rootInvoiceNo?: string;
  originalBatchNo?: string;
  trail: {
    submittedBy?: string;
    submittedAt?: string;
    reviewedBy?: string;
    reviewedAt?: string;
    approvedBy?: string;
    approvedAt?: string;
  };
  returnComment?: string;
  postedAt?: string;
  openItems: number;
  ledgerMovements: number;
  totalDebit: number;
  totalCredit: number;
  lines: CorrectionLine[];
}

export interface OriginalLine {
  batchNo: string;
  journalType: string;
  valueDate: string;
  lineNo: number;
  accountCode: string;
  accountName: string;
  side: Side;
  amount: number;
  partyCode?: string;
  costCenter?: string;
  businessLine?: string;
  narration?: string;
}

export interface RunView {
  runNo: number;
  runAt: string;
  runBy: string;
  outstanding: number;
  forRemittance: number;
  remitted: number;
  cancelled: number;
  directBilled: number;
  notFound: number;
  withVariance: number;
}

export interface SoaUpload {
  id: number;
  uploadNo: string;
  insurerCode: string;
  periodFrom: string;
  periodTo: string;
  fileName: string;
  layoutCode: string;
  rowsRead: number;
  rowsLoaded: number;
  rowsFailed: number;
  run?: RunView;
  reportName: string;
  createdBy: string;
  createdAt: string;
}

export interface LogRow {
  rowNo: number;
  status: 'LOADED' | 'FAILED';
  error?: string;
  invoiceNo?: string;
  policyNo?: string;
  assuredName?: string;
  grossPremium?: number;
  balance?: number;
  paid?: number;
}

export interface BookFigures {
  premium?: number;
  outstanding?: number;
  forRemittance?: number;
  remitted?: number;
  remittanceBatchNo?: string;
  remittanceDate?: string;
  pr2307Amount?: number;
  pr2307BatchNo?: string;
  pr2307Date?: string;
  cancellationRef?: string;
  directBilled: boolean;
}

export interface ReconRow {
  rowNo: number;
  invoiceNo?: string;
  policyNo?: string;
  assuredName?: string;
  rootInvoiceNo?: string;
  bucket: ReconBucket;
  book: BookFigures;
  soaPremium?: number;
  soaBalance?: number;
  premiumVariance?: number;
  outstandingVariance?: number;
}

export interface SoaLayout {
  insurerCode: string;
  name: string;
  headers: string[];
  active: boolean;
}

export interface GlSlRun {
  id: number;
  asOf: string;
  runAt: string;
  runBy: string;
  accounts: number;
  differences: number;
  totalDifference: number;
}

export interface GlSlRow {
  accountCode: string;
  accountName: string;
  source: SlSource;
  glBalance: number;
  slBalance: number;
  difference: number;
}

export interface GlSlControl {
  accountCode: string;
  source: SlSource;
  components?: string;
  documentTypes?: string;
  currency?: string;
  active: boolean;
}

export interface CaseInput {
  type: CaseType;
  invoiceNo?: string;
  arNo?: string;
  amount?: number;
  subject: string;
  details?: string;
}

export interface CorrectionInput {
  kind: string;
  invoiceNo?: string;
  originalBatchNo?: string;
  description: string;
}

export interface ProposalInput {
  batchNo: string;
  lineNo: number;
  targetAccountCode: string;
  targetPartyCode?: string;
  component?: string;
  targetComponent?: string;
}

export type Counts<T extends string> = Partial<Record<T, number>>;

const enc = encodeURIComponent;
const caseUrl = (id: number) => `/acsl/cases/${String(id)}`;
const correctionUrl = (id: number) => `/acsl/corrections/${String(id)}`;
const uploadUrl = (id: number) => `/acsl/soa-uploads/${String(id)}`;

export const acslApi = {
  cases: (
    companyId: number,
    f: { stage?: CaseStage; type?: CaseType; q?: string; page?: number },
  ) =>
    api.get<PageResponse<AcslCase>>(
      `/acsl/cases${toQuery({ companyId, ...f, page: f.page ?? 0, size: 20 })}`,
    ),
  caseCounts: (companyId: number) =>
    api.get<Counts<CaseStage>>(`/acsl/cases/counts${toQuery({ companyId })}`),
  getCase: (id: number) => api.get<AcslCase>(caseUrl(id)),
  familyCases: (invoiceNo: string) => api.get<AcslCase[]>(`/acsl/invoices/${enc(invoiceNo)}/cases`),
  openCase: (companyId: number, input: CaseInput) =>
    api.post<AcslCase>(`/acsl/cases${toQuery({ companyId })}`, input),
  assignCase: (id: number, username: string) =>
    api.post<AcslCase>(`${caseUrl(id)}/assign`, { username }),
  findings: (id: number, findings: string) =>
    api.put<AcslCase>(`${caseUrl(id)}/findings`, { findings }),
  result: (id: number, outcome: CaseOutcome, remarks?: string) =>
    api.post<AcslCase>(`${caseUrl(id)}/result`, { outcome, remarks }),
  raiseCorrection: (id: number, input: CorrectionInput) =>
    api.post<Correction>(`${caseUrl(id)}/correction`, input),
  reversal: (id: number, body: { receiptNo: string; amount?: number; reason: string }) =>
    api.post<AcslCase>(`${caseUrl(id)}/payment-reversal`, body),
  messageAo: (id: number, message: string) =>
    api.post<{ accountOfficer: string }>(`${caseUrl(id)}/message-ao`, { message }),
  corrections: (companyId: number, f: { stage?: CorrectionStage; q?: string; page?: number }) =>
    api.get<PageResponse<CorrectionSummary>>(
      `/acsl/corrections${toQuery({ companyId, ...f, page: f.page ?? 0, size: 20 })}`,
    ),
  correctionCounts: (companyId: number) =>
    api.get<Counts<CorrectionStage>>(`/acsl/corrections/counts${toQuery({ companyId })}`),
  getCorrection: (id: number) => api.get<Correction>(correctionUrl(id)),
  originalLines: (id: number) => api.get<OriginalLine[]>(`${correctionUrl(id)}/original-lines`),
  createCorrection: (companyId: number, input: CorrectionInput) =>
    api.post<Correction>(`/acsl/corrections${toQuery({ companyId })}`, input),
  assignCorrection: (id: number, username: string) =>
    api.post<Correction>(`${correctionUrl(id)}/assign`, { username }),
  propose: (id: number, input: ProposalInput) =>
    api.post<Correction>(`${correctionUrl(id)}/propose`, input),
  saveLines: (id: number, lines: CorrectionLine[]) =>
    api.put<Correction>(`${correctionUrl(id)}/lines`, { lines }),
  submitCorrection: (id: number, comment?: string) =>
    api.post<Correction>(`${correctionUrl(id)}/submit`, { comment }),
  endorseCorrection: (id: number, comment?: string) =>
    api.post<Correction>(`${correctionUrl(id)}/endorse`, { comment }),
  approveCorrection: (id: number, comment?: string) =>
    api.post<Correction>(`${correctionUrl(id)}/approve`, { comment }),
  uploads: (companyId: number, q: string, page: number) =>
    api.get<PageResponse<SoaUpload>>(
      `/acsl/soa-uploads${toQuery({ companyId, q, page, size: 20 })}`,
    ),
  getUpload: (id: number) => api.get<SoaUpload>(uploadUrl(id)),
  uploadLog: (id: number) => api.get<LogRow[]>(`${uploadUrl(id)}/log`),
  results: (id: number, bucket?: ReconBucket) =>
    api.get<ReconRow[]>(`${uploadUrl(id)}/results${toQuery({ bucket })}`),
  report: (id: number): Promise<DownloadedFile> => api.getFile(`${uploadUrl(id)}/report`),
  reconcile: (id: number) => api.post<SoaUpload>(`${uploadUrl(id)}/reconcile`),
  layouts: () => api.get<SoaLayout[]>('/acsl/soa-layouts'),
  uploadSoa: (
    companyId: number,
    meta: { insurerCode: string; periodFrom: string; periodTo: string },
    file: File,
  ) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<SoaUpload>(`/acsl/soa-uploads${toQuery({ companyId, ...meta })}`, form);
  },
  glSlRuns: (companyId: number) => api.get<GlSlRun[]>(`/acsl/gl-sl/runs${toQuery({ companyId })}`),
  glSlRows: (runId: number) => api.get<GlSlRow[]>(`/acsl/gl-sl/runs/${String(runId)}/rows`),
  runGlSl: (companyId: number, asOf: string) =>
    api.post<GlSlRun>(`/acsl/gl-sl/runs${toQuery({ companyId, asOf })}`),
  controls: (companyId: number) =>
    api.get<GlSlControl[]>(`/acsl/gl-sl/controls${toQuery({ companyId })}`),
  configure: (companyId: number, control: GlSlControl) =>
    api.put<GlSlControl>(`/acsl/gl-sl/controls${toQuery({ companyId })}`, control),
};
