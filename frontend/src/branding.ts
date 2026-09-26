import clientLogo from '@/assets/brand/bdo-insure.png';
import loginPhoto from '@/assets/brand/login-photo.jpg';
import vendorLogo from '@/assets/brand/iorta-technxt.png';

/**
 * Product and client branding (docs/design/BDO_UX_GUIDELINES.md). The client sees BIBS, the BDOI
 * Broker System, under the BDO Insure logo; the platform is iNXT BrokerVerse by iorta TechNXT.
 *
 * The BDO Insure logo and the sign-in photo come from the BDOI UX design pack. Replace them with the
 * master files from BDO Marketing Communications (never redrawn or altered) when they are supplied.
 */
export const BRAND = {
  product: 'BIBS',
  productName: 'BDOI Broker System',
  client: 'BDO Insure',
  platform: 'iNXT BrokerVerse',
  vendor: 'iorta TechNXT',
  clientLogo,
  vendorLogo,
  loginPhoto,
} as const;
