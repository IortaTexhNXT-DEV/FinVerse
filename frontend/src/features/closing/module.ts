import { CalendarCheck, CalendarClock, Coins } from 'lucide-react';
import { lazy } from 'react';
import { budgetScreens } from '@/features/budget/module';
import type { FeatureModule } from '@/navigation/types';

/**
 * "Planning & Closing" menu section: budgets, FX revaluation and period-end / year-end closing.
 */
export const planningModule: FeatureModule = {
  id: 'planning',
  section: 'Planning & Closing',
  screens: [
    ...budgetScreens,
    {
      path: '/planning/fx-revaluation',
      label: 'FX Revaluation',
      icon: Coins,
      permission: 'PERIOD_END_RUN',
      component: lazy(() => import('./FxRevaluationPage')),
    },
    {
      path: '/planning/closing',
      label: 'Period-End & Year-End',
      icon: CalendarCheck,
      permission: 'PERIOD_END_RUN',
      component: lazy(() => import('./PeriodEndPage')),
    },
    {
      path: '/planning/gl-close',
      label: 'GL Close & Cut-Off',
      icon: CalendarClock,
      permission: 'GL_CLOSE_SCHEDULE',
      component: lazy(() => import('./GlClosePage')),
    },
  ],
};
