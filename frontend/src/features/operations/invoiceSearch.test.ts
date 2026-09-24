import { orUndefined, searchFromParams, tabFilter, tabOf } from './invoiceSearch';

describe('invoice search criteria', () => {
  it('reads the criteria of a home tile link', () => {
    const search = searchFromParams(
      new URLSearchParams('payment=PAID&remittance=UNPROCESSED&dp=false'),
    );
    expect(search.payment).toBe('PAID');
    expect(search.remittance).toBe('UNPROCESSED');
    expect(search.dp).toBe(false);
    expect(search.locked).toBeUndefined();
    expect(tabOf(search)).toBe('ALL');
    expect(
      tabOf(searchFromParams(new URLSearchParams('payment=PAID&remittance=UNPROCESSED'))),
    ).toBe('PAID_UNREMITTED');
  });

  it('maps tabs to criteria and back', () => {
    expect(tabFilter('ON_HOLD')).toEqual({ flag: 'HOLD' });
    expect(tabOf(tabFilter('LOCKED'))).toBe('LOCKED');
    expect(tabOf({})).toBe('ALL');
  });

  it('drops blank optional criteria', () => {
    expect(orUndefined('  ')).toBeUndefined();
    expect(orUndefined(' INS-A ')).toBe('INS-A');
  });
});
