import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Download, FileSpreadsheet, Upload, XCircle } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { bulkApi } from '@/api/bulk';
import type { BulkJob, BulkRow, BulkRowStatus } from '@/api/bulk';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';

interface BulkUploadWizardProps {
  handler: string;
  /** Upload parameters chosen on screen (e.g. product), passed to the handler. */
  parameters?: Record<string, string>;
  /** Extra inputs for the parameters, shown above the file picker. */
  parameterFields?: ReactNode;
  /** Disables the upload until the parameters are complete. */
  parametersReady?: boolean;
  /** Called after a successful commit. */
  onCommitted?: (job: BulkJob) => void;
}

const FILTERS: { id: BulkRowStatus | ''; label: string }[] = [
  { id: '', label: 'All rows' },
  { id: 'INVALID', label: 'Invalid' },
  { id: 'VALID', label: 'Valid' },
  { id: 'COMMITTED', label: 'Committed' },
  { id: 'FAILED', label: 'Failed' },
];

/**
 * Three-step bulk upload (BRNB.024/025/039): 1. download the template, 2. upload the filled file
 * (Excel, OpenDocument or CSV) and review every row's validation result, 3. commit the valid rows;
 * rows failing at commit are reported, and the result report can be downloaded at any time.
 */
export function BulkUploadWizard({
  handler,
  parameters,
  parameterFields,
  parametersReady = true,
  onCommitted,
}: Readonly<BulkUploadWizardProps>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [job, setJob] = useState<BulkJob | null>(null);
  const [filter, setFilter] = useState<BulkRowStatus | ''>('');
  const definition = useQuery({
    queryKey: ['bulk', 'handler', handler],
    queryFn: () => bulkApi.handler(handler),
  });
  const rows = useQuery({
    queryKey: ['bulk', 'rows', job?.id, filter],
    queryFn: () => bulkApi.rows(job?.id ?? 0, filter || undefined),
    enabled: job !== null,
  });
  const upload = useMutation({
    mutationFn: (f: File) => bulkApi.upload(companyId, handler, f, parameters),
    onSuccess: (j) => {
      setJob(j);
      setFilter(j.invalidRows > 0 ? 'INVALID' : '');
    },
  });
  const commit = useMutation({
    mutationFn: () => bulkApi.commit(job?.id ?? 0),
    onSuccess: async (j) => {
      setJob(j);
      setFilter(j.failedRows > 0 ? 'FAILED' : 'COMMITTED');
      await queryClient.invalidateQueries({ queryKey: ['bulk'] });
      toast.success(`${j.committedRows} row(s) processed`);
      onCommitted?.(j);
    },
  });
  const cancel = useMutation({
    mutationFn: () => bulkApi.cancel(job?.id ?? 0),
    onSuccess: (j) => {
      setJob(j);
      toast.success('Upload discarded');
    },
  });
  const download = async (fetchFile: () => ReturnType<typeof bulkApi.template>) => {
    const f = await fetchFile();
    saveFile(f.blob, f.fileName);
  };
  const reset = () => {
    setJob(null);
    setFile(null);
    upload.reset();
    commit.reset();
  };
  const columns = definition.data?.columns ?? [];

  return (
    <div className="stack">
      <ErrorAlert error={definition.error ?? upload.error ?? commit.error ?? cancel.error} />
      <Card title="1. Template">
        <div className="row">
          <FileSpreadsheet size={20} aria-hidden="true" />
          <span>
            Use the current template: {columns.length} columns,{' '}
            {columns.filter((c) => c.required).length} mandatory. Keep the headers unchanged.
          </span>
          <span className="spacer" />
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            onClick={() => void download(() => bulkApi.template(handler))}
          >
            Download Template
          </Button>
        </div>
      </Card>
      {job === null ? (
        <Card title="2. Upload">
          <div className="stack">
            {parameterFields}
            <Field
              label="File"
              required
              hint="Excel (.xlsx), OpenDocument (.ods) or CSV (.csv), first sheet, headers in row 1."
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
                disabled={file === null || !parametersReady}
                onClick={() => {
                  if (file !== null) {
                    upload.mutate(file);
                  }
                }}
              >
                Upload and Validate
              </Button>
            </div>
          </div>
        </Card>
      ) : (
        <JobReview
          job={job}
          rows={rows.data?.content ?? []}
          loading={rows.isLoading}
          filter={filter}
          onFilter={setFilter}
          headers={columns.map((c) => c.header)}
          committing={commit.isPending}
          cancelling={cancel.isPending}
          onCommit={() => commit.mutate()}
          onCancel={() => cancel.mutate()}
          onReport={() => void download(() => bulkApi.report(job.id))}
          onReset={reset}
        />
      )}
    </div>
  );
}

interface JobReviewProps {
  job: BulkJob;
  rows: BulkRow[];
  loading: boolean;
  filter: BulkRowStatus | '';
  onFilter: (f: BulkRowStatus | '') => void;
  headers: string[];
  committing: boolean;
  cancelling: boolean;
  onCommit: () => void;
  onCancel: () => void;
  onReport: () => void;
  onReset: () => void;
}

function JobReview(p: Readonly<JobReviewProps>) {
  const { job } = p;
  const open = job.status === 'VALIDATED';
  const shown = p.headers.slice(0, 5);
  return (
    <Card
      title={`3. Review and process — ${job.jobNo} (${job.fileName})`}
      actions={<StatusBadge status={job.status} />}
    >
      <div className="stack">
        <div className="grid-4">
          <Kpi label="Rows" value={job.totalRows} />
          <Kpi label="Valid" value={job.validRows} accent />
          <Kpi label="Invalid" value={job.invalidRows} />
          <Kpi
            label={open ? 'To process' : 'Processed / failed'}
            value={open ? job.validRows : `${job.committedRows} / ${job.failedRows}`}
          />
        </div>
        <div className="row">
          {open && (
            <>
              <Button
                variant="accent"
                icon={<CheckCircle2 size={16} />}
                busy={p.committing}
                disabled={job.validRows === 0}
                onClick={p.onCommit}
              >
                Process {job.validRows} Valid Row(s)
              </Button>
              <Button
                variant="secondary"
                icon={<XCircle size={16} />}
                busy={p.cancelling}
                onClick={p.onCancel}
              >
                Discard Upload
              </Button>
            </>
          )}
          <Button variant="secondary" icon={<Download size={16} />} onClick={p.onReport}>
            Result Report
          </Button>
          {!open && (
            <Button variant="ghost" onClick={p.onReset}>
              Upload Another File
            </Button>
          )}
          <span className="spacer" />
          <label className="visually-hidden" htmlFor="bulk-row-filter">
            Rows to show
          </label>
          <select
            id="bulk-row-filter"
            className="select"
            value={p.filter}
            onChange={(e) => p.onFilter(e.target.value as BulkRowStatus | '')}
          >
            {FILTERS.map((f) => (
              <option key={f.id} value={f.id}>
                {f.label}
              </option>
            ))}
          </select>
        </div>
        <DataTable<BulkRow>
          loading={p.loading}
          rows={p.rows}
          rowKey={(r) => r.rowNo}
          emptyMessage="No rows with this status."
          columns={[
            { key: 'row', header: 'Row', render: (r) => r.rowNo, numeric: true, width: '4rem' },
            { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            ...shown.map((h) => ({ key: h, header: h, render: (r: BulkRow) => r.values[h] ?? '' })),
            {
              key: 'msg',
              header: 'Messages / Reference',
              render: (r) =>
                r.messages ? (
                  <span className="field-error">{r.messages}</span>
                ) : (
                  (r.resultRef ?? '')
                ),
            },
          ]}
        />
      </div>
    </Card>
  );
}
