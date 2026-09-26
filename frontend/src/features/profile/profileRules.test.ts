import { contactProblems, sessionStatus } from './profileRules';
import type { UserSessionEntry } from '@/api/auth';

const session = (patch: Partial<UserSessionEntry>): UserSessionEntry => ({
  sessionId: 's1',
  username: 'requestor',
  issuedAt: '2026-09-26T01:00:00Z',
  lastSeenAt: '2026-09-26T01:05:00Z',
  expiresAt: '2026-09-26T09:00:00Z',
  open: false,
  ...patch,
});

describe('contact details', () => {
  it('accepts an address and an optional mobile number', () => {
    expect(contactProblems({ email: 'a013000101@bdo.ph', mobileNo: '' })).toEqual({});
    expect(contactProblems({ email: 'a013000101@bdo.ph', mobileNo: '+63 917 123 4567' })).toEqual(
      {},
    );
  });

  it('explains what is wrong per field', () => {
    expect(contactProblems({ email: ' ', mobileNo: 'abc' })).toEqual({
      email: 'Enter your e-mail address',
      mobileNo: 'Enter the mobile number with digits only, for example +63 917 123 4567',
    });
    expect(contactProblems({ email: 'someone', mobileNo: '' }).email).toBe(
      'someone is not a valid e-mail address',
    );
  });
});

describe('session status', () => {
  it('shows open sessions and why the others ended', () => {
    expect(sessionStatus(session({ open: true }))).toBe('OPEN');
    expect(sessionStatus(session({ endReason: 'IDLE_TIMEOUT' }))).toBe('IDLE_TIMEOUT');
    expect(sessionStatus(session({}))).toBe('EXPIRED');
  });
});
