import { describe, expect, it } from 'vitest';
import { ACCOUNT_COLUMNS } from './accountColumns';

describe('account list columns', () => {
  it('stacks the insurer under the product and the flags under the status', () => {
    expect(ACCOUNT_COLUMNS.map((c) => c.header)).toEqual([
      'ARN',
      'Client',
      'Product / Insurer',
      'Period',
      'Gross Premium',
      'Status / Flags',
      'Officer',
    ]);
  });

  it('keeps the officer on one line within the list, the full name in the tooltip', () => {
    const officer = ACCOUNT_COLUMNS.find((c) => c.header === 'Officer');
    expect(officer?.truncate).toBe(true);
    expect(officer?.width).toBe('180px');
  });
});
