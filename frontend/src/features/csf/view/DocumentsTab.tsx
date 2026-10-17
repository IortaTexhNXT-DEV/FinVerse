import { useMutation, useQuery } from '@tanstack/react-query';
import { Download, FileArchive, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { csfApi } from '@/api/csf';
import type { AccountLine, CsfDocument } from '@/api/csf';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useLovLabel } from '@/components/broking/useLabels';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { CSF_LOV } from '../csfCodes';
import { ResendDialog } from '../dialogs/ResendDialog';
import { UploadDialog } from '../dialogs/UploadDialog';
import { TAB_UNAVAILABLE } from './AccountsTab';

function formatSize(bytes: number): string {
  if (bytes < 1024) {
    return `${String(bytes)} B`;
  }
  return bytes < 1024 * 1024
    ? `${(bytes / 1024).toFixed(0)} KB`
    : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function useDocumentColumns(): Column<CsfDocument>[] {
  const typeLabel = useLovLabel(CSF_LOV.platformDocumentType);
  return [
    {
      key: 'file',
      header: 'Document',
      render: (d) => <CellStack main={d.fileName} sub={formatSize(d.sizeBytes)} />,
    },
    {
      key: 'type',
      header: 'Type',
      render: (d) => (d.documentType ? typeLabel(d.documentType) : ''),
    },
    {
      key: 'record',
      header: 'Record',
      render: (d) => <CellStack main={d.reference} sub={d.recordType} />,
    },
    { key: 'by', header: 'Uploaded By', render: (d) => <UserName login={d.uploadedBy} /> },
    {
      key: 'at',
      header: 'Uploaded',
      kind: 'datetime',
      render: (d) => formatDateTime(d.uploadedAt),
    },
  ];
}

interface DocumentListProps {
  companyId: number;
  clientId: number;
  clientCode: string;
  documents: CsfDocument[];
  loading: boolean;
  /** Offer Resend (the renewal advices). */
  resend?: boolean;
}

/** A list of documents with download, ZIP of a selection and, for renewal advices, Resend. */
export function DocumentList({
  companyId,
  clientId,
  clientCode,
  documents,
  loading,
  resend = false,
}: Readonly<DocumentListProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const [resending, setResending] = useState<CsfDocument | null>(null);
  const selection = useRowSelection();
  const keyOf = (d: CsfDocument) => String(d.id);
  const columns = useDocumentColumns();
  const download = useMutation({
    mutationFn: (d: CsfDocument) => csfApi.download(companyId, clientId, d.id),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  const zip = useMutation({
    mutationFn: () => csfApi.zip(companyId, clientId, selection.keys.map(Number), clientCode),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  const mayDownload = can('ATTACHMENT_VIEW');
  const actions: Column<CsfDocument> = {
    key: 'actions',
    header: '',
    render: (d) => (
      <span className="row">
        {mayDownload && (
          <Button
            size="sm"
            variant="ghost"
            icon={<Download size={14} aria-hidden="true" />}
            aria-label={`Download ${d.fileName}`}
            onClick={() => download.mutate(d)}
          >
            Download
          </Button>
        )}
        {resend && can('CSF_RESEND') && (
          <Button
            size="sm"
            variant="ghost"
            icon={<Send size={14} aria-hidden="true" />}
            onClick={() => setResending(d)}
          >
            Resend
          </Button>
        )}
      </span>
    ),
  };
  return (
    <>
      <ErrorAlert error={download.error ?? zip.error} title="Cannot download the document" />
      {mayDownload && documents.length > 0 && (
        <div className="row">
          <Button
            variant="secondary"
            size="sm"
            icon={<FileArchive size={14} aria-hidden="true" />}
            disabled={selection.keys.length === 0}
            busy={zip.isPending}
            onClick={() => zip.mutate()}
          >
            Download ZIP
          </Button>
        </div>
      )}
      <DataTable
        columns={
          mayDownload
            ? [selectionColumn(documents, keyOf, selection, (d) => d.fileName), ...columns, actions]
            : columns
        }
        rows={documents}
        rowKey={(d) => d.id}
        loading={loading}
        caption="Documents"
      />
      {resending !== null && (
        <ResendDialog
          companyId={companyId}
          clientId={clientId}
          kind="RA"
          documentId={resending.id}
          onClose={() => setResending(null)}
        />
      )}
    </>
  );
}

/** The Renewal Advice tab (FR-CSF-030): the renewal advices of the client and its accounts. */
export function AdvicesTab({
  companyId,
  clientId,
  clientCode,
}: Readonly<{ companyId: number; clientId: number; clientCode: string }>) {
  const list = useQuery({
    queryKey: ['csf', 'advices', companyId, clientId],
    queryFn: () => csfApi.renewalAdvices(companyId, clientId),
  });
  return (
    <Card flush>
      <ErrorAlert error={list.error} title={TAB_UNAVAILABLE} onRetry={() => void list.refetch()} />
      <DocumentList
        companyId={companyId}
        clientId={clientId}
        clientCode={clientCode}
        documents={list.data ?? []}
        loading={list.isLoading}
        resend
      />
    </Card>
  );
}

/**
 * The Documents tab (FR-CSF-032, 033): every document of the client, its accounts and its
 * quotations the user may see, with Upload.
 */
export function DocumentsTab({
  companyId,
  clientId,
  clientCode,
  accounts,
}: Readonly<{
  companyId: number;
  clientId: number;
  clientCode: string;
  accounts: AccountLine[];
}>) {
  const { can } = useAuth();
  const [uploading, setUploading] = useState(false);
  const list = useQuery({
    queryKey: ['csf', 'documents', companyId, clientId],
    queryFn: () => csfApi.documents(companyId, clientId),
  });
  return (
    <Card
      flush
      title="Documents"
      actions={
        can('CSF_DOCUMENT_UPLOAD') && (
          <Button
            variant="primary"
            icon={<Upload size={16} aria-hidden="true" />}
            onClick={() => setUploading(true)}
          >
            Upload Document
          </Button>
        )
      }
    >
      <ErrorAlert error={list.error} title={TAB_UNAVAILABLE} onRetry={() => void list.refetch()} />
      <DocumentList
        companyId={companyId}
        clientId={clientId}
        clientCode={clientCode}
        documents={list.data ?? []}
        loading={list.isLoading}
      />
      {uploading && (
        <UploadDialog
          companyId={companyId}
          clientId={clientId}
          clientCode={clientCode}
          accounts={accounts}
          onClose={() => setUploading(false)}
        />
      )}
    </Card>
  );
}
