/** Types of the Disbursement API (DIS 2.2-3.28): requests, vouchers, instruments, payees, EOD, funding, banks. */

export type Mode = 'CTA' | 'ATD' | 'MC_DD' | 'CREDIT_TICKET' | 'TT' | 'ONLINE_BANKING' | 'CHECK';
export type RequestStatus =
  'RECEIVED' | 'NO_PAYEE' | 'IN_VOUCHER' | 'RELEASED' | 'RETURNED' | 'CANCELLED';
export type VoucherStage =
  'IN_PROCESS' | 'FOR_REVIEW' | 'FOR_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type PostingStatus = 'NOT_POSTED' | 'POSTED' | 'FAILED' | 'REVERSED' | 'REVERSAL_FAILED';
export type InstrumentStatus =
  | 'PENDING'
  | 'PRINTED'
  | 'RELEASED'
  | 'NEGOTIATED'
  | 'STALE'
  | 'CANCELLED'
  | 'EMAILED'
  | 'DEBITED'
  | 'EXTRACTED'
  | 'CREDITED'
  | 'RECEIVED'
  | 'APPROVED';
export type PayeeStage =
  'DRAFT' | 'FOR_AUTHORIZATION' | 'ACTIVE' | 'FOR_DEACTIVATION' | 'INACTIVE' | 'FOR_REACTIVATION';
export type FundingStage =
  | 'CREATED'
  | 'FOR_VERIFICATION'
  | 'FOR_APPROVAL_1'
  | 'FOR_APPROVAL_2'
  | 'APPROVED'
  | 'DECLINED'
  | 'CANCELLED';
export type Side = 'DEBIT' | 'CREDIT';

export interface Summary {
  requests: number;
  noPayee: number;
  inProcess: number;
  forReview: number;
  forApproval: number;
  approved: number;
  closed: number;
  unregularized: number;
  payeesPending: number;
  payeeRequests: number;
  fundingPending: number;
}

export interface PaymentRequest {
  id: number;
  requestNo: string;
  source: 'GATEWAY' | 'UPLOAD' | 'ENCODED' | 'PAYREQUEST';
  sourceModule: string;
  sourceRef: string;
  rfpNo?: string;
  disbursementType: string;
  payeeClass?: string;
  payeeCode: string;
  payeeName?: string;
  payeeId?: number;
  currency: string;
  amount: number;
  purpose?: string;
  receivedAt: string;
  rootInvoiceNo?: string;
  attachmentRefs: string[];
  status: RequestStatus;
  statusReason?: string;
  voucherId?: number;
  uploadJobNo?: string;
  createdBy: string;
}

export interface EncodeInput {
  companyId: number;
  disbursementType: string;
  payeeCode: string;
  payeeName?: string;
  currency: string;
  amount: number;
  purpose: string;
  rfpNo?: string;
  rootInvoiceNo?: string;
  expenseAccount?: string;
  costCenter?: string;
}

export interface VoucherSummary {
  id: number;
  dvNo: string;
  requestId: number;
  payeeCode: string;
  payeeName: string;
  disbursementType: string;
  mode?: Mode;
  currency: string;
  gross: number;
  ewt: number;
  net: number;
  stage: VoucherStage;
  postingStatus: PostingStatus;
  proformaEdited: boolean;
  autoCreated: boolean;
  rootInvoiceNo?: string;
  createdAt: string;
  approvedAt?: string;
}

export interface Line {
  side: Side;
  accountCode: string;
  partyCode?: string;
  costCenter?: string;
  businessLine?: string;
  amount: number;
  component?: string;
  origin?: 'RULE' | 'EDITED' | 'ALLOCATION';
  narration?: string;
}

export interface InstrumentEvent {
  fromStatus?: InstrumentStatus;
  toStatus: InstrumentStatus;
  source: 'USER' | 'SYSTEM' | 'UPLOAD' | 'JOB';
  note?: string;
  fileRef?: string;
  at: string;
  by: string;
}

export interface StatusEdit {
  id: number;
  fromStatus: InstrumentStatus;
  toStatus: InstrumentStatus;
  reason: string;
  stage: 'REQUESTED' | 'APPLIED' | 'REJECTED';
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string;
}

export interface Instrument {
  id: number;
  mode: Mode;
  instrumentNo?: string;
  status: InstrumentStatus;
  amount: number;
  currency: string;
  reference?: string;
  printedOn?: string;
  releasedTo?: string;
  history: InstrumentEvent[];
  edits: StatusEdit[];
  statuses: InstrumentStatus[];
}

export interface Tag {
  id: number;
  kind: 'OR_AR' | 'CWT';
  direction?: 'RECEIVED' | 'RELEASED';
  docNo?: string;
  docDate?: string;
  receivedOn?: string;
  releasedOn?: string;
  periodFrom?: string;
  periodTo?: string;
  amount?: number;
  certificateRef?: string;
  remarks?: string;
  taggedBy: string;
}

export interface Voucher {
  summary: VoucherSummary;
  request: PaymentRequest;
  bankAccountId?: number;
  bankAccount?: string;
  payeeAccountId?: number;
  payeeAccount?: string;
  purpose?: string;
  valueDate?: string;
  costCenter?: string;
  expenseAccount?: string;
  exchangeRate?: number;
  lines: Line[];
  missing: string[];
  journalNo?: string;
  cancelJournalNo?: string;
  postingError?: string;
  submittedBy?: string;
  reviewedBy?: string;
  approvedBy?: string;
  cancelReason?: string;
  cancelledBy?: string;
  instrument?: Instrument;
  tags: Tag[];
}

export interface TermsInput {
  mode: Mode;
  bankAccountId: number;
  payeeAccountId?: number;
  ewt: number;
  purpose: string;
  valueDate: string;
  costCenter?: string;
  expenseAccount?: string;
}

export interface AllocationRow {
  accountCode: string;
  costCenter?: string;
  amount: number;
  narration?: string;
}

export interface ItemResult {
  id: number;
  dvNo?: string;
  ok: boolean;
  message: string;
}

export interface PayeeAccount {
  id: number;
  bankName: string;
  bankBranch?: string;
  accountNo: string;
  accountName: string;
  currency: string;
  mode: Mode;
  primary: boolean;
  active: boolean;
}

export interface PayeeSummary {
  id: number;
  payeeCode: string;
  payeeClass: string;
  name: string;
  defaultMode: Mode;
  currency: string;
  source: string;
  stage: PayeeStage;
  accountNo?: string;
  used: boolean;
}

export interface Payee {
  summary: PayeeSummary;
  address?: string;
  email?: string;
  tin?: string;
  allowedModes: Mode[];
  disbursementTypes: string[];
  defaultCostCenter?: string;
  remarks?: string;
  accounts: PayeeAccount[];
  createdBy: string;
  updatedBy?: string;
}

export interface AccountInput {
  bankName: string;
  bankBranch?: string;
  accountNo: string;
  accountName: string;
  currency: string;
  mode: Mode;
  primary: boolean;
}

export interface PayeeInput {
  companyId?: number;
  payeeCode?: string;
  payeeClass: string;
  name: string;
  address?: string;
  email?: string;
  tin?: string;
  defaultMode: Mode;
  allowedModes: Mode[];
  disbursementTypes: string[];
  currency: string;
  defaultCostCenter?: string;
  remarks?: string;
  accounts?: AccountInput[];
}

export interface PayeeRequest {
  id: number;
  source: 'RRF' | 'DISBURSEMENT' | 'NO_MATCH';
  payeeCode?: string;
  payeeName: string;
  details?: string;
  sourceRef?: string;
  status: 'OPEN' | 'DONE' | 'CANCELLED';
  payeeId?: number;
  createdAt: string;
  closedBy?: string;
}

export interface EodOutput {
  id: number;
  kind: string;
  code: string;
  fileName: string;
  itemCount: number;
}

export interface EodRun {
  id: number;
  runNo: string;
  businessDate: string;
  status: 'COMPLETED' | 'CONFIRMED';
  vouchers: number;
  checks: number;
  credits: number;
  forms: number;
  reports: number;
  emails: number;
  message?: string;
  runBy: string;
  runAt: string;
  outputs: EodOutput[];
}

export interface Funding {
  id: number;
  fundingNo: string;
  sourceBankAccountId: number;
  targetBankAccountId: number;
  amount: number;
  currency: string;
  purpose: string;
  valueDate: string;
  stage: FundingStage;
  createdBy: string;
  verifiedBy?: string;
  firstApprover?: string;
  secondApprover?: string;
  bobReference?: string;
  journalNo?: string;
}

export interface FundingInput {
  companyId?: number;
  sourceBankAccountId: number;
  targetBankAccountId: number;
  amount: number;
  currency: string;
  purpose: string;
  valueDate: string;
  bobReference?: string;
}

export interface CheckBook {
  id: number;
  firstNo: number;
  lastNo: number;
  nextNo: number;
  remaining: number;
  receivedOn: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';
  editedBy?: string;
  previousRange?: string;
}

export interface Bank {
  id: number;
  code: string;
  name: string;
  bankName: string;
  accountNo: string;
  currency: string;
  glAccountCode: string;
  recordStatus: 'PENDING_AUTHORIZATION' | 'ACTIVE' | 'INACTIVE';
  status: 'ACTIVE' | 'INACTIVE';
  requestedStatus?: 'ACTIVE' | 'INACTIVE';
  maker?: string;
  remainingLeaves: number;
  books: CheckBook[];
}
