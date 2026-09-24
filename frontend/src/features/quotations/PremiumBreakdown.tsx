import { useQuery } from '@tanstack/react-query';
import type { AccountPremium } from '@/api/accounts';
import { quotationsApi } from '@/api/quotations';
import type { QuotationInput } from '@/api/quotations';
import { Card } from '@/components/ui/Card';
import { DetailList } from '@/features/catalog/DetailList';
import { formatAmount } from '@/utils/format';
import { premiumRows } from './premium';

/** A premium breakdown card, or the reason why it cannot be computed yet. */
export function PremiumCard({
  premium,
  currency,
  title = 'Premium',
}: Readonly<{ premium: AccountPremium; currency: string; title?: string }>) {
  const rated = premium.grossPremium !== undefined && premium.grossPremium !== null;
  return (
    <Card
      title={title}
      actions={
        rated && (
          <span className="premium-total">
            {currency} {formatAmount(premium.grossPremium)}
          </span>
        )
      }
    >
      {rated ? (
        <DetailList rows={premiumRows(premium, currency)} />
      ) : (
        <p className="muted">
          Not rated yet: give every item a sum insured (and a rate when the product has no default
          rate).
        </p>
      )}
    </Card>
  );
}

/**
 * Live premium of the wizard (BRNB.043): the server rates the draft with the rates in force on
 * every change, without saving it, and tells whether a TSU routing rule applies (BRNB.098).
 */
export function LivePremium({ input }: Readonly<{ input: QuotationInput }>) {
  const ready = input.productCode !== '' && input.items.length > 0;
  const preview = useQuery({
    queryKey: ['quotations', 'preview', JSON.stringify(input)],
    queryFn: () => quotationsApi.preview(input),
    enabled: ready,
    staleTime: 30_000,
    retry: false,
  });
  if (!ready) {
    return (
      <Card title="Premium">
        <p className="muted">Choose the product and add the risk items to see the premium.</p>
      </Card>
    );
  }
  if (preview.error) {
    return (
      <Card title="Premium">
        <p className="field-error">{preview.error.message}</p>
      </Card>
    );
  }
  const data = preview.data;
  return (
    <>
      {data?.tsuRequired === true && (
        <div className="alert warning" role="status">
          TSU routing applies: {data.tsuReason}. Consider a Proposal Request (PRF) for TSU to price
          the risk with the insurers.
        </div>
      )}
      {data === undefined ? (
        <Card title="Premium">
          <span className="spinner" aria-label="Rating" />
        </Card>
      ) : (
        <PremiumCard premium={data.content.premium} currency="PHP" title="Live premium" />
      )}
    </>
  );
}
