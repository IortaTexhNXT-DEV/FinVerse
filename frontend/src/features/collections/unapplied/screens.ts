import { Send, Wallet } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { ScreenDef } from '@/navigation/types';

/**
 * Screens of the collector side of unapplied payments (wave C1-C, BRCLXN.030-048): registered in
 * `features/collections/module.ts` by the Collections owner, in this order.
 */
export const UNAPPLIED_SCREENS: ScreenDef[] = [
  {
    path: '/collections/unapplied',
    label: 'Unapplied Payments',
    icon: Wallet,
    permission: 'CLX_VIEW',
    alsoPermissions: ['CLX_UNAPPLIED_WORK'],
    component: lazyWithRetry(() => import('./UnappliedPaymentsPage')),
  },
  {
    path: '/collections/unapplied/requests',
    label: 'Requests to Cashiering',
    icon: Send,
    permission: 'CLX_VIEW',
    alsoPermissions: ['CLX_UNAPPLIED_WORK'],
    component: lazyWithRetry(() => import('./UnappliedRequestsPage')),
  },
  {
    path: '/collections/unapplied/:ref',
    label: 'Unapplied Payment',
    icon: Wallet,
    permission: 'CLX_VIEW',
    alsoPermissions: ['CLX_UNAPPLIED_WORK'],
    component: lazyWithRetry(() => import('./UnappliedDetailPage')),
    hidden: true,
  },
];
