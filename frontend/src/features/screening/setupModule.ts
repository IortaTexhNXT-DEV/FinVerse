import { FileText, ListChecks, SlidersHorizontal, UploadCloud } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Compliance Setup (BRD-10, SNSRP-101-109, 201-204; SANCTION_SCREENING_DESIGN section 11.2):
 * versioned screening configuration, templates, watchlists and list sources, in the Setup &
 * Administration group. Registered by the foundation (S0); the screens come with wave S1-A
 * (features/screening/setup).
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
      component: lazy(() => import('./setup/ConfigVersionsPage')),
    },
    {
      path: '/screening-setup/templates',
      label: 'Templates',
      icon: FileText,
      permission: 'SCR_CONFIG_MAINTAIN',
      alsoPermissions: ['SCR_CONFIG_APPROVE'],
      component: lazy(() => import('./setup/TemplatesPage')),
    },
    {
      path: '/screening-setup/watchlist',
      label: 'Watchlist',
      icon: ListChecks,
      permission: 'SCR_VIEW',
      alsoPermissions: ['SCR_LIST_MAINTAIN', 'SCR_LIST_APPROVE'],
      component: lazy(() => import('./setup/WatchlistPage')),
    },
    {
      path: '/screening-setup/sources',
      label: 'List Sources and Runs',
      icon: UploadCloud,
      permission: 'SCR_LIST_MAINTAIN',
      // The list maker and checker only (design 11.2); SCR_VIEW alone showed it to every persona.
      alsoPermissions: ['SCR_LIST_APPROVE'],
      component: lazy(() => import('./setup/SourcesPage')),
    },
  ],
};
