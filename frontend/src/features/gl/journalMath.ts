import type { JournalLineInput } from '@/api/gl';

export interface Totals {
  debit: number;
  credit: number;
  difference: number;
  balanced: boolean;
}

const CENTS = 100;

/** Sums lines in header-currency terms (rate-converted lines are validated by the server). */
export function totals(lines: JournalLineInput[]): Totals {
  const sum = (side: 'DEBIT' | 'CREDIT') =>
    Math.round(
      lines
        .filter((l) => l.side === side)
        .reduce((acc, l) => acc + (Number.isFinite(l.amount) ? l.amount : 0), 0) * CENTS,
    ) / CENTS;
  const debit = sum('DEBIT');
  const credit = sum('CREDIT');
  const difference = Math.round((debit - credit) * CENTS) / CENTS;
  return { debit, credit, difference, balanced: debit > 0 && difference === 0 };
}

export function emptyLine(side: 'DEBIT' | 'CREDIT'): JournalLineInput {
  return { accountCode: '', side, amount: 0 };
}
