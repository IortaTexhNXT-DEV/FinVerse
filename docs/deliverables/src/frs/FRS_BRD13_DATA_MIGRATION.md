---
# Source of the Functional Requirements Specification for BRD-13 Data Migration.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD13_DATA_MIGRATION.md
title: Data Migration
subtitle: BRD BDOI Core Modernization - Data Migration (draft v0.01, 14-Apr-2026)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-13
name: Data Migration
doc_id: BIBS-FRS-BRD-13
version: "1.2"
date: 26 September 2026
status: Issued for BDOI review
header_title: FRS BRD-13 Data Migration
output: FRS/BIBS_FRS_BRD-13_Data_Migration_v1.2.docx
control:
  - version: "0.9"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-13 baseline and the build design
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue for BDOI review, on the draft BRD v0.01; to be re-issued when the BRD is signed
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "Calendar of the BDOI timeline (go-live January 2028); early-renewal concept paper recorded as superseded by the single January 2028 go-live; new FR-DM-034 (package remapping) and FR-DM-124 (renewals of the January-May 2028 expiries); trial-migration load order in FR-DM-120; layouts and file-name rule in FR-DM-010; test-plan findings in FR-DM-013, 021, 051, 061, 091, 110 and 123; DMQ25 and DMQ26 partly answered, DMQ36-DMQ39 added"
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "BDOI answers of 26-Sep-2026 - DMQ36 packages remapped at Renewal sanitation (FR-DM-034, 040); DMQ37 January-May 2028 expiries processed in BIBS after go-live (FR-DM-122, 124); DMQ38 RA-sent file with maker-checker review (new FR-DM-125); DMQ39 option A recommended, provisional GL opening and FY2027 true-ups (new FR-DM-022 to 024; FR-DM-120, 121, 123)"
distribution:
  - {name: "Program Manager, Business Project Services", role: "BRD owner and approver", organisation: BDO Unibank ESG, purpose: Review and sign-off}
  - {name: "Head, Comptrollership; Product Owners FRBS / ACSL and Disbursement", role: Approver, organisation: BDOI, purpose: "Review of the carried-forward balances, legacy sub-ledgers and GL"}
  - {name: "Head, Operations; Operations - Financial Transactions and Processing", role: Approver, organisation: BDOI, purpose: "Review of the legacy invoice flows (payments, UPP, reversals, remittance, endorsements, Prod Recon)"}
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: "Review of the client master and reference data"}
  - {name: "Heads of Retail and Corporate Marketing", role: Approver, organisation: BDOI, purpose: "Review of the client master and the renewal-driven transition"}
  - {name: "Unit Head, Analytics and Risk Management; Unit Head, Claims, RI and TU", role: Approver, organisation: BDOI, purpose: "Review of history, archive and inquiry"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test, rehearsals and cutover"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Data Migration business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the migration and the processing of legacy items in BIBS behave as the business expects. The project team uses it to build the Data Migration module and the changes to the Operations modules, to test them, and to prepare the rehearsals and the cutover. Every functional requirement (FR) cites the BRD requirements it meets and their BRD pages.

Data Migration is **designed and not yet built**. The FRs describe the behaviour of the build design (R3). Screen names and API paths are those of the design and are confirmed at build. Error codes are assigned at build; this document gives the message text only, except where a check reuses a code that already exists in BIBS (section 1.5).

The source BRD is a **draft (v0.01, 14-Apr-2026) that is not signed** (R1 p.15-16). This FRS will be re-issued when the BRD is signed or amended.

## Scope

BDOI replaces its legacy platforms (EBIX and QPS in the BRD; ISYS and Excel masterlists in other BRDs) with BIBS. The BRD asks for a **selective, continuity-focused** migration (p.3): clean master and reference data are migrated for Day 1, only open operational financial positions are carried forward, history stays in read-only legacy or an archive, and in-force policies move to BIBS at renewal.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Governance | Data object register, decision gates, sign-off gates, reconciliation of every object | BRID 1.1a, 1.1b |
| Master and reference data | Client Master with dedupe; LOV, insurer, product, package, risk code and MIS code maps with versions | BRID 2.1, 3.1 |
| Policies | Minimal in-force policy header (conditional); renewal-driven transition by RMEL cohort | BRID 4.1, 12.1 |
| Open items | Legacy invoices with open balances; legacy UPP; opening entries and legacy sub-ledgers | BRID 5.1-8.1 |
| Legacy invoice processing | OTC and autopay payments, UPP automatch rerun, dispositions, reclassification to income, DPPR and PR2307 batch reversals, remittance, endorsements, Prod Recon change report | BRID 5.1-10.1 |
| History | Read-only legacy and archive inquiry with access logging | BRID 11.1 |
| Cutover | Cutover plan and rehearsals on the BDOI timeline (go-live January 2028) at the year-end boundary, go / no-go, provisional GL opening and FY2027 true-ups, go-live renewal extraction of the January-May 2028 expiries with the renewal advices already sent, PACKAGE map for Renewal sanitation, run-off, decommissioning | BRID 1.1b, 3.1, 12.1 |

**Out of scope of this FRS until BDOI decides (DMQ30):** open claims, Employee Benefits programmes, the Excel submitted-policy masterlists, payees and users. Where BDOI brings them into scope, they are loaded through the same framework with the loaders that their own modules provide (R3 section 10).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | BDO Insure Core Modernization - Data Migration BRD | draft v0.01, 14-Apr-2026; not signed | `docs/source-documents/BRD - Data Migration - draft V0.01.pdf` |
| R2 | BDOI Data Migration (BRD-13) requirements baseline and fit/gap | current | `docs/requirements/BDOI_DM_BRD_SPEC.md` |
| R3 | Data Migration build design | current | `docs/architecture/DATA_MIGRATION_DESIGN.md` |
| R4 | BRD BDOI Core Replacement (umbrella BRD) | v01 | `docs/source-documents/00 - BRD BDOI Core Replacement v01.pdf` |
| R5 | Operations build design and FRS BRD-2 (cashiering, remittance, Prod Recon, adjustment, commission) | current | `docs/architecture/OPERATIONS_DESIGN.md`; FRS BRD-2 |
| R6 | Collections, Accounting / ACSL, Renewal and Customer Servicing designs | current | `docs/architecture/` |
| R7 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R8 | Deliverables plan (hosting appendix: masked non-production data, staging purge within 5 days, access from the Philippines only) | current | `docs/deliverables/README.md` |
| R9 | BDOI drop plan and programme timeline | received 26-Sep-2026 | `docs/source-documents/BDOI_DROP_PLAN.md` |
| R10 | Concept Paper - Advance Implementation of Renewal Processing (early renewal release) | V1.0, signed 06-Sep-2026; superseded by the single January 2028 go-live (BDOI, 26-Sep-2026) | `docs/source-documents/Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` |
| R11 | BDOI answers to DMQ36-DMQ39 (package remapping, January-May 2028 renewals, RMEL and disposition sources, year-end cut-over) | 26-Sep-2026; DMQ39 awaits Comptrollership confirmation | R2 section 10.2 |

Page references ("p.8") are pages of the Data Migration BRD (R1) unless another document is named.

## Definitions and acronyms

```glossary
AR: Acknowledgment Receipt, issued for premium collected on behalf of an insurer
Archive: Read-only store of legacy history in BIBS (BRID 11.1)
Autopay: Automated premium payments received in payment files (bills payment, trade, CLPC, direct credit) and matched by BIBS
Carry forward: Treatment of an open operational financial item that continues to be processed in BIBS (BRID 1.1a)
Clearing account: Migration Clearing GL account that takes the offset of every opening entry and must net to zero
Code map: Versioned table that maps a legacy code of a source system to a BIBS code (BRID 3.1)
Cohort: The legacy policies expiring in one month (RMEL expiry month)
Control totals: Row count, amount sums and hash totals that BDOI sends with an extract
Cutover: The move from legacy to BIBS processing at go-live
DPPR: Direct-payment premium receivable; premium receivable of an invoice paid by the client directly to the insurer (reading to be confirmed, DMQ19)
DTIP: Due to Insurer - Premium
EBIX: Legacy booking and accounting system
Extract: A file of one data object from one source system, with its control file
FR: Functional requirement of this document (FR-DM-nnn)
FY2027 adjustment register: Comptrollership's list of the legacy GL journals posted after the freeze for the FY2027 close and audit (FR-DM-024)
Freeze: The time from which legacy data may no longer change before the final extracts
Go / no-go: The decision to go live, taken on measured criteria
Go-live renewal extraction: One Renewal extraction at go-live of every migrated policy header expiring from go-live to 31 May 2028 (FR-DM-124)
ISYS: Legacy reporting and marketing diary system
Legacy invoice: An invoice booked in EBIX or QPS before go-live, still open at cutover, carried into BIBS and processed there
Legacy sub-ledger: The GL control accounts and ledger items that hold the legacy positions separately from the new ones
LOV: List of values
Opening period: The first GL period of BIBS, January 2028, into which the opening entries and the true-ups post (value date 1 January 2028)
MIS: Management information fields (market segment, department, unit head, AO, region, area, branch, business origin, customer segment)
Trial migration: A rehearsal of the migration on a test environment with masked data
Layout: The field list of one extract file; an object has one layout or several (R04 and R04B, P01 and P01S, F01, F01S and F01C)
OTC: Over-the-counter payment at a cashier
Package remapping: Mapping of a legacy package and version to a BIBS package version, done by the Renewal sanitation check PACKAGE_REMAP with the PACKAGE code map (FR-DM-034)
PR: Premium receivable
Provisional opening trial balance: The GL opening loaded at cut-over from the preliminary December 2027 trial balance, balance-sheet accounts only (FR-DM-022)
PR2307: Premium receivable covered by the client's BIR Form 2307 (2% creditable withholding tax)
Prod Recon: Production reconciliation with insurers
QPS: Quotation and Pre-processing System (legacy)
RA-sent file: Reference file of the renewal advices already sent by hand before go-live, loaded so that BIBS does not send them again (layout P03, FR-DM-125)
RMEL: Renewal Master Expiry List
Run-off: The period in which legacy policies expire and renew into BIBS
Sign-off gate: A recorded approval that a migration step is complete (G1-G7)
True-up: An opening-balance adjustment journal that brings a FY2027 closing or audit adjustment of the legacy GL into the BIBS opening balances (FR-DM-023)
Staging: Temporary area where extract rows are checked and mapped before load
TSU: Technical Support Unit; maintains the BIBS products and packages (BRD-3)
UPP: Unapplied premium payment
Xref: Key cross-reference from a legacy key to the BIBS record created from it
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R2), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained in BIBS (parameter, list of values, code map or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a check whose code is assigned at build, or a screen check without a code. Codes that already exist in BIBS are quoted (for example `MAKER_CHECKER_VIOLATION`, `ACCESS_DENIED`, `DISPOSITION_AMOUNT`).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the migration test plan.

The BRD numbers two requirements "BRID 1.1" (p.7). This FRS calls the first one (decision gates) **BRID 1.1a** and the second one (reconciliation) **BRID 1.1b**.

> [!NOTE]
> Values marked "default" (thresholds, retention days, chunk sizes, timings) are placeholders that BDOI confirms through the open questions of section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R2)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today in BIBS |
| CONFIGURE | Needs set-up only (parameters, lists, rules) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-13 |

# Business context and process overview

## Business context

Legacy data is spread over several systems, with duplicated clients, inconsistent identifiers and manual controls around payment application, UPP, remittance, reversals and endorsements (p.4). The target is a trusted Client Master, governed reference mappings, open positions carried forward and actionable in BIBS, legacy systems kept read-only for inquiry and audit during run-off, and renewals that progressively recreate clean records in BIBS (p.5).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.4-5)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Client and operational data are kept in several legacy systems, with duplicates, inconsistent identifiers and fragmented ownership | One Client Master: legacy clients are matched and deduplicated on agreed keys before load, and every legacy key points to one BIBS client |
| 2 | Codes differ between systems (LOV, insurers, products, packages, risk codes, MIS) | Governed code maps with versions and sign-off; unmapped codes are reported before any transaction loads |
| 3 | Payment application, UPP, remittance, reversals and endorsements run on legacy invoice structures with manual controls | Legacy invoices and UPP are carried into the BIBS Operations ledger and processed with the BIBS screens, with legacy sub-ledgers in the GL |
| 4 | Users verify data and postings by hand | Every migrated object is reconciled by counts, amounts, hash totals, field by field and to the GL; the reports are the evidence of the sign-off |
| 5 | Legacy systems hold the history | History stays in read-only legacy or the BIBS archive, searchable with logged access |
| 6 | Renewals are processed in legacy | From cutover, renewals are processed in BIBS by RMEL cohort; legacy policies run off |

## Migration lifecycle

Every data object goes through the same lifecycle (Figure 1). The decision gate (G1) sets its class. Only objects classed Migrate or Carry-forward are loaded. Each load is reconciled and signed off before the objects that depend on it can load.

![Lifecycle of a data object, from decision to acceptance (BRID 1.1a, 1.1b, 3.1)](figures/brd13_migration_lifecycle.dot)

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Lifecycle steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Data object and decision | Data Migration Lead; data owner approves | The object is registered with the four criteria and classed Migrate / Carry-forward / Archive / Excluded (gate G1) | BRID 1.1a |
| 2 | Layout and code maps | Data Steward; data owner approves | The file layout is frozen and the code maps are approved (gate G2) | BRID 3.1 |
| 3 | Extract intake | Migration Operator | The file and its control file are received; checksum, layout, row count and control totals are checked; personal data is masked outside production | BRID 1.1b |
| 4 | Staging and validation | BIBS job; Data Steward | Rows are mapped and validated; unmapped codes, rule failures and client duplicates are listed and resolved (gate G3) | BRID 1.1b, 2.1, 3.1 |
| 5 | Load | Data Migration Lead approves (gate G4); BIBS job | Rows are loaded through the BIBS services; each record gets a key cross-reference | BRID 1.1b |
| 6 | Reconciliation | Reconciliation approver | Counts, amounts, hash totals, fields and GL are reconciled; breaks are explained (gate G5) | BRID 1.1b |
| 7 | Acceptance | Data owner and Data Migration Lead | Business verification on screen; the object is accepted (gate G6) | BRID 1.1a, 1.1b |

## Legacy invoices after cutover

A legacy invoice is not re-booked. It enters the BIBS Operations ledger with its open balances and is then processed like any BIBS invoice, with one difference: its postings go to the **legacy sub-ledgers** (Figure 2).

![A legacy invoice and the flows that process it after cutover (BRID 5.1-10.1)](figures/brd13_legacy_invoice_flows.dot)

<!-- table: widths=4,7.6,5 caption="Legacy sub-ledgers (proposed; accounts confirmed by Comptrollership, DMQ18)" -->
| Legacy sub-ledger | Holds | Moved by |
|---|---|---|
| Premium Receivable - Legacy (by component) | Open PR of legacy invoices | Payments, automatch, dispositions, DPPR reversals, write-offs, endorsements |
| PR 2307 - Legacy | Open PR2307 of legacy invoices | PR2307 reversals |
| Commission Receivable - Legacy | Open commission and VAT on commission from insurers | Remittance, DP commission collection, endorsements |
| Due to Insurers (DTIP) - Legacy | Open premium payable to insurers | Remittance, DPPR and PR2307 reversals, endorsements |
| Unapplied Collections (UPP) - Legacy | Legacy UPP | Automatch, dispositions, refunds, reclassification to income |
| Migration Clearing | Offset of the opening entries and of the GL trial balance lines of the legacy control accounts | Opening entries only; must be 0.00 |

## Cutover and transition

The cutover follows the BDOI programme timeline (R9, Figure 3): requirements and mapping in September-October 2026, build from November 2026 to March 2027, Trial migrations 1 and 2 in the SIT migration window (April and July 2027), Trial migrations 3 and 4 in the UAT migration window (August and October 2027), the dress rehearsal in November 2027, and the production cutover over the year end, with go-live in January 2028 (DMQ25). In every trial migration, reference data and clients load first. Reference data and clients are pre-loaded in production two weeks before go-live and kept current with daily client deltas. Open items, UPP, policy headers, the renewal advices already sent and the provisional GL opening are loaded once, after the legacy freeze, over the cutover weekend.

**Year-end cut-over (DMQ39, recommendation awaiting Comptrollership confirmation).** Go-live is on Monday 3 January 2028, at the year-end boundary (option A). Legacy processes to 31 December 2027 and closes FY2027. BIBS opens with the open items at 31 December and a provisional GL opening trial balance taken from the preliminary December trial balance: balance-sheet accounts only, with the FY2027 result in retained earnings, so that the FY2028 P&L starts at zero (FR-DM-022). After the freeze the legacy GL stays open only for the FY2027 closing and audit adjustments, restricted to Comptrollership, with no new business. Each adjustment reaches BIBS as a controlled opening-balance adjustment journal in the opening period: the first true-up after the legacy year-end close, about mid to late January 2028, and the final one after the audited financial statements, about March-April 2028 (FR-DM-023, FR-DM-024). Comptrollership completes a December soft close by about 20 December 2027. The FY2027 BIR annual returns and the FY2027 audit come from legacy; FY2028 comes from BIBS. Two other options were considered and are not recommended: go-live after the first-quarter close in April 2028 with a migration of the year-to-date P&L (option B), and two sets of books in parallel for the first quarter of 2028 (option C). If BDOI wants more assurance in January, the key January reports are compared with the opening position instead of keeping two books (R3 section 17.7).

The concept paper of 6 September 2026 on an early renewal release (R10) is superseded by the single January 2028 go-live. Of its points, only the trial-migration load order still applies: the reference data and clients load first in every trial migration (FR-DM-120). BDOI answered on 26 September 2026 that the renewals of the January-May 2028 expiries are processed in BIBS after go-live (DMQ37) and that legacy packages are remapped at Renewal sanitation (DMQ36).

![Migration calendar on the BDOI timeline (BRID 12.1; go-live January 2028)](figures/brd13_cutover_timeline.dot)

The transition to BIBS follows the renewal expiry month (RMEL cohort):

<!-- table: widths=4.6,12 caption="Transition by expiry month (DMQ37 answered)" -->
| Expiry | Treatment |
|---|---|
| Before go-live (up to 2 January 2028) | Renewed or lapsed in legacy; a renewal booked in legacy before the freeze migrates as an in-force header; only open items are carried forward |
| From go-live to 31 May 2028 | Processed in BIBS after go-live. No renewal is carried from legacy. At go-live BIBS extracts every migrated policy header expiring in this window, prioritised by expiry date, with the January expiries flagged urgent; the renewal advices already sent by hand before go-live are recorded and not sent again (FR-DM-124, FR-DM-125) |
| From 1 June 2028 | Extracted by BIBS Renewal from the migrated policy headers on the normal lead time of 140 days (1 June 2028 is extracted on 13 January 2028) |

# Personas and roles

## Personas

<!-- table: widths=3.2,4.6,6.8,2.6 caption="Personas and BIBS roles" size=8.5 -->
| Persona | BIBS role | Responsibilities in Data Migration | BRD |
|---|---|---|---|
| Data Migration Lead | DATA_MIGRATION_LEAD | Maintains the data object register, submits decisions, approves loads, accepts objects, runs the cutover plan | BRID 1.1a, 1.1b, 12.1 |
| Data Steward | DATA_STEWARD | Maintains code maps, layouts and rules; resolves data-quality issues; decides doubtful client matches | BRID 2.1, 3.1 |
| Data owner (business owner of an object) | DATA_OWNER | Approves decisions, code map versions and waivers; accepts the object | BRID 1.1a, 3.1 |
| Migration Operator | MIGRATION_OPERATOR | Receives extracts, runs validation and loads | BRID 1.1b |
| Reconciliation approver | MIGRATION_RECON_APPROVER | Signs the reconciliation; approves break explanations and rollbacks (Comptrollership for financial objects) | BRID 1.1b |
| Go / no-go board | MIGRATION_GONOGO | Decides go-live on the measured criteria | BRID 12.1 |
| Program Manager | MIGRATION_GONOGO, MIG_VIEW | Owns the cutover approach and the transition model | BRID 12.1 |
| Marketing User | Existing Marketing roles | Finds migrated clients and uses them in transactions | BRID 2.1 |
| Operations User | Existing Operations roles (ADJUSTMENT, ADJUSTMENT_TL) | Finds migrated policies; processes endorsements on legacy invoices | BRID 4.1, 9.1-9.3 |
| Cashiering User | CASHIER, CASHIER_TL | Processes payments, automatch, dispositions and PR2307 reversals on legacy items | BRID 5.1-5.4, 6.1-6.4, 7.2 |
| Finance Approver / top management | TOP_MANAGEMENT_APPROVER | Approves the reclassification of UPP to other income | BRID 5.5 |
| Accounting User | COMMREC_TL, CASHIER_TL, COMPTROLLERSHIP | Requests and approves DPPR and PR2307 legacy batch reversals | BRID 7.1, 7.2 |
| Remittance User | REMIT_PROCESSOR, REMIT_TL | Extracts and processes remittances that include legacy invoices | BRID 8.1 |
| Prod Recon Analyst | RECON_HANDLER | Runs the legacy invoice change report | BRID 10.1 |
| Audit / Compliance User | LEGACY_INQUIRY, LEGACY_ACCESS_REVIEWER | Searches the legacy archive; reviews the access log | BRID 11.1 |
| Renewal processing team (maker and checker) | Renewal roles, plus MIG_DQ_RESOLVE (maker) or MIG_RESUBMIT_APPROVE (checker) limited to object P03 | Compiles the renewal advices already sent from the Excel trackers, corrects rejected rows (maker) and approves resubmissions (checker); works the day-1 priority queue and the Exception bucket in Renewal | BRID 12.1 |
| Comptrollership GL lead | COMPTROLLERSHIP | Prepares each FY2027 true-up | BRID 1.1b |
| Head, Comptrollership | MIGRATION_RECON_APPROVER | Signs the provisional opening trial balance; approves, reconciles and signs each true-up | BRID 1.1b |

The BRD does not define the roles of the migration or who "top management" is (DMQ16). The roles above are the project's proposal until BDOI confirms them.

<!-- pagebreak -->

## Permissions

<!-- table: widths=5.2,11.4 caption="Data Migration permissions" -->
| Permission | Allows |
|---|---|
| MIG_VIEW | Open the Migration Console (read only) |
| MIG_OBJECT_MANAGE | Maintain data objects; submit decisions |
| MIG_DECISION_APPROVE | Approve a data object decision (G1) |
| MIG_MAPPING_EDIT | Maintain code maps, layouts, rules and masking rules |
| MIG_MAPPING_APPROVE | Approve a code map version (G2) |
| MIG_INTAKE | Upload extracts and run the intake checks |
| MIG_DQ_RESOLVE | Resolve data-quality issues |
| MIG_DQ_WAIVE | Waive rows (G3) |
| MIG_MATCH_DECIDE | Decide doubtful client matches |
| MIG_LOAD_RUN | Run validation and loads |
| MIG_LOAD_APPROVE | Approve a load (G4) |
| MIG_ROLLBACK_REQUEST, MIG_ROLLBACK_APPROVE | Request and approve a batch rollback |
| MIG_RECON_SIGNOFF | Sign a reconciliation and approve break explanations (G5) |
| MIG_SIGNOFF | Accept an object (G6) |
| MIG_CUTOVER_MANAGE | Maintain cutover plans and tasks |
| MIG_GONOGO_DECIDE | Record the go / no-go decision (G7) |
| MIG_TRUEUP_PREPARE | Prepare a FY2027 true-up |
| MIG_TRUEUP_APPROVE | Approve and sign a FY2027 true-up |
| MIG_RESUBMIT_APPROVE | Approve a resubmission of corrected rejected rows (checker) |
| LEGACY_INQUIRY_VIEW, LEGACY_INQUIRY_EXPORT | Search the legacy archive; export results |
| LEGACY_ACCESS_LOG_VIEW | View the legacy access log |
| CASH_UPP_INCOME_REQUEST, CASH_UPP_INCOME_APPROVE | Prepare and approve (top management) the reclassification of UPP to other income |
| LEGACY_REVERSAL_REQUEST, LEGACY_REVERSAL_APPROVE | Request and approve DPPR and PR2307 legacy batch reversals |

## Permissions matrix

<!-- table: widths=4.8,1.3,1.3,1.3,1.3,1.3,1.3,1.3,1.3,1.3,1.3 caption="Role-to-permission matrix (proposal). Stew. = Data Steward; Oper. = Migration Operator; Go = go / no-go board; Inq. = Legacy inquiry; Log = access reviewer; Top = top management; Cash TL = Cashiering TL" size=8 -->
| Permission | DM Lead | Stew. | Owner | Oper. | Recon | Go | Inq. | Log | Top | Cash TL |
|---|---|---|---|---|---|---|---|---|---|---|
| MIG_VIEW | Y | Y | Y | Y | Y | Y | | | | |
| MIG_OBJECT_MANAGE | Y | | | | | | | | | |
| MIG_DECISION_APPROVE | | | Y | | | | | | | |
| MIG_MAPPING_EDIT | | Y | | | | | | | | |
| MIG_MAPPING_APPROVE | | | Y | | | | | | | |
| MIG_INTAKE | | | | Y | | | | | | |
| MIG_DQ_RESOLVE | | Y | | | | | | | | |
| MIG_DQ_WAIVE | | | Y | | | | | | | |
| MIG_MATCH_DECIDE | | Y | | | | | | | | |
| MIG_LOAD_RUN | | | | Y | | | | | | |
| MIG_LOAD_APPROVE | Y | | | | | | | | | |
| MIG_ROLLBACK_REQUEST | Y | | | | | | | | | |
| MIG_ROLLBACK_APPROVE | | | | | Y | | | | | |
| MIG_RECON_SIGNOFF | | | | | Y | | | | | |
| MIG_SIGNOFF | Y | | Y | | | | | | | |
| MIG_CUTOVER_MANAGE | Y | | | | | | | | | |
| MIG_GONOGO_DECIDE | | | | | | Y | | | | |
| MIG_TRUEUP_APPROVE | | | | | Y | | | | | |
| LEGACY_INQUIRY_VIEW | | | | | | | Y | | | |
| LEGACY_INQUIRY_EXPORT | | | | | | | Y | | | |
| LEGACY_ACCESS_LOG_VIEW | | | | | | | | Y | | |
| CASH_UPP_INCOME_APPROVE | | | | | | | | | Y | |
| LEGACY_REVERSAL_APPROVE | | | | | | | | | | Y |

Cashiers also get CASH_UPP_INCOME_REQUEST and LEGACY_REVERSAL_REQUEST; the Commission TL gets LEGACY_REVERSAL_APPROVE for DPPR batches. The COMPTROLLERSHIP role gets MIG_VIEW and MIG_TRUEUP_PREPARE. The Renewal processing team's makers get MIG_VIEW and MIG_DQ_RESOLVE, and its checkers MIG_VIEW and MIG_RESUBMIT_APPROVE, both limited to object P03.

Segregation of duties is enforced by the system, whatever the role grants: the maker of a decision, code map version, load, rollback, reclassification batch, reversal batch, true-up or resubmission never approves it; the operator who ran a batch cannot sign its reconciliation or acceptance; one person cannot sign two gates of the same batch.

# Functional requirements

## Governance and decision gates

```fr
id: FR-DM-001
title: Maintain the data object register
brd: [BRID 1.1a (p.7)]
actor: Data Migration Lead
priority: Must have
fit: NEW
screens: Data Objects
api: GET/POST /api/v1/migration/objects; PUT /api/v1/migration/objects/{code}
description:
  - BIBS keeps one register of every data object that could be migrated, with its source systems, target in BIBS, business owner, data steward, dependencies and load order. The register starts from the catalogue of the design (R3 section 2), for example clients, policy headers, open legacy invoices, UPP, GL trial balance and history.
  - For each object the Data Migration Lead records the four criteria of the BRD, Day-1 need, compliance need, read-only / archival option and data trust, and proposes a class.
preconditions:
  - The user has MIG_OBJECT_MANAGE.
main_flow:
  - The Data Migration Lead opens Data Objects and chooses New Object or an existing object.
  - The lead enters or updates the object fields and the four criteria.
  - The lead chooses the proposed class (Migrate, Carry-forward, Archive, Excluded or Conditional) and writes the rationale.
  - BIBS saves the object in status PROPOSED and shows it in the register with its dependencies.
alternate_flows:
  - Object with dependencies. BIBS lists the objects it depends on and refuses a load order lower than theirs.
  - Conditional object (for example policy headers, BRID 4.1). The lead enters the condition; the object cannot be loaded until the decision (FR-DM-002) states the condition as met.
rules:
  - [R1, "Object codes are unique; an object is never deleted, only set to Excluded.", Fixed, "-"]
  - [R2, "Classes are Migrate, Carry-forward (open items), Archive, Excluded and Conditional.", Fixed, "-"]
  - [R3, "Data trust is HIGH, MEDIUM or LOW; Day-1 need, compliance need and read-only / archival option are Yes / No with a note.", Configurable, "List MIG_TRUST_LEVEL"]
  - [R4, "An object loads only after every object it depends on is accepted in the same environment (gate G6).", Fixed, "-"]
validations:
  - [Code already used, This object code already exists, "-"]
  - [Load order lower than a dependency, The load order must follow the objects this object depends on, "-"]
  - [No rationale, Enter the rationale for the proposed class, "-"]
fields_screen: Data Object
fields:
  - [Object code, Text, "Yes", "-", "Unique; letters and digits"]
  - [Name, Text, "Yes", "-", "Up to 120 characters"]
  - [Category, List, "Yes", "LOV MIG_OBJECT_CATEGORY", "REFERENCE, CLIENT, POLICY, OPEN_ITEM, GL, HISTORY"]
  - [Source systems, Multi-select, "Yes", "LOV MIG_SOURCE_SYSTEM", "EBIX, QPS, ISYS, EXCEL, CMS"]
  - [Target in BIBS, Text, "Yes", "-", "Module and record"]
  - [Business owner, User, "Yes", "Users with DATA_OWNER", "-"]
  - [Data steward, User, "Yes", "Users with DATA_STEWARD", "-"]
  - [Day-1 need, Yes / No + note, "Yes", "-", "-"]
  - [Compliance need, Yes / No + note, "Yes", "-", "-"]
  - [Read-only / archival option, Yes / No + note, "Yes", "-", "-"]
  - [Data trust, List, "Yes", "HIGH, MEDIUM, LOW", "-"]
  - [Proposed class, List, "Yes", "Migrate, Carry-forward, Archive, Excluded, Conditional", "-"]
  - [Condition, Text, "Cond.", "-", "Mandatory for Conditional"]
  - [Depends on, Multi-select, "No", "Objects", "No cycle"]
  - [Load order, Number, "Yes", "-", "At least the load order of each dependency plus one"]
  - [Rationale, Text, "Yes", "-", "Up to 2,000 characters"]
notifications:
  - "None."
audit:
  - Creation and every change of an object are recorded with old and new values.
acceptance:
  - A new object is saved as PROPOSED with its four criteria and shown in the register.
  - An object cannot be given a load order before an object it depends on.
  - An Excluded object stays in the register and cannot be loaded.
```

```fr
id: FR-DM-002
title: Decide the migration class of a data object
brd: [BRID 1.1a (p.7)]
actor: Data Migration Lead (maker); business owner (approver)
priority: Must have
fit: NEW
screens: Data Objects (Decision tab); My Approvals
api: POST /api/v1/migration/objects/{code}/decision; .../decision/approve; .../decision/return
description:
  - The class of an object is decided through a recorded approval (gate G1). The Data Migration Lead submits the proposal; the business owner of the object approves or returns it. The approved class is the one BIBS enforces.
  - This meets the BRD acceptance criterion that each data object is "actioned upon" as Migrate / Carry-Forward (Open Items) / Archive / Excluded once the business owners have assigned the four criteria.
preconditions:
  - The object is PROPOSED or its decision was returned.
main_flow:
  - The Data Migration Lead opens the object and clicks **Submit Decision**.
  - BIBS moves the object to FOR_DECISION and puts it in the business owner's My Approvals.
  - The business owner reviews the criteria and the rationale and clicks **Approve**.
  - BIBS sets the object to DECIDED with the class and records the decision with date and approver.
alternate_flows:
  - Return. The business owner returns the decision with a reason; the object goes back to PROPOSED.
  - Change of class later. A new decision is submitted and approved; the history keeps every decision. A change from Migrate or Carry-forward to Archive or Excluded is refused while a batch of the object is loaded and not rolled back.
rules:
  - [R1, "The approver is the business owner of the object and never the submitter.", Fixed, "-"]
  - [R2, "Only DECIDED objects of class Migrate or Carry-forward (or Conditional with the condition met) can be loaded.", Fixed, "-"]
  - [R3, "Archive objects are loaded only into the legacy archive (FR-DM-110).", Fixed, "-"]
validations:
  - [Approver is the submitter, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
  - [Class change with loaded data, Roll back the loaded batches before changing the class of this object, "-"]
  - [Return without reason, Enter the reason for the return, "-"]
notifications:
  - On submission, the business owner is notified in My Approvals and by e-mail.
  - On approval or return, the Data Migration Lead is notified.
audit:
  - Every decision is kept with class, criteria, rationale, submitter, approver and dates (report MIG-DECISIONS).
acceptance:
  - A decision approved by the business owner sets the object to DECIDED with the approved class.
  - The submitter cannot approve their own decision.
  - An object classed Excluded cannot be selected for a batch.
  - The report MIG-DECISIONS lists every decision with its approver.
```

```fr
id: FR-DM-003
title: Sign off each object through gates
brd: [BRID 1.1a (p.7), BRID 1.1b (p.7)]
actor: Data owner, Data Steward, Data Migration Lead, reconciliation approver
priority: Must have
fit: NEW
screens: Sign-off; Batches
api: POST /api/v1/migration/batches/{no}/signoff/{gate}
description:
  - Every object passes seven gates, each recorded with the role, user, decision, comment, time and evidence. The gates are G1 Decision, G2 Mapping, G3 Validation, G4 Load approval, G5 Reconciliation, G6 Object accepted and G7 Go-live.
  - The Sign-off screen shows the gates per object and environment as a matrix, so the Data Migration Lead sees what is missing before a trial migration or the cutover.
preconditions:
  - The previous gate of the object or batch is signed.
main_flow:
  - The signer opens the batch or object and the gate to sign.
  - BIBS shows the evidence of the gate (map versions, issue counts, reconciliation summary, sample checks).
  - The signer approves or rejects with a comment and optional evidence file.
  - BIBS records the sign-off and updates the matrix.
alternate_flows:
  - Rejected gate. The object returns to the step of the gate; the reason is shown to the Data Migration Lead.
rules:
  - [R1, "G1 and G2 by the data owner; G3 by the Data Steward (waivers by the data owner); G4 by the Data Migration Lead; G5 by the reconciliation approver; G6 by the data owner and the Data Migration Lead; G7 by the go / no-go board.", Configurable, "Role-permission matrix"]
  - [R2, "The operator who ran a batch cannot sign its G5 or G6; one person cannot sign two gates of the same batch.", Fixed, "-"]
  - [R3, "G5 cannot be signed while a reconciliation break is not explained and approved.", Fixed, "-"]
validations:
  - [Previous gate not signed, Sign the previous gate first, "-"]
  - [Signer ran the batch or signed another gate of it, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
  - [Open break at G5, Explain and approve every break before signing the reconciliation, "-"]
notifications:
  - When a gate is ready to sign, the users of the signing role are notified.
audit:
  - Every sign-off is recorded and cannot be changed; a correction is a new sign-off.
acceptance:
  - An object reaches G6 only after G1-G5 are signed in order.
  - The operator of a batch cannot sign its reconciliation.
  - The Sign-off screen shows, per object and environment, which gates are signed, by whom and when.
```

## Extract intake, code maps, validation and load

```fr
id: FR-DM-010
title: Receive a source extract with control totals
brd: [BRID 1.1b (p.7)]
actor: Migration Operator
priority: Must have
fit: NEW
screens: Extracts
api: POST /api/v1/migration/extracts (multipart - data file and control file)
description:
  - BDOI sends one file per layout and source system with a control file. Most objects have one layout; R04 (insurers and insurer branches R04B), P01 (headers and insurer shares P01S) and F01 (invoice headers, insurer shares F01S and components F01C) have sub-layouts sent as separate files with the same as-of date. BIBS checks each file before any row is staged, so that what BIBS loads is exactly what BDOI extracted.
  - The file contract (names, CSV or XLSX, date and amount formats, control file content) is published to BDOI in the data requirements workbook, which BIBS generates from the layouts.
preconditions:
  - The object is DECIDED (Migrate or Carry-forward) and has an approved layout version.
  - The user has MIG_INTAKE.
main_flow:
  - The operator opens Extracts, chooses the object, the layout and the source system and uploads the data file and the control file.
  - BIBS checks the SHA-256 of the file against the control file, the header against the layout, the parsed row count against the control count, the amount sums per currency and the hash total of the key column.
  - When every check passes, BIBS gives the extract a number (MGX-yyyy-nnnnnn), masks personal data when the environment is not production, and stages the rows.
  - BIBS shows the extract as STAGED with its counts and control totals.
alternate_flows:
  - A check fails. The extract is REJECTED with the failed check and the difference; nothing is staged; the Data Migration Lead is notified.
  - Same file sent twice. BIBS refuses it as a duplicate.
  - Delta extract with an as-of date earlier than the last one of the object. BIBS refuses it.
rules:
  - [R1, "File names are <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv or .xlsx, where LAYOUT is the layout code (for example F01C), not the object code; the control file adds .ctl.csv.", Fixed, "-"]
  - [R5, "A batch of an object takes one checked extract of each of its layouts with the same as-of date.", Fixed, "-"]
  - [R2, "CSV is UTF-8 with comma separator and one header row; dates are yyyy-MM-dd; amounts use a dot decimal and 2 decimals without thousands separator.", Fixed, "-"]
  - [R3, "Outside production, names, addresses, TIN, ID, account and phone numbers, e-mail addresses and birth dates are masked before staging; the same value is always masked the same way.", Configurable, "Masking rules; parameter MIG_ENVIRONMENT_CLASS"]
  - [R4, "Files are received through the console or the SFTP drop and are never e-mailed.", Fixed, "-"]
validations:
  - [Checksum differs, The file does not match the checksum in the control file, "-"]
  - [Header differs from the layout, "The file columns do not match layout version {n}: missing {columns}, extra {columns}", "-"]
  - [Row count differs, "The file has {n} rows; the control file says {m}", "-"]
  - [Amount total differs, "The total of {column} in {currency} is {x}; the control file says {y}", "-"]
  - [Hash total differs, The hash total of the key column does not match the control file, "-"]
  - [Duplicate file, "This file was already received as extract {no}", "-"]
  - [As-of date out of order, The as-of date is earlier than the last extract of this object, "-"]
fields_screen: Extract upload
fields:
  - [Object, List, "Yes", "Decided objects", "Migrate or Carry-forward"]
  - [Layout, List, "Yes", "Layouts of the object", "Matches the file name and the header row"]
  - [Source system, List, "Yes", "Source systems of the object", "-"]
  - [Mode, List, "Yes", "FULL, DELTA", "-"]
  - [Data file, File, "Yes", "-", "CSV or XLSX"]
  - [Control file, File, "Yes", "-", "CSV in the control layout"]
notifications:
  - A rejected extract notifies the Data Migration Lead and the Migration Operator (alert MIG_EXTRACT_REJECTED).
audit:
  - Every upload is recorded with file names, checksum, user and result.
acceptance:
  - A file whose rows, amounts and hash total agree with its control file is staged and numbered.
  - A file with one row fewer than the control count is rejected and nothing is staged.
  - In a non-production environment the staged client names and TINs are masked; in production they are not.
```

```fr
id: FR-DM-011
title: Maintain and approve versioned code maps
brd: [BRID 3.1 (p.7)]
actor: Data Steward (maker); data owner (approver)
priority: Must have
fit: NEW
screens: Code Maps
api: GET/POST /api/v1/migration/maps; .../maps/{set}/versions; .../versions/{v}/submit; .../approve; .../export; .../import
description:
  - A code map set exists per domain, for example each LOV type, insurers, insurer branches, products and risk codes, packages, lines, branches, sales units, account officers, each MIS field, GL accounts and the status lists of each object. An entry maps a legacy code of a source system to a BIBS code, or says DEFAULT, REJECT or CREATE.
  - A set has versions. Only the approved version is used by a batch, and the batch records which version it used, so every loaded row can be traced to the mapping that produced it.
preconditions:
  - The user has MIG_MAPPING_EDIT to edit, MIG_MAPPING_APPROVE to approve.
main_flow:
  - The Data Steward opens Code Maps, chooses a set and creates a DRAFT version (empty, copied from the approved version, or imported from Excel).
  - The steward edits entries and submits the version.
  - The data owner compares the version with the approved one (added, changed and removed entries) and approves it.
  - BIBS marks it APPROVED and the previous version SUPERSEDED.
alternate_flows:
  - Return. The owner returns the version with a reason; it goes back to DRAFT.
  - CREATE entries. On approval, BIBS lists the reference values to create; the Data Steward creates them through the reference-data load (FR-DM-030), where the normal maker-checker of each master applies.
rules:
  - [R1, "One approved version per set; a batch uses the approved version current at validation.", Fixed, "-"]
  - [R2, "The approver is never the user who submitted the version.", Fixed, "-"]
  - [R3, "An entry action is MAP (to a target code), DEFAULT (to the set default), REJECT (the row fails) or CREATE (a new BIBS value).", Fixed, "-"]
  - [R4, "The target code of a MAP entry must exist and be active in BIBS when the version is approved.", Fixed, "-"]
validations:
  - [Target code does not exist, "Target code {code} does not exist in {domain}", "-"]
  - [Same legacy code twice, "Legacy code {code} of {source} is mapped twice", "-"]
  - [Approver is the submitter, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
fields_screen: Code map entry
fields:
  - [Source system, List, "Yes", "LOV MIG_SOURCE_SYSTEM", "-"]
  - [Legacy code, Text, "Yes", "-", "Unique per source in the version"]
  - [Legacy description, Text, "No", "-", "-"]
  - [Action, List, "Yes", "MAP, DEFAULT, REJECT, CREATE", "-"]
  - [Target code, List, "Cond.", "Values of the target domain", "Mandatory for MAP"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - Submitted versions go to the data owner's My Approvals.
audit:
  - Versions and entries keep their history; report MIG-MAP-VERSIONS lists versions, approvers and the batches that used them.
acceptance:
  - An approved version supersedes the previous one and is used by the next validated batch.
  - A version mapping to a BIBS code that does not exist cannot be approved.
  - Each batch shows the map versions it used.
```

```fr
id: FR-DM-012
title: Report unmapped and invalid codes before load
brd: [BRID 3.1 (p.7)]
actor: Data Steward
priority: Must have
fit: NEW
screens: Code Maps (Unmapped tab); Batches
api: GET /api/v1/migration/maps/unmapped; report MIG-UNMAPPED-CODES
description:
  - When staged rows are validated, every coded column is looked up in its code map. Codes without an entry, and entries whose target is no longer active, are reported with the number of rows and sample keys. A batch with unmapped codes in a mandatory column cannot be approved for load, so "all required codes exist" before any transaction loads.
preconditions:
  - At least one extract of the object is staged.
main_flow:
  - The Data Steward opens the Unmapped tab or runs MIG-UNMAPPED-CODES.
  - BIBS lists set, source system, legacy code, rows and sample legacy keys.
  - The steward adds the entries in a new map version and submits it (FR-DM-011).
  - After approval the batch is validated again and the codes disappear from the report.
rules:
  - [R1, "An unmapped code in a mandatory coded column is an ERROR; in an optional column a WARNING.", Configurable, "Rule catalogue"]
validations:
  - [Load approval with unmapped codes, "The batch has {n} unmapped codes; map them before approving the load", "-"]
notifications:
  - Alert MIG_UNMAPPED to the Data Steward when a validation finds unmapped codes.
audit:
  - The report run is recorded.
acceptance:
  - A staged row whose insurer code is not mapped appears in MIG-UNMAPPED-CODES with its legacy key.
  - The load of that batch cannot be approved until the code is mapped and the batch validated again.
  - The report exports to Excel and PDF.
```

```fr
id: FR-DM-013
title: Validate staged data against data-quality rules
brd: [BRID 1.1b (p.7), BRID 2.1 (p.7), BRID 3.1 (p.7)]
actor: BIBS job (MIG_VALIDATE); Data Steward
priority: Must have
fit: NEW
screens: Batches (Issues tab); Layouts and Rules
api: POST /api/v1/migration/batches/{no}/validate; GET .../issues
description:
  - A batch is validated before it can load. Each staged row is mapped and checked against the rules of its object - mandatory fields, formats, lookups, uniqueness, references to loaded parents, cross-field rules (for example components that add up to the gross premium, open balance equal to booked less paid) and plausibility warnings.
  - Each failed rule is an issue with severity ERROR (the row is not loaded) or WARNING (the row loads and is reported).
preconditions:
  - The batch is PLANNED with at least one staged extract.
main_flow:
  - The operator clicks **Validate**.
  - BIBS maps each row with the approved map versions, runs the rules and, for clients, the matching (FR-DM-031).
  - BIBS sets each row VALID, WARNING or INVALID and the batch VALIDATED with the counts.
  - The Data Steward resolves the issues - fixed at source (new extract), mapped (new map version) or waived by the data owner with a reason.
alternate_flows:
  - Error rate above the threshold. The batch cannot be approved for load; the steward sees the rate and the threshold.
rules:
  - [R1, "Master data may be loaded with at most MIG_MAX_ERROR_RATE_MASTER percent of rows rejected or waived (default 0.5).", Configurable, "Parameter MIG_MAX_ERROR_RATE_MASTER"]
  - [R2, "Financial objects (open invoices, UPP, GL trial balance) load only with 0 errors, or with each excluded row approved by the data owner with a manual-entry plan. The error rate is (invalid rows not waived + waived rows) / (staged rows - excluded rows); excluded rows are not errors and not in the base.", Configurable, "Parameter MIG_MAX_ERROR_RATE_FINANCIAL"]
  - [R3, "For each component of a legacy invoice - open = booked + adjusted - paid - written off (premium receivable) and open = booked + adjusted - remitted (DTIP and commission).", Fixed, "-"]
validations:
  - [Mandatory field blank, "{field} is mandatory", "-"]
  - [Wrong format, "{field} {value} is not a valid {type}", "-"]
  - [Parent not loaded, "{parent} {key} is not loaded", "-"]
  - [Components do not add up, "The premium components add up to {x}; the gross premium is {y}", "-"]
  - [Error rate above threshold, "{rate} percent of rows have errors; the limit for this object is {limit} percent", "-"]
notifications:
  - "None; the batch page shows the result."
audit:
  - Every validation run and every issue resolution or waiver is recorded with user and time.
acceptance:
  - A legacy invoice whose components do not add up to its gross premium is INVALID with that message.
  - A waived row shows the waiver reason and the data owner who approved it.
  - A batch of open invoices with one INVALID row cannot be approved for load unless the row is excluded by the data owner; once it is excluded, the error rate shown is 0 percent.
```

```fr
id: FR-DM-014
title: Load a batch through the BIBS services
brd: [BRID 1.1b (p.7)]
actor: Data Migration Lead (approval); Migration Operator; BIBS job (MIG_LOAD)
priority: Must have
fit: NEW
screens: Batches
api: POST /api/v1/migration/batches/{no}/approve-load; .../load
description:
  - A validated batch is approved for load (gate G4) and then loaded. Each row is loaded through the same BIBS service that the screens use (for example client registration, account import, the Operations ledger, the unapplied workbench, the journal service), so every BIBS rule, audit entry and accounting entry applies. BIBS never writes directly into business tables.
  - Each loaded record gets a key cross-reference from its legacy key, which makes the load rerunnable and the record traceable.
preconditions:
  - The batch is VALIDATED within the error threshold; the objects it depends on are accepted (G6) in the same environment.
main_flow:
  - The Data Migration Lead reviews the counts and clicks **Approve Load**.
  - The operator clicks **Load**, or the cutover plan starts it.
  - BIBS loads VALID and WARNING rows in chunks, several partitions in parallel, and marks each row LOADED, SKIPPED (already loaded, unchanged) or REJECTED (with the reason).
  - BIBS sets the batch LOADED or LOADED_WITH_REJECTS and starts the reconciliation (FR-DM-020).
alternate_flows:
  - A chunk fails. BIBS retries its rows one by one so that one bad row fails alone.
  - The row was loaded before with different content. A client or header is updated (delta, before the freeze); an open item is REJECTED as changed after load.
  - Job interrupted. The load resumes from the last committed chunk; loaded rows are skipped.
rules:
  - [R1, "Chunks of MIG_CHUNK_SIZE rows (default 500), MIG_PARTITIONS partitions (default 4).", Configurable, "Parameters MIG_CHUNK_SIZE, MIG_PARTITIONS"]
  - [R2, "One load at a time per object and environment.", Fixed, "-"]
  - [R3, "Records created by a load carry the origin MIGRATED (clients, accounts, UPP) or LEGACY (invoices); they trigger no notification to customers or insurers.", Fixed, "-"]
  - [R4, "The approver of the load is not the operator who runs it.", Fixed, "-"]
validations:
  - [Dependency not accepted, "Object {object} must be accepted before this object can load", "-"]
  - [Approver is the operator, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
notifications:
  - A failed load raises alert MIG_LOAD_FAILED to the Data Migration Lead and the operator.
audit:
  - The batch run log records each step with counts and times; every created record has its own audit entry from the owning module.
acceptance:
  - Loading the same batch twice creates no duplicate; the second run marks the rows SKIPPED.
  - A client created by a load is found in client search and has a cross-reference to its legacy key.
  - The batch page shows staged, loaded, skipped, rejected and excluded counts that add up.
```

```fr
id: FR-DM-015
title: Rerun and roll back a batch
brd: [BRID 1.1b (p.7)]
actor: Data Migration Lead (request); reconciliation approver (approval); Migration Operator
priority: Must have
fit: NEW
screens: Batches
api: POST /api/v1/migration/batches/{no}/rerun; .../rollback; .../rollback/approve
description:
  - Rejected rows are corrected and loaded again in a RERUN batch that belongs to the original batch and is reconciled with it.
  - A batch that is not yet signed off can be rolled back when its loader supports it and the records have not been changed since the load. Otherwise the environment is restored from the snapshot taken before the load.
preconditions:
  - Rerun - the batch has rejected rows and corrections are available (new extract, new map version or waiver).
  - Rollback - the batch is LOADED or RECONCILED and not signed off (G6).
main_flow:
  - For a rerun, the Data Migration Lead clicks **Rerun Rejects**; BIBS creates a child batch with the rejected and changed rows, validates it and, after approval, loads it.
  - For a rollback, the lead clicks **Request Rollback** with a reason; the reconciliation approver approves it.
  - BIBS undoes the records of the batch newest first through the owning services (for example deactivation of a migrated client without activity, reversal of an opening entry) and marks the batch ROLLED_BACK.
alternate_flows:
  - A record was changed after the load (for example a payment applied to a legacy invoice). BIBS lists the records and refuses the rollback; the environment snapshot is the remaining option.
rules:
  - [R1, "No rollback after the object is accepted (G6); after go-live, corrections are made through the normal business functions.", Fixed, "-"]
  - [R2, "Opening entries are reversed with negative entries of the same event; the original entries stay.", Fixed, "-"]
validations:
  - [Record changed after load, "{n} records were changed after the load and cannot be rolled back", "-"]
  - [Approver is the requester, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
notifications:
  - The approver is notified of a rollback request; the requester of the decision.
audit:
  - Rerun and rollback steps are in the batch log; each undone record is audited by its module.
acceptance:
  - A rerun batch loads only the previously rejected rows and its counts are added to the parent batch reconciliation.
  - A rollback of a client batch without later activity leaves no active migrated client of that batch.
  - A rollback is refused when a payment was applied to a legacy invoice of the batch.
```

## Reconciliation

```fr
id: FR-DM-020
title: Reconcile each data object from source to target
brd: [BRID 1.1b (p.7)]
actor: Reconciliation approver; BIBS job (MIG_RECONCILE)
priority: Must have
fit: NEW
screens: Reconciliation
api: POST /api/v1/migration/batches/{no}/reconcile; GET /api/v1/migration/reconciliation; reports MIG-RECON-SUMMARY, MIG-RECON-DETAIL
description:
  - After each load BIBS reconciles the object at four levels, and financial objects at a fifth. L1 counts - control file, received, staged, loaded, skipped, rejected and excluded. L2 amounts - sums per amount column and currency against the control totals and the values in BIBS. L3 hash totals - key column hash and a checksum per row. L4 fields - every mapped field of every loaded row read back from BIBS and compared with the staged value. L5 GL - see FR-DM-021.
  - Every difference is a break. A break is fixed (rerun) or explained; the explanation is approved by the reconciliation approver. This makes "all data objects and items reconcilable from source to target" (BRD acceptance criterion) a signed, reproducible result.
preconditions:
  - The batch is LOADED or LOADED_WITH_REJECTS.
main_flow:
  - BIBS runs the reconciliation after the load, or the approver clicks **Reconcile**.
  - BIBS shows the object by level with MATCHED or BREAK and the difference.
  - The Data Steward writes an explanation for each break (reason from a list and text).
  - The reconciliation approver approves the explanations and signs the reconciliation (gate G5).
rules:
  - [R1, "L1 - received = control count, and loaded + skipped + rejected + excluded = staged.", Fixed, "-"]
  - [R2, "L2 - amount difference within MIG_AMOUNT_TOLERANCE (default 0.00) per currency.", Configurable, "Parameter MIG_AMOUNT_TOLERANCE"]
  - [R3, "L4 - a field difference is a break unless the code map explains it (mapped value).", Fixed, "-"]
validations:
  - [Sign-off with unexplained break, Explain and approve every break before signing the reconciliation, "-"]
fields_screen: Break explanation
fields:
  - [Level, Text, "Yes", "L1-L5", "Read only"]
  - [Measure, Text, "Yes", "-", "Read only"]
  - [Difference, Amount / Number, "Yes", "-", "Read only"]
  - [Reason, List, "Yes", "LOV MIG_BREAK_REASON", "-"]
  - [Explanation, Text, "Yes", "-", "Up to 2,000 characters"]
notifications:
  - A break raises alert MIG_RECON_BREAK to the Data Migration Lead and the Data Steward.
audit:
  - Reconciliation runs, lines, explanations and approvals are kept as evidence and are not purged with the staging data.
acceptance:
  - For a batch of 1,000 open invoices loaded without rejects, L1 shows 1,000 at every stage and L2 shows 0.00 difference per currency.
  - A field changed in BIBS after the load appears as an L4 break on the next reconciliation.
  - The reconciliation cannot be signed while a break has no approved explanation.
  - MIG-RECON-SUMMARY and MIG-RECON-DETAIL export to Excel and PDF.
```

```fr
id: FR-DM-021
title: Reconcile carried-forward balances to the GL
brd: [BRID 1.1b (p.7), BRID 5.1 (p.8)]
actor: Reconciliation approver (Comptrollership)
priority: Must have
fit: NEW
screens: Reconciliation (GL tab); ACSL GL to Sub-ledger Reconciliation
api: Reports MIG-GL-CLEARING, MIG-LEGACY-POSITIONS; ACSL GL-SL reconciliation
description:
  - Each open legacy invoice and each legacy UPP item is loaded with an opening entry against the Migration Clearing account. The GL trial balance of the legacy systems is loaded as an opening journal in which the lines of the legacy control accounts are replaced by Migration Clearing. When the detail and the trial balance agree, Migration Clearing is 0.00 per branch and currency.
  - The legacy control accounts are reconciled to the legacy sub-ledgers in the ACSL GL to Sub-ledger Reconciliation, which gets a ledger-context filter (legacy / new).
preconditions:
  - Open invoices, UPP and the GL trial balance are loaded for the environment.
main_flow:
  - The approver runs MIG-GL-CLEARING.
  - BIBS shows the Migration Clearing balance per branch and currency and the legacy control accounts against their sub-ledgers.
  - Any non-zero balance is investigated and explained or corrected by a rerun.
  - The approver signs the GL level of the reconciliation.
rules:
  - [R1, "Migration Clearing must be 0.00 per branch and currency before go-live.", Fixed, "-"]
  - [R2, "The legacy control accounts and the Migration Clearing account are set up by Comptrollership; the GL account code map says which legacy accounts go to Migration Clearing.", Configurable, "Chart of accounts; code map GL_ACCOUNT"]
  - [R3, "A difference on Migration Clearing has the sign of the trial balance - for an invoice missing from the load, a debit equal to its net receivable position (open PR + PR2307 + commission receivable - open DTIP - unrealised commission - deferred VAT) when positive, a credit when negative; for a missing UPP item, a credit equal to its balance.", Fixed, "-"]
  - [R4, "MIG-GL-CLEARING also compares, per branch, currency and legacy control account, the trial balance line with the opening detail posted to that account, so a missing invoice whose net is zero is still found.", Fixed, "-"]
  - [R5, "The same checks run again after each FY2027 true-up (FR-DM-024).", Fixed, "-"]
validations:
  - [Clearing not zero at sign-off, "Migration Clearing is {amount} in {branch} {currency}; it must be zero", "-"]
  - [Control account line differs from the detail, "{account} differs from the opening detail by {amount} in {branch} {currency}", "-"]
notifications:
  - Alert MIG_CLEARING_NOT_ZERO to Comptrollership and the Data Migration Lead.
audit:
  - The report runs and the sign-off are recorded.
acceptance:
  - After the loads of open invoices, UPP and the trial balance of a branch, Migration Clearing is 0.00 in PHP and USD.
  - The ACSL reconciliation of the legacy Premium Receivable account against the legacy invoices shows no difference.
  - An invoice paid in full and not remitted, with 5,000.00 open DTIP and 800.00 commission receivable, missing from the load leaves a credit of 4,200.00 on Migration Clearing; an unpaid invoice with 3,000.00 premium receivable and 3,000.00 DTIP missing from the load leaves Migration Clearing at 0.00 and shows a 3,000.00 difference on Premium Receivable - Legacy and on DTIP - Legacy.
```

```fr
id: FR-DM-022
title: Load the provisional GL opening at the year-end boundary
brd: [BRID 1.1b (p.7), BRID 5.1 (p.8), BRID 12.1 (p.12)]
actor: Head, Comptrollership; Migration Operator
priority: Must have
fit: NEW
screens: Batches (object G01); Reconciliation (GL tab); Journal inquiry
api: Batch of object G01; reports MIG-GL-CLEARING, MIG-RECON-SUMMARY
description:
  - For a go-live on 3 January 2028 at the year-end boundary (DMQ39, option A, recommended and awaiting Comptrollership confirmation), the BIBS GL opens from the preliminary December 2027 trial balance of legacy, taken after the December soft close and the last legacy EOD. Only the balance-sheet accounts are opened. The FY2027 P&L accounts are not opened; their net result per branch and currency goes to retained earnings, so the FY2028 P&L in BIBS starts at zero.
  - The opening is provisional. The FY2027 closing and audit adjustments that Comptrollership posts in legacy after the freeze reach BIBS as true-ups (FR-DM-023). The FY2027 BIR annual returns and the FY2027 audit use legacy; FY2028 uses BIBS.
preconditions:
  - The GL_ACCOUNT code map is approved and the company's retained earnings account is set.
  - Comptrollership has signed the preliminary December trial balance as the provisional opening.
main_flow:
  - The operator validates and loads G01 (trial balance version PROVISIONAL).
  - BIBS posts one OPENING journal per branch and currency, value date 1 January 2028, reference MIG-TB-<as-of>, flag PROVISIONAL - balance-sheet lines on their mapped accounts, legacy control-account lines on Migration Clearing, and the lines whose mapped account is a P&L account on retained earnings.
  - The reconciliation compares the journals with the legacy trial balance and Migration Clearing with the open-item detail (FR-DM-020, FR-DM-021).
rules:
  - [R1, "The opening journals are system journals of type OPENING, source MIGRATION, value date MIG_OPENING_VALUE_DATE (default 1 January 2028, the first day of the opening period).", Configurable, "Parameter MIG_OPENING_VALUE_DATE"]
  - [R2, "No FY2027 P&L balance is opened in BIBS; a line whose mapped account is a P&L account posts to retained earnings, with the mapped account kept in the line description.", Fixed, "-"]
  - [R3, "The opening stays marked PROVISIONAL until the closure of the true-ups (FR-DM-024).", Fixed, "-"]
validations:
  - [Trial balance out of balance, "The trial balance of {branch} {currency} is out of balance by {amount}", "-"]
  - [Retained earnings account missing, "The retained earnings account of the company is not set", "-"]
  - [Opening not signed as provisional, "The preliminary trial balance is not signed by Comptrollership", "-"]
notifications:
  - "Alert MIG_CLEARING_NOT_ZERO when Migration Clearing is not 0.00 after the load."
audit:
  - The journals, the trial balance extract and the sign-off are kept as evidence.
acceptance:
  - After the load of a branch, its balance-sheet accounts in BIBS equal the legacy preliminary trial balance and its P&L accounts are 0.00 at 1 January 2028.
  - A legacy trial balance with a FY2027 net income of 1,250,000.00 in a branch gives a credit of 1,250,000.00 to retained earnings in that branch.
  - The load cannot be approved while the preliminary trial balance is not signed.
  - The opening journals show the flag PROVISIONAL until the true-ups are closed.
```

```fr
id: FR-DM-023
title: Post the FY2027 true-ups as opening-balance adjustment journals
brd: [BRID 1.1b (p.7), BRID 5.1 (p.8), BRID 12.1 (p.12)]
actor: Comptrollership GL lead (prepares); Head, Comptrollership (approves); Migration Operator
priority: Must have
fit: NEW
screens: True-ups; Extracts; Batches (object G03); Journal inquiry
api: GET/POST /api/v1/migration/trueups; POST .../trueups/{no}/submit, .../approve, .../return; batch of object G03
description:
  - After the freeze the legacy GL stays open only for the FY2027 closing and audit adjustments, restricted to named Comptrollership users, with no new business. Each adjustment reaches BIBS in a true-up - the first after the legacy year-end close (about mid to late January 2028), the final one after the audited financial statements (about March-April 2028), and interim ones only when Comptrollership posts material adjustments in between.
  - A true-up is an opening-balance adjustment journal in the opening period (January 2028, value date 1 January 2028), never a FY2028 transaction. FY2027 P&L effects go to retained earnings. Adjustments on legacy control accounts, which BIBS holds invoice by invoice and UPP by UPP, come with their open-item detail and change those items.
preconditions:
  - The provisional opening is loaded (FR-DM-022).
  - BDOI IT has sent, each with its control file, the legacy trial balance after the adjustments (G01, version TU1, TU2 or FINAL), the adjustment journal lines since the previous true-up (G03), the open-item detail of the lines on legacy control accounts (G03D) and the legacy journal listing since the freeze.
main_flow:
  - The operator runs the intake checks and the validation of the true-up batch.
  - The Comptrollership GL lead reviews the validation result against the FY2027 adjustment register and submits the true-up.
  - The Head of Comptrollership reviews the journals per branch and currency and approves; BIBS posts the opening-balance adjustment journals and the open-item adjustments.
  - BIBS runs the true-up reconciliation (FR-DM-024).
alternate_flows:
  - Opening period closed. When January 2028 is already closed, Comptrollership reopens it with a reason for the posting only; BIBS posts; Comptrollership closes it again the same day. The reconciliation lists every journal posted into the period during the window.
  - Return. The approver returns the true-up with a reason; the GL lead corrects the input (new extract) and submits again.
  - Error found after posting. A posted true-up is never changed; the next true-up corrects it.
rules:
  - [R1, "A true-up posts as a system journal of type OPENING, source MIGRATION, reference MIG-TU-<n>-<branch>-<currency>, value date MIG_OPENING_VALUE_DATE (1 January 2028).", Fixed, "-"]
  - [R2, "FY2027 P&L lines post to retained earnings. Lines on legacy control accounts post to Migration Clearing and are matched by the open-item detail, which changes the open balance of each legacy invoice or UPP item (movement Legacy adjusted or Legacy written off) against Migration Clearing.", Fixed, "-"]
  - [R3, "Only journals dated in FY2027, posted in legacy after the freeze and entered in the FY2027 adjustment register are accepted.", Fixed, "-"]
  - [R4, "The preparer and the approver are different users; the approver holds MIG_TRUEUP_APPROVE.", Fixed, "-"]
  - [R5, "Posting is idempotent on the reference; the same true-up posted twice creates no second journal.", Fixed, "-"]
validations:
  - [Journal dated outside FY2027, "Journal {no} is dated {date}, outside FY2027", "-"]
  - [Journal not in the register, "Journal {no} is not in the FY2027 adjustment register", "-"]
  - [Journal out of balance, "Journal {no} is out of balance by {amount} in {branch} {currency}", "-"]
  - [Control-account line without detail, "{account} adjustment of {amount} has no matching open-item detail", "-"]
  - [Detail beyond the item balance, "Adjustment of {component} on {item} makes the open balance {amount}", "-"]
  - [Approver is the preparer, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
fields_screen: True-up
fields:
  - [True-up no., Text, "Yes", "1, 2, ..., F (final)", "Read only; from the extract"]
  - [Legacy trial balance as of, Date, "Yes", "-", "Read only"]
  - [Journals, Read-only list, "-", "-", "Legacy journal no., date, kind, register reference, total per branch and currency"]
  - [Open-item detail, Read-only list, "-", "-", "Item, component, movement, amount"]
  - [Approval remarks, Text, "Cond.", "-", "Mandatory to return"]
notifications:
  - A submitted true-up goes to the Head of Comptrollership in My Approvals; the posting and a return are notified to the GL lead and the Data Migration Lead.
audit:
  - Preparation, approval, posting, and the reopening and closing of the period are audited; the true-up record keeps the evidence and is not purged with the staging data.
acceptance:
  - True-up 1 with a FY2027 accrual of 80,000.00 (Dr expense, Cr accrued expenses) posts, after approval, a journal MIG-TU-1 dated 1 January 2028 with Dr retained earnings 80,000.00 and Cr accrued expenses 80,000.00; the FY2028 P&L does not change.
  - An audit write-off of 3,000.00 of premium receivable on legacy invoice I00300002, sent with its detail line, reduces the open basic premium of the invoice and Premium Receivable - Legacy by 3,000.00 and leaves Migration Clearing at 0.00.
  - A journal dated 5 January 2028, or a journal not in the register, stops the validation with its message.
  - The GL lead who prepared a true-up cannot approve it.
  - With January 2028 closed, the final true-up posts after Comptrollership reopens the period, and the period is closed again the same day.
```

```fr
id: FR-DM-024
title: Reconcile each true-up to the legacy trial balance and close the FY2027 cut-off
brd: [BRID 1.1b (p.7), BRID 12.1 (p.12)]
actor: Head, Comptrollership; Data Migration Lead; Program Manager
priority: Must have
fit: NEW
screens: True-ups (Reconciliation tab); Run-off and Decommissioning
api: GET /api/v1/migration/trueups/{no}/reconciliation; POST .../signoff, POST /api/v1/migration/trueups/closure; reports MIG-TRUEUP-RECON, MIG-TRUEUP-REGISTER
description:
  - Each true-up is reconciled to the legacy trial balance before it is signed, and the legacy GL stays under cut-off controls until the final true-up. The checks are cut-off (every legacy journal since the freeze is in the register and in a true-up, dated in FY2027 and posted by a named Comptrollership user, and no legacy business module posted after the freeze), movement (the true-up equals the change of the legacy trial balance since the previous true-up), balance (the BIBS opening balances equal the legacy trial balance of the true-up), Migration Clearing and the legacy sub-ledgers, and, when the opening period was reopened, no other journal in it during the window.
  - After the final true-up, the balance check against the audited FY2027 trial balance is signed and shared with the external auditor, the legacy GL is locked for all users, and the true-ups are closed. A FY2027 finding after that is a prior-period adjustment in BIBS through the normal Comptrollership journal, outside the migration.
preconditions:
  - The true-up is posted (FR-DM-023).
main_flow:
  - BIBS runs MIG-TRUEUP-RECON after the posting.
  - The Head of Comptrollership reviews each check and signs the true-up reconciliation.
  - After the final true-up, BDOI IT locks the legacy GL, Comptrollership closes the adjustment register, and the Head of Comptrollership and the Program Manager sign the closure of the true-ups.
alternate_flows:
  - Break. A failed check raises MIG_TRUEUP_BREAK; the cause is found and corrected by a corrected extract before sign-off or by the next true-up.
rules:
  - [R1, "Movement - per BIBS account, branch and currency, the true-up lines equal the legacy trial balance of the true-up minus the legacy trial balance of the previous true-up (or of the provisional opening), both mapped through GL_ACCOUNT, with the P&L netted into retained earnings.", Fixed, "-"]
  - [R2, "Balance - per BIBS account, branch and currency, the provisional opening plus true-ups 1 to n equal the legacy trial balance of true-up n; Migration Clearing is 0.00; the legacy control accounts equal their sub-ledgers.", Fixed, "-"]
  - [R3, "Cut-off - the legacy journal listing since the freeze equals the FY2027 adjustment register and the true-up journals; no journal is dated outside FY2027; no posting user is outside the Comptrollership access list; no legacy business module posted after the freeze.", Fixed, "-"]
  - [R4, "A true-up with a break cannot be signed.", Fixed, "-"]
  - [R5, "After the closure no further true-up can be created, and the opening journals lose the PROVISIONAL flag.", Fixed, "-"]
validations:
  - [Legacy journal not in the true-up, "Legacy journal {no} posted on {date} is not in the adjustment register or the true-up", "-"]
  - [Movement differs, "{account} {branch} {currency} - true-up {x}, legacy trial balance change {y}", "-"]
  - [Balance differs, "{account} {branch} {currency} - BIBS opening {x}, legacy trial balance {y}", "-"]
  - [Sign-off with a break, "Check {check} is not met", "-"]
  - [New true-up after closure, "The FY2027 true-ups are closed", "-"]
notifications:
  - Alert MIG_TRUEUP_BREAK to the Head of Comptrollership and the Data Migration Lead.
audit:
  - Reconciliation runs, sign-offs, the legacy lock evidence and the closure are kept as evidence.
acceptance:
  - For true-up 1, MIG-TRUEUP-RECON shows each account's true-up equal to the change of the legacy trial balance, the BIBS opening equal to the legacy post-close trial balance, and Migration Clearing 0.00.
  - A legacy journal that is in the legacy journal listing but not in the register stops the sign-off with the message.
  - After the final true-up is signed and the closure recorded, a new true-up cannot be created and the opening journals are no longer marked PROVISIONAL.
```

## Client master and reference data

```fr
id: FR-DM-030
title: Load reference data through the code maps
brd: [BRID 3.1 (p.7)]
actor: Data Steward; data owner
priority: Must have
fit: NEW
screens: Code Maps; Batches; the reference-data screens of each master
api: Batches of objects R01-R07
description:
  - Reference data (LOV and MIS values, sales organisation, insurers and branches, product lines, products and risk codes, packages, commission rates) is loaded first. Legacy values already present in BIBS are mapped; values marked CREATE are created through the master's own service and authorised through its maker-checker.
preconditions:
  - The code map versions of the object are approved (G2).
main_flow:
  - The operator validates and loads the reference batch.
  - BIBS creates the CREATE values as pending authorisation in the master.
  - The authoriser of each master authorises them (existing maker-checker).
  - The Data Steward confirms that every required code exists; the batch is reconciled and accepted.
alternate_flows:
  - A created value is rejected by the master's authoriser. The code map entry is changed to MAP or REJECT in a new version.
rules:
  - [R1, "Dependent objects (clients, headers, invoices) do not load until every code they use is mapped to an active, authorised value.", Fixed, "-"]
validations:
  - [Required code missing, "Code {code} of {domain} does not exist or is not authorised", "-"]
notifications:
  - Created values appear in the authorisers' My Approvals.
audit:
  - Each created or authorised value is audited by its master.
acceptance:
  - A legacy LOV value marked CREATE becomes an authorised BIBS value after the master authorisation, and the batch reconciles.
  - A client batch using a segment that is not yet authorised cannot be approved for load.
```

```fr
id: FR-DM-031
title: Match and deduplicate legacy clients
brd: [BRID 2.1 (p.7)]
actor: BIBS job; Data Steward
priority: Must have
fit: CHANGE
screens: Client Matching
api: GET /api/v1/migration/matching/queue; POST .../matching/{pair}/decision
description:
  - Legacy clients of all source systems are matched with each other and with clients already in BIBS before any is loaded. Exact matches on hard keys merge automatically; likely matches go to the Data Steward's review queue; the rest become new clients. The keys start from the BIBS duplicate keys (TIN, ID type and number, name with birth date, corporate name) and the keys BDOI agrees (DMQ04).
  - Clients that match form one cluster and become one BIBS client, built field by field from the best source (survivorship rules).
preconditions:
  - The client batch is being validated (FR-DM-013).
main_flow:
  - BIBS scores every candidate pair on the keys.
  - Pairs at or above the auto-merge score merge; pairs between the review and auto-merge scores go to the review queue; lower pairs are separate clients.
  - The Data Steward opens Client Matching, compares the records side by side with the matching keys and decides merge or keep separate.
  - BIBS builds each cluster's record with the survivorship rules and shows the result before load.
alternate_flows:
  - Match with a client already in BIBS. The legacy record merges into the existing client; the load updates only blank fields and adds the cross-reference.
rules:
  - [R1, "Scores - TIN 100; ID type and number 100; name with birth date 95; corporate name with registration number 95, without 90; bank CIF 100 if agreed; e-mail 40; mobile 40; similar name with the same birth date or city 70.", Configurable, "Matching rules"]
  - [R2, "Auto-merge at MIG_CLIENT_MATCH_AUTO (default 90); review from MIG_CLIENT_MATCH_REVIEW (default 60).", Configurable, "Parameters MIG_CLIENT_MATCH_AUTO, MIG_CLIENT_MATCH_REVIEW"]
  - [R3, "Survivorship per field - first non-blank by source priority, or the latest updated value when the update date is given.", Configurable, "Survivorship rules"]
  - [R4, "The client batch cannot be approved for load while the review queue has open pairs.", Fixed, "-"]
validations:
  - [Load approval with open review pairs, "{n} client pairs are waiting for review", "-"]
fields_screen: Client match review
fields:
  - [Legacy record A / B, Read-only panel, "-", "-", "Names, IDs, TIN, birth date, addresses, contacts, source"]
  - [Matching keys, Read-only list, "-", "-", "Keys that matched and score"]
  - [Decision, List, "Yes", "Merge, Keep separate", "-"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - Every decision and every value that lost in survivorship is kept (report MIG-CLIENT-MATCH).
acceptance:
  - Two legacy clients with the same TIN from EBIX and QPS become one BIBS client with both legacy keys in the cross-reference.
  - Two legacy clients with similar names and the same birth date but different IDs go to the review queue.
  - The client batch cannot load while a pair is open in the review queue.
```

```fr
id: FR-DM-032
title: Create migrated clients in the Client Master
brd: [BRID 2.1 (p.7)]
actor: BIBS job (client load)
priority: Must have
fit: CHANGE
screens: Batches; Client (existing screens)
api: Batch of object C01-C03
description:
  - Migrated clients are created as active clients, not as prospects, with their contacts, addresses, payout accounts and the KYC status and review date given by legacy. The onboarding workflow is not repeated.
  - Sanction screening does not run client by client during the load. After the client object is loaded, one full screening run covers every client (DMQ06).
  - Until the freeze, new and changed legacy clients are loaded every day as delta batches (umbrella BRD p.43).
preconditions:
  - The client batch is validated and matched; reference codes exist.
main_flow:
  - BIBS creates each client cluster through the client service for migrated clients.
  - BIBS records the cross-reference of every legacy key of the cluster.
  - After the batch, the cutover plan starts the full screening run.
alternate_flows:
  - Delta - changed client. BIBS updates the fields of the delta layout through the client service; the change is audited.
  - Screening hit. The hit follows the Sanction Screening case process; the client stays active unless Compliance decides otherwise.
rules:
  - [R1, "Migrated clients have origin MIGRATED and status ACTIVE; KYC status and review date are taken from legacy.", Fixed, "-"]
  - [R2, "Delta loads stop at the freeze; after go-live the client master is maintained only in BIBS.", Fixed, "-"]
validations:
  - [Hard duplicate at load, This client matches an existing client on TIN or ID; decide the match first, "-"]
notifications:
  - "None to clients."
audit:
  - Each client creation and update is audited by the client module with the batch number.
acceptance:
  - A migrated client is ACTIVE and can be selected on a new quotation without re-encoding.
  - No screening case is created during the load; after the full run, hits appear in the screening queue.
  - A client changed in legacy the day before the freeze is updated in BIBS by the delta batch.
```

```fr
id: FR-DM-033
title: Find migrated clients and policies by legacy reference
brd: [BRID 2.1 (p.7), BRID 4.1 (p.8)]
actor: Marketing User, Operations User, Contact Center
priority: Must have
fit: CHANGE
screens: Client search; Account search; Customer Servicing search
api: GET /api/v1/crm/clients?legacyRef=; GET /api/v1/accounts?legacyRef=
description:
  - Users find a migrated client or policy by its BIBS code and also by its legacy client number, cover number or policy number. The record shows the source system and the legacy reference.
preconditions:
  - The records are loaded.
main_flow:
  - The user enters a legacy reference in the search.
  - BIBS looks it up in the cross-reference and opens the BIBS record.
alternate_flows:
  - Reference not migrated. BIBS says the reference is not in BIBS and, if the user has LEGACY_INQUIRY_VIEW, offers the legacy archive search.
rules:
  - [R1, "Legacy references are read only.", Fixed, "-"]
validations:
  - [Reference not found, No BIBS record has this legacy reference, "-"]
notifications:
  - "None."
audit:
  - "Searches follow the audit rules of each screen."
acceptance:
  - Searching an EBIX client number opens the BIBS client created from it.
  - The client page shows the source systems and legacy numbers of the cluster.
```

```fr
id: FR-DM-034
title: Load the PACKAGE code map for the Renewal package remapping
brd: [BRID 3.1 (p.7), BRID 12.1 (p.12)]
actor: TSU (prepares the PACKAGE map); Product Owner, Marketing Business System (approves); Renewal processing team (Exception bucket)
priority: Must have
fit: NEW
screens: Code Maps (set PACKAGE); Batches (object R06); Renewal candidates (Exception bucket)
api: GET/POST /api/v1/migration/maps/PACKAGE/versions; batch of object R06; report MIG-MAP-VERSIONS
description:
  - Legacy packages are remapped to the package versions that TSU maintains in BIBS (BRD-3) at Renewal sanitation, per candidate, and not at migration intake (DMQ36, answered 26 September 2026). The migration loads the PACKAGE code map as reference data only - entries from a legacy package and version to a BIBS package version, with conditional entries where one legacy package splits (one qualifier - risk code, insurer or sum-insured band).
  - Policy headers (FR-DM-040) and the renewal advices already sent (FR-DM-125) keep the legacy package as given; no package is resolved, rejected or warned at intake. At renewal the Renewal sanitation check PACKAGE_REMAP resolves the BIBS package through the map. A package without an entry, with a REJECT entry or with no matching qualifier sends the candidate to the Exception bucket, where the Renewal processing team chooses the package; each choice is passed to TSU for the next map version.
preconditions:
  - The BIBS package versions exist in Product Maintenance.
  - The user has MIG_MAPPING_EDIT (TSU) or MIG_MAPPING_APPROVE (Product Owner).
main_flow:
  - TSU creates a DRAFT version of set PACKAGE (copied from the approved version or imported from Excel), enters one entry per legacy package and version, with a qualifier where the package splits, and submits it.
  - The Product Owner compares the version with the approved one and approves it (FR-DM-011).
  - The operator loads object R06; BIBS hands the approved version to the Renewal package map and reconciles it (L1-L4).
  - In each trial migration BIBS lists the legacy packages of the headers expiring up to 31 May 2028 that have no entry (information report); TSU closes the gaps before the map freeze.
alternate_flows:
  - Overlapping qualifiers. The version cannot be submitted until the entries are corrected.
  - Package not resolved at renewal. The candidate goes to the Exception bucket of Renewal.
rules:
  - [R1, "Packages are remapped at Renewal sanitation (check PACKAGE_REMAP) with the PACKAGE map loaded by the migration; TSU prepares each version and the Product Owner of Marketing Business System approves it.", Configurable, "Code map PACKAGE; role-permission matrix"]
  - [R2, "A conditional entry has one qualifier - risk code, insurer or sum-insured band; the qualifiers of one legacy package do not overlap.", Fixed, "-"]
  - [R3, "The migration intake does not resolve or check the package of a policy header or of a renewal advice.", Fixed, "-"]
  - [R4, "An unresolved package at renewal sends the candidate to the Exception bucket, worked by the Renewal processing team.", Fixed, "-"]
validations:
  - [Overlapping qualifiers, "Legacy package {code} has overlapping entries for {qualifier}", "-"]
  - [Target package not active, "Target code {code} does not exist in {domain}", "-"]
fields_screen: PACKAGE map entry
fields:
  - [Legacy package and version, Text, "Yes", "-", "As in P01"]
  - [Qualifier, List, "No", "Risk code, Insurer, Sum-insured band", "One per entry"]
  - [Qualifier value, Text, "Cond.", "-", "Mandatory with a qualifier; a band is given as from and to amounts"]
  - [Action, List, "Yes", "MAP, REJECT", "REJECT = no BIBS package; the candidate goes to the Exception bucket at renewal"]
  - [BIBS package version, List, "Cond.", "Active package versions (Product Maintenance)", "Mandatory for MAP"]
notifications:
  - "None at intake; the Exception bucket is part of the Renewal worklist."
audit:
  - Versions and entries keep their history; the R06 batch records the version it loaded (report MIG-MAP-VERSIONS).
acceptance:
  - An approved PACKAGE version with 120 entries loads into the Renewal package map and reconciles with 120 entries.
  - At renewal, a candidate on a legacy package with one entry for a sum insured up to 2,000,000.00 and one above gets the first BIBS package for a sum insured of 950,000.00 and the second for 2,500,000.00.
  - A candidate whose legacy package has a REJECT entry is in the Exception bucket.
  - A version with two entries of the same legacy package whose sum-insured bands overlap cannot be submitted.
  - A header on a legacy package without an entry loads with no warning and keeps its legacy package.
```

## In-force policy headers

```fr
id: FR-DM-040
title: Load in-force policy headers
brd: [BRID 4.1 (p.8)]
actor: BIBS job (header load); Data Migration Lead
priority: Must have
fit: CHANGE
screens: Batches; Account (existing screens, read only for header fields)
api: Batch of object P01
description:
  - When the condition of BRID 4.1 is met (DMQ09), the in-force legacy policies are loaded as BIBS accounts with a minimal header - status, key dates, insurer and shares, line of business and product, policy number, sum insured, currency, AO, unit and branch, payment arrangement and PN numbers - linked to the migrated client.
  - The header is created without quotation, placement, issuance or invoice. It is what allows legacy invoices to be endorsed in BIBS (FR-DM-090 to 092) and the policy to be renewed in BIBS (FR-DM-122, FR-DM-124).
  - The header keeps the legacy package code and version as stored in legacy; packages are remapped at Renewal sanitation, not at load (FR-DM-034).
  - Whatever the condition of BRID 4.1, the headers of every policy expiring from go-live to 31 May 2028 and of every renewal term booked in legacy that starts on or after go-live are loaded, because the go-live renewal extraction works only from the migrated headers (FR-DM-124).
preconditions:
  - Clients and reference data are accepted.
main_flow:
  - The operator validates and loads the header batch.
  - BIBS creates each account with origin MIGRATED and status BOOKED and records the cross-reference of the cover or policy number.
  - BIBS links the legacy invoices of the policy to the account when they are loaded.
alternate_flows:
  - Client not migrated. The row is INVALID ("client not loaded").
rules:
  - [R1, "A migrated header has origin MIGRATED, status BOOKED and the legacy reference; it has no BIBS invoice of its own.", Fixed, "-"]
  - [R2, "Headers in scope follow the condition decided for the object (all in-force policies, or those with open legacy invoices, as BDOI decides).", Configurable, "Object decision (FR-DM-002)"]
  - [R3, "Always in scope - every policy expiring from go-live to MIG_GOLIVE_RENEWAL_TO (31 May 2028) and every renewal term booked in legacy that starts on or after go-live.", Fixed, "-"]
  - [R4, "The legacy package code and version are kept as given; no package is resolved at load.", Fixed, "-"]
validations:
  - [Expiry before inception, The expiry date is before the inception date, "-"]
  - [Insurer shares not 100, "Insurer shares add up to {x} percent", "-"]
notifications:
  - "None."
audit:
  - Each account creation is audited by the account module with the batch number.
acceptance:
  - A migrated in-force policy is found by its policy number and shows status, dates, insurer and line, linked to the correct client.
  - A migrated header has no quotation or placement and cannot be booked again.
  - A packaged header shows the legacy package and version as sent, with no warning.
  - The P01 reconciliation against the legacy in-force list shows every policy expiring from go-live to 31 May 2028.
```

```fr
id: FR-DM-041
title: Search migrated policies for servicing
brd: [BRID 4.1 (p.8)]
actor: Operations User
priority: Must have
fit: CHANGE
screens: Account search; Account page; Invoice 360
api: GET /api/v1/accounts
description:
  - The Operations User searches a migrated policy by ARN, policy number, legacy reference or client and sees the key header details and its legacy invoices with their balances.
preconditions:
  - Headers and legacy invoices are loaded.
main_flow:
  - The user searches the policy.
  - BIBS shows the header with a LEGACY badge, source system and legacy reference.
  - The Invoices tab lists the legacy invoices with open balances; each opens in Invoice 360.
rules:
  - [R1, "Header fields of a migrated account change only through endorsements (FR-DM-090 to 092).", Fixed, "-"]
validations:
  - [No match, No policy matches the search, "-"]
notifications:
  - "None."
audit:
  - "As the account screens."
acceptance:
  - A migrated policy shows status, inception and expiry, insurer and line, and its client.
  - Its legacy invoices are listed with their open balances.
```

## Legacy open items and UPP

```fr
id: FR-DM-050
title: Carry forward legacy invoices with their open balances
brd: [BRID 5.2 (p.8), BRID 6.1 (p.9), BRID 8.1 (p.10), BRID 10.1 (p.11)]
actor: BIBS job (open invoice load); Reconciliation approver
priority: Must have
fit: CHANGE
screens: Batches; Invoice 360; Operations invoice lists
api: Batch of object F01
description:
  - Every legacy invoice open at cutover (premium receivable, premium paid and not remitted, commission receivable or PR2307 still open) enters the BIBS Operations ledger with origin LEGACY. It keeps its legacy number, source system and legacy reference. Each component is loaded with its original amount and what was paid, remitted, adjusted or written off in legacy, so its open balance equals the legacy open balance.
  - The original values are frozen at load. Every later change is compared with them (FR-DM-100).
  - An opening entry per invoice puts the open balances on the legacy sub-ledgers against Migration Clearing (FR-DM-021).
preconditions:
  - Clients, insurers, products and, where applicable, headers are accepted.
main_flow:
  - The operator validates and loads the open invoice batch.
  - BIBS creates each legacy invoice in the Operations ledger with its components, insurer shares and movements.
  - BIBS posts the opening entry and records the sub-ledger items.
  - Collections, Cashiering and Remittance see the invoice from then on.
alternate_flows:
  - Legacy gives only open balances (DMQ12). BIBS loads the open balance as the booked amount; the original values exist only in the frozen snapshot.
  - Legacy invoice number already used by another source system. BIBS prefixes the source system to the invoice number and keeps the original as the legacy number.
rules:
  - [R1, "A legacy invoice has origin LEGACY and ledger context LEGACY; all its postings use the legacy components that Comptrollership maps to the legacy sub-ledgers.", Fixed, "-"]
  - [R2, "Legacy invoices are not part of the production register extract (they were reconciled in legacy).", Fixed, "-"]
  - [R3, "Opening entries are dated the cutover date; foreign-currency amounts use the rate decided in DMQ35.", Configurable, "Parameter MIG_CUTOVER_DATE; BOOK rate"]
validations:
  - [Open balance outside 0 and booked, "The open balance of {component} is outside the booked amount", "-"]
  - [Invoice number used twice in one source, "Invoice {no} appears twice", "-"]
notifications:
  - "None."
audit:
  - Each invoice creation and its movements are audited by the Operations ledger with the batch number.
acceptance:
  - A legacy invoice with PR 10,000.00 of which 4,000.00 was paid shows booked 10,000.00, applied 4,000.00 and balance 6,000.00 in Invoice 360, with the LEGACY badge.
  - A legacy invoice paid in full before cutover and not remitted is offered for remittance (FR-DM-080) and not for collection.
  - Invoice 360 of a legacy invoice shows the source system, legacy number and original values.
```

```fr
id: FR-DM-051
title: Carry forward legacy UPP
brd: [BRID 5.1 (p.8)]
actor: BIBS job (UPP load); Cashiering User
priority: Must have
fit: CHANGE
screens: Unapplied Payments workbench; Batches
api: Batch of object F02; GET /api/v1/cashiering/unapplied?origin=MIGRATED
description:
  - Every legacy UPP item open at cutover is created in the Unapplied Payments workbench with origin MIGRATED, its legacy AR number and date, payor, client, sales unit, amount, balance, status and the references that help to match it. A disposition in progress in legacy keeps its type and details.
  - An opening entry puts the balance on the UPP legacy sub-ledger against Migration Clearing.
preconditions:
  - Clients are accepted.
main_flow:
  - The operator validates and loads the UPP batch.
  - BIBS creates each item in the tab of its legacy status and posts the opening entry.
  - The Cashiering User sees the items with the LEGACY badge, the legacy AR and the balance.
alternate_flows:
  - Receipt for a migrated UPP (DMQ14). No new AR is issued at load; the legacy AR number is shown. If BDOI requires an acknowledgment, parameter MIG_UPP_ISSUE_AR set to true makes the load issue a BIBS AR without a cash posting, referring to the legacy AR number.
rules:
  - [R1, "Legacy statuses map to the tabs Unapplied, Monitoring and For Approval through the code map STATUS:UPP.", Configurable, "Code map STATUS:UPP"]
  - [R3, "No BIBS AR is issued for a migrated UPP unless MIG_UPP_ISSUE_AR is true (default false, DMQ14); the AR then has no cash posting.", Configurable, "Parameter MIG_UPP_ISSUE_AR"]
  - [R2, "A migrated UPP keeps ledger context LEGACY; its applications, refunds and reclassifications post to the UPP legacy sub-ledger.", Fixed, "-"]
validations:
  - [Balance above amount, The balance is more than the amount received, "-"]
notifications:
  - "None."
audit:
  - Each item creation is audited by Cashiering with the batch number.
acceptance:
  - A migrated UPP shows amount, balance, legacy AR, reference and status in the workbench.
  - The UPP legacy control account equals the total of the migrated UPP balances.
  - With MIG_UPP_ISSUE_AR false, no AR is issued at load; with true, each migrated UPP gets a BIBS AR that refers to its legacy AR and posts no cash entry.
```

```fr
id: FR-DM-052
title: Automatch rerun of UPP across legacy and new invoices
brd: [BRID 5.2 (p.8), BRID 6.3 (p.9)]
actor: BIBS job (PAYMENT_AUTOMATCH); Cashiering User
priority: Must have
fit: CHANGE
screens: Unapplied Payments workbench; Payments
api: POST /api/v1/cashiering/matching/run
description:
  - The existing automatch job also takes migrated UPP items. It matches them on the references carried from legacy (invoice number, cover number, PN, payor reference) and applies the balance, oldest invoice first, to the booked invoices found - legacy or new. One item can be applied to both kinds (mixed UPP).
  - Each application posts the UPP side to the sub-ledger of the item (legacy or new) and the invoice side to the sub-ledger of the invoice, and updates the invoice's balances.
preconditions:
  - The item has a balance and at least one reference; the invoice is booked or loaded.
main_flow:
  - The job runs after every payment upload and hourly, or the Cashiering User clicks **Run Matching**.
  - BIBS matches each item's references against the Operations ledger.
  - BIBS applies the balance by the component hierarchy to the matched invoices and closes the item when it is used up.
rules:
  - [R1, "Posting by context - legacy UPP to legacy invoice - Dr UPP legacy / Cr PR legacy; legacy UPP to new invoice - Dr UPP legacy / Cr PR; new UPP to legacy invoice - Dr unapplied collections / Cr PR legacy.", Configurable, "Accounting rules of OPS_PAYMENT_APPLY (Comptrollership)"]
  - [R2, "Application order is the component hierarchy DST, premium tax / VAT, LGT, FST, other charges, basic (CSHID.022).", Fixed, "-"]
  - [R3, "Commission is realised on collection on the sub-ledger of the invoice (parameter OPS_COMMISSION_REALIZATION).", Configurable, "Parameter OPS_COMMISSION_REALIZATION"]
validations:
  - [No reference on the item, "This item has no reference to match; apply it through a disposition", "-"]
notifications:
  - "None; the run result is on the Payments screen."
audit:
  - Each application is audited with the item, invoice and amount.
acceptance:
  - A migrated UPP of 5,000.00 referring to a legacy invoice with 3,000.00 open and a new invoice with 2,000.00 open is applied to both and closes; the legacy sub-ledger is credited 3,000.00 and the new PR 2,000.00.
  - The journal of each application shows the UPP legacy account on the debit side.
  - A migrated UPP without a matching invoice stays in the Unapplied tab.
```

```fr
id: FR-DM-053
title: Apply legacy UPP to another invoice
brd: [BRID 5.3 (p.8), BRID 6.4 (p.10)]
actor: Cashiering User; Cashiering TL where approval applies
priority: Must have
fit: CHANGE
screens: Unapplied Payments workbench (disposition drawer)
api: POST /api/v1/cashiering/unapplied/{id}/disposition
description:
  - On client instruction the Cashiering User applies a legacy UPP item to another invoice, legacy or new, with the existing dispositions "Apply to other invoice" and "DST application". The application posts by context as in FR-DM-052.
preconditions:
  - The item has a balance; the target invoice is booked or loaded and has an open balance.
main_flow:
  - The user opens the item, chooses the disposition and the target invoice and enters the amount.
  - BIBS shows the application preview by component.
  - The user confirms; BIBS applies, posts and updates both sub-ledgers.
rules:
  - [R1, "The amount is above zero and at most the item balance.", Fixed, "-"]
validations:
  - [Amount above balance, "The amount must be above zero and at most the balance {balance}", DISPOSITION_AMOUNT]
  - [Target invoice not found, "Invoice {no} is not in the Operations ledger", "-"]
notifications:
  - "As the existing dispositions."
audit:
  - "Disposition and application are audited."
acceptance:
  - A legacy UPP applied to another legacy invoice credits the PR legacy sub-ledger and debits the UPP legacy sub-ledger.
  - The disposition is refused when the amount is above the balance.
```

```fr
id: FR-DM-054
title: Refund legacy UPP
brd: [BRID 5.4 (p.8-9)]
actor: Cashiering User (request); Cashiering TL (approval); Disbursement
priority: Must have
fit: CHANGE
screens: Unapplied Payments workbench; Disbursement queue
api: POST /api/v1/cashiering/unapplied/{id}/disposition (REFUND); .../approve
description:
  - An approved refund of a legacy UPP item is recorded, posted from the UPP legacy sub-ledger to the refund payable and sent to Disbursement as a payment request, as for any UPP refund.
preconditions:
  - The client's payout details exist; the refund disposition is approved.
main_flow:
  - The user chooses Refund, enters the amount and submits.
  - The Cashiering TL approves.
  - BIBS posts the refund and creates the payment request to Disbursement.
alternate_flows:
  - Disbursement returns the request. The refund can then be reversed; the balance comes back to the item.
rules:
  - [R1, "Refund entry - Dr UPP legacy / Cr refund payable; Disbursement pays the refund payable.", Configurable, "Accounting rules of OPS_UNAPPLIED_REFUND"]
  - [R2, "A refund is reversed only after Disbursement returned the request.", Fixed, "-"]
validations:
  - [Reversal before return, A refund is reversed only after Disbursement returned the request, REFUND_NOT_RETURNED]
notifications:
  - "As the existing refund disposition."
audit:
  - "Request, approval, posting and payment request are audited."
acceptance:
  - An approved refund of 1,500.00 from a legacy UPP reduces the UPP legacy sub-ledger by 1,500.00 and appears in the Disbursement queue.
```

```fr
id: FR-DM-055
title: Reclassify UPP to other income with top-management approval
brd: [BRID 5.5 (p.9)]
actor: Cashiering User (maker); Cashiering TL; Finance Approver (top management)
priority: Must have
fit: CHANGE
screens: UPP Income Reclassification; My Approvals
api: POST /api/v1/cashiering/income-reclass-batches; .../{no}/submit; .../approve; .../return
description:
  - Exceptional dispositions of UPP to other income are made in a reclassification batch. The Cashiering User selects the items (filters on age, amount, origin), the batch is approved by the Cashiering TL and then by top management, and only then executed. On execution each item's balance is posted from the UPP sub-ledger (legacy or new) to other income and the item is closed.
preconditions:
  - The user has CASH_UPP_INCOME_REQUEST; the items have a balance and no disposition in progress.
main_flow:
  - The user creates a batch (UIR-yyyy-nnnn), adds items and a reason per item.
  - The user submits; the Cashiering TL approves.
  - The top-management approver reviews the batch (list, ages, total) in My Approvals and approves.
  - BIBS executes the batch - posts each reclassification, closes each item and shows the batch as EXECUTED.
alternate_flows:
  - Return at either approval, with a reason; the batch goes back to DRAFT.
  - An item changes before execution (applied or refunded). BIBS removes it from the batch and reports it.
rules:
  - [R1, "Two approvals - Cashiering TL, then top management; neither is the maker.", Fixed, "-"]
  - [R2, "Entry - Dr UPP (legacy or new) / Cr other income account per DMQ16.", Configurable, "Accounting rules of OPS_UNAPPLIED_TO_INCOME"]
  - [R3, "An executed reclassification is reversed only by a new approved batch of type REVERSAL.", Fixed, "-"]
validations:
  - [Item with a disposition in progress, "Item {ref} has a disposition in progress", "-"]
  - [Approver is the maker, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
fields_screen: UPP Income Reclassification
fields:
  - [Batch no., Text, "-", "System", "UIR-yyyy-nnnn"]
  - [Items, Grid, "Yes", "Unapplied items with balance", "At least one"]
  - [Reason per item, List, "Yes", "LOV UPP_INCOME_REASON", "-"]
  - [Total, Amount, "-", "System", "Sum of balances"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - Submission and each approval notify the next approver; execution notifies the maker.
audit:
  - The batch, its approvals and every posting are audited.
acceptance:
  - A batch executes only after the Cashiering TL and top management approve it.
  - On execution, the UPP legacy sub-ledger decreases and other income increases by the batch total, and the items are closed.
  - A batch returned by top management does not post.
```

## Payments on legacy invoices

```fr
id: FR-DM-060
title: Apply an OTC payment to a legacy invoice
brd: [BRID 6.1 (p.9)]
actor: Cashiering User
priority: Must have
fit: CHANGE
screens: Receive payment (OTC)
api: POST /api/v1/cashiering/payments/preview; POST /api/v1/cashiering/payments
description:
  - The Cashiering User receives a payment at the counter against a legacy invoice number or reference. BIBS finds the legacy invoice in the Operations ledger, issues the AR, applies the payment by component and posts it - the cash to unapplied collections and the application from unapplied collections to the Premium Receivable legacy sub-ledger. The receipt appears in the Cash Receipts Book.
preconditions:
  - The legacy invoice is loaded and has an open balance; the user has CASH_RECEIPT.
main_flow:
  - The user enters the legacy invoice number (or ARN, policy or PN).
  - BIBS shows the invoice with the LEGACY badge and the application preview.
  - The user enters the payment details and confirms.
  - BIBS issues the AR, applies, posts and updates the invoice balances.
alternate_flows:
  - Payment above the balance. The excess becomes a new UPP item (new context) as today.
rules:
  - [R1, "Entries - AR - Dr bank / Cr unapplied collections; application - Dr unapplied collections / Cr PR legacy by component.", Configurable, "Accounting rules of OPS_AR_RECEIPT and OPS_PAYMENT_APPLY"]
  - [R2, "BIBS AR numbering applies to payments received after cutover.", Fixed, "-"]
validations:
  - [Invoice not found, "No invoice matches {reference}", "-"]
notifications:
  - "As the existing OTC flow."
audit:
  - "Receipt, application and postings are audited."
acceptance:
  - An OTC payment of 6,000.00 on a legacy invoice with 6,000.00 open issues an AR, brings the balance to 0.00 and credits the PR legacy sub-ledger.
  - The receipt is listed in the Cash Receipts Book report for the day.
```

```fr
id: FR-DM-061
title: Match autopay payments to legacy invoices
brd: [BRID 6.2 (p.9)]
actor: BIBS (payment file handlers and matching); Cashiering User
priority: Must have
fit: CHANGE
screens: Payment uploads
api: Bulk handlers PAY_BILLS, PAY_TRADE, PAY_CLPC, PAY_DIRECT_CREDIT
description:
  - Autopay payment files are uploaded as today. Payment rows that carry a legacy invoice number or reference (for example the EBIX reference of the Direct Credit file) are matched to the legacy invoice and applied as in FR-DM-060.
preconditions:
  - The legacy invoice is loaded.
main_flow:
  - The Cashiering User uploads the file.
  - BIBS matches each row, issues ARs, applies and posts by the context of each invoice.
  - The run summary shows applied, unapplied, pre-booked, excess and failed rows.
rules:
  - [R1, "The matcher tries the BIBS invoice number, the legacy invoice number, ARN, policy number and PN in the order the row carries them.", Fixed, "-"]
  - [R2, "A legacy invoice number is recognised by the patterns of MIG_LEGACY_INVOICE_NO_PATTERN (default the EBIX pattern I followed by 8 digits). The QPS pattern is added to it and to CLX_INVOICE_NO_PATTERN when DMQ11 gives the QPS format; until then a row for a QPS invoice is matched by ARN, policy number or PN, or stays unapplied.", Configurable, "Parameters MIG_LEGACY_INVOICE_NO_PATTERN, CLX_INVOICE_NO_PATTERN"]
validations:
  - [Invalid row, "As the payment file handlers", "-"]
notifications:
  - "As the existing uploads."
audit:
  - "As the existing uploads."
acceptance:
  - A Direct Credit row with an EBIX invoice reference is applied to the legacy invoice and credits the PR legacy sub-ledger.
  - A file with rows for legacy and new invoices posts each application to the right sub-ledger.
  - Before the QPS pattern is configured, a row that quotes only a QPS invoice number stays unapplied, and a row that also quotes the ARN is applied to the QPS legacy invoice.
```

## DPPR and PR2307 batch reversals

```fr
id: FR-DM-070
title: Reverse DPPR legacy invoices in batch
brd: [BRID 7.1 (p.10)]
actor: Accounting User (request); Commission TL or Comptrollership (approval)
priority: Must have
fit: CHANGE
screens: DPPR Legacy Reversal; My Approvals
api: POST /api/v1/commission/dppr-batches; .../{no}/submit; .../approve; bulk handler DPPR_LEGACY_REVERSAL
description:
  - Legacy invoices whose premium was paid directly to the insurer (DPPR) are reversed in a batch. The list comes from an upload (invoice number, amount, reason) or from the Collections "DP PR for Reversal" tags of legacy invoices. The batch is approved before it runs. Each line reverses the premium receivable against DTIP on the legacy sub-ledgers and records the reversal on the invoice.
preconditions:
  - The invoices are legacy invoices with an open premium receivable.
main_flow:
  - The Accounting User creates the batch from an upload or from the tagged invoices.
  - BIBS validates each line (invoice, amount, open balance).
  - The approver approves the batch.
  - BIBS reverses each line in its own transaction and shows the run report with posted and failed lines.
alternate_flows:
  - A line fails (for example balance changed). It is reported; the other lines post.
rules:
  - [R1, "For legacy invoices the reversal always posts to the GL, whatever DP_PR_REVERSAL_POSTING says, because the legacy premium receivable is in the GL.", Fixed, "-"]
  - [R2, "Entry - Dr DTIP legacy / Cr PR legacy; the Commission Receivable effect follows the entries Comptrollership confirms (DMQ19).", Configurable, "Accounting rules of OPS_DP_PR_REVERSAL"]
  - [R3, "The approver is never the maker.", Fixed, "-"]
validations:
  - [Not a legacy invoice, "Invoice {no} is not a legacy invoice", "-"]
  - [Amount above open PR, "The reversal of {x} is more than the open premium receivable {y}", "-"]
  - [Approver is the maker, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
notifications:
  - Submission notifies the approvers; completion notifies the maker.
audit:
  - Batch, approval, each reversal and posting are audited.
acceptance:
  - An approved batch of 20 DPPR legacy invoices reverses each open premium receivable and reduces the PR and DTIP legacy sub-ledgers by the same total.
  - A line whose balance changed after the upload fails alone and is reported.
```

```fr
id: FR-DM-071
title: Reverse PR2307 legacy invoices in batch
brd: [BRID 7.2 (p.10)]
actor: Accounting User (request); Cashiering TL or Comptrollership (approval)
priority: Must have
fit: CHANGE
screens: PR2307 Legacy Reversal; My Approvals
api: POST /api/v1/cashiering/pr2307-reversal-batches; .../{no}/submit; .../approve
description:
  - Legacy PR2307 balances (premium receivable covered by the client's BIR 2307) are reversed in an approved batch. Each line offsets the PR2307 legacy balance against DTIP legacy, first moving any amount still on premium receivable to PR2307.
preconditions:
  - The invoices are legacy invoices with an open PR2307 or 2% portion.
main_flow:
  - The Accounting User creates the batch (upload or Collections "PR 2307 for Reversal" tags).
  - BIBS validates each line.
  - The approver approves.
  - BIBS posts each line and shows the run report.
rules:
  - [R1, "Entries - Dr PR2307 legacy / Cr PR legacy (when the amount is still on PR), then Dr DTIP legacy / Cr PR2307 legacy; Commission Receivable effect per DMQ20.", Configurable, "Accounting rules of OPS_CWT_RECLASS, OPS_CWT_DTIP_OFFSET"]
  - [R2, "The approver is never the maker.", Fixed, "-"]
validations:
  - [No PR2307 balance, "Invoice {no} has no PR2307 balance to reverse", "-"]
  - [Approver is the maker, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
notifications:
  - Submission notifies the approvers; completion notifies the maker.
audit:
  - Batch, approval and postings are audited.
acceptance:
  - An approved PR2307 batch brings the PR2307 legacy balance of each line to 0.00 and reduces DTIP legacy by the same amount.
```

## Remittance

```fr
id: FR-DM-080
title: Include legacy invoices in remittance extracts
brd: [BRID 8.1 (p.10)]
actor: Remittance User
priority: Must have
fit: CHANGE
screens: Extraction workbench; Remittance batch; Remittance reports
api: As the remittance module
description:
  - The remittance extraction takes every invoice with applied premium not yet remitted - legacy or new. Premium paid in legacy before cutover counts as applied, and amounts remitted in legacy before cutover are not remitted again. The batch, schedule, commission OR and push to Disbursement work as today; the lines of legacy invoices post to the DTIP and Commission Receivable legacy sub-ledgers; the disbursement flows to the Cash Disbursements Book.
preconditions:
  - Legacy invoices are loaded with their paid and remitted amounts.
main_flow:
  - The Remittance User runs the extraction for an insurer.
  - BIBS lists legacy and new invoices with paid AR, DTIP, commission, VAT, WTAX and net due.
  - The user processes the batch as today; on approval BIBS posts each line by its invoice's context.
rules:
  - [R1, "Legacy line entry - Dr DTIP legacy (+ CWT) / Cr Commission Receivable legacy / Cr due to insurer for disbursement.", Configurable, "Accounting rules of OPS_REMITTANCE"]
  - [R2, "Schedules show the legacy invoice number and source system for legacy lines.", Fixed, "-"]
validations:
  - [Exclusion reasons, "As the remittance module (hold, pending negative adjustment, check holding, paid AR above DTIP)", "-"]
notifications:
  - "As the remittance module."
audit:
  - "As the remittance module."
acceptance:
  - A legacy invoice fully paid before cutover and not remitted appears in the next extraction for its insurer with its full net due.
  - A legacy invoice remitted in legacy does not appear.
  - The approved batch reduces DTIP legacy for legacy lines and DTIP for new lines.
```

## Endorsements on legacy invoices

```fr
id: FR-DM-090
title: Process a positive endorsement on a legacy invoice
brd: [BRID 9.1 (p.11)]
actor: Operations User (Adjustment processor); Adjustment TL
priority: Must have
fit: CHANGE
screens: New Endorsement Request; Request page; Posting batches
api: As the adjustment module (/api/v1/adjustment/requests)
description:
  - A positive financial endorsement (additional premium) of a legacy policy is processed in BIBS with the Adjustment screens. The policy header must be migrated (FR-DM-040). BIBS computes the change from the legacy original invoice, books an endorsement invoice (BIBS number) whose parent is the legacy invoice, issues the service invoice, posts the entries to the legacy sub-ledgers and keeps the before / after snapshot linked to the endorsement number.
preconditions:
  - The legacy invoice is loaded and its policy header is migrated (capability 9 is subject to the coexistence decision, DMQ22).
main_flow:
  - The user selects the legacy invoice and the endorsement type.
  - BIBS shows the recompute preview per insurer from the legacy original.
  - The request is validated, approved and posted in a batch as for BIBS invoices.
  - BIBS books the endorsement invoice with ledger context LEGACY, issues the service invoice and updates the account.
alternate_flows:
  - Header not migrated. BIBS refuses the request.
rules:
  - [R1, "The endorsement invoice of a legacy invoice has ledger context LEGACY; its booking entries use the legacy components.", Fixed, "-"]
  - [R2, "Endorsement numbering, approval, duplicate check and service invoice rules are those of the Adjustment module.", Fixed, "-"]
validations:
  - [Policy header not migrated, "Invoice {no} has no migrated policy header; it cannot be endorsed in BIBS", "-"]
notifications:
  - "As the adjustment module."
audit:
  - Request, approvals, postings and the before / after snapshot are kept with the endorsement number.
acceptance:
  - An additional premium of 2,000.00 on a legacy invoice books an endorsement invoice with the legacy invoice as parent and increases the PR and DTIP legacy sub-ledgers.
  - The service invoice is issued for the commission increase.
  - The account shows the increased premium and sum insured.
```

```fr
id: FR-DM-091
title: Process a negative endorsement on a legacy invoice
brd: [BRID 9.2 (p.11)]
actor: Operations User; Adjustment TL
priority: Must have
fit: CHANGE
screens: New Endorsement Request; Request page; Posting batches
api: As the adjustment module
description:
  - A negative financial endorsement (return premium, decrease, cancellation) of a legacy policy is processed like FR-DM-090 with a return invoice. Payments already applied are re-applied and any excess goes to UPP; if the premium was already remitted, the AR Insurer is set up; all on the legacy sub-ledgers. The account is updated, so the renewal is based on the net values.
preconditions:
  - As FR-DM-090.
main_flow:
  - As FR-DM-090 with a negative type.
  - BIBS books the return invoice, credits the service invoice (referring to the legacy service invoice where needed), re-applies payments and sets up AR Insurer where applicable.
rules:
  - [R1, "Re-application and AR Insurer set-up of a legacy invoice use the legacy components.", Fixed, "-"]
  - [R2, "For a decrease of commission, the credit refers to the legacy service invoice number held on the frozen snapshot.", Fixed, "-"]
validations:
  - [Over-adjustment (ADJID.028; the original premium is taken from the frozen snapshot of the legacy invoice), "Cumulative adjustments of {amount} exceed the baseline of {n}% of the original premium {amount}: review the previous adjustments and give the justification to proceed", ADJ_OVER_BASELINE]
notifications:
  - "As the adjustment module."
audit:
  - "As FR-DM-090."
acceptance:
  - A return premium of 1,000.00 on a legacy invoice with 1,000.00 open reduces the PR and DTIP legacy balances by 1,000.00.
  - After the endorsement the account premium is the net premium used by Renewal.
```

```fr
id: FR-DM-092
title: Process a non-financial endorsement on a legacy policy
brd: [BRID 9.3 (p.11)]
actor: Operations User; approver per the Adjustment workflow
priority: Must have
fit: CHANGE
screens: New Endorsement Request; Request page
api: As the adjustment module
description:
  - A non-financial change (for example address, mortgagee, description) of a legacy policy goes through the Adjustment approval workflow and updates the migrated account without any posting. Renewal uses the updated values.
preconditions:
  - The policy header is migrated.
main_flow:
  - The user raises the request with the changed data.
  - The request is validated and approved.
  - BIBS applies the change to the account and records the endorsement.
rules:
  - [R1, "No accounting entry for a non-financial endorsement.", Fixed, "-"]
validations:
  - [Financial field changed, "As the adjustment module (financial change under a non-financial type is refused)", "-"]
notifications:
  - "As the adjustment module."
audit:
  - "The change is kept with old and new values and the endorsement number."
acceptance:
  - An approved address change of a migrated policy is shown on the account and in the renewal candidate.
  - No journal is posted.
```

## Prod Recon visibility

```fr
id: FR-DM-100
title: Report changes made to legacy invoices
brd: [BRID 10.1 (p.11)]
actor: Prod Recon Analyst
priority: Must have
fit: NEW
screens: Reports (Operations - Production Reconciliation)
api: Report PRC-LEGACY-CHANGES
description:
  - The report lists every change made in BIBS to a legacy invoice in a period, with the original value (frozen at load), the updated value and the delta, per field and component. Changes are endorsement invoices of the invoice's family, corrections, DPPR and PR2307 reversals, write-offs, minimal balance reversals and non-financial changes; payments and remittances are included if BDOI asks (DMQ23).
preconditions:
  - The user has RECON_PROCESS.
main_flow:
  - The analyst opens the report and sets the period and filters.
  - BIBS lists the changes.
  - The analyst exports to Excel or PDF.
rules:
  - [R1, "The original value is the value frozen at load and never changes.", Fixed, "-"]
validations:
  - [Period too long, "As the report framework limits", "-"]
notifications:
  - "None."
audit:
  - The report run and export are logged.
acceptance:
  - After a positive endorsement of 2,000.00 on a legacy invoice with original premium 10,000.00, the report shows original 10,000.00, updated 12,000.00 and delta 2,000.00 with the endorsement number.
  - The report exports to Excel with the same rows.
```

## Legacy read-only and archive

```fr
id: FR-DM-110
title: Search the legacy archive
brd: [BRID 11.1 (p.11-12)]
actor: Audit / Compliance User
priority: Must have
fit: NEW
screens: Legacy Inquiry
api: GET /api/v1/legacy-inquiry/records; GET .../records/{id}; GET .../records/{id}/documents/{docId}
description:
  - Historical records not migrated into BIBS (closed invoices, receipts, remittances, endorsements, claims, renewal advices, GL journals, expired policies, documents) are kept either in the legacy system in read-only mode or in the BIBS archive. The BIBS archive is loaded before a legacy system is decommissioned (DMQ24).
  - The Legacy Inquiry screen searches the archive by client, policy or cover number, invoice, receipt, claim, dates and record type, and shows each record read only with its documents.
preconditions:
  - The user has LEGACY_INQUIRY_VIEW; archive records are loaded.
main_flow:
  - The user opens Legacy Inquiry and enters a reason for the session (when required).
  - The user searches; BIBS lists the matching records.
  - The user opens a record and its documents.
  - The user exports results (with LEGACY_INQUIRY_EXPORT).
alternate_flows:
  - Record in read-only legacy only. BIBS shows the legacy reference and the link to the legacy system.
rules:
  - [R1, "Archive records and documents are read only; no user can change or delete them.", Fixed, "-"]
  - [R2, "Exports are limited to MIG_ARCHIVE_EXPORT_MAX_ROWS rows (default 1,000).", Configurable, "Parameter MIG_ARCHIVE_EXPORT_MAX_ROWS"]
  - [R3, "Archive records are kept per the retention rules (proposed 5 years online, 15 years archive, umbrella BRD p.45).", Configurable, "Retention rules"]
validations:
  - [No criteria, Enter at least one search criterion, "-"]
  - [No reason, Enter the reason for this inquiry, "-"]
  - [Export above the limit, "The export is limited to {1,000} rows; narrow the search", "-"]
fields_screen: Legacy Inquiry
fields:
  - [Record type, List, "No", "LOV LEGACY_RECORD_TYPE", "-"]
  - [Source system, List, "No", "LOV MIG_SOURCE_SYSTEM", "-"]
  - [Client name or no., Text, "No", "-", "At least 3 characters"]
  - [Policy / cover no., Text, "No", "-", "-"]
  - [Invoice / receipt / claim no., Text, "No", "-", "-"]
  - [Date from / to, Date, "No", "-", "From not after To"]
  - [Reason, List + text, "Cond.", "LOV MIG_ACCESS_REASON", "Mandatory when MIG_LEGACY_ACCESS_REASON_REQUIRED"]
notifications:
  - "None."
audit:
  - Every search, view, download and export is written to the legacy access log (FR-DM-111).
acceptance:
  - A closed legacy invoice of 2022 is found by its invoice number and opens read only with its documents.
  - An export above the row limit is refused with the limit message; an export at the limit works.
```

```fr
id: FR-DM-111
title: Log and review access to legacy history
brd: [BRID 11.1 (p.11-12)]
actor: Compliance reviewer
priority: Must have
fit: NEW
screens: Legacy Access Log
api: GET /api/v1/legacy-inquiry/access-log; report MIG-ACCESS-LOG
description:
  - Every access to legacy history in BIBS is logged - user, time, source address, action (search, view, download, export), criteria, records and reason. The log cannot be changed or deleted. Compliance reviews it on screen and receives a monthly digest; unusual exports raise an alert.
preconditions:
  - The user has LEGACY_ACCESS_LOG_VIEW.
main_flow:
  - The reviewer opens the log and filters by user, date, action or record.
  - BIBS shows the entries; the reviewer exports them.
rules:
  - [R1, "The log is append-only.", Fixed, "-"]
  - [R2, "An alert is raised when a user exports more than MIG_ACCESS_EXPORT_ALERT_ROWS rows in a day (default 5,000).", Configurable, "Parameter MIG_ACCESS_EXPORT_ALERT_ROWS"]
validations:
  - [Access without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - Monthly digest to the reviewers; alert MIG_LEGACY_ACCESS_UNUSUAL.
audit:
  - Views of the access log are themselves logged.
acceptance:
  - A search in Legacy Inquiry creates a log entry with the user, criteria and result count.
  - No user can edit or delete a log entry.
```

## Cutover and coexistence

```fr
id: FR-DM-120
title: Plan the cutover and run rehearsals
brd: [BRID 12.1 (p.12)]
actor: Data Migration Lead; Program Manager
priority: Must have
fit: NEW
screens: Cutover
api: GET/POST /api/v1/migration/cutover-plans; .../tasks
description:
  - The cutover is run from a plan in the console - tasks with phase, owner role, dependencies, planned and actual times, status and evidence. The same plan structure is used for the four trial migrations, the dress rehearsal and the production cutover, so timings are measured before go-live. The runbook is an export of the plan.
  - In every plan the reference data and the client master are loaded and accepted first. They are the prerequisites of renewal testing, which the Drop 1 SIT and UAT run on the trial-migration data (concept paper R10, section VI).
preconditions:
  - The user has MIG_CUTOVER_MANAGE.
main_flow:
  - The lead creates a plan of kind MOCK, DRESS_REHEARSAL or PRODUCTION from the template.
  - The owners start and complete tasks; BIBS records actual times and evidence.
  - BIBS shows progress against the plan and the critical path.
  - At the end the lead records the outcome and timings.
rules:
  - [R1, "Calendar (default, BDOI timeline R9) - object decisions and layouts by 30 October 2026; Trial migrations 1 and 2 in SIT (April and July 2027); Trial migrations 3 and 4 in UAT (August and October 2027); dress rehearsal November 2027; December soft close in legacy by about 20 December 2027; pre-load of reference data and clients T-14 days; map freeze T-7 days; last legacy business day 29 December 2027; business freeze after the last legacy EOD (31 December 2027 for the recommended go-live of 3 January 2028); go-live renewal extraction at T 04:00; FY2027 true-ups from T+15 to T+120.", Configurable, "Plan template"]
  - [R4, "Trial-migration load order - the tasks of objects R01-R07 and C01-C03 come first in every plan, and no policy, open-item or GL load starts before they are accepted (G6).", Fixed, "-"]
  - [R2, "The dress rehearsal must finish within the cutover window with at least 20 percent margin.", Configurable, "Go / no-go criteria"]
  - [R3, "Non-production rehearsals use masked data only.", Fixed, "-"]
validations:
  - [Task started before its dependency, "Task {task} depends on {dependency}, which is not complete", "-"]
notifications:
  - Task owners are notified when their task can start.
audit:
  - Every task change is audited.
acceptance:
  - A trial-migration plan records planned and actual times per task and per object load.
  - The runbook export lists every task with owner and timing.
  - In a trial-migration plan, the load of P01 cannot start while C01 is not accepted.
```

```fr
id: FR-DM-121
title: Decide go / no-go on measured criteria
brd: [BRID 12.1 (p.12)]
actor: Go / no-go board
priority: Must have
fit: NEW
screens: Cutover (Go / No-go tab)
api: GET /api/v1/migration/cutover-plans/{id}/gonogo; POST .../gonogo/decision
description:
  - Go-live is decided on criteria that BIBS measures - all Day-1 objects accepted, counts and amounts reconciled, zero financial rejects, Migration Clearing 0.00, legacy control accounts equal to their sub-ledgers, empty client review queue, business smoke test passed, rollback snapshot taken, hypercare in place, the preliminary trial balance signed as the provisional opening with the legacy GL restricted to the named FY2027 adjustment users, and, for Renewal, the headers of every expiry up to 31 May 2028 loaded, the RA-sent file loaded and the January staffing plan confirmed. The board records the decision with the values it saw.
preconditions:
  - The production plan has reached the go / no-go task.
main_flow:
  - BIBS computes the criteria and shows each with its value and status.
  - The board decides GO or NO-GO with a comment.
  - On NO-GO the fallback task starts - the database is restored to the rollback snapshot and legacy is reopened.
rules:
  - [R1, "GO is possible only when every mandatory criterion is met; a board member may record a waiver of a non-mandatory criterion with a reason.", Configurable, "Go / no-go criteria"]
  - [R2, "Point of no return - end of the first business day after go-live (DMQ32).", Configurable, "Cutover plan"]
validations:
  - [GO with an unmet mandatory criterion, "Criterion {criterion} is not met", "-"]
notifications:
  - The decision is sent to the distribution list of the plan.
audit:
  - The decision with every criterion value is kept.
acceptance:
  - GO cannot be recorded while Migration Clearing is not 0.00.
  - GO cannot be recorded while the preliminary trial balance is not signed as the provisional opening.
  - The decision record shows each criterion value at the time of the decision.
```

```fr
id: FR-DM-122
title: Transition renewals by RMEL cohort and track the run-off
brd: [BRID 12.1 (p.12), BRID 4.1 (p.8)]
actor: Program Manager; Renewal (BIBS module); Marketing
priority: Must have
fit: NEW
screens: Run-off and Decommissioning; Renewal screens
api: GET /api/v1/migration/runoff; report MIG-RUNOFF
description:
  - Renewal in BIBS is enabled from go-live. Policies expiring before go-live renew or lapse in legacy. The policies expiring from go-live to 31 May 2028 are extracted by BIBS at go-live from the migrated headers (FR-DM-124); later expiries are extracted by BIBS from the migrated headers on the normal lead time. A legacy policy renews on the new-business path pre-filled from its header, or as is on the package that the Renewal sanitation resolves (FR-DM-034), and the new BIBS account refers to the legacy policy.
  - The run-off tracker shows, per expiry month, how many legacy policies were in force at go-live, renewed in BIBS, not renewed, lapsed or still open.
preconditions:
  - Headers and the RA-sent file are loaded; the Renewal module is live.
main_flow:
  - At go-live, the go-live extraction creates the candidates of the expiries up to 31 May 2028 with source LEGACY (FR-DM-124).
  - Each day, Renewal extraction asks for migrated headers expiring at the lead date.
  - Each month, BIBS refreshes the run-off tracker.
rules:
  - [R1, "Daily extraction after go-live - migrated headers expiring at the business date + RNW_EXTRACTION_LEAD_DAYS (default 140); headers that already have a candidate from the go-live extraction are skipped, so the first new expiry is 1 June 2028, extracted on 13 January 2028.", Configurable, "Parameter RNW_EXTRACTION_LEAD_DAYS"]
  - [R2, "A renewal of a legacy policy refers to the legacy reference.", Fixed, "-"]
validations:
  - [Extraction of an expiry before go-live, "Expiry {date} is before go-live; the policy renews in legacy", "-"]
notifications:
  - "As Renewal."
audit:
  - "As Renewal; the tracker keeps monthly snapshots."
acceptance:
  - A legacy policy expiring 60 days after go-live appears in BIBS Renewal at go-live from the go-live extraction.
  - A legacy policy expiring 200 days after go-live is extracted by BIBS at 140 days before expiry.
  - The run-off report shows renewed, not renewed and open counts per expiry month.
```

```fr
id: FR-DM-123
title: Decommission legacy systems on agreed criteria
brd: [BRID 12.1 (p.12), BRID 11.1 (p.11-12)]
actor: Program Manager; system owners; Compliance; Comptrollership
priority: Must have
fit: NEW
screens: Run-off and Decommissioning
api: GET/POST /api/v1/migration/decommissioning
description:
  - Each legacy system has a decommissioning checklist with measurable criteria and sign-offs. A second checklist closes the legacy context in BIBS when the legacy positions have run off.
preconditions:
  - The user has MIG_CUTOVER_MANAGE (checklist) or MIG_SIGNOFF (sign-off).
main_flow:
  - The Program Manager opens the checklist of a system.
  - Owners attach evidence and mark each criterion met.
  - The system owner, Compliance and Comptrollership sign.
rules:
  - [R1, "Legacy system decommissioned - default criteria of the template (names as shown on screen and in messages) - Final extracts reconciled; Final true-up reconciled (EBIX and ISYS, which hold the GL - final true-up signed against the audited FY2027 trial balance and legacy GL locked); Archive reconciled (against legacy); Legacy Inquiry verified (by Audit / Compliance); No open item needs the system (including the last legacy policy expired); Claims tail covered; Access logs archived; Retention covered; Owners signed.", Configurable, "Checklist template (Cutover Runbook section 9)"]
  - [R2, "Legacy context closed in BIBS - criteria No open legacy invoice; No legacy UPP balance; Legacy accounts at zero (legacy control accounts and Migration Clearing at 0.00); Chart decision recorded.", Fixed, "-"]
validations:
  - [Sign-off with an unmet criterion, "Criterion {criterion} is not met", "-"]
notifications:
  - Sign-off requests go to the signers.
audit:
  - Evidence and sign-offs are kept.
acceptance:
  - A system cannot be marked decommissioned while its archive reconciliation is open ("Criterion Archive reconciled is not met").
  - The legacy context cannot be closed while a legacy UPP has a balance ("Criterion No legacy UPP balance is not met").
  - EBIX cannot be marked decommissioned while the final true-up is not signed ("Criterion Final true-up reconciled is not met").
```

```fr
id: FR-DM-124
title: Renew the January to May 2028 expiries in BIBS from go-live
brd: [BRID 12.1 (p.12), BRID 4.1 (p.8)]
actor: Renewal (BIBS module); Renewal processing team; Data Migration Lead
priority: Must have
fit: NEW
screens: Renewal candidates (day-1 worklist); Reconciliation
api: LegacyPolicySource (go-live extraction); report MIG-RENEWAL-GOLIVE
description:
  - The renewals of the policies expiring from go-live to 31 May 2028 are processed in BIBS after go-live; no renewal candidate is carried from legacy (DMQ37, answered 26 September 2026). At go-live, before business opens, BIBS extracts every migrated policy header expiring in this window into the renewal pipeline, prioritised by expiry date, with the January expiries flagged urgent.
  - A renewal advice already sent by hand before go-live is recorded on the candidate from the RA-sent file (FR-DM-125) and is not sent again. A header whose cover has a later term booked in legacy that starts on its expiry is not extracted, because it is already renewed.
  - The January expiries get days to four weeks instead of 140 days. The day-1 priority queue, a staffing plan for January, and a Renewal team trained and rehearsed on the Trial migration 4 and dress-rehearsal data before go-live mitigate this (Cutover Runbook).
preconditions:
  - The go / no-go decision is GO.
  - The policy headers (P01) include every policy expiring from go-live to 31 May 2028 and every renewal term booked in legacy that starts on or after go-live (FR-DM-040); the RA-sent file is loaded (FR-DM-125).
main_flow:
  - At go-live 04:00 the Renewal go-live extraction runs.
  - BIBS creates a candidate with source LEGACY for each header in the window not renewed in legacy - at the first stage, with the expiry date as priority, the flag URGENT when the expiry is on or before 31 January 2028, and the RA already sent when the RA-sent file has a row.
  - BIBS runs the extraction check per expiry month (MIG-RENEWAL-GOLIVE).
  - From 08:00 the Renewal processing team works the day-1 worklist, urgent January expiries first.
alternate_flows:
  - Daily extraction. From the day after go-live the daily Renewal extraction skips headers that already have a candidate; the first new expiry is 1 June 2028, extracted on 13 January 2028.
  - Rerun. A rerun of the go-live extraction creates no duplicate candidate.
rules:
  - [R1, "Go-live extraction window - expiries from the go-live date to MIG_GOLIVE_RENEWAL_TO (default 31 May 2028).", Configurable, "Parameters MIG_CUTOVER_DATE, MIG_GOLIVE_RENEWAL_TO"]
  - [R2, "Candidates expiring on or before MIG_RENEWAL_URGENT_TO (default 31 January 2028) are flagged URGENT; the worklist is ordered by expiry date.", Configurable, "Parameter MIG_RENEWAL_URGENT_TO"]
  - [R3, "An RA recorded as already sent is not sent again; a revised RA is a user action.", Fixed, "-"]
  - [R4, "A header whose cover has a later migrated term starting on its expiry is not extracted.", Fixed, "-"]
  - [R5, "Extraction check per expiry month - headers expiring in the window = candidates created + headers renewed in legacy; RA-sent rows loaded = candidates with the RA already sent.", Fixed, "-"]
validations:
  - [Extraction check break, "{month} - {x} headers expiring, {y} candidates, {z} renewed in legacy", "-"]
notifications:
  - A break raises alert MIG_RECON_BREAK to the Renewal processing team and the Data Migration Lead.
audit:
  - Each candidate keeps the legacy reference, the extraction run and, when given, the RA-sent batch.
acceptance:
  - At go-live the header C-2027-004512-01, expiring on 15 January 2028, is a candidate flagged URGENT among the January expiries at the top of the worklist in expiry-date order; its RA sent on 16 November 2027 is recorded and no RA is queued for it.
  - A header expiring on 31 May 2028 is extracted at go-live; a header expiring on 1 June 2028 is not, and the daily extraction takes it on 13 January 2028.
  - The header C-2027-004600-01, whose renewal term C-2027-004600-02 was booked in legacy, is not extracted.
  - For each month January to May 2028 MIG-RENEWAL-GOLIVE balances.
```

```fr
id: FR-DM-125
title: Load the renewal advices already sent before go-live
brd: [BRID 12.1 (p.12)]
actor: Renewal processing team (maker and checker); Migration Operator; Head of the Renewal processing team (data owner)
priority: Must have
fit: NEW
screens: Extracts; Batches (Issues tab); Resubmissions; Reconciliation
api: Batch of object P03; POST /api/v1/migration/batches/{no}/resubmissions; POST .../resubmissions/{id}/approve, .../return; report MIG-REJECTS
description:
  - The RMEL and the dispositions are kept in Excel trackers today (DMQ38, answered 26 September 2026). The migration takes one thing from them - the renewal advices (RAs) already sent by hand before go-live for the expiries from go-live to 31 May 2028 - so that BIBS does not send them again. The trackers themselves are not migrated.
  - The Renewal processing team compiles the RAs in the Excel template of layout P03, one row per expiring term (legacy policy reference, cover, expiry, RA date and reference, channel, recipient, proposed insurer and premium quoted, sender, tracker and sheet). The rows are validated; rejected rows come back in an Excel rejection report with correction columns; the maker corrects them and the checker approves the resubmission. The Head of the Renewal processing team owns the object.
preconditions:
  - The policy headers (P01) of the same load are loaded.
main_flow:
  - The maker compiles the file from the trackers; the checker compares it with the trackers (row counts per tracker and 10 sample rows) and releases it.
  - The operator uploads the file with its control file and validates the batch.
  - BIBS sends MIG-REJECTS of P03 to the maker and the checker.
  - The maker corrects each rejected row, fills the correction columns and prepares a resubmission file with the corrected rows only.
  - The checker reviews each correction against the tracker and approves the resubmission; the operator loads it as a rerun batch.
  - After the load approval (G4) BIBS loads the valid rows; the go-live extraction records them on the candidates (FR-DM-124).
alternate_flows:
  - Return. The checker returns the resubmission with a reason; the maker corrects it.
  - Deadline passed. Rows still rejected at the deadline are not loaded; they are listed for the day-1 queue, and the processor checks the tracker before sending an RA for such a policy.
  - Trial migrations. The file is sent in every trial migration from Trial migration 1; in production once, with the RAs sent up to the last legacy business day.
rules:
  - [R1, "Rows are accepted for expiries from the go-live date to MIG_GOLIVE_RENEWAL_TO (default 31 May 2028) whose header is in P01 and was not renewed in legacy.", Configurable, "Parameters MIG_CUTOVER_DATE, MIG_GOLIVE_RENEWAL_TO"]
  - [R2, "The RA date is not after the last legacy business day; an RA date more than 140 days before the expiry is a warning.", Fixed, "-"]
  - [R3, "A resubmission is approved by a checker who is not its maker; the checker holds MIG_RESUBMIT_APPROVE for object P03.", Fixed, "-"]
  - [R4, "In production, resubmissions are accepted until MIG_RESUBMIT_DEADLINE (default T-1 12:00).", Configurable, "Parameter MIG_RESUBMIT_DEADLINE"]
validations:
  - [Header not in P01, "Cover {no} has no migrated header", "-"]
  - [Expiry outside the window or different from the header, "Expiry {date} is outside the go-live renewal window or differs from the header", "-"]
  - [RA date after the last business day, "RA date {date} is after the last legacy business day", "-"]
  - [RA date early, "RA date {date} is more than 140 days before expiry {expiry}; check the tracker", "-"]
  - [Already renewed in legacy, "Cover {no} was already renewed in legacy as {ref}; remove the row", "-"]
  - [Checker is the maker, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
  - [Deadline passed, "Resubmissions closed at {deadline}", "-"]
fields_screen: Rejection report (Excel) and resubmission
fields:
  - [Row, Number, "-", "-", "Row of the file"]
  - [Legacy policy reference, Text, "-", "-", "As in the file"]
  - ["Column, value, rule and message", Text, "-", "-", "From the validation"]
  - [Correction, Text, "Yes", "-", "Filled by the maker - the new value, or Remove row"]
  - [Corrected by and date, Text, "Yes", "-", "Maker"]
  - [Checked by, Text, "Yes", "-", "Checker; recorded on approval in the console"]
notifications:
  - MIG-REJECTS of P03 is sent to the maker and the checker after each validation; a submitted resubmission goes to the checkers in My Approvals.
audit:
  - Each loaded row keeps its tracker, batch and resubmission; corrections and approvals are kept.
acceptance:
  - A file with the RA of C-2027-004512-01 sent on 16 November 2027 by e-mail loads, and at go-live the candidate shows the RA as already sent.
  - A row whose cover C-2027-009999 has no header, a row expiring on 15 June 2028 and a row with an RA date of 30 December 2027 are rejected with their messages and appear in the rejection report with the correction columns.
  - A resubmission prepared and approved by the same user is refused; approved by the checker, it loads as a rerun batch.
  - A row still rejected at the deadline is not loaded and is listed for the day-1 queue.
```

# Workflow and status model

## Data object and batch states

<!-- table: widths=3.2,4.4,9 caption="States of the migration records" size=8.5 -->
| Record | State | Meaning |
|---|---|---|
| Data object | PROPOSED | Registered with criteria; decision not submitted |
| Data object | FOR_DECISION | Decision submitted to the business owner |
| Data object | DECIDED | Class approved (G1) |
| Data object | READY | Layout frozen and code maps approved (G2) |
| Data object | ACCEPTED | Loaded, reconciled and accepted in the environment (G6) |
| Extract | RECEIVED, CHECKED, REJECTED | Intake checks of FR-DM-010 |
| Extract | STAGED, PURGED | Rows in staging; payloads purged after the retention |
| Batch | PLANNED | Created with its extracts |
| Batch | VALIDATED | Rows mapped and validated (G3) |
| Batch | LOADING | Load approved (G4) and running |
| Batch | LOADED, LOADED_WITH_REJECTS, FAILED | Load result |
| Batch | RECONCILED | L1-L5 run; breaks explained (G5) |
| Batch | SIGNED_OFF | Accepted (G6) |
| Batch | ROLLING_BACK, ROLLED_BACK | Rollback of FR-DM-015 |

## Approval workflows

<!-- table: widths=4.2,5.6,3.4,3.4 caption="Approval workflows" size=8.5 -->
| Workflow | Stages | Maker permission | Approver permission |
|---|---|---|---|
| MIG_OBJECT_DECISION | PROPOSED, FOR_DECISION, DECIDED | MIG_OBJECT_MANAGE | MIG_DECISION_APPROVE |
| MIG_MAP_VERSION | DRAFT, SUBMITTED, APPROVED, SUPERSEDED | MIG_MAPPING_EDIT | MIG_MAPPING_APPROVE |
| MIG_BATCH_ROLLBACK | REQUESTED, APPROVED, DONE, REJECTED | MIG_ROLLBACK_REQUEST | MIG_ROLLBACK_APPROVE |
| OPS_UPP_INCOME_RECLASS | DRAFT, FOR_TL_APPROVAL, FOR_TOP_MANAGEMENT, EXECUTED | CASH_UPP_INCOME_REQUEST | CASH_DISPOSITION_APPROVE, then CASH_UPP_INCOME_APPROVE |
| OPS_DPPR_REVERSAL | DRAFT, FOR_APPROVAL, APPROVED, POSTED | LEGACY_REVERSAL_REQUEST | LEGACY_REVERSAL_APPROVE |
| OPS_PR2307_REVERSAL | DRAFT, FOR_APPROVAL, APPROVED, POSTED | LEGACY_REVERSAL_REQUEST | LEGACY_REVERSAL_APPROVE |
| MIG_OPENING_TRUEUP | PREPARED, FOR_APPROVAL, APPROVED, POSTED | MIG_TRUEUP_PREPARE | MIG_TRUEUP_APPROVE |
| MIG_RESUBMISSION | PREPARED, APPROVED, RETURNED | MIG_DQ_RESOLVE | MIG_RESUBMIT_APPROVE |

Every approval stage allows a return with a reason, which sends the record back to the maker's stage.

## Sign-off gates

<!-- table: widths=2.6,6,4,4 caption="Sign-off gates" size=8.5 -->
| Gate | What is signed | Signer | Evidence |
|---|---|---|---|
| G1 | Class of the object | Business owner | Decision record |
| G2 | Code map versions and layout | Business owner | Map versions |
| G3 | Validation result and waivers | Data Steward; waivers by the owner | MIG-DQ-ISSUES |
| G4 | Approval to load | Data Migration Lead | Batch counts |
| G5 | Reconciliation | Reconciliation approver | MIG-RECON-SUMMARY, MIG-GL-CLEARING |
| G6 | Object accepted | Business owner and Data Migration Lead | Sample checks on screen |
| G7 | Go-live | Go / no-go board | Go / no-go criteria |

# Reports and documents

<!-- table: widths=4.2,5.4,5,2 caption="Data Migration reports" size=8.5 -->
| Code | Name | Content | BRID |
|---|---|---|---|
| MIG-OBJECT-REGISTER | Data Object Register | Objects, class, criteria, owners, status per environment | 1.1a |
| MIG-DECISIONS | Migration Decisions | Every decision with submitter, approver and dates | 1.1a |
| MIG-UNMAPPED-CODES | Unmapped Codes | Set, source, legacy code, rows, sample keys | 3.1 |
| MIG-MAP-VERSIONS | Code Map Versions | Versions, entries, approvers, batches that used them | 3.1 |
| MIG-DQ-ISSUES | Data-Quality Issues | Issues per rule, severity and resolution | 1.1b |
| MIG-REJECTS | Rejected Rows | Rejected and invalid rows with messages | 1.1b |
| MIG-BATCH-LOG | Batch Log | Batches with steps, counts and timings | 1.1b |
| MIG-RECON-SUMMARY | Reconciliation Summary | Object by level L1-L5 with status | 1.1b |
| MIG-RECON-DETAIL | Reconciliation Detail | Lines, breaks, explanations and approvals | 1.1b |
| MIG-GL-CLEARING | Migration Clearing and Legacy Control Accounts | Clearing balance per branch and currency; legacy control vs sub-ledger | 1.1b |
| MIG-CLIENT-MATCH | Client Matching | Clusters, scores, decisions, survivor values | 2.1 |
| MIG-SIGNOFF-STATUS | Sign-off Status | Gates per object and batch | 1.1a |
| MIG-CUTOVER-STATUS | Cutover Status | Tasks with planned and actual times | 12.1 |
| MIG-GONOGO | Go / No-go | Criteria, values, decision | 12.1 |
| MIG-RUNOFF | Legacy Run-off | Legacy in force by expiry month and outcome | 12.1 |
| MIG-RENEWAL-GOLIVE | Go-live Renewal Extraction | Per expiry month January-May 2028 - headers, candidates, renewed in legacy, RA already sent, urgent | 12.1 |
| MIG-TRUEUP-RECON | True-up Reconciliation | Per true-up - cut-off, movement, balance, clearing and opening-period checks | 1.1b |
| MIG-TRUEUP-REGISTER | True-up Register | True-ups with their legacy journals, register references, approvals and postings | 1.1b |
| MIG-LEGACY-POSITIONS | Legacy Open Positions | Open legacy invoices and UPP by component, insurer, client and age | 5.1-8.1 |
| MIG-ACCESS-LOG | Legacy Access Log | Access to legacy history | 11.1 |
| PRC-LEGACY-CHANGES | Legacy Invoice Changes | Original, updated and delta per change | 10.1 |
| CSH-UPP-LEGACY | Legacy UPP | Legacy UPP by age, status and client | 5.1 |
| CSH-UPP-INCOME-RECLASS | UPP Reclassified to Income | Batches, items, approvers, amounts | 5.5 |

All reports export to Excel and PDF. The MIG reports need MIG_VIEW; PRC and CSH reports follow the Operations report permissions; MIG-ACCESS-LOG needs LEGACY_ACCESS_LOG_VIEW.

**Documents.** The data requirements workbook (layouts, formats, control file, code map templates, and the Excel template of the RA-sent file) and the cutover runbook are generated from BIBS (layouts and cutover plan) in Excel and Word.

# Interfaces and integration

Figure 4 shows the interfaces of the migration module. It writes into BIBS only through the services of the owning modules and serves the Renewal and Customer Servicing modules through their ports.

![Interfaces of the migration module (dashed = parked seam or link only)](figures/brd13_integration.dot)

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Extract upload (console) | In | Data and control files per object | 1.1b | NEW |
| SFTP drop from BDOI IT | In | Same files, picked up by a job | 1.1b | PARKED |
| Reference masters (LOV, catalogue, sales organisation) | Out | Mapped and created values | 3.1 | NEW |
| Client master and screening | Out | Migrated clients; one full screening run | 2.1 | NEW |
| Accounts | Out | Migrated policy headers | 4.1 | NEW |
| Operations ledger and Cashiering | Out | Legacy invoices, UPP | 5.1-10.1 | NEW |
| GL | Out | Opening entries, provisional opening trial balance and FY2027 true-ups | 1.1b | NEW |
| Renewal (legacy policy source; package map) | Out | Migrated headers for the go-live and daily extractions; renewal advices already sent; PACKAGE map for the sanitation check | 12.1 | NEW |
| Customer Servicing (legacy account lookup) | Out | Legacy references and archive records | 11.1 | NEW |
| Legacy read-only systems | Link | Link and legacy reference only | 11.1 | PARKED |
| Write-back to QPS / EBIX | Out | Not needed after the freeze (legacy read-only) | - | OUT |

> [!PARKED] Parked seams
> The SFTP drop, automated delta extracts, bulk transfer of legacy documents and the legacy read-only links depend on BDOI IT (DMQ24, DMQ28). BIBS works with console uploads until then; adding a transport is a configuration of the intake, with no change to the checks.

# Non-functional requirements

The BRD refers every usage table to "the consolidated NFR requirements for BDO Insure Core Modernization project" (p.13-14), which is not in the pack (DMQ29). The values below are proposals.

<!-- table: widths=3,5.4,5.6,2.6 caption="Non-functional requirements" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Volumes | Not given | Loaders sized for 1,000,000 client rows and 500,000 open-item rows; planning bounds from the umbrella BRD (clients 2020 to present, p.43; 21,200 bookings and 25,800 renewal accounts a month, p.44-45) | OPEN |
| Cutover window | Not given | 48 hours over a weekend from the last legacy EOD to go / no-go; BIBS open 08:00 on the first business day; at the year-end boundary (recommended 3 January 2028), with FY2027 true-ups until about April 2028 | OPEN |
| Load performance | Not given | At least 50,000 rows an hour per partition, 4 partitions; reconciliation of an object within 1 hour; full production load within 20 hours, proven in the dress rehearsal | NEW |
| Staging security | Hosting appendix | Masked data outside production; staging and files purged within 5 days of sign-off; access only for migration roles and from the Philippines; encryption at rest and in transit | NEW |
| Audit | Not given | Every intake, validation, load, rerun, rollback, reconciliation, sign-off and archive access is audited | NEW |
| Retention | Consolidated NFR; umbrella p.45 (5 years online, 15 years archive) | Archive and access log by retention rule; migration evidence kept as project records (proposed 10 years) | OPEN |
| Availability and recovery | Consolidated NFR | Same as BIBS; database snapshot before each production load gate | FIT |

# Configuration items

## Parameters

<!-- table: widths=7.4,3,6.2 caption="Data Migration parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| MIG_ENVIRONMENT_CLASS | NON_PRODUCTION | Masking applies unless PRODUCTION |
| MIG_CUTOVER_DATE | - | Go-live date; start of the go-live renewal extraction window |
| MIG_OPENING_VALUE_DATE | 1 January 2028 | Value date of the opening entries and of the true-ups (first day of the opening period) |
| MIG_GOLIVE_RENEWAL_TO | 31 May 2028 | Last expiry of the go-live renewal extraction |
| MIG_RENEWAL_URGENT_TO | 31 January 2028 | Candidates expiring up to this date are flagged URGENT |
| MIG_RESUBMIT_DEADLINE | T-1 12:00 | Last approval time of a resubmission in production |
| MIG_STAGING_RETENTION_DAYS | 5 | Days after sign-off before staging and files are purged |
| MIG_CHUNK_SIZE | 500 | Rows per load transaction |
| MIG_PARTITIONS | 4 | Parallel load partitions |
| MIG_AMOUNT_TOLERANCE | 0.00 | Tolerance of amount reconciliation |
| MIG_MAX_ERROR_RATE_MASTER | 0.5 | Maximum percent of rejected or waived master-data rows |
| MIG_MAX_ERROR_RATE_FINANCIAL | 0 | Maximum percent of rejected financial rows |
| MIG_CLIENT_MATCH_AUTO | 90 | Score from which clients merge automatically |
| MIG_CLIENT_MATCH_REVIEW | 60 | Score from which a pair goes to review |
| MIG_INVOICE_NO_COLLISION_PREFIX | true | Prefix the source system when a legacy invoice number is already used |
| MIG_LEGACY_INVOICE_NO_PATTERN | EBIX pattern (I and 8 digits) | Patterns by which the payment matcher recognises a legacy invoice number; the QPS pattern is added after DMQ11 |
| MIG_UPP_ISSUE_AR | false | Issue a BIBS AR (without cash posting) for each migrated UPP (DMQ14) |
| MIG_ARCHIVE_EXPORT_MAX_ROWS | 1000 | Rows per archive export |
| MIG_ACCESS_EXPORT_ALERT_ROWS | 5000 | Exported rows per user and day that raise an alert |
| MIG_LEGACY_ACCESS_REASON_REQUIRED | true | A reason is required per Legacy Inquiry session |
| MIG_LEGACY_LINK_EBIX, MIG_LEGACY_LINK_QPS | - | Links to the read-only legacy systems |
| CMR_INCENTIVE_INCLUDE_LEGACY | false | Legacy invoices count in incentive runs |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| MIG_SOURCE_SYSTEM | EBIX; QPS; ISYS; Excel; CMS |
| MIG_OBJECT_CATEGORY | Reference; Client; Policy; Open item; GL; History |
| MIG_TRUST_LEVEL | High; Medium; Low |
| MIG_BREAK_REASON | Rounding at source; Record excluded by owner; Mapping difference; Legacy data error; Late legacy transaction; Other (see explanation) |
| MIG_WAIVER_REASON | Duplicate in source; Obsolete record; Corrected manually after go-live; Other (see reason) |
| MIG_ACCESS_REASON | Audit request; Regulatory inquiry; Client request; Claim support; Internal investigation; Other (see text) |
| LEGACY_RECORD_TYPE | Client; Policy; Invoice; Receipt; Remittance; Endorsement; Claim; GL journal; Renewal advice; Letter; Other |
| UPP_INCOME_REASON | Client cannot be identified; Unclaimed after follow-up; Below refund threshold; Other (see remarks) |

## Masters and rules maintained by the business

<!-- table: widths=4.4,4.6,7.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (approved by) | FR |
|---|---|---|
| Data object register and decisions | Data Migration Lead (business owner) | FR-DM-001, 002 |
| Layouts, validation rules, masking rules | Data Steward (Data Migration Lead) | FR-DM-010, 013 |
| Code maps | Data Steward (business owner) | FR-DM-011 |
| PACKAGE map (loaded for the Renewal sanitation) | TSU (Product Owner, Marketing Business System) | FR-DM-034 |
| RA-sent file template and rejected-row review | Renewal processing team, maker (checker of the team; owner the Head of the Renewal processing team) | FR-DM-125 |
| FY2027 adjustment register and the legacy GL access list | Comptrollership GL lead (Head, Comptrollership) | FR-DM-023, 024 |
| Matching and survivorship rules | Data Steward (business owner of clients) | FR-DM-031 |
| Legacy control accounts, Migration Clearing and the legacy accounting rule lines | Comptrollership (maker-checker on accounting rules) | FR-DM-021, 050-080 |
| Go / no-go criteria and cutover plan template | Data Migration Lead (Program Manager) | FR-DM-120, 121 |
| Decommissioning checklist template | Program Manager | FR-DM-123 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-DM-01 | The draft BRD v0.01 is the baseline until a signed version replaces it | R1 |
| A-DM-02 | BRID 1.1 (p.7) is two requirements, numbered 1.1a and 1.1b here | R1 p.7 |
| A-DM-03 | Legacy open items are processed only in BIBS from the freeze; legacy becomes read-only | p.5, DMQ22 |
| A-DM-04 | Legacy sub-ledgers are separate GL control accounts with a Migration Clearing account | DMQ18 |
| A-DM-05 | Endorsements of legacy invoices require the policy header to be migrated | DMQ09, DMQ22 |
| A-DM-06 | DPPR means direct-payment premium receivable | DMQ19 |
| A-DM-07 | Go-live is in January 2028 (BDOI timeline) on Monday 3 January 2028, at the year-end boundary (option A, recommended; awaiting Comptrollership confirmation) | DMQ25, DMQ39 |
| A-DM-08 | No BIBS AR is issued for a migrated UPP at load | DMQ14 |
| A-DM-09 | There is one production cut-over; the early renewal release of the concept paper is superseded and BIBS is not used in production before go-live | R10; DCR-240 |
| A-DM-10 | The renewals of the January-May 2028 expiries are processed in BIBS after go-live from a go-live extraction; no renewal is carried (BDOI answer) | DMQ37 |
| A-DM-11 | Legacy packages are remapped at Renewal sanitation; the migration loads the PACKAGE map only (BDOI answer) | DMQ36 |
| A-DM-12 | BIBS opens with a provisional balance-sheet opening; the FY2027 P&L is not opened, and FY2027 BIR returns and the FY2027 audit use legacy | DMQ39 |
| A-DM-13 | After the freeze the legacy GL accepts only FY2027 closing and audit adjustments by named Comptrollership users, until the final true-up | DMQ39 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-DM-01 | BDOI supplies the data object inventory, owners, volumes and extracts in the agreed layouts | All FRs (DMQ01, DMQ28) |
| D-DM-02 | Comptrollership sets up the legacy control accounts, Migration Clearing and the legacy rule lines | FR-DM-021, 050-091 (DMQ18) |
| D-DM-03 | BDOI agrees the client dedupe keys and survivorship | FR-DM-031 (DMQ04) |
| D-DM-04 | The Renewal module is in SIT for Trial migration 1 (April 2027) and live at go-live, with the go-live extraction (priority by expiry date, URGENT flag, RA already sent) and the sanitation check PACKAGE_REMAP with the Exception bucket | FR-DM-034, 122, 124, 125 |
| D-DM-05 | BDOI IT keeps the legacy systems read-only with access logging until the archive is loaded | FR-DM-110 (DMQ24) |
| D-DM-06 | The account business-type change (BT0) is built before the header load | FR-DM-040 |
| D-DM-07 | TSU maintains the BIBS packages and the PACKAGE map in time for Trial migration 2 (June 2027) | FR-DM-034 (DMQ36) |
| D-DM-08 | The Renewal processing team compiles the RAs already sent from its Excel trackers in the P03 template, reviews the rejects with a maker and a checker, and is staffed and trained for the January expiries | FR-DM-124, 125 (DMQ38) |
| D-DM-09 | Comptrollership confirms option A by M6 (1 October 2027), completes the December soft close by about 20 December 2027, keeps the FY2027 adjustment register and restricts the legacy GL after the freeze | FR-DM-022 to 024 (DMQ39) |
| D-DM-10 | The external audit of FY2027 is timed so that the audited financial statements are available by about April 2028 for the final true-up | FR-DM-023, 024 |

## Open questions

<!-- table: widths=1.4,9.6,2.8,2.9 caption="Open questions on BRD-13 (baseline R2 section 7)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| DMQ01 | Data object inventory, owners and classes | FR-DM-001, 002 | OPEN |
| DMQ02 | Rating of the four criteria and the rule that sets the class | FR-DM-002 | OPEN |
| DMQ03 | Source systems in scope and system of record per object | FR-DM-001 | OPEN |
| DMQ04 | Client dedupe keys, survivorship, review ownership | FR-DM-031 | OPEN |
| DMQ05 | Client scope (2020 to present; active only?) | FR-DM-032 | PARTIAL |
| DMQ06 | Screening of migrated clients | FR-DM-032 | OPEN |
| DMQ07 | KYC documents - migrate or archive | FR-DM-032, 110 | OPEN |
| DMQ08 | Code map ownership and MIS fields | FR-DM-011, 030 | OPEN |
| DMQ09 | Condition and fields of the in-force header | FR-DM-040, 090 | OPEN |
| DMQ10 | Scope of legacy invoices | FR-DM-050 | OPEN |
| DMQ11 | Invoice number uniqueness and QPS format | FR-DM-050, 060, 061 | OPEN |
| DMQ12 | Component and share breakdown from legacy | FR-DM-050 | OPEN |
| DMQ13 | Commission realisation state | FR-DM-050, 052 | OPEN |
| DMQ14 | Receipt for migrated UPP; status mapping | FR-DM-051 | OPEN |
| DMQ15 | UPP match references; rerun frequency | FR-DM-052 | OPEN |
| DMQ16 | Top management, criteria and account for reclassification to income | FR-DM-055 | OPEN |
| DMQ17 | Refund approvals for legacy UPP | FR-DM-054 | OPEN |
| DMQ18 | Legacy control accounts, clearing, end state, GL trial balance, GL ageing | FR-DM-021 | OPEN |
| DMQ19 | DPPR definition, entries, list source, approver | FR-DM-070 | OPEN |
| DMQ20 | PR2307 reversal entries, evidence, approver | FR-DM-071 | OPEN |
| DMQ21 | Remittance batches, holds and special remittances at cutover | FR-DM-080 | OPEN |
| DMQ22 | Coexistence decision for endorsements; service invoices | FR-DM-090-092 | OPEN |
| DMQ23 | Changes included in the legacy change report | FR-DM-100 | OPEN |
| DMQ24 | Read-only legacy or archive per system; retention | FR-DM-110, 111 | OPEN |
| DMQ25 | Go-live date, window, freeze, month-end alignment. Month answered by the BDOI timeline - January 2028; exact date 3 January 2028 recommended with DMQ39 | FR-DM-120 | PARTIAL |
| DMQ26 | RMEL transition model. Answered by DMQ37 - no cohort is carried; the January-May 2028 expiries are extracted at go-live; later expiries on the normal lead time | FR-DM-122, 124 | ANSWERED |
| DMQ27 | Decommissioning criteria and owners | FR-DM-123 | OPEN |
| DMQ28 | Volumes, data quality, historic depth, delta frequency | NFR | OPEN |
| DMQ29 | Consolidated NFR document | NFR | OPEN |
| DMQ30 | Claims, EB, submitted policies, payees, users in scope | Scope | OPEN |
| DMQ31 | Number of trial migrations; masked production data in test | FR-DM-120 | OPEN |
| DMQ32 | Point of no return and fallback | FR-DM-121 | OPEN |
| DMQ33 | In-flight legacy items at the freeze | FR-DM-050, 051 | OPEN |
| DMQ34 | Collection history and open dispositions | FR-DM-050 | OPEN |
| DMQ35 | Exchange rate for foreign-currency openings | FR-DM-050, 051 | OPEN |
| DMQ36 | Package remapping. Answered - at Renewal sanitation; the migration loads the PACKAGE map only | FR-DM-034, 040 | ANSWERED |
| DMQ37 | January-May 2028 renewals. Answered - processed in BIBS after go-live from a go-live extraction; RAs already sent loaded | FR-DM-124, 125 | ANSWERED |
| DMQ38 | RMEL and disposition sources. Answered - Excel trackers; rejects reviewed by the Renewal processing team, maker and checker | FR-DM-125 | ANSWERED |
| DMQ39 | Year-end cut-over. Answered with a recommendation, awaiting Comptrollership confirmation - option A, provisional opening and FY2027 true-ups | FR-DM-022 to 024, 120, 121 | RECOMMENDED |

# Traceability

Every BRD-13 requirement is met by at least one FR. The screen and API columns name the main entry points.

<!-- table: widths=2.8,3.6,4.8,5.4 caption="BRD ID to FR, screen and API" size=8 -->
| BRD ID | FR | Screen | API |
|---|---|---|---|
| BRID 1.1a (p.7) | FR-DM-001, FR-DM-002, FR-DM-003 | Data Objects; Sign-off | /migration/objects; .../decision; /migration/batches/{no}/signoff |
| BRID 1.1b (p.7) | FR-DM-010, 013, 014, 015, 020, 021, 022, 023, 024, 003 | Extracts; Batches; Reconciliation; True-ups | /migration/extracts; /migration/batches; /migration/trueups; reports MIG-RECON-*, MIG-TRUEUP-RECON |
| BRID 2.1 (p.7) | FR-DM-031, FR-DM-032, FR-DM-033 | Client Matching; Client search | /migration/matching; /crm/clients?legacyRef= |
| BRID 3.1 (p.7) | FR-DM-011, FR-DM-012, FR-DM-030, FR-DM-034 | Code Maps | /migration/maps; report MIG-UNMAPPED-CODES |
| BRID 4.1 (p.8) | FR-DM-040, FR-DM-041, FR-DM-033, FR-DM-124 | Account search; Account page | /accounts |
| BRID 5.1 (p.8) | FR-DM-051, FR-DM-021, FR-DM-022, FR-DM-023 | Unapplied Payments | /cashiering/unapplied |
| BRID 5.2 (p.8) | FR-DM-052, FR-DM-050 | Unapplied Payments; Payments | /cashiering/matching/run |
| BRID 5.3 (p.8) | FR-DM-053 | Unapplied Payments | /cashiering/unapplied/{id}/disposition |
| BRID 5.4 (p.8-9) | FR-DM-054 | Unapplied Payments; Disbursement queue | .../disposition (REFUND) |
| BRID 5.5 (p.9) | FR-DM-055 | UPP Income Reclassification; My Approvals | /cashiering/income-reclass-batches |
| BRID 6.1 (p.9) | FR-DM-060, FR-DM-050 | Receive payment (OTC) | /cashiering/payments |
| BRID 6.2 (p.9) | FR-DM-061 | Payment uploads | PAY_* handlers |
| BRID 6.3 (p.9) | FR-DM-052 | Unapplied Payments | /cashiering/matching/run |
| BRID 6.4 (p.10) | FR-DM-053 | Unapplied Payments | .../disposition |
| BRID 7.1 (p.10) | FR-DM-070 | DPPR Legacy Reversal | /commission/dppr-batches |
| BRID 7.2 (p.10) | FR-DM-071 | PR2307 Legacy Reversal | /cashiering/pr2307-reversal-batches |
| BRID 8.1 (p.10) | FR-DM-080, FR-DM-050 | Remittance screens | remittance module |
| BRID 9.1 (p.11) | FR-DM-090 | Endorsement request | /adjustment/requests |
| BRID 9.2 (p.11) | FR-DM-091 | Endorsement request | /adjustment/requests |
| BRID 9.3 (p.11) | FR-DM-092 | Endorsement request | /adjustment/requests |
| BRID 10.1 (p.11) | FR-DM-100, FR-DM-050 | Reports | report PRC-LEGACY-CHANGES |
| BRID 11.1 (p.11-12) | FR-DM-110, FR-DM-111 | Legacy Inquiry; Legacy Access Log | /legacy-inquiry |
| BRID 12.1 (p.12) | FR-DM-120, 121, 122, 123, 124, 125, 022, 023, 024, 034 | Cutover; Run-off and Decommissioning; Renewal candidates; True-ups | /migration/cutover-plans; /migration/runoff; /migration/trueups; LegacyPolicySource |

API paths start with `/api/v1`.

The concept paper on an early renewal release (R10) is superseded by the single January 2028 go-live. Early migration of the client master and the renewal reference data (section VI) is met by the trial-migration load order of FR-DM-120. Its RMEL ingestion (Annex C) is replaced by the go-live extraction and the RA-sent file (FR-DM-124, FR-DM-125), and its package remapping question (p.2, Annex B and C) is answered by the remapping at Renewal sanitation (FR-DM-034). No placement or booking happens in BIBS before the January 2028 cut-over, because BIBS is not live before it.

# Sign-off

By signing, BDOI confirms that this FRS describes the data migration and the processing of legacy items it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Product Owner, Comptrollership - FRBS and ACSL", organisation: BDOI}
  - {name: "", role: "Product Owner, Comptrollership - Disbursement", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Operations - Financial Transactions and Processing", organisation: BDOI}
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
