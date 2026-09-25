import { CircleDollarSign } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Collections (BRD-4, BRCLXN.001-060; docs/architecture/COLLECTIONS_DESIGN.md section 11): the
 * follow-up of premium receivables. Registered by the foundation (C0) with its home screen; the
 * build waves add the worklist, collection account, client view, unapplied payments, escalations,
 * assignments, billing statements, files and set-up screens here (routes under /collections).
 */
export const collectionsModule: FeatureModule = {
  id: 'collections',
  section: 'Collections',
  screens: [
    {
      path: '/collections',
      label: 'Collections Home',
      icon: CircleDollarSign,
      permission: 'CLX_VIEW',
      alsoPermissions: ['CLX_UNAPPLIED_WORK', 'CLX_SETUP', 'CLX_REPORT_VIEW', 'CLX_AUDIT_VIEW'],
      component: lazy(() => import('./CollectionsHomePage')),
    },
  ],
};
