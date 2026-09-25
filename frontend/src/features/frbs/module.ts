import { BookOpenCheck } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Accounting reports - the BDOI report pack and service fee (BRD-5, FRBS 2.10, 3.2;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 11). Registered by the foundation
 * (A0); wave A1-FRBS adds its screens (routes under /frbs).
 */
export const frbsModule: FeatureModule = {
  id: 'frbs',
  section: 'Accounting Reports',
  screens: [
    {
      path: '/frbs',
      label: 'Report Pack',
      icon: BookOpenCheck,
      permission: 'FRBS_REPORT_VIEW',
      alsoPermissions: ['SERVICE_FEE_MANAGE', 'SERVICE_FEE_APPROVE', 'SERVICE_FEE_TAG'],
      component: lazy(() => import('./FrbsHomePage')),
    },
  ],
};
