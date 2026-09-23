import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { reservesApi } from '@/api/reserves';
import type { ValuationRun, ValuationRunDetail } from '@/api/reserves';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, formatDateTime } from '@/utils/format';
import { PolicyUprTable } from './PolicyUprTable';
import { linesOfBusiness } from './reserveMath';
import { RunActionBar } from './RunActionBar';
import {
  LineSummaryTable,
  LinesTable,
  MovementsTable,
  TakafulTable,
  TotalsTable,
} from './RunTables';

const TABS = [
  { id: 'summary', label: 'By line of business' },
  { id: 'lines', label: 'All lines' },
  { id: 'movements', label: 'Journal movements' },
  { id: 'upr', label: 'Policy UPR' },
  { id: 'takaful', label: 'Takaful surplus' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function trail(run: ValuationRun): string {
  const parts = [`Calculated ${formatDateTime(run.calculatedAt)} by ${run.preparedBy}`];
  if (run.submittedBy) {
    parts.push(`submitted by ${run.submittedBy}`);
  }
  if (run.approvedBy) {
    parts.push(`approved by ${run.approvedBy}`);
  }
  if (run.postedBy) {
    parts.push(`posted by ${run.postedBy} (${run.journalCount} journals)`);
  }
  if (run.cancelledBy) {
    parts.push(`cancelled by ${run.cancelledBy}: ${run.cancelReason ?? ''}`);
  }
  return parts.join(' · ');
}

function headline(run: ValuationRun | undefined): string | undefined {
  return run && `Valuation date ${formatDate(run.valuationDate)} · amounts in ${run.baseCurrency}`;
}

/** Status, audit trail, rejection reason and calculation remarks of a run. */
function RunStatusCard({ run }: Readonly<{ run: ValuationRun }>) {
  return (
    <Card>
      <div className="row" style={{ gap: 'var(--space-3)', flexWrap: 'wrap' }}>
        <StatusBadge status={run.status} />
        <span className="muted">{trail(run)}</span>
      </div>
      {run.rejectionReason && <p className="alert warning">Rejected: {run.rejectionReason}</p>}
      {run.remarks && <p className="muted">{run.remarks}</p>}
    </Card>
  );
}

function TakafulTab({ runId }: Readonly<{ runId: number }>) {
  const takaful = useQuery({
    queryKey: ['reserve-takaful', runId],
    queryFn: () => reservesApi.takafulLines(runId),
  });
  return <TakafulTable items={takaful.data ?? []} loading={takaful.isLoading} />;
}

/** Content of the selected tab. */
function TabBody({
  tab,
  runId,
  detail,
}: Readonly<{ tab: TabId; runId: number; detail: ValuationRunDetail | undefined }>) {
  const lines = detail?.lines ?? [];
  switch (tab) {
    case 'lines':
      return <LinesTable lines={lines} />;
    case 'movements':
      return <MovementsTable movements={detail?.movements ?? []} />;
    case 'upr':
      return <PolicyUprTable runId={runId} lines={linesOfBusiness(lines)} />;
    case 'takaful':
      return <TakafulTab runId={runId} />;
    default:
      return <LineSummaryTable lines={lines} />;
  }
}

/** One valuation run: totals, lines, journal movements, policy UPR and life-cycle actions. */
export default function ValuationRunPage() {
  const id = Number(useParams().id);
  const [tab, setTab] = useState<TabId>('summary');
  const detail = useQuery({
    queryKey: ['reserve-run', id],
    queryFn: () => reservesApi.run(id),
    enabled: id > 0,
  });
  const d = detail.data;

  return (
    <div className="stack">
      <PageHeader
        section="Actuarial Reserves"
        title={`Valuation Run ${d?.run.periodName ?? ''}`}
        description={headline(d?.run)}
        actions={d && <RunActionBar run={d.run} />}
      />
      <ErrorAlert error={detail.error} />
      {d && <RunStatusCard run={d.run} />}
      <Card title="Totals by reserve" flush>
        <TotalsTable totals={d?.totals ?? []} />
      </Card>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Card flush>
        <TabBody tab={tab} runId={id} detail={d} />
      </Card>
    </div>
  );
}
