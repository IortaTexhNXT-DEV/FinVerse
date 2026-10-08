---
# Source of the Functional Requirements Specification for BRD-8 Employee Benefits.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-08_Employee_Benefits/FRS_BRD08_EMPLOYEE_BENEFITS.md
title: Employee Benefits
subtitle: BRD-8 Employee Benefits and Addendum, including the insurer and client portal
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-08
name: Employee Benefits
doc_id: BIBS-FRS-BRD-08
version: "1.1"
date: 08 October 2026
status: Issued for BDOI review
header_title: FRS BRD-8 Employee Benefits
output: FRS/BIBS_FRS_BRD-08_Employee_Benefits_v1.1.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-8 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions D1, D3 and D7
  - version: "1.1"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: "Re-based on the Employee Benefits BRD v1.0 with the Addendum of 23-Feb-2026 (unchanged; page references unchanged); report frequencies and Placement report fields of the Report List of 13-May-2026 applied; review findings applied (CLR-EB-01 to 03 restated, CLR-EB-09 added); user-story view and storyboard index added."
distribution:
  - {name: "Product Owner, Employee Benefits", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Marketing, Employee Benefits teams", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: Processing, role: Business user, organisation: BDOI, purpose: "Review of placement, booking, SOA and member changes"}
  - {name: "Collections and Marketing Support", role: Business user, organisation: BDOI, purpose: Review of billing and payment tracking}
  - {name: "Head, Comptrollership", role: Reviewer, organisation: BDOI, purpose: Review of booking and billing}
  - {name: BDO Information Security, role: Reviewer, organisation: BDO Unibank, purpose: Review of the internet-facing portal}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Employee Benefits (EB) business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to deliver and test the Employee Benefits functions and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

This FRS is written from the BRD and the requirements baseline (R3). It describes the proposed behaviour of the Employee Benefits screens and the partner portal. Where the proposed rule or screen differs from the BRD text, or needs a decision of BDOI, chapter 12 lists each such point for confirmation.

## Scope

The EB desk of Marketing places group benefit programmes (HMO, Group Life Insurance, Group Personal Accident) for corporate clients. The client is the employer, represented by its HR department; the risk is the membership roster. This FRS covers the EB cycle before the account (renewal advice, requirements, Broker on Record, franchise, Terms of Reference, insurer proposals, comparative analysis, approvals, client decision), the hand-over to the BRD-1 placement, issuance and booking, member servicing, billing, monitoring and reports, and the **external portal** through which insurers and client HR users take part.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access, documents and audit | Authorised access to documents and reports, department access classes, protected outbound files, audit and versions | BRID-007, 023, 024, 025 |
| Partner portal | Separate sign-in for insurers and client HR, user provisioning through User Access, staged uploads, review and validation, task inbox, structured proposal form, client HR views and uploads | BRID-005, 005.01-005.03, 014 |
| Programme and renewal start | Client capture, programme and cycle, business type, renewal advice, client feedback, incumbent indicative proposal | BRID-001-003, 006, 022.01 |
| BOR, franchise and TOR | Document register, BOR upload and validation, franchise request and decision, client advice, required documents per process, TOR distribution and remarketing | BRID-004, 007-009, 026, 027, 029 |
| Proposals and decision | Proposal validation, comparative analysis and sign-off, value threshold approval, presentation to the client, revisions, updated proposals, client confirmation and placement trigger | BRID-010-012, 015-017 |
| Processing, billing and servicing | Assignment and returns, issuance tracking, booking with billing number check, SOA intake and release, payment status, member roster and changes, direct billing, monitoring of contracts, cards and billing | BRID-013, 018-021, 025, 030 |
| Reports | Production, Renewal, Placement, New Business, TAT, pending items, franchise; business type filter; Word export | BRID-022, 022.01 |

**Out of scope for this phase:**

- BRID-028, presentation of documents for high-risk accounts. The addendum states that high-risk accounts do not apply to Employee Benefits (Add. p.13).
- System-to-system interfaces with insurers, e-signature verification of the BOR, HRIS feeds of master lists and reading insurer mailboxes. Each waits for a question to BDOI (section 7).
- Renewal of EB programmes by the general Renewal module (BRD-6). EB lines are excluded from the Renewal lists; EB renewals run in this module (decision D3).
- Booking in EBIX (TAT annex). Booking is done in BIBS (BRID-020).

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Employee Benefits Addendum, PDF pages 1-17 (BRID-005.01-005.03, 022.01, 025, 026, 028; volumes; target user matrix) | 23-Feb-2026; approved 26-Feb-2026 |
| R2 | Employee Benefits BRD, PDF pages 18-59 (BRID-001 to 030; usage requirements; current role matrix; TAT annex) | v1.0, 14-Nov-2025; signed 12 to 28-Nov-2025 |
| R3 | BDOI Employee Benefits (BRD-8) requirements baseline | current |
| R5 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | binding |
| R7 | BDO UX guidelines (brand, screen patterns) | current |
| R8 | BDOI - CoreModernization Report List 05132026, Employee Benefits reports (p.27) | 13-May-2026 |

Page references: "Add. p.n" is page n of the addendum (PDF page n). "p.n" is page n of the main BRD, which is PDF page n + 17; for example BRID-001 (p.7) is on PDF page 24. The addendum overrides the main BRD; BRID-025 and BRID-026 are cited with the addendum text.

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing, Employee Benefits)
ARN: Account Reference Number; the BIBS account created from the chosen proposal
BOR: Broker on Record; the client's signed appointment of BDOI as broker (BRID-008)
BRID: Requirement ID prefix of the EB BRD (BRID-001 to 030, 005.01-005.03, 022.01)
Business type: NEW_BUSINESS or RENEWAL, held on the account (shared work item BT0, decision D1)
COG: Named in the TAT annex as a recipient of cards and policies; not defined in the BRD (EBQ21)
Cycle: One EB marketing cycle of a programme for one policy year, typed new business or renewal
EBQnn: Open question on BRD-8 raised by the project team (section 10.3)
FR: Functional requirement of this document (FR-EB-nnn)
Franchise: The insurer's approval for BDOI to market the account to it (accreditation); meaning to confirm (EBQ07)
GLI: Group Life Insurance
GPA: Group Personal Accident
HMO: Health Maintenance Organization
ISACOM: Approval needed when a policy is awarded to a non-accredited provider (Add. p.4); not defined (EBQ24)
Master list: The roster of covered employees and dependants (named); the unnamed master list is a census without names
Portal: The BIBS partner portal; a separate sign-in and access for insurer and client HR users
Programme: One client's employee benefits, with its benefit lines, contacts and cycles
RA: Renewal Advice
SOA: Statement of Account issued by the insurer
TAT: Turn-around time (TAT annex, p.42)
TOR: Terms of Reference sent to insurers as the request for proposal
UAT: User acceptance testing
Utilization report: The claims use of the current programme, from the client or the incumbent
```

<!-- pagebreak -->

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority** and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained in BIBS (parameter, list of values, rule table or template, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-8 test plan.

> [!NOTE]
> Messages are given with their text; a code is quoted where it is confirmed (for example ACCESS_DENIED), and "To be confirmed" marks the others. Values marked "default" are placeholders that BDOI confirms through the open questions in section 10.3; they are configuration, so a changed answer does not need a change to the system.


# Business context and process overview

## Business context

Business comes from five EB teams (Add. p.14): BDO (captive bank group accounts), SM, Voluntary, Solicited and New Business. Renewals dominate: about 4,150 of the 4,200 transactions of 2025 are renewals (Team 5 New Business has 50). Today renewal advices are prepared in Word or Excel, TOR, master lists, utilization and proposals travel by e-mail, comparatives are compiled by hand, and hand-offs between Marketing, Processing and Collection run on e-mail and spreadsheets (Add. p.3).

The BRD asks for an end-to-end, system-driven workflow (Add. p.4-5): automatic renewal advices with reminders, one repository of version-controlled documents, secure access for insurers and clients, standard comparative reports, placement triggered by the client's confirmation, structured hand-offs with notifications and audit, and real-time reports.

<!-- table: widths=1,8,8 caption="Current and envisioned process (Add. p.3-5)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Renewal advices are prepared manually in Word or Excel | A daily job sends the RA 135 days before expiry (default), with reminders and a feedback request |
| 2 | TOR, master lists, utilization and proposals go by e-mail with no repository | Every document is registered with its cycle, type, process and version; outbound files are password protected |
| 3 | Insurers and clients reply by e-mail | Insurers and client HR users work in the partner portal; nothing they upload takes effect before BDOI validates it |
| 4 | The comparative is compiled by hand | BIBS compiles the comparative from the validated proposals; sign-off and value threshold approval are workflow steps |
| 5 | Placement starts on an e-mail to Processing | The client's confirmation triggers one account per benefit line and notifies Processing |
| 6 | Member movements and billing are tracked in spreadsheets | Member changes, direct billing, contracts, HMO cards and SOAs are tracked items with automatic follow-ups |
| 7 | Reports are produced manually | Production, Renewal, Placement, New Business and TAT reports run in BIBS with a business type filter |

## Process overview

The table lists the steps of an EB cycle, and Figure 1 shows them by actor. Steps 1 to 9 are stages of the workflow EB_CYCLE (section 5.1). From step 10 the BRD-1 spine takes over: account, placement, issuance and booking.

<!-- table: widths=0.8,3.6,3.4,7.4,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Renewal advice | System, AO | RA sent before expiry to the HR contacts of programmes flagged for renewal | BRID-001 |
| 2 | Requirements and feedback | AO, client HR | Master list, utilization and feedback uploaded; incumbent's indicative proposal sent | BRID-002, 003, 007, 014 |
| 3 | BOR | AO, client | Signed BOR uploaded and validated; insurer requests blocked without it | BRID-008 |
| 4 | Franchise | AO, insurer | BOR and documents to each target insurer; insurer approves or rejects in the portal | BRID-026, 027, 029 |
| 5 | TOR to insurers | AO | TOR, unnamed master list and utilization released to insurers with approved franchise | BRID-004, 007, 009 |
| 6 | Proposals | Insurer, AO | Proposals submitted on the structured portal form; the AO validates them | BRID-005.01-005.03 |
| 7 | Comparative | System, AO, signatory | Comparative compiled from validated proposals and signed off | BRID-010 |
| 8 | Threshold approval | BDOI Management | Required when the TSI or premium meets a threshold rule | BRID-016 |
| 9 | Client decision | Client HR, AO | Comparative presented; revisions relayed to insurers; client confirms | BRID-011, 012, 015, 017 |
| 10 | Placement to booking | Processing | Accounts created from the chosen proposal; placement, issuance and booking with the billing number check | BRID-017-020 |
| 11 | SOA and payment | Processing, Collection | SOA received, validated and released; payment status from the invoice ledger | BRID-021 |
| 12 | Member servicing | AO, Processing, insurer | Member changes relayed and billed; contracts, cards and billing monitored | BRID-013, 025, 030 |

![EB cycle by actor (Add. p.3-5; BRID-001-030)](figures/brd08_process_flow.dot){width=15}

## New business and renewal cycles

<!-- table: widths=3.4,6.6,6.6 caption="New business and renewal lanes (Add. p.4; main p.5)" -->
| Aspect | Renewal | New business |
|---|---|---|
| Start | The RA job opens the cycle before expiry | The AO opens the cycle for a new client or a new benefit line |
| Client record | Existing client | Captured as prospect and tagged (BRID-006) |
| Incumbent proposal | Indicative renewal terms sent to the client (BRID-003) | Not applicable |
| Remarketing | Optional; chosen by the AO when the client asks for other options | Always: TOR goes to the insurers with approved franchise |
| BOR | Required when remarketing (EBQ05) | Required before any insurer request |
| Business type on the account | RENEWAL, with a reference to the expiring ARN | NEW_BUSINESS |

# Personas and roles

## Personas

<!-- table: widths=3.2,4.1,7.3,3 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in Employee Benefits | BRD |
|---|---|---|---|
| Marketing AO (EB) | EB_AO | Maker of almost every EB action: programmes, RA, feedback, BOR, franchise, TOR, requests, proposal validation, comparative, revisions, confirmation, placement trigger, member changes; requests portal users | Add. p.14-16; BRID-001-017 |
| Marketing TL / UH (EB) | EB_TL | Authorised signatory of the comparative; assigns work | BRID-010 |
| BDOI Management | EB_MANAGEMENT | Approves above the value threshold | BRID-016 |
| Processing (EB) | EB_PROCESSOR | Validates member changes, policy forms and SOAs; completes placement, issuance and booking | BRID-018-021 |
| Processing Supervisor | EB_PROC_SUPERVISOR | As Processing; assigns and approves where BDOI defines a step (EBQ23) | BRID-018 |
| Collection (EB) | EB_COLLECTION | Billing, SOA and payment tracking | BRID-021, 025 |
| Business Administrator | BUSINESS_ADMIN | EB set-up (threshold rules, required documents); approves portal user requests; locks and unlocks portal users | BRID-016, 005 |
| Insurer user | Portal role INSURER_USER | Answers requests, submits proposals, decides franchise, bills member changes, uploads policy forms and SOAs, for its own insurer only | BRID-005.02, 015, 027 |
| Client HR user | Portal role CLIENT_HR | Views its programmes and roster, uploads master lists, utilization, member changes and feedback, comments on and confirms the comparative, for its own company only | BRID-014, 011, 017 |

The addendum notes that approvals are conditional: the client approves proposals and placement, the insurer approves franchise, proposals and issuance, and BDOI Management approves above the value threshold; Processing and Collection validate and execute (Add. p.16). The roles above are the project's proposal until BDOI confirms the matrix (OQ48).

## Permissions

<!-- table: widths=5.6,11 caption="Employee Benefits permissions" -->
| Permission | Allows |
|---|---|
| EB_VIEW | EB screens, programmes, cycles and documents (subject to the access classes) |
| EB_MARKET | AO maker actions: programme, RA, feedback, BOR, franchise, TOR, requests, proposal entry and validation, comparative, revisions, confirmation, placement trigger, member changes |
| EB_COMPARATIVE_APPROVE | Sign off a comparative (never the maker) |
| EB_THRESHOLD_APPROVE | Approve above the value threshold |
| EB_PROCESS | Processing: validate member changes, policy forms and SOAs; completeness checks |
| EB_COLLECT | Collection: SOA and billing view; release acknowledgement |
| EB_SETUP | Threshold rules, required documents, EB parameters and templates |
| EB_REPORT_VIEW | EB reports |
| PORTAL_USER_REQUEST | Raise User Access requests of user type External (portal users) |
| PORTAL_USER_APPROVE | Decide User Access requests of user type External |
| PORTAL_ADMIN | Lock and unlock portal users; portal log-in and download logs |

Portal users are not BIBS users and hold none of these permissions. Their portal role (INSURER_USER or CLIENT_HR) is valid only in the portal and is bound to one insurer or one client.

## Permissions matrix

<!-- table: widths=4.9,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.2 caption="Role-to-action matrix for Employee Benefits (proposal until OQ48)" size=8 -->
| Permission | EB AO | EB TL | EB Mgmt | EB Proc. | Proc. Sup. | EB Coll. | Bus. Admin | Auditor |
|---|---|---|---|---|---|---|---|---|
| EB_VIEW | Y | Y | Y | Y | Y | Y | Y | Y |
| EB_MARKET | Y | | | | | | | |
| EB_COMPARATIVE_APPROVE | | Y | | | | | | |
| EB_THRESHOLD_APPROVE | | | Y | | | | | |
| EB_PROCESS | | | | Y | Y | | | |
| EB_COLLECT | | | | | | Y | | |
| EB_SETUP | | | | | | | Y | |
| EB_REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y |
| PORTAL_USER_REQUEST | Y | | | | | | | |
| PORTAL_USER_APPROVE | | | | | | | Y | |
| PORTAL_ADMIN | | | | | | | Y | |
| WORK_ASSIGN (existing) | | Y | | | Y | | | |

The EB roles also receive the BRD-1 and Collections permissions they need for the shared screens (for example CLIENT_VIEW and ACCOUNT_VIEW for the AO, the Processing permissions for EB_PROCESSOR, the Collections work permissions for EB_COLLECTION). Segregation of duties is enforced by the system: a comparative is never signed off by its maker, a threshold approval is never given by the AO of the cycle, and a portal user request is never approved by its requester.

# Functional requirements

## Access, documents and audit

```fr
id: FR-EB-001
title: Restrict documents and reports to authorised users
brd: [BRID-023 (p.30-31), BRID-005 (p.10-11)]
actor: System
priority: Must have
screens: All EB screens; Reports; Documents tabs
description:
  - Only authorised users view, download, print or access EB documents and reports. Each EB screen, action and report requires one of the permissions of section 3.2; menus show only what the user's roles allow. Document lists and downloads also apply the department access classes of FR-EB-002.
  - Insurer and client HR users never reach the core BIBS screens or direct requests (FR-EB-010).
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a screen, a document or a report.
  - BIBS checks the permission and, for a document, its access class.
  - BIBS shows the item or refuses the request.
alternate_flows:
  - No permission. The action is not offered; a direct call is refused and logged.
rules:
  - [R1, "Roles are granted the permissions of section 3.3 until BDOI confirms the matrix.", Configurable, User Access Maintenance request]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Refused actions are logged with user, function and time."
acceptance:
  - A Collection user opens the EB SOA register and does not see the comparative approval action.
  - A user without EB_REPORT_VIEW cannot run the EB reports.
  - An authorised Processing user downloads a policy form without error.
```

```fr
id: FR-EB-002
title: Link every document to its transaction and restrict it by department
brd: [BRID-025 (Add. p.11; p.32)]
actor: Marketing AO; System
priority: Must have
screens: Programme page (Documents tab); Member Changes; SOA Register
description:
  - Every document uploaded or generated in EB is linked to the client, the programme or policy, and the transaction (cycle, member change, SOA), and carries a process tag - new business placement, renewal placement, endorsement, adjustment, franchise or proposal. An EB upload without its tag and transaction is refused.
  - Each document type has an access class listing the departments that may see it - Marketing, Processing, Collection (section 6.2). Lists and downloads show only the documents of the user's classes. Collection users open billing documents, SOAs, endorsement files and collection supporting documents directly.
preconditions:
  - "The user has EB_VIEW (view) or EB_MARKET (upload)."
main_flow:
  - The AO uploads a document on a cycle or member change, selects its type and process tag.
  - BIBS stores it with the links and applies the access class of its type.
  - A user of another department opens the Documents tab; BIBS lists only the documents of the user's classes.
rules:
  - [R1, "Access classes per document type (section 6.3); types without a row keep today's behaviour.", Configurable, Document access classes]
  - [R2, "Process tags - NB placement, renewal placement, endorsement, adjustment, franchise, proposal.", Configurable, LOV EB_PROCESS_TYPE]
  - [R3, "The final confidentiality matrix, including the contact centre, is confirmed under XQ04.", Configurable, Document access classes]
validations:
  - [Process tag not selected, Select the process of the document, "-"]
  - [No transaction linked, Link the document to its cycle or member change, To be confirmed]
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
  - [Empty file, The file is empty, ATTACHMENT_EMPTY]
fields_screen: Upload document (EB)
fields:
  - [Document type, List, "Yes", DOCUMENT_TYPE (EB values), "-"]
  - [Process, List, "Yes", LOV EB_PROCESS_TYPE, "-"]
  - [Linked to, Display, "Yes", Cycle / member change / SOA, From the screen]
  - [File, Attachment, "Yes", "-", Allowed file types and size of the platform]
notifications:
  - "Processing and Collection are notified of billing documents uploaded on a member change (FR-EB-056)."
audit:
  - "Upload, download and access refusal with user and time."
acceptance:
  - A direct billing document uploaded by the AO on a member change is visible to Processing and Collection.
  - A utilization report is not listed for a Collection user.
  - An EB upload without a process tag is refused.
```

```fr
id: FR-EB-003
title: Log key actions and keep the history of proposals and documents
brd: [BRID-024 (p.31)]
actor: System; users with EB_VIEW (read)
priority: Must have
screens: Programme page (History tab); Proposal and comparative pages (Versions)
description:
  - BIBS logs every key action with time, user and reference - RA sent, feedback received, proposals submitted and validated, documents uploaded and released, approvals, client confirmation, placement trigger and every portal action.
  - TOR, BOR, proposals, comparatives and member rosters are versioned. A submitted version is never changed; a change creates version n+1. Users review the version history and the differences between two versions of a proposal or TOR.
preconditions:
  - "None."
main_flow:
  - A user or portal user performs an action.
  - BIBS writes the audit entry and, for a versioned item, the new version, together with the change; one is never kept without the other.
  - The History tab lists the actions of the programme in time order.
rules:
  - [R1, "Audit and version entries can only be added; no user can change or delete them.", Fixed, "-"]
  - [R2, "Portal actions are recorded under the portal user with the prefix portal and the bound party.", Fixed, "-"]
  - [R3, "Retention 5 years online and 15 years offline (p.37).", Configurable, "Retention rules EB_PROGRAMME, EB_MEMBER"]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - The History tab of a placed cycle lists the RA, the proposals, the comparative sign-off, the confirmation and the placement trigger with user and time.
  - A revised proposal shows version 2 and the differences from version 1.
  - No screen allows an audit or version row to be changed.
```

```fr
id: FR-EB-004
title: Protect documents sent outside BDOI
brd: [BRID-007 (p.12-13)]
actor: Marketing AO; System
priority: Must have
screens: Send dialogs of the Programme page (TOR, RA, comparative, franchise, revision)
description: TOR, master lists, utilization reports, RAs, comparatives and other EB files leave BDOI encrypted and password protected, one or several files at a time. The password is a standard assigned value or a system-generated one following a defined syntax, and is always sent in a separate message. PDF, XLSX and DOCX files are protected; a file type that cannot be protected (CSV, images) is refused for outbound sending.
preconditions:
  - "A document is selected for sending."
main_flow:
  - The AO sends one or several documents from a cycle.
  - BIBS protects each file and sends it; the password goes in a second e-mail.
  - BIBS logs both e-mails.
alternate_flows:
  - File that cannot be protected. BIBS refuses it and names the file; the AO converts it to PDF or Excel.
rules:
  - [R1, "Every EB file sent outside BDOI is protected; protection cannot be switched off.", Fixed, "-"]
  - [R2, "Password convention (standard value or syntax) documented by BDOI under EBQ09; until then a password is generated per sending.", Configurable, Document password policy]
validations:
  - [File cannot be protected, "<file> cannot be password protected. Send it as PDF, Excel or Word", To be confirmed]
  - [Recipient address invalid, Enter a valid e-mail address, EMAIL_ADDRESS_INVALID]
notifications:
  - "The recipient receives the protected files and, separately, the password."
audit:
  - "Each e-mail with recipient, time, subject, attachment names and hash."
acceptance:
  - A TOR sent as Word arrives protected, and the password arrives in a separate e-mail.
  - A CSV master list is refused for outbound sending.
```

## Partner portal

```fr
id: FR-EB-010
title: Give insurers and client HR users a separate, secure portal
brd: [BRID-005 (p.10-11), BRID-005.01 (Add. p.7-8), BRID-005.02 (Add. p.8-9), BRID-014 (p.20-22)]
actor: Insurer user; client HR user; System
priority: Must have
screens: Portal sign-in; Portal home; Accept invitation
description:
  - Insurers and client HR users interact with BDOI only through the partner portal, never the core system. The portal has its own sign-in; a portal sign-in gives no access to the core system, and a core sign-in none to the portal. Each portal user is bound to one party - one insurer or one client - and every portal query is filtered on that party, so a user cannot see or reach another party's records.
  - The portal can run as a separate deployment in the internet zone. Its hosting, BDO Information Security approval and the multi-factor method are confirmed under EBQ13.
preconditions:
  - The portal user was provisioned (FR-EB-011) and has set a password.
main_flow:
  - The portal user opens the portal and signs in with e-mail and password.
  - BIBS sends a one-time code by e-mail when multi-factor authentication is on; the user enters it.
  - The portal home shows the user's open tasks and notices.
alternate_flows:
  - Wrong credentials. The sign-in is refused; after three failures the portal user is locked (decision D5).
  - Record of another party. BIBS answers "not found" and logs the attempt.
  - Idle session. The session ends after the portal timeout.
rules:
  - [R1, "Separate sign-in; the core system refuses a portal sign-in and the portal a core sign-in.", Fixed, "-"]
  - [R2, "Every portal query is scoped to the bound insurer or client.", Fixed, "-"]
  - [R3, "Lockout after 3 failed sign-ins.", Configurable, Parameter PORTAL_MAX_FAILED_LOGINS]
  - [R4, "Session timeout 15 minutes (default).", Configurable, Parameter PORTAL_SESSION_MINUTES]
  - [R5, "E-mail one-time code on (default); final method under EBQ13.", Configurable, Parameter PORTAL_MFA_REQUIRED]
validations:
  - [Wrong e-mail or password, Invalid e-mail or password, "-"]
  - [Portal user locked, Your portal access is locked. Contact your BDOI account officer, "-"]
  - [One-time code wrong or expired, The code is not valid. Request a new code, "-"]
fields_screen: Portal sign-in
fields:
  - [E-mail, Text, "Yes", "-", Registered portal user]
  - [Password, Password, "Yes", "-", Masked]
  - [One-time code, Text, Cond., "-", Required when multi-factor is on]
notifications:
  - "Lockout raises the alert PORTAL_LOGIN_LOCKED to the portal administrators."
audit:
  - "Every sign-in (success or failure) with time, address and browser; every portal action under the portal user."
acceptance:
  - An insurer user signs in and sees only the requests of its own insurer.
  - A portal user who tries to open a BIBS staff function is refused.
  - The fourth failed sign-in locks the portal user.
```

```fr
id: FR-EB-011
title: Provision portal users through User Access requests
brd: [BRID-005 (p.10-11), BRID-014 (p.20-22)]
actor: Marketing AO (requester, PORTAL_USER_REQUEST); approver (PORTAL_USER_APPROVE); Business Administrator (PORTAL_ADMIN)
priority: Must have
screens: User Access Requests (user type External); Portal Users (Setup & Administration)
description:
  - Portal users are created, disabled and enabled only through User Access Maintenance requests of user type External (decision D7). The AO raises the request with the person's name and e-mail, the party kind (insurer or client), the party code and the portal role. A chosen approver other than the requester decides it, with the draft, return and history steps of User Access Maintenance.
  - On approval BIBS creates the portal user as INVITED and e-mails an invitation link, valid for 72 hours (default). The user sets the password through the link; no password is ever sent by e-mail. The Business Administrator locks and unlocks portal users and reviews the portal sign-in and download logs.
preconditions:
  - The requester has PORTAL_USER_REQUEST.
main_flow:
  - The AO opens User Access Requests and chooses user type External.
  - The AO enters the user and party details and submits to an approver.
  - The approver approves the request.
  - BIBS creates the portal user and sends the invitation.
  - The user opens the link, sets a password and signs in.
alternate_flows:
  - The approver returns or rejects the request with a reason; no user is created.
  - Invitation expired. The job expires unused invitations; the AO requests a new invitation.
  - Disable. A disable request, once approved, blocks the user at once.
rules:
  - [R1, "The approver is never the requester.", Fixed, "-"]
  - [R2, "A portal user is bound to exactly one insurer or one client.", Fixed, "-"]
  - [R3, "Invitation valid 72 hours (default).", Configurable, Parameter PORTAL_INVITE_VALID_HOURS]
  - [R4, "Whether several users per insurer or client and delegated administration are allowed is confirmed under EBQ13.", Configurable, Change request after EBQ13]
validations:
  - [E-mail already used by a portal user, "<e-mail> already has a portal user", To be confirmed]
  - [Party not found, Select an existing insurer or client, "-"]
  - [Approver is the requester, An access request is approved by someone other than the requester, To be confirmed]
fields_screen: User Access Request (External)
fields:
  - [Full name, Text, "Yes", "-", Up to 200 characters]
  - [E-mail, Text, "Yes", "-", Valid e-mail; unique among portal users]
  - [Party kind, Option, "Yes", Insurer / Client, "-"]
  - [Party, Look-up, "Yes", Insurer master or client master, Active party]
  - [Portal role, List, "Yes", INSURER_USER / CLIENT_HR, Must match the party kind]
  - [Request type, List, "Yes", Create / Disable / Enable, "-"]
  - [Approver, List, "Yes", Holders of PORTAL_USER_APPROVE, Not the requester]
notifications:
  - "The approver on submission; the requester on the decision; the new user receives the invitation."
audit:
  - "Request, decision, provisioning, invitation, lock and unlock with users and times."
acceptance:
  - A portal user exists only after another user approves the request.
  - The invitation e-mail contains a link and no password.
  - A client HR user created for client A cannot see client B's programmes.
```

```fr
id: FR-EB-012
title: Stage portal uploads and link them automatically
brd: [BRID-005.01 (Add. p.7-8), BRID-014 (p.20-22)]
actor: Insurer user; client HR user; System
priority: Must have
screens: Portal task pages (upload)
description:
  - Insurers upload proposals, policy forms and billing documents; client HR users upload master lists, utilization reports, member changes and feedback. Every upload is made against a task (an insurer request, a franchise request, a member change, an SOA request, a programme), so BIBS links it automatically to the right client, policy, member change or transaction.
  - Uploads are staged. They are virus scanned and checked for type, size and content, and take effect only when an internal user validates them (FR-EB-013). The portal user sees a confirmation or an error at once.
preconditions:
  - The portal user has an open task for the target.
main_flow:
  - The portal user opens the task and uploads one or more files with their document type.
  - BIBS checks each file and stores it as RECEIVED.
  - BIBS shows the confirmation and notifies the reviewer.
alternate_flows:
  - Unsupported or infected file. BIBS refuses it with the reason; nothing is staged.
  - No open task for the target. The upload is not possible (no request, no upload).
rules:
  - [R1, "Nothing uploaded through the portal takes effect before validation.", Fixed, "-"]
  - [R2, "Accepted formats PDF, Word, Excel and the other platform types; size limit 10 MB (default).", Configurable, Parameter PORTAL_MAX_UPLOAD_MB]
  - [R3, "A production virus scanner is required before the portal goes live; files stay RECEIVED until the scan passes.", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
  - [File too large, The file is larger than the allowed size, ATTACHMENT_TOO_LARGE]
  - [Content does not match the type, The file content does not match its type, ATTACHMENT_CONTENT_MISMATCH]
  - [Virus found, The file failed the virus check, ATTACHMENT_INFECTED]
fields_screen: Portal upload
fields:
  - [Task, Display, "Yes", Open tasks of the party, "-"]
  - [Document type, List, "Yes", Document types allowed for the task, "-"]
  - [Files, Attachment, "Yes", "-", One or more; allowed types and size]
  - [Remarks, Text, "No", "-", Up to 1000 characters]
notifications:
  - "PORTAL_UPLOAD_WAITING alert when a staged upload is older than one working day."
audit:
  - "Each upload with portal user, party, target, file name, size, hash and scan result."
acceptance:
  - An insurer's proposal upload is linked to the request and client without any selection by the insurer.
  - An executable file is refused with ATTACHMENT_TYPE_NOT_ALLOWED.
  - A staged master list does not change the roster until the AO validates it.
```

```fr
id: FR-EB-013
title: Review, validate or reject portal uploads
brd: [BRID-005.03 (Add. p.9-10), BRID-005.01 (Add. p.7-8)]
actor: Marketing AO (proposals, franchise, client documents); Processing (policy forms); Collection (billing)
priority: Must have
screens: Portal Uploads (review queue); EB Home (tile Portal uploads to review)
description: Staged uploads appear in the Portal Uploads queue of the department that owns their target type. The reviewer opens the file, checks the mapping, and validates or rejects it with a reason. Before validation the reviewer can re-map an upload to another target of the same party. On validation the file becomes an attachment of the target (and, for a master list or proposal form, business data); on rejection the portal user is told why.
preconditions:
  - "The user has the review permission of the target type (EB_MARKET, EB_PROCESS or EB_COLLECT)."
main_flow:
  - The reviewer opens Portal Uploads and selects an upload.
  - The reviewer checks the file and its target.
  - The reviewer clicks **Validate**; BIBS creates the attachment and applies the data.
alternate_flows:
  - Reject. The reviewer selects the reason; the portal user receives a notice.
  - Wrong target. The reviewer re-maps the upload to another open target of the same party, then validates it.
rules:
  - [R1, "Reviewer by target - AO for proposals, franchise and client documents; Processing for policy forms and SOAs; Collection for billing.", Fixed, "-"]
  - [R2, "Reject reasons are maintained.", Configurable, LOV PORTAL_REJECT_REASON]
validations:
  - [Reject without reason, "Select a reason for 'reject'", WORKFLOW_REASON_REQUIRED]
  - [Re-map to another party's target, The upload can only be linked to a record of the same party, To be confirmed]
notifications:
  - "The reviewer on arrival (in-app and e-mail); the portal user on validation or rejection."
audit:
  - "Validation, rejection and re-mapping with reviewer, time and reason."
acceptance:
  - An insurer's policy form appears in the Processing queue, not the AO's.
  - A rejected upload shows the reason on the insurer's portal task.
  - A validated proposal counts in the comparative; a staged one does not.
```

```fr
id: FR-EB-014
title: Insurer task inbox and structured proposal form
brd: [BRID-005.02 (Add. p.8-9), BRID-005 (p.10-11)]
actor: Insurer user
priority: Must have
screens: Portal home (tasks); Requests for Proposal; Franchise Requests; Member Changes; SOA and Policy Forms
description:
  - The insurer's portal home lists its open tasks - requests for proposal to answer, franchise requests to decide, member changes to bill, SOAs and policy forms to send - with due dates.
  - A request for proposal shows the TOR and the documents to download, and a structured proposal form generated from the TOR items - per benefit line and plan, the insurer's offered value for each item, a deviation flag and remark, the premium per plan, the annual premium and TSI, exclusions and capability factors (company stability, clinic providers, hospital network, technology). Attachments go with the same submission.
preconditions:
  - The insurer has an open request (FR-EB-035).
main_flow:
  - The insurer user opens the request and downloads the TOR and documents.
  - The user completes the proposal form and attaches the insurer's proposal document.
  - The user submits; BIBS stores the proposal as SUBMITTED and notifies the AO.
alternate_flows:
  - Decline. The insurer declines the request with a reason; the request closes as DECLINED.
  - Save draft. The user saves the form and returns later before the due date.
rules:
  - [R1, "A proposal can only be submitted against an open request of the insurer.", Fixed, "-"]
  - [R2, "Every TOR item needs an offered value or a deviation remark.", Fixed, "-"]
  - [R3, "Capability factors are maintained.", Configurable, LOV EB_CAPABILITY_FACTOR]
validations:
  - [TOR item left blank, "Answer every TOR item (<item>) or mark a deviation", To be confirmed]
  - [Premium missing for a plan, "Enter the premium of plan <plan>", "-"]
  - [Request closed or past due, This request is closed. Contact the BDOI account officer, To be confirmed]
fields_screen: Proposal form (portal)
fields:
  - [Benefit line, Display, "Yes", TOR, "-"]
  - [TOR item, Display, "Yes", TOR items, "-"]
  - [Offered value, Text, Cond., "-", Required unless deviation]
  - [Deviation, Check box, "No", "-", Remark required when ticked]
  - [Premium per plan, Amount, "Yes", "-", ">= 0"]
  - [Annual premium / TSI, Amount, "Yes", "-", ">= 0"]
  - [Capability factors, Rating and text, "No", LOV EB_CAPABILITY_FACTOR, "-"]
  - [Validity date, Date, "Yes", "-", After today]
  - [Attachments, Attachment, "No", "-", Allowed types and size]
notifications:
  - "The AO when a proposal is submitted or a request declined."
audit:
  - "Draft saves and submission under the portal user."
acceptance:
  - A proposal form shows one line per TOR item of the request.
  - A submission with an unanswered TOR item is refused.
  - An insurer cannot submit a proposal without an open request.
```

```fr
id: FR-EB-015
title: Client HR portal
brd: [BRID-014 (p.20-22), BRID-011 (p.18)]
actor: Client HR user
priority: Must have
screens: Portal - Programmes; Members; Uploads; Comparative; Renewal Advice and Documents
description:
  - The client HR user sees only its own company - programme and policy summary, contacts, the member roster (read), the renewal advices and the documents released to the client. The user uploads master lists, utilization reports, member changes and feedback (staged, FR-EB-012), and views, comments on and, where allowed, confirms the comparative (FR-EB-043, 046).
  - Master lists uploaded in Excel are mapped automatically to the roster fields by a template with column mapping and row validation; the result is a staged roster version that the AO accepts (FR-EB-054). Every download is logged.
preconditions:
  - The client HR user is provisioned for the client.
main_flow:
  - The HR user signs in and opens Programmes.
  - The user downloads the renewal advice or a released document.
  - The user uploads the master list with the template; BIBS validates every row and stages the roster version.
alternate_flows:
  - Rows with errors. BIBS lists each row and error; nothing is staged until the file is corrected.
rules:
  - [R1, "Client HR users view and upload only; no change to policy data (BRID-014). What 'manage policy' adds is confirmed under EBQ14.", Configurable, Change request after EBQ14]
  - [R2, "Downloads by portal users are logged.", Fixed, "-"]
validations:
  - [Template columns missing, "The file does not have the column <column>. Use the master list template", To be confirmed]
  - [Row error, "Row <n>: <field> <error>", To be confirmed]
notifications:
  - "The AO when a master list, utilization report, member change or feedback is uploaded."
audit:
  - "Uploads and downloads under the portal user."
acceptance:
  - A client HR user sees the roster of its company and no other company.
  - A master list with a missing birth date on row 12 is refused with the row named.
  - Each download appears in the portal download log.
```

## Programme, business type and renewal start

```fr
id: FR-EB-020
title: Capture and tag new clients as prospect or client
brd: [BRID-006 (p.11-12)]
actor: Marketing AO
priority: Must have
screens: Client pages (CRM); New Programme (New Client)
description: New EB clients are captured in the BIBS client master with the required fields and tagged as prospect or confirmed client by the onboarding workflow of BRD-1. Search filters by the tag. The duplicate check stops a second record of the same client. The EB programme is opened from the client page.
preconditions:
  - "The AO has CLIENT_MAINTAIN."
main_flow:
  - The AO searches the client; none is found.
  - The AO creates the prospect with the required fields.
  - BIBS checks for duplicates and saves the prospect.
  - After KYC onboarding the client is confirmed.
alternate_flows:
  - Duplicate found. BIBS refuses the new record and shows the existing client.
rules:
  - [R1, "Prospect = client of status PROSPECT; client = CONFIRMED.", Fixed, "-"]
validations:
  - [Client already exists, A client with the same identifiers already exists, CLIENT_DUPLICATE]
  - [Name blank, Enter the client name, CLIENT_NAME_REQUIRED]
notifications:
  - "As the BRD-1 onboarding workflow."
audit:
  - "Client creation and status changes."
acceptance:
  - A new EB prospect is created and appears in the prospect search.
  - A second record with the same identifiers is refused with CLIENT_DUPLICATE.
```

```fr
id: FR-EB-021
title: Open an EB programme and its cycles with the business type
brd: [BRID-022.01 (Add. p.10-11), BRID-006 (p.11-12)]
actor: Marketing AO (EB_MARKET)
priority: Must have
screens: New Programme; Programmes; Programme page
description:
  - A programme groups the client's benefit lines (HMO, GLI, GPA), the team (BDO, SM, Voluntary, Solicited, New Business), the funding (employer or voluntary), the HR contacts and the renewal flag. Each line holds the incumbent insurer, the current policy and ARN, the period and the headcount.
  - One cycle per policy year carries the business type, NEW_BUSINESS or RENEWAL. The type is required and travels to the accounts created from the cycle (business type BT0), so every report can be filtered by it.
preconditions:
  - The client exists in BIBS.
main_flow:
  - The AO clicks **New Programme** from the client page.
  - The AO enters the lines, team, funding and contacts, and saves; BIBS numbers the programme EBP-yyyy-nnnnnn.
  - The AO opens a new-business cycle, or the RA job opens a renewal cycle (FR-EB-022).
rules:
  - [R1, "Programme numbers EBP-<yyyy>-nnnnnn; cycle numbers EBC-<yyyy>-nnnnnn.", Configurable, Document number series]
  - [R2, "Business type required on every cycle and account.", Fixed, "-"]
  - [R3, "Benefit lines and teams are lists of values.", Configurable, "LOV EB_BENEFIT_LINE, EB_TEAM"]
validations:
  - [No benefit line, Add at least one benefit line, "-"]
  - [No HR contact, Add at least one HR contact, "-"]
  - [Open cycle exists for the policy year, "Programme <no.> already has an open cycle for <year>", To be confirmed]
fields_screen: New Programme
fields:
  - [Client, Look-up, "Yes", Client master, Prospect or confirmed client]
  - [Programme name, Text, "Yes", "-", Up to 200 characters]
  - [Team, List, "Yes", LOV EB_TEAM, "-"]
  - [Funding, Option, "Yes", Employer / Voluntary, EBQ27]
  - [Benefit lines, Table, "Yes", LOV EB_BENEFIT_LINE, "Line, product, incumbent insurer, policy, period, headcount"]
  - [HR contacts, Table, "Yes", "-", "Name, e-mail, mobile, role, receives RA / SOA"]
  - [Eligible for renewal, Check box, "No", "-", "-"]
  - [Account officer, List, "Yes", EB AOs, Default the user]
notifications:
  - "None."
audit:
  - "Programme and cycle creation and changes."
acceptance:
  - A programme with HMO and GLI lines is saved with an EBP number.
  - A renewal cycle creates accounts of business type RENEWAL.
  - A cycle cannot be saved without its business type.
```

```fr
id: FR-EB-022
title: Send the renewal advice automatically before expiry
brd: [BRID-001 (p.7)]
actor: System; Marketing AO
priority: Must have
screens: EB Home (tile RA due); Programmes (tab Renewal Due; bulk Send RA); Programme page
description:
  - A daily job opens a renewal cycle for each programme flagged for renewal whose line expires in the lead time, composes the RA from the template with the key programme details, and sends it to the programme's HR contacts that receive RAs. Reminders follow at set days while no feedback is recorded; each carries a feedback request with a link to the client portal.
  - The RA is stored as document type RENEWAL_ADVICE linked to the programme, the current ARN and the client, so the contact centre can view and resend it (decision D3). No RA goes to new, cancelled or ineligible programmes. The AO can also send the RA manually from the Programmes list.
preconditions:
  - The programme is flagged for renewal and has at least one HR contact receiving RAs.
main_flow:
  - The job runs daily at 06:00 PHT.
  - BIBS selects eligible programmes at the lead time and opens their renewal cycles.
  - BIBS generates and sends the RA protected, and stores it.
  - The cycle moves to RA_SENT; reminders are scheduled.
alternate_flows:
  - No contact or no renewal flag inside the lead time. No RA is sent; the alert EB_RA_NOT_SENT goes to the AO.
  - Manual send. The AO selects programmes in Renewal Due and clicks **Send RA**.
rules:
  - [R1, "Lead time 135 days before expiry (default, TAT annex); the BRD example is 180 days (EBQ02).", Configurable, Parameter EB_RA_LEAD_DAYS]
  - [R2, "Reminders at 120, 105 and 90 days (default).", Configurable, Parameter EB_RA_REMINDER_DAYS]
  - [R3, "Only programmes flagged for renewal; who sets the flag is confirmed under EBQ03.", Fixed, "-"]
  - [R4, "EB lines are excluded from the general Renewal lists (decision D3).", Fixed, "-"]
validations:
  - [Manual send to an ineligible programme, "Programme <no.> is not flagged for renewal", To be confirmed]
notifications:
  - "RA and reminders to the HR contacts; EB_RA_NOT_SENT to the AO."
audit:
  - "RA sending, recipients, reminders and message references."
acceptance:
  - A programme expiring in 135 days receives the RA and moves to RA_SENT.
  - A programme without the renewal flag receives no RA.
  - The RA is listed in the client's documents as a renewal advice.
```

```fr
id: FR-EB-023
title: Collect and record client feedback
brd: [BRID-002 (p.8)]
actor: Marketing AO; client HR user
priority: Must have
screens: Programme page (Cycle tab, Record Feedback); Portal - Uploads
description: The AO records the client's feedback on the renewal cycle as text, attachments (PDF, Word, image) or both, with the channel (AO, portal, e-mail) and the date received. The client HR user can give the same feedback in the portal. BIBS links the feedback to the client and the cycle and confirms the entry. The first feedback moves the cycle from RA_SENT to REQUIREMENTS and stops the RA reminders.
preconditions:
  - "The cycle is open."
main_flow:
  - The AO clicks **Record Feedback**.
  - The AO enters the text or attaches files and the date received.
  - BIBS saves the feedback and shows the confirmation.
rules:
  - [R1, "Feedback needs text or at least one file.", Fixed, "-"]
validations:
  - [Feedback empty, Enter the feedback or attach a file, To be confirmed]
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
fields_screen: Record Feedback
fields:
  - [Channel, List, "Yes", AO / Portal / E-mail, "-"]
  - [Date received, Date, "Yes", "-", Not after today]
  - [Feedback, Long text, Cond., "-", Required without a file]
  - [Files, Attachment, Cond., "-", Required without text]
notifications:
  - "The AO when the client gives feedback in the portal."
audit:
  - "Feedback with user or portal user and time."
acceptance:
  - Feedback without text or file is refused.
  - Feedback recorded on a renewal cycle stops the RA reminders.
```

```fr
id: FR-EB-024
title: Send the incumbent's indicative renewal proposal
brd: [BRID-003 (p.8-9)]
actor: Marketing AO; incumbent insurer
priority: Must have
screens: Programme page (Proposals tab, Indicative Proposal)
description: For programmes flagged for renewal, the AO records the incumbent's indicative proposal (the latest incumbent rates and coverage) or the incumbent submits it in the portal. BIBS generates the letter with the policy number, coverage, incumbent rates, expiry and renewal instructions, and sends it protected to the client's HR contacts. The action is refused for new-business cycles and programmes not flagged for renewal.
preconditions:
  - The cycle is a renewal cycle of a programme flagged for renewal.
main_flow:
  - The AO enters or validates the incumbent's indicative terms.
  - The AO clicks **Send Indicative Proposal**; BIBS generates the letter.
  - BIBS sends it to the HR contacts and logs it.
rules:
  - [R1, "Only renewal cycles of flagged programmes.", Fixed, "-"]
  - [R2, "Whether the incumbent submits through the portal or the AO encodes the terms is confirmed under EBQ04.", Configurable, Change request after EBQ04]
validations:
  - [Cycle not a renewal, The indicative proposal applies to renewals only, To be confirmed]
  - [Rates missing, "Enter the incumbent rate of plan <plan>", "-"]
notifications:
  - "The client's HR contacts receive the letter."
audit:
  - "Letter generation and sending."
acceptance:
  - The indicative proposal of a renewal shows the current policy number, coverage and incumbent rates.
  - The action is not available on a new-business cycle.
```

## Documents, BOR, franchise and TOR

```fr
id: FR-EB-030
title: Upload TOR, master list and utilization report
brd: [BRID-007 (p.12-13)]
actor: Marketing AO; client HR user; incumbent insurer
priority: Must have
screens: Programme page (Documents tab); Portal uploads
description:
  - The TOR, master list (named), unnamed master list, utilization report and other EB documents are uploaded single or in bulk (Word, Excel, PDF), registered with the cycle, type, process tag, version and source, and linked to the client, policy and renewal transaction. A new upload of the same type creates version n+1 and supersedes the previous one.
  - The incumbent insurer is notified through a portal task, with e-mail, to upload the unnamed master list.
preconditions:
  - "The cycle is open."
main_flow:
  - The AO uploads one or several files and selects their types.
  - BIBS registers each file as a new version and confirms.
  - The AO clicks **Request Unnamed Master List**; BIBS creates the portal task for the incumbent and e-mails it.
rules:
  - [R1, "Documents are versioned per cycle and type.", Fixed, "-"]
  - [R2, "Outbound sending follows FR-EB-004.", Fixed, "-"]
validations:
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
  - [Document type not selected, Select the document type, "-"]
fields_screen: EB document upload
fields:
  - [Document type, List, "Yes", "TOR, master list, unnamed master list, utilization, others", "-"]
  - [Process, List, "Yes", LOV EB_PROCESS_TYPE, "-"]
  - [Files, Attachment, "Yes", "-", One or more]
notifications:
  - "The incumbent insurer receives the portal task and e-mail."
audit:
  - "Each version with uploader and time."
acceptance:
  - Uploading a second master list makes it version 2 and marks version 1 superseded.
  - The incumbent sees the task "Upload unnamed master list" in the portal.
```

```fr
id: FR-EB-031
title: Upload and validate the signed Broker on Record
brd: [BRID-008 (p.14-15)]
actor: Marketing AO
priority: Must have
screens: Programme page (BOR tab)
description:
  - The AO uploads the client's signed BOR (PDF or Word). BIBS shows its status - Pending, Uploaded, Validated or Rejected. The validator confirms a checklist - signed by an authorised signatory, not blank, client name matches - and sets the validity dates. An invalid BOR is rejected and re-uploaded as a new version; the latest validated version is the active one.
  - Insurer requests and franchise requests of a new-business cycle, or of a renewal cycle that remarkets, are blocked until a validated BOR exists.
preconditions:
  - "The cycle is open."
main_flow:
  - The AO uploads the signed BOR; the status becomes Uploaded.
  - The validator opens it and completes the checklist.
  - The validator clicks **Validate**; the BOR becomes the active version.
alternate_flows:
  - Checklist not met. The validator rejects it with a reason; the AO uploads a corrected BOR as version n+1.
rules:
  - [R1, "Proposal and franchise requests need a validated BOR for new business and remarketing (renewal need confirmed under EBQ05).", Fixed, "-"]
  - [R2, "The signature check is an attestation by the validator; e-signature verification waits for EBQ06.", Fixed, "-"]
validations:
  - [Checklist incomplete, Complete the BOR checklist before validating, To be confirmed]
  - [Request without a validated BOR, "Cycle <no.> has no validated Broker on Record", To be confirmed]
  - [File type not PDF or Word, Upload the BOR as PDF or Word, ATTACHMENT_TYPE_NOT_ALLOWED]
fields_screen: BOR
fields:
  - [BOR file, Attachment, "Yes", "-", PDF or Word]
  - [Signed by an authorised signatory, Check box, "Yes", "-", Validator]
  - [Not blank, Check box, "Yes", "-", Validator]
  - [Client name matches, Check box, "Yes", "-", Validator]
  - [Valid from / to, Date, "Yes", "-", To after from]
notifications:
  - "The AO when the BOR is validated or rejected."
audit:
  - "Upload, validation and rejection with user and time."
acceptance:
  - A franchise request on a new-business cycle without a validated BOR is refused.
  - A rejected BOR re-uploaded becomes version 2 with status Uploaded.
```

```fr
id: FR-EB-032
title: Request franchise from insurers and record their decision
brd: [BRID-026 (Add. p.12-13), BRID-027 (p.32-33)]
actor: Marketing AO; insurer user
priority: Must have
screens: Programme page (Franchise tab); Portal - Franchise Requests
description:
  - For each target insurer the AO submits a franchise request with the BOR and the required documents. The insurer approves or rejects it in the portal with a reason; where the insurer answers by e-mail, the AO records the outcome with the e-mail as evidence. Only insurers with an approved franchise receive the TOR (FR-EB-035).
  - The insurer has 5 working days to decide (TAT annex). A request not decided after the TAT and a grace period expires and raises an alert to the AO.
preconditions:
  - A validated BOR exists (FR-EB-031).
main_flow:
  - The AO selects the insurers and clicks **Request Franchise**.
  - BIBS checks the required documents (FR-EB-034) and sends the requests (portal task and protected e-mail); the cycle moves to FRANCHISE.
  - The insurer user opens the request and approves or rejects it.
  - BIBS records the decision and notifies the AO.
alternate_flows:
  - Decision by e-mail. The AO records the outcome and attaches the e-mail.
  - No reply. The job expires the request; alert EB_FRANCHISE_OVERDUE to the AO.
rules:
  - [R1, "Franchise TAT 5 working days (default).", Configurable, Parameter EB_FRANCHISE_TAT_DAYS]
  - [R2, "A decision recorded by the AO needs the insurer's evidence.", Fixed, "-"]
  - [R3, "Whether franchise is needed for the incumbent, per insurer or once per cycle, is confirmed under EBQ07.", Configurable, Change request after EBQ07]
validations:
  - [Reject without reason, "Select a reason for 'reject'", WORKFLOW_REASON_REQUIRED]
  - [Outcome recorded without evidence, Attach the insurer's reply, To be confirmed]
fields_screen: Franchise decision (portal)
fields:
  - [Decision, Option, "Yes", Approve / Reject, "-"]
  - [Reason, List, Cond., LOV EB_FRANCHISE_REJECT_REASON, Required for Reject]
  - [Remarks, Text, "No", "-", Up to 1000 characters]
notifications:
  - "The insurer on the request; the AO on the decision; EB_FRANCHISE_OVERDUE on expiry."
audit:
  - "Request, decision and expiry with user or portal user and time."
acceptance:
  - An insurer approves a franchise in the portal and the AO is notified.
  - A request without a decision after the TAT becomes EXPIRED and raises an alert.
  - The TOR cannot be released to an insurer whose franchise is rejected.
```

```fr
id: FR-EB-033
title: Advise the client of the franchise outcome
brd: [BRID-029 (p.33-34)]
actor: Marketing AO
priority: Must have
screens: Programme page (Franchise tab, Advise Client)
description: After an insurer decides a franchise request, the AO advises the client of the approval or rejection by e-mail and portal notice, from the template. The advice is due within the agreed working days from the decision; an overdue advice raises an alert.
preconditions:
  - "The franchise request is APPROVED or REJECTED."
main_flow:
  - The AO clicks **Advise Client**; BIBS generates the advice.
  - The AO confirms; BIBS sends it and marks the request ADVISED.
rules:
  - [R1, "Advice due 2 working days after the decision (default).", Configurable, Parameter EB_FRANCHISE_ADVICE_DAYS]
validations: []
notifications:
  - "The client's HR contacts receive the advice; the AO is alerted when it is overdue."
audit:
  - "Advice sending with user and time."
acceptance:
  - The client receives the franchise outcome and the request shows ADVISED.
  - An outcome not advised within 2 working days raises an alert.
```

```fr
id: FR-EB-034
title: Submit the required documents to insurers per process
brd: [BRID-026 (Add. p.12-13)]
actor: Marketing AO
priority: Must have
screens: Programme page (Submissions); Member Changes; EB Setup (Required Documents)
description:
  - The AO submits the documents an insurer needs for new-business placement, renewal placement, adjustment or endorsement (financial and non-financial), franchise request and proposal - BOR, TOR, master list, utilization, indicative proposal, placement and adjustment requirements. A checklist per process type and benefit line lists the mandatory documents; BIBS refuses the submission while one is missing.
  - The submission goes to the insurer's portal and by protected e-mail, is logged and linked to the client, policy and transaction, and Processing and Collection are notified and can view it. Insurers see only the submissions addressed to them.
preconditions:
  - "The user has EB_MARKET."
main_flow:
  - The AO opens **New Submission**, selects the process type and the insurer.
  - BIBS shows the checklist with the documents found on the cycle.
  - The AO adds missing documents and submits.
  - BIBS sends the documents and records the submission.
rules:
  - [R1, "Required documents per process type, benefit line and document type are maintained with maker-checker.", Configurable, Required Documents (EB_SETUP)]
validations:
  - [Mandatory document missing, "Add the required document <type> for <process>", To be confirmed]
  - [Insurer not selected, Select the insurer, "-"]
fields_screen: New Submission
fields:
  - [Process type, List, "Yes", LOV EB_PROCESS_TYPE, "-"]
  - [Insurer, List, "Yes", Insurers of the cycle, "-"]
  - [Documents, Checklist, "Yes", Required documents, All mandatory present]
notifications:
  - "The insurer (portal task and e-mail); Processing and Collection (in-app)."
audit:
  - "Submission, documents, recipient and time."
acceptance:
  - A renewal placement submission without the master list is refused.
  - Processing sees the submission on the programme.
```

```fr
id: FR-EB-035
title: Prepare the TOR and distribute it to insurers
brd: [BRID-009 (p.15-16), BRID-004 (p.9-10), BRID-007 (p.12-13)]
actor: Marketing AO
priority: Must have
screens: Programme page (TOR tab; Insurer Requests tab)
description:
  - The AO prepares the TOR as structured items per benefit line and plan (item, description, requirement) and releases it. The AO then selects the insurers - only insurers with an approved franchise for the cycle - and distributes the TOR, the unnamed master list and the utilization report. BIBS creates one insurer request per insurer with the TOR version, documents and due date, as a portal task and a protected e-mail.
  - For a renewal, remarketing to other insurers uses the same request, and is possible only for programmes flagged for renewal (BRID-004).
preconditions:
  - A validated BOR and at least one approved franchise exist.
main_flow:
  - The AO maintains the TOR items and clicks **Release TOR**.
  - The AO selects the insurers and clicks **Send Requests**.
  - BIBS creates the requests (EBR-yyyy-nnnnnn), sends them and moves the cycle to PROPOSALS.
alternate_flows:
  - TOR changed after release. The AO releases version n+1; open requests are updated and the insurers notified.
rules:
  - [R1, "Only insurers with an APPROVED franchise for the cycle receive requests.", Fixed, "-"]
  - [R2, "Reply due 5 working days after the request (default).", Configurable, Parameter EB_PROPOSAL_REPLY_DAYS]
  - [R3, "The standard TOR template per benefit line is confirmed under EBQ08.", Configurable, Template EB_TOR]
validations:
  - [Insurer without approved franchise, "Insurer <code> has no approved franchise for this cycle", To be confirmed]
  - [TOR without items, Add the TOR items before release, "-"]
notifications:
  - "Each insurer (portal task and e-mail); EB_PROPOSAL_OVERDUE to the AO for requests past due."
audit:
  - "TOR versions, distribution and downloads per insurer."
acceptance:
  - A TOR sent to three insurers creates three requests with the same TOR version.
  - An insurer with a rejected franchise cannot be selected.
  - Each insurer's download of the TOR is logged.
```

## Proposals, comparative, approvals and client decision

```fr
id: FR-EB-040
title: Record and validate insurer proposals
brd: [BRID-005.02 (Add. p.8-9), BRID-005.03 (Add. p.9-10), BRID-010 (p.16-17)]
actor: Marketing AO
priority: Must have
screens: Programme page (Proposals tab); Proposal page
description: Proposals come from the portal form (FR-EB-014) or are entered by the AO from an e-mailed proposal. Each proposal is SUBMITTED until the AO validates it; only validated proposals enter the comparative. The AO may reject a proposal with a reason, and the insurer can then submit a new version. The incumbent's indicative proposal counts as a proposal of kind INCUMBENT_INDICATIVE.
preconditions:
  - "The cycle is in PROPOSALS, INCUMBENT_TERMS or REVISION."
main_flow:
  - The AO opens a submitted proposal and checks it against the TOR.
  - The AO clicks **Validate**; the proposal becomes VALIDATED.
alternate_flows:
  - Reject. The AO rejects it with a reason; the insurer is notified.
  - Entry by the AO. The AO enters the proposal on behalf of the insurer with the insurer's document attached.
rules:
  - [R1, "Only validated proposals count in the comparative.", Fixed, "-"]
  - [R2, "A new version supersedes the earlier one; history kept (FR-EB-003).", Fixed, "-"]
validations:
  - [Reject without reason, "Select a reason for 'reject'", WORKFLOW_REASON_REQUIRED]
  - [AO entry without the insurer's document, Attach the insurer's proposal document, To be confirmed]
notifications:
  - "The insurer on validation or rejection."
audit:
  - "Validation and rejection with user and time."
acceptance:
  - A submitted proposal is not in the comparative until validated.
  - A proposal entered by the AO shows the source AO and the attached document.
```

```fr
id: FR-EB-041
title: Prepare and sign off the comparative analysis
brd: [BRID-010 (p.16-17)]
actor: System; Marketing AO; authorised signatory (EB_COMPARATIVE_APPROVE)
priority: Must have
screens: Comparative page; Programme page (Comparative tab)
description:
  - BIBS compiles the comparative from all validated proposals of the cycle - coverage per TOR item, premium rates and premium per plan, exclusions, terms, additional benefits, and capability factors such as company stability, clinic providers, hospitals and technology. It is a structured table linked to the client, the cycle and the proposals, stored as a snapshot version.
  - The comparative is due 3 working days after the last proposal (default). BIBS warns when requests are still open; the AO closes them explicitly or waits. The AO marks the recommended proposal per line and submits the comparative for sign-off by an authorised signatory other than the maker. It can be viewed, exported (PDF, Excel) and printed.
preconditions:
  - "At least one validated proposal exists."
main_flow:
  - The AO clicks **Build Comparative**; BIBS creates the snapshot.
  - The AO reviews it, marks the recommendation and clicks **Submit for Sign-off**.
  - The signatory approves; BIBS evaluates the threshold rules (FR-EB-042) and moves the cycle to THRESHOLD_APPROVAL or READY_TO_PRESENT.
alternate_flows:
  - Open requests. BIBS lists the insurers that have not answered; the AO closes their requests or waits.
  - Return. The signatory returns the comparative with a reason; the AO compiles it again or edits it.
rules:
  - [R1, "Signatory never the maker.", Fixed, "-"]
  - [R2, "Due 3 working days after the last proposal (default); alert EB_COMPARATIVE_LATE.", Configurable, Parameter EB_COMPARATIVE_DAYS]
  - [R3, "Factors and scoring confirmed under EBQ10.", Configurable, LOV EB_CAPABILITY_FACTOR]
validations:
  - [Requests still open, "<n> insurer requests are still open. Close them or wait for the proposals", To be confirmed]
  - [No recommendation marked, Mark the recommended proposal of each line, "-"]
  - [Signatory is the maker, A comparative is signed off by someone other than its maker, To be confirmed]
notifications:
  - "The signatories on submission; the AO on sign-off or return."
audit:
  - "Snapshot versions, sign-off and return with users and times."
acceptance:
  - A comparative of three validated proposals shows one column per insurer and the lowest premium per plan.
  - The maker cannot sign off the comparative.
  - The comparative exports to PDF and Excel with the same content.
```

```fr
id: FR-EB-042
title: Trigger approval when a value threshold is met
brd: [BRID-016 (p.23)]
actor: System; BDOI Management (EB_THRESHOLD_APPROVE)
priority: Must have
screens: EB Setup (Threshold Rules); My Approvals; Comparative page
description: Threshold rules define, per benefit line or all lines, a measure (TSI or annual premium), an amount and currency, the approver permission and level, and effective dates. BIBS evaluates them on the recommended proposal when the comparative is signed off, and again on client confirmation. When a rule is met, the cycle waits in THRESHOLD_APPROVAL; approvers are notified and see the item in My Approvals, and placement is blocked until approval. Administrators maintain the rules with maker-checker.
preconditions:
  - "A comparative is signed off or the client confirms."
main_flow:
  - BIBS evaluates the rules; a rule matches.
  - BIBS moves the cycle to THRESHOLD_APPROVAL and notifies the approvers.
  - An approver reviews the comparative and approves; the cycle moves on.
alternate_flows:
  - Return. The approver returns the comparative with a reason.
  - A new rule matches at confirmation. The cycle returns to THRESHOLD_APPROVAL.
rules:
  - [R1, "Seed values TSI >= 500M and premium >= 20M are examples; BDOI gives the values and levels (EBQ11).", Configurable, Threshold Rules (EB_SETUP)]
  - [R2, "The AO of the cycle never approves the threshold.", Fixed, "-"]
validations:
  - [Placement triggered while approval pending, "Cycle <no.> waits for the threshold approval", To be confirmed]
  - [Rule amount not positive, Enter an amount greater than zero, "-"]
fields_screen: Threshold rule
fields:
  - [Benefit line, List, "No", LOV EB_BENEFIT_LINE, Blank = all lines]
  - [Measure, List, "Yes", TSI / Annual premium, "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Currency, List, "Yes", Currency master, Default PHP]
  - [Approver permission and level, List, "Yes", Permissions, Default EB_THRESHOLD_APPROVE]
  - [Effective from / to, Date, "Yes", "-", To after from]
notifications:
  - "The approvers on entry; the AO on approval or return."
audit:
  - "Rule changes with maker and checker; approvals with user and time."
acceptance:
  - A comparative with a recommended annual premium above the rule amount waits for Management approval.
  - Placement cannot be triggered before the threshold approval.
  - A rule changed by the administrator applies after authorisation without a change to the system.
```

```fr
id: FR-EB-043
title: Present the comparative to the client
brd: [BRID-011 (p.18)]
actor: Marketing AO; client HR user
priority: Must have
screens: Comparative page (Present to Client); Portal - Comparative
description: The AO presents the approved comparative - table, summary and a downloadable PDF - to the client. BIBS publishes it on the client portal and sends it by protected e-mail, and notifies the HR contacts. The client HR user reads it in the portal and gives feedback or asks for clarification in a comment thread on the same page; the AO is notified and replies in the thread.
preconditions:
  - "The cycle is READY_TO_PRESENT."
main_flow:
  - The AO clicks **Present to Client**.
  - BIBS publishes the comparative and sends it; the cycle moves to WITH_CLIENT.
  - The HR user comments; the AO replies.
rules:
  - [R1, "Only an approved comparative can be presented.", Fixed, "-"]
validations:
  - [Comment empty, Enter your comment, "-"]
notifications:
  - "The HR contacts on presentation; the AO on each client comment."
audit:
  - "Presentation and comments with user or portal user and time."
acceptance:
  - The client HR user sees the comparative in the portal and downloads the PDF.
  - A client comment notifies the AO.
```

```fr
id: FR-EB-044
title: Capture client changes and relay them to insurers
brd: [BRID-012 (p.19-20)]
actor: Marketing AO; client HR user
priority: Must have
screens: Programme page (Revisions); Portal tasks
description: The AO captures the client's changes, additions or amendments as items (a TOR item or free text with the requested change) with documents, and selects the insurers to relay them to. BIBS relays the revision as a portal task and e-mail per insurer, tracks each insurer's status (open, answered) and confirms the relay. An empty revision is refused. The cycle moves to REVISION.
preconditions:
  - "The cycle is WITH_CLIENT."
main_flow:
  - The AO clicks **Request Revision** and enters the items.
  - The AO selects the insurers and relays the revision.
  - BIBS creates the insurer tasks and confirms.
rules:
  - [R1, "A revision has at least one item with a requested change.", Fixed, "-"]
validations:
  - [Revision without items, Add at least one requested change, To be confirmed]
  - [No insurer selected, Select the insurers, "-"]
notifications:
  - "Each selected insurer (portal task and e-mail)."
audit:
  - "Revision, items, targets and relay time."
acceptance:
  - A revision relayed to two insurers shows two open targets.
  - A revision with no items is refused.
```

```fr
id: FR-EB-045
title: Insurers update their proposals against a revision
brd: [BRID-015 (p.22-23)]
actor: Insurer user
priority: Must have
screens: Portal - Requests for Proposal (revision); Proposal page (Versions)
description: The insurer opens the revision task, which shows the requested items. The revised proposal is a new version that references the revision and must answer every requested item. BIBS notifies the AO, keeps the version history and shows the differences from the previous version. The target becomes answered.
preconditions:
  - The insurer has an open revision target.
main_flow:
  - The insurer opens the revision task and edits the proposal form.
  - The insurer answers each requested item and submits.
  - BIBS stores version n+1 and marks the target answered.
rules:
  - [R1, "Every requested item must be addressed.", Fixed, "-"]
validations:
  - [Requested item not addressed, "Answer the requested change <item>", To be confirmed]
notifications:
  - "The AO on submission."
audit:
  - "Version with portal user and time."
acceptance:
  - A revised proposal that skips a requested item is refused.
  - The proposal page shows version 2 with the changed items highlighted.
```

```fr
id: FR-EB-046
title: Record the client's confirmation and trigger placement
brd: [BRID-017 (p.24), BRID-019 (p.25-26)]
actor: Marketing AO; client HR user; System
priority: Must have
screens: Programme page (Confirm; Trigger Placement); Portal - Comparative (Confirm)
description:
  - The client's confirmation of the chosen proposal is recorded with its channel - in the system (portal), by e-mail or as a signed document; evidence is mandatory except for a portal confirmation. BIBS re-evaluates the threshold rules.
  - "**Trigger Placement** creates one draft account per benefit line of the chosen proposal (EB product of the line, insurer, premium, business type), links the placement documents (confirmation, proposal, TOR, master list, BOR) to each account, submits it to Processing and notifies Processing and the insurer. It is blocked while a threshold approval, a required placement document or, for a non-accredited provider, the ISACOM approval is missing (EBQ24)."
  - The cycle follows the account stages (placed, policy issued, booked) and becomes PLACED when all its accounts are booked.
preconditions:
  - "The cycle is WITH_CLIENT (confirmation) or CONFIRMED (trigger)."
main_flow:
  - The AO records the confirmation with its evidence, or the HR user confirms in the portal.
  - The AO clicks **Trigger Placement**.
  - BIBS checks approvals and documents, creates the accounts and submits them.
  - Processing receives the accounts in the placement queue.
alternate_flows:
  - Placement document missing. BIBS lists it; nothing is created.
  - Confirmation of the wrong proposal. The AO voids the confirmation before the trigger and records it again.
rules:
  - [R1, "One account per benefit line; business type from the cycle; a renewal account refers to the expiring ARN.", Fixed, "-"]
  - [R2, "Accepted evidence of confirmation confirmed under EBQ12.", Configurable, Change request after EBQ12]
  - [R3, "Placement documents from the required documents of NB_PLACEMENT / RENEWAL_PLACEMENT.", Configurable, Required Documents]
validations:
  - [Confirmation without evidence, Attach the client's confirmation, To be confirmed]
  - [Threshold approval pending, "Cycle <no.> waits for the threshold approval", To be confirmed]
  - [Placement document missing, "Add the required document <type> before placement", To be confirmed]
fields_screen: Client confirmation
fields:
  - [Chosen proposal per line, List, "Yes", Validated proposals, One per line]
  - [Channel, List, "Yes", System / E-mail / Signed document, "-"]
  - [Evidence, Attachment, Cond., "-", Required unless System]
  - [Confirmation date, Date, "Yes", "-", Not after today]
notifications:
  - "Processing and the insurer on the trigger; the AO as the accounts progress."
audit:
  - "Confirmation, trigger and created ARNs."
acceptance:
  - Triggering placement for an HMO and a GLI line creates two accounts with the cycle's business type.
  - The trigger is refused while the threshold approval is pending.
  - The cycle becomes PLACED when both accounts are booked.
```

## Processing, booking, billing and member servicing

```fr
id: FR-EB-050
title: Assign, reassign and return work with remarks
brd: [BRID-018 (p.25)]
actor: Processing; Processing Supervisor; TL (WORK_ASSIGN)
priority: Must have
screens: My Work queues; workflow panels of the EB and account records
description: Authorised users assign, reassign and return EB work items (accounts in placement, member changes, SOAs, portal uploads) with remarks; returns carry a reason and feedback that the maker sees. The Processing Supervisor approval applies only where BDOI defines such a step (EBQ23).
preconditions:
  - "The user holds WORK_ASSIGN (assign) or owns the stage (return)."
main_flow:
  - The supervisor selects work items and assigns them to a processor.
  - The processor works an item or returns it with a reason and remarks.
rules:
  - [R1, "Returns need a reason.", Fixed, "-"]
validations:
  - [Return without reason, "Select a reason for 'return'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "The assignee on assignment; the maker on return."
audit:
  - "Assignment and return with remarks in the case history."
acceptance:
  - A supervisor reassigns a member change; the new processor is notified.
  - A returned item shows the reason and remarks to the AO.
```

```fr
id: FR-EB-051
title: Track policy issuance after placement
brd: [BRID-019 (p.25-26), BRID-005.01 (Add. p.7-8)]
actor: Processing; insurer user
priority: Must have
screens: Account page (BRD-1); Programme page (Cycle tab); Pending Items
description: After the trigger, Processing completes placement and issuance on the BRD-1 screens with the EB documents already linked to each account. Insurers upload the policy form or contract through the portal; once Processing validates it (FR-EB-013) it is attached as the account's e-policy. The cycle shows the account stages so the AO sees progress, and the contract is a tracked item until received (FR-EB-057).
preconditions:
  - "The account was created by the placement trigger."
main_flow:
  - Processing places the account with the insurer.
  - The insurer uploads the policy form; Processing validates it.
  - Processing records the policy number and books the account (FR-EB-052).
rules:
  - [R1, "Access to the account follows the BRD-1 roles.", Fixed, "-"]
validations: []
notifications:
  - "The AO at each account stage change."
audit:
  - "As BRD-1 placement and issuance."
acceptance:
  - The EB documents linked at the trigger are on the account's Documents tab.
  - A validated policy form from the portal becomes the account's e-policy.
```

```fr
id: FR-EB-052
title: Book the account with an insurer billing number duplicate check
brd: [BRID-020 (p.26-27)]
actor: Processing
priority: Must have
screens: Book Account (BRD-1); Booking upload
description: For EB products the insurer's billing number is mandatory at booking, on screen and in the booking upload. The combination company, insurer and billing number is unique; a duplicate blocks the booking and notifies the user. Success and duplicate outcomes are notified; the booking audit records the user, time and billing number.
preconditions:
  - "The account is ready to book."
main_flow:
  - Processing enters the insurer billing number and books the account.
  - BIBS checks for duplicates and books.
alternate_flows:
  - Duplicate billing number. BIBS refuses the booking and names the existing invoice.
rules:
  - [R1, "Billing number mandatory for the product lines in the parameter (EB lines).", Configurable, Parameter BOOKING_BILLING_NO_LINES]
  - [R2, "Which number is the billing number and its scope are confirmed under EBQ18.", Configurable, Change request after EBQ18]
validations:
  - [Billing number blank for an EB product, Enter the insurer billing number, "-"]
  - [Billing number already used, "Billing number <no.> of <insurer> is already on invoice <invoice>", To be confirmed]
fields_screen: Book Account (EB)
fields:
  - [Insurer billing number, Text, Cond., "-", "Required for EB lines; up to 60 characters; unique per insurer"]
notifications:
  - "Processing and the AO on booking or duplicate."
audit:
  - "Booking with user, time and billing number."
acceptance:
  - Booking an HMO account without a billing number is refused.
  - A second booking with the same insurer and billing number is refused.
```

```fr
id: FR-EB-053
title: Receive, validate and release the SOA and track payment
brd: [BRID-021 (p.27-28)]
actor: Insurer user; Processing; Collection
priority: Must have
screens: SOA Register; Portal - SOA and Policy Forms; Programme page (Billing & SOA)
description:
  - The insurer's SOA (PDF or Excel) is received through the portal or uploaded by the AO. BIBS refuses a duplicate SOA (same insurer and SOA number, or the same file) and a corrupted file. Processing validates it (within 3 working days by default) and releases it to the client HR user and to Collection; a rejected SOA carries a reason.
  - The SOA is linked to the booked invoices. The payment status shown is the invoice ledger's; when an invoice becomes paid, BIBS notifies the AO and Collection with the document links.
preconditions:
  - "The programme has a booked account."
main_flow:
  - The SOA arrives; BIBS registers it as RECEIVED (EBS-yyyy-nnnnnn).
  - Processing validates it and links the invoices.
  - Processing releases it; the client and Collection are notified.
  - Payment is applied in Cashiering; BIBS shows PAID and notifies.
alternate_flows:
  - Reject. Processing rejects the SOA with a reason; the insurer is notified.
rules:
  - [R1, "Validation due 3 working days after receipt (default); alert EB_SOA_VALIDATION_LATE.", Configurable, Parameter EB_TAT_SOA_VALIDATION]
  - [R2, "EB keeps no copy of the payment status; it reads the invoice ledger.", Fixed, "-"]
validations:
  - [Duplicate SOA, "SOA <no.> of <insurer> is already registered", To be confirmed]
  - [Reject without reason, "Select a reason for 'reject'", WORKFLOW_REASON_REQUIRED]
fields_screen: SOA intake
fields:
  - [Insurer, List, "Yes", Insurers of the programme, "-"]
  - [SOA number, Text, "Yes", "-", Unique per insurer]
  - [Period, Date range, "Yes", "-", To after from]
  - [Amount, Amount, "Yes", "-", ">= 0"]
  - [File, Attachment, "Yes", "-", PDF or Excel]
  - [Invoices, Multi-select, "No", Booked invoices of the programme, "-"]
notifications:
  - "The client HR user and Collection on release; the AO and Collection when an invoice is paid."
audit:
  - "Intake, validation, rejection and release with user and time."
acceptance:
  - A second SOA with the same number from the same insurer is refused.
  - A released SOA is visible to the client HR user in the portal.
  - Paying the invoice in Cashiering notifies the AO and Collection.
```

```fr
id: FR-EB-054
title: Maintain the member roster from the master list
brd: [BRID-013 (p.20), BRID-014 (p.20-22)]
actor: Marketing AO; client HR user
priority: Must have
screens: Programme page (Members tab); Bulk Upload (EB_MASTERLIST)
description: The roster holds, per programme and policy year, each member - employee number, name, birth date, gender, civil status, plan, dependants, effective and end dates, status. It is loaded from the master list through the template (column mapping, row validation) as a staged version. The AO reviews the staged version (headcount, additions and removals against the current one) and accepts or rejects it; an accepted version replaces the roster. The roster holds no health data (EBQ15).
preconditions:
  - "The programme has a policy year."
main_flow:
  - The AO or HR user uploads the master list with the template.
  - BIBS validates every row and creates a staged version.
  - The AO reviews the differences and clicks **Accept Roster**.
rules:
  - [R1, "One employee number per programme and policy year in an accepted version.", Fixed, "-"]
  - [R2, "Roster fields confirmed under EBQ15; minimal fields until then.", Configurable, Master list template]
validations:
  - [Duplicate employee number, "Row <n>: employee <no.> appears twice", To be confirmed]
  - [Mandatory column blank, "Row <n>: <field> is required", To be confirmed]
notifications:
  - "The AO when a staged version is waiting."
audit:
  - "Staged, accepted and rejected versions with user and time."
acceptance:
  - A master list with a duplicate employee number is refused with the row named.
  - An accepted roster version replaces the previous one; the earlier version stays readable.
```

```fr
id: FR-EB-055
title: Capture member changes and relay them to the insurer
brd: [BRID-013 (p.20), BRID-025 (Add. p.11)]
actor: Marketing AO; client HR user; insurer user; Processing
priority: Must have
screens: Member Changes; Portal - Member Changes; Bulk Upload (EB_MEMBER_CHANGE)
description:
  - Member changes (new hires, deletions, plan changes, data changes) are entered on screen or uploaded by the AO or the client HR user, validated against the roster and linked to the programme and policy. BIBS relays them to the insurer as a portal task and e-mail. The insurer confirms and bills; Processing validates; the change closes.
  - A change with a premium effect raises an endorsement request in Operations (BRD-2), which re-rates and posts it. Under the parameter "no payment, no booking on adjustment" the request is raised only after the billing is paid (EBQ16).
preconditions:
  - "The programme has an accepted roster."
main_flow:
  - The AO or HR user captures the change lines with the effective date.
  - BIBS validates them and numbers the change EBM-yyyy-nnnnnn.
  - The AO relays it; the insurer bills it (FR-EB-056).
  - Processing validates it; BIBS raises the endorsement request if financial and closes the change.
alternate_flows:
  - Return. Processing returns the change to the AO with a reason.
  - Cancel. The AO cancels a change before it is relayed.
rules:
  - [R1, "Change types - add, delete, change plan, change data.", Configurable, LOV EB_MEMBER_CHANGE_TYPE]
  - [R2, "Endorsement request only after payment when the parameter is on (default off).", Configurable, Parameter EB_ADJ_BOOKING_REQUIRES_PAYMENT]
validations:
  - [Delete of a member not on the roster, "Employee <no.> is not on the roster", To be confirmed]
  - [Add of an existing member, "Employee <no.> is already on the roster", To be confirmed]
  - [Effective date outside the policy period, The effective date must be within the policy period, To be confirmed]
fields_screen: Member change line
fields:
  - [Action, List, "Yes", LOV EB_MEMBER_CHANGE_TYPE, "-"]
  - [Employee number, Text, "Yes", Roster, "Must exist (delete, change); must not exist (add)"]
  - [Member data, Fields, Cond., Master list template, Required for add and change data]
  - [Plan, List, Cond., Plans of the line, Required for add and change plan]
  - [Effective date, Date, "Yes", "-", Within the policy period]
notifications:
  - "The insurer on relay; Processing when billed; the AO on return."
audit:
  - "Every change, line and stage with user and time."
acceptance:
  - A new hire entered by the HR user is relayed to the insurer as a task.
  - Deleting an employee who is not on the roster is refused.
  - A validated financial change raises an endorsement request with reference EBM-yyyy-nnnnnn.
```

```fr
id: FR-EB-056
title: Upload insurer direct billing for member changes
brd: [BRID-025 (Add. p.11)]
actor: Marketing AO; insurer user
priority: Must have
screens: Member Changes (Billing); Portal - Member Changes
description: The AO uploads the insurer's direct billing documents for member changes (or the insurer uploads them in the portal). Each document is linked to the member change, the endorsement type, the client and the policy, with the Marketing, Processing and Collection access classes (FR-EB-002). The change is flagged as direct-billed. Processing validates completeness; Collection opens the billing directly.
preconditions:
  - "The member change is RELAYED."
main_flow:
  - The AO uploads the direct billing on the member change.
  - BIBS links it and moves the change to BILLED.
  - Processing and Collection are notified.
rules:
  - [R1, "Who collects from the client for direct-billed changes is confirmed under EBQ17.", Configurable, Change request after EBQ17]
validations:
  - [Billing not linked to a member change, Select the member change of the billing, To be confirmed]
notifications:
  - "Processing and Collection on upload."
audit:
  - "Upload with user and time."
acceptance:
  - A direct billing uploaded on a member change is opened by a Collection user.
  - The member change shows "Direct billed".
```

```fr
id: FR-EB-057
title: Monitor contracts, HMO cards and billing with automatic follow-ups
brd: [BRID-030 (p.34-35)]
actor: System; Marketing AO; Processing
priority: Must have
screens: Pending Items; Programme page (Pending Items tab); EB Home
description:
  - BIBS creates a tracked item automatically for each expected deliverable - the contract after placement, the HMO card for each added member, a card replacement on request, the billing for each member change - with the responsible party (insurer, client or BDOI), status (pending, received, released, closed) and due date. Tags update automatically when the related upload is validated or the item is updated.
  - A daily job sends follow-up e-mails for items past due and escalates after a set number of follow-ups. Users view and filter pending items by programme, member, type, party and age.
preconditions:
  - "None."
main_flow:
  - A trigger (placement, member added, member change) creates the tracked item.
  - The job at 07:00 PHT sends follow-ups for past-due items.
  - A validated upload or a manual update closes the item.
rules:
  - [R1, "Follow-up every 5 working days past due (default).", Configurable, Parameter EB_FOLLOWUP_DAYS]
  - [R2, "Escalation after 3 follow-ups (default).", Configurable, Parameter EB_FOLLOWUP_MAX]
  - [R3, "Statuses, thresholds and recipients confirmed under EBQ20.", Configurable, LOV EB_TRACKED_ITEM_TYPE]
validations:
  - [Close without a date received, Enter the date received, "-"]
notifications:
  - "Follow-up e-mails to the responsible party; EB_ITEM_OVERDUE to the AO on escalation."
audit:
  - "Item creation, status changes and follow-ups."
acceptance:
  - Adding a member creates a pending HMO card item.
  - An item 5 working days past due receives a follow-up e-mail.
  - The Pending Items list filters by member.
```

## Reports and monitoring

```fr
id: FR-EB-060
title: EB reports
brd: [BRID-022 (p.28-30)]
actor: Marketing, Processing, Collection, Management (EB_REPORT_VIEW)
priority: Must have
screens: Reports (category Employee Benefits)
description:
  - Reports are generated on request or on a schedule. The Report List of 13-May-2026 sets the schedules - Production, Renewal and New Business weekly, Placement monthly, all also as needed; each scheduled run is sent to its subscribers. Production (premium and commission by team, AO, line, insurer and business type), Renewal (programmes due with expiry, RA sent date, status, proposals, comparative result, client changes, BOR, TOR and remarketing actions), Placement (confirmation, insurer selected, coverage, commission, counter-proposals and revisions, final terms, document uploads and confirmation dates), Pending Items and Franchise.
  - Parameters include activity, team, AO, client, benefit line, insurer, business type and date range. Reports read live data, can be viewed, downloaded, printed and shared by e-mail from the report runner, and saved as variants.
preconditions:
  - The user has EB_REPORT_VIEW.
main_flow:
  - The user selects a report and the parameters.
  - BIBS runs it and shows the result.
  - The user exports it or e-mails the export.
rules:
  - [R1, "Placement columns from the Report List of 13-May-2026; Renewal and New Business share the columns of the EB sales report template that BDOI supplies (CLR-EB-09); columns of section 6.1 until then.", Configurable, Report definitions]
  - [R2, "Schedules - Production, Renewal and New Business weekly; Placement monthly.", Configurable, Report schedules (subscriptions)]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each run, export and share with user and parameters."
acceptance:
  - The Renewal report lists a programme with its RA sent date and the number of proposals received.
  - The Production report filtered by team BDO shows only that team's programmes.
  - The weekly Renewal report and the monthly Placement report reach their subscribers on schedule.
```

```fr
id: FR-EB-061
title: New Business report, business type filter and Word export
brd: [BRID-022.01 (Add. p.10-11)]
actor: Marketing, Management (EB_REPORT_VIEW)
priority: Must have
screens: Reports (EB-NEW-BUSINESS; NB-BOOKED-REG, NB-PRODUCTION, NB-PLC-UPDATE)
description: The New Business report lists the new client accounts and policies of a period with client, line or product, effective date, insurer selected, premium, commission, AO, business type, date and status. The business type is required on every account (FR-EB-021, shared work item BT0) and is a filter on the EB reports and on the BRD-1 booked register, production and placement update reports. Every report exports to PDF, Excel, CSV and Word, within the user's authorisation.
preconditions:
  - The user has EB_REPORT_VIEW.
main_flow:
  - The user runs the New Business report for a period.
  - The user exports it to Word.
rules:
  - [R1, "Word export applies to every report (P2); whether BDOI needs it beyond EB is confirmed under EBQ21.", Fixed, "-"]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Runs and exports logged."
acceptance:
  - The Production report filtered by business type Renewal excludes new-business accounts.
  - The New Business report exports to Word with the same rows as the PDF.
```

```fr
id: FR-EB-062
title: TAT monitoring, EB home and alerts
brd: [BRID-022 (p.28-30), "TAT annex (p.42)"]
actor: Marketing, Processing, Collection, Management
priority: Must have
screens: EB Home; Reports (EB-TAT)
description:
  - Each service records the received and released time stamps of the activities of the TAT annex (section 5.6). The TAT report shows, per activity, team or AO, the days taken and the breaches of the target. Alerts warn the owners of overdue franchise decisions, proposals, comparatives, SOA validations and tracked items.
  - EB Home shows tiles for RA due, awaiting feedback, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, with client, member changes open, pending items overdue and portal uploads to review; each opens its list.
preconditions:
  - "The user has EB_VIEW."
main_flow:
  - The user opens EB Home and clicks a tile.
  - The manager runs the TAT report for a month.
rules:
  - [R1, "TAT targets per activity (section 5.6).", Configurable, Parameters EB_TAT_*]
  - [R2, "Start and stop events per activity confirmed under EBQ21.", Configurable, Report definition]
validations: []
notifications:
  - "EB_FRANCHISE_OVERDUE, EB_PROPOSAL_OVERDUE, EB_COMPARATIVE_LATE, EB_SOA_VALIDATION_LATE, EB_ITEM_OVERDUE, PORTAL_UPLOAD_WAITING."
audit:
  - "Report runs logged; alerts logged."
acceptance:
  - A franchise decided in 7 working days appears as a breach in the TAT report.
  - The tile "Portal uploads to review" opens the review queue.
```

# Workflow and status model

## EB cycle

Figure 2 shows the workflow EB_CYCLE. Solid arrows are the main path, dashed arrows are returns and optional paths, and the dotted arrow closes a cycle without placement.

![Workflow EB_CYCLE: stages and actions](figures/brd08_cycle_workflow.dot){width=17}

<!-- table: widths=4.2,5.8,6.6 caption="Stages of EB_CYCLE" size=8.5 -->
| Stage | Owner (permission) | Business actions |
|---|---|---|
| OPEN | System, AO (EB_MARKET) | Send RA (renewal); start (new business) |
| RA_SENT | AO (EB_MARKET) | Record feedback; reminders run |
| REQUIREMENTS | AO (EB_MARKET) | Documents, BOR, indicative proposal; stay with incumbent or remarket |
| INCUMBENT_TERMS | AO (EB_MARKET) | Validate the incumbent's terms; build comparative |
| FRANCHISE | AO; insurers (portal) | Franchise requests and decisions; release TOR |
| PROPOSALS | Insurers (portal); AO | Requests answered; proposals validated; build comparative |
| COMPARATIVE | AO (EB_MARKET) | Recommend; submit for sign-off |
| FOR_SIGNOFF | Signatory (EB_COMPARATIVE_APPROVE) | Approve or return |
| THRESHOLD_APPROVAL | Management (EB_THRESHOLD_APPROVE) | Approve or return |
| READY_TO_PRESENT | AO (EB_MARKET) | Present to client |
| WITH_CLIENT | Client HR; AO | Comment; request revision; confirm; close lost / not renewed |
| REVISION | AO; insurers | Relay; revised proposals; compile the comparative again |
| CONFIRMED | AO (EB_MARKET) | Trigger placement |
| IN_PLACEMENT | Processing | Placement, issuance and booking of the accounts (BRD-1) |
| PLACED | - | Terminal; servicing continues on the programme |
| CLOSED_LOST, NOT_RENEWED | - | Terminal, with reason (LOV EB_LOST_REASON) |

<!-- table: widths=4.6,3.6,4.6,3.8 caption="Main transitions of EB_CYCLE" size=8 -->
| From | Action | To | Condition |
|---|---|---|---|
| OPEN | Send Renewal Advice | RA_SENT | Renewal cycle; job or AO |
| OPEN | Start Requirements | REQUIREMENTS | New-business cycle |
| RA_SENT | Record Feedback | REQUIREMENTS | Feedback recorded |
| REQUIREMENTS | Stay with Incumbent | INCUMBENT_TERMS | Renewal without remarketing |
| REQUIREMENTS | Remarket | FRANCHISE | Validated BOR |
| FRANCHISE | Release TOR | PROPOSALS | At least one franchise APPROVED; TOR released |
| INCUMBENT_TERMS, PROPOSALS, REVISION | Build Comparative | COMPARATIVE | At least one validated proposal |
| COMPARATIVE | Submit for Sign-off | FOR_SIGNOFF | Recommendation marked |
| FOR_SIGNOFF | Sign Off | THRESHOLD_APPROVAL or READY_TO_PRESENT | Signatory not the maker; rule matched or not |
| FOR_SIGNOFF, THRESHOLD_APPROVAL | Return to AO | COMPARATIVE | Reason |
| THRESHOLD_APPROVAL | Approve | READY_TO_PRESENT | Rule's approver permission |
| READY_TO_PRESENT | Present to Client | WITH_CLIENT | Approved comparative |
| WITH_CLIENT | Request Revision | REVISION | At least one item |
| WITH_CLIENT | Record Confirmation | CONFIRMED | Evidence; threshold re-evaluated |
| CONFIRMED | Trigger Placement | IN_PLACEMENT | Approvals and placement documents present |
| IN_PLACEMENT | All Accounts Booked (automatic) | PLACED | All accounts booked |
| any open stage | Close as Lost / Close as Not Renewed | CLOSED_LOST / NOT_RENEWED | Reason |

## Franchise

![Workflow EB_FRANCHISE](figures/brd08_franchise.dot){width=10}

A franchise request is DRAFT until submitted with the BOR and the required documents. The insurer approves or rejects it in the portal (or the AO records the decision with evidence). The job expires a request without a decision after the TAT and a grace period. Every decided request is advised to the client (FR-EB-033).

## Member changes

![Workflow EB_MEMBER_CHANGE](figures/brd08_member_change.dot){width=10}

A member change is CAPTURED by the AO or the client HR user, RELAYED to the insurer, BILLED when the insurer's billing or the AO's direct billing upload arrives, VALIDATED by Processing and CLOSED. A financial change raises an endorsement request in Operations on validation. Processing returns a change to CAPTURED with a reason; the AO cancels a change that was not relayed.

## SOA

![Workflow EB_SOA](figures/brd08_soa.dot){width=9}

An SOA is RECEIVED from the portal or the AO, VALIDATED by Processing and RELEASED to the client HR user and Collection, or REJECTED with a reason.

## Portal upload review

![Workflow PORTAL_UPLOAD_REVIEW](figures/brd08_portal_review.dot){width=9}

A portal upload stays RECEIVED until it passes the virus scan and the owner of its target validates it; the owner can re-map it to another target of the same party before validation.

## Turn-around times

The TAT annex (p.42) gives the targets below. They are parameters; the TAT report measures them (FR-EB-062).

<!-- table: widths=4.8,3.6,4.2,4 caption="Turn-around times (TAT annex, p.42)" size=8 -->
| Activity | Responsible | TAT | Parameter |
|---|---|---|---|
| Submission of request for franchise | BDOI Marketing | 1-3 working days upon complete documents | EB_TAT_FRANCHISE_SUBMIT |
| Franchise approval | Insurance provider | 1-5 working days upon request | EB_FRANCHISE_TAT_DAYS |
| Submission of request for quotation (new) | BDOI Marketing | 1-3 working days upon franchise approval | EB_TAT_RFQ_SUBMIT |
| Submission of renewal advice | BDOI Marketing | 135 days before inception | EB_RA_LEAD_DAYS |
| Request renewal requirements | BDOI Marketing | 135 days before inception | EB_RA_LEAD_DAYS |
| Issuance of quotation (new / renewal) | Insurance provider | 1-5 working days upon complete documents | EB_PROPOSAL_REPLY_DAYS |
| Sending of proposal to client | BDOI Marketing | 1-5 working days from receipt of proposal | EB_COMPARATIVE_DAYS |
| Sending of client's confirmation | BDOI Marketing | 1-2 working days upon client confirmation | EB_TAT_CONFIRMATION |
| Sending of certificate of cover | Insurance provider | Within 1 working day | EB_TAT_COC |
| Request for placement and booking | BDOI Marketing | 1-5 working days upon client confirmation | EB_TAT_PLACEMENT_REQUEST |
| Submission of placement slip | BDOI Processing | 1-3 days upon request | EB_TAT_PLACEMENT_SLIP |
| Policy / contract and billing / SOA | Insurance provider | 1-10 working days from placement slip | EB_TAT_POLICY_SOA |
| Validation of SOA | BDOI Processing | Within 3 working days upon final SOA | EB_TAT_SOA_VALIDATION |
| Booking (EBIX in the BRD; BIBS) | BDOI Processing | Within 3 working days upon request | EB_TAT_BOOKING |
| Checking of policy / contract | BDOI Processing | Within 3 working days upon request | EB_TAT_POLICY_CHECK |
| Releasing of SOA and policy | BDOI Processing | Within 3 working days upon request | EB_TAT_RELEASE |
| Adjustments (inclusion / deletion) | BDOI Marketing / Processing | 1-3 working days per step | EB_TAT_MEMBER_CHANGE |
| Collection of premium | BDOI Collection | 1-2 working days upon SOA / billing receipt | EB_TAT_COLLECTION |
| Submission, validation, release of cards | Insurance provider / COG | 5-10 working days depending on volume | EB_TAT_CARDS |
| OR submission | Insurance provider | Weekly | EB_TAT_OR |

# Reports and documents

## Reports

<!-- table: widths=3.8,4,6.6,2.2 caption="Employee Benefits reports (category Employee Benefits)" size=8.5 -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| EB-PRODUCTION | Production | Premium and commission by team, AO, line, insurer and business type; weekly and as needed | BRID-022 |
| EB-RENEWAL | Renewal | Programmes due for renewal and their progress; weekly and as needed | BRID-022 |
| EB-PLACEMENT | Placement | Placed programmes, terms and confirmation dates; monthly and as needed | BRID-022 |
| EB-NEW-BUSINESS | New Business | New client programmes of a period, from prospecting to placement; weekly and as needed | BRID-022.01 |
| EB-TAT | Turn-around Time | Days and breaches per TAT activity | BRID-022 |
| EB-PENDING-ITEMS | Pending Items | Contracts, cards and billing pending, age, follow-ups | BRID-030 |
| EB-FRANCHISE | Franchise | Franchise requests, outcomes and response times per insurer | BRID-026, 027 |
| NB-BOOKED-REG, NB-PRODUCTION, NB-PLC-UPDATE (existing) | BRD-1 reports | Gain the business type filter | BRID-022.01 |

All EB reports need EB_REPORT_VIEW, export to PDF, XLSX, CSV, ODS and Word, support saved variants, and take the common parameters company, date range, team, AO, client, benefit line, insurer and business type. The frequencies and the Placement fields are those of the Report List of 13-May-2026 (R8); the Renewal and New Business reports share the fields of the template "EB Sales Report N_2026 PRODUCTION" named in the Report List, which BDOI supplies (CLR-EB-09). Until then the layouts below are the project's proposal (EBQ21).

### Renewal report (EB-RENEWAL)

<!-- table: widths=3.8,2.6,10.2 caption="EB-RENEWAL columns (BRID-022 AC5)" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Programme / Client | Text | Programme number and client name |
| Benefit line | Text | HMO, GLI, GPA |
| Team / AO | Text | EB team and account officer |
| Expiry | Date | End of the current policy |
| RA sent | Date | Date the RA was sent |
| Status | Text | Pending, confirmed, declined (cycle stage) |
| Proposals received | Number | Validated proposals |
| Comparative result | Text | Recommended insurer and premium |
| Client changes | Number | Revision requests |
| BOR / TOR / remarketing | Text | BOR status, TOR version, remarketing yes / no |

### Placement report (EB-PLACEMENT)

<!-- table: widths=3.8,2.6,10.2 caption="EB-PLACEMENT columns (BRID-022 AC6; Report List p.27)" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Programme / Client | Text | Programme number and client name |
| Client confirmation | Date | Confirmation date and channel |
| Insurer selected | Text | Chosen insurer per line |
| Coverage | Text | Benefit line and plans |
| Premium / Commission | Amount | Annual premium and commission |
| Counter-proposals and revisions | Number | Revisions and proposal versions |
| Final terms | Text | Chosen proposal version |
| Document uploads | Date | Last upload per required document |
| ARN / Stage | Text | Accounts created and their stage |
| Cover No. / Version No. | Text | Cover of the placed account (Report List) |
| Line of insurance / Comm. rate / Inception date | Text / Rate / Date | Report List key fields |
| BIR 2303, masterlist, BDOI proposal to client, COC | Text | Availability of each document (Yes / No, date) |
| ISACOM | Text | Where needed |
| GM / TH approval | Text | Approval of a commission deviation or late placement |
| Status | Text | Placement status |

### New Business report (EB-NEW-BUSINESS)

Columns (Add. p.10): Client; Line of business / product; Effective date; Insurer selected; Premium; Commission; Account officer; Business type; Date; Status. Parameters: period, team, AO, benefit line.

## Documents

<!-- table: widths=4.4,5.2,7 caption="EB document types and access classes (spec R3, section 6.1)" size=8.5 -->
| Document type | Access classes | Source |
|---|---|---|
| RENEWAL_ADVICE | Marketing, Processing (and the contact centre, XQ04) | System |
| EB_CLIENT_FEEDBACK | Marketing | AO or client portal |
| EB_BOR | Marketing, Processing | AO or client |
| EB_TOR | Marketing, Processing | AO |
| EB_MASTERLIST | Marketing, Processing | Client HR or AO |
| EB_MASTERLIST_UNNAMED | Marketing | Incumbent insurer |
| EB_UTILIZATION | Marketing | Client HR or incumbent |
| EB_INDICATIVE_PROPOSAL | Marketing | Incumbent or AO |
| EB_PROPOSAL | Marketing | Insurer portal |
| EB_COMPARATIVE | Marketing | System |
| EB_FRANCHISE_FORM | Marketing | AO |
| EB_CLIENT_CONFIRMATION | Marketing, Processing | Client |
| EB_POLICY_FORM / EPOLICY | Marketing, Processing, Collection | Insurer |
| EB_DIRECT_BILLING | Marketing, Processing, Collection | AO |
| EB_SOA | Processing, Collection | Insurer |
| EB_MEMBER_CHANGE | Marketing, Processing | Client HR or AO |
| EB_ISACOM_APPROVAL | Marketing | AO |

Templates (draft layouts until BDOI supplies its own, EBQ21): EB_RENEWAL_ADVICE, EB_RA_REMINDER, EB_INDICATIVE_PROPOSAL, EB_TOR, EB_RFP_COVER, EB_FRANCHISE_REQUEST, EB_FRANCHISE_ADVICE, EB_COMPARATIVE, EB_REVISION_RELAY, EB_ITEM_FOLLOWUP, PORTAL_INVITATION.

# Interfaces and integration

Figure 7 shows the interfaces. External parties reach only the portal; the portal hands the uploads and tasks of external users to the EB module. EB uses the data of the BRD-1 and BRD-2 modules.

![Interfaces of Employee Benefits and the partner portal (dashed = on hold)](figures/brd08_integration.dot)

> [!NOTE] Interfaces on hold
> An insurer system-to-system interface, e-signature verification of the BOR, HRIS feeds of master lists, reading insurer mailboxes and SMS notices wait for the questions of section 10.3. A virus scan of portal uploads is in place before the portal goes live.

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| Partner portal | In / Out | Sign-in, tasks, staged uploads, notices, downloads for insurer and client HR users | BRID-005, 014 | IN SCOPE |
| User Access Maintenance (BRD-11) | In | Approved External requests provision portal users (decision D7) | BRID-005, 014 | IN SCOPE |
| Client master (CRM) | Out | Prospect and client capture; EB tab on the client page | BRID-006 | IN SCOPE |
| Accounts (BRD-1, BT0) | Out | One account per line from the chosen proposal, with business type | BRID-017, 022.01 | IN SCOPE |
| Placement, issuance, booking (BRD-1) | Out | Placement queue; e-policy from validated policy forms; billing number at booking | BRID-019, 020 | IN SCOPE |
| Endorsement requests (BRD-2) | Out | Financial member changes | BRID-013 | IN SCOPE |
| Invoice ledger (BRD-2) | In | Payment status and payment events | BRID-021 | IN SCOPE |
| Documents | Out | Access classes, process tags, RENEWAL_ADVICE for Renewal and CSF | BRID-025; D3 | IN SCOPE |
| Outgoing e-mail | Out | Protected TOR, RA, comparative, advices, follow-ups; separate password | BRID-007 | IN SCOPE |
| Reports | Out | EB reports; Word export; business type filter | BRID-022, 022.01 | IN SCOPE |
| Insurer system-to-system interface | In | System-to-system proposals and billing | BRID-005.01 | ON HOLD |
| HRIS master lists | In | Payroll feeds of master lists | BRID-014 | ON HOLD |
| BOR e-signature | In | Verification of the signature | BRID-008 | ON HOLD |

# Non-functional requirements

<!-- table: widths=3,5.6,5.4 caption="Non-functional requirements (p.36-37; Add. p.14)" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Users | Marketing 13, Processing 4, Collection 3 (all concurrent); plus insurer and client HR portal users (not sized) | Within the BRD-1 sizing; portal sized separately (EBQ13) |
| Volumes | 2025 / 2026 transactions: BDO 400 / 550; SM 2,500 / 3,500; Voluntary 550 / 560; Solicited 700 / 900; New Business 50 / 55 | Low volume; the roster is the larger dataset and is indexed by programme and year |
| Peak | Month-end; 08:30-19:00 | Jobs run at 06:00 and 07:00, before the peak |
| Availability | 99.99%; used 08:30-19:00; maintenance per bank standard | 99.99% is above every other BRD (99.9%); HA deployment; the portal adds an internet-facing part (EBQ25, XQ08) |
| Recovery | RTO 4 hours; RPO 24 hours | Platform backup and recovery; one BIBS-wide NFR set is being agreed (XQ08) |
| Retention | Application, system and audit logs and historical data 5 years online, 15 years offline; backup every 4 hours, kept 7 years; no anonymisation | Retention rules EB_PROGRAMME and EB_MEMBER |
| Devices | Same speed on mobile and desktop | Responsive screens, portal included |
| Portal security (derived) | Portal or system-to-system interface with authentication, role-based access, audit, validation before effect (BRID-005, 014) | Separate sign-in, each user sees only the records of its party, lockout, one-time code, virus scanning, download log; BDO Information Security approval (EBQ13) |
| Data privacy (derived) | Master lists hold personal data; utilization reports are health-related | Treated as sensitive personal information under the Data Privacy Act: access classes, encryption in transit and at rest, download logging, no health data in the roster (EBQ15) |

# Configuration items

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.6,7.8 caption="Employee Benefits and portal parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| EB_RA_LEAD_DAYS | 135 | Days before expiry the RA is sent (EBQ02) |
| EB_RA_REMINDER_DAYS | 120, 105, 90 | Days before expiry of the RA reminders |
| EB_PROPOSAL_REPLY_DAYS | 5 | Working days insurers have to answer a request |
| EB_FRANCHISE_TAT_DAYS | 5 | Working days for the franchise decision |
| EB_FRANCHISE_ADVICE_DAYS | 2 | Working days to advise the client of the franchise outcome |
| EB_COMPARATIVE_DAYS | 3 | Working days from the last proposal to the comparative |
| EB_FOLLOWUP_DAYS | 5 | Working days between follow-ups of tracked items |
| EB_FOLLOWUP_MAX | 3 | Follow-ups before escalation |
| EB_TAT_* | Section 5.6 | TAT targets of the annex |
| EB_ADJ_BOOKING_REQUIRES_PAYMENT | false | Raise endorsement requests only after the billing is paid (EBQ16) |
| BOOKING_BILLING_NO_LINES | EB lines | Product lines that need the insurer billing number at booking |
| PORTAL_SESSION_MINUTES | 15 | Portal idle timeout |
| PORTAL_MAX_FAILED_LOGINS | 3 | Failed portal sign-ins before lockout |
| PORTAL_MFA_REQUIRED | true | E-mail one-time code at portal sign-in |
| PORTAL_INVITE_VALID_HOURS | 72 | Validity of a portal invitation |
| PORTAL_MAX_UPLOAD_MB | 10 | Maximum size of a portal upload |
| Job schedule (Renewal Advice) | 06:00 PHT daily | RA and reminders |
| Job schedule (tracked items) | 07:00 PHT daily | Tracked item follow-ups |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values provided |
|---|---|
| EB_BENEFIT_LINE | HMO; GLI; GPA (EBQ01) |
| EB_TEAM | BDO; SM; Voluntary; Solicited; New Business (EBQ22) |
| EB_PROCESS_TYPE | NB placement; Renewal placement; Endorsement; Adjustment; Franchise; Proposal |
| EB_CAPABILITY_FACTOR | Company stability; Clinic providers; Hospital network; Technology (EBQ10) |
| EB_MEMBER_CHANGE_TYPE | Add; Delete; Change plan; Change data (EBQ16) |
| EB_TRACKED_ITEM_TYPE | Contract; HMO card; Card replacement; Billing invoice (EBQ20) |
| EB_LOST_REASON, EB_FRANCHISE_REJECT_REASON, EB_SOA_REJECT_REASON, PORTAL_REJECT_REASON | Generic values provided; BDOI supplies its own |
| DOCUMENT_TYPE (EB values) | The types of section 6.2 |

## Masters and rules maintained by the business

<!-- table: widths=5,5.2,6.4 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Threshold rules | Business Administrator (authoriser) | FR-EB-042 |
| Required documents per process type and line | Business Administrator (authoriser) | FR-EB-034, 046 |
| Document access classes | System Administrator | FR-EB-002 |
| EB templates and master list template | System Administrator | FR-EB-004, 054 |
| EB products, lines and HMO providers as panel insurers | MBS through Product Maintenance (BRD-3) | FR-EB-046 |
| Portal users | AO request, approver decides (User Access) | FR-EB-011 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-EB-01 | The addendum (Add. p.1-17) overrides the main BRD (p.1-42); BRID-025 and 026 are read with the addendum text | R1, R2 |
| A-EB-02 | HMO providers are set up as panel insurers of the EB lines in the catalogue | EBQ01 |
| A-EB-03 | The BOR is required for new business and for renewals that remarket | EBQ05 |
| A-EB-04 | The BOR signature check is an attestation by the validator | EBQ06 |
| A-EB-05 | EB renewals are handled in this module; the general Renewal lists exclude EB lines | D3; EBQ28 |
| A-EB-06 | Until the portal is approved for go-live, the AO uploads on behalf of insurers and clients | EBQ13 |
| A-EB-07 | The roster keeps no health data | EBQ15 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-EB-01 | The business type (BT0) is available on the account | FR-EB-021, 046, 061 |
| D-EB-02 | User Access Maintenance offers the External request type for portal users | FR-EB-011 |
| D-EB-03 | BDO Information Security approves the portal hosting, MFA and virus scanner | FR-EB-010, 012 |
| D-EB-04 | BDOI provides the TOR template, the threshold values and the password convention | FR-EB-035, 042, 004 |
| D-EB-05 | The Operations owner accepts EB endorsement requests with an EB source reference | FR-EB-055 |
| D-EB-06 | EB products and HMO providers are set up in Product Maintenance | FR-EB-046 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-8 (status from the cross-BRD decisions, R5)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| EBQ01 | Full list of benefit lines; HMO providers as insurers; commission per provider | FR-EB-021, 046 | OPEN |
| EBQ02 | RA lead time 180 or 135 days; reminders; recipients | FR-EB-022 | OPEN |
| EBQ03 | Who flags a programme for renewal | FR-EB-022, 024 | OPEN |
| EBQ04 | Incumbent's indicative terms through the portal or by the AO | FR-EB-024 | OPEN |
| EBQ05 | BOR on renewal | FR-EB-031 | OPEN |
| EBQ06 | BOR signature validation; template; validity | FR-EB-031 | OPEN |
| EBQ07 | Meaning of franchise; incumbent; per insurer or per cycle; form content | FR-EB-032 | OPEN |
| EBQ08 | Standard TOR template per benefit line | FR-EB-014, 035 | OPEN |
| EBQ09 | Password convention for EB documents | FR-EB-004 | OPEN |
| EBQ10 | Comparative factors, scoring, due time, signatories | FR-EB-041 | OPEN |
| EBQ11 | Threshold values, measure and approver levels | FR-EB-042 | OPEN |
| EBQ12 | Evidence of client confirmation; separate placement approval | FR-EB-043, 046 | OPEN |
| EBQ13 | Portal hosting, authentication, user administration, system-to-system interface, virus scanner | FR-EB-010-012 | PARTIAL |
| EBQ14 | What "manage policy" adds for client HR users | FR-EB-015 | OPEN |
| EBQ15 | Master list fields; roster per member; privacy rules | FR-EB-054 | OPEN |
| EBQ16 | Movement types; pro-rata; MIS Credit; no payment no booking | FR-EB-055 | OPEN |
| EBQ17 | Collection of direct-billed changes; commission receivable | FR-EB-056 | OPEN |
| EBQ18 | Billing number definition and duplicate scope | FR-EB-052 | OPEN |
| EBQ19 | SOA formats, validation rules, recipients | FR-EB-053 | OPEN |
| EBQ20 | Statuses, thresholds and recipients of monitored items | FR-EB-057 | OPEN |
| EBQ21 | Report layouts; TAT events; Word export scope; meaning of COG | FR-EB-060-062 | OPEN |
| EBQ22 | Teams as programme attribute or sales units | FR-EB-021 | OPEN |
| EBQ23 | When the Processing Supervisor approves | FR-EB-050 | OPEN |
| EBQ24 | Meaning of ISACOM and when its approval is needed | FR-EB-046 | OPEN |
| EBQ25 | NFR alignment (99.99%, hours, RPO, portal availability) | Section 8 | OPEN |
| EBQ26 | No high-risk handling for EB (BRID-028 OUT) | Section 1.2 | OPEN |
| EBQ27 | Voluntary plans: billing per member or employer | FR-EB-021 | OPEN |
| EBQ28 | Boundary with the Renewal BRD | FR-EB-022 | ANSWERED |

# Traceability

Every BRD-8 requirement is met by at least one FR, except BRID-028, which the addendum puts out of scope. The screen column names the main entry point; the test cases are listed by test condition (TC-EB-nnn.n). The non-functional requirements (p.36-37) are traced in section 8; the TAT annex (p.42) in section 5.6.

<!-- table: widths=2.4,3.6,5.6,5.2 caption="BRD ID to FR, screen and test cases" size=8 -->
| BRD ID | FR | Screen | Test cases |
|---|---|---|---|
| BRID-001 | FR-EB-022 | Programmes (Renewal Due) | TC-EB-022.1, 022.2, 022.3 (5 cases) |
| BRID-002 | FR-EB-023 | Programme page; portal | TC-EB-023.1, 023.2 (3 cases) |
| BRID-003 | FR-EB-024 | Proposals tab | TC-EB-024.1, 024.2 (3 cases) |
| BRID-004 | FR-EB-035 | Insurer Requests tab | TC-EB-035.2 (1 case) |
| BRID-005 | FR-EB-010, 011, 014, 001 | Portal sign-in and home | TC-EB-001.3, 010.1, 010.3, 011.1, 011.4, 014.3 (9 cases) |
| BRID-005.01 | FR-EB-010, 012, 013, 051 | Portal uploads; Portal Uploads queue | TC-EB-010.2, 012.1, 012.3, 013.3, 051.2 (8 cases) |
| BRID-005.02 | FR-EB-010, 014, 040 | Portal - Requests for Proposal | TC-EB-010.1, 014.1, 014.2, 040.3 (6 cases) |
| BRID-005.03 | FR-EB-013, 040 | Portal Uploads | TC-EB-013.1, 013.2, 013.3, 040.1, 040.2 (5 cases) |
| BRID-006 | FR-EB-020, 021 | Client pages; New Programme | TC-EB-020.1, 020.2, 021.1, 021.3 (5 cases) |
| BRID-007 | FR-EB-030, 004 | Documents tab | TC-EB-004.1, 004.2, 030.1, 030.2, 030.3 (6 cases) |
| BRID-008 | FR-EB-031 | BOR tab | TC-EB-031.1, 031.2, 031.3 (5 cases) |
| BRID-009 | FR-EB-035 | TOR and Insurer Requests tabs | TC-EB-035.1, 035.2 (3 cases) |
| BRID-010 | FR-EB-041, 040 | Comparative page | TC-EB-040.1, 041.1, 041.2, 041.3 (7 cases) |
| BRID-011 | FR-EB-043, 015 | Comparative page; portal | TC-EB-015.3, 043.1, 043.2, 043.3 (5 cases) |
| BRID-012 | FR-EB-044 | Revisions | TC-EB-044.1, 044.2 (3 cases) |
| BRID-013 | FR-EB-054, 055 | Members; Member Changes | TC-EB-054.1, 055.1, 055.3 (4 cases) |
| BRID-014 | FR-EB-015, 010, 011, 012, 054 | Portal - client HR | TC-EB-010.4, 011.2, 011.3, 011.4, 012.2, 012.3, 015.1, 015.2, 054.2, 054.1 (11 cases) |
| BRID-015 | FR-EB-045 | Portal - revision | TC-EB-045.1, 045.2 (2 cases) |
| BRID-016 | FR-EB-042 | Threshold Rules; My Approvals | TC-EB-042.1, 042.2, 042.3 (5 cases) |
| BRID-017 | FR-EB-046 | Programme page (Trigger Placement) | TC-EB-046.1 (2 cases) |
| BRID-018 | FR-EB-050 | My Work | TC-EB-050.1, 050.2 (3 cases) |
| BRID-019 | FR-EB-051, 046 | Account page (BRD-1) | TC-EB-046.2, 046.3, 046.4, 051.1 (4 cases) |
| BRID-020 | FR-EB-052 | Book Account | TC-EB-052.1, 052.3, 052.2 (3 cases) |
| BRID-021 | FR-EB-053 | SOA Register | TC-EB-053.1, 053.2, 053.3 (4 cases) |
| BRID-022 | FR-EB-060, 062 | Reports; EB Home | TC-EB-060.1, 060.2, 060.3, 060.4, 062.2 (7 cases) |
| BRID-022.01 | FR-EB-061, 021 | Reports | TC-EB-021.2, 021.3, 061.1, 061.2, 061.3 (7 cases) |
| BRID-023 | FR-EB-001 | All | TC-EB-001.1, 001.2 (3 cases) |
| BRID-024 | FR-EB-003 | History tab | TC-EB-003.1, 003.2, 003.3 (3 cases) |
| BRID-025 | FR-EB-002, 056, 055 | Documents; Member Changes | TC-EB-002.1, 002.2, 002.3, 055.2, 056.1, 056.2 (7 cases) |
| BRID-026 | FR-EB-034, 032 | Submissions; Franchise tab | TC-EB-032.1, 034.1, 034.2, 034.3 (5 cases) |
| BRID-027 | FR-EB-032 | Portal - Franchise Requests | TC-EB-032.2, 032.3 (4 cases) |
| BRID-028 | Out of scope (Add. p.13) | - | - |
| BRID-029 | FR-EB-033 | Franchise tab | TC-EB-033.1, 033.2 (2 cases) |
| BRID-030 | FR-EB-057 | Pending Items | TC-EB-057.1, 057.2, 057.3 (4 cases) |


# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, fills a gap the BRD leaves open, or needs a decision of BDOI. The open questions of section 10.3 stay open; each item below gives the proposed rule that applies until BDOI answers. BDOI records its decision with the sign-off of this FRS (chapter 13); a decision that changes a screen, field, rule or message is applied in the next version of the FRS.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-EB-01 | BOR on renewal (BRID-008; FR-EB-031) | A BOR is required for new business and for renewals that remarket. | The negative scenario of BRID-008 calls a BOR for a renewal invalid, while both flow charts ask for a BOR in the renewal lane (EBQ05). | Confirm when a renewal needs a BOR (EBQ05) before the Drop 2 sign-off; FR-EB-031 is aligned to the answer. |
| CLR-EB-02 | BOR signature (BRID-008; FR-EB-031) | The validator attests the signature with a checklist; e-signature verification is added if BDOI specifies it. | The BRD asks the system to validate the signature; the verification method is open (EBQ06). | Confirm the attestation, or give the verification method (EBQ06), before the Drop 2 sign-off. |
| CLR-EB-03 | Renewal Advice lead time (BRID-001; FR-EB-022) | The Renewal Advice is sent 135 days before expiry (default from the TAT annex). | The BRD gives 180 days as an example; the TAT annex gives 135 (EBQ02). | Choose the lead time (EBQ02); it is a parameter, so the answer needs no change to the system. |
| CLR-EB-04 | Secure portal or system-to-system interface (BRID-005; FR-EB-010) | Insurers and clients use the secure partner portal; a system-to-system interface is added when an insurer asks for it. | No insurer has asked for a system interface. | Confirm the portal. |
| CLR-EB-05 | Booking (TAT annex; FR-EB-052) | Booking is done in BIBS. | The TAT annex refers to booking in EBIX, which BIBS replaces. | Confirm booking in BIBS. |
| CLR-EB-06 | Portal users | Portal users are provisioned through User Access requests of user type External. | The BRD does not say how portal users are created (decision D7). | Confirm the provisioning (decision D7). |
| CLR-EB-07 | High-risk accounts (BRID-028) | Out of scope; there is no high-risk handling for EB. | The addendum states that high-risk accounts do not apply to Employee Benefits (Add. p.13; EBQ26). | Confirm the exclusion (EBQ26). |
| CLR-EB-08 | EB renewals (FR-EB-022) | EB programmes are renewed in this module; EB lines are excluded from the general Renewal lists. | Cross-BRD decision D3. | Confirm decision D3. |
| CLR-EB-09 | EB report layouts (BRID-022; FR-EB-060) | Production, Renewal and New Business run weekly and Placement monthly, each also on request. Placement shows the key fields of the Report List. Renewal and New Business share one layout, the template "EB Sales Report N_2026 PRODUCTION"; until BDOI supplies it, the columns of section 6.1 apply. | The Report List of 13-May-2026 changes the frequencies and the source to BIBS and refers to a template the project has not received (EBQ21). | Supply the template and confirm the schedules. |

# Sign-off

By signing, BDOI confirms that this FRS describes the Employee Benefits and portal functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Employee Benefits", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank / Corbank", organisation: BDOI}
  - {name: "", role: "Team Head, Processing", organisation: BDOI}
  - {name: "", role: "Head, Collections and Marketing Support", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

# Appendix: User-story view

The Employee Benefits BRD and its Addendum give each requirement a persona ("User", "System", "Client", "Insurer"), an expected result, acceptance criteria and a negative scenario, but write the requirement as a statement, not as a user story. This appendix gives each requirement (BRID-001 to 030 with 005.01 to 005.03 and 022.01) as one user story, with the FRs that meet it, their acceptance criteria and the test conditions of the test plan with their number of cases. The story keeps the BRD statement and names the persona of section 3.1 who acts or needs the result. Acceptance criteria are numbered in the order of the FR (AC1 is the first criterion of the FR). The table has 34 rows; BRID-028 is out of scope by the Addendum (p.13).

<!-- table: widths=2.4,7.5,2.3,2.8,3.4 caption="User-story view: BRD requirement, user story, FRs, acceptance criteria and test cases" size=8 -->
| BRD ID | User story | FR IDs | Acceptance criteria | Test cases |
|---|---|---|---|---|
| BRID-001 | As a Marketing AO (EB), I need BIBS to send the renewal advice before the programme expires, so that the client prepares the renewal in time. | FR-EB-022 | FR-EB-022 AC1-3 | TC-EB-022.1 to 022.3 (5 cases) |
| BRID-002 | As a Marketing AO (EB), I need to collect the client's feedback and upload it, so that the renewal reflects what the client wants. | FR-EB-023 | FR-EB-023 AC1-2 | TC-EB-023.1 to 023.2 (3 cases) |
| BRID-003 | As a Marketing AO (EB), I need to send the client an initial renewal proposal based on the incumbent rates, so that the client sees the renewal terms early. | FR-EB-024 | FR-EB-024 AC1-2 | TC-EB-024.1 to 024.2 (3 cases) |
| BRID-004 | As a Marketing AO (EB), I need to remarket the programme to other insurers for better options, so that the client gets the best terms. | FR-EB-035 | FR-EB-035 AC1-3 | TC-EB-035.1 to 035.3 (5 cases) |
| BRID-005 | As an Insurer user, I need to access and work with BIBS only through a secure portal, not the core system, so that I upload, view and manage the records assigned to me within my role and scope. | FR-EB-010, FR-EB-011, FR-EB-014, FR-EB-001 | FR-EB-010 AC1-3; FR-EB-011 AC1-3; FR-EB-014 AC1-3; FR-EB-001 AC1-3 | TC-EB-010.1 to 010.4, 011.1 to 011.4, 014.1 to 014.3, 001.1 to 001.3 (20 cases) |
| BRID-005.01 | As a Business Administrator, I need the portal to enforce authentication and role-based access, so that insurers and clients reach only their own records and every access is logged. | FR-EB-010, FR-EB-012, FR-EB-013, FR-EB-051 | FR-EB-010 AC1-3; FR-EB-012 AC1-3; FR-EB-013 AC1-3; FR-EB-051 AC1-2 | TC-EB-010.1 to 010.4, 012.1 to 012.3, 013.1 to 013.3, 051.1 to 051.2 (18 cases) |
| BRID-005.02 | As an Insurer user, I need to access BIBS securely through the portal and never directly, so that I answer requests and submit proposals and documents safely. | FR-EB-010, FR-EB-014, FR-EB-040 | FR-EB-010 AC1-3; FR-EB-014 AC1-3; FR-EB-040 AC1-2 | TC-EB-010.1 to 010.4, 014.1 to 014.3, 040.1 to 040.3 (15 cases) |
| BRID-005.03 | As a Marketing AO (EB), I need to review, validate and approve the documents insurers upload, so that only valid documents enter the programme. | FR-EB-013, FR-EB-040 | FR-EB-013 AC1-3; FR-EB-040 AC1-2 | TC-EB-013.1 to 013.3, 040.1 to 040.3 (10 cases) |
| BRID-006 | As a Marketing AO (EB), I need to capture new clients and tag them as prospect or account, so that new EB business is tracked from the start. | FR-EB-020, FR-EB-021 | FR-EB-020 AC1-2; FR-EB-021 AC1-3 | TC-EB-020.1 to 020.2, 021.1 to 021.3 (8 cases) |
| BRID-007 | As a Marketing AO (EB), I need to upload the Terms of Reference, the master list and the utilization report, so that the insurers receive complete information. | FR-EB-030, FR-EB-004 | FR-EB-030 AC1-2; FR-EB-004 AC1-2 | TC-EB-030.1 to 030.3, 004.1 to 004.2 (6 cases) |
| BRID-008 | As a Marketing AO (EB), I need to upload the signed Broker on Record, which BIBS checks before proposal requests proceed, so that BDOI is authorised to act for the client. | FR-EB-031 | FR-EB-031 AC1-2 | TC-EB-031.1 to 031.3 (5 cases) |
| BRID-009 | As a Marketing AO (EB), I need to distribute the TOR, the unnamed master list and the utilization report to the insurers, so that they can quote. | FR-EB-035 | FR-EB-035 AC1-3 | TC-EB-035.1 to 035.3 (5 cases) |
| BRID-010 | As a Marketing TL / UH (EB), I need BIBS to generate the comparative analysis of the renewal proposals, so that the best offer is identified and signed off. | FR-EB-041, FR-EB-040 | FR-EB-041 AC1-3; FR-EB-040 AC1-2 | TC-EB-041.1 to 041.3, 040.1 to 040.3 (10 cases) |
| BRID-011 | As a Marketing AO (EB), I need to present the comparative report to the client, so that the client chooses an insurer. | FR-EB-043, FR-EB-015 | FR-EB-043 AC1-2; FR-EB-015 AC1-3 | TC-EB-043.1 to 043.3, 015.1 to 015.3 (8 cases) |
| BRID-012 | As a Marketing AO (EB), I need to capture the client's changes, additions and amendment requests and relay them to the insurers, so that proposals follow the client's needs. | FR-EB-044 | FR-EB-044 AC1-2 | TC-EB-044.1 to 044.2 (3 cases) |
| BRID-013 | As a Marketing AO (EB), I need to enter and upload employee benefit changes, additions and amendments (for example new hires for HMO, GPA, GLI), so that the member roster stays current. | FR-EB-054, FR-EB-055 | FR-EB-054 AC1-2; FR-EB-055 AC1-3 | TC-EB-054.1 to 054.2, 055.1 to 055.3 (7 cases) |
| BRID-014 | As a Client HR user, I need to access BIBS only through a secure portal to upload master lists and utilization reports and view my policy details, so that I work with BDOI without direct access to the core system. | FR-EB-015, FR-EB-010, FR-EB-011, FR-EB-012, FR-EB-054 | FR-EB-015 AC1-3; FR-EB-010 AC1-3; FR-EB-011 AC1-3; FR-EB-012 AC1-3; FR-EB-054 AC1-2 | TC-EB-015.1 to 015.3, 010.1 to 010.4, 011.1 to 011.4, 012.1 to 012.3, 054.1 to 054.2 (23 cases) |
| BRID-015 | As an Insurer user, I need to update my proposals for the client's changes, additions and amendments, so that the comparative uses current terms. | FR-EB-045 | FR-EB-045 AC1-2 | TC-EB-045.1 to 045.2 (2 cases) |
| BRID-016 | As BDOI Management, I need an approval when a transaction meets or exceeds a defined value threshold (total sum insured or premium), so that large programmes are approved at the right level. | FR-EB-042 | FR-EB-042 AC1-3 | TC-EB-042.1 to 042.3 (5 cases) |
| BRID-017 | As a Marketing AO (EB), I need to trigger the placement after the client confirms, so that Processing places the programme with the chosen insurer. | FR-EB-046 | FR-EB-046 AC1-3 | TC-EB-046.1 to 046.4 (5 cases) |
| BRID-018 | As a Processing Supervisor, I need to assign, re-assign and return work and give remarks, so that the EB work is distributed and followed. | FR-EB-050 | FR-EB-050 AC1-2 | TC-EB-050.1 to 050.2 (3 cases) |
| BRID-019 | As Processing (EB), I need to complete the policy issuance after a valid placement confirmation, with every required document received and tracked, so that the policy is issued correctly. | FR-EB-051, FR-EB-046 | FR-EB-051 AC1-2; FR-EB-046 AC1-3 | TC-EB-051.1 to 051.2, 046.1 to 046.4 (8 cases) |
| BRID-020 | As Processing (EB), I need to submit the booking, so that the programme is booked and billed. | FR-EB-052 | FR-EB-052 AC1-2 | TC-EB-052.1 to 052.3 (3 cases) |
| BRID-021 | As Collection (EB), I need to manage billing, statements of account and payment tracking after placement, so that premiums are collected and documents reach the right teams. | FR-EB-053 | FR-EB-053 AC1-3 | TC-EB-053.1 to 053.3 (4 cases) |
| BRID-022 | As BDOI Management, I need EB reports generated automatically or on request, so that production, renewals and placements are monitored. | FR-EB-060, FR-EB-062 | FR-EB-060 AC1-3; FR-EB-062 AC1-2 | TC-EB-060.1 to 060.4, 062.1 to 062.3 (9 cases) |
| BRID-022.01 | As BDOI Management, I need a New Business report of the EB accounts, so that new business is tracked from prospecting to placement. | FR-EB-061, FR-EB-021 | FR-EB-061 AC1-2; FR-EB-021 AC1-3 | TC-EB-061.1 to 061.3, 021.1 to 021.3 (9 cases) |
| BRID-023 | As a Marketing AO (EB), I need only authorised users to view, download, print or access EB documents and reports, so that client data stays confidential. | FR-EB-001 | FR-EB-001 AC1-3 | TC-EB-001.1 to 001.3 (4 cases) |
| BRID-024 | As BDOI Management, I need BIBS to log every key action (renewal advice, feedback, proposals, documents, approvals, client confirmation) with time and user, so that the EB work is auditable. | FR-EB-003 | FR-EB-003 AC1-3 | TC-EB-003.1 to 003.3 (3 cases) |
| BRID-025 | As a Marketing AO (EB), I need to upload the insurer's direct billing documents for member changes, linked to the right transaction and endorsement type, so that billing matches the changes. | FR-EB-002, FR-EB-056, FR-EB-055 | FR-EB-002 AC1-3; FR-EB-056 AC1-2; FR-EB-055 AC1-3 | TC-EB-002.1 to 002.3, 056.1 to 056.2, 055.1 to 055.3 (10 cases) |
| BRID-026 | As a Marketing AO (EB), I need to submit the necessary documents to the insurer for a franchise request, so that the insurer can accredit the account. | FR-EB-034, FR-EB-032 | FR-EB-034 AC1-2; FR-EB-032 AC1-3 | TC-EB-034.1 to 034.3, 032.1 to 032.3 (9 cases) |
| BRID-027 | As an Insurer user, I need to approve or reject the franchise request to BDOI Marketing, so that the account can proceed or stop. | FR-EB-032 | FR-EB-032 AC1-3 | TC-EB-032.1 to 032.3 (5 cases) |
| BRID-028 | As an Insurer user, I need to present the required documents for high-risk accounts; the Addendum states that high-risk accounts do not apply to Employee Benefits. | Out of scope (Add. p.13) | - | - |
| BRID-029 | As a Marketing AO (EB), I need to advise the client of the franchise approval or rejection, so that the client knows the outcome. | FR-EB-033 | FR-EB-033 AC1-2 | TC-EB-033.1 to 033.2 (2 cases) |
| BRID-030 | As Processing (EB), I need BIBS to monitor and tag the pending items automatically, so that contracts, cards and billing pending are followed up. | FR-EB-057 | FR-EB-057 AC1-3 | TC-EB-057.1 to 057.3 (4 cases) |

<!-- landscape -->

# Appendix: Storyboard index

Employee Benefits has no end-to-end walkthroughs with screenshots yet. This index gives the storyboard of the EB cycle from the process flow of section 2.2 and the cycle workflow of chapter 5: one frame per step, with the persona, the step, the screen or document used, the outcome and the FRs. The screen-level frames with screenshots, including the insurer and client portal journeys, follow with the v2.0 business sign-off pack of BRD-8 (Drop 2), which adds the walkthroughs and the UX Screen Deck.

<!-- table: widths=1.3,2.8,5.6,3.6,5.2,2.2 caption="Storyboard of the business processes: frame, persona, step, screen or document, outcome and FR" size=8 -->
| Frame | Persona | Step | Screen or document | Outcome | FR |
|---|---|---|---|---|---|
| **P1** | | **Programme start and portal access** | | | |
| P1.1 | Marketing AO (EB) | Creates the client and the programme (new business) or receives the renewal due list | Clients; New Programme; Programmes (Renewal Due) | Programme with business type; renewal advice sent at the lead time | FR-EB-020, FR-EB-021, FR-EB-022 |
| P1.2 | Business Administrator | Approves the portal users of the insurer and the client | Portal user requests | Insurer and client HR users can sign in to the portal | FR-EB-010, FR-EB-011 |
| P1.3 | Client HR user | Uploads the master list and utilization report and gives feedback | Portal - client HR | Uploads waiting for review | FR-EB-015, FR-EB-012, FR-EB-023 |
| P1.4 | Marketing AO (EB) | Reviews and approves the portal uploads | Portal Uploads queue | Valid documents attached to the programme | FR-EB-013 |
| **P2** | | **Documents, BOR, franchise and TOR** | | | |
| P2.1 | Marketing AO (EB) | Uploads the TOR, master list, utilization report and the signed BOR | Documents tab; BOR tab | BOR attested; documents complete | FR-EB-030, FR-EB-031 |
| P2.2 | Marketing AO (EB) | Submits the franchise request; the insurer decides | Franchise tab; Portal - Franchise Requests | Franchise approved or rejected; the client is advised | FR-EB-032, FR-EB-033, FR-EB-034 |
| P2.3 | Marketing AO (EB) | Distributes the TOR and requests proposals from the insurers | TOR and Insurer Requests tabs | Requests sent to the incumbent and, when remarketing, other insurers | FR-EB-035, FR-EB-014 |
| **P3** | | **Proposals, comparative and client decision** | | | |
| P3.1 | Insurer user | Submits the proposal and updates it for client changes | Portal - Requests for Proposal | Proposal versions received | FR-EB-040, FR-EB-045 |
| P3.2 | Marketing TL / UH (EB) | Signs the comparative of the validated proposals | Comparative page | Comparative ready; approval above the threshold by BDOI Management | FR-EB-041, FR-EB-042 |
| P3.3 | Client HR user | Comments on and confirms the comparative | Portal - comparative | Client decision recorded; revisions if requested | FR-EB-043, FR-EB-044 |
| P3.4 | Marketing AO (EB) | Triggers the placement | Programme page (Trigger Placement) | Placement work created for Processing | FR-EB-046 |
| **P4** | | **Processing, booking, billing and members** | | | |
| P4.1 | Processing Supervisor | Assigns the placement work | My Work | Work assigned with remarks | FR-EB-050 |
| P4.2 | Processing (EB) | Completes issuance and books the accounts | Account page (BRD-1); Book Account | Accounts booked by line | FR-EB-051, FR-EB-052 |
| P4.3 | Collection (EB) | Records the SOA and tracks payment | SOA Register | SOA validated; payments tracked | FR-EB-053 |
| P4.4 | Marketing AO (EB) | Records member changes and uploads the insurer's billing documents | Members; Member Changes | Roster updated; billing linked to the change | FR-EB-054, FR-EB-055, FR-EB-056 |
| **P5** | | **Monitoring** | | | |
| P5.1 | BDOI Management | Receives the weekly Renewal and New Business reports and the monthly Placement report | Reports (Employee Benefits); EB Home | Reports on schedule with the Report List fields | FR-EB-060, FR-EB-061, FR-EB-062 |
| P5.2 | Processing (EB) | Follows the pending items | Pending Items | Contracts, cards and billing pending with age and follow-ups | FR-EB-057 |

