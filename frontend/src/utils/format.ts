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
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}
