import { FileSpreadsheet, Layers, Share2, ShieldCheck, Split } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const reinsuranceModule: FeatureModule = {
  id: 'reinsurance',
  section: 'Reinsurance',
  screens: [
    {
      path: '/reinsurance/treaties',
      label: 'Treaties',
      icon: Layers,
      permission: 'REINSURANCE_VIEW',
      component: lazy(() => import('./TreatiesPage')),
    },
    {
      path: '/reinsurance/allocation',
      label: 'RI Allocation',
      icon: Split,
      permission: 'REINSURANCE_VIEW',
      component: lazy(() => import('./AllocationPage')),
    },
    {
      path: '/reinsurance/fac',
      label: 'FAC Placements',
      icon: Share2,
      permission: 'REINSURANCE_VIEW',
      component: lazy(() => import('./FacPlacementsPage')),
    },
    {
      path: '/reinsurance/claims',
      label: 'Claims Recoveries',
      icon: ShieldCheck,
      permission: 'REINSURANCE_VIEW',
      component: lazy(() => import('./ClaimRecoveriesPage')),
    },
    {
      path: '/reinsurance/soa',
      label: 'Statements of Account',
      icon: FileSpreadsheet,
      permission: 'REINSURANCE_VIEW',
      component: lazy(() => import('./SoaPage')),
    },
  ],
};
