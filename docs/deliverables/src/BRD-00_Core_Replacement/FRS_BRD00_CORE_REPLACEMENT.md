---
# Source of the umbrella Functional Requirements Specification for the BDOI Core Replacement BRD.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-00_Core_Replacement/FRS_BRD00_CORE_REPLACEMENT.md
title: Core Replacement
subtitle: Umbrella FRS for the BDOI Core Replacement BRD (21 key capabilities)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-00
name: Core Replacement
doc_id: BIBS-FRS-BRD-00
version: "1.1"
date: 08 October 2026
status: Issued for BDOI review
header_title: FRS BRD-00 Core Replacement
output: FRS/BIBS_FRS_BRD-00_Core_Replacement_v1.1.docx
control:
  - version: "0.9"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the Core Replacement BRD and the twelve BRD specs
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue for BDOI review
  - version: "1.1"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: "Re-based on BDOI Core Replacement BRD v01 (v1.0, unchanged) and the function BRDs received on 08-Oct-2026; references, traceability IDs (BRNB.100.1, MKTID.013, MKTID.019, BRCSFCM) and CSF case resolution (CLR-CR-03) updated; BR-181 to BR-183 traced to the walkthroughs and the UX hand-over; user-story view and storyboard index (journey index) added."
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: Review and sign-off of the umbrella scope}
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review of the finance and data-management requirements}
  - {name: "Head, Operations", role: Approver, organisation: BDOI, purpose: Review of the Operations and reporting requirements}
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review of the front-office and dashboard requirements}
  - {name: "Unit Head, Claims, Analytics, Risk Management, Reinsurance and Technical Underwriting", role: Reviewer, organisation: BDOI, purpose: Claims and Reinsurance (phase 2) scope}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) is the umbrella FRS of BIBS (BDOI Broker System, on iNXT BrokerVerse). It answers the BDOI Core Replacement BRD, which summarises all BDOI functions as 21 key capabilities and 209 business requirements (BR-000 to BR-208) and refers to the function BRDs for the details.

The document does three things:

- it maps every capability and bullet of the umbrella BRD to the BRD and FRS that specify it (section 3.2 and the traceability in section 10);
- it states the **cross-cutting functional requirements** that no function BRD owns: navigation and persona menus, dashboards, notifications, audit, report generation, document generation, data management, security and integration (section 4);
- it lists the gaps of the umbrella BRD, with a proposed rule or FR and the decision requested from BDOI for each (section 5).

The function FRS (BRD-1 to BRD-12) remain the specifications of the business processes. Where this document and a function FRS describe the same behaviour, the function FRS governs.

## Scope

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Platform scope and journey | The end-to-end flow of the umbrella BRD and the BIBS BRD of each step | p.3, p.26 |
| Capability map | 21 capabilities, 184 bullets and 8 items of the capability matrix, each mapped to a BRD and an FRS | p.6-12, p.26-29 |
| Cross-cutting requirements | Navigation, dashboards, notifications, audit, reports, documents, data management, security, integration, Invoice Master List | p.4-5, BR-000-004, 027, 051, 063, 083, 088, 101, 125, 152, 160, 170-177 |
| Proposed rules and clarifications | Requirements with no owning BRD or in conflict with their BRD | Section 5 |
| Non-functional requirements | Users, response time, volumes, retention (p.42-46) against the function BRDs and the hosting appendix | Section 7 |

**Out of scope for phase 1:**

- Reinsurance (capability 15). The ReInsurance BRD is received and is **phase 2** (section 6).
- Emerging capabilities (capability 18, BR-176): not defined (CRQ10).
- The detailed process requirements of each function; they are in the function FRS.

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | BDOI Core Replacement BRD (48 pages) | v1.0, approved 21-Nov to 1-Dec-2025 |
| R2 | Core Replacement requirements baseline (rows CORE-nn.mm, XC-nn; questions CRQnn) | current |
| R4 | FRS BRD-1 to BRD-12 (BRD-5 in two volumes) and the Data Migration Handbook (BRD-13) | The versions re-issued on the BRD versions of 08-Oct-2026: the next version of each business sign-off pack (v2.1) and of each FRS-only BRD (v1.1 or v1.2) |
| R5 | Cross-BRD decisions and answered questions | current |
| R6 | BRD discrepancy and clarification register | v1.2 |
| R7 | ReInsurance BRD (phase 2) | 11-Mar-2025 |
| R8 | BDO Insure Core Modernization - Data Migration - Business Requirements Document (BRD) | V0.03 (received 08-Oct-2026) |
| R9 | BDO UX guidelines and BDOI UX design | current |
| R10 | Function BRD versions received on 08-Oct-2026: New Business BRD 05132026, Operations WS Addendum (re-issued BRD v1.01), Collections WS Addendum, Accounting WS Addendum v04152026, Renewal BRD, Claims WS Addendum, Customer Servicing Facility with the Case Management Addendum, Submitted Policies BRD, Report List 05132026 | 08-Oct-2026 |

Page references ("p.14") are pages of the umbrella BRD (R1). The umbrella BRD itself is unchanged; the requirement IDs of the function BRDs in chapter 10 are those of the versions of R10.

## Definitions and acronyms

```glossary
ACSL: Accounting Controls and Subsidiary Ledger
AO: Account Officer (Marketing)
AR: Acknowledgement Receipt
BR: Requirement ID prefix of the umbrella BRD table (BR-000 to BR-208)
CORE-nn.mm: Row of the requirements baseline (R2) for bullet mm of capability nn; the umbrella BRD numbers capabilities but not bullets
CRQnn: Open question on the umbrella BRD raised by the project team (section 9.3)
CSF: Customer Service Facility
CWT: Creditable withholding tax (BIR Form 2307)
DP: Direct payment (premium paid by the client directly to the insurer)
EB: Employee Benefits
FR: Functional requirement; FR-CR-nnn in this document
IA: Insurance Advice
LOV: List of values maintained by an authorised user
MIS: Management information system; the classification fields used to filter, group and report (market segment, branch, account officer ...)
NAL: No Advice Letter
NRL / NFR: Non-Renewal Letter (umbrella) / Not for Renewal Letter (BRD-6); the same letter (CRQ14)
OR: Official Receipt
PR: Premium receivable
PRF: Proposal Request Form (non-package placement)
RA: Renewal Advice
RMEL: Renewal master expiry list
SOA: Statement of Account
TSU: Technical Support Unit
UPP: Unapplied premium payment
XC-nn: Cross-cutting row of the requirements baseline (R2), a requirement of the umbrella outside the bullet list
```

## How to read the functional requirements

Each FR in section 4 has a header with the **BRD trace** (BR ID and page of the umbrella), the actor, the priority (every BR row is "Must have") and the screens.

Validations list the message the user sees and its code. A "-" marks a screen check without a business code. Values marked "default" are placeholders that BDOI confirms through the open questions in section 9.3; they are configuration.

# Platform scope and journey

## Business context

BDOI is a non-life insurance and reinsurance broker. The umbrella BRD sets the goal of one integrated platform from client onboarding to claims and accounting, for retail and wholesale business, meeting BIR and Insurance Commission reporting (p.4). Today intake, tagging, tracking and reconciliation run on spreadsheets and e-mail, cashiering, remittance and ACSL use separate systems, and reports depend on IT (p.4-5). The envisioned process has automated workflows, centralised dashboards for all roles, role-based self-service, system-generated and customised reports, audit logs and automated notifications (p.5).

## End-to-end journey

Figure 1 shows the flow of the umbrella BRD (p.26) with the BIBS BRD of each step. The left column is the new-business and money flow; the right column holds the lifecycle services and the functions that feed it (dashed arrows); the bar at the bottom lists the platform services that every step uses.

![End-to-end journey and the BIBS BRD of each step (dashed box = phase 2)](figures/brd00_journey.dot){width=16}

<!-- table: widths=3.6,4.6,6 caption="Umbrella BRD list (p.3) and BIBS" size=8.5 -->
| Umbrella BRD (p.3) | BIBS BRD and FRS | Note |
|---|---|---|
| 1 New Business | BRD-1, FR-NB | Capabilities 1-5 |
| 2 Renewal | BRD-6, FR-RN | Capability 6 |
| 3 Collection Management | BRD-4, FR-CL (BRD-4) | Capability 7 |
| 4 Accounting, Disbursement and ACSL | BRD-5, FR-AC / FR-DS / FR-AS | Capability 8 |
| 5 Claims | BRD-7, FR-CM (BRD-7) | Claim recording and insurer updates merged |
| 6 Operations | BRD-2, FR-OP | Capabilities 9-13 |
| 7 Reinsurance | ReInsurance BRD | Phase 2 - BRD received |
| 8 Customer Service Facility | BRD-9, FR-CSF | Capability 16, with the Case Management Addendum |
| 9 Product Maintenance | BRD-3, FR-PM | Capability 20 |
| 10 Employee Benefits | BRD-8, FR-EB | Capability 19 |
| Not listed | BRD-10 Sanction Screening, FR-SS | Screening of clients (capability 1) |
| Not listed | BRD-11 User Access Maintenance, FR-UA | BR-000 to BR-003 |
| Not listed | BRD-12 Submitted Policies, FR-SP | Submitted policies of the bank (capability 3) |
| Not listed | Data Migration BRD V0.03, Data Migration Handbook (BRD-13) | Client migration volumes (p.43) |

# Personas, navigation and capability map

## Personas and menu groups

The umbrella BRD names four generic personas (System, User, Administrator, Authorized User). The function BRDs define the business personas; BIBS gives each persona roles and shows only the menus its roles allow (FR-CR-001).

<!-- table: widths=4.4,5.8,6.4 caption="Personas and their menu groups" size=8.5 -->
| Persona (BRD) | Menu groups | Home page (FR-CR-010) |
|---|---|---|
| Marketing AO, TL, TH, UH (BRD-1, 3, 4, 6, 8) | Client & Policy; Reports | Pipeline, expiring accounts, PR to follow up |
| TSU Officer, TL, Head (BRD-1, 3) | Client & Policy (proposals, packages); Reports | PRFs and package requests to act on |
| Processing Officer, TL (BRD-1, 6, 8, 12) | Client & Policy (placement, issuance, booking); Reports | Placement and booking queues |
| Cashiering, Remittance, Prod Recon, Adjustment, Commission users (BRD-2) | Operations; Finance | Counts of their section home |
| Collection user and TL (BRD-4) | Finance > Collections | Worklist, promises due, escalations |
| FRBS, Disbursement, ACSL users (BRD-5) | Finance | Finance dashboard; vouchers and reconciliations |
| Claims Officer, Assistant, Unit Head (BRD-7) | Claims & Insurance > Claims Handling | Open claims by age and next follow-up |
| EB AO, Processing, Collection (BRD-8) | Client & Policy > Employee Benefits | EB renewals and proposals |
| CSF Agent, Supervisor (BRD-9) | Client & Policy > Customer Service | Search client |
| Compliance officer, investigator (BRD-10) | Client & Policy > Screening; Compliance Setup | Screening cases |
| UAM requestor, approver, implementer (BRD-11) | Setup & Administration > User Access | Requests to approve or implement |
| Business Administrator, System Administrator | Setup & Administration | Approvals, jobs, change log |

## Capability map

<!-- table: widths=6.4,1.4,1.8,1.4,5.6 caption="Umbrella capabilities and coverage" size=8 -->
| Capability | Rows | Phase 1 | Out | Covered by |
|---|---|---|---|---|
| CORE-01 Client Onboarding | 7 | 7 | 0 | BRD-1 |
| CORE-02 Quotation or Proposal | 9 | 9 | 0 | BRD-1 |
| CORE-03 Account Creation and Maintenance | 6 | 6 | 0 | BRD-1 |
| CORE-04 Non-Package Management | 24 | 24 | 0 | BRD-1 |
| CORE-05 Placement and Booking | 8 | 8 | 0 | BRD-1 |
| CORE-06 Renewal | 9 | 9 | 0 | BRD-6 |
| CORE-07 Marketing Collection | 6 | 6 | 0 | BRD-4 |
| CORE-08 Accounting / GL / Disbursement / ACSL | 22 | 22 | 0 | BRD-5, BRD-2, BRD-1 |
| CORE-09 Cashiering (Payments Acceptance and Application) | 12 | 12 | 0 | BRD-2 |
| CORE-10 Remittance | 7 | 7 | 0 | BRD-2 |
| CORE-11 Production Reconciliation | 5 | 5 | 0 | BRD-2 |
| CORE-12 Adjustment / Cancellation | 7 | 7 | 0 | BRD-2 |
| CORE-13 Collection of Commission Receivables (Direct Payment) | 8 | 8 | 0 | BRD-2 |
| CORE-14 Claims | 13 | 13 | 0 | BRD-7 |
| CORE-15 Reinsurance | 10 | 0 | 10 | ReInsurance BRD |
| CORE-16 Customer Service Facility | 6 | 6 | 0 | BRD-9 |
| CORE-17 Data Management | 5 | 5 | 0 | none, BRD-1, BRD-3 |
| CORE-18 Emerging Capabilities | 1 | 0 | 1 | none |
| CORE-19 Employee Benefits | 12 | 12 | 0 | BRD-8 |
| CORE-20 Product Maintenance | 10 | 10 | 0 | BRD-3 |
| CORE-21 Report Generation | 5 | 5 | 0 | BRD-1, none |

The row-level map (every bullet, BR ID, BRD requirement ID and FR) is in section 10. The 192 capability rows and 23 cross-cutting rows are:

<!-- table: widths=6.4,2.4,2.4,2 caption="Rows in and out of phase 1" -->
| Scope | Capability rows | Cross-cutting rows | Total |
|---|---|---|---|
| Phase 1 | 181 | 21 | 202 |
| Out of phase 1 scope | 11 | 2 | 13 |
| **Total** | **192** | **23** | **215** |

# Cross-cutting functional requirements

## Navigation and persona menus

```fr
id: FR-CR-001
title: Show each persona only its menus and screens
brd: [BR-002 (p.13), BR-181 (p.23), BR-182 (p.23), "p.4 benefit 4a"]
actor: Every user
priority: Must have
screens: Sidebar menu (groups Home, Client & Policy, Operations, Finance, Claims & Insurance, Reports, Setup & Administration)
description:
  - The sidebar lists the modules of BIBS. Each screen names the permission that opens it; a user sees only the screens that the permissions of his roles allow, in seven groups in a fixed order.
  - A user may hold several roles (decision D5); the menu is the union of the roles.
  - Insurer-side modules of the platform (underwriting, insurer claims, treaty reinsurance, reserves) are not granted to BDOI roles.
preconditions:
  - The user is signed in.
main_flow:
  - BIBS reads the permissions of the user's roles.
  - BIBS shows the menu groups and screens those permissions open.
  - The user opens a screen; BIBS checks the permission again on every call.
alternate_flows:
  - Screen opened by URL without permission. BIBS shows "You do not have access to this screen. Contact your administrator if you need it." and BIBS refuses the action, also when it is sent outside the screens.
rules:
  - [R1, "Roles and their permissions are changed only through an approved User Access request (BRD-11).", Configurable, User Access requests]
  - [R2, "A screen without a permission of the user is never shown, even if the user knows its address.", Fixed, "-"]
validations:
  - [Screen without permission, "You do not have access to this screen. Contact your administrator if you need it.", "-"]
notifications:
  - None.
audit:
  - Refused calls are logged with user, time and resource.
acceptance:
  - A Cashiering user sees Operations and Finance > Cashiering and no Client & Policy screen.
  - A user with two roles sees the screens of both roles.
  - Opening a screen by URL without its permission shows the no-access message.
```

## Dashboards

```fr
id: FR-CR-010
title: Open a role home page with the figures of the user's roles
brd: ["p.5 envisioned 2a and 4c", BR-125 (p.20), BR-152 (p.21), BR-160 (p.22), BR-203 (p.25)]
actor: Every business user; System Administrator (set-up)
priority: Must have
screens: Role Home (/); Role Home Set-up (Setup & Administration)
description:
  - After sign-in the user lands on a role home page made of widgets. Each widget shows a count, amount or short list from the user's own work (for example "PRFs to review", "Unapplied payments older than 30 days", "Claims without follow-up in 14 days") and refreshes on opening and every 5 minutes (default).
  - The widgets of a role are maintained by the System Administrator. A user with several roles sees the widgets of all his roles once.
  - The finance dashboard (premium, claims, collections, payables, cash, budget, workload) and the section home pages that exist today become widgets or stay as screens of their menus.
preconditions:
  - The user is signed in and holds at least one role with a role home configuration.
main_flow:
  - The user signs in or clicks Home.
  - BIBS reads the widget set of the user's roles.
  - BIBS shows the widgets in the configured order, each with its figure and the time it was computed.
  - The user clicks a widget; BIBS opens the filtered work list or report behind it (FR-CR-011).
alternate_flows:
  - No configuration for the user's roles. BIBS shows the default set of the role family (section 3.1) and the menu.
  - A widget's source fails. The widget shows "Not available" and the others load.
rules:
  - [R1, "A widget only counts records the user may see (same permission and data scope as the list it opens).", Fixed, "-"]
  - [R2, "Widget sets per role, order and thresholds (for example days overdue) are maintained without a release.", Configurable, Role Home Set-up]
  - [R3, "Refresh interval 5 minutes (default).", Configurable, Parameter DASHBOARD_REFRESH_MINUTES]
validations:
  - [Widget added twice to a role, The widget is already on this role's home page, "-"]
fields_screen: Role Home Set-up
fields:
  - [Role, List, "Yes", Roles, "-"]
  - [Widget, List, "Yes", Widget catalogue, Not already on the role]
  - [Position, Number, "Yes", "-", 1-30]
  - [Threshold, Number, "No", "-", Used by widgets with an age or amount limit]
notifications:
  - None.
audit:
  - Changes to widget sets are recorded in the master-data change log (FR-CR-031).
acceptance:
  - A Marketing AO lands on a home page with his pipeline and PR follow-up widgets; a Cashiering user on the cashiering counts.
  - A user with the Marketing AO and Collection roles sees both sets once.
  - A widget never counts a record that its list would not show the user.
```

```fr
id: FR-CR-011
title: Open the details behind a dashboard figure
brd: [BR-125 (p.20), BR-152 (p.21), BR-160 (p.22)]
actor: Every business user
priority: Must have
screens: Role Home; the work list or report of the widget
description:
  - Each widget carries the route and filters of the list or report that produced its figure. Clicking it opens that list already filtered, so the figure and the list agree.
preconditions:
  - FR-CR-010.
main_flow:
  - The user clicks a widget.
  - BIBS opens the target screen with the widget's filters.
  - The list shows the same number of records as the widget.
alternate_flows:
  - The records changed after the figure was computed. The list shows the current records and the widget refreshes on return.
rules:
  - [R1, "The contents of the dashboard views are 'to be defined' in BR-125, 152 and 160; BIBS delivers the default widgets of section 3.1 until BDOI answers CRQ06.", Configurable, Role Home Set-up]
validations:
  - [No permission on the target screen, "You do not have access to this screen. Contact your administrator if you need it.", "-"]
notifications:
  - None.
audit:
  - None beyond the audit of the target screen.
acceptance:
  - The count on a widget equals the number of rows of the list it opens at the same moment.
```

## Notifications and workflow communication

```fr
id: FR-CR-020
title: Notify users of status changes, approvals and pending work
brd: ["p.5 envisioned 5a", BR-083 (p.17), BR-111 (p.19), BR-206 (p.25)]
actor: System; every user
priority: Must have
screens: Notification bell; My Work; Notification preferences (profile)
description:
  - Workflow steps, approvals, returns, SLA breaches and job failures create in-system notifications for the users or roles concerned; selected events are also e-mailed (for example remittance schedules and confirmations, BR-083).
  - Users choose in their profile which notifications they also receive by e-mail, within the events their roles allow.
preconditions:
  - The event is defined in the notification set of its module.
main_flow:
  - A business event occurs (for example a request is submitted for approval).
  - BIBS creates a notification for the assigned user or the holders of the approving permission.
  - BIBS queues the e-mail when the event and the user's preference require it; the dispatch job sends it and logs the result.
alternate_flows:
  - E-mail delivery fails. BIBS retries and the failure is shown in the message log; the in-system notification stays.
rules:
  - [R1, "Notification events per module are those of the function FRS; the per-role catalogue is part of deliverable 15.", Configurable, Notification preferences and module parameters]
  - [R2, "Outbound documents that leave BDOI are password protected (FR-CR-071).", Fixed, "-"]
validations:
  - [E-mail address invalid, Enter a valid e-mail address, "-"]
notifications:
  - This FR is the notification service itself.
audit:
  - Every e-mail attempt is logged with recipients, time and result.
acceptance:
  - Submitting a request for approval notifies the approvers in the bell within one minute.
  - A user who switched off e-mail for an event still sees it in the bell.
```

## Audit

```fr
id: FR-CR-030
title: Keep a complete audit trail of actions and transactions
brd: ["p.4 benefit 2c", BR-124 (p.20), BR-129 (p.20), BR-151 (p.21), BR-207 (p.25)]
actor: System; Business Administrator (view)
priority: Must have
screens: Audit Trail (Setup & Administration); History tabs of the records
description:
  - Every create, update, approval, rejection, posting, reversal, export, log-in and log-out is recorded with user, time, record and a summary. Records show their history on a History tab; the Audit Trail screen searches all of it and exports it.
preconditions:
  - None.
main_flow:
  - A user performs an action.
  - BIBS records the audit entry together with the change; one is never kept without the other.
  - An authorised user searches the audit trail by user, record type, action and date, and exports the result.
alternate_flows:
  - The action fails. No audit entry of the change is written; failed log-ins are recorded.
rules:
  - [R1, "Audit entries cannot be changed or deleted by any user.", Fixed, "-"]
  - [R2, "Retention 5 years online and 15 years archive (p.45) unless a record type has a longer rule (CRQ22).", Configurable, Retention rules]
validations:
  - [Date range over the online period, Select a range within the online retention period, "-"]
notifications:
  - None.
audit:
  - Exports of the audit trail are themselves audited.
acceptance:
  - Every action of a test run appears in the audit trail with the right user and time.
  - An audit entry cannot be edited through any screen or direct request.
```

```fr
id: FR-CR-031
title: Log every change to master data with old and new values
brd: [BR-170 (p.22), "Capability 17 bullet 1 (p.11)"]
actor: System; Business Administrator, auditors (view)
priority: Must have
screens: Master Data Change Log (Setup & Administration)
description:
  - For registered master data (users and roles, products and versions, insurers and branches, commission rates, LOVs, chart of accounts, payees, parameters, role home and MIS field set-up), BIBS records each changed field with its old value, new value, user, time and the approval reference when the change was approved.
  - The log complements the audit trail (FR-CR-030), which records the action but not the field values.
preconditions:
  - The master entity is registered in the change-log list.
main_flow:
  - A user changes a master record (directly or through an approved request).
  - BIBS writes one log line per changed field together with the change; one is never kept without the other.
  - An authorised user opens the Master Data Change Log, filters by master type, record, user and date, and exports it.
alternate_flows:
  - Change rejected by the approver. Nothing is logged as changed; the rejection is in the audit trail.
rules:
  - [R1, "Registered master types are listed in section 9 (default); BDOI confirms the list (CRQ07).", Configurable, Change-log registration]
  - [R2, "Values of secret fields (passwords, access keys) are never logged; the log shows 'changed'.", Fixed, "-"]
  - [R3, "Retention as the audit trail (FR-CR-030 R2).", Configurable, Retention rules]
validations:
  - [No filter on a range longer than 12 months, Narrow the date range to 12 months or less, "-"]
fields_screen: Master Data Change Log
fields:
  - [Master type, List, "No", Registered master types, "-"]
  - [Record, Text, "No", "-", "-"]
  - [Changed by, Text, "No", Users, "-"]
  - [Date from / to, Date, "Yes", "-", At most 12 months]
notifications:
  - None.
audit:
  - Viewing and exporting the log are audited.
acceptance:
  - Changing an insurer's accreditation date shows one line with the old and new date, the user and the approval reference.
  - Changing a password shows "changed" and no value.
```

## Report generation

```fr
id: FR-CR-040
title: Generate standard reports and export or print them in several formats
brd: [BR-054 (p.15), BR-078 (p.17), BR-084 (p.17), "Capability 21 bullets 1-2 (p.12)"]
actor: Every user with a report permission
priority: Must have
screens: Reports (catalogue); report runner
description:
  - The report catalogue lists the reports the user's roles allow, grouped by category. The user enters the parameters, runs the report on screen and exports it to Excel (XLSX), PDF, CSV, ODS, XML or, for documents and schedules, Word. Printing shows a preview with the report name, user, time and filters.
  - Regulatory outputs are reports of the same catalogue, namely the BIR books (general journal, purchase, sales, cash receipts and cash disbursements journals), BIR forms and 2307 registers, Insurance Commission schedules.
preconditions:
  - The user holds the permission of the report.
main_flow:
  - The user opens Reports and selects a report.
  - The user enters the parameters and clicks Run.
  - BIBS shows the result and offers Export and Print.
alternate_flows:
  - Mandatory parameter missing. BIBS names it and does not run.
  - Long report. BIBS runs it as a job and notifies the user when the file is ready.
rules:
  - [R1, "Every run and export is logged with user, time and parameters.", Fixed, "-"]
  - [R2, "Word export is offered for documents and schedules; other reports export to Excel, PDF, CSV, ODS and XML (EBQ21).", Configurable, Report metadata]
validations:
  - [Parameter missing, Enter the required parameters, "-"]
notifications:
  - Completion of a report run as a job.
audit:
  - Runs and exports in the report run log.
acceptance:
  - A report exported to Excel and PDF shows the same rows and totals as on screen.
  - A user without the report's permission does not see it in the catalogue.
```

```fr
id: FR-CR-041
title: Save tailored report parameters and filters as variants
brd: [BR-053 (p.15), "Capability 21 bullet 3 (p.12)", "Capabilities 2 and 4, bullet 'Generate and customise report' (p.6-7)"]
actor: Every user with a report permission
priority: Must have
screens: Report runner (Variants)
description:
  - A user saves the current parameters and filters of a report under a name, as a private variant or shared with the users of the report. Variants are available on every report of the catalogue.
preconditions:
  - FR-CR-040.
main_flow:
  - The user sets parameters and filters and clicks Save variant.
  - The user names it and chooses private or shared.
  - Later the user selects the variant and runs the report.
alternate_flows:
  - Name already used by the user for this report. BIBS asks for another name.
rules:
  - [R1, "Only the owner changes or deletes a variant.", Fixed, "-"]
validations:
  - [Variant name used, A variant with this name already exists, "-"]
notifications:
  - None.
audit:
  - Creation and deletion of shared variants are audited.
acceptance:
  - A shared variant appears for another user of the same report and runs with the saved parameters.
```

```fr
id: FR-CR-042
title: Customise a report's fields, grouping, summaries and chart
brd: [BR-053 (p.15), "Capability 21 bullet 4 (p.12)"]
actor: Every user with a report permission
priority: Must have
screens: Report runner (Layout panel)
description:
  - On the report runner the user chooses the columns to show and their order, one or two grouping fields (including the MIS fields of FR-CR-062), subtotals and counts per group, and one bar or line chart over a grouped amount or count. The layout is saved in the variant and applied to the screen and to Excel and PDF exports.
  - The layout changes the presentation only; it never adds data the report definition does not provide, so permissions and data scope stay those of the report.
preconditions:
  - FR-CR-040, FR-CR-041.
main_flow:
  - The user opens the Layout panel.
  - The user selects columns, grouping, summaries and the chart.
  - BIBS redisplays the report with the layout; the user saves it in a variant.
alternate_flows:
  - Chart without a grouping field. BIBS asks for a grouping field first.
rules:
  - [R1, "Charts are exported to PDF and Excel only.", Fixed, "-"]
  - [R2, "Reports whose layout is prescribed (BIR books, forms, regulatory schedules) cannot be re-laid out.", Configurable, Report metadata]
validations:
  - [No column selected, Select at least one column, "-"]
  - [Chart without grouping, Select a grouping field for the chart, "-"]
notifications:
  - None.
audit:
  - Saved layouts are part of the variant audit.
acceptance:
  - A layout with two columns, a grouping by insurer and a bar chart is shown on screen and in the PDF export with the same subtotals.
  - A BIR book offers no Layout panel.
```

```fr
id: FR-CR-043
title: Schedule reports and distribute them
brd: [BR-054 (p.15), "Capability 21 bullet 5 (p.12)"]
actor: Every user with a report permission (own schedules); System Administrator (all)
priority: Must have
screens: Report runner (Schedule); My Subscriptions (Reports)
description:
  - A user schedules a report variant daily, weekly (chosen days) or monthly (chosen day or last working day) at a chosen time (Philippine time), in one or more formats, for himself and for other BIBS users or e-mail addresses allowed by the recipient policy.
  - A scheduled job runs the report with the owner's permissions, stores the file in the report archive and e-mails it (password protected when the report is marked confidential).
preconditions:
  - FR-CR-041 (a saved variant).
main_flow:
  - The user clicks Schedule on a variant.
  - The user sets frequency, time, formats and recipients and saves.
  - At each due time BIBS runs the report, archives the file and sends it.
  - The user sees each run and its result under My Subscriptions.
alternate_flows:
  - The owner lost the report permission. The subscription is suspended and the owner and the System Administrator are notified.
  - The run fails. BIBS retries once, then notifies the owner and the job-failure recipients.
rules:
  - [R1, "A subscription runs with its owner's permissions; recipients receive only the file.", Fixed, "-"]
  - [R2, "External recipients are allowed only for reports marked shareable and domains on the allowed list.", Configurable, Parameter REPORT_EXTERNAL_DOMAINS]
  - [R3, "At most 20 active subscriptions per user (default).", Configurable, Parameter REPORT_SUBSCRIPTIONS_MAX]
validations:
  - [No recipient, Add at least one recipient, "-"]
  - [External domain not allowed, This e-mail domain is not allowed for reports, "-"]
fields_screen: Schedule report
fields:
  - [Frequency, List, "Yes", Daily / Weekly / Monthly, "-"]
  - [Days, List, Cond., Days of week or day of month, Mandatory for Weekly and Monthly]
  - [Time, Time, "Yes", "-", Philippine time]
  - [Formats, List, "Yes", Formats of the report, At least one]
  - [Recipients, Text, "Yes", Users or e-mail addresses, Allowed domains]
  - [Active, Toggle, "Yes", "-", "-"]
notifications:
  - Each delivery to the recipients; failures to the owner.
audit:
  - Creation, change, suspension and every run are logged.
acceptance:
  - A weekly subscription runs on the chosen day and time and the file arrives with the variant's filters.
  - A subscription of a user whose report permission was removed stops and notifies him.
```

## Document generation

```fr
id: FR-CR-050
title: Generate business documents from maintained templates
brd: [BR-020 (p.13), BR-036 (p.14), BR-039 (p.14), BR-060 (p.16), BR-116 (p.19), BR-205 (p.25)]
actor: Business users; System Administrator (templates)
priority: Must have
screens: Templates (Broking Setup); document downloads on each record
description:
  - Quotations, proposal and placement slips, Insurance Advices, service invoices, receipts, remittance schedules, slips, letters and SOAs are generated from versioned templates with merge fields. Each document is downloadable as PDF and, where it is a document or schedule, as Word; it carries the BDO Insure logo and footer.
  - Documents that leave BDOI are password protected, with the password sent separately (FR-CR-071).
preconditions:
  - An active template for the document type.
main_flow:
  - A user triggers the document (for example Generate placement slip).
  - BIBS merges the record data into the active template and stores the document on the record.
  - The user downloads, prints or sends it.
alternate_flows:
  - No active template. BIBS names the missing template and does not generate.
rules:
  - [R1, "Template changes create a new version; documents keep the version they were generated with.", Fixed, "-"]
validations:
  - [Template missing, No active template for this document, "-"]
notifications:
  - As defined by the function FRS (for example e-mail of a slip to the insurer).
audit:
  - Generation, download and sending are recorded on the record.
acceptance:
  - A placement slip generated after a template change uses the new version; an earlier slip keeps the old one.
```

## Data management

```fr
id: FR-CR-060
title: Maintain lists of values with effectivity and approval
brd: [BR-004 (p.13), BR-172 (p.22), "Capability 17 bullet 3 (p.11)", "MILB volume (p.45)"]
actor: Authorised user (maker); approver
priority: Must have
screens: Lists of Values (Broking Setup)
description:
  - Authorised users add, change, deactivate and reactivate the values of maintainable lists (claim status, settlement types, adjusters, catastrophe codes, product types, payment types, disposition types and the others of the function BRDs) with an effectivity date. Changes take effect after approval. Deactivated values stay on existing records but cannot be selected.
preconditions:
  - The list is maintainable (not a fixed system list).
main_flow:
  - The maker opens the list, adds or changes a value and sets the effective date.
  - The approver approves; the value applies from the effective date.
alternate_flows:
  - Approver rejects. The change is not applied; the maker is notified.
rules:
  - [R1, "The maker never approves his own change.", Fixed, "-"]
validations:
  - [Effective date in the past, The effective date must be today or later, LOV_EFFECTIVITY_INVALID]
  - [List not maintainable, This list cannot be maintained, LOV_NOT_MAINTAINABLE]
notifications:
  - Approvers on submission; maker on decision.
audit:
  - Every change is in the master-data change log (FR-CR-031).
acceptance:
  - A deactivated value is no longer offered on new records and stays visible on old ones.
```

```fr
id: FR-CR-061
title: Maintain insurer records in one place
brd: [BR-171 (p.22), "Capability 17 bullet 2 (p.11)", "MILB volume (p.45)"]
actor: Authorised user (maker, by tab owner); approver
priority: Must have
screens: Insurer page (Catalog > Insurers), tabs Profile, Branches and LGT, Contacts, Products and Packages, Commission Rates, Payment Terms and Remittance, Risk Participation, Accounts
description:
  - One page shows everything BIBS holds about an insurer. Profile, branches with LGT rates, commission rates and the placement channel exist today; the page adds contacts, payment terms and the remittance schedule per remittance type, default risk participation (co-insurance shares) if BDOI confirms it (CRQ08), and a read-only view of the products and packages the insurer is on and of its GL and sub-ledger accounts.
  - Changes follow the catalog maker-checker and are logged field by field (FR-CR-031).
preconditions:
  - The insurer exists as a party of type Insurer.
main_flow:
  - The authorised user opens the insurer and edits a tab.
  - The user submits; the approver approves.
  - Quotations, placement, booking, remittance and reports use the approved values from their effective date.
alternate_flows:
  - Accreditation expired. BIBS warns on the insurer page and when the insurer is chosen on a new placement.
rules:
  - [R1, "Tabs are maintained by their owners: Marketing Business Services (profile, contacts, products), Comptrollership (commission rates, payment terms, accounts), Operations (remittance schedule) - default until CRQ08.", Configurable, Role permissions]
  - [R2, "Commission rates and remittance terms carry effective dates; history is kept.", Fixed, "-"]
validations:
  - [Accreditation end before start, The accreditation end date must be after its start, "-"]
  - [Shares over 100%, Risk participation shares must add up to 100%, "-"]
fields_screen: Insurer - Contacts and Payment Terms
fields:
  - [Contact name, Text, "Yes", "-", "-"]
  - [Function, List, "Yes", LOV INSURER_CONTACT_FUNCTION, "-"]
  - [E-mail, Text, "Yes", "-", Valid e-mail]
  - [Credit days, Number, "Yes", "-", 0-365]
  - [Remittance type, List, "Yes", With Incentives / Normal-Dollar / Normal-Peso, "-"]
  - [Remittance frequency, List, "Yes", Weekly / Semi-monthly / Monthly, "-"]
notifications:
  - Approvers on submission; accreditation expiry 30 days before (default) to the tab owner.
audit:
  - Field-level changes in the master-data change log.
acceptance:
  - A changed commission rate applies to bookings from its effective date and not before.
  - The insurer page shows the products and packages the insurer is on.
```

```fr
id: FR-CR-062
title: Define the MIS fields used for reports, dashboards and filters
brd: [BR-173 (p.23), BR-174 (p.23), "Capability 17 bullet 4 (p.11)"]
actor: System Administrator (maintain); every report user (use)
priority: Must have
screens: MIS Fields (Broking Setup); report runner filters and Layout panel
description:
  - A catalogue names each MIS field per core entity (user, product, insurer, claim, policy / account, transaction) with its business label, data type, list of values and the data it reads. It is delivered with the fields of BR-173 (product type, risk code, market segment, booking date, policy number, insurer, branch, account officer, status, transaction type, chart of account) and the MIS columns of the Report List (region, area, unit head, department, business origin).
  - Reports, dashboard widgets and exports offer the active MIS fields of their entity for filtering, grouping and export, with the same labels everywhere.
preconditions:
  - The field's data exists in BIBS; the catalogue does not create new data fields (CRQ09).
main_flow:
  - The System Administrator adds or changes a catalogue entry and activates it.
  - Report users find the field in the filters and the Layout panel of the reports of that entity.
alternate_flows:
  - Field deactivated. It disappears from filters; saved variants that used it show a warning.
rules:
  - [R1, "One label per MIS field across all reports.", Fixed, "-"]
  - [R2, "Only fields that map to existing data can be activated.", Fixed, "-"]
validations:
  - [Code already used, An MIS field with this code already exists, "-"]
fields_screen: MIS Fields
fields:
  - [Code, Text, "Yes", "-", Unique]
  - [Label, Text, "Yes", "-", "-"]
  - [Entity, List, "Yes", User / Product / Insurer / Claim / Account / Transaction, "-"]
  - [Source, List, "Yes", Mapped data elements of the entity, "-"]
  - [List of values, List, "No", LOV types, "-"]
  - [Active, Toggle, "Yes", "-", "-"]
notifications:
  - None.
audit:
  - Changes in the master-data change log.
acceptance:
  - Activating "Business origin" for accounts makes it available as a filter and grouping on the account reports.
```

```fr
id: FR-CR-063
title: Maintain products through Product Maintenance
brd: ["Capability 17 bullet 5 (p.11)", "Capability 20 (p.12)", BR-199 to BR-208 (p.24-25)]
actor: MBS, TSU, Marketing
priority: Must have
screens: Catalog; Package Requests (Product Maintenance)
description:
  - Product and package maintenance is specified by FRS BRD-3 (FR-PM-001 to FR-PM-081). The umbrella adds no requirement; its capability 20 bullets are traced in section 10.
preconditions:
  - None.
main_flow:
  - As FRS BRD-3.
alternate_flows:
  - As FRS BRD-3.
rules:
  - [R1, "Synchronisation with other BDOI systems (BR-206) waits for the target systems (PQ16).", Configurable, Product master feed (PQ16)]
validations:
  - [As FRS BRD-3, "-", "-"]
notifications:
  - As FRS BRD-3.
audit:
  - As FRS BRD-3.
acceptance:
  - The acceptance criteria of FRS BRD-3 apply.
```

## Security and access

```fr
id: FR-CR-070
title: Sign in, reset passwords and manage user access by request
brd: [BR-000 (p.13), BR-001 (p.13), BR-002 (p.13), BR-003 (p.13), "p.5 envisioned 3b"]
actor: Every user; System Administrator; UAM requestor and approver
priority: Must have
screens: Login; Reset password; User Access requests
description:
  - Users sign in with user ID and password (directory sign-in is added when BDO supplies the interface, decision D6). A user who forgot the password receives a reset link. Users are internal or external (portal) and hold one or more roles.
  - User accounts are created, changed, disabled and re-enabled only through User Access requests approved as in FRS BRD-11; the user ID itself is not changed.
preconditions:
  - None.
main_flow:
  - As FRS BRD-11 (sign-in, lock-out after 3 failed attempts, 30-minute inactivity log-out with a 15-minute warning, requests).
alternate_flows:
  - As FRS BRD-11.
rules:
  - [R1, "Lock-out after 3 failed attempts for every user (decision D5).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R2, "Accounts are disabled, never deleted, so that their history stays traceable (BR-003 'delete').", Fixed, "-"]
validations:
  - [Wrong credentials, Invalid user name or password, AUTHENTICATION_FAILED]
notifications:
  - Password reset link and access request decisions by e-mail.
audit:
  - Log-in, failed log-in, log-out and every access change.
acceptance:
  - The acceptance criteria of FRS BRD-11 apply.
```

```fr
id: FR-CR-071
title: Protect documents that leave BDOI
brd: ["Capability 4 bullet 22 (p.7)", BR-205 (p.25), "Capability 20 bullet 7 (p.12)"]
actor: System
priority: Must have
screens: Send e-mail dialogs of the records
description:
  - Documents e-mailed outside BDOI (quotation and placement slips, e-policies, Insurance Advices, TOR and master lists, reports marked confidential) are encrypted with a password. The password is either the standard assigned password or a system-generated one, and it is sent in a separate e-mail.
preconditions:
  - The document type is marked for protection.
main_flow:
  - The user sends the document.
  - BIBS protects the file and sends it, then sends the password separately.
alternate_flows:
  - Recipient domain not allowed. BIBS blocks the send.
rules:
  - [R1, "The password convention is BDOI's (Q07); until then the generated-password mode applies.", Configurable, Document password policy]
validations:
  - [Recipient missing, Add at least one recipient, "-"]
notifications:
  - None beyond the two e-mails.
audit:
  - Each send, with recipients and the protection used (never the password).
acceptance:
  - A slip received by the insurer opens only with the password from the second e-mail.
```

## Integration and data flow

```fr
id: FR-CR-080
title: Move work and data to the next process by configured workflow
brd: [BR-051 (p.15), BR-101 (p.18), BR-177 (p.23), BR-182 (p.23)]
actor: System; System Administrator (configuration)
priority: Must have
screens: My Work; workflow panels of the records
description:
  - Each business record moves through stages defined as data (stages, transitions, roles, SLA). A completed step places the record in the next queue and notifies it; other modules receive business events (for example invoice booked, receipt issued, client changed).
  - Files for other BDOI systems are handed to ports; their transport is configured when BDOI names the systems (Q08, PQ16).
preconditions:
  - The workflow of the record type is defined.
main_flow:
  - A user completes a step.
  - BIBS applies the transition, assigns the next queue and publishes the business event.
alternate_flows:
  - Receiving system unavailable. The event is kept and sent again; events that still fail are listed for support.
rules:
  - [R1, "Stages, roles and SLAs are maintained as configuration; a new stage or rule does not need a release.", Configurable, Workflow definitions]
validations:
  - [Transition not allowed, This action is not allowed at the current stage, "-"]
notifications:
  - Queue assignment and SLA breaches (FR-CR-020).
audit:
  - Every transition with user and time.
acceptance:
  - Submitting an account for placement puts it in the Processing queue and records the transition.
```

```fr
id: FR-CR-081
title: Deliver the day's service invoices to each insurer in one batch
brd: ["Invoice Batch Printing volumes (p.43)", BR-043 (p.15)]
actor: System; Processing and Comptrollership (view)
priority: Must have
screens: Insurer Invoice Batches (Client & Policy > Booking)
description:
  - Today each service invoice is e-mailed to the insurer. A daily job (after booking closes, default 20:00) also creates for each insurer one batch of that day's service invoices (merged PDF, or ZIP with a manifest), stores it and delivers it through the insurer's configured channel. The file transfer channel is added when BDOI supplies the file transfer addresses of the insurers (CRQ18). A delivery report lists each batch, invoice count, amount, channel and result.
preconditions:
  - Service invoices were issued for the insurer that day.
main_flow:
  - The job groups the day's service invoices by insurer.
  - BIBS creates and stores the batch and its manifest.
  - BIBS delivers it through the insurer's channel and records the result.
alternate_flows:
  - Delivery fails. BIBS retries at the next run and lists the batch as failed on the report.
  - Resend. An authorised user resends a stored batch.
rules:
  - [R1, "One batch per insurer per day; an invoice is in exactly one batch.", Fixed, "-"]
  - [R2, "Run time 20:00 PHT (default).", Configurable, Job schedule INSURER_INVOICE_BATCH]
validations:
  - [Resend of a batch still in delivery, The batch is being delivered, "-"]
notifications:
  - Failed deliveries to Processing and the job-failure recipients.
audit:
  - Batch creation, delivery attempts and resends.
acceptance:
  - All service invoices of a day appear once in the batch of their insurer and on the delivery report.
```

```fr
id: FR-CR-082
title: Receive migrated clients and legacy client batches during coexistence
brd: ["Client Migration volumes (p.43)", "Daily synchronisation from source (p.42-43)"]
actor: System; Data Migration lead
priority: Must have
screens: Bulk uploads (CLIENT_CREATE); client modification report
description:
  - Clients migrated from the legacy systems load through the bulk client upload with the duplicate check, as the Data Migration BRD specifies (BRID 2.1). If BDOI runs the legacy and BIBS in coexistence, the midday and end-of-day client batches of the umbrella (p.43) load new and changed legacy clients through a feed port, and a daily client modification report lists what changed.
  - The scope, period ("2020 to present") and source systems are decided with the Data Migration BRD (CRQ19).
preconditions:
  - Data Migration decisions for the client master (BRID 1.1).
main_flow:
  - The migration or feed file arrives.
  - BIBS validates, de-duplicates and loads it; fall-outs are listed for correction.
  - BIBS produces the load report and, for feeds, the client modification report.
alternate_flows:
  - Duplicate found. The row falls out with the matching client code.
rules:
  - [R1, "Legacy client codes are kept as a cross-reference.", Fixed, "-"]
validations:
  - [Mandatory field missing, The row names the missing fields, "-"]
notifications:
  - Load completed or failed, to the Data Migration lead.
audit:
  - Each load with counts of loaded, updated and fallen-out rows.
acceptance:
  - A test migration file loads with every row either loaded or listed as a fall-out with its reason.
```

```fr
id: FR-CR-083
title: Process in bulk and individually with concurrent users
brd: ["p.4 benefit 1c", BR-063 (p.16)]
actor: Every user; System
priority: Must have
screens: Bulk uploads; batch job monitor
description:
  - Every transaction that the BRDs ask to run in bulk (clients, accounts, quotations, bookings, payments, dispositions, adjustments) has an upload template and a batch job; failed rows are reported and can be corrected and re-submitted while the rest proceeds. The same functions exist for single records.
  - Jobs hold a lock so that the same job never runs twice at the same time.
preconditions:
  - None.
main_flow:
  - The user uploads a file with the template.
  - BIBS validates each row, processes the valid rows and reports the rest.
alternate_flows:
  - Same file uploaded twice. BIBS refuses the duplicate upload.
rules:
  - [R1, "A failed row never stops the batch.", Fixed, "-"]
validations:
  - [Wrong template, The file does not match the template, "-"]
notifications:
  - Batch completion with counts.
audit:
  - Each batch with user, file, counts and result.
acceptance:
  - A file with valid and invalid rows loads the valid rows and lists every invalid row with its reason.
```

## Other cross-cutting requirements

```fr
id: FR-CR-090
title: Show the Invoice Master List across workflows
brd: [BR-175 (p.23)]
actor: Operations, Comptrollership, Marketing (view)
priority: Must have
screens: Operations > Invoice Ledger, view "Invoice Master List"
description:
  - The invoice ledger of Operations becomes the Invoice Master List named by BR-175. It shows reference number, invoice number, cover number, version number, client code, status, assured's name, product line, inception and expiry dates, department, insurer, Marketing and Processing assignees, placement status, approval dates, cancellation status and a link to the audit trail. Cover number, version number, assignees, placement status and approval dates are added to today's columns.
preconditions:
  - The user holds the invoice ledger permission.
main_flow:
  - The user opens the Invoice Master List view and filters it.
  - The user opens an invoice to see its history and related transactions.
alternate_flows:
  - None.
rules:
  - [R1, "Columns and owner confirmed by BDOI (CRQ13).", Configurable, Saved view]
validations:
  - [No filter on more than 12 months, Narrow the booking date range to 12 months or less, "-"]
notifications:
  - None.
audit:
  - Exports are logged.
acceptance:
  - Every column named in BR-175 is available in the view and the export.
```

```fr
id: FR-CR-091
title: Select currency on accounts and report in several currencies
brd: [BR-027 (p.14), BR-088 (p.18)]
actor: Marketing, Processing, Finance
priority: Must have
screens: Account; reports with a currency parameter
description:
  - Accounts carry a currency (default PHP). Premium is computed in the account currency; accounting converts at the book rate and revalues open USD items at month end (FRS BRD-5). Reports show the transaction currency and the peso equivalent where the BRDs ask for both.
preconditions:
  - The currency and its rates are maintained.
main_flow:
  - The user selects the currency on the account; the rest follows the function FRS.
alternate_flows:
  - No rate for the date. Posting stops with the missing-rate message of FRS BRD-5.
rules:
  - [R1, "Default currency PHP.", Configurable, Parameter]
validations:
  - [Currency not active, Select an active currency, "-"]
notifications:
  - None.
audit:
  - As the account.
acceptance:
  - A USD account books in USD with the peso equivalent at the booking rate.
```

```fr
id: FR-CR-092
title: Apply the BDO brand, design system and user journeys on every screen
brd: [BR-178 (p.23), BR-179 (p.23), BR-180 (p.23), BR-181 (p.23), BR-183 (p.23)]
actor: Every user
priority: Must have
screens: All screens and documents
description:
  - Screens use the BDO Insure colours (Header Blue, CTA Blue, Yellow), logo and typography of the UX guidelines; documents, reports and exports carry the logo, blue table headers and the "Confidential" footer. A screen-by-screen alignment pass against the BDOI UX design (deliverable 18) is made before FRS v1.1.
  - BDO's prescribed icons and illustrations are used when BDOI supplies them (CRQ17). The identified user journeys (BR-181) are the end-to-end walkthroughs of the function FRS and the swimlanes of their UX Screen Decks; the Storyboard index of this FRS maps each capability to them.
  - The design hand-over to the BDOI UX Design team (BR-183, the use of Figma for design execution) is made through the UX documents of each business sign-off pack - the 07 UX Screen Deck, the 08 UX screen register and the 09 image package of every screen and state at twice the screen resolution - which the UX Design team imports into its design tool (CLR-CR-17).
preconditions:
  - None.
main_flow:
  - The project team reviews each screen against the BDOI UX design and records the deviations.
  - Each deviation is corrected before the annotated screenshots of FRS v1.1 are taken.
alternate_flows:
  - Deviation that needs a BDOI decision (for example an icon not yet supplied). It is logged in the register and closed after CRQ17.
rules:
  - [R1, "Brand colours, fonts and logos are defined once and used by screens, documents and exports.", Fixed, "-"]
validations:
  - [None, "-", "-"]
notifications:
  - None.
audit:
  - None.
acceptance:
  - The alignment review finds no screen outside the BDO colour and layout rules.
  - Each business sign-off pack carries the 07, 08 and 09 UX documents for the BDOI UX Design team.
```

```fr
id: FR-CR-093
title: Issue the SOA and the insurer's service invoice at booking
brd: [BR-039 (p.14), BR-043 (p.15)]
actor: Processing
priority: Must have
screens: Booking; Service Invoices; Billing Statements (Collections)
description:
  - At booking BIBS issues the service invoice to each insurer share (FRS BRD-1). The umbrella also asks for an SOA generated with the placement report and Insurance Advice, and issued with the service invoice at booking. The client SOA exists today as the Collections billing statement per billing cycle. Which SOA is meant, and whether it must be produced at booking, is open (CRQ12); once answered, the booking step triggers that SOA from its template.
preconditions:
  - CRQ12 answered.
main_flow:
  - The account is booked.
  - BIBS issues the service invoice and, after CRQ12, the SOA from its template.
alternate_flows:
  - None.
rules:
  - [R1, "Until CRQ12 is answered, no SOA is generated at booking.", Configurable, Booking trigger set-up]
validations:
  - [None, "-", "-"]
notifications:
  - As FRS BRD-1 (service invoice sent or failed).
audit:
  - As FRS BRD-1.
acceptance:
  - After CRQ12, booking an account produces the service invoice and the SOA with the same invoice number.
```

# Proposed business rules and clarifications for confirmation

The umbrella BRD has requirements that no function BRD owns, or that conflict with their BRD. The table gives, for each, the proposed rule or FR and the decision requested from BDOI. BDOI records its decision with the sign-off of this FRS (chapter 11); a decision that changes a screen, field, rule or message is applied in the next version of the FRS.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-CR-01 | Reinsurance (CORE-15.01-15.10) | Phase 2: the ReInsurance BRD is received; phase 1 keeps party types, templates, subscriptions, open-item matching and event types open for the phase 2 module (section 6). | The insurer-side reinsurance module does not fit the broker model. | Confirm reinsurance in phase 2. |
| CLR-CR-02 | Claims cheque safekeeping (CORE-14.08-14.10) | No cheque custody in phase 1; if BDOI holds claim settlement cheques, a cheque custody register is added in Claims with hand-over to and from Cashiering. | Whether BDOI holds claim cheques is open (CRQ03, CLQ10). | Answer CRQ03. |
| CLR-CR-03 | CSF case resolution (CORE-16.06) | Case resolution is BDOI scope: the signed CSF Addendum - Case Management (BRCSFCM.001-012) is specified in FRS BRD-9 (FR-CSF-050 to 061). Its delivery drop and plan follow the decision of BDOI and the Change Control Board (FRS BRD-9 CLR-CSF-07). | The addendum answers CRQ04 and the cross-BRD conflict XQ12 (DCR-163); change request CR-0005 kept case resolution out of phase 1. | Decide the delivery drop of the Case Management module (CLR-CSF-07); close DCR-163 and CR-0005 against it. |
| CLR-CR-04 | Master data change log (CORE-17.01; FR-CR-031) | A field-level log of registered master data. | No function BRD owns the log. | Confirm the master data in the log (CRQ07). |
| CLR-CR-05 | Insurer management (CORE-17.02; FR-CR-061) | An insurer page with the attributes the function BRDs do not hold. | No function BRD owns the insurer record. | Confirm the insurer attributes (CRQ08). |
| CLR-CR-06 | MIS field definition (CORE-17.04; FR-CR-062) | A catalogue names the MIS fields of the existing data; users do not add data fields. | No function BRD owns MIS fields. | Confirm the catalogue (CRQ09). |
| CLR-CR-07 | Report customisation (CORE-21.04; FR-CR-042) | The user saves the layout of a report in a variant. | No function BRD owns report customisation. | Confirm the variants. |
| CLR-CR-08 | Scheduled reports (CORE-21.05; FR-CR-043) | The user subscribes to a report with a schedule and recipients (at most 20 active subscriptions per user by default). | No function BRD owns report scheduling. | Confirm the subscriptions. |
| CLR-CR-09 | Emerging capabilities (CORE-18.01) | Not in scope until BDOI lists them. | The capability is not defined (CRQ10). | List the capabilities (CRQ10). |
| CLR-CR-10 | ALeA e-mail address encoding (XC-21) | Not in scope until the requirement is defined. | The requirement is not defined (CRQ20). | Define the requirement (CRQ20). |
| CLR-CR-11 | Role dashboards (XC-02; FR-CR-010, 011) | Each role has a home page with its counts and queues, refreshed every 5 minutes by default. | The umbrella asks for centralised dashboards for all roles; the contents per role are open (CRQ06). | Define the dashboard contents per role (CRQ06). |
| CLR-CR-12 | Invoice Master List (XC-13; FR-CR-090) | The Invoice Master List columns are added to the invoice ledger view. | No function BRD owns the list. | Confirm the columns. |
| CLR-CR-13 | Insurer invoice batch (XC-19; FR-CR-081) | A daily batch per insurer of the day's service invoices, stored and delivered through the insurer's channel; the file transfer channel is added when BDOI supplies the addresses. | The file format and whether the batch replaces the e-mail are open (CRQ18). | Answer CRQ18. |
| CLR-CR-14 | Client migration and daily client batches (XC-20; FR-CR-082) | Clients are loaded and fed through the Data Migration programme with the duplicate check. | The migration scope and coexistence are decided by the Data Migration BRD (CRQ19). | Confirm (CRQ19). |
| CLR-CR-15 | SOA at booking (XC-10; FR-CR-093) | Booking issues the service invoice; the SOA is issued at booking once BDOI says which SOA is meant. | The SOA of the placement report and booking is open (CRQ12). | Answer CRQ12. |
| CLR-CR-16 | Directory sign-in (FR-CR-070) | Users sign in with user ID and password; directory sign-in is added when BDO supplies the interface. | Cross-BRD decision D6. | Confirm decision D6. |
| CLR-CR-17 | Design hand-over (BR-183; XC-16; FR-CR-092) | The design hand-over to the BDOI UX Design team is the 07 UX Screen Deck, the 08 UX screen register and the 09 image package of each business sign-off pack, which the team imports into its design tool; the project team does not keep the screens in Figma. | BR-183 asks the vendor to use Figma for design execution; BIBS screens follow the platform design system. | Confirm that the UX documents meet BR-183, or name the Figma deliverables expected. |

# Reinsurance (phase 2)

Capability 15 (6 bullets, BR-153 to BR-160) and 4 more items of the capability matrix (p.28) belong to the ReInsurance BRD. That BRD is received and is **phase 2**: it is not in phase 1 and its rows are marked "Phase 2" in section 10. The insurer-side reinsurance module of the platform (treaties and cessions of an insurance company) is not the broker model BDOI needs and stays hidden from BDOI roles.

Phase 1 keeps these points open so that the phase 2 module can reuse them: party types for reinsurers and RI brokers; template-driven placement slips and SOAs; the report subscriptions of FR-CR-043 for SOA scheduling; open-item matching for net settlement; accounting event types added without changing existing posting rules; and a claim party model open to reinsurers.

# Non-functional requirements

<!-- table: widths=2.8,4.6,4.8,3.4 caption="Non-functional requirements of the umbrella (p.42-46)" size=8 -->
| Topic | Umbrella value | Function BRDs | BIBS target and approach |
|---|---|---|---|
| Users | 1,344 named / 429 concurrent (sum of the BRD rows; 388 without Reinsurance) | 150 concurrent + 20% a year (register proposal); BRD-8 20 internal; BRD-7 47 / 25 | Performance tests at 429 concurrent sessions as the peak case (CRQ21, CRQ24) |
| Response time | Under 5 seconds for every role | 2 s (BRD-12) to 10 s (BRD-1, BRD-6, BRD-11); reports 20 s to 15 min | p95 under 2 s for screens; reports and batches as jobs with progress (CRQ21) |
| Volumes | Per BRD (p.42-45), e.g. NB 21,200 a month per transaction type, RMEL 25,800, CSF 144,400 a year, CMS reports 60,000 weekly | Same values in the BRDs | Sized in the performance plan (deliverable 28) |
| Peak, availability, maintenance, BCP | "Refer to BRD" | 99.9%-99.99%; windows differ | Register proposal: 99.9% in service hours 06:00-22:00 Mon-Sat, maintenance 00:00-04:00, RTO 4 h |
| Retention | Application, system and audit logs, historical data: 5 years online, 15 years offline | BRD-7 10 / 15; BRD-5, 10, 12 5 / 5 | Default 5 / 15 per record type through the retention rules; exceptions after CRQ22 |
| Backup | Every 4 hours, kept 5 years | Daily to every 15 minutes; kept 5 or 7 years | Continuous log archiving plus a base backup every 4 hours, kept 7 years |
| Anonymisation | No | - | Production data not anonymised; non-production data masked (hosting appendix) |
| Hosting and access | - | - | AWS ap-southeast-1; access restricted to personnel in the Philippines; migration staging purged within 5 days |
| Security and audit | Role-based access; password protection; audit logs (p.4-5) | Per BRD | FR-CR-001, 030, 031, 070, 071 |

# Configuration items owned by the System Administrator

<!-- table: widths=5.4,2.6,8.6 caption="New parameters, lists and set-up (proposed defaults)" size=8.5 -->
| Item | Default | Meaning |
|---|---|---|
| DASHBOARD_REFRESH_MINUTES | 5 | Refresh interval of role home widgets |
| Role Home Set-up | Default widget set per role family (section 3.1) | Widgets, order and thresholds per role |
| REPORT_SUBSCRIPTIONS_MAX | 20 | Active report subscriptions per user |
| REPORT_EXTERNAL_DOMAINS | bdo.com.ph | E-mail domains allowed as external report recipients |
| Job REPORT_SUBSCRIPTIONS | Every 15 minutes | Runs due report subscriptions |
| Change-log registration | Users and roles, products and versions, insurers and branches, commission rates, LOVs, chart of accounts, payees, parameters, role home, MIS fields | Master types logged field by field (CRQ07) |
| MIS field catalogue | Fields of BR-173 and the Report List MIS columns | MIS fields per entity |
| LOV INSURER_CONTACT_FUNCTION | Placement; Underwriting; Claims; Billing and remittance; Accreditation; Others | Function of an insurer contact |
| Job INSURER_INVOICE_BATCH | 20:00 PHT daily | Daily insurer invoice batch |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-CR-01 | The function BRDs and their FRS govern the business processes; the umbrella adds the cross-cutting requirements of section 4 | p.3, footnote 1 |
| A-CR-02 | "Non-Package Management" (capability 4) is the non-package placement of New Business | CRQ02 |
| A-CR-03 | "Delete" of a prospect or a user means deactivation, with purge under the retention rules | CRQ05 |
| A-CR-04 | Reinsurance is phase 2; the insurer-side reinsurance module is not used for BDOI | Section 6 |
| A-CR-05 | BR-178 to BR-183 are met by the design system, the alignment pass, the walkthroughs of the function FRS and the UX documents 07 to 09 of each business sign-off pack | XC-14 to XC-16 |
| A-CR-06 | The MIS field catalogue names existing data; users do not add data fields | CRQ09 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-CR-01 | BDOI defines the dashboard contents per role | FR-CR-010, 011 (CRQ06) |
| D-CR-02 | BDOI confirms the master data in the change log and the insurer attributes | FR-CR-031, 061 (CRQ07, CRQ08) |
| D-CR-03 | BDOI supplies the file transfer addresses of the insurers | FR-CR-081 (CRQ18) |
| D-CR-04 | The Data Migration BRD decides the client migration scope and coexistence | FR-CR-082 (CRQ19) |
| D-CR-05 | BDOI supplies the icon set and design files | FR-CR-092 (CRQ17) |
| D-CR-06 | BDOI signs off the FRS of Renewal, Claims, Employee Benefits, CSF and Submitted Policies | Section 10 |

## Open questions

<!-- table: widths=1.4,10.9,2.6,1.7 caption="Open questions on the umbrella BRD" status=Status size=8 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| CRQ01 | Is the umbrella over all twelve BRDs, Data Migration and ReInsurance? Who owns Data Management (capability 17)? | Scope | OPEN |
| CRQ02 | Is Non-Package Management the NB non-package placement (no BR ID, added 20-Nov-2025)? | CORE-04 | OPEN |
| CRQ03 | Does BDOI hold claim settlement cheques (safekeeping, hand-over to and retrieval from Cashiering, BR-146-148)? | CLR-CR-02 | OPEN |
| CRQ04 | Is CSF case resolution (BR-165) in phase 1, given BRD-9 keeps it in SharePoint? | CLR-CR-03 | PARTIAL (in scope by the Case Management Addendum; drop open, CLR-CSF-07) |
| CRQ05 | Is deactivation acceptable for "delete of prospect" (BR-008)? | A-CR-03 | OPEN |
| CRQ06 | Which figures does each role see on its home page (BR-125, 152, 160)? | FR-CR-010 | OPEN |
| CRQ07 | Which master data is in the change log; old and new values; approver; who views it? | FR-CR-031 | OPEN |
| CRQ08 | Meaning of risk participation, payment terms and remittance schedules on the insurer; owners and approval | FR-CR-061 | OPEN |
| CRQ09 | Is MIS field definition a fixed list per entity or user-defined fields? | FR-CR-062 | OPEN |
| CRQ10 | List the emerging capabilities or withdraw BR-176 | CLR-CR-09 | OPEN |
| CRQ11 | Which reports need custom layouts and schedules; formats and delivery; Word for all reports? | FR-CR-042, 043 | OPEN |
| CRQ12 | Which SOA is issued with the placement report and at booking (BR-039, BR-043)? | FR-CR-093 | OPEN |
| CRQ13 | Is the Invoice Master List the Operations invoice ledger; columns and owner? | FR-CR-090 | OPEN |
| CRQ14 | Letter name: Non-Renewal Letter (NRL) or Not for Renewal Letter (NFR)? | CORE-06.07 | OPEN |
| CRQ15 | What does "Bank Account Operations" cover? | CORE-08.21 | OPEN |
| CRQ16 | Do CSF agents also resend e-policies (BR-164)? | CORE-16.04 | ANSWERED (BRD-9 e-mail item 9; FR-CSF-031) |
| CRQ17 | Supply the BDO icon set and design or Figma files | FR-CR-092 | OPEN |
| CRQ18 | Insurer invoice batch: file format, file transfer addresses, replaces e-mail? | FR-CR-081 | OPEN |
| CRQ19 | Client migration source, period (2020 to present) and duration of daily batches | FR-CR-082 | OPEN |
| CRQ20 | What is ALeA and its "e-mail address encoding"? | CLR-CR-10 | OPEN |
| CRQ21 | Response under 5 s for every role and 429 concurrent users: which values govern? | Section 7 | OPEN |
| CRQ22 | Do the umbrella retention and backup values replace the BRD-specific ones? | Section 7 | OPEN |
| CRQ23 | Send "E2E BDOI Mapping.xlsx"; confirm the final IDs of each BRD govern | Section 10 | OPEN |
| CRQ24 | User counts of EB, Claims and Product Maintenance differ from their BRDs | Section 7 | OPEN |
| CRQ25 | Is the umbrella approved (one approver on leave without signature; one "not applicable"; four by e-mail)? | Sign-off | OPEN |

<!-- landscape -->

# Traceability

Every capability bullet and cross-cutting row of the umbrella BRD, the BRD that specifies it and the FR that meets it. "Covered by" gives the BIBS BRD and its requirement IDs; FR IDs without a prefix note belong to the FRS of that BRD. FR-CL-nnn are the FRs of BRD-4 Collections; BRD-7 Claims numbers its FRs FR-CM-nnn (renumbered from FR-CL-nnn, DCR-188). Reinsurance rows are "Phase 2".

<!-- table: widths=1.7,6.2,2.4,5.4,4.4 caption="Traceability of the umbrella BRD" size=7 -->
| ID | Capability (Core BRD page) | Core BR | Covered by | FR |
|---|---|---|---|---|
| CORE-01.01 | Create prospect / record (p.6) | BR-005 | BRD-1 BRNB.090, BRNB.101, BRNB.048 | FR-NB-034, FR-NB-031 |
| CORE-01.02 | Update prospect / record (p.6) | BR-006 | BRD-1 BRNB.049, BRNB.047 | FR-NB-032, FR-NB-035 |
| CORE-01.03 | Convert prospect into client record (p.6) | BR-007 | BRD-1 BRNB.090, BRNB.101 | FR-NB-034 |
| CORE-01.04 | Delete prospect / record (p.6) | BR-008 | BRD-1 BRNB.019, BRNB.106 (BIBS voids instead of deleting: FRS BRD-1 CLR-NB-04) | FR-NB-012, FR-NB-137 |
| CORE-01.05 | Upload and validate client documents (p.6) | BR-009, BR-010, BR-011 | BRD-1 BRNB.030, BRNB.049, BRNB.026 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 ... |
| CORE-01.06 | Client search (p.6) | BR-012 | BRD-1 BRNB.046 | FR-NB-030 |
| CORE-01.07 | Generate client code (p.6) | BR-013 | BRD-1 BRNB.030, BRNB.101 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 |
| CORE-02.01 | Receive request for quotation / proposal (p.6) | BR-014 | BRD-1 BRNB.041, BRNB.023 | FR-NB-040 |
| CORE-02.02 | Create quotation / proposal (individual or bulk) (p.6) | BR-015, BR-016 | BRD-1 BRNB.043, BRNB.042, BRNB.028 | FR-NB-041, FR-NB-044, FR-NB-046 |
| CORE-02.03 | Track proposal status (p.6) | BR-017 | BRD-1 BRNB.022, BRNB.115 | FR-NB-010, FR-NB-122, FR-NB-120 |
| CORE-02.04 | Edit quotation / proposal (p.6) | BR-018 | BRD-1 BRNB.020 | FR-NB-042 |
| CORE-02.05 | Approve quotation / proposal (p.6) | BR-019 | BRD-1 BRNB.021, BRNB.014 | FR-NB-043, FR-NB-014, FR-NB-051 |
| CORE-02.06 | Print quotation / proposal (p.6) | BR-020 | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 |
| CORE-02.07 | Upload documents (p.6) | BR-021 | BRD-1 BRNB.055, BRNB.026 | FR-NB-017 |
| CORE-02.08 | Send quotation / proposal (p.6) | BR-022 | BRD-1 BRNB.043, BRNB.042 | FR-NB-041, FR-NB-044, FR-NB-046 |
| CORE-02.09 | Generate and customise report (p.6) | BR-053 | BRD-1 BRNB.057, BRNB.075 | FR-NB-123, FR-NB-122 |
| CORE-03.01 | Create account (manual, bulk upload, system) (p.6) | BR-023, BR-024, BR-025, BR-028 | BRD-1 BRNB.051, BRNB.066, BRNB.039 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 ... |
| CORE-03.02 | Update account (p.6) | BR-029 | BRD-1 BRNB.025, BRNB.053, BRNB.054 | FR-NB-064, FR-NB-065 |
| CORE-03.03 | Link to client record (p.6) | BR-030 | BRD-1 BRNB.051, BRNB.099 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-037 |
| CORE-03.04 | Approve account (p.6) | BR-031 | BRD-1 BRNB.022, BRNB.079 | FR-NB-010, FR-NB-122, FR-NB-133 |
| CORE-03.05 | Initiate placement request (p.6) | BR-033 | BRD-1 BRNB.069 | FR-NB-080 |
| CORE-03.06 | Client confirmation (capability matrix p.26) | BR-038 | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 |
| CORE-04.01 | Create proposal request form (PRF) (p.7) | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 |
| CORE-04.02 | Approve PRF (p.7) | - | BRD-1 BRNB.005, BRNB.014 | FR-NB-014, FR-NB-050, FR-NB-051, FR-NB-043 |
| CORE-04.03 | Submit PRF (p.7) | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 |
| CORE-04.04 | Receive PRF (TSU) (p.7) | - | BRD-1 BRNB.007 | FR-NB-052 |
| CORE-04.05 | Edit PRF (p.7) | - | BRD-1 BRNB.007 | FR-NB-052 |
| CORE-04.06 | Create Quotation Slip (p.7) | - | BRD-1 BRNB.008 | FR-NB-053 |
| CORE-04.07 | Send Quotation Slip to insurers (p.7) | - | BRD-1 BRNB.008 | FR-NB-053 |
| CORE-04.08 | Input insurers' feedback in the comparative table (p.7) | - | BRD-1 BRNB.009 | FR-NB-054, FR-NB-055 |
| CORE-04.09 | Submit comparative table (p.7) | - | BRD-1 BRNB.010 | FR-NB-055 |
| CORE-04.10 | Finalise Proposal Slip (p.7) | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 |
| CORE-04.11 | Submit Proposal Slip (p.7) | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 |
| CORE-04.12 | Receive and print quotation (p.7) | - | BRD-1 BRNB.017, BRNB.043 | FR-NB-056, FR-NB-057, FR-NB-041, FR-NB-044 |
| CORE-04.13 | Send quotation (p.7) | - | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 |
| CORE-04.14 | Convert quotation to account (p.7) | - | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 |
| CORE-04.15 | Edit account (p.7) | - | BRD-1 BRNB.053 | FR-NB-064 |
| CORE-04.16 | Approve account (p.7) | - | BRD-1 BRNB.022 | FR-NB-010, FR-NB-122 |
| CORE-04.17 | Initiate policy placement request (p.7) | - | BRD-1 BRNB.069 | FR-NB-080 |
| CORE-04.18 | Track PRF / proposal placement slip / quotation / placement request status (p.7) | - | BRD-1 BRNB.012, BRNB.022, BRNB.115 | FR-NB-120, FR-NB-010, FR-NB-122 |
| CORE-04.19 | Upload documents (p.7) | - | BRD-1 BRNB.055 | FR-NB-017 |
| CORE-04.20 | Generate and customise report (p.7) | - | BRD-1 BRNB.011, BRNB.057 | FR-NB-121, FR-NB-123 |
| CORE-04.21 | Automated reference number generation (p.7) | - | BRD-1 BRNB.006 | FR-NB-047, FR-NB-050 |
| CORE-04.22 | Password protection and encryption of shared documents (p.7) | - | BRD-1 BRNB.013 | FR-NB-013, FR-NB-044, FR-NB-057 |
| CORE-04.23 | System notification on status changes (p.7) | - | BRD-1 BRNB.015 | FR-NB-015 |
| CORE-04.24 | Complete logging of all actions (p.7) | - | BRD-1 BRNB.016 | FR-NB-016 |
| CORE-05.01 | Submit for placement and booking (p.7) | BR-032 | BRD-1 BRNB.022, BRNB.096 | FR-NB-010, FR-NB-122, FR-NB-011, FR-NB-064 |
| CORE-05.02 | Generate placement slip (p.7) | BR-036 | BRD-1 BRNB.069 | FR-NB-080 |
| CORE-05.03 | Generate placement report (p.7) | BR-039 | BRD-1 BRNB.011, BRNB.075 | FR-NB-121, FR-NB-122 |
| CORE-05.04 | Send placement slip to insurer (p.7) | BR-037 | BRD-1 BRNB.071 | FR-NB-081 |
| CORE-05.05 | Generate Insurance Advice (p.7) | BR-039 | BRD-1 BRNB.070, BRNB.060 | FR-NB-103, FR-NB-104 |
| CORE-05.06 | Receive e-policy (individual or batch) (p.7) | BR-040 | BRD-1 BRNB.073 | FR-NB-100 |
| CORE-05.07 | Send e-policy to client (individual or batch) (p.7) | BR-041 | BRD-1 BRNB.077 | FR-NB-105 |
| CORE-05.08 | Update client record with the e-policy and policy number (p.7) | BR-042 | BRD-1 BRNB.074 | FR-NB-101 |
| CORE-06.01 | Generate RMEL (list of expiring accounts) (p.8) | BR-055 | BRD-6 BRRN.002, BRRN.030, BRRN.005 | FR-RN-011, FR-RN-010, FR-RN-112 |
| CORE-06.02 | Filter and distribute RMEL (p.8) | BR-056 | BRD-6 BRRN.003, BRRN.011, BRRN.036 | FR-RN-012, FR-RN-040, FR-RN-102 |
| CORE-06.03 | Rules-based sanitation of accounts (bulk / individual) (p.8) | BR-026, BR-057 | BRD-6 BRRN.020, BRRN.023, BRRN.009 | FR-RN-020, FR-RN-103, FR-RN-004, FR-RN-022 ... |
| CORE-06.04 | Provide disposition (online or by upload) (p.8) | BR-058, BR-059 | BRD-6 BRRN.031, BRRN.018 | FR-RN-004, FR-RN-023, FR-RN-051, FR-RN-103 ... |
| CORE-06.05 | Process and generate renewal proposal (p.8) | - | BRD-6 BRRN.033, BRRN.038 | FR-RN-048, FR-RN-064, FR-RN-084 |
| CORE-06.06 | Generate and send Renewal Advice (p.8) | BR-060, BR-061 | BRD-6 BRRN.010 | FR-RN-080, FR-RN-081, FR-RN-090 |
| CORE-06.07 | Send No Advice Letter (NAL) and Non-Renewal Letter (NRL) (p.8) | BR-060, BR-061 | BRD-6 BRRN.001, BRRN.009 | FR-RN-082, FR-RN-103, FR-RN-024, FR-RN-112 |
| CORE-06.08 | Check renewal payment (capability matrix p.27) | BR-062 | BRD-6 BRRN.027 | FR-RN-042 |
| CORE-06.09 | Track renewal status (BR table only) | BR-064 | BRD-6 BRRN.036 | FR-RN-102 |
| CORE-07.01 | Premium Receivable (PR) management (p.8) | BR-044, BR-045 | BRD-4 BRCLXN.001, BRCLXN.011, BRCLXN.046 | FR-CL-010, FR-CL-015, FR-CL-016 |
| CORE-07.02 | Disposition tracking and management (p.8) | BR-048 | BRD-4 BRCLXN.016, BRCLXN.021, BRCLXN.023 | FR-CL-030, FR-CL-031, FR-CL-018 |
| CORE-07.03 | Tag CWT, premium / PR2307 (p.8) | BR-118, BR-139 | BRD-4 BRCLXN.026, BRCLXN.027; BRD-2 MKTID.013 (CSHID.026 removed from the Operations BRD) | FR-CL-032, FR-CL-081, FR-OP-113 |
| CORE-07.04 | Unapplied payment disposition (excess payment) (p.8) | BR-046, BR-047, BR-090 | BRD-4 BRCLXN.030, BRCLXN.034, BRCLXN.041 | FR-CL-074, FR-CL-070, FR-CL-077 |
| CORE-07.05 | Reporting and audit (p.8) | BR-053, BR-054 | BRD-4 BRCLXN.028, BRCLXN.043, BRCLXN.045 | FR-CL-082, FR-CL-003, FR-CL-083 |
| CORE-07.06 | Batch processing and automation (capability matrix p.27) | BR-049 | BRD-4 BRCLXN.013, BRCLXN.024, BRCLXN.041 | FR-CL-017, FR-CL-032, FR-CL-080, FR-CL-077 |
| CORE-08.01 | Daily financial report reconciliation (p.8) | BR-065 | BRD-5 ACSL 2.13.0, ACSL 2.13.2 | FR-AS-003, FR-AS-004 |
| CORE-08.02 | Daily cash movement reconciliation (p.8) | BR-067 | BRD-5 FRBS 3.3.0 | FR-AC-050 |
| CORE-08.03 | Insurer's statement of accounts (SOA) reconciliation (p.8) | BR-068 | BRD-5 ACSL 2.13.1, ACSL 2.14.1 | FR-AS-003 |
| CORE-08.04 | Generate automated journal entries (p.8) | BR-069 | BRD-5 ACSL 2.15.0, FRBS 3.1.0 | FR-AS-024, FR-AC-030 |
| CORE-08.05 | Perform manual entries (p.8) | BR-070 | BRD-5 FRBS 2.8.0, FRBS 2.8.5 | FR-AC-032 |
| CORE-08.06 | Perform manual / invoice adjustments (p.8) | BR-071 | BRD-5 ACSL 2.9.0, ACSL 2.9.1 | FR-AS-021 |
| CORE-08.07 | Perform accrual (p.8) | BR-072 | BRD-5 FRBS 2.8.1 | FR-AC-032, FR-AC-033 |
| CORE-08.08 | Perform revaluation (p.8) | BR-073 | BRD-5 FRBS 2.2.0, FRBS 3.5.0 | FR-AC-010, FR-AC-043 |
| CORE-08.09 | Perform month-end and year-end closing (p.8) | - | BRD-5 FRBS 3.4.0 | FR-AC-042 |
| CORE-08.10 | Generate financial reports (p.8) | BR-066 | BRD-5 FRBS 3.2.0, DIS 3.28.0 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 ... |
| CORE-08.11 | Accounts analysis (p.8) | BR-074 | BRD-5 ACSL 2.5.0, ACSL 2.5.3 | FR-AS-010 |
| CORE-08.12 | Receive and release CWT (commission / supplier) (p.8) | BR-075, BR-076 | BRD-5 DIS 2.11.0, DIS 2.12.0 | FR-DS-057, FR-DS-058 |
| CORE-08.13 | Disbursement to insurer, client, supplier, BDO subsidiaries (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 |
| CORE-08.14 | Reporting and documentation (p.8) | BR-078, BR-084 | BRD-5 DIS 3.28.0, FRBS 3.2.0 | FR-DS-081, FR-AC-060, FR-AC-061, FR-AC-062 ... |
| CORE-08.15 | Release BIR 2307 on premiums (PR2307) to insurer (p.8) | BR-078 | BRD-2 MKTID.013 (CSHID.027 removed from the Operations BRD) | FR-OP-113 |
| CORE-08.16 | Generate Direct Credit transactions file (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 |
| CORE-08.17 | Disbursement to government agencies and employees (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 |
| CORE-08.18 | Payee management (p.8) | BR-079 | BRD-5 DIS 2.2.0 | FR-DS-010 |
| CORE-08.19 | Check printing and series management (p.8) | BR-080 | BRD-5 DIS 2.7.0 | FR-DS-033 |
| CORE-08.20 | Status tagging and tracking (p.8) | BR-081, BR-082 | BRD-5 DIS 2.8.0, DIS 3.26.0 | FR-DS-050, FR-DS-052 |
| CORE-08.21 | Bank account operations (p.9) | - | BRD-5 DIS 2.7.0 | FR-DS-033 |
| CORE-08.22 | Generate manual service invoice (Other Income) (p.9) | BR-085 | BRD-1 BRNB.100.1 (BRNB.100 now holds the EOPT details of the placement) | FR-NB-117 |
| CORE-09.01 | Manual issuance of AR, OR, invoice, cash and cheque OTC payment (p.9) | BR-086 | BRD-2 CSHID.001, CSHID.002 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-012 |
| CORE-09.02 | Automated accounting entries (p.9) | BR-087 | BRD-2 CSHID.012, CSHID.014 | FR-OP-013, FR-OP-027 |
| CORE-09.03 | Batch payment files processing (automatching) (p.9) | BR-089 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 |
| CORE-09.04 | Batch processing and automation (automatch re-run) (p.9) | BR-092 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 |
| CORE-09.05 | Batch reversal processing (p.9) | - | BRD-2 CSHID.012, CSHID.016 | FR-OP-013, FR-OP-027, FR-OP-023 |
| CORE-09.06 | Unapplied payment management (excess payment) (p.9) | BR-090, BR-093 | BRD-2 CSHID.024, CSHID.025 | FR-OP-022 |
| CORE-09.07 | Payment auto matching (p.9) | BR-091 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 |
| CORE-09.08 | Generate acknowledgement receipt, official receipt, invoice (p.9) | BR-094, BR-095, BR-096 | BRD-2 CSHID.001, CSHID.006, CSHID.019 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 ... |
| CORE-09.09 | Premium payment monitoring (p.9) | BR-097 | BRD-2 CSHID.023, CSHID.017 | FR-OP-028, FR-OP-009 |
| CORE-09.10 | Commission fee collection (p.9) | BR-098, BR-099 | BRD-2 CSHID.007, CMRID.002 | FR-OP-021, FR-OP-091 |
| CORE-09.11 | PDC management (p.9) | BR-100 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 |
| CORE-09.12 | Traceability and auditability (p.9) | - | BRD-2 CSHID.011 | FR-OP-024 |
| CORE-10.01 | Extraction and remittance processing (scheduled or manual) (p.9) | BR-102, BR-103 | BRD-2 RMTID.001, RMTID.003, RMTID.004 | FR-OP-030 |
| CORE-10.02 | Sending and uploading of files (p.9) | BR-104, BR-105 | BRD-2 RMTID.011, RMTID.013 | FR-OP-034, FR-OP-037 |
| CORE-10.03 | Validation and filtering criteria (p.9) | BR-106, BR-107, BR-108 | BRD-2 RMTID.014, RMTID.017, RMTID.020 | FR-OP-031 |
| CORE-10.04 | Search and view (p.9) | BR-109 | BRD-2 RMTID.025, RMTID.026 | FR-OP-039, FR-OP-005 |
| CORE-10.05 | Hold remittance management (p.9) | BR-110, BR-112, BR-113, BR-114, BR-115, BR-116 | BRD-2 RMTID.020, RMTID.031, RMTID.032 | FR-OP-031, FR-OP-005 |
| CORE-10.06 | Special remittance request and processing (p.9) | BR-117 | BRD-2 RMTID.030 | FR-OP-040 |
| CORE-10.07 | Notifications and tracking (p.9) | BR-111 | BRD-2 RMTID.033, RMTID.035, RMTID.036 | FR-OP-040, FR-OP-031, FR-OP-036 |
| CORE-11.01 | Data extraction and file management (p.9) | BR-119, BR-120 | BRD-2 PRCID.001, PRCID.009 | FR-OP-070, FR-OP-074 |
| CORE-11.02 | Production register viewing and filtering (p.9) | BR-121 | BRD-2 PRCID.012, PRCID.021 | FR-OP-071, FR-OP-077 |
| CORE-11.03 | Matching and automation (p.9) | BR-122, BR-123 | BRD-2 PRCID.023, PRCID.024, PRCID.033 | FR-OP-076, FR-OP-075 |
| CORE-11.04 | Tracking and monitoring (p.9) | BR-124 | BRD-2 PRCID.029, PRCID.030, PRCID.032 | FR-OP-079, FR-OP-075, FR-OP-074 |
| CORE-11.05 | Reports generation (p.9) | - | BRD-2 PRCID.034, PRCID.035, PRCID.039 | FR-OP-071, FR-OP-080 |
| CORE-12.01 | Transaction management (endorsements, cancellations) (p.9) | BR-126 | BRD-2 ADJID.001, ADJID.003, ADJID.005 | FR-OP-050, FR-OP-053 |
| CORE-12.02 | Automated accounting entries (p.9) | BR-127, BR-132 | BRD-2 ADJID.011, ADJID.012 | FR-OP-056, FR-OP-057 |
| CORE-12.03 | Traceability and auditability (p.10) | BR-128, BR-129 | BRD-2 ADJID.020, ADJID.022 | FR-OP-050, FR-OP-061 |
| CORE-12.04 | Reporting and monitoring (p.10) | - | BRD-2 ADJID.016, ADJID.019, ADJID.021 | FR-OP-062, FR-OP-061 |
| CORE-12.05 | Search and document management (p.10) | BR-130, BR-131 | BRD-2 ADJID.024, MKTID.019 (was ADJID.025) | FR-OP-005, FR-OP-061, FR-OP-052 |
| CORE-12.06 | Sending and uploading of files (p.10) | - | BRD-2 ADJID.026 | FR-OP-059 |
| CORE-12.07 | Batch posting (p.10) | - | BRD-2 ADJID.006 | FR-OP-056 |
| CORE-13.01 | Automated incentive calculation (p.10) | BR-166, BR-167 | BRD-2 CMRID.005 | FR-OP-095 |
| CORE-13.02 | Motor Mania incentive plan (p.10) | BR-168 | BRD-2 CMRID.006 | FR-OP-095 |
| CORE-13.03 | Production data validation and exclusion handling (p.10) | BR-169 | BRD-2 CMRID.003 | FR-OP-095 |
| CORE-13.04 | Automated commission receivables processing (p.10) | BR-133 | BRD-2 CMRID.007 | FR-OP-091 |
| CORE-13.05 | Comprehensive production reporting (p.10) | BR-134 | BRD-2 CMRID.014, PRCID.035 | FR-OP-097, FR-OP-080 |
| CORE-13.06 | Risk mitigation and error handling (p.10) | BR-135 | BRD-2 CMRID.008 | FR-OP-091, FR-OP-093 |
| CORE-13.07 | Collection of commission receivables (direct payment) (p.10) | BR-052, BR-136, BR-137, BR-140 | BRD-2 CMRID.002, CMRID.004 | FR-OP-091, FR-OP-098 |
| CORE-13.08 | Auto-match reversals (p.10) | BR-138, BR-139 | BRD-2 CMRID.007, MKTID.013 (CSHID.027 removed) | FR-OP-091, FR-OP-113 |
| CORE-14.01 | Process claims advice from BDOI Marketing (p.10) | BR-141 | BRD-7 BRCLM.003, BRCLM.016 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-014 |
| CORE-14.02 | Process claims advice from client (p.10) | BR-141 | BRD-7 BRCLM.003, BRCLM.006 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-002 ... |
| CORE-14.03 | Process claims advice from insurer (p.10) | BR-141 | BRD-7 BRCLM.041, BRCLM.043 | FR-CM-003, FR-CM-011, FR-CM-022, FR-CM-024 ... |
| CORE-14.04 | Process LOA from insurer (p.10) | BR-142 | BRD-7 BRCLM.014, BRCLM.010 | FR-CM-043, FR-CM-040, FR-CM-046 |
| CORE-14.05 | Process settlement offer from insurer (p.10) | BR-143 | BRD-7 BRCLM.010, BRCLM.014 | FR-CM-040, FR-CM-046, FR-CM-043 |
| CORE-14.06 | Tag permanent closure (p.10) | BR-144 | BRD-7 BRCLM.035, BRCLM.005 | FR-CM-045, FR-CM-002 |
| CORE-14.07 | Tag temporary closure (p.10) | BR-145 | BRD-7 BRCLM.035 | FR-CM-045 |
| CORE-14.08 | Unclaimed checks safekeeping (p.10) | BR-146 | BRD-7 | CLR-CR-02 (CRQ03) |
| CORE-14.09 | Handover of settlement checks to Cashiering (p.10) | BR-147 | BRD-7 | CLR-CR-02 (CRQ03) |
| CORE-14.10 | Retrieval of checks from Cashiering for release (p.10) | BR-148 | BRD-7 | CLR-CR-02 (CRQ03) |
| CORE-14.11 | Maintain full claims history for audit and compliance (p.10) | BR-151 | BRD-7 BRCLM.004, BRCLM.022 | FR-CM-012, FR-CM-052, FR-CM-054 |
| CORE-14.12 | Reports and analytics viewing (p.10) | BR-149, BR-152 | BRD-7 BRCLM.026, BRCLM.029, BRCLM.030, BRCLM.031, BRCLM.032 | FR-CM-060, FR-CM-044, FR-CM-061, FR-CM-062 |
| CORE-14.13 | Encode / override the next follow-up date (p.10) | BR-150 | BRD-7 BRCLM.019 | FR-CM-002, FR-CM-050, FR-CM-054 |
| CORE-15.01 | Placement request initiation (p.10) | BR-153 | ReInsurance BRD FRID-001-FRID-010 | Phase 2 |
| CORE-15.02 | Placement slip management (p.10) | BR-154 | ReInsurance BRD FRID-017-FRID-028 | Phase 2 |
| CORE-15.03 | Statement of Account (SOA) generation (p.10) | BR-155, BR-156 | ReInsurance BRD FRID-033-FRID-041 | Phase 2 |
| CORE-15.04 | Claims reporting and settlement (p.10) | BR-157 | ReInsurance BRD FRID-101-FRID-105 | Phase 2 |
| CORE-15.05 | Claims payment processing (p.10) | BR-158 | ReInsurance BRD FRID-058-FRID-066 | Phase 2 |
| CORE-15.06 | Direct client claims payment (p.11) | BR-159 | ReInsurance BRD FRID-075-FRID-082 | Phase 2 |
| CORE-15.07 | Receive and validate file from stakeholders (capability matrix p.28) | - | ReInsurance BRD | Phase 2 |
| CORE-15.08 | Notification to stakeholders (capability matrix p.28) | - | ReInsurance BRD | Phase 2 |
| CORE-15.09 | Log, audit and history for traceability (capability matrix p.28) | - | ReInsurance BRD | Phase 2 |
| CORE-15.10 | Net settlement (capability matrix p.28) | - | ReInsurance BRD | Phase 2 |
| CORE-16.01 | Search, retrieve and display client contact and insurance account details (p.11) | BR-161 | BRD-9 BRCSF-002, BRCSF-003, BRCSF-008 | FR-CSF-011, FR-CSF-021, FR-CSF-022, FR-CSF-010 |
| CORE-16.02 | View, add and update client contact information (p.11) | BR-162 | BRD-9 BRCSF-004 | FR-CSF-020, FR-CSF-021 |
| CORE-16.03 | View mode of payment (history) and current status (p.11) | BR-163 | BRD-9 BRCSF-005 | FR-CSF-012, FR-CSF-013 |
| CORE-16.04 | View and resend RA (and e-policy) (p.11) | BR-164 | BRD-9 BRCSF-006, BRCSF-009 | FR-CSF-030, FR-CSF-033 |
| CORE-16.05 | Upload supporting documents (p.11) | - | BRD-9 BRCSF-007 | FR-CSF-032 |
| CORE-16.06 | Case resolution: add / edit case details and status (p.11) | BR-165 | BRD-9 BRCSFCM.001-012 (CSF Addendum - Case Management) | FR-CSF-050, FR-CSF-052, FR-CSF-056, FR-CSF-058, FR-CSF-060; CLR-CR-03 |
| CORE-17.01 | Master data change logging (user, product, insurer, LOVs) (p.11) | BR-170 | None (BRNB.016, BRNB.083, BRPM.024, BRCLXN.043 related) | FR-CR-031; FR-NB-016, FR-NB-132, FR-PM-005, FR-CL-003 |
| CORE-17.02 | Insurer management (p.11) | BR-171 | None (BRNB.008 related) | FR-CR-061; FR-NB-053 |
| CORE-17.03 | LOV maintenance (p.11) | BR-004, BR-172 | BRD-1 BRNB.083, BASAU 2.2.0 | FR-CR-060; FR-NB-132, FR-AC-070 |
| CORE-17.04 | MIS field definition (p.11) | BR-173, BR-174 | None (BRNB.108 related) | FR-CR-062; FR-NB-110, FR-NB-119 |
| CORE-17.05 | Product maintenance (p.11) | - | BRD-3 BRPM.003, PMADD01 | FR-CR-063; FR-PM-012, FR-PM-010 |
| CORE-18.01 | Include any other / additional system capabilities (p.11) | BR-176 | None | CLR-CR-09 (CRQ10) |
| CORE-19.01 | Automated renewal notifications (p.11) | BR-184, BR-196 | BRD-8 BRID-001 | FR-EB-022 |
| CORE-19.02 | Manual and system-based proposal generation (p.11) | BR-185, BR-197 | BRD-8 BRID-003 | FR-EB-024 |
| CORE-19.03 | Document and data upload management (p.11) | BR-186, BR-198 | BRD-8 BRID-005, BRID-005.01, BRID-014, BRID-025 | FR-EB-001, FR-EB-010, FR-EB-011, FR-EB-014 ... |
| CORE-19.04 | Broker on record management (p.11) | BR-187 | BRD-8 BRID-008 | FR-EB-031 |
| CORE-19.05 | Terms of Reference generation and distribution (p.11) | BR-188 | BRD-8 BRID-007, BRID-009 | FR-EB-004, FR-EB-030, FR-EB-035 |
| CORE-19.06 | Comparative report management (p.11) | BR-189 | BRD-8 BRID-010, BRID-011 | FR-EB-040, FR-EB-041, FR-EB-015, FR-EB-043 |
| CORE-19.07 | Client feedback, change, additional or amendment request capture and relay (p.11) | BR-190 | BRD-8 BRID-002, BRID-012, BRID-013, BRID-015 | FR-EB-023, FR-EB-044, FR-EB-054, FR-EB-055 ... |
| CORE-19.08 | Automated and manual sending of proposals (p.11) | BR-191 | BRD-8 BRID-003, BRID-009 | FR-EB-024, FR-EB-035 |
| CORE-19.09 | Approval workflow based on defined thresholds (p.11) | BR-192 | BRD-8 BRID-016 | FR-EB-042 |
| CORE-19.10 | Placement and booking management (p.11) | BR-193 | BRD-8 BRID-017, BRID-019, BRID-020 | FR-EB-046, FR-EB-051, FR-EB-052 |
| CORE-19.11 | Centralised reporting and analytics (p.11) | BR-194 | BRD-8 BRID-022, BRID-023, BRID-024 | FR-EB-060, FR-EB-062, FR-EB-001, FR-EB-003 |
| CORE-19.12 | Manage franchise requests and approvals (p.11) | BR-195 | BRD-8 BRID-026, BRID-027, BRID-029 | FR-EB-032, FR-EB-034, FR-EB-033 |
| CORE-20.01 | Automated request handling (p.12) | BR-199 | BRD-3 BRPM.011, BRPM.008 | FR-PM-020, FR-PM-024, FR-PM-045, FR-PM-021 |
| CORE-20.02 | Automatic reference numbers (p.12) | BR-200 | BRD-3 BRPM.008 | FR-PM-020, FR-PM-021 |
| CORE-20.03 | Quotation / proposal management (p.12) | BR-201 | BRD-3 BRPM.012, BRPM.013 | FR-PM-030, FR-PM-031, FR-PM-034, FR-PM-036 |
| CORE-20.04 | Automatic comparison tables (p.12) | BR-202 | BRD-3 BRPM.014, PMADD03 | FR-PM-034, FR-PM-035 |
| CORE-20.05 | Real-time dashboard / reports (p.12) | BR-203 | BRD-3 BRPM.019, BRPM.018 | FR-PM-070, FR-PM-071 |
| CORE-20.06 | Built-in checks / approvals (p.12) | BR-204 | BRD-3 BRPM.021, PMADD06 | FR-PM-021, FR-PM-073, FR-PM-043 |
| CORE-20.07 | Secure document sharing (p.12) | BR-205 | BRD-3 BRPM.020, BRPM.002 | FR-PM-004, FR-PM-002 |
| CORE-20.08 | Works with existing systems (p.12) | BR-206 | BRD-3 BRPM.022 | FR-PM-072 |
| CORE-20.09 | Track all changes (p.12) | BR-207 | BRD-3 BRPM.024 | FR-PM-005 |
| CORE-20.10 | Expiring packages monitoring (p.12) | BR-208 | BRD-3 BRPM.017, BRPM.006 | FR-PM-044, FR-PM-060, FR-PM-061, FR-PM-071 ... |
| CORE-21.01 | Generate standard reports for operational and analytical purposes (p.12) | BR-054 | BRD-1 BRNB.057, BRNB.075 | FR-CR-040; FR-NB-123, FR-NB-122 |
| CORE-21.02 | Extract, download and print reports in multiple formats (p.12) | BR-078 | BRD-1 BRNB.031, BRNB.037 | FR-CR-040; FR-NB-124, FR-NB-125 |
| CORE-21.03 | Create tailored reports by parameters, filters and business requirements (p.12) | BR-053 | BRD-1 BRNB.057 | FR-CR-041; FR-NB-123 |
| CORE-21.04 | Dynamic customisation: data fields, charts and summaries (p.12) | BR-053 | None | FR-CR-042 |
| CORE-21.05 | Scheduled or on-demand report generation (p.12) | BR-054, BR-103 | None (DIS 3.28.0, BRCLXN.028 related) | FR-CR-043; FR-DS-081, FR-CL-082 |
| XC-01 | Log-in, password reset, several types of user access, user administration (p.13) | BR-000, BR-001, BR-002, BR-003 | BRD-11 BRNB.040, BRNB.084 | FR-CR-070; FR-NB-130, FR-NB-134 |
| XC-02 | Centralised, real-time dashboards for all roles; details invoked from the dashboard (p.5, p.20-21) | BR-125, BR-152, BR-160, BR-203 | None (BRNB.012, BRQID.003, BRPM.019 related) | FR-CR-010, FR-CR-011; FR-NB-120, FR-OP-003, FR-PM-070 |
| XC-03 | Automated notifications, approvals and feedback tracking (p.5, p.19, p.24) | BR-083, BR-111, BR-206 | None (BRNB.015, RMTID.033 related) | FR-CR-020; FR-NB-015, FR-OP-040 |
| XC-04 | Audit logs and full transaction history (p.4-5, p.20, p.25) | BR-124, BR-129, BR-151, BR-207 | None (BRNB.016, BRNB.086, BRNB.089 related) | FR-CR-030; FR-NB-016, FR-NB-136 |
| XC-05 | Workflow maintained at the back end; data flows to the next process by rules (p.15, p.18, p.23) | BR-051, BR-101, BR-177, BR-182 | None (BRNB.096, BRNB.022 related) | FR-CR-080; FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 |
| XC-06 | Batch and individual processing; concurrent users and high-volume bulk uploads (p.4, p.16) | BR-063 | None (BRNB.064, BRQID.006 related) | FR-CR-083; FR-NB-019, FR-NB-065, FR-OP-008 |
| XC-07 | Currency selection and multi-currency support (p.15, p.18) | BR-027, BR-088 | None (BRCLM.009 related) | FR-CR-091; FR-CM-011 |
| XC-08 | 30-day hold cover request, assigned to a role (p.14) | BR-034 | BRD-1 BRNB.072, BRNB.103 | FR-NB-082, FR-NB-083 |
| XC-09 | Tag direct payment (DP) accounts (p.14) | BR-035 | BRD-1 BRNB.114 | FR-NB-069, FR-NB-092 |
| XC-10 | SOA generated with placement report and Insurance Advice; service invoice to insurer issued with the SOA at booking (p.14-15) | BR-039, BR-043 | BRD-1 BRNB.100.1, BRCLXN.058 | FR-CR-093; FR-NB-117, FR-CL-060, FR-CL-061 |
| XC-11 | Billing reports with premium and loan details; payment reports matched to accounts (p.15) | BR-049, BR-050 | BRD-1 BRNB.067, BRNB.068 | FR-NB-090, FR-NB-091, FR-NB-092 |
| XC-12 | BIR standard books (sales, purchase, cash receipts, cash disbursements, general journal) (p.17) | BR-084 | BRD-5 FRBS 3.2.0 | FR-CR-040; FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 |
| XC-13 | Invoice Master List across workflows (p.23) | BR-175 | None (BRCLXN.001, ACSL 2.16.0 related) | FR-CR-090; FR-CL-010, FR-AS-026 |
| XC-14 | BDO brand colours, logos, icons and design system (p.23) | BR-178, BR-179, BR-180 | None | FR-CR-092 |
| XC-15 | Identified user journeys and customisable interaction flows (with a walkthrough) (p.23) | BR-181, BR-182 | Walkthroughs of FRS BRD-1, 2, 3, 4, 5 and 11; UX Screen Decks; SIT walkthrough sessions (Storyboard index of this FRS) | FR-CR-001, FR-CR-080, FR-CR-092 |
| XC-16 | Vendor uses Figma for design execution (p.23) | BR-183 | UX documents 07 to 09 of each business sign-off pack (CLR-CR-17) | FR-CR-092 |
| XC-17 | Regulatory compliance: BIR and Insurance Commission reportorial requirements (p.4) | - | BRD-5 FRBS 3.2.0 | FR-CR-040; FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 |
| XC-18 | Retail and wholesale business (p.4) | - | BRD-1 BRNB.001 | FR-CR-001; FR-NB-001 |
| XC-19 | Invoice batch printing, delivery to insurers by file transfer and delivery report (daily, per insurer) (p.43) | - | None (BRNB.100.1 related) | FR-CR-081; FR-NB-117 |
| XC-20 | Client migration (one-time, 2020 to present) and daily midday / EOD client batches with modification report (p.43) | - | Data Migration BRD V0.03; Data Migration Handbook (BRD-13) | FR-CR-082 |
| XC-21 | ALeA e-mail address encoding (as needed) (p.43) | - | None | CLR-CR-10 (CRQ20) |
| XC-22 | Daily synchronisation from source systems (CMS, Reinsurance) (p.42-43) | - | BRD-4 BRCLXN.013, BRCLXN.014, BRCLXN.015 | FR-CR-082; FR-CL-017 |
| XC-23 | MIS LOV, QPS insurer, LGT rates and insurer branch maintenance (MILB, 24 a year) (p.45) | - | BRD-1 BRNB.083 | FR-CR-060; FR-NB-132 |

<!-- portrait -->

# Sign-off

By signing, BDOI confirms that this FRS describes the cross-cutting functions it expects in BIBS and the map of the umbrella capabilities to the function FRS, and accepts the assumptions in section 9.1. Open questions in section 9.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

# Appendix: User-story view

The umbrella BRD lists its requirements as capability bullets and cross-cutting rows with generic personas (User, System, Administrator, Authorized User); its acceptance criteria are left to the function BRDs. This appendix restates each requirement (BR-000 to BR-208) as one user story with the business persona of section 3.1 who needs it, the FRs that meet it - the cross-cutting FR-CR of this FRS and the FRs of the function FRS - their acceptance criteria and the test conditions of the function test plans with their number of cases. The capability and the persona come from the traceability chapter; where a BR ID appears in several rows, the story joins their capabilities. Acceptance criteria are numbered in the order of each FR in its own FRS. The table has 209 rows.

<!-- table: widths=2.4,7.5,2.3,2.8,3.4 caption="User-story view: BRD requirement, user story, FRs, acceptance criteria and test cases" size=8 -->
| BRD ID | User story | FR IDs | Acceptance criteria | Test cases |
|---|---|---|---|---|
| BR-000 | As a System Administrator, I need log-in, password reset, several types of user access, user administration, so that only authorised users reach BIBS. | FR-CR-070, FR-NB-130, FR-NB-134 | FR-CR-070 AC1; FR-NB-130 AC1-3; FR-NB-134 AC1-2 | TC-NB-130.1 to 130.3, 134.1 to 134.2 (8 cases) |
| BR-001 | As a System Administrator, I need log-in, password reset, several types of user access, user administration, so that only authorised users reach BIBS. | FR-CR-070, FR-NB-130, FR-NB-134 | FR-CR-070 AC1; FR-NB-130 AC1-3; FR-NB-134 AC1-2 | TC-NB-130.1 to 130.3, 134.1 to 134.2 (8 cases) |
| BR-002 | As a System Administrator, I need log-in, password reset, several types of user access, user administration, so that only authorised users reach BIBS. | FR-CR-070, FR-NB-130, FR-NB-134 | FR-CR-070 AC1; FR-NB-130 AC1-3; FR-NB-134 AC1-2 | TC-NB-130.1 to 130.3, 134.1 to 134.2 (8 cases) |
| BR-003 | As a System Administrator, I need log-in, password reset, several types of user access, user administration, so that only authorised users reach BIBS. | FR-CR-070, FR-NB-130, FR-NB-134 | FR-CR-070 AC1; FR-NB-130 AC1-3; FR-NB-134 AC1-2 | TC-NB-130.1 to 130.3, 134.1 to 134.2 (8 cases) |
| BR-004 | As a Business Administrator, I need LOV maintenance, so that master data is consistent and controlled across BIBS. | FR-CR-060, FR-NB-132, FR-AC-070 | FR-CR-060 AC1; FR-NB-132 AC1-2; FR-AC-070 AC1-2 | TC-NB-132.1 to 132.2, TC-AC-070.1 to 070.4 (10 cases) |
| BR-005 | As a Marketing AO, I need to create prospect / record, so that every client is onboarded once, verified and found again quickly. | FR-NB-034, FR-NB-031 | FR-NB-034 AC1-4; FR-NB-031 AC1-3 | TC-NB-034.1 to 034.4, 031.1 to 031.4 (15 cases) |
| BR-006 | As a Marketing AO, I need to update prospect / record, so that every client is onboarded once, verified and found again quickly. | FR-NB-032, FR-NB-035 | FR-NB-032 AC1-2; FR-NB-035 AC1-2 | TC-NB-032.1 to 032.2, 035.1 to 035.2 (6 cases) |
| BR-007 | As a Marketing AO, I need to convert prospect into client record, so that every client is onboarded once, verified and found again quickly. | FR-NB-034 | FR-NB-034 AC1-4 | TC-NB-034.1 to 034.4 (7 cases) |
| BR-008 | As a Marketing AO, I need to delete prospect / record, so that every client is onboarded once, verified and found again quickly. | FR-NB-012, FR-NB-137 | FR-NB-012 AC1-3; FR-NB-137 AC1-2 | TC-NB-012.1 to 012.3, 137.1 to 137.2 (7 cases) |
| BR-009 | As a Marketing AO, I need to upload and validate client documents, so that every client is onboarded once, verified and found again quickly. | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | FR-NB-031 AC1-3; FR-NB-032 AC1-2; FR-NB-033 AC1-2; FR-NB-034 AC1-4 | TC-NB-031.1 to 031.4, 032.1 to 032.2, 033.1 to 033.2, 034.1 to 034.4 (21 cases) |
| BR-010 | As a Marketing AO, I need to upload and validate client documents, so that every client is onboarded once, verified and found again quickly. | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | FR-NB-031 AC1-3; FR-NB-032 AC1-2; FR-NB-033 AC1-2; FR-NB-034 AC1-4 | TC-NB-031.1 to 031.4, 032.1 to 032.2, 033.1 to 033.2, 034.1 to 034.4 (21 cases) |
| BR-011 | As a Marketing AO, I need to upload and validate client documents, so that every client is onboarded once, verified and found again quickly. | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | FR-NB-031 AC1-3; FR-NB-032 AC1-2; FR-NB-033 AC1-2; FR-NB-034 AC1-4 | TC-NB-031.1 to 031.4, 032.1 to 032.2, 033.1 to 033.2, 034.1 to 034.4 (21 cases) |
| BR-012 | As a Marketing AO, I need client search, so that every client is onboarded once, verified and found again quickly. | FR-NB-030 | FR-NB-030 AC1-2 | TC-NB-030.1 to 030.3 (4 cases) |
| BR-013 | As a Marketing AO, I need to generate client code, so that every client is onboarded once, verified and found again quickly. | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | FR-NB-031 AC1-3; FR-NB-032 AC1-2; FR-NB-033 AC1-2; FR-NB-034 AC1-4 | TC-NB-031.1 to 031.4, 032.1 to 032.2, 033.1 to 033.2, 034.1 to 034.4 (21 cases) |
| BR-014 | As a Marketing AO, I need to receive request for quotation / proposal, so that clients receive approved quotations and proposals on time. | FR-NB-040 | FR-NB-040 AC1-3 | TC-NB-040.1 to 040.3 (5 cases) |
| BR-015 | As a Marketing AO, I need to create quotation / proposal (individual or bulk), so that clients receive approved quotations and proposals on time. | FR-NB-041, FR-NB-044, FR-NB-046 | FR-NB-041 AC1-3; FR-NB-044 AC1-5; FR-NB-046 AC1-3 | TC-NB-041.1 to 041.3, 044.1 to 044.5, 046.1 to 046.3 (20 cases) |
| BR-016 | As a Marketing AO, I need to create quotation / proposal (individual or bulk), so that clients receive approved quotations and proposals on time. | FR-NB-041, FR-NB-044, FR-NB-046 | FR-NB-041 AC1-3; FR-NB-044 AC1-5; FR-NB-046 AC1-3 | TC-NB-041.1 to 041.3, 044.1 to 044.5, 046.1 to 046.3 (20 cases) |
| BR-017 | As a Marketing AO, I need to track proposal status, so that clients receive approved quotations and proposals on time. | FR-NB-010, FR-NB-122, FR-NB-120 | FR-NB-010 AC1-3; FR-NB-122 AC1-3; FR-NB-120 AC1-2 | TC-NB-010.1 to 010.3, 122.1 to 122.3, 120.1 to 120.3 (12 cases) |
| BR-018 | As a Marketing AO, I need to edit quotation / proposal, so that clients receive approved quotations and proposals on time. | FR-NB-042 | FR-NB-042 AC1-2 | TC-NB-042.1 to 042.3 (4 cases) |
| BR-019 | As a Marketing AO, I need to approve quotation / proposal, so that clients receive approved quotations and proposals on time. | FR-NB-043, FR-NB-014, FR-NB-051 | FR-NB-043 AC1-3; FR-NB-014 AC1-3; FR-NB-051 AC1-2 | TC-NB-043.1 to 043.3, 014.1 to 014.3, 051.1 to 051.2 (11 cases) |
| BR-020 | As a Marketing AO, I need to print quotation / proposal, so that clients receive approved quotations and proposals on time. | FR-NB-041, FR-NB-044 | FR-NB-041 AC1-3; FR-NB-044 AC1-5 | TC-NB-041.1 to 041.3, 044.1 to 044.5 (15 cases) |
| BR-021 | As a Marketing AO, I need to upload documents, so that clients receive approved quotations and proposals on time. | FR-NB-017 | FR-NB-017 AC1-4 | TC-NB-017.1 to 017.4 (9 cases) |
| BR-022 | As a Marketing AO, I need to send quotation / proposal, so that clients receive approved quotations and proposals on time. | FR-NB-041, FR-NB-044, FR-NB-046 | FR-NB-041 AC1-3; FR-NB-044 AC1-5; FR-NB-046 AC1-3 | TC-NB-041.1 to 041.3, 044.1 to 044.5, 046.1 to 046.3 (20 cases) |
| BR-023 | As a Marketing AO, I need to create account (manual, bulk upload, system), so that every policy is held as an account linked to its client and ready for placement. | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 | FR-NB-061 AC1-3; FR-NB-062 AC1-3; FR-NB-063 AC1-3; FR-NB-065 AC1-3 | TC-NB-061.1 to 061.3, 062.1 to 062.3, 063.1 to 063.2, 065.1 to 065.2 (21 cases) |
| BR-024 | As a Marketing AO, I need to create account (manual, bulk upload, system), so that every policy is held as an account linked to its client and ready for placement. | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 | FR-NB-061 AC1-3; FR-NB-062 AC1-3; FR-NB-063 AC1-3; FR-NB-065 AC1-3 | TC-NB-061.1 to 061.3, 062.1 to 062.3, 063.1 to 063.2, 065.1 to 065.2 (21 cases) |
| BR-025 | As a Marketing AO, I need to create account (manual, bulk upload, system), so that every policy is held as an account linked to its client and ready for placement. | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 | FR-NB-061 AC1-3; FR-NB-062 AC1-3; FR-NB-063 AC1-3; FR-NB-065 AC1-3 | TC-NB-061.1 to 061.3, 062.1 to 062.3, 063.1 to 063.2, 065.1 to 065.2 (21 cases) |
| BR-026 | As a Marketing AO, I need rules-based sanitation of accounts (bulk / individual), so that expiring policies are renewed on time. | FR-RN-020, FR-RN-103, FR-RN-004, FR-RN-022 | FR-RN-020 AC1-3; FR-RN-103 AC1-2; FR-RN-004 AC1-3; FR-RN-022 AC1-3 | TC-RN-020.1 to 020.4, 103.1 to 103.3, 004.1 to 004.4, 022.1 to 022.5 (25 cases) |
| BR-027 | As an FRBS user, I need currency selection and multi-currency support, so that foreign currency business is recorded correctly. | FR-CR-091, FR-CM-011 | FR-CR-091 AC1; FR-CM-011 AC1-4 | TC-CM-011.1 to 011.5 (8 cases) |
| BR-028 | As a Marketing AO, I need to create account (manual, bulk upload, system), so that every policy is held as an account linked to its client and ready for placement. | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 | FR-NB-061 AC1-3; FR-NB-062 AC1-3; FR-NB-063 AC1-3; FR-NB-065 AC1-3 | TC-NB-061.1 to 061.3, 062.1 to 062.3, 063.1 to 063.2, 065.1 to 065.2 (21 cases) |
| BR-029 | As a Marketing AO, I need to update account, so that every policy is held as an account linked to its client and ready for placement. | FR-NB-064, FR-NB-065 | FR-NB-064 AC1-3; FR-NB-065 AC1-3 | TC-NB-064.1 to 064.3, 065.1 to 065.2 (10 cases) |
| BR-030 | As a Marketing AO, I need to link to client record, so that every policy is held as an account linked to its client and ready for placement. | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-037 | FR-NB-061 AC1-3; FR-NB-062 AC1-3; FR-NB-063 AC1-3; FR-NB-037 AC1-2 | TC-NB-061.1 to 061.3, 062.1 to 062.3, 063.1 to 063.2, 037.1 to 037.2 (18 cases) |
| BR-031 | As a Marketing AO, I need to approve account, so that every policy is held as an account linked to its client and ready for placement. | FR-NB-010, FR-NB-122, FR-NB-133 | FR-NB-010 AC1-3; FR-NB-122 AC1-3; FR-NB-133 AC1-2 | TC-NB-010.1 to 010.3, 122.1 to 122.3, 133.1 to 133.2 (12 cases) |
| BR-032 | As a Processing Officer, I need to submit for placement and booking, so that accounts are placed with the insurers and booked without re-keying. | FR-NB-010, FR-NB-122, FR-NB-011, FR-NB-064 | FR-NB-010 AC1-3; FR-NB-122 AC1-3; FR-NB-011 AC1-3; FR-NB-064 AC1-3 | TC-NB-010.1 to 010.3, 122.1 to 122.3, 011.1 to 011.3, 064.1 to 064.3 (19 cases) |
| BR-033 | As a Marketing AO, I need to initiate placement request, so that every policy is held as an account linked to its client and ready for placement. | FR-NB-080 | FR-NB-080 AC1-3 | TC-NB-080.1 to 080.3 (5 cases) |
| BR-034 | As a Processing Officer, I need 30-day hold cover request, assigned to a role, so that risks are covered while the policy is placed. | FR-NB-082, FR-NB-083 | FR-NB-082 AC1-2; FR-NB-083 AC1-2 | TC-NB-082.1 to 082.2, 083.1 to 083.3 (8 cases) |
| BR-035 | As a Marketing AO, I need to tag direct payment (DP) accounts, so that direct-payment accounts are billed and followed correctly. | FR-NB-069, FR-NB-092 | FR-NB-069 AC1-3; FR-NB-092 AC1-3 | TC-NB-069.1 to 069.2, 092.1 to 092.3 (8 cases) |
| BR-036 | As a Processing Officer, I need to generate placement slip, so that accounts are placed with the insurers and booked without re-keying. | FR-NB-080 | FR-NB-080 AC1-3 | TC-NB-080.1 to 080.3 (5 cases) |
| BR-037 | As a Processing Officer, I need to send placement slip to insurer, so that accounts are placed with the insurers and booked without re-keying. | FR-NB-081 | FR-NB-081 AC1-2 | TC-NB-081.1 to 081.2 (3 cases) |
| BR-038 | As a Marketing AO, I need client confirmation, so that every policy is held as an account linked to its client and ready for placement. | FR-NB-045, FR-NB-057 | FR-NB-045 AC1-3; FR-NB-057 AC1-2 | TC-NB-045.1 to 045.3, 057.1 to 057.2 (9 cases) |
| BR-039 | As a Processing Officer, I need to generate placement report; to generate Insurance Advice; SOA generated with placement report and Insurance Advice; service invoice to insurer issued with the SOA at booking, so that accounts are placed with the insurers and booked without re-keying. | FR-NB-121, FR-NB-122, FR-NB-103, FR-NB-104, FR-CR-093, FR-NB-117, FR-CL-060, FR-CL-061 | FR-NB-121 AC1-2; FR-NB-122 AC1-3; FR-NB-103 AC1-2; FR-NB-104 AC1-3; FR-CR-093 AC1; FR-NB-117 AC1-3; FR-CL-060 AC1-3; FR-CL-061 AC1-2 | TC-NB-121.1 to 121.2, 122.1 to 122.3, 103.1 to 103.2, 104.1 to 104.3, 117.1 to 117.4, TC-CL-060.1 to 060.3, 061.1 to 061.3 (30 cases) |
| BR-040 | As a Processing Officer, I need to receive e-policy (individual or batch), so that accounts are placed with the insurers and booked without re-keying. | FR-NB-100 | FR-NB-100 AC1-2 | TC-NB-100.1 to 100.3 (4 cases) |
| BR-041 | As a Processing Officer, I need to send e-policy to client (individual or batch), so that accounts are placed with the insurers and booked without re-keying. | FR-NB-105 | FR-NB-105 AC1-2 | TC-NB-105.1 to 105.2 (3 cases) |
| BR-042 | As a Processing Officer, I need to update client record with the e-policy and policy number, so that accounts are placed with the insurers and booked without re-keying. | FR-NB-101 | FR-NB-101 AC1-2 | TC-NB-101.1 to 101.2 (4 cases) |
| BR-043 | As a Processing Officer, I need SOA generated with placement report and Insurance Advice; service invoice to insurer issued with the SOA at booking, so that the insurer and the client receive the right statements at booking. | FR-CR-093, FR-NB-117, FR-CL-060, FR-CL-061 | FR-CR-093 AC1; FR-NB-117 AC1-3; FR-CL-060 AC1-3; FR-CL-061 AC1-2 | TC-NB-117.1 to 117.4, TC-CL-060.1 to 060.3, 061.1 to 061.3 (15 cases) |
| BR-044 | As a Collection user, I need premium Receivable (PR) management, so that premium receivables are collected and followed up. | FR-CL-010, FR-CL-015, FR-CL-016 | FR-CL-010 AC1-3; FR-CL-015 AC1-2; FR-CL-016 AC1-2 | TC-CL-010.1 to 010.4, 015.1 to 015.3, 016.1 to 016.2 (12 cases) |
| BR-045 | As a Collection user, I need premium Receivable (PR) management, so that premium receivables are collected and followed up. | FR-CL-010, FR-CL-015, FR-CL-016 | FR-CL-010 AC1-3; FR-CL-015 AC1-2; FR-CL-016 AC1-2 | TC-CL-010.1 to 010.4, 015.1 to 015.3, 016.1 to 016.2 (12 cases) |
| BR-046 | As a Collection user, I need unapplied payment disposition (excess payment), so that premium receivables are collected and followed up. | FR-CL-074, FR-CL-070, FR-CL-077 | FR-CL-074 AC1-3; FR-CL-070 AC1-3; FR-CL-077 AC1-2 | TC-CL-074.1 to 074.4, 070.1 to 070.3, 077.1 to 077.3 (13 cases) |
| BR-047 | As a Collection user, I need unapplied payment disposition (excess payment), so that premium receivables are collected and followed up. | FR-CL-074, FR-CL-070, FR-CL-077 | FR-CL-074 AC1-3; FR-CL-070 AC1-3; FR-CL-077 AC1-2 | TC-CL-074.1 to 074.4, 070.1 to 070.3, 077.1 to 077.3 (13 cases) |
| BR-048 | As a Collection user, I need disposition tracking and management, so that premium receivables are collected and followed up. | FR-CL-030, FR-CL-031, FR-CL-018 | FR-CL-030 AC1-3; FR-CL-031 AC1-3; FR-CL-018 AC1-2 | TC-CL-030.1 to 030.4, 031.1 to 031.4, 018.1 to 018.3 (17 cases) |
| BR-049 | As a Collection user, I need batch processing and automation; billing reports with premium and loan details; payment reports matched to accounts, so that premium receivables are collected and followed up. | FR-CL-017, FR-CL-032, FR-CL-080, FR-CL-077, FR-NB-090, FR-NB-091, FR-NB-092 | FR-CL-017 AC1-3; FR-CL-032 AC1-3; FR-CL-080 AC1-2; FR-CL-077 AC1-2; FR-NB-090 AC1-2; FR-NB-091 AC1-3; FR-NB-092 AC1-3 | TC-CL-017.1 to 017.4, 032.1 to 032.5, 080.1 to 080.3, 077.1 to 077.3, TC-NB-090.1 to 090.2, 091.1 to 091.3, 092.1 to 092.3 (37 cases) |
| BR-050 | As a Processing Officer, I need billing reports with premium and loan details; payment reports matched to accounts, so that bank-financed accounts are billed and payments matched. | FR-NB-090, FR-NB-091, FR-NB-092 | FR-NB-090 AC1-2; FR-NB-091 AC1-3; FR-NB-092 AC1-3 | TC-NB-090.1 to 090.2, 091.1 to 091.3, 092.1 to 092.3 (14 cases) |
| BR-051 | As a Business Administrator, I need workflow maintained at the back end; data flows to the next process by rules, so that work moves to the next process by rules. | FR-CR-080, FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 | FR-CR-080 AC1; FR-NB-011 AC1-3; FR-NB-064 AC1-3; FR-NB-010 AC1-3; FR-NB-122 AC1-3 | TC-NB-011.1 to 011.3, 064.1 to 064.3, 010.1 to 010.3, 122.1 to 122.3 (19 cases) |
| BR-052 | As a Commission user, I need collection of commission receivables (direct payment), so that commissions on direct payments are collected. | FR-OP-091, FR-OP-098 | FR-OP-091 AC1-3; FR-OP-098 AC1 | TC-OP-091.1 to 091.2, 098.1 to 098.2 (6 cases) |
| BR-053 | As a Marketing AO, I need to generate and customise report; reporting and audit; to create tailored reports by parameters, filters and business requirements; dynamic customisation: data fields, charts and summaries, so that clients receive approved quotations and proposals on time. | FR-NB-123, FR-NB-122, FR-CL-082, FR-CL-003, FR-CL-083, FR-CR-041, FR-CR-042 | FR-NB-123 AC1-2; FR-NB-122 AC1-3; FR-CL-082 AC1; FR-CL-003 AC1-3; FR-CL-083 AC1; FR-CR-041 AC1; FR-CR-042 AC1-2 | TC-NB-123.1 to 123.2, 122.1 to 122.3, TC-CL-082.1 to 082.2, 003.1 to 003.4, 083.1 to 083.2 (15 cases) |
| BR-054 | As a Collection user, I need reporting and audit; to generate standard reports for operational and analytical purposes; scheduled or on-demand report generation, so that premium receivables are collected and followed up. | FR-CL-082, FR-CL-003, FR-CL-083, FR-CR-040, FR-NB-123, FR-NB-122, FR-CR-043, FR-DS-081 | FR-CL-082 AC1; FR-CL-003 AC1-3; FR-CL-083 AC1; FR-CR-040 AC1-2; FR-NB-123 AC1-2; FR-NB-122 AC1-3; FR-CR-043 AC1-2; FR-DS-081 AC1 | TC-CL-082.1 to 082.2, 003.1 to 003.4, 083.1 to 083.2, TC-NB-123.1 to 123.2, 122.1 to 122.3, TC-AC-081.1 to 081.2 (17 cases) |
| BR-055 | As a Marketing AO, I need to generate RMEL (list of expiring accounts), so that expiring policies are renewed on time. | FR-RN-011, FR-RN-010, FR-RN-112 | FR-RN-011 AC1-3; FR-RN-010 AC1-3; FR-RN-112 AC1-2 | TC-RN-011.1 to 011.4, 010.1 to 010.6, 112.1 to 112.3 (18 cases) |
| BR-056 | As a Marketing AO, I need filter and distribute RMEL, so that expiring policies are renewed on time. | FR-RN-012, FR-RN-040, FR-RN-102 | FR-RN-012 AC1-3; FR-RN-040 AC1-4; FR-RN-102 AC1-2 | TC-RN-012.1 to 012.3, 040.1 to 040.5, 102.1 to 102.3 (16 cases) |
| BR-057 | As a Marketing AO, I need rules-based sanitation of accounts (bulk / individual), so that expiring policies are renewed on time. | FR-RN-020, FR-RN-103, FR-RN-004, FR-RN-022 | FR-RN-020 AC1-3; FR-RN-103 AC1-2; FR-RN-004 AC1-3; FR-RN-022 AC1-3 | TC-RN-020.1 to 020.4, 103.1 to 103.3, 004.1 to 004.4, 022.1 to 022.5 (25 cases) |
| BR-058 | As a Marketing AO, I need provide disposition (online or by upload), so that expiring policies are renewed on time. | FR-RN-004, FR-RN-023, FR-RN-051, FR-RN-103 | FR-RN-004 AC1-3; FR-RN-023 AC1-3; FR-RN-051 AC1-3; FR-RN-103 AC1-2 | TC-RN-004.1 to 004.4, 023.1 to 023.6, 051.1 to 051.5, 103.1 to 103.3 (29 cases) |
| BR-059 | As a Marketing AO, I need provide disposition (online or by upload), so that expiring policies are renewed on time. | FR-RN-004, FR-RN-023, FR-RN-051, FR-RN-103 | FR-RN-004 AC1-3; FR-RN-023 AC1-3; FR-RN-051 AC1-3; FR-RN-103 AC1-2 | TC-RN-004.1 to 004.4, 023.1 to 023.6, 051.1 to 051.5, 103.1 to 103.3 (29 cases) |
| BR-060 | As a Marketing AO, I need to generate and send Renewal Advice; to send No Advice Letter (NAL) and Non-Renewal Letter (NRL), so that expiring policies are renewed on time. | FR-RN-080, FR-RN-081, FR-RN-090, FR-RN-082, FR-RN-103, FR-RN-024, FR-RN-112 | FR-RN-080 AC1-4; FR-RN-081 AC1-2; FR-RN-090 AC1-3; FR-RN-082 AC1-2; FR-RN-103 AC1-2; FR-RN-024 AC1-3; FR-RN-112 AC1-2 | TC-RN-080.1 to 080.5, 081.1 to 081.4, 090.1 to 090.5, 082.1 to 082.2, 103.1 to 103.3, 024.1 to 024.4, 112.1 to 112.3 (47 cases) |
| BR-061 | As a Marketing AO, I need to generate and send Renewal Advice; to send No Advice Letter (NAL) and Non-Renewal Letter (NRL), so that expiring policies are renewed on time. | FR-RN-080, FR-RN-081, FR-RN-090, FR-RN-082, FR-RN-103, FR-RN-024, FR-RN-112 | FR-RN-080 AC1-4; FR-RN-081 AC1-2; FR-RN-090 AC1-3; FR-RN-082 AC1-2; FR-RN-103 AC1-2; FR-RN-024 AC1-3; FR-RN-112 AC1-2 | TC-RN-080.1 to 080.5, 081.1 to 081.4, 090.1 to 090.5, 082.1 to 082.2, 103.1 to 103.3, 024.1 to 024.4, 112.1 to 112.3 (47 cases) |
| BR-062 | As a Marketing AO, I need check renewal payment, so that expiring policies are renewed on time. | FR-RN-042 | FR-RN-042 AC1-3 | TC-RN-042.1 to 042.3 (4 cases) |
| BR-063 | As a Processing Officer, I need batch and individual processing; concurrent users and high-volume bulk uploads, so that high volumes are processed in batches as well as one by one. | FR-CR-083, FR-NB-019, FR-NB-065, FR-OP-008 | FR-CR-083 AC1; FR-NB-019 AC1-3; FR-NB-065 AC1-3; FR-OP-008 AC1-3 | TC-NB-019.1 to 019.4, 065.1 to 065.2, TC-OP-008.1 to 008.3 (16 cases) |
| BR-064 | As a Marketing AO, I need to track renewal status (BR table only), so that expiring policies are renewed on time. | FR-RN-102 | FR-RN-102 AC1-2 | TC-RN-102.1 to 102.3 (3 cases) |
| BR-065 | As an FRBS user, I need daily financial report reconciliation, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AS-003, FR-AS-004 | FR-AS-003 AC1-2; FR-AS-004 AC1 | TC-AC-003.1 to 003.3, 004.1 to 004.2 (5 cases) |
| BR-066 | As an FRBS user, I need to generate financial reports, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | FR-AC-060 AC1-2; FR-AC-061 AC1-2; FR-AC-062 AC1-2; FR-AC-063 AC1-2 | TC-AC-060.1 to 060.2, 061.1 to 061.3, 062.1 to 062.2, 063.1 to 063.3 (22 cases) |
| BR-067 | As an FRBS user, I need daily cash movement reconciliation, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AC-050 | FR-AC-050 AC1-2 | TC-AC-050.1 to 050.4 (6 cases) |
| BR-068 | As an FRBS user, I need insurer's statement of accounts (SOA) reconciliation, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AS-003 | FR-AS-003 AC1-2 | TC-AC-003.1 to 003.3 (3 cases) |
| BR-069 | As an FRBS user, I need to generate automated journal entries, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AS-024, FR-AC-030 | FR-AS-024 AC1; FR-AC-030 AC1-2 | TC-AC-024.1 to 024.2, 030.1 to 030.3 (8 cases) |
| BR-070 | As an FRBS user, I need to perform manual entries, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AC-032 | FR-AC-032 AC1-2 | TC-AC-032.1 to 032.4 (5 cases) |
| BR-071 | As an FRBS user, I need to perform manual / invoice adjustments, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AS-021 | FR-AS-021 AC1 | TC-AC-021.1 to 021.4 (11 cases) |
| BR-072 | As an FRBS user, I need to perform accrual, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AC-032, FR-AC-033 | FR-AC-032 AC1-2; FR-AC-033 AC1 | TC-AC-032.1 to 032.4, 033.1 to 033.2 (8 cases) |
| BR-073 | As an FRBS user, I need to perform revaluation, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AC-010, FR-AC-043 | FR-AC-010 AC1-2; FR-AC-043 AC1 | TC-AC-010.1 to 010.4, 043.1 to 043.2 (9 cases) |
| BR-074 | As an FRBS user, I need accounts analysis, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-AS-010 | FR-AS-010 AC1 | TC-AC-010.1 to 010.3 (5 cases) |
| BR-075 | As an FRBS user, I need to receive and release CWT (commission / supplier), so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-057, FR-DS-058 | FR-DS-057 AC1; FR-DS-058 AC1 | TC-AC-057.1 to 057.2, 058.1 to 058.2 (9 cases) |
| BR-076 | As an FRBS user, I need to receive and release CWT (commission / supplier), so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-057, FR-DS-058 | FR-DS-057 AC1; FR-DS-058 AC1 | TC-AC-057.1 to 057.2, 058.1 to 058.2 (9 cases) |
| BR-077 | As an FRBS user, I need disbursement to insurer, client, supplier, BDO subsidiaries; to generate Direct Credit transactions file; disbursement to government agencies and employees, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-033 | FR-DS-033 AC1 | TC-AC-033.1 to 033.2 (4 cases) |
| BR-078 | As an FRBS user, I need reporting and documentation; to release BIR 2307 on premiums (PR2307) to insurer; to extract, download and print reports in multiple formats, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-081, FR-AC-060, FR-AC-061, FR-AC-062, FR-OP-113, FR-CR-040, FR-NB-124, FR-NB-125 | FR-DS-081 AC1; FR-AC-060 AC1-2; FR-AC-061 AC1-2; FR-AC-062 AC1-2; FR-OP-113 AC1-2; FR-CR-040 AC1-2; FR-NB-124 AC1-2; FR-NB-125 AC1-2 | TC-AC-081.1 to 081.2, 060.1 to 060.2, 061.1 to 061.3, 062.1 to 062.2, TC-OP-113.1 to 113.2, TC-NB-124.1 to 124.2, 125.1 to 125.2 (24 cases) |
| BR-079 | As an FRBS user, I need payee management, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-010 | FR-DS-010 AC1-3 | TC-AC-010.1 to 010.4 (9 cases) |
| BR-080 | As an FRBS user, I need check printing and series management, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-033 | FR-DS-033 AC1 | TC-AC-033.1 to 033.2 (4 cases) |
| BR-081 | As an FRBS user, I need status tagging and tracking, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-050, FR-DS-052 | FR-DS-050 AC1; FR-DS-052 AC1 | TC-AC-050.1 to 050.2, 052.1 to 052.2 (5 cases) |
| BR-082 | As an FRBS user, I need status tagging and tracking, so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-050, FR-DS-052 | FR-DS-050 AC1; FR-DS-052 AC1 | TC-AC-050.1 to 050.2, 052.1 to 052.2 (5 cases) |
| BR-083 | As a Marketing AO, I need automated notifications, approvals and feedback tracking, so that approvals and feedback do not wait on e-mail. | FR-CR-020, FR-NB-015, FR-OP-040 | FR-CR-020 AC1-2; FR-NB-015 AC1-2; FR-OP-040 AC1-2 | TC-NB-015.1 to 015.2, TC-OP-040.1 to 040.2 (6 cases) |
| BR-084 | As an FRBS user, I need reporting and documentation; BIR standard books (sales, purchase, cash receipts, cash disbursements, general journal), so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-DS-081, FR-AC-060, FR-AC-061, FR-AC-062, FR-CR-040, FR-AC-063 | FR-DS-081 AC1; FR-AC-060 AC1-2; FR-AC-061 AC1-2; FR-AC-062 AC1-2; FR-CR-040 AC1-2; FR-AC-063 AC1-2 | TC-AC-081.1 to 081.2, 060.1 to 060.2, 061.1 to 061.3, 062.1 to 062.2, 063.1 to 063.3 (24 cases) |
| BR-085 | As an FRBS user, I need to generate manual service invoice (Other Income), so that the books, disbursements and subsidiary ledgers are complete and correct. | FR-NB-117 | FR-NB-117 AC1-3 | TC-NB-117.1 to 117.4 (6 cases) |
| BR-086 | As a Cashiering user, I need manual issuance of AR, OR, invoice, cash and cheque OTC payment, so that every payment is received, applied and receipted correctly. | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-012 | FR-OP-011 AC1-3; FR-OP-013 AC1-3; FR-OP-014 AC1-3; FR-OP-012 AC1-2 | TC-OP-011.1 to 011.3, 013.1 to 013.3, 014.1 to 014.3, 012.1 to 012.2 (16 cases) |
| BR-087 | As a Cashiering user, I need automated accounting entries, so that every payment is received, applied and receipted correctly. | FR-OP-013, FR-OP-027 | FR-OP-013 AC1-3; FR-OP-027 AC1-2 | TC-OP-013.1 to 013.3, 027.1 to 027.2 (7 cases) |
| BR-088 | As an FRBS user, I need currency selection and multi-currency support, so that foreign currency business is recorded correctly. | FR-CR-091, FR-CM-011 | FR-CR-091 AC1; FR-CM-011 AC1-4 | TC-CM-011.1 to 011.5 (8 cases) |
| BR-089 | As a Cashiering user, I need batch payment files processing (automatching), so that every payment is received, applied and receipted correctly. | FR-OP-015, FR-OP-016 | FR-OP-015 AC1-3; FR-OP-016 AC1-3 | TC-OP-015.1 to 015.3, 016.1 to 016.3 (8 cases) |
| BR-090 | As a Collection user, I need unapplied payment disposition (excess payment); unapplied payment management (excess payment), so that premium receivables are collected and followed up. | FR-CL-074, FR-CL-070, FR-CL-077, FR-OP-022 | FR-CL-074 AC1-3; FR-CL-070 AC1-3; FR-CL-077 AC1-2; FR-OP-022 AC1-4 | TC-CL-074.1 to 074.4, 070.1 to 070.3, 077.1 to 077.3, TC-OP-022.1 to 022.3 (19 cases) |
| BR-091 | As a Cashiering user, I need payment auto matching, so that every payment is received, applied and receipted correctly. | FR-OP-018, FR-OP-132 | FR-OP-018 AC1-4; FR-OP-132 AC1 | TC-OP-018.1 to 018.3, 132.1 to 132.2 (7 cases) |
| BR-092 | As a Cashiering user, I need batch processing and automation (automatch re-run), so that every payment is received, applied and receipted correctly. | FR-OP-018, FR-OP-132 | FR-OP-018 AC1-4; FR-OP-132 AC1 | TC-OP-018.1 to 018.3, 132.1 to 132.2 (7 cases) |
| BR-093 | As a Cashiering user, I need unapplied payment management (excess payment), so that every payment is received, applied and receipted correctly. | FR-OP-022 | FR-OP-022 AC1-4 | TC-OP-022.1 to 022.3 (6 cases) |
| BR-094 | As a Cashiering user, I need to generate acknowledgement receipt, official receipt, invoice, so that every payment is received, applied and receipted correctly. | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 | FR-OP-011 AC1-3; FR-OP-013 AC1-3; FR-OP-014 AC1-3; FR-OP-010 AC1-4 | TC-OP-011.1 to 011.3, 013.1 to 013.3, 014.1 to 014.3, 010.1 to 010.3 (18 cases) |
| BR-095 | As a Cashiering user, I need to generate acknowledgement receipt, official receipt, invoice, so that every payment is received, applied and receipted correctly. | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 | FR-OP-011 AC1-3; FR-OP-013 AC1-3; FR-OP-014 AC1-3; FR-OP-010 AC1-4 | TC-OP-011.1 to 011.3, 013.1 to 013.3, 014.1 to 014.3, 010.1 to 010.3 (18 cases) |
| BR-096 | As a Cashiering user, I need to generate acknowledgement receipt, official receipt, invoice, so that every payment is received, applied and receipted correctly. | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 | FR-OP-011 AC1-3; FR-OP-013 AC1-3; FR-OP-014 AC1-3; FR-OP-010 AC1-4 | TC-OP-011.1 to 011.3, 013.1 to 013.3, 014.1 to 014.3, 010.1 to 010.3 (18 cases) |
| BR-097 | As a Cashiering user, I need premium payment monitoring, so that every payment is received, applied and receipted correctly. | FR-OP-028, FR-OP-009 | FR-OP-028 AC1-2; FR-OP-009 AC1-3 | TC-OP-028.1 to 028.2, 009.1 to 009.2 (5 cases) |
| BR-098 | As a Cashiering user, I need commission fee collection, so that every payment is received, applied and receipted correctly. | FR-OP-021, FR-OP-091 | FR-OP-021 AC1-2; FR-OP-091 AC1-3 | TC-OP-021.1 to 021.2, 091.1 to 091.2 (8 cases) |
| BR-099 | As a Cashiering user, I need commission fee collection, so that every payment is received, applied and receipted correctly. | FR-OP-021, FR-OP-091 | FR-OP-021 AC1-2; FR-OP-091 AC1-3 | TC-OP-021.1 to 021.2, 091.1 to 091.2 (8 cases) |
| BR-100 | As a Cashiering user, I need PDC management, so that every payment is received, applied and receipted correctly. | FR-OP-015, FR-OP-016 | FR-OP-015 AC1-3; FR-OP-016 AC1-3 | TC-OP-015.1 to 015.3, 016.1 to 016.3 (8 cases) |
| BR-101 | As a Business Administrator, I need workflow maintained at the back end; data flows to the next process by rules, so that work moves to the next process by rules. | FR-CR-080, FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 | FR-CR-080 AC1; FR-NB-011 AC1-3; FR-NB-064 AC1-3; FR-NB-010 AC1-3; FR-NB-122 AC1-3 | TC-NB-011.1 to 011.3, 064.1 to 064.3, 010.1 to 010.3, 122.1 to 122.3 (19 cases) |
| BR-102 | As a Remittance user, I need extraction and remittance processing (scheduled or manual), so that premiums are remitted to the insurers in full and on time. | FR-OP-030 | FR-OP-030 AC1-4 | TC-OP-030.1 to 030.3 (5 cases) |
| BR-103 | As a Remittance user, I need extraction and remittance processing (scheduled or manual); scheduled or on-demand report generation, so that premiums are remitted to the insurers in full and on time. | FR-OP-030, FR-CR-043, FR-DS-081, FR-CL-082 | FR-OP-030 AC1-4; FR-CR-043 AC1-2; FR-DS-081 AC1; FR-CL-082 AC1 | TC-OP-030.1 to 030.3, TC-AC-081.1 to 081.2, TC-CL-082.1 to 082.2 (9 cases) |
| BR-104 | As a Remittance user, I need sending and uploading of files, so that premiums are remitted to the insurers in full and on time. | FR-OP-034, FR-OP-037 | FR-OP-034 AC1-2; FR-OP-037 AC1-3 | TC-OP-034.1 to 034.2, 037.1 to 037.2 (7 cases) |
| BR-105 | As a Remittance user, I need sending and uploading of files, so that premiums are remitted to the insurers in full and on time. | FR-OP-034, FR-OP-037 | FR-OP-034 AC1-2; FR-OP-037 AC1-3 | TC-OP-034.1 to 034.2, 037.1 to 037.2 (7 cases) |
| BR-106 | As a Remittance user, I need validation and filtering criteria, so that premiums are remitted to the insurers in full and on time. | FR-OP-031 | FR-OP-031 AC1-4 | TC-OP-031.1 to 031.3 (5 cases) |
| BR-107 | As a Remittance user, I need validation and filtering criteria, so that premiums are remitted to the insurers in full and on time. | FR-OP-031 | FR-OP-031 AC1-4 | TC-OP-031.1 to 031.3 (5 cases) |
| BR-108 | As a Remittance user, I need validation and filtering criteria, so that premiums are remitted to the insurers in full and on time. | FR-OP-031 | FR-OP-031 AC1-4 | TC-OP-031.1 to 031.3 (5 cases) |
| BR-109 | As a Remittance user, I need to search and view, so that premiums are remitted to the insurers in full and on time. | FR-OP-039, FR-OP-005 | FR-OP-039 AC1-2; FR-OP-005 AC1-3 | TC-OP-039.1 to 039.2, 005.1 to 005.3 (7 cases) |
| BR-110 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-111 | As a Remittance user, I need notifications and tracking; automated notifications, approvals and feedback tracking, so that premiums are remitted to the insurers in full and on time. | FR-OP-040, FR-OP-031, FR-OP-036, FR-CR-020, FR-NB-015 | FR-OP-040 AC1-2; FR-OP-031 AC1-4; FR-OP-036 AC1-2; FR-CR-020 AC1-2; FR-NB-015 AC1-2 | TC-OP-040.1 to 040.2, 031.1 to 031.3, 036.1 to 036.2, TC-NB-015.1 to 015.2 (14 cases) |
| BR-112 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-113 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-114 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-115 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-116 | As a Remittance user, I need hold remittance management, so that premiums are remitted to the insurers in full and on time. | FR-OP-031, FR-OP-005 | FR-OP-031 AC1-4; FR-OP-005 AC1-3 | TC-OP-031.1 to 031.3, 005.1 to 005.3 (9 cases) |
| BR-117 | As a Remittance user, I need special remittance request and processing, so that premiums are remitted to the insurers in full and on time. | FR-OP-040 | FR-OP-040 AC1-2 | TC-OP-040.1 to 040.2 (3 cases) |
| BR-118 | As a Collection user, I need to tag CWT, premium / PR2307, so that premium receivables are collected and followed up. | FR-CL-032, FR-CL-081, FR-OP-113 | FR-CL-032 AC1-3; FR-CL-081 AC1; FR-OP-113 AC1-2 | TC-CL-032.1 to 032.5, 081.1 to 081.2, TC-OP-113.1 to 113.2 (15 cases) |
| BR-119 | As a Production Reconciliation user, I need data extraction and file management, so that BDOI production agrees with the insurers' records. | FR-OP-070, FR-OP-074 | FR-OP-070 AC1; FR-OP-074 AC1-2 | TC-OP-070.1 to 070.2, 074.1 to 074.2 (6 cases) |
| BR-120 | As a Production Reconciliation user, I need data extraction and file management, so that BDOI production agrees with the insurers' records. | FR-OP-070, FR-OP-074 | FR-OP-070 AC1; FR-OP-074 AC1-2 | TC-OP-070.1 to 070.2, 074.1 to 074.2 (6 cases) |
| BR-121 | As a Production Reconciliation user, I need production register viewing and filtering, so that BDOI production agrees with the insurers' records. | FR-OP-071, FR-OP-077 | FR-OP-071 AC1-2; FR-OP-077 AC1-2 | TC-OP-071.1 to 071.2, 077.1 to 077.2 (7 cases) |
| BR-122 | As a Production Reconciliation user, I need matching and automation, so that BDOI production agrees with the insurers' records. | FR-OP-076, FR-OP-075 | FR-OP-076 AC1-2; FR-OP-075 AC1-3 | TC-OP-076.1 to 076.2, 075.1 to 075.3 (6 cases) |
| BR-123 | As a Production Reconciliation user, I need matching and automation, so that BDOI production agrees with the insurers' records. | FR-OP-076, FR-OP-075 | FR-OP-076 AC1-2; FR-OP-075 AC1-3 | TC-OP-076.1 to 076.2, 075.1 to 075.3 (6 cases) |
| BR-124 | As a Production Reconciliation user, I need tracking and monitoring; audit logs and full transaction history, so that BDOI production agrees with the insurers' records. | FR-OP-079, FR-OP-075, FR-OP-074, FR-CR-030, FR-NB-016, FR-NB-136 | FR-OP-079 AC1-2; FR-OP-075 AC1-3; FR-OP-074 AC1-2; FR-CR-030 AC1-2; FR-NB-016 AC1-2; FR-NB-136 AC1 | TC-OP-079.1 to 079.2, 075.1 to 075.3, 074.1 to 074.2, TC-NB-016.1 to 016.2, 136.1 to 136.2 (14 cases) |
| BR-125 | As a Marketing TL, I need centralised, real-time dashboards for all roles; details invoked from the dashboard, so that every role sees its work and figures in real time. | FR-CR-010, FR-CR-011, FR-NB-120, FR-OP-003, FR-PM-070 | FR-CR-010 AC1-3; FR-CR-011 AC1; FR-NB-120 AC1-2; FR-OP-003 AC1-3; FR-PM-070 AC1-2 | TC-NB-120.1 to 120.3, TC-OP-003.1 to 003.3, TC-PM-070.1 to 070.3 (11 cases) |
| BR-126 | As an Adjustment user, I need transaction management (endorsements, cancellations), so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-050, FR-OP-053 | FR-OP-050 AC1-4; FR-OP-053 AC1-3 | TC-OP-050.1 to 050.4, 053.1 to 053.3 (12 cases) |
| BR-127 | As an Adjustment user, I need automated accounting entries, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-056, FR-OP-057 | FR-OP-056 AC1-2; FR-OP-057 AC1-2 | TC-OP-056.1 to 056.2, 057.1 to 057.2 (6 cases) |
| BR-128 | As an Adjustment user, I need traceability and auditability, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-050, FR-OP-061 | FR-OP-050 AC1-4; FR-OP-061 AC1-5 | TC-OP-050.1 to 050.4, 061.1 to 061.3 (12 cases) |
| BR-129 | As an Adjustment user, I need traceability and auditability; audit logs and full transaction history, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-050, FR-OP-061, FR-CR-030, FR-NB-016, FR-NB-136 | FR-OP-050 AC1-4; FR-OP-061 AC1-5; FR-CR-030 AC1-2; FR-NB-016 AC1-2; FR-NB-136 AC1 | TC-OP-050.1 to 050.4, 061.1 to 061.3, TC-NB-016.1 to 016.2, 136.1 to 136.2 (17 cases) |
| BR-130 | As an Adjustment user, I need to search and document management, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-005, FR-OP-061, FR-OP-052 | FR-OP-005 AC1-3; FR-OP-061 AC1-5; FR-OP-052 AC1 | TC-OP-005.1 to 005.3, 061.1 to 061.3, 052.1 to 052.2 (11 cases) |
| BR-131 | As an Adjustment user, I need to search and document management, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-005, FR-OP-061, FR-OP-052 | FR-OP-005 AC1-3; FR-OP-061 AC1-5; FR-OP-052 AC1 | TC-OP-005.1 to 005.3, 061.1 to 061.3, 052.1 to 052.2 (11 cases) |
| BR-132 | As an Adjustment user, I need automated accounting entries, so that endorsements and cancellations are processed and accounted for correctly. | FR-OP-056, FR-OP-057 | FR-OP-056 AC1-2; FR-OP-057 AC1-2 | TC-OP-056.1 to 056.2, 057.1 to 057.2 (6 cases) |
| BR-133 | As a Commission user, I need automated commission receivables processing, so that commissions on direct payments are collected. | FR-OP-091 | FR-OP-091 AC1-3 | TC-OP-091.1 to 091.2 (4 cases) |
| BR-134 | As a Commission user, I need comprehensive production reporting, so that commissions on direct payments are collected. | FR-OP-097, FR-OP-080 | FR-OP-097 AC1-2; FR-OP-080 AC1-2 | TC-OP-097.1 to 097.2, 080.1 to 080.2 (6 cases) |
| BR-135 | As a Commission user, I need risk mitigation and error handling, so that commissions on direct payments are collected. | FR-OP-091, FR-OP-093 | FR-OP-091 AC1-3; FR-OP-093 AC1-3 | TC-OP-091.1 to 091.2, 093.1 to 093.3 (8 cases) |
| BR-136 | As a Commission user, I need collection of commission receivables (direct payment), so that commissions on direct payments are collected. | FR-OP-091, FR-OP-098 | FR-OP-091 AC1-3; FR-OP-098 AC1 | TC-OP-091.1 to 091.2, 098.1 to 098.2 (6 cases) |
| BR-137 | As a Commission user, I need collection of commission receivables (direct payment), so that commissions on direct payments are collected. | FR-OP-091, FR-OP-098 | FR-OP-091 AC1-3; FR-OP-098 AC1 | TC-OP-091.1 to 091.2, 098.1 to 098.2 (6 cases) |
| BR-138 | As a Commission user, I need auto-match reversals, so that commissions on direct payments are collected. | FR-OP-091, FR-OP-113 | FR-OP-091 AC1-3; FR-OP-113 AC1-2 | TC-OP-091.1 to 091.2, 113.1 to 113.2 (7 cases) |
| BR-139 | As a Collection user, I need to tag CWT, premium / PR2307; auto-match reversals, so that premium receivables are collected and followed up. | FR-CL-032, FR-CL-081, FR-OP-113, FR-OP-091 | FR-CL-032 AC1-3; FR-CL-081 AC1; FR-OP-113 AC1-2; FR-OP-091 AC1-3 | TC-CL-032.1 to 032.5, 081.1 to 081.2, TC-OP-113.1 to 113.2, 091.1 to 091.2 (19 cases) |
| BR-140 | As a Commission user, I need collection of commission receivables (direct payment), so that commissions on direct payments are collected. | FR-OP-091, FR-OP-098 | FR-OP-091 AC1-3; FR-OP-098 AC1 | TC-OP-091.1 to 091.2, 098.1 to 098.2 (6 cases) |
| BR-141 | As a Claims Officer, I need to process claims advice from BDOI Marketing; to process claims advice from client; to process claims advice from insurer, so that claims are filed and followed until settled. | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-014, FR-CM-002, FR-CM-003, FR-CM-022, FR-CM-024 | FR-CM-010 AC1-3; FR-CM-011 AC1-4; FR-CM-015 AC1-4; FR-CM-014 AC1-2; FR-CM-002 AC1-3; FR-CM-003 AC1-3; FR-CM-022 AC1-3; FR-CM-024 AC1-2 | TC-CM-010.1 to 010.4, 011.1 to 011.5, 015.1 to 015.3, 014.1 to 014.2, 002.1 to 002.4, 003.1 to 003.3, 022.1 to 022.4, 024.1 to 024.3 (40 cases) |
| BR-142 | As a Claims Officer, I need to process LOA from insurer, so that claims are filed and followed until settled. | FR-CM-043, FR-CM-040, FR-CM-046 | FR-CM-043 AC1-2; FR-CM-040 AC1-2; FR-CM-046 AC1-3 | TC-CM-043.1 to 043.3, 040.1 to 040.3, 046.1 to 046.3 (10 cases) |
| BR-143 | As a Claims Officer, I need to process settlement offer from insurer, so that claims are filed and followed until settled. | FR-CM-040, FR-CM-046, FR-CM-043 | FR-CM-040 AC1-2; FR-CM-046 AC1-3; FR-CM-043 AC1-2 | TC-CM-040.1 to 040.3, 046.1 to 046.3, 043.1 to 043.3 (10 cases) |
| BR-144 | As a Claims Officer, I need to tag permanent closure, so that claims are filed and followed until settled. | FR-CM-045, FR-CM-002 | FR-CM-045 AC1-3; FR-CM-002 AC1-3 | TC-CM-045.1 to 045.3, 002.1 to 002.4 (13 cases) |
| BR-145 | As a Claims Officer, I need to tag temporary closure, so that claims are filed and followed until settled. | FR-CM-045 | FR-CM-045 AC1-3 | TC-CM-045.1 to 045.3 (7 cases) |
| BR-146 | As a Claims Officer, I need unclaimed checks safekeeping, so that claims are filed and followed until settled. | CLR-CR-02 (CRQ03) | - | - |
| BR-147 | As a Claims Officer, I need handover of settlement checks to Cashiering, so that claims are filed and followed until settled. | CLR-CR-02 (CRQ03) | - | - |
| BR-148 | As a Claims Officer, I need retrieval of checks from Cashiering for release, so that claims are filed and followed until settled. | CLR-CR-02 (CRQ03) | - | - |
| BR-149 | As a Claims Officer, I need reports and analytics viewing, so that claims are filed and followed until settled. | FR-CM-060, FR-CM-044, FR-CM-061, FR-CM-062 | FR-CM-060 AC1-3; FR-CM-044 AC1-4; FR-CM-061 AC1-3; FR-CM-062 AC1-3 | TC-CM-060.1 to 060.4, 044.1 to 044.4, 061.1 to 061.5, 062.1 to 062.4 (23 cases) |
| BR-150 | As a Claims Officer, I need to encode / override the next follow-up date, so that claims are filed and followed until settled. | FR-CM-002, FR-CM-050, FR-CM-054 | FR-CM-002 AC1-3; FR-CM-050 AC1-2; FR-CM-054 AC1-2 | TC-CM-002.1 to 002.4, 050.1 to 050.3, 054.1 to 054.3 (14 cases) |
| BR-151 | As a Claims Officer, I need to maintain full claims history for audit and compliance; audit logs and full transaction history, so that claims are filed and followed until settled. | FR-CM-012, FR-CM-052, FR-CM-054, FR-CR-030, FR-NB-016, FR-NB-136 | FR-CM-012 AC1-3; FR-CM-052 AC1-2; FR-CM-054 AC1-2; FR-CR-030 AC1-2; FR-NB-016 AC1-2; FR-NB-136 AC1 | TC-CM-012.1 to 012.4, 052.1 to 052.4, 054.1 to 054.3, TC-NB-016.1 to 016.2, 136.1 to 136.2 (19 cases) |
| BR-152 | As a Claims Officer, I need reports and analytics viewing; centralised, real-time dashboards for all roles; details invoked from the dashboard, so that claims are filed and followed until settled. | FR-CM-060, FR-CM-044, FR-CM-061, FR-CM-062, FR-CR-010, FR-CR-011, FR-NB-120, FR-OP-003, FR-PM-070 | FR-CM-060 AC1-3; FR-CM-044 AC1-4; FR-CM-061 AC1-3; FR-CM-062 AC1-3; FR-CR-010 AC1-3; FR-CR-011 AC1; FR-NB-120 AC1-2; FR-OP-003 AC1-3; FR-PM-070 AC1-2 | TC-CM-060.1 to 060.4, 044.1 to 044.4, 061.1 to 061.5, 062.1 to 062.4, TC-NB-120.1 to 120.3, TC-OP-003.1 to 003.3, TC-PM-070.1 to 070.3 (34 cases) |
| BR-153 | As a Claims Unit Head, I need placement request initiation, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-154 | As a Claims Unit Head, I need placement slip management, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-155 | As a Claims Unit Head, I need statement of Account (SOA) generation, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-156 | As a Claims Unit Head, I need statement of Account (SOA) generation, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-157 | As a Claims Unit Head, I need claims reporting and settlement, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-158 | As a Claims Unit Head, I need claims payment processing, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-159 | As a Claims Unit Head, I need direct client claims payment, so that reinsurance placements are managed (phase 2). | Phase 2 (ReInsurance BRD) | - | - |
| BR-160 | As a Marketing TL, I need centralised, real-time dashboards for all roles; details invoked from the dashboard, so that every role sees its work and figures in real time. | FR-CR-010, FR-CR-011, FR-NB-120, FR-OP-003, FR-PM-070 | FR-CR-010 AC1-3; FR-CR-011 AC1; FR-NB-120 AC1-2; FR-OP-003 AC1-3; FR-PM-070 AC1-2 | TC-NB-120.1 to 120.3, TC-OP-003.1 to 003.3, TC-PM-070.1 to 070.3 (11 cases) |
| BR-161 | As a CSF Agent, I need to search, retrieve and display client contact and insurance account details, so that callers are served from one view of their client and policies and every inquiry is tracked to closure. | FR-CSF-011, FR-CSF-021, FR-CSF-022, FR-CSF-010 | FR-CSF-011 AC1-3; FR-CSF-021 AC1-4; FR-CSF-022 AC1-2; FR-CSF-010 AC1-4 | TC-CSF-011.1 to 011.5, 021.1 to 021.6, 022.1 to 022.4, 010.1 to 010.8 (35 cases) |
| BR-162 | As a CSF Agent, I need to view, add and update client contact information, so that callers are served from one view of their client and policies and every inquiry is tracked to closure. | FR-CSF-020, FR-CSF-021 | FR-CSF-020 AC1-2; FR-CSF-021 AC1-4 | TC-CSF-020.1 to 020.5, 021.1 to 021.6 (19 cases) |
| BR-163 | As a CSF Agent, I need to view mode of payment (history) and current status, so that callers are served from one view of their client and policies and every inquiry is tracked to closure. | FR-CSF-012, FR-CSF-013 | FR-CSF-012 AC1-3; FR-CSF-013 AC1-2 | TC-CSF-012.1 to 012.4, 013.1 to 013.5 (11 cases) |
| BR-164 | As a CSF Agent, I need to view and resend RA (and e-policy), so that callers are served from one view of their client and policies and every inquiry is tracked to closure. | FR-CSF-030, FR-CSF-033 | FR-CSF-030 AC1-3; FR-CSF-033 AC1-3 | TC-CSF-030.1 to 030.6, 033.1 to 033.5 (15 cases) |
| BR-165 | As a CSF Agent, I need case resolution: add / edit case details and status, so that callers are served from one view of their client and policies and every inquiry is tracked to closure. | FR-CSF-050, FR-CSF-052, FR-CSF-056, FR-CSF-058, FR-CSF-060 | FR-CSF-050 AC1-6; FR-CSF-052 AC1-4; FR-CSF-056 AC1-4; FR-CSF-058 AC1-3; FR-CSF-060 AC1-4 | TC-CSF-050.1 to 050.6, 052.1 to 052.4, 056.1 to 056.4, 058.1 to 058.3, 060.1 to 060.4 (28 cases) |
| BR-166 | As a Commission user, I need automated incentive calculation, so that commissions on direct payments are collected. | FR-OP-095 | FR-OP-095 AC1-2 | TC-OP-095.1 to 095.2 (4 cases) |
| BR-167 | As a Commission user, I need automated incentive calculation, so that commissions on direct payments are collected. | FR-OP-095 | FR-OP-095 AC1-2 | TC-OP-095.1 to 095.2 (4 cases) |
| BR-168 | As a Commission user, I need motor Mania incentive plan, so that commissions on direct payments are collected. | FR-OP-095 | FR-OP-095 AC1-2 | TC-OP-095.1 to 095.2 (4 cases) |
| BR-169 | As a Commission user, I need production data validation and exclusion handling, so that commissions on direct payments are collected. | FR-OP-095 | FR-OP-095 AC1-2 | TC-OP-095.1 to 095.2 (4 cases) |
| BR-170 | As a Business Administrator, I need master data change logging (user, product, insurer, LOVs), so that master data is consistent and controlled across BIBS. | FR-CR-031, FR-NB-016, FR-NB-132, FR-PM-005, FR-CL-003 | FR-CR-031 AC1-2; FR-NB-016 AC1-2; FR-NB-132 AC1-2; FR-PM-005 AC1-3; FR-CL-003 AC1-3 | TC-NB-016.1 to 016.2, 132.1 to 132.2, TC-PM-005.1 to 005.3, TC-CL-003.1 to 003.4 (14 cases) |
| BR-171 | As a Business Administrator, I need insurer management, so that master data is consistent and controlled across BIBS. | FR-CR-061, FR-NB-053 | FR-CR-061 AC1-2; FR-NB-053 AC1-3 | TC-NB-053.1 to 053.3 (5 cases) |
| BR-172 | As a Business Administrator, I need LOV maintenance, so that master data is consistent and controlled across BIBS. | FR-CR-060, FR-NB-132, FR-AC-070 | FR-CR-060 AC1; FR-NB-132 AC1-2; FR-AC-070 AC1-2 | TC-NB-132.1 to 132.2, TC-AC-070.1 to 070.4 (10 cases) |
| BR-173 | As a Business Administrator, I need MIS field definition, so that master data is consistent and controlled across BIBS. | FR-CR-062, FR-NB-110, FR-NB-119 | FR-CR-062 AC1; FR-NB-110 AC1-3; FR-NB-119 AC1-2 | TC-NB-110.1 to 110.3, 119.1 to 119.2 (7 cases) |
| BR-174 | As a Business Administrator, I need MIS field definition, so that master data is consistent and controlled across BIBS. | FR-CR-062, FR-NB-110, FR-NB-119 | FR-CR-062 AC1; FR-NB-110 AC1-3; FR-NB-119 AC1-2 | TC-NB-110.1 to 110.3, 119.1 to 119.2 (7 cases) |
| BR-175 | As an FRBS user, I need invoice Master List across workflows, so that every invoice is visible across the workflows. | FR-CR-090, FR-CL-010, FR-AS-026 | FR-CR-090 AC1; FR-CL-010 AC1-3; FR-AS-026 AC1 | TC-CL-010.1 to 010.4, TC-AC-026.1 to 026.2 (7 cases) |
| BR-176 | As a Business Administrator, I need to include other or additional system capabilities when BDOI defines them, so that BIBS can grow with the business. | CLR-CR-09 (CRQ10) | - | - |
| BR-177 | As a Business Administrator, I need workflow maintained at the back end; data flows to the next process by rules, so that work moves to the next process by rules. | FR-CR-080, FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 | FR-CR-080 AC1; FR-NB-011 AC1-3; FR-NB-064 AC1-3; FR-NB-010 AC1-3; FR-NB-122 AC1-3 | TC-NB-011.1 to 011.3, 064.1 to 064.3, 010.1 to 010.3, 122.1 to 122.3 (19 cases) |
| BR-178 | As a Business Administrator, I need BDO brand colours, logos, icons and design system, so that every screen and document carries the BDO brand. | FR-CR-092 | FR-CR-092 AC1-2 | - |
| BR-179 | As a Business Administrator, I need BDO brand colours, logos, icons and design system, so that every screen and document carries the BDO brand. | FR-CR-092 | FR-CR-092 AC1-2 | - |
| BR-180 | As a Business Administrator, I need BDO brand colours, logos, icons and design system, so that every screen and document carries the BDO brand. | FR-CR-092 | FR-CR-092 AC1-2 | - |
| BR-181 | As a Business Administrator, I need BIBS to follow the identified user journeys, shown in a walkthrough, so that every persona works through screens that match the way BDOI works. | FR-CR-001, FR-CR-080, FR-CR-092 | FR-CR-001 AC1-3; FR-CR-080 AC1; FR-CR-092 AC1-2 | - |
| BR-182 | As a Business Administrator, I need the interaction flows to be customisable, shown in a walkthrough, so that the screens follow the identified user journeys. | FR-CR-080, FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122, FR-CR-001, FR-CR-092 | FR-CR-080 AC1; FR-NB-011 AC1-3; FR-NB-064 AC1-3; FR-NB-010 AC1-3; FR-NB-122 AC1-3; FR-CR-001 AC1-3; FR-CR-092 AC1-2 | TC-NB-011.1 to 011.3, 064.1 to 064.3, 010.1 to 010.3, 122.1 to 122.3 (19 cases) |
| BR-183 | As a Business Administrator, I need the vendor's designs delivered for the BDOI design tool (Figma), so that the BDOI UX Design team works from the delivered designs. | FR-CR-092 | FR-CR-092 AC1-2 | - |
| BR-184 | As an EB AO, I need automated renewal notifications, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-022 | FR-EB-022 AC1-3 | TC-EB-022.1 to 022.3 (5 cases) |
| BR-185 | As an EB AO, I need manual and system-based proposal generation, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-024 | FR-EB-024 AC1-2 | TC-EB-024.1 to 024.2 (3 cases) |
| BR-186 | As an EB AO, I need document and data upload management, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-001, FR-EB-010, FR-EB-011, FR-EB-014 | FR-EB-001 AC1-3; FR-EB-010 AC1-3; FR-EB-011 AC1-3; FR-EB-014 AC1-3 | TC-EB-001.1 to 001.3, 010.1 to 010.4, 011.1 to 011.4, 014.1 to 014.3 (20 cases) |
| BR-187 | As an EB AO, I need broker on record management, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-031 | FR-EB-031 AC1-2 | TC-EB-031.1 to 031.3 (5 cases) |
| BR-188 | As an EB AO, I need terms of Reference generation and distribution, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-004, FR-EB-030, FR-EB-035 | FR-EB-004 AC1-2; FR-EB-030 AC1-2; FR-EB-035 AC1-3 | TC-EB-004.1 to 004.2, 030.1 to 030.3, 035.1 to 035.3 (11 cases) |
| BR-189 | As an EB AO, I need comparative report management, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-040, FR-EB-041, FR-EB-015, FR-EB-043 | FR-EB-040 AC1-2; FR-EB-041 AC1-3; FR-EB-015 AC1-3; FR-EB-043 AC1-2 | TC-EB-040.1 to 040.3, 041.1 to 041.3, 015.1 to 015.3, 043.1 to 043.3 (18 cases) |
| BR-190 | As an EB AO, I need client feedback, change, additional or amendment request capture and relay, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-023, FR-EB-044, FR-EB-054, FR-EB-055 | FR-EB-023 AC1-2; FR-EB-044 AC1-2; FR-EB-054 AC1-2; FR-EB-055 AC1-3 | TC-EB-023.1 to 023.2, 044.1 to 044.2, 054.1 to 054.2, 055.1 to 055.3 (13 cases) |
| BR-191 | As an EB AO, I need automated and manual sending of proposals, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-024, FR-EB-035 | FR-EB-024 AC1-2; FR-EB-035 AC1-3 | TC-EB-024.1 to 024.2, 035.1 to 035.3 (8 cases) |
| BR-192 | As an EB AO, I need approval workflow based on defined thresholds, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-042 | FR-EB-042 AC1-3 | TC-EB-042.1 to 042.3 (5 cases) |
| BR-193 | As an EB AO, I need placement and booking management, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-046, FR-EB-051, FR-EB-052 | FR-EB-046 AC1-3; FR-EB-051 AC1-2; FR-EB-052 AC1-2 | TC-EB-046.1 to 046.4, 051.1 to 051.2, 052.1 to 052.3 (11 cases) |
| BR-194 | As an EB AO, I need centralised reporting and analytics, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-060, FR-EB-062, FR-EB-001, FR-EB-003 | FR-EB-060 AC1-3; FR-EB-062 AC1-2; FR-EB-001 AC1-3; FR-EB-003 AC1-3 | TC-EB-060.1 to 060.4, 062.1 to 062.3, 001.1 to 001.3, 003.1 to 003.3 (16 cases) |
| BR-195 | As an EB AO, I need to manage franchise requests and approvals, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-032, FR-EB-034, FR-EB-033 | FR-EB-032 AC1-3; FR-EB-034 AC1-2; FR-EB-033 AC1-2 | TC-EB-032.1 to 032.3, 034.1 to 034.3, 033.1 to 033.2 (11 cases) |
| BR-196 | As an EB AO, I need automated renewal notifications, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-022 | FR-EB-022 AC1-3 | TC-EB-022.1 to 022.3 (5 cases) |
| BR-197 | As an EB AO, I need manual and system-based proposal generation, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-024 | FR-EB-024 AC1-2 | TC-EB-024.1 to 024.2 (3 cases) |
| BR-198 | As an EB AO, I need document and data upload management, so that employee benefit programmes are placed, renewed and serviced. | FR-EB-001, FR-EB-010, FR-EB-011, FR-EB-014 | FR-EB-001 AC1-3; FR-EB-010 AC1-3; FR-EB-011 AC1-3; FR-EB-014 AC1-3 | TC-EB-001.1 to 001.3, 010.1 to 010.4, 011.1 to 011.4, 014.1 to 014.3 (20 cases) |
| BR-199 | As a TSU Officer, I need automated request handling, so that products and packages are maintained in one place for all modules. | FR-PM-020, FR-PM-024, FR-PM-045, FR-PM-021 | FR-PM-020 AC1-4; FR-PM-024 AC1-3; FR-PM-045 AC1-2; FR-PM-021 AC1-3 | TC-PM-020.1 to 020.5, 024.1 to 024.4, 045.1 to 045.3, 021.1 to 021.3 (27 cases) |
| BR-200 | As a TSU Officer, I need automatic reference numbers, so that products and packages are maintained in one place for all modules. | FR-PM-020, FR-PM-021 | FR-PM-020 AC1-4; FR-PM-021 AC1-3 | TC-PM-020.1 to 020.5, 021.1 to 021.3 (16 cases) |
| BR-201 | As a TSU Officer, I need quotation / proposal management, so that products and packages are maintained in one place for all modules. | FR-PM-030, FR-PM-031, FR-PM-034, FR-PM-036 | FR-PM-030 AC1-4; FR-PM-031 AC1-3; FR-PM-034 AC1-3; FR-PM-036 AC1-2 | TC-PM-030.1 to 030.5, 031.1 to 031.3, 034.1 to 034.4, 036.1 to 036.3 (24 cases) |
| BR-202 | As a TSU Officer, I need automatic comparison tables, so that products and packages are maintained in one place for all modules. | FR-PM-034, FR-PM-035 | FR-PM-034 AC1-3; FR-PM-035 AC1-2 | TC-PM-034.1 to 034.4, 035.1 to 035.3 (10 cases) |
| BR-203 | As a TSU Officer, I need real-time dashboard / reports; centralised, real-time dashboards for all roles; details invoked from the dashboard, so that products and packages are maintained in one place for all modules. | FR-PM-070, FR-PM-071, FR-CR-010, FR-CR-011, FR-NB-120, FR-OP-003 | FR-PM-070 AC1-2; FR-PM-071 AC1-3; FR-CR-010 AC1-3; FR-CR-011 AC1; FR-NB-120 AC1-2; FR-OP-003 AC1-3 | TC-PM-070.1 to 070.3, 071.1 to 071.3, TC-NB-120.1 to 120.3, TC-OP-003.1 to 003.3 (16 cases) |
| BR-204 | As a TSU Officer, I need automatic checks and approvals in the package workflow, so that products and packages are maintained in one place for all modules. | FR-PM-021, FR-PM-073, FR-PM-043 | FR-PM-021 AC1-3; FR-PM-073 AC1-2; FR-PM-043 AC1-3 | TC-PM-021.1 to 021.3, 073.1 to 073.3, 043.1 to 043.4 (19 cases) |
| BR-205 | As a TSU Officer, I need secure document sharing, so that products and packages are maintained in one place for all modules. | FR-PM-004, FR-PM-002 | FR-PM-004 AC1-2; FR-PM-002 AC1-3 | TC-PM-004.1 to 004.3, 002.1 to 002.4 (10 cases) |
| BR-206 | As a TSU Officer, I need BIBS that works with existing systems; automated notifications, approvals and feedback tracking, so that products and packages are maintained in one place for all modules. | FR-PM-072, FR-CR-020, FR-NB-015, FR-OP-040 | FR-PM-072 AC1-2; FR-CR-020 AC1-2; FR-NB-015 AC1-2; FR-OP-040 AC1-2 | TC-PM-072.1 to 072.2, TC-NB-015.1 to 015.2, TC-OP-040.1 to 040.2 (10 cases) |
| BR-207 | As a TSU Officer, I need to track all changes; audit logs and full transaction history, so that products and packages are maintained in one place for all modules. | FR-PM-005, FR-CR-030, FR-NB-016, FR-NB-136 | FR-PM-005 AC1-3; FR-CR-030 AC1-2; FR-NB-016 AC1-2; FR-NB-136 AC1 | TC-PM-005.1 to 005.3, TC-NB-016.1 to 016.2, 136.1 to 136.2 (8 cases) |
| BR-208 | As a TSU Officer, I need expiring packages monitoring, so that products and packages are maintained in one place for all modules. | FR-PM-044, FR-PM-060, FR-PM-061, FR-PM-071 | FR-PM-044 AC1-3; FR-PM-060 AC1-2; FR-PM-061 AC1-2; FR-PM-071 AC1-3 | TC-PM-044.1 to 044.4, 060.1 to 060.3, 061.1 to 061.3, 071.1 to 071.3 (22 cases) |

<!-- landscape -->

# Appendix: Storyboard index

The umbrella FRS has no screens of its own. Its storyboard is the journey index asked for by BR-181 and BR-182: one frame per capability of the end-to-end journey (Figure 1), with the persona, the step, the walkthrough of the function FRS that shows it on screen (and the matching swimlane of its UX Screen Deck), the outcome and the main FRs. Where a function FRS has no walkthroughs yet, the frame points to the process storyboard of its Storyboard index; the screen-level frames follow with its v2.0 business sign-off pack.

<!-- table: widths=1.3,2.8,5.6,3.6,5.2,2.2 caption="Storyboard of the business processes: frame, persona, step, screen or document, outcome and FR" size=8 -->
| Frame | Persona | Step | Screen or document | Outcome | FR |
|---|---|---|---|---|---|
| **J1** | | **Access and set-up** | | | |
| J1.1 | UAM requestor, approver, implementer | Enrol a new user and give the persona its menus | FRS BRD-11 walkthrough WT-A (A.1-A.8) | The user signs in and sees only the menus of the persona | FR-CR-001, FR-CR-070 |
| J1.2 | TSU Officer | Capability 20 Product Maintenance: a new package from request to first quotation | FRS BRD-3 walkthrough WT-A | Package released and used by New Business | FR-CR-060 |
| J1.3 | Business Administrator | Capability 17 Data Management: lists of values, insurer record, MIS fields, change log | Lists of Values; insurer page (FR-CR-031, 060 to 062) | Master data maintained with approval and a change log | FR-CR-031, FR-CR-060, FR-CR-061, FR-CR-062 |
| **J2** | | **New business** | | | |
| J2.1 | Marketing AO | Capability 1 Client Onboarding: prospect, KYC, verified client; sanction screening | FRS BRD-1 walkthrough WT-A (A.1-A.4); FRS BRD-10 storyboard P2 | Client confirmed and screened | FR-NB-031, FR-NB-034 |
| J2.2 | Marketing AO | Capability 2 Quotation or Proposal: quotation approved and sent | FRS BRD-1 walkthrough WT-A (A.5-A.8) | Quotation accepted; accounts created | FR-NB-041, FR-NB-043, FR-NB-044 |
| J2.3 | TSU Officer | Capability 4 Non-Package Management: PRF, quotation slip, insurer terms, proposal slip | FRS BRD-1 walkthrough WT-B (B.1-B.9) | Proposal accepted; account created with the chosen insurer | FR-NB-050, FR-NB-053, FR-NB-055, FR-NB-056 |
| J2.4 | Marketing AO | Capability 3 Account Creation and Maintenance; submitted policies of the bank | FRS BRD-1 walkthrough WT-A (A.9-A.10); FRS BRD-12 storyboard P1 | Account validated for placement; submitted policies in the masterlist | FR-NB-064, FR-SP-001 |
| J2.5 | Processing Officer | Capability 5 Placement and Booking: CLPC billing, placement, e-policy, booking | FRS BRD-1 walkthrough WT-A (A.11-A.17) | Account booked; invoice and GL entries created | FR-NB-080, FR-NB-100, FR-NB-110 |
| J2.6 | Processing Officer | Returns and corrections across New Business | FRS BRD-1 walkthrough WT-C | Returned records corrected and resubmitted | FR-NB-034, FR-NB-043, FR-NB-067, FR-NB-084 |
| **J3** | | **Money and accounting** | | | |
| J3.1 | Collection user and TL | Capability 7 Marketing Collection: worklist, promise to pay, unapplied payment, installment SOA | FRS BRD-4 walkthroughs WT-A to WT-E | Receivables followed and collected | FR-CL-010, FR-CL-032 |
| J3.2 | Cashiering, Remittance, Prod Recon, Adjustment, Commission users | Capabilities 9 and 10 Cashiering and Remittance: payment to the insurer's official receipt | FRS BRD-2 walkthrough WT-A | Payment applied, receipted and remitted | FR-OP-011, FR-OP-018, FR-OP-040 |
| J3.3 | Cashiering, Remittance, Prod Recon, Adjustment, Commission users | Capability 12 Adjustment / Cancellation: cancellation of a paid and remitted policy to the refund | FRS BRD-2 walkthrough WT-B | Cancellation posted; refund paid | FR-OP-050, FR-OP-053, FR-OP-056 |
| J3.4 | Cashiering, Remittance, Prod Recon, Adjustment, Commission users | Capabilities 11 and 13 Production Reconciliation and Commission Receivables (direct payment) | FRS BRD-2 walkthrough WT-C | Production reconciled; DP commission collected | FR-OP-090, FR-OP-091 |
| J3.5 | FRBS, Disbursement, ACSL users | Capability 8 Accounting, GL, Disbursement and ACSL: accrual, refund, insurer SOA, month end | FRS BRD-5 walkthroughs WT-A to WT-E | Books closed with reconciled subsidiary ledgers | FR-CR-040 |
| **J4** | | **Lifecycle services** | | | |
| J4.1 | Marketing AO | Capability 6 Renewal | FRS BRD-6 storyboard (process flow) | Expiring policies renewed or closed with their letters | FR-SP-060 |
| J4.2 | Claims Officer, Assistant, Unit Head | Capability 14 Claims: recording to settlement and closure | FRS BRD-7 storyboard P1 to P4 | Claim followed to settlement; insurer numbers and updates recorded | FR-CM-011, FR-CM-021, FR-CM-044 |
| J4.3 | CSF Agent, Supervisor | Capability 16 Customer Service Facility: servicing contact and case management | FRS BRD-9 storyboard P1 and P2 | Caller served; inquiry logged as a case and closed | FR-CSF-010, FR-CSF-050, FR-CSF-060 |
| J4.4 | EB AO, Processing, Collection | Capability 19 Employee Benefits: programme to booking, billing and member changes | FRS BRD-8 storyboard P1 to P5 | Programme placed, booked and serviced | FR-EB-022, FR-EB-046, FR-EB-052 |
| J4.5 | Compliance officer, investigator | Sanction screening of clients (capability 1) | FRS BRD-10 storyboard P2 to P4 | Matches investigated and decided; STR filed where needed | FR-SS-031, FR-SS-051, FR-SS-071 |
| J4.6 | Claims Officer, Assistant, Unit Head | Capability 15 Reinsurance (phase 2) | ReInsurance BRD (phase 2) | Not in phase 1 | - |
| **J5** | | **Platform services** | | | |
| J5.1 | Marketing AO | Role dashboards, notifications and audit | Home page of each persona (FR-CR-010); notifications (FR-CR-020) | Each role sees its work; approvals notified; every change logged | FR-CR-010, FR-CR-020, FR-CR-030 |
| J5.2 | Business Administrator, System Administrator | Capability 21 Report Generation: standard, customised and scheduled reports | Report Centre (FR-CR-040 to 043) | Reports in Excel, PDF and Word, saved as variants and scheduled | FR-CR-040, FR-CR-042, FR-CR-043 |
| J5.3 | Business Administrator, System Administrator | Capability 18 Emerging Capabilities | Not defined (CLR-CR-09) | Added when BDOI lists them | - |

