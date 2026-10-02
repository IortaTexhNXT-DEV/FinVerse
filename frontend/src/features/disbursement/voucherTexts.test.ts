/** The summary of a voucher names the payee, not its code. */
import { describe, expect, it } from 'vitest';
import page from './VoucherPage.tsx?raw';
import proforma from './ProformaTab.tsx?raw';
import details from './VoucherDetailsTab.tsx?raw';

describe('voucher summary', () => {
  it('shows the payee by name', () => {
    expect(page).toContain("label: 'Payee', value: s.payeeName");
  });
});

describe('proforma entry wording', () => {
  it('says the entry is prepared again from the rule, in business words', () => {
    expect(proforma).toContain("'Entry prepared again from the rule'");
    expect(proforma).toContain('The rule could not prepare the entry yet; complete the details.');
    expect(details).toContain("'Terms saved; the entry was prepared again from the rule'");
    for (const source of [proforma, details]) {
      expect(source).not.toMatch(/\b(rebuilt|could not build)\b/);
    }
  });
});
