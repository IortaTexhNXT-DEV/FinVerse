import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';

export type DecisionMode = 'approve' | 'reject' | null;

interface Props {
  mode: DecisionMode;
  label: string;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onApprove: (accountingDate: string) => void;
  onReject: (reason: string) => void;
}

/** Checker dialog: approval with an accounting date, or rejection with a mandatory reason. */
export function DecisionDialog({
  mode,
  label,
  busy,
  error,
  onClose,
  onApprove,
  onReject,
}: Readonly<Props>) {
  const [reason, setReason] = useState('');
  const [accountingDate, setAccountingDate] = useState(today());
  const approving = mode === 'approve';
  const confirm = () => {
    if (approving) {
      onApprove(accountingDate);
    } else {
      onReject(reason);
      setReason('');
    }
  };
  return (
    <Modal
      title={approving ? `Approve ${label}` : `Reject ${label}`}
      open={mode !== null}
      onClose={onClose}
      footer={
        <Button
          variant={approving ? 'accent' : 'danger'}
          disabled={!approving && reason.trim() === ''}
          busy={busy}
          onClick={confirm}
        >
          Confirm
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {approving ? (
          <Field
            label="Accounting date"
            required
            hint="Premium, taxes and commission are posted on this date"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={accountingDate}
                onChange={(e) => setAccountingDate(e.target.value)}
              />
            )}
          </Field>
        ) : (
          <Field label="Reason" required>
            {(id) => (
              <textarea
                id={id}
                className="textarea"
                maxLength={200}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}
