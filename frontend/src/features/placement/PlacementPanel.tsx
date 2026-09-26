import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { InsurerReturn } from '@/api/placement';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { placementLink } from './placementLogic';

function returnText(r: InsurerReturn): string {
  const resolved = r.resolution === undefined ? 'open' : humanize(r.resolution);
  return `${humanize(r.reasonCode)} on ${formatDateTime(r.createdAt)} (${resolved})`;
}

/**
 * Placement tab of the account detail page: payment gate, current slip, hold cover and the last
 * insurer return, with a link to the account's placement record.
 */
export function PlacementPanel({ arn }: Readonly<{ arn: string }>) {
  const view = useQuery({
    queryKey: ['placement', 'account', arn],
    queryFn: () => placementApi.account(arn),
  });
  if (view.data === undefined) {
    return view.error ? (
      <ErrorAlert error={view.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const { gate, slips, holdCovers, returns } = view.data;
  const slip = slips.find((s) => s.status !== 'SUPERSEDED');
  const hold = holdCovers[0];
  const lastReturn = returns[0];
  return (
    <Card
      title="Placement"
      actions={
        <Link className="btn btn-secondary btn-sm" to={placementLink(arn)}>
          <ExternalLink size={14} aria-hidden="true" /> Open Placement
        </Link>
      }
    >
      <dl className="detail-list">
        <dt>Payment gate</dt>
        <dd>
          <StatusBadge status={gate.open ? 'OPEN' : 'PENDING'} /> {humanize(gate.rule)} ·{' '}
          {gate.ruleDescription}
        </dd>
        <dt>Placement slip</dt>
        <dd>
          {slip ? (
            <>
              <code>{slip.displayNo}</code> <StatusBadge status={slip.status} />{' '}
              {slip.sentAt !== undefined && (
                <span className="muted">sent {formatDateTime(slip.sentAt)}</span>
              )}
            </>
          ) : (
            'Not generated'
          )}
        </dd>
        <dt>Hold cover</dt>
        <dd>
          {hold
            ? `${humanize(hold.status)}, ${formatDate(hold.startDate)} to ${formatDate(hold.expiryDate)}`
            : 'None'}
        </dd>
        <dt>Insurer return</dt>
        <dd>{lastReturn ? returnText(lastReturn) : 'None'}</dd>
      </dl>
    </Card>
  );
}
