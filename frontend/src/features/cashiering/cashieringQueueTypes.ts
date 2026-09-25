import type { CwtPath, PdcStatus, PickupStatus, ReceiptKind } from './cashieringTypes';

/** Types of the Cashiering queues and setup (unapplied, checks, printing, BIR 2307, series). */

export interface Disposition {
  id: number;
  dispositionType: string;
  action: string;
  amount: number;
  targetInvoiceNo?: string;
  targetClientCode?: string;
  targetUnit?: string;
  payeeName?: string;
  remarks?: string;
  status: string;
  requestedBy: string;
  requestedAt: string;
  approvedBy?: string;
  approvedAt?: string;
  completedAt?: string;
  disbursementRequestNo?: string;
  journalBatchNo?: string;
  reversalReason?: string;
}

export interface UnappliedItem {
  id: number;
  reference: string;
  origin: string;
  receiptId?: number;
  paymentId?: number;
  invoiceNo?: string;
  clientCode?: string;
  payorName?: string;
  salesUnit?: string;
  currency: string;
  amount: number;
  balance: number;
  stage: string;
  dispositionHint?: string;
  sourceModule?: string;
  sourceRef?: string;
  remarks?: string;
  createdAt: string;
  current?: Disposition;
}

export interface DispositionType {
  code: string;
  action: string;
  requiresApproval: boolean;
  description: string;
}

export interface DispositionBody {
  dispositionType: string;
  amount: number;
  targetInvoiceNo?: string;
  targetClientCode?: string;
  targetUnit?: string;
  payeeName?: string;
  remarks?: string;
}

export interface BulkResult {
  done: string[];
  failures: string[];
}

export interface PdcItem {
  id: number;
  warehouseNo: string;
  clientCode?: string;
  payorName: string;
  reference: string;
  checkNo: string;
  bankCode: string;
  checkBranch?: string;
  maturityDate: string;
  amount: number;
  currency: string;
  segment?: string;
  status: PdcStatus;
  statusReason?: string;
  receiptNo?: string;
}

export interface PdcBody {
  companyId: number;
  branchId: number;
  clientCode?: string;
  payorName: string;
  reference: string;
  checkNo: string;
  bankCode: string;
  checkBranch?: string;
  maturityDate: string;
  amount: number;
  segment?: string;
}

export interface Pickup {
  id: number;
  collectionRef: string;
  reference: string;
  clientCode?: string;
  payorName: string;
  pickupDate: string;
  requestedAt: string;
  requestor: string;
  amount: number;
  currency: string;
  checkNo?: string;
  status: PickupStatus;
  receiptNo?: string;
  printedAt?: string;
}

export interface PrintLine {
  receiptId: number;
  receiptNo: string;
  status: string;
  message?: string;
}

export interface PrintBatch {
  id: number;
  batchNo: string;
  criteria?: string;
  requestedCount: number;
  printedCount: number;
  failedCount: number;
  status: string;
  fileName?: string;
  createdBy: string;
  createdAt: string;
  lines: PrintLine[];
}

export interface CwtTag {
  id: number;
  reference: string;
  invoiceNo: string;
  arn: string;
  clientCode?: string;
  insurerCode?: string;
  amount: number;
  path: CwtPath;
  certificateNo?: string;
  periodFrom?: string;
  periodTo?: string;
  cwtCopyReceived: boolean;
  remitted: boolean;
  stage: string;
  batchId?: number;
  receiptNo?: string;
  reclassJournalNo?: string;
  offsetJournalNo?: string;
  remarks?: string;
  taggedBy: string;
  taggedAt: string;
}

export interface CwtBatch {
  id: number;
  batchNo: string;
  insurerCode: string;
  tagCount: number;
  totalAmount: number;
  status: string;
  disbursementRequestNo?: string;
  routedAt?: string;
  releasedAt?: string;
  createdBy: string;
  createdAt: string;
}

export interface CwtExpected {
  invoiceNo: string;
  arn: string;
  assuredName?: string;
  insurerCode?: string;
  cwt: boolean;
  expected: number;
  remittanceStatus: string;
}

export interface CwtTagBody {
  companyId: number;
  invoiceNo: string;
  amount?: number;
  path: CwtPath;
  certificateNo?: string;
  periodFrom?: string;
  periodTo?: string;
  remarks?: string;
}

export interface Series {
  id: number;
  branchId: number;
  kind: ReceiptKind;
  atpNo?: string;
  prefix: string;
  fromNo: number;
  toNo: number;
  nextNo: number;
  remaining: number;
  warnAt: number;
  low: boolean;
  recordStatus: string;
  maker?: string;
  authorizedBy?: string;
}

export interface SeriesBody {
  companyId: number;
  branchId: number;
  kind: ReceiptKind;
  prefix: string;
  fromNo: number;
  toNo: number;
  atpNo?: string;
  warnAt: number;
}

export interface Layout {
  handlerCode: string;
  kind: string;
  delimiter?: string;
  fields?: string;
  description?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface MinimalBalanceRule {
  kind: string;
  maxAmount: number;
  excludeCwt: boolean;
  excludeDst: boolean;
  excludeWholePremium: boolean;
  action: string;
  active: boolean;
  description?: string;
}

export interface CommissionLine {
  id: number;
  jobNo: string;
  rowNo: number;
  insurerCode: string;
  payeeName: string;
  certificateRef?: string;
  paymentRef?: string;
  invoiceNo?: string;
  gross: number;
  vat: number;
  wtax: number;
  paymentDate: string;
  status: string;
  receiptNo?: string;
  message?: string;
}
