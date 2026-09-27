import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { RuleSetCreate, RuleSetView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { SBM_LOV } from '../common/submittedCodes';
import { STEPS } from './ruleCodes';
import { RuleSetEditor } from './RuleSetEditor';

function NewRuleSet({ onCreated }: Readonly<{ onCreated: (s: RuleSetView) => void }>) {
  const companyId = useCompanyId();
  const [form, setForm] = useState<RuleSetCreate>({
    companyId,
    code: '',
    step: 'DISPOSITION',
    segment: null,
    businessType: null,
    effectiveFrom: '',
    description: '',
  });
  const [touched, setTouched] = useState(false);
  const create = useMutation({
    mutationFn: () =>
      submittedApi.createRuleSet({ ...form, companyId, code: form.code.trim().toUpperCase() }),
    onSuccess: onCreated,
  });
  const missing = form.code.trim() === '' || form.effectiveFrom === '';
  return (
    <Card title="New Rule Set">
      <ErrorAlert error={create.error} />
      <div className="form-grid">
        <Field
          label="Code"
          required
          error={touched && form.code.trim() === '' ? 'Enter the code' : undefined}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.code}
              onBlur={() => setTouched(true)}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
            />
          )}
        </Field>
        <Field label="Step" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.step}
              onChange={(e) => setForm({ ...form, step: e.target.value })}
            >
              {Object.entries(STEPS).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Segment">
          {(id) => (
            <LovSelect
              id={id}
              type={SBM_LOV.segment}
              value={form.segment ?? ''}
              placeholder="All segments"
              onChange={(v) => setForm({ ...form, segment: v === '' ? null : v })}
            />
          )}
        </Field>
        <Field label="Business Type">
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.businessType ?? ''}
              onChange={(e) =>
                setForm({ ...form, businessType: e.target.value === '' ? null : e.target.value })
              }
            >
              <option value="">All</option>
              <option value="NB">New business</option>
              <option value="RB">Renewal business</option>
            </select>
          )}
        </Field>
        <Field
          label="Effective From"
          required
          error={touched && form.effectiveFrom === '' ? 'Enter the effective date' : undefined}
        >
          {(id) => (
            <DateInput
              id={id}
              value={form.effectiveFrom}
              onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
            />
          )}
        </Field>
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          )}
        </Field>
      </div>
      <div className="form-actions">
        <Button
          busy={create.isPending}
          onClick={() => {
            setTouched(true);
            if (!missing) {
              create.mutate();
            }
          }}
        >
          Create Draft
        </Button>
      </div>
    </Card>
  );
}

/** The rule sets of the processing steps, each opened in its editor (FR-SP-080). */
export function RuleSets() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<number>();
  const [creating, setCreating] = useState(false);
  const sets = useQuery({
    queryKey: ['submitted', 'rule-sets', companyId],
    queryFn: () => submittedApi.ruleSets(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <ErrorAlert error={sets.error} onRetry={() => void sets.refetch()} />
      <Card
        flush
        title="Rule Sets"
        actions={
          can('SBM_RULE_MAINTAIN') && (
            <Button variant="secondary" onClick={() => setCreating(true)}>
              New Rule Set
            </Button>
          )
        }
      >
        <DataTable<RuleSetView>
          loading={sets.isLoading}
          rows={sets.data ?? []}
          rowKey={(s) => s.id}
          selectedKey={open}
          onRowClick={(s) => setOpen(s.id)}
          emptyMessage="No rule set"
          columns={[
            { key: 'code', header: 'Rule Set', kind: 'code', render: (s) => s.code },
            { key: 'step', header: 'Step', render: (s) => STEPS[s.step] ?? s.step },
            {
              key: 'segment',
              header: 'Segment',
              render: (s) => <LovLabel type={SBM_LOV.segment} code={s.segment} empty="All" />,
            },
            { key: 'version', header: 'Version', kind: 'amount', render: (s) => s.versionNo },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (s) => <StatusBadge status={s.status} />,
            },
            {
              key: 'from',
              header: 'Effective From',
              kind: 'date',
              render: (s) => formatDate(s.effectiveFrom),
            },
            { key: 'maker', header: 'Maker', render: (s) => <UserName login={s.maker} /> },
          ]}
        />
      </Card>
      {creating && (
        <NewRuleSet
          onCreated={(s) => {
            setCreating(false);
            setOpen(s.id);
            void queryClient.invalidateQueries({ queryKey: ['submitted', 'rule-sets'] });
          }}
        />
      )}
      {open !== undefined && (
        <RuleSetEditor key={open} id={open} onClose={() => setOpen(undefined)} />
      )}
    </div>
  );
}
