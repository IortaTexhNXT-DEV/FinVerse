import {
  monthTotals,
  scale,
  SEASONALITY,
  spreadEven,
  spreadWeighted,
  sum,
  utilizationLevel,
} from './budgetMath';

describe('budget spreading', () => {
  it('spreads evenly and puts the rounding remainder in December', () => {
    const months = spreadEven(1000);
    expect(months).toHaveLength(12);
    expect(months[0]).toBe(83.33);
    expect(months[11]).toBe(83.37);
    expect(sum(months)).toBe(1000);
  });

  it('follows seasonality weights exactly to the cent', () => {
    const months = spreadWeighted(360_000_000, SEASONALITY['Premium renewals'] ?? []);
    expect(sum(months)).toBe(360_000_000);
    expect(months[11]).toBeGreaterThan(months[0] ?? 0);
    const weighted = spreadWeighted(2400, [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 13]);
    expect(weighted[0]).toBe(100);
    expect(weighted[11]).toBe(1300);
  });

  it('rejects invalid seasonality', () => {
    expect(() => spreadWeighted(10, [1, 2])).toThrow('twelve');
    expect(() =>
      spreadWeighted(
        10,
        Array.from({ length: 12 }, () => 0),
      ),
    ).toThrow('positive');
  });

  it('computes month totals and scaling', () => {
    const rows = [{ months: spreadEven(1200) }, { months: spreadEven(0.12) }];
    const totals = monthTotals(rows);
    expect(totals[0]).toBe(100.01);
    expect(sum(totals)).toBe(1200.12);
    expect(scale([100, 33.33], 10)).toEqual([110, 36.66]);
  });

  it('classifies utilization', () => {
    expect(utilizationLevel(undefined, 90)).toBe('none');
    expect(utilizationLevel(120, 90)).toBe('over');
    expect(utilizationLevel(95, 90)).toBe('warning');
    expect(utilizationLevel(10, 90)).toBe('ok');
  });
});
