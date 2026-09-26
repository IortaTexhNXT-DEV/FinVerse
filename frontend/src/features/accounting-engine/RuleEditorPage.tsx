import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FlaskConical, Save } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { accountingApi } from '@/api/accounting';
import type { Rule, RuleInput } from '@/api/accounting';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { RuleHeaderFields } from './RuleHeaderFields';
import { RuleLinesEditor } from './RuleLinesEditor';
import { components, newRule, roles, toRuleInput, validateRule } from './ruleModel';
import { useAccountingLookups } from './useAccountingLookups';

interface EditorProps {
  existing: Rule | undefined;
  initial: RuleInput;
}

function RuleEditor({ existing, initial }: Readonly<EditorProps>) {
  const { eventTypes } = useAccountingLookups();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [rule, setRule] = useState<RuleInput>(initial);
  const [touched, setTouched] = useState(false);
  const readOnly = !can('ACCOUNTING_RULE_MANAGE');
  const eventType = eventTypes.find((t) => t.code === rule.eventType);
  const errors = touched ? validateRule(rule, eventType) : [];
  const save = useMutation({
    mutationFn: (r: RuleInput) =>
      existing === undefined
        ? accountingApi.createRule(r)
        : accountingApi.updateRule(existing.id, r),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['accounting-rules'] });
      toast.success(`Rule ${r.name} saved – pending authorization`);
      void navigate('/accounting/rules');
    },
  });
  const submit = () => {
    setTouched(true);
    if (validateRule(rule, eventType).length === 0) {
      save.mutate(rule);
    }
  };
  const roleNames = roles(rule.lines);

  return (
    <div className="stack">
      <PageHeader
        section="Accounting Engine"
        title={existing === undefined ? 'New accounting rule' : existing.name}
        description="Conditions select the rule for an event; the lines give the Dr/Cr account and the amount component posted on each side."
        actions={
          <>
            {existing !== undefined && <StatusBadge status={existing.recordStatus} />}
            <Button
              variant="secondary"
              icon={<FlaskConical size={16} />}
              onClick={() => void navigate(`/accounting/simulator?eventType=${rule.eventType}`)}
            >
              Simulate
            </Button>
            {!readOnly && (
              <Button
                variant="accent"
                icon={<Save size={16} />}
                busy={save.isPending}
                onClick={submit}
              >
                Save for Authorization
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={save.error} />
      {errors.length > 0 && (
        <div className="alert danger" role="alert">
          <ul>
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </div>
      )}
      <Card title="Rule and conditions">
        <RuleHeaderFields
          rule={rule}
          eventTypes={eventTypes}
          lockEventType={existing !== undefined}
          readOnly={readOnly}
          onChange={setRule}
        />
      </Card>
      <Card title="Posting lines">
        <RuleLinesEditor
          lines={rule.lines}
          components={components(eventType)}
          readOnly={readOnly}
          onChange={(lines) => setRule({ ...rule, lines })}
        />
        {roleNames.length > 0 && (
          <p className="muted">
            The publishing module must supply accounts for: {roleNames.join(', ')}.
          </p>
        )}
      </Card>
    </div>
  );
}

/** Create or edit an accounting rule (route /accounting/rules/new or /accounting/rules/:id). */
export default function RuleEditorPage() {
  const { id } = useParams();
  const { rules, eventTypes, companyId, loading, error } = useAccountingLookups();
  if (loading || companyId === 0) {
    return <span className="spinner" aria-label="Loading" />;
  }
  const existing =
    id === undefined || id === 'new' ? undefined : rules.find((r) => r.id === Number(id));
  if (id !== undefined && id !== 'new' && existing === undefined) {
    return <ErrorAlert error={error ?? new Error(`Rule ${id} not found`)} />;
  }
  const initial =
    existing === undefined ? newRule(companyId, eventTypes[0], today()) : toRuleInput(existing);
  return <RuleEditor key={id ?? 'new'} existing={existing} initial={initial} />;
}
