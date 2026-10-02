/** The summary of a voucher names the payee, not its code. */
import { describe, expect, it } from 'vitest';
import page from './VoucherPage.tsx?raw';

describe('voucher summary', () => {
  it('shows the payee by name', () => {
    expect(page).toContain("label: 'Payee', value: s.payeeName");
  });
});
