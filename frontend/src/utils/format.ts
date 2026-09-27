/** Codes shown in capitals inside labels (BDO style guide: Title Case, acronyms kept). */
const ACRONYMS = new Set([
  'ARN',
  'AR',
  'OR',
  'CL',
  'CLPC',
  'CTPL',
  'CWT',
  'DP',
  'DST',
  'FFY',
  'GL',
  'IA',
  'ID',
  'IBNR',
  'KYC',
  'LGT',
  'LOV',
  'NB',
  'PN',
  'PR',
  'PRF',
  'PS',
  'QS',
  'SI',
  'SLA',
  'TIN',
  'TSU',
  'UPR',
  'VAT',
]);

/** Short words kept in lower case inside Title Case labels ("Ready for Placement"). */
const MINOR_WORDS = new Set([
  'a',
  'an',
  'and',
  'as',
  'at',
  'by',
  'for',
  'in',
  'of',
  'on',
  'or',
  'the',
  'to',
  'via',
  'with',
]);

/** Display formatting. Money uses accounting style: negatives in parentheses. */

const amountFormat = new Intl.NumberFormat('en-PH', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const integerFormat = new Intl.NumberFormat('en-PH', { maximumFractionDigits: 0 });

export function formatAmount(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '';
  }
  const n = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(n)) {
    return String(value);
  }
  const text = amountFormat.format(Math.abs(n));
  return n < 0 ? `(${text})` : text;
}

export function formatCompact(value: number): string {
  const abs = Math.abs(value);
  const units: [number, string][] = [
    [1e9, 'B'],
    [1e6, 'M'],
    [1e3, 'K'],
  ];
  const unit = units.find(([size]) => abs >= size);
  if (unit === undefined) {
    return integerFormat.format(value);
  }
  return `${(value / unit[0]).toFixed(1)}${unit[1]}`;
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Time zone of every displayed timestamp: BDO Insure operates in the Philippines. */
const DISPLAY_TIME_ZONE = 'Asia/Manila';

const timestampParts = new Intl.DateTimeFormat('en-GB', {
  timeZone: DISPLAY_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/** A calendar date (ISO yyyy-MM-dd, or the date part of a timestamp) as dd-MMM-yyyy: 23-Sep-2026. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.slice(0, 10).split('-');
  const name = MONTHS[Number(month) - 1];
  if (!year || !day || name === undefined) {
    return iso;
  }
  return `${day}-${name}-${year}`;
}

/**
 * A period on one line for texts and detail blocks: "20-Oct-2026 to 20-Oct-2027"; an open end
 * reads "from 20-Oct-2026" (lists use `PeriodCell`, which puts the two dates on two lines).
 */
export function formatPeriod(
  from: string | null | undefined,
  to: string | null | undefined,
): string {
  const start = formatDate(from);
  const end = formatDate(to);
  if (start === '' && end === '') {
    return '';
  }
  if (end === '') {
    return `from ${start}`;
  }
  return start === '' ? `to ${end}` : `${start} to ${end}`;
}

/** A number of days as users read it: "0 days", "1 day", "12 days" (aging columns). */
export function formatDays(days: number | null | undefined): string {
  if (days === null || days === undefined || !Number.isFinite(days)) {
    return '';
  }
  return `${String(days)} ${Math.abs(days) === 1 ? 'day' : 'days'}`;
}

/** A timestamp as dd-MMM-yyyy HH:mm in Philippine time: 25-Sep-2026 19:32. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    timestampParts.formatToParts(date).find((p) => p.type === type)?.value ?? '';
  const month = MONTHS[Number(part('month')) - 1] ?? '';
  return `${part('day')}-${month}-${part('year')} ${part('hour')}:${part('minute')}`;
}

/** Whole days elapsed between two timestamps, for "duration in stage" columns. */
export function formatDuration(fromIso: string, toIso: string | null | undefined): string {
  const start = new Date(fromIso).getTime();
  const end = toIso ? new Date(toIso).getTime() : Date.now();
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) {
    return '';
  }
  const minutes = Math.floor((end - start) / 60000);
  if (minutes < 60) {
    return `${String(minutes)}m`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${String(hours)}h ${String(minutes % 60)}m`;
  }
  const days = Math.floor(hours / 24);
  return `${String(days)}d ${String(hours % 24)}h`;
}

const businessDateFormat = new Intl.DateTimeFormat('en-CA', {
  timeZone: DISPLAY_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

/**
 * The business date (ISO yyyy-MM-dd) in Philippine time, whatever the zone of the browser: from
 * 00:00 to 07:59 in Manila the UTC date is still the day before.
 */
export function today(now: Date = new Date()): string {
  return businessDateFormat.format(now);
}

export function humanize(code: string): string {
  return code
    .split('_')
    .map((word, i) => {
      const w = word.toUpperCase();
      if (ACRONYMS.has(w)) {
        return w;
      }
      const lower = w.toLowerCase();
      return i > 0 && MINOR_WORDS.has(lower)
        ? lower
        : lower.charAt(0).toUpperCase() + lower.slice(1);
    })
    .join(' ');
}

function titleWord(word: string, first: boolean): string {
  const start = word.search(/[A-Za-z]/);
  if (start < 0) {
    return word;
  }
  const lead = word.slice(0, start);
  const core = word.slice(start);
  if (/[A-Z]/.test(core.slice(1))) {
    return word; // acronyms and mixed case as written: TSU, ManCom, OR
  }
  const lower = core.toLowerCase();
  if (!first && start === 0 && MINOR_WORDS.has(lower)) {
    return lower;
  }
  return lead + lower.charAt(0).toUpperCase() + lower.slice(1);
}

/**
 * Title Case for labels that are already words, e.g. workflow stages and actions kept as
 * sentences in the database ("Revise quotation slip (new round)" → "Revise Quotation Slip (New
 * Round)"). Words written with inner capitals are kept; minor words after the first are lowercase.
 */
export function titleCase(label: string): string {
  return label
    .split(' ')
    .map((word, i) => titleWord(word, i === 0))
    .join(' ');
}

/**
 * The template version of a generated document as users read it: "QUOTATION_SLIP v2" (the stored
 * tag) becomes "Version 2"; other texts are returned as they are.
 */
export function versionLabel(tag: string | null | undefined): string {
  if (!tag) {
    return '';
  }
  const m = /\bv(\d+)$/.exec(tag.trim());
  return m === null ? tag : `Version ${m[1] ?? ''}`;
}

/**
 * A status code as words inside a sentence: "QS_SENT" becomes "QS sent", "APPROVED" becomes
 * "approved" (acronyms kept in capitals), for messages such as "PRF-2026-000001 is now QS sent".
 */
export function statusPhrase(code: string | null | undefined): string {
  if (!code) {
    return '';
  }
  return code
    .split('_')
    .map((word) => {
      const w = word.toUpperCase();
      return ACRONYMS.has(w) ? w : w.toLowerCase();
    })
    .join(' ');
}

/**
 * The action of a dialog title as a phrase: Title Case words in lower case, acronyms and names
 * such as ManCom or TSU kept ("Submit Requirements for ManCom Sign-off" becomes "submit
 * requirements for ManCom sign-off").
 */
export function actionPhrase(title: string): string {
  return title
    .split(' ')
    .map((w) => (/^[A-Z][a-z]+(?:-[A-Za-z]+)*$/.test(w) ? w.toLowerCase() : w))
    .join(' ');
}
