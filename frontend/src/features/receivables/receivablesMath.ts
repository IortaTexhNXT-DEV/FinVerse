import type {
  BookEntry,
  OpenItem,
  PdcStatus,
  Receipt,
  ReceiptSummary,
  StatementLine,
} from '@/api/receivables';
import { formatAmount } from '@/utils/format';

const CENTS = 100;

/** Rounds to cents to avoid floating point noise in money totals. */
export function round2(value: number): number {
  return Math.round(value * CENTS) / CENTS;
}

/** Sum of the allocation amounts keyed by debit item id. */
export function allocationTotal(allocations: Record<number, number>): number {
  return round2(
    Object.values(allocations).reduce((acc, v) => acc + (Number.isFinite(v) ? v : 0), 0),
  );
}

/** Allocates an amount to the oldest due items first (same order as the server's FIFO). */
export function fifoAllocate(items: OpenItem[], amount: number): Record<number, number> {
  const sorted = [...items].sort(
    (a, b) => a.dueDate.localeCompare(b.dueDate) || a.documentDate.localeCompare(b.documentDate),
  );
  const result: Record<number, number> = {};
  let remaining = round2(amount);
  for (const item of sorted) {
    if (remaining <= 0) {
      break;
    }
    const value = round2(Math.min(remaining, item.outstanding));
    if (value > 0) {
      result[item.id] = value;
      remaining = round2(remaining - value);
    }
  }
  return result;
}

/** Validation messages for manual allocations (the server re-validates). */
export function allocationErrors(
  items: OpenItem[],
  allocations: Record<number, number>,
  amount: number,
): string[] {
  const errors: string[] = [];
  for (const item of items) {
    const value = allocations[item.id] ?? 0;
    if (value < 0) {
      errors.push(`${item.documentNo}: amount cannot be negative`);
    } else if (value > item.outstanding) {
      errors.push(`${item.documentNo}: exceeds the outstanding ${item.outstanding.toFixed(2)}`);
    }
  }
  if (allocationTotal(allocations) > round2(amount)) {
    errors.push('Allocations exceed the receipt amount');
  }
  return errors;
}

/** Bank Reconciliation Statement arithmetic (book-side signs). */
export function computeBrs(
  bookBalance: number,
  bookDebits: number,
  bookCredits: number,
  bankDebits: number,
  bankCredits: number,
  statementBalance: number,
): { computedBankBalance: number; difference: number } {
  const computed = round2(bookBalance - bookDebits + bookCredits - bankDebits + bankCredits);
  return { computedBankBalance: computed, difference: round2(statementBalance - computed) };
}

/**
 * Totals of a manual match selection. A selection is balanced when book and bank totals are
 * equal: book entries against bank lines, or offsetting items of one side only (a bounced cheque
 * and its reversal) netting to zero. At least two items are needed.
 */
export function selectionBalance(
  book: BookEntry[],
  bank: StatementLine[],
): { bookTotal: number; bankTotal: number; difference: number; balanced: boolean } {
  const bookTotal = round2(book.reduce((acc, e) => acc + e.debit - e.credit, 0));
  const bankTotal = round2(bank.reduce((acc, l) => acc + l.credit - l.debit, 0));
  const difference = round2(bookTotal - bankTotal);
  return {
    bookTotal,
    bankTotal,
    difference,
    balanced: book.length + bank.length >= 2 && difference === 0,
  };
}

/** Splits one CSV row, honouring double quotes. */
export function splitCsv(line: string): string[] {
  const cells: string[] = [];
  let current = '';
  let quoted = false;
  let i = 0;
  while (i < line.length) {
    const c = line.charAt(i);
    if (c === '"' && quoted && line.charAt(i + 1) === '"') {
      current += '"';
      i += 1;
    } else if (c === '"') {
      quoted = !quoted;
    } else if (c === ',' && !quoted) {
      cells.push(current);
      current = '';
    } else {
      current += c;
    }
    i += 1;
  }
  cells.push(current);
  return cells;
}

/** Quick preview of a statement file before upload: line count and debit / credit totals. */
export function csvPreview(text: string): { lines: number; debit: number; credit: number } {
  const rows = text.split(/\r?\n/).filter((r) => r.trim() !== '');
  const header = (rows[0] ?? '')
    .toLowerCase()
    .split(',')
    .map((h) => h.trim());
  const di = header.indexOf('debit');
  const ci = header.indexOf('credit');
  let debit = 0;
  let credit = 0;
  for (const row of rows.slice(1)) {
    const cells = splitCsv(row);
    debit += Number((cells[di] ?? '').replace(/,/g, '')) || 0;
    credit += Number((cells[ci] ?? '').replace(/,/g, '')) || 0;
  }
  return { lines: Math.max(rows.length - 1, 0), debit: round2(debit), credit: round2(credit) };
}

const REPORT_GROUPS: [string, string][] = [
  ['FIN-AR-AGE', 'Debtors ageing'],
  ['FIN-AR-SOO', 'Statements of outstanding'],
  ['FIN-ARAP-', 'Statements of account'],
  ['FIN-AR-CHQ', 'Cheques received'],
  ['FIN-BRS-', 'Bank reconciliation'],
  ['FIN-PDC-RCV', 'Post-dated cheques received'],
];

/** Groups the receivables reports of the catalogue (other reports are ignored). */
export function receivablesReportGroups<T extends { code: string }>(entries: T[]): [string, T[]][] {
  return REPORT_GROUPS.map(([prefix, label]): [string, T[]] => [
    label,
    entries.filter((e) => e.code.startsWith(prefix)).sort((a, b) => a.code.localeCompare(b.code)),
  ]).filter(([, list]) => list.length > 0);
}

/** Actions allowed on a post-dated cheque in a status. */
export function pdcActions(status: PdcStatus): ('deposit' | 'clear' | 'bounce' | 'return')[] {
  switch (status) {
    case 'ON_HAND':
    case 'DUE':
      return ['deposit', 'return'];
    case 'DEPOSITED':
      return ['clear', 'bounce'];
    default:
      return [];
  }
}

/** Actions allowed on a receipt for the current user (maker-checker). */
export function receiptActions(
  r: ReceiptSummary,
  username: string | undefined,
  can: (permission: string) => boolean,
): { approve: boolean; reject: boolean; cancel: boolean; bounce: boolean; apply: boolean } {
  const checker = can('RECEIPT_PAYMENT_AUTHORIZE');
  const decide = r.status === 'PENDING_APPROVAL' && checker && username !== r.createdBy;
  const reverse = r.status === 'APPROVED' && checker && r.depositStatus !== 'IN_SLIP';
  return {
    approve: decide,
    reject: decide,
    cancel: reverse,
    bounce: reverse && isChequeMode(r),
    apply: canApply(r, can),
  };
}

function isChequeMode(r: ReceiptSummary): boolean {
  return r.mode === 'CHEQUE' || r.mode === 'PDC';
}

function canApply(r: ReceiptSummary, can: (permission: string) => boolean): boolean {
  return (
    r.status === 'APPROVED' &&
    can('RECEIPT_PAYMENT_MAINTAIN') &&
    r.unappliedAmount > 0 &&
    r.payerType !== 'OTHER'
  );
}

/**
 * Outcome of "Apply on-account (FIFO)" for the confirmation message, from the receipt before and
 * after the application: "OR-…: 7,500.00 applied to 2 debit notes, 1,500.00 still on account".
 */
export function applicationSummary(before: Receipt, after: Receipt): string {
  const no = after.summary.receiptNo;
  const applied = round2(after.summary.appliedAmount - before.summary.appliedAmount);
  const notes = after.allocations.length - before.allocations.length;
  const left = after.summary.unappliedAmount;
  if (applied <= 0) {
    return `${no}: no open debit notes to apply; ${formatAmount(left)} still on account`;
  }
  const target = notes === 1 ? '1 debit note' : `${Math.max(notes, 1)} debit notes`;
  const rest = left > 0 ? `${formatAmount(left)} still on account` : 'nothing left on account';
  return `${no}: ${formatAmount(applied)} applied to ${target}, ${rest}`;
}
