import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { collectionsApi } from './api';
import type { ScheduledFile } from './api';
import { reportTitle } from './collectionsLogic';
import './collections.css';

type FileTab = '' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ON_REQUEST';

const TABS: readonly { id: FileTab; label: string }[] = [
  { id: '', label: 'All Files' },
  { id: 'DAILY', label: 'Daily' },
  { id: 'WEEKLY', label: 'Weekly' },
  { id: 'MONTHLY', label: 'Monthly' },
  { id: 'ON_REQUEST', label: 'Exports' },
];

/** Runs a file publication for a date (Collections Setup users). */
function GenerateDialog({
  busy,
  error,
  onClose,
  onRun,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onRun: (frequency: string, date: string) => void;
}>) {
  const [frequency, setFrequency] = useState('DAILY');
  const [date, setDate] = useState(today());
  return (
    <Modal
      title="Generate Files"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={date === ''} onClick={() => onRun(frequency, date)}>
            Generate Files
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p className="clx-muted">
          The jobs publish the files every night; run one here to repeat a publication. A file
          already published for its period is kept. Monthly files are produced on the first working
          day only.
        </p>
        <div className="form-grid">
          <Field label="Files" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={frequency}
                onChange={(e) => setFrequency(e.target.value)}
              >
                <option value="DAILY">Daily files</option>
                <option value="WEEKLY">Weekly files (week ending that Friday)</option>
                <option value="MONTHLY">Monthly files (previous month)</option>
              </select>
            )}
          </Field>
          <Field label="Business Date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

/**
 * Collections files (BRCLXN.024-029, 045; caveat p.93): the daily Outstanding PR List and Full
 * Production Report, the weekly and monthly DP PR / PR 2307 for reversal files per unit and
 * branch, and the exports, each downloadable from its availability time.
 */
export default function FilesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [tab, setTab] = useState<FileTab>('');
  const [page, setPage] = useState(0);
  const [generating, setGenerating] = useState(false);
  const files = useQuery({
    queryKey: ['collections', 'files', companyId, tab, page],
    queryFn: () => collectionsApi.files(companyId, tab, page),
    enabled: companyId > 0,
  });
  const generate = useMutation({
    mutationFn: (v: { frequency: string; date: string }) =>
      collectionsApi.generate(companyId, v.frequency, v.date),
    onSuccess: async (out) => {
      setGenerating(false);
      await queryClient.invalidateQueries({ queryKey: ['collections', 'files'] });
      toast.success(
        out.length === 0 ? 'Nothing to publish for that date' : `${out.length} file(s) published`,
      );
    },
  });
  const mayDownload = can('CLX_EXPORT');
  const columns: Column<ScheduledFile>[] = [
    {
      key: 'r',
      header: 'Report',
      render: (f) => (
        <>
          <strong>{reportTitle(f.reportCode)}</strong>
          <div className="clx-muted">{f.scope}</div>
        </>
      ),
    },
    {
      key: 'p',
      header: 'Period',
      render: (f) => `${f.periodKey} (${formatDate(f.periodFrom)} – ${formatDate(f.periodTo)})`,
    },
    { key: 'n', header: 'Rows', numeric: true, render: (f) => f.rowCount },
    {
      key: 'c',
      header: 'Published',
      render: (f) => `${formatDateTime(f.createdAt)} · ${f.createdBy}`,
    },
    {
      key: 'a',
      header: 'Available From',
      render: (f) => (f.availableFrom === undefined ? 'At once' : formatDateTime(f.availableFrom)),
    },
    { key: 's', header: 'Status', render: (f) => <StatusBadge status={f.status} /> },
    {
      key: 'd',
      header: '',
      render: (f) =>
        mayDownload && f.available && f.reportRunId !== undefined ? (
          <Button
            size="sm"
            variant="secondary"
            icon={<Download size={14} />}
            onClick={() => {
              const id = f.reportRunId;
              if (id !== undefined) {
                download.mutate(() => collectionsApi.downloadFile(id));
              }
            }}
          >
            Download
          </Button>
        ) : (
          <span className="clx-muted">
            {f.status === 'FAILED' ? f.message : 'Not yet available'}
          </span>
        ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        backTo="/collections"
        title="Collections Files"
        description="Scheduled files and exports with the time from which they may be downloaded."
        actions={
          can('CLX_SETUP') ? (
            <Button variant="secondary" onClick={() => setGenerating(true)}>
              Generate Files
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={files.error ?? download.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <DataTable
            caption="Collections files"
            columns={columns}
            rows={files.data?.content ?? []}
            rowKey={(f) => f.id}
            loading={files.isLoading}
            emptyMessage="No file published yet"
          />
          <PageFooter data={files.data} noun="files" onPage={setPage} />
        </div>
      </Card>
      {generating && (
        <GenerateDialog
          busy={generate.isPending}
          error={generate.error}
          onClose={() => setGenerating(false)}
          onRun={(frequency, date) => generate.mutate({ frequency, date })}
        />
      )}
    </div>
  );
}
