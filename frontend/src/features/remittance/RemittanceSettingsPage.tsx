import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, Pencil, X } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatDateTime } from '@/utils/format';
import { remittanceSettingsApi, settingsReady } from './settingsApi';
import type { RemittanceSettings, SettingChange } from './settingsApi';
import './remittance.css';

const YES_NO = [
  { value: 'true', label: 'Yes' },
  { value: 'false', label: 'No' },
];

function SettingsList({ values }: Readonly<{ values: RemittanceSettings }>) {
  return (
    <dl className="detail-list">
      <dt>Scheduled extraction</dt>
      <dd>{values.scheduleActive ? `On, at ${values.extractionTime} (Philippine time)` : 'Off'}</dd>
      <dt>Working days only</dt>
      <dd>{values.workingDaysOnly ? 'Yes' : 'No'}</dd>
      <dt>Folder, Normal - Peso</dt>
      <dd>{values.folderNormalPhp}</dd>
      <dt>Folder, Normal - Dollar</dt>
      <dd>{values.folderNormalUsd}</dd>
      <dt>Folder, With Incentives</dt>
      <dd>{values.folderWithIncentives}</dd>
      <dt>Excel file name</dt>
      <dd>{values.filePattern}.xls</dd>
      <dt>Text file prefix</dt>
      <dd>{values.textPrefix} + YYMMDD + insurer code + settlement number .txt</dd>
    </dl>
  );
}

function ChangeModal({
  companyId,
  current,
  onClose,
}: Readonly<{ companyId: number; current: RemittanceSettings; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RemittanceSettings>(current);
  const [reason, setReason] = useState('');
  const set = (patch: Partial<RemittanceSettings>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => remittanceSettingsApi.propose(companyId, form, reason),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'settings'] });
      toast.success('Change saved – waiting for the Remittance Team Leader');
      onClose();
    },
  });
  return (
    <Modal
      title="Change Remittance Settings"
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          disabled={!settingsReady(form) || reason.trim() === ''}
          onClick={() => save.mutate()}
        >
          Submit for Approval
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Scheduled extraction"
          value={String(form.scheduleActive)}
          options={YES_NO}
          onChange={(v) => set({ scheduleActive: v === 'true' })}
        />
        <TextInput
          label="Extraction time (HH:mm)"
          required
          value={form.extractionTime}
          onChange={(extractionTime) => set({ extractionTime })}
        />
        <SelectInput
          label="Working days only"
          value={String(form.workingDaysOnly)}
          options={YES_NO}
          onChange={(v) => set({ workingDaysOnly: v === 'true' })}
        />
        <TextInput
          label="Folder, Normal - Peso"
          required
          value={form.folderNormalPhp}
          onChange={(folderNormalPhp) => set({ folderNormalPhp })}
        />
        <TextInput
          label="Folder, Normal - Dollar"
          required
          value={form.folderNormalUsd}
          onChange={(folderNormalUsd) => set({ folderNormalUsd })}
        />
        <TextInput
          label="Folder, With Incentives"
          required
          value={form.folderWithIncentives}
          onChange={(folderWithIncentives) => set({ folderWithIncentives })}
        />
        <TextInput
          label="Excel file name"
          required
          hint="{date}, {insurer}, {settlement} and {type} are replaced; .xls is added"
          value={form.filePattern}
          onChange={(filePattern) => set({ filePattern })}
        />
        <TextInput
          label="Text file prefix"
          value={form.textPrefix}
          upper
          onChange={(textPrefix) => set({ textPrefix })}
        />
        <TextInput label="Reason" required value={reason} onChange={setReason} />
      </div>
    </Modal>
  );
}

function PendingChange({
  change,
  approver,
}: Readonly<{ change: SettingChange; approver: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const decide = useMutation({
    mutationFn: (approve: boolean) => remittanceSettingsApi.decide(change.id, approve),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'settings'] });
      toast.success(c.status === 'APPROVED' ? 'Settings approved' : 'Change rejected');
    },
  });
  return (
    <Card
      title="Change Waiting for Approval"
      actions={
        approver && (
          <>
            <Button
              size="sm"
              variant="secondary"
              icon={<X size={14} />}
              busy={decide.isPending}
              onClick={() => decide.mutate(false)}
            >
              Reject
            </Button>
            <Button
              size="sm"
              variant="primary"
              icon={<Check size={14} />}
              busy={decide.isPending}
              onClick={() => decide.mutate(true)}
            >
              Approve
            </Button>
          </>
        )
      }
    >
      <ErrorAlert error={decide.error} />
      <p>
        {change.reason} – proposed by <UserName login={change.requestedBy} /> on{' '}
        {formatDateTime(change.requestedAt)}
      </p>
      <SettingsList values={change.values} />
    </Card>
  );
}

const HISTORY: Column<SettingChange>[] = [
  { key: 'when', header: 'Proposed', render: (c) => formatDateTime(c.requestedAt) },
  { key: 'by', header: 'By', render: (c) => <UserName login={c.requestedBy} /> },
  { key: 'reason', header: 'Reason', render: (c) => c.reason },
  { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
  { key: 'decided', header: 'Decided by', render: (c) => <UserName login={c.decidedBy} /> },
  { key: 'on', header: 'Decided', render: (c) => formatDateTime(c.decidedAt) },
];

/**
 * Remittance Settings (FRS BRD-02 FRID.RMT.003): the extraction schedule, the shared folder of
 * each remittance type and the names of the extract files. A change takes effect when the
 * Remittance Team Leader approves it.
 */
export default function RemittanceSettingsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [editing, setEditing] = useState(false);
  const view = useQuery({
    queryKey: ['remittance', 'settings', companyId],
    queryFn: () => remittanceSettingsApi.get(companyId),
  });
  if (view.data === undefined) {
    return view.error ? <ErrorAlert error={view.error} /> : <LoadingPanel />;
  }
  const pending = view.data.changes.find((c) => c.status === 'PENDING');
  const maker = can('REMIT_SETTINGS_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Remittance Settings"
        description="Extraction schedule, shared folders and file naming; each change is approved by the Remittance Team Leader."
        actions={
          maker &&
          !pending && (
            <Button
              variant="secondary"
              icon={<Pencil size={14} />}
              onClick={() => setEditing(true)}
            >
              Change Settings
            </Button>
          )
        }
      />
      {pending && <PendingChange change={pending} approver={can('REMIT_SETTINGS_APPROVE')} />}
      <Card title="Settings in Force">
        <SettingsList values={view.data.current} />
      </Card>
      {view.data.changes.length === 0 && (
        <Notice>No change has been approved yet: the settings above are the defaults.</Notice>
      )}
      <Card title="Changes" flush>
        <DataTable
          caption="Changes of the Remittance Settings"
          columns={HISTORY}
          rows={view.data.changes}
          rowKey={(c) => c.id}
          emptyMessage="No change yet."
        />
      </Card>
      {editing && (
        <ChangeModal
          companyId={companyId}
          current={view.data.current}
          onClose={() => setEditing(false)}
        />
      )}
    </div>
  );
}
