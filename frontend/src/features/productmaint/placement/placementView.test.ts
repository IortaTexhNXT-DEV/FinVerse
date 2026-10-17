import { describe, expect, it } from 'vitest';
import { scheduleText } from './placementView';

describe('Placement Update Report schedule', () => {
  it('reads the schedule in words', () => {
    expect(
      scheduleText({
        day: 'THURSDAY',
        time: '08:00',
        periodDays: 7,
        scope: 'QUOTATION',
        nextRun: '2026-10-15T08:00:00',
      }),
    ).toBe('Every Thursday at 08:00, reporting period of 7 days');
  });
});
