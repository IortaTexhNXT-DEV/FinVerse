import { FilePlus2, FileStack, Gift, Upload, Wallet } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Accounts & Placement: New Business accounts from draft to booking
 * (docs/architecture/BROKING_ARCHITECTURE.md, account module).
 */
export const accountsModule: FeatureModule = {
  id: 'accounts',
  section: 'Accounts & Placement',
  screens: [
    {
      path: '/accounts',
      label: 'Accounts',
      icon: FileStack,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./AccountsPage')),
    },
    {
      path: '/accounts/new',
      label: 'New Account',
      icon: FilePlus2,
      permission: 'ACCOUNT_MAINTAIN',
      component: lazy(() => import('./AccountWizardPage')),
    },
    {
      path: '/accounts/:id',
      label: 'Account',
      icon: FileStack,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./AccountDetailPage')),
      hidden: true,
    },
    {
      path: '/accounts/:id/edit',
      label: 'Edit Account',
      icon: FilePlus2,
      permission: 'ACCOUNT_MAINTAIN',
      component: lazy(() => import('./AccountWizardPage')),
      hidden: true,
    },
    {
      path: '/accounts/by-arn/:arn',
      label: 'Account by ARN',
      icon: FileStack,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./AccountByArnPage')),
      hidden: true,
    },
    {
      path: '/accounts/ffy',
      label: 'FFY Register',
      icon: Gift,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./FfyRegisterPage')),
    },
    {
      path: '/accounts/direct-payment',
      label: 'Direct Payment',
      icon: Wallet,
      permission: 'ACCOUNT_VIEW',
      component: lazy(() => import('./DirectPaymentPage')),
    },
    {
      // More specific than /bulk/:handler: account uploads need the product parameter.
      path: '/bulk/ACCOUNT_CREATE',
      label: 'Bulk Account Creation',
      icon: Upload,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./AccountBulkPage')),
      hidden: true,
    },
  ],
};
