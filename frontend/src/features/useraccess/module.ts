import {
  FileBarChart,
  FolderSync,
  Grid3x3,
  History,
  Scale,
  ShieldCheck,
  SlidersHorizontal,
  Users,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule, ScreenDef } from '@/navigation/types';

/** The holders of a read right on User Access. */
const VIEWERS = [
  'UAM_VIEW',
  'ACCESS_APPROVE',
  'UAM_SECOND_APPROVE',
  'UAM_GROUP_REQUEST',
  'AUDIT_VIEW',
];

type RedirectName =
  | 'UsersRedirect'
  | 'GroupProfilesRedirect'
  | 'IdentitySyncRedirect'
  | 'RequestRedirect'
  | 'MatrixRedirect'
  | 'SodRedirect'
  | 'ReportsRedirect';

const redirect = (path: string, name: RedirectName): ScreenDef => ({
  path,
  label: 'User Access',
  icon: ShieldCheck,
  component: lazyWithRetry(() => import('./redirects').then((m) => ({ default: m[name] }))),
  hidden: true,
});

/**
 * User Access (BRD-11; docs/architecture/USER_ACCESS_DESIGN.md) under Setup & Administration. UIDM-ISC
 * maintains the users and their group-profile membership and Microsoft Entra ID signs them in; this
 * section keeps the group-profile definitions (maker-checker), data access, unlock and session end,
 * identity synchronisation and reconciliation, separation of duties, reports, the certification
 * extract, the audit log and the UAM settings. The screens of the earlier layout redirect here.
 */
export const userAccessModule: FeatureModule = {
  id: 'user-access',
  section: 'User Access',
  screens: [
    {
      path: '/access/users',
      label: 'Users',
      icon: Users,
      permission: 'USER_MANAGE',
      alsoPermissions: ['USER_VIEW'],
      component: lazyWithRetry(() => import('./UsersPage')),
    },
    {
      path: '/access/users/:id',
      label: 'User',
      icon: Users,
      permission: 'USER_MANAGE',
      alsoPermissions: ['USER_VIEW'],
      component: lazyWithRetry(() => import('./UserDetailPage')),
      hidden: true,
    },
    {
      path: '/access/group-profiles',
      label: 'Group Profiles',
      icon: ShieldCheck,
      permission: 'ROLE_MANAGE',
      alsoPermissions: VIEWERS,
      component: lazyWithRetry(() => import('./GroupProfilesPage')),
    },
    {
      path: '/access/group-profiles/:code',
      label: 'Group Profile',
      icon: ShieldCheck,
      permission: 'ROLE_MANAGE',
      alsoPermissions: VIEWERS,
      component: lazyWithRetry(() => import('./GroupProfileDetailPage')),
      hidden: true,
    },
    {
      path: '/access/identity-sync',
      label: 'Identity Sync',
      icon: FolderSync,
      permission: 'USER_MANAGE',
      component: lazyWithRetry(() => import('./IdentitySyncPage')),
    },
    {
      path: '/access/separation-of-duties',
      label: 'Separation of Duties',
      icon: Scale,
      permission: 'UAM_SOD_MAINTAIN',
      alsoPermissions: ['UAM_SOD_AUTHORIZE', 'AUDIT_VIEW', 'USER_MANAGE'],
      component: lazyWithRetry(() => import('@/features/nbadmin/SodRulesPage')),
    },
    {
      path: '/access/matrix',
      label: 'Access Matrix',
      icon: Grid3x3,
      permission: 'UAM_REPORT_VIEW',
      component: lazyWithRetry(() => import('@/features/nbadmin/AccessMatrixPage')),
    },
    {
      path: '/access/reports',
      label: 'Reports & Certification',
      icon: FileBarChart,
      permission: 'UAM_REPORT_VIEW',
      component: lazyWithRetry(() => import('./ReportsPage')),
    },
    {
      path: '/access/audit',
      label: 'Audit Log',
      icon: History,
      permission: 'AUDIT_VIEW',
      component: lazyWithRetry(() => import('./AuditLogPage')),
    },
    {
      path: '/access/settings',
      label: 'UAM Settings',
      icon: SlidersHorizontal,
      permission: 'USER_MANAGE',
      alsoPermissions: ['SYSTEM_PARAMETER_MANAGE', 'SECURITY_PARAMETER_APPROVE'],
      component: lazyWithRetry(() => import('./UamSettingsPage')),
    },
    // The screens of the earlier layout open their successor.
    redirect('/admin/users', 'UsersRedirect'),
    redirect('/admin/roles', 'GroupProfilesRedirect'),
    redirect('/admin/identity-sync', 'IdentitySyncRedirect'),
    redirect('/user-access/requests', 'GroupProfilesRedirect'),
    redirect('/user-access/requests/new', 'GroupProfilesRedirect'),
    redirect('/user-access/requests/:id', 'RequestRedirect'),
    redirect('/user-access/requests/:id/edit', 'RequestRedirect'),
    redirect('/user-access/group-profiles', 'GroupProfilesRedirect'),
    redirect('/user-access/matrix', 'MatrixRedirect'),
    redirect('/user-access/sod-rules', 'SodRedirect'),
    redirect('/user-access/reports', 'ReportsRedirect'),
  ],
};
