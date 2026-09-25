import type { BankAccount } from '@/api/receivables';
import { Field } from '@/components/ui/Field';
import type { StatementLayout } from './frbsSetupApi';
import type { FieldErrors } from './setupForms';

type TextKey =
  | 'name'
  | 'dateColumn'
  | 'descriptionColumn'
  | 'referenceColumn'
  | 'debitColumn'
  | 'creditColumn'
  | 'amountColumn'
  | 'balanceColumn'
  | 'datePattern';

const TEXT_FIELDS: { key: TextKey; label: string; hint?: string }[] = [
  { key: 'name', label: 'Layout name' },
  { key: 'dateColumn', label: 'Date column' },
  { key: 'datePattern', label: 'Date pattern', hint: 'e.g. MM/dd/yyyy or yyyy-MM-dd' },
  { key: 'descriptionColumn', label: 'Description column' },
  { key: 'referenceColumn', label: 'Cheque / reference column' },
  { key: 'amountColumn', label: 'Signed amount column', hint: 'Deposits positive' },
  { key: 'debitColumn', label: 'Withdrawals column', hint: 'When there is no signed amount' },
  { key: 'creditColumn', label: 'Deposits column', hint: 'When there is no signed amount' },
  { key: 'balanceColumn', label: 'Running balance column' },
];

interface Props {
  form: StatementLayout;
  banks: BankAccount[];
  errors: FieldErrors<StatementLayout>;
  onChange: (patch: Partial<StatementLayout>) => void;
}

/** Inputs of a statement layout: bank account, column names and the cheque matching rule. */
export function LayoutFields({ form, banks, errors, onChange }: Readonly<Props>) {
  return (
    <div className="stack">
      <div className="form-grid">
        <Field label="Bank account" required error={errors.bankAccountCode}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.bankAccountCode}
              onChange={(e) => onChange({ bankAccountCode: e.target.value })}
            >
              <option value="">Select…</option>
              {banks.map((b) => (
                <option key={b.code} value={b.code}>
                  {b.code} – {b.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        {TEXT_FIELDS.map((f) => (
          <Field key={f.key} label={f.label} hint={f.hint} error={errors[f.key]}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form[f.key] ?? ''}
                onChange={(e) => onChange({ [f.key]: e.target.value })}
              />
            )}
          </Field>
        ))}
      </div>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={form.chequeNumberFirst}
          onChange={(e) => onChange({ chequeNumberFirst: e.target.checked })}
        />
        Match cheque number and amount first (FRBS 3.3.2), then the standard rules
      </label>
    </div>
  );
}
