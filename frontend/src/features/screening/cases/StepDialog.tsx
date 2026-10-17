import type { ReactNode } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/** The shell of a case action dialog: title, close button, body, Cancel and the action. */
export function StepDialog({
  title,
  confirmLabel,
  busy,
  error,
  onConfirm,
  onClose,
  children,
}: Readonly<{
  title: string;
  confirmLabel: string;
  busy: boolean;
  error: unknown;
  onConfirm: () => void;
  onClose: () => void;
  children: ReactNode;
}>) {
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
          <Button variant="accent" busy={busy} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {children}
      </div>
    </Modal>
  );
}

/** A list-of-values field with its label and error. */
export function LovField({
  label,
  type,
  value,
  onChange,
  parentCode,
  error,
  required = true,
}: Readonly<{
  label: string;
  type: string;
  value: string;
  onChange: (value: string) => void;
  parentCode?: string;
  error?: string;
  required?: boolean;
}>) {
  return (
    <Field label={label} required={required} error={error}>
      {(id) => (
        <LovSelect id={id} type={type} value={value} parentCode={parentCode} onChange={onChange} />
      )}
    </Field>
  );
}

/** A long text field with its label and error. */
export function TextField({
  label,
  value,
  onChange,
  error,
  required = true,
  max = 4000,
  hint,
}: Readonly<{
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  required?: boolean;
  max?: number;
  hint?: string;
}>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={4}
          maxLength={max}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}
