import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, CornerUpLeft, FileSpreadsheet, Send, UserRound, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessBatch, AccessBatchDecision, AccessRequest } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { REQUEST_TYPE_LABELS } from './accessRequest';
import { ApproverPicker } from './ApproverPicker';
import { ReasonDialog } from './ReasonDialog';

type BatchAction = 'submit' | 'approve' | 'return' | 'reject' | 'cancel';

const SPECS: Record<
  BatchAction,
  { title: string; label: string; confirm: string; required: boolean }
> = {
  submit: { title: 'Submit Batch', label: 'Remarks', confirm: 'Submit Batch', required: true },
  approve: {
    title: 'Approve Batch',
    label: 'Comment',
    confirm: 'Approve and Apply',
    required: false,
  },
  return: { title: 'Return Batch', label: 'Remarks', confirm: 'Return Batch', required: true },
  reject: { title: 'Reject Batch', label: 'Reason', confirm: 'Reject Batch', required: true },
  cancel: { title: 'Cancel Batch', label: 'Reason', confirm: 'Cancel Batch', required: true },
};

function actionsOf(b: AccessBatch, username: string, can: (p: string) => boolean): BatchAction[] {
  if (b.createdBy.toLowerCase() === username.toLowerCase()) {
    const open = b.status === 'DRAFT' || b.status === 'RETURNED';
    const actions: BatchAction[] = open ? ['submit'] : [];
    if (open || b.status === 'PENDING') {
      actions.push('cancel');
    }
    return actions;
  }
  return b.status === 'PENDING' && can('ACCESS_APPROVE') ? ['approve', 'return', 'reject'] : [];
}

function run(
  action: BatchAction,
  id: number,
  text: string,
  approver: string,
): Promise<AccessBatch | AccessBatchDecision> {
  switch (action) {
    case 'submit':
      return nbadminApi.submitBatch(id, approver, text);
    case 'approve':
      return nbadminApi.approveBatch(id, text || undefined);
    case 'return':
      return nbadminApi.returnBatch(id, text);
    case 'reject':
      return nbadminApi.rejectBatch(id, text);
    default:
      return nbadminApi.cancelBatch(id, text);
  }
}

function isDecision(r: AccessBatch | AccessBatchDecision): r is AccessBatchDecision {
  return 'lines' in r && Array.isArray(r.lines);
}

function Outcome({ decision }: Readonly<{ decision: AccessBatchDecision }>) {
  return (
    <div className="alert success" role="status">
      <p>Each line was approved on its own; new users receive the temporary password shown once:</p>
      <ul>
        {decision.lines.map((l) => (
          <li key={l.requestNo ?? l.username}>
            {l.requestNo} {l.username}: {l.error ?? l.status}
            {l.temporaryPassword && <code className="secret-value">{l.temporaryPassword}</code>}
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * A bulk access request batch (BRD 1.009; FR-UA-019): its lines with their status; the requester
 * submits it to one approver or cancels it; the approver approves (each line applied on its own),
 * returns or rejects it.
 */
export default function AccessBatchPage() {
  const id = Number(useParams().id);
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<BatchAction>();
  const [approver, setApprover] = useState<string[]>([]);
  const [decision, setDecision] = useState<AccessBatchDecision>();
  const batch = useQuery({
    queryKey: ['nbadmin', 'batch', id],
    queryFn: () => nbadminApi.batch(id),
  });
  const lines = useQuery({
    queryKey: ['nbadmin', 'batch', id, 'lines'],
    queryFn: () => nbadminApi.batchLines(id),
  });
  const act = useMutation({
    mutationFn: ({ action, text }: { action: BatchAction; text: string }) =>
      run(action, id, text, approver[0] ?? ''),
    onSuccess: async (result) => {
      setDialog(undefined);
      const b = isDecision(result) ? result.batch : result;
      if (isDecision(result)) {
        setDecision(result);
      }
      toast.success(`${b.batchNo}: ${b.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
    },
  });
  if (batch.data === undefined) {
    return batch.error ? (
      <ErrorAlert error={batch.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const b = batch.data;
  const actions = actionsOf(b, user?.username ?? '', can);
  const spec = dialog === undefined ? undefined : SPECS[dialog];
  const icons = { submit: Send, approve: Check, return: CornerUpLeft, reject: X, cancel: X };
  return (
    <div className="stack">
      <PageHeader
        section="User Access · Bulk Request"
        backTo="/user-access/bulk"
        title={b.batchNo}
        description={b.remarks ?? 'Bulk access request'}
        actions={actions.map((a) => {
          const Icon = icons[a];
          return (
            <Button
              key={a}
              variant={a === 'submit' || a === 'approve' ? 'primary' : 'secondary'}
              icon={<Icon size={16} />}
              onClick={() => setDialog(a)}
            >
              {SPECS[a].title}
            </Button>
          );
        })}
      />
      {decision && <Outcome decision={decision} />}
      <RecordSummary
        title={`Batch of ${String(b.lines)} line(s)`}
        chips={
          <>
            <ReferenceChip label="Batch" value={b.batchNo} />
            <StatusBadge status={b.status} />
          </>
        }
        facts={[
          {
            icon: UserRound,
            label: 'Requested',
            value: `${b.createdBy} · ${formatDateTime(b.createdAt)}`,
          },
          { icon: FileSpreadsheet, label: 'Lines', value: String(b.lines) },
        ]}
      />
      <Card title="Lines" flush>
        <ErrorAlert error={lines.error} />
        <DataTable<AccessRequest>
          caption="Line requests"
          loading={lines.isLoading}
          rows={lines.data ?? []}
          rowKey={(l) => l.id}
          onRowClick={(l) => void navigate(`/user-access/requests/${String(l.id)}`)}
          columns={[
            {
              key: 'n',
              header: 'Request',
              render: (l) => <span className="mono">{l.requestNo}</span>,
            },
            { key: 't', header: 'Type', render: (l) => REQUEST_TYPE_LABELS[l.type] },
            { key: 'u', header: 'User', render: (l) => l.username ?? '' },
            { key: 'c', header: 'Change', render: (l) => l.summary },
            { key: 's', header: 'Status', render: (l) => <StatusBadge status={l.status} /> },
            { key: 'e', header: 'Error', render: (l) => l.lifecycle.applyError ?? '' },
          ]}
        />
      </Card>
      {dialog !== undefined && spec !== undefined && (
        <ReasonDialog
          title={`${spec.title} ${b.batchNo}`}
          label={spec.label}
          confirmLabel={spec.confirm}
          required={spec.required}
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(text) => act.mutate({ action: dialog, text })}
        >
          {dialog === 'submit' && (
            <ApproverPicker
              userType="INTERNAL"
              value={approver}
              onChange={setApprover}
              multiple={false}
            />
          )}
        </ReasonDialog>
      )}
    </div>
  );
}
