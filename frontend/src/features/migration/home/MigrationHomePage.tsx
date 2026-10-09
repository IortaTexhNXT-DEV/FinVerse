import { useQuery } from '@tanstack/react-query';
import { FileSpreadsheet, Layers, Upload } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { HomeTiles } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, titleCase } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel } from '../common/migrationCodes';
import '../migration.css';
import { BRAND } from '@/branding';

interface Tile {
  id: string;
  label: string;
  to: string;
  value: (t: HomeTiles) => number;
  alert?: boolean;
}

const sum = (m: Record<string, number>, keys: string[]) =>
  keys.reduce((total, k) => total + (m[k] ?? 0), 0);

const TILES: Tile[] = [
  {
    id: 'decided',
    label: 'Objects decided',
    to: '/migration/objects',
    value: (t) => t.objectsByStatus.DECIDED ?? 0,
  },
  {
    id: 'pending',
    label: 'Objects waiting for a decision',
    to: '/migration/objects',
    value: (t) => sum(t.objectsByStatus, ['PROPOSED', 'FOR_DECISION']),
  },
  {
    id: 'running',
    label: 'Batches in progress',
    to: '/migration/batches',
    value: (t) =>
      sum(t.batchesByStatus, ['PLANNED', 'VALIDATING', 'VALIDATED', 'APPROVED', 'LOADING']),
  },
  {
    id: 'failed',
    label: 'Batches failed or with rejects',
    to: '/migration/batches',
    value: (t) => sum(t.batchesByStatus, ['FAILED', 'LOADED_WITH_REJECTS']),
    alert: true,
  },
  {
    id: 'breaks',
    label: 'Open reconciliation breaks',
    to: '/migration/reconciliation',
    value: (t) => t.openBreaks,
    alert: true,
  },
  {
    id: 'unmapped',
    label: 'Unmapped legacy codes',
    to: '/migration/maps?tab=unmapped',
    value: (t) => t.unmappedCodes,
    alert: true,
  },
  {
    id: 'errors',
    label: 'Open validation errors',
    to: '/migration/batches',
    value: (t) => t.openErrors,
    alert: true,
  },
  {
    id: 'review',
    label: 'Client pairs to review',
    to: '/migration/matching',
    value: (t) => t.pairsToReview,
  },
];

/**
 * Migration Home (DATA_MIGRATION_DESIGN section 22): objects by status, batches running or failed,
 * open breaks, unmapped codes, the next cut-over tasks and the run-off of the legacy policies.
 */
export default function MigrationHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const tiles = useQuery({
    queryKey: ['migration', 'home', companyId],
    queryFn: () => migrationApi.home(companyId),
    enabled: companyId > 0,
  });
  const data = tiles.data;
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Migration Home"
        description={`Progress of the data migration from the legacy systems to ${BRAND.product}.`}
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileSpreadsheet size={16} />}
              onClick={() => void navigate('/migration/layouts')}
            >
              Load Templates
            </Button>
            <Button
              variant="secondary"
              icon={<Layers size={16} />}
              onClick={() => void navigate('/migration/batches')}
            >
              Batches
            </Button>
            {can('MIG_INTAKE') && (
              <Button
                variant="primary"
                icon={<Upload size={16} />}
                onClick={() => void navigate('/migration/extracts')}
              >
                Upload Extract
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={tiles.error} onRetry={() => void tiles.refetch()} />
      <div className="grid-4">
        {TILES.map((tile) => {
          const value = data === undefined ? '–' : String(tile.value(data));
          return (
            <Link key={tile.id} to={tile.to} className="card kpi" aria-label={tile.label}>
              <div className="kpi-label">{titleCase(tile.label)}</div>
              <div className="kpi-value">{value}</div>
              {tile.alert === true && value !== '0' && value !== '–' && (
                <div className="kpi-hint">Needs attention</div>
              )}
            </Link>
          );
        })}
      </div>
      <div className="mig-two">
        <Card title="Next cut-over tasks">
          <DataTable
            loading={tiles.isLoading}
            rows={data?.nextTasks ?? []}
            rowKey={(t) => `${t.planNo}-${String(t.seq)}`}
            emptyMessage="No open cut-over task"
            columns={[
              { key: 'plan', header: 'Plan', kind: 'code', render: (t) => t.planNo },
              { key: 'task', header: 'Task', render: (t) => t.task },
              { key: 'owner', header: 'Owner', render: (t) => migLabel(t.ownerRole) },
              {
                key: 'start',
                header: 'Planned start',
                kind: 'datetime',
                render: (t) => formatDateTime(t.plannedStart),
              },
              {
                key: 'status',
                header: 'Status',
                kind: 'status',
                render: (t) => <MigStatus status={t.status} />,
              },
            ]}
          />
        </Card>
        <Card title="Run-off of the legacy policies">
          <DataTable
            loading={tiles.isLoading}
            rows={data?.runoff ?? []}
            rowKey={(r) => r.expiryMonth}
            emptyMessage="No run-off snapshot yet"
            columns={[
              {
                key: 'month',
                header: 'Expiry month',
                render: (r) => r.expiryMonth.slice(0, 7),
              },
              { key: 'inForce', header: 'In force', kind: 'center', render: (r) => r.inForce },
              { key: 'renewed', header: 'Renewed', kind: 'center', render: (r) => r.renewed },
              {
                key: 'notRenewed',
                header: 'Not renewed',
                kind: 'center',
                render: (r) => r.notRenewed,
              },
              { key: 'open', header: 'Still open', kind: 'center', render: (r) => r.stillOpen },
            ]}
          />
        </Card>
      </div>
    </div>
  );
}
