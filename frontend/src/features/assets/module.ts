import {
  ArrowLeftRight,
  Boxes,
  Briefcase,
  CalendarClock,
  FolderTree,
  Landmark,
  TrendingDown,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/** Fixed assets and the investment portfolio. */
export const assetsModule: FeatureModule = {
  id: 'assets',
  section: 'Assets & Investments',
  screens: [
    {
      path: '/assets/categories',
      label: 'Asset Categories',
      icon: FolderTree,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./AssetCategoriesPage')),
    },
    {
      path: '/assets/register',
      label: 'Asset Register',
      icon: Boxes,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./AssetRegisterPage')),
    },
    {
      path: '/assets/depreciation',
      label: 'Depreciation Run',
      icon: TrendingDown,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./DepreciationRunPage')),
    },
    {
      path: '/investments/portfolios',
      label: 'Investment Portfolios',
      icon: Briefcase,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./PortfoliosPage')),
    },
    {
      path: '/investments/holdings',
      label: 'Investments',
      icon: Landmark,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./InvestmentsPage')),
    },
    {
      path: '/investments/runs',
      label: 'Accrual & Amortization',
      icon: CalendarClock,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./InvestmentRunsPage')),
    },
    {
      path: '/investments/maturities',
      label: 'Maturities & Sales',
      icon: ArrowLeftRight,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./HoldingEventsPage')),
    },
  ],
};
