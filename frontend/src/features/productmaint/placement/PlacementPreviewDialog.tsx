import { useQuery } from '@tanstack/react-query';
import { pmPlacementApi } from '@/api/pmPlacement';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { formatDate } from '@/utils/format';

type PreviewRow = Record<string, string> & { __key: string };

/** The preview of a Placement Update Report: the rows of its Excel file. */
export function PlacementPreviewDialog({
  id,
  onClose,
}: Readonly<{ id: number; onClose: () => void }>) {
  const preview = useQuery({
    queryKey: ['pm-placement', 'preview', id],
    queryFn: () => pmPlacementApi.preview(id),
  });
  const columns: Column<PreviewRow>[] = (preview.data?.columns ?? []).map((c) => ({
    key: c.key,
    header: c.label,
    numeric: c.key === 'aging',
    render: (r: PreviewRow) => r[c.key] ?? '',
  }));
  const rows: PreviewRow[] = (preview.data?.rows ?? []).map((r, i) => ({
    ...r,
    __key: String(i),
  }));
  const report = preview.data?.report;
  return (
    <Modal
      title={report ? `Preview ${report.fileName}` : 'Preview Placement Update Report'}
      open
      size="lg"
      onClose={onClose}
      facts={
        report
          ? [
              {
                label: 'Reporting Period',
                value: `${formatDate(report.periodFrom)} to ${formatDate(report.periodTo)}`,
              },
              { label: 'Records', value: String(report.recordCount) },
            ]
          : undefined
      }
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <ErrorAlert error={preview.error} />
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r) => r.__key}
        loading={preview.isLoading}
        emptyMessage="No quotation request in the period"
      />
    </Modal>
  );
}
