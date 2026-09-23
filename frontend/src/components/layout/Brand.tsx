import { Link } from 'react-router-dom';

/** iNXT FinVerse wordmark (IortaTechNXT product branding). */
export function Brand() {
  return (
    <Link to="/" className="brand" aria-label="iNXT FinVerse home">
      <span className="brand-mark" aria-hidden="true">
        F
      </span>
      <span>
        <span className="brand-name">
          iNXT <span>FinVerse</span>
        </span>
        <span className="brand-tagline">Insurance Finance Suite</span>
      </span>
    </Link>
  );
}
