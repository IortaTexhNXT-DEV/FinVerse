import { describe, expect, it } from 'vitest';
import { chargeProblems, chargeValueLabel, chargeVatLabel } from './otherCharges';
import { insurerTaxStatusLabel } from './insurerForm';

describe('other charges', () => {
  const valid = {
    chargeCode: 'DOC_FEE',
    name: 'Documentation fee',
    basis: 'AMOUNT' as const,
    value: 250,
    vatTreatment: 'VATABLE' as const,
    glAccountCode: '4100',
    effectiveFrom: '2027-01-01',
  };

  it('accepts a complete charge and lists the problems of an incomplete one', () => {
    expect(chargeProblems(valid)).toEqual({});
    const problems = chargeProblems({ basis: 'RATE', value: 120, chargeCode: 'doc fee' });
    expect(Object.keys(problems).sort((a, b) => a.localeCompare(b))).toEqual([
      'chargeCode',
      'effectiveFrom',
      'glAccountCode',
      'name',
      'value',
    ]);
    expect(chargeProblems({ ...valid, effectiveTo: '2026-12-31' }).effectiveTo).toBeDefined();
  });

  it('shows the amount or the rate and the VAT treatment in words', () => {
    expect(chargeValueLabel('RATE', 1.5)).toContain('% of the net premium');
    expect(chargeValueLabel('AMOUNT', 250)).toBe('250.00');
    expect(chargeVatLabel('EXEMPT')).toBe('VAT-exempt');
  });

  it('names the tax status of an insurer', () => {
    expect(insurerTaxStatusLabel('NON_VAT')).toBe('Not VAT-registered (premium tax)');
    expect(insurerTaxStatusLabel(undefined)).toBe('As the product line');
  });
});
