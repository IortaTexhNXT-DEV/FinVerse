import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarClock, FileText, UserRound, Building2 } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { remittanceApi } from './api';
import type { Hold } from './api';
import { AssignDialog, ExtendDialog } from './HoldDialogs';
import { joinParts } from './remittanceLabels';
import './remittance.css';

const ENTITY = 'RemittanceHold';

/** How each business action of OPS_HOLD is called: endpoint and body (MKTID.002-006). */
const CALLS: Record<string, { path: string; body: (comment?: string) => unknown; ask: boolean }> = {
  submit: { path: 'submit', body: () => undefined, ask: false },
  cancel: { path: 'cancel', body: () => undefined, ask: false },
  approve: { path: 'decision', body: (comment) => ({ approve: true, comment }), ask: true },
  reject: { path: 'decision', body: (comment) => ({ approve: false, comment }), ask: true },
  request_cancel: { path: 'request-cancel', body: (comment) => ({ comment }), ask: true },
  release: { path: 'release', body: (comment) => ({ comment }), ask: true },
  approve_extension: {
    path: 'extension-decision',
    body: (comment) => ({ approve: true, comment }),
    ask: true,
  },
  reject_extension: {
    path: 'extension-decision',
    body: (comment) => ({ approve: false, comment }),
    ask: true,
  },
  approve_cancel: {
    path: 'cancel-decision',
    body: (comment) => ({ approve: true, comment }),
    ask: true,
  },
  reject_cancel: {
    path: 'cancel-decision',
    body: (comment) => ({ approve: false, comment }),
    ask: true,
  },
};

function Summary({ hold }: Readonly<{ hold: Hold }>) {
  return (
    <RecordSummary
      title={hold.assuredName}
      chips={
        <>
          <ReferenceChip label="Hold" value={hold.requestNo} />
          <StatusBadge status={hold.stage} />
        </>
      }
      flags={
        hold.source === 'COLLECTION_FEED' ? <span className="tag">Collection File</span> : undefined
      }
      facts={[
        {
          icon: FileText,
          label: 'Invoice',
          value: <Link to={remittanceApi.invoiceLink(hold.invoiceNo)}>{hold.invoiceNo}</Link>,
        },
        { icon: Building2, label: 'Insurer', value: hold.insurerCode },
        {
          icon: CalendarClock,
          label: 'Hold Until',
          value: joinParts([
            formatDate(hold.holdUntil),
            hold.requestedUntil && `extension to ${formatDate(hold.requestedUntil)} requested`,
          ]),
        },
        {
          icon: UserRound,
          label: 'Requested / Approved By',
          value: `${hold.requestedBy} / ${hold.approvedBy ?? '—'}`,
        },
        { icon: UserRound, label: 'Processor', value: hold.assignedProcessor ?? 'Unassigned' },
      ]}
    />
  );
}

/** The hold's business actions, each refreshing the record and its workflow. */
function useHoldAction(id: number, done: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (v: { path: string; body?: unknown }) =>
      remittanceApi.holdAction(id, v.path, v.body),
    onSuccess: async (h) => {
      done();
      queryClient.setQueryData(['remittance', 'hold', id], h);
      await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'holds'] });
      toast.success(`${h.requestNo}: ${h.stage.toLowerCase().replaceAll('_', ' ')}`);
    },
  });
}

/**
 * Remittance hold request (MKTID.002-007, RMTID.021): the invoice held, reason, hold-until date and
 * extensions, the OPS_HOLD workflow with the actions of the user (submit, approve or reject,
 * extend, request cancellation, release, assign) and its history.
 */
export default function HoldDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const [asking, setAsking] = useState<WorkAction>();
  const [dialog, setDialog] = useState<'extend' | 'assign'>();
  const hold = useQuery({
    queryKey: ['remittance', 'hold', id],
    queryFn: () => remittanceApi.hold(id),
  });
  const act = useHoldAction(id, () => {
    setAsking(undefined);
    setDialog(undefined);
  });
  if (hold.data === undefined) {
    return hold.error ? (
      <ErrorAlert error={hold.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const h = hold.data;
  const run = (a: WorkAction) => {
    if (a.action === 'extend') {
      setDialog('extend');
      return;
    }
    const call = CALLS[a.action];
    if (call === undefined) {
      return;
    }
    if (call.ask) {
      setAsking(a);
    } else {
      act.mutate({ path: call.path, body: call.body() });
    }
  };
  return (
    <div className="stack">
      <PageHeader
        section="Remittance · Holds"
        backTo="/remittance/holds"
        title={h.requestNo}
        description={joinParts([h.invoiceNo, `created ${formatDateTime(h.createdAt)}`, h.remarks])}
        actions={
          h.stage === 'ACTIVE' && can('HOLD_APPROVE') ? (
            <Button variant="secondary" onClick={() => setDialog('assign')}>
              Assign to Processor
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={act.error} />
      <Summary hold={h} />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={(actions) =>
          actions
            .filter((a) => a.action === 'extend' || a.action in CALLS)
            .map((a) => (
              <Button
                key={a.action}
                size="sm"
                variant="secondary"
                busy={act.isPending}
                onClick={() => run(a)}
              >
                {a.label}
              </Button>
            ))
        }
      />
      {asking !== undefined && (
        <ActionDialog
          title={asking.label}
          confirmLabel={asking.label}
          busy={act.isPending}
          error={act.error}
          onClose={() => setAsking(undefined)}
          onConfirm={(note) => {
            const call = CALLS[asking.action];
            if (call !== undefined) {
              act.mutate({ path: call.path, body: call.body(note.comment) });
            }
          }}
        />
      )}
      {dialog === 'extend' && (
        <ExtendDialog
          current={h.holdUntil}
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onExtend={(until, comment) =>
            act.mutate({ path: 'extend', body: { holdUntil: until, comment } })
          }
        />
      )}
      {dialog === 'assign' && (
        <AssignDialog
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onAssign={(username) => act.mutate({ path: 'assign', body: { username } })}
        />
      )}
    </div>
  );
}
