import { LayoutDashboard } from 'lucide-react';
import { lazy } from 'react';
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
      component: lazy(() => import('./DashboardPage')),
    },
  ],
};
