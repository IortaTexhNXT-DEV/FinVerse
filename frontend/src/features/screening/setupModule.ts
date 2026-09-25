import { SlidersHorizontal } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Compliance Setup (BRD-10, SNSRP-101-109, 201-204; SANCTION_SCREENING_DESIGN section 11.2):
 * versioned screening configuration, templates, watchlists and list sources, in the Setup &
 * Administration group. Registered by the foundation (S0); wave S1-A adds the templates, watchlist
 * and sources screens (routes under /screening-setup).
 */
export const screeningSetupModule: FeatureModule = {
  id: 'screening-setup',
  section: 'Compliance Setup',
  screens: [
    {
      path: '/screening-setup/config',
      label: 'Configuration Versions',
      icon: SlidersHorizontal,
      permission: 'SCR_CONFIG_MAINTAIN',
      alsoPermissions: ['SCR_CONFIG_APPROVE', 'SCR_LIST_MAINTAIN', 'SCR_LIST_APPROVE'],
      component: lazy(() => import('./ConfigVersionsPage')),
    },
  ],
};
