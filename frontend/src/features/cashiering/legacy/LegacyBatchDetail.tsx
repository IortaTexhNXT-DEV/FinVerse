import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import type { LegacyBatch, LegacyBatchLine } from '@/api/legacyBatches';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { AddLinesPanel } from './AddLinesPanel';
import type { BatchScreen } from './batchScreens';
import { approverPermission, approvers } from './batchScreens';

interface BatchAction {
  title: string;
  effect: string;
  confirmLabel: string;
  reason?: 'required' | 'optional';
  destructive?: boolean;
  done: string;
  run: (note: string) => Promise<unknown>;
}

function actionsOf(
  screen: BatchScreen,
  batch: LegacyBatch,
  can: (p: string) => boolean,
): BatchAction[] {
  const no = batch.batchNo;
  const actions: BatchAction[] = [];
  if (batch.status === 'DRAFT' && can(screen.request)) {
    actions.push(
      {
        title: 'Submit Batch',
        effect: screen.approvalFlow,
        confirmLabel: 'Submit',
        done: 'Batch submitted',
        run: () => screen.api.submit(no),
      },
      {
        title: 'Cancel Batch',
        effect: 'The batch is closed without posting; its lines may be taken again.',
        confirmLabel: 'Cancel Batch',
        destructive: true,
        done: 'Batch cancelled',
        run: () => screen.api.cancel(no),
      },
    );
  }
  const approver = approverPermission(screen, batch);
  if (approver !== undefined && can(approver)) {
    actions.push(
      {
        title: 'Approve Batch',
        effect:
          batch.status === 'FOR_APPROVAL' && screen.finalApprove !== undefined
            ? 'The batch goes to top management for the second approval.'
            : 'Each line posts in its own transaction; a line that no longer fits is refused and the others still post.',
        confirmLabel: 'Approve',
        reason: 'optional',
        done: 'Batch approved',
        run: (note) => screen.api.approve(no, note),
      },
      {
        title: 'Return Batch',
        effect: 'The batch goes back to its requester as a draft.',
        confirmLabel: 'Return',
        reason: 'required',
        destructive: true,
        done: 'Batch returned',
        run: (note) => screen.api.returnBatch(no, note),
      },
    );
  }
  return actions;
}

/** A legacy batch: its facts, lines, the way to add lines while a draft, and its actions. */
export function LegacyBatchDetail({
  screen,
  batchNo,
}: Readonly<{ screen: BatchScreen; batchNo: string }>) {
  const { can } = useAuth();
  const client = useQueryClient();
  const [action, setAction] = useState<BatchAction>();
  const detail = useQuery({
    queryKey: ['legacy-batches', 'detail', batchNo],
    queryFn: () => screen.api.get(batchNo),
  });
  const refresh = () => client.invalidateQueries({ queryKey: ['legacy-batches'] });
  const remove = useMutation({
    mutationFn: (lineId: number) => screen.api.removeLine(batchNo, lineId),
    onSuccess: refresh,
  });
  const batch = detail.data?.batch;
  const draft = batch?.status === 'DRAFT' && can(screen.request);
  return (
    <div className="stack">
      <PageHeader
        section={screen.section}
        title={`${screen.title} ${batchNo}`}
        backTo={screen.listPath}
        actions={
          batch &&
          actionsOf(screen, batch, can).map((a) => (
            <Button key={a.title} variant="secondary" onClick={() => setAction(a)}>
              {a.confirmLabel}
            </Button>
          ))
        }
      />
      <ErrorAlert error={detail.error ?? remove.error} onRetry={() => void detail.refetch()} />
      {batch && <BatchFacts batch={batch} />}
      <BatchLines
        screen={screen}
        lines={detail.data?.lines ?? []}
        loading={detail.isLoading}
        onRemove={draft ? (id) => remove.mutate(id) : undefined}
        removing={remove.isPending}
      />
      {draft && <AddLinesPanel screen={screen} batch={batch} />}
      {action && (
        <BatchActionDialog action={action} batchNo={batchNo} onClose={() => setAction(undefined)} />
      )}
    </div>
  );
}

function BatchLines({
  screen,
  lines,
  loading,
  onRemove,
  removing,
}: Readonly<{
  screen: BatchScreen;
  lines: LegacyBatchLine[];
  loading: boolean;
  onRemove?: (lineId: number) => void;
  removing: boolean;
}>) {
  return (
    <Card flush>
      <DataTable<LegacyBatchLine>
        loading={loading}
        rows={lines}
        rowKey={(l) => l.id}
        emptyMessage="No line yet"
        columns={[
          { key: 'no', header: '#', numeric: true, render: (l) => String(l.lineNo) },
          {
            key: 'ref',
            header: screen.id === 'income' ? 'Unapplied Payment' : 'Invoice',
            render: (l) => (
              <CellStack
                main={
                  <>
                    {l.reference ?? l.invoiceNo}{' '}
                    {l.ledgerContext === 'LEGACY' && <Tag tone="info">LEGACY</Tag>}
                  </>
                }
                sub={l.ageDays === undefined ? l.reason : `${String(l.ageDays)} days old`}
              />
            ),
          },
          {
            key: 'amount',
            header: 'Amount',
            kind: 'amount',
            render: (l) => <Amount value={l.amount} />,
          },
          {
            key: 'result',
            header: 'Result',
            render: (l) => <CellStack main={l.journalBatchNo ?? ''} sub={l.message ?? ''} />,
          },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (l) => <StatusBadge status={l.status} />,
          },
          {
            key: 'remove',
            header: '',
            render: (l) =>
              onRemove ? (
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Trash2 size={14} />}
                  aria-label={`Remove line ${String(l.lineNo)}`}
                  busy={removing}
                  onClick={() => onRemove(l.id)}
                >
                  Remove
                </Button>
              ) : null,
          },
        ]}
      />
    </Card>
  );
}

function BatchFacts({ batch }: Readonly<{ batch: LegacyBatch }>) {
  return (
    <Card>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Status', value: <StatusBadge status={batch.status} /> },
          { label: 'Reason', value: batch.reason },
          { label: 'Total', value: <Amount value={batch.total} /> },
          { label: 'Lines', value: String(batch.lineCount) },
          { label: 'Requested By', value: batch.createdBy },
          { label: 'Submitted', value: formatDateTime(batch.submittedAt) },
          { label: 'Approved By', value: approvers(batch) },
          { label: 'Posted', value: formatDateTime(batch.executedAt) },
          { label: 'Returned Because', value: batch.returnReason, wide: true },
        ]}
      />
    </Card>
  );
}

function BatchActionDialog({
  action,
  batchNo,
  onClose,
}: Readonly<{ action: BatchAction; batchNo: string; onClose: () => void }>) {
  const client = useQueryClient();
  const toast = useToast();
  const run = useMutation({
    mutationFn: (note: string) => action.run(note),
    onSuccess: async () => {
      toast.success(action.done);
      await client.invalidateQueries({ queryKey: ['legacy-batches'] });
      onClose();
    },
  });
  return (
    <ConfirmDialog
      title={action.title}
      record={batchNo}
      effect={action.effect}
      confirmLabel={action.confirmLabel}
      reason={action.reason}
      destructive={action.destructive}
      busy={run.isPending}
      error={run.error}
      onConfirm={(note) => run.mutate(note)}
      onClose={onClose}
    />
  );
}
