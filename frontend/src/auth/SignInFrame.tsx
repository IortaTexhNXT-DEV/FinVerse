import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { authApi } from '@/api/auth';
import { BRAND } from '@/branding';
import { environmentLabel } from './environmentLabel';
import './auth.css';

/** The brand panel: product name, what the system covers and the company. */
function BrandPanel() {
  return (
    <aside className="signin-brand" aria-label={BRAND.productName}>
      <div className="signin-brand-body">
        <p className="signin-brand-product">{BRAND.product}</p>
        <p className="signin-brand-name">{BRAND.productName}</p>
        <span className="signin-brand-rule" aria-hidden="true" />
        <p className="signin-brand-tagline">{BRAND.signInTagline}</p>
        <ul className="signin-brand-points">
          {BRAND.signInHighlights.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      </div>
      <p className="signin-brand-company">{BRAND.legalName}</p>
    </aside>
  );
}

/**
 * The sign-in layout of BIBS: a brand panel in BDO Header Blue and the sign-in card with the BDO
 * Insure logo, the environment label outside production, the authorised-use notice and the
 * company footer. Shared by the sign-in, "Forgot password?", second factor, reset link, single
 * sign-on return and forced change pages.
 */
export function SignInFrame({
  title,
  subtitle,
  children,
}: Readonly<{ title: string; subtitle: string; children: ReactNode }>) {
  const options = useQuery({ queryKey: ['sign-in-options'], queryFn: authApi.signInOptions });
  const environment = environmentLabel(options.data?.environment);
  const company = BRAND.legalName.endsWith('.') ? BRAND.legalName : `${BRAND.legalName}.`;
  return (
    <div className="signin-page">
      <BrandPanel />
      <main className="signin-main">
        <div className="signin-topbar">
          {environment !== null && (
            <span className="signin-env" title="This is not the live system">
              {environment} environment
            </span>
          )}
        </div>
        <section className="signin-card login-card stack" aria-labelledby="signin-title">
          <img className="signin-logo" src={BRAND.clientLogo} alt={BRAND.client} />
          <div>
            <h1 id="signin-title" className="signin-title">
              {title}
            </h1>
            <p className="signin-subtitle">{subtitle}</p>
          </div>
          {children}
        </section>
        <p className="signin-legal">
          Authorised users only. {BRAND.product} is the property of {company} Sign-ins and activity
          are recorded and monitored; unauthorised use may lead to disciplinary and legal action.
        </p>
        <footer className="signin-footer">
          <span>
            © {new Date().getFullYear()} {BRAND.legalName}
          </span>
          <span className="powered-by">
            <span>Powered by</span>
            <img src={BRAND.vendorLogo} alt={BRAND.vendor} />
          </span>
        </footer>
      </main>
    </div>
  );
}
