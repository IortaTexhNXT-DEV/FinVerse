import { Banknote } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Cashiering (CSHID.001-027, MKTID.010/013, DBMID.001; docs/architecture/OPERATIONS_DESIGN.md). Registered by the Operations
 * foundation with its landing screen; the cashiering module adds its own screens here.
 */
export const cashieringModule: FeatureModule = {
  id: 'cashiering',
  section: 'Cashiering',
  screens: [
    {
      path: '/cashiering',
      label: 'Cashiering Workbench',
      icon: Banknote,
      permission: 'CASH_RECEIPT',
      alsoPermissions: [
        'CASH_APPLY',
        'CASH_APPROVE',
        'CASH_DISPOSITION',
        'CASH_UPLOAD',
        'CWT_PROCESS',
        'CWT_TAG',
      ],
      component: lazy(() => import('./CashieringHomePage')),
    },
  ],
};
