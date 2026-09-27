import type { CycleStep, ProgrammeView, SendRaResult } from '@/api/eb';
import { ebApi } from '@/api/eb';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ItemDialog } from '../pending/ItemDialogs';
import { ebLabel } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';
import { FeedbackDialog, OpenCycleDialog } from './CycleDialogs';
import type { FeedbackValue, OpenCycleValue } from './CycleDialogs';
import { ProfileDialog } from './ProgrammeDialogs';
import { stepLabel, STEP_ACTIONS } from './programmeView';

/** The dialog open on the programme page. */
export type ProgrammeDialog =
  'profile' | 'cycle' | 'feedback' | 'ra' | 'item' | { step: CycleStep };

/**
 * The dialogs of the programme page actions: Edit Programme, Open Cycle, Record Feedback, Send RA,
 * the requirement steps of the current cycle and Add Pending Item.
 */
export function ProgrammeActionDialogs({
  programme: p,
  dialog,
  onClose: close,
}: Readonly<{ programme: ProgrammeView; dialog: ProgrammeDialog; onClose: () => void }>) {
  const id = p.id;
  const current = p.cycles.find((c) => c.id === p.currentCycleId);
  const profile = useEbMutation(
    (c, v: Parameters<typeof ebApi.updateProfile>[2]) => ebApi.updateProfile(c, id, v),
    'Programme saved',
    close,
  );
  const open = useEbMutation(
    (c, v: OpenCycleValue) => ebApi.openCycle(c, id, v),
    'Cycle opened',
    close,
  );
  const step = useEbMutation(
    (c, v: { cycleId: number; step: CycleStep }) => ebApi.step(c, v.cycleId, v.step),
    (p: ProgrammeView) => `Cycle moved to ${ebLabel(p.cycles[0]?.stage ?? '')}`,
    close,
  );
  const feedback = useEbMutation(
    (c, v: { cycleId: number; value: FeedbackValue }) =>
      ebApi.recordFeedback(c, v.cycleId, v.value, v.value.files),
    'Feedback recorded',
    close,
  );
  const sendRa = useEbMutation<undefined, SendRaResult[]>(
    (c: number) => ebApi.sendRa(c, [id]),
    (r) => (r[0]?.sent ? `Renewal advice sent (${r[0].cycleNo ?? ''})` : (r[0]?.message ?? '')),
    close,
  );
  return (
    <>
      {dialog === 'profile' && (
        <ProfileDialog
          programme={p}
          busy={profile.isPending}
          error={profile.error}
          onClose={close}
          onSave={(v) => profile.mutate(v)}
        />
      )}
      {dialog === 'cycle' && (
        <OpenCycleDialog
          renewalAllowed={p.renewalEligible}
          busy={open.isPending}
          error={open.error}
          onClose={close}
          onSave={(v) => open.mutate(v)}
        />
      )}
      {dialog === 'feedback' && current && (
        <FeedbackDialog
          cycleNo={current.cycleNo}
          busy={feedback.isPending}
          error={feedback.error}
          onClose={close}
          onSave={(value) => feedback.mutate({ cycleId: current.id, value })}
        />
      )}
      {dialog === 'ra' && (
        <ConfirmDialog
          title="Send Renewal Advice"
          record={p.programmeNo}
          effect="The renewal advice is e-mailed, password-protected, to the HR contacts that receive it, and the cycle moves to Renewal Advice Sent."
          confirmLabel="Send RA"
          busy={sendRa.isPending}
          error={sendRa.error}
          onConfirm={() => sendRa.mutate(undefined)}
          onClose={close}
        />
      )}
      {typeof dialog === 'object' && current && (
        <ConfirmDialog
          title={stepLabel(dialog.step)}
          record={current.cycleNo}
          effect={STEP_ACTIONS.find((s) => s.step === dialog.step)?.effect ?? ''}
          confirmLabel={stepLabel(dialog.step)}
          busy={step.isPending}
          error={step.error}
          onConfirm={() => step.mutate({ cycleId: current.id, step: dialog.step })}
          onClose={close}
        />
      )}
      {dialog === 'item' && <ItemDialog programmeId={p.id} onClose={close} />}
    </>
  );
}
