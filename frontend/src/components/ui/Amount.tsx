import { formatAmount } from '@/utils/format';

/**
 * Right-aligned monetary amount in accounting format. A missing amount is the muted dash, never a
 * blank cell.
 */
export function Amount({ value }: Readonly<{ value: number | string | null | undefined }>) {
  const text = formatAmount(value);
  return text === '' ? <span className="num muted">—</span> : <span className="num">{text}</span>;
}
