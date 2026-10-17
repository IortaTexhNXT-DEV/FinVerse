import {
  BadgeCheck,
  BadgePercent,
  Building2,
  Calculator,
  Gift,
  Layers,
  ListChecks,
  Network,
  Package,
  Percent,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Product Maintenance (BDOI UX design name of the catalog section, BRD-3): product catalog and
 * rules, insurer panel, rates and taxes, sales organisation and the premium calculator
 * (docs/architecture/BROKING_ARCHITECTURE.md, catalog), with the Product Maintenance catalog
 * screens (BRD-3, PRODUCT_MAINTENANCE_DESIGN section 11): package versions and their validation,
 * coverages and clauses, and incentive criteria. The package request screens of `productmaint`
 * join this section (features/productmaint/module.ts).
 */
export const catalogModule: FeatureModule = {
  id: 'catalog',
  section: 'Product Maintenance',
  screens: [
    {
      path: '/catalog/products',
      label: 'Products',
      icon: Package,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW'],
      component: lazyWithRetry(() => import('./ProductsPage')),
    },
    {
      path: '/catalog/products/:code',
      label: 'Product',
      icon: Package,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW'],
      component: lazyWithRetry(() => import('./ProductDetailPage')),
      hidden: true,
    },
    {
      path: '/catalog/products/:code/versions/:versionNo',
      label: 'Package Version',
      icon: Layers,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW', 'PRODUCT_MAINTAIN', 'PRODUCT_VALIDATE'],
      component: lazyWithRetry(() => import('./VersionEditorPage')),
      hidden: true,
    },
    {
      path: '/catalog/rate-exceptions/:reference',
      label: 'Rate Exception',
      icon: BadgePercent,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW', 'QUOTE_VIEW', 'ACCOUNT_VIEW', 'PRODUCT_AUTHORIZE'],
      component: lazyWithRetry(() => import('./RateExceptionPage')),
      hidden: true,
    },
    {
      path: '/catalog/validation',
      label: 'Validation Queue',
      icon: BadgeCheck,
      permission: 'PRODUCT_VALIDATE',
      component: lazyWithRetry(() => import('./ValidationQueuePage')),
    },
    {
      path: '/catalog/coverages',
      label: 'Coverages & Clauses',
      icon: ListChecks,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['MASTER_VIEW'],
      component: lazyWithRetry(() => import('./CoveragesPage')),
    },
    {
      path: '/catalog/incentives',
      label: 'Incentive Criteria',
      icon: Gift,
      permission: 'PRODUCT_VIEW',
      alsoPermissions: ['INCENTIVE_CRITERIA_MAINTAIN'],
      component: lazyWithRetry(() => import('./IncentivesPage')),
    },
    {
      path: '/catalog/insurers',
      label: 'Insurers',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./InsurersPage')),
    },
    {
      path: '/catalog/insurers/:id',
      label: 'Insurer',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./InsurerDetailPage')),
      hidden: true,
    },
    {
      path: '/catalog/rates',
      label: 'Rates & Taxes',
      icon: Percent,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./RatesPage')),
    },
    {
      path: '/catalog/sales-organisation',
      label: 'Sales Organisation',
      icon: Network,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./SalesOrganisationPage')),
    },
    {
      path: '/catalog/calculator',
      label: 'Premium Calculator',
      icon: Calculator,
      permission: 'ACCOUNT_VIEW',
      component: lazyWithRetry(() => import('./PremiumCalculatorPage')),
    },
  ],
};
