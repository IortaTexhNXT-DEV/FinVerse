import { reinsuranceInUse, withoutReinsurance } from './reinsurance';

describe('reinsurance functions (deferred to Phase 2)', () => {
  const values = ['SUPPLIER', 'REINSURER', 'RI_BROKER', 'REINSURANCE', 'BROKER'] as const;

  it('are hidden from a user without RI_BROKING_TRANSACT (every BDOI group profile)', () => {
    const can = (p: string) => p === 'MASTER_MAINTAIN';
    expect(reinsuranceInUse(can)).toBe(false);
    expect(withoutReinsurance(values, can)).toEqual(['SUPPLIER', 'BROKER']);
  });

  it('are offered to the super user, who holds every permission', () => {
    const can = () => true;
    expect(reinsuranceInUse(can)).toBe(true);
    expect(withoutReinsurance(values, can)).toEqual([...values]);
  });
});
