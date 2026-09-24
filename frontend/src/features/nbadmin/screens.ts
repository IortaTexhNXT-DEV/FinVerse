import { Archive, Grid3x3, ListTree, UserCog } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/**
 * Broking Administration screens (nbadmin), shown in the Broking Setup section: lists of values,
 * user access requests, the user access matrix and data retention.
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
  {
    path: '/broking-setup/access-requests',
    label: 'Access Requests',
    icon: UserCog,
    permission: 'ACCESS_REQUEST',
    alsoPermissions: ['ACCESS_APPROVE'],
    component: lazy(() => import('./AccessRequestsPage')),
  },
  {
    path: '/broking-setup/access-matrix',
    label: 'User Access Matrix',
    icon: Grid3x3,
    permission: 'ACCESS_REQUEST',
    alsoPermissions: ['ACCESS_APPROVE', 'ROLE_MANAGE', 'AUDIT_VIEW'],
    component: lazy(() => import('./AccessMatrixPage')),
  },
  {
    path: '/broking-setup/retention',
    label: 'Data Retention',
    icon: Archive,
    permission: 'MASTER_VIEW',
    component: lazy(() => import('./RetentionPage')),
  },
];
