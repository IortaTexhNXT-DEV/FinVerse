import { describe, expect, it } from 'vitest';
import { BIR_OUTPUT_GROUPS, initialChoice, outputParams, periodLabel } from './birOutputs';
import type { BirOutput } from './birOutputs';
import { draftErrors, draftTax, emptyDraft, toBody } from './receivedCertificates';

const output = (period: BirOutput['period']): BirOutput => ({
  code: 'TAX-X',
  title: 'X',
  text: 'x',
  period,
});

describe('BIR outputs', () => {
  it('lists every new output once', () => {
    const codes = BIR_OUTPUT_GROUPS.flatMap((g) => g.outputs.map((o) => o.code));
    expect(new Set(codes).size).toBe(codes.length);
    expect(codes).toContain('TAX-SAWT');
    expect(codes).toContain('IC-BROKER-ASBO');
  });

  it('builds the parameters and label of each period', () => {
    const c = initialChoice('2026-08-15');
    expect(c).toMatchObject({ year: '2026', quarter: '3', month: '8', from: '2026-01-01' });
    expect(outputParams(output('MONTH'), 1, c)).toEqual({
      companyId: '1',
      year: '2026',
      month: '8',
    });
    expect(outputParams(output('QUARTER'), 1, c).quarter).toBe('3');
    expect(outputParams(output('RANGE'), 1, c)).toMatchObject({
      fromDate: '2026-01-01',
      toDate: '2026-08-15',
    });
    expect(outputParams(output('LEDGER'), 1, c).accountClass).toBe('ASSET');
    expect(periodLabel(output('MONTH'), c)).toBe('2026-08');
    expect(periodLabel(output('QUARTER'), c)).toBe('Q3 2026');
    expect(periodLabel(output('YEAR'), c)).toBe('2026');
    expect(periodLabel(output('RANGE'), c)).toBe('2026-01-01 to 2026-08-15');
    expect(periodLabel(output('LEDGER'), c)).toContain('ASSET');
  });
});

describe('certificates received', () => {
  it('validates a certificate and builds its body', () => {
    const empty = emptyDraft('2026-09-25');
    expect(
      Object.keys(draftErrors(empty, '2026-09-25')).sort((a, b) => a.localeCompare(b)),
    ).toEqual(['agentCode', 'agentName', 'certificateNo', 'lines']);
    const draft = {
      ...empty,
      certificateNo: ' 2307-1 ',
      agentCode: 'INS-1',
      agentName: 'Insurer',
      lines: [
        { kind: 'COMMISSION' as const, atc: 'WC158', incomeNature: '', income: '1000', tax: '20' },
        {
          kind: 'INCENTIVE' as const,
          atc: 'WC158',
          incomeNature: 'Incentive',
          income: '500',
          tax: '10',
        },
      ],
    };
    expect(draftErrors(draft, '2026-09-25')).toEqual({});
    expect(
      draftErrors({ ...draft, receivedOn: '2026-10-01' }, '2026-09-25').receivedOn,
    ).toBeDefined();
    expect(draftErrors({ ...draft, periodTo: '2026-08-01' }, '2026-09-25').periodTo).toBeDefined();
    expect(draftTax(draft)).toBe(30);
    const body = toBody(draft);
    expect(body.certificateNo).toBe('2307-1');
    expect(body.agentTin).toBeUndefined();
    expect(body.lines[1]).toEqual({
      kind: 'INCENTIVE',
      atc: 'WC158',
      incomeNature: 'Incentive',
      income: 500,
      tax: 10,
    });
  });
});
