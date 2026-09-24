import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { Feed, FeedRecord, FeedRun } from '@/api/operations';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { orUndefined } from './invoiceSearch';

const RECORD_COLUMNS: Column<FeedRecord>[] = [
  { key: 'k', header: 'Record', render: (r) => r.idempotencyKey },
  { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'r', header: 'Created', render: (r) => r.reference ?? '' },
  { key: 'm', header: 'Message', render: (r) => r.message ?? '' },
  { key: 't', header: 'Received', render: (r) => formatDateTime(r.receivedAt) },
];

/** The records of a run with their outcome (BRQID.005/006). */
export function RunRecordsDialog({
  run,
  onClose,
}: Readonly<{ run: FeedRun; onClose: () => void }>) {
  const records = useQuery({
    queryKey: ['ops', 'run-records', run.id],
    queryFn: () => opsApi.records(run.id),
  });
  return (
    <Modal
      title={`Run ${run.runNo}`}
      open
      onClose={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <div className="stack">
        <p>{run.message}</p>
        {run.errorDetail !== undefined && <ErrorAlert error={new Error(run.errorDetail)} />}
        <DataTable
          caption="Run records"
          columns={RECORD_COLUMNS}
          rows={records.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={records.isLoading}
          emptyMessage="No records in this run"
        />
      </div>
    </Modal>
  );
}

/** Schedule and activation of a feed. */
export function FeedConfigDialog({ feed, onClose }: Readonly<{ feed: Feed; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [cron, setCron] = useState(feed.cron);
  const [active, setActive] = useState(feed.active);
  const save = useMutation({
    mutationFn: () =>
      opsApi.configureFeed(feed.code, cron.trim() === '' ? '-' : cron.trim(), active),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ops', 'feeds'] });
      toast.success(`${feed.name} saved`);
      onClose();
    },
  });
  return (
    <Modal
      title={`Configure ${feed.name}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save Feed
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <Field label="Schedule (Spring cron, UTC)" hint="Use - for manual runs only">
          {(id) => (
            <input
              id={id}
              className="input"
              value={cron}
              onChange={(e) => setCron(e.target.value)}
            />
          )}
        </Field>
        <label className="ops-pref-row">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />{' '}
          Active
        </label>
      </div>
    </Modal>
  );
}

/** Upload of a feed file (manual transport). */
export function FeedUploadDialog({ feed, onClose }: Readonly<{ feed: Feed; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File>();
  const [error, setError] = useState<string>();
  const upload = useMutation({
    mutationFn: (f: File) => opsApi.uploadFeed(feed.code, f),
    onSuccess: async (run) => {
      await queryClient.invalidateQueries({ queryKey: ['ops'] });
      toast.success(`${run.runNo}: ${run.message ?? run.status}`);
      onClose();
    },
  });
  return (
    <Modal
      title={`Upload ${feed.name}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={upload.isPending}
            onClick={() => (file === undefined ? setError('Choose a file') : upload.mutate(file))}
          >
            Upload File
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <Field label="File" required error={error}>
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              onChange={(e) => {
                setFile(e.target.files?.[0]);
                setError(undefined);
              }}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Replay of booked invoices into the ledger: one invoice, one account or the company. */
export function ReplayDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [invoiceNo, setInvoiceNo] = useState('');
  const [arn, setArn] = useState('');
  const replay = useMutation({
    mutationFn: () =>
      opsApi.replay({
        invoiceNo: orUndefined(invoiceNo),
        arn: orUndefined(arn),
        companyId,
      }),
    onSuccess: async (run) => {
      await queryClient.invalidateQueries({ queryKey: ['ops'] });
      toast.success(`${run.runNo}: ${run.message ?? run.status}`);
      onClose();
    },
  });
  return (
    <Modal
      title="Replay Booked Invoices"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={replay.isPending} onClick={() => replay.mutate()}>
            Replay Invoices
          </Button>
        </>
      }
    >
      <div className="stack">
        <p className="ops-muted">
          Copies into the ledger the booked invoices it does not hold yet. Leave both fields blank
          to replay the whole company.
        </p>
        <ErrorAlert error={replay.error} />
        <Field label="Invoice No.">
          {(id) => (
            <input
              id={id}
              className="input"
              value={invoiceNo}
              onChange={(e) => setInvoiceNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="ARN">
          {(id) => (
            <input id={id} className="input" value={arn} onChange={(e) => setArn(e.target.value)} />
          )}
        </Field>
      </div>
    </Modal>
  );
}
