import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { nbReportsApi, UNIT_LEVELS } from '@/api/nbReports';
import type { SalesTarget, UnitLevel } from '@/api/nbReports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, humanize, today } from '@/utils/format';
import { emptyTarget, formOf, monthRange, targetErrors, toTarget } from './targetForm';
import type { TargetForm } from './targetForm';

const COLUMNS: Column<SalesTarget>[] = [
  { key: 'unit', header: 'Unit', render: (t) => t.unitCode },
  {
    key: 'period',
    header: 'Period',
    render: (t) => `${formatDate(t.periodFrom)} to ${formatDate(t.periodTo)}`,
  },
  { key: 'count', header: 'Bookings', numeric: true, render: (t) => t.targetCount },
  {
    key: 'premium',
    header: 'Premium (PHP)',
    numeric: true,
    render: (t) => formatAmount(t.targetPremium),
  },
  {
    key: 'commission',
    header: 'Commission (PHP)',
    numeric: true,
    render: (t) => formatAmount(t.targetCommission),
  },
];

const AMOUNT_FIELDS: {
  key: 'targetCount' | 'targetPremium' | 'targetCommission';
  label: string;
}[] = [
  { key: 'targetCount', label: 'Target Bookings' },
  { key: 'targetPremium', label: 'Target Premium (PHP)' },
  { key: 'targetCommission', label: 'Target Commission (PHP)' },
];

function TargetDialog({
  initial,
  onClose,
}: Readonly<{ initial: TargetForm; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState(initial);
  const [checked, setChecked] = useState(false);
  const errors = targetErrors(form);
  const save = useMutation({
    mutationFn: () => nbReportsApi.saveTarget(companyId, toTarget(form)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['nb-targets'] });
      toast.success('Target saved');
      onClose();
    },
  });
  const set = (patch: Partial<TargetForm>) => setForm((f) => ({ ...f, ...patch }));
  const submit = () => {
    setChecked(true);
    if (Object.keys(errors).length === 0) {
      save.mutate();
    }
  };
  const shown = (key: keyof TargetForm) => (checked ? errors[key] : undefined);
  return (
    <Modal
      open
      title={`${humanize(form.unitLevel)} Target`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={submit}>
            Save Target
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="Unit Code" required error={shown('unitCode')}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form.unitCode}
                placeholder={form.unitLevel === 'OFFICER' ? 'Username' : 'e.g. T-CBG1'}
                onChange={(e) => set({ unitCode: e.target.value })}
              />
            )}
          </Field>
          <Field label="Period From" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={form.periodFrom}
                onChange={(e) => set({ periodFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Period To" required error={shown('periodTo')}>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={form.periodTo}
                onChange={(e) => set({ periodTo: e.target.value })}
              />
            )}
          </Field>
          {AMOUNT_FIELDS.map((f) => (
            <Field key={f.key} label={f.label} required error={shown(f.key)}>
              {(id) => (
                <input
                  id={id}
                  inputMode="decimal"
                  className="input num"
                  value={form[f.key]}
                  onChange={(e) => set({ [f.key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
      </div>
    </Modal>
  );
}

/**
 * Production targets (BRNB.075): monthly booking, premium and commission targets per region,
 * department, team and account officer, compared with production on the dashboard and in the
 * Production Statistics report. Target values are open with BDOI (Q41).
 */
export default function SalesTargetsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [level, setLevel] = useState<UnitLevel>('TEAM');
  const [month, setMonth] = useState(today().slice(0, 7));
  const [editing, setEditing] = useState<TargetForm | null>(null);
  const range = monthRange(month);
  const targets = useQuery({
    queryKey: ['nb-targets', companyId, range.from, range.to],
    queryFn: () => nbReportsApi.targets(companyId, range.from, range.to),
    enabled: companyId > 0,
  });
  const maintain = can('MASTER_MAINTAIN');
  const rows = (targets.data ?? []).filter((t) => t.unitLevel === level);
  return (
    <div className="stack">
      <PageHeader
        section="Reports"
        title="Production Targets"
        description="Booking, premium and commission targets per sales unit and month (PHP), used by the NB dashboard and the Production Statistics report."
        actions={
          maintain && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setEditing(emptyTarget(level, month))}
            >
              Add Target
            </Button>
          )
        }
      />
      <Card flush>
        <Tabs
          tabs={UNIT_LEVELS.map((l) => ({ id: l, label: humanize(l) }))}
          active={level}
          onChange={setLevel}
        />
        <div className="worklist-toolbar">
          <Field label="Month">
            {(id) => (
              <input
                id={id}
                type="month"
                className="input"
                value={month}
                onChange={(e) => setMonth(e.target.value || today().slice(0, 7))}
              />
            )}
          </Field>
        </div>
        <ErrorAlert error={targets.error} />
        <DataTable
          caption="Production targets"
          rows={rows}
          rowKey={(t) => t.id ?? `${t.unitCode}-${t.periodFrom}`}
          columns={COLUMNS}
          loading={targets.isLoading}
          emptyMessage="No targets for this level and month"
          onRowClick={maintain ? (t) => setEditing(formOf(t)) : undefined}
        />
      </Card>
      {editing !== null && <TargetDialog initial={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}
