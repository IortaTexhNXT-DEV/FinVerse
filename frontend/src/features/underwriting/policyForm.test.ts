import type { Policy } from '@/api/underwriting';
import { fromPolicy, newPolicy, normalize, validatePolicy } from './policyForm';

describe('policy form', () => {
  it('starts with a one-year direct policy and one risk', () => {
    const form = newPolicy(1, 2, '2026-03-10');
    expect(form.periodTo).toBe('2027-03-09');
    expect(form.risks).toHaveLength(1);
    expect(validatePolicy(form)).toContain('Select a product');
  });

  it('accepts a complete form and flags missing parties', () => {
    const form = {
      ...newPolicy(1, 2, '2026-03-10'),
      productId: 5,
      customerCode: 'C-000201',
      insuredName: 'Luzon Steel',
      risks: [{ description: 'Plant', sumInsured: 1000 }],
    };
    expect(validatePolicy(form)).toEqual([]);
    const broker = { ...form, sourceType: 'BROKER' as const };
    expect(validatePolicy(broker)).toContain('Select the agent or broker');
    const coinsured = { ...form, businessType: 'DIRECT_WITH_COINSURANCE' as const };
    expect(validatePolicy(coinsured)).toContain('Select the coinsurer');
    expect(validatePolicy({ ...form, periodTo: '2026-01-01' })).toHaveLength(1);
  });

  it('checks the commission override of intermediated business', () => {
    const form = {
      ...newPolicy(1, 2, '2026-03-10'),
      productId: 5,
      customerCode: 'C-000201',
      insuredName: 'Luzon Steel',
      sourceType: 'BROKER' as const,
      intermediaryCode: 'B-0001',
      risks: [{ description: 'Plant', sumInsured: 1000 }],
    };
    const message = 'Commission must be between 0 and 100 %';
    expect(validatePolicy({ ...form, commissionRate: 12.5 })).toEqual([]);
    expect(validatePolicy({ ...form, commissionRate: 100.5 })).toContain(message);
    expect(validatePolicy({ ...form, commissionRate: -1 })).toContain(message);
    expect(validatePolicy({ ...form, sourceType: 'DIRECT', commissionRate: 150 })).not.toContain(
      message,
    );
  });

  it('normalizes dependent fields', () => {
    const form = {
      ...newPolicy(1, 2, '2026-03-10'),
      intermediaryCode: 'B-0001',
      sharePct: 60,
      coinsurerCode: 'CO-0001',
      coinsuranceLeader: true,
    };
    const n = normalize(form);
    expect(n.intermediaryCode).toBeUndefined();
    expect(n.sharePct).toBe(100);
    expect(n.coinsurerCode).toBeUndefined();
    expect(n.coinsuranceLeader).toBe(false);
  });

  it('converts a policy for editing', () => {
    const policy = {
      companyId: 1,
      branchId: 2,
      productId: 3,
      customerCode: 'C-000101',
      insuredName: 'Juan',
      sourceType: 'AGENT',
      intermediaryCode: 'A-0001',
      issueDate: '2026-01-01',
      periodFrom: '2026-01-01',
      periodTo: '2026-12-31',
      currency: 'PHP',
      businessType: 'DIRECT',
      sharePct: 100,
      coinsuranceLeader: false,
      discountRate: 0,
      loadingRate: 0,
      premium: { commissionRate: 15 },
      risks: [
        {
          lineNo: 1,
          description: 'Car',
          sumInsured: 1_000_000,
          rate: 3,
          premium: 30_000,
          marine: { vesselName: 'MV Star' },
        },
      ],
    } as unknown as Policy;
    const form = fromPolicy(policy);
    expect(form.commissionRate).toBe(15);
    expect(form.risks[0]?.vesselName).toBe('MV Star');
  });
});
