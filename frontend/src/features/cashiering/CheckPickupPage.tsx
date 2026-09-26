import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Printer } from 'lucide-react';
import { useState } from 'react';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { Pickup, PickupStatus, PrintBatch } from './cashieringApi';
import './cashiering.css';

const TABS: readonly { id: PickupStatus; label: string }[] = [
  { id: 'FOR_PICKUP', label: 'For Pick-up' },
  { id: 'AR_PRINTED', label: 'AR Printed' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

const keyOf = (p: Pickup) => String(p.id);

/**
 * Check Pick-up queue (CSHID.009): checks the collectors pick up from clients, filtered by pick-up
 * date. The cashier prints the ARs of the selected checks in one batch.
 */
export default function CheckPickupPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const download = useFileDownload();
  const [status, setStatus] = useState<PickupStatus>('FOR_PICKUP');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [batch, setBatch] = useState<PrintBatch>();
  const list = useQuery({
    queryKey: ['cashiering', 'pickups', companyId, status, from, to, page],
    queryFn: () =>
      cashieringApi.pickups(companyId, status, from || undefined, to || undefined, page),
    enabled: companyId > 0,
  });
  const done = async (message: string) => {
    toast.success(message);
    selection.clear();
    await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  };
  const print = useMutation({
    mutationFn: () => cashieringApi.printPickups(companyId, selection.keys.map(Number)),
    onSuccess: async (b) => {
      setBatch(b);
      await done(`${b.batchNo}: ${b.printedCount} AR(s) printed, ${b.failedCount} failed`);
    },
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: () => done('Check pick-up queue updated'),
  });
  const rows = list.data?.content ?? [];
  const receipt = can('CASH_RECEIPT');
  const columns: Column<Pickup>[] = [
    ...(status === 'FOR_PICKUP'
      ? [selectionColumn(rows, keyOf, selection, (p) => p.collectionRef)]
      : []),
    {
      key: 'ref',
      header: 'Collection Ref.',
      render: (p) => (
        <>
          <strong>{p.collectionRef}</strong>
          <span className="cell-sub">{p.reference}</span>
        </>
      ),
    },
    { key: 'payor', header: 'Payor', render: (p) => p.payorName },
    { key: 'date', header: 'Pick-up Date', render: (p) => formatDate(p.pickupDate) },
    {
      key: 'req',
      header: 'Requested By',
      render: (p) => `${p.requestor} · ${formatDateTime(p.requestedAt)}`,
    },
    { key: 'check', header: 'Check No.', render: (p) => p.checkNo ?? '' },
    { key: 'amount', header: 'Amount', numeric: true, render: (p) => <Amount value={p.amount} /> },
    { key: 'ar', header: 'AR', render: (p) => p.receiptNo ?? '' },
    { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
    {
      key: 'actions',
      header: '',
      render: (p) =>
        p.status === 'FOR_PICKUP' &&
        receipt && (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => act.mutate(() => cashieringApi.cancelPickup(p.id))}
          >
            Cancel
          </Button>
        ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Check Pick-up"
        description="Checks for collection from clients: print the ARs of the checks picked up."
        actions={
          receipt && (
            <Button
              variant="secondary"
              busy={act.isPending}
              onClick={() => act.mutate(() => cashieringApi.importPickups(companyId))}
            >
              Import Pending Requests
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? print.error ?? act.error ?? download.error} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={status}
          onChange={(s) => {
            setStatus(s);
            setPage(0);
            selection.clear();
          }}
        />
        <div className="worklist-toolbar">
          <TextField label="Pick-up From" type="date" value={from} onChange={setFrom} />
          <TextField label="Pick-up To" type="date" value={to} onChange={setTo} />
          <div className="worklist-actions">
            {status === 'FOR_PICKUP' && can('CASH_PRINT') && (
              <Button
                variant="accent"
                icon={<Printer size={16} />}
                disabled={selection.keys.length === 0}
                busy={print.isPending}
                onClick={() => print.mutate()}
              >
                Print ARs
              </Button>
            )}
            {batch?.fileName && (
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                onClick={() => download.mutate(() => cashieringApi.printFile(batch.id))}
              >
                Download {batch.batchNo}
              </Button>
            )}
          </div>
        </div>
        <DataTable
          caption="Check pick-up requests"
          columns={columns}
          rows={rows}
          rowKey={(p) => p.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
        />
        <PageFooter data={list.data} noun="checks" onPage={setPage} />
      </Card>
    </div>
  );
}
