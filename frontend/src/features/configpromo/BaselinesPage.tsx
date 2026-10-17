import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { configPromotionApi } from '@/api/configPromotion';
import type { Baseline, DatasetDrift } from '@/api/configPromotion';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { ItemsTable } from './ItemsTable';
import { driftLabel, driftTotals } from './promotion';

/**
 * Baselines and Drift (Configuration Promotion): the configuration baselines of this environment
 * (a package exported here or applied here, e.g. the UAT signed-off configuration) and the drift
 * of the current configuration from a baseline, dataset by dataset and item by item.
 */
export default function BaselinesPage() {
  const { can } = useAuth();
  const [selected, setSelected] = useState<Baseline | null>(null);
  const [retiring, setRetiring] = useState<Baseline | null>(null);
  const baselines = useQuery({
    queryKey: ['config-promotion', 'baselines'],
    queryFn: configPromotionApi.baselines,
  });
  const actions = (b: Baseline): RowAction[] => {
    const list: RowAction[] = [{ label: 'Show Drift', onSelect: () => setSelected(b) }];
    if (can('CONFIG_BASELINE_MANAGE') && b.active) {
      list.push({ label: 'Retire', danger: true, onSelect: () => setRetiring(b) });
    }
    return list;
  };
  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title="Baselines and Drift"
        description="Compares the current configuration with a signed-off baseline, before go-live and in audits. A package becomes a baseline under Packages and History."
      />
      <Card title="Baselines" flush>
        <ErrorAlert error={baselines.error} />
        <DataTable<Baseline>
          callout="config-baselines"
          loading={baselines.isLoading}
          rows={baselines.data ?? []}
          rowKey={(b) => b.id}
          selectedKey={selected?.id}
          onRowClick={setSelected}
          emptyMessage="No baseline yet: mark a package as baseline under Packages and History."
          columns={[
            {
              key: 'name',
              header: 'Baseline',
              render: (b) => (
                <CellStack main={<strong>{b.name}</strong>} sub={b.remarks ?? undefined} />
              ),
            },
            { key: 'package', header: 'Package', kind: 'code', render: (b) => b.packageNo },
            {
              key: 'env',
              header: 'Environment',
              kind: 'code',
              width: '120px',
              render: (b) => b.environment,
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              width: '120px',
              render: (b) => (
                <StatusBadge
                  status={b.active ? 'ACTIVE' : 'INACTIVE'}
                  label={b.active ? 'In Force' : 'Retired'}
                />
              ),
            },
            {
              key: 'marked',
              header: 'Marked By',
              render: (b) => (
                <CellStack
                  main={<UserName login={b.createdBy} />}
                  sub={formatDateTime(b.createdAt)}
                />
              ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (b) => <RowActionMenu label={b.name} actions={actions(b)} />,
            },
          ]}
        />
      </Card>
      {selected !== null && <DriftReport baseline={selected} />}
      {retiring !== null && <RetireDialog baseline={retiring} onClose={() => setRetiring(null)} />}
    </div>
  );
}

function DriftReport({ baseline }: Readonly<{ baseline: Baseline }>) {
  const [dataset, setDataset] = useState<DatasetDrift | null>(null);
  const drift = useQuery({
    queryKey: ['config-promotion', 'drift', baseline.id],
    queryFn: () => configPromotionApi.drift(baseline.id),
  });
  const items = useQuery({
    queryKey: ['config-promotion', 'drift', baseline.id, dataset?.code],
    queryFn: () => configPromotionApi.driftOf(baseline.id, dataset?.code ?? ''),
    enabled: dataset !== null,
  });
  const rows = drift.data ?? [];
  const totals = driftTotals(rows);
  return (
    <>
      <div className="grid-4">
        <Kpi label="Datasets Drifted" value={totals.datasets} accent />
        <Kpi label="New Since the Baseline" value={totals.added} />
        <Kpi label="Changed" value={totals.changed} />
        <Kpi label="No Longer Present" value={totals.removed} />
      </div>
      <Card title={`Drift from ${baseline.name}`} flush>
        <ErrorAlert error={drift.error} />
        <DataTable<DatasetDrift>
          callout="configuration-drift"
          loading={drift.isLoading}
          rows={rows.filter((d) => !d.comparable || d.added + d.changed + d.removed > 0)}
          rowKey={(d) => d.code}
          selectedKey={dataset?.code}
          emptyMessage="No drift: the configuration is as in the baseline."
          columns={[
            { key: 'name', header: 'Dataset', render: (d) => <strong>{d.name}</strong> },
            { key: 'added', header: 'New', numeric: true, width: '90px', render: (d) => d.added },
            {
              key: 'changed',
              header: 'Changed',
              numeric: true,
              width: '90px',
              render: (d) => d.changed,
            },
            {
              key: 'removed',
              header: 'No Longer Present',
              numeric: true,
              width: '150px',
              render: (d) => d.removed,
            },
            {
              key: 'comparable',
              header: 'Comparison',
              kind: 'status',
              width: '170px',
              render: (d) =>
                d.comparable ? (
                  <StatusBadge status="COMPARED" label="Compared" tone="info" />
                ) : (
                  <StatusBadge status="NOT_COMPARABLE" label="Fields Changed" tone="warning" />
                ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (d) => (
                <RowActionMenu
                  label={d.name}
                  actions={
                    d.comparable ? [{ label: 'View Items', onSelect: () => setDataset(d) }] : []
                  }
                />
              ),
            },
          ]}
        />
      </Card>
      {dataset !== null && (
        <Card
          title={`Drift – ${dataset.name}`}
          actions={
            <Button variant="ghost" size="sm" onClick={() => setDataset(null)}>
              Close
            </Button>
          }
        >
          <ErrorAlert error={items.error} />
          <ItemsTable
            items={items.data?.items ?? []}
            loading={items.isLoading}
            label={driftLabel}
            fromHeader="Baseline"
            toHeader="Now"
          />
        </Card>
      )}
    </>
  );
}

function RetireDialog({
  baseline,
  onClose,
}: Readonly<{ baseline: Baseline; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const retire = useMutation({
    mutationFn: () => configPromotionApi.retireBaseline(baseline.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['config-promotion'] });
      toast.success(`Baseline ${baseline.name} retired`);
      onClose();
    },
  });
  return (
    <ConfirmDialog
      title="Retire Baseline"
      record={baseline.name}
      effect="The baseline is kept for the audit trail but no longer offered for incremental exports."
      confirmLabel="Retire"
      destructive
      busy={retire.isPending}
      error={retire.error}
      onConfirm={() => retire.mutate()}
      onClose={onClose}
    />
  );
}
