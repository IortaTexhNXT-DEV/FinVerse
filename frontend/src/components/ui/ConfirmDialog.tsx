import { useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from './Button';
import { ErrorAlert } from './ErrorAlert';
import { Modal } from './Modal';

interface ConfirmDialogProps {
  /** "Cancel Voucher DV-2026-000003" */
  title: string;
  /** The record the action applies to, named in the dialog. */
  record?: string;
  /** What happens, in business terms ("The check is voided and the payable reopens."). */
  effect: ReactNode;
  /** Label of the confirming button, e.g. "Cancel Voucher". */
  confirmLabel: string;
  /** Ask for a reason (mandatory when the process needs one). */
  reason?: 'required' | 'optional';
  /** Destructive or irreversible: the confirming button is the red danger button. */
  destructive?: boolean;
  busy?: boolean;
  error?: unknown;
  onConfirm: (reason: string) => void;
  onClose: () => void;
}

/**
 * Themed confirmation of a destructive or irreversible action (cancel, reverse, deactivate,
 * delete, approve with money impact): names the record and the effect, asks for a reason where
 * the process needs one, and disables the button while the action runs (no double submit).
 */
export function ConfirmDialog({
  title,
  record,
  effect,
  confirmLabel,
  reason,
  destructive = false,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<ConfirmDialogProps>) {
  const [text, setText] = useState('');
  const [touched, setTouched] = useState(false);
  const missing = reason === 'required' && text.trim() === '';
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Go Back
          </Button>
          <Button
            variant={destructive ? 'danger' : 'primary'}
            busy={busy}
            onClick={() => {
              setTouched(true);
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
        {record !== undefined && (
          <p className="confirm-record">
            <strong>{record}</strong>
          </p>
        )}
        <div className="confirm-effect">{effect}</div>
        {reason !== undefined && (
          <div className="field">
            <label htmlFor="confirm-reason" className={reason === 'required' ? 'required' : ''}>
              Reason
            </label>
            <textarea
              id="confirm-reason"
              className="textarea"
              value={text}
              aria-invalid={touched && missing ? true : undefined}
              onChange={(e) => setText(e.target.value)}
            />
            {touched && missing && (
              <span className="field-error" role="alert">
                Enter the reason.
              </span>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
