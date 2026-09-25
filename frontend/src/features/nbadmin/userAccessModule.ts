import { FileSpreadsheet, Grid3x3, ShieldCheck, UserCheck } from 'lucide-react';
import { lazy } from 'react';
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
      component: lazy(() => import('./AccessRequestsPage')),
    },
    {
      path: '/user-access/requests/new',
      label: 'New Request',
      icon: UserCheck,
      permission: 'UAM_ENROLL',
      alsoPermissions: REQUEST,
      component: lazy(() => import('./AccessRequestFormPage')),
      hidden: true,
    },
    {
      path: '/user-access/requests/:id',
      label: 'Access Request',
      icon: UserCheck,
      permission: 'UAM_VIEW',
      alsoPermissions: VIEW,
      component: lazy(() => import('./AccessRequestPage')),
      hidden: true,
    },
    {
      path: '/user-access/requests/:id/edit',
      label: 'Edit Request',
      icon: UserCheck,
      permission: 'UAM_ENROLL',
      alsoPermissions: REQUEST,
      component: lazy(() => import('./AccessRequestFormPage')),
      hidden: true,
    },
    {
      path: '/user-access/group-profiles',
      label: 'Group Profile Requests',
      icon: ShieldCheck,
      permission: 'UAM_GROUP_REQUEST',
      alsoPermissions: VIEW,
      component: lazy(() => import('./GroupProfileRequestsPage')),
    },
    {
      path: '/user-access/bulk',
      label: 'Bulk Request',
      icon: FileSpreadsheet,
      permission: 'UAM_ENROLL',
      alsoPermissions: ['UAM_MODIFY', 'ACCESS_APPROVE'],
      component: lazy(() => import('./BulkRequestPage')),
    },
    {
      path: '/user-access/bulk/:id',
      label: 'Bulk Request Batch',
      icon: FileSpreadsheet,
      permission: 'UAM_ENROLL',
      alsoPermissions: ['UAM_MODIFY', 'ACCESS_APPROVE', 'UAM_VIEW'],
      component: lazy(() => import('./AccessBatchPage')),
      hidden: true,
    },
    {
      path: '/user-access/matrix',
      label: 'User Access Matrix',
      icon: Grid3x3,
      permission: 'ACCESS_REQUEST',
      alsoPermissions: ['ACCESS_APPROVE', 'ROLE_MANAGE', 'AUDIT_VIEW', 'UAM_VIEW'],
      component: lazy(() => import('./AccessMatrixPage')),
    },
  ],
};
