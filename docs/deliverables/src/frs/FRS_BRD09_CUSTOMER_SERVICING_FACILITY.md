---
# Source of the Functional Requirements Specification for BRD-9 Customer Servicing Facility.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD09_CUSTOMER_SERVICING_FACILITY.md
title: Customer Servicing Facility
subtitle: BRD-9 Customer Servicing Facility (CSF) and the scope e-mail of 13-Feb-2026
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-09
name: Customer Servicing Facility
doc_id: BIBS-FRS-BRD-09
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-9 Customer Servicing Facility
output: FRS/BIBS_FRS_BRD-09_Customer_Servicing_Facility_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-9 baseline and the Customer Servicing build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decision D3
distribution:
  - {name: "Product Owner, Customer Servicing Facility", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Alternative Distribution Head", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: BDO Insure Contact Center Management, role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: Marketing Business Services and System Support (MBS), role: Business user, organisation: BDOI, purpose: Review of the contact and status rules}
  - {name: ITG Core Business - Insurance Applications, role: Reviewer, organisation: BDO Unibank, purpose: Review of the legacy coexistence seams}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Customer Servicing Facility (CSF) business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to build, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

The CSF module is designed and not yet built. This FRS is written from the BRD, the requirements baseline (R3) and the build design (R4). Section 1.5 explains what that means for the screen, API and message-code entries.

## Scope

The CSF replaces the BDO-Insure Front-End System used by the BDO Insure Contact Center (p.4). Agents answer clients who call the hotline, write e-mails or use the website. In BIBS the CSF is a **servicing workspace over the BIBS modules**: client, account, invoice, payment, e-policy, renewal advice and document data stay in their owning modules, and the CSF reads them. The only changes an agent makes are contact details (after verifying the caller), resends, and document uploads.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access | Log-in with user ID and password, credential validation, role-based access for agents, supervisors and management | BRCSF-001, 1.001, 1.002; p.4 |
| Search and view | Search by name, client ID, account number, PN number and application number; client information; accounts with their status; payment history | BRCSF-002, 003, 3.001, 005, 5.001, 008; e-mail item 7 |
| Contact details | Verification of the caller, contact-only update, write-back seam for QPS and EBIX | BRCSF-002, 004 |
| Resend and documents | View, download and resend the renewal advice and the e-policy; upload and retrieve documents | BRCSF-006, 6.001, 007, 7.001, 009, 9.001; e-mail item 9 |
| Audit and backup | Audit trail of client changes, print or save in PDF or Excel, agent activity, backup every 15 minutes | BRCSF-010, 011, 11.001, 11.002 |

**Out of scope for this phase:**

- Case management and inquiry logging (SharePoint today). The e-mail of 13-Feb-2026 defers it to a future enhancement with its own requirement document (row CSF-EM10, p.17).
- The open items of the e-mail (priority order for servicing of payments versus claims; parameter alignment between the loan system and the insurance system), which are also future enhancements (p.17; CSQ16).
- Changes other than contact details (for example civil status); they stay with the fulfilment unit (e-mail topic 3, p.17).
- The transport of contact updates to QPS and EBIX and the lookup of accounts that exist only there. Both are seams until BDOI specifies the interface (CSQ01).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Customer Servicing Facility (CSF) BRD, pages 1-16 (BRCSF-001 to 011; usage requirements; approval sheet) | v1.0, 09-Jun-2025; approved 24-Jun to 02-Jul-2025 | `docs/source-documents/Customer Servicing Facility.PDF` |
| R2 | E-mail thread "Core Modernization: CSF", pages 17-18 (minutes of 23-Jan-2026; scope confirmation of 13-Feb-2026) | 22-Jan to 13-Feb-2026 | same file |
| R3 | BDOI Customer Servicing Facility (BRD-9) requirements baseline and fit/gap | current | `docs/requirements/BDOI_CSF_BRD_SPEC.md` |
| R4 | Customer Servicing Facility build design (module `csf`) | proposal for review | `docs/architecture/CUSTOMER_SERVICING_DESIGN.md` |
| R5 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | binding | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R6 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |

Page references in this document ("p.8") are pages of the BRD-9 PDF. The BRD reuses its BR ID for the process steps (for example BRCSF-003 / 3.001); FRs cite both. The e-mail items 7 and 9, which the e-mail of 13-Feb-2026 adds to the scope, are traced as CSF-EM07 and CSF-EM09.

## Definitions and acronyms

```glossary
Agent: BDO Insure Contact Center Agent
ARN: Account Reference Number of a BIBS account
BRCSF: Requirement ID prefix of the CSF BRD (BRCSF-001 to 011)
CBG: Consumer Banking Group; today's system shows full details only for CBG accounts (p.4)
CSF: Customer Servicing Facility
CSF-EMnn: Requirement added or deferred by the e-mail of 13-Feb-2026 (item nn, p.17)
CSF status: Status shown to the agent (Pending, Awaiting, Booked, Open, Closed), mapped from the BIBS account stage and payment status
CSQnn: Open question on BRD-9 raised by the project team (section 10.3)
EBIX / QPS: Legacy systems that are today's sources of client and account data (p.3)
FR: Functional requirement of this document (FR-CSF-nnn)
LOS: Loan origination system, source of PN and application numbers
PN: Promissory note number of a bank loan linked to an insurance account
RA: Renewal Advice
UAT: User acceptance testing
Verification: The check of the caller's identity before a change (address, contact number, e-mail, insured property)
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R3), the **screens** and the **API**.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained in BIBS (parameter, list of values, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-9 test plan.

> [!NOTE] Designed, not built
> Screen names, permissions, parameters and lists come from the build design (R4). API paths and business message codes are fixed when the module is built: the API entry reads "To be assigned at build" and the Code column reads "To be assigned at build". Codes quoted in this document exist in the platform today (for example ACCESS_DENIED). Values marked "default" are placeholders that BDOI confirms through the open questions in section 10.3; they are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R3)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the platform built for BRD-1 and BRD-2 |
| CONFIGURE | Needs set-up only (parameters, infrastructure settings) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-9 |
| OUT | Out of scope per the BRD pack |

# Business context and process overview

## Business context

The BDO-Insure Front-End System was built by BDO IT for the contact centre agents. It shows full insurance account details only for Consumer Banking Group (CBG) accounts; for non-CBG accounts it shows only the contact details, account number and expiry date. Agents can update only client contact details. Its servers have reached end of life, and it has no role-based access control (p.4).

The BRD asks for an application that shows client contact details and account information from the data sources, and that sends contact updates back to them (p.3, p.5). In January 2026 the BRD was clarified with the contact centre and BDO IT: search keys, verification before an update, payment history depth, two more statuses and e-policy resending (p.17).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.3-5, e-mail p.17)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Full account details only for CBG accounts | Every account of the client, CBG and non-CBG, with its status and payments |
| 2 | Search on a limited set of keys | One search box for name, client ID, account number, PN number and application number |
| 3 | No role-based access control | Agent, supervisor and management roles with separate rights to update, resend and upload |
| 4 | Contact updates are written in QPS or EBIX | The update is made once in the BIBS client master after verifying the caller; a write-back seam serves QPS and EBIX while they coexist |
| 5 | RA and e-policy resends are handled outside the tool | The agent views, downloads and resends the RA and the e-policy to the registered e-mail, password protected |
| 6 | No audit report for management | Every change and agent action is logged; the audit trail prints or saves in PDF or Excel |

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
| 7 | Legacy sync | System | Contact change queued for QPS and EBIX while they coexist (parked) | p.3; CSQ01 |
| 8 | Audit and reports | Management | Audit trail, contact changes and agent activity in PDF or Excel | BRCSF-010, 011 |

![Servicing flow of a contact centre agent (BRCSF-001-011)](figures/brd09_process_flow.dot){width=13}

## Data sources in BIBS

The BRD names QPS and EBIX as the data sources (p.3). In BIBS the data live in the BrokerVerse modules. The CSF reads them through their query services and stores only its own records: contact change requests with their verification, the legacy sync outbox and the agent activity log.

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

# Personas and roles

## Personas

<!-- table: widths=3.6,4,7.2,2.8 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in the CSF | BRD |
|---|---|---|---|
| BDO Insure Contact Center Agent | CSF_AGENT | Searches clients; views client, accounts, payments and documents; verifies callers and updates contacts; resends RAs and e-policies; uploads documents | BRCSF-001-009 |
| Contact Center Supervisor, BDO Contact Center personnel | CSF_SUPERVISOR | As the agent; resends to another address with a reason; runs CSF reports | p.13 |
| Contact Center Management, Leads / Heads | CSF_MANAGEMENT | Views the audit trail and the CSF reports; prints or saves them | BRCSF-011 / 11.002; p.13 |
| System Administrator | SYSADMIN | Users and roles; restricts access to authorised users | BRCSF-001 |

The BRD asks for differentiated roles for updating records and uploading documents (p.4). The roles above are the project's proposal until BDOI confirms the matrix (CSQ10). The Renewal design has its own Contact Center role for renewal follow-ups; whether it is the same group of people is open (XQ02). A user may hold both roles.

## Permissions

<!-- table: widths=5,11.6 caption="CSF permissions (build design R4, section 6.1)" -->
| Permission | Allows |
|---|---|
| CSF_VIEW | Customer Search and Servicing View |
| CSF_CONTACT_UPDATE | Verification and contact changes |
| CSF_RESEND | Resend the RA and the e-policy to the registered e-mail |
| CSF_RESEND_OTHER | Resend to another address, with a reason |
| CSF_DOCUMENT_UPLOAD | Upload documents from the Servicing View |
| CSF_REPORT_VIEW | CSF reports |
| AUDIT_VIEW (existing) | Audit trail report CTL-AUDIT |
| ATTACHMENT_VIEW (existing) | Open and download documents (subject to the access classes) |

Agents do not receive CLIENT_MAINTAIN or EPOLICY_SEND. Their changes go through the narrow CSF permissions, which call a contact-only update and the e-policy dispatch service.

## Permissions matrix

<!-- table: widths=5,2.9,2.9,2.9,2.9 caption="Role-to-action matrix for the CSF (proposal until CSQ10)" size=8.5 -->
| Permission | CSF Agent | CSF Supervisor | CSF Management | System Admin |
|---|---|---|---|---|
| CSF_VIEW | Y | Y | Y | |
| CSF_CONTACT_UPDATE | Y | Y | | |
| CSF_RESEND | Y | Y | | |
| CSF_RESEND_OTHER | | Y | | |
| CSF_DOCUMENT_UPLOAD | Y | Y | | |
| CSF_REPORT_VIEW | | Y | Y | |
| AUDIT_VIEW | | | Y | Y |
| ATTACHMENT_VIEW | Y | Y | | |
| User and role administration | | | | Y |

CSF roles see every client and account, CBG and non-CBG (p.4; CQ06), with no segment scoping unless BDOI asks for one (CSQ10).

# Functional requirements

## Access

```fr
id: FR-CSF-001
title: Log in with user ID and password
brd: [BRCSF-001 (p.6), BRCSF-001 / 1.001 (p.6-7), BRCSF-001 / 1.002 (p.7)]
actor: Contact Center Agent; System
priority: Must have
fit: FIT
screens: Login; Customer Search (landing page)
api: POST /api/v1/auth/login (existing)
description: Agents log in with their BIBS user ID and password on the platform log-in. BIBS validates the credentials and grants access to the CSF screens of the user's roles; the Customer Search opens as the landing page. Wrong credentials are refused. Directory sign-in (Windows ID) is built as a parked port for all BIBS users; local sign-in stays until BDO supplies the interface (decision D6).
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
  - [User ID or password wrong, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
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
brd: [BRCSF-001 (p.6), "Current process limitations (p.4)"]
actor: System; System Administrator
priority: Must have
fit: FIT
screens: All CSF screens
api: Every endpoint checks its permission
description: Only authorised personnel reach the CSF. Every CSF screen, button and call needs one of the permissions of section 3.2, so updating contacts, resending to another address, uploading documents and running reports are separate rights (p.4). Menus show only what the user's roles allow. Role grants change through User Access Maintenance requests (BRD-11).
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a CSF screen or starts an action.
  - BIBS checks the permission.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The action is not offered; a direct call is refused (HTTP 403) and logged.
rules:
  - [R1, "Grants of section 3.3 until BDOI confirms the matrix (CSQ10).", Configurable, User Access Maintenance request]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Refused calls with user, endpoint and time."
acceptance:
  - A CSF Management user does not see the Update Contact action.
  - An agent's direct call to resend to another address is refused with ACCESS_DENIED.
```

## Search and servicing view

```fr
id: FR-CSF-010
title: Search a client by name, client ID, account, PN or application number
brd: [BRCSF-003 (p.7-8), BRCSF-003 / 3.001 (p.8), "E-mail topic 4 (p.17)"]
actor: Contact Center Agent (CSF_VIEW)
priority: Must have
fit: CHANGE
screens: Customer Search
api: To be assigned at build
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
  - PN or application number of an account not in BIBS. The legacy lookup seam returns nothing until QPS, EBIX or LOS are connected (CSQ01, CSQ02).
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
  - A search returns within 3 seconds (p.13).
```

```fr
id: FR-CSF-011
title: View current client information and accounts
brd: [BRCSF-002 (p.7), BRCSF-008 (p.10)]
actor: Contact Center Agent (CSF_VIEW)
priority: Must have
fit: "CHANGE (BRCSF-002), FIT (BRCSF-008)"
screens: Servicing View (summary card; tabs Accounts, Payments, Renewal Advice, E-policies, Documents, Contact History)
api: To be assigned at build
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
  - [R1, "No segment scoping for CSF roles (p.4; CQ06).", Configurable, Change request after CSQ10]
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
brd: [BRCSF-005 / 5.001 (p.8), CSF-EM07 (p.17)]
actor: Contact Center Agent
priority: Must have
fit: CHANGE
screens: Servicing View (Accounts tab)
api: To be assigned at build
description: Each account shows the latest CSF status - Pending, Awaiting, Booked, Open or Closed - next to its BIBS stage. BIBS computes the status from the account stage, the invoice payment status and the policy period through a maintainable mapping, so the definitions agreed by Marketing, Processing and Operations can be applied without a new build (e-mail open item 3).
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
  - Changing the mapping of a stage changes the displayed status without a new build.
```

```fr
id: FR-CSF-013
title: View the payment history
brd: [BRCSF-005 (p.8), "E-mail topic 6 (p.17)"]
actor: Contact Center Agent
priority: Must have
fit: CHANGE
screens: Servicing View (Payments tab)
api: To be assigned at build
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
brd: [BRCSF-004 (p.8), "E-mail topic 5 (p.17)"]
actor: Contact Center Agent (CSF_CONTACT_UPDATE)
priority: Must have
fit: CHANGE
screens: Update Contact dialog (step 1, verification)
api: To be assigned at build
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
  - [Verification failed, The caller could not be verified. The contact details cannot be changed, To be assigned at build]
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
brd: [BRCSF-004 (p.8), BRCSF-002 (p.7), BRCSF-010 (p.11)]
actor: Contact Center Agent (CSF_CONTACT_UPDATE)
priority: Must have
fit: CHANGE
screens: Update Contact dialog (step 2); Servicing View (Contact History tab)
api: To be assigned at build
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
  - [No valid verification, Verify the caller before changing the contact details, To be assigned at build]
  - [Field not updatable, "<field> cannot be changed here. Refer the client to the fulfilment unit", To be assigned at build]
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
  - The update is saved within 3 seconds (p.13).
```

```fr
id: FR-CSF-022
title: Queue contact changes for QPS and EBIX while they coexist
brd: [BRCSF-002 (p.7), "Executive summary and envisioned process (p.3, p.5)"]
actor: System
priority: Must have
fit: CHANGE
screens: Servicing View (Contact History, sync status); CSF Contact Changes report
api: Port ContactSyncGateway (to be implemented at build)
description: The BRD asks that contact updates be sent back to the data sources (QPS and EBIX, p.3). In BIBS the client master is updated at once; the write-back to QPS and EBIX, which matters only while they coexist, goes through an outbox. Until BDOI specifies the interface, each change is stored with the status "not configured" and nothing is sent. When the interface exists, a job sends queued changes every 15 minutes, retries failures and raises an alert.
preconditions:
  - "A contact change was applied."
main_flow:
  - BIBS writes the outbox rows for QPS and EBIX.
  - With sync disabled, the status is NOT_CONFIGURED.
  - With sync enabled, the job sends the rows and sets SENT or FAILED.
rules:
  - [R1, "Sync off until the interface is specified (CSQ01).", Configurable, Parameter CSF_LEGACY_SYNC_ENABLED]
  - [R2, "Every change keeps its payload, so the outbox can be replayed.", Fixed, "-"]
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
> The BRD describes a front end that reads QPS and EBIX and writes updates back to them. In BIBS the client and account data are in BIBS, so the CSF reads BIBS and the write-back is a seam (CSQ01).

## Renewal advice, e-policy and documents

```fr
id: FR-CSF-030
title: View, download and resend the renewal advice
brd: [BRCSF-006 (p.9), BRCSF-006 / 6.001 (p.9), "E-mail topic 8 (p.17)"]
actor: Contact Center Agent (CSF_RESEND); Supervisor (CSF_RESEND_OTHER)
priority: Must have
fit: NEW
screens: Servicing View (Renewal Advice tab); Resend dialog
api: To be assigned at build
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
  - [No registered e-mail, The client has no registered e-mail. Update the contact details first, To be assigned at build]
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
brd: [CSF-EM09 (p.17)]
actor: Contact Center Agent (CSF_RESEND)
priority: Must have
fit: CHANGE
screens: Servicing View (E-policies tab); Resend dialog
api: Existing e-policy dispatch service, called under CSF_RESEND
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
  - [No confirmed e-policy, The account has no confirmed e-policy, To be assigned at build]
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
brd: [BRCSF-007 (p.9), BRCSF-007 / 7.001 (p.9-10)]
actor: Contact Center Agent (CSF_DOCUMENT_UPLOAD)
priority: Must have
fit: "CHANGE (BRCSF-007), FIT (7.001)"
screens: Servicing View (Documents tab, Upload)
api: Attachment service /api/v1/attachments (existing, file types added)
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
  - A document upload completes within 5 seconds (p.13).
```

```fr
id: FR-CSF-033
title: View, retrieve or download policy-related documents
brd: [BRCSF-009 (p.10), BRCSF-009 / 9.001 (p.10)]
actor: Contact Center Agent
priority: Must have
fit: CHANGE
screens: Servicing View (Documents tab)
api: Attachment service /api/v1/attachments (existing)
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
  - The documents are retrieved within 5 seconds (p.13).
```

## Audit trail, reports and backup

```fr
id: FR-CSF-040
title: Record changes to client information as an audit trail
brd: [BRCSF-010 (p.11)]
actor: System
priority: Must have
fit: FIT
screens: Servicing View (Contact History); Audit Trail (Administration)
api: Audit service (existing)
description: Every change to client information is recorded with the entity, field, old and new values, user, time and source. CSF contact changes also carry the verification reference and the reason. Audit rows are append-only.
preconditions:
  - "None."
main_flow:
  - A user changes client information.
  - BIBS writes the audit entry in the same transaction.
rules:
  - [R1, "Audit rows are append-only.", Fixed, "-"]
  - [R2, "Retention 5 years online, 15 years archive (p.14).", Configurable, Retention rule CSF_CONTACT_CHANGE]
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
brd: [BRCSF-011 (p.11), BRCSF-011 / 11.002 (p.12)]
actor: Contact Center Management (AUDIT_VIEW, CSF_REPORT_VIEW)
priority: Must have
fit: FIT
screens: Administration, Audit Trail (CTL-AUDIT); Reports (category Customer Service)
api: Report service /api/v1/reports; codes CTL-AUDIT, CSF-CONTACT-CHANGES
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
  - The report is generated within 5 seconds (p.13).
```

```fr
id: FR-CSF-042
title: Log agent activity for leads and heads
brd: ["Usage requirements - report generation by Leads / Heads (p.13)", BRCSF-010 (p.11)]
actor: System; Supervisor, Management (CSF_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Reports (CSF-ACTIVITY)
api: Report service /api/v1/reports; code CSF-ACTIVITY
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
  - "The activity log is itself append-only."
acceptance:
  - An agent's search, view and resend appear in the Agent Activity report for that day.
```

```fr
id: FR-CSF-043
title: Back up the data every 15 minutes
brd: [BRCSF-011 / 11.001 (p.11)]
actor: Infrastructure (System)
priority: Must have
fit: CONFIGURE
screens: None
api: None (database infrastructure)
description: The BIBS database is backed up continuously - write-ahead log archiving at most every 15 minutes to the designated secure storage, plus daily base backups. Each backup is time stamped, complete and restorable, and is tested monthly by a restore. Archiving runs without degrading the online response times. The requirement applies to the whole BIBS database, not only to CSF data.
preconditions:
  - "None."
main_flow:
  - The database archives its log at least every 15 minutes.
  - The daily base backup runs outside the peak hours.
  - A monthly restore test proves the backups.
rules:
  - [R1, "Archive interval at most 15 minutes.", Configurable, Database setting archive_timeout]
  - [R2, "Whether the 15-minute backup is BIBS-wide (RPO 15 minutes) is confirmed under CSQ11 and XQ08.", Configurable, Infrastructure decision]
validations: []
notifications:
  - "A failed archive or backup alerts the infrastructure team."
audit:
  - "Backup and restore test logs kept by the infrastructure team."
acceptance:
  - A restore test recovers the database to a point less than 15 minutes before the test.
  - Online response times during archiving stay within the targets of section 8.
```

# Status and state model

The CSF has no approval workflow. Two models apply: the CSF status of an account and the states of a contact change.

## CSF status of an account

<!-- table: widths=2.6,7.8,6.2 caption="CSF status mapping (seed of LOV CSF_STATUS_MAP; definitions to confirm, CSQ04)" -->
| CSF status | BIBS account stage and payment status | Source |
|---|---|---|
| Pending | DRAFT, SUBMITTED, RETURNED_TO_MARKETING | BRCSF-005 / 5.001 |
| Awaiting | AWAITING_PAYMENT, READY_FOR_PLACEMENT, PLACED, RETURNED_BY_INSURER | BRCSF-005 / 5.001 |
| Booked | BOOKED with premium receivable outstanding | CSF-EM07 |
| Open | BOOKED and in force | CSF-EM07 |
| Closed | CANCELLED, VOIDED, or policy period ended | CSF-EM07 |

The BRD does not define Pending and Awaiting; the e-mail says the statuses follow the definitions of Marketing, Processing and Operations. The mapping is data, so the agreed definitions are applied without a new build.

## Contact change and legacy sync

Figure 2 shows the states of a contact change. A verification passes or fails; a passed verification allows the change for 30 minutes (default). An applied change is queued for QPS and EBIX only when the legacy sync is enabled.

<!-- table: widths=3.4,13.2 caption="Contact change states" -->
| State | Meaning |
|---|---|
| PASSED / FAILED | Result of the verification checklist (FR-CSF-020) |
| APPLIED | The contact change is saved in the client master (FR-CSF-021) |
| REFUSED | The change asked for a field the CSF cannot change |
| NOT_CONFIGURED | Legacy sync disabled; the change stays in the outbox (FR-CSF-022) |
| QUEUED / SENT / FAILED | Legacy sync enabled; sending state of the outbox row |

![Verification, contact change and legacy sync states](figures/brd09_contact_change.dot){width=13}

# Reports and documents

## Reports

<!-- table: widths=4.2,4.2,6.2,2 caption="CSF reports (category Customer Service)" size=8.5 -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| CTL-AUDIT (existing) | Audit Trail | Changes to client records with old and new values | BRCSF-011 |
| CSF-CONTACT-CHANGES | Contact Changes | Contact changes with verification, agent, reason and sync status | BRCSF-010, 011 |
| CSF-ACTIVITY | Agent Activity | Searches, views, downloads, resends, uploads and changes per agent | p.13 |

Reports export to PDF, Excel and CSV and support saved variants. CTL-AUDIT needs AUDIT_VIEW; the CSF reports need CSF_REPORT_VIEW.

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

## Documents

The CSF generates no documents. It reads and resends documents of other modules: RENEWAL_ADVICE (Renewal, Employee Benefits), CLAIM_REPORT (Claims), e-policies (Issuance) and quotations. Uploaded documents take a type from the list CSF_DOCUMENT_TYPE, a subset of the platform document types.

# Interfaces and integration

Figure 3 shows the interfaces. The CSF depends on the BIBS modules through their query services; no module depends on the CSF.

![Interfaces of the Customer Servicing Facility (dashed = parked)](figures/brd09_integration.dot)

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Client master (CRM) | In / Out | Client information; contact-only update with reason, source and verification | BRCSF-002, 004, 008 | DESIGNED |
| Accounts (BRD-1) | In | Accounts by client, PN numbers, stage | BRCSF-002, 003 | DESIGNED |
| Placement (BRD-1) | In | Accounts by loan application number | BRCSF-003 | DESIGNED |
| Issuance (BRD-1) | In / Out | Policy numbers, e-policies; resend through the dispatch service | CSF-EM09 | DESIGNED |
| Invoice ledger and Cashiering (BRD-2) | In | Invoices, balances, payments of a client | BRCSF-005 | DESIGNED |
| Documents | In / Out | RENEWAL_ADVICE, CLAIM_REPORT and other documents; uploads | BRCSF-006, 007, 009 | DESIGNED |
| E-mail outbox | Out | RA resend, protected, with separate password | BRCSF-006 | DESIGNED |
| QPS / EBIX contact write-back | Out | Contact changes through the outbox | p.3; BRCSF-002 | PARKED |
| QPS / EBIX / LOS account lookup | In | Accounts not migrated to BIBS by PN or application number | BRCSF-003 | PARKED |
| Case management | - | Inquiry logging and case tracking | CSF-EM10 | OUT |

> [!PARKED] Parked seams
> The write-back to QPS and EBIX keeps every change in an outbox with status "not configured" (CSQ01); a replay is possible once the interface is specified. The lookup of accounts that exist only in QPS, EBIX or the loan system returns nothing until it is connected (CSQ02).

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.13-14)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | 24 concurrent agents, supervisors and personnel; 16 document users; 8 report users (leads / heads) | Small load within the BRD-1 sizing | FIT |
| Volumes | 144,400 transactions a year for access, retrieval and search (about 600 a working day); 113,800 document transactions; 1% growth | Indexed search keys; paged results | FIT |
| Response time | Under 3 seconds for retrieval, updates and search; 5 seconds for documents and reports | Each Servicing View tab loads on its own; search on indexed keys; no report on the agent's path | CONFIGURE |
| Peak | First quarter; 10:00-12:00 and 14:00-16:00 | No batch work in these windows | FIT |
| Availability | 99.9%; used 06:00-22:00; maintenance weekdays and Saturdays 21:00-05:00 | The maintenance window overlaps usage 21:00-22:00 (CSQ12) | CONFIGURE |
| Recovery | RTO 4 hours; RPO 4 hours | Met by log archiving (FR-CSF-043) | FIT |
| Backup | Every 15 minutes (BRCSF-011 / 11.001); every 4 hours in the retention table | Log archiving every 15 minutes plus daily base backups (CSQ11) | CONFIGURE |
| Retention | Client and account data, contact updates, audit trail, attachments - 5 years online, 15 years archive; backups kept 5 years | Retention rule CSF_CONTACT_CHANGE; platform rules for the other records | CHANGE |
| Anonymisation | Not required | None | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Usability and security | User-friendly, intuitive; secure access (p.13) | BDO UX guidelines (R6); role-based access (FR-CSF-002) | FIT |

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
| Job CSF_LEGACY_SYNC | every 15 minutes | Sends queued contact changes (manual only until sync is enabled) |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| CSF_STATUS_MAP | The mapping of section 5.1 |
| CSF_VERIFY_CHECK | Address; Contact number; E-mail; Insured property (CSQ03) |
| CSF_CHANGE_REASON | Delivered with generic values (client request, correction, returned mail); BDOI supplies its own |
| CSF_DOCUMENT_TYPE | Subset of the platform document types for contact centre uploads (CSQ07) |

## Platform settings

<!-- table: widths=5.4,11.2 caption="Platform settings used by the CSF" size=8.5 -->
| Setting | Value |
|---|---|
| Allowed file types | Platform list plus TXT, RTF, HEIC / HEIF, GIF, BMP, TIFF, WEBP (BRCSF-007) |
| Document access classes | Rows for RENEWAL_ADVICE and CLAIM_REPORT that let the CSF roles list and download them (XQ04) |
| Database archive interval | At most 15 minutes (BRCSF-011 / 11.001) |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-CSF-01 | The signed BRD (p.1-16) is the baseline; the e-mail of 13-Feb-2026 adds e-mail items 7 and 9 and defers item 10 | R1, R2 |
| A-CSF-02 | BIBS is the source of client and account data; QPS and EBIX matter only while they coexist | CSQ01 |
| A-CSF-03 | Agents change contact details only; other changes go to the fulfilment unit | E-mail topic 3 |
| A-CSF-04 | CSF agents see every segment (CBG and non-CBG) | p.4; CQ06 |
| A-CSF-05 | Inquiry logging and case management stay out of this phase | CSQ09 |
| A-CSF-06 | The 15-minute backup applies to the whole BIBS database | CSQ11 |

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
| CSQ09 | Case management out of this phase; a minimal interaction note | Section 1.2 | OPEN |
| CSQ10 | Role matrix; who uploads and who updates contacts | Section 3 | OPEN |
| CSQ11 | 15-minute backup BIBS-wide or CSF only | FR-CSF-043 | OPEN |
| CSQ12 | Usage hours overlap the maintenance window; Sunday usage | Section 8 | OPEN |
| CSQ13 | E-policy resend: confirmed e-policy only; password | FR-CSF-031 | OPEN |
| CSQ14 | Should a contact update also change the contacts of open accounts | FR-CSF-021 | OPEN |
| CSQ15 | Additional contacts per client | FR-CSF-021 | OPEN |
| CSQ16 | Priority order for servicing; loan system parameter alignment | Section 1.2 | OPEN |
| XQ02 | Contact Center roles of Renewal and CSF: same people or not | Section 3.1 | OPEN |
| XQ04 | Confidentiality matrix of shared document types | FR-CSF-033 | OPEN |
| XQ05 | Which claim documents are claims reports | FR-CSF-033 | OPEN |
| CQ06 | Data scope by segment for the contact centre | Section 3.3 | PARTIAL |
| Q42 | BDO single sign-on / Windows ID | FR-CSF-001 | ANSWERED |

## Differences between the design and the BRD

<!-- table: widths=3,7,6.6 caption="Recorded differences" size=8.5 -->
| BRD item | BRD text | FRS and design |
|---|---|---|
| BRCSF-002, p.3, p.5 | Retrieve data from QPS and EBIX and send updates back to them | BIBS is the source; the write-back is an outbox seam (FR-CSF-022; CSQ01) |
| BRCSF-004 | View, add or update contact details | Update after a recorded verification (e-mail topic 5); "add" = additional contacts only if CSQ15 confirms |
| BRCSF-011 / 11.001 | Backup every 15 minutes, under the audit trail ID | Infrastructure requirement for the whole database (FR-CSF-043); conflicts with the 4-hour backup of the retention table (CSQ11) |
| BRCSF-001 | User ID and password | Local sign-in; directory sign-in built as a parked port for all BIBS users (decision D6) |
| CSF-EM10 | Case management | Out of scope per the e-mail of 13-Feb-2026 |

# Traceability

Every BRD-9 requirement row is met by at least one FR, except CSF-EM10, which the e-mail of 13-Feb-2026 defers. The screen column names the main entry point; the last column names the section of the build design (R4). The non-functional requirements (p.13-14) are traced in section 8.

<!-- table: widths=3.2,3.2,5.6,4.6 caption="BRD ID to FR, screen and design" size=8 -->
| BRD ID | FR | Screen | Design (R4) |
|---|---|---|---|
| BRCSF-001 | FR-CSF-001, FR-CSF-002 | Login | 6 |
| BRCSF-001 / 1.001 | FR-CSF-001 | Login; Customer Search | 6, 10 |
| BRCSF-001 / 1.002 | FR-CSF-001 | Login | 6 |
| BRCSF-002 | FR-CSF-011, FR-CSF-021, FR-CSF-022 | Servicing View | 5, 2.2 |
| BRCSF-003 | FR-CSF-010 | Customer Search | 5 |
| BRCSF-003 / 3.001 | FR-CSF-010 | Customer Search | 5 |
| BRCSF-004 | FR-CSF-020, FR-CSF-021 | Update Contact dialog | 5, 11 |
| BRCSF-005 | FR-CSF-013 | Payments tab | 5, 11 |
| BRCSF-005 / 5.001 | FR-CSF-012 | Accounts tab | 5 |
| CSF-EM07 | FR-CSF-012 | Accounts tab | 5 |
| BRCSF-006 | FR-CSF-030 | Renewal Advice tab | 5, 11 |
| BRCSF-006 / 6.001 | FR-CSF-030 | Resend dialog | 5 |
| CSF-EM09 | FR-CSF-031 | E-policies tab | 5, 11 |
| BRCSF-007 | FR-CSF-032 | Documents tab (Upload) | 8 |
| BRCSF-007 / 7.001 | FR-CSF-032 | Documents tab (Upload) | 5 |
| BRCSF-008 | FR-CSF-011 | Servicing View (summary card) | 5 |
| BRCSF-009 | FR-CSF-033 | Documents tab | 5, 11 |
| BRCSF-009 / 9.001 | FR-CSF-033 | Documents tab | 5 |
| BRCSF-010 | FR-CSF-040, FR-CSF-021, FR-CSF-042 | Contact History; Audit Trail | 1, 5 |
| BRCSF-011 | FR-CSF-041 | Audit Trail; Reports | 9 |
| BRCSF-011 / 11.001 | FR-CSF-043 | None (infrastructure) | 8 |
| BRCSF-011 / 11.002 | FR-CSF-041 | Audit Trail; Reports | 9 |
| CSF-EM10 | Out of scope (e-mail of 13-Feb-2026) | - | 12 |


# Sign-off

By signing, BDOI confirms that this FRS describes the Customer Servicing Facility functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Customer Servicing Facility", organisation: BDOI}
  - {name: "", role: Alternative Distribution Head, organisation: BDOI}
  - {name: "", role: "Head, Contact Center Management", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
