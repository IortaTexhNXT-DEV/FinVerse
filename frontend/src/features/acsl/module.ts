import { Scale } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * ACSL - Accounting Control and Sub-Ledger (BRD-5, ACSL 2.2-2.16;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 11): cases, correction entries,
 * insurer SOA reconciliation and GL-SL reconciliation. Registered by the foundation (A0); wave
 * A1-PRQ adds its screens (routes under /acsl).
 */
export const acslModule: FeatureModule = {
  id: 'acsl',
  section: 'ACSL',
  screens: [
    {
      path: '/acsl',
      label: 'ACSL Cases',
      icon: Scale,
      permission: 'ACSL_VIEW',
      alsoPermissions: ['ACSL_PROCESS', 'ACSL_ASSIGN', 'ACSL_REVIEW', 'ACSL_APPROVE'],
      component: lazy(() => import('./AcslHomePage')),
    },
  ],
};
