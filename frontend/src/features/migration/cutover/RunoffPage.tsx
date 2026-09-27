import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Camera, Plus } from 'lucide-react';
import { useState } from 'react';
import { cutoverApi } from '@/api/migrationCutover';
import type { DecommissionItem, RunoffCohort } from '@/api/migrationCutover';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime, today } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION } from '../common/migrationCodes';
import { itemActions } from './cutoverActions';
import '../migration.css';

const SYSTEMS = ['EBIX', 'QPS', 'ISYS', 'CMS'];

const COHORTS: Column<RunoffCohort>[] = [
  { key: 'month', header: 'Expiry Month', render: (c) => c.expiryMonth.slice(0, 7) },
  { key: 'system', header: 'System', render: (c) => c.sourceSystem },
  { key: 'inforce', header: 'In Force', numeric: true, render: (c) => c.headersInForce },
  {
    key: 'premium',
    header: 'Premium in Force',
    kind: 'amount',
    render: (c) => formatAmount(c.premiumInForce),
  },
  { key: 'renewed', header: 'Renewed', numeric: true, render: (c) => c.renewed },
  { key: 'not', header: 'Not Renewed', numeric: true, render: (c) => c.notRenewed },
  { key: 'lapsed', header: 'Lapsed', numeric: true, render: (c) => c.lapsed },
  { key: 'open', header: 'Still Open', numeric: true, render: (c) => c.stillOpen },
];

/**
 * Run-off and Decommissioning (DATA_MIGRATION_DESIGN 17.6): the monthly run-off of the legacy
 * in-force headers by expiry month, and the checklists that close each legacy system and, once
 * every legacy item is settled, the legacy context of BIBS.
 */
export default function RunoffPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const client = useQueryClient();
  const manage = can('MIG_CUTOVER_MANAGE');
  const [action, setAction] = useState<MigAction>();
  const [system, setSystem] = useState('EBIX');
  const runoff = useQuery({
    queryKey: ['migration', 'runoff', companyId],
    queryFn: () => cutoverApi.runoff(companyId),
    enabled: companyId > 0,
  });
  const checklists = useQuery({
    queryKey: ['migration', 'decommission', companyId],
    queryFn: () => cutoverApi.checklists(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => client.invalidateQueries({ queryKey: ['migration'] });
  const snapshot = useMutation({
    mutationFn: () => cutoverApi.snapshot(companyId, today()),
    onSuccess: refresh,
  });
  const open = useMutation({
    mutationFn: () => cutoverApi.open(companyId, system),
    onSuccess: refresh,
  });
  const cohorts = runoff.data ?? [];
  const items = checklists.data ?? [];
  const opened = new Set(items.map((i) => i.systemCode));
  const itemColumns: Column<DecommissionItem>[] = [
    {
      key: 'system',
      header: 'System',
      render: (i) => (
        <CellStack
          main={i.systemCode === 'LEGACY' ? 'Legacy context' : i.systemCode}
          sub={i.milestone === 'CONTEXT' ? 'Legacy context closed' : 'System decommissioned'}
        />
      ),
    },
    {
      key: 'criterion',
      header: 'Criterion',
      render: (i) => <CellStack main={i.name} sub={i.description} />,
    },
    {
      key: 'evidence',
      header: 'Evidence',
      render: (i) => (
        <CellStack
          main={i.evidence ?? ''}
          sub={i.signedBy === undefined ? '' : `${i.signedBy}, ${formatDateTime(i.signedAt)}`}
        />
      ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (i) => <MigStatus status={i.status} />,
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (i) =>
        manage ? (
          <span className="mig-actions">
            {itemActions(i).map((a) => (
              <Button key={a.title} variant="secondary" size="sm" onClick={() => setAction(a)}>
                {a.confirmLabel}
              </Button>
            ))}
          </span>
        ) : null,
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Run-off and Decommissioning"
        description="Legacy policies running off after go-live, and the checklists that close each legacy system."
        actions={
          manage && (
            <Button
              variant="secondary"
              icon={<Camera size={14} />}
              busy={snapshot.isPending}
              onClick={() => snapshot.mutate()}
            >
              Take Snapshot
            </Button>
          )
        }
      />
      <Card
        title={
          cohorts.length === 0
            ? 'Run-off'
            : `Run-off at ${formatDate(cohorts[0]?.snapshotDate ?? '')}`
        }
        flush
      >
        <ErrorAlert error={runoff.error ?? snapshot.error} onRetry={() => void runoff.refetch()} />
        <DataTable<RunoffCohort>
          loading={runoff.isLoading}
          rows={cohorts}
          rowKey={(c) => `${c.expiryMonth}-${c.sourceSystem}`}
          emptyMessage="No run-off snapshot yet; the snapshot is taken on the first of each month"
          columns={COHORTS}
        />
      </Card>
      <Card
        title="Decommissioning Checklists"
        actions={
          manage && (
            <span className="mig-actions">
              <Field label="Legacy System">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={system}
                    onChange={(e) => setSystem(e.target.value)}
                  >
                    {SYSTEMS.filter((s) => !opened.has(s)).map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Button
                variant="secondary"
                size="sm"
                icon={<Plus size={14} />}
                disabled={opened.has(system)}
                busy={open.isPending}
                onClick={() => open.mutate()}
              >
                Open Checklist
              </Button>
            </span>
          )
        }
        flush
      >
        <ErrorAlert
          error={checklists.error ?? open.error}
          onRetry={() => void checklists.refetch()}
        />
        <DataTable<DecommissionItem>
          loading={checklists.isLoading}
          rows={items}
          rowKey={(i) => String(i.id)}
          columns={itemColumns}
        />
      </Card>
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}
