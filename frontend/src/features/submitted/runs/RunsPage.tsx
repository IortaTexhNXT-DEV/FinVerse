import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { ResultView, RunView } from '@/api/submitted';
import { LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { SBM_LOV, STEP_LABELS, SUBMITTED_SECTION, policyLink } from '../common/submittedCodes';

function Results({ run }: Readonly<{ run: RunView }>) {
  const [outcome, setOutcome] = useState<'ALL' | 'FALLOUT'>('FALLOUT');
  const results = useQuery({
    queryKey: ['submitted', 'results', run.id, outcome],
    queryFn: () => submittedApi.results(run.id, outcome === 'ALL' ? undefined : outcome),
  });
  return (
    <Card title={`Results of ${run.runNo}`} flush>
      <Tabs
        tabs={[
          { id: 'FALLOUT', label: 'Fallout', count: run.fallout },
          { id: 'ALL', label: 'All Steps' },
        ]}
        active={outcome}
        onChange={setOutcome}
      />
      <ErrorAlert error={results.error} />
      <DataTable<ResultView>
        loading={results.isLoading}
        rows={results.data?.content ?? []}
        rowKey={(r) => r.id}
        emptyMessage={outcome === 'FALLOUT' ? 'No fallout in this run' : 'No results'}
        columns={[
          {
            key: 'sbm',
            header: 'Masterlist No.',
            kind: 'code',
            render: (r) => <Link to={policyLink(r.policyId)}>{r.sbmNo}</Link>,
          },
          { key: 'assured', header: 'Assured', render: (r) => r.assuredName },
          { key: 'step', header: 'Step', render: (r) => STEP_LABELS[r.step] ?? humanize(r.step) },
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
        ]}
      />
    </Card>
  );
}

/**
 * Processing Runs (FR-SP-020 to 024): each run with its counts (bucketed, fallout, overridden,
 * limit breaches) and the step results of its records.
 */
export default function RunsPage() {
  const companyId = useCompanyId();
  const [open, setOpen] = useState<number>();
  const runs = useQuery({
    queryKey: ['submitted', 'runs', companyId],
    queryFn: () => submittedApi.runs(companyId),
    enabled: companyId > 0,
  });
  const rows = runs.data?.content ?? [];
  const selected = rows.find((r) => r.id === open);
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Processing Runs"
        description="Sanitation, matching, classification, disposition and limits of each run."
      />
      <ErrorAlert error={runs.error} onRetry={() => void runs.refetch()} />
      <Card flush>
        <DataTable<RunView>
          loading={runs.isLoading}
          rows={rows}
          rowKey={(r) => r.id}
          selectedKey={open}
          onRowClick={(r) => setOpen(r.id)}
          emptyMessage="No processing run yet"
          columns={[
            { key: 'no', header: 'Run', kind: 'code', render: (r) => r.runNo },
            {
              key: 'trigger',
              header: 'Started By',
              render: (r) =>
                r.trigger === 'MANUAL' ? <UserName login={r.startedBy} /> : humanize(r.trigger),
            },
            { key: 'scope', header: 'Scope', render: (r) => r.scope },
            { key: 'total', header: 'Records', kind: 'amount', render: (r) => r.total },
            { key: 'bucketed', header: 'Bucketed', kind: 'amount', render: (r) => r.bucketed },
            { key: 'fallout', header: 'Fallout', kind: 'amount', render: (r) => r.fallout },
            {
              key: 'overridden',
              header: 'Overridden',
              kind: 'amount',
              render: (r) => r.overridden,
            },
            {
              key: 'breaches',
              header: 'Limit Breaches',
              kind: 'amount',
              render: (r) => r.breaches,
            },
            {
              key: 'at',
              header: 'Started',
              kind: 'datetime',
              render: (r) => formatDateTime(r.startedAt),
            },
          ]}
        />
      </Card>
      {selected && <Results run={selected} />}
    </div>
  );
}
