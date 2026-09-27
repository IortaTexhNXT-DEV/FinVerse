import { BookOpenCheck, Calculator, FileText, Layers, Settings2 } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const FEE_ROLES = ['SERVICE_FEE_MANAGE', 'SERVICE_FEE_APPROVE', 'SERVICE_FEE_TAG'];

/**
 * Accounting reports - the BDOI report pack, the account schedules and the service fee (BRD-5,
 * FRBS 2.10, 3.2; docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 11 and 17).
 */
export const frbsModule: FeatureModule = {
  id: 'frbs',
  section: 'Accounting Reports',
  screens: [
    {
      path: '/frbs',
      label: 'Report Pack',
      icon: BookOpenCheck,
      permission: 'FRBS_REPORT_VIEW',
      component: lazy(() => import('./FrbsHomePage')),
    },
    {
      path: '/frbs/schedules',
      label: 'Account Schedules',
      icon: Layers,
      permission: 'FRBS_REPORT_VIEW',
      alsoPermissions: ['REPORT_FINANCIAL', 'MASTER_MAINTAIN'],
      component: lazy(() => import('./SchedulesPage')),
    },
    {
      path: '/frbs/service-fee',
      label: 'Service Fee Runs',
      icon: Calculator,
      permission: 'SERVICE_FEE_MANAGE',
      alsoPermissions: [...FEE_ROLES, 'FRBS_REPORT_VIEW'],
      component: lazy(() => import('./ServiceFeeRunsPage')),
    },
    {
      path: '/frbs/service-fee/setup',
      label: 'Service Fee Rates',
      icon: Settings2,
      permission: 'SERVICE_FEE_APPROVE',
      alsoPermissions: [...FEE_ROLES, 'FRBS_REPORT_VIEW'],
      component: lazy(() => import('./ServiceFeeSetupPage')),
    },
    {
      path: '/frbs/service-fee/runs/:id',
      label: 'Service Fee Run',
      icon: FileText,
      permission: 'SERVICE_FEE_MANAGE',
      alsoPermissions: [...FEE_ROLES, 'FRBS_REPORT_VIEW'],
      component: lazy(() => import('./ServiceFeeRunPage')),
      hidden: true,
    },
  ],
};
