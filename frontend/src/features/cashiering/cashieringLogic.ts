import type { LedgerComponent } from '@/api/operations';
import { humanize } from '@/utils/format';
import type {
  Allocation,
  DispositionBody,
  PaymentPreview,
  PdcItem,
  ReceiptDetail,
  ReinstateBody,
  Series,
} from './cashieringApi';

/** The payment application hierarchy (CSHID.022): DST, VAT / premium tax, LGT, other, basic. */
export const APPLICATION_ORDER: readonly LedgerComponent[] = [
  'DST',
  'PREMIUM_TAX_VAT',
  'LGT',
  'FST',
  'OTHER',
  'BASIC',
];

const COMPONENT_LABELS: Partial<Record<LedgerComponent, string>> = {
  DST: 'DST',
  PREMIUM_TAX_VAT: 'VAT / Premium Tax',
  LGT: 'LGT',
  FST: 'FST',
  OTHER: 'Other Charges',
  BASIC: 'Basic Premium',
};

/** Display label of a premium component. */
export function componentLabel(c: LedgerComponent): string {
  return COMPONENT_LABELS[c] ?? c;
}

/** The components of an allocation in hierarchy order, zero amounts left out. */
export function allocationRows(
  allocation: Allocation,
): { component: LedgerComponent; amount: number }[] {
  return APPLICATION_ORDER.flatMap((component) => {
    const amount = allocation[component] ?? 0;
    return amount === 0 ? [] : [{ component, amount }];
  });
}

/** Splits the references typed by the cashier (comma, space or line separated). */
export function parseReferences(text: string): string[] {
  const out: string[] = [];
  text.split(/[\s,;]+/).forEach((part) => {
    const r = part.trim();
    if (r !== '' && !out.includes(r)) {
      out.push(r);
    }
  });
  return out;
}

/** Whether a typed or computed amount is a number above zero. */
export function positive(value: number | string): boolean {
  const n = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(n) && n > 0;
}

/** Rounds to centavos. */
export function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

/** Totals of a payment preview: applied, excess (unapplied) and the premium outstanding. */
export function previewTotals(p: PaymentPreview | undefined): {
  applied: number;
  excess: number;
  outstanding: number;
  cwt: boolean;
} {
  if (p === undefined) {
    return { applied: 0, excess: 0, outstanding: 0, cwt: false };
  }
  const applied = round2(p.invoices.reduce((s, i) => s + i.applied, 0));
  const outstanding = round2(p.invoices.reduce((s, i) => s + i.outstanding, 0));
  return {
    applied,
    excess: round2(p.excess),
    outstanding,
    cwt: p.invoices.some((i) => i.cwt),
  };
}

/** What happens to a payment, in words, for the preview pane. */
export function matchMessage(p: PaymentPreview | undefined): string {
  switch (p?.match) {
    case undefined:
      return 'Enter an ARN, invoice, policy or PN number to preview the application.';
    case 'BOOKED':
      return `Matched ${p.invoices.length} booked invoice(s) on ${p.reference ?? ''}.`;
    case 'PREBOOKED':
      return `Account ${p.prebookedArn ?? ''} is not booked yet: the payment waits in the pre-booked queue.`;
    case 'CANCELLED':
      return 'The reference belongs to a cancelled invoice: the payment goes to unapplied collections.';
    default:
      return 'No booked invoice found: the payment goes to unapplied collections.';
  }
}

/** Errors of the over-the-counter form, by field. */
export function receiveErrors(f: {
  references: string[];
  payorName: string;
  amount: string;
  mode: string;
  checkNo: string;
}): Record<string, string> {
  const errors: Record<string, string> = {};
  const amount = Number(f.amount);
  if (f.payorName.trim() === '') {
    errors.payorName = 'Payor name is required';
  }
  if (f.amount.trim() === '' || !Number.isFinite(amount) || amount <= 0) {
    errors.amount = 'Enter an amount above zero';
  }
  if (f.mode === 'CHECK' && f.checkNo.trim() === '') {
    errors.checkNo = 'Check number is required for a check payment';
  }
  if (f.references.length === 0) {
    errors.references = 'Enter at least one reference, or the payor code for an unmatched payment';
  }
  return errors;
}

/** Errors of a reinstatement request (CSHID.004/005): partial amount and the encoded fields. */
export function reinstateErrors(
  body: ReinstateBody,
  receiptAmount: number,
  premium: boolean,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (body.reasonCode === '') {
    errors.reasonCode = 'Select a reason';
  }
  if (!body.full) {
    const amount = body.amount ?? 0;
    if (!positive(amount) || amount > receiptAmount) {
      errors.amount = `Enter an amount above zero and at most ${receiptAmount.toFixed(2)}`;
    }
  }
  const required: [keyof ReinstateBody, string][] = [
    ['invoiceNo', 'Invoice number'],
    ['documentNo', premium ? 'AR number' : 'OR number'],
    ['payorName', 'Assured / payor'],
  ];
  if (premium) {
    required.push(
      ['accountOfficer', 'Account officer'],
      ['unitHead', 'Unit head'],
      ['teamLeader', 'Team leader'],
    );
  }
  required.forEach(([key, label]) => {
    if (String(body[key] ?? '').trim() === '') {
      errors[key] = `${label} is required`;
    }
  });
  return errors;
}

/** The fields a disposition action needs (CSHID.024). */
export function dispositionFields(
  action: string,
): ('targetInvoiceNo' | 'targetClientCode' | 'targetUnit' | 'payeeName')[] {
  switch (action) {
    case 'APPLY':
    case 'DST_APPLY':
      return ['targetInvoiceNo'];
    case 'RECLASS':
      return ['targetClientCode'];
    case 'TRANSFER':
      return ['targetUnit'];
    case 'REFUND':
      return ['payeeName'];
    default:
      return [];
  }
}

/** Errors of a disposition: amount within the balance, the fields of the action, whole balance to move. */
export function dispositionErrors(
  body: DispositionBody,
  action: string,
  balance: number,
): Record<string, string> {
  const errors: Record<string, string> = {};
  const amount = body.amount;
  if (!positive(amount) || amount > balance) {
    errors.amount = `Enter an amount above zero and at most ${balance.toFixed(2)}`;
  } else if ((action === 'RECLASS' || action === 'TRANSFER') && amount !== balance) {
    errors.amount = 'A reclass or transfer moves the whole balance';
  }
  dispositionFields(action).forEach((f) => {
    if ((body[f] ?? '').trim() === '') {
      errors[f] = 'Required for this disposition type';
    }
  });
  return errors;
}

/** Share of a receipt series already used, 0-100. */
export function seriesUsedPercent(s: Pick<Series, 'fromNo' | 'toNo' | 'remaining'>): number {
  const size = s.toNo - s.fromNo + 1;
  if (size <= 0) {
    return 100;
  }
  return Math.min(100, Math.max(0, Math.round(((size - s.remaining) / size) * 100)));
}

/** Ageing bucket of a pre-booked payment (PREBOOKED_AGEING alert). */
export function ageBucket(days: number): 'fresh' | 'ageing' | 'overdue' {
  if (days > 30) {
    return 'overdue';
  }
  return days > 7 ? 'ageing' : 'fresh';
}

/** Post-dated checks grouped by maturity month, months in order (the maturity calendar). */
export function byMaturityMonth(
  items: readonly PdcItem[],
): { month: string; items: PdcItem[]; total: number }[] {
  const groups = new Map<string, PdcItem[]>();
  [...items]
    .sort((a, b) => a.maturityDate.localeCompare(b.maturityDate))
    .forEach((i) => {
      const month = i.maturityDate.slice(0, 7);
      groups.set(month, [...(groups.get(month) ?? []), i]);
    });
  return [...groups.entries()].map(([month, list]) => ({
    month,
    items: list,
    total: round2(list.reduce((s, i) => s + i.amount, 0)),
  }));
}

/** "2026-09" as "September 2026". */
export function monthLabel(month: string): string {
  const [y, m] = month.split('-').map(Number);
  if (y === undefined || m === undefined || Number.isNaN(y) || Number.isNaN(m)) {
    return month;
  }
  return new Date(Date.UTC(y, m - 1, 1)).toLocaleString('en-PH', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  });
}

/** Payments of an upload run counted by outcome (applied / unapplied / prebooked / excess / failed). */
export function runTiles(
  payments: readonly { matchCategory: string }[],
  failedRows: number,
): { id: string; label: string; count: number }[] {
  const count = (c: string) => payments.filter((p) => p.matchCategory === c).length;
  return [
    { id: 'APPLIED', label: 'Applied', count: count('APPLIED') },
    {
      id: 'UNAPPLIED',
      label: 'Unapplied',
      count: count('UNAPPLIED_NO_MATCH') + count('CANCELLED_REFERENCE'),
    },
    { id: 'PREBOOKED', label: 'Pre-booked', count: count('PREBOOKED') },
    { id: 'EXCESS', label: 'With Excess', count: count('EXCESS') },
    { id: 'FAILED', label: 'Failed Rows', count: failedRows },
  ];
}

export interface JournalRow {
  batch: string;
  what: string;
}

/** Every journal batch posted for the receipt: issue, applications and actions. */
export function journalRows(r: ReceiptDetail): JournalRow[] {
  const rows: JournalRow[] = [];
  if (r.journalBatchNo) {
    rows.push({ batch: r.journalBatchNo, what: `Receipt ${r.summary.receiptNo}` });
  }
  r.applications.forEach((a) => {
    if (a.journalBatchNo) {
      rows.push({ batch: a.journalBatchNo, what: `Application ${a.reference} to ${a.invoiceNo}` });
    }
  });
  r.actions.forEach((a) => {
    if (a.journalBatchNo) {
      rows.push({ batch: a.journalBatchNo, what: `${humanize(a.action)} ${a.transactionNo}` });
    }
  });
  return rows;
}

export type ReceiptTabId = 'applications' | 'lines' | 'journal' | 'history';

export const RECEIPT_TABS: readonly { id: ReceiptTabId; label: string }[] = [
  { id: 'applications', label: 'Applications' },
  { id: 'lines', label: 'Lines' },
  { id: 'journal', label: 'Journal' },
  { id: 'history', label: 'History' },
];
