---
# Source of the Functional Requirements Specification for BRD-11 User Access Maintenance.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-11_User_Access_Maintenance/FRS_BRD11_USER_ACCESS_MAINTENANCE.md
# The traceability table (section 11) and the NFR table (section 8) were generated once from the rows of
# docs/requirements/BDOI_UAM_BRD_SPEC.md (sections 5 and 7); keep them in step with the FR BRD traces when editing.
title: User Access Maintenance
subtitle: BRD-11 User Access Maintenance (QPS User Access Maintenance Module)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-11
name: User Access Maintenance
doc_id: BIBS-FRS-BRD-11
version: "2.1"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: FRS BRD-11 User Access Maintenance
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-11 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Product Owner (pending)
    change: "BDOI Drop 0 integrations of 26-Sep-2026: sign-in through EIAM (Microsoft Entra ID, OpenID Connect) as the target of FR-UA-003; UIDM-ISC (IGA) provisioning against the BIBS request rule, options for BDOI (IQ04, IQ05; register DCR-229, DCR-230); portal users of decision D7 dormant (EB without portal, IQ22)"
  - version: "2.0"
    date: 27 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business owner, Information Security and BDOI IT (sign-off)
    change: "Business sign-off pack: the messages and codes of the screens in every FR, the access functions of section 2.3, proposed business rules and clarifications for confirmation (chapter 22), the menu of every persona, screen specifications with screenshots, walkthroughs, messages, notifications, reports as documents, the bulk upload, configuration inputs the business provides, cross-BRD contract, sign-off and change control (chapters 12-21). Issued 27-Sep-2026. FR, BRD and test IDs kept"
  - version: "2.1"
    date: 8 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business owner, Information Security and BDOI IT (sign-off)
    change: "Re-based on the User Access Maintenance BRD v1 (15-Apr-2025; no newer version received) and the Drop 0 decisions of 26-Sep-2026; user-story view and storyboard index added. Single sign-on with OpenID Connect and SAML 2.0 stated as part of BIBS, switched on by configuration, with EIAM on Microsoft Entra ID as the provider (FR-UA-003, chapter 7: SSO no longer on hold); EIAM sign-out on Log Out (FR-UA-004) and user matching by Windows ID or UPN as proposed rules; clarifications CLR-UA-21 to CLR-UA-27 and the Drop 0 question of every clarification (UA-Q01 to UA-Q57); reference to the Authentication and Identity Integration Requirements v1.0 (03-Oct-2026); screen count corrected to 19"
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Business Administrators (process owner)", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Unit Heads, Combank and Corbank Marketing / Corporate Processing", role: Approver, organisation: BDOI, purpose: Review of the request and approval steps}
  - {name: "BDOI Information Security", role: Signatory, organisation: BDOI, purpose: "Review and sign-off of the role matrix, the menus by persona and the separation-of-duties rules"}
  - {name: "BDOI IT", role: Signatory, organisation: BDOI, purpose: "Review and sign-off of the sign-in and identity integration assumptions (EIAM, UIDM-ISC) and the System Administrator functions"}
  - {name: "ITIO-SRE and ITSD-AMS", role: System administration, organisation: BDO Unibank, purpose: "Review of the System Administrator functions, sign-in and logs"}
  - {name: "ES-BPDS, Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the User Access Maintenance business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that user access will be managed as the business expects. The project team uses it to deliver, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

User access in BIBS covers users, group profiles (roles) and permissions, sign-in with lock-out, the session policy, access requests under four eyes and the User Access Matrix; section 2.3 summarises each access function. The FRs quote the screen names, messages and codes that the users see. Where the proposed rule or screen differs from the BRD, or needs a decision of BDOI, chapter 22 lists it for confirmation. Chapters 12 to 21 are the business view of the system for the sign-off: the menu of every persona, the screens with their fields and messages, the walkthroughs, the reports, the configuration inputs and the interface contract.

## Scope

The BRD was written in April 2025 for the Quotation and Pre-processing System (QPS), before BIBS. In BIBS it becomes the **system-wide** user access maintenance: every BIBS module uses the same users, group profiles (BIBS roles) and permissions (UQ19).

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Sign-in and session | Log-in by persona; single sign-on with the BDO identity provider (EIAM on Microsoft Entra ID) through OpenID Connect or SAML 2.0; directory authentication (EUA / Windows ID) until EIAM replaces it; inactivity warning and log-out, lock-out, password policy for local accounts, log-out and session log | BRD 1.001, 2.001, 3.001, 4.001; NFR p.13-14, 17 |
| User access requests | Enrol, modify, deactivate and reactivate users; drafts, remarks, chosen approver, correction of returned requests, cancellation, view of own requests, bulk requests, effective dates | BRD 1.002 to 1.009; NFR p.13, 17 |
| Approval | Notification, list and filter, details, approve, reject, return; second approval of risky changes | BRD 2.002; NFR p.17 |
| Group profiles | Requests to create, modify, deactivate and reactivate group profiles with approvers in order; implementation by the System Administrator | BRD 3.002; p.6 |
| System Administrator | Group profiles of the UAM personas; functions assigned to profiles; user data maintenance | BRD 4.002; NFR p.13-14 |
| Reports and logs | User Access Report, User Group Profile Report, Group Profile Membership list, User Access Audit Log; structured access-change log | BRD 3.003, 4.003; p.18 |

**Out of scope for this phase:**

- Infrastructure items the BRD answers with "Follow existing QPS set up" (servers, network, DR, environments, support). They follow the BIBS deployment standards (section 8).
- Data and user migration: "N/A" in the BRD (p.12). Existing users can be loaded with the bulk request (FR-UA-019).
- Automatic joiner and leaver feeds from HR. The BRD does not ask for them; bulk requests cover mass changes.
- The EUA and LDAP / Active Directory connections, which the project team proposes to replace by EIAM (CLR-UA-21), and an external ACL (UQ14). Single sign-on is in scope: BIBS signs users in through the BDO identity provider with OpenID Connect or SAML 2.0, switched on by configuration once BDOI IT registers BIBS in EIAM for each environment (FR-UA-003, CLR-UA-01). The protocol, registration and provisioning details are in the Authentication and Identity Integration Requirements v1.0, 03-Oct-2026 (R9), reviewed by BDOI IT.

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Business Requirements Document - QPS User Access Maintenance Module, 39 pages (pp.1-19 text; pp.20-38 the signed scanned copy; p.39 e-mail sign-off) | v1, 15-Apr-2025; signed April-May 2025 |
| R2 | BDOI User Access Maintenance (BRD-11) requirements baseline | current |
| R4 | Cross-BRD decisions and answered questions | current |
| R6 | FRS BRD-3 Product Maintenance (role-permission change requests, PMADD05) | v2.0 |
| R7 | BDO UX guidelines (brand, screen patterns) | current |
| R8 | BRD-1 New Business business sign-off set (My Approvals, Lists of Values, Data Retention) | v2.0 |
| R9 | Authentication and Identity Integration Requirements (single sign-on with OpenID Connect and SAML 2.0 through EIAM, provisioning and aggregation with UIDM-ISC; issued separately, reviewed by BDOI IT) | v1.0, 03-Oct-2026 |
| R10 | Drop 0 questions to BDOI, BRD-11 User Access Maintenance (UA-Q01 to UA-Q57) | October 2026 |

Page references in this document ("p.8") are pages of the BRD PDF (R1). The BRD has no NFR IDs; the NFR rows carry the analyst's IDs UAM-NFR-01 to UAM-NFR-41 of R2.

## Definitions and acronyms

```glossary
Access request: A request to create, change, deactivate or reactivate a user, or to change a group profile, decided under four eyes; numbered AR-yyyy-nnnnnn
Approver: The user who reviews and approves, rejects or returns access requests (BRD section B)
BRD: Business Requirements Document
Business Administrator: The BDOI user who defines and manages group profiles and raises group-profile requests (BRD section C)
EIAM: BDO Enterprise Identity Access Management on Microsoft Entra ID, the target sign-in of BDO users named by BDOI for Drop 0 (owned by BDOI IT)
EUA: BDO end-user authentication service that validates the Windows ID and password (NFR p.14)
FR: Functional requirement of this document (FR-UA-nnn)
Group profile: A set of permissions assigned to a group of users (BRD p.4); a role in BIBS
LDAP / AD: Lightweight Directory Access Protocol / Active Directory
OIDC: OpenID Connect, a single sign-on standard; BIBS signs a user in with the identity the BDO identity provider returns
SAML 2.0: Security Assertion Markup Language, the other single sign-on standard BIBS supports
Permission: One function or capability in BIBS, with an area (module) and an action class (VIEW, CREATE, AMEND, APPROVE)
QPS: Quotation and Pre-processing System, the legacy system the BRD was written for
Requestor: The user who submits user access requests (BRD section A)
SSO: Single sign-on - the user signs in once with the BDO account at the BDO identity provider (EIAM) and BIBS opens a session without a BIBS password
Break-glass administrator: A named System Administrator account that keeps a local BIBS password and second factor so that BIBS can be administered when the identity provider is not available
System Administrator: The IT user who implements group profiles and maintains users and roles (BRD section D)
UAM-NFR-nn: ID given by the analyst to a non-functional requirement of the BRD (R2 section 7)
Seed data: The fictitious data of the SIT environment (users, requests, profiles) used in the screenshots and walkthroughs
UAT: User acceptance testing
UIDM-ISC: BDO's identity governance and administration (IGA) tool, named by BDOI for Drop 0 for joiner, mover and leaver provisioning (owned by BDOI IT)
UPN: User principal name, the sign-in name of a BDO account in Microsoft Entra ID (for example name@bdo.com.ph)
UQnn: Open question on BRD-11 raised by the project team (section 10.3)
UA-Qnn: Question on BRD-11 sent to BDOI with the Drop 0 questions (R10)
User Access: The permissions and privileges granted to a user (BRD p.4)
User Access Matrix: The screen and export that show which roles hold which permissions, by permission and by action class
Windows ID: The user's BDO network log-on name, held on every user; used to match the EIAM account to the BIBS user (proposed, CLR-UA-22) and for EUA authentication
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the **priority** ("Must have" for the rows the BRD marks "Mandatory: Yes"; rows without a flag are noted) and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**. The description states the proposed behaviour of the function.
- **Business rules**. *Configurable* rules are maintained by the System Administrator in BIBS (parameter or list of values, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a screen check (for example a blank mandatory field), which has no business code. Chapter 15 lists every message, as the screens show it.
- **Screens and fields**, **notifications**, **audit** and numbered **acceptance criteria**, which are the basis of the BRD-11 test plan.

The BRD lists 160 requirement lines, many of them sub-steps of one function ("create request", "add remarks", "save remarks"). The FRs group them by function; section 11 lists every line with its FR.


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

## Access functions

The table summarises each access function of BIBS: the screen, the permission and the proposed behaviour. The Access Requests and User Access Matrix screens are in the User Access section of the menu.

<!-- table: widths=3.4,6.6,6.6 caption="Access functions of BIBS" size=8.5 -->
| Function | Screen, permission and main behaviour | Further proposed behaviour |
|---|---|---|
| Sign-in and lock-out | Login screen; user name and password; the account locks after LOGIN_MAX_FAILED_ATTEMPTS = 3 failed attempts until an administrator unlocks it; every success and failure is in the audit trail | Directory authentication with the Windows ID when BDO supplies the interface; log-out and session log |
| Session policy | Inactivity warning after SESSION_IDLE_WARNING_MINUTES = 15; sign-out after SESSION_TIMEOUT_MINUTES = 30 of inactivity; warning SESSION_EXPIRY_WARNING_MINUTES = 30 before the fixed session expiry | Confirmed by the BRD (Q42) |
| Users | Administration > Users (USER_MANAGE): user name, full name, e-mail, roles, authorisation limit, last login, status; unlock and reset password | Windows ID, business unit group, user level, status incl. online; changes through requests |
| Roles | Administration > Roles & Permissions (ROLE_MANAGE): create a role, name it, tick permissions | Active flag, description, privilege level; changes by implementing an approved group-profile request |
| Access requests | User Access > Access Requests (ACCESS_REQUEST to raise, ACCESS_APPROVE to decide): types Create user, Change roles, Disable user, Enable user, Change role permissions; the approver cannot be the requester; approval applies the change and shows a temporary password once | Drafts, return, correction, cancellation, chosen approver(s), new types (modify user, group-profile create / deactivate / reactivate), effective date, bulk, second approval, history, implementation step |
| Notifications | The chosen approver on submission; the requester on the decision | Returned, cancelled, second approval, implementation; the affected user |
| User Access Matrix | User Access > User Access Matrix: roles by permission and by area / action class, Excel export | Every permission gets an area, so the group-profile report shows a module for every task |
| Password | My Profile > Change password: at least 10 characters with upper and lower case, digit and symbol; administrator reset | Password history, maximum and minimum age, forced change after reset, self-service reset |
| Audit | Audit trail that no user can change; Administration > Audit Trail report (CTL-AUDIT) with summary text | Access-change log with from / to values that no user can change; four reports |

# Personas and roles

## Personas

<!-- table: widths=3,4.6,6.8,3.2 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in User Access Maintenance | BRD |
|---|---|---|---|
| Requestor | UAM_REQUESTOR | Raises user access requests: enrol, modify, deactivate, reactivate, bulk; corrects and cancels them; views own requests | Section A (1.001-1.009) |
| Approver | UAM_APPROVER; NB_APPROVER keeps its approval right | Reviews and approves, rejects or returns requests assigned to him or her | Section B (2.001-2.002) |
| Second approver | UAM_SECOND_APPROVER | Reviews privileged or out-of-hours changes after the first approval | NFR 11 (p.17) |
| Business Administrator | BUSINESS_ADMIN | Raises group-profile requests; generates the reports; maintains the separation-of-duties rules; uploads bulk requests; supports users | Section C (3.001-3.003) |
| System Administrator | SYSADMIN | Implements approved group-profile requests; defines the UAM group profiles and their functions; unlocks users and resets passwords; audit logs | Section D (4.001-4.003) |
| Auditor | AUDITOR | Views requests and runs the reports and the audit log | NFR 6 (p.17) |
| Information Security Officer | INFOSEC_OFFICER | Views the access requests, the group-profile requests and the User Access Matrix, and the audit trail; runs the user access reports; authorises the separation-of-duties rules; approves the changes of the security settings | NFR 11 (p.17) |
| System | - | Validates, grants the access, applies dated changes, logs, notifies | p.6 |

The BRD's stakeholders also list ITIO-SRE (system administration), ITIO-ES (Storage Administrator) and ITSD-AMS (application support) (p.7). They support the platform and have no functional role in BIBS beyond the System Administrator. Who the 14 Requestors and 8 Approvers are is UQ01.

## Permissions

BRD 4.002.2 asks that each function can be assigned to any group profile. BIBS gives each function its own permission (area USER_ACCESS). ACCESS_REQUEST is the umbrella permission of the request functions for the roles that hold it.

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
| ACCESS_APPROVE | APPROVE | Review and approve requests | 4.002.2.8 |
| UAM_GROUP_REQUEST | CREATE | Submit group-profile requests | 4.002.2.9 |
| UAM_REPORT_VIEW | VIEW | Generate the user access reports | 4.002.2.10 |
| UAM_SECOND_APPROVE | APPROVE | Second approval of privileged or out-of-hours changes | NFR 11 |
| UAM_SOD_MAINTAIN | AMEND | Add and deactivate separation-of-duties rules | NFR 11 |
| UAM_SOD_AUTHORIZE | APPROVE | Authorise the separation-of-duties rules | NFR 11 |
| SECURITY_PARAMETER_APPROVE (area ADMINISTRATION) | APPROVE | Approve a change of a security setting | NFR 11 |
| ACCESS_REQUEST | CREATE | Umbrella of the request functions | - |
| USER_MANAGE, ROLE_MANAGE | AMEND | Users and Roles screens; implementation of group-profile requests | 4.002.1 |

## Permissions matrix

<!-- table: widths=4.4,1.6,1.6,1.6,1.6,1.6,1.6,1.6,1.6 caption="Role-to-action matrix for User Access Maintenance (proposal until UQ01 / OQ48)" size=8 -->
| Permission | Requestor | Approver | Second approver | Business Admin | System Admin | NB Approver | Auditor | InfoSec Officer |
|---|---|---|---|---|---|---|---|---|
| UAM_ENROLL | Y | | | | | | | |
| UAM_MODIFY | Y | | | | | | | |
| UAM_DEACTIVATE | Y | | | | | | | |
| UAM_REACTIVATE | Y | | | | | | | |
| UAM_CORRECT | Y | | | Y | | | | |
| UAM_CANCEL | Y | | | Y | | | | |
| UAM_VIEW | Y | Y | Y | Y | Y | | Y | |
| ACCESS_APPROVE | | Y | | | | Y | | |
| UAM_SECOND_APPROVE | | | Y | | | | | |
| UAM_GROUP_REQUEST | | | | Y | | | | |
| UAM_REPORT_VIEW | | Y | | Y | Y | | Y | Y |
| ACCESS_REQUEST | | | | Y | Y | | | |
| USER_MANAGE, ROLE_MANAGE | | | | | Y | | | |
| BULK_PROCESS (bulk upload) | Y | | | Y | Y | | | |
| UAM_SOD_MAINTAIN | | | | Y | | | | |
| UAM_SOD_AUTHORIZE | | | | | | | | Y |
| SECURITY_PARAMETER_APPROVE | | | | | | | | Y |

Segregation of duties is enforced by the system, whatever the roles grant: the requester and the user the request is about never decide it, the second approver differs from the first, and the implementer of a group-profile request is not its requester. On top of this, the separation-of-duties rules name the pairs of group profiles one user may not hold together; every request and bulk line is checked against them (FR-UA-053).

# Functional requirements

## Sign-in, session and password

```fr
id: FR-UA-001
title: Log in with a persona profile from a BDO-issued device
brd: [BRD 1.001.1 (p.8), BRD 1.001.1.1 (p.8), BRD 2.001.1 (p.8), BRD 2.001.1.1 (p.8), BRD 3.001.1 (p.9), BRD 3.001.1.1 (p.9), BRD 4.001.1 (p.9), BRD 4.001.1.1 (p.9), UAM-NFR-18 (p.14), UAM-NFR-19 (p.14)]
actor: Requestor, Approver, Business Administrator, System Administrator
priority: "Must have (4.001.x has no flag in the BRD)"
screens: Login; home page with the menu of the user's roles
description:
  - "Users sign in to BIBS in a browser with their user ID and password. BIBS checks the credentials and the account status, counts failed attempts and locks the account after LOGIN_MAX_FAILED_ATTEMPTS failed attempts (3, for every BIBS user). Every successful and failed attempt is written to the audit trail. The menu shows only the screens of the user's roles."
  - "The Requestor and Approver profiles are the roles UAM_REQUESTOR and UAM_APPROVER. Access only from BDO-issued devices is enforced by the BDO network and device policy, not by BIBS."
preconditions:
  - "The user has an enabled BIBS account with at least one role."
main_flow:
  - The user opens BIBS from a BDO-issued device and enters the user ID and password.
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the menu of the user's roles.
alternate_flows:
  - Wrong credentials. BIBS refuses the log-in and counts the failed attempt.
  - Third failed attempt. The account locks; the System Administrator unlocks it (Administration > Users, Unlock) or reactivates it through a request.
  - Deactivated account. BIBS refuses the log-in with the same message as a wrong password; the attempt does not count towards the lock-out and is written to the audit trail.
  - Single sign-on or directory mode (FR-UA-003). The password is checked by the BDO identity provider (EIAM) or by EUA instead of BIBS.
rules:
  - [R1, "Lock-out after LOGIN_MAX_FAILED_ATTEMPTS consecutive failures (3); applies to all users (CQ23 answered by this BRD).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R2, "A user may hold several roles; the menu is the union of their screens (NFR 1.c-d).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user name or password, AUTHENTICATION_FAILED]
  - [Account locked (the same message as a wrong password; the lock shows to the System Administrator on Users), Invalid user name or password, AUTHENTICATION_FAILED]
  - [Account deactivated (the same message as a wrong password; the status shows to the System Administrator on Users), Invalid user name or password, AUTHENTICATION_FAILED]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded (LOGIN, LOGIN_FAILED) with user and time.
acceptance:
  - A user with the Requestor profile logs in and sees the User Access screens of the Requestor only.
  - The third wrong password locks the account; the fourth attempt, with the correct password, is refused with the same message as a wrong password ("Invalid user name or password"), and Users shows the account as locked to the System Administrator.
  - A deactivated user is refused with the same message as a wrong password ("Invalid user name or password") and the count of failed attempts does not change.
  - The audit trail lists the failed and successful attempts.
```

```fr
id: FR-UA-002
title: Warn before the inactivity and system log-out
brd: [BRD 1.001.1.2 (p.8), BRD 1.001.1.3 (p.8), BRD 2.001.1.2 (p.8), BRD 2.001.1.3 (p.8), BRD 3.001.1.2 (p.9), BRD 3.001.1.3 (p.9), BRD 4.001.1.2 (p.9), BRD 4.001.1.3 (p.9), UAM-NFR-34 (p.17)]
actor: All users
priority: "Must have (4.001.x has no flag in the BRD)"
screens: Every screen (session guard dialog)
description:
  - After 15 minutes without activity BIBS shows a warning; after 30 minutes without activity it signs the user out. Activity in any browser tab keeps all tabs signed in. Separately, BIBS warns 30 minutes before the fixed end of the session ("For your security the system signs you out in n minute(s)...").
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
  - [R3, "Warning SESSION_EXPIRY_WARNING_MINUTES (30) before the fixed session expiry.", Configurable, Parameter SESSION_EXPIRY_WARNING_MINUTES]
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
title: Sign in with the BDO account (single sign-on through EIAM)
brd: [UAM-NFR-11 (p.13), UAM-NFR-17 (p.14), UAM-NFR-33 (p.17)]
actor: All users; System
priority: Must have
screens: Login
description:
  - The BRD asks for sign-in with the Windows ID checked by BDO EUA (p.14), for LDAP or Active Directory authentication (p.13) and for integration with single sign-on (SSO) or LDAP (p.17). BDOI named EIAM, BDO's Enterprise Identity Access Management on Microsoft Entra ID, as the sign-in of BDO users for Drop 0 (decisions of 26 September 2026).
  - Single sign-on is part of BIBS, with OpenID Connect and with SAML 2.0. The sign-in mode parameter AUTH_MODE selects LOCAL (password held in BIBS), DIRECTORY (password checked by EUA), OIDC or SAML (single sign-on). In OIDC or SAML mode the Login page shows **Sign in with** the name of the BDO identity provider; the user signs in at EIAM, and BIBS checks the answer of the provider and opens the session with the user's BIBS roles. BIBS keeps no password of such a user.
  - "In single sign-on, the password, the lock-out and the second factor belong to EIAM (Entra ID); the BIBS inactivity warning and sign-out, the session log and the audit stay. Named break-glass System Administrators keep a local password and the BIBS second factor, so that BIBS can be administered when EIAM is not available (CLR-UA-24)."
  - Switching to single sign-on is configuration, not a change request; it needs BIBS registered in EIAM for each environment by BDOI IT (CLR-UA-01). Until then SIT uses LOCAL mode. The protocol, the registration, the claims and the session settings are in the Authentication and Identity Integration Requirements v1.0, 03-Oct-2026 (R9), reviewed by BDOI IT.
  - "Proposed rules for confirmation: EIAM replaces the EUA and LDAP sign-in (CLR-UA-21); the account returned by EIAM is matched to the BIBS user by the Windows ID, or by the user principal name (UPN) where the Windows ID is not released (CLR-UA-22); OpenID Connect is the protocol, SAML 2.0 the fall-back (CLR-UA-01); roles stay in BIBS (CLR-UA-26)."
preconditions:
  - "AUTH_MODE = OIDC or SAML, with BIBS registered in EIAM; the user exists in BIBS, is active and carries the Windows ID or UPN that EIAM returns (FR-UA-052)."
main_flow:
  - The user opens BIBS and clicks **Sign in with** the BDO identity provider on the Login page.
  - EIAM signs the user in with the BDO account (and its second factor) and returns the user to BIBS.
  - BIBS checks the answer of EIAM, finds the active BIBS user that matches the returned identity and opens the session with the user's BIBS roles; the sign-in is audited with the sign-in method.
alternate_flows:
  - The identity is not linked to an active BIBS user. BIBS refuses the sign-in with the message below and audits the attempt.
  - EIAM cannot complete the sign-in or is not available. BIBS shows the message below; a break-glass System Administrator signs in locally with the password and the second factor (CLR-UA-24).
  - DIRECTORY mode (EUA), if BDOI keeps it (CLR-UA-21). The user enters the Windows ID and password; BIBS passes them to EUA; on refusal BIBS shows the EUA message, counts the failed attempt and applies the lock-out.
rules:
  - [R1, "Sign-in mode LOCAL, DIRECTORY, OIDC or SAML; the identity provider, its registration and the claim that identifies the user are set per environment.", Configurable, Parameter AUTH_MODE and the sign-in settings of the environment]
  - [R2, "Authorisation (roles and permissions) stays in BIBS in every mode; no group of the identity provider is mapped to a BIBS role, and an external ACL is on hold (UQ14, CLR-UA-26).", Fixed, "-"]
  - [R3, "In OIDC or SAML mode only the named break-glass System Administrators sign in with a local password, always with the second factor; every such sign-in is alerted to Information Security (CLR-UA-24).", Configurable, Break-glass administrators of the environment]
  - [R4, "The returned identity is matched to the Windows ID held on the user, or to the user principal name where the Windows ID is not released (proposed, CLR-UA-22).", Configurable, The sign-in settings of the environment]
validations:
  - [The identity is not linked to an active user, "Your sign-in was accepted by your organisation, but it is not linked to an active user of this system. Contact your administrator.", SSO_NOT_LINKED]
  - [The identity provider could not complete the sign-in, "Your organisation’s sign-in service could not complete the sign-in. Try again in a moment.", SSO_PROVIDER_ERROR]
  - [Single sign-on not set up in the environment, "Single sign-on is not available. Contact your administrator.", SSO_NOT_CONFIGURED]
  - [DIRECTORY mode - the directory refuses the log-on, The message returned by the directory, AUTHENTICATION_FAILED]
notifications:
  - "A sign-in of a break-glass administrator in OIDC or SAML mode alerts Information Security."
audit:
  - "As FR-UA-001, with the sign-in method (LOCAL, DIRECTORY, OIDC or SAML)."
acceptance:
  - In OIDC mode a user signs in with the BDO account at EIAM and BIBS opens the session with the user's BIBS roles; BIBS stores no password for the user.
  - An identity that is not linked to an active BIBS user is refused with the message SSO_NOT_LINKED and the attempt is audited.
  - In OIDC mode a break-glass System Administrator signs in with the local password and the second factor; another user cannot sign in with a local password.
  - The menu after a single sign-on is that of the user's BIBS roles, whatever groups the identity provider holds.
```

```fr
id: FR-UA-004
title: Log out and keep a session log
brd: [UAM-NFR-35 (p.17), UAM-NFR-15 (p.14)]
actor: All users; System Administrator
priority: Must have
screens: User menu (Log Out); Administration > Users (status Online)
description:
  - "Log-in and failed log-in are audited. **Log Out** ends the session in BIBS and records LOGOUT in the audit trail. Every session is kept in a session log - user, start, last activity, end and end reason (log-out, inactivity, session expiry, ended by an administrator, account locked). The user list shows a user as Online while a session is open (UQ13)."
preconditions:
  - "The user is signed in."
main_flow:
  - The user clicks **Log Out**.
  - BIBS ends the session, records LOGOUT and shows the log-in screen.
alternate_flows:
  - A page left open after the session ended is used again. BIBS refuses the action and asks the user to log in.
  - Single sign-on (OIDC or SAML). After ending the BIBS session, BIBS sends the user to the EIAM sign-out page, so the BDO session ends too (proposed, CLR-UA-23); after an inactivity sign-out the next sign-in goes through EIAM again.
rules:
  - [R1, "Last activity is updated at most every 5 minutes.", Fixed, "-"]
  - [R2, "Single session per device is not enforced; the session log prepares it (UQ09).", Fixed, "-"]
  - [R3, "In single sign-on, Log Out also ends the user's EIAM session (proposed rule, CLR-UA-23; BDOI IT confirms that this is acceptable to the other BDO applications that share the session).", Configurable, The sign-in settings of the environment]
validations: []
notifications:
  - "None."
audit:
  - "LOGOUT audited; the session log is kept with the audit trail."
acceptance:
  - After Log Out, the audit log report shows the log-out with user and time.
  - The Users screen shows a signed-in user as Online.
  - In single sign-on, after Log Out the user is at the EIAM sign-out page, and opening BIBS again asks for the BDO sign-in.
```

```fr
id: FR-UA-005
title: Enforce the password policy and self-service reset
brd: [UAM-NFR-31 (p.17), UAM-NFR-36 (p.17), UAM-NFR-37 (p.17)]
actor: All users (local accounts); System Administrator
priority: Must have
screens: My Profile (Change password, Details); Login (Forgot password?); Administration > Users (Reset password)
description:
  - "Users change their own password on My Profile; the password has at least 10 characters with upper and lower case letters, a digit and a symbol. The System Administrator resets a password and BIBS shows a temporary password once."
  - "BIBS refuses the last PASSWORD_HISTORY_COUNT passwords, requires a change after PASSWORD_MAX_AGE_DAYS, refuses a second change within PASSWORD_MIN_AGE_DAYS and forces a change at the first log-in after a reset. **Forgot password?** e-mails a single-use link valid for 30 minutes. Users update their own e-mail and mobile number on My Profile (UQ17)."
  - These rules apply to local accounts (LOCAL mode and the break-glass administrators). With single sign-on through EIAM, or with EUA, the password belongs to BDO (UQ08, UA-Q16).
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
  - [New password too weak, "Still needed: At least 10 characters; An upper-case letter; A lower-case letter; A digit; A symbol (on the screen, as the user types)", "-"]
  - [Password used before, You used this password recently. Choose another one, PASSWORD_REUSED]
  - [Changed too soon, You changed your password less than a day ago, PASSWORD_CHANGED_TOO_SOON]
notifications:
  - "PASSWORD_EXPIRY_NOTICE to users whose password expires within 7 days (job, 06:00 daily)."
audit:
  - "Password change and reset recorded (no password value)."
acceptance:
  - A new password equal to one of the last 10 is refused.
  - After an administrator reset, the user must change the password at the next log-in.
  - The reset link works once and not after 30 minutes.
```

```fr
id: FR-UA-006
title: Deactivate dormant users
brd: [UAM-NFR-06 (p.12), UAM-NFR-09 (p.13)]
actor: System (daily job); Requestor (told of the result)
priority: Should have
screens: Access Requests (the requests of the job); Notifications
description:
  - "Every night BIBS deactivates the users who have not signed in for UAM_DORMANT_DAYS days (90), counted from the last sign-in, or from the creation or the last reactivation when later. UAM_DORMANT_NOTICE_DAYS days before (7) the user is told to sign in. Each deactivation is a Deactivate user request with the reason \"No sign-in for the dormancy period\", approved and applied by the system, so it has the request history, the change log and the notice to the user like any other request. The holders of the System Administrator profile are never deactivated this way."
preconditions:
  - "UAM_DORMANT_DAYS is more than 0."
main_flow:
  - The job finds the active users without a sign-in for the dormancy period.
  - For each, BIBS raises, approves and applies a Deactivate user request as the system.
  - BIBS tells the user, and tells the holders of the request right which users were deactivated.
alternate_flows:
  - Notice day. A user whose deactivation date is UAM_DORMANT_NOTICE_DAYS days away is told to sign in, in the app and by e-mail.
  - A deactivated user who needs access again is reactivated by a request (FR-UA-014).
rules:
  - [R1, "Dormancy period 90 days; 0 switches the job off.", Configurable, Parameter UAM_DORMANT_DAYS]
  - [R2, "Notice 7 days before; 0 sends no notice.", Configurable, Parameter UAM_DORMANT_NOTICE_DAYS]
  - [R3, "The System Administrator profile is never deactivated by the job, so the administration stays reachable.", Fixed, "-"]
validations: []
notifications:
  - "UAM_DORMANT_WARNING to the user on the notice day (in the app and by e-mail); UAM_ACCESS_CHANGED to the deactivated user; UAM_DORMANT_DEACTIVATED to the holders of the request right."
audit:
  - "The request of the job, its history and the change log DISABLE_USER, done by SYSTEM."
acceptance:
  - A user without a sign-in for 91 days is deactivated by the job with a request whose reason is "No sign-in for the dormancy period".
  - A user whose deactivation date is 7 days away receives the notice and stays active.
```

## User access requests (Requestor)

```fr
id: FR-UA-010
title: Draft, edit, cancel and submit an access request
brd: [BRD 1.002.1.1 (p.8), BRD 1.002.1.1.4-1.002.1.1.5 (p.8), BRD 1.002.1.2-1.002.1.4 (p.8), BRD 1.003.1.1 (p.8), BRD 1.003.1.1.5-1.003.1.1.6 (p.8), BRD 1.003.1.2-1.003.1.4 (p.8), BRD 1.004.1.1 (p.8), BRD 1.004.1.1.2-1.004.1.1.3 (p.8), BRD 1.004.1.2-1.004.1.4 (p.8), BRD 1.005.1 (p.8), BRD 1.005.1.1.1-1.005.1.1.2 (p.8), BRD 1.005.1.2-1.005.1.4 (p.8)]
actor: Requestor
priority: "Must have (the 'add / save remarks' lines have no flag in the BRD)"
screens: User Access > Access Requests; New Request; Edit Request
description:
  - "The Requestor clicks **New Request** on Access Requests and fills the request. Every request type (enrol, modify, deactivate, reactivate, and the group-profile types of section 4.4) can be saved as a DRAFT. Only its creator sees and edits a draft. The creator cancels a draft (CANCELLED, kept, never deleted) or submits it. Remarks - justification, reason for deactivation or reactivation - are saved with the draft, and every remark is also kept in the request history. A request gets the number AR-yyyy-nnnnnn."
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
  - [Justification blank, Enter the justification, ACCESS_JUSTIFICATION]
  - [Another request for the user is pending, A request for user <user> is already waiting for approval, ACCESS_REQUEST_PENDING]
  - [Edit of a draft by another user, "Only the creator can edit request <request no>", ACCESS_NOT_REQUESTER]
  - [Cancel without reason, Enter the reason for the cancellation, ACCESS_CANCEL_REASON]
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
screens: New Request (type Enrol new user)
description:
  - "A Create user request carries the user name, full name, e-mail, home branch and group profiles, and also the Windows ID, the business unit group, the user level, the authorisation limit and an optional effective date (NFR p.13-14). The user ID follows the BDOI format (a letter followed by nine digits, parameter USER_ID_PATTERN, shown to the requester in words from USER_ID_FORMAT_TEXT). Only active group profiles are offered. On approval BIBS creates the user and shows a temporary password once to the approver."
preconditions:
  - "The user has UAM_ENROLL (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Enrol new user**.
  - The Requestor enters the new user's data and selects the group profiles.
  - The Requestor completes the common part (FR-UA-010) and submits.
alternate_flows:
  - In DIRECTORY mode (FR-UA-003) no temporary password is created; the user signs in with the Windows ID.
rules:
  - [R1, "User ID pattern (default ^[a-zA-Z][0-9]{9}$, to confirm UQ05); existing service and SIT/UAT users are exempt.", Configurable, Parameter USER_ID_PATTERN]
  - [R2, "Windows ID is unique across users.", Fixed, "-"]
  - [R3, "At least one active group profile.", Fixed, "-"]
validations:
  - [User ID with wrong characters or length, "The user name has 3 to 50 letters, digits, dots, dashes or underscores", ACCESS_USERNAME]
  - [User ID not in the BDOI format, "The user ID must be <user ID format in words>", ACCESS_USER_ID_FORMAT]
  - [User already exists, User <user> already exists, ACCESS_USER_EXISTS]
  - [Full name blank, Enter the full name of the new user, ACCESS_FULL_NAME]
  - [No group profile, Select at least one role, ACCESS_ROLES]
  - [Unknown group profile, "Unknown role(s): <codes>", ACCESS_UNKNOWN_ROLE]
  - [Inactive group profile, "Group profile <name> is not active", ACCESS_ROLE_INACTIVE]
  - [Profiles one user may not hold together, "One user may not hold both <profile> and <profile> (separation-of-duties rule <rule>)", ACCESS_SOD_CONFLICT]
  - [Windows ID already used, "Windows ID <id> belongs to another user", ACCESS_WINDOWS_ID_IN_USE]
fields_screen: New Request (Enrol new user)
fields:
  - [User ID, Text, "Yes", "-", "USER_ID_PATTERN; unique"]
  - [Windows ID, Text, Conditional, "-", "Unique; required in DIRECTORY mode"]
  - [Full name, Text, "Yes", "-", Up to 120 characters]
  - [E-mail, Text, "No", "-", Valid e-mail; up to 120 characters]
  - [Home branch, List, "No", Branches, "-"]
  - [Business unit group, List, "No", LOV UAM_BUSINESS_UNIT, Values from BDOI (UQ05)]
  - [User level, List, "No", LOV UAM_USER_LEVEL, Values from BDOI (UQ05)]
  - [Group profiles, Multi-select, "Yes", Active roles, "At least one; no pair of an active separation-of-duties rule (FR-UA-053)"]
  - [Authorisation limit, Amount, "No", "-", "Zero or more, 2 decimals; blank = no limit"]
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
screens: New Request (type Modify user)
description:
  - "A Modify user request changes the user data (full name, e-mail, home branch, business unit group, user level, Windows ID, authorisation limit) and / or the group profiles in one request. The Requestor searches the user by user ID, Windows ID or name; the form shows the current values next to the new ones. A Change roles request, which replaces the group profiles only, stays available."
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
  - [Nothing changed, The request does not change the user, ACCESS_NOTHING_CHANGED]
  - [Own roles, You cannot change your own roles, SELF_ROLE_CHANGE]
fields_screen: New Request (Modify user)
fields:
  - [User, Look-up, "Yes", "Users (user ID, Windows ID, name)", Existing user]
  - [Current / new values, Pairs, "No", "Full name, e-mail, home branch, business unit group, user level, Windows ID, authorisation limit", At least one change]
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
screens: New Request (type Deactivate user)
description:
  - "A Disable user request disables the account on approval; the user can no longer sign in. Only enabled users are offered. An optional reason code (list UAM_DEACTIVATION_REASON) is kept with the remarks, and an effective date allows a future deactivation, for example on the last working day."
preconditions:
  - "The user has UAM_DEACTIVATE (or ACCESS_REQUEST)."
main_flow:
  - The Requestor chooses **Deactivate user** and searches the user.
  - The Requestor selects the reason and enters the remarks.
  - The Requestor completes the common part and submits.
rules:
  - [R1, "A deactivated user keeps the group profiles so a reactivation restores them.", Fixed, "-"]
  - [R2, "Reasons - Resigned, Transferred, Long leave, Security, Others (to confirm with BDOI); No sign-in for the dormancy period is used by the dormant-user job (FR-UA-006).", Configurable, LOV UAM_DEACTIVATION_REASON]
validations:
  - [User not found, User <user> does not exist, ACCESS_UNKNOWN_USER]
  - [User already disabled, "User <user> is already deactivated", ACCESS_USER_ALREADY_INACTIVE]
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
screens: New Request (type Reactivate user)
description:
  - "An Enable user request enables a disabled account on approval. Only disabled or locked users are offered, and the request can also unlock a locked account. The BRD numbers this function one level up (BRD 1.005 and 1.005.1.1 for the sub-steps); the traceability keeps the printed IDs."
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
  - [User already active, "User <user> is already active", ACCESS_USER_ALREADY_ACTIVE]
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
screens: New Request (Approver)
description:
  - "The Requestor selects the approver from a drop-down of eligible approvers - enabled holders of ACCESS_APPROVE, excluding the requester and the user the request is about. The notice goes to that approver, and the request appears in that approver's My Approvals and \"Assigned to me\". Other approvers see it only when UAM_ANY_APPROVER is true."
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
  - [Approver not selected, Select the approver, ACCESS_APPROVER_REQUIRED]
  - [Approver not eligible, "<user> cannot approve this request", ACCESS_APPROVER_NOT_ELIGIBLE]
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
screens: Access Requests (My Requests); Edit Request
description:
  - "A request returned by the approver (FR-UA-033) is RETURNED. The Requestor receives a notification with the return remarks, edits the data, adds a correction remark and re-submits it to the same approver or a newly chosen one. The history keeps every round."
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
  - [Correction remark blank, Enter the correction remarks, ACCESS_CORRECTION_REMARKS]
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
screens: Access Requests (My Requests)
description:
  - "The Requestor searches the request by status, type, text (user, group profile or request number), requester, approver and date range, and cancels a PENDING or RETURNED request with a mandatory reason. The request becomes CANCELLED, leaves the approver's My Approvals and the approver is notified. An approved request with a future effective date (SCHEDULED) can be cancelled before its date (UQ06)."
preconditions:
  - "The request is PENDING, PENDING_SECOND, RETURNED or SCHEDULED; the user is its creator and has UAM_CANCEL (or ACCESS_REQUEST)."
main_flow:
  - The Requestor searches the request and opens it.
  - The Requestor clicks **Cancel Request** and enters the reason.
  - BIBS cancels the request and notifies the approver.
rules:
  - [R1, "A cancelled request is kept with its reason; it cannot be re-opened.", Fixed, "-"]
validations:
  - [Reason blank, Enter the reason for the cancellation, ACCESS_CANCEL_REASON]
  - [Request already decided, "Request <request no> is already <status>", ACCESS_REQUEST_DECIDED]
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
screens: Access Requests (My Requests); request detail (History)
description:
  - "The Access Requests screen lists requests with number, type, change, requested by, requested on, status and decided by, with filters by status, type and text; a row opens the request detail with a History tab of every event and remark. A Requestor sees his or her own requests on the tab My Requests; holders of ACCESS_APPROVE, USER_MANAGE or AUDIT_VIEW see all. The requester is notified of each decision."
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
screens: User Access > Bulk Request
description:
  - The Requestor downloads the template, fills one row per user - action (enrol, modify, deactivate, reactivate), user ID, Windows ID, name, e-mail, branch, business unit group, user level, group profiles, effective date, remarks - and attaches it. BIBS validates every row and shows a validation report before the batch is created.
  - A valid file creates a draft batch with one line request per row. The Requestor adds the batch remarks, edits the draft (replaces the file or removes lines), cancels it or submits it to the chosen approver. On approval every line is applied in its own transaction; failures are listed and the other lines stand.
preconditions:
  - "The user has UAM_ENROLL or UAM_MODIFY (and the permissions of the actions in the file), and the bulk upload right BULK_PROCESS. BULK_PROCESS is granted to the Requestor, the Business Administrator and the System Administrator."
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
  - [Row with an unknown action, "Action <action> is not valid", ACCESS_BULK_ACTION]
  - [Row fails a request check, "The message of the check of a single request (for example User <user> already exists)", "Code of the check"]
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
screens: New Request (Effective date); Access Requests (status SCHEDULED)
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
  - [R2, "Temporary access with an end date follows BDOI's answer to UQ06 (chapter 22, CLR-UA-13).", Fixed, "-"]
validations:
  - [Effective date in the past, The effective date cannot be before today, ACCESS_EFFECTIVE_DATE]
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
screens: My Approvals; Access Requests (tab Assigned to Me); request detail
description:
  - "Pending requests appear in My Approvals of the chosen approver (never the requester's own) and on the Access Requests screen with status PENDING. The approver receives a notification for each request assigned to him or her and filters the list with **Assigned to me**. The detail shows the current values next to the requested ones (user data and group profiles), the members affected (group profiles) and the history."
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
screens: Request detail (Approve and Apply)
description:
  - "**Approve and Apply** records the decision and applies the change - creates the user, changes the user data or group profiles, disables or enables the user - and notifies the requester. For a new user BIBS shows a temporary password once; it is never stored in clear. Neither the requester nor the subject user can approve. After approval, a request with a risk flag goes to the second approver (FR-UA-034); a request with a future effective date becomes SCHEDULED (FR-UA-020); a group-profile request goes to the next approver or to the System Administrator (FR-UA-044, FR-UA-045). Every applied attribute is logged with from and to values (FR-UA-064), and the affected user is notified."
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
  - [R4, "An enrolment is approved on its own, from its page: Approve Selected on Access Requests cannot select it and My Approvals refuses it, so its temporary password is always shown to the approver.", Fixed, "-"]
validations:
  - [Approver is the requester, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
  - [Approver is the subject user, You cannot decide a request about your own access, ACCESS_SUBJECT_DECIDES]
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
screens: Request detail (Reject)
description: The approver enters the rejection reason in Comment ("Mandatory to reject; sent to the requester") and clicks **Reject**. BIBS records the decision, notifies the requester and changes nothing. The reason is also written to the request history.
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
screens: Request detail (Return)
description:
  - "The approver clicks **Return**, enters the remarks (reason for return) and saves. The request becomes RETURNED, the remarks are saved in the history and the requester is notified to correct it (FR-UA-016). The BRD uses ID 2.002.7.1 twice, for \"add remarks\" and \"save remarks\"."
preconditions:
  - "As FR-UA-031; also PENDING_SECOND for the second approver."
main_flow:
  - The approver enters the remarks and clicks **Return**.
  - BIBS sets the request to RETURNED and notifies the requester.
rules:
  - [R1, "Return remarks are mandatory.", Fixed, "-"]
validations:
  - [Remarks blank, Enter the remarks for the requester, ACCESS_RETURN_REASON]
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
screens: My Approvals; Access Requests (tab Second Approval)
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
  - [Second approver is the first approver, The second approval is given by another approver, ACCESS_SECOND_SAME_APPROVER]
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
screens: User Access > Group Profile Requests (New); permission picker by area and action
description:
  - "The Business Administrator requests a new group profile - code, name, description, privilege level and the permissions, picked by area and action class. The request follows the lifecycle of FR-UA-010 (draft, edit, cancel, submit, remarks) and the approvers of FR-UA-044. After the last approval it waits for the System Administrator (FR-UA-045)."
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
  - [No permission, Select at least one permission, ACCESS_NO_PERMISSION]
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
screens: Group Profile Requests (Modify); Change role permissions fields
description:
  - "A Change group profile request names the profile, the permissions to add and to remove, any change of its name, description and privilege level, and the justification. The request has drafts and approvers in order, and after approval waits for the System Administrator (FR-UA-045), unless UAM_ROLE_APPLY_ON_APPROVAL is true (UQ03). The difference is applied to the profile as it is at implementation, so concurrent changes are not lost."
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

> [!DECISION] For confirmation (chapter 22, CLR-UA-12)
> The BRD-11 diagram (p.6) has the System Administrator implement group-profile changes after approval. BIBS applies an approved change of role permissions at the implementation step (UAM_ROLE_APPLY_ON_APPROVAL = false), for every module, including Product Maintenance (BRD-3 PMADD05); BDOI confirms under UQ03 (PQ17 answered, R4).

```fr
id: FR-UA-042
title: Request the deactivation of a group profile
brd: [BRD 3.002.3 (p.9), BRD 3.002.3.1 (p.9), BRD 3.002.3.2 (p.9), BRD 3.002.3.4 (p.9), BRD 3.002.3.5 (p.9), BRD 3.002.3.6 (p.9), BRD 3.002.3.7 (p.9), BRD 3.002.3.8 (p.9)]
actor: Business Administrator
priority: "Must have (3.002.3.2, 3.7, 3.8 have no flag in the BRD)"
screens: Group Profile Requests (Deactivate)
description:
  - "The Business Administrator requests the deactivation of an active profile. The request lists the members of the profile. When implemented, the profile is inactive - it grants nothing, is not offered on requests and keeps its permissions for a reactivation. Whether a profile with active members may be deactivated, or members must be moved first, is UQ16."
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
  - [Profile already inactive, "Group profile <code> is already inactive", ACCESS_ROLE_ALREADY_INACTIVE]
  - [System Administrator profile, Group profile SYSADMIN cannot be deactivated, ACCESS_ROLE_PROTECTED]
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
screens: Group Profile Requests (Reactivate)
description: The Business Administrator requests the reactivation of an inactive profile, searched from the drop-down of inactive profiles. When implemented, the profile is active again with its last permissions, and its members regain them.
preconditions:
  - "The user has UAM_GROUP_REQUEST."
main_flow:
  - The Business Administrator chooses Reactivate and selects an inactive profile.
  - The Business Administrator chooses the approvers, enters the remarks and submits.
rules:
  - [R1, "The profile is restored with the permissions it had at deactivation.", Fixed, "-"]
validations:
  - [Profile already active, "Group profile <code> is already active", ACCESS_ROLE_ALREADY_ACTIVE]
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
screens: Group Profile Requests (Approvers); My Approvals
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
  - [No approver, Add at least one approver, ACCESS_APPROVER_REQUIRED]
  - [Same approver twice, "<user> is already an approver of this request", ACCESS_APPROVER_TWICE]
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
screens: Access Requests (tab For Implementation); Administration > Roles & Permissions (Implement Request)
description:
  - "The System Administrator maintains group profiles only by implementing approved requests. The For Implementation tab lists approved group-profile requests; **Implement Request** opens the Roles screen with the approved change, and saving it applies the change and sets the request to IMPLEMENTED. Direct profile edits are refused unless the audited emergency parameter UAM_DIRECT_ROLE_EDIT is true, which raises alert UAM_DIRECT_ROLE_EDIT."
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
  - [Direct role edit without a request, Roles are changed through an approved group-profile request; implement the request instead, ROLE_EDIT_BY_REQUEST]
  - [Implementer is the requester, A request is implemented by someone other than its requester, ACCESS_IMPLEMENTER_IS_REQUESTER]
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
screens: Administration > Roles & Permissions
description: BIBS provides the group profiles of the BRD personas - UAM_REQUESTOR (Requestor), UAM_APPROVER (Approver) and UAM_SECOND_APPROVER - and grants the user access permissions to BUSINESS_ADMIN (Business Administrator) and SYSADMIN, as in section 3.3. Later changes to these profiles are group-profile requests (FR-UA-040 to FR-UA-045).
preconditions:
  - "None (provided with the system)."
main_flow:
  - The System Administrator opens Roles & Permissions.
  - BIBS lists the delivered profiles with their permissions.
rules:
  - [R1, "Roles holding ACCESS_REQUEST also receive the type-specific request permissions, so their users keep their functions.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The permissions delivered with the profiles are the starting point; every later change is in the change log."
acceptance:
  - The Requestor, Approver and Business Administrator profiles exist with the permissions of section 3.3.
```

```fr
id: FR-UA-051
title: Assign functions to a group profile
brd: [BRD 4.002.2 (p.9), BRD 4.002.2.1 to 4.002.2.7 (p.9-10), BRD 4.002.2.8 (p.10), BRD 4.002.2.9 (p.10), BRD 4.002.2.10 (p.10), UAM-NFR-32 (p.17)]
actor: System Administrator (through approved group-profile requests)
priority: Must have
screens: Roles & Permissions; User Access Matrix
description:
  - "Every BIBS screen, button and action requires a permission; group profiles are bundles of permissions; the User Access Matrix shows them by permission and by area / action class and exports to Excel. Each access-maintenance function of BRD 4.002.2 has its own permission (section 3.2), so it can be assigned to any profile, and every permission has an area and action class, so the matrix and the group-profile report show a module for every task. A request is accepted with the type-specific permission or with ACCESS_REQUEST."
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
screens: Administration > Users; Lists of Values (UAM_BUSINESS_UNIT, UAM_USER_LEVEL)
description:
  - "Users are listed with user name, user ID, Windows ID, full name, business unit group, user level, group profiles, authorisation limit, last login and status (Active, Disabled, Locked or Online). A user can hold several group profiles and a profile many users. User IDs follow USER_ID_PATTERN. Users are created and changed through requests (\"Raise request\"); only the System Administrator edits directly, in an emergency (UAM_DIRECT_ROLE_EDIT). Unlock and password reset are direct administrator actions and are logged."
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

```fr
id: FR-UA-053
title: Keep profiles that one user may not hold apart (separation of duties)
brd: [UAM-NFR-40 (p.17), UAM-NFR-06 (p.12)]
actor: Business Administrator (maker); Information Security Officer (checker)
priority: Must have
screens: User Access > Separation of Duties; My Approvals
description:
  - "BIBS keeps the requester, the approver and the implementer of a request apart. The Business Administrator also records the pairs of group profiles that one user may not hold together, with the reason. Each new rule and each deactivation waits for the authorisation of Information Security (maker-checker). Every user request and every line of a bulk request that would give a user both profiles of an active rule is refused, on submission and again at approval."
preconditions:
  - "The maker has UAM_SOD_MAINTAIN; the checker has UAM_SOD_AUTHORIZE."
main_flow:
  - The Business Administrator opens Separation of Duties, chooses **New Rule**, selects the two group profiles and enters the reason.
  - BIBS saves the rule as pending and tells the Information Security Officers.
  - The Information Security Officer authorises the rule from the screen or My Approvals; the rule is active at once.
alternate_flows:
  - Rejection. The rule is closed and never applies.
  - Deactivation. The Business Administrator asks for it; the rule stays active until Information Security authorises the deactivation.
rules:
  - [R1, "The maker of a rule or of its deactivation never authorises it.", Fixed, "-"]
  - [R2, "One rule per pair of profiles; the order of the two profiles does not matter.", Fixed, "-"]
  - [R3, "The rules are checked on single requests, bulk lines and again at approval; existing users are not changed by a new rule and are found with the reports.", Fixed, "-"]
validations:
  - [Same profile twice, Choose two different group profiles, SOD_SAME_PROFILE]
  - [Rule exists, A rule for these two group profiles already exists, SOD_RULE_EXISTS]
  - [Maker authorises, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
  - [Request breaks a rule, "One user may not hold both <profile> and <profile> (separation-of-duties rule <rule>)", ACCESS_SOD_CONFLICT]
fields_screen: Separation of Duties (New Rule)
fields:
  - [Group profile, List, "Yes", Active group profiles, "-"]
  - [May not be held with, List, "Yes", Active group profiles, Different from the first]
  - [Reason, Long text, "Yes", "-", Up to 500 characters]
notifications:
  - "UAM_SOD_TO_AUTHORIZE to the holders of UAM_SOD_AUTHORIZE for each new rule and each deactivation."
audit:
  - "Creation, deactivation request, authorisation and rejection of each rule in the audit trail."
acceptance:
  - A rule on the Requestor and Approver profiles, once authorised, refuses a request that gives one user both, with the names of the two profiles.
  - The Business Administrator who created the rule cannot authorise it.
```

```fr
id: FR-UA-054
title: Second approval of the security settings
brd: [UAM-NFR-40 (p.17), UAM-NFR-16 (p.14)]
actor: System Administrator (maker); Information Security Officer (checker)
priority: Must have
screens: Administration > System Parameters; My Approvals
description:
  - "A change of a security setting (category Security - sign-in, lock-out, password, session and access settings, including the emergency direct edit of profiles UAM_DIRECT_ROLE_EDIT) is kept as pending and applies only when a holder of SECURITY_PARAMETER_APPROVE other than the requester approves it. The other parameters change at once, with an audit of the old and new value."
preconditions:
  - "The maker has SYSTEM_PARAMETER_MANAGE; the checker has SECURITY_PARAMETER_APPROVE."
main_flow:
  - The System Administrator opens System Parameters, changes a security setting and saves.
  - BIBS keeps the new value as waiting for approval, shows it under the current value and tells the approvers.
  - The Information Security Officer opens the setting from System Parameters or My Approvals and approves; the value applies at once.
alternate_flows:
  - Rejection. The approver rejects, or the requester withdraws the change; the current value stays.
rules:
  - [R1, "One pending change per setting; the requester never approves it.", Fixed, "-"]
  - [R2, "The settings of the Security category need the second approval.", Fixed, "-"]
validations:
  - [Change already pending, "A change of <setting> already waits for approval; approve or reject it first", PARAMETER_CHANGE_PENDING]
  - [Requester approves, A change cannot be approved by the user who asked for it, MAKER_CHECKER_VIOLATION]
notifications:
  - "UAM_PARAMETER_TO_APPROVE to the holders of SECURITY_PARAMETER_APPROVE."
audit:
  - "The request, the approval with the requester and the change with the old and new value, or the rejection, in the audit trail."
acceptance:
  - A change of LOGIN_MAX_FAILED_ATTEMPTS by the System Administrator applies only after the Information Security Officer approves it.
  - Opening the emergency direct edit (UAM_DIRECT_ROLE_EDIT = true) needs the same approval.
```

## Reports and logs

```fr
id: FR-UA-060
title: User Access Report
brd: [BRD 3.003.1 (p.9), BRD 3.003.1.1 (p.9), BRD 3.003.1.1 (2nd) (p.9), BRD 3.003.1.2 to 3.003.1.6 (p.9), UAM-NFR-10 (p.13), UAM-NFR-41 (p.18)]
actor: Approver; Business Administrator; System Administrator; Information Security Officer; Auditor (UAM_REPORT_VIEW)
priority: Must have
screens: User Access > User Access Reports; Reports (category Control & Audit)
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
  - [As-of date in the future, The as-of date cannot be in the future, AS_OF_IN_FUTURE]
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
screens: User Access Reports
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
screens: User Access Reports
description:
  - "The report lists the members of each profile - user name and user ID, added by and date, modified by and date - grouped by profile (sample C, p.18, titled \"User Group Profile Report\" in the BRD). The User Access Matrix also shows the number of enabled users per profile."
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
screens: User Access Reports; Administration > Audit Trail (CTL-AUDIT, exists)
description:
  - "The User Access Audit Log lists, for a date range, each access activity - date, activity, from, to, done by, approved by, request number (sample D, p.18) - from the access-change log and the request history. Log-ins, failed log-ins and log-outs can be included. The general Audit Trail report (CTL-AUDIT) lists audit entries by date range, user and entity type, and exports to PDF, Excel and CSV."
preconditions:
  - "The user has UAM_REPORT_VIEW (or AUDIT_VIEW)."
main_flow:
  - The user selects the report and the date range, user and activity.
  - BIBS generates the log; the user exports it.
rules:
  - [R1, "Activities - request created, approved, rejected, returned, cancelled, implemented; user created, modified, deactivated, reactivated, unlocked; password reset; group profile created, changed, deactivated, reactivated; log-in, failed log-in, log-out.", Fixed, "-"]
validations:
  - [Date to before date from, The end date must be on or after the start date, DATE_RANGE_REVERSED]
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
title: Keep an access-change log that no user can change
brd: [BRD 4.003.1 (p.10), UAM-NFR-09 (p.13), UAM-NFR-22 (p.14)]
actor: System
priority: Must have
screens: Reports of section 6; request History tab
description: Every change is in the audit trail as summary text, and every applied change also records one line per attribute in the access-change log (time, user or profile, activity, attribute, from value, to value, request number, done by, approved by) together with the change, so that a change is never applied without its log lines. No user can change or delete it. The reports read it.
preconditions:
  - "None."
main_flow:
  - A change is applied (approval, scheduled application, implementation, unlock, reset).
  - BIBS applies the change and records its log lines together.
rules:
  - [R1, "Entries can only be added; no user, including the System Administrator, can change or delete them.", Fixed, "-"]
  - [R2, "Kept at least as long as the audit trail; QPS retention values are UQ12.", Configurable, Retention rules]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - An attempt to change or delete a log entry outside the screens is refused.
  - Each attribute of a modified user has one row with from and to values and the request number.
```

## Notifications

```fr
id: FR-UA-070
title: Notify requesters, approvers and affected users
brd: [BRD 1.006.1.1 (p.8), BRD 1.008.1.4 (p.8), BRD 2.002.1 (p.8), UAM-NFR-39 (p.17)]
actor: System
priority: Must have
screens: Notifications (bell); e-mail
description:
  - "The notice of a submitted request goes to the chosen approver; the decision goes to the requester, each with a link to the request. The requester is also notified of a return and of the application of a scheduled change; the approver of a cancellation; the second approvers and the System Administrators of the requests waiting for them; the affected user (in the app and by e-mail) when his or her access changes, and each member of a group profile that is deactivated or reactivated (NFR 10, p.17); the users of the dormant-user deactivation (FR-UA-006); the authorisers of the separation-of-duties rules (FR-UA-053) and the approvers of the security settings (FR-UA-054)."
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
screens: Administration > Jobs (run history); e-mail
description:
  - "Batch jobs are scheduled, run on demand from the screen, monitored and re-run; each run is logged with success or failure. A failed run raises the in-app alert JOB_FAILURE and is e-mailed to the designated users in parameter JOB_FAILURE_RECIPIENTS, with the job, time and error."
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

Figure 2 shows the statuses of a user access request. On the screens the statuses are shown as Draft, Pending, Pending Second, Returned, Scheduled, Approved, Rejected, Cancelled, For Implementation and Implemented.

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

Figure 4 shows the interfaces. Every BIBS module reads the effective permissions of the signed-in user. Single sign-on through EIAM is in scope and is switched on by configuration once BIBS is registered in EIAM; the EUA and LDAP sign-in are proposed to be replaced by EIAM (CLR-UA-21); the external ACL is on hold. The protocol and provisioning details are in the Authentication and Identity Integration Requirements v1.0, 03-Oct-2026 (R9), reviewed by BDOI IT.

![Interfaces of User Access Maintenance (dashed = on hold or proposed to be replaced)](figures/brd11_integration.dot){width=16}

<!-- table: widths=3.8,1.8,7.6,2.8,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| EIAM (Microsoft Entra ID), Drop 0 | Out / In | The BDO identity provider of BIBS: sign-in with the BDO account through single sign-on (FR-UA-003); proposed rules for confirmation - the account is matched to the BIBS user by the Windows ID or the user principal name (CLR-UA-22), and Log Out also ends the EIAM session (CLR-UA-23). BDOI IT registers BIBS per environment (CLR-UA-01); details in R9 | NFR p.13-14, p.17 | IN SCOPE |
| UIDM-ISC (identity governance), Drop 0 | In / Out | Joiner, mover and leaver provisioning and access certification. Options: the IGA provisions user accounts while role changes stay BIBS requests (proposal), the IGA provisions users and roles, or aggregation only (IQ05, DCR-229, UA-Q18 to UA-Q21); the provisioning and aggregation interface is described in R9 | 1.001-1.009 (request rule, p.6) | OPEN |
| BDO EUA with Windows ID | Out / In | User ID and password passed at log-in; success or failure with message returned (sign-in mode DIRECTORY). Proposed to be replaced by EIAM (CLR-UA-21, UA-Q05) | NFR p.14 | OPEN |
| LDAP / Active Directory | Out / In | User ID and password checked against the BDO directory. Proposed to be replaced by EIAM, which holds the BDO directory accounts (CLR-UA-21, UA-Q05) | NFR 1.h (p.13) | OPEN |
| Single sign-on (OpenID Connect or SAML 2.0) | In | The sign-in of the BDO identity provider opens a BIBS session; part of BIBS and switched on by configuration (sign-in mode OIDC or SAML); EIAM is the provider (row above); OpenID Connect proposed, SAML 2.0 the fall-back (CLR-UA-01, UA-Q06) | Other BU NFR 4 (p.17) | IN SCOPE |
| External ACL | In | Authorisation by an external access-control list | NFR 1.i (p.13) | ON HOLD |
| All BIBS modules | Out | Effective permissions of the user (menus, buttons and system functions) | 4.002.2 | IN SCOPE |
| Portal users (BRD-8 Employee Benefits) | Out | External user requests provisioned on approval (decision D7). Dormant: BDOI drops the EB portal (drop plan item 2.4, IQ22); EXTERNAL requests stay refused | D7 (R4) | OUT |
| Notifications and e-mail | Out | Request, access-change and batch-failure notices | 1.006.1.1, 1.008.1.4, 2.002.1; NFR 10 | IN SCOPE |
| Bulk upload | In | Template file of access requests (Bulk Request) | 1.009 | IN SCOPE |
| Remote log server / syslog | Out | Application and error logs | NFR p.14 | ON HOLD |

> [!NOTE] Sign-in through EIAM
> Single sign-on with OpenID Connect and SAML 2.0 is part of BIBS: the BDO identity provider (EIAM on Microsoft Entra ID) is set for each environment and the sign-in mode switched to OIDC or SAML, without a change to the screens, users, group profiles or requests. BDOI IT registers BIBS in EIAM for each environment (CLR-UA-01); until then SIT uses local sign-in. The Windows ID is held on every user so that the EIAM account can be matched (CLR-UA-22). The protocol, the claims, the registration and the provisioning interface of UIDM-ISC are in the Authentication and Identity Integration Requirements v1.0, 03-Oct-2026 (R9).

# Non-functional requirements

The BRD's NFR section has no IDs; R2 numbers the rows UAM-NFR-01 to UAM-NFR-41. Most infrastructure rows are answered "Follow existing QPS set up" in the BRD; BIBS follows its own deployment standards for them. Rows that describe functions are specified as FRs in section 4 and cross-referenced here.

<!-- NFR:START -->
<!-- table: widths=2.1,2.8,4.9,4.0,1.5 caption="Non-functional requirements (BRD p.11-18)" size=7.5 -->
| ID | Topic | BRD requirement | BIBS approach | FR |
|---|---|---|---|---|
| UAM-NFR-01 | Capacity and performance | Requestor 14 users / 5 concurrent (enrol, modify, deactivate, reactivate, view: 10 tpm each; bulk 3 tpm); Approver 8 / 3 (10 tpm); BU Admin 4 / 2 (group profile 2 tpm); System Admin 1 / 1 (10 tpm, group profile 2 tpm); reports BU / System Admin 5 / 3 (10 tpm). Response 10 s, reports 20 s (p.11) | Within the BIBS sizing (145 concurrent users); online p95 under 3 seconds, reports under 20 seconds | - |
| UAM-NFR-02 | Projected volume | 10 transactions per month (average); login / logout, start-up and event reports 14 users / 5 concurrent, 10 per month, 5% growth, 10 s, 0% error; audit log report 5 / 3, 10 per month, 20% growth, 20 s, 0% error (p.11) | No specific sizing needed | - |
| UAM-NFR-03 | Data retention (records, logs) | Follow QPS retention policy (p.11) | Retention rules per record type; access requests, change log and session log kept at least as long as the audit trail; QPS values UQ12 | - |
| UAM-NFR-04 | Scalability, availability, reliability, DR, audit and data management, portability, interoperability, maintainability, environments, migration, support | "Follow existing QPS set up" for every item (scaling, operating hours, maintenance windows, high availability, uptime, recovery point and time, backup, disaster recovery, delivery models, integration with other systems, core banking, monitoring, environments from development to disaster recovery, data and user migration, QA, training, 24/7 support) (p.11-12) | The same BIBS standards for every BRD, specified in the Technical Specification, reviewed by BDOI IT; existing users loaded with the bulk request (FR-UA-019) | - |
| UAM-NFR-05 | Accessibility and channels | Accessibility options for impaired vision or colour blindness; website and mobile website: "Follow existing QPS set up" (p.12) | BDO UX contrast rules; responsive screens for desktop and mobile browsers | - |
| UAM-NFR-06 | Regulatory and compliance | MORB (BSP), Circular 808 of 2013, AMLA 2001, Data Privacy Act 2012 (p.12) | Four eyes on every access change, least privilege, separation-of-duties rules, deactivation of dormant users, an audit trail no user can change, retention | FR-UA-006, 053 |
| UAM-NFR-07 | Hardware, software, file locations, embedded IDs | Server / workstation specifications; binaries and logs in standard locations; embedded application user IDs named e_appshortname_description (e.g. p_appname_sftp) (p.12-13) | Service accounts named by the BDO convention; servers and software as specified in the Technical Specification, reviewed by BDOI IT | - |
| UAM-NFR-08 | Network | No impact on branch / ATM operations, no change to network design; ports, devices, bandwidth, latency, interfaces (p.13) | Encrypted connections only; no change to the BDO network; hosts and ports as specified in the Technical Specification, reviewed by BDOI IT | - |
| UAM-NFR-09 | User and role management 1.a-f | Online modules to view, add, modify, delete master data incl. users and roles; access via roles; more than one user per role; **a user can have more than one role**; accounts can be disabled and re-enabled; maintenance recorded in the application log (p.13) | Users, roles and permissions; several roles per user and users per role; enable / disable, also of dormant users by a job; every change audited (CQ23 answered) | FR-UA-006, 052, 064 |
| UAM-NFR-10 | User and role management 1.g | Query or report showing the roles assigned to users; exportable (p.13) | Reports UAM-USER-ACCESS and UAM-GROUP-MEMBERS, exportable | FR-UA-060, 062 |
| UAM-NFR-11 | User and role management 1.h | Supports LDAP or Active Directory authentication (p.13) | Directory sign-in through EIAM on Microsoft Entra ID, which holds the BDO directory accounts: single sign-on with OpenID Connect or SAML 2.0, switched on by configuration (CLR-UA-01, CLR-UA-21) | FR-UA-003 |
| UAM-NFR-12 | User and role management 1.i | Can support authorisation of users by interfacing with an external ACL (p.13) | On hold until BDOI answers UQ14 | - |
| UAM-NFR-13 | User and role management 1.j | User ID of at least ten (10) alphanumeric characters, supports the format a999999999 (p.13) | Parameter USER_ID_PATTERN checked on requests (default a999999999 format, UQ05) | FR-UA-011, 052 |
| UAM-NFR-14 | User and role management 1.k | Ability to specify an effective date for user and role changes (p.13) | Effective date on requests; daily job applies scheduled changes (UQ06) | FR-UA-020 |
| UAM-NFR-15 | User ID maintenance fields | Windows ID, user name, user status (active, disabled, locked out, online, etc.), Business Unit Group, User Level (p.14) | Windows ID, business unit group, user level; status Active, Disabled, Locked, Online (UQ05, UQ13) | FR-UA-052, 004 |
| UAM-NFR-16 | System Administrator capabilities | Access rights per group per role; user administrator maintenance; group administration; maintain reference tables; workflow set-up; screen update and maintenance (p.14) | Access rights per profile, user and group administration and reference tables exist; approver rules are parameters, and a change of a security setting needs a second approval; screens change by release | FR-UA-052, 054 |
| UAM-NFR-17 | Authentication through EUA | Interface the user ID with EUA using the Windows ID to authenticate access if the user exists in the BDO network, with password validation: the system passes the user ID and password; EUA returns whether the logon succeeded; on failure the error message is forwarded (p.14) | Windows ID sign-in through EUA when AUTH_MODE = DIRECTORY, with the EUA message shown; proposed to be replaced by the single sign-on through EIAM, where the Windows ID identifies the user (CLR-UA-21, CLR-UA-22) | FR-UA-003 |
| UAM-NFR-18 | Login logging | Log all valid and invalid attempts (p.14) | Every valid and invalid log-in is audited | FR-UA-001 |
| UAM-NFR-19 | Lockout | Users are locked out after 3 invalid attempts (p.14) | LOGIN_MAX_FAILED_ATTEMPTS = 3 for local accounts; with single sign-on the EIAM lock-out policy applies (CLR-UA-25) | FR-UA-001 |
| UAM-NFR-20 | Reference / master data management | Online view / add / modify / delete; referential integrity and validation; logical deletion; changes logged; role-based access (p.14) | Lists of Values with maker-checker, effective dates and deactivation instead of delete; audited | - |
| UAM-NFR-21 | Error logging | Error log that records each failed action with its details: description, time, user ID and module ID; enable / disable; location; rotation; size; one line per entry; level and message-type selection; sent to a remote log server (p.14) | Every error is logged in one line with the user, the module and the failed action; the log details, levels and the transfer to the BDO log server are specified in the Technical Specification, reviewed by BDOI IT | - |
| UAM-NFR-22 | Transaction logging and audit trail | Transaction log with type, time, user ID, module ID; enable / disable; location; transmit; all user activities logged through the audit trail and retrievable by the administrator; allow automatic save option (p.14) | Audit trail that no user can change and the Audit Trail screen; meaning of 'automatic save option' is UQ15 | FR-UA-064 |
| UAM-NFR-23 | Logs retention and archiving | Retention / archival / purging / backup for application, system, audit and infrastructure logs, historical data, snapshots, video, documents: "Follow existing QPS set up" (p.14) | As UAM-NFR-03 (UQ12) | - |
| UAM-NFR-24 | Batch processing a-l | Schedule, invoke manually, monitor, re-run, graceful terminate, UI without command line, publish status, single instance, restart at the interruption point, no privileged access, multi-core, parallel independent jobs (p.15) | Job scheduler with run history, run on demand, re-run, failure alert; restart without duplicates; status in monitoring | FR-UA-071 |
| UAM-NFR-25 | Batch failure e-mail | Monitor batch runs, log success and failure; forward an e-mail notification to designated users on failed batch runs (stated twice) (p.15) | Failed runs e-mailed to JOB_FAILURE_RECIPIENTS | FR-UA-071 |
| UAM-NFR-26 | System monitoring; network configuration; DR | Monitor and start / stop application processes without command line; DNS aliases; configurable ports; no hosts file; standard ports; Netbackup / Bacula, clustering, offsite replication, active-active, disk estimates (p.15) | Monitoring, start and stop from the platform tools without a command line, as specified in the Technical Specification, reviewed by BDOI IT | - |
| UAM-NFR-27 | Data purging and archiving | Automated archiving and purging of transactional data, logs, reports, temporary files; schedule; restricted access; configurable rules (p.15-16) | Retention rules and the monthly retention review; physical archive and purge follow the BIBS retention decision (Q39, UQ12) | - |
| UAM-NFR-28 | Server and workstation software | Documented install / deploy / update; deploy from repository; runs as a service; multiple instances; dynamic configuration reload; least privilege; browser only on workstations; no proprietary office software; not tied to third-party versions (p.16) | Installation, deployment, instances and service accounts as specified in the Technical Specification, reviewed by BDOI IT; users need only a browser on their workstations | - |
| UAM-NFR-29 | System documentation | Functional design, technical design, user manual, installation guide, release notes, instruction guide on how to manage users and extract the users list and group profile list; troubleshooting guide; training; capacity planning; performance benchmarks (p.16-17) | User-administration guide in the in-app Help Center and the administration guide (UQ18) | - |
| UAM-NFR-30 | System development, testing and production support | Access to resources outside the BDO network, off-site / on-site development, performance testing; maintenance access (p.17) | Engagement terms | - |
| UAM-NFR-31 | Other BU NFR 1 | Password management (reset, change) (p.17) | Administrator reset and own change exist | FR-UA-005 |
| UAM-NFR-32 | Other BU NFR 2-3 | Role-based access control (define roles, assign permissions, map users to roles); permission management (p.17) | Roles, permissions, user-role mapping, Roles screen and User Access Matrix exist | FR-UA-051 |
| UAM-NFR-33 | Other BU NFR 4 | Integration with Single Sign-On (SSO) or LDAP (p.17) | Single sign-on with OpenID Connect and SAML 2.0 is part of BIBS; EIAM (Entra ID) is the provider, switched on by configuration (CLR-UA-01) | FR-UA-003 |
| UAM-NFR-34 | Other BU NFR 5 | Session management: inactivity warning after 15 minutes; warning prior to system-triggered log out after 30 minutes (p.17) | Session policy parameters and the web session guard | FR-UA-002 |
| UAM-NFR-35 | Other BU NFR 6 | Audit logging: track changes in user roles and permissions; log user login / log out; audit log reports (p.17) | Role and permission changes in the structured change log; log-out and session log added; audit log report | FR-UA-004, 063 |
| UAM-NFR-36 | Other BU NFR 7 | Enforce strong password policies (p.17) | Complexity and length exist; history, maximum and minimum age and forced change added (UQ08) | FR-UA-005 |
| UAM-NFR-37 | Other BU NFR 8 | Self-service and admin tools: self-service password reset; user-friendly screens to manage access; a defined user can update profile information (p.17) | Self-service reset by e-mailed single-use link (local accounts); own e-mail and mobile update (UQ08, UQ17) | FR-UA-005 |
| UAM-NFR-38 | Other BU NFR 9 | Bulk uploads or batch updates of user profiles (p.17) | Bulk request (UQ10) | FR-UA-019 |
| UAM-NFR-39 | Other BU NFR 10 | Notification for access changes (p.17) | Affected user notified in the app and by e-mail when the change is applied | FR-UA-070 |
| UAM-NFR-40 | Other BU NFR 11 | Configurable workflow for access requests and approvals; changes to user privileges systematically monitored, evaluated and, if necessary, escalated for additional review (e.g. modifications outside working hours, low to high privilege) (p.17) | Privilege level per profile; privilege increase or out-of-hours change needs a second approval and raises an alert (UQ07); separation-of-duties rules refuse combinations of profiles; security settings change under a second approval | FR-UA-034, 053, 054 |
| UAM-NFR-41 | Report requirements (section 7) | Samples A User Access Report, B User Group Profile Report, C Group Profile membership (titled "User Group Profile Report"), D User Access Audit Log (date, activity, from, to, done by); generated by, date coverage, date and time generated (p.18) | Four reports with generated by, coverage and time generated in the header | FR-UA-060 to 063 |
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
| SESSION_EXPIRY_WARNING_MINUTES (exists) | 30 | Warning before the fixed end of the session |
| AUTH_MODE | LOCAL | LOCAL, DIRECTORY (EUA), OIDC or SAML (single sign-on through EIAM, Entra ID; CLR-UA-01) |
| USER_ID_PATTERN | ^[a-zA-Z][0-9]{9}$ | User ID format (to confirm, UQ05) |
| PASSWORD_HISTORY_COUNT | 8 | Previous passwords refused (UQ08) |
| PASSWORD_MAX_AGE_DAYS | 90 | Password expiry (UQ08) |
| PASSWORD_MIN_AGE_DAYS | 1 | Minimum days between changes (UQ08) |
| UAM_WORKING_HOURS | 08:00-18:00, MON-FRI | Working hours for the out-of-hours flag (UQ07) |
| UAM_ANY_APPROVER | false | Any approver may decide, not only the chosen one (UQ02) |
| UAM_DIRECT_ROLE_EDIT | false | Emergency direct role edit (audited, alerted) |
| UAM_ROLE_APPLY_ON_APPROVAL | false | Group-profile requests applied on approval instead of implemented (UQ03) |
| JOB_FAILURE_RECIPIENTS | empty | E-mail recipients of failed batch runs |
| USER_ID_FORMAT_TEXT | a letter followed by nine digits, for example a013000196 | The user ID format in words, shown to the requester with the format check |
| UAM_EXTERNAL_USERS | false | Requests for external (portal) users; false while no portal is installed |
| UAM_DORMANT_DAYS | 90 | Days without a sign-in after which the job deactivates a user; 0 = never (FR-UA-006) |
| UAM_DORMANT_NOTICE_DAYS | 7 | Days before the deactivation on which the user is told to sign in; 0 = no notice |
| Job UAM_EFFECTIVE_CHANGES | 00:05 daily | Applies scheduled requests |
| Job UAM_DORMANT_USERS | 00:15 daily | Deactivates the dormant users and sends the notices (FR-UA-006) |
| Job PASSWORD_EXPIRY_NOTICE | 06:00 daily | Notifies users whose password expires within 7 days (LOCAL mode) |

Every parameter of this table belongs to the Security category: a change waits for the approval of Information Security (FR-UA-054).

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| UAM_BUSINESS_UNIT | Empty until BDOI supplies the business unit groups (UQ05) |
| UAM_USER_LEVEL | Empty until BDOI supplies the user levels (UQ05) |
| UAM_DEACTIVATION_REASON | Resigned; Transferred; Long leave; Security; Others (to confirm with BDOI); No sign-in for the dormancy period (dormant-user job) |
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
| A-UA-06 | Single sign-on through EIAM is switched on by configuration once BDOI IT registers BIBS for each environment; SIT uses local sign-in until then, and the break-glass administrators keep it | UA-Q06, UA-Q07, UA-Q13 |
| A-UA-07 | Infrastructure answers "Follow existing QPS set up" are met by the BIBS deployment standards | Section 8 |
| A-UA-08 | Duplicate IDs 2.002.7.1 and 3.003.1.1 are kept as printed and marked "(2nd)" | R2 section 10 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-UA-01 | BDOI IT registers BIBS in EIAM (Entra ID) for each environment: tenant, application registration, sign-in and sign-out addresses, the claim that identifies the user, test accounts for SIT and UAT (R9) | FR-UA-003 (UA-Q07, UA-Q08) |
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
| UQ04 | EUA interface: protocol, host, Windows ID format, messages; break-glass administrator; password policy with EUA (proposed to be replaced by EIAM, UA-Q05, CLR-UA-21) | FR-UA-003 | OPEN |
| IQ04 | EIAM (Entra ID): tenant and app registration, claims, MFA and conditional access, session life against the 30-minute rule, non-BDO users (programme alignment; Drop 0 questions UA-Q06 to UA-Q15) | FR-UA-003 | OPEN |
| IQ05 | UIDM-ISC: which access changes are requested and approved in the IGA and which stay as BIBS requests; connector type; certification (programme alignment) | Section 5, FR-UA-010 to 045 | OPEN |
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

Every BRD-11 requirement line is met by at least one FR; the appendix User-story view gives each line as a user story with its FRs, acceptance criteria and test cases. The BRD prints 160 lines; IDs 2.002.7.1 and 3.003.1.1 appear twice and the second occurrence is marked "(2nd)". "Flag" repeats the BRD's Pilot Phase / Mandatory columns ("-" = not flagged). The NFR rows are traced in section 8.

<!-- TRACE:START -->
<!-- table: widths=2.7,1.1,6.9,1.2,3.1,5.2 caption="BRD requirement line to FR (160 lines) and test cases" size=7.5 -->
| BRD ID | Page | Activity and requirement | Flag | FR | Test cases |
|---|---|---|---|---|---|
| BRD 1.001.1 | p.8 | Log in as a Requestor - Access the application using any BDO-issued device | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 1.001.1.1 | p.8 | Log in as a Requestor - Log in with a Requestor user profile | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 1.001.1.2 | p.8 | Log in as a Requestor - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 1.001.1.3 | p.8 | Log in as a Requestor - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 1.002.1 | p.8 | Enroll New User - Submit a new User Access request | Yes | FR-UA-011 | TC-UA-011.1, 011.2, 011.3, 011.4 (9 cases) |
| BRD 1.002.1.1 | p.8 | Enroll New User - 1. Create request | Yes | FR-UA-010, FR-UA-011 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.002.1.1.1 | p.8 | Enroll New User - 1.1 Input new user data | Yes | FR-UA-011 | TC-UA-011.1, 011.2, 011.3, 011.4 (9 cases) |
| BRD 1.002.1.1.2 | p.8 | Enroll New User - 1.2 Select User Access Group profile | Yes | FR-UA-011 | TC-UA-011.1, 011.2, 011.3, 011.4 (9 cases) |
| BRD 1.002.1.1.3 | p.8 | Enroll New User - 1.3 Select approver from the drop-down | Yes | FR-UA-015 | TC-UA-015.1, 015.2, 015.3 (4 cases) |
| BRD 1.002.1.1.4 | p.8 | Enroll New User - 1.4 Add remarks (i.e. justification, etc.) | - | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.002.1.1.5 | p.8 | Enroll New User - 1.5 Save remarks | - | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.002.1.2 | p.8 | Enroll New User - 2. Edit drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.002.1.3 | p.8 | Enroll New User - 3. Cancel drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.002.1.4 | p.8 | Enroll New User - 4. Submit request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1 | p.8 | Modify Existing User - Submit a modify User Access / permission request | Yes | FR-UA-012 | TC-UA-012.1, 012.2, 012.3 (5 cases) |
| BRD 1.003.1.1 | p.8 | Modify Existing User - 1. Create request | Yes | FR-UA-010, FR-UA-012 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1.1.1 | p.8 | Modify Existing User - 1.1 Search existing user | Yes | FR-UA-012 | TC-UA-012.1, 012.2, 012.3 (5 cases) |
| BRD 1.003.1.1.2 | p.8 | Modify Existing User - 1.2 Edit / update selected user data | - | FR-UA-012 | TC-UA-012.1, 012.2, 012.3 (5 cases) |
| BRD 1.003.1.1.3 | p.8 | Modify Existing User - 1.3 Select User Access Group profile | Yes | FR-UA-012 | TC-UA-012.1, 012.2, 012.3 (5 cases) |
| BRD 1.003.1.1.4 | p.8 | Modify Existing User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | TC-UA-015.1, 015.2, 015.3 (4 cases) |
| BRD 1.003.1.1.5 | p.8 | Modify Existing User - 1.5 Add remarks (i.e. justification, etc.) | - | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1.1.6 | p.8 | Modify Existing User - 1.6 Save remarks | - | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1.2 | p.8 | Modify Existing User - 2. Edit drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1.3 | p.8 | Modify Existing User - 3. Cancel drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.003.1.4 | p.8 | Modify Existing User - 4. Submit request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.004.1 | p.8 | Deactivate User - Submit a User Access deactivation request | Yes | FR-UA-013 | TC-UA-013.1, 013.2, 013.3 (4 cases) |
| BRD 1.004.1.1 | p.8 | Deactivate User - 1. Create a request | Yes | FR-UA-010, FR-UA-013 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.004.1.1.1 | p.8 | Deactivate User - 1.1 Search an existing user | Yes | FR-UA-013 | TC-UA-013.1, 013.2, 013.3 (4 cases) |
| BRD 1.004.1.1.2 | p.8 | Deactivate User - 1.2 Add remarks (i.e. reason for deactivation) | - | FR-UA-010, FR-UA-013 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5, 013.1, 013.2, 013.3 (12 cases) |
| BRD 1.004.1.1.3 | p.8 | Deactivate User - 1.3 Save remarks | Yes | FR-UA-010, FR-UA-013 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5, 013.1, 013.2, 013.3 (12 cases) |
| BRD 1.004.1.1.4 | p.8 | Deactivate User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | TC-UA-015.1, 015.2, 015.3 (4 cases) |
| BRD 1.004.1.2 | p.8 | Deactivate User - 2. Edit drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.004.1.3 | p.8 | Deactivate User - 3. Cancel drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.004.1.4 | p.8 | Deactivate User - 4. Submit request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.005 | p.8 | Reactivate User - Submit a User Access reactivation request | Yes | FR-UA-014 | TC-UA-014.1, 014.2 (3 cases) |
| BRD 1.005.1 | p.8 | Reactivate User - 1. Create a request | Yes | FR-UA-010, FR-UA-014 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.005.1.1 | p.8 | Reactivate User - 1.1 Search an existing user | Yes | FR-UA-014 | TC-UA-014.1, 014.2 (3 cases) |
| BRD 1.005.1.1.1 | p.8 | Reactivate User - 1.2 Add remarks (i.e. reason for reactivation) | - | FR-UA-010, FR-UA-014 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5, 014.1, 014.2 (11 cases) |
| BRD 1.005.1.1.2 | p.8 | Reactivate User - 1.3 Save remarks | Yes | FR-UA-010, FR-UA-014 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5, 014.1, 014.2 (11 cases) |
| BRD 1.005.1.1.3 | p.8 | Reactivate User - 1.4 Select approver from the drop-down | Yes | FR-UA-015 | TC-UA-015.1, 015.2, 015.3 (4 cases) |
| BRD 1.005.1.2 | p.8 | Reactivate User - 2. Edit a drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.005.1.3 | p.8 | Reactivate User - 3. Cancel a drafted request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.005.1.4 | p.8 | Reactivate User - 4. Submit a request | Yes | FR-UA-010 | TC-UA-010.1, 010.2, 010.3, 010.4, 010.5 (8 cases) |
| BRD 1.006.1 | p.8 | Apply Correction - Apply correction on returned requests | Yes | FR-UA-016 | TC-UA-016.1, 016.2, 016.3 (4 cases) |
| BRD 1.006.1.1 | p.8 | Apply Correction - 1. Receive notification on the returned request | Yes | FR-UA-016, FR-UA-070 | TC-UA-016.1, 016.2, 016.3, 070.1, 070.2, 070.3 (7 cases) |
| BRD 1.006.1.2 | p.8 | Apply Correction - 2. Edit data on the returned request | Yes | FR-UA-016 | TC-UA-016.1, 016.2, 016.3 (4 cases) |
| BRD 1.006.1.3 | p.8 | Apply Correction - 3. Add remarks | Yes | FR-UA-016 | TC-UA-016.1, 016.2, 016.3 (4 cases) |
| BRD 1.006.1.4 | p.8 | Apply Correction - 4. Save remarks | Yes | FR-UA-016 | TC-UA-016.1, 016.2, 016.3 (4 cases) |
| BRD 1.006.1.5 | p.8 | Apply Correction - 5. Re-submit request | Yes | FR-UA-016 | TC-UA-016.1, 016.2, 016.3 (4 cases) |
| BRD 1.007.1 | p.8 | Cancel a Request - Cancel a submitted request for approval | Yes | FR-UA-017 | TC-UA-017.1, 017.2, 017.3, 017.4 (5 cases) |
| BRD 1.007.1.1 | p.8 | Cancel a Request - 1. Search request | Yes | FR-UA-017 | TC-UA-017.1, 017.2, 017.3, 017.4 (5 cases) |
| BRD 1.007.1.2 | p.8 | Cancel a Request - 2. Add remarks (i.e. reason for cancellation, etc.) | Yes | FR-UA-017 | TC-UA-017.1, 017.2, 017.3, 017.4 (5 cases) |
| BRD 1.007.1.3 | p.8 | Cancel a Request - 3. Save remarks | Yes | FR-UA-017 | TC-UA-017.1, 017.2, 017.3, 017.4 (5 cases) |
| BRD 1.007.1.4 | p.8 | Cancel a Request - 4. Cancel submitted request | Yes | FR-UA-017 | TC-UA-017.1, 017.2, 017.3, 017.4 (5 cases) |
| BRD 1.008.1 | p.8 | View Request/s - View the submitted request/s | - | FR-UA-018 | TC-UA-018.1, 018.2, 018.3 (5 cases) |
| BRD 1.008.1.1 | p.8 | View Request/s - 1. View the list of the submitted request/s | Yes | FR-UA-018 | TC-UA-018.1, 018.2, 018.3 (5 cases) |
| BRD 1.008.1.2 | p.8 | View Request/s - 2. View the status of the submitted request/s | Yes | FR-UA-018 | TC-UA-018.1, 018.2, 018.3 (5 cases) |
| BRD 1.008.1.3 | p.8 | View Request/s - 3. View the details of the submitted request/s | Yes | FR-UA-018 | TC-UA-018.1, 018.2, 018.3 (5 cases) |
| BRD 1.008.1.4 | p.8 | View Request/s - 4. Receive notification on the approved / rejected request | Yes | FR-UA-018, FR-UA-070 | TC-UA-018.1, 018.2, 018.3, 070.1, 070.2, 070.3 (8 cases) |
| BRD 1.009.1 | p.8 | Bulk Request - Submit request for bulk creation / modification / deactivation / reactivation | - | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.1 | p.8 | Bulk Request - 1. Attach a file for bulk processing | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.2 | p.8 | Bulk Request - 2. Add remarks | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.3 | p.8 | Bulk Request - 3. Save remarks | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.4 | p.8 | Bulk Request - 4. Edit a drafted request | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.5 | p.8 | Bulk Request - 5. Cancel a drafted request | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 1.009.1.6 | p.8 | Bulk Request - 6. Submit a request | Yes | FR-UA-019 | TC-UA-019.1, 019.2, 019.3, 019.4 (5 cases) |
| BRD 2.001.1 | p.8 | Log in as an Approver - Access the application using any BDO-issued device | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 2.001.1.1 | p.8 | Log in as an Approver - Log in with an Approver user profile | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 2.001.1.2 | p.8 | Log in as an Approver - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 2.001.1.3 | p.8 | Log in as an Approver - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 2.002.1 | p.8 | Review and Approve Request/s - a. Receive notification on the request for approval | Yes | FR-UA-030, FR-UA-015, FR-UA-070 | TC-UA-030.1, 030.2, 030.3, 070.1, 070.2, 070.3 (6 cases) |
| BRD 2.002.2 | p.8 | Review and Approve Request/s - b. View the list of requests for approval | Yes | FR-UA-030 | TC-UA-030.1, 030.2, 030.3 (3 cases) |
| BRD 2.002.2.1 | p.8 | Review and Approve Request/s - 1. Filter the list assigned to me | Yes | FR-UA-030 | TC-UA-030.1, 030.2, 030.3 (3 cases) |
| BRD 2.002.3 | p.8 | Review and Approve Request/s - c. Select request | Yes | FR-UA-030 | TC-UA-030.1, 030.2, 030.3 (3 cases) |
| BRD 2.002.4 | p.8 | Review and Approve Request/s - d. View request details | Yes | FR-UA-030 | TC-UA-030.1, 030.2, 030.3 (3 cases) |
| BRD 2.002.5 | p.8 | Review and Approve Request/s - e. Approve request | Yes | FR-UA-031 | TC-UA-031.1, 031.2, 031.3 (4 cases) |
| BRD 2.002.6 | p.8 | Review and Approve Request/s - f. Reject request | Yes | FR-UA-032 | TC-UA-032.1, 032.2 (3 cases) |
| BRD 2.002.6.1 | p.8 | Review and Approve Request/s - 1. Add remarks (i.e. rejection reason, etc.) | - | FR-UA-032 | TC-UA-032.1, 032.2 (3 cases) |
| BRD 2.002.6.2 | p.9 | Review and Approve Request/s - 2. Save remarks | - | FR-UA-032 | TC-UA-032.1, 032.2 (3 cases) |
| BRD 2.002.7 | p.9 | Review and Approve Request/s - g. Return request to Requestor | Yes | FR-UA-033 | TC-UA-033.1, 033.2 (2 cases) |
| BRD 2.002.7.1 | p.9 | Review and Approve Request/s - 1. Add remarks (i.e. reason for return, etc.) | Yes | FR-UA-033 | TC-UA-033.1, 033.2 (2 cases) |
| BRD 2.002.7.1 (2nd) | p.9 | Review and Approve Request/s - 2. Save remarks | Yes | FR-UA-033 | TC-UA-033.1, 033.2 (2 cases) |
| BRD 3.001.1 | p.9 | Log in as a Business Administrator - Access the application using any BDO-issued device | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 3.001.1.1 | p.9 | Log in as a Business Administrator - Log in with a Business Administrator user profile | Yes | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 3.001.1.2 | p.9 | Log in as a Business Administrator - Receive the inactivity warning (15 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 3.001.1.3 | p.9 | Log in as a Business Administrator - Receive a warning prior to the system-triggered log out (30 minutes) | Yes | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 3.002.1 | p.9 | Submit a User Access Group Profile Request - a. Submit a new User Access Group Profiles request | Yes | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.2 | p.9 | Submit a User Access Group Profile Request - 2. Select approver/s from the drop-down | - | FR-UA-044 | TC-UA-044.1, 044.2, 044.3 (5 cases) |
| BRD 3.002.1.3 | p.9 | Submit a User Access Group Profile Request - 3. Edit a drafted request | Yes | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.4 | p.9 | Submit a User Access Group Profile Request - 4. Cancel a drafted request | Yes | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.5 | p.9 | Submit a User Access Group Profile Request - 5. Submit a request | Yes | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.6 | p.9 | Submit a User Access Group Profile Request - 6. Add remarks | - | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.1.7 | p.9 | Submit a User Access Group Profile Request - 7. Save remarks | - | FR-UA-040 | TC-UA-040.1, 040.2, 040.3 (6 cases) |
| BRD 3.002.2 | p.9 | Submit a User Access Group Profile Request - b. Submit a modify User Access Group Profiles request | Yes | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | TC-UA-044.1, 044.2, 044.3 (5 cases) |
| BRD 3.002.2.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.2.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-041 | TC-UA-041.1, 041.2, 041.3 (6 cases) |
| BRD 3.002.3 | p.9 | Submit a User Access Group Profile Request - c. Submit a User Access Group Profiles deactivation request | Yes | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | TC-UA-044.1, 044.2, 044.3 (5 cases) |
| BRD 3.002.3.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.3.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-042 | TC-UA-042.1, 042.2, 042.3 (4 cases) |
| BRD 3.002.4 | p.9 | Submit a User Access Group Profile Request - d. Submit a User Access Group Profiles reactivation request | Yes | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.1 | p.9 | Submit a User Access Group Profile Request - 1. Create a request | Yes | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.2 | p.9 | Submit a User Access Group Profile Request - 2. Search existing group profile from the drop-down | - | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.3 | p.9 | Submit a User Access Group Profile Request - 3. Select approver/s from the drop-down | - | FR-UA-044 | TC-UA-044.1, 044.2, 044.3 (5 cases) |
| BRD 3.002.4.4 | p.9 | Submit a User Access Group Profile Request - 4. Edit a drafted request | Yes | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.5 | p.9 | Submit a User Access Group Profile Request - 5. Cancel a drafted request | Yes | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.6 | p.9 | Submit a User Access Group Profile Request - 6. Submit a request | Yes | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.7 | p.9 | Submit a User Access Group Profile Request - 7. Add remarks | - | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.002.4.8 | p.9 | Submit a User Access Group Profile Request - 8. Save remarks | - | FR-UA-043 | TC-UA-043.1, 043.2 (2 cases) |
| BRD 3.003.1 | p.9 | Generate Reports - a. Generate customized report | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.1 | p.9 | Generate Reports - 1. User Access Report | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.1 (2nd) | p.9 | Generate Reports - 1.1 User name | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.2 | p.9 | Generate Reports - 1.2 Group profile | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.3 | p.9 | Generate Reports - 1.3 Date created | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.4 | p.9 | Generate Reports - 1.4 Date modified | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.5 | p.9 | Generate Reports - 1.5 Created by | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.1.6 | p.9 | Generate Reports - 1.6 Added / Modified / deactivated / reactivated by | Yes | FR-UA-060 | TC-UA-060.1, 060.2, 060.3 (5 cases) |
| BRD 3.003.2 | p.9 | Generate Reports - 2. User Group Profile Report | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.1 | p.9 | Generate Reports - 2.1 Group profile name | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.2 | p.9 | Generate Reports - 2.2 Modules accessed under each group profile | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.3 | p.9 | Generate Reports - 2.3 Date created | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.4 | p.9 | Generate Reports - 2.4 Date modified | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.5 | p.9 | Generate Reports - 2.5 Created by | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.2.6 | p.9 | Generate Reports - 2.6 Added / Modified / deactivated / reactivated by | Yes | FR-UA-061 | TC-UA-061.1, 061.2 (2 cases) |
| BRD 3.003.3 | p.9 | Generate Reports - 3. Group Profile Membership list | Yes | FR-UA-062 | TC-UA-062.1, 062.2 (2 cases) |
| BRD 3.003.3.1 | p.9 | Generate Reports - 1. List of members under each profile | Yes | FR-UA-062 | TC-UA-062.1, 062.2 (2 cases) |
| BRD 3.003.3.1.1 | p.9 | Generate Reports - 1.1 Group profile | Yes | FR-UA-062 | TC-UA-062.1, 062.2 (2 cases) |
| BRD 3.003.3.1.2 | p.9 | Generate Reports - 1.2 User name | Yes | FR-UA-062 | TC-UA-062.1, 062.2 (2 cases) |
| BRD 4.001.1 | p.9 | Log in as a System Administrator - Access the application using any BDO-issued device | - | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 4.001.1.1 | p.9 | Log in as a System Administrator - Log in with a System Administrator user profile | - | FR-UA-001 | TC-UA-001.1, 001.2, 001.3, 001.4, 001.5 (7 cases) |
| BRD 4.001.1.2 | p.9 | Log in as a System Administrator - Receive the inactivity warning (15 minutes) | - | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 4.001.1.3 | p.9 | Log in as a System Administrator - Receive a warning prior to the system-triggered log out (30 minutes) | - | FR-UA-002 | TC-UA-002.1, 002.2, 002.3, 002.4 (5 cases) |
| BRD 4.002.1 | p.9 | Manage Users of the System - a. Define user profile / role | Yes | FR-UA-050, FR-UA-045 | TC-UA-045.1, 045.2, 045.3, 050.1, 050.2, 050.3 (7 cases) |
| BRD 4.002.1.1 | p.9 | Manage Users of the System - 1. Create user profile / role for Requestor | Yes | FR-UA-050 | TC-UA-050.1, 050.2, 050.3 (3 cases) |
| BRD 4.002.1.2 | p.9 | Manage Users of the System - 2. Create user profile / role for Approver | Yes | FR-UA-050 | TC-UA-050.1, 050.2, 050.3 (3 cases) |
| BRD 4.002.1.3 | p.9 | Manage Users of the System - 3. Create user profile / role for Business Administrator | Yes | FR-UA-050 | TC-UA-050.1, 050.2, 050.3 (3 cases) |
| BRD 4.002.2 | p.9 | Manage Users of the System - b. Define specific functionality or capability for a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.1 | p.9 | Manage Users of the System - 1. Assign Enroll New User to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.2 | p.9 | Manage Users of the System - 2. Assign Modify Existing User function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.3 | p.9 | Manage Users of the System - 3. Assign Deactivate User function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.4 | p.9 | Manage Users of the System - 4. Assign Reactivate User function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.5 | p.9 | Manage Users of the System - 5. Assign Apply Correction function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.6 | p.9 | Manage Users of the System - 6. Assign Cancel a Request function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.7 | p.10 | Manage Users of the System - 7. Assign View Request/s function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.8 | p.10 | Manage Users of the System - 8. Assign Review and Approve Request/s function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.9 | p.10 | Manage Users of the System - 9. Assign Submit a User Access Group Profiles Request function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.002.2.10 | p.10 | Manage Users of the System - 10. [Assign] Generate Reports Request function to a specific user profile / role | Yes | FR-UA-051 | TC-UA-051.1, 051.2, 051.3 (3 cases) |
| BRD 4.003.1 | p.10 | Generate Logs - a. Generate audit logs for compliance and audit purposes as needed | Yes | FR-UA-063, FR-UA-064 | TC-UA-063.1, 063.2, 063.3, 064.1, 064.2 (6 cases) |
<!-- TRACE:END -->

<!-- landscape -->

# Navigation

This chapter shows how each user reaches the screens, and what every persona of BIBS sees. Signing BRD-11 signs the role matrix: the sidebar shows a screen only when one of the user's active group profiles holds its permission (section 3.2), so each persona sees a different menu. The tables follow the proposed role grants.

## Screens of User Access Maintenance

The 19 screens specified in chapter 13, with the menu path and the roles that can open them. A screen without its own menu entry (a record, a form, a dialog) is reached from the screen before it; its path ends with that screen.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screen-index
```

## Screen flow

How the screens link: from a list to its record, from a record action to the next screen, from the sign-in to the forced password change, and from the Users and Roles screens to the requests. The walkthroughs of chapter 14 follow these links with real steps.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: flow
```

<!-- portrait -->

## Menu by persona

For each persona, the SIT and UAT user of the seed data and the sidebar that user sees, section by section. The first eight tables are the personas of User Access Maintenance; the others are the personas of the persona suites of the other BRDs. The BRD column shows which BRD owns a section. The workbook of this set has the same table as one sheet per persona, with the review columns. The Auditor's and the System Administrator's menus are wide by design (read access for audit; administration).

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: menus
```

## Common screen elements

Elements that behave the same on every User Access Maintenance screen are described once here and not repeated in the screen specifications.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: common
```

<!-- landscape -->

# Screen specifications

One specification per screen, grouped by area. Each gives:

- **Purpose**, **who can open it** (personas and the permission), **navigation** (menu path and the other ways in) and the related **FRs**;
- **screenshots** of the SIT environment with seed data; the numbered markers on the first screenshot match the **No.** column of the field table;
- the **field table**: section of the screen, label as shown, type, length or format, mandatory (Y, N or the condition), source list or master, default, the statuses in which the field can be changed, the validation and the message shown when it fails, word for word;
- the **actions table**: button, who sees it, when it is enabled, what happens, the resulting status and the notification sent;
- the **business rules** of the screen with their FRs, the **expected outcome** and the **test cases** of the test plan that run on the screen.

A message in angle brackets (`<request no>`) is completed by the system with the value shown. "The approval right" is the permission ACCESS_APPROVE; the other rights are named in section 3.2.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screens
```

<!-- portrait -->

# End-to-end walkthroughs

Four walkthroughs follow user access through the screens, persona by persona, with what the user does, what the user sees and the result of each step. They use seed data only and run in order. They are the script of the SIT review sessions of the Start Here guide.

## WT-A A new user from the enrolment request to the first sign-in with the menus of the persona

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-A
```

## WT-B A change and a revocation under four eyes, and a group-profile change approved in order and implemented

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-B
```

## WT-C Refused cases and the messages the user sees

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-C
```

## WT-D The evidence of access, and the insurer roles hidden from BDOI

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-D
```

<!-- landscape -->

# Messages catalogue

Every message a User Access Maintenance user can see, grouped by the screen or dialog that shows it, with its code, its type and what the user does. The texts are quoted exactly as the screens show them.

- **Validation**: shown on the screen while the user fills in a field or before the form is sent.
- **Error**: the system refused the action; nothing was saved. The code is shown under the message as the Reference.
- **Warning**: the action is possible, but the user should check something first.
- **Confirmation** and **Information**: the outcome of an action, or a hint on the screen.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: messages
```

# Notifications catalogue

The in-app notifications, e-mails and alerts that User Access Maintenance sends: what triggers each, who receives it and what it contains. The temporary password of a new user is never sent by e-mail; it is shown once to the approver.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: notifications
```

<!-- portrait -->

# Document outputs

User Access Maintenance produces no letter. Its documents are the five user access reports, downloaded from the report runner as PDF, Excel or CSV with the BDO Insure header (report name, generated by, the date coverage and the date and time generated, BRD p.18), and the Excel export of the User Access Matrix. For each: the layout, the formats, the screen that produces it, where every field comes from and the first page as generated from seed data. The User Access Matrix holds every permission against every group profile, so its image is an extract: the User Access area against the six group profiles of user access maintenance.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: documents
```

# Upload screens

User Access Maintenance has one bulk upload: the Bulk Request (SCR-UA-09), which turns a file of user requests into a batch decided by an approver (FR-UA-019). The file is uploaded in the upload wizard of the Bulk Request screen: download the template, fill in one row per user, upload the file, review the check of every row (the rows in error are listed with their message and can be downloaded as an error file), correct and upload again, then save the valid rows as the draft lines of a batch. The batch is submitted from Bulk Request Batch (SCR-UA-10). The upload needs the bulk upload right besides the enrolment right (section 10.4).

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: uploads
```

The users of go-live are loaded with this upload, from the template CI-01 of chapter 19 (users are out of the scope of the data migration, DMQ30).

<!-- landscape -->

# Configuration inputs the business provides

User Access Maintenance is a setup object of Drop 0: before any other BRD is tested with its real users, BDOI provides the users and their group profiles, the role-to-permission matrix, the approvers and the approval limits, the separation-of-duties rules, the password, session and sign-in values, and the lists of values. The users are loaded with the bulk request; the matrix is implemented by group-profile requests; the parameters are set on System Parameters. Each input has a template in the workbook of configuration input templates of this set (06). The workbook opens on a Start here sheet that lists the templates in the order they are filled in (a template whose codes others use comes first), with who provides each, when it is due, how it is loaded, the rows entered, the mandatory cells still missing and a status. Each template sheet is self-contained: its purpose, owner, due date and the templates it depends on, then directly above each column its guide (mandatory or not, format, allowed values, what to enter), one fictitious example row to overwrite, and drop-downs with the allowed values and the codes of the templates filled in before. The allowed values are listed on the sheet Reference lists; questions per template and column are raised on the sheet Questions and comments. The sections below follow the same order.

```pack
plugin: ../signoff/config_inputs.py
source: pack/config_inputs.yaml
render: summary
```

```pack
plugin: ../signoff/config_inputs.py
source: pack/config_inputs.yaml
render: templates
```

# Cross-BRD dependencies and interface contract

User Access Maintenance is the source of the users, group profiles and permissions that every BIBS module checks, and of the menus of every persona; it takes lists and the approval list from New Business, and it has two identity integrations owned by BDOI IT. The contract below lists each exchange: the BRD or system, the direction, what is exchanged, when and how, and who owns the data. Chapter 7 describes the interfaces with the external systems.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: contract
```

<!-- portrait -->

# Sign-off and change control

## What is signed

The business sign-off covers the release set BRD-11 User Access Maintenance v2.1:

<!-- table: widths=6,11.6 caption="Documents of the release set" -->
| Document | Content |
|---|---|
| Start Here guide | The map of the set, who reads what, the steps and the dates |
| Sign-off Pack Guide deck | Purpose, approach, the module at a glance, caveats, entry and exit criteria, change control |
| This FRS v2.1 | Requirements (chapters 1-11), the business view of the system (chapters 12-21) and the proposed rules for confirmation (chapter 22) |
| Sign-off workbook v2.1 | The screens, fields, actions, rules, messages, notifications, contract and the menu of every persona of this FRS, one row each, with the BU review columns |
| Test plan v2.1 | The test cases traced to the FRs and to the screens of chapter 13 |
| Configuration input templates | The templates of chapter 19 |

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: counts
```

## How the review is recorded

Each unit records its review in the sign-off workbook: Accept, Change requested or Comment on each row of the screen catalogue, field register, business rules, messages and the menu sheets of its personas, with the comment, the reviewer and the date. The project team answers every Change requested row before sign-off: either the row is corrected in the next version of the set, or the change is raised as a change request (below) and the row is signed as specified.

## What each signatory confirms

<!-- table: widths=5.2,12.4 caption="Signatories and what they sign" -->
| Signatory | Confirms |
|---|---|
| Business owner (Product Owner, Marketing Business System) | The whole set: functions, screens, navigation, messages and the configuration inputs |
| Process owner, User Access Maintenance (Business Administration) | The requests, approvals, group-profile requests, bulk requests and reports, and the templates CI-01, CI-03 and CI-06 |
| BDOI Information Security | The role matrix (the menu of every persona, chapter 12, and template CI-02), the separation-of-duties rules (chapter 13 and template CI-04), the second approval of privileged changes, the emergency path, the password, lock-out and session values (template CI-05) and the audit log |
| BDOI IT | The sign-in and identity integration: single sign-on through EIAM on Entra ID, switched on by configuration once BIBS is registered for each environment (local sign-in on SIT until then), the proposed rules CLR-UA-21 to CLR-UA-26, and every access change as a BIBS request until the UIDM-ISC option is chosen (chapter 7, contract lines IC-08 and IC-09; R9; these integrations are owned by BDOI IT); the System Administrator functions |

## What signing freezes

Signing this release set freezes, for User Access Maintenance:

- the screens and their navigation (chapters 12 and 13), the fields with their order, labels, types, mandatory rules, lists and validations;
- the actions with their conditions and resulting statuses, the separation-of-duties rules and the other business rules;
- the messages (chapter 15), the notifications (chapter 16) and the reports (chapter 17);
- the upload template (chapter 18), the configuration input templates (chapter 19) and the interface contract with the other BRDs (chapter 20).

The content of the configuration is not frozen: the group profiles and their permissions, the users, the approvers, the list values and the parameters (password, session, working hours) are maintained in the system through requests and audited changes, without a change request. The menu of a persona therefore follows its group profiles: a change to a profile made by an implemented group-profile request is not a change to this signed set, and the menu tables are refreshed in its next version.

## Change after sign-off

A change to anything frozen is raised in the Change Management Register. The request states the screen, field, rule or message concerned, the reason and the business priority. The project team assesses it with its mandays, including its effect on the other BRDs through the interface contract of chapter 20, and the owners of every BRD it touches approve it; a change to a separation-of-duties rule or to the sign-in is also approved by BDOI Information Security. An approved change is delivered as a new version of this release set (v2.2, v2.3 and so on), and the changed rows are reviewed again.

## Proposed rules for confirmation

Chapter 22 lists the proposed business rules and screen behaviour that differ from the BRD or need a decision of BDOI. BDOI Information Security and BDOI IT record their decisions on the items they own with the review; a decision that changes a screen, field, rule or message is applied in the next version of this set.

<!-- pagebreak -->

## Signatures

By signing, BDOI confirms that this FRS and the sign-off workbook describe the User Access Maintenance functions, screens, menus and messages it expects in BIBS, accepts the assumptions in section 10.1 and records its decisions on the items of chapter 22. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Process Owner, User Access Maintenance (Business Administration)", organisation: BDOI}
  - {name: "", role: "Head, BDOI Information Security", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

<!-- pagebreak -->

# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, or where BIBS needs a decision of BDOI, BDOI Information Security or BDOI IT to complete the rule. Each item names the proposed behaviour, the reason and the decision requested. The decisions are recorded with the review of this set (section 21.6); a decision that changes a screen, field, rule or message is applied in the next version of the set, and an answer that only sets a value (a list, a parameter) is applied as configuration.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-UA-01 | Single sign-on through EIAM (UAM-NFR-11, 17, 33; FR-UA-003) | Single sign-on with OpenID Connect against the BDO Entra ID tenant (EIAM), SAML 2.0 kept as the fall-back if the BDOI IT standards require it. It is part of BIBS and switched on by configuration (sign-in mode OIDC or SAML) once BDOI IT registers BIBS for each environment; SIT uses local sign-in until then. Not a change request. | BDOI named EIAM on Microsoft Entra ID for Drop 0; the protocol details are in the Authentication and Identity Integration Requirements v1.0 (R9). | BDOI IT to choose the protocol and to register BIBS in EIAM for each environment, with test accounts for SIT and UAT (UA-Q06, UA-Q07, IQ04). |
| CLR-UA-02 | External access-control list (UAM-NFR-12; FR-UA-003) | Roles and permissions are kept in BIBS; there is no authorisation through an external access-control list. | The external ACL is not specified (UQ14). | Confirm that authorisation stays in BIBS, or specify the external ACL (UQ14, UA-Q23). |
| CLR-UA-03 | Provisioning from UIDM-ISC (p.6 request rule; Drop 0; FR-UA-010) | Every access change is a BIBS request under four eyes. The reports give the user and profile lists for access certification. | BDOI named UIDM-ISC for joiner, mover and leaver provisioning; the option is open (IQ05). | BDOI IT to choose the UIDM-ISC option: accounts only, accounts and roles, or aggregation only (IQ05, UA-Q18 to UA-Q21). |
| CLR-UA-04 | External (portal) users (decision D7; FR-UA-011, 013, 014) | The user type External is not offered while no portal is installed (UAM_EXTERNAL_USERS = false), and a request of that type is refused. Setting the parameter to true, with the second approval of Information Security, offers it again. | BDOI drops the EB portal (drop plan item 2.4, IQ22). | Confirm that external users stay switched off (UA-Q24). |
| CLR-UA-05 | Business unit group and user level (1.002.1.1.1, UAM-NFR-15; FR-UA-011, 052) | Both are captured on the user; their lists stay empty until BDOI gives the values, and a value is not checked against a list until then. | The values are not in the BRD (UQ05). | Give the business unit groups and user levels (UQ05, UA-Q26). |
| CLR-UA-06 | Effective date of an enrolment (UAM-NFR-14; FR-UA-020) | A scheduled enrolment creates the user on its date with a password that is never shown; the System Administrator resets it and the user changes it at the first sign-in. | The temporary password is shown only once, to the approver of an immediate enrolment. | Confirm the handling of scheduled enrolments (UA-Q39). |
| CLR-UA-07 | Deactivation of a group profile with members (3.002.3; FR-UA-042) | A profile with members can be deactivated; the members lose its screens when the request is implemented, are listed on the request and are each told (in the app and by e-mail). | The BRD does not say whether members must be moved first (UQ16). | Decide whether members must be moved before a deactivation (UQ16, UA-Q40). |
| CLR-UA-08 | Dormant accounts (information security practice; FR-UA-006) | A daily deactivation of users with no sign-in for 90 days, with a notice 7 days before; the System Administrator profile is exempt. | The period and the notice are proposals; Information Security has not given its values. | Information Security to confirm the dormancy period and the notice (template CI-05, UA-Q42). |
| CLR-UA-09 | One session per device (UAM-NFR-35; FR-UA-004) | The session log records every session; one session per device is not enforced. | The rule is not specified (UQ09). | Decide whether one session per device must be enforced (UQ09, UA-Q44). |
| CLR-UA-10 | Module and task names in the reports (3.003; FR-UA-060, 061) | The User Group Profile Report shows the module as the permission area and the task as the permission name. The report parameters Group Profile and Module are offered as lists. | The module and task names of sample B are not defined (UQ11). | Confirm the module and task names (UQ11, UA-Q50). |
| CLR-UA-11 | User Access Matrix for requesters (4.002.2.7; FR-UA-051) | The Requestor and the Second Approver do not open the User Access Matrix; the matrix serves the request, approval, role management and audit rights. | The BRD's view function does not name the matrix. | Decide whether requesters should read the User Access Matrix (UA-Q32). |
| CLR-UA-12 | Group-profile changes applied at implementation (UQ03; FR-UA-041, 045) | An approved change of group-profile permissions is applied when the System Administrator implements it, for every module, including Product Maintenance (BRD-3 PMADD05). | The BRD-11 diagram (p.6) has the System Administrator implement group-profile changes (PQ17 answered). | Confirm the implementation step for every module (UQ03, UA-Q36). |
| CLR-UA-13 | Temporary access with an end date (UQ06; FR-UA-017, 020) | Access is given until it is removed by a request; there is no end date on a group profile of a user. | Temporary access is not specified in the BRD (UQ06). | Decide whether temporary access with an end date is needed (UQ06, UA-Q39). |
| CLR-UA-14 | Out-of-hours flag on public holidays (NFR 11; FR-UA-034) | A request submitted or approved outside UAM_WORKING_HOURS is flagged for second approval; the flag is evaluated at submission and again at approval. Public holidays are not treated as out of hours. | The working hours have no holiday calendar. | Decide whether public holidays count as out of hours (UA-Q37). |
| CLR-UA-15 | Users who already hold both profiles of a new rule (FR-UA-053) | A new separation-of-duties rule is checked on later requests; users who already hold both profiles are not changed and are found with the User Access Report. | Existing access is changed only through requests. | Information Security to confirm that it reviews the report after each new rule and raises the requests (UA-Q31). |
| CLR-UA-16 | Bulk requests (1.009; FR-UA-019) | A bulk request is decided as a whole. | The maximum number of rows, line-by-line decisions and mixed actions are not given (UQ10). | Give the bulk settings (UQ10, UA-Q41). |
| CLR-UA-17 | Access from BDO-issued devices only (1.001.1; FR-UA-001) | Access only from BDO-issued devices is enforced by the BDO network and device policy, not by BIBS. | BIBS cannot see the device a browser runs on. | BDOI IT to confirm the network and device policy that enforces it (UA-Q10). |
| CLR-UA-18 | Stage list of an access request | The stepper of an access request shows Draft, Pending Approval, Second approval (only where one is needed), then Approved; a group-profile request ends with For implementation and Implemented. Scheduled shows while an approved change waits for its effective date; a returned request shows in amber, a rejected or cancelled one in red. | The request is not in a configured workflow; its stages are fixed for the screen. | Confirm the stage list and its names (UA-Q36). |
| CLR-UA-19 | Main path with two good endings | A user request ends at Approved and a group-profile request at Implemented; the stepper shows only the ending of the request type. | Two endings on one line would read as two steps. | Confirm the ending shown for each request type. |
| CLR-UA-20 | Full status labels | Request and user statuses show their full label (for example Pending Approval, For implementation); a short form with the full label in the tooltip is used only above 21 characters. | The client asked for full labels where they fit (27-Sep-2026). | Confirm full labels. |
| CLR-UA-21 | EIAM replaces the EUA and LDAP sign-in (UAM-NFR-11, 17; FR-UA-003) | EIAM on Microsoft Entra ID is the only directory sign-in of BDO users; the EUA password check and the LDAP / Active Directory sign-in are closed as replaced by EIAM, and the sign-in mode DIRECTORY is not used. Local sign-in is kept only for the break-glass administrators (CLR-UA-24). | The BRD asks for EUA, LDAP / AD or single sign-on; BDOI named EIAM for Drop 0, which holds the BDO directory accounts. | Confirm that EIAM replaces EUA and LDAP / AD for every BDO user (UA-Q05). |
| CLR-UA-22 | Matching the EIAM account to the BIBS user (UAM-NFR-15, 17; FR-UA-003, 052) | EIAM returns the Windows ID in a dedicated claim (or the user principal name where the Windows ID is not released); BIBS matches it to the Windows ID held on each user. The BIBS user ID stays the employee-number format (a999999999). An account with no matching active user is refused with the message SSO_NOT_LINKED. | The BRD identifies users by the Windows ID; Entra ID identifies them by the user principal name. | BDOI IT to confirm the claim that identifies the user and the relation of the Windows ID to the user ID (UA-Q08, UA-Q25). |
| CLR-UA-23 | Log Out ends the EIAM session (UAM-NFR-35; FR-UA-004) | Log Out ends the BIBS session, records it in the session log and sends the user to the EIAM sign-out page, so the BDO session ends too; after an inactivity sign-out the next sign-in goes through EIAM again. | Ending the Entra ID session also signs the user out of the other BDO applications opened with the same session. | BDOI IT to confirm that single sign-out is acceptable, and the re-authentication after an inactivity sign-out (UA-Q11, UA-Q12). |
| CLR-UA-24 | Break-glass administrators (FR-UA-003, 006) | Two named System Administrator accounts keep a local password and the BIBS second factor; their credentials are sealed by Information Security, every sign-in is alerted to Information Security and reviewed monthly; single sign-on is switched on only once these accounts exist. | BIBS must stay manageable when EIAM is not available. | Information Security to confirm the number, the holders and the review (UA-Q13). |
| CLR-UA-25 | Lock-out, second factor and BDO devices with EIAM (BRD 1.001.1, UAM-NFR-19; FR-UA-001, 003) | For users who sign in through EIAM, Entra ID enforces the password, the lock-out, the second factor and the BDO-device rule (conditional access); BIBS does not ask a second code after the single sign-on. The BIBS lock-out after 3 attempts and the BIBS second factor stay for local accounts. | With single sign-on BIBS no longer sees the password or the device. | BDOI IT and Information Security to confirm that the Entra ID policies meet the BRD rules (UA-Q10, UA-Q15). |
| CLR-UA-26 | Roles stay in BIBS with single sign-on (FR-UA-003) | Group profiles and permissions stay in BIBS and change only by BIBS requests; no Entra ID group is mapped to a BIBS role. EIAM may restrict who reaches BIBS through an assignment to a 'BIBS users' group kept by BDOI IT. | The BRD keeps every access change under four eyes in BIBS (p.6). | Confirm that roles are not taken from Entra ID groups (UA-Q09). |
| CLR-UA-27 | Acceptance criteria and NFR numbering written by the project team (all FRs; section 8) | The numbered acceptance criteria of each FR and the NFR IDs UAM-NFR-01 to 41 are the acceptance basis of BRD-11; rows without a Mandatory flag are treated as Must have; the duplicate IDs are marked "(2nd)". The appendix User-story view sets each BRD line next to its acceptance criteria. | The BRD has user-story lines but no acceptance criteria and no NFR IDs. | Business Administration and Information Security to accept the acceptance criteria and the numbering (UA-Q04). |

# Appendix: User-story view

One row per requirement of the User Access Maintenance BRD. The BRD writes its requirements as user stories; the stories below keep their meaning with the personas of this FRS. The acceptance criteria are the numbered criteria (AC) of each FR; the test cases are those of the test plan (file 04) that trace to the requirement.

<!-- table: widths=2.4,6.6,2.6,2.6,3.6 caption="User-story view: one row per BRD requirement" size=7.5 -->
| BRD ID | User story | FR | Acceptance criteria | Test cases |
|---|---|---|---|---|
| BRD 1.001.1 (p.8) | As a Requestor, I need to access the application using any BDO-issued device (Log in as a Requestor), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 1.001.1.1 (p.8) | As a Requestor, I need to log in with a Requestor user profile (Log in as a Requestor), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 1.001.1.2 (p.8) | As a Requestor, I need to receive the inactivity warning (15 minutes) (Log in as a Requestor), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 1.001.1.3 (p.8) | As a Requestor, I need to receive a warning prior to the system-triggered log out (30 minutes) (Log in as a Requestor), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 1.002.1 (p.8) | As a Requestor, I need to submit a new User Access request (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-011 | FR-UA-011 AC 1-3 | TC-UA-011.1-01 to 011.4-01 (9) |
| BRD 1.002.1.1 (p.8) | As a Requestor, I need to create the request (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.002.1.1.1 (p.8) | As a Requestor, I need to input the new user data (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-011 | FR-UA-011 AC 1-3 | TC-UA-011.1-01 to 011.4-01 (9) |
| BRD 1.002.1.1.2 (p.8) | As a Requestor, I need to select the User Access Group profile (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-011 | FR-UA-011 AC 1-3 | TC-UA-011.1-01 to 011.4-01 (9) |
| BRD 1.002.1.1.3 (p.8) | As a Requestor, I need to select the approver from the drop-down (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-015 | FR-UA-015 AC 1-2 | TC-UA-015.1-01 to 015.3-02 (4) |
| BRD 1.002.1.1.4 (p.8) | As a Requestor, I need to add remarks (for example justification) (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.002.1.1.5 (p.8) | As a Requestor, I need to save remarks (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.002.1.2 (p.8) | As a Requestor, I need to edit the drafted request (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.002.1.3 (p.8) | As a Requestor, I need to cancel the drafted request (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.002.1.4 (p.8) | As a Requestor, I need to submit the request (Enrol New User), so that a new employee gets the access the job needs, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1 (p.8) | As a Requestor, I need to submit a modify User Access / permission request (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-012 | FR-UA-012 AC 1-2 | TC-UA-012.1-01 to 012.3-03 (5) |
| BRD 1.003.1.1 (p.8) | As a Requestor, I need to create the request (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1.1.1 (p.8) | As a Requestor, I need to search the existing user (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-012 | FR-UA-012 AC 1-2 | TC-UA-012.1-01 to 012.3-03 (5) |
| BRD 1.003.1.1.2 (p.8) | As a Requestor, I need to edit or update the selected user data (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-012 | FR-UA-012 AC 1-2 | TC-UA-012.1-01 to 012.3-03 (5) |
| BRD 1.003.1.1.3 (p.8) | As a Requestor, I need to select the User Access Group profile (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-012 | FR-UA-012 AC 1-2 | TC-UA-012.1-01 to 012.3-03 (5) |
| BRD 1.003.1.1.4 (p.8) | As a Requestor, I need to select the approver from the drop-down (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-015 | FR-UA-015 AC 1-2 | TC-UA-015.1-01 to 015.3-02 (4) |
| BRD 1.003.1.1.5 (p.8) | As a Requestor, I need to add remarks (for example justification) (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1.1.6 (p.8) | As a Requestor, I need to save remarks (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1.2 (p.8) | As a Requestor, I need to edit the drafted request (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1.3 (p.8) | As a Requestor, I need to cancel the drafted request (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.003.1.4 (p.8) | As a Requestor, I need to submit the request (Modify Existing User), so that a user's access follows a change of job, decided by an approver. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1 (p.8) | As a Requestor, I need to submit a User Access deactivation request (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-013 | FR-UA-013 AC 1-2 | TC-UA-013.1-01 to 013.3-02 (4) |
| BRD 1.004.1.1 (p.8) | As a Requestor, I need to create a request (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1.1.1 (p.8) | As a Requestor, I need to search an existing user (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-013 | FR-UA-013 AC 1-2 | TC-UA-013.1-01 to 013.3-02 (4) |
| BRD 1.004.1.1.2 (p.8) | As a Requestor, I need to add remarks (for example reason for deactivation) (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1.1.3 (p.8) | As a Requestor, I need to save remarks (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1.1.4 (p.8) | As a Requestor, I need to select the approver from the drop-down (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-015 | FR-UA-015 AC 1-2 | TC-UA-015.1-01 to 015.3-02 (4) |
| BRD 1.004.1.2 (p.8) | As a Requestor, I need to edit the drafted request (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1.3 (p.8) | As a Requestor, I need to cancel the drafted request (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.004.1.4 (p.8) | As a Requestor, I need to submit the request (Deactivate User), so that a leaver, or a user who no longer needs access, loses it under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005 (p.8) | As a Requestor, I need to submit a User Access reactivation request (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-014 | FR-UA-014 AC 1 | TC-UA-014.1-01 to 014.2-01 (3) |
| BRD 1.005.1 (p.8) | As a Requestor, I need to create a request (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005.1.1 (p.8) | As a Requestor, I need to search an existing user (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-014 | FR-UA-014 AC 1 | TC-UA-014.1-01 to 014.2-01 (3) |
| BRD 1.005.1.1.1 (p.8) | As a Requestor, I need to add remarks (for example reason for reactivation) (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005.1.1.2 (p.8) | As a Requestor, I need to save remarks (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005.1.1.3 (p.8) | As a Requestor, I need to select the approver from the drop-down (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-015 | FR-UA-015 AC 1-2 | TC-UA-015.1-01 to 015.3-02 (4) |
| BRD 1.005.1.2 (p.8) | As a Requestor, I need to edit a drafted request (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005.1.3 (p.8) | As a Requestor, I need to cancel a drafted request (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.005.1.4 (p.8) | As a Requestor, I need to submit a request (Reactivate User), so that a returning user regains access under four eyes. | FR-UA-010 | FR-UA-010 AC 1-3 | TC-UA-010.1-01 to 010.5-01 (8), TC-UA-SCR-07, TC-UA-MSG-07 |
| BRD 1.006.1 (p.8) | As a Requestor, I need to apply correction on returned requests (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016 | FR-UA-016 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4) |
| BRD 1.006.1.1 (p.8) | As a Requestor, I need to receive notification on the returned request (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016, FR-UA-070 | FR-UA-016 AC 1-2; FR-UA-070 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4); TC-UA-070.1-01 to 070.3-01 (3) |
| BRD 1.006.1.2 (p.8) | As a Requestor, I need to edit data on the returned request (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016 | FR-UA-016 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4) |
| BRD 1.006.1.3 (p.8) | As a Requestor, I need to add remarks (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016 | FR-UA-016 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4) |
| BRD 1.006.1.4 (p.8) | As a Requestor, I need to save remarks (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016 | FR-UA-016 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4) |
| BRD 1.006.1.5 (p.8) | As a Requestor, I need to re-submit the request (Apply Correction), so that a returned request is corrected and decided without starting again. | FR-UA-016 | FR-UA-016 AC 1-2 | TC-UA-016.1-01 to 016.3-02 (4) |
| BRD 1.007.1 (p.8) | As a Requestor, I need to cancel a submitted request for approval (Cancel a Request), so that a request no longer needed is withdrawn before it is decided. | FR-UA-017 | FR-UA-017 AC 1-2 | TC-UA-017.1-01 to 017.4-01 (5), TC-UA-SCR-08, TC-UA-MSG-08 |
| BRD 1.007.1.1 (p.8) | As a Requestor, I need to search the request (Cancel a Request), so that a request no longer needed is withdrawn before it is decided. | FR-UA-017 | FR-UA-017 AC 1-2 | TC-UA-017.1-01 to 017.4-01 (5), TC-UA-SCR-08, TC-UA-MSG-08 |
| BRD 1.007.1.2 (p.8) | As a Requestor, I need to add remarks (for example reason for cancellation) (Cancel a Request), so that a request no longer needed is withdrawn before it is decided. | FR-UA-017 | FR-UA-017 AC 1-2 | TC-UA-017.1-01 to 017.4-01 (5), TC-UA-SCR-08, TC-UA-MSG-08 |
| BRD 1.007.1.3 (p.8) | As a Requestor, I need to save remarks (Cancel a Request), so that a request no longer needed is withdrawn before it is decided. | FR-UA-017 | FR-UA-017 AC 1-2 | TC-UA-017.1-01 to 017.4-01 (5), TC-UA-SCR-08, TC-UA-MSG-08 |
| BRD 1.007.1.4 (p.8) | As a Requestor, I need to cancel the submitted request (Cancel a Request), so that a request no longer needed is withdrawn before it is decided. | FR-UA-017 | FR-UA-017 AC 1-2 | TC-UA-017.1-01 to 017.4-01 (5), TC-UA-SCR-08, TC-UA-MSG-08 |
| BRD 1.008.1 (p.8) | As a Requestor, I need to view the submitted requests (View Requests), so that I know where each of my requests stands. | FR-UA-018 | FR-UA-018 AC 1-2 | TC-UA-018.1-01 to 018.3-01 (5), TC-UA-SCR-06 |
| BRD 1.008.1.1 (p.8) | As a Requestor, I need to view the list of the submitted requests (View Requests), so that I know where each of my requests stands. | FR-UA-018 | FR-UA-018 AC 1-2 | TC-UA-018.1-01 to 018.3-01 (5), TC-UA-SCR-06 |
| BRD 1.008.1.2 (p.8) | As a Requestor, I need to view the status of the submitted requests (View Requests), so that I know where each of my requests stands. | FR-UA-018 | FR-UA-018 AC 1-2 | TC-UA-018.1-01 to 018.3-01 (5), TC-UA-SCR-06 |
| BRD 1.008.1.3 (p.8) | As a Requestor, I need to view the details of the submitted requests (View Requests), so that I know where each of my requests stands. | FR-UA-018 | FR-UA-018 AC 1-2 | TC-UA-018.1-01 to 018.3-01 (5), TC-UA-SCR-06 |
| BRD 1.008.1.4 (p.8) | As a Requestor, I need to receive notification on the approved / rejected request (View Requests), so that I know where each of my requests stands. | FR-UA-018, FR-UA-070 | FR-UA-018 AC 1-2; FR-UA-070 AC 1-2 | TC-UA-018.1-01 to 018.3-01 (5), TC-UA-SCR-06; TC-UA-070.1-01 to 070.3-01 (3) |
| BRD 1.009.1 (p.8) | As a Requestor, I need to submit the request for the bulk creation, modification, deactivation or reactivation of users (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.1 (p.8) | As a Requestor, I need to attach a file for bulk processing (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.2 (p.8) | As a Requestor, I need to add remarks (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.3 (p.8) | As a Requestor, I need to save remarks (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.4 (p.8) | As a Requestor, I need to edit a drafted request (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.5 (p.8) | As a Requestor, I need to cancel a drafted request (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 1.009.1.6 (p.8) | As a Requestor, I need to submit a request (Bulk Request), so that many users are enrolled or changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| BRD 2.001.1 (p.8) | As an Approver, I need to access the application using any BDO-issued device (Log in as an Approver), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 2.001.1.1 (p.8) | As an Approver, I need to log in with an Approver user profile (Log in as an Approver), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 2.001.1.2 (p.8) | As an Approver, I need to receive the inactivity warning (15 minutes) (Log in as an Approver), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 2.001.1.3 (p.8) | As an Approver, I need to receive a warning prior to the system-triggered log out (30 minutes) (Log in as an Approver), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 2.002.1 (p.8) | As an Approver, I need to receive notification on the request for approval (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-030, FR-UA-070 | FR-UA-030 AC 1-2; FR-UA-070 AC 1-2 | TC-UA-030.1-01 to 030.3-01 (3); TC-UA-070.1-01 to 070.3-01 (3) |
| BRD 2.002.2 (p.8) | As an Approver, I need to view the list of requests for approval (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-030 | FR-UA-030 AC 1-2 | TC-UA-030.1-01 to 030.3-01 (3) |
| BRD 2.002.2.1 (p.8) | As an Approver, I need to filter the list assigned to me (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-030 | FR-UA-030 AC 1-2 | TC-UA-030.1-01 to 030.3-01 (3) |
| BRD 2.002.3 (p.8) | As an Approver, I need to select a request (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-030 | FR-UA-030 AC 1-2 | TC-UA-030.1-01 to 030.3-01 (3) |
| BRD 2.002.4 (p.8) | As an Approver, I need to view the request details (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-030 | FR-UA-030 AC 1-2 | TC-UA-030.1-01 to 030.3-01 (3) |
| BRD 2.002.5 (p.8) | As an Approver, I need to approve the request (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-031 | FR-UA-031 AC 1-2 | TC-UA-031.1-01 to 031.3-01 (4) |
| BRD 2.002.6 (p.8) | As an Approver, I need to reject the request (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-032 | FR-UA-032 AC 1-2 | TC-UA-032.1-01 to 032.2-02 (3) |
| BRD 2.002.6.1 (p.8) | As an Approver, I need to add remarks (for example rejection reason) (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-032 | FR-UA-032 AC 1-2 | TC-UA-032.1-01 to 032.2-02 (3) |
| BRD 2.002.6.2 (p.9) | As an Approver, I need to save remarks (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-032 | FR-UA-032 AC 1-2 | TC-UA-032.1-01 to 032.2-02 (3) |
| BRD 2.002.7 (p.9) | As an Approver, I need to return the request to the Requestor (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-033 | FR-UA-033 AC 1 | TC-UA-033.1-01 to 033.2-01 (2) |
| BRD 2.002.7.1 (p.9) | As an Approver, I need to add remarks (for example reason for return) (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-033 | FR-UA-033 AC 1 | TC-UA-033.1-01 to 033.2-01 (2) |
| BRD 2.002.7.1 (2nd) (p.9) | As an Approver, I need to save remarks (Review and Approve Requests), so that every access change is decided by a second person with the reason recorded. | FR-UA-033 | FR-UA-033 AC 1 | TC-UA-033.1-01 to 033.2-01 (2) |
| BRD 3.001.1 (p.9) | As a Business Administrator, I need to access the application using any BDO-issued device (Log in as a Business Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 3.001.1.1 (p.9) | As a Business Administrator, I need to log in with a Business Administrator user profile (Log in as a Business Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 3.001.1.2 (p.9) | As a Business Administrator, I need to receive the inactivity warning (15 minutes) (Log in as a Business Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 3.001.1.3 (p.9) | As a Business Administrator, I need to receive a warning prior to the system-triggered log out (30 minutes) (Log in as a Business Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 3.002.1 (p.9) | As a Business Administrator, I need to submit a new User Access Group Profiles request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.1 (p.9) | As a Business Administrator, I need to create a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.2 (p.9) | As a Business Administrator, I need to select approvers from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-044 | FR-UA-044 AC 1-2 | TC-UA-044.1-01 to 044.3-02 (5) |
| BRD 3.002.1.3 (p.9) | As a Business Administrator, I need to edit a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.4 (p.9) | As a Business Administrator, I need to cancel a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.5 (p.9) | As a Business Administrator, I need to submit a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.6 (p.9) | As a Business Administrator, I need to add remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.1.7 (p.9) | As a Business Administrator, I need to save remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-040 | FR-UA-040 AC 1-2 | TC-UA-040.1-01 to 040.3-01 (6), TC-UA-SCR-11, TC-UA-SCR-12, TC-UA-MSG-11 |
| BRD 3.002.2 (p.9) | As a Business Administrator, I need to submit a modify User Access Group Profiles request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.1 (p.9) | As a Business Administrator, I need to create a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.2 (p.9) | As a Business Administrator, I need to search the existing group profile from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.3 (p.9) | As a Business Administrator, I need to select approvers from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-044 | FR-UA-044 AC 1-2 | TC-UA-044.1-01 to 044.3-02 (5) |
| BRD 3.002.2.4 (p.9) | As a Business Administrator, I need to edit a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.5 (p.9) | As a Business Administrator, I need to cancel a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.6 (p.9) | As a Business Administrator, I need to submit a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.7 (p.9) | As a Business Administrator, I need to add remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.2.8 (p.9) | As a Business Administrator, I need to save remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-041 | FR-UA-041 AC 1-2 | TC-UA-041.1-01 to 041.3-01 (6) |
| BRD 3.002.3 (p.9) | As a Business Administrator, I need to submit a User Access Group Profiles deactivation request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.1 (p.9) | As a Business Administrator, I need to create a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.2 (p.9) | As a Business Administrator, I need to search the existing group profile from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.3 (p.9) | As a Business Administrator, I need to select approvers from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-044 | FR-UA-044 AC 1-2 | TC-UA-044.1-01 to 044.3-02 (5) |
| BRD 3.002.3.4 (p.9) | As a Business Administrator, I need to edit a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.5 (p.9) | As a Business Administrator, I need to cancel a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.6 (p.9) | As a Business Administrator, I need to submit a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.7 (p.9) | As a Business Administrator, I need to add remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.3.8 (p.9) | As a Business Administrator, I need to save remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-042 | FR-UA-042 AC 1-2 | TC-UA-042.1-01 to 042.3-02 (4) |
| BRD 3.002.4 (p.9) | As a Business Administrator, I need to submit a User Access Group Profiles reactivation request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.1 (p.9) | As a Business Administrator, I need to create a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.2 (p.9) | As a Business Administrator, I need to search the existing group profile from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.3 (p.9) | As a Business Administrator, I need to select approvers from the drop-down (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-044 | FR-UA-044 AC 1-2 | TC-UA-044.1-01 to 044.3-02 (5) |
| BRD 3.002.4.4 (p.9) | As a Business Administrator, I need to edit a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.5 (p.9) | As a Business Administrator, I need to cancel a drafted request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.6 (p.9) | As a Business Administrator, I need to submit a request (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.7 (p.9) | As a Business Administrator, I need to add remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.002.4.8 (p.9) | As a Business Administrator, I need to save remarks (Submit a User Access Group Profile Request), so that group profiles are created and changed only under approval and implemented by the System Administrator. | FR-UA-043 | FR-UA-043 AC 1 | TC-UA-043.1-01 to 043.2-01 (2) |
| BRD 3.003.1 (p.9) | As a Business Administrator, I need to generate a customised report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.1 (p.9) | As a Business Administrator, I need to generate the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.1 (2nd) (p.9) | As a Business Administrator, I need to see the User name column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.2 (p.9) | As a Business Administrator, I need to see the Group profile column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.3 (p.9) | As a Business Administrator, I need to see the Date created column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.4 (p.9) | As a Business Administrator, I need to see the Date modified column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.5 (p.9) | As a Business Administrator, I need to see the Created by column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.1.6 (p.9) | As a Business Administrator, I need to see the Added / Modified / deactivated / reactivated by column on the User Access Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-060 | FR-UA-060 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12 |
| BRD 3.003.2 (p.9) | As a Business Administrator, I need to generate the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.1 (p.9) | As a Business Administrator, I need to see the Group profile name column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.2 (p.9) | As a Business Administrator, I need to see the Modules accessed under each group profile column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.3 (p.9) | As a Business Administrator, I need to see the Date created column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.4 (p.9) | As a Business Administrator, I need to see the Date modified column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.5 (p.9) | As a Business Administrator, I need to see the Created by column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.2.6 (p.9) | As a Business Administrator, I need to see the Added / Modified / deactivated / reactivated by column on the User Group Profile Report (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-061 | FR-UA-061 AC 1 | TC-UA-061.1-01 to 061.2-01 (2) |
| BRD 3.003.3 (p.9) | As a Business Administrator, I need to generate the Group Profile Membership list (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-062 | FR-UA-062 AC 1 | TC-UA-062.1-01 to 062.2-01 (2) |
| BRD 3.003.3.1 (p.9) | As a Business Administrator, I need to generate the list of members under each profile (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-062 | FR-UA-062 AC 1 | TC-UA-062.1-01 to 062.2-01 (2) |
| BRD 3.003.3.1.1 (p.9) | As a Business Administrator, I need to see the Group profile column on the Group Profile Membership list (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-062 | FR-UA-062 AC 1 | TC-UA-062.1-01 to 062.2-01 (2) |
| BRD 3.003.3.1.2 (p.9) | As a Business Administrator, I need to see the User name column on the Group Profile Membership list (Generate Reports), so that the business and the auditors can review who holds which access. | FR-UA-062 | FR-UA-062 AC 1 | TC-UA-062.1-01 to 062.2-01 (2) |
| BRD 4.001.1 (p.9) | As the System Administrator, I need to access the application using any BDO-issued device (Log in as a System Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 4.001.1.1 (p.9) | As the System Administrator, I need to log in with a System Administrator user profile (Log in as a System Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| BRD 4.001.1.2 (p.9) | As the System Administrator, I need to receive the inactivity warning (15 minutes) (Log in as a System Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 4.001.1.3 (p.9) | As the System Administrator, I need to receive a warning prior to the system-triggered log out (30 minutes) (Log in as a System Administrator), so that only authorised staff on BDO devices reach BIBS and an unattended session is closed safely. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| BRD 4.002.1 (p.9) | As the System Administrator, I need to define a user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-045, FR-UA-050 | FR-UA-045 AC 1-2; FR-UA-050 AC 1 | TC-UA-045.1-01 to 045.3-01 (4), TC-UA-SCR-16, TC-UA-MSG-14; TC-UA-050.1-01 to 050.3-01 (3) |
| BRD 4.002.1.1 (p.9) | As the System Administrator, I need to create user profile or role of the Requestor (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-050 | FR-UA-050 AC 1 | TC-UA-050.1-01 to 050.3-01 (3) |
| BRD 4.002.1.2 (p.9) | As the System Administrator, I need to create user profile or role of the Approver (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-050 | FR-UA-050 AC 1 | TC-UA-050.1-01 to 050.3-01 (3) |
| BRD 4.002.1.3 (p.9) | As the System Administrator, I need to create user profile or role of the Business Administrator (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-050 | FR-UA-050 AC 1 | TC-UA-050.1-01 to 050.3-01 (3) |
| BRD 4.002.2 (p.9) | As the System Administrator, I need to define specific functionality or capability for a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.1 (p.9) | As the System Administrator, I need to assign the Enrol New User function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.2 (p.9) | As the System Administrator, I need to assign the Modify Existing User function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.3 (p.9) | As the System Administrator, I need to assign the Deactivate User function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.4 (p.9) | As the System Administrator, I need to assign the Reactivate User function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.5 (p.9) | As the System Administrator, I need to assign the Apply Correction function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.6 (p.9) | As the System Administrator, I need to assign the Cancel a Request function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.7 (p.10) | As the System Administrator, I need to assign the View Requests function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.8 (p.10) | As the System Administrator, I need to assign the Review and Approve Requests function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.9 (p.10) | As the System Administrator, I need to assign the Submit a User Access Group Profile Request function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.002.2.10 (p.10) | As the System Administrator, I need to assign the Generate Reports function to a specific user profile or role (Manage Users of the System), so that the user access roles and their functions are defined and kept under control. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| BRD 4.003.1 (p.10) | As the System Administrator, I need to generate audit logs for compliance and audit purposes as needed (Generate Logs), so that compliance and audit reviews have the evidence of every access change. | FR-UA-063, FR-UA-064 | FR-UA-063 AC 1-2; FR-UA-064 AC 1-2 | TC-UA-063.1-01 to 063.3-01 (4); TC-UA-064.1-01 to 064.2-01 (2) |
| UAM-NFR-06 (p.12) | As the Information Security Officer, I need four eyes on every access change, least privilege, separation-of-duties rules and the deactivation of dormant users, so that BIBS meets the BSP, AMLA and Data Privacy Act requirements. | FR-UA-006, FR-UA-053 | FR-UA-006 AC 1-2; FR-UA-053 AC 1-2 | TC-UA-006.1-01 to 006.3-01 (3); TC-UA-053.1-01 to 053.3-02 (5), TC-UA-SCR-19, TC-UA-MSG-16 |
| UAM-NFR-09 (p.13) | As the System Administrator, I need to maintain users and roles online, with several roles per user and several users per role, and to disable and re-enable accounts, so that access matches each job and every change is logged. | FR-UA-006, FR-UA-052, FR-UA-064 | FR-UA-006 AC 1-2; FR-UA-052 AC 1-2; FR-UA-064 AC 1-2 | TC-UA-006.1-01 to 006.3-01 (3); TC-UA-052.1-01 to 052.3-02 (4), TC-UA-SCR-15, TC-UA-MSG-13; TC-UA-064.1-01 to 064.2-01 (2) |
| UAM-NFR-10 (p.13) | As a Business Administrator, I need an exportable report of the roles assigned to users, so that access can be reviewed outside BIBS. | FR-UA-060, FR-UA-062 | FR-UA-060 AC 1-2; FR-UA-062 AC 1 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12; TC-UA-062.1-01 to 062.2-01 (2) |
| UAM-NFR-11 (p.13) | As a BIBS user, I need to sign in with my BDO directory account (EIAM, Microsoft Entra ID), so that I use one BDO identity and BIBS keeps no password for me. | FR-UA-003 | FR-UA-003 AC 1-4 | TC-UA-003.1-01 to 003.4-01 (5) |
| UAM-NFR-13 (p.13) | As a Requestor, I need user IDs checked against the BDO format (a999999999), so that every user ID follows the bank standard. | FR-UA-011, FR-UA-052 | FR-UA-011 AC 1-3; FR-UA-052 AC 1-2 | TC-UA-011.1-01 to 011.4-01 (9); TC-UA-052.1-01 to 052.3-02 (4), TC-UA-SCR-15, TC-UA-MSG-13 |
| UAM-NFR-14 (p.13) | As a Requestor, I need to give an effective date for a user or role change, so that the change applies on the agreed day. | FR-UA-020 | FR-UA-020 AC 1-2 | TC-UA-020.1-01 to 020.4-01 (4) |
| UAM-NFR-15 (p.14) | As the System Administrator, I need the Windows ID, user name, status, business unit group and user level on every user, so that each user is identified and classified. | FR-UA-004, FR-UA-052 | FR-UA-004 AC 1-3; FR-UA-052 AC 1-2 | TC-UA-004.1-01 to 004.4-01 (4), TC-UA-SCR-05, TC-UA-MSG-05; TC-UA-052.1-01 to 052.3-02 (4), TC-UA-SCR-15, TC-UA-MSG-13 |
| UAM-NFR-16 (p.14) | As the System Administrator, I need to maintain the access rights per group profile, the users, the groups and the reference tables, with a second approval of security settings, so that the security set-up stays under control. | FR-UA-052, FR-UA-054 | FR-UA-052 AC 1-2; FR-UA-054 AC 1-2 | TC-UA-052.1-01 to 052.3-02 (4), TC-UA-SCR-15, TC-UA-MSG-13; TC-UA-054.1-01 to 054.3-02 (5) |
| UAM-NFR-17 (p.14) | As a BIBS user, I need my Windows ID and network password accepted by the BDO authentication service, so that I sign in with my BDO account (proposed to be met by EIAM, CLR-UA-21). | FR-UA-003 | FR-UA-003 AC 1-4 | TC-UA-003.1-01 to 003.4-01 (5) |
| UAM-NFR-18 (p.14) | As the Information Security Officer, I need every valid and invalid sign-in attempt logged, so that misuse can be detected. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| UAM-NFR-19 (p.14) | As the Information Security Officer, I need an account locked after 3 invalid attempts, so that passwords cannot be guessed. | FR-UA-001 | FR-UA-001 AC 1-4 | TC-UA-001.1-01 to 001.5-01 (7), TC-UA-SCR-01, TC-UA-SCR-18, TC-UA-MSG-01, TC-UA-MSG-15 |
| UAM-NFR-22 (p.14) | As an Auditor, I need every user activity in an audit trail no user can change, so that the administrator can retrieve what was done. | FR-UA-064 | FR-UA-064 AC 1-2 | TC-UA-064.1-01 to 064.2-01 (2) |
| UAM-NFR-24 (p.15) | As the System Administrator, I need the batch jobs scheduled, run on demand, monitored and re-run from the screens, so that scheduled access changes run without a command line. | FR-UA-071 | FR-UA-071 AC 1 | TC-UA-071.1-01 to 071.2-02 (3) |
| UAM-NFR-25 (p.15) | As the System Administrator, I need an e-mail to the designated users when a batch run fails, so that a failure is handled the same day. | FR-UA-071 | FR-UA-071 AC 1 | TC-UA-071.1-01 to 071.2-02 (3) |
| UAM-NFR-31 (p.17) | As a BIBS user with a local account, I need to change my password and have it reset by the administrator, so that I keep access securely. | FR-UA-005 | FR-UA-005 AC 1-3 | TC-UA-005.1-01 to 005.5-01 (10), TC-UA-SCR-02, TC-UA-SCR-03, TC-UA-MSG-02, TC-UA-MSG-03 |
| UAM-NFR-32 (p.17) | As the System Administrator, I need to define roles, assign permissions and map users to roles, so that access is role-based. | FR-UA-051 | FR-UA-051 AC 1-2 | TC-UA-051.1-01 to 051.3-01 (3), TC-UA-SCR-13 |
| UAM-NFR-33 (p.17) | As a BIBS user, I need single sign-on with the BDO identity provider, so that I sign in once with my BDO account. | FR-UA-003 | FR-UA-003 AC 1-4 | TC-UA-003.1-01 to 003.4-01 (5) |
| UAM-NFR-34 (p.17) | As a BIBS user, I need a warning after 15 minutes of inactivity and a warning before the sign-out at 30 minutes, so that an unattended session is closed without losing my work by surprise. | FR-UA-002 | FR-UA-002 AC 1-2 | TC-UA-002.1-01 to 002.4-01 (5), TC-UA-SCR-04, TC-UA-MSG-04 |
| UAM-NFR-35 (p.17) | As an Auditor, I need the changes of roles and permissions and every sign-in and sign-out logged and reported, so that access can be audited. | FR-UA-004, FR-UA-063 | FR-UA-004 AC 1-3; FR-UA-063 AC 1-2 | TC-UA-004.1-01 to 004.4-01 (4), TC-UA-SCR-05, TC-UA-MSG-05; TC-UA-063.1-01 to 063.3-01 (4) |
| UAM-NFR-36 (p.17) | As the Information Security Officer, I need a strong password policy for local accounts, so that local passwords resist guessing. | FR-UA-005 | FR-UA-005 AC 1-3 | TC-UA-005.1-01 to 005.5-01 (10), TC-UA-SCR-02, TC-UA-SCR-03, TC-UA-MSG-02, TC-UA-MSG-03 |
| UAM-NFR-37 (p.17) | As a BIBS user, I need to reset a forgotten password myself and update my own contact details, so that I do not wait for the administrator. | FR-UA-005 | FR-UA-005 AC 1-3 | TC-UA-005.1-01 to 005.5-01 (10), TC-UA-SCR-02, TC-UA-SCR-03, TC-UA-MSG-02, TC-UA-MSG-03 |
| UAM-NFR-38 (p.17) | As a Requestor, I need to upload user changes in bulk, so that many users are changed at once under the same controls. | FR-UA-019 | FR-UA-019 AC 1-2 | TC-UA-019.1-01 to 019.4-01 (5), TC-UA-SCR-09, TC-UA-SCR-10, TC-UA-MSG-09, TC-UA-MSG-10, TC-UA-MSG-18 |
| UAM-NFR-39 (p.17) | As a BIBS user, I need to be told when my access changes, so that I know what I can now do. | FR-UA-070 | FR-UA-070 AC 1-2 | TC-UA-070.1-01 to 070.3-01 (3) |
| UAM-NFR-40 (p.17) | As the Information Security Officer, I need privileged and out-of-hours changes escalated for a second review and conflicting profiles refused, so that risky access changes get an additional check. | FR-UA-034, FR-UA-053, FR-UA-054 | FR-UA-034 AC 1-2; FR-UA-053 AC 1-2; FR-UA-054 AC 1-2 | TC-UA-034.1-01 to 034.3-02 (5); TC-UA-053.1-01 to 053.3-02 (5), TC-UA-SCR-19, TC-UA-MSG-16; TC-UA-054.1-01 to 054.3-02 (5) |
| UAM-NFR-41 (p.18) | As a Business Administrator, I need the four sample reports with who generated them, the date coverage and the time generated, so that the reports can be used as audit evidence. | FR-UA-060, FR-UA-061, FR-UA-063 | FR-UA-060 AC 1-2; FR-UA-061 AC 1; FR-UA-063 AC 1-2 | TC-UA-060.1-01 to 060.3-02 (5), TC-UA-SCR-14, TC-UA-SCR-17, TC-UA-MSG-12; TC-UA-061.1-01 to 061.2-01 (2); TC-UA-063.1-01 to 063.3-01 (4) |
| BRD p.6 (process diagram) - System Administrator to create / modify group profile | As the System Administrator, I need to create or modify a group profile once its request is approved, so that a group-profile change takes effect only after approval. | FR-UA-045 | FR-UA-045 AC 1-2 | TC-UA-045.1-01 to 045.3-01 (4), TC-UA-SCR-16, TC-UA-MSG-14 |
| BRD p.6 (process diagram) - System to grant the access | As a Requestor, I need BIBS to grant the access once a request is approved, so that an approved change applies without manual work and is logged. | FR-UA-031 | FR-UA-031 AC 1-2 | TC-UA-031.1-01 to 031.3-01 (4) |

# Appendix: Storyboard index

One row per step of the end-to-end walkthroughs (chapter End-to-end walkthroughs). Frame WT-A.3 is step 3 of walkthrough WT-A; the screenshot is the figure of the step in this FRS and the UX deck slide is the first slide of the step in the UX Screen Deck (file 07).

<!-- table: widths=1.3,2.2,2.5,4.0,4.0,1.9,2.2,1.3 caption="Storyboard index: one row per step" size=7 -->
| Frame | Persona | Screen | Action | What the user sees / outcome | Screenshot | FR | UX deck slide |
|---|---|---|---|---|---|---|---|
| WT-A.1 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Opens User Access › Access Requests › New Request, keeps Enrol new user, enters the user ID a013000196, the full name, e-mail, Windows ID and home branch, ticks Marketing Account Officer, chooses the approver Ulysses Access Approver, writes the justification and clicks Save Draft | Toast: AR-yyyy-nnnnnn saved as a draft; the request opens as Draft with Approver Chosen on submission (Draft request) | Figure 47 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 71 |
| WT-A.2 | Requestor | SCR-UA-08 Access Request record | Clicks Edit Request, checks the data and clicks Submit | Toast: AR-yyyy-nnnnnn submitted; the status is Pending Approval and the History tab lists Saved and Submitted (Pending Approval) | Figure 48 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 72 |
| WT-A.3 | Approver | SCR-UA-06 Access Requests | Opens the notice Access request AR-yyyy-nnnnnn to approve, or User Access › Access Requests | The request is on the Assigned to Me tab with its change: Create user a013000196 with group profiles Marketing Account Officer (Request found) | Figure 49 | FR-UA-018, FR-UA-030, FR-UA-031 | 73 |
| WT-A.4 | Approver | SCR-UA-08 Access Request record | Opens the request, reads the Details tab (current and requested values), clicks Approve and Apply, adds a comment and confirms | User a013000196 was created, with the temporary password shown once to be given through a secure channel; the status is Approved (Approved and applied) | Figure 50 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 74 |
| WT-A.5 | Marketing Account Officer | SCR-UA-01 Login | The new user opens BIBS and signs in with a013000196 and the temporary password | Change Your Password: Your password was set by an administrator. Choose your own password to continue. (Forced change) | Figure 51 | FR-UA-001, FR-UA-003 | 75 |
| WT-A.6 | Marketing Account Officer | SCR-UA-03 Change Your Password (forced change) | Enters the temporary password as the current password and a personal password twice, and clicks Change Password | Toast: Password changed; the home page opens (Signed in) | Figure 52 | FR-UA-005 | 77 |
| WT-A.7 | Marketing Account Officer | SCR-UA-05 My Profile | Opens the sidebar and My Profile | The sidebar holds exactly the entries of the Marketing Account Officer (chapter 12); My Profile lists the group profile Marketing Account Officer and its permissions, and the first session (Menus of the persona) | Figure 53 | FR-UA-004, FR-UA-005 | 78 |
| WT-A.8 | Auditor | SCR-UA-17 User access report (report runner) | Opens User Access Reports › User Access Audit Log for today, ticks Include Log-ins and Log-outs and clicks Run Report | Enroll New User a013000196 with the from and to values of the full name, e-mail and profiles, Done By and Approved By the Approver, the request number, and the Log-in of the new user (Evidence of the enrolment) | Figure 54 | FR-UA-060, FR-UA-061, FR-UA-062, FR-UA-063 | 79 |
| WT-B.1 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Opens a New Request of type Modify user for a013000196, ticks Marketing Team Leader / Head (approver) besides the current profile, chooses the approver, writes the justification and clicks Submit | The Group Profiles show (current) after Marketing Account Officer; toast AR-yyyy-nnnnnn submitted (Pending Approval) | Figure 55 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 83 |
| WT-B.2 | Approver | SCR-UA-08 Access Request record | Opens the request: the Details tab shows the group profiles added; clicks Approve and Apply | The status is Approved; the user receives Your access changed in the app and by e-mail (Change applied) | Figure 56 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 84 |
| WT-B.3 | Second approver | SCR-UA-08 Access Request record | Opens AR-2026-900008 from the Second Approval tab: the tag Privilege increase, first approved by the Approver; clicks Second Approval and confirms | The status is Approved; the alert Privileged access change stays in the Alert inbox as evidence (Second approval given) | Figure 57 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 85 |
| WT-B.4 | Business Administrator | SCR-UA-12 New Group Profile Request and Edit Request | Opens Group Profile Requests › New Group Profile Request, enters the code RENEWAL_ENQUIRY, the name and description, keeps the level Standard, ticks two permissions, adds the approvers Ulysses Access Approver then the New Business Approver, writes the justification and clicks Submit | Toast: AR-yyyy-nnnnnn submitted; the Approvers tab lists the two approvers in order (Pending Approval (first approver)) | Figure 58 | FR-UA-040, FR-UA-041, FR-UA-042, FR-UA-043, FR-UA-044, FR-UA-051 | 86 |
| WT-B.5 | Approver | SCR-UA-08 Access Request record | Approves the group-profile request | The status stays Pending Approval; the request moves to the New Business Approver, who receives the notice (Next approver) | Figure 59 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 87 |
| WT-B.6 | New Business Approver (keeps the access approval right) | SCR-UA-08 Access Request record | Opens the request from My Approvals and approves it | The status is For Implementation; the System Administrators receive Access request AR-yyyy-nnnnnn to implement (For implementation) | Figure 60 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 88 |
| WT-B.7 | System Administrator | SCR-UA-16 Roles & Permissions | Opens Roles & Permissions, finds the request under Approved Requests to Implement, clicks Implement Request and confirms | Toast: AR-yyyy-nnnnnn implemented; the profile appears in the matrix with its two permissions (Implemented) | Figure 61 | FR-UA-045, FR-UA-050, FR-UA-051 | 89 |
| WT-B.8 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Opens a New Request of type Deactivate user for a013000196, chooses the reason Resigned, leaves the effective date blank, chooses the approver and clicks Submit | Toast: AR-yyyy-nnnnnn submitted (Pending Approval) | Figure 62 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 90 |
| WT-B.9 | Approver | SCR-UA-08 Access Request record | Approves the deactivation | The status is Approved; the user is Disabled and every open session of the user has ended (User deactivated) | Figure 63 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 91 |
| WT-B.10 | System Administrator | SCR-UA-15 Users | Opens Administration › Users with the status Disabled | a013000196 is listed Disabled with the profiles it held; Sessions shows the last session ended by the administrator (Evidence of the revocation) | Figure 64 | FR-UA-052, FR-UA-004, FR-UA-064 | 92 |
| WT-C.1 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Opens New Request, enters the user ID ab and clicks Submit without a full name, profile, approver or justification | Under the fields: Use 3 to 50 letters, digits, dots, dashes or underscores; Enter the full name of the new user; Select at least one role; Select the approver; Enter the justification. Nothing is saved (Form corrected) | Figure 65 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 96 |
| WT-C.2 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Enters the user ID a01300019X with the other fields complete and clicks Submit | Under the user ID: The user ID must be a letter followed by nine digits, for example a013000196 (User ID corrected) | Figure 66 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 97 |
| WT-C.3 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Raises a Modify user request for a013000101, which already has a pending request, and clicks Submit | A request for user a013000101 is already waiting for approval, with its reference (Wait for the open request) | Figure 67 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 98 |
| WT-C.4 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Raises a Modify user request for himself (requestor), ticks an additional profile and clicks Submit | You cannot change your own roles (Another requester raises it) | Figure 68 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 99 |
| WT-C.5 | Requestor | SCR-UA-07 New Request and Edit Request (user requests) | Enrols a new user with both the User Access Requestor and the User Access Approver profiles and clicks Submit | One user may not hold both User Access Requestor and User Access Approver (separation-of-duties rule SOD-900001): the rule authorised by Information Security (One of the two profiles removed) | Figure 69 | FR-UA-010, FR-UA-011, FR-UA-012, FR-UA-013, FR-UA-014, FR-UA-015, FR-UA-016, FR-UA-020, FR-UA-053 | 100 |
| WT-C.6 | New Business Approver (keeps the access approval right) | SCR-UA-08 Access Request record | Opens AR-2026-900002, assigned to Ulysses Access Approver | The request shows no decision button for this approver; a decision would be refused: Request AR-2026-900002 is assigned to uamapprover for approval (Left to the chosen approver) | Figure 70 | FR-UA-017, FR-UA-018, FR-UA-030, FR-UA-031, FR-UA-032, FR-UA-033, FR-UA-034, FR-UA-045, FR-UA-064 | 101 |
| WT-C.7 | System Administrator | SCR-UA-16 Roles & Permissions | Clicks Implement Request on a group-profile request he raised himself | A request is implemented by someone other than its requester (Another System Administrator implements it) | Figure 71 | FR-UA-045, FR-UA-050, FR-UA-051 | 102 |
| WT-C.8 | Marketing Account Officer | SCR-UA-01 Login | User a013000101 enters a wrong password three times, then the right one | Invalid user name or password each time, also for the right password: a locked account is refused with the same message as a wrong password; Users shows the lock to the System Administrator (Account locked) | Figure 72 | FR-UA-001, FR-UA-003 | 103 |
| WT-C.9 | System Administrator | SCR-UA-15 Users | Opens Users with the status Locked and clicks Unlock on a013000101 | The user is Active again; the unlock is in the access change log (Account unlocked) | Figure 73 | FR-UA-052, FR-UA-004, FR-UA-064 | 105 |
| WT-C.10 | Marketing Account Officer | SCR-UA-01 Login | The deactivated user a013000196 tries to sign in | Invalid user name or password, the same message as a wrong password; the attempt does not count towards the lock-out (Refused) | Figure 74 | FR-UA-001, FR-UA-003 | 108 |
| WT-C.11 | Requestor | SCR-UA-05 My Profile | On My Profile, types a new password that is too short and has no symbol | Still needed: At least 10 characters; A symbol; Change Password stays disabled (Password rules shown) | Figure 75 | FR-UA-004, FR-UA-005 | 110 |
| WT-C.12 | Requestor | SCR-UA-05 My Profile | Enters the current password as the new password and clicks Change Password | You used this password recently. Choose another one (Another password chosen) | Figure 76 | FR-UA-004, FR-UA-005 | 111 |
| WT-D.1 | Auditor | SCR-UA-13 User Access Matrix | Opens User Access › User Access Matrix, By Action, and searches USER ACCESS | The User Access area by action class, with the permissions each profile holds: the Requestor creates and amends, the Approver approves, the Second Approver gives the second approval (Matrix reviewed) | Figure 77 | FR-UA-051, FR-UA-061 | 114 |
| WT-D.2 | Auditor | SCR-UA-17 User access report (report runner) | Runs the User Group Profile Report for the profile UAM_APPROVER | The modules and tasks of the Approver with With Access, and the dates and actors of its creation and last change (Profile content reviewed) | Figure 78 | FR-UA-060, FR-UA-061, FR-UA-062, FR-UA-063 | 115 |
| WT-D.3 | Auditor | SCR-UA-17 User access report (report runner) | Runs Group Profile Membership for MKT_AO as of today | The members of the Marketing Account Officer profile with who added each and when (Members reviewed) | Figure 79 | FR-UA-060, FR-UA-061, FR-UA-062, FR-UA-063 | 116 |
| WT-D.4 | Auditor | SCR-UA-17 User access report (report runner) | Runs the User Access Audit Log from the installation date with the activity Group Profile Changes | Deactivate Group Profile of the three insurer roles, Done By the system at installation, with their from and to values (Insurer roles deactivated) | Figure 80 | FR-UA-060, FR-UA-061, FR-UA-062, FR-UA-063 | 117 |
| WT-D.5 | Business Administrator | SCR-UA-12 New Group Profile Request and Edit Request | Opens New Group Profile Request of type New group profile and looks for the underwriting, insurer claims, reinsurance and reserve permissions | The permission picker lists no insurer permission; the Reactivate group profile list offers no insurer role (Nothing to request) | Figure 81 | FR-UA-040, FR-UA-041, FR-UA-042, FR-UA-043, FR-UA-044, FR-UA-051 | 118 |

# Appendix: Screen standards

The User Access Maintenance screens follow the screen standards of BIBS. They are listed here, in the same words in every FRS, so that BDOI can agree the look of the screens once and check the screenshots against it; they are not repeated in each screen specification.

<!-- table: widths=4.2,13.4 caption="Screen standards of BIBS" -->
| Area | Standard |
|---|---|
| Record pages | Back arrow and breadcrumb, title, then the record header: name, reference chips, status pill, tags (for example a client tag such as BDO employee) and the key facts. Page actions on the right in one order: secondary, primary, and the destructive action last and apart. |
| Workflow header (step bar) | Every record with a workflow shows a stepper under the record header: the stages of the main path in their order, passed stages ticked in blue, the current stage highlighted, the stages ahead in grey with their number. A returned or on-hold stage shows in amber after the stage it came from; a rejected, cancelled or voided record shows that stage in red and the path ends there; the last stage turns green when it is reached. Under the stepper one row gives Current Stage, Since, Due (with the Overdue pill) and Assigned To, with the actions of the stage on the right. The history is a table (Stage, From Stage, Action, By, Date and Time, Remarks, Duration in Stage), newest first. |
| Messages (notice standard) | One standard for errors, warnings, information and success: a white notice with a thin bar and an icon in the colour of its kind (red, amber, blue, green), a bold short title, then the business message with one bullet per missing item. No codes, internal references or technical terms in what the user reads. Field errors appear under the field; a long form lists its errors at the top with a link to the first field. Only an unexpected system error offers Retry and a reference for support behind Details. Special instructions are one short information notice. |
| Tables, not highlight boxes | Records and lists of records are rows of a titled table (for example the Rate Exceptions of a quotation: Exception No., Requested Rate, Scheme Rate, Difference, Valid Until, Requested By, Decided By, Status); key data is a label and value grid. A coloured box carries a short message only, never a record. |
| Tables | Header row in BDO blue that stays in view; rows alternate white and Background Blue, with a light hover and a stronger blue for the selected row; a list scrolls inside its card, the card as high as its rows and never higher than the window; text left, amounts right, dates in one format; one value per cell with at most one muted line under it; a dash for an empty value; the kind of a mixed list of records in a Type column; a due date on one line, in red with the warning icon once past due; ages and durations in words (for example 3 days); "Showing x to y of n results" with page numbers. |
| Status labels | Statuses are outlined pills (a border and text in the colour of the state group on a light tint) of one size and never wrap. The full label is shown (for example Returned to Marketing, Pending Authorization); an agreed short form, with the full label in the tooltip, only for a label longer than 21 characters. |
| Labels and names | List values are shown by their label, never by their code; products by name with the code as a second line; insurers by name; users by their names, never by their user ID. |
| Dates, amounts and rates | One date picker; dates entered and shown as dd-MMM-yyyy, times as dd-MMM-yyyy HH:mm (Philippine time). Amounts with two decimals and thousand separators, negatives in brackets, the currency in the column header or before the amount. Rates as percentages with two to four decimals, as keyed. |
| Period cell | A period (period of cover, package term, statement period) is shown in lists on two lines, the start date and "to" the end date, each date kept whole; an open end reads "to open". In a sentence or a label and value grid it is one line: "20-Oct-2026 to 20-Oct-2027". |
| Forms | Labels above the fields, fields of one height on one line, required fields marked, the error under the field, and the actions in one place (Cancel, then the main action). Format hints only; other guidance is in the tooltip of the label. |
| Uploads | Download Template next to the upload; a drop zone with the accepted types and maximum size; every row checked before anything is saved; Rows Read, Valid and Rejected with the rows; valid rows are processed and the rejected rows are returned in the error file (the template layout with an Error column and the wrong cells highlighted); Upload Corrected File keeps the link to the first upload; a file uploaded before is refused. |
| Confirmations and reasons | Every approve, authorise, post, release, cancel, void or deactivate asks for confirmation in a dialog that names the record and the effect. Every reject, return, cancel and void needs a reason (from its list where there is one) before it can be confirmed. Destructive actions are confirmed with the red button. The reason is kept in the history and sent with the notification. |
| Notifications | The bell shows the unread count and opens the panel grouped by day (Today, Yesterday, then the date), each notice with its title, one-line summary, record reference and time; Mark Read, Mark All Read and View All; the Notifications page lists every notice with filters. |
| Documents | Generated documents carry the BDO Insure letterhead, the document name and reference, and a business footer with "Confidential" and page x of y. |
| Four eyes | Nothing changes access until a second person approves; the screens offer a decision only to the users who may take it. |

The project team checks each screen against these standards before UAT and records the result in the screen readiness checklist. A screen found not to follow a standard during the review is recorded as Change requested in the sign-off workbook and corrected before UAT without a change request. The presentation choices that BDOI is asked to confirm are items of the clarifications chapter.
