import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { LoanMatchView, ResultView, RunView } from '@/api/submitted';
import { LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { SBM_LOV, STEP_LABELS, SUBMITTED_SECTION, policyLink } from '../common/submittedCodes';

type ResultTab = 'ALL' | 'FALLOUT' | 'LOANS' | 'UNMATCHED';

const MATCH_KEYS: Record<string, string> = {
  PN: 'PN number',
  LOAN_APPLICATION: 'Loan application number',
};

/** Loan file results of a run: the loan file and key of each record, or why it is unmatched. */
function LoanMatches({ run, unmatched }: Readonly<{ run: RunView; unmatched: boolean }>) {
  const matches = useQuery({
    queryKey: ['submitted', 'loan-matches', run.id, unmatched],
    queryFn: () => submittedApi.loanMatches(run.id, unmatched ? 'UNMATCHED' : undefined),
  });
  return (
    <>
      <ErrorAlert error={matches.error} />
      <DataTable<LoanMatchView>
        loading={matches.isLoading}
        rows={matches.data?.content ?? []}
        rowKey={(m) => m.id}
        emptyMessage={unmatched ? 'Every record matched a loan file' : 'No loan file results'}
        columns={[
          {
            key: 'sbm',
            header: 'Masterlist No.',
            kind: 'code',
            render: (m) => <Link to={policyLink(m.policyId)}>{m.sbmNo ?? m.policyId}</Link>,
          },
          { key: 'assured', header: 'Assured', render: (m) => m.assuredName ?? '' },
          {
            key: 'outcome',
            header: 'Result',
            kind: 'status',
            render: (m) => <StatusBadge status={m.outcome} />,
          },
          {
            key: 'key',
            header: 'Matched On',
            render: (m) => (m.keyUsed ? MATCH_KEYS[m.keyUsed] : '—'),
          },
          {
            key: 'file',
            header: 'Loan File',
            render: (m) =>
              m.loanReport ? `${m.loanReport} of ${formatDate(m.fileDate)}` : (m.reason ?? '—'),
          },
          { key: 'upload', header: 'Upload', kind: 'code', render: (m) => m.uploadNo ?? '' },
          {
            key: 'at',
            header: 'Time',
            kind: 'datetime',
            render: (m) => formatDateTime(m.matchedAt),
          },
        ]}
      />
    </>
  );
}

function Results({ run }: Readonly<{ run: RunView }>) {
  const [outcome, setOutcome] = useState<ResultTab>('FALLOUT');
  const loans = outcome === 'LOANS' || outcome === 'UNMATCHED';
  const results = useQuery({
    queryKey: ['submitted', 'results', run.id, outcome],
    queryFn: () => submittedApi.results(run.id, outcome === 'ALL' ? undefined : outcome),
    enabled: !loans,
  });
  return (
    <Card title={`Results of ${run.runNo}`} flush>
      <Tabs
        tabs={[
          { id: 'FALLOUT', label: 'Fallout', count: run.fallout },
          { id: 'ALL', label: 'All Steps' },
          { id: 'LOANS', label: 'Loan File Results' },
          { id: 'UNMATCHED', label: 'Unmatched Loans' },
        ]}
        active={outcome}
        onChange={setOutcome}
      />
      {loans ? (
        <LoanMatches run={run} unmatched={outcome === 'UNMATCHED'} />
      ) : (
        <StepResults
          results={results.data?.content ?? []}
          loading={results.isLoading}
          error={results.error}
          fallout={outcome === 'FALLOUT'}
        />
      )}
    </Card>
  );
}

function StepResults({
  results,
  loading,
  error,
  fallout,
}: Readonly<{ results: ResultView[]; loading: boolean; error: unknown; fallout: boolean }>) {
  return (
    <>
      <ErrorAlert error={error} />
      <DataTable<ResultView>
        loading={loading}
        rows={results}
        rowKey={(r) => r.id}
        emptyMessage={fallout ? 'No fallout in this run' : 'No results'}
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
    </>
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
        description="Sanitation, matching with the loan files, classification, disposition and limits of each run."
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
