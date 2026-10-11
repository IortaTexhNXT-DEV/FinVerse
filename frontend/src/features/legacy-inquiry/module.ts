import { FileSearch, ScrollText } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Legacy Inquiry (BRD-13; docs/architecture/DATA_MIGRATION_DESIGN.md section 16): the read-only
 * archive of the legacy systems for Audit and Compliance, and the access log that records every
 * search, view, download and export.
 */
export const legacyInquiryModule: FeatureModule = {
  id: 'legacy-inquiry',
  section: 'Legacy Inquiry',
  screens: [
    {
      path: '/legacy-inquiry',
      label: 'Legacy Inquiry',
      icon: FileSearch,
      permission: 'LEGACY_INQUIRY_VIEW',
      component: lazyWithRetry(() => import('./LegacyInquiryPage')),
    },
    {
      path: '/legacy-inquiry/access-log',
      label: 'Access Log',
      icon: ScrollText,
      permission: 'LEGACY_ACCESS_LOG_VIEW',
      component: lazyWithRetry(() => import('./AccessLogPage')),
    },
  ],
};
