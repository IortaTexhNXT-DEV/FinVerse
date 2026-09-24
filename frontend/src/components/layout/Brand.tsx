import { Link } from 'react-router-dom';
import { CLIENT_BRAND } from '@/branding';

/**
 * Sidebar brand: the client logo when deployed (see {@link CLIENT_BRAND}), otherwise the iNXT
 * BrokerVerse wordmark (IortaTechNXT product branding).
 */
export function Brand() {
  if (CLIENT_BRAND.logoUrl !== '') {
    return (
      <Link to="/" className="brand" aria-label={`${CLIENT_BRAND.name} home`}>
        <img className="brand-client-logo" src={CLIENT_BRAND.logoUrl} alt={CLIENT_BRAND.name} />
      </Link>
    );
  }
  return (
    <Link to="/" className="brand" aria-label="iNXT BrokerVerse home">
      <span className="brand-mark" aria-hidden="true">
        B
      </span>
      <span>
        <span className="brand-name">
          iNXT <span>BrokerVerse</span>
        </span>
        <span className="brand-tagline">for {CLIENT_BRAND.name}</span>
      </span>
    </Link>
  );
}
