import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { systemApi } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { createIdleTracker } from './idleTimer';
import type { IdlePhase, IdleTracker } from './idleTimer';

const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'wheel', 'touchstart', 'scroll'] as const;
const DEFAULT_TIMEOUT_MINUTES = 30;
const DEFAULT_WARNING_SECONDS = 60;

/**
 * Signs the user out after the configured period of inactivity (system parameter
 * SESSION_TIMEOUT_MINUTES), showing a countdown dialog shortly before.
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

  useEffect(() => {
    const instance = createIdleTracker({
      timeoutMs: timeoutMinutes * 60_000,
      warningMs: warningSeconds * 1000,
      onChange: (next, left) => {
        setPhase(next);
        setSecondsLeft(left);
        if (next === 'expired') {
          toast.error(`You were signed out after ${timeoutMinutes} minutes of inactivity.`);
          logout();
        }
      },
    });
    const onActivity = () => instance.activity();
    ACTIVITY_EVENTS.forEach((e) => window.addEventListener(e, onActivity, { passive: true }));
    instance.start();
    tracker.current = instance;
    return () => {
      instance.stop();
      ACTIVITY_EVENTS.forEach((e) => window.removeEventListener(e, onActivity));
    };
  }, [timeoutMinutes, warningSeconds, logout, toast]);

  return (
    <Modal
      title="Your session is about to expire"
      open={phase === 'warning'}
      onClose={() => tracker.current?.extend()}
      footer={
        <>
          <Button variant="secondary" onClick={logout}>
            Sign out now
          </Button>
          <Button variant="accent" onClick={() => tracker.current?.extend()}>
            Stay signed in
          </Button>
        </>
      }
    >
      <p role="timer" aria-live="polite">
        For your security you will be signed out in <strong>{secondsLeft}</strong> seconds because
        of inactivity.
      </p>
    </Modal>
  );
}
