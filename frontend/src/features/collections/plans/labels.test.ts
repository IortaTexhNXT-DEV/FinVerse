import { describe, expect, it } from 'vitest';
import {
  PLAN_TABS,
  PROMISE_TABS,
  optionalAmount,
  optionalText,
  parseInvoiceList,
  planFormErrors,
  promiseFormErrors,
  statusesOf,
} from './labels';

describe('plans and promises labels', () => {
  it('maps tabs to their statuses', () => {
    expect(statusesOf(PLAN_TABS, 'ACTIVE')).toEqual(['ACTIVE']);
    expect(statusesOf(PROMISE_TABS, 'KEPT')).toEqual(['KEPT', 'PARTIALLY_KEPT']);
  });

  it('reads a pasted list of invoices without blanks or repeats', () => {
    expect(parseInvoiceList('BI-1, BI-2\nBI-1;  BI-3 ')).toEqual(['BI-1', 'BI-2', 'BI-3']);
    expect(parseInvoiceList('   ')).toEqual([]);
  });

  it('checks the new plan form per kind', () => {
    const base = {
      kind: 'POLICY_YEARS' as const,
      arn: '',
      invoiceNo: '',
      frequency: '',
      firstDue: '',
      count: '',
    };
    expect(planFormErrors(base)).toEqual({
      frequency: 'Choose the billing frequency',
      arn: 'Enter the account reference number',
    });
    expect(planFormErrors({ ...base, arn: 'ARN-1', frequency: 'ANNUAL' })).toEqual({});
    const generated = planFormErrors({
      ...base,
      kind: 'GENERATED',
      frequency: 'QUARTERLY',
      count: '0',
    });
    expect(Object.keys(generated)).toEqual(['invoiceNo', 'firstDue', 'count']);
    expect(
      planFormErrors({
        ...base,
        kind: 'GENERATED',
        frequency: 'QUARTERLY',
        invoiceNo: 'BI-1',
        firstDue: '2026-10-01',
        count: '4',
      }),
    ).toEqual({});
  });

  it('checks the promise form', () => {
    const today = '2026-09-25';
    expect(
      promiseFormErrors(
        { invoiceNo: '', promisedOn: '2026-09-30', promisedDate: '', amount: '-1' },
        today,
      ),
    ).toEqual({
      invoiceNo: 'Enter at least one invoice number',
      promisedOn: 'The day of the promise cannot be in the future',
      promisedDate: 'Enter the promised payment date',
      amount: 'Enter a positive amount, or leave it empty for the whole outstanding',
    });
    expect(
      promiseFormErrors(
        { invoiceNo: 'BI-1', promisedOn: '2026-09-20', promisedDate: '2026-09-19', amount: '' },
        today,
      ),
    ).toEqual({ promisedDate: 'The promised date cannot be before the day of the promise' });
    expect(
      promiseFormErrors(
        { invoiceNo: 'BI-1', promisedOn: '', promisedDate: '2026-10-01', amount: '100' },
        today,
      ),
    ).toEqual({});
  });

  it('reads optional fields', () => {
    expect(optionalAmount(' ')).toBeUndefined();
    expect(optionalAmount('12.5')).toBe(12.5);
    expect(optionalText('  ')).toBeUndefined();
    expect(optionalText(' x ')).toBe('x');
  });
});
