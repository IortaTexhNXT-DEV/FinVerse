import type { Ageing } from '@/api/subledger';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { Kpi } from '@/components/ui/Kpi';
import { formatAmount, formatDate } from '@/utils/format';
import type { StatementSummary } from './partyStatement';

/** Headline figures of a party statement. */
export function StatementKpis({
  summary,
  partyName,
}: Readonly<{ summary: StatementSummary; partyName: string | undefined }>) {
  return (
    <div className="grid-4">
      <Kpi label="Receivable" value={<Amount value={summary.receivable} />} hint={partyName} />
      <Kpi label="Payable / unallocated" value={<Amount value={summary.payable} />} />
      <Kpi
        label="Net balance"
        accent
        value={<Amount value={summary.net} />}
        hint={summary.net >= 0 ? 'Party owes the company' : 'Company owes the party'}
      />
      <Kpi
        label="Overdue"
        value={<Amount value={summary.overdue} />}
        hint={`${summary.openItems} open items`}
      />
    </div>
  );
}

/** Ageing buckets of one party (base currency), or nothing when the party has no open item. */
export function AgeingStrip({
  ageing,
  partyCode,
}: Readonly<{ ageing: Ageing | undefined; partyCode: string }>) {
  const row = ageing?.rows.find((r) => r.partyCode === partyCode);
  if (ageing === undefined || row === undefined) {
    return null;
  }
  return (
    <Card title={`Ageing by due date as of ${formatDate(ageing.asOf)} (base currency)`}>
      <div className="row">
        {ageing.buckets.map((b, i) => (
          <div key={b} className="kpi-label">
            {b}: <strong>{formatAmount(row.amounts[i] ?? 0)}</strong>
          </div>
        ))}
        <div className="kpi-label">
          Total: <strong>{formatAmount(row.total)}</strong>
        </div>
      </div>
    </Card>
  );
}
