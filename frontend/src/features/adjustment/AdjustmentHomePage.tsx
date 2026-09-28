import { useQuery } from '@tanstack/react-query';
import { FilePlus2, Layers, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { InsurerName, ProductName } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDays } from '@/utils/format';
import { adjustmentApi } from './api';
import type { RequestSummary, StageCounts } from './api';
import { RequestFlags, RequestStatus, RequestTypeCell } from './RequestParts';
import { STAGE_TABS, tabOf } from './requestForm';
import type { StageTab } from './requestForm';

const COLUMNS: Column<RequestSummary>[] = [
  {
    key: 'no',
    header: 'Request No.',
    kind: 'code',
    render: (r) => (
      <CellStack main={<strong>{r.requestNo}</strong>} sub={formatDate(r.createdAt)} />
    ),
  },
  {
    key: 'policy',
    header: 'Policy No. / ARN',
    kind: 'code',
    render: (r) => <CellStack main={r.policy?.policyNo} sub={r.arn} />,
  },
  {
    key: 'invoice',
    header: 'Invoice / Placement Slip',
    kind: 'code',
    render: (r) => <CellStack main={r.invoiceNo} sub={r.policy?.slipNo} />,
  },
  {
    key: 'assured',
    header: 'Assured',
    render: (r) => <CellStack main={r.assuredName} sub={r.policy?.clientCode} />,
  },
  {
    key: 'insurer',
    header: 'Insurer / Product',
    render: (r) => (
      <CellStack
        main={<InsurerName code={r.insurerCode} />}
        sub={r.policy?.productCode ? <ProductName code={r.policy.productCode} /> : undefined}
      />
    ),
  },
  {
    key: 'type',
    header: 'Type',
    render: (r) => (
      <RequestTypeCell endorsementType={r.endorsementType} requestType={r.requestType} />
    ),
  },
  {
    key: 'effective',
    header: 'Effective / Aging',
    kind: 'date',
    render: (r) => <CellStack main={formatDate(r.effectiveDate)} sub={formatDays(r.agingDays)} />,
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (r) => (
      <CellStack
        main={<RequestStatus stage={r.stage} />}
        sub={
          <RequestFlags
            negative={r.negative}
            quotationRequired={r.quotationRequired}
            duplicateOverride={r.duplicateOverride}
          />
        }
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
 * aging, each with the policy it is against (insurer policy number, ARN, invoice, placement slip,
 * insurer and product), searchable by request, policy, ARN, invoice, client, insurer, product,
 * placement slip or endorsement reference.
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
          placeholder="Search request, policy, ARN, invoice or client"
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
