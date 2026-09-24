import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { BRAND } from '@/branding';
import { useAuth } from './authContext';

/** Sign-in screen following the BDO Insure portal design: photo panel and the sign-in form. */
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
      <section className="login-hero" aria-hidden="true">
        <img src={BRAND.loginPhoto} alt="" />
      </section>
      <section className="login-panel">
        <form
          className="login-card stack"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <img className="login-logo" src={BRAND.clientLogo} alt={BRAND.client} />
          <div>
            <h1 className="login-title">Welcome to {BRAND.product}</h1>
            <p className="login-subtitle">{BRAND.productName}</p>
          </div>
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
          <Button type="submit" variant="accent" busy={busy}>
            Login
          </Button>
          <div className="powered-by">
            <span>Powered by</span>
            <img src={BRAND.vendorLogo} alt={BRAND.vendor} />
          </div>
        </form>
      </section>
    </div>
  );
}
