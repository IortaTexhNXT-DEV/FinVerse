import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type PayerType = 'POLICYHOLDER' | 'INTERMEDIARY' | 'REINSURER' | 'OTHER';
export type ReceiptMode = 'CASH' | 'CHEQUE' | 'BANK_TRANSFER' | 'CARD' | 'PDC';
export type ReceiptStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'BOUNCED';
export type DepositStatus = 'NOT_REQUIRED' | 'UNDEPOSITED' | 'IN_SLIP' | 'DEPOSITED';
export type AllocationMethod = 'MANUAL' | 'FIFO' | 'NONE';
export type PdcStatus =
  'ON_HAND' | 'DUE' | 'DEPOSITED' | 'CLEARED' | 'BOUNCED' | 'RETURNED' | 'REPLACED';

export const PAYER_PARTY_TYPES: Record<PayerType, string[]> = {
  POLICYHOLDER: ['INDIVIDUAL_CLIENT', 'CORPORATE_CLIENT'],
  INTERMEDIARY: ['AGENT', 'BROKER'],
  REINSURER: ['REINSURER', 'RI_BROKER'],
  OTHER: [],
};

export interface BankAccount {
  id: number;
  code: string;
  name: string;
  currency: string;
}

export interface PartyOption {
  id: number;
  code: string;
  name: string;
  partyType: string;
  defaultCurrency: string;
}

export interface OpenItem {
  id: number;
  partyCode: string;
  direction: 'DEBIT' | 'CREDIT';
  documentType: string;
  documentNo: string;
  documentDate: string;
  dueDate: string;
  currency: string;
  amount: number;
  settledAmount: number;
  outstanding: number;
  status: string;
  narration?: string;
}

export interface AllocationInput {
  debitItemId: number;
  amount: number;
}

export interface ReceiptInput {
  companyId: number;
  branchId: number;
  receiptDate: string;
  payerType: PayerType;
  partyCode?: string;
  payerName?: string;
  department?: string;
  mode: ReceiptMode;
  instrumentNo?: string;
  instrumentDate?: string;
  draweeBank?: string;
  currency: string;
  amount: number;
  bankAccountCode: string;
  incomeAccountCode?: string;
  allocationMethod: AllocationMethod;
  narration?: string;
  allocations: AllocationInput[];
}

export interface ReceiptSummary {
  id: number;
  receiptNo: string;
  receiptDate: string;
  branchId: number;
  payerType: PayerType;
  partyCode?: string;
  payerName: string;
  mode: ReceiptMode;
  instrumentNo?: string;
  instrumentDate?: string;
  currency: string;
  amount: number;
  appliedAmount: number;
  unappliedAmount: number;
  bankAccountCode: string;
  status: ReceiptStatus;
  depositStatus: DepositStatus;
  depositedOn?: string;
  createdBy: string;
  approvedBy?: string;
}

export interface Receipt {
  summary: ReceiptSummary;
  department?: string;
  draweeBank?: string;
  exchangeRate: number;
  baseAmount: number;
  incomeAccountCode?: string;
  allocationMethod: AllocationMethod;
  narration?: string;
  approvedAt?: string;
  journalBatchNo?: string;
  reversalDate?: string;
  reversalReason?: string;
  reversedBy?: string;
  depositSlipId?: number;
  pdcId?: number;
  allocations: {
    id: number;
    debitItemId: number;
    documentNo: string;
    amount: number;
    matched: boolean;
    appliedOn?: string;
  }[];
}

export interface ReceiptFilters {
  companyId: number;
  status?: string;
  partyCode?: string;
  mode?: string;
  from?: string;
  to?: string;
  receiptNo?: string;
  page?: number;
  size?: number;
}

export interface DepositSlip {
  id: number;
  slipNo: string;
  slipDate: string;
  bankAccountCode: string;
  currency: string;
  totalAmount: number;
  receiptCount: number;
  status: 'PREPARED' | 'DEPOSITED' | 'CANCELLED';
  depositedOn?: string;
  createdBy: string;
  receipts: ReceiptSummary[];
}

export interface PdcInput {
  companyId: number;
  branchId: number;
  receivedDate: string;
  partyCode: string;
  department?: string;
  chequeNo: string;
  chequeDate: string;
  draweeBank: string;
  currency: string;
  amount: number;
  bankAccountCode: string;
  debitItemId?: number;
  narration?: string;
}

export interface Pdc extends Omit<PdcInput, 'companyId'> {
  id: number;
  pdcNo: string;
  payerName: string;
  baseAmount: number;
  status: PdcStatus;
  statusDate: string;
  receiptId?: number;
  replacedById?: number;
  history: {
    fromStatus?: PdcStatus;
    toStatus: PdcStatus;
    eventDate: string;
    remarks?: string;
    receiptNo?: string;
    createdBy: string;
  }[];
}

export interface BankStatement {
  id: number;
  bankAccountCode: string;
  statementRef: string;
  periodFrom: string;
  periodTo: string;
  openingBalance: number;
  closingBalance: number;
  totalDebit: number;
  totalCredit: number;
  lineCount: number;
  fileName?: string;
  createdBy: string;
}

export interface StatementLine {
  id: number;
  statementId: number;
  lineNo: number;
  valueDate: string;
  description?: string;
  reference?: string;
  debit: number;
  credit: number;
  balance: number;
  matchId?: number;
}

export interface BookEntry {
  id: number;
  valueDate: string;
  batchNo: string;
  journalType: string;
  reference?: string;
  narration?: string;
  currency: string;
  debit: number;
  credit: number;
}

export interface Workbench {
  bank: BankAccount;
  bookEntries: BookEntry[];
  bankLines: StatementLine[];
}

export interface BrsFigures {
  bookBalance: number;
  bookDebitsNotInBank: number;
  bookCreditsNotInBank: number;
  bankDebitsNotInBook: number;
  bankCreditsNotInBook: number;
  computedBankBalance: number;
  statementBalance: number;
  difference: number;
}

export interface Reconciliation extends BrsFigures {
  id: number;
  bankAccountCode: string;
  asOfDate: string;
  status: 'IN_PROGRESS' | 'FINALIZED';
  finalizedBy?: string;
}

export interface Brs {
  bank: BankAccount;
  asOf: string;
  figures: BrsFigures;
  hasStatement: boolean;
  bookDebits: BookEntry[];
  bookCredits: BookEntry[];
  bankDebits: StatementLine[];
  bankCredits: StatementLine[];
  reconciliation?: Reconciliation;
}

export interface Match {
  id: number;
  method: 'AUTO' | 'MANUAL';
  matchDate: string;
  amount: number;
  createdBy: string;
}

export interface StatementLineItem {
  itemId: number;
  documentDate: string;
  reference?: string;
  transactionCode: string;
  chequeNo?: string;
  chequeDate?: string;
  currency: string;
  debit: number;
  credit: number;
  original: number;
  balance: number;
}

export interface PartyStatement {
  partyCode: string;
  partyName: string;
  matched: StatementLineItem[];
  unmatched: StatementLineItem[];
  matchedNet: number;
  unmatchedNet: number;
}

const R = '/receivables';

function partyQuery(companyId: number, types: string[]): string {
  const typeParams = types.map((t) => `&types=${encodeURIComponent(t)}`).join('');
  return `/parties${toQuery({ companyId })}${typeParams}`;
}

export const receivablesApi = {
  bankAccounts: (companyId: number) =>
    api.get<BankAccount[]>(`${R}/bank-accounts${toQuery({ companyId })}`),
  parties: (companyId: number, types: string[]) =>
    api.get<PartyOption[]>(partyQuery(companyId, types)),
  openItems: (companyId: number, partyCode: string, currency?: string) =>
    api.get<OpenItem[]>(`${R}/open-items${toQuery({ companyId, partyCode, currency })}`),
  partyStatement: (
    companyId: number,
    partyCode: string,
    from: string,
    to: string,
    foreign: boolean,
  ) =>
    api.get<PartyStatement[]>(
      `${R}/party-statement${toQuery({ companyId, partyCode, from, to, foreign })}`,
    ),

  receipts: (f: ReceiptFilters) =>
    api.get<PageResponse<ReceiptSummary>>(`${R}/receipts${toQuery({ ...f })}`),
  receipt: (id: number) => api.get<Receipt>(`${R}/receipts/${id}`),
  createReceipt: (body: ReceiptInput) => api.post<Receipt>(`${R}/receipts`, body),
  approveReceipt: (id: number) => api.post<Receipt>(`${R}/receipts/${id}/approve`),
  rejectReceipt: (id: number, date: string, reason: string) =>
    api.post<Receipt>(`${R}/receipts/${id}/reject`, { date, reason }),
  cancelReceipt: (id: number, date: string, reason: string) =>
    api.post<Receipt>(`${R}/receipts/${id}/cancel`, { date, reason }),
  bounceReceipt: (id: number, date: string, reason: string) =>
    api.post<Receipt>(`${R}/receipts/${id}/bounce`, { date, reason }),
  applyReceipt: (
    id: number,
    date: string,
    method: AllocationMethod,
    allocations: AllocationInput[],
  ) => api.post<Receipt>(`${R}/receipts/${id}/apply`, { date, method, allocations }),

  undeposited: (companyId: number, bankAccountCode?: string) =>
    api.get<ReceiptSummary[]>(
      `${R}/deposits/undeposited${toQuery({ companyId, bankAccountCode })}`,
    ),
  slips: (companyId: number) =>
    api.get<DepositSlip[]>(`${R}/deposits/slips${toQuery({ companyId })}`),
  createSlip: (body: {
    companyId: number;
    branchId: number;
    bankAccountCode: string;
    slipDate: string;
    receiptIds: number[];
  }) => api.post<DepositSlip>(`${R}/deposits/slips`, body),
  confirmSlip: (id: number, date: string) =>
    api.post<DepositSlip>(`${R}/deposits/slips/${id}/confirm`, { date }),
  cancelSlip: (id: number) => api.post<DepositSlip>(`${R}/deposits/slips/${id}/cancel`),

  pdcs: (companyId: number, status?: string) =>
    api.get<Pdc[]>(`${R}/pdcs${toQuery({ companyId, status })}`),
  pdc: (id: number) => api.get<Pdc>(`${R}/pdcs/${id}`),
  registerPdc: (body: PdcInput) => api.post<Pdc>(`${R}/pdcs`, body),
  markDue: (companyId: number, asOf: string) =>
    api.post<Pdc[]>(`${R}/pdcs/mark-due${toQuery({ companyId, asOf })}`),
  depositPdc: (id: number, date: string) => api.post<Receipt>(`${R}/pdcs/${id}/deposit`, { date }),
  clearPdc: (id: number, date: string) => api.post<Pdc>(`${R}/pdcs/${id}/clear`, { date }),
  bouncePdc: (id: number, date: string, reason: string) =>
    api.post<Pdc>(`${R}/pdcs/${id}/bounce`, { date, reason }),
  returnPdc: (id: number, date: string, reason: string) =>
    api.post<Pdc>(`${R}/pdcs/${id}/return`, { date, reason }),

  importStatement: (body: {
    companyId: number;
    bankAccountCode: string;
    statementRef?: string;
    fileName?: string;
    content: string;
    openingBalance?: number;
  }) => api.post<BankStatement>(`${R}/bank-rec/statements`, body),
  statements: (companyId: number, bankAccountCode?: string) =>
    api.get<BankStatement[]>(`${R}/bank-rec/statements${toQuery({ companyId, bankAccountCode })}`),
  statementLines: (id: number) => api.get<StatementLine[]>(`${R}/bank-rec/statements/${id}/lines`),
  workbench: (companyId: number, bankAccountCode: string, asOf: string) =>
    api.get<Workbench>(`${R}/bank-rec/workbench${toQuery({ companyId, bankAccountCode, asOf })}`),
  autoMatch: (companyId: number, bankAccountCode: string, asOf: string, dateWindowDays: number) =>
    api.post<Match[]>(`${R}/bank-rec/auto-match`, {
      companyId,
      bankAccountCode,
      asOf,
      dateWindowDays,
    }),
  manualMatch: (
    companyId: number,
    bankAccountCode: string,
    ledgerEntryIds: number[],
    statementLineIds: number[],
  ) =>
    api.post<Match>(`${R}/bank-rec/matches`, {
      companyId,
      bankAccountCode,
      ledgerEntryIds,
      statementLineIds,
    }),
  matches: (companyId: number, bankAccountCode: string) =>
    api.get<Match[]>(`${R}/bank-rec/matches${toQuery({ companyId, bankAccountCode })}`),
  unmatch: (id: number) => api.post<Match>(`${R}/bank-rec/matches/${id}/unmatch`),
  brs: (companyId: number, bankAccountCode: string, asOf: string) =>
    api.get<Brs>(`${R}/bank-rec/brs${toQuery({ companyId, bankAccountCode, asOf })}`),
  reconciliations: (companyId: number) =>
    api.get<Reconciliation[]>(`${R}/bank-rec/reconciliations${toQuery({ companyId })}`),
  saveReconciliation: (companyId: number, bankAccountCode: string, asOf: string) =>
    api.post<Reconciliation>(`${R}/bank-rec/reconciliations`, { companyId, bankAccountCode, asOf }),
  finalizeReconciliation: (id: number) =>
    api.post<Reconciliation>(`${R}/bank-rec/reconciliations/${id}/finalize`),
};
