import { BellRing, Inbox } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule, ScreenDef } from '@/navigation/types';

const myApprovals: ScreenDef = {
  path: '/approvals',
  label: 'My Approvals',
  icon: Inbox,
  component: lazyWithRetry(() => import('./MyApprovalsPage')),
};

const alerts: ScreenDef = {
  path: '/alerts',
  label: 'Alerts',
  icon: BellRing,
  permission: 'ALERT_VIEW',
  component: lazyWithRetry(() => import('@/features/alerts/AlertsPage')),
};

/** Overview section with My Approvals as its first item and the alerts inbox after it. */
export function withOverviewScreens(overview: FeatureModule): FeatureModule {
  return { ...overview, screens: [myApprovals, ...overview.screens, alerts] };
}
