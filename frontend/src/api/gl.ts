import { api, toQuery } from './client';
import type { PageResponse, RecordStatus } from './types';

export type AccountClass = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'INCOME' | 'EXPENSE' | 'MEMORANDUM';
export type AccountLevel = 'GROUP' | 'MAIN' | 'SUB' | 'MICRO';
export type SubLedgerType =
  'NONE' | 'POLICYHOLDER' | 'INTERMEDIARY' | 'REINSURER' | 'COINSURER' | 'BANK' | 'VENDOR';

export interface GlAccount {
  id: number;
  companyId: number;
  code: string;
  name: string;
  shortName?: string;
  accountClass: AccountClass;
  level: AccountLevel;
  parentCode?: string;
  categoryCode?: string;
  postable: boolean;
  controlAccount: boolean;
  subLedgerType: SubLedgerType;
  allowManualPosting: boolean;
  costCenterRequired: boolean;
  businessLineRequired: boolean;
  revaluationRequired: boolean;
  reconcilable: boolean;
  interBranch: boolean;
  contraAccountCode?: string;
  reportGroup?: string;
  frozen: boolean;
  freezeReason?: string;
  openedOn: string;
  closedOn?: string;
  allowedCurrencies: string[];
  allowedBranchIds: number[];
  allowedRoleCodes: string[];
  recordStatus: RecordStatus;
  createdBy: string;
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}

export type GlAccountRequest = Omit<
  GlAccount,
  | 'id'
  | 'postable'
  | 'frozen'
  | 'freezeReason'
  | 'closedOn'
  | 'recordStatus'
  | 'createdBy'
  | 'maker'
  | 'authorizedBy'
>;

export interface GlCategory {
  id: number;
  code: string;
  name: string;
  accountClass: AccountClass;
  bankCategory: boolean;
}

export type Side = 'DEBIT' | 'CREDIT';
export type JournalStatus =
  'DRAFT' | 'PENDING_APPROVAL' | 'POSTED' | 'REJECTED' | 'CANCELLED' | 'REVERSED';
export type ManualJournalType = 'MANUAL' | 'ADJUSTMENT' | 'ACCRUAL';

export interface JournalLineInput {
  accountCode: string;
  side: Side;
  amount: number;
  currency?: string;
  exchangeRate?: number;
  branchId?: number;
  costCenter?: string;
  businessLine?: string;
  partyCode?: string;
  reference?: string;
  narration?: string;
}

export interface JournalInput {
  companyId: number;
  branchId: number;
  journalType: ManualJournalType;
  valueDate: string;
  currency: string;
  narration: string;
  reference?: string;
  lines: JournalLineInput[];
}

export interface JournalLine extends JournalLineInput {
  lineNo: number;
  accountId: number;
  accountName: string;
  baseAmount: number;
  exchangeRate: number;
  branchId: number;
  currency: string;
}

export interface Journal {
  id: number;
  companyId: number;
  branchId: number;
  batchNo: string;
  journalType: string;
  status: JournalStatus;
  transactionDate: string;
  valueDate: string;
  currency: string;
  narration: string;
  reference?: string;
  sourceModule?: string;
  sourceReference?: string;
  reversalOfId?: number;
  reversedById?: number;
  totalDebit: number;
  totalCredit: number;
  createdBy: string;
  createdAt: string;
  submittedBy?: string;
  submittedAt?: string;
  authorizedBy?: string;
  authorizedAt?: string;
  rejectedBy?: string;
  rejectionReason?: string;
  postedAt?: string;
  lines: JournalLine[];
}

export interface JournalFilters {
  companyId: number;
  branchId?: number;
  status?: string;
  journalType?: string;
  fromDate?: string;
  toDate?: string;
  batchNo?: string;
  inputter?: string;
  authorizer?: string;
  page?: number;
  size?: number;
}

export interface StatementLine {
  valueDate: string;
  batchId: number;
  batchNo: string;
  journalType: string;
  narration?: string;
  reference?: string;
  currency: string;
  amountFc: number;
  debit: number;
  credit: number;
  runningBalance: number;
}

export interface AccountStatement {
  accountId: number;
  accountCode: string;
  accountName: string;
  from: string;
  to: string;
  openingBalance: number;
  totalDebit: number;
  totalCredit: number;
  closingBalance: number;
  lines: StatementLine[];
}

export const glApi = {
  accounts: (companyId: number, q?: string) =>
    api.get<GlAccount[]>(`/coa/accounts${toQuery({ companyId, q })}`),
  createAccount: (body: GlAccountRequest) => api.post<GlAccount>('/coa/accounts', body),
  updateAccount: (id: number, body: GlAccountRequest) =>
    api.put<GlAccount>(`/coa/accounts/${id}`, body),
  authorizeAccount: (id: number) => api.post<GlAccount>(`/coa/accounts/${id}/authorize`),
  freezeAccount: (id: number, reason: string) =>
    api.post<GlAccount>(`/coa/accounts/${id}/freeze`, { reason }),
  unfreezeAccount: (id: number) => api.post<GlAccount>(`/coa/accounts/${id}/unfreeze`),
  categories: () => api.get<GlCategory[]>('/coa/categories'),

  journals: (f: JournalFilters) => api.get<PageResponse<Journal>>(`/journals${toQuery({ ...f })}`),
  journal: (id: number) => api.get<Journal>(`/journals/${id}`),
  createJournal: (body: JournalInput) => api.post<Journal>('/journals', body),
  updateJournal: (id: number, body: JournalInput) => api.put<Journal>(`/journals/${id}`, body),
  submitJournal: (id: number) => api.post<Journal>(`/journals/${id}/submit`),
  approveJournal: (id: number) => api.post<Journal>(`/journals/${id}/approve`),
  rejectJournal: (id: number, reason: string) =>
    api.post<Journal>(`/journals/${id}/reject`, { reason }),
  cancelJournal: (id: number) => api.post<Journal>(`/journals/${id}/cancel`),
  reverseJournal: (id: number, reversalDate: string, reason: string) =>
    api.post<Journal>(`/journals/${id}/reverse`, { reversalDate, reason }),
  copyJournal: (id: number, valueDate: string) =>
    api.post<Journal>(`/journals/${id}/copy${toQuery({ valueDate })}`),

  statement: (accountId: number, from: string, to: string, branchId?: number) =>
    api.get<AccountStatement>(
      `/ledger/accounts/${accountId}/statement${toQuery({ from, to, branchId })}`,
    ),
};
