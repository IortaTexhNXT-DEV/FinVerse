import { describe, expect, it } from 'vitest';
import type { StrTransaction } from './api';
import {
  blankTransaction,
  extractionPeriodError,
  filingErrors,
  keyed,
  monthStart,
  plain,
  total,
  transactionError,
  transactionsError,
} from './strLogic';

function line(patch: Partial<StrTransaction> = {}): StrTransaction {
  return {
    reference: 'OR-1',
    date: '2026-05-01',
    amount: '1,000.25',
    currency: 'PHP',
    type: 'RECEIPT',
    description: null,
    ...patch,
  };
}

describe('transaction lines', () => {
  it('gives each line its own key and strips it for the request', () => {
    const a = blankTransaction('2026-05-01');
    const lines = keyed([line(), line({ reference: 'OR-2' })]);
    expect(new Set([a.key, ...lines.map((l) => l.key)]).size).toBe(3);
    expect(a.currency).toBe('PHP');
    expect(plain(lines.slice(0, 1))).toEqual([line()]);
  });

  it('checks the reference, date, currency and a positive amount', () => {
    expect(transactionError(line())).toBeUndefined();
    expect(transactionError(line({ reference: ' ' }))).toBe(
      'Enter the reference, date and currency of each transaction',
    );
    expect(transactionError(line({ amount: '0' }))).toBe('The amount must be greater than 0');
    expect(transactionError(line({ amount: 'abc' }))).toBe('The amount must be greater than 0');
    expect(transactionsError([line(), line({ date: '' })])).toBe(
      'Enter the reference, date and currency of each transaction',
    );
    expect(transactionsError([line()])).toBeUndefined();
  });

  it('totals the amounts to centavos, ignoring non-numbers', () => {
    expect(total([line(), line({ amount: 0.1 }), line({ amount: 'x' })])).toBeCloseTo(1000.35, 2);
  });
});

describe('extraction and filing', () => {
  it('needs a period in order', () => {
    expect(extractionPeriodError('', '2026-05-01')).toBe('Enter the period');
    expect(extractionPeriodError('2026-05-02', '2026-05-01')).toBe(
      'The end date must be on or after the start date',
    );
    expect(extractionPeriodError('2026-05-01', '2026-05-31')).toBeUndefined();
    expect(monthStart('2026-05-17')).toBe('2026-05-01');
  });

  it('needs the AMLC reference and a filing date not before the extraction', () => {
    expect(filingErrors(' ', '', undefined)).toEqual({
      reference: 'Enter the AMLC reference',
      filedOn: 'Enter the filing date',
    });
    expect(filingErrors('AMLC-1', '2026-05-01', '2026-05-02')).toEqual({
      filedOn: 'The filing date cannot be before the extraction date',
    });
    expect(filingErrors('AMLC-1', '2026-05-02', '2026-05-02')).toEqual({});
  });
});
