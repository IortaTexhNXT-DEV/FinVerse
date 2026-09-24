import { ListTodo } from 'lucide-react';
import { lazy } from 'react';
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
      component: lazy(() => import('./MyWorkPage')),
    },
  ],
};
