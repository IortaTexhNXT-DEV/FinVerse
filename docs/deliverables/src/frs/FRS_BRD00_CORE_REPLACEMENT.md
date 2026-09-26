---
# Source of the umbrella Functional Requirements Specification for the BDOI Core Replacement BRD.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD00_CORE_REPLACEMENT.md
title: Core Replacement
subtitle: Umbrella FRS for the BDOI Core Replacement BRD (21 key capabilities)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-00
name: Core Replacement
doc_id: BIBS-FRS-BRD-00
version: "1.0"
date: 26 September 2026
status: Issued for BDOI review
header_title: FRS BRD-00 Core Replacement
output: FRS/BIBS_FRS_BRD-00_Core_Replacement_v1.0.docx
control:
  - version: "0.9"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the Core Replacement BRD, the twelve BRD specs and the code as built
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue for BDOI review
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: Review and sign-off of the umbrella scope}
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review of the finance and data-management requirements}
  - {name: "Head, Operations", role: Approver, organisation: BDOI, purpose: Review of the Operations and reporting requirements}
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review of the front-office and dashboard requirements}
  - {name: "Unit Head, Claims, Analytics, Risk Management, Reinsurance and Technical Underwriting", role: Reviewer, organisation: BDOI, purpose: Claims and Reinsurance (phase 2) scope}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) is the umbrella FRS of BIBS (BDOI Broker System, on iNXT BrokerVerse). It answers the BDOI Core Replacement BRD, which summarises all BDOI functions as 21 key capabilities and 209 business requirements (BR-000 to BR-208) and refers to the function BRDs for the details.

The document does three things:

- it maps every capability and bullet of the umbrella BRD to the BRD and FRS that specify it (section 3.2 and the traceability in section 10);
- it states the **cross-cutting functional requirements** that no function BRD owns: navigation and persona menus, dashboards, notifications, audit, report generation, document generation, data management, security and integration (section 4);
- it lists the gaps of the umbrella BRD, with a proposed FR or an open question for each (section 5).

The function FRS (BRD-1 to BRD-12) remain the specifications of the business processes. Where this document and a function FRS describe the same behaviour, the function FRS governs.

## Scope

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Platform scope and journey | The end-to-end flow of the umbrella BRD and the BIBS BRD of each step | p.3, p.26 |
| Capability map | 21 capabilities, 184 bullets and 8 items of the capability matrix, each mapped to a BRD, an FRS and a fit class | p.6-12, p.26-29 |
| Cross-cutting requirements | Navigation, dashboards, notifications, audit, reports, documents, data management, security, integration, Invoice Master List | p.4-5, BR-000-004, 027, 051, 063, 083, 088, 101, 125, 152, 160, 170-177 |
| Gaps | Requirements with no owning BRD or in conflict with their BRD | Section 5 |
| Non-functional requirements | Users, response time, volumes, retention (p.42-46) against the function BRDs and the hosting appendix | Section 7 |

**Out of scope for phase 1:**

- Reinsurance (capability 15). The ReInsurance BRD is received and is **phase 2** (section 6).
- Emerging capabilities (capability 18, BR-176): not defined (CRQ10).
- The detailed process requirements of each function; they are in the function FRS.

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | BDOI Core Replacement BRD (48 pages) | v1.0, approved 21-Nov to 1-Dec-2025 | `docs/source-documents/00 - BRD BDOI Core Replacement v01.pdf` |
| R2 | Core Replacement requirements baseline and fit/gap (rows CORE-nn.mm, XC-nn; questions CRQnn) | current | `docs/requirements/BDOI_CORE_BRD_SPEC.md` |
| R3 | Core Replacement impact on BIBS (design proposals, build waves) | current | `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| R4 | FRS BRD-1 to BRD-12 (BRD-5 in two volumes) | v1.0 | `docs/deliverables/out/FRS/` |
| R5 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R6 | BRD discrepancy and clarification register | v1.0 | `docs/deliverables/out/Registers/` |
| R7 | ReInsurance BRD (phase 2) | 11-Mar-2025 | `docs/source-documents/ReInsurance (Phase 2).PDF` |
| R8 | Data Migration BRD | draft v0.01 | `docs/source-documents/BRD - Data Migration - draft V0.01.pdf` |
| R9 | BDO UX guidelines and BDOI UX design | current | `docs/design/BDO_UX_GUIDELINES.md`; `docs/source-documents/BDOI_UXD.docx` |

Page references ("p.14") are pages of the umbrella BRD (R1).

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

Each FR in section 4 has a header with the **BRD trace** (BR ID and page of the umbrella), the actor, the priority (every BR row is "Must have"), the **fit** class and the screens and API. The fit class is measured against BIBS as built:

<!-- table: widths=2.6,14 caption="Fit classes" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today |
| CONFIGURE | Needs set-up only (parameters, templates, rules) |
| CHANGE | Extends an existing capability |
| NEW | A capability that does not exist yet |
| OUT | Not in phase 1 |

Validations list the message the user sees and its code. A "-" marks a screen check without a business code. Values marked "default" are placeholders that BDOI confirms through the open questions in section 9.3; they are configuration.

# Platform scope and journey

## Business context

BDOI is a non-life insurance and reinsurance broker. The umbrella BRD sets the goal of one integrated platform from client onboarding to claims and accounting, for retail and wholesale business, meeting BIR and Insurance Commission reporting (p.4). Today intake, tagging, tracking and reconciliation run on spreadsheets and e-mail, cashiering, remittance and ACSL use separate systems, and reports depend on IT (p.4-5). The envisioned process has automated workflows, centralised dashboards for all roles, role-based self-service, system-generated and customised reports, audit logs and automated notifications (p.5).

## End-to-end journey

Figure 1 shows the flow of the umbrella BRD (p.26) with the BIBS BRD of each step. The left column is the new-business and money flow; the right column holds the lifecycle services and the functions that feed it (dashed arrows); the bar at the bottom lists the platform services that every step uses.

![End-to-end journey and the BIBS BRD of each step (amber = designed or being built; dashed box = phase 2)](figures/brd00_journey.dot){width=16}

<!-- table: widths=3.6,4.6,2.4,6 caption="Umbrella BRD list (p.3) and BIBS" status=Status size=8.5 -->
| Umbrella BRD (p.3) | BIBS BRD and FRS | Status | Note |
|---|---|---|---|
| 1 New Business | BRD-1, FR-NB | BUILT | Capabilities 1-5 |
| 2 Renewal | BRD-6, FR-RN | OPEN | Designed, not built |
| 3 Collection Management | BRD-4, FR-CL (BRD-4) | BUILT | Capability 7 |
| 4 Accounting, Disbursement and ACSL | BRD-5, FR-AC / FR-DS / FR-AS | BUILT | Capability 8 |
| 5 Claims | BRD-7, FR-CM (BRD-7) | IN PROGRESS | Claim recording and insurer updates merged |
| 6 Operations | BRD-2, FR-OP | BUILT | Capabilities 9-13 |
| 7 Reinsurance | ReInsurance BRD | PARKED | Phase 2 - BRD received |
| 8 Customer Service Facility | BRD-9, FR-CSF | OPEN | Designed, not built |
| 9 Product Maintenance | BRD-3, FR-PM | BUILT | Capability 20 |
| 10 Employee Benefits | BRD-8, FR-EB | OPEN | Designed, not built |
| Not listed | BRD-10 Sanction Screening, FR-SS | BUILT | Screening of clients (capability 1) |
| Not listed | BRD-11 User Access Maintenance, FR-UA | BUILT | BR-000 to BR-003 |
| Not listed | BRD-12 Submitted Policies, FR-SP | OPEN | Designed, not built |
| Not listed | Data Migration BRD | OPEN | Draft; client migration volumes (p.43) |

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

<!-- table: widths=3.7,1.1,0.8,1.8,1.5,1.1,0.9,2.6,2.2 caption="Umbrella capabilities, fit and coverage" size=8 -->
| Capability | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT | Covered by | BRD status |
|---|---|---|---|---|---|---|---|---|
| CORE-01 Client Onboarding | 7 | 7 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-02 Quotation or Proposal | 9 | 9 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-03 Account Creation and Maintenance | 6 | 6 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-04 Non-Package Management | 24 | 24 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-05 Placement and Booking | 8 | 8 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-06 Renewal | 9 | 0 | 0 | 3 | 6 | 0 | BRD-6 | Designed |
| CORE-07 Marketing Collection | 6 | 6 | 0 | 0 | 0 | 0 | BRD-4 | Built |
| CORE-08 Accounting / GL / Disbursement / ACSL | 22 | 22 | 0 | 0 | 0 | 0 | BRD-5, BRD-2, BRD-1 | Built |
| CORE-09 Cashiering (Payments Acceptance and Application) | 12 | 12 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-10 Remittance | 7 | 7 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-11 Production Reconciliation | 5 | 5 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-12 Adjustment / Cancellation | 7 | 7 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-13 Collection of Commission Receivables (Direct Payment) | 8 | 8 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-14 Claims | 13 | 0 | 0 | 0 | 13 | 0 | BRD-7 | Being built |
| CORE-15 Reinsurance | 10 | 0 | 0 | 0 | 0 | 10 | ReInsurance BRD | Phase 2 |
| CORE-16 Customer Service Facility | 6 | 0 | 0 | 4 | 1 | 1 | BRD-9 | Designed |
| CORE-17 Data Management | 5 | 2 | 0 | 2 | 1 | 0 | none, BRD-1, BRD-3 | Built, No BRD |
| CORE-18 Emerging Capabilities | 1 | 0 | 0 | 0 | 0 | 1 | none | No BRD |
| CORE-19 Employee Benefits | 12 | 0 | 0 | 4 | 8 | 0 | BRD-8 | Designed |
| CORE-20 Product Maintenance | 10 | 9 | 1 | 0 | 0 | 0 | BRD-3 | Built |
| CORE-21 Report Generation | 5 | 3 | 0 | 2 | 0 | 0 | BRD-1, none | Built, No BRD |

The row-level map (every bullet, BR ID, BRD requirement ID and FR) is in section 10. Totals of the 192 capability rows and 23 cross-cutting rows:

<!-- table: widths=2.4,6.4,2.4,2.4,2 caption="Rows per fit class" status=Fit -->
| Fit | Meaning | Capability rows | Cross-cutting rows | Total |
|---|---|---|---|---|
| FIT | Works today | 135 | 12 | 147 |
| CONFIGURE | Set-up only | 1 | 2 | 3 |
| CHANGE | Extends an existing capability | 15 | 6 | 21 |
| NEW | New build (in BIBS, or in a designed BRD not yet built) | 29 | 1 | 30 |
| OUT | Out of phase 1 scope | 12 | 2 | 14 |
| **Total** | | **192** | **23** | **215** |

# Cross-cutting functional requirements

## Navigation and persona menus

```fr
id: FR-CR-001
title: Show each persona only its menus and screens
brd: [BR-002 (p.13), BR-181 (p.23), BR-182 (p.23), "p.4 benefit 4a"]
actor: Every user
priority: Must have
fit: FIT
screens: Sidebar menu (groups Home, Client & Policy, Operations, Finance, Claims & Insurance, Reports, Setup & Administration)
api: Every endpoint checks its permission (@PreAuthorize)
description:
  - The sidebar is built from the modules of BIBS. Each screen names the permission that opens it; a user sees only the screens that the permissions of his roles allow, in seven groups in a fixed order.
  - A user may hold several roles (decision D5); the menu is the union of the roles.
  - Insurer-side modules of the platform (underwriting, insurer claims, treaty reinsurance, reserves) are not granted to BDOI roles.
preconditions:
  - The user is signed in.
main_flow:
  - BIBS reads the permissions of the user's roles.
  - BIBS shows the menu groups and screens those permissions open.
  - The user opens a screen; BIBS checks the permission again on every call.
alternate_flows:
  - Screen opened by URL without permission. BIBS shows "You do not have access to this page" and the API refuses the call (HTTP 403).
rules:
  - [R1, "Roles and their permissions are changed only through an approved User Access request (BRD-11).", Configurable, User Access requests]
  - [R2, "A screen without a permission of the user is never shown, even if the user knows its address.", Fixed, "-"]
validations:
  - [Screen without permission, You do not have access to this page, "-"]
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
fit: CHANGE
screens: Role Home (/); Role Home Set-up (Setup & Administration)
api: GET /api/v1/dashboard/role-home; PUT /api/v1/dashboard/role-home/config/{role}
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
fit: CHANGE
screens: Role Home; the work list or report of the widget
api: Widget link (route with filters)
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
  - [No permission on the target screen, You do not have access to this page, "-"]
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
fit: FIT
screens: Notification bell; My Work; Notification preferences (profile)
api: GET /api/v1/notifications; messaging outbox
description:
  - Workflow steps, approvals, returns, SLA breaches and job failures create in-system notifications for the users or roles concerned; selected events are also e-mailed through the outbox (for example remittance schedules and confirmations, BR-083).
  - Users choose in their profile which notifications they also receive by e-mail, within the events their roles allow.
preconditions:
  - The event is defined in the notification set of its module.
main_flow:
  - A business event occurs (for example a request is submitted for approval).
  - BIBS creates a notification for the assigned user or the holders of the approving permission.
  - BIBS queues the e-mail when the event and the user's preference require it; the dispatch job sends it and logs the result.
alternate_flows:
  - E-mail delivery fails. The outbox retries and the failure is shown in the message log; the in-system notification stays.
rules:
  - [R1, "Notification events per module are those of the function FRS; the per-role catalogue is part of deliverable 15.", Configurable, Notification preferences and module parameters]
  - [R2, "Outbound documents that leave BDOI are password protected (FR-CR-071).", Fixed, "-"]
validations:
  - [E-mail address invalid, Enter a valid e-mail address, "-"]
notifications:
  - This FR is the notification service itself.
audit:
  - Every e-mail attempt is logged in the outbox with recipients, time and result.
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
fit: FIT
screens: Audit Trail (Setup & Administration); History tabs of the records
api: GET /api/v1/audit
description:
  - Every create, update, approval, rejection, posting, reversal, export, log-in and log-out is recorded with user, time, record and a summary. Records show their history on a History tab; the Audit Trail screen searches all of it and exports it.
preconditions:
  - None.
main_flow:
  - A user performs an action.
  - BIBS records the audit entry in the same transaction as the change.
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
  - An audit entry cannot be edited through any screen or API.
```

```fr
id: FR-CR-031
title: Log every change to master data with old and new values
brd: [BR-170 (p.22), "Capability 17 bullet 1 (p.11)"]
actor: System; Business Administrator, auditors (view)
priority: Must have
fit: CHANGE
screens: Master Data Change Log (Setup & Administration)
api: GET /api/v1/admin/master-changes; report ADM-MASTER-CHANGES
description:
  - For registered master data (users and roles, products and versions, insurers and branches, commission rates, LOVs, chart of accounts, payees, parameters, role home and MIS field set-up), BIBS records each changed field with its old value, new value, user, time and the approval reference when the change was approved.
  - The log complements the audit trail (FR-CR-030), which records the action but not the field values.
preconditions:
  - The master entity is registered in the change-log list.
main_flow:
  - A user changes a master record (directly or through an approved request).
  - BIBS writes one log line per changed field in the same transaction.
  - An authorised user opens the Master Data Change Log, filters by master type, record, user and date, and exports it.
alternate_flows:
  - Change rejected by the approver. Nothing is logged as changed; the rejection is in the audit trail.
rules:
  - [R1, "Registered master types are listed in section 9 (default); BDOI confirms the list (CRQ07).", Configurable, Change-log registration]
  - [R2, "Values of secret fields (passwords, tokens) are never logged; the log shows 'changed'.", Fixed, "-"]
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
fit: FIT
screens: Reports (catalogue); report runner
api: GET /api/v1/reports; POST /api/v1/reports/{code}/run; POST /api/v1/reports/{code}/export
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
fit: FIT
screens: Report runner (Variants)
api: GET/POST/DELETE /api/v1/nb/reports/variants
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
fit: CHANGE
screens: Report runner (Layout panel)
api: Variant layout (extension of the variant API)
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
fit: CHANGE
screens: Report runner (Schedule); My Subscriptions (Reports)
api: GET/POST/PUT/DELETE /api/v1/reports/subscriptions
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
fit: FIT
screens: Templates (Broking Setup); document downloads on each record
api: Document template service; downloads per module
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
fit: FIT
screens: Lists of Values (Broking Setup)
api: /api/v1/lov
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
fit: CHANGE
screens: Insurer page (Catalog > Insurers), tabs Profile, Branches and LGT, Contacts, Products and Packages, Commission Rates, Payment Terms and Remittance, Risk Participation, Accounts
api: /api/v1/catalog/insurers/{code} and sub-resources
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
fit: NEW
screens: MIS Fields (Broking Setup); report runner filters and Layout panel
api: /api/v1/admin/mis-fields
description:
  - A catalogue names each MIS field per core entity (user, product, insurer, claim, policy / account, transaction) with its business label, data type, list of values and the data it reads. It is delivered with the fields of BR-173 (product type, risk code, market segment, booking date, policy number, insurer, branch, account officer, status, transaction type, chart of account) and the MIS columns of the Report List (region, area, unit head, department, business origin).
  - Reports, dashboard widgets and exports offer the active MIS fields of their entity for filtering, grouping and export, with the same labels everywhere.
preconditions:
  - The field's data exists in BIBS; the catalogue does not create database columns (CRQ09).
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
fit: FIT
screens: Catalog; Package Requests (Product Maintenance)
api: /api/v1/catalog; /api/v1/product-maintenance
description:
  - Product and package maintenance is specified by FRS BRD-3 (FR-PM-001 to FR-PM-081). The umbrella adds no requirement; its capability 20 bullets are traced in section 10.
preconditions:
  - None.
main_flow:
  - As FRS BRD-3.
alternate_flows:
  - As FRS BRD-3.
rules:
  - [R1, "Synchronisation with other BDOI systems (BR-206) waits for the target systems (PQ16).", Configurable, ProductMasterFeed adapter]
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
fit: FIT
screens: Login; Reset password; User Access requests
api: POST /api/v1/auth/login; password reset link; /api/v1/nbadmin/access-requests
description:
  - Users sign in with user ID and password (directory sign-in is a parked port until BDO supplies the interface, decision D6). A user who forgot the password receives a reset link. Users are internal or external (portal) and hold one or more roles.
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
  - [Wrong credentials, Invalid user ID or password, "-"]
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
fit: FIT
screens: Send e-mail dialogs of the records
api: messaging outbox
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
fit: CONFIGURE
screens: My Work; workflow panels of the records
api: Workflow service; integration events
description:
  - Each business record moves through stages defined as data (stages, transitions, roles, SLA). A completed step places the record in the next queue and notifies it; other modules receive business events (for example invoice booked, receipt issued, client changed) through the event outbox.
  - Files for other BDOI systems are handed to ports; their transport is configured when BDOI names the systems (Q08, PQ16).
preconditions:
  - The workflow of the record type is defined.
main_flow:
  - A user completes a step.
  - BIBS applies the transition, assigns the next queue and publishes the business event.
alternate_flows:
  - Receiving system unavailable. The event stays in the outbox and is retried; failures go to the dead-letter log for support.
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
fit: CHANGE
screens: Insurer Invoice Batches (Client & Policy > Booking)
api: /api/v1/booking/insurer-batches
description:
  - Today each service invoice is e-mailed to the insurer. A daily job (after booking closes, default 20:00) also builds for each insurer one batch of that day's service invoices (merged PDF, or ZIP with a manifest), stores it and delivers it through the insurer's configured channel. The SFTP channel is added when BDOI supplies the endpoints (CRQ18). A delivery report lists each batch, invoice count, amount, channel and result.
preconditions:
  - Service invoices were issued for the insurer that day.
main_flow:
  - The job groups the day's service invoices by insurer.
  - BIBS builds and stores the batch and its manifest.
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
fit: NEW
screens: Bulk uploads (CLIENT_CREATE); client modification report
api: Bulk upload; legacy client feed port
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
fit: FIT
screens: Bulk uploads; batch job monitor
api: /api/v1/bulk
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
fit: CHANGE
screens: Operations > Invoice Ledger, view "Invoice Master List"
api: /api/v1/ops/invoices
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
fit: FIT
screens: Account; reports with a currency parameter
api: Account and report APIs
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
brd: [BR-178 (p.23), BR-179 (p.23), BR-180 (p.23), BR-181 (p.23)]
actor: Every user
priority: Must have
fit: CHANGE
screens: All screens and documents
api: "-"
description:
  - Screens use the BDO Insure colours (Header Blue, CTA Blue, Yellow), logo and typography of the UX guidelines; documents, reports and exports carry the logo, blue table headers and the "Confidential" footer. A screen-by-screen alignment pass against the BDOI UX design (deliverable 18) is made before FRS v1.1.
  - BDO's prescribed icons and illustrations are used when BDOI supplies them (CRQ17). The persona journeys are demonstrated with the end-to-end persona deck (deliverable 7).
preconditions:
  - None.
main_flow:
  - The project team reviews each screen against the BDOI UX design and records the deviations.
  - Each deviation is corrected before the annotated screenshots of FRS v1.1 are taken.
alternate_flows:
  - Deviation that needs a BDOI decision (for example an icon not yet supplied). It is logged in the register and closed after CRQ17.
rules:
  - [R1, "Brand tokens are defined once and used by screens, documents and exports.", Fixed, "-"]
validations:
  - [None, "-", "-"]
notifications:
  - None.
audit:
  - None.
acceptance:
  - The alignment review finds no screen outside the BDO colour and layout rules.
```

```fr
id: FR-CR-093
title: Issue the SOA and the insurer's service invoice at booking
brd: [BR-039 (p.14), BR-043 (p.15)]
actor: Processing
priority: Must have
fit: CHANGE
screens: Booking; Service Invoices; Billing Statements (Collections)
api: Booking and billing APIs
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

# Gaps and proposed requirements

<!-- table: widths=0.9,4,2.5,5.6,2.1,1.9 caption="Gaps of the umbrella BRD" status=Fit size=8 -->
| # | Gap | Rows | Proposal | FR | Fit |
|---|---|---|---|---|---|
| G1 | Reinsurance | CORE-15.01-15.10 | Phase 2 - BRD received; seams kept open (section 6) | - | OUT |
| G2 | Claims cheque safekeeping and hand-over to / from Cashiering | CORE-14.08-14.10 | Parked until CRQ03 (with CLQ10); if yes, a cheque custody register in Claims | - | NEW |
| G3 | CSF case resolution | CORE-16.06 | Out until CRQ04; BRD-9 keeps case logging in SharePoint | - | OUT |
| G4 | Master data change log | CORE-17.01 | Field-level log of registered master data | FR-CR-031 | CHANGE |
| G5 | Insurer management | CORE-17.02 | Insurer page with the missing attributes | FR-CR-061 | CHANGE |
| G6 | MIS field definition | CORE-17.04 | MIS field catalogue | FR-CR-062 | NEW |
| G7 | Report customisation | CORE-21.04 | Layout in the variant | FR-CR-042 | CHANGE |
| G8 | Scheduled reports | CORE-21.05 | Report subscriptions | FR-CR-043 | CHANGE |
| G9 | Emerging capabilities | CORE-18.01 | No build until listed (CRQ10) | - | OUT |
| G10 | ALeA e-mail address encoding | XC-21 | No build until defined (CRQ20) | - | OUT |
| G11 | Role dashboards | XC-02 | Role home page | FR-CR-010, 011 | CHANGE |
| G12 | Invoice Master List | XC-13 | Columns added to the invoice ledger view | FR-CR-090 | CHANGE |
| G13 | Insurer invoice batch and SFTP | XC-19 | Daily batch per insurer | FR-CR-081 | CHANGE |
| G14 | Client migration and daily client batches | XC-20 | Load and feed through the Data Migration programme | FR-CR-082 | NEW |
| G15 | SOA at booking | XC-10 | After CRQ12 | FR-CR-093 | CHANGE |

# Reinsurance (phase 2)

Capability 15 (6 bullets, BR-153 to BR-160) and 4 more items of the capability matrix (p.28) belong to the ReInsurance BRD. That BRD is received and is **phase 2**: it is not built in phase 1 and its rows are marked "Phase 2" in section 10. The insurer-side reinsurance module of the platform (treaties and cessions of an insurance company) is not the broker model BDOI needs and stays hidden from BDOI roles.

Phase 1 keeps these seams open so that the phase 2 module can reuse them: party types for reinsurers and RI brokers; template-driven placement slips and SOAs; the report subscriptions of FR-CR-043 for SOA scheduling; open-item matching for net settlement; accounting event types added without changing existing posting rules; and a claim party model open to reinsurers. The design notes are in R3, section 6.

# Non-functional requirements

<!-- table: widths=2.8,4.6,4.8,3.4,1.6 caption="Non-functional requirements of the umbrella (p.42-46)" status=Status size=8 -->
| Topic | Umbrella value | Function BRDs | BIBS target and approach | Status |
|---|---|---|---|---|
| Users | 1,344 named / 429 concurrent (sum of the BRD rows; 388 without Reinsurance) | 150 concurrent + 20% a year (register proposal); BRD-8 20 internal; BRD-7 47 / 25 | Performance tests at 429 concurrent sessions as the peak case (CRQ21, CRQ24) | OPEN |
| Response time | Under 5 seconds for every role | 2 s (BRD-12) to 10 s (BRD-1, BRD-6, BRD-11); reports 20 s to 15 min | p95 under 2 s for screens; reports and batches as jobs with progress (CRQ21) | OPEN |
| Volumes | Per BRD (p.42-45), e.g. NB 21,200 a month per transaction type, RMEL 25,800, CSF 144,400 a year, CMS reports 60,000 weekly | Same values in the BRDs | Sized in the performance plan (deliverable 28) | FIT |
| Peak, availability, maintenance, BCP | "Refer to BRD" | 99.9%-99.99%; windows differ | Register proposal: 99.9% in service hours 06:00-22:00 Mon-Sat, maintenance 00:00-04:00, RTO 4 h | OPEN |
| Retention | Application, database and audit logs, historical data: 5 years online, 15 years offline | BRD-7 10 / 15; BRD-5, 10, 12 5 / 5 | Default 5 / 15 per record type through the retention rules; exceptions after CRQ22 | PARTIAL |
| Backup | Every 4 hours, kept 5 years | Daily to every 15 minutes; kept 5 or 7 years | Continuous log archiving plus a base backup every 4 hours, kept 7 years | FIT |
| Anonymisation | No | - | Production data not anonymised; non-production data masked (hosting appendix) | FIT |
| Hosting and access | - | - | AWS ap-southeast-1; access restricted to personnel in the Philippines; migration staging purged within 5 days | FIT |
| Security and audit | Role-based access; password protection; audit logs (p.4-5) | Per BRD | FR-CR-001, 030, 031, 070, 071 | FIT |

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
| A-CR-05 | BR-178 to BR-183 are vendor-qualification items; they are met by the design system, the alignment pass and the persona demonstrations | XC-14 to XC-16 |
| A-CR-06 | The MIS field catalogue names existing data; users do not add database fields | CRQ09 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-CR-01 | BDOI defines the dashboard contents per role | FR-CR-010, 011 (CRQ06) |
| D-CR-02 | BDOI confirms the master data in the change log and the insurer attributes | FR-CR-031, 061 (CRQ07, CRQ08) |
| D-CR-03 | BDOI supplies the SFTP endpoints of the insurers | FR-CR-081 (CRQ18) |
| D-CR-04 | The Data Migration BRD decides the client migration scope and coexistence | FR-CR-082 (CRQ19) |
| D-CR-05 | BDOI supplies the icon set and design files | FR-CR-092 (CRQ17) |
| D-CR-06 | Builds of Renewal, Claims, Employee Benefits, CSF and Submitted Policies complete the rows marked Designed or Being built | Section 10 |

## Open questions

<!-- table: widths=1.4,10.9,2.6,1.7 caption="Open questions on the umbrella BRD" status=Status size=8 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| CRQ01 | Is the umbrella over all twelve BRDs, Data Migration and ReInsurance? Who owns Data Management (capability 17)? | Scope | OPEN |
| CRQ02 | Is Non-Package Management the NB non-package placement (no BR ID, added 20-Nov-2025)? | CORE-04 | OPEN |
| CRQ03 | Does BDOI hold claim settlement cheques (safekeeping, hand-over to and retrieval from Cashiering, BR-146-148)? | G2 | OPEN |
| CRQ04 | Is CSF case resolution (BR-165) in phase 1, given BRD-9 keeps it in SharePoint? | G3 | OPEN |
| CRQ05 | Is deactivation acceptable for "delete of prospect" (BR-008)? | A-CR-03 | OPEN |
| CRQ06 | Which figures does each role see on its home page (BR-125, 152, 160)? | FR-CR-010 | OPEN |
| CRQ07 | Which master data is in the change log; old and new values; approver; who views it? | FR-CR-031 | OPEN |
| CRQ08 | Meaning of risk participation, payment terms and remittance schedules on the insurer; owners and approval | FR-CR-061 | OPEN |
| CRQ09 | Is MIS field definition a fixed list per entity or user-defined fields? | FR-CR-062 | OPEN |
| CRQ10 | List the emerging capabilities or withdraw BR-176 | G9 | OPEN |
| CRQ11 | Which reports need custom layouts and schedules; formats and delivery; Word for all reports? | FR-CR-042, 043 | OPEN |
| CRQ12 | Which SOA is issued with the placement report and at booking (BR-039, BR-043)? | FR-CR-093 | OPEN |
| CRQ13 | Is the Invoice Master List the Operations invoice ledger; columns and owner? | FR-CR-090 | OPEN |
| CRQ14 | Letter name: Non-Renewal Letter (NRL) or Not for Renewal Letter (NFR)? | CORE-06.07 | OPEN |
| CRQ15 | What does "Bank Account Operations" cover? | CORE-08.21 | OPEN |
| CRQ16 | Do CSF agents also resend e-policies (BR-164)? | CORE-16.04 | OPEN |
| CRQ17 | Supply the BDO icon set and design or Figma files | FR-CR-092 | OPEN |
| CRQ18 | Insurer invoice batch: file format, SFTP endpoints, replaces e-mail? | FR-CR-081 | OPEN |
| CRQ19 | Client migration source, period (2020 to present) and duration of daily batches | FR-CR-082 | OPEN |
| CRQ20 | What is ALeA and its "e-mail address encoding"? | G10 | OPEN |
| CRQ21 | Response under 5 s for every role and 429 concurrent users: which values govern? | Section 7 | OPEN |
| CRQ22 | Do the umbrella retention and backup values replace the BRD-specific ones? | Section 7 | OPEN |
| CRQ23 | Send "E2E BDOI Mapping.xlsx"; confirm the final IDs of each BRD govern | Section 10 | OPEN |
| CRQ24 | User counts of EB, Claims and Product Maintenance differ from their BRDs | Section 7 | OPEN |
| CRQ25 | Is the umbrella approved (one approver on leave without signature; one "not applicable"; four by e-mail)? | Sign-off | OPEN |

<!-- landscape -->

# Traceability

Every capability bullet and cross-cutting row of the umbrella BRD, the BRD that specifies it and the FR that meets it. "Covered by" gives the BIBS BRD and its requirement IDs; FR IDs without a prefix note belong to the FRS of that BRD. FR-CL-nnn are the FRs of BRD-4 Collections; BRD-7 Claims numbers its FRs FR-CM-nnn (renumbered from FR-CL-nnn, DCR-188). Reinsurance rows are "Phase 2".

<!-- table: widths=1.7,6.2,2.4,5.4,4.4,2.0 caption="Traceability of the umbrella BRD" status=Fit size=7 -->
| ID | Capability (Core BRD page) | Core BR | Covered by | FR | Fit |
|---|---|---|---|---|---|
| CORE-01.01 | Create prospect / record (p.6) | BR-005 | BRD-1 BRNB.090, BRNB.101, BRNB.048 | FR-NB-034, FR-NB-031 | FIT |
| CORE-01.02 | Update prospect / record (p.6) | BR-006 | BRD-1 BRNB.049, BRNB.047 | FR-NB-032, FR-NB-035 | FIT |
| CORE-01.03 | Convert prospect into client record (p.6) | BR-007 | BRD-1 BRNB.090, BRNB.101 | FR-NB-034 | FIT |
| CORE-01.04 | Delete prospect / record (p.6) | BR-008 | BRD-1 BRNB.019, BRNB.106 | FR-NB-012, FR-NB-137 | FIT |
| CORE-01.05 | Upload and validate client documents (p.6) | BR-009, BR-010, BR-011 | BRD-1 BRNB.030, BRNB.049, BRNB.026 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 ... | FIT |
| CORE-01.06 | Client search (p.6) | BR-012 | BRD-1 BRNB.046 | FR-NB-030 | FIT |
| CORE-01.07 | Generate client code (p.6) | BR-013 | BRD-1 BRNB.030, BRNB.101 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | FIT |
| CORE-02.01 | Receive request for quotation / proposal (p.6) | BR-014 | BRD-1 BRNB.041, BRNB.023 | FR-NB-040 | FIT |
| CORE-02.02 | Create quotation / proposal (individual or bulk) (p.6) | BR-015, BR-016 | BRD-1 BRNB.043, BRNB.042, BRNB.028 | FR-NB-041, FR-NB-044, FR-NB-046 | FIT |
| CORE-02.03 | Track proposal status (p.6) | BR-017 | BRD-1 BRNB.022, BRNB.115 | FR-NB-010, FR-NB-122, FR-NB-120 | FIT |
| CORE-02.04 | Edit quotation / proposal (p.6) | BR-018 | BRD-1 BRNB.020 | FR-NB-042 | FIT |
| CORE-02.05 | Approve quotation / proposal (p.6) | BR-019 | BRD-1 BRNB.021, BRNB.014 | FR-NB-043, FR-NB-014, FR-NB-051 | FIT |
| CORE-02.06 | Print quotation / proposal (p.6) | BR-020 | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 | FIT |
| CORE-02.07 | Upload documents (p.6) | BR-021 | BRD-1 BRNB.055, BRNB.026 | FR-NB-017 | FIT |
| CORE-02.08 | Send quotation / proposal (p.6) | BR-022 | BRD-1 BRNB.043, BRNB.042 | FR-NB-041, FR-NB-044, FR-NB-046 | FIT |
| CORE-02.09 | Generate and customise report (p.6) | BR-053 | BRD-1 BRNB.057, BRNB.075 | FR-NB-123, FR-NB-122 | FIT |
| CORE-03.01 | Create account (manual, bulk upload, system) (p.6) | BR-023, BR-024, BR-025, BR-028 | BRD-1 BRNB.051, BRNB.066, BRNB.039 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 ... | FIT |
| CORE-03.02 | Update account (p.6) | BR-029 | BRD-1 BRNB.025, BRNB.053, BRNB.054 | FR-NB-064, FR-NB-065 | FIT |
| CORE-03.03 | Link to client record (p.6) | BR-030 | BRD-1 BRNB.051, BRNB.099 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-037 | FIT |
| CORE-03.04 | Approve account (p.6) | BR-031 | BRD-1 BRNB.022, BRNB.079 | FR-NB-010, FR-NB-122, FR-NB-133 | FIT |
| CORE-03.05 | Initiate placement request (p.6) | BR-033 | BRD-1 BRNB.069 | FR-NB-080 | FIT |
| CORE-03.06 | Client confirmation (capability matrix p.26) | BR-038 | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 | FIT |
| CORE-04.01 | Create proposal request form (PRF) (p.7) | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 | FIT |
| CORE-04.02 | Approve PRF (p.7) | - | BRD-1 BRNB.005, BRNB.014 | FR-NB-014, FR-NB-050, FR-NB-051, FR-NB-043 | FIT |
| CORE-04.03 | Submit PRF (p.7) | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 | FIT |
| CORE-04.04 | Receive PRF (TSU) (p.7) | - | BRD-1 BRNB.007 | FR-NB-052 | FIT |
| CORE-04.05 | Edit PRF (p.7) | - | BRD-1 BRNB.007 | FR-NB-052 | FIT |
| CORE-04.06 | Create Quotation Slip (p.7) | - | BRD-1 BRNB.008 | FR-NB-053 | FIT |
| CORE-04.07 | Send Quotation Slip to insurers (p.7) | - | BRD-1 BRNB.008 | FR-NB-053 | FIT |
| CORE-04.08 | Input insurers' feedback in the comparative table (p.7) | - | BRD-1 BRNB.009 | FR-NB-054, FR-NB-055 | FIT |
| CORE-04.09 | Submit comparative table (p.7) | - | BRD-1 BRNB.010 | FR-NB-055 | FIT |
| CORE-04.10 | Finalise Proposal Slip (p.7) | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 | FIT |
| CORE-04.11 | Submit Proposal Slip (p.7) | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 | FIT |
| CORE-04.12 | Receive and print quotation (p.7) | - | BRD-1 BRNB.017, BRNB.043 | FR-NB-056, FR-NB-057, FR-NB-041, FR-NB-044 | FIT |
| CORE-04.13 | Send quotation (p.7) | - | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 | FIT |
| CORE-04.14 | Convert quotation to account (p.7) | - | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 | FIT |
| CORE-04.15 | Edit account (p.7) | - | BRD-1 BRNB.053 | FR-NB-064 | FIT |
| CORE-04.16 | Approve account (p.7) | - | BRD-1 BRNB.022 | FR-NB-010, FR-NB-122 | FIT |
| CORE-04.17 | Initiate policy placement request (p.7) | - | BRD-1 BRNB.069 | FR-NB-080 | FIT |
| CORE-04.18 | Track PRF / proposal placement slip / quotation / placement request status (p.7) | - | BRD-1 BRNB.012, BRNB.022, BRNB.115 | FR-NB-120, FR-NB-010, FR-NB-122 | FIT |
| CORE-04.19 | Upload documents (p.7) | - | BRD-1 BRNB.055 | FR-NB-017 | FIT |
| CORE-04.20 | Generate and customise report (p.7) | - | BRD-1 BRNB.011, BRNB.057 | FR-NB-121, FR-NB-123 | FIT |
| CORE-04.21 | Automated reference number generation (p.7) | - | BRD-1 BRNB.006 | FR-NB-047, FR-NB-050 | FIT |
| CORE-04.22 | Password protection and encryption of shared documents (p.7) | - | BRD-1 BRNB.013 | FR-NB-013, FR-NB-044, FR-NB-057 | FIT |
| CORE-04.23 | System notification on status changes (p.7) | - | BRD-1 BRNB.015 | FR-NB-015 | FIT |
| CORE-04.24 | Complete logging of all actions (p.7) | - | BRD-1 BRNB.016 | FR-NB-016 | FIT |
| CORE-05.01 | Submit for placement and booking (p.7) | BR-032 | BRD-1 BRNB.022, BRNB.096 | FR-NB-010, FR-NB-122, FR-NB-011, FR-NB-064 | FIT |
| CORE-05.02 | Generate placement slip (p.7) | BR-036 | BRD-1 BRNB.069 | FR-NB-080 | FIT |
| CORE-05.03 | Generate placement report (p.7) | BR-039 | BRD-1 BRNB.011, BRNB.075 | FR-NB-121, FR-NB-122 | FIT |
| CORE-05.04 | Send placement slip to insurer (p.7) | BR-037 | BRD-1 BRNB.071 | FR-NB-081 | FIT |
| CORE-05.05 | Generate Insurance Advice (p.7) | BR-039 | BRD-1 BRNB.070, BRNB.060 | FR-NB-103, FR-NB-104 | FIT |
| CORE-05.06 | Receive e-policy (individual or batch) (p.7) | BR-040 | BRD-1 BRNB.073 | FR-NB-100 | FIT |
| CORE-05.07 | Send e-policy to client (individual or batch) (p.7) | BR-041 | BRD-1 BRNB.077 | FR-NB-105 | FIT |
| CORE-05.08 | Update client record with the e-policy and policy number (p.7) | BR-042 | BRD-1 BRNB.074 | FR-NB-101 | FIT |
| CORE-06.01 | Generate RMEL (list of expiring accounts) (p.8) | BR-055 | BRD-6 BRRN.002, BRRN.030, BRRN.005 | FR-RN-011, FR-RN-010, FR-RN-112 | NEW |
| CORE-06.02 | Filter and distribute RMEL (p.8) | BR-056 | BRD-6 BRRN.003, BRRN.011, BRRN.036 | FR-RN-012, FR-RN-040, FR-RN-102 | CHANGE |
| CORE-06.03 | Rules-based sanitation of accounts (bulk / individual) (p.8) | BR-026, BR-057 | BRD-6 BRRN.020, BRRN.023, BRRN.009 | FR-RN-020, FR-RN-103, FR-RN-004, FR-RN-022 ... | NEW |
| CORE-06.04 | Provide disposition (online or by upload) (p.8) | BR-058, BR-059 | BRD-6 BRRN.031, BRRN.018 | FR-RN-004, FR-RN-023, FR-RN-051, FR-RN-103 ... | NEW |
| CORE-06.05 | Process and generate renewal proposal (p.8) | - | BRD-6 BRRN.033, BRRN.038 | FR-RN-048, FR-RN-064, FR-RN-084 | CHANGE |
| CORE-06.06 | Generate and send Renewal Advice (p.8) | BR-060, BR-061 | BRD-6 BRRN.010 | FR-RN-080, FR-RN-081, FR-RN-090 | NEW |
| CORE-06.07 | Send No Advice Letter (NAL) and Non-Renewal Letter (NRL) (p.8) | BR-060, BR-061 | BRD-6 BRRN.001, BRRN.009 | FR-RN-082, FR-RN-103, FR-RN-024, FR-RN-112 | NEW |
| CORE-06.08 | Check renewal payment (capability matrix p.27) | BR-062 | BRD-6 BRRN.027 | FR-RN-042 | CHANGE |
| CORE-06.09 | Track renewal status (BR table only) | BR-064 | BRD-6 BRRN.036 | FR-RN-102 | NEW |
| CORE-07.01 | Premium Receivable (PR) management (p.8) | BR-044, BR-045 | BRD-4 BRCLXN.001, BRCLXN.011, BRCLXN.046 | FR-CL-010, FR-CL-015, FR-CL-016 | FIT |
| CORE-07.02 | Disposition tracking and management (p.8) | BR-048 | BRD-4 BRCLXN.016, BRCLXN.021, BRCLXN.023 | FR-CL-030, FR-CL-031, FR-CL-018 | FIT |
| CORE-07.03 | Tag CWT, premium / PR2307 (p.8) | BR-118, BR-139 | BRD-4 BRCLXN.026, BRCLXN.027, CSHID.026 | FR-CL-032, FR-CL-081, FR-OP-026 | FIT |
| CORE-07.04 | Unapplied payment disposition (excess payment) (p.8) | BR-046, BR-047, BR-090 | BRD-4 BRCLXN.030, BRCLXN.034, BRCLXN.041 | FR-CL-074, FR-CL-070, FR-CL-077 | FIT |
| CORE-07.05 | Reporting and audit (p.8) | BR-053, BR-054 | BRD-4 BRCLXN.028, BRCLXN.043, BRCLXN.045 | FR-CL-082, FR-CL-003, FR-CL-083 | FIT |
| CORE-07.06 | Batch processing and automation (capability matrix p.27) | BR-049 | BRD-4 BRCLXN.013, BRCLXN.024, BRCLXN.041 | FR-CL-017, FR-CL-032, FR-CL-080, FR-CL-077 | FIT |
| CORE-08.01 | Daily financial report reconciliation (p.8) | BR-065 | BRD-5 ACSL 2.13.0, ACSL 2.13.2 | FR-AS-003, FR-AS-004 | FIT |
| CORE-08.02 | Daily cash movement reconciliation (p.8) | BR-067 | BRD-5 FRBS 3.3.0 | FR-AC-050 | FIT |
| CORE-08.03 | Insurer's statement of accounts (SOA) reconciliation (p.8) | BR-068 | BRD-5 ACSL 2.13.1, ACSL 2.14.1 | FR-AS-003 | FIT |
| CORE-08.04 | Generate automated journal entries (p.8) | BR-069 | BRD-5 ACSL 2.15.0, FRBS 3.1.0 | FR-AS-024, FR-AC-030 | FIT |
| CORE-08.05 | Perform manual entries (p.8) | BR-070 | BRD-5 FRBS 2.8.0, FRBS 2.8.5 | FR-AC-032 | FIT |
| CORE-08.06 | Perform manual / invoice adjustments (p.8) | BR-071 | BRD-5 ACSL 2.9.0, ACSL 2.9.1 | FR-AS-021 | FIT |
| CORE-08.07 | Perform accrual (p.8) | BR-072 | BRD-5 FRBS 2.8.1 | FR-AC-032, FR-AC-033 | FIT |
| CORE-08.08 | Perform revaluation (p.8) | BR-073 | BRD-5 FRBS 2.2.0, FRBS 3.5.0 | FR-AC-010, FR-AC-043 | FIT |
| CORE-08.09 | Perform month-end and year-end closing (p.8) | - | BRD-5 FRBS 3.4.0 | FR-AC-042 | FIT |
| CORE-08.10 | Generate financial reports (p.8) | BR-066 | BRD-5 FRBS 3.2.0, DIS 3.28.0 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 ... | FIT |
| CORE-08.11 | Accounts analysis (p.8) | BR-074 | BRD-5 ACSL 2.5.0, ACSL 2.5.3 | FR-AS-010 | FIT |
| CORE-08.12 | Receive and release CWT (commission / supplier) (p.8) | BR-075, BR-076 | BRD-5 DIS 2.11.0, DIS 2.12.0 | FR-DS-057, FR-DS-058 | FIT |
| CORE-08.13 | Disbursement to insurer, client, supplier, BDO subsidiaries (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | FIT |
| CORE-08.14 | Reporting and documentation (p.8) | BR-078, BR-084 | BRD-5 DIS 3.28.0, FRBS 3.2.0 | FR-DS-081, FR-AC-060, FR-AC-061, FR-AC-062 ... | FIT |
| CORE-08.15 | Release BIR 2307 on premiums (PR2307) to insurer (p.8) | BR-078 | BRD-2 CSHID.027 | FR-OP-026 | FIT |
| CORE-08.16 | Generate Direct Credit transactions file (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | FIT |
| CORE-08.17 | Disbursement to government agencies and employees (p.8) | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | FIT |
| CORE-08.18 | Payee management (p.8) | BR-079 | BRD-5 DIS 2.2.0 | FR-DS-010 | FIT |
| CORE-08.19 | Check printing and series management (p.8) | BR-080 | BRD-5 DIS 2.7.0 | FR-DS-033 | FIT |
| CORE-08.20 | Status tagging and tracking (p.8) | BR-081, BR-082 | BRD-5 DIS 2.8.0, DIS 3.26.0 | FR-DS-050, FR-DS-052 | FIT |
| CORE-08.21 | Bank account operations (p.9) | - | BRD-5 DIS 2.7.0 | FR-DS-033 | FIT |
| CORE-08.22 | Generate manual service invoice (Other Income) (p.9) | BR-085 | BRD-1 BRNB.100 | FR-NB-117 | FIT |
| CORE-09.01 | Manual issuance of AR, OR, invoice, cash and cheque OTC payment (p.9) | BR-086 | BRD-2 CSHID.001, CSHID.002 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-012 | FIT |
| CORE-09.02 | Automated accounting entries (p.9) | BR-087 | BRD-2 CSHID.012, CSHID.014 | FR-OP-013, FR-OP-027 | FIT |
| CORE-09.03 | Batch payment files processing (automatching) (p.9) | BR-089 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 | FIT |
| CORE-09.04 | Batch processing and automation (automatch re-run) (p.9) | BR-092 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 | FIT |
| CORE-09.05 | Batch reversal processing (p.9) | - | BRD-2 CSHID.012, CSHID.016 | FR-OP-013, FR-OP-027, FR-OP-023 | FIT |
| CORE-09.06 | Unapplied payment management (excess payment) (p.9) | BR-090, BR-093 | BRD-2 CSHID.024, CSHID.025 | FR-OP-022 | FIT |
| CORE-09.07 | Payment auto matching (p.9) | BR-091 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 | FIT |
| CORE-09.08 | Generate acknowledgement receipt, official receipt, invoice (p.9) | BR-094, BR-095, BR-096 | BRD-2 CSHID.001, CSHID.006, CSHID.019 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 ... | FIT |
| CORE-09.09 | Premium payment monitoring (p.9) | BR-097 | BRD-2 CSHID.023, CSHID.017 | FR-OP-028, FR-OP-009 | FIT |
| CORE-09.10 | Commission fee collection (p.9) | BR-098, BR-099 | BRD-2 CSHID.007, CMRID.002 | FR-OP-021, FR-OP-091 | FIT |
| CORE-09.11 | PDC management (p.9) | BR-100 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 | FIT |
| CORE-09.12 | Traceability and auditability (p.9) | - | BRD-2 CSHID.011 | FR-OP-024 | FIT |
| CORE-10.01 | Extraction and remittance processing (scheduled or manual) (p.9) | BR-102, BR-103 | BRD-2 RMTID.001, RMTID.003, RMTID.004 | FR-OP-030 | FIT |
| CORE-10.02 | Sending and uploading of files (p.9) | BR-104, BR-105 | BRD-2 RMTID.011, RMTID.013 | FR-OP-034, FR-OP-037 | FIT |
| CORE-10.03 | Validation and filtering criteria (p.9) | BR-106, BR-107, BR-108 | BRD-2 RMTID.014, RMTID.017, RMTID.020 | FR-OP-031 | FIT |
| CORE-10.04 | Search and view (p.9) | BR-109 | BRD-2 RMTID.025, RMTID.026 | FR-OP-039, FR-OP-005 | FIT |
| CORE-10.05 | Hold remittance management (p.9) | BR-110, BR-112, BR-113, BR-114, BR-115, BR-116 | BRD-2 RMTID.020, RMTID.031, RMTID.032 | FR-OP-031, FR-OP-005 | FIT |
| CORE-10.06 | Special remittance request and processing (p.9) | BR-117 | BRD-2 RMTID.030 | FR-OP-040 | FIT |
| CORE-10.07 | Notifications and tracking (p.9) | BR-111 | BRD-2 RMTID.033, RMTID.035, RMTID.036 | FR-OP-040, FR-OP-031, FR-OP-036 | FIT |
| CORE-11.01 | Data extraction and file management (p.9) | BR-119, BR-120 | BRD-2 PRCID.001, PRCID.009 | FR-OP-070, FR-OP-074 | FIT |
| CORE-11.02 | Production register viewing and filtering (p.9) | BR-121 | BRD-2 PRCID.012, PRCID.021 | FR-OP-071, FR-OP-077 | FIT |
| CORE-11.03 | Matching and automation (p.9) | BR-122, BR-123 | BRD-2 PRCID.023, PRCID.024, PRCID.033 | FR-OP-076, FR-OP-075 | FIT |
| CORE-11.04 | Tracking and monitoring (p.9) | BR-124 | BRD-2 PRCID.029, PRCID.030, PRCID.032 | FR-OP-079, FR-OP-075, FR-OP-074 | FIT |
| CORE-11.05 | Reports generation (p.9) | - | BRD-2 PRCID.034, PRCID.035, PRCID.039 | FR-OP-071, FR-OP-080 | FIT |
| CORE-12.01 | Transaction management (endorsements, cancellations) (p.9) | BR-126 | BRD-2 ADJID.001, ADJID.003, ADJID.005 | FR-OP-050, FR-OP-053 | FIT |
| CORE-12.02 | Automated accounting entries (p.9) | BR-127, BR-132 | BRD-2 ADJID.011, ADJID.012 | FR-OP-056, FR-OP-057 | FIT |
| CORE-12.03 | Traceability and auditability (p.10) | BR-128, BR-129 | BRD-2 ADJID.020, ADJID.022 | FR-OP-050, FR-OP-061 | FIT |
| CORE-12.04 | Reporting and monitoring (p.10) | - | BRD-2 ADJID.016, ADJID.019, ADJID.021 | FR-OP-062, FR-OP-061 | FIT |
| CORE-12.05 | Search and document management (p.10) | BR-130, BR-131 | BRD-2 ADJID.024, ADJID.025 | FR-OP-005, FR-OP-061, FR-OP-052 | FIT |
| CORE-12.06 | Sending and uploading of files (p.10) | - | BRD-2 ADJID.026 | FR-OP-059 | FIT |
| CORE-12.07 | Batch posting (p.10) | - | BRD-2 ADJID.006 | FR-OP-056 | FIT |
| CORE-13.01 | Automated incentive calculation (p.10) | BR-166, BR-167 | BRD-2 CMRID.005 | FR-OP-095 | FIT |
| CORE-13.02 | Motor Mania incentive plan (p.10) | BR-168 | BRD-2 CMRID.006 | FR-OP-095 | FIT |
| CORE-13.03 | Production data validation and exclusion handling (p.10) | BR-169 | BRD-2 CMRID.003 | FR-OP-095 | FIT |
| CORE-13.04 | Automated commission receivables processing (p.10) | BR-133 | BRD-2 CMRID.007 | FR-OP-091 | FIT |
| CORE-13.05 | Comprehensive production reporting (p.10) | BR-134 | BRD-2 CMRID.014, PRCID.035 | FR-OP-097, FR-OP-080 | FIT |
| CORE-13.06 | Risk mitigation and error handling (p.10) | BR-135 | BRD-2 CMRID.008 | FR-OP-091, FR-OP-093 | FIT |
| CORE-13.07 | Collection of commission receivables (direct payment) (p.10) | BR-052, BR-136, BR-137, BR-140 | BRD-2 CMRID.002, CMRID.004 | FR-OP-091, FR-OP-098 | FIT |
| CORE-13.08 | Auto-match reversals (p.10) | BR-138, BR-139 | BRD-2 CMRID.007, CSHID.027 | FR-OP-091, FR-OP-026 | FIT |
| CORE-14.01 | Process claims advice from BDOI Marketing (p.10) | BR-141 | BRD-7 BRCLM.003, BRCLM.016 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-014 | NEW |
| CORE-14.02 | Process claims advice from client (p.10) | BR-141 | BRD-7 BRCLM.003, BRCLM.006 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-002 ... | NEW |
| CORE-14.03 | Process claims advice from insurer (p.10) | BR-141 | BRD-7 BRCLM.041, BRCLM.043 | FR-CM-003, FR-CM-011, FR-CM-022, FR-CM-024 ... | NEW |
| CORE-14.04 | Process LOA from insurer (p.10) | BR-142 | BRD-7 BRCLM.014, BRCLM.010 | FR-CM-043, FR-CM-040, FR-CM-046 | NEW |
| CORE-14.05 | Process settlement offer from insurer (p.10) | BR-143 | BRD-7 BRCLM.010, BRCLM.014 | FR-CM-040, FR-CM-046, FR-CM-043 | NEW |
| CORE-14.06 | Tag permanent closure (p.10) | BR-144 | BRD-7 BRCLM.035, BRCLM.005 | FR-CM-045, FR-CM-002 | NEW |
| CORE-14.07 | Tag temporary closure (p.10) | BR-145 | BRD-7 BRCLM.035 | FR-CM-045 | NEW |
| CORE-14.08 | Unclaimed checks safekeeping (p.10) | BR-146 | BRD-7 | Gap G2 (CRQ03) | NEW |
| CORE-14.09 | Handover of settlement checks to Cashiering (p.10) | BR-147 | BRD-7 | Gap G2 (CRQ03) | NEW |
| CORE-14.10 | Retrieval of checks from Cashiering for release (p.10) | BR-148 | BRD-7 | Gap G2 (CRQ03) | NEW |
| CORE-14.11 | Maintain full claims history for audit and compliance (p.10) | BR-151 | BRD-7 BRCLM.004, BRCLM.022 | FR-CM-012, FR-CM-052, FR-CM-054 | NEW |
| CORE-14.12 | Reports and analytics viewing (p.10) | BR-149, BR-152 | BRD-7 BRCLM.026, BRCLM.029, BRCLM.030, BRCLM.031, BRCLM.032 | FR-CM-060, FR-CM-044, FR-CM-061, FR-CM-062 | NEW |
| CORE-14.13 | Encode / override the next follow-up date (p.10) | BR-150 | BRD-7 BRCLM.019 | FR-CM-002, FR-CM-050, FR-CM-054 | NEW |
| CORE-15.01 | Placement request initiation (p.10) | BR-153 | ReInsurance BRD FRID-001-FRID-010 | Phase 2 | OUT |
| CORE-15.02 | Placement slip management (p.10) | BR-154 | ReInsurance BRD FRID-017-FRID-028 | Phase 2 | OUT |
| CORE-15.03 | Statement of Account (SOA) generation (p.10) | BR-155, BR-156 | ReInsurance BRD FRID-033-FRID-041 | Phase 2 | OUT |
| CORE-15.04 | Claims reporting and settlement (p.10) | BR-157 | ReInsurance BRD FRID-101-FRID-105 | Phase 2 | OUT |
| CORE-15.05 | Claims payment processing (p.10) | BR-158 | ReInsurance BRD FRID-058-FRID-066 | Phase 2 | OUT |
| CORE-15.06 | Direct client claims payment (p.11) | BR-159 | ReInsurance BRD FRID-075-FRID-082 | Phase 2 | OUT |
| CORE-15.07 | Receive and validate file from stakeholders (capability matrix p.28) | - | ReInsurance BRD | Phase 2 | OUT |
| CORE-15.08 | Notification to stakeholders (capability matrix p.28) | - | ReInsurance BRD | Phase 2 | OUT |
| CORE-15.09 | Log, audit and history for traceability (capability matrix p.28) | - | ReInsurance BRD | Phase 2 | OUT |
| CORE-15.10 | Net settlement (capability matrix p.28) | - | ReInsurance BRD | Phase 2 | OUT |
| CORE-16.01 | Search, retrieve and display client contact and insurance account details (p.11) | BR-161 | BRD-9 BRCSF-002, BRCSF-003, BRCSF-008 | FR-CSF-011, FR-CSF-021, FR-CSF-022, FR-CSF-010 | CHANGE |
| CORE-16.02 | View, add and update client contact information (p.11) | BR-162 | BRD-9 BRCSF-004 | FR-CSF-020, FR-CSF-021 | CHANGE |
| CORE-16.03 | View mode of payment (history) and current status (p.11) | BR-163 | BRD-9 BRCSF-005 | FR-CSF-012, FR-CSF-013 | CHANGE |
| CORE-16.04 | View and resend RA (and e-policy) (p.11) | BR-164 | BRD-9 BRCSF-006, BRCSF-009 | FR-CSF-030, FR-CSF-033 | NEW |
| CORE-16.05 | Upload supporting documents (p.11) | - | BRD-9 BRCSF-007 | FR-CSF-032 | CHANGE |
| CORE-16.06 | Case resolution: add / edit case details and status (p.11) | BR-165 | BRD-9 | Gap G3 (CRQ04) | OUT |
| CORE-17.01 | Master data change logging (user, product, insurer, LOVs) (p.11) | BR-170 | None (BRNB.016, BRNB.083, BRPM.024, BRCLXN.043 related) | FR-CR-031; FR-NB-016, FR-NB-132, FR-PM-005, FR-CL-003 | CHANGE |
| CORE-17.02 | Insurer management (p.11) | BR-171 | None (BRNB.008 related) | FR-CR-061; FR-NB-053 | CHANGE |
| CORE-17.03 | LOV maintenance (p.11) | BR-004, BR-172 | BRD-1 BRNB.083, BASAU 2.2.0 | FR-CR-060; FR-NB-132, FR-AC-070 | FIT |
| CORE-17.04 | MIS field definition (p.11) | BR-173, BR-174 | None (BRNB.108 related) | FR-CR-062; FR-NB-110, FR-NB-119 | NEW |
| CORE-17.05 | Product maintenance (p.11) | - | BRD-3 BRPM.003, PMADD01 | FR-CR-063; FR-PM-012, FR-PM-010 | FIT |
| CORE-18.01 | Include any other / additional system capabilities (p.11) | BR-176 | None | Gap G9 (CRQ10) | OUT |
| CORE-19.01 | Automated renewal notifications (p.11) | BR-184, BR-196 | BRD-8 BRID-001 | FR-EB-022 | NEW |
| CORE-19.02 | Manual and system-based proposal generation (p.11) | BR-185, BR-197 | BRD-8 BRID-003 | FR-EB-024 | NEW |
| CORE-19.03 | Document and data upload management (p.11) | BR-186, BR-198 | BRD-8 BRID-005, BRID-005.01, BRID-014, BRID-025 | FR-EB-001, FR-EB-010, FR-EB-011, FR-EB-014 ... | NEW |
| CORE-19.04 | Broker on record management (p.11) | BR-187 | BRD-8 BRID-008 | FR-EB-031 | NEW |
| CORE-19.05 | Terms of Reference generation and distribution (p.11) | BR-188 | BRD-8 BRID-007, BRID-009 | FR-EB-004, FR-EB-030, FR-EB-035 | CHANGE |
| CORE-19.06 | Comparative report management (p.11) | BR-189 | BRD-8 BRID-010, BRID-011 | FR-EB-040, FR-EB-041, FR-EB-015, FR-EB-043 | CHANGE |
| CORE-19.07 | Client feedback, change, additional or amendment request capture and relay (p.11) | BR-190 | BRD-8 BRID-002, BRID-012, BRID-013, BRID-015 | FR-EB-023, FR-EB-044, FR-EB-054, FR-EB-055 ... | NEW |
| CORE-19.08 | Automated and manual sending of proposals (p.11) | BR-191 | BRD-8 BRID-003, BRID-009 | FR-EB-024, FR-EB-035 | NEW |
| CORE-19.09 | Approval workflow based on defined thresholds (p.11) | BR-192 | BRD-8 BRID-016 | FR-EB-042 | CHANGE |
| CORE-19.10 | Placement and booking management (p.11) | BR-193 | BRD-8 BRID-017, BRID-019, BRID-020 | FR-EB-046, FR-EB-051, FR-EB-052 | CHANGE |
| CORE-19.11 | Centralised reporting and analytics (p.11) | BR-194 | BRD-8 BRID-022, BRID-023, BRID-024 | FR-EB-060, FR-EB-062, FR-EB-001, FR-EB-003 | NEW |
| CORE-19.12 | Manage franchise requests and approvals (p.11) | BR-195 | BRD-8 BRID-026, BRID-027, BRID-029 | FR-EB-032, FR-EB-034, FR-EB-033 | NEW |
| CORE-20.01 | Automated request handling (p.12) | BR-199 | BRD-3 BRPM.011, BRPM.008 | FR-PM-020, FR-PM-024, FR-PM-045, FR-PM-021 | FIT |
| CORE-20.02 | Automatic reference numbers (p.12) | BR-200 | BRD-3 BRPM.008 | FR-PM-020, FR-PM-021 | FIT |
| CORE-20.03 | Quotation / proposal management (p.12) | BR-201 | BRD-3 BRPM.012, BRPM.013 | FR-PM-030, FR-PM-031, FR-PM-034, FR-PM-036 | FIT |
| CORE-20.04 | Automatic comparison tables (p.12) | BR-202 | BRD-3 BRPM.014, PMADD03 | FR-PM-034, FR-PM-035 | FIT |
| CORE-20.05 | Real-time dashboard / reports (p.12) | BR-203 | BRD-3 BRPM.019, BRPM.018 | FR-PM-070, FR-PM-071 | FIT |
| CORE-20.06 | Built-in checks / approvals (p.12) | BR-204 | BRD-3 BRPM.021, PMADD06 | FR-PM-021, FR-PM-073, FR-PM-043 | FIT |
| CORE-20.07 | Secure document sharing (p.12) | BR-205 | BRD-3 BRPM.020, BRPM.002 | FR-PM-004, FR-PM-002 | FIT |
| CORE-20.08 | Works with existing systems (p.12) | BR-206 | BRD-3 BRPM.022 | FR-PM-072 | CONFIGURE |
| CORE-20.09 | Track all changes (p.12) | BR-207 | BRD-3 BRPM.024 | FR-PM-005 | FIT |
| CORE-20.10 | Expiring packages monitoring (p.12) | BR-208 | BRD-3 BRPM.017, BRPM.006 | FR-PM-044, FR-PM-060, FR-PM-061, FR-PM-071 ... | FIT |
| CORE-21.01 | Generate standard reports for operational and analytical purposes (p.12) | BR-054 | BRD-1 BRNB.057, BRNB.075 | FR-CR-040; FR-NB-123, FR-NB-122 | FIT |
| CORE-21.02 | Extract, download and print reports in multiple formats (p.12) | BR-078 | BRD-1 BRNB.031, BRNB.037 | FR-CR-040; FR-NB-124, FR-NB-125 | FIT |
| CORE-21.03 | Create tailored reports by parameters, filters and business requirements (p.12) | BR-053 | BRD-1 BRNB.057 | FR-CR-041; FR-NB-123 | FIT |
| CORE-21.04 | Dynamic customisation: data fields, charts and summaries (p.12) | BR-053 | None | FR-CR-042 | CHANGE |
| CORE-21.05 | Scheduled or on-demand report generation (p.12) | BR-054, BR-103 | None (DIS 3.28.0, BRCLXN.028 related) | FR-CR-043; FR-DS-081, FR-CL-082 | CHANGE |
| XC-01 | Log-in, password reset, several types of user access, user administration (p.13) | BR-000, BR-001, BR-002, BR-003 | BRD-11 BRNB.040, BRNB.084 | FR-CR-070; FR-NB-130, FR-NB-134 | FIT |
| XC-02 | Centralised, real-time dashboards for all roles; details invoked from the dashboard (p.5, p.20-21) | BR-125, BR-152, BR-160, BR-203 | None (BRNB.012, BRQID.003, BRPM.019 related) | FR-CR-010, FR-CR-011; FR-NB-120, FR-OP-003, FR-PM-070 | CHANGE |
| XC-03 | Automated notifications, approvals and feedback tracking (p.5, p.19, p.24) | BR-083, BR-111, BR-206 | None (BRNB.015, RMTID.033 related) | FR-CR-020; FR-NB-015, FR-OP-040 | FIT |
| XC-04 | Audit logs and full transaction history (p.4-5, p.20, p.25) | BR-124, BR-129, BR-151, BR-207 | None (BRNB.016, BRNB.086, BRNB.089 related) | FR-CR-030; FR-NB-016, FR-NB-136 | FIT |
| XC-05 | Workflow maintained at the back end; data flows to the next process by rules (p.15, p.18, p.23) | BR-051, BR-101, BR-177, BR-182 | None (BRNB.096, BRNB.022 related) | FR-CR-080; FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 | CONFIGURE |
| XC-06 | Batch and individual processing; concurrent users and high-volume bulk uploads (p.4, p.16) | BR-063 | None (BRNB.064, BRQID.006 related) | FR-CR-083; FR-NB-019, FR-NB-065, FR-OP-008 | FIT |
| XC-07 | Currency selection and multi-currency support (p.15, p.18) | BR-027, BR-088 | None (BRCLM.009 related) | FR-CR-091; FR-CM-011 | FIT |
| XC-08 | 30-day hold cover request, assigned to a role (p.14) | BR-034 | BRD-1 BRNB.072, BRNB.103 | FR-NB-082, FR-NB-083 | FIT |
| XC-09 | Tag direct payment (DP) accounts (p.14) | BR-035 | BRD-1 BRNB.114 | FR-NB-069, FR-NB-092 | FIT |
| XC-10 | SOA generated with placement report and Insurance Advice; service invoice to insurer issued with the SOA at booking (p.14-15) | BR-039, BR-043 | BRD-1 BRNB.100, BRCLXN.058 | FR-CR-093; FR-NB-117, FR-CL-060, FR-CL-061 | CHANGE |
| XC-11 | Billing reports with premium and loan details; payment reports matched to accounts (p.15) | BR-049, BR-050 | BRD-1 BRNB.067, BRNB.068 | FR-NB-090, FR-NB-091, FR-NB-092 | FIT |
| XC-12 | BIR standard books (sales, purchase, cash receipts, cash disbursements, general journal) (p.17) | BR-084 | BRD-5 FRBS 3.2.0 | FR-CR-040; FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | FIT |
| XC-13 | Invoice Master List across workflows (p.23) | BR-175 | None (BRCLXN.001, ACSL 2.16.0 related) | FR-CR-090; FR-CL-010, FR-AS-026 | CHANGE |
| XC-14 | BDO brand colours, logos, icons and design system (p.23) | BR-178, BR-179, BR-180 | None | FR-CR-092 | CHANGE |
| XC-15 | Identified user journeys and customisable interaction flows (with a demo) (p.23) | BR-181, BR-182 | None | FR-CR-001 | FIT |
| XC-16 | Vendor uses Figma for design execution (p.23) | BR-183 | None | - | OUT |
| XC-17 | Regulatory compliance: BIR and Insurance Commission reportorial requirements (p.4) | - | BRD-5 FRBS 3.2.0 | FR-CR-040; FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | CHANGE |
| XC-18 | Retail and wholesale business (p.4) | - | BRD-1 BRNB.001 | FR-CR-001; FR-NB-001 | FIT |
| XC-19 | Invoice batch printing, delivery to insurers by SFTP and delivery report (daily, per insurer) (p.43) | - | None (BRNB.100 related) | FR-CR-081; FR-NB-117 | CHANGE |
| XC-20 | Client migration (one-time, 2020 to present) and daily midday / EOD client batches with modification report (p.43) | - | Data Migration BRD | FR-CR-082 | NEW |
| XC-21 | ALeA e-mail address encoding (as needed) (p.43) | - | None | Gap G10 (CRQ20) | OUT |
| XC-22 | Daily synchronisation from source systems (CMS, Reinsurance) (p.42-43) | - | BRD-4 BRCLXN.013, BRCLXN.014, BRCLXN.015 | FR-CR-082; FR-CL-017 | CONFIGURE |
| XC-23 | MIS LOV, QPS insurer, LGT rates and insurer branch maintenance (MILB, 24 a year) (p.45) | - | BRD-1 BRNB.083 | FR-CR-060; FR-NB-132 | FIT |

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
