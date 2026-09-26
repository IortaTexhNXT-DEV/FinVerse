# Codebase relevance audit: insurer-suite code in BIBS

BrokerVerse started as a general ledger and finance suite for an insurance **company** (insurer). It was then adapted to
BDO Insurance and Reinsurance Brokers (BDOI), an insurance **broker**, as BIBS (BRD-00 to BRD-13). This audit lists the
code, data, permissions and menu entries that belong to the insurer suite and that no BDOI process uses. For each item it
records:
- the evidence of non-use;
- whether BDOI roles can see it today;
- its dependants and the Flyway migrations it owns;
- the risk of removing it, and the recommended action.

**This audit changes no code.** The recommendations need a decision from the project lead, and the reinsurance items
also need BDOI's phase 2 scope.

**Status.** Step 1 (hide) is done in the backend with migration V1064 and seed V1961; see section 4.1. The code
removal (steps 2-6) is still open.

Audit date: 26-Sep-2026, on commit `61b1df8`. Sizes are counted as files / lines under
`backend/src/main/java/com/iortatechnxt/brokerverse/<package>`, `backend/src/test/...` and `frontend/src/features/<feature>`.

## 1. Summary

| # | Item | Used by BDOI? | Visible to BDOI roles today | Recommendation | Size (main + test + UI) | Risk |
|---|---|---|---|---|---|---|
| A1 | `underwriting` (insurer policies, quotations, open covers, UW reports, `QUOTATION_EXPIRY` job) | No | Menu: not for BDOI-specific roles, yes for the shared `FIN_MANAGER` / `AUDITOR` roles. Access matrix: yes (permissions `POLICY_*`) | Hide now; remove after A2-A4 and T1 | 108 / 11,489 + 12 / 2,146 + 31 / 3,748 | Medium (tax depends on it) |
| A2 | `claims` (insurer claims, reserves, settlements, recoveries, LPOs) | No; BDOI claims are `brokerclaims` | As A1 (`CLAIM_*`) | Hide now; remove | 100 / 9,187 + 10 / 1,713 + 27 / 2,599 | Low |
| A3 | `reinsurance` (treaties, cessions, FAC, RI recoveries, RI SOA) | No in phase 1; the ReInsurance BRD is phase 2 and is a broker model | As A1 (`REINSURANCE_*`) | Hide now; decide at phase 2 (remove, reuse only the SOA layout ideas) | 87 / 10,575 + 9 / 1,822 + 20 / 2,122 | Low |
| A4 | `reserves` (UPR, DAC, OSLR, IBNR, takaful surplus) | No; BDOI does no reserving (Q44) | **Yes**: Reserve Summary (`REPORT_FINANCIAL`), Reserve Parameters (`MASTER_VIEW`), 7 reserve reports in the Report Centre, run approval (`PERIOD_END_RUN`), a row in the period-end checklist | Hide now (priority); remove | 74 / 7,398 + 10 / 1,505 + 16 / 1,846 | Low |
| A5 | `insurance` shared kernel (claim movement ports) | No | No | Remove with A2-A4 | 7 / 177 | Low |
| A6 | `consolidation` (inter-company, consolidation groups, consolidated statements) | No BDOI requirement | **Yes**: 5 consolidation reports in the Report Centre (`REPORT_FINANCIAL`); the screens need `CONSOLIDATION_RUN` (no BDOI role holds it) | Hide the reports now; remove after the BRD-5 report pack is confirmed | 41 / 3,890 + 2 / 425 + 9 / 1,030 | Low-medium (`closing` seed and a test use it) |
| T1 | Insurer parts of `tax`: premium tax, DST, the insurer IC schedules | No; BDOI uses BIR forms, books, 2307 and `IC-BROKER-ASBO` | **Yes**: Tax & Statutory screens with `TAX_VIEW` (FRBS, Disbursement roles) | Hide the insurer screens and reports; then remove `PremiumTaxSource` | part of `tax` (134 / 13,015) | Medium |
| D1 | Executive dashboard widgets "gross written premium" and "claims paid and outstanding" | No (insurer KPIs) | **Yes**: home screen `/` with `DASHBOARD_VIEW` | Hide the two widgets; the BRD-00 role home replaces the page | 2 widgets | Low |
| R1 | Insurer roles and SIT/UAT users (`UNDERWRITER`, `CLAIMS_OFFICER`, `RI_OFFICER`; `uw`, `claims`, `reinsurer`) | No | Roles exist in every database (V1 / V2 are not seed) | Deactivate the three roles in a new migration; keep the SIT/UAT users until the tests move | - | Low |
| P1 | `payables` documents (supplier invoices, payment vouchers, PDC issued, petty cash) | **Unsure**: BRD-5 disbursement pays through `disbursement` and `payrequest`; bank accounts and cheque books of `payables` are used | Yes (`JOURNAL_VIEW`: FRBS, ACSL, Comptrollership) | Keep the code; ask Comptrollership, then hide the document screens | - | Medium |
| P2 | `receivables` receipts, deposits and PDC received | **Unsure**: Operations re-uses the patterns, not the entity (BDOI_OPS_BRD_SPEC, platform table); cashiering issues BDOI's AR / OR | Yes (`JOURNAL_VIEW`) | Keep bank statements and reconciliation (BRD-5, FIT); hide the receipt screens after confirmation | - | Medium |

**Keep (used by BRD-5 or the platform):**
- GL: `coa`, `journal`, `ledger`, `period`, `currency`, `dimension` and the `accounting` engine;
- `subledger`, `party`, `organization`, `report`, `finreport` (schedule engine, FIN reports), `frbs` and `tax` (BIR);
- `closing` (FX revaluation, period close, broking books close, year-end);
- `budget` (budget vs actual, Mancom budgets: `BDOI_ACCT_BRD_SPEC.md` Appendix A III and V);
- `fixedasset` and `investment` (Appendix A schedules: FFE, leasehold improvements, placements and interest; spec rows
  II-IV).

## 2. Evidence that the insurer modules are out of scope

| Source | Statement |
|---|---|
| `docs/requirements/BDOI_NB_BRD_SPEC.md` (platform table and Q44) | "Reinsurance, reserves, claims reserving, actuarial: Not applicable ... Insurer-only; hide from BDOI tenant" |
| `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` (Q44, two rows) | "insurer-side `claims` hidden from BDOI roles"; "Insurer-side `reinsurance` stays hidden from BDOI roles in phase 1" |
| `docs/requirements/BDOI_CLM_BRD_SPEC.md` (platform table, BRCLM.023, BRCLM.026, Q44) | Insurer-side `claims` "Not extended"; its reserve "posts to the GL and is not usable for a broker"; PGIBR018 "is not a broker report" |
| `docs/architecture/CLAIMS_BROKING_DESIGN.md` section 2 and its module-impact table | "Do not extend the insurer-side `claims` module ... no BDOI role receives `CLAIM_VIEW`" |
| `docs/architecture/CORE_REPLACEMENT_IMPACT.md` §6 | Insurer-side `reinsurance` (treaties, cessions, FAC, `SoaDialog`) "not a broker model. Stay hidden from BDOI roles" |
| `docs/requirements/BDOI_OPS_BRD_SPEC.md` (platform table) | "Receivables (insurer model): Re-use patterns, not the entity" |
| `docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md` §3 | The seed company `FVI` keeps an insurer chart "that the underwriting, claims, reinsurance, reserves and IC seed stories need" |
| `tools/screenshots/screens.cjs` | None of the 143 current BDOI screens is an underwriting, claims, reinsurance, reserves or consolidation screen |

No BDOI module imports these packages. The only production dependency from a kept module into them is
`tax/service/PremiumTaxSource.java`, which imports `underwriting` (see T1). The other imports stay among the insurer
modules themselves.

## 3. Items

### A1. `underwriting` (insurer policy administration)

- **What.** Insurer products, quotations, policies, endorsements, marine open covers and UW reports PGIBR003-PGIBR085.
  Also the ports `PolicyClaimsView` and `PolicyReinsuranceView`, and the job `QUOTATION_EXPIRY`
  (`underwriting/service/QuotationExpiryJob.java`).
- **Not the same as the BDOI broking flow.** The broking flow is `quotation`, `account`, `placement`, `issuance`,
  `booking` and `catalog`; none of them imports `underwriting`.
- **Non-use.** BDOI_NB_BRD_SPEC treats the insurer underwriting as a pattern to re-purpose, and it was rebuilt as the
  broking modules. No screen in `screens.cjs` uses it.
- **Visibility.**
  - Menu: group "Claims & Insurance" (`frontend/src/navigation/modules.ts`), gated by `POLICY_VIEW` / `POLICY_MAINTAIN`.
    No BDOI-specific role holds these permissions (no migration after V421 grants any insurer permission).
  - The generic roles of V2 (`FIN_MANAGER`, `ACCOUNTANT`, `AUTHORIZER`, `BRANCH_FINANCE`, `AUDITOR`, `READ_ONLY`,
    `UNDERWRITER`, `CLAIMS_OFFICER`, `RI_OFFICER`) do hold them.
  - BDOI migrations extend `FIN_MANAGER`, `AUDITOR` and `FIN_ADMIN` with broking and Operations permissions (V750,
    V755, V760, V890), and several BDOI screenshots are taken as `fmanager`. A BDOI user given one of these roles
    therefore also sees the insurer menus.
  - BDOI's User Access screens list **every** `Permission` value (`nbadmin/service/AccessMatrixService.java`,
    `RolePermissionChangeValidator.java`, `security/api/UserAdminController.java`), so a BDOI administrator can request
    `POLICY_*` for a BDOI group profile.
- **Job.** `QUOTATION_EXPIRY` is scheduled daily by default (`application.yml`: `quotation-expiry-cron` defaults to
  `0 45 0 * * *`), so it runs in a BDOI production deployment against the empty insurer tables.
- **Dependants.**
  - Main code: `claims`, `reinsurance`, `reserves` and **`tax`** (`PremiumTaxSource`, used by `VatWorksheetBuilder`,
    `EwtWorksheetBuilder` and `LevyWorksheetBuilder`).
  - Tests: `claims`, `reinsurance`, `reserves`, `tax` and `api/ApiSmokeIT` (four `/api/v1/underwriting/...` URLs).
  - UI: `features/claims` and `features/reinsurance` import `features/underwriting`; `features/help/helpContent.ts`
    imports `UNDERWRITING_HELP`.
- **Migrations.**
  - Owned: `V100__underwriting.sql`, `V101__endorsement_underwriting_year.sql`.
  - Shared: the event types in `V3`.
  - Seed: `V910__seed_underwriting_roles.sql`, part of `V901`; the runner `underwriting.seed.UnderwritingSeedData`.
- **Risk.** Medium:
  - `tax` must first stop reading insurer premiums (T1);
  - the seed ledger loses the insurer policy journals, which changes the seed balances behind the finance screenshots
    (GL journals, finance dashboard, trial balance). Re-capture the screenshots after removal.
- **Action.**
  1. **Done (V1064):** the default of `BROKERVERSE_JOB_QUOTATION_EXPIRY_CRON` is `-` (off).
  2. **Done (V1064):** the insurer permissions are excluded from the User Access permission catalogue (R1).
  3. Then remove the backend package, `features/underwriting`, `api/underwriting.ts`, the menu entry, the help entry
     and the tests, together with A2-A4 and after T1.
  4. Tables: keep V100 / V101 applied. Add `V102__drop_insurer_underwriting.sql` (owner range V100-V199) that drops
     the `uw_*` tables only after A2-A4 are removed, because `claims` and `reinsurance` reference them.

### A2. `claims` (insurer-side claims)

- **What.** Insurer claims at 100 % with the company share: estimates (reserve changes posted to the GL), settlements,
  recoveries, LPOs, and the reports PGIBR002-PGIBR082, `CLM-REGISTER` and `CLM-MOVEMENT`.
- **Non-use.** BDOI claims are the new `brokerclaims` module (BRD-7), which "posts no journal"; the design decided not to
  extend `claims` (section 2 above).
- **Visibility.** Menu "Claims & Insurance > Claims / Notify Claim / LPO Register" with `CLAIM_VIEW` / `CLAIM_MAINTAIN`.
  As A1: no BDOI-specific role holds them, the shared `FIN_MANAGER` / `AUDITOR` roles do, and the User Access
  catalogue offers them (R1).
- **Dependants.** None in main code (`claims` is a leaf). In the UI it is imported only by `navigation/modules.ts` and
  `help/helpContent.ts`.
- **Name clash to keep in mind.** `brokerclaims` uses the bean name `BrokerClaimRepository` because `claims` owns
  `claimRepository` (CLAIMS_BROKING_DESIGN, build notes of CL0). Removing `claims` frees the name; do not rename the broker classes.
- **Migrations.** `V200__claims.sql` (`clm_*` tables, event type, exception codes); seed `V920__seed_claims.sql` and
  the runner `claims.seed.ClaimsSeedData`.
- **Risk.** Low.
- **Action.**
  1. Remove the backend package, `features/claims`, `api/claims.ts`, the menu entry, the help entry and the tests.
  2. Keep V200 applied; add `V201__drop_insurer_claims.sql` to drop the `clm_*` tables.
  3. Keep the accounting event type rows of V3; with no rule configured they are inert.

### A3. `reinsurance` (insurer-side treaties and cessions)

- **What.** Quota share, surplus and XOL treaties; cessions and allocation (job `RI_ALLOCATION`, cron empty by
  default); FAC placements; claims recoveries; RI statements of account; reports `RI-BAL`, `RI-BDX` and `RI-SOA`.
- **Non-use.** BDOI is a reinsurance **broker**. Its ReInsurance BRD (`docs/source-documents/ReInsurance (Phase 2).PDF`)
  is phase 2 and describes placement, SOA and remittance between cedant and reinsurer, not treaty cession accounting.
  CORE_REPLACEMENT_IMPACT §6 keeps only the seams (`party` types `REINSURER` and `RI_BROKER`, generic SOA dispatch,
  open-item netting).
- **Visibility.** Menu with `REINSURANCE_VIEW`. As A1: no BDOI-specific role holds it, the shared `FIN_MANAGER` /
  `AUDITOR` roles do, and the User Access catalogue offers it.
- **Dependants.** None in main code; its tests use `underwriting`.
- **Migrations.** `V300__reinsurance.sql` and `V301__cession_transaction_underwriting_year.sql` (`ri_*` tables, event
  types, alert codes `RI_*`); seed `V930__seed_reinsurance.sql` and the runners `ReinsuranceSeedData` and
  `ReinsuranceStatementsSeedData`.
- **Risk.** Low for the code. The phase 2 scope decides whether any layout is reused.
- **Action.**
  1. Hide now (R1).
  2. Remove at the start of phase 2 design, after checking what `SoaDialog` and `RI-SOA` layouts phase 2 wants to copy.
  3. Keep `party` types `REINSURER` and `RI_BROKER`.
  4. Add `V302__drop_insurer_reinsurance.sql` when removed.

### A4. `reserves` (actuarial reserves), **priority**

- **What.** Monthly technical reserves of a non-life insurer (UPR, DAC / UCR, OSLR, IBNR triangles, ULAE, takaful
  surplus) posted through the accounting engine. Also the job `RESERVE_VALUATION` (cron empty by default) and 7 reports.
- **Non-use.** Q44: BDOI "does no reserving and posts nothing" (BDOI_CLM_BRD_SPEC, BDOI_CROSS_BRD_DECISIONS).
- **Visibility (leaks to BDOI roles today).**
  - Menu "Claims & Insurance > Actuarial Reserves":
    - Reserve Summary is gated by `REPORT_FINANCIAL`, which V890 grants to `FRBS_PROCESSOR`, `FRBS_TL` and `FRBS_HEAD`;
    - Reserve Parameters is gated by `MASTER_VIEW`, which BDOI roles hold widely (`NB_APPROVER`, `BUSINESS_ADMIN`,
      `MBS`, `CASHIER_TL`, the FRBS roles and `DISB_APPROVER`).
  - Report Centre: `IbnrProcessingReport`, `OslrProcessingReport`, `IbnrTriangleReport`, `TakafulSurplusReport`,
    `TechnicalReservesSummaryReport`, `UprSummaryReport` and `UprMovementReport` require `REPORT_FINANCIAL`.
  - API: approving, posting and cancelling a valuation run require `PERIOD_END_RUN`, which `FRBS_TL` and `FRBS_HEAD`
    hold.
  - Period-end checklist: `reserves/service/ReserveCloseCheck.java` adds the row "Actuarial reserves valued and
    posted". It reports "Not applicable" while no parameter or run exists, so it does not block BDOI's close, but it is
    shown to FRBS.
- **Dependants.** None in main code (it implements `closing.service.PeriodEndCheckProvider`). It reads
  `underwriting`, `claims` and `reinsurance` through their ports.
- **Migrations.** `V420__actuarial_reserves.sql` (`rsv_*` tables, events, job parameter) and
  `V421__reserve_permissions.sql` (grants `RESERVE_PREPARE` to `FIN_MANAGER` and `ACCOUNTANT`); seed
  `V940__seed_reserve_accounts_and_rules.sql` and the runner `ReservesSeedData`.
- **Risk.** Low.
- **Action.**
  1. **Now:** remove `reservesModule` from `NAV_GROUPS`. Give the reserve screens and reports a dedicated permission
     (for example `RESERVE_VIEW`), held by no BDOI role, instead of `REPORT_FINANCIAL` / `MASTER_VIEW` /
     `PERIOD_END_RUN`. **Backend done (V1064):** `RESERVE_VIEW` (screens and the 7 reports) and `RESERVE_APPROVE`
     (valuation approval, posting, cancellation, parameter authorization), held by no BDOI role; the period-end
     checklist has no reserves row when reserving is not set up. The menu entry is a web client change.
  2. Then remove the package, `features/reserves`, `api/reserves.ts` and the tests.
  3. Keep V420 / V421 applied; add `V422__drop_actuarial_reserves.sql` to drop the `rsv_*` tables and the job parameter.
- **Note.** The applied migration `V420` has a comment that points to the removed guide `docs/modules/ACTUARIAL_RESERVES.md`.
  Do not edit V420 (Flyway checksum); the guide is in git history (section 5).

### A5. `insurance` shared kernel

- **What.** 7 files: `ClaimMovement`, `ClaimMovementListener`, `ClaimMovementType`, `ClaimsExperienceView`,
  `ClaimReinsuranceView`, `OutstandingClaim`.
- **Used by.** Only `claims`, `reinsurance` and `reserves`.
- **Action.** Remove together with A2-A4. It owns no migration.

### A6. `consolidation` (inter-company and group consolidation)

- **What.** Inter-company relationships and transactions, consolidation groups and runs (currency translation,
  eliminations, non-controlling interest), and 5 reports: consolidated trial balance, balance sheet and income
  statement, elimination details, inter-company reconciliation.
- **Non-use.** No BDOI BRD requires group consolidation. BDOI_ACCT_BRD_SPEC names no consolidation or inter-company
  requirement (the only "consolidated" in the spec is a consolidated payee list, DIS 2.2.8). Appendix A "loans and advances to associates" and "reciprocal deposits" are schedules built with the
  schedule engine, not consolidation.
- **Visibility.**
  - The screens "Inter-company" and "Consolidation" (reached through `features/closing/module.ts`) require
    `CONSOLIDATION_RUN`, which only `FIN_MANAGER` holds.
  - **The 5 reports require `REPORT_FINANCIAL`** and are listed for the FRBS roles.
- **Dependants.**
  - `closing/seed/PlanningSeedData.java` and `SeedSubsidiaryData.java` (seed only).
  - Tests: `api/PlanningApiIT` and `closing` tests.
  - UI: `features/closing/module.ts` imports `consolidationScreens`.
- **Migrations.** `V600__budget_intercompany_consolidation.sql`, shared with `budget`, so it must stay (`con_*` and
  `ic_*` tables). Seed: `V960__seed_group_accounts_and_subsidiary.sql` (a second seed company `FVS`) and
  `V961__seed_intercompany_group_and_average_rates.sql`.
- **Risk.** Low-medium: the seed subsidiary `FVS` may appear in company pickers and in the FX revaluation seed.
- **Action.**
  1. **Done (V1064):** the 5 reports and the read endpoints require `CONSOLIDATION_RUN`, which no BDOI role holds.
  2. Remove after FRBS confirms the Appendix A pack. Keep `budget`.
  3. Add `V601__drop_consolidation.sql` for the `con_*` / `ic_*` tables. Keep V600.

### T1. Insurer parts of `tax`

- **What.**
  - Premium-based worksheets: premium tax (`/tax/premium-tax`, `TAX-PREMTAX`) and documentary stamp tax on policies
    (`/tax/dst`, `TAX-DST-2000`), fed by `PremiumTaxSource` from `underwriting`. The VAT and EWT worksheets also read
    this source.
  - The Insurance Commission schedules of an insurer: `IC-PREM-LOB`, `IC-LOSS-LOB`, `IC-COMM-LOB`, `IC-RESERVES`,
    `IC-RBC`, `IC-NETWORTH` and `IC-INVEST`.
- **Kept for BDOI.** The BIR outputs of BRD-5 (`TAX-0619F`, `TAX-1603`, `TAX-1702Q`, `TAX-1702`, `TAX-1604E`,
  `TAX-MAP`, `TAX-SAWT`, `TAX-BOOK-*`), the 2307 register and received certificates, the broker IC report
  `IC-BROKER-ASBO`, VAT (2550Q) and EWT on the broker's own transactions.
- **Visibility.** The Tax & Statutory menu uses `TAX_VIEW`, which the FRBS and Disbursement roles hold, so the premium
  tax, DST and IC schedule screens are visible to them.
- **Risk.** Medium. The VAT and EWT worksheets must keep their other sources. Confirm with FRBS that BDOI files no
  premium tax or DST (the insurer files them).
- **Action.**
  1. **Done (V1064):** the premium-tax / LGT / FST and DST worksheets, the insurer IC schedules and their reports
     require the dedicated permission `INSURER_TAX_VIEW` (with `TAX_VIEW`), which no BDOI role holds.
  2. After confirmation, remove `PremiumTaxSource` and the premium branches of the three worksheet builders. This
     unblocks A1.
  3. Add a V704 migration only if a tax table becomes unused.

### D1. Executive dashboard insurer widgets

- **What.** `/` (Executive dashboard, `DASHBOARD_VIEW`). It shows `PremiumWidget` ("gross written premium ... against
  the prior year") and `ClaimsWidget` ("claims paid and outstanding"), read from the ledger's insurer account
  categories (`dashboard/service/LedgerDashboardService`).
- **Visibility.** V750 and V890 grant `DASHBOARD_VIEW` to many BDOI roles (for example `MKT_TL`, `PROCESSING_TL`,
  `NB_APPROVER` and the BRD-5 roles).
- **Action.** Hide the two widgets. The BRD-00 role home (CORE_REPLACEMENT_IMPACT, V1090+) replaces the page for BDOI
  roles. Low risk.

### R1. Insurer roles, permissions and SIT/UAT users

- **Roles.** `sec_role` rows `UNDERWRITER`, `CLAIMS_OFFICER` and `RI_OFFICER`, and their grants, come from `V1` / `V2`
  (not seed), so they exist in every environment, including BDOI production. The generic finance roles `FIN_ADMIN`,
  `FIN_MANAGER`, `ACCOUNTANT`, `AUTHORIZER`, `BRANCH_FINANCE`, `AUDITOR` and `READ_ONLY` also carry `POLICY_VIEW`,
  `CLAIM_VIEW` and `REINSURANCE_VIEW`.
- **Permissions.** `Permission.java` values `POLICY_VIEW`, `POLICY_MAINTAIN`, `POLICY_AUTHORIZE`, `CLAIM_VIEW`,
  `CLAIM_MAINTAIN`, `CLAIM_AUTHORIZE`, `REINSURANCE_VIEW`, `REINSURANCE_MAINTAIN`, `REINSURANCE_AUTHORIZE`,
  `RESERVE_PREPARE` and `CONSOLIDATION_RUN`. The User Access (BRD-11) matrix, the group-profile report and the
  role-permission change requests list every value.
- **SIT/UAT users.** `uw`, `claims` and `reinsurer`, plus `fmanager`, `accountant`, `checker` and `auditor` for the insurer
  stories. The integration tests use them through `AsUser` (Developer Guide section 8). Several BDOI screenshots are
  also taken as `fmanager`.
- **Action.**
  1. **Done:** `V1064__hide_insurer_roles_and_permissions.sql` sets the three insurer roles inactive and removes the
     insurer permissions from every other role, and the permission catalogue excludes them (section 4.1). The
     seed profile keeps the insurer stories through the seed-only roles `SIT_INS_*` (seed V1961).
  2. Keep the SIT/UAT users until the tests of A1-A6 are removed.

### P1 / P2. Overlaps to confirm with BDOI (keep for now)

- **`payables`.**
  - Disbursement (BRD-5) uses its bank accounts, cheque books and notification formats (`BankAccountQueryService`,
    V502), so the package stays.
  - Unsure: the supplier invoice, payment voucher, PDC issued and petty cash screens (`/payables/*`, `JOURNAL_VIEW`)
    duplicate the BDOI path Request for Payment (`payrequest`) → DV (`disbursement`).
  - Ask Comptrollership (AQ / OQ02 follow-up) whether AP supplier invoices stay in BIBS. If not, hide the screens.
- **`receivables`.**
  - Bank statement import and bank reconciliation are FIT for BRD-5 (FRBS 3.3.x; V551), so they stay.
  - Unsure: official receipts, deposits and PDC received (`/receivables/receipts`, `/deposits`, `/pdcs`,
    `JOURNAL_VIEW`) duplicate cashiering (BRD-2 CSHID.001-002).
  - Hide them from BDOI roles once Operations confirms.

## 4. Proposed order of work

| Step | Change | Size | Risk |
|---|---|---|---|
| 1 | Hide: remove `reservesModule`, `underwritingModule`, `claimsModule` and `reinsuranceModule` from `NAV_GROUPS` (the "Claims & Insurance" group keeps `brokerClaimsModule`); dedicated permissions for the reserve, consolidation, premium-tax / DST / insurer IC reports; drop the D1 widgets; empty the `QUOTATION_EXPIRY` cron; exclude the insurer permissions from the User Access catalogue (R1) | S (1-2 days) | Low |
| 2 | Tax: remove `PremiumTaxSource` and the premium branches (T1) | S | Medium |
| 3 | Remove `reserves`, `claims`, `insurance` (A4, A2, A5) with their UI, API clients, help, tests and seed runners; drop migrations V201 and V422 | M (2-3 days) | Low |
| 4 | Remove `underwriting` (A1) and its seed runner; drop migration V102; re-capture the screenshots | M | Medium |
| 5 | Remove `reinsurance` (A3) at phase 2 design; drop migration V302 | M | Low |
| 6 | Remove `consolidation` (A6) after FRBS confirms the report pack; drop migration V601 | S | Low-medium |
| 7 | Decide P1 / P2 with Comptrollership and Operations | - | - |

### 4.1 Step 1 as built (backend)

| Area | What changed |
|---|---|
| Permissions | `Permission.isInsurerOnly()` flags `POLICY_VIEW`, `POLICY_MAINTAIN`, `POLICY_AUTHORIZE`, `CLAIM_VIEW`, `CLAIM_MAINTAIN`, `CLAIM_AUTHORIZE`, `REINSURANCE_VIEW`, `REINSURANCE_MAINTAIN`, `REINSURANCE_AUTHORIZE`, `RESERVE_PREPARE`, `CONSOLIDATION_RUN` and the new `RESERVE_VIEW`, `RESERVE_APPROVE`, `INSURER_TAX_VIEW` |
| Grants (V1064) | The insurer-only permissions are withdrawn from every role (`FIN_ADMIN`, `FIN_MANAGER`, `ACCOUNTANT`, `AUTHORIZER`, `BRANCH_FINANCE`, `AUDITOR`, `READ_ONLY` and every BDOI role, FRBS and Disbursement included), one access change log row per role |
| Roles (V1064) | `UNDERWRITER`, `CLAIMS_OFFICER`, `RI_OFFICER` inactive (they keep their permissions and members but grant nothing) |
| User Access screens | `/admin/permissions`, the access matrix (both views and the export), role-permission and group-profile requests, the group profile report and the role lists (`/admin/roles`, `/nbadmin/roles`) exclude the insurer-only permissions and the roles holding one; a direct role edit refuses them (`PERMISSION_NOT_OFFERED`) |
| Reserves | Screens and 7 reports need `RESERVE_VIEW`; approval, posting, cancellation and parameter authorization need `RESERVE_APPROVE`; the approval inbox uses `RESERVE_APPROVE`; the period-end checklist shows no reserves row while reserving is not set up |
| Consolidation | 5 reports and the read endpoints need `CONSOLIDATION_RUN` |
| Tax | Premium tax / LGT / FST and DST worksheets, the IC schedules endpoint and the reports `TAX-PREMTAX`, `TAX-DST-2000`, `IC-PREM-LOB`, `IC-LOSS-LOB`, `IC-COMM-LOB`, `IC-RESERVES`, `IC-RBC`, `IC-NETWORTH`, `IC-INVEST` need `INSURER_TAX_VIEW` |
| Jobs | `QUOTATION_EXPIRY` off by default (`-`), like `RESERVE_VALUATION` and `RI_ALLOCATION` |
| Seed (V1961) | Seed-only roles `SIT_INS_<role>` give the SIT/UAT users the insurer access they had before, so the insurer seed runners and tests keep running until the removal waves |
| Test | `nbadmin/service/InsurerSuiteHiddenIT` |

Still open in step 1: the web client (menu entries of `NAV_GROUPS` and the D1 widgets).

In total about 42,700 lines of main code (A1-A6), 7,600 of tests and 11,300 of UI, plus the API clients `underwriting.ts`,
`claims.ts`, `reinsurance.ts`, `reserves.ts` and `consolidation.ts` (about 1,350 lines).

**Rules for every step:**
- Never edit or delete an applied Flyway migration (`V100`, `V101`, `V200`, `V300`, `V301`, `V420`, `V421`, `V600`, and
  the seed `V901`-`V961`).
- Add new migrations in the owner's free range; the Developer Guide migration table allows a lower version than one
  already applied. A seed migration can be dropped only if every seed database is rebuilt from scratch, as the
  screenshot procedure does.
- Update `ArchitectureTest`, `ApiSmokeIT`, `docs/architecture/ARCHITECTURE.md`, `docs/operations/CONFIGURATION.md`
  and `RUNBOOK.md` (the `reserve-valuation-cron`, `ri-allocation-cron` and `quotation-expiry-cron` settings), and
  `features/help/helpContent.ts` in the same change.

## 5. Document clean-up done with this audit

| Removed | Category | Replaced by / reason |
|---|---|---|
| `New Business (NB) BRD.pdf` (repository root) | Superseded (duplicate) | Byte-identical copy of `docs/source-documents/New Business (NB) BRD.pdf` |
| `docs/requirements/iNXT_BrokerVerse_BDOI_BRD1_New_Business_Fit_Gap.xlsx` | Superseded | Pre-build concurrence workbook; replaced by FRS BRD-01 v1.0, the discrepancy register v1.1 and `BDOI_NB_TRACEABILITY.md` |
| `docs/requirements/REPORTS_BOOK_SPEC.md` | Not relevant | Insurer GI reports book (Annexure 2(c)), not in the BDOI pack; implemented only by the insurer modules above |
| `docs/modules/UNDERWRITING.md`, `CLAIMS.md`, `REINSURANCE.md`, `ACTUARIAL_RESERVES.md` | Not relevant | Guides of the insurer modules A1-A4 |

The removed files are in git history. Read them with `git show 61b1df8:<path>` (for example
`git show 61b1df8:docs/modules/CLAIMS.md`) when the code of A1-A4 is changed or removed.

Kept on purpose, with notes:
- `docs/architecture/ARCHITECTURE.md`: platform architecture; insurer modules marked.
- `docs/requirements/GL_FUNCTIONAL_SPEC.md` and `FINANCE_REPORTS_SPEC.md`: baselines of the GL and FIN reports that
  BRD-5 uses.
- `docs/development/PLANNING_AND_CLOSING.md`: budgets and closing are used; its consolidation part goes with A6.
- `docs/modules/PAYABLES_AND_CASH.md`, `RECEIVABLES_AND_BANKING.md` and `TAX_AND_STATUTORY.md`: see P1, P2 and T1.
