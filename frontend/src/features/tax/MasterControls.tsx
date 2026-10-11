import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { TypedInput } from '@/components/ui/DateInput';

interface TextProps {
  label: string;
  value: string | number | undefined;
  onChange: (value: string) => void;
  required?: boolean;
  disabled?: boolean;
  type?: 'text' | 'number' | 'date';
  hint?: string;
}

/** Labelled text / number / date input of the tax master forms. */
export function TextField({
  label,
  value,
  onChange,
  required = false,
  disabled = false,
  type = 'text',
  hint,
}: Readonly<TextProps>) {
  return (
    <Field label={label} required={required} hint={hint}>
      {(id) => (
        <TypedInput
          id={id}
          className="input"
          type={type}
          disabled={disabled}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

interface SelectProps<T extends string> {
  label: string;
  value: T | undefined;
  options: readonly T[];
  onChange: (value: T | undefined) => void;
  allowEmpty?: boolean;
  required?: boolean;
}

/** Labelled select over string options (optionally with an empty choice). */
export function SelectField<T extends string>({
  label,
  value,
  options,
  onChange,
  allowEmpty = false,
  required = false,
}: Readonly<SelectProps<T>>) {
  return (
    <Field label={label} required={required}>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value ?? ''}
          onChange={(e) => onChange(options.find((o) => o === e.target.value))}
        >
          {allowEmpty && <option value="">—</option>}
          {options.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

/** Labelled check box of the tax master forms. */
export function CheckField({
  label,
  checked,
  onChange,
}: Readonly<{ label: string; checked: boolean | undefined; onChange: (v: boolean) => void }>) {
  return (
    <label className="checkbox" style={{ alignSelf: 'end' }}>
      <input
        type="checkbox"
        checked={checked ?? false}
        onChange={(e) => onChange(e.target.checked)}
      />{' '}
      {label}
    </label>
  );
}

/** "Authorize" button of a pending master record (checker). */
export function AuthorizeButton({
  onClick,
  busy,
}: Readonly<{ onClick: () => void; busy: boolean }>): ReactNode {
  return (
    <Button
      size="sm"
      variant="secondary"
      busy={busy}
      onClick={(e) => {
        e.stopPropagation();
        onClick();
      }}
    >
      Authorize
    </Button>
  );
}
