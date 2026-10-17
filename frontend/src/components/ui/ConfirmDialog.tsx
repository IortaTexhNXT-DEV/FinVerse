import { useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from './Button';
import { actionPhrase } from '@/utils/format';
import { CommentField } from './CommentField';
import { commentProblem } from './commentRules';
import type { Definition } from './DefinitionGrid';
import { ErrorAlert } from './ErrorAlert';
import { Modal } from './Modal';
import { cancelLabelFor, consequenceOf } from './dialogStandard';
import type { ModalSize } from './dialogStandard';

interface ConfirmDialogProps {
  /** "Cancel Voucher DV-2026-000003" */
  title: string;
  /** The record the action applies to, named in the dialog. */
  record?: string;
  /** The key facts of the record (reference, status, amount...). */
  facts?: readonly Definition[];
  /** What happens, in business terms ("The check is voided and the payable reopens."). */
  effect: ReactNode;
  /** Label of the confirming button, e.g. "Cancel Voucher". */
  confirmLabel: string;
  /** Ask for a reason (mandatory when the process needs one). */
  reason?: 'required' | 'optional';
  /** Least and most characters of the reason. */
  reasonMin?: number;
  reasonMax?: number;
  /** Destructive or irreversible: the confirming button is the red danger button. */
  destructive?: boolean;
  size?: ModalSize;
  busy?: boolean;
  error?: unknown;
  onConfirm: (reason: string) => void;
  onClose: () => void;
}

/** What the action does; a destructive one also says that it cannot be undone. */
function Effect({ effect, destructive }: Readonly<{ effect: ReactNode; destructive: boolean }>) {
  const consequence = destructive && typeof effect === 'string' ? consequenceOf(effect) : '';
  return (
    <div className={destructive ? 'confirm-effect danger' : 'confirm-effect'}>
      <div>{effect}</div>
      {consequence !== '' && <div className="confirm-consequence">{consequence}</div>}
    </div>
  );
}

/**
 * Themed confirmation of a destructive or irreversible action (cancel, reverse, deactivate,
 * delete, approve with money impact): names the record, its key facts and the effect (with the
 * consequence of a destructive action), asks for a reason where the process needs one, and disables
 * the button while the action runs (no double submit).
 */
export function ConfirmDialog({
  title,
  record,
  facts,
  effect,
  confirmLabel,
  reason,
  reasonMin,
  reasonMax,
  destructive = false,
  size,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<ConfirmDialogProps>) {
  const [text, setText] = useState('');
  const [touched, setTouched] = useState(false);
  const problem =
    reason === undefined
      ? undefined
      : commentProblem(text, {
          required: reason === 'required',
          min: reasonMin,
          max: reasonMax,
          noun: 'reason',
        });
  return (
    <Modal
      title={title}
      open
      size={size ?? (facts === undefined ? 'sm' : 'md')}
      facts={facts}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            {cancelLabelFor(confirmLabel)}
          </Button>
          <Button
            variant={destructive ? 'danger' : 'primary'}
            busy={busy}
            onClick={() => {
              setTouched(true);
              if (problem === undefined) {
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
        <ErrorAlert error={error} title={`Cannot ${actionPhrase(confirmLabel)}`} />
        {record !== undefined && (
          <p className="confirm-record">
            <strong>{record}</strong>
          </p>
        )}
        <Effect effect={effect} destructive={destructive} />
        {reason !== undefined && (
          <CommentField
            label="Reason"
            value={text}
            onChange={setText}
            required={reason === 'required'}
            min={reasonMin}
            max={reasonMax}
            showProblem={touched}
          />
        )}
      </div>
    </Modal>
  );
}
