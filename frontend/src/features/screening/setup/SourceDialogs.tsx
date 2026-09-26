import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { ListSource, RunDetail } from './api';

/** Upload List File (FR-SS-020): a CSV or XLSX in the list template, logged as a run at once. */
export function UploadDialog({
  sources,
  onDone,
  onClose,
}: Readonly<{ sources: ListSource[]; onDone: (runId: number) => void; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const files = sources.filter((s) => s.active);
  const [code, setCode] = useState(files[0]?.code ?? '');
  const [file, setFile] = useState<File>();
  const upload = useMutation({
    mutationFn: (f: File) => screeningSetupApi.upload(code, f),
    onSuccess: async (run) => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup'] });
      toast.success(
        `Run ${run.runNo}: ${run.added} added, ${run.updated} updated, ${run.failed} failed`,
      );
      onDone(run.id);
    },
  });
  return (
    <Modal
      open
      title="Upload List File"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={upload.isPending}
            disabled={file === undefined || code === ''}
            onClick={() => file && upload.mutate(file)}
          >
            Upload
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <Field label="Source" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={code}
              onChange={(e) => setCode(e.target.value)}
            >
              {files.map((s) => (
                <option key={s.code} value={s.code}>
                  {`${s.name} (${s.code})`}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field
          label="File"
          required
          hint="CSV or XLSX in the watchlist template; the changes wait for a Compliance Checker"
        >
          {(id) => (
            <input
              id={id}
              type="file"
              accept=".csv,.xlsx"
              className="input"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Settings of a list source: name, schedule, layout, full file, active (FR-SS-020). */
export function SourceDialog({
  source,
  onClose,
}: Readonly<{ source: ListSource; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [edit, setEdit] = useState(source);
  const save = useMutation({
    mutationFn: () => screeningSetupApi.updateSource(source.code, edit),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup', 'sources'] });
      toast.success(`Source ${source.code} saved`);
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Source ${source.code}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="Name" required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={edit.name}
                onChange={(e) => setEdit({ ...edit, name: e.target.value })}
              />
            )}
          </Field>
          <Field
            label="Schedule"
            hint="e.g. Daily 01:00 (the job cron is set by the administrator)"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                value={edit.schedule ?? ''}
                onChange={(e) => setEdit({ ...edit, schedule: e.target.value })}
              />
            )}
          </Field>
          <Field label="File Layout">
            {(id) => (
              <select
                id={id}
                className="select"
                value={edit.fileLayout ?? ''}
                onChange={(e) => setEdit({ ...edit, fileLayout: e.target.value || undefined })}
              >
                <option value="">None (manual)</option>
                <option value="CSV">CSV template</option>
                <option value="XLSX">XLSX template</option>
              </select>
            )}
          </Field>
          <Field
            label="Full File"
            hint="A new file replaces the list: missing entries are delisted"
          >
            {(id) => (
              <input
                id={id}
                type="checkbox"
                checked={edit.fullFile}
                onChange={(e) => setEdit({ ...edit, fullFile: e.target.checked })}
              />
            )}
          </Field>
          <Field label="Active">
            {(id) => (
              <input
                id={id}
                type="checkbox"
                checked={edit.active}
                onChange={(e) => setEdit({ ...edit, active: e.target.checked })}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

function Counts({ d }: Readonly<{ d: RunDetail }>) {
  const r = d.run;
  return (
    <dl className="detail-list">
      <dt>Status</dt>
      <dd>
        <StatusBadge status={r.status} />
      </dd>
      <dt>Source / Trigger</dt>
      <dd>{`${r.sourceCode} / ${humanize(r.trigger)}`}</dd>
      <dt>File</dt>
      <dd>{r.fileName ?? '—'}</dd>
      <dt>Received / Added / Updated</dt>
      <dd>{`${r.received} / ${r.added} / ${r.updated}`}</dd>
      <dt>Delisted / Unchanged / Failed</dt>
      <dd>{`${r.delisted} / ${r.unchanged} / ${r.failed}`}</dd>
      <dt>Started / Ended</dt>
      <dd>{`${formatDateTime(r.startedAt)} / ${formatDateTime(r.endedAt)}`}</dd>
      {r.error !== undefined && (
        <>
          <dt>Reason</dt>
          <dd className="text-danger">{r.error}</dd>
        </>
      )}
      {r.pendingApproval && (
        <>
          <dt>Waiting for Approval</dt>
          <dd>{d.pendingChanges}</dd>
        </>
      )}
    </dl>
  );
}

/** A run with its failed records (FR-SS-020, 021) and Approve All Changes for the checker. */
export function RunDialog({ runId, onClose }: Readonly<{ runId: number; onClose: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const detail = useQuery({
    queryKey: ['screening-setup', 'run', runId],
    queryFn: () => screeningSetupApi.run(runId),
  });
  const approve = useMutation({
    mutationFn: () => screeningSetupApi.approveRun(runId),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup'] });
      toast.success(`${r.approved} change(s) approved; the entries are active`);
    },
  });
  const d = detail.data;
  const mayApprove =
    d !== undefined &&
    d.pendingChanges > 0 &&
    can('SCR_LIST_APPROVE') &&
    d.run.createdBy !== user?.username;
  return (
    <Modal
      open
      title={d === undefined ? 'Ingestion Run' : `Run ${d.run.runNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {mayApprove && (
            <Button variant="accent" busy={approve.isPending} onClick={() => approve.mutate()}>
              Approve All Changes
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={detail.error ?? approve.error} />
        {d !== undefined && <Counts d={d} />}
        <DataTable
          caption="Failed records"
          rows={d?.errors ?? []}
          rowKey={(e) => e.lineNo}
          loading={detail.isLoading}
          emptyMessage="No failed record"
          columns={[
            { key: 'line', header: 'Line', numeric: true, render: (e) => e.lineNo },
            { key: 'reason', header: 'Reason', render: (e) => e.reason },
            {
              key: 'raw',
              header: 'Record',
              render: (e) => <span className="mono">{e.rawRecord}</span>,
            },
            { key: 'digest', header: 'E-mailed', render: (e) => (e.digested ? 'Yes' : 'No') },
          ]}
        />
      </div>
    </Modal>
  );
}
