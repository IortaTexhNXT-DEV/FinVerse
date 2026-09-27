import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import { collectionsApi } from './api';
import type { Criteria, ReassignInput, Rule, RuleInput } from './api';
import { ReassignDialog } from './WorkDialogs';
import './collections.css';

const CRITERIA_FIELDS: readonly { key: keyof Criteria; label: string; numeric?: boolean }[] = [
  { key: 'segment', label: 'Market Segment' },
  { key: 'salesUnit', label: 'Sales Unit' },
  { key: 'clientCode', label: 'Client Code' },
  { key: 'amountFrom', label: 'Outstanding From', numeric: true },
  { key: 'amountTo', label: 'Outstanding To', numeric: true },
  { key: 'agingFrom', label: 'Aging From (days)', numeric: true },
  { key: 'agingTo', label: 'Aging To (days)', numeric: true },
];

function range(label: string, from?: number, to?: number, unit = ''): string | undefined {
  if (from === undefined && to === undefined) {
    return undefined;
  }
  return `${label} ${from ?? 0}–${to ?? '∞'}${unit}`;
}

function describe(c: Criteria): string {
  const parts = [
    c.segment && `segment ${c.segment}`,
    c.salesUnit && `unit ${c.salesUnit}`,
    c.clientCode && `client ${c.clientCode}`,
    range('amount', c.amountFrom, c.amountTo),
    range('aging', c.agingFrom, c.agingTo, ' d'),
  ].filter(Boolean);
  return parts.length === 0 ? 'Every account' : parts.join(' · ');
}

function CriteriaFields({
  value,
  onChange,
}: Readonly<{ value: Criteria; onChange: (c: Criteria) => void }>) {
  return (
    <div className="form-grid">
      {CRITERIA_FIELDS.map((f) => (
        <Field key={f.key} label={f.label}>
          {(id) => (
            <input
              id={id}
              className="input"
              type={f.numeric === true ? 'number' : 'text'}
              value={value[f.key] ?? ''}
              onChange={(e) => {
                const raw = e.target.value;
                const typed = f.numeric === true ? Number(raw) : raw;
                const v = raw === '' ? undefined : typed;
                onChange({ ...value, [f.key]: v });
              }}
            />
          )}
        </Field>
      ))}
    </div>
  );
}

/** Adds or changes a default assignment rule (BRCLXN.052). */
function RuleDialog({
  rule,
  handlers,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  rule?: Rule;
  handlers: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: RuleInput) => void;
}>) {
  const [form, setForm] = useState<RuleInput>(
    rule === undefined
      ? { priority: 100, name: '', criteria: {}, handler: '' }
      : {
          priority: rule.priority,
          name: rule.name,
          criteria: rule.criteria,
          handler: rule.handler,
        },
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = () => {
    const found: Record<string, string> = {};
    if (form.name.trim() === '') {
      found.name = 'Give the rule a name';
    }
    if (form.handler === '') {
      found.handler = 'Choose the handler';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({ ...form, name: form.name.trim() });
    }
  };
  return (
    <Modal
      title={rule === undefined ? 'New Assignment Rule' : 'Change Assignment Rule'}
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
        <div className="form-grid">
          <Field label="Priority" required hint="Lower numbers are tried first">
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                min={1}
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: Number(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Name" required error={errors.name}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={120}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            )}
          </Field>
          <Field label="Handler" required error={errors.handler}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.handler}
                onChange={(e) => setForm({ ...form, handler: e.target.value })}
              >
                <option value="">Select…</option>
                {handlers.map((h) => (
                  <option key={h} value={h}>
                    {h}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <CriteriaFields
          value={form.criteria}
          onChange={(criteria) => setForm({ ...form, criteria })}
        />
      </div>
    </Modal>
  );
}

/** Reassignment by criteria with a preview of the affected accounts (BRCLXN.052). */
function ReassignByCriteria({ companyId }: Readonly<{ companyId: number }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [criteria, setCriteria] = useState<Criteria>({});
  const [confirming, setConfirming] = useState(false);
  const preview = useMutation({
    mutationFn: () => collectionsApi.preview(companyId, { criteria }),
  });
  const reassign = useMutation({
    mutationFn: (input: ReassignInput) =>
      collectionsApi.reassign(companyId, { ...input, criteria }),
    onSuccess: async (r) => {
      setConfirming(false);
      preview.reset();
      await queryClient.invalidateQueries({ queryKey: ['collections'] });
      const ref = r.bulkRef === undefined ? '' : ' (' + r.bulkRef + ')';
      toast.success(`${r.moved} account(s) reassigned${ref}`);
    },
  });
  return (
    <Card
      title="Reassign by Criteria"
      actions={
        <Button variant="secondary" busy={preview.isPending} onClick={() => preview.mutate()}>
          Preview Accounts
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={preview.error} />
        <Field label="Current Handler">
          {(id) => (
            <input
              id={id}
              className="input"
              value={criteria.handler ?? ''}
              onChange={(e) => setCriteria({ ...criteria, handler: e.target.value || undefined })}
            />
          )}
        </Field>
        <CriteriaFields value={criteria} onChange={setCriteria} />
        {preview.data !== undefined && (
          <>
            <p className="clx-muted">{preview.data.total} open account(s) match.</p>
            <DataTable
              caption="Accounts to reassign"
              columns={[
                { key: 'i', header: 'Invoice No.', render: (i) => i.invoiceNo },
                { key: 'a', header: 'Assured', render: (i) => i.assuredName },
                { key: 'h', header: 'Handler', render: (i) => i.currentHandler ?? 'Unassigned' },
                {
                  key: 'n',
                  header: 'Outstanding',
                  numeric: true,
                  render: (i) => formatAmount(i.netOutstanding),
                },
              ]}
              rows={preview.data.items.slice(0, 20)}
              rowKey={(i) => i.id}
            />
            <div className="clx-form-actions">
              <Button disabled={preview.data.total === 0} onClick={() => setConfirming(true)}>
                Reassign {preview.data.total} Account(s)
              </Button>
            </div>
          </>
        )}
      </div>
      {confirming && preview.data !== undefined && (
        <ReassignDialog
          total={preview.data.total}
          busy={reassign.isPending}
          error={reassign.error}
          onClose={() => setConfirming(false)}
          onSave={(input) => reassign.mutate(input)}
        />
      )}
    </Card>
  );
}

/**
 * Assignments (BRCLXN.052): the default assignment rules applied by the nightly refresh to new
 * accounts, and the reassignment of accounts by criteria - permanent or temporary with an end
 * date - after a preview.
 */
export default function AssignmentsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Rule | 'new'>();
  const rules = useQuery({
    queryKey: ['collections', 'rules', companyId],
    queryFn: () => collectionsApi.rules(companyId),
    enabled: companyId > 0,
  });
  const handlers = useQuery({
    queryKey: ['collections', 'handlers'],
    queryFn: collectionsApi.handlers,
  });
  const refresh = async (message: string) => {
    setEditing(undefined);
    await queryClient.invalidateQueries({ queryKey: ['collections', 'rules'] });
    toast.success(message);
  };
  const save = useMutation({
    mutationFn: (input: RuleInput) =>
      editing === 'new' || editing === undefined
        ? collectionsApi.createRule(companyId, input)
        : collectionsApi.updateRule(editing.id, input),
    onSuccess: (r) => refresh(`Rule ${r.name} saved`),
  });
  const activate = useMutation({
    mutationFn: (r: Rule) => collectionsApi.activateRule(r.id, !r.active),
    onSuccess: (r) => refresh(`Rule ${r.name} ${r.active ? 'activated' : 'deactivated'}`),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        backTo="/collections"
        title="Assignments"
        description="Who handles new accounts by default, and the reassignment of accounts to balance the workload."
        actions={
          <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
            New Rule
          </Button>
        }
      />
      <ErrorAlert error={rules.error ?? activate.error} />
      <Card title="Default Assignment Rules" flush>
        <DataTable
          caption="Assignment rules"
          columns={[
            { key: 'p', header: 'Priority', numeric: true, render: (r) => r.priority },
            { key: 'n', header: 'Rule', render: (r) => <strong>{r.name}</strong> },
            { key: 'c', header: 'Criteria', render: (r) => describe(r.criteria) },
            { key: 'h', header: 'Handler', render: (r) => r.handler },
            {
              key: 's',
              header: 'Status',
              render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'a',
              header: '',
              render: (r) => (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={(e) => {
                    e.stopPropagation();
                    activate.mutate(r);
                  }}
                >
                  {r.active ? 'Deactivate' : 'Activate'}
                </Button>
              ),
            },
          ]}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          loading={rules.isLoading}
          emptyMessage="No assignment rule: new accounts go to their account officer"
          onRowClick={(r) => setEditing(r)}
        />
      </Card>
      <ReassignByCriteria companyId={companyId} />
      {editing !== undefined && (
        <RuleDialog
          rule={editing === 'new' ? undefined : editing}
          handlers={handlers.data ?? []}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(input) => save.mutate(input)}
        />
      )}
    </div>
  );
}
