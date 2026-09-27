import type { LedgerComponent } from '@/api/operations';

/** Types of the Cashiering API (BRD-2 CSHID.001-027, MKTID.010/013, DBMID.001). */

export type ReceiptKind = 'AR' | 'OR';
export type ReceiptStatus = 'ISSUED' | 'CANCELLED' | 'REINSTATED';
export type PaymentMode =
  | 'CASH'
  | 'CHECK'
  | 'BILLS_PAYMENT'
  | 'TRADE'
  | 'CLPC'
  | 'PDC'
  | 'DIRECT_CREDIT'
  | 'ADA'
  | 'CREDIT_TO_ACCOUNT'
  | 'NON_CASH';
export type UnappliedTab = 'UNAPPLIED' | 'MONITORING' | 'FOR_APPROVAL' | 'FOR_REVERSAL' | 'DONE';
export type PdcStatus =
  'WAREHOUSED' | 'MATURED' | 'APPLIED' | 'RETURNED' | 'REPLACED' | 'PULLED_OUT';
export type PickupStatus = 'FOR_PICKUP' | 'AR_PRINTED' | 'CANCELLED';
export type CwtPath = 'CASH' | 'CERTIFICATE';
export type Allocation = Partial<Record<LedgerComponent, number>>;

export interface ReceiptSummary {
  id: number;
  receiptNo: string;
  kind: ReceiptKind;
  receiptClass: string;
  branchId: number;
  receiptDate: string;
  payorCode?: string;
  payorName: string;
  assuredName?: string;
  currency: string;
  amount: number;
  appliedAmount: number;
  unappliedAmount: number;
  mode: PaymentMode;
  source: string;
  status: ReceiptStatus;
  printedCount: number;
}

export interface ReceiptLine {
  invoiceNo?: string;
  insurerCode?: string;
  gross: number;
  vat: number;
  wtax: number;
  net: number;
  description?: string;
}

export interface Application {
  id: number;
  reference: string;
  invoiceNo: string;
  arn: string;
  source: string;
  amount: number;
  allocation: Allocation;
  realizedCommission: number;
  realizedVat: number;
  valueDate: string;
  status: string;
  reversalReason?: string;
  journalBatchNo?: string;
}

export interface ReceiptAction {
  id: number;
  receiptId: number;
  transactionNo: string;
  action: 'CANCEL' | 'REINSTATE_FULL' | 'REINSTATE_PARTIAL';
  reasonCode: string;
  reasonText?: string;
  amount?: number;
  invoiceNo?: string;
  documentNo?: string;
  payorName?: string;
  accountOfficer?: string;
  unitHead?: string;
  teamLeader?: string;
  stage: string;
  requestedBy: string;
  requestedAt: string;
  approvedBy?: string;
  approvedAt?: string;
  journalBatchNo?: string;
}

export interface ReceiptDetail {
  summary: ReceiptSummary;
  bookRate: number;
  baseAmount: number;
  gross?: number;
  vat?: number;
  wtax?: number;
  checkNo?: string;
  checkBank?: string;
  checkDate?: string;
  certificateRef?: string;
  sourceModule?: string;
  sourceRef?: string;
  reinstatedAmount?: number;
  journalBatchNo?: string;
  remarks?: string;
  salesUnit?: string;
  lastPrintedAt?: string;
  createdBy: string;
  createdAt: string;
  lines: ReceiptLine[];
  applications: Application[];
  actions: ReceiptAction[];
}

export interface ReceiptCriteria {
  companyId: number;
  receiptNo?: string;
  clientCode?: string;
  invoiceNo?: string;
  payor?: string;
  assured?: string;
  amount?: string;
  from?: string;
  to?: string;
  policyNo?: string;
  insurer?: string;
  kind?: ReceiptKind | '';
  status?: ReceiptStatus | '';
}

export interface ReinstateBody {
  full: boolean;
  amount?: number;
  reasonCode: string;
  reasonText?: string;
  invoiceNo?: string;
  documentNo?: string;
  payorName?: string;
  accountOfficer?: string;
  unitHead?: string;
  teamLeader?: string;
}

export interface OrBody {
  companyId: number;
  orType: string;
  receiptDate: string;
  payorCode?: string;
  payorName: string;
  currency: string;
  mode: PaymentMode;
  checkNo?: string;
  checkBank?: string;
  certificateRef?: string;
  remarks?: string;
  lines: { invoiceNo?: string; gross: number; vat: number; wtax: number; description?: string }[];
}

export interface InvoicePreview {
  invoiceNo: string;
  arn: string;
  assuredName?: string;
  currency: string;
  cwt: boolean;
  receivable: boolean;
  outstanding: number;
  balances: Allocation;
  allocation: Allocation;
  applied: number;
}

export interface PaymentPreview {
  match: 'BOOKED' | 'CANCELLED' | 'PREBOOKED' | 'NONE';
  reference?: string;
  prebookedArn?: string;
  clientCode?: string;
  invoices: InvoicePreview[];
  cwtWithheld: number;
  excess: number;
  currency: string;
  bookRate: number;
  cwtPercent: number;
}

export interface ReceivePaymentBody {
  companyId: number;
  branchId: number;
  references: string[];
  payorCode?: string;
  payorName: string;
  assuredName?: string;
  currency: string;
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  checkNo?: string;
  checkBank?: string;
  arClass?: string;
}

export interface Payment {
  id: number;
  paymentNo: string;
  channel: string;
  batchRef?: string;
  rowNo?: number;
  reference?: string;
  otherRefs?: string;
  payorName: string;
  amount: number;
  currency: string;
  valueDate: string;
  matchCategory: string;
  matchedRef?: string;
  appliedAmount: number;
  unappliedAmount: number;
  receiptId?: number;
  message?: string;
}

export interface IntakeResult {
  payment: Payment;
  receiptId: number;
  receiptNo: string;
  applications: Application[];
  unappliedId?: number;
  unappliedRef?: string;
  prebookedId?: number;
}

export interface Prebooked {
  id: number;
  arn: string;
  reference: string;
  amount: number;
  currency: string;
  firstSeen: string;
  ageDays: number;
  rematchCount: number;
  lastRematch?: string;
  status: string;
  receiptId: number;
  paymentId: number;
  remarks?: string;
}
