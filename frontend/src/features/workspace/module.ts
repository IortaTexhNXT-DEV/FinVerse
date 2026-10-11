import { Bell, ListTodo } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/** My Work: the broking work queues (docs/architecture/BROKING_ARCHITECTURE.md section 5). */
export const workspaceModule: FeatureModule = {
  id: 'workspace',
  section: 'My Work',
  screens: [
    {
      path: '/my-work',
      label: 'My Work',
      icon: ListTodo,
      permission: 'WORK_VIEW',
      component: lazyWithRetry(() => import('./MyWorkPage')),
    },
    {
      path: '/notifications',
      label: 'Notifications',
      icon: Bell,
      hidden: true,
      component: lazyWithRetry(() => import('./NotificationsPage')),
    },
  ],
};
