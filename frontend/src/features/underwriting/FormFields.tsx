import { Field } from '@/components/ui/Field';

/** Compact labelled inputs used by the underwriting forms (built on the UI kit Field). */

interface BaseProps {
  label: string;
  required?: boolean;
  disabled?: boolean;
  hint?: string;
}

export function TextField({
  label,
  required,
  disabled,
  hint,
  value,
  onChange,
}: Readonly<BaseProps & { value: string | undefined; onChange: (value: string) => void }>) {
  return (
    <Field label={label} required={required} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

export function NumberField({
  label,
  required,
  disabled,
  hint,
  value,
  onChange,
}: Readonly<
  BaseProps & { value: number | undefined; onChange: (value: number | undefined) => void }
>) {
  return (
    <Field label={label} required={required} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input num"
          type="number"
          step="any"
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
        />
      )}
    </Field>
  );
}

export function DateField({
  label,
  required,
  disabled,
  value,
  onChange,
}: Readonly<BaseProps & { value: string | undefined; onChange: (value: string) => void }>) {
  return (
    <Field label={label} required={required}>
      {(id) => (
        <input
          id={id}
          className="input"
          type="date"
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

export interface Option {
  value: string;
  label: string;
}

export function SelectField({
  label,
  required,
  disabled,
  value,
  options,
  emptyLabel,
  onChange,
}: Readonly<
  BaseProps & {
    value: string | undefined;
    options: Option[];
    emptyLabel?: string;
    onChange: (value: string) => void;
  }
>) {
  return (
    <Field label={label} required={required}>
      {(id) => (
        <select
          id={id}
          className="select"
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        >
          {emptyLabel !== undefined && <option value="">{emptyLabel}</option>}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

export function CheckboxField({
  label,
  checked,
  disabled,
  onChange,
}: Readonly<{
  label: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}>) {
  return (
    <label className="checkbox">
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
      />
      {label}
    </label>
  );
}
