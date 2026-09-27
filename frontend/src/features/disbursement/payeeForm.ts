import type { Column } from '@/components/ui/DataTable';
import type { AccountInput, Mode, Payee, PayeeAccount, PayeeInput } from './api';
import { CREDIT_MODES, MODE_LABELS } from './labels';

/** The editable fields of a payee (DIS 2.2.2-2.2.3). */
export interface PayeeForm {
  payeeCode: string;
  payeeClass: string;
  name: string;
  address: string;
  email: string;
  tin: string;
  defaultMode: Mode;
  allowedModes: Mode[];
  disbursementTypes: string[];
  currency: string;
  defaultCostCenter: string;
  remarks: string;
}

export const EMPTY_PAYEE: PayeeForm = {
  payeeCode: '',
  payeeClass: '',
  name: '',
  address: '',
  email: '',
  tin: '',
  defaultMode: 'CHECK',
  allowedModes: ['CHECK'],
  disbursementTypes: [],
  currency: 'PHP',
  defaultCostCenter: '',
  remarks: '',
};

/** The form of a maintained payee. */
export function payeeFormOf(p: Payee): PayeeForm {
  return {
    payeeCode: p.summary.payeeCode,
    payeeClass: p.summary.payeeClass,
    name: p.summary.name,
    address: p.address ?? '',
    email: p.email ?? '',
    tin: p.tin ?? '',
    defaultMode: p.summary.defaultMode,
    allowedModes: p.allowedModes,
    disbursementTypes: p.disbursementTypes,
    currency: p.summary.currency,
    defaultCostCenter: p.defaultCostCenter ?? '',
    remarks: p.remarks ?? '',
  };
}

const optional = (value: string) => (value.trim() === '' ? undefined : value.trim());

/** The request body of the form; the default mode is always one of the allowed modes. */
export function payeeInputOf(
  form: PayeeForm,
  companyId: number,
  accounts?: AccountInput[],
): PayeeInput {
  const allowed = form.allowedModes.includes(form.defaultMode)
    ? form.allowedModes
    : [...form.allowedModes, form.defaultMode];
  return {
    companyId,
    payeeCode: optional(form.payeeCode),
    payeeClass: form.payeeClass,
    name: form.name.trim(),
    address: optional(form.address),
    email: optional(form.email),
    tin: optional(form.tin),
    defaultMode: form.defaultMode,
    allowedModes: allowed,
    disbursementTypes: form.disbursementTypes,
    currency: form.currency,
    defaultCostCenter: optional(form.defaultCostCenter),
    remarks: optional(form.remarks),
    accounts,
  };
}

/** A plausible e-mail: one @, a local part and a dotted domain, no spaces. */
export function isEmail(value: string): boolean {
  const parts = value.split('@');
  const domain = parts[1] ?? '';
  return (
    parts.length === 2 &&
    parts[0] !== '' &&
    !value.includes(' ') &&
    domain.includes('.') &&
    !domain.startsWith('.') &&
    !domain.endsWith('.')
  );
}

/** Field errors of the payee form (DIS 2.2.2). */
export function payeeErrors(form: PayeeForm, hasCreditAccount: boolean): Record<string, string> {
  const errors: Record<string, string> = {};
  if (form.payeeCode.trim() === '') {
    errors.payeeCode = 'Enter the payee (party) code';
  }
  if (form.payeeClass === '') {
    errors.payeeClass = 'Select the payee class';
  }
  if (form.name.trim() === '') {
    errors.name = 'Enter the payee name';
  }
  if (!/^[A-Z]{3}$/.test(form.currency)) {
    errors.currency = 'Enter a 3-letter currency';
  }
  if (form.email.trim() !== '' && !isEmail(form.email.trim())) {
    errors.email = 'Enter a valid e-mail';
  }
  if (CREDIT_MODES.includes(form.defaultMode) && !hasCreditAccount) {
    errors.defaultMode = 'A credit mode needs a bank account of the payee';
  }
  return errors;
}

/** Field errors of a payee bank account. */
export function accountErrors(a: AccountInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (a.bankName.trim() === '') {
    errors.bankName = 'Enter the bank';
  }
  if (!/^[0-9-]{6,40}$/.test(a.accountNo.trim())) {
    errors.accountNo = 'Enter the account number (digits)';
  }
  if (a.accountName.trim() === '') {
    errors.accountName = 'Enter the account name';
  }
  return errors;
}

export const ACCOUNT_COLUMNS: Column<PayeeAccount>[] = [
  {
    key: 'bank',
    header: 'Bank',
    render: (a) => (a.bankBranch ? `${a.bankName} · ${a.bankBranch}` : a.bankName),
  },
  { key: 'no', header: 'Account No.', render: (a) => a.accountNo },
  { key: 'name', header: 'Account Name', render: (a) => a.accountName },
  { key: 'ccy', header: 'Currency', render: (a) => a.currency },
  { key: 'mode', header: 'Mode', render: (a) => MODE_LABELS[a.mode] },
  { key: 'primary', header: 'Primary', render: (a) => (a.primary ? 'Yes' : '') },
  { key: 'active', header: 'Active', render: (a) => (a.active ? 'Yes' : 'No') },
];

export const EMPTY_ACCOUNT: AccountInput = {
  bankName: '',
  bankBranch: '',
  accountNo: '',
  accountName: '',
  currency: 'PHP',
  mode: 'CTA',
  primary: true,
};
