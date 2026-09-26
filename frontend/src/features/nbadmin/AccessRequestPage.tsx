import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CalendarClock,
  Check,
  CornerUpLeft,
  KeyRound,
  Pencil,
  ShieldAlert,
  UserRound,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessDecision, AccessRequest } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { requestActions } from './accessActions';
import type { RequestAction } from './accessActions';
import { isGroupProfile, REQUEST_TYPE_LABELS } from './accessRequest';
import { RequestApprovers, RequestDetails, RequestHistory } from './AccessRequestTabs';
import { ReasonDialog } from './ReasonDialog';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'approvers', label: 'Approvers' },
  { id: 'history', label: 'History' },
] as const;
type TabId = (typeof TABS)[number]['id'];
type DialogAction = Exclude<RequestAction, 'edit'>;

const DIALOGS: Record<
  DialogAction,
  { title: string; label: string; confirm: string; required: boolean; hint?: string }
> = {
  approve: {
    title: 'Approve and Apply',
    label: 'Comment',
    confirm: 'Approve and Apply',
    required: false,
  },
  secondApprove: {
    title: 'Give the Second Approval',
    label: 'Comment',
    confirm: 'Approve',
    required: false,
  },
  return: {
    title: 'Return to the Requester',
    label: 'Remarks',
    confirm: 'Return Request',
    required: true,
    hint: 'Tell the requester what to correct.',
  },
  reject: {
    title: 'Reject Request',
    label: 'Reason',
    confirm: 'Reject Request',
    required: true,
    hint: 'Sent to the requester; a rejection is final.',
  },
  cancel: { title: 'Cancel Request', label: 'Reason', confirm: 'Cancel Request', required: true },
  implement: {
    title: 'Implement Request',
    label: 'Comment',
    confirm: 'Implement Request',
    required: false,
    hint: 'The approved change is applied to the group profile now.',
  },
};

const BUTTONS: Record<RequestAction, { label: string; icon: typeof Check }> = {
  edit: { label: 'Edit Request', icon: Pencil },
  cancel: { label: 'Cancel Request', icon: X },
  approve: { label: 'Approve and Apply', icon: Check },
  secondApprove: { label: 'Second Approval', icon: Check },
  return: { label: 'Return', icon: CornerUpLeft },
  reject: { label: 'Reject', icon: X },
  implement: { label: 'Implement Request', icon: Wrench },
};

function perform(
  action: DialogAction,
  id: number,
  text: string,
): Promise<AccessDecision | AccessRequest> {
  switch (action) {
    case 'approve':
      return nbadminApi.approve(id, text || undefined);
    case 'secondApprove':
      return nbadminApi.secondApprove(id, text || undefined);
    case 'return':
      return nbadminApi.returnRequest(id, text);
    case 'reject':
      return nbadminApi.reject(id, text);
    case 'cancel':
      return nbadminApi.cancel(id, text);
    default:
      return nbadminApi.implement(id);
  }
}

function decisionText(r: AccessRequest): string {
  if (!r.decidedBy) {
    return 'Pending';
  }
  const when = `${r.decidedBy} · ${formatDateTime(r.decidedAt)}`;
  return r.decisionComment ? `${when} – ${r.decisionComment}` : when;
}

function Summary({ request: r }: Readonly<{ request: AccessRequest }>) {
  const who = isGroupProfile(r.type) ? r.roleCode : r.username;
  return (
    <RecordSummary
      title={`${REQUEST_TYPE_LABELS[r.type]} · ${who ?? ''}`}
      chips={
        <>
          <ReferenceChip label="Request" value={r.requestNo} />
          <StatusBadge status={r.status} />
        </>
      }
      flags={r.lifecycle.riskFlags.map((f) => (
        <span key={f} className="tag">
          <ShieldAlert size={12} aria-hidden="true" /> {humanize(f)}
        </span>
      ))}
      facts={[
        {
          icon: UserRound,
          label: 'Requested',
          value: `${r.requestedBy} · ${formatDateTime(r.requestedAt)}`,
        },
        {
          icon: Users,
          label: 'Approver',
          value: r.lifecycle.assignedApprover ?? r.decidedBy ?? 'Chosen on submission',
        },
        {
          icon: CalendarClock,
          label: 'Effective',
          value: r.details.effectiveFrom ? formatDate(r.details.effectiveFrom) : 'On approval',
        },
        {
          icon: Check,
          label: 'Decision',
          value: decisionText(r),
        },
      ]}
    />
  );
}

function Password({ decision }: Readonly<{ decision: AccessDecision }>) {
  if (decision.temporaryPassword === undefined) {
    return null;
  }
  return (
    <div className="alert success" role="status">
      <p>
        <KeyRound size={16} aria-hidden="true" /> User <strong>{decision.request.username}</strong>{' '}
        was created. Give this temporary password to the user through a secure channel; it is shown
        only now.
      </p>
      <code className="secret-value">{decision.temporaryPassword}</code>
    </div>
  );
}

function Banners({
  request: r,
  decision,
}: Readonly<{ request: AccessRequest; decision: AccessDecision | undefined }>) {
  return (
    <>
      {decision && <Password decision={decision} />}
      {r.status === 'RETURNED' && r.decisionComment && (
        <div className="alert warning" role="status">
          Returned by {r.decidedBy}: {r.decisionComment}
        </div>
      )}
      {r.lifecycle.cancelReason && (
        <div className="alert" role="status">
          Cancelled by {r.lifecycle.cancelledBy}: {r.lifecycle.cancelReason}
        </div>
      )}
    </>
  );
}

function ActionButtons({
  actions,
  onAction,
}: Readonly<{ actions: RequestAction[]; onAction: (a: RequestAction) => void }>) {
  return (
    <>
      {actions.map((a) => {
        const { label, icon: Icon } = BUTTONS[a];
        const primary = a === 'approve' || a === 'secondApprove' || a === 'implement';
        return (
          <Button
            key={a}
            variant={primary ? 'primary' : 'secondary'}
            icon={<Icon size={16} />}
            onClick={() => onAction(a)}
          >
            {label}
          </Button>
        );
      })}
    </>
  );
}

function RequestView({ request: r }: Readonly<{ request: AccessRequest }>) {
  const { can, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const [dialog, setDialog] = useState<DialogAction>();
  const [decision, setDecision] = useState<AccessDecision>();
  const settings = useQuery({ queryKey: ['nbadmin', 'settings'], queryFn: nbadminApi.settings });
  const act = useMutation({
    mutationFn: ({ action, text }: { action: DialogAction; text: string }) =>
      perform(action, r.id, text),
    onSuccess: async (result) => {
      setDialog(undefined);
      const done = 'request' in result ? result.request : result;
      if ('request' in result) {
        setDecision(result);
      }
      toast.success(`${done.requestNo}: ${humanize(done.status).toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
      await queryClient.invalidateQueries({ queryKey: ['approvals'] });
    },
  });
  const actions = requestActions(r, {
    username: user?.username ?? '',
    can,
    anyApprover: settings.data?.anyApprover === true,
  });
  const onAction = (action: RequestAction) => {
    if (action === 'edit') {
      void navigate(`/user-access/requests/${String(r.id)}/edit`);
    } else {
      setDialog(action);
    }
  };
  const spec = dialog === undefined ? undefined : DIALOGS[dialog];
  return (
    <div className="stack">
      <PageHeader
        section="User Access · Access Requests"
        backTo={isGroupProfile(r.type) ? '/user-access/group-profiles' : '/user-access/requests'}
        title={r.requestNo}
        description={r.summary}
        actions={<ActionButtons actions={actions} onAction={onAction} />}
      />
      <Banners request={r} decision={decision} />
      <Summary request={r} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'details' && <RequestDetails request={r} />}
      {tab === 'approvers' && <RequestApprovers request={r} />}
      {tab === 'history' && <RequestHistory requestId={r.id} />}
      {dialog !== undefined && spec !== undefined && (
        <ReasonDialog
          title={`${spec.title} ${r.requestNo}`}
          label={spec.label}
          confirmLabel={spec.confirm}
          required={spec.required}
          hint={spec.hint}
          busy={act.isPending}
          error={act.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(text) => act.mutate({ action: dialog, text })}
        >
          <p>{r.summary}</p>
        </ReasonDialog>
      )}
    </div>
  );
}

/**
 * An access request (BRD 1.008, 2.002; FR-UA-018, FR-UA-030 to FR-UA-034, FR-UA-045): summary with
 * status and risk flags, the actions of the viewer, and the tabs Details (current and requested
 * values), Approvers and History.
 */
export default function AccessRequestPage() {
  const id = Number(useParams().id);
  const request = useQuery({
    queryKey: ['nbadmin', 'request', id],
    queryFn: () => nbadminApi.request(id),
  });
  if (request.data === undefined) {
    return request.error ? (
      <ErrorAlert error={request.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <RequestView request={request.data} />;
}
