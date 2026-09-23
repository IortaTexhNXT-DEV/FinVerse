import { api, ApiError, tokenStore, toQuery } from './client';
import type { ProblemDetail } from './client';

export type BudgetStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'SUPERSEDED';
export type BudgetVersionType = 'ORIGINAL' | 'REVISED';

export interface BudgetLine {
  accountCode: string;
  costCenter?: string;
  months: number[];
  annual?: number;
}

export interface Budget {
  id: number;
  companyId: number;
  fiscalYear: number;
  versionNo: number;
  versionType: BudgetVersionType;
  name: string;
  currency: string;
  status: BudgetStatus;
  basedOnId?: number;
  total: number;
  lineCount: number;
  createdBy: string;
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectionReason?: string;
  lines?: BudgetLine[];
}

export interface CreateBudgetInput {
  companyId: number;
  fiscalYear: number;
  versionType: BudgetVersionType;
  name: string;
  copyFromId?: number;
}

export interface VarianceLine {
  accountCode: string;
  accountName: string;
  accountClass: 'INCOME' | 'EXPENSE';
  costCenter?: string;
  budgetMonth: number;
  actualMonth: number;
  monthVariance: number;
  monthVariancePct?: number;
  budgetYtd: number;
  actualYtd: number;
  ytdVariance: number;
  ytdVariancePct?: number;
  annualBudget: number;
  utilizationPct?: number;
  available: number;
  favourable: boolean;
}

export interface BudgetComparison {
  fiscalYear: number;
  periodNo: number;
  budgetId?: number;
  budgetVersion?: number;
  lines: VarianceLine[];
}

const BASE = '/budgets';

/** Uploads CSV text (the shared client only sends JSON). */
async function importCsv(id: number, csv: string): Promise<Budget> {
  const token = tokenStore.get();
  const response = await fetch(`/api/v1${BASE}/${id}/import`, {
    method: 'POST',
    headers: {
      'Content-Type': 'text/plain',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: csv,
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ProblemDetail;
    throw new ApiError(response.status, problem);
  }
  return (await response.json()) as Budget;
}

export const budgetApi = {
  list: (companyId: number, fiscalYear?: number) =>
    api.get<Budget[]>(`${BASE}${toQuery({ companyId, fiscalYear })}`),
  get: (id: number) => api.get<Budget>(`${BASE}/${id}`),
  create: (body: CreateBudgetInput) => api.post<Budget>(BASE, body),
  saveLines: (id: number, lines: BudgetLine[]) => api.put<Budget>(`${BASE}/${id}/lines`, lines),
  importCsv,
  copyActuals: (id: number, sourceYear: number, adjustmentPercent: number) =>
    api.post<Budget>(`${BASE}/${id}/copy-actuals`, { sourceYear, adjustmentPercent }),
  submit: (id: number) => api.post<Budget>(`${BASE}/${id}/submit`),
  approve: (id: number) => api.post<Budget>(`${BASE}/${id}/approve`),
  reject: (id: number, reason: string) => api.post<Budget>(`${BASE}/${id}/reject`, { reason }),
  variance: (companyId: number, asOf: string, byCostCenter: boolean) =>
    api.get<BudgetComparison>(`${BASE}/variance${toQuery({ companyId, asOf, byCostCenter })}`),
  alerts: (companyId: number, asOf: string, threshold: number) =>
    api.get<VarianceLine[]>(`${BASE}/alerts${toQuery({ companyId, asOf, threshold })}`),
};
