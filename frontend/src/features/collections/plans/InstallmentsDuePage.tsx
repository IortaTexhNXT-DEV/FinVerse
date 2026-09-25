import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import { formatDate } from '@/utils/format';
import type { DueInstallment } from './api';
import { plansApi } from './api';

type DueTab = 'OVERDUE' | 'DUE';

const TABS: readonly { id: DueTab; label: string }[] = [
  { id: 'OVERDUE', label: 'Overdue' },
  { id: 'DUE', label: 'Due to Date' },
];

const COLUMNS: Column<DueInstallment>[] = [
  {
    key: 'plan',
    header: 'Plan No.',
    render: (d) => (
      <>
        <strong>{d.planNo}</strong>
        <div className="muted">{d.arn}</div>
      </>
    ),
  },
  { key: 'assured', header: 'Name of Assured', render: (d) => d.assuredName },
  { key: 'seq', header: 'Cycle', render: (d) => d.installment.seq },
  { key: 'inv', header: 'Invoice No.', render: (d) => d.installment.invoiceNo ?? 'Scheduled' },
  { key: 'due', header: 'Due Date', render: (d) => formatDate(d.installment.dueDate) },
  {
    key: 'since',
    header: 'Overdue Since',
    render: (d) => formatDate(d.installment.overdueSince),
  },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (d) => <Amount value={d.installment.amount} />,
  },
  {
    key: 'bal',
    header: 'Balance',
    numeric: true,
    render: (d) => <Amount value={d.installment.balance} />,
  },
  { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.installment.status} /> },
];

/**
 * Installments due and overdue (BRCLXN.053): the unpaid installments of the live plans up to
 * today, oldest first, for follow-up and escalation.
 */
export default function InstallmentsDuePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [tab, setTab] = useState<DueTab>('OVERDUE');
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['collections', 'installments-due', companyId, tab, page],
    queryFn: () => plansApi.due(companyId, tab === 'OVERDUE', page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Installments Due"
        description="Unpaid installments of the live plans falling due up to today, oldest first. Payments are allocated nightly from the invoice ledger."
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <DataTable
            caption="Installments due"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(d) => d.installment.id}
            loading={rows.isLoading}
            emptyMessage="No installments due"
            onRowClick={(d) => void navigate(`/collections/plans/${d.planId}`)}
          />
          <PageFooter data={rows.data} noun="installments" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
