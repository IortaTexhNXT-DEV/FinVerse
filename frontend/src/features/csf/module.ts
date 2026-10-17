import { BarChart3, ClipboardList, Gauge, History, Headset, UserRound } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';
import { CSF_SECTION } from './csfCodes';

/**
 * Customer Service Facility (BRD-9; docs/architecture/CUSTOMER_SERVICING_DESIGN.md section 10): the
 * Customer Search, the landing page of the contact centre roles, the Servicing View of a client,
 * the cases (work queues, case record and Case Dashboard; Case Routing is in Broking Setup), the Contact Changes list
 * and the CSF reports, in Client & Policy.
 */
export const csfModule: FeatureModule = {
  id: 'csf',
  section: CSF_SECTION,
  screens: [
    {
      path: '/csf',
      label: 'Customer Search',
      icon: Headset,
      permission: 'CSF_VIEW',
      component: lazyWithRetry(() => import('./search/CustomerSearchPage')),
    },
    {
      path: '/csf/cases',
      label: 'Cases',
      icon: ClipboardList,
      permission: 'CSF_VIEW',
      alsoPermissions: ['CSF_CASE_FULFIL'],
      component: lazyWithRetry(() => import('./cases/CasesPage')),
    },
    {
      path: '/csf/case-dashboard',
      label: 'Case Dashboard',
      icon: Gauge,
      permission: 'CSF_CASE_REPORT',
      component: lazyWithRetry(() => import('./cases/CaseDashboardPage')),
    },
    {
      path: '/csf/changes',
      label: 'Contact Changes',
      icon: History,
      permission: 'CSF_REPORT_VIEW',
      alsoPermissions: ['CSF_CONTACT_UPDATE'],
      component: lazyWithRetry(() => import('./changes/ContactChangesPage')),
    },
    {
      path: '/csf/reports',
      label: 'Customer Service Reports',
      icon: BarChart3,
      permission: 'CSF_REPORT_VIEW',
      component: lazyWithRetry(() => import('./reports/CsfReportsPage')),
    },
    {
      path: '/csf/clients/:clientId',
      label: 'Servicing View',
      icon: UserRound,
      permission: 'CSF_VIEW',
      component: lazyWithRetry(() => import('./view/ServicingViewPage')),
      hidden: true,
    },
    {
      path: '/csf/cases/:id',
      label: 'Case',
      icon: ClipboardList,
      permission: 'CSF_VIEW',
      alsoPermissions: ['CSF_CASE_FULFIL'],
      component: lazyWithRetry(() => import('./cases/CaseRecordPage')),
      hidden: true,
    },
  ],
};
