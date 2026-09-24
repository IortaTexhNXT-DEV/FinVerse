import { useState } from 'react';
import type { ActionNote } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { LovSelect } from './LovSelect';

interface ActionDialogProps {
  title: string;
  /** List of values of the mandatory reason; omit when no reason is needed. */
  reasonLov?: string;
  confirmLabel: string;
  busy?: boolean;
  error?: unknown;
  onConfirm: (note: ActionNote) => void;
  onClose: () => void;
}

/** Confirmation with an optional mandatory reason and a comment (return, void, cancel...). */
export function ActionDialog({
  title,
  reasonLov,
  confirmLabel,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<ActionDialogProps>) {
  const [reasonCode, setReasonCode] = useState('');
  const [comment, setComment] = useState('');
  const missingReason = reasonLov !== undefined && reasonCode === '';
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
        <Field label="Comment" hint="Shown in the status history and sent with the notification.">
          {(id) => (
            <textarea
              id={id}
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
