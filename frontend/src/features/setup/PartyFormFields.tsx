import type { PartyType } from '@/api/parties';
import { Field } from '@/components/ui/Field';
import { humanize } from '@/utils/format';
import { hasWithholding, isIntermediary } from './partyForm';
import type { PartyForm } from './partyForm';
import { SETUP_PARTY_TYPES } from './partyTypes';

interface Props {
  form: PartyForm;
  errors: Record<string, string>;
  onChange: (form: PartyForm) => void;
}

type TextKey =
  | 'name'
  | 'taxId'
  | 'address'
  | 'email'
  | 'phone'
  | 'defaultCurrency'
  | 'bankName'
  | 'bankAccountNo'
  | 'licenceNo';

const TEXT_FIELDS: { key: TextKey; label: string; required?: boolean }[] = [
  { key: 'name', label: 'Name', required: true },
  { key: 'taxId', label: 'TIN / tax id' },
  { key: 'address', label: 'Address' },
  { key: 'email', label: 'E-mail' },
  { key: 'phone', label: 'Phone' },
  { key: 'defaultCurrency', label: 'Default currency', required: true },
  { key: 'bankName', label: 'Bank name' },
  { key: 'bankAccountNo', label: 'Bank account no.' },
];

function toNumber(value: string): number | undefined {
  return value === '' ? undefined : Number(value);
}

/** Fields of the business partner form; intermediary and payee fields follow the party type. */
export function PartyFormFields({ form, errors, onChange }: Readonly<Props>) {
  const set = (patch: Partial<PartyForm>) => onChange({ ...form, ...patch });
  const textFields = isIntermediary(form.partyType)
    ? [...TEXT_FIELDS, { key: 'licenceNo' as const, label: 'Licence no.' }]
    : TEXT_FIELDS;
  return (
    <div className="form-grid">
      <Field label="Party code" required error={errors.code}>
        {(id) => (
          <input
            id={id}
            className="input"
            disabled={form.id !== undefined}
            value={form.code ?? ''}
            onChange={(e) => set({ code: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Party type" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.partyType}
            onChange={(e) => set({ partyType: e.target.value as PartyType })}
          >
            {SETUP_PARTY_TYPES.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </select>
        )}
      </Field>
      {textFields.map((f) => (
        <Field key={f.key} label={f.label} required={f.required} error={errors[f.key]}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={form[f.key] ?? ''}
              onChange={(e) => set({ [f.key]: e.target.value })}
            />
          )}
        </Field>
      ))}
      <Field label="Credit days" error={errors.creditDays}>
        {(id) => (
          <input
            id={id}
            className="input"
            type="number"
            min={0}
            max={365}
            value={form.creditDays ?? 0}
            onChange={(e) => set({ creditDays: Number(e.target.value) })}
          />
        )}
      </Field>
      {isIntermediary(form.partyType) && (
        <Field label="Commission rate %" error={errors.commissionRate}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="number"
              step="0.01"
              value={form.commissionRate ?? ''}
              onChange={(e) => set({ commissionRate: toNumber(e.target.value) })}
            />
          )}
        </Field>
      )}
      {hasWithholding(form.partyType) && (
        <Field label="Withholding tax rate %" error={errors.withholdingTaxRate}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="number"
              step="0.01"
              value={form.withholdingTaxRate ?? ''}
              onChange={(e) => set({ withholdingTaxRate: toNumber(e.target.value) })}
            />
          )}
        </Field>
      )}
    </div>
  );
}
