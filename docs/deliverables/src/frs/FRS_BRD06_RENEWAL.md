---
# Source of the Functional Requirements Specification for BRD-6 Renewal.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD06_RENEWAL.md
title: Renewal
subtitle: BRD-6 Renewal (RMEL Phase 2 Online Dispositioning, Addendum 1 and Workshop Addendum)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-06
name: Renewal
doc_id: BIBS-FRS-BRD-06
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-6 Renewal
output: FRS/BIBS_FRS_BRD-06_Renewal_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-6 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions D1 to D4
distribution:
  - {name: "Product Owner, Renewal", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Unit Head, Combank and Corbank", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Retail and Corporate Marketing (Team Leaders, Account Officers)", role: Business user, organisation: BDOI, purpose: "Review of extraction, assignment, disposition and review"}
  - {name: "Processing (Team Leaders, Processing Officers)", role: Business user, organisation: BDOI, purpose: "Review of processing, insurer and Renewal Advice steps"}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: "Review of administration, lists of values and templates"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Renewal business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to build the Renewal module, to test it and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirements it meets and their BRD pages.

Renewal is **designed and not yet built**. The FRs describe the behaviour of the build design (R5). Screen names and API paths are those of the design and are confirmed at build. Error codes are assigned at build; this document gives the message text only, except where a check reuses a platform code that already exists (section 1.5).

## Scope

The scope is the renewal of **client policies placed and booked by BDOI**: from the extraction of the expiring accounts (the legacy "RMEL" list) to the disposition by Marketing, the processing and insurer round-trip, the Renewal Advice and other letters, the client's acceptance, and the placement and booking of the renewal. Packaged and non-packaged accounts follow one process (p.43).

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access and administration | Log-in, session, multi-tab, profiles and function grants, lists of values, templates, LAMD and Contact Center roles | BRD x.001, x.002, 5.003-5.005, 6.002; BRRN.024, 026 |
| Expiring list | Automatic and on-demand extraction, date range, filter criteria, one scrollable grid, column filters and search, export, print | BRRN.002-008, 030; BRD 1.003, 3.003 |
| Governance rules | In-system sanitation, unique reference, Clean / Review / Exception buckets, decision matrix, explicit initiation, non-renewable risk codes, LAMD reports, KYC flag, endorsements on the mother policy | BRRN.009, 020-023, 028-032, 034, 039 |
| Marketing | Assignment, re-assignment, transfer between units, disposition with reasons and remarks, account history, TL review, return and post, outstanding-balance override, New Business path | BRD 1.004-1.008, 1.011, 2.003-2.007; BRRN.011-017, 027, 033 |
| Processing and insurer | Dispositioned-file upload, assignment to Processing Officers, data update, computations, return to Marketing, extract per insurer, insurer responses | BRD 3.004-3.009, 4.003-4.007; BRRN.018, 035, 038 |
| Letters and acceptance | Renewal Advice (first and second notice), NAL, NFR, NRNS reminder and non-acceptance letters, Contact Center follow-up, acceptance and automatic progression to placement and booking | BRRN.001, 010, 025, 026, 037, 040 |
| Monitoring | Renewal home and dashboards, renewal status report with 34-line summary, centralised listings with escalation, other renewal reports | BRD 1.009, 2.008, 3.010, 4.008; BRRN.019, 036 |
| Submitted policies | Renewal of submitted policies handed over by BRD-12 (`RenewalHandOff`) | Cross-BRD decision D2 |

**Out of scope for this phase:**

- Requirements that reference BDOIsys, EBIX or QPS (Addendum 1, p.34): the nine BRD IDs of section 11.3. The BIBS renewal reference and invoice number replace those references, and the reports stay.
- Renewal of Employee Benefits programmes. They are renewed in the EB module (BRD-8) and excluded from the renewal lists (decision D3).
- Renewal of **package products** (the insurer agreement). This is BRD-3 (BRPM.017); BRD-6 renews the client policies sold on them.
- Accounting entries of their own. A renewal books through the BRD-1 booking with business type RENEWAL; Renewal posts nothing itself.

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Renewal Addendum (Workshop), BRRN.020-040, pages 1-18 of the BRD-6 pack | v1.0, 8 to 10-Apr-2026; approved 16-Apr-2026 | `docs/source-documents/Renewal (RN) BRD.pdf` |
| R2 | Renewal Addendum 1, BRRN.001-019 and the out-of-scope list, pages 19-38 | v1.0, 18-Nov-2025 / 16-Dec-2025; approved 20 to 23-Jan-2026 | same file |
| R3 | RMEL Phase 2 - Online Dispositioning BRD (main BRD, persona IDs 1.xxx to 6.xxx), pages 39-189 | v1.0, 9-May-2025; signed 9 to 27-May-2025 | same file |
| R4 | BDOI Renewal (BRD-6) requirements baseline and fit/gap | current | `docs/requirements/BDOI_RN_BRD_SPEC.md` |
| R5 | Renewal build design | current | `docs/architecture/RENEWAL_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R7 | Submitted Policies build design (`RenewalHandOff`) | current | `docs/architecture/SUBMITTED_POLICIES_DESIGN.md` |
| R8 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |
| R9 | BRD-1 New Business requirements baseline (shared platform capabilities) | current | `docs/requirements/BDOI_NB_BRD_SPEC.md` |

Page references in this document ("p.27") are PDF page numbers of the BRD-6 pack (R1 to R3). The main BRD also carries its own page footer ("x of 151"); this document does not use it.

**Order of precedence.** The Workshop addendum (R1) is the latest signed layer. Where a BRRN ID of Addendum 1 (R2) restates a main-BRD ID, the BRRN governs and both IDs are traced to the same FR. The main BRD (R3) governs the persona IDs that no BRRN restates.

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing); "Marketing AO / Admin" in the BRD, with the Account Broker
ARN: Account Reference Number of a BIBS account (BRD-1)
BRD: Business Requirements Document
BRRN: Requirement ID prefix of the two Renewal addenda (BRRN.001-019 Addendum 1, BRRN.020-040 Workshop addendum)
Bucket: Classification of a renewal candidate after the checks (Clean, Review or Exception; BRRN.023)
Candidate: A renewal record created for one expiring root invoice and policy year; it exists before any renewal account
CBG: Consumer Banking Group segment of the bank (definition to confirm, RQ08)
CLG: Segment initiated in bulk in the workshop example (BRRN.021)
Decision matrix: Versioned rules that propose a disposition and say whether it is automatic or manual (BRRN.034)
Disposition: Marketing's decision on an expiring account (For Renewal, Not for Renewal, For Quotation, For Proposal, Lost Business)
FR: Functional requirement of this document (FR-RN-nnn)
IBG: Institutional Banking Group segment (BRRN.036)
KYC: Know Your Customer review of the client
LAMD: Bank loan unit that supplies the paid-off and RMU loan reports (the BRD does not expand the acronym, RQ20)
LOV: List of values maintained by the Business Administrator
NAL: No Advice Letter, for expiring policies that need neither an RA nor an NFR (BRRN.001)
NB path: A renewal with financial or structural changes processed as a New Business quotation or PRF (BRRN.033)
NFR: Not for Renewal Letter; the addendum also writes NRL
NRNS: No Renew / No Submit account (BRRN.037)
PN: Promissory note number of the bank loan; the key of LAMD matching
PO: Processing Officer
PRF: Proposal Request Form (non-package New Business, BRD-1)
RA: Renewal Advice, the letter that offers the renewal terms to the client (first or second notice)
RMEL: Legacy name of the list of expiring accounts (not expanded in the BRD)
RMU: Remedial Management Unit (BRD 2.004.4.1)
RQnn: Open question on BRD-6 raised by the project team (section 10.3)
STP: Straight-through processing, a renewal that proceeds without manual action (BRRN.031, 039)
TL: Team Leader (Marketing or Processing)
UAT: User acceptance testing
UH: Unit Head
```

<!-- pagebreak -->

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement IDs and pages), the **actor**, the BRD **priority**, the **fit** class of the baseline (R4), and the **screens** and **API** of the build design (R5).
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the Business Administrator or the System Administrator in BIBS (parameter, list of values, rule set or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. Codes of the Renewal module are assigned at build ("To be assigned at build"). A code is quoted only where the check reuses a platform code that exists today (for example `ACCESS_DENIED`, `WORKFLOW_REASON_REQUIRED`). A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-6 test plan.

The main BRD repeats most capabilities once per persona (for example report generation under 1.009, 2.008, 3.010 and 4.008). One FR covers the capability for every persona that has it, and its BRD trace lists each persona's IDs. A range such as "1.009.2.1-40" stands for every line ID in it; the full list of line IDs with their pages is in section 11.2.

> [!NOTE]
> Values marked "default" (lead days, notice days, thresholds, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the platform built for BRD-1 |
| CONFIGURE | Needs set-up only (parameters, lists, roles, templates) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that does not exist before BRD-6 |
| OUT | Out of scope per the BRD (Addendum 1, p.34) |

# Business context and process overview

## Business context

BDOI renews the policies it has placed. Today IT extracts the list of expiring accounts, Marketing records its dispositions in spreadsheets, communication runs by e-mail, and the upload to QPS and the text file for Renewal Advices work for packaged accounts only; non-packaged accounts and exceptions are handled by hand (p.42). The BRD asks for one online process in which the Business Units extract the list themselves, disposition online with a staged workflow, export the accounts per insurer and monitor each renewal (p.41-43). The Workshop addendum adds rule-based governance: in-system sanitation, a mandatory reference, buckets, a decision matrix and automatic progression to booking (p.3-15).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.42-43, workshop p.3)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | IT extracts the expiring list (RMEL) | A daily job extracts the expiring accounts at the lead days; users also generate the list for any date range |
| 2 | Eligibility depends on manual pre-cleaning of the list | BIBS runs the sanitation, matching and eligibility checks and classifies each record Clean, Review or Exception |
| 3 | Disposition is done in a spreadsheet | The AO dispositions online; clean cases that the decision matrix marks automatic get a system disposition |
| 4 | Communication by e-mail between Marketing and Processing | A staged workflow with assignment, review, return and post, and notifications at each step |
| 5 | Accounts are sent to insurers by e-mail from Excel | BIBS builds the extract per insurer and sends it protected; the insurer's decision file is uploaded and applied |
| 6 | RAs come from an IT text file for packaged accounts only | Users generate RAs (first and second notice) for any account from the latest template and send them in batch, protected |
| 7 | Renewal status is monitored by hand | Renewal home, status report with summary counters, listings by unit and segment with escalation |
| 8 | Accepted renewals are re-keyed for placement and booking | Acceptance moves the renewal account to placement and booking without re-keying; it books as business type RENEWAL |

<!-- pagebreak -->

## Process overview

The table lists the steps of the renewal, and Figure 1 shows them by actor. Each step is a stage of the BIBS workflow RNW_CASE (section 5).

<!-- table: widths=0.8,3.8,3.4,7,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Extraction | System (job) or Marketing / Processing TL | Booked invoices whose policy period ends in the window become renewal candidates, each with a renewal reference. Extraction does not start the renewal | BRRN.002, 005, 021, 030 |
| 2 | Sanitation and buckets | System | Reference and PN matching and the eligibility checks run; the candidate is Clean, Review or Exception. Non-renewable risk codes are tagged Not for Renewal | BRRN.009, 020, 022, 023 |
| 3 | Initiation | Authorised user | Explicit start of renewal processing, in bulk (for example CLG) or one by one (for example Non-Mortgage) | BRRN.021 |
| 4 | Rule-based disposition | System | The decision matrix proposes a disposition; a Clean candidate with an automatic rule proceeds straight through; CBG Motor and Fire follow the LAMD-driven STP | BRRN.031, 034, 039 |
| 5 | Assignment and transfer | Marketing TL | Assigns or re-assigns the account to an AO (non-CBG), or transfers it to another Marketing unit | BRD 1.005-1.007 |
| 6 | Disposition | Marketing AO | Views the account and its history; gives the disposition with reason and remarks; pushes | BRD 2.004; BRRN.027 |
| 7 | Review and post | Marketing TL | Returns the account with remarks or posts it; overrides the outstanding-balance flag | BRD 1.008, 1.011 |
| 8 | Processing | Processing TL / Officer | Uploads dispositioned files; assigns to a PO; updates the renewal account; reviews computations; returns to Marketing | BRD 3.004-3.008, 4.003-4.006; BRRN.018 |
| 9 | Insurer | Processing | Extract per insurer, sent protected; the insurer's file is uploaded; Renew As Is advances, Reject or Revise stops or reroutes | BRD 3.009, 4.007; BRRN.035 |
| 10 | Renewal Advice | Marketing / Processing | RA generated (first or second notice) and sent in batch, protected; the account locks on the Marketing side | BRRN.010 |
| 11 | Follow-up and letters | System, Contact Center | NRNS reminders and non-acceptance letters; NAL and NFR; Contact Center remarks and client documents | BRRN.001, 009, 025, 026, 037 |
| 12 | Acceptance, placement, booking | Client, System | Acceptance recorded; the renewal account is placed and queued for booking; it books as RENEWAL | BRRN.038, 040 |
| 13 | New Business path | Marketing | A renewal with financial or structural changes becomes a quotation or PRF tagged "For Proposal / New Business Path" | BRRN.033, 038 |
| 14 | Monitoring | All | Home, reports, summary counters, listings with escalation | BRD 1.009, 2.008, 3.010, 4.008; BRRN.019, 036 |

![Renewal process by actor (BRRN.001-040, BRD 1.003-4.09)](figures/brd06_process_flow.dot)

## Dispositions and paths

The disposition decides the path of the account after the TL posts it. The codes are fixed because they drive the workflow; their labels are maintained in the list RNW_DISPOSITION.

<!-- table: widths=3,4.6,9 caption="Dispositions (BRD 2.004.3, p.81)" -->
| Code | Label | Path after posting |
|---|---|---|
| FOR_RENEWAL | For Renewal | Processing (FOR_PROCESSING), insurer round-trip, Renewal Advice, acceptance, placement and booking |
| NOT_FOR_RENEWAL | Not for Renewal | Letter step (NFR or NAL), then closed as NOT_RENEWED; can be reopened until expiry plus the reopen days |
| FOR_QUOTATION | For Quotation | New Business path: a package quotation pre-filled from the expiring account |
| FOR_PROPOSAL | For Proposal | New Business path: a PRF (non-package) pre-filled from the expiring account |
| LOST_BUSINESS | Lost Business | Closed as LOST |

# Personas and roles

## Personas

<!-- table: widths=3.2,3.3,8.1,3 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in Renewal | BRD |
|---|---|---|---|
| Marketing Team Leader | MKT_TL | Generates the expiring list; initiates; assigns and re-assigns AOs; transfers and receives accounts; reviews, returns and posts; overrides the outstanding-balance flag; RAs; reports | BRD 1.001-1.011 |
| Marketing AO / Admin, Account Broker | MKT_AO | Views assigned accounts; dispositions; remarks; requests transfers; updates returned accounts; RAs; reports | BRD 2.001-2.009 |
| Processing Team Leader | PROCESSING_TL | Generates the list for processing; uploads dispositioned files; assigns to POs; updates data; computations; insurer extract and responses; RAs; reports | BRD 3.001-3.011 |
| Processing Officer / Broker | PROCESSOR | Processes assigned accounts; uploads; updates data; computations; insurer extract and responses; RAs; reports | BRD 4.001-4.09 |
| Business Administrator | BUSINESS_ADMIN | Maintains lists of values, renewal rules and templates; assigns user profiles | BRD 5.001-5.005 |
| System Administrator | SYSADMIN | Defines profiles and assigns functions to them | BRD 6.001-6.002 |
| LAMD user | LAMD (new) | Uploads paid-off and RMU reports; validation and eligibility checks only | BRRN.024, 029 |
| Contact Center user | CONTACT_CENTER (new) | Views renewal lists; records follow-up remarks; uploads client documents | BRRN.026 |
| Client | - (external) | Receives the RA and letters; accepts by e-mail, signed RA or payment | BRRN.010, 040 |
| Insurer | - (external) | Receives the renewal extract; returns its decision file | BRD 3.009; BRRN.035 |

The Processing Officer section of the BRD says "Log in with a Operations Team Leader user profile" (4.001.1.1, p.148). BIBS reads it as the Processing Officer profile. The final role-to-function matrix is open (OQ48, RQ20, RQ21).

## Permissions

The BRD lists 25 functions that the System Administrator assigns to profiles (6.002.2.1-25, p.181-185). BIBS gives each function a permission.

<!-- table: widths=4.6,7.2,4.8 caption="Renewal permissions and the BRD functions they carry" -->
| Permission | Allows | BRD function (6.002.2.n) |
|---|---|---|
| RNW_VIEW | Renewal home, lists, record page, account history | 4, 11, 13, 17 |
| RNW_EXTRACT | Generate the expiring list for a range, run the extraction, initiate candidates | 3, 15 |
| RNW_ASSIGN | Assign and re-assign AOs, transfer, receive transfers (an AO only requests transfers) | 5, 6, 7 |
| RNW_DISPOSE | Give the disposition, remarks, update returned accounts, start the NB path | 12, 14 |
| RNW_REVIEW | TL review, return and post | 8, 22 |
| RNW_OVERRIDE | Outstanding-balance, bucket, disposition and insurer-mismatch overrides; RA cancellation | BRD 1.011; BRRN.023, 031, 035 |
| RNW_PROCESS_ASSIGN | Assign accounts to Processing Officers | 5 (processing) |
| RNW_PROCESS | Update data, review computations, return to Marketing | 18, 19, 22 |
| RNW_UPLOAD | Upload dispositioned files | 16 |
| RNW_INSURER | Extract per insurer, send, upload insurer responses | 20 |
| RNW_RA_GENERATE | Generate, view and download RA, NAL and NFR | 10, 21 |
| RNW_RA_SEND | Send letters in batch | 10 |
| RNW_ACCEPT | Record client acceptance | BRRN.040 |
| RNW_FOLLOWUP | Contact Center remarks, documents and follow-ups | BRRN.026 |
| RNW_LAMD_UPLOAD, RNW_VALIDATE | LAMD reports and validation checks | BRRN.024, 029 |
| RNW_REPORT_VIEW, RNW_EXPORT | Renewal reports; exports | 9 |
| RNW_SETUP | Non-renewable risk codes, check settings, bucket rules, decision matrix, lead days | 23 (rules) |
| RNW_TEMPLATE_MAINTAIN | Update the renewal templates | 25 |
| ATTACHMENT_MANAGE, LOV_MANAGE, ACCESS_REQUEST (existing) | Acquire documents, maintain LOVs, request user access | 1, 23, 24 |

Function 2 (open the application in several tabs) needs no permission; it is the platform behaviour for every user.

<!-- pagebreak -->

## Permissions matrix

The table is the role-to-permission matrix proposed in the build design ("Y" = granted; "T" = transfer request only). The System Administrator maintains it through role-permission change requests (FR-RN-003).

<!-- table: widths=4.4,1.52,1.52,1.52,1.52,1.52,1.52,1.52,1.52 caption="Role-to-permission matrix for Renewal (proposal until OQ48 / RQ20 / RQ21)" size=8 -->
| Permission | MKT TL | MKT AO | Proc. TL | Proc. Officer | Bus. Admin | Sys. Admin | LAMD | Contact Center |
|---|---|---|---|---|---|---|---|---|
| RNW_VIEW | Y | Y | Y | Y | Y | | Y | Y |
| RNW_EXTRACT | Y | | Y | | | | | |
| RNW_ASSIGN | Y | T | | | | | | |
| RNW_DISPOSE | Y | Y | | | | | | |
| RNW_REVIEW | Y | | | | | | | |
| RNW_OVERRIDE | Y | | | | | | | |
| RNW_PROCESS_ASSIGN | | | Y | | | | | |
| RNW_PROCESS | | | Y | Y | | | | |
| RNW_UPLOAD | | | Y | Y | | | | |
| RNW_INSURER | | | Y | Y | | | | |
| RNW_RA_GENERATE | Y | Y | Y | Y | | | | |
| RNW_RA_SEND | Y | Y | Y | Y | | | | |
| RNW_ACCEPT | Y | Y | Y | Y | | | | |
| RNW_FOLLOWUP | | | | | | | | Y |
| RNW_LAMD_UPLOAD | | | | | | | Y | |
| RNW_VALIDATE | | | | | | | Y | |
| RNW_REPORT_VIEW | Y | Y | Y | Y | Y | | | |
| RNW_EXPORT | Y | Y | Y | | | | | |
| RNW_SETUP | | | | | Y | | | |
| RNW_TEMPLATE_MAINTAIN | | | | | Y | | | |
| LOV_MANAGE | | | | | Y | | | |
| Role-permission change requests | | | | | | Y | | |

**Data scope.** Marketing users see the candidates of their sales units, and an AO sees the candidates assigned to him. Processing users see the processing stages. LAMD and Contact Center users see a read-only projection without premium columns (to confirm, RQ21). The scope is applied by the server, never by the screen only.

**Segregation of duties.** Activation of a bucket rule set, a decision matrix or a non-renewable risk code needs a checker other than the maker (MASTER_AUTHORIZE). The LAMD role never holds a placement, booking, issuance, letter, disposition or messaging permission (BRRN.024).

# Functional requirements

## Access, security and audit

```fr
id: FR-RN-001
title: Log in, session warnings and multiple tabs
brd:
  - BRD 1.001.1-1.001.3.1, 2.001.1-2.001.1.3, 3.001.1-3.001.1.3, 4.001.1-4.001.1.3, 5.001.1-5.001.1.3, 6.001.1-6.001.1.1 (p.44, 78-79, 108, 148, 179-180)
  - BRD 1.002.1, 2.002.1, 3.002.1, 4.002.1, 5.002.1 (p.44, 79, 109, 148, 179)
actor: All Renewal personas
priority: Must have
fit: FIT
screens: Login; session warning dialog; any Renewal screen in a second tab
api: POST /api/v1/auth/login; GET /api/v1/system/session-policy
description:
  - Users of every Renewal persona log in on any BDO-issued device with their BIBS user ID and the profile of their persona. Renewal uses the platform log-in, session policy and multi-tab behaviour built for BRD-1; it adds no screen of its own.
  - BIBS warns the user after 15 minutes of inactivity and 30 minutes before the system-triggered log-out, as the BRD asks for each persona.
preconditions:
  - The user has an active BIBS account with a Renewal role.
main_flow:
  - The user enters the user ID and password on the Login screen.
  - BIBS checks the credentials and opens the home page with the menu of the user's roles (section Renewal).
  - The user opens Renewal screens in several browser tabs; each tab works on its own and the session is shared.
alternate_flows:
  - Idle user. After the idle-warning minutes BIBS shows the warning; the user continues or is logged out at the idle limit.
  - End of session. The warning appears the configured minutes before the system log-out.
  - Wrong credentials. BIBS refuses the log-in; after the configured failed attempts the account locks (3 attempts, decision D5).
rules:
  - [R1, "Idle warning after 15 minutes; warning 30 minutes before the system log-out.", Configurable, "Parameters SESSION_IDLE_WARNING_MINUTES, SESSION_EXPIRY_WARNING_MINUTES"]
  - [R2, "Account lock after 3 failed attempts for all users (decision D5).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R3, "Directory sign-in (BDO EUA / Windows ID) is built as a parked port; local sign-in stays until BDO supplies the interface (decision D6).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded with user, time and source address.
acceptance:
  - A user with the Processing TL profile logs in and sees the Renewal section with the Processing screens only.
  - The idle warning appears after 15 minutes of inactivity.
  - The user works on the Expiry List in one tab and on a record page in a second tab without losing either.
```

```fr
id: FR-RN-002
title: Restrict each function and each record to authorised users
brd:
  - BRD 6.002.2.1-6.002.2.24 (p.181-185)
  - BRRN.024 (p.6)
  - BRD p.43 key capability (Team Leaders view only their team's accounts)
actor: System
priority: Must have
fit: CHANGE
screens: All Renewal screens; User Access Matrix
api: Every endpoint checks its permission and the data scope
description:
  - Every Renewal screen, button and API call requires a permission (section 3.2). Menus show only the screens that the user's roles allow; buttons for actions the user may not perform are hidden.
  - Every list, record, report and export is limited to the user's data scope. A Marketing TL sees the candidates of his sales units, an AO those assigned to him, Processing the processing stages. LAMD and Contact Center users see a projection without premium columns.
  - The LAMD role is limited to validation and never holds booking, issuance, placement, letter or customer-communication permissions (BRRN.024).
preconditions:
  - The user is logged in.
main_flow:
  - The user opens a screen or starts an action.
  - BIBS checks the permission of the action and the scope of the record.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link shows "You do not have access to this page". An API call is refused and logged.
  - Out of scope. A record outside the user's scope is not listed and cannot be opened.
rules:
  - [R1, "Roles are granted permissions as in section 3.3 until BDOI confirms the matrix (OQ48, RQ20, RQ21).", Configurable, Role-permission change request (FR-RN-003)]
  - [R2, "Data scope is applied by the server in every query, report and export.", Fixed, "-"]
  - [R3, "The LAMD role cannot be granted placement, booking, issuance, letter or messaging permissions; an automated test guards this.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - Refused API calls are logged with user, endpoint and time.
  - "All LAMD actions are logged (BRRN.024 AC3)."
acceptance:
  - A Marketing AO sees My Dispositions and the record pages of his accounts, and does not see the Processing Worklist or Renewal Setup.
  - A Marketing TL does not see candidates of another sales unit.
  - A LAMD user can upload a LAMD report and view match results, and cannot generate or send an RA.
```

```fr
id: FR-RN-003
title: Define profiles and assign Renewal functions
brd:
  - BRD 6.002.1, 6.002.1.2-6.002.1.6 (p.180-181)
  - BRD 6.002.2, 6.002.2.1-6.002.2.25 (p.181-185)
  - BRD 5.004.1 (p.180)
actor: System Administrator; Business Administrator (requester); access approver
priority: Must have
fit: CONFIGURE
screens: Access Requests; User Access Matrix; Roles
api: POST /api/v1/nbadmin/access-requests (types ASSIGN_ROLE, MODIFY_ROLE_PERMISSIONS)
description:
  - The profiles Marketing TL, Marketing AO / Admin, Processing TL, Processing Officer and Business Administrator exist as BIBS roles (MKT_TL, MKT_AO, PROCESSING_TL, PROCESSOR, BUSINESS_ADMIN). The workshop adds LAMD and CONTACT_CENTER.
  - Each of the 25 BRD functions maps to a permission (section 3.2). The System Administrator assigns functions to a profile through an approved role-permission change request, and the Business Administrator assigns profiles to users through an access request (BRD 5.004.1). Both are the User Access Maintenance flow of BRD-11.
preconditions:
  - The requester has ACCESS_REQUEST.
main_flow:
  - The requester opens Access Requests and chooses the type (assign role to a user, or modify role permissions).
  - The requester selects the user or role and the roles or permissions, and writes the justification.
  - An approver other than the requester approves the request.
  - BIBS applies the change and records it on the role or user history.
alternate_flows:
  - The approver rejects the request with a reason; nothing changes.
rules:
  - [R1, "The approver is never the requester.", Fixed, "-"]
  - [R2, "Users may hold several roles (decision D5); there is no single-role check.", Fixed, "-"]
validations:
  - [No role or permission selected, Select at least one role or permission, "-"]
notifications:
  - The approvers when a request is submitted; the requester when it is decided.
audit:
  - Request, decision and the applied change with users and times.
acceptance:
  - The function "Extract accounts for renewal per insurer" (6.002.2.20) is granted to the Processing Officer profile only after another user approves the request.
  - A user assigned the Marketing AO profile sees the Renewal screens of that profile at the next log-in.
```

```fr
id: FR-RN-004
title: Keep a complete audit trail of renewal actions
brd:
  - BRRN.021 (p.4)
  - BRRN.022 (p.4)
  - BRRN.023 (p.4-6)
  - BRRN.031 (p.9-10)
  - BRRN.035 (p.11-12)
  - BRRN.040 (p.14-15)
actor: System; any user with RNW_VIEW (read)
priority: Must have
fit: FIT
screens: Record page (History tab); Renewal Reports (RNW-DECISIONS, RNW-SANITATION)
api: Audit service; GET /api/v1/renewal/candidates/{ref}/history
description:
  - The workshop addendum asks, requirement by requirement, that every action is logged with the user, the time and the data source. BIBS records extraction, initiation, reference matching, every check run and bucket change, every disposition (user, matrix or upload), override, transfer, return, post, letter generation and sending, insurer response, LAMD match and acceptance.
  - The History tab of a record shows the workflow history, the disposition rows and the audit rows in time order. Extraction and initiation are different events, so the audit tells them apart (BRRN.021 AC3).
preconditions:
  - "None."
main_flow:
  - A user or a job performs an action on a candidate.
  - BIBS writes the history row and the audit row (who, when, what, source, before and after values) in the same transaction.
  - A user opens the History tab and reads the trail.
rules:
  - [R1, "Audit and history rows are append-only; no user can change or delete them.", Fixed, "-"]
  - [R2, "System actions are recorded with the job or rule that made them (for example matrix version and rule id).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - For a renewed account, the History tab lists extraction, initiation, bucket, disposition, post, insurer response, RA, acceptance and booking with user or source and time.
  - An extraction and the later initiation of the same candidate appear as two events with different users and times.
  - No screen or API allows history or audit rows to be edited or deleted.
```

## Extraction and the expiring list

```fr
id: FR-RN-010
title: Extract renewal candidates automatically at the lead days
brd:
  - BRRN.030 (p.8-9)
  - BRRN.005 (p.24-25), restating BRD 1.003.4.1.2, 3.003.4.2 (p.50, 114)
actor: System (job RNW_EXTRACTION)
priority: Must have
fit: NEW
screens: Renewal Home (extraction runs); Expiry List
api: Job RNW_EXTRACTION; GET /api/v1/renewal/extraction-runs
description:
  - Every day BIBS extracts as renewal candidates the booked root invoices (policy years) whose policy expiry date equals the business date plus the lead days (default 140 days; a value per segment may override it). Extraction is expiry-based, not maturity-based (BRRN.030 AC1).
  - Each candidate gets a renewal reference and keeps the expiring invoice number, ARN, policy number and a snapshot of the listing fields. Expired and expiring accounts that are not yet renewed stay listed until the candidate closes, so the list has no omissions and no duplicates (BRRN.005).
  - After extraction BIBS runs the checks of FR-RN-020. A candidate does nothing more until it is initiated (FR-RN-015).
preconditions:
  - The job is active.
main_flow:
  - The job runs at 01:00 PHT.
  - BIBS selects the booked root invoices whose expiry falls on the target date, skipping invoices already extracted and excluded lines.
  - BIBS creates one candidate per invoice with stage EXTRACTED, records the run (RXR-yyyy) with its counts, and runs the checks.
alternate_flows:
  - Multi-year account. Only the last policy year of the term produces a candidate.
  - Run failure. The run is marked failed, alert RNW_EXTRACTION_FAILED is raised, and the next run picks up the missed dates.
  - Placed or issued accounts that are not yet booked ("unbooked", BRRN.005) are included when their period ends in the window (interpretation to confirm, RQ06).
rules:
  - [R1, "Lead days 140 (default); per-segment values override it.", Configurable, "Parameters RNW_EXTRACTION_LEAD_DAYS, RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT"]
  - [R2, "One open candidate per expiring invoice (unique company + expiring invoice no.).", Fixed, "-"]
  - [R3, "Employee Benefits lines (HMO, GLI, GPA) are excluded (decision D3).", Configurable, Parameter RNW_EXCLUDED_LINES]
  - [R4, "Extraction never starts the renewal and never guarantees renewability (BRRN.021, BRRN.030 AC3).", Fixed, "-"]
  - [R5, "Job time 01:00 PHT.", Configurable, Job schedule (System Administrator)]
validations: []
notifications:
  - Alert RNW_EXTRACTION_FAILED to the job owners when a run fails.
audit:
  - "Each run with trigger, range, counts (read, new, existing, skipped), start and end."
acceptance:
  - A policy expiring 140 days after the business date becomes a candidate that day, with a renewal reference, and does not appear twice after a second run.
  - A change of the lead days to 150 takes effect at the next run without a release.
  - An HMO policy is not extracted.
```

```fr
id: FR-RN-011
title: Generate the list of expiring accounts for a date range
brd:
  - BRRN.002 (p.22-23), restating BRD 1.003.1, 1.003.1.1, 1.003.2, 3.003.1, 3.003.1.1, 3.003.2 (p.44-45, 109)
  - BRD 3.003.4, 3.003.4.1 (p.111)
actor: Marketing TL; Processing TL
priority: Must have
fit: NEW
screens: Expiry List (Generate Expiry List dialog)
api: GET /api/v1/renewal/candidates?expiryFrom=&expiryTo=
description:
  - The TL opens the Expiry List and clicks **Generate Expiry List**. He selects a start and end date (month, day, year) and BIBS shows the expiring accounts in that range. Changing the range refreshes the list at once.
  - When the range goes beyond what the job has already extracted, BIBS extracts the missing dates on demand. This is recorded as an extraction, not as an initiation (BRRN.021).
preconditions:
  - The user has RNW_EXTRACT (generate) or RNW_VIEW (view the list).
main_flow:
  - The user clicks **Generate Expiry List**.
  - The user picks the start and end dates (MM/DD/YYYY) and clicks **Generate**.
  - BIBS extracts any missing dates and lists the candidates whose expiry falls in the range, within the user's scope.
  - The user changes the range; the list refreshes.
alternate_flows:
  - No account matches. BIBS shows "No expiring accounts in the selected range".
rules:
  - [R1, "An on-demand extraction uses the same selection as the job and is recorded with trigger MANUAL_RANGE.", Fixed, "-"]
  - [R2, "Users may extract on demand for any range; the scheduled run and the on-demand run share the same duplicate guard (RQ04).", Fixed, "-"]
validations:
  - [Start or end date missing or not a date, Enter a valid start and end date, "-"]
  - [End date before start date, The end date must be on or after the start date, "-"]
fields_screen: Generate Expiry List
fields:
  - [Expiry from, Date, "Yes", "-", MM/DD/YYYY]
  - [Expiry to, Date, "Yes", "-", "On or after expiry from"]
notifications:
  - "None."
audit:
  - "On-demand extractions are recorded as runs with the user, range and counts."
acceptance:
  - A TL generates the list for 01-Mar to 31-Mar and sees every candidate of his units expiring in March, and only those.
  - An end date before the start date cannot be submitted.
  - A range with no expiring account shows the empty-list message.
```

```fr
id: FR-RN-012
title: Filter the expiring list with multi-select criteria
brd:
  - BRRN.003 (p.23), restating BRD 1.003.3, 1.003.3.1-1.003.3.1.13, 3.003.3, 3.003.3.1-3.003.3.13 (p.45-47, 109-111)
actor: Marketing TL; Processing TL
priority: Must have
fit: CHANGE
screens: Expiry List (Filters panel)
api: GET /api/v1/renewal/candidates (filter parameters as include / exclude lists)
description:
  - The Filters panel offers Unit Head, Business Origin, Account Type, Region, Area, Invoicing Branch, Risk Code, Market Segment, Department Name and Account Officer, plus Bucket, Disposition and Stage. Each criterion supports single, multiple, "all except" and all selections, with a search in the drop-down.
  - "**Apply** filters the list at once; **Cancel** closes the panel without change; **Clear** restores all. The selected values stay visible above the grid and in the page address, so a filtered list can be shared."
preconditions:
  - The list is displayed.
main_flow:
  - The user opens Filters and, per criterion, ticks values or chooses "all except" and ticks the values to exclude.
  - The user clicks **Apply**; BIBS filters the list and shows the active filters as chips.
alternate_flows:
  - The user clicks **Clear**; all criteria return to "All".
rules:
  - [R1, "Selection types: single, multiple, all except, all.", Fixed, "-"]
  - [R2, "An 'all except' and an 'only' selection on the same criterion cannot be combined.", Fixed, "-"]
validations:
  - [Conflicting selection on one criterion, "Choose either 'only' or 'all except' for <criterion>", "-"]
fields_screen: Filters
fields:
  - [Unit Head / Business Origin / Account Type / Region / Area / Invoicing Branch / Risk Code / Market Segment / Department Name / Account Officer, Multi-select with search, "No", "Masters and LOVs of each field", "Include or exclude mode"]
  - [Bucket, Multi-select, "No", "Clean, Review, Exception", "-"]
  - [Disposition / Stage, Multi-select, "No", "LOV RNW_DISPOSITION; RNW_CASE stages", "-"]
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - Choosing Risk Code "all except" MC01 lists every expiring account except those with risk code MC01.
  - Two Unit Heads selected together list the accounts of both.
  - Clear returns the full list.
```

```fr
id: FR-RN-013
title: Show the expiring list in one scrollable grid with column filters and search
brd:
  - BRRN.004 (p.23-24), restating BRD 1.003.4.1, 1.003.4.1.1, 1.003.4.1.1.2-1.003.4.1.1.24, 3.003.4.1, 3.003.4.1.2-3.003.4.1.21 (p.47-50, 111-114)
  - BRRN.006 (p.25), restating BRD 1.003.4.1.3, 1.003.4.1.4, 3.003.4.3, 3.003.4.4, 3.006.1.5, 4.003.1.5 (p.50, 114-115, 119, 149-150)
actor: Marketing TL; Processing TL; Processing Officer
priority: Must have
fit: CHANGE
screens: Expiry List (tabs Unassigned Disposition, For Renewal, For Quotation, For Proposal, Not for Renewal, Lost Business, Exceptions, All)
api: GET /api/v1/renewal/candidates (keyset chunks)
description:
  - The list shows every candidate of the range in **one scrollable view**, without pages, with a row count. The columns are those of BRD 1.003.4.1.1 (section 6.2), plus the Classification pill (Clean / Review / Exception) and the flag chips (Returned, Transferred, Endorsed, Claims, Outstanding, KYC due, NRNS). The Processing list has the first 21 columns (3.003.4.1) and not the claims and outstanding-premium columns.
  - Columns can be resized and reordered, and the layout is kept per user. Each column has a filter (text, list, date or amount). A keyword search across all columns filters and highlights the matches, like Ctrl+F.
preconditions:
  - The list is generated (FR-RN-011).
main_flow:
  - The user scrolls the list; BIBS loads further rows as the user scrolls.
  - The user types in a column filter or in the search box; the list filters and the matches are highlighted.
  - The user resizes or drags a column; the layout is saved for the next visit.
alternate_flows:
  - More than 5,000 rows. Filters and search run on the server; the grid keeps the same behaviour.
rules:
  - [R1, "No pagination: one scrollable view with a row count (BRRN.004; UX-6 / RQ05 to confirm against the UX pager rule).", Fixed, "-"]
  - [R2, "The EBIX invoice number and BDOIsys reference columns are out of scope; the BIBS expiring invoice number and the renewal reference take their place.", Fixed, "-"]
  - [R3, "Claims columns read the Claims module; until it is connected they show 'claims not connected' (decision D4).", Fixed, "-"]
validations: []
fields_screen: Expiry List grid (main columns)
fields:
  - [Expiring Invoice No., Display, "-", Booked invoice, BIBS invoice number]
  - [Renewal Reference No., Display, "-", Candidate, RNW-yyyy-nnnnnn]
  - [Cover No. / Version No., Display, "-", Invoice, "-"]
  - [Invoicing Branch / Department Code / Department Name / Unit Head, Display, "-", Sales organisation, "-"]
  - [Client Name / Assured's Name / Business Origin / Account Officer, Display, "-", Account and client, "-"]
  - [Expiring Policy No. / Insurance Company / Risk Code / Risk Name / Risk Description, Display, "-", Account and catalogue, "-"]
  - [Renewal Stage / Status / Renewal Disposition / Disposition Remarks, Display, "-", Candidate, "-"]
  - [Number of Claims / Status of Each Claim, Display, "-", Claims module, Marketing list only]
  - [Amount of Outstanding Premium, Amount, "-", Operations ledger, Marketing list only]
  - [Classification, Pill, "-", Bucket, "Clean (green), Review (yellow), Exception (red)"]
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - A list of 20,000 candidates scrolls from the first to the last row without a pager, and the row count shows 20,000.
  - Typing a client name in the search box filters the list and highlights the name in each matching row.
  - A column moved to the first position stays first after the user logs out and in.
```

```fr
id: FR-RN-014
title: Export and print the expiring list
brd:
  - BRRN.007 (p.25-26), restating BRD 1.003.5, 1.003.6, 1.003.7, 3.003.5, 3.003.6, 3.003.7 (p.50-51, 115)
  - BRRN.008 (p.26), restating BRD 1.003.7.1, 1.003.7.2, 3.003.7.1, 3.003.7.2 (p.51, 115)
actor: Marketing TL; Processing TL
priority: Must have
fit: CHANGE
screens: Expiry List (Export, Print)
api: GET /api/v1/reports/RNW-EXPIRY-LIST/export?format=XLSX|PDF
description:
  - The user downloads the list as .xlsx with the columns and filters of the current view, saves it, and prints it. The export is the report RNW-EXPIRY-LIST run with the grid's filter state, so the file equals the displayed list.
  - Print opens a PDF preview (landscape, A4 or Legal) that matches the printout; the browser print dialog sets margins and scaling.
preconditions:
  - The user has RNW_EXPORT.
main_flow:
  - The user clicks **Export** and chooses Excel, or **Print**.
  - BIBS produces the file from the current view and downloads it, or opens the print preview.
alternate_flows:
  - The user ticks "Keep a copy"; BIBS also stores the file in the report archive.
rules:
  - [R1, "The export has the same rows and columns as the view; nothing is truncated.", Fixed, "-"]
  - [R2, "Page size A4 or Legal; orientation landscape by default.", Configurable, Report parameters pageSize / orientation]
validations: []
notifications:
  - "None."
audit:
  - "Each export is logged with user, filters and time."
acceptance:
  - The Excel file of a filtered list has exactly the rows and columns shown on screen.
  - The print preview shows the same rows as the printed pages.
```

```fr
id: FR-RN-015
title: Initiate renewal processing explicitly
brd:
  - BRRN.021 (p.4)
actor: Authorised user (Marketing TL, Processing TL with RNW_EXTRACT)
priority: Must have
fit: NEW
screens: Expiry List (Initiate); record page
api: POST /api/v1/renewal/candidates/initiate
description:
  - Extracted accounts are displayed, but renewal processing starts only when an authorised user initiates them. Segments flagged for bulk initiation (for example CLG) are initiated in bulk from the list; the others (for example Non-Mortgage) one by one.
  - Initiation records the user and time and moves the candidate to EVALUATING, where BIBS applies the bucket and the decision matrix (FR-RN-022, FR-RN-023). Candidates handed over by Submitted Policies are initiated by the hand-off itself (FR-RN-090).
preconditions:
  - The candidate is EXTRACTED; the user has RNW_EXTRACT.
main_flow:
  - The user selects candidates (single, multiple, all except, all) and clicks **Initiate**.
  - BIBS records the initiation and routes each candidate by its bucket and the matrix.
alternate_flows:
  - A bulk selection includes a segment that is initiated one by one. BIBS initiates the others and lists the refused ones.
rules:
  - [R1, "No extracted candidate progresses without initiation.", Fixed, "-"]
  - [R2, "Segments initiated in bulk: CLG (default).", Configurable, Parameter RNW_BULK_INITIATION_SEGMENTS (RQ02)]
validations:
  - [Candidate not EXTRACTED, "Renewal <ref> is already initiated", To be assigned at build]
  - [Bulk initiation of a single-initiation segment, "Segment <segment> is initiated one account at a time", To be assigned at build]
notifications:
  - The assigned TL queue when candidates reach UNASSIGNED.
audit:
  - "INITIATED event with user and time, separate from EXTRACTED."
acceptance:
  - An extracted candidate stays EXTRACTED and appears in no worklist until it is initiated.
  - Bulk initiation of 200 CLG candidates records the user and time on each.
  - A Non-Mortgage candidate in a bulk selection is refused with the segment message.
```

## Sanitation, buckets and rules

```fr
id: FR-RN-020
title: Run the sanitation, matching and eligibility checks
brd:
  - BRRN.020 (p.4)
  - BRRN.022 (p.4)
actor: System (check engine); Renewal processor (view)
priority: Must have
fit: NEW
screens: Record page (Checks & Bucket tab); Renewal Reports (RNW-SANITATION)
api: GET /api/v1/renewal/candidates/{ref}/checks
description:
  - After extraction, after every upload and on every relevant event, BIBS runs the checks of section 5.3 on the candidate - reference match, PN present, risk code renewable, LAMD status, claims, pending endorsement, outstanding premium, financial impact, insurer response match, mandatory fields, product renewable, insurer usable, duplicate candidate and KYC due. Eligibility no longer depends on manual pre-cleaning of the list.
  - Each check writes a result PASS, WARN, FAIL, INFO or NOT_APPLICABLE with its detail. The Checks & Bucket tab shows the latest results, and the RNW-SANITATION report lists them for any range.
preconditions:
  - "A candidate exists."
main_flow:
  - A trigger occurs (extraction, upload, insurer or LAMD file, endorsement posted, payment, nightly job).
  - BIBS runs every active check and stores the run and its results.
  - BIBS recomputes the bucket (FR-RN-022).
alternate_flows:
  - The Claims module is not connected. The claims check reports INFO "claims not connected" and does not block (decision D4).
rules:
  - [R1, "Checks and their default severities are those of section 5.3; the list is to be confirmed (RQ01).", Configurable, Check settings (Renewal Setup)]
  - [R2, "Re-evaluation on InvoiceBooked, InvoiceMovementPosted, AccountStatusChanged, every upload and nightly (job RNW_REEVALUATE).", Fixed, "-"]
  - [R3, "KYC due is information only and never blocks (BRRN.028).", Fixed, "-"]
validations: []
notifications:
  - "None; the bucket change of FR-RN-022 notifies."
audit:
  - "Every check run with trigger, time and the results per check."
acceptance:
  - A candidate with an open endorsement request on the expiring invoice shows ENDORSEMENT_PENDING = FAIL on the Checks & Bucket tab.
  - When the endorsement is posted, the check turns PASS at the next event without user action.
  - The RNW-SANITATION report lists the failed checks of a date range with the candidate and detail.
```

```fr
id: FR-RN-021
title: Use a unique renewal reference as the matching key
brd:
  - BRRN.022 (p.4)
  - BRD 1.003.4.1.1.2 (p.47)
actor: System; Renewal user
priority: Must have
fit: NEW
screens: Expiry List; record page; every upload
api: Number series RNW-yyyy; check REFERENCE_MATCH
description:
  - BIBS issues a renewal reference RNW-yyyy-nnnnnn at extraction and stores it with the expiring ARN and invoice number. The reference is mandatory on every upload (dispositioned file, insurer response, acceptance list) and is the primary key that links a renewal record to its expiring policy; the expiring invoice number is the alternate key.
  - The check REFERENCE_MATCH records the outcome per record (MATCHED, MISSING, INVALID, AMBIGUOUS). A record without a valid reference goes to exception handling and is never dropped.
preconditions:
  - "None."
main_flow:
  - BIBS generates the reference when the candidate is created.
  - An upload row carries the reference; BIBS matches it to the candidate.
  - The match outcome is visible on the record and in the upload result.
alternate_flows:
  - Missing or unknown reference. The row is kept as an exception line with the reason.
rules:
  - [R1, "Reference format RNW-<yyyy>-<nnnnnn>, unique per company.", Configurable, Parameter RNW_REFERENCE_PREFIX (format RQ03)]
  - [R2, "Uploads without a valid reference never update a candidate.", Fixed, "-"]
validations:
  - [Upload row without reference, Row <n> has no renewal reference, To be assigned at build]
  - [Reference not found, "Row <n>: renewal reference <ref> does not exist", To be assigned at build]
notifications:
  - "None."
audit:
  - "Match result and status per record."
acceptance:
  - Every candidate has a unique renewal reference.
  - A dispositioned-file row with an unknown reference is listed as an exception and updates nothing.
```

```fr
id: FR-RN-022
title: Classify candidates into Clean, Review and Exception buckets
brd:
  - BRRN.023 (p.4-6)
actor: System; authorised user (override)
priority: Must have
fit: NEW
screens: Expiry List (Classification pill, Exceptions tab, Bucket filter); record page (Checks & Bucket tab, Override Bucket)
api: POST /api/v1/renewal/candidates/{ref}/bucket-override
description:
  - After each check run BIBS computes the bucket from the active bucket rule set. By default any check that fails with severity FAIL_EXCEPTION gives Exception; any FAIL_REVIEW or WARN gives Review; otherwise Clean. Records move between buckets when conditions change.
  - Users filter and export by bucket. A bucket is changed by hand only through a controlled override with mandatory remarks, and an override can never make a record with a failed check Clean (BRRN.023 negative scenario).
preconditions:
  - "A check run exists."
main_flow:
  - BIBS evaluates the bucket rules on the latest results and stores the bucket with the rule-set version.
  - The Classification pill shows the bucket; the Checks & Bucket tab shows why.
alternate_flows:
  - Override. A user with RNW_OVERRIDE clicks **Override Bucket**, chooses the bucket and writes remarks; BIBS records the override.
rules:
  - [R1, "Default rule set: FAIL_EXCEPTION gives EXCEPTION; FAIL_REVIEW or WARN gives REVIEW; otherwise CLEAN.", Configurable, "Bucket rule sets (versioned, maker-checker; content RQ01)"]
  - [R2, "A failed check never gives CLEAN, by rule or by override.", Fixed, "-"]
  - [R3, "Every bucket change is logged with the rule-set version, rule, check run, cause and user.", Fixed, "-"]
validations:
  - [Override to Clean with a failed check, "Renewal <ref> has failed checks and cannot be Clean", To be assigned at build]
  - [Override without remarks, Enter the remarks of the override, "-"]
fields_screen: Override Bucket
fields:
  - [Bucket, List, "Yes", "Review, Exception (Clean only when no check fails)", "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "The owners of the stage when a candidate enters Exception."
audit:
  - "Bucket history: from, to, rule-set version, rule, check run, cause (RULE or OVERRIDE), user, remarks."
acceptance:
  - A candidate with an open claim is Review, never Clean.
  - When the claim closes, the candidate becomes Clean at the next re-evaluation and the bucket history shows both entries.
  - An override to Clean on a record with a failed check is refused.
```

```fr
id: FR-RN-023
title: Apply the decision matrix and rule-based disposition
brd:
  - BRRN.031 (p.9-10)
  - BRRN.034 (p.11)
actor: System; Business Administrator (matrix); authorised user (override)
priority: Must have
fit: NEW
screens: Renewal Setup (Decision Matrix); record page (disposition proposal); Renewal Reports (RNW-DECISIONS)
api: /api/v1/renewal/setup/decision-matrices; POST /api/v1/renewal/candidates/{ref}/disposition-override
description:
  - After initiation BIBS evaluates the active decision matrix on Clean and Review candidates. The first rule by priority gives a proposed disposition and an automation flag. A Clean candidate whose rule says AUTO receives a system disposition (source MATRIX) and proceeds without user action; otherwise the proposal is shown to the AO as the default.
  - Matrix rules refer to segment, line, product, mortgaged flag, bucket, claims, endorsements, payment status and days to expiry; outcomes are disposition options (BRRN.034 AC1-2). Candidates with claims, endorsements, a missing or invalid PN or unresolved payment go to manual review (BRRN.031 AC2).
  - The matrix is versioned; a version is activated by a checker other than its maker. Every system decision stores the matrix version and the rule id.
preconditions:
  - "An active matrix exists; without one, every candidate is manual."
main_flow:
  - The Business Administrator drafts a matrix version with its rules and submits it.
  - A checker with MASTER_AUTHORIZE activates it from its effective date.
  - On initiation BIBS evaluates the matrix and applies or proposes the disposition.
alternate_flows:
  - Override of a system disposition. An authorised user chooses another disposition with mandatory remarks; the override is logged.
  - "TL review of automatic dispositions. With parameter RNW_STP_SKIP_TL_REVIEW = true (default), an AUTO disposition skips the TL review."
rules:
  - [R1, "AUTO applies only to CLEAN candidates; any failed check means manual review.", Fixed, "-"]
  - [R2, "Only one ACTIVE matrix version at a time; activation by a checker who is not the maker.", Fixed, "-"]
  - [R3, "Matrix content is supplied by BDOI (RQ24); none ships with production.", Configurable, Decision matrix (Renewal Setup)]
  - [R4, "The condition TOTAL_LOSS is inactive until Claims defines a total-loss indicator (CLQ28).", Fixed, "-"]
validations:
  - [Activation by the maker, A matrix version is activated by someone other than its maker, To be assigned at build]
  - [Override without remarks, Enter the remarks of the override, "-"]
  - [Rule without outcome, Select the disposition of the rule, "-"]
fields_screen: Decision rule
fields:
  - [Priority, Number, "Yes", "-", Unique in the version]
  - [Segment / Line / Product, List, "No", Masters, Blank = any]
  - [Mortgaged, Option, "No", Yes / No / Any, "-"]
  - [Bucket, List, "No", "Clean, Review", "-"]
  - [Claims condition, List, "No", "None, Open, Paid (Total loss parked)", "-"]
  - [Endorsement condition, List, "No", "None, Posted in term, Pending", "-"]
  - [Payment condition, List, "No", "Paid, Outstanding, DP", "-"]
  - [Days to expiry from / to, Number, "No", "-", "From <= to"]
  - [Outcome disposition, List, "Yes", LOV RNW_DISPOSITION, "-"]
  - [Automation, Option, "Yes", AUTO / MANUAL, "-"]
  - [Letter hint, List, "No", "RA, NFR, NAL", "-"]
notifications:
  - "The checkers when a matrix version is submitted."
audit:
  - "Matrix versions with maker, checker and dates; each decision with version, rule id and user or MATRIX."
acceptance:
  - A Clean CBG Motor candidate matching an AUTO rule receives disposition For Renewal with source MATRIX and moves on without user action.
  - A candidate with an open claim is not disposed automatically and shows the proposal to the AO.
  - The RNW-DECISIONS report shows, for each system disposition, the matrix version and rule.
```

```fr
id: FR-RN-024
title: Tag non-renewable risk codes and send the NFR
brd:
  - BRRN.009 (p.26-27), restating BRD 1.003.7.3, 1.003.7.3.1 (p.51)
actor: System; Business Administrator (codes)
priority: Must have
fit: NEW
screens: Renewal Setup (Non-renewable Risk Codes); Letters (NFR tab)
api: /api/v1/renewal/setup/non-renewable-risk-codes
description:
  - The Business Administrator maintains the risk codes that are not renewable, with line, reason and effective dates. Each change is authorised by a checker.
  - The check RISK_CODE_RENEWABLE tags a candidate on such a code Not for Renewal with reason "Non-renewable Accounts" and source SYSTEM, excludes it from the assignment list, and queues a Not for Renewal Letter (NFR) to the client. The delivery outcome is read from the e-mail log.
preconditions:
  - "The user has RNW_SETUP (codes)."
main_flow:
  - The Business Administrator adds a risk code with reason and effective dates; a checker authorises it.
  - At the next check run, candidates on that code are tagged and moved to LETTER_PENDING.
  - BIBS generates the NFR from template RNW_NFR and sends it protected to the client's registered e-mail.
alternate_flows:
  - The client later decides to renew. The AO re-dispositions the account (FR-RN-047); the NFR stays in the history with the flag "NFR sent".
rules:
  - [R1, "Codes are never deleted; they are end-dated.", Fixed, "-"]
  - [R2, "The NFR is queued on tagging without a human review step; BDOI confirms this under RQ07.", Fixed, "-"]
validations:
  - [Effective to before effective from, The effective-to date is before the effective-from date, LOV_EFFECTIVITY_INVALID]
  - [Risk code not in the catalogue, "Risk code <code> does not exist", To be assigned at build]
fields_screen: Non-renewable risk code
fields:
  - [Risk code, List, "Yes", Product catalogue, Active risk code]
  - [Product line, List, "No", Product lines, "-"]
  - [Reason, Text, "Yes", "-", Up to 200 characters]
  - [Effective from / to, Date, "Yes (from)", "-", To on or after from]
notifications:
  - "NFR to the client; confirmation of delivery on the Letters tab."
audit:
  - "Changes to codes (maker, checker, before and after) and every NFR action, including failures."
acceptance:
  - A candidate on a non-renewable risk code is tagged Not for Renewal, does not appear in Unassigned Disposition, and an NFR is generated.
  - The Letters tab shows the NFR with its sent status.
  - A code change takes effect only after the checker authorises it.
```

```fr
id: FR-RN-025
title: Ingest LAMD reports and route paid-off and RMU accounts
brd:
  - BRRN.029 (p.8)
  - BRRN.039 (p.14)
  - BRRN.024 (p.6)
actor: LAMD user (upload); System (matching)
priority: Must have
fit: NEW
screens: LAMD Reports (upload, match results, routing)
api: Bulk handler RNW_LAMD_REPORT; GET /api/v1/renewal/lamd-reports
description:
  - The LAMD user uploads the paid-off and RMU loan reports. BIBS matches each line to the expiring candidates by PN, the primary matching key, and applies the routing. A paid-off loan tags the account Not for Renewal with reason "Loan fully paid"; an RMU loan tags it Not for Renewal with reason "RMU" or transfers it to the Corporate RMU unit.
  - For CBG Motor and Fire, PN matching is mandatory. A CBG candidate without a PN fails PN_PRESENT, and matched Clean candidates follow the straight-through path to the RA or the NFR without AO assignment (BRRN.039).
preconditions:
  - The user has RNW_LAMD_UPLOAD.
main_flow:
  - The LAMD user chooses the report type (Paid-off or RMU) and the period, and uploads the file.
  - BIBS validates the rows, matches them by PN and shows matched, unmatched and ambiguous lines.
  - BIBS applies the routing to matched candidates and re-runs their checks.
alternate_flows:
  - Unmatched PN. The line is kept as unmatched and listed in RNW-LAMD-MATCH.
  - Paid-off client wants to renew without a mortgage. The AO re-dispositions (FR-RN-047; RQ23).
rules:
  - [R1, "PN is the primary matching key.", Fixed, "-"]
  - [R2, "RMU routing: tag Not for Renewal, or transfer to the unit in RNW_RMU_UNIT when set.", Configurable, Parameter RNW_RMU_UNIT (RQ23)]
  - [R3, "CBG lines on the STP path: Motor and Property.", Configurable, Parameter RNW_CBG_STP_LINES (RQ08)]
  - [R4, "Report layout: column mapping of the upload handler.", Configurable, Bulk template RNW_LAMD_REPORT (RQ20)]
validations:
  - [File type not allowed, The file type is not allowed, BULK_FILE_TYPE]
  - [Row without PN, Row <n> has no PN number, To be assigned at build]
  - [PN matches several candidates, "Row <n>: PN <pn> matches several renewals", To be assigned at build]
fields_screen: Upload LAMD report
fields:
  - [Report type, Option, "Yes", "Paid-off, RMU", "-"]
  - [Period, Month, "Yes", "-", "-"]
  - [File, Attachment, "Yes", Document type LAMD_REPORT, Platform file rules]
notifications:
  - "The owning TL when an account is tagged or routed by LAMD."
audit:
  - "Upload, each match and routing with time and data source (BRRN.029 AC3)."
acceptance:
  - A paid-off PN matching an expiring candidate tags it Not for Renewal with reason Loan fully paid.
  - A CBG Motor candidate without a PN is Exception.
  - A LAMD user cannot see the RA or disposition actions.
```

```fr
id: FR-RN-026
title: Flag accounts due for KYC review
brd:
  - BRRN.028 (p.7-8)
actor: System; any list user
priority: Must have
fit: CHANGE
screens: Expiry List (flag chip KYC due); record page; Renewal Reports
api: Check KYC_DUE
description:
  - BIBS flags candidates whose client is due or upcoming for KYC review, using the client's KYC review date and the KYC due window. The flag shows as a chip in the lists and a column in the reports, with the date it was first identified. It is for visibility and reporting only and never blocks, stops or reroutes the renewal.
preconditions:
  - "None."
main_flow:
  - The check KYC_DUE runs with the other checks.
  - When the client is due within the window, BIBS sets the flag and records when it was identified.
rules:
  - [R1, "Severity INFO only; the flag is never an input of the bucket rules.", Fixed, "-"]
  - [R2, "Segments in scope: all (default); list to confirm (bank / non-bank, CLG, BBG, Motor).", Configurable, Parameter RNW_KYC_SEGMENTS (RQ22)]
  - [R3, "Due window from the client master.", Configurable, Parameter KYC_DUE_WINDOW_DAYS]
validations: []
notifications:
  - "None."
audit:
  - "The date and time the flag was set and cleared."
acceptance:
  - A candidate whose client KYC review is due in 20 days shows the KYC due chip, and its bucket is unchanged.
  - The renewal status report shows the flag and the date it was identified.
```

```fr
id: FR-RN-027
title: Reflect endorsements on the mother policy before approval
brd:
  - BRRN.032 (p.10)
  - BRD 1.008.1.4 (p.56)
actor: System
priority: Must have
fit: CHANGE
screens: Record page (Checks & Bucket, Account History); TL Review
api: Check ENDORSEMENT_PENDING
description:
  - Endorsements booked before or during the renewal update the expiring policy (the mother policy). The renewal account is built from the policy as of the last posted endorsement, so the renewal computation reflects the latest policy state.
  - While an endorsement request on the expiring invoice family is neither posted nor cancelled, the check ENDORSEMENT_PENDING fails and the renewal cannot be posted, have its RA generated or be accepted. BIBS records the link between each endorsement and the candidate.
preconditions:
  - "None."
main_flow:
  - An endorsement request is opened on the expiring invoice family.
  - The check fails; the candidate moves to Review and shows the Endorsed flag.
  - The endorsement is posted; the check passes at the next re-evaluation and the link is recorded.
rules:
  - [R1, "Post, RA generation and acceptance are refused while a blocking check is open, unless it is overridden with remarks.", Fixed, "-"]
validations:
  - [Post or RA with an open endorsement, "Renewal <ref> has an endorsement in progress (<endorsement no.>)", To be assigned at build]
notifications:
  - "The owner of the stage when the check fails."
audit:
  - "Endorsement-to-renewal link with the endorsement number, source and status at link time."
acceptance:
  - The TL cannot post a candidate while an endorsement request on the expiring invoice is open.
  - After the endorsement is posted, the renewal account shows the endorsed sum insured.
  - The record lists the linked endorsement numbers.
```

## Assignment and transfer (Marketing TL)

```fr
id: FR-RN-030
title: Assign and re-assign accounts to Marketing AOs
brd:
  - BRD 1.005.1, 1.005.1.1, 1.005.1.1.1, 1.005.1.1.2, 1.005.1.2, 1.005.1.2.1-1.005.1.2.4 (p.53-54)
  - BRD 1.005.2 (p.54)
actor: Marketing TL
priority: Must have
fit: CHANGE
screens: Expiry List (tab Unassigned Disposition; Assign Disposition, Re-assign Officer)
api: POST /api/v1/renewal/candidates/assign; POST /api/v1/renewal/candidates/reassign
description:
  - The TL selects accounts of the Unassigned Disposition tab (single, multiple, all except, all), searches the AO by name or ID or picks him from the list of his unit's officers, tags the accounts to the AO and pushes them. The accounts move to the AO's list (stage FOR_DISPOSITION).
  - The TL re-assigns accounts to another AO with **Re-assign Officer** until the RA locks the account. Assignment applies to non-CBG accounts only; CBG accounts follow the LAMD-driven path (BRRN.039).
preconditions:
  - The candidates are UNASSIGNED or FOR_DISPOSITION; the user has RNW_ASSIGN.
main_flow:
  - The TL ticks the accounts and clicks **Assign Disposition**.
  - The TL searches and selects the AO.
  - The TL clicks **Assign and Push**. BIBS records the assignment and notifies the AO.
alternate_flows:
  - Unselect. The TL clears the selection before pushing; nothing changes.
  - Re-assign. The TL ticks assigned accounts, clicks **Re-assign Officer**, selects the new AO and, optionally, a reason.
rules:
  - [R1, "The AO list is limited to the officers of the TL's sales unit.", Fixed, "-"]
  - [R2, "CBG accounts are not assigned to AOs (definition of CBG to confirm, RQ08).", Fixed, "-"]
  - [R3, "Re-assignment is allowed until the RA is generated (Marketing lock).", Fixed, "-"]
validations:
  - [No account selected, Select at least one account, "-"]
  - [AO outside the unit, "<AO> is not an officer of your unit", To be assigned at build]
  - [Account locked by the RA, "Renewal <ref> is locked since the Renewal Advice was generated", To be assigned at build]
fields_screen: Assign Disposition
fields:
  - [Account Officer, Look-up (name or ID), "Yes", Sales officers of the TL's unit, Active officer]
  - [Reason (re-assign only), List, "No", LOV RNW_TRANSFER_REASON, "-"]
notifications:
  - "RNW_ASSIGNED to the AO (in-app; e-mail per preference)."
audit:
  - "Assignment and re-assignment with from, to, user and time."
acceptance:
  - Accounts assigned to an AO leave the Unassigned Disposition tab and appear in the AO's My Dispositions.
  - A TL cannot assign an account to an officer of another unit.
  - A CBG account never appears in Unassigned Disposition.
```

```fr
id: FR-RN-031
title: Transfer an account to another Marketing unit
brd:
  - BRD 1.006.1-1.006.5 (p.54-55)
  - BRD 2.005.1, 2.005.2, 2.005.3, 2.005.3.1, 2.005.3.2, 2.005.4, 2.005.5 (p.84-85)
  - BRD 2.004.4.8 (p.83)
actor: Marketing TL; Marketing AO (request)
priority: Must have
fit: NEW
screens: Expiry List and My Dispositions (Transfer); Transfers (Outgoing)
api: POST /api/v1/renewal/transfers
description:
  - The TL or the AO selects an account, views it, chooses the receiving Marketing unit, writes remarks and submits the transfer request. The account moves to TRANSFER_PENDING and leaves the sender's list; the receiving unit's TLs are notified.
  - The non-renewal reason "Transfer to Another Marketing Unit" (2.004.4.8) opens the same transfer.
preconditions:
  - The account is UNASSIGNED or FOR_DISPOSITION and not locked by an RA.
main_flow:
  - The user selects the account and clicks **Transfer**.
  - The user selects the receiving unit and writes the remarks.
  - The user clicks **Submit and Push**.
alternate_flows:
  - The sender cancels an open request before it is decided; the account returns to its stage.
rules:
  - [R1, "One open transfer request per account.", Fixed, "-"]
  - [R2, "The receiving unit must be another active Marketing unit.", Fixed, "-"]
validations:
  - [Unit not selected, Select the receiving unit, "-"]
  - [Remarks blank, Enter the remarks, "-"]
  - [Same unit, The receiving unit must be another unit, To be assigned at build]
  - [Transfer already open, "Renewal <ref> already has a transfer request", To be assigned at build]
fields_screen: Transfer account
fields:
  - [Receiving unit, List, "Yes", Sales units, Active Marketing unit other than the account's]
  - [Reason, List, "No", LOV RNW_TRANSFER_REASON, "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "RNW_TRANSFER_REQUESTED to the TLs of the receiving unit."
audit:
  - "Request with from unit, to unit, remarks, user and time."
acceptance:
  - A submitted transfer removes the account from the sender's list and shows it in the receiving TL's Transfers inbox.
  - A transfer to the same unit is refused.
```

```fr
id: FR-RN-032
title: Receive, accept or decline transferred accounts
brd:
  - BRD 1.007.1, 1.007.2, 1.007.3, 1.007.3.1, 1.007.4, 1.007.4.1, 1.007.4.2, 1.007.5 (p.55-56)
actor: Marketing TL (receiving unit)
priority: Must have
fit: NEW
screens: Transfers (Incoming)
api: POST /api/v1/renewal/transfers/{id}/accept | decline
description:
  - The receiving TL is notified of the request and sees the transferred accounts flagged "Transferred". He accepts the account, which is added to his unit's assignment list (UNASSIGNED), or declines it with remarks, which returns it to the sending unit at its previous stage.
preconditions:
  - The request is REQUESTED; the user is a TL of the receiving unit.
main_flow:
  - The TL opens Transfers, tab Incoming, and views the account.
  - The TL clicks **Accept** and pushes; the account joins the unit's Unassigned Disposition tab with the Transferred chip.
alternate_flows:
  - Decline. The TL clicks **Decline**, writes the remarks and pushes; the account returns to the sender with the remarks.
rules:
  - [R1, "Only a TL of the receiving unit decides the request.", Fixed, "-"]
validations:
  - [Decline without remarks, Enter the remarks of the decline, "-"]
  - [Request already decided, "The transfer of <ref> is already decided", To be assigned at build]
fields_screen: Decide transfer
fields:
  - [Decision, Option, "Yes", "Accept, Decline", "-"]
  - [Remarks, Text, Conditional, "-", Required for Decline; up to 200 characters]
notifications:
  - "RNW_TRANSFER_DECIDED to the requester and the sending TL."
audit:
  - "Decision, decided by, remarks and time."
acceptance:
  - An accepted account appears in the receiving unit's Unassigned Disposition tab with the Transferred flag.
  - A declined account returns to the sender with the decline remarks visible.
```

## Disposition (Marketing AO)

```fr
id: FR-RN-040
title: View all my accounts in one scrollable list
brd:
  - BRRN.011 (p.28-29), restating BRD 2.003.1 (p.79)
  - BRRN.012 (p.29), restating BRD 2.003.1.1 (p.79)
  - BRRN.014 (p.30), restating BRD 2.003.1.3 (p.79)
  - BRRN.015 (p.30), restating BRD 2.003.1.4 (p.80)
  - BRRN.016 (p.30-31), restating BRD 2.003.1.5 (p.80)
  - BRRN.017 (p.31), restating BRD 2.003.1.6 without the QPS / EBIX references (p.80)
actor: Marketing AO
priority: Must have
fit: CHANGE
screens: My Dispositions (quick filters Returned to me, Due in 30 days, NRNS)
api: GET /api/v1/renewal/candidates?assignee=me
description:
  - My Dispositions lists every account ever assigned to the AO in one scrollable view, whatever its status (active, expiring, expired, previous years). Filters are optional; the default is all statuses and years.
  - Policy fields are read from the current account and invoice, not copied, so endorsements and updates on the mother policy show at once.
  - The AO selects any row to view or edit it, filters and sorts on every column, searches like Ctrl+F, and searches an account by name or reference (renewal reference, ARN, expiring invoice or policy number, PN, client code).
preconditions:
  - The user has RNW_DISPOSE.
main_flow:
  - The AO opens My Dispositions.
  - The AO filters, sorts or searches.
  - The AO clicks a row; the record page opens (FR-RN-041).
rules:
  - [R1, "Default view: all statuses and years, no pagination.", Fixed, "-"]
  - [R2, "Search fields: renewal reference, ARN, expiring invoice no., expiring policy no., PN, client code and names.", Fixed, "-"]
validations: []
fields_screen: My Dispositions (toolbar)
fields:
  - [Search Renewal Ref / Proposal No. / Name, Text, "No", "-", "-"]
  - [Quick filter, Chips, "No", "Returned to me, Due in 30 days, NRNS", "-"]
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - An AO sees his accounts of the current and previous years without choosing a filter.
  - Sorting by expiry date ascending and descending works on every column.
  - A search by the client's name returns the AO's accounts of that client.
  - An endorsement posted on the mother policy shows in the list without a new extraction.
```

```fr
id: FR-RN-041
title: View a renewal account and download its details
brd:
  - BRRN.013 (p.29-30), restating BRD 2.003.1.2 (p.79)
  - BRD 1.004.1, 1.004.1.1, 1.004.1.2, 1.008.1.2, 1.008.1.3, 3.006.1.1, 3.006.1.2, 4.003.1.1, 4.003.1.2 (p.51-52, 56, 118-119, 149)
  - BRD 1.004.1.3-1.004.1.5.2, 2.003.2, 2.003.3, 2.003.3.1, 2.003.3.2, 3.006.2, 3.006.3, 3.006.3.1, 3.006.3.2, 4.003.2, 4.003.3, 4.003.3.1, 4.003.3.2 (p.52-53, 80, 119-120, 150)
actor: Marketing TL / AO; Processing TL / Officer
priority: Must have
fit: NEW
screens: Record page /renewal/candidates/{ref} (tabs Details, Checks & Bucket, Account History, Computations, Insurer, Letters, Documents, Remarks & Follow-ups, History)
api: GET /api/v1/renewal/candidates/{ref}; GET .../details.pdf | details.xlsx
description:
  - The record page shows the summary card (renewal reference, Classification pill, disposition, flag chips) and every renewal field of the account, read live from the expiring account and invoice, so any update is reflected.
  - The user downloads the account details as PDF or Excel, saves them, and prints them with a preview and print settings.
preconditions:
  - The user has RNW_VIEW and the record is in scope.
main_flow:
  - The user opens a record from a list.
  - The user reads the tabs.
  - The user clicks **Download** (PDF or Excel) or **Print**.
rules:
  - [R1, "The details are generated from template RNW_ACCOUNT_DETAILS.", Configurable, Document templates]
validations: []
notifications:
  - "None."
audit:
  - "Downloads are logged with user and time."
acceptance:
  - The Details tab shows every field of the expiring list for the account.
  - The PDF and Excel downloads contain the same values as the Details tab.
```

```fr
id: FR-RN-042
title: View the full account history before the disposition
brd:
  - BRRN.027 (p.7)
actor: Renewal user (Marketing, Processing)
priority: Must have
fit: CHANGE
screens: Record page (Account History tab)
api: GET /api/v1/renewal/candidates/{ref}/account-history; GET .../account-history.pdf
description:
  - The Account History tab shows, read-only, the prior policies and renewals of the account (the chain of ARNs), the endorsements, the payment history and the claims. It is sourced from the mother policy in BIBS, not from the extraction file. The user extracts or prints it as the role allows.
  - The final disposition is refused until the user has opened the history (BRRN.027 AC4); BIBS records who opened it and when.
preconditions:
  - The user has RNW_VIEW.
main_flow:
  - The user opens the Account History tab.
  - BIBS records the view and shows the sections.
  - The user disposes of the account (FR-RN-043).
alternate_flows:
  - Claims not connected. Until the Claims module is built (wave CL1-B), the claims section shows "claims not connected" (decision D4).
rules:
  - [R1, "The disposition is refused until the history was viewed by the user.", Fixed, "-"]
  - [R2, "History before BIBS (legacy EBIX / QPS) is not shown (RQ13, RQ27).", Fixed, "-"]
validations:
  - [Disposition before the history was opened, Open the Account History before you give the disposition, To be assigned at build]
notifications:
  - "None."
audit:
  - "History view with user, time and sections viewed."
acceptance:
  - The history of a renewed account lists the previous year's ARN, its endorsements, payments and claims.
  - A disposition submitted without opening the history is refused with the message.
  - Once opened, the disposition is accepted.
```

```fr
id: FR-RN-043
title: Give the disposition of an account
brd:
  - BRD 2.004.1, 2.004.2, 2.004.3, 2.004.3.1-2.004.3.5 (p.80-81)
  - BRD 2.004.4, 2.004.4.1-2.004.4.10 (p.82-83)
  - BRD 2.004.5, 2.004.5.1, 2.004.5.2, 2.004.5.3 (p.83-84)
actor: Marketing AO
priority: Must have
fit: NEW
screens: Record page (Disposition panel); My Dispositions
api: POST /api/v1/renewal/candidates/{ref}/disposition; POST .../push
description:
  - The AO selects the account, views its current renewal details and gives the disposition - For Renewal, Not for Renewal, For Quotation, For Proposal or Lost Business. The decision-matrix proposal, if any, is the default.
  - Not for Renewal needs a reason from the list of ten (RMU, With submitted policy, Loan fully paid, Direct to Insurer, Total Loss Claim, Unit Sold, Canceled Policy, Transfer to Another Marketing Unit, Non-renewable Accounts, Booked to New Invoice). "Transfer to Another Marketing Unit" opens the transfer (FR-RN-031); "Booked to New Invoice" asks for the new invoice number.
  - Mandatory fields are marked; BIBS lists every missing one and refuses the push until they are complete. The AO may save and push later. Pushing sends the account to TL review ("Review in progress").
preconditions:
  - The account is FOR_DISPOSITION, assigned to the user, not locked, and its history has been viewed.
main_flow:
  - The AO opens the account and the Disposition panel.
  - The AO selects the disposition and, for Not for Renewal, the reason.
  - The AO completes the mandatory details and the remarks (FR-RN-044) and clicks **Save**.
  - The AO clicks **Push**. BIBS checks completeness and moves the account to FOR_TL_REVIEW.
alternate_flows:
  - Missing fields. BIBS names each missing field and keeps the account in FOR_DISPOSITION.
  - For Quotation or For Proposal. After posting, the account takes the NB path (FR-RN-048).
rules:
  - [R1, "Disposition codes are fixed; their labels are in LOV RNW_DISPOSITION.", Fixed, "-"]
  - [R2, "Non-renewal reasons (ten values).", Configurable, "LOV RNW_NONRENEWAL_REASON (attributes nal_eligible, requires_invoice_no)"]
  - [R3, "Mandatory fields per disposition, plus the product field rules when the renewal account is created.", Configurable, LOV attributes and product field rules]
  - [R4, "Every disposition is a new row; earlier ones are kept (source USER, MATRIX, UPLOAD, INSURER, LAMD or SYSTEM_CHECK).", Fixed, "-"]
  - [R5, "Total Loss Claim stays a manual reason until Claims defines a total-loss indicator (CLQ28).", Fixed, "-"]
validations:
  - [Disposition not selected, Select the disposition, "-"]
  - [Not for Renewal without reason, Select the reason for Not for Renewal, "-"]
  - [Booked to New Invoice without invoice no., Enter the new invoice number, "-"]
  - [New invoice no. not found, "Invoice <no.> does not exist", To be assigned at build]
  - [Mandatory fields missing on push, "Complete the mandatory fields: <fields>", To be assigned at build]
  - [Account locked by the RA, "Renewal <ref> is locked since the Renewal Advice was generated", To be assigned at build]
fields_screen: Disposition panel
fields:
  - [Disposition, List, "Yes", LOV RNW_DISPOSITION, "-"]
  - [Reason for Not for Renewal, List, Conditional, LOV RNW_NONRENEWAL_REASON, Required for Not for Renewal]
  - [New invoice no., Text, Conditional, Operations ledger, Required for Booked to New Invoice; must exist]
  - [Receiving unit, List, Conditional, Sales units, Required for Transfer to Another Marketing Unit]
  - [Remarks, Text, "No", "-", Up to 200 characters]
notifications:
  - "The TL queue when the account is pushed (RNW_POSTED on posting)."
audit:
  - "Each disposition row with code, reason, remarks, source, user and time."
acceptance:
  - An AO disposes an account For Renewal and pushes it; it appears in the TL Review list with status Review in progress.
  - Not for Renewal without a reason cannot be pushed.
  - A push with missing mandatory fields names every missing field.
```

```fr
id: FR-RN-044
title: Add remarks to an account
brd:
  - BRD 2.004.6, 2.004.7, 2.004.8 (p.84)
  - BRD 2.006.1.5, 2.006.1.5.1, 2.006.1.5.2, 2.006.1.6 (p.86-87)
actor: Marketing AO; Marketing TL; Processing
priority: Must have
fit: NEW
screens: Record page (Remarks & Follow-ups tab); Disposition panel
api: POST /api/v1/renewal/candidates/{ref}/remarks
description:
  - The user adds remarks of up to 200 characters. BIBS stamps each remark with the date, time and user ID and the stage, saves it and shows it on the Remarks tab and in the listing column "Remarks of Acct Officer".
preconditions:
  - "The user has RNW_DISPOSE, RNW_REVIEW or RNW_PROCESS."
main_flow:
  - The user types the remarks and clicks **Save**.
  - The user pushes the account when the step is complete.
rules:
  - [R1, "Remarks are at most 200 characters; saved remarks are not edited.", Fixed, "-"]
validations:
  - [Remarks longer than 200 characters, Remarks may have at most 200 characters, "-"]
fields_screen: Remarks
fields:
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "None."
audit:
  - "Remark with user, time and stage."
acceptance:
  - A saved remark shows the user ID, date and time.
  - A 201-character remark cannot be saved.
```

```fr
id: FR-RN-045
title: List my dispositioned accounts with status, flags and lock
brd:
  - BRD 2.006.1, 2.006.1.1, 2.006.1.1.1, 2.006.1.1.2, 2.006.1.3, 2.006.1.4, 2.006.1.5.3 (p.85-87)
actor: Marketing AO
priority: Must have
fit: NEW
screens: My Dispositions (Dispositioned view)
api: GET /api/v1/renewal/candidates?assignee=me&disposed=true
description:
  - The AO lists his dispositioned accounts with their renewal status. Accounts with endorsements and with claims carry the flag chips Endorsed and Claims; the latest endorsement effective on the mother policy is shown. A padlock marks accounts that are closed or locked. The AO selects and views any of them.
preconditions:
  - "The user has RNW_DISPOSE."
main_flow:
  - The AO opens My Dispositions and chooses the Dispositioned view.
  - BIBS lists the accounts with status, flags and padlock.
rules:
  - [R1, "Flags are chips next to the reference, never inside the status pill (UX guidelines).", Fixed, "-"]
  - [R2, "Claims flag from the Claims module; 'claims not connected' until it is built (decision D4).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - An account with a posted endorsement shows the Endorsed chip and the endorsement's effective date.
  - A closed account shows the padlock and cannot be edited.
```

```fr
id: FR-RN-046
title: Update returned accounts
brd:
  - BRD 2.007.1, 2.007.2, 2.007.3, 2.007.4, 2.007.4.2-2.007.4.7, 2.007.5 (p.87-89)
actor: Marketing AO
priority: Must have
fit: CHANGE
screens: My Dispositions (quick filter Returned to me); record page
api: POST /api/v1/renewal/candidates/{ref}/disposition; POST .../push
description:
  - The AO is notified when the TL or Processing returns an account. Returned accounts carry a red Returned flag. The AO reads the return remarks, searches, selects and edits the account, adds remarks, saves or cancels, and pushes it back to the TL.
preconditions:
  - The account is FOR_DISPOSITION with the Returned flag.
main_flow:
  - The AO opens the notification or the quick filter Returned to me.
  - The AO reads the reason and remarks of the return.
  - The AO corrects the disposition or details, adds remarks and pushes. The Returned flag clears.
alternate_flows:
  - Cancel. The AO leaves without saving; the account stays returned.
rules:
  - [R1, "The Returned flag stays until the AO pushes the account again.", Fixed, "-"]
validations:
  - [Push with missing fields, "Complete the mandatory fields: <fields>", To be assigned at build]
notifications:
  - "RNW_RETURNED to the AO when the account is returned."
audit:
  - "Return reason and remarks; the resubmission."
acceptance:
  - A returned account shows the red Returned chip and the TL's remarks.
  - After the AO pushes it, the account is back in the TL Review list without the chip.
```

```fr
id: FR-RN-047
title: Re-open accounts tagged Not for Renewal
brd:
  - BRD 2.004.10 (p.84)
  - BRD 3.004.5 (p.117)
actor: Marketing AO; Processing TL
priority: Must have
fit: NEW
screens: Record page (Re-open Disposition)
api: POST /api/v1/renewal/candidates/{ref}/reopen
description:
  - When a client decides to renew after the account was tagged Not for Renewal (by the AO, by the system or by the upload of 3.004.4), the AO or the Processing TL re-opens it and gives a new disposition. The new disposition row supersedes the old one. If an NFR was already sent, the history keeps it and the record shows the chip "NFR sent".
preconditions:
  - The account is closed NOT_RENEWED or in LETTER_PENDING, and today is not later than the expiry date plus the reopen days.
main_flow:
  - The user clicks **Re-open Disposition** and writes the reason.
  - BIBS moves the account back to FOR_DISPOSITION.
  - The AO disposes it again (FR-RN-043).
rules:
  - [R1, "Re-opening is allowed until expiry plus 30 days (default).", Configurable, Parameter RNW_REOPEN_DAYS]
validations:
  - [Re-open after the limit, "Renewal <ref> can no longer be re-opened (expired on <date>)", To be assigned at build]
  - [Reason blank, Enter the reason for re-opening, "-"]
notifications:
  - "The assigned AO and TL."
audit:
  - "Re-open with user, reason and time; superseded disposition kept."
acceptance:
  - A Not for Renewal account re-opened ten days before expiry returns to FOR_DISPOSITION.
  - The earlier disposition and the sent NFR stay in the history.
```

```fr
id: FR-RN-048
title: Route renewals with financial or structural changes to the New Business path
brd:
  - BRRN.033 (p.10)
  - BRRN.038 (p.14)
actor: Marketing AO; System
priority: Must have
fit: CHANGE
screens: Record page (Start NB Path); Quotation; Proposal (PRF)
api: POST /api/v1/renewal/candidates/{ref}/nb-path
description:
  - A renewal takes the New Business path when the disposition is For Quotation or For Proposal, or when the financial-impact check finds a change in premium, sum insured, rate or charges beyond the tolerance (BRRN.038). The account is tagged "For Proposal / New Business Path" and the audit trail records the reason of the conversion.
  - "**Start NB Path** creates a package quotation or a non-package PRF pre-filled from the expiring account and linked by the renewal reference. The accounts it produces have business type RENEWAL and point to the expiring ARN (shared change BT0, decision D1). The candidate follows the quotation or PRF and closes RENEWED on booking or LOST on decline."
  - When the financial-impact check fails, the acceptance of the client must be recorded with its method (e-mail, signed RA or payment), whichever path the renewal takes (BRRN.038 AC3).
preconditions:
  - The candidate is posted with For Quotation or For Proposal, or FINANCIAL_IMPACT = FAIL.
main_flow:
  - The AO clicks **Start NB Path** and confirms the quotation or PRF type.
  - BIBS creates the quotation or PRF with the renewal reference and opens it.
  - The quotation or PRF follows the BRD-1 flow; its accounts are created as RENEWAL.
alternate_flows:
  - The quotation or PRF is declined or voided. The candidate closes LOST.
rules:
  - [R1, "Financial-impact tolerance 0.00 (any change) by default.", Configurable, Parameter RNW_FIN_IMPACT_TOLERANCE (RQ25)]
  - [R2, "An NB-path renewal rates as New Business on the current package version.", Fixed, "-"]
  - [R3, "One NB path per candidate; no second quotation for the same renewal.", Fixed, "-"]
validations:
  - [NB path already started, "Renewal <ref> already has quotation <no.>", To be assigned at build]
notifications:
  - "The AO when the quotation's accounts are booked or the quotation is declined."
audit:
  - "Conversion to the NB path with reason, user and time; link to the quotation or PRF."
acceptance:
  - A renewal with a 10% premium increase from the insurer is tagged For Proposal / New Business Path with the reason.
  - The quotation created from the candidate carries the renewal reference, and its booked invoice has business type RENEWAL.
  - The candidate closes RENEWED when the quotation's account is booked.
```

## Team Leader review

```fr
id: FR-RN-050
title: Review, return and post dispositioned accounts
brd:
  - BRD 1.008.1, 1.008.1.1, 1.008.1.4 (p.56)
  - BRD 1.008.2, 1.008.2.1-1.008.2.7 (p.57)
  - BRD 1.008.3, 1.008.3.1-1.008.3.4 (p.57-58)
actor: Marketing TL (non-CBG)
priority: Must have
fit: NEW
screens: TL Review (Review in progress list; Return, Post)
api: POST /api/v1/renewal/candidates/return; POST /api/v1/renewal/candidates/post
description:
  - The TL sees every dispositioned account of the unit in one scrollable view with status "Review in progress" until it is posted. Endorsements are reflected on the mother policy (FR-RN-027).
  - The TL returns accounts (single, multiple, all except, all) to the assigned AO with a reason and remarks; BIBS sets status Returned and the red flag. Or the TL posts them after reviewing the details; each posted account moves on by its disposition.
preconditions:
  - The accounts are FOR_TL_REVIEW; the user has RNW_REVIEW.
main_flow:
  - The TL opens TL Review and reviews the accounts.
  - The TL selects accounts and clicks **Post**.
  - BIBS routes each account - For Renewal to Processing, For Quotation and For Proposal to the NB path, Not for Renewal to the letter step, Lost Business to closure.
alternate_flows:
  - Return. The TL selects accounts, clicks **Return**, chooses a reason and writes remarks, and pushes.
  - Blocking check open. BIBS refuses to post the account and names the failing check.
rules:
  - [R1, "An account cannot be posted while a blocking check fails, unless it is overridden with remarks.", Fixed, "-"]
  - [R2, "Return reasons in LOV RNW_RETURN_REASON; the value DISAPPROVED counts as 'Disapproved' in the summary (RQ10).", Configurable, LOV RNW_RETURN_REASON]
validations:
  - [Return without reason, "Select a reason for 'Return'", WORKFLOW_REASON_REQUIRED]
  - [Post with a failing check, "Renewal <ref> cannot be posted: <check> failed", To be assigned at build]
fields_screen: Return accounts
fields:
  - [Reason, List, "Yes", LOV RNW_RETURN_REASON, "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "RNW_RETURNED to the AO; RNW_POSTED to Processing for For Renewal accounts."
audit:
  - "Return and post with user, time, reason and remarks."
acceptance:
  - A posted For Renewal account appears in the Processing Worklist, tab For Processing.
  - A returned account is back in the AO's list with the Returned flag and the TL's remarks.
  - An account with a failing outstanding-premium check cannot be posted until the TL overrides it.
```

```fr
id: FR-RN-051
title: Override the outstanding-balance flag and other controlled overrides
brd:
  - BRD 1.011.1, 1.011.1.1, 1.011.1.2, 1.011.1.3 (p.78)
  - BRRN.023 AC5 (p.5), BRRN.031 AC4 (p.10), BRRN.035 negative scenario 2d (p.12)
actor: Marketing TL (RNW_OVERRIDE)
priority: Must have
fit: NEW
screens: TL Review (Override Outstanding Balance); record page (Override)
api: POST /api/v1/renewal/candidates/override
description:
  - The check OUTSTANDING_PREMIUM flags an account whose expiring invoice family has an open premium balance above the threshold, and routes it to Review. The TL sees the flag, selects one or several flagged accounts and overrides it with mandatory remarks; the block clears for those accounts. This is the renewal override that BRD-1 deferred (OOS-1).
  - The same controlled override clears a bucket, a system disposition or an insurer-response mismatch, and cancels an RA to unlock an account. There is no approver step; every override is explicit, needs remarks and is logged.
preconditions:
  - The account carries the flag or failing check; the user has RNW_OVERRIDE.
main_flow:
  - The TL filters the flagged accounts and selects them.
  - The TL clicks **Override**, chooses the kind, a reason and writes remarks.
  - BIBS records the override and re-evaluates the accounts.
rules:
  - [R1, "Outstanding threshold 0.00 (default), on the expiring invoice family.", Configurable, Parameter RNW_OUTSTANDING_THRESHOLD (scope RQ09)]
  - [R2, "Override kinds: outstanding balance, bucket, disposition, insurer mismatch, RA unlock.", Fixed, "-"]
  - [R3, "An override never turns a failed check into Clean (FR-RN-022).", Fixed, "-"]
validations:
  - [Override without remarks, Enter the remarks of the override, "-"]
  - [Override reason missing, Select the reason of the override, "-"]
fields_screen: Override
fields:
  - [Kind, List, "Yes", "Outstanding balance, Bucket, Disposition, Insurer mismatch, RA unlock", "-"]
  - [Reason, List, "Yes", LOV RNW_OVERRIDE_REASON, "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "None."
audit:
  - "Override log: kind, from, to, reason, remarks, user and time; RNW-DECISIONS report."
acceptance:
  - An account with an unpaid premium shows the Outstanding flag; after the TL's override it can be posted.
  - An override without remarks is refused.
  - The RNW-DECISIONS report lists the override with the TL and the remarks.
```

## Processing

```fr
id: FR-RN-060
title: Upload dispositioned files
brd:
  - BRRN.018 (p.31), restating BRD 3.004.1, 3.004.1.1, 3.004.1.2, 4.004.1, 4.004.1.1, 4.004.1.2 (p.115-116, 150-151)
  - BRD 3.004.2, 3.004.3, 3.004.3.1, 4.004.2, 4.004.3, 4.004.3.1 (p.116, 151)
  - BRD 3.004.4 (p.116)
actor: Processing TL; Processing Officer
priority: Must have
fit: CHANGE
screens: Processing Worklist (Upload Dispositioned File); bulk upload wizard RNW_DISPOSITION_UPLOAD
api: Bulk handler RNW_DISPOSITION_UPLOAD
description:
  - When dispositioning is done outside the system, Processing selects one or more dispositioned files and uploads them. BIBS validates each row (renewal reference mandatory; disposition, reason, remarks and updated fields), updates the valid accounts in bulk, pushes them to the processing list and shows a confirmation with the errors per row.
  - When the user declares the file complete for an expiry range and unit, the candidates of that range and unit missing from the file are tagged Not for Renewal with reason "Non-renewable Accounts" by the system and kept out of processing (3.004.4). The tag can be reviewed and re-opened (FR-RN-047).
preconditions:
  - The user has RNW_UPLOAD.
main_flow:
  - The user clicks **Upload Dispositioned File**, selects the files and, optionally, ticks "Complete file for" with the range and unit.
  - BIBS validates the rows and shows valid and invalid counts.
  - The user confirms; BIBS updates the valid rows (disposition source UPLOAD) and pushes them to FOR_PROCESSING.
alternate_flows:
  - Rows with errors. They are listed with the reason, can be downloaded, and do not update any account (partial commit).
  - Several files. Each file is its own upload job with its own result.
rules:
  - [R1, "The renewal reference is the key of every row (BRRN.022).", Fixed, "-"]
  - [R2, "The 'not in file' tag applies only when the complete-file scope is declared on the upload.", Fixed, "-"]
  - [R3, "Online and uploaded dispositions write the same disposition history with their source (RQ14).", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, BULK_FILE_TYPE]
  - [File without header row, The file has no header row, BULK_FILE_EMPTY]
  - [Row without valid reference, Row <n> has no valid renewal reference, To be assigned at build]
  - [Disposition not in the list, "Row <n>: disposition <value> is not valid", To be assigned at build]
  - [Account locked or closed, "Row <n>: renewal <ref> cannot be updated in stage <stage>", To be assigned at build]
fields_screen: Upload Dispositioned File
fields:
  - [Files, Attachment (several), "Yes", Template RNW_DISPOSITION_UPLOAD, "Excel or CSV; platform file rules"]
  - [Complete file for (range and unit), Date range + List, "No", "Sales units", Required together when ticked]
notifications:
  - "The uploader receives the job result; the Processing TL queue for pushed accounts."
audit:
  - "Upload job with file, scope, user, counts; each updated account and each system tag."
acceptance:
  - A file of 500 rows with 3 unknown references updates 497 accounts and lists the 3 errors.
  - With "Complete file for March, unit X" ticked, a March account of unit X absent from the file is tagged Not for Renewal (Non-renewable Accounts).
  - Without the complete-file tick, no account is tagged.
```

```fr
id: FR-RN-061
title: Assign accounts to Processing Officers
brd:
  - BRD 3.005.1, 3.005.1.1, 3.005.1.1.1, 3.005.1.1.2, 3.005.1.2, 3.005.1.2.1, 3.005.1.2.2, 3.005.1.3, 3.005.1.4, 3.005.1.5, 3.005.1.6 (p.117-118)
actor: Processing TL
priority: Must have
fit: CHANGE
screens: Processing Worklist (Assign PO, Assign to Me)
api: POST /api/v1/renewal/processing/assign
description:
  - The Processing TL selects accounts of the For Processing tab (single, multiple, all except, all), searches the Processing Officer by name or ID or assigns them to himself, and pushes them. He also re-assigns accounts to another PO.
preconditions:
  - The accounts are FOR_PROCESSING or IN_PROCESSING; the user has RNW_PROCESS_ASSIGN.
main_flow:
  - The TL ticks accounts and clicks **Assign PO**, or **Assign to Me**.
  - The TL selects the officer and pushes. The accounts move to IN_PROCESSING with the officer as assignee.
rules:
  - [R1, "Only Processing Officers (and the TL himself) are selectable.", Fixed, "-"]
validations:
  - [No account selected, Select at least one account, "-"]
  - [Officer not selected, Select the Processing Officer, "-"]
fields_screen: Assign PO
fields:
  - [Processing Officer, Look-up (name or ID), "Yes", Users with PROCESSOR, Active user]
notifications:
  - "RNW_ASSIGNED to the officer."
audit:
  - "Assignment with from, to, user and time."
acceptance:
  - Accounts assigned to an officer appear in his In Processing tab.
  - Assign to Me makes the TL the assignee.
```

```fr
id: FR-RN-062
title: Work the processing list
brd:
  - BRD 3.006.1, 3.006.1.3, 3.006.1.4 (p.118-119)
  - BRD 4.003.1, 4.003.1.3, 4.003.1.4 (p.149)
actor: Processing TL; Processing Officer
priority: Must have
fit: CHANGE
screens: Processing Worklist (tabs For Processing, In Processing, With Insurer, Insurer Responded, Returned)
api: GET /api/v1/renewal/candidates?stage=...&assignedPo=
description:
  - The Processing Worklist shows the accounts of the processing stages in one scrollable view with filter and sort on every column, the search of FR-RN-013 and the tabs by stage. An officer sees his own accounts; the TL sees the unit's.
preconditions:
  - "The user has RNW_PROCESS."
main_flow:
  - The user opens the worklist and a tab.
  - The user filters, sorts or searches, and opens an account.
rules:
  - [R1, "Same grid behaviour as the expiring list (FR-RN-013).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - A Processing Officer sees only the accounts assigned to him in In Processing.
  - Sorting and column filters work on every column.
```

```fr
id: FR-RN-063
title: Update data on the renewal account
brd:
  - BRD 3.007.1, 3.007.2, 3.007.3, 3.007.3.1-3.007.3.4 (p.120-121)
  - BRD 4.005.1, 4.005.2, 4.005.3, 4.005.3.1-4.005.3.4 (p.151-152)
actor: Processing TL; Processing Officer
priority: Must have
fit: CHANGE
screens: Record page (Renewal Account); Account (BRD-1 draft edit)
api: PUT /api/v1/accounts/{arn} (draft of business type RENEWAL)
description:
  - When a For Renewal candidate reaches FOR_PROCESSING, BIBS creates the renewal account as a draft BRD-1 account of business type RENEWAL, with the same client, product, items, insurer, PN and mortgage, and contact as the expiring account, the next period (start = old end + 1 day, same term), and a link to the expiring ARN. A renew-as-is keeps the package version of the expiring account.
  - Processing updates the data of the renewal account. Mandatory fields are prompted and the push is refused while any are missing. Editing is disabled once placement is completed.
preconditions:
  - The candidate is IN_PROCESSING; the user has RNW_PROCESS.
main_flow:
  - The officer opens the Renewal Account section of the record.
  - The officer edits the fields and saves.
  - BIBS checks the mandatory fields of the product and the rated premium.
alternate_flows:
  - Missing fields. BIBS lists them; the officer completes them or saves the draft.
  - Account already placed. BIBS refuses the edit.
rules:
  - [R1, "The renewal account is built from the policy as of the last posted endorsement (BRRN.032).", Fixed, "-"]
  - [R2, "Rating with purpose RENEWAL and the expiring account's package version (PQ11); an expired version is RQ30.", Fixed, "-"]
  - [R3, "Edits after placement are refused by the account workflow.", Fixed, "-"]
validations:
  - [Mandatory fields missing, "Complete the mandatory fields of <product>: <fields>", ACCOUNT_INCOMPLETE]
  - [Product not sellable for renewal, "Product <code> is <status> and cannot be sold", PRODUCT_NOT_SELLABLE]
notifications:
  - "None."
audit:
  - "Account changes are audited by the account module with before and after values."
acceptance:
  - The renewal account of a Motor policy has the next period and the same vehicle, insurer and PN as the expiring account.
  - An update that leaves a mandatory field blank cannot be pushed.
  - After placement the renewal account cannot be edited.
```

```fr
id: FR-RN-064
title: Review computations and the financial impact
brd:
  - BRD 3.008.1, 3.008.1.1 (p.121)
  - BRD 4.006.1, 4.006.1.1 (p.152)
  - BRRN.038 (p.14)
actor: Processing TL; Processing Officer; System
priority: Must have
fit: CHANGE
screens: Record page (Computations tab)
api: GET /api/v1/renewal/candidates/{ref}/computations
description:
  - The Computations tab shows every calculated field of the renewal account (premium breakdown by component, taxes, commission, total), rated with purpose RENEWAL, next to the expiring invoice (premium, sum insured, rate, commission and charges) with the difference.
  - The check FINANCIAL_IMPACT compares them. A difference beyond the tolerance fails the check, routes the account to Review and, by the matrix, to the NB path (FR-RN-048); the acceptance of the client must then be recorded with its method.
preconditions:
  - The renewal account exists.
main_flow:
  - The officer opens the Computations tab.
  - BIBS shows the breakdown and the comparison, and the result of the financial-impact check.
rules:
  - [R1, "Financial-impact tolerance 0.00 (default).", Configurable, Parameter RNW_FIN_IMPACT_TOLERANCE (RQ25)]
  - [R2, "The financial-impact check is mandatory; it runs on every change of the renewal account or insurer revision.", Fixed, "-"]
validations: []
notifications:
  - "The owner of the stage when the check fails."
audit:
  - "Check result with the compared values."
acceptance:
  - The Computations tab shows the same premium components as the renewal account's premium breakdown.
  - A renewal premium different from the expiring premium fails FINANCIAL_IMPACT and shows the difference.
```

```fr
id: FR-RN-065
title: Return accounts to Marketing
brd:
  - BRD 3.008.2, 3.008.2.1-3.008.2.7 (p.121-122)
  - BRD 4.006.2, 4.006.2.1-4.006.2.7 (p.152-153)
actor: Processing TL; Processing Officer
priority: Must have
fit: CHANGE
screens: Processing Worklist; record page (Return to Marketing)
api: POST /api/v1/renewal/candidates/return-to-marketing
description:
  - Processing returns accounts (single, multiple, all except, all) to the assigned Marketing AO (stage FOR_DISPOSITION) or to the Marketing TL (FOR_TL_REVIEW) with a reason and remarks. BIBS flags them Returned and pushes them.
preconditions:
  - The accounts are FOR_PROCESSING or IN_PROCESSING; the user has RNW_PROCESS.
main_flow:
  - The user selects accounts and clicks **Return to Marketing**.
  - The user chooses AO or TL, the reason and the remarks, and pushes.
rules:
  - [R1, "The BRD title says 'Marketing TL' and the result 'Marketing AO or Marketing TL' (3.008.2.5); BIBS offers both.", Fixed, "-"]
validations:
  - [Return without reason, "Select a reason for 'Return to Marketing'", WORKFLOW_REASON_REQUIRED]
  - [Remarks blank, Enter the remarks, "-"]
fields_screen: Return to Marketing
fields:
  - [Return to, Option, "Yes", "Account Officer, Team Leader", "-"]
  - [Reason, List, "Yes", LOV RNW_RETURN_REASON, "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
notifications:
  - "RNW_RETURNED to the AO or TL."
audit:
  - "Return with user, time, reason and remarks."
acceptance:
  - An account returned to the AO appears in his Returned to me filter with the red flag.
  - A return without a reason is refused.
```

## Insurer round-trip

```fr
id: FR-RN-070
title: Extract For Renewal accounts per insurer and send them
brd:
  - BRD 3.009.1, 3.009.1.1-3.009.1.4, 3.009.1.4.1-3.009.1.4.28 (p.122-126)
  - BRD 4.007.1, 4.007.1.1-4.007.1.4, 4.007.1.4.1-4.007.1.4.28 (p.153-158)
  - BRD 3.009.2-3.009.5.1, 4.007.2-4.007.5.1 (p.126-127, 158-159)
actor: Processing TL; Processing Officer
priority: Must have
fit: NEW
screens: Insurer Batches (build, preview, download, send)
api: POST /api/v1/renewal/insurer-batches; GET .../{no}.xlsx; POST .../{no}/send
description:
  - Processing selects the expiry range and the insurer from a searchable list. BIBS builds a batch (RIB-yyyy) of the For Renewal accounts of that insurer with the 28 columns of BRD 3.009.1.4 (section 6.4) and shows them in one scrollable view.
  - The user downloads the extract as .xlsx, saves and prints it with preview and settings, and sends it to the insurer's renewal (or placement) mailbox, encrypted and password-protected; the password goes in a separate e-mail.
preconditions:
  - The accounts are IN_PROCESSING and For Renewal; the user has RNW_INSURER.
main_flow:
  - The user clicks **New Batch**, selects the range and the insurer.
  - BIBS lists the accounts and the 28 columns.
  - The user clicks **Download** or **Send**. On send BIBS protects the file, e-mails it, records the send and moves the accounts to WITH_INSURER.
alternate_flows:
  - Delivery failure. The batch shows the failed send; the user resends it.
rules:
  - [R1, "One batch per insurer and range; an account is in one open batch at a time.", Fixed, "-"]
  - [R2, "Files leave BIBS protected; the password is sent separately (convention Q07).", Fixed, "-"]
  - [R3, "'Encoder Name' is the user who booked the expiring invoice.", Fixed, "-"]
  - [R4, "Reply due date = send date + reply days; alert RNW_INSURER_OVERDUE after it.", Configurable, Insurer batch reply days (RQ15)]
validations:
  - [Insurer not selected, Select the insurer, "-"]
  - [No account in the batch, "No For Renewal account of <insurer> expires in the range", To be assigned at build]
  - [Insurer without renewal or placement e-mail, "<insurer> has no e-mail address for renewals", To be assigned at build]
fields_screen: New insurer batch
fields:
  - [Expiry from / to, Date, "Yes", "-", To on or after from]
  - [Insurer, Searchable list, "Yes", Insurer master, Active insurer]
notifications:
  - "E-mail with the protected file to the insurer, and the password e-mail."
audit:
  - "Batch creation, download, send (recipient, time, file hash) and failures."
acceptance:
  - A batch for insurer X and March lists every For Renewal account of X expiring in March with the 28 columns.
  - The insurer receives a password-protected Excel file and, separately, the password.
  - The accounts of a sent batch move to With Insurer.
```

```fr
id: FR-RN-071
title: Receive and apply insurer responses
brd:
  - BRD 3.009.6, 3.009.6.1, 3.009.6.2 (p.127)
  - BRD 4.007.6, 4.007.6.1, 4.007.6.2 (p.159)
  - BRRN.035 (p.11-12)
actor: Processing TL; Processing Officer; System
priority: Must have
fit: NEW
screens: Insurer Batches (Upload Response, Match Review); record page (Insurer tab)
api: Bulk handler RNW_INSURER_RESPONSE
description:
  - Processing uploads the insurer's dispositioned file and views it. Each row carries the renewal reference and the expiring policy number, the response (Renew As Is / Approve, Revise, Reject), the insurer reference and, for Revise, the revised premium, sum insured, rate or terms. The response is captured once and is the single source of truth of the renewal's progress.
  - A matched Renew As Is advances the account to RA_READY without re-keying and without changing terms, price or structure. Revise returns it to processing and runs the financial-impact check. Reject tags it Not for Renewal (reason insurer declined) or sends it back to Marketing to re-market.
  - Late, conflicting or revised responses block straight-through progress, RA and booking, and route the account to Review or Exception on the latest valid response. A mismatch (reference, policy number, ambiguous row) never progresses the account; only an explicit override with remarks does.
preconditions:
  - The accounts are WITH_INSURER; the user has RNW_INSURER.
main_flow:
  - The user clicks **Upload Response** on the batch and selects the file.
  - BIBS validates and matches each row and shows matched, mismatched and late rows.
  - The user confirms. BIBS stores each response and applies it to the account.
alternate_flows:
  - Manual response. For a single account the user records the response on the Insurer tab.
  - Mismatch. The row is kept as an exception; the account stays WITH_INSURER in Exception until the file is corrected or an override is recorded (FR-RN-051).
rules:
  - [R1, "Responses are append-only; the latest valid response drives the account.", Fixed, "-"]
  - [R2, "Response codes: Renew As Is, Revise, Reject (layout and codes RQ15).", Configurable, LOV RNW_INSURER_RESPONSE; bulk template]
  - [R3, "A response received after the reply due date is late and blocks STP.", Fixed, "-"]
validations:
  - [Reference or policy number do not match, "Row <n>: <ref> does not match policy <no.>", To be assigned at build]
  - [Response code unknown, "Row <n>: response <value> is not valid", To be assigned at build]
  - [Revise without revised values, "Row <n>: enter the revised premium, sum insured or rate", To be assigned at build]
  - [Conflicting responses in one file, "Row <n>: <ref> has conflicting responses", To be assigned at build]
fields_screen: Insurer response (manual)
fields:
  - [Response, List, "Yes", LOV RNW_INSURER_RESPONSE, "-"]
  - [Insurer reference, Text, "No", "-", "-"]
  - [Revised premium / sum insured / rate, Amount / Number, Conditional, "-", "Required for Revise; >= 0"]
  - [Received on, Date, "Yes", "-", Not in the future]
  - [Remarks, Text, "No", "-", Up to 200 characters]
notifications:
  - "RNW_INSURER_RESPONDED to the assigned officer; alert RNW_INSURER_OVERDUE past the reply date."
audit:
  - "Each response with timestamp, response type, source (upload or manual), match outcome and the resulting status."
acceptance:
  - A matched Renew As Is moves the account to RA Ready with unchanged premium and terms.
  - A row whose policy number differs from the candidate's is not applied and the account shows Exception.
  - A Revise with a higher premium returns the account to In Processing and fails FINANCIAL_IMPACT.
```

## Letters, acceptance and follow-up

```fr
id: FR-RN-080
title: Generate the Renewal Advice
brd:
  - BRRN.010 (p.27-28), restating BRD 1.010.1-1.010.7, 2.009.1-2.009.7.1, 3.011.1-3.011.7.1, 4.09.1-4.09.7 (p.76-78, 106-108, 146-148, 177-179)
  - BRD 2.004.9 (p.84)
actor: Marketing TL / AO; Processing TL / Officer (RNW_RA_GENERATE)
priority: Must have
fit: NEW
screens: Letters (tabs RA Ready, RA Generated, RA Sent; Generate, Preview, Download ZIP)
api: POST /api/v1/renewal/letters/ra (job RNW_LETTER_BATCH); GET /api/v1/renewal/letters/{no}.pdf
description:
  - Authorised users select RA Ready accounts (single, multiple, all except, all) and generate the RA as **First Notice** or **Second Notice** from the latest approved template. The RA contains the policy details, the renewal terms, the premium and the BDOI contacts. Generation runs as a job with progress.
  - Generation locks the account on the Marketing side, so disposition and editing are disabled (2.004.9). Only an RA cancellation by a user with RNW_OVERRIDE unlocks it.
  - The user views, previews and closes each RA, and downloads it as PDF, one by one or as a batch (ZIP). Every RA is stored as document type RENEWAL_ADVICE linked to the renewal account, the client and the candidate (decision D3).
preconditions:
  - The accounts are RA_READY with no failing blocking check; the user has RNW_RA_GENERATE.
main_flow:
  - The user opens Letters, tab RA Ready, and selects accounts.
  - The user clicks **Generate RA** and chooses First or Second Notice.
  - BIBS generates the RAs (RA-yyyy numbers), stores them and locks the accounts.
  - The user previews or downloads them.
alternate_flows:
  - Less than 30 days to expiry. BIBS warns that the RA is generated late; the user confirms to continue, and the confirmation is recorded.
  - Second notice. Offered when a first notice was sent at least the second-notice days earlier without acceptance.
rules:
  - [R1, "Templates RNW_RA_FIRST and RNW_RA_SECOND, latest approved version; drafts until BDOI provides the layouts (RQ26).", Configurable, Document templates]
  - [R2, "Warning when the RA is generated less than 30 days before expiry.", Configurable, Parameter RNW_RA_MIN_NOTICE_DAYS]
  - [R3, "Second notice after 15 days without acceptance (default).", Configurable, Parameter RNW_RA_SECOND_NOTICE_DAYS]
  - [R4, "Only authorised roles generate RAs.", Fixed, "-"]
validations:
  - [User without the permission, You are not permitted to perform this action, ACCESS_DENIED]
  - [Account locked or already with an RA of that notice, "Renewal <ref> already has a <notice> Renewal Advice", To be assigned at build]
  - [Blocking check open, "Renewal <ref> cannot get a Renewal Advice: <check> failed", To be assigned at build]
  - [Late RA not confirmed, "The Renewal Advice is less than <n> days before expiry; confirm to continue", "-"]
fields_screen: Generate RA
fields:
  - [Notice, Option, "Yes", "First Notice, Second Notice", "-"]
  - [Confirm late notice, Check box, Conditional, "-", Required when less than 30 days to expiry]
notifications:
  - "None until sending."
audit:
  - "Generation with user, time, accounts, template code and version; failures logged (BRRN.010 AC)."
acceptance:
  - A Marketing AO generates First Notice RAs for 50 accounts; each has an RA number and a PDF on the Letters tab.
  - After generation the AO cannot change the disposition of those accounts.
  - An RA generated 20 days before expiry requires the user's confirmation.
  - A user without RNW_RA_GENERATE cannot generate an RA.
```

```fr
id: FR-RN-081
title: Send Renewal Advices in batch, encrypted and password-protected
brd:
  - BRRN.010 (p.27-28), restating BRD 1.010.6, 1.010.7, 2.009.6, 2.009.7, 2.009.7.1, 3.011.6, 3.011.7, 3.011.7.1, 4.09.6, 4.09.7 (p.77-78, 107-108, 147-148, 178-179)
actor: Marketing TL / AO; Processing TL / Officer (RNW_RA_SEND)
priority: Must have
fit: NEW
screens: Letters (tab RA Generated; Send in Batch, Extract Details)
api: POST /api/v1/renewal/letters/send (job RNW_LETTER_BATCH)
description:
  - The user extracts the details needed for sending (report RNW-RA-DISPATCH) and sends the generated RAs in batch. Each client receives one e-mail with the RA as an encrypted, password-protected PDF, and the password in a separate e-mail. The account status becomes RA Sent / Awaiting Response.
  - Every action, including failures, is logged with the user, the time and the accounts.
preconditions:
  - The RAs are GENERATED; the user has RNW_RA_SEND.
main_flow:
  - The user selects RAs and clicks **Send in Batch**.
  - BIBS checks each recipient, queues one protected e-mail per client and the password e-mail.
  - BIBS shows the send status per RA (Queued, Sent, Failed).
alternate_flows:
  - Recipient refused by the recipient policy. The RA stays GENERATED with the reason.
  - Delivery failure. The RA is FAILED; alert RNW_LETTER_FAILED; the user resends it.
rules:
  - [R1, "RAs are sent only protected; protection cannot be switched off.", Fixed, "-"]
  - [R2, "Recipient policy (approved domains, TLS) through a port with a permissive default until BDOI defines it (RQ16).", Configurable, RecipientPolicy (RQ16)]
  - [R3, "Password convention per Q07; until then a generated password per e-mail.", Configurable, Messaging settings (Q07)]
validations:
  - [Client without e-mail, "Client <name> has no registered e-mail address", To be assigned at build]
  - [Recipient refused, "<address> is not an approved recipient domain", To be assigned at build]
notifications:
  - "Protected RA and separate password e-mail to the client."
audit:
  - "Each send with recipient, time, subject, attachment hash and status; failures."
acceptance:
  - Sending 50 RAs produces 50 protected e-mails and 50 password e-mails, and the accounts show RA Sent / Awaiting Response.
  - A failed send is listed with the error and can be resent.
```

```fr
id: FR-RN-082
title: Generate and send the No Advice Letter
brd:
  - BRRN.001 (p.22)
actor: Marketing / Processing user (RNW_RA_GENERATE, RNW_RA_SEND)
priority: Must have
fit: NEW
screens: Letters (NAL tab)
api: POST /api/v1/renewal/letters/nal
description:
  - For expiring policies that need neither an RA nor an NFR, the user selects one or several eligible accounts and generates the No Advice Letter (NAL) from its template. BIBS sends it to the client's registered e-mail, stores it, updates the account status to NAL Sent and shows a confirmation.
preconditions:
  - The accounts are eligible - no RA or NFR on the candidate and a disposition or reason marked NAL-eligible.
main_flow:
  - The user opens the NAL tab, which lists eligible accounts.
  - The user selects accounts and clicks **Generate and Send NAL**.
  - BIBS generates, stores and sends the letters and confirms.
rules:
  - [R1, "Eligibility from the reason attribute nal_eligible; definition to confirm (RQ17).", Configurable, LOV RNW_NONRENEWAL_REASON attribute]
  - [R2, "No NAL for an account that already has an RA or NFR.", Fixed, "-"]
validations:
  - [Ineligible account selected, "Renewal <ref> already has a <letter> and cannot receive a NAL", To be assigned at build]
notifications:
  - "NAL to the client's registered e-mail."
audit:
  - "Generation and sending with user, time, account and recipient."
acceptance:
  - A NAL is sent to an eligible account and the account shows NAL Sent.
  - An account with an RA cannot receive a NAL.
```

```fr
id: FR-RN-083
title: NRNS reminders and non-acceptance letters
brd:
  - BRRN.025 (p.6)
  - BRRN.037 (p.13)
actor: System (job RNW_NRNS_LETTERS, RNW_EXPIRY_SWEEP)
priority: Must have
fit: NEW
screens: Letters (NRNS tab); Expiry List (NRNS chip)
api: Jobs RNW_NRNS_LETTERS, RNW_EXPIRY_SWEEP
description:
  - BIBS classifies as NRNS (No Renew / No Submit) the accounts that are not yet submitted at the checkpoint (no disposition, or For Renewal without RA acceptance). At the checkpoint (90 days before expiry by default) the daily job generates a reminder letter; policies already submitted or in progress are excluded.
  - At the non-acceptance point (expiry by default) BIBS sends the non-acceptance letter, closes the candidate as EXPIRED_UNRENEWED and notifies the owners.
preconditions:
  - "None."
main_flow:
  - The job runs daily at 06:00 PHT and sets the NRNS flag.
  - BIBS generates reminder letters (template RNW_NRNS_REMINDER) once per candidate and checkpoint, and logs recipients.
  - The expiry sweep sends the non-acceptance letters (RNW_NON_ACCEPTANCE) and closes the candidates.
rules:
  - [R1, "Reminder checkpoint 90 days before expiry (the BRD also writes '+90 days'; RQ18).", Configurable, Parameter RNW_NRNS_REMINDER_DAYS]
  - [R2, "Non-acceptance at expiry (0 days) by default.", Configurable, Parameter RNW_NON_ACCEPTANCE_DAYS]
  - [R3, "One reminder per candidate and checkpoint.", Fixed, "-"]
validations: []
notifications:
  - "Reminder and non-acceptance letters to the client; owners notified of closures."
audit:
  - "Reminder generation and recipients; NRNS classification; closure."
acceptance:
  - A candidate without disposition 90 days before expiry gets one reminder letter and the NRNS chip.
  - A candidate already disposed For Renewal and accepted gets no reminder.
  - At expiry without acceptance the candidate closes EXPIRED_UNRENEWED with a non-acceptance letter.
```

```fr
id: FR-RN-084
title: Record client acceptance and proceed to placement and booking
brd:
  - BRRN.040 (p.14-15)
  - BRRN.038 AC3 (p.14)
actor: Marketing / Processing user (RNW_ACCEPT); System
priority: Must have
fit: CHANGE
screens: Record page (Record Acceptance); bulk upload RNW_ACCEPTANCE
api: POST /api/v1/renewal/candidates/{ref}/acceptance
description:
  - The user records the client's acceptance of the RA with its method - e-mail (attachment RA_ACCEPTANCE), signed RA (attachment SIGNED_RA) or payment (the payment gate's evidence) - and the evidence. BIBS marks the renewal Accepted.
  - When there is no unresolved exception and the payment rules are satisfied, BIBS moves the renewal account through the account workflow without resubmission (system fast track), places it (slip generated and sent when automatic placement is on) and, on policy issuance, queues it for booking. The booked invoice carries business type RENEWAL and the candidate closes RENEWED. Standard placement, booking and issuance SLAs apply.
preconditions:
  - The candidate is RA_SENT; the user has RNW_ACCEPT.
main_flow:
  - The user clicks **Record Acceptance**, chooses the method and attaches the evidence.
  - BIBS sets ACCEPTED and runs the account completeness checks.
  - BIBS moves the account to awaiting payment; the payment gate, placement, issuance and booking follow.
  - On InvoiceBooked of the renewal account, the candidate closes RENEWED.
alternate_flows:
  - Payment as acceptance. A payment on the renewal account records the acceptance with source SYSTEM.
  - Account incomplete. The fast track fails with the account's message; the account stays in draft for Processing.
  - Bulk acceptance by e-mail list through the upload RNW_ACCEPTANCE.
rules:
  - [R1, "No duplicate proposal, resubmission or new initiation: the renewal account is the one created at processing.", Fixed, "-"]
  - [R2, "Automatic placement on (default).", Configurable, Parameter RNW_AUTO_PLACEMENT]
  - [R3, "Evidence is mandatory when FINANCIAL_IMPACT failed (BRRN.038).", Fixed, "-"]
  - [R4, "Payment rules are the BRD-1 payment gate (RQ19).", Fixed, "-"]
validations:
  - [Method not selected, Select the acceptance method, "-"]
  - [Evidence missing, Attach the acceptance evidence, "-"]
  - [Blocking check open, "Renewal <ref> cannot be accepted: <check> failed", To be assigned at build]
  - [Renewal account incomplete, "Complete the mandatory fields of <product>: <fields>", ACCOUNT_INCOMPLETE]
fields_screen: Record Acceptance
fields:
  - [Method, Option, "Yes", "E-mail, Signed RA, Payment", "-"]
  - [Evidence, Attachment, Conditional, "Document types RA_ACCEPTANCE, SIGNED_RA", Required for e-mail and signed RA]
  - [Accepted on, Date, "Yes", "-", Not in the future]
notifications:
  - "RNW_ACCEPTED to the assigned users; RNW_RENEWED when booked."
audit:
  - "Acceptance with method, evidence, user or SYSTEM and time; payment-rule outcome; every status change and its trigger."
acceptance:
  - After the acceptance is recorded and the payment gate passes, the renewal account is placed and booked without re-keying.
  - The booked invoice shows business type RENEWAL and the candidate is RENEWED.
  - Acceptance of a renewal with a failed financial-impact check needs evidence.
```

```fr
id: FR-RN-085
title: Contact Center follow-up
brd:
  - BRRN.026 (p.6-7)
actor: Contact Center user
priority: Must have
fit: CHANGE
screens: Follow-ups (list); record page (Remarks & Follow-ups, Documents)
api: POST /api/v1/renewal/candidates/{ref}/followups; attachments
description:
  - Contact Center users view the renewal lists in a read-only projection and record follow-ups on an account - channel, outcome, remarks and next action date - to show the renewal status. They upload documents received from the client. Every change is logged with user and time; access is role-restricted.
preconditions:
  - The user has RNW_FOLLOWUP.
main_flow:
  - The user opens Follow-ups and selects an account.
  - The user records the follow-up and, if any, uploads the client's documents.
rules:
  - [R1, "Follow-up outcomes in LOV RNW_FOLLOWUP_OUTCOME (codes RQ21).", Configurable, LOV RNW_FOLLOWUP_OUTCOME]
  - [R2, "Contact Center cannot dispose, send letters or see premium columns.", Fixed, "-"]
validations:
  - [Outcome not selected, Select the outcome, "-"]
  - [File type not allowed, The file type is not allowed, "-"]
fields_screen: Follow-up
fields:
  - [Channel, List, "Yes", "Call, E-mail, SMS", "-"]
  - [Outcome, List, "Yes", LOV RNW_FOLLOWUP_OUTCOME, "-"]
  - [Remarks, Text, "Yes", "-", Up to 200 characters]
  - [Next action date, Date, "No", "-", Today or later]
  - [Client document, Attachment, "No", Document types, Platform file rules]
notifications:
  - "The assigned AO sees new follow-ups on the record."
audit:
  - "Each follow-up and upload with user and time."
acceptance:
  - A Contact Center user records a call outcome and uploads a signed RA; the AO sees both on the record.
  - A Contact Center user cannot see the premium columns.
```

## Submitted policies hand-off

```fr
id: FR-RN-090
title: Renew submitted policies handed over by Submitted Policies
brd:
  - Cross-BRD decision D2 (R6); BRD-12 BRIDSP-22, 23, 25, 26 (BRD-12 p.11)
  - BRRN.021 (p.4), BRRN.010 (p.27-28)
actor: System (RenewalHandOff); Marketing AO (Non-CBG Retail)
priority: Must have
fit: NEW
screens: Record page (source Submitted Policy); Letters
api: Port RenewalHandOff (implemented by Renewal)
description:
  - Renewal owns the renewal of submitted policies from the hand-off on (decision D2). When the Submitted Policies expiry scan or the "Renew with BDOI" action hands a masterlist record over, Renewal creates a candidate with source SUBMITTED_POLICY and the SBM number, initiates it at once (the scan is the explicit initiation), runs the checks and the matrix, creates the renewal account (business type RENEWAL, renewal-of reference = SBM number, the assigned insurer), requests the 30-day hold cover and queues the letters - RA (generic or FFY template), reminder, SFU for mortgaged accounts, NRNS and NAL - in the one renewal letter engine. Print letters go through the Submitted Policies mail-house port.
  - Renewal answers the port's status query (open, or closed as renewed, not renewed, lost or expired unrenewed, with the reason) so the masterlist can close records whose renewal never books.
  - A Non-CBG Retail hand-off starts at UNASSIGNED for a manual disposition.
preconditions:
  - The hand-off carries the SBM number, the policy and risk data, the RA template and the assigned insurer.
main_flow:
  - Submitted Policies calls the port.
  - Renewal creates or finds the candidate (idempotent on the SBM number) and initiates it.
  - Renewal creates the renewal account, requests the hold cover and queues the letters.
alternate_flows:
  - Hand-offs recorded as PENDING before this adapter is deployed are replayed once, idempotent on the SBM number.
rules:
  - [R1, "One candidate per SBM number; one RA per client.", Fixed, "-"]
  - [R2, "RA timing of submitted policies: 90 days before expiry (BRD-12 p.5).", Configurable, Hand-off parameters (V1017)]
  - [R3, "Submitted policies are never found by RNW_EXTRACTION, so there is no double extraction.", Fixed, "-"]
validations:
  - [Hold cover already open, "Account <arn> already has a hold cover requested or confirmed", HOLD_COVER_OPEN]
notifications:
  - "As the letters and hold cover of this document."
audit:
  - "Hand-off receipt, candidate, account, hold cover and letters with the SBM number."
acceptance:
  - A submitted CBG Motor policy handed over creates one candidate, one renewal account with a hold cover request and one RA.
  - A second hand-off of the same SBM number creates nothing new.
  - When the renewal account is booked, the masterlist record shows BOOKED.
```

## Monitoring and reports

```fr
id: FR-RN-100
title: Renewal home and officer dashboards
brd:
  - BRD 1.009.7 (p.75-76)
  - BRD 3.010.7 (p.145)
actor: Marketing TL; Processing TL; all Renewal users
priority: Must have
fit: NEW
screens: Renewal Home
api: GET /api/v1/renewal/home
description:
  - The Renewal Home shows tiles by stage and bucket, accounts expiring in 30, 60, 90 and 140 days, renewals at risk, exceptions, insurer batches overdue and letters failed. The Marketing TL sees the accounts assigned per AO and the Processing TL per Processing Officer (open, overdue against expiry, returned), with the ageing to expiry. Each tile opens its filtered list, which can be exported.
preconditions:
  - The user has RNW_VIEW.
main_flow:
  - The user opens Renewal Home.
  - The user clicks a tile or an officer row; the filtered list opens.
rules:
  - [R1, "Counts follow the user's data scope.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Exports are logged."
acceptance:
  - A Marketing TL sees, for each AO of his unit, the number of assigned open accounts.
  - Clicking "Expiring in 30 days" opens the list filtered to 30 days.
```

```fr
id: FR-RN-101
title: Renewal status report with summary
brd:
  - BRRN.019 (p.32-33), restating BRD 1.009.3.1.6, 2.008.3.1.6, 3.010.3.1.6, 4.008.3.1.6
  - BRD 1.009.1-1.009.6, 2.008.1-2.008.6, 3.010.1-3.010.6, 4.008.1-4.008.6 with their sub-IDs (p.58-75, 89-106, 128-145, 159-177)
actor: All report users (RNW_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Renewal Reports (RNW-STATUS)
api: Report RNW-STATUS; GET /api/v1/reports/RNW-STATUS/export?format=
description:
  - The user selects and changes the expiry date range and the twenty criteria of section 6.1, each with single, multiple, all except and all selection and search in the drop-down. The report shows the overall renewal status per invoice, all rows in one scrollable view, with 37 columns for Marketing and 40 for Processing (section 6.3), and a summary of 34 counters (section 6.5).
  - The report exports to .xlsx and .pdf, is saved, printed with preview and settings, and can be saved as a variant. Data is limited to the user's scope; the report is generated within 20 seconds.
preconditions:
  - The user has RNW_REPORT_VIEW.
main_flow:
  - The user opens RNW-STATUS and sets the range and criteria.
  - BIBS runs the report and shows rows and summary.
  - The user exports, prints or saves the variant.
rules:
  - [R1, "Column set by persona variant: MARKETING (37) or PROCESSING (40).", Fixed, "-"]
  - [R2, "Counters are documented predicates on stage, disposition, reason and mortgage flag (section 6.5); four counters have no source in the BRD and show 0 with 'pending definition' until RQ10 is answered.", Configurable, Report definition (RQ10)]
  - [R3, "Presets current, prior and next month.", Fixed, "-"]
validations:
  - [Range missing or end before start, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Each run and export with user, parameters and time."
acceptance:
  - RNW-STATUS for March with Risk Code "all except" MC01 lists the March invoices except MC01 and shows the 34 counters.
  - The Excel and PDF exports have the same rows as the screen.
  - The Processing variant shows the renewal processor, the assigned AO and the sum insured per cover.
```

```fr
id: FR-RN-102
title: Centralised renewal listings with escalation
brd:
  - BRRN.036 (p.12-13)
actor: User (Marketing TL, Unit Head); System (escalation)
priority: Must have
fit: NEW
screens: Renewal Reports (RNW-LISTING); Renewal Home (At risk tile)
api: Report RNW-LISTING; alert RNW_RENEWAL_AT_RISK
description:
  - Users generate renewal listings by unit and segment (for example UH, IBG, Leasing), by expiry window (current month, prior month, future or chosen months) and by status (Renewed, Unrenewed, Expired). The listings are live, viewed on screen and extracted, with the ageing to expiry.
  - For IBG and Leasing the listing supports escalation. Accounts not disposed or not accepted within the escalation days before expiry raise the alert RNW_RENEWAL_AT_RISK to the unit head and appear flagged as at risk.
preconditions:
  - The user has RNW_REPORT_VIEW.
main_flow:
  - The user chooses unit, segment, window and status, and runs the listing.
  - The daily alert check raises RNW_RENEWAL_AT_RISK for accounts inside the escalation days.
rules:
  - [R1, "Escalation days IBG 60, Leasing 60, others 30 (default).", Configurable, Parameter RNW_ESCALATION_DAYS (RQ11)]
  - [R2, "Status groups: Renewed = closed RENEWED; Unrenewed = open; Expired = past expiry and not renewed.", Fixed, "-"]
validations: []
notifications:
  - "RNW_RENEWAL_AT_RISK to the unit head of the account."
audit:
  - "Runs, exports and alerts."
acceptance:
  - A listing of IBG for the next month shows each account with its status and days to expiry.
  - An IBG account not disposed 59 days before expiry raises the at-risk alert once.
```

```fr
id: FR-RN-103
title: Operational renewal reports
brd:
  - BRRN.020 (p.4), BRRN.023 (p.4-6), BRRN.029 (p.8), BRRN.031 (p.9-10), BRRN.034 (p.11)
  - BRD 3.009.2-3.009.4.2, 4.007.2-4.007.4.2 (p.126-127, 158)
  - BRRN.001, 010, 025, 037 (p.6, 13, 22, 27-28)
actor: Report users (RNW_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Renewal Reports
api: Reports RNW-SANITATION, RNW-DECISIONS, RNW-LAMD-MATCH, RNW-INSURER-EXTRACT, RNW-RA-DISPATCH, RNW-WORKLOAD
description:
  - Beside the status report and listings, the Renewal report category holds the check results and bucket changes (RNW-SANITATION), the dispositions with matrix rule and version and the overrides with rationale (RNW-DECISIONS), the LAMD match and routing (RNW-LAMD-MATCH), the 28-column extract per insurer (RNW-INSURER-EXTRACT), the letter log with delivery status (RNW-RA-DISPATCH) and the accounts per AO and PO by stage (RNW-WORKLOAD). Section 6.1 lists them.
preconditions:
  - The user has RNW_REPORT_VIEW.
main_flow:
  - The user selects a report, sets the parameters, runs and exports it.
rules:
  - [R1, "Every report exports to PDF, XLSX, ODS and CSV, supports saved variants and applies the user's scope.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Each run and export."
acceptance:
  - RNW-DECISIONS lists each system disposition with the matrix version and rule, and each override with user and remarks.
  - RNW-RA-DISPATCH lists every RA of a range with recipient and send status.
```

## Administration

```fr
id: FR-RN-110
title: Maintain the renewal lists of values
brd:
  - BRD 5.003.1, 5.003.2, 5.003.3, 5.003.4, 5.003.5 (p.179-180)
  - BRD 6.002.2.23 (p.185)
actor: Business Administrator (LOV_MANAGE)
priority: Must have
fit: CONFIGURE
screens: Lists of Values (Broking Setup)
api: /api/v1/lov
description:
  - The Business Administrator views, adds, edits and ends the values of the renewal lists (section 9.2) with an effective date; a change takes effect on its date. "Delete" end-dates a value, since records that used it must stay readable. Each change is authorised by a checker.
preconditions:
  - The user has LOV_MANAGE.
main_flow:
  - The user opens the list, adds or edits a value with its effective dates, and saves.
  - A checker authorises the change; it takes effect on the effective date.
rules:
  - [R1, "Values are end-dated, never physically deleted.", Fixed, "-"]
  - [R2, "Disposition codes are fixed; only their labels change.", Fixed, "-"]
validations:
  - [Effective to before effective from, The effective-to date is before the effective-from date, LOV_EFFECTIVITY_INVALID]
  - [List maintained by the system, "The list <type> is maintained by the system", LOV_NOT_MAINTAINABLE]
fields_screen: List value
fields:
  - [Code, Text, "Yes", "-", Unique in the list]
  - [Label, Text, "Yes", "-", Up to 120 characters]
  - [Effective from / to, Date, "Yes (from)", "-", To on or after from]
  - [Attributes, Per list, "No", "for example nal_eligible, requires_invoice_no", "-"]
notifications:
  - "Checkers in My Approvals."
audit:
  - "Changes with maker, checker, before and after values."
acceptance:
  - A new non-renewal reason effective next month is selectable from that date only.
  - An end-dated reason stays visible on the accounts that used it.
```

```fr
id: FR-RN-111
title: Update the Renewal Advice and letter templates
brd:
  - BRD 5.005.1 (p.180)
  - BRD 6.002.2.25 (p.185)
actor: Business Administrator (RNW_TEMPLATE_MAINTAIN)
priority: Must have
fit: CONFIGURE
screens: Document Templates
api: /api/v1/docgen/templates
description:
  - The Business Administrator updates the RA template and the other renewal templates (section 6.6). A new template version is used for every document generated after its activation; documents already generated keep their version.
preconditions:
  - The user has RNW_TEMPLATE_MAINTAIN.
main_flow:
  - The user opens the template, edits it and saves a new version.
  - The user activates the version; later RAs use it.
rules:
  - [R1, "Every generated letter records the template code and version.", Fixed, "-"]
validations:
  - [Unknown placeholder, "The template uses an unknown field <name>", To be assigned at build]
notifications:
  - "None."
audit:
  - "Template versions with user and time."
acceptance:
  - An RA generated after the new version is activated shows the new layout and version.
```

```fr
id: FR-RN-112
title: Maintain renewal rules and parameters
brd:
  - BRRN.023 (p.4-6), BRRN.030 (p.8-9), BRRN.034 (p.11), BRRN.009 (p.26-27)
actor: Business Administrator (RNW_SETUP); checker (MASTER_AUTHORIZE)
priority: Must have
fit: NEW
screens: Renewal Setup (Check Settings, Bucket Rules, Decision Matrix, Non-renewable Risk Codes, Parameters)
api: /api/v1/renewal/setup/**
description:
  - The Renewal Setup screen holds the business rules that govern the renewal without IT - the severity and parameters of each check, the bucket rule sets, the decision matrix, the non-renewable risk codes and the renewal parameters (lead days, thresholds, notice days). Rule sets and matrices are versioned drafts until a checker activates them; parameters are changed through the platform parameter screen.
preconditions:
  - The user has RNW_SETUP.
main_flow:
  - The user drafts a new version of a rule set or matrix and submits it.
  - A checker compares it with the active version and activates it with an effective date.
rules:
  - [R1, "Lead days must be configurable, never hard-coded (BRRN.030 AC2).", Fixed, "-"]
  - [R2, "A bucket rule can never map a failed check to Clean (database and service guard).", Fixed, "-"]
validations:
  - [Rule maps a failure to Clean, A failed check cannot give the Clean bucket, To be assigned at build]
  - [Activation by the maker, A version is activated by someone other than its maker, To be assigned at build]
notifications:
  - "Checkers when a version is submitted."
audit:
  - "Versions with maker, checker, dates and differences."
acceptance:
  - Changing the lead days from 140 to 120 applies from the next extraction run.
  - A bucket rule that maps a failed check to Clean cannot be saved.
```

# Workflow and status model

## Stages

Figure 2 and Figure 3 show the workflow RNW_CASE, one work case per candidate. Solid arrows are the main path, dashed arrows are returns, system routes and optional paths, and dotted arrows close a case. The system routes from EVALUATING to FOR_PROCESSING (CBG straight-through) and to LETTER_PENDING (system Not for Renewal) are in Table 11. The SLA of a renewal is measured against the **expiry date**, not the age of the stage.

![Workflow RNW_CASE part 1: extraction to posting](figures/brd06_workflow_marketing.dot)

![Workflow RNW_CASE part 2: processing to closure](figures/brd06_workflow_processing.dot)

<!-- table: widths=3.6,4.2,5,4 caption="Stages of RNW_CASE" size=8 -->
| Stage | Owner (permission) | Business actions | Screen label |
|---|---|---|---|
| EXTRACTED | Marketing / Processing TL (RNW_EXTRACT) | Initiate | Extracted |
| EVALUATING | System | Checks, bucket, matrix; route | Evaluating |
| UNASSIGNED | Marketing TL (RNW_ASSIGN) | Assign, transfer | Unassigned Disposition |
| TRANSFER_PENDING | Receiving TL (RNW_ASSIGN) | Accept, decline | Transfer pending |
| FOR_DISPOSITION | Marketing AO (RNW_DISPOSE) | Dispose, remarks, push, transfer request, re-assign | For disposition / Returned |
| FOR_TL_REVIEW | Marketing TL (RNW_REVIEW) | Return, post, override | Review in progress |
| NB_PATH | Marketing (RNW_DISPOSE) | Start quotation or PRF; follow it | For Proposal / New Business Path |
| FOR_PROCESSING | Processing TL (RNW_PROCESS_ASSIGN) | Assign PO, upload, return | For processing |
| IN_PROCESSING | Processing Officer (RNW_PROCESS) | Update data, computations, return, send to insurer | In processing |
| WITH_INSURER | Processing (RNW_INSURER) | Upload response, override mismatch | With insurer |
| RA_READY | Marketing / Processing (RNW_RA_GENERATE) | Generate RA | RA ready |
| RA_GENERATED | Marketing / Processing (RNW_RA_SEND) | Send, cancel RA (override) | RA generated |
| RA_SENT | Marketing / Processing (RNW_ACCEPT) | Record acceptance, second notice | RA Sent / Awaiting Response |
| ACCEPTED | System | Fast track of the renewal account | Accepted |
| FOR_PLACEMENT_BOOKING | NB placement and booking users | BRD-1 payment gate, placement, issuance, booking | For Placement and Booking |
| LETTER_PENDING | Marketing / Processing (RNW_RA_GENERATE) | Send NFR or NAL; re-open | Letter pending |
| RENEWED | - | terminal | Renewed |
| CLOSED | - | terminal; closed as NOT_RENEWED, LOST, EXPIRED_UNRENEWED or BOOKED_OTHER_INVOICE | Closed |

## Transitions

<!-- table: widths=3.4,3.4,3.6,3.4,3.2 caption="Transitions of RNW_CASE" size=8 -->
| From | Action | To | Permission | Condition / reason |
|---|---|---|---|---|
| EXTRACTED | initiate | EVALUATING | RNW_EXTRACT | Bulk only for the bulk segments |
| EVALUATING | route (system) | UNASSIGNED | - | Non-CBG, MANUAL or not Clean |
| EVALUATING | route (system) | FOR_TL_REVIEW or FOR_PROCESSING | - | Clean and AUTO; CBG STP |
| EVALUATING | route (system) | LETTER_PENDING | - | System Not for Renewal |
| UNASSIGNED | assign | FOR_DISPOSITION | RNW_ASSIGN | - |
| UNASSIGNED, FOR_DISPOSITION | transfer_request | TRANSFER_PENDING | RNW_ASSIGN | Remarks |
| TRANSFER_PENDING | accept / decline | UNASSIGNED (receiving unit) / previous stage | RNW_ASSIGN | Decline remarks |
| FOR_DISPOSITION | dispose + push | FOR_TL_REVIEW | RNW_DISPOSE | History viewed |
| FOR_TL_REVIEW | return | FOR_DISPOSITION | RNW_REVIEW | RNW_RETURN_REASON |
| FOR_TL_REVIEW | post | FOR_PROCESSING, NB_PATH, LETTER_PENDING or CLOSED | RNW_REVIEW | By disposition; no blocking check |
| FOR_PROCESSING | assign_po | IN_PROCESSING | RNW_PROCESS_ASSIGN | - |
| FOR_PROCESSING, IN_PROCESSING | return | FOR_DISPOSITION or FOR_TL_REVIEW | RNW_PROCESS | RNW_RETURN_REASON |
| IN_PROCESSING | send_to_insurer | WITH_INSURER | RNW_INSURER | Batch sent |
| WITH_INSURER | insurer_response | RA_READY / IN_PROCESSING / LETTER_PENDING or FOR_DISPOSITION | RNW_INSURER or system | Renew As Is matched / Revise / Reject |
| RA_READY | generate_ra | RA_GENERATED | RNW_RA_GENERATE | No blocking check |
| RA_GENERATED | send | RA_SENT | RNW_RA_SEND | - |
| RA_SENT | accept | ACCEPTED | RNW_ACCEPT or system (payment) | Evidence |
| ACCEPTED | fast track (system) | FOR_PLACEMENT_BOOKING | - | Account checks pass |
| FOR_PLACEMENT_BOOKING | booked (system) | RENEWED | - | InvoiceBooked of the renewal ARN |
| NB_PATH | booked / declined (system) | RENEWED / CLOSED (LOST) | - | Quotation or PRF outcome |
| LETTER_PENDING | send NFR / NAL | CLOSED (NOT_RENEWED) | RNW_RA_SEND | - |
| CLOSED (NOT_RENEWED) | reopen | FOR_DISPOSITION | RNW_DISPOSE | Until expiry + RNW_REOPEN_DAYS |
| RA_SENT, FOR_DISPOSITION, UNASSIGNED | expiry sweep (system) | CLOSED (EXPIRED_UNRENEWED) | - | Expiry + RNW_NON_ACCEPTANCE_DAYS |
| any open stage | override | target of the override | RNW_OVERRIDE | Mandatory remarks |

## Checks, buckets and decision matrix

Figure 4 shows how a trigger runs the checks, how the bucket rule set turns the results into a bucket, and how the decision matrix decides between an automatic and a manual disposition.

![Checks, buckets and decision matrix (BRRN.020, 023, 031, 034)](figures/brd06_rules.dot)

<!-- table: widths=4.2,3.4,5.6,3.4 caption="Checks and default severities (list to confirm, RQ01)" size=8 -->
| Check | BRD | Source | Default severity |
|---|---|---|---|
| REFERENCE_MATCH | BRRN.022 | Candidate against expiring invoice, ARN and policy | FAIL_EXCEPTION |
| PN_PRESENT | BRRN.029, 031, 039 | PN on the account or invoice (mortgaged and CBG lines) | FAIL_EXCEPTION |
| RISK_CODE_RENEWABLE | BRRN.009 | Non-renewable risk codes | System Not for Renewal |
| LAMD_STATUS | BRRN.029, 039 | LAMD lines matched by PN | System Not for Renewal / RMU routing |
| CLAIMS | BRRN.031, 034 | Claims module summary (INFO while not connected) | FAIL_REVIEW when claims are open |
| ENDORSEMENT_PENDING | BRRN.032 | Open endorsement requests on the invoice family | FAIL_REVIEW |
| OUTSTANDING_PREMIUM | BRD 1.011; BRRN.031 | Invoice family balance above the threshold | FAIL_REVIEW (TL override) |
| FINANCIAL_IMPACT | BRRN.038 | Renewal account or insurer revision against the expiring invoice | FAIL_REVIEW; NB path by the matrix |
| INSURER_RESPONSE_MATCH | BRRN.035 | Latest valid response matched, not late or conflicting | FAIL_EXCEPTION |
| MANDATORY_FIELDS | BRD 2.004.5, 3.007.3 | Disposition requirements and product field rules | FAIL_REVIEW |
| PRODUCT_RENEWABLE | PQ11 | Package version sellable for renewal | FAIL_REVIEW (RQ30) |
| INSURER_USABLE | - | Insurer active and usable | FAIL_EXCEPTION |
| DUPLICATE_CANDIDATE | BRRN.005 | Another open candidate or live renewal on the same risk | FAIL_EXCEPTION |
| KYC_DUE | BRRN.028 | Client KYC review within the due window | INFO only, never blocks |

## Insurer batch and letter states

![Insurer batch and letter states](figures/brd06_letter_states.dot)

<!-- table: widths=3.2,5,8.4 caption="Letter types and numbers" size=8.5 -->
| Letter | Number | When |
|---|---|---|
| Renewal Advice (first, second notice) | RA-yyyy | RA_READY; second notice after RNW_RA_SECOND_NOTICE_DAYS without acceptance |
| No Advice Letter | NAL-yyyy | Eligible accounts that need neither RA nor NFR |
| Not for Renewal Letter | NFR-yyyy | System or user Not for Renewal (LETTER_PENDING) |
| NRNS reminder | RNL-yyyy | RNW_NRNS_REMINDER_DAYS before expiry, not yet submitted |
| Non-acceptance letter | NAC-yyyy | RNW_NON_ACCEPTANCE_DAYS after expiry without acceptance |
| SFU letter | - | Submitted mortgaged accounts (FR-RN-090) |

## SLA and escalation

- A renewal's urgency is its days to expiry. The at-risk alert RNW_RENEWAL_AT_RISK uses RNW_ESCALATION_DAYS per segment (FR-RN-102); alert RNW_EXCEPTION_AGEING flags exceptions that stay open.
- Insurer batches past their reply date raise RNW_INSURER_OVERDUE; failed letters raise RNW_LETTER_FAILED; failed extraction runs raise RNW_EXTRACTION_FAILED.
- After acceptance, the standard BRD-1 placement, booking and issuance SLAs apply (BRRN.040 AC5).
- The BRD gives no stage SLA in hours; none is configured until BDOI states one.

# Reports and documents

## Reports

<!-- table: widths=3.8,4.2,6,2.6 caption="Renewal reports (category Renewal)" -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| RNW-EXPIRY-LIST | Expiring list | The grid as displayed, with its filters (section 6.2 columns) | BRRN.004, 007, 008 |
| RNW-STATUS | Renewal status report | Per invoice, 37 or 40 columns, 34-line summary; variants MARKETING and PROCESSING | BRD 1.009, 2.008, 3.010, 4.008; BRRN.019 |
| RNW-LISTING | Renewal listing | Unit, segment, window and status; ageing to expiry; escalation flag | BRRN.036 |
| RNW-INSURER-EXTRACT | Extract per insurer | 28 columns per insurer (section 6.4) | BRD 3.009, 4.007 |
| RNW-RA-DISPATCH | Letter dispatch | Details for RA sending; RA, NAL, NFR and NRNS letters with recipient and status | BRD 1.010.6; BRRN.001, 010, 025, 037 |
| RNW-SANITATION | Sanitation | Check results, bucket and changes | BRRN.020, 023 |
| RNW-DECISIONS | Decision log | Dispositions, matrix rule and version, overrides with rationale | BRRN.031, 034; BRD 1.011 |
| RNW-LAMD-MATCH | LAMD match | LAMD lines matched and unmatched, with the routing | BRRN.029 |
| RNW-WORKLOAD | Workload | Accounts per AO and PO by stage | BRD 1.009.7, 3.010.7 |

Every report needs RNW_REPORT_VIEW, applies the user's scope, exports to PDF, XLSX, ODS and CSV, has print preview and saved variants, and answers within 20 seconds at 30,000 rows.

**Report criteria** (BRD 1.009.2.1-40, the same in 2.008.2, 3.010.2 and 4.008.2). Each criterion supports single, multiple, all except and all, and a search in the drop-down: Unit Head, Risk Code, Insurance Company, Insurance Group, Business Origin, Market Category, BDO Branch, BDO Area, Region, Bank Officer, Bank Unit, Product Code, Account Type, BDOI Branch, Location, Account Broker, Business Type, Customer Segment, Specific Client, Renewal Status.

## Expiring list columns

<!-- table: widths=1,5.2,1.9,1.9,6.6 caption="Expiring list columns (BRD 1.003.4.1.1.1-24, p.47-50; 3.003.4.1.1-21, p.111-114)" size=8 -->
| # | Column | Marketing | Processing | Content in BIBS |
|---|---|---|---|---|
| 1 | Expiring EBIX Invoice No. | OUT | OUT | Replaced by the BIBS expiring invoice number (p.34) |
| 2 | Renewal Reference No. | Y | Y | RNW-yyyy-nnnnnn |
| 3 | BDOIsys Reference No. | OUT | OUT | Replaced by the renewal reference and ARN (p.34) |
| 4-5 | Cover No.; Version No. | Y | Y | From the expiring invoice |
| 6-9 | Invoicing Branch; Department Code; Department Name; Unit Head | Y | Y | Sales organisation of the account |
| 10-13 | Client Name; Assured's Name; Business Origin; Account Officer | Y | Y | Account and client master |
| 14-18 | Expiring Policy No.; Insurance Company; Risk Code; Risk Name; Risk Description | Y | Y | Account, insurer and catalogue |
| 19-21 | Renewal Stage / Status; Renewal Disposition; Disposition Remarks | Y | Y | Candidate |
| 22-23 | Number of Claims; Status of Each Claim | Y | - | Claims module (decision D4) |
| 24 | Amount of Outstanding Premium | Y | - | Operations ledger, invoice family |
| + | Classification; flags (Returned, Transferred, Endorsed, Claims, Outstanding, KYC due, NRNS) | Y | Y | Workshop addendum |

## Renewal status report columns

Columns 1-37 are in every variant; 38-40 only in the Processing variant (BRD 1.009.3.1.1-37, 3.010.3.1.38-40).

<!-- table: widths=1,5.6,1,5.6,1,5.6 caption="RNW-STATUS columns" size=8 -->
| # | Column | # | Column | # | Column |
|---|---|---|---|---|---|
| 1 | Unit Head | 15 | Basic Premium | 29 | Location |
| 2 | Account Broker | 16 | Gross Premium | 30 | Mortgaged To BDO |
| 3 | Business Origin | 17 | Premium Rate | 31 | Mortgagee Bank |
| 4 | Bank Officer | 18 | Commission Rate | 32 | PN Number |
| 5 | Renewal Reference | 19 | Total Sum Insured | 33 | Hold Cover (HC) |
| 6 | Expiring Invoice No. (BIBS; BRRN.019) | 20 | Risk Description | 34 | HC End Date |
| 7 | Assured | 21 | Location of Risk | 35 | Renewal Status |
| 8 | Type of Risk | 22 | Address | 36 | Reason for "Not for Renewal" |
| 9 | Expiring Invoice Inception Date | 23 | Contact No(s). | 37 | Remarks of Acct Officer |
| 10 | Expiring Invoice Expiry Date | 24 | Email Address | 38 | Name of Renewal Processor |
| 11 | Renewal Reference Inception Date | 25 | Region | 39 | Name of Assigned Marketing AO |
| 12 | Renewal Reference Expiry Date | 26 | Area | 40 | Sum Insured per Cover |
| 13 | Expiring Invoice Policy Number | 27 | Branch |  |  |
| 14 | Account Type | 28 | Insurer |  |  |

Hold Cover and HC End Date are read from the hold cover of the renewal account. The Expiring Ebix Invoice No. of the BRD is the BIBS expiring invoice number (BRRN.019, p.34).

## Insurer extract columns

<!-- table: widths=1,5.6,1,5.6,1,5.6 caption="RNW-INSURER-EXTRACT columns (BRD 3.009.1.4.1-28, p.122-126)" size=8 -->
| # | Column | # | Column | # | Column |
|---|---|---|---|---|---|
| 1 | Insurance Company | 11 | Is Mortgaged (Yes / No) | 21 | Branch |
| 2 | Invoice No | 12 | Premium Rate (per cover) | 22 | Account Officer Name |
| 3 | Assured Name | 13 | Policy No | 23 | Remarks |
| 4 | Risk Code | 14 | Risk Description | 24 | BIPD for Motor |
| 5 | Inception Date | 15 | Encoder Name (user who booked) | 25 | Seating Capacity for Motor |
| 6 | Expiry Date | 16 | Unit Head | 26 | Auto Personal Accident for Motor |
| 7 | Sum Insured | 17 | Business Origin | 27 | Sum Insured (per cover) |
| 8 | Premium Amount | 18 | Account Type | 28 | Packaged / Non-Packaged |
| 9 | Commission Rate | 19 | Region | | |
| 10 | AR Client (Unpaid Balance) | 20 | Area | | |

## Summary counters

The mapping below is the project's proposal; BDOI confirms each counter under RQ10. Counters marked "pending" have no disposition, reason or step in the BRD that produces them and show 0 until defined.

<!-- table: widths=0.9,6.2,9.5 caption="RNW-STATUS summary counters (BRD 1.009.3.2.1-34, p.70-74)" size=8 -->
| # | Counter | Predicate in BIBS |
|---|---|---|
| 1 | Booked | Candidate closed RENEWED |
| 2 | Cancelled | Renewal account cancelled |
| 3 | Renewal - For Placement | Stage FOR_PLACEMENT_BOOKING |
| 4 | Renewal - For Proposal | NB_PATH with a PRF |
| 5 | Renewal - For ARF | NB_PATH with a quotation (ARF to define: pending) |
| 6 | RA Processed | RA_SENT or later |
| 7 | Renewal - For RA Processing | RA_READY or RA_GENERATED |
| 8 | Renew to TSU | NB path routed to TSU (pending) |
| 9 | Returned to Marketing | Returned flag set |
| 10-13 | Awaiting Marketing TL Approval (For Renewal, For Quotation, Not For Renewal, Lost Business) | FOR_TL_REVIEW by disposition |
| 14 | Disapproved Accounts | TL return with reason DISAPPROVED (pending) |
| 15 | Not For Renewal Accounts | Disposition Not for Renewal |
| 16-23 | Mortgaged: Lost Business; Unrenewed For Proposal; Transferred to Other Unit; Loan Fully Paid; Awaiting Confirmation; Booked under New Invoice; No Disposition; to Other Bank | Mortgaged = Yes, split by disposition, reason or stage; "Awaiting Confirmation" = RA_SENT; "to Other Bank" pending |
| 24-31 | Non-Mortgaged: the same eight counters | Mortgaged = No, same predicates |
| 32 | Non-Renewable Accounts | Reason Non-renewable Accounts |
| 33 | No Disposition | UNASSIGNED or FOR_DISPOSITION without disposition |
| 34 | Total No. of Accounts | All candidates in scope |

## Documents

Documents are generated from versioned templates. The layouts are drafts until BDOI provides its own (RQ26).

<!-- table: widths=4,3,9.6 caption="Renewal documents" size=8.5 -->
| Template | Output | Content |
|---|---|---|
| RNW_RA_FIRST, RNW_RA_SECOND | PDF, protected e-mail | Client and policy details; renewal period; renewal terms and premium; payment instructions; BDOI contacts; notice (first or second) |
| RNW_RA_FFY, RNW_SFU | PDF, e-mail or print | Submitted-policy variants (FFY RA; SFU for mortgaged accounts) |
| RNW_NAL | PDF, e-mail | No Advice Letter |
| RNW_NFR | PDF, e-mail | Not for Renewal Letter with the reason |
| RNW_NRNS_REMINDER | PDF, e-mail | Reminder that the renewal has not been submitted |
| RNW_NON_ACCEPTANCE | PDF, e-mail | Non-acceptance notice after expiry |
| RNW_INSURER_COVER | E-mail | Cover e-mail of the insurer extract |
| RNW_ACCOUNT_DETAILS | PDF, XLSX | Account details of the record page (BRD 1.004.1.3) |

RAs are stored as document type RENEWAL_ADVICE; the other letters as RENEWAL_LETTER; both are linked to the candidate, the renewal account and the client (decision D3).

# Interfaces and integration

Figure 6 shows the interfaces of Renewal. Renewal reads the expiring population from the Operations ledger and the account, and hands the renewal back to the BRD-1 modules as an account of business type RENEWAL.

![Interfaces of Renewal (dashed = upload or parked)](figures/brd06_integration.dot)

<!-- table: widths=3.8,1.8,7.2,2.4,1.6 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Operations ledger | In | Booked root invoices, balances, movements; InvoiceBooked, InvoiceMovementPosted events | BRRN.005, 030; BRD 1.011 | BUILT |
| Account (BRD-1) | In / Out | Expiring policy data; renewal account (business type RENEWAL, BT0), fast track, status events | BRRN.033, 040 | CHANGE |
| Quotation, PRF (BRD-1) | Out | NB path with the renewal reference | BRRN.033 | CHANGE |
| Placement, issuance, booking (BRD-1) | Out | Slip, hold cover, booking queue source RENEWAL; InvoiceBooked back | BRRN.040 | CHANGE |
| Product catalogue (BRD-3) | In | Rating purpose RENEWAL; package version of the expiring account | PQ11 | BUILT |
| Adjustment | In | Open endorsement requests of the invoice family | BRRN.032 | BUILT |
| Claims (BRD-7) | In | Claims summary per ARN and policy year (decision D4) | BRRN.027, 031, 034 | NEW |
| Client master | In | KYC review date | BRRN.028 | BUILT |
| Submitted Policies (BRD-12) | In | RenewalHandOff; status query back | Decision D2 | NEW |
| E-mail outbox | Out | Protected RA, letters, insurer extract; send log | BRRN.010; BRD 3.009.5 | BUILT |
| Insurers | In | Response file by upload; no insurer API or SFTP | BRD 3.009.6; BRRN.035 | PARKED |
| LAMD | In | Paid-off and RMU reports by upload | BRRN.029 | PARKED |
| Legacy EBIX / QPS | In | One-time load of expiring legacy policies (NB path only) | RQ27 | PARKED |
| Directory sign-in | In | BDO EUA / Windows ID (decision D6) | BRD x.001 | PARKED |

> [!PARKED] Parked seams
> Insurer channels, the LAMD feed, legacy policies, the recipient-domain policy and directory sign-in each have a port or an upload. The default is a manual upload or "not connected"; adding a channel is a new adapter with no change to the workflow.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.186-188)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Marketing TL 75 (23 concurrent); AO / Account Broker 319 (50); Processing TL 50 (15) for lists, 9 (3) for assignment and upload; Processing Officer / Broker 73 (22); reports and RA 485 (145) | Within the BRD-1 sizing of 145 concurrent users; Renewal adds load, not users | FIT |
| Volumes | 25,800 transactions a month per process, growth 20% a year | About 26,000 candidates a month; indexes on company, stage, expiry, assignee and bucket; no partitioning for 5 years | CONFIGURE |
| Response time | 10 seconds online; 20 seconds for reports and RA generation | Grid served in chunks; RA generation and batch sending as jobs with progress; reports within 20 seconds at 30,000 rows | CONFIGURE |
| Peak | Month-end; 08:00-10:00 and 15:00-17:00 daily | Extraction, re-evaluation and letter jobs run at night | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Availability | Use 07:00-22:00 Monday to Saturday; availability, downtime and maintenance "follow existing QPS set up" | BRD-1 window; QPS is out of scope, so BDOI gives the values (RQ28, XQ08) | OPEN |
| Recovery | "Follow existing QPS set up" | Platform backup and recovery; one BIBS-wide NFR set is being agreed (XQ08) | OPEN |
| Retention | "Follow existing QPS set up" | BRD-1 retention framework with record type RENEWAL_CANDIDATE; values to confirm (RQ28) | OPEN |
| Security | Encrypted, password-protected RA and insurer files; role-based access; approved domains and TLS | Protected e-mails; permissions and data scope; recipient policy port (RQ16) | CHANGE |
| Audit | Every action logged with user, time and source | Append-only history and audit (FR-RN-004) | FIT |

# Configuration items owned by the Business and System Administrators

The items below are changed in BIBS without a release. Changes are audited.

## Parameters

<!-- table: widths=6.2,3,7.4 caption="Renewal parameters (category RENEWAL)" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| RNW_EXTRACTION_LEAD_DAYS | 140 | Days before expiry at which the job extracts a policy |
| RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT | empty | Lead days per segment (segment=days) |
| RNW_BULK_INITIATION_SEGMENTS | CLG | Segments initiated in bulk |
| RNW_CBG_STP_LINES | MOTOR, PROPERTY (CBG) | Lines of the CBG straight-through path |
| RNW_STP_SKIP_TL_REVIEW | true | AUTO dispositions skip the TL review |
| RNW_OUTSTANDING_THRESHOLD | 0.00 | Open premium above which the outstanding flag is raised |
| RNW_FIN_IMPACT_TOLERANCE | 0.00 | Difference above which a renewal has a financial impact |
| RNW_RA_MIN_NOTICE_DAYS | 30 | RA later than this before expiry needs a confirmation |
| RNW_RA_SECOND_NOTICE_DAYS | 15 | Days after the first notice before a second notice |
| RNW_NRNS_REMINDER_DAYS | 90 | Days before expiry of the NRNS reminder |
| RNW_NON_ACCEPTANCE_DAYS | 0 | Days after expiry of the non-acceptance letter and closure |
| RNW_REOPEN_DAYS | 30 | Days after expiry during which Not for Renewal can be re-opened |
| RNW_ESCALATION_DAYS | IBG=60, LEASING=60, *=30 | Days to expiry of the at-risk alert per segment |
| RNW_KYC_SEGMENTS | empty (all) | Segments where the KYC flag applies |
| RNW_RMU_UNIT | empty | Unit that receives RMU accounts (empty = tag only) |
| RNW_AUTO_PLACEMENT | true | Slip generated and sent automatically after acceptance |
| RNW_REFERENCE_PREFIX | RNW-<yyyy> | Prefix of renewal references |
| RNW_EXCLUDED_LINES | HMO, GLI, GPA | Lines excluded from extraction (Employee Benefits) |
| Job schedules | 01:00, 01:30, 06:00 PHT; sweep 00:15 PHT | RNW_EXTRACTION, RNW_REEVALUATE, RNW_NRNS_LETTERS, RNW_EXPIRY_SWEEP |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| RNW_DISPOSITION | For Renewal; Not for Renewal; For Quotation; For Proposal; Lost Business (labels only; codes fixed) |
| RNW_NONRENEWAL_REASON | RMU; With submitted policy; Loan fully paid; Direct to Insurer; Total Loss Claim; Unit Sold; Canceled Policy; Transfer to Another Marketing Unit; Non-renewable Accounts; Booked to New Invoice (attributes NAL-eligible, requires invoice no.) |
| RNW_RETURN_REASON | Values to be supplied by BDOI; Disapproved is delivered for the summary counter 14 (RQ10) |
| RNW_TRANSFER_REASON | Values to be supplied by BDOI |
| RNW_FOLLOWUP_OUTCOME | To be supplied by BDOI (RQ21) |
| RNW_OVERRIDE_REASON | To be supplied by BDOI |
| RNW_INSURER_RESPONSE | Renew As Is; Revise; Reject |
| RNW_LAMD_STATUS | Paid-off; RMU |
| DOCUMENT_TYPE (Renewal values) | RA_ACCEPTANCE; SIGNED_RA; LAMD_REPORT; INSURER_RENEWAL_FILE; RENEWAL_LETTER; RENEWAL_ADVICE |

## Masters and rules maintained by the business

<!-- table: widths=5.2,4.8,6.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Non-renewable risk codes | Business Administrator (MASTER_AUTHORIZE) | FR-RN-024 |
| Check settings (active, severity, parameters) | Business Administrator (MASTER_AUTHORIZE) | FR-RN-020, 112 |
| Bucket rule sets | Business Administrator (MASTER_AUTHORIZE) | FR-RN-022, 112 |
| Decision matrix | Business Administrator (MASTER_AUTHORIZE) | FR-RN-023, 112 |
| Lists of values | Business Administrator (checker) | FR-RN-110 |
| Renewal templates | Business Administrator | FR-RN-111 |
| Upload templates (dispositioned file, insurer response, LAMD, acceptance) | System Administrator | FR-RN-060, 071, 025, 084 |
| Roles and function grants | System Administrator via access request (approver) | FR-RN-003 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-RN-01 | The Workshop addendum (R1) is the latest layer; a BRRN of Addendum 1 governs the main-BRD IDs it restates; the main BRD governs the rest | R1-R3 |
| A-RN-02 | The expiring population is the booked root invoices in BIBS; one candidate per root invoice and policy year | BRRN.005, RQ06 |
| A-RN-03 | The "unique reference number" is the renewal reference generated by BIBS; the expiring invoice number is the alternate key | BRRN.022, RQ03 |
| A-RN-04 | Online disposition (main BRD) and dispositioned-file upload (BRRN.018) are both needed and write the same history | RQ14 |
| A-RN-05 | Straight-through processing starts only after explicit initiation and only for Clean candidates whose matrix rule says AUTO | BRRN.021, 031, RQ02 |
| A-RN-06 | CBG accounts skip AO assignment and TL review; non-CBG accounts follow the manual path | BRD 1.005, 1.008, RQ08 |
| A-RN-07 | The NRNS checkpoint is 90 days **before** expiry (the BRD also writes "+90 days") | BRRN.025, RQ18 |
| A-RN-08 | The renewal is a new BRD-1 account of business type RENEWAL; the expiring invoice is never endorsed to renew it | Decision D1 |
| A-RN-09 | The payment rules of BRRN.040 are the BRD-1 payment gate | RQ19 |
| A-RN-10 | "Approved domains" for RA e-mails is a recipient policy that BDOI will define; until then any well-formed address is accepted with protection | RQ16 |
| A-RN-11 | Renewal volume is 25,800 transactions a month per process, growing 20% a year | NFR p.186 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-RN-01 | The shared account business-type change BT0 (V822) is merged before the Renewal foundation | FR-RN-048, 063, 084 (D1) |
| D-RN-02 | The Claims module exposes the claims summary (wave CL1-B) | FR-RN-013, 020, 042 (D4) |
| D-RN-03 | Submitted Policies declares `RenewalHandOff` and records hand-offs as pending until Renewal wave R3 | FR-RN-090 (D2) |
| D-RN-04 | BDOI provides the check list, bucket rules and decision matrix content | FR-RN-020, 022, 023 (RQ01, RQ24) |
| D-RN-05 | BDOI provides the letter layouts, insurer and LAMD file layouts and the password convention | FR-RN-025, 071, 080-083 (RQ15, RQ20, RQ26, Q07) |
| D-RN-06 | The e-mail relay of the BIBS environment is available for client and insurer e-mails | FR-RN-070, 081 |
| D-RN-07 | The Operations ledger carries the business type of booked invoices for ACSL (requested of the Operations owner) | Section 7 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-6 (status from the cross-BRD decisions, R6)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| RQ01 | List of sanitation, matching and eligibility checks and, for each, Review or Exception on failure | FR-RN-020, 022 | OPEN |
| RQ02 | Segments initiated in bulk and individually; who initiates; is CBG STP initiated automatically | FR-RN-015 | OPEN |
| RQ03 | Meaning of the "unique reference number"; key carried by insurer and LAMD files | FR-RN-021 | OPEN |
| RQ04 | Lead days per segment or line; daily or monthly extraction; on-demand ranges | FR-RN-010, 011 | OPEN |
| RQ05 | One scrollable view without pagination against the UX pager rule (UX-6) | FR-RN-013 | OPEN |
| RQ06 | Meaning of "unbooked" accounts in the expiring list | FR-RN-010 | OPEN |
| RQ07 | NFR sent automatically on the system tag; NFR after the "not in file" tag; NFR vs NRL | FR-RN-024, 060 | OPEN |
| RQ08 | Definition of CBG and non-CBG; does CBG skip assignment and review | FR-RN-025, 030 | OPEN |
| RQ09 | Threshold and scope of the outstanding-balance flag; who overrides | FR-RN-051 | OPEN |
| RQ10 | Definition of the 34 summary counters, including For ARF, Renew to TSU, Disapproved, to Other Bank | FR-RN-101 | OPEN |
| RQ11 | IBG and Leasing escalation thresholds, recipients and "high-risk renewals" | FR-RN-102 | OPEN |
| RQ12 | What unlocks an account after RA generation; may Processing still edit | FR-RN-080 | OPEN |
| RQ13 | Claims summary sufficiency; total-loss flag (CLQ28); legacy history | FR-RN-042, 043 | OPEN |
| RQ14 | Online vs offline disposition; file layout; scope of the "not in file" tag | FR-RN-060 | OPEN |
| RQ15 | Insurer response file layout, codes, late cut-off; Reject followed by re-marketing or Not for Renewal | FR-RN-070, 071 | OPEN |
| RQ16 | RA recipients and "approved domains"; TLS on the BDO relay | FR-RN-081 | OPEN |
| RQ17 | Which accounts are NAL-eligible | FR-RN-082 | OPEN |
| RQ18 | Definition of "not yet submitted"; checkpoint; timing and recipients of non-acceptance letters | FR-RN-083 | OPEN |
| RQ19 | Approved acceptance inputs; payment rules; renewal e-policy | FR-RN-084 | OPEN |
| RQ20 | LAMD organisation, log-in, report layouts and frequency (XQ01: one or two LAMD intakes) | FR-RN-025 | OPEN |
| RQ21 | Contact Center lists, statuses and outcome codes (XQ02) | FR-RN-085 | OPEN |
| RQ22 | Source of KYC data for bank clients | FR-RN-026 | OPEN |
| RQ23 | Paid-off loans: Not for Renewal or non-mortgaged renewal; RMU tag or routing | FR-RN-025 | OPEN |
| RQ24 | Initial decision-matrix content; who maintains and approves it | FR-RN-023 | OPEN |
| RQ25 | Financial-impact tolerance; NB path or explicit acceptance for revised premiums | FR-RN-048, 064 | OPEN |
| RQ26 | Layouts of the RA, NAL, NFR, reminder, non-acceptance and insurer cover letters | FR-RN-080-083, 111 | OPEN |
| RQ27 | One-time load of expiring legacy EBIX / QPS policies at go-live | Section 7 | OPEN |
| RQ28 | Availability, maintenance, DR, retention and backup values ("follow QPS") | Section 8 | OPEN |
| RQ29 | Late Renewal Requests Report (BRNB.018) as a listing variant (XQ09) | FR-RN-102 | OPEN |
| RQ30 | Renewal of an account whose package version is expired with no current version | FR-RN-063 | OPEN |
| SP SQ10 | Renewal of submitted policies | FR-RN-090 | ANSWERED |
| OOS-1 | Renewal overrides deferred by BRD-1 | FR-RN-051 | ANSWERED |

# Traceability

Every BRD-6 requirement ID is met by at least one FR or is out of scope by the BRD itself. Section 11.1 traces the 40 BRRN IDs of the addenda; section 11.2 traces every one of the 1,032 line IDs of the main BRD with its page; section 11.3 lists the nine out-of-scope IDs.

<!-- table: widths=2.4,10,4.2 caption="Traceability summary" -->
| Source | IDs | Traced to |
|---|---|---|
| Workshop addendum BRRN.020-040 | 21 | Section 11.1 |
| Addendum 1 BRRN.001-019 | 19 (with the main-BRD IDs they restate) | Section 11.1 |
| Main BRD 1.001-6.002 | 1,032 line IDs, of which 9 out of scope | Section 11.2 |
| Usage requirements (NFR) p.186-188 | 10 topics | Section 8 |

## BRRN requirements

<!-- table: widths=2,3.4,5.6,5.6 caption="BRRN ID to FR, screen and API" size=8 -->
| BRD ID | FR | Screen | API / job |
|---|---|---|---|
| BRRN.001 (p.22) | FR-RN-082 | Letters (NAL) | /renewal/letters/nal |
| BRRN.002 (p.22-23) | FR-RN-011 | Expiry List (Generate Expiry List) | /renewal/candidates?expiryFrom=&expiryTo= |
| BRRN.003 (p.23) | FR-RN-012 | Expiry List (Filters) | /renewal/candidates (filters) |
| BRRN.004 (p.23-24) | FR-RN-013 | Expiry List grid | /renewal/candidates |
| BRRN.005 (p.24-25) | FR-RN-010 | Expiry List; Renewal Home | Job RNW_EXTRACTION |
| BRRN.006 (p.25) | FR-RN-013 | Expiry List (column filters, search) | /renewal/candidates |
| BRRN.007 (p.25-26) | FR-RN-014 | Expiry List (Export) | Report RNW-EXPIRY-LIST |
| BRRN.008 (p.26) | FR-RN-014 | Expiry List (Print) | Report RNW-EXPIRY-LIST |
| BRRN.009 (p.26-27) | FR-RN-024 | Renewal Setup; Letters (NFR) | /renewal/setup/non-renewable-risk-codes |
| BRRN.010 (p.27-28) | FR-RN-080, FR-RN-081 | Letters | /renewal/letters/ra; /renewal/letters/send |
| BRRN.011 (p.28-29) | FR-RN-040 | My Dispositions | /renewal/candidates?assignee=me |
| BRRN.012 (p.29) | FR-RN-040 | My Dispositions | /renewal/candidates |
| BRRN.013 (p.29-30) | FR-RN-041 | Record page | /renewal/candidates/{ref} |
| BRRN.014 (p.30) | FR-RN-040 | My Dispositions (column filters) | /renewal/candidates |
| BRRN.015 (p.30) | FR-RN-040 | My Dispositions (sort) | /renewal/candidates |
| BRRN.016 (p.30-31) | FR-RN-040 | My Dispositions (search) | /renewal/candidates |
| BRRN.017 (p.31) | FR-RN-040 | My Dispositions (name / reference search) | /renewal/candidates |
| BRRN.018 (p.31) | FR-RN-060 | Processing Worklist (upload) | Bulk RNW_DISPOSITION_UPLOAD |
| BRRN.019 (p.32-33) | FR-RN-101, FR-RN-103 | Renewal Reports | Report RNW-STATUS |
| BRRN.020 (p.4) | FR-RN-020 | Record page (Checks & Bucket) | Check engine; RNW-SANITATION |
| BRRN.021 (p.4) | FR-RN-015, FR-RN-004 | Expiry List (Initiate) | /renewal/candidates/initiate |
| BRRN.022 (p.4) | FR-RN-021, FR-RN-020 | Record page; uploads | Check REFERENCE_MATCH |
| BRRN.023 (p.4-6) | FR-RN-022, FR-RN-051, FR-RN-112 | Expiry List (pill, Exceptions); record page | /renewal/candidates/{ref}/bucket-override |
| BRRN.024 (p.6) | FR-RN-002, FR-RN-025 | LAMD Reports | Role LAMD |
| BRRN.025 (p.6) | FR-RN-083 | Letters (NRNS) | Job RNW_NRNS_LETTERS |
| BRRN.026 (p.6-7) | FR-RN-085 | Follow-ups | /renewal/candidates/{ref}/followups |
| BRRN.027 (p.7) | FR-RN-042 | Record page (Account History) | /renewal/candidates/{ref}/account-history |
| BRRN.028 (p.7-8) | FR-RN-026 | Expiry List (KYC chip) | Check KYC_DUE |
| BRRN.029 (p.8) | FR-RN-025 | LAMD Reports | Bulk RNW_LAMD_REPORT |
| BRRN.030 (p.8-9) | FR-RN-010, FR-RN-112 | Renewal Home; Renewal Setup | Job RNW_EXTRACTION |
| BRRN.031 (p.9-10) | FR-RN-023, FR-RN-051 | Record page; Renewal Setup | Decision matrix |
| BRRN.032 (p.10) | FR-RN-027 | Record page (Checks, Account History) | Check ENDORSEMENT_PENDING |
| BRRN.033 (p.10) | FR-RN-048 | Record page (Start NB Path) | /renewal/candidates/{ref}/nb-path |
| BRRN.034 (p.11) | FR-RN-023, FR-RN-112 | Renewal Setup (Decision Matrix) | /renewal/setup/decision-matrices |
| BRRN.035 (p.11-12) | FR-RN-071, FR-RN-051 | Insurer Batches (responses) | Bulk RNW_INSURER_RESPONSE |
| BRRN.036 (p.12-13) | FR-RN-102 | Renewal Reports (RNW-LISTING) | Alert RNW_RENEWAL_AT_RISK |
| BRRN.037 (p.13) | FR-RN-083 | Letters (NRNS) | Jobs RNW_NRNS_LETTERS, RNW_EXPIRY_SWEEP |
| BRRN.038 (p.14) | FR-RN-048, FR-RN-064, FR-RN-084 | Record page (Computations, Acceptance) | Check FINANCIAL_IMPACT |
| BRRN.039 (p.14) | FR-RN-025, FR-RN-023 | LAMD Reports | Check PN_PRESENT, LAMD_STATUS |
| BRRN.040 (p.14-15) | FR-RN-084 | Record page (Record Acceptance) | /renewal/candidates/{ref}/acceptance |

API paths start with `/api/v1`. They are the paths of the build design (R5) and are confirmed at build.

## Main BRD line IDs

The table lists every line ID of the main BRD in page order, grouped by BRD function. Where a BRRN restates the ID, the FR is the one of that BRRN (section 11.1). The label numbers printed next to some IDs in the BRD are shifted (for example 1.003.4.1.1.21-24 all read "1.21", p.50); the table uses the IDs of the BR ID column.

<!-- table: widths=3.3,8.4,1.7,3.2 caption="Main BRD line IDs to FR (every line ID of pages 44-185)" size=7.5 -->
| BRD function | Line IDs | Pages | FR |
|---|---|---|---|
| 1.001 Log in | 1.001.1, 1.001.1.1, 1.001.2.1, 1.001.3.1 | p.44 | FR-RN-001 |
| 1.002 Open in several tabs | 1.002.1 | p.44 | FR-RN-001 |
| 1.003 Generate the list of expiring accounts | 1.003.1, 1.003.1.1, 1.003.2 | p.44-45 | FR-RN-011 |
|  | 1.003.3, 1.003.3.1, 1.003.3.1.2, 1.003.3.1.3, 1.003.3.1.4, 1.003.3.1.5, 1.003.3.1.6, 1.003.3.1.7, 1.003.3.1.8, 1.003.3.1.9, 1.003.3.1.10, 1.003.3.1.11, 1.003.3.1.12, 1.003.3.1.13 | p.45-47 | FR-RN-012 |
|  | 1.003.4.1, 1.003.4.1.1 | p.47 | FR-RN-013 |
|  | 1.003.4.1.1.1 | p.47 | OUT (p.34) |
|  | 1.003.4.1.1.2 | p.47 | FR-RN-013 |
|  | 1.003.4.1.1.3 | p.47 | OUT (p.34) |
|  | 1.003.4.1.1.4, 1.003.4.1.1.5, 1.003.4.1.1.6, 1.003.4.1.1.7, 1.003.4.1.1.8, 1.003.4.1.1.9, 1.003.4.1.1.10, 1.003.4.1.1.11, 1.003.4.1.1.12, 1.003.4.1.1.13, 1.003.4.1.1.14, 1.003.4.1.1.15, 1.003.4.1.1.16, 1.003.4.1.1.17, 1.003.4.1.1.18, 1.003.4.1.1.19, 1.003.4.1.1.20, 1.003.4.1.1.21, 1.003.4.1.1.22, 1.003.4.1.1.23, 1.003.4.1.1.24 | p.48-50 | FR-RN-013 |
|  | 1.003.4.1.2 | p.50 | FR-RN-010 |
|  | 1.003.4.1.3, 1.003.4.1.4 | p.50 | FR-RN-013 |
|  | 1.003.5, 1.003.6, 1.003.7, 1.003.7.1, 1.003.7.2 | p.50-51 | FR-RN-014 |
|  | 1.003.7.3, 1.003.7.3.1 | p.51 | FR-RN-024 |
| 1.004 View account | 1.004.1, 1.004.1.1, 1.004.1.2, 1.004.1.3, 1.004.1.4, 1.004.1.5, 1.004.1.5.1, 1.004.1.5.2 | p.51-52 | FR-RN-041 |
| 1.005 Assign accounts | 1.005.1, 1.005.1.1, 1.005.1.1.1, 1.005.1.1.2, 1.005.1.2, 1.005.1.2.1, 1.005.1.2.2, 1.005.1.2.3, 1.005.1.2.4, 1.005.2 | p.53-54 | FR-RN-030 |
| 1.006 Transfer account to other units | 1.006.1, 1.006.2, 1.006.3, 1.006.4, 1.006.5 | p.54-55 | FR-RN-031 |
| 1.007 Receive account transfer | 1.007.1, 1.007.2, 1.007.3, 1.007.3.1, 1.007.4, 1.007.4.1, 1.007.4.2, 1.007.5 | p.55-56 | FR-RN-032 |
| 1.008 Review and post | 1.008.1, 1.008.1.1 | p.56 | FR-RN-050 |
|  | 1.008.1.2, 1.008.1.3 | p.56 | FR-RN-050, FR-RN-041 |
|  | 1.008.1.4 | p.56 | FR-RN-050, FR-RN-027 |
|  | 1.008.2, 1.008.2.1, 1.008.2.2, 1.008.2.3, 1.008.2.4, 1.008.2.5, 1.008.2.6, 1.008.2.7, 1.008.3, 1.008.3.1, 1.008.3.2, 1.008.3.3, 1.008.3.4 | p.56-58 | FR-RN-050 |
| 1.009 Generate report | 1.009.1, 1.009.1.1, 1.009.1.2, 1.009.2, 1.009.2.1, 1.009.2.2, 1.009.2.3, 1.009.2.4, 1.009.2.5, 1.009.2.6, 1.009.2.7, 1.009.2.8, 1.009.2.9, 1.009.2.10, 1.009.2.11, 1.009.2.12, 1.009.2.13, 1.009.2.14, 1.009.2.15, 1.009.2.16, 1.009.2.17, 1.009.2.18, 1.009.2.19, 1.009.2.20, 1.009.2.21, 1.009.2.22, 1.009.2.23, 1.009.2.24, 1.009.2.25, 1.009.2.26, 1.009.2.27, 1.009.2.28, 1.009.2.29, 1.009.2.30, 1.009.2.31, 1.009.2.32, 1.009.2.33, 1.009.2.34, 1.009.2.35, 1.009.2.36, 1.009.2.37, 1.009.2.38, 1.009.2.39, 1.009.2.40, 1.009.3, 1.009.3.1, 1.009.3.1.1, 1.009.3.1.2, 1.009.3.1.3, 1.009.3.1.4, 1.009.3.1.5, 1.009.3.1.6, 1.009.3.1.7, 1.009.3.1.8, 1.009.3.1.9, 1.009.3.1.10, 1.009.3.1.11, 1.009.3.1.12, 1.009.3.1.13, 1.009.3.1.14, 1.009.3.1.15, 1.009.3.1.16, 1.009.3.1.17, 1.009.3.1.18, 1.009.3.1.19, 1.009.3.1.20, 1.009.3.1.21, 1.009.3.1.22, 1.009.3.1.23, 1.009.3.1.24, 1.009.3.1.25, 1.009.3.1.26, 1.009.3.1.27, 1.009.3.1.28, 1.009.3.1.29, 1.009.3.1.30, 1.009.3.1.31, 1.009.3.1.32, 1.009.3.1.33, 1.009.3.1.34, 1.009.3.1.35, 1.009.3.1.36, 1.009.3.1.37, 1.009.3.2, 1.009.3.2.1, 1.009.3.2.2, 1.009.3.2.3, 1.009.3.2.4, 1.009.3.2.5, 1.009.3.2.6, 1.009.3.2.7, 1.009.3.2.8, 1.009.3.2.9, 1.009.3.2.10, 1.009.3.2.11, 1.009.3.2.12, 1.009.3.2.13, 1.009.3.2.14, 1.009.3.2.15, 1.009.3.2.16, 1.009.3.2.17, 1.009.3.2.18, 1.009.3.2.19, 1.009.3.2.20, 1.009.3.2.21, 1.009.3.2.22, 1.009.3.2.23, 1.009.3.2.24, 1.009.3.2.25, 1.009.3.2.26, 1.009.3.2.27, 1.009.3.2.28, 1.009.3.2.29, 1.009.3.2.30, 1.009.3.2.31, 1.009.3.2.32, 1.009.3.2.33, 1.009.3.2.34, 1.009.4, 1.009.4.1, 1.009.4.2, 1.009.5, 1.009.5.1, 1.009.5.2, 1.009.6 | p.58-75 | FR-RN-101 |
|  | 1.009.7 | p.75 | FR-RN-100 |
| 1.010 Generate Renewal Advice | 1.010.1, 1.010.2, 1.010.2.1, 1.010.2.2, 1.010.3, 1.010.3.1, 1.010.3.2, 1.010.3.3, 1.010.4, 1.010.4.1, 1.010.4.2, 1.010.5, 1.010.5.1, 1.010.5.2 | p.76-77 | FR-RN-080 |
|  | 1.010.6, 1.010.7 | p.77 | FR-RN-081 |
| 1.011 Override accounts | 1.011.1, 1.011.1.1, 1.011.1.2, 1.011.1.3 | p.78 | FR-RN-051 |
| 2.001 Log in | 2.001.1, 2.001.1.1, 2.001.1.2, 2.001.1.3 | p.78-79 | FR-RN-001 |
| 2.002 Open in several tabs | 2.002.1 | p.79 | FR-RN-001 |
| 2.003 View accounts for disposition | 2.003.1, 2.003.1.1 | p.79 | FR-RN-040 |
|  | 2.003.1.2 | p.79 | FR-RN-040, FR-RN-041 |
|  | 2.003.1.3, 2.003.1.4, 2.003.1.5 | p.79-80 | FR-RN-040 |
|  | 2.003.1.6 | p.80 | OUT (p.34) |
|  | 2.003.2, 2.003.3, 2.003.3.1, 2.003.3.2 | p.80 | FR-RN-041 |
| 2.004 Provide disposition | 2.004.1, 2.004.2, 2.004.3, 2.004.3.1, 2.004.3.2, 2.004.3.3, 2.004.3.4, 2.004.3.5, 2.004.4, 2.004.4.1, 2.004.4.2, 2.004.4.3, 2.004.4.4, 2.004.4.5, 2.004.4.6, 2.004.4.7 | p.80-83 | FR-RN-043 |
|  | 2.004.4.8 | p.83 | FR-RN-043, FR-RN-031 |
|  | 2.004.4.9, 2.004.4.10, 2.004.5, 2.004.5.1, 2.004.5.2, 2.004.5.3 | p.83-84 | FR-RN-043 |
|  | 2.004.6, 2.004.7, 2.004.8 | p.84 | FR-RN-044 |
|  | 2.004.9 | p.84 | FR-RN-080 |
|  | 2.004.10 | p.84 | FR-RN-047 |
| 2.005 Transfer account to other units | 2.005.1, 2.005.2, 2.005.3, 2.005.3.1, 2.005.3.2, 2.005.4, 2.005.5 | p.84-85 | FR-RN-031 |
| 2.006 View the list of dispositioned accounts | 2.006.1, 2.006.1.1, 2.006.1.1.1, 2.006.1.1.2 | p.85-86 | FR-RN-045 |
|  | 2.006.1.2 | p.86 | OUT (p.34) |
|  | 2.006.1.3, 2.006.1.4 | p.86 | FR-RN-045 |
|  | 2.006.1.5, 2.006.1.5.1, 2.006.1.5.2, 2.006.1.5.3, 2.006.1.6 | p.86-87 | FR-RN-044 |
| 2.007 Update the returned accounts | 2.007.1, 2.007.2, 2.007.3, 2.007.4 | p.87 | FR-RN-046 |
|  | 2.007.4.1 | p.88 | OUT (p.34) |
|  | 2.007.4.2, 2.007.4.3, 2.007.4.4, 2.007.4.5, 2.007.4.6, 2.007.4.7, 2.007.5 | p.88 | FR-RN-046 |
| 2.008 Generate report | 2.008.1, 2.008.1.1, 2.008.1.2, 2.008.2, 2.008.2.1, 2.008.2.2, 2.008.2.3, 2.008.2.4, 2.008.2.5, 2.008.2.6, 2.008.2.7, 2.008.2.8, 2.008.2.9, 2.008.2.10, 2.008.2.11, 2.008.2.12, 2.008.2.13, 2.008.2.14, 2.008.2.15, 2.008.2.16, 2.008.2.17, 2.008.2.18, 2.008.2.19, 2.008.2.20, 2.008.2.21, 2.008.2.22, 2.008.2.23, 2.008.2.24, 2.008.2.25, 2.008.2.26, 2.008.2.27, 2.008.2.28, 2.008.2.29, 2.008.2.30, 2.008.2.31, 2.008.2.32, 2.008.2.33, 2.008.2.34, 2.008.2.35, 2.008.2.36, 2.008.2.37, 2.008.2.38, 2.008.2.39, 2.008.2.40, 2.008.3, 2.008.3.1, 2.008.3.1.1, 2.008.3.1.2, 2.008.3.1.3, 2.008.3.1.4, 2.008.3.1.5, 2.008.3.1.6, 2.008.3.1.7, 2.008.3.1.8, 2.008.3.1.9, 2.008.3.1.10, 2.008.3.1.11, 2.008.3.1.12, 2.008.3.1.13, 2.008.3.1.14, 2.008.3.1.15, 2.008.3.1.16, 2.008.3.1.17, 2.008.3.1.18, 2.008.3.1.19, 2.008.3.1.20, 2.008.3.1.21, 2.008.3.1.22, 2.008.3.1.23, 2.008.3.1.24, 2.008.3.1.25, 2.008.3.1.26, 2.008.3.1.27, 2.008.3.1.28, 2.008.3.1.29, 2.008.3.1.30, 2.008.3.1.31, 2.008.3.1.32, 2.008.3.1.33, 2.008.3.1.34, 2.008.3.1.35, 2.008.3.1.36, 2.008.3.1.37, 2.008.3.2, 2.008.3.2.1, 2.008.3.2.2, 2.008.3.2.3, 2.008.3.2.4, 2.008.3.2.5, 2.008.3.2.6, 2.008.3.2.7, 2.008.3.2.8, 2.008.3.2.9, 2.008.3.2.10, 2.008.3.2.11, 2.008.3.2.12, 2.008.3.2.13, 2.008.3.2.14, 2.008.3.2.15, 2.008.3.2.16, 2.008.3.2.17, 2.008.3.2.18, 2.008.3.2.19, 2.008.3.2.20, 2.008.3.2.21, 2.008.3.2.22, 2.008.3.2.23, 2.008.3.2.24, 2.008.3.2.25, 2.008.3.2.26, 2.008.3.2.27, 2.008.3.2.28, 2.008.3.2.29, 2.008.3.2.30, 2.008.3.2.31, 2.008.3.2.32, 2.008.3.2.33, 2.008.3.2.34, 2.008.4, 2.008.4.1, 2.008.4.2, 2.008.5, 2.008.5.1, 2.008.5.2, 2.008.6 | p.89-106 | FR-RN-101 |
| 2.009 Generate Renewal Advice | 2.009.1, 2.009.2, 2.009.2.1, 2.009.2.2, 2.009.3, 2.009.3.1, 2.009.3.2, 2.009.3.3, 2.009.4, 2.009.4.1, 2.009.4.2, 2.009.5, 2.009.5.1, 2.009.5.2 | p.106-107 | FR-RN-080 |
|  | 2.009.6, 2.009.7, 2.009.7.1 | p.108 | FR-RN-081 |
| 3.001 Log in | 3.001.1, 3.001.1.1, 3.001.1.2, 3.001.1.3 | p.108 | FR-RN-001 |
| 3.002 Open in several tabs | 3.002.1 | p.108 | FR-RN-001 |
| 3.003 Generate list of accounts for processing | 3.003.1, 3.003.1.1, 3.003.2 | p.109 | FR-RN-011 |
|  | 3.003.3, 3.003.3.1, 3.003.3.2, 3.003.3.3, 3.003.3.4, 3.003.3.5, 3.003.3.6, 3.003.3.7, 3.003.3.8, 3.003.3.9, 3.003.3.10, 3.003.3.11, 3.003.3.12, 3.003.3.13 | p.109-111 | FR-RN-012 |
|  | 3.003.4, 3.003.4.1 | p.111 | FR-RN-013 |
|  | 3.003.4.1.1 | p.111 | OUT (p.34) |
|  | 3.003.4.1.2 | p.112 | FR-RN-013 |
|  | 3.003.4.1.3 | p.112 | OUT (p.34) |
|  | 3.003.4.1.4, 3.003.4.1.5, 3.003.4.1.6, 3.003.4.1.7, 3.003.4.1.8, 3.003.4.1.9, 3.003.4.1.10, 3.003.4.1.11, 3.003.4.1.12, 3.003.4.1.13, 3.003.4.1.14, 3.003.4.1.15, 3.003.4.1.16, 3.003.4.1.17, 3.003.4.1.18, 3.003.4.1.19, 3.003.4.1.20, 3.003.4.1.21 | p.112-114 | FR-RN-013 |
|  | 3.003.4.2 | p.114 | FR-RN-010 |
|  | 3.003.4.3, 3.003.4.4 | p.114 | FR-RN-013 |
|  | 3.003.4.5 | p.115 | OUT (p.34) |
|  | 3.003.5, 3.003.6, 3.003.7, 3.003.7.1, 3.003.7.2 | p.115 | FR-RN-014 |
| 3.004 Upload dispositioned file | 3.004.1, 3.004.1.1, 3.004.1.2, 3.004.2, 3.004.3, 3.004.3.1, 3.004.4 | p.115-116 | FR-RN-060 |
|  | 3.004.5 | p.117 | FR-RN-047 |
| 3.005 Assign accounts | 3.005.1, 3.005.1.1, 3.005.1.1.1, 3.005.1.1.2, 3.005.1.2, 3.005.1.2.1, 3.005.1.2.2, 3.005.1.3, 3.005.1.4, 3.005.1.5, 3.005.1.6 | p.117-118 | FR-RN-061 |
| 3.006 View accounts for processing | 3.006.1 | p.118 | FR-RN-062 |
|  | 3.006.1.1, 3.006.1.2 | p.118 | FR-RN-062, FR-RN-041 |
|  | 3.006.1.3, 3.006.1.4 | p.119 | FR-RN-062 |
|  | 3.006.1.5 | p.119 | FR-RN-013 |
|  | 3.006.1.6 | p.119 | FR-RN-062 |
|  | 3.006.2, 3.006.3, 3.006.3.1, 3.006.3.2 | p.119 | FR-RN-041 |
| 3.007 Update data on an account | 3.007.1, 3.007.2, 3.007.3, 3.007.3.1, 3.007.3.2, 3.007.3.3, 3.007.3.4 | p.120 | FR-RN-063 |
| 3.008 Review computations; return | 3.008.1, 3.008.1.1 | p.121 | FR-RN-064 |
|  | 3.008.2, 3.008.2.1, 3.008.2.2, 3.008.2.3, 3.008.2.4, 3.008.2.5, 3.008.2.6, 3.008.2.7 | p.121-122 | FR-RN-065 |
| 3.009 Extract accounts per insurer | 3.009.1, 3.009.1.1, 3.009.1.2, 3.009.1.3, 3.009.1.4, 3.009.1.4.1, 3.009.1.4.2, 3.009.1.4.3, 3.009.1.4.4, 3.009.1.4.5, 3.009.1.4.6, 3.009.1.4.7, 3.009.1.4.8, 3.009.1.4.9, 3.009.1.4.10, 3.009.1.4.11, 3.009.1.4.12, 3.009.1.4.13, 3.009.1.4.14, 3.009.1.4.15, 3.009.1.4.16, 3.009.1.4.17, 3.009.1.4.18, 3.009.1.4.19, 3.009.1.4.20, 3.009.1.4.21, 3.009.1.4.22, 3.009.1.4.23, 3.009.1.4.24, 3.009.1.4.25, 3.009.1.4.26, 3.009.1.4.27, 3.009.1.4.28 | p.122-126 | FR-RN-070 |
|  | 3.009.2, 3.009.3, 3.009.4, 3.009.4.1, 3.009.4.2 | p.126-127 | FR-RN-070, FR-RN-103 |
|  | 3.009.5, 3.009.5.1 | p.127 | FR-RN-070 |
|  | 3.009.6, 3.009.6.1, 3.009.6.2 | p.127 | FR-RN-071 |
| 3.010 Generate report | 3.010.1, 3.010.1.1, 3.010.1.2, 3.010.2, 3.010.2.1, 3.010.2.2, 3.010.2.3, 3.010.2.4, 3.010.2.5, 3.010.2.6, 3.010.2.7, 3.010.2.8, 3.010.2.9, 3.010.2.10, 3.010.2.11, 3.010.2.12, 3.010.2.13, 3.010.2.14, 3.010.2.15, 3.010.2.16, 3.010.2.17, 3.010.2.18, 3.010.2.19, 3.010.2.20, 3.010.2.21, 3.010.2.22, 3.010.2.23, 3.010.2.24, 3.010.2.25, 3.010.2.26, 3.010.2.27, 3.010.2.28, 3.010.2.29, 3.010.2.30, 3.010.2.31, 3.010.2.32, 3.010.2.33, 3.010.2.34, 3.010.2.35, 3.010.2.36, 3.010.2.37, 3.010.2.38, 3.010.2.39, 3.010.2.40, 3.010.3, 3.010.3.1, 3.010.3.1.1, 3.010.3.1.2, 3.010.3.1.3, 3.010.3.1.4, 3.010.3.1.5, 3.010.3.1.6, 3.010.3.1.7, 3.010.3.1.8, 3.010.3.1.9, 3.010.3.1.10, 3.010.3.1.11, 3.010.3.1.12, 3.010.3.1.13, 3.010.3.1.14, 3.010.3.1.15, 3.010.3.1.16, 3.010.3.1.17, 3.010.3.1.18, 3.010.3.1.19, 3.010.3.1.20, 3.010.3.1.21, 3.010.3.1.22, 3.010.3.1.23, 3.010.3.1.24, 3.010.3.1.25, 3.010.3.1.26, 3.010.3.1.27, 3.010.3.1.28, 3.010.3.1.29, 3.010.3.1.30, 3.010.3.1.31, 3.010.3.1.32, 3.010.3.1.33, 3.010.3.1.34, 3.010.3.1.35, 3.010.3.1.36, 3.010.3.1.37, 3.010.3.1.38, 3.010.3.1.39, 3.010.3.1.40, 3.010.3.2, 3.010.3.2.1, 3.010.3.2.2, 3.010.3.2.3, 3.010.3.2.4, 3.010.3.2.5, 3.010.3.2.6, 3.010.3.2.7, 3.010.3.2.8, 3.010.3.2.9, 3.010.3.2.10, 3.010.3.2.11, 3.010.3.2.12, 3.010.3.2.13, 3.010.3.2.14, 3.010.3.2.15, 3.010.3.2.16, 3.010.3.2.17, 3.010.3.2.18, 3.010.3.2.19, 3.010.3.2.20, 3.010.3.2.21, 3.010.3.2.22, 3.010.3.2.23, 3.010.3.2.24, 3.010.3.2.25, 3.010.3.2.26, 3.010.3.2.27, 3.010.3.2.28, 3.010.3.2.29, 3.010.3.2.30, 3.010.3.2.31, 3.010.3.2.32, 3.010.3.2.33, 3.010.3.2.34, 3.010.4, 3.010.4.1, 3.010.4.2, 3.010.5, 3.010.5.1, 3.010.5.2, 3.010.6 | p.127-145 | FR-RN-101 |
|  | 3.010.7 | p.145 | FR-RN-100 |
| 3.011 Generate Renewal Advice | 3.011.1, 3.011.2, 3.011.2.1, 3.011.2.2, 3.011.3, 3.011.3.1, 3.011.3.2, 3.011.3.3, 3.011.4, 3.011.4.1, 3.011.4.2, 3.011.5, 3.011.5.1, 3.011.5.2 | p.145-147 | FR-RN-080 |
|  | 3.011.6, 3.011.7, 3.011.7.1 | p.147 | FR-RN-081 |
| 4.001 Log in | 4.001.1, 4.001.1.1, 4.001.1.2, 4.001.1.3 | p.148 | FR-RN-001 |
| 4.002 Open in several tabs | 4.002.1 | p.148 | FR-RN-001 |
| 4.003 View accounts for processing | 4.003.1 | p.148 | FR-RN-062 |
|  | 4.003.1.1, 4.003.1.2 | p.149 | FR-RN-062, FR-RN-041 |
|  | 4.003.1.3, 4.003.1.4 | p.149 | FR-RN-062 |
|  | 4.003.1.5 | p.149 | FR-RN-013 |
|  | 4.003.1.6 | p.149 | OUT (p.34) |
|  | 4.003.2, 4.003.3, 4.003.3.1, 4.003.3.2 | p.150 | FR-RN-041 |
| 4.004 Upload dispositioned file | 4.004.1, 4.004.1.1, 4.004.1.2, 4.004.2, 4.004.3, 4.004.3.1 | p.150-151 | FR-RN-060 |
| 4.005 Update data on an account | 4.005.1, 4.005.2, 4.005.3, 4.005.3.1, 4.005.3.2, 4.005.3.3, 4.005.3.4 | p.151-152 | FR-RN-063 |
| 4.006 Review computations; return | 4.006.1, 4.006.1.1 | p.152 | FR-RN-064 |
|  | 4.006.2, 4.006.2.1, 4.006.2.2, 4.006.2.3, 4.006.2.4, 4.006.2.5, 4.006.2.6, 4.006.2.7 | p.152-153 | FR-RN-065 |
| 4.007 Extract accounts per insurer | 4.007.1, 4.007.1.1, 4.007.1.2, 4.007.1.3, 4.007.1.4, 4.007.1.4.1, 4.007.1.4.2, 4.007.1.4.3, 4.007.1.4.4, 4.007.1.4.5, 4.007.1.4.6, 4.007.1.4.7, 4.007.1.4.8, 4.007.1.4.9, 4.007.1.4.10, 4.007.1.4.11, 4.007.1.4.12, 4.007.1.4.13, 4.007.1.4.14, 4.007.1.4.15, 4.007.1.4.16, 4.007.1.4.17, 4.007.1.4.18, 4.007.1.4.19, 4.007.1.4.20, 4.007.1.4.21, 4.007.1.4.22, 4.007.1.4.23, 4.007.1.4.24, 4.007.1.4.25, 4.007.1.4.26, 4.007.1.4.27, 4.007.1.4.28 | p.153-157 | FR-RN-070 |
|  | 4.007.2, 4.007.3, 4.007.4, 4.007.4.1, 4.007.4.2 | p.158 | FR-RN-070, FR-RN-103 |
|  | 4.007.5, 4.007.5.1 | p.158 | FR-RN-070 |
|  | 4.007.6, 4.007.6.1, 4.007.6.2 | p.159 | FR-RN-071 |
| 4.008 Generate report | 4.008.1, 4.008.1.1, 4.008.1.2, 4.008.2, 4.008.2.1, 4.008.2.2, 4.008.2.3, 4.008.2.4, 4.008.2.5, 4.008.2.6, 4.008.2.7, 4.008.2.8, 4.008.2.9, 4.008.2.10, 4.008.2.11, 4.008.2.12, 4.008.2.13, 4.008.2.14, 4.008.2.15, 4.008.2.16, 4.008.2.17, 4.008.2.18, 4.008.2.19, 4.008.2.20, 4.008.2.21, 4.008.2.22, 4.008.2.23, 4.008.2.24, 4.008.2.25, 4.008.2.26, 4.008.2.27, 4.008.2.28, 4.008.2.29, 4.008.2.30, 4.008.2.31, 4.008.2.32, 4.008.2.33, 4.008.2.34, 4.008.2.35, 4.008.2.36, 4.008.2.37, 4.008.2.38, 4.008.2.39, 4.008.2.40, 4.008.3, 4.008.3.1, 4.008.3.1.1, 4.008.3.1.2, 4.008.3.1.3, 4.008.3.1.4, 4.008.3.1.5, 4.008.3.1.6, 4.008.3.1.7, 4.008.3.1.8, 4.008.3.1.9, 4.008.3.1.10, 4.008.3.1.11, 4.008.3.1.12, 4.008.3.1.13, 4.008.3.1.14, 4.008.3.1.15, 4.008.3.1.16, 4.008.3.1.17, 4.008.3.1.18, 4.008.3.1.19, 4.008.3.1.20, 4.008.3.1.21, 4.008.3.1.22, 4.008.3.1.23, 4.008.3.1.24, 4.008.3.1.25, 4.008.3.1.26, 4.008.3.1.27, 4.008.3.1.28, 4.008.3.1.29, 4.008.3.1.30, 4.008.3.1.31, 4.008.3.1.32, 4.008.3.1.33, 4.008.3.1.34, 4.008.3.1.35, 4.008.3.1.36, 4.008.3.1.37, 4.008.3.1.38, 4.008.3.1.39, 4.008.3.1.40, 4.008.3.2, 4.008.3.2.1, 4.008.3.2.2, 4.008.3.2.3, 4.008.3.2.4, 4.008.3.2.5, 4.008.3.2.6, 4.008.3.2.7, 4.008.3.2.8, 4.008.3.2.9, 4.008.3.2.10, 4.008.3.2.11, 4.008.3.2.12, 4.008.3.2.13, 4.008.3.2.14, 4.008.3.2.15, 4.008.3.2.16, 4.008.3.2.17, 4.008.3.2.18, 4.008.3.2.19, 4.008.3.2.20, 4.008.3.2.21, 4.008.3.2.22, 4.008.3.2.23, 4.008.3.2.24, 4.008.3.2.25, 4.008.3.2.26, 4.008.3.2.27, 4.008.3.2.28, 4.008.3.2.29, 4.008.3.2.30, 4.008.3.2.31, 4.008.3.2.32, 4.008.3.2.33, 4.008.3.2.34, 4.008.4, 4.008.4.1, 4.008.4.2, 4.008.5, 4.008.5.1, 4.008.5.2, 4.008.6 | p.159-177 | FR-RN-101 |
| 4.09 Generate Renewal Advice | 4.09.1, 4.09.2, 4.09.2.1, 4.09.2.2, 4.09.2.3, 4.09.3, 4.09.3.1, 4.09.3.2, 4.09.4, 4.09.4.1, 4.09.4.2, 4.09.5, 4.09.5.1, 4.09.5.2 | p.177-178 | FR-RN-080 |
|  | 4.09.6, 4.09.7 | p.178-179 | FR-RN-081 |
| 5.001 Log in | 5.001.1, 5.001.1.1, 5.001.1.2, 5.001.1.3 | p.179 | FR-RN-001 |
| 5.002 Open in several tabs | 5.002.1 | p.179 | FR-RN-001 |
| 5.003 Maintain lists of values | 5.003.1, 5.003.2, 5.003.3, 5.003.4, 5.003.5 | p.179-180 | FR-RN-110 |
| 5.004 Maintain users | 5.004.1 | p.180 | FR-RN-003 |
| 5.005 Update the RA template | 5.005.1 | p.180 | FR-RN-111 |
| 6.001 Access the application | 6.001.1, 6.001.1.1 | p.180 | FR-RN-001 |
| 6.002 Manage users, profiles and functions | 6.002.1, 6.002.1.2, 6.002.1.3, 6.002.1.4, 6.002.1.5, 6.002.1.6, 6.002.2, 6.002.2.1, 6.002.2.2, 6.002.2.3, 6.002.2.4, 6.002.2.5, 6.002.2.6, 6.002.2.7, 6.002.2.8, 6.002.2.9, 6.002.2.10, 6.002.2.11, 6.002.2.12, 6.002.2.13, 6.002.2.14, 6.002.2.15, 6.002.2.16, 6.002.2.17, 6.002.2.18, 6.002.2.19, 6.002.2.20, 6.002.2.21, 6.002.2.22 | p.180-185 | FR-RN-003 |
|  | 6.002.2.23 | p.185 | FR-RN-003, FR-RN-110 |
|  | 6.002.2.24 | p.185 | FR-RN-003 |
|  | 6.002.2.25 | p.185 | FR-RN-003, FR-RN-111 |

## Out-of-scope IDs

<!-- table: widths=2.8,7.8,6 caption="IDs out of scope (Addendum 1, p.34)" status=Status size=8.5 -->
| BRD ID | Requirement | Replaced by | Status |
|---|---|---|---|
| 1.003.4.1.1.1 (p.47) | Expiring EBIX Invoice No. column | BIBS expiring invoice no. (FR-RN-013) | OUT |
| 1.003.4.1.1.3 (p.47) | BDOIsys Reference No. column | Renewal reference and ARN (FR-RN-013) | OUT |
| 2.003.1.6 (p.80) | Search via QPS / EBIX reference numbers | Name and BIBS reference search (FR-RN-040) | OUT |
| 2.006.1.2 (p.86) | Search via QPS / EBIX reference numbers | As above | OUT |
| 2.007.4.1 (p.88) | Search via QPS / EBIX reference numbers | As above | OUT |
| 3.003.4.1.1 (p.111) | Expiring EBIX Invoice No. column | BIBS expiring invoice no. (FR-RN-013) | OUT |
| 3.003.4.1.3 (p.112) | BDOIsys Reference No. column | Renewal reference and ARN (FR-RN-013) | OUT |
| 3.003.4.5 (p.115) | Search via QPS / EBIX reference numbers | Name and BIBS reference search (FR-RN-013) | OUT |
| 4.003.1.6 (p.150) | Search via QPS / EBIX reference numbers | Name and BIBS reference search (FR-RN-013) | OUT |

# Sign-off

By signing, BDOI confirms that this FRS describes the Renewal functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Renewal", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank and Corbank", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Head, Corporate and Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
