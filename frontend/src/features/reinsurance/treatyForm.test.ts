import type { Treaty } from '@/api/reinsurance';
import {
  blankLayer,
  blankParticipant,
  blankTreaty,
  capacityLabel,
  toForm,
  toRequest,
  totalShare,
  treatyProblems,
} from './treatyForm';

describe('treaty form', () => {
  it('starts as a quota share of the year in base currency', () => {
    const f = blankTreaty(1, 'PHP', 2026);
    expect(f.periodFrom).toBe('2026-01-01');
    expect(f.periodTo).toBe('2026-12-31');
    expect(f.participants).toHaveLength(1);
    expect(blankParticipant().key).not.toBe(blankParticipant().key);
    expect(blankLayer().reinstatements).toBe(1);
  });

  it('reports missing terms and shares not totalling 100 %', () => {
    const f = blankTreaty(1, 'PHP', 2026);
    const problems = treatyProblems({ ...f, quotaSharePct: 0 });
    expect(problems).toContain('Code, name and class are required.');
    expect(problems.join(' ')).toContain('now 0 %');
    expect(problems).toContain('Select a reinsurer on every participant line.');
    expect(problems).toContain('Enter the quota share %.');
    expect(treatyProblems({ ...f, treatyType: 'SURPLUS' })).toContain(
      'Enter the retention (one line) and the number of lines.',
    );
    expect(treatyProblems({ ...f, treatyType: 'XOL' })).toContain('Add at least one layer.');
  });

  it('accepts a complete surplus treaty', () => {
    const f = {
      ...blankTreaty(1, 'PHP', 2026),
      code: 'SP',
      name: 'Surplus',
      businessLine: 'FIRE',
      treatyType: 'SURPLUS' as const,
      retentionLimit: 25_000_000,
      lines: 3,
      participants: [
        { reinsurerCode: 'R-1', sharePct: 60.5 },
        { reinsurerCode: 'R-2', sharePct: 39.5 },
      ],
    };
    expect(totalShare(f.participants)).toBe(100);
    expect(treatyProblems(f)).toEqual([]);
    const body = toRequest({ ...f, quotaSharePct: 40, brokerCode: '' });
    expect(body.quotaSharePct).toBeUndefined();
    expect(body.retentionLimit).toBe(25_000_000);
    expect(body.brokerCode).toBeUndefined();
    expect(body.layers).toEqual([]);
    expect(capacityLabel({ ...f, layers: [] })).toContain('3 lines of');
  });

  it('keeps the layers of an excess of loss treaty and edits existing treaties', () => {
    const layer = { priority: 500_000, limit: 2_000_000, minDepositPremium: 1, reinstatements: 1 };
    const treaty = {
      ...blankTreaty(1, 'PHP', 2026),
      id: 7,
      code: 'XL',
      treatyType: 'XOL' as const,
      statementFrequency: 'QUARTERLY',
      recordStatus: 'ACTIVE' as const,
      createdBy: 'reinsurer',
      participants: [{ lineNo: 1, reinsurerCode: 'R-1', reinsurerName: 'R', sharePct: 100 }],
      layers: [{ ...layer, layerNo: 1 }],
    } satisfies Treaty;
    const form = toForm(treaty);
    expect(form.layers[0]?.key).toBeDefined();
    expect(toRequest(form).layers).toEqual([layer]);
    expect(capacityLabel(treaty)).toBe('1 layer(s)');
    expect(capacityLabel({ ...treaty, treatyType: 'QUOTA_SHARE', quotaSharePct: 40 })).toBe(
      '40 % quota share',
    );
  });
});
