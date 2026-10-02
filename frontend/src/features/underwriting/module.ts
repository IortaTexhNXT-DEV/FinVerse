import { ClipboardList, FilePlus2, FileText, Package, Ship } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const PolicyFormPage = lazy(() => import('./PolicyFormPage'));

export const underwritingModule: FeatureModule = {
  id: 'underwriting',
  section: 'Underwriting',
  screens: [
    {
      path: '/underwriting/policies',
      label: 'Policies',
      icon: FileText,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./PoliciesPage')),
    },
    {
      path: '/underwriting/policies/new',
      label: 'New Policy',
      icon: FilePlus2,
      permission: 'POLICY_MAINTAIN',
      component: PolicyFormPage,
    },
    {
      path: '/underwriting/policies/:id',
      label: 'Policy',
      icon: FileText,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./PolicyDetailPage')),
      hidden: true,
    },
    {
      path: '/underwriting/policies/:id/edit',
      label: 'Edit Policy',
      icon: FileText,
      permission: 'POLICY_MAINTAIN',
      component: PolicyFormPage,
      hidden: true,
    },
    {
      path: '/underwriting/quotations',
      label: 'Quotations',
      icon: ClipboardList,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./QuotationsPage')),
    },
    {
      path: '/underwriting/quotations/:id',
      label: 'Quotation',
      icon: ClipboardList,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./QuotationDetailPage')),
      hidden: true,
    },
    {
      path: '/underwriting/open-covers',
      label: 'Open Covers',
      icon: Ship,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./OpenCoversPage')),
    },
    {
      path: '/underwriting/open-covers/:id',
      label: 'Open Cover',
      icon: Ship,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./OpenCoverDetailPage')),
      hidden: true,
    },
    {
      path: '/underwriting/products',
      label: 'Products',
      icon: Package,
      permission: 'POLICY_VIEW',
      component: lazy(() => import('./ProductsPage')),
    },
  ],
};
