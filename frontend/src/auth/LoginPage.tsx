import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { authApi } from '@/api/auth';
import type { SignInOptions } from '@/api/auth';
import type { LoginResponse } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { BRAND } from '@/branding';
import { useAuth } from './authContext';
import { SignInFrame } from './SignInFrame';
import { Notice } from '@/components/ui/Notice';
import { SecondFactorStep } from './SecondFactorStep';

type Mode = 'signin' | 'forgot' | 'sent';

function ForgotPassword({ onBack, onSent }: Readonly<{ onBack: () => void; onSent: () => void }>) {
  const [userId, setUserId] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const submit = () => {
    setBusy(true);
    setError(null);
    authApi
      .requestReset(userId.trim())
      .then(onSent)
      .catch(setError)
      .finally(() => setBusy(false));
  };
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <p className="login-notice">
        Enter your user ID. We e-mail a link to set a new password to the address registered for it.
        The link works once, within 30 minutes.
      </p>
      <ErrorAlert error={error} />
      <Field label="User ID" required>
        {(id) => (
          <input
            id={id}
            className="input"
            autoComplete="username"
            placeholder="Enter user ID"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
          />
        )}
      </Field>
      <Button type="submit" variant="accent" busy={busy} disabled={userId.trim() === ''}>
        Send Reset Link
      </Button>
      <Button variant="secondary" onClick={onBack}>
        Back to Sign in
      </Button>
    </form>
  );
}

/** The button of the single sign-on: opens the identity provider's sign-in page. */
function SingleSignOnButton({ options }: Readonly<{ options: SignInOptions }>) {
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const go = () => {
    setBusy(true);
    setError(null);
    authApi
      .startSingleSignOn()
      .then((start) => globalThis.location.assign(start.redirectUrl))
      .catch((e: unknown) => {
        setError(e);
        setBusy(false);
      });
  };
  return (
    <div className="stack">
      <ErrorAlert error={error} />
      <Button variant="accent" busy={busy} onClick={go}>
        Sign in with {options.providerLabel ?? 'your organisation'}
      </Button>
    </div>
  );
}

interface PasswordFormProps {
  mode: Mode;
  showForgot: boolean;
  onForgot: () => void;
  onSecondFactor: (pending: LoginResponse) => void;
}

/** User ID and password; the answer opens the session or asks for the second factor. */
function PasswordForm({ mode, showForgot, onForgot, onSecondFactor }: Readonly<PasswordFormProps>) {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [capsLock, setCapsLock] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const submit = () => {
    setBusy(true);
    setError(null);
    login(username, password)
      .then((result) => {
        if (result.mfaStep !== undefined) {
          onSecondFactor(result);
        }
      })
      .catch(setError)
      .finally(() => {
        setBusy(false);
      });
  };
  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      {mode === 'sent' && (
        <Notice tone="success">
          If the user ID has an e-mail address, a reset link is on its way. Check your mailbox.
        </Notice>
      )}
      <ErrorAlert error={error} />
      <Field label="User ID">
        {(id) => (
          <input
            id={id}
            className="input"
            autoComplete="username"
            placeholder="Enter user ID"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        )}
      </Field>
      <Field label="Password">
        {(id) => (
          <div className="password-input">
            <input
              id={id}
              className="input"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Enter password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyUp={(e) => setCapsLock(e.getModifierState('CapsLock'))}
              required
            />
            <button
              type="button"
              className="password-toggle"
              aria-pressed={showPassword}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword((shown) => !shown)}
            >
              {showPassword ? 'Hide' : 'Show'}
            </button>
          </div>
        )}
      </Field>
      {capsLock && (
        <p className="caps-lock" role="status">
          Caps Lock is on.
        </p>
      )}
      {showForgot && (
        <div className="login-links">
          <Button variant="ghost" className="login-link" onClick={onForgot}>
            Forgot password?
          </Button>
        </div>
      )}
      <Button type="submit" variant="accent" busy={busy}>
        Sign in
      </Button>
    </form>
  );
}

interface ChoicesProps {
  options: SignInOptions | undefined;
  mode: Mode;
  onForgot: () => void;
  onSecondFactor: (pending: LoginResponse) => void;
}

/**
 * The ways to sign in: the password form, or with single sign-on the provider's button and the
 * password form of the break-glass administrators behind "Administrator sign-in".
 */
function SignInChoices({ options, mode, onForgot, onSecondFactor }: Readonly<ChoicesProps>) {
  const [adminSignIn, setAdminSignIn] = useState(false);
  const form = (
    <PasswordForm
      mode={mode}
      showForgot={options?.passwordReset ?? true}
      onForgot={onForgot}
      onSecondFactor={onSecondFactor}
    />
  );
  if (options?.singleSignOn !== true) {
    return form;
  }
  return (
    <>
      <SingleSignOnButton options={options} />
      {adminSignIn ? (
        form
      ) : (
        <div className="login-links">
          <Button variant="ghost" className="login-link" onClick={() => setAdminSignIn(true)}>
            Administrator sign-in
          </Button>
        </div>
      )}
    </>
  );
}

/**
 * Sign-in screen: brand panel and the sign-in form, with "Forgot password?" (UAM-NFR-37; FR-UA-005)
 * where the passwords are held by the system. With single sign-on the main button opens the
 * identity provider and the password form stays for the break-glass administrators. After the
 * password the second factor is asked for when required (code of the authenticator app, or its
 * enrolment at the first sign-in).
 */
export default function LoginPage() {
  const { user, completeSignIn } = useAuth();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;
  const [mode, setMode] = useState<Mode>('signin');
  const [pending, setPending] = useState<LoginResponse | null>(null);
  const options = useQuery({ queryKey: ['sign-in-options'], queryFn: authApi.signInOptions });

  if (user !== null) {
    return <Navigate to={from?.startsWith('/') === true ? from : '/'} replace />;
  }

  if (pending !== null) {
    return (
      <SignInFrame title="Second Factor" subtitle={BRAND.productName}>
        <SecondFactorStep
          pending={pending}
          onSignedIn={completeSignIn}
          onBack={() => setPending(null)}
        />
      </SignInFrame>
    );
  }

  if (mode === 'forgot') {
    return (
      <SignInFrame title="Forgot Password" subtitle={BRAND.productName}>
        <ForgotPassword onBack={() => setMode('signin')} onSent={() => setMode('sent')} />
      </SignInFrame>
    );
  }

  return (
    <SignInFrame title="Sign in" subtitle={`to ${BRAND.product} – ${BRAND.productName}`}>
      <SignInChoices
        options={options.data}
        mode={mode}
        onForgot={() => setMode('forgot')}
        onSecondFactor={setPending}
      />
    </SignInFrame>
  );
}
