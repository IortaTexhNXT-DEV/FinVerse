import { ArrowLeftRight, Network } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/** Inter-company and consolidation screens (in the "Planning & Closing" section). */
export const consolidationScreens: ScreenDef[] = [
  {
    path: '/planning/intercompany',
    label: 'Inter-company',
    icon: ArrowLeftRight,
    permission: 'CONSOLIDATION_RUN',
    component: lazy(() => import('./IntercompanyPage')),
  },
  {
    path: '/planning/consolidation',
    label: 'Consolidation',
    icon: Network,
    permission: 'CONSOLIDATION_RUN',
    component: lazy(() => import('./ConsolidationPage')),
  },
];
