/** Disbursement API client (DIS 2.2-3.28): /api/v1/disbursement. */
import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type {
  RequestStatus,
  VoucherStage,
  InstrumentStatus,
  PayeeStage,
  FundingStage,
  Summary,
  PaymentRequest,
  EncodeInput,
  VoucherSummary,
  Line,
  StatusEdit,
  Voucher,
  TermsInput,
  AllocationRow,
  ItemResult,
  PayeeSummary,
  Payee,
  AccountInput,
  PayeeInput,
  PayeeRequest,
  EodRun,
  Funding,
  FundingInput,
  Bank,
} from './types';

export type * from './types';

const BASE = '/disbursement';

type QueryValue = string | number | boolean | null | undefined | readonly string[];

/** A query string; arrays repeat their key, empty values are left out. */
export function toQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    const values = Array.isArray(value) ? (value as readonly string[]) : [value];
    values
      .filter((v) => v !== undefined && v !== null && v !== '')
      .forEach((v) => search.append(key, String(v)));
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

const voucherPath = (id: number, action: string) => `${BASE}/vouchers/${id}/${action}`;
const instrumentPath = (id: number, action: string) =>
  `${BASE}/vouchers/${id}/instrument/${action}`;

export const disbursementApi = {
  summary: (companyId: number) => api.get<Summary>(`${BASE}/summary${toQuery({ companyId })}`),
  requests: (
    companyId: number,
    statuses: readonly RequestStatus[],
    filters: { q: string; from: string; to: string },
    page: number,
  ) =>
    api.get<PageResponse<PaymentRequest>>(
      `${BASE}/requests${toQuery({ companyId, status: statuses, ...filters, page, size: 20 })}`,
    ),
  encode: (body: EncodeInput) => api.post<PaymentRequest>(`${BASE}/requests`, body),
  createVoucher: (id: number) => api.post<PaymentRequest>(`${BASE}/requests/${id}/voucher`),
  returnRequest: (id: number, reasonCode: string, comment?: string) =>
    api.post<PaymentRequest>(`${BASE}/requests/${id}/return`, { reasonCode, comment }),
  releaseRequest: (id: number) => api.post<PaymentRequest>(`${BASE}/requests/${id}/release`),
  vouchers: (
    companyId: number,
    stages: readonly VoucherStage[],
    filters: { q: string; type: string; unregularized?: boolean },
    page: number,
  ) =>
    api.get<PageResponse<VoucherSummary>>(
      `${BASE}/vouchers${toQuery({ companyId, stage: stages, ...filters, page, size: 20 })}`,
    ),
  voucher: (id: number) => api.get<Voucher>(`${BASE}/vouchers/${id}`),
  terms: (id: number, body: TermsInput) => api.put<Voucher>(voucherPath(id, 'terms'), body),
  proforma: (id: number, lines: Line[]) => api.put<Voucher>(voucherPath(id, 'proforma'), { lines }),
  resetProforma: (id: number) => api.post<Voucher>(voucherPath(id, 'proforma/reset')),
  allocate: (id: number, rows: AllocationRow[]) =>
    api.post<Voucher>(voucherPath(id, 'allocation'), { rows }),
  act: (id: number, action: string, comment?: string) =>
    api.post<Voucher>(voucherPath(id, action), { comment }),
  withReason: (id: number, action: 'reject' | 'cancel', reasonCode: string, comment?: string) =>
    api.post<Voucher>(voucherPath(id, action), { reasonCode, comment }),
  approveAll: (ids: number[], comment?: string) =>
    api.post<ItemResult[]>(`${BASE}/vouchers/approve`, { ids, comment }),
  voucherDocument: (id: number) => api.getFile(voucherPath(id, 'document')),
  tagReceipt: (id: number, body: Record<string, unknown>) =>
    api.post<Voucher>(voucherPath(id, 'tags/receipt'), body),
  tagCwt: (id: number, body: Record<string, unknown>) =>
    api.post<Voucher>(voucherPath(id, 'tags/cwt'), body),
  instrument: (id: number, action: string, body?: unknown) =>
    api.post<Voucher>(instrumentPath(id, action), body),
  instrumentDocument: (id: number) => api.getFile(instrumentPath(id, 'document')),
  reissue: (id: number) => api.post<PaymentRequest>(instrumentPath(id, 'reissue')),
  requestEdit: (id: number, toStatus: InstrumentStatus, reason: string) =>
    api.post<StatusEdit>(instrumentPath(id, 'status-edits'), { toStatus, reason }),
  approveEdit: (editId: number) => api.post<StatusEdit>(`${BASE}/status-edits/${editId}/approve`),
  payees: (companyId: number, stages: readonly PayeeStage[], q: string, page: number) =>
    api.get<PageResponse<PayeeSummary>>(
      `${BASE}/payees${toQuery({ companyId, stage: stages, q, page, size: 20 })}`,
    ),
  payee: (id: number) => api.get<Payee>(`${BASE}/payees/${id}`),
  createPayee: (body: PayeeInput) => api.post<Payee>(`${BASE}/payees`, body),
  updatePayee: (id: number, body: PayeeInput) => api.put<Payee>(`${BASE}/payees/${id}`, body),
  payeeAction: (id: number, action: string) => api.post<Payee>(`${BASE}/payees/${id}/${action}`),
  addAccount: (id: number, body: AccountInput) =>
    api.post<Payee>(`${BASE}/payees/${id}/accounts`, body),
  deactivateAccount: (id: number, accountId: number) =>
    api.post<Payee>(`${BASE}/payees/${id}/accounts/${accountId}/deactivate`),
  deletePayee: (id: number) => api.delete(`${BASE}/payees/${id}`),
  payeeRequests: (companyId: number, page: number) =>
    api.get<PageResponse<PayeeRequest>>(
      `${BASE}/payee-requests${toQuery({ companyId, page, size: 20 })}`,
    ),
  closePayeeRequest: (id: number) => api.post<PayeeRequest>(`${BASE}/payee-requests/${id}/close`),
  eodRuns: (companyId: number, page: number) =>
    api.get<PageResponse<EodRun>>(`${BASE}/eod/runs${toQuery({ companyId, page, size: 10 })}`),
  eodRun: (id: number) => api.get<EodRun>(`${BASE}/eod/runs/${id}`),
  runEod: (companyId: number, businessDate: string) =>
    api.post<EodRun>(`${BASE}/eod/runs`, { companyId, businessDate }),
  confirmEod: (id: number) => api.post<EodRun>(`${BASE}/eod/runs/${id}/confirm`),
  eodOutput: (outputId: number) => api.getFile(`${BASE}/eod/outputs/${outputId}`),
  fundings: (companyId: number, stages: readonly FundingStage[], page: number) =>
    api.get<PageResponse<Funding>>(
      `${BASE}/funding${toQuery({ companyId, stage: stages, page, size: 20 })}`,
    ),
  funding: (id: number) => api.get<Funding>(`${BASE}/funding/${id}`),
  createFunding: (body: FundingInput) => api.post<Funding>(`${BASE}/funding`, body),
  updateFunding: (id: number, body: FundingInput) =>
    api.put<Funding>(`${BASE}/funding/${id}`, body),
  fundingAction: (id: number, action: 'submit' | 'verify' | 'approve', body?: unknown) =>
    api.post<Funding>(`${BASE}/funding/${id}/${action}`, body),
  banks: (companyId: number) => api.get<Bank[]>(`${BASE}/banks${toQuery({ companyId })}`),
  bankStatus: (id: number, status: 'ACTIVE' | 'INACTIVE') =>
    api.post<Bank>(`${BASE}/banks/${id}/status`, { status }),
  authorizeBank: (id: number) => api.post<Bank>(`${BASE}/banks/${id}/authorize`),
  addBook: (id: number, body: { firstNo: number; lastNo: number; receivedOn?: string }) =>
    api.post<Bank>(`${BASE}/banks/${id}/cheque-books`, body),
  editBook: (bookId: number, body: { firstNo: number; lastNo: number }) =>
    api.put<Bank>(`${BASE}/cheque-books/${bookId}`, body),
};
