import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { PermissionName } from '@/components/ui/PermissionName';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { riskFlagLabel } from '@/features/nbadmin/riskFlags';
import { formatDateTime, humanize } from '@/utils/format';
import { CHANGE_LABELS } from './groupProfileChange';

type Decision = 'approve' | 'second' | 'reject' | 'return' | 'cancel' | 'implement' | 'submit';

const DECISIONS: Record<
  Decision,
  { label: string; effect: string; reason?: 'required' | 'optional'; destructive?: boolean }
> = {
  approve: {
    label: 'Approve',
    effect:
      'The change is approved; it applies at once, or after the second approval or the implementation when required.',
    reason: 'optional',
  },
  second: {
    label: 'Give Second Approval',
    effect: 'The privileged change is approved a second time.',
    reason: 'optional',
  },
  reject: {
    label: 'Reject',
    effect: 'The change is rejected and the maker is told.',
    reason: 'required',
    destructive: true,
  },
  return: {
    label: 'Return for Correction',
    effect: 'The maker corrects the change and submits it again.',
    reason: 'required',
  },
  cancel: {
    label: 'Cancel Change',
    effect: 'The change is withdrawn and does not apply.',
    reason: 'required',
    destructive: true,
  },
  implement: {
    label: 'Implement',
    effect: 'The approved change applies to the group profile now.',
  },
  submit: { label: 'Submit for Approval', effect: 'The draft goes to the checkers.' },
};

function variantOf(a: Decision): 'danger' | 'secondary' | 'accent' {
  if (DECISIONS[a].destructive === true) {
    return 'danger';
  }
  return a === 'cancel' ? 'secondary' : 'accent';
}

/** Who may take each action, by status: the checkers never on their own change. */
const RULES: readonly {
  status: AccessRequest['status'];
  maker: boolean;
  permission?: string;
  actions: Decision[];
}[] = [
  {
    status: 'PENDING',
    maker: false,
    permission: 'ACCESS_APPROVE',
    actions: ['approve', 'return', 'reject'],
  },
  {
    status: 'PENDING_SECOND',
    maker: false,
    permission: 'UAM_SECOND_APPROVE',
    actions: ['second', 'reject'],
  },
  { status: 'FOR_IMPLEMENTATION', maker: false, permission: 'ROLE_MANAGE', actions: ['implement'] },
  { status: 'DRAFT', maker: true, actions: ['submit'] },
];

const sameUser = (a: string, b: string) => a.toLocaleLowerCase() === b.toLocaleLowerCase();

/** The actions on the open change the viewer may take. */
function actionsOf(r: AccessRequest, me: string, can: (p: string) => boolean): Decision[] {
  const maker = sameUser(r.requestedBy, me);
  const actions = RULES.filter(
    (rule) =>
      rule.status === r.status &&
      rule.maker === maker &&
      (rule.permission === undefined || can(rule.permission)),
  ).flatMap((rule) => rule.actions);
  const cancellable = r.status !== 'FOR_IMPLEMENTATION' && (maker || can('UAM_CANCEL'));
  return cancellable ? [...actions, 'cancel'] : actions;
}

function decideOn(id: number, d: Decision, reason: string): Promise<AccessRequest> {
  const text = reason.trim();
  const optional = text === '' ? undefined : text;
  switch (d) {
    case 'approve':
      return nbadminApi.approve(id, optional).then((x) => x.request);
    case 'second':
      return nbadminApi.secondApprove(id, optional).then((x) => x.request);
    case 'reject':
      return nbadminApi.reject(id, text).then((x) => x.request);
    case 'return':
      return nbadminApi.returnRequest(id, text);
    case 'cancel':
      return nbadminApi.cancel(id, text);
    case 'implement':
      return nbadminApi.implement(id);
    default:
      return nbadminApi.submitSaved(id);
  }
}

function ChangeFacts({ change }: Readonly<{ change: AccessRequest }>) {
  const second = change.lifecycle.secondApprovalRequired
    ? `Needed: ${change.lifecycle.riskFlags.map(riskFlagLabel).join(', ')}`
    : undefined;
  return (
    <DefinitionGrid
      columns={2}
      items={[
        { label: 'Status', value: <StatusBadge status={change.status} /> },
        { label: 'Maker', value: <UserName login={change.requestedBy} /> },
        { label: 'Proposed on', value: formatDateTime(change.requestedAt) },
        { label: 'Change', value: change.summary, wide: true },
        {
          label: 'Permissions added',
          value: change.permissionsAdded.map((p) => <PermissionName key={p} code={p} />),
          wide: true,
        },
        {
          label: 'Permissions removed',
          value: change.permissionsRemoved.map((p) => <PermissionName key={p} code={p} />),
          wide: true,
        },
        { label: 'Reason', value: change.justification, wide: true },
        { label: 'Checker remarks', value: change.decisionComment, wide: true },
        { label: 'Second approval', value: second, wide: true },
      ]}
    />
  );
}

function MakerNote({ show }: Readonly<{ show: boolean }>) {
  return show ? (
    <p className="muted">Another user with the approval right decides your change.</p>
  ) : null;
}

/** The change of a group profile waiting for a decision, with the actions the viewer may take. */
export function PendingTab({
  change,
  onEdit,
}: Readonly<{ change?: AccessRequest; onEdit: (r: AccessRequest) => void }>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const queries = useQueryClient();
  const [deciding, setDeciding] = useState<Decision | null>(null);
  const decide = useMutation({
    mutationFn: ({ d, reason }: { d: Decision; reason: string }) =>
      decideOn(change?.id ?? 0, d, reason),
    onSuccess: async (r) => {
      toast.success(`${r.requestNo}: ${humanize(r.status)}`);
      setDeciding(null);
      await queries.invalidateQueries({ queryKey: ['nbadmin'] });
    },
  });
  if (change === undefined) {
    return <EmptyState message="No change of this group profile is waiting" />;
  }
  const me = user?.username ?? '';
  const maker = sameUser(change.requestedBy, me);
  const editable = maker && ['DRAFT', 'RETURNED'].includes(change.status);
  const actions = actionsOf(change, me, can);
  return (
    <Card title={`${CHANGE_LABELS[change.type]} · ${change.requestNo}`}>
      <div className="stack">
        <ChangeFacts change={change} />
        <MakerNote show={maker && change.status === 'PENDING'} />
        <div className="row" style={{ gap: 'var(--space-2)', flexWrap: 'wrap' }}>
          {editable && (
            <Button variant="secondary" onClick={() => onEdit(change)}>
              {change.status === 'RETURNED' ? 'Correct and Resubmit' : 'Edit Draft'}
            </Button>
          )}
          {actions.map((a) => (
            <Button key={a} variant={variantOf(a)} onClick={() => setDeciding(a)}>
              {DECISIONS[a].label}
            </Button>
          ))}
        </div>
      </div>
      {deciding !== null && (
        <ConfirmDialog
          title={`${DECISIONS[deciding].label}: ${change.requestNo}`}
          record={change.summary}
          effect={DECISIONS[deciding].effect}
          confirmLabel={DECISIONS[deciding].label}
          reason={DECISIONS[deciding].reason}
          destructive={DECISIONS[deciding].destructive === true}
          busy={decide.isPending}
          error={decide.error}
          onConfirm={(reason) => decide.mutate({ d: deciding, reason })}
          onClose={() => setDeciding(null)}
        />
      )}
    </Card>
  );
}
