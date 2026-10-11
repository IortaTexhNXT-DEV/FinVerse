import { describe, expect, it } from 'vitest';
import { ageText } from './age';

describe('ageText', () => {
  const now = new Date('2026-09-24T12:00:00Z').getTime();
  it('uses minutes, hours then whole days, in words', () => {
    expect(ageText('2026-09-24T11:15:00Z', now)).toBe('45 min');
    expect(ageText('2026-09-24T07:00:00Z', now)).toBe('5 hrs');
    expect(ageText('2026-09-24T11:00:00Z', now)).toBe('1 hr');
    expect(ageText('2026-09-23T11:00:00Z', now)).toBe('1 day');
    expect(ageText('2026-09-21T11:00:00Z', now)).toBe('3 days');
    expect(ageText('2026-09-02T11:00:00Z', now)).toBe('22 days');
  });
  it('never shows a negative age or a one-letter unit', () => {
    expect(ageText('2026-09-25T12:00:00Z', now)).toBe('0 min');
    expect(ageText('2026-09-02T11:00:00Z', now)).not.toMatch(/\d d$/);
  });
});
