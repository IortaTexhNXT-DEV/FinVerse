import { describe, expect, it, vi } from 'vitest';
import { rateTypeLabel, revaluationRateActions } from './rateLabels';

describe('exchange rate texts', () => {
  it('names the rate types in words', () => {
    expect(rateTypeLabel('BOOK')).toBe('Book');
    expect(rateTypeLabel('CLOSING')).toBe('Closing');
  });

  it('copies a month-end rate to the Book rates from the row menu of the maintainer', () => {
    const copy = vi.fn();
    const actions = revaluationRateActions(true, copy);
    expect(actions.map((a) => a.label)).toEqual(['Copy to Book Rates']);
    actions[0]?.onSelect('');
    expect(copy).toHaveBeenCalled();
    expect(revaluationRateActions(false, copy)).toEqual([]);
  });
});
