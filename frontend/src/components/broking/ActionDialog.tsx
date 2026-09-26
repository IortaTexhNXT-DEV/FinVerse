import { useState } from 'react';
import type { ActionNote } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { LovSelect } from './LovSelect';

interface ActionDialogProps {
  title: string;
  /** The record the action applies to (reference), named in the dialog. */
  record?: string;
  /** What the action does, in business terms. */
  effect?: string;
  /** List of values of the mandatory reason; omit when no reason is needed. */
  reasonLov?: string;
  /** The comment must be entered (e.g. the reason for closing a request). */
  commentRequired?: boolean;
  /** Label of the comment field (default "Comment"). */
  commentLabel?: string;
  confirmLabel: string;
  busy?: boolean;
  error?: unknown;
  onConfirm: (note: ActionNote) => void;
  onClose: () => void;
}

/** Irreversible or destructive actions: the confirming button is the red danger button. */
const DESTRUCTIVE = /\b(cancel|void|revers|deactivat|delet|reject|declin|terminat|write[- ]off)/i;

/**
 * Confirmation with an optional mandatory reason and a comment (return, void, cancel...): names
 * the record and the effect; a destructive action confirms with the red danger button.
 */
export function ActionDialog({
  title,
  record,
  effect,
  reasonLov,
  commentRequired = false,
  commentLabel = 'Comment',
  confirmLabel,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<ActionDialogProps>) {
  const [reasonCode, setReasonCode] = useState('');
  const [comment, setComment] = useState('');
  const missingReason =
    (reasonLov !== undefined && reasonCode === '') || (commentRequired && comment.trim() === '');
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Go Back
          </Button>
          <Button
            variant={DESTRUCTIVE.test(confirmLabel) ? 'danger' : 'accent'}
            busy={busy}
            disabled={missingReason}
            onClick={() =>
              onConfirm({
                reasonCode: reasonCode || undefined,
                comment: comment.trim() || undefined,
              })
            }
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
        {effect !== undefined && <p className="confirm-effect">{effect}</p>}
        {reasonLov && (
          <Field label="Reason" required>
            {(id) => (
              <LovSelect
                id={id}
                type={reasonLov}
                value={reasonCode}
                onChange={setReasonCode}
                required
              />
            )}
          </Field>
        )}
        <Field
          label={commentLabel}
          required={commentRequired}
          hint="Shown in the status history and sent with the notification."
        >
          {(id) => (
            <textarea
              id={id}
              required={commentRequired}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
