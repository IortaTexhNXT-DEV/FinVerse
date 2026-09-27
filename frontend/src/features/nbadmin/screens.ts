import { Archive, Grid3x3, ListTree, UserCog } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/**
 * Broking Administration screens (nbadmin), shown in the Broking Setup section: lists of values
 * and data retention. The access requests and the user access matrix moved to User Access.
 */
export const NBADMIN_SCREENS: ScreenDef[] = [
  {
    path: '/broking-setup/lists',
    label: 'Lists of Values',
    icon: ListTree,
    permission: 'LOV_MANAGE',
    alsoPermissions: ['MASTER_AUTHORIZE'],
    component: lazy(() => import('./LovPage')),
  },
  // Moved to the User Access section (BRD-11, wave U1-A); the old routes redirect.
  {
    path: '/broking-setup/access-requests',
    label: 'Access Requests',
    icon: UserCog,
    component: lazy(() =>
      import('./legacyRedirects').then((m) => ({ default: m.AccessRequestsRedirect })),
    ),
    hidden: true,
  },
  {
    path: '/broking-setup/access-matrix',
    label: 'User Access Matrix',
    icon: Grid3x3,
    component: lazy(() =>
      import('./legacyRedirects').then((m) => ({ default: m.AccessMatrixRedirect })),
    ),
    hidden: true,
  },
  {
    path: '/broking-setup/retention',
    label: 'Data Retention',
    icon: Archive,
    permission: 'MASTER_VIEW',
    component: lazy(() => import('./RetentionPage')),
  },
];
