import { describe, expect, it } from 'vitest';
import { QUOTATION_COLUMNS } from './quotationColumns';

describe('quotation work list columns', () => {
  it('stacks the insurer under the product so that every column fits the window', () => {
    expect(QUOTATION_COLUMNS.map((c) => c.header)).toEqual([
      'Quotation No.',
      'ARN',
      'Client',
      'Product / Insurer',
      'Gross Premium',
      'Valid Until',
      'Status',
    ]);
  });

  it('keeps the valid-until date on one line', () => {
    expect(QUOTATION_COLUMNS.find((c) => c.key === 'valid')?.kind).toBe('date');
  });
});
