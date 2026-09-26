import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { BillingStage, DpBilling } from './commissionApi';
import { STAGE_TABS } from './commissionLogic';

/** Columns of a billing list. */
const COLUMNS: Column<DpBilling>[] = [
  { key: 'no', header: 'Billing No.', render: (b) => <strong>{b.billingNo}</strong> },
  { key: 'insurer', header: 'Insurer', render: (b) => b.insurerCode },
  { key: 'items', header: 'Accounts', numeric: true, render: (b) => b.itemCount },
  {
    key: 'net',
    header: 'Net Commission',
    numeric: true,
    render: (b) => <Amount value={b.amounts.net} />,
  },
  { key: 'sent', header: 'Sent', render: (b) => formatDateTime(b.sentAt) },
  {
    key: 'sla',
    header: 'Answer Due',
    render: (b) => (
      <>
        {formatDate(b.slaDue)} {b.overdue && <StatusBadge status="OVERDUE" />}
      </>
    ),
  },
  { key: 'handler', header: 'Handler', render: (b) => b.handler ?? '' },
  { key: 'stage', header: 'Stage', render: (b) => <StatusBadge status={b.stage} /> },
];

/**
 * Direct payment billings (CMRID.008-011): commission billed to each insurer, waiting for its
 * answer within the feedback days, then collected and closed.
 */
export default function DpBillingsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [stage, setStage] = useState<BillingStage>('AWAITING_INSURER');
  const [insurer, setInsurer] = useState('');
  const [page, setPage] = useState(0);
  const billings = useQuery({
    queryKey: ['commission', 'billings', companyId, stage, insurer, page],
    queryFn: () =>
      commissionApi.billings(companyId, { stage, insurer: insurer || undefined }, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="DP Billings"
        description="Commission billed to the insurers on direct payment accounts, from sending to collection."
      />
      <ErrorAlert error={billings.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={STAGE_TABS}
            active={stage}
            onChange={(s) => {
              setStage(s);
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
            caption="DP billings"
            columns={COLUMNS}
            rows={billings.data?.content ?? []}
            rowKey={(b) => b.id}
            loading={billings.isLoading}
            onRowClick={(b) => void navigate(`/commission/dp/billings/${String(b.id)}`)}
            emptyMessage="No items to display"
          />
          <PageFooter data={billings.data} noun="billings" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
