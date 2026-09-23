import type { Period } from '@/api/tax';

/** Granularity of a tax period picker. */
export type Granularity = 'MONTH' | 'QUARTER';

/** Selection of a period picker: a month (1-12) or a quarter (1-4) of a year. */
export interface PeriodChoice {
  year: number;
  granularity: Granularity;
  index: number;
}

const MONTHS_PER_QUARTER = 3;

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

function lastDay(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/** ISO dates of a month or quarter. */
export function periodOf(choice: PeriodChoice): Period {
  const first =
    choice.granularity === 'QUARTER' ? (choice.index - 1) * MONTHS_PER_QUARTER + 1 : choice.index;
  const last = choice.granularity === 'QUARTER' ? first + MONTHS_PER_QUARTER - 1 : first;
  return {
    from: `${choice.year}-${pad(first)}-01`,
    to: `${choice.year}-${pad(last)}-${pad(lastDay(choice.year, last))}`,
  };
}

/** Quarter number (1-4) of an ISO date. */
export function quarterOf(iso: string): number {
  return Math.floor((Number(iso.slice(5, 7)) - 1) / MONTHS_PER_QUARTER) + 1;
}

/** The last completed period before a date (the one normally being worked on). */
export function defaultChoice(todayIso: string, granularity: Granularity): PeriodChoice {
  const year = Number(todayIso.slice(0, 4));
  const month = Number(todayIso.slice(5, 7));
  if (granularity === 'MONTH') {
    return month === 1
      ? { year: year - 1, granularity, index: 12 }
      : { year, granularity, index: month - 1 };
  }
  const quarter = quarterOf(todayIso);
  return quarter === 1
    ? { year: year - 1, granularity, index: 4 }
    : { year, granularity, index: quarter - 1 };
}

/** Label of a selection, e.g. "2026-Q2" or "2026-03". */
export function choiceLabel(choice: PeriodChoice): string {
  return choice.granularity === 'QUARTER'
    ? `${choice.year}-Q${choice.index}`
    : `${choice.year}-${pad(choice.index)}`;
}

/** Options of the index select for a granularity. */
export function indexOptions(granularity: Granularity): { value: number; label: string }[] {
  if (granularity === 'QUARTER') {
    return [1, 2, 3, 4].map((q) => ({ value: q, label: `Q${q}` }));
  }
  const names = [
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
  ];
  return names.map((label, i) => ({ value: i + 1, label }));
}
