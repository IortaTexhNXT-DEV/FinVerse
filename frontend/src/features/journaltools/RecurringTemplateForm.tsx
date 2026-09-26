import type { Frequency } from '@/api/journalAutomation';
import type { ManualJournalType } from '@/api/gl';
import { Field } from '@/components/ui/Field';
import { useWorkspace } from '@/context/workspaceContext';
import { JournalLinesEditor } from '@/features/gl/JournalLinesEditor';
import { lineProblems } from '@/features/gl/journalMath';
import { humanize } from '@/utils/format';
import { FREQUENCIES } from './recurringModel';
import type { TemplateForm } from './recurringModel';

interface Props {
  value: TemplateForm;
  onChange: (value: TemplateForm) => void;
}

function HeaderFields({ value, onChange }: Readonly<Props>) {
  const { branches } = useWorkspace();
  const set = <K extends keyof TemplateForm>(key: K, v: TemplateForm[K]) =>
    onChange({ ...value, [key]: v });
  return (
    <div className="form-grid">
      <Field label="Template name" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={120}
            value={value.name}
            onChange={(e) => set('name', e.target.value)}
          />
        )}
      </Field>
      <Field label="Journal type" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.journalType}
            onChange={(e) => set('journalType', e.target.value as ManualJournalType)}
          >
            <option value="ACCRUAL">Accrual</option>
            <option value="MANUAL">Manual journal</option>
            <option value="ADJUSTMENT">Adjustment</option>
          </select>
        )}
      </Field>
      <Field label="Branch" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.branchId}
            onChange={(e) => set('branchId', Number(e.target.value))}
          >
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.code} – {b.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Currency" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={3}
            value={value.currency}
            onChange={(e) => set('currency', e.target.value.toUpperCase())}
          />
        )}
      </Field>
      <Field label="Narration" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={450}
            value={value.narration}
            onChange={(e) => set('narration', e.target.value)}
          />
        )}
      </Field>
      <Field label="Reference">
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={60}
            value={value.reference ?? ''}
            onChange={(e) => set('reference', e.target.value)}
          />
        )}
      </Field>
    </div>
  );
}

function ScheduleFields({ value, onChange }: Readonly<Props>) {
  const set = <K extends keyof TemplateForm>(key: K, v: TemplateForm[K]) =>
    onChange({ ...value, [key]: v });
  return (
    <div className="stack">
      <div className="form-grid">
        <Field label="Frequency" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={value.frequency}
              onChange={(e) => set('frequency', e.target.value as Frequency)}
            >
              {FREQUENCIES.map((f) => (
                <option key={f} value={f}>
                  {humanize(f)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Day of month" required hint="31 = last day of the month">
          {(id) => (
            <input
              id={id}
              className="input num"
              type="number"
              min={1}
              max={31}
              value={value.dayOfMonth}
              onChange={(e) => set('dayOfMonth', Number(e.target.value))}
            />
          )}
        </Field>
        <Field label="Start date" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={value.startDate}
              onChange={(e) => set('startDate', e.target.value)}
            />
          )}
        </Field>
        <Field label="End date" hint="Blank = no end">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={value.endDate ?? ''}
              onChange={(e) => set('endDate', e.target.value)}
            />
          )}
        </Field>
      </div>
      <div className="row">
        <label className="checkbox">
          <input
            type="checkbox"
            checked={value.autoReverse}
            onChange={(e) => set('autoReverse', e.target.checked)}
          />
          Auto-reverse on the first day of the next period (accruals)
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={value.autoSubmit}
            onChange={(e) => set('autoSubmit', e.target.checked)}
          />
          Submit generated journals for approval (otherwise they stay drafts)
        </label>
      </div>
    </div>
  );
}

/** Recurring journal template form: header, schedule and balanced lines. */
export function RecurringTemplateForm({ value, onChange }: Readonly<Props>) {
  return (
    <div className="stack">
      <HeaderFields value={value} onChange={onChange} />
      <ScheduleFields value={value} onChange={onChange} />
      <JournalLinesEditor
        lines={value.lines}
        problems={lineProblems(value.lines)}
        onChange={(lines) => onChange({ ...value, lines })}
      />
    </div>
  );
}
