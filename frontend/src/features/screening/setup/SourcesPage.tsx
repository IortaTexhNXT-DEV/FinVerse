import { useQuery } from '@tanstack/react-query';
import { Download, Upload } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { IngestionRun, ListSource } from './api';
import { RunDialog, SourceDialog, UploadDialog } from './SourceDialogs';

function linkedRun(value: string | null): number | undefined {
  const id = Number(value ?? 0);
  return id > 0 ? id : undefined;
}

/**
 * List Sources and Runs (SNSRP-201, 202; FR-SS-020, 021): the watchlist sources and their
 * settings, the list file template, Upload List File, and the run log with its counts, status and
 * failed records. The job SCR_WATCHLIST_INGEST reads the files staged on each source.
 */
export default function SourcesPage() {
  const { can } = useAuth();
  const [search] = useSearchParams();
  const [source, setSource] = useState('');
  const [page, setPage] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [editing, setEditing] = useState<ListSource>();
  const [runId, setRunId] = useState<number | undefined>(() => linkedRun(search.get('run')));
  const download = useFileDownload();
  const sources = useQuery({
    queryKey: ['screening-setup', 'sources'],
    queryFn: screeningSetupApi.sources,
  });
  const runs = useQuery({
    queryKey: ['screening-setup', 'runs', source, page],
    queryFn: () => screeningSetupApi.runs({ source, page }),
  });
  const maintain = can('SCR_LIST_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section="Setup & Administration · Compliance Setup"
        title="List Sources and Runs"
        description="Sources of the sanctions, PEP and internal lists, the list file template and the log of every ingestion run with its failed records."
        actions={
          maintain && (
            <>
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                busy={download.isPending}
                onClick={() => download.mutate(screeningSetupApi.template)}
              >
                Download Template
              </Button>
              <Button
                variant="accent"
                icon={<Upload size={16} />}
                onClick={() => setUploading(true)}
              >
                Upload List File
              </Button>
            </>
          )
        }
      />
      <ErrorAlert error={sources.error ?? runs.error} />
      <ErrorAlert error={download.error} />
      <Card title="Sources" flush>
        <DataTable<ListSource>
          caption="List sources"
          rows={sources.data ?? []}
          rowKey={(s) => s.code}
          loading={sources.isLoading}
          onRowClick={maintain ? setEditing : undefined}
          columns={[
            { key: 'code', header: 'Code', render: (s) => <strong>{s.code}</strong> },
            { key: 'name', header: 'Name', render: (s) => s.name },
            { key: 'list', header: 'List Type', render: (s) => humanize(s.listType) },
            { key: 'transport', header: 'Transport', render: (s) => humanize(s.transport) },
            { key: 'schedule', header: 'Schedule', render: (s) => s.schedule ?? '—' },
            { key: 'layout', header: 'File Layout', render: (s) => s.fileLayout ?? '—' },
            { key: 'full', header: 'Full File', render: (s) => (s.fullFile ? 'Yes' : 'No') },
            {
              key: 'active',
              header: 'Status',
              render: (s) => <StatusBadge status={s.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
          ]}
        />
      </Card>
      <Card
        title="Run Log"
        flush
        actions={
          <select
            className="select"
            aria-label="Source of the runs"
            value={source}
            onChange={(e) => {
              setSource(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All sources</option>
            {(sources.data ?? []).map((s) => (
              <option key={s.code} value={s.code}>
                {s.code}
              </option>
            ))}
          </select>
        }
      >
        <DataTable<IngestionRun>
          caption="Ingestion runs"
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={runs.isLoading}
          onRowClick={(r) => setRunId(r.id)}
          emptyMessage="No run yet"
          columns={[
            { key: 'no', header: 'Run', render: (r) => <span className="mono">{r.runNo}</span> },
            { key: 'source', header: 'Source', render: (r) => r.sourceCode },
            { key: 'trigger', header: 'Trigger', render: (r) => humanize(r.trigger) },
            { key: 'file', header: 'File', render: (r) => r.fileName ?? '—' },
            { key: 'received', header: 'Received', numeric: true, render: (r) => r.received },
            { key: 'added', header: 'Added', numeric: true, render: (r) => r.added },
            { key: 'updated', header: 'Updated', numeric: true, render: (r) => r.updated },
            { key: 'delisted', header: 'Delisted', numeric: true, render: (r) => r.delisted },
            { key: 'failed', header: 'Failed', numeric: true, render: (r) => r.failed },
            { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'started', header: 'Started', render: (r) => formatDateTime(r.startedAt) },
          ]}
        />
        <PageFooter data={runs.data} noun="runs" onPage={setPage} />
      </Card>
      {uploading && (
        <UploadDialog
          sources={sources.data ?? []}
          onDone={(id) => {
            setUploading(false);
            setRunId(id);
          }}
          onClose={() => setUploading(false)}
        />
      )}
      {editing !== undefined && (
        <SourceDialog source={editing} onClose={() => setEditing(undefined)} />
      )}
      {runId !== undefined && <RunDialog runId={runId} onClose={() => setRunId(undefined)} />}
    </div>
  );
}
