import { useQuery } from '@tanstack/react-query';
import { FilePlus2, ListChecks } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { PHASE_LABELS } from '../status/statusLogic';
import type { ClaimPhase } from '../status/api';
import { claimsHomeApi } from './api';
import type { BucketCount, StatusCount } from './api';
import { bucketShares } from './homeLogic';
import './home.css';

/** Outstanding claims per ageing bucket (BRCLM.025/026; BCL_AGEING_BUCKETS). */
function AgeingChart({ buckets }: Readonly<{ buckets: BucketCount[] }>) {
  const bars = bucketShares(buckets);
  return (
    <Card title="Outstanding Claims by Age">
      {bars.every((b) => b.claims === 0) ? (
        <EmptyState message="No outstanding claims" />
      ) : (
        <div className="bcl-ageing" role="table" aria-label="Outstanding claims by age">
          {bars.map((b) => (
            <div
              key={b.bucket}
              className="bcl-ageing-row"
              role="row"
              title={`${b.bucket} days: ${b.claims} claim(s)`}
            >
              <span className="bcl-ageing-label" role="rowheader">
                {b.bucket}
              </span>
              <span className="bcl-ageing-track" role="cell" aria-hidden="true">
                <span className="bcl-ageing-bar" style={{ width: `${b.share * 100}%` }} />
              </span>
              <span className="bcl-ageing-value" role="cell">
                {b.claims}
              </span>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

/**
 * Claims home (NFR 15.03, BRCLM.025/034; FR-CM-055): the handler's work tiles (open claims,
 * follow-ups due and overdue, diary due, temporarily closed, unpaid premium, awaiting premium
 * remittance) that open the worklist filtered, the open claims by status and the ageing buckets.
 */
export default function ClaimsHomePage() {
  const navigate = useNavigate();
  const companyId = useCompanyId();
  const { can } = useAuth();
  const home = useQuery({
    queryKey: ['broker-claims', 'home', companyId],
    queryFn: () => claimsHomeApi.home(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Claims Home"
        description="Your claims at a glance: open claims, follow-ups due today and overdue, claims by status, ageing and claims waiting on premium."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<ListChecks size={16} />}
              onClick={() => void navigate('/claims-handling/worklist')}
            >
              Open Worklist
            </Button>
            {can('BCL_RECORD') && (
              <Button
                variant="accent"
                icon={<FilePlus2 size={16} />}
                onClick={() => void navigate('/claims-handling/new')}
              >
                Record Claim
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={home.error} />
      {home.isLoading && <span className="spinner" aria-label="Loading" />}
      {home.data !== undefined && (
        <>
          <WorkTiles
            label="Claims work"
            tiles={home.data.tiles.map((t) => ({
              key: t.key,
              label: t.label,
              value: t.value,
              alert: t.alert,
              onClick: () => void navigate(t.link),
            }))}
          />
          <div className="grid-2">
            <Card title="Open Claims by Status" flush>
              <DataTable<StatusCount>
                caption="Open claims by status"
                rows={home.data.byStatus}
                rowKey={(s) => `${s.phase}-${s.statusCode ?? ''}`}
                emptyMessage="No open claims"
                onRowClick={(s) =>
                  void navigate(
                    `/claims-handling/worklist?tab=ALL&status=${encodeURIComponent(s.statusCode ?? '')}`,
                  )
                }
                columns={[
                  { key: 'status', header: 'Status', render: (s) => s.statusLabel ?? '—' },
                  {
                    key: 'phase',
                    header: 'Phase',
                    render: (s) => <StatusBadge status={PHASE_LABELS[s.phase as ClaimPhase]} />,
                  },
                  { key: 'claims', header: 'Claims', numeric: true, render: (s) => s.claims },
                ]}
              />
            </Card>
            <AgeingChart buckets={home.data.ageing} />
          </div>
        </>
      )}
    </div>
  );
}
