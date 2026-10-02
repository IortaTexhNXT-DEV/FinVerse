import { FileBarChart2 } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const reportsModule: FeatureModule = {
  id: 'reports',
  section: 'Reports',
  screens: [
    {
      path: '/reports',
      label: 'Report Centre',
      icon: FileBarChart2,
      permission: 'REPORT_VIEW',
      component: lazy(() => import('./ReportsPage')),
    },
    {
      path: '/reports/:code',
      label: 'Report',
      icon: FileBarChart2,
      permission: 'REPORT_VIEW',
      component: lazy(() => import('./ReportRunnerPage')),
      hidden: true,
    },
  ],
};
