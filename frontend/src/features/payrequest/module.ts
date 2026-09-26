import { Banknote, BookKey, FilePen, FilePlus2, FileText, FileX2 } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const REQUEST_ROLES = ['PRQ_CREATE', 'PRQ_REVIEW', 'PRQ_APPROVE', 'PRQ_HR_APPROVE'];

/**
 * Refund and cash-advance requests (BRD-5, MKT 1.2-2.26;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 11 and 17): the request work list,
 * the Refund Request Form, the Request for Payment of a cash advance, the cancellation of a
 * disbursed check, the request page with its validations, payment and liquidation, and the
 * accounts of the liquidation.
 */
export const payRequestModule: FeatureModule = {
  id: 'payrequest',
  section: 'Refund & Cash Advance Requests',
  screens: [
    {
      path: '/payment-requests',
      label: 'Requests Home',
      icon: FilePen,
      permission: 'PRQ_VIEW',
      alsoPermissions: REQUEST_ROLES,
      component: lazy(() => import('./PayRequestsHomePage')),
    },
    {
      path: '/payment-requests/new-refund',
      label: 'New Refund Request',
      icon: FilePlus2,
      permission: 'PRQ_CREATE',
      component: lazy(() => import('./RefundFormPage')),
    },
    {
      path: '/payment-requests/new-cash-advance',
      label: 'New Cash Advance',
      icon: Banknote,
      permission: 'PRQ_CREATE',
      component: lazy(() => import('./CashAdvanceFormPage')),
    },
    {
      path: '/payment-requests/check-cancellation',
      label: 'Cancel a Check',
      icon: FileX2,
      permission: 'PRQ_CREATE',
      component: lazy(() => import('./CheckCancellationPage')),
    },
    {
      path: '/payment-requests/liquidation-accounts',
      label: 'Liquidation Accounts',
      icon: BookKey,
      permission: 'ACCOUNTING_RULE_MANAGE',
      alsoPermissions: ['PRQ_REVIEW'],
      component: lazy(() => import('./LiquidationAccountsPage')),
    },
    {
      path: '/payment-requests/requests/:id',
      label: 'Request',
      icon: FileText,
      permission: 'PRQ_VIEW',
      alsoPermissions: REQUEST_ROLES,
      component: lazy(() => import('./RequestDetailPage')),
      hidden: true,
    },
    {
      path: '/payment-requests/requests/:id/edit-refund',
      label: 'Change Refund Request',
      icon: FilePen,
      permission: 'PRQ_CREATE',
      component: lazy(() => import('./RefundFormPage')),
      hidden: true,
    },
    {
      path: '/payment-requests/requests/:id/edit-cash-advance',
      label: 'Change Cash Advance',
      icon: FilePen,
      permission: 'PRQ_CREATE',
      component: lazy(() => import('./CashAdvanceFormPage')),
      hidden: true,
    },
  ],
};
