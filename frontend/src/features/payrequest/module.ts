import { FilePen } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Refund and cash-advance requests (BRD-5, MKT 1.2-2.26;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 11): refund requests (RRF), cash
 * advance requests (RFP) and disbursed-check cancellations. Registered by the foundation (A0);
 * wave A1-PRQ adds its screens (routes under /payment-requests).
 */
export const payRequestModule: FeatureModule = {
  id: 'payrequest',
  section: 'Refund & Cash Advance Requests',
  screens: [
    {
      path: '/payment-requests',
      label: 'Requests Home',
      icon: FilePen,
      permission: 'PRQ_VIEW',
      alsoPermissions: ['PRQ_CREATE', 'PRQ_REVIEW', 'PRQ_APPROVE', 'PRQ_HR_APPROVE'],
      component: lazy(() => import('./PayRequestsHomePage')),
    },
  ],
};
