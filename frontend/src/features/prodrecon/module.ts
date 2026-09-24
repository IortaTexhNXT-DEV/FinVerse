import { Scale } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Product Reconciliation (PRCID.001-039; docs/architecture/OPERATIONS_DESIGN.md). Registered by the Operations
 * foundation with its landing screen; the prodrecon module adds its own screens here.
 */
export const prodreconModule: FeatureModule = {
  id: 'prodrecon',
  section: 'Product Reconciliation',
  screens: [
    {
      path: '/prodrecon',
      label: 'Reconciliation Workbench',
      icon: Scale,
      permission: 'RECON_PROCESS',
      alsoPermissions: ['RECON_SEND'],
      component: lazy(() => import('./ProdReconHomePage')),
    },
  ],
};
