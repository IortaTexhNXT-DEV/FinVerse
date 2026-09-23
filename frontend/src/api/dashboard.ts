import { api, toQuery } from './client';

export interface MonthlyPoint {
  month: string;
  income: number;
  expense: number;
  netResult: number;
}

export interface CompositionItem {
  label: string;
  amount: number;
}

export interface DashboardSummary {
  asOf: string;
  fiscalYearStart: string;
  totalIncomeYtd: number;
  totalExpenseYtd: number;
  netResultYtd: number;
  cashPosition: number;
  receivables: number;
  technicalReserves: number;
  totalAssets: number;
  totalEquity: number;
  pendingJournals: number;
  draftJournals: number;
  monthly: MonthlyPoint[];
  incomeComposition: CompositionItem[];
  expenseComposition: CompositionItem[];
}

export const dashboardApi = {
  summary: (companyId: number, branchId?: number) =>
    api.get<DashboardSummary>(`/dashboard${toQuery({ companyId, branchId })}`),
};
