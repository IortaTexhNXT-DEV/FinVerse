import { Check, CornerUpLeft, X } from 'lucide-react';
import { Fragment } from 'react';
import { titleCase } from '@/utils/format';
import type { Step, StepState } from './stageSteps';

/** Words read by screen readers for each step state. */
const STATE_TEXT: Record<StepState, string> = {
  done: 'completed',
  current: 'current stage',
  returned: 'current stage, returned',
  ended: 'current stage, closed',
  complete: 'current stage, completed',
  upcoming: 'not started',
};

/** The marker of a step: a check when passed, the step number otherwise. */
function Marker({ step, number }: Readonly<{ step: Step; number: number }>) {
  if (step.state === 'done' || step.state === 'complete') {
    return <Check size={14} strokeWidth={3} aria-hidden="true" />;
  }
  if (step.state === 'ended') {
    return <X size={14} strokeWidth={3} aria-hidden="true" />;
  }
  if (step.state === 'returned') {
    return <CornerUpLeft size={14} strokeWidth={3} aria-hidden="true" />;
  }
  return <span aria-hidden="true">{number}</span>;
}

/**
 * Horizontal stage stepper of a record's workflow header: passed stages with a check, the current
 * stage highlighted (amber when returned, red when the record ended off the normal path, green at
 * the normal end), stages ahead in grey. On narrow widths only the first and last stages, the
 * current stage and its neighbours stay; the others collapse into an ellipsis.
 */
export function StageStepper({
  steps,
  label = 'Workflow stages',
}: Readonly<{ steps: readonly Step[]; label?: string }>) {
  if (steps.length === 0) {
    return null;
  }
  const at = steps.findIndex((s) => s.state !== 'done' && s.state !== 'upcoming');
  return (
    <div className="stage-stepper-wrap">
      <ol className="stage-stepper" aria-label={label}>
        {steps.map((step, i) => {
          const collapsedBefore = !step.near && (i === 0 || (steps[i - 1]?.near ?? false));
          return (
            <Fragment key={step.code}>
              {collapsedBefore && (
                <li className="stage-step stage-gap" aria-hidden="true">
                  <span className="stage-marker">…</span>
                </li>
              )}
              <li
                className={`stage-step is-${step.state}${step.near ? '' : ' is-far'}`}
                aria-current={i === at ? 'step' : undefined}
              >
                <span className="stage-marker">
                  <Marker step={step} number={i + 1} />
                </span>
                <span className="stage-label" title={titleCase(step.name)}>
                  {titleCase(step.name)}
                  <span className="visually-hidden"> ({STATE_TEXT[step.state]})</span>
                </span>
              </li>
            </Fragment>
          );
        })}
      </ol>
    </div>
  );
}
