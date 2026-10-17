import { useMutation } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { frbsApi } from './api';
import type { Measure, Schedule, ScheduleValues } from './api';
import { FAMILY_LABELS, MEASURE_LABELS, emptySchedule, scheduleErrors } from './schedules';

type Change = (next: Partial<ScheduleValues>) => void;

function Choice<T extends string>({
  label,
  value,
  options,
  onChange,
}: Readonly<{
  label: string;
  value: T;
  options: readonly (readonly [T, string])[];
  onChange: (value: T) => void;
}>) {
  return (
    <Field label={label} required>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(e.target.value as T)}
        >
          {options.map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

function Columns({
  values,
  error,
  onChange,
}: Readonly<{ values: ScheduleValues; error?: string; onChange: Change }>) {
  const used = new Set(values.columns.map((c) => c.measure));
  const free = (Object.keys(MEASURE_LABELS) as Measure[]).find((m) => !used.has(m));
  const set = (i: number, next: Partial<ScheduleValues['columns'][number]>) =>
    onChange({ columns: values.columns.map((c, j) => (j === i ? { ...c, ...next } : c)) });
  return (
    <Field label="Figures, in column order" required error={error}>
      {() => (
        <div className="frbs-columns">
          {values.columns.map((c, i) => (
            <div key={c.measure + String(i)} className="row">
              <select
                className="select"
                aria-label={`Figure ${String(i + 1)}`}
                value={c.measure}
                onChange={(e) => set(i, { measure: e.target.value as Measure })}
              >
                {(Object.keys(MEASURE_LABELS) as Measure[]).map((m) => (
                  <option key={m} value={m}>
                    {MEASURE_LABELS[m]}
                  </option>
                ))}
              </select>
              <input
                className="input"
                aria-label={`Heading ${String(i + 1)}`}
                maxLength={60}
                value={c.label}
                onChange={(e) => set(i, { label: e.target.value })}
              />
              <Button
                variant="ghost"
                size="sm"
                icon={<Trash2 size={14} />}
                onClick={() => onChange({ columns: values.columns.filter((_, j) => j !== i) })}
              >
                Remove
              </Button>
            </div>
          ))}
          {free && values.columns.length < 8 && (
            <Button
              variant="secondary"
              size="sm"
              icon={<Plus size={14} />}
              onClick={() =>
                onChange({
                  columns: [...values.columns, { measure: free, label: MEASURE_LABELS[free] }],
                })
              }
            >
              Add Figure
            </Button>
          )}
        </div>
      )}
    </Field>
  );
}

function TextField({
  label,
  value,
  error,
  hint,
  required = false,
  onChange,
}: Readonly<{
  label: string;
  value: string | undefined;
  error?: string;
  hint?: string;
  required?: boolean;
  onChange: (value: string) => void;
}>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * Adds or changes an account schedule definition (FRBS 3.2.0): the accounts, the rows, the side,
 * the basis, ageing, comparative period and commentary, and the figures in column order. Layouts
 * stay "to confirm" until BDOI confirms them (AQ05).
 */
export function ScheduleEditor({
  schedule,
  onSaved,
  onClose,
}: Readonly<{ schedule?: Schedule; onSaved: (s: Schedule) => void; onClose: () => void }>) {
  const [code, setCode] = useState(schedule?.code ?? '');
  const [values, setValues] = useState<ScheduleValues>(schedule?.values ?? emptySchedule());
  const [checked, setChecked] = useState(false);
  const change: Change = (next) => setValues((v) => ({ ...v, ...next }));
  const errors = checked ? scheduleErrors(code, values) : {};
  const save = useMutation({
    mutationFn: () =>
      schedule
        ? frbsApi.updateSchedule(schedule.code, values)
        : frbsApi.createSchedule(code.trim().toUpperCase(), values),
    onSuccess: onSaved,
  });
  const submit = () => {
    setChecked(true);
    if (Object.keys(scheduleErrors(code, values)).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      open
      title={schedule ? `Change Schedule ${schedule.code}` : 'New Account Schedule'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={submit}>
            {schedule ? 'Save Changes' : 'Add Schedule'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="frbs-form">
          {!schedule && (
            <TextField label="Code" required value={code} error={errors.code} onChange={setCode} />
          )}
          <TextField
            label="Title"
            required
            value={values.name}
            error={errors.name}
            onChange={(v) => change({ name: v })}
          />
          <Choice
            label="Family"
            value={values.family}
            options={Object.entries(FAMILY_LABELS) as [ScheduleValues['family'], string][]}
            onChange={(v) => change({ family: v })}
          />
          <TextField
            label="Source"
            value={values.sourceRef}
            hint="Appendix row or report list number"
            onChange={(v) => change({ sourceRef: v })}
          />
          <Choice
            label="Accounts By"
            value={values.selectorKind}
            options={[
              ['ACCOUNT_PREFIX', 'Account code prefixes'],
              ['REPORT_GROUP', 'Report groups'],
            ]}
            onChange={(v) => change({ selectorKind: v })}
          />
          <TextField
            label="Accounts"
            required
            value={values.accountSelector}
            error={errors.accountSelector}
            hint="Comma separated, e.g. 1210,1211"
            onChange={(v) => change({ accountSelector: v })}
          />
          <Choice
            label="Rows"
            value={values.grouping}
            options={[
              ['ACCOUNT', 'Account'],
              ['PARTY', 'Party'],
              ['DOCUMENT', 'Party and document'],
              ['COST_CENTER', 'Cost centre'],
              ['BRANCH', 'Branch'],
              ['BUSINESS_LINE', 'Line of business'],
            ]}
            onChange={(v) => change({ grouping: v })}
          />
          <TextField
            label="Currency"
            value={values.currency}
            error={errors.currency}
            hint="Blank: every currency in base currency"
            onChange={(v) => change({ currency: v })}
          />
          <Choice
            label="Positive Side"
            value={values.side}
            options={[
              ['DEBIT', 'Debit balances'],
              ['CREDIT', 'Credit balances'],
            ]}
            onChange={(v) => change({ side: v })}
          />
          <Choice
            label="Basis"
            value={values.basis}
            options={[
              ['BALANCE', 'Balance as of'],
              ['MOVEMENT', 'Movement of the period'],
            ]}
            onChange={(v) => change({ basis: v })}
          />
          <TextField
            label="Ageing Buckets"
            value={values.ageingSlots}
            error={errors.ageingSlots}
            hint="Days, e.g. 30,90,180,365,730"
            onChange={(v) => change({ ageingSlots: v })}
          />
          <Choice
            label="Comparative"
            value={values.comparative}
            options={[
              ['NONE', 'None'],
              ['PREVIOUS_MONTH', 'Previous month'],
              ['PREVIOUS_YEAR', 'Previous year'],
            ]}
            onChange={(v) => change({ comparative: v })}
          />
          <Choice
            label="Layout"
            value={values.layoutStatus}
            options={[
              ['TO_CONFIRM', 'To confirm with FRBS'],
              ['CONFIRMED', 'Confirmed'],
            ]}
            onChange={(v) => change({ layoutStatus: v })}
          />
          <label className="checkbox">
            <input
              type="checkbox"
              checked={values.commentary}
              onChange={(e) => change({ commentary: e.target.checked })}
            />{' '}
            Commentary column
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={values.boardDocument}
              onChange={(e) => change({ boardDocument: e.target.checked })}
            />{' '}
            Board document (Word requested)
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={values.active}
              onChange={(e) => change({ active: e.target.checked })}
            />{' '}
            Active
          </label>
          <div className="frbs-form-wide">
            <Columns values={values} error={errors.columns} onChange={change} />
          </div>
        </div>
      </div>
    </Modal>
  );
}
