import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Paperclip, Trash2 } from 'lucide-react';
import { attachmentsApi } from '@/api/attachments';
import type { AttachmentInfo } from '@/api/attachments';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { formatBytes } from '@/utils/files';
import { AttachmentUploadBar } from './AttachmentUploadBar';

interface AttachmentsProps {
  /** Owning record type, e.g. "JournalBatch". */
  entityType: string;
  /** Owning record id or business key. */
  entityId: string | number;
  title?: string;
}

const INFO_COLUMNS: Column<AttachmentInfo>[] = [
  { key: 'f', header: 'File', render: (a) => <strong>{a.fileName}</strong> },
  { key: 'd', header: 'Description', render: (a) => a.description ?? '' },
  { key: 's', header: 'Size', numeric: true, render: (a) => formatBytes(a.sizeBytes) },
  { key: 'u', header: 'Uploaded by', render: (a) => a.uploadedBy },
  { key: 't', header: 'Uploaded', render: (a) => formatDateTime(a.uploadedAt) },
  {
    key: 'c',
    header: 'SHA-256',
    render: (a) => (
      <span className="muted" title={a.sha256} style={{ fontFamily: 'var(--font-mono)' }}>
        {a.sha256.slice(0, 12)}…
      </span>
    ),
  },
];

/**
 * Supporting documents of any record: list, upload (PDF, images, Excel, CSV, Word), download and
 * remove. Files are stored with a SHA-256 checksum and every action is audited on the server.
 */
export function Attachments({
  entityType,
  entityId,
  title = 'Attachments',
}: Readonly<AttachmentsProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const id = String(entityId);
  const key = ['attachments', entityType, id];
  const canManage = can('ATTACHMENT_MANAGE');

  const list = useQuery({ queryKey: key, queryFn: () => attachmentsApi.list(entityType, id) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: key });
  const upload = useMutation({
    mutationFn: ({ file, description }: { file: File; description: string }) =>
      attachmentsApi.upload(entityType, id, file, description),
    onSuccess: async (a) => {
      await refresh();
      toast.success(`${a.fileName} attached`);
    },
  });
  const remove = useMutation({
    mutationFn: (a: AttachmentInfo) => attachmentsApi.remove(a.id),
    onSuccess: refresh,
  });
  const download = useMutation({
    mutationFn: (a: AttachmentInfo) => attachmentsApi.download(a.id),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });

  const actions: Column<AttachmentInfo> = {
    key: 'a',
    header: 'Actions',
    render: (a) => (
      <div className="row">
        <Button
          size="sm"
          variant="ghost"
          icon={<Download size={14} />}
          aria-label={`Download ${a.fileName}`}
          onClick={() => download.mutate(a)}
        />
        {canManage && (
          <Button
            size="sm"
            variant="ghost"
            icon={<Trash2 size={14} />}
            aria-label={`Remove ${a.fileName}`}
            onClick={() => remove.mutate(a)}
          />
        )}
      </div>
    ),
  };

  return (
    <Card
      title={
        <>
          <Paperclip size={16} aria-hidden="true" /> {title} ({list.data?.length ?? 0})
        </>
      }
      flush
    >
      {canManage && (
        <AttachmentUploadBar
          idSuffix={`${entityType}-${id}`}
          busy={upload.isPending}
          onUpload={(file, description) => upload.mutate({ file, description })}
        />
      )}
      <ErrorAlert error={upload.error ?? remove.error ?? download.error ?? list.error} />
      <DataTable<AttachmentInfo>
        loading={list.isLoading}
        rows={list.data ?? []}
        rowKey={(a) => a.id}
        emptyMessage="No documents attached."
        columns={[...INFO_COLUMNS, actions]}
      />
    </Card>
  );
}
