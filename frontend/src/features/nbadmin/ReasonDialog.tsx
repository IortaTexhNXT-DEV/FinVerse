import { useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

interface ReasonDialogProps {
  title: string;
  label: string;
  confirmLabel: string;
  /** Whether the text is mandatory (return, reject, cancel) or optional (approval comment). */
  required: boolean;
  hint?: string;
  busy: boolean;
  error: unknown;
  /** Content shown above the text (e.g. the change to implement). */
  children?: ReactNode;
  onConfirm: (text: string) => void;
  onClose: () => void;
}

/** A single decision on an access request with its remarks (approve, return, reject, cancel). */
export function ReasonDialog({
  title,
  label,
  confirmLabel,
  required,
  hint,
  busy,
  error,
  children,
  onConfirm,
  onClose,
}: Readonly<ReasonDialogProps>) {
  const [text, setText] = useState('');
  const [tried, setTried] = useState(false);
  const missing = required && text.trim() === '';
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
          <Button
            variant="accent"
            busy={busy}
            onClick={() => {
              setTried(true);
              if (!missing) {
                onConfirm(text.trim());
              }
            }}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {children}
        <Field
          label={label}
          required={required}
          hint={hint}
          error={tried && missing ? `Enter the ${label.toLowerCase()}` : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
