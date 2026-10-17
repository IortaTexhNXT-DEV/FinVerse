import type { AccountPremium } from '@/api/accounts';
import type { DetailRow } from '@/features/catalog/DetailList';
import { formatAmount, humanize } from '@/utils/format';

/** Rows of an Appendix A premium breakdown. */
export function premiumRows(p: AccountPremium, currency: string): DetailRow[] {
  const money = (v: number | null | undefined) =>
    v === undefined || v === null ? '—' : `${currency} ${formatAmount(v)}`;
  return [
    ['Rating basis', humanize(p.ratingBasis ?? 'ANNUAL')],
    ['Net premium', `${money(p.netPremium)}${p.minimumApplied ? ' (minimum premium)' : ''}`],
    ['Documentary stamp tax', money(p.dst)],
    ['Premium tax', money(p.premiumTax)],
    ['VAT', money(p.vat)],
    ['Fire service tax', money(p.fst)],
    ['Local government tax', money(p.lgt)],
    ['Total charges', money(p.totalCharges)],
    ['Gross premium', money(p.grossPremium)],
    ['Commission', `${money(p.commission)} (${p.commissionRate ?? 0}%)`],
  ];
}
