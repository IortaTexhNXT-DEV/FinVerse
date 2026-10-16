import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { ReactNode } from 'react';
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
import { Field, InfoTip } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import {
  formatSettingValue,
  rangeHint,
  settingGroup,
  settingLabel,
  settingUnit,
  settingValueError,
} from './uamSettings';

const pending = (p: SystemParameter) => p.pendingValue !== undefined && p.pendingValue !== null;

/** The new value: Yes / No for a switch, a number with its unit, or a text. */
function ValueInput({
  setting,
  value,
  error,
  onChange,
}: Readonly<{
  setting: SystemParameter;
  value: string;
  error: string | undefined;
  onChange: (value: string) => void;
}>) {
  if (setting.valueType === 'BOOLEAN') {
    const on = value.toLowerCase() === 'true';
    return (
      <fieldset className="field variant-options">
        <legend>New value</legend>
        <div className="row" style={{ gap: 'var(--space-4)' }}>
          <label className="checkbox">
            <input
              type="radio"
              name={`setting-${setting.key}`}
              checked={on}
              onChange={() => onChange('true')}
            />
            Yes
          </label>
          <label className="checkbox">
            <input
              type="radio"
              name={`setting-${setting.key}`}
              checked={!on}
              onChange={() => onChange('false')}
            />
            No
          </label>
        </div>
      </fieldset>
    );
  }
  const unit = settingUnit(setting.key);
  const number = setting.valueType === 'INTEGER';
  return (
    <Field label="New value" hint={rangeHint(setting)} error={error}>
      {(id) => (
        <div className="row" style={{ gap: 'var(--space-2)', alignItems: 'center' }}>
          <input
            id={id}
            className="input"
            inputMode={number ? 'numeric' : undefined}
            style={number ? { maxWidth: '10rem' } : undefined}
            maxLength={1000}
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
          {unit !== undefined && <span>{unit.many}</span>}
        </div>
      )}
    </Field>
  );
}

function ChangeDialog({
  setting,
  onClose,
}: Readonly<{ setting: SystemParameter; onClose: () => void }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [value, setValue] = useState(setting.value);
  const [tried, setTried] = useState(false);
  const label = settingLabel(setting.key);
  const invalid = settingValueError(setting, value);
  const save = useMutation({
    mutationFn: () => userAccessApi.changeSetting(setting.key, value.trim()),
    onSuccess: async () => {
      toast.success(`${label}: change sent for approval`);
      await queries.invalidateQueries({ queryKey: ['uam-settings'] });
      onClose();
    },
  });
  return (
    <Modal
      open
      title={label}
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
            onClick={() => {
              setTried(true);
              if (invalid === undefined) {
                save.mutate();
              }
            }}
          >
            Send for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} title="The change was not sent" />
        <p>{setting.description}</p>
        <DefinitionGrid
          label="Current setting"
          items={[
            { label: 'Area', value: settingGroup(setting.key) },
            { label: 'Current value', value: formatSettingValue(setting, setting.value) },
          ]}
        />
        <ValueInput
          setting={setting}
          value={value}
          error={tried ? invalid : undefined}
          onChange={setValue}
        />
        <Notice tone="info">The new value applies once another user approves it.</Notice>
        <p className="muted" style={{ fontSize: 12 }}>
          Parameter {setting.key}
        </p>
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
      toast.success(
        approve ? `${settingLabel(setting.key)}: change approved` : 'Change not applied',
      );
      await queries.invalidateQueries({ queryKey: ['uam-settings'] });
      await queries.invalidateQueries({ queryKey: ['session-policy'] });
      onClose();
    },
  });
  const reasonMissing = mayApprove && reason.trim() === '';
  return (
    <Modal
      open
      title={`${settingLabel(setting.key)}: Change to Approve`}
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
            { label: 'Setting', value: settingLabel(setting.key) },
            { label: 'Current value', value: formatSettingValue(setting, setting.value) },
            { label: 'Proposed value', value: formatSettingValue(setting, setting.pendingValue) },
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
  /** The buttons of a row: Change, or Approve / Reject for the checker, Withdraw for the maker. */
  const actionOf = (p: SystemParameter) => {
    const decide = () => setDeciding(p);
    let buttons: ReactNode = null;
    if (!pending(p)) {
      buttons = maker && (
        <Button size="sm" variant="secondary" onClick={() => setEditing(p)}>
          Change
        </Button>
      );
    } else if (checker && !mine(p)) {
      buttons = (
        <>
          <Button size="sm" variant="accent" onClick={decide}>
            Approve
          </Button>
          <Button size="sm" variant="secondary" onClick={decide}>
            Reject
          </Button>
        </>
      );
    } else if (maker && mine(p)) {
      buttons = (
        <Button size="sm" variant="secondary" onClick={decide}>
          Withdraw
        </Button>
      );
    }
    return (
      <span className="row" style={{ gap: 'var(--space-2)', flexWrap: 'nowrap' }}>
        {buttons}
      </span>
    );
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
          columns={[
            { key: 'g', header: 'Area', render: (p) => settingGroup(p.key) },
            {
              key: 'd',
              header: 'Setting',
              render: (p) => (
                <span className="row" style={{ gap: 'var(--space-1)', flexWrap: 'nowrap' }}>
                  {settingLabel(p.key)}
                  <InfoTip text={p.description} />
                </span>
              ),
            },
            {
              key: 'v',
              header: 'Value',
              render: (p) => (
                <CellStack
                  main={formatSettingValue(p, p.value)}
                  sub={
                    pending(p) ? `Proposed: ${formatSettingValue(p, p.pendingValue)}` : undefined
                  }
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
                    main={<StatusBadge status="PENDING_APPROVAL" label="Pending Approval" />}
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
            { key: 'a', header: 'Action', kind: 'actions', render: (p) => actionOf(p) },
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
