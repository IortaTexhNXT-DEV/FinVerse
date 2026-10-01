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
});
