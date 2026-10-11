import { FileOutput, Gauge, Landmark, LayoutList, Receipt, ShieldCheck } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Placement & Booking: payment gate, CLPC billing, placement slips, hold cover and insurer returns
 * (docs/architecture/BROKING_ARCHITECTURE.md, placement module).
 */
export const placementModule: FeatureModule = {
  id: 'placement',
  section: 'Placement & Booking',
  screens: [
    {
      path: '/placement',
      label: 'Placement Workbench',
      icon: LayoutList,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['PLACEMENT_MANAGE', 'BILLING_MANAGE'],
      component: lazyWithRetry(() => import('./PlacementWorkbenchPage')),
    },
    {
      path: '/placement/processing-dashboard',
      label: 'Processing Dashboard',
      icon: Gauge,
      permission: 'PLACEMENT_MANAGE',
      alsoPermissions: ['BOOKING_PROCESS', 'EPOLICY_MANAGE'],
      component: lazyWithRetry(() => import('../renewal/dashboard/ProcessingDashboardPage')),
    },
    {
      path: '/placement/slips',
      label: 'Placement Slips',
      icon: FileOutput,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['PLACEMENT_MANAGE'],
      component: lazyWithRetry(() => import('./SlipsPage')),
    },
    {
      path: '/placement/billing',
      label: 'CLPC Billing',
      icon: Landmark,
      permission: 'BILLING_MANAGE',
      component: lazyWithRetry(() => import('./BillingPage')),
    },
    {
      path: '/placement/billing/reports/:id',
      label: 'Payment Report',
      icon: Receipt,
      permission: 'BILLING_MANAGE',
      component: lazyWithRetry(() => import('./PaymentReportPage')),
      hidden: true,
    },
    {
      path: '/placement/accounts/:arn',
      label: 'Account Placement',
      icon: ShieldCheck,
      permission: 'ACCOUNT_VIEW',
      alsoPermissions: ['PLACEMENT_MANAGE', 'BILLING_MANAGE'],
      component: lazyWithRetry(() => import('./AccountPlacementPage')),
      hidden: true,
    },
  ],
};
