import { accountingEngineModule } from '@/features/accounting-engine/module';
import { accountsModule } from '@/features/accounts/module';
import { adminModule } from '@/features/admin/module';
import { withOverviewScreens } from '@/features/approvals/module';
import { brokingSetupModule } from '@/features/brokingsetup/module';
import { catalogModule } from '@/features/catalog/module';
import { bulkModule } from '@/features/bulk/module';
import { assetsModule } from '@/features/assets/module';
import { claimsModule } from '@/features/claims/module';
import { crmModule } from '@/features/crm/module';
import { planningModule } from '@/features/closing/module';
import { dashboardModule } from '@/features/dashboard/module';
import { glModule } from '@/features/gl/module';
import { helpModule } from '@/features/help/module';
import { issuanceModule } from '@/features/issuance/module';
import { withJournalAutomation } from '@/features/journaltools/module';
import { payablesModule } from '@/features/payables/module';
import { placementModule } from '@/features/placement/module';
import { receivablesModule } from '@/features/receivables/module';
import { reinsuranceModule } from '@/features/reinsurance/module';
import { reportsModule } from '@/features/reports/module';
import { reservesModule } from '@/features/reserves/module';
import { setupModule } from '@/features/setup/module';
import { taxModule } from '@/features/tax/module';
import { underwritingModule } from '@/features/underwriting/module';
import { workspaceModule } from '@/features/workspace/module';
import type { FeatureModule, NavGroup } from './types';

/**
 * Sidebar groups and sections in display order, following the BDOI navigation of the BDO Insure
 * UX design (docs/design/BDO_UX_GUIDELINES.md). To add a module: create
 * `features/<name>/module.ts` exporting a FeatureModule and list it in its group here. Routes are
 * generated from the same definitions. Platform screens extend existing sections: My Approvals and
 * Alerts join Overview, recurring journals and journal upload join General Ledger.
 */
export const NAV_GROUPS: NavGroup[] = [
  { id: 'home', modules: [withOverviewScreens(dashboardModule), workspaceModule] },
  // Broking (BDOI New Business) - docs/architecture/BROKING_ARCHITECTURE.md section 5.
  {
    id: 'client-policy',
    title: 'Client & Policy',
    modules: [
      crmModule,
      accountsModule,
      placementModule,
      issuanceModule,
      catalogModule,
      bulkModule,
    ],
  },
  {
    id: 'finance',
    title: 'Finance',
    modules: [
      withJournalAutomation(glModule),
      receivablesModule,
      payablesModule,
      assetsModule,
      planningModule,
      taxModule,
      accountingEngineModule,
    ],
  },
  {
    id: 'insurance',
    title: 'Claims & Insurance',
    modules: [underwritingModule, claimsModule, reinsuranceModule, reservesModule],
  },
  { id: 'reports', title: 'Reports', modules: [reportsModule] },
  {
    id: 'setup',
    title: 'Setup & Administration',
    modules: [brokingSetupModule, setupModule, adminModule, helpModule],
  },
];

/** All modules in menu order (routes, help and access checks use the flat list). */
export const MODULES: FeatureModule[] = NAV_GROUPS.flatMap((group) => group.modules);
