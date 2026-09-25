import { formatAmount, formatCompact, formatDate, humanize, titleCase } from './format';

describe('format', () => {
  it('formats amounts in accounting style', () => {
    expect(formatAmount(1234.5)).toBe('1,234.50');
    expect(formatAmount(-99)).toBe('(99.00)');
    expect(formatAmount(null)).toBe('');
    expect(formatAmount('abc')).toBe('abc');
  });

  it('compacts large numbers', () => {
    expect(formatCompact(1_500_000)).toBe('1.5M');
    expect(formatCompact(2_300)).toBe('2.3K');
    expect(formatCompact(12)).toBe('12');
  });

  it('formats ISO dates as dd-mm-yyyy', () => {
    expect(formatDate('2026-09-23')).toBe('23-09-2026');
    expect(formatDate(undefined)).toBe('');
  });

  it('humanizes enum codes', () => {
    expect(humanize('PENDING_APPROVAL')).toBe('Pending Approval');
  });
});

describe('titleCase', () => {
  it('capitalises sentence labels and keeps minor words lowercase', () => {
    expect(titleCase('Revise quotation slip (new round)')).toBe(
      'Revise Quotation Slip (New Round)',
    );
    expect(titleCase('Holds for approval')).toBe('Holds for Approval');
    expect(titleCase('not proceeded')).toBe('Not Proceeded');
  });

  it('keeps acronyms and mixed-case words as written', () => {
    expect(titleCase('TSU recommendation')).toBe('TSU Recommendation');
    expect(titleCase('Sent to ManCom')).toBe('Sent to ManCom');
    expect(titleCase('Awaiting insurer OR')).toBe('Awaiting Insurer OR');
    expect(titleCase('')).toBe('');
  });
});
