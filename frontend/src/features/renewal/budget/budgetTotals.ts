import type { BudgetMonth } from '@/api/renewalDashboard';

/** The annual totals of the monthly amounts (FRRN.042.02). */
export function annualTotals(months: BudgetMonth[]) {
  const sum = (k: 'newAmount' | 'renewalAmount' | 'organicAmount') =>
    months.reduce((s, m) => s + m[k], 0);
  const n = sum('newAmount');
  const r = sum('renewalAmount');
  const o = sum('organicAmount');
  return { newTotal: n, renewalTotal: r, organicTotal: o, grandTotal: n + r + o };
}
