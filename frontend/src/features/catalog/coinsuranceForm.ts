import type { ParticipationInput } from '@/api/insurerPage';

/** Whether a participation form can be saved. */
export function participationReady(form: Partial<ParticipationInput>): boolean {
  return (
    Boolean(form.participantCode) &&
    form.sharePct !== undefined &&
    form.sharePct > 0 &&
    form.sharePct <= 100 &&
    Boolean(form.effectiveFrom)
  );
}
