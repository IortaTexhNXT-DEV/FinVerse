import { accountingEngineModule } from '@/features/accounting-engine/module';
import { adminModule } from '@/features/admin/module';
import { withOverviewScreens } from '@/features/approvals/module';
import { assetsModule } from '@/features/assets/module';
import { claimsModule } from '@/features/claims/module';
import { planningModule } from '@/features/closing/module';
import { dashboardModule } from '@/features/dashboard/module';
import { glModule } from '@/features/gl/module';
import { helpModule } from '@/features/help/module';
import { withJournalAutomation } from '@/features/journaltools/module';
import { payablesModule } from '@/features/payables/module';
import { receivablesModule } from '@/features/receivables/module';
import { reportsModule } from '@/features/reports/module';
import { setupModule } from '@/features/setup/module';
import { underwritingModule } from '@/features/underwriting/module';
import type { FeatureModule } from './types';

/**
 * Sidebar sections in display order. To add a module: create `features/<name>/module.ts`
 * exporting a FeatureModule and list it here. Routes are generated from the same definitions.
 * Platform screens extend existing sections: My Approvals and Alerts join Overview, recurring
 * journals and journal upload join General Ledger.
 */
export const MODULES: FeatureModule[] = [
  withOverviewScreens(dashboardModule),
  withJournalAutomation(glModule),
  underwritingModule,
  claimsModule,
  receivablesModule,
  payablesModule,
  assetsModule,
  planningModule,
  accountingEngineModule,
  reportsModule,
  setupModule,
  adminModule,
  helpModule,
];
