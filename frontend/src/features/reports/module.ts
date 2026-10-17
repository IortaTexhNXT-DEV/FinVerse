import { CalendarClock, FileBarChart2 } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
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
      component: lazyWithRetry(() => import('./ReportsPage')),
    },
    {
      path: '/reports/subscriptions',
      label: 'My Report Subscriptions',
      icon: CalendarClock,
      permission: 'REPORT_VIEW',
      component: lazyWithRetry(() => import('./ReportSubscriptionsPage')),
    },
    {
      path: '/reports/:code',
      label: 'Report',
      icon: FileBarChart2,
      permission: 'REPORT_VIEW',
      component: lazyWithRetry(() => import('./ReportRunnerPage')),
      hidden: true,
    },
  ],
};
