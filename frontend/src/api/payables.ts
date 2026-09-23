import { api, toQuery } from './client';
import type { PageResponse, RecordStatus } from './types';

/** Payables, payments, PDC issued and petty cash API (backend module `payables`). */

export type NotificationFormat = 'FIXED_WIDTH' | 'CSV';
export type InvoiceStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'CANCELLED';
export type VoucherStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'CANCELLED' | 'VOIDED';
export type PaymentMode = 'CHEQUE' | 'BANK_TRANSFER' | 'PDC';
export type PaymentCategory =
  'SUPPLIER' | 'COMMISSION' | 'CLAIM' | 'REINSURANCE' | 'PREMIUM_REFUND';
export type PdcStatus = 'ISSUED' | 'DUE' | 'PRESENTED' | 'CLEARED' | 'CANCELLED' | 'REPLACED';
export type PettyCashStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';

export interface BankAccount {
  id: number;
  companyId: number;
  code: string;
  name: string;
  bankPartyCode?: string;
  bankName: string;
  accountNo: string;
  currency: string;
  glAccountCode: string;
  pdcClearingAccountCode?: string;
  branchId?: number;
  notificationFormat: NotificationFormat;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export type BankAccountRequest = Omit<
  BankAccount,
  'id' | 'recordStatus' | 'createdBy' | 'authorizedBy'
>;

export interface ChequeBook {
  id: number;
  bankAccountId: number;
  firstNo: number;
  lastNo: number;
  nextNo: number;
  remaining: number;
  receivedOn: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';
}

export interface InvoiceLine {
  lineNo?: number;
  expenseAccountCode: string;
  costCenter?: string;
  description: string;
  netAmount: number;
  vatAmount?: number;
  whtAmount?: number;
}

export interface Invoice {
  id: number;
  documentNo: string;
  companyId: number;
  branchId: number;
  partyCode: string;
  supplierInvoiceNo: string;
  invoiceDate: string;
  dueDate: string;
  currency: string;
  vatApplicable: boolean;
  whtRate: number;
  netAmount: number;
  vatAmount: number;
  whtAmount: number;
  payableAmount: number;
  narration?: string;
  status: InvoiceStatus;
  statusReason?: string;
  createdBy: string;
  submittedBy?: string;
  approvedBy?: string;
  journalBatchNos?: string;
  lines: InvoiceLine[];
}

export interface InvoiceRequest {
  companyId: number;
  branchId: number;
  partyCode: string;
  supplierInvoiceNo: string;
  invoiceDate: string;
  dueDate?: string;
  currency?: string;
  vatApplicable: boolean;
  narration?: string;
  lines: InvoiceLine[];
}

export interface PayableItem {
  openItemId: number;
  documentType: string;
  documentNo: string;
  documentDate: string;
  dueDate: string;
  currency: string;
  amount: number;
  outstanding: number;
  available: number;
  narration?: string;
}

export interface Allocation {
  openItemId: number;
  documentType: string;
  documentNo: string;
  documentDate: string;
  dueDate: string;
  amount: number;
}

export interface Voucher {
  id: number;
  voucherNo: string;
  companyId: number;
  branchId: number;
  partyCode: string;
  payeeName: string;
  category: PaymentCategory;
  paymentMode: PaymentMode;
  bankAccountId: number;
  voucherDate: string;
  chequeNo?: string;
  chequeDate?: string;
  currency: string;
  amount: number;
  baseAmount?: number;
  department?: string;
  narration?: string;
  status: VoucherStatus;
  statusReason?: string;
  createdBy: string;
  approvedBy?: string;
  journalBatchNo?: string;
  presentedOn?: string;
  voidedOn?: string;
  allocations: Allocation[];
}

export interface PaymentRequest {
  companyId: number;
  branchId: number;
  partyCode: string;
  payeeName?: string;
  category?: PaymentCategory;
  mode: PaymentMode;
  bankAccountId: number;
  voucherDate: string;
  chequeDate?: string;
  department?: string;
  narration?: string;
  items: { openItemId: number; amount: number }[];
}

export interface Pdc {
  id: number;
  voucherId: number;
  bankAccountId: number;
  branchId: number;
  partyCode: string;
  payeeName: string;
  chequeNo: string;
  chequeDate: string;
  issueDate: string;
  currency: string;
  amount: number;
  baseAmount: number;
  department?: string;
  status: PdcStatus;
  presentedOn?: string;
  presentationBatchNo?: string;
  clearedOn?: string;
  cancelledOn?: string;
  replacedOn?: string;
  remarks?: string;
}

export interface PdcEvent {
  eventDate: string;
  fromStatus?: PdcStatus;
  toStatus: PdcStatus;
  batchNo?: string;
  remarks?: string;
  createdBy: string;
  createdAt: string;
}

export interface Fund {
  id: number;
  companyId: number;
  branchId: number;
  code: string;
  name: string;
  custodian: string;
  glAccountCode: string;
  replenishBankAccountId: number;
  currency: string;
  imprestAmount: number;
  cashBalance: number;
  pendingReimbursement: number;
  establishedOn?: string;
  recordStatus: RecordStatus;
  createdBy: string;
}

export interface FundRequest {
  companyId: number;
  branchId: number;
  code: string;
  name: string;
  custodian: string;
  glAccountCode: string;
  replenishBankAccountId: number;
  imprestAmount: number;
}

export interface Disbursement {
  id: number;
  fundId: number;
  documentNo: string;
  date: string;
  payee: string;
  expenseAccountCode: string;
  costCenter?: string;
  description: string;
  receiptRef?: string;
  amount: number;
  status: PettyCashStatus;
  statusReason?: string;
  createdBy: string;
  approvedBy?: string;
  reimbursementId?: number;
}

export type DisbursementRequest = Pick<
  Disbursement,
  'date' | 'payee' | 'expenseAccountCode' | 'costCenter' | 'description' | 'receiptRef' | 'amount'
>;

export interface Reimbursement {
  id: number;
  fundId: number;
  documentNo: string;
  claimDate: string;
  bankAccountId: number;
  amount: number;
  narration?: string;
  status: PettyCashStatus;
  createdBy: string;
  approvedBy?: string;
  journalBatchNo?: string;
}

export interface Party {
  id: number;
  code: string;
  name: string;
  partyType: string;
  defaultCurrency: string;
  creditDays: number;
  withholdingTaxRate?: number;
}

export interface ListFilter {
  companyId: number;
  status?: string;
  partyCode?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

const BASE = '/payables';
const reason = (text: string) => ({ reason: text });

/** Query string with the company and a repeated list parameter (e.g. types=A&types=B). */
export function listQuery(companyId: number, name: string, values: string[]): string {
  const search = new URLSearchParams({ companyId: String(companyId) });
  values.forEach((v) => search.append(name, v));
  return `?${search.toString()}`;
}

export const payablesApi = {
  parties: (companyId: number, types: string[]) =>
    api.get<Party[]>(`/parties${listQuery(companyId, 'types', types)}`),

  bankAccounts: (companyId: number) =>
    api.get<BankAccount[]>(`${BASE}/bank-accounts${toQuery({ companyId })}`),
  createBankAccount: (body: BankAccountRequest) =>
    api.post<BankAccount>(`${BASE}/bank-accounts`, body),
  updateBankAccount: (id: number, body: BankAccountRequest) =>
    api.put<BankAccount>(`${BASE}/bank-accounts/${id}`, body),
  authorizeBankAccount: (id: number) =>
    api.post<BankAccount>(`${BASE}/bank-accounts/${id}/authorize`),
  chequeBooks: (id: number) => api.get<ChequeBook[]>(`${BASE}/bank-accounts/${id}/cheque-books`),
  addChequeBook: (id: number, body: { firstNo: number; lastNo: number; receivedOn: string }) =>
    api.post<ChequeBook>(`${BASE}/bank-accounts/${id}/cheque-books`, body),

  invoices: (f: ListFilter) =>
    api.get<PageResponse<Invoice>>(`${BASE}/invoices${toQuery({ ...f })}`),
  invoice: (id: number) => api.get<Invoice>(`${BASE}/invoices/${id}`),
  createInvoice: (body: InvoiceRequest) => api.post<Invoice>(`${BASE}/invoices`, body),
  submitInvoice: (id: number) => api.post<Invoice>(`${BASE}/invoices/${id}/submit`),
  approveInvoice: (id: number) => api.post<Invoice>(`${BASE}/invoices/${id}/approve`),
  rejectInvoice: (id: number, text: string) =>
    api.post<Invoice>(`${BASE}/invoices/${id}/reject`, reason(text)),
  cancelInvoice: (id: number, text: string) =>
    api.post<Invoice>(`${BASE}/invoices/${id}/cancel`, reason(text)),

  payableItems: (companyId: number, partyCode: string) =>
    api.get<PayableItem[]>(`${BASE}/vouchers/payable-items${toQuery({ companyId, partyCode })}`),
  vouchers: (f: ListFilter) =>
    api.get<PageResponse<Voucher>>(`${BASE}/vouchers${toQuery({ ...f })}`),
  voucher: (id: number) => api.get<Voucher>(`${BASE}/vouchers/${id}`),
  createVoucher: (body: PaymentRequest) => api.post<Voucher>(`${BASE}/vouchers`, body),
  submitVoucher: (id: number) => api.post<Voucher>(`${BASE}/vouchers/${id}/submit`),
  approveVoucher: (id: number) => api.post<Voucher>(`${BASE}/vouchers/${id}/approve`),
  rejectVoucher: (id: number, text: string) =>
    api.post<Voucher>(`${BASE}/vouchers/${id}/reject`, reason(text)),
  cancelVoucher: (id: number, text: string) =>
    api.post<Voucher>(`${BASE}/vouchers/${id}/cancel`, reason(text)),
  voidVoucher: (id: number, date: string, text: string) =>
    api.post<Voucher>(`${BASE}/vouchers/${id}/void`, { date, reason: text }),
  presentedVoucher: (id: number, date: string) =>
    api.post<Voucher>(`${BASE}/vouchers/${id}/presented`, { date }),

  pdcs: (companyId: number, statuses: PdcStatus[]) =>
    api.get<Pdc[]>(`${BASE}/pdc-issued${listQuery(companyId, 'statuses', statuses)}`),
  pdcHistory: (id: number) => api.get<PdcEvent[]>(`${BASE}/pdc-issued/${id}/history`),
  refreshDue: (asOf: string) =>
    api.post<number>(`${BASE}/pdc-issued/refresh-due${toQuery({ asOf })}`),
  presentPdc: (id: number, date: string) =>
    api.post<Pdc>(`${BASE}/pdc-issued/${id}/present`, { date }),
  clearPdc: (id: number, date: string) => api.post<Pdc>(`${BASE}/pdc-issued/${id}/clear`, { date }),
  cancelPdc: (id: number, date: string, text: string) =>
    api.post<Pdc>(`${BASE}/pdc-issued/${id}/cancel`, { date, reason: text }),
  replacePdc: (id: number, chequeDate: string, date: string, text: string) =>
    api.post<Pdc>(`${BASE}/pdc-issued/${id}/replace`, { chequeDate, date, reason: text }),

  funds: (companyId: number) =>
    api.get<Fund[]>(`${BASE}/petty-cash/funds${toQuery({ companyId })}`),
  createFund: (body: FundRequest) => api.post<Fund>(`${BASE}/petty-cash/funds`, body),
  authorizeFund: (id: number) => api.post<Fund>(`${BASE}/petty-cash/funds/${id}/authorize`),
  establishFund: (id: number, date: string) =>
    api.post<Fund>(`${BASE}/petty-cash/funds/${id}/establish`, { date }),
  disbursements: (fundId: number) =>
    api.get<Disbursement[]>(`${BASE}/petty-cash/funds/${fundId}/disbursements`),
  disburse: (fundId: number, body: DisbursementRequest) =>
    api.post<Disbursement>(`${BASE}/petty-cash/funds/${fundId}/disbursements`, body),
  approveDisbursement: (id: number) =>
    api.post<Disbursement>(`${BASE}/petty-cash/disbursements/${id}/approve`),
  rejectDisbursement: (id: number, text: string) =>
    api.post<Disbursement>(`${BASE}/petty-cash/disbursements/${id}/reject`, reason(text)),
  reimbursements: (fundId: number) =>
    api.get<Reimbursement[]>(`${BASE}/petty-cash/funds/${fundId}/reimbursements`),
  claimReimbursement: (fundId: number, date: string) =>
    api.post<Reimbursement>(`${BASE}/petty-cash/funds/${fundId}/reimbursements`, { date }),
  approveReimbursement: (id: number) =>
    api.post<Reimbursement>(`${BASE}/petty-cash/reimbursements/${id}/approve`),
  rejectReimbursement: (id: number, text: string) =>
    api.post<Reimbursement>(`${BASE}/petty-cash/reimbursements/${id}/reject`, reason(text)),

  notificationFileUrl: (bankAccountId: number, from: string, to: string, includeCheques: boolean) =>
    `${BASE}/payment-notifications/file${toQuery({ bankAccountId, from, to, includeCheques })}`,
};
