import {
  CircleDollarSign,
  FileSpreadsheet,
  ListChecks,
  Settings2,
  UserCog,
  Users,
  WalletCards,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';
import { BILLING_SCREENS } from './billing/screens';
import { ESCALATION_SCREENS } from './escalations/screens';
import { PLAN_SCREENS } from './plans/screens';
import { UNAPPLIED_SCREENS } from './unapplied/screens';

/**
 * Collections (BRD-4, BRCLXN.001-060; docs/architecture/COLLECTIONS_DESIGN.md section 11): the
 * follow-up of premium receivables. The core (C1-A) gives the home, the PR worklist, the collection
 * account, the client view, assignments, files and set-up; the plans / escalation (C1-B) and
 * unapplied-payment (C1-C) screens are registered here after the worklist (routes under
 * /collections).
 */
export const collectionsModule: FeatureModule = {
  id: 'collections',
  section: 'Collections',
  screens: [
    {
      path: '/collections',
      label: 'Collections Home',
      icon: CircleDollarSign,
      permission: 'CLX_VIEW',
      alsoPermissions: ['CLX_UNAPPLIED_WORK', 'CLX_SETUP', 'CLX_REPORT_VIEW', 'CLX_AUDIT_VIEW'],
      component: lazyWithRetry(() => import('./CollectionsHomePage')),
    },
    {
      path: '/collections/worklist',
      label: 'PR Worklist',
      icon: ListChecks,
      permission: 'CLX_VIEW',
      component: lazyWithRetry(() => import('./WorklistPage')),
    },
    {
      path: '/collections/items/:invoiceNo',
      label: 'Collection Account',
      icon: WalletCards,
      permission: 'CLX_VIEW',
      component: lazyWithRetry(() => import('./AccountPage')),
      hidden: true,
    },
    {
      path: '/collections/clients/:clientCode',
      label: 'Client View',
      icon: Users,
      permission: 'CLX_VIEW',
      component: lazyWithRetry(() => import('./ClientViewPage')),
      hidden: true,
    },
    ...PLAN_SCREENS,
    ...ESCALATION_SCREENS,
    ...BILLING_SCREENS,
    ...UNAPPLIED_SCREENS,
    {
      path: '/collections/assignments',
      label: 'Assignments',
      icon: UserCog,
      permission: 'CLX_ASSIGN',
      component: lazyWithRetry(() => import('./AssignmentsPage')),
    },
    {
      path: '/collections/files',
      label: 'Collections Files',
      icon: FileSpreadsheet,
      permission: 'CLX_REPORT_VIEW',
      alsoPermissions: ['CLX_EXPORT'],
      component: lazyWithRetry(() => import('./FilesPage')),
    },
    {
      path: '/collections/setup',
      label: 'Collections Setup',
      icon: Settings2,
      permission: 'CLX_SETUP',
      component: lazyWithRetry(() => import('./SetupPage')),
    },
  ],
};
