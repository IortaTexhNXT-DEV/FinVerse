import { describe, expect, it } from 'vitest';
import {
  approveLabel,
  businessActions,
  cashAdvanceErrors,
  cashAdvanceInput,
  dayTotal,
  daysErrors,
  emptyLine,
  expenseInputs,
  isAmount,
  linesTotal,
  refundErrors,
  refundInput,
  submitLabel,
  tabOf,
} from './requestForm';
import type { CashAdvanceDraft, DayDraft, LineDraft } from './requestForm';

const line = (over: Partial<LineDraft>): LineDraft => ({
  ...emptyLine('CL-1', 'Juan'),
  arNo: 'AR-1',
  amount: '100.50',
  reasonCode: 'OVERPAYMENT',
  ...over,
});

const check = { paymentMode: 'CHECK', accountNo: '', accountName: 'Juan Dela Cruz' };

describe('refund request form', () => {
  it('accepts a complete form and totals the lines', () => {
    const lines = [line({}), line({ arNo: 'AR-2', amount: '10' })];
    expect(refundErrors(check, lines)).toEqual({});
    expect(linesTotal(lines)).toBeCloseTo(110.5);
    const input = refundInput(
      { segment: 'CBG', referenceText: ' ', requestingUnit: '', purpose: 'Refund' },
      check,
      lines,
    );
    expect(input.referenceText).toBeUndefined();
    expect(input.lines[0]?.amount).toBe(100.5);
    expect(input.lines[1]?.accountName).toBe('Juan Dela Cruz');
  });

  it('flags duplicate ARs, bad amounts, missing reasons and mixed clients', () => {
    const errors = refundErrors(check, [
      line({}),
      line({ amount: '1.234', reasonCode: '' }),
      line({ arNo: 'AR-3', clientCode: 'CL-2' }),
    ]);
    expect(errors.arNo1).toBe('Duplicate AR');
    expect(errors.amount1).toBeDefined();
    expect(errors.reasonCode1).toBe('Required');
    expect(errors.lines).toContain('one client');
    expect(refundErrors(check, []).lines).toBeDefined();
    expect(refundErrors(check, [line({ arNo: ' ' })]).arNo0).toBe('Required');
  });

  it('checks the credit to account details', () => {
    const cta = { paymentMode: 'CTA', accountNo: '12AB', accountName: '' };
    const errors = refundErrors(cta, [line({})]);
    expect(errors.accountNo).toBeDefined();
    expect(errors.accountName).toBe('Enter the account name');
    expect(refundErrors({ ...cta, paymentMode: '' }, [line({})]).paymentMode).toBeDefined();
  });
});

describe('cash advance form', () => {
  const draft: CashAdvanceDraft = {
    paymentMode: 'CTA',
    accountNo: '001234567890',
    accountName: 'Ana',
    employeeNo: 'E-1',
    employeeName: 'Ana',
    rfpType: '',
    purpose: 'Field work',
    amount: '5000',
    segment: '',
    requestingUnit: '',
  };

  it('builds the request of a valid form', () => {
    expect(cashAdvanceErrors(draft)).toEqual({});
    expect(cashAdvanceInput(draft)).toMatchObject({ rfpType: 'CASH_ADVANCE', amount: 5000 });
  });

  it('lists the missing fields', () => {
    const errors = cashAdvanceErrors({
      ...draft,
      employeeNo: '',
      employeeName: '',
      purpose: '',
      amount: '0',
    });
    expect(Object.keys(errors)).toEqual(['employeeNo', 'employeeName', 'purpose', 'amount']);
  });
});

describe('liquidation days', () => {
  const day: DayDraft = {
    fieldworkDate: '2026-09-01',
    particulars: 'Client visit',
    perDiem: '300',
    representation: '',
    transport: '120.50',
    lodging: '',
    others: '',
  };

  it('totals and converts the days', () => {
    expect(dayTotal(day)).toBeCloseTo(420.5);
    expect(daysErrors([day])).toEqual({});
    expect(expenseInputs([day])[0]).toMatchObject({ perDiem: 300, lodging: 0 });
  });

  it('flags incomplete days and bad amounts', () => {
    expect(daysErrors([{ ...day, particulars: '' }]).day0).toContain('required');
    expect(daysErrors([{ ...day, lodging: '-1' }]).day0).toContain('two decimals');
  });
});

describe('work list and actions', () => {
  it('reads the tab of the URL', () => {
    expect(tabOf('FOR_REVIEW')).toBe('FOR_REVIEW');
    expect(tabOf('NOPE')).toBe('ALL');
    expect(tabOf(null)).toBe('ALL');
  });

  it('offers one button per business action and labels them by stage', () => {
    expect(businessActions(['submit_for_validation', 'submit', 'return', 'approve'])).toEqual([
      'submit',
      'approve',
    ]);
    expect(submitLabel('REFUND', true)).toBe('Send for Validation');
    expect(submitLabel('CASH_ADVANCE', false)).toBe('Submit for Review');
    expect(approveLabel('HR_APPROVAL', 'CASH_ADVANCE')).toContain('HR');
    expect(approveLabel('FOR_APPROVAL', 'CASH_ADVANCE')).toBe('Approve and Send to HR');
    expect(approveLabel('FOR_APPROVAL', 'REFUND')).toBe('Approve and Send');
    expect(isAmount('12.3')).toBe(true);
    expect(isAmount('0')).toBe(false);
  });
});
