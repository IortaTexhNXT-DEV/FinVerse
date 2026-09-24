import { Link } from 'react-router-dom';

/** iNXT BrokerVerse wordmark (IortaTechNXT product branding). */
export function Brand() {
  return (
    <Link to="/" className="brand" aria-label="iNXT BrokerVerse home">
      <span className="brand-mark" aria-hidden="true">
        F
      </span>
      <span>
        <span className="brand-name">
          iNXT <span>BrokerVerse</span>
        </span>
        <span className="brand-tagline">Insurance Finance Suite</span>
      </span>
    </Link>
  );
}
