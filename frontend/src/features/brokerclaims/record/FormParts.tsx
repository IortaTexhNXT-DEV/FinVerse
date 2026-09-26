import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';

/** Shared form pieces of the Claims Handling screens and dialogs. */

/** Modal footer of a single parameterised action: Cancel, then the primary action. */
export function DialogFooter({
  label,
  busy,
  onClose,
  onConfirm,
  disabled = false,
}: Readonly<{
  label: string;
  busy: boolean;
  onClose: () => void;
  onConfirm: () => void;
  disabled?: boolean;
}>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button variant="accent" busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/** A labelled text, date or number input with its field error. */
export function InputField({
  label,
  value,
  onChange,
  type = 'text',
  required = false,
  error,
  hint,
  maxLength = 60,
}: Readonly<{
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: 'text' | 'date' | 'number' | 'email';
  required?: boolean;
  error?: string;
  hint?: string;
  maxLength?: number;
}>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          maxLength={type === 'text' ? maxLength : undefined}
          min={type === 'number' ? 0 : undefined}
          step={type === 'number' ? 'any' : undefined}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/** A labelled text area. */
export function TextAreaField({
  label,
  value,
  onChange,
  required = false,
  error,
  maxLength = 2000,
  rows = 3,
}: Readonly<{
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  error?: string;
  maxLength?: number;
  rows?: number;
}>) {
  return (
    <Field label={label} required={required} error={error}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={rows}
          maxLength={maxLength}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/** A labelled select bound to a list of values. */
export function LovField({
  label,
  type,
  value,
  onChange,
  required = false,
  error,
  placeholder,
}: Readonly<{
  label: string;
  type: string;
  value: string;
  onChange: (code: string) => void;
  required?: boolean;
  error?: string;
  placeholder?: string;
}>) {
  return (
    <Field label={label} required={required} error={error}>
      {(id) => (
        <LovSelect
          id={id}
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
        />
      )}
    </Field>
  );
}
