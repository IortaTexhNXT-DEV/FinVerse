/**
 * The steps of a record's workflow stepper, built from the workflow's defined stages: the main
 * path (the stages in their defined order and the normal end), with side stages (returned, on hold,
 * rejected, cancelled, voided...) shown only when the record is in one of them.
 */

/** A stage of the workflow definition, in its defined order. */
export interface StageDef {
  code: string;
  name: string;
  initial?: boolean;
  terminal: boolean;
}

/**
 * done: passed; current: where the record is; returned: current, after being sent back; ended:
 * current, the record left the normal path (rejected, cancelled, voided...); complete: current,
 * the normal end; upcoming: not reached yet.
 */
export type StepState = 'done' | 'current' | 'returned' | 'ended' | 'complete' | 'upcoming';

export interface Step {
  code: string;
  name: string;
  state: StepState;
  /** Kept on narrow widths (first, last, current and its neighbours); others collapse. */
  near: boolean;
}

/** One status change of the record (the last one tells where the record came from). */
export interface StageMove {
  fromStage?: string;
  toStage: string;
}

/**
 * Stages off the normal path: shown only while the record is in them (an endorsement posted with
 * payments still to re-apply is such a detour on the way to Posted).
 */
const SIDE =
  /(^|_)(RETURNED|REJECTED|CANCELLED|CANCEL|VOIDED|DECLINED|WITHDRAWN|INACTIVE|NOT|LOST|ON_HOLD|HOLD|EXTENSION|CORRECTION|REAPPLICATION)(_|$)/;
/** Stages that end the record off the normal path (shown red). */
const EXIT =
  /(^|_)(REJECTED|CANCELLED|VOIDED|DECLINED|WITHDRAWN|INACTIVE|NOT_PROCEEDED|NOT_RENEWED|LOST)(_|$)/;
/** Stages a record is sent back to (shown amber). */
const BACK = /(^|_)(RETURNED|ON_HOLD|HOLD|CORRECTION)(_|$)/;

/** Whether a stage belongs to the normal path of its workflow. */
function onMainPath(stage: StageDef, firstEnd: StageDef | undefined): boolean {
  if (SIDE.test(stage.code)) {
    return false;
  }
  return !stage.terminal || stage === firstEnd;
}

/** The state of the current step when it is off the normal path or at its end. */
function sideState(current: StageDef): StepState {
  if (EXIT.test(current.code)) {
    return 'ended';
  }
  if (BACK.test(current.code)) {
    return 'returned';
  }
  return current.terminal ? 'complete' : 'current';
}

/** The state of a step of the normal path, by its position against the current step. */
function positionState(index: number, at: number, current: StepState): StepState {
  if (index < at) {
    return 'done';
  }
  return index === at ? current : 'upcoming';
}

/** Marks the steps kept on narrow widths: first, last, current and its neighbours. */
function withNear(steps: Omit<Step, 'near'>[]): Step[] {
  const at = steps.findIndex((s) => s.state !== 'done' && s.state !== 'upcoming');
  return steps.map((s, i) => ({
    ...s,
    near: i === 0 || i === steps.length - 1 || (at >= 0 && Math.abs(i - at) <= 1),
  }));
}

/**
 * The stepper of a record: done stages, the current stage (returned, ended or complete when it
 * is), then the stages still ahead. A side stage (returned, rejected...) is placed after the
 * stage the record came from; a path that ended off the normal path shows no stages after it.
 */
export function stageSteps(
  stages: readonly StageDef[],
  currentCode: string,
  history: readonly StageMove[] = [],
): Step[] {
  const firstEnd = stages.find((s) => s.terminal && !SIDE.test(s.code));
  const main = stages.filter((s) => onMainPath(s, firstEnd));
  const current = stages.find((s) => s.code === currentCode);
  if (current === undefined) {
    return withNear(main.map((s) => ({ code: s.code, name: s.name, state: 'upcoming' })));
  }
  const last = history.at(-1);
  const cameFrom = last?.toStage === currentCode ? last.fromStage : undefined;
  return withNear(
    main.includes(current)
      ? mainSteps(main, current, cameFrom)
      : sideSteps(main, current, cameFrom, firstEnd),
  );
}

type BareStep = Omit<Step, 'near'>;

/** The record is on the normal path: done, current (returned when sent back), ahead. */
function mainSteps(main: readonly StageDef[], current: StageDef, cameFrom?: string): BareStep[] {
  const at = main.indexOf(current);
  const sentBack = main.findIndex((s) => s.code === cameFrom) > at;
  let state: StepState = sentBack ? 'returned' : 'current';
  if (current.terminal) {
    state = 'complete';
  }
  return main.map((s, i) => ({ code: s.code, name: s.name, state: positionState(i, at, state) }));
}

/** The record is off the normal path: placed after the stage it came from (or the start). */
function sideSteps(
  main: readonly StageDef[],
  current: StageDef,
  cameFrom: string | undefined,
  firstEnd: StageDef | undefined,
): BareStep[] {
  const state = sideState(current);
  const alternativeEnd = current.terminal && state === 'complete';
  const path = alternativeEnd ? main.filter((s) => s !== firstEnd) : main;
  const doneUpTo = alternativeEnd
    ? path.length - 1
    : Math.max(
        path.findIndex((s) => s.code === cameFrom),
        0,
      );
  const step = (s: StageDef, st: StepState): BareStep => ({
    code: s.code,
    name: s.name,
    state: st,
  });
  const before = path.slice(0, doneUpTo + 1).map((s) => step(s, 'done'));
  const closed = current.terminal || state === 'ended';
  const after = closed ? [] : path.slice(doneUpTo + 1).map((s) => step(s, 'upcoming'));
  return [...before, step(current, state), ...after];
}
