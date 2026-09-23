import type { ManualJournalType } from '@/api/gl';
import { Field } from '@/components/ui/Field';
import { useWorkspace } from '@/context/workspaceContext';
import type { JournalHeaderValues } from './journalForm';

interface Props {
  value: JournalHeaderValues;
  onChange: (value: JournalHeaderValues) => void;
}

/** Voucher header inputs: type, branch, value date, currency, reference and narration. */
export function JournalHeaderFields({ value, onChange }: Readonly<Props>) {
  const { branches } = useWorkspace();
  const set = <K extends keyof JournalHeaderValues>(key: K, v: JournalHeaderValues[K]) =>
    onChange({ ...value, [key]: v });

  return (
    <div className="stack">
      <div className="form-grid">
        <Field label="Journal type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={value.journalType}
              onChange={(e) => set('journalType', e.target.value as ManualJournalType)}
            >
              <option value="MANUAL">Manual journal</option>
              <option value="ADJUSTMENT">Adjustment</option>
              <option value="ACCRUAL">Accrual</option>
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
        <Field label="Value date" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={value.valueDate}
              onChange={(e) => set('valueDate', e.target.value)}
            />
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
        <Field label="Reference">
          {(id) => (
            <input
              id={id}
              className="input"
              value={value.reference}
              onChange={(e) => set('reference', e.target.value)}
            />
          )}
        </Field>
      </div>
      <Field label="Narration" required>
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            maxLength={500}
            value={value.narration}
            onChange={(e) => set('narration', e.target.value)}
          />
        )}
      </Field>
    </div>
  );
}
