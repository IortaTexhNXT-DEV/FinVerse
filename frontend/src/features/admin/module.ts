import {
  Cable,
  History,
  Server,
  ShieldCheck,
  SlidersHorizontal,
  Timer,
  TriangleAlert,
  Users,
} from 'lucide-react';
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
    {
      path: '/admin/parameters',
      label: 'System Parameters',
      icon: SlidersHorizontal,
      permission: 'SYSTEM_MONITOR',
      component: lazy(() => import('@/features/system/SystemParametersPage')),
    },
    {
      path: '/admin/exception-codes',
      label: 'Exception Codes',
      icon: TriangleAlert,
      permission: 'ALERT_VIEW',
      component: lazy(() => import('@/features/alerts/ExceptionCodesPage')),
    },
    {
      path: '/admin/jobs',
      label: 'Scheduled Jobs',
      icon: Timer,
      permission: 'SYSTEM_MONITOR',
      component: lazy(() => import('@/features/system/ScheduledJobsPage')),
    },
    {
      path: '/admin/integration-events',
      label: 'Integration Events',
      icon: Cable,
      permission: 'SYSTEM_PARAMETER_MANAGE',
      component: lazy(() => import('@/features/system/IntegrationEventsPage')),
    },
    {
      path: '/admin/info',
      label: 'Application Info',
      icon: Server,
      permission: 'SYSTEM_MONITOR',
      component: lazy(() => import('@/features/system/ApplicationInfoPage')),
    },
  ],
};
