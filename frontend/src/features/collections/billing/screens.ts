import { ReceiptText } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/**
 * Screens of the Collections billing statements (wave C1-B, BRCLXN.058/060): registered in
 * `features/collections/module.ts` by the Collections owner, in this order.
 */
export const BILLING_SCREENS: ScreenDef[] = [
  {
    path: '/collections/billing',
    label: 'Billing Statements',
    icon: ReceiptText,
    permission: 'CLX_VIEW',
    alsoPermissions: ['CLX_BILLING'],
    component: lazy(() => import('./BillingStatementsPage')),
  },
  {
    path: '/collections/billing/:id',
    label: 'Statement of Account',
    icon: ReceiptText,
    permission: 'CLX_VIEW',
    component: lazy(() => import('./StatementDetailPage')),
    hidden: true,
  },
];
