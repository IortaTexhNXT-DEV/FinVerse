import type { PayableItem } from '@/api/payables';
import {
  checkSelection,
  daysBetween,
  dueDate,
  fundLevel,
  invoiceTotals,
  lineTaxes,
  round2,
} from './payablesMath';

const item = (id: number, available: number): PayableItem => ({
  openItemId: id,
  documentType: 'SUPPLIER_INVOICE',
  documentNo: `SI-${id}`,
  documentDate: '2026-09-01',
  dueDate: '2026-10-01',
  currency: 'PHP',
  amount: available,
  outstanding: available,
  available,
});

describe('invoice taxes', () => {
  it('computes VAT, withholding and payable like the server', () => {
    expect(lineTaxes(1234.56, true, 2)).toEqual({ vat: 148.15, wht: 24.69, payable: 1358.02 });
    expect(lineTaxes(1000, false, 0)).toEqual({ vat: 0, wht: 0, payable: 1000 });
    expect(lineTaxes(Number.NaN, true, 2)).toEqual({ vat: 0, wht: 0, payable: 0 });
  });

  it('sums rounded lines into invoice totals', () => {
    const t = invoiceTotals(
      [{ netAmount: 10000 }, { netAmount: 2500 }, { netAmount: 1000 }],
      true,
      2,
    );
    expect(t).toEqual({ net: 13500, vat: 1620, wht: 270, payable: 14850 });
  });

  it('rounds to centavos', () => {
    expect(round2(2.5551)).toBe(2.56);
    expect(round2(2.444)).toBe(2.44);
  });
});

describe('dates', () => {
  it('adds supplier credit days for the due date', () => {
    expect(dueDate('2026-09-01', 30)).toBe('2026-10-01');
    expect(dueDate('', 30)).toBe('');
  });

  it('counts days between dates', () => {
    expect(daysBetween('2026-09-01', '2026-09-23')).toBe(22);
    expect(daysBetween('2026-09-23', '2026-09-01')).toBe(-22);
  });
});

describe('payment selection', () => {
  it('totals the selected items and flags invalid amounts', () => {
    const items = [item(1, 100), item(2, 50), item(3, 10)];
    expect(checkSelection(items, { 1: 100, 2: 20.5 })).toEqual({ total: 120.5, errors: [] });
    const bad = checkSelection(items, { 1: 150, 2: 0 });
    expect(bad.errors).toHaveLength(2);
    expect(bad.errors[0]).toContain('exceeds');
  });
});

describe('petty cash', () => {
  it('shows the cash level of the imprest', () => {
    expect(fundLevel(25000, 50000)).toBe(50);
    expect(fundLevel(60000, 50000)).toBe(100);
    expect(fundLevel(10, 0)).toBe(0);
  });
});
