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

export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.slice(0, 10).split('-');
  return `${day ?? ''}-${month ?? ''}-${year ?? ''}`;
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Date(iso).toLocaleString('en-PH', { dateStyle: 'medium', timeStyle: 'short' });
}

export function today(): string {
  return new Date().toISOString().slice(0, 10);
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
