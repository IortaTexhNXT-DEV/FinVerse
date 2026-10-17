import { statusLabel } from '@/components/ui/statusTones';
import { humanize } from '@/utils/format';

/** A fiscal year in the year selector: FY 2026 · Open. */
export function yearOption(yearCode: string | number, status: string): string {
  return `FY ${String(yearCode)} · ${statusLabel(status)}`;
}

/** A period in the period selector: 2026-09 · Open. */
export function periodOption(name: string, status: string): string {
  return `${name} · ${statusLabel(status)}`;
}

/** A foreign-currency open item: the document and whether it is a debit or a credit, in words. */
export function openItemDocument(documentNo: string, direction: string): string {
  return `${documentNo} (${humanize(direction)})`;
}

/** The notice when a currency has no month-end rate for the revaluation. */
export function missingRateNotice(currencies: readonly string[]): string {
  return `No month-end rate for ${currencies.join(', ')}: posting is blocked.`;
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** A day of the year kept as MM-dd (04-15) as the screens show it: 15-Apr. */
export function dayOfYear(monthDay: string | undefined): string {
  const [month, day] = (monthDay ?? '').split('-');
  const name = MONTHS[Number(month) - 1];
  return name === undefined || day === undefined ? (monthDay ?? '—') : `${day}-${name}`;
}

/** What happens at the time of a scheduled close, in the words of the business. */
export const SCHEDULE_NOTE =
  'At the chosen time the period-end checklist runs; when a control fails, the close is recorded as failed with the blocking items and the alert Scheduled close failed is sent.';
