import {
  CirclePause,
  FileCheck,
  Layers,
  MinusCircle,
  PackageSearch,
  Percent,
  Scale,
  Send,
  Zap,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const TEAM = ['REMIT_PROCESS', 'REMIT_EXTRACT', 'REMIT_APPROVE', 'REMIT_OR_UPLOAD'];
const DEDUCTION_READERS = [
  'ACSL_VIEW',
  'REMIT_DEDUCTION_CONFIRM',
  'REMIT_PROCESS',
  'REMIT_APPROVE',
];

/**
 * Remittance (RMTID.001-040, MKTID.001-009; docs/architecture/OPERATIONS_DESIGN.md section 12):
 * the workbench, extraction, batches with Process Remittance, insurer OR upload, holds, special
 * remittance, DTIP status, the insurer-confirmed deductions (ACSL 2.9.2) and the early-remittance
 * incentive rules.
 */
export const remittanceModule: FeatureModule = {
  id: 'remittance',
  section: 'Remittance',
  screens: [
    {
      path: '/remittance',
      label: 'Remittance Workbench',
      icon: Send,
      permission: 'REMIT_PROCESS',
      alsoPermissions: [
        'REMIT_EXTRACT',
        'REMIT_APPROVE',
        'REMIT_EXCLUDE',
        'REMIT_OR_UPLOAD',
        'HOLD_REQUEST',
        'HOLD_APPROVE',
        'SPECIAL_REMIT_REQUEST',
        'SPECIAL_REMIT_APPROVE',
      ],
      component: lazy(() => import('./RemittanceHomePage')),
    },
    {
      path: '/remittance/extraction',
      label: 'Extraction',
      icon: PackageSearch,
      permission: 'REMIT_EXTRACT',
      alsoPermissions: TEAM,
      component: lazy(() => import('./ExtractionPage')),
    },
    {
      path: '/remittance/batches',
      label: 'Remittance Batches',
      icon: Layers,
      permission: 'REMIT_PROCESS',
      alsoPermissions: TEAM,
      component: lazy(() => import('./BatchesPage')),
    },
    {
      path: '/remittance/batches/:id',
      label: 'Remittance Batch',
      icon: Layers,
      permission: 'REMIT_PROCESS',
      alsoPermissions: TEAM,
      component: lazy(() => import('./BatchDetailPage')),
      hidden: true,
    },
    {
      path: '/remittance/insurer-or',
      label: 'Insurer OR Upload',
      icon: FileCheck,
      permission: 'REMIT_OR_UPLOAD',
      component: lazy(() => import('./InsurerOrPage')),
    },
    {
      path: '/remittance/holds',
      label: 'Remittance Holds',
      icon: CirclePause,
      permission: 'HOLD_REQUEST',
      alsoPermissions: ['HOLD_APPROVE', 'REMIT_PROCESS', 'REMIT_APPROVE'],
      component: lazy(() => import('./HoldsPage')),
    },
    {
      path: '/remittance/holds/:id',
      label: 'Remittance Hold',
      icon: CirclePause,
      permission: 'HOLD_REQUEST',
      alsoPermissions: ['HOLD_APPROVE', 'REMIT_PROCESS', 'REMIT_APPROVE'],
      component: lazy(() => import('./HoldDetailPage')),
      hidden: true,
    },
    {
      path: '/remittance/special',
      label: 'Special Remittance',
      icon: Zap,
      permission: 'SPECIAL_REMIT_REQUEST',
      alsoPermissions: ['SPECIAL_REMIT_APPROVE', 'REMIT_PROCESS'],
      component: lazy(() => import('./SpecialPage')),
    },
    {
      path: '/remittance/special/:id',
      label: 'Special Remittance Request',
      icon: Zap,
      permission: 'SPECIAL_REMIT_REQUEST',
      alsoPermissions: ['SPECIAL_REMIT_APPROVE', 'REMIT_PROCESS'],
      component: lazy(() => import('./SpecialDetailPage')),
      hidden: true,
    },
    {
      path: '/remittance/dtip',
      label: 'DTIP Status',
      icon: Scale,
      permission: 'REMIT_PROCESS',
      alsoPermissions: TEAM,
      component: lazy(() => import('./DtipStatusPage')),
    },
    {
      path: '/remittance/deductions',
      label: 'Remittance Deductions',
      icon: MinusCircle,
      permission: 'ACSL_PROCESS',
      alsoPermissions: DEDUCTION_READERS,
      component: lazy(() => import('./DeductionsPage')),
    },
    {
      path: '/remittance/deductions/:id',
      label: 'Remittance Deduction',
      icon: MinusCircle,
      permission: 'ACSL_PROCESS',
      alsoPermissions: DEDUCTION_READERS,
      component: lazy(() => import('./DeductionDetailPage')),
      hidden: true,
    },
    {
      path: '/remittance/incentive-rules',
      label: 'Incentive Rules',
      icon: Percent,
      permission: 'REMIT_APPROVE',
      alsoPermissions: TEAM,
      component: lazy(() => import('./IncentiveRulesPage')),
    },
  ],
};
