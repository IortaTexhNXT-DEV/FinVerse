import { FileUp, Upload } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/** Bulk Processing: generic uploads offered by the broking modules. */
export const bulkModule: FeatureModule = {
  id: 'bulk',
  section: 'Bulk Processing',
  screens: [
    {
      path: '/bulk',
      label: 'Bulk Uploads',
      icon: Upload,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./BulkCenterPage')),
    },
    {
      path: '/bulk/:handler',
      label: 'Bulk Upload',
      icon: FileUp,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./BulkUploadPage')),
      hidden: true,
    },
  ],
};
