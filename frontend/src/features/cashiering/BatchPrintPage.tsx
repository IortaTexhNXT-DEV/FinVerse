import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Printer, RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { PrintBatch, ReceiptKind, ReceiptSummary } from './cashieringApi';
import './cashiering.css';

const keyOf = (r: ReceiptSummary) => String(r.id);

interface Criteria {
  kind: ReceiptKind | '';
  insurer: string;
  from: string;
  to: string;
}

function describe(c: Criteria): string {
  return [
    c.kind || 'All receipts',
    c.insurer && `insurer ${c.insurer}`,
    c.from && `from ${c.from}`,
    c.to && `to ${c.to}`,
  ]
    .filter(Boolean)
    .join(', ');
}

function Progress({ b }: Readonly<{ b: PrintBatch }>) {
  const pct = b.requestedCount === 0 ? 0 : Math.round((b.printedCount / b.requestedCount) * 100);
  return (
    <span
      className={b.failedCount > 0 ? 'csh-gauge csh-gauge-low' : 'csh-gauge'}
      title={`${b.printedCount} of ${b.requestedCount} printed`}
    >
      <span className="csh-gauge-fill" style={{ width: `${pct}%`, display: 'block' }} />
    </span>
  );
}

function Batches({ companyId }: Readonly<{ companyId: number }>) {
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'print-batches', companyId, page],
    queryFn: () => cashieringApi.printBatches(companyId, page),
    enabled: companyId > 0,
  });
  const retry = useMutation({
    mutationFn: cashieringApi.retryPrint,
    onSuccess: () =>
      void queryClient.invalidateQueries({ queryKey: ['cashiering', 'print-batches'] }),
  });
  const columns: Column<PrintBatch>[] = [
    { key: 'no', header: 'Batch No.', render: (b) => <strong>{b.batchNo}</strong> },
    { key: 'criteria', header: 'Criteria', render: (b) => b.criteria ?? '' },
    { key: 'progress', header: 'Progress', render: (b) => <Progress b={b} /> },
    {
      key: 'count',
      header: 'Printed / Failed',
      render: (b) => `${b.printedCount} / ${b.failedCount} of ${b.requestedCount}`,
    },
    {
      key: 'by',
      header: 'Created',
      render: (b) => `${b.createdBy} · ${formatDateTime(b.createdAt)}`,
    },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'actions',
      header: '',
      render: (b) => (
        <div className="row">
          {b.fileName && (
            <Button
              size="sm"
              variant="secondary"
              icon={<Download size={14} />}
              onClick={() => download.mutate(() => cashieringApi.printFile(b.id))}
            >
              Download
            </Button>
          )}
          {b.failedCount > 0 && (
            <Button
              size="sm"
              variant="ghost"
              icon={<RotateCcw size={14} />}
              onClick={() => retry.mutate(b.id)}
            >
              Retry Failures
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <Card title="Print Batches" flush>
      <ErrorAlert error={list.error ?? retry.error ?? download.error} />
      <DataTable
        caption="Print batches"
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(b) => b.id}
        loading={list.isLoading}
        emptyMessage="No print batch yet"
      />
      <PageFooter data={list.data} noun="batches" onPage={setPage} />
    </Card>
  );
}

/**
 * Batch Print (CSHID.019): pick receipts by kind, insurer and date, preview the list, print the
 * selected receipts into one PDF and retry the ones that failed.
 */
export default function BatchPrintPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [criteria, setCriteria] = useState<Criteria>({ kind: '', insurer: '', from: '', to: '' });
  const [applied, setApplied] = useState<Criteria>(criteria);
  const [page, setPage] = useState(0);
  const preview = useQuery({
    queryKey: ['cashiering', 'print-preview', companyId, applied, page],
    queryFn: () =>
      cashieringApi.receipts(
        {
          companyId,
          status: 'ISSUED',
          kind: applied.kind,
          insurer: applied.insurer,
          from: applied.from,
          to: applied.to,
        },
        page,
      ),
    enabled: companyId > 0,
  });
  const print = useMutation({
    mutationFn: () => cashieringApi.print(companyId, selection.keys.map(Number), describe(applied)),
    onSuccess: async (b) => {
      selection.clear();
      toast.success(`${b.batchNo}: ${b.printedCount} printed, ${b.failedCount} failed`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const rows = preview.data?.content ?? [];
  const columns: Column<ReceiptSummary>[] = [
    selectionColumn(rows, keyOf, selection, (r) => r.receiptNo),
    { key: 'no', header: 'Receipt No.', render: (r) => <strong>{r.receiptNo}</strong> },
    { key: 'date', header: 'Date', render: (r) => formatDate(r.receiptDate) },
    { key: 'payor', header: 'Payor', render: (r) => r.payorName },
    { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.amount} /> },
    {
      key: 'printed',
      header: 'Printed',
      render: (r) => (r.printedCount > 0 ? `${r.printedCount}x` : 'Not yet'),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Batch Print"
        description="Print receipts in one batch and retry the ones that failed."
      />
      <ErrorAlert error={preview.error ?? print.error} />
      <Card title="Receipts to Print" flush>
        <div className="worklist-filters csh-filters">
          <CodeSelect
            label="Kind"
            value={criteria.kind}
            options={['AR', 'OR']}
            empty="All"
            labelOf={(c) => c}
            onChange={(v) => setCriteria({ ...criteria, kind: v as ReceiptKind | '' })}
          />
          <TextField
            label="Insurer Code"
            value={criteria.insurer}
            onChange={(insurer) => setCriteria({ ...criteria, insurer })}
          />
          <TextField
            label="Date From"
            type="date"
            value={criteria.from}
            onChange={(from) => setCriteria({ ...criteria, from })}
          />
          <TextField
            label="Date To"
            type="date"
            value={criteria.to}
            onChange={(to) => setCriteria({ ...criteria, to })}
          />
        </div>
        <div className="worklist-toolbar">
          <Button
            variant="secondary"
            onClick={() => {
              setApplied(criteria);
              setPage(0);
              selection.clear();
            }}
          >
            Preview List
          </Button>
          <div className="worklist-actions">
            <Button
              variant="accent"
              icon={<Printer size={16} />}
              disabled={selection.keys.length === 0}
              busy={print.isPending}
              onClick={() => print.mutate()}
            >
              Print Selected ({selection.keys.length})
            </Button>
          </div>
        </div>
        <DataTable
          caption="Receipts to print"
          columns={columns}
          rows={rows}
          rowKey={(r) => r.id}
          loading={preview.isLoading}
          emptyMessage="No receipts match the criteria"
        />
        <PageFooter data={preview.data} noun="receipts" onPage={setPage} />
      </Card>
      <Batches companyId={companyId} />
    </div>
  );
}
