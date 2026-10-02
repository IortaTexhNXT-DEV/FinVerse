/**
 * Inactivity tracking for automatic sign-out. Pure logic (no React, no DOM) so it can be unit
 * tested with fake timers; SessionTimeoutGuard wires it to user activity events.
 */

export type IdlePhase = 'active' | 'warning' | 'expired';

/** Phase after `now - lastActivity` milliseconds without activity. */
export function idlePhase(
  lastActivity: number,
  now: number,
  timeoutMs: number,
  warningMs: number,
): IdlePhase {
  const idle = now - lastActivity;
  if (idle >= timeoutMs) {
    return 'expired';
  }
  return idle >= timeoutMs - warningMs ? 'warning' : 'active';
}

/** Whole seconds left before sign-out (never negative). */
export function secondsRemaining(lastActivity: number, now: number, timeoutMs: number): number {
  return Math.max(0, Math.ceil((timeoutMs - (now - lastActivity)) / 1000));
}

export interface IdleTrackerOptions {
  timeoutMs: number;
  warningMs: number;
  /** Called when the phase changes, and every tick while the warning is shown. */
  onChange: (phase: IdlePhase, secondsLeft: number) => void;
  now?: () => number;
  tickMs?: number;
}

export interface IdleTracker {
  start: () => void;
  stop: () => void;
  /** Passive activity (mouse, keyboard): resets the timer unless the warning is already shown. */
  activity: () => void;
  /** Explicit "stay signed in": always resets the timer. */
  extend: () => void;
  phase: () => IdlePhase;
}

/** Creates a tracker; call `start()` to begin ticking. */
export function createIdleTracker(options: IdleTrackerOptions): IdleTracker {
  const now = options.now ?? Date.now;
  const tickMs = options.tickMs ?? 1000;
  let last = now();
  let phase: IdlePhase = 'active';
  let timer: ReturnType<typeof setInterval> | undefined;

  const stop = () => {
    if (timer !== undefined) {
      clearInterval(timer);
      timer = undefined;
    }
  };

  const tick = () => {
    const current = now();
    const next = idlePhase(last, current, options.timeoutMs, options.warningMs);
    if (next !== phase || next === 'warning') {
      phase = next;
      options.onChange(next, secondsRemaining(last, current, options.timeoutMs));
    }
    if (next === 'expired') {
      stop();
    }
  };

  return {
    start: () => {
      stop();
      timer = setInterval(tick, tickMs);
    },
    stop,
    activity: () => {
      if (phase === 'active') {
        last = now();
      }
    },
    extend: () => {
      last = now();
      if (phase !== 'active') {
        phase = 'active';
        options.onChange('active', secondsRemaining(last, last, options.timeoutMs));
      }
    },
    phase: () => phase,
  };
}
