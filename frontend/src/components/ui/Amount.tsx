import { formatAmount } from '@/utils/format';

/** Right-aligned monetary amount in accounting format. */
export function Amount({ value }: Readonly<{ value: number | string | null | undefined }>) {
  return <span className="num">{formatAmount(value)}</span>;
}
