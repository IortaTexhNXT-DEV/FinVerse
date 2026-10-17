import { LayoutDashboard } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

export const dashboardModule: FeatureModule = {
  id: 'dashboard',
  section: 'Overview',
  screens: [
    {
      path: '/',
      label: 'Dashboard',
      icon: LayoutDashboard,
      permission: 'DASHBOARD_VIEW',
      component: lazyWithRetry(() => import('./DashboardPage')),
    },
  ],
};
