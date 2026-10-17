import { useQuery } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';
import type { LimitCheckView, ResultView } from '@/api/submitted';
import { LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { SBM_LOV, STEP_LABELS } from '../common/submittedCodes';

/** The rule results of the processing runs of a record and its insurer limit checks. */
export function RuleResultsTab({ policyId }: Readonly<{ policyId: number }>) {
  const results = useQuery({
    queryKey: ['submitted', 'results-of', policyId],
    queryFn: () => submittedApi.resultsOf(policyId),
  });
  return (
    <div className="stack">
      <ErrorAlert error={results.error} />
      <Card title="Rule Results" flush>
        <DataTable<ResultView>
          loading={results.isLoading}
          rows={results.data?.results ?? []}
          rowKey={(r) => r.id}
          emptyMessage="Not processed yet"
          columns={[
            {
              key: 'at',
              header: 'Run Date',
              kind: 'datetime',
              render: (r) => formatDateTime(r.at),
            },
            { key: 'step', header: 'Step', render: (r) => STEP_LABELS[r.step] ?? r.step },
            {
              key: 'outcome',
              header: 'Outcome',
              kind: 'status',
              render: (r) => <StatusBadge status={r.outcome} />,
            },
            {
              key: 'bucket',
              header: 'Bucket',
              render: (r) => <LovLabel type={SBM_LOV.bucket} code={r.bucket} />,
            },
            {
              key: 'reason',
              header: 'Reason',
              render: (r) => <LovLabel type={SBM_LOV.reason} code={r.reasonCode} />,
            },
            { key: 'rule', header: 'Rule', render: (r) => r.ruleName ?? '—' },
            {
              key: 'set',
              header: 'Rule Set',
              render: (r) =>
                r.ruleSetCode ? `${r.ruleSetCode} v${String(r.ruleSetVersion ?? '')}` : '—',
            },
          ]}
        />
      </Card>
      <Card title="Insurer Limits" flush>
        <DataTable<LimitCheckView>
          loading={results.isLoading}
          rows={results.data?.limitChecks ?? []}
          rowKey={(c) => c.id}
          emptyMessage="No limit checked"
          columns={[
            { key: 'attr', header: 'Limit', render: (c) => c.attribute },
            { key: 'limit', header: 'Allowed', render: (c) => c.limit },
            { key: 'value', header: 'Policy', render: (c) => c.value },
            {
              key: 'breached',
              header: 'Result',
              kind: 'status',
              render: (c) => (
                <StatusBadge
                  status={c.breached ? 'BREACHED' : 'WITHIN'}
                  label={c.breached ? 'Above the limit' : 'Within'}
                />
              ),
            },
            { key: 'at', header: 'Checked', kind: 'datetime', render: (c) => formatDateTime(c.at) },
          ]}
        />
      </Card>
    </div>
  );
}
