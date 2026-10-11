import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { tokenStore } from '@/api/client';
import { systemApi } from '@/api/system';
import type { SessionPolicy } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { authApi } from '@/api/auth';
import { formatDateTime } from '@/utils/format';
import { createIdleTracker } from './idleTimer';
import type { IdlePhase, IdleTracker } from './idleTimer';
import { tabSession } from './tabSession';
import { inactivityText, markTimedOut } from './timedOut';
import { useSessionExpiry } from './useSessionExpiry';

const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'wheel', 'touchstart', 'scroll'] as const;
/** The delivered values, for what the server does not say. */
const DEFAULT_POLICY: Required<SessionPolicy> = {
  timeoutMinutes: 30,
  warningSeconds: 900,
  expiryWarningMinutes: 30,
  idleWarningMinutes: 15,
  bdoiDialog: true,
  timeoutPage: true,
};

interface InactivityDialogProps {
  open: boolean;
  bdoiDialog: boolean;
  idleMinutes: number;
  secondsLeft: number;
  onStay: () => void;
  onLogOut: () => void;
}

/** The inactivity warning: BDOI's text and buttons, or the earlier wording. */
function InactivityDialog({
  open,
  bdoiDialog,
  idleMinutes,
  secondsLeft,
  onStay,
  onLogOut,
}: Readonly<InactivityDialogProps>) {
  return (
    <Modal
      title="Your session is about to expire"
      open={open}
      onClose={onStay}
      footer={
        <>
          <Button variant="secondary" onClick={onLogOut}>
            {bdoiDialog ? 'Log Out' : 'Sign Out Now'}
          </Button>
          <Button variant="accent" onClick={onStay}>
            {bdoiDialog ? 'Stay Logged In' : 'Stay Signed In'}
          </Button>
        </>
      }
    >
      {bdoiDialog ? (
        <>
          <p>{inactivityText(idleMinutes)}</p>
          <p role="timer" aria-live="polite" className="muted">
            Your session ends in <strong>{secondsLeft}</strong> seconds.
          </p>
        </>
      ) : (
        <p role="timer" aria-live="polite">
          You have been inactive for a while. For your security you will be signed out in{' '}
          <strong>{secondsLeft}</strong> seconds.
        </p>
      )}
    </Modal>
  );
}
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
          I Understand
        </Button>
      }
    >
      <p role="timer" aria-live="polite">
        For your security the system signs you out in <strong>{minutes}</strong>{' '}
        {minutes === 1 ? 'minute' : 'minutes'}, at {formatDateTime(tokenStore.expiresAt())},
        whatever your activity. Save your work; you can sign in again afterwards.
      </p>
    </Modal>
  );
}

/**
 * Session policy (BRNB.040/082): warns after the configured inactivity (SESSION_IDLE_WARNING_MINUTES)
 * and signs out at SESSION_TIMEOUT_MINUTES; warns SESSION_EXPIRY_WARNING_MINUTES before the
 * system-triggered sign-out at the token expiry. Activity in any tab keeps every tab signed in.
 * With SESSION_BDOI_DIALOG the warning carries BDOI's text and the buttons Stay Logged In and Log
 * Out (FRUM.001.03) and is recorded in the audit trail; with SESSION_TIMEOUT_PAGE the inactivity
 * sign-out opens the page "Your session timed out" (FRUM.001.04).
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

  const {
    timeoutMinutes,
    warningSeconds,
    expiryWarningMinutes: expiryWarning,
    idleWarningMinutes: idleMinutes,
    bdoiDialog,
    timeoutPage,
  } = { ...DEFAULT_POLICY, ...policy.data };

  const onExpired = useCallback(() => {
    toast.error('Your session has ended. Please sign in again.');
    logout('EXPIRED');
  }, [logout, toast]);
  const expiry = useSessionExpiry(expiryWarning, onExpired);

  useEffect(() => {
    const sync = tabSession();
    let lastShared = 0;
    let warned = false;
    const instance = createIdleTracker({
      timeoutMs: timeoutMinutes * 60_000,
      warningMs: warningSeconds * 1000,
      onChange: (next, left) => {
        setPhase(next);
        setSecondsLeft(left);
        if (next === 'warning' && !warned) {
          warned = true;
          authApi.reportInactivity().catch(() => undefined);
        } else if (next === 'active') {
          warned = false;
        }
        if (next === 'expired') {
          if (timeoutPage) {
            markTimedOut();
          } else {
            toast.error(
              `You were signed out after ${String(timeoutMinutes)} minutes of inactivity.`,
            );
          }
          logout('IDLE_TIMEOUT');
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
  }, [timeoutMinutes, warningSeconds, timeoutPage, logout, toast]);

  const stay = () => {
    tracker.current?.extend();
    tabSession().announceActivity(true);
  };

  return (
    <>
      <InactivityDialog
        open={phase === 'warning'}
        bdoiDialog={bdoiDialog}
        idleMinutes={idleMinutes}
        secondsLeft={secondsLeft}
        onStay={stay}
        onLogOut={() => logout()}
      />
      {expiry.phase === 'warning' && !expiry.dismissed && phase !== 'warning' && (
        <ExpiryWarning minutes={expiry.minutesLeft} onClose={expiry.dismiss} />
      )}
    </>
  );
}
