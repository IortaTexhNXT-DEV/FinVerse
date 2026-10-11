import { describe, expect, it } from 'vitest';
import { exemptionLabel, taxTypeLabel, withholdingLabel } from './taxDisplay';

describe('party tax status', () => {
  it('names the strongest withholding status', () => {
    expect(withholdingLabel({ withholdingAgent: true, governmentPayor: true })).toBe(
      'Government payor',
    );
    expect(withholdingLabel({ withholdingAgent: true, topWithholdingAgent: true })).toBe(
      'Top withholding agent',
    );
    expect(withholdingLabel({ withholdingAgent: true })).toBe('Withholding agent');
    expect(withholdingLabel({})).toBe('');
  });

  it('shows the exemption certificate with its validity', () => {
    const fmt = (iso: string) => `d:${iso}`;
    expect(
      exemptionLabel(
        {
          exemptionCertificateNo: 'TEC-1',
          exemptionValidFrom: '2027-01-01',
          exemptionValidTo: '2027-12-31',
        },
        fmt,
      ),
    ).toBe('TEC-1 (d:2027-01-01 to d:2027-12-31)');
    expect(exemptionLabel({}, fmt)).toBe('');
  });

  it('names the final taxes and the percentage tax', () => {
    expect(taxTypeLabel('FWT')).toBe('Final withholding tax');
    expect(taxTypeLabel('FINAL_VAT')).toBe('Final VAT withheld');
    expect(taxTypeLabel('PERCENTAGE_TAX')).toBe('Percentage tax');
  });
});
