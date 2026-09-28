---
# Source of "02 Data Migration Handbook" (Word), release set v2.0 of BRD-13 Data Migration.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-13_Data_Migration/HANDBOOK_BRD13_DATA_MIGRATION.md
# The functional requirements are ```fr blocks (read by the test plan builder); the screens, messages, menus and
# contract come from pack/ through signoff_pack.py, and the migration tables from pack/catalogue.yaml and
# pack/cutover.yaml through build_dm_pack.py (```pack blocks).
title: Data Migration Handbook
subtitle: BRD-13 Data Migration - strategy, functional specification, reconciliation and cut-over, release set v2.0
doc_type: Data Migration Handbook
doc_code: Handbook
brd: BRD-13
name: Data Migration
doc_id: BIBS-DMH-BRD-13
version: "2.0"
date: 5 October 2026
status: Issued for BDOI business sign-off
header_title: Data Migration Handbook BRD-13
control:
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis and Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "Previous issue as separate documents: FRS BRD-13 v1.2, Data Migration Strategy and Approach, Reconciliation Approach and Sign-off, Cutover Runbook (BDOI answers of 26-Sep-2026 to DMQ36-DMQ39)"
  - version: "2.0"
    date: 5 Oct 2026
    author: iorta TechNXT Business Analysis and Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: "Program Manager, Business Project Services (at sign-off)"
    change: "One handbook for the business sign-off of BRD-13: Part A strategy and approach, Part B functional specification of the Migration Console with one specification per screen (screenshots, fields, actions, rules, messages, outcome), the markers of migrated data and the legacy batches, Part C reconciliation and the sign-off gates per object, Part D the cut-over runbook, run-off and decommissioning; the proposed business rules and clarifications for confirmation"
distribution:
  - {name: "Program Manager, Business Project Services", role: "Business owner of BRD-13 and approver", organisation: BDO Unibank ESG, purpose: "Sign-off; owner of the cut-over"}
  - {name: "Head, Comptrollership; Product Owners FRBS / ACSL and Disbursement", role: Approver, organisation: BDOI, purpose: "Open items, GL opening, legacy accounts, reconciliation, opening-balance adjustments"}
  - {name: "Head, Operations; Operations - Financial Transactions, Cashiering, Remittance, Collections", role: Data owners, organisation: BDOI, purpose: "Insurers, policy headers, legacy invoices, unapplied payments, collection state"}
  - {name: "Product Owner, Marketing Business System; Heads of Retail and Corporate Marketing; TSU", role: Data owners, organisation: BDOI, purpose: "Reference data, clients, packages, renewal transition"}
  - {name: "Head, Renewal processing team", role: Data owner, organisation: BDOI, purpose: "Renewal advices already sent; day-1 renewal queue"}
  - {name: "Compliance; Unit Head, Analytics and Risk Management", role: Data owners, organisation: BDOI, purpose: "Masking, archive, Legacy Inquiry and its access log"}
  - {name: "BDOI IT (legacy EBIX, QPS, ISYS, CMS)", role: Reviewer, organisation: BDOI, purpose: "Extracts, transfer, freeze, read-only access, decommissioning"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, the migration runs and the cut-over"}
---

# Introduction

## Purpose

This handbook describes how BDO Insurance and Reinsurance Brokers, Inc. (BDOI) moves from its legacy systems to BIBS (BDOI Broker System, on iNXT BrokerVerse), and how BIBS supports the move. It is the document the business owner, the data owners, Comptrollership, the Renewal processing team and BDOI IT read and sign for BRD-13 Data Migration. It answers five questions:

1. **What data moves**, object by object, in which class (migrate, carry forward, conditional, archive, excluded), and who owns it (Part A).
2. **What BDOI provides**, in which form and by when: the load templates and the control files (Part A and the Migration Workbook).
3. **How the migration is run in BIBS**: the Migration Console screen by screen, the rules, the messages, how migrated records are told apart from new ones, and the batches that work the legacy balances down after go-live (Part B).
4. **How every object is proven** from source to target and signed off (Part C).
5. **How the cut-over is run**: the runbook from 30 days before go-live to the close of the opening-balance adjustments, the go / no-go, the rollback, the hypercare, the run-off and the decommissioning of the legacy systems (Part D).

The last chapter lists the business rules the project team proposes where the BRD is silent or where BDOI must confirm a recommendation, each with the decision requested.

## The BRD-13 set

This handbook is file 02 of the BRD-13 business sign-off set, release set v2.0, in the folder BRD-13_Data_Migration of Drop 0. It replaces the separate documents issued before (the FRS, the Strategy and Approach, the Reconciliation Approach and Sign-off, the Cutover Runbook, the Cutover Task Plan, the Data Requirements Workbook and the loose extract templates).

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: guide-map
```

The load templates are not issued as loose files. The Migration Console exports them from the layouts it holds (Layouts and Rules: a CSV template per layout, a workbook per object and one workbook of all templates, with the control-file template and the filling rules); the Migration Workbook lists the same layouts column for column for the review.

## Scope and basis

The basis is the Data Migration BRD (R1), a **draft v0.01 of 14-Apr-2026 that is not signed** (p.2, p.15-16; register DCR-189). The BRD asks for a **selective, continuity-focused migration** (p.3): migrate clean master and reference data for Day 1, carry forward only the open operational financial positions, keep history in read-only legacy or an archive, and let renewals recreate clean records in BIBS (p.5). This handbook follows that approach and fills the gaps the BRD leaves with proposals, each tied to an open decision (DMQnn) or to a proposed rule of the last chapter.

<!-- table: widths=4,9,4 caption="Scope of BRD-13" -->
| Area | In scope | BRD |
|---|---|---|
| Governance | Data object register, decision gates, sign-off gates, reconciliation of every object | BRID 1.1a, 1.1b |
| Master and reference data | Client master with matching and deduplication; lists of values, insurers, products, packages, risk codes and MIS values through versioned code maps | BRID 2.1, 3.1 |
| Policies | In-force policy headers; renewal-driven transition by expiry month | BRID 4.1, 12.1 |
| Open items | Legacy invoices with open balances; legacy unapplied payments; opening entries and legacy sub-ledgers | BRID 5.1-8.1 |
| Legacy invoice processing | Counter and automated payments, automatch, dispositions, reclassification to income, direct-payment and PR 2307 reversals, remittance, endorsements, the legacy change report of Prod Recon | BRID 5.1-10.1 |
| History | Read-only legacy and the BIBS archive with logged access | BRID 11.1 |
| Cut-over | Trial migrations, dress rehearsal and the production cut-over on the BDOI timeline (go-live January 2028) at the year-end boundary; go / no-go; provisional GL opening and the FY2027 opening-balance adjustments; go-live renewal extraction; run-off and decommissioning | BRID 1.1b, 3.1, 12.1 |

**Out of scope until BDOI decides (DMQ30):** open claims, Employee Benefits programmes, the Excel submitted-policy masterlists and payees; users are not migrated (they are created through User Access requests). Where BDOI brings an object into scope, it is loaded through the upload of its own module with the same reconciliation and gates (proposed rule PR-DM-05).

**Programme calendar.** The migration follows the BDOI drop plan and timeline (R9): data migration is part of Drop 0; requirements and mapping run in September-October 2026, the preparation of the migration from November 2026 to March 2027, the SIT migration from April to July 2027, the UAT migration from August to October 2027, and the full migration and cut-over from November 2027 to January 2028, with go-live in January 2028. The concept paper on an early renewal release (R10), signed on 6 September 2026, is superseded by BDOI's decision of 26 September 2026 that everything goes live together; there is **one production cut-over** (register DCR-240). Of its points, the trial-migration load order still applies: reference data and clients load first in every trial migration.

**BDOI answers of 26 September 2026.** Legacy packages are remapped at Renewal sanitation and the migration only loads the package code map (DMQ36); the renewals of the January-May 2028 expiries are processed in BIBS after go-live from a go-live extraction of the migrated policy headers, with the renewal advices already sent loaded so they are not sent again (DMQ37); the RMEL and dispositions are kept in Excel, and the Renewal processing team reviews rejected rows with a maker and a checker (DMQ38); and, as a recommendation that Comptrollership confirms, the go-live sits on the year-end boundary with a provisional GL opening and controlled adjustments of the FY2027 closing and audit entries (DMQ39, proposed rule PR-DM-01).

## How to read this handbook

- **Part A** is the approach, for every reader. Its tables of objects, owners, templates and code maps are the same rows as the Migration Workbook.
- **Part B** is the functional specification. Each screen has its purpose, who opens it, its navigation, screenshots with numbered callouts, the field table (No. = callout number), the actions, the business rules and the expected outcome, with the requirements (FR) and the test cases. The functional requirements follow, each with its BRD trace, actor, flows, rules, validations, fields, notifications, audit and acceptance criteria; then the messages, notifications, reports, configuration and the interface contract with the other BRDs.
- **Part C** says how each object is reconciled and verified and who signs each gate, with the forms.
- **Part D** is the cut-over runbook.
- Rules marked *Configurable* are settings of BIBS (a parameter, a list of values, a code map or a master record); rules marked *Fixed* change only through a change request. Values marked "default" (thresholds, retention days, timings) are proposals that BDOI confirms; a changed answer is a change of setting.
- The BRD numbers two requirements "BRID 1.1" (p.7). This handbook calls the first one (decision gates) **BRID 1.1a** and the second one (reconciliation) **BRID 1.1b**.
- The screenshots are taken on the SIT environment with seed data. Names, numbers and amounts are fictitious.

## References

<!-- table: widths=1.2,11.4,4 caption="Reference documents" size=8.5 -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | BDO Insure Core Modernization - Data Migration BRD | draft v0.01, 14-Apr-2026; not signed |
| R2 | Data Migration (BRD-13) requirements baseline | current |
| R5 | BRD BDOI Core Replacement (umbrella BRD) | v01 |
| R6 | BRD discrepancy and clarification register | 1.2 |
| R7 | Deliverables plan, hosting appendix and UAT readiness programme | current |
| R8 | Sign-off sets and FRS of Operations, Collections, Accounting / ACSL and Renewal | current |
| R9 | BDOI drop plan and programme timeline | received 26-Sep-2026 |
| R10 | Concept Paper - Advance Implementation of Renewal Processing | V1.0, signed 06-Sep-2026; superseded 26-Sep-2026 |

Page references ("p.8") are pages of the Data Migration BRD (R1) unless another document is named.

## Definitions and acronyms

```glossary
AR: Acknowledgment Receipt, issued for premium collected on behalf of an insurer
Archive: Read-only store of legacy history in BIBS, searched in Legacy Inquiry (BRID 11.1)
Autopay: Automated premium payments received in payment files (bills payment, trade, CLPC, direct credit) and matched by BIBS
Batch: One load of one data object in one environment, from validation to acceptance
Carry forward: Treatment of an open operational or financial item that continues to be processed in BIBS
CMS: Collection Management System (legacy)
Code map: Versioned table that maps a legacy code of a source system to a BIBS value (BRID 3.1)
Cohort: The legacy policies expiring in one month (RMEL expiry month)
Control file: File sent with each extract with its row count, hash total, amount totals and SHA-256
DP PR: Premium receivable of an invoice paid by the client directly to the insurer (reading to be confirmed, DMQ19)
DTIP: Due to Insurer - Premium
EBIX: Legacy booking, receivables and accounting system
Extract: A file of one layout from one source system, with its control file
Freeze: The time from which legacy data may no longer change (T-3 22:00; 31 December 2027 for the proposed date); only FY2027 GL adjustments by named Comptrollership users follow it
G1-G7: Sign-off gates of a data object - decision, mapping, validation, load approval, reconciliation, acceptance, go-live
Go / no-go: The decision to go live, taken on measured criteria
Go-live renewal extraction: One Renewal extraction at go-live of every migrated policy header expiring from go-live to 31 May 2028
ISYS: Legacy reporting system (claims and ACSL reports, Marketing Diary)
Layout: The column list of one extract file; the load template of the Migration Console
Legacy invoice: An invoice booked in EBIX or QPS before go-live, still open at the freeze, carried into BIBS and processed there
Legacy sub-ledger: The legacy control accounts and ledger items that hold the legacy positions apart from the new ones
LOV: List of values
Migration Clearing: GL account that takes the offset of every opening entry; must net to 0.00
MIS: Management information fields (market segment, department, unit head, AO, region, area, branch, business origin, customer segment)
Opening-balance adjustment: A journal in the opening period that brings a FY2027 closing or audit adjustment of the legacy GL into the BIBS opening balances (also called a true-up)
Opening period: The first GL period of BIBS, January 2028, into which the opening entries and the adjustments post (value date 1 January 2028)
OTC: Over-the-counter payment at a cashier
PR: Premium receivable
PR 2307: Premium receivable covered by the client's BIR Form 2307
Provisional opening: The GL opening loaded at cut-over from the preliminary December 2027 trial balance, balance-sheet accounts only
QPS: Legacy quotation, client and policy system
RA-sent file: Reference file (P03) of the renewal advices sent by hand before go-live
RMEL: Renewal Master Expiry List
Run-off: The period in which legacy policies expire and renew into BIBS
Staging: Area in BIBS where extract rows are checked, mapped and validated before they load
T: Go-live date - January 2028 (BDOI timeline), proposed Monday 3 January 2028
Trial migration: A rehearsal of the migration on a test environment with masked data
TSU: Technical Support Unit; maintains the BIBS products and packages (BRD-3)
UPP: Unapplied premium payment
```

# Part A - Strategy and approach {-}

Part A says what moves from the legacy systems to BIBS, in which class and by which steps, who owns each object and each step, how the migration is rehearsed, how the cut-over is run and how the legacy items are processed after go-live. Its tables are the same rows as the sheets of the Migration Workbook.

# Objectives and principles

## Objectives

The BRD states the objective as "a stable Core go-live by ensuring Day-1 operational continuity for marketing, operations, and accounting while establishing a clean and trusted data foundation" (p.3). In measurable terms, at go-live:

<!-- table: widths=0.8,7.6,8.2 caption="Migration objectives and how they are measured" -->
| # | Objective | Measure at go-live |
|---|---|---|
| 1 | Marketing finds every in-scope client and uses it without re-encoding (BRID 2.1) | Every in-scope legacy client number resolves to one BIBS client; client review queue empty |
| 2 | Every code used by a migrated record exists in BIBS (BRID 3.1) | Unmapped-code report empty for all Day-1 objects |
| 3 | Every open receivable, payable and unapplied payment continues in BIBS (BRID 5.1-8.1) | Open balances in BIBS equal the legacy extract per currency; Migration Clearing 0.00 per branch and currency |
| 4 | Legacy invoices are processed with the normal BIBS screens (BRID 5.2-10.1) | Smoke test on production passed (payment, automatch, remittance, Invoice 360) |
| 5 | Every migrated object is reconcilable from source to target (BRID 1.1b) | Reconciliation L1-L5 signed per object (gate G5) |
| 6 | History remains retrievable with logged access (BRID 11.1) | Legacy read-only link or archive inquiry available per legacy system |
| 7 | Renewals move to BIBS by expiry month (BRID 12.1) | Every policy expiring from go-live to 31 May 2028 and not renewed in legacy is a candidate in BIBS Renewal at go-live, January expiries flagged urgent, RAs already sent recorded; later expiries extracted by BIBS on the normal lead time |
| 8 | The GL opens correctly at the year end (DMQ39) | Balance-sheet opening equals the signed preliminary December TB per branch and currency; FY2028 P&L at zero; every FY2027 adjustment reaches BIBS through a signed true-up |

## Principles

1. **Selective, not a conversion.** Every data object gets one class through a decision gate (BRID 1.1a). History is never loaded into business tables; it stays in read-only legacy or goes to the archive.
2. **Continuity first.** Anything BDOI must act on after go-live (a receivable, a payable, an unapplied payment, a promise to pay) is carried forward, so no business process depends on legacy after the freeze.
3. **Clean start.** Master data is cleansed and deduplicated before it is loaded. Data that fails the rules is fixed at source or excluded by its owner; it is not loaded "as is".
4. **One processing system per item.** From the freeze, a legacy open item is processed only in BIBS. Legacy becomes read-only (BRD p.5). There is no two-way synchronisation.
5. **Load through the BIBS services.** Records are created by the same services the screens use, so every BIBS rule, audit entry and accounting entry applies. Nothing is written directly into a business table.
6. **Reconcile everything, and keep the evidence.** Every extract carries control totals; every object is reconciled by counts, amounts, hash totals, fields and GL; a break is fixed or explained and approved before sign-off.
7. **Rehearse before production.** Four trial migrations and a dress rehearsal run the full cutover with masked data and measured timings, in the SIT and UAT migration windows of the BDOI timeline. Production follows the rehearsed plan.
8. **Protect personal data.** Masked data outside production, staging purged within 5 days of sign-off, access only for migration roles and only from the Philippines (hosting appendix, R7).

## What this migration does not do

- It does not convert closed transactions, GL history or expired policies into BIBS business tables (BRD p.3).
- It does not re-book legacy invoices. A legacy invoice keeps its legacy number and is processed as a legacy invoice until it is settled.
- It does not create users from legacy. Users are created through User Access requests (BRD-11); legacy user IDs are only mapped so that AO, handler and collector fields resolve.
- It does not run legacy and BIBS in parallel for the same items (chapter Cut-over strategy, Parallel run).
- It does not write back to QPS or EBIX after the freeze (register DCR-202).

# Scope and decisions per data object

## The decision gate

BRID 1.1a asks that each data object is classed Migrate, Carry forward (open items), Archive or Excluded once the business owners have assessed four criteria: Day-1 need, compliance need, read-only / archival option and data trust (p.7). The BRD does not give the rule that turns the criteria into a class (DMQ02, DCR-192). The proposal:

<!-- table: widths=2.6,6.4,7.6 caption="Proposed decision rule (DMQ02)" size=9 -->
| Class | When | Examples |
|---|---|---|
| Migrate | Day-1 need = Yes and the data is master or reference data; data trust High or Medium after cleansing | Clients, LOVs, insurers, products, GL opening balances |
| Carry forward | Day-1 need = Yes and the record is an open operational or financial position that BDOI must still act on | Open legacy invoices, UPP, open promises to pay |
| Conditional | Day-1 need depends on a business condition that BDOI states (BRID 4.1 "when operationally required") | In-force policy headers; objects not named in the BRD (claims, EB, submitted policies, payees) |
| Archive | Day-1 need = No and compliance need = Yes, or the read-only / archival option is required | Closed invoices, receipts, remittances, expired policies, GL history, documents |
| Excluded | Day-1 need = No and compliance need = No, or the data is set up again in BIBS | Users and roles, screening results |

The Data Migration Lead records the four criteria and the proposed class per object in the Migration Console; the business owner of the object approves it (gate G1, FR-DM-002). A later change of class needs a new approval, and an object whose data is loaded cannot be moved to Archive or Excluded until the load is rolled back.

## Data object register

The register below is the proposal for gate G1 (DMQ01, DCR-195). The Migration Workbook sheet Object catalogue holds the same rows with the review columns; the sheets Data owners and Extract planning add the columns BDOI fills in (named owners and stewards, volumes). The Migration Console holds the same register (Data Objects).

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-objects
```

The four criteria of the BRD, as proposed for each object in the Migration Console:

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-criteria
```

## Depth, delta and due dates

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-planning
```

The milestones in the Due column are:

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-milestones
```

## Business owners and data stewards

Each object has a business owner who signs its decision (G1), its code maps (G2) and its acceptance (G6), and a data steward who prepares the code maps, resolves the data-quality issues and fixes records at source. The table is the proposal from the BRD approvers (p.15-16); BDOI names the people (DMQ01).

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-owners
```

## Client migration "2020 to present" and daily client batches

The umbrella BRD sizes a "one-time migration from Broker and source", "2020 to present", plus a daily "midday batch (new transactions)", an "EOD batch (new clients)" and a "client modification report" (R5 p.43; register DCR-173). The Data Migration BRD asks for a deduplicated client master (BRID 2.1). This handbook reconciles the two as follows, for BDOI to confirm:

- **One-time load.** Clients created from 1-Jan-2020 to the freeze, plus older clients that have an in-force policy, an open item or activity since 2020 (DMQ05), are extracted in layouts C01 and C02, matched and deduplicated, and loaded two weeks before go-live.
- **Daily batches until the freeze.** From the pre-load until the freeze, the legacy EOD produces a daily delta of new clients and of changed clients (the modification report) in the same layouts; BIBS loads it overnight (daily client delta job). New transactions of the day ("midday batch") are not migrated as transactions: open items are loaded once, after the freeze.
- **No feed after go-live.** After the freeze, legacy is read-only and BIBS is the system of record of the client master (BRD p.5), so the daily batches stop. If BDOI needs a client feed from another source after go-live (for example bank referrals), it is a separate interface and not part of this migration.

# Source systems

## Legacy systems in scope

The BRD names QPS and EBIX "e.g." (p.3). The other BRDs also name ISYS, the CMS and Excel masterlists. BDOI IT confirms the list and the system of record per object (DMQ03, DCR-195).

<!-- table: widths=2.4,6.8,7.4 caption="Legacy systems and their role in the migration" size=9 -->
| System | Holds (as known from the BRDs) | Role after the freeze |
|---|---|---|
| EBIX | Booking, invoices, receipts (AR / OR), remittance, UPP, GL, payees, branches | Read-only for inquiry until the archive is loaded and the decommissioning checklist is signed |
| QPS | Quotations, clients (assured), covers and policies, packages, rates, sales organisation, renewals | Read-only; decommissioned after the last QPS-issued policy has renewed or lapsed |
| ISYS | Claims and ACSL reports, Marketing Diary, GL reports | Read-only; archived before decommissioning |
| CMS (Collection Management System) | Collection dispositions, promises, installment plans, assignments; client data from the bank | Read-only; archived before decommissioning |
| Excel masterlists and file shares | Submitted-policy masterlists, KYC documents, legacy documents | Migrated (P04) or archived with checksums, then removed per retention |

## Source per object

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-sources
```

Where two systems hold the same object (clients in QPS, EBIX and the CMS; products in QPS and EBIX), each system sends its own extract. BIBS matches the records across systems and keeps one BIBS record with a cross-reference to every legacy key. The survivorship rule decides which value wins per field (Client matching and survivorship, below).

# Target in BIBS

## Target per object and load order

Every object lands in a BIBS module through that module's service. The load order follows the dependencies: reference data first, then clients, then policy headers, then open items, then the GL, so every record finds the records it refers to.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-targets
```

## How migrated records are marked

- **Origin.** Every migrated record carries the origin Migrated with its source system, legacy reference and migration batch. Legacy invoices and legacy unapplied payments also belong to the legacy ledger context: their postings go to the legacy accounts. The lists and records show a LEGACY badge, and the lists that hold migrated records have an Origin filter (All origins, BIBS, Migrated) (Part B, Migrated data on the business screens).
- **Key cross-reference.** Every loaded record has a cross-reference from its legacy key (source system, object, legacy key) to the BIBS record. Users search clients and policies by their legacy numbers (FR-DM-033); reruns skip what is already loaded.
- **No side effects.** Loads send no notification to clients or insurers, do not start onboarding workflows, and do not trigger sanction screening per client (one full screening run follows the client load, DMQ06, DCR-208).
- **Values at cut-over.** The values of each legacy invoice at load are kept as loaded and shown on Invoice 360. The report Changes to Legacy Invoices compares every later change with them (BRID 10.1).

# Migration approach

## End-to-end process

Figure 1 shows the steps every object goes through, and who does each step.

![Migration pipeline per data object: BDOI steps (left) and BIBS Migration Console steps (right)](figures/dm_pipeline.dot){width=15}

<!-- table: widths=0.7,3.6,5.6,3.6,3.1 caption="Steps, responsibilities and evidence" size=8.5 -->
| # | Step | What happens | Who | Evidence |
|---|---|---|---|---|
| 1 | Extract | BDOI IT extracts each object in the agreed layout with its control file | BDOI IT | Data file and control file |
| 2 | Secure transfer | Files are uploaded on the Extracts screen of the Migration Console, or dropped on the secure file drop once BDOI IT names it (PR-DM-03); never e-mailed | BDOI IT | Upload log with SHA-256 |
| 3 | Intake checks | BIBS checks checksum, header, row count, amount totals and hash total against the control file; a failing file is rejected as a whole | iorta Migration Operator | Extract status Staged or Rejected |
| 4 | Staging and profiling | Rows are staged (masked outside production) and profiled; the profiling report goes to the data stewards | iorta Migration Lead | Profiling report per object |
| 5 | Cleansing | Issues at source are fixed in legacy by the stewards; formats, defaults and mapped values are handled in staging | BDOI data stewards; iorta | Data-Quality Issues report |
| 6 | Mapping | Legacy codes are mapped to BIBS codes in versioned code maps; the business owner approves each version (G2) | Data steward prepares; owner approves | Code Map Versions and Unmapped Legacy Codes reports |
| 7 | Validation and matching | Data-quality rules run on every row; clients are matched and deduplicated (G3) | BIBS; Data steward | Data-Quality Issues and Client Matching reports |
| 8 | Load | Approved batches load through the BIBS services (G4) | Data Migration Lead approves; iorta runs | Run log of the batch; Rejected Rows report |
| 9 | Reconciliation | L1 counts, L2 amounts, L3 hash totals, L4 fields, L5 GL; breaks explained (G5) | Reconciliation approver | Reconciliation Summary and Migration Clearing reports |
| 10 | Acceptance | Business owner checks samples on the BIBS screens and accepts the object (G6) | Business owner; Data Migration Lead | Sign-off form |
| 11 | Purge | Staged records and files are deleted within 5 days of sign-off; counts, hashes and totals are kept | BIBS (daily staging purge) | Purge date on the batch |

## Extract templates

BDOI extracts each object in its layouts. The layouts are held by the Migration Console (Layouts and Rules), which exports them as load templates: a CSV template per layout with the column names as its header row, a workbook per object and one workbook of all templates with the column descriptions, the control-file template and the filling rules. A layout gives, per column: name, description, type and length, mandatory flag, allowed values or code map, format, example and validation. The Migration Workbook lists every layout on a sheet of its own, with the same sheet name as the console workbook, for the review.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-layouts
```

Four objects have sub-layouts sent as separate files with the same as-of date: R04 and R04B (insurers and their branches), P01 and P01S (policy headers and their insurer shares), F01, F01S and F01C (invoice headers, insurer shares and components), G03 and G03D (adjustment journal lines and their open-item detail). The rules of every file (Migration Workbook sheet File rules; the first rows are the How to fill sheet of the console workbook):

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-file-rules
```

The control file sent with every data file:

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-control-file
```

## Secure transfer and staging

- Files are received through the Extracts screen of the Migration Console, or through a secure file drop once BDOI IT names it (DMQ28, PR-DM-03). E-mail is never used.
- Files land in an encrypted intake bucket in AWS ap-southeast-1 with a 5-day lifecycle.
- The intake checks run before any row is staged. A file that fails is rejected with the reason and the difference, and BDOI IT re-sends it.
- Staged rows keep the raw values as received and the mapped values. Outside production, names, addresses, TIN, ID, account and phone numbers, e-mail addresses and birth dates are masked at intake with a keyed, repeatable masking, so dedupe still works on masked data.

## Profiling

After the first full extract (M3, 29 January 2027) and after each trial migration, iorta TechNXT profiles every staged object and sends the report to the data stewards. The report gives, per field: fill rate, distinct values, top values, values out of format, codes without a map entry, duplicates of the key, and, per object, the rows that fail each data-quality rule. For clients it gives the candidate duplicate pairs by score band; for open items it gives the totals by component, currency, branch and age band. The stewards use it to plan the cleansing and the code maps.

## Cleansing: who fixes what

Cleansing is shared. BDOI owns the data and fixes it where it lives; iorta TechNXT fixes what is a matter of format or mapping.

<!-- table: widths=4.4,6.2,2.6,3.4 caption="Cleansing responsibilities" size=8.5 -->
| Issue | Example | Fixed by | Where |
|---|---|---|---|
| Wrong or missing business data | Client without birth date, invoice without insurer, TIN of another client | BDOI data steward | In legacy, before the next extract |
| Duplicate clients | Same person in QPS and EBIX with different numbers | BIBS matching; data steward decides doubtful pairs | Client Matching queue |
| Legacy codes without a BIBS value | Discontinued segment code, insurer branch not in BIBS | Data steward proposes; business owner approves | Code map version (Map, Default, Create or Reject) |
| Format differences | Date as dd/MM/yyyy, TIN with dashes, mobile without prefix | BDOI IT with iorta TechNXT | BDOI IT extract |
| Records out of scope | Client not active since 2019, invoice fully settled | Business owner decides | Exclusion recorded in the batch |
| Open balances that do not add up | Components do not equal gross premium; open not equal to booked less paid | BDOI data steward with Comptrollership | In legacy (correction entry) before the freeze |
| Data that cannot be fixed before go-live | UPP without payor reference | Business owner waives with a reason and a plan | Waiver in the batch (G3) |

Thresholds for loading an object (configurable): master data may be loaded with at most 0.5 percent of rows rejected or waived; financial objects (open invoices, UPP, trial balance) load only with 0 errors, or with each excluded row approved by the business owner with a manual-entry plan.

## Mapping and code maps

Legacy codes are mapped to BIBS codes through code map sets, one per domain. Each set has versions: Draft, Submitted, Approved, Superseded. The data steward prepares a version on the Code Maps screen, or in Excel (Export and Import Excel), and submits it; the business owner approves it (maker-checker). Each batch records the map versions it used, so every loaded value can be traced to the entry that produced it (BRID 3.1).

An entry maps a legacy code to a BIBS value (Map to a BIBS value), to the default of the set (Use the default value), rejects the rows that carry it (Reject the record), or creates a new BIBS value (Create in BIBS). Values created in BIBS go through the owning master (for example the lists of values or the insurers) and are authorised there before any dependent object loads.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-map-sets
```

### Package remapping

Legacy packages (QPS package code and version) must map to the package names that TSU maintains in BIBS (BRD-3). BDOI decided on 26 September 2026 that the remapping happens **at sanitation in Renewal**, per renewal candidate, and not in the migration intake (DMQ36, register DCR-241). The concept paper had left "during upload processing or during sanitation" open (R10 p.2, Annex B, Annex C).

<!-- table: widths=3.2,13.4 caption="Package remapping (DMQ36 answered)" size=8.5 -->
| Where | What happens |
|---|---|
| Migration | Loads the package code map as reference data (object R06): entries from a legacy package and version to a BIBS package version, with conditional entries where one legacy package splits (by risk code, insurer or sum-insured band). TSU prepares each version and the Product Owner of Marketing Business System approves it (G2). Policy headers (P01) and the RA-sent file (P03) keep the legacy package as given; no package is resolved, rejected or warned at intake |
| Renewal sanitation | The package check of the Renewal sanitation resolves the BIBS package of each candidate through the map. A package without an entry, with a Reject entry or with no matching qualifier sends the candidate to the Exception bucket |
| Exception bucket | Worked by the Renewal processing team: the processor chooses the BIBS package (or the new-business path) and records the reason; each choice goes to TSU for the next map version |

**Testing.** The package code map is loaded in every trial migration. Profiling lists the legacy packages of the headers expiring up to 31 May 2028 that have no entry: this is the expected Exception-bucket volume at go-live, and TSU closes the gaps before the map freeze. Business verification samples cover every legacy package with more than one BIBS target.

## Validation

Every staged row is checked against the data-quality rules of its layout before a batch can be approved for load. An error stops the row; a warning loads it and reports it. The catalogue (Layouts and Rules, Data-Quality Rules; Migration Workbook sheet Validation rules):

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-rules
```

## Client matching and survivorship

Legacy clients of all source systems are matched with each other and with any client already in BIBS before any client is loaded (FR-DM-031). Keys and scores (proposal until DMQ04 is answered):

<!-- table: widths=1,8.2,2 caption="Client matching keys" size=9 -->
| Key | Rule | Score |
|---|---|---|
| K1 | TIN, digits only | 100 |
| K2 | ID type and number | 100 |
| K3 | Last name, first name and birth date | 95 |
| K4 | Normalised corporate name with registration number (90 without) | 95 |
| K5 | Bank CIF, if BDOI confirms it as a key | 100 |
| K6 | E-mail; PH mobile | 40 each |
| K7 | Similar name (at least 0.85) with the same birth date or city | 70 |

Pairs scoring 90 or more merge automatically; 60 to 89 go to the data steward's review queue; below 60 are separate clients. The client batch cannot be approved while the queue has open pairs. Survivorship takes, per field, the first non-blank value by source priority (proposal: QPS for contact data, EBIX for billing data) or the most recently updated value. Every value that lost is kept for review (Client Matching report).

## Load through the BIBS services

An approved batch is loaded in chunks of 500 rows, four at a time; each chunk is saved as a whole; a failing chunk is retried row by row so one bad row fails alone. Loading the same batch again creates no duplicate: rows already loaded and unchanged are skipped through the key cross-reference. The planning target is 50,000 rows an hour per partition, proven in the dress rehearsal.

Before sign-off a batch can be rerun (only the rejected rows, after a fix) or rolled back (the records it created are undone through the BIBS services, unless a record was changed in BIBS since the load). In the production cut-over the rollback point is a full backup (snapshot) of the production system (chapter Cut-over strategy, Rollback).

## Reconciliation and sign-off

Every object is reconciled at up to five levels. Part C gives the measures per object, the sample plan for business verification and the sign-off forms.

<!-- table: widths=1.4,3.6,11.6 caption="Reconciliation levels (BRID 1.1b)" size=9 -->
| Level | Measure | Rule |
|---|---|---|
| L1 | Record counts | Received = control count; loaded + skipped + rejected + excluded = staged |
| L2 | Amounts | Sum per amount column and currency: control total = staged = BIBS; difference 0.00 |
| L3 | Hash totals | Hash total of the key and SHA-256 per row equal between control, staging and BIBS keys |
| L4 | Fields | Every mapped field of every loaded row, read back from BIBS, equals the staged value |
| L5 | GL | Migration Clearing is 0.00 per branch and currency; legacy control accounts equal the legacy sub-ledgers |

Seven gates record the sign-offs of each object: G1 decision, G2 mapping, G3 validation, G4 load approval, G5 reconciliation, G6 object accepted, G7 go-live. A reconciliation with a break cannot be signed until the break is explained and the explanation approved. The operator who ran a batch cannot sign its reconciliation or acceptance.

# Legacy invoices after go-live

This chapter is for the Cashiering, Remittance, Collections, Accounting and Operations users who will process legacy items in BIBS.

## What a legacy invoice is

A legacy invoice is an invoice booked in EBIX or QPS before go-live that still has something open at the freeze: premium the client has not paid, premium paid but not yet remitted to the insurer, commission not yet collected from the insurer, or a PR2307 balance. It is **not re-booked** in BIBS. It is loaded into the BIBS Operations ledger with:

- its legacy invoice number (kept as the BIBS invoice number unless it collides with a number of another source system, DMQ11), the source system and the legacy policy reference;
- each component (basic premium, DST, premium tax or VAT, LGT, FST, other charges, DTIP, commission, VAT on commission, withholding tax, PR2307) with what was booked, paid, remitted, adjusted and written off in legacy, so its open balance equals the legacy open balance;
- its insurer shares, client, AO, unit, branch and flags (direct payment, 2307);
- an opening accounting entry that puts the open balances on the **legacy sub-ledgers**.

From then on BIBS processes it with the normal screens (Figure 2). The only difference from a BIBS invoice is where its postings go: to the legacy control accounts, so the legacy positions run off visibly and separately from the new business.

![A legacy invoice and the processes that act on it after go-live (BRID 5.1-10.1)](figures/brd13_legacy_invoice_flows.dot){width=12}

## Legacy sub-ledgers

The BRD asks for legacy Premium Receivable, Commission Receivable, DTIP and UPP sub-ledgers (BRID 5.2, 6.1, 6.3, 6.4, 7.1, 7.2, 8.1). Comptrollership assigns the account codes of the legacy control accounts and of the Migration Clearing account (DMQ18, DCR-197); this handbook names the accounts only.

<!-- table: widths=4.6,6.2,5.8 caption="Legacy sub-ledgers (account codes assigned by Comptrollership, DMQ18)" size=9 -->
| Legacy control account | Holds | Moved by |
|---|---|---|
| Premium Receivable - Legacy (by component) | Open premium of legacy invoices | Payments, automatch, dispositions, DP PR reversals, write-offs, endorsements |
| PR 2307 - Legacy | Open PR2307 of legacy invoices | PR2307 reversals |
| Due to Insurers (DTIP) - Legacy | Premium payable to insurers on legacy invoices | Remittance, DP PR and PR2307 reversals, endorsements |
| Commission Receivable - Legacy | Commission and VAT on commission still due from insurers | Remittance, DP commission collection, endorsements |
| Unrealised Commission and Deferred Output VAT - Legacy | Commission not yet recognised as income | Realisation on collection |
| Unapplied Collections (UPP) - Legacy | Unapplied payments received in legacy | Automatch, dispositions, refunds, reclassification to income |
| Migration Clearing | Offsets of the opening entries and of the legacy control-account lines of the trial balance | Opening entries only; must be 0.00 |

## Worked example: one legacy invoice from load to remittance

Legacy invoice **I00123456** (EBIX), private car, PHP, lead insurer 100 percent. Booked in legacy before go-live: basic premium 20,000.00, DST 2,500.00, premium tax 400.00, LGT 150.00; gross premium 23,050.00; commission 4,000.00 (20 percent), VAT on commission 480.00, withholding tax on commission 400.00. Before the freeze the client paid 13,050.00, which covered DST, premium tax, LGT and 10,000.00 of the basic premium (the application order DST, premium tax / VAT, LGT, FST, other charges, basic). Nothing was remitted to the insurer. The figures are made up.

<!-- table: widths=3.6,2.4,2.2,2.2,2.2,4 caption="Positions of I00123456 at the freeze (extract F01C)" size=9 -->
| Component | Booked | Paid | Remitted | Open | Legacy sub-ledger |
|---|---|---|---|---|---|
| Basic premium | 20,000.00 | 10,000.00 | 0.00 | 10,000.00 | Premium Receivable - Legacy |
| DST | 2,500.00 | 2,500.00 | 0.00 | 0.00 | Premium Receivable - Legacy |
| Premium tax / VAT | 400.00 | 400.00 | 0.00 | 0.00 | Premium Receivable - Legacy |
| LGT | 150.00 | 150.00 | 0.00 | 0.00 | Premium Receivable - Legacy |
| DTIP | 23,050.00 | - | 0.00 | 23,050.00 | DTIP - Legacy |
| Commission | 4,000.00 | - | 0.00 | 4,000.00 | Commission Receivable - Legacy |
| VAT on commission | 480.00 | - | 0.00 | 480.00 | Commission Receivable - Legacy |
| Withholding tax | 400.00 | - | 0.00 | 400.00 | (settled at remittance) |

What happens after go-live:

<!-- table: widths=2,5.4,9.2 caption="Life of I00123456 in BIBS" size=8.5 -->
| When | Event in BIBS | Effect |
|---|---|---|
| Load (T-2) | Invoice created in the Operations ledger with the origin Migrated, in the legacy ledger context; opening entry | Premium Receivable - Legacy 10,000.00 debit; DTIP - Legacy 23,050.00 credit; Commission Receivable - Legacy 4,480.00 debit; the unrealised part per DMQ13; the balance of the entry to Migration Clearing. Invoice 360 shows booked 23,050.00, applied 13,050.00, open 10,000.00 and the LEGACY badge |
| T+2 | The client pays 10,000.00 at the counter quoting I00123456 | BIBS finds the legacy invoice, issues a BIBS AR and applies the payment to basic. Entries: Dr Bank / Cr Unapplied Collections 10,000.00; Dr Unapplied Collections / Cr Premium Receivable - Legacy 10,000.00. The invoice is fully paid; the receipt is in the Cash Receipts Book |
| T+9 | Remittance extraction for the insurer | The invoice is extracted with paid AR 23,050.00 (13,050.00 paid in legacy and 10,000.00 in BIBS). Net due to the insurer 18,970.00 = 23,050.00 - 4,480.00 + 400.00. Entry: Dr DTIP - Legacy 23,050.00 and Dr CWT 400.00 / Cr Commission Receivable - Legacy 4,480.00 and Cr Due to Insurer for Disbursement 18,970.00. The schedule shows the legacy invoice number and source system; the commission OR is issued as for any batch |
| T+20 | Positive endorsement, additional basic premium 2,000.00 | The policy header is a migrated account (P01). BIBS books an endorsement invoice with a BI- number whose parent is I00123456 and the legacy ledger context; its entries go to the legacy accounts; the service invoice is issued for the commission increase |
| Month end | Changes to Legacy Invoices report | Shows for I00123456 basic premium original 20,000.00, updated 22,000.00, delta 2,000.00, with the endorsement number, date and user (BRID 10.1) |

## Unapplied premium payments (UPP)

Every legacy UPP item with a balance at the freeze is created in the BIBS Unapplied Payments workbench with the origin Migrated, its legacy AR number and date, payor, client, sales unit, amount, balance, status and the references the payor gave (invoice, cover, PN, bank reference). An opening entry puts the balance on Unapplied Collections - Legacy. No new AR is issued for money already acknowledged in legacy (proposal, DMQ14, DCR-204).

<!-- table: widths=3.6,6.2,6.8 caption="Processing legacy UPP in BIBS (BRID 5.1-5.5, 6.3, 6.4)" size=8.5 -->
| Process | Example | Entries (by context) |
|---|---|---|
| Automatch rerun (after every payment upload and hourly) | Legacy UPP of 5,000.00 quotes legacy invoice I00200001 (3,000.00 open) and new invoice BI-2028-000150 (2,000.00 open). Automatch applies both and closes the item | Dr Unapplied Collections - Legacy 3,000.00 / Cr Premium Receivable - Legacy 3,000.00; Dr Unapplied Collections - Legacy 2,000.00 / Cr Premium Receivable (new) 2,000.00 |
| Apply to another invoice (client instruction) | The client asks to apply a legacy UPP to another legacy invoice | Dr Unapplied Collections - Legacy / Cr Premium Receivable - Legacy |
| Refund | Approved refund of 1,500.00 | Dr Unapplied Collections - Legacy / Cr Refund Payable; payment request to Disbursement |
| Reclassification to other income (Unapplied to Income) | UPP of 1,500.00 unclaimed for more than 2 years; batch approved by the Cashiering team lead and then top management (DMQ16, PR-DM-06) | Dr Unapplied Collections - Legacy / Cr Other Income (account per DMQ16); item closed |

## Payments, reversals, remittance, endorsements and Prod Recon

<!-- table: widths=3.4,6.4,6.8 caption="Other processes on legacy invoices" size=8.5 -->
| Process (BRID) | What the user does | What BIBS does |
|---|---|---|
| OTC payment (6.1) | Keys the legacy invoice number, ARN, policy or PN at the counter | Finds the legacy invoice, issues the AR, applies by component, posts to Premium Receivable - Legacy; Cash Receipts Book as usual |
| Autopay (6.2) | Uploads the bills payment, trade, CLPC or direct credit file as today | Matches rows that carry the legacy invoice number or the EBIX reference; applies and posts by the context of each invoice |
| DP PR Legacy Reversal (7.1) | A Commission user adds the legacy invoices tagged "DP PR for reversal" by Collections, or other legacy invoices with open premium; the Commission team lead approves | Reverses each open premium receivable against DTIP on the legacy sub-ledgers, one line at a time (Dr DTIP - Legacy / Cr Premium Receivable - Legacy); Commission Receivable effect per DMQ19 |
| Legacy PR 2307 Reversal (7.2) | A Cashiering user adds each legacy invoice with the amount to reverse; the Cashiering team lead approves | Offsets PR2307 - Legacy against DTIP - Legacy; commission effect per DMQ20 |
| Remittance (8.1) | Runs the extraction as today | Includes legacy invoices with paid premium not yet remitted; never remits again what was remitted in legacy; posts legacy lines to DTIP and Commission Receivable - Legacy |
| Endorsements (9.1-9.3) | Raises the endorsement request on the legacy invoice | Needs the migrated policy header; books the endorsement invoice in the legacy ledger context; non-financial changes update the account with no posting; renewal uses the updated values |
| Collections | Works the worklist as today | Legacy invoices above the threshold appear with the LEGACY badge; carried promises and assignments (F03) are shown |
| Prod Recon (10.1) | Runs the report Changes to Legacy Invoices | Lists original, updated and delta per change; legacy invoices are not part of the production register extract |

## When the legacy context closes

The legacy sub-ledgers run off as legacy invoices are paid, remitted, reversed or written off and as legacy UPP is applied, refunded or reclassified. The report Open Legacy Positions shows the open legacy positions by component, insurer, client and age each month. When no legacy invoice or UPP is open and the legacy control accounts and Migration Clearing are 0.00, Comptrollership decides whether to close the legacy accounts.

# In-force policies and the renewal-driven transition

## In-force policy headers

BRID 4.1 asks for a minimal header of in-force policies "when operationally required" (p.8); capability 4 is conditional (p.6). BIBS needs the header for three things: to locate active coverage for servicing, to endorse a legacy invoice (BRID 9 cannot be met without it), and to renew the policy in BIBS. The proposal is therefore to migrate the header of **every policy in force at go-live** (DMQ09, DCR-194), in layouts P01 (header) and P01S (insurer shares).

A header becomes a BIBS account with the origin Migrated and the status Booked: client, product and line, insurer and shares, policy number, inception and expiry, sum insured, premium, currency, payment arrangement, PN numbers, AO, unit and branch. It has no quotation, placement or BIBS invoice. Its legacy invoices link to it. It is found by ARN, policy number, legacy reference or client.

## Renewal by RMEL cohort

The transition follows the renewal expiry month (RMEL, BRID 12.1). BDOI answered on 26 September 2026 that the renewals of the January-May 2028 expiries are **processed in BIBS after go-live**, and that no renewal candidate is carried from legacy (DMQ37, register DCR-240). BIBS Renewal normally extracts policies 140 days before expiry; at go-live it runs one extraction of every expiry from go-live to 31 May 2028 instead.

![Transition by expiry month (go-live 3 January 2028, recommended)](figures/dm_rmel_cohorts.dot){width=16}

<!-- table: widths=4,12.6 caption="Treatment of each cohort (DMQ37 answered)" size=9 -->
| Expiry | Treatment |
|---|---|
| Before go-live (up to 2 January 2028) | Renewed or lapsed in legacy; a renewal whose new term starts before go-live is placed and booked in legacy before the freeze and migrates as an in-force header with its open invoice |
| Go-live to 31 May 2028 | Processed in BIBS after go-live. At 04:00 on go-live day BIBS extracts every migrated policy header expiring in this window that was not renewed in legacy, prioritised by expiry date, with the January expiries flagged urgent. A renewal advice already sent by hand before go-live is recorded on the candidate and is not sent again |
| From 1 June 2028 | Extracted by BIBS Renewal from the migrated headers on the normal lead time of 140 days; 1 June 2028 is extracted on 13 January 2028 |

**What this needs from the migration.**

- **Policy headers (P01).** The go-live extraction works only from the migrated headers, so P01 must hold every policy in force at go-live that expires up to 31 May 2028, and every renewal term booked in legacy that starts on or after go-live (so that the expiring term is recognised as renewed). The P01 reconciliation against the legacy in-force list and the go-live extraction check (per expiry month, headers = candidates + renewals booked in legacy) prove that no expiry is missed.
- **Renewal advices already sent (P03).** The RMEL and the dispositions are kept in Excel trackers today (DMQ38, register DCR-243). The migration takes only the RAs already sent from them, in a small reference file: one row per expiring term with the legacy policy reference, cover, expiry, RA date and reference, channel, recipient, proposed insurer and premium quoted, sender, and the tracker and sheet it comes from.

<!-- table: widths=3.2,9.6,3.8 caption="Excel intake of the RA-sent file (DMQ38)" size=8.5 -->
| Step | What happens | Who |
|---|---|---|
| Template | The load template workbook of object P03, exported by the Migration Console (first sheet with the header row, the Columns sheet with the allowed values, the How to fill sheet); the CSV template is also accepted | Migration Console |
| Compilation and check | The maker copies the RA rows of the Retail and Corporate trackers into the template; the checker compares the file with the trackers (counts per tracker, 10 sample rows) and releases it | Renewal processing team - maker, checker |
| Validation | Header in P01; expiry equal to the header and from go-live to 31 May 2028; RA date not after the last legacy business day (warning when more than 140 days before expiry); cover not already renewed in legacy; one row per expiring term | BIBS |
| Rejection report | The rejected rows of the P03 batch in Excel (Rejects on the batch) with the row, column, value and message, and columns for the maker's correction and the checker's review | BIBS |
| Resubmission | The maker corrects the rows and uploads a resubmission file with the corrected rows only (Resubmit Corrected Rows); the checker reviews each correction against the tracker and approves it on the Resubmissions tab of Batches (never the same person); it loads as a rerun batch. In production resubmissions close at 12:00 on the day before go-live; rows still rejected are checked against the tracker before any RA is sent | Maker, checker; Migration Operator |

The Head of the Renewal processing team owns the object (signs G1, G2 and G6). In the trial migrations the file is sent from Trial migration 1; in production once, with the RAs sent up to the last legacy business day.

**The January lead time.** The January expiries get days to four weeks instead of 140 days for insurer requests and RAs (risk 16). The mitigation: the day-1 priority queue (go-live extraction at 04:00, January expiries flagged urgent, earliest expiry first); a staffing plan for January signed by the Head of the Renewal processing team; and a Renewal team that is trained and has worked the day-1 queue on the Trial migration 4 and dress-rehearsal data before go-live.

A legacy policy renews on the new-business path pre-filled from its header, or as is when the Renewal sanitation resolves its package. The renewal account refers to the legacy reference, so the run-off tracker can link them. The Run-off and Decommissioning screen and the Legacy Run-off report show, per expiry month, the legacy policies in force at go-live, renewed in BIBS, not renewed, lapsed and still open. Legacy systems are decommissioned when their last cohort has run off and the other checklist criteria are met (Part D, Decommissioning checklists).

# History, archive and read-only access

History is not migrated into BIBS business tables. For each legacy system BDOI chooses how history stays available (DMQ24, DCR-209):

<!-- table: widths=3.6,6.6,6.4 caption="Options for history" size=9 -->
| Option | What it means | When |
|---|---|---|
| Read-only legacy | The legacy application stays up in read-only mode, with its own access logging (BDOI IT). Legacy Inquiry shows its address to the users who still need it | From the freeze until the system is decommissioned |
| BIBS archive | Closed transactions, expired policies, GL history and documents are loaded into the BIBS archive (layouts H01 and H02) and searched in Legacy Inquiry | Before a legacy system is decommissioned |

**Legacy Inquiry** searches the archive by client, policy or cover number, invoice, receipt, claim, date and record type, and shows each record read-only with its documents. Every search, view, download and export is logged (user, time, criteria, records, reason); the log cannot be changed; Compliance reviews it on screen and receives a monthly digest (FR-DM-110, FR-DM-111).

**Retention.** Archive records and access logs follow the retention rules: the umbrella BRD gives 5 years online and 15 years archive for historical data (R5 p.45). Reconciliation reports, sign-offs and run logs are kept as project records and are not purged with the staging data (proposed 10 years, DMQ29).

**Archive reconciliation.** Each archive load is reconciled against legacy by counts, amount totals and hash totals per record type, and Audit / Compliance checks samples in Legacy Inquiry before the system is decommissioned.

# Migration cycles

## Trial migrations, dress rehearsal and production

The migration is run five times before it counts, inside the SIT and UAT migration windows of the BDOI timeline and the full migration and cut-over window. Each run is a cut-over plan in the Migration Console (Cutover), so timings and issues are measured and the production cut-over repeats a rehearsed plan.

**Trial-migration load order.** In every run, reference data (R01-R07) and the client master (C01-C03) are loaded and accepted first, before any policy header, RA-sent file or open item. This follows the load order, and it gives the SIT and UAT of the Drop 1 modules, Renewal first, migrated clients and reference data to test with from the first trial migration: the prerequisite the concept paper sets for renewal (R10 section VI).

![Migration cycles on the BDOI timeline (go-live January 2028)](figures/dm_cycles.dot){width=15}

<!-- table: widths=2.2,2.4,2.6,4.7,4.7 caption="Migration cycles with entry and exit criteria" size=8 -->
| Cycle | When / where | Data and objects | Entry criteria | Exit criteria |
|---|---|---|---|---|
| Trial migration 1 | 19-30 Apr 2027; SIT | Masked full extracts; reference data and clients first, then policy headers and the RA-sent file (P03) | SIT environment set up with the Migration Console and the Renewal module by 9 Apr 2027; layouts frozen (M2); draft code maps; extracts for Trial migration 1 received (M4) | Objects loaded; L1-L4 run; issues logged; timing per object recorded; profiling report to the owners; P03 rejects reviewed by the Renewal processing team (maker-checker) |
| Trial migration 2 | 5-16 Jul 2027; SIT | Masked full extracts; all objects | Trial migration 1 exit met; code maps approved (M5); legacy accounts and accounting rules set up on SIT by Comptrollership by 18 Jun 2027 | All objects loaded; L1-L5 reconciled; Migration Clearing 0.00 per branch and currency; no open Critical migration issue |
| Trial migration 3 (UAT load) | 2-13 Aug 2027; UAT | Masked extracts refreshed; all objects | Trial migration 2 exit met; UAT readiness statement issued (R7, programme item 11) | Business owners verify samples on screen (G6 rehearsal); the Drop 1 end-to-end UAT (Aug-Dec 2027) runs on the migrated data; data-quality issues below the thresholds |
| Trial migration 4 (UAT refresh) | 4-15 Oct 2027; UAT | Fresh masked extracts; all objects; go-live renewal extraction of the January-May 2028 expiries; a test true-up | Trial migration 3 exit met; cut-over date, year-end option, fallback and decommissioning criteria agreed (M6) | Run as a timed cut-over; go-live extraction check balanced and the day-1 queue worked by the Renewal team; test true-up reconciled; UAT continues on the refreshed data |
| Dress rehearsal | 15-26 Nov 2027 (reserve 6-10 Dec 2027); production-sized environment | Masked full-volume extracts taken after a legacy EOD | Trial migration 4 exit met; production-sized environment (performance test window); cutover plan frozen | Full cutover within the window with at least 20 percent margin; rollback (snapshot restore) rehearsed; go / no-go criteria measured |
| Production | 20 Dec 2027 (T-14) to 3 Jan 2028 (T) | Real data in production only | GNG-1 passed (Part D) | GNG-3 GO; hypercare exit criteria (Part D) |

Issues found in a cycle are resolved before the next one: in legacy (cleansing), in the code maps, in the layouts or in the load settings. The next cycle uses fresh extracts, so every fix is proven with new data.

## Test data and masking

SIT and UAT receive masked extracts only. Masking is done at intake in BIBS with a keyed, repeatable method: the same person gets the same masked name and numbers in every file, so dedupe and matching behave as in production. Amounts, codes and transaction dates are not masked, so reconciliation and business verification work on real figures. Unmasked extracts never leave production (DMQ31, DCR-200).

# Cutover strategy

## Approach

- **Big bang for open items.** All open items (invoices, UPP, collection state, remittance items, PDCs), the policy headers, the RA-sent file and the provisional GL opening are loaded once, after the legacy freeze, over one weekend. They change every day, so a delta approach would add reconciliation risk for no benefit.
- **Pre-load and deltas for master data.** Reference data and clients are loaded two weeks before go-live and kept current with daily client deltas until the freeze. This takes the largest objects out of the cutover window.
- **Year-end cut-over.** The BDOI timeline sets go-live in January 2028 (DMQ25). The recommendation below (DMQ39, register DCR-242) awaits Comptrollership confirmation at M6 (1 October 2027).

<!-- table: widths=3.6,6.6,6.4 caption="Year-end cut-over options (DMQ39)" size=8.5 -->
| Option | What it means | Assessment |
|---|---|---|
| **A. Go-live at the year-end boundary (recommended)** | Go-live Monday 3 January 2028. Legacy processes to 31 December 2027 and closes FY2027. BIBS opens with the open items at 31 December and a provisional GL opening trial balance from the preliminary December TB: balance sheet only, the FY2027 result in retained earnings, P&L at zero in FY2028. The legacy GL stays open only for FY2027 closing and audit adjustments, restricted to Comptrollership, with no new business. Each adjustment reaches BIBS as a controlled opening-balance adjustment journal in the opening period: true-up 1 after the legacy year-end close (about mid to late January 2028), the final true-up after the audited financial statements (about March-April 2028). December soft close by about 20 December 2027. FY2027 BIR annual returns and the FY2027 audit from legacy; FY2028 from BIBS | Recommended. One fiscal year per system; no P&L migration; books of accounts and BIR returns never split inside a year; the late adjustments are few, approved and reconciled |
| B. Go-live after the first-quarter close (April 2028) | Legacy runs January-March 2028; BIBS opens in April with the open items and the TB at 31 March, including the year-to-date P&L by account, branch and month | Not recommended. Go-live moves three months off the BDOI timeline; FY2028 is split across two systems (first-quarter books and returns from legacy, the rest and the annual returns from BIBS); the year-to-date P&L must be migrated and reconciled month by month; the FY2027 audit and the renewal peak fall in the cut-over period |
| C. Two books in parallel for the first quarter of 2028 | BIBS goes live in January and legacy also keeps the books for January-March | Not recommended. Every transaction is keyed twice for three months; two ledgers must be reconciled every day and diverge on every rule difference; it breaks the rule of one processing system per item (BRD p.5); the book of record for BIR is unclear; the extra work falls in the January renewal peak |

If BDOI wants more assurance in January, Comptrollership **compares reports instead of keeping two books**: the BIBS opening reports against their legacy counterparts at 31 December (trial balance; premium receivable, DTIP, commission receivable and UPP by insurer and age), and the January month-end reports of BIBS (trial balance, GL schedules, Cash Receipts and Cash Disbursements Books, remittance schedules) against the opening position and the January activity.

**True-ups and their controls.** The last legacy business day is Wednesday 29 December 2027 (30 and 31 December are holidays); the last legacy EOD runs on 31 December and the business freeze follows. From then on:

<!-- table: widths=0.8,9.2,6.6 caption="FY2027 true-up controls" size=8.5 -->
| # | Control | Evidence |
|---|---|---|
| 1 | Legacy business modules read-only from the freeze; no new business in legacy | Freeze test in the runbook |
| 2 | Legacy GL open only for FY2027 periods and only for the Comptrollership users named on a signed access list; FY2028 periods never opened in legacy | Access list; legacy period status |
| 3 | Every legacy journal after the freeze entered in the FY2027 adjustment register (kind closing or audit, reason, preparer, approver) | Register |
| 4 | Each true-up is prepared by the Comptrollership GL lead and approved by the Head of Comptrollership (never the same person); it posts as an opening-balance adjustment journal dated 1 January 2028; FY2027 P&L effects go to retained earnings; adjustments on the legacy control accounts come with their invoice or UPP detail | Opening-Balance Adjustments screen |
| 5 | Each true-up is reconciled before sign-off: cut-off (legacy journal listing = register = true-up), movement (true-up = change of the legacy TB), balance (BIBS opening = legacy TB), Migration Clearing 0.00 | Opening-Balance Adjustment Reconciliation report, signed (Part C) |
| 6 | When January 2028 is already closed, it is reopened for the posting only and closed again the same day; no other journal may post in the window | Period audit trail |
| 7 | After the final true-up the legacy GL is locked and the true-ups are closed (about 2 May 2028); a later FY2027 finding is a prior-period adjustment in BIBS outside the migration | Closure sign-off |

## Freeze windows

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-freeze
```

## Parallel run

There is no parallel run of legacy and BIBS on the same items. A parallel run would need each receipt, application, remittance and endorsement keyed twice and reconciled daily, and it contradicts the principle that an open item is processed in one system only (BRD p.5). The controls that replace it are the four trial migrations and the dress rehearsal on full extracts, the go / no-go criteria measured on production data, the rollback point until the go / no-go, and the daily hypercare reconciliation after go-live.

## Go / no-go

Three checkpoints decide the cutover. The criteria are measured by BIBS and recorded with their values (FR-DM-121). GNG-3 is the go-live decision on the Sunday before go-live at 18:00 (2 January 2028 for the recommended date):

<!-- table: widths=0.8,9.6,6.2 caption="Go-live criteria (GNG-3)" size=9 -->
| # | Criterion | Threshold |
|---|---|---|
| 1 | Day-1 objects accepted (G6) | 100 percent |
| 2 | Count reconciliation per object | Received = control; loaded + skipped + rejected + excluded = staged |
| 3 | Financial rejects (open invoices, UPP, trial balance) | 0, or each excluded item approved with a manual-entry plan |
| 4 | Amount reconciliation of open items, UPP and trial balance | 0.00 per currency |
| 5 | Migration Clearing | 0.00 per branch and currency |
| 6 | Legacy control accounts vs legacy sub-ledgers | 0.00 |
| 7 | Client review queue; screening run | Empty; complete |
| 8 | Business smoke test on production | Passed |
| 9 | Rollback point (snapshot) | Taken and verified |
| 10 | Hypercare roster and support channels | In place |
| 11 | Preliminary December TB signed as the provisional opening; legacy GL restricted to the named FY2027 adjustment users | Signed |
| 12 | Headers of every expiry up to 31 May 2028 loaded; RA-sent file loaded; Renewal January staffing plan | Complete; confirmed |

GNG-1 (T-15, start the pre-load) and GNG-2 (T-5, enter the freeze) are in Part D.

## Rollback

Until the go / no-go decision the rollback is a restore of the production backup (snapshot) taken at the start of the production load, and legacy is reopened for business on Monday. After go-live, until the point of no return at 18:00 on the first business day, the same restore is possible with re-keying in legacy of the transactions keyed in BIBS that day. After the point of no return, issues are fixed forward in BIBS (DMQ32, DCR-200). The steps are in Part D.

## Hypercare

Hypercare runs from go-live to the first month-end close. Every day: Migration Clearing 0.00, legacy control accounts against their sub-ledgers, automatch results, the urgent January renewals, exception queues and failed jobs; issue triage twice a day. True-up 1 is posted and reconciled before the January close. Exit requires no open Critical or High issue, 10 consecutive business days of clean daily checks, true-up 1 signed, and the first month-end close with the ACSL GL-SL reconciliation (legacy ledger context) without difference.

## In-flight items at the freeze

Items in process in legacy at the freeze (pending endorsements, un-booked accounts, placements, PDCs, check pick-ups, refunds, remittance batches) are, in order of preference, completed in legacy before the freeze, carried forward in a layout (F04, F06), or re-keyed in BIBS after go-live from a list signed by the business owner (DMQ33, DMQ21). A renewal placed or bound in legacy but not booked is such an item: it is booked in legacy before the freeze so that its new term migrates as a header. The runbook schedules the completion drives from T-12.

## Decommissioning

Each legacy system is decommissioned only against a signed checklist (FR-DM-123): final extracts reconciled, for EBIX and ISYS the final true-up reconciled and the legacy GL locked, archive loaded and reconciled, Legacy Inquiry verified by Audit / Compliance, no open inquiry or claim that needs the system, the last legacy-booked policy expired plus the claims tail, access logs archived, retention covered, and sign-off by the system owner, Compliance and Comptrollership (DMQ27). The checklists per system are in Part D.

# Data security

The migration follows the hosting appendix (R7) and the security controls of BIBS.

<!-- table: widths=4,12.6 caption="Data protection controls" size=9 -->
| Control | How it is applied |
|---|---|
| Hosting | All migration data (intake bucket, staging, archive) is in the BIBS environment in AWS ap-southeast-1 |
| Masked non-production data | Every non-production environment (SIT, UAT, dress rehearsal) receives data masked at intake; unmasked extracts never leave production |
| Staging purge within 5 days | Staged records and extract files are deleted within 5 days of the batch sign-off or rollback, or of an extract rejection (daily purge of the staging area and of the file intake area); an alert is raised when a signed-off batch still has staged records after the limit. Counts, hashes and totals are kept as evidence |
| Access restricted to the Philippines | The Migration Console, the staging area and the file intake area are reachable only by migration roles and only from the Philippines (network restrictions of the infrastructure team, specified in the Technical Specification, reviewed by BDOI IT) |
| Encrypted transfer and storage | Files move through the console or the secure file drop over encrypted connections; stored files are encrypted; never by e-mail or removable media |
| Access logging | Every console action (upload, validation, load, rerun, rollback, reconciliation, sign-off, decision) is audited; every access to archived history is written to the legacy access log, which no user can change |
| Segregation of duties | The maker of a decision, map version, load or rollback never approves it; the operator of a batch cannot sign its reconciliation or acceptance |
| Least privilege | Migration roles are granted through User Access requests for the migration period and removed at hypercare exit |

# Roles and responsibilities

## Migration organisation

- **Go / no-go board**: BDOI Program Manager (chair), Heads of Operations, Comptrollership, Marketing and Compliance, BDOI IT head, iorta TechNXT Project Manager. Decides at GNG-1, GNG-2, GNG-3 and the point of no return.
- **BDOI Data Migration Lead**: runs the migration on the BDOI side: object register, decisions, load approvals (G4), acceptance with the owners (G6), cutover plan.
- **Data owners**: department heads who own an object; approve its class, code maps, waivers and acceptance.
- **Data stewards**: named staff per object; profile review, cleansing at source, code maps, client match review.
- **BDOI IT**: extracts, control files, transfer, legacy freeze and read-only mode, archive extracts, decommissioning.
- **Comptrollership**: legacy control accounts, Migration Clearing and accounting rules; reconciliation approver for financial objects (G5).
- **iorta TechNXT**: Project Manager; Migration Lead (loads, reconciliation, profiling, the Migration Console); Migration Operator (intake, validation, loads); infrastructure team (environments, snapshots, access); test team (smoke tests, migration test plan).

## RACI

R = responsible, A = accountable, C = consulted, I = informed.

<!-- table: widths=5.6,1,1,1,1,1,1,1,1,1,1,1 caption="RACI. PM = BDOI Program Manager; DML = Data Migration Lead; OWN = data owners; STW = data stewards; BIT = BDOI IT; COMP = Comptrollership; CMP = Compliance; IPM = iorta Project Manager; IML = iorta Migration Lead; IOP = iorta Operator and infrastructure team; GNG = go / no-go board" size=8 -->
| Activity | PM | DML | OWN | STW | BIT | COMP | CMP | IPM | IML | IOP | GNG |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Migration approach and this handbook | A | R | C | I | C | C | C | R | R | I | I |
| Object register and decisions (G1) | I | R | A | C | C | C | C | I | C | I | I |
| Extract layouts and templates | I | A | C | C | R | C | I | I | R | I | - |
| Extracts and control files | I | A | I | C | R | I | I | I | C | I | - |
| Secure transfer and intake | I | A | - | - | R | - | C | I | C | R | - |
| Profiling report | I | I | I | C | I | I | - | I | R | C | - |
| Cleansing at source | I | A | C | R | R | C | - | I | C | - | - |
| Code maps (G2) | I | C | A | R | C | C | - | I | C | - | - |
| Client matching and survivorship | I | C | A | R | I | - | C | I | R | C | - |
| Validation and waivers (G3) | I | C | A | R | I | C | - | I | R | C | - |
| Load approval (G4) and loads | I | A | I | I | I | I | - | I | C | R | - |
| Reconciliation (G5) | I | C | C | C | I | A | - | I | R | C | - |
| Legacy accounts, clearing, rules | I | I | C | - | - | A | - | I | C | - | - |
| Business verification and acceptance (G6) | I | R | A | R | - | C | - | I | C | - | - |
| Trial migrations and dress rehearsal | A | R | C | C | R | C | I | R | R | R | I |
| Legacy freeze and read-only mode | C | A | I | - | R | I | I | I | I | - | I |
| Go / no-go (G7) | R | C | C | - | C | C | C | C | C | - | A |
| Rollback | C | C | - | - | R | I | - | R | R | R | A |
| Hypercare | A | R | C | C | C | C | I | R | R | R | I |
| RA-sent file (P03) - template, rejects review, resubmission | I | C | A | R | - | - | - | I | C | R | - |
| Provisional GL opening and FY2027 true-ups | I | C | - | - | R | A | - | I | R | R | I |
| Archive loads and Legacy Inquiry verification | I | R | C | C | R | C | A | I | R | C | - |
| Decommissioning | A | R | C | - | R | C | C | I | C | - | I |

# Risks and mitigations

<!-- table: widths=0.9,5.4,1.5,8.8 caption="Migration risks" size=8 -->
| # | Risk | Impact | Mitigation |
|---|---|---|---|
| 1 | Legacy cannot give open balances per component or the paid / remitted split (DMQ12, DCR-207) | High | Profiling of the first full extracts (29 January 2027); open-balance mode (the open balance loaded as booked; the Legacy Invoice card says so); split rules as configuration agreed with Comptrollership |
| 2 | Migration Clearing not 0.00 at cutover (detail and trial balance disagree) | High | Reconciliation in every trial migration from Trial migration 2; trial balance extracted after the same EOD as the detail; break explanations agreed with Comptrollership before GNG-3 |
| 3 | Duplicate clients merged wrongly, or not merged | High | Auto-merge only on hard keys; review queue for the rest; every lost value kept; client batches can be rolled back before sign-off |
| 4 | Answers to the scope-shaping decisions arrive late (M1) | High | Decisions listed with dates in the chapter Proposed business rules and clarifications for confirmation; defaults in configuration where possible, so late answers change configuration |
| 5 | Load time exceeds the cutover window | Medium | Pre-load of reference data and clients; four partitions; dress rehearsal at full volume with 20 percent margin |
| 6 | Endorsements of legacy invoices without policy headers (BRID 9 depends on BRID 4.1) | High | Decide DMQ09 / DMQ22 by M1; proposal: headers of every in-force policy |
| 7 | Real personal data in a test environment | High | Masking at intake; unmasked files never leave production; 5-day purge; access only from the Philippines |
| 8 | Legacy postings after the freeze | High | Legacy set read-only at T-3 22:00 and tested; any late legacy transaction is a reconciliation break and is re-keyed in BIBS |
| 9 | In-flight items forgotten at the freeze | Medium | Completion drives from T-9; carry-forward layouts F04, F06; signed re-keying list |
| 10 | Code maps incomplete when transactions load | Medium | Unmapped-code report before load; gate G3 blocks the batch; map freeze at T-7 |
| 11 | BDOI steward capacity for cleansing and review | Medium | Named stewards per object by M2; profiling report per trial migration with the top issues; review queue sized from Trial migration 1 |
| 12 | Legacy system decommissioned before history is archived | Medium | Decommissioning only against the signed checklist; archive reconciled and verified by Audit / Compliance |
| 13 | Foreign-currency openings at the wrong rate (DMQ35) | Medium | Rate decision by M2; booking rate carried in the extract; reconciliation per currency |
| 14 | The draft BRD changes when it is signed (DCR-189) | Medium | This set is re-issued; decisions are configuration where possible |
| 15 | Cut-over over the year-end holidays (24 December 2027 to 1 January 2028) | High | Year-end option confirmed by Comptrollership at M6 (DMQ39); tasks moved off holidays in the runbook; roster confirmed at T-29 |
| 16 | Tight lead time for the January 2028 expiries, processed in BIBS after go-live (DMQ37) | High | Day-1 priority queue (go-live extraction at 04:00, January expiries urgent, earliest first); January staffing plan signed at T-20; Renewal team trained and rehearsed on the Trial migration 4 and dress-rehearsal data; RAs already sent loaded so none is re-sent |
| 17 | Many candidates in the Renewal Exception bucket because packages are remapped at sanitation (DMQ36) | Medium | package code map loaded and tested in every trial migration; profiling of the legacy packages of the headers expiring up to 31 May 2028 without an entry, closed by TSU before the map freeze; each Exception-bucket choice fed into the next map version |
| 18 | A header missing from P01 means a renewal never extracted | High | P01 scope includes every expiry up to 31 May 2028 and every booked renewal term starting on or after go-live; P01 reconciled to the legacy in-force list; extraction check at go-live; go / no-go criterion |
| 19 | Late or large FY2027 adjustments after go-live (provisional opening, DMQ39) | Medium | December soft close by 20 December 2027; legacy GL restricted to named users and FY2027 periods; adjustment register; cut-off checks and reconciliation at every true-up; true-ups closed about 2 May 2028 |

# Dependencies

<!-- table: widths=1.2,9.4,3.2,2.8 caption="Dependencies" size=8.5 -->
| # | Dependency | Owner | Needed by |
|---|---|---|---|
| D1 | SIT environment with the Migration Console and the seed users of the migration roles | iorta TechNXT | Trial migration 1 (by 9 Apr 2027) |
| D2 | UAT and production-sized environments with the migration settings of each environment | iorta TechNXT | Trial migration 3; dress rehearsal |
| D3 | Performance run at full volume on the production-sized environment | iorta TechNXT | Dress rehearsal |
| D4 | Renewal module (BRD-6) in SIT for Trial migration 1 and live at go-live, with the go-live extraction (priority, urgent flag, RAs already sent) and the package check of the sanitation with the Exception bucket | iorta TechNXT | Trial migration 1; go-live |
| D5 | Legacy control accounts, Migration Clearing and the legacy accounting rules set up by Comptrollership | BDOI Comptrollership | Trial migration 2 (SIT); T-25 (production) |
| D6 | Extracts in the templates with control files, and volumes per object | BDOI IT | M3 (29 Jan 2027), M4 (9 Apr 2027) and every trial migration |
| D7 | Named data owners and stewards | BDOI | M2 |
| D8 | Legacy systems can be set read-only with access logging; secure file drop and document transfer folder named (PR-DM-03, PR-DM-04) | BDOI IT | Dress rehearsal |
| D9 | Users of every persona created through User Access requests | BDOI and iorta TechNXT | T-24 |
| D10 | Renewal processing team - RA-sent file from the trackers, maker-checker review of rejects, January staffing plan and training | BDOI Renewal processing team | Trial migration 1 (first file); T-20 (readiness); T-5 (final file) |
| D11 | Comptrollership - confirmation of option A, December soft close, legacy GL access list and FY2027 adjustment register, true-up preparation and approval | BDOI Comptrollership | M6; 20 Dec 2027; T-5; T+15 to T+120 |
| D12 | External audit of FY2027 timed so that the audited financial statements are available by about April 2028 | BDOI Comptrollership | Final true-up (T+92) |

The open decisions of BDOI, with the date each is needed by, are in the chapter Proposed business rules and clarifications for confirmation.

# Timeline

## Alignment with the BDOI timeline

The migration timeline follows the BDOI drop plan and timeline (R9): Drop 0 "Setup & Data Migration", go-live January 2028 (DMQ25), recommended Monday 3 January 2028 at the year-end boundary (DMQ39). It ties into the SIT and UAT of the programme and into the UAT readiness programme (R7) as follows.

<!-- table: widths=3.4,5.2,8 caption="Migration timeline on the BDOI calendar" size=8.5 -->
| When (BDOI timeline stream) | Migration step | Link to SIT, UAT and go-live readiness |
|---|---|---|
| 5 Oct 2026 (M0) | This business sign-off set issued (release set v2.0) | Business review until the sign-off on 30 Oct 2026 |
| 16 Oct 2026 (M1) - requirements and mapping | Remaining scope-shaping decisions answered (package remapping DMQ36 and the January-May 2028 renewals DMQ37 answered on 26 Sep 2026) | Proposed rules decided (chapter Proposed business rules) |
| 30 Oct 2026 (M2) - requirements and mapping | Owners, stewards, keys, remaining rules; object decisions signed (G1); layouts frozen, version 1 | Sign-off of this set; the first full extracts are prepared in the frozen layouts; migration test plan agreed |
| Nov 2026 to Mar 2027 | Code maps drafted from the profiling; legacy accounts and accounting rules prepared by Comptrollership; SIT environment set up | SIT environment ready by 9 Apr 2027 |
| 29 Jan 2027 (M3) | First full extracts with volumes; profiling | Performance run sized from the volumes |
| 9 Apr 2027 (M4) - SIT migration | Extracts for Trial migration 1 | - |
| 19-30 Apr 2027 - SIT migration | Trial migration 1 on SIT: reference data and clients first, then headers and the RA-sent file | Drop 1 SIT (Renewal first) tests on migrated clients and reference data |
| 18 Jun 2027 (M5) | Code maps approved for Trial migration 2, including the package code map loaded for Renewal | - |
| 5-16 Jul 2027 - SIT migration | Trial migration 2 on SIT, all objects, L1-L5 | Provides the "data migration dry run with reconciliation" of UAT readiness item 11 |
| 2-13 Aug 2027 - UAT migration | Trial migration 3 = UAT load | UAT readiness statement issued; the Drop 1 end-to-end UAT (Aug-Dec 2027) runs on migrated data |
| 1 Oct 2027 (M6) | Cut-over date and window, fallback and decommissioning criteria agreed; Comptrollership confirms the year-end option A (DMQ39) | - |
| 4-15 Oct 2027 - UAT migration | Trial migration 4 = UAT refresh, timed as a cut-over, with the go-live renewal extraction and a test true-up | Business owners verify migrated data during UAT; the Renewal team works the day-1 queue |
| 15-26 Nov 2027 - full migration and cut-over; performance test | Dress rehearsal on the production-sized environment (reserve 6-10 Dec 2027) | Performance and penetration test Nov-Dec 2027 |
| 4 Dec 2027 (T-30) to 3 Jan 2028 (T) - full migration and cut-over; ORR / PRR | Production cut-over (Part D): December soft close by 20 Dec, pre-load from 20 Dec, freeze 31 Dec 22:00, load and provisional GL opening 1 Jan, go / no-go 2 Jan 18:00, go-live renewal extraction 3 Jan 04:00 | ORR / PRR Dec 2027-Jan 2028; production release frozen from T-7 |
| 3 Jan 2028 (T) to the January month-end close | Hypercare; day-1 renewal queue, January expiries first; true-up 1 after the legacy year-end close (about 18-21 Jan 2028) | Production support runbook (deliverable 24) |
| About March to 2 May 2028 (T+85 to T+120) | Final true-up after the audited FY2027 financial statements; legacy GL locked; true-ups closed | FY2027 BIR annual returns and audit from legacy |
| Run-off (about 12-15 months) | Monthly run-off tracking; archive loads; decommissioning | - |

If the extracts for Trial migration 1 are late, Trial migration 1 moves within the SIT migration window (to May 2027) and Trial migration 2 stays in July; the reserve slot of the dress rehearsal (6-10 December 2027) is the last buffer before the cut-over.

# Part B - Functional specification of the Migration Console {-}

Part B specifies the Migration Console screen by screen: who uses each screen, what it shows (screenshot and fields), what the user can do, the rules it applies, the messages it gives and the outcome of each action. It then specifies how migrated data is marked on the business screens (the LEGACY badge and the Origin filter) and the legacy batches of Cashiering and Commission (Unapplied to Income, Legacy PR 2307 Reversal and DP PR Legacy Reversal), and gives the functional requirements with their acceptance criteria, the messages, the reports, the states and the settings.

# Personas, roles and navigation

## Personas

The migration roles are given to named people through User Access requests for the migration period and removed at hypercare exit. The business personas at the end of the table work the migrated records and the legacy batches with their usual roles.

<!-- table: widths=4.4,8.2,4 caption="Personas of BRD-13" size=8.5 -->
| Persona | Responsibilities | BRD |
|---|---|---|
| Data Migration Lead | Runs the migration on the BDOI side: object register and decisions, load approvals (G4), acceptance with the owners (G6), rollback requests, cut-over plans, decommissioning checklists | BRID 1.1a, 1.1b, 12.1 |
| Data Steward | Code maps and layouts; data-quality issues; doubtful client matches; signs the validation (G3) | BRID 2.1, 3.1 |
| Data owner (business owner of an object) | Decides the class (G1), approves the code map versions and signs the mapping (G2), waives or excludes rows, accepts the object (G6) | BRID 1.1a, 3.1 |
| Migration Operator | Uploads extracts, plans, validates, loads and reconciles batches | BRID 1.1b |
| Reconciliation approver (Head of Comptrollership for financial objects) | Approves break explanations, signs the reconciliation (G5), approves rollbacks and the opening-balance adjustments | BRID 1.1b |
| Go / no-go board | Measures and records the criteria and decides GO or NO-GO (G7 on the production plan) | BRID 12.1 |
| Comptrollership GL lead | Prepares each opening-balance adjustment | BRID 1.1b |
| Audit / Compliance user | Searches, views, downloads and exports the legacy archive | BRID 11.1 |
| Compliance reviewer | Reviews the access log of the archive and its monthly digest | BRID 11.1 |
| Renewal processing team (maker and checker) | Compiles the renewal advices already sent from the Excel trackers, corrects rejected rows (maker) and approves resubmissions (checker) | BRID 12.1 |
| Cashiering user and team lead; top management | Unapplied to Income and Legacy PR 2307 Reversal batches; payments and dispositions on legacy items | BRID 5.1-5.5, 6.1-6.4, 7.2 |
| Commission user and team lead | DP PR Legacy Reversal batches | BRID 7.1 |
| Marketing, Operations, Remittance and Prod Recon users | Find and use migrated clients, policies and invoices on their usual screens | BRID 2.1, 4.1, 8.1-10.1 |

Segregation of duties is enforced by BIBS whatever the roles held: the maker of a decision, code map version, load, rollback, reclassification batch, reversal batch, opening-balance adjustment or resubmission never approves it; the operator who ran a batch cannot sign its reconciliation or acceptance; one person cannot sign two gates of the same batch.

## Screens of BRD-13

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: screen-index
```

## Screen flow

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: flow
```

## Menu by persona

The Migration Console and Legacy Inquiry are sections of the sidebar group Data Migration. Each persona sees only the entries of its role; the menus below are read from the role grants of BIBS.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: menus
```

# Common screen elements

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: common
```

<!-- landscape -->

# Screen specifications

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: screens
```

<!-- portrait -->

# Functional requirements

## Governance and decision gates

```fr
id: FR-DM-001
title: Maintain the data object register
brd: [BRID 1.1a (p.7)]
actor: Data Migration Lead
priority: Must have
screens: Data Objects
description:
  - BIBS keeps one register of every data object that could be migrated, with its source systems, target in BIBS, business owner, data steward, dependencies and load order. The register starts from the catalogue of data objects of this handbook (Part A, Data object register), for example clients, policy headers, open legacy invoices, UPP, GL trial balance and history.
  - For each object the Data Migration Lead records the four criteria of the BRD, Day-1 need, compliance need, read-only / archival option and data trust, and proposes a class.
preconditions:
  - The user has the right "Data migration object manage".
main_flow:
  - The Data Migration Lead opens Data Objects and chooses New Object or an existing object.
  - The lead enters or updates the object fields and the four criteria.
  - The lead chooses the proposed class (Migrate, Carry-forward, Archive, Excluded or Conditional) and writes the rationale.
  - BIBS saves the object in status Proposed and shows it in the register with its dependencies.
alternate_flows:
  - Object with dependencies. BIBS lists the objects it depends on and refuses a load order lower than theirs.
  - Conditional object (for example policy headers, BRID 4.1). The lead enters the condition; the object cannot be loaded until the decision (FR-DM-002) states the condition as met.
rules:
  - [R1, "Object codes are unique; an object is never deleted, only set to Excluded.", Fixed, "-"]
  - [R2, "Classes are Migrate, Carry-forward (open items), Archive, Excluded and Conditional.", Fixed, "-"]
  - [R3, "Data trust is High, Medium or Low; Day-1 need, compliance need and read-only / archival option are Yes / No with a note.", Configurable, "the list of data trust levels"]
  - [R4, "An object loads only after every object it depends on is accepted in the same environment (gate G6).", Fixed, "-"]
validations:
  - [Code already used, This object code already exists, "-"]
  - [Load order lower than a dependency, The load order must follow the objects this object depends on, "-"]
  - [No rationale, Enter the rationale for the proposed class, "-"]
fields_screen: Data Object
fields:
  - [Object code, Text, "Yes", "-", "Unique; letters and digits"]
  - [Name, Text, "Yes", "-", "Up to 120 characters"]
  - [Category, List, "Yes", "the list of object categories", "Reference, Client, Policy, Open item, GL, History"]
  - [Source systems, Multi-select, "Yes", "the list of source systems", "EBIX, QPS, ISYS, Excel, CMS"]
  - [Target in BIBS, Text, "Yes", "-", "Module and record"]
  - [Business owner, User, "Yes", "Users with data owner", "-"]
  - [Data steward, User, "Yes", "Users with Data Steward", "-"]
  - [Day-1 need, Yes / No + note, "Yes", "-", "-"]
  - [Compliance need, Yes / No + note, "Yes", "-", "-"]
  - [Read-only / archival option, Yes / No + note, "Yes", "-", "-"]
  - [Data trust, List, "Yes", "High, Medium, Low", "-"]
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
  - A new object is saved as Proposed with its four criteria and shown in the register.
  - An object cannot be given a load order before an object it depends on.
  - An Excluded object stays in the register and cannot be loaded.
```

```fr
id: FR-DM-002
title: Decide the migration class of a data object
brd: [BRID 1.1a (p.7)]
actor: Data Migration Lead (maker); business owner (approver)
priority: Must have
screens: Data Objects (Decision tab); My Approvals
description:
  - The class of an object is decided through a recorded approval (gate G1). The Data Migration Lead submits the proposal; the business owner of the object approves or returns it. The approved class is the one BIBS enforces.
  - This meets the BRD acceptance criterion that each data object is "actioned upon" as Migrate / Carry-Forward (Open Items) / Archive / Excluded once the business owners have assigned the four criteria.
preconditions:
  - The object is Proposed or its decision was returned.
main_flow:
  - The Data Migration Lead opens the object and clicks **Submit Decision**.
  - BIBS moves the object to For decision and puts it in the business owner's My Approvals.
  - The business owner reviews the criteria and the rationale and clicks **Approve**.
  - BIBS sets the object to Decided with the class and records the decision with date and approver.
alternate_flows:
  - Return. The business owner returns the decision with a reason; the object goes back to Proposed.
  - Change of class later. A new decision is submitted and approved; the history keeps every decision. A change from Migrate or Carry-forward to Archive or Excluded is refused while a batch of the object is loaded and not rolled back.
rules:
  - [R1, "The approver is the business owner of the object and never the submitter.", Fixed, "-"]
  - [R2, "Only Decided objects of class Migrate or Carry-forward (or Conditional with the condition met) can be loaded.", Fixed, "-"]
  - [R3, "Archive objects are loaded only into the legacy archive (FR-DM-110).", Fixed, "-"]
validations:
  - [Approver is the submitter, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
  - [Class change with loaded data, Roll back the loaded batches before changing the class of this object, "-"]
  - [Return without reason, Enter the reason for the return, "-"]
notifications:
  - On submission, the business owner is notified in My Approvals and by e-mail.
  - On approval or return, the Data Migration Lead is notified.
audit:
  - Every decision is kept with class, criteria, rationale, submitter, approver and dates (report Migration Decisions).
acceptance:
  - A decision approved by the business owner sets the object to Decided with the approved class.
  - The submitter cannot approve their own decision.
  - An object classed Excluded cannot be selected for a batch.
  - The report Migration Decisions lists every decision with its approver.
```

```fr
id: FR-DM-003
title: Sign off each object through gates
brd: [BRID 1.1a (p.7), BRID 1.1b (p.7)]
actor: Data owner, Data Steward, Data Migration Lead, reconciliation approver
priority: Must have
screens: Sign-off; Batches
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
screens: Extracts
description:
  - BDOI sends one file per layout and source system with a control file. Most objects have one layout; R04 (insurers and insurer branches R04B), P01 (headers and insurer shares P01S) and F01 (invoice headers, insurer shares F01S and components F01C) have sub-layouts sent as separate files with the same as-of date. BIBS checks each file before any row is staged, so that what BIBS loads is exactly what BDOI extracted.
  - The file contract (names, CSV or Excel (.xlsx), date and amount formats, control file content) is published to BDOI in the data requirements workbook, which BIBS generates from the layouts.
preconditions:
  - The object is Decided (Migrate or Carry-forward) and has an approved layout version.
  - The user has the right "Data migration intake".
main_flow:
  - The operator opens Extracts, chooses the object, the layout and the source system and uploads the data file and the control file.
  - BIBS checks the SHA-256 of the file against the control file, the header against the layout, the parsed row count against the control count, the amount sums per currency and the hash total of the key column.
  - When every check passes, BIBS gives the extract a number (MGX-yyyy-nnnnnn), masks personal data when the environment is not production, and stages the rows.
  - BIBS shows the extract as Staged with its counts and control totals.
alternate_flows:
  - A check fails. The extract is Rejected with the failed check and the difference; nothing is staged; the Data Migration Lead is notified.
  - Same file sent twice. BIBS refuses it as a duplicate.
  - Delta extract with an as-of date earlier than the last one of the object. BIBS refuses it.
rules:
  - [R1, "File names are <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv or .xlsx, where LAYOUT is the layout code (for example F01C), not the object code; the control file adds .ctl.csv.", Fixed, "-"]
  - [R5, "A batch of an object takes one checked extract of each of its layouts with the same as-of date.", Fixed, "-"]
  - [R2, "CSV is UTF-8 with comma separator and one header row; dates are yyyy-MM-dd; amounts use a dot decimal and 2 decimals without thousands separator.", Fixed, "-"]
  - [R3, "Outside production, names, addresses, TIN, ID, account and phone numbers, e-mail addresses and birth dates are masked before staging; the same value is always masked the same way.", Configurable, "Masking rules; parameter the environment class parameter"]
  - [R4, "Files are received through the console or the secure file drop and are never e-mailed.", Fixed, "-"]
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
  - [Mode, List, "Yes", "Full, Delta", "-"]
  - [Data file, File, "Yes", "-", "CSV or Excel (.xlsx)"]
  - [Control file, File, "Yes", "-", "CSV in the control layout"]
notifications:
  - A rejected extract notifies the Data Migration Lead and the Migration Operator (alert Migration extract rejected).
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
screens: Code Maps
description:
  - A code map set exists per domain, for example each LOV type, insurers, insurer branches, products and risk codes, packages, lines, branches, sales units, account officers, each MIS field, GL accounts and the status lists of each object. An entry maps a legacy code of a source system to a BIBS code, or says Default, Reject or Create.
  - A set has versions. Only the approved version is used by a batch, and the batch records which version it used, so every loaded row can be traced to the mapping that produced it.
preconditions:
  - The user has the right "Data migration mapping edit" to edit, the right "Data migration mapping approve" to approve.
main_flow:
  - The Data Steward opens Code Maps, chooses a set and creates a Draft version (empty, copied from the approved version, or imported from Excel).
  - The steward edits entries and submits the version.
  - The data owner compares the version with the approved one (added, changed and removed entries) and approves it.
  - BIBS marks it Approved and the previous version Superseded.
alternate_flows:
  - Return. The owner returns the version with a reason; it goes back to Draft.
  - Create entries. On approval, BIBS lists the reference values to create; the Data Steward creates them through the reference-data load (FR-DM-030), where the normal maker-checker of each master applies.
rules:
  - [R1, "One approved version per set; a batch uses the approved version current at validation.", Fixed, "-"]
  - [R2, "The approver is never the user who submitted the version.", Fixed, "-"]
  - [R3, "An entry action is Map (to a target code), Default (to the set default), Reject (the row fails) or Create (a new BIBS value).", Fixed, "-"]
  - [R4, "The target code of a MAP entry must exist and be active in BIBS when the version is approved.", Fixed, "-"]
validations:
  - [Target code does not exist, "Target code {code} does not exist in {domain}", "-"]
  - [Same legacy code twice, "Legacy code {code} of {source} is mapped twice", "-"]
  - [Approver is the submitter, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
fields_screen: Code map entry
fields:
  - [Source system, List, "Yes", "the list of source systems", "-"]
  - [Legacy code, Text, "Yes", "-", "Unique per source in the version"]
  - [Legacy description, Text, "No", "-", "-"]
  - [Action, List, "Yes", "Map, Default, Reject, Create", "-"]
  - [Target code, List, "Cond.", "Values of the target domain", "Mandatory for MAP"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - Submitted versions go to the data owner's My Approvals.
audit:
  - Versions and entries keep their history; the report Code Map Versions lists versions, approvers and the batches that used them.
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
screens: Code Maps (Unmapped tab); Batches
description:
  - When staged rows are validated, every coded column is looked up in its code map. Codes without an entry, and entries whose target is no longer active, are reported with the number of rows and sample keys. A batch with unmapped codes in a mandatory column cannot be approved for load, so "all required codes exist" before any transaction loads.
preconditions:
  - At least one extract of the object is staged.
main_flow:
  - The Data Steward opens the Unmapped tab or runs the report Unmapped Legacy Codes.
  - BIBS lists set, source system, legacy code, rows and sample legacy keys.
  - The steward adds the entries in a new map version and submits it (FR-DM-011).
  - After approval the batch is validated again and the codes disappear from the report.
rules:
  - [R1, "An unmapped code in a mandatory coded column is an Error; in an optional column a Warning.", Configurable, "Rule catalogue"]
validations:
  - [Load approval with unmapped codes, "The batch has {n} unmapped codes; map them before approving the load", "-"]
notifications:
  - Alert Unmapped legacy codes to the Data Steward when a validation finds unmapped codes.
audit:
  - The report run is recorded.
acceptance:
  - A staged row whose insurer code is not mapped appears on the Unmapped tab and in the report Unmapped Legacy Codes with its legacy key.
  - The load of that batch cannot be approved until the code is mapped and the batch validated again.
  - The report exports to Excel and PDF.
```

```fr
id: FR-DM-013
title: Validate staged data against data-quality rules
brd: [BRID 1.1b (p.7), BRID 2.1 (p.7), BRID 3.1 (p.7)]
actor: BIBS (validation); Data Steward
priority: Must have
screens: Batches (Issues tab); Layouts and Rules
description:
  - A batch is validated before it can load. Each staged row is mapped and checked against the rules of its object - mandatory fields, formats, lookups, uniqueness, references to loaded parents, cross-field rules (for example components that add up to the gross premium, open balance equal to booked less paid) and plausibility warnings.
  - Each failed rule is an issue with severity Error (the row is not loaded) or Warning (the row loads and is reported).
preconditions:
  - The batch is Planned with at least one staged extract.
main_flow:
  - The operator clicks **Validate**.
  - BIBS maps each row with the approved map versions, runs the rules and, for clients, the matching (FR-DM-031).
  - BIBS sets each row Valid, Warning or Invalid and the batch Validated with the counts.
  - The Data Steward resolves the issues - fixed at source (new extract), mapped (new map version) or waived by the data owner with a reason.
alternate_flows:
  - Error rate above the threshold. The batch cannot be approved for load; the steward sees the rate and the threshold.
rules:
  - [R1, "Master data may be loaded with at most the error-rate limit of master data percent of rows rejected or waived (default 0.5).", Configurable, "Parameters: the error-rate limit of master data"]
  - [R2, "Financial objects (open invoices, UPP, GL trial balance) load only with 0 errors, or with each excluded row approved by the data owner with a manual-entry plan. The error rate is (invalid rows not waived + waived rows) / (staged rows - excluded rows); excluded rows are not errors and not in the base.", Configurable, "Parameters: the error-rate limit of financial objects"]
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
  - A legacy invoice whose components do not add up to its gross premium is Invalid with that message.
  - A waived row shows the waiver reason and the data owner who approved it.
  - A batch of open invoices with one Invalid row cannot be approved for load unless the row is excluded by the data owner; once it is excluded, the error rate shown is 0 percent.
```

```fr
id: FR-DM-014
title: Load a batch through the BIBS services
brd: [BRID 1.1b (p.7)]
actor: Data Migration Lead (approval); Migration Operator; BIBS (load)
priority: Must have
screens: Batches
description:
  - A validated batch is approved for load (gate G4) and then loaded. Each row is loaded through the same BIBS service that the screens use (for example client registration, account import, the Operations ledger, the unapplied workbench, the journal service), so every BIBS rule, audit entry and accounting entry applies. BIBS never writes directly into business tables.
  - Each loaded record gets a key cross-reference from its legacy key, which makes the load rerunnable and the record traceable.
preconditions:
  - The batch is Validated within the error threshold; the objects it depends on are accepted (G6) in the same environment.
main_flow:
  - The Data Migration Lead reviews the counts and clicks **Approve Load**.
  - The operator clicks **Load**, or the cutover plan starts it.
  - BIBS loads Valid and Warning rows in chunks, several at a time, and marks each row Loaded, Skipped (already loaded, unchanged) or Rejected (with the reason).
  - BIBS sets the batch Loaded or Loaded with rejects and starts the reconciliation (FR-DM-020).
alternate_flows:
  - A chunk fails. BIBS retries its rows one by one so that one bad row fails alone.
  - The row was loaded before with different content. A client or header is updated (delta, before the freeze); an open item is Rejected as changed after load.
  - Load interrupted. The load resumes from the last committed chunk; loaded rows are skipped.
rules:
  - [R1, "Rows are loaded in transactions of the rows per load transaction (default 500), in the partitions loaded in parallel (default 4).", Configurable, "Parameters: the rows per load transaction, the partitions loaded in parallel"]
  - [R2, "One load at a time per object and environment.", Fixed, "-"]
  - [R3, "Records created by a load carry the origin Migrated (clients, accounts, UPP) or Legacy (invoices); they trigger no notification to customers or insurers.", Fixed, "-"]
  - [R4, "The approver of the load is not the operator who runs it.", Fixed, "-"]
validations:
  - [Dependency not accepted, "Object {object} must be accepted before this object can load", "-"]
  - [Approver is the operator, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
notifications:
  - A failed load raises alert Migration load failed to the Data Migration Lead and the operator.
audit:
  - The batch run log records each step with counts and times; every created record has its own audit entry from the owning module.
acceptance:
  - Loading the same batch twice creates no duplicate; the second run marks the rows Skipped.
  - A client created by a load is found in client search and has a cross-reference to its legacy key.
  - The batch page shows staged, loaded, skipped, rejected and excluded counts that add up.
```

```fr
id: FR-DM-015
title: Rerun and roll back a batch
brd: [BRID 1.1b (p.7)]
actor: Data Migration Lead (request); reconciliation approver (approval); Migration Operator
priority: Must have
screens: Batches
description:
  - Rejected rows are corrected and loaded again in a rerun batch that belongs to the original batch and is reconciled with it.
  - A batch that is not yet signed off can be rolled back when rollback is offered for its object and the records have not been changed since the load. Otherwise the environment is restored from the snapshot taken before the load.
preconditions:
  - Rerun - the batch has rejected rows and corrections are available (new extract, new map version or waiver).
  - Rollback - the batch is Loaded or Reconciled and not signed off (G6).
main_flow:
  - For a rerun, the Data Migration Lead clicks **Rerun Rejects**; BIBS creates a child batch with the rejected and changed rows, validates it and, after approval, loads it.
  - For a rollback, the lead clicks **Request Rollback** with a reason; the reconciliation approver approves it.
  - BIBS undoes the records of the batch newest first through the owning services (for example deactivation of a migrated client without activity, reversal of an opening entry) and marks the batch Rolled back.
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
actor: Reconciliation approver; BIBS (reconciliation)
priority: Must have
screens: Reconciliation
description:
  - After each load BIBS reconciles the object at four levels, and financial objects at a fifth. L1 counts - control file, received, staged, loaded, skipped, rejected and excluded. L2 amounts - sums per amount column and currency against the control totals and the values in BIBS. L3 hash totals - key column hash and a checksum per row. L4 fields - every mapped field of every loaded row read back from BIBS and compared with the staged value. L5 GL - see FR-DM-021.
  - Every difference is a break. A break is fixed (rerun) or explained; the explanation is approved by the reconciliation approver. This makes "all data objects and items reconcilable from source to target" (BRD acceptance criterion) a signed, reproducible result.
preconditions:
  - The batch is Loaded or Loaded with rejects.
main_flow:
  - BIBS runs the reconciliation after the load, or the approver clicks **Reconcile**.
  - BIBS shows the object by level with Matched or Break and the difference.
  - The Data Steward writes an explanation for each break (reason from a list and text).
  - The reconciliation approver approves the explanations and signs the reconciliation (gate G5).
rules:
  - [R1, "L1 - received = control count, and loaded + skipped + rejected + excluded = staged.", Fixed, "-"]
  - [R2, "L2 - amount difference within the amount tolerance (default 0.00) per currency.", Configurable, "Parameters: the amount tolerance"]
  - [R3, "L4 - a field difference is a break unless the code map explains it (mapped value).", Fixed, "-"]
validations:
  - [Sign-off with unexplained break, Explain and approve every break before signing the reconciliation, "-"]
fields_screen: Break explanation
fields:
  - [Level, Text, "Yes", "L1-L5", "Read only"]
  - [Measure, Text, "Yes", "-", "Read only"]
  - [Difference, Amount / Number, "Yes", "-", "Read only"]
  - [Reason, List, "Yes", "the list of break reasons", "-"]
  - [Explanation, Text, "Yes", "-", "Up to 2,000 characters"]
notifications:
  - A break raises alert Migration reconciliation break to the Data Migration Lead and the Data Steward.
audit:
  - Reconciliation runs, lines, explanations and approvals are kept as evidence and are not purged with the staging data.
acceptance:
  - For a batch of 1,000 open invoices loaded without rejects, L1 shows 1,000 at every stage and L2 shows 0.00 difference per currency.
  - A field changed in BIBS after the load appears as an L4 break on the next reconciliation.
  - The reconciliation cannot be signed while a break has no approved explanation.
  - The reports Reconciliation Summary and Reconciliation Detail export to Excel and PDF.
```

```fr
id: FR-DM-021
title: Reconcile carried-forward balances to the GL
brd: [BRID 1.1b (p.7), BRID 5.1 (p.8)]
actor: Reconciliation approver (Comptrollership)
priority: Must have
screens: Reconciliation (GL tab); ACSL GL to Sub-ledger Reconciliation
description:
  - Each open legacy invoice and each legacy UPP item is loaded with an opening entry against the Migration Clearing account. The GL trial balance of the legacy systems is loaded as an opening journal in which the lines of the legacy control accounts are replaced by Migration Clearing. When the detail and the trial balance agree, Migration Clearing is 0.00 per branch and currency.
  - The legacy control accounts are reconciled to the legacy sub-ledgers in the ACSL GL to Sub-ledger Reconciliation, which gets a ledger-context filter (legacy / new).
preconditions:
  - Open invoices, UPP and the GL trial balance are loaded for the environment.
main_flow:
  - The approver runs the report Migration Clearing and Legacy Control Accounts.
  - BIBS shows the Migration Clearing balance per branch and currency and the legacy control accounts against their sub-ledgers.
  - Any non-zero balance is investigated and explained or corrected by a rerun.
  - The approver signs the GL level of the reconciliation.
rules:
  - [R1, "Migration Clearing must be 0.00 per branch and currency before go-live.", Fixed, "-"]
  - [R2, "The legacy control accounts and the Migration Clearing account are set up by Comptrollership; the GL account code map says which legacy accounts go to Migration Clearing.", Configurable, "Chart of accounts; GL account code map"]
  - [R3, "A difference on Migration Clearing has the sign of the trial balance - for an invoice missing from the load, a debit equal to its net receivable position (open PR + PR2307 + commission receivable - open DTIP - unrealised commission - deferred VAT) when positive, a credit when negative; for a missing UPP item, a credit equal to its balance.", Fixed, "-"]
  - [R4, "The report Migration Clearing and Legacy Control Accounts also compares, per branch, currency and legacy control account, the trial balance line with the opening detail posted to that account, so a missing invoice whose net is zero is still found.", Fixed, "-"]
  - [R5, "The same checks run again after each FY2027 true-up (FR-DM-024).", Fixed, "-"]
validations:
  - [Clearing not zero at sign-off, "Migration Clearing is {amount} in {branch} {currency}; it must be zero", "-"]
  - [Control account line differs from the detail, "{account} differs from the opening detail by {amount} in {branch} {currency}", "-"]
notifications:
  - Alert Migration clearing not zero to Comptrollership and the Data Migration Lead.
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
screens: Batches (object G01); Reconciliation (GL tab); Journal inquiry
description:
  - For a go-live on 3 January 2028 at the year-end boundary (DMQ39, option A, recommended and awaiting Comptrollership confirmation), the BIBS GL opens from the preliminary December 2027 trial balance of legacy, taken after the December soft close and the last legacy EOD. Only the balance-sheet accounts are opened. The FY2027 P&L accounts are not opened; their net result per branch and currency goes to retained earnings, so the FY2028 P&L in BIBS starts at zero.
  - The opening is provisional. The FY2027 closing and audit adjustments that Comptrollership posts in legacy after the freeze reach BIBS as true-ups (FR-DM-023). The FY2027 BIR annual returns and the FY2027 audit use legacy; FY2028 uses BIBS.
preconditions:
  - The GL account code map is approved and the company's retained earnings account is set.
  - Comptrollership has signed the preliminary December trial balance as the provisional opening.
main_flow:
  - The operator validates and loads G01 (trial balance version Provisional).
  - BIBS posts one opening journal per branch and currency, value date 1 January 2028, reference MIG-TB-<as-of>, flag Provisional - balance-sheet lines on their mapped accounts, legacy control-account lines on Migration Clearing, and the lines whose mapped account is a P&L account on retained earnings.
  - The reconciliation compares the journals with the legacy trial balance and Migration Clearing with the open-item detail (FR-DM-020, FR-DM-021).
rules:
  - [R1, "The opening journals are system journals of type Opening, source Migration, value date the opening value date (default 1 January 2028, the first day of the opening period).", Configurable, "Parameters: the opening value date"]
  - [R2, "No FY2027 P&L balance is opened in BIBS; a line whose mapped account is a P&L account posts to retained earnings, with the mapped account kept in the line description.", Fixed, "-"]
  - [R3, "The opening stays marked Provisional until the closure of the true-ups (FR-DM-024).", Fixed, "-"]
validations:
  - [Trial balance out of balance, "The trial balance of {branch} {currency} is out of balance by {amount}", "-"]
  - [Retained earnings account missing, "The retained earnings account of the company is not set", "-"]
  - [Opening not signed as provisional, "The preliminary trial balance is not signed by Comptrollership", "-"]
notifications:
  - "Alert Migration clearing not zero when Migration Clearing is not 0.00 after the load."
audit:
  - The journals, the trial balance extract and the sign-off are kept as evidence.
acceptance:
  - After the load of a branch, its balance-sheet accounts in BIBS equal the legacy preliminary trial balance and its P&L accounts are 0.00 at 1 January 2028.
  - A legacy trial balance with a FY2027 net income of 1,250,000.00 in a branch gives a credit of 1,250,000.00 to retained earnings in that branch.
  - The load cannot be approved while the preliminary trial balance is not signed.
  - The opening journals show the flag Provisional until the true-ups are closed.
```

```fr
id: FR-DM-023
title: Post the FY2027 true-ups as opening-balance adjustment journals
brd: [BRID 1.1b (p.7), BRID 5.1 (p.8), BRID 12.1 (p.12)]
actor: Comptrollership GL lead (prepares); Head, Comptrollership (approves); Migration Operator
priority: Must have
screens: True-ups; Extracts; Batches (object G03); Journal inquiry
description:
  - After the freeze the legacy GL stays open only for the FY2027 closing and audit adjustments, restricted to named Comptrollership users, with no new business. Each adjustment reaches BIBS in a true-up - the first after the legacy year-end close (about mid to late January 2028), the final one after the audited financial statements (about March-April 2028), and interim ones only when Comptrollership posts material adjustments in between.
  - A true-up is an opening-balance adjustment journal in the opening period (January 2028, value date 1 January 2028), never a FY2028 transaction. FY2027 P&L effects go to retained earnings. Adjustments on legacy control accounts, which BIBS holds invoice by invoice and UPP by UPP, come with their open-item detail and change those items.
preconditions:
  - The provisional opening is loaded (FR-DM-022).
  - BDOI IT has sent, each with its control file, the legacy trial balance after the adjustments (G01, version TU1, TU2 or Final), the adjustment journal lines since the previous true-up (G03), the open-item detail of the lines on legacy control accounts (G03D) and the legacy journal listing since the freeze.
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
  - [R1, "A true-up posts as a system journal of type Opening, source Migration, reference MIG-TU-<n>-<branch>-<currency>, value date the opening value date (1 January 2028).", Fixed, "-"]
  - [R2, "FY2027 P&L lines post to retained earnings. Lines on legacy control accounts post to Migration Clearing and are matched by the open-item detail, which changes the open balance of each legacy invoice or UPP item (movement Legacy adjusted or Legacy written off) against Migration Clearing.", Fixed, "-"]
  - [R3, "Only journals dated in FY2027, posted in legacy after the freeze and entered in the FY2027 adjustment register are accepted.", Fixed, "-"]
  - [R4, "The preparer and the approver are different users; the approver holds the permission Data migration trueup approve.", Fixed, "-"]
  - [R5, "Posting happens once per reference; the same true-up posted twice creates no second journal.", Fixed, "-"]
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
screens: True-ups (Reconciliation tab); Run-off and Decommissioning
description:
  - Each true-up is reconciled to the legacy trial balance before it is signed, and the legacy GL stays under cut-off controls until the final true-up. The checks are cut-off (every legacy journal since the freeze is in the register and in a true-up, dated in FY2027 and posted by a named Comptrollership user, and no legacy business module posted after the freeze), movement (the true-up equals the change of the legacy trial balance since the previous true-up), balance (the BIBS opening balances equal the legacy trial balance of the true-up), Migration Clearing and the legacy sub-ledgers, and, when the opening period was reopened, no other journal in it during the window.
  - After the final true-up, the balance check against the audited FY2027 trial balance is signed and shared with the external auditor, the legacy GL is locked for all users, and the true-ups are closed. A FY2027 finding after that is a prior-period adjustment in BIBS through the normal Comptrollership journal, outside the migration.
preconditions:
  - The true-up is posted (FR-DM-023).
main_flow:
  - BIBS runs the report Opening-Balance Adjustment Reconciliation after the posting.
  - The Head of Comptrollership reviews each check and signs the true-up reconciliation.
  - After the final true-up, BDOI IT locks the legacy GL, Comptrollership closes the adjustment register, and the Head of Comptrollership and the Program Manager sign the closure of the true-ups.
alternate_flows:
  - Break. A failed check raises Opening-balance adjustment break; the cause is found and corrected by a corrected extract before sign-off or by the next true-up.
rules:
  - [R1, "Movement - per BIBS account, branch and currency, the true-up lines equal the legacy trial balance of the true-up minus the legacy trial balance of the previous true-up (or of the provisional opening), both mapped through the GL account code map, with the P&L netted into retained earnings.", Fixed, "-"]
  - [R2, "Balance - per BIBS account, branch and currency, the provisional opening plus true-ups 1 to n equal the legacy trial balance of true-up n; Migration Clearing is 0.00; the legacy control accounts equal their sub-ledgers.", Fixed, "-"]
  - [R3, "Cut-off - the legacy journal listing since the freeze equals the FY2027 adjustment register and the true-up journals; no journal is dated outside FY2027; no posting user is outside the Comptrollership access list; no legacy business module posted after the freeze.", Fixed, "-"]
  - [R4, "A true-up with a break cannot be signed.", Fixed, "-"]
  - [R5, "After the closure no further true-up can be created, and the opening journals lose the Provisional flag.", Fixed, "-"]
validations:
  - [Legacy journal not in the true-up, "Legacy journal {no} posted on {date} is not in the adjustment register or the true-up", "-"]
  - [Movement differs, "{account} {branch} {currency} - true-up {x}, legacy trial balance change {y}", "-"]
  - [Balance differs, "{account} {branch} {currency} - BIBS opening {x}, legacy trial balance {y}", "-"]
  - [Sign-off with a break, "Check {check} is not met", "-"]
  - [New true-up after closure, "The FY2027 true-ups are closed", "-"]
notifications:
  - Alert Opening-balance adjustment break to the Head of Comptrollership and the Data Migration Lead.
audit:
  - Reconciliation runs, sign-offs, the legacy lock evidence and the closure are kept as evidence.
acceptance:
  - For true-up 1, the report Opening-Balance Adjustment Reconciliation shows each account's true-up equal to the change of the legacy trial balance, the BIBS opening equal to the legacy post-close trial balance, and Migration Clearing 0.00.
  - A legacy journal that is in the legacy journal listing but not in the register stops the sign-off with the message.
  - After the final true-up is signed and the closure recorded, a new true-up cannot be created and the opening journals are no longer marked Provisional.
```

## Client master and reference data

```fr
id: FR-DM-030
title: Load reference data through the code maps
brd: [BRID 3.1 (p.7)]
actor: Data Steward; data owner
priority: Must have
screens: Code Maps; Batches; the reference-data screens of each master
description:
  - Reference data (LOV and MIS values, sales organisation, insurers and branches, product lines, products and risk codes, packages, commission rates) is loaded first. Legacy values already present in BIBS are mapped; values marked Create are created through the master's own service and authorised through its maker-checker.
preconditions:
  - The code map versions of the object are approved (G2).
main_flow:
  - The operator validates and loads the reference batch.
  - BIBS creates the Create values as pending authorisation in the master.
  - The authoriser of each master authorises them (existing maker-checker).
  - The Data Steward confirms that every required code exists; the batch is reconciled and accepted.
alternate_flows:
  - A created value is rejected by the master's authoriser. The code map entry is changed to Map or Reject in a new version.
rules:
  - [R1, "Dependent objects (clients, headers, invoices) do not load until every code they use is mapped to an active, authorised value.", Fixed, "-"]
validations:
  - [Required code missing, "Code {code} of {domain} does not exist or is not authorised", "-"]
notifications:
  - Created values appear in the authorisers' My Approvals.
audit:
  - Each created or authorised value is audited by its master.
acceptance:
  - A legacy LOV value marked Create becomes an authorised BIBS value after the master authorisation, and the batch reconciles.
  - A client batch using a segment that is not yet authorised cannot be approved for load.
```

```fr
id: FR-DM-031
title: Match and deduplicate legacy clients
brd: [BRID 2.1 (p.7)]
actor: BIBS; Data Steward
priority: Must have
screens: Client Matching
description:
  - Legacy clients of all source systems are matched with each other and with clients already in BIBS before any is loaded. Exact matches on hard keys merge automatically; likely matches go to the Data Steward's review queue; the rest become new clients. The keys start from the BIBS duplicate keys (TIN, ID type and number, name with birth date, corporate name) and the keys BDOI agrees (DMQ04).
  - Clients that match form one cluster and become one BIBS client, completed field by field from the best source (survivorship rules).
preconditions:
  - The client batch is being validated (FR-DM-013).
main_flow:
  - BIBS scores every candidate pair on the keys.
  - Pairs at or above the auto-merge score merge; pairs between the review and auto-merge scores go to the review queue; lower pairs are separate clients.
  - The Data Steward opens Client Matching, compares the records side by side with the matching keys and decides merge or keep separate.
  - BIBS prepares each cluster's record with the survivorship rules and shows the result before load.
alternate_flows:
  - Match with a client already in BIBS. The legacy record merges into the existing client; the load updates only blank fields and adds the cross-reference.
rules:
  - [R1, "Scores - TIN 100; ID type and number 100; name with birth date 95; corporate name with registration number 95, without 90; bank CIF 100 if agreed; e-mail 40; mobile 40; similar name with the same birth date or city 70.", Configurable, "Matching rules"]
  - [R2, "Auto-merge at the automatic merge score (default 90); review from the review score (default 60).", Configurable, "Parameters: the automatic merge score, the review score"]
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
  - Every decision and every value that lost in survivorship is kept (report Client Matching).
acceptance:
  - Two legacy clients with the same TIN from EBIX and QPS become one BIBS client with both legacy keys in the cross-reference.
  - Two legacy clients with similar names and the same birth date but different IDs go to the review queue.
  - The client batch cannot load while a pair is open in the review queue.
```

```fr
id: FR-DM-032
title: Create migrated clients in the Client Master
brd: [BRID 2.1 (p.7)]
actor: BIBS (client load)
priority: Must have
screens: Batches; Client (existing screens)
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
  - [R1, "Migrated clients have the origin Migrated and the status Active; KYC status and review date are taken from legacy.", Fixed, "-"]
  - [R2, "Delta loads stop at the freeze; after go-live the client master is maintained only in BIBS.", Fixed, "-"]
validations:
  - [Hard duplicate at load, This client matches an existing client on TIN or ID; decide the match first, "-"]
notifications:
  - "None to clients."
audit:
  - Each client creation and update is audited by the client module with the batch number.
acceptance:
  - A migrated client is Active and can be selected on a new quotation without re-encoding.
  - No screening case is created during the load; after the full run, hits appear in the screening queue.
  - A client changed in legacy the day before the freeze is updated in BIBS by the delta batch.
```

```fr
id: FR-DM-033
title: Find migrated clients and policies by legacy reference
brd: [BRID 2.1 (p.7), BRID 4.1 (p.8)]
actor: Marketing User, Operations User, Contact Center
priority: Must have
screens: Client search; Account search; Customer Servicing search
description:
  - Users find a migrated client or policy by its BIBS code and also by its legacy client number, cover number or policy number. The record shows the source system and the legacy reference.
preconditions:
  - The records are loaded.
main_flow:
  - The user enters a legacy reference in the search.
  - BIBS looks it up in the cross-reference and opens the BIBS record.
alternate_flows:
  - Reference not migrated. BIBS says the reference is not in BIBS and, if the user has the right "Legacy inquiry view", offers the legacy archive search.
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
title: Load the package code map for the Renewal package remapping
brd: [BRID 3.1 (p.7), BRID 12.1 (p.12)]
actor: TSU (prepares the package code map); Product Owner, Marketing Business System (approves); Renewal processing team (Exception bucket)
priority: Must have
screens: Code Maps (the package code map); Batches (object R06); Renewal candidates (Exception bucket)
description:
  - Legacy packages are remapped to the package versions that TSU maintains in BIBS (BRD-3) at Renewal sanitation, per candidate, and not at migration intake (DMQ36, answered 26 September 2026). The migration loads the package code map as reference data only - entries from a legacy package and version to a BIBS package version, with conditional entries where one legacy package splits (one qualifier - risk code, insurer or sum-insured band).
  - Policy headers (FR-DM-040) and the renewal advices already sent (FR-DM-125) keep the legacy package as given; no package is resolved, rejected or warned at intake. At renewal the package check of the Renewal sanitation resolves the BIBS package through the map. A package without an entry, with a Reject entry or with no matching qualifier sends the candidate to the Exception bucket, where the Renewal processing team chooses the package; each choice is passed to TSU for the next map version.
preconditions:
  - The BIBS package versions exist in Product Maintenance.
  - The user has the right "Data migration mapping edit" (TSU) or the right "Data migration mapping approve" (Product Owner).
main_flow:
  - TSU creates a Draft version of the package code map (copied from the approved version or imported from Excel), enters one entry per legacy package and version, with a qualifier where the package splits, and submits it.
  - The Product Owner compares the version with the approved one and approves it (FR-DM-011).
  - The operator loads object R06; BIBS hands the approved version to the Renewal package map and reconciles it (L1-L4).
  - In each trial migration BIBS lists the legacy packages of the headers expiring up to 31 May 2028 that have no entry (information report); TSU closes the gaps before the map freeze.
alternate_flows:
  - Overlapping qualifiers. The version cannot be submitted until the entries are corrected.
  - Package not resolved at renewal. The candidate goes to the Exception bucket of Renewal.
rules:
  - [R1, "Packages are remapped at Renewal sanitation (package check) with the package code map loaded by the migration; TSU prepares each version and the Product Owner of Marketing Business System approves it.", Configurable, "Package code map; role-permission matrix"]
  - [R2, "A conditional entry has one qualifier - risk code, insurer or sum-insured band; the qualifiers of one legacy package do not overlap.", Fixed, "-"]
  - [R3, "The migration intake does not resolve or check the package of a policy header or of a renewal advice.", Fixed, "-"]
  - [R4, "An unresolved package at renewal sends the candidate to the Exception bucket, worked by the Renewal processing team.", Fixed, "-"]
validations:
  - [Overlapping qualifiers, "Legacy package {code} has overlapping entries for {qualifier}", "-"]
  - [Target package not active, "Target code {code} does not exist in {domain}", "-"]
fields_screen: package code map entry
fields:
  - [Legacy package and version, Text, "Yes", "-", "As in P01"]
  - [Qualifier, List, "No", "Risk code, Insurer, Sum-insured band", "One per entry"]
  - [Qualifier value, Text, "Cond.", "-", "Mandatory with a qualifier; a band is given as from and to amounts"]
  - [Action, List, "Yes", "Map, Reject", "Reject = no BIBS package; the candidate goes to the Exception bucket at renewal"]
  - [BIBS package version, List, "Cond.", "Active package versions (Product Maintenance)", "Mandatory for MAP"]
notifications:
  - "None at intake; the Exception bucket is part of the Renewal worklist."
audit:
  - Versions and entries keep their history; the R06 batch records the version it loaded (report Code Map Versions).
acceptance:
  - An approved package code map version with 120 entries loads into the Renewal package map and reconciles with 120 entries.
  - At renewal, a candidate on a legacy package with one entry for a sum insured up to 2,000,000.00 and one above gets the first BIBS package for a sum insured of 950,000.00 and the second for 2,500,000.00.
  - A candidate whose legacy package has a Reject entry is in the Exception bucket.
  - A version with two entries of the same legacy package whose sum-insured bands overlap cannot be submitted.
  - A header on a legacy package without an entry loads with no warning and keeps its legacy package.
```

## In-force policy headers

```fr
id: FR-DM-040
title: Load in-force policy headers
brd: [BRID 4.1 (p.8)]
actor: BIBS (header load); Data Migration Lead
priority: Must have
screens: Batches; Account (existing screens, read only for header fields)
description:
  - When the condition of BRID 4.1 is met (DMQ09), the in-force legacy policies are loaded as BIBS accounts with a minimal header - status, key dates, insurer and shares, line of business and product, policy number, sum insured, currency, AO, unit and branch, payment arrangement and PN numbers - linked to the migrated client.
  - The header is created without quotation, placement, issuance or invoice. It is what allows legacy invoices to be endorsed in BIBS (FR-DM-090 to 092) and the policy to be renewed in BIBS (FR-DM-122, FR-DM-124).
  - The header keeps the legacy package code and version as stored in legacy; packages are remapped at Renewal sanitation, not at load (FR-DM-034).
  - Whatever the condition of BRID 4.1, the headers of every policy expiring from go-live to 31 May 2028 and of every renewal term booked in legacy that starts on or after go-live are loaded, because the go-live renewal extraction works only from the migrated headers (FR-DM-124).
preconditions:
  - Clients and reference data are accepted.
main_flow:
  - The operator validates and loads the header batch.
  - BIBS creates each account with the origin Migrated and the status Booked and records the cross-reference of the cover or policy number.
  - BIBS links the legacy invoices of the policy to the account when they are loaded.
alternate_flows:
  - Client not migrated. The row is Invalid ("client not loaded").
rules:
  - [R1, "A migrated header has the origin Migrated, the status Booked and the legacy reference; it has no BIBS invoice of its own.", Fixed, "-"]
  - [R2, "Headers in scope follow the condition decided for the object (all in-force policies, or those with open legacy invoices, as BDOI decides).", Configurable, "Object decision (FR-DM-002)"]
  - [R3, "Always in scope - every policy expiring from go-live to the end of the go-live renewal window (31 May 2028) and every renewal term booked in legacy that starts on or after go-live.", Fixed, "-"]
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
screens: Account search; Account page; Invoice 360
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
actor: BIBS (open invoice load); Reconciliation approver
priority: Must have
screens: Batches; Invoice 360; Operations invoice lists
description:
  - Every legacy invoice open at cutover (premium receivable, premium paid and not remitted, commission receivable or PR2307 still open) enters the BIBS Operations ledger with the origin Legacy. It keeps its legacy number, source system and legacy reference. Each component is loaded with its original amount and what was paid, remitted, adjusted or written off in legacy, so its open balance equals the legacy open balance.
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
  - [R1, "A legacy invoice has the origin Legacy and the legacy ledger context; all its postings use the legacy components that Comptrollership maps to the legacy sub-ledgers.", Fixed, "-"]
  - [R2, "Legacy invoices are not part of the production register extract (they were reconciled in legacy).", Fixed, "-"]
  - [R3, "Opening entries are dated the cutover date; foreign-currency amounts use the rate decided in DMQ35.", Configurable, "Parameters: the go-live date parameter; booking rate"]
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
actor: BIBS (UPP load); Cashiering User
priority: Must have
screens: Unapplied Payments workbench; Batches
description:
  - Every legacy UPP item open at cutover is created in the Unapplied Payments workbench with the origin Migrated, its legacy AR number and date, payor, client, sales unit, amount, balance, status and the references that help to match it. A disposition in progress in legacy keeps its type and details.
  - An opening entry puts the balance on the UPP legacy sub-ledger against Migration Clearing.
preconditions:
  - Clients are accepted.
main_flow:
  - The operator validates and loads the UPP batch.
  - BIBS creates each item in the tab of its legacy status and posts the opening entry.
  - The Cashiering User sees the items with the LEGACY badge, the legacy AR and the balance.
alternate_flows:
  - Receipt for a migrated UPP (DMQ14). No new AR is issued at load; the legacy AR number is shown. If BDOI requires an acknowledgment, parameter the setting Issue an AR for a migrated unapplied payment set to true makes the load issue a BIBS AR without a cash posting, referring to the legacy AR number.
rules:
  - [R1, "Legacy statuses map to the tabs Unapplied, Monitoring and For Approval through the UPP status code map.", Configurable, "UPP status code map"]
  - [R3, "No BIBS AR is issued for a migrated UPP unless the setting Issue an AR for a migrated unapplied payment is true (default false, DMQ14); the AR then has no cash posting.", Configurable, "Parameters: the setting Issue an AR for a migrated unapplied payment"]
  - [R2, "A migrated UPP stays in the legacy ledger context; its applications, refunds and reclassifications post to the UPP legacy sub-ledger.", Fixed, "-"]
validations:
  - [Balance above amount, The balance is more than the amount received, "-"]
notifications:
  - "None."
audit:
  - Each item creation is audited by Cashiering with the batch number.
acceptance:
  - A migrated UPP shows amount, balance, legacy AR, reference and status in the workbench.
  - The UPP legacy control account equals the total of the migrated UPP balances.
  - With the setting Issue an AR for a migrated unapplied payment false, no AR is issued at load; with true, each migrated UPP gets a BIBS AR that refers to its legacy AR and posts no cash entry.
```

```fr
id: FR-DM-052
title: Automatch rerun of UPP across legacy and new invoices
brd: [BRID 5.2 (p.8), BRID 6.3 (p.9)]
actor: BIBS (payment automatch); Cashiering User
priority: Must have
screens: Unapplied Payments workbench; Payments
description:
  - The payment automatch also takes migrated UPP items. It matches them on the references carried from legacy (invoice number, cover number, PN, payor reference) and applies the balance, oldest invoice first, to the booked invoices found - legacy or new. One item can be applied to both kinds (mixed UPP).
  - Each application posts the UPP side to the sub-ledger of the item (legacy or new) and the invoice side to the sub-ledger of the invoice, and updates the invoice's balances.
preconditions:
  - The item has a balance and at least one reference; the invoice is booked or loaded.
main_flow:
  - Automatch runs after every payment upload and hourly, or the Cashiering User clicks **Run Matching**.
  - BIBS matches each item's references against the Operations ledger.
  - BIBS applies the balance by the component hierarchy to the matched invoices and closes the item when it is used up.
rules:
  - [R1, "Posting by context - legacy UPP to legacy invoice - Dr UPP legacy / Cr PR legacy; legacy UPP to new invoice - Dr UPP legacy / Cr PR; new UPP to legacy invoice - Dr unapplied collections / Cr PR legacy.", Configurable, "Accounting rules of the accounting rule of the payment application (Comptrollership)"]
  - [R2, "Application order is the component hierarchy DST, premium tax / VAT, LGT, FST, other charges, basic (CSHID.022).", Fixed, "-"]
  - [R3, "Commission is realised on collection on the sub-ledger of the invoice (parameter the commission realisation setting of Operations).", Configurable, "Parameters: the commission realisation setting of Operations"]
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
screens: Unapplied Payments workbench (disposition drawer)
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
screens: Unapplied Payments workbench; Disbursement queue
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
  - [R1, "Refund entry - Dr UPP legacy / Cr refund payable; Disbursement pays the refund payable.", Configurable, "Accounting rules of the accounting rule of the refund"]
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
screens: UPP Income Reclassification; My Approvals
description:
  - Exceptional dispositions of UPP to other income are made in a reclassification batch. The Cashiering User selects the items (filters on age, amount, origin), the batch is approved by the Cashiering TL and then by top management, and only then executed. On execution each item's balance is posted from the UPP sub-ledger (legacy or new) to other income and the item is closed.
preconditions:
  - The user has the right "Cash UPP income request"; the items have a balance and no disposition in progress.
main_flow:
  - The user creates a batch (UIR-yyyy-nnnn), adds items and a reason per item.
  - The user submits; the Cashiering TL approves.
  - The top-management approver reviews the batch (list, ages, total) in My Approvals and approves.
  - BIBS executes the batch - posts each reclassification, closes each item and shows the batch as Executed.
alternate_flows:
  - Return at either approval, with a reason; the batch goes back to Draft.
  - An item changes before execution (applied or refunded). BIBS removes it from the batch and reports it.
rules:
  - [R1, "Two approvals - Cashiering TL, then top management; neither is the maker.", Fixed, "-"]
  - [R2, "Entry - Dr UPP (legacy or new) / Cr other income account per DMQ16.", Configurable, "Accounting rules of the accounting rule of the reclassification to income"]
  - [R3, "An executed reclassification is reversed only by a new approved batch of type Reversal.", Fixed, "-"]
validations:
  - [Item with a disposition in progress, "Item {ref} has a disposition in progress", "-"]
  - [Approver is the maker, "A record cannot be authorized by the user who maintained it", MAKER_CHECKER_VIOLATION]
fields_screen: UPP Income Reclassification
fields:
  - [Batch no., Text, "-", "System", "UIR-yyyy-nnnn"]
  - [Items, Grid, "Yes", "Unapplied items with balance", "At least one"]
  - [Reason per item, List, "Yes", "the list of reclassification reasons", "-"]
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
screens: Receive payment (OTC)
description:
  - The Cashiering User receives a payment at the counter against a legacy invoice number or reference. BIBS finds the legacy invoice in the Operations ledger, issues the AR, applies the payment by component and posts it - the cash to unapplied collections and the application from unapplied collections to the Premium Receivable legacy sub-ledger. The receipt appears in the Cash Receipts Book.
preconditions:
  - The legacy invoice is loaded and has an open balance; the user has the permission Cash receipt.
main_flow:
  - The user enters the legacy invoice number (or ARN, policy or PN).
  - BIBS shows the invoice with the LEGACY badge and the application preview.
  - The user enters the payment details and confirms.
  - BIBS issues the AR, applies, posts and updates the invoice balances.
alternate_flows:
  - Payment above the balance. The excess becomes a new UPP item (new context) as today.
rules:
  - [R1, "Entries - AR - Dr bank / Cr unapplied collections; application - Dr unapplied collections / Cr PR legacy by component.", Configurable, "Accounting rules of the accounting rule of the receipt and the accounting rule of the payment application"]
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
screens: Payment uploads
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
  - [R2, "A legacy invoice number is recognised by the patterns of the legacy invoice number patterns (default the EBIX pattern I followed by 8 digits). The QPS pattern is added to it and to the invoice number patterns of Collections when DMQ11 gives the QPS format; until then a row for a QPS invoice is matched by ARN, policy number or PN, or stays unapplied.", Configurable, "Parameters: the legacy invoice number patterns, the invoice number patterns of Collections"]
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

## DP PR and PR2307 batch reversals

```fr
id: FR-DM-070
title: Reverse DP PR legacy invoices in batch
brd: [BRID 7.1 (p.10)]
actor: Accounting User (request); Commission TL or Comptrollership (approval)
priority: Must have
screens: DP PR Legacy Reversal; My Approvals
description:
  - Legacy invoices whose premium was paid directly to the insurer (DP PR) are reversed in a batch. The list comes from an upload (invoice number, amount, reason) or from the Collections "DP PR for Reversal" tags of legacy invoices. The batch is approved before it runs. Each line reverses the premium receivable against DTIP on the legacy sub-ledgers and records the reversal on the invoice.
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
  - [R1, "For legacy invoices the reversal always posts to the GL, whatever the direct-payment reversal posting says, because the legacy premium receivable is in the GL.", Fixed, "-"]
  - [R2, "Entry - Dr DTIP legacy / Cr PR legacy; the Commission Receivable effect follows the entries Comptrollership confirms (DMQ19).", Configurable, "Accounting rules of the accounting rule of the direct-payment reversal"]
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
  - An approved batch of 20 DP PR legacy invoices reverses each open premium receivable and reduces the PR and DTIP legacy sub-ledgers by the same total.
  - A line whose balance changed after the upload fails alone and is reported.
```

```fr
id: FR-DM-071
title: Reverse PR2307 legacy invoices in batch
brd: [BRID 7.2 (p.10)]
actor: Accounting User (request); Cashiering TL or Comptrollership (approval)
priority: Must have
screens: PR2307 Legacy Reversal; My Approvals
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
  - [R1, "Entries - Dr PR2307 legacy / Cr PR legacy (when the amount is still on PR), then Dr DTIP legacy / Cr PR2307 legacy; Commission Receivable effect per DMQ20.", Configurable, "Accounting rules of the accounting rule of the 2307 reclassification, the accounting rule of the 2307 offset against the due to insurer"]
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
screens: Extraction workbench; Remittance batch; Remittance reports
description:
  - The remittance extraction takes every invoice with applied premium not yet remitted - legacy or new. Premium paid in legacy before cutover counts as applied, and amounts remitted in legacy before cutover are not remitted again. The batch, schedule, commission OR and push to Disbursement work as today; the lines of legacy invoices post to the DTIP and Commission Receivable legacy sub-ledgers; the disbursement flows to the Cash Disbursements Book.
preconditions:
  - Legacy invoices are loaded with their paid and remitted amounts.
main_flow:
  - The Remittance User runs the extraction for an insurer.
  - BIBS lists legacy and new invoices with paid AR, DTIP, commission, VAT, WTAX and net due.
  - The user processes the batch as today; on approval BIBS posts each line by its invoice's context.
rules:
  - [R1, "Legacy line entry - Dr DTIP legacy (+ CWT) / Cr Commission Receivable legacy / Cr due to insurer for disbursement.", Configurable, "Accounting rules of the accounting rule of the remittance"]
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
screens: New Endorsement Request; Request page; Posting batches
description:
  - A positive financial endorsement (additional premium) of a legacy policy is processed in BIBS with the Adjustment screens. The policy header must be migrated (FR-DM-040). BIBS computes the change from the legacy original invoice, books an endorsement invoice (BIBS number) whose parent is the legacy invoice, issues the service invoice, posts the entries to the legacy sub-ledgers and keeps the before / after snapshot linked to the endorsement number.
preconditions:
  - The legacy invoice is loaded and its policy header is migrated (capability 9 is subject to the coexistence decision, DMQ22).
main_flow:
  - The user selects the legacy invoice and the endorsement type.
  - BIBS shows the recompute preview per insurer from the legacy original.
  - The request is validated, approved and posted in a batch as for BIBS invoices.
  - BIBS books the endorsement invoice in the legacy ledger context, issues the service invoice and updates the account.
alternate_flows:
  - Header not migrated. BIBS refuses the request.
rules:
  - [R1, "The endorsement invoice of a legacy invoice is in the legacy ledger context; its booking entries use the legacy components.", Fixed, "-"]
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
screens: New Endorsement Request; Request page; Posting batches
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
screens: New Endorsement Request; Request page
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
screens: Reports (Operations - Production Reconciliation)
description:
  - The report lists every change made in BIBS to a legacy invoice in a period, with the original value (frozen at load), the updated value and the delta, per field and component. Changes are endorsement invoices of the invoice's family, corrections, DP PR and PR2307 reversals, write-offs, minimal balance reversals and non-financial changes; payments and remittances are included if BDOI asks (DMQ23).
preconditions:
  - The user has the permission Recon process (Production reconciliation).
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
screens: Legacy Inquiry
description:
  - Historical records not migrated into BIBS (closed invoices, receipts, remittances, endorsements, claims, renewal advices, GL journals, expired policies, documents) are kept either in the legacy system in read-only mode or in the BIBS archive. The BIBS archive is loaded before a legacy system is decommissioned (DMQ24).
  - The Legacy Inquiry screen searches the archive by client, policy or cover number, invoice, receipt, claim, dates and record type, and shows each record read only with its documents.
preconditions:
  - The user has the right "Legacy inquiry view"; archive records are loaded.
main_flow:
  - The user opens Legacy Inquiry and enters a reason for the session (when required).
  - The user searches; BIBS lists the matching records.
  - The user opens a record and its documents.
  - The user exports results (with the right "Legacy inquiry export").
alternate_flows:
  - Record in read-only legacy only. BIBS shows the legacy reference and the link to the legacy system.
rules:
  - [R1, "Archive records and documents are read only; no user can change or delete them.", Fixed, "-"]
  - [R2, "Exports are limited to the archive export limit rows (default 1,000).", Configurable, "Parameters: the archive export limit"]
  - [R3, "Archive records are kept per the retention rules (proposed 5 years online, 15 years archive, umbrella BRD p.45).", Configurable, "Retention rules"]
validations:
  - [No criteria, Enter at least one search criterion, "-"]
  - [No reason, Enter the reason for this inquiry, "-"]
  - [Export above the limit, "The export is limited to {1,000} rows; narrow the search", "-"]
fields_screen: Legacy Inquiry
fields:
  - [Record type, List, "No", "the list of legacy record types", "-"]
  - [Source system, List, "No", "the list of source systems", "-"]
  - [Client name or no., Text, "No", "-", "At least 3 characters"]
  - [Policy / cover no., Text, "No", "-", "-"]
  - [Invoice / receipt / claim no., Text, "No", "-", "-"]
  - [Date from / to, Date, "No", "-", "From not after To"]
  - [Reason, List + text, "Cond.", "the list of access reasons", "Mandatory when the access-reason setting"]
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
screens: Legacy Access Log
description:
  - Every access to legacy history in BIBS is logged - user, time, source address, action (search, view, download, export), criteria, records and reason. The log cannot be changed or deleted. Compliance reviews it on screen and receives a monthly digest; unusual exports raise an alert.
preconditions:
  - The user has the right "Legacy access log view".
main_flow:
  - The reviewer opens the log and filters by user, date, action or record.
  - BIBS shows the entries; the reviewer exports them.
rules:
  - [R1, "Entries can only be added; no entry can be changed or deleted.", Fixed, "-"]
  - [R2, "An alert is raised when a user exports more than the unusual-export alert limit rows in a day (default 5,000).", Configurable, "Parameters: the unusual-export alert limit"]
validations:
  - [Access without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - Monthly digest to the reviewers; alert Unusual legacy archive access.
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
screens: Cutover
description:
  - The cutover is run from a plan in the console - tasks with phase, owner role, dependencies, planned and actual times, status and evidence. The same plan structure is used for the four trial migrations, the dress rehearsal and the production cutover, so timings are measured before go-live. The runbook is an export of the plan.
  - In every plan the reference data and the client master are loaded and accepted first. They are the prerequisites of renewal testing, which the Drop 1 SIT and UAT run on the trial-migration data (concept paper R10, section VI).
preconditions:
  - The user has the right "Data migration cutover manage".
main_flow:
  - The lead creates a plan of kind Mock run, Dress rehearsal or Production cut-over from the template.
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
screens: Cutover (Go / No-go tab)
description:
  - Go-live is decided on criteria that BIBS measures - all Day-1 objects accepted, counts and amounts reconciled, zero financial rejects, Migration Clearing 0.00, legacy control accounts equal to their sub-ledgers, empty client review queue, business smoke test passed, rollback snapshot taken, hypercare in place, the preliminary trial balance signed as the provisional opening with the legacy GL restricted to the named FY2027 adjustment users, and, for Renewal, the headers of every expiry up to 31 May 2028 loaded, the RA-sent file loaded and the January staffing plan confirmed. The board records the decision with the values it saw.
preconditions:
  - The production plan has reached the go / no-go task.
main_flow:
  - BIBS computes the criteria and shows each with its value and status.
  - The board decides GO or NO-GO with a comment.
  - On NO-GO the fallback task starts - BIBS is restored to the rollback backup (snapshot) and legacy is reopened.
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
screens: Run-off and Decommissioning; Renewal screens
description:
  - Renewal in BIBS is enabled from go-live. Policies expiring before go-live renew or lapse in legacy. The policies expiring from go-live to 31 May 2028 are extracted by BIBS at go-live from the migrated headers (FR-DM-124); later expiries are extracted by BIBS from the migrated headers on the normal lead time. A legacy policy renews on the new-business path pre-filled from its header, or as is on the package that the Renewal sanitation resolves (FR-DM-034), and the new BIBS account refers to the legacy policy.
  - The run-off tracker shows, per expiry month, how many legacy policies were in force at go-live, renewed in BIBS, not renewed, lapsed or still open.
preconditions:
  - Headers and the RA-sent file are loaded; the Renewal module is live.
main_flow:
  - At go-live, the go-live extraction creates the candidates of the expiries up to 31 May 2028 with the source Migrated policy (FR-DM-124).
  - Each day, Renewal extraction asks for migrated headers expiring at the lead date.
  - Each month, BIBS refreshes the run-off tracker.
rules:
  - [R1, "Daily extraction after go-live - migrated headers expiring at the business date + the renewal extraction lead time (default 140); headers that already have a candidate from the go-live extraction are skipped, so the first new expiry is 1 June 2028, extracted on 13 January 2028.", Configurable, "Parameters: the renewal extraction lead time"]
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
screens: Run-off and Decommissioning
description:
  - Each legacy system has a decommissioning checklist with measurable criteria and sign-offs. A second checklist closes the legacy context in BIBS when the legacy positions have run off.
preconditions:
  - The user has the right "Data migration cutover manage" (checklist) or the right "Data migration signoff" (sign-off).
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
screens: Renewal candidates (day-1 worklist); Reconciliation
description:
  - The renewals of the policies expiring from go-live to 31 May 2028 are processed in BIBS after go-live; no renewal candidate is carried from legacy (DMQ37, answered 26 September 2026). At go-live, before business opens, BIBS extracts every migrated policy header expiring in this window into the renewal pipeline, prioritised by expiry date, with the January expiries flagged urgent.
  - A renewal advice already sent by hand before go-live is recorded on the candidate from the RA-sent file (FR-DM-125) and is not sent again. A header whose cover has a later term booked in legacy that starts on its expiry is not extracted, because it is already renewed.
  - The January expiries get days to four weeks instead of 140 days. The day-1 priority queue, a staffing plan for January, and a Renewal team trained and rehearsed on the Trial migration 4 and dress-rehearsal data before go-live mitigate this (Cutover Runbook).
preconditions:
  - The go / no-go decision is GO.
  - The policy headers (P01) include every policy expiring from go-live to 31 May 2028 and every renewal term booked in legacy that starts on or after go-live (FR-DM-040); the RA-sent file is loaded (FR-DM-125).
main_flow:
  - At go-live 04:00 the Renewal go-live extraction runs.
  - BIBS creates a candidate with the source Migrated policy for each header in the window not renewed in legacy - at the first stage, with the expiry date as priority, the flag Urgent when the expiry is on or before 31 January 2028, and the RA already sent when the RA-sent file has a row.
  - BIBS runs the go-live renewal extraction check per expiry month.
  - From 08:00 the Renewal processing team works the day-1 worklist, urgent January expiries first.
alternate_flows:
  - Daily extraction. From the day after go-live the daily Renewal extraction skips headers that already have a candidate; the first new expiry is 1 June 2028, extracted on 13 January 2028.
  - Rerun. A rerun of the go-live extraction creates no duplicate candidate.
rules:
  - [R1, "Go-live extraction window - expiries from the go-live date to the end of the go-live renewal window (default 31 May 2028).", Configurable, "Parameters: the go-live date parameter, the end of the go-live renewal window"]
  - [R2, "Candidates expiring on or before the urgent-renewal date (default 31 January 2028) are flagged Urgent; the worklist is ordered by expiry date.", Configurable, "Parameters: the urgent-renewal date"]
  - [R3, "An RA recorded as already sent is not sent again; a revised RA is a user action.", Fixed, "-"]
  - [R4, "A header whose cover has a later migrated term starting on its expiry is not extracted.", Fixed, "-"]
  - [R5, "Extraction check per expiry month - headers expiring in the window = candidates created + headers renewed in legacy; RA-sent rows loaded = candidates with the RA already sent.", Fixed, "-"]
validations:
  - [Extraction check break, "{month} - {x} headers expiring, {y} candidates, {z} renewed in legacy", "-"]
notifications:
  - A break raises alert Migration reconciliation break to the Renewal processing team and the Data Migration Lead.
audit:
  - Each candidate keeps the legacy reference, the extraction run and, when given, the RA-sent batch.
acceptance:
  - At go-live the header C-2027-004512-01, expiring on 15 January 2028, is a candidate flagged Urgent among the January expiries at the top of the worklist in expiry-date order; its RA sent on 16 November 2027 is recorded and no RA is queued for it.
  - A header expiring on 31 May 2028 is extracted at go-live; a header expiring on 1 June 2028 is not, and the daily extraction takes it on 13 January 2028.
  - The header C-2027-004600-01, whose renewal term C-2027-004600-02 was booked in legacy, is not extracted.
  - For each month January to May 2028 the go-live renewal extraction check balances.
```

```fr
id: FR-DM-125
title: Load the renewal advices already sent before go-live
brd: [BRID 12.1 (p.12)]
actor: Renewal processing team (maker and checker); Migration Operator; Head of the Renewal processing team (data owner)
priority: Must have
screens: Extracts; Batches (Issues tab); Resubmissions; Reconciliation
description:
  - The RMEL and the dispositions are kept in Excel trackers today (DMQ38, answered 26 September 2026). The migration takes one thing from them - the renewal advices (RAs) already sent by hand before go-live for the expiries from go-live to 31 May 2028 - so that BIBS does not send them again. The trackers themselves are not migrated.
  - The Renewal processing team compiles the RAs in the Excel template of layout P03, one row per expiring term (legacy policy reference, cover, expiry, RA date and reference, channel, recipient, proposed insurer and premium quoted, sender, tracker and sheet). The rows are validated; rejected rows come back in an Excel rejection report with correction columns; the maker corrects them and the checker approves the resubmission. The Head of the Renewal processing team owns the object.
preconditions:
  - The policy headers (P01) of the same load are loaded.
main_flow:
  - The maker compiles the file from the trackers; the checker compares it with the trackers (row counts per tracker and 10 sample rows) and releases it.
  - The operator uploads the file with its control file and validates the batch.
  - The rejected rows of the P03 batch are available to the maker and the checker on the batch (Rejects, export to Excel).
  - The maker corrects each rejected row, fills the correction columns and prepares a resubmission file with the corrected rows only.
  - The checker reviews each correction against the tracker and approves the resubmission; the operator loads it as a rerun batch.
  - After the load approval (G4) BIBS loads the valid rows; the go-live extraction records them on the candidates (FR-DM-124).
alternate_flows:
  - Return. The checker returns the resubmission with a reason; the maker corrects it.
  - Deadline passed. Rows still rejected at the deadline are not loaded; they are listed for the day-1 queue, and the processor checks the tracker before sending an RA for such a policy.
  - Trial migrations. The file is sent in every trial migration from Trial migration 1; in production once, with the RAs sent up to the last legacy business day.
rules:
  - [R1, "Rows are accepted for expiries from the go-live date to the end of the go-live renewal window (default 31 May 2028) whose header is in P01 and was not renewed in legacy.", Configurable, "Parameters: the go-live date parameter, the end of the go-live renewal window"]
  - [R2, "The RA date is not after the last legacy business day; an RA date more than 140 days before the expiry is a warning.", Fixed, "-"]
  - [R3, "A resubmission is approved by a checker who is not its maker; the checker holds the permission Data migration resubmit approve for object P03.", Fixed, "-"]
  - [R4, "In production, resubmissions are accepted until the resubmission deadline (default T-1 12:00).", Configurable, "Parameters: the resubmission deadline"]
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
  - The rejected rows of P03 are available to the maker and the checker after each validation; a submitted resubmission goes to the checkers in My Approvals.
audit:
  - Each loaded row keeps its tracker, batch and resubmission; corrections and approvals are kept.
acceptance:
  - A file with the RA of C-2027-004512-01 sent on 16 November 2027 by e-mail loads, and at go-live the candidate shows the RA as already sent.
  - A row whose cover C-2027-009999 has no header, a row expiring on 15 June 2028 and a row with an RA date of 30 December 2027 are rejected with their messages and appear in the rejection report with the correction columns.
  - A resubmission prepared and approved by the same user is refused; approved by the checker, it loads as a rerun batch.
  - A row still rejected at the deadline is not loaded and is listed for the day-1 queue.
```

# Messages

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: messages
```

# Notifications and alerts

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: notifications
```

# Reports

Every report exports to Excel and PDF from the Report Centre. The migration reports are in the category Data Migration; the legacy invoice and unapplied payment reports are with the Operations and Cashiering reports. The Collections item reports, the DTIP status reports, the Cashiering receivable and commission reports, the remittance schedules and the special remittance register have the Origin filter (All origins, BIBS, Migrated).

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-reports
```

# Workflow and status model

## Data object and batch states

<!-- table: widths=3.2,4.4,9 caption="States of the migration records" size=8.5 -->
| Record | State | Meaning |
|---|---|---|
| Data object | Proposed | Registered with its criteria; decision not submitted |
| Data object | For decision | Decision submitted to the business owner |
| Data object | Decided | Class approved (G1) |
| Data object | Ready | Layout frozen and code maps approved (G2) |
| Data object | Accepted | Loaded, reconciled and accepted in the environment (G6) |
| Extract | Received, Checked, Rejected | Intake checks of FR-DM-010 |
| Extract | Staged, Purged | Rows in staging; files purged after the retention days |
| Batch | Planned | Created with its extracts |
| Batch | Validating, Validated | Rows mapped and validated (G3) |
| Batch | Loading | Load approved (G4) and running |
| Batch | Loaded, Loaded with rejects, Failed | Load result |
| Batch | Reconciled | L1-L5 run; breaks explained (G5) |
| Batch | Accepted | Accepted (G6) |
| Batch | Rollback requested, Rolled back | Rollback of FR-DM-015 |

## Approval workflows

<!-- table: widths=4.2,5.6,3.4,3.4 caption="Approval workflows" size=8.5 -->
| Workflow | Stages | Maker permission | Approver permission |
|---|---|---|---|
| Decision of a data object | Proposed, For decision, Decided | Data migration object manage | Data migration decision approve |
| Code map version | Draft, Submitted, Approved, Superseded | Data migration mapping edit | Data migration mapping approve |
| Rollback of a batch | Requested, Approved, Done, Rejected | Data migration rollback request | Data migration rollback approve |
| Unapplied to Income batch | Draft, For team lead approval, For top management, Executed | Cash UPP income request | Cash disposition approve (team lead), then Cash UPP income approve (top management) |
| DP PR Legacy Reversal batch | Draft, For approval, Approved, Posted | Legacy reversal request | Legacy reversal approve (Commission team lead) |
| Legacy PR 2307 Reversal batch | Draft, For approval, Approved, Posted | Legacy reversal request | Legacy reversal approve (Cashiering team lead) |
| Opening-balance adjustment | Prepared, For approval, Approved, Posted | Data migration trueup prepare | Data migration trueup approve (Head, Comptrollership) |
| Resubmission of corrected rows | Prepared, Approved, Returned | Data migration DQ resolve | Data migration resubmit approve |

Every approval stage allows a return with a reason, which sends the record back to the maker's stage.

## Sign-off gates

<!-- table: widths=2.6,6,4,4 caption="Sign-off gates" size=8.5 -->
| Gate | What is signed | Signer | Evidence |
|---|---|---|---|
| G1 | Class of the object | Business owner | Decision record |
| G2 | Code map versions and layout | Business owner | Map versions |
| G3 | Validation result and waivers | Data Steward; waivers by the owner | Data-Quality Issues report |
| G4 | Approval to load | Data Migration Lead | Batch counts |
| G5 | Reconciliation | Reconciliation approver | Reconciliation Summary; Migration Clearing and Legacy Control Accounts |
| G6 | Object accepted | Business owner and Data Migration Lead | Sample checks on screen |
| G7 | Go-live | Go / no-go board | Go / no-go criteria |

## Documents

The load templates (one CSV template and one Excel workbook per layout, the control-file template and a workbook with every template), the code map templates and the rejected rows of a batch are exported from the Migration Console in the current frozen layouts; they are the same as the layouts of the Migration Workbook (file 03). The cut-over plan is exported from the Cutover screen in Excel.

# Interfaces and integration

Figure 4 shows the interfaces of the migration module. It writes into BIBS only through the functions of the owning modules and serves the Renewal and Customer Servicing modules.

![Interfaces of the migration module (dashed = on hold or link only)](figures/brd13_integration.dot)

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| Extract upload (console) | In | Data and control files per object | 1.1b | In scope |
| Secure file drop from BDOI IT | In | Same files, picked up by the system | 1.1b | On hold |
| Reference masters (LOV, catalogue, sales organisation) | Out | Mapped and created values | 3.1 | In scope |
| Client master and screening | Out | Migrated clients; one full screening run | 2.1 | In scope |
| Accounts | Out | Migrated policy headers | 4.1 | In scope |
| Operations ledger and Cashiering | Out | Legacy invoices, UPP | 5.1-10.1 | In scope |
| GL | Out | Opening entries, provisional opening trial balance and FY2027 true-ups | 1.1b | In scope |
| Renewal (legacy policies; package map) | Out | Migrated headers for the go-live and daily extractions; renewal advices already sent; package code map for the package check of the sanitation | 12.1 | In scope |
| Customer Servicing (legacy account lookup) | Out | Legacy references and archive records | 11.1 | In scope |
| Legacy read-only systems | Link | Address of the read-only legacy application shown on Legacy Inquiry | 11.1 | On hold |
| Write-back to QPS / EBIX | Out | Not needed after the freeze (legacy read-only) | - | Out of scope |

> [!NOTE] Interfaces on hold
> The secure file drop, delta extracts sent without an upload, the bulk transfer of legacy documents and the read-only legacy addresses depend on BDOI IT (DMQ24, DMQ28; PR-DM-03, PR-DM-04). Until they are named, files are uploaded on the Extracts screen; adding the file drop is a setting of the intake, with the same checks.

# Non-functional requirements

The BRD refers every usage table to "the consolidated NFR requirements for BDO Insure Core Modernization project" (p.13-14), which is not in the pack (DMQ29). The values below are proposals.

<!-- table: widths=3,5.4,5.6 caption="Non-functional requirements" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Volumes | Not given | Loads sized for 1,000,000 client rows and 500,000 open-item rows; planning bounds from the umbrella BRD (clients 2020 to present, p.43; 21,200 bookings and 25,800 renewal accounts a month, p.44-45) |
| Cutover window | Not given | 48 hours over a weekend from the last legacy EOD to go / no-go; BIBS open 08:00 on the first business day; at the year-end boundary (recommended 3 January 2028), with FY2027 true-ups until about April 2028 |
| Load performance | Not given | At least 50,000 rows an hour per partition, 4 partitions; reconciliation of an object within 1 hour; full production load within 20 hours, proven in the dress rehearsal |
| Staging security | Hosting appendix | Masked data outside production; staging and files purged within 5 days of sign-off; access only for migration roles and from the Philippines; encryption at rest and in transit |
| Audit | Not given | Every intake, validation, load, rerun, rollback, reconciliation, sign-off and archive access is audited |
| Retention | Consolidated NFR; umbrella p.45 (5 years online, 15 years archive) | Archive and access log by retention rule; migration evidence kept as project records (proposed 10 years) |
| Availability and recovery | Consolidated NFR | Same as BIBS; full backup (snapshot) of production before each production load gate |

# Configuration items

## Parameters

<!-- table: widths=7.4,3,6.2 caption="Data Migration parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| the environment class | Non-production | Masking applies in every environment except production |
| the go-live date | - | Go-live date; start of the go-live renewal extraction window |
| the opening value date | 1 January 2028 | Value date of the opening entries and of the true-ups (first day of the opening period) |
| the end of the go-live renewal window | 31 May 2028 | Last expiry of the go-live renewal extraction |
| the urgent-renewal date | 31 January 2028 | Candidates expiring up to this date are flagged Urgent |
| the resubmission deadline | T-1 12:00 | Last approval time of a resubmission in production |
| the staging retention days | 5 | Days after sign-off before staging and files are purged |
| the rows per load transaction | 500 | Rows per load transaction |
| the partitions loaded in parallel | 4 | Parallel load partitions |
| the amount tolerance | 0.00 | Tolerance of amount reconciliation |
| the error-rate limit of master data | 0.5 | Maximum percent of rejected or waived master-data rows |
| the error-rate limit of financial objects | 0 | Maximum percent of rejected financial rows |
| the automatic merge score | 90 | Score from which clients merge automatically |
| the review score | 60 | Score from which a pair goes to review |
| the prefix of colliding invoice numbers | Yes | Prefix the source system when a legacy invoice number is already used |
| the legacy invoice number patterns | EBIX pattern (I and 8 digits) | Patterns by which the payment matcher recognises a legacy invoice number; the QPS pattern is added after DMQ11 |
| the setting Issue an AR for a migrated unapplied payment | No | Issue a BIBS AR (without cash posting) for each migrated UPP (DMQ14) |
| the archive export limit | 1000 | Rows per archive export |
| the unusual-export alert limit | 5000 | Exported rows per user and day that raise an alert |
| the access-reason setting | Yes | A reason is required per Legacy Inquiry session |
| the read-only address of EBIX, the read-only address of QPS | - | Links to the read-only legacy systems |
| the setting Legacy invoices count in incentive runs | No | Legacy invoices count in incentive runs |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values provided |
|---|---|
| the list of source systems | EBIX; QPS; ISYS; Excel; CMS |
| the list of object categories | Reference; Client; Policy; Open item; GL; History |
| the list of data trust levels | High; Medium; Low |
| the list of break reasons | Timing difference between extract and ledger; Rounding in the legacy extract; Rows excluded by the data owner; Error in the legacy source, corrected by manual entry; Code map difference; Other (see explanation) |
| the list of waiver reasons | Record not needed in BIBS; Entered manually in BIBS after go-live |
| the list of access reasons | Internal or external audit; Compliance review; Regulator request; Client request or complaint; Claim on a legacy policy; Other (give details) |
| the list of legacy record types | Client; Policy; Invoice; Receipt; Remittance; Endorsement; Claim; GL journal; Renewal advice; Letter; Other |
| the list of reclassification reasons | Client cannot be identified; Unclaimed after follow-up; Below refund threshold; Other (see remarks) |

## Masters and rules maintained by the business

<!-- table: widths=4.4,4.6,7.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (approved by) | FR |
|---|---|---|
| Data object register and decisions | Data Migration Lead (business owner) | FR-DM-001, 002 |
| Layouts, validation rules, masking rules | Data Steward (Data Migration Lead) | FR-DM-010, 013 |
| Code maps | Data Steward (business owner) | FR-DM-011 |
| Package code map (loaded for the Renewal sanitation) | TSU (Product Owner, Marketing Business System) | FR-DM-034 |
| RA-sent file template and rejected-row review | Renewal processing team, maker (checker of the team; owner the Head of the Renewal processing team) | FR-DM-125 |
| FY2027 adjustment register and the legacy GL access list | Comptrollership GL lead (Head, Comptrollership) | FR-DM-023, 024 |
| Matching and survivorship rules | Data Steward (business owner of clients) | FR-DM-031 |
| Legacy control accounts, Migration Clearing and the legacy accounting rules | Comptrollership (maker-checker on accounting rules) | FR-DM-021, 050-080 |
| Go / no-go criteria and cutover plan template | Data Migration Lead (Program Manager) | FR-DM-120, 121 |
| Decommissioning checklist template | Program Manager | FR-DM-123 |

# Cross-BRD interface contract

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: contract
```

# Part C - Reconciliation and the sign-off gates per object {-}

# Purpose and scope

BRID 1.1b asks that, after cutover, "all data objects and items are reconcilable from source to target" from the generated data migration reports (BRD p.7). This part states how each object is reconciled, what BDOI verifies on the BIBS screens, how breaks are handled, which evidence is kept, and the forms that BDOI signs. It applies to every trial migration, the dress rehearsal and the production cutover, to the FY2027 true-ups after go-live, and to the archive loads before decommissioning.

It completes Part A (Reconciliation approach) and the functional specification of Part B (FR-DM-020 to FR-DM-024, FR-DM-003). Account codes are not named: the legacy control accounts and the Migration Clearing account are assigned by Comptrollership (DMQ18, register DCR-197).

# Reconciliation levels

Five levels are run by BIBS after each load and on demand (Reconciliation screen). Levels L1 to L4 apply to every object; L5 to the financial objects.

<!-- table: widths=1.2,3,6.2,6.2 caption="Reconciliation levels" size=8.5 -->
| Level | Measure | How it is computed | Pass rule |
|---|---|---|---|
| L1 | Record counts | Control file row count; rows received, staged, valid, warning, invalid, loaded, skipped, rejected, excluded (per batch and its reruns) | Received = control count; loaded + skipped + rejected + excluded = staged; rejected rows are 0 for financial objects or excluded by the owner |
| L2 | Amounts | Per amount column and currency: control total, staged sum, and the value read from BIBS (ledger components of the origin Migrated, unapplied balances, journal lines) | Difference 0.00 (setting the amount tolerance, default 0.00) at each stage |
| L3 | Hash totals | Hash total of the key column as the layout defines it; SHA-256 per staged row; count of distinct keys in the cross-reference | Control = staged = cross-reference |
| L4 | Fields | Every mapped field of every loaded row read back through the owning service and compared with the staged, mapped value | Equal; a difference explained by the code map (mapped value) is not a break |
| L5 | GL | Migration Clearing balance per branch and currency, analysed per legacy control account (trial balance line against the opening detail posted to that account); each legacy control account against its legacy sub-ledger (ACSL GL-SL reconciliation in the legacy ledger context) | Migration Clearing 0.00; per control account TB line = opening detail; control account = sub-ledger |

**Breaks.** Any difference is a break (status Break). A break is fixed by a rerun (new extract, new map version or a corrected row) or explained. An explanation gives a reason from the list of break reasons (timing difference between extract and ledger, rounding in the legacy extract, rows excluded by the data owner, error in the legacy source corrected by manual entry, code map difference, other) and a text, and is approved by the reconciliation approver. A reconciliation cannot be signed (gate G5) while a break is open.

**Reports.** Reconciliation Summary (object by level with status), Reconciliation Detail (lines, breaks, explanations, approvals), Rejected Rows (rejected and invalid rows with their messages), Migration Clearing and Legacy Control Accounts (Migration Clearing per branch and currency; legacy control accounts against sub-ledgers). All export to Excel and PDF.

# Control totals BDOI sends

Every data file comes with a control file (workbook sheet "Control File" and template `CONTROL_template.ctl.csv`). It always carries the row count and the SHA-256 of the data file, plus the hash total of the key and the amount totals of the layout:

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-controls
```

The workbook sheet "Control Totals" lists the same measures one per row, with columns for the value from BDOI, the value in BIBS, the difference and the status. It is the tie-out sheet of L1 to L3 for each cycle.

# Reconciliation per data object

<!-- table: widths=1.2,2.6,3.4,2.6,2.6,4.2 caption="What is reconciled per object" size=8 -->
| Object | L1 counts | L2 amounts | L3 hash | L4 fields | L5 GL / business check |
|---|---|---|---|---|---|
| R01-R08, R11 | Values received, mapped, created, rejected; package code map entries | - | Distinct keys | Created values (name, status, dates); package code map entries in the Renewal package map | Unmapped-code report empty; every Create value authorised in its master; package code map entries loaded into the Renewal package map equal the approved version |
| R09 | Payees received and loaded | - | Distinct payee codes | All loaded fields | Payee list in Disbursement |
| C01, C02 | Legacy clients received, clusters, merged, new, loaded | - | Distinct legacy client numbers = cross-reference entries | Identity, contact, segment, KYC fields of the survivor | Client review queue empty; each legacy number resolves to one BIBS client |
| C03 | Accounts received and loaded | - | Distinct (client, account) | All fields | - |
| P01, P01S | Headers received and loaded; shares | Sum insured, gross and net premium per currency | Distinct legacy policy references | All header fields and shares | Headers linked to their clients and legacy invoices |
| P03 | RA-sent rows received, loaded, rejected; resubmitted rows | Proposed premium per currency | Distinct legacy policy references | RA date, reference, channel, sender, tracker | At go-live, the go-live renewal extraction check per expiry month January-May 2028: P01 headers expiring from T to 31 May 2028 = candidates + renewals booked in legacy; loaded P03 rows = candidates with the RA already sent; every January candidate flagged Urgent |
| F01, F01S, F01C | Invoices, share rows, component rows | Booked, adjusted, paid, remitted, written off and open per component and currency | Distinct invoice numbers | Header fields, shares, every component and bucket | L5 - Premium Receivable, PR2307, DTIP, Commission Receivable (legacy) against the legacy invoices; Migration Clearing 0.00 |
| F02 | UPP items | Amount and balance per currency | Distinct UPP references | All fields, stage and disposition | L5 - Unapplied Collections (legacy) against the migrated UPP balances |
| F03 | Rows per record type | Promise and installment amounts | Distinct (invoice, type, seq) | All fields | Worklist items show the carried promises and assignments |
| F04, F06 | Rows per record type | Amount per currency | Distinct keys | All fields | Holds and PDCs visible in Remittance and Cashiering |
| G01 | Trial balance lines | Debit and credit per branch and currency, in currency and PHP | Lines per branch | Account, branch, currency and amounts per line | Provisional opening - balance sheet in BIBS = preliminary December TB per branch and currency; FY2027 result on retained earnings; P&L accounts 0.00; Migration Clearing 0.00 |
| G03 (G03, G03D) | Journal lines and detail lines per true-up | Debit and credit per branch and currency; detail per account | Distinct legacy journals | Account, branch, currency, amounts; item and component of each detail line | True-up reconciliation (section 5.1) |
| H01, H02 | Records per record type; documents | Amount per record type and currency | Distinct keys per record type; SHA-256 per document | Labelled legacy columns | Samples found in Legacy Inquiry with their documents |

# GL reconciliation: Migration Clearing

The open-item objects and the trial balance are loaded from two independent sources in legacy: the invoice and UPP detail (F01, F02) and the GL (G01). Migration Clearing proves that they agree (Figure 1):

- each legacy invoice and each UPP item is loaded with an opening entry whose balancing line goes to Migration Clearing;
- the trial balance is loaded as opening journals; its lines on the legacy control accounts (premium receivable, PR2307, DTIP, commission receivable, unrealised commission, deferred VAT, unapplied collections) are mapped to Migration Clearing instead of the legacy control accounts, because the opening entries create those balances in detail;
- when the detail equals the GL, Migration Clearing is 0.00 per branch and currency.

![Migration Clearing nets to zero when the detail equals the legacy GL](figures/dm_clearing.dot){width=15}

**Worked example** (one branch, PHP, one invoice and one UPP item; made-up figures; the unrealised commission follows DMQ13 and is taken here as fully unrealised):

<!-- table: widths=6.4,3.4,3.4,3.4 caption="Migration Clearing for one branch" size=8.5 -->
| Line | Debit | Credit | Effect on Migration Clearing |
|---|---|---|---|
| F01 opening of I00123456 - Premium Receivable - Legacy | 10,000.00 | | |
| F01 opening - Commission Receivable - Legacy | 4,480.00 | | |
| F01 opening - DTIP - Legacy | | 23,050.00 | |
| F01 opening - Unrealised Commission and Deferred VAT - Legacy | | 4,480.00 | |
| F01 opening - balancing line to Migration Clearing | 13,050.00 | | Debit 13,050.00 |
| F02 opening of UPP-2026-004321 - Unapplied Collections - Legacy | | 5,000.00 | |
| F02 opening - balancing line to Migration Clearing | 5,000.00 | | Debit 5,000.00 |
| G01 trial balance - legacy control-account lines (PR 10,000.00 Dr, commission 4,480.00 Dr, DTIP 23,050.00 Cr, unrealised and deferred 4,480.00 Cr, UPP 5,000.00 Cr) mapped to Migration Clearing | | 18,050.00 (net) | Credit 18,050.00 |
| G01 trial balance - cash in bank 18,050.00 Dr, mapped to the BIBS bank account | 18,050.00 | | - |
| **Migration Clearing** | | | **0.00** |

**A missing invoice.** A difference on Migration Clearing has the sign of the trial balance, because the TB puts the legacy control-account lines on Migration Clearing with their own sign and the opening entries put the detail there with the opposite sign. So an invoice missing from F01 leaves a **debit** equal to its net receivable position (open premium receivable, PR2307 and commission receivable, less open DTIP, unrealised commission and deferred VAT) when that is positive, and a **credit** when it is negative (FR-DM-021 acceptance 3). Two cases, made-up figures:

- F01 missed an invoice paid in full and not remitted, with DTIP 5,000.00 open and commission receivable 800.00 (realised on collection): Migration Clearing shows a **credit of 4,200.00** in that branch.
- F01 missed an unpaid invoice with premium receivable 3,000.00, DTIP 3,000.00, commission receivable 480.00 and the same 480.00 unrealised: Migration Clearing nets to 0.00. The per-account comparison of the report Migration Clearing and Legacy Control Accounts finds it: the TB is 3,000.00 debit higher than the detail on Premium Receivable - Legacy, 3,000.00 credit higher on DTIP - Legacy, and 480.00 higher on each commission account.

Either break is investigated and fixed or explained before go / no-go.

**Legacy control accounts against sub-ledgers.** After the loads, the ACSL GL to Sub-ledger Reconciliation is run in the legacy ledger context: each legacy control account must equal the open positions of the legacy invoices (or legacy UPP) behind it. This is criterion 6 of the go / no-go and is repeated daily in hypercare and at every month-end until the legacy context closes.

## True-up reconciliation (FY2027 closing and audit adjustments)

With the year-end cut-over (DMQ39, option A, recommended), the GL opening is provisional. After the freeze the legacy GL accepts only the FY2027 closing and audit adjustments of the Comptrollership users on the signed access list, and each adjustment reaches BIBS in a true-up (FR-DM-023): true-up 1 after the legacy year-end close (about 18-21 January 2028), interim true-ups only when needed, and the final true-up after the audited financial statements (about March-April 2028). Each true-up comes with the legacy trial balance after the adjustments (G01, version TU1, TU2 or Final), the adjustment journal lines (G03), their open-item detail on the legacy control accounts (G03D) and the legacy journal listing since the freeze. BIBS runs the report Opening-Balance Adjustment Reconciliation after the posting; the Head of Comptrollership signs the true-up only when every check is met (FR-DM-024).

<!-- table: widths=2.6,7.4,6.6 caption="True-up checks (report Opening-Balance Adjustment Reconciliation)" size=8.5 -->
| Check | Rule | What a break usually means |
|---|---|---|
| Cut-off | Legacy journal listing since the freeze = FY2027 adjustment register = journals of the true-ups so far; every journal dated in FY2027, posted after the freeze by a user on the access list; no posting by a legacy business module | A journal posted outside the register, by the wrong user, or in FY2028 |
| Movement | Per BIBS account, branch and currency: true-up n = legacy TB of true-up n - legacy TB of the previous true-up (or of the provisional opening), both mapped, P&L netted into retained earnings | A legacy journal missing from G03, or a TB extracted at a different time |
| Balance | Per BIBS account, branch and currency: provisional opening + true-ups 1 to n = legacy TB of true-up n, mapped (P&L netted into retained earnings; legacy control accounts replaced by the open-item detail) | An earlier true-up incomplete, or a mapping change between true-ups |
| Clearing and sub-ledgers | Migration Clearing 0.00 per branch and currency; each legacy control account = its legacy sub-ledger (ACSL, legacy ledger context) | A control-account adjustment whose G03D detail is missing or on the wrong item |
| Opening period | When January 2028 was reopened for the posting: no journal other than the true-up posted into it during the window; the period closed again the same day | The reopen window used for other postings |

**Worked example** (one branch, PHP, made-up figures). True-up 1 brings two FY2027 adjustments: a closing accrual of professional fees of 80,000.00 (Dr expense, Cr accrued expenses) and an audit write-off of the 3,000.00 premium receivable of legacy invoice I00300002 (Dr bad debts, Cr Premium Receivable - Legacy), sent with its detail line.

<!-- table: widths=6.8,3.2,3.2,3.4 caption="True-up 1 in BIBS (journal MIG-TU-1, value date 1 January 2028)" size=8.5 -->
| Line | Debit | Credit | Note |
|---|---|---|---|
| Retained earnings (FY2027 expense of the accrual) | 80,000.00 | | P&L of FY2027, not FY2028 |
| Accrued expenses | | 80,000.00 | Balance-sheet account |
| Retained earnings (FY2027 bad debts) | 3,000.00 | | P&L of FY2027 |
| Migration Clearing (legacy control-account line of the write-off) | | 3,000.00 | Replaced by the detail |
| Detail - Migration Clearing / Premium Receivable - Legacy, invoice I00300002 basic | 3,000.00 | 3,000.00 | Legacy written off on the invoice; open balance 0.00 |
| **Migration Clearing after true-up 1** | | | **0.00** |

Movement check: the legacy TB after the close differs from the preliminary TB by exactly these amounts on the mapped accounts; balance check: the BIBS opening plus true-up 1 equals the legacy post-close TB; the FY2028 P&L is unchanged.

# Business verification on screen (gate G6)

L1 to L5 prove that what BDOI sent is what BIBS holds. Business verification proves that what BIBS holds means what the business expects: the right client, the right policy, the right balance, on the screens users will work with. The data owner (or the steward on the owner's behalf) checks a sample per object in the environment of the cycle and signs the object acceptance.

## Sample plan

The Migration Lead draws the samples from the loaded batch (random with a fixed seed recorded on the form, plus the targeted records). Samples are checked against the legacy screen or a legacy report of the same as-of date.

<!-- table: widths=1.6,7.8,7.2 caption="Business verification samples per object" size=8 -->
| Object | Sample | What is checked on the BIBS screen |
|---|---|---|
| R01-R08, R11 | Every Create value; 10 mapped values per map set; every legacy package with more than one BIBS target (conditional package code map entries); every receipt series | Value, description and status in the master; the package code map entries of each split in the Renewal package map; next AR / OR number = legacy last used + 1 |
| C01-C03 | 30 random clients per source system; 20 merged clusters; 10 decisions of the review queue; 10 corporate clients | Client search by legacy number; name, identity, contacts, segment, AO, KYC status and review date; cluster shows every legacy number |
| P01 | 30 random headers; 10 with several insurers; 10 expiring within 140 days | Account search by policy and legacy reference; dates, insurer and shares, sum insured, premium, client link, legacy invoices listed |
| P03 | 10 rows per tracker; every resubmitted row; at go-live 20 candidates per expiry month January-May 2028 | RA date, reference and channel against the tracker; at go-live the candidate shows the RA already sent, January candidates Urgent and in expiry-date order |
| F01 | 20 largest open balances per currency; 30 random; 10 endorsement or cancellation invoices; 10 direct-payment; 10 with 2307; up to 20 foreign-currency | Invoice 360: legacy badge, source, legacy number, original values, booked / paid / remitted / open per component, shares; Collections item for open premium |
| F02 | 20 largest balances; 20 random; every item with a disposition in progress (up to 30) | Unapplied Payments workbench: legacy AR, amount, balance, tab, references, disposition |
| F03, F04, F06 | 20 random per object | Promise and assignment on the Collections item; hold or special request in Remittance; PDC in the warehouse list |
| G01 | Every branch and currency total; 20 largest account lines; retained earnings per branch | Trial balance report in BIBS against the signed preliminary trial balance; P&L accounts at 0.00 |
| G03 | Every journal of the true-up; every detail line on a legacy control account | Journal inquiry (MIG-TU-n) against the legacy journal and the register; Invoice 360 or the Unapplied workbench for the item adjusted |
| H01, H02 | 10 records per record type; 10 documents | Legacy Inquiry search by the legacy key; labelled columns; document opens and matches its checksum |

## Acceptance rule

- **Financial objects (F01, F02, G01, G03) and identity fields of clients:** no error is accepted. One error stops the sign-off: the cause is found, fixed at source, in the map or in the load settings, the batch is rerun, and a new sample is drawn.
- **Other fields:** an error that is isolated and cosmetic (for example capitalisation of an address) is listed on the form with its fix (legacy correction before the next extract, or correction in BIBS after go-live by the owner). Two or more errors of the same kind in a sample are treated as systematic: the sample is doubled and the cause is fixed before sign-off.
- The result is recorded on the object sign-off form (Sign-off forms, Object reconciliation and acceptance) with the sample seed, the records checked and the errors found.

# Breaks, waivers and exclusions

<!-- table: widths=4.2,6.6,5.8 caption="How differences are closed" size=8.5 -->
| Situation | Handling | Approved by |
|---|---|---|
| Row rejected by a data-quality rule | Fixed at source and re-extracted, fixed by a new map version, or waived (master data only) | Data steward; waiver by the data owner |
| Financial row that cannot be fixed before go-live | Excluded from the batch with a manual-entry plan (who keys it in BIBS after go-live, when, and the amount) | Data owner and Comptrollership |
| Amount or count difference between control file and staging | The extract is rejected at intake; BDOI IT re-sends | - |
| Difference between staging and BIBS (L2, L4) | Load or mapping issue: fixed and rerun; never explained away | iorta Migration Lead |
| Migration Clearing not 0.00 | Traced to the invoice, UPP or TB line; fixed by rerun or by a legacy correction and a new extract; a residual rounding difference is explained with its amount | Reconciliation approver (Comptrollership) |
| Late legacy transaction after the freeze | Recorded as a break; the transaction is re-keyed in BIBS after go-live and the legacy system owner explains the access. A FY2027 GL adjustment by a named Comptrollership user is not a late transaction: it goes through the next true-up | Data owner; BDOI IT |
| True-up check not met | The true-up is not signed; the cause is corrected by a new extract before sign-off or by the next true-up; a posted journal is never edited | Head, Comptrollership |

# Evidence and retention

For each object and cycle the evidence pack holds: the extract numbers with file names, SHA-256 and control totals; the map versions used; the reports Data-Quality Issues with the waivers, Rejected Rows, Reconciliation Summary and Reconciliation Detail; Migration Clearing and Legacy Control Accounts for financial objects; the sample list with the check results; and the signed forms of this part (Sign-off forms). The pack is exported from the Migration Console and filed in the project records. Staging rows and files are purged within 5 days of sign-off, but counts, hashes, totals, reports and sign-offs are kept (proposed 10 years, DMQ29).

# Sign-off forms

The forms below are signed in the Migration Console (gates G5 and G6) and printed for the project file. One form per object and cycle.

<!-- pagebreak -->

## Object reconciliation and acceptance (G5 and G6)

```keyvalues
Object and name: "..........  ...................................................."
Cycle and environment: "Trial migration 1 / Trial migration 2 / Trial migration 3 / Trial migration 4 / Dress rehearsal / Production     Environment: ................"
Batch numbers (MGB-): "........................................................................"
Extract numbers (MGX-) and as-of: "........................................................................"
Code map versions used: "........................................................................"
Sample seed and size: "Seed ............     Records checked ............"
```

<!-- table: widths=1.2,4.8,2.4,2.4,2.2,3.6 caption="Reconciliation results (from the report Reconciliation Summary)" size=8.5 -->
| Level | Measure | Source (control) | BIBS | Difference | Status (Matched / Explained) |
|---|---|---|---|---|---|
| L1 | Rows received / staged / loaded / skipped / rejected / excluded | | | | |
| L2 | Amount totals per currency (list) | | | | |
| L3 | Hash total of the key | | | | |
| L4 | Field differences (count) | - | | | |
| L5 | Migration Clearing; control account vs sub-ledger | - | | | |
| G6 | Business verification sample - errors found | - | | | |

<!-- table: widths=1.2,6.4,5,4 caption="Breaks explained, waivers and exclusions" size=8.5 -->
| # | Break, waiver or exclusion | Reason and explanation | Approved by / manual-entry plan |
|---|---|---|---|
| 1 | | | |
| 2 | | | |

```signoff
rows:
  - {name: "", role: "Reconciliation approver - G5 (Comptrollership for financial objects)", organisation: BDOI}
  - {name: "", role: "Data owner - G6 object accepted", organisation: BDOI}
  - {name: "", role: "Data Migration Lead - G6", organisation: BDOI}
  - {name: "", role: "Migration Lead (reconciliation run)", organisation: iorta TechNXT}
```

<!-- pagebreak -->

## Cutover reconciliation summary (go / no-go GNG-3)

```keyvalues
Cutover: "Production     Go-live date (T): ................     Freeze: T-3 22:00"
Rollback point (snapshot ID and time): "........................................................................"
```

<!-- table: widths=3,4.2,3.2,2.6,3.6 caption="Day-1 objects at go / no-go" size=8.5 -->
| Object | Batch | Loaded / staged | G5 signed | G6 signed |
|---|---|---|---|---|
| R01-R08, R11 | | | | |
| C01-C03 | | | | |
| P01, P03 | | | | |
| F01 | | | | |
| F02 | | | | |
| F03, F04, F06 | | | | |
| G01 (provisional) | | | | |

<!-- table: widths=5.4,3.4,3.4,4.4 caption="Migration Clearing and legacy control accounts per branch and currency" size=8.5 -->
| Branch / currency | Migration Clearing | Control accounts vs sub-ledgers | Status |
|---|---|---|---|
| | | | |
| | | | |
| | | | |

<!-- table: widths=3,3.4,3.4,3.4,3.4 caption="Go-live renewal extraction check (completed on the morning of T)" size=8.5 -->
| Expiry month | P01 headers expiring | Renewed in legacy | Candidates created | RA already sent (P03) |
|---|---|---|---|---|
| January 2028 (urgent) | | | | |
| February 2028 | | | | |
| March 2028 | | | | |
| April 2028 | | | | |
| May 2028 | | | | |

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Program Manager (for the go / no-go board)", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

<!-- pagebreak -->

## True-up reconciliation and sign-off (FY2027)

```keyvalues
True-up: "1 / 2 / 3 / F (final)     Legacy trial balance version: TU1 / TU2 / TU3 / Final (audited)"
Legacy extracts (MGX-) and as-of: "........................................................................"
Journals posted (MIG-TU-): "........................................................................"
Prepared by (Comptrollership GL lead) and date: "........................................................................"
Opening period reopened: "No / Yes - reopened at ........ closed at ........ reason ................................"
```

<!-- table: widths=3.2,6.2,3.4,3.8 caption="True-up checks (from the report Opening-Balance Adjustment Reconciliation)" size=8.5 -->
| Check | Measure | Result | Status (Met / Break) |
|---|---|---|---|
| Cut-off | Legacy journals since the freeze / in the register / in the true-ups | | |
| Movement | Accounts with a difference between the true-up and the legacy TB change | | |
| Balance | Accounts with a difference between the BIBS opening and the legacy TB | | |
| Clearing | Migration Clearing per branch and currency | | |
| Sub-ledgers | Legacy control accounts vs legacy sub-ledgers | | |
| Opening period | Other journals posted in the reopen window | | |

```signoff
rows:
  - {name: "", role: "Head, Comptrollership - true-up approved and reconciliation signed", organisation: BDOI}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Program Manager - closure of the true-ups (final true-up only)", organisation: BDO Unibank ESG}
  - {name: "", role: "Migration Lead (reconciliation run)", organisation: iorta TechNXT}
```

The final true-up's reconciliation against the audited FY2027 trial balance is shared with the external auditor.

<!-- pagebreak -->

## Archive reconciliation (before decommissioning)

```keyvalues
Legacy system: "EBIX / QPS / ISYS / CMS / File shares"
Record types archived: "........................................................................"
Archive batches (MGB-): "........................................................................"
```

<!-- table: widths=3.4,2.6,2.6,2.6,2.6,2.8 caption="Archive counts and totals per record type" size=8.5 -->
| Record type | Legacy count | Archived count | Amount total legacy | Amount total archive | Samples checked in Legacy Inquiry |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

```signoff
rows:
  - {name: "", role: "Legacy system owner", organisation: BDOI}
  - {name: "", role: "Audit / Compliance", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
```

# Part D - Cut-over runbook, run-off and decommissioning {-}

# Purpose and use

This runbook is the plan of the production cutover of BIBS: from 30 days before go-live (T) to the end of hypercare. It lists every task with its owner, start, duration, predecessors and the evidence that proves it is done; the go / no-go checkpoints with their criteria; the freeze windows; the rollback procedure; the communication plan; the hypercare roster and exit criteria; and the decommissioning checklists of the legacy systems.

- **T** is the go-live date. The BDOI timeline sets it in January 2028 (DMQ25). The recommendation, awaiting Comptrollership confirmation, is **Monday 3 January 2028** at the year-end boundary (DMQ39 option A, register DCR-242): legacy processes to 31 December 2027 and closes FY2027, and BIBS opens with the open items at 31 December and a provisional GL opening. Days are calendar days; times are Philippine time (PHT). The calendar (Timeline, Calendar) maps the relative days to dates; tasks that would fall on the year-end holidays are placed on the nearest working day.
- The same tasks are on the sheet Cut-over tasks of the Migration Workbook (file 03), where names are recorded and the business reviews them, and in the Migration Console as the plan of kind Production cut-over (FR-DM-120), where the actual times and status are recorded. The console is the record during the cut-over; the workbook is the planning and review copy.
- The plan is rehearsed in four trial migrations (April, July, August and October 2027) and a dress rehearsal (November 2027) (Part A, Migration cycles). This issue gives planned durations; the update issued after the dress rehearsal gives the measured durations and the named people.
- The early renewal release of the concept paper of 6 September 2026 is superseded by the single January 2028 go-live (register DCR-240). The renewals of the January-May 2028 expiries are processed in BIBS after go-live (DMQ37): no renewal is carried from legacy. The Renewal processing team is made ready for the January expiries (CT-013), compiles the renewal advices already sent from its Excel trackers (CT-033) and reviews the rejected rows with a maker and a checker (CT-055); BIBS extracts every expiry to 31 May 2028 at T 04:00 (CT-062), and the team works the day-1 priority queue, January expiries first (CT-066).
- The FY2027 closing and audit adjustments that Comptrollership posts in the legacy GL after the freeze reach BIBS as true-ups: true-up 1 after the legacy year-end close (CT-080 to CT-084) and the final true-up after the audited financial statements, followed by the legacy GL lock and the closure (phase H, CT-087 to CT-095).

# Cutover organisation

## Command centre

- **Location and channels.** A command centre room at BDOI head office, a standing bridge line and a chat channel for the cutover team, open from T-3 17:00 to T+5.
- **Status calls.** T-3 to T: every 4 hours and at each checkpoint. T+1 to T+5: 08:00 and 17:00. Afterwards daily at 17:00 until hypercare exit.
- **Decisions.** Task owners report completion with the evidence in the console. The Data Migration Lead decides within the plan; anything that changes the plan, the scope or a checkpoint goes to the go / no-go board.
- **Escalation.** Task owner to the Data Migration Lead (15 minutes), to the iorta Project Manager and the Program Manager (30 minutes), to the go / no-go board (1 hour, or immediately for a stop condition).

**Stop conditions** (the board meets at once): a rejected final extract that cannot be re-sent within 2 hours; a load failure that leaves a financial object incomplete; Migration Clearing not 0.00 after the reconciliation window; a legacy posting after the freeze; loss of the production environment or of the snapshot.

## Roles

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-roles
```

# Timeline

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-phases
```

## Calendar

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-calendar
```

![The cutover weekend (T = Monday 3 January 2028, recommended; times PHT)](figures/dm_cutover_weekend.dot){width=16}

## Freeze windows

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-freeze
```

# Go / no-go checkpoints

Each checkpoint is recorded in the Migration Console with the value of every criterion at the time of the decision (FR-DM-121). A criterion that is not met blocks GO unless the board records a waiver with its reason (only for criteria marked as waivable in the console; the financial and clearing criteria of GNG-3 are never waived).

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-checkpoints
```

<!-- landscape -->

# Task list

Columns: **When** is the day relative to T and the planned start time; **Owner** is the accountable role (codes in Cutover organisation, Roles); **Hours** is the planned elapsed time; **After** lists the tasks that must be complete first; **Verification** is the evidence recorded in the console.

## Phase A - Readiness

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: A
```

## Phase B - Pre-load and daily deltas

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: B
```

## Phase C - Legacy freeze and final extracts

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: C
```

## Phase D - Production load

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: D
```

## Phase E - Reconciliation, verification and go / no-go

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: E
```

## Phase F - Go-live

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: F
```

## Phase G - Hypercare

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: G
```

## Phase H - Final true-up and legacy GL close

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-tasks
phase: H
```

<!-- portrait -->

# Rollback

The rollback returns BDOI to legacy with no loss of data. It is available until the point of no return, the end-of-day review on T at 18:00 (DMQ32). The rollback point is the production backup (snapshot) taken at T-2 02:00 (task CT-040), before any open item, header or trial balance is loaded; reference data and clients pre-loaded before it stay loaded and are harmless because legacy stays the system of record until go-live.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-rollback
```

After the point of no return, there is no technical rollback. An issue is corrected forward in BIBS through the normal business functions (correction, reversal, adjustment), with the change visible in the Prod Recon legacy change report where it touches a legacy invoice. The true-ups come after the point of no return; a true-up that is wrong is corrected by the next true-up, never by editing a posted journal.

# Year-end cut-over and FY2027 true-ups

The cut-over sits on the year-end boundary (DMQ39, option A; recommended, awaiting Comptrollership confirmation at M6). It changes three things in the plan:

- **Before the freeze.** Comptrollership completes a December soft close by 20 December 2027 (CT-011), so the preliminary December trial balance is close to final, and names the users who may post FY2027 adjustments in the legacy GL after the freeze (CT-034).
- **At the freeze.** The legacy business modules become read-only, and the legacy GL stays open only for FY2027 closing and audit adjustments by those users, in FY2027 periods (CT-036). The preliminary December trial balance is signed as the provisional opening (CT-039) and loaded as balance-sheet opening journals dated 1 January 2028, with the FY2027 result in retained earnings (CT-048). FY2028 P&L starts at zero in BIBS.
- **After go-live.** Each FY2027 adjustment reaches BIBS as an opening-balance adjustment journal (type Opening, value date 1 January 2028) in a true-up, prepared by the Comptrollership GL lead and approved by the Head of Comptrollership: true-up 1 after the legacy year-end close (CT-080 to CT-084, before the January close) and the final true-up after the audited financial statements (CT-088 to CT-093). Each true-up is reconciled to the legacy trial balance with the cut-off checks of the Reconciliation Approach before it is signed. The legacy GL is then locked (CT-094) and the true-ups are closed (CT-095). The FY2027 BIR annual returns and the FY2027 audit use legacy (CT-092); FY2028 uses BIBS.

Options B (go-live after the first-quarter close in April 2028, with a year-to-date P&L migration) and C (two books in parallel for the first quarter of 2028) were considered and are not recommended (Part A, Cutover strategy). If BDOI wants more assurance in January, Comptrollership compares the key BIBS reports with the opening position instead of keeping two books.

# Communication plan

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-communication
```

# Hypercare

Hypercare runs from go-live to the first month-end close with the legacy control accounts (about T+30). The command centre stays open to T+5, then the daily 17:00 call continues until exit.

**Daily checks** (task CT-071): Migration Clearing 0.00 per branch and currency; legacy control accounts against their sub-ledgers (ACSL, legacy ledger context); automatch runs and the unapplied items they left; urgent January renewals without an RA sent or an insurer request; payment file results; exception queues (rejected receipts, remittance exclusions, failed postings); failed jobs and alerts; open issues by severity; user tickets by department.

## Roster

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-roster
```

## Exit criteria

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-exit
```

# Decommissioning checklists

A legacy system is decommissioned only when every item of its checklist is met and signed (FR-DM-123; DMQ27). The legacy context in BIBS closes separately, when the legacy positions have run off. The words before the dash are the default criterion names that the Migration Console and its messages use.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: cut-decommissioning
```

# Decisions for BDOI and traceability {-}

The last chapters list the rules and clarifications BDOI confirms, the open decisions with their dates, the assumptions of the set and the traceability of the BRD requirements.

# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed rule fills a gap of the BRD, differs from its text, or rests on a recommendation that BDOI confirms. Until BDOI decides, the proposed rule is the working assumption of the migration. BDOI records its decision on the sheet Proposed rules of the Migration Workbook (Agree, Agree with change, Disagree, Need more information); a decision that changes a screen, layout, rule or message is applied in the next version of this set.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-proposals
```

## Open decisions and the date each is needed by

Decisions marked M1 change the migration and are needed by **16 October 2026**; M2 decisions are needed by **30 October 2026**, the end of requirements and mapping on the BDOI timeline. The others are tied to the migration calendar. The register column refers to the BRD discrepancy and clarification register (R6). BDOI answered DMQ36, DMQ37 and DMQ38 on 26 September 2026 and answered DMQ39 with a recommendation that Comptrollership confirms at M6; DMQ26 is answered by DMQ37.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: dm-decisions
```

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-DM-01 | The draft BRD v0.01 is the baseline until a signed version replaces it | R1 |
| A-DM-02 | BRID 1.1 (p.7) is two requirements, numbered 1.1a and 1.1b here | R1 p.7 |
| A-DM-03 | Legacy open items are processed only in BIBS from the freeze; legacy becomes read-only | p.5, DMQ22 |
| A-DM-04 | Legacy sub-ledgers are separate GL control accounts with a Migration Clearing account | DMQ18 |
| A-DM-05 | Endorsements of legacy invoices need the policy header to be migrated | DMQ09, DMQ22 |
| A-DM-06 | DP PR means the premium receivable of an invoice paid directly to the insurer | DMQ19 |
| A-DM-07 | Go-live is on Monday 3 January 2028, at the year-end boundary (option A, for Comptrollership to confirm) | DMQ25, DMQ39 |
| A-DM-08 | No BIBS AR is issued for a migrated unapplied payment at load | DMQ14 |
| A-DM-09 | There is one production cut-over; BIBS is not used in production before go-live | R10; DCR-240 |
| A-DM-10 | The renewals of the January-May 2028 expiries are processed in BIBS after go-live from a go-live extraction; no renewal is carried (BDOI answer) | DMQ37 |
| A-DM-11 | Legacy packages are remapped at Renewal sanitation; the migration loads the package code map only (BDOI answer) | DMQ36 |
| A-DM-12 | BIBS opens with a provisional balance-sheet opening; the FY2027 P&L is not opened, and the FY2027 BIR returns and the FY2027 audit use legacy | DMQ39 |
| A-DM-13 | After the freeze the legacy GL accepts only FY2027 closing and audit adjustments by named Comptrollership users, until the final adjustment | DMQ39 |

# Traceability

Every BRD-13 requirement is met by at least one FR of Part B. The test cases of each FR are in the test plan (files 04 and 05 of the set).

<!-- table: widths=3,5.4,8.2 caption="BRD ID to FR and screen" size=8.5 -->
| BRD ID | FR | Screen |
|---|---|---|
| BRID 1.1a (p.7) | FR-DM-001, 002, 003 | Data Objects; Sign-off |
| BRID 1.1b (p.7) | FR-DM-003, 010, 013, 014, 015, 020, 021, 022, 023, 024 | Extracts; Batches; Batch; Reconciliation; Opening-Balance Adjustments |
| BRID 2.1 (p.7) | FR-DM-031, 032, 033 | Client Matching; Clients |
| BRID 3.1 (p.7) | FR-DM-011, 012, 030, 034 | Code Maps; Layouts and Rules |
| BRID 4.1 (p.8) | FR-DM-033, 040, 041, 124 | Accounts; Account |
| BRID 5.1 (p.8) | FR-DM-021, 022, 023, 051 | Unapplied Payments; Reconciliation |
| BRID 5.2 (p.8) | FR-DM-050, 052 | Unapplied Payments; Payments |
| BRID 5.3 (p.8) | FR-DM-053 | Unapplied Payments |
| BRID 5.4 (p.8-9) | FR-DM-054 | Unapplied Payments; Disbursement |
| BRID 5.5 (p.9) | FR-DM-055 | Unapplied to Income |
| BRID 6.1 (p.9) | FR-DM-050, 060 | Receive payment (OTC); Invoice 360 |
| BRID 6.2 (p.9) | FR-DM-061 | Payment uploads |
| BRID 6.3 (p.9) | FR-DM-052 | Unapplied Payments |
| BRID 6.4 (p.10) | FR-DM-053 | Unapplied Payments |
| BRID 7.1 (p.10) | FR-DM-070 | DP PR Legacy Reversal |
| BRID 7.2 (p.10) | FR-DM-071 | Legacy PR 2307 Reversal |
| BRID 8.1 (p.10) | FR-DM-050, 080 | Remittance |
| BRID 9.1-9.3 (p.11) | FR-DM-090, 091, 092 | Endorsement request |
| BRID 10.1 (p.11) | FR-DM-050, 100 | Changes to Legacy Invoices report |
| BRID 11.1 (p.11-12) | FR-DM-110, 111 | Legacy Inquiry; Access Log |
| BRID 12.1 (p.12) | FR-DM-022, 023, 024, 034, 120, 121, 122, 123, 124, 125 | Cutover; Run-off and Decommissioning; Opening-Balance Adjustments; Renewal candidates |

The concept paper on an early renewal release (R10) is superseded by the single January 2028 go-live. Its early migration of the client master and the renewal reference data (section VI) is met by the trial-migration load order of FR-DM-120; its RMEL ingestion (Annex C) is replaced by the go-live extraction and the RA-sent file (FR-DM-124, FR-DM-125); and its package remapping question (p.2, Annex B and C) is answered by the remapping at Renewal sanitation (FR-DM-034).

# Sign-off {-}

By signing, BDOI approves this handbook and the Migration Workbook of the same version: the decisions per data object, the load templates, the screens, rules and messages, the reconciliation and gates, and the cut-over plan as the plan of the production cut-over. The confirmed go-live date, the Comptrollership confirmation of the year-end option (M6), the named people and the measured durations are added in the update issued after the dress rehearsal. Open decisions stay open with their dates; their answers are applied as configuration or through a change request. Signing freezes the content of the set; a later change goes through the Change Management Register.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Heads, Retail and Corporate Marketing", organisation: BDOI}
  - {name: "", role: "Head, Renewal processing team", organisation: BDOI}
  - {name: "", role: "Compliance; Unit Head, Analytics and Risk Management", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
