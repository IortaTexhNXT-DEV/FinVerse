import { describe, expect, it, vi } from 'vitest';
import { CLOSE_JOBS_NOTE, scheduleRowActions } from './rowActions';

describe('scheduled close row menu', () => {
  it('withdraws a scheduled close in red after a confirmation', () => {
    const withdraw = vi.fn();
    const actions = scheduleRowActions(
      { status: 'SCHEDULED', periodName: 'Sep 2026' },
      true,
      withdraw,
    );
    expect(actions.map((a) => a.label)).toEqual(['Withdraw']);
    expect(actions[0]?.danger).toBe(true);
    expect(actions[0]?.confirm?.destructive).toBe(true);
    actions[0]?.onSelect('');
    expect(withdraw).toHaveBeenCalled();
  });

  it('offers nothing on a close that ran or to a user who may not schedule', () => {
    const withdraw = vi.fn();
    expect(
      scheduleRowActions({ status: 'COMPLETED', periodName: 'Aug 2026' }, true, withdraw),
    ).toEqual([]);
    expect(
      scheduleRowActions({ status: 'SCHEDULED', periodName: 'Sep 2026' }, false, withdraw),
    ).toEqual([]);
  });

  it('describes the scheduled jobs without job codes', () => {
    expect(CLOSE_JOBS_NOTE).not.toMatch(/[A-Z]_[A-Z]/);
  });
});
