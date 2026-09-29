import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { Batch } from '@/api/migration';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION } from '../common/migrationCodes';
import '../migration.css';
import { ReconPanel } from './ReconPanel';
import { BRAND } from '@/branding';

const RECONCILABLE = ['LOADED', 'LOADED_WITH_REJECTS', 'RECONCILED', 'SIGNED_OFF'];

/**
 * Reconciliation (DATA_MIGRATION_DESIGN section 12): the loaded batches with their reconciliation
 * status and, for the chosen batch, the lines by level with the break explanations and approvals.
 * The report MIG-RECON-DETAIL exports the lines.
 */
export default function ReconciliationPage() {
  const companyId = useCompanyId();
  const [params, setParams] = useSearchParams();
  const batches = useQuery({
    queryKey: ['migration', 'batches', companyId, 'recon'],
    queryFn: () => migrationApi.batches(companyId, 0, 200),
    enabled: companyId > 0,
  });
  const rows = (batches.data?.content ?? []).filter((b) => RECONCILABLE.includes(b.status));
  const selected = params.get('batch') ?? undefined;
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Reconciliation"
        description={`Source, staging and ${BRAND.product} compared per batch: counts, amounts, hash totals, fields and the GL.`}
      />
      <Card flush>
        <ErrorAlert error={batches.error} onRetry={() => void batches.refetch()} />
        <DataTable<Batch>
          loading={batches.isLoading}
          rows={rows}
          rowKey={(b) => b.batchNo}
          selectedKey={selected}
          onRowClick={(b) => setParams({ batch: b.batchNo })}
          emptyMessage="No loaded batch to reconcile"
          columns={[
            { key: 'batch', header: 'Batch', kind: 'code', render: (b) => b.batchNo },
            { key: 'object', header: 'Object', kind: 'code', render: (b) => b.objectCode },
            { key: 'loaded', header: 'Loaded', kind: 'center', render: (b) => b.counts.loaded },
            {
              key: 'ended',
              header: 'Load ended',
              kind: 'datetime',
              render: (b) => formatDateTime(b.endedAt),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (b) => <MigStatus status={b.status} />,
            },
          ]}
        />
      </Card>
      {selected !== undefined && <ReconPanel batchNo={selected} />}
    </div>
  );
}
