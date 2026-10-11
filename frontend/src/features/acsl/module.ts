import { FileCheck2, FileSpreadsheet, FolderSearch, Scale, Scale3d } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

const ACSL_ROLES = ['ACSL_PROCESS', 'ACSL_ASSIGN', 'ACSL_REVIEW', 'ACSL_APPROVE'];

/**
 * ACSL - Accounting Control and Sub-Ledger (BRD-5, ACSL 2.2-2.16;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 11): cases, correction entries,
 * insurer SOA reconciliation and GL-SL reconciliation (wave A1-PRQ).
 */
export const acslModule: FeatureModule = {
  id: 'acsl',
  section: 'ACSL',
  screens: [
    {
      path: '/acsl',
      label: 'ACSL Cases',
      icon: Scale,
      permission: 'ACSL_VIEW',
      alsoPermissions: ACSL_ROLES,
      component: lazyWithRetry(() => import('./AcslHomePage')),
    },
    {
      path: '/acsl/corrections',
      label: 'Correction Entries',
      icon: FileCheck2,
      permission: 'ACSL_VIEW',
      alsoPermissions: ACSL_ROLES,
      component: lazyWithRetry(() => import('./CorrectionsPage')),
    },
    {
      path: '/acsl/soa',
      label: 'Insurer SOA Reconciliation',
      icon: FileSpreadsheet,
      permission: 'ACSL_VIEW',
      alsoPermissions: [...ACSL_ROLES, 'ACSL_UPLOAD'],
      component: lazyWithRetry(() => import('./SoaUploadsPage')),
    },
    {
      path: '/acsl/gl-sl',
      label: 'GL-SL Reconciliation',
      icon: Scale3d,
      permission: 'ACSL_VIEW',
      alsoPermissions: ACSL_ROLES,
      component: lazyWithRetry(() => import('./GlSlPage')),
    },
    {
      path: '/acsl/cases/:id',
      label: 'ACSL Case',
      icon: FolderSearch,
      permission: 'ACSL_VIEW',
      alsoPermissions: ACSL_ROLES,
      component: lazyWithRetry(() => import('./CaseDetailPage')),
      hidden: true,
    },
    {
      path: '/acsl/corrections/:id',
      label: 'Correction Entry',
      icon: FileCheck2,
      permission: 'ACSL_VIEW',
      alsoPermissions: ACSL_ROLES,
      component: lazyWithRetry(() => import('./CorrectionDetailPage')),
      hidden: true,
    },
    {
      path: '/acsl/soa/:id',
      label: 'Insurer SOA',
      icon: FileSpreadsheet,
      permission: 'ACSL_VIEW',
      alsoPermissions: [...ACSL_ROLES, 'ACSL_UPLOAD'],
      component: lazyWithRetry(() => import('./SoaUploadDetailPage')),
      hidden: true,
    },
  ],
};
