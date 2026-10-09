import { describe, expect, it } from 'vitest';
import { dayTotals } from './dayEndLogic';
import type { ReceiptRecord } from './recordsApi';

const record = (
  id: number,
  tenderType: 'CASH' | 'CHECK',
  bank: string,
  total: number,
  stage: ReceiptRecord['stage'] = 'POSTED',
): ReceiptRecord => ({
  id,
  recordNo: `CR-AR-${id}`,
  recordKind: 'CREATION',
  receiptKind: 'AR',
  stage,
  statusLabel: '',
  editable: false,
  tender: { tenderType, currency: 'PHP', bankAccount: bank },
  bankAccountName: bank,
  accounts: [],
  accountsText: '',
  total,
  createdAt: '2026-10-09T01:00:00Z',
});

describe("Cashier's day-end list (FRS.CSH.02.01.16)", () => {
  it('totals the records of the day per payment type and per bank account', () => {
    const totals = dayTotals([
      record(1, 'CASH', 'Ortigas Savings', 1000.1),
      record(2, 'CHECK', 'Ortigas Savings', 500),
      record(3, 'CHECK', 'Makati Current', 250.25),
      record(4, 'CASH', 'Ortigas Savings', 99, 'RECORD_CANCELLED'),
    ]);
    const find = (group: string, label: string) =>
      totals.find((t) => t.group === group && t.label === label);
    expect(find('Payment type', 'Cash')).toMatchObject({ count: 1, amount: 1000.1 });
    expect(find('Payment type', 'Check')).toMatchObject({ count: 2, amount: 750.25 });
    expect(find('Bank account', 'Ortigas Savings')).toMatchObject({ count: 2, amount: 1500.1 });
    expect(find('Bank account', 'Makati Current')?.amount).toBe(250.25);
  });
});
