import { BarChart3, History, Headset, UserRound } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';
import { CSF_SECTION } from './csfCodes';

/**
 * Customer Service Facility (BRD-9; docs/architecture/CUSTOMER_SERVICING_DESIGN.md section 10): the
 * Customer Search, the landing page of the contact centre roles, the Servicing View of a client,
 * the Contact Changes list and the CSF reports, in Client & Policy.
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
  ],
};
