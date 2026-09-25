import { describe, expect, it } from 'vitest';
import {
  EMPTY_DEDUCTION,
  appliedPercent,
  deductionErrors,
  formOf,
  inputOf,
  isConfirmedByInsurer,
} from './deductionForm';
import type { Deduction } from './deductionsApi';

const deduction: Deduction = {
  id: 7,
  deductionNo: 'RDN-2026-000001',
  insurerCode: 'INS-LAC',
  currency: 'PHP',
  sourceType: 'AR_INSURER_REFUND',
  sourceRef: 'ARI-1',
  amount: 1250,
  appliedAmount: 500,
  remaining: 750,
  stage: 'CONFIRMED',
  createdBy: 'acsl',
  createdAt: '2026-09-20T01:00:00Z',
};

describe('deduction form', () => {
  it('requires the insurer, source, reference and a positive amount', () => {
    const errors = deductionErrors({ ...EMPTY_DEDUCTION, currency: '' }, '2026-09-25');
    expect(Object.keys(errors).sort((a, b) => a.localeCompare(b))).toEqual([
      'amount',
      'currency',
      'insurerCode',
      'sourceRef',
      'sourceType',
    ]);
    expect(
      deductionErrors({ ...formOf(deduction), amount: '10.005' }, '2026-09-25').amount,
    ).toBeDefined();
  });

  it('refuses a confirmation dated in the future', () => {
    const form = { ...formOf(deduction), confirmationDate: '2026-09-26' };
    expect(deductionErrors(form, '2026-09-25')).toEqual({
      confirmationDate: 'The confirmation date cannot be in the future',
    });
  });

  it('builds the request with blanks left out', () => {
    const input = inputOf(1, { ...formOf(deduction), insurerCode: ' ins-lac ', remarks: ' ' });
    expect(input).toMatchObject({ companyId: 1, insurerCode: 'INS-LAC', amount: 1250 });
    expect(input.remarks).toBeUndefined();
    expect(input.invoiceNo).toBeUndefined();
  });

  it('knows whether the insurer confirmation is recorded and how much is applied', () => {
    expect(isConfirmedByInsurer(deduction)).toBe(false);
    expect(
      isConfirmedByInsurer({
        ...deduction,
        confirmationRef: 'L-1',
        confirmationDate: '2026-09-20',
      }),
    ).toBe(true);
    expect(appliedPercent(deduction)).toBe(40);
    expect(appliedPercent({ amount: 0, appliedAmount: 0 })).toBe(0);
  });
});
