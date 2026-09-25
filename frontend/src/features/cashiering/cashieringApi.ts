import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type {
  IntakeResult,
  OrBody,
  Payment,
  PaymentPreview,
  PdcStatus,
  PickupStatus,
  Prebooked,
  ReceiptAction,
  ReceiptCriteria,
  ReceiptDetail,
  ReceiptSummary,
  ReceivePaymentBody,
  ReinstateBody,
  UnappliedTab,
} from './cashieringTypes';
import type {
  BulkResult,
  CommissionLine,
  CwtBatch,
  CwtExpected,
  CwtTag,
  CwtTagBody,
  Disposition,
  DispositionBody,
  DispositionType,
  Layout,
  MinimalBalanceRule,
  PdcBody,
  PdcItem,
  Pickup,
  PrintBatch,
  Series,
  SeriesBody,
  UnappliedItem,
} from './cashieringQueueTypes';

/**
 * Cashiering API (BRD-2 CSHID.001-027, MKTID.010/013, DBMID.001): receipts and their actions,
 * payment intake and preview, pre-booked and unapplied payments, checks, batch printing, BIR 2307
 * and the Cashiering setup.
 */

export type * from './cashieringTypes';
export type * from './cashieringQueueTypes';

const BASE = '/cashiering';

type Counts = Record<string, number>;

const post = <T>(path: string, body?: unknown) => api.post<T>(`${BASE}${path}`, body);
const get = <T>(path: string) => api.get<T>(`${BASE}${path}`);

export const cashieringApi = {
  receipts: (c: ReceiptCriteria, page = 0) =>
    get<PageResponse<ReceiptSummary>>(`/receipts${toQuery({ ...c, page })}`),
  receipt: (id: number) => get<ReceiptDetail>(`/receipts/${id}`),
  issueOr: (body: OrBody) => post<ReceiptDetail>('/receipts/or', body),
  cancel: (id: number, reasonCode: string, reasonText?: string) =>
    post<ReceiptAction>(`/receipts/${id}/cancel`, { reasonCode, reasonText }),
  reinstate: (id: number, body: ReinstateBody) =>
    post<ReceiptAction>(`/receipts/${id}/reinstate`, body),
  receiptPdf: (id: number) => api.getFile(`${BASE}/receipts/${id}/pdf`),
  actions: (companyId: number, stage: string[], page = 0) =>
    get<PageResponse<ReceiptAction>>(
      `/receipt-actions${toQuery({ companyId, stage: stage.join(','), page })}`,
    ),
  approveAction: (id: number) => post<ReceiptAction>(`/receipt-actions/${id}/approve`),
  resubmitAction: (id: number) => post<ReceiptAction>(`/receipt-actions/${id}/resubmit`),

  preview: (companyId: number, references: string[], amount?: number, currency?: string) =>
    post<PaymentPreview>('/payments/preview', { companyId, references, amount, currency }),
  receive: (body: ReceivePaymentBody) => post<IntakeResult>('/payments', body),
  payments: (companyId: number, batchRef: string, page = 0) =>
    get<PageResponse<Payment>>(`/payments${toQuery({ companyId, batchRef, page, size: 100 })}`),

  prebooked: (companyId: number, status: string, page = 0) =>
    get<PageResponse<Prebooked>>(`/prebooked${toQuery({ companyId, status, page })}`),
  rematch: (id: number) => post<Prebooked>(`/prebooked/${id}/rematch`),
  releasePrebooked: (id: number, reason: string) =>
    post<Prebooked>(`/prebooked/${id}/release`, { reason }),
  runMatching: () => post<Counts>('/matching/run'),

  unapplied: (companyId: number, tab: UnappliedTab, q: string, page = 0) =>
    get<PageResponse<UnappliedItem>>(`/unapplied${toQuery({ companyId, tab, q, page })}`),
  unappliedItem: (id: number) => get<UnappliedItem>(`/unapplied/${id}`),
  dispositions: (id: number) => get<Disposition[]>(`/unapplied/${id}/dispositions`),
  dispositionTypes: () => get<DispositionType[]>('/disposition-types'),
  assign: (id: number, body: DispositionBody) =>
    post<Disposition>(`/unapplied/${id}/disposition`, body),
  updateDisposition: (id: number, body: DispositionBody) =>
    api.put<Disposition>(`${BASE}/unapplied/${id}/disposition`, body),
  submit: (id: number) => post<Disposition>(`/unapplied/${id}/submit`),
  approve: (id: number) => post<Disposition>(`/unapplied/${id}/approve`),
  withdraw: (id: number) => post<UnappliedItem>(`/unapplied/${id}/withdraw`),
  markReversal: (id: number, reason: string) =>
    post<Disposition>(`/unapplied/${id}/reversal`, { reason }),
  approveReversal: (id: number) => post<Disposition>(`/unapplied/${id}/reversal/approve`),
  bulkSubmit: (ids: number[]) => post<BulkResult>('/unapplied/bulk/submit', { ids }),
  bulkApprove: (ids: number[]) => post<BulkResult>('/unapplied/bulk/approve', { ids }),

  pdcs: (companyId: number, status: PdcStatus | '', page = 0) =>
    get<PageResponse<PdcItem>>(`/pdc${toQuery({ companyId, status, page, size: 200 })}`),
  warehouse: (body: PdcBody) => post<PdcItem>('/pdc', body),
  releasePdc: (id: number, outcome: PdcStatus, reason: string) =>
    post<PdcItem>(`/pdc/${id}/release`, { outcome, reason }),
  matureNow: () => post<Counts>('/pdc/mature'),

  pickups: (companyId: number, status: PickupStatus, from?: string, to?: string, page = 0) =>
    get<PageResponse<Pickup>>(`/pickups${toQuery({ companyId, status, from, to, page })}`),
  importPickups: (companyId: number) => post<Counts>(`/pickups/import${toQuery({ companyId })}`),
  printPickups: (companyId: number, ids: number[]) =>
    post<PrintBatch>('/pickups/print', { companyId, ids, criteria: 'Check pick-up' }),
  cancelPickup: (id: number) => post<Pickup>(`/pickups/${id}/cancel`),

  printBatches: (companyId: number, page = 0) =>
    get<PageResponse<PrintBatch>>(`/print-batches${toQuery({ companyId, page })}`),
  printBatch: (id: number) => get<PrintBatch>(`/print-batches/${id}`),
  print: (companyId: number, ids: number[], criteria: string) =>
    post<PrintBatch>('/print-batches', { companyId, ids, criteria }),
  retryPrint: (id: number) => post<PrintBatch>(`/print-batches/${id}/retry`),
  printFile: (id: number) => api.getFile(`${BASE}/print-batches/${id}/file`),

  cwtTags: (companyId: number, stage: string[], page = 0) =>
    get<PageResponse<CwtTag>>(`/cwt${toQuery({ companyId, stage: stage.join(','), page })}`),
  cwtExpected: (invoiceNo: string) => get<CwtExpected>(`/cwt/expected${toQuery({ invoiceNo })}`),
  tagCwt: (body: CwtTagBody) => post<CwtTag>('/cwt', body),
  receiveCwt: (id: number, copyReceived: boolean) =>
    post<CwtTag>(`/cwt/${id}/receive${toQuery({ copyReceived })}`),
  checklist: (id: number, copyReceived: boolean) =>
    post<CwtTag>(`/cwt/${id}/checklist${toQuery({ copyReceived })}`),
  settleCash: (id: number, branchId: number) =>
    post<CwtTag>(`/cwt/${id}/settle-cash${toQuery({ branchId })}`),
  cwtBatches: (companyId: number) => get<CwtBatch[]>(`/cwt/batches${toQuery({ companyId })}`),
  validateCwt: (companyId: number, tagIds: number[]) =>
    post<CwtBatch>('/cwt/batches', { companyId, tagIds }),
  routeCwt: (id: number) => post<CwtBatch>(`/cwt/batches/${id}/route`),
  releaseCwt: (id: number) => post<CwtBatch>(`/cwt/batches/${id}/release`),

  series: (companyId: number) => get<Series[]>(`/series${toQuery({ companyId })}`),
  createSeries: (body: SeriesBody) => post<Series>('/series', body),
  updateSeries: (id: number, atpNo: string | undefined, toNo: number, warnAt: number) =>
    api.put<Series>(`${BASE}/series/${id}`, { atpNo, toNo, warnAt }),
  authorizeSeries: (id: number) => post<Series>(`/series/${id}/authorize`),
  deactivateSeries: (id: number) => post<Series>(`/series/${id}/deactivate`),

  layouts: () => get<Layout[]>('/layouts'),
  changeLayout: (code: string, kind: string, delimiter?: string, fields?: string) =>
    api.put<Layout>(`${BASE}/layouts/${code}`, { kind, delimiter, fields }),
  minimalRules: () => get<MinimalBalanceRule[]>('/minimal-balance/rules'),
  sweep: () => post<{ premium: number; excess: number }>('/minimal-balance/sweep'),
  commissionLines: (companyId: number) =>
    get<CommissionLine[]>(`/commission-payments${toQuery({ companyId })}`),
  issueCommissionOrs: (companyId: number) =>
    post<string[]>(`/commission-payments/issue${toQuery({ companyId })}`),
};
