import { api, toQuery } from '@/api/client';
import type { ExchangeRate } from '@/api/masters';
import type { PageResponse } from '@/api/types';

/** Employee with cost centre (DIS 3.30.1). */
export interface Employee {
  id: number;
  employeeNo: string;
  fullName: string;
  branchId: number;
  costCenter: string;
  position?: string;
  email?: string;
  partyCode?: string;
  hiredOn?: string;
  separatedOn?: string;
  active: boolean;
}

/** Employee create / update. */
export interface EmployeeInput {
  companyId: number;
  employeeNo: string;
  fullName: string;
  branchId: number;
  costCenter: string;
  position?: string;
  email?: string;
  partyCode?: string;
  hiredOn?: string;
  separatedOn?: string;
}

/** Cost-centre rule of the accounting engine (FRBS 3.1.1). */
export interface CostCenterRule {
  id: number;
  priority: number;
  sourceModule?: string;
  eventType?: string;
  branchId?: number;
  partyCode?: string;
  accountCode?: string;
  costCenter: string;
  description?: string;
  active: boolean;
  updatedBy: string;
  updatedAt: string;
}

/** Cost-centre rule create / update. */
export type CostCenterRuleInput = Omit<CostCenterRule, 'id' | 'updatedBy' | 'updatedAt'> & {
  companyId: number;
};

/** Spreadsheet statement layout of a bank account (FRBS 3.3.1). */
export interface StatementLayout {
  id?: number;
  bankAccountCode: string;
  name: string;
  dateColumn: string;
  descriptionColumn?: string;
  referenceColumn?: string;
  debitColumn?: string;
  creditColumn?: string;
  amountColumn?: string;
  balanceColumn?: string;
  datePattern: string;
  chequeNumberFirst: boolean;
}

/** Outcome of a statement file import. */
export interface StatementImport {
  statement: { statementRef: string; lineCount: number; closingBalance: number };
  matched: number;
}

/** Setup screens of BRD-5: employees, cost-centre rules, statement layouts, revaluation rates. */
export const frbsSetupApi = {
  employees: (companyId: number, q: string, page: number) =>
    api.get<PageResponse<Employee>>(
      `/organization/employees${toQuery({ companyId, q: q || undefined, page, size: 25 })}`,
    ),
  createEmployee: (body: EmployeeInput) => api.post<Employee>('/organization/employees', body),
  updateEmployee: (id: number, body: EmployeeInput) =>
    api.put<Employee>(`/organization/employees/${id}`, body),

  costCenterRules: (companyId: number) =>
    api.get<CostCenterRule[]>(`/accounting/cost-center-rules${toQuery({ companyId })}`),
  createRule: (body: CostCenterRuleInput) =>
    api.post<CostCenterRule>('/accounting/cost-center-rules', body),
  updateRule: (id: number, body: CostCenterRuleInput) =>
    api.put<CostCenterRule>(`/accounting/cost-center-rules/${id}`, body),

  layouts: (companyId: number) =>
    api.get<StatementLayout[]>(`/receivables/bank-rec/layouts${toQuery({ companyId })}`),
  saveLayout: (companyId: number, body: StatementLayout) =>
    api.put<StatementLayout>('/receivables/bank-rec/layouts', { ...body, companyId }),
  importStatement: (companyId: number, bankAccountCode: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<StatementImport>(
      `/receivables/bank-rec/statements/file${toQuery({ companyId, bankAccountCode })}`,
      form,
    );
  },

  revaluationRates: (year: number) =>
    api.get<ExchangeRate[]>(`/currencies/revaluation-rates${toQuery({ year })}`),
  saveRevaluationRate: (currencyCode: string, month: string, rate: number) =>
    api.post<ExchangeRate>('/currencies/revaluation-rates', { currencyCode, month, rate }),
  copyToBook: (month: string) =>
    api.post<ExchangeRate[]>(`/currencies/revaluation-rates/${month}/copy-to-book`),
};
