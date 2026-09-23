import { Building2, Coins, Landmark, Tags } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const setupModule: FeatureModule = {
  id: 'setup',
  section: 'Setup',
  screens: [
    {
      path: '/setup/companies',
      label: 'Companies',
      icon: Landmark,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./CompaniesPage')),
    },
    {
      path: '/setup/branches',
      label: 'Branches',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./BranchesPage')),
    },
    {
      path: '/setup/currencies',
      label: 'Currencies & Rates',
      icon: Coins,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./CurrencyRatesPage')),
    },
    {
      path: '/setup/dimensions',
      label: 'Dimensions',
      icon: Tags,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./DimensionsPage')),
    },
  ],
};
