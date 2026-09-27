import { useQuery } from '@tanstack/react-query';
import { accountsApi } from '@/api/accounts';
import type { AccountPremium } from '@/api/accounts';
import { Card } from '@/components/ui/Card';
import { formatAmount, humanize } from '@/utils/format';
import { DetailList } from '@/features/catalog/DetailList';
import type { DetailRow } from '@/features/catalog/DetailList';

function rows(p: AccountPremium, currency: string): DetailRow[] {
  const money = (v: number | null | undefined) =>
    (v ?? null) === null ? '—' : `${currency} ${formatAmount(v)}`;
  return [
    ['Rating basis', humanize(p.ratingBasis ?? 'ANNUAL')],
    ['Net premium', `${money(p.netPremium)}${p.minimumApplied ? ' (minimum)' : ''}`],
    ['Documentary stamp tax', money(p.dst)],
    ['Premium tax', money(p.premiumTax)],
    ['VAT', money(p.vat)],
    ['Fire service tax', money(p.fst)],
    ['Local government tax', money(p.lgt)],
    ['Total charges', money(p.totalCharges)],
    ['Gross premium', money(p.grossPremium)],
    ['Commission', `${money(p.commission)} (${p.commissionRate ?? 0}%)`],
    ['VAT on commission', money(p.vatOnCommission)],
  ];
}

/** Premium of an account as rated by the server when it was last saved. */
export function PremiumSummary({ accountId }: Readonly<{ accountId: number }>) {
  const account = useQuery({
    queryKey: ['account', accountId],
    queryFn: () => accountsApi.get(accountId),
  });
  if (account.data === undefined) {
    return null;
  }
  const { premium, currency } = account.data;
  return (
    <Card title="Premium">
      {(premium.grossPremium ?? null) === null ? (
        <p className="muted">Not rated yet: enter the sum insured and period of every item.</p>
      ) : (
        <DetailList rows={rows(premium, currency)} />
      )}
    </Card>
  );
}
