import { Field } from '@/components/ui/Field';
import type { Option } from './options';

interface BaseProps {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  disabled?: boolean;
}

interface TextProps extends BaseProps {
  value: string | undefined;
  onChange: (value: string) => void;
  type?: 'text' | 'date' | 'month';
  upper?: boolean;
}

/** Labelled text / date input. */
export function TextInput({
  label,
  required,
  error,
  hint,
  disabled,
  value,
  onChange,
  type = 'text',
  upper = false,
}: Readonly<TextProps>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(upper ? e.target.value.toUpperCase() : e.target.value)}
        />
      )}
    </Field>
  );
}

interface NumberProps extends BaseProps {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  step?: string;
}

/** Labelled numeric input; blank maps to undefined. */
export function NumberInput({
  label,
  required,
  error,
  hint,
  disabled,
  value,
  onChange,
  step = '0.01',
}: Readonly<NumberProps>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input num"
          type="number"
          step={step}
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
        />
      )}
    </Field>
  );
}

interface SelectProps extends BaseProps {
  value: string | number | undefined;
  options: Option[];
  onChange: (value: string) => void;
  blank?: string;
}

/** Labelled select; `blank` adds an empty first option. */
export function SelectInput({
  label,
  required,
  error,
  hint,
  disabled,
  value,
  options,
  onChange,
  blank,
}: Readonly<SelectProps>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <select
          id={id}
          className="select"
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        >
          {blank !== undefined && <option value="">{blank}</option>}
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
