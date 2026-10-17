import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import './disbursement.css';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

/** Modal footer: Cancel (secondary) and the action (primary). */
export function DialogFooter({
  label,
  busy,
  disabled = false,
  onClose,
  onConfirm,
}: Readonly<{
  label: string;
  busy: boolean;
  disabled?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} disabled={disabled} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/** A workflow action with an optional comment (submit, route, approve). */
export function CommentDialog({
  title,
  label,
  intro,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & { title: string; label: string; intro?: string; onConfirm: (c?: string) => void }
>) {
  const [comment, setComment] = useState('');
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <DialogFooter
          label={label}
          busy={busy}
          onClose={onClose}
          onConfirm={() => onConfirm(comment.trim() || undefined)}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {intro !== undefined && <p>{intro}</p>}
        <Field label="Comment" hint="Kept in the status history and sent with the notification.">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Asks one or more text values (release recipient, branch reference, ATD recipients). */
export function TextDialog({
  title,
  label,
  fieldLabel,
  hint,
  required = true,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<
  DialogProps & {
    title: string;
    label: string;
    fieldLabel: string;
    hint?: string;
    required?: boolean;
    onConfirm: (value: string) => void;
  }
>) {
  const [value, setValue] = useState('');
  const [touched, setTouched] = useState(false);
  const missing = required && value.trim() === '';
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <DialogFooter
          label={label}
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            setTouched(true);
            if (!missing) {
              onConfirm(value.trim());
            }
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label={fieldLabel}
          required={required}
          hint={hint}
          error={touched && missing ? `Enter the ${fieldLabel.toLowerCase()}` : undefined}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
