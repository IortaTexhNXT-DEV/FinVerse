import type { BookEntry, OpenItem, ReceiptSummary, StatementLine } from '@/api/receivables';
import {
  allocationErrors,
  allocationTotal,
  computeBrs,
  csvPreview,
  fifoAllocate,
  pdcActions,
  receiptActions,
  receivablesReportGroups,
  selectionBalance,
  splitCsv,
} from './receivablesMath';

const item = (id: number, due: string, outstanding: number): OpenItem => ({
  id,
  partyCode: 'C-1',
  direction: 'DEBIT',
  documentType: 'DEBIT_NOTE',
  documentNo: `DN-${id}`,
  documentDate: due,
  dueDate: due,
  currency: 'PHP',
  amount: outstanding,
  settledAmount: 0,
  outstanding,
  status: 'OPEN',
});

const receipt = (patch: Partial<ReceiptSummary>): ReceiptSummary => ({
  id: 1,
  receiptNo: 'OR-1',
  receiptDate: '2026-09-01',
  branchId: 1,
  payerType: 'POLICYHOLDER',
  payerName: 'Juan',
  mode: 'CHEQUE',
  currency: 'PHP',
  amount: 100,
  appliedAmount: 60,
  unappliedAmount: 40,
  bankAccountCode: '1111',
  status: 'APPROVED',
  depositStatus: 'DEPOSITED',
  createdBy: 'accountant',
  ...patch,
});

describe('allocation', () => {
  it('allocates oldest due first and keeps the rest on account', () => {
    const items = [item(2, '2026-03-01', 500), item(1, '2026-01-01', 300.1)];
    const result = fifoAllocate(items, 600);
    expect(result).toEqual({ 1: 300.1, 2: 299.9 });
    expect(allocationTotal(result)).toBe(600);
    expect(fifoAllocate(items, 0)).toEqual({});
  });

  it('reports over allocation and negative amounts', () => {
    const items = [item(1, '2026-01-01', 100), item(2, '2026-02-01', 50)];
    expect(allocationErrors(items, { 1: 100, 2: 50 }, 200)).toEqual([]);
    expect(allocationErrors(items, { 1: 120, 2: -1 }, 100)).toEqual([
      'DN-1: exceeds the outstanding 100.00',
      'DN-2: amount cannot be negative',
      'Allocations exceed the receipt amount',
    ]);
  });
});

describe('bank reconciliation', () => {
  it('computes the balance per bank and the difference', () => {
    expect(computeBrs(10000, 1500, 700, 50, 20, 9170)).toEqual({
      computedBankBalance: 9170,
      difference: 0,
    });
    expect(computeBrs(100, 0, 0, 0, 0, 90).difference).toBe(-10);
  });

  it('balances a manual match selection', () => {
    const book = [
      { id: 1, debit: 100, credit: 0 } as BookEntry,
      { id: 2, debit: 0, credit: 30 } as BookEntry,
    ];
    const bank = [{ id: 9, debit: 0, credit: 70 } as StatementLine];
    expect(selectionBalance(book, bank)).toEqual({
      bookTotal: 70,
      bankTotal: 70,
      difference: 0,
      balanced: true,
    });
    expect(selectionBalance(book, []).balanced).toBe(false);
  });

  it('previews a statement file', () => {
    const csv =
      'date,description,reference,debit,credit\n2026-09-01,"Dep, A",R1,,"1,000.50"\n\n2026-09-02,Chq,C1,250,\n';
    expect(csvPreview(csv)).toEqual({ lines: 2, debit: 250, credit: 1000.5 });
    expect(splitCsv('a,"say ""hi""",b')).toEqual(['a', 'say "hi"', 'b']);
    expect(csvPreview('')).toEqual({ lines: 0, debit: 0, credit: 0 });
  });
});

describe('report shortcuts', () => {
  it('groups receivables reports and ignores the others', () => {
    const groups = receivablesReportGroups([
      { code: 'FIN-AR-AGE-SUM' },
      { code: 'FIN-AR-AGE-DET' },
      { code: 'FIN-BRS-STMT' },
      { code: 'GL-TB' },
    ]);
    expect(groups).toEqual([
      ['Debtors ageing', [{ code: 'FIN-AR-AGE-DET' }, { code: 'FIN-AR-AGE-SUM' }]],
      ['Bank reconciliation', [{ code: 'FIN-BRS-STMT' }]],
    ]);
  });
});

describe('workflow actions', () => {
  it('lists PDC actions by status', () => {
    expect(pdcActions('ON_HAND')).toEqual(['deposit', 'return']);
    expect(pdcActions('DEPOSITED')).toEqual(['clear', 'bounce']);
    expect(pdcActions('CLEARED')).toEqual([]);
  });

  it('applies maker-checker to receipt actions', () => {
    const all = () => true;
    const pending = receipt({ status: 'PENDING_APPROVAL' });
    expect(receiptActions(pending, 'accountant', all).approve).toBe(false);
    expect(receiptActions(pending, 'checker', all).approve).toBe(true);
    const approved = receiptActions(receipt({}), 'checker', all);
    expect(approved).toMatchObject({ cancel: true, bounce: true, apply: true, approve: false });
    expect(
      receiptActions(receipt({ mode: 'CASH', depositStatus: 'IN_SLIP' }), 'checker', all),
    ).toMatchObject({
      cancel: false,
      bounce: false,
    });
  });
});
