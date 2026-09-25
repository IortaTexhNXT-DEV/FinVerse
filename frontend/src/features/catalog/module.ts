import {
  BadgeCheck,
  Building2,
  Calculator,
  Gift,
  Layers,
  ListChecks,
  Network,
  Package,
  Percent,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/** Placeholder of the catalog screens pre-registered for Product Maintenance (wave P1-A). */
const placeholder = lazy(() => import('./CatalogPlaceholder'));

/**
 * Product Maintenance (BDOI prototype name of the catalog section, BRD-3): product catalog and
 * rules, insurer panel, rates and taxes, sales organisation and the premium calculator
 * (docs/architecture/BROKING_ARCHITECTURE.md, catalog). The package request screens of
 * `productmaint` join this section (features/productmaint/module.ts). The hidden screens below are
 * pre-registered for the Product Maintenance build (PRODUCT_MAINTENANCE_DESIGN section 11): the
 * catalog wave replaces their component, shows them in the menu and adds their help entries.
 */
export const catalogModule: FeatureModule = {
  id: 'catalog',
  section: 'Product Maintenance',
  screens: [
    {
      path: '/catalog/products',
      label: 'Products',
      icon: Package,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./ProductsPage')),
    },
    {
      path: '/catalog/products/:code',
      label: 'Product',
      icon: Package,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./ProductDetailPage')),
      hidden: true,
    },
    {
      path: '/catalog/products/:code/versions/:versionNo',
      label: 'Package Version',
      icon: Layers,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['PRODUCT_MAINTAIN', 'PRODUCT_VALIDATE'],
      component: placeholder,
      hidden: true,
    },
    {
      path: '/catalog/validation',
      label: 'Validation Queue',
      icon: BadgeCheck,
      permission: 'PRODUCT_VALIDATE',
      component: placeholder,
      hidden: true,
    },
    {
      path: '/catalog/coverages',
      label: 'Coverages & Clauses',
      icon: ListChecks,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW'],
      component: placeholder,
      hidden: true,
    },
    {
      path: '/catalog/incentives',
      label: 'Incentive Criteria',
      icon: Gift,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['INCENTIVE_CRITERIA_MAINTAIN'],
      component: placeholder,
      hidden: true,
    },
    {
      path: '/catalog/insurers',
      label: 'Insurers',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./InsurersPage')),
    },
    {
      path: '/catalog/insurers/:id',
      label: 'Insurer',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./InsurerDetailPage')),
      hidden: true,
    },
    {
      path: '/catalog/rates',
      label: 'Rates & Taxes',
      icon: Percent,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./RatesPage')),
    },
    {
      path: '/catalog/sales-organisation',
      label: 'Sales Organisation',
      icon: Network,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./SalesOrganisationPage')),
    },
    {
      path: '/catalog/calculator',
      label: 'Premium Calculator',
      icon: Calculator,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./PremiumCalculatorPage')),
    },
  ],
};
