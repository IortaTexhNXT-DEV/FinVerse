import { UserCheck } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * User Access (BRD-11 User Access Maintenance; docs/architecture/USER_ACCESS_DESIGN.md section
 * 11.2): access requests with drafts, returns, cancellations, effective dates and second approval,
 * bulk requests, group-profile requests, the user access matrix and the user access reports, in
 * the Setup & Administration group before Administration. Registered by the foundation (U0); wave
 * U1-A moves the access request and matrix screens here (routes under /user-access).
 */
export const userAccessModule: FeatureModule = {
  id: 'user-access',
  section: 'User Access',
  screens: [
    {
      path: '/user-access/requests',
      label: 'Access Request Queues',
      icon: UserCheck,
      permission: 'UAM_VIEW',
      alsoPermissions: ['ACCESS_REQUEST', 'ACCESS_APPROVE', 'UAM_SECOND_APPROVE'],
      component: lazy(() => import('./UserAccessHomePage')),
    },
  ],
};
