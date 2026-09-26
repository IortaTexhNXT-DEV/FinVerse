---
# Data Migration Strategy and Approach (client deliverable 29), BRD-13.
# Build: python docs/deliverables/src/migration/build_migration_pack.py (expands the <!-- dm:... --> tables
# from dm_layouts.yaml and dm_cutover.yaml, then builds with tools/deliverables/bdoi_docx.py).
title: Data Migration Strategy and Approach
subtitle: BRD-13 Data Migration - scope, approach, cutover and proof
doc_type: Strategy and Approach
doc_code: Migration
brd: BRD-13
name: Data Migration Strategy and Approach
doc_id: BIBS-DMS-BRD-13
version: "1.2"
date: 26 September 2026
status: Issued for BDOI review
header_title: Data Migration Strategy and Approach
output: Migration/BIBS_Migration_BRD-13_Data_Migration_Strategy_and_Approach_v1.2.docx
control:
  - version: "0.9"
    date: 25 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: Internal draft from the BRD-13 baseline, the build design and FRS BRD-13 v1.0
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue for BDOI review, with the Data Requirements Workbook, the extract templates, the Cutover Runbook and the Reconciliation Approach of the same version
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "Re-based on the BDOI programme timeline (go-live January 2028; mocks in the SIT and UAT migration windows; BDOI input dates inside requirements and mapping, Sep-Oct 2026); early-renewal concept paper recorded as superseded by the single January 2028 go-live; mock-load order; carried RMEL cohorts of January-May 2028 (layout P03); package remapping options and recommendation; decisions DMQ36-DMQ39"
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "BDOI answers of 26-Sep-2026 (DMQ36-DMQ39): packages remapped at Renewal sanitation; January-May 2028 expiries processed in BIBS after go-live, RA-sent file with maker-checker review; year-end option A with a provisional GL opening and FY2027 true-ups (object G03)"
distribution:
  - {name: "Program Manager, Business Project Services", role: "Approver", organisation: BDO Unibank ESG, purpose: "Review and sign-off; owner of the cutover"}
  - {name: "Head, Comptrollership; Product Owners FRBS / ACSL and Disbursement", role: Approver, organisation: BDOI, purpose: "Open items, GL opening, legacy sub-ledgers, reconciliation"}
  - {name: "Head, Operations; Operations - Financial Transactions and Processing", role: Approver, organisation: BDOI, purpose: "Legacy invoices, UPP, remittance, collections"}
  - {name: "Product Owner, Marketing Business System; Heads of Retail and Corporate Marketing", role: Approver, organisation: BDOI, purpose: "Client master, reference data, renewal transition"}
  - {name: "Compliance; Unit Head, Analytics and Risk Management", role: Reviewer, organisation: BDOI, purpose: "Data protection, archive, access logging"}
  - {name: "BDOI IT (legacy EBIX, QPS, ISYS, CMS)", role: Reviewer, organisation: BDOI, purpose: "Extracts, transfer, legacy read-only and decommissioning"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, rehearsals, cutover, hypercare"}
---

# Introduction

## Purpose

This document sets out how BDO Insurance and Reinsurance Brokers, Inc. (BDOI) moves from its legacy systems to BIBS (BDOI Broker System, on iNXT BrokerVerse). It answers four questions for the BDOI departments and BDOI IT:

1. **What data moves**, object by object, and what stays behind (Migrate, Carry forward, Archive, Excluded).
2. **What BDOI provides**, in which form and by when (the Data Requirements Workbook and the extract templates).
3. **How the data gets into BIBS**: transfer, staging, cleansing, code mapping, validation, load and reconciliation, and who does each step.
4. **How the cutover is run and proven**: rehearsals, freeze, go / no-go, rollback, hypercare and decommissioning of the legacy systems.

It also explains, for business readers, how legacy invoices and unapplied payments that are still open at cutover are processed in BIBS after go-live.

## Scope and basis

The basis is the Data Migration BRD (R1), a **draft v0.01 of 14-Apr-2026 that is not signed** (p.2, p.15-16; register DCR-189). The BRD asks for a **selective, continuity-focused migration** (p.3): migrate clean master and reference data for Day 1, carry forward only the open operational financial positions, keep history in read-only legacy or an archive, and let renewals recreate clean records in BIBS (p.5). This document follows that approach and fills the gaps the BRD leaves with proposals, each tied to an open decision (DMQ##) that BDOI confirms.

Data Migration is **designed and not yet built** in BIBS. The mechanisms named here (Migration Console, loaders, reconciliation, legacy invoice processing) are those of the build design (R3) and the FRS (R4).

**Programme calendar and the early-renewal concept paper.** This version follows the BDOI drop plan and timeline (R9): data migration is part of Drop 0, requirements and mapping run in September-October 2026, the build from November 2026 to March 2027, SIT migration from April to July 2027, UAT migration from August to October 2027, full migration and cut-over from November 2027 to January 2028, and go-live is in January 2028. The concept paper on an early renewal release (R10), signed on 6 September 2026, proposed to take renewal processing live by 15 August 2027 with the client master and reference data migrated ahead of it. BDOI decided on 26 September 2026 that everything goes live together in January 2028; the concept paper is therefore superseded and there is **one production cut-over** (register DCR-240). Of its points, the mock-load order still applies: reference data and clients are loaded first in every mock (section 10.1).

**BDOI answers of 26 September 2026.** This version applies BDOI's answers to four open decisions: legacy packages are remapped at Renewal sanitation and the migration only loads the PACKAGE code map (DMQ36, section 6.6.1); the renewals of the January-May 2028 expiries are processed in BIBS after go-live from a go-live extraction of the migrated policy headers, with the renewal advices already sent loaded so they are not sent again (DMQ37, section 8.2); the RMEL and dispositions are kept in Excel, and the Renewal processing team reviews rejected rows with a maker and a checker (DMQ38, section 8.2); and, as a recommendation that Comptrollership still confirms, the go-live sits on the year-end boundary with a provisional GL opening and controlled true-ups of the FY2027 closing and audit adjustments (DMQ39, section 11.1).

## The Data Migration document set

<!-- table: widths=5.2,3.2,8.2 caption="Documents of this set (version 1.2)" -->
| Document | Format | Use |
|---|---|---|
| Data Migration Strategy and Approach (this document) | Word | Decisions, approach, responsibilities, timeline; for approval |
| BDOI Data Requirements Workbook | Excel | One sheet per extract layout (fields, formats, rules, BIBS target), object register, code map templates, control totals, data-quality rules, open decisions; for BDOI IT and data stewards |
| Extract templates (`templates/`) | CSV, XLSX + README | One header-only CSV per layout, the Excel template of the RA-sent file (P03) for the Renewal processing team, and the control-file template (deliverable 20) |
| Reconciliation Approach and Sign-off | Word | Reconciliation per object, sampling, evidence and the sign-off forms |
| Cutover Runbook | Word | Cutover organisation, T-30 to hypercare exit, checkpoints, rollback, communication, decommissioning |
| Cutover Task Plan | Excel | The runbook tasks with owner, duration, predecessors and verification, for execution |
| Migration Test Plan BRD-13 | Excel + Word | Test conditions and cases of every Data Migration FR (deliverable 3) |

## References

<!-- table: widths=1.2,8.4,3.2,5.4 caption="Reference documents" size=8.5 -->
| Ref. | Document | Version | Location |
|---|---|---|---|
| R1 | BDO Insure Core Modernization - Data Migration BRD | draft v0.01, 14-Apr-2026 | `docs/source-documents/BRD - Data Migration - draft V0.01.pdf` |
| R2 | Data Migration (BRD-13) requirements baseline and fit/gap | current | `docs/requirements/BDOI_DM_BRD_SPEC.md` |
| R3 | Data Migration build design | current | `docs/architecture/DATA_MIGRATION_DESIGN.md` |
| R4 | Functional Requirements Specification BRD-13 Data Migration | 1.2 | `BIBS_FRS_BRD-13_Data_Migration_v1.2.docx` |
| R5 | BRD BDOI Core Replacement (umbrella BRD) and its analysis | v01 | `docs/source-documents/00 - BRD BDOI Core Replacement v01.pdf`; `BDOI_CORE_BRD_SPEC.md` |
| R6 | BRD discrepancy and clarification register | 1.2 | `BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx` |
| R7 | Deliverables plan, UAT readiness programme and hosting appendix | current | `docs/deliverables/README.md` |
| R8 | Operations, Collections, Accounting / ACSL and Renewal designs | current | `docs/architecture/` |
| R9 | BDOI drop plan and programme timeline | received 26-Sep-2026 | `docs/source-documents/BDOI_DROP_PLAN.md`; `BDOI Programme Timeline.webp` |
| R10 | Concept Paper - Advance Implementation of Renewal Processing | V1.0, signed 06-Sep-2026; superseded 26-Sep-2026 | `docs/source-documents/Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` |

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
7. **Rehearse before production.** Four mocks and a dress rehearsal run the full cutover with masked data and measured timings, in the SIT and UAT migration windows of the BDOI timeline. Production follows the rehearsed plan.
8. **Protect personal data.** Masked data outside production, staging purged within 5 days of sign-off, access only for migration roles and only from the Philippines (hosting appendix, R7).

## What this migration does not do

- It does not convert closed transactions, GL history or expired policies into BIBS business tables (BRD p.3).
- It does not re-book legacy invoices. A legacy invoice keeps its legacy number and is processed as a legacy invoice until it is settled.
- It does not create users from legacy. Users are created through User Access requests (BRD-11); legacy user IDs are only mapped so that AO, handler and collector fields resolve.
- It does not run legacy and BIBS in parallel for the same items (section 11.3).
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
| Excluded | Day-1 need = No and compliance need = No, or the data is rebuilt in BIBS | Users and roles, screening results |

The Data Migration Lead records the four criteria and the proposed class per object in the Migration Console; the business owner of the object approves it (gate G1, FR-DM-002). A later change of class needs a new approval, and an object whose data is loaded cannot be moved to Archive or Excluded until the load is rolled back.

## Data object register

The register below is the proposal for gate G1 (R3 section 2; DMQ01, DCR-195). The workbook sheet "Object Register" holds the same rows with the columns BDOI fills in (volumes, named owners and stewards, status).

<!-- dm:register -->

## Depth, delta and due dates

<!-- dm:register-detail -->

The milestones in the Due column are:

<!-- dm:milestones -->

## Business owners and data stewards

Each object has a business owner who signs its decision (G1), its code maps (G2) and its acceptance (G6), and a data steward who prepares the code maps, resolves the data-quality issues and fixes records at source. The table is the proposal from the BRD approvers (p.15-16); BDOI names the people (DMQ01).

<!-- dm:owners -->

## Client migration "2020 to present" and daily client batches

The umbrella BRD sizes a "one-time migration from Broker and source", "2020 to present", plus a daily "midday batch (new transactions)", an "EOD batch (new clients)" and a "client modification report" (R5 p.43; register DCR-173). The Data Migration BRD asks for a deduplicated client master (BRID 2.1). This document reconciles the two as follows, for BDOI to confirm:

- **One-time load.** Clients created from 1-Jan-2020 to the freeze, plus older clients that have an in-force policy, an open item or activity since 2020 (DMQ05), are extracted in layouts C01 and C02, matched and deduplicated, and loaded two weeks before go-live.
- **Daily batches until the freeze.** From the pre-load until the freeze, the legacy EOD produces a daily delta of new clients and of changed clients (the modification report) in the same layouts; BIBS loads it overnight (job `MIG_CLIENT_DELTA`). New transactions of the day ("midday batch") are not migrated as transactions: open items are loaded once, after the freeze.
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

<!-- dm:source-matrix -->

Where two systems hold the same object (clients in QPS, EBIX and the CMS; products in QPS and EBIX), each system sends its own extract. BIBS matches the records across systems and keeps one BIBS record with a cross-reference to every legacy key. The survivorship rule decides which value wins per field (section 6.8).

# Target in BIBS

## Target per object and load order

Every object lands in a BIBS module through that module's service. The load order follows the dependencies: reference data first, then clients, then policy headers, then open items, then the GL, so every record finds the records it refers to.

<!-- dm:targets -->

## How migrated records are marked

- **Origin.** Migrated clients, accounts and UPP carry origin MIGRATED; legacy invoices carry origin LEGACY and ledger context LEGACY. Screens show a LEGACY badge, the source system and the legacy number.
- **Key cross-reference.** Every loaded record has a cross-reference from its legacy key (source system, object, legacy key) to the BIBS record. Users search clients and policies by their legacy numbers (FR-DM-033); reruns skip what is already loaded.
- **No side effects.** Loads send no notification to clients or insurers, do not start onboarding workflows, and do not trigger sanction screening per client (one full screening run follows the client load, DMQ06, DCR-208).
- **Frozen original values.** The values of each legacy invoice at load are frozen. The Prod Recon legacy change report compares every later change with them (BRID 10.1).

# Migration approach

## End-to-end process

Figure 1 shows the steps every object goes through, and who does each step.

![Migration pipeline per data object: BDOI steps (left) and BIBS Migration Console steps (right)](figures/dm_pipeline.dot){width=15}

<!-- table: widths=0.7,3.6,5.6,3.6,3.1 caption="Steps, responsibilities and evidence" size=8.5 -->
| # | Step | What happens | Who | Evidence |
|---|---|---|---|---|
| 1 | Extract | BDOI IT extracts each object in the agreed layout with its control file | BDOI IT | Data file and control file |
| 2 | Secure transfer | Files are uploaded in the Migration Console or dropped on the agreed SFTP folder; never e-mailed | BDOI IT | Upload log with SHA-256 |
| 3 | Intake checks | BIBS checks checksum, header, row count, amount totals and hash total against the control file; a failing file is rejected as a whole | iorta Migration Operator | Extract status CHECKED or REJECTED |
| 4 | Staging and profiling | Rows are staged (masked outside production) and profiled; the profiling report goes to the data stewards | iorta Migration Lead | Profiling report per object |
| 5 | Cleansing | Issues at source are fixed in legacy by the stewards (5b); formats, defaults and mapped values are handled in staging (5a) | BDOI data stewards; iorta | Issue log (MIG-DQ-ISSUES) |
| 6 | Mapping | Legacy codes are mapped to BIBS codes in versioned code maps; the business owner approves each version (G2) | Data steward prepares; owner approves | MIG-MAP-VERSIONS, MIG-UNMAPPED-CODES |
| 7 | Validation and matching | Data-quality rules run on every row; clients are matched and deduplicated (G3) | BIBS; Data steward | MIG-DQ-ISSUES, MIG-CLIENT-MATCH |
| 8 | Load | Approved batches load through the BIBS services (G4) | Data Migration Lead approves; iorta runs | Batch log, MIG-REJECTS |
| 9 | Reconciliation | L1 counts, L2 amounts, L3 hash totals, L4 fields, L5 GL; breaks explained (G5) | Reconciliation approver | MIG-RECON-SUMMARY, MIG-GL-CLEARING |
| 10 | Acceptance | Business owner checks samples on the BIBS screens and accepts the object (G6) | Business owner; Data Migration Lead | Sign-off form |
| 11 | Purge | Staging payloads and files are deleted within 5 days of sign-off; counts, hashes and totals are kept | BIBS job | Purge log |

## Extract templates

BDOI extracts each object in the layout of the Data Requirements Workbook. A layout gives, per field: name, description, type, length, mandatory flag, allowed values or code list, format, example, BIBS target field, validation rule and a source system hint. The CSV templates carry the same field names as their header row.

<!-- dm:layouts -->

Rules for every file (workbook sheet "File Contract"):

- File name `<LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv`, one file per layout, source system and extract: the layout code, not the object code, starts the name. Three objects have sub-layouts sent as separate files with the same as-of date: R04 and R04B (insurers and their branches), P01 and P01S (policy headers and their insurer shares), F01, F01S and F01C (invoice headers, insurer shares and components).
- CSV in UTF-8 without BOM, comma separator, RFC 4180 quoting, one header row; or XLSX with the header in row 1 of the first sheet.
- Dates `yyyy-MM-dd`; amounts with a dot decimal, 2 decimals, no thousands separator; codes exactly as stored in legacy.
- A control file `<data file>.ctl.csv` with the row count, the hash total of the key, the amount totals per column and currency, and the SHA-256 of the data file.

What BDOI provides per object:

<!-- dm:bdoi-inputs -->

## Secure transfer and staging

- Files are received through the Migration Console upload, or through an SFTP drop that BDOI IT and iorta TechNXT set up (a seam of the intake; DMQ28). E-mail is never used.
- Files land in an encrypted intake bucket in AWS ap-southeast-1 with a 5-day lifecycle.
- The intake checks run before any row is staged. A file that fails is rejected with the reason and the difference, and BDOI IT re-sends it.
- Staged rows keep the raw values as received and the mapped values. Outside production, names, addresses, TIN, ID, account and phone numbers, e-mail addresses and birth dates are masked at intake with a keyed, repeatable masking, so dedupe still works on masked data.

## Profiling

After the first full extract (M3, 29 January 2027) and after each mock, iorta TechNXT profiles every staged object and sends the report to the data stewards. The report gives, per field: fill rate, distinct values, top values, values out of format, codes without a map entry, duplicates of the key, and, per object, the rows that fail each data-quality rule. For clients it gives the candidate duplicate pairs by score band; for open items it gives the totals by component, currency, branch and age band. The stewards use it to plan the cleansing and the code maps.

## Cleansing: who fixes what

Cleansing is shared. BDOI owns the data and fixes it where it lives; iorta TechNXT fixes what is a matter of format or mapping.

<!-- table: widths=4.4,6.2,2.6,3.4 caption="Cleansing responsibilities" size=8.5 -->
| Issue | Example | Fixed by | Where |
|---|---|---|---|
| Wrong or missing business data | Client without birth date, invoice without insurer, TIN of another client | BDOI data steward | In legacy, before the next extract |
| Duplicate clients | Same person in QPS and EBIX with different numbers | BIBS matching; data steward decides doubtful pairs | Client Matching queue |
| Legacy codes without a BIBS value | Discontinued segment code, insurer branch not in BIBS | Data steward proposes; business owner approves | Code map version (MAP, DEFAULT, CREATE or REJECT) |
| Format differences | Date as dd/MM/yyyy, TIN with dashes, mobile without prefix | iorta TechNXT (extract layout or staging rule) | Extract job or staging |
| Records out of scope | Client not active since 2019, invoice fully settled | Business owner decides | Exclusion recorded in the batch |
| Open balances that do not add up | Components do not equal gross premium; open not equal to booked less paid | BDOI data steward with Comptrollership | In legacy (correction entry) before the freeze |
| Data that cannot be fixed before go-live | UPP without payor reference | Business owner waives with a reason and a plan | Waiver in the batch (G3) |

Thresholds for loading an object (configurable): master data may be loaded with at most 0.5 percent of rows rejected or waived; financial objects (open invoices, UPP, trial balance) load only with 0 errors, or with each excluded row approved by the business owner with a manual-entry plan (R3 section 8).

## Mapping and code maps

Legacy codes are mapped to BIBS codes through code map sets, one per domain. Each set has versions: DRAFT, SUBMITTED, APPROVED, SUPERSEDED. The data steward prepares a version in the console or in the Excel template of the workbook and submits it; the business owner approves it (maker-checker). Each batch records the map versions it used, so every loaded value can be traced to the entry that produced it (BRID 3.1).

An entry maps a legacy code to a BIBS code (MAP), to the default of the set (DEFAULT), rejects the rows that carry it (REJECT), or creates a new BIBS value (CREATE). CREATE values are created through the owning master (for example the LOV master or the insurer master) and authorised there before any dependent object loads.

<!-- dm:map-sets -->

### Package remapping

Legacy packages (QPS package code and version) must map to the package names that TSU maintains in BIBS (BRD-3). BDOI decided on 26 September 2026 that the remapping happens **at sanitation in Renewal**, per renewal candidate, and not in the migration intake (DMQ36, register DCR-241). The concept paper had left "during upload processing or during sanitation" open (R10 p.2, Annex B, Annex C).

<!-- table: widths=3.2,13.4 caption="Package remapping (DMQ36 answered)" size=8.5 -->
| Where | What happens |
|---|---|
| Migration | Loads the PACKAGE code map as reference data (object R06): entries from a legacy package and version to a BIBS package version, with conditional entries where one legacy package splits (by risk code, insurer or sum-insured band). TSU prepares each version and the Product Owner of Marketing Business System approves it (G2). Policy headers (P01) and the RA-sent file (P03) keep the legacy package as given; no package is resolved, rejected or warned at intake |
| Renewal sanitation | The Renewal check PACKAGE_REMAP resolves the BIBS package of each candidate through the map. A package without an entry, with a REJECT entry or with no matching qualifier sends the candidate to the Exception bucket |
| Exception bucket | Worked by the Renewal processing team: the processor chooses the BIBS package (or the new-business path) and records the reason; each choice goes to TSU for the next map version |

**Testing.** The PACKAGE map is loaded in every mock. Profiling lists the legacy packages of the headers expiring up to 31 May 2028 that have no entry: this is the expected Exception-bucket volume at go-live, and TSU closes the gaps before the map freeze. Business verification samples cover every legacy package with more than one BIBS target.

## Validation

Every staged row is checked against the data-quality rules of its layout before a batch can be approved for load. An ERROR stops the row; a WARNING loads it and reports it. The catalogue (workbook sheet "DQ Rules") has 54 rules:

<!-- dm:dq-summary -->

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

Pairs scoring 90 or more merge automatically; 60 to 89 go to the data steward's review queue; below 60 are separate clients. The client batch cannot be approved while the queue has open pairs. Survivorship takes, per field, the first non-blank value by source priority (proposal: QPS for contact data, EBIX for billing data) or the most recently updated value. Every value that lost is kept for review (report MIG-CLIENT-MATCH).

## Load through the BIBS services

An approved batch is loaded in chunks of 500 rows, four partitions in parallel, one transaction per chunk; a failing chunk is retried row by row so one bad row fails alone. Loading the same batch again creates no duplicate: rows already loaded and unchanged are skipped through the key cross-reference. The planning target is 50,000 rows an hour per partition, proven in the dress rehearsal.

Before sign-off a batch can be rerun (only the rejected rows, after a fix) or rolled back (when its loader can undo the records and nothing has changed them since the load). In the production cutover the rollback point is a database snapshot (section 11.5).

## Reconciliation and sign-off

Every object is reconciled at up to five levels. The Reconciliation Approach and Sign-off document gives the measures per object, the sample plan for business verification and the sign-off forms.

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

![A legacy invoice and the processes that act on it after go-live (BRID 5.1-10.1)](figures/dm_legacy_invoice_flows.dot){width=12}

## Legacy sub-ledgers

The BRD asks for legacy Premium Receivable, Commission Receivable, DTIP and UPP sub-ledgers (BRID 5.2, 6.1, 6.3, 6.4, 7.1, 7.2, 8.1). Comptrollership assigns the account codes of the legacy control accounts and of the Migration Clearing account (DMQ18, DCR-197); this document names the accounts only.

<!-- table: widths=4.6,6.2,5.8 caption="Legacy sub-ledgers (account codes assigned by Comptrollership, DMQ18)" size=9 -->
| Legacy control account | Holds | Moved by |
|---|---|---|
| Premium Receivable - Legacy (by component) | Open premium of legacy invoices | Payments, automatch, dispositions, DPPR reversals, write-offs, endorsements |
| PR 2307 - Legacy | Open PR2307 of legacy invoices | PR2307 reversals |
| Due to Insurers (DTIP) - Legacy | Premium payable to insurers on legacy invoices | Remittance, DPPR and PR2307 reversals, endorsements |
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
| Load (T-2) | Invoice created in the Operations ledger with origin LEGACY; opening entry | Premium Receivable - Legacy 10,000.00 debit; DTIP - Legacy 23,050.00 credit; Commission Receivable - Legacy 4,480.00 debit; the unrealised part per DMQ13; the balance of the entry to Migration Clearing. Invoice 360 shows booked 23,050.00, applied 13,050.00, open 10,000.00 and the LEGACY badge |
| T+2 | The client pays 10,000.00 at the counter quoting I00123456 | BIBS finds the legacy invoice, issues a BIBS AR and applies the payment to basic. Entries: Dr Bank / Cr Unapplied Collections 10,000.00; Dr Unapplied Collections / Cr Premium Receivable - Legacy 10,000.00. The invoice is fully paid; the receipt is in the Cash Receipts Book |
| T+9 | Remittance extraction for the insurer | The invoice is extracted with paid AR 23,050.00 (13,050.00 paid in legacy and 10,000.00 in BIBS). Net due to the insurer 18,970.00 = 23,050.00 - 4,480.00 + 400.00. Entry: Dr DTIP - Legacy 23,050.00 and Dr CWT 400.00 / Cr Commission Receivable - Legacy 4,480.00 and Cr Due to Insurer for Disbursement 18,970.00. The schedule shows the legacy invoice number and source system; the commission OR is issued as for any batch |
| T+20 | Positive endorsement, additional basic premium 2,000.00 | The policy header is a migrated account (P01). BIBS books an endorsement invoice with a BI- number whose parent is I00123456 and ledger context LEGACY; its entries go to the legacy accounts; the service invoice is issued for the commission increase |
| Month end | Prod Recon legacy change report | Shows for I00123456 basic premium original 20,000.00, updated 22,000.00, delta 2,000.00, with the endorsement number, date and user (BRID 10.1) |

## Unapplied premium payments (UPP)

Every legacy UPP item with a balance at the freeze is created in the BIBS Unapplied Payments workbench with origin MIGRATED, its legacy AR number and date, payor, client, sales unit, amount, balance, status and the references the payor gave (invoice, cover, PN, bank reference). An opening entry puts the balance on Unapplied Collections - Legacy. No new AR is issued for money already acknowledged in legacy (proposal, DMQ14, DCR-204).

<!-- table: widths=3.6,6.2,6.8 caption="Processing legacy UPP in BIBS (BRID 5.1-5.5, 6.3, 6.4)" size=8.5 -->
| Process | Example | Entries (by context) |
|---|---|---|
| Automatch rerun (after every payment upload and hourly) | Legacy UPP of 5,000.00 quotes legacy invoice I00200001 (3,000.00 open) and new invoice BI-2028-000150 (2,000.00 open). The job applies both and closes the item | Dr Unapplied Collections - Legacy 3,000.00 / Cr Premium Receivable - Legacy 3,000.00; Dr Unapplied Collections - Legacy 2,000.00 / Cr Premium Receivable (new) 2,000.00 |
| Apply to another invoice (client instruction) | The client asks to apply a legacy UPP to another legacy invoice | Dr Unapplied Collections - Legacy / Cr Premium Receivable - Legacy |
| Refund | Approved refund of 1,500.00 | Dr Unapplied Collections - Legacy / Cr Refund Payable; payment request to Disbursement |
| Reclassification to other income | UPP of 1,500.00 unclaimed for more than 2 years; batch approved by the Cashiering TL and then top management (DMQ16) | Dr Unapplied Collections - Legacy / Cr Other Income (account per DMQ16); item closed |

## Payments, reversals, remittance, endorsements and Prod Recon

<!-- table: widths=3.4,6.4,6.8 caption="Other processes on legacy invoices" size=8.5 -->
| Process (BRID) | What the user does | What BIBS does |
|---|---|---|
| OTC payment (6.1) | Keys the legacy invoice number, ARN, policy or PN at the counter | Finds the legacy invoice, issues the AR, applies by component, posts to Premium Receivable - Legacy; Cash Receipts Book as usual |
| Autopay (6.2) | Uploads the bills payment, trade, CLPC or direct credit file as today | Matches rows that carry the legacy invoice number or the EBIX reference; applies and posts by the context of each invoice |
| DPPR batch reversal (7.1) | Uploads or selects the approved list of direct-payment legacy invoices; a checker approves | Reverses each open premium receivable against DTIP on the legacy sub-ledgers, one line at a time (Dr DTIP - Legacy / Cr Premium Receivable - Legacy); Commission Receivable effect per DMQ19 |
| PR2307 batch reversal (7.2) | Selects legacy invoices with PR2307 balances; approval | Offsets PR2307 - Legacy against DTIP - Legacy; commission effect per DMQ20 |
| Remittance (8.1) | Runs the extraction as today | Includes legacy invoices with paid premium not yet remitted; never remits again what was remitted in legacy; posts legacy lines to DTIP and Commission Receivable - Legacy |
| Endorsements (9.1-9.3) | Raises the endorsement request on the legacy invoice | Needs the migrated policy header; books the endorsement invoice in context LEGACY; non-financial changes update the account with no posting; renewal uses the updated values |
| Collections | Works the worklist as today | Legacy invoices above the threshold appear with the LEGACY badge; carried promises and assignments (F03) are shown |
| Prod Recon (10.1) | Runs the legacy change report | Lists original, updated and delta per change; legacy invoices are not part of the production register extract |

## When the legacy context closes

The legacy sub-ledgers run off as legacy invoices are paid, remitted, reversed or written off and as legacy UPP is applied, refunded or reclassified. The report MIG-LEGACY-POSITIONS shows the open legacy positions by component, insurer, client and age each month. When no legacy invoice or UPP is open and the legacy control accounts and Migration Clearing are 0.00, Comptrollership decides whether to close the legacy accounts.

# In-force policies and the renewal-driven transition

## In-force policy headers

BRID 4.1 asks for a minimal header of in-force policies "when operationally required" (p.8); capability 4 is conditional (p.6). BIBS needs the header for three things: to locate active coverage for servicing, to endorse a legacy invoice (BRID 9 cannot be met without it), and to renew the policy in BIBS. The proposal is therefore to migrate the header of **every policy in force at go-live** (DMQ09, DCR-194), in layouts P01 (header) and P01S (insurer shares).

A header becomes a BIBS account with origin MIGRATED and status BOOKED: client, product and line, insurer and shares, policy number, inception and expiry, sum insured, premium, currency, payment arrangement, PN numbers, AO, unit and branch. It has no quotation, placement or BIBS invoice. Its legacy invoices link to it. It is found by ARN, policy number, legacy reference or client.

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

- **Policy headers (P01).** The go-live extraction works only from the migrated headers, so P01 must hold every policy in force at go-live that expires up to 31 May 2028, and every renewal term booked in legacy that starts on or after go-live (so that the expiring term is recognised as renewed). The P01 reconciliation against the legacy in-force list and the extraction check (report MIG-RENEWAL-GOLIVE: per expiry month, headers = candidates + renewals booked in legacy) prove that no expiry is missed.
- **Renewal advices already sent (P03).** The RMEL and the dispositions are kept in Excel trackers today (DMQ38, register DCR-243). The migration takes only the RAs already sent from them, in a small reference file: one row per expiring term with the legacy policy reference, cover, expiry, RA date and reference, channel, recipient, proposed insurer and premium quoted, sender, and the tracker and sheet it comes from.

<!-- table: widths=3.2,9.6,3.8 caption="Excel intake of the RA-sent file (DMQ38)" size=8.5 -->
| Step | What happens | Who |
|---|---|---|
| Template | Excel template `P03_template.xlsx` (sheet RA_SENT, header in row 1, list on the channel, instructions sheet); the CSV template is also accepted | iorta TechNXT issues |
| Compilation and check | The maker copies the RA rows of the Retail and Corporate trackers into the template; the checker compares the file with the trackers (counts per tracker, 10 sample rows) and releases it | Renewal processing team - maker, checker |
| Validation | Header in P01; expiry equal to the header and from go-live to 31 May 2028; RA date not after the last legacy business day (warning when more than 140 days before expiry); cover not already renewed in legacy; one row per expiring term | BIBS |
| Rejection report | MIG-REJECTS of P03 in Excel with the row, column, value and message, and columns for the maker's correction and the checker's review | BIBS |
| Resubmission | The maker corrects the rows and sends a resubmission file with the corrected rows only; the checker reviews each correction against the tracker and approves it in the Migration Console (never the same person); it loads as a rerun batch. In production resubmissions close at 12:00 on the day before go-live; rows still rejected are checked against the tracker before any RA is sent | Maker, checker; Migration Operator |

The Head of the Renewal processing team owns the object (signs G1, G2 and G6). In the mocks the file is sent from Mock 1; in production once, with the RAs sent up to the last legacy business day.

**The January lead time.** The January expiries get days to four weeks instead of 140 days for insurer requests and RAs (risk 16). The mitigation: the day-1 priority queue (go-live extraction at 04:00, January expiries flagged urgent, earliest expiry first); a staffing plan for January signed by the Head of the Renewal processing team; and a Renewal team that is trained and has worked the day-1 queue on the Mock 4 and dress-rehearsal data before go-live.

A legacy policy renews on the new-business path pre-filled from its header, or as is when the Renewal sanitation resolves its package. The renewal account refers to the legacy reference, so the run-off tracker can link them. The tracker (report MIG-RUNOFF) shows, per expiry month, the legacy policies in force at go-live, renewed in BIBS, not renewed, lapsed and still open. Legacy systems are decommissioned when their last cohort has run off and the other checklist criteria are met (section 11.8).

# History, archive and read-only access

History is not migrated into BIBS business tables. For each legacy system BDOI chooses how history stays available (DMQ24, DCR-209):

<!-- table: widths=3.6,6.6,6.4 caption="Options for history" size=9 -->
| Option | What it means | When |
|---|---|---|
| Read-only legacy | The legacy application stays up in read-only mode, with its own access logging (BDOI IT). BIBS shows the legacy reference and a link | From the freeze until the system is decommissioned |
| BIBS archive | Closed transactions, expired policies, GL history and documents are loaded into the BIBS archive (layouts H01 and H02) and searched in Legacy Inquiry | Before a legacy system is decommissioned |

**Legacy Inquiry** searches the archive by client, policy or cover number, invoice, receipt, claim, date and record type, and shows each record read-only with its documents. Every search, view, download and export is logged (user, time, criteria, records, reason); the log cannot be changed; Compliance reviews it on screen and receives a monthly digest (FR-DM-110, FR-DM-111).

**Retention.** Archive records and access logs follow the retention rules: the umbrella BRD gives 5 years online and 15 years archive for historical data (R5 p.45). Reconciliation reports, sign-offs and run logs are kept as project records and are not purged with the staging data (proposed 10 years, DMQ29).

**Archive reconciliation.** Each archive load is reconciled against legacy by counts, amount totals and hash totals per record type, and Audit / Compliance checks samples in Legacy Inquiry before the system is decommissioned.

# Migration cycles

## Mocks, dress rehearsal and production

The migration is run five times before it counts, inside the SIT and UAT migration windows of the BDOI timeline and the full migration and cut-over window. Each run follows the same plan in the Migration Console, so timings and defects are measured and the production cutover repeats a rehearsed plan.

**Mock-load order.** In every run, reference data (R01-R07) and the client master (C01-C03) are loaded and accepted first, before any policy header, RA-sent file or open item. This follows the load order, and it gives the SIT and UAT of the Drop 1 modules, Renewal first, migrated clients and reference data to test with from the first mock: the prerequisite the concept paper sets for renewal (R10 section VI).

![Migration cycles on the BDOI timeline (go-live January 2028)](figures/dm_cycles.dot){width=15}

<!-- table: widths=2.2,2.4,2.6,4.7,4.7 caption="Migration cycles with entry and exit criteria" size=8 -->
| Cycle | When / where | Data and objects | Entry criteria | Exit criteria |
|---|---|---|---|---|
| Mock 1 | 19-30 Apr 2027; SIT | Masked full extracts; reference data and clients first, then policy headers and the RA-sent file (P03) | Build waves DM0 and DM1-C deployed on SIT by 9 Apr 2027; layouts frozen (M2); draft code maps; extracts for Mock 1 received (M4) | Objects loaded; L1-L4 run; defects logged; timing per object recorded; profiling report to the owners; P03 rejects reviewed by the Renewal processing team (maker-checker) |
| Mock 2 | 5-16 Jul 2027; SIT | Masked full extracts; all objects | Mock 1 exit met; build waves DM1-A, DM1-B, DM2-A, DM2-B deployed by 18 Jun 2027; code maps approved (M5); legacy accounts and rules configured on SIT | All objects loaded; L1-L5 reconciled; Migration Clearing 0.00 per branch and currency; no open Critical migration defect |
| Mock 3 (UAT load) | 2-13 Aug 2027; UAT | Masked extracts refreshed; all objects | Mock 2 exit met; UAT readiness statement issued (R7, programme item 11) | Business owners verify samples on screen (G6 rehearsal); the Drop 1 end-to-end UAT (Aug-Dec 2027) runs on the migrated data; data-quality issues below the thresholds |
| Mock 4 (UAT refresh) | 4-15 Oct 2027; UAT | Fresh masked extracts; all objects; go-live renewal extraction of the January-May 2028 expiries; a test true-up | Mock 3 exit met; cut-over date, year-end option, fallback and decommissioning criteria agreed (M6) | Run as a timed cut-over; go-live extraction check balanced and the day-1 queue worked by the Renewal team; test true-up reconciled; UAT continues on the refreshed data |
| Dress rehearsal | 15-26 Nov 2027 (reserve 6-10 Dec 2027); production-sized environment | Masked full-volume extracts taken after a legacy EOD | Mock 4 exit met; production-sized environment (performance test window); cutover plan frozen | Full cutover within the window with at least 20 percent margin; rollback (snapshot restore) rehearsed; go / no-go criteria measured |
| Production | 20 Dec 2027 (T-14) to 3 Jan 2028 (T) | Real data in production only | GNG-1 passed (Cutover Runbook) | GNG-3 GO; hypercare exit criteria (Cutover Runbook) |

Defects found in a cycle are fixed before the next one: in legacy (cleansing), in the code maps, in the layouts or in the loaders. The next cycle uses fresh extracts, so every fix is proven with new data.

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
| 4 | Each true-up is prepared by the Comptrollership GL lead and approved by the Head of Comptrollership (never the same person); it posts as an opening-balance adjustment journal dated 1 January 2028; FY2027 P&L effects go to retained earnings; adjustments on the legacy control accounts come with their invoice or UPP detail | True-up record in the Migration Console |
| 5 | Each true-up is reconciled before sign-off: cut-off (legacy journal listing = register = true-up), movement (true-up = change of the legacy TB), balance (BIBS opening = legacy TB), Migration Clearing 0.00 | MIG-TRUEUP-RECON, signed (Reconciliation Approach) |
| 6 | When January 2028 is already closed, it is reopened for the posting only and closed again the same day; no other journal may post in the window | Period audit trail |
| 7 | After the final true-up the legacy GL is locked and the true-ups are closed (about 2 May 2028); a later FY2027 finding is a prior-period adjustment in BIBS outside the migration | Closure sign-off |

## Freeze windows

<!-- dm:freeze -->

## Parallel run

There is no parallel run of legacy and BIBS on the same items. A parallel run would need each receipt, application, remittance and endorsement keyed twice and reconciled daily, and it contradicts the principle that an open item is processed in one system only (BRD p.5). The controls that replace it are the four mocks and the dress rehearsal on full extracts, the go / no-go criteria measured on production data, the rollback point until the go / no-go, and the daily hypercare reconciliation after go-live.

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

GNG-1 (T-15, start the pre-load) and GNG-2 (T-5, enter the freeze) are in the Cutover Runbook.

## Rollback

Until the go / no-go decision the rollback is a restore of the production database snapshot taken at the start of the production load, and legacy is reopened for business on Monday. After go-live, until the point of no return at 18:00 on the first business day, the same restore is possible with re-keying in legacy of the transactions keyed in BIBS that day. After the point of no return, issues are fixed forward in BIBS (DMQ32, DCR-200). The steps are in the Cutover Runbook.

## Hypercare

Hypercare runs from go-live to the first month-end close. Every day: Migration Clearing 0.00, legacy control accounts against their sub-ledgers, automatch results, the urgent January renewals, exception queues and failed jobs; defect triage twice a day. True-up 1 is posted and reconciled before the January close. Exit requires no open Critical or High defect, 10 consecutive business days of clean daily checks, true-up 1 signed, and the first month-end close with the ACSL GL-SL reconciliation (context LEGACY) without difference.

## In-flight items at the freeze

Items in process in legacy at the freeze (pending endorsements, un-booked accounts, placements, PDCs, check pick-ups, refunds, remittance batches) are, in order of preference, completed in legacy before the freeze, carried forward in a layout (F04, F06), or re-keyed in BIBS after go-live from a list signed by the business owner (DMQ33, DMQ21). A renewal placed or bound in legacy but not booked is such an item: it is booked in legacy before the freeze so that its new term migrates as a header. The runbook schedules the completion drives from T-12.

## Decommissioning

Each legacy system is decommissioned only against a signed checklist (FR-DM-123): final extracts reconciled, for EBIX and ISYS the final true-up reconciled and the legacy GL locked, archive loaded and reconciled, Legacy Inquiry verified by Audit / Compliance, no open inquiry or claim that needs the system, the last legacy-booked policy expired plus the claims tail, access logs archived, retention covered, and sign-off by the system owner, Compliance and Comptrollership (DMQ27). The checklists per system are in the Cutover Runbook.

# Data security

The migration follows the hosting appendix (R7) and the security controls of BIBS.

<!-- table: widths=4,12.6 caption="Data protection controls" size=9 -->
| Control | How it is applied |
|---|---|
| Hosting | All migration data (intake bucket, staging, archive) is in the BIBS environment in AWS ap-southeast-1 |
| Masked non-production data | Every non-production environment (SIT, UAT, dress rehearsal) receives data masked at intake; unmasked extracts never leave production |
| Staging purge within 5 days | Staging payloads and extract files are deleted within 5 days of the batch sign-off or rollback, or of an extract rejection (job MIG_STAGING_PURGE and a lifecycle rule on the intake bucket); an alert is raised when a signed-off batch still has payloads after the limit. Counts, hashes and totals are kept as evidence |
| Access restricted to the Philippines | The Migration Console, staging and the intake bucket are reachable only by migration roles and only from the Philippines (network allow-list and VPN at the infrastructure layer) |
| Encrypted transfer and storage | Files move through the console (TLS) or SFTP; at rest they are encrypted with KMS keys; never by e-mail or removable media |
| Access logging | Every console action (upload, validation, load, rerun, rollback, reconciliation, sign-off, decision) is audited; every access to archived history is written to the append-only legacy access log |
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
- **iorta TechNXT**: Project Manager; Migration Lead (loaders, reconciliation, profiling, console); Migration Operator (intake, validation, loads); DevOps (environments, snapshots, access); QA (smoke tests, migration test plan).

## RACI

R = responsible, A = accountable, C = consulted, I = informed.

<!-- table: widths=5.6,1,1,1,1,1,1,1,1,1,1,1 caption="RACI. PM = BDOI Program Manager; DML = Data Migration Lead; OWN = data owners; STW = data stewards; BIT = BDOI IT; COMP = Comptrollership; CMP = Compliance; IPM = iorta Project Manager; IML = iorta Migration Lead; IOP = iorta Operator / DevOps; GNG = go / no-go board" size=8 -->
| Activity | PM | DML | OWN | STW | BIT | COMP | CMP | IPM | IML | IOP | GNG |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Migration strategy and this document | A | R | C | I | C | C | C | R | R | I | I |
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
| Mocks and dress rehearsal | A | R | C | C | R | C | I | R | R | R | I |
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
| 1 | Legacy cannot give open balances per component or the paid / remitted split (DMQ12, DCR-207) | High | Profiling of the first full extracts (29 January 2027); "open-balance mode" (open balance loaded as booked, original values in the snapshot); split rules as configuration agreed with Comptrollership |
| 2 | Migration Clearing not 0.00 at cutover (detail and trial balance disagree) | High | Reconciliation in every mock from Mock 2; trial balance extracted after the same EOD as the detail; break explanations agreed with Comptrollership before GNG-3 |
| 3 | Duplicate clients merged wrongly, or not merged | High | Auto-merge only on hard keys; review queue for the rest; every lost value kept; client batches can be rolled back before sign-off |
| 4 | Answers to the build-shaping decisions arrive late (M1) | High | Decisions listed with dates (chapter 15); defaults in configuration where possible; build waves sequenced so late answers change configuration, not code |
| 5 | Load time exceeds the cutover window | Medium | Pre-load of reference data and clients; four partitions; dress rehearsal at full volume with 20 percent margin |
| 6 | Endorsements of legacy invoices without policy headers (BRID 9 depends on BRID 4.1) | High | Decide DMQ09 / DMQ22 by M1; proposal: headers of every in-force policy |
| 7 | Real personal data in a test environment | High | Masking at intake; unmasked files never leave production; 5-day purge; access only from the Philippines |
| 8 | Legacy postings after the freeze | High | Legacy set read-only at T-3 22:00 and tested; any late legacy transaction is a reconciliation break and is re-keyed in BIBS |
| 9 | In-flight items forgotten at the freeze | Medium | Completion drives from T-9; carry-forward layouts F04, F06; signed re-keying list |
| 10 | Code maps incomplete when transactions load | Medium | Unmapped-code report before load; gate G3 blocks the batch; map freeze at T-7 |
| 11 | BDOI steward capacity for cleansing and review | Medium | Named stewards per object by M2; profiling report per mock with the top issues; review queue sized from Mock 1 |
| 12 | Legacy system decommissioned before history is archived | Medium | Decommissioning only against the signed checklist; archive reconciled and verified by Audit / Compliance |
| 13 | Foreign-currency openings at the wrong rate (DMQ35) | Medium | Rate decision by M2; booking rate carried in the extract; reconciliation per currency |
| 14 | The draft BRD changes when it is signed (DCR-189) | Medium | This document and the FRS are re-issued; decisions are configuration where possible |
| 15 | Cut-over over the year-end holidays (24 December 2027 to 1 January 2028) | High | Year-end option confirmed by Comptrollership at M6 (DMQ39); tasks moved off holidays in the runbook; roster confirmed at T-29 |
| 16 | Tight lead time for the January 2028 expiries, processed in BIBS after go-live (DMQ37) | High | Day-1 priority queue (go-live extraction at 04:00, January expiries urgent, earliest first); January staffing plan signed at T-20; Renewal team trained and rehearsed on the Mock 4 and dress-rehearsal data; RAs already sent loaded so none is re-sent |
| 17 | Many candidates in the Renewal Exception bucket because packages are remapped at sanitation (DMQ36) | Medium | PACKAGE map loaded and tested in every mock; profiling of the legacy packages of the headers expiring up to 31 May 2028 without an entry, closed by TSU before the map freeze; each Exception-bucket choice fed into the next map version |
| 18 | A header missing from P01 means a renewal never extracted | High | P01 scope includes every expiry up to 31 May 2028 and every booked renewal term starting on or after go-live; P01 reconciled to the legacy in-force list; extraction check at go-live; go / no-go criterion |
| 19 | Late or large FY2027 adjustments after go-live (provisional opening, DMQ39) | Medium | December soft close by 20 December 2027; legacy GL restricted to named users and FY2027 periods; adjustment register; cut-off checks and reconciliation at every true-up; true-ups closed about 2 May 2028 |

# Dependencies and open decisions

## Dependencies

<!-- table: widths=1.2,9.4,3.2,2.8 caption="Dependencies" size=8.5 -->
| # | Dependency | Owner | Needed by |
|---|---|---|---|
| D1 | Build waves DM0 and DM1-C of the migration module (reference data, clients, headers, RA-sent file, PACKAGE map) deployed on SIT | iorta TechNXT | Mock 1 (deployed by 9 Apr 2027) |
| D2 | Build waves DM1-A, DM1-B, DM2-A, DM2-B (legacy invoices, UPP, reversals, remittance, endorsements, Prod Recon, cutover plan, run-off) deployed on SIT | iorta TechNXT | Mock 2 (deployed by 18 Jun 2027) |
| D3 | Build wave DM3 (end-to-end test, performance harness) | iorta TechNXT | Mock 2 (end-to-end test); dress rehearsal (performance harness) |
| D4 | Renewal module (BRD-6) in SIT for Mock 1 and live at go-live, with the legacy policy source, the go-live extraction (priority, urgent flag, RAs already sent) and the sanitation check PACKAGE_REMAP with the Exception bucket | iorta TechNXT | Mock 1; go-live |
| D5 | Legacy control accounts, Migration Clearing and legacy rule lines set up by Comptrollership | BDOI Comptrollership | Mock 2 (SIT); T-25 (production) |
| D6 | Extracts in the templates with control files, and volumes per object | BDOI IT | M3 (29 Jan 2027), M4 (9 Apr 2027) and every mock |
| D7 | Named data owners and stewards | BDOI | M2 |
| D8 | Legacy systems can be set read-only with access logging; SFTP drop | BDOI IT | Dress rehearsal |
| D9 | Users of every persona created through User Access requests | BDOI and iorta TechNXT | T-24 |
| D10 | Renewal processing team - RA-sent file from the trackers, maker-checker review of rejects, January staffing plan and training | BDOI Renewal processing team | Mock 1 (first file); T-20 (readiness); T-5 (final file) |
| D11 | Comptrollership - confirmation of option A, December soft close, legacy GL access list and FY2027 adjustment register, true-up preparation and approval | BDOI Comptrollership | M6; 20 Dec 2027; T-5; T+15 to T+120 |
| D12 | External audit of FY2027 timed so that the audited financial statements are available by about April 2028 | BDOI Comptrollership | Final true-up (T+92) |

## Open decisions and the date each is needed by

Decisions marked M1 change what is built and are needed by **16 October 2026**; M2 decisions are needed by **30 October 2026**. Both dates are inside the requirements and mapping window of the BDOI timeline (September-October 2026), so the build can start in November 2026 on agreed layouts. The others are tied to the migration calendar. The register column refers to the BRD discrepancy and clarification register (R6). BDOI answered DMQ36, DMQ37 and DMQ38 on 26 September 2026 and answered DMQ39 with a recommendation that Comptrollership confirms at M6; DMQ26 is answered by DMQ37.

<!-- dm:decisions -->

# Timeline

## Alignment with the BDOI timeline

The migration timeline follows the BDOI drop plan and timeline (R9): Drop 0 "Setup & Data Migration", go-live January 2028 (DMQ25), recommended Monday 3 January 2028 at the year-end boundary (DMQ39). It ties into the build of the migration module and into the UAT readiness programme (R7) as follows.

<!-- table: widths=3.4,5.2,8 caption="Migration timeline on the BDOI calendar" size=8.5 -->
| When (BDOI timeline stream) | Migration step | Link to build and UAT readiness |
|---|---|---|
| 26 Sep 2026 (M0) | This document set issued (version 1.2 with the BDOI answers to DMQ36-DMQ39) | FRS BRD-13 v1.2 under review |
| 16 Oct 2026 (M1) - requirements and mapping | Remaining build-shaping decisions answered (package remapping DMQ36 and the January-May 2028 renewals DMQ37 answered on 26 Sep 2026) | Needed before the build waves start |
| 30 Oct 2026 (M2) - requirements and mapping | Owners, stewards, keys, remaining rules; object decisions signed (G1); layouts frozen, version 1 | Build starts in November 2026 on agreed layouts; migration test plan cases finalised |
| Nov 2026 to Mar 2027 - build | Build waves in mock order: DM0 and DM1-C first, then DM1-A, DM1-B, DM2-A, DM2-B; DM3 | DM0 and DM1-C on SIT by 9 Apr 2027; the rest by 18 Jun 2027 |
| 29 Jan 2027 (M3) | First full extracts with volumes; profiling | DM3 performance harness sized from the volumes |
| 9 Apr 2027 (M4) - SIT migration | Extracts for Mock 1 | - |
| 19-30 Apr 2027 - SIT migration | Mock 1 on SIT: reference data and clients first, then headers and the RA-sent file | Drop 1 SIT (Renewal first) tests on migrated clients and reference data |
| 18 Jun 2027 (M5) | Code maps approved for Mock 2, including the PACKAGE map loaded for Renewal | - |
| 5-16 Jul 2027 - SIT migration | Mock 2 on SIT, all objects, L1-L5 | Provides the "data migration dry run with reconciliation" of UAT readiness item 11 |
| 2-13 Aug 2027 - UAT migration | Mock 3 = UAT load | UAT readiness statement issued; the Drop 1 end-to-end UAT (Aug-Dec 2027) runs on migrated data |
| 1 Oct 2027 (M6) | Cut-over date and window, fallback and decommissioning criteria agreed; Comptrollership confirms the year-end option A (DMQ39) | - |
| 4-15 Oct 2027 - UAT migration | Mock 4 = UAT refresh, timed as a cut-over, with the go-live renewal extraction and a test true-up | Business owners verify migrated data during UAT; the Renewal team works the day-1 queue |
| 15-26 Nov 2027 - full migration and cut-over; performance test | Dress rehearsal on the production-sized environment (reserve 6-10 Dec 2027) | Performance and penetration test Nov-Dec 2027 |
| 4 Dec 2027 (T-30) to 3 Jan 2028 (T) - full migration and cut-over; ORR / PRR | Production cutover (Cutover Runbook): December soft close by 20 Dec, pre-load from 20 Dec, freeze 31 Dec 22:00, load and provisional GL opening 1 Jan, go / no-go 2 Jan 18:00, go-live renewal extraction 3 Jan 04:00 | ORR / PRR Dec 2027-Jan 2028; production release frozen from T-7 |
| 3 Jan 2028 (T) to the January month-end close | Hypercare; day-1 renewal queue, January expiries first; true-up 1 after the legacy year-end close (about 18-21 Jan 2028) | Production support runbook (deliverable 24) |
| About March to 2 May 2028 (T+85 to T+120) | Final true-up after the audited FY2027 financial statements; legacy GL locked; true-ups closed | FY2027 BIR annual returns and audit from legacy |
| Run-off (about 12-15 months) | Monthly run-off tracking; archive loads; decommissioning | - |

The plan has no slack between the build and Mock 1: if DM0 and DM1-C are not on SIT by 9 April 2027, Mock 1 moves within the SIT migration window (to May 2027) and Mock 2 stays in July; the reserve slot of the dress rehearsal (6-10 December 2027) is the last buffer before the cut-over.

# Glossary {-}

```glossary
AR: Acknowledgment Receipt, issued for premium collected on behalf of an insurer
Archive: Read-only store of legacy history in BIBS, searched in Legacy Inquiry (BRID 11.1)
Carry forward: Treatment of an open operational or financial item that continues to be processed in BIBS
CMS: Collection Management System (legacy)
Code map: Versioned table that maps a legacy code of a source system to a BIBS code (BRID 3.1)
Cohort: The legacy policies expiring in one month (RMEL expiry month)
Control file: File sent with each extract with its row count, hash total, amount totals and SHA-256
DPPR: Direct-payment premium receivable (reading to be confirmed, DMQ19)
DTIP: Due to Insurer - Premium
EBIX: Legacy booking, receivables and accounting system
Freeze: The time from which legacy data may no longer change (T-3 22:00; 31 December 2027 for the recommended date); only FY2027 GL adjustments by named Comptrollership users follow it
Go-live renewal extraction: One Renewal extraction at go-live of every migrated header expiring up to 31 May 2028
Provisional opening: GL opening from the preliminary December 2027 TB, balance sheet only
RA-sent file: Reference file (P03) of the RAs sent by hand before go-live
G1-G7: Sign-off gates of a data object - decision, mapping, validation, load, reconciliation, acceptance, go-live
Go / no-go: The decision to go live, taken on measured criteria
ISYS: Legacy reporting system (claims and ACSL reports, Marketing Diary)
Layout: The field list of one extract file (a sheet of the workbook and a CSV template)
Legacy invoice: Invoice booked in EBIX or QPS before go-live and still open at the freeze, processed in BIBS
Legacy sub-ledger: Legacy control accounts and ledger items that hold the legacy positions apart from the new ones
Migration Clearing: GL account that takes the offset of every opening entry; must net to 0.00
Mock: Rehearsal of the migration on a test environment with masked data
OTC: Over-the-counter payment
PR: Premium receivable
PR2307: Premium receivable covered by the client's BIR Form 2307
QPS: Legacy quotation, client and policy system
RMEL: Renewal Master Expiry List
Run-off: The period in which legacy policies expire and renew into BIBS
Staging: Temporary area in BIBS where extract rows are checked, mapped and validated before load
T: Go-live date - January 2028 (BDOI timeline), recommended Monday 3 January 2028
True-up: Opening-balance adjustment journal for a FY2027 closing or audit adjustment
UPP: Unapplied premium payment
Xref: Key cross-reference from a legacy key to the BIBS record created from it
```

# Sign-off {-}

By signing, BDOI approves the approach of this document and the proposed decisions per data object, subject to the open decisions of chapter 15, whose answers are applied through the object register, the code maps and the configuration.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing; Head, Corporate Marketing", organisation: BDOI}
  - {name: "", role: "Compliance", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
