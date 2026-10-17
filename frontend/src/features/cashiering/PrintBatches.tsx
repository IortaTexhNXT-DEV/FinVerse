import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { DownloadedFile } from '@/api/client';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { PrintBatch, PrintLine } from './cashieringQueueTypes';
import { COPY_LABELS, LINE_STATUS_LABELS, failedLines, printApi } from './printApi';
import { PrintPreview } from './PrintPreview';

const LINE_COLUMNS: Column<PrintLine>[] = [
  { key: 'no', header: 'Receipt Number', render: (l) => <strong>{l.receiptNo}</strong> },
  { key: 'certificate', header: 'Certificate No.', render: (l) => l.certificateNo ?? '' },
  { key: 'reprint', header: 'Reprint', render: (l) => (l.reprint ? 'Yes' : 'No') },
  {
    key: 'status',
    header: 'Status',
    render: (l) => <StatusBadge status={l.status} label={LINE_STATUS_LABELS[l.status]} />,
  },
  { key: 'message', header: 'Reason', truncate: true, render: (l) => l.message ?? '' },
];

function BatchLines({ batchId }: Readonly<{ batchId: number }>) {
  const batch = useQuery({
    queryKey: ['cashiering', 'print-batch', batchId],
    queryFn: () => cashieringApi.printBatch(batchId),
  });
  return (
    <Card title={`Print Log ${batch.data?.batchNo ?? ''}`} flush>
      <ErrorAlert error={batch.error} />
      <DataTable
        caption="Receipts of the print batch"
        columns={LINE_COLUMNS}
        rows={batch.data?.lines ?? []}
        rowKey={(l) => l.receiptId}
        loading={batch.isLoading}
        emptyMessage="No receipt in this batch"
      />
    </Card>
  );
}

/**
 * The print batches with their print log (FRS.CSH.02.04.13 to 02.04.16): the receipts printed,
 * re-printed, not printed with the reason, retried or skipped, and the merged PDF and the ZIP of
 * each batch.
 */
export function PrintBatches({ companyId }: Readonly<{ companyId: number }>) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const download = useFileDownload();
  const [page, setPage] = useState(0);
  const [shown, setShown] = useState<number>();
  const [preview, setPreview] = useState<{ batch: PrintBatch; file: DownloadedFile }>();
  const list = useQuery({
    queryKey: ['cashiering', 'print-batches', companyId, page],
    queryFn: () => cashieringApi.printBatches(companyId, page),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  const act = useMutation({
    mutationFn: (fn: () => Promise<PrintBatch>) => fn(),
    onSuccess: async (b) => {
      toast.success(`${b.batchNo}: ${b.printedCount} printed, ${b.skippedCount} skipped`);
      setShown(b.id);
      await refresh();
    },
  });
  const open = useMutation({
    mutationFn: async (b: PrintBatch) => ({ batch: b, file: await cashieringApi.printFile(b.id) }),
    onSuccess: setPreview,
  });
  const columns: Column<PrintBatch>[] = [
    { key: 'no', header: 'Batch No.', render: (b) => <strong>{b.batchNo}</strong> },
    { key: 'criteria', header: 'Criteria', truncate: true, render: (b) => b.criteria ?? '' },
    { key: 'copy', header: 'Copy', render: (b) => COPY_LABELS[b.copyLabel] },
    {
      key: 'count',
      header: 'Printed / Not Printed / Skipped',
      render: (b) => `${b.printedCount} / ${b.failedCount} / ${b.skippedCount}`,
    },
    {
      key: 'by',
      header: 'Printed By',
      render: (b) => (
        <CellStack main={<UserName login={b.createdBy} />} sub={formatDateTime(b.createdAt)} />
      ),
    },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (b) => (
        <RowActionMenu
          label={b.batchNo}
          actions={[
            { label: 'Print Log', onSelect: () => setShown(b.id) },
            { label: 'Print Preview', onSelect: () => open.mutate(b), disabled: !b.fileName },
            {
              label: 'Download PDF',
              onSelect: () => download.mutate(() => cashieringApi.printFile(b.id)),
              disabled: !b.fileName,
            },
            {
              label: 'Download ZIP',
              onSelect: () => download.mutate(() => printApi.zip(b.id)),
              disabled: !b.zipped,
            },
            {
              label: 'Retry Failures',
              onSelect: () => act.mutate(() => cashieringApi.retryPrint(b.id)),
              disabled: b.failedCount === 0,
            },
            {
              label: 'Skip Failures',
              onSelect: () => act.mutate(() => printApi.skip(b.id, [])),
              disabled: b.failedCount === 0,
            },
          ]}
        />
      ),
    },
  ];
  return (
    <>
      <Card title="Print Batches" flush>
        <ErrorAlert error={list.error ?? act.error ?? open.error ?? download.error} />
        <DataTable
          caption="Print batches"
          list="cashiering-print-batches"
          columns={columns}
          rows={list.data?.content ?? []}
          rowKey={(b) => b.id}
          loading={list.isLoading}
          emptyMessage="No print batch yet"
          onRowClick={(b) => setShown(b.id)}
        />
        <PageFooter data={list.data} noun="batches" onPage={setPage} />
      </Card>
      {shown !== undefined && <BatchLines key={shown} batchId={shown} />}
      {preview && (
        <PrintPreview
          file={preview.file}
          title={preview.batch.batchNo}
          onClose={() => setPreview(undefined)}
          onSkip={
            failedLines(preview.batch).length > 0
              ? () => act.mutate(() => printApi.skip(preview.batch.id, []))
              : undefined
          }
        />
      )}
    </>
  );
}
