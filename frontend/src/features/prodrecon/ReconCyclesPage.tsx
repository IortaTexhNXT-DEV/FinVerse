import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { prodreconApi } from './prodreconApi';
import type { ReconCycle } from './prodreconApi';
import { matchedPercent, monthLabel } from './prodreconLogic';

type StageTab = 'ALL' | 'EXTRACTED' | 'SENT_TO_INSURER' | 'RECONCILING' | 'CLOSED';

const TABS: readonly { id: StageTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'EXTRACTED', label: 'Extracted' },
  { id: 'SENT_TO_INSURER', label: 'Sent to Insurer' },
  { id: 'RECONCILING', label: 'Reconciling' },
  { id: 'CLOSED', label: 'Closed' },
];

const COLUMNS: Column<ReconCycle>[] = [
  { key: 'no', header: 'Cycle No.', render: (c) => <strong>{c.cycleNo}</strong> },
  { key: 'insurer', header: 'Insurer', render: (c) => c.insurerCode },
  { key: 'month', header: 'Production Month', render: (c) => monthLabel(c.productionMonth) },
  { key: 'total', header: 'Items', numeric: true, render: (c) => c.counts.total },
  { key: 'bdoi', header: 'BDOI Only', numeric: true, render: (c) => c.counts.bdoiOnly },
  { key: 'ins', header: 'Insurer Only', numeric: true, render: (c) => c.counts.insurerOnly },
  {
    key: 'pct',
    header: 'Matched',
    numeric: true,
    render: (c) => `${String(matchedPercent(c.counts))}%`,
  },
  { key: 'sent', header: 'Sent', render: (c) => formatDateTime(c.sentAt) },
  { key: 'upload', header: 'Last Feedback', render: (c) => formatDateTime(c.lastUploadAt) },
  { key: 'stage', header: 'Stage', render: (c) => <StatusBadge status={c.stage} /> },
];

function initialTab(stage: string | null): StageTab {
  return TABS.find((t) => t.id === stage)?.id ?? 'RECONCILING';
}

/**
 * Reconciliation cycles (PRCID.009-033): one per insurer and production month, from the register
 * sent to the insurer to its closure, with the matched share and the open buckets.
 */
export default function ReconCyclesPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<StageTab>(() => initialTab(params.get('stage')));
  const [insurer, setInsurer] = useState('');
  const [page, setPage] = useState(0);
  const cycles = useQuery({
    queryKey: ['prodrecon', 'cycles', companyId, tab, insurer, page],
    queryFn: () =>
      prodreconApi.cycles(
        companyId,
        { insurer: insurer || undefined, stage: tab === 'ALL' ? undefined : tab },
        page,
      ),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Production Reconciliation"
        title="Reconciliation Cycles"
        description="Production registers sent to each insurer and the matching of their feedback, month by month."
      />
      <ErrorAlert error={cycles.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Insurer Code"
            onSearch={(text) => {
              setInsurer(text.trim().toUpperCase());
              setPage(0);
            }}
          />
          <DataTable
            caption="Reconciliation cycles"
            columns={COLUMNS}
            rows={cycles.data?.content ?? []}
            rowKey={(c) => c.id}
            loading={cycles.isLoading}
            onRowClick={(c) => void navigate(`/prodrecon/cycles/${String(c.id)}`)}
            emptyMessage="No items to display"
          />
          <PageFooter data={cycles.data} noun="cycles" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
