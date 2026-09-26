import { accountingEngineModule } from '@/features/accounting-engine/module';
import { acslModule } from '@/features/acsl/module';
import { accountsModule } from '@/features/accounts/module';
import { adjustmentModule } from '@/features/adjustment/module';
import { adminModule } from '@/features/admin/module';
import { withOverviewScreens } from '@/features/approvals/module';
import { brokerClaimsModule } from '@/features/brokerclaims/module';
import { brokingSetupModule } from '@/features/brokingsetup/module';
import { cashieringModule } from '@/features/cashiering/module';
import { catalogModule } from '@/features/catalog/module';
import { bookingModule } from '@/features/booking/module';
import { bulkModule } from '@/features/bulk/module';
import { assetsModule } from '@/features/assets/module';
import { claimsModule } from '@/features/claims/module';
import { collectionsModule } from '@/features/collections/module';
import { commissionModule } from '@/features/commission/module';
import { crmModule } from '@/features/crm/module';
import { proposalsModule } from '@/features/proposals/module';
import { quotationsModule } from '@/features/quotations/module';
import { planningModule } from '@/features/closing/module';
import { dashboardModule } from '@/features/dashboard/module';
import { ebModule } from '@/features/eb/module';
import { disbursementModule } from '@/features/disbursement/module';
import { frbsModule } from '@/features/frbs/module';
import { glModule } from '@/features/gl/module';
import { helpModule } from '@/features/help/module';
import { issuanceModule } from '@/features/issuance/module';
import { userAccessModule } from '@/features/nbadmin/userAccessModule';
import { nbDashboardModule, nbReportsModule } from '@/features/nbreports/module';
import { operationsModule } from '@/features/operations/module';
import { withJournalAutomation } from '@/features/journaltools/module';
import { payablesModule } from '@/features/payables/module';
import { payRequestModule } from '@/features/payrequest/module';
import { placementModule } from '@/features/placement/module';
import { prodreconModule } from '@/features/prodrecon/module';
import { withPackageRequests } from '@/features/productmaint/module';
import { receivablesModule } from '@/features/receivables/module';
import { reinsuranceModule } from '@/features/reinsurance/module';
import { remittanceModule } from '@/features/remittance/module';
import { reportsModule } from '@/features/reports/module';
import { reservesModule } from '@/features/reserves/module';
import { screeningModule } from '@/features/screening/module';
import { screeningSetupModule } from '@/features/screening/setupModule';
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
  {
    id: 'home',
    modules: [withOverviewScreens(dashboardModule), nbDashboardModule, workspaceModule],
  },
  // Broking (BDOI New Business) - docs/architecture/BROKING_ARCHITECTURE.md section 5.
  {
    id: 'client-policy',
    title: 'Client & Policy',
    modules: [
      crmModule,
      // Sanction Screening (BRD-10) after Client Management, SANCTION_SCREENING_DESIGN 11.2.
      screeningModule,
      quotationsModule,
      accountsModule,
      proposalsModule,
      // Employee Benefits (BRD-8) after Non-Package Management, EMPLOYEE_BENEFITS_DESIGN 10.1.
      ebModule,
      placementModule,
      issuanceModule,
      bookingModule,
      // Operations (BRD-2), prototype placement: Production Reconciliation and Adjustment here.
      prodreconModule,
      adjustmentModule,
      // Product Maintenance (BRD-3): catalog screens with the package request screens.
      withPackageRequests(catalogModule),
      bulkModule,
    ],
  },
  // Operations (BRD-2) foundation: home, invoice ledger, Disbursement queue, interfaces.
  // docs/architecture/OPERATIONS_DESIGN.md section 12.
  { id: 'operations', title: 'Operations', modules: [operationsModule] },
  {
    id: 'finance',
    title: 'Finance',
    modules: [
      // Collections (BRD-4) first, before the Operations cash modules (COLLECTIONS_DESIGN 11).
      collectionsModule,
      // Operations cash modules (BDOI prototype placement).
      cashieringModule,
      remittanceModule,
      commissionModule,
      // Accounting, Disbursement and ACSL (BRD-5), ACCOUNTING_DISBURSEMENT_DESIGN section 11.
      disbursementModule,
      payRequestModule,
      acslModule,
      frbsModule,
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
    modules: [
      // Claims Handling (BRD-7) first, CLAIMS_BROKING_DESIGN section 11.
      brokerClaimsModule,
      underwritingModule,
      claimsModule,
      reinsuranceModule,
      reservesModule,
    ],
  },
  { id: 'reports', title: 'Reports', modules: [nbReportsModule, reportsModule] },
  {
    id: 'setup',
    title: 'Setup & Administration',
    modules: [
      brokingSetupModule,
      // Compliance Setup (BRD-10), SANCTION_SCREENING_DESIGN 11.2.
      screeningSetupModule,
      setupModule,
      // User Access (BRD-11) before Administration, USER_ACCESS_DESIGN 11.2.
      userAccessModule,
      adminModule,
      helpModule,
    ],
  },
];

/** All modules in menu order (routes, help and access checks use the flat list). */
export const MODULES: FeatureModule[] = NAV_GROUPS.flatMap((group) => group.modules);
