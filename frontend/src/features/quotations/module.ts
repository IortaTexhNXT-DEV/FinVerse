import { FilePlus2, FileText, Inbox, Upload } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Quotation / Proposal: package quotations from request to accounts
 * (docs/architecture/BROKING_ARCHITECTURE.md section 11).
 */
export const quotationsModule: FeatureModule = {
  id: 'quotations',
  section: 'Quotation / Proposal',
  screens: [
    {
      path: '/quotations',
      label: 'Quotations',
      icon: FileText,
      permission: 'QUOTE_VIEW',
      component: lazy(() => import('./QuotationsPage')),
    },
    {
      path: '/quotations/new',
      label: 'New Quotation',
      icon: FilePlus2,
      permission: 'QUOTE_MAINTAIN',
      component: lazy(() => import('./QuotationWizardPage')),
    },
    {
      path: '/quotations/requests',
      label: 'Quotation Requests',
      icon: Inbox,
      permission: 'QUOTE_VIEW',
      component: lazy(() => import('./RequestsPage')),
    },
    {
      path: '/quotations/:id',
      label: 'Quotation',
      icon: FileText,
      permission: 'QUOTE_VIEW',
      component: lazy(() => import('./QuotationDetailPage')),
      hidden: true,
    },
    {
      path: '/quotations/:id/edit',
      label: 'Edit Quotation',
      icon: FilePlus2,
      permission: 'QUOTE_MAINTAIN',
      component: lazy(() => import('./QuotationWizardPage')),
      hidden: true,
    },
    {
      // More specific than /bulk/:handler: the upload needs the product parameter.
      path: '/bulk/QUOTATION_CREATE',
      label: 'Bulk Quotations',
      icon: Upload,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./QuotationBulkPage')),
      hidden: true,
    },
    {
      path: '/bulk/QUOTATION_ACCEPTANCE',
      label: 'Bulk Quotation Acceptance',
      icon: Upload,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./AcceptanceBulkPage')),
      hidden: true,
    },
  ],
};
