import { useQuery } from '@tanstack/react-query';
import { useFileDownload } from '@/components/broking/useFileDownload';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import type { ReceiptSummary } from './cashieringApi';
import type { ReceiptDocumentFile } from './printApi';
import { printApi } from './printApi';

/**
 * The Documents tab of a receipt (FRS.CSH.02.04.12 / 02.04.17): the print count and certificate
 * number, and the company's copies printed, each opened as a PDF.
 */
export function ReceiptDocuments({ summary }: Readonly<{ summary: ReceiptSummary }>) {
  const download = useFileDownload();
  const files = useQuery({
    queryKey: ['cashiering', 'receipt-documents', summary.id],
    queryFn: () => printApi.documents(summary.id),
  });
  const columns: Column<ReceiptDocumentFile>[] = [
    { key: 'name', header: 'Document', render: (f) => f.fileName },
    { key: 'by', header: 'Printed By', render: (f) => <UserName login={f.createdBy} /> },
    { key: 'at', header: 'Printed', render: (f) => formatDateTime(f.createdAt) },
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (f) => (
        <RowActionMenu
          label={f.fileName}
          actions={[
            {
              label: 'Download',
              onSelect: () => download.mutate(() => printApi.document(summary.id, f.id)),
            },
          ]}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <p className="muted">
        {summary.printedCount > 0
          ? `Printed ${summary.printedCount} time(s), certificate ${summary.certificateNo ?? ''}`
          : 'Not printed yet'}
      </p>
      <ErrorAlert error={files.error ?? download.error} />
      <DataTable
        caption="Documents of the receipt"
        columns={columns}
        rows={files.data ?? []}
        rowKey={(f) => f.id}
        loading={files.isLoading}
        emptyMessage="No document kept with this receipt"
      />
    </div>
  );
}
