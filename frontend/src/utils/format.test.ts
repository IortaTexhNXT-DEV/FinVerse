import { formatAmount, formatCompact, formatDate, humanize } from './format';

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
