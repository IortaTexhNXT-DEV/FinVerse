import type { Quotation } from '@/api/quotations';
import { formatPeriod } from '@/utils/format';

/**
 * The cover period of a quotation as users read it: inception to expiry when entered, else the
 * package term given by the server; empty (shown as a dash) only when neither is known.
 */
export function coverPeriod(q: Pick<Quotation, 'content' | 'coverPeriod'>): string {
  const dates = formatPeriod(q.content.periodFrom, q.content.periodTo);
  return dates === '' ? (q.coverPeriod ?? '') : dates;
}
