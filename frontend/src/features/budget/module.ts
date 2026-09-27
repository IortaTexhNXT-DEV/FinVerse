import { ChartColumn, Wallet } from 'lucide-react';
import { lazy } from 'react';
import type { ScreenDef } from '@/navigation/types';

/** Budget screens (listed in the "Planning & Closing" section, see closing/module.ts). */
export const budgetScreens: ScreenDef[] = [
  {
    path: '/planning/budgets',
    label: 'Budgets',
    icon: Wallet,
    permission: 'BUDGET_MANAGE',
    component: lazy(() => import('./BudgetsPage')),
  },
  {
    path: '/planning/budgets/:id',
    label: 'Budget',
    icon: Wallet,
    permission: 'BUDGET_MANAGE',
    component: lazy(() => import('./BudgetEditorPage')),
    hidden: true,
  },
  {
    path: '/planning/budget-vs-actual',
    label: 'Budget vs Actual',
    icon: ChartColumn,
    permission: 'REPORT_FINANCIAL',
    component: lazy(() => import('./BudgetVsActualPage')),
  },
];
