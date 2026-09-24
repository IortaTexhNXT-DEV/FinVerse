import { useState } from 'react';
import type { WorkbenchRow } from '@/api/booking';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';

interface DateDialogProps {
  title: string;
  confirmLabel: string;
  intro: string;
  busy: boolean;
  error: unknown;
  onConfirm: (date: string) => void;
  onClose: () => void;
}

/** A single parameterised action: the booking / business date of a batch (BRNB.036). */
export function BookingDateDialog({
  title,
  confirmLabel,
  intro,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<DateDialogProps>) {
  const [date, setDate] = useState(today());
  const invalid = date === '' || date > today();
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
          <Button busy={busy} disabled={invalid} onClick={() => onConfirm(date)}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <p>{intro}</p>
        <ErrorAlert error={error} />
        <Field
          label="Booking date"
          required
          error={invalid ? 'Enter a booking date that is not in the future' : undefined}
          hint="The accounting period of this date must be open."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              max={today()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

interface EditProps {
  row: WorkbenchRow;
  busy: boolean;
  error: unknown;
  onSave: (bookingDate: string | undefined, costCenter: string | undefined) => void;
  onClose: () => void;
}

/** Changes the booking date and cost center of a queued account before the batch (BRNB.036). */
export function QueueEditDialog({ row, busy, error, onSave, onClose }: Readonly<EditProps>) {
  const [date, setDate] = useState(row.bookingDate ?? '');
  const [costCenter, setCostCenter] = useState(row.costCenter ?? '');
  const future = date !== '' && date > today();
  return (
    <Modal
      open
      title={`Edit ${row.arn}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={future}
            onClick={() => onSave(date || undefined, costCenter.trim() || undefined)}
          >
            Save Changes
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="Booking date"
          hint="Leave blank to use the batch date."
          error={future ? 'The booking date cannot be in the future' : undefined}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              max={today()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          )}
        </Field>
        <Field label="Cost center" hint="Leave blank for the account's cost center (BRNB.108).">
          {(id) => (
            <input
              id={id}
              className="input"
              value={costCenter}
              maxLength={20}
              onChange={(e) => setCostCenter(e.target.value.toUpperCase())}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

interface ConfirmProps {
  title: string;
  message: string;
  confirmLabel: string;
  busy: boolean;
  error: unknown;
  onConfirm: () => void;
  onClose: () => void;
}

/** Confirmation of an action without parameters (cancel the batch). */
export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<ConfirmProps>) {
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Keep Batch
          </Button>
          <Button variant="danger" busy={busy} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>{message}</p>
      </div>
    </Modal>
  );
}
