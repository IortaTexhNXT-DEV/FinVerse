import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Booking (booking module): workbench, booked invoices, endorsements, service invoices, setup. */

export const ACCOUNT_ENTITY = 'Account';
export const SERVICE_INVOICE_ENTITY = 'ServiceInvoice';

export type WorkbenchTab = 'READY' | 'QUEUED' | 'BOOKED' | 'FAILED';
export type InvoiceKind = 'BOOKING' | 'ENDORSEMENT_PLUS' | 'ENDORSEMENT_MINUS' | 'CANCELLATION';
export type InvoiceStatus = 'SCHEDULED' | 'BOOKED' | 'CANCELLED';
export type EndorsementType = 'POSITIVE' | 'NEGATIVE' | 'NON_FINANCIAL' | 'CANCELLATION';
export type CancellationKind = 'FLAT' | 'FLAT_RETAIN_DST' | 'PARTIAL';
export type PeriodBasis = 'PRO_RATA' | 'SHORT_PERIOD';
export type SiKind = 'INVOICE' | 'CREDIT';
export type SiRecipient = 'INSURER' | 'INTERNAL';
export type SiTrigger = 'ON_BOOKING' | 'ON_ENDORSEMENT' | 'MANUAL' | 'ON_INCENTIVE';
export type BalanceSide = 'DEBIT' | 'CREDIT';

export interface WorkbenchCounts {
  readyToBook: number;
  queued: number;
  bookedToday: number;
  failed: number;
}

export interface WorkbenchRow {
  id: number;
  accountId?: number;
  invoiceId?: number;
  clientName?: string;
  clientCode?: string;
  arn: string;
  invoiceNo?: string;
  status: string;
  lineCode?: string;
  productCode?: string;
  department?: string;
  bookingDate?: string;
  message?: string;
  source?: string;
  costCenter?: string;
}

export interface Premium {
  basic: number;
  dst: number;
  premiumTaxVat: number;
  lgt: number;
  fst: number;
  other: number;
  total: number;
}

export interface Commission {
  rate: number;
  commission: number;
  vatOnCommission: number;
  wtaxRate: number;
  wtaxAmount: number;
  net: number;
}

export interface InvoiceFacts {
  clientId: number;
  clientCode: string;
  clientName: string;
  insurerCode: string;
  riskCode: string;
  lineCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  accountOfficer?: string;
  salesUnit?: string;
  department?: string;
  costCenter: string;
}

export interface InvoiceFlags {
  directPayment: boolean;
  cwt2Percent: boolean;
  incentiveEligible: boolean;
  businessType: 'NEW_BUSINESS' | 'RENEWAL';
}

export interface Share {
  insurerCode: string;
  sharePct: number;
}

export interface InvoiceDraft {
  arn: string;
  transactionNo: string;
  kind: InvoiceKind;
  policyYear: number;
  policyNo?: string;
  facts: InvoiceFacts;
  currency: string;
  inceptionDate: string;
  expiryDate: string;
  premium: Premium;
  commission: Commission;
  flags: InvoiceFlags;
  shares: Share[];
}

export interface BookedInvoice extends InvoiceDraft {
  id: number;
  invoiceNo?: string;
  accountId: number;
  status: InvoiceStatus;
  endorsementNo?: string;
  parentInvoiceNo?: string;
  /** Root of the invoice family (DIS 3.27.2). */
  rootInvoiceNo?: string;
  bookingDate?: string;
  source: string;
  serviceInvoiceNo?: string;
  bookedBy?: string;
  bookedAt?: string;
  journalBatches: string[];
  /** Insurer billing number (BRID-020). */
  insurerBillingNo?: string;
}

export interface PreviewLine {
  insurerCode: string;
  accountCode: string;
  accountName: string;
  side: BalanceSide;
  amount: number;
  partyCode?: string;
  narration?: string;
}

export interface BookingPreview {
  bookingDate: string;
  invoices: InvoiceDraft[];
  journal: PreviewLine[];
}

export interface PostedLine {
  batchId: number;
  batchNo: string;
  accountCode: string;
  accountName: string;
  side: BalanceSide;
  amount: number;
  partyCode?: string;
  narration?: string;
}

export interface OpenItemLine {
  id: number;
  role: 'CLIENT_PREMIUM' | 'INSURER_DTIP' | 'INSURER_COMMISSION';
  partyCode: string;
  direction: 'DEBIT' | 'CREDIT';
  documentType: string;
  amount: number;
  settledAmount: number;
  outstanding: number;
  status: string;
  dueDate: string;
  journalBatchNo?: string;
}

export interface BookRequest {
  arn: string;
  bookingDate?: string;
  costCenter?: string;
  cwt2Percent?: boolean;
  shares?: Share[];
  /** Insurer billing number (BRID-020): required for the lines of BOOKING_BILLING_NO_LINES. */
  insurerBillingNo?: string;
}

export interface EnqueueResult {
  arn: string;
  queued: boolean;
  message?: string;
}

export interface QueueEntry {
  id: number;
  arn: string;
  status: string;
  source: string;
  bookingDate?: string;
  costCenter?: string;
  lastError?: string;
  invoiceNo?: string;
  batchRunNo?: string;
}

export interface BatchRow {
  arn: string;
  outcome: 'BOOKED' | 'FAILED';
  invoiceNo?: string;
  message?: string;
}

export interface BatchRun {
  id: number;
  runNo: string;
  trigger: 'MANUAL' | 'SCHEDULED' | 'BOOK_NOW' | 'UPLOAD';
  businessDate: string;
  startedBy: string;
  startedAt: string;
  finishedAt?: string;
  bookedCount: number;
  failedCount: number;
  rows: BatchRow[];
}

export interface Endorsement {
  id: number;
  endorsementNo: string;
  arn: string;
  accountId: number;
  type: EndorsementType;
  cancellationKind?: CancellationKind;
  periodBasis?: string;
  effectiveDate: string;
  policyYear: number;
  sumInsuredChange?: number;
  ratePercent?: number;
  description: string;
  reasonCode?: string;
  invoiceNo?: string;
  createdBy: string;
  createdAt: string;
}

export interface EndorsementRequest {
  arn: string;
  type: EndorsementType;
  cancellationKind?: CancellationKind;
  effectiveDate: string;
  basis?: PeriodBasis;
  sumInsuredChange?: number;
  ratePercent?: number;
  description: string;
  reasonCode?: string;
  bookingDate?: string;
}

export interface EndorsementPreview {
  policyYear: number;
  invoice?: InvoiceDraft;
  journal: PreviewLine[];
}

export interface EndorsementResult {
  endorsementNo: string;
  invoiceNo?: string;
  journalBatches: string[];
}

export interface ServiceInvoice {
  id: number;
  siNo: string;
  typeCode: string;
  kind: SiKind;
  invoiceNo?: string;
  arn?: string;
  recipientCode: string;
  recipientName: string;
  recipientEmail?: string;
  issueDate: string;
  currency: string;
  commission: number;
  vatOnCommission: number;
  wtaxAmount: number;
  netAmount: number;
  templateCode: string;
  templateVersion: number;
  ownerUsername?: string;
  ownerPermission?: string;
  dispatchStatus: 'NOT_SENT' | 'QUEUED' | 'SENT' | 'FAILED';
  dispatchError?: string;
  creditOf?: string;
  remarks?: string;
  createdBy: string;
  createdAt: string;
}

export interface CreditRequest {
  commission?: number;
  vatOnCommission?: number;
  reason: string;
}

export interface AutoBookRule {
  id?: number;
  productCode?: string;
  marketSegment?: string;
  enabled: boolean;
  description: string;
}

export interface IncentiveRule {
  id?: number;
  productCode?: string;
  marketSegment?: string;
  sourceChannel?: string;
  periodFrom: string;
  periodTo?: string;
  active: boolean;
  description: string;
}

export interface ServiceInvoiceType {
  id?: number;
  code: string;
  name: string;
  recipient: SiRecipient;
  trigger: SiTrigger;
  ownerPermission?: string;
  ownerUsername?: string;
  templateCode: string;
  active: boolean;
}

const BASE = '/booking';

export const bookingApi = {
  counts: (companyId: number) =>
    api.get<WorkbenchCounts>(`${BASE}/workbench/counts${toQuery({ companyId })}`),
  workbench: (companyId: number, tab: WorkbenchTab, q: string, line: string, page: number) =>
    api.get<PageResponse<WorkbenchRow>>(
      `${BASE}/workbench${toQuery({ companyId, tab, q, line, page, size: 20 })}`,
    ),
  preview: (request: BookRequest) => api.post<BookingPreview>(`${BASE}/preview`, request),
  book: (request: BookRequest) =>
    api.post<{ id: number; invoiceNo: string }>(`${BASE}/book`, request),
  enqueue: (companyId: number, arns: string[]) =>
    api.post<EnqueueResult[]>(`${BASE}/queue`, { companyId, arns }),
  editQueued: (id: number, bookingDate?: string, costCenter?: string) =>
    api.put<QueueEntry>(`${BASE}/queue/${id}`, { bookingDate, costCenter }),
  removeQueued: (id: number) => api.post<QueueEntry>(`${BASE}/queue/${id}/remove`),
  confirmBatch: (companyId: number, entryIds: number[], businessDate?: string) =>
    api.post<BatchRun>(`${BASE}/batch/confirm`, { companyId, entryIds, businessDate }),
  cancelBatch: (companyId: number) =>
    api.post<{ removed: number }>(`${BASE}/batch/cancel${toQuery({ companyId })}`),
  bookNow: (companyId: number, arns: string[], bookingDate?: string) =>
    api.post<BatchRun>(`${BASE}/book-now`, { companyId, arns, bookingDate }),
  runs: (companyId: number, page: number) =>
    api.get<PageResponse<BatchRun>>(`${BASE}/batch-runs${toQuery({ companyId, page })}`),
  run: (runNo: string) => api.get<BatchRun>(`${BASE}/batch-runs/${encodeURIComponent(runNo)}`),
  invoice: (id: number) => api.get<BookedInvoice>(`${BASE}/invoices/${id}`),
  invoiceByNo: (invoiceNo: string) =>
    api.get<BookedInvoice>(`${BASE}/invoices/by-no/${encodeURIComponent(invoiceNo)}`),
  journal: (id: number) => api.get<PostedLine[]>(`${BASE}/invoices/${id}/journal`),
  openItems: (id: number) => api.get<OpenItemLine[]>(`${BASE}/invoices/${id}/open-items`),
  schedule: (arn: string) =>
    api.get<BookedInvoice[]>(`${BASE}/accounts/${encodeURIComponent(arn)}/invoices`),
  accountEndorsements: (arn: string) =>
    api.get<Endorsement[]>(`${BASE}/accounts/${encodeURIComponent(arn)}/endorsements`),
  endorsements: (companyId: number, page: number) =>
    api.get<PageResponse<Endorsement>>(`${BASE}/endorsements${toQuery({ companyId, page })}`),
  previewEndorsement: (request: EndorsementRequest) =>
    api.post<EndorsementPreview>(`${BASE}/endorsements/preview`, request),
  postEndorsement: (request: EndorsementRequest) =>
    api.post<EndorsementResult>(`${BASE}/endorsements`, request),
  serviceInvoices: (companyId: number, q: string, kind: SiKind | undefined, page: number) =>
    api.get<PageResponse<ServiceInvoice>>(
      `${BASE}/service-invoices${toQuery({ companyId, q, kind, page })}`,
    ),
  serviceInvoice: (id: number) => api.get<ServiceInvoice>(`${BASE}/service-invoices/${id}`),
  serviceInvoicesOf: (invoiceNo: string) =>
    api.get<ServiceInvoice[]>(
      `${BASE}/service-invoices/by-invoice/${encodeURIComponent(invoiceNo)}`,
    ),
  serviceInvoicePdf: (id: number) => api.getFile(`${BASE}/service-invoices/${id}/pdf`),
  resend: (id: number) => api.post<ServiceInvoice>(`${BASE}/service-invoices/${id}/resend`),
  credit: (id: number, request: CreditRequest) =>
    api.post<ServiceInvoice>(`${BASE}/service-invoices/${id}/credit`, request),
  autoBookRules: (companyId: number) =>
    api.get<AutoBookRule[]>(`${BASE}/setup/auto-book-rules${toQuery({ companyId })}`),
  saveAutoBookRule: (companyId: number, rule: AutoBookRule) =>
    rule.id === undefined
      ? api.post<AutoBookRule>(`${BASE}/setup/auto-book-rules${toQuery({ companyId })}`, rule)
      : api.put<AutoBookRule>(`${BASE}/setup/auto-book-rules/${rule.id}`, rule),
  incentiveRules: (companyId: number) =>
    api.get<IncentiveRule[]>(`${BASE}/setup/incentive-rules${toQuery({ companyId })}`),
  saveIncentiveRule: (companyId: number, rule: IncentiveRule) =>
    rule.id === undefined
      ? api.post<IncentiveRule>(`${BASE}/setup/incentive-rules${toQuery({ companyId })}`, rule)
      : api.put<IncentiveRule>(`${BASE}/setup/incentive-rules/${rule.id}`, rule),
  serviceInvoiceTypes: () => api.get<ServiceInvoiceType[]>(`${BASE}/setup/service-invoice-types`),
  saveServiceInvoiceType: (type: ServiceInvoiceType) =>
    type.id === undefined
      ? api.post<ServiceInvoiceType>(`${BASE}/setup/service-invoice-types`, type)
      : api.put<ServiceInvoiceType>(`${BASE}/setup/service-invoice-types/${type.id}`, type),
};
