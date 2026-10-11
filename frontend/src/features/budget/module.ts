import { ChartColumn, Wallet } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { ScreenDef } from '@/navigation/types';

/** Budget screens (listed in the "Planning & Closing" section, see closing/module.ts). */
export const budgetScreens: ScreenDef[] = [
  {
    path: '/planning/budgets',
    label: 'Budgets',
    icon: Wallet,
    permission: 'BUDGET_MANAGE',
    component: lazyWithRetry(() => import('./BudgetsPage')),
  },
  {
    path: '/planning/budgets/:id',
    label: 'Budget',
    icon: Wallet,
    permission: 'BUDGET_MANAGE',
    component: lazyWithRetry(() => import('./BudgetEditorPage')),
    hidden: true,
  },
  {
    path: '/planning/budget-vs-actual',
    label: 'Budget vs Actual',
    icon: ChartColumn,
    permission: 'REPORT_FINANCIAL',
    component: lazyWithRetry(() => import('./BudgetVsActualPage')),
  },
];
