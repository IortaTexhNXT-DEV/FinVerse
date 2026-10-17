import { countOf } from '@/utils/format';

/** What waiving or excluding the selected rows does, in the right number ("1 row loads..."). */
export function effectOf(mode: 'waive' | 'exclude', count: number): string {
  const rows = countOf(count, 'row');
  if (count === 1) {
    return mode === 'waive'
      ? `${rows} loads despite its errors and counts as waived.`
      : `${rows} is left out of the load; state how it is handled.`;
  }
  return mode === 'waive'
    ? `${rows} load despite their errors and count as waived.`
    : `${rows} are left out of the load; state how they are handled.`;
}
