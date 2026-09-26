import { describe, expect, it } from 'vitest';
import { ageText } from './age';

describe('ageText', () => {
  const now = new Date('2026-09-24T12:00:00Z').getTime();
  it('uses minutes, hours then days', () => {
    expect(ageText('2026-09-24T11:15:00Z', now)).toBe('45 min');
    expect(ageText('2026-09-24T07:00:00Z', now)).toBe('5 h');
    expect(ageText('2026-09-21T11:00:00Z', now)).toBe('3 d');
  });
  it('never shows a negative age', () => {
    expect(ageText('2026-09-25T12:00:00Z', now)).toBe('0 min');
  });
});
