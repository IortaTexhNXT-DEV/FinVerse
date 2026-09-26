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
import { formatDate, today } from '@/utils/format';
import { remittanceApi } from './api';
import type { IncentiveRule, IncentiveRuleInput } from './api';
import './remittance.css';

type Form = Omit<IncentiveRuleInput, 'companyId' | 'rate' | 'windowDays'> & {
  rate: string;
  windowDays: string;
};

const EMPTY: Form = {
  insurerCode: '',
  productLine: '',
  segment: '',
  rate: '',
  windowDays: '30',
  basis: 'INCEPTION',
  effectiveFrom: '',
  effectiveTo: '',
  active: true,
  description: '',
};

function formOf(rule: IncentiveRule | undefined): Form {
  if (rule === undefined) {
    return { ...EMPTY, effectiveFrom: today() };
  }
  return {
    ...EMPTY,
    ...rule,
    productLine: rule.productLine ?? '',
    segment: rule.segment ?? '',
    effectiveTo: rule.effectiveTo ?? '',
    description: rule.description ?? '',
    rate: String(rule.rate),
    windowDays: String(rule.windowDays),
  };
}

/** A blank text as undefined (optional fields of the request). */
function optional(value: string | undefined): string | undefined {
  const text = value?.trim();
  return text === undefined || text === '' ? undefined : text;
}

/** Field errors of the rule form. */
function errorsOf(f: Form): Partial<Record<keyof Form, string>> {
  const errors: Partial<Record<keyof Form, string>> = {};
  const rate = Number(f.rate);
  const days = Number(f.windowDays);
  if (f.insurerCode.trim() === '') {
    errors.insurerCode = 'Insurer code is required';
  }
  if (!(rate > 0 && rate <= 100)) {
    errors.rate = 'Rate must be above 0 and at most 100';
  }
  if (!Number.isInteger(days) || days < 1 || days > 366) {
    errors.windowDays = 'Window must be 1 to 366 days';
  }
  if (f.effectiveTo !== '' && f.effectiveTo !== undefined && f.effectiveTo < f.effectiveFrom) {
    errors.effectiveTo = 'The rule cannot end before it starts';
  }
  return errors;
}

function TextField({
  label,
  value,
  error,
  type = 'text',
  onChange,
}: Readonly<{
  label: string;
  value: string;
  error?: string;
  type?: string;
  onChange: (v: string) => void;
}>) {
  return (
    <Field label={label} error={error}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

function RuleDialog({
  rule,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  rule: IncentiveRule | undefined;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (form: Form) => void;
}>) {
  const [form, setForm] = useState<Form>(() => formOf(rule));
  const [errors, setErrors] = useState<Partial<Record<keyof Form, string>>>({});
  const set = (key: keyof Form, value: string | boolean) => setForm({ ...form, [key]: value });
  const save = () => {
    const found = errorsOf(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave(form);
    }
  };
  return (
    <Modal
      title={rule === undefined ? 'New Incentive Rule' : 'Edit Incentive Rule'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Save Rule
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="remit-form">
          <TextField
            label="Insurer Code"
            value={form.insurerCode}
            error={errors.insurerCode}
            onChange={(v) => set('insurerCode', v)}
          />
          <TextField
            label="Product Line (blank = all)"
            value={form.productLine ?? ''}
            onChange={(v) => set('productLine', v)}
          />
          <TextField
            label="Segment (blank = all)"
            value={form.segment ?? ''}
            onChange={(v) => set('segment', v)}
          />
          <TextField
            label="Rate (% of basic premium)"
            type="number"
            value={form.rate}
            error={errors.rate}
            onChange={(v) => set('rate', v)}
          />
          <TextField
            label="Window (days)"
            type="number"
            value={form.windowDays}
            error={errors.windowDays}
            onChange={(v) => set('windowDays', v)}
          />
          <Field label="Window Starts From">
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.basis}
                onChange={(e) => set('basis', e.target.value)}
              >
                <option value="INCEPTION">Inception date</option>
                <option value="BOOKING">Booking date</option>
              </select>
            )}
          </Field>
          <TextField
            label="Effective From"
            type="date"
            value={form.effectiveFrom}
            onChange={(v) => set('effectiveFrom', v)}
          />
          <TextField
            label="Effective To"
            type="date"
            value={form.effectiveTo ?? ''}
            error={errors.effectiveTo}
            onChange={(v) => set('effectiveTo', v)}
          />
        </div>
        <TextField
          label="Description"
          value={form.description ?? ''}
          onChange={(v) => set('description', v)}
        />
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => set('active', e.target.checked)}
          />{' '}
          Active
        </label>
      </div>
    </Modal>
  );
}

const COLUMNS: Column<IncentiveRule>[] = [
  { key: 'ins', header: 'Insurer', render: (r) => r.insurerCode },
  { key: 'line', header: 'Product Line', render: (r) => r.productLine ?? 'All' },
  { key: 'seg', header: 'Segment', render: (r) => r.segment ?? 'All' },
  { key: 'rate', header: 'Rate', numeric: true, render: (r) => `${r.rate}%` },
  {
    key: 'win',
    header: 'Window',
    render: (r) => `${r.windowDays} days from ${r.basis.toLowerCase()}`,
  },
  {
    key: 'from',
    header: 'Effective',
    render: (r) =>
      `${formatDate(r.effectiveFrom)} – ${r.effectiveTo ? formatDate(r.effectiveTo) : 'open'}`,
  },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

/**
 * Early-remittance incentive rules (RMTID.023, PRCID.028): the insurers' incentive for remitting
 * within a window after inception or booking, per product line and segment. BDOI has not given the
 * rates yet (OQ23); invoices matching an active rule go to With Incentives batches.
 */
export default function IncentiveRulesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<IncentiveRule | 'new'>();
  const rules = useQuery({
    queryKey: ['remittance', 'incentive-rules', companyId],
    queryFn: () => remittanceApi.incentiveRules(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (f: Form) =>
      remittanceApi.saveIncentiveRule(editing === 'new' ? undefined : editing?.id, {
        ...f,
        companyId,
        rate: Number(f.rate),
        windowDays: Number(f.windowDays),
        productLine: optional(f.productLine),
        segment: optional(f.segment),
        effectiveTo: optional(f.effectiveTo),
      }),
    onSuccess: async (r) => {
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'incentive-rules'] });
      toast.success(`Incentive rule of ${r.insurerCode} saved`);
    },
  });
  const manage = can('REMIT_APPROVE');
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Incentive Rules"
        description="Early remittance incentives per insurer, product line and segment (rates to be confirmed by BDOI)."
        actions={
          manage ? (
            <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
              New Rule
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rules.error} />
      <Card flush>
        <DataTable
          caption="Incentive rules"
          columns={COLUMNS}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          loading={rules.isLoading}
          onRowClick={manage ? setEditing : undefined}
        />
      </Card>
      {editing !== undefined && (
        <RuleDialog
          rule={editing === 'new' ? undefined : editing}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(f) => save.mutate(f)}
        />
      )}
    </div>
  );
}
