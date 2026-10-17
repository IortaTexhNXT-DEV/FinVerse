import { createIdleTracker, idlePhase, secondsRemaining } from './idleTimer';

const MINUTE = 60_000;

describe('idle phase', () => {
  it('moves from active to warning to expired', () => {
    expect(idlePhase(0, 10 * MINUTE, 30 * MINUTE, MINUTE)).toBe('active');
    expect(idlePhase(0, 29 * MINUTE, 30 * MINUTE, MINUTE)).toBe('warning');
    expect(idlePhase(0, 30 * MINUTE, 30 * MINUTE, MINUTE)).toBe('expired');
  });

  it('counts remaining seconds without going negative', () => {
    expect(secondsRemaining(0, 29 * MINUTE + 500, 30 * MINUTE)).toBe(60);
    expect(secondsRemaining(0, 31 * MINUTE, 30 * MINUTE)).toBe(0);
  });
});

describe('idle tracker', () => {
  let clock = 0;
  const now = () => clock;

  beforeEach(() => {
    vi.useFakeTimers();
    clock = 0;
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  const advance = (ms: number) => {
    clock += ms;
    vi.advanceTimersByTime(ms);
  };

  it('warns, then expires and stops ticking', () => {
    const onChange = vi.fn();
    const tracker = createIdleTracker({ timeoutMs: 5000, warningMs: 2000, onChange, now });
    tracker.start();
    advance(2000);
    expect(onChange).not.toHaveBeenCalled();
    advance(1000);
    expect(onChange).toHaveBeenLastCalledWith('warning', 2);
    advance(1000);
    expect(onChange).toHaveBeenLastCalledWith('warning', 1);
    advance(1000);
    expect(onChange).toHaveBeenLastCalledWith('expired', 0);
    expect(tracker.phase()).toBe('expired');
    const calls = onChange.mock.calls.length;
    advance(5000);
    expect(onChange.mock.calls.length).toBe(calls);
  });

  it('activity keeps the session alive but is ignored once the warning shows', () => {
    const onChange = vi.fn();
    const tracker = createIdleTracker({ timeoutMs: 5000, warningMs: 2000, onChange, now });
    tracker.start();
    advance(2000);
    tracker.activity();
    advance(2000);
    expect(tracker.phase()).toBe('active');
    advance(1000);
    expect(tracker.phase()).toBe('warning');
    tracker.activity();
    advance(2000);
    expect(tracker.phase()).toBe('expired');
    tracker.stop();
  });

  it('extend resets a warning back to active', () => {
    const onChange = vi.fn();
    const tracker = createIdleTracker({ timeoutMs: 5000, warningMs: 2000, onChange, now });
    tracker.start();
    advance(3000);
    expect(tracker.phase()).toBe('warning');
    tracker.extend();
    expect(onChange).toHaveBeenLastCalledWith('active', 5);
    advance(2000);
    expect(tracker.phase()).toBe('active');
    tracker.extend();
    tracker.stop();
    tracker.stop();
  });

  it('uses the system clock by default', () => {
    const tracker = createIdleTracker({ timeoutMs: 1000, warningMs: 500, onChange: vi.fn() });
    tracker.start();
    tracker.start();
    expect(tracker.phase()).toBe('active');
    tracker.stop();
  });
});
