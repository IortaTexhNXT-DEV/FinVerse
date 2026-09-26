# BDOI Core Replacement (umbrella BRD) - Impact on BIBS

Status: **analysis; nothing in this document is built.** Requirements baseline and fit/gap:
[`BDOI_CORE_BRD_SPEC.md`](../requirements/BDOI_CORE_BRD_SPEC.md) (rows CORE-nn.mm, XC-nn, questions CRQnn). Client
document: `docs/deliverables/src/frs/FRS_BRD00_CORE_REPLACEMENT.md` (FR-CR-nnn).

## 1. Summary

The umbrella BRD (`00 - BRD BDOI Core Replacement v01.pdf`, 48 pages) restates the function BRDs as 21 capabilities
and 209 BR rows and defers every acceptance criterion to them. Measured against the code as built:

| Result | Rows |
|---|---|
| Capability rows already met by the built BRDs (BRD-1 to BRD-5, BRD-10, BRD-11) | 135 of 192 FIT |
| Rows met by BRDs that are designed or being built (Renewal, Claims, Employee Benefits, CSF) | 39 (NEW / CHANGE in those builds) |
| Reinsurance | 10 rows: phase 2, BRD received |
| Data-management, report and cross-cutting rows that need work (CHANGE, NEW or CONFIGURE) | 14, section 4 |

The umbrella therefore changes **little in the business modules** and adds a **platform layer**: role dashboards,
report customisation and scheduling, a master-data change log, MIS field definition, insurer management in one place,
the Invoice Master List, and batch delivery of invoices to insurers. It also raises NFR values (response under 5 s
for every role; 429 concurrent users; 5 / 15-year retention) that the performance and security work must use.

## 2. Impact by dimension

| Dimension | Impact |
|---|---|
| Build | Seven work items without an owning BRD (section 7, waves CR-W1 to CR-W5, Flyway V1090-V1096, demo V1990-V1991). Two parked items belong to other ranges: claims cheque custody (Claims V1026-V1029, CRQ03) and CSF cases (CSF V1043-V1049, CRQ04) |
| Design | New sections for `dashboard` (role home), `report` (layouts, subscriptions), `audit` (field-level change log), `catalog` (insurer page, MIS field catalogue), `booking` (insurer invoice batch), `opsledger` (Invoice Master List view). No change to the business workflows of BRD-1 to BRD-5 |
| Code | Additive: new tables, services and screens; one Hibernate listener in `audit` (pattern of `integration/service/EntityChangeCapture.java`); report runner extensions in `frontend/src/features/reports`. No existing contract changes |
| Screens | New: Role Home, Master Data Change Log, MIS Fields, Insurer page (tabs), Report Subscriptions, Report Layout panel, Insurer Invoice Batches. Changed: report runner (columns, grouping, chart, schedule button), Invoice Ledger (Invoice Master List view) |
| Navigation | Home group: Role Home becomes the landing page (`/`) for business roles; the finance Dashboard stays for Finance roles. Setup & Administration > Broking Setup: MIS Fields, Master Data Change Log. Reports: My Subscriptions. Client & Policy > Catalog: Insurers opens the Insurer page |
| Personas | No new persona. Every persona gets a role home (section 4.1). The System Administrator maintains MIS fields and subscriptions of others; the Business Administrator views the change log; Finance and Operations maintain the insurer tabs they own |
| NFR | Response under 5 s for every role (p.42) and 429 concurrent sessions (p.42) go into the performance plan (deliverable 28); retention 5 / 15 years applies to `audit_log` and the new change log |

## 3. What the umbrella confirms (no change)

- Client onboarding, quotation, non-package placement, account, placement and booking (capabilities 1-5): BRD-1 as
  built. Capability 4 "Non-Package Management" is the NB non-package flow (`nonpackage`), subject to CRQ02.
- Marketing Collection, Cashiering, Remittance, Production Reconciliation, Adjustment, Commission Receivables
  (capabilities 7, 9-13): BRD-4 and BRD-2 as built.
- Accounting, GL, Disbursement, ACSL (capability 8): BRD-5 as built, including BIR books (`tax/report/BookOfAccountsReport.java`).
- Product Maintenance (capability 20): BRD-3 as built; the integration of BRPM.022 stays a parked seam (PQ16).
- LOV maintenance, sign-in, user types, password reset, audit trail, notifications (XC-01, 03, 04; CORE-17.03).
- Renewal, Claims, Employee Benefits, CSF: their designs cover the bullets; build order unchanged
  (`BDOI_CROSS_BRD_DECISIONS.md` §6.2), except the two conflicts in section 5.

## 4. Cross-cutting capabilities to build or confirm

### 4.1 Role home dashboards (XC-02; BR-125, 152, 160, 203; p.5) - build

Today: `dashboard/service/DashboardService.java` serves finance widgets (premium, claims, collections, payables,
cash, budget, workload) to every holder of `DASHBOARD_VIEW`; `nbreport/service/NbDashboardService.java` serves the
NB dashboard; about a dozen section homes exist (`OperationsHomePage`, `CashieringHomePage`, `RemittanceHomePage`,
`ProdReconHomePage`, `AdjustmentHomePage`, `CommissionHomePage`, `CollectionsHomePage`, `AcslHomePage`,
`FrbsHomePage`, `DisbursementHomePage`, `ProductMaintenanceHomePage`, `ScreeningHomePage`).

Design:
- A `RoleHomeWidget` port in `dashboard`; each module contributes widgets from counts it already computes (for
  example the section-home count endpoints), so `dashboard` does not depend on the modules (same pattern as
  `nbreport` reading through constant SQL, BROKING_ARCHITECTURE §16).
- Table `dsh_role_home` (role, widget code, position, parameters) maintained by the System Administrator; a user with
  several roles sees the union, de-duplicated.
- Each widget links to the filtered work list or report it summarises ("view details invoked from the dashboard",
  BR-152, 160).
- Default widget sets per persona (Marketing AO / TL, TSU, Processing, Cashiering, Remittance, Collections, FRBS,
  Disbursement, ACSL, Claims, CSF, Compliance, System Administrator) until BDOI answers CRQ06.

### 4.2 Report generation (capability 21) - confirm 21.01-21.03, build 21.04-21.05

Confirmed: 174 report definitions on `report/core/ReportDefinition`; export XLSX, PDF, CSV, ODS, XML and DOCX
(`report/render/ExportFormat`, `DocxReportRenderer`); print with metadata; saved variants for every report
(`nbreport/domain/ReportVariant`, `frontend/src/features/reports/ReportVariants.tsx`); report batches
(`report/core/ReportBatchService`).

Build:
- **Layout in the variant** (CORE-21.04): visible columns and order, group-by column, subtotal and count per group,
  one chart (bar or line) over a grouped measure. Stored as JSON next to the parameters of `nbr_report_variant`;
  applied by `ReportService` after `TabularReportBuilder` (no change to report definitions). Exports honour the
  layout; the chart goes to PDF and XLSX only.
- **Subscriptions** (CORE-21.05): table `rpt_subscription` (owner, report code, variant, schedule, format, recipients,
  protection, active, next run). A job `REPORT_SUBSCRIPTIONS` (`system/service/JobScheduler`) runs due subscriptions
  with the owner's permissions, archives the file (`ReportArchiveService`) and e-mails it through `messaging`
  (password-protected when the report is confidential). Failed runs notify the owner and the job-failure recipients.

### 4.3 Master data change logging (CORE-17.01; BR-170) - build

Today: `audit/domain/AuditLog` stores a summary text per action; LOV changes go through approval with effectivity
(`lov`), catalog records through `catalog/service/CatalogApprovalSource.java`, access changes through `nbadmin`
requests (`UserAccessHistory` report); screening keeps its own `ConfigDiff`.

Design: table `mdc_change` (entity type, key, field, old value, new value, changed by, changed at, approval
reference). A Hibernate listener in `audit` records updates of registered master entities (registration list
`mdc_entity`: users and roles, products and versions, insurers and branches, commission rates, LOVs, chart of
accounts, payees, parameters). Report `ADM-MASTER-CHANGES` (category CONTROL) with filters by entity, user and date;
screen Setup & Administration > Master Data Change Log. Retention: 5 years online, 15 years archive (p.45).

### 4.4 MIS field definition (CORE-17.04; BR-173, 174) - build

Design: catalogue `mis_field` (code, business label, entity, source column or expression, LOV type, data type,
active). Seeded with the fields of BR-173 (product type, risk code, market segment, booking date, policy number,
insurer, branch, account officer, status, transaction type, chart of account) and the Report List MIS columns
(region, area, unit head, department, business origin). Used by report parameters and the layout panel (4.2) and by
dashboard widgets. Phase 1 does not let users add database fields (CRQ09).

### 4.5 Insurer management (CORE-17.02; BR-171) - change

Today the insurer is spread over `catalog/domain/InsurerProfile` (name, accreditation number and expiry, placement
channel and e-mails, default credit days), `InsurerBranch` (LGT), `CommissionRate`, `party` (INSURER sub-ledger),
remittance rules (`remittance/domain/EarlyIncentiveRule`, extraction by insurer), GL control accounts and product
versions (`VersionInsurer`, `VersionInsurerTerm`).

Design: one Insurer page in Catalog with tabs Profile, Branches and LGT, Contacts (new `cat_insurer_contact`),
Products and packages (read from versions), Commission rates, Payment terms (credit days, remittance schedule per
remittance type: new `cat_insurer_remittance`), Risk participation (default co-insurance shares, if CRQ08 confirms),
Accounts (GL and sub-ledger links, read-only). Changes use the existing catalog maker-checker and are logged by 4.3.

### 4.6 Notifications (XC-03) - confirm

`messaging/service/NotificationService`, `NotificationPreferenceService`, `MailDispatchJob`, `alert` checks and
workflow notifications cover BR-083, 111 and 206. The notification catalogue per role is produced for deliverable 15;
no build.

### 4.7 Audit (XC-04) - confirm

`audit/service/AuditTrailService` is called on every change; `/admin/audit` and `AuditTrailReport` view and export it.
Apply the umbrella retention (5 / 15 years) to `audit_log` through the retention rules of `nbadmin` (BRNB.106).

### 4.8 Invoice Master List (XC-13; BR-175) - change

`opsledger` InvoiceLedgerQueryService already lists invoices with reference, invoice number, client code, assured,
product line, dates, insurer, status and cancellation. Add cover number, version number, Marketing and Processing
assignees, placement status and approval dates (read from `account`, `workflow` and `placement` through the existing
ports), and a saved view "Invoice Master List". Confirm with the Operations owner (CRQ13).

### 4.9 Insurer invoice batch and SFTP delivery (XC-19; p.43) - change

Today each service invoice is e-mailed to the insurer (`booking/service/ServiceInvoiceDispatch.java`). Design: a daily
job `INSURER_INVOICE_BATCH` that builds, per insurer, one batch (merged PDF or ZIP with a manifest) of the day's
service invoices, stores it (`ins_invoice_batch`) and sends it through a port `InsurerFileOutbox` (default adapter:
in-system repository, like `opsledger/service/port/FileDropPort.java`; SFTP adapter parked until CRQ18), with a
delivery report per insurer.

### 4.10 Other confirmations

- SOA at booking (XC-10, CRQ12): no build until BDOI names the SOA.
- Brand and UI (XC-14): the screen-alignment pass (deliverable 18) against `BDOI_UXD.docx` and BDO_UX_GUIDELINES;
  icons wait for CRQ17.
- Workflow at the back end (XC-05): workflow definitions are data (`workflow`); CONFIGURE only.

## 5. Conflicts that change a build

| # | Item | Proposal until answered |
|---|---|---|
| CF-01 / CRQ04 | CSF case resolution (BR-165) vs BRD-9 out of scope | Keep out of phase 1; if BDOI confirms it, add a CSF case entity in the CSF range after CSF S1 |
| CF-02 / CRQ03 | Claims cheque safekeeping and hand-over (BR-146-148) vs BRD-7 CLQ10 | Keep out; if yes, a cheque custody register in `brokerclaims` (V1026-V1029) using the cashiering cheque hand-over; no GL entry unless BDOI receives the money |
| CF-03, CF-04 / CRQ21 | Response under 5 s for every role; 429 concurrent | Test at 429 concurrent sessions and p95 under 2 s (register proposal); report per BRD |
| CF-05 / CRQ22 | Retention 5 / 15 and backup every 4 hours kept 5 years | Default rule for all record types; BRD-7 keeps 10 years online |

## 6. Reinsurance: phase-2 readiness

The ReInsurance BRD (32 pages, `ReInsurance (Phase 2).PDF`) is received and is **phase 2**; it is not built in phase
1 and no build option is chosen here. A full analysis is a separate task. From its scope pages (sections 2-5): BDOI RI
receives placement requests from BDOI TSU, sends placement slips to reinsurers and RI brokers, books the accepted
slip, issues SOAs, collects and remits RI premium (with net settlement), and handles RI claims (PLA, RCRF, FLA,
cash calls, debit notes, claims proceeds to the client). Users 95 / 41 (umbrella p.42).

Phase 1 must not preclude:

| Seam | Phase 1 today | Keep |
|---|---|---|
| Parties | `party` PartyType has REINSURER and RI_BROKER (from the finance suite) | Do not repurpose these types for insurers; the RI module adds cedant as a party role |
| Placement request from TSU (FRID-001-010) | TSU work in `nonpackage` and `productmaint`; placement slips in `placement` | Keep slip generation template-driven (`docgen`); do not hard-wire the insurer as the only slip recipient |
| Placement slip revisions, sign-off and hand-over to Operations (FRID-017-032) | `placement`, `workflow`, `booking` | Booking events carry the party role, so an RI booking can post receivable from cedant and payable to reinsurer |
| SOA generation, templates, schedule and e-mail (FRID-033-041) | `collections` BillingStatementService (client SOA); `report` subscriptions (4.2) | Build the subscription and SOA dispatch generically so RI SOAs reuse them |
| RI premium collection, application, remittance schedule, proof of remittance, net settlement (FRID-039-052) | `cashiering`, `remittance`, `subledger/service/OpenItemService.match` | Keep remittance schedule and proof-of-remittance templates configurable; netting through open-item matching |
| RI claims: PLA, RCRF, FLA, cash call, debit note, claims proceeds (FRID-058-105) | `brokerclaims` records claims and loss advice (`LossAdviceService`) | Keep the claim party model open to reinsurers; claim money flow decided by CRQ03 and CLQ10 |
| Accounting entries (FRID-031, 042, 066) | `accounting` event rules by event type | New RI event types only; no change to existing posting rules |
| Insurer-side `reinsurance` module (treaties, cessions, FAC, `SoaDialog`) | Built for the insurer suite; not a broker model | Stay hidden from BDOI roles; reuse only its SOA statement layout ideas |

Dependencies seen in the ReInsurance BRD: BDOI TSU (request initiation), Comptrollership (remittance to cedant,
FRID-039), Operations (booking, FRID-028-030), Claims handlers (RCRF, PLA), e-mail and notifications, report
templates; retention 10 years online and 15 years archive for infrastructure logs (NFR 25.21).

## 7. Dependencies on the Data Migration BRD

| Umbrella item | Data Migration BRD | Effect on BIBS |
|---|---|---|
| Client migration "from Broker and source, 2020 to present" (p.43) | BRID 2.1 client master deduped per agreed keys | Load through `crm` bulk CLIENT_CREATE with the duplicate check; period to confirm (CRQ19) |
| Daily midday and EOD client batches, client modification report (p.43) | Coexistence (capability 12) | A client feed from the legacy source during coexistence, into `crm` (like the CSF `ContactSyncGateway` in reverse); only if coexistence is chosen |
| MIS, LOV, insurer, product, risk codes (capability 17; MILB p.45) | BRID 3.1 governed mapping of reference data | MIS field catalogue (4.4) and insurer page (4.5) are the targets of the mapping; load before cutover |
| Marketing Collection, Cashiering, Remittance, Adjustment on legacy invoices (capabilities 7, 9, 10, 12) | BRID 5.1-9.3 (UPP, OTC, autopay, DPPR / PR2307 reversals, remittance extract, endorsements on legacy invoices) | The built modules must accept legacy invoice references with legacy sub-ledgers; owned by the Data Migration design |
| Renewal (capability 6) | Renewal-driven transition, RMEL run-off (BRID 4.1, 12.1) | Renewal reads legacy in-force headers (`LegacyPolicySource` port) |
| Claims (capability 14) | - | V1025 is held for the claims legacy migration (CLQ14) |
| History and audit (XC-04) | BRID 11.1 legacy read-only | No history migrated into `audit_log` or `mdc_change` |
| Reconciliation reports | BRID 1.1 reconcile per data object | Migration reports belong to the Data Migration build |

Hosting appendix: migration staging data is purged within 5 days and non-production data is masked.

## 8. Proposed build waves for the gaps

Flyway range V1090-V1099 (Core Replacement platform items); demo V1990-V1999. Each wave touches shared files, so it
runs as a foundation wave in the order of `BDOI_CROSS_BRD_DECISIONS.md` §6.1 P5 (one at a time, `Permission.java`
first).

| Wave | Scope | Flyway | Modules and shared files | Depends on |
|---|---|---|---|---|
| CR-W0 | Permissions (`ROLE_HOME_ADMIN`, `REPORT_SUBSCRIBE`, `REPORT_SUBSCRIBE_ADMIN`, `MDC_VIEW`, `MIS_FIELD_MAINTAIN`, `INSURER_BATCH_VIEW`), parameters, job registrations | V1090 | `security/domain/Permission.java`, `application.yml`, `docs/operations/CONFIGURATION.md` | - |
| CR-W1 | Master data change log (4.3) and MIS field catalogue (4.4) | V1091, V1092 | `audit`, `catalog`; report `ADM-MASTER-CHANGES`; screens in Broking Setup | CR-W0; answers CRQ07, CRQ09 (defaults until then) |
| CR-W2 | Report layout in variants and report subscriptions (4.2) | V1093 | `report`, `nbreport` (variant table), `messaging`; `frontend/src/features/reports` | CR-W0, CR-W1 (MIS fields in the layout panel) |
| CR-W3 | Role home dashboards (4.1) | V1094; demo V1990 (widget sets of the demo roles) | `dashboard`; widget contributors in each module; `navigation/modules.ts` | CR-W0; CRQ06 for final widget sets |
| CR-W4 | Insurer page (4.5) | V1095 | `catalog`, `remittance` (read), `party` | CR-W1 (change log); CRQ08 |
| CR-W5 | Invoice Master List view (4.8) and insurer invoice batch (4.9) | V1096; demo V1991 | `opsledger`, `booking` | CRQ13, CRQ18 (SFTP adapter parked) |
| - | Reserved: V1097-V1099 for answers to CRQ06-CRQ13 | V1097-V1099 | - | - |

Not in these waves: claims cheque custody (Claims range, CRQ03), CSF cases (CSF range, CRQ04), client feeds (Data
Migration), Reinsurance (phase 2).

## 9. Edits needed to shared documents (not made here)

Applied on 26 September 2026 in the consolidation with the Data Migration analysis, except the help entries in
`helpContent.ts`, which come with the waves.

| Document | Edit |
|---|---|
| `docs/development/DEVELOPER_GUIDE.md` §4 | Add the range "Core Replacement platform items V1090-V1099 (demo V1990-V1999)" to the V1000-V1899 and V1900-V1999 rows; §11 add this document to the list of designs |
| `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` | Add the umbrella to the BRD table; §5 add conflicts CF-01 (CSF cases) and CF-02 (claims cheques) as XQ12 and XQ13; §3 record the partial answers to XQ08, Q39, Q40, PQ19 |
| `docs/requirements/BDOI_CSF_BRD_SPEC.md` §10 | Reference CRQ04 next to CSF-EM10 |
| `docs/requirements/BDOI_CLM_BRD_SPEC.md` §11 | Reference CRQ03 next to CLQ10 |
| `docs/requirements/BDOI_RN_BRD_SPEC.md` | Note the letter name conflict NRL / NFR (CRQ14) |
| `docs/deliverables/src/frs/FRS_BRD07_CLAIMS.md` | Renumber FR-CL-nnn to a unique prefix (for example FR-CM-nnn); 28 IDs collide with BRD-4 |
| `docs/deliverables/src/registers/discrepancy_register.yaml` | Add BRD-00 (Core Replacement) to `brds` and `owners`; add the new items (DCR-163 onward); add a BRD-00 value to the NFR themes (users, response time, retention, backup frequency and retention); add CRQ questions |
| `docs/deliverables/src/registers/build_discrepancy_register.py` | Add `("BRD-00", "BDOI_CORE_BRD_SPEC.md", "## 13. New open questions (Core Replacement)", "")` to `QUESTION_SOURCES` |
| `docs/deliverables/README.md` | Item 1: 15 FRS files (BRD-00 umbrella added); item 28: test at 429 concurrent sessions; item 29: link the umbrella's client-migration volumes |
| `docs/architecture/BROKING_ARCHITECTURE.md` | Navigation: Role Home as the landing page; Broking Setup entries MIS Fields and Master Data Change Log |
| `frontend/src/features/help/helpContent.ts` | Help entries for the new screens (with the waves) |
