import { describe, expect, it } from 'vitest';
import { INVOICE_COLUMNS } from './invoiceColumns';

describe('invoice search columns', () => {
  it('pairs the amounts and the statuses as the FRS columns, so the list fits its card', () => {
    expect(INVOICE_COLUMNS.map((c) => c.header)).toEqual([
      'Invoice No.',
      'Assured / Client Code',
      'Insurer',
      'Booked / Inception',
      'Account Officer',
      'Gross Premium / Outstanding',
      'Status / Remittance',
      'Flags',
    ]);
  });
});
