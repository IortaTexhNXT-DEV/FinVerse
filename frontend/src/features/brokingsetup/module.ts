import { FileText, Mail } from 'lucide-react';
import { lazy } from 'react';
import { NBADMIN_SCREENS } from '@/features/nbadmin/screens';
import type { FeatureModule } from '@/navigation/types';

/**
 * Broking Setup: communication log and document templates. The broking modules add their
 * masters here (lists of values, products and insurers, access requests...).
 */
export const brokingSetupModule: FeatureModule = {
  id: 'broking-setup',
  section: 'Broking Setup',
  screens: [
    ...NBADMIN_SCREENS,
    {
      path: '/broking-setup/templates',
      label: 'Document Templates',
      icon: FileText,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./DocumentTemplatesPage')),
    },
    {
      path: '/broking-setup/messages',
      label: 'Outbound Messages',
      icon: Mail,
      permission: 'MESSAGE_VIEW',
      component: lazy(() => import('./OutboundMessagesPage')),
    },
  ],
};
