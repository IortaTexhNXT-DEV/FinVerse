import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Download, FileSpreadsheet, Upload, XCircle } from 'lucide-react';
import { useState } from 'react';
import type { BulkJob, BulkRow, BulkRowStatus } from '@/api/bulk';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { NumberingPanel } from './NumberingPanel';
import { glPlatformApi } from './glPlatformApi';

type Tab = 'upload' | 'numbering' | 'history';

const TABS = [
  { id: 'upload', label: 'Upload Chart' },
  { id: 'numbering', label: 'Numbering Schemes' },
  { id: 'history', label: 'Upload History' },
] as const;

const FILTERS: { id: BulkRowStatus | ''; label: string }[] = [
  { id: '', label: 'All rows' },
  { id: 'INVALID', label: 'Invalid' },
  { id: 'VALID', label: 'Valid' },
  { id: 'COMMITTED', label: 'Created' },
  { id: 'FAILED', label: 'Failed' },
];

async function download(fetchFile: () => Promise<DownloadedFile>) {
  const f = await fetchFile();
  saveFile(f.blob, f.fileName);
}

/**
 * Chart of accounts upload (FRBS 2.3.1): download the template, upload the parent and child
 * accounts, review each row's validation, then create the valid accounts pending authorization.
 * Numbering schemes (FRBS 2.3.2) give children a system-generated code when the code is blank.
 */
export default function ChartUploadPage() {
  const [tab, setTab] = useState<Tab>('upload');
  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        backTo="/gl/accounts"
        title="Chart Upload & Numbering"
        description="Load parent and child accounts from a file and set how child account numbers are generated. Every account waits for authorization."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'upload' && <UploadPanel />}
      {tab === 'numbering' && <NumberingPanel />}
      {tab === 'history' && <HistoryPanel />}
    </div>
  );
}

function UploadPanel() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [job, setJob] = useState<BulkJob | null>(null);
  const [filter, setFilter] = useState<BulkRowStatus | ''>('');
  const rows = useQuery({
    queryKey: ['coa-upload-rows', job?.id, job?.status, filter],
    queryFn: () => glPlatformApi.uploadRows(job?.id ?? 0, filter || undefined),
    enabled: job !== null,
  });
  const upload = useMutation({
    mutationFn: (f: File) => glPlatformApi.uploadChart(companyId, f),
    onSuccess: (j) => {
      setJob(j);
      setFilter(j.invalidRows > 0 ? 'INVALID' : '');
    },
  });
  const commit = useMutation({
    mutationFn: () => glPlatformApi.commitUpload(job?.id ?? 0),
    onSuccess: async (j) => {
      setJob(j);
      setFilter(j.failedRows > 0 ? 'FAILED' : 'COMMITTED');
      await queryClient.invalidateQueries({ queryKey: ['accounts'] });
      await queryClient.invalidateQueries({ queryKey: ['coa-uploads'] });
      toast.success(`${j.committedRows} account(s) created, pending authorization`);
    },
  });
  const cancel = useMutation({
    mutationFn: () => glPlatformApi.cancelUpload(job?.id ?? 0),
    onSuccess: () => {
      setJob(null);
      setFile(null);
      toast.success('Upload discarded');
    },
  });

  return (
    <div className="stack">
      <ErrorAlert error={upload.error ?? commit.error ?? cancel.error} />
      <Card title="1. Template">
        <div className="row">
          <FileSpreadsheet size={20} aria-hidden="true" />
          <span>
            One row per account, parents before their children. Leave the account code blank to use
            the parent&apos;s numbering scheme. A sample file is in
            docs/samples/coa_upload_sample.xlsx.
          </span>
          <span className="spacer" />
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            onClick={() => void download(glPlatformApi.uploadTemplate)}
          >
            Download Template
          </Button>
        </div>
      </Card>
      {job === null ? (
        <Card title="2. Upload">
          <div className="stack">
            <Field
              label="Chart file"
              required
              hint="Excel (.xlsx), OpenDocument (.ods) or CSV, headers in row 1."
            >
              {(id) => (
                <input
                  id={id}
                  type="file"
                  className="input"
                  accept=".xlsx,.ods,.csv"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                />
              )}
            </Field>
            <div className="row">
              <Button
                variant="accent"
                icon={<Upload size={16} />}
                busy={upload.isPending}
                disabled={file === null}
                onClick={() => file !== null && upload.mutate(file)}
              >
                Upload and Validate
              </Button>
            </div>
          </div>
        </Card>
      ) : (
        <Card
          title={`3. Review — ${job.jobNo} (${job.fileName})`}
          actions={<StatusBadge status={job.status} />}
        >
          <div className="stack">
            <div className="grid-4">
              <Kpi label="Accounts" value={job.totalRows} />
              <Kpi label="Valid" value={job.validRows} accent />
              <Kpi label="Invalid" value={job.invalidRows} />
              <Kpi label="Created / failed" value={`${job.committedRows} / ${job.failedRows}`} />
            </div>
            <div className="row">
              {job.status === 'VALIDATED' && (
                <>
                  <Button
                    variant="accent"
                    icon={<CheckCircle2 size={16} />}
                    busy={commit.isPending}
                    disabled={job.validRows === 0}
                    onClick={() => commit.mutate()}
                  >
                    Create {job.validRows} Account(s)
                  </Button>
                  <Button
                    variant="secondary"
                    icon={<XCircle size={16} />}
                    busy={cancel.isPending}
                    onClick={() => cancel.mutate()}
                  >
                    Discard Upload
                  </Button>
                </>
              )}
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                onClick={() => void download(() => glPlatformApi.uploadReport(job.id))}
              >
                Result Report
              </Button>
              {job.status !== 'VALIDATED' && (
                <Button variant="ghost" onClick={() => setJob(null)}>
                  Upload Another File
                </Button>
              )}
              <span className="spacer" />
              <label className="visually-hidden" htmlFor="coa-row-filter">
                Rows to show
              </label>
              <select
                id="coa-row-filter"
                className="select"
                value={filter}
                onChange={(e) => setFilter(e.target.value as BulkRowStatus | '')}
              >
                {FILTERS.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.label}
                  </option>
                ))}
              </select>
            </div>
            <RowTable rows={rows.data?.content ?? []} loading={rows.isLoading} />
          </div>
        </Card>
      )}
    </div>
  );
}

function RowTable({ rows, loading }: Readonly<{ rows: BulkRow[]; loading: boolean }>) {
  return (
    <DataTable<BulkRow>
      loading={loading}
      rows={rows}
      rowKey={(r) => r.rowNo}
      caption="Uploaded accounts"
      emptyMessage="No rows in this view"
      columns={[
        { key: 'row', header: 'Row', width: '64px', render: (r) => r.rowNo },
        { key: 'st', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
        {
          key: 'code',
          header: 'Account Code',
          render: (r) => (
            <strong>{r.resultRef ?? r.values['Account Code'] ?? '(generated)'}</strong>
          ),
        },
        { key: 'name', header: 'Account Name', render: (r) => r.values['Account Name'] ?? '' },
        { key: 'parent', header: 'Parent', render: (r) => r.values['Parent Code'] ?? '' },
        { key: 'class', header: 'Class / Level', render: (r) => classLevel(r) },
        { key: 'msg', header: 'Messages', render: (r) => r.messages ?? '' },
      ]}
    />
  );
}

function classLevel(r: BulkRow): string {
  return [r.values.Class, r.values.Level].filter(Boolean).join(' / ');
}

function HistoryPanel() {
  const companyId = useCompanyId();
  const query = useQuery({
    queryKey: ['coa-uploads', companyId],
    queryFn: () => glPlatformApi.uploads(companyId),
    enabled: companyId > 0,
  });
  return (
    <Card flush>
      <ErrorAlert error={query.error} />
      <DataTable<BulkJob>
        loading={query.isLoading}
        rows={query.data?.content ?? []}
        rowKey={(j) => j.id}
        caption="Chart uploads"
        emptyMessage="No chart has been uploaded yet"
        columns={[
          { key: 'no', header: 'Upload No.', render: (j) => <strong>{j.jobNo}</strong> },
          { key: 'file', header: 'File', render: (j) => j.fileName },
          { key: 'at', header: 'Uploaded', render: (j) => formatDateTime(j.createdAt) },
          { key: 'by', header: 'By', render: (j) => j.createdBy },
          {
            key: 'rows',
            header: 'Created / Rows',
            numeric: true,
            render: (j) => `${j.committedRows} / ${j.totalRows}`,
          },
          { key: 'st', header: 'Status', render: (j) => <StatusBadge status={j.status} /> },
        ]}
      />
    </Card>
  );
}
