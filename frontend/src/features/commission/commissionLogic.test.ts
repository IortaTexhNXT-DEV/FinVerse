import { describe, expect, it } from 'vitest';
import type { CertificateInput, SchemeTerms } from './commissionApi';
import {
  answersOf,
  answersProblem,
  bulkActions,
  certificateProblem,
  describeTier,
  failedRules,
  receiptsTotal,
  schemeProblem,
  withCount,
} from './commissionLogic';

describe('accounts', () => {
  it('lists the failed rules', () => {
    const rules = [
      { rule: 'INVOICE_BOOKED', passed: true },
      { rule: 'DIRECT_PAYMENT', passed: false, message: 'Not tagged direct payment' },
      { rule: 'PREMIUM', passed: false },
    ];
    expect(failedRules({ rules })).toBe('Not tagged direct payment; PREMIUM');
    expect(failedRules({ rules: [] })).toBe('');
  });

  it('offers the bulk actions of each tab', () => {
    expect(bulkActions('DP_FOR_CONFIRMATION')).toEqual({
      confirm: true,
      exclude: true,
      bill: false,
    });
    expect(bulkActions('DP_FOR_BILLING')).toEqual({ confirm: false, exclude: true, bill: true });
    expect(bulkActions('BILLED')).toEqual({ confirm: false, exclude: false, bill: false });
  });

  it('shows counts on tabs', () => {
    expect(withCount('Billed', 3)).toBe('Billed (3)');
    expect(withCount('Billed', 0)).toBe('Billed');
    expect(withCount('Billed', undefined)).toBe('Billed');
  });
});

describe('insurer answers', () => {
  it('sends only the accounts answered, with the reason of rejections', () => {
    const answers = answersOf({
      'BI-1': { approved: true, reason: 'OTHERS', comment: '' },
      'BI-2': { approved: false, reason: 'OTHERS', comment: 'Paid to BDOI' },
      'BI-3': { approved: undefined, reason: '', comment: '' },
    });
    expect(answers).toEqual([
      { invoiceNo: 'BI-1', approved: true, reason: undefined, comment: undefined },
      { invoiceNo: 'BI-2', approved: false, reason: 'OTHERS', comment: 'Paid to BDOI' },
    ]);
    expect(answersProblem(answers)).toBeUndefined();
  });

  it('needs an answer and the reason of a rejection', () => {
    expect(answersProblem([])).toMatch(/at least one/);
    expect(answersProblem([{ invoiceNo: 'BI-9', approved: false }])).toMatch(/BI-9/);
  });
});

describe('schemes', () => {
  const terms: SchemeTerms = {
    name: 'Top Up',
    schemeType: 'TOP_UP',
    calculation: 'TARGET_TIERED',
    periodType: 'MONTHLY',
    beneficiary: 'BDOI',
    segments: [],
    productLines: [],
    active: true,
    tiers: [{ minProduction: 100000, ratePercent: 2, multiplier: 1.5 }],
  };

  it('accepts complete tiers', () => {
    expect(schemeProblem('TOP_UP', terms)).toBeUndefined();
    expect(
      schemeProblem('MM', {
        ...terms,
        calculation: 'FIXED_PER_POLICY',
        tiers: [{ minBasicPremium: 10000, fixedAmount: 500 }],
      }),
    ).toBeUndefined();
    expect(schemeProblem('EMPTY', { ...terms, active: false, tiers: [] })).toBeUndefined();
  });

  it('refuses incomplete schemes', () => {
    expect(schemeProblem(' ', terms)).toMatch(/Code/);
    expect(schemeProblem('X', { ...terms, tiers: [{ minProduction: 1 }] })).toMatch(/Tier 1/);
    expect(
      schemeProblem('X', {
        ...terms,
        calculation: 'FIXED_PER_POLICY',
        tiers: [{ fixedAmount: 5 }],
      }),
    ).toMatch(/minimum basic premium/);
    expect(schemeProblem('X', { ...terms, tiers: [] })).toMatch(/at least one tier/);
    expect(
      schemeProblem('X', { ...terms, effectiveFrom: '2026-12-01', effectiveTo: '2026-01-01' }),
    ).toMatch(/ends before/);
  });

  it('describes tiers', () => {
    expect(
      describeTier('TARGET_TIERED', { minProduction: 100000, ratePercent: 2, multiplier: 1.5 }),
    ).toBe('From 100000: 2% × 1.5');
    expect(describeTier('TARGET_TIERED', { minProduction: 0, ratePercent: 1, multiplier: 1 })).toBe(
      'From 0: 1%',
    );
    expect(describeTier('FIXED_PER_POLICY', { minBasicPremium: 10000, fixedAmount: 500 })).toBe(
      'Basic premium from 10000: 500 per policy',
    );
  });
});

describe('certificates', () => {
  const cert: CertificateInput = {
    insurerCode: 'INS-MGIC',
    form: '2307',
    number: 'C-1',
    periodFrom: '2026-07-01',
    periodTo: '2026-09-30',
    taxWithheld: 237.91,
    receipts: [
      { orNo: 'OR-1', amount: 1000 },
      { orNo: 'OR-2', amount: 1379.13 },
    ],
  };

  it('totals the ORs', () => {
    expect(receiptsTotal(cert)).toBeCloseTo(2379.13);
    expect(receiptsTotal({ receipts: [{ orNo: 'X', amount: Number.NaN }] })).toBe(0);
  });

  it('checks a certificate before submission', () => {
    expect(certificateProblem(cert)).toBeUndefined();
    expect(certificateProblem({ ...cert, number: '' })).toMatch(/required/);
    expect(certificateProblem({ ...cert, periodTo: '2026-06-30' })).toMatch(/period/);
    expect(certificateProblem({ ...cert, taxWithheld: 0 })).toMatch(/above zero/);
    expect(certificateProblem({ ...cert, receipts: [] })).toMatch(/official receipt/);
  });
});
