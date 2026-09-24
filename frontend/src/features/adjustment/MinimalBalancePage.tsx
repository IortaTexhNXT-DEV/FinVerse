import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { adjustmentApi } from './api';
import type { WriteOff } from './api';

const COLUMNS: Column<WriteOff>[] = [
  { key: 'file', header: 'File', render: (w) => w.fileRef },
  {
    key: 'invoice',
    header: 'Invoice No. / ARN',
    render: (w) => (
      <>
        <strong>{w.invoiceNo}</strong>
        <div className="muted">{w.arn}</div>
      </>
    ),
  },
  { key: 'client', header: 'Client', render: (w) => w.clientCode },
  {
    key: 'balance',
    header: 'Balance',
    numeric: true,
    render: (w) => <Amount value={w.balance} />,
  },
  {
    key: 'action',
    header: 'Action',
    render: (w) => (
      <span className={`badge ${w.action === 'WRITE_OFF' ? 'warning' : 'info'}`}>
        {w.action === 'WRITE_OFF' ? 'Written Off' : 'Credited'}
      </span>
    ),
  },
  { key: 'journal', header: 'Journal', render: (w) => w.journalBatchNo ?? '—' },
  {
    key: 'at',
    header: 'Processed',
    render: (w) => `${formatDateTime(w.createdAt)} · ${w.createdBy}`,
  },
];

/**
 * Minimal balance file (ADJID.026): upload the invoices with balances from 10.00 to 100.00; each
 * row is matched with the ledger balance and written off (debit) or credited (credit); the
 * write-offs done are listed below.
 */
export default function MinimalBalancePage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['adjustment', 'write-offs', companyId, page],
    queryFn: () => adjustmentApi.writeOffs(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        backTo="/adjustment"
        section="Client & Policy · Adjustment"
        title="Minimal Balance File"
        description="Write off or credit premium receivable balances of 10.00 to 100.00 from a file of invoices."
      />
      <BulkUploadWizard
        handler="MINIMAL_BALANCE_FILE"
        onCommitted={() =>
          void queryClient.invalidateQueries({ queryKey: ['adjustment', 'write-offs'] })
        }
      />
      <Card title="Balances Written Off or Credited" flush>
        <ErrorAlert error={list.error} />
        <DataTable
          caption="Minimal balance write-offs"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(w) => w.invoiceNo}
          loading={list.isLoading}
          emptyMessage="No items to display"
        />
        <PageFooter data={list.data} noun="balances" onPage={setPage} />
      </Card>
    </div>
  );
}
