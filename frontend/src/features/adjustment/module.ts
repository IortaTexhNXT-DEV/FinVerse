import { Eraser, FilePen, FilePlus2, FileText, Layers, Upload } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Adjustment (ADJID.001-028, MKTID.008; docs/architecture/OPERATIONS_DESIGN.md sections 4.5 and
 * 12): the workbench of endorsement requests, the new request wizard, the request page, posting
 * batches, the batch upload of requests and the minimal balance file.
 */
export const adjustmentModule: FeatureModule = {
  id: 'adjustment',
  section: 'Adjustment',
  screens: [
    {
      path: '/adjustment',
      label: 'Adjustment Workbench',
      icon: FilePen,
      permission: 'ADJ_PROCESS',
      alsoPermissions: ['ADJ_REQUEST', 'ADJ_APPROVE', 'ADJ_POST'],
      component: lazyWithRetry(() => import('./AdjustmentHomePage')),
    },
    {
      path: '/adjustment/new',
      label: 'New Request',
      icon: FilePlus2,
      permission: 'ADJ_REQUEST',
      alsoPermissions: ['ADJ_PROCESS'],
      component: lazyWithRetry(() => import('./NewRequestPage')),
    },
    {
      path: '/adjustment/requests/:id',
      label: 'Endorsement Request',
      icon: FileText,
      permission: 'ADJ_PROCESS',
      alsoPermissions: ['ADJ_REQUEST', 'ADJ_APPROVE', 'ADJ_POST', 'OPS_VIEW'],
      component: lazyWithRetry(() => import('./RequestDetailPage')),
      hidden: true,
    },
    {
      path: '/adjustment/requests/:id/edit',
      label: 'Change Request',
      icon: FilePen,
      permission: 'ADJ_REQUEST',
      alsoPermissions: ['ADJ_PROCESS'],
      component: lazyWithRetry(() => import('./NewRequestPage')),
      hidden: true,
    },
    {
      path: '/adjustment/batches',
      label: 'Posting Batches',
      icon: Layers,
      permission: 'ADJ_POST',
      component: lazyWithRetry(() => import('./PostingBatchesPage')),
    },
    {
      path: '/adjustment/upload',
      label: 'Batch Request Upload',
      icon: Upload,
      permission: 'ADJ_POST',
      component: lazyWithRetry(() => import('./BatchUploadPage')),
    },
    {
      path: '/adjustment/minimal-balance',
      label: 'Minimal Balance File',
      icon: Eraser,
      permission: 'ADJ_POST',
      component: lazyWithRetry(() => import('./MinimalBalancePage')),
    },
  ],
};
