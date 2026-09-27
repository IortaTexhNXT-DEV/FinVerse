import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

interface RemarksDialogProps {
  title: string;
  label: string;
  confirmLabel: string;
  /** Whether the remarks are mandatory (rejection reason, deactivation reason). */
  required?: boolean;
  busy?: boolean;
  error?: unknown;
  onConfirm: (remarks: string) => void;
  onClose: () => void;
}

/** A decision with remarks: reject a version or a list change, deactivate an entry. */
export function RemarksDialog({
  title,
  label,
  confirmLabel,
  required = true,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<RemarksDialogProps>) {
  const [remarks, setRemarks] = useState('');
  const [touched, setTouched] = useState(false);
  const missing = required && remarks.trim() === '';
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
              setTouched(true);
              if (!missing) {
                onConfirm(remarks.trim());
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
        <Field
          label={label}
          required={required}
          error={touched && missing ? `Enter the ${label.toLowerCase()}` : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
