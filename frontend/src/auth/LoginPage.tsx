import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useAuth } from './authContext';
import { CLIENT_BRAND } from '@/branding';

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
          <div className="login-kicker">IortaTechNXT</div>
          <h1>
            iNXT <span>BrokerVerse</span>
          </h1>
          <p className="login-lead">
            Insurance broking and finance for {CLIENT_BRAND.name}: clients, quotations and
            proposals, placement, issuance, booking and accounting in one secure platform.
          </p>
          <ul>
            <li>One reference (ARN) from quotation to booking</li>
            <li>Maker-checker controls, work queues and complete audit trail</li>
            <li>Broker accounting on the general ledger and sub-ledgers</li>
          </ul>
        </div>
        <small className="login-footer">© IortaTechNXT. All rights reserved.</small>
      </section>
      <section className="login-panel">
        <form
          className="login-card"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <div className="card-body stack">
            <h1 className="login-title">Welcome to BrokerVerse</h1>
            <p className="muted login-subtitle">Sign in with your BrokerVerse credentials.</p>
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
