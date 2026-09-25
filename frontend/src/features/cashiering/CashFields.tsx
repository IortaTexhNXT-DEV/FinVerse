import type { ReactNode } from 'react';
import { Field } from '@/components/ui/Field';
import { humanize } from '@/utils/format';

interface TextFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  hint?: string;
  required?: boolean;
  type?: 'text' | 'number' | 'date';
  maxLength?: number;
  placeholder?: string;
}

/** A labelled text, number or date input with its field error. */
export function TextField({
  label,
  value,
  onChange,
  error,
  hint,
  required = false,
  type = 'text',
  maxLength,
  placeholder,
}: Readonly<TextFieldProps>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          step={type === 'number' ? '0.01' : undefined}
          value={value}
          maxLength={maxLength}
          placeholder={placeholder}
          aria-invalid={error !== undefined}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

interface CodeSelectProps {
  label: string;
  value: string;
  options: readonly string[];
  onChange: (value: string) => void;
  error?: string;
  required?: boolean;
  /** Text of an empty option; omit to force a choice. */
  empty?: string;
  labelOf?: (code: string) => ReactNode;
}

/** A labelled select over fixed codes (enums), shown humanised. */
export function CodeSelect({
  label,
  value,
  options,
  onChange,
  error,
  required = false,
  empty,
  labelOf = humanize,
}: Readonly<CodeSelectProps>) {
  return (
    <Field label={label} required={required} error={error}>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          aria-invalid={error !== undefined}
          onChange={(e) => onChange(e.target.value)}
        >
          {empty !== undefined && <option value="">{empty}</option>}
          {options.map((o) => (
            <option key={o} value={o}>
              {labelOf(o)}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}
