import { FileWarning, FolderSearch, History, ShieldAlert, ShieldX, UserSearch } from 'lucide-react';
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
    {
      path: '/screening/cases',
      label: 'Cases',
      icon: FolderSearch,
      permission: 'SCR_VIEW',
      component: lazy(() => import('./cases/CasesPage')),
    },
    {
      path: '/screening/cases/:id',
      label: 'Screening Case',
      icon: FolderSearch,
      permission: 'SCR_VIEW',
      hidden: true,
      component: lazy(() => import('./cases/CasePage')),
    },
    {
      path: '/screening/matches',
      label: 'Matches',
      icon: UserSearch,
      permission: 'SCR_VIEW',
      component: lazy(() => import('./matches/MatchesPage')),
    },
    {
      path: '/screening/runs',
      label: 'Screening Runs',
      icon: History,
      permission: 'SCR_VIEW',
      component: lazy(() => import('./matches/RunsPage')),
    },
    {
      path: '/screening/high-risk',
      label: 'High-risk Clients',
      icon: ShieldX,
      permission: 'SCR_VIEW',
      component: lazy(() => import('./cases/HighRiskPage')),
    },
    {
      path: '/screening/str',
      label: 'STR',
      icon: FileWarning,
      permission: 'SCR_COMPLIANCE_REVIEW',
      alsoPermissions: ['SCR_STR_EXTRACT', 'SCR_AUDIT_VIEW'],
      component: lazy(() => import('./str/StrPage')),
    },
  ],
};
