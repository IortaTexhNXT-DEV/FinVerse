import { useQuery } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { lovApi } from '@/api/lov';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { formatAmount, today } from '@/utils/format';
import type { TemplateField } from '../cases/api';
import type { StrTransaction } from './api';
import { blankTransaction, total, transactionError } from './strLogic';
import type { EditLine } from './strLogic';

/** The list of the STR reason codes (SQ09). */
export const REASON_LOV = 'SCR_STR_REASON';

/** The reason codes as check boxes (at least one, FR-SS-070). */
export function ReasonCodes({
  value,
  disabled,
  error,
  onChange,
}: Readonly<{
  value: string[];
  disabled: boolean;
  error?: string;
  onChange: (codes: string[]) => void;
}>) {
  const options = useQuery({
    queryKey: ['lov', REASON_LOV],
    queryFn: () => lovApi.options(REASON_LOV),
    staleTime: 5 * 60_000,
  });
  return (
    <fieldset className="stack">
      <legend>Reason Codes</legend>
      {(options.data ?? []).length === 0 && (
        <p className="muted">No STR reason code is configured yet (AMLC codes pending, SQ09).</p>
      )}
      {(options.data ?? []).map((o) => (
        <label key={o.code} className="checkbox">
          <input
            type="checkbox"
            disabled={disabled}
            checked={value.includes(o.code)}
            onChange={(e) =>
              onChange(e.target.checked ? [...value, o.code] : value.filter((c) => c !== o.code))
            }
          />{' '}
          {o.label}
        </label>
      ))}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </fieldset>
  );
}

/** The template fields of the STR (the reason field is the check boxes above). */
export function TemplateFields({
  fields,
  values,
  gaps,
  disabled,
  onChange,
}: Readonly<{
  fields: TemplateField[];
  values: Record<string, string | null>;
  gaps: Record<string, string>;
  disabled: boolean;
  onChange: (values: Record<string, string | null>) => void;
}>) {
  return (
    <div className="form-grid">
      {fields
        .filter((f) => f.lovType !== REASON_LOV)
        .map((f) => (
          <Field
            key={f.code}
            label={f.label}
            required={f.mandatory}
            hint={f.help ?? undefined}
            error={gaps[f.code]}
          >
            {(id) =>
              f.dataType === 'LONG_TEXT' ? (
                <textarea
                  id={id}
                  className="textarea"
                  rows={5}
                  disabled={disabled}
                  value={values[f.code] ?? ''}
                  onChange={(e) => onChange({ ...values, [f.code]: e.target.value })}
                />
              ) : (
                <input
                  id={id}
                  className="input"
                  disabled={disabled}
                  value={values[f.code] ?? ''}
                  onChange={(e) => onChange({ ...values, [f.code]: e.target.value })}
                />
              )
            }
          </Field>
        ))}
    </div>
  );
}

const COLUMNS: { key: keyof StrTransaction; label: string; type?: string }[] = [
  { key: 'reference', label: 'Reference' },
  { key: 'date', label: 'Date', type: 'date' },
  { key: 'type', label: 'Type' },
  { key: 'currency', label: 'Currency' },
  { key: 'amount', label: 'Amount' },
  { key: 'description', label: 'Description' },
];

/** The transactions of the STR, editable while it is a draft (FR-SS-070). */
export function Transactions({
  lines,
  disabled,
  error,
  onChange,
}: Readonly<{
  lines: EditLine[];
  disabled: boolean;
  error?: string;
  onChange: (lines: EditLine[]) => void;
}>) {
  const set = (index: number, key: keyof StrTransaction, value: string) =>
    onChange(lines.map((l, i) => (i === index ? { ...l, [key]: value } : l)));
  return (
    <div className="stack">
      <table className="table">
        <caption className="visually-hidden">STR transactions</caption>
        <thead>
          <tr>
            {COLUMNS.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
            <th aria-label="Remove" />
          </tr>
        </thead>
        <tbody>
          {lines.map((line, index) => (
            <tr key={line.key} title={transactionError(line)}>
              {COLUMNS.map((c) => (
                <td key={c.key}>
                  <input
                    className="input"
                    aria-label={`${c.label} of line ${index + 1}`}
                    type={c.type ?? 'text'}
                    disabled={disabled}
                    value={String(line[c.key] ?? '')}
                    onChange={(e) => set(index, c.key, e.target.value)}
                  />
                </td>
              ))}
              <td>
                {!disabled && (
                  <Button
                    size="sm"
                    variant="ghost"
                    aria-label={`Remove line ${index + 1}`}
                    icon={<Trash2 size={14} />}
                    onClick={() => onChange(lines.filter((_, i) => i !== index))}
                  />
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        {!disabled && (
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => onChange([...lines, blankTransaction(today())])}
          >
            Add Transaction
          </Button>
        )}
        <span className="muted">Total {formatAmount(total(lines))}</span>
      </div>
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
