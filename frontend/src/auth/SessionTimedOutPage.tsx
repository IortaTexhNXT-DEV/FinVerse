import { useNavigate } from 'react-router-dom';
import { BRAND } from '@/branding';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { clearTimedOut, TIMED_OUT_TEXT } from '@/session/timedOut';
import { SignInFrame } from './SignInFrame';

/**
 * The page shown after the session ended for inactivity (BDOI FRS FRUM.001.04): "Your session timed
 * out. Please log in again to continue" with the Log In button, which opens the Login page.
 */
export default function SessionTimedOutPage() {
  const navigate = useNavigate();
  const logIn = () => {
    clearTimedOut();
    void navigate('/login', { replace: true });
  };
  return (
    <SignInFrame title="Session Timed Out" subtitle={BRAND.productName}>
      <Notice tone="warning">{TIMED_OUT_TEXT}</Notice>
      <Button variant="accent" onClick={logIn}>
        Log In
      </Button>
    </SignInFrame>
  );
}
