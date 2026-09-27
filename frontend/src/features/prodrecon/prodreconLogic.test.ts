import { describe, expect, it } from 'vitest';
import {
  addresses,
  bucketTabs,
  matchedPercent,
  mayPair,
  maySplit,
  monthLabel,
  monthRange,
  scheduleProblem,
  sideBySide,
  validAddresses,
} from './prodreconLogic';

const counts = { matched: 6, discrepancy: 1, bdoiOnly: 2, insurerOnly: 1, total: 10 };

describe('sideBySide', () => {
  it('flags the fields the matcher reported and visible differences', () => {
    const rows = sideBySide({
      bdoi: { policyNo: 'POL-1', assuredName: 'Juan Dela Cruz', grossPremium: 1000 },
      insurer: { policyNo: 'pol-1', assuredName: 'Juan Cruz', grossPremium: 1000.5 },
      discrepancies: ['GROSS_PREMIUM'],
    });
    const byKey = Object.fromEntries(rows.map((r) => [r.key, r]));
    expect(byKey.policyNo?.differs).toBe(false);
    expect(byKey.assuredName?.differs).toBe(true);
    expect(byKey.grossPremium?.differs).toBe(true);
    expect(byKey.grossPremium?.bdoi).toBe('1000.00');
    expect(byKey.pnNo?.differs).toBe(false);
  });

  it('shows one side only for unpaired items', () => {
    const rows = sideBySide({ insurer: { policyNo: 'P' }, discrepancies: [] });
    expect(rows.find((r) => r.key === 'policyNo')).toMatchObject({ bdoi: '', insurer: 'P' });
    expect(rows.every((r) => !r.differs)).toBe(true);
  });
});

describe('cycle counts', () => {
  it('labels the buckets with their counts', () => {
    expect(bucketTabs(counts).map((t) => t.label)).toEqual([
      'All (10)',
      'Matched (6)',
      'With Discrepancy (1)',
      'BDOI Only (2)',
      'Insurer Only (1)',
    ]);
  });

  it('counts discrepancies as matched', () => {
    expect(matchedPercent(counts)).toBe(70);
    expect(matchedPercent({ ...counts, total: 0 })).toBe(0);
  });
});

describe('months', () => {
  it('formats and spans a production month', () => {
    expect(monthLabel('2026-09-01')).toBe('Sep 2026');
    expect(monthLabel(undefined)).toBe('');
    expect(monthRange('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(monthRange('2028-02')).toEqual({ from: '2028-02-01', to: '2028-02-29' });
    expect(monthRange('2026-12').to).toBe('2026-12-31');
  });
});

describe('addresses', () => {
  it('splits and validates recipient lists', () => {
    expect(addresses('a@x.ph; b@y.ph, c@z.ph')).toEqual(['a@x.ph', 'b@y.ph', 'c@z.ph']);
    expect(validAddresses('a@x.ph b@y.ph')).toBe(true);
    expect(validAddresses('a@x')).toBe(false);
    expect(validAddresses('')).toBe(false);
  });
});

describe('item actions', () => {
  it('pairs BDOI-only items and splits paired ones', () => {
    expect(mayPair({ status: 'BDOI_ONLY' })).toBe(true);
    expect(mayPair({ status: 'MATCHED' })).toBe(false);
    expect(maySplit({ status: 'MATCHED_WITH_DISCREPANCY' })).toBe(true);
    expect(maySplit({ status: 'UNMATCHED_NO_BOOKING' })).toBe(false);
  });
});

describe('scheduleProblem', () => {
  const base = {
    companyId: 1,
    insurerCode: 'INS-MGIC',
    frequency: 'MONTHLY' as const,
    runDay: 5,
    autoSend: false,
    recipients: '',
    active: true,
  };

  it('accepts a valid schedule', () => {
    expect(scheduleProblem(base)).toBeUndefined();
    expect(scheduleProblem({ ...base, frequency: 'WEEKLY', runDay: 7 })).toBeUndefined();
  });

  it('refuses missing insurers, days out of range and bad addresses', () => {
    expect(scheduleProblem({ ...base, insurerCode: ' ' })).toMatch(/Insurer/);
    expect(scheduleProblem({ ...base, runDay: 29 })).toMatch(/1 to 28/);
    expect(scheduleProblem({ ...base, frequency: 'WEEKLY', runDay: 8 })).toMatch(/Monday/);
    expect(scheduleProblem({ ...base, autoSend: true, recipients: 'nope' })).toMatch(/e-mail/);
  });
});
