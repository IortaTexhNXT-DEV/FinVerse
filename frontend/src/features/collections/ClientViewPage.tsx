import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDays } from '@/utils/format';
import { collectionsApi } from './api';
import { dispositionLabel } from './presentation';
import { UserName } from '@/components/ui/UserName';
import './collections.css';
import { CellStack } from '@/components/ui/CellStack';

/**
 * Client view (BRCLXN.003): every collection account of a client - open, completed, credit - with
 * the total outstanding and PR2307 of the open accounts.
 */
export default function ClientViewPage() {
  const clientCode = decodeURIComponent(useParams().clientCode ?? '');
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const view = useQuery({
    queryKey: ['collections', 'client', companyId, clientCode],
    queryFn: () => collectionsApi.client(companyId, clientCode),
    enabled: companyId > 0,
  });
  const rules = useQuery({
    queryKey: ['collections', 'disposition-rules'],
    queryFn: collectionsApi.dispositionRules,
    staleTime: 5 * 60_000,
  });
  const v = view.data;
  const open = v?.items.filter((i) => i.status === 'OPEN').length ?? 0;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · PR Worklist"
        backTo="/collections/worklist"
        title={v?.name ?? clientCode}
        description={`Client ${clientCode}: every collection account with the totals of the open ones.`}
      />
      <ErrorAlert error={view.error} />
      <div className="clx-totals">
        <div className="clx-total">
          <span className="clx-total-label">Open Accounts</span>
          <span className="clx-total-value">{open}</span>
        </div>
        <div className="clx-total clx-total-strong">
          <span className="clx-total-label">Net Outstanding</span>
          <span className="clx-total-value">{formatAmount(v?.openOutstanding ?? 0)}</span>
        </div>
        <div className="clx-total">
          <span className="clx-total-label">PR2307 Open</span>
          <span className="clx-total-value">{formatAmount(v?.openPr2307 ?? 0)}</span>
        </div>
      </div>
      <Card flush>
        <DataTable
          caption="Client accounts"
          columns={[
            { key: 'i', header: 'Invoice No.', render: (i) => <strong>{i.invoiceNo}</strong> },
            {
              key: 'a',
              header: 'ARN / Policy',
              render: (i) => <CellStack main={i.arn} sub={i.policyNo} />,
            },
            { key: 'b', header: 'Booked', render: (i) => formatDate(i.bookingDate) },
            { key: 'g', header: 'Aging', render: (i) => formatDays(i.agingDays) },
            {
              key: 'n',
              header: 'Outstanding',
              numeric: true,
              render: (i) => `${i.currency} ${formatAmount(i.netOutstanding)}`,
            },
            {
              key: 'h',
              header: 'Handler',
              render: (i) => <UserName login={i.currentHandler} empty="Unassigned" />,
            },
            {
              key: 'd',
              header: 'Disposition',
              render: (i) => dispositionLabel(rules.data, i.dispositionCode),
            },
            { key: 's', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
          ]}
          rows={v?.items ?? []}
          rowKey={(i) => i.id}
          loading={view.isLoading}
          emptyMessage="The client has no collection account"
          onRowClick={(i) => void navigate(`/collections/items/${encodeURIComponent(i.invoiceNo)}`)}
        />
      </Card>
    </div>
  );
}
