import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { RuleInput, RuleSetView, RuleView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDate, formatDateTime } from '@/utils/format';
import { SBM_LOV } from '../common/submittedCodes';
import { FACT_LABELS, OPERATOR_LABELS, OUTCOME_CHOICES, STEPS, emptyRule } from './ruleCodes';
import { RuleForm } from './RuleForm';

const PRIORITY_STEP = 10;

const conditionText = (r: RuleView) =>
  r.conditions
    .map((c) =>
      `${FACT_LABELS[c.field] ?? c.field} ${OPERATOR_LABELS[c.operator] ?? c.operator} ${c.value ?? ''}`.trim(),
    )
    .join(' and ');

function outcomeText(r: RuleView): string {
  const o = r.outcome;
  const parts = [
    o.tag ? OUTCOME_CHOICES.tag[o.tag as keyof typeof OUTCOME_CHOICES.tag] : null,
    o.classification
      ? OUTCOME_CHOICES.classification[
          o.classification as keyof typeof OUTCOME_CHOICES.classification
        ]
      : null,
    o.raTemplate
      ? `RA ${OUTCOME_CHOICES.raTemplate[o.raTemplate as keyof typeof OUTCOME_CHOICES.raTemplate]}`
      : null,
    o.flag ? OUTCOME_CHOICES.flag[o.flag as keyof typeof OUTCOME_CHOICES.flag] : null,
  ].filter((p): p is string => typeof p === 'string');
  return parts.join(', ');
}

function Header({ set, onDone }: Readonly<{ set: RuleSetView; onDone: () => void }>) {
  const [from, setFrom] = useState(set.effectiveFrom);
  const [description, setDescription] = useState(set.description ?? '');
  const save = useMutation({
    mutationFn: () => submittedApi.describeRuleSet(set.id, from, description),
    onSuccess: onDone,
  });
  return (
    <div className="stack">
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Effective From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          )}
        </Field>
      </div>
      <div className="form-actions">
        <Button variant="secondary" busy={save.isPending} onClick={() => save.mutate()}>
          Save Header
        </Button>
      </div>
    </div>
  );
}

/** The decisions on a rule set: submit (maker), approve and reject (checker), new version. */
function Decisions({ set, onDone }: Readonly<{ set: RuleSetView; onDone: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const [from, setFrom] = useState('');
  const decide = (d: 'submit' | 'approve' | 'reject', remarks?: string) =>
    submittedApi.ruleSetDecision(set.id, d, remarks).then((s) => {
      toast.success(`${s.code} version ${String(s.versionNo)}: ${s.status.toLowerCase()}`);
      onDone();
    });
  const version = useMutation({
    mutationFn: () => submittedApi.newVersion(set.id, from),
    onSuccess: onDone,
  });
  const checker = can('SBM_RULE_APPROVE') && user?.username !== set.maker;
  const label = `${set.code} version ${String(set.versionNo)}`;
  return (
    <span className="form-actions">
      <ErrorAlert error={version.error} />
      {can('SBM_RULE_MAINTAIN') && set.status === 'DRAFT' && (
        <ConfirmButton
          confirm={{
            title: `Submit ${label}`,
            effect: 'The rule set goes to a checker; it can no longer be changed.',
          }}
          onConfirm={() => decide('submit')}
        >
          Submit
        </ConfirmButton>
      )}
      {checker && set.status === 'SUBMITTED' && (
        <>
          <ConfirmButton
            confirm={{
              title: `Approve ${label}`,
              effect:
                'The rule set becomes active from its effective date and replaces the active version.',
              reason: 'optional',
            }}
            onConfirm={(remarks) => decide('approve', remarks)}
          >
            Approve
          </ConfirmButton>
          <ConfirmButton
            variant="secondary"
            confirm={{
              title: `Reject ${label}`,
              effect: 'The rule set goes back to its maker, who can change it and submit it again.',
              reason: 'required',
              destructive: true,
            }}
            onConfirm={(remarks) => decide('reject', remarks)}
          >
            Reject
          </ConfirmButton>
        </>
      )}
      {can('SBM_RULE_MAINTAIN') && set.status === 'ACTIVE' && (
        <>
          <DateInput
            aria-label="Effective from of the new version"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <Button
            variant="secondary"
            disabled={from === ''}
            busy={version.isPending}
            onClick={() => version.mutate()}
          >
            New Version
          </Button>
        </>
      )}
    </span>
  );
}

/**
 * A rule set with its rules (FR-SP-080): the maker changes a draft (header, rules added, changed
 * or removed) and submits it; a checker who is not the maker approves or rejects it with a reason.
 */
export function RuleSetEditor({ id, onClose }: Readonly<{ id: number; onClose: () => void }>) {
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<{ ruleId: number | null; rule: RuleInput } | null>(null);
  const set = useQuery({
    queryKey: ['submitted', 'rule-set', id],
    queryFn: () => submittedApi.ruleSet(id),
  });
  const vocabulary = useQuery({
    queryKey: ['submitted', 'vocabulary'],
    queryFn: () => submittedApi.vocabulary(),
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const save = useMutation({
    mutationFn: (rule: RuleInput) => {
      const ruleId = editing?.ruleId ?? null;
      return ruleId === null
        ? submittedApi.addRule(id, rule)
        : submittedApi.changeRule(ruleId, rule);
    },
    onSuccess: () => {
      setEditing(null);
      refresh();
    },
  });
  const s = set.data;
  if (!s) {
    return <ErrorAlert error={set.error} />;
  }
  const draft = s.status === 'DRAFT' && can('SBM_RULE_MAINTAIN');
  const next = Math.max(0, ...s.rules.map((r) => r.priority)) + PRIORITY_STEP;
  return (
    <Card
      title={`${s.code} version ${String(s.versionNo)}`}
      actions={
        <span className="form-actions">
          <Decisions set={s} onDone={refresh} />
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
        </span>
      }
    >
      <SetFacts set={s} />
      {draft && <Header set={s} onDone={refresh} />}
      <RulesTable
        rules={s.rules}
        draft={draft}
        onEdit={(r) => setEditing({ ruleId: r.id, rule: { ...r } })}
        onDone={refresh}
      />
      {draft && (
        <div className="form-actions">
          <Button onClick={() => setEditing({ ruleId: null, rule: emptyRule(next) })}>
            Add Rule
          </Button>
        </div>
      )}
      {editing && (
        <RuleForm
          title={editing.ruleId === null ? 'New Rule' : 'Change Rule'}
          initial={editing.rule}
          facts={vocabulary.data?.facts ?? Object.keys(FACT_LABELS)}
          operators={vocabulary.data?.operators ?? Object.keys(OPERATOR_LABELS)}
          busy={save.isPending}
          error={save.error}
          onSave={(rule) => save.mutate(rule)}
          onClose={() => setEditing(null)}
        />
      )}
    </Card>
  );
}

function SetFacts({ set: s }: Readonly<{ set: RuleSetView }>) {
  return (
    <>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Step', value: STEPS[s.step] ?? s.step },
          { label: 'Status', value: <StatusBadge status={s.status} /> },
          {
            label: 'Segment',
            value: <LovLabel type={SBM_LOV.segment} code={s.segment} empty="All" />,
          },
          { label: 'Business Type', value: s.businessType ?? 'All' },
          { label: 'Effective From', value: formatDate(s.effectiveFrom) },
          { label: 'Maker', value: <UserName login={s.maker} /> },
          { label: 'Submitted', value: formatDateTime(s.submittedAt) },
          { label: 'Approved By', value: s.approvedBy ? <UserName login={s.approvedBy} /> : null },
        ]}
      />
      {s.decisionRemarks && (
        <Notice tone={s.status === 'REJECTED' ? 'warning' : 'info'} title="Checker's remarks">
          {s.decisionRemarks}
        </Notice>
      )}
    </>
  );
}

function RulesTable({
  rules,
  draft,
  onEdit,
  onDone,
}: Readonly<{
  rules: RuleView[];
  draft: boolean;
  onEdit: (r: RuleView) => void;
  onDone: () => void;
}>) {
  return (
    <DataTable<RuleView>
      rows={rules}
      rowKey={(r) => r.id}
      emptyMessage="No rule yet: a record no rule places falls out"
      columns={[
        { key: 'priority', header: 'Priority', kind: 'amount', render: (r) => r.priority },
        { key: 'name', header: 'Rule', render: (r) => r.name },
        { key: 'when', header: 'When', render: conditionText },
        {
          key: 'bucket',
          header: 'Bucket',
          render: (r) => <LovLabel type={SBM_LOV.bucket} code={r.outcome.bucket} />,
        },
        { key: 'then', header: 'Outcome', render: (r) => outcomeText(r) || '—' },
        {
          key: 'reason',
          header: 'Reason',
          render: (r) => <LovLabel type={SBM_LOV.reason} code={r.reasonCode} />,
        },
        { key: 'stop', header: 'Stop', render: (r) => (r.stop ? 'Yes' : 'No') },
        { key: 'active', header: 'Active', render: (r) => (r.active ? 'Yes' : 'No') },
        {
          key: 'actions',
          header: 'Actions',
          render: (r) =>
            draft ? <RuleActions rule={r} onEdit={() => onEdit(r)} onDone={onDone} /> : null,
        },
      ]}
    />
  );
}

function RuleActions({
  rule,
  onEdit,
  onDone,
}: Readonly<{ rule: RuleView; onEdit: () => void; onDone: () => void }>) {
  return (
    <span className="form-actions">
      <Button variant="ghost" onClick={onEdit}>
        Change
      </Button>
      <ConfirmButton
        variant="ghost"
        confirm={{
          title: `Remove Rule ${rule.name}`,
          effect: 'The rule is removed from this draft.',
          destructive: true,
        }}
        onConfirm={() => submittedApi.removeRule(rule.id).then(onDone)}
      >
        Remove
      </ConfirmButton>
    </span>
  );
}
