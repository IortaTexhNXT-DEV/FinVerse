import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Inbox, Plus, Upload } from 'lucide-react';
import { useState } from 'react';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { CodeSelect } from './CashFields';
import type { ChannelFile, ChannelFileType, RunLine, RunSummaryRow } from './channelFilesApi';
import {
  FILE_STATUS_LABELS,
  FILE_TYPE_LABELS,
  channelFilesApi,
  filesPerType,
  pickedFiles,
} from './channelFilesApi';
import './cashiering.css';

interface UploadRow {
  key: number;
  type: ChannelFileType;
  file?: File;
}

const TYPES = Object.keys(FILE_TYPE_LABELS) as ChannelFileType[];

const SUMMARY_COLUMNS: Column<RunSummaryRow>[] = [
  { key: 'category', header: 'Category', render: (s) => s.category },
  { key: 'count', header: 'Count', numeric: true, render: (s) => s.count },
  { key: 'amount', header: 'Amount', numeric: true, render: (s) => <Amount value={s.amount} /> },
];

const LINE_COLUMNS: Column<RunLine>[] = [
  { key: 'date', header: 'Transaction Date', render: (l) => formatDate(l.transactionDate) },
  { key: 'file', header: 'BP Filename', truncate: true, render: (l) => l.fileName ?? '' },
  { key: 'txn', header: 'Transaction Number', render: (l) => l.transactionNo ?? '' },
  { key: 'amount', header: 'Amount', numeric: true, render: (l) => <Amount value={l.amount} /> },
  { key: 'ref', header: 'Reference Number', render: (l) => l.reference ?? '' },
  { key: 'account', header: 'Account Number', render: (l) => l.accountNo ?? '' },
  {
    key: 'status',
    header: 'Record Status',
    render: (l) => <StatusBadge status={l.status} label={l.status} />,
  },
  { key: 'why', header: 'Reason for Failure', truncate: true, render: (l) => l.messages ?? '' },
];

function UploadForm({ onDone }: Readonly<{ onDone: (files: ChannelFile[]) => void }>) {
  const companyId = useCompanyId();
  const [rows, setRows] = useState<UploadRow[]>([{ key: 1, type: 'BILLS_PAYMENT' }]);
  const upload = useMutation({
    mutationFn: () => channelFilesApi.upload(companyId, pickedFiles(rows)),
    onSuccess: (files) => {
      setRows([{ key: Date.now(), type: rows[0]?.type ?? 'BILLS_PAYMENT' }]);
      onDone(files);
    },
  });
  const change = (key: number, patch: Partial<UploadRow>) =>
    setRows((all) => all.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const selected = filesPerType(rows);
  return (
    <Card title="Upload Payment Files">
      <div className="stack">
        <ErrorAlert error={upload.error} />
        {rows.map((r) => (
          <div key={r.key} className="form-grid">
            <CodeSelect
              label="Payment File Type"
              value={r.type}
              options={TYPES}
              labelOf={(t) => FILE_TYPE_LABELS[t as ChannelFileType]}
              onChange={(v) => change(r.key, { type: v as ChannelFileType })}
            />
            <label className="field">
              <span className="field-label">File</span>
              <input
                type="file"
                className="input"
                onChange={(e) => change(r.key, { file: e.target.files?.[0] })}
              />
            </label>
          </div>
        ))}
        {Object.keys(selected).length > 0 && (
          <ul aria-label="Files selected per payment file type">
            {Object.entries(selected).map(([type, names]) => (
              <li key={type}>
                {type}: {names.join(', ')}
              </li>
            ))}
          </ul>
        )}
        <div className="worklist-actions">
          <Button
            variant="secondary"
            icon={<Plus size={16} />}
            onClick={() =>
              setRows((all) => [
                ...all,
                { key: Date.now(), type: all[all.length - 1]?.type ?? 'BILLS_PAYMENT' },
              ])
            }
          >
            Add File
          </Button>
          <Button
            variant="accent"
            icon={<Upload size={16} />}
            disabled={pickedFiles(rows).length === 0}
            busy={upload.isPending}
            onClick={() => upload.mutate()}
          >
            Upload
          </Button>
        </div>
      </div>
    </Card>
  );
}

function RunReportCard({ file }: Readonly<{ file: ChannelFile }>) {
  const report = useQuery({
    queryKey: ['cashiering', 'payment-file-report', file.id],
    queryFn: () => channelFilesApi.report(file.id),
  });
  return (
    <Card title={`Run Report ${file.uploadRef} · ${file.fileName}`}>
      <div className="stack">
        <ErrorAlert error={report.error} />
        {file.message && <p className="muted">{file.message}</p>}
        <DataTable
          caption="Summary per category"
          columns={SUMMARY_COLUMNS}
          rows={report.data?.summary ?? []}
          rowKey={(s) => s.category}
          emptyMessage="No payment record"
        />
        <DataTable
          caption="Payment records"
          list="cashiering-run-lines"
          columns={LINE_COLUMNS}
          rows={report.data?.lines ?? []}
          rowKey={(l) => l.rowNo}
          emptyMessage="No payment record"
        />
      </div>
    </Card>
  );
}

/**
 * Payment Files (FRS.CSH.05.01, 02.02.03 to 02.02.09): upload of several files of several types in
 * one event, the files received by upload or via MFT with the checks they passed or the reason they
 * were refused, and for each file the run report per category, the validation report, the failed
 * rows to correct, the raw file and processing again.
 */
export default function PaymentFilesPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [page, setPage] = useState(0);
  const [shown, setShown] = useState<ChannelFile | null>(null);
  const list = useQuery({
    queryKey: ['cashiering', 'payment-files', companyId, page],
    queryFn: () => channelFilesApi.list(companyId, page),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  const reprocess = useMutation({
    mutationFn: (id: number) => channelFilesApi.reprocess(id),
    onSuccess: async (f) => {
      toast.success(`${f.uploadRef}: ${FILE_STATUS_LABELS[f.status]}`);
      await refresh();
    },
  });
  const intake = useMutation({
    mutationFn: () => channelFilesApi.mftIntake(),
    onSuccess: async (files) => {
      toast.success(`${files.length} file(s) received via MFT`);
      await refresh();
    },
  });
  const columns: Column<ChannelFile>[] = [
    { key: 'ref', header: 'Upload Reference', render: (f) => <strong>{f.uploadRef}</strong> },
    { key: 'type', header: 'Payment File Type', render: (f) => FILE_TYPE_LABELS[f.fileType] },
    { key: 'name', header: 'File Name', truncate: true, render: (f) => f.fileName },
    { key: 'source', header: 'Source', render: (f) => (f.source === 'MFT' ? 'MFT' : 'Upload') },
    { key: 'rows', header: 'Rows', numeric: true, render: (f) => f.rowsRead },
    { key: 'failed', header: 'Failed', numeric: true, render: (f) => f.rowsFailed },
    { key: 'received', header: 'Received', render: (f) => formatDateTime(f.receivedAt) },
    {
      key: 'status',
      header: 'Status',
      render: (f) => <StatusBadge status={f.status} label={FILE_STATUS_LABELS[f.status]} />,
    },
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (f) => (
        <RowActionMenu
          label={f.uploadRef}
          actions={[
            { label: 'Run Report', onSelect: () => setShown(f) },
            {
              label: 'Download Run Report',
              onSelect: () => download.mutate(() => channelFilesApi.reportFile(f.id)),
              disabled: f.runNo === undefined,
            },
            {
              label: 'Validation Report',
              onSelect: () => download.mutate(() => channelFilesApi.validationFile(f.id)),
              disabled: f.runNo === undefined,
            },
            {
              label: 'Failed Rows',
              onSelect: () => download.mutate(() => channelFilesApi.failedFile(f.id)),
              disabled: f.rowsFailed === 0,
            },
            {
              label: 'Raw File',
              onSelect: () => download.mutate(() => channelFilesApi.rawFile(f.id)),
              disabled: !f.hasRaw,
            },
            {
              label: 'Process Again',
              onSelect: () => reprocess.mutate(f.id),
              disabled: f.status !== 'PARTIAL',
            },
          ]}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Payment Files"
        description="Bills Payment, CLPC, Trade, Direct Credit, post-dated checks and Commission Schedule files in the bank and channel layouts."
        actions={
          <Button
            variant="secondary"
            icon={<Inbox size={16} />}
            busy={intake.isPending}
            onClick={() => intake.mutate()}
          >
            Receive MFT Files Now
          </Button>
        }
      />
      <ErrorAlert error={list.error ?? reprocess.error ?? intake.error ?? download.error} />
      <UploadForm
        onDone={(files) => {
          toast.success(`${files.length} file(s) uploaded`);
          setShown(files[0] ?? null);
          void refresh();
        }}
      />
      <Card title="Files Received">
        <DataTable
          caption="Payment files received"
          list="cashiering-payment-files"
          columns={columns}
          rows={list.data?.content ?? []}
          rowKey={(f) => f.id}
          loading={list.isLoading}
          emptyMessage="No payment file received yet"
          onRowClick={setShown}
        />
        <PageFooter data={list.data} noun="files" onPage={setPage} />
      </Card>
      {shown && <RunReportCard key={shown.id} file={shown} />}
    </div>
  );
}
