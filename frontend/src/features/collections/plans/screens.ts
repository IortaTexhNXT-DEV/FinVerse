import { CalendarClock, HandCoins, ListChecks } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { ScreenDef } from '@/navigation/types';

/**
 * Screens of the Collections plans (wave C1-B, BRCLXN.053/054/055): registered in
 * `features/collections/module.ts` by the Collections owner, in this order.
 */
export const PLAN_SCREENS: ScreenDef[] = [
  {
    path: '/collections/plans',
    label: 'Installment Plans',
    icon: ListChecks,
    permission: 'CLX_VIEW',
    component: lazyWithRetry(() => import('./PlansPage')),
  },
  {
    path: '/collections/plans/:id',
    label: 'Installment Plan',
    icon: ListChecks,
    permission: 'CLX_VIEW',
    component: lazyWithRetry(() => import('./PlanDetailPage')),
    hidden: true,
  },
  {
    path: '/collections/installments-due',
    label: 'Installments Due',
    icon: CalendarClock,
    permission: 'CLX_VIEW',
    component: lazyWithRetry(() => import('./InstallmentsDuePage')),
  },
  {
    path: '/collections/promises',
    label: 'Promises to Pay',
    icon: HandCoins,
    permission: 'CLX_VIEW',
    component: lazyWithRetry(() => import('./PromisesPage')),
  },
];
