import { FilePen } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Adjustment (ADJID.001-028, MKTID.008; docs/architecture/OPERATIONS_DESIGN.md). Registered by the Operations
 * foundation with its landing screen; the adjustment module adds its own screens here.
 */
export const adjustmentModule: FeatureModule = {
  id: 'adjustment',
  section: 'Adjustment',
  screens: [
    {
      path: '/adjustment',
      label: 'Adjustment Workbench',
      icon: FilePen,
      permission: 'ADJ_PROCESS',
      alsoPermissions: ['ADJ_REQUEST', 'ADJ_APPROVE', 'ADJ_POST'],
      component: lazy(() => import('./AdjustmentHomePage')),
    },
  ],
};
