import { describe, expect, it } from 'vitest';
import type { BatchLine, DtipRow } from './api';
import { toQuery } from './api';
import {
  BATCH_TABS,
  batchTabOf,
  dtipFlags,
  HOLD_TABS,
  holdFormErrors,
  isExcluded,
  parseEmails,
  stagesOf,
  totalsOf,
} from './remittanceLabels';

function line(invoiceNo: string, paidAr: number, excluded = false): BatchLine {
  return {
    invoiceNo,
    arn: 'ARN-1',
    clientCode: 'CL-1',
    assuredName: 'Assured',
    inceptionDate: '2026-10-01',
    bookingDate: '2026-09-15',
    basicPremium: paidAr * 0.8,
    amounts: {
      paidAr,
      commission: 10.1,
      commissionVat: 1.21,
      wtax: 1.01,
      dtip: paidAr,
      incentive: 0,
      incentiveVat: 0,
      netDue: paidAr - 10.3,
      payable: paidAr - 10.3,
    },
    exclusion: excluded ? { excluded: true, reason: 'OTHERS' } : undefined,
  };
}

describe('remittance labels', () => {
  it('opens the batch tab named in the URL or the one of a stage', () => {
    expect(batchTabOf('FOR_APPROVAL')).toBe('APPROVAL');
    expect(batchTabOf('OR')).toBe('OR');
    expect(batchTabOf('PARTIALLY_REMITTED')).toBe('REMITTED');
    expect(batchTabOf(null)).toBe('REVIEW');
    expect(stagesOf(BATCH_TABS, 'REVIEW')).toEqual(['REVIEW_IN_PROCESS', 'ON_HOLD']);
    expect(stagesOf(HOLD_TABS, 'CLOSED')).toContain('RELEASED');
  });

  it('totals only the accounts kept, rounded to centavos', () => {
    const totals = totalsOf([line('A', 100.1), line('B', 200.2), line('C', 999, true)]);
    expect(totals.paidAr).toBeCloseTo(300.3, 2);
    expect(totals.commission).toBeCloseTo(20.2, 2);
    expect(totals.netDue).toBeCloseTo(279.7, 2);
    expect(isExcluded(line('C', 1, true))).toBe(true);
    expect(isExcluded(line('D', 1))).toBe(false);
  });

  it('validates the hold request form', () => {
    expect(holdFormErrors({ invoiceNo: ' ', reasonCode: '', holdUntil: '' }, '2026-09-24')).toEqual(
      {
        invoiceNo: 'Invoice No. is required',
        reasonCode: 'Select a reason',
        holdUntil: 'Hold Until is required',
      },
    );
    expect(
      holdFormErrors(
        { invoiceNo: 'INV', reasonCode: 'OTHERS', holdUntil: '2026-09-24' },
        '2026-09-24',
      ).holdUntil,
    ).toBe('Hold Until must be a future date');
    expect(
      holdFormErrors(
        { invoiceNo: 'INV', reasonCode: 'OTHERS', holdUntil: '2026-10-01' },
        '2026-09-24',
      ),
    ).toEqual({});
  });

  it('splits e-mail lists and flags invalid addresses', () => {
    expect(parseEmails('a@insurer.ph; b@insurer.ph, nope')).toEqual({
      valid: ['a@insurer.ph', 'b@insurer.ph'],
      invalid: ['nope'],
    });
  });

  it('shows invoice flags as chips', () => {
    const row = {
      hold: true,
      pendingNegativeAdjustment: true,
      writtenOff: true,
      lockOwner: 'ADJUSTMENT',
    } as DtipRow;
    expect(dtipFlags(row)).toEqual([
      'On Hold',
      'Pending Negative Adjustment',
      'Written Off',
      'Locked by ADJUSTMENT',
    ]);
    expect(
      dtipFlags({ hold: false, pendingNegativeAdjustment: false, writtenOff: false } as DtipRow),
    ).toEqual([]);
  });

  it('repeats array parameters in the query string', () => {
    expect(toQuery({ companyId: 1, stage: ['A', 'B'], q: '', page: 0 })).toBe(
      '?companyId=1&stage=A&stage=B&page=0',
    );
    expect(toQuery({ q: undefined })).toBe('');
  });
});
