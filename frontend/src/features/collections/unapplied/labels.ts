import { formatAmount, formatDate, humanize } from '@/utils/format';
import type {
  CashieringAction,
  CashieringTab,
  DispositionRule,
  RequestStatus,
  UnappliedFilters,
  UnappliedRow,
} from './api';

/** Tabs, labels and form checks of the unapplied-payment screens (BRCLXN.030-048). */

export type UnappliedTab = 'ALL' | CashieringTab;

export const UNAPPLIED_TABS: readonly { id: UnappliedTab; label: string }[] = [
  { id: 'UNAPPLIED', label: 'Awaiting Disposition' },
  { id: 'MONITORING', label: 'In Cashiering' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'FOR_REVERSAL', label: 'For Reversal' },
  { id: 'ALL', label: 'All Open' },
];

export type RequestTab = 'OPEN' | 'APPLIED' | 'REJECTED' | 'ALL';

export const REQUEST_TABS: readonly { id: RequestTab; label: string; statuses: RequestStatus[] }[] =
  [
    { id: 'OPEN', label: 'Open', statuses: ['SENT', 'DEFERRED', 'ACCEPTED'] },
    { id: 'APPLIED', label: 'Executed', statuses: ['APPLIED'] },
    { id: 'REJECTED', label: 'Rejected', statuses: ['REJECTED'] },
    { id: 'ALL', label: 'All Requests', statuses: [] },
  ];

export const ACTION_LABELS: Record<CashieringAction, string> = {
  APPLY_TO_INVOICE: 'Apply to Invoice',
  REFUND: 'Refund',
  RECLASS: 'Reclass',
  TRANSFER: 'Transfer to Unit',
  NONE: 'Note Only',
};

export const TAB_LABELS: Record<CashieringTab, string> = {
  UNAPPLIED: 'Unapplied',
  MONITORING: 'Monitoring',
  FOR_APPROVAL: 'For Approval',
  FOR_REVERSAL: 'For Reversal',
  DONE: 'Done',
};

/** The statuses behind a request tab (empty = every status). */
export function requestStatuses(tab: RequestTab): RequestStatus[] {
  return REQUEST_TABS.find((t) => t.id === tab)?.statuses ?? [];
}

/** The Cashiering status of an item as a pill: a disposition status or a request status. */
export function cashieringStatus(status: string | undefined): string | undefined {
  if (status === undefined) {
    return undefined;
  }
  return status.startsWith('REQUEST_') ? status.substring('REQUEST_'.length) : status;
}

export interface PanelValues {
  segment: string;
  disposition: string;
  age: string;
}

export const EMPTY_PANEL: PanelValues = { segment: '', disposition: '', age: '' };

/** Age buckets of the Filters panel (BRCLXN.035). */
export const AGE_BUCKETS: readonly { id: string; label: string; min?: number; max?: number }[] = [
  { id: '', label: 'Any age' },
  { id: '0-7', label: 'Up to 7 days', max: 7 },
  { id: '8-30', label: '8 to 30 days', min: 8, max: 30 },
  { id: '31-90', label: '31 to 90 days', min: 31, max: 90 },
  { id: '91+', label: 'Over 90 days', min: 91 },
];

/** The list filters of a tab, search text and panel values. */
export function listFilters(tab: UnappliedTab, q: string, panel: PanelValues): UnappliedFilters {
  const bucket = AGE_BUCKETS.find((b) => b.id === panel.age);
  return {
    q: q || undefined,
    tab: tab === 'ALL' ? undefined : tab,
    segment: panel.segment || undefined,
    disposition: panel.disposition || undefined,
    ageMin: bucket?.min,
    ageMax: bucket?.max,
  };
}

export interface DispositionForm {
  dispositionCode: string;
  invoiceNo: string;
  amount: string;
  remarks: string;
}

export type DispositionErrors = Partial<Record<keyof DispositionForm, string>>;

/**
 * Checks the disposition form (BRCLXN.047/048): a value is chosen; the invoice number is
 * mandatory and in the parameter's format when the value requires one; the amount, when given, is
 * above zero and at most the balance.
 */
export function dispositionErrors(
  form: DispositionForm,
  rule: DispositionRule | undefined,
  pattern: string,
  balance: number,
): DispositionErrors {
  const errors: DispositionErrors = {};
  if (rule === undefined) {
    errors.dispositionCode = 'Choose the disposition';
  }
  const invoice = form.invoiceNo.trim();
  if (rule?.requiresInvoice === true && invoice === '') {
    errors.invoiceNo = 'Enter the invoice number to apply the payment to';
  } else if (invoice !== '' && !matches(pattern, invoice)) {
    errors.invoiceNo = 'The invoice number does not have the expected format';
  }
  const amount = form.amount.trim();
  if (amount !== '') {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0 || value > balance) {
      errors.amount = `Enter an amount above zero and at most ${balance.toFixed(2)}`;
    }
  }
  return errors;
}

function matches(pattern: string, value: string): boolean {
  try {
    return new RegExp(pattern).test(value);
  } catch {
    return true;
  }
}

/** The body of a disposition from the form. */
export function toDraft(form: DispositionForm) {
  const amount = form.amount.trim();
  return {
    dispositionCode: form.dispositionCode,
    invoiceNo: form.invoiceNo.trim() || undefined,
    amount: amount === '' ? undefined : Number(amount),
    remarks: form.remarks.trim() || undefined,
  };
}

function mapped<T>(value: T | undefined, format: (v: T) => string): string | undefined {
  return value === undefined ? undefined : format(value);
}

/** The payment and account fields of an item (BRCLXN.036), as label and value. */
export function paymentFacts(row: UnappliedRow): [string, string | undefined][] {
  const a = row.account;
  return [
    ['Payment File', row.paymentFileName],
    ['Transaction No.', row.transactionNo],
    ['Payment Type', mapped(row.paymentType, humanize)],
    ['Bank', row.bankCode],
    ['Check No.', row.checkNo],
    ['Payor Reference', row.reference],
    ['Matched Client', row.clientCode],
    ['Matched Invoice', row.invoiceNo],
    ['Name of Assured', a?.assuredName],
    ['PR Balance', mapped(a?.prBalance, formatAmount)],
    ['Inception', mapped(a?.inceptionDate, formatDate)],
    ['Market Segment', a?.segment],
    ['Sales Unit', a?.salesUnit ?? row.salesUnit],
    ['Unit Head', a?.unitHead],
    ['Account Officer', a?.aoUsername],
    ['Insurer', a?.insurerCode],
    ['Invoice Category', mapped(a?.invoiceCategory, humanize)],
    ['Collection Handler', a?.handler],
  ];
}
