import type { StrTransaction } from './api';

/**
 * Pure helpers of the STR screens (SNSRP-705, 706; FR-SS-070 to 072): the transaction checks, the
 * total, the period and filing checks with the FRS messages.
 */

/** A transaction line being edited, with a key of its own. */
export interface EditLine extends StrTransaction {
  key: number;
}

let nextKey = 0;

/** A blank transaction line. */
export function blankTransaction(date: string): EditLine {
  nextKey += 1;
  return {
    key: nextKey,
    reference: '',
    date,
    amount: '',
    currency: 'PHP',
    type: 'RECEIPT',
    description: '',
  };
}

/** The lines of an STR with keys for editing. */
export function keyed(lines: StrTransaction[]): EditLine[] {
  return lines.map((line) => {
    nextKey += 1;
    return { ...line, key: nextKey };
  });
}

/** The lines without their keys (the request). */
export function plain(lines: EditLine[]): StrTransaction[] {
  return lines.map((l) => ({
    reference: l.reference,
    date: l.date,
    amount: l.amount,
    currency: l.currency,
    type: l.type,
    description: l.description,
  }));
}

/** The error of a transaction line, or undefined when valid. */
export function transactionError(t: StrTransaction): string | undefined {
  if (t.reference.trim() === '' || t.date === '' || t.currency.trim() === '') {
    return 'Enter the reference, date and currency of each transaction';
  }
  const amount = Number(String(t.amount).replaceAll(',', ''));
  if (!Number.isFinite(amount) || amount <= 0) {
    return 'The amount must be greater than 0';
  }
  return undefined;
}

/** The first error of the transactions, or undefined. */
export function transactionsError(lines: StrTransaction[]): string | undefined {
  return lines.map(transactionError).find((e) => e !== undefined);
}

/** The total of the transactions (rounded to centavos). */
export function total(lines: StrTransaction[]): number {
  const sum = lines.reduce((acc, t) => {
    const amount = Number(String(t.amount).replaceAll(',', ''));
    return acc + (Number.isFinite(amount) ? amount : 0);
  }, 0);
  return Math.round(sum * 100) / 100;
}

/** "The end date must be on or after the start date" (FR-SS-071). */
export function extractionPeriodError(from: string, to: string): string | undefined {
  if (from === '' || to === '') {
    return 'Enter the period';
  }
  return to < from ? 'The end date must be on or after the start date' : undefined;
}

/** The FRS messages of Record Filing (FR-SS-072). */
export function filingErrors(
  reference: string,
  filedOn: string,
  extractedOn: string | undefined,
): { reference?: string; filedOn?: string } {
  const errors: { reference?: string; filedOn?: string } = {};
  if (reference.trim() === '') {
    errors.reference = 'Enter the AMLC reference';
  }
  if (filedOn === '') {
    errors.filedOn = 'Enter the filing date';
  } else if (extractedOn !== undefined && filedOn < extractedOn) {
    errors.filedOn = 'The filing date cannot be before the extraction date';
  }
  return errors;
}

/** The first day of the month of an ISO date. */
export function monthStart(isoDay: string): string {
  return `${isoDay.slice(0, 8)}01`;
}
