import vendorLogo from '@/assets/platform/iorta-technxt.png';
import pack from '@theme-pack';

/**
 * Product and client branding from the theme pack of the deployment (src/theme/themePack.ts): the
 * client sees its product under its own logo; the platform is iNXT BrokerVerse by iorta TechNXT.
 */
export const BRAND = {
  product: pack.product,
  productName: pack.productName,
  client: pack.client,
  clientShortName: pack.clientShortName,
  groupName: pack.groupName,
  platform: 'iNXT BrokerVerse',
  vendor: 'iorta TechNXT',
  clientLogo: pack.clientLogo,
  vendorLogo,
  loginPhoto: pack.loginPhoto,
} as const;
