---
# Source of the Functional Requirements Specification for BRD-9 Customer Servicing Facility.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-09_Customer_Servicing_Facility/FRS_BRD09_CUSTOMER_SERVICING_FACILITY.md
title: Customer Servicing Facility
subtitle: BRD-9 Customer Servicing Facility (CSF) with the Case Management Addendum and the scope e-mail of 13-Feb-2026
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-09
name: Customer Servicing Facility
doc_id: BIBS-FRS-BRD-09
version: "1.1"
date: 08 October 2026
status: Issued for BDOI review
header_title: FRS BRD-9 Customer Servicing Facility
output: FRS/BIBS_FRS_BRD-09_Customer_Servicing_Facility_v1.1.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-9 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decision D3
  - version: "1.1"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: "Re-based on Customer Servicing Facility_Case Management (CSF BRD v1.0 with the CSF Addendum - Case Management, signed 31-Mar to 22-Apr-2026); Case Management requirements FR-CSF-050 to FR-CSF-061 added; user-story view and storyboard index added."
distribution:
  - {name: "Product Owner, Customer Servicing Facility", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Alternative Distribution Head", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Head, Corporate and Retail Marketing", role: Approver, organisation: BDOI, purpose: Review and sign-off of the Case Management requirements}
  - {name: BDO Insure Contact Center Management, role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: Fulfilment units named in the case lists of values, role: Business user, organisation: BDOI, purpose: Review of the case routing and resolution rules}
  - {name: Marketing Business Services and System Support (MBS), role: Business user, organisation: BDOI, purpose: Review of the contact and status rules}
  - {name: ITG Core Business - Insurance Applications, role: Reviewer, organisation: BDO Unibank, purpose: Review of the coexistence with the legacy systems}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Customer Servicing Facility (CSF) business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to deliver and test the CSF functions and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

This FRS is written from the BRD, its Case Management Addendum and the requirements baseline (R3). It describes the proposed behaviour of the CSF screens. Where the proposed rule or screen differs from the BRD text, or needs a decision of BDOI, chapter 12 lists each such point for confirmation.

## Scope

The CSF replaces the BDO-Insure Front-End System used by the BDO Insure Contact Center (p.21). Agents answer clients who call the hotline, write e-mails or use the website. In BIBS the CSF is a **servicing workspace over the BIBS modules**: client, account, invoice, payment, e-policy, renewal advice and document data stay in their owning modules, and the CSF reads them. The only changes an agent makes to that data are contact details (after verifying the caller), resends, and document uploads.

The CSF Addendum - Case Management (approved 31-Mar to 22-Apr-2026, p.1-17) adds a Case Management module to the CSF. Every customer inquiry, request, problem and complaint is logged as a case with a unique Case ID, routed to a fulfilment unit, tracked against the turnaround time of its case type and closed, in place of the SharePoint tracker used today (p.3-4). The cases are the CSF's own records.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access | Log-in with user ID and password, credential validation, role-based access for agents, supervisors and management | BRCSF-001, 1.001, 1.002; p.21 |
| Search and view | Search by name, client ID, account number, PN number and application number; client information; accounts with their status; payment history | BRCSF-002, 003, 3.001, 005, 5.001, 008; e-mail item 7 |
| Contact details | Verification of the caller, contact-only update, write-back to QPS and EBIX while they coexist | BRCSF-002, 004 |
| Resend and documents | View, download and resend the renewal advice and the e-policy; upload and retrieve documents | BRCSF-006, 6.001, 007, 7.001, 009, 9.001; e-mail item 9 |
| Audit and backup | Audit trail of client changes, print or save in PDF or Excel, agent activity, backup every 15 minutes | BRCSF-010, 011, 11.001, 11.002 |
| Case management | Case logging with Case ID, case type with turnaround time, lifecycle statuses, notes, attachments, audit trail, reassignment and escalation, SLA ageing and notifications, closure with a linked follow-up case, fulfilment unit routing, resolution and return, reports and dashboards, case lists of values | BRCSFCM.001-008, 010-012; p.4, p.11-17 |

**Out of scope for this phase:**

- The migration of the cases tracked in SharePoint today. The addendum does not ask for it; open cases are re-keyed at go-live unless BDOI asks for a migration (CSQ23).
- The open items of the e-mail (priority order for servicing of payments versus claims; parameter alignment between the loan system and the insurance system), which are also future enhancements (p.34; CSQ16).
- Changes other than contact details (for example civil status); they stay with the fulfilment unit (e-mail topic 3, p.34).
- The transport of contact updates to QPS and EBIX and the lookup of accounts that exist only there. Both wait until BDOI specifies the interface (CSQ01).

> [!DECISION] Case management is BDOI scope; its delivery drop is to be decided
> The e-mail of 13-Feb-2026 deferred case management to a future enhancement with its own requirement document (row CSF-EM10, p.34). That document is the CSF Addendum - Case Management, signed by Contact Center Management, the Product Owners, the Head of Corporate and Retail Marketing and the Program Manager. Version 1.1 of this FRS therefore specifies case management in full (section 4.6). The drop in which it is delivered and its plan need a decision of BDOI and the Change Control Board (CLR-CSF-07).

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Customer Servicing Facility (CSF) BRD, pages 18-33 of the file Customer Servicing Facility_Case Management (BRCSF-001 to 011; usage requirements; approval sheet) | v1.0, 09-Jun-2025; approved 24-Jun to 02-Jul-2025 |
| R2 | E-mail thread "Core Modernization: CSF", pages 34-35 (minutes of 23-Jan-2026; scope confirmation of 13-Feb-2026) | 22-Jan to 13-Feb-2026 |
| R3 | BDOI Customer Servicing Facility (BRD-9) requirements baseline | current |
| R4 | Customer Servicing Facility (CSF) Addendum - Case Management, pages 1-17 of the same file (BRCSFCM.001 to 008 and 010 to 012; lists of values p.11-17; approval sheet p.9-10) | v1.0, 03-Mar-2026, revised 05-Mar and 26-Mar-2026; signed 31-Mar to 22-Apr-2026 |
| R5 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | binding |
| R6 | BDO UX guidelines (brand, screen patterns) | current |

Page references in this document ("p.25") are pages of the file Customer Servicing Facility_Case Management (35 pages) received on 08-Oct-2026: pages 1-17 are the Case Management Addendum, pages 18-33 the CSF BRD and pages 34-35 the e-mail thread. Version 1.0 of this FRS cited the CSF BRD without the addendum; its page numbers are 17 lower. The BRD reuses its BR ID for the process steps (for example BRCSF-003 / 3.001); FRs cite both. The e-mail items 7 and 9, which the e-mail of 13-Feb-2026 adds to the scope, are traced as CSF-EM07 and CSF-EM09. The addendum numbers its requirements BRCSFCM.001 to 008 and 010 to 012; there is no BRCSFCM.009 (CSQ22).

## Definitions and acronyms

```glossary
Agent: BDO Insure Contact Center Agent
ARN: Account Reference Number of a BIBS account
BRCSF: Requirement ID prefix of the CSF BRD (BRCSF-001 to 011)
BRCSFCM: Requirement ID prefix of the CSF Addendum - Case Management (BRCSFCM.001 to 012)
Case: Record of one customer inquiry, request, problem, complaint, follow-up or commendation, with a unique Case ID (CASE-yyyy-nnnnnn)
CSR: Customer Service Representative; the addendum's name for the Contact Center Agent in the case statuses
Fulfilment unit: BDOI unit that resolves a case (Fulfillment Unit in the addendum; 53 units listed on p.16-17)
SLA: Service level of a case - its turnaround time and the near-breach and breach thresholds
TAT: Turnaround time of a case type (p.11)
CBG: Consumer Banking Group; today's system shows full details only for CBG accounts (p.21)
CSF: Customer Servicing Facility
CSF-EMnn: Requirement added or deferred by the e-mail of 13-Feb-2026 (item nn, p.34)
CSF status: Status shown to the agent (Pending, Awaiting, Booked, Open, Closed), mapped from the BIBS account stage and payment status
CSQnn: Open question on BRD-9 raised by the project team (section 10.3)
EBIX / QPS: Legacy systems that are today's sources of client and account data (p.20)
FR: Functional requirement of this document (FR-CSF-nnn)
LOS: Loan origination system, source of PN and application numbers
PN: Promissory note number of a bank loan linked to an insurance account
RA: Renewal Advice
UAT: User acceptance testing
Verification: The check of the caller's identity before a change (address, contact number, e-mail, insured property)
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority** and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained in BIBS (parameter, list of values, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-9 test plan.

> [!NOTE]
> Messages are given with their text; a code is quoted where it is confirmed (for example ACCESS_DENIED), and "To be confirmed" marks the others. Values marked "default" are placeholders that BDOI confirms through the open questions in section 10.3; they are configuration, so a changed answer does not need a change to the system.


# Business context and process overview

## Business context

The BDO-Insure Front-End System of BDO IT serves the contact centre agents today. It shows full insurance account details only for Consumer Banking Group (CBG) accounts; for non-CBG accounts it shows only the contact details, account number and expiry date. Agents can update only client contact details. Its servers have reached end of life, and it has no role-based access control (p.21).

The BRD asks for an application that shows client contact details and account information from the data sources, and that sends contact updates back to them (p.20, p.22). In January 2026 the BRD was clarified with the contact centre and BDO IT: search keys, verification before an update, payment history depth, two more statuses and e-policy resending (p.34).

The Case Management Addendum (p.3-4) adds the module for the inquiries themselves: today they are logged in SharePoint as an interim tool, follow-ups depend on the agent and management has limited visibility. Most cases are retail, many tied to renewals. Rows 7 to 10 of the table come from the addendum.

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.20-22, e-mail p.34, addendum p.4)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Full account details only for CBG accounts | Every account of the client, CBG and non-CBG, with its status and payments |
| 2 | Search on a limited set of keys | One search box for name, client ID, account number, PN number and application number |
| 3 | No role-based access control | Agent, supervisor and management roles with separate rights to update, resend and upload |
| 4 | Contact updates are written in QPS or EBIX | The update is made once in the BIBS client master after verifying the caller; a write-back serves QPS and EBIX while they coexist |
| 5 | RA and e-policy resends are handled outside the tool | The agent views, downloads and resends the RA and the e-policy to the registered e-mail, password protected |
| 6 | No audit report for management | Every change and agent action is logged; the audit trail prints or saves in PDF or Excel |
| 7 | Inquiries are logged manually in SharePoint or offline trackers (addendum p.4) | Every inquiry, whatever the channel, is logged as a case with a unique Case ID in the CSF |
| 8 | Follow-ups depend on the individual agent | Each case has a type with a turnaround time, an owner, a due date, SLA tracking, escalation and supervisor oversight |
| 9 | Limited reporting and performance visibility | Case reports and dashboards: volume, ageing, SLA compliance and status distribution |
| 10 | Servicing and routing flow through the business units and account officers by market segment | Routing is system-driven by case type, market segment and nature of transaction to the fulfilment unit, with the account officer kept informed |

## Process overview

The table lists the steps of a servicing contact, and Figure 1 shows them. CSF has no approval workflow: every action is immediate and logged.

<!-- table: widths=0.8,3.6,3.2,7.6,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Sign in | Agent | BIBS log-in; the Customer Search is the agent's landing page | BRCSF-001 |
| 2 | Find the client | Agent | One search by key type; results grouped by client with the matching accounts | BRCSF-003 |
| 3 | Servicing view | Agent | Client summary, accounts with CSF status, payments of the last 12 months, RAs, e-policies, documents | BRCSF-002, 005, 008, 009 |
| 4 | Verify and update contact | Agent | Verification checklist, then contact fields only, with a reason | BRCSF-004 |
| 5 | Resend | Agent | RA or e-policy to the registered e-mail, protected, password sent separately | BRCSF-006; e-mail item 9 |
| 6 | Documents | Agent | Retrieve quotations, RAs, claims reports; upload documents to the client or an account | BRCSF-007, 009 |
| 7 | Legacy sync | System | Contact change queued for QPS and EBIX while they coexist (on hold) | p.20; CSQ01 |
| 8 | Audit and reports | Management | Audit trail, contact changes and agent activity in PDF or Excel | BRCSF-010, 011 |

![Servicing flow of a contact centre agent (BRCSF-001-011)](figures/brd09_process_flow.dot){width=13}

A case runs alongside the servicing contact (Figure 2). The agent logs it from the Servicing View, resolves it on the call or submits it to the fulfilment unit; the fulfilment unit resolves and closes it, returns it to the agent or asks the agent to act with the client. Supervisors reassign and escalate; management follows the dashboard.

<!-- table: widths=0.8,3.6,3.2,7.6,2.6 caption="Case management steps (addendum p.4-8)" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 9 | Log the case | Agent | Case ID, client and account, channel, case type with TAT, nature of transaction, product type, market segment, fulfilment unit; duplicate check | BRCSFCM.001, 002 |
| 10 | Route | System | Fulfilment unit from the routing table; notification to the unit and the account officer | BRCSFCM.012; p.4 |
| 11 | Resolve, return or request CSR action | Fulfilment Unit | Resolution and closure, or return to the agent, or CSR action required | BRCSFCM.003, 012 |
| 12 | Notes and documents | Agent; Fulfilment Unit | Append-only notes, supporting documents, audit trail | BRCSFCM.004-006 |
| 13 | Supervise | Supervisor | Reassign, escalate, SLA ageing and breach notifications | BRCSFCM.007, 008 |
| 14 | Close and follow up | Agent; Fulfilment Unit | Closure status, read-only closed case, linked follow-up case | BRCSFCM.010 |
| 15 | Report | Management | Case dashboard and reports in Excel, CSV and PDF | BRCSFCM.011 |

![Case lifecycle (BRCSFCM.003, 010, 012)](figures/brd09_case_lifecycle.dot){width=13}

## Data sources in BIBS

The BRD names QPS and EBIX as the data sources (p.20). In BIBS the data live in the BIBS modules. The CSF reads them and stores only its own records: contact change requests with their verification, the legacy sync outbox, the agent activity log and the cases with their notes, documents and history.

<!-- table: widths=4.4,5,7.2 caption="What the CSF shows and where it comes from" -->
| Data | Owning module | Used in |
|---|---|---|
| Client information and contacts | Client master (CRM, BRD-1) | FR-CSF-011, 021 |
| Accounts, ARN, PN numbers, account stage | Accounts (BRD-1) | FR-CSF-010, 012 |
| Loan application numbers | Placement billing items (BRD-1) | FR-CSF-010 |
| Policy numbers and e-policies | Issuance (BRD-1) | FR-CSF-011, 031 |
| Invoices, balances, payment status, receipts | Invoice ledger and Cashiering (BRD-2) | FR-CSF-012, 013 |
| Renewal advices | Renewal (BRD-6) and Employee Benefits (BRD-8), document type RENEWAL_ADVICE | FR-CSF-030 |
| Claims reports | Claims (BRD-7), document type CLAIM_REPORT | FR-CSF-033 |
| Quotations and other documents | Quotation, placement and attachment services | FR-CSF-033 |
| Cases, case notes, case documents, routing table and case lists | CSF (own records) | FR-CSF-050 to 061 |
| Account officer and unit of an account | Accounts (BRD-1) and organisation (BRD-11) | FR-CSF-060 |

# Personas and roles

## Personas

<!-- table: widths=3.6,4,7.2,2.8 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in the CSF | BRD |
|---|---|---|---|
| BDO Insure Contact Center Agent | CSF_AGENT | Searches clients; views client, accounts, payments and documents; verifies callers and updates contacts; resends RAs and e-policies; uploads documents; logs, notes, documents and closes cases (CSR in the addendum) | BRCSF-001-009; BRCSFCM.001-005, 010 |
| Contact Center Supervisor, BDO Contact Center personnel | CSF_SUPERVISOR | As the agent; resends to another address with a reason; runs CSF reports; reassigns, escalates, recategorises and closes cases; follows the SLA dashboard | p.30; BRCSFCM.002, 007, 008 |
| Contact Center Management, Leads / Heads | CSF_MANAGEMENT | Views the audit trail and the CSF reports; prints or saves them; case dashboard and case reports; owns the case lists and the routing table | BRCSF-011 / 11.002; p.30; BRCSFCM.011 |
| Fulfilment Unit | CSF_FULFILMENT | Receives the cases routed to its unit; maintains the resolution and closes the case; returns it to the agent or asks for CSR action | BRCSFCM.012 |
| System Administrator | SYSADMIN | Users and roles; restricts access to authorised users | BRCSF-001 |

The BRD asks for differentiated roles for updating records and uploading documents (p.21). The roles above are the project's proposal until BDOI confirms the matrix (CSQ10). The Renewal design has its own Contact Center role for renewal follow-ups; whether it is the same group of people is open (XQ02). A user may hold both roles. The Fulfilment Unit is a new persona of the addendum: its users are staff of the 53 units of the list (p.16-17) - claims, marketing, collections, submitted policies and other teams - who keep their own BIBS roles and receive CSF_FULFILMENT with the membership of their units (CLR-CSF-10).

## Permissions

<!-- table: widths=5,11.6 caption="CSF permissions" -->
| Permission | Allows |
|---|---|
| CSF_VIEW | Customer Search and Servicing View |
| CSF_CONTACT_UPDATE | Verification and contact changes |
| CSF_RESEND | Resend the RA and the e-policy to the registered e-mail |
| CSF_RESEND_OTHER | Resend to another address, with a reason |
| CSF_DOCUMENT_UPLOAD | Upload documents from the Servicing View |
| CSF_REPORT_VIEW | CSF reports |
| CSF_CASE_CREATE | Log a case; add notes and documents; re-endorse and close own cases as Closed by CSR; create a follow-up case |
| CSF_CASE_SUPERVISE | Reassign, escalate, change the category and close any open case |
| CSF_CASE_FULFIL | Resolve and close, return or request CSR action on the cases of the user's fulfilment units |
| CSF_CASE_REPORT | Case dashboard and case reports |
| CSF_CASE_ROUTING | Maintain the case routing table |
| AUDIT_VIEW (existing) | Audit trail report CTL-AUDIT |
| ATTACHMENT_VIEW (existing) | Open and download documents (subject to the access classes) |

Agents do not receive CLIENT_MAINTAIN or EPOLICY_SEND. Their changes go through the narrow CSF permissions, which call a contact-only update and the e-policy dispatch service.

## Permissions matrix

<!-- table: widths=4.6,2.4,2.4,2.4,2.4,2.4 caption="Role-to-action matrix for the CSF (proposal until CSQ10)" size=8.5 -->
| Permission | CSF Agent | CSF Supervisor | CSF Management | Fulfilment Unit | System Admin |
|---|---|---|---|---|---|
| CSF_VIEW | Y | Y | Y | | |
| CSF_CONTACT_UPDATE | Y | Y | | | |
| CSF_RESEND | Y | Y | | | |
| CSF_RESEND_OTHER | | Y | | | |
| CSF_DOCUMENT_UPLOAD | Y | Y | | | |
| CSF_REPORT_VIEW | | Y | Y | | |
| CSF_CASE_CREATE | Y | Y | | | |
| CSF_CASE_SUPERVISE | | Y | | | |
| CSF_CASE_FULFIL | | | | Y | |
| CSF_CASE_REPORT | | Y | Y | | |
| CSF_CASE_ROUTING | | | | | Y |
| AUDIT_VIEW | | | Y | | Y |
| ATTACHMENT_VIEW | Y | Y | | Y | |
| User and role administration | | | | | Y |

CSF roles see every client and account, CBG and non-CBG (p.21; CQ06), with no segment scoping unless BDOI asks for one (CSQ10). The CSF roles see every case; a Fulfilment Unit user sees the cases routed to his units only (FR-CSF-060).

# Functional requirements

## Access

```fr
id: FR-CSF-001
title: Log in with user ID and password
brd: [BRCSF-001 (p.23), BRCSF-001 / 1.001 (p.23-24), BRCSF-001 / 1.002 (p.24)]
actor: Contact Center Agent; System
priority: Must have
screens: Login; Customer Search (landing page)
description: Agents log in with their BIBS user ID and password on the platform log-in. BIBS validates the credentials and grants access to the CSF screens of the user's roles; the Customer Search opens as the landing page. Wrong credentials are refused. Directory sign-in (Windows ID) is added for all BIBS users when BDO supplies the interface; the BIBS sign-in applies until then (decision D6).
preconditions:
  - The user has an active BIBS account with a CSF role.
main_flow:
  - The agent enters the user ID and password.
  - BIBS validates the credentials and the account status.
  - BIBS opens the Customer Search.
alternate_flows:
  - Incorrect credentials. Access is denied and the failed attempt is counted.
  - Locked account. After three failed attempts the account locks; the System Administrator unlocks it (decision D5).
rules:
  - [R1, "Lockout after 3 failed attempts.", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R2, "The landing page of CSF roles is the Customer Search.", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user name or password, AUTHENTICATION_FAILED]
  - [Account locked (the same message as a wrong password; the lock shows to the System Administrator on Users), Invalid user name or password, AUTHENTICATION_FAILED]
notifications:
  - "None."
audit:
  - "Every successful and failed log-in with user, time and source address."
acceptance:
  - An agent with valid credentials lands on the Customer Search.
  - Wrong credentials are refused and no session is created.
  - A user without a CSF role does not see the CSF menu section.
```

```fr
id: FR-CSF-002
title: Restrict CSF actions to authorised roles
brd: [BRCSF-001 (p.23), "Current process limitations (p.21)"]
actor: System; System Administrator
priority: Must have
screens: All CSF screens
description: Only authorised personnel reach the CSF. Every CSF screen, button and call needs one of the permissions of section 3.2, so updating contacts, resending to another address, uploading documents and running reports are separate rights (p.21). Menus show only what the user's roles allow. Role grants change through User Access Maintenance requests (BRD-11).
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a CSF screen or starts an action.
  - BIBS checks the permission.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The action is not offered; a direct call is refused and logged.
rules:
  - [R1, "Grants of section 3.3 until BDOI confirms the matrix (CSQ10).", Configurable, User Access Maintenance request]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Refused actions with user, function and time."
acceptance:
  - A CSF Management user does not see the Update Contact action.
  - An agent's direct call to resend to another address is refused with ACCESS_DENIED.
```

## Search and servicing view

```fr
id: FR-CSF-010
title: Search a client by name, client ID, account, PN or application number
brd: [BRCSF-003 (p.24-25), BRCSF-003 / 3.001 (p.25), "E-mail topic 4 (p.34)"]
actor: Contact Center Agent (CSF_VIEW)
priority: Must have
screens: Customer Search
description:
  - The agent selects the key type and enters the value. Name searches the client master; client ID searches the client code, prospect code and government ID number; account number searches the ARN (with or without its suffix), the policy number and the legacy account number of migrated accounts; PN number searches the account PN numbers; application number searches the loan application numbers of the placement billing items.
  - Results are grouped by client, each with its matching accounts. When exactly one client matches, its Servicing View opens directly. When none matches, BIBS says so clearly (e-mail topic 4).
preconditions:
  - The user has CSF_VIEW.
main_flow:
  - The agent selects the key type and enters the value.
  - BIBS runs the search on the indexed keys and lists the results.
  - The agent opens a client.
alternate_flows:
  - No match. BIBS shows "No client found for <criteria>".
  - PN or application number of an account not in BIBS. The lookup of legacy accounts returns nothing until QPS, EBIX or LOS are connected (CSQ01, CSQ02).
rules:
  - [R1, "Minimum 3 characters for name searches (default).", Configurable, Parameter CSF_SEARCH_MIN_CHARS]
  - [R2, "At most 50 results (default); the agent refines the search beyond that.", Configurable, Parameter CSF_SEARCH_MAX_RESULTS]
  - [R3, "Meaning of client ID and account number confirmed under CSQ02.", Configurable, Change request after CSQ02]
validations:
  - [Search value blank, Enter the value to search, "-"]
  - [Name shorter than the minimum, Enter at least 3 characters, "-"]
  - [More results than the maximum, "More than 50 clients match. Refine the search", "-"]
fields_screen: Customer Search
fields:
  - [Search by, List, "Yes", Name / Client ID / Account No. / PN No. / Application No., "-"]
  - [Value, Text, "Yes", "-", At least 3 characters for Name]
notifications:
  - "None."
audit:
  - "Each search with agent, key type, criteria and time (activity log)."
acceptance:
  - A search by PN number returns the client of the account with that PN.
  - A search by application number returns the client of the placement billing item.
  - A search with no result shows "No client found" with the criteria.
  - A search returns within 3 seconds (p.30).
```

```fr
id: FR-CSF-011
title: View current client information and accounts
brd: [BRCSF-002 (p.24), BRCSF-008 (p.27)]
actor: Contact Center Agent (CSF_VIEW)
priority: Must have
screens: Servicing View (summary card; tabs Accounts, Payments, Renewal Advice, E-policies, Documents, Contact History)
description:
  - The Servicing View shows the client's current information - code, name, status, flags and contact details - and every account of the client with its CSF status, BIBS stage, policy number, period and balance. All segments are visible, CBG and non-CBG. The data are read from the owning BIBS modules when the view opens; nothing is copied.
  - Each tab loads on its own so that every tab answers within 3 seconds. Users with CLIENT_VIEW open the full client page with one click.
preconditions:
  - The user has CSF_VIEW.
main_flow:
  - The agent opens a client from the search.
  - BIBS shows the summary card and the Accounts tab.
  - The agent opens the other tabs as needed.
alternate_flows:
  - A source module does not answer. The tab shows "Information not available now. Try again" and the other tabs still work.
rules:
  - [R1, "No segment scoping for CSF roles (p.21; CQ06).", Configurable, Change request after CSQ10]
  - [R2, "Accounts only in QPS or EBIX are not shown until the legacy lookup is connected (CSQ01).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Each opening of a Servicing View with agent, client and time (activity log)."
acceptance:
  - The Servicing View of a client with a CBG and a non-CBG account shows both with their details.
  - Each tab loads within 3 seconds.
  - A contact change made in BIBS shows at once in the Servicing View.
```

```fr
id: FR-CSF-012
title: Show the status of each account
brd: [BRCSF-005 / 5.001 (p.25), CSF-EM07 (p.34)]
actor: Contact Center Agent
priority: Must have
screens: Servicing View (Accounts tab)
description: Each account shows the latest CSF status - Pending, Awaiting, Booked, Open or Closed - next to its BIBS stage. BIBS computes the status from the account stage, the invoice payment status and the policy period through a maintainable mapping, so the definitions agreed by Marketing, Processing and Operations can be applied without a change to the system (e-mail open item 3).
preconditions:
  - "None."
main_flow:
  - The agent opens the Accounts tab.
  - BIBS maps each account to its CSF status and shows both statuses.
rules:
  - [R1, "Mapping rows (account stage pattern, payment status pattern, CSF status, order) are maintained; seeds in section 5.1.", Configurable, LOV CSF_STATUS_MAP]
  - [R2, "Definitions confirmed under CSQ04.", Configurable, LOV CSF_STATUS_MAP]
validations: []
notifications:
  - "None."
audit:
  - "Mapping changes are audited."
acceptance:
  - An account awaiting payment shows the CSF status Awaiting and the BIBS stage AWAITING_PAYMENT.
  - A booked account in force shows Open; a cancelled account shows Closed.
  - Changing the mapping of a stage changes the displayed status without a change to the system.
```

```fr
id: FR-CSF-013
title: View the payment history
brd: [BRCSF-005 (p.25), "E-mail topic 6 (p.34)"]
actor: Contact Center Agent
priority: Must have
screens: Servicing View (Payments tab)
description: The Payments tab shows the client's payments of the last 12 months (default) - receipt number, date, mode and amount, the invoices each payment was applied to, and the current balance and payment status of each invoice - newest first. The agent filters by account and clicks **Show older** to extend the window. The data come from the invoice ledger and Cashiering.
preconditions:
  - "None."
main_flow:
  - The agent opens the Payments tab.
  - BIBS lists the payments of the window.
  - The agent filters by account or shows older payments.
rules:
  - [R1, "Window 12 months (default; 11-12 months in the e-mail).", Configurable, Parameter CSF_PAYMENT_HISTORY_MONTHS]
  - [R2, "Content (receipts, applications, remittances) confirmed under CSQ05.", Configurable, Change request after CSQ05]
validations: []
notifications:
  - "None."
audit:
  - "Tab views are part of the Servicing View log."
acceptance:
  - A payment applied to two invoices shows both invoices and the remaining balance of each.
  - Payments older than 12 months appear only after Show older.
```

## Contact details

```fr
id: FR-CSF-020
title: Verify the caller before a change
brd: [BRCSF-004 (p.25), "E-mail topic 5 (p.34)"]
actor: Contact Center Agent (CSF_CONTACT_UPDATE)
priority: Must have
screens: Update Contact dialog (step 1, verification)
description: Before an update the agent verifies the caller's identity with a checklist - address, contact number, e-mail, insured property details - recording for each check whether it matched, and the channel (hotline, e-mail, website). The verification passes when enough checks match and stays valid for a set time. Repeated failed verifications of the same client on one day raise an alert.
preconditions:
  - "The agent has the client's Servicing View open."
main_flow:
  - The agent clicks **Update Contact**.
  - The agent asks the verification questions and ticks the result of each check.
  - BIBS records the verification as PASSED or FAILED.
alternate_flows:
  - Failed. BIBS stops the update and shows the reason.
rules:
  - [R1, "Passed when at least 2 checks match (default).", Configurable, Parameter CSF_VERIFY_MIN_MATCHES]
  - [R2, "A passed verification is valid 30 minutes (default).", Configurable, Parameter CSF_VERIFICATION_VALID_MINUTES]
  - [R3, "Alert after 3 failed verifications of one client in a day (default).", Configurable, Parameter CSF_VERIFY_MAX_FAILS]
  - [R4, "Checks and the number required are confirmed under CSQ03.", Configurable, LOV CSF_VERIFY_CHECK]
validations:
  - [No check performed, Record the result of the verification checks, "-"]
  - [Verification failed, The caller could not be verified. The contact details cannot be changed, To be confirmed]
fields_screen: Verification
fields:
  - [Channel, List, "Yes", Hotline / E-mail / Website, "-"]
  - [Address matched, Yes / No, "Yes", "-", "-"]
  - [Contact number matched, Yes / No, "Yes", "-", "-"]
  - [E-mail matched, Yes / No, "Yes", "-", "-"]
  - [Insured property matched, Yes / No, "Yes", "-", "-"]
notifications:
  - "CSF_VERIFICATION_FAILED_REPEAT alert to supervisors."
audit:
  - "Each verification with checks, result, agent and time."
acceptance:
  - A verification with 2 of 4 matches passes; with 1 match it fails and the update is not offered.
  - A third failed verification of the same client on one day raises an alert.
```

```fr
id: FR-CSF-021
title: View, add or update client contact details
brd: [BRCSF-004 (p.25), BRCSF-002 (p.24), BRCSF-010 (p.28)]
actor: Contact Center Agent (CSF_CONTACT_UPDATE)
priority: Must have
screens: Update Contact dialog (step 2); Servicing View (Contact History tab)
description:
  - After a passed verification the agent changes the contact details only - e-mail, mobile, phone and address lines - with a reason. BIBS validates the values with the client master rules, saves them once in the BIBS client master, so every BIBS module sees them at once, and records the change with the old and new values, the verification, the agent and the reason (CSF-yyyy-nnnnnn).
  - Other fields (name, civil status, ID) are refused and routed to the fulfilment unit. "Add" contact details means additional e-mails or mobiles with a primary flag if BDOI confirms it (CSQ15).
preconditions:
  - A PASSED verification younger than the validity time exists for the client.
main_flow:
  - The agent edits the contact fields and selects the reason.
  - The agent clicks **Save**; BIBS validates and saves.
  - BIBS shows the confirmation and the change in the Contact History tab.
alternate_flows:
  - Verification expired. BIBS asks for a new verification.
  - Legacy sync enabled. The change is queued for QPS and EBIX (FR-CSF-022).
rules:
  - [R1, "Only e-mail, mobile, phone and address can be changed from the CSF.", Fixed, "-"]
  - [R2, "Change reasons are maintained.", Configurable, LOV CSF_CHANGE_REASON]
  - [R3, "Whether the contacts of open accounts also change is confirmed under CSQ14; client master only by default.", Configurable, Change request after CSQ14]
validations:
  - [No valid verification, Verify the caller before changing the contact details, To be confirmed]
  - [Field not updatable, "<field> cannot be changed here. Refer the client to the fulfilment unit", To be confirmed]
  - [E-mail format invalid, Enter a valid e-mail address, "-"]
  - [Mobile number format invalid, Enter a valid mobile number, "-"]
  - [Reason not selected, Select the reason for the change, "-"]
fields_screen: Update Contact
fields:
  - [E-mail, Text, "No", "-", Valid e-mail]
  - [Mobile, Text, "No", "-", Philippine mobile format]
  - [Phone, Text, "No", "-", Digits and separators]
  - [Address lines, Text, "No", "-", Up to the client master lengths]
  - [Reason, List, "Yes", LOV CSF_CHANGE_REASON, "-"]
notifications:
  - "None."
audit:
  - "Before and after values, source CSF, verification reference, agent, reason and time."
acceptance:
  - A verified agent changes the client's mobile number; the Contact History shows the old and new number.
  - A change without a passed verification is refused.
  - An attempt to change the civil status is refused and names the fulfilment unit.
  - The update is saved within 3 seconds (p.30).
```

```fr
id: FR-CSF-022
title: Queue contact changes for QPS and EBIX while they coexist
brd: [BRCSF-002 (p.24), "Executive summary and envisioned process (p.20, p.22)"]
actor: System
priority: Must have
screens: Servicing View (Contact History, sync status); CSF Contact Changes report
description: The BRD asks that contact updates be sent back to the data sources (QPS and EBIX, p.20). In BIBS the client master is updated at once; the write-back to QPS and EBIX, which matters only while they coexist, goes through an outbox. Until BDOI specifies the interface, each change is stored with the status "not configured" and nothing is sent. When the interface exists, a job sends queued changes every 15 minutes, retries failures and raises an alert.
preconditions:
  - "A contact change was applied."
main_flow:
  - BIBS writes the outbox rows for QPS and EBIX.
  - With sync disabled, the status is NOT_CONFIGURED.
  - With sync enabled, the job sends the rows and sets SENT or FAILED.
rules:
  - [R1, "Sync off until the interface is specified (CSQ01).", Configurable, Parameter CSF_LEGACY_SYNC_ENABLED]
  - [R2, "Every change keeps its content, so it can be sent again.", Fixed, "-"]
validations: []
notifications:
  - "CSF_SYNC_FAILED alert on a failed sending."
audit:
  - "Outbox rows with attempts and last error."
acceptance:
  - With sync disabled, a contact change shows the sync status "Not configured".
  - The CSF Contact Changes report lists the sync status of each change.
```

> [!NOTE] Difference from the BRD
> The BRD describes a front end that reads QPS and EBIX and writes updates back to them. In BIBS the client and account data are in BIBS, so the CSF reads BIBS and the write-back waits for the interface (CSQ01).

## Renewal advice, e-policy and documents

```fr
id: FR-CSF-030
title: View, download and resend the renewal advice
brd: [BRCSF-006 (p.26), BRCSF-006 / 6.001 (p.26), "E-mail topic 8 (p.34)"]
actor: Contact Center Agent (CSF_RESEND); Supervisor (CSF_RESEND_OTHER)
priority: Must have
screens: Servicing View (Renewal Advice tab); Resend dialog
description:
  - The Renewal Advice tab lists the RAs of the client and its accounts - every attachment of document type RENEWAL_ADVICE, whether produced by the Renewal module, Employee Benefits or the submitted policies renewal (decision D3). The agent views and downloads an RA and resends it.
  - The resend goes to the client's registered e-mail. The RA file is password protected and the password is sent in a separate e-mail. A supervisor may send to another address with a reason. The resend is logged.
preconditions:
  - The client has at least one RA.
main_flow:
  - The agent opens the Renewal Advice tab and selects an RA.
  - The agent clicks **Resend**; the dialog shows the recipient and a preview.
  - The agent confirms; BIBS sends the RA and shows a confirmation.
alternate_flows:
  - No RA. The tab shows "No items to display".
  - Other address. A supervisor enters the address and the reason.
rules:
  - [R1, "Default recipient is the registered e-mail.", Fixed, "-"]
  - [R2, "Another address needs CSF_RESEND_OTHER and a reason (CSQ06).", Configurable, Role grants]
  - [R3, "Legacy RAs are viewable only if loaded as RENEWAL_ADVICE documents (CSQ06).", Fixed, "-"]
validations:
  - [No registered e-mail, The client has no registered e-mail. Update the contact details first, To be confirmed]
  - [Other address without reason, Enter the reason for sending to another address, "-"]
  - [Address invalid, Enter a valid e-mail address, EMAIL_ADDRESS_INVALID]
fields_screen: Resend
fields:
  - [Document, Display, "Yes", RA selected, "-"]
  - [Recipient, Text, "Yes", Registered e-mail, Another address only with CSF_RESEND_OTHER]
  - [Reason, Text, Cond., "-", Required for another address]
notifications:
  - "The client receives the RA and, separately, the password."
audit:
  - "Resend with agent, document, recipient and message reference (activity log and outbox)."
acceptance:
  - An agent resends an EB renewal advice to the registered e-mail; the password arrives separately.
  - An agent cannot change the recipient; a supervisor can, with a reason.
  - The Renewal Advice tab lists RAs from both the Renewal and the EB modules.
```

```fr
id: FR-CSF-031
title: Resend the e-policy
brd: [CSF-EM09 (p.34)]
actor: Contact Center Agent (CSF_RESEND)
priority: Must have
screens: Servicing View (E-policies tab); Resend dialog
description: The E-policies tab lists the confirmed e-policies of the client's accounts. The agent resends one to the registered e-mail. BIBS calls the e-policy dispatch service of Issuance, which encrypts the file and sends the password separately, as for the original dispatch. The resend appears in the dispatch report and in the CSF activity log.
preconditions:
  - The account has a confirmed e-policy.
main_flow:
  - The agent selects the e-policy and clicks **Resend**.
  - The agent confirms the recipient.
  - BIBS dispatches it and confirms.
rules:
  - [R1, "Only the confirmed e-policy of the account (CSQ13).", Fixed, "-"]
  - [R2, "Agents resend without the EPOLICY_SEND permission.", Fixed, "-"]
validations:
  - [No confirmed e-policy, The account has no confirmed e-policy, To be confirmed]
notifications:
  - "The client receives the e-policy and, separately, the password."
audit:
  - "Dispatch record and activity log."
acceptance:
  - An agent resends a confirmed e-policy; it appears in the e-policy dispatch report.
  - An account without a confirmed e-policy offers no resend.
```

```fr
id: FR-CSF-032
title: Attach or upload documents
brd: [BRCSF-007 (p.26), BRCSF-007 / 7.001 (p.26-27)]
actor: Contact Center Agent (CSF_DOCUMENT_UPLOAD)
priority: Must have
screens: Servicing View (Documents tab, Upload)
description: The agent uploads documents to the client or to one of its accounts - text files (DOC, DOCX, TXT, RTF), spreadsheets (XLS, XLSX, ODS, CSV), images (PNG, JPG, HEIF / HEIC, GIF, BMP, TIFF, WEBP) and PDF - with a document type. BIBS prompts for the file, checks its type and content, stores it and confirms; the document is then accessible in the Documents tab.
preconditions:
  - "The agent has the Servicing View open."
main_flow:
  - The agent clicks **Upload** and selects the target (client or account) and the document type.
  - The agent chooses the file and confirms.
  - BIBS stores it and shows the confirmation.
alternate_flows:
  - Upload failed. BIBS names the reason; nothing is stored.
rules:
  - [R1, "Seven file types are added to the platform list - TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP.", Fixed, "-"]
  - [R2, "Document types for CSF uploads are maintained.", Configurable, LOV CSF_DOCUMENT_TYPE]
  - [R3, "Maximum file size and virus scanning confirmed under CSQ08.", Configurable, Platform attachment settings]
validations:
  - [File not chosen, Choose the file to upload, "-"]
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
  - [File too large, The file is larger than the allowed size, ATTACHMENT_TOO_LARGE]
  - [Empty file, The file is empty, ATTACHMENT_EMPTY]
  - [Content does not match the type, The file content does not match its type, ATTACHMENT_CONTENT_MISMATCH]
fields_screen: Upload
fields:
  - [Attach to, List, "Yes", Client / accounts of the client, "-"]
  - [Document type, List, "Yes", LOV CSF_DOCUMENT_TYPE, "-"]
  - [File, Attachment, "Yes", "-", Allowed types and size]
notifications:
  - "None."
audit:
  - "Upload with agent, target, file name, size and hash."
acceptance:
  - A HEIF photo uploaded to an account is stored and listed on the Documents tab.
  - A renamed executable with a .pdf extension is refused.
  - A document upload completes within 5 seconds (p.30).
```

```fr
id: FR-CSF-033
title: View, retrieve or download policy-related documents
brd: [BRCSF-009 (p.27), BRCSF-009 / 9.001 (p.27)]
actor: Contact Center Agent
priority: Must have
screens: Servicing View (Documents tab)
description:
  - The Documents tab lists every document linked to the client, its accounts, quotations, placement records and invoices, grouped by type - quotations, renewal advices, e-policies, claims reports (document type CLAIM_REPORT from the Claims module, decision D3) and others. The agent previews a document, downloads it, or downloads a selection as a ZIP file.
  - The attachment access classes decide which document types the CSF roles can list and open (XQ04, CSQ07). Each download is logged.
preconditions:
  - The user has ATTACHMENT_VIEW.
main_flow:
  - The agent searches the client and opens the Documents tab.
  - The agent selects a document and previews or downloads it.
alternate_flows:
  - No documents. The tab shows "No items to display".
  - Document type not allowed for the CSF roles. It is not listed.
rules:
  - [R1, "Which claim documents count as claims reports is confirmed under XQ05.", Configurable, Document access classes]
  - [R2, "Confidentiality matrix per document type confirmed under CSQ07.", Configurable, Document access classes]
validations: []
notifications:
  - "None."
audit:
  - "Each download and ZIP with agent, documents and time."
acceptance:
  - The Documents tab of a client with a claim lists its claims report.
  - A quotation PDF opens in preview.
  - The documents are retrieved within 5 seconds (p.30).
```

## Audit trail, reports and backup

```fr
id: FR-CSF-040
title: Record changes to client information as an audit trail
brd: [BRCSF-010 (p.28)]
actor: System
priority: Must have
screens: Servicing View (Contact History); Audit Trail (Administration)
description: Every change to client information is recorded with the entity, field, old and new values, user, time and source. CSF contact changes also carry the verification reference and the reason. Audit entries can only be added; no user can change or delete them.
preconditions:
  - "None."
main_flow:
  - A user changes client information.
  - BIBS writes the audit entry together with the change; one is never kept without the other.
rules:
  - [R1, "Audit entries can only be added; no user can change or delete them.", Fixed, "-"]
  - [R2, "Retention 5 years online, 15 years archive (p.31).", Configurable, Retention rule CSF_CONTACT_CHANGE]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - A contact change shows in the audit trail with the old and new values and the agent.
  - No screen allows an audit row to be changed.
```

```fr
id: FR-CSF-041
title: Print or save the audit trail and the CSF reports
brd: [BRCSF-011 (p.28), BRCSF-011 / 11.002 (p.29)]
actor: Contact Center Management (AUDIT_VIEW, CSF_REPORT_VIEW)
priority: Must have
screens: Administration, Audit Trail (CTL-AUDIT); Reports (category Customer Service)
description: Management accesses the audit trail filtered on client records, and the Contact Changes report (client, field, old and new value, verification result, agent, reason, legacy sync status). They select print or save in Excel or PDF and confirm; BIBS produces the file.
preconditions:
  - "The user has AUDIT_VIEW or CSF_REPORT_VIEW."
main_flow:
  - The manager opens the report and sets the date range and filters.
  - The manager selects PDF or Excel and confirms.
  - BIBS produces the file for printing or saving.
rules:
  - [R1, "Export formats PDF and Excel (also CSV).", Fixed, "-"]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each run and export with user and parameters."
acceptance:
  - The Contact Changes report for a month exports to Excel and PDF with the same rows.
  - An agent cannot run the audit trail report.
  - The report is generated within 5 seconds (p.30).
```

```fr
id: FR-CSF-042
title: Log agent activity for leads and heads
brd: ["Usage requirements - report generation by Leads / Heads (p.30)", BRCSF-010 (p.28)]
actor: System; Supervisor, Management (CSF_REPORT_VIEW)
priority: Must have
screens: Reports (CSF-ACTIVITY)
description: BIBS logs every agent action in the CSF - searches (with the criteria), Servicing Views opened, downloads, RA and e-policy resends, uploads and contact changes. The Agent Activity report shows counts per agent and day and the detail, for supervision and for the leads' and heads' reports.
preconditions:
  - "None."
main_flow:
  - Agents work in the CSF; BIBS writes an activity row per action.
  - The supervisor runs the Agent Activity report for a period.
rules:
  - [R1, "Every CSF action is logged; the log cannot be switched off.", Fixed, "-"]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "No entry of the activity log can be changed or deleted."
acceptance:
  - An agent's search, view and resend appear in the Agent Activity report for that day.
```

```fr
id: FR-CSF-043
title: Back up the data every 15 minutes
brd: [BRCSF-011 / 11.001 (p.28)]
actor: Infrastructure (System)
priority: Must have
screens: None
description: BIBS data is backed up continuously - the changes are saved at least every 15 minutes to the designated secure storage, plus a daily full backup. Each backup is time stamped, complete and restorable, and is tested monthly by a restore. Saving the changes does not slow down the online response times. The requirement applies to all BIBS data, not only to CSF data. The backup set-up is specified in the Technical Specification, reviewed by BDOI IT.
preconditions:
  - "None."
main_flow:
  - The changes are saved to the backup storage at least every 15 minutes.
  - The daily base backup runs outside the peak hours.
  - A monthly restore test proves the backups.
rules:
  - [R1, "Archive interval at most 15 minutes.", Configurable, Infrastructure setting (backup interval)]
  - [R2, "Whether the 15-minute backup is BIBS-wide (RPO 15 minutes) is confirmed under CSQ11 and XQ08.", Configurable, Infrastructure decision]
validations: []
notifications:
  - "A failed archive or backup alerts the infrastructure team."
audit:
  - "Backup and restore test logs kept by the infrastructure team."
acceptance:
  - A restore test recovers BIBS data to a point less than 15 minutes before the test.
  - Online response times during archiving stay within the targets of section 8.
```

## Case management

The CSF Addendum - Case Management (pages 1-17) brings a Case Management module into the CSF. It replaces the SharePoint tracker used today (p.3-4). Every inquiry, request, problem, complaint, follow-up or commendation is logged as a case, routed to the fulfilment unit that resolves it, tracked against the turnaround time of its case type and closed by the fulfilment unit or by the agent. The FRs below specify the module in full. Its delivery drop and plan are a decision of BDOI and the Change Control Board (CLR-CSF-07); the points of the addendum that need a BDOI answer are listed in chapter 12 with the proposed rule that applies until then.

The case screens are reached from the menu section Customer Service Facility:

- **Cases**: the work queues - My Cases, Returned to Me, CSR Action Required, Team Queue (supervisors), Fulfilment Unit Queue (fulfilment units), All Open and Closed - as tables with a row action menu (Open, Add Note, Reassign, Escalate, Close).
- **Log Case** dialog: opened from the Servicing View of a client (button **Log Case** and the Cases tab) or from the Cases list for a caller who is not a client.
- **Case record**: a header with the Case ID, the status, the case type with its turnaround time, the due date and the SLA indicator, the client and account, the fulfilment unit and the owner; tabs Details, Notes, Documents, Linked Cases and History.
- **Case Dashboard**: the supervisor and management views of BRCSFCM.008 and BRCSFCM.011.

```fr
id: FR-CSF-050
title: Log a customer transaction as a case
brd: [BRCSFCM.001 (p.5), "Envisioned process IV.1-2 (p.4)"]
actor: Contact Center Agent (CSF_CASE_CREATE)
priority: Must have
screens: Servicing View (Log Case; Cases tab); Log Case dialog; Case record
description:
  - Every inquiry, whatever the channel (phone, e-mail, e-mail through the BDO website), is logged as a case. The agent clicks **Log Case** on the Servicing View of the client; the dialog takes the client and, where the inquiry is about a policy, the account from the Servicing View. The agent selects the channel, the case type, the nature of transaction, the product type, the market segment and the fulfilment unit from the case lists of values (FR-CSF-061), enters the subject and the description of the request, and either resolves the case at once or submits it to the fulfilment unit.
  - On submission BIBS gives the case a unique Case ID (CASE-yyyy-nnnnnn), links it to the client and the account, computes the due date from the turnaround time of the case type (FR-CSF-051), records the creator, date and time in the case audit trail (FR-CSF-055) and notifies the recipients of the case (FR-CSF-060) with the Case ID, the policy number, the case type, the nature of transaction and the due date.
  - BIBS refuses a second open case for the same transaction - the same client, account and nature of transaction - and opens the existing case, where the agent adds a note instead (FR-CSF-053).
  - A caller who is not a client of BIBS (for example a misrouted call or a directory request) is logged from the Cases list with the caller's name and contact number or e-mail; the client and account are then left blank (CLR-CSF-11).
preconditions:
  - The user has CSF_CASE_CREATE.
  - "For a client case: the client's Servicing View is open."
main_flow:
  - The agent clicks **Log Case**.
  - BIBS opens the Log Case dialog with the client, the account list of the client and the case lists of values; it proposes the market segment of the account and the fulfilment unit of the routing table (FR-CSF-060).
  - The agent completes the mandatory fields, the subject and the description.
  - The agent clicks **Submit to Fulfilment Unit**, or **Resolve and Close** when the inquiry was answered on the call.
  - BIBS validates the fields and the duplicate rule, creates the case with its Case ID and shows the confirmation with the Case ID and the due date.
  - The case shows on the Cases tab of the client and in the queue of the fulfilment unit (status Open - With Fulfillment Unit) or in the Closed list (status Closed by CSR).
alternate_flows:
  - Open case for the same transaction. BIBS names the open case and offers **Open case**; no new case is created.
  - Resolve and Close. The resolution is mandatory; the case is created with the status Closed by CSR and the fulfilment unit receives no notification.
  - Caller not a client. The agent logs the case from the Cases list; the caller name and one contact are mandatory.
rules:
  - [R1, "Case IDs are CASE-yyyy-nnnnnn, numbered by year.", Configurable, Parameter CSF_CASE_PREFIX]
  - [R2, "Mandatory fields - channel, case type, nature of transaction, product type, market segment and fulfilment unit - come from the case lists of values.", Fixed, "-"]
  - [R3, "Duplicate rule - one open case per client, account and nature of transaction (CLR-CSF-11).", Configurable, Parameter CSF_CASE_DUPLICATE_CHECK]
  - [R4, "The market segment is proposed from the account and the fulfilment unit from the routing table; the agent may change both.", Configurable, Case routing table (FR-CSF-060)]
validations:
  - [Mandatory field blank, Complete the mandatory fields - <fields>, "-"]
  - [Open case for the same transaction, "Case <Case ID> is already open for this client, account and nature of transaction. Add a note to it instead", To be confirmed]
  - [Resolve and Close without resolution, Enter the resolution given to the client, "-"]
  - [Caller not a client without name or contact, Enter the caller's name and a contact number or e-mail, "-"]
  - [Inactive list value, The value <value> is no longer in use. Select another value, To be confirmed]
fields_screen: Log Case
fields:
  - [Client, Display, Cond., Servicing View, Blank only for a caller who is not a client]
  - [Caller name, Text, Cond., "-", Mandatory when there is no client]
  - [Caller contact, Text, Cond., "-", Mobile or e-mail; mandatory when there is no client]
  - [Account / policy, List, "No", Accounts of the client, "-"]
  - [Channel, List, "Yes", LOV CSF_CASE_CHANNEL, Active values only]
  - [Case type, List, "Yes", LOV CSF_CASE_TYPE, Shows the turnaround time]
  - [Nature of transaction, List, "Yes", LOV CSF_CASE_NATURE, Searchable list]
  - [Product type, List, "Yes", LOV CSF_CASE_PRODUCT, "-"]
  - [Market segment, List, "Yes", LOV CSF_CASE_SEGMENT, Proposed from the account]
  - [Fulfilment unit, List, "Yes", LOV CSF_FULFILMENT_UNIT, Proposed from the routing table]
  - [Priority, List, "Yes", LOV CSF_CASE_PRIORITY, Default Normal]
  - [Subject, Text, "Yes", "-", Up to 150 characters]
  - [Description, Text area, "Yes", "-", "Up to 4,000 characters"]
  - [Resolution, Text area, Cond., "-", Mandatory for Resolve and Close]
notifications:
  - "On submission to the fulfilment unit: in-app notice and e-mail to the members of the fulfilment unit, with a copy to the account officer of the account, giving the Case ID, policy number, case type, nature of transaction and due date."
audit:
  - "Case creation with the creator, date, time and every field value (FR-CSF-055); activity log row for the agent (FR-CSF-042)."
acceptance:
  - A case submitted with all mandatory fields gets a unique Case ID, is linked to the client and the account and shows in the queue of the fulfilment unit with the status Open - With Fulfillment Unit.
  - A case with a blank mandatory field is refused and the missing fields are named.
  - A second case for the same client, account and nature of transaction is refused while the first is open, and the open case is named.
  - The creation is recorded in the case history with the creator, date and time.
  - The members of the fulfilment unit receive the notification with the Case ID and the policy number.
  - A case resolved on the call is created with the status Closed by CSR and its resolution.
```

```fr
id: FR-CSF-051
title: Categorise a case by case type and turnaround time
brd: [BRCSFCM.002 (p.5), "List of values II Case Type and TAT (p.11)"]
actor: Contact Center Agent; Contact Center Supervisor (CSF_CASE_SUPERVISE)
priority: Must have
screens: Log Case dialog; Case record (Details tab, Change Category)
description:
  - The agent selects the case type from the predefined list - Inquiry, Request, Request (1-day), Problem, Complaint, Follow-up, Commendation. The case type carries the turnaround time (TAT) of the list (Inquiry 3, Request 4, Request (1-day) 1, Problem 6, Complaint 6, Follow-up 1, Commendation 6), counted in working days from the creation of the case (CLR-CSF-09). The case type sets the due date, the SLA thresholds (FR-CSF-057) and, with the market segment and the nature of transaction, the routing (FR-CSF-060).
  - The category of a submitted case is changed only by a supervisor, with a reason, through **Change Category**. BIBS recomputes the due date from the creation time with the turnaround time of the new type, keeps the old and new values in the history and notifies the owner and the fulfilment unit.
preconditions:
  - "For a change: the case is open and the user has CSF_CASE_SUPERVISE."
main_flow:
  - The agent selects the case type when logging the case; BIBS shows its turnaround time and the due date.
  - To change it, the supervisor opens the case, clicks **Change Category**, selects the new case type or nature of transaction and the reason, and confirms.
  - BIBS saves the change, recomputes the due date and logs the change.
alternate_flows:
  - Closed case. Change Category is not offered.
rules:
  - [R1, "Case types and their turnaround times are maintained by Contact Center Management.", Configurable, LOV CSF_CASE_TYPE (turnaround time per value)]
  - [R2, "Turnaround time in working days from the creation of the case, on the BIBS holiday calendar (CLR-CSF-09).", Configurable, Parameter CSF_CASE_TAT_UNIT]
  - [R3, "Only CSF_CASE_SUPERVISE changes the category of a submitted case; a reason is mandatory.", Fixed, "-"]
  - [R4, "A new case type recomputes the due date from the creation time, not from the change.", Fixed, "-"]
validations:
  - [Case type blank, Select the case type, "-"]
  - [Change without reason, Enter the reason for changing the category, "-"]
  - [Change by a user without the right, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: Change Category
fields:
  - [Case type, List, "Yes", LOV CSF_CASE_TYPE, Shows the turnaround time]
  - [Nature of transaction, List, "Yes", LOV CSF_CASE_NATURE, "-"]
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - "A change of category notifies the owner and the fulfilment unit of the case with the new due date."
audit:
  - "Category changes with the old and new values, the user, the reason and the time."
acceptance:
  - An Inquiry logged on a Monday without holidays is due on the Thursday (3 working days).
  - An agent cannot change the category of a submitted case; a supervisor can, with a reason.
  - Changing an Inquiry to a Complaint moves the due date to 6 working days from the creation and records the old and new type.
  - The list of case types offers only the active values of the list, each with its turnaround time.
```

```fr
id: FR-CSF-052
title: Manage the case lifecycle statuses
brd: [BRCSFCM.003 (p.5-6), "Envisioned process IV.3 (p.4)"]
actor: System; Contact Center Agent; Fulfilment Unit
priority: Must have
screens: Case record (header and History tab); Cases list
description:
  - A case moves through the statuses of BRCSFCM.003 - Open - With Fulfillment Unit, Open - Returned to Agent, Open - CSR Action Required, Closed by Fulfillment Unit and Closed by CSR - by the actions of section 5.3 only. Each status change is time stamped, logged with the user, and shown at all times in the case header and the Cases lists. A transition that the table of section 5.3 does not allow is refused.
  - The envisioned process names the stages Open, In Progress, Pending, Escalated and Closed (p.4). BIBS shows them as the status group of each status, and escalation as an indicator on the open case (FR-CSF-056), so that both lists of the addendum are met (CLR-CSF-08).
preconditions:
  - "None."
main_flow:
  - A user takes an action on the case (submit, return, request CSR action, re-endorse, close).
  - BIBS checks the action against the allowed transitions of the current status.
  - BIBS saves the new status with the time and the user and shows it in the header.
alternate_flows:
  - Transition not allowed. The action is not offered; a direct request is refused.
rules:
  - [R1, "Allowed transitions are those of section 5.3.", Fixed, "-"]
  - [R2, "Closed by Fulfillment Unit and Closed by CSR are final; a follow-up is a new linked case (FR-CSF-058).", Fixed, "-"]
  - [R3, "Status labels and status groups are maintained; the transitions are not.", Configurable, LOV CSF_CASE_STATUS]
validations:
  - [Action not allowed in the current status, "The case is <status>. This action is not allowed", To be confirmed]
notifications:
  - "Each status change notifies the next owner: the fulfilment unit on Open - With Fulfillment Unit, the creating agent on Open - Returned to Agent and Open - CSR Action Required."
audit:
  - "Status changes with the from and to status, the user and the time (FR-CSF-055)."
acceptance:
  - A case returned by the fulfilment unit shows Open - Returned to Agent in its header and in the Returned to Me queue of the agent.
  - An attempt to return a case that is Closed by CSR is refused.
  - Every status change is listed in the History tab with the time and the user.
  - The Cases list shows the current status and the status group of every case.
```

```fr
id: FR-CSF-053
title: Add notes and updates to a case
brd: [BRCSFCM.004 (p.6)]
actor: Contact Center Agent; Contact Center Supervisor; Fulfilment Unit
priority: Must have
screens: Case record (Notes tab, Add Note)
description: Throughout the life of a case the users who work on it add notes - the client's follow-up call, the information given, the action taken. Notes are listed in date order with the author, date and time, cannot be changed or deleted once saved, and are visible only to the users who have access to the case (the CSF roles and the members of the fulfilment unit of the case). A closed case accepts no notes; a follow-up goes to a linked case (FR-CSF-058).
preconditions:
  - The case is open and the user has access to it.
main_flow:
  - The user opens the Notes tab and clicks **Add Note**.
  - The user writes the note and clicks **Save**.
  - BIBS saves the note with the author, date and time and lists it in date order.
alternate_flows:
  - Closed case. Add Note is not offered.
rules:
  - [R1, "Notes are append-only - no edit, no delete.", Fixed, "-"]
  - [R2, "Notes are listed oldest first, with author, date and time.", Fixed, "-"]
  - [R3, "Notes are visible to the CSF roles and to the fulfilment unit of the case only.", Fixed, "-"]
validations:
  - [Note blank, Enter the note, "-"]
  - [Note on a closed case, "The case is <status>. This action is not allowed", To be confirmed]
fields_screen: Add Note
fields:
  - [Note, Text area, "Yes", "-", "Up to 4,000 characters"]
notifications:
  - "A note by the fulfilment unit notifies the owning agent; a note by the agent notifies the fulfilment unit."
audit:
  - "Each note with the author and time (FR-CSF-055)."
acceptance:
  - Two notes added by different users are listed in date order with their author, date and time.
  - A saved note offers no edit or delete action.
  - A user of another fulfilment unit cannot open the case or its notes.
```

```fr
id: FR-CSF-054
title: Attach supporting documents to a case
brd: [BRCSFCM.005 (p.6)]
actor: Contact Center Agent; Fulfilment Unit
priority: Must have
screens: Case record (Documents tab, Upload)
description: The agent and the fulfilment unit attach supporting documents to a case - e-mails from the client, proofs of payment, letters, photos. The upload follows the file types, size limit and content checks of FR-CSF-032. Each document is linked to the case and to its client, so it also shows on the Documents tab of the Servicing View with the Case ID. Access follows the case access of FR-CSF-053 and the document access classes; every upload, view and download is logged.
preconditions:
  - The case is open and the user has access to it.
main_flow:
  - The user opens the Documents tab of the case and clicks **Upload**.
  - The user selects the document type and the file and confirms.
  - BIBS checks the file, stores it against the case and the client and lists it.
alternate_flows:
  - File refused. BIBS names the reason; nothing is stored.
  - Closed case. Upload is not offered; the documents stay viewable.
rules:
  - [R1, "File types, size limit and content checks of FR-CSF-032.", Configurable, Platform attachment settings]
  - [R2, "A case document is linked to the case and to the client of the case.", Fixed, "-"]
validations:
  - [File not chosen, Choose the file to upload, "-"]
  - [File type not allowed, The file type is not allowed, ATTACHMENT_TYPE_NOT_ALLOWED]
  - [File too large, The file is larger than the allowed size, ATTACHMENT_TOO_LARGE]
  - [Content does not match the type, The file content does not match its type, ATTACHMENT_CONTENT_MISMATCH]
fields_screen: Upload (case)
fields:
  - [Document type, List, "Yes", LOV CSF_DOCUMENT_TYPE, "-"]
  - [File, Attachment, "Yes", "-", Allowed types and size]
notifications:
  - "None."
audit:
  - "Upload, view and download with the user, document and time (FR-CSF-055)."
acceptance:
  - A PDF uploaded to a case is listed on the case and on the Documents tab of the client with the Case ID.
  - A file type that is not allowed is refused with its message.
  - The upload and a later download of the document appear in the case history.
```

```fr
id: FR-CSF-055
title: Keep a complete audit trail of case actions
brd: [BRCSFCM.006 (p.6)]
actor: System
priority: Must have
screens: Case record (History tab); Administration, Audit Trail
description: BIBS logs every case action automatically - creation, updates of fields, status changes, priority changes, category changes, reassignments, escalations, notes and attachments - each with the from value, the to value, the user, the action and the date and time. The History tab of the case lists them; the Audit Trail report of management (FR-CSF-041) can be filtered on cases. Audit records are read only and kept for the retention period of the CSF records.
preconditions:
  - "None."
main_flow:
  - A user acts on a case.
  - BIBS writes the audit record together with the action; one is never kept without the other.
rules:
  - [R1, "Audit records can only be added; no user can change or delete them.", Fixed, "-"]
  - [R2, "Retention 5 years online and 15 years in the archive, as the other CSF records (p.31; CSQ21).", Configurable, Retention rule CSF_CASE]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - A reassignment shows in the History tab with the from and to owner, the user and the time.
  - A priority change shows the old and new priority.
  - No screen allows an audit record of a case to be changed or deleted.
  - The Audit Trail report filtered on a Case ID lists every action of the case.
```

```fr
id: FR-CSF-056
title: Reassign or escalate a case
brd: [BRCSFCM.007 (p.7)]
actor: Contact Center Supervisor (CSF_CASE_SUPERVISE)
priority: Must have
screens: Cases (Team Queue); Case record (Reassign, Escalate, Close)
description:
  - The supervisor reassigns an open case to another agent or to another team (fulfilment unit), maintains the resolution and closes the case where needed. The new owner sees the case in the queue at once.
  - The supervisor escalates an open case with an escalation reason (for example Past TAT, Due for Resolution) to the escalation group of the fulfilment unit. The case keeps its status and shows the Escalated indicator with the reason and the date; the escalation is reflected in the case header, the queues and the dashboard. Every reassignment and escalation is logged.
preconditions:
  - The case is open and the user has CSF_CASE_SUPERVISE.
main_flow:
  - The supervisor opens the case or selects it in the Team Queue.
  - The supervisor clicks **Reassign**, selects the agent or the fulfilment unit and enters the reason; or clicks **Escalate** and selects the escalation reason.
  - BIBS changes the owner or sets the Escalated indicator at once, notifies the new owner or the escalation group, and logs the action.
alternate_flows:
  - Close by supervisor. The supervisor enters the resolution and closes the case as Closed by CSR.
rules:
  - [R1, "Escalation reasons are maintained.", Configurable, LOV CSF_ESCALATION_REASON]
  - [R2, "Each fulfilment unit has an escalation group; default the Contact Center Supervisors.", Configurable, Case routing table (FR-CSF-060)]
  - [R3, "An escalation does not change the status; it sets the Escalated indicator until the case is closed.", Fixed, "-"]
validations:
  - [Reassign without new owner, Select the agent or the fulfilment unit, "-"]
  - [Escalate without reason, Select the escalation reason, "-"]
  - [Action by a user without the right, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: Reassign / Escalate
fields:
  - [Reassign to, List, Cond., Active agents and fulfilment units, Mandatory for Reassign]
  - [Escalation reason, List, Cond., LOV CSF_ESCALATION_REASON, Mandatory for Escalate]
  - [Comment, Text, "No", "-", "-"]
notifications:
  - "Reassignment notifies the new owner; escalation notifies the escalation group and the current owner."
audit:
  - "Reassignments and escalations with the from and to values, reason, user and time."
acceptance:
  - A case reassigned from one agent to another shows in the new agent's My Cases at once and leaves the old agent's queue.
  - An escalation without a reason is refused.
  - An escalated case shows the Escalated indicator with the reason in its header, its queue row and the dashboard.
  - An agent does not see Reassign or Escalate.
```

```fr
id: FR-CSF-057
title: Track case ageing and SLA thresholds
brd: [BRCSFCM.008 (p.7)]
actor: System; Contact Center Supervisor
priority: Must have
screens: Case record (header SLA indicator); Cases lists; Case Dashboard
description:
  - BIBS computes the age of every case from its creation time and its SLA status against the turnaround time of its case type - Within TAT, Approaching breach (from the near-breach threshold) and Breached. The SLA indicator shows on the case header, on each queue row and on the supervisor dashboard.
  - BIBS notifies, automatically and with a log entry, the assigned agent, the supervisor, the fulfilment unit and the escalation group when a case approaches its breach and when it breaches. A case that breaches is escalated automatically to the escalation group with the reason Past TAT where the case type is set for automatic escalation.
  - The SLA and ageing data feed the dashboard (SLA compliance rate, average ageing, breached versus non-breached cases) and the reports of FR-CSF-059.
preconditions:
  - "None."
main_flow:
  - A scheduled check runs every 15 minutes (default) on the open cases.
  - BIBS updates the SLA status of each case and sends the near-breach and breach notifications once per case and threshold.
  - BIBS sets the Escalated indicator on breached cases of the case types set for automatic escalation.
rules:
  - [R1, "Ageing runs from the creation time; the clock does not stop while the case waits for the client (CLR-CSF-09).", Configurable, Parameter CSF_CASE_SLA_PAUSE]
  - [R2, "Near-breach threshold - 1 working day before the due date (default).", Configurable, Parameter CSF_CASE_NEAR_BREACH_DAYS]
  - [R3, "Automatic escalation on breach per case type (default on).", Configurable, LOV CSF_CASE_TYPE (auto-escalate flag)]
  - [R4, "Check interval 15 minutes (default).", Configurable, Job CSF_CASE_SLA]
validations: []
notifications:
  - "Approaching breach and breached - in-app notice and e-mail to the assigned agent, the supervisor, the fulfilment unit and the escalation group, each logged."
audit:
  - "SLA status changes, notifications sent and automatic escalations (FR-CSF-055)."
acceptance:
  - A case one working day before its due date shows Approaching breach and the four recipients are notified once.
  - A case past its due date shows Breached on the case, the queue and the dashboard.
  - A breached Complaint is escalated automatically with the reason Past TAT.
  - The age of a case is counted from its creation time.
```

```fr
id: FR-CSF-058
title: Close a case and create a linked follow-up case
brd: [BRCSFCM.010 (p.7-8)]
actor: Contact Center Agent; Fulfilment Unit; Contact Center Supervisor
priority: Must have
screens: Case record (Close; Create Follow-up Case; Linked Cases tab)
description:
  - An authorised user closes a case with a closure status - Closed by Fulfillment Unit (fulfilment unit) or Closed by CSR (agent or supervisor) - the resolution and a closure reason. A closed case is read only.
  - When client information or a client action is still pending at closure, the user creates a new case from the closed one with **Create Follow-up Case**. The new case copies the client, the account and the categories, is linked to the original case by its Case ID and records the reason for closure and re-creation. Both cases show the link on their Linked Cases tab - the original (closed) case, the new (active) case and the reason.
preconditions:
  - "For closure: the case is open and the user may close it in its status (section 5.3)."
  - "For a follow-up: the case is closed."
main_flow:
  - The user clicks **Close**, enters the resolution and selects the closure reason; BIBS sets the closure status and makes the case read only.
  - Where an item is pending, the user clicks **Create Follow-up Case** on the closed case and enters the reason.
  - BIBS creates the new case with its own Case ID and due date, links both cases and opens the new case.
alternate_flows:
  - Close without resolution. Refused.
rules:
  - [R1, "A closed case cannot be changed, reopened or noted; only a follow-up case can be created from it.", Fixed, "-"]
  - [R2, "Closure reasons are maintained.", Configurable, LOV CSF_CLOSURE_REASON]
  - [R3, "A follow-up case gets a new due date from its own creation time.", Fixed, "-"]
validations:
  - [Close without resolution, Enter the resolution given to the client, "-"]
  - [Close without reason, Select the closure reason, "-"]
  - [Follow-up without reason, Enter the reason for the follow-up case, "-"]
fields_screen: Close / Create Follow-up Case
fields:
  - [Resolution, Text area, "Yes", "-", "For Close"]
  - [Closure reason, List, "Yes", LOV CSF_CLOSURE_REASON, "For Close"]
  - [Follow-up reason, Text, "Yes", "-", For Create Follow-up Case]
notifications:
  - "Closure by the fulfilment unit notifies the owning agent; a follow-up case notifies its fulfilment unit as a new case."
audit:
  - "Closure and follow-up creation with the user, reason, time and both Case IDs."
acceptance:
  - A closed case offers no edit, note, upload or status action.
  - A follow-up case created from a closed case has a new Case ID and shows the link to the original case, and the original case shows the link to the new case with the reason.
  - A case cannot be closed without the resolution and the closure reason.
```

```fr
id: FR-CSF-059
title: Produce case management reports and dashboards
brd: [BRCSFCM.011 (p.8), BRCSFCM.008 (p.7), "Envisioned process IV.4-5 (p.4)"]
actor: Contact Center Management; Contact Center Supervisor (CSF_CASE_REPORT)
priority: Must have
screens: Case Dashboard; Reports (category Customer Service)
description:
  - The Case Dashboard shows, for the period and filters chosen, the case volume, the ageing (average and by ageing band), the SLA compliance rate, breached versus non-breached cases and the distribution by status, by case type, by nature of transaction and by fulfilment unit. Supervisors see the open cases of their teams with the SLA indicators and work from them to the case.
  - The case reports give the same figures from the same records - the dashboard and the reports are views of one set of data, not a separate system (revision log, p.2). Reports and dashboards filter by date, case type, priority, status, market segment and fulfilment unit; the reports export to Excel, CSV and PDF. Only users with CSF_CASE_REPORT open the dashboard or run and export the reports.
preconditions:
  - The user has CSF_CASE_REPORT.
main_flow:
  - The user opens the Case Dashboard and sets the period and filters; BIBS shows the figures.
  - The user opens a figure to see the cases behind it.
  - The user runs a case report with the same filters and exports it.
rules:
  - [R1, "Reports - Case Register (CSF-CASES), Case Ageing and SLA (CSF-CASE-SLA), Case Summary (CSF-CASE-SUMMARY); section 6.1.", Fixed, "-"]
  - [R2, "Export formats Excel, CSV and PDF.", Fixed, "-"]
  - [R3, "SLA compliance rate = cases closed within TAT / cases closed in the period.", Fixed, "-"]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each report run and export with the user and the filters."
acceptance:
  - The dashboard and the Case Summary report for the same month and filters show the same case volume and SLA compliance rate.
  - The Case Register exports to Excel, CSV and PDF with the same rows.
  - An agent has no Case Dashboard and cannot run the case reports.
  - The dashboard filters by date, case type, priority and status.
```

```fr
id: FR-CSF-060
title: Route, resolve and return a case by the Fulfilment Unit
brd: [BRCSFCM.012 (p.8), "Envisioned process IV.6 (p.4)", "Current process III.6 (p.4)"]
actor: Fulfilment Unit (CSF_CASE_FULFIL); System
priority: Must have
screens: Cases (Fulfilment Unit Queue); Case record (Resolve and Close, Return to Agent, Request CSR Action)
description:
  - BIBS routes each submitted case to the fulfilment unit selected on the case. The fulfilment unit is proposed from a routing table maintained by Contact Center Management - case type, market segment and nature of transaction to fulfilment unit - so routing is system-driven; the account officer and the unit of the account receive a copy of the notification, so the BU/AO ownership of the account is kept.
  - The members of the fulfilment unit see the case in the Fulfilment Unit Queue, maintain the resolution in the case and close it (Closed by Fulfillment Unit). If the case has an error or needs more handling by the contact centre, they return it with a reason (Open - Returned to Agent). If the resolution is done but the agent must act with the client - call back, collect a document - they request CSR action with the instruction (Open - CSR Action Required).
preconditions:
  - The user is a member of the fulfilment unit of the case and has CSF_CASE_FULFIL.
main_flow:
  - The case arrives in the Fulfilment Unit Queue with the status Open - With Fulfillment Unit.
  - A member opens it, records the resolution and clicks **Resolve and Close**.
  - BIBS sets Closed by Fulfillment Unit and notifies the owning agent.
alternate_flows:
  - Return to Agent. The member selects the return reason and writes the comment; the case goes to the agent's Returned to Me queue. The agent corrects the case and clicks **Re-endorse to Fulfilment Unit**, or closes it as Closed by CSR.
  - Request CSR Action. The member writes the instruction; the case goes to the agent's CSR Action Required queue. The agent acts and closes the case as Closed by CSR.
  - Wrong unit. The member returns the case with the reason Wrong fulfilment unit; the supervisor or the agent re-routes it.
rules:
  - [R1, "Routing table - case type, market segment and nature of transaction to fulfilment unit and escalation group.", Configurable, Case routing table (Contact Center Management)]
  - [R2, "Each fulfilment unit lists its member users; a user may belong to several units.", Configurable, Fulfilment unit membership (User Access request)]
  - [R3, "Return reasons are maintained.", Configurable, LOV CSF_RETURN_REASON]
  - [R4, "A member sees only the cases of his fulfilment units.", Fixed, "-"]
validations:
  - [Resolve without resolution, Enter the resolution given to the client, "-"]
  - [Return without reason, Select the return reason, "-"]
  - [Case of another fulfilment unit, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: Resolve / Return / Request CSR Action
fields:
  - [Resolution, Text area, Cond., "-", Mandatory for Resolve and Close]
  - [Return reason, List, Cond., LOV CSF_RETURN_REASON, Mandatory for Return to Agent]
  - [Instruction to the agent, Text area, Cond., "-", Mandatory for Request CSR Action]
notifications:
  - "Routing notifies the members of the fulfilment unit, with a copy to the account officer of the account; resolution, return and CSR action notify the owning agent."
audit:
  - "Routing, resolution, return and CSR action with the user, reason and time."
acceptance:
  - A Request case for a Consumer 1 Motor account is proposed to the fulfilment unit of the routing table and shows in that unit's queue.
  - A member of the fulfilment unit resolves and closes the case; the agent is notified and the case shows Closed by Fulfillment Unit.
  - A returned case shows Open - Returned to Agent in the agent's Returned to Me queue with the reason.
  - A member of another fulfilment unit cannot see the case.
```

```fr
id: FR-CSF-061
title: Maintain the case lists of values and the routing table
brd: ["Lists of values I-VI (p.11-17)", "Envisioned process IV.2, IV.5 (p.4)", BRCSFCM.002 (p.5)]
actor: System Administrator, at the request of Contact Center Management
priority: Must have
screens: Broking Setup > Lists of Values; Customer Service Facility > Case Routing
description:
  - The case lists of values of the addendum are loaded at go-live and maintained without a release - Channel (3 values), Case Type with its turnaround time (7), Nature of Transaction (125), Product Type (51), Market Segment (33) and Fulfillment Unit (53) - together with the lists that the FRs add - case priority, escalation reason, return reason, closure reason and case status labels. A value is deactivated, not deleted, so that existing cases keep it.
  - The routing table links the case type, market segment and nature of transaction to the fulfilment unit and its escalation group (FR-CSF-060). The market segments and product types of the case lists are the contact centre's own lists; each may carry the BIBS market segment or product line it corresponds to, for reporting across modules (CLR-CSF-10).
preconditions:
  - The user has LOV_MANAGE (lists) or CSF_CASE_ROUTING (routing table).
main_flow:
  - Contact Center Management sends the change; the System Administrator adds, changes or deactivates the value or the routing row.
  - BIBS saves the change with its effective date and logs it.
rules:
  - [R1, "List values are deactivated, never deleted, once used on a case.", Fixed, "-"]
  - [R2, "Each list value may carry the corresponding BIBS market segment or product line (CLR-CSF-10).", Configurable, LOV CSF_CASE_SEGMENT and CSF_CASE_PRODUCT]
validations:
  - [Duplicate value in a list, The value <value> already exists in the list, To be confirmed]
  - [Turnaround time blank or not a whole number, Enter the turnaround time in whole working days, "-"]
  - [Routing row duplicate, "A routing row already exists for this case type, market segment and nature of transaction", To be confirmed]
notifications:
  - "None."
audit:
  - "Changes to lists and routing rows with the old and new values, the user and the time."
acceptance:
  - The Log Case dialog offers the 3 channels, 7 case types, 125 natures of transaction, 51 product types, 33 market segments and 53 fulfilment units of the addendum.
  - A deactivated nature of transaction is no longer offered and still shows on the cases that used it.
  - A routing row added for a case type and market segment proposes its fulfilment unit on the next case logged.
```


# Status and state model

The CSF has no approval workflow. Three models apply: the CSF status of an account, the states of a contact change and the lifecycle of a case.

## CSF status of an account

<!-- table: widths=2.6,7.8,6.2 caption="CSF status mapping (seed of LOV CSF_STATUS_MAP; definitions to confirm, CSQ04)" -->
| CSF status | BIBS account stage and payment status | Source |
|---|---|---|
| Pending | DRAFT, SUBMITTED, RETURNED_TO_MARKETING | BRCSF-005 / 5.001 |
| Awaiting | AWAITING_PAYMENT, READY_FOR_PLACEMENT, PLACED, RETURNED_BY_INSURER | BRCSF-005 / 5.001 |
| Booked | BOOKED with premium receivable outstanding | CSF-EM07 |
| Open | BOOKED and in force | CSF-EM07 |
| Closed | CANCELLED, VOIDED, or policy period ended | CSF-EM07 |

The BRD does not define Pending and Awaiting; the e-mail says the statuses follow the definitions of Marketing, Processing and Operations. The mapping is data, so the agreed definitions are applied without a change to the system.

## Contact change and legacy sync

Figure 3 shows the states of a contact change. A verification passes or fails; a passed verification allows the change for 30 minutes (default). An applied change is queued for QPS and EBIX only when the legacy sync is enabled.

<!-- table: widths=3.4,13.2 caption="Contact change states" -->
| State | Meaning |
|---|---|
| PASSED / FAILED | Result of the verification checklist (FR-CSF-020) |
| APPLIED | The contact change is saved in the client master (FR-CSF-021) |
| REFUSED | The change asked for a field the CSF cannot change |
| NOT_CONFIGURED | Legacy sync disabled; the change stays in the outbox (FR-CSF-022) |
| QUEUED / SENT / FAILED | Legacy sync enabled; sending state of the outbox row |

![Verification, contact change and legacy sync states](figures/brd09_contact_change.dot){width=13}

## Case lifecycle

A case has the five statuses of BRCSFCM.003 (p.5). Figure 2 in section 2.2 shows the flow. The table gives the allowed transitions; any other transition is refused (FR-CSF-052). The status group maps each status to the stages of the envisioned process (p.4); escalation is an indicator on an open case, not a status (FR-CSF-056; CLR-CSF-08).

<!-- table: widths=3.4,2.2,6.2,4.8 caption="Case statuses and status groups" size=8.5 -->
| Status | Status group (p.4) | Meaning | Next statuses and action |
|---|---|---|---|
| Open - With Fulfillment Unit | Open / In Progress | Submitted by the agent or re-endorsed; in the queue of the fulfilment unit | Closed by Fulfillment Unit (Resolve and Close); Open - Returned to Agent (Return to Agent); Open - CSR Action Required (Request CSR Action) |
| Open - Returned to Agent | Pending | Returned by the fulfilment unit for an error or more handling, with the reason | Open - With Fulfillment Unit (Re-endorse); Closed by CSR (Close) |
| Open - CSR Action Required | Pending | The fulfilment unit needs the agent to act with the client, with the instruction | Closed by CSR (Close); Open - With Fulfillment Unit (Re-endorse) |
| Closed by Fulfillment Unit | Closed | Resolved and closed by the fulfilment unit; read only | None; a follow-up is a new linked case |
| Closed by CSR | Closed | Resolved on the call, or closed by the agent or the supervisor; read only | None; a follow-up is a new linked case |

<!-- table: widths=4,3.6,9 caption="Case indicators" size=8.5 -->
| Indicator | Values | Set by |
|---|---|---|
| SLA status | Within TAT, Approaching breach, Breached | The SLA check from the creation time and the turnaround time of the case type (FR-CSF-057) |
| Escalated | Yes with the reason and date, or No | The supervisor (FR-CSF-056) or the automatic escalation on breach (FR-CSF-057) |
| Priority | High, Normal, Low (proposed, CSQ21) | The agent when logging; the supervisor afterwards |

# Reports and documents

## Reports

<!-- table: widths=4.2,4.2,6.2,2 caption="CSF reports (category Customer Service)" size=8.5 -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| CTL-AUDIT (existing) | Audit Trail | Changes to client records with old and new values | BRCSF-011 |
| CSF-CONTACT-CHANGES | Contact Changes | Contact changes with verification, agent, reason and sync status | BRCSF-010, 011 |
| CSF-ACTIVITY | Agent Activity | Searches, views, downloads, resends, uploads and changes per agent | p.30 |
| CSF-CASES | Case Register | Every case of the period with its categories, status, owner, fulfilment unit, age and SLA status | BRCSFCM.011 |
| CSF-CASE-SLA | Case Ageing and SLA | Open and closed cases by ageing band and SLA status; breached cases with their escalation | BRCSFCM.008, 011 |
| CSF-CASE-SUMMARY | Case Summary | Case volume, average ageing, SLA compliance rate and status distribution by case type, nature of transaction and fulfilment unit | BRCSFCM.011 |

Reports export to PDF, Excel and CSV and support saved variants. CTL-AUDIT needs AUDIT_VIEW; the CSF reports need CSF_REPORT_VIEW; the case reports need CSF_CASE_REPORT. The Case Dashboard (FR-CSF-059) shows the figures of the case reports from the same records. The case reports are proposed for the BDOI report list (Report List 05132026), which does not name them yet.

### Contact Changes (CSF-CONTACT-CHANGES)

Parameters: date range, agent, client. Layout: landscape, one row per changed field, sorted by change time.

<!-- table: widths=3.6,3,10 caption="CSF-CONTACT-CHANGES columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Change No. | Text | CSF-yyyy-nnnnnn |
| Date / Time | Date | Time the change was applied |
| Client | Text | Client code and name |
| Field | Text | E-mail, mobile, phone or address line |
| Old value / New value | Text | Values before and after |
| Verification | Text | Checks matched and result |
| Channel | Text | Hotline, e-mail or website |
| Agent | Text | User who made the change |
| Reason | Text | Change reason |
| Sync status | Text | Not required, not configured, queued, sent, failed |

### Agent Activity (CSF-ACTIVITY)

Parameters: date range, agent, action. Layout: summary of counts per agent and day by action (search, view, download, RA resend, e-policy resend, upload, contact change), followed by the detail rows with client, reference and time.

### Case Register (CSF-CASES)

Parameters: date range (created or closed), case type, nature of transaction, priority, status, market segment, fulfilment unit, agent. Layout: landscape, one row per case, sorted by creation time.

<!-- table: widths=3.6,3,10 caption="CSF-CASES columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Case ID | Text | CASE-yyyy-nnnnnn |
| Created | Date and time | dd-MMM-yyyy hh:mm |
| Client / caller | Text | Client code and name, or the caller name |
| Account / policy | Text | ARN and policy number |
| Channel | Text | Phone, Email, Email via BDO Website |
| Case type | Text | Case type with its TAT in working days |
| Nature of transaction | Text | Value of the list |
| Product type / market segment | Text | Values of the lists |
| Fulfilment unit | Text | Unit of the case |
| Owner | Text | Name of the agent or unit member |
| Priority | Text | High, Normal, Low |
| Status | Text | Status label |
| Due date | Date | dd-MMM-yyyy |
| Age (working days) | Number | From creation to closure or to the report date |
| SLA status | Text | Within TAT, Approaching breach, Breached |
| Escalated | Text | Reason and date, or blank |
| Closed | Date and time | dd-MMM-yyyy hh:mm, closure status and reason |
| Linked case | Text | Case ID of the original or follow-up case |

### Case Ageing and SLA (CSF-CASE-SLA)

Parameters: as of date, case type, fulfilment unit. Layout: summary by fulfilment unit and case type with the open cases in the ageing bands 0-1, 2-3, 4-6 and over 6 working days (bands configurable), within TAT, approaching breach and breached; then the breached cases with their owner, due date, days past due and escalation.

### Case Summary (CSF-CASE-SUMMARY)

Parameters: date range, case type, priority, status, market segment, fulfilment unit. Layout: case volume (created, closed, open at the end), average ageing of closed cases, SLA compliance rate (closed within TAT / closed), breached versus non-breached, and the distribution by status, case type, nature of transaction and fulfilment unit.

## Documents

The CSF generates no documents. It reads and resends documents of other modules: RENEWAL_ADVICE (Renewal, Employee Benefits), CLAIM_REPORT (Claims), e-policies (Issuance) and quotations. Uploaded documents take a type from the list CSF_DOCUMENT_TYPE, a subset of the platform document types.

# Interfaces and integration

Figure 4 shows the interfaces. The CSF reads the BIBS modules and keeps its own case records; no module depends on the CSF.

![Interfaces of the Customer Servicing Facility (dashed = on hold)](figures/brd09_integration.dot)

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| Client master (CRM) | In / Out | Client information; contact-only update with reason, source and verification | BRCSF-002, 004, 008 | IN SCOPE |
| Accounts (BRD-1) | In | Accounts by client, PN numbers, stage | BRCSF-002, 003 | IN SCOPE |
| Placement (BRD-1) | In | Accounts by loan application number | BRCSF-003 | IN SCOPE |
| Issuance (BRD-1) | In / Out | Policy numbers, e-policies; resend through the dispatch service | CSF-EM09 | IN SCOPE |
| Invoice ledger and Cashiering (BRD-2) | In | Invoices, balances, payments of a client | BRCSF-005 | IN SCOPE |
| Documents | In / Out | RENEWAL_ADVICE, CLAIM_REPORT and other documents; uploads | BRCSF-006, 007, 009 | IN SCOPE |
| E-mail outbox | Out | RA resend, protected, with separate password | BRCSF-006 | IN SCOPE |
| QPS / EBIX contact write-back | Out | Contact changes through the outbox | p.20; BRCSF-002 | ON HOLD |
| QPS / EBIX / LOS account lookup | In | Accounts not migrated to BIBS by PN or application number | BRCSF-003 | ON HOLD |
| Case management | Internal (CSF) | Case records, notes, documents, routing and SLA within BIBS | BRCSFCM.001-012 | IN SCOPE (drop to be decided, CLR-CSF-07) |
| Organisation and user access (BRD-11) | In | Fulfilment unit membership of users; account officer and unit of an account | BRCSFCM.012 | IN SCOPE |
| Notification and e-mail service | Out | Case notifications to the fulfilment unit, agent, supervisor, escalation group and account officer | BRCSFCM.001, 008 | IN SCOPE |

> [!NOTE] Interfaces on hold
> The write-back to QPS and EBIX keeps every change in an outbox with status "not configured" (CSQ01); a replay is possible once the interface is specified. The lookup of accounts that exist only in QPS, EBIX or the loan system returns nothing until it is connected (CSQ02).

# Non-functional requirements

<!-- table: widths=3,5.6,5.4 caption="Non-functional requirements (BRD p.30-31)" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Users | 24 concurrent agents, supervisors and personnel; 16 document users; 8 report users (leads / heads) | Small load within the BRD-1 sizing |
| Volumes | 144,400 transactions a year for access, retrieval and search (about 600 a working day); 113,800 document transactions; 1% growth | Indexed search keys; paged results |
| Response time | Under 3 seconds for retrieval, updates and search; 5 seconds for documents and reports | Each Servicing View tab loads on its own; search on indexed keys; no report on the agent's path |
| Peak | First quarter; 10:00-12:00 and 14:00-16:00 | No batch work in these windows |
| Availability | 99.9%; used 06:00-22:00; maintenance weekdays and Saturdays 21:00-05:00 | The maintenance window overlaps usage 21:00-22:00 (CSQ12) |
| Recovery | RTO 4 hours; RPO 4 hours | Met by log archiving (FR-CSF-043) |
| Backup | Every 15 minutes (BRCSF-011 / 11.001); every 4 hours in the retention table | Log archiving every 15 minutes plus daily base backups (CSQ11) |
| Retention | Client and account data, contact updates, audit trail, attachments - 5 years online, 15 years archive; backups kept 5 years | Retention rule CSF_CONTACT_CHANGE; platform rules for the other records |
| Anonymisation | Not required | None |
| Devices | Same performance on mobile and desktop | Responsive screens |
| Usability and security | User-friendly, intuitive; secure access (p.30) | BDO UX guidelines (R6); role-based access (FR-CSF-002) |
| Case management | The addendum states no volumes or response times | The CSF targets apply: 3 seconds to log or open a case, 5 seconds for case reports and the dashboard; case volumes to be confirmed with Contact Center Management (CSQ23) |

# Configuration items

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.4,8 caption="CSF parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| CSF_PAYMENT_HISTORY_MONTHS | 12 | Months of payment history shown by default |
| CSF_VERIFY_MIN_MATCHES | 2 | Verification checks that must match |
| CSF_VERIFY_MAX_FAILS | 3 | Failed verifications of one client a day before the alert |
| CSF_VERIFICATION_VALID_MINUTES | 30 | Validity of a passed verification |
| CSF_LEGACY_SYNC_ENABLED | false | Send contact changes to QPS and EBIX |
| CSF_SEARCH_MIN_CHARS | 3 | Minimum characters of a name search |
| CSF_SEARCH_MAX_RESULTS | 50 | Maximum clients returned by a search |
| LOGIN_MAX_FAILED_ATTEMPTS | 3 | Failed log-ins before lockout (platform, decision D5) |
| CSF_CASE_PREFIX | CASE | Prefix of the Case ID CASE-yyyy-nnnnnn |
| CSF_CASE_DUPLICATE_CHECK | true | Refuse a second open case for the same client, account and nature of transaction |
| CSF_CASE_TAT_UNIT | Working days | Unit of the turnaround times of the case types (CSQ18) |
| CSF_CASE_SLA_PAUSE | false | Stop the ageing while a case waits for the client (CSQ18) |
| CSF_CASE_NEAR_BREACH_DAYS | 1 | Working days before the due date when a case is Approaching breach |
| Job CSF_CASE_SLA | every 15 minutes | Updates the SLA status, sends the near-breach and breach notifications, escalates breached cases |
| Job CSF_LEGACY_SYNC | every 15 minutes | Sends queued contact changes (manual only until sync is enabled) |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values provided |
|---|---|
| CSF_STATUS_MAP | The mapping of section 5.1 |
| CSF_VERIFY_CHECK | Address; Contact number; E-mail; Insured property (CSQ03) |
| CSF_CHANGE_REASON | Generic values provided (client request, correction, returned mail); BDOI supplies its own |
| CSF_DOCUMENT_TYPE | Subset of the platform document types for contact centre uploads (CSQ07) |
| CSF_CASE_CHANNEL | Phone; Email; Email via BDO Website (p.11) |
| CSF_CASE_TYPE | Inquiry 3; Request 4; Request (1-day) 1; Problem 6; Complaint 6; Follow-up 1; Commendation 6 - turnaround time in working days, with the automatic escalation flag (p.11) |
| CSF_CASE_NATURE | The 125 natures of transaction of p.11-14, from Accredited Insurance Companies to Welcome Call and Confirmation if E-policy was received |
| CSF_CASE_PRODUCT | The 51 product types of p.14-15, from Accident & Health to Voluntary Group Life Insurance, each with its BIBS product line where one exists (CSQ19) |
| CSF_CASE_SEGMENT | The 33 market segments of p.15-16, from Alternative Distribution to SM Tenants and Consignor, each with its BIBS market segment where one exists (CSQ19) |
| CSF_FULFILMENT_UNIT | The 53 fulfilment units of p.16-17, from Alternative Distribution to SM Providers |
| CSF_CASE_PRIORITY | High; Normal; Low (proposed, CSQ21) |
| CSF_CASE_STATUS | The five statuses of section 5.3 with their status group |
| CSF_ESCALATION_REASON | Past TAT; Due for Resolution (p.7); BDOI adds its own |
| CSF_RETURN_REASON | Incomplete case details; Wrong fulfilment unit; Needs contact centre handling (proposed) |
| CSF_CLOSURE_REASON | Resolved; Information provided; Request completed; Client did not respond; Duplicate (proposed) |

The case routing table (FR-CSF-060, FR-CSF-061) is maintained on Customer Service Facility > Case Routing: one row per case type, market segment and nature of transaction (a blank matches any value) with the fulfilment unit and its escalation group. Contact Center Management provides the first table (CSQ19).

## Platform settings

<!-- table: widths=5.4,11.2 caption="Platform settings used by the CSF" size=8.5 -->
| Setting | Value |
|---|---|
| Allowed file types | Platform list plus TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP (BRCSF-007) |
| Document access classes | Rows for RENEWAL_ADVICE and CLAIM_REPORT that let the CSF roles list and download them (XQ04) |
| Backup interval of the changes | At most 15 minutes (BRCSF-011 / 11.001) |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-CSF-01 | The signed BRD (p.18-33) is the baseline; the e-mail of 13-Feb-2026 adds e-mail items 7 and 9 and defers item 10 | R1, R2 |
| A-CSF-02 | BIBS is the source of client and account data; QPS and EBIX matter only while they coexist | CSQ01 |
| A-CSF-03 | Agents change contact details only; other changes go to the fulfilment unit | E-mail topic 3 |
| A-CSF-04 | CSF agents see every segment (CBG and non-CBG) | p.21; CQ06 |
| A-CSF-05 | Case management is BDOI scope by the signed Case Management Addendum; its delivery drop is decided by BDOI and the Change Control Board | CLR-CSF-07; CSQ09 answered |
| A-CSF-07 | The case market segments, product types and fulfilment units are the contact centre's own lists, as in the addendum | CSQ19 |
| A-CSF-08 | Turnaround times are working days, counted from the creation of the case | CSQ18 |
| A-CSF-06 | The 15-minute backup applies to all BIBS data | CSQ11 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-CSF-01 | The Renewal and EB modules store RAs as RENEWAL_ADVICE; Claims stores claims reports as CLAIM_REPORT (decision D3) | FR-CSF-030, 033 |
| D-CSF-02 | The attachment access classes (EB work item P3) include rows for the CSF roles | FR-CSF-033 |
| D-CSF-03 | The Operations owner agrees the read method for the payments of a client | FR-CSF-013 |
| D-CSF-04 | The client master accepts the contact-only update contract | FR-CSF-021 |
| D-CSF-05 | BDOI specifies the QPS / EBIX interface if a write-back is needed | FR-CSF-022 |
| D-CSF-06 | The infrastructure team configures log archiving and restore tests | FR-CSF-043 |
| D-CSF-07 | User Access Maintenance (BRD-11) grants CSF_FULFILMENT and the fulfilment unit membership to the users of each unit | FR-CSF-060 |
| D-CSF-08 | Contact Center Management provides the routing table and the escalation groups | FR-CSF-056, 060, 061 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-9 (status from the cross-BRD decisions, R5)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| CSQ01 | Until when QPS and EBIX are systems of record; write-back interface; accounts not migrated | FR-CSF-011, 022 | OPEN |
| CSQ02 | Meaning of client ID and account number; LOS lookup for PN and application numbers | FR-CSF-010 | OPEN |
| CSQ03 | Mandatory verification checks; number to match; verification before a resend | FR-CSF-020 | OPEN |
| CSQ04 | Definitions of Pending, Awaiting, Booked, Open and Closed | FR-CSF-012 | OPEN |
| CSQ05 | Content and depth of the payment history | FR-CSF-013 | OPEN |
| CSQ06 | Legacy RAs; resend to another address; RA password convention | FR-CSF-030 | OPEN |
| CSQ07 | Document types, linking, retention, download and upload rights, confidentiality | FR-CSF-032, 033 | OPEN |
| CSQ08 | Maximum file size; HEIF photos; virus scanning | FR-CSF-032 | OPEN |
| CSQ09 | Case management out of this phase; a minimal interaction note | Section 1.2 | ANSWERED (Case Management Addendum: in scope; drop open, CLR-CSF-07) |
| CSQ10 | Role matrix; who uploads and who updates contacts | Section 3 | OPEN |
| CSQ11 | 15-minute backup BIBS-wide or CSF only | FR-CSF-043 | OPEN |
| CSQ12 | Usage hours overlap the maintenance window; Sunday usage | Section 8 | OPEN |
| CSQ13 | E-policy resend: confirmed e-policy only; password | FR-CSF-031 | OPEN |
| CSQ14 | Should a contact update also change the contacts of open accounts | FR-CSF-021 | OPEN |
| CSQ15 | Additional contacts per client | FR-CSF-021 | OPEN |
| CSQ16 | Priority order for servicing; loan system parameter alignment | Section 1.2 | OPEN |
| CSQ17 | Which status list governs - the five statuses of BRCSFCM.003 or Open, In Progress, Pending, Escalated, Closed of p.4 | FR-CSF-052 | OPEN |
| CSQ18 | Unit of the turnaround times (working or calendar days); whether ageing stops while waiting for the client; near-breach threshold | FR-CSF-051, 057 | OPEN |
| CSQ19 | Fulfilment units: member users, escalation group and the BIBS unit of each; mapping of the case market segments and product types to the BIBS masters; first routing table | FR-CSF-060, 061 | OPEN |
| CSQ20 | Meaning of "same transaction" for the duplicate check; cases for callers who are not clients; acknowledgement e-mail to the client | FR-CSF-050 | OPEN |
| CSQ21 | Case priority values; retention period of case records | FR-CSF-050, 055 | OPEN |
| CSQ22 | BRCSFCM.009 is not in the addendum - removed on purpose?; priority of BRCSFCM.012 is blank - Must have? | Chapter 11 | OPEN |
| CSQ23 | Case volumes and peak; migration of the open SharePoint cases at go-live | Sections 1.2, 8 | OPEN |
| CSQ24 | Delivery drop and plan of the Case Management module (with DCR-163, CR-0005) | Section 4.6 | OPEN |
| XQ02 | Contact Center roles of Renewal and CSF: same people or not | Section 3.1 | OPEN |
| XQ04 | Confidentiality matrix of shared document types | FR-CSF-033 | OPEN |
| XQ05 | Which claim documents are claims reports | FR-CSF-033 | OPEN |
| XQ12 | CSF case resolution in phase 1 (Core Replacement BR-165 against BRD-9 CSF-EM10; DCR-163) | Section 4.6 | ANSWERED (Case Management Addendum; drop open, CLR-CSF-07) |
| CQ06 | Data scope by segment for the contact centre | Section 3.3 | PARTIAL |
| Q42 | BDO single sign-on / Windows ID | FR-CSF-001 | ANSWERED |

# Traceability

Every requirement row of the CSF BRD, of the e-mail of 13-Feb-2026 and of the Case Management Addendum is met by at least one FR. CSF-EM10, which the e-mail deferred, is now met by the Case Management FRs of the addendum. The screen column names the main entry point; the test cases are listed by test condition (TC-CSF-nnn.n). The non-functional requirements (p.30-31) are traced in section 8.

<!-- table: widths=3.2,3.2,5.6,5.2 caption="BRD ID to FR, screen and test cases" size=8 -->
| BRD ID | FR | Screen | Test cases |
|---|---|---|---|
| BRCSF-001 | FR-CSF-001, FR-CSF-002 | Login | TC-CSF-001.1, 001.2, 001.3, 001.4, 001.5, 002.1, 002.2, 002.3 (14 cases) |
| BRCSF-001 / 1.001 | FR-CSF-001 | Login; Customer Search | TC-CSF-001.1, 001.2, 001.3, 001.4, 001.5 (8 cases) |
| BRCSF-001 / 1.002 | FR-CSF-001 | Login | TC-CSF-001.1, 001.2, 001.3, 001.4, 001.5 (8 cases) |
| BRCSF-002 | FR-CSF-011, FR-CSF-021, FR-CSF-022 | Servicing View | TC-CSF-011.1, 011.2, 011.3, 011.4, 011.5, 021.1, 021.2, 021.3, 021.4, 021.5, 021.6, 022.1, 022.2, 022.3, 022.4 (22 cases) |
| BRCSF-003 | FR-CSF-010 | Customer Search | TC-CSF-010.1, 010.2, 010.3, 010.4, 010.5, 010.6, 010.7, 010.8 (13 cases) |
| BRCSF-003 / 3.001 | FR-CSF-010 | Customer Search | TC-CSF-010.1, 010.2, 010.3, 010.4, 010.5, 010.6, 010.7, 010.8 (13 cases) |
| BRCSF-004 | FR-CSF-020, FR-CSF-021 | Update Contact dialog | TC-CSF-020.1, 020.2, 020.3, 020.4, 020.5, 021.1, 021.2, 021.3, 021.4, 021.5, 021.6 (19 cases) |
| BRCSF-005 | FR-CSF-013 | Payments tab | TC-CSF-013.1, 013.2, 013.3, 013.4, 013.5 (6 cases) |
| BRCSF-005 / 5.001 | FR-CSF-012 | Accounts tab | TC-CSF-012.1, 012.2, 012.3, 012.4 (5 cases) |
| CSF-EM07 | FR-CSF-012 | Accounts tab | TC-CSF-012.1, 012.2, 012.3, 012.4 (5 cases) |
| BRCSF-006 | FR-CSF-030 | Renewal Advice tab | TC-CSF-030.1, 030.2, 030.3, 030.4, 030.5, 030.6 (9 cases) |
| BRCSF-006 / 6.001 | FR-CSF-030 | Resend dialog | TC-CSF-030.1, 030.2, 030.3, 030.4, 030.5, 030.6 (9 cases) |
| CSF-EM09 | FR-CSF-031 | E-policies tab | TC-CSF-031.1, 031.2, 031.3 (4 cases) |
| BRCSF-007 | FR-CSF-032 | Documents tab (Upload) | TC-CSF-032.1, 032.2, 032.3, 032.4, 032.5, 032.6 (11 cases) |
| BRCSF-007 / 7.001 | FR-CSF-032 | Documents tab (Upload) | TC-CSF-032.1, 032.2, 032.3, 032.4, 032.5, 032.6 (11 cases) |
| BRCSF-008 | FR-CSF-011 | Servicing View (summary card) | TC-CSF-011.1, 011.2, 011.3, 011.4, 011.5 (6 cases) |
| BRCSF-009 | FR-CSF-033 | Documents tab | TC-CSF-033.1, 033.2, 033.3, 033.4, 033.5 (6 cases) |
| BRCSF-009 / 9.001 | FR-CSF-033 | Documents tab | TC-CSF-033.1, 033.2, 033.3, 033.4, 033.5 (6 cases) |
| BRCSF-010 | FR-CSF-040, FR-CSF-021, FR-CSF-042 | Contact History; Audit Trail | TC-CSF-021.1, 021.2, 021.3, 021.4, 021.5, 021.6, 040.1, 040.2, 040.3, 042.1, 042.2, 042.3, 042.4, 042.5 (18 cases) |
| BRCSF-011 | FR-CSF-041 | Audit Trail; Reports | TC-CSF-041.1, 041.2, 041.3, 041.4, 041.5 (5 cases) |
| BRCSF-011 / 11.001 | FR-CSF-043 | None (infrastructure) | TC-CSF-043.1, 043.2, 043.3, 043.4 (4 cases) |
| BRCSF-011 / 11.002 | FR-CSF-041 | Audit Trail; Reports | TC-CSF-041.1, 041.2, 041.3, 041.4, 041.5 (5 cases) |
| CSF-EM10 | FR-CSF-050 to FR-CSF-061 (deferral superseded by the Case Management Addendum) | Cases | See the BRCSFCM rows |
| BRCSFCM.001 | FR-CSF-050 | Log Case dialog | TC-CSF-050.1, 050.2, 050.3, 050.4, 050.5, 050.6 (10 cases) |
| BRCSFCM.002 | FR-CSF-051, FR-CSF-061 | Log Case dialog; Change Category | TC-CSF-051.1, 051.2, 051.3, 051.4, 061.1, 061.2, 061.3 (10 cases) |
| BRCSFCM.003 | FR-CSF-052 | Case record | TC-CSF-052.1, 052.2, 052.3, 052.4 (4 cases) |
| BRCSFCM.004 | FR-CSF-053 | Case record (Notes) | TC-CSF-053.1, 053.2, 053.3 (3 cases) |
| BRCSFCM.005 | FR-CSF-054 | Case record (Documents) | TC-CSF-054.1, 054.2, 054.3 (3 cases) |
| BRCSFCM.006 | FR-CSF-055 | Case record (History); Audit Trail | TC-CSF-055.1, 055.2, 055.3, 055.4 (4 cases) |
| BRCSFCM.007 | FR-CSF-056 | Cases (Team Queue); Case record | TC-CSF-056.1, 056.2, 056.3, 056.4 (4 cases) |
| BRCSFCM.008 | FR-CSF-057, FR-CSF-059 | Case record; Case Dashboard | TC-CSF-057.1, 057.2, 057.3, 057.4, 059.1, 059.2, 059.3, 059.4 (10 cases) |
| BRCSFCM.010 | FR-CSF-058 | Case record (Linked Cases) | TC-CSF-058.1, 058.2, 058.3 (4 cases) |
| BRCSFCM.011 | FR-CSF-059 | Case Dashboard; Reports | TC-CSF-059.1, 059.2, 059.3, 059.4 (5 cases) |
| BRCSFCM.012 | FR-CSF-060 | Cases (Fulfilment Unit Queue) | TC-CSF-060.1, 060.2, 060.3, 060.4 (6 cases) |
| Lists of values I-VI (p.11-17) | FR-CSF-061 | Lists of Values; Case Routing | TC-CSF-061.1, 061.2, 061.3 (4 cases) |


# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, fills a gap the BRD leaves open, or needs a decision of BDOI. The open questions of section 10.3 stay open; each item below gives the proposed rule that applies until BDOI answers. BDOI records its decision with the sign-off of this FRS (chapter 13); a decision that changes a screen, field, rule or message is applied in the next version of the FRS.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-CSF-01 | Data source and write-back (BRCSF-002, p.20, p.22; FR-CSF-022) | BIBS is the source of the client and account data. A contact change updates the BIBS client master at once and is kept for QPS and EBIX with the status "not configured"; it is sent when BDOI specifies the interface. | The BRD reads QPS and EBIX and writes updates back to them; how long they stay systems of record is open (CSQ01). | Say until when QPS and EBIX stay systems of record and whether a write-back is needed (CSQ01). |
| CLR-CSF-02 | Accounts not in BIBS (BRCSF-003; FR-CSF-010) | The search by PN or application number finds the accounts in BIBS; accounts that exist only in QPS, EBIX or the loan system are found once they are connected. | The legacy lookup interface is not specified (CSQ01, CSQ02). | Specify the lookup, or confirm the search in BIBS only (CSQ02). |
| CLR-CSF-03 | Contact details (BRCSF-004) | Contact details are updated after a recorded verification of the caller; "add" means additional contacts only if BDOI confirms it. | E-mail topic 5 asks for verification; additional contacts are open (CSQ15). | Confirm the verification checks (CSQ03) and additional contacts (CSQ15). |
| CLR-CSF-04 | Backup every 15 minutes (BRCSF-011 / 11.001; FR-CSF-043) | The changes are saved to the backup storage at least every 15 minutes, as an infrastructure requirement for all BIBS data. | The 15-minute backup conflicts with the 4-hour backup of the retention table (CSQ11, XQ08). | Confirm the backup interval (CSQ11). |
| CLR-CSF-05 | Log-in (BRCSF-001; FR-CSF-001) | The BIBS sign-in applies; directory sign-in (Windows ID) is added for all BIBS users when BDO supplies the interface. | Cross-BRD decision D6. | Confirm decision D6. |
| CLR-CSF-06 | Case management (CSF-EM10) | Superseded. Case management is specified in section 4.6 (FR-CSF-050 to 061) from the signed Case Management Addendum. | The addendum is the requirement document the e-mail of 13-Feb-2026 asked for. | None; the delivery drop is CLR-CSF-07. |
| CLR-CSF-07 | Delivery drop and plan of the Case Management module (BRCSFCM.001-012; CSQ24) | The Case Management module is specified in full in this FRS and its cases are in the test plan. It is a new module: case record, routing table, fulfilment unit role and queues, SLA check and notifications, dashboard and reports, and the case lists. It is proposed for delivery as a change to the BRD-9 scope of Drop 1, assessed and planned through the Change Management Register; the other CSF functions keep their Drop 1 plan. | The signed addendum (approved 31-Mar to 22-Apr-2026) makes case management BDOI scope after FRS v1.0 placed it out of scope. It settles the cross-BRD question XQ12 (DCR-163) - Core Replacement BR-165 against BRD-9 CSF-EM10 - and change request CR-0005, which kept case resolution out of phase 1. | BDOI and the Change Control Board decide the drop in which the Case Management module is delivered and approve its plan and effort; DCR-163 and CR-0005 are then closed against that decision. |
| CLR-CSF-08 | Case statuses (BRCSFCM.003, p.4; CSQ17) | The five statuses of BRCSFCM.003 are the statuses of a case; Open, In Progress, Pending and Closed of p.4 are shown as their status group and Escalated as an indicator on the open case (section 5.3). | The addendum gives two different status lists. | Confirm the status list, the transitions of section 5.3 and the escalation indicator. |
| CLR-CSF-09 | Turnaround time and ageing (BRCSFCM.002, 008; p.11; CSQ18) | The turnaround times of p.11 are working days on the BIBS holiday calendar; ageing runs from the creation time without stopping; a case is Approaching breach 1 working day before its due date; a breached case of a type set for it is escalated automatically with the reason Past TAT. | The addendum gives the numbers without a unit or a near-breach threshold. | Confirm the unit, the pause rule, the near-breach threshold and the automatic escalation per case type. |
| CLR-CSF-10 | Fulfilment units, routing and lists (BRCSFCM.012, p.4, p.14-17; CSQ19) | The case market segments, product types and fulfilment units are the contact centre's own lists, each value optionally linked to the BIBS market segment or product line. The fulfilment unit is proposed from a routing table of case type, market segment and nature of transaction; the account officer of the account receives a copy. Fulfilment unit users get the CSF_FULFILMENT role with the membership of their units through User Access requests. | The case lists do not match the BIBS market segments and product catalogue, and the addendum does not say who belongs to each unit or how the BU/AO ownership applies. | Confirm the lists as CSF lists, provide the members and escalation group of each unit and the first routing table. |
| CLR-CSF-11 | Case capture rules (BRCSFCM.001, 006, 011; CSQ20, CSQ21) | A second open case for the same client, account and nature of transaction is refused and the open case is shown. A caller who is not a client is logged with name and contact. The case notification goes to the fulfilment unit and the account officer; no acknowledgement e-mail goes to the client unless BDOI asks for one. Priority values High, Normal (default) and Low; case records are kept as the other CSF records (5 years online, 15 years archive). | The addendum asks to prevent duplicates "where applicable", names priority changes and filters without values, and does not name the notification recipients. | Confirm the duplicate rule, the non-client cases, the notification recipients, the priority values and the retention. |
| CLR-CSF-12 | Requirement numbering of the addendum (CSQ22) | BRCSFCM.009 is treated as not used; BRCSFCM.012 is treated as Must have, like the other rows. | The addendum skips BRCSFCM.009 and leaves the priority of BRCSFCM.012 blank. | Confirm both. |

# Sign-off

By signing, BDOI confirms that this FRS describes the Customer Servicing Facility functions it expects in BIBS, the Case Management functions of the addendum included, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Customer Servicing Facility", organisation: BDOI}
  - {name: "", role: Alternative Distribution Head, organisation: BDOI}
  - {name: "", role: "Head, Contact Center Management", organisation: BDOI}
  - {name: "", role: "Head, Corporate and Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

# Appendix: User-story view

The CSF BRD and the Case Management Addendum write their requirements as user stories ("As a <Persona>, I must be able to <Requirement> so that ..."). This appendix restates each requirement of the file Customer Servicing Facility_Case Management (received on 08-Oct-2026) as one user story, with the FRs that meet it, their acceptance criteria and the test conditions of the test plan with their number of cases. The BRD wording is kept with tidied grammar; where the BRD names the persona "System", the story is written for the persona who needs the result. The personas are those of section 3.1. Acceptance criteria are numbered in the order of the FR (AC1 is the first criterion of the FR). The table has 35 rows: the 20 rows of the CSF BRD (BRCSF-001 to 011 with their process steps), the e-mail items CSF-EM07, CSF-EM09 and CSF-EM10, the eleven rows of the addendum (BRCSFCM.001 to 008 and 010 to 012) and its lists of values.

<!-- table: widths=1.9,8.0,2.3,2.8,3.4 caption="User-story view: BRD requirement, user story, FRs, acceptance criteria and test cases" size=8 -->
| BRD ID | User story | FR IDs | Acceptance criteria | Test cases |
|---|---|---|---|---|
| BRCSF-001 | As the System Administrator, I need to restrict access to authorised users, so that only authorised personnel can use the Customer Servicing Facility. | FR-CSF-001, FR-CSF-002 | FR-CSF-001 AC1-3; FR-CSF-002 AC1-2 | TC-CSF-001.1 to 001.5, 002.1 to 002.3 (14 cases) |
| BRCSF-001 / 1.001 | As a BDO Insure Contact Center Agent, I need to log in to the system by entering my user ID and password, so that I can access the servicing functions. | FR-CSF-001 | FR-CSF-001 AC1-3 | TC-CSF-001.1 to 001.5 (8 cases) |
| BRCSF-001 / 1.002 | As a BDO Insure Contact Center Agent, I need BIBS to validate my credentials and grant access only on a valid entry, so that wrong credentials are refused. | FR-CSF-001 | FR-CSF-001 AC1-3 | TC-CSF-001.1 to 001.5 (8 cases) |
| BRCSF-002 | As a BDO Insure Contact Center Agent, I need to access client and account information from the source systems, so that I can view and update customer details. | FR-CSF-011, FR-CSF-021, FR-CSF-022 | FR-CSF-011 AC1-3; FR-CSF-021 AC1-4; FR-CSF-022 AC1-2 | TC-CSF-011.1 to 011.5, 021.1 to 021.6, 022.1 to 022.4 (22 cases) |
| BRCSF-003 | As a BDO Insure Contact Center Agent, I need to search for client details by name, client ID number, account number, promissory note number or application number, so that I can quickly find the customer's information. | FR-CSF-010 | FR-CSF-010 AC1-4 | TC-CSF-010.1 to 010.8 (13 cases) |
| BRCSF-003 / 3.001 | As a BDO Insure Contact Center Agent, I need to search for the client and see the accurate client details, so that I serve the right customer. | FR-CSF-010 | FR-CSF-010 AC1-4 | TC-CSF-010.1 to 010.8 (13 cases) |
| BRCSF-004 | As a BDO Insure Contact Center Agent, I need to view, add or update the client's contact details, so that customer information is current and accurate. | FR-CSF-020, FR-CSF-021 | FR-CSF-020 AC1-2; FR-CSF-021 AC1-4 | TC-CSF-020.1 to 020.5, 021.1 to 021.6 (19 cases) |
| BRCSF-005 | As a BDO Insure Contact Center Agent, I need to view the payment history, so that I can give customers accurate information. | FR-CSF-013 | FR-CSF-013 AC1-2 | TC-CSF-013.1 to 013.5 (6 cases) |
| BRCSF-005 / 5.001 | As a BDO Insure Contact Center Agent, I need to view the latest status of each account (Pending, Awaiting), so that I can give customers accurate information. | FR-CSF-012 | FR-CSF-012 AC1-3 | TC-CSF-012.1 to 012.4 (5 cases) |
| CSF-EM07 | As a BDO Insure Contact Center Agent, I need the account statuses Booked, Open and Closed in addition to Pending and Awaiting, so that the status I give matches the definitions of Marketing, Processing and Operations. | FR-CSF-012 | FR-CSF-012 AC1-3 | TC-CSF-012.1 to 012.4 (5 cases) |
| BRCSF-006 | As a BDO Insure Contact Center Agent, I need to view and download the renewal advice to resend it to clients, so that customers receive renewal information when they request it. | FR-CSF-030 | FR-CSF-030 AC1-3 | TC-CSF-030.1 to 030.6 (9 cases) |
| BRCSF-006 / 6.001 | As a BDO Insure Contact Center Agent, I need to access the renewal advice, select the resend option and confirm the resend, so that the RA is sent successfully. | FR-CSF-030 | FR-CSF-030 AC1-3 | TC-CSF-030.1 to 030.6 (9 cases) |
| CSF-EM09 | As a BDO Insure Contact Center Agent, I need to resend the e-policy to the client, so that a client who did not receive or lost the policy gets it without a manual request to Processing. | FR-CSF-031 | FR-CSF-031 AC1-2 | TC-CSF-031.1 to 031.3 (4 cases) |
| BRCSF-007 | As a BDO Insure Contact Center Agent, I need to attach or upload documents (text files, spreadsheets, images and PDF), so that all relevant information is stored. | FR-CSF-032 | FR-CSF-032 AC1-3 | TC-CSF-032.1 to 032.6 (11 cases) |
| BRCSF-007 / 7.001 | As a BDO Insure Contact Center Agent, I need to select document upload, choose the file, upload it and confirm the upload, so that the documents are correctly attached and accessible. | FR-CSF-032 | FR-CSF-032 AC1-3 | TC-CSF-032.1 to 032.6 (11 cases) |
| BRCSF-008 | As a BDO Insure Contact Center Agent, I need to view the current client information, so that I can provide accurate support. | FR-CSF-011 | FR-CSF-011 AC1-3 | TC-CSF-011.1 to 011.5 (6 cases) |
| BRCSF-009 | As a BDO Insure Contact Center Agent, I need to view, retrieve or download all uploaded policy-related documents, including quotations, renewal advices and claims reports, so that I can access the information I need. | FR-CSF-033 | FR-CSF-033 AC1-3 | TC-CSF-033.1 to 033.5 (6 cases) |
| BRCSF-009 / 9.001 | As a BDO Insure Contact Center Agent, I need to search for the client, access the uploaded documents and retrieve a document, so that the document is displayed and retrieved accurately. | FR-CSF-033 | FR-CSF-033 AC1-3 | TC-CSF-033.1 to 033.5 (6 cases) |
| BRCSF-010 | As Contact Center Management, I need BIBS to record every change to client information, so that there is an audit trail. | FR-CSF-040, FR-CSF-021, FR-CSF-042 | FR-CSF-040 AC1-2; FR-CSF-021 AC1-4; FR-CSF-042 AC1 | TC-CSF-040.1 to 040.3, 021.1 to 021.6, 042.1 to 042.5 (18 cases) |
| BRCSF-011 | As Contact Center Management, I need to print or save the audit trail report, so that there is a record of the changes. | FR-CSF-041 | FR-CSF-041 AC1-3 | TC-CSF-041.1 to 041.5 (5 cases) |
| BRCSF-011 / 11.001 | As Contact Center Management, I need BIBS to back up the data every 15 minutes to a designated secure location, so that data loss is minimised. | FR-CSF-043 | FR-CSF-043 AC1-2 | TC-CSF-043.1 to 043.4 (4 cases) |
| BRCSF-011 / 11.002 | As Contact Center Management, I need to access the audit trail and print or save it in Excel or PDF, so that I keep a record of the changes. | FR-CSF-041 | FR-CSF-041 AC1-3 | TC-CSF-041.1 to 041.5 (5 cases) |
| CSF-EM10 | As a BDO Insure Contact Center Agent, I need to log and track customer inquiries in BIBS instead of SharePoint, so that every inquiry is handled to closure; the e-mail of 13-Feb-2026 deferred this to its own requirement document, which is the Case Management Addendum (rows BRCSFCM below). | FR-CSF-050 to FR-CSF-061 | See the rows BRCSFCM.001 to 012 | TC-CSF-050.1 to 061.3 (58 cases) |
| BRCSFCM.001 | As a BDO Insure Contact Center Agent, I need to log a customer's transaction as a case with a unique Case ID, linked to the customer and account, with the mandatory case details validated and duplicates prevented, so that every inquiry is recorded in one system and the right people are notified. | FR-CSF-050 | FR-CSF-050 AC1-6 | TC-CSF-050.1 to 050.6 (10 cases) |
| BRCSFCM.002 | As a BDO Insure Contact Center Agent, I need to categorise the case from predefined case types (inquiry, request, problem, complaint and others), so that the category drives the routing and the turnaround time and only authorised users change it. | FR-CSF-051, FR-CSF-061 | FR-CSF-051 AC1-4; FR-CSF-061 AC1-3 | TC-CSF-051.1 to 051.4, 061.1 to 061.3 (10 cases) |
| BRCSFCM.003 | As a Contact Center Supervisor, I need BIBS to manage the case lifecycle statuses (Open - With Fulfillment Unit, Closed by Fulfillment Unit, Closed by CSR, Open - Returned to Agent, Open - CSR Action Required) by predefined rules, so that the current status is always visible, time stamped and traceable and invalid transitions are prevented. | FR-CSF-052 | FR-CSF-052 AC1-4 | TC-CSF-052.1 to 052.4 (4 cases) |
| BRCSFCM.004 | As a BDO Insure Contact Center Agent, I need to add notes and updates to a case throughout its lifecycle, so that every follow-up is recorded in date order with its author and cannot be altered. | FR-CSF-053 | FR-CSF-053 AC1-3 | TC-CSF-053.1 to 053.3 (3 cases) |
| BRCSFCM.005 | As a BDO Insure Contact Center Agent, I need to attach supporting documents to a case, so that the documents are stored with the case and the customer under the file and access rules. | FR-CSF-054 | FR-CSF-054 AC1-3 | TC-CSF-054.1 to 054.3 (3 cases) |
| BRCSFCM.006 | As Contact Center Management, I need BIBS to keep a complete audit trail of case actions - creation, updates, status, priority and escalation changes with the from and to values, user, time and attachments - so that what changed, who changed it and when can always be shown. | FR-CSF-055 | FR-CSF-055 AC1-4 | TC-CSF-055.1 to 055.4 (4 cases) |
| BRCSFCM.007 | As a Contact Center Supervisor, I need to reassign a case to another agent or team, escalate it with a reason, maintain the resolution and close it, so that cases keep moving and every ownership change is logged. | FR-CSF-056 | FR-CSF-056 AC1-4 | TC-CSF-056.1 to 056.4 (4 cases) |
| BRCSFCM.008 | As a Contact Center Supervisor, I need BIBS to track case ageing and SLA thresholds by case type and notify the agent, supervisor, fulfilment unit or escalation group when a breach approaches or happens, so that cases are resolved within their turnaround time. | FR-CSF-057, FR-CSF-059 | FR-CSF-057 AC1-4; FR-CSF-059 AC1-4 | TC-CSF-057.1 to 057.4, 059.1 to 059.4 (10 cases) |
| BRCSFCM.010 | As a BDO Insure Contact Center Agent, I need to close a case with its closure status and, when client information or action is still pending, create a new case linked to the closed one, so that the closed record stays unchanged and the follow-up remains traceable. | FR-CSF-058 | FR-CSF-058 AC1-3 | TC-CSF-058.1 to 058.3 (4 cases) |
| BRCSFCM.011 | As Contact Center Management, I need case management reports and dashboards on case volume, ageing, SLA compliance and status distribution, filtered by date, category, priority and status and exportable to Excel, CSV and PDF, so that I can monitor performance and workload. | FR-CSF-059 | FR-CSF-059 AC1-4 | TC-CSF-059.1 to 059.4 (5 cases) |
| BRCSFCM.012 | As a Fulfilment Unit, I need to receive the logged case, maintain its resolution and close it, or return it to the contact centre when it has an error or needs more handling, so that each case is resolved by the unit that owns it. | FR-CSF-060 | FR-CSF-060 AC1-4 | TC-CSF-060.1 to 060.4 (6 cases) |
| Lists of values I-VI (p.11-17) | As Contact Center Management, I need the case channels, case types with turnaround times, natures of transaction, product types, market segments and fulfilment units maintained as lists, so that cases are categorised, routed and reported consistently. | FR-CSF-061 | FR-CSF-061 AC1-3 | TC-CSF-061.1 to 061.3 (4 cases) |

<!-- landscape -->

# Appendix: Storyboard index

The Customer Servicing Facility has no end-to-end walkthroughs with screenshots yet. This index gives the storyboard of its two business processes from the process flows of chapter 2 (Figures 1 and 2): one frame per step, with the persona, the step, the screen or document used, the outcome and the FRs. The screen-level frames with screenshots follow with the v2.0 business sign-off pack of BRD-9, which adds the walkthroughs and the UX Screen Deck.

<!-- table: widths=1.3,2.8,5.6,3.6,5.2,2.2 caption="Storyboard of the business processes: frame, persona, step, screen or document, outcome and FR" size=8 -->
| Frame | Persona | Step | Screen or document | Outcome | FR |
|---|---|---|---|---|---|
| **P1** | | **Servicing contact: from the call to the audit report** | | | |
| P1.1 | BDO Insure Contact Center Agent | Signs in with user ID and password | Login | The Customer Search opens as the landing page; a wrong password is refused | FR-CSF-001, FR-CSF-002 |
| P1.2 | BDO Insure Contact Center Agent | Searches the caller by name, client ID, account, PN or application number | Customer Search | Results grouped by client with the matching accounts; one match opens directly | FR-CSF-010 |
| P1.3 | BDO Insure Contact Center Agent | Opens the client and reads the client information and accounts | Servicing View (summary card, Accounts tab) | Every account, CBG and non-CBG, with its CSF status and BIBS stage | FR-CSF-011, FR-CSF-012 |
| P1.4 | BDO Insure Contact Center Agent | Opens the payment history | Servicing View (Payments tab) | Payments of the last 12 months with the invoices and balances | FR-CSF-013 |
| P1.5 | BDO Insure Contact Center Agent | Verifies the caller with the checklist | Update Contact dialog (step 1) | Verification Passed (valid 30 minutes) or Failed | FR-CSF-020 |
| P1.6 | BDO Insure Contact Center Agent | Changes the mobile number with a reason | Update Contact dialog (step 2) | Contact saved in the client master; shown on the Contact History tab; queued for QPS and EBIX while they coexist | FR-CSF-021, FR-CSF-022 |
| P1.7 | BDO Insure Contact Center Agent | Resends the renewal advice to the registered e-mail | Renewal Advice tab; Resend dialog | RA sent password protected; the password follows in a separate e-mail | FR-CSF-030 |
| P1.8 | Contact Center Supervisor | Resends the RA to another address with a reason | Resend dialog | RA sent to the other address; the reason is logged | FR-CSF-030 |
| P1.9 | BDO Insure Contact Center Agent | Resends the confirmed e-policy | E-policies tab; Resend dialog | E-policy dispatched; listed in the e-policy dispatch report | FR-CSF-031 |
| P1.10 | BDO Insure Contact Center Agent | Uploads the client's document and retrieves a claims report | Documents tab; Upload dialog | Document stored on the account; claims report previewed and downloaded | FR-CSF-032, FR-CSF-033 |
| P1.11 | Contact Center Management | Prints and saves the audit trail and the Contact Changes and Agent Activity reports | Audit Trail; Reports (Customer Service) | PDF and Excel files with the same rows as the screen | FR-CSF-040, FR-CSF-041, FR-CSF-042 |
| **P2** | | **Case management: from logging to closure and reporting** | | | |
| P2.1 | BDO Insure Contact Center Agent | Clicks Log Case on the Servicing View and enters the channel, case type, nature of transaction, product type, market segment and fulfilment unit | Log Case dialog | Case ID CASE-yyyy-nnnnnn and the due date from the turnaround time; a duplicate open case is refused | FR-CSF-050, FR-CSF-051 |
| P2.2 | BDO Insure Contact Center Agent | Answers an inquiry on the call and clicks Resolve and Close | Log Case dialog | Case created as Closed by CSR with the resolution | FR-CSF-050, FR-CSF-052 |
| P2.3 | Fulfilment Unit | Receives the routed case in the Fulfilment Unit Queue | Cases (Fulfilment Unit Queue); notification | Case Open - With Fulfillment Unit; the account officer has a copy | FR-CSF-060 |
| P2.4 | Fulfilment Unit | Records the resolution and clicks Resolve and Close | Case record | Case Closed by Fulfillment Unit; the agent is notified | FR-CSF-060, FR-CSF-052 |
| P2.5 | Fulfilment Unit | Returns a case with an error, or asks the agent to act with the client | Case record (Return to Agent, Request CSR Action) | Case Open - Returned to Agent or Open - CSR Action Required in the agent's queue | FR-CSF-060, FR-CSF-052 |
| P2.6 | BDO Insure Contact Center Agent | Adds a note and a document, then re-endorses or closes the case | Case record (Notes, Documents) | Note and document listed with author and time; case back with the unit or Closed by CSR | FR-CSF-053, FR-CSF-054, FR-CSF-058 |
| P2.7 | Contact Center Supervisor | Reassigns a case and escalates another with a reason | Cases (Team Queue); Case record | New owner sees the case at once; Escalated indicator with the reason | FR-CSF-056 |
| P2.8 | Contact Center Supervisor | Follows the cases approaching or past their due date | Case Dashboard; notifications | Approaching breach and Breached indicators; breach notices; automatic escalation | FR-CSF-057 |
| P2.9 | BDO Insure Contact Center Agent | Creates a follow-up case from a closed case while a client item is pending | Case record (Create Follow-up Case, Linked Cases) | New linked case; the closed case stays read only | FR-CSF-058 |
| P2.10 | Contact Center Management | Reviews the case history and the audit trail of a case | Case record (History); Audit Trail | Every action with from and to values, user and time | FR-CSF-055 |
| P2.11 | Contact Center Management | Opens the case dashboard and exports the case reports | Case Dashboard; Reports (Case Register, Case Ageing and SLA, Case Summary) | Same figures on the dashboard and in the Excel, CSV and PDF reports | FR-CSF-059 |
| P2.12 | System Administrator | Maintains the case lists and the routing table at the request of Contact Center Management | Lists of Values; Case Routing | New values offered on the next case; deactivated values kept on old cases | FR-CSF-061 |

