import { FileBarChart, Grid3x3, Scale, ShieldCheck, UserCheck } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/** Anyone taking part in access requests. */
const VIEW = [
  'ACCESS_REQUEST',
  'ACCESS_APPROVE',
  'UAM_SECOND_APPROVE',
  'ROLE_MANAGE',
  'AUDIT_VIEW',
];

/** The request functions (BRD 4.002.2.1-5, 9). */
const REQUEST = [
  'UAM_MODIFY',
  'UAM_DEACTIVATE',
  'UAM_REACTIVATE',
  'UAM_GROUP_REQUEST',
  'UAM_CORRECT',
  'ACCESS_REQUEST',
];

/**
 * User Access (BRD-11 User Access Maintenance; docs/architecture/USER_ACCESS_DESIGN.md section
 * 11.2): access requests with drafts, returns, cancellations, effective dates and second approval,
 * group-profile requests implemented by the System Administrator, bulk requests and the user access
 * matrix, in the Setup & Administration group before Administration. The old Broking Setup routes
 * of the access requests and the matrix redirect here.
 */
export const userAccessModule: FeatureModule = {
  id: 'user-access',
  section: 'User Access',
  screens: [
    {
      path: '/user-access/requests',
      label: 'Access Requests',
      icon: UserCheck,
      permission: 'UAM_VIEW',
      alsoPermissions: VIEW,
      component: lazyWithRetry(() => import('./AccessRequestsPage')),
    },
    {
      path: '/user-access/requests/new',
      label: 'New Request',
      icon: UserCheck,
      permission: 'UAM_ENROLL',
      alsoPermissions: REQUEST,
      component: lazyWithRetry(() => import('./AccessRequestFormPage')),
      hidden: true,
    },
    {
      path: '/user-access/requests/:id',
      label: 'Access Request',
      icon: UserCheck,
      permission: 'UAM_VIEW',
      alsoPermissions: VIEW,
      component: lazyWithRetry(() => import('./AccessRequestPage')),
      hidden: true,
    },
    {
      path: '/user-access/requests/:id/edit',
      label: 'Edit Request',
      icon: UserCheck,
      permission: 'UAM_ENROLL',
      alsoPermissions: REQUEST,
      component: lazyWithRetry(() => import('./AccessRequestFormPage')),
      hidden: true,
    },
    {
      path: '/user-access/group-profiles',
      label: 'Group Profile Requests',
      icon: ShieldCheck,
      permission: 'UAM_GROUP_REQUEST',
      alsoPermissions: VIEW,
      component: lazyWithRetry(() => import('./GroupProfileRequestsPage')),
    },
    {
      path: '/user-access/matrix',
      label: 'User Access Matrix',
      icon: Grid3x3,
      permission: 'ACCESS_REQUEST',
      // The permissions of the matrix endpoint: UAM_VIEW alone showed an entry that answers 403.
      alsoPermissions: ['ACCESS_APPROVE', 'ROLE_MANAGE', 'AUDIT_VIEW'],
      component: lazyWithRetry(() => import('./AccessMatrixPage')),
    },
    {
      path: '/user-access/sod-rules',
      label: 'Separation of Duties',
      icon: Scale,
      permission: 'UAM_SOD_MAINTAIN',
      alsoPermissions: ['UAM_SOD_AUTHORIZE'],
      component: lazyWithRetry(() => import('./SodRulesPage')),
    },
    {
      path: '/user-access/reports',
      label: 'User Access Reports',
      icon: FileBarChart,
      permission: 'UAM_REPORT_VIEW',
      component: lazyWithRetry(() => import('./UserAccessReportsPage')),
    },
  ],
};
