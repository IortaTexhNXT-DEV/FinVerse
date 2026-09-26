import { useEffect } from 'react';
import { api } from '@/api/client';

/** User activity that keeps the server session alive. */
const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'wheel', 'touchstart'] as const;

/**
 * At most this often, activity without any server call tells the server the user is still there,
 * so the session sweep does not end the session of someone typing a long form (UAM-NFR-35).
 */
export const KEEP_ALIVE_MS = 4 * 60_000;

/** Whether a keep-alive is due (pure, for tests). */
export function keepAliveDue(lastSent: number, now: number, intervalMs = KEEP_ALIVE_MS): boolean {
  return now - lastSent >= intervalMs;
}

/** Records the user's activity on the server while signed in (throttled). */
export function useServerKeepAlive(signedIn: boolean): void {
  useEffect(() => {
    if (!signedIn) {
      return undefined;
    }
    let lastSent = Date.now();
    const onActivity = () => {
      const now = Date.now();
      if (keepAliveDue(lastSent, now)) {
        lastSent = now;
        api.get('/auth/me').catch(() => undefined);
      }
    };
    ACTIVITY_EVENTS.forEach((e) => window.addEventListener(e, onActivity, { passive: true }));
    return () => {
      ACTIVITY_EVENTS.forEach((e) => window.removeEventListener(e, onActivity));
    };
  }, [signedIn]);
}
