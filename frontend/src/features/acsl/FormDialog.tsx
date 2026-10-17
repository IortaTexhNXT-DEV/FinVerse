import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/** One field of a dialog. */
export interface DialogField {
  key: string;
  label: string;
  required?: boolean;
  hint?: string;
  /** Options of a select; omit for a text input. */
  options?: readonly { value: string; label: string }[];
  multiline?: boolean;
  initial?: string;
}

interface FormDialogProps {
  title: string;
  confirmLabel: string;
  fields: readonly DialogField[];
  busy: boolean;
  error: unknown;
  onConfirm: (values: Record<string, string>) => void;
  onClose: () => void;
}

function Control({
  id,
  field,
  value,
  invalid,
  onChange,
}: Readonly<{
  id: string;
  field: DialogField;
  value: string;
  invalid: boolean;
  onChange: (v: string) => void;
}>) {
  if (field.options) {
    return (
      <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Select…</option>
        {field.options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    );
  }
  if (field.multiline) {
    return (
      <textarea
        id={id}
        className="textarea"
        rows={4}
        aria-invalid={invalid}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }
  return (
    <input
      id={id}
      className="input"
      aria-invalid={invalid}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

/**
 * A single parameterised action of an ACSL record (BDO dialog pattern): title with close, the
 * fields with their errors, Cancel and the action in the footer.
 */
export function FormDialog({
  title,
  confirmLabel,
  fields,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<FormDialogProps>) {
  const [values, setValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(fields.map((f) => [f.key, f.initial ?? ''])),
  );
  const [missing, setMissing] = useState<string[]>([]);
  const confirm = () => {
    const empty = fields.filter((f) => f.required && (values[f.key] ?? '').trim() === '');
    setMissing(empty.map((f) => f.key));
    if (empty.length === 0) {
      onConfirm(values);
    }
  };
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={busy} onClick={confirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {fields.map((f) => (
          <Field
            key={f.key}
            label={f.label}
            required={f.required}
            hint={f.hint}
            error={missing.includes(f.key) ? 'Required' : undefined}
          >
            {(id) => (
              <Control
                id={id}
                field={f}
                value={values[f.key] ?? ''}
                invalid={missing.includes(f.key)}
                onChange={(v) => setValues({ ...values, [f.key]: v })}
              />
            )}
          </Field>
        ))}
      </div>
    </Modal>
  );
}
