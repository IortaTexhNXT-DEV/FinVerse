import { BellRing, Inbox } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule, ScreenDef } from '@/navigation/types';

const myApprovals: ScreenDef = {
  path: '/approvals',
  label: 'My Approvals',
  icon: Inbox,
  component: lazy(() => import('./MyApprovalsPage')),
};

const alerts: ScreenDef = {
  path: '/alerts',
  label: 'Alerts',
  icon: BellRing,
  permission: 'ALERT_VIEW',
  component: lazy(() => import('@/features/alerts/AlertsPage')),
};

/** Overview section with My Approvals as its first item and the alerts inbox after it. */
export function withOverviewScreens(overview: FeatureModule): FeatureModule {
  return { ...overview, screens: [myApprovals, ...overview.screens, alerts] };
}
