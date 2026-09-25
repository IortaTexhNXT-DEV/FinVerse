import {
  Calculator,
  FileCheck,
  FileInput,
  FileStack,
  Gift,
  HandCoins,
  ListChecks,
  MailQuestionMark,
  Percent,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const PROCESS = 'COMMREC_PROCESS';
const READERS = ['COMMREC_APPROVE'];
const INCENTIVE_READERS = ['COMMREC_APPROVE', 'INCENTIVE_MANAGE'];
const CERT = 'BIR_CERT_SUBMIT';
const CERT_READERS = ['BIR_CERT_ACK'];

/**
 * Commission Receivables (CMRID.001-015, MKTID.012; docs/architecture/OPERATIONS_DESIGN.md): the
 * workbench, direct payment lists, accounts, billings and insurer responses, incentive schemes
 * and runs, BIR certificates and estimated items.
 */
export const commissionModule: FeatureModule = {
  id: 'commission',
  section: 'Commission Receivables',
  screens: [
    {
      path: '/commission',
      label: 'Commission Workbench',
      icon: HandCoins,
      permission: PROCESS,
      alsoPermissions: ['COMMREC_APPROVE', 'INCENTIVE_MANAGE', 'BIR_CERT_SUBMIT', 'BIR_CERT_ACK'],
      component: lazy(() => import('./CommissionHomePage')),
    },
    {
      path: '/commission/dp/lists',
      label: 'DP Lists',
      icon: FileInput,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./DpListsPage')),
    },
    {
      path: '/commission/dp/items',
      label: 'DP Accounts',
      icon: ListChecks,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./DpItemsPage')),
    },
    {
      path: '/commission/dp/billings',
      label: 'DP Billings',
      icon: FileStack,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./DpBillingsPage')),
    },
    {
      path: '/commission/dp/billings/:id',
      label: 'DP Billing',
      icon: FileStack,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./DpBillingPage')),
      hidden: true,
    },
    {
      path: '/commission/dp/responses',
      label: 'Insurer Responses',
      icon: MailQuestionMark,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./DpResponsesPage')),
    },
    {
      path: '/commission/incentives/schemes',
      label: 'Incentive Schemes',
      icon: Gift,
      permission: PROCESS,
      alsoPermissions: INCENTIVE_READERS,
      component: lazy(() => import('./IncentiveSchemesPage')),
    },
    {
      path: '/commission/incentives/runs',
      label: 'Incentive Runs',
      icon: Calculator,
      permission: PROCESS,
      alsoPermissions: INCENTIVE_READERS,
      component: lazy(() => import('./IncentiveRunsPage')),
    },
    {
      path: '/commission/certificates',
      label: 'BIR Certificates',
      icon: FileCheck,
      permission: CERT,
      alsoPermissions: CERT_READERS,
      component: lazy(() => import('./CertificatesPage')),
    },
    {
      path: '/commission/certificates/:id',
      label: 'BIR Certificate',
      icon: FileCheck,
      permission: CERT,
      alsoPermissions: CERT_READERS,
      component: lazy(() => import('./CertificatePage')),
      hidden: true,
    },
    {
      path: '/commission/estimated',
      label: 'Estimated Items',
      icon: Percent,
      permission: PROCESS,
      alsoPermissions: READERS,
      component: lazy(() => import('./EstimatedItemsPage')),
    },
  ],
};
