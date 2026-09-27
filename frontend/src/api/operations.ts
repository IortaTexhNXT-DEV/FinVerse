import { api, toQuery } from './client';
import type { PageResponse } from './types';

/**
 * Operations foundation (opsledger, BRD-2): invoice ledger and 360, Operations home, Disbursement
 * queue, interfaces (flow-in), hand-offs, extract repository, notification preferences and the
 * report archive.
 */

export type LedgerComponent =
  | 'BASIC'
  | 'DST'
  | 'PREMIUM_TAX_VAT'
  | 'LGT'
  | 'FST'
  | 'OTHER'
  | 'DTIP'
  | 'COMMISSION'
  | 'COMMISSION_VAT'
  | 'WTAX'
  | 'PR2307';
export type PaymentStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'NOT_APPLICABLE';
export type RemittanceStatus =
  | 'UNPROCESSED'
  | 'UNAPPLIED_PAYMENT'
  | 'WITH_OUTSTANDING_BALANCE'
  | 'REVIEW_IN_PROCESS'
  | 'REQUESTED_FOR_HOLD'
  | 'APPROVED'
  | 'PARTIALLY_REMITTED'
  | 'FULLY_REMITTED'
  | 'NOT_APPLICABLE';
export type InvoiceFlag = 'HOLD' | 'PENDING_NEG_ADJ' | 'WRITTEN_OFF' | 'CANCELLED' | 'ESTIMATED';
export type OpsSection =
  | 'CASHIERING'
  | 'REMITTANCE'
  | 'PRODRECON'
  | 'ADJUSTMENT'
  | 'COMMISSION'
  | 'DISBURSEMENT'
  | 'INTERFACES';
export type Severity = 'INFO' | 'WARNING' | 'ALERT';
export type DisbursementStatus =
  'SENT' | 'ACKNOWLEDGED' | 'DV_ASSIGNED' | 'PAID' | 'RETURNED' | 'CANCELLED';
/** Gateway payment types (opsledger DisbursementRequest.Type; BRD-5 adds the non-Operations ones). */
export type DisbursementType =
  | 'REMITTANCE'
  | 'REFUND'
  | 'CWT2307'
  | 'PASS_ON'
  | 'SUPPLIER'
  | 'GOVERNMENT'
  | 'OTHER_BANK_UNIT'
  | 'EMPLOYEE'
  | 'CASH_ADVANCE'
  | 'SERVICE_FEE'
  | 'OTHER';
export type RelatedSection =
  'RECEIPTS' | 'REMITTANCES' | 'ADJUSTMENTS' | 'RECONCILIATION' | 'COMMISSION' | 'DOCUMENTS';

export interface InvoiceFlags {
  directPayment: boolean;
  cwt2Percent: boolean;
  incentiveEligible: boolean;
  hold: boolean;
  pendingNegativeAdjustment: boolean;
  writtenOff: boolean;
  cancelled: boolean;
  estimated: boolean;
  lockOwner?: string;
  lockReason?: string;
  lockedBy?: string;
  lockedAt?: string;
}

export interface OpsInvoiceSummary {
  invoiceNo: string;
  arn: string;
  kind: string;
  policyNo?: string;
  clientCode: string;
  assuredName: string;
  insurerCode: string;
  currency: string;
  bookingDate: string;
  grossPremium: number;
  premiumBalance: number;
  paymentStatus: PaymentStatus;
  remittanceStatus: RemittanceStatus;
  flags: InvoiceFlags;
  inceptionDate?: string;
  aoUsername?: string;
  /** Root of the invoice family (DIS 3.27.2). */
  rootInvoiceNo?: string;
}

export interface InvoiceComponentRow {
  component: LedgerComponent;
  premiumReceivable: boolean;
  booked: number;
  applied: number;
  reversed: number;
  remitted: number;
  adjusted: number;
  writtenOff: number;
  balance: number;
}

export interface OpsInvoice {
  keys: {
    invoiceNo: string;
    arn: string;
    accountId?: number;
    kind: string;
    endorsementNo?: string;
    parentInvoiceNo?: string;
    /** Root of the invoice family (DIS 3.27.2). */
    rootInvoiceNo?: string;
    policyNo?: string;
    policyYear: number;
    pnNos?: string;
  };
  parties: { clientCode: string; assuredName: string; payorName?: string; insurerCode: string };
  classification: {
    currency: string;
    bookingDate: string;
    inceptionDate: string;
    expiryDate: string;
    riskCode?: string;
    productLine?: string;
    segment?: string;
    aoUsername?: string;
    salesUnit?: string;
    costCenter?: string;
  };
  grossPremium: number;
  commission: number;
  vatOnCommission: number;
  wtaxRate: number;
  premiumBalance: number;
  paymentStatus: PaymentStatus;
  remittanceStatus: RemittanceStatus;
  flags: InvoiceFlags;
  feedSource: 'EVENT' | 'REPLAY';
  components: InvoiceComponentRow[];
  shares: { insurerCode: string; sharePct: number; lead: boolean }[];
}

export interface Movement {
  id: number;
  type: string;
  component: LedgerComponent;
  amount: number;
  sourceModule: string;
  sourceRef: string;
  arNo?: string;
  orNo?: string;
  batchNo?: string;
  valueDate: string;
  journalBatchNo?: string;
  remarks?: string;
  postedAt: string;
  postedBy: string;
}

export interface StatusChange {
  id: number;
  field: string;
  from?: string;
  to?: string;
  module: string;
  reason?: string;
  changedAt: string;
  changedBy: string;
}

export interface RelatedItem {
  type: string;
  reference: string;
  date?: string;
  amount?: number;
  status?: string;
  description?: string;
  link?: string;
}

export interface Invoice360 {
  invoice: OpsInvoice;
  booking: { bookedInvoiceId?: number; serviceInvoiceNo?: string; journalBatches: string[] };
  movements: Movement[];
  history: StatusChange[];
  adjustments?: {
    originalInvoiceNo: string;
    originalPremium: number;
    originalDtip: number;
    originalCommission: number;
    adjustedPremium: number;
    adjustedDtip: number;
    adjustedCommission: number;
    adjustmentCount: number;
    overAdjusted: boolean;
  };
  related: Partial<Record<RelatedSection, RelatedItem[]>>;
}

export interface WorkCount {
  section: OpsSection;
  key: string;
  label: string;
  count: number;
  severity: Severity;
  link?: string;
}

export interface OperationsHome {
  sections: { section: OpsSection; permissions: string[]; counts: WorkCount[] }[];
  links: { code: string; name: string; url?: string }[];
}

export interface InvoiceSearch {
  q?: string;
  payment?: PaymentStatus;
  remittance?: RemittanceStatus;
  flag?: InvoiceFlag;
  locked?: boolean;
  dp?: boolean;
  insurer?: string;
  from?: string;
  to?: string;
  /** Part of the assured name (DIS 3.27.2). */
  assured?: string;
  inceptionFrom?: string;
  inceptionTo?: string;
  /** Account officer. */
  ao?: string;
}

export interface Disbursement {
  id: number;
  requestNo: string;
  type: DisbursementType;
  sourceModule: string;
  sourceRef: string;
  payeeCode: string;
  payeeName?: string;
  currency: string;
  amount: number;
  description?: string;
  status: DisbursementStatus;
  dvNo?: string;
  sentAt: string;
  acknowledgedAt?: string;
  dvAssignedAt?: string;
  paidAt?: string;
  returnedAt?: string;
  returnReason?: string;
  sentBy: string;
  rfpNo?: string;
  rootInvoiceNo?: string;
  dvStatus?: string;
  instrumentStatus?: string;
  cancelledAt?: string;
  cancelReason?: string;
}

export interface Feed {
  code: string;
  name: string;
  partnerSystem: string;
  direction: 'INBOUND' | 'OUTBOUND';
  transport: string;
  ownerModule: string;
  cron: string;
  active: boolean;
  description?: string;
  uploadable: boolean;
}

export interface FeedRun {
  id: number;
  feedCode: string;
  runNo: string;
  trigger: string;
  status: 'RUNNING' | 'SUCCEEDED' | 'PARTIAL' | 'FAILED';
  startedAt: string;
  endedAt?: string;
  readCount: number;
  okCount: number;
  duplicateCount: number;
  failedCount: number;
  fileName?: string;
  message?: string;
  errorDetail?: string;
}

export interface FeedRecord {
  id: number;
  idempotencyKey: string;
  status: 'ACCEPTED' | 'FAILED';
  reference?: string;
  message?: string;
  receivedAt: string;
}

export interface Handoff {
  id: number;
  port: string;
  sourceModule: string;
  sourceRef: string;
  reference?: string;
  amount?: number;
  currency?: string;
  summary: string;
  status: 'OPEN' | 'CLOSED';
  createdAt: string;
  closedAt?: string;
  closedBy?: string;
  closingNote?: string;
}

export interface ExtractFileInfo {
  id: number;
  folder: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  sourceModule: string;
  sourceRef?: string;
  createdAt: string;
  createdBy: string;
}

export interface NotificationChannels {
  code: string;
  name: string;
  module: string;
  description?: string;
  inApp: boolean;
  email: boolean;
  custom: boolean;
}

export interface ReportRun {
  id: number;
  reportCode: string;
  title: string;
  category: string;
  parameters?: string;
  action: 'VIEW' | 'EXPORT' | 'GENERATE';
  format?: string;
  rowCount: number;
  fileName?: string;
  sizeBytes?: number;
  createdBy: string;
  createdAt: string;
  /** When a generated file may be downloaded (scheduled files). */
  availableFrom?: string;
}

const enc = encodeURIComponent;

/** Operations foundation API. */
export const opsApi = {
  home: (companyId: number) => api.get<OperationsHome>(`/ops/home${toQuery({ companyId })}`),
  invoices: (companyId: number, search: InvoiceSearch, page = 0) =>
    api.get<PageResponse<OpsInvoiceSummary>>(
      `/ops/invoices${toQuery({ companyId, ...search, page, size: 20 })}`,
    ),
  invoice: (invoiceNo: string) => api.get<Invoice360>(`/ops/invoices/${enc(invoiceNo)}`),
  accountInvoices: (arn: string) => api.get<OpsInvoice[]>(`/ops/accounts/${enc(arn)}/invoices`),
  /** Every invoice sharing the root invoice number of an invoice (DIS 3.27.2), root first. */
  family: (invoiceNo: string) => api.get<OpsInvoice[]>(`/ops/invoices/${enc(invoiceNo)}/family`),
  replay: (body: { companyId?: number; arn?: string; invoiceNo?: string }) =>
    api.post<FeedRun>('/ops/invoices/replay', body),
  disbursements: (companyId: number, status?: DisbursementStatus, page = 0) =>
    api.get<PageResponse<Disbursement>>(
      `/ops/disbursements${toQuery({ companyId, status, page, size: 20 })}`,
    ),
  acknowledge: (id: number) => api.post<Disbursement>(`/ops/disbursements/${id}/acknowledge`),
  assignDv: (id: number, dvNo: string) =>
    api.post<Disbursement>(`/ops/disbursements/${id}/dv`, { dvNo }),
  markPaid: (id: number) => api.post<Disbursement>(`/ops/disbursements/${id}/paid`),
  returnRequest: (id: number, reason: string) =>
    api.post<Disbursement>(`/ops/disbursements/${id}/return`, { reason }),
  feeds: () => api.get<Feed[]>('/ops/flow-in/feeds'),
  configureFeed: (code: string, cron: string, active: boolean) =>
    api.put<Feed>(`/ops/flow-in/feeds/${enc(code)}`, { cron, active }),
  uploadFeed: (code: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<FeedRun>(`/ops/flow-in/feeds/${enc(code)}/upload`, form);
  },
  runs: (feed?: string, page = 0) =>
    api.get<PageResponse<FeedRun>>(`/ops/flow-in/runs${toQuery({ feed, page, size: 20 })}`),
  records: (runId: number) =>
    api.get<PageResponse<FeedRecord>>(
      `/ops/flow-in/runs/${runId}/records${toQuery({ size: 200 })}`,
    ),
  handoffs: (companyId: number, status: 'OPEN' | 'CLOSED', page = 0) =>
    api.get<PageResponse<Handoff>>(
      `/ops/handoffs${toQuery({ companyId, status, page, size: 20 })}`,
    ),
  closeHandoff: (id: number, reason: string) =>
    api.post<Handoff>(`/ops/handoffs/${id}/close`, { reason }),
  extracts: (companyId: number, folder?: string) =>
    api.get<ExtractFileInfo[]>(`/ops/extracts${toQuery({ companyId, folder })}`),
  extractFile: (id: number) => api.getFile(`/ops/extracts/${id}/file`),
  preferences: () => api.get<NotificationChannels[]>('/notifications/preferences'),
  updatePreference: (code: string, inApp: boolean, email: boolean) =>
    api.put<NotificationChannels>(`/notifications/preferences/${enc(code)}`, { inApp, email }),
  reportRuns: (code?: string, page = 0) =>
    api.get<PageResponse<ReportRun>>(`/reports/runs${toQuery({ code, page, size: 20 })}`),
  reportRunFile: (id: number) => api.getFile(`/reports/runs/${id}/file`),
};
