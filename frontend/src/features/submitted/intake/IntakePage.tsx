import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { IntakeRunView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { SOURCE_LABELS, SUBMITTED_SECTION } from '../common/submittedCodes';
import { UploadPanel } from '../common/UploadPanel';

/** Uploads of the sources with their bulk handler (the register of Setup). */
const SOURCES: Record<string, string> = {
  LFS_INSURANCE: 'SBM_LFS_INSURANCE',
  HLS_INSURANCE: 'SBM_HLS_INSURANCE',
  CIU: 'SBM_CIU',
  SPI: 'SBM_SPI',
  LOAN_BOOKING: 'SBM_LOAN_BOOKING',
  IA_MASTERLIST: 'SBM_IA_MASTERLIST',
};

type Panel = 'source' | 'lamd' | 'migration' | null;

/**
 * Upload & Intake (FR-SP-001 to 006): the uploads of the bank sources, the LAMD loan snapshot
 * matched by the processing runs and the migration of the Excel masterlists, with the intake runs.
 */
export default function IntakePage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [panel, setPanel] = useState<Panel>(null);
  const [source, setSource] = useState('LFS_INSURANCE');
  const [page, setPage] = useState(0);
  const runs = useQuery({
    queryKey: ['submitted', 'intake-runs', companyId, page],
    queryFn: () => submittedApi.intakeRuns(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Upload & Intake"
        description="Source files of the bank, the LAMD loan snapshot and the migration of the Excel masterlists."
        actions={
          <>
            {can('SBM_INTAKE') && (
              <>
                <Button icon={<Upload size={16} />} onClick={() => setPanel('source')}>
                  Upload Source File
                </Button>
                <Button variant="secondary" onClick={() => setPanel('lamd')}>
                  Upload LAMD Snapshot
                </Button>
              </>
            )}
            {can('SBM_MIGRATE') && (
              <Button variant="secondary" onClick={() => setPanel('migration')}>
                Migrate Masterlist
              </Button>
            )}
          </>
        }
      />
      {panel === 'source' && (
        <UploadPanel
          label={`Upload ${SOURCE_LABELS[source] ?? source}`}
          handler={SOURCES[source] ?? 'SBM_LFS_INSURANCE'}
          parameterFields={
            <div className="form-grid">
              <Field label="Source" required>
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                  >
                    {Object.keys(SOURCES).map((code) => (
                      <option key={code} value={code}>
                        {SOURCE_LABELS[code] ?? code}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
            </div>
          }
          onCommitted={refresh}
          onClose={() => setPanel(null)}
        />
      )}
      {panel === 'lamd' && (
        <UploadPanel
          label="Upload LAMD Snapshot"
          handler="SBM_LAMD"
          onCommitted={refresh}
          onClose={() => setPanel(null)}
        />
      )}
      {panel === 'migration' && (
        <UploadPanel
          label="Migrate Excel Masterlist"
          handler="SBM_MIGRATION"
          onCommitted={refresh}
          onClose={() => setPanel(null)}
        />
      )}
      <ErrorAlert error={runs.error} onRetry={() => void runs.refetch()} />
      <Card title="Intake Runs" flush>
        <DataTable<IntakeRunView>
          loading={runs.isLoading}
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No intake run yet"
          columns={[
            { key: 'no', header: 'Run', kind: 'code', render: (r) => r.runNo },
            {
              key: 'source',
              header: 'Source',
              render: (r) => SOURCE_LABELS[r.sourceCode] ?? r.sourceCode,
            },
            { key: 'file', header: 'File', render: (r) => r.fileName ?? '—' },
            { key: 'received', header: 'Received', kind: 'amount', render: (r) => r.received },
            { key: 'created', header: 'New', kind: 'amount', render: (r) => r.created },
            { key: 'updated', header: 'Updated', kind: 'amount', render: (r) => r.updated },
            { key: 'dup', header: 'Duplicates', kind: 'amount', render: (r) => r.duplicate },
            { key: 'failed', header: 'Refused', kind: 'amount', render: (r) => r.failed },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (r) => <StatusBadge status={r.status} />,
            },
            {
              key: 'run',
              header: 'Processing Run',
              kind: 'code',
              render: (r) => r.processingRunNo ?? '—',
            },
            {
              key: 'at',
              header: 'Started',
              kind: 'datetime',
              render: (r) => formatDateTime(r.startedAt),
            },
          ]}
          footer={<PageFooter data={runs.data} noun="runs" onPage={setPage} />}
        />
      </Card>
    </div>
  );
}
