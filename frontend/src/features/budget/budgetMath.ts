/**
 * Budget grid arithmetic. Amounts are handled in whole cents so spreads always add up exactly to
 * the annual amount; the rounding remainder goes to the last month (same rule as the backend).
 */

export const MONTHS = 12;

export const MONTH_LABELS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const;

/** Seasonality presets (relative monthly weights). */
export const SEASONALITY: Record<string, number[]> = {
  Even: Array.from({ length: MONTHS }, () => 1),
  'Premium renewals': [7, 7, 9, 8, 8, 9, 8, 8, 9, 8, 9, 10],
  'Year-end heavy': [6, 6, 7, 7, 8, 8, 8, 8, 9, 9, 10, 14],
};

const toCents = (value: number): number => Math.round(value * 100);
const fromCents = (cents: number): number => cents / 100;

/** Spreads an annual amount over twelve months in proportion to weights. */
export function spreadWeighted(annual: number, weights: readonly number[]): number[] {
  if (weights.length !== MONTHS) {
    throw new Error('Seasonality needs twelve monthly weights');
  }
  const totalWeight = weights.reduce((acc, w) => acc + w, 0);
  if (totalWeight <= 0 || weights.some((w) => w < 0)) {
    throw new Error('Seasonality weights must be positive');
  }
  const target = toCents(annual);
  const cents = weights.slice(0, MONTHS - 1).map((w) => Math.round((target * w) / totalWeight));
  const allocated = cents.reduce((acc, c) => acc + c, 0);
  return [...cents, target - allocated].map(fromCents);
}

/** Spreads an annual amount evenly. */
export function spreadEven(annual: number): number[] {
  return spreadWeighted(annual, SEASONALITY.Even ?? []);
}

/** Sum of amounts, exact to the cent. */
export function sum(values: readonly number[]): number {
  return fromCents(values.reduce((acc, v) => acc + toCents(v), 0));
}

/** Monthly column totals of a grid. */
export function monthTotals(rows: readonly { months: readonly number[] }[]): number[] {
  return Array.from({ length: MONTHS }, (_, i) => sum(rows.map((r) => r.months[i] ?? 0)));
}

/** Scales every month by a percentage (e.g. +5 %). */
export function scale(months: readonly number[], percent: number): number[] {
  return months.map((m) => fromCents(Math.round(toCents(m) * (1 + percent / 100))));
}

/** Utilization category used for colour coding: over (≥ 100 %), warning (≥ threshold), ok. */
export function utilizationLevel(
  pct: number | undefined,
  threshold: number,
): 'over' | 'warning' | 'ok' | 'none' {
  if (pct === undefined) {
    return 'none';
  }
  if (pct >= 100) {
    return 'over';
  }
  return pct >= threshold ? 'warning' : 'ok';
}
