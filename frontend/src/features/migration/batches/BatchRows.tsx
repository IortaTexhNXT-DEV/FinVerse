import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { Batch, StageRow } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageFooter } from '@/components/ui/Pager';
import { MigStatus } from '../common/MigStatus';
import { WAIVER_REASONS, migLabel } from '../common/migrationCodes';

const ROW_STATUSES = [
  'VALID',
  'WARNING',
  'INVALID',
  'LOADED',
  'SKIPPED',
  'REJECTED',
  'EXCLUDED',
  'WAIVED',
];

/** The staged rows of a batch, with the data owner's waiver or exclusion of failing rows. */
export function BatchRows({ batch }: Readonly<{ batch: Batch }>) {
  const { can } = useAuth();
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [resolving, setResolving] = useState<'waive' | 'exclude'>();
  const [resubmitting, setResubmitting] = useState(false);
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: ['migration', 'batch', batch.batchNo, 'rows', status, page],
    queryFn: () => migrationApi.batchRows(batch.batchNo, status || undefined, page),
  });
  const list = rows.data?.content ?? [];
  const mayResolve = can('MIG_DQ_WAIVE') && batch.status === 'VALIDATED';
  const cols = [
    {
      key: 'layout',
      header: 'Layout',
      kind: 'code' as const,
      render: (r: StageRow) => r.layoutCode,
    },
    { key: 'row', header: 'Row', kind: 'center' as const, render: (r: StageRow) => r.rowNo },
    {
      key: 'key',
      header: 'Legacy key',
      kind: 'code' as const,
      render: (r: StageRow) => r.legacyKey,
    },
    {
      key: 'target',
      header: 'BIBS record',
      render: (r: StageRow) => <CellStack main={r.targetCode ?? ''} sub={r.targetEntity} />,
    },
    { key: 'message', header: 'Message', render: (r: StageRow) => r.message ?? '' },
    {
      key: 'status',
      header: 'Status',
      kind: 'status' as const,
      render: (r: StageRow) => <MigStatus status={r.status} />,
    },
  ];
  return (
    <Card flush>
      <div className="worklist-filters form-grid">
        <Field label="Row status">
          {(id) => (
            <select
              id={id}
              className="select"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
            >
              <option value="">All rows</option>
              {ROW_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {migLabel(s)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <span className="mig-actions">
          {mayResolve && (
            <>
              <Button
                variant="secondary"
                size="sm"
                disabled={selection.keys.length === 0}
                onClick={() => setResolving('waive')}
              >
                Waive Rows
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={selection.keys.length === 0}
                onClick={() => setResolving('exclude')}
              >
                Exclude Rows
              </Button>
            </>
          )}
          {(can('MIG_DQ_RESOLVE') || can('MIG_INTAKE')) && batch.counts.rejected > 0 && (
            <Button variant="secondary" size="sm" onClick={() => setResubmitting(true)}>
              Resubmit Corrected Rows
            </Button>
          )}
        </span>
      </div>
      <ErrorAlert error={rows.error} onRetry={() => void rows.refetch()} />
      <DataTable<StageRow>
        loading={rows.isLoading}
        rows={list}
        rowKey={(r) => r.id}
        emptyMessage="No row"
        columns={
          mayResolve
            ? [
                selectionColumn(
                  list,
                  (r) => String(r.id),
                  selection,
                  (r) => `Row ${String(r.rowNo)}`,
                ),
                ...cols,
              ]
            : cols
        }
      />
      <PageFooter data={rows.data} noun="rows" onPage={setPage} />
      {resolving !== undefined && (
        <ResolveDialog
          batchNo={batch.batchNo}
          mode={resolving}
          rowIds={selection.keys.map(Number)}
          onClose={() => {
            selection.clear();
            setResolving(undefined);
          }}
        />
      )}
      {resubmitting && (
        <ResubmitDialog batchNo={batch.batchNo} onClose={() => setResubmitting(false)} />
      )}
    </Card>
  );
}

function ResolveDialog({
  batchNo,
  mode,
  rowIds,
  onClose,
}: Readonly<{
  batchNo: string;
  mode: 'waive' | 'exclude';
  rowIds: number[];
  onClose: () => void;
}>) {
  const client = useQueryClient();
  const [reason, setReason] = useState('NOT_NEEDED');
  const [note, setNote] = useState('');
  const save = useMutation({
    mutationFn: () =>
      mode === 'waive'
        ? migrationApi.waive(batchNo, rowIds, reason, note)
        : migrationApi.exclude(batchNo, rowIds, reason, note),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
    },
  });
  const title = mode === 'waive' ? 'Waive Rows' : 'Exclude Rows';
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={note.trim() === ''}
            busy={save.isPending}
            onClick={() => save.mutate()}
          >
            {title}
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <p>
        {mode === 'waive'
          ? `${String(rowIds.length)} rows load despite their errors and count as waived.`
          : `${String(rowIds.length)} rows are left out of the load; state how they are handled.`}
      </p>
      <div className="form-grid">
        <Field label="Reason" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            >
              {WAIVER_REASONS.map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={mode === 'waive' ? 'Note' : 'Manual-entry plan'} required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function ResubmitDialog({ batchNo, onClose }: Readonly<{ batchNo: string; onClose: () => void }>) {
  const client = useQueryClient();
  const [extractNo, setExtractNo] = useState('');
  const save = useMutation({
    mutationFn: () => migrationApi.resubmit(batchNo, extractNo.trim()),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Resubmit Corrected Rows"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={extractNo.trim() === ''}
            busy={save.isPending}
            onClick={() => save.mutate()}
          >
            Prepare
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <p>
        Upload the corrected rejection file as an extract first; a checker approves the resubmission
        before the corrected rows are loaded.
      </p>
      <Field label="Extract of the corrected rows" required>
        {(id) => (
          <input
            id={id}
            className="input"
            value={extractNo}
            onChange={(e) => setExtractNo(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
