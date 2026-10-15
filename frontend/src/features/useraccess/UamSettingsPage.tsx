import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { SystemParameter } from '@/api/system';
import { userAccessApi } from '@/api/userAccess';
import { displayNameOf } from '@/api/users';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { settingGroup, valueHint } from './uamSettings';

const pending = (p: SystemParameter) => p.pendingValue !== undefined && p.pendingValue !== null;

function ChangeDialog({
  setting,
  onClose,
}: Readonly<{ setting: SystemParameter; onClose: () => void }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [value, setValue] = useState(setting.value);
  const save = useMutation({
    mutationFn: () => userAccessApi.changeSetting(setting.key, value.trim()),
    onSuccess: async () => {
      toast.success(`${setting.description}: change sent for approval`);
      await queries.invalidateQueries({ queryKey: ['uam-settings'] });
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Change ${setting.key}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={value.trim() === setting.value}
            onClick={() => save.mutate()}
          >
            Send for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} title="The change was not sent" />
        <p className="muted">{setting.description}</p>
        <Notice tone="info">The new value applies once another user approves it.</Notice>
        <Field label="New value" hint={valueHint(setting)}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={1000}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function DecideDialog({
  setting,
  mayApprove,
  onClose,
}: Readonly<{ setting: SystemParameter; mayApprove: boolean; onClose: () => void }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [reason, setReason] = useState('');
  const [tried, setTried] = useState(false);
  const decide = useMutation({
    mutationFn: (approve: boolean) =>
      approve
        ? userAccessApi.approveSetting(setting.key)
        : userAccessApi.rejectSetting(setting.key, reason.trim() || undefined),
    onSuccess: async (_p, approve) => {
      toast.success(approve ? `${setting.description}: change approved` : 'Change not applied');
      await queries.invalidateQueries({ queryKey: ['uam-settings'] });
      await queries.invalidateQueries({ queryKey: ['session-policy'] });
      onClose();
    },
  });
  const reasonMissing = mayApprove && reason.trim() === '';
  return (
    <Modal
      open
      title="UAM Setting to Approve"
      onClose={onClose}
      footer={
        <>
          <Button
            variant={mayApprove ? 'danger' : 'secondary'}
            busy={decide.isPending}
            onClick={() => {
              setTried(true);
              if (!reasonMissing) {
                decide.mutate(false);
              }
            }}
          >
            {mayApprove ? 'Reject' : 'Withdraw'}
          </Button>
          {mayApprove && (
            <Button variant="accent" busy={decide.isPending} onClick={() => decide.mutate(true)}>
              Approve
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={decide.error} title="The change was not decided" />
        <DefinitionGrid
          label="Change to approve"
          items={[
            { label: 'Setting', value: setting.key },
            { label: 'Current value', value: setting.value },
            { label: 'New value', value: setting.pendingValue },
            { label: 'Requested by', value: <UserName login={setting.pendingBy ?? ''} /> },
            { label: 'Requested on', value: formatDateTime(setting.pendingAt) },
          ]}
        />
        {!mayApprove && (
          <Notice tone="info">Another user with the approval right approves the change.</Notice>
        )}
        <Field
          label={mayApprove ? 'Reason (needed to reject)' : 'Reason'}
          error={tried && reasonMissing ? 'Enter the reason for the rejection' : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * UAM Settings: the session timeout and warnings, dormancy, lock-out and password of the local
 * test accounts, the provisioning and reconciliation and the group-profile switches. Every change
 * waits until another user with the approval right approves it (maker-checker).
 */
export default function UamSettingsPage() {
  const { can, user } = useAuth();
  const [editing, setEditing] = useState<SystemParameter | null>(null);
  const [deciding, setDeciding] = useState<SystemParameter | null>(null);
  const settings = useQuery({ queryKey: ['uam-settings'], queryFn: userAccessApi.settings });
  const maker = can('USER_MANAGE') || can('SYSTEM_PARAMETER_MANAGE');
  const checker = can('SECURITY_PARAMETER_APPROVE');
  const mine = (p: SystemParameter) =>
    user !== null && (p.pendingBy ?? '').toLocaleLowerCase() === user.username.toLocaleLowerCase();
  const open = (p: SystemParameter) => {
    if (pending(p)) {
      if ((checker && !mine(p)) || (maker && mine(p))) {
        setDeciding(p);
      }
    } else if (maker) {
      setEditing(p);
    }
  };
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="UAM Settings"
        description="Sessions, dormancy, local test accounts and synchronisation; each change is approved by a second user."
      />
      <ErrorAlert error={settings.error} />
      <Card flush>
        <DataTable<SystemParameter>
          caption="UAM settings"
          loading={settings.isLoading}
          rows={settings.data ?? []}
          rowKey={(p) => p.key}
          emptyMessage="No UAM setting is installed"
          onRowClick={maker || checker ? open : undefined}
          columns={[
            { key: 'g', header: 'Area', render: (p) => settingGroup(p.key) },
            {
              key: 'd',
              header: 'Setting',
              render: (p) => <CellStack main={p.description} sub={p.key} />,
            },
            {
              key: 'v',
              header: 'Value',
              render: (p) => (
                <CellStack
                  main={p.value}
                  sub={pending(p) ? `New value: ${p.pendingValue ?? ''}` : undefined}
                />
              ),
            },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (p) =>
                pending(p) ? (
                  <CellStack
                    main={<StatusBadge status="PENDING_APPROVAL" />}
                    sub={`by ${displayNameOf(p.pendingBy ?? '')}`}
                  />
                ) : (
                  <StatusBadge status="ACTIVE" label="In Force" />
                ),
            },
            {
              key: 'u',
              header: 'Last Changed',
              render: (p) =>
                p.updatedBy === undefined ? (
                  ''
                ) : (
                  <CellStack
                    main={<UserName login={p.updatedBy} />}
                    sub={formatDateTime(p.updatedAt)}
                  />
                ),
            },
          ]}
        />
      </Card>
      {editing !== null && <ChangeDialog setting={editing} onClose={() => setEditing(null)} />}
      {deciding !== null && (
        <DecideDialog
          setting={deciding}
          mayApprove={checker && !mine(deciding)}
          onClose={() => setDeciding(null)}
        />
      )}
    </div>
  );
}
