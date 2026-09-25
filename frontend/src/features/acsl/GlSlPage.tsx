import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { hasVariance } from './acsl';
import { acslApi } from './api';
import type { GlSlRow, GlSlRun } from './api';
import { GlSlControlsCard } from './GlSlControlsCard';
import './acsl.css';

const RUN_COLUMNS: Column<GlSlRun>[] = [
  {
    key: 'asOf',
    header: 'As Of',
    render: (r) => (
      <>
        <strong>{formatDate(r.asOf)}</strong>
        <span className="cell-sub">{formatDateTime(r.runAt)}</span>
      </>
    ),
  },
  { key: 'by', header: 'Run By', render: (r) => r.runBy },
  { key: 'accounts', header: 'Accounts', numeric: true, render: (r) => r.accounts },
  { key: 'differences', header: 'Differences', numeric: true, render: (r) => r.differences },
  {
    key: 'total',
    header: 'Total Difference',
    numeric: true,
    render: (r) => (
      <span className={hasVariance(r.totalDifference) ? 'acsl-variance' : undefined}>
        <Amount value={r.totalDifference} />
      </span>
    ),
  },
];

const ROW_COLUMNS: Column<GlSlRow>[] = [
  {
    key: 'account',
    header: 'Control Account',
    render: (r) => (
      <>
        <strong>{r.accountCode}</strong>
        <span className="cell-sub">{r.accountName}</span>
      </>
    ),
  },
  { key: 'source', header: 'Sub-ledger', render: (r) => humanize(r.source) },
  { key: 'gl', header: 'GL Balance', numeric: true, render: (r) => <Amount value={r.glBalance} /> },
  { key: 'sl', header: 'SL Balance', numeric: true, render: (r) => <Amount value={r.slBalance} /> },
  {
    key: 'diff',
    header: 'Difference',
    numeric: true,
    render: (r) => (
      <span className={hasVariance(r.difference) ? 'acsl-variance' : undefined}>
        <Amount value={r.difference} />
      </span>
    ),
  },
];

/**
 * GL-SL reconciliation (ACSL 2.16.0): the balance of each control account against its sub-ledger,
 * run nightly by the scheduler or on demand as of a date, with the differences highlighted and
 * alerted, and the control accounts compared.
 */
export default function GlSlPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useState('');
  const [picked, setPicked] = useState<number>();
  const runs = useQuery({
    queryKey: ['acsl', 'glslRuns', companyId],
    queryFn: () => acslApi.glSlRuns(companyId),
    enabled: companyId > 0,
  });
  const runId = picked ?? runs.data?.[0]?.id;
  const rows = useQuery({
    queryKey: ['acsl', 'glslRows', runId],
    queryFn: () => acslApi.glSlRows(runId ?? 0),
    enabled: runId !== undefined,
  });
  const run = useMutation({
    mutationFn: () => acslApi.runGlSl(companyId, asOf),
    onSuccess: async (r) => {
      setPicked(r.id);
      await queryClient.invalidateQueries({ queryKey: ['acsl', 'glslRuns'] });
      toast.success(`${String(r.differences)} difference(s) as of ${formatDate(r.asOf)}`);
    },
  });
  const shown = runs.data?.find((r) => r.id === runId);
  return (
    <div className="stack">
      <PageHeader
        section="Finance · ACSL"
        title="GL-SL Reconciliation"
        description="Compares the balance of each control account in the general ledger with its sub-ledger; runs every night and on demand."
      />
      <ErrorAlert error={runs.error ?? run.error} />
      {can('ACSL_PROCESS') && (
        <Card title="Run Now">
          <div className="form-grid">
            <Field label="As Of" hint="Leave empty for today">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={asOf}
                  onChange={(e) => setAsOf(e.target.value)}
                />
              )}
            </Field>
          </div>
          <Button
            variant="primary"
            icon={<Play size={16} />}
            busy={run.isPending}
            onClick={() => run.mutate()}
          >
            Run Reconciliation
          </Button>
        </Card>
      )}
      <Card title="Runs" flush>
        <DataTable
          caption="GL-SL reconciliation runs"
          columns={RUN_COLUMNS}
          rows={runs.data ?? []}
          rowKey={(r) => r.id}
          loading={runs.isLoading}
          emptyMessage="No items to display"
          onRowClick={(r) => setPicked(r.id)}
        />
      </Card>
      {shown && (
        <Card title={`Balances as of ${formatDate(shown.asOf)}`} flush>
          <ErrorAlert error={rows.error} />
          <DataTable
            caption="Control account balances"
            columns={ROW_COLUMNS}
            rows={rows.data ?? []}
            rowKey={(r) => `${r.accountCode}:${r.source}`}
            loading={rows.isLoading}
            emptyMessage="No items to display"
          />
        </Card>
      )}
      <GlSlControlsCard />
    </div>
  );
}
