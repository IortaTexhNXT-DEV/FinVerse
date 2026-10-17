import { FileText, Mail, Route } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import { NBADMIN_SCREENS } from '@/features/nbadmin/screens';
import type { FeatureModule } from '@/navigation/types';

/**
 * Broking Setup: communication log, document templates and the case routing of the contact
 * centre. The broking modules add their
 * masters here (lists of values, products and insurers, access requests...).
 */
export const brokingSetupModule: FeatureModule = {
  id: 'broking-setup',
  section: 'Broking Setup',
  screens: [
    ...NBADMIN_SCREENS,
    {
      path: '/broking-setup/case-routing',
      label: 'Case Routing',
      icon: Route,
      permission: 'CSF_CASE_ROUTING',
      component: lazyWithRetry(() => import('@/features/csf/cases/CaseRoutingPage')),
    },
    {
      path: '/broking-setup/templates',
      label: 'Document Templates',
      icon: FileText,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./DocumentTemplatesPage')),
    },
    {
      path: '/broking-setup/messages',
      label: 'Outbound Messages',
      icon: Mail,
      permission: 'MESSAGE_VIEW',
      component: lazyWithRetry(() => import('./OutboundMessagesPage')),
    },
  ],
};
