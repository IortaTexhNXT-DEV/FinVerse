/**
 * Client branding. BrokerVerse shows the client's own logo when the official file is deployed
 * (BDO Style Guide: the logo is never re-drawn or altered, so it must come from BDO Marketing
 * Communications). Set `VITE_CLIENT_LOGO_URL` at build time, e.g. `/brand/bdo-insure.svg`, to a
 * file placed in `public/brand/`; until then the iNXT BrokerVerse wordmark is shown.
 */
export const CLIENT_BRAND = {
  name: 'BDO Insure',
  logoUrl: (import.meta.env.VITE_CLIENT_LOGO_URL as string | undefined) ?? '',
};
