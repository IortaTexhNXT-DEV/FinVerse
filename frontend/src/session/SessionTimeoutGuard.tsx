import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { tokenStore } from '@/api/client';
import { systemApi } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { createIdleTracker } from './idleTimer';
import type { IdlePhase, IdleTracker } from './idleTimer';
import { tabSession } from './tabSession';
import { useSessionExpiry } from './useSessionExpiry';

const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'wheel', 'touchstart', 'scroll'] as const;
const DEFAULT_TIMEOUT_MINUTES = 30;
const DEFAULT_WARNING_SECONDS = 900;
const DEFAULT_EXPIRY_WARNING_MINUTES = 30;
/** Activity is shared with the other tabs at most this often. */
const SHARE_ACTIVITY_MS = 30_000;

function ExpiryWarning({ minutes, onClose }: Readonly<{ minutes: number; onClose: () => void }>) {
  return (
    <Modal
      title="Your session will end soon"
      open
      onClose={onClose}
      footer={
        <Button variant="accent" onClick={onClose}>
          I understand
        </Button>
      }
    >
      <p role="timer" aria-live="polite">
        For your security the system signs you out in <strong>{minutes}</strong> minute(s), at{' '}
        {formatDateTime(tokenStore.expiresAt())}, whatever your activity. Save your work; you can
        sign in again afterwards.
      </p>
    </Modal>
  );
}

/**
 * Session policy (BRNB.040/082): warns after the configured inactivity (SESSION_IDLE_WARNING_MINUTES)
 * and signs out at SESSION_TIMEOUT_MINUTES; warns SESSION_EXPIRY_WARNING_MINUTES before the
 * system-triggered sign-out at the token expiry. Activity in any tab keeps every tab signed in.
 */
export function SessionTimeoutGuard() {
  const { logout } = useAuth();
  const toast = useToast();
  const policy = useQuery({
    queryKey: ['session-policy'],
    queryFn: systemApi.sessionPolicy,
    staleTime: Infinity,
  });
  const [phase, setPhase] = useState<IdlePhase>('active');
  const [secondsLeft, setSecondsLeft] = useState(0);
  const tracker = useRef<IdleTracker | null>(null);

  const timeoutMinutes = policy.data?.timeoutMinutes ?? DEFAULT_TIMEOUT_MINUTES;
  const warningSeconds = policy.data?.warningSeconds ?? DEFAULT_WARNING_SECONDS;
  const expiryWarning = policy.data?.expiryWarningMinutes ?? DEFAULT_EXPIRY_WARNING_MINUTES;

  const onExpired = useCallback(() => {
    toast.error('Your session has ended. Please sign in again.');
    logout();
  }, [logout, toast]);
  const expiry = useSessionExpiry(expiryWarning, onExpired);

  useEffect(() => {
    const sync = tabSession();
    let lastShared = 0;
    const instance = createIdleTracker({
      timeoutMs: timeoutMinutes * 60_000,
      warningMs: warningSeconds * 1000,
      onChange: (next, left) => {
        setPhase(next);
        setSecondsLeft(left);
        if (next === 'expired') {
          toast.error(`You were signed out after ${String(timeoutMinutes)} minutes of inactivity.`);
          logout();
        }
      },
    });
    const onActivity = () => {
      instance.activity();
      const now = Date.now();
      if (now - lastShared > SHARE_ACTIVITY_MS) {
        lastShared = now;
        sync.announceActivity(false);
      }
    };
    const unsubscribe = sync.subscribe((message) => {
      if (message.type === 'activity') {
        if (message.extend) {
          instance.extend();
        } else {
          instance.activity();
        }
      }
    });
    ACTIVITY_EVENTS.forEach((e) => window.addEventListener(e, onActivity, { passive: true }));
    instance.start();
    tracker.current = instance;
    return () => {
      instance.stop();
      unsubscribe();
      ACTIVITY_EVENTS.forEach((e) => window.removeEventListener(e, onActivity));
    };
  }, [timeoutMinutes, warningSeconds, logout, toast]);

  const stay = () => {
    tracker.current?.extend();
    tabSession().announceActivity(true);
  };

  return (
    <>
      <Modal
        title="Your session is about to expire"
        open={phase === 'warning'}
        onClose={stay}
        footer={
          <>
            <Button variant="secondary" onClick={logout}>
              Sign out now
            </Button>
            <Button variant="accent" onClick={stay}>
              Stay signed in
            </Button>
          </>
        }
      >
        <p role="timer" aria-live="polite">
          You have been inactive for a while. For your security you will be signed out in{' '}
          <strong>{secondsLeft}</strong> seconds.
        </p>
      </Modal>
      {expiry.phase === 'warning' && !expiry.dismissed && phase !== 'warning' && (
        <ExpiryWarning minutes={expiry.minutesLeft} onClose={expiry.dismiss} />
      )}
    </>
  );
}
