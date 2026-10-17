import type { ReceiptRecord } from './recordsApi';

/** A total of the day-end list (FRS.CSH.02.01.16). */
export interface DayTotal {
  key: string;
  group: 'Payment type' | 'Bank account';
  label: string;
  currency: string;
  count: number;
  amount: number;
}

const TENDER_LABELS: Record<string, string> = {
  CASH: 'Cash',
  CHECK: 'Check',
  DIRECT_CREDIT: 'Direct Credit',
};

/**
 * The totals of the records of a day per payment type and per bank account, in each currency;
 * cancelled records are left out.
 */
export function dayTotals(records: readonly ReceiptRecord[]): DayTotal[] {
  const totals = new Map<string, DayTotal>();
  const add = (group: DayTotal['group'], label: string, currency: string, amount: number) => {
    const key = `${group}|${label}|${currency}`;
    const t = totals.get(key) ?? { key, group, label, currency, count: 0, amount: 0 };
    t.count += 1;
    t.amount = Math.round((t.amount + amount) * 100) / 100;
    totals.set(key, t);
  };
  for (const r of records) {
    if (r.stage === 'RECORD_CANCELLED' || r.recordKind !== 'CREATION') {
      continue;
    }
    const currency = r.tender?.currency ?? '';
    const tender = r.tender?.tenderType ?? '';
    add('Payment type', TENDER_LABELS[tender] ?? tender, currency, r.total);
    add('Bank account', r.bankAccountName ?? r.tender?.bankAccount ?? '', currency, r.total);
  }
  return [...totals.values()].sort((a, b) => a.key.localeCompare(b.key));
}
