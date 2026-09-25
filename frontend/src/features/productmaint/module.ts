import { FilePlus2, FileStack, Hourglass, LayoutDashboard, Workflow } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule, ScreenDef } from '@/navigation/types';

/**
 * Package request screens of Product Maintenance (BRD-3, `productmaint`; BRPM.008-019,
 * PMADD03/04; PRODUCT_MAINTENANCE_DESIGN section 11). The menu screens are Home, Package
 * Requests, TSU Workbench and Package Expiry (help entries in `PACKAGE_REQUEST_HELP_SCREENS`, same
 * order); the form and the record page open from the list.
 */
export const PACKAGE_REQUEST_SCREENS: readonly ScreenDef[] = [
  {
    path: '/product-maintenance',
    label: 'Product Maintenance Home',
    icon: LayoutDashboard,
    permission: 'PRODUCT_VIEW',
    alsoPermissions: ['PKG_REPORT_VIEW'],
    component: lazy(() => import('./ProductMaintenanceHomePage')),
  },
  {
    path: '/product-maintenance/requests',
    label: 'Package Requests',
    icon: FileStack,
    permission: 'PRODUCT_VIEW',
    alsoPermissions: ['PKG_REPORT_VIEW'],
    component: lazy(() => import('./PackageRequestsPage')),
  },
  {
    path: '/product-maintenance/requests/new',
    label: 'New Package Request',
    icon: FilePlus2,
    permission: 'PKG_REQUEST',
    component: lazy(() => import('./PackageRequestFormPage')),
    hidden: true,
  },
  {
    path: '/product-maintenance/requests/:id',
    label: 'Package Request',
    icon: FileStack,
    permission: 'PRODUCT_VIEW',
    alsoPermissions: ['PKG_REPORT_VIEW'],
    component: lazy(() => import('./PackageRequestPage')),
    hidden: true,
  },
  {
    path: '/product-maintenance/requests/:id/edit',
    label: 'Edit Package Request',
    icon: FilePlus2,
    permission: 'PKG_REQUEST',
    alsoPermissions: ['PKG_TSU_RECOMMEND'],
    component: lazy(() => import('./PackageRequestFormPage')),
    hidden: true,
  },
  {
    path: '/product-maintenance/tsu',
    label: 'TSU Workbench',
    icon: Workflow,
    permission: 'PKG_NEGOTIATE',
    alsoPermissions: ['PKG_TSU_RECOMMEND', 'PKG_TSU_APPROVE'],
    component: lazy(() => import('./PackageTsuWorkbenchPage')),
  },
  {
    path: '/product-maintenance/expiry',
    label: 'Package Expiry',
    icon: Hourglass,
    permission: 'PKG_NEGOTIATE',
    alsoPermissions: ['PRODUCT_MAINTAIN', 'PKG_REPORT_VIEW'],
    component: lazy(() => import('./PackageExpiryPage')),
  },
];

/**
 * The Product Maintenance sidebar section: the package request screens first, then the catalog
 * screens (one section, BDOI prototype; the same pattern as `withOverviewScreens`).
 *
 * @param catalog the catalog module (section "Product Maintenance")
 * @returns the catalog module with the package request screens
 */
export function withPackageRequests(catalog: FeatureModule): FeatureModule {
  return { ...catalog, screens: [...PACKAGE_REQUEST_SCREENS, ...catalog.screens] };
}
