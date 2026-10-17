import { useState } from 'react';
import type { RuleCondition, RuleInput, RuleOutcome } from '@/api/submitted';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { SBM_LOV } from '../common/submittedCodes';
import { FACT_LABELS, OPERATOR_LABELS, OUTCOME_CHOICES, ruleProblems } from './ruleCodes';

type Choice = keyof typeof OUTCOME_CHOICES;

const CHOICE_LABELS: Record<Choice, string> = {
  tag: 'Renewal Tag',
  classification: 'Classification',
  raTemplate: 'RA Template',
  flag: 'Flag',
};

function ConditionRow({
  c,
  facts,
  operators,
  onChange,
  onRemove,
}: Readonly<{
  c: RuleCondition;
  facts: readonly string[];
  operators: readonly string[];
  onChange: (c: RuleCondition) => void;
  onRemove: () => void;
}>) {
  const valueless = c.operator === 'EMPTY' || c.operator === 'NOT_EMPTY';
  return (
    <div className="row">
      <select
        className="select"
        aria-label="Fact"
        value={c.field}
        onChange={(e) => onChange({ ...c, field: e.target.value })}
      >
        {facts.map((f) => (
          <option key={f} value={f}>
            {FACT_LABELS[f] ?? f}
          </option>
        ))}
      </select>
      <select
        className="select"
        aria-label="Operator"
        value={c.operator}
        onChange={(e) => onChange({ ...c, operator: e.target.value })}
      >
        {operators.map((o) => (
          <option key={o} value={o}>
            {OPERATOR_LABELS[o] ?? o}
          </option>
        ))}
      </select>
      {!valueless && (
        <input
          className="input"
          aria-label="Value"
          placeholder="Value, or values separated by commas"
          value={c.value ?? ''}
          onChange={(e) => onChange({ ...c, value: e.target.value })}
        />
      )}
      <Button variant="ghost" onClick={onRemove}>
        Remove
      </Button>
    </div>
  );
}

function OutcomeFields({
  outcome,
  onChange,
}: Readonly<{ outcome: RuleOutcome; onChange: (o: RuleOutcome) => void }>) {
  return (
    <>
      <Field label="Bucket">
        {(id) => (
          <LovSelect
            id={id}
            type={SBM_LOV.bucket}
            value={outcome.bucket ?? ''}
            placeholder="No bucket"
            onChange={(v) => onChange({ ...outcome, bucket: v === '' ? null : v })}
          />
        )}
      </Field>
      {(Object.keys(OUTCOME_CHOICES) as Choice[]).map((key) => (
        <Field key={key} label={CHOICE_LABELS[key]}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={outcome[key] ?? ''}
              onChange={(e) =>
                onChange({ ...outcome, [key]: e.target.value === '' ? null : e.target.value })
              }
            >
              <option value="">None</option>
              {Object.entries(OUTCOME_CHOICES[key]).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </select>
          )}
        </Field>
      ))}
    </>
  );
}

/** A rule of a draft rule set: priority, name, conditions, outcome, reason and stop flag. */
export function RuleForm({
  title,
  initial,
  facts,
  operators,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<{
  title: string;
  initial: RuleInput;
  facts: readonly string[];
  operators: readonly string[];
  busy: boolean;
  error: unknown;
  onSave: (rule: RuleInput) => void;
  onClose: () => void;
}>) {
  const [rule, setRule] = useState<RuleInput>(initial);
  const [touched, setTouched] = useState(false);
  const problems = ruleProblems(rule);
  const shown = touched ? problems : {};
  const setCondition = (i: number, c: RuleCondition) =>
    setRule({ ...rule, conditions: rule.conditions.map((x, j) => (j === i ? c : x)) });
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() => {
              setTouched(true);
              if (Object.keys(problems).length === 0) {
                onSave(rule);
              }
            }}
          >
            Save Rule
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <div className="form-grid">
        <Field
          label="Priority"
          required
          error={shown.priority}
          hint="Higher priorities are tried first"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              inputMode="numeric"
              value={String(rule.priority)}
              onChange={(e) => setRule({ ...rule, priority: Number(e.target.value) })}
              onBlur={() => setTouched(true)}
            />
          )}
        </Field>
        <Field label="Name" required error={shown.name}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={rule.name}
              onChange={(e) => setRule({ ...rule, name: e.target.value })}
              onBlur={() => setTouched(true)}
            />
          )}
        </Field>
      </div>
      <Field label="Conditions (all must hold)" required error={shown.conditions}>
        {() => (
          <div className="stack">
            {rule.conditions.map((c, i) => (
              <ConditionRow
                key={i}
                c={c}
                facts={facts}
                operators={operators}
                onChange={(n) => setCondition(i, n)}
                onRemove={() =>
                  setRule({ ...rule, conditions: rule.conditions.filter((_, j) => j !== i) })
                }
              />
            ))}
            <Button
              variant="secondary"
              onClick={() =>
                setRule({
                  ...rule,
                  conditions: [...rule.conditions, { field: 'segment', operator: 'EQ', value: '' }],
                })
              }
            >
              Add Condition
            </Button>
          </div>
        )}
      </Field>
      <div className="form-grid">
        <OutcomeFields
          outcome={rule.outcome}
          onChange={(outcome) => setRule({ ...rule, outcome })}
        />
        <Field label="Reason" error={shown.outcome}>
          {(id) => (
            <LovSelect
              id={id}
              type={SBM_LOV.reason}
              value={rule.reasonCode ?? ''}
              placeholder="No reason"
              onChange={(v) => setRule({ ...rule, reasonCode: v === '' ? null : v })}
            />
          )}
        </Field>
        <Field label="Stop at This Rule">
          {(id) => (
            <input
              id={id}
              type="checkbox"
              checked={rule.stop}
              onChange={(e) => setRule({ ...rule, stop: e.target.checked })}
            />
          )}
        </Field>
        <Field label="Active">
          {(id) => (
            <input
              id={id}
              type="checkbox"
              checked={rule.active}
              onChange={(e) => setRule({ ...rule, active: e.target.checked })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
