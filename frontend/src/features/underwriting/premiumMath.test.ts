import {
  cancellationReturn,
  estimate,
  expiryDate,
  oneYearFrom,
  pct,
  riskPremium,
  riskTotals,
  round2,
} from './premiumMath';

describe('premium math', () => {
  it('rounds to cents', () => {
    expect(round2(1.005)).toBe(1.01);
    expect(pct(95_000, 12.5)).toBe(11_875);
    expect(pct(100, undefined)).toBe(0);
  });

  it('computes risk premium from rate when no premium is entered', () => {
    expect(riskPremium({ sumInsured: 15_000_000, rate: 0.35 })).toBe(52_500);
    expect(riskPremium({ sumInsured: 1000, rate: 5, premium: 42 })).toBe(42);
    const totals = riskTotals([
      { sumInsured: 7_000_000, rate: 1 },
      { sumInsured: 3_000_000, premium: 30_000 },
    ]);
    expect(totals).toEqual({ sumInsured: 10_000_000, grossPremium: 100_000 });
  });

  it('matches the server debit note of a fire policy', () => {
    const e = estimate({
      grossPremium: 100_000,
      discountRate: 10,
      loadingRate: 5,
      sharePct: 100,
      leader: false,
      taxRatePct: 27.25,
      policyFee: 250,
      commissionRate: 20,
    });
    expect(e.netPremium).toBe(95_000);
    expect(e.taxes).toBe(25_887.5);
    expect(e.totalDue).toBe(121_137.5);
    expect(e.commission).toBe(19_000);
  });

  it('bills the whole premium when leading a coinsurance', () => {
    const base = {
      grossPremium: 100_000,
      sharePct: 60,
      taxRatePct: 0,
      policyFee: 0,
      commissionRate: 0,
    };
    expect(estimate({ ...base, leader: true }).billedPremium).toBe(100_000);
    expect(estimate({ ...base, leader: false }).billedPremium).toBe(60_000);
  });

  it('returns unexpired premium pro rata on cancellation', () => {
    expect(cancellationReturn(100_000, '2026-03-10', '2027-03-09', '2026-09-10')).toBe(49_589.04);
    expect(cancellationReturn(100_000, '2026-01-01', '2026-12-31', '2027-02-01')).toBe(0);
    expect(cancellationReturn(100_000, '2026-01-02', '2026-01-01', '2026-01-01')).toBe(0);
  });

  it('computes dates', () => {
    expect(expiryDate('2026-02-03', 15)).toBe('2026-02-18');
    expect(oneYearFrom('2026-03-10')).toBe('2027-03-09');
  });
});
