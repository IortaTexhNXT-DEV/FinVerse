import { useQuery } from '@tanstack/react-query';
import { Scale } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { ComparativeSummary, ComparativeView } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { useEbMutation } from '../common/useEbMutation';

const COLUMNS: Column<ComparativeSummary>[] = [
  {
    key: 'no',
    header: 'Comparative',
    kind: 'code',
    render: (c) => <CellStack main={c.comparativeNo} sub={`Version ${String(c.versionNo)}`} />,
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (c) => <StatusBadge status={c.status} />,
  },
  { key: 'due', header: 'Sign-off Due', kind: 'date', render: (c) => formatDate(c.dueDate) },
  {
    key: 'submitted',
    header: 'Submitted By',
    render: (c) => (c.submittedBy ? <UserName login={c.submittedBy} /> : ''),
  },
  {
    key: 'threshold',
    header: 'Threshold',
    render: (c) => (c.thresholdRules ? 'Above the value threshold' : ''),
  },
  {
    key: 'presented',
    header: 'Presented',
    kind: 'datetime',
    render: (c) => formatDateTime(c.presentedAt),
  },
];

/**
 * Comparative tab: the comparatives built on the programme's cycles, newest first; Build
 * Comparative puts the validated proposals of the current cycle side by side. A row opens the
 * comparative with its matrix, recommendation, sign-off and threshold approval.
 */
export function ComparativesTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [building, setBuilding] = useState(false);
  const cycleId = programme.currentCycleId ?? undefined;
  const list = useQuery({
    queryKey: ['eb', 'comparatives', programme.id],
    queryFn: () => ebMarketApi.comparatives(companyId, programme.id),
  });
  const build = useEbMutation(
    (c, id: number) => ebMarketApi.buildComparative(c, id),
    (v: ComparativeView) => `Comparative ${v.comparative.comparativeNo} built`,
    () => setBuilding(false),
  );
  const open = (id: number) => {
    void navigate(`/eb/comparatives/${String(id)}`);
  };
  return (
    <Card
      title="Comparatives"
      actions={
        can('EB_MARKET') &&
        cycleId !== undefined && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Scale size={14} />}
            onClick={() => setBuilding(true)}
          >
            Build Comparative
          </Button>
        )
      }
    >
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <DataTable<ComparativeSummary>
        loading={list.isLoading}
        rows={list.data ?? []}
        rowKey={(c) => c.id}
        columns={COLUMNS}
        onRowClick={(c) => open(c.id)}
        emptyMessage="No comparative built"
      />
      {building && cycleId !== undefined && (
        <ConfirmDialog
          title="Build Comparative"
          effect="The validated proposals of the current cycle are compared per benefit line; the lowest premium is recommended by default. An earlier draft is superseded."
          confirmLabel="Build"
          busy={build.isPending}
          error={build.error}
          onConfirm={() => build.mutate(cycleId, { onSuccess: (v) => open(v.comparative.id) })}
          onClose={() => setBuilding(false)}
        />
      )}
    </Card>
  );
}
