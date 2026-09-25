import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/** Shared pieces of the Collections plans, escalation and billing dialogs. */

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

/** A record page while its record loads: the spinner, or the error when it failed. */
export function RecordLoading({ error }: Readonly<{ error: unknown }>) {
  return error ? <ErrorAlert error={error} /> : <span className="spinner" aria-label="Loading" />;
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
  type?: 'text' | 'date' | 'number';
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
  hint,
  rows = 3,
}: Readonly<{
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  error?: string;
  hint?: string;
  rows?: number;
}>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={rows}
          maxLength={1000}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * A single action that needs a reason typed by the user (cancel a plan, a promise or a
 * statement).
 */
export function ReasonDialog({
  title,
  label = 'Reason',
  confirmLabel,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  title: string;
  label?: string;
  confirmLabel: string;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (reason: string) => void;
}>) {
  const [reason, setReason] = useState('');
  const [missing, setMissing] = useState(false);
  const confirm = () => {
    const text = reason.trim();
    setMissing(text === '');
    if (text !== '') {
      onConfirm(text);
    }
  };
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <DialogFooter label={confirmLabel} busy={busy} onClose={onClose} onConfirm={confirm} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <TextAreaField
          label={label}
          value={reason}
          onChange={setReason}
          required
          error={missing ? `Enter the ${label.toLowerCase()}` : undefined}
        />
      </div>
    </Modal>
  );
}
