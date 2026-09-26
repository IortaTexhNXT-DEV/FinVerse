# BDOI Submitted Policies (BRD-12) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS, BDOI Broker System).

Status: **for BDOI concurrence.** The review workbook is consolidated later with the other BRDs. Build design: [`SUBMITTED_POLICIES_DESIGN.md`](../architecture/SUBMITTED_POLICIES_DESIGN.md). The cross-BRD report inventory is in [`BDOI_REPORT_LIST.md`](BDOI_REPORT_LIST.md).

## 1. Source documents

Sources:
- `docs/source-documents/BRD - Submitted Policies (with e-sig MCM 4.24.2026).pdf` (16 pages). Pages 1-12 have no text layer: they were rendered as images and read.
- `docs/source-documents/Report List as of APR-27-2026.pdf` (58 pages, 185 reports). The Submitted Policies rows are on pages 44-45 and 49-52.

| Pages | Content | Notes |
|---|---|---|
| 1 | Cover: "BDOI Submitted Policies, BRD Template", prepared by ESG-BPS | |
| 2-3 | Executive summary, business goals, objectives and benefits | No goal box is ticked (Revenue, Cost Savings, Compliance, CX, Tech Obsolescence, Others) |
| 4 | Current process ("Before") and two flow charts: Submitted Policies - New Business (CBG Motor, CBG Fire, Non-CBG Corporate & Branches, Non-CBG Retail) and Handling Fee for Submitted Account - CBG Motor only | Image only |
| 5 | Current process: Renewal Business flows for CBG Motor, CBG Fire and Non-CBG Retail | Image only |
| 6 | Current transactions, reports and notifications; envisioned journey ("After") | |
| 7 | Envisioned flow: Submitted Policies (system, in-system workflow, client), Handling for Manual Encoded with IAAF; target reports (4) and notifications (10) | Image only |
| 8-12 | 11 key capabilities and the table of business requirements BRIDSP-01 to BRIDSP-33 | Image only |
| 13-14 | Usage requirements: volume and performance, availability, data retention | |
| 15-16 | Approval sheet (prepared, inputs, reviewed, approved), template revision log v1.0 10-Apr-26 to v1.3 20-Apr-26 | One BU representative is annotated "medical leave 04/28/26" |

## 2. Business summary

**What "submitted policies" are.** A bank borrower (auto loan, housing loan, leasing, corporate loan) must insure the collateral. When the borrower buys the insurance elsewhere and **submits his own policy** to the bank instead of buying through BDOI, the policy is a *submitted policy*. BDOI does not earn on it, but it has two interests:
1. **Compliance of the collateral cover.** For Non-CBG Corporate & Branches accounts (and CBG Fire home loans), a Policy Reviewer checks that the submitted policy is adequate for the bank and issues an **IAAF** (Insurance Adequacy Assessment Form) to the bank counterpart (IBG / Leasing, Home Loan AO).
2. **Conversion at renewal.** BDOI tracks the expiry of every submitted policy and tries to win the renewal: it sanitises the list against the bank's loan data (LAMD), excludes non-renewable accounts, prepares a renewal proposal with an insurer (with a 30-day hold cover), sends a Renewal Advice (RA) letter to the client, and places and books the account when the client accepts.

Today the work runs in Excel masterlists on shared drives (Drive H:\), e-mail and SharePoint, per market segment:

| Segment | New business intake (p.4) | Renewal (p.5) |
|---|---|---|
| CBG Motor | Team Lead extracts policy details from LFS and CIU; the Submitted Handler validates, matches and consolidates them into the Submitted Masterlist within 30 days of extraction | Sanitation Handler matches the SPI list against LAMD, excludes FFY, BDO/SM Group employee accounts and No Touch, updates the loan status (active, open market, fully paid, remedial / RMU), classifies the vehicle type and assigns an insurer, sends a proposal with a 30-day hold cover request, generates the ISYS reference and RA text file for COG to mail (90 days before expiry), then the NRNS list to Marketing for call-out |
| CBG Fire | The Home Loan AO sends policy documents; the Submitted Handler reviews adequacy, coordinates findings, a Submitted Checker validates, an IAAF is issued to the Home Loan AO. The New Business AO extracts the daily HLS insurance report and matches it against the IAAF list | Submitted Handler sends the expiring NB and renewal accounts; the Sanitation Handler matches LAMD, assigns a different insurer from the expiring one, sends the proposal with hold cover, generates the ISYS reference and RA; the Team Lead monitors the status |
| Non-CBG Corporate & Branches | The Policy Reviewer receives the documents from IBG / Leasing, validates and encodes them, issues an IAAF (one IAAF may cover several policies) and sends the complete policy with IAAF to the Marketing AO, who reviews it for bidding / conversion | Same as NB (combined flow) |
| Non-CBG Retail | Marketing AO receives the list of submitted policies from the LAMD report and encodes them in the masterlist | MAO asks an insurer to quote on the expiring terms, sends the quotation to the client (employee), prepares the ARF in ISYS for Processing, Processing places and books; declines get an NRNS letter |
| Handling fee (CBG Motor only) | Admin Team UPP handler filters the Unapplied Payment List for handling-fee billing, NB TL assigns the accounts to AOs, the AO builds the client IDs, the NB TL sends the disposition back, the UPP handler updates the list on SharePoint | - |

The envisioned journey (p.6-7) is one system-driven flow: **receive** expiring and submitted policy details, **sanitise**, **identify the policies due for renewal**, **provide the disposition**, send the **insurer proposal with a hold cover request** (follow up after 3-5 days, re-assign on decline), **send letters / proposals** to the client (RA, NRNS…) and reminders c/o the bank counterpart, then **payment confirmation, placement and booking**. Manually encoded policies with an IAAF follow a review → IAAF approval → IAAF issued to the bank counterpart path, then either a reminder letter or the monitoring of the policy expiry for conversion.

Target reports (p.7): Submitted Masterlist; Sanitation, Matching, Disposition and Classification reports; Renewal and Placement reports; Fallout and Exception reports. Target notifications: ten types (BRIDSP-24).

**Fit in BIBS.** Nothing in BIBS holds policies that BDOI did not place. The masterlist, the rules, the IAAF and TOR, the buckets and the letters are new. Intake, extraction, workflow, approvals, documents, messaging, hold cover, placement, booking and reporting already exist and are reused. The renewal leg (RA letter, proposal, placement, booking) overlaps the Renewal BRD, analysed in parallel.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 1 |
| CONFIGURE | Set-up only | 0 |
| CHANGE | Extends an existing capability | 15 |
| NEW | New build | 17 |
| OUT | Out of scope per the BRD | 0 |
| **Total** | | **33** |

### Rows per key capability

| KC | Capability (p.8) | Rows | FIT | CHANGE | NEW | Effort S/M/L |
|---|---|---|---|---|---|---|
| 1 | Capture submissions from approved sources in the prescribed formats | 01, 02, 03, 33 | 0 | 3 | 1 | 1/3/0 |
| 2 | Single source of truth with controlled access | 04, 27, 28, 29 | 0 | 2 | 2 | 3/1/0 |
| 3 | Governance-driven policy reviews with traceability (IAAF) | 05, 06, 07 | 0 | 1 | 2 | 2/1/0 |
| 4 | Configurable, rule-driven processing | 08, 31, 32 | 0 | 2 | 1 | 1/2/0 |
| 5 | Automated workflow triggering; exception and fallout reporting | 09, 10 | 0 | 1 | 1 | 1/1/0 |
| 6 | Classification driving renewal and disposition | 11, 12, 13, 14, 15 | 0 | 0 | 5 | 3/2/0 |
| 7 | Limit breaches and insurer approval (TOR) | 16, 17, 18, 19 | 0 | 3 | 1 | 3/1/0 |
| 8 | Visibility of operations, exceptions and outcomes | 20, 21, 30 | 1 | 0 | 2 | 2/1/0 |
| 9 | Expiry detection, renewal initiation, auto-sending of proposals and letters | 22, 23 | 0 | 1 | 1 | 0/2/0 |
| 10 | Renewal candidates, placement files, booking confirmation | 25, 26 | 0 | 1 | 1 | 1/1/0 |
| 11 | System-generated alerts and notifications | 24 | 0 | 1 | 0 | 0/1/0 |

### Target modules

| Module | Rows |
|---|---|
| `submitted` (new) | 01-20, 22-25, 28-30, 33 (28) |
| `placement` (change, with `account`) | 26, 32 |
| `booking` / `account` (change) | 27 |
| `cashiering` + `collections` (change) | 31 |
| platform (`report`) | 21 |

### Big-ticket items

1. **Submitted Masterlist** (`sbm_policy`): one record per submitted or inforced policy per segment and business type, with loan data, risk details, classification, bucket, renewal tag, handler, conversion status and a full history. It is fed by six sources plus manual entry and by the Excel migration.
2. **Rule engine for sanitation, matching, classification and disposition** (BRIDSP-08/09/11-15): configurable criteria with maker-checker. Each run records its outcome per policy and a fallout list with reason codes.
3. **IAAF and TOR** (BRIDSP-05-07/16-19): documents with approval matrices, signatures and hand-over to the AO.
4. **Renewal hand-off** (BRIDSP-22/23/25/26/27): expiry scan, RA / NRNS / NAL / SFU letters, renewal account, hold cover, placement and booking. It reuses BRD-1. Agreed with the Renewal BRD: the Renewal module owns everything after the hand-off (SQ10 closed, cross-BRD decision D2).
5. **Handling fee tagging in the Unapplied Payment List** (BRIDSP-31): a cross-module change in cashiering and collections.

## 4. Submitted Policies flow (for concurrence)

```
Sources (LFS, HLS, CIU, SPI, LAMD, IBG/Leasing docs, manual, Excel migration)
   │ intake runs (bulk / flow-in) ── document upload → extraction → user confirmation (BRIDSP-01/02/03/33)
   ▼
Submitted Masterlist (sbm_policy) ── history, handler, conversion status, remarks (04/28/29)
   │ processing run: sanitation → matching vs LAMD → classification INFORCED / SUBMITTED → buckets (08-15)
   │                 └── fallout report with reason codes (10/14)
   ├── Policy review (Non-CBG Corporate, CBG Fire): review → IAAF → approval matrix → signed → bank counterpart (05-07)
   ├── Limit check → TOR → TSU approval matrix → released to AO (16-19)
   ▼
Expiry scan (lead days per segment) → For Renewal list (23/25)
   │ insurer proposal + 30-day hold cover (follow-up 3-5 days, re-assign, 32)
   │ letters: RA generic / FFY, NRNS, NAL, SFU, reminders (13/22)
   ▼
Client accepts → renewal account (ARN) → payment gate → placement file → booking (26/27)
   └── masterlist shows BOOKED / NOT RENEWED; conversion and persistency reports (20/30)
Handling fee (CBG Motor): UPP item with PN (CLPC) / location ref (OTC) = handling-fee record → tagged (31)
```

## 5. Requirements and fit/gap

Personas as written: System, Marketing User, Account Officer, Placement User, Booking User, User. All rows are "Must Have".

### KC 1. Capture of submissions

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-01 | System | Receive submitted policy details from defined and approved sources (LFS, HLS, CIU, SPI, etc.) so that all submissions are captured consistently | Given a submission from a defined source, when received, then the policy details are recorded | p.8 | **CHANGE** | Bulk upload framework with templates, validation and partial commit (`bulk/service/BulkImportHandler.java`); flow-in runs with idempotency per record and failure alerts (`opsledger/service/FlowInService.java`); in-app extract repository (`opsledger/service/ExtractRepositoryService.java`) | Source register `sbm_source` (code, segment, format, template, active). One `BulkImportHandler` per source layout (`SBM_LFS_INSURANCE`, `SBM_HLS_INSURANCE`, `SBM_CIU`, `SBM_SPI`, `SBM_LOAN_BOOKING`, `SBM_LAMD`); file hash duplicate block; each row upserts the masterlist by natural key (segment + PN / policy no. + expiry). Transports parked: default is upload | `submitted` | M | SQ01, SQ02, Q11 |
| BRIDSP-02 | System | Extract policy details from uploaded policy documents and present them for confirmation, so that only validated data is stored | Given a document is uploaded, when data is extracted and shown, then data is saved only after user confirmation | p.8 | **CHANGE** | Extraction port with regex patterns per insurer and a review screen before confirmation (`issuance/service/PolicyDataExtractor.java`, `PdfTextPolicyDataExtractor.java`); text PDFs only, OCR parked (BRD-1 Q24) | Extend the port with a document kind `SUBMITTED_POLICY` and a wider field set (assured, PN, policy no., insurer, period, TSI, premium, vehicle or property details). `sbm_extraction` holds the proposal; the confirm screen shows the extracted value, the confidence and an editable field side by side; nothing reaches `sbm_policy` before *Confirm*. An `OcrEngine` port for scanned or printed documents is parked | `submitted` (+ `issuance` port) | M | SQ03, Q24 |
| BRIDSP-03 | Marketing User | Create or update policy details manually and tag accounts / policies as Renewable or Non-Renewable, so that incomplete or non-extractable data is still recorded and renewal opportunities are acted on | Manual entry saves the policy; selecting or updating the renewal opportunity tag saves it, shows it on the record and makes it available for reporting and filtering | p.8 | **NEW** | Tagging pattern (FFY / payment arrangement) with reason and audit on accounts (`account/service/AccountTaggingService.java`); no record for policies BDOI did not place | Manual create / edit form on `sbm_policy` (mandatory fields per segment from `sbm_source` MANUAL); renewal tag RENEWABLE / NON_RENEWABLE with reason (LOV `SBM_NON_RENEWAL_REASON`), user and time; a manual tag overrides the rule result and is shown as a flag chip | `submitted` | S | SQ17 |
| BRIDSP-33 | System | Migrate existing submitted masterlists in Excel so that historical data is preserved, standardised and available | Excel (.xlsx) files are read and processed; valid records migrated completely and accurately; original submission status and key metadata (submission date, reference ID) retained; invalid, incomplete or failed records captured in an error log with clear reason codes | p.12 | **CHANGE** | Bulk framework: row validation, partial commit, error report XLSX, job audit (`bulk/service/BulkImportHandler.java`) | Handler `SBM_MIGRATION` per legacy layout (NB Motor, RB Motor, Fire, Non-CBG) with a column mapping table; legacy status mapped by `sbm_status_map`; `migrated = true`, `legacy_ref`, `date_received` kept; failed rows land in the bulk error log with reason codes `SBM_MIG_*` and in report `SBM-MIGRATION-ERRORS`; re-runnable (idempotent on legacy ref) | `submitted` | M | SQ16 |

### KC 2. Single source of truth and access

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-04 | System | Centralise all submitted policy details into one Submitted Masterlist / database | When a policy is saved or updated, the masterlist reflects the latest information | p.9 | **NEW** | - | `sbm_policy` is the masterlist (segment, business type NB / RB, source, dates, loan data, risk details, insurer and policy, classification, bucket, tag, handler, conversion status, linked ARN and invoice). Every change writes `sbm_policy_history` and the audit trail (`audit/service/AuditTrailService.java`) | `submitted` | M | |
| BRIDSP-27 | Booking User | Book the account so that the policy lifecycle is completed | Given placement is successful, when the account / policy is tagged as booked, then the booked status shows in the masterlist | p.11 | **CHANGE** | Booking of accounts with events and `InvoiceBooked` after commit (`booking/service/BookingService.java`, `InvoiceBooked.java`); the invoice business type is hard-coded NEW_BUSINESS (`booking/service/InvoiceBuilder.java` line 129) | The renewal account carries its origin (`SUBMITTED_POLICY`) and business type RENEWAL; booking copies it to the invoice flags and to `InvoiceBooked`. A `submitted` listener sets the masterlist status BOOKED with invoice no. and booking date. Direct booking stays in `booking` | `booking`, `account`, `submitted` | S | |
| BRIDSP-28 | User | View and extract the Submitted Masterlist based on role | Role-based access rules apply to viewing and downloading | p.12 | **CHANGE** | Permissions on every endpoint and report, split view / export permissions and the report archive (`report/core/ReportMetadata.java`, `ReportArchiveService.java`) | Permissions `SBM_VIEW` / `SBM_EXPORT`; **row scope** by segment and, for Marketing AOs, by own accounts (`sbm_user_scope`, default from the user's sales unit). Extract = report `SBM-MASTERLIST` (XLSX, CSV, PDF) with the archive | `submitted` | S | SQ15 |
| BRIDSP-29 | Marketing User | Monitor updates to handler, conversion status and remarks in the masterlist | When changes are saved, the masterlist shows the latest handler, conversion status and remarks | p.12 | **NEW** | Field change audit pattern (`audit`) | Columns `handler_username`, `conversion_status` (LOV `SBM_CONVERSION_STATUS`), `remarks`; change log per field on the record's History tab; filters and a "changed since" quick filter on the masterlist | `submitted` | S | |

### KC 3. Policy review and IAAF

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-05 | Marketing User | Generate one IAAF per policy, linkable to related policy reviews, so that reviews are traceable | Only one IAAF is created per policy; the IAAF may be linked to a related policy via a unique reference number | p.9 | **NEW** | Versioned templates and branded PDF (`docgen/service/DocumentComposer.java`); gap-free numbers (`common/sequence/DocumentNumberService.java`) | `sbm_iaaf` (`IAAF-yyyy-nnnnnn`, unique per policy); link table `sbm_iaaf_link` to related policies (e.g. the previous term); template `SBM_IAAF` | `submitted` | S | SQ07 |
| BRIDSP-06 | System | Store and display remarks and review dates per review within the IAAF | The IAAF shows the review history and the review count | p.9 | **NEW** | - | `sbm_iaaf_review` (review no., date, reviewer, adequacy ADEQUATE / WITH_FINDINGS, findings, remarks); count derived; shown on the IAAF page and printed on the form | `submitted` | S | |
| BRIDSP-07 | System | Route the IAAF for approval and signature per the IAAF Approval Matrix | When conditions are met, the IAAF goes to the correct approver | p.9 | **CHANGE** | Workflow engine with stages, permissions and SLA (`workflow/service/WorkflowService.java`); approval inbox (`approval/service/PendingApprovalSource.java`); signature block in docgen | Workflow `SBM_IAAF` (DRAFT → FOR_APPROVAL(level n) → APPROVED → ISSUED; RETURNED). Matrix `sbm_approval_matrix` (document IAAF, segment, TSI band, level, permission / named signatory). The approver's name, position and time are stamped as the signature; a qualified e-signature is parked | `submitted` | M | SQ07 |

### KC 4. Configurable processing

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-08 | Marketing User | Define matching, sanitation and account classification criteria so that processing rules are configurable | Given sets of criteria, when a process is triggered, then the rules are applied consistently | p.9 | **NEW** | Maker-checker master data (`common/domain/AuthorizableEntity.java`); rule-table pattern (`cat_tsu_rule`, `csh_disposition_type_rule`) | `sbm_rule_set` / `sbm_rule` (step SANITATION / MATCHING / CLASSIFICATION / DISPOSITION, segment, business type, priority, conditions as field / operator / value rows, outcome bucket, reason code, stop flag, effective dates), maker-checker (`SBM_RULE_APPROVE`), version stamped on each run result | `submitted` | M | SQ05 |
| BRIDSP-31 | System | Identify and tag Handling Fee payments in the Unapplied Payment List using the PN number (CLPC) and the Location Reference Number (OTC) | Given unpaid items in the UPP list, when a payment carries a valid PN (CLPC) or location reference (OTC) that matches a Handling Fee record, then it is tagged Handling Fee, its classification status is updated, and it is available for reporting, audit and application | p.12 | **CHANGE** | Unapplied items with payment channel and references (`cashiering/domain/Unapplied.java`, `Payment.java`, channels CLPC / OTC in `CashCodes.java`); read and disposition-request ports (`opsledger/service/port/UnappliedDirectory.java`, `UnappliedDispositionRequests.java`); collector-side UPP disposition in Collections (COLLECTIONS_DESIGN 4.4) | `sbm_handling_fee` (policy, PN, location ref, amount, status BILLED / TAGGED / APPLIED). Job `SBM_HANDLING_FEE_TAGGER` (and an event on new unapplied items) reads the directory, matches by channel + reference and sends a disposition request with the new action `RECOGNIZE_INCOME` (type `HANDLING_FEE`). Cashiering applies it: event `SBM_HANDLING_FEE` and an OR through `ReceiptIssuer` | `cashiering`, `collections`, `submitted` | M | SQ13, OQ12, OQ15 |
| BRIDSP-32 | Marketing User | Update the assigned insurer even when a Hold Cover request is ongoing | When the insurer update is saved, the change is reflected and a Hold Cover request goes to the newly assigned insurer | p.12 | **CHANGE** | Hold cover request / confirm / decline per ARN, expiry job and alerts (`placement/service/HoldCoverService.java`) | `HoldCoverService.reassign(arn, newInsurerCode, reason)`: closes the open request as REASSIGNED, updates the account insurer through `AccountLifecycleService`, sends a new request (template `HOLD_COVER_REQUEST`) and keeps both in history | `placement` | S | SQ12 |

### KC 5. Workflow triggering and fallout

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-09 | System | Process uploaded / extracted documents so that sanitation, matching, disposition and classification run automatically | Given documents are uploaded, when processing occurs, then the right workflow is triggered; when needed, account tagging runs per the defined criteria | p.9 | **NEW** | Managed jobs with run history (`system/service/ManagedJob.java`); document trigger table pattern (`iss_document_trigger`) | `SbmProcessingService.run(trigger, scope)`: the steps run in order (sanitation → matching vs LAMD → classification → disposition), each writing `sbm_run_result`. It is started after each intake commit, by job `SBM_PROCESSING` (daily) or on demand, and it sets tags and buckets and opens work cases | `submitted` | M | SQ05 |
| BRIDSP-10 | System | Generate a fallout report after document processing so that exceptions and failed records are identified | Given processing completes, when errors or exceptions occur, then a fallout report is generated | p.9 | **CHANGE** | Bulk error report and job summary (`bulk`) | Fallout rows = `sbm_run_result` with outcome FALLOUT and reason code, plus extraction failures; reports `SBM-DOC-FALLOUT` and `SBM-PROCESS-FALLOUT`, archived per run; alert `SBM_FALLOUT` when the count is above zero | `submitted` | S | |

### KC 6. Classification and buckets

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-11 | System | Identify whether an account / policy is Inforced or Submitted so that the right renewal and disposition rules apply | When the policy details are evaluated, the system classifies it Inforced or Submitted | p.9 | **NEW** | - | CLASSIFICATION step: SUBMITTED when the PN matches an active LAMD loan and the policy is not BDOI-placed; INFORCED otherwise (rule-driven, SQ04) | `submitted` | S | SQ04 |
| BRIDSP-12 | System | Group Submitted Policies (matched PN vs LAMD) into qualification buckets | Policies grouped into For Renewal, For Manual Disposition, Non-Renewal, No Touch, FFY, BDO/SM Group Employee Accounts or RMU, and the next workflow is triggered | p.9 | **NEW** | - | Buckets as LOV `SBM_BUCKET` (with attribute `renewal_action`); DISPOSITION step rules; entering a bucket opens the matching work case (renewal hand-off, manual disposition queue, non-renewal fallout) | `submitted` | M | SQ05 |
| BRIDSP-13 | System | Identify an Active Loan per LAMD with a non-amortised account so that the right RA template is generated | Active non-amortised loan → ready for Generic RA or FFY RA template; next workflow triggered | p.10 | **NEW** | Templates (`docgen`) | LAMD snapshot `sbm_lamd_loan` (PN, status, amortised, maturity); rule output `ra_template` = RA_GENERIC or RA_FFY on the policy | `submitted` | S | SQ06 |
| BRIDSP-14 | System | Assign policies automatically to Non-Renewal buckets (Free First Year, SM Group Employee, No-Touch, CARI, Bonds, RMU, etc.) so that they are excluded from renewal | Tagged Non-Renewal and recorded in a fallout report with the exclusion reason | p.10 | **NEW** | - | Non-renewal rules set tag NON_RENEWABLE with reason; report `SBM-NON-RENEWAL`; manual override (BRIDSP-03) with audit | `submitted` | S | SQ05 |
| BRIDSP-15 | System | Group Inforced Policies (unmatched PN vs LAMD) into Fully Paid Loan, OPM, BDOFC or Sold buckets so that RAs and Proposals can be created | Policy placed in the right bucket; for eligible inforced policies RA and Proposal are generated | p.10 | **NEW** | Templates and PDF (`docgen`) | Inforced bucket rules; eligible → renewal hand-off with RA (template per bucket) and proposal (`SBM_RENEWAL_PROPOSAL`) | `submitted` | M | SQ05 |

### KC 7. Limits and TOR

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-16 | System | Detect accounts exceeding acceptance or coverage limits so that insurer approval can be required | Account flagged for insurer approval when a limit is exceeded | p.10 | **CHANGE** | Package TSI limit and TSU routing (`catalog/domain/RiskProduct.java` `exceedsPackageLimit`, `catalog/service/TsuRoutingService.java`) | `sbm_limit_rule` (insurer, line / product, max TSI, max vehicle age, other attribute limits) plus the catalog package limit; the LIMIT step flags `insurer_approval_required` with the breached limit in `sbm_limit_check` | `submitted` | S | SQ08 |
| BRIDSP-17 | Marketing User | Generate a Terms of Reference (TOR) for accounts exceeding limits | A TOR document is created for the account | p.10 | **NEW** | `docgen` | `sbm_tor` (`TOR-yyyy-nnnnnn`, policy, breaches, proposed terms), template `SBM_TOR` | `submitted` | S | SQ08 |
| BRIDSP-18 | System | Route the TOR for approval and signature per the TSU Approval Matrix | TOR sent to the correct approver(s) for signature | p.10 | **CHANGE** | Workflow and approval inbox; TSU roles and `TSU_APPROVE` (BRD-1) | Workflow `SBM_TOR` (DRAFT → FOR_APPROVAL(level n) → APPROVED → RELEASED); matrix `sbm_approval_matrix` document TOR | `submitted` | M | SQ08 |
| BRIDSP-19 | Account Officer | Receive / be notified of the approved TOR to proceed with renewal and insurer engagement | Approved TOR handed over to the assigned AO, who can view or download it | p.10 | **CHANGE** | In-app and e-mail notifications (`messaging/service/NotificationService.java`); attachments (`attachment`) | On APPROVED: signed PDF stored as attachment `TOR`, notification to the AO, status RELEASED when the AO opens / downloads it | `submitted` | S | |

### KC 8. Reports

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-20 | Marketing User | Generate reports so that sanitation, disposition, account classification and other results can be monitored | The correct report is generated on request; a fallout report can be generated for exceptions | p.10 | **NEW** | Report framework: parameters, grouping, PDF / XLSX / ODS / CSV / XML, saved variants (`report/core/ReportDefinition.java`, `TabularReportBuilder.java`) | Reports `SBM-SANITATION`, `SBM-DISPOSITION`, `SBM-CLASSIFICATION`, `SBM-RENEWABLE`, `SBM-PROCESS-FALLOUT` and the other SP rows of the Report List (design section 11) | `submitted` | M | SQ25 |
| BRIDSP-21 | System | Support viewing, downloading and printing reports in multiple formats (Excel, PDF …) | Report rendered correctly in the chosen format | p.10 | **FIT** | PDF / XLSX / ODS / CSV / XML renderers and print preview (`report/render/**`) | Re-use | `platform (report)` | S | |
| BRIDSP-30 | Marketing User | Same text as BRIDSP-20 | Same as BRIDSP-20 | p.12 | **NEW** | As BRIDSP-20 | Covered by BRIDSP-20 (recorded once, see section 11) | `submitted` | S | |

### KC 9. Expiry, renewal initiation, letters

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-22 | System | Send letters or proposals automatically when defined criteria are met | When policy evaluation occurs and the sending criteria are satisfied, letters or proposals are sent automatically | p.11 | **CHANGE** | Outbox with protected attachments and send log (`messaging/service/MessageService.java`); templates (`docgen`); batch print pattern (`cashiering/service/BatchPrintService.java`) | `sbm_letter` (type RA_GENERIC, RA_FFY, NRNS, NAL, SFU, REMINDER, RENEWAL_PROPOSAL; channel EMAIL / PRINT / BANK_COUNTERPART), sending rules `sbm_letter_rule` (bucket, days before expiry, channel). Job `SBM_LETTER_DISPATCH`; the print channel builds a merged PDF batch for the mail house (COG), whose transport is parked | `submitted` | M | SQ09 |
| BRIDSP-23 | System | Detect expiring policies and initiate renewal processing | When a policy nears expiry and renewal conditions are met, renewal processing starts | p.11 | **NEW** | Insurer-side renewal due report (`underwriting/report/RenewalDueReport.java`) does not apply to broker records | Job `SBM_EXPIRY_SCAN` (daily): policies whose expiry falls within the segment lead days (parameters, e.g. 150 days CBG Fire hand-over, 90 days RA) and whose bucket is For Renewal enter RENEWAL_DUE and are handed to `RenewalHandOff` | `submitted` | M | SQ10 |

### KC 10. Renewal candidates, placement, booking

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-25 | Marketing User | Receive the list of accounts / policies subject for renewal for call-out and placement | When the renewal cycle starts and eligible records are extracted, a complete list of renewable accounts is generated | p.11 | **NEW** | - | Work list "For Renewal" (by expiry month, bucket, AO, insurer) and report `SBM-RENEWABLE`; assign / re-assign AO in bulk | `submitted` | S | |
| BRIDSP-26 | Placement User | Process placement files so that policies are placed efficiently | Given the client confirms renewal with BDOI, when placement is triggered, a placement file is transmitted to the insurer | p.11 | **CHANGE** | Payment gate, placement slip per insurer branch, protected send to the insurer mailbox (`placement/service/PlacementSlipService.java`, `PaymentGateService.java`) | "Renew with BDOI" on a masterlist record creates the renewal account through `AccountService.createDraft` (origin SUBMITTED_POLICY, business type RENEWAL, risk details copied), then the normal BRD-1 path; placement files are the existing slips | `account`, `placement`, `submitted` | M | SQ10, SQ11 |

### KC 11. Notifications

| BR ID | Persona | Requirement | Acceptance criteria | Page | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRIDSP-24 | System | Generate and send notifications for key lifecycle events and exceptions, so that users, account officers and clients act on time | On each event (submission, validation, pending approval / approved, classification change, exception, expiry, placement, renewal decision…) the rules and thresholds are evaluated and the notification sent: new submission or manual validation needed; pending IAAF / TOR approvals; moves to renewal / manual / non-renewal buckets; exception or fallout cases; nearing expiry and renewal start; TOR hand-over to AOs; placement file ready or transmitted; RA letters, SFU letters (CBG only), NAL, renewal notices, proposals and reminders; NRNS for declined or non-renewal cases; hold cover of unbooked accounts | p.11 | **CHANGE** | Notifications to users / permissions, per-user preferences (`messaging/service/NotificationService.java`, `NotificationPreferenceService.java`); alert codes with thresholds (`alert/service/AlertService.java`); hold cover expiry job (`placement`) | Notification events `SBM_*` registered in the preference catalogue; client-facing items are the letters of BRIDSP-22; alert `SBM_HOLD_COVER_UNBOOKED` (hold cover confirmed, account not booked n days before hold-cover expiry, parameter) | `submitted` (+ `placement` alert) | M | SQ09, SQ12 |

## 6. Lists from the BRD and the Report List

| List | Values as written | Source |
|---|---|---|
| Segments | CBG Motor, CBG Fire, NON CBG Corporate & Branches, NON CBG Retail | p.2 |
| Business type | New Business, Renewal Business | p.2, p.4-5 |
| Sources | LFS, HLS, CIU, SPI "etc."; also LAMD report, IBG / Leasing documents (scanned / printed), Loan Booking Report (CBG Doc Def Mgt Team), IA Masterlist | BRIDSP-01, p.4-5, Report List #138/#151 |
| Classification | Inforced, Submitted | BRIDSP-11 |
| Submitted buckets | For Renewal, For Manual Disposition, Non-Renewal, No Touch, FFY, BDO/SM Group Employee Accounts, RMU | BRIDSP-12 |
| Non-renewal buckets | Free First Year, SM Group Employee, No-Touch, CARI, Bonds, RMU, "etc." | BRIDSP-14 |
| Inforced buckets | Fully Paid Loan, OPM, BDOFC, Sold | BRIDSP-15 |
| Loan statuses (CBG Motor) | Active Loans, Open Market, Loan Fully Paid, Remedial (RMU) | p.5 |
| RA templates | Generic RA, FFY RA | BRIDSP-13 |
| Letters | RA letter, NRNS letter, SFU letter (CBG only, mortgaged), NAL (mortgaged and non-mortgaged), renewal notices, proposals, reminder letters | p.6-7, BRIDSP-24, Report List #163 |
| Sanitation report (RMEL certification, Motor) | Expiring accounts: Inforce, Submitted, Pending from previous months. For renewal: Regular, RMU, Open Market, CTPL, Auto Promo (FFY). Not for renewal: SM/BDO Employee, No Touch, Mortgaged, Fully Paid, Cancelled / Did Not Materialize, Branch Account, Total Loss, Endorsement, Duplicate, Incorrect Expiry / Encoding, No Record with LAMD. Pending for disposition; RB Submitted | Report List #156 |
| Conversion statuses | Renewed / Unrenewed / Process Placement, each by Inforce, NB Submitted, RB Submitted, OPM, RMU; Issued SFU (mortgaged only) | Report List #163 |
| Policy review fields | Adequacy status (adequate / with findings), renewal status (remarks), opportunity tag (Yes / No) | Report List #134 |
| TOR statuses | Pending, Approved, Released | Report List #161 |
| Timings | Masterlist within 30 days of extraction (CBG Motor); CBG Fire hand-over 150 days from expiry month; RA 90 days before expiry; hold cover 30 days; insurer acceptance within 3-5 days | p.4-5 |

## 7. Reports named by the BRD and the Report List

| Report (as written) | Source | Code (design) |
|---|---|---|
| Submitted Masterlist Report / Extract (with migrated history) | p.6-7, BRIDSP-28, RL #151 | `SBM-MASTERLIST` |
| Document Processing Fallout Report | BRIDSP-10, RL #152 | `SBM-DOC-FALLOUT` |
| Fallout Reports: Sanitation, Matching, Disposition, Account Classification | BRIDSP-10/20, RL #153 | `SBM-PROCESS-FALLOUT` |
| Non-renewal Report (exclusions with reason codes) | BRIDSP-14, RL #154 | `SBM-NON-RENEWAL` |
| Migration Error Log | BRIDSP-33, RL #155 | `SBM-MIGRATION-ERRORS` |
| Sanitation Report (RMEL certification) | BRIDSP-20, RL #156 | `SBM-SANITATION` |
| Disposition Report | BRIDSP-20, RL #157 | `SBM-DISPOSITION` |
| Account Classification Report | BRIDSP-20, RL #158 | `SBM-CLASSIFICATION` |
| Renewable Accounts / Policies Report | BRIDSP-25, RL #159 | `SBM-RENEWABLE` |
| IAAF Tracking / Review Report | BRIDSP-05-07, RL #160 | `SBM-IAAF` |
| TOR Status & Approval Report | BRIDSP-17-19, RL #161 | `SBM-TOR` |
| Handling Fee Payment Classification Report | BRIDSP-31, RL #162 | `SBM-HANDLING-FEE` |
| Submitted Policies Conversion Report (and persistency, accounts for review) | BRIDSP-20/29, RL #163 | `SBM-CONVERSION` |
| Submitted Policies No Touch Report (billing list to insurers, re-upload for billing statement) | BRIDSP-14, RL #164 | `SBM-NO-TOUCH` |
| Policy Review Conversion Report | RL #133 | `SBM-PR-CONVERSION` |
| Policy Review Monitoring Report (ageing from bank request to endorsement) | RL #134 | `SBM-PR-MONITORING` |
| Renewal Persistency Report (submitted part) | RL #135 | `SBM-PERSISTENCY` |
| Penetration Report (CBG Motor per channel) and Hold Cover with Gap | RL #138 | `SBM-PENETRATION`, `SBM-HOLD-COVER-GAP` |
| Letters register (RA / NRNS / NAL / SFU dispatch) | BRIDSP-22/24 | `SBM-LETTERS` |

## 8. Non-functional requirements (p.13-14)

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Users | CBG Admin / Mktg: 30 users, 15 concurrent (login and sanitation). Non-CBG Corporate Policy Review Officer: 8 users, 6 concurrent (login and policy review) | Within the BRD-1 sizing (145 concurrent) | FIT |
| Volumes | Logins 22,000 / yr (+5%); sanitation 63,000 / yr (+25% per year); Non-CBG logins 2,000 / yr and policy reviews 2,000 / yr (+5%) | Small. Processing runs are set-based SQL per step; 63,000 sanitations grow to ~154,000 in year 5 | FIT |
| Response time | 2 seconds for all listed transactions | Screens and single-record actions within 2 s; processing runs and exports are asynchronous jobs with progress, so a user action never waits on a batch. Tighter than BRD-1 (10 s) and Operations (5 s): SQ21 | CHANGE |
| Peak | End of month / year-end; 08:00-18:00 daily; no mobile vs desktop difference | Batch jobs run at night (expiry scan, letters, processing) | FIT |
| Availability | 99.9%; business hours; max 45 min downtime per month (planned only, IT to assess); maintenance 00:00-04:00 | Platform deployment | FIT |
| Continuity | Critical services restored within 4 hours; full recovery within 24 hours | Platform DR | FIT |
| Retention | Transaction / submitted policy records: 5 years online, 5 years archive, daily backup, accessible daily, backup retention 5 years; no anonymisation | Retention rule `SUBMITTED_POLICY` in `nbadmin` (`nba_retention_rule`); archive vs purge stays the DBA decision (BRD-1 Q39) | CONFIGURE |

## 9. Questions answered by this BRD and the Report List

| Q# | Origin | Question (short) | Answer | Design impact |
|---|---|---|---|---|
| **Q11** | BRD-1 | HLS interface and other sources for bulk (auto loans for FFYMI) | **Partially.** Sources are named: LFS insurance report (auto loans), HLS daily insurance report, CIU report, SPI consolidated list, LAMD report, Loan Booking Report from the CBG Doc Def Mgt Team, IA masterlist (p.4-5, RL #138/#151). All arrive as Excel today; layouts and transports are still not given (SQ01) | One bulk handler per source layout, uploads by default |
| **Q14** | BRD-1 | Sanitisation criteria for bulk lists | **Partially.** For submitted policies: match the PN against LAMD, exclude FFY, BDO / SM Group employee accounts and No Touch, update the loan status (active, open market, fully paid, remedial / RMU), check duplicates by PN or unit details ("For Review" bucket), validate PN = serial / motor number and PN = assured name / address / contacts (RL #151) | Rules are data (`sbm_rule`), seeded from these lists; the NB bulk sanitiser is not changed |
| **Q24** | BRD-1 | Which documents need auto-extraction | **Partially.** Submitted policy documents (scanned or printed) must be extracted and confirmed by a user (BRIDSP-02). Scanned input implies OCR | The extraction port gets a second document kind; OCR remains a parked port |
| **Q27** | BRD-1 | Which accounts need an automatic 30-day hold cover; what happens at expiry | **Partially.** Renewal proposals for submitted CBG Motor / Fire policies go to the insurer with a 30-day hold cover request, to be accepted within 3-5 days, else follow up or re-assign to another insurer (p.5); hold covers of unbooked accounts must be alerted (BRIDSP-24). Expiry handling still open | `HoldCoverService.reassign`; alert `SBM_HOLD_COVER_UNBOOKED`; parameter `SBM_INSURER_ACCEPT_DAYS` |
| **Q36** | BRD-1 | FFY: payer, period, renewal hand-off | **Partially.** At renewal an FFY account gets an FFY-specific RA template (BRIDSP-13) and the Sanitation report counts "Auto Promo (FFY)" under For Renewal, while BRIDSP-14 lists FFY as a non-renewal bucket (SQ05). The payer is not stated | RA template choice on the policy; FFY bucket behaviour configurable |
| **Q40** | BRD-1 | Scope of dynamic reports | **Partially (Report List #74).** "User-defined filters as needed; export to Excel / PDF; no defined template" | Saved variants plus column filters (BRD-5 report platform change) meet the stated scope; no query builder |
| **Q28** | BRD-1 | CLPC billing file and payment report layouts | **Partially (Report List #65-67).** Three CLPC billing variants: non-built-in FIP, built-in HLS amortised / non-built-in, HLS amortised loan release, with sample files | Variants of `NB-CLPC-BILLING` (report list gap) |
| **OQ12** | Operations | Matching key for pre-booked / identified payments | **Partially.** Handling-fee payments are identified by the PN number for CLPC and by the Location Reference Number for OTC (BRIDSP-31) | Handling-fee tagger matches on channel + reference; cashiering matcher unchanged |
| **OQ15** | Operations | Full disposition type list and approvals | **Partially.** Report List #105 groups unapplied payments by: For Booking, For Reinstatement, For Refund, For Re-application, For Advanced OR / Pre-signed OR, For Client Identification, For AO / Unit Head review. BRIDSP-31 adds Handling Fee (p.4: Admin Team UPP handler → NB TL → AO → NB TL). Approvals still open | New disposition type `HANDLING_FEE`; the seven dispositions to seed in `csh_disposition_type_rule` / `CLX_UPP_DISPOSITION` (owners) |
| **OQ39** | Operations | No Touch / Top Up incentive schemes | **Partially.** "No Touch" accounts are submitted CBG Motor accounts that BDOI sends to the insurer for validation and billing: the insurer fills in the basic premium; BDOI bills a service fee (gross service fee + 12% VAT − 15% WTax) (RL #164). Targets and tiers are not given | `SBM-NO-TOUCH` export / re-upload and a service-fee billing via `ServiceInvoiceService` |
| **OQ42** | Operations | Layouts of Cashiering reports 9-18, 20-21 and Remittance 8 | **Partially (Report List #85-96, #103).** Descriptions list the key fields (e.g. Daily Cash Reconciliation: encoder, client / payor, OR no., deposit bank; Direct Payment: invoice no., assured, policy no., AR balance, payment type, OR no., date, OR amount, reversal amount) | Cashiering report columns to be aligned (owner) |
| **OQ45** | Operations | Are Marketing activities built in Operations or fed from other systems | **Partially.** Handling-fee disposition of UPP items is a Marketing (NB TL / AO) activity inside BIBS | The UPP handler works in Collections' unapplied view |
| **OQ17** | Operations | Shared drive or in-system repository | **Partially.** Today's masterlists live on Drive H:\; the target is "a single, master database" (p.6) | Masterlist and extracts stay in BIBS; `FileDropPort` unchanged |
| **AQ05** | BRD-5 | Layouts of the FRBS report pack | **Partially (Report List #1-31).** Fields are listed for the FRBS reports, with "AR expectation: summaries are derived values" | FRBS owners align columns |
| **CQ09** | BRD-4 | Which weekly reports per unit per branch | **Partially (Report List #59).** "Reports per Unit per Branch": daily / as needed, production-style columns (invoicing branch, department, unit head, business type, account type, QPS / BDOI SYS ref, AO, bank branch …) | Collections owner |

Not answered and still open: OQ01, OQ02, OQ07 (handling-fee and No Touch accounts are not given), OQ44 (this BRD adds a third NFR set), Q09 (Late Renewal stays with the Renewal BRD; RL #182 describes a *package* late-renewal report for TSU).

## 10. New open questions (Submitted Policies)

| Q# | Topic | Question | Related |
|---|---|---|---|
| SQ01 | Sources | Definition, owner, layout, frequency and transport of each source: LFS insurance report, HLS daily insurance report, CIU report, SPI list, LAMD report, Loan Booking Report, IBG / Leasing documents, IA masterlist. Is the list complete ("etc.")? | BRIDSP-01 |
| SQ02 | Formats | The "prescribed submission formats" (KC 1): templates per segment and business type | BRIDSP-01 |
| SQ03 | Extraction | Document types, share of scanned vs text PDFs (OCR?), mandatory fields and accepted accuracy | BRIDSP-02 |
| SQ04 | Classification | Definitions of Inforced and Submitted. BRIDSP-12 says Submitted = matched PN vs LAMD and BRIDSP-15 says Inforced = unmatched PN; is "Inforced" a policy placed by BDOI? | BRIDSP-11/12/15 |
| SQ05 | Buckets | Complete bucket list, precedence and meaning: RMU is a renewal bucket in the Sanitation report and a non-renewal bucket in BRIDSP-14; FFY likewise; Fully Paid is both an inforced renewal bucket (BRIDSP-15) and "not for renewal" (RL #156); CARI, Bonds, BDOFC, OPM, Sold definitions | BRIDSP-12/14/15, RL #156 |
| SQ06 | LAMD | What LAMD is, its fields (loan status, amortised flag, maturity), refresh frequency; rule for Generic vs FFY RA | BRIDSP-13 |
| SQ07 | IAAF | Approval matrix (levels, criteria, signatories), signature method (the pack title mentions "e-sig"), template; one IAAF per policy (BRIDSP-05) vs "one IAAF may consist of multiple policies" (p.4) | BRIDSP-05-07 |
| SQ08 | TOR | TSU approval matrix; acceptance and coverage limits per insurer / product and who maintains them (Product Maintenance?); TOR template; meaning of Released | BRIDSP-16-19 |
| SQ09 | Letters | Templates and triggers for RA (generic / FFY), NRNS, NAL, SFU, reminders, renewal notices and proposals; lead days per segment; channels (e-mail, snail mail through COG, c/o bank counterpart); COG hand-off format | BRIDSP-22/24 |
| SQ10 | Renewal boundary | After classification, does the Renewal BRD own the renewal of submitted policies (expiry list, RA, quotation, placement), or does this BRD? Who is the Placement / Booking user for submitted renewals? | BRIDSP-23/25/26 |
| SQ11 | Insurer assignment | Rules: by vehicle type (CBG Motor); "must be different from the expiring policy" (CBG Fire); insurer panel per segment | p.5 |
| SQ12 | Hold cover | Acceptance window "3-5 days": which value, calendar or working days; outcome when no insurer accepts; alert lead days for unbooked accounts | BRIDSP-24/32 |
| SQ13 | Handling fee | What the handling fee is, who pays it, amount or rate, VAT and OR, GL account, how the Handling Fee record is created (billing list), CBG Motor only? | BRIDSP-31 |
| SQ14 | No Touch billing | Service-fee basis and rates (12% VAT, 15% WTax as written), billing statement format, OR / service invoice, relation to the No Touch incentive of CMRID.005 | RL #164, OQ39 |
| SQ15 | Roles and scope | Access matrix for CBG Admin / Mktg, Submitted Handler, Submitted Checker, Sanitation Handler, Team Lead, Policy Reviewer, Marketing AO, Admin Team UPP handler, NB TL, Processing, COG; data visible per segment / AO | BRIDSP-28 |
| SQ16 | Migration | Files, volumes, legacy status values, reference IDs, cut-over date and whether history (reviews, letters) is migrated | BRIDSP-33 |
| SQ17 | Tags | Meaning of "opportunity tag" and "conversion" in the policy review reports; who may override a rule-set tag | BRIDSP-03, RL #133/#134 |
| SQ18 | Penetration | Inputs (LFS Loan Booking Report detailed, IA masterlist), the channel rule (Branch, Dealer, Broker / Direct), the exemption box and the "Hold Cover with Gap" report | RL #138 |
| SQ19 | Glossary | Expansions of IAAF (given), TOR, RA, NRNS, NAL, SFU, RMEL, RMU, OPM, BDOFC, CARI, FFYMI, SPI, CIU, LAMD, COG, UPP | whole BRD |
| SQ20 | ISYS | BIBS replaces the "ISYS reference" and the ARF: confirm that the BIBS ARN is the renewal reference and ISYS is not fed | p.5 |
| SQ21 | NFR | 2 s response for all transactions vs BRD-1 10 s and Operations 5 s; user counts for CBG Fire, Non-CBG Retail and handling-fee users | NFR |
| SQ22 | Retention | 5 years online + 5 years archive here vs 5 / 15 in BRD-1 | NFR |
| SQ23 | Non-CBG Retail | Employee accounts: quotation by the MAO in BIBS quotation module; NRNS "to client (employee)" by e-mail | p.5 |
| SQ24 | Policy review ageing | Start and end events of the "aging from date of request from the bank to endorsement of BDOI" | RL #134 |
| SQ25 | Report layouts | Final fields of the rows marked "for editing" (Sanitation, fallout, Conversion summary) and of rows without fields (#152, #154, #155, #157-#162) | BRIDSP-20 |

**SQ10 closed** by cross-BRD decision D2 ([`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md)): the Renewal
module implements `RenewalHandOff` and owns the renewal of submitted policies from the hand-off on (renewal account,
hold cover request, RA / NRNS / NAL / SFU letters); this BRD keeps intake, classification, the expiry scan and the
insurer rules. The renewal account then follows the BRD-1 placement and booking screens; which users handle submitted
renewals there is part of the role matrix (SQ15, OQ48).

## 11. Observations on the BRD pack

- Pages 1-12 are images without a text layer, including the requirements table; the text above was read from rendered pages.
- **BRIDSP-20 and BRIDSP-30** have identical text, persona and criteria. They are recorded as two rows but built once.
- **BRIDSP-27** (booking) is filed under key capability 2 (single source of truth), although KC 10 names "booking confirmation". KC numbers are not in ID order (BRIDSP-24 is KC 11, BRIDSP-25/26 are KC 10, BRIDSP-31/32 are KC 4, BRIDSP-33 is KC 1).
- **BRIDSP-09** carries two blocks of criteria, the second titled "Acceptance Criteria".
- No business goal is ticked on p.2; only the free-text objectives and benefits are given.
- **Bucket lists conflict** between BRIDSP-12, BRIDSP-14, BRIDSP-15 and the Sanitation report (RL #156): RMU, FFY and Fully Paid appear on both the renewal and the non-renewal side (SQ05).
- **One IAAF per policy** (BRIDSP-05) conflicts with the current-process note "IAAF may consist of multiple policies" (p.4). The design keeps one IAAF per policy with links (SQ07).
- The handling-fee flow (p.4) describes a manual Marketing loop around the UPP list; the requirement (BRIDSP-31) only automates the tagging. The disposition steps (NB TL assigns, AO builds client IDs) have no BR ID.
- The process pages use many acronyms that are never expanded (TOR, NRNS, NAL, SFU, RMEL, RMU, OPM, BDOFC, CARI, SPI, CIU, LAMD, COG, UPP) (SQ19).
- The renewal flows (RA letters, proposals, hold cover, placement, booking) overlap the Renewal BRD, which is analysed separately; the boundary is not stated (SQ10).
- The usage table covers only CBG Admin / Mktg and Non-CBG Corporate policy review officers; there are no figures for CBG Fire, Non-CBG Retail or the handling-fee users (SQ21).
- The NFR response time (2 s) is tighter than BRD-1 (10 s) and Operations (5 s); retention (5 / 5 years) differs from BRD-1 (5 / 15).
- The approval sheet: one BU representative is annotated "medical leave 04/28/26"; the file name says "with e-sig MCM 4.24.2026", while the revision log ends at v1.3 on 20-Apr-26.
- In the Report List, several Submitted Policies rows are incomplete: no fields for #152, #154, #155, #157-#162; "Note: For editing" on #153, #156 and #163; #134 has no New / Existing, source or completion status.
