import type { InstallmentStatus, PlanSource, PlanStatus, PromiseStatus } from './api';

/** Labels, work list tabs and form checks of the plans and promises screens (BRCLXN.053/055). */

export type PlanTab = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export const PLAN_TABS: readonly { id: PlanTab; label: string; statuses: PlanStatus[] }[] = [
  { id: 'ACTIVE', label: 'Active Plans', statuses: ['ACTIVE'] },
  { id: 'COMPLETED', label: 'Completed', statuses: ['COMPLETED'] },
  { id: 'CANCELLED', label: 'Cancelled', statuses: ['CANCELLED'] },
];

export type PromiseTab = 'OPEN' | 'BROKEN' | 'KEPT' | 'WITHDRAWN';

export const PROMISE_TABS: readonly { id: PromiseTab; label: string; statuses: PromiseStatus[] }[] =
  [
    { id: 'OPEN', label: 'Open Promises', statuses: ['OPEN'] },
    { id: 'BROKEN', label: 'Broken', statuses: ['BROKEN'] },
    { id: 'KEPT', label: 'Kept', statuses: ['KEPT', 'PARTIALLY_KEPT'] },
    { id: 'WITHDRAWN', label: 'Withdrawn', statuses: ['CANCELLED'] },
  ];

export const SOURCE_LABELS: Record<PlanSource, string> = {
  POLICY_YEARS: 'Policy Years',
  GENERATED: 'Generated',
  MANUAL: 'Entered',
};

/** Installment statuses that still need follow-up. */
export const OPEN_INSTALLMENT: readonly InstallmentStatus[] = [
  'NOT_DUE',
  'DUE',
  'OVERDUE',
  'PARTIAL',
];

/** The statuses behind a tab. */
export function statusesOf<T extends string, S>(
  tabs: readonly { id: T; statuses: S[] }[],
  tab: T,
): S[] {
  return tabs.find((t) => t.id === tab)?.statuses ?? [];
}

/** Invoice numbers typed or pasted in a box: one per line or separated by commas, no repeats. */
export function parseInvoiceList(text: string): string[] {
  const numbers = text
    .split(/[\s,;]+/)
    .map((n) => n.trim())
    .filter((n) => n.length > 0);
  return [...new Set(numbers)];
}

export interface PlanForm {
  kind: 'POLICY_YEARS' | 'GENERATED';
  arn: string;
  invoiceNo: string;
  frequency: string;
  firstDue: string;
  count: string;
}

/** Field errors of the new plan form. */
export function planFormErrors(form: PlanForm): Partial<Record<keyof PlanForm, string>> {
  const errors: Partial<Record<keyof PlanForm, string>> = {};
  if (form.frequency === '') {
    errors.frequency = 'Choose the billing frequency';
  }
  if (form.kind === 'POLICY_YEARS') {
    if (form.arn.trim() === '') {
      errors.arn = 'Enter the account reference number';
    }
    return errors;
  }
  if (form.invoiceNo.trim() === '') {
    errors.invoiceNo = 'Enter the invoice number';
  }
  if (form.firstDue === '') {
    errors.firstDue = 'Enter the first due date';
  }
  const count = Number(form.count);
  if (!Number.isInteger(count) || count < 1 || count > 120) {
    errors.count = 'Enter between 1 and 120 installments';
  }
  return errors;
}

export interface PromiseForm {
  invoiceNo: string;
  promisedOn: string;
  promisedDate: string;
  amount: string;
}

/** Field errors of a promise to pay (dates as yyyy-mm-dd). */
export function promiseFormErrors(
  form: PromiseForm,
  today: string,
): Partial<Record<keyof PromiseForm, string>> {
  const errors: Partial<Record<keyof PromiseForm, string>> = {};
  if (form.invoiceNo.trim() === '') {
    errors.invoiceNo = 'Enter at least one invoice number';
  }
  if (form.promisedOn !== '' && form.promisedOn > today) {
    errors.promisedOn = 'The day of the promise cannot be in the future';
  }
  if (form.promisedDate === '') {
    errors.promisedDate = 'Enter the promised payment date';
  } else if (form.promisedOn !== '' && form.promisedDate < form.promisedOn) {
    errors.promisedDate = 'The promised date cannot be before the day of the promise';
  }
  const amount = Number(form.amount);
  if (form.amount !== '' && (Number.isNaN(amount) || amount <= 0)) {
    errors.amount = 'Enter a positive amount, or leave it empty for the whole outstanding';
  }
  return errors;
}

/** An optional amount typed in a form. */
export function optionalAmount(text: string): number | undefined {
  return text.trim() === '' ? undefined : Number(text);
}

/** An optional text typed in a form. */
export function optionalText(text: string): string | undefined {
  const t = text.trim();
  return t === '' ? undefined : t;
}
