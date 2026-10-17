import { describe, expect, it } from 'vitest';
import { readSchedule, scheduleRule, scheduleText } from './schedule';

describe('schedules in words', () => {
  it('reads the stored rules in Philippine time', () => {
    expect(scheduleText('-')).toBe('Manual');
    expect(scheduleText('0 30 0 * * *')).toBe('Every day at 08:30');
    expect(scheduleText('0 0 10 * * MON-FRI')).toBe('Monday to Friday at 18:00');
    expect(scheduleText('0 15 * * * *')).toBe('Every hour at :15');
    expect(scheduleText('0 0/5 * * * *')).toBe('Set by the System Administrator');
  });

  it('writes the rule of a schedule chosen on screen', () => {
    expect(scheduleRule({ frequency: 'DAILY', time: '08:30' })).toBe('0 30 0 * * *');
    expect(scheduleRule({ frequency: 'WEEKDAYS', time: '18:00' })).toBe('0 0 10 * * MON-FRI');
    expect(scheduleRule({ frequency: 'MANUAL', time: '' })).toBe('-');
    expect(scheduleText(scheduleRule({ frequency: 'WEEKDAYS', time: '06:00' }))).toBe(
      'Monday to Friday at 06:00',
    );
    expect(readSchedule(scheduleRule({ frequency: 'DAILY', time: '06:00' }))).toEqual({
      frequency: 'DAILY',
      time: '06:00',
    });
  });
});
