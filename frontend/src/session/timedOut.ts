/**
 * Marker of a session that ended by inactivity in this tab (BDOI FRS FRUM.001.04): the sign-in
 * gate then opens the "Your session timed out" page instead of the Login page. Kept in the tab's
 * session storage; without storage the Login page opens as before.
 */

const KEY = 'brokerverse.sessionTimedOut';

/** Remembers that the session of this tab timed out. */
export function markTimedOut(): void {
  try {
    sessionStorage.setItem(KEY, '1');
  } catch {
    // Storage unavailable: the Login page opens instead.
  }
}

/** Whether the session of this tab timed out and the user has not gone back to the Login page. */
export function hasTimedOut(): boolean {
  try {
    return sessionStorage.getItem(KEY) === '1';
  } catch {
    return false;
  }
}

/** Forgets the time-out (the user goes on to the Login page). */
export function clearTimedOut(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // Nothing to clear.
  }
}

/** BDOI's text of the inactivity warning (FRUM.001.03). */
export function inactivityText(minutes: number): string {
  return `You have been inactive for ${String(minutes)} ${minutes === 1 ? 'minute' : 'minutes'}. For your security, your session is about to expire. Would you like to stay logged in or log out?`;
}

/** BDOI's text after the inactivity sign-out (FRUM.001.04). */
export const TIMED_OUT_TEXT = 'Your session timed out. Please log in again to continue';
