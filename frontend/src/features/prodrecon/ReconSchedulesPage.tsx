import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { prodreconApi } from './prodreconApi';
import type { Frequency, ReconSchedule, ScheduleInput } from './prodreconApi';
import { MAX_MONTH_DAY, MAX_WEEK_DAY, scheduleForm, scheduleProblem } from './prodreconLogic';

function ScheduleDialog({
  companyId,
  schedule,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  companyId: number;
  schedule: ReconSchedule | undefined;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: ScheduleInput) => void;
}>) {
  const [form, setForm] = useState<ScheduleInput>(() => scheduleForm(companyId, schedule));
  const [problem, setProblem] = useState<string>();
  const set = (patch: Partial<ScheduleInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setProblem(undefined);
  };
  const save = () => {
    const p = scheduleProblem(form);
    if (p === undefined) {
      onSave({ ...form, insurerCode: form.insurerCode.trim().toUpperCase() });
    } else {
      setProblem(p);
    }
  };
  return (
    <Modal
      title={schedule ? `Edit Schedule of ${schedule.insurerCode}` : 'New Extract Schedule'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Save Schedule
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {problem && <p className="field-error">{problem}</p>}
        <div className="form-grid">
          <Field label="Insurer Code" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                disabled={schedule !== undefined}
                value={form.insurerCode}
                onChange={(e) => set({ insurerCode: e.target.value })}
              />
            )}
          </Field>
          <Field label="Frequency" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.frequency}
                onChange={(e) => set({ frequency: e.target.value as Frequency })}
              >
                <option value="MONTHLY">Monthly</option>
                <option value="WEEKLY">Weekly</option>
              </select>
            )}
          </Field>
          <Field
            label="Run Day"
            required
            hint={
              form.frequency === 'WEEKLY' ? '1 = Monday … 7 = Sunday' : 'Day of the month, 1-28'
            }
          >
            {(id) => (
              <input
                id={id}
                type="number"
                className="input"
                min={1}
                max={form.frequency === 'WEEKLY' ? MAX_WEEK_DAY : MAX_MONTH_DAY}
                value={form.runDay}
                onChange={(e) => set({ runDay: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Recipients" hint="Blank: the insurer's reconciliation contacts">
            {(id) => (
              <input
                id={id}
                className="input"
                value={form.recipients}
                onChange={(e) => set({ recipients: e.target.value })}
              />
            )}
          </Field>
        </div>
        <label className="row">
          <input
            type="checkbox"
            checked={form.autoSend}
            onChange={(e) => set({ autoSend: e.target.checked })}
          />{' '}
          Send the register automatically
        </label>
        <label className="row">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => set({ active: e.target.checked })}
          />{' '}
          Active
        </label>
      </div>
    </Modal>
  );
}

/**
 * Extract schedules (PRCID.001-003): when each insurer's production register is extracted by the
 * PRODUCTION_EXTRACT job, moved to the next working day on holidays, and whether it is sent.
 */
export default function ReconSchedulesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<{ schedule?: ReconSchedule }>();
  const schedules = useQuery({
    queryKey: ['prodrecon', 'schedules', companyId],
    queryFn: () => prodreconApi.schedules(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (input: ScheduleInput) =>
      editing?.schedule
        ? prodreconApi.updateSchedule(editing.schedule.id, input)
        : prodreconApi.createSchedule(input),
    onSuccess: async (s) => {
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: ['prodrecon', 'schedules'] });
      toast.success(`Schedule of ${s.insurerCode} saved`);
    },
  });
  const mayEdit = can('RECON_PROCESS');
  const columns: Column<ReconSchedule>[] = [
    { key: 'insurer', header: 'Insurer', render: (s) => <strong>{s.insurerCode}</strong> },
    {
      key: 'freq',
      header: 'Frequency',
      render: (s) => `${humanize(s.frequency)}, day ${String(s.runDay)}`,
    },
    { key: 'next', header: 'Next Run', render: (s) => formatDate(s.nextRunDate) },
    { key: 'send', header: 'Auto Send', render: (s) => (s.autoSend ? 'Yes' : 'No') },
    { key: 'to', header: 'Recipients', render: (s) => s.recipients ?? 'Insurer contacts' },
    {
      key: 'last',
      header: 'Last Run',
      render: (s) => `${formatDateTime(s.lastRunAt)} ${s.lastExtractNo ?? ''}`,
    },
    {
      key: 'status',
      header: 'Status',
      render: (s) => <StatusBadge status={s.active ? 'ACTIVE' : 'INACTIVE'} />,
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Production Reconciliation"
        title="Extract Schedules"
        description="When each insurer's production register is extracted and whether it is sent automatically."
        actions={
          mayEdit ? (
            <Button icon={<Plus size={16} />} onClick={() => setEditing({})}>
              New Schedule
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={schedules.error} />
      <Card>
        <DataTable
          caption="Extract schedules"
          columns={columns}
          rows={schedules.data ?? []}
          rowKey={(s) => s.id}
          loading={schedules.isLoading}
          onRowClick={mayEdit ? (s) => setEditing({ schedule: s }) : undefined}
          emptyMessage="No items to display"
        />
      </Card>
      {editing !== undefined && (
        <ScheduleDialog
          companyId={companyId}
          schedule={editing.schedule}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(input) => save.mutate(input)}
        />
      )}
    </div>
  );
}
