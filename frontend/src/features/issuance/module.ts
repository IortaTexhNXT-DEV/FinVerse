import { FileBadge, FileSearch, FileUp, LayoutList, MailCheck, Send } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Policy Issuance: e-policy receipt and extraction review, Insurance Advice and e-policy dispatch
 * (docs/architecture/BROKING_ARCHITECTURE.md, issuance module).
 */
export const issuanceModule: FeatureModule = {
  id: 'issuance',
  section: 'Policy Issuance',
  screens: [
    {
      path: '/issuance',
      label: 'Issuance Workbench',
      icon: LayoutList,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['EPOLICY_MANAGE', 'EPOLICY_SEND'],
      component: lazyWithRetry(() => import('./IssuanceWorkbenchPage')),
    },
    {
      path: '/issuance/upload',
      label: 'E-policy Upload',
      icon: FileUp,
      permission: 'EPOLICY_MANAGE',
      component: lazyWithRetry(() => import('./EpolicyUploadPage')),
    },
    {
      path: '/issuance/epolicies/:id',
      label: 'Extraction Review',
      icon: FileSearch,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['EPOLICY_MANAGE', 'EPOLICY_SEND'],
      component: lazyWithRetry(() => import('./ExtractionReviewPage')),
      hidden: true,
    },
    {
      path: '/issuance/insurance-advice',
      label: 'Insurance Advice',
      icon: FileBadge,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['EPOLICY_MANAGE', 'EPOLICY_SEND'],
      component: lazyWithRetry(() => import('./InsuranceAdvicePage')),
    },
    {
      path: '/issuance/advice-recipients',
      label: 'Insurance Advice Recipients',
      icon: MailCheck,
      permission: 'MASTER_VIEW',
      alsoPermissions: ['LOV_MANAGE', 'MASTER_AUTHORIZE', 'EPOLICY_MANAGE', 'EPOLICY_SEND'],
      component: lazyWithRetry(() => import('./AdviceRecipientsPage')),
    },
    {
      path: '/issuance/dispatch',
      label: 'E-policy Dispatch',
      icon: Send,
      permission: 'EPOLICY_SEND',
      alsoPermissions: ['EPOLICY_MANAGE'],
      component: lazyWithRetry(() => import('./DispatchPage')),
    },
  ],
};
