import type { RatingResult } from '@/api/catalog';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Kpi } from '@/components/ui/Kpi';
import { formatAmount, humanize } from '@/utils/format';

type Line = readonly [string, number | undefined, string?];

function lines(r: RatingResult): Line[] {
  const b = r.breakdown;
  const motor: Line[] =
    b.method === 'MOTOR'
      ? [
          ['Own damage & theft cover', b.odTheftCoverage],
          ['Own damage & theft premium', b.odTheftPremium],
          ['Excess BI premium', b.biPremium],
          ['PD premium', b.pdPremium],
        ]
      : [];
  return [
    ...motor,
    ['Annual premium', b.annualPremium],
    ['Net premium', b.netPremium, b.minimumApplied ? 'minimum premium applied' : undefined],
    ['Documentary stamp tax', b.dst, `${r.rates.dst}%, rounded up to the next 0.50`],
    ['Premium tax', b.premiumTax, `${r.rates.premiumTax}%`],
    ['VAT', b.vat, `${r.rates.vatPremium}%`],
    ['Fire service tax', b.fst, `${r.rates.fireServiceTax}%`],
    ['Local government tax', b.lgt, `${r.rates.lgt}%`],
    ['Total charges', b.totalCharges],
    ['Commission', b.commission, `${r.rates.commission}%`],
    ['VAT on commission', b.vatOnCommission, `${r.rates.vatCommission}%`],
  ];
}

function periodText(r: RatingResult): string {
  if (r.basis === 'SHORT_PERIOD') {
    return `short period, ${r.shortPeriodPercent ?? 0}% of annual`;
  }
  if (r.basis === 'PRO_RATA') {
    return `pro-rata, factor ${r.breakdown.periodFactor}`;
  }
  return 'annual';
}

/** Premium breakdown of a rating: headline figures, each charge with its rate, and items. */
export function CalculatorResult({ result }: Readonly<{ result: RatingResult }>) {
  const b = result.breakdown;
  return (
    <div className="stack">
      <div className="grid-4">
        <Kpi label="Sum insured" value={formatAmount(b.sumInsured)} />
        <Kpi label="Net premium" value={formatAmount(b.netPremium)} hint={periodText(result)} />
        <Kpi label="Gross premium" value={formatAmount(b.grossPremium)} accent />
        <Kpi
          label="Commission"
          value={formatAmount(b.commission)}
          hint={`${result.rates.commission}%`}
        />
      </div>
      <Card title={`Breakdown (${humanize(b.method)} rating)`} flush>
        <DataTable<Line>
          rows={lines(result)}
          rowKey={(l) => l[0]}
          columns={[
            { key: 'l', header: 'Component', render: (l) => l[0] },
            {
              key: 'n',
              header: 'Basis',
              render: (l) => <span className="muted">{l[2] ?? ''}</span>,
            },
            { key: 'a', header: 'Amount', numeric: true, render: (l) => <Amount value={l[1]} /> },
          ]}
        />
      </Card>
      {b.items.length > 0 && (
        <Card title="Items" flush>
          <DataTable
            rows={b.items.map((item, n) => ({ ...item, n }))}
            rowKey={(i) => i.n}
            columns={[
              { key: 'l', header: 'Item', render: (i) => i.label },
              {
                key: 's',
                header: 'Sum insured',
                numeric: true,
                render: (i) => <Amount value={i.sumInsured} />,
              },
              { key: 'r', header: 'Rate %', numeric: true, render: (i) => i.ratePercent ?? '' },
              {
                key: 'p',
                header: 'Premium',
                numeric: true,
                render: (i) => <Amount value={i.premium} />,
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
