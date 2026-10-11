---
# Source of the Functional Requirements Specification for BRD-12 Submitted Policies.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-12_Submitted_Policies/FRS_BRD12_SUBMITTED_POLICIES.md
title: Submitted Policies
subtitle: BRD-12 BDOI Submitted Policies, issue received on 08-Oct-2026 (with the Submitted Policies rows of the Report List of 13-May-2026)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-12
name: Submitted Policies
doc_id: BIBS-FRS-BRD-12
version: "1.1"
date: 08 October 2026
status: Issued for BDOI review
header_title: FRS BRD-12 Submitted Policies
output: FRS/BIBS_FRS_BRD-12_Submitted_Policies_v1.1.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-12 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions D1 and D2
  - version: "1.1"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: "Re-based on Business Requirements Document (BRD) Template - Submitted Policies (BRIDSP-01 to 36, signed 22-Apr to 12-May-2026) and the Report List of 13-May-2026; traceability renumbered; FR-SP-035, 066, 067 and 083 added; FR-SP-002 deferred to Release 2; report layouts from the Report List; user-story view and storyboard index added."
distribution:
  - {name: "Product Owner, Submitted Policies", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Unit Head, Combank and Corbank", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Head, Retail Marketing", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Head, Corporate and Retail Marketing", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "CBG Admin / Marketing (Submitted Handlers, Sanitation Handlers, Team Leads)", role: Business user, organisation: BDOI, purpose: "Review of intake, processing and renewal hand-off"}
  - {name: "Non-CBG Corporate Policy Review Officers", role: Business user, organisation: BDOI, purpose: "Review of policy review, IAAF and TOR"}
  - {name: "Admin Team (UPP handlers), NB Team Leads", role: Business user, organisation: BDOI, purpose: "Review of the handling-fee tagging"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Submitted Policies business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to deliver and test the Submitted Policies functions and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirements it meets and their BRD pages.

The FRs describe the proposed behaviour of the Submitted Policies screens. Where the proposed rule or screen differs from the BRD text, or needs a decision of BDOI, chapter 12 lists each such point for confirmation. Messages are given with their text; a code is quoted where it is confirmed, and "To be confirmed" marks the others.

## Scope

A **submitted policy** is a policy that a bank borrower bought elsewhere and submitted to the bank to insure the collateral of an auto, housing, leasing or corporate loan. The BRD defines them as insurance policy documents provided by clients with existing mortgage accounts in BDO, where the cover is obtained from providers outside BDOI (p.2). BDOI did not place it and earns nothing on it, but it has two interests in it (BRD p.2-5): the adequacy of the collateral cover, which a Policy Reviewer checks and certifies with an IAAF, and the renewal, which BDOI tries to win when the policy expires.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Intake | Upload of submitted policies (Excel, CSV, ODS, PDF), manual entry and tagging, migration of the Excel masterlists; document extraction with confirmation in Release 2 | BRIDSP-01, 04, 05, 36 |
| Loan files and matching | Loan files (LAMD, LMS, LAD) received and matched to the submitted policies automatically, unmatched records flagged, processing log | BRIDSP-02 |
| Masterlist and access | One Submitted Masterlist with history; role-based view and extract; handler, conversion status and remarks | BRIDSP-06, 32, 33 |
| Rules and processing | Configurable sanitation, matching, classification and disposition criteria; automatic processing runs; fallout reports | BRIDSP-10 to 12 |
| Classification | Inforced / Submitted; qualification, non-renewal and inforced buckets; RA template choice | BRIDSP-13 to 17 |
| Policy review | IAAF per policy with reviews, approval matrix and signature | BRIDSP-07 to 09 |
| Limits and TOR | Limit breaches, Terms of Reference, TSU approval, hand-over to the AO | BRIDSP-20 to 23 |
| Renewal hand-off | Expiry detection, list of renewable policies, hand-off to Renewal, letters and proposals, hold cover re-assignment, placement and booking status | BRIDSP-26, 27, 29 to 31, 35 |
| Proposals | Individual and batch proposals with nominated package rates; assignment to a preferred accredited insurer | BRIDSP-18, 19 |
| Handling fee | Tagging of handling-fee payments in the Unapplied Payment List | BRIDSP-34 |
| Monitoring | Reports in several formats, including the monthly report of expiring accounts and active loans without an active policy; notifications and alerts with definable recipients | BRIDSP-03, 24, 25, 28 |

**Out of scope for this phase:**

- The renewal process itself after the hand-off: renewal account, hold cover request, RA, NRNS, NAL and SFU letters. It belongs to BRD-6 Renewal, which takes the hand-off (cross-BRD decision D2, R5). This document specifies what Submitted Policies does before and around the hand-off.
- Extraction of policy details from uploaded documents (BRIDSP-04), which the BRD now marks Must Have (Release 2). FR-SP-002 keeps its specification for Release 2; in Release 1 an uploaded policy document is attached to the record and its details are keyed in (FR-SP-003).
- Direct feeds from the bank systems, OCR of scanned documents, a qualified electronic signature and the mail-house (COG) transport. The BRD asks for uploads (BRIDSP-01, 02); a file transfer is added when BDOI names one. Until then an upload, manual entry, a stamped signature or a print batch is used.
- The ISYS reference and the ARF of the current process. The BIBS ARN replaces them (SP SQ20).

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Business Requirements Document (BRD) Template - Submitted Policies (16 pages; BRIDSP-01 to 36 on p.8-12; approval sheet p.15-16), received on 08-Oct-2026 | Revision log v1.3, 20-Apr-2026; signed 22-Apr to 12-May-2026 (Product Owner 22-Apr, Unit Head Combank and Corbank 24-Apr, Program Manager 28-Apr, Head Retail Marketing 06-May, Head Corporate and Retail Marketing 12-May-2026) |
| R2 | BDOI - CoreModernization Report List 05132026 (Submitted Policies reports p.21-26) | 13-May-2026 |
| R4 | BDOI Submitted Policies (BRD-12) requirements baseline | current |
| R5 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | current |
| R7 | BDO UX guidelines (brand, screen patterns) | current |
| R8 | BRD-1 New Business requirements baseline (shared platform capabilities) | current |

Page references in this document ("p.9") are pages of the BRD PDF (R1); "RL p.nn" is a page of the Report List (R2), which no longer numbers its rows. The issue of 8-Oct-2026 renumbers the requirements: version 1.0 of this FRS cited the e-signed copy of 24-Apr-2026 (BRIDSP-01 to 33); this version cites BRIDSP-01 to 36. Old BRIDSP-02 to 15 are now 04 to 17, old 16 to 29 are now 20 to 33, old 31 to 33 are now 34 to 36; BRIDSP-02, 03, 18 and 19 are new; old BRIDSP-30, a repeat of old BRIDSP-20 (now 24), was removed. The question prefix SQ is shared by two BRDs; this document writes the Submitted Policies questions as SP SQnn (cross-BRD question XQ07).

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing); MAO in the BRD process pages
ARN: Account Reference Number of a BIBS account (BRD-1)
BRIDSP: Requirement ID prefix of this BRD (BRIDSP-01 to BRIDSP-36)
Bucket: Qualification group of a policy after classification, which decides its next action (BRIDSP-14, 16, 17)
CBG: Consumer Banking Group segment (CBG Motor, CBG Fire)
CIU: Source report of CBG Motor policy details (not expanded in the BRD, SP SQ19)
CLPC: Payment channel whose payments carry the PN number (BRIDSP-34)
COG: Unit that prints and mails letters today (mail house)
FFY: Free First Year promotion of auto loans
HLS: Home loan system; source of the daily insurance report (CBG Fire)
IAAF: Insurance Adequacy Assessment Form, issued to the bank counterpart after a policy review
Inforced: Policy whose PN does not match an active loan of the loan reports (LAMD, LMS, LAD) (BRIDSP-17; definition SP SQ04)
LAD: Loan report of the bank named with LAMD and LMS among the loan reports (not expanded in the BRD, SP SQ27)
LAMD: Bank loan unit whose loan report is matched by PN (not expanded in the BRD)
LMS: Loan management system report of the bank, a loan file matched by PN or loan application number (not expanded in the BRD, SP SQ27)
Nominated rate: Package rate of an insurer and vehicle classification, nominated for submitted-policy proposals (BRIDSP-18)
LFS: Loan system of the auto loans; source of the insurance report (CBG Motor)
LOV: List of values maintained by the business administrator
NAL: No Advice Letter
No Touch: Submitted CBG Motor accounts that BDOI sends to the insurer for validation and billing (Report List p.26)
NRNS: No Renew / No Submit letter or notification
OTC: Over-the-counter payment channel; payments carry a location reference number
PN: Promissory note number of the bank loan; the first key of loan matching
RA: Renewal Advice letter (generic or FFY template)
RMU: Remedial Management Unit
SBM: Prefix of masterlist numbers (SBM-yyyy-nnnnnn) and of the module's codes
SFU: Letter to mortgaged CBG accounts (not expanded in the BRD)
SPI: Consolidated list of submitted policies (not expanded in the BRD)
SP SQnn: Open question on BRD-12 raised by the project team (section 10.3)
Submitted: Policy document provided by a client with a mortgage account in BDO, with cover from a provider outside BDOI (p.2); in the rules, a policy whose PN matches an active loan of the loan reports and that BDOI did not place (BRIDSP-14)
TOR: Terms of Reference, prepared when an account exceeds acceptance or coverage limits (BRIDSP-21)
TSU: Technical Support Unit
UPP: Unapplied Payment List
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor** (the BRD persona and the BIBS role), the BRD **priority** and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the business (rule sets, matrices, parameters, lists of values; section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A code is quoted where it is confirmed; "To be confirmed" marks the codes of the Submitted Policies messages that are confirmed with BDOI's review. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**, **notifications**, **audit** and numbered **acceptance criteria**, the basis of the BRD-12 test plan.

The BRD writes its requirements as user stories with Given / When / Then acceptance criteria, and its personas as "As a System", "As a Marketing User", "As an Account Officer", "As a Placement User", "As a Booking User" and "As a User". The FRs name the BIBS roles that act for them (section 3).

> [!NOTE]
> Values marked "default" (lead days, acceptance days, thresholds, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a change to the system.


# Business context and process overview

## Business context

Today submitted policies are tracked per segment in Excel masterlists on a shared drive, with e-mail and SharePoint (p.4-5). CBG Motor Team Leads extract policy details from LFS and CIU and the Submitted Handler consolidates them within 30 days; CBG Fire policies come from Home Loan AOs and are reviewed for adequacy; Non-CBG Corporate and Branch policies are reviewed by a Policy Reviewer who issues an IAAF; Non-CBG Retail policies come from the LAMD report. At renewal a Sanitation Handler matches the list against LAMD, excludes accounts that must not be renewed, assigns an insurer and sends a proposal with a 30-day hold cover request, and an RA text file goes to COG for mailing.

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.4-7)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Policy details arrive as Excel reports and documents per segment | Each source is uploaded into BIBS (Excel, CSV, ODS, PDF); document extraction with user confirmation follows in Release 2 |
| 2 | Separate Excel masterlists per segment on Drive H:\ | One Submitted Masterlist with history and role-based access |
| 3 | Sanitation and LAMD matching by hand | Loan files (LAMD, LMS, LAD) are matched automatically by PN and loan application number; configurable rules sanitise, classify and bucket every policy; fallout is reported |
| 4 | Policy reviews and IAAFs by e-mail and paper signature | Reviews, IAAF and approval matrix in BIBS, with stamped signatures |
| 5 | Limit breaches handled by e-mail with TSU | TOR generated, approved by the TSU matrix and handed over to the AO |
| 6 | Renewal proposals, hold covers and RA text files prepared by hand | Proposals are created one by one or in batches with nominated package rates and a preferred insurer; the expiry scan hands renewable policies to the Renewal module, which creates the account, requests the hold cover and sends the letters |
| 7 | Handling-fee payments filtered from the UPP list on SharePoint | Handling-fee payments tagged automatically by PN or location reference |
| 8 | Status monitored in the masterlists | Reports, alerts and notifications from live data, including the monthly list of expiring accounts and of active loans without an active policy |

## Process overview

The table lists the steps and Figure 1 shows them by actor. The masterlist record follows the workflow SBM_POLICY (section 5.1).

<!-- table: widths=0.8,3.8,3.4,7,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Intake | Handlers; System | Source files uploaded and validated; one masterlist record per policy; migration of the Excel masterlists | BRIDSP-01, 36 |
| 2 | Manual entry (extraction in Release 2) | Handler, Policy Reviewer | Details keyed in and tagged; fields proposed from the policy document and saved after confirmation in Release 2 | BRIDSP-04, 05 |
| 3 | Masterlist | All | Record, history, handler, conversion status, remarks; role-based view and extract | BRIDSP-06, 32, 33 |
| 4 | Loan files and processing run | System | Loan files received; matching by PN and loan application number; sanitation, classification, disposition, limits | BRIDSP-02, 10, 11 |
| 5 | Classification and buckets | System | Inforced / Submitted; buckets; renewal tag; RA template | BRIDSP-13 to 17 |
| 6 | Fallout | Sanitation Handler | Exceptions and failures listed with reason codes and resolved | BRIDSP-12 |
| 7 | Policy review and IAAF | Policy Reviewer; approvers | Reviews recorded; IAAF generated, approved by the matrix and issued to the bank counterpart | BRIDSP-07 to 09 |
| 8 | Limits and TOR | Marketing user; TSU | Limit breach flagged; TOR prepared, approved and handed over to the AO | BRIDSP-20 to 23 |
| 9 | Proposals, expiry scan and hand-off | Marketing user; System | Proposals with nominated rates and a preferred insurer; renewable policies within the lead days are handed to Renewal with the assigned insurer | BRIDSP-18, 19, 27, 29 |
| 10 | Renewal | Renewal module (BRD-6) | Renewal account, 30-day hold cover, RA and letters; insurer re-assignment when the insurer does not accept | BRIDSP-26, 30, 35 |
| 11 | Placement and booking | Placement and Booking users (BRD-1) | Placement file to the insurer; booking; masterlist shows BOOKED | BRIDSP-30, 31 |
| 12 | Monitoring | All | Reports, including the monthly expiring and uninsured-loan report; alerts and notifications | BRIDSP-03, 24, 25, 28 |
| - | Handling fee | System; UPP handler | Handling-fee payments tagged in the Unapplied Payment List | BRIDSP-34 |

![Submitted Policies process by actor (BRIDSP-01 to 36)](figures/brd12_process_flow.dot)

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
| Sanitation Handler | SBM_SANITATION | Uploads sources and loan files; runs processing; resolves fallout and unmatched records; creates proposals; hand-off; sends letters | System, Marketing User |
| Team Lead | SBM_TL | All Submitted Policies functions except rule approval; assigns handlers | Marketing User |
| Policy Review Officer (Non-CBG Corporate) | SBM_POLICY_REVIEWER | Reviews policies; prepares IAAFs | Marketing User |
| Rule administrator | SBM_RULE_ADMIN | Maintains rule sets, limits, insurer and letter rules, matrices (maker) | Marketing User (BRIDSP-10) |
| Admin Team UPP handler | SBM_UPP_HANDLER | Handling-fee records and tagging results | System (BRIDSP-34) |
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
| SBM_INTAKE | Source uploads, intake runs, loan file uploads (LAMD, LMS, LAD) |
| SBM_PROCESS | Processing runs, fallout resolution, manual disposition, renewal hand-off, insurer re-assignment |
| SBM_RULE_MAINTAIN, SBM_RULE_APPROVE | Rule sets, limit, insurer and letter rules, approval matrices (maker and checker) |
| IAAF_PREPARE, IAAF_APPROVE | Reviews and IAAF; IAAF approval levels |
| TOR_PREPARE, TOR_APPROVE | TOR; TSU approval levels |
| SBM_LETTER_SEND | Generate, send and print the letters of this module; print batches |
| SBM_PROPOSAL | Create individual and batch proposals, edit nominated rates for a batch, assign the insurer; release a proposal (Team Lead) |
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
| SBM_PROPOSAL | | | Y | Y (release) | | | | Y | | |
| SBM_HANDLING_FEE | | | | Y | | | Y | | | |
| SBM_MIGRATE | | | | Y | | | | | | |
| SBM_EXPORT | Y | | Y | Y | | | | | | |
| SBM_REPORT_VIEW | Y | Y | Y | Y | Y | | | | | |
| SBM_REPORT_EXPORT | Y | | Y | Y | | | | | | |

"L1" and "L2" are IAAF approval levels of the seed matrix. Placement and Booking users also receive SBM_VIEW.

**Data scope.** Every query, report and export is limited to the user's scope: a list of segments, and for Marketing AOs only the records they handle or whose renewal account they own (BRIDSP-32).

**Segregation of duties.** A rule set is approved by someone other than its maker; an IAAF or TOR level is never approved by the preparer.

# Functional requirements

## Intake of submissions

```fr
id: FR-SP-001
title: Upload submitted policies
brd: [BRIDSP-01 (p.8)]
actor: User (Submitted Handler, Sanitation Handler; SBM_INTAKE)
priority: Must have
screens: Upload & Intake (source uploads, intake runs)
description:
  - BIBS keeps a register of the approved sources (LFS insurance report, HLS daily insurance report, CIU report, SPI list, LAMD report, Loan Booking Report, IA masterlist, IBG / Leasing documents, manual entry, migration) with segment, format and upload template. Each source file is uploaded with its own template, validated row by row and recorded as an intake run.
  - Each valid row creates or updates one masterlist record on the natural key (segment, business type, PN or policy number, expiry date). New records get the number SBM-yyyy-nnnnnn and status RECEIVED. A file already uploaded (same content) is refused.
  - The user uploads submitted policies as Excel (XLSX, XLS), CSV or ODS files with the template of the source, or as PDF policy documents. A PDF is attached to a new or existing record and its details are keyed in (FR-SP-003); extraction from the PDF follows in Release 2 (FR-SP-002). The BRD asks for uploads; transfers from the bank systems are not required by BRIDSP-01 (SP SQ01).
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
  - [R3, "A file is identified by its content fingerprint; the same file cannot be loaded twice.", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, BULK_FILE_TYPE]
  - [File without header row, The file has no header row, BULK_FILE_EMPTY]
  - [Same file loaded before, "This file was already loaded in run <run no.>", To be confirmed]
  - [Mandatory column blank, "Row <n>: <column> is required", To be confirmed]
fields_screen: Source upload
fields:
  - [Source, List, "Yes", Source register, Active source]
  - [File, Attachment, "Yes", Template of the source, "Excel, CSV, ODS or PDF; platform file rules"]
notifications:
  - "SBM_NEW_SUBMISSION to the handlers of the segment; SBM_INTAKE_FAILED alert on failure."
audit:
  - "Intake run with source, file name, hash, counts (received, created, updated, duplicate, failed), user and time."
acceptance:
  - An LFS insurance report of 300 rows creates 300 CBG Motor records with status RECEIVED.
  - The same report saved as ODS is accepted; a PDF policy document is attached to its record for manual entry.
  - Loading the same file a second time is refused.
  - A row updating an existing policy changes the record and writes the history.
```

```fr
id: FR-SP-002
title: Extract policy details from documents and save them after confirmation (Release 2)
brd: [BRIDSP-04 (p.9)]
actor: System; Submitted Handler, Policy Reviewer (SBM_MAINTAIN)
priority: Must have (Release 2)
screens: Extraction Review (proposal side by side with the record)
description:
  - The user uploads a policy document (policy copy, scanned or PDF) to a new or existing record. BIBS extracts the fields - assured, PN, policy number, insurer, period, sum insured, premium, vehicle or property details - and shows each proposed value with its confidence next to an editable field.
  - Nothing is saved to the masterlist before the user confirms. On **Confirm** the fields are written and the record becomes VALIDATED; on **Reject** the file is kept with a reason and the user enters the data by hand.
  - Release. The BRD issue of 8-Oct-2026 marks BRIDSP-04 "Must Have (Release 2)". This FR is delivered in Release 2; in Release 1 the document is attached and keyed in (FR-SP-001, FR-SP-003). Its test cases run in Release 2 (CLR-SP-03).
preconditions:
  - The user has SBM_MAINTAIN.
main_flow:
  - The user uploads the document on the record, or as a new record.
  - BIBS extracts the fields and opens Extraction Review.
  - The user checks and corrects the values and clicks **Confirm**.
alternate_flows:
  - Scanned document without text. Extraction returns "not readable" (OCR waits for SP SQ03); the user enters the fields by hand (FR-SP-003).
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
brd: [BRIDSP-05 (p.9)]
actor: Marketing user (Submitted Handler, Policy Reviewer, Marketing AO; SBM_MAINTAIN)
priority: Must have
screens: Masterlist (New Policy); Policy record (Edit, Tag Renewable / Non-Renewable)
description:
  - A Marketing user creates a policy record by hand when data is incomplete or cannot be extracted, or updates an existing one. Mandatory fields depend on the segment.
  - For a record of a defined segment (default Non-CBG Corporate, list SBM_RENEWAL_TAG_SEGMENTS) the user tags it Renewable or Non-Renewable with a reason. The tag is saved with user and time, shown as a flag chip on the record and available in filters and reports. A manual tag overrides the rule result; the processing run records the rule result but keeps the manual tag.
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
  - [R4, "The manual renewal tag is offered only for the defined segments (default Non-CBG Corporate; SP SQ31).", Configurable, Parameter SBM_RENEWAL_TAG_SEGMENTS]
validations:
  - [Mandatory field blank, "<Field> is required", "-"]
  - [Non-Renewable without reason, Select the reason for Non-Renewable, "-"]
  - [Tag on a record outside the defined segments, "The renewal tag is not used for segment <segment>", To be confirmed]
  - [Same policy already in the masterlist, "Policy <no.> is already in the masterlist (<SBM no.>)", To be confirmed]
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
  - A CBG Motor record does not offer the manual renewal tag while only Non-CBG Corporate is defined.
```

```fr
id: FR-SP-004
title: Migrate the existing Excel masterlists
brd: [BRIDSP-36 (p.12)]
actor: System; Team Lead (SBM_MIGRATE)
priority: Must have
screens: Upload & Intake (Migration); Reports (SBM-MIGRATION-ERRORS)
description:
  - The existing masterlists in Excel (NB Motor, RB Motor, Fire, Non-CBG) are read with a column mapping per legacy layout. Valid records are migrated completely, keep their original submission status (mapped to a BIBS status and bucket), submission date and reference ID, and are flagged Migrated. Invalid, incomplete or failed records are captured in an error log with reason codes.
  - The migration can be run again; a record with the same legacy reference is not created twice.
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
  - [Status not in the map, "Row <n>: status <value> has no mapping", To be confirmed]
  - [Mandatory column blank, "Row <n>: <column> is required", To be confirmed]
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
brd: [BRIDSP-06 (p.9)]
actor: System; all Submitted Policies users
priority: Must have
screens: Masterlist (tabs All, For Validation, Classified, For Renewal, Manual Disposition, Non-Renewal, Fallout, Unmatched); Policy record
description:
  - All submitted policy details are stored and can be viewed, updated and extracted per market segment (BRIDSP-06). They are held in one masterlist, one record per submitted or inforced policy, segment and business type - with loan data, risk details, insurer and policy, classification, bucket, renewal tag, handler, conversion status and the linked renewal ARN and booked invoice. When a record is saved or updated, the masterlist shows the latest data.
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
  - Each market segment can be extracted on its own with only its records.
```

```fr
id: FR-SP-011
title: View and extract the masterlist by role
brd: [BRIDSP-32 (p.12)]
actor: User (SBM_VIEW, SBM_EXPORT)
priority: Must have
screens: Masterlist (Export); Reports (SBM-MASTERLIST)
description:
  - Viewing and downloading the masterlist follow role-based access. Viewing needs SBM_VIEW and extracting needs SBM_EXPORT. Every record, list, report and extract is limited to the user's scope - the segments of the user, and for Marketing AOs only their own records.
  - The extract is the report SBM-MASTERLIST with the fields of the Submitted Masterlist Report / Extract of the Report List (RL p.24) per segment, including migrated records, in XLSX, CSV or PDF, archived.
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
brd: [BRIDSP-33 (p.12)]
actor: Marketing user (SBM_MAINTAIN)
priority: Must have
screens: Masterlist (Assign Handler; quick filter Changed since); Policy record (History)
description:
  - The user updates the handler, the conversion status and the remarks of a record, one by one or in bulk (Assign Handler). The masterlist shows the latest values; the History tab shows every change per field; a "Changed since" quick filter lists the records changed after a date.
preconditions:
  - The user has SBM_MAINTAIN.
main_flow:
  - The user edits the handler, conversion status or remarks and saves.
  - BIBS records the change and shows it in the list.
rules:
  - [R1, "Conversion statuses in LOV SBM_CONVERSION_STATUS (Renewed, Unrenewed, Process Placement per group; For Issuance of SFU Letter; For Issuance of NAL Letter; Accounts for Review; RL p.26).", Configurable, LOV SBM_CONVERSION_STATUS]
validations:
  - [Handler not an active user, "<user> is not an active user", To be confirmed]
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
brd: [BRIDSP-10 (p.9)]
actor: Marketing user as rule administrator (SBM_RULE_MAINTAIN); approver (SBM_RULE_APPROVE)
priority: Must have
screens: Submitted Policies Setup (Rule Sets)
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
  - [Approval by the maker, A rule set is approved by someone other than its maker, To be confirmed]
  - [Rule without outcome, Select the outcome of the rule, "-"]
  - [Condition on an unknown field, "Field <name> cannot be used in a rule", To be confirmed]
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
brd: [BRIDSP-11 (p.9)]
actor: System (job SBM_PROCESSING); Sanitation Handler (SBM_PROCESS)
priority: Must have
screens: Processing Runs (list, run detail); Masterlist (Run Processing)
description:
  - When documents or files are uploaded and confirmed, BIBS runs the processing steps in order - sanitation, matching against the loan files (FR-SP-035), classification, disposition and limits - and triggers the workflow of each record by its result (renewal hand-off, manual disposition queue, non-renewal, policy review, TOR). Account tagging is applied as the criteria define.
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
  - [R3, "Matching checks PN and loan application number against the loan files (LAMD, LMS, LAD) and PN against serial / motor number and assured name / address / contacts (RL p.24).", Configurable, Matching rule set]
  - [R4, "Job time 21:30 PHT.", Configurable, Job schedule]
validations: []
notifications:
  - "SBM_BUCKET_CHANGED to the handlers; SBM_FALLOUT alert when the run has fallout."
audit:
  - "Run with trigger, scope, counts per outcome and job run; result rows per record and step."
acceptance:
  - Uploading an LFS file starts a run; each new record ends with a classification and a bucket or fallout.
  - A record matched to an active loan of the loan files is moved to the right bucket and the handlers are notified.
```

```fr
id: FR-SP-022
title: Report fallout after processing
brd: [BRIDSP-12 (p.10)]
actor: System; Sanitation Handler
priority: Must have
screens: Processing Runs (Fallout tab); Masterlist (Fallout tab); Reports (SBM-PROCESS-FALLOUT)
description:
  - When document processing or a run ends with errors or exceptions, BIBS produces the fallout report SBM-PROCESS-FALLOUT - intake failures, the records that no rule could place or that failed a step (sanitation, matching, disposition, account classification), each with its reason code. The Report List of 13-May-2026 no longer has a separate document processing fallout report; extraction failures join this report when extraction comes in Release 2 (CLR-SP-17). The fallout of each run is archived and the handler resolves it on the record (correct and re-run, dispose by hand, or exclude).
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
brd: [BRIDSP-13 (p.10)]
actor: System
priority: Must have
screens: Policy record (Loan & Matching, Rule Results); Reports (SBM-CLASSIFICATION)
description:
  - When the policy details are evaluated, the classification step sets the record Submitted when its PN (or loan application number) matches an active loan of the loan reports - LAMD, LMS, LAD - and BDOI did not place the policy, and Inforced otherwise, so the right renewal and disposition rules apply. The definition is rule data and is to be confirmed (SP SQ04).
preconditions:
  - "The matching step has run."
main_flow:
  - BIBS evaluates the classification rules on the matching result and stores the classification.
rules:
  - [R1, "Default - PN matched to an active loan of the loan reports gives SUBMITTED; unmatched gives INFORCED.", Configurable, Classification rule set (SP SQ04)]
validations: []
notifications:
  - "None."
audit:
  - "Result row with rule and version."
acceptance:
  - A record whose PN matches an active LAMD or LMS loan is Submitted; a record whose PN is in none of the loan reports is Inforced.
```

```fr
id: FR-SP-031
title: Group Submitted policies into qualification buckets
brd: [BRIDSP-14 (p.10)]
actor: System
priority: Must have
screens: Masterlist (bucket tabs and filter); Policy record
description:
  - Submitted policies (PN matched against the loan reports - LAMD, LMS, LAD) are grouped into For Renewal, For Manual Disposition, Non-Renewal, No Touch, FFY, BDO / SM Group Employee Accounts or RMU. Entering a bucket triggers the next workflow - renewal hand-off at the lead days, the manual disposition queue, or exclusion with a fallout entry.
preconditions:
  - "The record is classified Submitted."
main_flow:
  - BIBS evaluates the disposition rules and stores the bucket, the renewal tag and the reason.
  - BIBS moves the record to FOR_RENEWAL, FOR_MANUAL_DISPOSITION or EXCLUDED.
alternate_flows:
  - Manual disposition. A Sanitation Handler disposes the record by hand with a reason (For Renewal or Exclude).
rules:
  - [R1, "Buckets are a list with the attribute renewal action (RENEW, MANUAL, EXCLUDE).", Configurable, LOV SBM_BUCKET]
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
title: Choose the RA template from the active loan of the loan reports
brd: [BRIDSP-15 (p.10)]
actor: System
priority: Must have
screens: Upload & Intake (Loan Files); Policy record (Loan & Matching)
description:
  - The loan files (LAMD, LMS, LAD; FR-SP-035) hold per PN the loan status, the amortised flag and the maturity date. For an active loan that is not amortised (for example a LAMD non-amortised account), the disposition rules mark the record ready for the Generic RA or the FFY RA template, and the next workflow is triggered. The chosen template travels with the renewal hand-off; the RA itself is generated by the Renewal module.
preconditions:
  - A loan file is loaded.
main_flow:
  - The Sanitation Handler uploads the loan file, or the job picks it up (FR-SP-035).
  - The run reads the loan and sets the RA template on the record.
rules:
  - [R1, "Rule for Generic vs FFY RA to be confirmed (SP SQ06).", Configurable, Disposition rule set]
validations:
  - [Snapshot row without PN, "Row <n>: PN is required", To be confirmed]
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
brd: [BRIDSP-16 (p.10)]
actor: System
priority: Must have
screens: Masterlist (Non-Renewal tab); Reports (SBM-NON-RENEWAL)
description:
  - Policies that meet the non-renewal criteria (Free First Year, SM Group Employee, No Touch, CARI, Bonds, RMU and the other exclusion reasons of the rule set) are tagged Non-Renewal, excluded from renewal and recorded in the non-renewal report with the exclusion reason. A manual tag can override the result (FR-SP-003).
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
brd: [BRIDSP-17 (p.10)]
actor: System
priority: Must have
screens: Masterlist; Policy record (Renewal tab)
description:
  - Inforced policies (PN not matched to the loan reports, for example LAMD) are grouped into Fully Paid Loan, OPM, BDOFC or Sold. For an eligible inforced policy, the RA and the renewal proposal are generated - by the Renewal module after the hand-off (decision D2), with the RA template of its bucket and the proposal template SBM_RENEWAL_PROPOSAL.
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

```fr
id: FR-SP-035
title: Receive loan files and match them to the submitted policies automatically
brd: [BRIDSP-02 (p.8-9)]
actor: System; Sanitation Handler (SBM_INTAKE)
priority: Must have
screens: Upload & Intake (Loan Files); Processing Runs (Loan File Results); Masterlist (Unmatched tab)
description:
  - The loan reports of the bank - LAMD, LMS and LAD (list SBM_LOAN_REPORT) - are loaded as loan files. A file is uploaded on Upload & Intake or, once BDOI names a transfer, placed in the agreed drop folder and picked up by the scheduled job; in both cases the processing then runs without manual intervention.
  - BIBS matches every loan record against the submitted policies on the defined keys, in the order of the matching rule set - PN number, then loan application number, then the other keys of the rule set. A matched account is categorised by the classification and disposition rules (FR-SP-030 to FR-SP-034). A record that cannot be matched is flagged Unmatched with its reason and listed for review or exception handling.
  - For every processed record BIBS logs the file source, the file name, the time stamp, the matching result (key used, matched policy or reason) and the categorisation, so that the processing is traceable and consistent from end to end.
preconditions:
  - The loan report is active in SBM_LOAN_REPORT with its layout.
main_flow:
  - The Sanitation Handler uploads the loan file and chooses the loan report, or the scheduled job picks up the file.
  - BIBS validates the rows and stores the loan snapshot of the report.
  - BIBS runs the matching on the defined keys and categorises the matched accounts.
  - BIBS lists the unmatched records on the Unmatched tab and notifies the Sanitation Handlers.
alternate_flows:
  - Unmatched record. The handler matches it by hand to a masterlist record with a reason, or disposes it (FR-SP-031).
  - Same file loaded before. BIBS refuses it and names the earlier run.
rules:
  - [R1, "Loan reports and their layouts - LAMD, LMS, LAD; precedence when two reports hold the same loan is LAMD, then LMS, then LAD (default, SP SQ27).", Configurable, LOV SBM_LOAN_REPORT]
  - [R2, "Matching keys and their order - PN number, loan application number, then the other keys of the matching rule set.", Configurable, Matching rule set]
  - [R3, "Every processed record writes one log row - file source, file name, time stamp, key used, matching outcome, category.", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, BULK_FILE_TYPE]
  - [Same file loaded before, "This file was already loaded in run <run no.>", To be confirmed]
  - [Row without PN and without loan application number, "Row <n>: PN number or loan application number is required", To be confirmed]
fields_screen: Loan file upload
fields:
  - [Loan report, List, "Yes", LOV SBM_LOAN_REPORT, Active report]
  - [As-of date, Date, "Yes", "-", Not in the future]
  - [File, Attachment, "Yes", Template of the loan report, "Excel, CSV or ODS"]
notifications:
  - "SBM_FALLOUT to the Sanitation Handlers when a loan file leaves unmatched records; SBM_INTAKE_FAILED when a file cannot be read."
audit:
  - "Loan file run with source, file name, counts (rows, matched, unmatched, failed), user or job and time; one log row per processed record."
acceptance:
  - An LMS file whose rows carry only the loan application number is matched to the submitted policies with that number and the matched accounts are categorised.
  - A loan record without a matching policy is flagged Unmatched with its reason and listed on the Unmatched tab.
  - The processing log of a record shows the file source, time stamp, key used, matching outcome and category.
  - A loan file is processed to the end without any user action after the upload.
```

## Policy review and IAAF

```fr
id: FR-SP-040
title: Record policy reviews and generate one IAAF per policy
brd:
  - BRIDSP-07 (p.9)
  - BRIDSP-08 (p.9)
actor: Marketing user (Policy Reviewer, Submitted Handler; IAAF_PREPARE)
priority: Must have
screens: Policy Reviews / IAAF (review queue, IAAF page); Policy record (Review & IAAF tab)
description:
  - Non-CBG Corporate and Branch records and CBG Fire records with documents enter the review queue. The reviewer records each review - review number, date, adequacy (Adequate or With findings), findings and remarks. With findings, the reviewer e-mails the bank counterpart from the record and a new review follows. The IAAF shows the review history and the review count.
  - When the policy is adequate, the reviewer generates the IAAF. Its unique reference number follows the BRD's sample format Region Code-Year-Counter (for example NCR-2026-000123) - the region code of the referring branch of the record, the year and a counter per region and year (SP SQ30). Only one IAAF exists per policy; it can be linked to related policies (for example the previous term or the same borrower) by their reference numbers.
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
  - [R3, "IAAF reference Region Code-Year-Counter; region code from the referring branch (SP SQ30).", Configurable, Parameter SBM_IAAF_NUMBER_FORMAT and the branch-to-region list]
validations:
  - [Second IAAF for the policy, "Policy <SBM no.> already has IAAF <no.>", To be confirmed]
  - [Adequacy not selected, Select the adequacy, "-"]
  - [IAAF with findings open, The last review has findings; record an adequate review first, To be confirmed]
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
  - The IAAF of a record referred by a branch of region NCR is numbered NCR-<year>-<counter>.
```

```fr
id: FR-SP-041
title: Route the IAAF for approval and signature
brd: [BRIDSP-09 (p.9)]
actor: System; approvers (IAAF_APPROVE)
priority: Must have
screens: My Approvals; IAAF page (approvals); Submitted Policies Setup (Approval Matrix)
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
  - [Approver is the preparer, An IAAF is approved by someone other than its preparer, To be confirmed]
  - [Return without reason, "Select a reason for 'Return'", WORKFLOW_REASON_REQUIRED]
  - [No matrix row for the IAAF, "No approval level is defined for segment <segment> and sum insured <amount>", To be confirmed]
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
brd: [BRIDSP-20 (p.10)]
actor: System
priority: Must have
screens: Policy record (Rule Results, flag Insurer approval); Submitted Policies Setup (Limit Rules)
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
brd: [BRIDSP-21 (p.11)]
actor: Marketing user (TOR_PREPARE)
priority: Must have
screens: TOR (list, TOR page); Policy record (TOR tab)
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
  - [No breach on the account, "Policy <SBM no.> exceeds no limit", To be confirmed]
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
brd: [BRIDSP-22 (p.11)]
actor: System; TSU (TOR_APPROVE)
priority: Must have
screens: My Approvals; TOR page; Submitted Policies Setup (Approval Matrix)
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
  - [Approver is the preparer, A TOR is approved by someone other than its preparer, To be confirmed]
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
brd: [BRIDSP-23 (p.11)]
actor: Account Officer
priority: Must have
screens: Notifications; TOR page (Download)
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
brd: [BRIDSP-27 (p.11)]
actor: System (job SBM_EXPIRY_SCAN); Marketing AO (Renew with BDOI)
priority: Must have
screens: Renewal Work List (For Renewal, Hand-off Pending); Masterlist (Renew with BDOI)
description:
  - Every day the expiry scan selects For Renewal records whose expiry falls within the lead days of their segment (for example CBG Fire 150 days, CBG Motor 120 days), assigns the insurer with the insurer rules (by vehicle type for CBG Motor; different from the expiring insurer for CBG Fire), and hands each record to the Renewal module with the policy and risk data, the RA template and the assigned insurer. The record moves to RENEWAL_IN_PROGRESS.
  - From the hand-off on, the Renewal module owns the renewal - renewal account, 30-day hold cover request and letters (decision D2; FRS BRD-6 FR-RN-090). For Non-CBG Retail, the AO starts the renewal by hand with **Renew with BDOI**, which makes the same hand-off.
preconditions:
  - The record is FOR_RENEWAL.
main_flow:
  - The job runs at 22:00 PHT and selects the records.
  - BIBS assigns the insurer and hands the record over; it records the hand-off (renewal reference returned by Renewal).
alternate_flows:
  - Renewal not yet in use. The hand-off is recorded as PENDING and listed on the Renewal Work List; no account is created and no letter is sent. Renewal takes the pending hand-offs once, when it starts.
  - No insurer rule applies. The record stays For Renewal and appears in the fallout with reason no insurer.
rules:
  - [R1, "Lead days per segment - CBG Fire 150, CBG Motor 120 (defaults).", Configurable, Parameter SBM_RENEWAL_LEAD_DAYS]
  - [R2, "Insurer rules by segment, vehicle type or occupancy; exclude the expiring insurer where set. A preferred insurer assigned to a released proposal (FR-SP-067) replaces the rule result.", Configurable, Insurer rules (SP SQ11)]
  - [R3, "One hand-off per record and term; a repeat for the same SBM number creates nothing new.", Fixed, "-"]
validations:
  - [Renew with BDOI on a record not For Renewal, "Policy <SBM no.> is not For Renewal", To be confirmed]
notifications:
  - "SBM_EXPIRY_NEAR and SBM_RENEWAL_STARTED to the handler and AO."
audit:
  - "Hand-off with insurer assigned, renewal reference and time."
acceptance:
  - A For Renewal CBG Fire record 150 days before expiry is handed over with an insurer different from the expiring one.
  - A second scan does not hand the same record over again.
  - While Renewal is not in use, the hand-off shows as pending and no letter is sent.
```

```fr
id: FR-SP-061
title: List the policies subject for renewal
brd: [BRIDSP-29 (p.11)]
actor: Marketing user
priority: Must have
screens: Renewal Work List (tabs For Renewal, Hand-off Pending, Hold Cover Pending, Insurer Not Accepted, Converted, Not Renewed); Reports (SBM-CONVERSION, Renewal Status Report)
description:
  - When the renewal cycle starts, the Marketing user sees the complete list of renewable policies, by expiry month, bucket, AO and insurer, for call-out and placement. The user assigns or re-assigns the AO in bulk, exports the list (the Renewal Status Report of SBM-CONVERSION, which replaces the Renewable Accounts report removed from the Report List; CLR-SP-17) and opens the Renewal record of a handed-over policy for its letters and progress.
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
brd: [BRIDSP-26 (p.11)]
actor: System (job SBM_LETTER_DISPATCH; Renewal letter engine)
priority: Must have
screens: Letters & Print Batches; Policy record (Letters tab)
description:
  - When policy evaluation meets the sending criteria of a letter rule (letter type, segment, bucket, days relative to expiry, channel), the letter is generated and sent automatically, by e-mail, through the bank counterpart, or printed in a batch for the mail house (COG).
  - Renewal letters of handed-over policies - RA (generic or FFY), NRNS, NAL, SFU and renewal reminders - are generated by the Renewal module's letter engine, one RA per client (decision D2); the RA is sent 90 days before expiry. Submitted Policies sends the letters that are not renewal letters, namely the policy-review reminder to the client c/o the bank counterpart, and the renewal notice and renewal proposal until their owner is confirmed (XQ03). Printed renewal letters use this module's mail-house port.
preconditions:
  - "Active letter rules exist."
main_flow:
  - The daily job (06:30 PHT) finds the records that meet a rule.
  - BIBS generates the letters from their templates and sends them by the rule's channel.
  - For the print channel BIBS creates a merged PDF batch with a control list for COG.
alternate_flows:
  - Delivery failure. The letter is FAILED; alert SBM_LETTER_FAILED; the user resends it.
rules:
  - [R1, "Letter rules - type, segment, bucket, days relative to expiry, channel, template.", Configurable, Letter rules (Setup)]
  - [R2, "RA, NRNS, NAL, SFU and renewal reminders are not letters of this module (decision D2).", Fixed, "-"]
  - [R3, "Templates SBM_REMINDER, SBM_RENEWAL_NOTICE, SBM_RENEWAL_PROPOSAL; drafts until BDOI provides the layouts (SP SQ09).", Configurable, Document templates]
validations:
  - [Client without e-mail for an e-mail rule, "Client <name> has no e-mail address", To be confirmed]
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
brd: [BRIDSP-35 (p.12)]
actor: Marketing user (SBM_PROCESS)
priority: Must have
screens: Renewal Work List (Insurer Not Accepted; Re-assign Insurer)
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
  - [Same insurer selected, Select an insurer other than the current one, To be confirmed]
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
brd: [BRIDSP-30 (p.12)]
actor: Placement user (BRD-1 placement roles)
priority: Must have
screens: BRD-1 Placement screens; Renewal Work List (Converted)
description:
  - When the client confirms the renewal with BDOI, the renewal account created by the Renewal module follows the BRD-1 path - payment gate, placement slip, protected send to the insurer - and the placement file is transmitted to the insurer. The masterlist conversion status follows the account (Process Placement, Placed).
preconditions:
  - The renewal account exists and the client has confirmed.
main_flow:
  - The payment gate passes; the placement user generates and sends the slip (or the Renewal module does it automatically).
  - The change of the account status updates the masterlist record to PLACED.
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
brd: [BRIDSP-31 (p.12)]
actor: Booking user (BRD-1 booking roles); System
priority: Must have
screens: BRD-1 Booking; Masterlist; Policy record (Renewal tab)
description:
  - When the renewal account is booked, the booking carries business type RENEWAL and the origin Submitted Policy (business type BT0, decision D1). The listener of this module sets the masterlist record BOOKED with the invoice number and booking date. A renewal that never books is closed Not Renewed with the reason read from the Renewal module.
preconditions:
  - Placement is successful.
main_flow:
  - The booking user books the account.
  - When the account is booked, the record becomes BOOKED.
alternate_flows:
  - The renewal is declined, lost or expires. The record becomes NOT_RENEWED with the reason given by Renewal's status query.
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

## Proposals with nominated rates

The issue of 8-Oct-2026 adds two requirements on renewal proposals that the Marketing user prepares from the masterlist: proposals created one by one or in a batch with nominated package rates (BRIDSP-18), and the assignment of a proposal or a batch to a preferred accredited insurer (BRIDSP-19). The proposal is prepared in Submitted Policies, before or at the renewal hand-off, and travels with the record to the Renewal module, which keeps the renewal account, hold cover and letters (decision D2; CLR-SP-15).

```fr
id: FR-SP-066
title: Create individual or batch proposals with nominated package rates
brd: [BRIDSP-18 (p.10)]
actor: Marketing user (Sanitation Handler, Team Lead, Marketing AO; SBM_PROPOSAL)
priority: Must have
screens: Renewal Work List (Create Proposals); Proposal Batch page; Submitted Policies Setup (Nominated Rates)
description:
  - The nominated package rates are a maintained table - segment, package, vehicle classification, insurer, rate per cover and effective dates - approved by a checker like the other rule tables. The Marketing user selects one record or many records of the Renewal Work List (For Renewal, Hand-off Pending) and clicks **Create Proposals**. BIBS takes the nominated rate of each record from its vehicle classification and assigned or chosen insurer and shows the proposed premium per record.
  - Before generating, the user may enter or edit the nominated rate and apply it to one record, to all selected records, or to the selected records of a vehicle classification and / or an insurer, with a reason. BIBS maps the edited rate to the matching records only and recomputes their premium. On **Generate**, BIBS creates one proposal per record from the template SBM_RENEWAL_PROPOSAL with the applied rates, groups them in a proposal batch (SBP-yyyy-nnnnnn) and sets them For Review.
  - A proposal For Review is checked by the Team Lead, who releases it or returns it with a reason. Released proposals are available to the Renewal hand-off (FR-SP-060) and to the letter rules (FR-SP-062).
preconditions:
  - The user has SBM_PROPOSAL; an approved nominated rate table is active.
main_flow:
  - The user selects the records and clicks **Create Proposals**.
  - BIBS proposes the nominated rates and premiums per record.
  - The user edits rates where needed, applies them by record, vehicle classification or insurer, and clicks **Generate**.
  - BIBS creates the proposals in a batch, For Review, and notifies the Team Lead.
  - The Team Lead releases the proposals.
alternate_flows:
  - No nominated rate for a record. The record is listed with the reason No nominated rate; the user enters the rate or removes the record from the batch.
  - Return. The Team Lead returns a proposal with a reason; the user edits the rate and generates it again as the next version.
rules:
  - [R1, "Nominated package rates by segment, package, vehicle classification and insurer, with effective dates, maker and checker.", Configurable, Nominated rate table (Setup)]
  - [R2, "An edited rate is applied only to the selected records of the chosen vehicle classification and / or insurer and is recorded with the reason, the old and the new rate.", Fixed, "-"]
  - [R3, "Proposal numbers SBP-yyyy-nnnnnn per batch, one proposal per record and version.", Fixed, "-"]
  - [R4, "A proposal is released by someone other than its maker.", Fixed, "-"]
validations:
  - [No record selected, Select at least one policy, "-"]
  - [Edited rate without reason, Enter the reason for changing the nominated rate, "-"]
  - [Rate outside the allowed range, "The rate must be between <min> and <max> percent", To be confirmed]
  - [Record without nominated rate on Generate, "Policy <SBM no.> has no nominated rate. Enter the rate or remove the policy", To be confirmed]
  - [Release by the maker, A proposal is released by someone other than its maker, To be confirmed]
fields_screen: Create Proposals
fields:
  - [Records, Table, "Yes", Selected masterlist records, At least one]
  - [Package, List, "Yes", Nominated rate table, "-"]
  - [Apply rate to, Option, "Yes", "This record, All selected, Vehicle classification, Insurer, Vehicle classification and insurer", "-"]
  - [Vehicle classification, List, Conditional, LOV SBM_VEHICLE_CLASS, For Vehicle classification]
  - [Insurer, List, Conditional, Accredited insurers, For Insurer]
  - [Nominated rate (%), Rate, "Yes", Nominated rate table, 2 to 4 decimals]
  - [Reason, Text, Conditional, "-", Required when the rate is edited]
notifications:
  - "SBM_PROPOSAL_REVIEW to the Team Lead when a batch is generated; the maker is notified of a release or return."
audit:
  - "Batch, proposals and versions with the applied rates, edits with old and new rate and reason, user and time."
acceptance:
  - A batch of 20 CBG Motor records creates 20 proposals with the nominated rate of each record's vehicle classification and insurer.
  - A rate edited for the vehicle classification Private Car of insurer A changes only the proposals of the Private Car records of insurer A.
  - A record without a nominated rate is listed with its reason and is not generated until a rate is entered.
  - Generated proposals are For Review and become available to the hand-off after the Team Lead releases them.
```

```fr
id: FR-SP-067
title: Assign proposals to a preferred accredited insurer
brd: [BRIDSP-19 (p.10)]
actor: Marketing user (SBM_PROPOSAL)
priority: Must have
screens: Create Proposals; Proposal Batch page (Assign Insurer)
description:
  - When a proposal is created, the user assigns it, or all selected proposals of a batch, to a preferred insurer chosen from the accredited insurers of the line of business. The insurer rules of FR-SP-060 give the default; the user's choice replaces it. BIBS assigns the selected insurer to each selected proposal, re-applies the nominated rate of that insurer (FR-SP-066), and keeps the default and the chosen insurer in the history. The hand-off passes the chosen insurer to the Renewal module.
preconditions:
  - The proposals are being created or are For Review; the user has SBM_PROPOSAL.
main_flow:
  - The user selects one proposal or many and clicks **Assign Insurer**.
  - The user chooses the accredited insurer and confirms.
  - BIBS assigns it to every selected proposal and recomputes their rates.
alternate_flows:
  - Insurer not accredited for the line. It is not offered.
  - Released proposal. Re-assignment needs a return by the Team Lead first.
rules:
  - [R1, "Only insurers accredited for the line of business and active on the date are offered.", Configurable, Insurer master (accreditation)]
  - [R2, "The insurer rules give the default; a manual choice is kept until changed by hand.", Fixed, "-"]
  - [R3, "For CBG Fire the expiring insurer is not offered (insurer rule) unless the Team Lead allows it.", Configurable, Insurer rules (SP SQ11)]
validations:
  - [No insurer chosen, Select the insurer, "-"]
  - [Proposal already released, "Proposal <no.> is released. Ask the Team Lead to return it first", To be confirmed]
fields_screen: Assign Insurer
fields:
  - [Proposals, Table, "Yes", Selected proposals, At least one]
  - [Insurer, List, "Yes", Accredited insurers of the line, Active and accredited]
notifications:
  - "None."
audit:
  - "Insurer assignment per proposal with default insurer, chosen insurer, user and time."
acceptance:
  - Assigning insurer B to 15 selected proposals of a batch sets insurer B on all 15 and recomputes their nominated rates.
  - A single proposal can be assigned to an insurer different from the batch.
  - An insurer that is not accredited for the line is not offered.
```


## Handling fee

```fr
id: FR-SP-070
title: Tag handling-fee payments in the Unapplied Payment List
brd: [BRIDSP-34 (p.12)]
actor: System (job SBM_HANDLING_FEE_TAGGER); UPP handler (SBM_HANDLING_FEE)
priority: Must have
screens: Handling Fees (records, tagger results, unmatched items); Collections unapplied view
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
  - [R3, "The Handling Fee Payment Classification report of the Report List lists the CLPC bills payments per account; OTC payments stay tagged by location reference as BRIDSP-34 asks (CLR-SP-17).", Configurable, Report definition SBM-HANDLING-FEE]
validations:
  - [Manual tag of an already tagged payment, "Payment <ref> is already tagged", To be confirmed]
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
brd: [BRIDSP-24 (p.11)]
actor: Marketing user (SBM_REPORT_VIEW)
priority: Must have
screens: Reports (category Submitted Policies); Submitted Policies home
description:
  - The Marketing user generates the reports of section 6.1 to monitor sanitation, disposition, classification and the other results - including the fallout reports for exceptions. The report columns are those of the Report List of 13-May-2026 (RL p.21-26; section 6.2). The earlier copy of the BRD repeated this requirement as BRIDSP-30; the issue of 8-Oct-2026 removed the repeat.
  - The Submitted Policies home shows tiles (received today, awaiting validation, fallout of the last run, for renewal this month, IAAF and TOR pending, insurer not accepted, hold cover unbooked, handling fees tagged today); each tile opens its filtered list.
preconditions:
  - The user has SBM_REPORT_VIEW.
main_flow:
  - The user selects a report and sets its parameters.
  - BIBS runs it within the user's scope.
rules:
  - [R1, "Report columns and file names as defined in the Report List of 13-May-2026 (section 6.2); a report it does not define keeps the proposed columns until BDOI confirms them.", Configurable, Report definitions]
validations:
  - [Date range invalid, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Each run with user, parameters and time."
acceptance:
  - The Sanitation Report of a month lists one row per invoice with the columns of RL p.25 (EBIX invoice number to expiry date).
  - The process fallout report of a run lists every fallout record with its reason code and the masterlist columns of RL p.24.
  - The Conversion report offers its four parts - Renewal Status, Monthly Status, Account Classification, Persistency and Conversion - for a covered period and as of a date.
```

```fr
id: FR-SP-081
title: View, download and print reports in several formats
brd: [BRIDSP-25 (p.11)]
actor: System; report users (SBM_REPORT_EXPORT)
priority: Must have
screens: Reports (Export, Print)
description:
  - Every report is rendered in the chosen format - Excel, PDF, ODS, CSV or XML - with print preview, saved variants and an archive of the exported files. This is the report framework of BRD-1.
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
brd: [BRIDSP-28 (p.11)]
actor: System; rule administrator (recipient set-up)
priority: Must have
screens: Notifications (bell); notification preferences; alerts; Submitted Policies Setup (Notification Recipients)
description:
  - BIBS evaluates the notification rules and thresholds on each lifecycle event and notifies the intended or assigned recipients in the app and, per their preferences, by e-mail. The recipients of each event type are defined on Notification Recipients - roles, named users, the assigned handler or AO of the record, and outside e-mail addresses for the bank counterpart - with the segment they apply to. The events are those of BRIDSP-28 (section 5.6) - new submission or manual validation, pending IAAF or TOR, bucket moves, exceptions and fallout, nearing expiry and renewal start, TOR hand-over, placement file ready or sent, and the alert for hold covers of unbooked accounts.
  - Client-facing items (RA, SFU, NAL, renewal notices, proposals, reminders, NRNS) are the letters of FR-SP-062 and of the Renewal module.
  - The notification of exception or fallout cases is marked "nice to have" in the BRD; it is delivered with the other events and can be switched off per event type.
preconditions:
  - "None."
main_flow:
  - An event occurs.
  - BIBS notifies the recipients defined for the event type and raises the alert where a threshold is passed.
rules:
  - [R1, "Hold cover of an unbooked account is alerted 5 days (default) before the hold cover ends.", Configurable, Parameter SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS]
  - [R2, "Users choose in-app and e-mail per event type.", Configurable, Notification preferences]
  - [R3, "Recipients per event type - roles, named users, assigned handler or AO, outside addresses - and segment; each event type can be switched off.", Configurable, Notification Recipients (Setup)]
validations: []
notifications:
  - "This FR is the notification service of the module."
audit:
  - "Notifications and alerts are logged."
acceptance:
  - The approvers of level 1 are notified when an IAAF is submitted.
  - A confirmed hold cover whose account is not booked 5 days before it ends raises SBM_HOLD_COVER_UNBOOKED.
  - A named user added as recipient of SBM_TOR_RELEASED for CBG Fire receives the next TOR hand-over notice of a CBG Fire record.
```


```fr
id: FR-SP-083
title: Report the expiring accounts of a month and the active loans without an active policy
brd: [BRIDSP-03 (p.9)]
actor: Marketing user (SBM_REPORT_VIEW)
priority: Must have
screens: Reports (SBM-EXPIRING-UNINSURED)
description:
  - For the reporting month the user chooses, the report SBM-EXPIRING-UNINSURED gives two parts. Part 1 lists every masterlist account whose policy expires in that month, with its segment, classification, bucket, renewal tag, insurer, policy, expiry date, handler and renewal status. Part 2 lists every active loan account of the latest loan files (FR-SP-035) that has no active insurance policy - no masterlist record and no BIBS policy in force on the last day of the month, or only expired ones - with the loan report, PN number, loan application number, borrower, branch, segment, loan status and the expiry date of the last policy known.
  - Both parts follow the user's scope, export to Excel, PDF and CSV (FR-SP-081) and can be scheduled monthly.
preconditions:
  - The user has SBM_REPORT_VIEW; loan files are loaded for the month.
main_flow:
  - The user opens SBM-EXPIRING-UNINSURED and selects the reporting month and the segments.
  - BIBS produces both parts within the user's scope.
  - The user exports the report or schedules it monthly.
rules:
  - [R1, "Active loan = loan status Active in the latest loan file of each loan report on or before the month end (SP SQ28).", Configurable, Report definition]
  - [R2, "Active policy = a masterlist record or BIBS policy whose period covers the last day of the month.", Fixed, "-"]
validations:
  - [Reporting month blank, Select the reporting month, "-"]
  - [No loan file for the month, "No loan file is loaded for <month>. Part 2 uses the file of <date>", To be confirmed]
notifications:
  - "A scheduled run sends the report to its subscribers."
audit:
  - "Each run with user, month, segments and time."
acceptance:
  - The report for March lists every account in scope whose policy expires in March.
  - An active LMS loan without any policy in force at the end of March appears in Part 2 with its PN number and loan application number.
  - A loan whose policy runs beyond March does not appear in Part 2.
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
| BOOKED | Booking of the renewal | Renewal booked; invoice no. and date on the record |
| NOT_RENEWED | Renewal status query | Declined, lost or expired unrenewed, with the reason |
| CLOSED | Handler or system | No further action |

## Processing run

Figure 3 shows the steps of a processing run. Each step writes one result per record (outcome, bucket, reason code, rule id and rule-set version).

![Processing run steps (BRIDSP-10 to 17, 20)](figures/brd12_processing_run.dot){width=12}

## IAAF approval (SBM_IAAF)

![Workflow SBM_IAAF](figures/brd12_iaaf_workflow.dot){width=13}

## TOR approval (SBM_TOR)

![Workflow SBM_TOR](figures/brd12_tor_workflow.dot){width=13}

## Handling-fee tagging

![Handling-fee tagging (BRIDSP-34)](figures/brd12_handling_fee.dot){width=13}

<!-- table: widths=3.6,13 caption="Handling-fee record statuses" size=8.5 -->
| Status | Meaning |
|---|---|
| BILLED | Record created from the billing list or a processing rule; waits for payment |
| TAGGED | A payment matched and the disposition request was sent to Cashiering |
| APPLIED | Cashiering applied the payment and issued the OR |
| CANCELLED | Record cancelled by the handler |

## Notifications and alerts

<!-- table: widths=5.6,7,4 caption="Notification events and alerts (BRIDSP-28, p.7 and 11)" size=8.5 -->
| Event / alert | When | Default recipients (Notification Recipients set-up) |
|---|---|---|
| SBM_NEW_SUBMISSION | New records from an intake run | Handlers of the segment |
| SBM_MANUAL_VALIDATION | A document waits for confirmation | Handlers of the segment |
| SBM_IAAF_PENDING, SBM_TOR_PENDING | An approval level is waiting | Approvers of the level |
| SBM_BUCKET_CHANGED | Move to renewal, manual or non-renewal buckets | Handlers |
| SBM_FALLOUT | A run or loan file ends with fallout or unmatched records (nice to have in the BRD) | Sanitation Handlers |
| SBM_EXPIRY_NEAR, SBM_RENEWAL_STARTED | Policy within the lead days; hand-off made | Handler, AO |
| SBM_TOR_RELEASED | TOR approved and handed over | Assigned AO |
| SBM_PROPOSAL_REVIEW | A proposal batch waits for review; a proposal is released or returned | Team Lead; maker |
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
| SBM-MASTERLIST | Submitted Masterlist Report / Extract | Masterlist columns (section 6.2) per segment; migrated records included | BRIDSP-06, 32; RL p.24 |
| SBM-PROCESS-FALLOUT | Fallout Reports: sanitation, matching, disposition, account classification | Masterlist columns with the fallout reason, per run or period | BRIDSP-12, 24; RL p.24 |
| SBM-NON-RENEWAL | Non-renewal Report | Masterlist columns, location of risk and the exclusion reason | BRIDSP-16; RL p.24 |
| SBM-MIGRATION-ERRORS | Migration Error Log | Masterlist columns, location of risk, TCT and the error reason of the failed rows | BRIDSP-36; RL p.24 |
| SBM-SANITATION | Sanitation Report | One row per invoice: EBIX invoice, QPS and BDOI system references, cover, branch, department, unit head, AO, client, CIF, insured, PN, mortgagee, insurer, policy, inception and expiry dates | BRIDSP-24; RL p.25 |
| SBM-DISPOSITION | Disposition Report | Masterlist columns, location of risk and the bucket and disposition | BRIDSP-24; RL p.25 |
| SBM-CLASSIFICATION | Account Classification Report | Masterlist columns, location of risk, TCT and the classification (Inforce, Submitted, FFY) | BRIDSP-24; RL p.25 |
| SBM-IAAF | IAAF Tracking / Review Report | Masterlist columns, location of risk, TCT, reviews, approvals and IAAF number | BRIDSP-07 to 09; RL p.25 |
| SBM-TOR | TOR Status & Approval Report | Reference no., AO / TL, assured, line, TSI, premium and commission rates, basic premium and commission, total premium, insurer, status | BRIDSP-21 to 23; RL p.26 |
| SBM-HANDLING-FEE | Handling Fee Payment Classification | Submitted-policy accounts with handling-fee payments from the CLPC payments: PN, assured, bills payment file, transaction and reference numbers, payment date, amount, payment status, client ID | BRIDSP-34; RL p.26 |
| SBM-CONVERSION | Submitted Policies Conversion Report | Four parts, each for a covered period or as of a date: Renewal Status Report, Monthly Status Report (counts), Account Classification (counts), Persistency and Conversion Report; statuses Renewed, Unrenewed, Process Placement (by Inforce, NB Submitted, RB Submitted, OPM, RMU), For Issuance of SFU Letter, For Issuance of NAL Letter, Accounts for Review | BRIDSP-29, 33; RL p.26 |
| SBM-NO-TOUCH | Submitted Policies No Touch Report | Detailed report (PN, loan status, assured, unit, serial and motor numbers, insurer, policy, dates, basic premium, service fee with VAT and withholding tax, remarks) and the motor billing list | BRIDSP-16; RL p.26 |
| SBM-EXPIRING-UNINSURED | Expiring Accounts and Active Loans without an Active Policy | Part 1 accounts expiring in the month; part 2 active loans without an active policy | BRIDSP-03 |
| SBM-PR-CONVERSION | Policy Review Conversion Report | Converted reviewed accounts: assured, borrower, CIF, policy, dates, insurer, property, invoices, review reference, source, unit, basic premium after conversion, commission, previous insurer | RL p.21 |
| SBM-PR-MONITORING | Policy Review Monitoring Report | Reviewed policies with ageing, adequacy status, renewal status and opportunity tag (SP SQ24) | RL p.21 |
| SBM-PERSISTENCY | Renewal Persistency Report | Region, AO, total renewable, renewed, non-renewable, cancelled, not for renewal, lost business, unrenewed, persistency rate; marked Non-CBG Retail in the Report List (CLR-SP-17) | RL p.21 |
| SBM-PENETRATION, SBM-HOLD-COVER-GAP | Penetration Report; Hold Cover with Gap | CBG Motor per channel; hold covers with gaps | RL p.21 |
| SBM-PROPOSALS | Proposal register | Proposal batches and proposals with nominated and edited rates, insurer and status | BRIDSP-18, 19 |
| SBM-LETTERS | Letters register | Letters of this module and print batches | BRIDSP-26, 28 |
| SBM-DOC-FALLOUT | Document Processing Fallout (Release 2) | Extraction failures; removed from the Report List of 13-May-2026, joins SBM-PROCESS-FALLOUT when extraction comes (CLR-SP-17) | BRIDSP-04, 12 |

All reports need SBM_REPORT_VIEW (export SBM_REPORT_EXPORT), apply the user's scope, export to PDF, XLSX, ODS, CSV and XML, and support saved variants and the archive. The file names follow the naming conventions of the Report List (for example "Submitted Masterlist Report / Extract", "Sanitation Report"). The Renewable Accounts / Policies report of the earlier Report List is replaced by the Renewal Status Report of SBM-CONVERSION (CLR-SP-17). The Report List also adds the Renewal report "Accounts from LAMD Report tagged as Submitted in system", which FRS BRD-6 specifies with the masterlist columns of this module. The renewal letters of submitted policies are listed in the Renewal report RNW-RA-DISPATCH (source Submitted Policy).

## Report layouts

The Report List of 13-May-2026 defines the columns of the Submitted Policies reports. Most of them share the masterlist columns below; the others are listed in the table of section 6.1.

<!-- table: widths=4,12.6 caption="Masterlist columns of the Submitted Policies reports (RL p.24-25)" size=8.5 -->
| Group | Columns |
|---|---|
| Receipt | Date received of submitted policy; Received via (e-mail, CLS, walk-in, other BDO counterpart) |
| Classification | Classification (Inforce, Submitted, FFY) |
| Loan | Application number; PN number; Other PN number; Value date; Maturity date |
| Branch | Referring branch; Branch code (BDO); Unit head; Customer segment (Consumer 1 - Fire, Consumer 1 - Auto) |
| Client | Assured's name; Mailing address; Home, business and mobile numbers; E-mail address |
| Policy | Risk (for example PAR, CARI, Motor); Submitted insurance company; Submitted policy number; Accredited insurer (Yes / No) |
| Added per report | Location of risk (Non-renewal, Migration, Disposition, Classification, IAAF); Transfer certificate of title (Migration, Classification, IAAF); the reason, bucket or classification of the report |

<!-- table: widths=4,12.6 caption="Sanitation Report columns (RL p.25)" size=8.5 -->
| Group | Columns |
|---|---|
| References | EBIX invoice number; QPS reference; BDOI system reference; Cover number; Cover version; Transaction type |
| Organisation | Invoicing branch; Department code and description; Unit head; Business type; Account type; Account officer |
| Client | Client name; Client no.; CIF; Bank branch name; Insured name; PN number; E-mail address |
| Cover | Mortgagee and its description; Co-insurer; Insurer name; Policy number; Inception date; Expiry date |

The earlier Sanitation Report gave counts in the RMEL certification grouping (expiring, for renewal, not for renewal, other). The Report List now asks for the invoice-level list above; the count summary is kept as an optional second sheet only if BDOI confirms it (CLR-SP-17).

## Documents

<!-- table: widths=4.2,3,9.4 caption="Submitted Policies documents" size=8.5 -->
| Template | Output | Content |
|---|---|---|
| SBM_IAAF | PDF, e-mail to the bank counterpart | Policy and borrower; reviews with dates, adequacy and findings; review count; linked policies; approval levels with stamped signatures |
| SBM_TOR | PDF, attachment TOR | Account; breached limits with limit and value; proposed terms; approvals with stamped signatures |
| SBM_REMINDER | PDF, e-mail or print | Policy-review follow-up to the client c/o the bank counterpart |
| SBM_RENEWAL_PROPOSAL (batch) | PDF per proposal, merged PDF per batch | Proposal with the nominated or edited package rates and the preferred insurer (FR-SP-066, 067) |
| SBM_RENEWAL_NOTICE, SBM_RENEWAL_PROPOSAL | PDF, e-mail or print | Renewal notice and proposal (owner to confirm, XQ03) |
| SBM_NO_TOUCH_BILLING | XLSX, PDF | No Touch billing list and statement |

All templates are drafts until BDOI supplies the layouts (SP SQ07-SQ09). The RA (generic and FFY), NRNS, NAL and SFU templates belong to the Renewal module (RNW_RA_FIRST, RNW_RA_FFY, RNW_NRNS_REMINDER, RNW_NAL, RNW_SFU).

# Interfaces and integration

Figure 7 shows the interfaces. Sources arrive as uploads; the renewal is handed over to the Renewal module; handling-fee payments are applied by Cashiering.

![Interfaces of Submitted Policies (dashed = on hold or upload)](figures/brd12_integration.dot)

<!-- table: widths=3.6,1.6,7,2.4,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| LFS, HLS, CIU, SPI, Loan Booking Report, IA masterlist | In | Uploads per source layout (Excel, CSV, ODS, PDF) as BRIDSP-01 asks; a file transfer only if BDOI names one (SP SQ01) | BRIDSP-01 | IN SCOPE |
| Loan files (LAMD, LMS, LAD) | In | Loan file upload or pick-up, automatic matching by PN and loan application number (layouts SP SQ27) | BRIDSP-02, 15 | IN SCOPE |
| Policy documents | In | PDF attached and keyed in; extraction of text PDFs in Release 2; OCR on hold (SP SQ03) | BRIDSP-01, 04 | RELEASE 2 |
| Excel masterlists | In | One-time migration | BRIDSP-36 | IN SCOPE |
| Renewal (BRD-6) | Out / In | Renewal hand-off; status query of a hand-off | BRIDSP-26, 27, 29, 30 | IN SCOPE |
| Placement (BRD-1) | Out | Hold cover re-assignment | BRIDSP-35 | IN SCOPE |
| Account and booking (BRD-1) | In | Account status changes; booking with business type RENEWAL (BT0) | BRIDSP-30, 31 | IN SCOPE |
| Cashiering and Collections | Out | Disposition request RECOGNIZE_INCOME for handling fees | BRIDSP-34 | IN SCOPE |
| Outgoing e-mail | Out | IAAF, TOR, letters | BRIDSP-09, 26 | IN SCOPE |
| Mail house (COG) | Out | Print batches (merged PDF and control list) | BRIDSP-26 | ON HOLD |
| E-signature | Out | Qualified e-signature of IAAF and TOR; stamped signature until then | BRIDSP-09, 22 | ON HOLD |
| ISYS / ARF | - | Not fed; the ARN is the reference (SP SQ20) | p.5 | OUT |

# Non-functional requirements

<!-- table: widths=3,5.6,5.4 caption="Non-functional requirements (BRD p.13-14)" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Users | CBG Admin / Mktg 30 (15 concurrent) for log-in and sanitation; Non-CBG Corporate Policy Review officers 8 (6) for log-in and policy review | Within the BRD-1 sizing of 145 concurrent users |
| Volumes | Log-ins 22,000 a year (+5%); sanitation 63,000 a year (+25% a year); Non-CBG log-ins and policy reviews 2,000 a year each (+5%) | Processing runs set-based per step; about 154,000 sanitations in year 5 |
| Response time | 2 seconds for all listed transactions | Screens and single-record actions within 2 seconds; runs, exports and letters asynchronous with progress. Tighter than BRD-1 (10 s) and Operations (5 s) (SP SQ21) |
| Peak | End of month and year-end; 08:00-18:00 daily; no mobile vs desktop difference | Batch jobs at night (processing 21:30, expiry scan 22:00, letters 06:30 PHT) |
| Availability | 99.9%; business hours; at most 45 minutes of planned downtime a month; maintenance 00:00-04:00 | Platform deployment; one BIBS-wide NFR set is being agreed (XQ08) |
| Continuity | Critical services restored within 4 hours; full recovery within 24 hours | Platform backup and recovery |
| Retention | Transaction and submitted policy records 5 years online, 5 years archive; daily backup kept 5 years; no anonymisation | Retention rule SUBMITTED_POLICY; differs from BRD-1 (5 / 15 years) (SP SQ22) |

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
| SBM_RENEWAL_TAG_SEGMENTS | NON_CBG_CORPORATE | Segments whose records take the manual Renewable / Non-Renewable tag |
| SBM_IAAF_NUMBER_FORMAT | {REGION}-{YYYY}-{COUNTER} | IAAF reference number; region from the referring branch |
| SBM_PROPOSAL_RATE_RANGE | 0.50 to 10.00 percent | Allowed range of an edited nominated rate |
| RA timing of submitted policies | 90 days before expiry | Hand-off parameter of the Renewal module, not of this module |
| Job schedules | 21:30, 22:00, 06:30, 07:00 PHT; tagger every 30 minutes; loan file pick-up 20:30 PHT | SBM_PROCESSING, SBM_EXPIRY_SCAN, SBM_LETTER_DISPATCH, SBM_HOLD_COVER_WATCH, SBM_HANDLING_FEE_TAGGER, SBM_LOAN_FILE_PICKUP |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values provided |
|---|---|
| SBM_SEGMENT | CBG Motor; CBG Fire; Non-CBG Corporate and Branches; Non-CBG Retail |
| SBM_BUCKET | For Renewal; For Manual Disposition; Non-Renewal; No Touch; FFY; BDO / SM Group Employee Accounts; RMU; For Review; Fully Paid Loan; OPM; BDOFC; Sold (attribute renewal action) |
| SBM_NON_RENEWAL_REASON | Free First Year; SM Group Employee; No Touch; CARI; Bonds; RMU; Mortgaged; Fully Paid; Cancelled / Did Not Materialize; Branch Account; Total Loss; Endorsement; Duplicate; Incorrect Expiry / Encoding; No Record in the loan reports |
| SBM_LOAN_REPORT | LAMD; LMS; LAD (layout, keys and precedence per report; SP SQ27) |
| SBM_VEHICLE_CLASS | Vehicle classifications of the nominated rates (to be supplied by BDOI, SP SQ29) |
| SBM_LOAN_STATUS | Active; Open Market; Fully Paid; Remedial (RMU) |
| SBM_CONVERSION_STATUS | Renewed; Unrenewed; Process Placement (each by Inforce, NB Submitted, RB Submitted, OPM, RMU); For Issuance of SFU Letter; For Issuance of NAL Letter; Accounts for Review |
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
| Nominated package rates | Rule administrator (SBM_RULE_APPROVE) | FR-SP-066 |
| Notification recipients per event type | Rule administrator (SBM_RULE_APPROVE) | FR-SP-082 |
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
| A-SP-02 | Submitted = PN matched to an active loan of the loan reports (LAMD, LMS, LAD) and not BDOI-placed; Inforced = otherwise; consistent with the definition of the BRD (p.2) | SP SQ04 |
| A-SP-03 | One IAAF per policy, with links to related policies | SP SQ07 |
| A-SP-04 | The Renewal module owns the renewal of submitted policies from the hand-off on | Decision D2 |
| A-SP-05 | The issue of 8-Oct-2026 is the signed baseline although its revision log ends at v1.3 of 20-Apr-2026 | SP SQ26 |
| A-SP-06 | The BIBS ARN replaces the ISYS reference and the ARF | SP SQ20 |
| A-SP-07 | Sources and loan files arrive as uploads (Excel, CSV, ODS, PDF), as BRIDSP-01 and 02 ask | SP SQ01 |
| A-SP-08 | Document extraction (BRIDSP-04) is delivered in Release 2 | CLR-SP-03 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-SP-01 | The business type (BT0) is available on the account | FR-SP-065 (D1) |
| D-SP-02 | The Renewal module takes the hand-offs; until then they wait as pending | FR-SP-060, 062 (D2) |
| D-SP-03 | Cashiering and Collections accept the unapplied-payment disposition RECOGNIZE_INCOME | FR-SP-070 |
| D-SP-04 | Placement adds the hold cover re-assignment | FR-SP-063 |
| D-SP-05 | BDOI supplies the source and loan file layouts, rule content, matrices, templates, the nominated rate table and the loan report definitions | FR-SP-001, 020, 035, 041, 052, 062, 066 |
| D-SP-06 | FRS BRD-6 Renewal takes the proposals released in Submitted Policies with the hand-off | FR-SP-060, 066 |

## Open questions

<!-- table: widths=1.6,9.9,2.8,2.4 caption="Open questions on BRD-12 (status from the cross-BRD decisions, R5)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| SP SQ01 | Definition, owner, layout, frequency and transport of each source | FR-SP-001 | PARTIAL (BRIDSP-01: upload; layouts open) |
| SP SQ02 | Prescribed submission formats per segment and business type | FR-SP-001 | OPEN |
| SP SQ03 | Document types, share of scanned documents, OCR, mandatory fields | FR-SP-002 | OPEN (Release 2) |
| SP SQ04 | Definitions of Inforced and Submitted | FR-SP-030 | PARTIAL (Submitted defined on p.2) |
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
| SP SQ25 | Final fields of the reports without layouts | FR-SP-080 | ANSWERED (Report List of 13-May-2026) |
| SP SQ26 | Is the issue received on 08-Oct-2026 the signed baseline, given that its revision log still ends at v1.3 of 20-Apr-2026? | Section 1.3 | OPEN |
| SP SQ27 | LMS and LAD: definition, owner, layout, keys, frequency and precedence over LAMD | FR-SP-030 to 035 | OPEN |
| SP SQ28 | Source of the active loans and definition of an active policy for the monthly report | FR-SP-083 | OPEN |
| SP SQ29 | Nominated package rates: owner, table (package, vehicle classification, insurer), allowed range of an edit; proposals in Submitted Policies or Renewal | FR-SP-066, 067 | OPEN |
| SP SQ30 | Source of the region code of the IAAF number | FR-SP-040 | OPEN |
| SP SQ31 | Segments that take the manual renewal tag (Non-CBG Corporate only, or more) | FR-SP-003 | OPEN |
| SP SQ32 | Reports changed in the Report List: document fallout, renewable accounts, persistency owner, handling fee without OTC, sanitation counts | Section 6.1 | OPEN |

# Traceability

Every requirement ID of the issue of 8-Oct-2026 (BRIDSP-01 to 36) is met by at least one FR. BRIDSP-04 is met by FR-SP-002 in Release 2. The removed BRIDSP-30 of the earlier copy, a repeat of the report requirement, has no row.

<!-- table: widths=2.2,3.2,5.4,5.2 caption="BRD ID to FR, screen and test cases" size=8 -->
| BRD ID | FR | Screen | Test cases |
|---|---|---|---|
| BRIDSP-01 (p.8) | FR-SP-001 | Upload & Intake | TC-SP-001.1, 001.2, 001.3, 001.4, 001.5, 001.6 (8 cases) |
| BRIDSP-02 (p.8-9) | FR-SP-035, FR-SP-030, FR-SP-031 | Upload & Intake (Loan Files); Masterlist (Unmatched) | TC-SP-035.1, 035.2, 035.3, 035.4, 030.1, 030.2, 031.1, 031.2, 031.3 (11 cases) |
| BRIDSP-03 (p.9) | FR-SP-083 | Reports (SBM-EXPIRING-UNINSURED) | TC-SP-083.1, 083.2, 083.3 (3 cases) |
| BRIDSP-04 (p.9) | FR-SP-002 | Extraction Review | TC-SP-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRIDSP-05 (p.9) | FR-SP-003 | Masterlist; Policy record | TC-SP-003.1, 003.2, 003.3, 003.4, 003.5 (8 cases) |
| BRIDSP-06 (p.9) | FR-SP-010 | Masterlist; Policy record | TC-SP-010.1, 010.2, 010.3, 010.4 (5 cases) |
| BRIDSP-07 (p.9) | FR-SP-040 | Policy Reviews / IAAF | TC-SP-040.1, 040.2, 040.3, 040.4, 040.5 (6 cases) |
| BRIDSP-08 (p.9) | FR-SP-040 | Policy Reviews / IAAF | TC-SP-040.1, 040.2, 040.3, 040.4, 040.5 (6 cases) |
| BRIDSP-09 (p.9) | FR-SP-041 | My Approvals; IAAF page | TC-SP-041.1, 041.2, 041.3, 041.4 (5 cases) |
| BRIDSP-10 (p.9) | FR-SP-020 | Setup (Rule Sets) | TC-SP-020.1, 020.2, 020.3, 020.4 (7 cases) |
| BRIDSP-11 (p.9) | FR-SP-021 | Processing Runs | TC-SP-021.1, 021.2, 021.3, 021.4 (4 cases) |
| BRIDSP-12 (p.10) | FR-SP-022 | Processing Runs (Fallout) | TC-SP-022.1, 022.2, 022.3 (3 cases) |
| BRIDSP-13 (p.10) | FR-SP-030 | Policy record (Loan & Matching) | TC-SP-030.1, 030.2 (2 cases) |
| BRIDSP-14 (p.10) | FR-SP-031 | Masterlist (buckets) | TC-SP-031.1, 031.2, 031.3 (4 cases) |
| BRIDSP-15 (p.10) | FR-SP-032 | Upload & Intake (Loan Files) | TC-SP-032.1, 032.2 (3 cases) |
| BRIDSP-16 (p.10) | FR-SP-033 | Masterlist (Non-Renewal) | TC-SP-033.1, 033.2, 033.3 (4 cases) |
| BRIDSP-17 (p.10) | FR-SP-034 | Masterlist; Renewal tab | TC-SP-034.1, 034.2 (2 cases) |
| BRIDSP-18 (p.10) | FR-SP-066 | Renewal Work List (Create Proposals) | TC-SP-066.1, 066.2, 066.3, 066.4 (7 cases) |
| BRIDSP-19 (p.10) | FR-SP-067 | Proposal Batch (Assign Insurer) | TC-SP-067.1, 067.2, 067.3 (4 cases) |
| BRIDSP-20 (p.10) | FR-SP-050 | Policy record; Setup (Limit Rules) | TC-SP-050.1, 050.2 (2 cases) |
| BRIDSP-21 (p.11) | FR-SP-051 | TOR | TC-SP-051.1, 051.2 (3 cases) |
| BRIDSP-22 (p.11) | FR-SP-052 | My Approvals; TOR page | TC-SP-052.1, 052.2 (3 cases) |
| BRIDSP-23 (p.11) | FR-SP-053 | Notifications; TOR page | TC-SP-053.1, 053.2 (2 cases) |
| BRIDSP-24 (p.11) | FR-SP-080 | Reports; home | TC-SP-080.1, 080.2, 080.3 (5 cases) |
| BRIDSP-25 (p.11) | FR-SP-081 | Reports (Export, Print) | TC-SP-081.1, 081.2 (2 cases) |
| BRIDSP-26 (p.11) | FR-SP-062 | Letters & Print Batches | TC-SP-062.1, 062.2, 062.3, 062.4 (5 cases) |
| BRIDSP-27 (p.11) | FR-SP-060 | Renewal Work List | TC-SP-060.1, 060.2, 060.3, 060.4 (7 cases) |
| BRIDSP-28 (p.11) | FR-SP-082 | Notifications | TC-SP-082.1, 082.2, 082.3, 082.4 (5 cases) |
| BRIDSP-29 (p.11) | FR-SP-061 | Renewal Work List; Reports (SBM-CONVERSION) | TC-SP-061.1, 061.2, 061.3 (3 cases) |
| BRIDSP-30 (p.12) | FR-SP-064 | BRD-1 Placement | TC-SP-064.1, 064.2 (2 cases) |
| BRIDSP-31 (p.12) | FR-SP-065 | BRD-1 Booking; Masterlist | TC-SP-065.1, 065.2 (2 cases) |
| BRIDSP-32 (p.12) | FR-SP-011 | Masterlist (Export) | TC-SP-011.1, 011.2, 011.3 (4 cases) |
| BRIDSP-33 (p.12) | FR-SP-012 | Masterlist; Policy record (History) | TC-SP-012.1, 012.2, 012.3 (4 cases) |
| BRIDSP-34 (p.12) | FR-SP-070 | Handling Fees | TC-SP-070.1, 070.2, 070.3 (4 cases) |
| BRIDSP-35 (p.12) | FR-SP-063 | Renewal Work List | TC-SP-063.1, 063.2, 063.3 (5 cases) |
| BRIDSP-36 (p.12) | FR-SP-004 | Upload & Intake (Migration) | TC-SP-004.1, 004.2, 004.3, 004.4 (5 cases) |
| Usage requirements (p.13-14) | Section 8 | - | - |


# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, fills a gap the BRD leaves open, or rests on a decision that BDOI confirms. The open questions of section 10.3 stay open; each item below gives the proposed rule that applies until BDOI answers. BDOI records its decision with the sign-off of this FRS (chapter 13); a decision that changes a screen, field, rule or message is applied in the next version of the FRS.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-SP-01 | Renewal of submitted policies (BRIDSP-26, 27, 29, 30; FR-SP-060, 062) | The expiry scan hands each For Renewal record to the Renewal module, which owns the renewal account, hold cover request and renewal letters; Submitted Policies follows the record to PLACED, BOOKED or NOT_RENEWED. | Cross-BRD decision D2 (SP SQ10). | Confirm decision D2. |
| CLR-SP-02 | Source files (BRIDSP-01; FR-SP-001) | Each source (LFS, HLS, CIU, SPI, Loan Booking Report, IA masterlist) is uploaded with its own template as Excel, CSV or ODS; a PDF policy document is attached to its record. No direct feed is provided unless BDOI names one. | BRIDSP-01 now asks for uploads (Excel, CSV, ODS, PDF); the layouts are still open (SP SQ01, SQ02). | Give the source layouts and confirm that no direct feed is needed (SP SQ01, SQ02). |
| CLR-SP-03 | Document extraction (BRIDSP-04; FR-SP-002) | Extraction from uploaded documents is delivered in Release 2, as the BRD now marks it. In Release 1 a policy document is attached and its details are keyed in (FR-SP-001, 003). In Release 2 text PDFs are read automatically and a scanned document without text is keyed in. | BRIDSP-04 is marked Must Have (Release 2); OCR is open (SP SQ03). | Confirm that Release 1 runs without extraction, and the OCR need for Release 2 (SP SQ03). |
| CLR-SP-04 | IAAF signature (FR-SP-041) | The IAAF carries a stamped signature until a qualified e-signature is connected. | The signature method is open (SP SQ07). | Confirm the signature method (SP SQ07). |
| CLR-SP-05 | Insurer assignment (FR-SP-060) | The insurer is assigned by rules by segment, vehicle type or occupancy; for CBG Fire the expiring insurer is excluded. | The insurer rules are open (SP SQ11). | Give the insurer rules (SP SQ11). |
| CLR-SP-06 | Hand-off lead days (FR-SP-060) | Records are handed over 150 days before expiry for CBG Fire and 120 days for CBG Motor (defaults). | The lead days per segment are to be confirmed. | Confirm the lead days. |
| CLR-SP-07 | Mail house (BRIDSP-26; FR-SP-062) | Print letters are produced as a merged PDF batch with a control list for COG. | The COG format and transfer are open (SP SQ09, XQ03). | Give the COG format (SP SQ09). |
| CLR-SP-08 | Report layouts (FR-SP-080) | The report columns and file names are those of the Report List of 13-May-2026 (section 6.2). The two reports that the list does not define (SBM-EXPIRING-UNINSURED, SBM-PROPOSALS) carry the proposed columns of section 6.1. | The Report List now defines the Submitted Policies reports (SP SQ25 answered). | Confirm the columns of the two new reports. |
| CLR-SP-09 | ISYS reference and ARF (section 7) | The BIBS ARN replaces the ISYS reference and the ARF. | BIBS replaces ISYS (SP SQ20). | Confirm the ARN (SP SQ20). |
| CLR-SP-10 | Response time and retention (section 8) | The BIBS-wide targets apply to Submitted Policies. | The BRD gives 2 seconds and 5 + 5 years, which differ from BRD-1 and Operations (SP SQ21, SQ22). | Confirm the response time and retention (SP SQ21, SQ22). |
| CLR-SP-11 | Baseline of this FRS (R1; SP SQ26) | This FRS follows the issue received on 08-Oct-2026 (BRIDSP-01 to 36), signed by all approvers between 22-Apr and 12-May-2026. | The revision log of that issue still ends at v1.3 of 20-Apr-2026, the same version as the earlier e-signed copy, although the requirements were rewritten and renumbered. | Confirm in writing that this issue is the approved baseline and add a revision-log entry for the changes. |
| CLR-SP-12 | Loan files beyond LAMD (BRIDSP-02, 14, 15, 17; FR-SP-030 to 035; SP SQ27) | LAMD, LMS and LAD are loan reports of one list, each with its layout. Loan records are matched by PN number, then loan application number. When two reports hold the same loan, LAMD is used first, then LMS, then LAD. Unmatched loan records are listed for review. | The BRD now names LMS and LAD next to LAMD without defining them or their precedence. | Define LMS and LAD, their layouts, frequency and delivery, and confirm the matching keys and the precedence. |
| CLR-SP-13 | Monthly expiring and uninsured-loan report (BRIDSP-03; FR-SP-083; SP SQ28) | Part 1 lists the accounts whose policy expires in the month; part 2 lists the loans that are Active in the latest loan file of the month and have no policy covering the last day of the month. | The BRD does not say which loan file is the source of the active loans or what counts as an active policy. | Confirm the source, the rule and the columns. |
| CLR-SP-14 | Manual renewal tag (BRIDSP-05; FR-SP-003; SP SQ31) | The manual Renewable / Non-Renewable tag is offered for Non-CBG Corporate records only; the list of segments is a parameter. | The BRD limits the tag to "a defined segment (i.e. NON CBG Corporate)". | Confirm the segments. |
| CLR-SP-15 | Proposals with nominated rates and preferred insurer (BRIDSP-18, 19; FR-SP-066, 067; SP SQ29) | Proposals are created in Submitted Policies from the Renewal Work List, one by one or in batches, with the nominated package rates of a maintained table (package, vehicle classification, insurer) that the user may edit with a reason, and with a preferred accredited insurer; the Team Lead releases them and the hand-off passes them to the Renewal module, which keeps the renewal account, hold cover and letters (decision D2). | The new requirements place proposal creation with the Marketing user of Submitted Policies, while decision D2 gave the renewal of submitted policies to the Renewal module (FRS BRD-6 has no nominated rates). | Confirm that proposals are prepared in Submitted Policies, provide the nominated rate table and the vehicle classifications, and confirm the allowed range of an edited rate. |
| CLR-SP-16 | IAAF number (BRIDSP-07; FR-SP-040; SP SQ30) | The IAAF reference is Region Code-Year-Counter, with the region of the referring branch of the record and a counter per region and year. | The BRD gives a sample format without the source of the region code. | Confirm the format and the source of the region code. |
| CLR-SP-17 | Reports changed in the Report List of 13-May-2026 (section 6.1; SP SQ32) | (a) The Document Processing Fallout report is not in the list: extraction failures join SBM-PROCESS-FALLOUT in Release 2. (b) The Renewable Accounts / Policies report is not in the list: it is replaced by the Renewal Status Report of SBM-CONVERSION. (c) The Renewal Persistency Report is marked Non-CBG Retail: it stays in this catalogue with the listed columns until BDOI names its owner (Submitted Policies or Renewal). (d) The Handling Fee Payment Classification report lists the CLPC payments per account; OTC payments stay tagged by BRIDSP-34. (e) The Sanitation Report becomes the invoice-level list; the earlier count summary is kept as an optional sheet only if BDOI confirms it. | The Report List removed two reports and changed the scope of three. | Confirm (a) to (e). |
| CLR-SP-18 | Notification recipients (BRIDSP-28; FR-SP-082) | The recipients of each event type are defined on a set-up screen (roles, named users, assigned handler or AO, outside addresses, per segment). The exception and fallout notification, "nice to have" in the BRD, is delivered and can be switched off. | The BRD now asks for definable recipients. | Provide the first recipient list per event type. |

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

# Appendix: User-story view

The BRD writes each requirement as a user story ("As a <persona>, I want / I must ... so that ...") with Given / When / Then acceptance criteria. This appendix gives each requirement of the issue received on 08-Oct-2026 (BRIDSP-01 to 36) as one user story, with the FRs that meet it, their acceptance criteria and the test conditions of the test plan with their number of cases. The BRD wording is kept with tidied grammar; where the BRD names the persona "System", "User" or "Marketing User", the story names the persona of section 3.1 who acts or needs the result. Acceptance criteria are numbered in the order of the FR (AC1 is the first criterion of the FR). The table has 36 rows; BRIDSP-04 is met in Release 2.

<!-- table: widths=2.4,7.5,2.3,2.8,3.4 caption="User-story view: BRD requirement, user story, FRs, acceptance criteria and test cases" size=8 -->
| BRD ID | User story | FR IDs | Acceptance criteria | Test cases |
|---|---|---|---|---|
| BRIDSP-01 (p.8) | As a Submitted Handler (CBG), I need to upload the submitted policies (Excel, CSV, ODS, PDF and the like) into BIBS, so that the uploaded data is accepted for further processing. | FR-SP-001 | FR-SP-001 AC1-4 | TC-SP-001.1 to 001.6 (8 cases) |
| BRIDSP-02 (p.8-9) | As a Sanitation Handler, I need BIBS to receive the uploaded loan files (LAMD, LMS and the like) and match them automatically against the submitted policies on defined keys such as the PN number and the loan application number, so that accounts are categorised without manual intervention, unmatched records are flagged and every result is logged. | FR-SP-035, FR-SP-030, FR-SP-031 | FR-SP-035 AC1-4; FR-SP-030 AC1; FR-SP-031 AC1-2 | TC-SP-035.1 to 035.4, 030.1 to 030.2, 031.1 to 031.3 (11 cases) |
| BRIDSP-03 (p.9) | As a Sanitation Handler, I need a report of all accounts with policies expiring in a given month and a list of active loan accounts without an active policy, so that expiring and uninsured loans are followed up. | FR-SP-083 | FR-SP-083 AC1-3 | TC-SP-083.1 to 083.3 (3 cases) |
| BRIDSP-04 (p.9) | As a Submitted Handler (CBG), I need BIBS to extract the policy details from uploaded policy documents and present them for my confirmation, so that only validated data is stored (Release 2). | FR-SP-002 | FR-SP-002 AC1-3 | TC-SP-002.1 to 002.4 (5 cases) |
| BRIDSP-05 (p.9) | As a Policy Review Officer (Non-CBG Corporate), I need to create or update policy details by hand and tag the accounts of a defined segment such as Non-CBG Corporate as Renewable or Non-Renewable, so that policies with incomplete or non-extractable data are still recorded and renewal opportunities are identified and acted upon. | FR-SP-003 | FR-SP-003 AC1-4 | TC-SP-003.1 to 003.5 (8 cases) |
| BRIDSP-06 (p.9) | As a Submitted Handler (CBG), I need BIBS to store all submitted policy details and let me view, update and extract them per market segment, so that each segment works from one masterlist. | FR-SP-010 | FR-SP-010 AC1-3 | TC-SP-010.1 to 010.4 (5 cases) |
| BRIDSP-07 (p.9) | As a Policy Review Officer (Non-CBG Corporate), I need to generate one IAAF per policy with a unique reference number (Region Code-Year-Counter) and link it to related policy reviews, so that reviews are traceable. | FR-SP-040 | FR-SP-040 AC1-4 | TC-SP-040.1 to 040.5 (6 cases) |
| BRIDSP-08 (p.9) | As a Policy Review Officer (Non-CBG Corporate), I need BIBS to store and show the remarks and review dates of each policy review within the IAAF, so that the review history and review count are tracked. | FR-SP-040 | FR-SP-040 AC1-4 | TC-SP-040.1 to 040.5 (6 cases) |
| BRIDSP-09 (p.9) | As a Submitted Checker, I need BIBS to route the IAAF for approval and signature by the IAAF Approval Matrix and its criteria, so that governance is enforced. | FR-SP-041 | FR-SP-041 AC1-3 | TC-SP-041.1 to 041.4 (5 cases) |
| BRIDSP-10 (p.9) | As a Rule administrator, I need to define the matching, sanitation and account classification criteria, so that the processing rules are configurable and applied consistently. | FR-SP-020 | FR-SP-020 AC1-2 | TC-SP-020.1 to 020.4 (7 cases) |
| BRIDSP-11 (p.9) | As a Sanitation Handler, I need BIBS to process uploaded documents and trigger the downstream workflows (sanitation, matching, disposition, account classification), so that the business processes run automatically and accounts are tagged by the defined criteria. | FR-SP-021 | FR-SP-021 AC1-2 | TC-SP-021.1 to 021.4 (4 cases) |
| BRIDSP-12 (p.10) | As a Sanitation Handler, I need BIBS to generate a fallout report after processing, so that exceptions and failed records are clearly identified. | FR-SP-022 | FR-SP-022 AC1-2 | TC-SP-022.1 to 022.3 (3 cases) |
| BRIDSP-13 (p.10) | As a Sanitation Handler, I need BIBS to identify whether an account or policy is Inforced or Submitted, so that the correct renewal and disposition rules apply. | FR-SP-030 | FR-SP-030 AC1 | TC-SP-030.1 to 030.2 (2 cases) |
| BRIDSP-14 (p.10) | As a Sanitation Handler, I need BIBS to place Submitted policies (PN matched against the loan reports LAMD, LMS, LAD) into qualification buckets, so that the correct renewal action is applied and the next workflow is triggered. | FR-SP-031 | FR-SP-031 AC1-2 | TC-SP-031.1 to 031.3 (4 cases) |
| BRIDSP-15 (p.10) | As a Sanitation Handler, I need BIBS to identify the active loans of the loan reports (for example LAMD non-amortised accounts), so that the right RA template, Generic or FFY, is generated. | FR-SP-032 | FR-SP-032 AC1 | TC-SP-032.1 to 032.2 (3 cases) |
| BRIDSP-16 (p.10) | As a Sanitation Handler, I need BIBS to assign policies to the non-renewal buckets (Free First Year, SM Group Employee, No Touch, CARI, Bonds, RMU) automatically, so that they are excluded from renewal and recorded with their exclusion reason. | FR-SP-033 | FR-SP-033 AC1-2 | TC-SP-033.1 to 033.3 (4 cases) |
| BRIDSP-17 (p.10) | As a Sanitation Handler, I need BIBS to group Inforced policies with unmatched PN into qualified buckets (Fully Paid Loan, OPM, BDOFC, Sold), so that the required RAs and proposals can be created. | FR-SP-034 | FR-SP-034 AC1 | TC-SP-034.1 to 034.2 (2 cases) |
| BRIDSP-18 (p.10) | As a Sanitation Handler, I need to create individual or batch proposals with nominated package rates that I can enter or edit and apply to one or many accounts by vehicle classification and / or insurer, so that proposals are generated with the right rates and are ready for review. | FR-SP-066 | FR-SP-066 AC1-4 | TC-SP-066.1 to 066.4 (7 cases) |
| BRIDSP-19 (p.10) | As a Sanitation Handler, I need to assign a proposal or a batch of proposals to a preferred accredited insurer when it is created, so that each proposal goes to the insurer we prefer. | FR-SP-067 | FR-SP-067 AC1-3 | TC-SP-067.1 to 067.3 (4 cases) |
| BRIDSP-20 (p.10) | As a Marketing AO / MAO, I need BIBS to detect the accounts exceeding acceptance or coverage limits, so that insurer approval is required for them. | FR-SP-050 | FR-SP-050 AC1 | TC-SP-050.1 to 050.2 (2 cases) |
| BRIDSP-21 (p.11) | As a Marketing AO / MAO, I need to generate a Terms of Reference for an account that exceeds the acceptance or coverage limits, so that approval can be obtained. | FR-SP-051 | FR-SP-051 AC1 | TC-SP-051.1 to 051.2 (3 cases) |
| BRIDSP-22 (p.11) | As the TSU, I need BIBS to route the TOR for approval and signature by the TSU Approval Matrix and its criteria, so that the governance rules are followed. | FR-SP-052 | FR-SP-052 AC1 | TC-SP-052.1 to 052.2 (3 cases) |
| BRIDSP-23 (p.11) | As a Marketing AO / MAO, I need to receive or be notified of the approved TOR and view or download it, so that I can proceed with the renewal and the insurer engagement. | FR-SP-053 | FR-SP-053 AC1 | TC-SP-053.1 to 053.2 (2 cases) |
| BRIDSP-24 (p.11) | As a Team Lead, I need to generate reports of the business processing results (sanitation, disposition, account classification and the others) and of the exceptions, so that the results can be monitored. | FR-SP-080 | FR-SP-080 AC1-3 | TC-SP-080.1 to 080.3 (5 cases) |
| BRIDSP-25 (p.11) | As a Team Lead, I need BIBS to let me view, download and print reports in several formats such as Excel and PDF, so that reporting is flexible. | FR-SP-081 | FR-SP-081 AC1 | TC-SP-081.1 to 081.2 (2 cases) |
| BRIDSP-26 (p.11) | As a Sanitation Handler, I need BIBS to send letters (reminder, RA and others) or proposals automatically when the defined criteria are met, so that manual intervention is minimised. | FR-SP-062 | FR-SP-062 AC1-3 | TC-SP-062.1 to 062.4 (5 cases) |
| BRIDSP-27 (p.11) | As a Sanitation Handler, I need BIBS to detect expiring policies and start the renewal processing, so that policy continuity is maintained. | FR-SP-060 | FR-SP-060 AC1-3 | TC-SP-060.1 to 060.4 (7 cases) |
| BRIDSP-28 (p.11) | As a Team Lead, I need BIBS to generate and send notifications for the key policy events and exceptions to recipients I can define, so that they are informed and act on time without manual follow-up. | FR-SP-082 | FR-SP-082 AC1-3 | TC-SP-082.1 to 082.4 (5 cases) |
| BRIDSP-29 (p.11) | As a Sanitation Handler, I need to receive the list of accounts and policies subject for renewal, so that they can be prepared for call-out and placement. | FR-SP-061 | FR-SP-061 AC1-2 | TC-SP-061.1 to 061.3 (3 cases) |
| BRIDSP-30 (p.12) | As a Placement and Booking user, I need to process the placement files of the clients who renew with BDOI, so that the policies are placed efficiently with the insurer. | FR-SP-064 | FR-SP-064 AC1 | TC-SP-064.1 to 064.2 (2 cases) |
| BRIDSP-31 (p.12) | As a Placement and Booking user, I need to book the account so that the booked status shows in the Submitted Masterlist and the policy lifecycle is completed. | FR-SP-065 | FR-SP-065 AC1-2 | TC-SP-065.1 to 065.2 (2 cases) |
| BRIDSP-32 (p.12) | As a Submitted Handler (CBG), I need to view and extract the Submitted Masterlist according to my role, so that data access is controlled. | FR-SP-011 | FR-SP-011 AC1-3 | TC-SP-011.1 to 011.3 (4 cases) |
| BRIDSP-33 (p.12) | As a Submitted Handler (CBG), I need to monitor the updates to the handler, conversion status and remarks in the Submitted Masterlist, so that policy tracking remains accurate. | FR-SP-012 | FR-SP-012 AC1-2 | TC-SP-012.1 to 012.3 (4 cases) |
| BRIDSP-34 (p.12) | As an Admin Team UPP handler, I need BIBS to tag the handling-fee payments in the Unapplied Payment List automatically by the PN number (CLPC) or the location reference number (OTC), so that they are classified and ready for application, reporting and audit without manual intervention. | FR-SP-070 | FR-SP-070 AC1-2 | TC-SP-070.1 to 070.3 (4 cases) |
| BRIDSP-35 (p.12) | As a Sanitation Handler, I need to update the assigned insurer even when a hold cover request is open, so that the hold cover request goes to the newly assigned insurer. | FR-SP-063 | FR-SP-063 AC1-2 | TC-SP-063.1 to 063.3 (5 cases) |
| BRIDSP-36 (p.12) | As a Team Lead, I need BIBS to migrate the existing submitted masterlists in Excel with their original status and key data and an error log for failed rows, so that the historical masterlist data is preserved, standardised and available in BIBS. | FR-SP-004 | FR-SP-004 AC1-3 | TC-SP-004.1 to 004.4 (5 cases) |

<!-- landscape -->

# Appendix: Storyboard index

Submitted Policies has no end-to-end walkthroughs with screenshots yet. This index gives the storyboard of its business processes from the process flow of section 2.2 (Figure 1): one frame per step, with the persona, the step, the screen or document used, the outcome and the FRs. The screen-level frames with screenshots follow with the v2.0 business sign-off pack of BRD-12, which adds the walkthroughs and the UX Screen Deck.

<!-- table: widths=1.3,2.8,5.6,3.6,5.2,2.2 caption="Storyboard of the business processes: frame, persona, step, screen or document, outcome and FR" size=8 -->
| Frame | Persona | Step | Screen or document | Outcome | FR |
|---|---|---|---|---|---|
| **P1** | | **Intake, loan files and classification** | | | |
| P1.1 | Submitted Handler (CBG) | Uploads the LFS insurance report and a PDF policy document | Upload & Intake | Records RECEIVED with SBM numbers; the PDF attached for manual entry | FR-SP-001 |
| P1.2 | Policy Review Officer (Non-CBG Corporate) | Creates a Non-CBG Corporate policy by hand and tags it Renewable | Masterlist (New Policy); Policy record | Record VALIDATED with the Renewable chip | FR-SP-003 |
| P1.3 | Team Lead | Migrates a legacy Excel masterlist | Upload & Intake (Migration); Migration Error Log | Records flagged Migrated; failed rows in the error log | FR-SP-004 |
| P1.4 | Sanitation Handler | Uploads the LAMD and LMS loan files | Upload & Intake (Loan Files) | Loans matched by PN or application number; unmatched loans listed | FR-SP-035 |
| P1.5 | Sanitation Handler | Follows the processing run | Processing Runs | Records sanitised, classified Inforced or Submitted and bucketed; fallout with reasons | FR-SP-021, FR-SP-030, FR-SP-031, FR-SP-033, FR-SP-034 |
| P1.6 | Sanitation Handler | Resolves the fallout and the unmatched records | Masterlist (Fallout, Unmatched); Fallout report | Corrected records re-run; manual dispositions with reasons | FR-SP-022, FR-SP-031 |
| P1.7 | Rule administrator | Changes a disposition rule; the NB Team Lead approves it | Submitted Policies Setup; My Approvals | New rule version applies from the next run | FR-SP-020 |
| **P2** | | **Policy review, IAAF, limits and TOR** | | | |
| P2.1 | Policy Review Officer (Non-CBG Corporate) | Records two reviews and generates the IAAF | Policy Reviews / IAAF | IAAF Region Code-Year-Counter with the review history | FR-SP-040 |
| P2.2 | Submitted Checker | Approves the IAAF at level 1; the NB Team Lead at level 2 | My Approvals; IAAF page | IAAF approved, stamped and e-mailed to the bank counterpart | FR-SP-041 |
| P2.3 | Marketing AO / MAO | Prepares the TOR of an account flagged for a limit breach | Policy record; TOR | TOR generated and submitted | FR-SP-050, FR-SP-051 |
| P2.4 | TSU | Approves the TOR by the TSU matrix | My Approvals; TOR page | TOR approved and handed over; the AO is notified and downloads it | FR-SP-052, FR-SP-053 |
| **P3** | | **Proposals, renewal hand-off, placement and booking** | | | |
| P3.1 | Sanitation Handler | Creates a batch of proposals with nominated rates and edits the rate of a vehicle classification | Renewal Work List (Create Proposals) | One proposal per record with the applied rates; batch For Review | FR-SP-066 |
| P3.2 | Sanitation Handler | Assigns part of the batch to a preferred accredited insurer | Proposal Batch page (Assign Insurer) | Insurer and rates updated on the selected proposals | FR-SP-067 |
| P3.3 | Team Lead | Releases the proposals | Proposal Batch page | Proposals Released for the hand-off | FR-SP-066 |
| P3.4 | Sanitation Handler | Lets the expiry scan hand the records over and reviews the work list | Renewal Work List | Records RENEWAL_IN_PROGRESS with the insurer and proposal; letters sent by the rules | FR-SP-060, FR-SP-061, FR-SP-062 |
| P3.5 | Sanitation Handler | Re-assigns the insurer of a hold cover not accepted in time | Renewal Work List (Insurer Not Accepted) | New hold cover request to the new insurer | FR-SP-063 |
| P3.6 | Placement and Booking user | Places and books the renewal account | Placement and Booking Workbench (BRD-1) | Masterlist shows PLACED, then BOOKED with the invoice | FR-SP-064, FR-SP-065 |
| **P4** | | **Handling fee and monitoring** | | | |
| P4.1 | Admin Team UPP handler | Reviews the payments tagged as handling fees | Handling Fees; Unapplied Payments | CLPC and OTC payments tagged and applied with an OR | FR-SP-070 |
| P4.2 | Team Lead | Runs the Sanitation, Disposition and Conversion reports and exports them | Reports (Submitted Policies) | Report columns of the Report List in Excel and PDF | FR-SP-080, FR-SP-081 |
| P4.3 | Sanitation Handler | Runs the monthly expiring and uninsured-loan report | Reports (SBM-EXPIRING-UNINSURED) | Expiring accounts and active loans without an active policy | FR-SP-083 |
| P4.4 | Rule administrator | Defines the recipients of the TOR hand-over notice | Submitted Policies Setup (Notification Recipients); notifications | The named users receive the notices of the event | FR-SP-082 |
| P4.5 | Submitted Handler (CBG) | Updates the handler and conversion status and extracts the masterlist of the segment | Masterlist; Policy record (History) | Latest values and history; extract within the user's scope | FR-SP-010, FR-SP-011, FR-SP-012 |

