import { ReceiptText } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
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
    alsoPermissions: ['CLX_SOA_ISSUE'],
    component: lazyWithRetry(() => import('./BillingStatementsPage')),
  },
  {
    path: '/collections/billing/:id',
    label: 'Statement of Account',
    icon: ReceiptText,
    permission: 'CLX_VIEW',
    component: lazyWithRetry(() => import('./StatementDetailPage')),
    hidden: true,
  },
];
