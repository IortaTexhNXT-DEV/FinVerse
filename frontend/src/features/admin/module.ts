import { History, ShieldCheck, Users } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const adminModule: FeatureModule = {
  id: 'admin',
  section: 'Administration',
  screens: [
    {
      path: '/admin/users',
      label: 'Users',
      icon: Users,
      permission: 'USER_MANAGE',
      component: lazy(() => import('./UsersPage')),
    },
    {
      path: '/admin/roles',
      label: 'Roles & Permissions',
      icon: ShieldCheck,
      permission: 'ROLE_MANAGE',
      component: lazy(() => import('./RolesPage')),
    },
    {
      path: '/admin/audit',
      label: 'Audit Trail',
      icon: History,
      permission: 'AUDIT_VIEW',
      component: lazy(() => import('./AuditTrailPage')),
    },
  ],
};
