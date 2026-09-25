import { ShieldAlert } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Sanction Screening (BRD-10, SNSRP-101-903; docs/architecture/SANCTION_SCREENING_DESIGN.md
 * section 11.2): screening cases, potential matches, high-risk clients and STRs. Registered by the
 * foundation (S0) in the Client & Policy group after Client Management; waves S1-B / S1-C add the
 * matches, case, high-risk and STR screens (routes under /screening).
 */
export const screeningModule: FeatureModule = {
  id: 'screening',
  section: 'Sanction Screening',
  screens: [
    {
      path: '/screening',
      label: 'Screening Home',
      icon: ShieldAlert,
      permission: 'SCR_VIEW',
      component: lazy(() => import('./ScreeningHomePage')),
    },
  ],
};
