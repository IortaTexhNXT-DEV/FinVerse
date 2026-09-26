import { describe, expect, it } from 'vitest';
import type { PaymentPreview, PdcItem, ReceiptDetail } from './cashieringApi';
import {
  ageBucket,
  allocationRows,
  byMaturityMonth,
  componentLabel,
  dispositionErrors,
  dispositionFields,
  journalRows,
  matchMessage,
  monthLabel,
  parseReferences,
  previewTotals,
  receiveErrors,
  reinstateErrors,
  round2,
  runTiles,
  seriesUsedPercent,
} from './cashieringLogic';

const preview: PaymentPreview = {
  match: 'BOOKED',
  reference: 'ARN-1',
  invoices: [
    {
      invoiceNo: 'BI-1',
      arn: 'ARN-1',
      currency: 'PHP',
      cwt: true,
      receivable: true,
      outstanding: 1252.5,
      balances: { BASIC: 1000, DST: 125, PREMIUM_TAX_VAT: 120, LGT: 7.5 },
      allocation: { DST: 125, PREMIUM_TAX_VAT: 120, LGT: 7.5, BASIC: 975 },
      applied: 1227.5,
    },
  ],
  cwtWithheld: 25.05,
  excess: 10.1,
  currency: 'PHP',
  bookRate: 1,
  cwtPercent: 98,
};

describe('application preview', () => {
  it('lists the components in the application hierarchy and skips zeros', () => {
    expect(allocationRows({ BASIC: 10, DST: 2, OTHER: 0, LGT: 1 }).map((r) => r.component)).toEqual(
      ['DST', 'LGT', 'BASIC'],
    );
    expect(componentLabel('PREMIUM_TAX_VAT')).toBe('VAT / Premium Tax');
    expect(componentLabel('DTIP')).toBe('DTIP');
  });

  it('totals the applied amount, the excess and flags CWT clients', () => {
    expect(previewTotals(preview)).toEqual({
      applied: 1227.5,
      excess: 10.1,
      outstanding: 1252.5,
      cwt: true,
    });
    expect(previewTotals(undefined)).toEqual({ applied: 0, excess: 0, outstanding: 0, cwt: false });
  });

  it('explains every kind of match', () => {
    expect(matchMessage(undefined)).toContain('Enter an ARN');
    expect(matchMessage(preview)).toContain('Matched 1 booked invoice');
    expect(matchMessage({ ...preview, match: 'PREBOOKED', prebookedArn: 'ARN-9' })).toContain(
      'ARN-9',
    );
    expect(matchMessage({ ...preview, match: 'CANCELLED' })).toContain('cancelled invoice');
    expect(matchMessage({ ...preview, match: 'NONE' })).toContain('No booked invoice');
  });

  it('splits references on commas, spaces and lines without duplicates', () => {
    expect(parseReferences(' ARN-1, BI-2\nARN-1;PN-3  ')).toEqual(['ARN-1', 'BI-2', 'PN-3']);
    expect(parseReferences('   ')).toEqual([]);
    expect(round2(0.1 + 0.2)).toBeCloseTo(0.3, 10);
  });
});

describe('form checks', () => {
  it('requires payor, amount, references and the check number of a check', () => {
    const errors = receiveErrors({
      references: [],
      payorName: ' ',
      amount: '0',
      mode: 'CHECK',
      checkNo: '',
    });
    expect(Object.keys(errors).sort((a, b) => a.localeCompare(b))).toEqual([
      'amount',
      'checkNo',
      'payorName',
      'references',
    ]);
    expect(
      receiveErrors({ references: ['A'], payorName: 'P', amount: '5', mode: 'CASH', checkNo: '' }),
    ).toEqual({});
  });

  it('checks a reinstatement by reason group and amount', () => {
    const base = {
      full: false,
      amount: 600,
      reasonCode: '',
      invoiceNo: 'BI',
      documentNo: 'AR',
      payorName: 'P',
    };
    const premium = reinstateErrors(base, 500, true);
    expect(Object.keys(premium).sort((a, b) => a.localeCompare(b))).toEqual([
      'accountOfficer',
      'amount',
      'reasonCode',
      'teamLeader',
      'unitHead',
    ]);
    expect(reinstateErrors({ ...base, full: true, reasonCode: 'R' }, 500, false)).toEqual({});
  });

  it('asks for the fields of each disposition action', () => {
    expect(dispositionFields('APPLY')).toEqual(['targetInvoiceNo']);
    expect(dispositionFields('DST_APPLY')).toEqual(['targetInvoiceNo']);
    expect(dispositionFields('RECLASS')).toEqual(['targetClientCode']);
    expect(dispositionFields('TRANSFER')).toEqual(['targetUnit']);
    expect(dispositionFields('REFUND')).toEqual(['payeeName']);
    expect(dispositionFields('MANUAL')).toEqual([]);
    expect(dispositionErrors({ dispositionType: 'R', amount: 50 }, 'RECLASS', 100)).toEqual({
      amount: 'A reclass or transfer moves the whole balance',
      targetClientCode: 'Required for this disposition type',
    });
    expect(dispositionErrors({ dispositionType: 'A', amount: 150 }, 'APPLY', 100).amount).toContain(
      'at most 100.00',
    );
    expect(
      dispositionErrors({ dispositionType: 'F', amount: 100, payeeName: 'X' }, 'REFUND', 100),
    ).toEqual({});
  });
});

describe('queues', () => {
  it('measures a receipt series and ages pre-booked payments', () => {
    expect(seriesUsedPercent({ fromNo: 1, toNo: 100, remaining: 25 })).toBe(75);
    expect(seriesUsedPercent({ fromNo: 10, toNo: 1, remaining: 0 })).toBe(100);
    expect(ageBucket(3)).toBe('fresh');
    expect(ageBucket(8)).toBe('ageing');
    expect(ageBucket(31)).toBe('overdue');
  });

  it('groups post-dated checks by maturity month', () => {
    const pdc = (id: number, maturityDate: string, amount: number) =>
      ({ id, maturityDate, amount }) as PdcItem;
    const months = byMaturityMonth([
      pdc(1, '2026-11-02', 10),
      pdc(2, '2026-10-30', 5.5),
      pdc(3, '2026-11-20', 4.5),
    ]);
    expect(months.map((m) => [m.month, m.items.length, m.total])).toEqual([
      ['2026-10', 1, 5.5],
      ['2026-11', 2, 14.5],
    ]);
    expect(monthLabel('2026-10')).toContain('2026');
    expect(monthLabel('bad')).toBe('bad');
  });

  it('counts an upload run by outcome', () => {
    const tiles = runTiles(
      [
        { matchCategory: 'APPLIED' },
        { matchCategory: 'UNAPPLIED_NO_MATCH' },
        { matchCategory: 'CANCELLED_REFERENCE' },
        { matchCategory: 'EXCESS' },
      ],
      2,
    );
    expect(Object.fromEntries(tiles.map((t) => [t.id, t.count]))).toEqual({
      APPLIED: 1,
      UNAPPLIED: 2,
      PREBOOKED: 0,
      EXCESS: 1,
      FAILED: 2,
    });
  });

  it('lists the journal batches of a receipt', () => {
    const detail = {
      summary: { receiptNo: 'AR-1' },
      journalBatchNo: 'JB-1',
      applications: [
        { journalBatchNo: 'JB-2', reference: 'APP:1', invoiceNo: 'BI-1' },
        { reference: 'APP:2', invoiceNo: 'BI-2' },
      ],
      actions: [{ journalBatchNo: 'JB-3', action: 'CANCEL', transactionNo: 'CAN-1' }],
    } as unknown as ReceiptDetail;
    expect(journalRows(detail).map((j) => j.batch)).toEqual(['JB-1', 'JB-2', 'JB-3']);
  });
});
