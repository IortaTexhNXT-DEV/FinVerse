import { afterEach, describe, expect, it } from 'vitest';
import {
  clearTimedOut,
  hasTimedOut,
  inactivityText,
  markTimedOut,
  TIMED_OUT_TEXT,
} from './timedOut';

describe('session time-out marker and texts', () => {
  afterEach(() => clearTimedOut());

  it('remembers a timed-out session until the user goes on to the Login page', () => {
    expect(hasTimedOut()).toBe(false);
    markTimedOut();
    expect(hasTimedOut()).toBe(true);
    clearTimedOut();
    expect(hasTimedOut()).toBe(false);
  });

  it("uses BDOI's wording of the inactivity warning with the configured minutes", () => {
    expect(inactivityText(15)).toBe(
      'You have been inactive for 15 minutes. For your security, your session is about to expire. Would you like to stay logged in or log out?',
    );
    expect(inactivityText(1)).toContain('inactive for 1 minute.');
  });

  it("uses BDOI's wording after the inactivity sign-out", () => {
    expect(TIMED_OUT_TEXT).toBe('Your session timed out. Please log in again to continue');
  });
});
