---
# Source of the Functional Requirements Specification for BRD-12 Submitted Policies.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD12_SUBMITTED_POLICIES.md
title: Submitted Policies
subtitle: BRD-12 BDOI Submitted Policies (with the Submitted Policies rows of the Report List of 27-Apr-2026)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-12
name: Submitted Policies
doc_id: BIBS-FRS-BRD-12
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-12 Submitted Policies
output: FRS/BIBS_FRS_BRD-12_Submitted_Policies_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-12 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions D1 and D2
distribution:
  - {name: "Product Owner, Submitted Policies", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Unit Head, Combank and Corbank", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "CBG Admin / Marketing (Submitted Handlers, Sanitation Handlers, Team Leads)", role: Business user, organisation: BDOI, purpose: "Review of intake, processing and renewal hand-off"}
  - {name: "Non-CBG Corporate Policy Review Officers", role: Business user, organisation: BDOI, purpose: "Review of policy review, IAAF and TOR"}
  - {name: "Admin Team (UPP handlers), NB Team Leads", role: Business user, organisation: BDOI, purpose: "Review of the handling-fee tagging"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Submitted Policies business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to build the Submitted Policies module, to test it and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirements it meets and their BRD pages.

Submitted Policies is **designed and not yet built**. The FRs describe the behaviour of the build design (R3). Screen names and API paths are those of the design and are confirmed at build. Error codes are assigned at build; this document gives the message text only, except where a check reuses a platform code that already exists (section 1.5).

## Scope

A **submitted policy** is a policy that a bank borrower bought elsewhere and submitted to the bank to insure the collateral of an auto, housing, leasing or corporate loan. BDOI did not place it and earns nothing on it, but it has two interests in it (BRD p.2-5): the adequacy of the collateral cover, which a Policy Reviewer checks and certifies with an IAAF, and the renewal, which BDOI tries to win when the policy expires.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Intake | Submissions from the approved sources, document extraction with confirmation, manual entry and tagging, migration of the Excel masterlists | BRIDSP-01, 02, 03, 33 |
| Masterlist and access | One Submitted Masterlist with history; role-based view and extract; handler, conversion status and remarks | BRIDSP-04, 28, 29 |
| Rules and processing | Configurable sanitation, matching, classification and disposition criteria; automatic processing runs; fallout reports | BRIDSP-08, 09, 10 |
| Classification | Inforced / Submitted; qualification, non-renewal and inforced buckets; RA template choice | BRIDSP-11 to 15 |
| Policy review | IAAF per policy with reviews, approval matrix and signature | BRIDSP-05, 06, 07 |
| Limits and TOR | Limit breaches, Terms of Reference, TSU approval, hand-over to the AO | BRIDSP-16 to 19 |
| Renewal hand-off | Expiry detection, list of renewable policies, hand-off to Renewal, letters and proposals, hold cover re-assignment, placement and booking status | BRIDSP-22, 23, 25, 26, 27, 32 |
| Handling fee | Tagging of handling-fee payments in the Unapplied Payment List | BRIDSP-31 |
| Monitoring | Reports in several formats, notifications and alerts | BRIDSP-20, 21, 24, 30 |

**Out of scope for this phase:**

- The renewal process itself after the hand-off: renewal account, hold cover request, RA, NRNS, NAL and SFU letters. It belongs to BRD-6 Renewal, which implements the hand-off port (cross-BRD decision D2, R5). This document specifies what Submitted Policies does before and around the hand-off.
- Direct feeds from LFS, HLS, CIU, SPI and LAMD, OCR of scanned documents, a qualified electronic signature and the mail-house (COG) transport. They are built as ports whose default is an upload, manual entry, a stamped signature or a print batch.
- The ISYS reference and the ARF of the current process. The BIBS ARN replaces them (SP SQ20).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | BDOI Submitted Policies BRD (16 pages; pages 1-12 scanned) | v1.3, 20-Apr-2026; e-signed 24 to 28-Apr-2026 | `docs/source-documents/BRD - Submitted Policies (with e-sig MCM 4.24.2026).pdf` |
| R2 | BDOI Report List (Submitted Policies rows #133-#164) | 27-Apr-2026 | `docs/source-documents/Report List as of APR-27-2026.pdf` |
| R3 | Submitted Policies build design | current | `docs/architecture/SUBMITTED_POLICIES_DESIGN.md` |
| R4 | BDOI Submitted Policies (BRD-12) requirements baseline and fit/gap | current | `docs/requirements/BDOI_SP_BRD_SPEC.md` |
| R5 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R6 | Renewal build design (implementation of `RenewalHandOff`) and FRS BRD-6 Renewal | current | `docs/architecture/RENEWAL_DESIGN.md`; BIBS-FRS-BRD-06 |
| R7 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |
| R8 | BRD-1 New Business requirements baseline (shared platform capabilities) | current | `docs/requirements/BDOI_NB_BRD_SPEC.md` |

Page references in this document ("p.9") are pages of the BRD PDF (R1); "RL #nnn" is a row of the Report List (R2). The question prefix SQ is shared by two BRDs; this document writes the Submitted Policies questions as SP SQnn (cross-BRD question XQ07).

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing); MAO in the BRD process pages
ARN: Account Reference Number of a BIBS account (BRD-1)
BRIDSP: Requirement ID prefix of this BRD (BRIDSP-01 to BRIDSP-33)
Bucket: Qualification group of a policy after classification, which decides its next action (BRIDSP-12, 14, 15)
CBG: Consumer Banking Group segment (CBG Motor, CBG Fire)
CIU: Source report of CBG Motor policy details (not expanded in the BRD, SP SQ19)
CLPC: Payment channel whose payments carry the PN number (BRIDSP-31)
COG: Unit that prints and mails letters today (mail house)
FFY: Free First Year promotion of auto loans
HLS: Home loan system; source of the daily insurance report (CBG Fire)
IAAF: Insurance Adequacy Assessment Form, issued to the bank counterpart after a policy review
Inforced: Policy whose PN does not match an active LAMD loan (BRIDSP-15; definition SP SQ04)
LAMD: Bank loan unit whose loan report is matched by PN (not expanded in the BRD)
LFS: Loan system of the auto loans; source of the insurance report (CBG Motor)
LOV: List of values maintained by the business administrator
NAL: No Advice Letter
No Touch: Submitted CBG Motor accounts that BDOI sends to the insurer for validation and billing (Report List row 164)
NRNS: No Renew / No Submit letter or notification
OTC: Over-the-counter payment channel; payments carry a location reference number
PN: Promissory note number of the bank loan; the key of LAMD matching
RA: Renewal Advice letter (generic or FFY template)
RMU: Remedial Management Unit
SBM: Prefix of masterlist numbers (SBM-yyyy-nnnnnn) and of the module's codes
SFU: Letter to mortgaged CBG accounts (not expanded in the BRD)
SPI: Consolidated list of submitted policies (not expanded in the BRD)
SP SQnn: Open question on BRD-12 raised by the project team (section 10.3)
Submitted: Policy whose PN matches an active LAMD loan and that BDOI did not place (BRIDSP-12)
TOR: Terms of Reference, prepared when an account exceeds acceptance or coverage limits (BRIDSP-17)
TSU: Technical Support Unit
UPP: Unapplied Payment List
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor** (the BRD persona and the BIBS role), the BRD **priority**, the **fit** class of the baseline (R4), and the **screens** and **API** of the build design (R3).
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the business (rule sets, matrices, parameters, lists of values; section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. Codes of the Submitted Policies module are assigned at build ("To be assigned at build"). A code is quoted only where the check reuses a platform code that exists today. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**, **notifications**, **audit** and numbered **acceptance criteria**, the basis of the BRD-12 test plan.

The BRD writes its personas as "As a System", "As a Marketing User", "As an Account Officer", "As a Placement User", "As a Booking User" and "As a User". The FRs name the BIBS roles that act for them (section 3).

> [!NOTE]
> Values marked "default" (lead days, acceptance days, thresholds, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the platform built for BRD-1 |
| CONFIGURE | Needs set-up only (parameters, templates, rules) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-12 |

# Business context and process overview

## Business context

Today submitted policies are tracked per segment in Excel masterlists on a shared drive, with e-mail and SharePoint (p.4-5). CBG Motor Team Leads extract policy details from LFS and CIU and the Submitted Handler consolidates them within 30 days; CBG Fire policies come from Home Loan AOs and are reviewed for adequacy; Non-CBG Corporate and Branch policies are reviewed by a Policy Reviewer who issues an IAAF; Non-CBG Retail policies come from the LAMD report. At renewal a Sanitation Handler matches the list against LAMD, excludes accounts that must not be renewed, assigns an insurer and sends a proposal with a 30-day hold cover request, and an RA text file goes to COG for mailing.

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.4-7)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Policy details arrive as Excel reports and documents per segment | Each source is uploaded into BIBS; documents are extracted and confirmed by a user |
| 2 | Separate Excel masterlists per segment on Drive H:\ | One Submitted Masterlist with history and role-based access |
| 3 | Sanitation and LAMD matching by hand | Configurable rules sanitise, match, classify and bucket every policy; fallout is reported |
| 4 | Policy reviews and IAAFs by e-mail and paper signature | Reviews, IAAF and approval matrix in BIBS, with stamped signatures |
| 5 | Limit breaches handled by e-mail with TSU | TOR generated, approved by the TSU matrix and handed over to the AO |
| 6 | Renewal proposals, hold covers and RA text files prepared by hand | The expiry scan hands renewable policies to the Renewal module, which creates the account, requests the hold cover and sends the letters |
| 7 | Handling-fee payments filtered from the UPP list on SharePoint | Handling-fee payments tagged automatically by PN or location reference |
| 8 | Status monitored in the masterlists | Reports, alerts and notifications from live data |

## Process overview

The table lists the steps and Figure 1 shows them by actor. The masterlist record follows the workflow SBM_POLICY (section 5.1).

<!-- table: widths=0.8,3.8,3.4,7,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Intake | Handlers; System | Source files uploaded and validated; one masterlist record per policy; migration of the Excel masterlists | BRIDSP-01, 33 |
| 2 | Extraction and confirmation | Handler, Policy Reviewer | Fields proposed from the policy document; saved only after confirmation; manual entry | BRIDSP-02, 03 |
| 3 | Masterlist | All | Record, history, handler, conversion status, remarks; role-based view and extract | BRIDSP-04, 28, 29 |
| 4 | Processing run | System | Sanitation, matching against LAMD by PN, classification, disposition, limits | BRIDSP-08, 09 |
| 5 | Classification and buckets | System | Inforced / Submitted; buckets; renewal tag; RA template | BRIDSP-11 to 15 |
| 6 | Fallout | Sanitation Handler | Exceptions and failures listed with reason codes and resolved | BRIDSP-10 |
| 7 | Policy review and IAAF | Policy Reviewer; approvers | Reviews recorded; IAAF generated, approved by the matrix and issued to the bank counterpart | BRIDSP-05, 06, 07 |
| 8 | Limits and TOR | Marketing user; TSU | Limit breach flagged; TOR prepared, approved and handed over to the AO | BRIDSP-16 to 19 |
| 9 | Expiry scan and hand-off | System; Marketing user | Renewable policies within the lead days are handed to Renewal with the assigned insurer | BRIDSP-23, 25 |
| 10 | Renewal | Renewal module (BRD-6) | Renewal account, 30-day hold cover, RA and letters; insurer re-assignment when the insurer does not accept | BRIDSP-22, 26, 32 |
| 11 | Placement and booking | Placement and Booking users (BRD-1) | Placement file to the insurer; booking; masterlist shows BOOKED | BRIDSP-26, 27 |
| 12 | Monitoring | All | Reports, alerts and notifications | BRIDSP-20, 21, 24, 30 |
| - | Handling fee | System; UPP handler | Handling-fee payments tagged in the Unapplied Payment List | BRIDSP-31 |

![Submitted Policies process by actor (BRIDSP-01-33)](figures/brd12_process_flow.dot)

## Segments and business types

<!-- table: widths=3.6,6.5,6.5 caption="Segments (BRD p.2, 4-5)" -->
| Segment | New business intake | Renewal |
|---|---|---|
| CBG Motor | LFS and CIU reports; masterlist within 30 days of extraction | Match with LAMD; exclude FFY, BDO / SM Group employees and No Touch; insurer by vehicle type; hold cover; RA 90 days before expiry; NRNS list for call-out |
| CBG Fire | Home Loan AO sends the documents; adequacy review; IAAF to the Home Loan AO; HLS daily report | Match with LAMD; insurer different from the expiring one; hold cover; RA |
| Non-CBG Corporate and Branches | Policy Reviewer receives the documents from IBG / Leasing, reviews them and issues an IAAF | Same as new business; conversion opportunity monitored |
| Non-CBG Retail | Marketing AO encodes the policies of the LAMD report | AO asks an insurer to quote on the expiring terms; quotation, placement and booking; NRNS on decline |

Each record carries its business type, New Business (NB) or Renewal Business (RB), as the BRD's process pages separate them (p.4-5).

# Personas and roles

## Personas

<!-- table: widths=3.4,3.3,7.9,3 caption="Personas and BIBS roles" -->
| Persona (current process) | BIBS role | Responsibilities in Submitted Policies | BRD persona |
|---|---|---|---|
| Submitted Handler (CBG) | SBM_HANDLER | Uploads sources; confirms extractions; maintains records; prepares IAAF and TOR | System, Marketing User |
| Submitted Checker | SBM_CHECKER | Approves IAAFs (level 1) | System |
| Sanitation Handler | SBM_SANITATION | Uploads sources and LAMD; runs processing; resolves fallout; hand-off; sends letters | System, Marketing User |
| Team Lead | SBM_TL | All Submitted Policies functions except rule approval; assigns handlers | Marketing User |
| Policy Review Officer (Non-CBG Corporate) | SBM_POLICY_REVIEWER | Reviews policies; prepares IAAFs | Marketing User |
| Rule administrator | SBM_RULE_ADMIN | Maintains rule sets, limits, insurer and letter rules, matrices (maker) | Marketing User (BRIDSP-08) |
| Admin Team UPP handler | SBM_UPP_HANDLER | Handling-fee records and tagging results | System (BRIDSP-31) |
| Marketing AO / MAO | MKT_AO | Own records; manual entry; receives TORs; Renew with BDOI | Account Officer, Marketing User |
| NB Team Lead | MKT_TL | Views records; approves IAAFs (level 2) and rule sets | Marketing User |
| TSU | TSU | Approves TORs | System (TSU matrix) |
| Placement and Booking users | PROCESSOR and booking roles | Place and book the renewal accounts (BRD-1 screens) | Placement User, Booking User |

The BRD does not give an access matrix; the roles above are the project's proposal until BDOI confirms them (SP SQ15, OQ48).

## Permissions

<!-- table: widths=4.8,11.8 caption="Submitted Policies permissions" -->
| Permission | Allows |
|---|---|
| SBM_VIEW | Masterlist, records, home page (within the user's scope) |
| SBM_MAINTAIN | Manual entry and edit, renewal tag, handler, remarks, document upload, confirm extraction |
| SBM_INTAKE | Source uploads, intake runs, LAMD snapshot upload |
| SBM_PROCESS | Processing runs, fallout resolution, manual disposition, renewal hand-off, insurer re-assignment |
| SBM_RULE_MAINTAIN, SBM_RULE_APPROVE | Rule sets, limit, insurer and letter rules, approval matrices (maker and checker) |
| IAAF_PREPARE, IAAF_APPROVE | Reviews and IAAF; IAAF approval levels |
| TOR_PREPARE, TOR_APPROVE | TOR; TSU approval levels |
| SBM_LETTER_SEND | Generate, send and print the letters of this module; print batches |
| SBM_HANDLING_FEE | Handling-fee records, tagger results, manual tag |
| SBM_MIGRATE | Migration upload |
| SBM_EXPORT | Masterlist extract |
| SBM_REPORT_VIEW, SBM_REPORT_EXPORT | Submitted Policies reports (view; export and archive) |

## Permissions matrix

<!-- table: widths=4.1,1.25,1.25,1.25,1.25,1.25,1.25,1.25,1.25,1.25,1.25 caption="Role-to-permission matrix (proposal until SP SQ15 / OQ48)" size=8 -->
| Permission | SBM Hand. | SBM Check. | Sanit. | SBM TL | Pol. Rev. | Rule Admin | UPP | MKT AO | MKT TL | TSU |
|---|---|---|---|---|---|---|---|---|---|---|
| SBM_VIEW | Y | Y | Y | Y | Y | | Y | Y (own) | Y | |
| SBM_MAINTAIN | Y | | Y | Y | Y | | | Y | | |
| SBM_INTAKE | Y | | Y | Y | | | | | | |
| SBM_PROCESS | | | Y | Y | | | | | | |
| SBM_RULE_MAINTAIN | | | | Y | | Y | | | | |
| SBM_RULE_APPROVE | | | | | | | | | Y | |
| IAAF_PREPARE | Y | | | Y | Y | | | | | |
| IAAF_APPROVE | | L1 | | Y | | | | | L2 | |
| TOR_PREPARE | Y | | | Y | | | | Y | | |
| TOR_APPROVE | | | | | | | | | | Y |
| SBM_LETTER_SEND | | | Y | Y | | | | | | |
| SBM_HANDLING_FEE | | | | Y | | | Y | | | |
| SBM_MIGRATE | | | | Y | | | | | | |
| SBM_EXPORT | Y | | Y | Y | | | | | | |
| SBM_REPORT_VIEW | Y | Y | Y | Y | Y | | | | | |
| SBM_REPORT_EXPORT | Y | | Y | Y | | | | | | |

"L1" and "L2" are IAAF approval levels of the seed matrix. Placement and Booking users also receive SBM_VIEW.

**Data scope.** Every query, report and export is limited to the user's scope: a list of segments, and for Marketing AOs only the records they handle or whose renewal account they own (BRIDSP-28).

**Segregation of duties.** A rule set is approved by someone other than its maker; an IAAF or TOR level is never approved by the preparer.

# Functional requirements

## Intake of submissions

```fr
id: FR-SP-001
title: Receive submitted policy details from the approved sources
brd: [BRIDSP-01 (p.8)]
actor: System; Submitted Handler, Sanitation Handler (SBM_INTAKE)
priority: Must have
fit: CHANGE
screens: Upload & Intake (source uploads, intake runs)
api: Bulk handlers SBM_LFS_INSURANCE, SBM_HLS_INSURANCE, SBM_CIU, SBM_SPI, SBM_LOAN_BOOKING, SBM_LAMD, SBM_IA_MASTERLIST
description:
  - BIBS keeps a register of the approved sources (LFS insurance report, HLS daily insurance report, CIU report, SPI list, LAMD report, Loan Booking Report, IA masterlist, IBG / Leasing documents, manual entry, migration) with segment, format and upload template. Each source file is uploaded with its own template, validated row by row and recorded as an intake run.
  - Each valid row creates or updates one masterlist record on the natural key (segment, business type, PN or policy number, expiry date). New records get the number SBM-yyyy-nnnnnn and status RECEIVED. A file already uploaded (same content) is refused.
  - Transports from the bank systems are parked; the default is an upload.
preconditions:
  - The user has SBM_INTAKE.
main_flow:
  - The user opens Upload & Intake and chooses the source.
  - The user downloads the template if needed, and uploads the file.
  - BIBS validates the rows and shows valid and invalid counts.
  - The user confirms; BIBS creates or updates the records, records the run (SBI-yyyy-nnnnnn) and starts a processing run for them (FR-SP-021).
alternate_flows:
  - Invalid rows. They are listed with the reason and can be downloaded; valid rows are committed.
  - Duplicate file. BIBS refuses it and names the earlier run.
rules:
  - [R1, "One upload template per source layout; layouts to be supplied by BDOI (SP SQ01, SQ02).", Configurable, Source register and bulk templates]
  - [R2, "Natural key - segment, business type, PN or policy no., expiry date; a changed row writes the record history.", Fixed, "-"]
  - [R3, "A file is identified by its SHA-256; the same file cannot be loaded twice.", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, BULK_FILE_TYPE]
  - [File without header row, The file has no header row, BULK_FILE_EMPTY]
  - [Same file loaded before, "This file was already loaded in run <run no.>", To be assigned at build]
  - [Mandatory column blank, "Row <n>: <column> is required", To be assigned at build]
fields_screen: Source upload
fields:
  - [Source, List, "Yes", Source register, Active source]
  - [File, Attachment, "Yes", Template of the source, "Excel or CSV; platform file rules"]
notifications:
  - "SBM_NEW_SUBMISSION to the handlers of the segment; SBM_INTAKE_FAILED alert on failure."
audit:
  - "Intake run with source, file name, hash, counts (received, created, updated, duplicate, failed), user and time."
acceptance:
  - An LFS insurance report of 300 rows creates 300 CBG Motor records with status RECEIVED.
  - Loading the same file a second time is refused.
  - A row updating an existing policy changes the record and writes the history.
```

```fr
id: FR-SP-002
title: Extract policy details from documents and save them after confirmation
brd: [BRIDSP-02 (p.8)]
actor: System; Submitted Handler, Policy Reviewer (SBM_MAINTAIN)
priority: Must have
fit: CHANGE
screens: Extraction Review (proposal side by side with the record)
api: POST /api/v1/submitted/extractions; POST .../{id}/confirm | reject
description:
  - The user uploads a policy document (policy copy, scanned or PDF) to a new or existing record. BIBS extracts the fields - assured, PN, policy number, insurer, period, sum insured, premium, vehicle or property details - and shows each proposed value with its confidence next to an editable field.
  - Nothing is saved to the masterlist before the user confirms. On **Confirm** the fields are written and the record becomes VALIDATED; on **Reject** the file is kept with a reason and the user enters the data by hand.
preconditions:
  - The user has SBM_MAINTAIN.
main_flow:
  - The user uploads the document on the record, or as a new record.
  - BIBS extracts the fields and opens Extraction Review.
  - The user checks and corrects the values and clicks **Confirm**.
alternate_flows:
  - Scanned document without text. Extraction returns "not readable" (OCR is parked, SP SQ03); the user enters the fields by hand (FR-SP-003).
  - Reject. The user rejects the proposal with a reason.
rules:
  - [R1, "Extraction never writes to the masterlist without confirmation.", Fixed, "-"]
  - [R2, "Extraction patterns per insurer are maintained as data.", Configurable, Extraction patterns (kind SUBMITTED_POLICY)]
validations:
  - [Confirm with a mandatory field blank, "<Field> is required", "-"]
  - [Reject without reason, Enter the reason of the rejection, "-"]
fields_screen: Extraction Review
fields:
  - [Assured / PN / Policy no. / Insurer, Text and list, "Yes", Proposal and masters, "-"]
  - [Inception / Expiry, Date, "Yes", Proposal, Expiry after inception]
  - [Sum insured / Premium, Amount, "Yes", Proposal, ">= 0"]
  - ["Risk details (unit, serial, motor no., plate / location, occupancy)", Text, Conditional, Proposal, By segment]
notifications:
  - "SBM_MANUAL_VALIDATION to the handlers when a document waits for confirmation."
audit:
  - "Proposal, confirmation or rejection with user and time; the document is kept as an attachment."
acceptance:
  - A text PDF of a motor policy produces a proposal; after confirmation the record shows the confirmed values and status VALIDATED.
  - Before confirmation the masterlist is unchanged.
  - A scanned document without text leads to manual entry.
```

```fr
id: FR-SP-003
title: Create or update a policy manually and tag its renewal opportunity
brd: [BRIDSP-03 (p.8)]
actor: Marketing user (Submitted Handler, Policy Reviewer, Marketing AO; SBM_MAINTAIN)
priority: Must have
fit: NEW
screens: Masterlist (New Policy); Policy record (Edit, Tag Renewable / Non-Renewable)
api: POST /api/v1/submitted/policies; PUT .../{id}; POST .../{id}/renewal-tag
description:
  - A Marketing user creates a policy record by hand when data is incomplete or cannot be extracted, or updates an existing one. Mandatory fields depend on the segment.
  - The user tags a record Renewable or Non-Renewable with a reason. The tag is saved with user and time, shown as a flag chip on the record and available in filters and reports. A manual tag overrides the rule result; the processing run records the rule result but keeps the manual tag.
preconditions:
  - The user has SBM_MAINTAIN.
main_flow:
  - The user clicks **New Policy** (or **Edit** on a record) and enters the fields.
  - The user saves; the record is VALIDATED.
  - The user clicks **Tag Renewable** or **Tag Non-Renewable**, selects the reason and saves.
rules:
  - [R1, "Mandatory fields per segment are those of the MANUAL source.", Configurable, Source register]
  - [R2, "A manual tag overrides the rule tag until it is changed by hand.", Fixed, "-"]
  - [R3, "Who may override a rule-set tag is to be confirmed (SP SQ17).", Configurable, Permissions]
validations:
  - [Mandatory field blank, "<Field> is required", "-"]
  - [Non-Renewable without reason, Select the reason for Non-Renewable, "-"]
  - [Same policy already in the masterlist, "Policy <no.> is already in the masterlist (<SBM no.>)", To be assigned at build]
fields_screen: Policy record (main fields)
fields:
  - [Segment, List, "Yes", LOV SBM_SEGMENT, "-"]
  - [Business type, Option, "Yes", "NB, RB", "-"]
  - [PN no. / Loan application no. / CIF, Text, Conditional, "-", PN required for CBG]
  - [Borrower / Assured / Mailing address / Contacts, Text, "Yes", "-", "Assured required"]
  - [Insurer / Policy no., List / Text, "Yes", Insurer master, "-"]
  - [Inception / Expiry, Date, "Yes", "-", Expiry after inception]
  - [Amount insured / Total premium, Amount, "Yes", "-", ">= 0"]
  - [Risk details, Text, Conditional, "-", Unit and serial for Motor; location for Fire]
  - [Renewal tag, Option, "No", "Renewable, Non-Renewable", "-"]
  - [Reason, List, Conditional, LOV SBM_NON_RENEWAL_REASON, Required for Non-Renewable]
notifications:
  - "None."
audit:
  - "Create, edit and tag with before and after values, user and time."
acceptance:
  - A manually created Non-CBG Corporate policy is saved and appears in the masterlist.
  - A record tagged Non-Renewable shows the chip and appears in the Non-Renewal filter and report.
  - A later processing run does not change a manual tag.
```

```fr
id: FR-SP-004
title: Migrate the existing Excel masterlists
brd: [BRIDSP-33 (p.12)]
actor: System; Team Lead (SBM_MIGRATE)
priority: Must have
fit: CHANGE
screens: Upload & Intake (Migration); Reports (SBM-MIGRATION-ERRORS)
api: Bulk handler SBM_MIGRATION
description:
  - The existing masterlists in Excel (NB Motor, RB Motor, Fire, Non-CBG) are read with a column mapping per legacy layout. Valid records are migrated completely, keep their original submission status (mapped to a BIBS status and bucket), submission date and reference ID, and are flagged Migrated. Invalid, incomplete or failed records are captured in an error log with reason codes.
  - The migration can be run again; it is idempotent on the legacy reference.
preconditions:
  - The user has SBM_MIGRATE; the status map is set up.
main_flow:
  - The user uploads a legacy file and chooses its layout.
  - BIBS validates, maps the statuses and shows the result.
  - The user confirms; BIBS creates the records and writes the errors to the log.
rules:
  - [R1, "Legacy status to BIBS status and bucket through the status map.", Configurable, Status map]
  - [R2, "Migrated records keep legacy reference and date received; flag Migrated.", Fixed, "-"]
validations:
  - [Status not in the map, "Row <n>: status <value> has no mapping", To be assigned at build]
  - [Mandatory column blank, "Row <n>: <column> is required", To be assigned at build]
fields_screen: Migration upload
fields:
  - [Layout, List, "Yes", "NB Motor, RB Motor, Fire, Non-CBG", "-"]
  - [File, Attachment, "Yes", "-", ".xlsx"]
notifications:
  - "None."
audit:
  - "Migration job with counts; error log rows with reason codes (report SBM-MIGRATION-ERRORS)."
acceptance:
  - A legacy file with 1,000 rows and 12 invalid rows migrates 988 records flagged Migrated and logs 12 errors with reason codes.
  - A migrated record shows its original date received and legacy reference.
  - Running the same file again creates no duplicates.
```

## Masterlist and access

```fr
id: FR-SP-010
title: Keep one Submitted Masterlist
brd: [BRIDSP-04 (p.9)]
actor: System; all Submitted Policies users
priority: Must have
fit: NEW
screens: Masterlist (tabs All, For Validation, Classified, For Renewal, Manual Disposition, Non-Renewal, Fallout); Policy record
api: GET /api/v1/submitted/policies; GET .../{id}
description:
  - All submitted policy details are held in one masterlist, one record per submitted or inforced policy, segment and business type - with loan data, risk details, insurer and policy, classification, bucket, renewal tag, handler, conversion status and the linked renewal ARN and booked invoice. When a record is saved or updated, the masterlist shows the latest data.
  - The Policy record page has the summary card with flag chips (Renewable, FFY, No Touch, Migrated, Insurer approval) and the tabs Details, Loan & Matching, Rule Results, Review & IAAF, TOR, Renewal, Letters, Documents and History.
preconditions:
  - The user has SBM_VIEW.
main_flow:
  - The user opens the Masterlist, chooses a tab and filters by segment, business type, bucket, expiry month, insurer, handler, conversion status or Migrated.
  - The user opens a record.
rules:
  - [R1, "Every change writes the record history and the audit trail.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Record history per field with source (intake, extraction, manual, run, renewal, booking)."
acceptance:
  - After a record is updated by an upload, the masterlist shows the new values at once.
  - Filters by segment and expiry month return only the matching records.
```

```fr
id: FR-SP-011
title: View and extract the masterlist by role
brd: [BRIDSP-28 (p.12)]
actor: User (SBM_VIEW, SBM_EXPORT)
priority: Must have
fit: CHANGE
screens: Masterlist (Export); Reports (SBM-MASTERLIST)
api: GET /api/v1/reports/SBM-MASTERLIST/export?format=
description:
  - Viewing and downloading the masterlist follow role-based access. Viewing needs SBM_VIEW and extracting needs SBM_EXPORT. Every record, list, report and extract is limited to the user's scope - the segments of the user, and for Marketing AOs only their own records.
  - The extract is the report SBM-MASTERLIST with the fields of Report List row 151 per segment, including migrated records, in XLSX, CSV or PDF, archived.
preconditions:
  - "The user has SBM_VIEW."
main_flow:
  - The user filters the masterlist and clicks **Export**.
  - BIBS produces the extract within the user's scope and archives it.
alternate_flows:
  - User without SBM_EXPORT. The Export button is not shown; a direct call is refused.
rules:
  - [R1, "Scope by segment; own records only for AOs (user scope table).", Configurable, User scope (Setup)]
validations:
  - [Export without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Every view of a record and every extract with user and time."
acceptance:
  - A CBG Motor handler does not see CBG Fire records.
  - An AO sees only his records.
  - A user without SBM_EXPORT cannot extract the masterlist.
```

```fr
id: FR-SP-012
title: Monitor handler, conversion status and remarks
brd: [BRIDSP-29 (p.12)]
actor: Marketing user (SBM_MAINTAIN)
priority: Must have
fit: NEW
screens: Masterlist (Assign Handler; quick filter Changed since); Policy record (History)
api: PUT /api/v1/submitted/policies/{id} (handler, conversion status, remarks)
description:
  - The user updates the handler, the conversion status and the remarks of a record, one by one or in bulk (Assign Handler). The masterlist shows the latest values; the History tab shows every change per field; a "Changed since" quick filter lists the records changed after a date.
preconditions:
  - The user has SBM_MAINTAIN.
main_flow:
  - The user edits the handler, conversion status or remarks and saves.
  - BIBS records the change and shows it in the list.
rules:
  - [R1, "Conversion statuses in LOV SBM_CONVERSION_STATUS (Renewed, Unrenewed, Process Placement per group; RL #163).", Configurable, LOV SBM_CONVERSION_STATUS]
validations:
  - [Handler not an active user, "<user> is not an active user", To be assigned at build]
fields_screen: Policy record (tracking)
fields:
  - [Handler, Look-up, "No", Users with SBM_MAINTAIN, Active user]
  - [Conversion status, List, "No", LOV SBM_CONVERSION_STATUS, "-"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - "The new handler is notified."
audit:
  - "Field change log with old and new value, user and time."
acceptance:
  - A handler change shows at once in the masterlist and in the record history.
  - The quick filter Changed since yesterday lists the records updated today.
```

## Rules and processing

```fr
id: FR-SP-020
title: Define sanitation, matching, classification and disposition criteria
brd: [BRIDSP-08 (p.9)]
actor: Marketing user as rule administrator (SBM_RULE_MAINTAIN); approver (SBM_RULE_APPROVE)
priority: Must have
fit: NEW
screens: Submitted Policies Setup (Rule Sets)
api: /api/v1/submitted/setup/rule-sets
description:
  - The criteria of each processing step are rule sets - per step (Sanitation, Matching, Classification, Disposition), segment and business type - with versions and effective dates. A rule has a priority, conditions (field, operator, value), an outcome (bucket, tag, classification, RA template, flag), a reason code and a stop flag.
  - A new or changed rule set is approved by someone other than its maker before it applies. Every run result stores the rule id and rule-set version, so the rules are applied consistently and explained.
  - The seeds are the BRD lists (section 9.2); rules whose meaning is disputed are seeded inactive until SP SQ05 is answered.
preconditions:
  - The user has SBM_RULE_MAINTAIN.
main_flow:
  - The user creates a draft version of a rule set and edits its rules.
  - The user submits it; an approver compares it with the active version and approves it with an effective date.
alternate_flows:
  - The approver rejects the version with a reason; it stays a draft.
  - Rules can be imported from a file (upload SBM_RULES).
rules:
  - [R1, "The first matching rule with stop = true ends the step; a record with no matching rule is fallout (reason SBM_NO_RULE).", Fixed, "-"]
  - [R2, "Maker-checker on every rule set.", Fixed, "-"]
validations:
  - [Approval by the maker, A rule set is approved by someone other than its maker, To be assigned at build]
  - [Rule without outcome, Select the outcome of the rule, "-"]
  - [Condition on an unknown field, "Field <name> cannot be used in a rule", To be assigned at build]
fields_screen: Rule
fields:
  - [Priority, Number, "Yes", "-", Unique in the version]
  - [Conditions, Table, "Yes", "Field / operator / value", At least one]
  - [Outcome, Composite, "Yes", "Bucket (LOV SBM_BUCKET), tag, classification, RA template, flag", "-"]
  - [Reason code, List, "No", LOV SBM_REASON, "-"]
  - [Stop, Check box, "No", "-", "-"]
notifications:
  - "Approvers see pending rule sets in My Approvals."
audit:
  - "Versions with maker, approver, dates and differences."
acceptance:
  - A new exclusion rule for SM Group employee accounts applies to the next run only after the approver approves it.
  - Each run result shows the rule and version that produced it.
```

```fr
id: FR-SP-021
title: Run processing automatically after intake
brd: [BRIDSP-09 (p.9)]
actor: System (SbmProcessingService, job SBM_PROCESSING); Sanitation Handler (SBM_PROCESS)
priority: Must have
fit: NEW
screens: Processing Runs (list, run detail); Masterlist (Run Processing)
api: POST /api/v1/submitted/runs; job SBM_PROCESSING
description:
  - When documents or files are uploaded and confirmed, BIBS runs the processing steps in order - sanitation, matching against LAMD, classification, disposition and limits - and triggers the workflow of each record by its result (renewal hand-off, manual disposition queue, non-renewal, policy review, TOR). Account tagging is applied as the criteria define.
  - A run also starts from the daily job at 21:30 PHT and on demand. Each run (SBR-yyyy-nnnnnn) records one result per record and step.
preconditions:
  - "Active rule sets exist for the steps."
main_flow:
  - An intake commit, the job or a user starts the run for a scope.
  - BIBS runs the steps set-based and writes the results.
  - BIBS moves each record in SBM_POLICY and notifies the owners of the new bucket.
rules:
  - [R1, "Step order - SANITATION, MATCHING, CLASSIFICATION, DISPOSITION, LIMITS.", Fixed, "-"]
  - [R2, "Sanitation marks duplicates (same PN or same serial / motor number) For Review and incomplete records as fallout.", Configurable, Sanitation rule set]
  - [R3, "Matching checks PN against the LAMD snapshot and PN against serial / motor number and assured name / address / contacts (RL #151).", Configurable, Matching rule set]
  - [R4, "Job time 21:30 PHT.", Configurable, Job schedule]
validations: []
notifications:
  - "SBM_BUCKET_CHANGED to the handlers; SBM_FALLOUT alert when the run has fallout."
audit:
  - "Run with trigger, scope, counts per outcome and job run; result rows per record and step."
acceptance:
  - Uploading an LFS file starts a run; each new record ends with a classification and a bucket or fallout.
  - A record matched to an active LAMD loan is moved to the right bucket and the handlers are notified.
```

```fr
id: FR-SP-022
title: Report fallout after processing
brd: [BRIDSP-10 (p.9)]
actor: System; Sanitation Handler
priority: Must have
fit: CHANGE
screens: Processing Runs (Fallout tab); Masterlist (Fallout tab); Reports (SBM-DOC-FALLOUT, SBM-PROCESS-FALLOUT)
api: Reports SBM-DOC-FALLOUT, SBM-PROCESS-FALLOUT
description:
  - When document processing or a run ends with errors or exceptions, BIBS produces the fallout - intake and extraction failures (SBM-DOC-FALLOUT) and the records that no rule could place or that failed a step (SBM-PROCESS-FALLOUT), each with its reason code. The fallout of each run is archived and the handler resolves it on the record (correct and re-run, dispose by hand, or exclude).
preconditions:
  - "A run or intake has ended."
main_flow:
  - The run ends; BIBS writes the fallout rows and raises SBM_FALLOUT when the count is above zero.
  - The handler opens the Fallout tab, corrects the records and re-runs them.
rules:
  - [R1, "Every fallout row has a reason code (LOV SBM_REASON).", Fixed, "-"]
validations: []
notifications:
  - "SBM_FALLOUT to the Sanitation Handlers."
audit:
  - "Fallout rows per run; resolution on the record history."
acceptance:
  - A run with five records without PN produces a fallout report listing the five with their reason code.
  - A corrected record leaves the fallout after the next run.
```

## Classification and buckets

```fr
id: FR-SP-030
title: Classify policies as Inforced or Submitted
brd: [BRIDSP-11 (p.9)]
actor: System
priority: Must have
fit: NEW
screens: Policy record (Loan & Matching, Rule Results); Reports (SBM-CLASSIFICATION)
api: Classification step
description:
  - When the policy details are evaluated, the classification step sets the record Submitted when its PN matches an active LAMD loan and BDOI did not place the policy, and Inforced otherwise, so the right renewal and disposition rules apply. The definition is rule data and is to be confirmed (SP SQ04).
preconditions:
  - "The matching step has run."
main_flow:
  - BIBS evaluates the classification rules on the matching result and stores the classification.
rules:
  - [R1, "Default - PN matched to an active LAMD loan gives SUBMITTED; unmatched gives INFORCED.", Configurable, Classification rule set (SP SQ04)]
validations: []
notifications:
  - "None."
audit:
  - "Result row with rule and version."
acceptance:
  - A record whose PN matches an active LAMD loan is Submitted; a record whose PN is not in LAMD is Inforced.
```

```fr
id: FR-SP-031
title: Group Submitted policies into qualification buckets
brd: [BRIDSP-12 (p.9)]
actor: System
priority: Must have
fit: NEW
screens: Masterlist (bucket tabs and filter); Policy record
api: Disposition step
description:
  - Submitted policies (PN matched against LAMD) are grouped into For Renewal, For Manual Disposition, Non-Renewal, No Touch, FFY, BDO / SM Group Employee Accounts or RMU. Entering a bucket triggers the next workflow - renewal hand-off at the lead days, the manual disposition queue, or exclusion with a fallout entry.
preconditions:
  - "The record is classified Submitted."
main_flow:
  - BIBS evaluates the disposition rules and stores the bucket, the renewal tag and the reason.
  - BIBS moves the record to FOR_RENEWAL, FOR_MANUAL_DISPOSITION or EXCLUDED.
alternate_flows:
  - Manual disposition. A Sanitation Handler disposes the record by hand with a reason (For Renewal or Exclude).
rules:
  - [R1, "Buckets are a list with the attribute renewal_action (RENEW, MANUAL, EXCLUDE).", Configurable, LOV SBM_BUCKET]
  - [R2, "Bucket precedence and the RMU / FFY conflicts are open (SP SQ05); the conflicting rules are seeded inactive.", Configurable, Disposition rule set]
validations:
  - [Manual disposition without reason, "Select a reason for 'Dispose'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "SBM_BUCKET_CHANGED to the handlers of the segment."
audit:
  - "Bucket with rule and version; manual dispositions with user and reason."
acceptance:
  - A Submitted CBG Motor policy of an active loan with no exclusion goes to For Renewal.
  - A policy on the employee list goes to BDO / SM Group Employee Accounts and is not handed to renewal.
```

```fr
id: FR-SP-032
title: Choose the RA template from the LAMD loan
brd: [BRIDSP-13 (p.10)]
actor: System
priority: Must have
fit: NEW
screens: Upload & Intake (LAMD snapshot); Policy record (Loan & Matching)
api: Bulk handler SBM_LAMD; disposition step
description:
  - The LAMD snapshot holds per PN the loan status, the amortised flag and the maturity date. For an active loan that is not amortised, the disposition rules mark the record ready for the Generic RA or the FFY RA template, and the next workflow is triggered. The chosen template travels with the renewal hand-off; the RA itself is generated by the Renewal module.
preconditions:
  - A LAMD snapshot is loaded.
main_flow:
  - The Sanitation Handler uploads the LAMD snapshot.
  - The run reads the loan and sets the RA template on the record.
rules:
  - [R1, "Rule for Generic vs FFY RA to be confirmed (SP SQ06).", Configurable, Disposition rule set]
validations:
  - [Snapshot row without PN, "Row <n>: PN is required", To be assigned at build]
notifications:
  - "None."
audit:
  - "Snapshot upload and the template decision per record."
acceptance:
  - An FFY account with an active non-amortised loan gets the FFY RA template.
```

```fr
id: FR-SP-033
title: Assign policies to non-renewal buckets
brd: [BRIDSP-14 (p.10)]
actor: System
priority: Must have
fit: NEW
screens: Masterlist (Non-Renewal tab); Reports (SBM-NON-RENEWAL)
api: Disposition step; report SBM-NON-RENEWAL
description:
  - Policies that meet the non-renewal criteria (Free First Year, SM Group Employee, No Touch, CARI, Bonds, RMU and the other exclusions of Report List row 156) are tagged Non-Renewal, excluded from renewal and recorded in the non-renewal report with the exclusion reason. A manual tag can override the result (FR-SP-003).
preconditions:
  - "The disposition step runs."
main_flow:
  - BIBS applies the non-renewal rules and stores the tag and reason.
  - The record moves to EXCLUDED and appears in SBM-NON-RENEWAL.
alternate_flows:
  - Reinstate. A handler reinstates an excluded record with a reason; it is classified again at the next run.
rules:
  - [R1, "Exclusion reasons in LOV SBM_NON_RENEWAL_REASON.", Configurable, LOV SBM_NON_RENEWAL_REASON]
validations:
  - [Reinstate without reason, "Select a reason for 'Reinstate'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "SBM_BUCKET_CHANGED."
audit:
  - "Tag and reason with rule and version."
acceptance:
  - A No Touch account is tagged Non-Renewal and listed in the non-renewal report with reason No Touch.
  - An excluded record is never handed to renewal.
```

```fr
id: FR-SP-034
title: Group Inforced policies and create RAs and proposals
brd: [BRIDSP-15 (p.10)]
actor: System
priority: Must have
fit: NEW
screens: Masterlist; Policy record (Renewal tab)
api: Disposition step; RenewalHandOff
description:
  - Inforced policies (PN not matched to LAMD) are grouped into Fully Paid Loan, OPM, BDOFC or Sold. For an eligible inforced policy, the RA and the renewal proposal are generated - by the Renewal module after the hand-off (decision D2), with the RA template of its bucket and the proposal template SBM_RENEWAL_PROPOSAL.
preconditions:
  - "The record is classified Inforced."
main_flow:
  - BIBS evaluates the inforced bucket rules and stores the bucket.
  - For an eligible bucket, the record becomes For Renewal and is handed over at the lead days (FR-SP-060).
rules:
  - [R1, "Inforced buckets Fully Paid Loan, OPM, BDOFC, Sold; eligibility per bucket (SP SQ05).", Configurable, Disposition rule set]
validations: []
notifications:
  - "SBM_BUCKET_CHANGED."
audit:
  - "Bucket and eligibility with rule and version."
acceptance:
  - An inforced Fully Paid Loan policy marked eligible reaches For Renewal and, after hand-off, has an RA and a proposal.
```

## Policy review and IAAF

```fr
id: FR-SP-040
title: Record policy reviews and generate one IAAF per policy
brd:
  - BRIDSP-05 (p.9)
  - BRIDSP-06 (p.9)
actor: Marketing user (Policy Reviewer, Submitted Handler; IAAF_PREPARE)
priority: Must have
fit: NEW
screens: Policy Reviews / IAAF (review queue, IAAF page); Policy record (Review & IAAF tab)
api: POST /api/v1/submitted/policies/{id}/reviews; POST .../iaaf
description:
  - Non-CBG Corporate and Branch records and CBG Fire records with documents enter the review queue. The reviewer records each review - review number, date, adequacy (Adequate or With findings), findings and remarks. With findings, the reviewer e-mails the bank counterpart from the record and a new review follows. The IAAF shows the review history and the review count.
  - When the policy is adequate, the reviewer generates the IAAF (IAAF-yyyy-nnnnnn). Only one IAAF exists per policy; it can be linked to related policies (for example the previous term or the same borrower) by their reference numbers.
preconditions:
  - The record is IN_REVIEW; the user has IAAF_PREPARE.
main_flow:
  - The reviewer opens the record from the review queue and records the review.
  - When adequate, the reviewer clicks **Generate IAAF** and links related policies.
  - The reviewer submits the IAAF for approval (FR-SP-041).
alternate_flows:
  - With findings. The reviewer sends the findings to the bank counterpart; the record stays IN_REVIEW.
rules:
  - [R1, "One IAAF per policy; the BRD's current-process note that one IAAF may cover several policies is handled with links (SP SQ07).", Fixed, "-"]
  - [R2, "Template SBM_IAAF; draft until BDOI provides the layout.", Configurable, Document templates]
validations:
  - [Second IAAF for the policy, "Policy <SBM no.> already has IAAF <no.>", To be assigned at build]
  - [Adequacy not selected, Select the adequacy, "-"]
  - [IAAF with findings open, The last review has findings; record an adequate review first, To be assigned at build]
fields_screen: Review
fields:
  - [Review date, Date, "Yes", "-", Not in the future]
  - [Adequacy, Option, "Yes", "Adequate, With findings", "-"]
  - [Findings, Multi-select, Conditional, LOV SBM_IAAF_FINDING, Required for With findings]
  - [Remarks, Text, "No", "-", "-"]
  - [Related policies, Look-up, "No", Masterlist, By SBM number]
notifications:
  - "The bank counterpart receives the findings by e-mail from the record."
audit:
  - "Each review and the IAAF generation with user and time."
acceptance:
  - A policy reviewed twice shows two reviews and a review count of 2 on its IAAF.
  - A second IAAF for the same policy is refused.
  - An IAAF linked to the previous term's record shows the link.
```

```fr
id: FR-SP-041
title: Route the IAAF for approval and signature
brd: [BRIDSP-07 (p.9)]
actor: System; approvers (IAAF_APPROVE)
priority: Must have
fit: CHANGE
screens: My Approvals; IAAF page (approvals); Submitted Policies Setup (Approval Matrix)
api: Workflow SBM_IAAF; POST /api/v1/submitted/iaaf/{id}/approve | return
description:
  - The IAAF Approval Matrix gives, for the document IAAF, the segment and the sum-insured band, the approval levels and the permission or named signatory of each level. When the IAAF is submitted, BIBS routes it to the first level; each approval moves it to the next level until the last one.
  - At the last approval BIBS stamps the signatures (name, position, time and hash of each approver), renders the PDF and sets the IAAF APPROVED. **Send to bank counterpart** e-mails it and sets ISSUED. The record then either waits for expiry as a conversion opportunity or receives a reminder letter (p.7).
preconditions:
  - The IAAF is FOR_APPROVAL; the user holds the permission of the current level and did not prepare the IAAF.
main_flow:
  - The approver opens the IAAF from My Approvals and reviews it.
  - The approver clicks **Approve**; BIBS moves it to the next level or to APPROVED.
  - The reviewer sends the approved IAAF to the bank counterpart.
alternate_flows:
  - Return. The approver returns the IAAF with a reason; it goes back to the reviewer.
rules:
  - [R1, "Levels, criteria and signatories of the matrix are to be supplied by BDOI (SP SQ07).", Configurable, Approval matrix (document IAAF)]
  - [R2, "The preparer never approves a level.", Fixed, "-"]
  - [R3, "The signature is a stamped signature until a qualified e-signature is connected.", Fixed, "-"]
validations:
  - [Approver is the preparer, An IAAF is approved by someone other than its preparer, To be assigned at build]
  - [Return without reason, "Select a reason for 'Return'", WORKFLOW_REASON_REQUIRED]
  - [No matrix row for the IAAF, "No approval level is defined for segment <segment> and sum insured <amount>", To be assigned at build]
notifications:
  - "SBM_IAAF_PENDING to the approvers of each level; alert SBM_IAAF_SLA past the review SLA."
audit:
  - "Each level with approver, time, decision and signature hash."
acceptance:
  - An IAAF of a Non-CBG Corporate policy goes to level 1 and, after approval, to level 2.
  - The preparer cannot approve it.
  - The issued IAAF PDF shows the stamped name, position and time of every approver.
```

## Limits and TOR

```fr
id: FR-SP-050
title: Detect accounts exceeding acceptance or coverage limits
brd: [BRIDSP-16 (p.10)]
actor: System
priority: Must have
fit: CHANGE
screens: Policy record (Rule Results, flag Insurer approval); Submitted Policies Setup (Limit Rules)
api: Limits step
description:
  - The limits step compares each record with the limit rules - per insurer and line or product, the maximum sum insured, the maximum vehicle age and other attribute limits - and with the package limit of the catalogue. A breach flags the record "Insurer approval required", records the breached limit and creates a TOR task for the handling Marketing user.
preconditions:
  - "Active limit rules exist."
main_flow:
  - The run evaluates the limits and stores each check.
  - On a breach BIBS sets the flag and creates the TOR task.
rules:
  - [R1, "Limit rules per insurer and product until Product Maintenance takes them over (SP SQ08).", Configurable, Limit rules (Setup)]
validations: []
notifications:
  - "The handling Marketing user receives the TOR task."
audit:
  - "Limit check rows (rule, attribute, limit, value, breached)."
acceptance:
  - A motor policy whose vehicle is older than the insurer's maximum age is flagged Insurer approval required with the limit and value.
```

```fr
id: FR-SP-051
title: Generate the Terms of Reference
brd: [BRIDSP-17 (p.10)]
actor: Marketing user (TOR_PREPARE)
priority: Must have
fit: NEW
screens: TOR (list, TOR page); Policy record (TOR tab)
api: POST /api/v1/submitted/tors
description:
  - For an account that exceeds the limits, the Marketing user generates a TOR (TOR-yyyy-nnnnnn) from template SBM_TOR. It lists the breached limits and the proposed terms. The TOR is created for the account (masterlist record or renewal ARN).
preconditions:
  - The record carries the flag Insurer approval required; the user has TOR_PREPARE.
main_flow:
  - The user opens the TOR task and clicks **Generate TOR**.
  - The user completes the proposed terms and saves the draft.
  - The user submits it (FR-SP-052).
rules:
  - [R1, "Template SBM_TOR; draft until BDOI provides the layout (SP SQ08).", Configurable, Document templates]
validations:
  - [Proposed terms blank, Enter the proposed terms, "-"]
  - [No breach on the account, "Policy <SBM no.> exceeds no limit", To be assigned at build]
fields_screen: TOR
fields:
  - [Breaches, Display, "-", Limit checks, "-"]
  - [Proposed terms, Long text, "Yes", "-", "-"]
  - [Account Officer, Look-up, "Yes", Sales officers, Active officer]
notifications:
  - "None until submission."
audit:
  - "TOR creation and edits."
acceptance:
  - A TOR generated for a flagged record lists its breached limits.
```

```fr
id: FR-SP-052
title: Route the TOR for approval per the TSU Approval Matrix
brd: [BRIDSP-18 (p.10)]
actor: System; TSU (TOR_APPROVE)
priority: Must have
fit: CHANGE
screens: My Approvals; TOR page; Submitted Policies Setup (Approval Matrix)
api: Workflow SBM_TOR
description:
  - The submitted TOR is routed by the TSU Approval Matrix (document TOR, segment and sum-insured band) to the approvers of each level for signature. The last approval attaches the signed PDF and sets APPROVED.
preconditions:
  - The TOR is FOR_APPROVAL.
main_flow:
  - The TSU approver opens the TOR in My Approvals and approves it.
  - BIBS moves it to the next level or to APPROVED with the signed PDF.
alternate_flows:
  - Return with a reason to the preparer.
rules:
  - [R1, "TSU matrix levels and signatories to be supplied by BDOI (SP SQ08).", Configurable, Approval matrix (document TOR)]
  - [R2, "The preparer never approves a level.", Fixed, "-"]
validations:
  - [Approver is the preparer, A TOR is approved by someone other than its preparer, To be assigned at build]
  - [Return without reason, "Select a reason for 'Return'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "SBM_TOR_PENDING to the approvers; alert SBM_TOR_SLA."
audit:
  - "Each level with approver, time, decision and signature hash."
acceptance:
  - A TOR reaches the TSU approver of its level and, after the last approval, carries the signed PDF.
```

```fr
id: FR-SP-053
title: Hand the approved TOR over to the Account Officer
brd: [BRIDSP-19 (p.10)]
actor: Account Officer
priority: Must have
fit: CHANGE
screens: Notifications; TOR page (Download)
api: GET /api/v1/submitted/tors/{id}.pdf
description:
  - When the TOR is approved, BIBS stores the signed PDF as an attachment of type TOR and notifies the assigned AO, who views or downloads it to proceed with the renewal and the insurer. The TOR becomes RELEASED when the AO opens or downloads it.
preconditions:
  - The TOR is APPROVED.
main_flow:
  - The AO opens the notification and the TOR.
  - The AO downloads the PDF; BIBS sets RELEASED.
rules:
  - [R1, "Meaning of Released to be confirmed (SP SQ08); default is the AO's first opening.", Fixed, "-"]
validations: []
notifications:
  - "SBM_TOR_RELEASED to the assigned AO."
audit:
  - "Opening and download with user and time."
acceptance:
  - The assigned AO is notified of the approved TOR and can download it; the TOR then shows Released.
```

## Renewal hand-off

```fr
id: FR-SP-060
title: Detect expiring policies and hand them to Renewal
brd: [BRIDSP-23 (p.11)]
actor: System (job SBM_EXPIRY_SCAN); Marketing AO (Renew with BDOI)
priority: Must have
fit: NEW
screens: Renewal Work List (For Renewal, Hand-off Pending); Masterlist (Renew with BDOI)
api: Job SBM_EXPIRY_SCAN; port RenewalHandOff
description:
  - Every day the expiry scan selects For Renewal records whose expiry falls within the lead days of their segment (for example CBG Fire 150 days, CBG Motor 120 days), assigns the insurer with the insurer rules (by vehicle type for CBG Motor; different from the expiring insurer for CBG Fire), and hands each record to the Renewal module through the port RenewalHandOff with the policy and risk data, the RA template and the assigned insurer. The record moves to RENEWAL_IN_PROGRESS.
  - From the hand-off on, the Renewal module owns the renewal - renewal account, 30-day hold cover request and letters (decision D2; FRS BRD-6 FR-RN-090). For Non-CBG Retail, the AO starts the renewal by hand with **Renew with BDOI**, which calls the same port.
preconditions:
  - The record is FOR_RENEWAL.
main_flow:
  - The job runs at 22:00 PHT and selects the records.
  - BIBS assigns the insurer and calls the port; it records the hand-off (renewal reference returned by Renewal).
alternate_flows:
  - Renewal module not yet deployed. The default adapter records the hand-off as PENDING and lists it on the Renewal Work List; it creates no account and sends no letter. The hand-offs are replayed when the Renewal adapter is deployed.
  - No insurer rule applies. The record stays For Renewal and appears in the fallout with reason no insurer.
rules:
  - [R1, "Lead days per segment - CBG Fire 150, CBG Motor 120 (defaults).", Configurable, Parameter SBM_RENEWAL_LEAD_DAYS]
  - [R2, "Insurer rules by segment, vehicle type or occupancy; exclude the expiring insurer where set.", Configurable, Insurer rules (SP SQ11)]
  - [R3, "One hand-off per record and term; the port is idempotent on the SBM number.", Fixed, "-"]
validations:
  - [Renew with BDOI on a record not For Renewal, "Policy <SBM no.> is not For Renewal", To be assigned at build]
notifications:
  - "SBM_EXPIRY_NEAR and SBM_RENEWAL_STARTED to the handler and AO."
audit:
  - "Hand-off with insurer assigned, renewal reference and time."
acceptance:
  - A For Renewal CBG Fire record 150 days before expiry is handed over with an insurer different from the expiring one.
  - A second scan does not hand the same record over again.
  - Before the Renewal adapter exists, the hand-off shows as pending and no letter is sent.
```

```fr
id: FR-SP-061
title: List the policies subject for renewal
brd: [BRIDSP-25 (p.11)]
actor: Marketing user
priority: Must have
fit: NEW
screens: Renewal Work List (tabs For Renewal, Hand-off Pending, Hold Cover Pending, Insurer Not Accepted, Converted, Not Renewed); Reports (SBM-RENEWABLE)
api: GET /api/v1/submitted/renewals; report SBM-RENEWABLE
description:
  - When the renewal cycle starts, the Marketing user sees the complete list of renewable policies, by expiry month, bucket, AO and insurer, for call-out and placement. The user assigns or re-assigns the AO in bulk, exports the list (SBM-RENEWABLE) and opens the Renewal record of a handed-over policy for its letters and progress.
preconditions:
  - The user has SBM_VIEW.
main_flow:
  - The user opens the Renewal Work List and filters by expiry month.
  - The user assigns AOs and exports the list.
rules:
  - [R1, "The list shows every record For Renewal or in progress within the user's scope.", Fixed, "-"]
validations: []
notifications:
  - "The assigned AO is notified."
audit:
  - "Assignments with user and time."
acceptance:
  - The For Renewal tab for March lists every renewable record expiring in March in the user's scope.
  - The export has the same rows as the screen.
```

```fr
id: FR-SP-062
title: Send letters and proposals when the criteria are met
brd: [BRIDSP-22 (p.11)]
actor: System (job SBM_LETTER_DISPATCH; Renewal letter engine)
priority: Must have
fit: CHANGE
screens: Letters & Print Batches; Policy record (Letters tab)
api: Job SBM_LETTER_DISPATCH; letter rules
description:
  - When policy evaluation meets the sending criteria of a letter rule (letter type, segment, bucket, days relative to expiry, channel), the letter is generated and sent automatically, by e-mail, through the bank counterpart, or printed in a batch for the mail house (COG).
  - Renewal letters of handed-over policies - RA (generic or FFY), NRNS, NAL, SFU and renewal reminders - are generated by the Renewal module's letter engine, one RA per client (decision D2); the RA is sent 90 days before expiry. Submitted Policies sends the letters that are not renewal letters, namely the policy-review reminder to the client c/o the bank counterpart, and the renewal notice and renewal proposal until their owner is confirmed (XQ03). Printed renewal letters use this module's mail-house port.
preconditions:
  - "Active letter rules exist."
main_flow:
  - The daily job (06:30 PHT) finds the records that meet a rule.
  - BIBS generates the letters from their templates and sends them by the rule's channel.
  - For the print channel BIBS builds a merged PDF batch with a control list for COG.
alternate_flows:
  - Delivery failure. The letter is FAILED; alert SBM_LETTER_FAILED; the user resends it.
rules:
  - [R1, "Letter rules - type, segment, bucket, days relative to expiry, channel, template.", Configurable, Letter rules (Setup)]
  - [R2, "RA, NRNS, NAL, SFU and renewal reminders are not letters of this module (decision D2).", Fixed, "-"]
  - [R3, "Templates SBM_REMINDER, SBM_RENEWAL_NOTICE, SBM_RENEWAL_PROPOSAL; drafts until BDOI provides the layouts (SP SQ09).", Configurable, Document templates]
validations:
  - [Client without e-mail for an e-mail rule, "Client <name> has no e-mail address", To be assigned at build]
notifications:
  - "The letter to the client or bank counterpart."
audit:
  - "Each letter with number SBL-yyyy-nnnnnn, channel, status, template version; print batches with count and hand-over date."
acceptance:
  - A reviewed policy with a renewal opportunity gets the reminder letter by the rule's channel and date.
  - A print batch contains one PDF with all letters of the day and a control list.
  - No RA is produced by this module; the RA of a handed-over policy appears on its Renewal record.
```

```fr
id: FR-SP-063
title: Re-assign the insurer while a hold cover request is open
brd: [BRIDSP-32 (p.12)]
actor: Marketing user (SBM_PROCESS)
priority: Must have
fit: CHANGE
screens: Renewal Work List (Insurer Not Accepted; Re-assign Insurer)
api: POST /api/v1/submitted/renewals/{id}/reassign-insurer (HoldCoverService.reassign)
description:
  - When the insurer has not accepted the hold cover within the acceptance days (3-5 days in the BRD), the watch job alerts the handler to follow up or re-assign. The user updates the assigned insurer even with a hold cover request open. BIBS closes the open request as Reassigned, updates the insurer of the renewal account, sends a new hold cover request to the new insurer and keeps both in the history.
preconditions:
  - The renewal account has an open hold cover request.
main_flow:
  - The user selects the record and clicks **Re-assign Insurer**.
  - The user selects the new insurer and the reason and saves.
  - BIBS re-assigns and sends the new request.
rules:
  - [R1, "Acceptance window 5 days (default).", Configurable, Parameter SBM_INSURER_ACCEPT_DAYS (SP SQ12)]
  - [R2, "The new insurer must differ from the current one.", Fixed, "-"]
validations:
  - [Same insurer selected, Select an insurer other than the current one, To be assigned at build]
  - [Reason not selected, Select the reason, "-"]
fields_screen: Re-assign Insurer
fields:
  - [New insurer, List, "Yes", Insurer master, Active insurer]
  - [Reason, List, "Yes", LOV SBM_DECLINE_REASON, "-"]
notifications:
  - "Hold cover request e-mail to the new insurer; alert SBM_INSURER_NOT_ACCEPTED past the acceptance days."
audit:
  - "Both requests in the hold cover history; re-assign count on the record."
acceptance:
  - After re-assignment the old request shows Reassigned and a new request is sent to the new insurer.
  - The renewal account shows the new insurer.
```

```fr
id: FR-SP-064
title: Process placement of the renewal account
brd: [BRIDSP-26 (p.11)]
actor: Placement user (BRD-1 placement roles)
priority: Must have
fit: CHANGE
screens: BRD-1 Placement screens; Renewal Work List (Converted)
api: BRD-1 placement (payment gate, slip generation and sending)
description:
  - When the client confirms the renewal with BDOI, the renewal account created by the Renewal module follows the BRD-1 path - payment gate, placement slip, protected send to the insurer - and the placement file is transmitted to the insurer. The masterlist conversion status follows the account (Process Placement, Placed).
preconditions:
  - The renewal account exists and the client has confirmed.
main_flow:
  - The payment gate passes; the placement user generates and sends the slip (or the Renewal module does it automatically).
  - AccountStatusChanged updates the masterlist record to PLACED.
rules:
  - [R1, "Placement files are the existing BRD-1 placement slips.", Fixed, "-"]
validations: []
notifications:
  - "SBM_PLACEMENT_READY and SBM_PLACEMENT_SENT to the handler."
audit:
  - "Placement events on the account; conversion status change on the record."
acceptance:
  - After the client confirms and pays, the slip is sent to the insurer and the masterlist record shows Placed.
```

```fr
id: FR-SP-065
title: Show the booked status in the masterlist
brd: [BRIDSP-27 (p.11)]
actor: Booking user (BRD-1 booking roles); System
priority: Must have
fit: CHANGE
screens: BRD-1 Booking; Masterlist; Policy record (Renewal tab)
api: InvoiceBooked event (business type RENEWAL)
description:
  - When the renewal account is booked, the booking carries business type RENEWAL and the origin Submitted Policy (shared change BT0, decision D1). The listener of this module sets the masterlist record BOOKED with the invoice number and booking date. A renewal that never books is closed Not Renewed with the reason read from the Renewal module.
preconditions:
  - Placement is successful.
main_flow:
  - The booking user books the account.
  - BIBS publishes InvoiceBooked; the record becomes BOOKED.
alternate_flows:
  - The renewal is declined, lost or expires. The record becomes NOT_RENEWED with the reason given by the port's status query.
rules:
  - [R1, "Booking stays in the booking module; this module only follows it.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Status change with invoice number and time."
acceptance:
  - A booked renewal shows BOOKED, the invoice number and the booking date in the masterlist.
  - The booked invoice has business type RENEWAL.
```

## Handling fee

```fr
id: FR-SP-070
title: Tag handling-fee payments in the Unapplied Payment List
brd: [BRIDSP-31 (p.12)]
actor: System (job SBM_HANDLING_FEE_TAGGER); UPP handler (SBM_HANDLING_FEE)
priority: Must have
fit: CHANGE
screens: Handling Fees (records, tagger results, unmatched items); Collections unapplied view
api: Job SBM_HANDLING_FEE_TAGGER; UnappliedDispositionRequests (RECOGNIZE_INCOME)
description:
  - Handling-fee records exist for the CBG Motor submitted accounts that BDOI bills, with PN, location reference, amount and billing date. Every 30 minutes the tagger reads the open unapplied payments and matches CLPC payments on the PN and OTC payments on the location reference against billed records.
  - A match tags the payment Handling Fee - BIBS sends a disposition request (action RECOGNIZE_INCOME, income type HANDLING_FEE) to Cashiering, which applies it and issues the OR; the record becomes TAGGED then APPLIED, and the payment is available for reporting, audit and application. Unmatched or ambiguous payments stay for the UPP handler, who can apply the Handling Fee disposition by hand.
preconditions:
  - Billed handling-fee records exist.
main_flow:
  - The tagger runs and matches the payments.
  - BIBS sends the disposition requests and updates the records.
  - Cashiering applies them and issues the ORs.
alternate_flows:
  - Several records match one payment. The payment is left untagged and listed as ambiguous.
rules:
  - [R1, "Match key - PN for CLPC, location reference for OTC.", Fixed, "-"]
  - [R2, "Amount, VAT, OR and GL account of the handling fee to be confirmed (SP SQ13).", Configurable, Cashiering disposition type and accounting rules]
validations:
  - [Manual tag of an already tagged payment, "Payment <ref> is already tagged", To be assigned at build]
fields_screen: Handling-fee record
fields:
  - [Policy, Look-up, "Yes", Masterlist, CBG Motor]
  - [PN no. / Location reference, Text, "Yes (one)", "-", "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Billing date, Date, "Yes", "-", "-"]
notifications:
  - "None; the Handling Fees screen shows the tagger results."
audit:
  - "Each match, request and application with time; report SBM-HANDLING-FEE."
acceptance:
  - A CLPC payment carrying the PN of a billed record is tagged Handling Fee and applied with an OR.
  - An OTC payment with an unknown location reference stays in the unapplied list for the UPP handler.
```

## Monitoring, reports and notifications

```fr
id: FR-SP-080
title: Generate Submitted Policies reports
brd:
  - BRIDSP-20 (p.10)
  - BRIDSP-30 (p.12)
actor: Marketing user (SBM_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Reports (category Submitted Policies); Submitted Policies home
api: Report service, codes of section 6.1
description:
  - The Marketing user generates the reports of section 6.1 to monitor sanitation, disposition, classification and the other results - including the fallout reports for exceptions. BRIDSP-30 repeats BRIDSP-20 word for word; both are met by this FR.
  - The Submitted Policies home shows tiles (received today, awaiting validation, fallout of the last run, for renewal this month, IAAF and TOR pending, insurer not accepted, hold cover unbooked, handling fees tagged today); each tile opens its filtered list.
preconditions:
  - The user has SBM_REPORT_VIEW.
main_flow:
  - The user selects a report and sets its parameters.
  - BIBS runs it within the user's scope.
rules:
  - [R1, "Layouts without fields in the Report List are built with the obvious columns and flagged 'layout to confirm' (SP SQ25).", Configurable, Report definitions]
validations:
  - [Date range invalid, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Each run with user, parameters and time."
acceptance:
  - The Sanitation Report of a month shows the counts per the grouping of Report List row 156.
  - The process fallout report of a run lists every fallout record with its reason code.
```

```fr
id: FR-SP-081
title: View, download and print reports in several formats
brd: [BRIDSP-21 (p.10)]
actor: System; report users (SBM_REPORT_EXPORT)
priority: Must have
fit: FIT
screens: Reports (Export, Print)
api: GET /api/v1/reports/{code}/export?format=PDF|XLSX|ODS|CSV|XML
description:
  - Every report is rendered in the chosen format - Excel, PDF, ODS, CSV or XML - with print preview, saved variants and an archive of the exported files. This is the report framework built for BRD-1.
preconditions:
  - The user has SBM_REPORT_EXPORT.
main_flow:
  - The user runs a report and clicks Export with a format, or Print.
rules:
  - [R1, "Exports are archived with user and parameters.", Fixed, "-"]
validations:
  - [Export without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Every export."
acceptance:
  - SBM-MASTERLIST exported as Excel and as PDF has the same rows.
```

```fr
id: FR-SP-082
title: Send notifications and alerts for key events
brd: [BRIDSP-24 (p.11)]
actor: System
priority: Must have
fit: CHANGE
screens: Notifications (bell); notification preferences; alerts
api: Notification events and alert codes of section 5.6
description:
  - BIBS evaluates the notification rules and thresholds on each lifecycle event and notifies the users concerned in the app and, per their preferences, by e-mail. The events are those of BRIDSP-24 (section 5.6) - new submission or manual validation, pending IAAF or TOR, bucket moves, exceptions and fallout, nearing expiry and renewal start, TOR hand-over, placement file ready or sent, and the alert for hold covers of unbooked accounts.
  - Client-facing items (RA, SFU, NAL, renewal notices, proposals, reminders, NRNS) are the letters of FR-SP-062 and of the Renewal module.
preconditions:
  - "None."
main_flow:
  - An event occurs.
  - BIBS notifies the owners and raises the alert where a threshold is passed.
rules:
  - [R1, "Hold cover of an unbooked account is alerted 5 days (default) before the hold cover ends.", Configurable, Parameter SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS]
  - [R2, "Users choose in-app and e-mail per event type.", Configurable, Notification preferences]
validations: []
notifications:
  - "This FR is the notification service of the module."
audit:
  - "Notifications and alerts are logged."
acceptance:
  - The approvers of level 1 are notified when an IAAF is submitted.
  - A confirmed hold cover whose account is not booked 5 days before it ends raises SBM_HOLD_COVER_UNBOOKED.
```

# Workflow and status model

## Masterlist record (SBM_POLICY)

Figure 2 shows the workflow of a masterlist record. System transitions are driven by the processing run and by the account and booking events; user transitions are dispose, exclude, reinstate, renew (Renew with BDOI) and close. The figure shows the main path; Table 10 lists every status and what sets it.

![Workflow SBM_POLICY of a masterlist record](figures/brd12_policy_states.dot)

<!-- table: widths=4,4.2,8.4 caption="Statuses of a masterlist record" size=8.5 -->
| Status | Set by | Meaning |
|---|---|---|
| RECEIVED | Intake | Row loaded from a source; not yet confirmed |
| VALIDATED | Extraction confirmation, manual entry | Data confirmed by a user |
| CLASSIFIED | Processing run | Classification and bucket set |
| FOR_RENEWAL | Run or manual disposition | Renewable; waits for the expiry scan |
| FOR_MANUAL_DISPOSITION | Run | A handler decides |
| IN_REVIEW | Run (documents to review) | Policy review and IAAF in progress |
| EXCLUDED | Run or handler | Non-renewal bucket or manual exclusion |
| RENEWAL_IN_PROGRESS | Expiry scan, Renew with BDOI | Handed to the Renewal module |
| PLACED | Account event | Placement slip sent |
| BOOKED | InvoiceBooked | Renewal booked; invoice no. and date on the record |
| NOT_RENEWED | Renewal status query | Declined, lost or expired unrenewed, with the reason |
| CLOSED | Handler or system | No further action |

## Processing run

Figure 3 shows the steps of a processing run. Each step writes one result per record (outcome, bucket, reason code, rule id and rule-set version).

![Processing run steps (BRIDSP-08-16)](figures/brd12_processing_run.dot){width=12}

## IAAF approval (SBM_IAAF)

![Workflow SBM_IAAF](figures/brd12_iaaf_workflow.dot){width=13}

## TOR approval (SBM_TOR)

![Workflow SBM_TOR](figures/brd12_tor_workflow.dot){width=13}

## Handling-fee tagging

![Handling-fee tagging (BRIDSP-31)](figures/brd12_handling_fee.dot){width=13}

<!-- table: widths=3.6,13 caption="Handling-fee record statuses" size=8.5 -->
| Status | Meaning |
|---|---|
| BILLED | Record created from the billing list or a processing rule; waits for payment |
| TAGGED | A payment matched and the disposition request was sent to Cashiering |
| APPLIED | Cashiering applied the payment and issued the OR |
| CANCELLED | Record cancelled by the handler |

## Notifications and alerts

<!-- table: widths=5.6,7,4 caption="Notification events and alerts (BRIDSP-24, p.7 and 11)" size=8.5 -->
| Event / alert | When | Recipients |
|---|---|---|
| SBM_NEW_SUBMISSION | New records from an intake run | Handlers of the segment |
| SBM_MANUAL_VALIDATION | A document waits for confirmation | Handlers of the segment |
| SBM_IAAF_PENDING, SBM_TOR_PENDING | An approval level is waiting | Approvers of the level |
| SBM_BUCKET_CHANGED | Move to renewal, manual or non-renewal buckets | Handlers |
| SBM_FALLOUT | A run ends with fallout | Sanitation Handlers |
| SBM_EXPIRY_NEAR, SBM_RENEWAL_STARTED | Policy within the lead days; hand-off made | Handler, AO |
| SBM_TOR_RELEASED | TOR approved and handed over | Assigned AO |
| SBM_PLACEMENT_READY, SBM_PLACEMENT_SENT | Placement file ready or transmitted | Handler |
| SBM_HOLD_COVER_UNBOOKED (alert) | Hold cover confirmed, account not booked n days before it ends | Handler, TL |
| SBM_INSURER_NOT_ACCEPTED (alert) | Insurer has not accepted within the acceptance days | Handler |
| SBM_INTAKE_FAILED, SBM_LETTER_FAILED, SBM_IAAF_SLA, SBM_TOR_SLA (alerts) | Intake or letter failure; approval past its SLA | Job owners, approvers |
| Letters to clients (RA, SFU, NAL, renewal notices, proposals, reminders, NRNS) | Letter rules; Renewal letter engine | Client or bank counterpart |

# Reports and documents

## Reports

<!-- table: widths=4,5.8,4.4,2.4 caption="Submitted Policies reports (category Submitted Policies)" size=8.5 -->
| Code | Report | Content | BRD / Report List |
|---|---|---|---|
| SBM-MASTERLIST | Submitted Masterlist Report / Extract | Fields of RL #151 per segment; migrated records included | BRIDSP-04, 28; RL #151 |
| SBM-DOC-FALLOUT | Document Processing Fallout | Intake and extraction failures | BRIDSP-10; RL #152 |
| SBM-PROCESS-FALLOUT | Fallout: sanitation, matching, disposition, classification | Per run or period, with reason code | BRIDSP-10, 20; RL #153 |
| SBM-NON-RENEWAL | Non-renewal Report | Exclusions with reason codes | BRIDSP-14; RL #154 |
| SBM-MIGRATION-ERRORS | Migration Error Log | Rows of the migration jobs that failed | BRIDSP-33; RL #155 |
| SBM-SANITATION | Sanitation Report (RMEL certification) | Counts per the RL #156 grouping | BRIDSP-20; RL #156 |
| SBM-DISPOSITION | Disposition Report | Buckets and dispositions | BRIDSP-20; RL #157 |
| SBM-CLASSIFICATION | Account Classification Report | Inforced / Submitted | BRIDSP-20; RL #158 |
| SBM-RENEWABLE | Renewable Accounts / Policies | By bucket and expiry month | BRIDSP-25; RL #159 |
| SBM-IAAF | IAAF Tracking / Review | Reviews, approvals, audit | BRIDSP-05-07; RL #160 |
| SBM-TOR | TOR Status and Approval | Pending, Approved, Released; AO | BRIDSP-17-19; RL #161 |
| SBM-HANDLING-FEE | Handling Fee Payment Classification | CLPC / OTC, status, OR | BRIDSP-31; RL #162 |
| SBM-CONVERSION | Submitted Policies Conversion | Renewed, Unrenewed, Process Placement by group; persistency; accounts for review | BRIDSP-29; RL #163 |
| SBM-NO-TOUCH | Submitted Policies No Touch | Billing list to insurers; re-upload builds the billing statement | BRIDSP-14; RL #164 |
| SBM-PR-CONVERSION | Policy Review Conversion | Reviewed policies converted | RL #133 |
| SBM-PR-MONITORING | Policy Review Monitoring | Ageing from bank request to endorsement (SP SQ24) | RL #134 |
| SBM-PERSISTENCY | Renewal Persistency (submitted accounts) | Renewal rate of submitted accounts | RL #135 |
| SBM-PENETRATION, SBM-HOLD-COVER-GAP | Penetration Report; Hold Cover with Gap | CBG Motor per channel; hold covers with gaps | RL #138 |
| SBM-LETTERS | Letters register | Letters of this module and print batches | BRIDSP-22, 24 |

All reports need SBM_REPORT_VIEW (export SBM_REPORT_EXPORT), apply the user's scope, export to PDF, XLSX, ODS, CSV and XML, and support saved variants and the archive. Rows without fields in the Report List are delivered with the obvious columns and marked "layout to confirm" (SP SQ25). The renewal letters of submitted policies are listed in the Renewal report RNW-RA-DISPATCH (source Submitted Policy).

## Sanitation report grouping

<!-- table: widths=4,12.6 caption="SBM-SANITATION grouping (RL #156, Motor)" size=8.5 -->
| Group | Lines |
|---|---|
| Expiring accounts | Inforce; Submitted; Pending from previous months |
| For renewal | Regular; RMU; Open Market; CTPL; Auto Promo (FFY) |
| Not for renewal | SM / BDO Employee; No Touch; Mortgaged; Fully Paid; Cancelled / Did Not Materialize; Branch Account; Total Loss; Endorsement; Duplicate; Incorrect Expiry / Encoding; No Record with LAMD |
| Other | Pending for disposition; RB Submitted |

## Documents

<!-- table: widths=4.2,3,9.4 caption="Submitted Policies documents" size=8.5 -->
| Template | Output | Content |
|---|---|---|
| SBM_IAAF | PDF, e-mail to the bank counterpart | Policy and borrower; reviews with dates, adequacy and findings; review count; linked policies; approval levels with stamped signatures |
| SBM_TOR | PDF, attachment TOR | Account; breached limits with limit and value; proposed terms; approvals with stamped signatures |
| SBM_REMINDER | PDF, e-mail or print | Policy-review follow-up to the client c/o the bank counterpart |
| SBM_RENEWAL_NOTICE, SBM_RENEWAL_PROPOSAL | PDF, e-mail or print | Renewal notice and proposal (owner to confirm, XQ03) |
| SBM_NO_TOUCH_BILLING | XLSX, PDF | No Touch billing list and statement |

All templates are drafts until BDOI supplies the layouts (SP SQ07-SQ09). The RA (generic and FFY), NRNS, NAL and SFU templates belong to the Renewal module (RNW_RA_FIRST, RNW_RA_FFY, RNW_NRNS_REMINDER, RNW_NAL, RNW_SFU).

# Interfaces and integration

Figure 7 shows the interfaces. Sources arrive as uploads; the renewal leaves through the hand-off port to the Renewal module; handling-fee payments are applied by Cashiering through the Operations ports.

![Interfaces of Submitted Policies (dashed = parked or upload)](figures/brd12_integration.dot)

<!-- table: widths=3.8,1.8,7.2,2.4,1.6 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| LFS, HLS, CIU, SPI, Loan Booking Report, IA masterlist | In | Excel uploads per source layout; feeds parked (SubmittedSourceFeed) | BRIDSP-01 | PARKED |
| LAMD | In | Loan snapshot upload by PN | BRIDSP-13 | PARKED |
| Policy documents | In | Extraction of text PDFs; OCR parked | BRIDSP-02 | CHANGE |
| Excel masterlists | In | One-time migration | BRIDSP-33 | NEW |
| Renewal (BRD-6) | Out / In | RenewalHandOff; status query of a hand-off | BRIDSP-22, 23, 25, 26 | NEW |
| Placement (BRD-1) | Out | Hold cover re-assignment | BRIDSP-32 | CHANGE |
| Account and booking (BRD-1) | In | AccountStatusChanged; InvoiceBooked with business type RENEWAL (BT0) | BRIDSP-26, 27 | CHANGE |
| Cashiering and Collections | Out | Disposition request RECOGNIZE_INCOME for handling fees | BRIDSP-31 | CHANGE |
| E-mail outbox | Out | IAAF, TOR, letters | BRIDSP-07, 22 | BUILT |
| Mail house (COG) | Out | Print batches (merged PDF and control list) | BRIDSP-22 | PARKED |
| E-signature | Out | Qualified e-signature of IAAF and TOR; stamped signature until then | BRIDSP-07, 18 | PARKED |
| ISYS / ARF | - | Not fed; the ARN is the reference (SP SQ20) | p.5 | OUT |

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.13-14)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | CBG Admin / Mktg 30 (15 concurrent) for log-in and sanitation; Non-CBG Corporate Policy Review officers 8 (6) for log-in and policy review | Within the BRD-1 sizing of 145 concurrent users | FIT |
| Volumes | Log-ins 22,000 a year (+5%); sanitation 63,000 a year (+25% a year); Non-CBG log-ins and policy reviews 2,000 a year each (+5%) | Processing runs set-based per step; about 154,000 sanitations in year 5 | FIT |
| Response time | 2 seconds for all listed transactions | Screens and single-record actions within 2 seconds; runs, exports and letters asynchronous with progress. Tighter than BRD-1 (10 s) and Operations (5 s) (SP SQ21) | CHANGE |
| Peak | End of month and year-end; 08:00-18:00 daily; no mobile vs desktop difference | Batch jobs at night (processing 21:30, expiry scan 22:00, letters 06:30 PHT) | FIT |
| Availability | 99.9%; business hours; at most 45 minutes of planned downtime a month; maintenance 00:00-04:00 | Platform deployment; one BIBS-wide NFR set is being agreed (XQ08) | FIT |
| Continuity | Critical services restored within 4 hours; full recovery within 24 hours | Platform backup and recovery | FIT |
| Retention | Transaction and submitted policy records 5 years online, 5 years archive; daily backup kept 5 years; no anonymisation | Retention rule SUBMITTED_POLICY; differs from BRD-1 (5 / 15 years) (SP SQ22) | CONFIGURE |

# Configuration items owned by the business and the System Administrator

The items below are changed in BIBS without a release. Changes are audited.

## Parameters

<!-- table: widths=6.2,3,7.4 caption="Submitted Policies parameters (category SUBMITTED)" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| SBM_RENEWAL_LEAD_DAYS | CBG_FIRE=150, CBG_MOTOR=120 | Days before expiry of the hand-off to Renewal, per segment |
| SBM_MASTERLIST_DAYS_FROM_EXTRACTION | 30 | Target days from extraction to masterlist (CBG Motor) |
| SBM_INSURER_ACCEPT_DAYS | 5 | Days for the insurer to accept the hold cover before the alert |
| SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS | 5 | Days before the hold cover ends to alert an unbooked account |
| SBM_REVIEW_SLA_DAYS | to confirm | SLA of IAAF and TOR approval levels |
| RA timing of submitted policies | 90 days before expiry | Hand-off parameter of the Renewal module (V1017), not of this module |
| Job schedules | 21:30, 22:00, 06:30, 07:00 PHT; tagger every 30 minutes | SBM_PROCESSING, SBM_EXPIRY_SCAN, SBM_LETTER_DISPATCH, SBM_HOLD_COVER_WATCH, SBM_HANDLING_FEE_TAGGER |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| SBM_SEGMENT | CBG Motor; CBG Fire; Non-CBG Corporate and Branches; Non-CBG Retail |
| SBM_BUCKET | For Renewal; For Manual Disposition; Non-Renewal; No Touch; FFY; BDO / SM Group Employee Accounts; RMU; For Review; Fully Paid Loan; OPM; BDOFC; Sold (attribute renewal_action) |
| SBM_NON_RENEWAL_REASON | Free First Year; SM Group Employee; No Touch; CARI; Bonds; RMU; and the RL #156 reasons |
| SBM_LOAN_STATUS | Active; Open Market; Fully Paid; Remedial (RMU) |
| SBM_CONVERSION_STATUS | Renewed; Unrenewed; Process Placement (each by Inforce, NB Submitted, RB Submitted, OPM, RMU); Issued SFU |
| SBM_REASON | Fallout and exclusion reason codes (for example SBM_NO_RULE) |
| SBM_LETTER_TYPE | Reminder; Renewal notice; Renewal proposal |
| SBM_DECLINE_REASON | Reasons of insurer re-assignment |
| SBM_IAAF_FINDING | Findings of a policy review (to be supplied by BDOI) |

## Masters and rules maintained by the business

<!-- table: widths=5.4,4.6,6.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (approved by) | FR |
|---|---|---|
| Source register and upload templates | System Administrator | FR-SP-001 |
| Rule sets (sanitation, matching, classification, disposition) | Rule administrator (SBM_RULE_APPROVE) | FR-SP-020 |
| Limit rules | Rule administrator (SBM_RULE_APPROVE) | FR-SP-050 |
| Insurer rules | Rule administrator (SBM_RULE_APPROVE) | FR-SP-060 |
| Letter rules | Rule administrator (SBM_RULE_APPROVE) | FR-SP-062 |
| IAAF and TOR approval matrices | Rule administrator (SBM_RULE_APPROVE) | FR-SP-041, 052 |
| Legacy status map | Team Lead | FR-SP-004 |
| User scope (segments, own accounts) | System Administrator | FR-SP-011 |
| Document templates | System Administrator | Section 6.3 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-SP-01 | A submitted policy is not an account; it becomes one only when the client renews with BDOI | Design principle 1 |
| A-SP-02 | Submitted = PN matched to an active LAMD loan and not BDOI-placed; Inforced = otherwise | SP SQ04 |
| A-SP-03 | One IAAF per policy, with links to related policies | SP SQ07 |
| A-SP-04 | The Renewal module owns the renewal of submitted policies from the hand-off on | Decision D2 |
| A-SP-05 | BRIDSP-20 and BRIDSP-30 are the same requirement and are met once | BRD p.10, 12 |
| A-SP-06 | The BIBS ARN replaces the ISYS reference and the ARF | SP SQ20 |
| A-SP-07 | Sources arrive as Excel files until BDOI names the feeds | SP SQ01 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-SP-01 | The shared account business-type change BT0 (V822) is merged | FR-SP-065 (D1) |
| D-SP-02 | The Renewal module implements RenewalHandOff (wave R3); until then hand-offs wait as pending | FR-SP-060, 062 (D2) |
| D-SP-03 | Cashiering and Collections expose the unapplied-payment ports with the action RECOGNIZE_INCOME | FR-SP-070 |
| D-SP-04 | Placement adds the hold cover re-assignment | FR-SP-063 |
| D-SP-05 | BDOI supplies the source layouts, rule content, matrices, templates and the LAMD definition | FR-SP-001, 020, 041, 052, 062 |

## Open questions

<!-- table: widths=1.6,9.9,2.8,2.4 caption="Open questions on BRD-12 (status from the cross-BRD decisions, R5)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| SP SQ01 | Definition, owner, layout, frequency and transport of each source | FR-SP-001 | OPEN |
| SP SQ02 | Prescribed submission formats per segment and business type | FR-SP-001 | OPEN |
| SP SQ03 | Document types, share of scanned documents, OCR, mandatory fields | FR-SP-002 | OPEN |
| SP SQ04 | Definitions of Inforced and Submitted | FR-SP-030 | OPEN |
| SP SQ05 | Complete bucket list and precedence (RMU, FFY, Fully Paid conflicts); CARI, Bonds, BDOFC, OPM, Sold | FR-SP-031, 033, 034 | OPEN |
| SP SQ06 | LAMD fields and frequency; Generic vs FFY RA rule (XQ01: one or two LAMD intakes) | FR-SP-032 | OPEN |
| SP SQ07 | IAAF approval matrix, signature method, template; one IAAF per policy | FR-SP-040, 041 | OPEN |
| SP SQ08 | TSU matrix; limits per insurer and product; TOR template; meaning of Released | FR-SP-050 to 053 | OPEN |
| SP SQ09 | Letter templates, triggers, lead days and channels; COG format (XQ03) | FR-SP-062 | OPEN |
| SP SQ10 | Owner of the renewal of submitted policies | FR-SP-060 | ANSWERED |
| SP SQ11 | Insurer assignment rules | FR-SP-060 | OPEN |
| SP SQ12 | Acceptance window 3 or 5 days; outcome when no insurer accepts; alert lead days | FR-SP-063, 082 | OPEN |
| SP SQ13 | Handling fee amount, VAT, OR, GL account and record creation | FR-SP-070 | OPEN |
| SP SQ14 | No Touch service-fee basis and billing statement | Section 6.1 | OPEN |
| SP SQ15 | Access matrix and data scope of the personas | Section 3 | OPEN |
| SP SQ16 | Migration files, volumes, statuses and cut-over | FR-SP-004 | OPEN |
| SP SQ17 | Meaning of opportunity tag and conversion; who overrides a rule tag | FR-SP-003 | OPEN |
| SP SQ18 | Penetration report inputs and channel rule | Section 6.1 | OPEN |
| SP SQ19 | Expansion of the acronyms of the BRD | Section 1.4 | OPEN |
| SP SQ20 | BIBS ARN replaces the ISYS reference and ARF | Section 7 | OPEN |
| SP SQ21 | 2-second response against 10 s (BRD-1) and 5 s (Operations) | Section 8 | OPEN |
| SP SQ22 | Retention 5 + 5 years against 5 + 15 years (BRD-1) | Section 8 | OPEN |
| SP SQ23 | Non-CBG Retail employee accounts: quotation and NRNS by e-mail | FR-SP-060 | OPEN |
| SP SQ24 | Start and end events of the policy-review ageing | Section 6.1 | OPEN |
| SP SQ25 | Final fields of the reports without layouts | FR-SP-080 | OPEN |

# Traceability

Every BRD-12 requirement ID is met by at least one FR.

<!-- table: widths=2.2,3.2,5.4,5.8 caption="BRD ID to FR, screen and API" size=8 -->
| BRD ID | FR | Screen | API / job |
|---|---|---|---|
| BRIDSP-01 (p.8) | FR-SP-001 | Upload & Intake | Bulk handlers per source |
| BRIDSP-02 (p.8) | FR-SP-002 | Extraction Review | /submitted/extractions |
| BRIDSP-03 (p.8) | FR-SP-003 | Masterlist; Policy record | /submitted/policies; .../renewal-tag |
| BRIDSP-04 (p.9) | FR-SP-010 | Masterlist; Policy record | /submitted/policies |
| BRIDSP-05 (p.9) | FR-SP-040 | Policy Reviews / IAAF | .../iaaf |
| BRIDSP-06 (p.9) | FR-SP-040 | Policy Reviews / IAAF | .../reviews |
| BRIDSP-07 (p.9) | FR-SP-041 | My Approvals; IAAF page | Workflow SBM_IAAF |
| BRIDSP-08 (p.9) | FR-SP-020 | Setup (Rule Sets) | /submitted/setup/rule-sets |
| BRIDSP-09 (p.9) | FR-SP-021 | Processing Runs | /submitted/runs; job SBM_PROCESSING |
| BRIDSP-10 (p.9) | FR-SP-022 | Processing Runs (Fallout) | Reports SBM-DOC-FALLOUT, SBM-PROCESS-FALLOUT |
| BRIDSP-11 (p.9) | FR-SP-030 | Policy record (Loan & Matching) | Classification step |
| BRIDSP-12 (p.9) | FR-SP-031 | Masterlist (buckets) | Disposition step |
| BRIDSP-13 (p.10) | FR-SP-032 | Upload & Intake (LAMD) | Bulk SBM_LAMD |
| BRIDSP-14 (p.10) | FR-SP-033 | Masterlist (Non-Renewal) | Report SBM-NON-RENEWAL |
| BRIDSP-15 (p.10) | FR-SP-034 | Masterlist; Renewal tab | Disposition step; RenewalHandOff |
| BRIDSP-16 (p.10) | FR-SP-050 | Policy record; Setup (Limit Rules) | Limits step |
| BRIDSP-17 (p.10) | FR-SP-051 | TOR | /submitted/tors |
| BRIDSP-18 (p.10) | FR-SP-052 | My Approvals; TOR page | Workflow SBM_TOR |
| BRIDSP-19 (p.10) | FR-SP-053 | Notifications; TOR page | /submitted/tors/{id}.pdf |
| BRIDSP-20 (p.10) | FR-SP-080 | Reports; home | Report service |
| BRIDSP-21 (p.10) | FR-SP-081 | Reports (Export, Print) | /reports/{code}/export |
| BRIDSP-22 (p.11) | FR-SP-062 | Letters & Print Batches | Job SBM_LETTER_DISPATCH |
| BRIDSP-23 (p.11) | FR-SP-060 | Renewal Work List | Job SBM_EXPIRY_SCAN; RenewalHandOff |
| BRIDSP-24 (p.11) | FR-SP-082 | Notifications | Events and alerts SBM_* |
| BRIDSP-25 (p.11) | FR-SP-061 | Renewal Work List | Report SBM-RENEWABLE |
| BRIDSP-26 (p.11) | FR-SP-064 | BRD-1 Placement | Payment gate; placement slip |
| BRIDSP-27 (p.11) | FR-SP-065 | BRD-1 Booking; Masterlist | InvoiceBooked |
| BRIDSP-28 (p.12) | FR-SP-011 | Masterlist (Export) | Report SBM-MASTERLIST |
| BRIDSP-29 (p.12) | FR-SP-012 | Masterlist; Policy record (History) | /submitted/policies/{id} |
| BRIDSP-30 (p.12) | FR-SP-080 | Reports; home | Report service |
| BRIDSP-31 (p.12) | FR-SP-070 | Handling Fees | Job SBM_HANDLING_FEE_TAGGER |
| BRIDSP-32 (p.12) | FR-SP-063 | Renewal Work List | .../reassign-insurer |
| BRIDSP-33 (p.12) | FR-SP-004 | Upload & Intake (Migration) | Bulk SBM_MIGRATION |
| Usage requirements (p.13-14) | Section 8 | - | - |

API paths start with `/api/v1`. They are the paths of the build design (R3) and are confirmed at build.

# Sign-off

By signing, BDOI confirms that this FRS describes the Submitted Policies functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank and Corbank", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Head, Corporate and Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
