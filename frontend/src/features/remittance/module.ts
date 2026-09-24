import { Send } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Remittance (RMTID.001-040, MKTID.001-009; docs/architecture/OPERATIONS_DESIGN.md). Registered by the Operations
 * foundation with its landing screen; the remittance module adds its own screens here.
 */
export const remittanceModule: FeatureModule = {
  id: 'remittance',
  section: 'Remittance',
  screens: [
    {
      path: '/remittance',
      label: 'Remittance Workbench',
      icon: Send,
      permission: 'REMIT_PROCESS',
      alsoPermissions: [
        'REMIT_EXTRACT',
        'REMIT_APPROVE',
        'REMIT_EXCLUDE',
        'REMIT_OR_UPLOAD',
        'HOLD_REQUEST',
        'HOLD_APPROVE',
        'SPECIAL_REMIT_REQUEST',
        'SPECIAL_REMIT_APPROVE',
      ],
      component: lazy(() => import('./RemittanceHomePage')),
    },
  ],
};
