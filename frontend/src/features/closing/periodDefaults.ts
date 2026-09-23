import type { FiscalYear, Period } from '@/api/periods';

const covers = (from: string, to: string, day: string) => from <= day && day <= to;

/**
 * Fiscal year a period picker opens on: the year containing today, else the latest open year,
 * else the latest year. Never simply the first year of the list, which is the newest one and may
 * be a next year whose periods are all FUTURE.
 */
export function defaultYear(years: readonly FiscalYear[], today: string): FiscalYear | undefined {
  return (
    years.find((y) => covers(y.startDate, y.endDate, today)) ??
    years.find((y) => y.status === 'OPEN') ??
    years[0]
  );
}

/**
 * Period a picker opens on: the period containing today unless it is still FUTURE, else the latest
 * period that is not FUTURE, else the first period.
 */
export function defaultPeriod(periods: readonly Period[], today: string): Period | undefined {
  const current = periods.find((p) => covers(p.startDate, p.endDate, today));
  if (current !== undefined && current.status !== 'FUTURE') {
    return current;
  }
  const started = periods.filter((p) => p.status !== 'FUTURE');
  return started[started.length - 1] ?? periods[0];
}
