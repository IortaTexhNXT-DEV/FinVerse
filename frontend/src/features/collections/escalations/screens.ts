import { Siren, SlidersHorizontal } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/**
 * Screens of the Collections escalations (wave C1-B, BRCLXN.049/050): registered in
 * `features/collections/module.ts` by the Collections owner, in this order.
 */
export const ESCALATION_SCREENS: ScreenDef[] = [
  {
    path: '/collections/escalations',
    label: 'Escalations',
    icon: Siren,
    permission: 'CLX_VIEW',
    component: lazy(() => import('./EscalationsPage')),
  },
  {
    path: '/collections/escalations/:id',
    label: 'Escalation',
    icon: Siren,
    permission: 'CLX_VIEW',
    component: lazy(() => import('./EscalationDetailPage')),
    hidden: true,
  },
  {
    path: '/collections/escalation-rules',
    label: 'Escalation Rules',
    icon: SlidersHorizontal,
    permission: 'CLX_SETUP',
    alsoPermissions: ['MASTER_AUTHORIZE', 'CLX_ESCALATION_HANDLE'],
    component: lazy(() => import('./EscalationRulesPage')),
  },
];
