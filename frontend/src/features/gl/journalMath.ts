import type { JournalLineInput } from '@/api/gl';
import { formatAmount } from '@/utils/format';

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

/** An unused placeholder row: no account and no amount. Only such rows are left out on save. */
export function isBlankLine(line: JournalLineInput): boolean {
  return line.accountCode.trim() === '' && (line.amount === 0 || Number.isNaN(line.amount));
}

/** What is wrong with a journal line, and in which input. */
export interface LineProblem {
  field: 'account' | 'amount';
  message: string;
}

/**
 * Problems of the lines that are filled in, by row index (0-based). Blank rows have none; a row
 * with a zero or negative amount is reported, never silently dropped.
 */
export function lineProblems(lines: JournalLineInput[]): Record<number, LineProblem> {
  const problems: Record<number, LineProblem> = {};
  lines.forEach((line, index) => {
    if (isBlankLine(line)) {
      return;
    }
    if (line.accountCode.trim() === '') {
      problems[index] = { field: 'account', message: 'Choose an account' };
    } else if (!Number.isFinite(line.amount) || line.amount <= 0) {
      problems[index] = { field: 'amount', message: 'Amount must be greater than zero' };
    }
  });
  return problems;
}

export interface BalanceStatus {
  tone: 'success' | 'danger' | 'neutral';
  label: string;
}

/** Balance indicator of the lines grid: neutral until amounts are entered. */
export function balanceStatus(t: Totals): BalanceStatus {
  if (t.balanced) {
    return { tone: 'success', label: 'Balanced' };
  }
  if (t.debit === 0 && t.credit === 0) {
    return { tone: 'neutral', label: 'Enter amounts' };
  }
  if (t.difference === 0) {
    return { tone: 'danger', label: 'Amounts must be greater than zero' };
  }
  return { tone: 'danger', label: `Difference ${formatAmount(t.difference)}` };
}
