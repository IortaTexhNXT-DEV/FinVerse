import { api, toQuery } from './client';

export interface RevaluationItem {
  branchId: number;
  accountCode: string;
  accountName?: string;
  currency: string;
  fcBalance: number;
  bookedBase: number;
  closingRate: number;
  revaluedBase: number;
  difference: number;
  postable?: boolean;
  posted?: boolean;
}

export interface OpenItemRevaluation {
  partyCode: string;
  documentNo: string;
  direction: 'DEBIT' | 'CREDIT';
  currency: string;
  outstanding: number;
  bookedBase: number;
  closingRate: number;
  revaluedBase: number;
  gainLoss: number;
}

export interface FxPreview {
  periodId: number;
  periodName: string;
  revaluationDate: string;
  items: RevaluationItem[];
  missingRates: string[];
  openItems: OpenItemRevaluation[];
  existingRunId?: number;
  totalDifference: number;
}

export interface FxRun {
  id: number;
  companyId: number;
  periodId: number;
  periodName: string;
  revaluationDate: string;
  gainLossAccount: string;
  autoReverse: boolean;
  reversalDate?: string;
  status: 'POSTED' | 'REVERSED';
  journalBatchNo?: string;
  reversalBatchNo?: string;
  reversalPending: boolean;
  totalGain: number;
  totalLoss: number;
  createdBy: string;
  lines?: RevaluationItem[];
}

export interface CheckItem {
  code: string;
  label: string;
  passed: boolean;
  detail: string;
  blocking: boolean;
}

export interface Checklist {
  ready: boolean;
  items: CheckItem[];
}

export interface YearEndClose {
  id: number;
  fiscalYearId: number;
  yearCode: number;
  closingDate: string;
  netResult: number;
  retainedEarningsAccount: string;
  closingBatches: string;
  nextYearCode?: number;
  closedBy: string;
  closedAt: string;
}

export interface ClosingBalance {
  branchId: number;
  accountCode: string;
  costCenter?: string;
  businessLine?: string;
  netDebit: number;
}

export const closingApi = {
  fxRuns: (companyId: number) =>
    api.get<FxRun[]>(`/closing/fx-revaluations${toQuery({ companyId })}`),
  fxPreview: (companyId: number, periodId: number) =>
    api.get<FxPreview>(`/closing/fx-revaluations/preview${toQuery({ companyId, periodId })}`),
  fxPost: (companyId: number, periodId: number, autoReverse: boolean) =>
    api.post<FxRun>('/closing/fx-revaluations', { companyId, periodId, autoReverse }),
  periodChecklist: (companyId: number, periodId: number) =>
    api.get<Checklist>(`/closing/period-end/checklist${toQuery({ companyId, periodId })}`),
  yearChecklist: (companyId: number, fiscalYearId: number) =>
    api.get<Checklist>(`/closing/year-end/checklist${toQuery({ companyId, fiscalYearId })}`),
  yearPreview: (companyId: number, fiscalYearId: number) =>
    api.get<ClosingBalance[]>(`/closing/year-end/preview${toQuery({ companyId, fiscalYearId })}`),
  closeRecord: (fiscalYearId: number) =>
    api.get<YearEndClose | undefined>(`/closing/year-end/close${toQuery({ fiscalYearId })}`),
  closeYear: (companyId: number, fiscalYearId: number) =>
    api.post<YearEndClose>('/closing/year-end/close', { companyId, fiscalYearId }),
};
