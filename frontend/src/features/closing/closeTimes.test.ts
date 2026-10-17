import {
  closeGuards,
  fromLocalInput,
  isPreviousMonth,
  joinWhen,
  splitWhen,
  toLocalInput,
} from './closeTimes';

describe('close times', () => {
  it('round-trips a datetime-local value', () => {
    const iso = fromLocalInput('2030-04-02T17:00');
    expect(iso).toBeDefined();
    expect(toLocalInput(iso)).toBe('2030-04-02T17:00');
    expect(fromLocalInput(' ')).toBeUndefined();
    expect(fromLocalInput('not a date')).toBeUndefined();
    expect(toLocalInput(undefined)).toBe('');
    expect(toLocalInput('garbage')).toBe('');
  });

  it('accepts only the previous month (FRBS 2.6.1)', () => {
    expect(isPreviousMonth('2030-03-31', '2030-04-02')).toBe(true);
    expect(isPreviousMonth('2029-12-31', '2030-01-03')).toBe(true);
    expect(isPreviousMonth('2030-03-31', '2030-05-02')).toBe(false);
    expect(isPreviousMonth('2030-04-30', '2030-04-02')).toBe(false);
    expect(isPreviousMonth('x', '2030-04-02')).toBe(false);
  });

  it('explains why a close is refused', () => {
    expect(closeGuards(undefined, '', '2030-04-02')).toEqual({ closeDate: '2030-04-02' });
    const ok = closeGuards('2030-03-31', '2030-04-02T17:00', '2030-04-10');
    expect(ok.schedule).toBeUndefined();
    expect(ok.now).toBeUndefined();
    const late = closeGuards('2030-03-31', '2030-06-02T17:00', '2030-06-01');
    expect(late.schedule).toContain('2030-06');
    expect(late.now).toBeDefined();
  });
});

describe('close date and time kept apart', () => {
  it('splits and joins the close time, never mixing a time into the date', () => {
    expect(splitWhen('2026-11-03T17:00')).toEqual({ date: '2026-11-03', time: '17:00' });
    expect(splitWhen('')).toEqual({ date: '', time: '' });
    expect(joinWhen('2026-10-04', '17:00')).toBe('2026-10-04T17:00');
    expect(joinWhen('', '17:00')).toBe('');
    expect(joinWhen('2026-10-04', '')).toBe('');
  });
});
