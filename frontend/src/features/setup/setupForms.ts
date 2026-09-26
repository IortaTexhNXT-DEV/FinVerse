import type { CostCenterRuleInput, Employee, EmployeeInput, StatementLayout } from './frbsSetupApi';

/** Errors by field name. */
export type FieldErrors<T> = Partial<Record<keyof T, string>>;

const EMPLOYEE_NO = /^[A-Za-z0-9-]{1,20}$/;
const MAX_PRIORITY = 9999;

/**
 * Whether a text looks like an e-mail address (one "@", a dot in the domain, no spaces).
 *
 * @param text text
 * @returns true when plausible
 */
export function isEmail(text: string): boolean {
  const at = text.indexOf('@');
  return (
    at > 0 &&
    at === text.lastIndexOf('@') &&
    text.lastIndexOf('.') > at + 1 &&
    !text.endsWith('.') &&
    !/\s/.test(text)
  );
}

/**
 * An empty employee for the create form.
 *
 * @param companyId company
 * @param branchId default branch
 * @returns form values
 */
export function newEmployee(companyId: number, branchId: number): EmployeeInput {
  return { companyId, employeeNo: '', fullName: '', branchId, costCenter: '' };
}

/**
 * Form values of an existing employee.
 *
 * @param companyId company
 * @param e employee
 * @returns form values
 */
export function employeeInput(companyId: number, e: Employee): EmployeeInput {
  return {
    companyId,
    employeeNo: e.employeeNo,
    fullName: e.fullName,
    branchId: e.branchId,
    costCenter: e.costCenter,
    position: e.position,
    email: e.email,
    partyCode: e.partyCode,
    hiredOn: e.hiredOn,
    separatedOn: e.separatedOn,
  };
}

/**
 * Field errors of an employee (DIS 3.30.1).
 *
 * @param e employee
 * @returns errors
 */
export function employeeProblems(e: EmployeeInput): FieldErrors<EmployeeInput> {
  const errors: FieldErrors<EmployeeInput> = {};
  if (!EMPLOYEE_NO.test(e.employeeNo.trim())) {
    errors.employeeNo = 'Letters, digits and dashes, at most 20';
  }
  if (e.fullName.trim() === '') {
    errors.fullName = 'Enter the full name';
  }
  if (e.costCenter.trim() === '') {
    errors.costCenter = 'Select the cost centre';
  }
  if (e.email !== undefined && e.email !== '' && !isEmail(e.email)) {
    errors.email = 'Enter a valid e-mail address';
  }
  if (e.hiredOn && e.separatedOn && e.separatedOn < e.hiredOn) {
    errors.separatedOn = 'Must be on or after the hiring date';
  }
  return errors;
}

/**
 * Field errors of a cost-centre rule (FRBS 3.1.1).
 *
 * @param r rule
 * @returns errors
 */
export function ruleProblems(r: CostCenterRuleInput): FieldErrors<CostCenterRuleInput> {
  const errors: FieldErrors<CostCenterRuleInput> = {};
  if (!Number.isInteger(r.priority) || r.priority < 1 || r.priority > MAX_PRIORITY) {
    errors.priority = 'A whole number from 1 to 9999';
  }
  if (r.costCenter.trim() === '') {
    errors.costCenter = 'Select the cost centre';
  }
  return errors;
}

/**
 * Field errors of a statement layout (FRBS 3.3.1): a date column and either a signed amount or
 * both debit and credit columns.
 *
 * @param l layout
 * @returns errors
 */
export function layoutProblems(l: StatementLayout): FieldErrors<StatementLayout> {
  const errors: FieldErrors<StatementLayout> = {};
  if (l.bankAccountCode.trim() === '') {
    errors.bankAccountCode = 'Select the bank account';
  }
  if (l.name.trim() === '') {
    errors.name = 'Name the layout';
  }
  if (l.dateColumn.trim() === '') {
    errors.dateColumn = 'Enter the date column';
  }
  const hasAmount = (l.amountColumn ?? '').trim() !== '';
  const hasBoth = (l.debitColumn ?? '').trim() !== '' && (l.creditColumn ?? '').trim() !== '';
  if (!hasAmount && !hasBoth) {
    errors.amountColumn = 'Enter a signed amount column, or both debit and credit columns';
  }
  return errors;
}

/**
 * Describes the criteria of a rule for the list ("any" for blank criteria).
 *
 * @param r rule
 * @returns e.g. "FRBS · FRBS_SERVICE_FEE_ACCRUE · account 5614"
 */
export function ruleCriteria(r: Omit<CostCenterRuleInput, 'companyId'>): string {
  const parts = [
    r.sourceModule,
    r.eventType,
    r.accountCode === undefined || r.accountCode === '' ? undefined : `account ${r.accountCode}`,
    r.partyCode === undefined || r.partyCode === '' ? undefined : `party ${r.partyCode}`,
  ].filter((p) => p !== undefined && p !== '');
  return parts.length === 0 ? 'Any posting' : parts.join(' · ');
}
