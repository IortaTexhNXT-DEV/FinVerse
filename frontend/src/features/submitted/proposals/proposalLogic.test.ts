import { describe, expect, it } from 'vitest';
import type { ProposedLine } from '@/api/submittedProposals';
import { applyRate, generationLines, premiumAt } from './proposalLogic';

const line = (
  policyId: number,
  nominatedRate: number | null,
  vehicleType = 'CAR',
): ProposedLine => ({
  policyId,
  sbmNo: `SBM-${String(policyId)}`,
  assuredName: 'Assured',
  segment: 'NONCBG_RETAIL',
  vehicleType,
  sumInsured: 900000,
  defaultInsurer: 'INS-A',
  insurerCode: 'INS-A',
  nominatedRate,
  premium: null,
  problem: null,
});

describe('renewal proposals', () => {
  it('applies an edited rate only to the chosen vehicle classification', () => {
    const edits = applyRate(
      {},
      [line(1, 0.25), line(2, 0.25, 'TRUCK')],
      { rate: '0.3', reason: 'Bank' },
      { vehicleType: 'TRUCK' },
    );
    expect(Object.keys(edits)).toEqual(['2']);
  });

  it('needs a rate for every record and a reason for a changed rate', () => {
    expect(generationLines([line(1, null)], {}).problem).toMatch(/no nominated rate/);
    expect(generationLines([line(1, 0.25)], { 1: { rate: '0.3', reason: '' } }).problem).toMatch(
      /reason/,
    );
    expect(
      generationLines([line(1, 0.25)], { 1: { rate: '0.3', reason: 'Bank rate' } }).lines,
    ).toEqual([{ policyId: 1, rate: 0.3, reason: 'Bank rate' }]);
    expect(generationLines([line(1, 0.25)], {}).lines).toEqual([
      { policyId: 1, rate: undefined, reason: undefined },
    ]);
  });

  it('computes the premium in percent of the sum insured', () => {
    expect(premiumAt(900000, 0.25)).toBe(2250);
    expect(premiumAt(null, 0.25)).toBeNull();
  });
});
