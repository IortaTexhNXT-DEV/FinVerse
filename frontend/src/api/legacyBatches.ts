import { api, toQuery } from './client';

/**
 * The batches of the legacy context after go-live: Cashiering reclassification of old unapplied
 * payments to other income, Cashiering reversal of legacy PR 2307 balances, and Commission
 * reversal of the premium receivable of legacy invoices paid directly to the insurer. Each batch is
 * prepared, submitted, approved by another user and then posted line by line.
 */

export type LegacyBatchStatus =
  'DRAFT' | 'FOR_APPROVAL' | 'FOR_TOP_MANAGEMENT' | 'EXECUTED' | 'CANCELLED';

export type CashBatchKind = 'INCOME_RECLASS' | 'PR2307_REVERSAL';

export interface LegacyBatch {
  batchNo: string;
  kind?: CashBatchKind;
  status: LegacyBatchStatus;
  reason: string;
  currency?: string;
  total: number;
  lineCount: number;
  createdBy: string;
  submittedAt?: string;
  firstApprovedBy?: string;
  finalApprovedBy?: string;
  approvedBy?: string;
  executedAt?: string;
  postedCount: number;
  failedCount: number;
  returnReason?: string;
}

export interface LegacyBatchLine {
  id: number;
  lineNo: number;
  unappliedId?: number;
  invoiceNo?: string;
  reference?: string;
  ledgerContext?: string;
  amount: number;
  ageDays?: number;
  reason?: string;
  status: 'PENDING' | 'POSTED' | 'FAILED';
  journalBatchNo?: string;
  message?: string;
}

export interface LegacyBatchDetail {
  batch: LegacyBatch;
  lines: LegacyBatchLine[];
}

/** An unapplied payment that may be taken to income. */
export interface UnappliedCandidate {
  id: number;
  reference: string;
  origin: string;
  ledgerContext: string;
  legacyArNo?: string;
  payorName?: string;
  clientCode?: string;
  currency: string;
  balance: number;
  ageDays: number;
  stage: string;
}

/** A legacy invoice whose direct payment premium may be reversed. */
export interface DpprCandidate {
  invoiceNo: string;
  legacyInvoiceNo?: string;
  sourceSystem?: string;
  policyNo?: string;
  clientCode: string;
  insurerCode: string;
  currency: string;
  openPremium: number;
  tagged: boolean;
}

/** The operations the batch screens need, common to the Cashiering and Commission batches. */
export interface LegacyBatchApi {
  list: (companyId: number) => Promise<LegacyBatch[]>;
  get: (batchNo: string) => Promise<LegacyBatchDetail>;
  create: (companyId: number, reason: string, currency: string) => Promise<LegacyBatch>;
  removeLine: (batchNo: string, lineId: number) => Promise<unknown>;
  submit: (batchNo: string) => Promise<LegacyBatch>;
  cancel: (batchNo: string) => Promise<LegacyBatch>;
  approve: (batchNo: string, comment: string) => Promise<LegacyBatch>;
  returnBatch: (batchNo: string, comment: string) => Promise<LegacyBatch>;
}

const CASH = '/cashiering/legacy-batches';
const DPPR = '/commission/dppr-batches';

function common(base: string): Omit<LegacyBatchApi, 'list' | 'create'> {
  return {
    get: (batchNo) => api.get<LegacyBatchDetail>(`${base}/${batchNo}`),
    removeLine: (batchNo, lineId) => api.delete(`${base}/${batchNo}/lines/${String(lineId)}`),
    submit: (batchNo) => api.post<LegacyBatch>(`${base}/${batchNo}/submit`),
    cancel: (batchNo) => api.post<LegacyBatch>(`${base}/${batchNo}/cancel`),
    approve: (batchNo, comment) => api.post<LegacyBatch>(`${base}/${batchNo}/approve`, { comment }),
    returnBatch: (batchNo, comment) =>
      api.post<LegacyBatch>(`${base}/${batchNo}/return`, { comment }),
  };
}

/** Cashiering legacy batches of a kind. */
export function cashBatchApi(kind: CashBatchKind): LegacyBatchApi {
  return {
    ...common(CASH),
    list: (companyId) => api.get<LegacyBatch[]>(`${CASH}${toQuery({ companyId, kind })}`),
    create: (companyId, reason, currency) =>
      api.post<LegacyBatch>(`${CASH}${toQuery({ companyId })}`, { kind, reason, currency }),
  };
}

/** Commission legacy direct payment PR reversal batches. */
export const dpprBatchApi: LegacyBatchApi = {
  ...common(DPPR),
  list: (companyId) => api.get<LegacyBatch[]>(`${DPPR}${toQuery({ companyId })}`),
  create: (companyId, reason) =>
    api.post<LegacyBatch>(`${DPPR}${toQuery({ companyId })}`, { comment: reason }),
};

export const legacyBatchLines = {
  unappliedCandidates: (companyId: number, minAgeDays: number, origin?: string) =>
    api.get<UnappliedCandidate[]>(
      `${CASH}/candidates${toQuery({ companyId, minAgeDays, origin })}`,
    ),
  addUnapplied: (batchNo: string, unappliedId: number, reason: string) =>
    api.post<LegacyBatchLine>(`${CASH}/${batchNo}/items`, { unappliedId, reason }),
  addPr2307: (batchNo: string, invoiceNo: string, amount: number, reason: string) =>
    api.post<LegacyBatchLine>(`${CASH}/${batchNo}/invoices`, { invoiceNo, amount, reason }),
  dpprCandidates: (companyId: number, taggedOnly: boolean) =>
    api.get<DpprCandidate[]>(`${DPPR}/candidates${toQuery({ companyId, taggedOnly })}`),
  addDppr: (batchNo: string, invoiceNo: string, reason: string) =>
    api.post<LegacyBatchLine>(`${DPPR}/${batchNo}/invoices`, { invoiceNo, reason }),
};
