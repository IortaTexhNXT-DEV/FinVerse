import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { MatchPair } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tag } from '@/components/ui/Tag';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION } from '../common/migrationCodes';
import '../migration.css';

/**
 * Client Matching (DATA_MIGRATION_DESIGN section 9): the pairs of legacy client records, or of a
 * legacy record and a BIBS client, whose score is between the review and the automatic thresholds;
 * the two records side by side with the keys that matched, and the Data Steward's decision to
 * merge them or keep them separate. The load of a client batch waits until its queue is empty.
 */
export default function MatchingPage() {
  const [pairId, setPairId] = useState<number>();
  const queue = useQuery({
    queryKey: ['migration', 'matches'],
    queryFn: () => migrationApi.matches(),
  });
  const pair = queue.data?.find((p) => p.id === pairId);
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Client Matching"
        description="Doubtful client pairs to merge or keep separate before the client load."
      />
      <Card flush>
        <ErrorAlert error={queue.error} onRetry={() => void queue.refetch()} />
        <DataTable<MatchPair>
          loading={queue.isLoading}
          rows={queue.data ?? []}
          rowKey={(p) => p.id}
          selectedKey={pairId}
          onRowClick={(p) => setPairId(p.id)}
          emptyMessage="No client pair waiting for review"
          columns={[
            { key: 'batch', header: 'Batch', kind: 'code', render: (p) => p.batchNo ?? '' },
            { key: 'cluster', header: 'Cluster', kind: 'center', render: (p) => p.clusterNo },
            { key: 'left', header: 'Legacy client', kind: 'code', render: (p) => p.leftKey },
            {
              key: 'right',
              header: 'Matched with',
              render: (p) => (
                <CellStack
                  main={p.rightKey ?? p.rightClientCode ?? ''}
                  sub={p.rightClientCode ? 'BIBS client' : 'Legacy client'}
                />
              ),
            },
            { key: 'score', header: 'Score', kind: 'center', render: (p) => p.score },
            { key: 'keys', header: 'Keys matched', render: (p) => p.matchedKeys },
            {
              key: 'status',
              header: 'Decision',
              kind: 'status',
              render: (p) => <MigStatus status={p.decision} />,
            },
          ]}
        />
      </Card>
      {pair !== undefined && <PairCard pair={pair} onDecided={() => setPairId(undefined)} />}
    </div>
  );
}

function PairCard({ pair, onDecided }: Readonly<{ pair: MatchPair; onDecided: () => void }>) {
  const { can } = useAuth();
  const client = useQueryClient();
  const pairId = pair.id;
  const sides = useQuery({
    queryKey: ['migration', 'matches', pairId, 'sides'],
    queryFn: () => migrationApi.matchSides(pairId),
  });
  const decide = useMutation({
    mutationFn: (merge: boolean) => migrationApi.decideMatch(pairId, merge),
    onSuccess: async () => {
      onDecided();
      await client.invalidateQueries({ queryKey: ['migration'] });
    },
  });
  const keys = [
    ...new Set([...Object.keys(sides.data?.left ?? {}), ...Object.keys(sides.data?.right ?? {})]),
  ].sort((a, b) => a.localeCompare(b));
  const matched = new Set(pair.matchedKeys.split(/[,\s]+/));
  const other = pair.rightKey ?? pair.rightClientCode ?? 'Other record';
  return (
    <Card
      title={`Pair ${pair.leftKey} and ${other}`}
      actions={
        can('MIG_MATCH_DECIDE') &&
        pair.decision === 'REVIEW' && (
          <span className="mig-actions">
            <Button variant="primary" busy={decide.isPending} onClick={() => decide.mutate(true)}>
              Merge
            </Button>
            <Button
              variant="secondary"
              busy={decide.isPending}
              onClick={() => decide.mutate(false)}
            >
              Keep Separate
            </Button>
          </span>
        )
      }
    >
      <ErrorAlert error={sides.error ?? decide.error} />
      <DataTable<string>
        loading={sides.isLoading}
        rows={keys}
        rowKey={(k) => k}
        emptyMessage="No values"
        columns={[
          {
            key: 'field',
            header: 'Field',
            render: (k) => (matched.has(k) ? <Tag tone="info">{k}</Tag> : k),
          },
          { key: 'left', header: pair.leftKey, render: (k) => sides.data?.left[k] ?? '' },
          {
            key: 'right',
            header: other,
            render: (k) => sides.data?.right[k] ?? '',
          },
        ]}
      />
    </Card>
  );
}
