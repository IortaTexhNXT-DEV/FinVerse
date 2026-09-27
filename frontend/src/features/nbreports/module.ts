import { FileBarChart2, Gauge, Target } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/** Route of the New Business dashboard, the landing page of the broking roles (BRNB.012). */
export const NB_DASHBOARD_PATH = '/nb/dashboard';

/** New Business dashboard, shown with Dashboard and My Work at the top of the sidebar. */
export const nbDashboardModule: FeatureModule = {
  id: 'nb-dashboard',
  section: 'New Business Dashboard',
  screens: [
    {
      path: NB_DASHBOARD_PATH,
      label: 'NB Dashboard',
      icon: Gauge,
      permission: 'WORK_VIEW',
      component: lazy(() => import('./NbDashboardPage')),
    },
  ],
};

/** New Business reports and production targets, in the Reports group before the Report Centre. */
export const nbReportsModule: FeatureModule = {
  id: 'nb-reports',
  section: 'New Business Reports',
  screens: [
    {
      path: '/nb/reports',
      label: 'New Business Reports',
      icon: FileBarChart2,
      permission: 'REPORT_VIEW',
      component: lazy(() => import('./NbReportsPage')),
    },
    {
      path: '/nb/targets',
      label: 'Production Targets',
      icon: Target,
      permission: 'WORK_ASSIGN',
      alsoPermissions: ['MASTER_VIEW'],
      component: lazy(() => import('./SalesTargetsPage')),
    },
  ],
};
