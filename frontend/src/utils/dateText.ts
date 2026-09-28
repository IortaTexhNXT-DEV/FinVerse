import { today } from '@/utils/format';

const MONTHS = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec'];

function iso(year: number, month: number, day: number): string | undefined {
  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return undefined;
  }
  return date.toISOString().slice(0, 10);
}

/**
 * A typed date as ISO yyyy-mm-dd: "23-Sep-2026" (the BIBS format), "23/09/2026" or "2026-09-23",
 * or "today" (the business date in Philippine time); '' for an empty text; undefined while the
 * text is not a complete, valid date.
 */
export function parseDateText(text: string): string | undefined {
  const t = text.trim();
  if (t === '') {
    return '';
  }
  if (t.toLowerCase() === 'today') {
    return today();
  }
  let m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(t);
  if (m !== null) {
    return iso(Number(m[1]), Number(m[2]), Number(m[3]));
  }
  m = /^(\d{1,2})[-/ ]([A-Za-z]{3})[-/ ](\d{4})$/.exec(t);
  if (m !== null) {
    const month = MONTHS.indexOf((m[2] ?? '').toLowerCase());
    return month < 0 ? undefined : iso(Number(m[3]), month + 1, Number(m[1]));
  }
  m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(t);
  if (m !== null) {
    return iso(Number(m[3]), Number(m[2]), Number(m[1]));
  }
  return undefined;
}

const MONTH_NAMES = [
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

/** A month (ISO yyyy-mm) as users read it: Sep-2026; '' for none. */
export function formatMonth(month: string | null | undefined): string {
  const m = /^(\d{4})-(\d{2})/.exec(month ?? '');
  if (m === null) {
    return month ?? '';
  }
  const name = MONTH_NAMES[Number(m[2]) - 1];
  return name === undefined ? (month ?? '') : `${name}-${m[1] ?? ''}`;
}

/**
 * A typed month as ISO yyyy-mm: "Sep-2026" (the BIBS format), "09/2026" or "2026-09"; '' for an
 * empty text; undefined while the text is not a complete month.
 */
export function parseMonthText(text: string): string | undefined {
  const t = text.trim();
  if (t === '') {
    return '';
  }
  const found = monthParts(t);
  if (found === undefined || found.month < 1 || found.month > 12) {
    return undefined;
  }
  return `${String(found.year)}-${String(found.month).padStart(2, '0')}`;
}

/** The year and month of a typed month in one of the accepted forms. */
function monthParts(t: string): { year: number; month: number } | undefined {
  const iso = /^(\d{4})-(\d{1,2})$/.exec(t);
  if (iso !== null) {
    return { year: Number(iso[1]), month: Number(iso[2]) };
  }
  const named = /^([A-Za-z]{3})[-/ ](\d{4})$/.exec(t);
  if (named !== null) {
    return { year: Number(named[2]), month: MONTHS.indexOf((named[1] ?? '').toLowerCase()) + 1 };
  }
  const slashed = /^(\d{1,2})\/(\d{4})$/.exec(t);
  return slashed === null ? undefined : { year: Number(slashed[2]), month: Number(slashed[1]) };
}
