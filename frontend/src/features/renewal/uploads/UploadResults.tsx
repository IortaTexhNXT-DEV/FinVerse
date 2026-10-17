import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalUploadsApi } from '@/api/renewalUploads';
import type { UploadKind, UploadRecord, UploadSummary } from '@/api/renewalUploads';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import type { UploadDefinition } from './uploadMessages';

const STATUSES = ['Successful', 'Partially Successful', 'Failed'];

function matchingLabel(code: string | null): string {
  if (code === null) return '';
  return code === 'MATCHED' ? 'Matched' : 'Unmatched';
}

/** The record-level results of an upload, with the matching filter and a search. */
function RecordsDialog({
  upload,
  onClose,
}: Readonly<{ upload: UploadSummary; onClose: () => void }>) {
  const companyId = useCompanyId();
  const [matching, setMatching] = useState('');
  const [search, setSearch] = useState('');
  const rows = useQuery({
    queryKey: ['renewal', 'upload-records', companyId, upload.uploadId, matching, search],
    queryFn: () =>
      renewalUploadsApi.records(
        companyId,
        upload.uploadId,
        matching || undefined,
        search || undefined,
      ),
  });
  const headers = Object.keys(rows.data?.[0]?.values ?? {}).slice(0, 6);
  return (
    <Modal
      open
      size="lg"
      title={`Upload ${upload.uploadId}`}
      onClose={onClose}
      facts={[
        { label: 'File name', value: upload.fileName },
        { label: 'Uploaded', value: formatDateTime(upload.uploadedAt) },
        { label: 'Status', value: upload.status },
      ]}
    >
      <div className="rnw-drill-tools">
        <select
          className="input"
          aria-label="Matching status"
          value={matching}
          onChange={(e) => setMatching(e.target.value)}
        >
          <option value="">All records</option>
          <option value="MATCHED">Matched</option>
          <option value="UNMATCHED">Unmatched</option>
        </select>
        <input
          className="input"
          aria-label="Search records"
          placeholder="Search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <ErrorAlert error={rows.error} />
      <div className="rnw-scroll">
        <DataTable<UploadRecord>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => String(r.rowNo)}
          emptyMessage="No record"
          columns={[
            { key: 'row', header: 'Row', kind: 'amount', render: (r) => r.rowNo },
            ...headers.map((h) => ({
              key: h,
              header: h,
              render: (r: UploadRecord) => r.values[h] ?? '',
            })),
            {
              key: 'match',
              header: 'Matching Status',
              render: (r) => matchingLabel(r.matchingStatus),
            },
            {
              key: 'processing',
              header: 'Processing Status',
              kind: 'status',
              render: (r) => (
                <StatusBadge
                  status={r.processingStatus === 'Success' ? 'COMPLETED' : 'FAILED'}
                  label={r.processingStatus}
                />
              ),
            },
            { key: 'reason', header: 'Reason', render: (r) => r.reason ?? '' },
          ]}
        />
      </div>
    </Modal>
  );
}

function UploadTable({ kind }: Readonly<{ kind: UploadKind }>) {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [open, setOpen] = useState<UploadSummary>();
  const rows = useQuery({
    queryKey: ['renewal', 'uploads', companyId, kind, search, status],
    queryFn: () =>
      renewalUploadsApi.uploads(companyId, kind, search || undefined, status || undefined),
  });
  return (
    <>
      <div className="rnw-drill-tools">
        <input
          className="input"
          aria-label="Search file name"
          placeholder="Search file name"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="input"
          aria-label="Upload status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <ErrorAlert error={rows.error} />
      <DataTable<UploadSummary>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(u) => u.uploadId}
        emptyMessage="No upload yet"
        columns={[
          {
            key: 'id',
            header: 'Upload ID',
            kind: 'code',
            render: (u) => (
              <button type="button" className="link-button" onClick={() => setOpen(u)}>
                {u.uploadId}
              </button>
            ),
          },
          { key: 'file', header: 'File Name', render: (u) => u.fileName },
          {
            key: 'at',
            header: 'Upload Date and Time',
            kind: 'datetime',
            render: (u) => formatDateTime(u.uploadedAt),
          },
          { key: 'by', header: 'Uploaded By', render: (u) => <UserName login={u.uploadedBy} /> },
          {
            key: 'status',
            header: 'Upload Status',
            kind: 'status',
            render: (u) => (
              <StatusBadge
                status={u.status === 'Successful' ? 'COMPLETED' : 'FAILED'}
                label={u.status}
              />
            ),
          },
          { key: 'remarks', header: 'Reason/Remarks', render: (u) => u.remarks },
          {
            key: 'action',
            header: 'Action',
            render: (u) => (
              <RowActionMenu
                label={u.uploadId}
                actions={[
                  { label: 'View Records', onSelect: () => setOpen(u) },
                  {
                    label: 'Download Processing Result',
                    onSelect: () =>
                      download.mutate(() => renewalUploadsApi.result(companyId, kind, u.uploadId)),
                  },
                ]}
              />
            ),
          },
        ]}
      />
      {open && <RecordsDialog upload={open} onClose={() => setOpen(undefined)} />}
    </>
  );
}

/** Upload outcome summaries, one tab per kind of upload the user may run. */
export function UploadResults({ uploads }: Readonly<{ uploads: UploadDefinition[] }>) {
  const [active, setActive] = useState<UploadKind>(uploads[0]?.kind ?? 'LAMD');
  if (uploads.length === 0) return null;
  return (
    <Card title="Upload Results">
      <Tabs
        tabs={uploads.map((u) => ({ id: u.kind, label: u.button.replace(' Upload', '') }))}
        active={active}
        onChange={setActive}
      />
      <UploadTable key={active} kind={active} />
    </Card>
  );
}
