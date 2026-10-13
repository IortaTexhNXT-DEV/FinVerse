import { useEffect, useState } from 'react';
import { BRAND } from '@/branding';

/** Delay before the splash shows, so a fast load shows nothing at all instead of a flash. */
const SHOW_AFTER_MS = 250;

/**
 * Start-up screen while the application or the session loads (opening the URL, signing in again):
 * the client logo and a quiet progress indicator on a plain page, never grey placeholder lines of
 * a page the user has not reached yet. Announced as "Loading".
 */
export function AppSplash() {
  const [shown, setShown] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setShown(true), SHOW_AFTER_MS);
    return () => clearTimeout(timer);
  }, []);
  return (
    <div className="app-splash" role="status" aria-label="Loading">
      {shown && (
        <>
          <img src={BRAND.clientLogo} alt={BRAND.client} />
          <div className="app-splash-bar" />
          <span className="app-splash-name">
            {BRAND.product} – {BRAND.productName}
          </span>
        </>
      )}
    </div>
  );
}
