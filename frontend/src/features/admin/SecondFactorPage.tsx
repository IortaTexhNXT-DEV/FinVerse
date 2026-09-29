import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { mfaApi } from '@/api/mfa';
import type { MfaResetRequest, UserSecondFactor } from '@/api/mfa';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

const REQUESTS = ['mfa-reset-requests'];
const USERS = ['mfa-users'];

/** Asks for the reset of a user's authenticator app, with the reason. */
function RequestReset({
  user,
  onClose,
}: Readonly<{ user: UserSecondFactor | null; onClose: () => void }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [reason, setReason] = useState('');
  const submit = useMutation({
    mutationFn: () => mfaApi.requestReset(user?.username ?? '', reason.trim()),
    onSuccess: () => {
      toast.success('Reset requested; another administrator approves it');
      void queries.invalidateQueries({ queryKey: REQUESTS });
      void queries.invalidateQueries({ queryKey: USERS });
      setReason('');
      onClose();
    },
  });
  return (
    <Modal
      title="Request Reset"
      open={user !== null}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={submit.isPending}
            disabled={reason.trim() === ''}
            onClick={() => submit.mutate()}
          >
            Request Reset
          </Button>
        </>
      }
    >
      <div className="stack">
        <p className="muted">
          The authenticator app of {user === null ? '' : <UserName login={user.username} />} is
          removed once another administrator approves; the user sets it up again at the next
          sign-in.
        </p>
        <ErrorAlert error={submit.error} />
        <Field label="Reason" required>
          {(id) => (
            <textarea
              id={id}
              className="input"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function RequestActions({ request }: Readonly<{ request: MfaResetRequest }>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const queries = useQueryClient();
  const refresh = () => {
    void queries.invalidateQueries({ queryKey: REQUESTS });
    void queries.invalidateQueries({ queryKey: USERS });
  };
  if (request.status !== 'PENDING') {
    return null;
  }
  const own = user?.username.toLowerCase() === request.requestedBy.toLowerCase();
  const record = `Second factor of ${request.username}`;
  return (
    <div className="row">
      {can('MFA_RESET_APPROVE') && !own && (
        <ConfirmButton
          size="sm"
          variant="primary"
          confirm={{
            title: 'Approve Reset',
            record,
            effect:
              'The authenticator app and the recovery codes are removed and the user is signed out.',
          }}
          onConfirm={() =>
            mfaApi.approveReset(request.id).then(() => {
              toast.success('Reset approved');
              refresh();
            })
          }
        >
          Approve
        </ConfirmButton>
      )}
      {(own || can('MFA_RESET_APPROVE')) && (
        <ConfirmButton
          size="sm"
          variant="secondary"
          confirm={{
            title: own ? 'Withdraw Reset' : 'Reject Reset',
            record,
            effect: 'The authenticator app stays as it is.',
            reason: own ? 'optional' : 'required',
            destructive: true,
          }}
          onConfirm={(reason) =>
            mfaApi.rejectReset(request.id, reason).then(() => {
              toast.success(own ? 'Reset withdrawn' : 'Reset rejected');
              refresh();
            })
          }
        >
          {own ? 'Withdraw' : 'Reject'}
        </ConfirmButton>
      )}
    </div>
  );
}

const REQUEST_COLUMNS: Column<MfaResetRequest>[] = [
  { key: 'user', header: 'User', render: (r) => <UserName login={r.username} /> },
  { key: 'reason', header: 'Reason', render: (r) => r.reason },
  { key: 'by', header: 'Requested by', render: (r) => <UserName login={r.requestedBy} /> },
  {
    key: 'at',
    header: 'Requested',
    kind: 'datetime',
    render: (r) => formatDateTime(r.requestedAt),
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (r) => <StatusBadge status={r.status} />,
  },
  { key: 'actions', header: 'Actions', render: (r) => <RequestActions request={r} /> },
];

/**
 * Administration of the second factor: the users with their authenticator app, and the reset of
 * a user's app under four eyes (one administrator requests with a reason, another approves).
 */
export default function SecondFactorPage() {
  const { can } = useAuth();
  const [resetting, setResetting] = useState<UserSecondFactor | null>(null);
  const requests = useQuery({ queryKey: REQUESTS, queryFn: () => mfaApi.resetRequests('PENDING') });
  const users = useQuery({ queryKey: USERS, queryFn: mfaApi.users });
  const userColumns: Column<UserSecondFactor>[] = [
    { key: 'user', header: 'User', render: (u) => <UserName login={u.username} /> },
    {
      key: 'app',
      header: 'Authenticator app',
      kind: 'status',
      render: (u) => (
        <StatusBadge
          status={u.enrolled ? 'ACTIVE' : 'NOT_SET_UP'}
          label={u.enrolled ? 'Set up' : 'Not set up'}
        />
      ),
    },
    {
      key: 'since',
      header: 'Set up',
      kind: 'datetime',
      render: (u) => formatDateTime(u.enrolledAt) || '—',
    },
    {
      key: 'used',
      header: 'Last used',
      kind: 'datetime',
      render: (u) => formatDateTime(u.lastUsedAt) || '—',
    },
    { key: 'privileged', header: 'Privileged role', render: (u) => (u.privileged ? 'Yes' : 'No') },
    {
      key: 'actions',
      header: 'Actions',
      render: (u) =>
        can('MFA_RESET') && u.enrolled && !u.resetPending ? (
          <Button size="sm" variant="secondary" onClick={() => setResetting(u)}>
            Request Reset
          </Button>
        ) : null,
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Second Factor"
        description="Authenticator apps of the users and their reset under four eyes."
      />
      <Card title="Resets waiting for approval" flush>
        <ErrorAlert error={requests.error} />
        <DataTable
          columns={REQUEST_COLUMNS}
          rows={requests.data ?? []}
          rowKey={(r) => r.id}
          loading={requests.isLoading}
          emptyMessage="No reset waits for approval."
        />
      </Card>
      <Card title="Users" flush>
        <ErrorAlert error={users.error} />
        <DataTable
          columns={userColumns}
          rows={users.data ?? []}
          rowKey={(u) => u.username}
          loading={users.isLoading}
          emptyMessage="No users."
        />
      </Card>
      <RequestReset user={resetting} onClose={() => setResetting(null)} />
    </div>
  );
}
