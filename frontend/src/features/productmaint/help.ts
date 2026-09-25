import type { HelpScreen, HelpSection } from '@/features/help/helpContent';

/**
 * In-app help of the package request screens (BRD-3, `productmaint`). Empty until the screens are
 * shown in the menu: the productmaint wave adds one entry per menu screen here (Product
 * Maintenance Home, Package Requests, TSU Workbench, Package Expiry), in sidebar order.
 */
export const PACKAGE_REQUEST_HELP_SCREENS: readonly HelpScreen[] = [];

/**
 * The help section of the Product Maintenance sidebar section: the package request screens first,
 * then the catalog screens (mirrors `withPackageRequests` of module.ts).
 *
 * @param catalog help section of the catalog screens
 * @returns the combined section
 */
export function withPackageRequestHelp(catalog: HelpSection): HelpSection {
  return { ...catalog, screens: [...PACKAGE_REQUEST_HELP_SCREENS, ...catalog.screens] };
}
