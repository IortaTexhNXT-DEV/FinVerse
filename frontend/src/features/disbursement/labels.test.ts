import { describe, expect, it } from 'vitest';
import { toQuery } from './api';
import type { Line, Payee } from './api';
import { fundingErrors } from './forms';
import {
  encodeErrors,
  entryTotals,
  instrumentActions,
  requestActions,
  workbenchTab,
  workbenchTabOf,
} from './labels';
import { EMPTY_PAYEE, accountErrors, payeeErrors, payeeFormOf, payeeInputOf } from './payeeForm';

const line = (side: Line['side'], amount: number): Line => ({ side, accountCode: '1000', amount });

describe('disbursement labels', () => {
  it('maps the workbench tab of the URL, defaulting to the system requests', () => {
    expect(workbenchTabOf('FOR_APPROVAL')).toBe('FOR_APPROVAL');
    expect(workbenchTabOf('nope')).toBe('REQUESTS');
    expect(workbenchTabOf(null)).toBe('REQUESTS');
    expect(workbenchTab('CLOSED').stages).toEqual(['CANCELLED', 'REJECTED']);
    expect(workbenchTab('NO_PAYEE').requestStatuses).toEqual(['NO_PAYEE']);
  });

  it('offers the instrument actions of each mode and status', () => {
    expect(instrumentActions('CHECK', 'PENDING')).toEqual(['print']);
    expect(instrumentActions('CHECK', 'PRINTED')).toEqual(['release']);
    expect(instrumentActions('CHECK', 'STALE')).toEqual(['reissue']);
    expect(instrumentActions('CTA', 'PENDING')).toEqual([]);
    expect(instrumentActions('ATD', 'PRINTED')).toEqual(['email']);
    expect(instrumentActions('ATD', 'EMAILED')).toEqual(['debited']);
    expect(instrumentActions('MC_DD', 'RECEIVED')).toEqual(['release']);
    expect(instrumentActions('ONLINE_BANKING', 'APPROVED')).toEqual(['debited']);
    expect(instrumentActions('CHECK', 'NEGOTIATED')).toEqual([]);
  });

  it('totals the proforma entry in cents', () => {
    expect(entryTotals([line('DEBIT', 0.1), line('DEBIT', 0.2), line('CREDIT', 0.3)])).toEqual({
      debit: 0.3,
      credit: 0.3,
      balanced: true,
    });
    expect(entryTotals([line('DEBIT', 10), line('CREDIT', 9.99)]).balanced).toBe(false);
    expect(entryTotals([]).balanced).toBe(false);
  });

  it('validates the encode form', () => {
    const ok = {
      disbursementType: 'SUPPLIER',
      payeeCode: 'S-1',
      currency: 'PHP',
      amount: '10',
      purpose: 'Rent',
    };
    expect(encodeErrors(ok)).toEqual({});
    const bad = encodeErrors({
      disbursementType: '',
      payeeCode: ' ',
      currency: 'ph',
      amount: '0',
      purpose: '',
    });
    expect(Object.keys(bad).sort((a, b) => a.localeCompare(b))).toEqual([
      'amount',
      'currency',
      'disbursementType',
      'payeeCode',
      'purpose',
    ]);
  });

  it('lists the request actions by status', () => {
    expect(requestActions('RECEIVED', 'REFUND')).toEqual(['voucher', 'return']);
    expect(requestActions('RECEIVED', 'CWT2307')).toEqual(['release', 'return']);
    expect(requestActions('NO_PAYEE', 'REMITTANCE')).toEqual(['voucher', 'return']);
    expect(requestActions('IN_VOUCHER', 'REFUND')).toEqual([]);
  });

  it('builds query strings without empty values and with repeated arrays', () => {
    expect(toQuery({ companyId: 1, stage: ['A', 'B'], q: '', x: undefined })).toBe(
      '?companyId=1&stage=A&stage=B',
    );
    expect(toQuery({})).toBe('');
  });
});

describe('payee form', () => {
  it('requires the code, class, name and a bank account for credit modes', () => {
    const errors = payeeErrors({ ...EMPTY_PAYEE, defaultMode: 'CTA', email: 'x' }, false);
    expect(Object.keys(errors).sort((a, b) => a.localeCompare(b))).toEqual([
      'defaultMode',
      'email',
      'name',
      'payeeClass',
      'payeeCode',
    ]);
    const ok = { ...EMPTY_PAYEE, payeeCode: 'S-1', payeeClass: 'SUPPLIER', name: 'Acme' };
    expect(payeeErrors(ok, false)).toEqual({});
  });

  it('adds the default mode to the allowed modes and drops blank fields', () => {
    const input = payeeInputOf(
      {
        ...EMPTY_PAYEE,
        payeeCode: 'S-1',
        name: ' Acme ',
        defaultMode: 'ATD',
        allowedModes: ['CHECK'],
      },
      3,
    );
    expect(input.allowedModes).toEqual(['CHECK', 'ATD']);
    expect(input.name).toBe('Acme');
    expect(input.email).toBeUndefined();
    expect(input.companyId).toBe(3);
  });

  it('reads the form of a maintained payee', () => {
    const payee: Payee = {
      summary: {
        id: 1,
        payeeCode: 'INS-1',
        payeeClass: 'INSURER',
        name: 'Insurer',
        defaultMode: 'CHECK',
        currency: 'PHP',
        source: 'MANUAL',
        stage: 'ACTIVE',
        used: true,
      },
      allowedModes: ['CHECK'],
      disbursementTypes: ['REMITTANCE'],
      accounts: [],
      createdBy: 'disbtl',
    };
    expect(payeeFormOf(payee)).toMatchObject({
      payeeCode: 'INS-1',
      payeeClass: 'INSURER',
      address: '',
    });
  });

  it('validates a bank account', () => {
    expect(
      accountErrors({
        bankName: '',
        accountNo: 'abc',
        accountName: '',
        currency: 'PHP',
        mode: 'CTA',
        primary: true,
      }),
    ).toEqual({
      bankName: 'Enter the bank',
      accountNo: 'Enter the account number (digits)',
      accountName: 'Enter the account name',
    });
  });
});

describe('funding form', () => {
  it('needs two different accounts, an amount, a purpose and a value date', () => {
    const ok = {
      source: '1',
      target: '2',
      amount: '100',
      currency: 'PHP',
      purpose: 'Top up',
      valueDate: '2026-09-25',
    };
    expect(fundingErrors(ok)).toEqual({});
    expect(
      Object.keys(
        fundingErrors({ ...ok, target: '1', amount: '0', purpose: ' ', valueDate: '' }),
      ).sort((a, b) => a.localeCompare(b)),
    ).toEqual(['amount', 'purpose', 'target', 'valueDate']);
  });
});
