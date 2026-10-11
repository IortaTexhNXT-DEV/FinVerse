import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { systemApi } from '@/api/system';
import type { ConfigEntry, SystemParameter } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { displayNameOf } from '@/api/users';
import { CellStack } from '@/components/ui/CellStack';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { ConfigUploadButton } from '@/features/configpromo/ConfigUploadButton';

type Tab = 'parameters' | 'configuration';

const TABS = [
  { id: 'parameters', label: 'Business parameters' },
  { id: 'configuration', label: 'Configuration (read only)' },
] as const;

function hintFor(p: SystemParameter): string {
  const range = p.minValue !== undefined ? ` between ${p.minValue} and ${p.maxValue ?? '…'}` : '';
  switch (p.valueType) {
    case 'INTEGER':
      return `Whole number${range}`;
    case 'INTEGER_LIST':
      return `Comma separated ascending numbers${range}`;
    case 'CODE_LIST':
      return 'Comma separated codes (blank allowed)';
    case 'BOOLEAN':
      return 'true or false';
    default:
      return humanize(p.valueType);
  }
}

function ConfigurationTable() {
  const config = useQuery({
    queryKey: ['system', 'configuration'],
    queryFn: systemApi.configuration,
  });
  return (
    <Card flush>
      <ErrorAlert error={config.error} />
      <DataTable<ConfigEntry>
        loading={config.isLoading}
        rows={config.data ?? []}
        rowKey={(c) => c.key}
        columns={[
          { key: 'k', header: 'Setting', render: (c) => <code>{c.key}</code> },
          { key: 'v', header: 'Value', render: (c) => c.value },
        ]}
      />
      <p className="muted" style={{ padding: 'var(--space-3) var(--space-4)' }}>
        Environment settings are changed through deployment configuration. Secrets are never shown.
      </p>
    </Card>
  );
}

/** A value as shown: a dash when blank. */
function shown(value: string | null | undefined): string {
  return value === undefined || value === null || value === '' ? '—' : value;
}

/** The change of a parameter that waits for approval, in words. */
function pendingText(p: SystemParameter): string {
  if (p.pendingValue === undefined || p.pendingValue === null) {
    return '';
  }
  return `Waiting for approval: ${shown(p.pendingValue)}, requested by ${displayNameOf(p.pendingBy)}`;
}

function PendingDialog({
  parameter,
  mayApprove,
  onClose,
}: Readonly<{ parameter: SystemParameter; mayApprove: boolean; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [tried, setTried] = useState(false);
  const reasonMissing = mayApprove && reason.trim() === '';
  const decide = useMutation({
    mutationFn: (approve: boolean) =>
      approve
        ? systemApi.approveParameter(parameter.key)
        : systemApi.rejectParameter(parameter.key, reason.trim() || undefined),
    onSuccess: async (p, approve) => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['system'] });
      await queryClient.invalidateQueries({ queryKey: ['session-policy'] });
      toast.success(approve ? `${p.description}: change approved` : 'Change rejected');
    },
  });
  return (
    <Modal
      open
      title="Security Setting to Approve"
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
        <ErrorAlert error={decide.error} title="Cannot decide the change" />
        <p>{parameter.description}</p>
        <DefinitionGrid
          label="Change to approve"
          items={[
            { label: 'Parameter', value: parameter.key },
            { label: 'Current value', value: parameter.value },
            { label: 'New value', value: parameter.pendingValue },
            { label: 'Requested by', value: <UserName login={parameter.pendingBy} /> },
            { label: 'Requested on', value: formatDateTime(parameter.pendingAt) },
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
 * System parameters: editable business parameters (validated by type, audited) and a read-only
 * view of the non-secret runtime configuration. A change of a security parameter waits until
 * another user approves it (SECURITY_PARAMETER_APPROVE).
 */
export default function SystemParametersPage() {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>('parameters');
  const [editing, setEditing] = useState<SystemParameter | null>(null);
  const [deciding, setDeciding] = useState<SystemParameter | null>(null);
  const parameters = useQuery({
    queryKey: ['system', 'parameters'],
    queryFn: systemApi.parameters,
  });
  const canEdit = can('SYSTEM_PARAMETER_MANAGE');
  const canApprove = can('SECURITY_PARAMETER_APPROVE');
  const mine = (p: SystemParameter) =>
    user !== null && (p.pendingBy ?? '').toLowerCase() === user.username.toLowerCase();
  const open = (p: SystemParameter) => {
    if (pendingText(p) !== '') {
      if ((canApprove && !mine(p)) || (canEdit && mine(p))) {
        setDeciding(p);
      }
    } else if (canEdit) {
      setEditing(p);
    }
  };

  const save = useMutation({
    mutationFn: (p: SystemParameter) => systemApi.updateParameter(p.key, p.value),
    onSuccess: async (p) => {
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ['system'] });
      await queryClient.invalidateQueries({ queryKey: ['session-policy'] });
      toast.success(
        pendingText(p) === ''
          ? `${p.description}: updated`
          : `${p.description}: change sent for approval`,
      );
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="System Parameters"
        description="Business parameters used across modules."
        actions={<ConfigUploadButton types={['CFG_SECURITY_PARAMETER', 'CFG_PM_LIST_PARAMETER']} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'configuration' && <ConfigurationTable />}
      {tab === 'parameters' && (
        <Card flush>
          <ErrorAlert error={parameters.error} />
          <DataTable<SystemParameter>
            loading={parameters.isLoading}
            rows={parameters.data ?? []}
            rowKey={(p) => p.key}
            onRowClick={canEdit || canApprove ? open : undefined}
            columns={[
              { key: 'c', header: 'Category', render: (p) => humanize(p.category) },
              { key: 'k', header: 'Parameter', render: (p) => <strong>{p.key}</strong> },
              {
                key: 'v',
                header: 'Value',
                render: (p) => (
                  <CellStack
                    main={p.value}
                    sub={pendingText(p) && `New value: ${shown(p.pendingValue)}`}
                  />
                ),
              },
              {
                key: 's',
                header: 'Status',
                kind: 'status',
                render: (p) =>
                  pendingText(p) ? (
                    <StatusBadge status="PENDING_APPROVAL" />
                  ) : (
                    <StatusBadge status="ACTIVE" label="In Force" />
                  ),
              },
              { key: 'd', header: 'Description', render: (p) => p.description },
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
      )}
      <Modal
        title={editing?.key ?? ''}
        open={editing !== null}
        onClose={() => setEditing(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => editing && save.mutate(editing)}
          >
            Save
          </Button>
        }
      >
        <ErrorAlert error={save.error} title="Cannot save the parameter" />
        {editing !== null && (
          <div className="stack">
            <p className="muted">{editing.description}</p>
            {editing.secondApproval && (
              <Notice tone="info" title="Security setting">
                The change applies once another user approves it.
              </Notice>
            )}
            <Field label="Value" hint={hintFor(editing)}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  maxLength={1000}
                  value={editing.value}
                  onChange={(e) => setEditing({ ...editing, value: e.target.value })}
                />
              )}
            </Field>
          </div>
        )}
      </Modal>
      {deciding !== null && (
        <PendingDialog
          parameter={deciding}
          mayApprove={canApprove && !mine(deciding)}
          onClose={() => setDeciding(null)}
        />
      )}
    </div>
  );
}
