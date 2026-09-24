import { accountingEngineModule } from '@/features/accounting-engine/module';
import { adminModule } from '@/features/admin/module';
import { withOverviewScreens } from '@/features/approvals/module';
import { brokingSetupModule } from '@/features/brokingsetup/module';
import { bulkModule } from '@/features/bulk/module';
import { assetsModule } from '@/features/assets/module';
import { claimsModule } from '@/features/claims/module';
import { planningModule } from '@/features/closing/module';
import { dashboardModule } from '@/features/dashboard/module';
import { glModule } from '@/features/gl/module';
import { helpModule } from '@/features/help/module';
import { withJournalAutomation } from '@/features/journaltools/module';
import { payablesModule } from '@/features/payables/module';
import { receivablesModule } from '@/features/receivables/module';
import { reinsuranceModule } from '@/features/reinsurance/module';
import { reportsModule } from '@/features/reports/module';
import { reservesModule } from '@/features/reserves/module';
import { setupModule } from '@/features/setup/module';
import { taxModule } from '@/features/tax/module';
import { underwritingModule } from '@/features/underwriting/module';
import { workspaceModule } from '@/features/workspace/module';
import type { FeatureModule } from './types';

/**
 * Sidebar sections in display order. To add a module: create `features/<name>/module.ts`
 * exporting a FeatureModule and list it here. Routes are generated from the same definitions.
 * Platform screens extend existing sections: My Approvals and Alerts join Overview, recurring
 * journals and journal upload join General Ledger.
 */
export const MODULES: FeatureModule[] = [
  withOverviewScreens(dashboardModule),
  // Broking (BDOI New Business) - docs/architecture/BROKING_ARCHITECTURE.md section 5.
  workspaceModule,
  bulkModule,
  brokingSetupModule,
  // Insurer core and finance.
  withJournalAutomation(glModule),
  underwritingModule,
  claimsModule,
  reinsuranceModule,
  receivablesModule,
  payablesModule,
  assetsModule,
  planningModule,
  reservesModule,
  accountingEngineModule,
  taxModule,
  reportsModule,
  setupModule,
  adminModule,
  helpModule,
];
