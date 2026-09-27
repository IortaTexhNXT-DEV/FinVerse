import { describe, expect, it } from 'vitest';
import {
  CASE_TABS,
  correctionActions,
  emptyLine,
  hasVariance,
  lineErrors,
  tabParam,
  toDraft,
  toLine,
  totals,
  wrongAccountDrafts,
} from './acsl';
import type { OriginalLine } from './api';

const original: OriginalLine = {
  batchNo: 'PRM-1',
  journalType: 'PREMIUM',
  valueDate: '2026-01-15',
  lineNo: 1,
  accountCode: '1210.01',
  accountName: 'PR basic',
  side: 'DEBIT',
  amount: 1000,
  partyCode: 'CL-1',
};

describe('correction entries', () => {
  it('reverses a line and re-posts it to the right account', () => {
    const drafts = wrongAccountDrafts(original, '1210.06', 'INV-1');
    expect(drafts.map((d) => [d.accountCode, d.side, d.origin])).toEqual([
      ['1210.01', 'CREDIT', 'REVERSAL'],
      ['1210.06', 'DEBIT', 'REPOST'],
    ]);
    const lines = drafts.map(toLine);
    expect(totals(lines)).toEqual({ debit: 1000, credit: 1000, balanced: true });
    expect(lines[0]?.partyCode).toBe('CL-1');
    const repost = lines[1];
    expect(repost ? toDraft(repost).amount : '').toBe('1000');
  });

  it('checks balance and lines', () => {
    expect(totals([{ side: 'DEBIT', amount: 10 }]).balanced).toBe(false);
    expect(
      totals([
        { side: 'DEBIT', amount: 10.1 },
        { side: 'CREDIT', amount: 10.2 },
      ]).balanced,
    ).toBe(false);
    const errors = lineErrors([
      emptyLine(),
      { ...emptyLine(), accountCode: '4101', amount: '-1' },
      { ...emptyLine(), accountCode: '4101', amount: '5', component: 'BASIC' },
      { ...emptyLine(), accountCode: '4101', amount: '5' },
    ]);
    expect(Object.keys(errors)).toEqual(['0', '1', '2']);
    expect(
      toLine({ ...emptyLine('CREDIT'), accountCode: ' 4101 ', amount: '5' }).partyCode,
    ).toBeUndefined();
  });

  it('offers one button per business action', () => {
    expect(correctionActions(['submit', 'return', 'cancel', 'approve'])).toEqual([
      'submit',
      'approve',
    ]);
  });
});

describe('boards', () => {
  it('reads the tab of the URL and flags variances', () => {
    expect(tabParam('INVESTIGATING', CASE_TABS, 'ALL')).toBe('INVESTIGATING');
    expect(tabParam('X', CASE_TABS, 'ALL')).toBe('ALL');
    expect(hasVariance(0)).toBe(false);
    expect(hasVariance(-0.5)).toBe(true);
    expect(hasVariance(undefined)).toBe(false);
  });
});
