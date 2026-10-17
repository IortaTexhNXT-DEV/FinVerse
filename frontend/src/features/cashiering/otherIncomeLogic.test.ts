import { describe, expect, it } from 'vitest';
import { otherIncomeParty } from './otherIncomeLogic';

describe('OR of other income (FRS.CSH.02.02.08)', () => {
  it('offers the SOAs of the insurer for an incentive and the fee invoices of the client for a fee', () => {
    const form = { insurerCode: 'INS-1', clientCode: 'CL-9' };
    expect(otherIncomeParty({ ...form, receiptType: 'INCENTIVE' })).toBe('INS-1');
    expect(otherIncomeParty({ ...form, receiptType: 'SERVICE_FEE' })).toBe('CL-9');
    expect(otherIncomeParty({ ...form, receiptType: 'OTHERS' })).toBe('');
  });
});
