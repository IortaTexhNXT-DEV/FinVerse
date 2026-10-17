import { useState } from 'react';
import type { AlertItem } from '@/api/alerts';
import { Button } from '@/components/ui/Button';
import { CommentField } from '@/components/ui/CommentField';
import { commentProblem } from '@/components/ui/commentRules';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { alertFacts } from './alertFacts';
import { alertRef } from './alertWording';

export type AlertActionKind = 'acknowledge' | 'resolve';

/** What each action asks and says. */
const ACTIONS: Record<
  AlertActionKind,
  { verb: string; helper: string; label: string; required: boolean; min?: number; max: number }
> = {
  acknowledge: {
    verb: 'Acknowledge',
    helper:
      'Acknowledging tells the team that you are handling the alert; it stays live until it is resolved.',
    label: 'Comment',
    required: false,
    max: 500,
  },
  resolve: {
    verb: 'Resolve',
    helper: 'Resolving closes the alert. The rule raises a new alert if the condition comes back.',
    label: 'Resolution',
    required: true,
    min: 10,
    max: 200,
  },
};

interface AlertActionDialogProps {
  alert: AlertItem;
  kind: AlertActionKind;
  /** The business name of the alert's rule. */
  rule: string;
  busy: boolean;
  error: unknown;
  onConfirm: (comment: string) => void;
  onClose: () => void;
}

/** Acknowledge or resolve an alert, with the alert's key facts and a comment. */
export function AlertActionDialog({
  alert,
  kind,
  rule,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<AlertActionDialogProps>) {
  const action = ACTIONS[kind];
  const [comment, setComment] = useState('');
  const [touched, setTouched] = useState(false);
  const rules = { required: action.required, min: action.min, max: action.max };
  const problem = commentProblem(comment, { ...rules, noun: action.label.toLowerCase() });
  return (
    <Modal
      open
      size="md"
      title={`${action.verb} Alert ${alertRef(alert.id)}`}
      facts={alertFacts(alert, rule)}
      helper={action.helper}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={busy}
            onClick={() => {
              setTouched(true);
              if (problem === undefined) {
                onConfirm(comment.trim());
              }
            }}
          >
            {`${action.verb} Alert`}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} title={`Cannot ${action.verb.toLowerCase()} the alert`} />
        <CommentField
          label={action.label}
          value={comment}
          onChange={setComment}
          {...rules}
          showProblem={touched}
          onBlur={() => setTouched(comment !== '' || touched)}
        />
      </div>
    </Modal>
  );
}
