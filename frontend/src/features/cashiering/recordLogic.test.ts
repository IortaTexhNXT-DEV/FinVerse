import { describe, expect, it } from 'vitest';
import type { AccountLine, RecordForm } from './recordLogic';
import {
  bodyOf,
  currenciesOf,
  defaultBankAccount,
  emptyForm,
  excessOf,
  formOf,
  recordActions,
  recordErrors,
  recordTotals,
} from './recordLogic';
import type { FormSettings, ReceiptRecord } from './recordsApi';

const BASE = 'PHP';
const FX = 'USD';

const settings: FormSettings = {
  arPostingStep: true,
  orPostingStep: true,
  remarksRequired: true,
  maxAmount: 1_000_000_000,
  latestCheckDate: '2026-10-05',
  holdingDays: 4,
  bankAccounts: [
    {
      code: 'DFLB_CURRENT_ORTIGAS',
      name: 'DFLB - Current (BDO Ortigas)',
      currency: BASE,
      defaultForCurrency: false,
    },
    {
      code: 'DFLB_SAVINGS_ORTIGAS',
      name: 'DFLB Savings (BDO Ortigas)',
      currency: BASE,
      defaultForCurrency: true,
    },
    {
      code: 'DFLB_SAVINGS_FX',
      name: 'DFLB Savings FX (BDO Ortigas)',
      currency: FX,
      defaultForCurrency: true,
    },
  ],
  arBranches: [
    { id: 1, code: 'HO', name: 'Head Office' },
    { id: 2, code: 'CEB', name: 'Cebu' },
  ],
  orBranches: [{ id: 1, code: 'HO', name: 'Head Office' }],
  usdRate: 56.12,
  today: '2026-10-09',
};

const line = (outstanding: number, amount: string, pr2307?: number): AccountLine => ({
  row: {
    invoiceNo: 'BI-1',
    arn: 'ARN-1',
    outstanding,
    pr2307,
    bir2307: pr2307 !== undefined,
    prebooked: false,
  },
  amount,
});

function filled(): RecordForm {
  return {
    ...emptyForm('AR', settings, 2, BASE),
    clientName: 'Maria Santos',
    payorName: 'Maria Santos',
    amount: '1500',
    remarks: 'Counter payment',
    accounts: [line(12200, '1500')],
  };
}

describe('Create AR / OR record logic (FRS.CSH.02.01)', () => {
  it('defaults the receipting branch to the branch of the user and the bank account by currency', () => {
    const form = emptyForm('AR', settings, 2, BASE);
    expect(form.branchId).toBe('2');
    expect(form.bankAccount).toBe('DFLB_SAVINGS_ORTIGAS');
    expect(defaultBankAccount(settings.bankAccounts, FX)).toBe('DFLB_SAVINGS_FX');
    expect(currenciesOf(settings.bankAccounts)).toEqual([BASE, FX]);
    expect(emptyForm('OR', settings, 2, BASE).branchId).toBe('1');
    expect(emptyForm('OR', settings, 2, BASE).entryType).toBe('INSURER');
  });

  it('computes the excess less the PR 2307 and the totals per receipt record', () => {
    expect(excessOf(line(10000, '10050'))).toBe(50);
    expect(excessOf(line(10000, '9800', 200))).toBe(0);
    expect(excessOf(line(10000, '10000', 200))).toBe(200);
    expect(recordTotals([line(10000, '10050'), line(500, '300')])).toEqual({
      paid: 10350,
      excess: 50,
    });
  });

  it('refuses a record without Post to Bank Account, remarks or a valid amount', () => {
    expect(recordErrors(filled(), settings)).toEqual({});
    const errors = recordErrors(
      { ...filled(), bankAccount: '', remarks: '', amount: '0' },
      settings,
    );
    expect(errors.bankAccount).toBe('Post to Bank Account is required');
    expect(errors.remarks).toBe('Remarks is required');
    expect(errors.amount).toBe('Paid Amount must be above zero and not more than 1,000,000,000.00');
    expect(recordErrors({ ...filled(), amount: '1500000000' }, settings).amount).toBeDefined();
    expect(recordErrors({ ...filled(), amount: '2000' }, settings).accounts).toBe(
      'The amounts of the accounts must add up to the Paid Amount',
    );
  });

  it('needs the check number and date of a check and the insurer of a non-premium AR', () => {
    const check = recordErrors({ ...filled(), tenderType: 'CHECK' }, settings);
    expect(check.checkNo).toBe('Check Number is required');
    expect(check.checkDate).toBe('Check Date is required');
    const refund = recordErrors({ ...filled(), receiptType: 'REFUND', accounts: [] }, settings);
    expect(refund.insurerCode).toBe('Insurer is required');
  });

  it('sends the check only for a check payment and keeps the accounts with their amounts', () => {
    const body = bodyOf(9, { ...filled(), checkNo: 'CHK-1' });
    expect(body.checkNo).toBeUndefined();
    expect(body.accounts).toEqual([{ reference: 'BI-1', amount: 1500 }]);
    expect(body.branchId).toBe(2);
  });

  it('reads a saved record back into the form and offers the actions of the user', () => {
    const record: ReceiptRecord = {
      id: 5,
      recordNo: 'CR-AR-000012',
      recordKind: 'CREATION',
      receiptKind: 'AR',
      receiptType: 'PREMIUM',
      stage: 'FOR_POSTING',
      statusLabel: 'For Posting',
      editable: false,
      branchId: 1,
      party: { entryType: 'CLIENT', clientName: 'Maria Santos', payorName: 'Maria Santos' },
      tender: {
        tenderType: 'CASH',
        currency: BASE,
        bankAccount: 'DFLB_SAVINGS_ORTIGAS',
        amount: 1500,
      },
      accounts: [{ reference: 'BI-1', amount: 1500 }],
      accountsText: 'BI-1',
      total: 1500,
      createdByUser: 'cashier',
      createdAt: '2026-10-09T01:00:00Z',
    };
    const form = formOf(record, settings, BASE);
    expect(form.amount).toBe('1500');
    expect(form.accounts[0]?.amount).toBe('1500');
    const all = () => true;
    expect(recordActions(record, 'cashier', all).post).toBe(false);
    expect(recordActions(record, 'cashtl', all).post).toBe(true);
    expect(
      recordActions({ ...record, stage: 'CREATED', editable: true }, 'cashier', all),
    ).toMatchObject({
      edit: true,
      submit: true,
      cancel: true,
    });
  });
});
