import type { Premium } from '@/api/underwriting';
import { Amount } from '@/components/ui/Amount';

type Line = [label: string, value: number | undefined, strong?: boolean];

function lines(p: Premium): Line[] {
  const all: Line[] = [
    ['Sum insured (100%)', p.sumInsured],
    ['Our sum insured', p.ourSumInsured],
    ['Gross premium (100%)', p.grossPremium],
    ['Discount', -p.discountAmount],
    ['Loading', p.loadingAmount],
    ['Net premium (100%)', p.netPremium, true],
    ['Our net premium', p.ourNetPremium, true],
    ["Coinsurers' share billed", p.coinsurerPremium],
    ['Documentary stamp tax', p.dst],
    ['VAT', p.vat],
    ['Local government tax', p.lgt],
    ['Fire service tax', p.fst],
    ['Premium tax', p.premiumTax],
    ['Policy fee', p.policyFee],
    ['Total due', p.totalDue, true],
    [`Commission (${String(p.commissionRate)}%)`, p.commission],
    ['Withholding tax on commission', -p.withholdingTax],
    ['Net commission payable', p.netCommission, true],
  ];
  return all.filter(([, value, strong]) => strong === true || (value ?? 0) !== 0);
}

/** Premium computation breakdown (preview, policy, endorsement, debit note). */
export function PremiumSummary({
  premium,
  currency,
}: Readonly<{ premium: Premium; currency?: string }>) {
  return (
    <table className="table">
      <caption className="visually-hidden">Premium breakdown</caption>
      <tbody>
        {lines(premium).map(([label, value, strong]) => (
          <tr key={label}>
            <td>{strong === true ? <strong>{label}</strong> : label}</td>
            <td className="num">
              {currency !== undefined && strong === true && (
                <span className="muted">{currency} </span>
              )}
              {strong === true ? (
                <strong>
                  <Amount value={value} />
                </strong>
              ) : (
                <Amount value={value} />
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
