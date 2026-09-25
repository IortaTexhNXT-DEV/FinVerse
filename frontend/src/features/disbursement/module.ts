import { Landmark } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Disbursement (BRD-5, DIS 2.2-3.28; docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section
 * 11): payment requests, disbursement vouchers, instruments, end of day, payees and account
 * funding. Registered by the foundation (A0); wave A1-DSB adds its screens (routes under
 * /disbursement).
 */
export const disbursementModule: FeatureModule = {
  id: 'disbursement',
  section: 'Disbursement',
  screens: [
    {
      path: '/disbursement',
      label: 'Disbursement Workbench',
      icon: Landmark,
      permission: 'DISB_VIEW',
      alsoPermissions: [
        'DISB_PROCESS',
        'DISB_REVIEW',
        'DISB_APPROVE',
        'DISB_PAYEE_MAINTAIN',
        'DISB_PAYEE_AUTHORIZE',
        'DISB_FUNDING_REQUEST',
        'DISB_FUNDING_APPROVE',
        'DISB_EOD',
      ],
      component: lazy(() => import('./DisbursementHomePage')),
    },
  ],
};
