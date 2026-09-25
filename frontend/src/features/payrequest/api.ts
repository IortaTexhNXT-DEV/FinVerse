import { api, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Refund and cash-advance requests API (BRD-5 MKT 1.2-2.26): refund requests (RRF), cash advances
 * (RFP), check cancellations, validations, the liquidation of a cash advance and its accounts.
 */

export const REQUEST_ENTITY = 'PaymentRequest';

export type RequestKind = 'REFUND' | 'CASH_ADVANCE' | 'CHECK_CANCELLATION';
export type RequestStage =
  | 'DRAFT'
  | 'PREPARING'
  | 'FOR_VALIDATION'
  | 'FOR_REVIEW'
  | 'FOR_APPROVAL'
  | 'HR_APPROVAL'
  | 'SENT_TO_DISBURSEMENT'
  | 'DISBURSED'
  | 'REQUESTED'
  | 'SENT'
  | 'CANCELLED';
export type ValidationStatus = 'OPEN' | 'DEFERRED' | 'CONFIRMED' | 'REJECTED';
export type LiquidationStatus = 'DRAFT' | 'SUBMITTED' | 'POSTED';

export interface RequestSummary {
  id: number;
  requestNo: string;
  kind: RequestKind;
  stage: RequestStage;
  requestDate: string;
  payeeName: string;
  payeeCode: string;
  paymentMode: string;
  currency: string;
  amount: number;
  validationRequired: boolean;
  dvNo?: string;
  disbursementStatus?: string;
  instrumentStatus?: string;
  createdBy: string;
  createdAt: string;
}

export interface RequestContent {
  segment?: string;
  referenceText?: string;
  requestingUnit?: string;
  rfpType?: string;
  purpose: string;
  currency: string;
}

export interface Payee {
  type: 'CLIENT' | 'EMPLOYEE';
  code: string;
  name: string;
  mode: string;
  accountNo?: string;
  accountName?: string;
}

export interface RequestTrail {
  submittedBy?: string;
  submittedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  hrApprovedBy?: string;
  hrApprovedAt?: string;
  returnReason?: string;
  returnComment?: string;
}

export interface DisbursementTrack {
  requestNo?: string;
  status?: string;
  dvNo?: string;
  dvStatus?: string;
  instrumentStatus?: string;
  message?: string;
  disbursedAt?: string;
  handoffRef?: string;
}

export interface CancellationTarget {
  requestNo?: string;
  dvNo?: string;
  checkNo?: string;
  reasonCode?: string;
}

export interface RefundLineView {
  lineNo: number;
  arNo: string;
  clientCode: string;
  assuredName: string;
  invoiceNo?: string;
  rootInvoiceNo?: string;
  amount: number;
  reasonCode: string;
  branchUnit?: string;
  categoryA?: string;
  categoryB?: string;
  accountName?: string;
  cancelledPolicy: boolean;
}

export interface ExpenseView {
  lineNo: number;
  fieldworkDate: string;
  particulars: string;
  perDiem: number;
  representation: number;
  transport: number;
  lodging: number;
  others: number;
  total: number;
}

export interface LiquidationView {
  id: number;
  liquidationNo: string;
  status: LiquidationStatus;
  jobLevel?: string;
  costCenter?: string;
  remarks?: string;
  totalExpenses: number;
  cashAdvanced: number;
  overShort: number;
  submittedBy?: string;
  postedBy?: string;
  postedAt?: string;
  journalBatchNo?: string;
  days: ExpenseView[];
}

export interface PayRequest {
  id: number;
  requestNo: string;
  kind: RequestKind;
  stage: RequestStage;
  companyId: number;
  requestDate: string;
  content: RequestContent;
  payee: Payee;
  amount: number;
  validationRequired: boolean;
  validationRound: number;
  target: CancellationTarget;
  trail: RequestTrail;
  track: DisbursementTrack;
  payoutRecorded: boolean;
  lines: RefundLineView[];
  liquidation?: LiquidationView;
  createdBy: string;
  createdAt: string;
}

export interface ValidationView {
  id: number;
  roundNo: number;
  lineNo: number;
  validator: 'ACSL' | 'CASHIERING';
  status: ValidationStatus;
  ticketRef?: string;
  message?: string;
  newArNo?: string;
  remarks?: string;
  completedBy?: string;
  completedAt?: string;
}

export interface PayoutAccount {
  id: number;
  mode: 'CTA' | 'CHECK';
  payeeName: string;
  accountNo?: string;
  sourceModule: string;
  sourceRef: string;
  active: boolean;
  createdAt: string;
  createdBy: string;
}

export interface AccountView {
  role: string;
  accountCode: string;
}

export interface RefundLineInput {
  arNo: string;
  clientCode: string;
  assuredName: string;
  invoiceNo?: string;
  amount: number;
  reasonCode: string;
  branchUnit?: string;
  categoryA?: string;
  categoryB?: string;
  accountName?: string;
}

export interface RefundInput {
  segment?: string;
  referenceText?: string;
  requestingUnit?: string;
  purpose?: string;
  currency?: string;
  paymentMode: string;
  accountNo?: string;
  accountName?: string;
  lines: RefundLineInput[];
}

export interface CashAdvanceInput {
  segment?: string;
  referenceText?: string;
  requestingUnit?: string;
  rfpType?: string;
  purpose: string;
  currency?: string;
  employeeNo: string;
  employeeName: string;
  paymentMode: string;
  accountNo?: string;
  accountName?: string;
  amount: number;
}

export interface CheckCancellationInput {
  targetRequestNo: string;
  checkNo?: string;
  reasonCode: string;
  remarks?: string;
}

export interface ExpenseInput {
  fieldworkDate: string;
  particulars: string;
  perDiem?: number;
  representation?: number;
  transport?: number;
  lodging?: number;
  others?: number;
}

export interface LiquidationInput {
  jobLevel?: string;
  costCenter?: string;
  remarks?: string;
  days: ExpenseInput[];
}

export interface RequestFilter {
  stage?: RequestStage;
  kind?: RequestKind;
  from?: string;
  to?: string;
  q?: string;
  page?: number;
}

export type StageCounts = Partial<Record<RequestStage, number>>;

const BASE = '/payment-requests';
const request = (id: number) => `${BASE}/requests/${String(id)}`;

export const payRequestApi = {
  search: (companyId: number, f: RequestFilter) =>
    api.get<PageResponse<RequestSummary>>(
      `${BASE}/requests${toQuery({ companyId, ...f, page: f.page ?? 0, size: 20 })}`,
    ),
  counts: (companyId: number) =>
    api.get<StageCounts>(`${BASE}/requests/counts${toQuery({ companyId })}`),
  get: (id: number) => api.get<PayRequest>(request(id)),
  validations: (id: number) => api.get<ValidationView[]>(`${request(id)}/validations`),
  form: (id: number): Promise<DownloadedFile> => api.getFile(`${request(id)}/form`),
  liquidationForm: (id: number): Promise<DownloadedFile> =>
    api.getFile(`${request(id)}/liquidation/form`),
  payoutAccounts: (companyId: number, clientCode: string) =>
    api.get<PayoutAccount[]>(`${BASE}/payout-accounts${toQuery({ companyId, clientCode })}`),
  createRefund: (companyId: number, input: RefundInput) =>
    api.post<PayRequest>(`${BASE}/requests/refunds${toQuery({ companyId })}`, input),
  updateRefund: (id: number, input: RefundInput) =>
    api.put<PayRequest>(`${request(id)}/refund`, input),
  createCashAdvance: (companyId: number, input: CashAdvanceInput) =>
    api.post<PayRequest>(`${BASE}/requests/cash-advances${toQuery({ companyId })}`, input),
  updateCashAdvance: (id: number, input: CashAdvanceInput) =>
    api.put<PayRequest>(`${request(id)}/cash-advance`, input),
  createCheckCancellation: (companyId: number, input: CheckCancellationInput) =>
    api.post<PayRequest>(`${BASE}/requests/check-cancellations${toQuery({ companyId })}`, input),
  assign: (id: number, username: string, comment?: string) =>
    api.post<PayRequest>(`${request(id)}/assign`, { username, comment }),
  submit: (id: number, comment?: string) =>
    api.post<PayRequest>(`${request(id)}/submit`, { comment }),
  endorse: (id: number, comment?: string) =>
    api.post<PayRequest>(`${request(id)}/endorse`, { comment }),
  approve: (id: number, comment?: string) =>
    api.post<PayRequest>(`${request(id)}/approve`, { comment }),
  recordValidation: (
    id: number,
    validationId: number,
    body: { confirmed: boolean; newArNo?: string; remarks?: string },
  ) => api.post<ValidationView>(`${request(id)}/validations/${String(validationId)}/result`, body),
  saveLiquidation: (id: number, input: LiquidationInput) =>
    api.put<LiquidationView>(`${request(id)}/liquidation`, input),
  submitLiquidation: (id: number) => api.post<LiquidationView>(`${request(id)}/liquidation/submit`),
  returnLiquidation: (id: number, comment: string) =>
    api.post<LiquidationView>(`${request(id)}/liquidation/return`, { comment }),
  postLiquidation: (id: number) => api.post<LiquidationView>(`${request(id)}/liquidation/post`),
  accounts: (companyId: number) =>
    api.get<AccountView[]>(`${BASE}/liquidation-accounts${toQuery({ companyId })}`),
  assignAccount: (companyId: number, role: string, accountCode: string) =>
    api.put<AccountView>(`${BASE}/liquidation-accounts${toQuery({ companyId })}`, {
      role,
      accountCode,
    }),
};
