import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, KeyRound, X } from 'lucide-react';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessDecision, AccessRequest } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { REQUEST_TYPE_LABELS, roleChanges } from './accessRequest';

function RoleChange({ request }: Readonly<{ request: AccessRequest }>) {
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const current = users.data?.find((u) => u.username === request.username)?.roleCodes ?? [];
  const { added, removed } = roleChanges(current, request.roleCodes);
  return (
    <>
      <dt>Roles added</dt>
      <dd>{added.join(', ') || '—'}</dd>
      <dt>Roles removed</dt>
      <dd>{removed.join(', ') || '—'}</dd>
    </>
  );
}

function Facts({ request: r }: Readonly<{ request: AccessRequest }>) {
  return (
    <dl className="detail-list">
      <dt>Request</dt>
      <dd>
        {REQUEST_TYPE_LABELS[r.type]} · <StatusBadge status={r.status} />
      </dd>
      <dt>User</dt>
      <dd>
        {r.username}
        {r.fullName ? ` – ${r.fullName}` : ''}
        {r.email ? ` (${r.email})` : ''}
      </dd>
      {r.roleCodes.length > 0 && (
        <>
          <dt>Requested roles</dt>
          <dd>{r.roleCodes.join(', ')}</dd>
        </>
      )}
      {r.type === 'MODIFY_ROLES' && r.status === 'PENDING' && <RoleChange request={r} />}
      <dt>Justification</dt>
      <dd>{r.justification}</dd>
      <dt>Requested</dt>
      <dd>
        {r.requestedBy} · {formatDateTime(r.requestedAt)}
      </dd>
      {r.decidedBy && (
        <>
          <dt>Decided</dt>
          <dd>
            {r.decidedBy} · {formatDateTime(r.decidedAt)}
            {r.decisionComment ? ` – ${r.decisionComment}` : ''}
          </dd>
        </>
      )}
    </dl>
  );
}

function Password({ decision }: Readonly<{ decision: AccessDecision }>) {
  if (decision.temporaryPassword === undefined) {
    return null;
  }
  return (
    <div className="alert success" role="status">
      <p style={{ marginTop: 0 }}>
        <KeyRound size={16} aria-hidden="true" /> User <strong>{decision.request.username}</strong>{' '}
        was created. Give this temporary password to the user through a secure channel; it is shown
        only now. Ask the user to change it on My Profile after the first sign-in.
      </p>
      <code className="secret-value">{decision.temporaryPassword}</code>
    </div>
  );
}

type DecisionState = 'decide' | 'own' | 'none';

/** Whether the viewer may decide the request (four eyes: never the requester). */
function decisionState(
  r: AccessRequest | undefined,
  approver: boolean,
  username: string | undefined,
): DecisionState {
  if (r?.status !== 'PENDING') {
    return 'none';
  }
  if (r.requestedBy === username) {
    return 'own';
  }
  return approver ? 'decide' : 'none';
}

function DecisionButtons({
  rejectable,
  deciding,
  onDecide,
}: Readonly<{
  rejectable: boolean;
  deciding: boolean | undefined;
  onDecide: (approve: boolean) => void;
}>) {
  return (
    <>
      <Button
        variant="danger"
        icon={<X size={16} />}
        disabled={!rejectable}
        busy={deciding === false}
        onClick={() => onDecide(false)}
      >
        Reject
      </Button>
      <Button
        variant="accent"
        icon={<Check size={16} />}
        busy={deciding === true}
        onClick={() => onDecide(true)}
      >
        Approve and Apply
      </Button>
    </>
  );
}

function DecisionNote({
  state,
  comment,
  onComment,
}: Readonly<{ state: DecisionState; comment: string; onComment: (text: string) => void }>) {
  if (state === 'own') {
    return (
      <div className="alert warning">You submitted this request: another approver decides it.</div>
    );
  }
  if (state !== 'decide') {
    return null;
  }
  return (
    <Field label="Comment" hint="Mandatory to reject; sent to the requester.">
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={2}
          maxLength={1000}
          value={comment}
          onChange={(e) => onComment(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * An access request with the Approver's decision (BRNB.085/079): approve (the change is applied)
 * or reject with a comment. The requester cannot decide their own request.
 */
export function AccessRequestDetailDialog({
  requestId,
  onClose,
}: Readonly<{ requestId: number; onClose: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [comment, setComment] = useState('');
  const [decision, setDecision] = useState<AccessDecision | null>(null);
  const request = useQuery({
    queryKey: ['nbadmin', 'request', requestId],
    queryFn: () => nbadminApi.request(requestId),
  });
  const note = comment.trim();
  const approvalNote = note === '' ? undefined : note;
  const decide = useMutation({
    mutationFn: (approve: boolean) =>
      approve ? nbadminApi.approve(requestId, approvalNote) : nbadminApi.reject(requestId, note),
    onSuccess: async (d) => {
      setDecision(d);
      toast.success(`${d.request.requestNo} ${d.request.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
      await queryClient.invalidateQueries({ queryKey: ['approvals'] });
    },
  });
  const r = decision?.request ?? request.data;
  const state = decisionState(r, can('ACCESS_APPROVE'), user?.username);
  return (
    <Modal
      open
      title={r ? `Access request ${r.requestNo}` : 'Access request'}
      onClose={onClose}
      footer={
        state === 'decide' ? (
          <DecisionButtons
            rejectable={comment.trim() !== ''}
            deciding={decide.isPending ? decide.variables : undefined}
            onDecide={(approve) => decide.mutate(approve)}
          />
        ) : undefined
      }
    >
      <div className="stack">
        <ErrorAlert error={request.error ?? decide.error} />
        {decision && <Password decision={decision} />}
        {r ? <Facts request={r} /> : <span className="spinner" aria-label="Loading" />}
        <DecisionNote state={state} comment={comment} onComment={setComment} />
      </div>
    </Modal>
  );
}
