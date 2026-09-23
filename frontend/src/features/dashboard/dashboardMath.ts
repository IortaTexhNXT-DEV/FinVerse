/** Pure helpers of the executive dashboard widgets. */

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Short month name of a `yyyy-MM` label ("2026-03" → "Mar"); unknown input is returned as is. */
export function monthLabel(yearMonth: string): string {
  const month = Number(yearMonth.slice(5, 7));
  return MONTHS[month - 1] ?? yearMonth;
}

/**
 * Change of a figure against its comparison value in percent, rounded to one decimal. Undefined
 * when there is nothing to compare with (comparison value 0).
 */
export function changePct(current: number, previous: number): number | undefined {
  if (previous === 0) {
    return undefined;
  }
  return Math.round(((current - previous) / Math.abs(previous)) * 1000) / 10;
}

/** Signed change text for a KPI hint, e.g. "+12.5% vs prior year"; empty without comparison. */
export function changeText(current: number, previous: number, against: string): string {
  const pct = changePct(current, previous);
  if (pct === undefined) {
    return `no ${against} figure`;
  }
  return `${pct > 0 ? '+' : ''}${pct.toFixed(1)}% vs ${against}`;
}

/** Whether any of the given numbers is not zero (a widget shows its chart only then). */
export function hasData(values: number[]): boolean {
  return values.some((v) => v !== 0);
}

/** Share of a part in a whole in percent (0 when the whole is 0), for progress bars. */
export function sharePct(part: number, whole: number): number {
  if (whole === 0) {
    return 0;
  }
  return Math.min(100, Math.max(0, (part / whole) * 100));
}
