import { passwordProblems } from './passwordRules';
import { ageInDays, oldestAge } from '@/features/approvals/age';

describe('password rules', () => {
  it('accepts a strong matching password', () => {
    expect(passwordProblems('Brokerverse@2027', 'Brokerverse@2027')).toEqual([]);
  });

  it('lists every unmet rule', () => {
    expect(passwordProblems('abc', 'abd')).toEqual([
      'At least 10 characters',
      'An upper-case letter',
      'A digit',
      'A symbol',
      'Both new passwords must match',
    ]);
    expect(passwordProblems('ABCDEFGHIJ1!', 'ABCDEFGHIJ1!')).toEqual(['A lower-case letter']);
  });
});

describe('approval age', () => {
  const now = Date.parse('2026-09-23T12:00:00Z');

  it('counts whole days waiting', () => {
    expect(ageInDays('2026-09-20T11:00:00Z', now)).toBe(3);
    expect(ageInDays(undefined, now)).toBe(0);
    expect(ageInDays('2026-09-24T00:00:00Z', now)).toBe(0);
    expect(oldestAge(['2026-09-22T12:00:00Z', '2026-09-13T12:00:00Z', undefined], now)).toBe(10);
    expect(oldestAge([], now)).toBe(0);
  });
});
