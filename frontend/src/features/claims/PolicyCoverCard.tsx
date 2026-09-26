import type { PolicyCover } from '@/api/claims';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';

function shareLabel(cover: PolicyCover): string {
  if (cover.coinsurerCode === undefined) {
    return '100%';
  }
  const role = cover.coinsuranceLeader ? 'leader' : 'follower';
  return `${String(cover.sharePct)}% (${role}, ${cover.coinsurerCode})`;
}

/** Summary of the policy found for a claim notification, with its cover at the loss date. */
export function PolicyCoverCard({ cover }: Readonly<{ cover: PolicyCover }>) {
  const facts: [string, string][] = [
    ['Product', `${cover.productCode} – ${cover.productName}`],
    ['Policyholder', `${cover.customerCode} – ${cover.customerName}`],
    ['Insured', cover.insuredName],
    ['Period', `${formatDate(cover.periodFrom)} – ${formatDate(cover.periodTo)}`],
    ['Currency', cover.currency],
    ['Our share', shareLabel(cover)],
  ];
  return (
    <Card
      title={`Policy ${cover.policyNo}`}
      actions={
        <>
          <StatusBadge status={cover.status} />
          <span className={`badge ${cover.inForce ? 'success' : 'danger'}`}>
            {cover.inForce ? 'In force at loss date' : 'Not in force at loss date'}
          </span>
        </>
      }
    >
      <dl className="form-grid" style={{ margin: 0 }}>
        {facts.map(([label, value]) => (
          <div key={label}>
            <dt className="muted">{label}</dt>
            <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}
