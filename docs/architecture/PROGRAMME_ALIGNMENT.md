# BIBS programme alignment: BDOI drops, timeline, integrations and infrastructure

Status: **analysis for BDOI review, 26-Sep-2026.** Client document built from the same data:
`docs/deliverables/out/Programme/Alignment/BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v1.0.docx` and
`BIBS_Alignment_BRD-00_Integration_Inventory_v1.0.xlsx` (source `docs/deliverables/src/alignment/`, data file
`alignment_data.yaml`, builder `build_alignment_pack.py`). The tables of sections 2 to 6 are generated from that data
file; change the data file and regenerate both.

## 1. Sources and BDOI answers

### 1.1 Sources (all in `docs/source-documents/`)

| Ref | Document | Read |
|---|---|---|
| S1 | `BDOI Drop Plan - modules and integrations.webp`, transcribed in `BDOI_DROP_PLAN.md` | Every line item; ICBS is struck through on the slide |
| S2 | `BDOI Programme Timeline.webp` | Bars read against the month grid (see 4.1: the transcription differs for the requirement bars) |
| S3 | `BDOI_IER_Workbook_v20_iorta.xlsx` (FR-ITC-ENG007 v19 template) | All 28 sheets including the 3 hidden ones, all cell values and formulas, the three embedded images (`xl/media/image1.png` logo; `image2.tmp` = the "Architecture Diagram" sheet, a multi-tenant microservice picture titled "Technical Architecture - Multi-Tenant"; `image3.tmp` = the "Architecture Diagram (Infra)" sheet, "AWS EKS Architecture - System Overview"). The "Architecture Diagram (Infra)" sheet has 294 rows because of hidden rows 142-294 that hold only four stray labels ("DB1", "DB2" twice) left from the picture; no other content |
| S4 | `Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` | All 7 pages; Annex A (p.4, a timeline figure) read as an image |
| S5 | BDOI answers of 26-Sep-2026 and `BDOI Integration Systems 1.png`, `2.png` (recorded at the end of `BDOI_DROP_PLAN.md`) | A1-A6 below |
| S6 | `docs/architecture/DOCUMENT_STORAGE_DECISION.md` | S3 decision (option C) |

### 1.2 BDOI answers (26-Sep-2026)

- **A1** All modules go live together in January 2028; no early go-live. Drops split the requirements, build, test and sign-off workload; the platform is built end to end and goes to UAT complete. The concept paper is superseded as an operating model.
- **A2** Integration names: EIAM (Entra ID), UIDM-ISC (IGA), ECM, CCM (e-mail sending), M365 Exchange, LMS (Loans Management System), HL-LOAS (Home Loan System, Loan Origination and Admin), LFS SaaS (Loan Front-End System), PMS (PDC Management System), CMS / New BOB (Cash Management System, outward payments), OBPCS (Online Bills Payment Consolidation System), Old BOB (Old Business Online Banking Collection System, SOA bills payments via funds transfer), TFS (Trade Finance System, client onboarding and payment, manual), EDP, EGL, Bridger Insight XG. All touch-points are owned by BDOI IT. Not explained: AFTS, CARMS, "Insurer System".
- **A3** Remittance handles the reinsurance transactions in Drop 1; the reinsurance module is phase 2.
- **A4** Documents and attachments live in S3 only (design: S6).
- **A5** BRDs, FRS, test plans and collaterals are grouped under the drops (section 2.3).
- **A6** BDOI supplies the IER architecture diagrams: produced as `docs/deliverables/out/Programme/Alignment/IER/BIBS_IER_Application_Architecture.png` and `BIBS_IER_Infrastructure_Deployment.png` (sources `docs/deliverables/src/alignment/figures/al_application_architecture.dot`, `al_deployment.dot`, rendered at 300 dpi).

## 2. Drop mapping

### 2.1 Every drop item against BRDs, FRS, BIBS modules and build status

| Drop | # | Item | BRD named by BDOI | BIBS BRD | FRS | BIBS modules and screens | Status | Gap or mismatch | Refs |
|---|---|---|---|---|---|---|---|---|---|
| Drop 0 | 0.1 | Accessibility & Login | - | BRD-11 UAM; BRD-00 XC-01 | FR-UA (log-in, session, password); FR-CR | security (JWT log-in, lockout, sessions); Setup & Administration > Users; accessibility statement (deliverable 40) | Mixed | No BRD named. Log-in is built with local passwords; EIAM (Entra ID) sign-in is not built (the DirectoryAuthenticator port is for a password bind, not an OIDC redirect). "Accessibility" may mean WCAG (UAM-NFR-05) or system access. | IQ04, IQ24; DCR-214, DCR-230 |
| Drop 0 | 0.2 | Authorization | - | BRD-11 UAM | FR-UA | security (roles, permissions, persona menus); Setup & Administration > Access Requests | Built | No BRD named. Access requests and approvals are in BIBS (BRD-11); UIDM-ISC (the IGA) may take them over. | IQ05; DCR-229 |
| Drop 0 | 0.3 | User Maintenance | - | BRD-11 UAM | FR-UA | nbadmin (request lifecycle, four eyes, bulk), security (UserAdminService, access change log) | Built | Provisioning by UIDM-ISC conflicts with BRD-11 principle "a request in BIBS is the only way to change access". | IQ05; DCR-229 |
| Drop 0 | 0.4 | Data Management (GL accounts, reference tables) | - | BRD-00 CORE-17; BRD-5 (chart of accounts); BRD-1 (LOVs); BRD-3; BRD-13 (reference data R01-R09) | FR-CR (CORE-17); FR-AC (chart); FR-NB-132 (LOVs) | coa (chart, upload), lov (LOV master with approval), catalog (insurers), dimension; Broking Setup > Lists of Values | Mixed | CORE-17 has no BRD: MIS field definition is NEW (CRQ09), insurer management is CHANGE (one screen). Acceptance criteria not stated. | IQ24; DCR-214 |
| Drop 0 | 0.5 | Workflow | - | Cross-BRD (every BRD with approvals) | Workflow chapters of each FRS | workflow (definitions, queues, SLA timers), approval (My Approvals) | Built | No BRD; accepted through the workflows of each BRD. The BRD-6, 8, 9, 12 and 13 workflows are designed, not built. | DCR-214 |
| Drop 0 | 0.6 | Product Maintenance | - | BRD-3 PM | FR-PM | catalog, productmaint; Client & Policy > Products, Package Requests | Built | None. BRD-3 is also named for Quotation (1.U2). | - |
| Drop 1 upstream | 1.U1 | Client Onboarding | Data Migration BRD, New Business BRD | BRD-1 NB; BRD-13 DM (C01-C03); BRD-10 SANC (onboarding screening) | FR-NB (client); FR-DM; FR-SS | crm (Clients), screening (onboarding check); migration (designed) | Mixed | Client onboarding is built; client migration is designed. Screening at onboarding (BRD-10, built) is in no drop. | IQ23; DCR-213 |
| Drop 1 upstream | 1.U2 | Quotation or Proposal | New Business BRD, Product Maintenance | BRD-1 NB (incl. Non-Package); BRD-3 PM | FR-NB; FR-PM | quotation, nonpackage; Client & Policy > Quotations, Proposal Requests | Built | Non-Package Management (CORE-04) is not named; read as part of this item. | - |
| Drop 1 upstream | 1.U3 | Account Creation and Maintenance | New Business BRD, CSF | BRD-1 NB; BRD-9 CSF | FR-NB; FR-CSF | account (Accounts); csf (designed) | Mixed | Accounts are built; the CSF maintenance functions (contact updates, service requests) are designed, not built. The rest of CSF is in no drop. | IQ23; DCR-213 |
| Drop 1 upstream | 1.U4 | Submitted Policy | Submitted Policies | BRD-12 SP | FR-SP | submitted (designed) | Designed | Build needed in wave 1 (Nov-Dec 2026). Sources LFS, HL-LOAS, LMS (LAMD) are integrations. | IQ06, IQ07, IQ08 |
| Drop 1 upstream | 1.U5 | Renewal | Renewal | BRD-6 RN | FR-RN | renewal (designed; waves R0-R3) | Designed | Build needed in waves 1-2. The early renewal release is withdrawn (answer A1); legacy RMEL cohorts are carried forward at cut-over (P03). | IQ02, IQ03; DCR-218, DCR-219 |
| Drop 1 upstream | 1.U6 | Placement & ePolicy | New Business BRD, Renewal BRD, Employee Benefits | BRD-1 NB; BRD-6 RN; BRD-8 EB | FR-NB; FR-RN; FR-EB | placement, issuance (Placement and Issuance Workbenches); eb (designed) | Mixed | EB placement is in Drop 1 while the EB module is in Drop 2. | IQ22; DCR-235 |
| Drop 1 upstream | 1.U7 | Booking | New Business BRD, Renewal BRD, Submitted Policies | BRD-1 NB; BRD-6; BRD-12 | FR-NB; FR-RN; FR-SP | booking (Booking Workbench, EOD booking batch) | Mixed | Booking is built; the shared business-type change (V822, BT0) for renewal and submitted policies is designed. | - |
| Drop 1 upstream | 1.U8 | Accounting / GL | Accounting, Disbursement & ACSL BRD | BRD-5 ACCT | FR-AC | accounting engine (BROKER_BOOKING events), journal, ledger | Built | Listed again as downstream 1.D5. Read as: upstream = postings of booking; downstream = GL, FRBS and ACSL. | - |
| Drop 1 upstream | 1.U9 | Reports (upstream) | NB, RN, SP, EB BRDs | BRD-1, 6, 12, 8; Report List | Report sections of each FRS | nbreport, report (Report Centre); renewal, submitted, eb reports (designed) | Mixed | NB reports built; RN, SP and EB reports designed. The Report List is not split by drop. | DCR-235 |
| Drop 1 downstream | 1.D1 | Disbursement | Accounting, Disbursement & ACSL BRD | BRD-5 ACCT (Vol. 2) | FR-DS, FR-PQ | disbursement, payrequest; Finance > Disbursement Workbench | Built | Outward bank channel is CMS / New BOB (a file or host-to-host interface); today BOB approvals are uploaded (DISB_BOB_APPROVED). | IQ10 |
| Drop 1 downstream | 1.D2 | Cashiering (Payments Acceptance and Application) | Operations BRD (Cashiering Addendum), Data Migration BRD | BRD-2 OPS; BRD-13 (legacy UPP, F02) | FR-OP (Cashiering); FR-DM | cashiering (payment files PAY_BILLS, PAY_TRADE, PAY_CLPC, PAY_PDC, PAY_DIRECT_CREDIT; automatch; AR / OR) | Built | The "Cashiering Addendum" is not in the BIBS pack. Channels OBPCS, Old BOB, PMS and TFS feed this module. | IQ09, IQ10, IQ14, IQ20; DCR-215 |
| Drop 1 downstream | 1.D3 | Remittance | Operations BRD (Remittance Addendum), Reinsurance BRD | BRD-2 OPS; ReInsurance BRD (phase 2) | FR-OP (Remittance) | remittance (extraction, batches, holds, special remittances) | Built | Answer A3: Remittance handles the reinsurance transactions in Drop 1; the reinsurance module is phase 2. Which RI transactions, and the "Remittance Addendum", are not defined. | IQ20; DCR-210, DCR-215 |
| Drop 1 downstream | 1.D4 | Adjustment / Cancellation | Operations BRD, Accounting BRD | BRD-2 OPS; BRD-5 | FR-OP (Adjustment) | adjustment (Adjustment Workbench) | Built | None. | - |
| Drop 1 downstream | 1.D5 | Accounting / GL | Accounting, Disbursement & ACSL BRD | BRD-5 ACCT (Vol. 1, 2) | FR-AC; FR-DS (ACSL) | coa, journal, ledger, period, closing, frbs, acsl | Built | EGL (Drop 2 integration) takes GL data out of BIBS; not built. | IQ16 |
| Drop 1 downstream | 1.D6 | Reports (BIR / Regulatory) | Accounting BRD, Data Migration BRD | BRD-5 ACCT; BRD-13 | FR-AC (BIR, IC) | tax (BIR forms, 2307, alphalists), finreport, frbs report pack | Built | Legacy-period figures in BIR reports depend on the migration of open items and the GL opening (DMQ18). | - |
| Drop 1 downstream | 1.D7 | Collection of Commission Receivables (Direct Payment) | Collections BRD Addendum, Operations BRD | BRD-2 OPS (CMRID); BRD-4 BRCLXN.059-064 | FR-OP (Commission / DP); FR-CL | commission (DP billing), collections (DP PR lists) | Built | BRCLXN.061-064 are in the unsigned draft addendum only (CQ01). | - |
| Drop 2 | 2.1 | Marketing Collection (extraction) | - | BRD-4 CLXN; BRD-2 (MKTID) | FR-CL; FR-OP (Marketing Collection items) | collections (worklists, dispositions, daily files), opsledger CollectionFeed | Built | BRD-4 is a BIBS module fed by the Operations ledger; "extraction" is undefined (a feed to a separate CMS application, or the daily files?). | IQ21; DCR-212, DCR-221 |
| Drop 2 | 2.2 | Claims | - | BRD-7 CLM | FR-CM | brokerclaims (Claims Home) | Being built | Built ahead of the drop; open-claims migration (F07) is conditional (CLQ14). | - |
| Drop 2 | 2.3 | Production Reconciliation | - | BRD-2 OPS (PRCID) | FR-OP (Production Reconciliation) | prodrecon (Reconciliation Workbench) | Built | Built with Operations; tested in SIT Drop 2 as BDOI plans. Insurer feedback files are an Insurer System integration. | IQ18 |
| Drop 2 | 2.4 | Employee Benefits (no portal feature) | - | BRD-8 EB | FR-EB | eb (designed); portal module (designed) to be dropped | Designed | BRD-8 BRID-005, 005.01, 014 and the EB design use an insurer / client-HR portal; the IER diagram shows an "EB Insurer Portal" tenant. | IQ22; DCR-211 |
| Drop 2 | 2.5 | Other Reports | - | Report List; BRD-00 CORE-21 | FR-CR (reports) | report (Report Centre, 174 definitions) | Mixed | "Other" is not defined; propose: every Report List item not owned by a Drop 1 module. | DCR-235 |

### 2.2 Gaps and mismatches

1. **Reinsurance under Remittance (1.D3)** - answered in part (A3). The `remittance` module handles insurers; reinsurance counterparties (reinsurers, cedants) exist as party types in `party` (REINSURER, RI_BROKER, see CORE_REPLACEMENT_IMPACT.md) and the finance-suite `reinsurance` module (treaties, cessions) is not scoped for BDOI. Which RI transactions go through Remittance, and their accounting, is not defined; the "Remittance Addendum" and "Cashiering Addendum" named on the slide are not in the pack (the Operations pack has only Addendum 1). IQ20, DCR-210, DCR-215.
2. **EB "no portal feature" (2.4)** conflicts with BRD-8 BRID-005 / 005.01 / 014, the Q06 / Q31 answers ("EB insurers use a secure portal"), the EB design (`portal` module, V1032, `ExternalUserProvisioner`, decision D7) and the IER diagram tenant "EB Insurer Portal ~50 partners". Build impact: drop `portal` from wave E1-A; EXTERNAL access requests stay refused (`NbadminPortDefaults`). IQ22, DCR-211.
3. **"Marketing Collection (extraction)" (2.1)** vs BRD-4 as a BIBS module (COLLECTIONS_DESIGN principle 1, OQ01 answered). IQ21, DCR-212. Related name clash: "CMS" = Collection Management System in our specs (BDOI_CLXN_BRD_SPEC.md, DM objects C01 / F02 / F03, `MIG_SOURCE_SYSTEM` value CMS) but Cash Management System on the slide. DCR-221.
4. **Claims and Production Reconciliation in Drop 2** are built / being built; no conflict. Production Reconciliation is a chapter of FRS BRD-2 (FR-OP section "Production Reconciliation") and of `brd02_cases.yaml`.
5. **Data Management, Workflow, Accessibility & Login** have no BRD (CORE-17 in BDOI_CORE_BRD_SPEC.md; `workflow`; BRD-11 plus EIAM; UAM-NFR-05 / deliverable 40 for WCAG). IQ24, DCR-214.
6. **Not placed:** BRD-10 Sanction Screening and the CSF service-request functions. IQ23, DCR-213.
7. **HL-LOAS in two drops** (HLS Drop 0, LOAS Drop 2). IQ07, DCR-216.
8. **EB in Drop 1** (Placement & ePolicy, Reports upstream) while EB is Drop 2. DCR-235.
9. **LFS SaaS** is in the BDOI integration list but not on the slide. IQ08.

### 2.3 Drop-to-document map (for the folder restructure, A5)

| Document | Kind | Files | Drop (folder) | Shared with | Note |
|---|---|---|---|---|---|
| BRD-00 Core Replacement (umbrella) | FRS, spec | BIBS_FRS_BRD-00_Core_Replacement_v1.0.docx | Programme | All drops (cross-cutting FR-CR, NFRs) | Stays at programme level |
| BRD-11 User Access Maintenance | FRS, test plan | BIBS_FRS_BRD-11_*; BIBS_TestPlan_BRD-11_* | Drop 0 | - | Items 0.1-0.3; EIAM and UIDM-ISC change it |
| BRD-3 Product Maintenance | FRS, test plan | BIBS_FRS_BRD-03_*; BIBS_TestPlan_BRD-03_* | Drop 0 | Drop 1 (quotation, 1.U2) | Item 0.6 |
| BRD-13 Data Migration | FRS, test plan, migration pack (strategy, workbook, runbook, reconciliation, templates) | BIBS_FRS_BRD-13_*; BIBS_TestPlan_BRD-13_*; out/Drop-0_Setup_and_Data_Migration/Migration/* | Drop 0 | Drop 1 (client onboarding, cashiering, BIR reports) and the migration stream | Migration has its own timeline row |
| BRD-1 New Business | FRS, test plan | BIBS_FRS_BRD-01_*; BIBS_TestPlan_BRD-01_* | Drop 1 | - | Upstream items 1.U1-1.U3, 1.U6, 1.U7, 1.U9 |
| BRD-6 Renewal | FRS, test plan | BIBS_FRS_BRD-06_*; BIBS_TestPlan_BRD-06_* | Drop 1 | - | Upstream 1.U5 |
| BRD-12 Submitted Policies | FRS, test plan | BIBS_FRS_BRD-12_*; BIBS_TestPlan_BRD-12_* | Drop 1 | - | Upstream 1.U4 |
| BRD-10 Sanction Screening and Risk Profiling | FRS, test plan | BIBS_FRS_BRD-10_*; BIBS_TestPlan_BRD-10_* | Drop 1 (proposed) | Drop 2 (Bridger Insight XG) | Not on the slide; proposed with Client Onboarding (IQ23) |
| BRD-9 Customer Servicing Facility | FRS, test plan | BIBS_FRS_BRD-09_*; BIBS_TestPlan_BRD-09_* | Drop 1 | Drop 2 (proposed for the service-request functions) | Only account maintenance is on the slide (IQ23) |
| BRD-5 Accounting, Disbursement and ACSL | FRS (cover note, Vol. 1, Vol. 2), test plans | BIBS_FRS_BRD-05_*; BIBS_TestPlan_BRD-05_* | Drop 1 | Drop 0 (GL accounts, item 0.4) | Upstream 1.U8, downstream 1.D1, 1.D5, 1.D6 |
| BRD-2 Operations | FRS, test plan | BIBS_FRS_BRD-02_*; BIBS_TestPlan_BRD-02_* | Drop 1 | Drop 2 (Production Reconciliation chapter, 2.3) | Test plan sheets split by chapter |
| BRD-4 Collections | FRS, test plan | BIBS_FRS_BRD-04_*; BIBS_TestPlan_BRD-04_* | Drop 1 | Drop 2 (Marketing Collection extraction, 2.1) | DP rows BRCLXN.059-064 in Drop 1 (1.D7); rest per IQ21 |
| BRD-7 Claims | FRS, test plan | BIBS_FRS_BRD-07_*; BIBS_TestPlan_BRD-07_* | Drop 2 | - | 2.2 |
| BRD-8 Employee Benefits | FRS, test plan | BIBS_FRS_BRD-08_*; BIBS_TestPlan_BRD-08_* | Drop 2 | Drop 1 (EB placement and reports, 1.U6, 1.U9) | Portal requirements out (answer, IQ22) |
| Report List as of 27-Apr-2026 | Report catalogue | BDOI_REPORT_LIST.md | Programme | Each report goes with the drop of its owning module | Split rule: 1.U9, 1.D6, 2.5 |
| Discrepancy and clarification register | Register (Excel) | BIBS_Register_BRD-00_Discrepancies_and_Clarifications_*.xlsx | Programme | Filter column Drop to be added | DCR-210 to DCR-235 from this document |
| Business process deck | PowerPoint | BIBS_Deck_BRD-00_Business_Process_*.pptx | Programme | All drops | - |
| Programme alignment and integration inventory (this document) | Word, Excel | BIBS_Alignment_BRD-00_* | Programme | All drops | Integration rows carry their drop |
| Bill of materials, technical and deployment architecture (items 4, 12) | Word, Excel (to write) | - | Drop 0 | - | Built from section 6 and the IER |
| Security and data-protection mapping (item 26) | Word, Excel (to write) | - | Drop 0 | - | EIAM, UIDM-ISC, S3 encryption, masking |
| Interface specifications (one per integration) | Word (to write) | - | Drop of the integration | - | After BDOI IT answers the IQ questions |
| UAT plans and sign-off forms (item 30) | Word (to write) | - | Per drop | End-to-end UAT script is programme level | UAT runs end to end (answer A1) |
| Performance, penetration test, ORR / PRR evidence (items 28, 37) | Word, Excel (to write) | - | Programme | - | Nov 2027 - Jan 2028 |

Folders as built (26-Sep-2026) under `docs/deliverables/out/`: `Drop-0_Setup_and_Data_Migration/`,
`Drop-1_Transactional/`, `Drop-2_Independent/` and `Programme/`, each with `FRS/`, `TestPlans/`, `Migration/`,
`Registers/`, `Decks/` or `Alignment/` as applicable and an index `README.md` (BRD, version, BDOI dates of the drop).
Documents moved by the "Drop (folder)" column; a document shared with another drop stays in its primary drop and the
index of the other drop points to it (no copies). The drop map is `tools/deliverables/brand.py` (`BRD_DROP`,
`DROP_SHARED`), used by every builder. Sources stay in `src/<kind>/`. The register has a Drop column (v1.2).

## 3. Early renewal concept paper (superseded)

The paper (dated 06/09/2026 on the signature block, MANCOM sign-off block unsigned in the PDF) proposed a pre-cut-over
renewal release by 15-Aug-2027 for January-May 2028 expiries (140 days before 1-Jan-2028 is 14-Aug-2027, the
`RNW_EXTRACTION_LEAD_DAYS` default), with TSU package maintenance and quotation, SP and FFY renewal, early migration of
the client master and renewal reference data, and no placement or booking before cut-over. **A1 supersedes it.**

| Concept paper capability | BIBS design | Status | Note |
|---|---|---|---|
| Renewal intake and pipeline tracking | BRD-6 Renewal Home, Expiry List (RENEWAL_DESIGN section 12) | Designed | Build waves R0-R2 |
| RMEL extract ingestion and validation | RNW_EXTRACTION from the ledger; legacy cohorts through LegacyPolicySource (P03) and RNW_LEGACY_POLICIES | Designed | P03 layout in the migration workbook |
| Data sanitation and enrichment | Check engine, buckets Clean / Review / Exception (BRRN.020-023) | Designed | Check content still open (RQ01) |
| Disposition management | Assignment, disposition, TL review (R1-B) | Designed | - |
| Proposal / quotation generation | quotation and nonpackage with renewalRef (R0 contract change) | Built (NB) + Designed (link) | - |
| Insurer engagement and approval tracking | Insurer batches and responses (R1-C) | Designed | Insurer channel is INT-19 |
| Renewal advice, NAL, NRL | Letter engine (R1-D), protected e-mail via CCM | Designed | - |
| Pre-placement adjustments | Account amendment before placement (BRD-1) | Built | Meaning to confirm |
| Product / Package Maintenance (TSU) | BRD-3 catalog and productmaint | Built | - |
| Quotation capability for renewals | RatingQuery.Purpose.RENEWAL (BT0 / R0) | Designed change | - |
| Submitted Policy renewal | RenewalHandOff implemented by Renewal (R3) | Designed | After SP build |
| Free First Year renewal | RNW_RA_FFY template; SP FFY buckets | Designed | Q36 payer open |
| Early migration of client master and reference data | Migration R01-R09, C01-C03 at T-2 weeks | Designed | No longer early (answer A1) |

What the early release would have required, recorded so the decision is traceable: a production environment and
ORR / PRR by July 2027; client master and reference data (R01-R07, C01-C03) loaded into production in July 2027 with
daily deltas for five months while legacy stayed the system of record (contradicting DM_STRATEGY_AND_APPROACH "no feed
after go-live" and DCR-202); RMEL cohorts ingested from legacy files (the renewal design extracts from the BIBS ledger,
RENEWAL_DESIGN principle 2, so the check engine would have read outstanding premium and claims from the RMEL file, not
live); a progression gate stopping renewal accounts before placement until T. None of this is built now.

What remains (updated with the BDOI answers to DMQ36-DMQ39 of 26-Sep-2026, which answer IQ02 and IQ03):
- **Renewals around T - answered (DMQ37):** the January-May 2028 expiries are processed in BIBS after go-live; no candidate is carried from legacy. At go-live Renewal extracts every expiry from go-live to 31-May-2028, January expiries flagged urgent, and records the renewal advices already sent by hand before go-live from the Excel trackers (rejected rows reviewed maker-checker by the Renewal processing team, DMQ38); RENEWAL_DESIGN section 13.1, FRS BRD-6 v1.1 FR-RN-016. The text that follows is the earlier proposal, kept for the record.
- **Renewals around T** (IQ02, earlier proposal): unchanged DM design - cohorts T to T+140 days carried forward (object P03, layout P03 in `dm_layouts.yaml`, `renewal.service.port.LegacyPolicySource` implemented by `migration`, DATA_MIGRATION_DESIGN section 15); renewals expiring between the freeze and T placed in legacy or held by hold covers.
- **Package remapping - answered (DMQ36):** at Renewal sanitation, not at upload. The migration loads the legacy package code and the `PACKAGE` code map only; the check is named `PACKAGE_REMAP` in the build (this document called it `PACKAGE_MAPPING`) and sends unmapped packages to the Exception bucket for the Renewal processing team (RENEWAL_DESIGN sections 8.1 and 13.1; FRS BRD-6 v1.1 FR-RN-028).
- **Package remapping** (IQ03, DCR-219, earlier proposal): recommended hybrid - a versioned `PACKAGE` code map set at migration intake (add to the code-map list of `dm_layouts.yaml`, today it has no PACKAGE set) plus a renewal sanitation check `PACKAGE_MAPPING` (new bean in `renewal.service.check`, RENEWAL_DESIGN section 8.1) that sends unmapped / retired / split packages to Review for TSU; owner TSU with the MBS steward; rules agreed before the first full extract (15-Jan-2027).
- **"R0 early renewal" release:** not planned. Note the name clash: R0 is also the first Renewal build wave (RENEWAL_DESIGN section 14).

## 4. Timeline alignment

### 4.1 Reading of the slide

Bars read against the month grid: Reqt Drop 1 Sep-Nov 2026 and Reqt / Mapping Migration Sep-Nov 2026 (the
transcription in `BDOI_DROP_PLAN.md` says Sep-Oct); Reqt Drop 2 Dec 2026-Feb 2027 (transcription: Nov-Feb); Build Drop 1
Nov 2026-Feb 2027; Build Migration Nov 2026-Mar 2027; Build Drop 2 Mar-Apr 2027; SIT Drop 1 Jan-Jul 2027; SIT Migration
Apr-Jul 2027; SIT Drop 2 Jul-Sep 2027; UAT Drop 1 Aug-Dec 2027; UAT Migration Aug-Oct 2027; UAT Drop 2 Oct-Nov 2027;
Full Migration & Cut-over Nov 2027-Jan 2028; Perf / PenTest Nov-Dec 2027; ORR / PRR Dec 2027-Jan 2028; go-live marker
at the end of January 2028. Drop 0 has no bar (wave 1).

| Stream | BDOI window | Status today | What we deliver, by when |
|---|---|---|---|
| Requirements Drop 1 (with Drop 0 setup) | Sep - Nov 2026 (slide bar; the transcription says Sep - Oct) | FRS v1.0 of BRD-00, 1, 2, 3, 5, 6, 9, 10, 11, 12, 13 issued 25-26 Sep 2026; register v1.1 | FRS v1.1 with BDOI comments by 13 Nov 2026; sign-off by 30 Nov 2026; answers to the build-shaping IQ / DCR items by 16 Oct 2026 |
| Requirements / mapping Migration | Sep - Nov 2026 | Migration pack v1.0 issued 26 Sep 2026 (strategy, workbook, runbook, reconciliation) | Object decisions (G1) and layouts frozen by 30 Nov 2026; first full extracts by 15 Jan 2027 |
| Requirements Drop 2 | Dec 2026 - Feb 2027 | FRS v1.0 of BRD-4, 7, 8 issued; EB FRS to re-issue without the portal | Sign-off by 26 Feb 2027; EGL, EDP, CARMS, insurer specifications from BDOI IT by 31 Jan 2027 |
| Build Drop 1 (waves 1 and 2) | Nov 2026 - Feb 2027 | Built: BRD-1, 2, 3, 4, 5, 10, 11. To build: BRD-12 SP, BRD-6 RN, BRD-9 CSF (account maintenance), EIAM, UIDM-ISC, S3 store, bank channel layouts | Wave 1 (Nov-Dec): SP, RN foundation, EIAM, S3 store. Wave 2 (Jan-Feb): RN completion, bank channels, changes from signed FRS |
| Build Migration | Nov 2026 - Mar 2027 | Designed (DM0-DM3) | Migration console and loaders by 31 Mar 2027 |
| Build Drop 2 (wave 3) | Mar - Apr 2027 | Claims being built; EB, CSF (rest) designed | Claims, EB without portal, CSF, EGL / EDP / insurer adapters, other reports by 30 Apr 2027 |
| SIT Drop 1 | Jan - Jul 2027 | Test plans v1.0 for BRD-1 to BRD-13 | Test plans approved by 18 Dec 2026; SIT environment with S3, MSK, ElastiCache by 4 Jan 2027 |
| SIT Migration | Apr - Jul 2027 | Mocks defined relative to T in the design | Mock 1 Apr-May 2027 (reference, clients, headers); Mock 2 Jun-Jul 2027 (all objects, L1-L5) |
| SIT Drop 2 | Jul - Sep 2027 | - | Drop 2 test plans approved by 30 Jun 2027 |
| UAT Drop 1 (end to end) | Aug - Dec 2027 | UAT readiness programme (deliverables README) | UAT readiness statement by 30 Jul 2027; UAT plans and sign-off forms by 16 Jul 2027; UAT runs on migrated, masked data (Mock 3) |
| UAT Migration | Aug - Oct 2027 | - | Mock 3 = UAT load in Aug 2027; Mock 4 re-load in Oct 2027 after fixes |
| UAT Drop 2 | Oct - Nov 2027 | - | Readiness statement for Drop 2 by 30 Sep 2027 |
| Performance / penetration test | Nov - Dec 2027 | Performance plan (item 28); peak 429 concurrent until DCR-166 | Pre-Prod ready by 1 Oct 2027; an early security scan in SIT (Jun 2027) proposed so findings do not arrive in December |
| Full migration and cut-over | Nov 2027 - Jan 2028 | Cutover runbook v1.0 (T-30 to T+30) | Dress rehearsal on Pre-Prod in Nov - Dec 2027; go / no-go on T-1 |
| ORR / PRR | Dec 2027 - Jan 2028 | Runbook, observability, alerts, error catalogue (items 24, 35, 38) | Evidence pack by 15 Dec 2027 |
| Go-live | January 2028 (answer A1: all modules together) | - | T to be fixed (IQ01): proposed Mon 3 Jan 2028, legacy freeze after the EOD of Wed 29 Dec 2027 |

### 4.2 Go-live date and the migration calendar

- DATA_MIGRATION_DESIGN 17.2 defines T as the first business day of a month after a legacy month-end close. In January 2028 that is **Mon 3-Jan-2028** (1-Jan is a Saturday; 30-Dec Rizal Day, 31-Dec and 1-Jan non-working): freeze after the EOD of Wed 29-Dec-2027. A later January date needs a January stub period (DMQ18, DMQ25). IQ01, DCR-217.
- The design's relative mock calendar (Mock 1 at T-12 weeks, Mock 2 T-9, Mock 3 T-6, dress rehearsal T-3) is compressed against BDOI's longer plan. Proposed absolute calendar: Mock 1 Apr-May 2027 (SIT), Mock 2 Jun-Jul 2027 (SIT), Mock 3 = UAT load Aug 2027, re-load Oct 2027, dress rehearsal Nov-Dec 2027 on Pre-Prod, production load T-2 days. Milestones M3-M6 of `dm_layouts.yaml` (relative to T) should get these dates.
- The example dates in `dm_layouts.yaml` (as-of 2027-02-26, `F01C_EBIX_20270226_01.csv`) imply a March 2027 go-live; change them to December 2027 values at the next issue.
- Performance and penetration tests run after most of UAT (Nov-Dec 2027); propose an earlier security scan in SIT (Jun 2027). IQ35.

## 5. Integration inventory

| ID | Integration | Drop | BDOI meaning | Our understanding (to confirm) | Direction | Data | BIBS module | Existing seam (code) | Fit | Build needed | Open | IQ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| INT-01 | EIAM (Entra ID) | Drop 0 | Enterprise Identity Access Management, on Microsoft Entra ID | Single sign-on of BDO users. BIBS redirects to Entra ID (OpenID Connect, authorisation code with PKCE) and maps the returned identity to the BIBS user; roles stay in BIBS. | Both (browser redirect; tokens to BIBS) | User identity (UPN or Windows ID, name, e-mail); optional group claims | security; Setup & Administration > Users | security.service.directory.DirectoryAuthenticator (password check, LOCAL default; AUTH_MODE); SsoTokenExchange designed, not built (USER_ACCESS_DESIGN section 10) | CHANGE | OIDC log-in (frontend redirect, backend token validation with Spring Security OAuth2), user mapping by sec_user.windows_id or UPN, log-out to Entra, local break-glass administrator; lockout and password rules then sit in Entra ID | Protocol and claims, MFA and conditional access, session life against the 30-minute inactivity rule, non-BDO users | IQ04 |
| INT-02 | UIDM-ISC (IGA) | Drop 0 | User ID Maintenance - Identity Security Cloud, the identity governance and administration (IGA) tool | Joiner, mover and leaver provisioning and access certification from the IGA. We read "Identity Security Cloud" as the SailPoint product; to confirm. | Inbound (provisioning) and outbound (account and entitlement aggregation) | User accounts, enable / disable, group profiles (BIBS roles) as entitlements | security, nbadmin (BRD-11 access requests) | None for provisioning. nbadmin request lifecycle and UserAdminService apply changes; ExternalUserProvisioner serves portal users only | CHANGE | Provisioning API (SCIM 2.0 Users / Groups, or the connector ISC offers), aggregation export, change-log rows with the ISC request number; decide which BRD-11 request types remain in BIBS | Who owns requests and approvals, connector type, entitlement level, certification | IQ05 |
| INT-03 | LMS | Drop 0 | Loans Management System | Bank loan data used by Renewal (paid-off and RMU loans, BRRN.029), Submitted Policies (loan snapshot, PN) and CLPC billing. Probably the source of the "LAMD reports". | Inbound | PN number, loan status (paid-off, RMU), loan release and amortisation for CLPC | renewal (designed), submitted (designed), placement (CLPC reports), cashiering (PAY_CLPC) | Bulk handlers RNW_LAMD_REPORT and SBM LAMD upload (designed); PaymentReportService / NB-CLPC-BILLING; PAY_CLPC | CHANGE | Layouts per report; automated transfer if LMS delivers files (SFTP drop to S3) instead of user uploads | Which LMS extracts, layouts, frequency, transport | IQ06 |
| INT-04 | HL-LOAS (HLS / LOAS) | Drop 0 (HLS) and Drop 2 (LOAS) | Home Loan System (Loan Origination and Admin) | One system shown twice on the slide. Origination side sends home-insurance quotation requests (BRNB.023) and the daily HLS insurance report (Submitted Policies, CBG Fire); admin side gives the HLS amortised loan data for CLPC billing. | Inbound | Quotation requests (borrower, property, sum insured, PN); HLS insurance report; amortised loan release | quotation, submitted (designed), placement (CLPC) | quotation.service.QuotationRequestSource with job QUOTATION_REQUEST_INTAKE (cron off) and bulk QUOTATION_REQUEST; NB-CLPC-BILLING variants | CHANGE | QuotationRequestSource adapter (file or REST) with source authentication; SP intake handler; CLPC layout | One interface or two, formats, frequency, transport | IQ07 |
| INT-05 | LFS SaaS | Not on the slide | Loan Front-End System (SaaS) | Source of the LFS insurance report for Submitted Policies (BRD-12 p.4-5). | Inbound | Loan-linked policies submitted by borrowers | submitted (designed) | Submitted-policy intake handlers (SUBMITTED_POLICIES_DESIGN); Q11 / SP-SQ01 | NEW | Intake layout and transfer; part of the SP build | Drop, layout, transport | IQ08 |
| INT-06 | PMS | Drop 0 | PDC Management System | Holds the post-dated checks in the warehouse. BIBS Cashiering receives the daily PDC list and matures PDCs on their date (CSHID.008 item 4). | Inbound; outbound status to confirm | PDC number, bank, date, amount, payor, account / invoice reference; possibly deposit, return and pull-out status | cashiering | Payment file handler PAY_PDC (layout table, PdcFileHandler); job PDC_MATURITY | CONFIGURE | Layout in the payment-file layout table; an automated transfer if PMS drops files; outbound status feed if required | Layout (OQ03), direction, frequency | IQ09 |
| INT-07 | CMS / New BOB | Drop 0 | Cash Management System, outward payments (also called New BOB) | Bank channel for BDOI's outgoing payments (insurer remittances, refunds, supplier and commission payments) from Disbursement; the bank returns the debit status. | Outbound (payment instructions); inbound (status, debit confirmation) | Payee, account, amount, currency, value date, voucher reference; status per transaction | disbursement | Payment instruments and EOD outputs (dsb_eod_output); DCTF file; upload DISB_BOB_APPROVED (BobApprovedHandler); BankChannelPort named in the design, not built (AQ09) | CHANGE | Outward payment file in the CMS layout (or host-to-host), status upload or pull, reconciliation to vouchers | Layout, host-to-host or upload, cut-off times, approvals in CMS | IQ10 |
| INT-08 | OBPCS | Drop 0 | Online Bills Payment Consolidation System | The consolidated bills-payment collections file (today "Bills Payment FS01") that Cashiering uploads and automatches. | Inbound | Payment date, reference (ARN, invoice, PN), payor, amount, channel | cashiering | Payment file handler PAY_BILLS (configurable layout); PAYMENT_AUTOMATCH job | CONFIGURE | Layout; automated transfer if OBPCS drops files | Layout, frequency, transport | IQ10 |
| INT-09 | Old BOB | Drop 0 | Old Business Online Banking Collection System; SOA bills payments via funds transfer | Corporate clients pay statements of account by funds transfer; the collection file is applied in Cashiering. | Inbound | Funds-transfer credits with SOA or invoice reference | cashiering (Direct Credit or a new layout) | Payment file handler PAY_DIRECT_CREDIT or a new layout row | CONFIGURE | Layout; SOA-to-invoice matching rule if the file carries the SOA number only | Layout, SOA reference, frequency | IQ10 |
| INT-10 | AFTS | Drop 0 | Automatic Fund Transfer System (on the slide; not explained in the answers) | Unknown. Possibly auto-debit of client accounts for premium, or automatic transfers for disbursement. | To confirm | To confirm | cashiering or disbursement | Payment file handlers; disbursement instruments | CHANGE | To size after the answer | Purpose, direction, layout | IQ10 |
| INT-11 | ICBS | Drop 0 (struck through) | Struck through on the slide | Out of scope (core banking system replaced by the channels above). | - | - | - | - | OUT | None | Confirm out of scope | IQ10 |
| INT-12 | ECM | Drop 0 | Enterprise Content Management | Answer A4 places every BIBS document and attachment in Amazon S3, not in ECM. A remaining ECM role is not stated (source of legacy documents for the archive, or records transfer at end of retention). | To confirm (possibly inbound legacy documents only) | Legacy documents (H02) if ECM holds them | attachment; migration archive (designed) | migration ArchiveDocumentStore (designed) | OUT | None for document storage (S3); archive transfer if ECM holds legacy documents | Any ECM touch-point left | IQ13 |
| INT-13 | CCM | Drop 0 | Centralized Communications Management, e-mail sending | All outbound BIBS e-mail (renewal advices, protected PDFs, notifications, job-failure mails) goes through CCM. | Outbound; delivery status inbound | Recipient, subject, body, attachments (password-protected PDFs), template id | messaging | messaging.service.MailTransport (SmtpMailTransport, SimulatedMailTransport); outbox msg_outbound_message; MAIL_DISPATCH job; DocumentProtector | CONFIGURE | SMTP relay: configuration only. API: a new MailTransport adapter with status callback | SMTP or API, attachment limits, volume, bounce feedback, sender domains | IQ11 |
| INT-14 | M365 Exchange | Drop 0 | M365 Exchange | Mailboxes. Candidate uses are reading e-mailed quotation requests (BRNB.041, Q12 parked) and insurer replies, or relaying mail if CCM does not cover a case. | Inbound (mailbox reading) and/or outbound | E-mails and attachments | quotation (request intake), messaging | None for mailbox reading (Q12); MailTransport for sending | NEW | Mailbox reader through Microsoft Graph with an application registration, if confirmed | Role against CCM, mailboxes, permissions | IQ12 |
| INT-15 | TFS (manual) | Drop 0 | Trade Finance System - client onboarding and payment (manual) | Trade (CIB) clients and their payments; no system interface. Users key the client in BIBS and upload the Trade payment file. | Manual inbound | Trade client details; Trade payment file | crm, cashiering | Client onboarding screens; payment file handler PAY_TRADE | FIT | None (layout confirmation only) | Which onboarding data, layout of the Trade file | IQ14 |
| INT-16 | EDP - Data Ingestion (SD 11) | Drop 2 | Enterprise Data Platform | BIBS data delivered to the BDO data platform for analytics. "SD 11" is not explained (a data standard or source-delivery specification). | Outbound | To agree (clients, accounts, invoices, receipts, remittances, GL; history and daily changes) | platform (events, report) | Kafka integration events bibs.* (9 topics, PLATFORM_CACHE_AND_EVENTS.md); report exports; no bulk data feed | NEW | Daily extracts to an S3 drop per the SD 11 specification, or change-data capture from a read replica; masking rules | What SD 11 is, objects, format, frequency, push or pull | IQ15 |
| INT-17 | EGL | Drop 2 | Enterprise General Ledger | The BDO group ledger receives BDOI accounting from the BIBS GL (journal detail or balances) for consolidation. | Outbound | Journals or trial balance by account, branch, cost centre, currency; mapping to EGL accounts | journal, ledger, coa, finreport | None as an interface; GL reports (trial balance, GL-SCHEDULE); BDO GARD submission is report exports only (AQ31) | NEW | EGL extract job with the account mapping (code map), control totals and reconciliation report | Level of detail, frequency, mapping, layout, transport | IQ16 |
| INT-18 | CARMS | Drop 2 | Not explained in the answers | Unknown. | To confirm | To confirm | To confirm | - | NEW | To size after the answer | Purpose, data, direction | IQ17 |
| INT-19 | Insurer System | Drop 2 | Insurer systems (not detailed) | Files exchanged with each insurer today by e-mail: production reports and feedback (PRCID.009), remittance schedules and ORs (RMTID.012), DP billing responses (CMRID.009), renewal responses, SOAs (ACSL 2.2.1), e-policies, claims. | Both | Per process | prodrecon, remittance, commission, renewal, acsl, issuance, brokerclaims | opsledger.service.port.InsurerFileInbox (default: no inbox, uploads on screens); FileDropPort for outgoing extracts | CHANGE | InsurerFileInbox adapter per channel (SFTP to S3, API) and outbound drops | Insurers, processes, channel, layouts | IQ18 |
| INT-20 | Bridger Insight XG (manual) | Drop 2 | Bridger Insight XG (screening) | Compliance screens names in Bridger Insight XG by hand and records the result in the BIBS screening case. | Manual | Screening result and evidence | screening | Screening cases with evidence attachments; WatchlistFeed for list files (StagedFileWatchlistFeed) | CONFIGURE | Evidence document type and disposition values; a client export for Bridger batch screening if asked | Does Bridger replace or complement BIBS list matching | IQ19 |

Code references for the seams:
- `backend/src/main/java/com/iortatechnxt/brokerverse/security/service/directory/DirectoryAuthenticator.java` (password check; AUTH_MODE LOCAL / DIRECTORY). An OIDC redirect does not fit this port: EIAM needs a new login flow (Spring Security OAuth2 client or a token exchange `SsoTokenExchange`, USER_ACCESS_DESIGN section 10).
- `nbadmin/service/ExternalUserProvisioner.java` (portal users only; no internal provisioning port for UIDM-ISC).
- `opsledger/service/port/DisbursementGateway.java` (Operations to Disbursement, in-app); `disbursement/service/BobApprovedHandler.java` (`DISB_BOB_APPROVED` upload, "manual seam until a BOB interface exists (BankChannelPort, AQ09)"); `BankChannelPort` exists only in ACCOUNTING_DISBURSEMENT_DESIGN section 13, not in code; DCTF files in `disbursement/service/EodService.java`, `CtaCreditedHandler.java`.
- `cashiering/service/PaymentFileHandlers.java`, `PaymentFileLayouts.java`, `PdcFileHandler.java` (handlers `PAY_BILLS`, `PAY_TRADE`, `PAY_CLPC`, `PAY_PDC`, `PAY_DIRECT_CREDIT`, layout table `PaymentFileLayout`).
- `quotation/service/QuotationRequestSource.java` (HLS; job `QUOTATION_REQUEST_INTAKE`, cron `-` in `application.yml`).
- `messaging/service/MailTransport.java` (`SmtpMailTransport`, `SimulatedMailTransport`; `brokerverse.mail.enabled`, `spring.mail.*`).
- `attachment/domain/AttachmentContent.java` (`doc_attachment_content` bytea) and 12 more bytea tables (section 6.3).
- `opsledger/service/port/InsurerFileInbox.java`, `FileDropPort.java`, `CollectionFeed.java`, `MarketingFeed.java`, `ClaimsFeed.java`.
- `screening/watchlist/service/WatchlistFeed.java` (`StagedFileWatchlistFeed`).
- `productmaint/service/ProductMasterFeed.java` (logs only; PQ16).
- `integration/service/IntegrationTopics.java` (9 topics `bibs.*.v1`), PLATFORM_CACHE_AND_EVENTS.md section 3.
- GL out: no interface exists (journal / ledger / finreport reports only).

### 5.1 Interfaces named in the BRDs but not on the slide

| Interface | Named in | BIBS seam | To confirm |
|---|---|---|---|
| LAMD reports (paid-off, RMU) | BRD-6 BRRN.029; BRD-12 | RNW_LAMD_REPORT and SBM uploads (designed) | Covered by LMS (INT-03)? |
| CLPC loan credits and billing | BRD-1 BRNB.067; Report List #65-67 | PAY_CLPC, NB-CLPC-BILLING | Covered by LMS / HL-LOAS? |
| NLDS PEP list and AML advisory | BRD-10 SNSRP-201, p.7 | WatchlistFeed sources NLDS_PEP, AML_ADVISORY | Transport (SQ01) |
| Mail house (printed letters) | BRD-12; BRD-6 letters | MailHouseGateway (designed) | Channel and layout |
| BIR eFPS / eBIRForms, CAS books | BRD-5 Appendix A VII | Export files per form | Manual upload by Comptrollership |
| BDO Unibank GARD submission | BRD-5 Appendix A II | Report exports | AQ31; relation to EGL |
| Legacy EBIX, QPS, ISYS, Collection Management System (migration sources) | BRD-13 | Migration intake (ExtractInbox, designed) | Extract transport (DMQ28) |

## 6. Infrastructure alignment

### 6.1 IER facts (S3)

- Assumptions: non-production 12 x 5 (260.7 h / month), production 24 x 7, AWS enterprise discount 13 % (SUMMARY C26; the Assumptions sheet says 10.5 %), VAT 12 %, cost buffer 25 %, USD 1 = PHP 60, growth 15 % a year, 60 months.
- RPO 15 minutes, RTO 4 hours (both diagram sheets).
- HW & SW Requirements: RDS PostgreSQL 16 ("RHEL 9.x + PostgreSQL 16 (Amazon RDS, Single-AZ)" for DEV / SIT / UAT at 4 / 16 / 200, 4 / 16 / 200, 8 / 32 / 500; Multi-AZ for Pre-Prod 8 / 64 / 1,000 and PROD 16 / 128 / 2,000); ElastiCache Redis 7 + MSK Kafka 3.6 (Pre-Prod, PROD, DR "(cluster)", 3 brokers); bastion / CI-CD runner "RHEL 9.x + GitLab Runner, AWS SSM, kubectl / helm" 4 / 16 / 200; PROD DR "PostgreSQL 16 (cross-region read replica)" 16 / 128 / 2,000 with RPO <= 15 min; S3 (document store) + EFS (shared K8s PV) with lifecycle to S3-IA / Glacier.
- VDI: 15 users - 8 developers (6 concurrent, 24 months), 4 testers (3, 18 months), 3 production support (2, 60 months); software Visual Studio Code, **OpenJDK Temurin 17 ("BrokerVerse services build on JDK 17")**, Node.js 20 ("Angular SPA build"), Git ("Connects to GitLab"), DBeaver, Postman, Docker / kubectl / Helm, Katalon / Selenium, JMeter 5.6, Xray / TestRail, Dynatrace, Lens; VDI licence USD 13 / user / month.
- Environment sheets DEV, SIT, UAT, Pre-Prod, PROD, DR: template rows only (Windows Server / RHEL / Ubuntu / Oracle Linux with C:\ or / drives, platform AWS / EXACC / ON PREM, CPU and memory 0 except one PROD "App Server" 4 / 16 Windows); the Pre-Prod sheet is titled "UAT ENVIRONMENT"; DR runs 12 x 7. SUMMARY totals (PHP 3.55 M 5-year incl. buffer) are therefore not a costing of the declared components (RDS, MSK, ElastiCache, EKS nodes are not costed).
- Kubernetes sheets (DEV, SIT, UAT, PROD Y1-Y5; no Pre-Prod, no DR): kube-system add-ons, ingress-nginx, Dynatrace and CrowdStrike (UAT, PROD), and ten services in `brokerverse-core` (bv-policy-admin-svc, bv-claims-svc, bv-billing-collections-svc, bv-distribution-svc, bv-web-portal) and `brokerverse-platform` (bv-api-gateway, bv-iam-svc, bv-integration-svc, bv-document-svc, bv-batch-notification-svc); m5.4xlarge nodes; PROD max replicas 4 (Y1) to 8 (Y5); worker nodes 1-4 (Y1) to 2-8 (Y5). Formula error: "Total PV (GB) EFS" is `=E*H` (max replicas x CPU limit in millicores) instead of `=E*K`, giving 21,800 GB (DEV), 32,760 GB (UAT), 43,600-87,200 GB (PROD Y1-Y5); the real PV request is 20 GB for bv-document-svc.
- Diagrams: "Technical Architecture - Multi-Tenant" (Apigee X, CloudFront + WAF, Istio + OPA, EventBridge + Glue Schema Registry ~80 topics, per-tenant Aurora / Mongo / Redis / S3, "no cross-DB transactions", ArgoCD, Terraform + Crossplane) and "AWS EKS Architecture - System Overview" (Route 53, ALB, ECS router, GitHub + Jenkins, PostgreSQL primary + replica, Prometheus / Grafana / CloudWatch).

### 6.2 IER against BIBS

| Topic | IER (BDOI IT) | BIBS as built | Assessment | Change needed | Ref |
|---|---|---|---|---|---|
| Application architecture | Diagram sheet: multi-tenant microservices (identity, audit, workflow, document, notification, reference, tenant; client, policy, quotation, cashiering, billing, accounting, product; nb, sp, acc, clxn, rn, clm, ops, pm, eb, rl), Istio + OPA per pod, "no cross-DB transactions" | One Spring Boot 3.5 modular monolith (backend) and one React SPA served by nginx (frontend); modules isolated by ArchUnit; a business record, its journal, ledger and sub-ledger rows commit in one PostgreSQL transaction | Mismatch | Replace the diagram with the BIBS diagrams of this document; state the modular monolith in the IER | DCR-222; IQ25 |
| Tenancy | Three tenants: BDOI internal ~1,200 users, EB insurer portal ~50 partners, broker / RI network ~200 accounts; tenant_id + RLS per data layer | Single tenant (company_id, branch scope, role scope); no portal (EB portal dropped by answer); RI network is phase 2 | Mismatch | Remove tenants 2 and 3; size for internal users | DCR-222, DCR-232 |
| Kubernetes workloads | Ten services bv-policy-admin-svc, bv-claims-svc, bv-billing-collections-svc, bv-distribution-svc, bv-web-portal, bv-api-gateway, bv-iam-svc, bv-integration-svc, bv-document-svc, bv-batch-notification-svc in namespaces brokerverse-core / -platform | Two deployments: brokerverse-backend (2 replicas, 500m / 1 Gi requests, 2 CPU / 2 Gi limits) and brokerverse-frontend (2 replicas); namespace brokerverse (deploy/k8s/brokerverse.yaml) | Mismatch | Re-state the K8s sheets with the sizing of section 6.4 | DCR-223 |
| Database | HW sheet: "RHEL 9.x + PostgreSQL 16 (Amazon RDS)", Single-AZ DEV / SIT / UAT, Multi-AZ Pre-Prod / PROD; diagram: Aurora and MongoDB per tenant | PostgreSQL 16 with Flyway, JSONB where needed; Amazon RDS Multi-AZ in production (deploy notes); no MongoDB | Aligned (RDS); Mismatch (Aurora, Mongo, "RHEL 9") | Drop "RHEL 9.x" from RDS rows (managed engine); remove Mongo; RDS PostgreSQL 16 or Aurora PostgreSQL both work | DCR-224 |
| Cache | ElastiCache for Redis 7.x; "(cluster)" in Pre-Prod, PROD | Redis 7 with cluster mode disabled (one shard, primary + replica, TLS, AUTH); the cache clear uses SCAN and the job lock uses scripts on single keys | Mismatch (wording) | IER: cluster mode disabled, Multi-AZ replica. Code change only if cluster mode enabled is imposed | DCR-228; IQ34 |
| Event streaming | Amazon MSK (Kafka 3.6), 3 brokers in Pre-Prod / PROD / DR; diagram: MSK + EventBridge, Glue Schema Registry, ~80 topics | MSK Kafka 3.6+ with SASL/SCRAM + TLS, 9 topics bibs.* plus .dlt, transactional outbox; no EventBridge, no schema registry | Aligned (MSK); Mismatch (EventBridge, 80 topics) | IER: 3 brokers kafka.m5.large class, 9 topics x 3 partitions; DEV / SIT with 2 brokers need BROKERVERSE_KAFKA_REPLICATION_FACTOR=2 | IQ25 |
| Documents and files | Amazon S3 (document store) + Amazon EFS (shared K8s PV); K8s sheets: "Total PV (GB) EFS" of 21,800 GB (DEV), 32,760 GB (UAT) and 43,600 to 87,200 GB (PROD Y1 to Y5) | File content in PostgreSQL bytea in 13 tables (doc_attachment_content and others); no volume on the pods | Mismatch | Answer A4 and DOCUMENT_STORAGE_DECISION.md (option C, steps ST0 / ST1): S3 for bytes, PostgreSQL for metadata; no EFS for BIBS. The EFS totals are a formula error (Total PV = max replicas x CPU limit) | DCR-227, DCR-223; IQ31 |
| Edge and ingress | Diagram: CloudFront + WAF, Apigee X (OIDC validation, quotas), Istio ingress mTLS; infra diagram: Route 53 + ALB; K8s sheets: ingress-nginx | Kubernetes Ingress to the nginx frontend, /api proxied to the backend; JWT issued by BIBS; login rate limit in the application | Gap | Agree one edge: Route 53 + WAF + ALB (AWS Load Balancer Controller) or ingress-nginx; Apigee only if BDO mandates it for internal APIs | IQ25 |
| Runtime and images | VDI: OpenJDK Temurin 17 ("BrokerVerse services build on JDK 17"), Node 20, Angular SPA | Java 21 (Temurin 21 JRE Alpine image), Spring Boot 3.5, Node 22, React 19, Vite 8 | Mismatch | VDI list: Temurin 21, Maven 3.9, Node 22 LTS; no Angular. Base images RHEL UBI 9 only if BDO requires | DCR-224; IQ28, IQ29 |
| CI / CD | HW sheet: GitLab Runner, AWS SSM, kubectl / helm on a bastion; infra diagram: a second CI toolchain named in the IER; diagram: ArgoCD GitOps + Helm, Terraform + Crossplane | the CI pipeline (.gitlab-ci.yml: mvn verify, SonarQube, npm verify); plain Kubernetes manifests; no Helm chart | Gap | Helm chart (or Kustomize overlays) per environment; pipeline on the BDO toolchain once named | IQ29 |
| Observability | VDI and K8s sheets: Dynatrace (ActiveGate, OneAgent); diagram: CloudWatch, X-Ray, Prometheus, Node Exporter, Loki, Grafana | Actuator health, metrics and /actuator/prometheus; structured logs with CR/LF neutralised; alert catalogue planned (deliverable 38) | Gap | Choose Dynatrace or Prometheus / Grafana; ship logs to the BDO log platform | IQ30 |
| Recovery objectives | RPO 15 minutes, RTO 4 hours (both diagram sheets); DR standby with async replication, RPO <= 15 min | Register proposal DCR-135: RPO 15 min (continuous WAL archiving), RTO 4 h; RUNBOOK: daily full backup + WAL archiving, quarterly restore test | Aligned | Record the IER as the BDOI answer to DCR-135 (to confirm by the BRD owners) | DCR-135, DCR-225 |
| Disaster recovery | PROD DR: "cross-region read replica" (HW sheet); diagram: "ap-southeast-1 primary + DR warm-standby"; DR environment at 12 x 7 hours | Hosting appendix: AWS ap-southeast-1; access restricted to personnel in the Philippines | Gap | Name the DR region and confirm data residency; S3 cross-region replication and MSK / ElastiCache rebuild in DR; DR runbook | DCR-225; IQ26 |
| Environments | DEV, SIT, UAT, Pre-Prod, PROD, DR; K8s sheets only for DEV, SIT, UAT, PROD Y1-Y5; the Pre-Prod sheet is titled "UAT ENVIRONMENT"; environment cost sheets hold template rows (Windows Server, EXACC, ON PREM, 0 CPU) | Needs all six; Pre-Prod for the dress rehearsal and performance tests; DR for the failover test | Gap | Add Pre-Prod and DR cluster sheets; replace template rows; re-cost | DCR-223; IQ33 |
| Operating hours | Non-production 12 x 5 (260.7 h / month); PROD 24 x 7; DR 12 x 7 | Night jobs in PHT: EOD booking and remittance extraction 20:00, Collections 22:15-23:00, renewal extraction 01:00, screening 01:00-01:30, application file 05:00, alerts 06:00 | Mismatch | Non-production crons shifted into the window by environment variables; extended hours for batch, month-end and migration rehearsal cycles; MSK and ElastiCache run 24 x 7 anyway | DCR-226; IQ27 |
| Sizing basis | YoY growth 15 %, 60 months; ~1,200 internal users (diagram) | Umbrella BRD p.42: 1,344 named / 429 concurrent (DCR-166); renewal 25,800 accounts and NB 21,200 bookings a month | Gap | One user figure for sizing and the performance test | DCR-232; IQ32 |
| VDI | 15 users: 8 developers (24 months), 4 testers (18), 3 production support (60); 12 x 5 | Hosting appendix: access restricted to personnel in the Philippines; migration staging purged within 5 days | Gap | Confirm who needs VDI and from where; update the software list | DCR-231; IQ28 |

**Architecture option (IQ25, DCR-222, DCR-223).** The recommendation is recorded in
[`ARCHITECTURE_OPTION_DECISION.md`](ARCHITECTURE_OPTION_DECISION.md): BIBS stays a modular monolith and takes three
enterprise elements from the IER (edge security, managed AWS services, separate web / jobs / integration workloads);
awaiting BDOI confirmation. The register carries it as the proposed resolution of DCR-222 and DCR-223.

BIBS references: `deploy/k8s/brokerverse.yaml` (namespace, two deployments, 2 replicas each, backend 500m / 1 Gi requests and 2 / 2 Gi limits, AWS notes for RDS Multi-AZ, ElastiCache cluster mode disabled, MSK SASL/SCRAM port 9096); `backend/Dockerfile` (`eclipse-temurin:21-jre-alpine`), `frontend/Dockerfile` (`node:22-alpine`, `nginx:1.27-alpine`); `backend/src/main/resources/application.yml` (Redis, Kafka, job crons in UTC); `.github/workflows/ci.yml` (Java 21, Node 22, SonarQube); `docs/operations/RUNBOOK.md` section 6 (daily full backup + WAL, quarterly restore test); `docs/architecture/PLATFORM_CACHE_AND_EVENTS.md`.

### 6.3 Documents in S3 only (A4; design S6)

| Area | Today | Change |
|---|---|---|
| Storage port and metadata (ST0) | Each module holds file bytes in its own bytea column | FileStore port in common/storage (S3FileStore, LocalFileStore, MinIO test); shared table stored_file (owner, document type, name, size, SHA-256, bucket, key, version id, retention class, legal hold); module tables get stored_file_id |
| Attachments (all modules) | doc_attachment_content.content bytea (V21, AttachmentService) | Bytes in bibs-<env>-documents; the attachment record points to stored_file (ST1) |
| Generated business documents | plc_slip_file, iss_insurance_advice, bkg_service_invoice, rem_batch_document, clx_billing_document, csh_print_batch, dsb_eod_output (bytea) | Written through FileStore to the documents bucket (ST1); doc_rendition keeps its spec and SHA-256 |
| Report archive and batch files | report_run_file, report_batch (bytea); ReportArchiveService | bibs-<env>-reports, expiry per report archive setting (default 400 days); users get a link |
| Operations extracts, e-mail attachments | ops_extract_file (FileDropPort), msg_outbound_attachment | FileDropPort writes to S3; e-mail attachments and ZIP bundles are read from S3 and streamed (option B path) |
| Uploads and inbound files | bulk_job keeps only the file name; iss_upload_item bytea; watchlist files as attachments | bibs-<env>-inbound: quarantine prefix until the malware scan tags the object clean; kept 90 days; large files by presigned PUT |
| Migration files (designed) | mig_extract files; staging purge within 5 days | bibs-<env>-migration, expiry after 5 days, migration role only |
| Access and audit | Backend streams the bytes after the permission check | Presigned GET valid 5 minutes (FILE_LINK_TTL_SECONDS), issued after the permission check, each issue audited; Content-Disposition attachment, no-store |
| Security | Database encryption at rest | SSE-KMS with a customer-managed key per environment; deny non-TLS and unencrypted puts; IRSA role and S3 gateway endpoint; keys without personal data |
| Retention, legal hold, DR | Files follow the database backup | Versioning; lifecycle Standard, Standard-IA after 90 days, Glacier Instant Retrieval after 1 year; Object Lock governance mode with legal hold; cross-region replication with replication time control |
| Existing content | - | One-off copy job per table with SHA-256 check, switch reads, drop the bytea column later (ST1) |
| IER: new rows | IER has "Amazon S3 (document store)" with no size | Four buckets per environment; storage estimate per year (to size from the document volumes); KMS keys per environment; S3 gateway endpoint; replication to the DR region; GuardDuty Malware Protection for S3 (or the BDO scanner); Object Lock enabled at bucket creation |
| IER: rows to remove | Amazon EFS (shared K8s PV); efs-csi-controller pods; "Total PV (GB) EFS" column | Not used by BIBS for documents; pods are stateless |
| IER: database size | PROD RDS 2,000 GB, sized with documents in mind | Re-size RDS without file content once the volumes are known; backups and cross-region replica get smaller |

Note for S6: section 1 of DOCUMENT_STORAGE_DECISION.md lists `doc_rendition` among the bytea tables; `docgen/domain/DocRendition.java` stores the document spec and SHA-256 only (the file is regenerated), so it needs no move.

### 6.4 Kubernetes sizing proposal (replaces the bv-* rows)

| Environment | Deployment | Min | Max | Requests CPU / memory | Limits CPU / memory | Note |
|---|---|---|---|---|---|---|
| DEV | brokerverse-backend | 1 | 2 | 500m / 1.5 Gi | 2 / 3 Gi | Seed data; jobs run on the single pod |
| DEV | brokerverse-frontend | 1 | 2 | 50m / 64 Mi | 500m / 256 Mi | nginx static files and /api proxy |
| SIT | brokerverse-backend | 2 | 3 | 500m / 1.5 Gi | 2 / 3 Gi | Two pods to test the Redis job lock and cache eviction across pods |
| SIT | brokerverse-frontend | 2 | 2 | 50m / 64 Mi | 500m / 256 Mi | - |
| UAT | brokerverse-backend | 2 | 4 | 1 / 2 Gi | 2 / 4 Gi | Migration mock loads run here (Mock 3) |
| UAT | brokerverse-frontend | 2 | 3 | 50m / 64 Mi | 500m / 256 Mi | - |
| Pre-Prod | brokerverse-backend | 3 | 6 | 1 / 2 Gi | 2 / 4 Gi | Production-sized; performance test and dress rehearsal |
| Pre-Prod | brokerverse-frontend | 2 | 4 | 50m / 64 Mi | 500m / 256 Mi | - |
| PROD (Y1) | brokerverse-backend | 3 | 6 | 1 / 2 Gi | 2 / 4 Gi | HPA on CPU; PodDisruptionBudget minAvailable 2; spread over 3 AZs |
| PROD (Y1) | brokerverse-frontend | 2 | 4 | 50m / 64 Mi | 500m / 256 Mi | PDB minAvailable 1 |
| PROD (Y5) | brokerverse-backend | 4 | 8 | 1 / 2 Gi | 2 / 4 Gi | 15 % growth a year; to be proved by the performance test |
| DR | brokerverse-backend | 0 | 6 | 1 / 2 Gi | 2 / 4 Gi | Scaled up at failover (warm standby); images and configuration kept current |

### 6.5 Hours, batch window and DR

- Night jobs (application.yml, UTC -> PHT): `booking-batch-cron` and `remittance-extraction-cron` 20:00, `acsl-gl-sl-recon-cron` 20:00, `clx-daily-refresh-cron` 22:15, `clx-daily-files-cron` 22:30, `clx-escalation-cron` 23:00, `broking-books-close-cron` 23:00 on the last day, `package-expiry-cron` and `scr-watchlist-ingest-cron` 01:00, `clx-application-file-cron` 05:00, `bcl-*` 05:30-06:00. In 12 x 5 non-production most of them never fire. Every cron is an environment variable (`BROKERVERSE_JOB_*_CRON`), so non-production sets them inside the window; batch and month-end cycles, weekend migration mocks and the dress rehearsal need extended hours. MSK and ElastiCache bill 24 x 7 whatever the hours. IQ27, DCR-226.
- DR: RDS cross-region replica (RPO seconds to minutes), S3 CRR with replication time control, EKS standby scaled at failover. Redis is rebuilt (caches refill; the token denylist is lost, so logged-out tokens are valid until expiry, at most `BROKERVERSE_TOKEN_VALIDITY` = 8 h); MSK is recreated (topics created at start-up by `KafkaAdmin`, `BROKERVERSE_KAFKA_CREATE_TOPICS`); undelivered events are resent from `evt_outbox`. Region and data residency open (IQ26, DCR-225). The IER values RPO 15 min / RTO 4 h equal the DCR-135 proposal.
- Kafka replication factor: `application.yml` defaults to 1, the k8s manifest sets 3; DEV / SIT MSK with 2 brokers need 2.

### 6.6 Changes needed in code, configuration and documents

- **Code (build items):** ST0 / ST1 of S6 (FileStore, `stored_file`, module move, one-off copy, AWS SDK v2 dependency - none in `backend/pom.xml` today); EIAM OIDC sign-in (security, frontend login); UIDM-ISC provisioning API (SCIM 2.0 or connector) and aggregation export; bank channel layouts and the CMS / New BOB outward file; HL-LOAS `QuotationRequestSource` adapter; EGL extract; EDP extracts per SD 11; insurer `InsurerFileInbox` adapters; `PACKAGE_MAPPING` renewal check; EB without `portal`.
- **Configuration / deployment:** Helm chart or Kustomize overlays per environment (replicas, HPA, PDB, topology spread, IRSA service account, S3 settings); non-production cron overrides; `BROKERVERSE_KAFKA_REPLICATION_FACTOR=2` on 2-broker MSK; backend memory limit 4 Gi in UAT / Pre-Prod / PROD for report generation; image base per BDO standard (IQ29).
- **IER (BDOI IT):** replace both diagram sheets with the A6 PNGs; restate K8s sheets (6.4) and add Pre-Prod and DR sheets; fix the EFS formula or drop the column; remove EFS and efs-csi-controller; add S3 buckets (4 per environment), KMS keys, S3 gateway endpoint, CRR, GuardDuty Malware Protection for S3 (or BDO scanner), Object Lock at bucket creation; "RHEL 9.x" off the RDS rows; ElastiCache "cluster mode disabled"; VDI software list Temurin 21 / Maven 3.9 / Node 22, no Angular; retitle the Pre-Prod sheet; replace template rows of the environment sheets and re-cost; reconcile the enterprise discount (10.5 % vs 13 %).

## 7. Register items

DCR-210 to DCR-235 are proposed for the discrepancy register (drop plan, concept paper, IER, integrations).

## 8. Edits needed to shared documents

Status 26-Sep-2026: applied in the consolidation of this date (register v1.2, BRD-6 FRS and test plan v1.1, BRD-2 and
BRD-11 FRS v1.1, the designs named below), except the Employee Benefits rows, which wait for the Employee Benefits build team
(`docs/deliverables/PENDING_EDITS.md`), and the Developer Guide FileStore rule, which follows build step ST0.

| Document | Edit |
|---|---|
| `docs/source-documents/BDOI_DROP_PLAN.md` | Requirement bars: Drop 1 Sep-Nov 2026, Drop 2 Dec 2026-Feb 2027 (as on the slide) |
| `docs/deliverables/src/registers/discrepancy_register.yaml` and `build_discrepancy_register.py` | Add DCR-210 to DCR-235; mark DCR-135 answered by the IER; add the QUESTION_SOURCES line for section 9 of this document; add a Drop column |
| `docs/deliverables/README.md` | New item: programme alignment and integration inventory (v1.0); status of items 4, 12 and 26 fed by this document; drop folders (A5); UAT readiness dates (30-Jul-2027 Drop 1, 30-Sep-2027 Drop 2); item 28 notes the IER sizing |
| `docs/architecture/DATA_MIGRATION_DESIGN.md` section 17.2 and `dm_layouts.yaml` milestones / examples | Absolute calendar (4.2); PACKAGE code map set; example dates for a January 2028 T |
| `docs/architecture/RENEWAL_DESIGN.md` | `PACKAGE_MAPPING` check; note that the early release is withdrawn |
| `docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md`, FRS BRD-8, test plan BRD-8 | Remove the portal (if IQ22 confirms) |
| `docs/architecture/USER_ACCESS_DESIGN.md`, FRS BRD-11 | EIAM (OIDC) instead of EUA password bind; UIDM-ISC provisioning and which request types remain |
| `docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md` section 13 | CMS / New BOB as the BOB / BankChannelPort target; EGL interface |
| `docs/architecture/OPERATIONS_DESIGN.md`, FRS BRD-2 | OBPCS, Old BOB, PMS, TFS names on the payment-file handlers; reinsurance transactions in Remittance (after IQ20) |
| `docs/architecture/DOCUMENT_STORAGE_DECISION.md` | `doc_rendition` holds no bytes (6.3) |
| `docs/architecture/ARCHITECTURE.md` | Deployment view pointing to section 6 and the IER diagrams |
| `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` | Glossary: CMS (Cash Management System) vs Collection Management System |
| `docs/development/DEVELOPER_GUIDE.md` | FileStore rule (no bytea for files) once ST0 lands |

## 9. Open questions for BDOI (programme, integrations, infrastructure)

| ID | Topic | Question | Source |
|---|---|---|---|
| IQ01 | Go-live date | (Priority 1) Which date in January 2028 is T? We propose Monday 3 January 2028, with the legacy freeze after the EOD of Wednesday 29 December 2027 and the legacy December / year-end close completed in legacy before the final extracts. If T is later in January, the migration rule "first business day of a month after a legacy month-end close" (DMQ25) no longer holds and the GL opening needs a January stub period. | Timeline slide; answer A1; DMQ25, DMQ18 |
| IQ02 | Renewals around cut-over | (Priority 2) With the early renewal release withdrawn, please confirm that (a) legacy extracts, disposes, places and books all renewals up to T, (b) the RMEL cohorts expiring from T to T+140 days are carried forward into BIBS with their disposition (object P03), and (c) renewals expiring between the freeze and T are placed in legacy before the freeze or covered by hold covers. | Concept paper (superseded); answer A1; DMQ26 |
| IQ03 | Package remapping | (Priority 2) Who owns the remapping of legacy package names to BIBS packages, and where does it happen? We recommend a versioned PACKAGE code map applied at migration intake, plus a renewal sanitation check that sends unmapped or retired packages to Review for TSU. | Concept paper VII, VIII, Annex C |
| IQ04 | EIAM (Entra ID) | (Priority 1) Please confirm OpenID Connect (authorisation code with PKCE) against Entra ID; the claim that identifies the user (UPN, Windows ID or employee number); MFA and conditional access; token and session lifetimes against the 30-minute inactivity rule; whether EIAM replaces EUA (DCR-143); how non-BDO users (if any) sign in; and whether a local break-glass administrator is allowed. | Drop plan Drop 0; answer A2; UAM-NFR-11, 17, 19 |
| IQ05 | UIDM-ISC (IGA) | (Priority 1) Does UIDM-ISC own access requests and approvals, replacing the BRD-11 request workflow in BIBS, or does it only provision approved changes? Which connector (SCIM 2.0, REST, flat file)? Are entitlements the BIBS group profiles (roles) only? Frequency of aggregation and certification; leaver timing. | Drop plan Drop 0; answer A2; BRD-11 |
| IQ06 | LMS | (Priority 2) Which LMS data does BIBS receive (paid-off and RMU loans for renewal, loan snapshot for Submitted Policies, CLPC loan credits), in which layout, how often and by which transport? Is LMS the system behind the LAMD reports? | Answer A2; BRRN.029; SP-SQ06 |
| IQ07 | HL-LOAS | (Priority 2) HLS (Drop 0) and LOAS (Drop 2) are one system: is there one interface or two (quotation requests, HLS insurance report, amortised loan data)? Formats, frequency and transport. | Drop plan; answer A2; Q11 |
| IQ08 | LFS SaaS | (Priority 3) In which drop is the LFS interface, and what is its layout and transport? | Answer A2; BRD-12 p.4-5 |
| IQ09 | PMS | (Priority 2) Layout of the daily PDC list from the PDC Management System; does BIBS also send PDC status (deposited, returned, pulled out) back to PMS? Frequency and transport. | Answer A2; OQ03; CSHID.008 |
| IQ10 | Bank channels | (Priority 1) For CMS / New BOB (outward payments), OBPCS (bills payment), Old BOB (SOA funds transfers) and AFTS: file layouts or APIs, host-to-host or user upload, cut-off times, status and debit confirmation. What is AFTS used for? Is ICBS out of scope (struck through)? | Drop plan Drop 0; answer A2; AQ09, OQ03 |
| IQ11 | CCM | (Priority 2) Does BIBS send through CCM by SMTP relay or by API? Attachment size limit, password-protected PDFs, volume (renewal advice batches), delivery status and bounces, sender addresses. | Answer A2 |
| IQ12 | M365 Exchange | (Priority 3) What does BIBS do with M365 Exchange that CCM does not cover (read the mailboxes of e-mailed quotation requests or insurer replies through Microsoft Graph)? | Answer A2; Q12 |
| IQ13 | ECM | (Priority 3) With documents in S3 only, is there any ECM touch-point left (source of legacy documents for the archive, transfer of records at the end of retention)? | Answers A2, A4 |
| IQ14 | TFS | (Priority 3) Which client onboarding data comes from TFS, and what is the layout of the Trade payment file uploaded to Cashiering? | Answer A2; CSHID.008 |
| IQ15 | EDP (SD 11) | (Priority 1) What is SD 11? Which BIBS data does EDP ingest, at what frequency and in which format (files to S3, change-data capture, events)? Masking of personal data? | Drop plan Drop 2; answer A2 |
| IQ16 | EGL | (Priority 1) Does EGL take journal detail or balances, daily or monthly? Mapping of the BIBS chart to EGL accounts and segments, currency handling, layout and transport, reconciliation owner; relation to the GARD submission (AQ31). | Drop plan Drop 2; answer A2 |
| IQ17 | CARMS | (Priority 2) What is CARMS, and which data flows between it and BIBS in which direction? | Drop plan Drop 2 |
| IQ18 | Insurer System | (Priority 2) Which insurers and which exchanges (production feedback, remittance ORs, DP billing responses, renewal responses, SOAs, e-policies, claims) are in scope, and by which channel (SFTP, API, e-mail)? | Drop plan Drop 2; Q06, OQ22 |
| IQ19 | Bridger Insight XG | (Priority 3) Is Bridger the screening engine (BIBS records its results) or a second check next to BIBS list matching (SNSRP-201, 301)? Is a batch export of clients for Bridger needed? | Drop plan Drop 2; answer A2; SQ01 |
| IQ20 | Reinsurance transactions in Remittance | (Priority 1) Which reinsurance transactions does Remittance handle in Drop 1 (premium received from cedants and remitted to reinsurers, RI claims recoveries), with which accounting? Please send the "Operations BRD (Remittance Addendum)" and "(Cashiering Addendum)" named on the slide. | Drop plan 1.D2, 1.D3; answer A3 |
| IQ21 | Marketing Collection (extraction) | (Priority 2) What is "Marketing Collection (extraction)" in Drop 2? BRD-4 Collections is a BIBS module fed by the Operations ledger; is a separate Collection Management System kept, with an extraction from BIBS? | Drop plan 2.1; BRD-4 |
| IQ22 | Employee Benefits without portal | (Priority 1) Please confirm that insurers and client HR send EB documents by e-mail and BDOI users upload them (BRID-005, 005.01, 014 changed), and that EB placement (Drop 1) uses the NB placement screens. | Drop plan 2.4, 1.U6; BRD-8 |
| IQ23 | Unplaced BRDs | (Priority 2) In which drop are BRD-10 Sanction Screening (we propose Drop 1 with Client Onboarding) and the CSF service-request functions (we propose Drop 2)? | Drop plan |
| IQ24 | Drop 0 items without a BRD | (Priority 2) Which requirements govern Drop 0 items 1 (Accessibility & Login: sign-in, or WCAG accessibility, or both), 4 (Data Management) and 5 (Workflow)? We propose BRD-11 + EIAM, CORE-17 of the umbrella BRD, and the workflows of each BRD. | Drop plan Drop 0 |
| IQ25 | Architecture in the IER | (Priority 1) Please confirm that the IER describes BIBS as built: one backend (modular monolith) and one frontend on EKS, RDS PostgreSQL 16, ElastiCache Redis 7, MSK Kafka, S3; not the ten bv-* microservices, Aurora / MongoDB, Istio / OPA or EventBridge. Which edge is the BDO standard: ALB + WAF, CloudFront, Apigee X? | IER Architecture Diagram, K8s sheets |
| IQ26 | Disaster recovery | (Priority 1) Which region hosts DR (the HW sheet says cross-region read replica; the diagram says ap-southeast-1 warm standby)? Does data residency allow a second region? DR drill frequency? | IER HW & SW Requirements; hosting appendix; DCR-135 |
| IQ27 | Non-production hours | (Priority 2) What are the 12 x 5 hours? We need extended hours for batch and month-end test cycles, weekend migration mocks and the dress rehearsal; MSK and ElastiCache cannot be stopped. | IER Assumptions 3 |
| IQ28 | VDI and access | (Priority 2) Does "access restricted to personnel in the Philippines" apply to DEV and SIT with masked data? Which vendor staff need VDI? Software list: Temurin 21, Maven 3.9, Node 22 (not JDK 17, Node 20, Angular). | IER VDI Requirements; hosting appendix |
| IQ29 | CI / CD toolchain | (Priority 2) Which toolchain applies: GitLab (HW sheet), a second CI toolchain named in the IER (infra diagram) or ArgoCD + Helm (diagram)? Container registry, image base (RHEL UBI or Alpine), SonarQube instance. | IER sheets |
| IQ30 | Observability | (Priority 3) Dynatrace (VDI, K8s sheets) or CloudWatch, X-Ray, Prometheus, Loki, Grafana (diagram)? Log platform and alert routing. | IER sheets |
| IQ31 | S3 document store | (Priority 1) Please confirm the five decisions of DOCUMENT_STORAGE_DECISION.md section 5: option C with 5-minute presigned links (or option B, streaming through BIBS); the malware scanning service; Object Lock mode and the records under legal hold; KMS key ownership and rotation; whether ECM receives copies of any record class. Also the bucket account and the DR region for replication. | Answer A4; DOCUMENT_STORAGE_DECISION.md |
| IQ32 | Sizing basis | (Priority 2) Which user figures size the platform: 1,200 internal users (IER diagram) or 1,344 named / 429 concurrent (umbrella BRD p.42)? Transactions per second per year (IER K8s guideline 3). | IER; DCR-166 |
| IQ33 | IER completeness | (Priority 2) Will BDOI IT add the Pre-Prod and DR cluster sheets, retitle the Pre-Prod sheet (it reads "UAT ENVIRONMENT") and replace the template rows of the environment cost sheets with the sizing of this document? | IER Pre-Prod, K8s sheets |
| IQ34 | ElastiCache mode | (Priority 3) Please confirm cluster mode disabled (one shard, Multi-AZ replica) for Pre-Prod and PROD; "cluster" in the HW sheet would need a code change. | IER HW & SW Requirements |
| IQ35 | Security testing | (Priority 2) Who runs the performance and penetration tests of Nov - Dec 2027, and may an earlier scan run in SIT in June 2027? | Timeline slide |
