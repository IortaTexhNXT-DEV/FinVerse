import type { Quotation } from '@/api/quotations';
import { coverPeriod } from './coverPeriod';

const quotation = (
  content: Partial<Quotation['content']>,
  period?: string,
): Pick<Quotation, 'content' | 'coverPeriod'> => ({
  content: content as Quotation['content'],
  coverPeriod: period,
});

describe('quotation cover period', () => {
  it('shows inception to expiry when entered', () => {
    expect(coverPeriod(quotation({ periodFrom: '2026-11-01', periodTo: '2027-11-01' }))).toBe(
      '01-Nov-2026 to 01-Nov-2027',
    );
  });

  it('shows the package term of a package quotation without dates', () => {
    expect(coverPeriod(quotation({}, 'Package term 01-Sep-2026 to 31-Aug-2027'))).toBe(
      'Package term 01-Sep-2026 to 31-Aug-2027',
    );
  });

  it('is empty only when neither is known', () => {
    expect(coverPeriod(quotation({}))).toBe('');
    expect(coverPeriod(quotation({}, ''))).toBe('');
  });
});
