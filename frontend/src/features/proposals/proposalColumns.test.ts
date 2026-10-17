import { describe, expect, it } from 'vitest';
import { PROPOSAL_COLUMNS } from './proposalColumns';

describe('PRF list columns', () => {
  it('stacks the chosen insurer under the slips so that the stage stays in view', () => {
    expect(PROPOSAL_COLUMNS.map((c) => c.header)).toEqual([
      'PRF No.',
      'ARN',
      'Client',
      'Product',
      'Sum Insured',
      'Slips / Chosen Insurer',
      'Stage',
    ]);
  });
});
