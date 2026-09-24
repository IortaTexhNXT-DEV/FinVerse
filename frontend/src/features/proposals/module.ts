import { ClipboardList, FilePlus2, FileSignature, Wrench } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Non-Package Management: proposal requests worked by Marketing and the Technical Services Unit
 * (docs/architecture/BROKING_ARCHITECTURE.md section 12).
 */
export const proposalsModule: FeatureModule = {
  id: 'proposals',
  section: 'Non-Package Management',
  screens: [
    {
      path: '/proposals',
      label: 'Proposal Requests',
      icon: ClipboardList,
      permission: 'PROPOSAL_REQUEST',
      alsoPermissions: ['PROPOSAL_APPROVE', 'TSU_PROCESS', 'TSU_APPROVE'],
      component: lazy(() => import('./ProposalsPage')),
    },
    {
      path: '/proposals/new',
      label: 'New Proposal Request',
      icon: FilePlus2,
      permission: 'PROPOSAL_REQUEST',
      component: lazy(() => import('./ProposalFormPage')),
    },
    {
      path: '/proposals/tsu',
      label: 'TSU Workbench',
      icon: Wrench,
      permission: 'TSU_PROCESS',
      alsoPermissions: ['TSU_APPROVE'],
      component: lazy(() => import('./TsuWorkbenchPage')),
    },
    {
      path: '/proposals/:id',
      label: 'Proposal Request',
      icon: FileSignature,
      permission: 'PROPOSAL_REQUEST',
      alsoPermissions: ['PROPOSAL_APPROVE', 'TSU_PROCESS', 'TSU_APPROVE'],
      component: lazy(() => import('./ProposalDetailPage')),
      hidden: true,
    },
    {
      path: '/proposals/:id/edit',
      label: 'Edit Proposal Request',
      icon: FilePlus2,
      permission: 'PROPOSAL_REQUEST',
      alsoPermissions: ['TSU_PROCESS'],
      component: lazy(() => import('./ProposalFormPage')),
      hidden: true,
    },
  ],
};
