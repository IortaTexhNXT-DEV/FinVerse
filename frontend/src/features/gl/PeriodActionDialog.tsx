import { useState } from 'react';
import type { Period } from '@/api/periods';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatDate } from '@/utils/format';
import { periodActionText, REASON_MAX_LENGTH, reasonProblem } from './periodActions';
import type { PeriodAction } from './periodActions';

export interface PendingPeriodAction {
  period: Period;
  action: PeriodAction;
}

/** Confirmation of a period status change, naming the period and the consequence. */
export function PeriodActionDialog({
  pending,
  busy,
  error,
  onCancel,
  onConfirm,
}: Readonly<{
  pending: PendingPeriodAction | null;
  busy: boolean;
  error: unknown;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}>) {
  const [reason, setReason] = useState('');
  const [touched, setTouched] = useState(false);
  if (pending === null) {
    return null;
  }
  const text = periodActionText(pending.action, pending.period);
  const problem = text.needsReason ? reasonProblem(reason) : undefined;
  const close = () => {
    setReason('');
    setTouched(false);
    onCancel();
  };
  return (
    <Modal
      title={text.title}
      open
      onClose={close}
      footer={
        <>
          <Button variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button
            variant={text.danger ? 'danger' : 'accent'}
            busy={busy}
            disabled={problem !== undefined}
            onClick={() => onConfirm(reason.trim())}
          >
            {text.confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p style={{ margin: 0 }}>
          <strong>
            {pending.period.name} ({formatDate(pending.period.startDate)} –{' '}
            {formatDate(pending.period.endDate)})
          </strong>
        </p>
        <p style={{ margin: 0 }}>{text.consequence}</p>
        {text.needsReason && (
          <Field
            label="Reason"
            required
            error={touched ? problem : undefined}
            hint={`${reason.length} / ${REASON_MAX_LENGTH} characters`}
          >
            {(id) => (
              <textarea
                id={id}
                className="textarea"
                maxLength={REASON_MAX_LENGTH}
                value={reason}
                onBlur={() => setTouched(true)}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}
