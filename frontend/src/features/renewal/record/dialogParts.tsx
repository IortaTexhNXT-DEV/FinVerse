import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';

/** Props of a record dialog. */
export interface DialogProps<T> {
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (input: T) => void;
}

/** Cancel and confirm buttons of a record dialog. */
export function DialogFooter({
  label,
  busy,
  disabled,
  onClose,
  onConfirm,
}: Readonly<{
  label: string;
  busy: boolean;
  disabled: boolean;
  onClose: () => void;
  onConfirm: () => void;
}>) {
  return (
    <>
      <Button variant="ghost" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/** A remarks box of up to 200 characters. */
export function TextArea({
  label,
  value,
  onChange,
  required = false,
}: Readonly<{ label: string; value: string; onChange: (v: string) => void; required?: boolean }>) {
  return (
    <Field label={label} required={required} hint="Up to 200 characters">
      {(id) => (
        <textarea
          id={id}
          className="input"
          rows={3}
          maxLength={200}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}
