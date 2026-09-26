import {
  activeFilterCount,
  keptFilters,
  orUndefined,
  searchFromParams,
  tabFilter,
  tabOf,
} from './invoiceSearch';

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

  it('reads and keeps the assured, inception and account officer filters', () => {
    const search = searchFromParams(
      new URLSearchParams(
        'assured=Cruz&inceptionFrom=2026-01-01&inceptionTo=2026-12-31&ao=ao&flag=HOLD',
      ),
    );
    expect(search).toMatchObject({
      assured: 'Cruz',
      inceptionFrom: '2026-01-01',
      inceptionTo: '2026-12-31',
      ao: 'ao',
    });
    expect(keptFilters(search)).not.toHaveProperty('flag', 'HOLD');
    expect(keptFilters(search).ao).toBe('ao');
    expect(activeFilterCount(search)).toBe(4);
    expect(activeFilterCount({})).toBe(0);
  });
});
