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

/** A fiscal-year month against the same month one year earlier. */
export interface TrendPoint {
  month: string;
  current: number;
  priorYear: number;
}

export interface MonthlyValue {
  month: string;
  amount: number;
}

export interface LabelledAmount {
  label: string;
  amount: number;
}

export interface PremiumWidgetData {
  asOf: string;
  yearStart: string;
  monthToDate: number;
  monthToDatePriorYear: number;
  yearToDate: number;
  yearToDatePriorYear: number;
  monthly: TrendPoint[];
}

export interface ClaimsWidgetData {
  asOf: string;
  paidMonthToDate: number;
  paidYearToDate: number;
  outstanding: number;
  monthly: TrendPoint[];
}

export interface CollectionsWidgetData {
  asOf: string;
  collectedMonthToDate: number;
  collectedYearToDate: number;
  receivables: number;
  notYetDue: number;
  ageingSlots: string;
  ageing: LabelledAmount[];
  monthly: MonthlyValue[];
}

export interface PayablesWidgetData {
  asOf: string;
  overdue: number;
  dueIn7Days: number;
  dueIn30Days: number;
  total: number;
  openItems: number;
}

export interface CashWidgetData {
  asOf: string;
  total: number;
  accounts: LabelledAmount[];
  monthly: MonthlyValue[];
}

export interface BudgetLine {
  account: string;
  budgetToDate: number;
  actualToDate: number;
}

export interface BudgetWidgetData {
  fiscalYear: number;
  budgetVersion?: number | null;
  annualBudget: number;
  budgetToDate: number;
  actualToDate: number;
  utilizationPct?: number | null;
  lines: BudgetLine[];
}

export interface WorkloadWidgetData {
  openAlerts: number;
  pendingApprovals: number;
  approvalsByModule: Record<string, number>;
}

function widget<T>(name: string, companyId: number, branchId?: number): Promise<T> {
  return api.get<T>(`/dashboard/${name}${toQuery({ companyId, branchId })}`);
}

export const dashboardApi = {
  summary: (companyId: number, branchId?: number) =>
    api.get<DashboardSummary>(`/dashboard${toQuery({ companyId, branchId })}`),
  premium: (companyId: number, branchId?: number) =>
    widget<PremiumWidgetData>('premium', companyId, branchId),
  claims: (companyId: number, branchId?: number) =>
    widget<ClaimsWidgetData>('claims', companyId, branchId),
  collections: (companyId: number, branchId?: number) =>
    widget<CollectionsWidgetData>('collections', companyId, branchId),
  payables: (companyId: number, branchId?: number) =>
    widget<PayablesWidgetData>('payables', companyId, branchId),
  cash: (companyId: number, branchId?: number) =>
    widget<CashWidgetData>('cash', companyId, branchId),
  /** Budgets are company-wide (no branch). */
  budget: (companyId: number) => widget<BudgetWidgetData>('budget', companyId),
  workload: (companyId: number) => widget<WorkloadWidgetData>('workload', companyId),
};
