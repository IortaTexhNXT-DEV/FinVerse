import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';

export interface DateReasonValue {
  date: string;
  reason: string;
  chequeDate: string;
}

interface DateReasonModalProps {
  title: string;
  open: boolean;
  /** Which inputs to show. */
  withDate?: boolean;
  withReason?: boolean;
  withChequeDate?: boolean;
  confirmLabel: string;
  busy?: boolean;
  error?: unknown;
  onConfirm: (value: DateReasonValue) => void;
  onClose: () => void;
}

/** Small confirmation dialog asking for an effective date and / or a reason. */
export function DateReasonModal({
  title,
  open,
  withDate = false,
  withReason = false,
  withChequeDate = false,
  confirmLabel,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Readonly<DateReasonModalProps>) {
  const [value, setValue] = useState<DateReasonValue>({
    date: today(),
    reason: '',
    chequeDate: today(),
  });
  const missingReason = withReason && value.reason.trim() === '';
  return (
    <Modal
      title={title}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={busy}
          disabled={missingReason}
          onClick={() => onConfirm(value)}
        >
          {confirmLabel}
        </Button>
      }
    >
      <ErrorAlert error={error} />
      <div className="form-grid">
        {withDate && (
          <Field label="Effective date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={value.date}
                onChange={(e) => setValue({ ...value, date: e.target.value })}
              />
            )}
          </Field>
        )}
        {withChequeDate && (
          <Field label="New cheque date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={value.chequeDate}
                onChange={(e) => setValue({ ...value, chequeDate: e.target.value })}
              />
            )}
          </Field>
        )}
        {withReason && (
          <Field label="Reason" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={200}
                value={value.reason}
                onChange={(e) => setValue({ ...value, reason: e.target.value })}
              />
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}
