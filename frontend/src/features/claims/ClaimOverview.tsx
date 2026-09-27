import type { Claim } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Kpi } from '@/components/ui/Kpi';
import { formatDate, humanize } from '@/utils/format';
import { displayNameOf } from '@/api/users';
import { Notice } from '@/components/ui/Notice';

function share(c: Claim): string {
  if (c.coinsurerCode === undefined) {
    return '100%';
  }
  return `${String(c.sharePct)}% (${c.coinsuranceLeader ? 'leader' : 'follower'} with ${c.coinsurerCode})`;
}

/** Claim header facts, company-share figures and involved parties. */
export function ClaimOverview({ claim: c }: Readonly<{ claim: Claim }>) {
  const facts: [string, string][] = [
    ['Policy', c.policyNo],
    ['Product', `${c.productCode} – ${c.productName}`],
    ['Class', c.businessLine],
    ['Policyholder', `${c.customerCode} – ${c.customerName}`],
    ['Claimant', `${c.claimantCode} – ${c.claimantName}`],
    ['Risk', c.riskDescription ?? 'Whole policy'],
    ['Date of loss', formatDate(c.lossDate)],
    ['Reported on', formatDate(c.reportedDate)],
    ['Nature / cause', `${c.natureOfLoss} – ${c.causeOfLoss}`],
    ['Place of loss', c.lossLocation],
    ['Currency', c.currency],
    ['Our share', share(c)],
    ['Registered by', displayNameOf(c.createdBy)],
    ['Closed on', c.closedOn === undefined ? '—' : formatDate(c.closedOn)],
  ];
  return (
    <div className="stack">
      <div className="grid-4">
        <Kpi label="Estimate (our share)" value={<Amount value={c.totals.ourEstimate} />} />
        <Kpi label="Paid" value={<Amount value={c.totals.ourPaid} />} />
        <Kpi
          label="Outstanding reserve"
          value={<Amount value={c.totals.ourOutstanding} />}
          accent
        />
        <Kpi label="Recovered" value={<Amount value={c.totals.ourRecovered} />} />
      </div>
      <Card title="Loss">
        <p style={{ marginTop: 0 }}>{c.description}</p>
        <dl className="form-grid" style={{ margin: 0 }}>
          {facts.map(([label, value]) => (
            <div key={label}>
              <dt className="muted">{label}</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
            </div>
          ))}
        </dl>
        {c.statusReason !== undefined && (
          <Notice tone="warning" title={humanize(c.status)}>
            {c.statusReason}
          </Notice>
        )}
      </Card>
      <Card title="Involved parties" flush>
        <DataTable
          rows={c.parties}
          rowKey={(p) => `${p.role}-${p.partyCode}`}
          columns={[
            { key: 'role', header: 'Role', render: (p) => humanize(p.role) },
            { key: 'code', header: 'Code', render: (p) => p.partyCode },
            { key: 'name', header: 'Name', render: (p) => p.partyName },
            { key: 'type', header: 'Type', render: (p) => humanize(p.partyType) },
          ]}
        />
      </Card>
    </div>
  );
}
