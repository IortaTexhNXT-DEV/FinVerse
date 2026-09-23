import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useAuth } from './authContext';

/** Sign-in screen with the product hero panel. */
export default function LoginPage() {
  const { user, login } = useAuth();
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

  return (
    <div className="login-page">
      <section className="login-hero">
        <div>
          <div className="brand-tagline" style={{ color: '#FDB913' }}>
            IortaTechNXT
          </div>
          <h1>
            iNXT <span>FinVerse</span>
          </h1>
          <p style={{ fontSize: 17, maxWidth: 480 }}>
            The insurance finance suite: general ledger, sub-ledgers, reinsurance, reserves and
            statutory reporting in one secure platform.
          </p>
          <ul>
            <li>Real-time, event-driven insurance accounting</li>
            <li>Maker-checker controls and complete audit trail</li>
            <li>Financial, underwriting, claims and reinsurance reports</li>
          </ul>
        </div>
        <small style={{ color: '#9fb2da' }}>© IortaTechNXT. All rights reserved.</small>
      </section>
      <section className="login-panel">
        <form
          className="card login-card"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <div className="card-body stack">
            <h2>Sign in</h2>
            <p className="muted" style={{ margin: 0 }}>
              Use your FinVerse credentials.
            </p>
            <ErrorAlert error={error} />
            <Field label="User name" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              )}
            </Field>
            <Field label="Password" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              )}
            </Field>
            <Button type="submit" variant="accent" busy={busy}>
              Sign in
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}
