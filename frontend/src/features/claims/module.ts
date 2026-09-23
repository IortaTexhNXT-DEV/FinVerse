import { FilePlus2, FileWarning, Wrench } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const claimsModule: FeatureModule = {
  id: 'claims',
  section: 'Claims',
  screens: [
    {
      path: '/claims',
      label: 'Claims',
      icon: FileWarning,
      permission: 'CLAIM_VIEW',
      component: lazy(() => import('./ClaimsPage')),
    },
    {
      path: '/claims/new',
      label: 'Notify Claim',
      icon: FilePlus2,
      permission: 'CLAIM_MAINTAIN',
      component: lazy(() => import('./ClaimFormPage')),
    },
    {
      path: '/claims/lpos',
      label: 'LPO Register',
      icon: Wrench,
      permission: 'CLAIM_VIEW',
      component: lazy(() => import('./LposPage')),
    },
    {
      path: '/claims/:id',
      label: 'Claim',
      icon: FileWarning,
      permission: 'CLAIM_VIEW',
      component: lazy(() => import('./ClaimDetailPage')),
      hidden: true,
    },
  ],
};
