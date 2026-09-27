import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileArchive, Link2, Paperclip, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { AttachmentInfo, UploadOptions } from '@/api/attachments';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { formatBytes } from '@/utils/files';
import { AttachmentUploadBar } from './AttachmentUploadBar';

interface AttachmentsProps {
  /** Owning record type, e.g. "JournalBatch". */
  entityType: string;
  /** Owning record id or business key. */
  entityId: string | number;
  title?: string;
  /** Business reference (ARN, client code...) offered for nominated file names. */
  reference?: string;
  /** Show the document type list on upload (default on). */
  documentTypes?: boolean;
}

const INFO_COLUMNS: Column<AttachmentInfo>[] = [
  {
    key: 'f',
    header: 'File',
    render: (a) => (
      <>
        <strong>{a.fileName}</strong>
        {a.linked === true && (
          <span className="muted" title="Linked from another record">
            {' '}
            <Link2 size={12} aria-label="linked" />
          </span>
        )}
      </>
    ),
  },
  { key: 'k', header: 'Type', render: (a) => (a.documentType ? humanize(a.documentType) : '') },
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

/** Toggles one id in a selection. */
function toggled(selected: ReadonlySet<number>, id: number): Set<number> {
  const next = new Set(selected);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return next;
}

/**
 * Supporting documents of any record: list (own and linked files), upload one or several files
 * with a document type (PDF, images, Office, CSV, e-mails), download one file or a ZIP of the
 * selected files, and remove. Files are stored with a SHA-256 checksum and every action is
 * audited on the server.
 */
export function Attachments({
  entityType,
  entityId,
  title = 'Attachments',
  reference,
  documentTypes = true,
}: Readonly<AttachmentsProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const id = String(entityId);
  const key = ['attachments', entityType, id];
  const canManage = can('ATTACHMENT_MANAGE');
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const list = useQuery({ queryKey: key, queryFn: () => attachmentsApi.list(entityType, id) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: key });
  const upload = useMutation({
    mutationFn: ({ files, options }: { files: File[]; options: UploadOptions }) =>
      attachmentsApi.uploadMany(entityType, id, files, options),
    onSuccess: async (saved) => {
      await refresh();
      toast.success(
        saved.length === 1
          ? `${saved[0]?.fileName ?? 'File'} attached`
          : `${saved.length} files attached`,
      );
    },
  });
  const remove = useMutation({
    mutationFn: (a: AttachmentInfo) => attachmentsApi.removeFrom(a.id, entityType, id),
    onSuccess: refresh,
  });
  const download = useMutation({
    mutationFn: (a: AttachmentInfo) => attachmentsApi.download(a.id),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });
  const zip = useMutation({
    mutationFn: () => attachmentsApi.zip([...selected], reference ?? `${entityType}-${id}`),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });

  const rows = list.data ?? [];
  const pick: Column<AttachmentInfo> = {
    key: 'x',
    header: '',
    width: '32px',
    render: (a) => (
      <input
        type="checkbox"
        aria-label={`Select ${a.fileName}`}
        checked={selected.has(a.id)}
        onChange={() => setSelected((s) => toggled(s, a.id))}
      />
    ),
  };
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
            aria-label={a.linked === true ? `Unlink ${a.fileName}` : `Remove ${a.fileName}`}
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
          <Paperclip size={16} aria-hidden="true" /> {title} ({rows.length})
        </>
      }
      actions={
        rows.length > 1 && (
          <Button
            size="sm"
            variant="secondary"
            icon={<FileArchive size={14} />}
            disabled={selected.size === 0}
            busy={zip.isPending}
            onClick={() => zip.mutate()}
          >
            Download ZIP ({selected.size})
          </Button>
        )
      }
      flush
    >
      {canManage && (
        <AttachmentUploadBar
          idSuffix={`${entityType}-${id}`}
          busy={upload.isPending}
          documentTypes={documentTypes}
          reference={reference}
          onUpload={(files, options) => upload.mutate({ files, options })}
        />
      )}
      <ErrorAlert
        error={upload.error ?? remove.error ?? download.error ?? zip.error ?? list.error}
      />
      <DataTable<AttachmentInfo>
        loading={list.isLoading}
        rows={rows}
        rowKey={(a) => a.id}
        emptyMessage="No documents attached."
        columns={[pick, ...INFO_COLUMNS, actions]}
      />
    </Card>
  );
}
