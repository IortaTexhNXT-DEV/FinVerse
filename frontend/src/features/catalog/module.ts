import { Building2, Calculator, Network, Package, Percent } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Products & Insurers: product catalog and rules, insurer panel, rates and taxes, sales
 * organisation and the premium calculator (docs/architecture/BROKING_ARCHITECTURE.md, catalog).
 */
export const catalogModule: FeatureModule = {
  id: 'catalog',
  section: 'Products & Insurers',
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
