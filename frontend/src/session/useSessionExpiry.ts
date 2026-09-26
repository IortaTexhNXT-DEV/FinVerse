import { useEffect, useState } from 'react';
import { tokenStore } from '@/api/client';
import { expiryPhase, minutesLeft } from './sessionExpiry';
import type { ExpiryPhase } from './sessionExpiry';

const TICK_MS = 15_000;

export interface SessionExpiry {
  phase: ExpiryPhase;
  minutesLeft: number;
  /** Hides the warning until the session ends. */
  dismissed: boolean;
  dismiss: () => void;
}

/**
 * Watches the absolute end of the session (token expiry, BRNB.040): warns the configured minutes
 * before and calls {@code onExpired} at the end.
 */
export function useSessionExpiry(warningMinutes: number, onExpired: () => void): SessionExpiry {
  const [now, setNow] = useState(() => Date.now());
  const [dismissed, setDismissed] = useState(false);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), TICK_MS);
    return () => clearInterval(timer);
  }, []);
  const expiresAt = tokenStore.expiresAt();
  const phase = expiryPhase(expiresAt, now, warningMinutes);
  useEffect(() => {
    if (phase === 'expired') {
      onExpired();
    }
  }, [phase, onExpired]);
  return {
    phase,
    minutesLeft: expiresAt === null ? 0 : minutesLeft(expiresAt, now),
    dismissed,
    dismiss: () => setDismissed(true),
  };
}
