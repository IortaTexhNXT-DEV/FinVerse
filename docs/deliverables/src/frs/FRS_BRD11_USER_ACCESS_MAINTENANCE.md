---
# Source of the Functional Requirements Specification for BRD-11 User Access Maintenance.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD11_USER_ACCESS_MAINTENANCE.md
# The traceability table (section 11) and the NFR table (section 8) were generated once from the rows of
# docs/requirements/BDOI_UAM_BRD_SPEC.md (sections 5 and 7); keep them in step with the FR BRD traces when editing.
title: User Access Maintenance
subtitle: BRD-11 User Access Maintenance (QPS User Access Maintenance Module)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-11
name: User Access Maintenance
doc_id: BIBS-FRS-BRD-11
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-11 User Access Maintenance
output: FRS/BIBS_FRS_BRD-11_User_Access_Maintenance_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-11 baseline, the build design and the built access functions
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; built access functions as the baseline, BRD changes specified from the design; aligned with the cross-BRD decisions
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Business Administrators (process owner)", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Unit Heads, Combank and Corbank Marketing / Corporate Processing", role: Approver, organisation: BDOI, purpose: Review of the request and approval steps}
  - {name: "ITIO-SRE and ITSD-AMS", role: System administration, organisation: BDO Unibank, purpose: "Review of the System Administrator functions, sign-in and logs"}
  - {name: "ES-BPDS, Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the User Access Maintenance business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that user access will be managed as the business expects. The project team uses it to build, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

Part of user access is already built in BIBS: users, roles, permissions, sign-in with lock-out, the session policy, access requests under four eyes and the User Access Matrix (built for BRD-1 and BRD-3). This document describes those functions as the **baseline** (section 2.3) and specifies the BRD-11 changes on top of them from the build design (R3). Where a function is built, the FR quotes the screen names, messages and codes of the build. Where it is new, the message code is assigned at build and the FR gives the message text only.

## Scope

The BRD was written in April 2025 for the Quotation and Pre-processing System (QPS), before BIBS. In BIBS it becomes the **system-wide** user access maintenance: every BIBS module uses the same users, group profiles (BIBS roles) and permissions (UQ19).

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Sign-in and session | Log-in by persona, inactivity warning and log-out, lock-out, directory authentication (EUA / Windows ID), password policy, log-out and session log | BRD 1.001, 2.001, 3.001, 4.001; NFR p.13-14, 17 |
| User access requests | Enrol, modify, deactivate and reactivate users; drafts, remarks, chosen approver, correction of returned requests, cancellation, view of own requests, bulk requests, effective dates | BRD 1.002 to 1.009; NFR p.13, 17 |
| Approval | Notification, list and filter, details, approve, reject, return; second approval of risky changes | BRD 2.002; NFR p.17 |
| Group profiles | Requests to create, modify, deactivate and reactivate group profiles with approvers in order; implementation by the System Administrator | BRD 3.002; p.6 |
| System Administrator | Group profiles of the UAM personas; functions assigned to profiles; user data maintenance | BRD 4.002; NFR p.13-14 |
| Reports and logs | User Access Report, User Group Profile Report, Group Profile Membership list, User Access Audit Log; structured access-change log | BRD 3.003, 4.003; p.18 |

**Out of scope for this phase:**

- Infrastructure items the BRD answers with "Follow existing QPS set up" (servers, network, DR, environments, support). They follow the BIBS deployment standards (section 8).
- Data and user migration: "N/A" in the BRD (p.12). Existing users can be loaded with the bulk request (FR-UA-019).
- Automatic joiner and leaver feeds from HR. The BRD does not ask for them; bulk requests cover mass changes.
- The final EUA, LDAP / Active Directory, single sign-on and external ACL connections. BIBS builds the seam and keeps local sign-in until BDO supplies the interfaces (UQ04, UQ14; decision D6 of R4).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Business Requirements Document - QPS User Access Maintenance Module, 39 pages (pp.1-19 text; pp.20-38 the signed scanned copy; p.39 e-mail sign-off) | v1, 15-Apr-2025; signed April-May 2025 | `docs/source-documents/User Access Maintenance.pdf` |
| R2 | BDOI User Access Maintenance (BRD-11) requirements baseline and fit/gap | current | `docs/requirements/BDOI_UAM_BRD_SPEC.md` |
| R3 | User Access Maintenance build design | current (proposal for review) | `docs/architecture/USER_ACCESS_DESIGN.md` |
| R4 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R5 | Built access functions: users, roles and sign-in (`security`); access requests, User Access Matrix, retention (`nbadmin`); screens | as built | `backend/.../security`, `backend/.../nbadmin`, `frontend/src/features/nbadmin`, `frontend/src/features/admin` |
| R6 | FRS BRD-3 Product Maintenance (role-permission change requests, PMADD05) | v1.0 | `docs/deliverables/out/FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.pdf` |
| R7 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |

Page references in this document ("p.8") are pages of the BRD PDF (R1). The BRD has no NFR IDs; the NFR rows carry the analyst's IDs UAM-NFR-01 to UAM-NFR-41 of R2.

## Definitions and acronyms

```glossary
Access request: A request to create, change, deactivate or reactivate a user, or to change a group profile, decided under four eyes; numbered AR-yyyy-nnnnnn
Approver: The user who reviews and approves, rejects or returns access requests (BRD section B)
BRD: Business Requirements Document
Business Administrator: The BDOI user who defines and manages group profiles and raises group-profile requests (BRD section C)
EUA: BDO end-user authentication service that validates the Windows ID and password (NFR p.14)
FR: Functional requirement of this document (FR-UA-nnn)
Group profile: A set of permissions assigned to a group of users (BRD p.4); a role in BIBS
LDAP / AD: Lightweight Directory Access Protocol / Active Directory
Permission: One function or capability in BIBS, with an area (module) and an action class (VIEW, CREATE, AMEND, APPROVE)
QPS: Quotation and Pre-processing System, the legacy system the BRD was written for
Requestor: The user who submits user access requests (BRD section A)
SSO: Single sign-on
System Administrator: The IT user who implements group profiles and maintains users and roles (BRD section D)
UAM-NFR-nn: ID given by the analyst to a non-functional requirement of the BRD (R2 section 7)
UAT: User acceptance testing
UQnn: Open question on BRD-11 raised by the project team (section 10.3)
User Access: The permissions and privileges granted to a user (BRD p.4)
User Access Matrix: The screen and export that show which roles hold which permissions, by permission and by action class
Windows ID: The user's BDO network log-on name, used for EUA authentication
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the **priority** ("Must have" for the rows the BRD marks "Mandatory: Yes"; rows without a flag are noted), the **fit** class of the baseline (R2), and the **screens** and **API** that implement it. API paths marked "(designed)" are added by the build of BRD-11; the others exist today.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**. Where BIBS already has the function, the description starts with the baseline and then states the change.
- **Business rules**. *Configurable* rules are maintained by the System Administrator in BIBS (parameter or list of values, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. Codes quoted exist in the built system. For a check added by BRD-11 the code column shows "To be assigned at build". A "-" marks a screen check (for example a blank mandatory field), which has no business code.
- **Screens and fields**, **notifications**, **audit** and numbered **acceptance criteria**, which are the basis of the BRD-11 test plan.

The BRD lists 160 requirement lines, many of them sub-steps of one function ("create request", "add remarks", "save remarks"). The FRs group them by function; section 11 lists every line with its FR.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R2)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today in BIBS |
| CONFIGURE | Needs set-up only (roles, parameters, lists) |
| CHANGE | Extends a built function |
| NEW | A function that does not exist in BIBS before BRD-11 |

The 160 requirement lines split into 67 FIT, 4 CONFIGURE, 61 CHANGE and 28 NEW; the 41 NFR rows into 13 FIT, 8 CONFIGURE, 12 CHANGE and 8 NEW (R2, section 3).

# Business context and process overview

## Business context

Today BDOI coordinates every QPS user access request with IT, which causes delays (p.5). The BRD asks for a module in which the business unit manages user permissions, roles and access levels itself, securely and with self-service requests, and changes roles simply. It assumes that a User Access Matrix is defined (p.5).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.5-6)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Access requests are sent to IT and coordinated by the business unit | The Requestor raises the request in BIBS, as a draft or submitted, alone or in bulk |
| 2 | The approver is reached by e-mail | The Requestor chooses the approver; BIBS notifies the approver and lists the request under "Assigned to me" |
| 3 | IT grants the access | BIBS grants the access on approval, or on the effective date |
| 4 | Group profiles are created or changed by IT on request | The Business Administrator requests the change, the approvers approve in order, and the System Administrator implements it in BIBS |
| 5 | Access lists are compiled by hand | Four reports and a structured audit log show who has which access, since when and who changed it |

## Process overview

The BRD shows two processes (p.6). The table lists the steps and Figure 1 shows them with the BIBS additions (risk rules, effective date, reports).

<!-- table: widths=0.8,3.8,3.6,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Draft | Requestor | Creates a request to enrol, modify, deactivate or reactivate a user, or a bulk file; enters data, group profiles, effective date and remarks; chooses the approver; saves, edits or cancels the draft | 1.002-1.005, 1.009 |
| 2 | Submit | Requestor | BIBS checks the request; status PENDING; the chosen approver is notified | 1.00x.4, 2.002.1 |
| 3 | Risk check | System | A change to a high-privilege profile, or a submission outside working hours, needs a second approval and raises an alert | NFR 11 (p.17) |
| 4 | Decide | Approver | Approves, rejects with a reason, or returns with remarks | 2.002.5-2.002.7 |
| 5 | Correct | Requestor | Corrects the returned request and re-submits it, or cancels it | 1.006, 1.007 |
| 6 | Apply | System | Applies the change on approval or on the effective date; logs from / to values; notifies the requester and the affected user | p.6; 1.008.1.4; NFR 10 |
| 7 | Group profile | Business Administrator, Approvers, System Administrator | Requests a new, changed, deactivated or reactivated profile; approvers decide in order; the System Administrator implements the approved request | 3.002; p.6 |
| 8 | Reports and logs | Business Administrator, System Administrator, Auditor | User access, group profile, membership and audit log reports | 3.003, 4.003 |

![User access request and group-profile request by actor (BRD p.6)](figures/brd11_process_flow.dot){width=14}

<!-- pagebreak -->

## Baseline: access functions built today

The table lists what BIBS does today and what BRD-11 changes. The FRs refer to it as "the baseline".

<!-- table: widths=3.4,6.6,6.6 caption="Built access functions and the BRD-11 change" size=8.5 -->
| Function | Built today (screen, permission) | BRD-11 change |
|---|---|---|
| Sign-in and lock-out | Login screen; user name and password; the account locks after LOGIN_MAX_FAILED_ATTEMPTS = 3 failed attempts until an administrator unlocks it; every success and failure is in the audit trail | Directory authentication with the Windows ID (parked seam); log-out and session log |
| Session policy | Inactivity warning after SESSION_IDLE_WARNING_MINUTES = 15; sign-out after SESSION_TIMEOUT_MINUTES = 30 of inactivity; warning SESSION_EXPIRY_WARNING_MINUTES = 30 before the fixed token expiry | None; confirmed by the BRD (Q42) |
| Users | Administration > Users (USER_MANAGE): user name, full name, e-mail, roles, authorisation limit, last login, status; unlock and reset password | Windows ID, business unit group, user level, status incl. online; direct edits limited to requests |
| Roles | Administration > Roles & Permissions (ROLE_MANAGE): create a role, name it, tick permissions | Active flag, description, privilege level; changes only by implementing an approved group-profile request |
| Access requests | Broking Setup > Access Requests (ACCESS_REQUEST to raise, ACCESS_APPROVE to decide): types Create user, Change roles, Disable user, Enable user, Change role permissions; submitted directly; status PENDING, APPROVED, REJECTED; the approver cannot be the requester; approval applies the change at once and shows a temporary password once | Drafts, return, correction, cancellation, chosen approver(s), new types (modify user, group-profile create / deactivate / reactivate), effective date, bulk, second approval, history, implementation step |
| Notifications | All holders of ACCESS_APPROVE on submission; the requester on the decision | The chosen approver only; returned, cancelled, second approval, implementation; the affected user |
| User Access Matrix | Broking Setup > User Access Matrix: roles by permission and by area / action class, Excel export | Every permission gets an area, so the group-profile report shows a module for every task |
| Password | My Profile > Change password: at least 10 characters with upper and lower case, digit and symbol; administrator reset | Password history, maximum and minimum age, forced change after reset, self-service reset |
| Audit | Insert-only audit trail; Administration > Audit Trail report (CTL-AUDIT) with summary text | Structured, insert-only access-change log with from / to values; four reports |

# Personas and roles

## Personas

<!-- table: widths=3,4.6,6.8,3.2 caption="Personas and BIBS roles (design R3, section 6.2)" -->
| Persona | BIBS role | Responsibilities in User Access Maintenance | BRD |
|---|---|---|---|
| Requestor | UAM_REQUESTOR (new) | Raises user access requests: enrol, modify, deactivate, reactivate, bulk; corrects and cancels them; views own requests | Section A (1.001-1.009) |
| Approver | UAM_APPROVER (new); NB_APPROVER keeps its approval right | Reviews and approves, rejects or returns requests assigned to him or her | Section B (2.001-2.002) |
| Second approver | UAM_SECOND_APPROVER (new) | Reviews privileged or out-of-hours changes after the first approval | NFR 11 (p.17) |
| Business Administrator | BUSINESS_ADMIN (exists) | Raises group-profile requests; generates the reports; supports users | Section C (3.001-3.003) |
| System Administrator | SYSADMIN (exists) | Implements approved group-profile requests; defines the UAM group profiles and their functions; unlocks users and resets passwords; audit logs | Section D (4.001-4.003) |
| Auditor | AUDITOR (exists) | Views requests and runs the reports and the audit log | NFR 6 (p.17) |
| System | - | Validates, grants the access, applies dated changes, logs, notifies | p.6 |

The BRD's stakeholders also list ITIO-SRE (System and Database Administrators), ITIO-ES (Storage Administrator) and ITSD-AMS (application support) (p.7). They support the platform and have no functional role in BIBS beyond the System Administrator. Who the 14 Requestors and 8 Approvers are is UQ01.

## Permissions

BRD 4.002.2 asks that each function can be assigned to any group profile. BIBS gives each function its own permission (area USER_ACCESS). ACCESS_REQUEST stays as the umbrella permission of the roles that hold it today, so existing roles keep working.

<!-- table: widths=4.4,2.8,7.4,2.4 caption="User Access Maintenance permissions (area USER_ACCESS)" -->
| Permission | Action class | Allows | BRD |
|---|---|---|---|
| UAM_ENROLL | CREATE | Enrol a new user | 4.002.2.1 |
| UAM_MODIFY | AMEND | Modify an existing user | 4.002.2.2 |
| UAM_DEACTIVATE | AMEND | Deactivate a user | 4.002.2.3 |
| UAM_REACTIVATE | AMEND | Reactivate a user | 4.002.2.4 |
| UAM_CORRECT | AMEND | Apply correction to a returned request | 4.002.2.5 |
| UAM_CANCEL | AMEND | Cancel a request | 4.002.2.6 |
| UAM_VIEW | VIEW | View requests (own; all with ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW) | 4.002.2.7 |
| ACCESS_APPROVE (exists) | APPROVE | Review and approve requests | 4.002.2.8 |
| UAM_GROUP_REQUEST | CREATE | Submit group-profile requests | 4.002.2.9 |
| UAM_REPORT_VIEW | VIEW | Generate the user access reports | 4.002.2.10 |
| UAM_SECOND_APPROVE | APPROVE | Second approval of privileged or out-of-hours changes | NFR 11 |
| ACCESS_REQUEST (exists) | CREATE | Umbrella of the request functions for the roles that hold it today | - |
| USER_MANAGE, ROLE_MANAGE (exist) | AMEND | Users and Roles screens; implementation of group-profile requests | 4.002.1 |

## Permissions matrix

<!-- table: widths=4.4,1.8,1.8,1.8,1.8,1.8,1.8,1.8 caption="Role-to-action matrix for User Access Maintenance (proposal until UQ01 / OQ48)" size=8 -->
| Permission | Requestor | Approver | Second approver | Business Admin | System Admin | NB Approver | Auditor |
|---|---|---|---|---|---|---|---|
| UAM_ENROLL | Y | | | | | | |
| UAM_MODIFY | Y | | | | | | |
| UAM_DEACTIVATE | Y | | | | | | |
| UAM_REACTIVATE | Y | | | | | | |
| UAM_CORRECT | Y | | | Y | | | |
| UAM_CANCEL | Y | | | Y | | | |
| UAM_VIEW | Y | Y | Y | Y | Y | | Y |
| ACCESS_APPROVE | | Y | | | | Y | |
| UAM_SECOND_APPROVE | | | Y | | | | |
| UAM_GROUP_REQUEST | | | | Y | | | |
| UAM_REPORT_VIEW | | Y | | Y | Y | | Y |
| ACCESS_REQUEST | | | | Y | Y | | |
| USER_MANAGE, ROLE_MANAGE | | | | | Y | | |

Segregation of duties is enforced by the system, whatever the roles grant: the requester and the user the request is about never decide it, the second approver differs from the first, and the implementer of a group-profile request is not its requester.

# Functional requirements

## Sign-in, session and password

```fr
id: FR-UA-001
title: Log in with a persona profile from a BDO-issued device
brd: [BRD 1.001.1 (p.8), BRD 1.001.1.1 (p.8), BRD 2.001.1 (p.8), BRD 2.001.1.1 (p.8), BRD 3.001.1 (p.9), BRD 3.001.1.1 (p.9), BRD 4.001.1 (p.9), BRD 4.001.1.1 (p.9), UAM-NFR-18 (p.14), UAM-NFR-19 (p.14)]
actor: Requestor, Approver, Business Administrator, System Administrator
priority: "Must have (4.001.x has no flag in the BRD)"
fit: "FIT; CONFIGURE for the Requestor and Approver profiles (1.001.1.1, 2.001.1.1)"
screens: Login; home page with the menu of the user's roles
api: POST /api/v1/auth/login
description:
  - Baseline. Users sign in to BIBS in a browser with their user ID and password. BIBS checks the credentials and the account status, counts failed attempts and locks the account after LOGIN_MAX_FAILED_ATTEMPTS failed attempts (3, for every BIBS user). Every successful and failed attempt is written to the audit trail. The menu shows only the screens of the user's roles.
  - Change. The Requestor and Approver profiles are delivered as the roles UAM_REQUESTOR and UAM_APPROVER. Access only from BDO-issued devices is enforced by the BDO network and endpoint policy, not by BIBS.
preconditions:
  - "The user has an enabled BIBS account with at least one role."
main_flow:
  - The user opens BIBS from a BDO-issued device and enters the user ID and password.
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the menu of the user's roles.
alternate_flows:
  - Wrong credentials. BIBS refuses the log-in and counts the failed attempt.
  - Third failed attempt. The account locks; the System Administrator unlocks it (Administration > Users, Unlock) or reactivates it through a request.
  - Directory mode (FR-UA-003). The password is checked by EUA instead of BIBS.
rules:
  - [R1, "Lock-out after LOGIN_MAX_FAILED_ATTEMPTS consecutive failures (3); applies to all users (CQ23 answered by this BRD).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R2, "A user may hold several roles; the menu is the union of their screens (NFR 1.c-d).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user name or password, "-"]
  - [Account locked, Account is locked. Contact your administrator., "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded (LOGIN, LOGIN_FAILED) with user and time.
acceptance:
  - A user with the Requestor profile logs in and sees the User Access screens of the Requestor only.
  - The third wrong password locks the account and the fourth attempt is refused with "Account is locked".
  - The audit trail lists the failed and successful attempts.
```

```fr
id: FR-UA-002
title: Warn before the inactivity and system log-out
brd: [BRD 1.001.1.2 (p.8), BRD 1.001.1.3 (p.8), BRD 2.001.1.2 (p.8), BRD 2.001.1.3 (p.8), BRD 3.001.1.2 (p.9), BRD 3.001.1.3 (p.9), BRD 4.001.1.2 (p.9), BRD 4.001.1.3 (p.9), UAM-NFR-34 (p.17)]
actor: All users
priority: "Must have (4.001.x has no flag in the BRD)"
fit: FIT
screens: Every screen (session guard dialog)
api: GET /api/v1/system/session-policy
description:
  - Baseline, confirmed by the BRD. After 15 minutes without activity BIBS shows a warning; after 30 minutes without activity it signs the user out. Activity in any browser tab keeps all tabs signed in. Separately, BIBS warns 30 minutes before the fixed end of the sign-in token ("For your security the system signs you out in n minute(s)...").
  - The BRD reads "warning prior to system triggered log out (30 minutes)" and "inactivity warning (after 15 minutes)" (p.8, p.17). The inactivity sign-out at 30 minutes with the warning at 15 minutes meets both (Q42 answered, R4).
preconditions:
  - "The user is signed in."
main_flow:
  - The user stops working for 15 minutes; BIBS shows the inactivity warning.
  - The user continues; the warning closes and the timer restarts.
alternate_flows:
  - No activity for 30 minutes. BIBS signs the user out and shows the log-in screen; the session ends with reason IDLE_TIMEOUT in the session log (FR-UA-004).
rules:
  - [R1, "Inactivity warning after SESSION_IDLE_WARNING_MINUTES (15).", Configurable, Parameter SESSION_IDLE_WARNING_MINUTES]
  - [R2, "Inactivity sign-out after SESSION_TIMEOUT_MINUTES (30).", Configurable, Parameter SESSION_TIMEOUT_MINUTES]
  - [R3, "Warning SESSION_EXPIRY_WARNING_MINUTES (30) before the fixed token expiry.", Configurable, Parameter SESSION_EXPIRY_WARNING_MINUTES]
validations: []
notifications:
  - "On-screen warnings only."
audit:
  - "The sign-out is recorded in the session log (FR-UA-004)."
acceptance:
  - After 15 minutes without activity the warning appears.
  - After 30 minutes without activity the user is signed out.
```

```fr
id: FR-UA-003
title: Authenticate with the Windows ID against BDO EUA
brd: [UAM-NFR-11 (p.13), UAM-NFR-17 (p.14), UAM-NFR-33 (p.17)]
actor: All users; System
priority: Must have
fit: NEW (seam; adapter parked until UQ04)
screens: Login
api: POST /api/v1/auth/login (unchanged contract); directory authenticator port
description:
  - The BRD asks that the user ID is interfaced with EUA using the Windows ID. BIBS passes the user ID and the entered password; EUA returns whether the log-on succeeded and, on failure, an error message that BIBS shows (p.14). LDAP or Active Directory authentication and single sign-on or LDAP integration are also required (p.13, p.17).
  - BIBS authenticates through a directory port. Parameter AUTH_MODE selects LOCAL (password held in BIBS, today) or DIRECTORY (password checked by EUA; the user is found by the Windows ID). The lock-out, audit and session behaviour are the same in both modes, and BIBS never stores the password in DIRECTORY mode.
  - The EUA protocol, host and messages are not in the BRD (UQ04). BIBS keeps LOCAL mode until BDO supplies them; switching is then configuration (decision D6, R4).
preconditions:
  - "AUTH_MODE = DIRECTORY; the user has a Windows ID in BIBS (FR-UA-052)."
main_flow:
  - The user enters the Windows ID and password.
  - BIBS passes them to EUA.
  - EUA confirms the log-on; BIBS signs the user in with the BIBS roles of the user.
alternate_flows:
  - EUA refuses. BIBS shows the EUA message, counts the failed attempt and applies the lock-out.
  - The Windows ID is not linked to a BIBS user. BIBS refuses the log-in.
  - EUA not reachable. BIBS refuses the log-in with a service message; a local break-glass administrator, if BDO allows one, signs in locally (UQ04).
rules:
  - [R1, "Authentication mode LOCAL or DIRECTORY.", Configurable, Parameter AUTH_MODE]
  - [R2, "Authorisation (roles and permissions) stays in BIBS in both modes; an external ACL is a parked seam (UQ14).", Fixed, "-"]
validations:
  - [EUA refuses the log-on, The message returned by EUA, "-"]
  - [Windows ID not linked to a user, Invalid user name or password, "-"]
notifications:
  - "None."
audit:
  - "As FR-UA-001, with the authentication mode."
acceptance:
  - In DIRECTORY mode, a user signs in with the Windows ID and the network password, and BIBS stores no password.
  - A wrong password shows the EUA message and counts towards the lock-out.
```

```fr
id: FR-UA-004
title: Log out and keep a session log
brd: [UAM-NFR-35 (p.17), UAM-NFR-15 (p.14)]
actor: All users; System Administrator
priority: Must have
fit: CHANGE
screens: User menu (Log Out); Administration > Users (status Online)
api: POST /api/v1/auth/logout (designed)
description:
  - Baseline. Log-in and failed log-in are audited. Log-out is not recorded - the web client discards the token.
  - Change. **Log Out** ends the session in BIBS and records LOGOUT in the audit trail. Every session is kept in a session log - user, start, last activity, end and end reason (log-out, inactivity, token expiry, ended by an administrator, account locked). The user list shows a user as Online while a session is open (UQ13).
preconditions:
  - "The user is signed in."
main_flow:
  - The user clicks **Log Out**.
  - BIBS ends the session, records LOGOUT and shows the log-in screen.
alternate_flows:
  - A token of an ended session is used again. BIBS refuses it.
rules:
  - [R1, "Last activity is updated at most every 5 minutes.", Fixed, "-"]
  - [R2, "Single session per device is not enforced; the session log prepares it (UQ09).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "LOGOUT audited; the session log is kept with the audit trail."
acceptance:
  - After Log Out, the audit log report shows the log-out with user and time.
  - The Users screen shows a signed-in user as Online.
```

```fr
id: FR-UA-005
title: Enforce the password policy and self-service reset
brd: [UAM-NFR-31 (p.17), UAM-NFR-36 (p.17), UAM-NFR-37 (p.17)]
actor: All users (local accounts); System Administrator
priority: Must have
fit: "FIT (UAM-NFR-31); CHANGE (UAM-NFR-36); NEW (UAM-NFR-37)"
screens: My Profile (Change password, Details); Login (Forgot password?); Administration > Users (Reset password)
api: POST /api/v1/auth/change-password; POST /api/v1/admin/users/{id}/reset-password; password-reset flow (designed)
description:
  - Baseline. Users change their own password on My Profile; the password has at least 10 characters with upper and lower case letters, a digit and a symbol. The System Administrator resets a password and BIBS shows a temporary password once.
  - Change. BIBS also refuses the last PASSWORD_HISTORY_COUNT passwords, requires a change after PASSWORD_MAX_AGE_DAYS, refuses a second change within PASSWORD_MIN_AGE_DAYS and forces a change at the first log-in after a reset. **Forgot password?** e-mails a single-use link valid for 30 minutes. Users update their own e-mail and mobile number on My Profile (UQ17).
  - These rules apply to LOCAL accounts. With EUA the password belongs to BDO (UQ08).
preconditions:
  - "AUTH_MODE = LOCAL."
main_flow:
  - The user opens My Profile and enters the current and the new password twice.
  - BIBS checks the policy and saves the new password.
alternate_flows:
  - Forgot password. The user enters the user ID on the log-in screen; BIBS e-mails the reset link to the registered address; the user sets a new password.
  - Password expired. At log-in BIBS asks for a new password before opening the home page.
rules:
  - [R1, "Length and complexity - at least 10 characters, upper and lower case, digit, symbol.", Fixed, "-"]
  - [R2, "History 8, maximum age 90 days, minimum age 1 day (values of the Collections NFR, to confirm, UQ08).", Configurable, "Parameters PASSWORD_HISTORY_COUNT, PASSWORD_MAX_AGE_DAYS, PASSWORD_MIN_AGE_DAYS"]
  - [R3, "Reset links are single-use and expire after 30 minutes.", Fixed, "-"]
validations:
  - [Current password wrong, Current password is incorrect, INVALID_PASSWORD]
  - [New password too weak, must contain upper and lower case letters and a digit and a symbol, VALIDATION_FAILED]
  - [Password used before, You used this password recently. Choose another one, To be assigned at build]
  - [Changed too soon, You changed your password less than a day ago, To be assigned at build]
notifications:
  - "PASSWORD_EXPIRY_NOTICE to users whose password expires within 7 days (job, 06:00 daily)."
audit:
  - "Password change and reset recorded (no password value)."
acceptance:
  - A new password equal to one of the last 8 is refused.
  - After an administrator reset, the user must change the password at the next log-in.
  - The reset link works once and not after 30 minutes.
```

## User access requests (Requestor)

```fr
id: FR-UA-010
title: Draft, edit, cancel and submit an access request
brd: [BRD 1.002.1.1 (p.8), BRD 1.002.1.1.4-1.002.1.1.5 (p.8), BRD 1.002.1.2-1.002.1.4 (p.8), BRD 1.003.1.1 (p.8), BRD 1.003.1.1.5-1.003.1.1.6 (p.8), BRD 1.003.1.2-1.003.1.4 (p.8), BRD 1.004.1.1 (p.8), BRD 1.004.1.1.2-1.004.1.1.3 (p.8), BRD 1.004.1.2-1.004.1.4 (p.8), BRD 1.005.1 (p.8), BRD 1.005.1.1.1-1.005.1.1.2 (p.8), BRD 1.005.1.2-1.005.1.4 (p.8)]
actor: Requestor
priority: "Must have (the 'add / save remarks' lines have no flag in the BRD)"
fit: "CHANGE (create, edit, cancel, save remarks); FIT (submit, add remarks)"
screens: User Access > Access Requests; New Request; Edit Request
api: POST /api/v1/nbadmin/access-requests (draft=true, designed); PUT .../access-requests/{id} (designed); POST .../access-requests/{id}/submit (designed)
description:
  - Baseline. The Business Administrator clicks **New Request** on Access Requests, fills the New Access Request dialog (request, user name, full name, e-mail, home branch, roles, justification) and submits it at once. The request gets a number AR-yyyy-nnnnnn and status PENDING. There is no draft.
  - Change. Every request type (enrol, modify, deactivate, reactivate, and the group-profile types of section 4.4) can be saved as a DRAFT. Only its creator sees and edits a draft. The creator cancels a draft (CANCELLED, kept, never deleted) or submits it. Remarks - justification, reason for deactivation or reactivation - are saved with the draft, and every remark is also kept in the request history.
  - On submission BIBS runs the full checks - the user exists or not, group profiles active, user ID format, approver eligible, one open request per user - evaluates the risk rules (FR-UA-034) and moves the request to PENDING.
preconditions:
  - "The user holds the permission of the request type (UAM_ENROLL, UAM_MODIFY, UAM_DEACTIVATE, UAM_REACTIVATE or UAM_GROUP_REQUEST) or ACCESS_REQUEST."
main_flow:
  - The Requestor clicks **New Request** and chooses the type.
  - The Requestor enters the data (FR-UA-011 to FR-UA-014), the effective date, the approver (FR-UA-015) and the remarks.
  - The Requestor clicks **Save Draft**. BIBS saves the draft with light checks (formats only).
  - The Requestor opens the draft later and edits it.
  - The Requestor clicks **Submit**. BIBS runs the full checks and moves the request to PENDING.
alternate_flows:
  - Cancel draft. The Requestor clicks **Cancel Request**, enters the reason; the request becomes CANCELLED.
  - A check fails on submission. BIBS keeps the draft and shows the message.
  - Compatibility. The existing one-step submission (used by the Product Maintenance role-permission requests) still creates and submits in one call.
rules:
  - [R1, "Request numbers are AR-<yyyy>-<nnnnnn>.", Fixed, "-"]
  - [R2, "A draft is visible and editable only by its creator.", Fixed, "-"]
  - [R3, "Only one open request (DRAFT excluded) per user or per group profile.", Fixed, "-"]
  - [R4, "Remarks (justification) are mandatory on submission, up to 1000 characters.", Fixed, "-"]
  - [R5, "Requests are never deleted; a cancelled request stays visible with its reason.", Fixed, "-"]
validations:
  - [Justification blank, Enter the justification, "-"]
  - [Another request for the user is pending, A request for user <user> is already waiting for approval, ACCESS_REQUEST_PENDING]
  - [Edit of a draft by another user, Only the creator can edit this draft, To be assigned at build]
  - [Cancel without reason, Enter the reason for the cancellation, "-"]
fields_screen: New Request (common part)
fields:
  - [Request type, List, "Yes", "Enrol new user, Modify user, Deactivate user, Reactivate user", Fixed after first save]
  - [Effective date, Date, "No", "-", "Today or later; blank = on approval (FR-UA-020)"]
  - [Approver, List, "Yes", Eligible approvers (FR-UA-015), Required on submission]
  - [Remarks (justification), Long text, "Yes", "-", Up to 1000 characters]
notifications:
  - "On submission, the chosen approver is notified (FR-UA-070)."
audit:
  - "Save, submit and cancel are recorded in the request history with remarks, user and time, and in the audit trail."
acceptance:
  - A Requestor saves an enrolment request as a draft, edits it the next day and submits it; it becomes PENDING with the same number.
  - A cancelled draft stays in My Requests with status CANCELLED and its reason.
  - Another Requestor cannot open the draft.
```

```fr
id: FR-UA-011
title: Enrol a new user
brd: [BRD 1.002.1 (p.8), BRD 1.002.1.1.1 (p.8), BRD 1.002.1.1.2 (p.8), UAM-NFR-13 (p.13)]
actor: Requestor
priority: Must have
fit: "FIT (1.002.1, 1.002.1.1.2); CHANGE (1.002.1.1.1)"
screens: New Request (type Enrol new user)
api: POST /api/v1/nbadmin/access-requests (type CREATE_USER)
description:
  - Baseline. A Create user request carries the user name, full name, e-mail, home branch and roles. On approval BIBS creates the user and shows a temporary password once to the approver.
  - Change. The request also carries the Windows ID, the business unit group, the user level and an optional effective date (NFR p.13-14). The user ID follows the BDOI format (at least 10 alphanumeric characters, format a999999999, parameter USER_ID_PATTERN). Only active group profiles are offered.
preconditions:
  - "The user has UAM_ENROLL (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Enrol new user**.
  - The Requestor enters the new user's data and selects the group profiles.
  - The Requestor completes the common part (FR-UA-010) and submits.
alternate_flows:
  - In DIRECTORY mode (FR-UA-003) no temporary password is created; the user signs in with the Windows ID.
rules:
  - [R1, "User ID pattern (default ^[a-zA-Z][0-9]{9}$, to confirm UQ05); existing service and demo users are exempt.", Configurable, Parameter USER_ID_PATTERN]
  - [R2, "Windows ID is unique across users.", Fixed, "-"]
  - [R3, "At least one active group profile.", Fixed, "-"]
validations:
  - [User ID with wrong characters or length, "The user name has 3 to 50 letters, digits, dots, dashes or underscores", ACCESS_USERNAME]
  - [User ID not in the BDOI format, "The user ID must follow the format <pattern>", To be assigned at build]
  - [User already exists, User <user> already exists, ACCESS_USER_EXISTS]
  - [Full name blank, Enter the full name of the new user, ACCESS_FULL_NAME]
  - [No group profile, Select at least one role, ACCESS_ROLES]
  - [Unknown group profile, "Unknown role(s): <codes>", ACCESS_UNKNOWN_ROLE]
  - [Inactive group profile, "Group profile <code> is not active", To be assigned at build]
  - [Windows ID already used, "Windows ID <id> belongs to another user", To be assigned at build]
fields_screen: New Request (Enrol new user)
fields:
  - [User ID, Text, "Yes", "-", "USER_ID_PATTERN; unique"]
  - [Windows ID, Text, Conditional, "-", "Unique; required in DIRECTORY mode"]
  - [Full name, Text, "Yes", "-", Up to 120 characters]
  - [E-mail, Text, "No", "-", Valid e-mail; up to 120 characters]
  - [Home branch, List, "No", Branches, "-"]
  - [Business unit group, List, "No", LOV UAM_BUSINESS_UNIT, Values from BDOI (UQ05)]
  - [User level, List, "No", LOV UAM_USER_LEVEL, Values from BDOI (UQ05)]
  - [Group profiles, Multi-select, "Yes", Active roles, At least one]
notifications:
  - "As FR-UA-010; on application the new user receives the access notice (FR-UA-070)."
audit:
  - "On application, the change log records every attribute of the new user with the request number (FR-UA-064)."
acceptance:
  - An enrolment for user ID a013000196 with the Marketing Account Officer profile is applied after approval and the user can sign in.
  - An enrolment for an existing user ID is refused with ACCESS_USER_EXISTS.
  - An inactive group profile is not offered.
```

```fr
id: FR-UA-012
title: Modify an existing user
brd: [BRD 1.003.1 (p.8), BRD 1.003.1.1.1 (p.8), BRD 1.003.1.1.2 (p.8), BRD 1.003.1.1.3 (p.8)]
actor: Requestor
priority: "Must have (1.003.1.1.2 has no flag in the BRD)"
fit: "CHANGE (1.003.1, 1.003.1.1.2); FIT (1.003.1.1.1, 1.003.1.1.3)"
screens: New Request (type Modify user)
api: POST /api/v1/nbadmin/access-requests (type MODIFY_USER, designed); GET /api/v1/nbadmin/users
description:
  - Baseline. A Change roles request replaces the roles of an existing user. The user's name, e-mail and branch change only by a direct edit of the System Administrator on the Users screen.
  - Change. A Modify user request changes the user data (full name, e-mail, home branch, business unit group, user level, Windows ID) and / or the group profiles in one request. The Requestor searches the user by user ID, Windows ID or name; the form shows the current values next to the new ones. Change roles stays available for compatibility.
preconditions:
  - "The user has UAM_MODIFY (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Modify user** and searches the user.
  - BIBS loads the current data and group profiles.
  - The Requestor changes the data and / or the group profiles.
  - The Requestor completes the common part and submits.
rules:
  - [R1, "Only the changed attributes are applied; each is logged with from and to values.", Fixed, "-"]
  - [R2, "A user never requests a change of his or her own roles.", Fixed, "-"]
validations:
  - [User not found, User <user> does not exist, ACCESS_UNKNOWN_USER]
  - [Nothing changed, The request does not change the user, To be assigned at build]
  - [Own roles, You cannot change your own roles, SELF_ROLE_CHANGE]
fields_screen: New Request (Modify user)
fields:
  - [User, Look-up, "Yes", "Users (user ID, Windows ID, name)", Existing user]
  - [Current / new values, Pairs, "No", "Full name, e-mail, home branch, business unit group, user level, Windows ID", At least one change]
  - [Group profiles, Multi-select, "No", Active roles, "-"]
notifications:
  - "As FR-UA-010; the affected user is notified on application (FR-UA-070)."
audit:
  - "Change log with from and to values per attribute (FR-UA-064)."
acceptance:
  - A request that moves a user from Marketing Team Lead to Processing Team Lead is applied after approval, and the audit log report shows "Marketing Team Lead" to "Processing Team Lead".
  - A request that changes the e-mail only leaves the group profiles unchanged.
```

```fr
id: FR-UA-013
title: Deactivate a user
brd: [BRD 1.004.1 (p.8), BRD 1.004.1.1.1 (p.8)]
actor: Requestor
priority: Must have
fit: FIT
screens: New Request (type Deactivate user)
api: POST /api/v1/nbadmin/access-requests (type DISABLE_USER)
description:
  - Baseline. A Disable user request disables the account on approval; the user can no longer sign in.
  - Change. Only enabled users are offered. An optional reason code (list UAM_DEACTIVATION_REASON) is kept with the remarks, and an effective date allows a future deactivation, for example on the last working day.
preconditions:
  - "The user has UAM_DEACTIVATE (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Deactivate user** and searches the user.
  - The Requestor selects the reason and enters the remarks.
  - The Requestor completes the common part and submits.
rules:
  - [R1, "A deactivated user keeps the group profiles so a reactivation restores them.", Fixed, "-"]
  - [R2, "Reasons - Resigned, Transferred, Long leave, Security, Others (to confirm).", Configurable, LOV UAM_DEACTIVATION_REASON]
validations:
  - [User not found, User <user> does not exist, ACCESS_UNKNOWN_USER]
  - [User already disabled, "User <user> is already deactivated", To be assigned at build]
fields_screen: New Request (Deactivate user)
fields:
  - [User, Look-up, "Yes", Enabled users, "-"]
  - [Reason, List, "No", LOV UAM_DEACTIVATION_REASON, "-"]
  - [Remarks, Long text, "Yes", "-", Up to 1000 characters]
notifications:
  - "As FR-UA-010."
audit:
  - "Change log DISABLE_USER with the request number."
acceptance:
  - After approval the user cannot sign in, and the User Access Report shows the deactivation with the approver.
  - A deactivation dated the 30th is applied on the 30th (FR-UA-020).
```

```fr
id: FR-UA-014
title: Reactivate a user
brd: [BRD 1.005 (p.8), BRD 1.005.1.1 (p.8)]
actor: Requestor
priority: Must have
fit: FIT
screens: New Request (type Reactivate user)
api: POST /api/v1/nbadmin/access-requests (type ENABLE_USER)
description:
  - Baseline. An Enable user request enables a disabled account on approval.
  - Change. Only disabled or locked users are offered. The request can also unlock a locked account. The BRD numbers this function one level up (BRD 1.005 and 1.005.1.1 for the sub-steps); the traceability keeps the printed IDs.
preconditions:
  - "The user has UAM_REACTIVATE (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Reactivate user** and searches the user.
  - The Requestor enters the reason for reactivation.
  - The Requestor completes the common part and submits.
rules:
  - [R1, "The user's group profiles are restored as they were; changed profiles need a Modify user request.", Fixed, "-"]
validations:
  - [User not found, User <user> does not exist, ACCESS_UNKNOWN_USER]
  - [User already active, "User <user> is already active", To be assigned at build]
fields_screen: New Request (Reactivate user)
fields:
  - [User, Look-up, "Yes", Disabled or locked users, "-"]
  - [Unlock account, Check box, "No", "-", Shown when the account is locked]
  - [Remarks, Long text, "Yes", "-", Up to 1000 characters]
notifications:
  - "As FR-UA-010."
audit:
  - "Change log ENABLE_USER (and UNLOCK) with the request number."
acceptance:
  - After approval the reactivated user signs in with the group profiles held before deactivation.
```

```fr
id: FR-UA-015
title: Choose the approver
brd: [BRD 1.002.1.1.3 (p.8), BRD 1.003.1.1.4 (p.8), BRD 1.004.1.1.4 (p.8), BRD 1.005.1.1.3 (p.8)]
actor: Requestor
priority: Must have
fit: NEW
screens: New Request (Approver)
api: GET /api/v1/nbadmin/approvers?type= (designed)
description:
  - Baseline. A submitted request is notified to every holder of ACCESS_APPROVE, and any of them can decide it.
  - Change. The Requestor selects the approver from a drop-down of eligible approvers - enabled holders of ACCESS_APPROVE, excluding the requester and the user the request is about. The notice goes to that approver, and the request appears in that approver's My Approvals and "Assigned to me". Other approvers see it only when UAM_ANY_APPROVER is true.
preconditions:
  - "A request is being drafted (FR-UA-010)."
main_flow:
  - The Requestor opens the Approver drop-down.
  - BIBS lists the eligible approvers.
  - The Requestor selects one and submits.
alternate_flows:
  - The chosen approver is disabled before deciding. The Requestor chooses another approver (re-submit), or any approver decides when UAM_ANY_APPROVER is true.
rules:
  - [R1, "The requester and the subject user are never eligible.", Fixed, "-"]
  - [R2, "Who may be chosen (any holder of the approval right, or the approver of the requester's unit) is UQ02.", Configurable, Parameter UAM_ANY_APPROVER]
validations:
  - [Approver not selected, Select the approver, "-"]
  - [Approver not eligible, "<user> cannot approve this request", To be assigned at build]
notifications:
  - "UAM_REQUEST_TO_APPROVE to the chosen approver."
audit:
  - "Chosen approver kept on the request and in the history."
acceptance:
  - The drop-down does not list the requester or the user the request is about.
  - Only the chosen approver receives the notification and sees the request under "Assigned to me".
```

```fr
id: FR-UA-016
title: Apply correction on a returned request
brd: [BRD 1.006.1 (p.8), BRD 1.006.1.1 (p.8), BRD 1.006.1.2 (p.8), BRD 1.006.1.3 (p.8), BRD 1.006.1.4 (p.8), BRD 1.006.1.5 (p.8)]
actor: Requestor (UAM_CORRECT)
priority: Must have
fit: CHANGE
screens: Access Requests (My Requests); Edit Request
api: PUT /api/v1/nbadmin/access-requests/{id}; POST .../{id}/submit (designed)
description:
  - Baseline. There is no return - the approver can only approve or reject.
  - Change. A request returned by the approver (FR-UA-033) is RETURNED. The Requestor receives a notification with the return remarks, edits the data, adds a correction remark and re-submits it to the same approver or a newly chosen one. The history keeps every round.
preconditions:
  - "The request is RETURNED; the user is its creator and has UAM_CORRECT (or ACCESS_REQUEST)."
main_flow:
  - The Requestor opens the notification; the request opens.
  - The Requestor reads the return remarks and edits the data.
  - The Requestor adds the correction remark and clicks **Re-submit**.
  - BIBS runs the checks of FR-UA-010 and moves the request to PENDING.
alternate_flows:
  - The Requestor cancels the returned request instead (FR-UA-017).
rules:
  - [R1, "A correction remark is mandatory on re-submission.", Fixed, "-"]
validations:
  - [Correction remark blank, Enter the correction remarks, "-"]
notifications:
  - "UAM_REQUEST_RETURNED to the requester; UAM_REQUEST_TO_APPROVE to the approver on re-submission."
audit:
  - "RETURN and RESUBMIT events with remarks in the request history."
acceptance:
  - A returned request shows the approver's remarks; after correction and re-submission it is PENDING again.
  - The history shows both rounds with their remarks.
```

```fr
id: FR-UA-017
title: Cancel a submitted request
brd: [BRD 1.007.1 (p.8), BRD 1.007.1.1 (p.8), BRD 1.007.1.2 (p.8), BRD 1.007.1.3 (p.8), BRD 1.007.1.4 (p.8)]
actor: Requestor (UAM_CANCEL)
priority: Must have
fit: "CHANGE; FIT (1.007.1.1 search)"
screens: Access Requests (My Requests)
api: POST /api/v1/nbadmin/access-requests/{id}/cancel (designed)
description:
  - Baseline. Requests are searched by status, type and text (user, role or request number). A pending request cannot be withdrawn.
  - Change. The Requestor searches the request - also by requester, approver and date range - and cancels a PENDING or RETURNED request with a mandatory reason. The request becomes CANCELLED, leaves the approver's My Approvals and the approver is notified. An approved request with a future effective date (SCHEDULED) can be cancelled before its date (UQ06).
preconditions:
  - "The request is PENDING, PENDING_SECOND, RETURNED or SCHEDULED; the user is its creator and has UAM_CANCEL (or ACCESS_REQUEST)."
main_flow:
  - The Requestor searches the request and opens it.
  - The Requestor clicks **Cancel Request** and enters the reason.
  - BIBS cancels the request and notifies the approver.
rules:
  - [R1, "A cancelled request is kept with its reason; it cannot be re-opened.", Fixed, "-"]
validations:
  - [Reason blank, Enter the reason for the cancellation, "-"]
  - [Request already decided, "Request <no.> is already <status>", To be assigned at build]
notifications:
  - "UAM_REQUEST_CANCELLED to the approver."
audit:
  - "CANCEL event with reason, user and time."
acceptance:
  - A pending request cancelled by its creator disappears from the approver's My Approvals and shows CANCELLED with the reason.
  - An approved and applied request cannot be cancelled.
```

```fr
id: FR-UA-018
title: View submitted requests and their status
brd: [BRD 1.008.1 (p.8), BRD 1.008.1.1 (p.8), BRD 1.008.1.2 (p.8), BRD 1.008.1.3 (p.8), BRD 1.008.1.4 (p.8)]
actor: Requestor (UAM_VIEW)
priority: "Must have (1.008.1 has no flag in the BRD)"
fit: FIT
screens: Access Requests (My Requests); request detail (History)
api: GET /api/v1/nbadmin/access-requests; GET .../access-requests/{id}; GET .../{id}/history (designed)
description:
  - Baseline. The Access Requests screen lists requests with number, type, change, requested by, requested on, status and decided by, with filters by status, type and text; a row opens the request detail. The requester is notified when the request is approved or rejected.
  - Change. A Requestor sees his or her own requests on the tab My Requests; holders of ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW see all. The detail adds a History tab with every event and remark, and the new statuses.
preconditions:
  - "The user has UAM_VIEW (or ACCESS_REQUEST / ACCESS_APPROVE)."
main_flow:
  - The Requestor opens Access Requests, tab My Requests.
  - The Requestor filters by status or type.
  - The Requestor opens a request to see its details and history.
rules:
  - [R1, "Without ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW, a user sees only the requests he or she created.", Fixed, "-"]
validations: []
fields_screen: Access Requests (columns)
fields:
  - [Request, Link, "-", "-", AR-yyyy-nnnnnn]
  - [Type, Text, "-", "-", "-"]
  - [Change, Text, "-", "-", Summary of the change]
  - [Requested by / Requested, Text / Date-time, "-", "-", "-"]
  - [Approver, Text, "-", "-", Chosen approver]
  - [Status, Pill, "-", "-", "DRAFT to IMPLEMENTED (section 5)"]
  - [Decided by, Text, "-", "-", "-"]
notifications:
  - "UAM_REQUEST_DECIDED to the requester on approval or rejection (FR-UA-070)."
audit:
  - "None (read only)."
acceptance:
  - A Requestor sees only his or her requests, with the current status.
  - The History tab lists submission, return, re-submission and approval with remarks.
```

```fr
id: FR-UA-019
title: Submit a bulk request
brd: [BRD 1.009.1 (p.8), BRD 1.009.1.1 (p.8), BRD 1.009.1.2 (p.8), BRD 1.009.1.3 (p.8), BRD 1.009.1.4 (p.8), BRD 1.009.1.5 (p.8), BRD 1.009.1.6 (p.8), UAM-NFR-38 (p.17)]
actor: Requestor
priority: "Must have (1.009.1 has no flag in the BRD)"
fit: NEW
screens: User Access > Bulk Request
api: POST /api/v1/nbadmin/access-requests/bulk (multipart, designed); bulk handler UAM_ACCESS_REQUEST
description:
  - The Requestor downloads the template, fills one row per user - action (enrol, modify, deactivate, reactivate), user ID, Windows ID, name, e-mail, branch, business unit group, user level, group profiles, effective date, remarks - and attaches it. BIBS validates every row and shows a validation report before the batch is created.
  - A valid file creates a draft batch with one line request per row. The Requestor adds the batch remarks, edits the draft (replaces the file or removes lines), cancels it or submits it to the chosen approver. On approval every line is applied in its own transaction; failures are listed and the other lines stand.
preconditions:
  - "The user has UAM_ENROLL or UAM_MODIFY (and the permissions of the actions in the file)."
main_flow:
  - The Requestor opens Bulk Request and downloads the template.
  - The Requestor attaches the completed file.
  - BIBS validates each row and shows the report.
  - The Requestor enters the remarks and saves the draft batch.
  - The Requestor chooses the approver and submits the batch.
alternate_flows:
  - Rows with errors. The report lists each row and error; the Requestor corrects the file and attaches it again.
  - A line fails when applied. The line is marked failed with the reason; the batch status shows the counts.
rules:
  - [R1, "Each row is checked as a single request (FR-UA-011 to FR-UA-014).", Fixed, "-"]
  - [R2, "The batch is decided as a whole; line-by-line decisions, maximum rows and mixed actions are UQ10.", Configurable, Bulk settings (UQ10)]
validations:
  - [Row with an unknown action, "Row <n>: action <value> is not valid", To be assigned at build]
  - [Row fails a request check, "Row <n>: <message of the check>", To be assigned at build]
  - [File type not allowed, The file type is not allowed, "-"]
fields_screen: Bulk Request
fields:
  - [File, Attachment, "Yes", Template UAM_ACCESS_REQUEST, "XLSX or CSV"]
  - [Remarks, Long text, "Yes", "-", Up to 1000 characters]
  - [Approver, List, "Yes", Eligible approvers, "-"]
notifications:
  - "As FR-UA-010; the requester receives the result of the batch application."
audit:
  - "Batch and each line in the request history; each applied line in the change log."
acceptance:
  - A file of three enrolments creates a draft batch of three lines; after approval the three users exist.
  - A row with an existing user ID is reported before the batch is created.
```

```fr
id: FR-UA-020
title: Apply changes on an effective date
brd: [UAM-NFR-14 (p.13)]
actor: Requestor; System
priority: Must have
fit: NEW
screens: New Request (Effective date); Access Requests (status SCHEDULED)
api: Job UAM_EFFECTIVE_CHANGES (00:05 daily)
description: A request may carry an effective date. When it is approved before that date, the request becomes SCHEDULED and the daily job applies it on the date, after checking it again. A blank date means the change applies on approval. A scheduled request can be cancelled before its date (FR-UA-017).
preconditions:
  - "The request has an effective date later than the approval date."
main_flow:
  - The approver approves; BIBS sets the request to SCHEDULED.
  - On the effective date the job re-checks and applies the change.
  - BIBS sets the request to APPROVED, logs the change and notifies the requester and the affected user.
alternate_flows:
  - The re-check fails (for example the user was deactivated meanwhile). The request stays SCHEDULED and alert UAM_SCHEDULED_APPLY_FAILED is raised to the System Administrator.
rules:
  - [R1, "Effective date is today or later.", Fixed, "-"]
  - [R2, "An end date for temporary access is not built (UQ06).", Fixed, "-"]
validations:
  - [Effective date in the past, The effective date cannot be before today, "-"]
notifications:
  - "UAM_ACCESS_CHANGED to the affected user on application."
audit:
  - "SCHEDULE and APPLY events; change log on application."
acceptance:
  - A deactivation approved on the 10th with effective date the 15th is applied on the 15th.
  - A failed scheduled application raises UAM_SCHEDULED_APPLY_FAILED.
```

## Review and approval (Approver)

```fr
id: FR-UA-030
title: Receive, list and open requests for approval
brd: [BRD 2.002.1 (p.8), BRD 2.002.2 (p.8), BRD 2.002.2.1 (p.8), BRD 2.002.3 (p.8), BRD 2.002.4 (p.8)]
actor: Approver (ACCESS_APPROVE)
priority: Must have
fit: "FIT (2.002.2, 2.002.3, 2.002.4); CHANGE (2.002.1, 2.002.2.1)"
screens: My Approvals; Access Requests (tab Assigned to Me); request detail
api: GET /api/v1/nbadmin/access-requests; GET .../access-requests/{id}
description:
  - Baseline. Pending requests appear in My Approvals for every holder of ACCESS_APPROVE (never the requester's own) and on the Access Requests screen with status PENDING. A row opens the request detail with the requested change.
  - Change. The approver receives a notification for each request assigned to him or her and filters the list with **Assigned to me**. The detail shows the current values next to the requested ones (user data and group profiles), the members affected (group profiles) and the history.
preconditions:
  - "The user has ACCESS_APPROVE."
main_flow:
  - The approver opens the notification, My Approvals or the tab Assigned to Me.
  - The approver selects a request.
  - BIBS shows the details, current and requested values, risk flags and history.
rules:
  - [R1, "A request appears for its chosen approver; for every approver only when UAM_ANY_APPROVER = true.", Configurable, Parameter UAM_ANY_APPROVER]
validations: []
notifications:
  - "UAM_REQUEST_TO_APPROVE on submission or re-submission."
audit:
  - "None (read only)."
acceptance:
  - The chosen approver receives a notification and finds the request under Assigned to Me.
  - The detail shows "Marketing Team Lead" as current and "Processing Team Lead" as requested for a role change.
```

```fr
id: FR-UA-031
title: Approve a request and grant the access
brd: [BRD 2.002.5 (p.8); p.6 "System to grant the access"]
actor: Approver; System
priority: Must have
fit: FIT
screens: Request detail (Approve and Apply)
api: POST /api/v1/nbadmin/access-requests/{id}/approve
description:
  - Baseline. **Approve and Apply** applies the change at once - creates the user, replaces the roles, disables or enables the user, or changes the role permissions - records the decision and notifies the requester. For a new user BIBS shows a temporary password once; it is never stored in clear. The approver can never be the requester.
  - Change. The subject user can never approve either. After approval, a request with a risk flag goes to the second approver (FR-UA-034); a request with a future effective date becomes SCHEDULED (FR-UA-020); a group-profile request goes to the next approver or to the System Administrator (FR-UA-044, FR-UA-045). Every applied attribute is logged with from and to values (FR-UA-064), and the affected user is notified.
preconditions:
  - "The request is PENDING and assigned to the user (or UAM_ANY_APPROVER); the user has ACCESS_APPROVE and is neither the requester nor the subject user."
main_flow:
  - The approver reviews the request and enters an optional comment.
  - The approver clicks **Approve and Apply**.
  - BIBS applies the change and records the decision.
  - BIBS notifies the requester and the affected user.
rules:
  - [R1, "Four eyes - the requester and the subject user never decide.", Fixed, "-"]
  - [R2, "The temporary password of a new user is shown once to the approver (LOCAL mode).", Fixed, "-"]
  - [R3, "The applied change is checked again at approval (the user or role may have changed since submission).", Fixed, "-"]
validations:
  - [Approver is the requester, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
  - [Approver is the subject user, You cannot decide a request about your own access, To be assigned at build]
notifications:
  - "UAM_REQUEST_DECIDED to the requester; UAM_ACCESS_CHANGED to the affected user."
audit:
  - "AUTHORIZE in the audit trail ('Approved and applied - <change>'); APPROVE and APPLY in the history; change log rows."
acceptance:
  - An approved enrolment creates the user and shows the temporary password once.
  - The requester cannot approve his or her own request (ACCESS_FOUR_EYES).
```

```fr
id: FR-UA-032
title: Reject a request
brd: [BRD 2.002.6 (p.8), BRD 2.002.6.1 (p.8), BRD 2.002.6.2 (p.9)]
actor: Approver
priority: "Must have (2.002.6.1-6.2 have no flag in the BRD)"
fit: FIT
screens: Request detail (Reject)
api: POST /api/v1/nbadmin/access-requests/{id}/reject
description: Baseline, unchanged. The approver enters the rejection reason in Comment ("Mandatory to reject; sent to the requester") and clicks **Reject**. BIBS records the decision, notifies the requester and changes nothing. The reason is also written to the request history.
preconditions:
  - "As FR-UA-031."
main_flow:
  - The approver enters the reason and clicks **Reject**.
  - BIBS sets the request to REJECTED and notifies the requester.
rules:
  - [R1, "A rejected request is final; the requester raises a new request.", Fixed, "-"]
validations:
  - [Reason blank, Enter the reason of the rejection, ACCESS_REJECT_REASON]
  - [Approver is the requester, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
notifications:
  - "UAM_REQUEST_DECIDED to the requester with the reason."
audit:
  - "REJECT in the audit trail and the history with the reason."
acceptance:
  - A rejection without a reason is refused with ACCESS_REJECT_REASON.
  - The requester sees the reason on the rejected request.
```

```fr
id: FR-UA-033
title: Return a request to the Requestor
brd: [BRD 2.002.7 (p.9), BRD 2.002.7.1 (p.9)]
actor: Approver
priority: Must have
fit: CHANGE
screens: Request detail (Return)
api: POST /api/v1/nbadmin/access-requests/{id}/return (designed)
description:
  - Baseline. There is no return.
  - Change. The approver clicks **Return**, enters the remarks (reason for return) and saves. The request becomes RETURNED, the remarks are saved in the history and the requester is notified to correct it (FR-UA-016). The BRD uses ID 2.002.7.1 twice, for "add remarks" and "save remarks".
preconditions:
  - "As FR-UA-031; also PENDING_SECOND for the second approver."
main_flow:
  - The approver enters the remarks and clicks **Return**.
  - BIBS sets the request to RETURNED and notifies the requester.
rules:
  - [R1, "Return remarks are mandatory.", Fixed, "-"]
validations:
  - [Remarks blank, Enter the reason for the return, "-"]
notifications:
  - "UAM_REQUEST_RETURNED to the requester."
audit:
  - "RETURN event with remarks, user and time."
acceptance:
  - A returned request is RETURNED with the approver's remarks and appears in the requester's list for correction.
```

```fr
id: FR-UA-034
title: Escalate privileged and out-of-hours changes for a second approval
brd: [UAM-NFR-40 (p.17)]
actor: System; Second approver (UAM_SECOND_APPROVE)
priority: Must have
fit: NEW
screens: My Approvals; Access Requests (tab Second Approval)
api: POST /api/v1/nbadmin/access-requests/{id}/second-approve (designed); alert UAM_PRIVILEGED_CHANGE
description:
  - The BRD asks that changes to user privileges are monitored, evaluated and escalated for additional review, for example modifications outside working hours or from low to high privilege (p.17).
  - Each group profile has a privilege level (LOW, STANDARD, HIGH, ADMIN). On submission and on approval, BIBS flags a request that raises a user to a HIGH or ADMIN profile (PRIVILEGE_INCREASE) or that is submitted or approved outside UAM_WORKING_HOURS (OUTSIDE_HOURS). A flagged request needs a second approval by a holder of UAM_SECOND_APPROVE other than the first approver, and raises alert UAM_PRIVILEGED_CHANGE.
preconditions:
  - "A request has a risk flag and the first approver approved it."
main_flow:
  - BIBS sets the request to PENDING_SECOND and raises the alert.
  - The second approver reviews the flags and the request.
  - The second approver approves; BIBS continues as FR-UA-031.
alternate_flows:
  - The second approver rejects or returns the request (as FR-UA-032, FR-UA-033).
rules:
  - [R1, "Privilege level per group profile; SYSADMIN is ADMIN; the high-privilege profiles are UQ07.", Configurable, Group profile privilege level]
  - [R2, "Working hours 08:00-18:00 Monday to Friday (to confirm, UQ07).", Configurable, Parameter UAM_WORKING_HOURS]
  - [R3, "The second approver differs from the first approver, the requester and the subject user.", Fixed, "-"]
validations:
  - [Second approver is the first approver, The second approval is given by another approver, To be assigned at build]
notifications:
  - "UAM_SECOND_APPROVAL to the holders of UAM_SECOND_APPROVE; alert UAM_PRIVILEGED_CHANGE."
audit:
  - "Risk flags kept on the request; SECOND_APPROVE event."
acceptance:
  - A request that adds the System Administrator profile to a user needs a second approval.
  - A request submitted at 21:00 on a weekday is flagged OUTSIDE_HOURS.
```

## Group-profile requests (Business Administrator)

```fr
id: FR-UA-040
title: Request a new group profile
brd: [BRD 3.002.1 (p.9), BRD 3.002.1.1 (p.9), BRD 3.002.1.3 (p.9), BRD 3.002.1.4 (p.9), BRD 3.002.1.5 (p.9), BRD 3.002.1.6 (p.9), BRD 3.002.1.7 (p.9)]
actor: Business Administrator (UAM_GROUP_REQUEST)
priority: "Must have (3.002.1.6-1.7 have no flag in the BRD)"
fit: "CHANGE (3.002.1, 3.002.1.1, 1.3, 1.4, 1.7); FIT (3.002.1.5, 1.6)"
screens: User Access > Group Profile Requests (New); permission picker by area and action
api: POST /api/v1/nbadmin/access-requests (type CREATE_ROLE, designed)
description:
  - Baseline. The System Administrator creates roles directly on Roles & Permissions. Only a change of the permissions of an existing role goes through a request (Change role permissions, BRD-3 PMADD05, R6).
  - Change. The Business Administrator requests a new group profile - code, name, description, privilege level and the permissions, picked by area and action class. The request follows the lifecycle of FR-UA-010 (draft, edit, cancel, submit, remarks) and the approvers of FR-UA-044. After the last approval it waits for the System Administrator (FR-UA-045).
preconditions:
  - "The user has UAM_GROUP_REQUEST."
main_flow:
  - The Business Administrator clicks **New Group Profile Request**, type Create.
  - The Business Administrator enters the profile data and picks the permissions.
  - The Business Administrator chooses the approvers, enters the remarks and submits.
rules:
  - [R1, "Profile code - capital letters, digits and underscore, up to 40 characters; unique.", Fixed, "-"]
  - [R2, "At least one permission.", Fixed, "-"]
validations:
  - [Code already used, "Role <code> already exists", DUPLICATE]
  - [No permission, Select at least one permission, To be assigned at build]
  - [Unknown permission, "Unknown permission(s): <codes>", ACCESS_UNKNOWN_PERMISSION]
  - [Too many permissions in one request, Split the change into several requests, ACCESS_PERMISSION_LIST_TOO_LONG]
fields_screen: Group Profile Request (Create)
fields:
  - [Code, Text, "Yes", "-", "A-Z, 0-9, _; up to 40; unique"]
  - [Name, Text, "Yes", "-", Up to 120 characters]
  - [Description, Text, "No", "-", Up to 500 characters]
  - [Privilege level, List, "Yes", "Low, Standard, High, Admin", "-"]
  - [Permissions, Picker, "Yes", Permissions by area and action class, At least one]
  - [Approvers, Ordered list, "Yes", Eligible approvers, At least one (FR-UA-044)]
  - [Remarks, Long text, "Yes", "-", Up to 1000 characters]
notifications:
  - "UAM_REQUEST_TO_APPROVE to the first approver."
audit:
  - "History of the request; change log CREATE_ROLE on implementation."
acceptance:
  - A request for a new profile "Processing Team Lead" with its permissions reaches FOR_IMPLEMENTATION after its approvers approve.
  - A code that already exists is refused.
```

```fr
id: FR-UA-041
title: Request a change to a group profile
brd: [BRD 3.002.2 (p.9), BRD 3.002.2.1 (p.9), BRD 3.002.2.2 (p.9), BRD 3.002.2.4 (p.9), BRD 3.002.2.5 (p.9), BRD 3.002.2.6 (p.9), BRD 3.002.2.7 (p.9), BRD 3.002.2.8 (p.9)]
actor: Business Administrator
priority: "Must have (3.002.2.2, 2.7, 2.8 have no flag in the BRD)"
fit: "FIT (3.002.2, 2.2, 2.6, 2.7); CHANGE (3.002.2.1, 2.4, 2.5, 2.8)"
screens: Group Profile Requests (Modify); Change role permissions fields
api: POST /api/v1/nbadmin/access-requests (type MODIFY_ROLE_PERMISSIONS)
description:
  - Baseline. A Change role permissions request names the role, the permissions to add and to remove and the justification; on approval the difference is applied to the role as it is at approval, so concurrent changes are not lost (R6, FR-PM-003).
  - Change. The Business Administrator searches the profile from the drop-down and may also change its name, description and privilege level. The request gets drafts and approvers in order, and after approval waits for the System Administrator (FR-UA-045) unless UAM_ROLE_APPLY_ON_APPROVAL is true (UQ03).
preconditions:
  - "The user has UAM_GROUP_REQUEST (or ACCESS_REQUEST)."
main_flow:
  - The Business Administrator chooses Modify and selects the profile.
  - The Business Administrator picks the permissions to add and to remove and edits the profile data.
  - The Business Administrator chooses the approvers, enters the remarks and submits.
rules:
  - [R1, "The change applied is the difference between the role at implementation and the request.", Fixed, "-"]
  - [R2, "Implementation step after approval (p.6); switch to apply on approval.", Configurable, Parameter UAM_ROLE_APPLY_ON_APPROVAL (UQ03)]
validations:
  - [No role selected, Select the role to change, ACCESS_ROLE]
  - [Unknown role, "Unknown role(s): <code>", ACCESS_UNKNOWN_ROLE]
  - [Permission both added and removed, "Permission(s) both added and removed: <codes>", ACCESS_PERMISSION_CONFLICT]
  - [No change, "The request does not change the permissions of role <code>", ACCESS_NO_PERMISSION_CHANGE]
  - [Another request for the role pending, A request for role <code> is already waiting for approval, ACCESS_REQUEST_PENDING]
fields_screen: Group Profile Request (Modify)
fields:
  - [Group profile, List, "Yes", Active roles, "-"]
  - [Permissions to add, Multi-select, "No", Permissions, Not already granted]
  - [Permissions to remove, Multi-select, "No", Permissions of the role, Granted to the role]
  - [Name / description / privilege level, Text / List, "No", "-", "-"]
  - [Approvers, Ordered list, "Yes", Eligible approvers, "-"]
  - [Remarks, Long text, "Yes", "-", Up to 1000 characters]
notifications:
  - "As FR-UA-040."
audit:
  - "Change log ROLE_PERMISSIONS with added and removed permissions on implementation."
acceptance:
  - A request that adds one permission and removes another is implemented with exactly that difference.
  - A request that adds and removes the same permission is refused with ACCESS_PERMISSION_CONFLICT.
```

> [!NOTE] Built behaviour differs from the BRD
> Today an approved Change role permissions request is applied by the system at approval (BRD-3 PMADD05). The BRD-11 diagram (p.6) has the System Administrator implement group-profile changes. The build adds the implementation step with UAM_ROLE_APPLY_ON_APPROVAL = false, which also changes the Product Maintenance behaviour; BDOI confirms under UQ03 (PQ17 answered, R4).

```fr
id: FR-UA-042
title: Request the deactivation of a group profile
brd: [BRD 3.002.3 (p.9), BRD 3.002.3.1 (p.9), BRD 3.002.3.2 (p.9), BRD 3.002.3.4 (p.9), BRD 3.002.3.5 (p.9), BRD 3.002.3.6 (p.9), BRD 3.002.3.7 (p.9), BRD 3.002.3.8 (p.9)]
actor: Business Administrator
priority: "Must have (3.002.3.2, 3.7, 3.8 have no flag in the BRD)"
fit: "NEW; FIT (3.002.3.2, 3.7); CHANGE (3.002.3.8)"
screens: Group Profile Requests (Deactivate)
api: POST /api/v1/nbadmin/access-requests (type DEACTIVATE_ROLE, designed)
description:
  - Baseline. Roles have no active flag; a role can only be emptied of permissions.
  - Change. The Business Administrator requests the deactivation of an active profile. The request lists the members of the profile. When implemented, the profile is inactive - it grants nothing, is not offered on requests and keeps its permissions for a reactivation. Whether a profile with active members may be deactivated, or members must be moved first, is UQ16.
preconditions:
  - "The user has UAM_GROUP_REQUEST."
main_flow:
  - The Business Administrator chooses Deactivate and selects an active profile.
  - BIBS shows the members.
  - The Business Administrator chooses the approvers, enters the remarks and submits.
rules:
  - [R1, "An inactive profile grants no permission to its members.", Fixed, "-"]
  - [R2, "SYSADMIN cannot be deactivated.", Fixed, "-"]
validations:
  - [Profile already inactive, "Group profile <code> is already inactive", To be assigned at build]
  - [Profile has members (if UQ16 says block), "Move the <n> members of <code> before deactivating it", To be assigned at build]
notifications:
  - "As FR-UA-040; members are notified on implementation (UAM_ACCESS_CHANGED)."
audit:
  - "Change log DEACTIVATE_ROLE."
acceptance:
  - After implementation, the members of the deactivated profile lose its screens at their next action.
  - The deactivated profile is not offered on new requests.
```

```fr
id: FR-UA-043
title: Request the reactivation of a group profile
brd: [BRD 3.002.4 (p.9), BRD 3.002.4.1 (p.9), BRD 3.002.4.2 (p.9), BRD 3.002.4.4 (p.9), BRD 3.002.4.5 (p.9), BRD 3.002.4.6 (p.9), BRD 3.002.4.7 (p.9), BRD 3.002.4.8 (p.9)]
actor: Business Administrator
priority: "Must have (3.002.4.2, 4.7, 4.8 have no flag in the BRD)"
fit: "NEW; FIT (3.002.4.2, 4.7); CHANGE (3.002.4.8)"
screens: Group Profile Requests (Reactivate)
api: POST /api/v1/nbadmin/access-requests (type REACTIVATE_ROLE, designed)
description: The Business Administrator requests the reactivation of an inactive profile, searched from the drop-down of inactive profiles. When implemented, the profile is active again with its last permissions, and its members regain them.
preconditions:
  - "The user has UAM_GROUP_REQUEST."
main_flow:
  - The Business Administrator chooses Reactivate and selects an inactive profile.
  - The Business Administrator chooses the approvers, enters the remarks and submits.
rules:
  - [R1, "The profile is restored with the permissions it had at deactivation.", Fixed, "-"]
validations:
  - [Profile already active, "Group profile <code> is already active", To be assigned at build]
notifications:
  - "As FR-UA-040."
audit:
  - "Change log REACTIVATE_ROLE."
acceptance:
  - A reactivated profile grants its permissions again to its members.
```

```fr
id: FR-UA-044
title: Route group-profile requests to approvers in order
brd: [BRD 3.002.1.2 (p.9), BRD 3.002.2.3 (p.9), BRD 3.002.3.3 (p.9), BRD 3.002.4.3 (p.9)]
actor: Business Administrator; Approvers
priority: "Not flagged in the BRD"
fit: NEW
screens: Group Profile Requests (Approvers); My Approvals
api: Request approvers (sequence, approver, decision, remarks, time)
description: The BRD lets the Business Administrator select "approver/s" from the drop-down. The request holds one or more approvers in order. Each approver decides in turn; the request moves to the next approver on approval. All must approve. Any approver can reject or return it. User requests have one approver (FR-UA-015).
preconditions:
  - "A group-profile request is being drafted."
main_flow:
  - The Business Administrator adds the approvers in order.
  - On submission the first approver is notified.
  - Each approval notifies the next approver; the last approval moves the request to FOR_IMPLEMENTATION.
alternate_flows:
  - An approver rejects. The request is REJECTED; later approvers are not asked.
  - An approver returns. The request is RETURNED; after re-submission it starts again with the first approver.
rules:
  - [R1, "Approvers in sequence and all required (default until UQ02).", Fixed, "-"]
  - [R2, "An approver appears once; the requester is never an approver.", Fixed, "-"]
validations:
  - [No approver, Add at least one approver, "-"]
  - [Same approver twice, "<user> is already an approver of this request", To be assigned at build]
notifications:
  - "UAM_REQUEST_TO_APPROVE to each approver in turn."
audit:
  - "Each approver's decision, remarks and time on the request."
acceptance:
  - With two approvers, the second is notified only after the first approves.
  - A rejection by the first approver ends the request.
```

```fr
id: FR-UA-045
title: Implement an approved group-profile request
brd: [p.6 "System Administrator to create / modify group profile"; BRD 4.002.1 (p.9)]
actor: System Administrator (ROLE_MANAGE)
priority: Must have
fit: CHANGE
screens: Access Requests (tab For Implementation); Administration > Roles & Permissions (Implement Request)
api: POST /api/v1/nbadmin/access-requests/{id}/implement (designed)
description:
  - Baseline. The System Administrator creates and edits roles freely on Roles & Permissions.
  - Change. Direct role maintenance is limited to implementing approved requests. The For Implementation tab lists approved group-profile requests; **Implement Request** opens the Roles screen with the approved change, and saving it applies the change and sets the request to IMPLEMENTED. Direct role edits are refused unless the audited emergency parameter UAM_DIRECT_ROLE_EDIT is true, which raises alert UAM_DIRECT_ROLE_EDIT.
preconditions:
  - "The request is FOR_IMPLEMENTATION; the user has ROLE_MANAGE and is not the requester."
main_flow:
  - The System Administrator opens the For Implementation tab and selects the request.
  - The System Administrator clicks **Implement Request** and reviews the change.
  - The System Administrator saves; BIBS applies it, logs it and sets the request to IMPLEMENTED.
alternate_flows:
  - Emergency. With UAM_DIRECT_ROLE_EDIT = true the System Administrator edits a role directly; the edit is audited and alerted.
rules:
  - [R1, "The implementer is never the requester.", Fixed, "-"]
  - [R2, "Emergency direct edit off by default.", Configurable, Parameter UAM_DIRECT_ROLE_EDIT]
validations:
  - [Direct role edit without a request, A group profile is changed only by implementing an approved request, To be assigned at build]
  - [Implementer is the requester, A request is implemented by someone other than its requester, To be assigned at build]
notifications:
  - "UAM_FOR_IMPLEMENTATION to the System Administrators; UAM_REQUEST_DECIDED to the requester on implementation."
audit:
  - "IMPLEMENT event; change log of the role."
acceptance:
  - An approved new profile is created only when the System Administrator implements it.
  - A direct role edit is refused while UAM_DIRECT_ROLE_EDIT is false.
```

## System Administrator functions

```fr
id: FR-UA-050
title: Define the group profiles of the access personas
brd: [BRD 4.002.1 (p.9), BRD 4.002.1.1 (p.9), BRD 4.002.1.2 (p.9), BRD 4.002.1.3 (p.9)]
actor: System Administrator
priority: Must have
fit: "FIT (4.002.1, 4.002.1.3); CONFIGURE (4.002.1.1, 4.002.1.2)"
screens: Administration > Roles & Permissions
api: GET /api/v1/admin/roles
description: The build delivers the group profiles of the BRD personas - UAM_REQUESTOR (Requestor), UAM_APPROVER (Approver) and UAM_SECOND_APPROVER - and grants the new permissions to BUSINESS_ADMIN (Business Administrator, exists) and SYSADMIN (exists), as in section 3.3. Later changes to these profiles are group-profile requests (FR-UA-040 to FR-UA-045).
preconditions:
  - "None (delivered with the build)."
main_flow:
  - The System Administrator opens Roles & Permissions.
  - BIBS lists the delivered profiles with their permissions.
rules:
  - [R1, "Roles holding ACCESS_REQUEST today also receive the new request permissions, so existing users keep their functions.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Delivered grants are in the database migration; later changes in the change log."
acceptance:
  - The Requestor, Approver and Business Administrator profiles exist with the permissions of section 3.3.
```

```fr
id: FR-UA-051
title: Assign functions to a group profile
brd: [BRD 4.002.2 (p.9), BRD 4.002.2.1 to 4.002.2.7 (p.9-10), BRD 4.002.2.8 (p.10), BRD 4.002.2.9 (p.10), BRD 4.002.2.10 (p.10), UAM-NFR-32 (p.17)]
actor: System Administrator (through approved group-profile requests)
priority: Must have
fit: "FIT (4.002.2, 4.002.2.8); CHANGE (4.002.2.1-2.7, 2.9, 2.10)"
screens: Roles & Permissions; User Access Matrix
api: Permissions of section 3.2; GET /api/v1/nbadmin/access-matrix, /by-action
description:
  - Baseline. Every BIBS screen, button and API call requires a permission; roles are bundles of permissions; the User Access Matrix shows them by permission and by area / action class and exports to Excel.
  - Change. Each access-maintenance function of BRD 4.002.2 has its own permission (section 3.2), so it can be assigned to any profile. Every permission of BIBS gets an area and action class, so the matrix and the group-profile report show a module for every task. The request endpoints accept the type-specific permission or ACCESS_REQUEST.
preconditions:
  - "An approved group-profile request (FR-UA-045)."
main_flow:
  - The System Administrator implements a request that adds, for example, UAM_CANCEL to a profile.
  - BIBS grants the function to all members of the profile.
rules:
  - [R1, "Functions and permissions - section 3.2.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Change log ROLE_PERMISSIONS."
acceptance:
  - A profile without UAM_CANCEL does not show **Cancel Request**, and a direct call is refused with ACCESS_DENIED.
  - The User Access Matrix shows UAM_ENROLL under area USER_ACCESS, action CREATE.
```

```fr
id: FR-UA-052
title: Maintain user data and the user ID format
brd: [UAM-NFR-09 (p.13), UAM-NFR-13 (p.13), UAM-NFR-15 (p.14), UAM-NFR-16 (p.14)]
actor: System Administrator; Requestor (through requests)
priority: Must have
fit: "FIT (UAM-NFR-09); CHANGE (UAM-NFR-13, 15, 16)"
screens: Administration > Users; Lists of Values (UAM_BUSINESS_UNIT, UAM_USER_LEVEL)
api: GET /api/v1/admin/users; PUT /api/v1/admin/users/{id} (implementation and emergency only)
description:
  - Baseline. Users are listed with user name, full name, roles, authorisation limit, last login and status; the System Administrator unlocks accounts and resets passwords. A user can hold several roles and a role many users; accounts are disabled and re-enabled.
  - Change. The user record adds the Windows ID, business unit group and user level; the status shown is Active, Disabled, Locked or Online. User IDs follow USER_ID_PATTERN. Creating and editing users directly is replaced by "Raise request" for everyone except the System Administrator in an emergency (UAM_DIRECT_ROLE_EDIT). Unlock and password reset stay direct administrator actions and are logged.
preconditions:
  - "The user has USER_MANAGE."
main_flow:
  - The System Administrator opens Users and filters by status, business unit or profile.
  - The System Administrator unlocks an account or resets a password, or raises a request for other changes.
rules:
  - [R1, "Status is derived from the enabled flag, the lock and an open session.", Fixed, "-"]
  - [R2, "Business unit groups and user levels are lists maintained by the Business Administrator (values UQ05).", Configurable, LOV UAM_BUSINESS_UNIT and UAM_USER_LEVEL]
validations:
  - [Own roles changed directly, You cannot change your own roles, SELF_ROLE_CHANGE]
  - [Unknown role, "One or more roles do not exist: <codes>", UNKNOWN_ROLE]
fields_screen: Users (columns)
fields:
  - [User ID / Windows ID, Text, "-", "-", "-"]
  - [Full name, Text, "-", "-", "-"]
  - [Group profiles, Text, "-", "-", "-"]
  - [Business unit group / User level, Text, "-", LOVs, "-"]
  - [Last login, Date-time, "-", "-", "-"]
  - [Status, Pill, "-", "-", "Active, Disabled, Locked, Online"]
notifications:
  - "None."
audit:
  - "Unlock and reset in the change log (UNLOCK, PASSWORD_RESET)."
acceptance:
  - The Users screen shows the Windows ID, business unit, user level and status of each user.
  - An unlock is listed in the audit log report with the administrator.
```

## Reports and logs

```fr
id: FR-UA-060
title: User Access Report
brd: [BRD 3.003.1 (p.9), BRD 3.003.1.1 (p.9), BRD 3.003.1.1 (2nd) (p.9), BRD 3.003.1.2 to 3.003.1.6 (p.9), UAM-NFR-10 (p.13), UAM-NFR-41 (p.18)]
actor: Business Administrator; System Administrator; Auditor (UAM_REPORT_VIEW)
priority: Must have
fit: "NEW (3.003.1.1); CHANGE (3.003.1, 1.5, 1.6); FIT (the other columns)"
screens: User Access > User Access Reports; Reports (category Control & Audit)
api: Report code UAM-USER-ACCESS
description: The report lists users with their group profiles and who created, modified, deactivated or reactivated them, as of a date (sample A, p.18). The "created by" column shows the approver and the request number from the change log, not the session that applied the change. Layout in section 6.
preconditions:
  - "The user has UAM_REPORT_VIEW."
main_flow:
  - The user opens User Access Reports and selects the User Access Report.
  - The user sets the as-of date and filters.
  - BIBS generates the report; the user exports it.
rules:
  - [R1, "As-of date shows the access held at the end of that date, from the change log (meaning to confirm, UQ11).", Fixed, "-"]
validations:
  - [As-of date in the future, The as-of date cannot be in the future, "-"]
notifications:
  - "None."
audit:
  - "Report runs and exports logged."
acceptance:
  - The report shows user a024000000 with profile Processing Officer, created by the approver of its request and modified by the approver of the later change.
  - The report exports to PDF, XLSX and CSV.
```

```fr
id: FR-UA-061
title: User Group Profile Report
brd: [BRD 3.003.2 (p.9), BRD 3.003.2.1 to 3.003.2.6 (p.9), UAM-NFR-41 (p.18)]
actor: Business Administrator; System Administrator; Auditor
priority: Must have
fit: "CHANGE (3.003.2, 2.2, 2.6); FIT (the other columns)"
screens: User Access Reports
api: Report code UAM-GROUP-PROFILE
description: The report lists each group profile with the modules and tasks it can access - module = permission area, task = permission and action class - marked With Access or No Access (sample B, p.18), and the profile's created, modified, deactivated and reactivated dates and actors.
preconditions:
  - "The user has UAM_REPORT_VIEW."
main_flow:
  - The user selects the report, the profile(s), area and active flag.
  - BIBS generates the report; the user exports it.
rules:
  - [R1, "Module and task come from the permission areas and action classes; the BRD sample's QPS task names are UQ11.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Report runs logged."
acceptance:
  - For the Marketing Account Officer profile, the report lists Client Management tasks with "With Access" or "No Access".
```

```fr
id: FR-UA-062
title: Group Profile Membership list
brd: [BRD 3.003.3 (p.9), BRD 3.003.3.1 (p.9), BRD 3.003.3.1.1 (p.9), BRD 3.003.3.1.2 (p.9), UAM-NFR-10 (p.13)]
actor: Business Administrator; System Administrator; Auditor
priority: Must have
fit: "NEW (3.003.3, 3.1); FIT (3.003.3.1.1, 3.1.2)"
screens: User Access Reports
api: Report code UAM-GROUP-MEMBERS
description:
  - Baseline. The User Access Matrix shows only the number of enabled users per role.
  - Change. The report lists the members of each profile - user name and user ID, added by and date, modified by and date - grouped by profile (sample C, p.18, titled "User Group Profile Report" in the BRD).
preconditions:
  - "The user has UAM_REPORT_VIEW."
main_flow:
  - The user selects the report, the profile(s) and the as-of date.
  - BIBS generates the list; the user exports it.
rules: []
validations: []
notifications:
  - "None."
audit:
  - "Report runs logged."
acceptance:
  - The list shows every member of Marketing Team Lead with the user who added each member and the date.
```

```fr
id: FR-UA-063
title: User Access Audit Log
brd: [BRD 4.003.1 (p.10), UAM-NFR-35 (p.17), UAM-NFR-41 (p.18)]
actor: System Administrator; Business Administrator; Auditor
priority: Must have
fit: CHANGE
screens: User Access Reports; Administration > Audit Trail (CTL-AUDIT, exists)
api: Report code UAM-AUDIT-LOG
description:
  - Baseline. The Audit Trail report (CTL-AUDIT) lists audit entries by date range, user and entity type, with summary text, and exports to PDF, Excel and CSV.
  - Change. The User Access Audit Log lists, for a date range, each access activity - date, activity, from, to, done by, approved by, request number (sample D, p.18) - from the access-change log and the request history. Log-ins, failed log-ins and log-outs can be included. CTL-AUDIT stays.
preconditions:
  - "The user has UAM_REPORT_VIEW (or AUDIT_VIEW)."
main_flow:
  - The user selects the report and the date range, user and activity.
  - BIBS generates the log; the user exports it.
rules:
  - [R1, "Activities - request created, approved, rejected, returned, cancelled, implemented; user created, modified, deactivated, reactivated, unlocked; password reset; group profile created, changed, deactivated, reactivated; log-in, failed log-in, log-out.", Fixed, "-"]
validations:
  - [Date to before date from, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Report runs logged."
acceptance:
  - A profile change of User 123 appears as "Modify User Group Profile" from "Marketing Team Lead" to "Processing Team Lead" with the user who did it.
  - Log-outs appear when the option is selected.
```

```fr
id: FR-UA-064
title: Keep a structured, insert-only access-change log
brd: [BRD 4.003.1 (p.10), UAM-NFR-09 (p.13), UAM-NFR-22 (p.14)]
actor: System
priority: Must have
fit: "FIT (UAM-NFR-22); CHANGE (structured log)"
screens: Reports of section 6; request History tab
api: Access-change log (insert-only)
description: Baseline - every change is in the insert-only audit trail as summary text. Change - every applied change also writes one row per attribute into the access-change log (time, user or profile, activity, attribute, from value, to value, request number, done by, approved by) in the same transaction as the change. No user can change or delete it. The reports read it.
preconditions:
  - "None."
main_flow:
  - A change is applied (approval, scheduled application, implementation, unlock, reset).
  - BIBS writes the change and the log rows in one transaction.
rules:
  - [R1, "Insert-only; enforced in the database.", Fixed, "-"]
  - [R2, "Kept at least as long as the audit trail; QPS retention values are UQ12.", Configurable, Retention rules]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - A database update of a log row is refused.
  - Each attribute of a modified user has one row with from and to values and the request number.
```

## Notifications

```fr
id: FR-UA-070
title: Notify requesters, approvers and affected users
brd: [BRD 1.006.1.1 (p.8), BRD 1.008.1.4 (p.8), BRD 2.002.1 (p.8), UAM-NFR-39 (p.17)]
actor: System
priority: Must have
fit: CHANGE
screens: Notifications (bell); e-mail
api: Notification events UAM_REQUEST_TO_APPROVE, UAM_REQUEST_RETURNED, UAM_REQUEST_CANCELLED, UAM_REQUEST_DECIDED, UAM_ACCESS_CHANGED, UAM_SECOND_APPROVAL, UAM_FOR_IMPLEMENTATION
description:
  - Baseline. On submission every holder of ACCESS_APPROVE receives "Access request <no.> to approve"; on the decision the requester receives "Access request <no.> approved / rejected", each with a link to the request.
  - Change. The notice goes to the chosen approver only. The requester is also notified of a return and of the application of a scheduled change; the approver of a cancellation; the second approvers and the System Administrators of the requests waiting for them; and the affected user (in the app and by e-mail) when his or her access changes (NFR 10, p.17).
preconditions:
  - "None."
main_flow:
  - A request event occurs.
  - BIBS notifies the users concerned in the app and, per their preferences, by e-mail.
rules:
  - [R1, "Users choose in-app or e-mail per event type; the affected-user notice is always sent by e-mail too.", Configurable, Notification preferences]
validations: []
notifications:
  - "This FR is the notification set."
audit:
  - "Notifications logged with recipient, type and time."
acceptance:
  - A returned request notifies the requester with the remarks.
  - An applied deactivation notifies the affected user by e-mail.
```

```fr
id: FR-UA-071
title: E-mail designated users on failed batch runs
brd: [UAM-NFR-24 (p.15), UAM-NFR-25 (p.15)]
actor: System; System Administrator (recipient)
priority: Must have
fit: CHANGE
screens: Administration > Jobs (run history); e-mail
api: Alert JOB_FAILURE; parameter JOB_FAILURE_RECIPIENTS
description:
  - Baseline. Batch jobs are scheduled, run on demand from the screen, monitored and re-run; each run is logged with success or failure, and a failed run raises the in-app alert JOB_FAILURE.
  - Change. A failed run is also e-mailed to the designated users in parameter JOB_FAILURE_RECIPIENTS, with the job, time and error.
preconditions:
  - "A batch run fails."
main_flow:
  - The job run fails.
  - BIBS logs the failure, raises JOB_FAILURE and e-mails the recipients.
rules:
  - [R1, "Recipients.", Configurable, Parameter JOB_FAILURE_RECIPIENTS]
validations:
  - [Invalid e-mail in the recipients, "<address> is not a valid e-mail address", "-"]
notifications:
  - "E-mail to the recipients; in-app alert JOB_FAILURE."
audit:
  - "Job run history."
acceptance:
  - A failed UAM_EFFECTIVE_CHANGES run sends an e-mail to the recipients with the error.
```

# Request lifecycle

## User access requests

Figure 2 shows the statuses of a user access request. The grey box (PENDING) and the end statuses APPROVED and REJECTED exist today; the others are added by BRD-11.

![Status of a user access request (BRD 1.002-1.009, 2.002)](figures/brd11_request_states.dot){width=17}

<!-- table: widths=3.2,6.8,6.6 caption="Request statuses" -->
| Status | Meaning | Who acts |
|---|---|---|
| DRAFT | Saved, not submitted; visible to its creator only | Creator (edit, submit, cancel) |
| PENDING | Submitted; waiting for the chosen approver (group profiles - the current approver in order) | Approver; creator may cancel |
| PENDING_SECOND | Approved once; waiting for a second approval because of a risk flag | Second approver |
| RETURNED | Returned with remarks for correction | Creator (correct and re-submit, or cancel) |
| SCHEDULED | Approved; applied by the daily job on the effective date | System; creator or approver may cancel |
| APPROVED | Approved and applied | - |
| REJECTED | Rejected with a reason | - |
| CANCELLED | Cancelled by the creator with a reason | - |
| FOR_IMPLEMENTATION | Group profile - approved; waiting for the System Administrator | System Administrator |
| IMPLEMENTED | Group profile - implemented | - |

<!-- table: widths=3.4,3,3.6,4.2,2.4 caption="Transitions" size=8 -->
| From | Action | To | Who | Remarks |
|---|---|---|---|---|
| (new) | save | DRAFT | Requestor (type permission) | Optional |
| DRAFT | submit | PENDING | Creator | Mandatory |
| DRAFT | cancel | CANCELLED | Creator | Mandatory |
| PENDING | approve | APPROVED, SCHEDULED, PENDING_SECOND, FOR_IMPLEMENTATION or next approver | Chosen approver (ACCESS_APPROVE) | Optional |
| PENDING_SECOND | second approve | APPROVED, SCHEDULED or FOR_IMPLEMENTATION | UAM_SECOND_APPROVE, not the first approver | Optional |
| PENDING, PENDING_SECOND | return | RETURNED | Approver | Mandatory |
| PENDING, PENDING_SECOND | reject | REJECTED | Approver | Mandatory |
| RETURNED | re-submit | PENDING | Creator (UAM_CORRECT) | Mandatory |
| PENDING, RETURNED, SCHEDULED | cancel | CANCELLED | Creator (UAM_CANCEL); approver for SCHEDULED | Mandatory |
| SCHEDULED | apply (job) | APPROVED | System | - |
| FOR_IMPLEMENTATION | implement | IMPLEMENTED | System Administrator (ROLE_MANAGE), not the requester | Optional |

## Group-profile requests

Figure 3 shows a group-profile request with approvers in order and the implementation step.

![Status of a group-profile request (BRD 3.002; p.6)](figures/brd11_group_states.dot){width=15}

# Reports

## Report list

<!-- table: widths=4,4.4,6,2.4 caption="User Access Maintenance reports (category Control & Audit)" -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| UAM-USER-ACCESS | User Access Report | Users, their group profiles and who created and changed them | 3.003.1; sample A |
| UAM-GROUP-PROFILE | User Group Profile Report | Modules and tasks each profile can access | 3.003.2; sample B |
| UAM-GROUP-MEMBERS | Group Profile Membership | Members of each profile | 3.003.3; sample C |
| UAM-AUDIT-LOG | User Access Audit Log | Access activities with from and to values | 4.003.1; sample D |
| UAM-REQUESTS | Access Requests | Requests by status, type, requester, approver and age | 1.008 |

All reports need UAM_REPORT_VIEW, export to PDF, XLSX and CSV, and print in the header the report name, the user who generated it, the date coverage ("as of" or "from / to") and the date and time generated (p.18).

### User Access Report (UAM-USER-ACCESS)

Parameters: As of Date; Business Unit Group; Status; Group Profile. Layout: landscape, sorted by user name.

<!-- table: widths=3.8,3,9.8 caption="UAM-USER-ACCESS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| User Name | Text | Full name |
| User ID / Windows ID | Text | - |
| User Group Profile | Text | Group profiles held on the as-of date |
| Business Unit / User Level | Text | - |
| Status | Text | Active, Disabled, Locked |
| Created by / Date Created | Text / Date | Approver of the enrolment (request number) and date |
| Modified by / Date Modified | Text / Date | Approver of the last change and date |
| Last Action / by | Text | Added, modified, deactivated or reactivated, and by whom |

### User Group Profile Report (UAM-GROUP-PROFILE)

Parameters: Group Profile; Area; Active. Layout: portrait, grouped by profile and module.

<!-- table: widths=3.8,3,9.8 caption="UAM-GROUP-PROFILE columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Group Profile | Text | Profile name (and code) |
| Module Name | Text | Permission area |
| Task Name | Text | Permission and action class |
| Access | Text | With Access / No Access |
| Date Created / Created by | Date / Text | - |
| Date Modified / Modified by | Date / Text | Last change and actor (added, modified, deactivated, reactivated) |

### Group Profile Membership (UAM-GROUP-MEMBERS)

Parameters: Group Profile; As of Date. Layout: portrait, grouped by profile.

<!-- table: widths=3.8,3,9.8 caption="UAM-GROUP-MEMBERS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Group Profile | Text | - |
| Member Username | Text | Full name |
| Member User ID | Text | - |
| Created by / Date Created | Text / Date | Who added the member to the profile, and when |
| Modified by / Date Modified | Text / Date | Last change to the membership |

### User Access Audit Log (UAM-AUDIT-LOG)

Parameters: Date From / To; User; Activity; include log-ins and log-outs (yes / no). Layout: landscape, sorted by date and time.

<!-- table: widths=3.8,3,9.8 caption="UAM-AUDIT-LOG columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Date | Date-time | - |
| Activity | Text | For example "Approved Request to Enroll New User", "Modify User Group Profile of <user>" |
| From | Text | Value before, or "Null" |
| To | Text | Value after |
| Done By | Text | User who did it |
| Approved By / Request No. | Text | Where the activity came from a request |

# Interfaces and integration

Figure 4 shows the interfaces. Every BIBS module reads the effective permissions of the signed-in user; the directory, SSO and external ACL are parked seams.

![Interfaces of User Access Maintenance (dashed = parked)](figures/brd11_integration.dot){width=16}

<!-- table: widths=3.8,1.8,7.6,2.8,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| BDO EUA with Windows ID | Out / In | User ID and password passed at log-in; success or failure with message returned | NFR p.14 | PARKED |
| LDAP / Active Directory | Out / In | Bind authentication, same port | NFR 1.h (p.13) | PARKED |
| SSO (SAML / OIDC) | In | Identity-provider assertion exchanged for a BIBS session | Other BU NFR 4 (p.17) | PARKED |
| External ACL | In | Authorisation by an external access-control list | NFR 1.i (p.13) | PARKED |
| All BIBS modules | Out | Effective permissions of the user (menus, buttons, API checks) | 4.002.2 | BUILT |
| Portal users (BRD-8 Employee Benefits) | Out | External user requests provisioned on approval (decision D7) | D7 (R4) | DESIGNED |
| Notifications and e-mail | Out | Request, access-change and batch-failure notices | 1.006.1.1, 1.008.1.4, 2.002.1; NFR 10 | BUILT |
| Bulk upload | In | Template file of access requests | 1.009 | DESIGNED |
| Remote log server / syslog | Out | Application and error logs | NFR p.14 | PARKED |

> [!PARKED] Parked seams
> The EUA, LDAP / AD and SSO interfaces are required by the BRD but their protocol, host and messages are not given (UQ04). BIBS captures the Windows ID now and authenticates through a port with a LOCAL default, so connecting EUA is a configuration of a new adapter, with no change to users, roles or requests (decision D6, R4).

# Non-functional requirements

The BRD's NFR section has no IDs; R2 numbers the rows UAM-NFR-01 to UAM-NFR-41. Most infrastructure rows are answered "Follow existing QPS set up" in the BRD; BIBS follows its own deployment standards for them. Rows that describe functions are specified as FRs in section 4 and cross-referenced here.

<!-- NFR:START -->
<!-- table: widths=2.1,2.8,4.9,4.0,1.5,2.3 caption="Non-functional requirements (BRD p.11-18)" status=Fit size=7.5 -->
| ID | Topic | BRD requirement | BIBS approach | FR | Fit |
|---|---|---|---|---|---|
| UAM-NFR-01 | Capacity and performance | Requestor 14 users / 5 concurrent (enrol, modify, deactivate, reactivate, view: 10 tpm each; bulk 3 tpm); Approver 8 / 3 (10 tpm); BU Admin 4 / 2 (group profile 2 tpm); System Admin 1 / 1 (10 tpm, group profile 2 tpm); reports BU / System Admin 5 / 3 (10 tpm). Response 10 s, reports 20 s (p.11) | Within the BIBS sizing (145 concurrent users); online p95 under 3 seconds, reports under 20 seconds | - | FIT |
| UAM-NFR-02 | Projected volume | 10 transactions per month (average); login / logout, start-up and event reports 14 users / 5 concurrent, 10 per month, 5% growth, 10 s, 0% error; audit log report 5 / 3, 10 per month, 20% growth, 20 s, 0% error (p.11) | No specific sizing needed | - | FIT |
| UAM-NFR-03 | Data retention (databases, logs) | Follow QPS retention policy (p.11) | Retention rules per record type; access requests, change log and session log kept at least as long as the audit trail; QPS values UQ12 | - | CONFIGURE |
| UAM-NFR-04 | Scalability, availability, reliability, DR, audit and data management, portability, interoperability, maintainability, environments, migration, support | "Follow existing QPS set up" for every item (vertical / horizontal scaling, operating hours, maintenance windows, HA, uptime, RPO / RTO, backup, DR server, delivery models, web / file / microservice integration, API gateway, ETL, core banking, monitoring, load balancer, environments Dev to DR, data and user migration, QA, training, 24/7 support) (p.11-12) | BIBS deployment standards, the same for every BRD; existing users loaded with the bulk request (FR-UA-019) | - | CONFIGURE |
| UAM-NFR-05 | Accessibility and channels | Accessibility options for impaired vision or colour blindness; website and mobile website: "Follow existing QPS set up" (p.12) | BDO UX contrast rules; responsive screens for desktop and mobile browsers | - | FIT |
| UAM-NFR-06 | Regulatory and compliance | MORB (BSP), Circular 808 of 2013, AMLA 2001, Data Privacy Act 2012 (p.12) | Four eyes on every access change, least privilege, insert-only audit, retention | - | FIT |
| UAM-NFR-07 | Hardware, software, file locations, embedded IDs | Server / workstation specifications; binaries and logs in standard locations; embedded application user IDs named e_appshortname_description (e.g. p_appname_sftp) (p.12-13) | Container deployment; service accounts named by the BDO convention (for example p_bibs_db) | - | CONFIGURE |
| UAM-NFR-08 | Network | No impact on branch / ATM operations, no change to network design; ports, devices, bandwidth, latency, interfaces (p.13) | HTTPS only; hosts and ports are configuration | - | CONFIGURE |
| UAM-NFR-09 | User and role management 1.a-f | Online modules to view, add, modify, delete master data incl. users and roles; access via roles; more than one user per role; **a user can have more than one role**; accounts can be disabled and re-enabled; maintenance recorded in the application log (p.13) | Users, roles and permissions; several roles per user and users per role; enable / disable; every change audited (CQ23 answered) | FR-UA-052, 064 | FIT |
| UAM-NFR-10 | User and role management 1.g | Query or report showing the roles assigned to users; exportable (p.13) | Reports UAM-USER-ACCESS and UAM-GROUP-MEMBERS, exportable | FR-UA-060, 062 | CHANGE |
| UAM-NFR-11 | User and role management 1.h | Supports LDAP or Active Directory authentication (p.13) | Directory authentication port; adapter parked until BDO gives the interface (Q42, UQ04) | FR-UA-003 | NEW |
| UAM-NFR-12 | User and role management 1.i | Can support authorisation of users by interfacing with an external ACL (p.13) | Seam only; parked (UQ14) | - | NEW |
| UAM-NFR-13 | User and role management 1.j | User ID of at least ten (10) alphanumeric characters, supports the format a999999999 (p.13) | Parameter USER_ID_PATTERN checked on requests (default a999999999 format, UQ05) | FR-UA-011, 052 | CHANGE |
| UAM-NFR-14 | User and role management 1.k | Ability to specify an effective date for user and role changes (p.13) | Effective date on requests; daily job applies scheduled changes (UQ06) | FR-UA-020 | NEW |
| UAM-NFR-15 | User ID maintenance fields | Windows ID, user name, user status (active, disabled, locked out, online, etc.), Business Unit Group, User Level (p.14) | Windows ID, business unit group, user level; status Active, Disabled, Locked, Online (UQ05, UQ13) | FR-UA-052, 004 | CHANGE |
| UAM-NFR-16 | System Administrator capabilities | Access rights per group per role; user administrator maintenance; group administration; maintain reference tables; workflow set-up; screen update and maintenance (p.14) | Access rights per profile, user and group administration and reference tables exist; approver rules are parameters; screens change by release | FR-UA-052 | CHANGE |
| UAM-NFR-17 | Authentication through EUA | Interface the user ID with EUA using the Windows ID to authenticate access if the user exists in the BDO network, with password validation: the system passes the user ID and password; EUA returns whether the logon succeeded; on failure the error message is forwarded (p.14) | Windows ID sign-in through EUA when AUTH_MODE = DIRECTORY; EUA message shown; lock-out and audit unchanged (UQ04) | FR-UA-003 | NEW |
| UAM-NFR-18 | Login logging | Log all valid and invalid attempts (p.14) | Every valid and invalid log-in is audited | FR-UA-001 | FIT |
| UAM-NFR-19 | Lockout | Users are locked out after 3 invalid attempts (p.14) | LOGIN_MAX_FAILED_ATTEMPTS = 3 for every BIBS user | FR-UA-001 | FIT |
| UAM-NFR-20 | Reference / master data management | Online view / add / modify / delete; referential integrity and validation; logical deletion; changes logged; role-based access (p.14) | Lists of Values with maker-checker, effective dates and deactivation instead of delete; audited | - | FIT |
| UAM-NFR-21 | Error logging | Error log with description, time, user ID, module ID, command / SQL; enable / disable; location; rotation; size parameters; native syslog; one line per entry; verbosity and message-type selection; transmit to a remote server (p.14) | One-line structured logs with user and module; levels changeable at run time; shipping to the BDO log server by the platform | - | CONFIGURE |
| UAM-NFR-22 | Transaction logging and audit trail | Transaction log with type, time, user ID, module ID; enable / disable; location; transmit; all user activities logged through the audit trail and retrievable by the administrator; allow automatic save option (p.14) | Insert-only audit trail and the Audit Trail screen; meaning of 'automatic save option' is UQ15 | FR-UA-064 | FIT |
| UAM-NFR-23 | Logs retention and archiving | Retention / archival / purging / backup for application, database, audit, infrastructure logs, historical data, snapshots, video, documents: "Follow existing QPS set up" (p.14) | As UAM-NFR-03 (UQ12) | - | CONFIGURE |
| UAM-NFR-24 | Batch processing a-l | Schedule, invoke manually, monitor, re-run, graceful terminate, UI without command line, publish status, single instance, restart at the interruption point, no privileged access, multi-core, parallel independent jobs (p.15) | Job scheduler with run history, run on demand, re-run, failure alert; restart by idempotent keys; status in monitoring | FR-UA-071 | CHANGE |
| UAM-NFR-25 | Batch failure e-mail | Monitor batch runs, log success and failure; forward an e-mail notification to designated users on failed batch runs (stated twice) (p.15) | Failed runs e-mailed to JOB_FAILURE_RECIPIENTS | FR-UA-071 | CHANGE |
| UAM-NFR-26 | System monitoring; network configuration; DR | Monitor and start / stop application processes without command line; DNS aliases; configurable ports; no hosts file; standard ports; Netbackup / Bacula, clustering, offsite replication, active-active, disk estimates (p.15) | Platform monitoring, container orchestration, environment-based configuration | - | CONFIGURE |
| UAM-NFR-27 | Data purging and archiving | Automated archiving and purging of transactional data, logs, reports, temporary files; schedule; restricted access; configurable rules (p.15-16) | Retention rules and the monthly retention review; physical archive and purge follow the BIBS retention decision (Q39, UQ12) | - | CHANGE |
| UAM-NFR-28 | Server and workstation software | Documented install / deploy / update; deploy from repository; runs as a service; multiple instances; dynamic configuration reload; least privilege; browser only on workstations; no proprietary office software; not tied to third-party versions (p.16) | Deployed from the repository; stateless multiple instances; least-privileged database user; browser-only workstations | - | FIT |
| UAM-NFR-29 | System documentation | Functional design, technical design, user manual, installation guide, release notes, instruction guide on how to manage users and extract the users list and group profile list; troubleshooting guide; training; capacity planning; performance benchmarks (p.16-17) | User-administration guide in the in-app Help Center and the administration guide (UQ18) | - | CHANGE |
| UAM-NFR-30 | System development, testing and production support | Access to resources outside the BDO network, off-site / on-site development, performance testing; maintenance access (p.17) | Engagement terms | - | CONFIGURE |
| UAM-NFR-31 | Other BU NFR 1 | Password management (reset, change) (p.17) | Administrator reset and own change exist | FR-UA-005 | FIT |
| UAM-NFR-32 | Other BU NFR 2-3 | Role-based access control (define roles, assign permissions, map users to roles); permission management (p.17) | Roles, permissions, user-role mapping, Roles screen and User Access Matrix exist | FR-UA-051 | FIT |
| UAM-NFR-33 | Other BU NFR 4 | Integration with Single Sign-On (SSO) or LDAP (p.17) | Same directory port; SSO adapter parked (UQ04) | FR-UA-003 | NEW |
| UAM-NFR-34 | Other BU NFR 5 | Session management: inactivity warning after 15 minutes; warning prior to system-triggered log out after 30 minutes (p.17) | Session policy parameters and the web session guard | FR-UA-002 | FIT |
| UAM-NFR-35 | Other BU NFR 6 | Audit logging: track changes in user roles and permissions; log user login / log out; audit log reports (p.17) | Role and permission changes in the structured change log; log-out and session log added; audit log report | FR-UA-004, 063 | CHANGE |
| UAM-NFR-36 | Other BU NFR 7 | Enforce strong password policies (p.17) | Complexity and length exist; history, maximum and minimum age and forced change added (UQ08) | FR-UA-005 | CHANGE |
| UAM-NFR-37 | Other BU NFR 8 | Self-service and admin tools: self-service password reset; user-friendly screens to manage access; a defined user can update profile information (p.17) | Self-service reset by e-mailed single-use link (local accounts); own e-mail and mobile update (UQ08, UQ17) | FR-UA-005 | NEW |
| UAM-NFR-38 | Other BU NFR 9 | Bulk uploads or batch updates of user profiles (p.17) | Bulk request (UQ10) | FR-UA-019 | NEW |
| UAM-NFR-39 | Other BU NFR 10 | Notification for access changes (p.17) | Affected user notified in the app and by e-mail when the change is applied | FR-UA-070 | CHANGE |
| UAM-NFR-40 | Other BU NFR 11 | Configurable workflow for access requests and approvals; changes to user privileges systematically monitored, evaluated and, if necessary, escalated for additional review (e.g. modifications outside working hours, low to high privilege) (p.17) | Privilege level per profile; privilege increase or out-of-hours change needs a second approval and raises an alert (UQ07) | FR-UA-034 | NEW |
| UAM-NFR-41 | Report requirements (section 7) | Samples A User Access Report, B User Group Profile Report, C Group Profile membership (titled "User Group Profile Report"), D User Access Audit Log (date, activity, from, to, done by); generated by, date coverage, date and time generated (p.18) | Four reports with generated by, coverage and time generated in the header | FR-UA-060 to 063 | CHANGE |
<!-- NFR:END -->

# Configuration items

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters and jobs

<!-- table: widths=6.2,3.6,6.8 caption="User Access Maintenance parameters and jobs" size=8.5 -->
| Parameter / job | Default | Meaning |
|---|---|---|
| LOGIN_MAX_FAILED_ATTEMPTS (exists) | 3 | Failed attempts before lock-out; all users |
| SESSION_IDLE_WARNING_MINUTES (exists) | 15 | Inactivity warning |
| SESSION_TIMEOUT_MINUTES (exists) | 30 | Inactivity sign-out |
| SESSION_EXPIRY_WARNING_MINUTES (exists) | 30 | Warning before the fixed token expiry |
| AUTH_MODE | LOCAL | LOCAL or DIRECTORY (EUA) |
| USER_ID_PATTERN | ^[a-zA-Z][0-9]{9}$ | User ID format (to confirm, UQ05) |
| PASSWORD_HISTORY_COUNT | 8 | Previous passwords refused (UQ08) |
| PASSWORD_MAX_AGE_DAYS | 90 | Password expiry (UQ08) |
| PASSWORD_MIN_AGE_DAYS | 1 | Minimum days between changes (UQ08) |
| UAM_WORKING_HOURS | 08:00-18:00, MON-FRI | Working hours for the out-of-hours flag (UQ07) |
| UAM_ANY_APPROVER | false | Any approver may decide, not only the chosen one (UQ02) |
| UAM_DIRECT_ROLE_EDIT | false | Emergency direct role edit (audited, alerted) |
| UAM_ROLE_APPLY_ON_APPROVAL | false | Group-profile requests applied on approval instead of implemented (UQ03) |
| JOB_FAILURE_RECIPIENTS | empty | E-mail recipients of failed batch runs |
| Job UAM_EFFECTIVE_CHANGES | 00:05 daily | Applies scheduled requests |
| Job PASSWORD_EXPIRY_NOTICE | 06:00 daily | Notifies users whose password expires within 7 days (LOCAL mode) |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| UAM_BUSINESS_UNIT | Empty until BDOI supplies the business unit groups (UQ05) |
| UAM_USER_LEVEL | Empty until BDOI supplies the user levels (UQ05) |
| UAM_DEACTIVATION_REASON | Resigned; Transferred; Long leave; Security; Others (to confirm) |
| Group profile privilege level | Low; Standard; High; Admin |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-UA-01 | The QPS BRD applies to BIBS as a whole: every module and persona | UQ19 |
| A-UA-02 | Group profiles are BIBS roles; a user may hold several (NFR 1.c-d) | CQ23 (answered) |
| A-UA-03 | BDOI has a User Access Matrix (p.5); its content per unit is supplied separately | OQ48 |
| A-UA-04 | Group-profile requests are implemented by the System Administrator after approval, as on p.6 | UQ03 |
| A-UA-05 | Approvers of a group-profile request decide in order and all must approve | UQ02 |
| A-UA-06 | Local sign-in continues until BDO supplies the EUA interface | UQ04, D6 |
| A-UA-07 | Infrastructure answers "Follow existing QPS set up" are met by the BIBS deployment standards | Section 8 |
| A-UA-08 | Duplicate IDs 2.002.7.1 and 3.003.1.1 are kept as printed and marked "(2nd)" | R2 section 10 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-UA-01 | BDO supplies the EUA / LDAP / SSO interface, hosts and certificates | FR-UA-003 (UQ04) |
| D-UA-02 | BDOI supplies the business unit groups, user levels and the user ID format | FR-UA-011, 052 (UQ05) |
| D-UA-03 | BDOI names the Requestors, Approvers and high-privilege profiles | Section 3, FR-UA-034 (UQ01, UQ07) |
| D-UA-04 | BDOI supplies the bulk file layout and limits | FR-UA-019 (UQ10) |
| D-UA-05 | The e-mail relay of the BIBS environment is available | FR-UA-005, 070, 071 |
| D-UA-06 | The Employee Benefits portal implements the provisioning port for external users | Section 7 (D7) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-11 (R2 section 9; status from R4)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| UQ01 | Who are the 14 Requestors and 8 Approvers; is the Requestor separate from the Business Administrator | Section 3 | OPEN |
| UQ02 | Which users may be chosen as approver; several approvers in sequence or in parallel, all required | FR-UA-015, 044 | OPEN |
| UQ03 | Must the System Administrator implement group profiles by hand, or may BIBS apply them on approval | FR-UA-041, 045 | OPEN |
| UQ04 | EUA interface: protocol, host, Windows ID format, messages; break-glass administrator; password policy with EUA | FR-UA-003 | OPEN |
| UQ05 | Values of business unit group and user level; user ID format and relation to the Windows ID | FR-UA-011, 052 | OPEN |
| UQ06 | Effective date: start only or also end (temporary access); cancelling a scheduled request | FR-UA-020 | OPEN |
| UQ07 | Which profiles are high privilege; working hours; who does the additional review | FR-UA-034 | OPEN |
| UQ08 | Password values for local accounts; channel of the self-service reset | FR-UA-005 | OPEN |
| UQ09 | Single session per device; single-role rule dropped | FR-UA-004 | PARTIAL |
| UQ10 | Bulk file layout, maximum rows, batch or line decisions, mixed actions | FR-UA-019 | OPEN |
| UQ11 | Module / task of sample B; meaning of "as of" | FR-UA-060, 061 | OPEN |
| UQ12 | QPS retention values for requests, audit and log-in logs | FR-UA-064 | OPEN |
| UQ13 | Real-time online status; administrator ending a session | FR-UA-004, 052 | OPEN |
| UQ14 | Which external ACL system is meant | FR-UA-003 | OPEN |
| UQ15 | Meaning of "allow automatic save option" in the audit-trail requirement | Section 8 | OPEN |
| UQ16 | Deactivation of a profile with active members | FR-UA-042 | OPEN |
| UQ17 | Profile fields users update themselves; approval of the change | FR-UA-005 | OPEN |
| UQ18 | The truncated documentation requirement on p.17 | Section 8 | OPEN |
| UQ19 | The QPS BRD applies to BIBS as a whole | Section 1.2 | OPEN |

BRD-11 answers questions raised on other BRDs: Q42 (directory sign-in required; inactivity sign-out at 30 minutes with a 15-minute warning), CQ23 (lock-out at 3 for all users; several roles per user) and PQ17 (group-profile changes are approved and then implemented by the System Administrator) (R4, decisions D5 and D6).

# Traceability

Every BRD-11 requirement line is met by at least one FR. The BRD prints 160 lines; IDs 2.002.7.1 and 3.003.1.1 appear twice and the second occurrence is marked "(2nd)". "Flag" repeats the BRD's Pilot Phase / Mandatory columns ("-" = not flagged). The NFR rows are traced in section 8.

<!-- TRACE:START -->
<!-- table: widths=2.7,1.1,6.9,1.2,3.1,2 caption="BRD requirement line to FR (160 lines)" status=Fit size=7.5 -->
| BRD ID | Page | Activity and requirement | Flag | FR | Fit |
|---|---|---|---|---|---|
| BRD 1.001.1 | p.8 | Log in as a Requestor - Access the application using any BDO-issued device | Yes | FR-UA-001 | FIT |
| BRD 1.001.1.1 | p.8 | Log in as a Requestor - Log in with a Requestor user profile | Yes | FR-UA-001 | CONFIGURE |
| BRD 1.001.1.2 | p.8 | Log in as a Requestor - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | FIT |
| BRD 1.001.1.3 | p.8 | Log in as a Requestor - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | FIT |
| BRD 1.002.1 | p.8 | Enroll New User - Submit a new User Access request | Yes | FR-UA-011 | FIT |
| BRD 1.002.1.1 | p.8 | Enroll New User - 1. Create request | Yes | FR-UA-010, FR-UA-011 | CHANGE |
| BRD 1.002.1.1.1 | p.8 | Enroll New User - 1.1 Input new user data | Yes | FR-UA-011 | CHANGE |
| BRD 1.002.1.1.2 | p.8 | Enroll New User - 1.2 Select User Access Group profile | Yes | FR-UA-011 | FIT |
| BRD 1.002.1.1.3 | p.8 | Enroll New User - 1.3 Select approver from the drop-down | Yes | FR-UA-015 | NEW |
| BRD 1.002.1.1.4 | p.8 | Enroll New User - 1.4 Add remarks (i.e. justification, etc.) | - | FR-UA-010 | FIT |
| BRD 1.002.1.1.5 | p.8 | Enroll New User - 1.5 Save remarks | - | FR-UA-010 | CHANGE |
| BRD 1.002.1.2 | p.8 | Enroll New User - 2. Edit drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.002.1.3 | p.8 | Enroll New User - 3. Cancel drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.002.1.4 | p.8 | Enroll New User - 4. Submit request | Yes | FR-UA-010 | FIT |
| BRD 1.003.1 | p.8 | Modify Existing User - Submit a modify User Access / permission request | Yes | FR-UA-012 | CHANGE |
| BRD 1.003.1.1 | p.8 | Modify Existing User - 1. Create request | Yes | FR-UA-010, FR-UA-012 | CHANGE |
| BRD 1.003.1.1.1 | p.8 | Modify Existing User - 1.1 Search existing user | Yes | FR-UA-012 | FIT |
| BRD 1.003.1.1.2 | p.8 | Modify Existing User - 1.2 Edit / update selected user data | - | FR-UA-012 | CHANGE |
| BRD 1.003.1.1.3 | p.8 | Modify Existing User - 1.3 Select User Access Group profile | Yes | FR-UA-012 | FIT |
| BRD 1.003.1.1.4 | p.8 | Modify Existing User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | NEW |
| BRD 1.003.1.1.5 | p.8 | Modify Existing User - 1.5 Add remarks (i.e. justification, etc.) | - | FR-UA-010 | FIT |
| BRD 1.003.1.1.6 | p.8 | Modify Existing User - 1.6 Save remarks | - | FR-UA-010 | CHANGE |
| BRD 1.003.1.2 | p.8 | Modify Existing User - 2. Edit drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.003.1.3 | p.8 | Modify Existing User - 3. Cancel drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.003.1.4 | p.8 | Modify Existing User - 4. Submit request | Yes | FR-UA-010 | FIT |
| BRD 1.004.1 | p.8 | Deactivate User - Submit a User Access deactivation request | Yes | FR-UA-013 | FIT |
| BRD 1.004.1.1 | p.8 | Deactivate User - 1. Create a request | Yes | FR-UA-010, FR-UA-013 | CHANGE |
| BRD 1.004.1.1.1 | p.8 | Deactivate User - 1.1 Search an existing user | Yes | FR-UA-013 | FIT |
| BRD 1.004.1.1.2 | p.8 | Deactivate User - 1.2 Add remarks (i.e. reason for deactivation) | - | FR-UA-010, FR-UA-013 | FIT |
| BRD 1.004.1.1.3 | p.8 | Deactivate User - 1.3 Save remarks | Yes | FR-UA-010, FR-UA-013 | CHANGE |
| BRD 1.004.1.1.4 | p.8 | Deactivate User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | NEW |
| BRD 1.004.1.2 | p.8 | Deactivate User - 2. Edit drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.004.1.3 | p.8 | Deactivate User - 3. Cancel drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.004.1.4 | p.8 | Deactivate User - 4. Submit request | Yes | FR-UA-010 | FIT |
| BRD 1.005 | p.8 | Reactivate User - Submit a User Access reactivation request | Yes | FR-UA-014 | FIT |
| BRD 1.005.1 | p.8 | Reactivate User - 1. Create a request | Yes | FR-UA-010, FR-UA-014 | CHANGE |
| BRD 1.005.1.1 | p.8 | Reactivate User - 1.1 Search an existing user | Yes | FR-UA-014 | FIT |
| BRD 1.005.1.1.1 | p.8 | Reactivate User - 1.2 Add remarks (i.e. reason for reactivation) | - | FR-UA-010, FR-UA-014 | FIT |
| BRD 1.005.1.1.2 | p.8 | Reactivate User - 1.3 Save remarks | Yes | FR-UA-010, FR-UA-014 | CHANGE |
| BRD 1.005.1.1.3 | p.8 | Reactivate User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | NEW |
| BRD 1.005.1.2 | p.8 | Reactivate User - 2. Edit a drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.005.1.3 | p.8 | Reactivate User - 3. Cancel a drafted request | Yes | FR-UA-010 | CHANGE |
| BRD 1.005.1.4 | p.8 | Reactivate User - 4. Submit a request | Yes | FR-UA-010 | FIT |
| BRD 1.006.1 | p.8 | Apply Correction - Apply correction on returned requests | Yes | FR-UA-016 | CHANGE |
| BRD 1.006.1.1 | p.8 | Apply Correction - 1. Receive notification on the returned request | Yes | FR-UA-016, FR-UA-070 | CHANGE |
| BRD 1.006.1.2 | p.8 | Apply Correction - 2. Edit data on the returned request | Yes | FR-UA-016 | CHANGE |
| BRD 1.006.1.3 | p.8 | Apply Correction - 3. Add remarks | Yes | FR-UA-016 | CHANGE |
| BRD 1.006.1.4 | p.8 | Apply Correction - 4. Save remarks | Yes | FR-UA-016 | CHANGE |
| BRD 1.006.1.5 | p.8 | Apply Correction - 5. Re-submit request | Yes | FR-UA-016 | CHANGE |
| BRD 1.007.1 | p.8 | Cancel a Request - Cancel a submitted request for approval | Yes | FR-UA-017 | CHANGE |
| BRD 1.007.1.1 | p.8 | Cancel a Request - 1. Search request | Yes | FR-UA-017 | FIT |
| BRD 1.007.1.2 | p.8 | Cancel a Request - 2. Add remarks (i.e. reason for cancellation, etc.) | Yes | FR-UA-017 | CHANGE |
| BRD 1.007.1.3 | p.8 | Cancel a Request - 3. Save remarks | Yes | FR-UA-017 | CHANGE |
| BRD 1.007.1.4 | p.8 | Cancel a Request - 4. Cancel submitted request | Yes | FR-UA-017 | CHANGE |
| BRD 1.008.1 | p.8 | View Request/s - View the submitted request/s | - | FR-UA-018 | FIT |
| BRD 1.008.1.1 | p.8 | View Request/s - 1. View the list of the submitted request/s | Yes | FR-UA-018 | FIT |
| BRD 1.008.1.2 | p.8 | View Request/s - 2. View the status of the submitted request/s | Yes | FR-UA-018 | FIT |
| BRD 1.008.1.3 | p.8 | View Request/s - 3. View the details of the submitted request/s | Yes | FR-UA-018 | FIT |
| BRD 1.008.1.4 | p.8 | View Request/s - 4. Receive notification on the approved / rejected request | Yes | FR-UA-018, FR-UA-070 | FIT |
| BRD 1.009.1 | p.8 | Bulk Request - Submit request for bulk creation / modification / deactivation / reactivation | - | FR-UA-019 | NEW |
| BRD 1.009.1.1 | p.8 | Bulk Request - 1. Attach a file for bulk processing | Yes | FR-UA-019 | NEW |
| BRD 1.009.1.2 | p.8 | Bulk Request - 2. Add remarks | Yes | FR-UA-019 | NEW |
| BRD 1.009.1.3 | p.8 | Bulk Request - 3. Save remarks | Yes | FR-UA-019 | NEW |
| BRD 1.009.1.4 | p.8 | Bulk Request - 4. Edit a drafted request | Yes | FR-UA-019 | NEW |
| BRD 1.009.1.5 | p.8 | Bulk Request - 5. Cancel a drafted request | Yes | FR-UA-019 | NEW |
| BRD 1.009.1.6 | p.8 | Bulk Request - 6. Submit a request | Yes | FR-UA-019 | NEW |
| BRD 2.001.1 | p.8 | Log in as an Approver - Access the application using any BDO-issued device | Yes | FR-UA-001 | FIT |
| BRD 2.001.1.1 | p.8 | Log in as an Approver - Log in with an Approver user profile | Yes | FR-UA-001 | CONFIGURE |
| BRD 2.001.1.2 | p.8 | Log in as an Approver - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | FIT |
| BRD 2.001.1.3 | p.8 | Log in as an Approver - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | FIT |
| BRD 2.002.1 | p.8 | Review and Approve Request/s - a. Receive notification on the request for approval | Yes | FR-UA-030, FR-UA-015, FR-UA-070 | CHANGE |
| BRD 2.002.2 | p.8 | Review and Approve Request/s - b. View the list of requests for approval | Yes | FR-UA-030 | FIT |
| BRD 2.002.2.1 | p.8 | Review and Approve Request/s - 1. Filter the list assigned to me | Yes | FR-UA-030 | CHANGE |
| BRD 2.002.3 | p.8 | Review and Approve Request/s - c. Select request | Yes | FR-UA-030 | FIT |
| BRD 2.002.4 | p.8 | Review and Approve Request/s - d. View request details | Yes | FR-UA-030 | FIT |
| BRD 2.002.5 | p.8 | Review and Approve Request/s - e. Approve request | Yes | FR-UA-031 | FIT |
| BRD 2.002.6 | p.8 | Review and Approve Request/s - f. Reject request | Yes | FR-UA-032 | FIT |
| BRD 2.002.6.1 | p.8 | Review and Approve Request/s - 1. Add remarks (i.e. rejection reason, etc.) | - | FR-UA-032 | FIT |
| BRD 2.002.6.2 | p.9 | Review and Approve Request/s - 2. Save remarks | - | FR-UA-032 | FIT |
| BRD 2.002.7 | p.9 | Review and Approve Request/s - g. Return request to Requestor | Yes | FR-UA-033 | CHANGE |
| BRD 2.002.7.1 | p.9 | Review and Approve Request/s - 1. Add remarks (i.e. reason for return, etc.) | Yes | FR-UA-033 | CHANGE |
| BRD 2.002.7.1 (2nd) | p.9 | Review and Approve Request/s - 2. Save remarks | Yes | FR-UA-033 | CHANGE |
| BRD 3.001.1 | p.9 | Log in as a Business Administrator - Access the application using any BDO-issued device | Yes | FR-UA-001 | FIT |
| BRD 3.001.1.1 | p.9 | Log in as a Business Administrator - Log in with a Business Administrator user profile | Yes | FR-UA-001 | FIT |
| BRD 3.001.1.2 | p.9 | Log in as a Business Administrator - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | FIT |
| BRD 3.001.1.3 | p.9 | Log in as a Business Administrator - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | FIT |
| BRD 3.002.1 | p.9 | Submit a User Access Group Profile Request - a. Submit a new User Access Group Profiles request | Yes | FR-UA-040 | CHANGE |
| BRD 3.002.1.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-040 | CHANGE |
| BRD 3.002.1.2 | p.9 | Submit a User Access Group Profile Request - 2. Select approver/s from the drop-down | - | FR-UA-044 | NEW |
| BRD 3.002.1.3 | p.9 | Submit a User Access Group Profile Request - 3. Edit a drafted request | Yes | FR-UA-040 | CHANGE |
| BRD 3.002.1.4 | p.9 | Submit a User Access Group Profile Request - 4. Cancel a drafted request | Yes | FR-UA-040 | CHANGE |
| BRD 3.002.1.5 | p.9 | Submit a User Access Group Profile Request - 5. Submit a request | Yes | FR-UA-040 | FIT |
| BRD 3.002.1.6 | p.9 | Submit a User Access Group Profile Request - 6. Add remarks | - | FR-UA-040 | FIT |
| BRD 3.002.1.7 | p.9 | Submit a User Access Group Profile Request - 7. Save remarks | - | FR-UA-040 | CHANGE |
| BRD 3.002.2 | p.9 | Submit a User Access Group Profile Request - b. Submit a modify User Access Group Profiles request | Yes | FR-UA-041 | FIT |
| BRD 3.002.2.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-041 | CHANGE |
| BRD 3.002.2.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-041 | FIT |
| BRD 3.002.2.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | NEW |
| BRD 3.002.2.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-041 | CHANGE |
| BRD 3.002.2.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-041 | CHANGE |
| BRD 3.002.2.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-041 | FIT |
| BRD 3.002.2.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-041 | FIT |
| BRD 3.002.2.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-041 | CHANGE |
| BRD 3.002.3 | p.9 | Submit a User Access Group Profile Request - c. Submit a User Access Group Profiles deactivation request | Yes | FR-UA-042 | NEW |
| BRD 3.002.3.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-042 | NEW |
| BRD 3.002.3.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-042 | FIT |
| BRD 3.002.3.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | NEW |
| BRD 3.002.3.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-042 | NEW |
| BRD 3.002.3.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-042 | NEW |
| BRD 3.002.3.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-042 | NEW |
| BRD 3.002.3.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-042 | FIT |
| BRD 3.002.3.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-042 | CHANGE |
| BRD 3.002.4 | p.9 | Submit a User Access Group Profile Request - d. Submit a User Access Group Profiles reactivation request | Yes | FR-UA-043 | NEW |
| BRD 3.002.4.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-043 | NEW |
| BRD 3.002.4.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-043 | FIT |
| BRD 3.002.4.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | NEW |
| BRD 3.002.4.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-043 | NEW |
| BRD 3.002.4.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-043 | NEW |
| BRD 3.002.4.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-043 | NEW |
| BRD 3.002.4.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-043 | FIT |
| BRD 3.002.4.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-043 | CHANGE |
| BRD 3.003.1 | p.9 | Generate Reports - a. Generate customized report | Yes | FR-UA-060 | CHANGE |
| BRD 3.003.1.1 | p.9 | Generate Reports - 1. User Access Report | Yes | FR-UA-060 | NEW |
| BRD 3.003.1.1 (2nd) | p.9 | Generate Reports - 1.1 User name | Yes | FR-UA-060 | FIT |
| BRD 3.003.1.2 | p.9 | Generate Reports - 1.2 Group profile | Yes | FR-UA-060 | FIT |
| BRD 3.003.1.3 | p.9 | Generate Reports - 1.3 Date created | Yes | FR-UA-060 | FIT |
| BRD 3.003.1.4 | p.9 | Generate Reports - 1.4 Date modified | Yes | FR-UA-060 | FIT |
| BRD 3.003.1.5 | p.9 | Generate Reports - 1.5 Created by | Yes | FR-UA-060 | CHANGE |
| BRD 3.003.1.6 | p.9 | Generate Reports - 1.6 Added / Modified / deactivated / reactivated by | Yes | FR-UA-060 | CHANGE |
| BRD 3.003.2 | p.9 | Generate Reports - 2. User Group Profile Report | Yes | FR-UA-061 | CHANGE |
| BRD 3.003.2.1 | p.9 | Generate Reports - 2.1 Group profile name | Yes | FR-UA-061 | FIT |
| BRD 3.003.2.2 | p.9 | Generate Reports - 2.2 Modules accessed under each group profile | Yes | FR-UA-061 | CHANGE |
| BRD 3.003.2.3 | p.9 | Generate Reports - 2.3 Date created | Yes | FR-UA-061 | FIT |
| BRD 3.003.2.4 | p.9 | Generate Reports - 2.4 Date modified | Yes | FR-UA-061 | FIT |
| BRD 3.003.2.5 | p.9 | Generate Reports - 2.5 Created by | Yes | FR-UA-061 | FIT |
| BRD 3.003.2.6 | p.9 | Generate Reports - 2.6 Added / Modified / deactivated / reactivated by | Yes | FR-UA-061 | CHANGE |
| BRD 3.003.3 | p.9 | Generate Reports - 3. Group Profile Membership list | Yes | FR-UA-062 | NEW |
| BRD 3.003.3.1 | p.9 | Generate Reports - 1. List of members under each profile | Yes | FR-UA-062 | NEW |
| BRD 3.003.3.1.1 | p.9 | Generate Reports - 1.1 Group profile | Yes | FR-UA-062 | FIT |
| BRD 3.003.3.1.2 | p.9 | Generate Reports - 1.2 User name | Yes | FR-UA-062 | FIT |
| BRD 4.001.1 | p.9 | Log in as a System Administrator - Access the application using any BDO-issued device | - | FR-UA-001 | FIT |
| BRD 4.001.1.1 | p.9 | Log in as a System Administrator - Log in with a System Administrator user profile | - | FR-UA-001 | FIT |
| BRD 4.001.1.2 | p.9 | Log in as a System Administrator - Receive the inactivity warning (15 minutes) | - | FR-UA-002 | FIT |
| BRD 4.001.1.3 | p.9 | Log in as a System Administrator - Receive a warning prior to the system-triggered log out (30 minutes) | - | FR-UA-002 | FIT |
| BRD 4.002.1 | p.9 | Manage Users of the System - a. Define user profile / role | Yes | FR-UA-050, FR-UA-045 | FIT |
| BRD 4.002.1.1 | p.9 | Manage Users of the System - 1. Create user profile / role for Requestor | Yes | FR-UA-050 | CONFIGURE |
| BRD 4.002.1.2 | p.9 | Manage Users of the System - 2. Create user profile / role for Approver | Yes | FR-UA-050 | CONFIGURE |
| BRD 4.002.1.3 | p.9 | Manage Users of the System - 3. Create user profile / role for Business Administrator | Yes | FR-UA-050 | FIT |
| BRD 4.002.2 | p.9 | Manage Users of the System - b. Define specific functionality or capability for a specific user profile / role | Yes | FR-UA-051 | FIT |
| BRD 4.002.2.1 | p.9 | Manage Users of the System - 1. Assign Enroll New User to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.2 | p.9 | Manage Users of the System - 2. Assign Modify Existing User function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.3 | p.9 | Manage Users of the System - 3. Assign Deactivate User function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.4 | p.9 | Manage Users of the System - 4. Assign Reactivate User function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.5 | p.9 | Manage Users of the System - 5. Assign Apply Correction function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.6 | p.9 | Manage Users of the System - 6. Assign Cancel a Request function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.7 | p.10 | Manage Users of the System - 7. Assign View Request/s function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.8 | p.10 | Manage Users of the System - 8. Assign Review and Approve Request/s function to a specific user profile / role | Yes | FR-UA-051 | FIT |
| BRD 4.002.2.9 | p.10 | Manage Users of the System - 9. Assign Submit a User Access Group Profiles Request function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.002.2.10 | p.10 | Manage Users of the System - 10. [Assign] Generate Reports Request function to a specific user profile / role | Yes | FR-UA-051 | CHANGE |
| BRD 4.003.1 | p.10 | Generate Logs - a. Generate audit logs for compliance and audit purposes as needed | Yes | FR-UA-063, FR-UA-064 | CHANGE |
<!-- TRACE:END -->

# Sign-off

By signing, BDOI confirms that this FRS describes the User Access Maintenance functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Process Owner, User Access Maintenance", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank / Corbank Marketing and Corporate Processing", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Program Manager, ES-BPS", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
