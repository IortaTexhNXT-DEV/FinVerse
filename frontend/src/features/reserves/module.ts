import { ChartColumn, ClipboardList, SlidersHorizontal, Triangle } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * "Actuarial Reserves" menu section: reserve summary dashboard, valuation runs (preview, approve,
 * post, cancel) with policy-level UPR, IBNR development triangles and the reserve parameters.
 */
export const reservesModule: FeatureModule = {
  id: 'reserves',
  section: 'Actuarial Reserves',
  screens: [
    {
      path: '/reserves/summary',
      label: 'Reserve Summary',
      icon: ChartColumn,
      permission: 'REPORT_FINANCIAL',
      component: lazy(() => import('./ReserveDashboardPage')),
    },
    {
      path: '/reserves/runs',
      label: 'Valuation Runs',
      icon: ClipboardList,
      permission: 'RESERVE_PREPARE',
      component: lazy(() => import('./ValuationRunsPage')),
    },
    {
      path: '/reserves/runs/:id',
      label: 'Valuation Run',
      icon: ClipboardList,
      permission: 'RESERVE_PREPARE',
      component: lazy(() => import('./ValuationRunPage')),
      hidden: true,
    },
    {
      path: '/reserves/triangles',
      label: 'IBNR Triangles',
      icon: Triangle,
      permission: 'RESERVE_PREPARE',
      component: lazy(() => import('./TriangleViewerPage')),
    },
    {
      path: '/reserves/parameters',
      label: 'Reserve Parameters',
      icon: SlidersHorizontal,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./ReserveParametersPage')),
    },
  ],
};
