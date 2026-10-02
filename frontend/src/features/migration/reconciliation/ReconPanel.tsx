import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { ReconLine } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatDateTime } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { BREAK_REASONS, migLabel } from '../common/migrationCodes';
import { useDisplayName } from '@/components/ui/useDisplayName';

function explainedBy(l: ReconLine, name: (login: string | undefined) => string): string {
  const who = [migLabel(l.breakReason), name(l.explainedBy)].join(' · ');
  return l.approvedBy ? who + ', approved by ' + name(l.approvedBy) : who;
}

const LEVELS: Record<string, string> = {
  L1: 'Counts',
  L2: 'Amounts',
  L3: 'Hash totals',
  L4: 'Fields',
  L5: 'General ledger',
  TU: 'Opening-balance adjustment',
};

/**
 * The latest reconciliation of a batch by level (counts, amounts, hash totals, fields, GL) with
 * the explanation of breaks and their approval.
 */
export function ReconPanel({ batchNo }: Readonly<{ batchNo: string }>) {
  const { can, user } = useAuth();
  const name = useDisplayName();
  const client = useQueryClient();
  const [explaining, setExplaining] = useState<ReconLine>();
  const run = useQuery({
    queryKey: ['migration', 'batch', batchNo, 'recon'],
    queryFn: () => migrationApi.reconciliation(batchNo),
  });
  const approve = useMutation({
    mutationFn: (lineId: number) => migrationApi.approveBreak(lineId),
    onSuccess: () => client.invalidateQueries({ queryKey: ['migration'] }),
  });
  const r = run.data;
  return (
    <Card
      flush
      title={
        r
          ? `Reconciliation ${r.runNo} · ${formatDateTime(r.runAt)} by ${name(r.runBy)}`
          : 'Reconciliation'
      }
      actions={r ? <MigStatus status={r.status} /> : undefined}
    >
      <ErrorAlert error={run.error ?? approve.error} onRetry={() => void run.refetch()} />
      <DataTable<ReconLine>
        loading={run.isLoading}
        rows={r?.lines ?? []}
        rowKey={(l) => l.id}
        emptyMessage="The batch is not reconciled yet"
        columns={[
          {
            key: 'level',
            header: 'Level',
            render: (l) => <CellStack main={l.level} sub={LEVELS[l.level]} />,
          },
          {
            key: 'measure',
            header: 'Measure',
            render: (l) => <CellStack main={l.measure} sub={l.detail} />,
          },
          { key: 'currency', header: 'Currency', render: (l) => l.currency ?? '' },
          {
            key: 'source',
            header: 'Source',
            kind: 'amount',
            render: (l) => <Amount value={l.sourceValue} />,
          },
          {
            key: 'staged',
            header: 'Staged',
            kind: 'amount',
            render: (l) => <Amount value={l.stagedValue} />,
          },
          {
            key: 'target',
            header: 'BIBS',
            kind: 'amount',
            render: (l) => <Amount value={l.targetValue} />,
          },
          {
            key: 'diff',
            header: 'Difference',
            kind: 'amount',
            render: (l) => <Amount value={l.difference} />,
          },
          {
            key: 'explanation',
            header: 'Explanation',
            render: (l) =>
              l.explanation ? <CellStack main={l.explanation} sub={explainedBy(l, name)} /> : '',
          },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (l) => <MigStatus status={l.status} />,
          },
          {
            key: 'actions',
            header: '',
            render: (l) => (
              <span className="mig-actions">
                {l.status === 'BREAK' && (can('MIG_DQ_RESOLVE') || can('MIG_LOAD_RUN')) && (
                  <Button variant="secondary" size="sm" onClick={() => setExplaining(l)}>
                    Explain
                  </Button>
                )}
                {l.status === 'EXPLAINED' &&
                  can('MIG_RECON_SIGNOFF') &&
                  l.explainedBy !== user?.username && (
                    <Button
                      variant="primary"
                      size="sm"
                      busy={approve.isPending}
                      onClick={() => approve.mutate(l.id)}
                    >
                      Approve
                    </Button>
                  )}
              </span>
            ),
          },
        ]}
      />
      {explaining !== undefined && (
        <ExplainDialog line={explaining} onClose={() => setExplaining(undefined)} />
      )}
    </Card>
  );
}

function ExplainDialog({ line, onClose }: Readonly<{ line: ReconLine; onClose: () => void }>) {
  const client = useQueryClient();
  const [reason, setReason] = useState('TIMING');
  const [text, setText] = useState('');
  const save = useMutation({
    mutationFn: () => migrationApi.explainBreak(line.id, reason, text),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Explain Break"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={text.trim() === ''}
            busy={save.isPending}
            onClick={() => save.mutate()}
          >
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <p>
        {line.level} {line.measure}: difference {String(line.difference ?? 0)}
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
              {BREAK_REASONS.map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Explanation" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
