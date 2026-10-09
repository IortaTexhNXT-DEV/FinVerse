import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type { ReceiptKind } from './cashieringTypes';

/**
 * BDOI's AR / OR creation, cancellation and reinstatement records (Operations Cashiering FRS
 * v3.2: FRS.CSH.02.01, 02.02, 02.05, 03.01, 04.01).
 */

export type RecordKind = 'CREATION' | 'CANCELLATION' | 'REINSTATEMENT';
export type RecordStage = 'CREATED' | 'FOR_POSTING' | 'RETURNED' | 'POSTED' | 'RECORD_CANCELLED';
export type EntryType = 'CLIENT' | 'INSURER' | 'OTHER';
export type TenderType = 'CASH' | 'CHECK' | 'DIRECT_CREDIT';
export type ReinstatementType = 'FULL' | 'PARTIAL';

export interface RecordAccountLine {
  reference: string;
  amount: number;
}

export interface RecordParty {
  entryType?: EntryType;
  clientCode?: string;
  clientName?: string;
  insurerCode?: string;
  insurerName?: string;
  insurerBank?: string;
  payorName?: string;
}

export interface RecordTender {
  tenderType?: TenderType;
  currency?: string;
  bankAccount?: string;
  amount?: number;
  vat?: number;
  wtax?: number;
  certificateRef?: string;
  check?: { checkNo?: string; checkDate?: string; checkBank?: string };
  receiptDate?: string;
  remarks?: string;
  otherIncomeRef?: string;
}

export interface RecordReason {
  reasonCode?: string;
  reasonText?: string;
  reinstatementType?: ReinstatementType;
  invoiceNo?: string;
  accountOfficer?: string;
  unitHead?: string;
  teamLeader?: string;
}

export interface ReceiptRecord {
  id: number;
  recordNo: string;
  recordKind: RecordKind;
  receiptKind: ReceiptKind;
  receiptType?: string;
  receiptTypeLabel?: string;
  stage: RecordStage;
  statusLabel: string;
  editable: boolean;
  branchId?: number;
  branchName?: string;
  party?: RecordParty;
  tender?: RecordTender;
  bankAccountName?: string;
  reason?: RecordReason;
  reasonLabel?: string;
  receiptId?: number;
  receiptNo?: string;
  accounts: RecordAccountLine[];
  accountsText: string;
  total: number;
  returnReason?: string;
  createdBy?: string;
  createdByUser?: string;
  createdAt: string;
  submittedAt?: string;
  postedBy?: string;
  postedAt?: string;
  postingResult?: string;
}

export interface RecordBody {
  companyId: number;
  receiptKind: ReceiptKind;
  receiptType?: string;
  branchId?: number;
  entryType?: EntryType;
  clientCode?: string;
  clientName?: string;
  insurerCode?: string;
  insurerName?: string;
  insurerBank?: string;
  payorName?: string;
  tenderType?: TenderType;
  currency?: string;
  bankAccount?: string;
  amount?: number;
  vat?: number;
  wtax?: number;
  certificateRef?: string;
  checkNo?: string;
  checkDate?: string;
  checkBank?: string;
  receiptDate?: string;
  remarks?: string;
  otherIncomeRef?: string;
  accounts: RecordAccountLine[];
}

export interface BankOption {
  code: string;
  name: string;
  currency: string;
  defaultForCurrency: boolean;
}

export interface BranchOption {
  id: number;
  code: string;
  name: string;
}

export interface FormSettings {
  arPostingStep: boolean;
  orPostingStep: boolean;
  remarksRequired: boolean;
  maxAmount: number;
  latestCheckDate?: string;
  holdingDays: number;
  bankAccounts: BankOption[];
  arBranches: BranchOption[];
  orBranches: BranchOption[];
  usdRate: number;
  today: string;
}

export interface AccountRow {
  invoiceNo: string;
  arn: string;
  policyNo?: string;
  clientCode?: string;
  assuredName?: string;
  insurerCode?: string;
  currency?: string;
  premiumReceivable?: number;
  commissionReceivable?: number;
  totalInvoice?: number;
  totalPaid?: number;
  outstanding?: number;
  bir2307: boolean;
  pr2307?: number;
  prebooked: boolean;
}

export interface PostingOutcome {
  id: number;
  recordNo: string;
  posted: boolean;
  message: string;
  receiptNo?: string;
}

export interface RecordFilters {
  stages?: RecordStage[];
  receiptKind?: ReceiptKind;
  receiptType?: string;
  branchId?: number;
  createdBy?: string;
  from?: string;
  to?: string;
  recordNo?: string;
  name?: string;
}

export interface ReinstatementBody {
  companyId: number;
  receiptIds: number[];
  type?: ReinstatementType;
  accounts: string[];
  reasonCode?: string;
  remarks?: string;
  invoiceNo?: string;
  accountOfficer?: string;
  unitHead?: string;
  teamLeader?: string;
}

const BASE = '/cashiering/records';

export const recordsApi = {
  list: (companyId: number, kind: RecordKind, f: RecordFilters, page = 0) =>
    api.get<PageResponse<ReceiptRecord>>(
      `${BASE}${toQuery({ companyId, kind, ...f, stages: f.stages?.join(','), page })}`,
    ),
  get: (id: number) => api.get<ReceiptRecord>(`${BASE}/${id}`),
  create: (body: RecordBody) => api.post<ReceiptRecord>(BASE, body),
  edit: (id: number, body: RecordBody) => api.put<ReceiptRecord>(`${BASE}/${id}`, body),
  submit: (id: number) => api.post<ReceiptRecord>(`${BASE}/${id}/submit`),
  cancelRecord: (id: number) => api.post<ReceiptRecord>(`${BASE}/${id}/cancel-record`),
  post: (ids: number[]) => api.post<PostingOutcome[]>(`${BASE}/post`, { ids }),
  returnToCreator: (id: number, reason: string) =>
    api.post<ReceiptRecord>(`${BASE}/${id}/return`, { reason }),
  settings: (companyId: number) =>
    api.get<FormSettings>(`${BASE}/settings${toQuery({ companyId })}`),
  accounts: (companyId: number, q: string) =>
    api.get<AccountRow[]>(`${BASE}/accounts${toQuery({ companyId, q })}`),
  day: (companyId: number, date?: string) =>
    api.get<ReceiptRecord[]>(`${BASE}/day${toQuery({ companyId, date })}`),
  cancellations: (
    companyId: number,
    receiptIds: number[],
    reasonCode: string,
    reasonText: string,
  ) =>
    api.post<ReceiptRecord[]>(`${BASE}/cancellations`, {
      companyId,
      receiptIds,
      reasonCode,
      reasonText,
    }),
  reinstatements: (body: ReinstatementBody) =>
    api.post<ReceiptRecord[]>(`${BASE}/reinstatements`, body),
  editReason: (id: number, body: ReinstatementBody) =>
    api.put<ReceiptRecord>(`${BASE}/${id}/reason`, body),
};
