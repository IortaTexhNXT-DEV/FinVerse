/**
 * Absolute session end (BRNB.040): the token expires at a fixed time whatever the activity. The
 * user is warned a configured number of minutes before (SESSION_EXPIRY_WARNING_MINUTES) and
 * signed out at the expiry. Pure logic.
 */

export type ExpiryPhase = 'none' | 'warning' | 'expired';

/** Phase of the absolute session end at a time. */
export function expiryPhase(
  expiresAt: string | null | undefined,
  now: number,
  warningMinutes: number,
): ExpiryPhase {
  if (expiresAt === null || expiresAt === undefined) {
    return 'none';
  }
  const end = Date.parse(expiresAt);
  if (Number.isNaN(end)) {
    return 'none';
  }
  if (now >= end) {
    return 'expired';
  }
  return end - now <= warningMinutes * 60_000 ? 'warning' : 'none';
}

/** Whole minutes left before the session ends (at least 1 while it has not ended). */
export function minutesLeft(expiresAt: string, now: number): number {
  return Math.max(1, Math.ceil((Date.parse(expiresAt) - now) / 60_000));
}
