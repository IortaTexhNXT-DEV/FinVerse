import { changeReasonText, passwordProblems, policyHint } from './passwordRules';
import { KEEP_ALIVE_MS, keepAliveDue } from '@/auth/useServerKeepAlive';
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

describe('password policy text', () => {
  it('words the server rules', () => {
    expect(policyHint({ historyCount: 8, minAgeDays: 1, maxAgeDays: 90 })).toBe(
      'It must differ from your last 8 passwords; it can be changed once within a day; it expires after 90 days.',
    );
    expect(policyHint({ historyCount: 0, minAgeDays: 2, maxAgeDays: 0 })).toBe(
      'It can be changed once within 2 days.',
    );
    expect(policyHint({ historyCount: 0, minAgeDays: 0, maxAgeDays: 0 })).toBe('');
  });

  it('explains a due change', () => {
    expect(changeReasonText('RESET')).toContain('set by an administrator');
    expect(changeReasonText('EXPIRED', 90)).toBe(
      'Your password is older than 90 days and has expired. Choose a new password to continue.',
    );
    expect(changeReasonText('EXPIRED')).toContain('older and has expired');
  });
});

describe('keep-alive', () => {
  it('is due once the interval has passed', () => {
    expect(keepAliveDue(0, KEEP_ALIVE_MS)).toBe(true);
    expect(keepAliveDue(0, KEEP_ALIVE_MS - 1)).toBe(false);
    expect(keepAliveDue(100, 150, 50)).toBe(true);
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
