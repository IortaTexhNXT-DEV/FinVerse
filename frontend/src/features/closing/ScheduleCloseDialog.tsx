import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { PeriodSelectors } from './PeriodSelectors';
import { closeControlsApi } from './closeControlsApi';
import type { CloseSchedule } from './closeControlsApi';
import { closeGuards, fromLocalInput, toLocalInput } from './closeTimes';
import type { usePeriodPicker } from './usePeriodPicker';

interface Props {
  open: boolean;
  picker: ReturnType<typeof usePeriodPicker>;
  onDone: (schedule: CloseSchedule) => void;
  onClose: () => void;
}

/** The chosen close time: the proposal of the period until the user picks another. */
function useCloseTime(companyId: number, periodId: number, open: boolean) {
  const [when, setWhen] = useState('');
  const proposal = useQuery({
    queryKey: ['close-proposal', companyId, periodId],
    queryFn: () => closeControlsApi.proposal(companyId, periodId),
    enabled: open && periodId > 0,
  });
  const chosen = when === '' ? toLocalInput(proposal.data?.scheduledAt) : when;
  return { chosen, setWhen, proposalError: proposal.error };
}

/**
 * Schedules the close of a period for a date and time, proposed as the 2nd banking day of the
 * next month at 17:00 (FRBS 2.6.0), or closes it now; only the previous month may be closed.
 */
export function ScheduleCloseDialog({ open, picker, onDone, onClose }: Readonly<Props>) {
  const { companyId } = picker;
  const periodId = picker.period?.id ?? 0;
  const { chosen, setWhen, proposalError } = useCloseTime(companyId, periodId, open);
  const guards = closeGuards(picker.period?.endDate, chosen, today());
  const schedule = useMutation({
    mutationFn: () => closeControlsApi.schedule(companyId, periodId, fromLocalInput(chosen)),
    onSuccess: onDone,
  });
  const closeNow = useMutation({
    mutationFn: () => closeControlsApi.closeNow(companyId, periodId),
    onSuccess: onDone,
  });

  return (
    <Modal
      title="Schedule month-end close"
      open={open}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="secondary"
            busy={closeNow.isPending}
            disabled={periodId === 0 || guards.now !== undefined}
            title={guards.now}
            onClick={() => closeNow.mutate()}
          >
            Close Now
          </Button>
          <Button
            variant="accent"
            busy={schedule.isPending}
            disabled={periodId === 0 || chosen === '' || guards.schedule !== undefined}
            onClick={() => schedule.mutate()}
          >
            Schedule Close
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={proposalError ?? schedule.error ?? closeNow.error} />
        <div className="form-grid">
          <PeriodSelectors picker={picker} />
        </div>
        <Field
          label="Close on"
          required
          error={guards.schedule}
          hint="Proposed: the 2nd banking day of the next month, 17:00. The checklist runs at that time."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type="datetime-local"
              value={chosen}
              onChange={(e) => setWhen(e.target.value)}
            />
          )}
        </Field>
        <p className="muted">
          At the chosen time the period-end checklist runs; when a control fails the close is
          recorded as failed with the blocking items and the GL_CLOSE_FAILED alert is raised.
        </p>
      </div>
    </Modal>
  );
}
