import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileSpreadsheet, FileText, RefreshCw } from 'lucide-react';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import type { Batch } from './api';
import { remittanceSettingsApi } from './settingsApi';
import type { FileDelivery } from './settingsApi';

const COLUMNS: Column<FileDelivery>[] = [
  { key: 'when', header: 'Copied', render: (d) => formatDateTime(d.attemptedAt) },
  { key: 'stage', header: 'At', render: (d) => humanize(d.stage) },
  { key: 'file', header: 'File', render: (d) => d.fileName },
  { key: 'folder', header: 'Folder', render: (d) => d.folder },
  { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
  { key: 'error', header: 'Reason', render: (d) => d.error ?? '' },
  { key: 'by', header: 'By', render: (d) => <UserName login={d.attemptedBy} /> },
];

/**
 * The extract files of a batch (ICD IF-RMT-01 Excel, IF-RMT-02 text) and their copies to the
 * shared folder of the remittance type (IF-RMT-04), with a copy again after a failure.
 */
export function ExtractFilesCard({ batch }: Readonly<{ batch: Batch }>) {
  const id = batch.summary.id;
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const deliveries = useQuery({
    queryKey: ['remittance', 'batch', id, 'deliveries'],
    queryFn: () => remittanceSettingsApi.deliveries(id),
  });
  const resend = useMutation({
    mutationFn: () => remittanceSettingsApi.resend(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'batch', id, 'deliveries'] });
      toast.success('Extract files copied again');
    },
  });
  return (
    <Card
      title="Extract Files and Shared Folder"
      flush
      actions={
        <>
          <Button
            size="sm"
            variant="secondary"
            icon={<FileSpreadsheet size={14} />}
            busy={download.isPending}
            onClick={() => download.mutate(() => remittanceSettingsApi.extract(id, 'EXCEL'))}
          >
            Excel Extract
          </Button>
          <Button
            size="sm"
            variant="secondary"
            icon={<FileText size={14} />}
            busy={download.isPending}
            onClick={() => download.mutate(() => remittanceSettingsApi.extract(id, 'TEXT'))}
          >
            Text Extract
          </Button>
          {can('REMIT_PROCESS') && (
            <Button
              size="sm"
              variant="secondary"
              icon={<RefreshCw size={14} />}
              busy={resend.isPending}
              onClick={() => resend.mutate()}
            >
              Copy Again
            </Button>
          )}
        </>
      }
    >
      <ErrorAlert error={download.error ?? resend.error ?? deliveries.error} />
      <DataTable
        caption="Copies to the shared folder"
        columns={COLUMNS}
        rows={deliveries.data ?? []}
        rowKey={(d) => d.id}
        emptyMessage="No copy yet."
      />
    </Card>
  );
}
