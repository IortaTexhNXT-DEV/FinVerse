import { describe, expect, it } from 'vitest';
import type { ReceiptFormVersion } from './printApi';
import { COPY_LABELS, failedLines, formInUse, selectionError } from './printApi';
import { EMPTY_FILTERS, criteriaOf } from './printLogic';

const version = (
  versionNo: number,
  effectiveFrom: string,
  status: ReceiptFormVersion['status'] = 'APPROVED',
): ReceiptFormVersion => ({
  id: versionNo,
  formKind: 'AR',
  versionNo,
  text: {},
  effectiveFrom,
  status,
  createdBy: 'mbs',
  createdAt: '2026-10-01T00:00:00Z',
});

describe('Batch printing (FRS.CSH.02.04)', () => {
  it('refuses an empty selection and more than 500 receipts', () => {
    expect(selectionError(0)).toBe('Select between 1 and 500 receipts to print');
    expect(selectionError(501)).toBe('Select between 1 and 500 receipts to print');
    expect(selectionError(500)).toBeUndefined();
  });

  it('lists the failed receipts of a batch and names the copies', () => {
    const lines = [
      { receiptId: 1, receiptNo: 'AR-1', status: 'PRINTED' as const, reprint: false },
      { receiptId: 2, receiptNo: 'AR-2', status: 'FAILED' as const, reprint: false },
      { receiptId: 3, receiptNo: 'AR-3', status: 'SKIPPED' as const, reprint: false },
    ];
    expect(failedLines({ lines }).map((l) => l.receiptNo)).toEqual(['AR-2']);
    expect(COPY_LABELS.CLIENT).toBe("Client's Copy");
  });
});

describe('AR and OR forms (FRS.CSH.02.06.01)', () => {
  it('uses the latest approved version effective on the print date', () => {
    const versions = [
      version(1, '2000-01-01'),
      version(2, '2026-10-01'),
      version(3, '2026-11-01'),
      version(4, '2026-10-05', 'PENDING_APPROVAL'),
    ];
    expect(formInUse(versions, 'AR', '2026-10-09')?.versionNo).toBe(2);
    expect(formInUse(versions, 'AR', '2026-09-30')?.versionNo).toBe(1);
    expect(formInUse(versions, 'OR', '2026-10-09')).toBeUndefined();
  });
});

describe('Batch print searches (FRS.CSH.02.04.02)', () => {
  const f = {
    ...EMPTY_FILTERS,
    branchId: '7',
    insurer: 'INS-1',
    from: '2026-10-01',
    number: '0042',
  };

  it('prints ARs by receipting branch and ORs by insurer, with the issue dates', () => {
    expect(criteriaOf('manual', f, 1)).toMatchObject({
      kind: 'AR',
      branchId: 7,
      insurer: undefined,
    });
    expect(criteriaOf('manual', { ...f, kind: 'OR' }, 1)).toMatchObject({
      kind: 'OR',
      branchId: undefined,
      insurer: 'INS-1',
      from: '2026-10-01',
    });
  });

  it('lists the receipts the system generated not printed yet, and re-prints by part of the number', () => {
    expect(criteriaOf('queue', f, 1)).toMatchObject({ printed: false, systemOnly: true, kind: '' });
    expect(criteriaOf('reprint', f, 1)).toMatchObject({ printed: true, numberPart: '0042' });
  });
});
