import type { OpenItem } from '@/api/subledger';

const DAY_MS = 86_400_000;

export interface StatementSummary {
  /** Outstanding receivables (debit items). */
  receivable: number;
  /** Outstanding payables and unallocated credits (credit items). */
  payable: number;
  /** Net balance, debit positive (the party owes us). */
  net: number;
  /** Outstanding amount already past its due date (either side, net). */
  overdue: number;
  openItems: number;
}

function round(n: number): number {
  return Math.round(n * 100) / 100;
}

/** Signed outstanding amount of an item: receivables positive, payables negative. */
export function signedOutstanding(item: OpenItem): number {
  return item.direction === 'DEBIT' ? item.outstanding : -item.outstanding;
}

/** Days an item is past due on a date (0 when not yet due). */
export function daysOverdue(item: OpenItem, asOf: string): number {
  const diff = (Date.parse(asOf) - Date.parse(item.dueDate)) / DAY_MS;
  return diff > 0 ? Math.floor(diff) : 0;
}

/** Totals of a party statement (items in one currency are expected; amounts are summed as given). */
export function summarize(items: OpenItem[], asOf: string): StatementSummary {
  const open = items.filter((i) => i.outstanding !== 0);
  const receivable = open
    .filter((i) => i.direction === 'DEBIT')
    .reduce((a, i) => a + i.outstanding, 0);
  const payable = open
    .filter((i) => i.direction === 'CREDIT')
    .reduce((a, i) => a + i.outstanding, 0);
  const overdue = open
    .filter((i) => daysOverdue(i, asOf) > 0)
    .reduce((a, i) => a + signedOutstanding(i), 0);
  return {
    receivable: round(receivable),
    payable: round(payable),
    net: round(receivable - payable),
    overdue: round(overdue),
    openItems: open.length,
  };
}

/** Items ordered by document date then number, optionally only those still open. */
export function statementLines(items: OpenItem[], openOnly: boolean): OpenItem[] {
  return items
    .filter((i) => !openOnly || i.outstanding !== 0)
    .sort(
      (a, b) =>
        a.documentDate.localeCompare(b.documentDate) || a.documentNo.localeCompare(b.documentNo),
    );
}
