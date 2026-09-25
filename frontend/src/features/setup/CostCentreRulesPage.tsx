import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { accountingApi } from '@/api/accounting';
import { mastersApi } from '@/api/masters';
import { useAuth } from '@/auth/authContext';
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
import { formatDateTime } from '@/utils/format';
import { frbsSetupApi } from './frbsSetupApi';
import type { CostCenterRule, CostCenterRuleInput } from './frbsSetupApi';
import { ruleCriteria, ruleProblems } from './setupForms';

const blank = (companyId: number, priority: number): CostCenterRuleInput => ({
  companyId,
  priority,
  costCenter: '',
  active: true,
});

/**
 * Cost-centre rules of the accounting engine (FRBS 3.1.1, DIS 3.30.0): a generated line whose
 * account requires a cost centre and has none takes the cost centre of the first matching rule;
 * without one the posting is stopped and COST_CENTER_MISSING is raised.
 */
export default function CostCentreRulesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<CostCenterRule | null>(null);
  const [form, setForm] = useState<CostCenterRuleInput | null>(null);
  const [checked, setChecked] = useState(false);
  const maintainer = can('ACCOUNTING_RULE_MANAGE') || can('MASTER_MAINTAIN');

  const rules = useQuery({
    queryKey: ['cost-centre-rules', companyId],
    queryFn: () => frbsSetupApi.costCenterRules(companyId),
    enabled: companyId > 0,
  });
  const events = useQuery({ queryKey: ['event-types'], queryFn: accountingApi.eventTypes });
  const costCentres = useQuery({
    queryKey: ['dimensions', companyId, 'COST_CENTER'],
    queryFn: () => mastersApi.dimensions(companyId, 'COST_CENTER'),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (body: CostCenterRuleInput) =>
      editing === null ? frbsSetupApi.createRule(body) : frbsSetupApi.updateRule(editing.id, body),
    onSuccess: async (r) => {
      setForm(null);
      await queryClient.invalidateQueries({ queryKey: ['cost-centre-rules'] });
      toast.success(`Rule ${r.priority} saved`);
    },
  });
  const problems = form === null ? {} : ruleProblems(form);
  const open = (r: CostCenterRule | null) => {
    setEditing(r);
    setChecked(false);
    save.reset();
    const next = Math.max(0, ...(rules.data ?? []).map((x) => x.priority)) + 10;
    setForm(r === null ? blank(companyId, next) : { ...r, companyId });
  };
  const submit = () => {
    setChecked(true);
    if (form !== null && Object.keys(problems).length === 0) {
      save.mutate(form);
    }
  };
  const set = (patch: Partial<CostCenterRuleInput>) =>
    setForm((f) => (f === null ? f : { ...f, ...patch }));

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Cost-Centre Rules"
        description="Standard rules that give generated journal lines their cost centre, evaluated from the lowest priority number."
        actions={
          maintainer && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => open(null)}>
              Add Rule
            </Button>
          )
        }
      />
      <ErrorAlert error={rules.error ?? events.error} />
      <Card flush>
        <DataTable<CostCenterRule>
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          caption="Cost-centre rules"
          emptyMessage="No rule yet: lines without a cost centre are refused on accounts that require one"
          onRowClick={maintainer ? open : undefined}
          columns={[
            { key: 'p', header: 'Priority', numeric: true, render: (r) => r.priority },
            { key: 'c', header: 'Applies To', render: (r) => ruleCriteria(r) },
            { key: 'cc', header: 'Cost Centre', render: (r) => <strong>{r.costCenter}</strong> },
            { key: 'd', header: 'Description', render: (r) => r.description ?? '' },
            {
              key: 'st',
              header: 'Status',
              render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'u',
              header: 'Maintained',
              render: (r) => `${r.updatedBy} ${formatDateTime(r.updatedAt)}`,
            },
          ]}
        />
      </Card>
      <Modal
        title={editing === null ? 'Add cost-centre rule' : `Cost-centre rule ${editing.priority}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setForm(null)}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Save Rule
            </Button>
          </>
        }
      >
        {form !== null && (
          <div className="stack">
            <ErrorAlert error={save.error} />
            <p className="muted">Leave a criterion blank to match every posting.</p>
            <div className="form-grid">
              <Field label="Priority" required error={checked ? problems.priority : undefined}>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    type="number"
                    min={1}
                    max={9999}
                    value={form.priority}
                    onChange={(e) => set({ priority: Number(e.target.value) })}
                  />
                )}
              </Field>
              <Field label="Source module">
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    placeholder="e.g. DISBURSEMENT"
                    value={form.sourceModule ?? ''}
                    onChange={(e) => set({ sourceModule: e.target.value.toUpperCase() })}
                  />
                )}
              </Field>
              <Field label="Event type">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={form.eventType ?? ''}
                    onChange={(e) => set({ eventType: e.target.value || undefined })}
                  >
                    <option value="">Any event</option>
                    {(events.data ?? []).map((t) => (
                      <option key={t.code} value={t.code}>
                        {t.code} – {t.name}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="GL account">
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={form.accountCode ?? ''}
                    onChange={(e) => set({ accountCode: e.target.value.toUpperCase() })}
                  />
                )}
              </Field>
              <Field label="Party code">
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={form.partyCode ?? ''}
                    onChange={(e) => set({ partyCode: e.target.value.toUpperCase() })}
                  />
                )}
              </Field>
              <Field label="Cost centre" required error={checked ? problems.costCenter : undefined}>
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={form.costCenter}
                    onChange={(e) => set({ costCenter: e.target.value })}
                  >
                    <option value="">Select…</option>
                    {(costCentres.data ?? []).map((d) => (
                      <option key={d.code} value={d.code}>
                        {d.code} – {d.name}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field label="Description">
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={form.description ?? ''}
                    onChange={(e) => set({ description: e.target.value })}
                  />
                )}
              </Field>
              <Field label="Status">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={form.active ? 'Y' : 'N'}
                    onChange={(e) => set({ active: e.target.value === 'Y' })}
                  >
                    <option value="Y">Active</option>
                    <option value="N">Inactive</option>
                  </select>
                )}
              </Field>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
