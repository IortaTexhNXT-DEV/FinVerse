import { describe, expect, it } from 'vitest';
import type { BatchLine, DtipRow } from './api';
import { toQuery } from './api';
import {
  BATCH_TABS,
  HOLD_TABS,
  batchLine,
  batchTabOf,
  deductionLine,
  dtipFlags,
  holdFormErrors,
  holdLine,
  isExcluded,
  parseEmails,
  reasonsText,
  specialLine,
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
      cpc2: 1,
      cpc2Vat: 0.12,
      payable: paidAr - 11.42,
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
    expect(totals.cpc2 + totals.cpc2Vat).toBeCloseTo(2.24, 2);
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
      'Locked by Adjustment',
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

  it('words the reasons of a due invoice that was not extracted', () => {
    expect(reasonsText('PENDING_NEG_ADJ,OTHERS')).toBe(
      'Pending negative adjustment, Locked by another team',
    );
    expect(reasonsText(undefined)).toBe('');
  });
});

describe('one sentence under the title of a remittance record', () => {
  it('reads as a sentence, never a dot-separated run of values', () => {
    const at = '2026-10-09T04:40:00+08:00';
    expect(batchLine('Mabuhay General Insurance Corp.', 1, at)).toBe(
      'Remittance to Mabuhay General Insurance Corp.: 1 account extracted on 09-Oct-2026 04:40.',
    );
    expect(holdLine('BI-HO-2026-000008', at)).toBe(
      'Hold on invoice BI-HO-2026-000008, requested on 09-Oct-2026 04:40.',
    );
    expect(deductionLine('ACSL-2026-000001', 'BI-HO-2026-000002', at)).toBe(
      'Deduction ACSL-2026-000001 on invoice BI-HO-2026-000002, created on 09-Oct-2026 04:40.',
    );
    expect(specialLine('BI-HO-2026-000003', at, 'Client paid the insurer directly')).toBe(
      'Special remittance of invoice BI-HO-2026-000003, requested on 09-Oct-2026 04:40. Client paid the insurer directly',
    );
    for (const line of [batchLine('X', 2, at), holdLine('Y', at)]) {
      expect(line).not.toContain('·');
    }
  });
});
