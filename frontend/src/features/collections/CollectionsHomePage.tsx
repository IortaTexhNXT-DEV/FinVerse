import { useQuery } from '@tanstack/react-query';
import { ListChecks } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import { collectionsApi } from './api';
import type { AgingCell } from './api';
import { agingBars, agingSegments } from './collectionsLogic';
import './collections.css';

/** Open amounts per aging bracket, for all segments or one (BRCLXN.001-012, OQ43). */
function AgingChart({ cells }: Readonly<{ cells: AgingCell[] }>) {
  const [segment, setSegment] = useState('');
  const bars = agingBars(cells, segment);
  return (
    <Card
      title="Open Accounts by Aging Bracket"
      actions={
        <label className="checkbox">
          <span className="visually-hidden">Market segment</span>
          <select className="select" value={segment} onChange={(e) => setSegment(e.target.value)}>
            <option value="">All segments</option>
            {agingSegments(cells).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
      }
    >
      {bars.length === 0 ? (
        <EmptyState message="No open accounts" />
      ) : (
        <div className="clx-aging" role="table" aria-label="Open accounts by aging bracket">
          {bars.map((b) => (
            <div
              key={b.bracket}
              className="clx-aging-row"
              role="row"
              title={`${b.bracket} days: ${b.items} account(s), ${formatAmount(b.amount)}`}
            >
              <span className="clx-aging-label" role="rowheader">
                {b.bracket}
              </span>
              <span className="clx-aging-track" role="cell" aria-hidden="true">
                <span className="clx-aging-bar" style={{ width: `${b.share * 100}%` }} />
              </span>
              <span className="clx-aging-value" role="cell">
                {formatAmount(b.amount)} · {b.items}
              </span>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

/**
 * Collections home (COLLECTIONS_DESIGN 11, CQ21): the collector's work queues as tiles - my open
 * accounts, unassigned accounts, direct payments returned by insurers, credit balances, files ready
 * and the tiles of the plans, escalation and unapplied-payment screens - and the open amounts per
 * aging bracket and segment.
 */
export default function CollectionsHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const home = useQuery({
    queryKey: ['collections', 'home', companyId],
    queryFn: () => collectionsApi.home(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Collections Home"
        description="Outstanding premium receivables to follow up: your accounts, assignments, direct payments returned by insurers and the files of the day."
        actions={
          can('CLX_VIEW') ? (
            <Button
              icon={<ListChecks size={16} />}
              onClick={() => void navigate('/collections/worklist')}
            >
              Open PR Worklist
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={home.error} />
      {home.isLoading && <span className="spinner" aria-label="Loading" />}
      {home.data !== undefined && (
        <>
          <WorkTiles
            label="Collections work"
            tiles={home.data.tiles.map((t) => ({
              key: t.key,
              label: t.label,
              value: t.value,
              alert: t.alert,
              onClick: () => void navigate(t.link),
            }))}
          />
          <AgingChart cells={home.data.aging} />
        </>
      )}
    </div>
  );
}
