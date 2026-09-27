import type { ReactNode } from 'react';
import { BRAND } from '@/branding';
import './auth.css';

/**
 * The sign-in layout of the BDO Insure portal: photo panel and a card with the logo, a title and
 * a subtitle. Shared by the sign-in, "Forgot password?", reset link and forced change pages.
 */
export function SignInFrame({
  title,
  subtitle,
  children,
}: Readonly<{ title: string; subtitle: string; children: ReactNode }>) {
  return (
    <div className="login-page">
      <section className="login-hero" aria-hidden="true">
        <img src={BRAND.loginPhoto} alt="" />
      </section>
      <section className="login-panel">
        <div className="login-card stack">
          <img className="login-logo" src={BRAND.clientLogo} alt={BRAND.client} />
          <div>
            <h1 className="login-title">{title}</h1>
            <p className="login-subtitle">{subtitle}</p>
          </div>
          {children}
          <div className="powered-by">
            <span>Powered by</span>
            <img src={BRAND.vendorLogo} alt={BRAND.vendor} />
          </div>
        </div>
      </section>
    </div>
  );
}
