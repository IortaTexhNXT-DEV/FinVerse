import { useQuery } from '@tanstack/react-query';
import { FilePlus2, Layers, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { adjustmentApi } from './api';
import type { RequestSummary, StageCounts } from './api';
import { RequestFlags, RequestStatus } from './RequestParts';
import { STAGE_TABS, tabOf } from './requestForm';
import type { StageTab } from './requestForm';

const COLUMNS: Column<RequestSummary>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (r) => (
      <>
        <strong>{r.requestNo}</strong>
        <div className="muted">{formatDate(r.createdAt)}</div>
      </>
    ),
  },
  {
    key: 'invoice',
    header: 'Invoice / ARN',
    render: (r) => (
      <>
        {r.invoiceNo}
        <div className="muted">{r.arn}</div>
      </>
    ),
  },
  { key: 'assured', header: 'Assured', render: (r) => r.assuredName },
  {
    key: 'type',
    header: 'Type',
    render: (r) => (
      <>
        {humanize(r.endorsementType)}
        {r.requestType && <div className="muted">{humanize(r.requestType)}</div>}
      </>
    ),
  },
  { key: 'effective', header: 'Effective', render: (r) => formatDate(r.effectiveDate) },
  { key: 'aging', header: 'Aging', numeric: true, render: (r) => `${String(r.agingDays)} d` },
  { key: 'status', header: 'Status', render: (r) => <RequestStatus stage={r.stage} /> },
  {
    key: 'flags',
    header: 'Flags',
    render: (r) => (
      <RequestFlags
        negative={r.negative}
        quotationRequired={r.quotationRequired}
        duplicateOverride={r.duplicateOverride}
      />
    ),
  },
];

function tabsWithCounts(counts: StageCounts | undefined) {
  return STAGE_TABS.map((t) => {
    const count = t.id === 'ALL' ? undefined : counts?.[t.id];
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

/**
 * Adjustment Workbench (ADJID.001-025): endorsement and cancellation requests by stage with their
 * aging, searchable by request, invoice, ARN, policy, assured or endorsement reference.
 */
export default function AdjustmentHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = tabOf(params.get('stage'));
  const [text, setText] = useState('');
  const [page, setPage] = useState(0);
  const stage = tab === 'ALL' ? undefined : tab;
  const rows = useQuery({
    queryKey: ['adjustment', 'requests', companyId, stage, text, page],
    queryFn: () => adjustmentApi.requests(companyId, stage, text, page),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['adjustment', 'counts', companyId],
    queryFn: () => adjustmentApi.counts(companyId),
    enabled: companyId > 0,
  });
  const changeTab = (next: StageTab) => {
    setPage(0);
    setParams(next === 'ALL' ? {} : { stage: next });
  };
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Adjustment"
        title="Adjustment Workbench"
        description="Endorsement and cancellation requests on booked invoices, from request to posting."
        actions={
          <>
            {can('ADJ_POST') && (
              <Link className="btn btn-secondary" to="/adjustment/batches">
                <Layers size={16} aria-hidden="true" /> Posting Batches
              </Link>
            )}
            {can('ADJ_POST') && (
              <Link className="btn btn-secondary" to="/adjustment/upload">
                <Upload size={16} aria-hidden="true" /> Batch Upload
              </Link>
            )}
            {(can('ADJ_REQUEST') || can('ADJ_PROCESS')) && (
              <Link className="btn btn-primary" to="/adjustment/new">
                <FilePlus2 size={16} aria-hidden="true" /> New Request
              </Link>
            )}
          </>
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<StageTab> tabs={tabsWithCounts(counts.data)} active={tab} onChange={changeTab} />
        </div>
        <WorklistToolbar
          placeholder="Search Request No."
          onSearch={(q) => {
            setText(q);
            setPage(0);
          }}
        />
        <DataTable
          caption="Endorsement requests"
          columns={COLUMNS}
          rows={rows.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={rows.isLoading}
          emptyMessage="No items to display"
          onRowClick={(r) => void navigate(`/adjustment/requests/${String(r.id)}`)}
        />
        <PageFooter data={rows.data} noun="requests" onPage={setPage} />
      </Card>
    </div>
  );
}
