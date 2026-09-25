import { CalendarClock, FileSpreadsheet, FileUp, Layers, Scale, SearchX } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const PROCESS = 'RECON_PROCESS';
const SEND = ['RECON_SEND'];

/**
 * Product Reconciliation (PRCID.001-039; docs/architecture/OPERATIONS_DESIGN.md): the workbench,
 * reconciliation cycles per insurer and month, production extracts, insurer feedback uploads,
 * the unbooked repository and the extract schedules.
 */
export const prodreconModule: FeatureModule = {
  id: 'prodrecon',
  section: 'Product Reconciliation',
  screens: [
    {
      path: '/prodrecon',
      label: 'Reconciliation Workbench',
      icon: Scale,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ProdReconHomePage')),
    },
    {
      path: '/prodrecon/cycles',
      label: 'Reconciliation Cycles',
      icon: Layers,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ReconCyclesPage')),
    },
    {
      path: '/prodrecon/cycles/:id',
      label: 'Reconciliation Cycle',
      icon: Layers,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ReconCyclePage')),
      hidden: true,
    },
    {
      path: '/prodrecon/extracts',
      label: 'Production Extracts',
      icon: FileSpreadsheet,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ReconExtractsPage')),
    },
    {
      path: '/prodrecon/uploads',
      label: 'Insurer Feedback',
      icon: FileUp,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ReconUploadsPage')),
    },
    {
      path: '/prodrecon/unbooked',
      label: 'Unbooked Accounts',
      icon: SearchX,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./UnbookedPage')),
    },
    {
      path: '/prodrecon/schedules',
      label: 'Extract Schedules',
      icon: CalendarClock,
      permission: PROCESS,
      alsoPermissions: SEND,
      component: lazy(() => import('./ReconSchedulesPage')),
    },
  ],
};
