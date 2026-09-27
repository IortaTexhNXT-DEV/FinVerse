import type { CycleStep, ProgrammeView } from '@/api/eb';
import { today } from '@/utils/format';

/** The requirement steps of a cycle offered on the programme page (workflow EB_CYCLE). */
export const STEP_ACTIONS: readonly {
  action: string;
  step: CycleStep;
  label: string;
  effect: string;
}[] = [
  {
    action: 'start',
    step: 'start',
    label: 'Start Requirements',
    effect: 'The new-business cycle moves to Client Requirements.',
  },
  {
    action: 'stay_with_incumbent',
    step: 'stay-with-incumbent',
    label: 'Stay with Incumbent',
    effect: 'The renewal continues with the incumbent insurer without remarketing.',
  },
  {
    action: 'remarket',
    step: 'remarket',
    label: 'Go to Market',
    effect:
      'The cycle moves to Franchise to request the insurers. A validated Broker on Record is needed.',
  },
];

/** The label of a step. */
export function stepLabel(step: CycleStep): string {
  return STEP_ACTIONS.find((s) => s.step === step)?.label ?? step;
}

/** The next expiry of the programme's active lines, on or after today. */
export function nextExpiry(p: ProgrammeView, on = today()): string | undefined {
  return p.lines
    .filter((l) => l.active && l.periodTo !== null && l.periodTo !== undefined && l.periodTo >= on)
    .map((l) => l.periodTo ?? '')
    .sort((a, b) => a.localeCompare(b))[0];
}
