import {
  ArrowDownToLine,
  ArrowUpFromLine,
  FileUp,
  Flag,
  Layers,
  SlidersVertical,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const ANY = [
  'CONFIG_EXPORT',
  'CONFIG_IMPORT_PREPARE',
  'CONFIG_IMPORT_APPROVE',
  'CONFIG_BASELINE_MANAGE',
];

/** The permissions that upload or approve the data of a configuration screen. */
const UPLOADS = [
  'MASTER_MAINTAIN',
  'EMPLOYEE_MAINTAIN',
  'ACCOUNTING_RULE_MANAGE',
  'MIG_MAPPING_EDIT',
  'TAX_MANAGE',
  'RECONCILIATION_MANAGE',
  'PRODUCT_MAINTAIN',
  'PRODUCT_AUTHORIZE',
  'LOV_MANAGE',
  'INCENTIVE_CRITERIA_MAINTAIN',
  'ROLE_MANAGE',
  'USER_MANAGE',
  'ACCESS_APPROVE',
  'UAM_SOD_MAINTAIN',
  'UAM_SOD_AUTHORIZE',
  'SYSTEM_PARAMETER_MANAGE',
  'SECURITY_PARAMETER_APPROVE',
];

/**
 * Configuration Promotion (System Administration): move the set-up of the platform between
 * environments without transactions. docs/modules/CONFIG_PROMOTION.md.
 */
export const configPromotionModule: FeatureModule = {
  id: 'config-promotion',
  section: 'Configuration Promotion',
  screens: [
    {
      path: '/admin/config-promotion/export',
      label: 'Export Configuration',
      icon: ArrowUpFromLine,
      permission: 'CONFIG_EXPORT',
      component: lazy(() => import('./ExportPage')),
    },
    {
      path: '/admin/config-promotion/imports',
      label: 'Import Configuration',
      icon: ArrowDownToLine,
      permission: 'CONFIG_IMPORT_PREPARE',
      // The approvers open the imports waiting for them.
      alsoPermissions: ['CONFIG_IMPORT_APPROVE'],
      component: lazy(() => import('./ImportsPage')),
    },
    {
      path: '/admin/config-promotion/packages',
      label: 'Packages and History',
      icon: Layers,
      permission: 'CONFIG_EXPORT',
      alsoPermissions: ANY,
      component: lazy(() => import('./PackagesPage')),
    },
    {
      path: '/admin/config-promotion/baselines',
      label: 'Baselines and Drift',
      icon: Flag,
      permission: 'CONFIG_BASELINE_MANAGE',
      alsoPermissions: ANY,
      component: lazy(() => import('./BaselinesPage')),
    },
    {
      path: '/admin/config-promotion/overrides',
      label: 'Environment Overrides',
      icon: SlidersVertical,
      permission: 'CONFIG_EXPORT',
      alsoPermissions: ANY,
      component: lazy(() => import('./OverridesPage')),
    },
    {
      path: '/admin/config-uploads',
      label: 'Configuration Uploads',
      icon: FileUp,
      // Opened from the Upload action of the configuration screens and from My Approvals.
      hidden: true,
      permission: 'MASTER_AUTHORIZE',
      alsoPermissions: UPLOADS,
      component: lazy(() => import('./ConfigUploadsPage')),
    },
  ],
};
