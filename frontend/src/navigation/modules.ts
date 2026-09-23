import { accountingEngineModule } from '@/features/accounting-engine/module';
import { adminModule } from '@/features/admin/module';
import { dashboardModule } from '@/features/dashboard/module';
import { glModule } from '@/features/gl/module';
import { payablesModule } from '@/features/payables/module';
import { reportsModule } from '@/features/reports/module';
import { setupModule } from '@/features/setup/module';
import type { FeatureModule } from './types';

/**
 * Sidebar sections in display order. To add a module: create `features/<name>/module.ts`
 * exporting a FeatureModule and list it here. Routes are generated from the same definitions.
 */
export const MODULES: FeatureModule[] = [
  dashboardModule,
  glModule,
  accountingEngineModule,
  payablesModule,
  reportsModule,
  setupModule,
  adminModule,
];
