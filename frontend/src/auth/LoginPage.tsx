import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { authApi } from '@/api/auth';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { BRAND } from '@/branding';
import { useAuth } from './authContext';
import { SignInFrame } from './SignInFrame';

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
        Back to Login
      </Button>
    </form>
  );
}

/**
 * Sign-in screen following the BDO Insure portal design: photo panel and the sign-in form, with
 * "Forgot password?" (UAM-NFR-37; FR-UA-005). In directory mode the password belongs to BDO and
 * no link is sent.
 */
export default function LoginPage() {
  const { user, login } = useAuth();
  const [mode, setMode] = useState<Mode>('signin');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  if (user !== null) {
    return <Navigate to="/" replace />;
  }

  const submit = () => {
    setBusy(true);
    setError(null);
    login(username, password)
      .catch(setError)
      .finally(() => {
        setBusy(false);
      });
  };

  if (mode === 'forgot') {
    return (
      <SignInFrame title="Forgot Password" subtitle={BRAND.productName}>
        <ForgotPassword onBack={() => setMode('signin')} onSent={() => setMode('sent')} />
      </SignInFrame>
    );
  }

  return (
    <SignInFrame title={`Welcome to ${BRAND.product}`} subtitle={BRAND.productName}>
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        {mode === 'sent' && (
          <div className="alert success" role="status">
            If the user ID has an e-mail address, a reset link is on its way. Check your mailbox.
          </div>
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
            <input
              id={id}
              className="input"
              type="password"
              autoComplete="current-password"
              placeholder="Enter password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          )}
        </Field>
        <div className="login-links">
          <Button variant="ghost" className="login-link" onClick={() => setMode('forgot')}>
            Forgot password?
          </Button>
        </div>
        <Button type="submit" variant="accent" busy={busy}>
          Login
        </Button>
      </form>
    </SignInFrame>
  );
}
