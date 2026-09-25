import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PeriodSelectors } from './PeriodSelectors';
import { closeControlsApi } from './closeControlsApi';
import type { BooksCutoff } from './closeControlsApi';
import type { usePeriodPicker } from './usePeriodPicker';

type Mode = 'close' | 'reopen';

const TEXT: Record<Mode, { title: string; action: string; variant: 'accent' | 'danger' }> = {
  close: { title: 'Close the broking books', action: 'Close Books', variant: 'accent' },
  reopen: { title: 'Reopen the broking books', action: 'Reopen Books', variant: 'danger' },
};

interface Props {
  mode: Mode;
  open: boolean;
  picker: ReturnType<typeof usePeriodPicker>;
  onDone: (cutoff: BooksCutoff) => void;
  onClose: () => void;
}

function PendingNotice({ items }: Readonly<{ items: string[] }>) {
  const nothing = items.length === 0;
  return (
    <div className={nothing ? 'alert info' : 'alert warning'} role="status">
      {nothing
        ? 'Nothing is reported pending in the broking modules for this period.'
        : `Pending at cut-off: ${items.join('; ')}`}
      <br />
      From now on, postings of booking, Operations, cashiering, remittance and disbursement into
      this period are refused; the GL stays open for FRBS adjustments.
    </div>
  );
}

/**
 * Closes the broking books of a period now, showing what is still pending, or reopens them with a
 * reason (FRBS 3.4.0 / 3.4.1).
 */
export function BooksDialog({ mode, open, picker, onDone, onClose }: Readonly<Props>) {
  const { companyId } = picker;
  const periodId = picker.period?.id ?? 0;
  const [reason, setReason] = useState('');
  const [checked, setChecked] = useState(false);
  const closing = mode === 'close';
  const pending = useQuery({
    queryKey: ['broking-pending', companyId, periodId],
    queryFn: () => closeControlsApi.pending(companyId, periodId),
    enabled: open && closing && periodId > 0,
  });
  const run = useMutation({
    mutationFn: () =>
      closing
        ? closeControlsApi.closeBooks(companyId, periodId)
        : closeControlsApi.reopenBooks(companyId, periodId, reason.trim()),
    onSuccess: (b) => {
      setReason('');
      setChecked(false);
      onDone(b);
    },
  });
  const missingReason = !closing && reason.trim() === '';
  const submit = () => {
    setChecked(true);
    if (!missingReason) {
      run.mutate();
    }
  };

  return (
    <Modal
      title={TEXT[mode].title}
      open={open}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant={TEXT[mode].variant}
            busy={run.isPending}
            disabled={periodId === 0}
            onClick={submit}
          >
            {TEXT[mode].action}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={pending.error ?? run.error} />
        <div className="form-grid">
          <PeriodSelectors picker={picker} />
        </div>
        {closing ? (
          <PendingNotice items={pending.data ?? []} />
        ) : (
          <Field
            label="Reason"
            required
            error={checked && missingReason ? 'Enter the reason' : undefined}
          >
            {(id) => (
              <textarea
                id={id}
                className="textarea"
                maxLength={300}
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
