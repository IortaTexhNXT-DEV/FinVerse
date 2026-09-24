import { HandCoins } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Commission Receivables (CMRID.001-015, MKTID.012; docs/architecture/OPERATIONS_DESIGN.md). Registered by the Operations
 * foundation with its landing screen; the commission module adds its own screens here.
 */
export const commissionModule: FeatureModule = {
  id: 'commission',
  section: 'Commission Receivables',
  screens: [
    {
      path: '/commission',
      label: 'Commission Workbench',
      icon: HandCoins,
      permission: 'COMMREC_PROCESS',
      alsoPermissions: ['COMMREC_APPROVE', 'INCENTIVE_MANAGE', 'BIR_CERT_SUBMIT', 'BIR_CERT_ACK'],
      component: lazy(() => import('./CommissionHomePage')),
    },
  ],
};
