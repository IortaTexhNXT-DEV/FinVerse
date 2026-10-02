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
