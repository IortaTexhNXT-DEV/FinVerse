import { Link } from 'react-router-dom';
import { BRAND } from '@/branding';

/** Sidebar brand: the BDO Insure logo and the product name (BIBS). */
export function Brand() {
  return (
    <Link to="/" className="brand" aria-label={`${BRAND.product} home`}>
      <img className="brand-client-logo" src={BRAND.clientLogo} alt={BRAND.client} />
      <span className="brand-product">
        {BRAND.product} · {BRAND.productName}
      </span>
    </Link>
  );
}
