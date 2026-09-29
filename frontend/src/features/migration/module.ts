import {
  ArrowLeftRight,
  CalendarClock,
  Archive,
  ClipboardCheck,
  Database,
  FileSpreadsheet,
  FileUp,
  Layers,
  ListTree,
  Scale,
  Scroll,
  Users,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/** Work permissions of the migration roles: they land on Migration Home after sign-in. */
const MIGRATION_WORK = [
  'MIG_INTAKE',
  'MIG_LOAD_RUN',
  'MIG_LOAD_APPROVE',
  'MIG_OBJECT_MANAGE',
  'MIG_MAPPING_EDIT',
  'MIG_MAPPING_APPROVE',
  'MIG_DECISION_APPROVE',
  'MIG_DQ_RESOLVE',
  'MIG_DQ_WAIVE',
  'MIG_MATCH_DECIDE',
  'MIG_SIGNOFF',
  'MIG_RECON_SIGNOFF',
  'MIG_GONOGO_DECIDE',
  'MIG_CUTOVER_MANAGE',
] as const;

/**
 * Data Migration (BRD-13; docs/architecture/DATA_MIGRATION_DESIGN.md section 22): the migration
 * console in its own sidebar group - home, data objects, code maps, layouts and load templates,
 * extracts, batches, client matching, reconciliation, the opening-balance adjustments of the
 * year-end cut-over, the cutover plans with their go / no-go, the run-off and decommissioning
 * of the legacy systems, and sign-off. Migration reports are in the
 * Report Centre under Data Migration.
 */
export const migrationModule: FeatureModule = {
  id: 'migration',
  section: 'Data Migration',
  screens: [
    {
      path: '/migration',
      label: 'Migration Home',
      icon: Database,
      permission: 'MIG_VIEW',
      landingFor: MIGRATION_WORK,
      component: lazy(() => import('./home/MigrationHomePage')),
    },
    {
      path: '/migration/objects',
      label: 'Data Objects',
      icon: ListTree,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./objects/ObjectsPage')),
    },
    {
      path: '/migration/maps',
      label: 'Code Maps',
      icon: ArrowLeftRight,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./maps/CodeMapsPage')),
    },
    {
      path: '/migration/layouts',
      label: 'Layouts and Rules',
      icon: FileSpreadsheet,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./layouts/LayoutsPage')),
    },
    {
      path: '/migration/extracts',
      label: 'Extracts',
      icon: FileUp,
      permission: 'MIG_INTAKE',
      component: lazy(() => import('./extracts/ExtractsPage')),
    },
    {
      path: '/migration/batches',
      label: 'Batches',
      icon: Layers,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./batches/BatchesPage')),
    },
    {
      path: '/migration/batches/:batchNo',
      label: 'Batch',
      icon: Layers,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./batches/BatchPage')),
      hidden: true,
    },
    {
      path: '/migration/matching',
      label: 'Client Matching',
      icon: Users,
      permission: 'MIG_MATCH_DECIDE',
      component: lazy(() => import('./matching/MatchingPage')),
    },
    {
      path: '/migration/reconciliation',
      label: 'Reconciliation',
      icon: Scale,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./reconciliation/ReconciliationPage')),
    },
    {
      path: '/migration/trueups',
      label: 'Opening Adjustments',
      icon: Scroll,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./trueups/TrueUpsPage')),
    },
    {
      path: '/migration/cutover',
      label: 'Cutover',
      icon: CalendarClock,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./cutover/CutoverPage')),
    },
    {
      path: '/migration/runoff',
      label: 'Run-off and Decommissioning',
      icon: Archive,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./cutover/RunoffPage')),
    },
    {
      path: '/migration/signoff',
      label: 'Sign-off',
      icon: ClipboardCheck,
      permission: 'MIG_VIEW',
      component: lazy(() => import('./signoff/SignoffPage')),
    },
  ],
};
