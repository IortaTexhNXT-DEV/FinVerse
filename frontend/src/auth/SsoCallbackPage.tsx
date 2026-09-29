import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router-dom';
import { authApi } from '@/api/auth';
import type { LoginResponse } from '@/api/types';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { BRAND } from '@/branding';
import { useAuth } from './authContext';
import { SecondFactorStep } from './SecondFactorStep';
import { SignInFrame } from './SignInFrame';
import { singleSignOnMessage } from './ssoMessages';

/**
 * Where the identity provider sends the browser back: the one-time ticket is exchanged for the
 * session (or the second factor when the installation asks for it); an error code is shown in
 * words.
 */
export default function SsoCallbackPage() {
  const { user, completeSignIn } = useAuth();
  const [params] = useSearchParams();
  const ticket = params.get('ticket');
  const errorCode = params.get('error');
  const [pending, setPending] = useState<LoginResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const sent = useRef(false);

  useEffect(() => {
    if (ticket === null || sent.current) {
      return;
    }
    sent.current = true;
    authApi
      .completeSingleSignOn(ticket)
      .then((result) => {
        if (result.mfaStep === undefined) {
          completeSignIn(result);
        } else {
          setPending(result);
        }
      })
      .catch(setError);
  }, [ticket, completeSignIn]);

  if (user !== null) {
    return <Navigate to="/" replace />;
  }
  let content;
  if (errorCode !== null || ticket === null) {
    content = <Notice tone="error">{singleSignOnMessage(errorCode ?? '')}</Notice>;
  } else if (pending !== null) {
    content = (
      <SecondFactorStep
        pending={pending}
        onSignedIn={completeSignIn}
        onBack={() => setPending(null)}
      />
    );
  } else if (error === null) {
    content = <span className="spinner" aria-label="Signing in" />;
  } else {
    content = <ErrorAlert error={error} />;
  }
  return (
    <SignInFrame title="Signing In" subtitle={BRAND.productName}>
      {content}
      <Link to="/login" className="login-notice">
        Back to Login
      </Link>
    </SignInFrame>
  );
}
