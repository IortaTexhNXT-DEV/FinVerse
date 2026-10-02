import type { RateType } from '@/api/masters';
import type { RowAction } from '@/components/ui/RowActions';

const RATE_TYPE_LABELS: Record<RateType, string> = {
  SPOT: 'Spot',
  CLOSING: 'Closing',
  AVERAGE: 'Average',
  BUDGET: 'Budget',
  BOOK: 'Book',
};

/** The type of an exchange rate in words (Book, Closing...), never its code. */
export function rateTypeLabel(type: RateType): string {
  return RATE_TYPE_LABELS[type];
}

/**
 * The row menu of a month-end revaluation rate: the maintainer copies the rates of the month to the
 * Book rates of the next month.
 */
export function revaluationRateActions(maintainer: boolean, copy: () => unknown): RowAction[] {
  return maintainer ? [{ label: 'Copy to Book Rates', onSelect: () => copy() }] : [];
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** A month of the form value (2026-09) as the screens show it (Sep-2026). */
export function monthText(month: string): string {
  const [year, no] = month.split('-');
  const name = MONTHS[Number(no) - 1];
  return name === undefined || year === undefined ? month : `${name}-${year}`;
}
