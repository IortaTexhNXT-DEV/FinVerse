import {
  disposalResult,
  monthlyDepreciation,
  periodOf,
  residualValue,
  roundMoney,
  validateAsset,
} from './assetMath';
import {
  actualDays,
  couponInterest,
  dayCountDays,
  discountOrPremium,
  maturityBucket,
  realizedGain,
  validateHolding,
} from './investmentMath';

describe('fixed asset calculations', () => {
  it('computes straight line and declining balance like the server', () => {
    expect(residualValue(60000, 5)).toBe(3000);
    expect(monthlyDepreciation(60000, 5, 'STRAIGHT_LINE', 60)).toBe(950);
    expect(monthlyDepreciation(36000, 0, 'DECLINING_BALANCE', 36)).toBe(2000);
    expect(monthlyDepreciation(1000, 0, 'STRAIGHT_LINE', 3)).toBe(333.33);
    expect(monthlyDepreciation(1000, 10, 'DECLINING_BALANCE', 1)).toBe(900);
    expect(monthlyDepreciation(0, 0, 'STRAIGHT_LINE', 12)).toBe(0);
  });

  it('computes disposal gain or loss and periods', () => {
    expect(disposalResult(50000, 58100)).toBe(-8100);
    expect(disposalResult(60000, 58100)).toBe(1900);
    expect(roundMoney(10.126)).toBe(10.13);
    expect(periodOf('2026-09-23')).toBe('2026-09');
  });

  it('validates an asset registration', () => {
    const errors = validateAsset({ tagNo: 'bad tag', takeOn: false });
    expect(Object.keys(errors)).toEqual(
      expect.arrayContaining([
        'tagNo',
        'description',
        'categoryId',
        'costCenter',
        'acquisitionCost',
        'acquisitionDate',
        'settlementAccount',
      ]),
    );
    expect(validateAsset({ takeOn: true }).capitalizationDate).toBeDefined();
    expect(
      validateAsset({
        tagNo: 'FA-1',
        description: 'Laptop',
        categoryId: 1,
        costCenter: 'IT',
        acquisitionCost: 1000,
        acquisitionDate: '2026-09-01',
        settlementAccount: '2501',
        takeOn: false,
      }),
    ).toEqual({});
  });
});

describe('investment calculations', () => {
  it('counts days under both conventions', () => {
    expect(actualDays('2026-06-01', '2026-06-30')).toBe(29);
    expect(dayCountDays('THIRTY_360', '2026-03-15', '2026-03-31')).toBe(15);
    expect(dayCountDays('THIRTY_360', '2026-01-31', '2026-02-28')).toBe(28);
    expect(dayCountDays('THIRTY_360', '2026-03-15', '2026-09-15')).toBe(180);
    expect(dayCountDays('ACT_365', '2026-03-15', '2026-09-15')).toBe(184);
  });

  it('computes coupon interest, discounts and realized gains like the server', () => {
    expect(couponInterest(1_000_000, 6, 'THIRTY_360', '2026-03-15', '2026-03-31')).toBe(2500);
    expect(couponInterest(1_000_000, 5, 'ACT_365', '2026-06-01', '2026-06-30')).toBe(3972.6);
    expect(couponInterest(1_000_000, 0, 'ACT_365', '2026-06-01', '2026-06-30')).toBe(0);
    expect(discountOrPremium(1_000_000, 980_000)).toBe(20_000);
    expect(realizedGain(1_030_000, 0, 1_020_000, 0, 20_000)).toBe(30_000);
    expect(realizedGain(1_000_000, 0, 1_000_000, 0)).toBe(0);
  });

  it('buckets maturities', () => {
    expect(maturityBucket('2026-09-30', undefined)).toBe('No maturity');
    expect(maturityBucket('2026-09-30', '2026-09-01')).toBe('Due / overdue');
    expect(maturityBucket('2026-09-30', '2026-10-10')).toBe('Within 1 month');
    expect(maturityBucket('2026-09-30', '2027-11-01')).toBe('1 - 3 years');
    expect(maturityBucket('2026-09-30', '2036-09-30')).toBe('Over 5 years');
  });

  it('validates a holding', () => {
    const errors = validateHolding({ instrumentType: 'GOVERNMENT_BOND', takeOn: true });
    expect(Object.keys(errors)).toEqual(
      expect.arrayContaining([
        'portfolioId',
        'branchId',
        'description',
        'issuerCode',
        'faceValue',
        'purchasePrice',
        'settlementDate',
        'maturityDate',
        'bankAccount',
        'takeOnDate',
      ]),
    );
    expect(
      validateHolding({
        instrumentType: 'EQUITY',
        portfolioId: 1,
        branchId: 2,
        description: 'Shares',
        issuerCode: 'IS-ALI',
        faceValue: 100,
        purchasePrice: 100,
        settlementDate: '2026-09-01',
        bankAccount: '1111',
        takeOn: false,
      }),
    ).toEqual({});
  });
});
