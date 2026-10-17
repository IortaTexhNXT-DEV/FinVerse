import type {
  AccountPurpose,
  BankAccountInput,
  ContactInput,
  ContactRole,
  RemittanceCycle,
  RemittanceMode,
  RemittanceTerms,
} from '@/api/insurerPage';

const CYCLE_LABELS: Record<RemittanceCycle, string> = {
  DAILY: 'Daily',
  WEEKLY: 'Weekly',
  SEMI_MONTHLY: 'Twice a month',
  MONTHLY: 'Monthly',
};

const MODE_LABELS: Record<RemittanceMode, string> = {
  CHECK: 'Check',
  FUND_TRANSFER: 'Fund transfer',
  ONLINE_BANKING: 'Online banking',
};

const ROLE_LABELS: Record<ContactRole, string> = {
  PLACEMENT: 'Placement',
  CLAIMS: 'Claims',
  ACCOUNTING: 'Accounting',
  REMITTANCE: 'Remittance',
  RELATIONSHIP: 'Relationship manager',
  OTHER: 'Other',
};

const PURPOSE_LABELS: Record<AccountPurpose, string> = {
  REMITTANCE: 'Premium remittance',
  REFUND: 'Refunds',
  COMMISSION: 'Commissions',
};

const optionsOf = <T extends string>(labels: Record<T, string>) =>
  (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));

export const REMITTANCE_CYCLES = optionsOf(CYCLE_LABELS);
export const REMITTANCE_MODES = optionsOf(MODE_LABELS);
export const CONTACT_ROLES = optionsOf(ROLE_LABELS);
export const ACCOUNT_PURPOSES = optionsOf(PURPOSE_LABELS);

export const remittanceCycleLabel = (c: RemittanceCycle | null | undefined) =>
  c ? CYCLE_LABELS[c] : '';
export const remittanceModeLabel = (m: RemittanceMode | null | undefined) =>
  m ? MODE_LABELS[m] : '';
export const contactRoleLabel = (r: ContactRole) => ROLE_LABELS[r];
export const accountPurposeLabel = (p: AccountPurpose) => PURPOSE_LABELS[p];

const blankToNull = (value: string | null | undefined) =>
  value === undefined || value === null || value.trim() === '' ? null : value.trim();

/** The remittance request of the form: blanks are cleared. */
export function remittanceInputOf(form: RemittanceTerms): RemittanceTerms {
  return {
    paymentTerms: blankToNull(form.paymentTerms),
    cycle: form.cycle ?? null,
    mode: form.mode ?? null,
    email: blankToNull(form.email),
    currency: blankToNull(form.currency),
  };
}

/** The contact request of the form. */
export function contactInputOf(form: Partial<ContactInput>): ContactInput {
  return {
    role: form.role ?? 'OTHER',
    name: (form.name ?? '').trim(),
    position: blankToNull(form.position),
    email: blankToNull(form.email),
    phone: blankToNull(form.phone),
  };
}

/** The bank account request of the form. */
export function bankAccountInputOf(form: Partial<BankAccountInput>): BankAccountInput {
  return {
    purpose: form.purpose ?? 'REMITTANCE',
    bankName: (form.bankName ?? '').trim(),
    bankBranch: blankToNull(form.bankBranch),
    accountNo: (form.accountNo ?? '').trim(),
    accountName: (form.accountName ?? '').trim(),
    currency: (form.currency ?? '').trim(),
    primaryAccount: form.primaryAccount ?? false,
  };
}
