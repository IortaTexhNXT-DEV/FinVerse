import { Activity, FlaskConical, ListChecks, Workflow, Zap } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const RuleEditorPage = lazy(() => import('./RuleEditorPage'));

export const accountingEngineModule: FeatureModule = {
  id: 'accounting-engine',
  section: 'Accounting Engine',
  screens: [
    {
      path: '/accounting/event-types',
      label: 'Event Types',
      icon: Zap,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./EventTypesPage')),
    },
    {
      path: '/accounting/rules',
      label: 'Accounting Rules',
      icon: Workflow,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./RulesPage')),
    },
    {
      path: '/accounting/rules/new',
      label: 'New Rule',
      icon: ListChecks,
      permission: 'ACCOUNTING_RULE_MANAGE',
      component: RuleEditorPage,
      hidden: true,
    },
    {
      path: '/accounting/rules/:id',
      label: 'Rule',
      icon: ListChecks,
      permission: 'MASTER_VIEW',
      component: RuleEditorPage,
      hidden: true,
    },
    {
      path: '/accounting/simulator',
      label: 'Rule Simulator',
      icon: FlaskConical,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./SimulatorPage')),
    },
    {
      path: '/accounting/events',
      label: 'Event Register',
      icon: Activity,
      permission: 'JOURNAL_VIEW',
      component: lazy(() => import('./EventRegisterPage')),
    },
  ],
};
