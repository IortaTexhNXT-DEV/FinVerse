import { useState } from 'react';
import type { ActionNote } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { CommentField } from '@/components/ui/CommentField';
import { commentProblem } from '@/components/ui/commentRules';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { cancelLabelFor, consequenceOf } from '@/components/ui/dialogStandard';
import { actionPhrase } from '@/utils/format';
import { LovSelect } from './LovSelect';

interface ActionDialogProps {
  title: string;
  /** The record the action applies to (reference), named in the dialog. */
  record?: string;
  /** The key facts of the record (reference, status, amount...). */
  facts?: readonly Definition[];
  /** What the action does, in business terms. */
  effect?: string;
  /** List of values of the mandatory reason; omit when no reason is needed. */
  reasonLov?: string;
  /** The comment must be entered (e.g. the reason for closing a request). */
  commentRequired?: boolean;
  /** Label of the comment field (default "Comment"). */
  commentLabel?: string;
  /** Least characters of the comment once entered. */
  commentMin?: number;
  confirmLabel: string;
  busy?: boolean;
  error?: unknown;
  /** Title of the error notice; "Cannot <action>" by default. */
  errorTitle?: string;
  onConfirm: (note: ActionNote) => void;
  onClose: () => void;
}

/** Irreversible or destructive actions: the confirming button is the red danger button. */
const DESTRUCTIVE = /\b(cancel|void|revers|deactivat|delet|reject|declin|terminat|write[- ]off)/i;

/** The longest comment of a workflow action. */
const COMMENT_MAX = 1000;

/** The record the action applies to and what the action does (with its consequence when destructive). */
function RecordAndEffect({
  record,
  effect,
  destructive,
}: Readonly<{ record?: string; effect?: string; destructive: boolean }>) {
  const sentence = [effect, destructive ? consequenceOf(effect ?? '') : '']
    .filter(Boolean)
    .join(' ');
  return (
    <>
      {record !== undefined && (
        <p className="confirm-record">
          <strong>{record}</strong>
        </p>
      )}
      {sentence !== '' && (
        <p className={destructive ? 'confirm-effect danger' : 'confirm-effect'}>{sentence}</p>
      )}
    </>
  );
}

/** The mandatory reason from a list of values, with its message under the field. */
function ReasonField({
  type,
  value,
  touched,
  onChange,
}: Readonly<{ type: string; value: string; touched: boolean; onChange: (code: string) => void }>) {
  return (
    <Field
      label="Reason"
      required
      error={touched && value === '' ? 'Choose the reason.' : undefined}
    >
      {(id) => <LovSelect id={id} type={type} value={value} onChange={onChange} required />}
    </Field>
  );
}

/**
 * Confirmation with an optional mandatory reason and a comment (return, void, cancel...): names
 * the record, its key facts and the effect; a destructive action confirms with the red danger
 * button and says that it cannot be undone.
 */
export function ActionDialog({
  title,
  record,
  facts,
  effect,
  reasonLov,
  commentRequired = false,
  commentLabel = 'Comment',
  commentMin,
  confirmLabel,
  busy = false,
  error,
  errorTitle,
  onConfirm,
  onClose,
}: Readonly<ActionDialogProps>) {
  const [reasonCode, setReasonCode] = useState('');
  const [comment, setComment] = useState('');
  const [touched, setTouched] = useState(false);
  const destructive = DESTRUCTIVE.test(confirmLabel);
  const problem = commentProblem(comment, {
    required: commentRequired,
    min: commentMin,
    max: COMMENT_MAX,
    noun: commentLabel.toLowerCase(),
  });
  const missing = (reasonLov !== undefined && reasonCode === '') || problem !== undefined;
  return (
    <Modal
      open
      title={title}
      size="md"
      facts={facts}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            {cancelLabelFor(confirmLabel)}
          </Button>
          <Button
            variant={destructive ? 'danger' : 'accent'}
            busy={busy}
            disabled={missing}
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
        <ErrorAlert error={error} title={errorTitle ?? `Cannot ${actionPhrase(title)}`} />
        <RecordAndEffect record={record} effect={effect} destructive={destructive} />
        {reasonLov !== undefined && reasonLov !== '' && (
          <ReasonField
            type={reasonLov}
            value={reasonCode}
            touched={touched}
            onChange={(code) => {
              setTouched(true);
              setReasonCode(code);
            }}
          />
        )}
        <CommentField
          label={commentLabel}
          value={comment}
          onChange={setComment}
          required={commentRequired}
          min={commentMin}
          max={COMMENT_MAX}
          showProblem={touched}
          onBlur={() => setTouched(true)}
          helper="Shown in the status history and sent with the notification."
        />
      </div>
    </Modal>
  );
}
