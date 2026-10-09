import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Printer } from 'lucide-react';
import { useState } from 'react';
import type { DownloadedFile } from '@/api/client';
import { InsurerName } from '@/components/broking/LovLabel';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CodeSelect, InsurerField, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { PrintBatch, ReceiptSummary } from './cashieringApi';
import type { PrintCopy } from './cashieringQueueTypes';
import type { Filters, PrintTab } from './printLogic';
import { EMPTY_FILTERS, PRINT_TABS, criteriaOf } from './printLogic';
import { COPY_LABELS, printApi, selectionError } from './printApi';
import { PrintBatches } from './PrintBatches';
import { PrintPreview } from './PrintPreview';
import './cashiering.css';

const keyOf = (r: ReceiptSummary) => String(r.id);

function FilterBar({
  tab,
  f,
  onChange,
}: Readonly<{ tab: PrintTab; f: Filters; onChange: (f: Filters) => void }>) {
  const { branches } = useWorkspace();
  if (tab === 'queue') {
    return null;
  }
  const kind = (
    <CodeSelect
      label="Receipt Type"
      value={f.kind}
      options={['AR', 'OR']}
      labelOf={(c) => (c === 'AR' ? 'Acknowledgement Receipt' : 'Official Receipt')}
      onChange={(v) => onChange({ ...f, kind: v as Filters['kind'] })}
    />
  );
  if (tab === 'reprint') {
    return (
      <div className="worklist-filters csh-filters">
        {kind}
        <TextField
          label={f.kind === 'AR' ? 'AR Number' : 'OR Number'}
          value={f.number}
          onChange={(number) => onChange({ ...f, number })}
          placeholder="Full or partial number"
        />
      </div>
    );
  }
  return (
    <div className="worklist-filters csh-filters">
      {kind}
      {f.kind === 'AR' ? (
        <CodeSelect
          label="Receipting Branch"
          value={f.branchId}
          options={branches.map((b) => String(b.id))}
          empty="All branches"
          labelOf={(id) => branches.find((b) => String(b.id) === id)?.name ?? id}
          onChange={(branchId) => onChange({ ...f, branchId })}
        />
      ) : (
        <InsurerField value={f.insurer} onChange={(insurer) => onChange({ ...f, insurer })} />
      )}
      <TextField
        label="Issued From"
        type="date"
        value={f.from}
        onChange={(from) => onChange({ ...f, from })}
      />
      <TextField
        label="Issued To"
        type="date"
        value={f.to}
        onChange={(to) => onChange({ ...f, to })}
      />
    </div>
  );
}

/**
 * Batch Print (FRS.CSH.02.04): print ARs by receipting branch and issue date and ORs by insurer
 * and issue date, the print queue of the receipts the system generated (For Printing) printed in
 * one action, re-printing by full or partial number with the REPRINT mark, the Client's Copy, the
 * company's copy or both, at most 500 receipts per batch, the print preview, the merged PDF and the
 * ZIP of one PDF per receipt, and the print log with retry and skip.
 */
export default function BatchPrintPage() {
  const companyId = useCompanyId();
  const { branches } = useWorkspace();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [tab, setTab] = useState<PrintTab>('manual');
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS);
  const [copy, setCopy] = useState<PrintCopy>('CLIENT');
  const [page, setPage] = useState(0);
  const [preview, setPreview] = useState<{ batch: PrintBatch; file: DownloadedFile }>();
  const criteria = criteriaOf(tab, applied, companyId);
  const list = useQuery({
    queryKey: ['cashiering', 'print-list', criteria, page],
    queryFn: () => cashieringApi.receipts(criteria, page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const print = useMutation({
    mutationFn: async (ids: number[]) => {
      const batch = await printApi.print(
        companyId,
        ids,
        PRINT_TABS.find((t) => t.id === tab)?.label ?? '',
        copy,
      );
      return { batch, file: await cashieringApi.printFile(batch.id) };
    },
    onSuccess: async (done) => {
      selection.clear();
      toast.success(
        `${done.batch.batchNo}: ${done.batch.printedCount} printed, ${done.batch.failedCount} not printed`,
      );
      setPreview(done);
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const start = (ids: number[]) => {
    const refused = selectionError(ids.length);
    if (refused) {
      toast.error(refused);
    } else {
      print.mutate(ids);
    }
  };
  const columns: Column<ReceiptSummary>[] = [
    selectionColumn(rows, keyOf, selection, (r) => r.receiptNo),
    { key: 'no', header: 'AR or OR Number', render: (r) => <strong>{r.receiptNo}</strong> },
    {
      key: 'branch',
      header: 'Receipting Branch',
      render: (r) => branches.find((b) => b.id === r.branchId)?.name ?? '',
    },
    { key: 'date', header: 'Issued', render: (r) => formatDate(r.receiptDate) },
    {
      key: 'client',
      header: 'Client Name',
      render: (r) => (r.kind === 'AR' ? r.payorName : (r.assuredName ?? '')),
    },
    {
      key: 'insurer',
      header: 'Insurer Name',
      render: (r) => <InsurerName code={r.insurerCode} />,
    },
    {
      key: 'amount',
      header: 'AR or OR Amount',
      numeric: true,
      render: (r) => <Amount value={r.amount} />,
    },
    {
      key: 'printed',
      header: 'Printed',
      render: (r) => (r.printedCount > 0 ? `${r.printedCount} time(s)` : 'For Printing'),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Batch Print"
        description="Print and re-print ARs and ORs in their forms, at most 500 per batch."
      />
      <ErrorAlert error={list.error ?? print.error} />
      <Tabs
        tabs={PRINT_TABS}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setPage(0);
          selection.clear();
        }}
      />
      <Card title={PRINT_TABS.find((t) => t.id === tab)?.label} flush>
        <FilterBar tab={tab} f={filters} onChange={setFilters} />
        <div className="worklist-toolbar">
          {tab !== 'queue' && (
            <Button
              variant="secondary"
              onClick={() => {
                setApplied(filters);
                setPage(0);
                selection.clear();
              }}
            >
              Search
            </Button>
          )}
          <div className="worklist-actions">
            <CodeSelect
              label="Copy"
              value={copy}
              options={Object.keys(COPY_LABELS)}
              labelOf={(c) => COPY_LABELS[c as PrintCopy]}
              onChange={(c) => setCopy(c as PrintCopy)}
            />
            {tab === 'queue' && (
              <Button
                variant="secondary"
                icon={<Printer size={16} />}
                disabled={rows.length === 0}
                busy={print.isPending}
                onClick={() => start(rows.map((r) => r.id))}
              >
                Print All on This Page ({rows.length})
              </Button>
            )}
            <Button
              variant="accent"
              icon={<Printer size={16} />}
              disabled={selection.keys.length === 0}
              busy={print.isPending}
              onClick={() => start(selection.keys.map(Number))}
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
          loading={list.isLoading}
          emptyMessage={
            tab === 'queue' ? 'No receipt waiting for printing' : 'No receipt matches the criteria'
          }
        />
        <PageFooter data={list.data} noun="receipts" onPage={setPage} />
      </Card>
      <PrintBatches companyId={companyId} />
      {preview && (
        <PrintPreview
          file={preview.file}
          title={preview.batch.batchNo}
          onClose={() => setPreview(undefined)}
        />
      )}
    </div>
  );
}
