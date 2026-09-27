---
# Source of the Functional Requirements Specification for BRD-3 Product Maintenance.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD03_PRODUCT_MAINTENANCE.md
title: Product Maintenance
subtitle: BRD-3 Product Maintenance (Package) and Workshop Addendum
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-03
name: Product Maintenance
doc_id: BIBS-FRS-BRD-03
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-3 Product Maintenance
output: FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-3 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the as-built screens and the cross-BRD decisions
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: Technical Support Unit (TSU), role: Business user, organisation: BDOI, purpose: Review of the negotiation and validation steps}
  - {name: "Retail, Corporate and Commercial Marketing", role: Business user, organisation: BDOI, purpose: Review of the request and approval steps}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Product Maintenance business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to build, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

## Scope

The scope is the maintenance of **package products**: pre-arranged insurance programmes whose terms, rates and conditions BDOI negotiates with one or more insurers and then offers to clients through Marketing. It covers the whole package lifecycle, from the Package Request Form to release, advisory, expiry and renewal, and the product master data that quotations, accounts and booking read.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access and audit | Log-in, role-to-action matrix, document protection, audit trail | BRPM.001, 002, 020, 024; PMADD05 |
| Product structure | Hierarchy (line, cover type, subtype, coverage / peril), clause library, product-specific field rules, shared templates, insurer-specific terms | BRPM.003-005; PMADD01, PMADD02 |
| Package request | Request form, Marketing approval, TSU TL review, TSU Head approval, work lists | BRPM.008, 009, 011, 021 |
| Negotiation | Quotation slip, insurer responses and exception outcomes, revision rounds, comparative master and client views, Marketing review | BRPM.010, 012-014; PMADD03, PMADD04 |
| Sign-off and set-up | Requirements pack, ManCom sign-off, MBS set-up, post-set-up validation, release, advisories, retirement | BRPM.015, 016; PMADD06 |
| Versions and rate schemes | Package versions, latest approved scheme for new business, rate-scheme exceptions, archive of expired packages | BRPM.006, 007 |
| Expiry and renewal | Expiry monitor, alerts, renewal requests | BRPM.017 |
| Monitoring | Product Maintenance home, package reports, notifications | BRPM.018, 019, 022 |
| Incentive criteria | Criteria (for example CPC2) on the products matrix | PMADD07, PMADD08 |

**Out of scope for this phase:**

- Maintenance of non-package products. The BRD revision log of 27-Oct-2025 (p.11) moved it to an addendum, and the April 2026 addendum does not add it (PQ19).
- Renewal of client policies (accounts). This is BRD-6 Renewal. BRD-3 covers the renewal of the *package* only.
- Computation and payout of incentives. PMADD07 and PMADD08 cover the criteria only (Q33).
- Transport of product master changes to other BDOI systems. The seam is built and parked (PQ16, section 7).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Product Maintenance (Package) BRD, pages 10-35 of the BRD-3 pack | v1.0, 24-Nov-2025; approved Nov-2025 to Jan-2026 | `docs/source-documents/Product Maintenance.pdf` |
| R2 | Product Maintenance Addendum (Workshop), pages 1-9 of the BRD-3 pack | v1.1, 10-Apr-2026; signed 12 to 17-Apr-2026 | same file |
| R3 | Earlier signed version of the main BRD (BRQID numbering), pages 36-61; used only to trace changes | 16-Nov-2025 | same file |
| R4 | BDOI Product Maintenance (BRD-3) requirements baseline and fit/gap | current | `docs/requirements/BDOI_PM_BRD_SPEC.md` |
| R5 | Product Maintenance build design, including the as-built notes of sections 15-17 | current | `docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R7 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |
| R8 | BRD-1 New Business requirements baseline (shared platform capabilities) | current | `docs/requirements/BDOI_NB_BRD_SPEC.md` |

Page references in this document ("p.18") are pages of the BRD-3 PDF (R1, R2).

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing)
BRD: Business Requirements Document
BRPM: Requirement ID prefix of the main Product Maintenance BRD (BRPM.001-BRPM.024; there is no BRPM.023)
PMADD: Requirement ID prefix of the Product Maintenance Addendum (PMADD01-PMADD08)
Comparative master: The one current comparative table of a package request, compiled from the insurer responses of the latest round (BRPM.014)
Client view: A comparative output derived from the current master with selected fields and insurers, for a client presentation (PMADD03)
CPC2: An incentive criterion named in PMADD08 as an example; its definition is not in the BRD (PQ04)
FR: Functional requirement of this document (FR-PM-nnn)
LOB: Line of business; in BIBS the product line (for example Motor, Fire)
LOV: List of values maintained by the System Administrator
ManCom: Management Committee, which signs off package requirements (BRPM.015)
MBS: Marketing Business Services and System Support; sets up packages in the system
Package: A catalogue product flagged as packaged, with pre-negotiated terms, rates and insurers; generic for a segment or client-specific
Package version: One effective-dated set of package terms (rate scheme, coverages, insurers, dates); changed only through a new version
PKR: Prefix of package request numbers (PKR-yyyy-n)
PQS: Prefix of package quotation slip numbers (PQS-yyyy-n)
PQnn: Open question on BRD-3 raised by the project team (section 10.3)
QS: Quotation Slip sent to insurers
Rate scheme: The package rate, minimum premium, commission and TSI limit of a package version
SLA: Service level agreement; here the target hours of a workflow stage
TH: Team Head
TL: Team Lead
TSI: Total sum insured
TSU: Technical Support Unit
UAT: User acceptance testing
UH: Unit Head (Marketing)
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R4), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the System Administrator or the business owner in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check (for example a blank mandatory field); its message follows the same wording but has no business code.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-3 test plan.

> [!NOTE]
> Values marked "default" (SLA hours, notice days, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the platform built for BRD-1 |
| CONFIGURE | Needs set-up only (parameters, templates, rules) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-3 |

# Business context and process overview

## Business context

BDOI sets up package products today through e-mail, Excel comparative tables, manual sign-off and spreadsheets for expiry monitoring (p.12-13). The BRD asks for one maintenance module in which MBS / Admin and TSU manage package products without IT, with automatic audit, enforced approvals, secure document sharing and monitoring of expiring packages (p.12).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.12-13)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Marketing requests placements by e-mail using a TSU request form | Marketing logs the Package Request Form in BIBS; the system numbers it (PKR-yyyy-n) and tracks it |
| 2 | TSU prepares and e-mails quotation slips to insurers | TSU prepares the QS in BIBS; it is approved and e-mailed per insurer, protected and logged |
| 3 | Insurers reply by e-mail with signed slips or approved terms | TSU keys in each response with its outcome; every change is kept |
| 4 | Comparative tables are compiled in Excel and shared by e-mail | BIBS compiles one comparative master per package and derives client views from it |
| 5 | Sign-off and advisories are manual | ManCom signs off in BIBS; the advisory is drafted on release and sent with its documents |
| 6 | Shared documents have no password protection | Documents leave BIBS password protected; the password goes in a separate e-mail |
| 7 | Package expiry is monitored in spreadsheets or calendars | A daily job raises expiry alerts and can draft the renewal request |
| 8 | Changes, approvals and documents are tracked in e-mail threads and files | Every action is in the request history and the audit trail |

## Process overview

The table lists the steps of the package lifecycle, and Figure 1 shows them by actor. Each step is a stage of the BIBS workflow PM_PACKAGE_REQUEST (section 5).

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Package Request Form | Marketing AO or TSU | Enters client or programme, line, cover type, requested terms and target insurers; submits | BRPM.008 |
| 2 | Marketing approval | Marketing TL, TH or UH | Approves or returns; never the maker | BRPM.008, 021 |
| 3 | TSU review and approval | TSU TL, then TSU TH | TL recommends with a written recommendation; TH approves; a round 1 opens | BRPM.009 |
| 4 | Negotiation | TSU Officer / TL | QS approved and sent per insurer; responses and outcomes keyed in; further rounds as needed | BRPM.010, 012, 013; PMADD04 |
| 5 | Comparative tables | TSU | One current master per request; client views derived from it | BRPM.014; PMADD03 |
| 6 | Marketing review | Marketing | Client-specific packages only: accept terms or request changes | BRPM.013 |
| 7 | Requirements pack | TSU | Scheme rate, dates, computation basis, insurers, signed package slip | BRPM.015 |
| 8 | ManCom sign-off | ManCom | Signs off or returns; the quotation slips lock | BRPM.015 |
| 9 | Package set-up | MBS | Creates the draft package version from the signed-off terms, or returns the pack as incomplete | BRPM.015 |
| 10 | Validation and release | TSU Head or Business Administrator | Confirms the checklist and releases the version, or returns it | PMADD06 |
| 11 | Advisory | TSU / Operations | Advisory drafted on release; sent only with the signed package slip and ManCom sign-off | BRPM.016 |
| 12 | Monitoring and renewal | TSU Officer, MBS | Expiry alerts; a renewal request pre-filled from the current version | BRPM.017, 006 |

![Package lifecycle by actor (BRPM.008-017, PMADD03/04/06)](figures/brd03_process_flow.dot)

## Request types

A package request has one of six types (list PKG_REQUEST_TYPE). The type decides whether negotiation takes place and what the MBS step does.

<!-- table: widths=2.4,4,3.6,7 caption="Request types" -->
| Type | Label | Negotiation | Result |
|---|---|---|---|
| NEW | New package | Always | A new packaged product and its version 1 |
| AMEND | Amend package terms | Always | A new version of an existing package |
| UPDATE | Update package details | Chosen on the form | A new version of an existing package |
| RENEW | Renew package | Chosen on the form | A new version with the next term dates |
| REACTIVATE | Reactivate expired package | Chosen on the form | A new version of an expired package; the product becomes active again |
| RETIRE | Retire package | Never | The product is retired; no version is released (the BRD "deletion", BRPM.011) |

# Personas and roles

## Personas

<!-- table: widths=3.2,3.3,8.1,3 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in Product Maintenance | BRD |
|---|---|---|---|
| Marketing AO | MKT_AO | Creates and submits package requests; views packages | BRPM.008 |
| Marketing TL / TH / UH | MKT_TL | Approves package requests; reviews terms of client-specific packages; views reports | BRPM.008, 013, 021 |
| TSU Officer | TSU | Creates requests; prepares quotation slips; keys in insurer responses; prepares requirements and advisories; monitors expiry | BRPM.010-013, 016, 017 |
| TSU Team Lead | TSU_TL | Reviews and recommends requests; approves quotation slips (or a co-officer); negotiates | BRPM.009, 012 |
| TSU Team Head / Head | TSU_HEAD | Approves requests; releases terms to Marketing; validates package versions | BRPM.009, 013; PMADD06 |
| MBS | MBS | Sets up package versions; maintains incentive criteria and product masters; views archived packages | BRPM.015; PMADD07, 08 |
| ManCom | MANCOM | Signs off package requirements | BRPM.015 |
| Business Administrator | BUSINESS_ADMIN | Validates package versions; authorises product masters, incentive criteria and rate exceptions | PMADD05, 06 |
| System Administrator | SYSADMIN | Maintains parameters, lists of values and roles (section 9); read access to Product Maintenance | PMADD05 |
| Auditor | AUDITOR | Read access to packages, archive and reports | BRPM.024 |

The addendum states that user roles "will be further defined in succeeding documentations" (p.4, footnote 2). The roles above are the project's proposal until BDOI confirms the matrix (PQ17, OQ48).

## Permissions

<!-- table: widths=4.6,2.6,9.6 caption="Product Maintenance permissions and action classes (PMADD05)" -->
| Permission | Action class | Allows |
|---|---|---|
| PRODUCT_VIEW | VIEW | Open the Product Maintenance screens and the catalogue (read only) |
| PRODUCT_ARCHIVE_VIEW | VIEW | See expired and retired packages and their history (BRPM.006) |
| PKG_REPORT_VIEW | VIEW | Package reports and the Product Maintenance home |
| PKG_REQUEST | CREATE / AMEND | Create, edit and submit package requests; Marketing review of terms |
| PKG_REQUEST_APPROVE | APPROVE | Marketing approval of a package request |
| PKG_TSU_RECOMMEND | APPROVE | TSU TL review and recommendation |
| PKG_TSU_APPROVE | APPROVE | TSU TH approval; release of terms to Marketing |
| PKG_NEGOTIATE | CREATE / AMEND | Quotation slips, insurer responses, rounds, comparative outputs, requirements pack |
| PKG_QS_APPROVE | APPROVE | Approval of a quotation slip (not by its preparer) |
| PKG_MANCOM_SIGNOFF | APPROVE | ManCom sign-off |
| PKG_ADVISORY | CREATE | Draft and send advisories |
| PRODUCT_MAINTAIN | CREATE / AMEND | Package set-up (draft versions), coverages, clauses |
| PRODUCT_VALIDATE | APPROVE | Validation and release of package versions (PMADD06) |
| PRODUCT_AUTHORIZE | APPROVE | Authorisation of product masters, incentive criteria and rate-scheme exceptions |
| INCENTIVE_CRITERIA_MAINTAIN | CREATE / AMEND | Incentive criteria (PMADD07, 08) |

## Permissions matrix

The table below is the role-to-action matrix delivered with the build ("Y" = granted). The System Administrator maintains it through role-permission change requests (FR-PM-003). The User Access Matrix screen shows it by permission and by action class and exports it to Excel.

<!-- table: widths=4.7,1.29,1.29,1.29,1.29,1.29,1.29,1.29,1.29,1.29,1.29 caption="Role-to-action matrix for Product Maintenance (proposal until PQ17 / OQ48)" size=8 -->
| Permission | MKT AO | MKT TL | TSU | TSU TL | TSU Head | MBS | Man Com | Bus. Admin | NB Appr. | Auditor |
|---|---|---|---|---|---|---|---|---|---|---|
| PRODUCT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| PRODUCT_ARCHIVE_VIEW | | | | | Y | Y | | Y | | Y |
| PKG_REPORT_VIEW | | Y | | Y | Y | Y | | Y | | Y |
| PKG_REQUEST | Y | Y | Y | | | | | | | |
| PKG_REQUEST_APPROVE | | Y | | | | | | | | |
| PKG_TSU_RECOMMEND | | | | Y | | | | | | |
| PKG_TSU_APPROVE | | | | | Y | | | | | |
| PKG_NEGOTIATE | | | Y | Y | | | | | | |
| PKG_QS_APPROVE | | | | Y | | | | | | |
| PKG_MANCOM_SIGNOFF | | | | | | | Y | | | |
| PKG_ADVISORY | | | Y | Y | | | | | | |
| PRODUCT_MAINTAIN | | | | | | Y | | | | |
| PRODUCT_VALIDATE | | | | | Y | | | Y | | |
| PRODUCT_AUTHORIZE | | | | | | | | Y | Y | |
| INCENTIVE_CRITERIA_MAINTAIN | | | | | | Y | | | | |

Segregation of duties is enforced by the system, whatever the role grants: a request is never approved by its maker, the TSU Head approval is never given by the recommender, a quotation slip is never approved by its preparer, ManCom never signs off requirements it submitted, and a package version is never validated by its maker or submitter.

# Functional requirements

## Access, security and audit

```fr
id: FR-PM-001
title: Log in to BIBS
brd: [BRPM.001 (p.15)]
actor: MBS, TSU, Marketing
priority: Must have
fit: FIT
screens: Login
api: POST /api/v1/auth/login
description: Users of MBS, TSU and Marketing log in with their BIBS user ID and password. Product Maintenance uses the platform log-in built for BRD-1; it adds no screen of its own.
preconditions:
  - The user has an active BIBS account with at least one Product Maintenance role.
main_flow:
  - The user enters the user ID and password on the Login screen.
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the menu of the user's roles (Product Maintenance section).
alternate_flows:
  - Wrong credentials. BIBS refuses the log-in and counts the failed attempt.
  - Locked account. After the configured number of failed attempts the account locks; the System Administrator unlocks it.
  - Idle session. The session ends after the configured idle time; the user logs in again.
rules:
  - [R1, "Log-in, lock-out and session rules follow the session policy of BRD-1 (BRNB.040).", Configurable, Session policy (System Administrator)]
  - [R2, "BDO single sign-on / Active Directory is not part of this phase (Q42).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded with user, time and source address.
acceptance:
  - A user with valid credentials and a Product Maintenance role logs in and sees the Product Maintenance menu section.
  - A user with wrong credentials is refused and no session is created.
  - The account locks after the configured number of failed attempts.
```

```fr
id: FR-PM-002
title: Restrict each action to authorised roles
brd: [BRPM.002 (p.15), PMADD05 (p.5)]
actor: System
priority: Must have
fit: CHANGE
screens: All Product Maintenance screens; User Access Matrix
api: Every endpoint checks its permission; GET /api/v1/nbadmin/access-matrix/by-action
description:
  - Every Product Maintenance screen, button and API call requires a permission (section 3.2). Permissions carry an action class (VIEW, CREATE, AMEND, APPROVE), so the matrix answers the addendum's question "who may create, amend, approve or only view" per area.
  - Menus show only the screens that the user's roles allow. Buttons for actions the user may not perform are hidden.
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a screen or starts an action.
  - BIBS checks the user's permissions for that screen or action.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link returns "You do not have access to this page". An API call is refused (HTTP 403) and logged.
  - Segregation of duties. An action refused by a four-eyes rule (for example approving one's own request) is refused with its message even when the role has the permission.
rules:
  - [R1, "Each permission has an action class: VIEW, CREATE, AMEND or APPROVE (table sec_permission_action).", Configurable, Role-permission change request (FR-PM-003)]
  - [R2, Roles are granted permissions as in section 3.3 until BDOI confirms the matrix (PQ17 / OQ48)., Configurable, Role-permission change request]
  - [R3, "Four-eyes rules (maker never approves) cannot be switched off.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: User Access Matrix
fields:
  - [View, Toggle, "Yes", By Permission / By Action, "-"]
  - [Area, List, "No", "Areas of sec_permission_action (for example PRODUCT_MAINTENANCE, PACKAGE_REQUEST, INCENTIVES)", "-"]
notifications:
  - "None."
audit:
  - Refused API calls are logged with user, endpoint and time.
  - The Excel export of the matrix is audited.
acceptance:
  - A Marketing AO sees Product Maintenance Home, Package Requests and the catalogue screens, and does not see the Validation Queue or TSU Workbench.
  - A direct call to an approval endpoint by a user without the approval permission is refused and logged.
  - The User Access Matrix shows, for each role, which Product Maintenance areas it may view, create, amend and approve, and exports both views to Excel.
```

```fr
id: FR-PM-003
title: Change role permissions through an approved request
brd: [PMADD05 (p.5)]
actor: Business Administrator (requester), access approver, System Administrator
priority: Must have
fit: CHANGE
screens: Access Requests; User Access Matrix
api: POST /api/v1/nbadmin/access-requests (type MODIFY_ROLE_PERMISSIONS)
description: Changes to the permissions of a role are requested, approved by someone other than the requester and then applied. This meets PMADD05 AC2 (changes auditable and traceable) and AC3 (Admin maintains roles and permissions). BRD-11 User Access Maintenance confirmed that group-profile changes are approved before the System Administrator implements them (PQ17, R6).
preconditions:
  - "The requester has the access-request permission."
main_flow:
  - The requester opens Access Requests and chooses the type "Modify role permissions".
  - The requester selects the role, the permissions to add and to remove, and writes the justification.
  - The approver reviews and approves the request.
  - BIBS applies the change to the role as it is at approval and records the effective change.
alternate_flows:
  - The approver rejects the request with a reason; the role does not change.
  - Emergency change. The direct role edit of the System Administrator stays available only through the audited emergency parameter UAM_DIRECT_ROLE_EDIT (BRD-11).
rules:
  - [R1, The approver is never the requester., Fixed, "-"]
  - [R2, "The change applied is the difference between the role at approval and the request, so concurrent changes are not lost.", Fixed, "-"]
validations:
  - [No permission added or removed, Select at least one permission to add or remove, "-"]
  - [Approver is the requester, An access request is approved by someone other than the requester, ACCESS_FOUR_EYES]
fields_screen: Access Request (Modify role permissions)
fields:
  - [Role, List, "Yes", Roles, Active role]
  - [Permissions to add, Multi-select, "No", Permissions, Not already granted]
  - [Permissions to remove, Multi-select, "No", Permissions of the role, Granted to the role]
  - [Justification, Text, "Yes", "-", Up to 1000 characters]
notifications:
  - The approvers are notified when the request is submitted; the requester when it is decided.
audit:
  - Request, decision and the applied change are recorded with users and times.
acceptance:
  - A role change requested by the Business Administrator takes effect only after another user approves it.
  - The history of the role shows who requested, approved and applied each change.
```

```fr
id: FR-PM-004
title: Protect documents that leave BIBS
brd: [BRPM.020 (p.29-30)]
actor: System
priority: Must have
fit: FIT
screens: Package Request page (E-mails, Documents tabs)
api: Messaging outbox (internal)
description: The quotation slips to insurers, comparative outputs, package slip and advisories are e-mailed as password-protected PDF (AES) or protected XLSX, with the password in a separate e-mail. Inside BIBS, package documents open only for users with PRODUCT_VIEW or the Product Maintenance permissions of the request.
preconditions:
  - "A document is generated or attached on a package request."
main_flow:
  - A user sends a document from a package request (QS, comparative, advisory).
  - BIBS protects the file and sends it; it sends the password in a second e-mail.
  - BIBS logs both e-mails in the request's E-mails tab.
alternate_flows:
  - Delivery failure. The e-mail shows the failed status in the E-mails tab; the user resends it.
rules:
  - [R1, "Every Product Maintenance document e-mailed outside BIBS is protected; protection cannot be switched off per e-mail.", Fixed, "-"]
  - [R2, "The password convention is the one BDOI confirms under Q07; until then BIBS generates a password per e-mail.", Configurable, Messaging settings (parked Q07 / PQ21)]
validations:
  - [User without permission opens a document, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "The recipient receives the protected document and, separately, its password."
audit:
  - Each e-mail is logged with recipient, time, subject, attachment name and SHA-256 hash of the attachment.
acceptance:
  - A QS e-mailed to an insurer arrives as a protected PDF, and the password arrives in a separate e-mail.
  - A user without Product Maintenance permissions cannot open the request's documents.
```

```fr
id: FR-PM-005
title: Keep a complete audit trail and history
brd: [BRPM.024 (p.31-32)]
actor: System; any user with PRODUCT_VIEW (read)
priority: Must have
fit: FIT
screens: Package Request page (History tab); Product page (History); Package Version page
api: GET /api/v1/product-maintenance/requests/{id} (history); audit service
description: BIBS records every creation, edit, approval, return, sending and generation for package requests, package versions, masters and incentive criteria. Users review the complete trail of a request or document on its History tab.
preconditions:
  - "None."
main_flow:
  - A user performs an action on a request, version or master.
  - BIBS writes an audit row (who, when, what, before and after values where applicable) in the same transaction.
  - The History tab shows the workflow history and the audit rows in time order.
rules:
  - [R1, "Audit rows are append-only; no user, including the System Administrator, can change or delete them.", Fixed, "-"]
  - [R2, "Retention follows the BRD-1 rule (5 years online, 15 years archive) until BDOI states a Product Maintenance retention (PQ18).", Configurable, Retention parameters]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - For a released package, the History tab lists every step from submission to release with user and time.
  - A changed insurer response shows its earlier values in the response history.
  - No screen or API allows audit rows to be edited or deleted.
```

## Product structure and master data

```fr
id: FR-PM-010
title: Maintain the product hierarchy
brd: [PMADD01 (p.4)]
actor: MBS (maintain), Business Administrator (authorise)
priority: Must have
fit: CHANGE
screens: Products; Product page; Coverages & Clauses
api: /api/v1/catalog/lines, /cover-types, /coverages, /products
description:
  - BIBS holds one product hierarchy for all packages. Level 1 is the product line (LOB, for example Motor, Fire). Level 2 is the cover type, with an optional subtype under it (depth at most 2). Level 3 is the product (risk code). Level 4 is the coverage / peril of the line, used in each package version.
  - A packaged product cannot be saved without its cover type, and a package version cannot be submitted without at least one basic coverage. New risk codes must follow the naming convention of their line when BDOI has stated it.
preconditions:
  - "The user has PRODUCT_MAINTAIN."
main_flow:
  - MBS creates or edits a line, cover type or subtype, coverage or product.
  - BIBS checks the hierarchy and the naming convention.
  - The record is saved as pending authorisation.
  - A PRODUCT_AUTHORIZE holder other than the maker authorises it; it becomes usable.
alternate_flows:
  - The authoriser rejects the record with a reason; it stays unusable and the maker can correct it.
  - A subtype is created under a subtype. BIBS refuses it (depth at most 2).
rules:
  - [R1, "Hierarchy levels: line > cover type > subtype (optional) > product; coverages / perils per line.", Fixed, "-"]
  - [R2, "A packaged product needs a cover type.", Fixed, "-"]
  - [R3, "Risk codes follow the pattern of their line (for example ^MTR\\d{2}$) when the line has one.", Configurable, "Product line, Code Pattern (values PQ02)"]
  - [R4, "Coverage kinds: Section, Coverage, Peril, Extension.", Configurable, LOV COVERAGE_KIND]
  - [R5, "Every hierarchy record is maker-checker.", Fixed, "-"]
validations:
  - [Packaged product without cover type, A package needs its cover type (PMADD01), PACKAGE_HIERARCHY_INCOMPLETE]
  - [Risk code does not match the line pattern, "Risk code <code> does not follow the naming convention of <line>", PRODUCT_CODE_PATTERN]
fields_screen: Coverage (Coverages & Clauses)
fields:
  - [Product line, List, "Yes", Product lines, Active line]
  - [Code, Text, "Yes", "-", "Unique within the line; letters, digits, hyphen"]
  - [Name, Text, "Yes", "-", Up to 120 characters]
  - [Kind, List, "Yes", LOV COVERAGE_KIND, "-"]
  - [Basic coverage, Check box, "No", "-", "-"]
  - [Display order, Number, "No", "-", Whole number]
notifications:
  - "The authorisers see pending records in My Approvals."
audit:
  - "Create, edit, authorise, reject and deactivate are audited with before and after values."
acceptance:
  - A packaged product without a cover type cannot be saved.
  - A risk code that does not follow its line's pattern is refused with PRODUCT_CODE_PATTERN.
  - A new coverage is usable in package versions only after another user authorises it.
```

```fr
id: FR-PM-011
title: Maintain the clause library
brd: [PMADD02 (p.4)]
actor: MBS / TSU with PRODUCT_MAINTAIN; Business Administrator (authorise)
priority: Must have
fit: NEW
screens: Coverages & Clauses (Clauses)
api: /api/v1/catalog/clauses
description: Warranties, clauses, exclusions and deductible wordings are held once in a clause library with their wording and effective dates. Package versions refer to clauses by code for each insurer and coverage (FR-PM-014).
preconditions:
  - "The user has PRODUCT_MAINTAIN."
main_flow:
  - The user adds a clause with its kind, line (or all lines), title, wording and effective dates.
  - The clause is saved as pending authorisation.
  - Another user with PRODUCT_AUTHORIZE authorises it.
rules:
  - [R1, "Clause kinds: Warranty, Clause, Exclusion, Deductible wording.", Configurable, LOV CLAUSE_KIND]
  - [R2, "A clause is never deleted; it is end-dated or deactivated.", Fixed, "-"]
validations:
  - [Wording blank, Enter the wording, "-"]
  - [Effective to before effective from, The end date must be on or after the start date, "-"]
fields_screen: Clause
fields:
  - [Code, Text, "Yes", "-", Unique]
  - [Kind, List, "Yes", LOV CLAUSE_KIND, "-"]
  - [Product line, List, "No", Product lines, Blank = all lines]
  - [Title, Text, "Yes", "-", Up to 200 characters]
  - [Effective from, Date, "Yes", "-", "-"]
  - [Effective to, Date, "No", "-", On or after effective from]
  - [Wording, Long text, "Yes", "-", "-"]
notifications:
  - "Pending clauses appear in My Approvals."
audit:
  - "All changes audited with before and after values."
acceptance:
  - A clause is selectable in insurer terms only after authorisation and within its effective dates.
  - An end-dated clause stays readable on the versions that used it.
```

```fr
id: FR-PM-012
title: Capture and validate product-specific fields
brd: [BRPM.003 (p.16), BRPM.004 (p.16)]
actor: MBS (rules); System (checks)
priority: "BRPM.003: not stated in the BRD; BRPM.004: Must have"
fit: "CONFIGURE (BRPM.003), CHANGE (BRPM.004)"
screens: Product page (Field Rules & Documents tab)
api: /api/v1/catalog/field-rules
description:
  - MBS maintains, per line or per product, which fields a quotation or account must capture (presence) and how their values are checked (list, range, pattern). BIBS applies the rules of the product's type when a quotation or account is submitted, so each product line gets its own validation without IT.
preconditions:
  - "The user has PRODUCT_MAINTAIN."
main_flow:
  - MBS adds a field rule with its scope (all products, line or product), target (account or item), field and check type.
  - The rule is authorised by another user.
  - When a quotation or account of that product is submitted, BIBS checks all rules in scope and lists every violation against its field.
alternate_flows:
  - A pattern that cannot be evaluated safely (back-references, look-around) is refused when the rule is saved.
rules:
  - [R1, "Check types: Presence, List (LOV), Range (minimum / maximum), Pattern.", Configurable, Field rules]
  - [R2, "The most specific scope applies in addition to the wider ones (product + line + all).", Fixed, "-"]
  - [R3, "Patterns are evaluated in linear time; unsafe patterns are refused.", Fixed, "-"]
validations:
  - [Mandatory field blank on submission, "<Field> is required for <product>", "-"]
  - [Value outside the range or list, "<Field> must be between <min> and <max> / <Field> is not a valid value", "-"]
  - [Invalid pattern entered on a rule, The pattern is not a valid expression, "-"]
fields_screen: Field rule
fields:
  - [Scope, List, "Yes", All / Line / Product, "-"]
  - [Line or product, List, Conditional, Product lines / products, Required for Line or Product scope]
  - [Target, List, "Yes", Account / Item, "-"]
  - [Field, List, "Yes", Field catalogue, "-"]
  - [Check type, List, "Yes", Presence / List / Range / Pattern, "-"]
  - [List type, List, Conditional, LOV types, Required for List]
  - [Minimum / Maximum, Number, Conditional, "-", "Required for Range; minimum <= maximum"]
  - [Pattern, Text, Conditional, "-", Required for Pattern; safe expression]
notifications:
  - "None."
audit:
  - "Rule changes audited; violations are returned to the user and not stored."
acceptance:
  - A Motor product rule "plate number required" stops submission of a Motor account without a plate number and names the field.
  - A range rule refuses a value outside the range and accepts the limits.
  - The field list per line is set up by MBS as each package is set up (content Q02).
```

```fr
id: FR-PM-013
title: Use shared document templates with product-specific fields
brd: [BRPM.005 (p.16-17)]
actor: System; MBS (templates)
priority: Must have
fit: CONFIGURE
screens: Package Request page (form.pdf, QS, comparative, package slip, advisory downloads)
api: GET .../requests/{id}/form.pdf, .../quotation-slip.pdf, .../package-slip.pdf, .../comparatives/{oid}.pdf
description: Product Maintenance documents are generated from versioned templates with placeholders. The templates include line-specific sections, so one template serves Motor, Fire, CGL, Equipment Floater, Marine Cargo and other lines. Each generated document records the template version used.
preconditions:
  - "The templates PKG_REQUEST_FORM, PKG_QUOTATION_SLIP, PKG_COMPARATIVE, PKG_SLIP, PKG_ADVISORY and PKG_RENEWAL_ADVISORY exist."
main_flow:
  - A user generates a document from a package request.
  - BIBS merges the request, terms and insurer data into the current template version.
  - BIBS stores the document with the template version and makes it available for download or sending.
rules:
  - [R1, "Template layouts are drafts until BDOI provides its layouts (Q03).", Configurable, Document templates]
  - [R2, "A document is always generated from stored values, so a regenerated file shows the same content.", Fixed, "-"]
validations:
  - [No active template of the type, The template <code> is not available, "-"]
notifications:
  - "None."
audit:
  - "Document generation is audited with template code and version."
acceptance:
  - A QS for a Motor package shows the Motor coverages and deductibles; a QS for a Fire package shows the Fire coverages.
  - Each generated document shows the template version in its record.
```

```fr
id: FR-PM-014
title: Maintain insurer-specific package terms
brd: [PMADD02 (p.4)]
actor: MBS (on a draft version); TSU (through negotiated terms)
priority: Must have
fit: NEW
screens: Package Version (Insurers, Insurer Terms sections); Product page (Versions)
api: PUT /api/v1/catalog/products/{code}/versions/{n}
description:
  - Each package version lists its insurers with role (Lead, Participant or Panel), share (co-insurance), rate and minimum premium. For each insurer and coverage, the version holds whether the coverage is included, the limit, sub-limit, deductible (amount, percent or wording) and the clauses that apply.
  - Terms are maintained on a draft version only. They reach new business after validation and release (FR-PM-043), so a change never alters a package already on sale.
preconditions:
  - "A draft version exists (from the MBS set-up or \"New Version\")."
main_flow:
  - MBS opens the draft version.
  - MBS adds or edits insurers and the insurer x coverage terms.
  - MBS saves; BIBS checks the shares and the completeness of the terms.
  - MBS submits the version for validation (FR-PM-043).
alternate_flows:
  - An insurer without its own terms receives the package's included coverages when the version is set up from a request, so every panel insurer has a term per included coverage.
rules:
  - [R1, "Insurer roles: Lead, Participant, Panel.", Fixed, "-"]
  - [R2, "When shares are used, they total 100%.", Fixed, "-"]
  - [R3, "Every insurer has a term for every included coverage before submission.", Fixed, "-"]
  - [R4, "Clauses come from the authorised clause library.", Fixed, "-"]
  - [R5, "A new-business transaction with an insurer outside a package's panel is refused.", Fixed, "-"]
validations:
  - [Shares do not total 100, The co-insurance shares must add up to 100%, PACKAGE_SHARES_INVALID]
  - [Insurer lacks a term for an included coverage, "Insurer <code> has no terms for coverage <coverage>", PACKAGE_INSURER_TERMS_MISSING]
  - [Non-panel insurer on a quotation, "Insurer <code> is not on the panel of <package>", INSURER_NOT_ON_PACKAGE]
  - [Direct edit of a versioned field on the product, "The rate, minimum premium, commission and TSI limit of package <code> change through a new version (Versions tab)", PRODUCT_FIELD_VERSIONED]
fields_screen: Package Version, Insurer Terms
fields:
  - [Insurer, List, "Yes", Insurer master, Active insurer]
  - [Role, List, "Yes", Lead / Participant / Panel, "-"]
  - [Share %, Number, "No", "-", "0-100; total 100 when used"]
  - [Rate %, Number, Conditional, "-", Required when no package rate]
  - [Minimum premium, Amount, "No", "-", ">= 0"]
  - [Coverage, List, "Yes", Coverages of the line, Authorised coverage]
  - [Included, Check box, "Yes", "-", "-"]
  - [Limit / Sub-limit, Amount, "No", "-", ">= 0"]
  - [Deductible amount / % / wording, Amount / Number / Text, "No", "-", ">= 0"]
  - [Clauses, Multi-select, "No", Clause library, Effective on the version date]
notifications:
  - "None until submission."
audit:
  - "Every change to insurers and terms on a draft is audited; released versions are read-only."
acceptance:
  - MBS maintains different deductibles for two insurers on the same coverage without IT.
  - A version whose insurer has no term for an included coverage cannot be submitted.
  - A quotation on the package with an insurer outside the panel is refused.
```

## Package request and approvals

```fr
id: FR-PM-020
title: Create and submit a Package Request Form
brd: [BRPM.008 (p.18-19), BRPM.011 (p.21)]
actor: Marketing AO; TSU Officer
priority: Must have
fit: NEW
screens: Package Requests; New Package Request; Edit Package Request
api: POST /api/v1/product-maintenance/requests; PUT .../requests/{id}; POST .../requests/{id}/submit; GET .../prefill
description:
  - The requester enters the request type and scope, the client (client-specific packages) or programme name, the product line, cover type or existing package, the reason, the requested terms (sections, coverages with limits and deductibles, requested rate and dates) and the target insurers, and attaches documents.
  - For AMEND, UPDATE, RENEW and REACTIVATE requests, BIBS pre-fills the terms from the package's current version, so nothing is keyed in twice (BRPM.011 AC3-4).
  - The request is saved as a draft with a number PKR-yyyy-n and submitted for Marketing approval when complete.
preconditions:
  - "The user has PKG_REQUEST."
main_flow:
  - The requester clicks **New Package Request**.
  - The requester selects the request type and scope and completes the header fields.
  - For a request on an existing package, BIBS loads the current terms of the package.
  - The requester enters or changes the requested terms and target insurers, and attaches documents.
  - The requester saves the draft. BIBS assigns the request number.
  - The requester clicks **Submit for Approval**. BIBS checks completeness and moves the request to FOR_MKT_APPROVAL.
alternate_flows:
  - Incomplete request. BIBS lists what is missing and keeps the request in DRAFT.
  - The requester voids a draft with a reason (list VOID_REASON); the request closes as VOIDED.
  - A returned request (FR-PM-021 to FR-PM-023) comes back to DRAFT with the reason; the requester corrects and resubmits it.
rules:
  - [R1, "Request numbers are PKR-<yyyy>-<n>.", Configurable, Parameter PKG_REQUEST_PREFIX]
  - [R2, "A NEW request names the cover type; other types name the existing package.", Fixed, "-"]
  - [R3, "A client-specific request names the client; a generic programme does not need one (PQ14).", Fixed, "-"]
  - [R4, "NEW and AMEND requests always negotiate; RETIRE never; UPDATE, RENEW and REACTIVATE choose on the form.", Fixed, "-"]
  - [R5, "Before submission, a request other than RETIRE has requested terms, a package end date and, when negotiated, at least one target insurer.", Fixed, "-"]
  - [R6, "The type of a saved request cannot change.", Fixed, "-"]
validations:
  - [Package or programme name blank, Enter the package or programme name, PKG_REQUEST_INCOMPLETE]
  - [Line not selected, Select the product line, PKG_REQUEST_INCOMPLETE]
  - [Reason not selected, Select the reason, PKG_REQUEST_INCOMPLETE]
  - [NEW request without cover type, Select the cover type, PKG_REQUEST_INCOMPLETE]
  - [Other type without package, "Select the package product of the <type> request", PKG_REQUEST_INCOMPLETE]
  - [Client-specific without client, Select the client of the client-specific package (PQ14), PKG_REQUEST_INCOMPLETE]
  - [End date not after start date, The package end date must be after its start date, PKG_DATES_INVALID]
  - [Negative rate or amount, Rates and amounts cannot be negative, PKG_AMOUNT_NEGATIVE]
  - [Missing items on submit, "Complete the request: <items>", PKG_REQUEST_INCOMPLETE]
  - [Type changed on a saved request, The type of a request cannot change, PKG_TYPE_FIXED]
fields_screen: New Package Request
fields:
  - [Request type, List, "Yes", LOV PKG_REQUEST_TYPE, Fixed after first save]
  - [Scope, Option, "Yes", Generic programme / Client-specific package, "-"]
  - [Package / programme name, Text, "Yes", "-", Up to 200 characters]
  - [Client or prospect, Look-up, Conditional, Client master (CRM), Required for client-specific]
  - [Product line, List, "Yes", Product lines, Active line]
  - [Cover type / subtype, List, Conditional, Cover types of the line, Required for NEW]
  - [Package product, List, Conditional, Packaged products of the line, Required except for NEW]
  - [Market segments, Multi-select, "No", Market segments, "-"]
  - [Reason, List, "Yes", LOV PKG_REQUEST_REASON, "-"]
  - [Comment on the reason, Text, Conditional, "-", Required when the reason is Others]
  - [Negotiation required, Check box, Conditional, "-", "Shown for UPDATE, RENEW and REACTIVATE"]
  - [Requested terms, Sections, Conditional, Heading and details per section, Required on submission except RETIRE]
  - [Coverages, Table, "No", Coverages of the line, "Code, included, limit, deductible per line"]
  - [Requested rate % / Minimum premium / Commission % / Package TSI limit, Number / Amount, "No", "-", ">= 0"]
  - [Effective from / Package start / Package end / Anniversary, Date, Conditional, "-", Package end required on submission; end after start]
  - [Target insurers, Multi-select, Conditional, Insurer master, At least one when negotiated]
  - [Documents, Attachment, "No", Document type PKG_REQUEST_FORM and others, Allowed file types and size of the platform]
notifications:
  - On submission, the holders of PKG_REQUEST_APPROVE are notified in BIBS (stage entry).
audit:
  - "Create, each save, submit and void are recorded in the request history."
acceptance:
  - A Marketing AO submits a complete NEW request; it receives a PKR number and moves to FOR_MKT_APPROVAL.
  - Submission with missing mandatory fields is refused and each missing item is named.
  - An AMEND request opens with the current version's terms pre-filled.
  - A client-specific request without a client cannot be saved.
```

```fr
id: FR-PM-021
title: Approve a package request (Marketing)
brd: [BRPM.008 (p.18-19), BRPM.021 (p.30-31)]
actor: Marketing TL, TH or UH
priority: Must have
fit: NEW
screens: Package Requests (tab For Approval); Package Request page
api: POST /api/v1/product-maintenance/requests/{id}/approve; workflow action return
description: A Marketing approver reviews the submitted request and approves it, which sends it to the TSU TL, or returns it to the requester with a reason. The BRD allows the TL, TH or UH to approve; BIBS accepts any one of them (PQ08).
preconditions:
  - The request is in FOR_MKT_APPROVAL.
  - The user has PKG_REQUEST_APPROVE and is not the maker or submitter.
main_flow:
  - The approver opens the request from the For Approval tab.
  - The approver reviews the form, terms and documents.
  - The approver clicks **Approve and Send to TSU**, optionally with a comment.
  - BIBS moves the request to FOR_TSU_REVIEW and notifies the TSU TL queue.
alternate_flows:
  - Return. The approver clicks **Return to Requester** and selects a reason (list RETURN_REASON); the request goes back to DRAFT.
  - Void. The requester or approver voids the request with a reason; it closes as VOIDED.
rules:
  - [R1, "The approver is never the maker or submitter of the request.", Fixed, "-"]
  - [R2, "One approval by TL, TH or UH is enough (a chain by segment or amount waits for PQ08).", Configurable, Workflow stage owner]
  - [R3, "SLA 24 hours (default).", Configurable, Parameter PKG_SLA_MKT_APPROVAL]
validations:
  - [Approver is the maker, A package request is approved by someone other than its maker, PKG_FOUR_EYES]
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - The TSU TL queue on approval; the requester on return or void.
  - SLA alert to the stage owners when the SLA passes.
audit:
  - "Approval, return and void recorded with user, time, comment and reason."
acceptance:
  - The requester cannot approve their own request.
  - An approved request appears in the TSU Review tab.
  - A returned request is back in DRAFT with the reason visible on the request.
```

```fr
id: FR-PM-022
title: Review and recommend a package request (TSU TL)
brd: [BRPM.009 (p.19-20)]
actor: TSU Team Lead
priority: Must have
fit: NEW
screens: TSU Workbench; Package Request page
api: POST .../requests/{id}/recommend; PUT .../requests/{id} (edit in FOR_TSU_REVIEW)
description: The TSU TL checks the request for completeness, accuracy and compliance, may correct it, and writes a recommendation for the TSU Head. The recommendation is stored on the request and shown to the TSU Head.
preconditions:
  - "The request is in FOR_TSU_REVIEW; the user has PKG_TSU_RECOMMEND."
main_flow:
  - The TSU TL opens the request from the TSU Workbench.
  - The TSU TL reviews the form and, where needed, edits it.
  - The TSU TL clicks **Recommend for Approval** and writes the recommendation.
  - BIBS stores the recommendation and moves the request to FOR_TSU_APPROVAL.
alternate_flows:
  - Return. The TSU TL returns the request to the requester with a reason; it goes back to DRAFT.
rules:
  - [R1, "A recommendation text is mandatory.", Fixed, "-"]
  - [R2, "SLA 24 hours (default).", Configurable, Parameter PKG_SLA_TSU_REVIEW]
validations:
  - [Recommendation blank, Write the recommendation for the TSU Head, PKG_RECOMMENDATION_REQUIRED]
fields_screen: Recommend dialog
fields:
  - [Recommendation, Long text, "Yes", "-", "Up to 4000 characters"]
notifications:
  - "TSU Head queue on recommendation; requester on return."
audit:
  - "Edit, recommendation and return recorded with user and time."
acceptance:
  - The request cannot reach the TSU Head without a recommendation.
  - The TSU Head sees the recommendation text on the request.
```

```fr
id: FR-PM-023
title: Approve a package request (TSU Head)
brd: [BRPM.009 (p.19-20)]
actor: TSU Team Head / Head
priority: Must have
fit: NEW
screens: TSU Workbench; Package Request page
api: POST .../requests/{id}/tsu-approve
description: The TSU Head approves the recommended request. For a negotiated request, BIBS opens negotiation round 1 with the target insurers. For a request without negotiation (RETIRE, or UPDATE / RENEW / REACTIVATE without negotiation) the requested terms become the proposed terms and the request goes to ManCom.
preconditions:
  - "The request is in FOR_TSU_APPROVAL; the user has PKG_TSU_APPROVE and did not recommend the request."
main_flow:
  - The TSU Head reads the request and the TSU TL's recommendation.
  - The TSU Head clicks **Approve**.
  - BIBS moves the request to NEGOTIATION and opens round 1, or to FOR_MANCOM for a request without negotiation.
alternate_flows:
  - Return to the requester with a reason (DRAFT).
rules:
  - [R1, "The TSU Head approval is never given by the recommender.", Fixed, "-"]
  - [R2, "The request cannot proceed without this approval (BRPM.009 AC4).", Fixed, "-"]
  - [R3, "SLA 24 hours (default).", Configurable, Parameter PKG_SLA_TSU_APPROVAL]
validations:
  - [Approver is the recommender, The TSU Head approval is given by someone other than the recommender, PKG_FOUR_EYES]
notifications:
  - "TSU negotiation queue (or ManCom) on approval; requester on return."
audit:
  - "Approval and return recorded with user, time and comment."
acceptance:
  - The recommender cannot give the TSU Head approval.
  - After approval of a NEW request, round 1 exists with the target insurers.
  - A RETIRE request goes to FOR_MANCOM without a negotiation round.
```

```fr
id: FR-PM-024
title: Receive, view and track package requests
brd: [BRPM.011 (p.21)]
actor: TSU; Marketing; MBS
priority: Must have
fit: CHANGE
screens: Package Requests; TSU Workbench; Package Request page
api: GET /api/v1/product-maintenance/requests; GET .../counts
description:
  - All requests are listed with request number, type, scope, client or programme, line, product, stage, days in stage and assignee. Tabs group them by stage (Drafts, For Approval, TSU Review, Negotiation, ManCom, With MBS, For Validation, Released, Closed). The TSU Workbench shows the TSU queues as tiles.
  - The responsible users are notified when a request enters their stage. A TL or Head assigns requests to officers (bulk Assign), and an officer can claim a request.
preconditions:
  - "The user has PRODUCT_VIEW or PKG_REPORT_VIEW."
main_flow:
  - The user opens Package Requests and selects a tab.
  - The user searches by request number or filters by type, or shows only their own requests.
  - The user opens a request to view its details, terms, negotiation, documents and history.
alternate_flows:
  - A TL selects several requests and assigns them to an officer.
rules:
  - [R1, "Stage owners are notified on stage entry.", Fixed, "-"]
  - [R2, "\"Deletion\" of a product is a RETIRE request; nothing is physically deleted.", Fixed, "-"]
validations: []
fields_screen: Package Requests (filters)
fields:
  - [Search Request No., Text, "No", "-", "-"]
  - [Type, List, "No", LOV PKG_REQUEST_TYPE, "-"]
  - [Mine only, Check box, "No", "-", "-"]
notifications:
  - "Stage-entry notification to the owners of the new stage."
audit:
  - "Assignment and claim recorded on the work case."
acceptance:
  - A request submitted by Marketing appears in the TSU Review tab with its type, client, line and reason.
  - The TSU TL receives a notification when a request enters FOR_TSU_REVIEW.
  - A request assigned to an officer shows the officer as assignee.
```

## Negotiation and comparative tables

```fr
id: FR-PM-030
title: Prepare, approve and send the quotation slip
brd: [BRPM.012 (p.22)]
actor: TSU Officer (prepare); TSU TL or co-officer (approve)
priority: Must have
fit: CHANGE
screens: Package Request page (Negotiation tab)
api: PUT .../rounds/{n}; POST .../rounds/{n}/quotation-slip/submit | approve; GET .../quotation-slip.pdf; POST .../rounds/{n}/insurers/{code}/resend
description:
  - The QS of each round is generated from the request, so the Marketing details flow into it; TSU edits the insurers and notes of the round. The preparer submits the QS, which gets a number PQS-yyyy-n and a reply due date. A TL or co-officer approves it, and BIBS then sends one protected e-mail per insurer and opens a pending response for each.
  - The slips stay editable by TSU until the ManCom sign-off, when they lock.
preconditions:
  - "The request is in NEGOTIATION; a round is open."
main_flow:
  - The TSU Officer opens the round and selects the insurers.
  - The officer submits the QS. BIBS numbers it and sets the reply due date.
  - The TSU TL (or a co-officer) reviews the QS PDF and clicks **Approve and Send**.
  - BIBS e-mails the protected QS to each insurer and shows the send status per insurer.
alternate_flows:
  - Resend. The officer resends the QS to one insurer from the round (for example after a bounce); the resend is logged.
  - Locked. After the ManCom sign-off any edit or resend is refused.
rules:
  - [R1, "QS numbers are PQS-<yyyy>-<n>.", Configurable, Parameter PKG_QS_PREFIX]
  - [R2, "Reply due date = send date + 5 days (default).", Configurable, Parameter PKG_QS_REPLY_DAYS]
  - [R3, "The QS is approved by someone other than its preparer.", Fixed, "-"]
  - [R4, "One protected e-mail per insurer; the password is sent separately.", Fixed, "-"]
  - [R5, "Slips lock at the ManCom sign-off.", Fixed, "-"]
validations:
  - [No insurer selected, Select at least one insurer for the quotation slip, QS_NO_INSURER]
  - [Approver is the preparer, The quotation slip is approved by a TL or a co-officer, QS_FOUR_EYES]
  - [Edit after sign-off, The slips are locked since the ManCom sign-off, QS_LOCKED]
  - [Round already sent, "Round <n> is already sent", QS_ALREADY_SENT]
  - [Resend of a closed round, "Only the slip of an open, sent round can be resent", QS_NOT_RESENDABLE]
fields_screen: Negotiation tab, round
fields:
  - [Insurers, Multi-select, "Yes", Insurer master, Active insurers]
  - [What changes in this round, Long text, "No", "-", Shown on rounds 2 and later]
  - [QS number, Display, "-", System, PQS-yyyy-n]
  - [Reply due, Display, "-", System, Send date + PKG_QS_REPLY_DAYS]
notifications:
  - The QS approvers are notified when a QS is submitted.
  - The preparer sees the per-insurer send confirmation (BRPM.012 AC5).
audit:
  - "Submit, approve, send, resend: user, time, recipient, subject and attachment hash."
acceptance:
  - The preparer cannot approve their own QS.
  - An approved QS is e-mailed to each insurer of the round as a protected PDF, and the E-mails tab lists each send.
  - The officer can view the sent history and resend to one insurer.
  - After ManCom sign-off the QS cannot be edited or resent.
```

```fr
id: FR-PM-031
title: Record insurer responses and outcomes
brd: [BRPM.013 (p.23), PMADD04 (p.5)]
actor: TSU Officer / TL
priority: Must have
fit: CHANGE
screens: Package Request page (Negotiation tab, response dialog)
api: PUT .../responses/{rid}; POST .../responses/{rid}/document; GET .../responses/history
description: TSU keys in each insurer's reply per round, with the outcome, the offered rate and minimum premium, the terms per coverage (included, limit, deductible, clauses), conditions and warranties, validity and remarks, and attaches the insurer's reply. The outcome list contains the exception states the addendum asks for, so a decline or a counter-proposal is a recorded outcome and not an e-mail.
preconditions:
  - "The QS of the round has been sent to the insurer."
main_flow:
  - The officer opens the insurer's response in the round.
  - The officer selects the outcome and enters the offered terms.
  - The officer attaches the insurer's reply document.
  - The officer saves. BIBS keeps the previous values in the response history.
alternate_flows:
  - No response by the due date. The officer records "No response".
  - Correction. The officer edits a saved response; the history shows both values.
rules:
  - [R1, "Outcomes: Pending, Accepted as requested, Approved with changes, Counter-proposal, Declined, No response (list to confirm, PQ05).", Configurable, LOV PKG_RESPONSE_OUTCOME]
  - [R2, "An offer (accepted, approved with changes, counter-proposal) needs a rate.", Fixed, "-"]
  - [R3, "Every change to a response is kept in its history.", Fixed, "-"]
validations:
  - [Offer without rate, "Enter the rate offered by <insurer> (<outcome>)", PKG_RESPONSE_RATE_REQUIRED]
  - [Negative rate or premium, Rates and amounts cannot be negative, PKG_AMOUNT_NEGATIVE]
fields_screen: Insurer response
fields:
  - [Outcome, List, "Yes", LOV PKG_RESPONSE_OUTCOME, "-"]
  - [Rate %, Number, Conditional, "-", Required for an offer; >= 0]
  - [Minimum premium, Amount, "No", "-", ">= 0"]
  - [Valid until, Date, "No", "-", "-"]
  - [Coverage terms, Table, "No", Coverages of the request, "Included, limit, deductible, clauses per coverage"]
  - [Conditions and warranties, Long text, "No", "-", "-"]
  - [Remarks, Text, "No", "-", "-"]
  - [Response document, Attachment, "No", Document type PKG_INSURER_RESPONSE, Platform file rules]
notifications:
  - "None; the round's status updates on the Negotiation tab and the dashboard."
audit:
  - "Each save creates a history row with the previous values, user and time."
acceptance:
  - TSU records "Approved with changes" with a rate and changed deductibles for one insurer and "Declined" for another.
  - A counter-proposal without a rate is refused.
  - The response history shows every earlier value with user and time.
```

```fr
id: FR-PM-032
title: Run further negotiation rounds
brd: [PMADD04 (p.5), BRPM.010 (p.20)]
actor: TSU Officer / TL
priority: Must have
fit: CHANGE
screens: Package Request page (Negotiation tab)
api: POST .../requests/{id}/rounds (revise QS)
description: When terms need another round, TSU clicks **Revise Quotation Slip**. BIBS closes the sent round and opens round n+1, by default with the insurers that did not decline, and the new QS goes through the same approval and sending (FR-PM-030). All rounds and their responses stay on the request.
preconditions:
  - "The request is in NEGOTIATION and the latest round has been sent."
main_flow:
  - The officer clicks **Revise Quotation Slip** and states what changes in this round.
  - BIBS closes the current round and opens the next one with the proposed insurers.
  - The officer adjusts the insurers and submits the new QS.
alternate_flows:
  - Marketing requests changes to the terms (FR-PM-036). The request returns to NEGOTIATION with the round still open, and TSU revises or makes the terms final again.
  - Not proceeded. TSU or the TSU Head closes the request with a reason from PKG_NOT_PROCEEDED_REASON (for example "Declined by the insurers").
rules:
  - [R1, "Round n+1 proposes the insurers of round n that did not decline.", Fixed, "-"]
  - [R2, "There is no maximum number of rounds until BDOI states one (PQ05).", Configurable, "-"]
  - [R3, "Not-proceeded reasons: declined by the insurers, terms not accepted, client withdrew, business decision, others.", Configurable, LOV PKG_NOT_PROCEEDED_REASON]
validations:
  - [Revise before the current QS is sent, "Send the slip of round <n> first", QS_NOT_SENT]
  - [Not proceeded without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "QS approvers on each new QS; the requester when a request is not proceeded."
audit:
  - "Opening and closing of each round and the not-proceeded decision are recorded."
acceptance:
  - A request negotiated in two rounds shows both rounds, their QS numbers and the responses of each.
  - Round 2 opens by default without the insurers that declined in round 1.
  - A request closed as not proceeded shows the reason and cannot be reopened.
```

```fr
id: FR-PM-033
title: Make the negotiated terms final
brd: [BRPM.010 (p.20)]
actor: TSU Officer / TL
priority: Must have
fit: CHANGE
screens: Package Request page (Negotiation tab, Terms Final dialog)
api: POST .../requests/{id}/terms-final
description: When negotiation is complete, TSU chooses the insurer or insurers whose terms the package takes, with their roles and shares, and makes the terms final. BIBS refuses this while any insurer of the latest round has no outcome, so a package cannot be presented or encoded before negotiation is documented. The chosen terms become the proposed package terms and BIBS compiles the comparative master (FR-PM-034).
preconditions:
  - "The request is in NEGOTIATION."
main_flow:
  - The officer clicks **Terms Final**.
  - The officer selects the chosen insurers and sets role and share for each.
  - BIBS checks the round and the choice, stores the proposed terms and compiles the comparative master.
  - The request moves to TERMS_REVIEW.
rules:
  - [R1, "Every insurer of the latest round has an outcome other than Pending.", Fixed, "-"]
  - [R2, "Only insurers that offered terms can be chosen.", Fixed, "-"]
  - [R3, "Shares, when used, total 100%.", Fixed, "-"]
  - [R4, "SLA of NEGOTIATION 120 hours (default).", Configurable, Parameter PKG_SLA_NEGOTIATION]
validations:
  - [An insurer still pending, "Every insurer of round <n> needs an outcome before the terms are final (<insurers>)", NEGOTIATION_INCOMPLETE]
  - [No insurer chosen, Choose the insurer(s) whose terms the package takes, NEGOTIATION_INCOMPLETE]
  - [Chosen insurer did not offer, "<insurer> did not offer terms in this round", PKG_INSURER_NOT_OFFERED]
  - [Shares do not total 100, The co-insurance shares must add up to 100%, PKG_SHARES_INVALID]
fields_screen: Terms Final dialog
fields:
  - [Chosen insurer, Check box per insurer, "Yes", Insurers that offered terms, At least one]
  - [Role, List, "Yes", Panel / Lead / Participant, "-"]
  - [Share %, Number, "No", "-", Total 100 when used]
notifications:
  - "The TSU Head queue (TERMS_REVIEW)."
audit:
  - "Terms final recorded with the chosen insurers and the proposed terms."
acceptance:
  - Terms Final is refused while one insurer of the round is Pending.
  - After Terms Final, the request shows the proposed terms and a current comparative master.
```

```fr
id: FR-PM-034
title: Compile one comparative master per package request
brd: [BRPM.014 (p.24-25), BRPM.013 (p.23)]
actor: System; TSU
priority: Must have
fit: CHANGE
screens: Package Request page (Comparative tab)
api: GET .../comparatives; POST .../comparatives/master; GET .../comparatives/{oid}.pdf | .xlsx
description: BIBS compiles all insurer responses of the latest round into one comparative table per request, showing outcome, rate, minimum premium, coverages, deductibles, conditions, validity and remarks per insurer. A new master supersedes the previous one, so there is only one current master; earlier masters stay as superseded outputs. Each output is stored as a PDF and XLSX attachment with its hash. Marketing sees it read-only.
preconditions:
  - "At least one response exists in the latest round."
main_flow:
  - BIBS compiles the master at Terms Final, or TSU clicks **Compile Master** during negotiation.
  - BIBS marks the previous master as superseded and stores the new one with template version, user, time and SHA-256.
  - Users with access view or download the master as PDF or Excel.
rules:
  - [R1, "One current MASTER per request.", Fixed, "-"]
  - [R2, "An output is rendered from the values stored at generation, so a file downloaded later shows the same content.", Fixed, "-"]
  - [R3, "Marketing has view-only access to comparative tables.", Fixed, "-"]
validations:
  - [Compile with no response, There are no responses to compare, "-"]
notifications:
  - "None."
audit:
  - "Every generation is audited with the output id, hash and user."
acceptance:
  - A request with three responses has one current master listing the three insurers with their terms.
  - After a second compilation the first master is marked superseded and is still downloadable.
  - A Marketing user can open the master but has no Compile or Generate buttons.
```

```fr
id: FR-PM-035
title: Generate a client-tailored comparative view
brd: [PMADD03 (p.4)]
actor: TSU Officer / TL
priority: Must have
fit: CHANGE
screens: Package Request page (Comparative tab, Generate Client View dialog)
api: POST .../comparatives
description: TSU generates a client view from the current master by choosing the fields and insurers to show, with a title. The view never edits values; it shows a selection of the master's stored content. Each client view keeps a link to its master.
preconditions:
  - "A current master exists."
main_flow:
  - The officer clicks **Generate Client View**.
  - The officer enters the title and ticks the fields and insurers to include.
  - BIBS generates the PDF and Excel files, stores them with the selection and links them to the master.
rules:
  - [R1, "Selectable fields: Outcome, Rate %, Minimum Premium, Coverages, Deductibles, Conditions / Warranties, Valid Until, Remarks (to confirm, PQ06).", Configurable, Field catalogue (PQ06)]
  - [R2, "A client view is always derived from the current master.", Fixed, "-"]
validations:
  - [No current master, Compile the comparative master first, COMPARATIVE_NO_MASTER]
  - [Title blank, Enter the title of the view, COMPARATIVE_TITLE_REQUIRED]
  - [No field or insurer selected, Select at least one field and one insurer, "-"]
fields_screen: Generate Client View
fields:
  - [Title, Text, "Yes", "-", Up to 200 characters]
  - [Fields, Check boxes, "Yes", Field catalogue, At least one]
  - [Insurers, Check boxes, "Yes", Insurers of the master, At least one]
notifications:
  - "None."
audit:
  - "Generation audited with the selection, master id, template version and hash."
acceptance:
  - A client view that excludes one insurer and the Remarks field shows neither, and its values equal the master's.
  - The view lists the master it was derived from.
```

```fr
id: FR-PM-036
title: Review negotiated terms (TSU Head and Marketing)
brd: [BRPM.013 (p.23), BRPM.010 (p.20)]
actor: TSU Head; Marketing (client-specific packages)
priority: Must have
fit: CHANGE
screens: Package Request page
api: POST .../release-to-marketing; POST .../skip-marketing-review; POST .../accept-terms; workflow action request_changes
description: The TSU Head reviews the proposed terms. For a client-specific package, the TSU Head releases them to Marketing, who reviews them before any client presentation and accepts them or requests changes. A generic programme has no client presentation, so the TSU Head sends it straight to requirements preparation.
preconditions:
  - "The request is in TERMS_REVIEW."
main_flow:
  - The TSU Head opens the request and reviews the proposed terms and the comparative master.
  - For a client-specific package, the TSU Head clicks **Release Terms to Marketing** (FOR_MKT_REVIEW).
  - Marketing reviews the terms and clicks **Accept Terms** (REQUIREMENTS_PREP).
alternate_flows:
  - Generic programme. The TSU Head clicks **Proceed to Requirements**; the request moves to REQUIREMENTS_PREP.
  - Marketing clicks **Request Changes** with a reason; the request returns to NEGOTIATION.
  - Marketing closes the request as not proceeded with a reason.
rules:
  - [R1, "Marketing review is mandatory for client-specific packages and not offered for generic programmes.", Fixed, "-"]
  - [R2, "SLA of TERMS_REVIEW 24 hours and FOR_MKT_REVIEW 48 hours (defaults).", Configurable, Workflow stage SLA]
validations:
  - [Skip review on a client-specific package, Marketing reviews the terms of a client-specific package, PKG_SCOPE_CLIENT]
  - [Release to Marketing of a generic programme, A generic programme goes straight to the requirements, PKG_SCOPE_GENERIC]
  - [Request changes without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "Marketing on release of terms; TSU on acceptance or change request."
audit:
  - "Each decision recorded with user, time and reason."
acceptance:
  - A client-specific package cannot skip the Marketing review.
  - Marketing's change request returns the request to NEGOTIATION with the reason shown.
```

## Sign-off, set-up and release

```fr
id: FR-PM-040
title: Prepare and submit the requirements pack
brd: [BRPM.015 (p.25-26)]
actor: TSU Officer / TL
priority: Must have
fit: NEW
screens: Package Request page (Requirements & Sign-off tab)
api: GET | PUT .../requirements; GET .../package-slip.pdf; POST .../submit-requirements
description: TSU completes the product requirements that Marketing needs for its transactions, namely the scheme rate (or a rate per insurer), minimum premium, commission, package TSI limit, computation basis, effective date and package dates, the insurers, and the signed package slip. BIBS generates the unsigned package slip; TSU uploads the signed copy. Submission sends the pack to ManCom.
preconditions:
  - "The request is in REQUIREMENTS_PREP."
main_flow:
  - The officer opens the Requirements & Sign-off tab; the negotiated terms are shown.
  - The officer completes the rate scheme, computation basis and dates.
  - The officer downloads the package slip, obtains signatures and uploads it as PKG_SLIP_SIGNED.
  - The officer clicks **Submit Requirements**. BIBS checks the pack and moves the request to FOR_MANCOM.
alternate_flows:
  - Incomplete pack. BIBS lists every missing item and keeps the request in REQUIREMENTS_PREP.
rules:
  - [R1, "The pack needs: effective date, package end date, scheme rate or a rate per insurer, computation basis, at least one insurer, a signed package slip.", Fixed, "-"]
  - [R2, "SLA 48 hours (default).", Configurable, Workflow stage SLA]
validations:
  - [Pack incomplete, "Complete the requirements: <missing items>", REQUIREMENTS_INCOMPLETE]
  - [End date not after effective date, The package end date must be after the effective date, PKG_DATES_INVALID]
fields_screen: Requirements & Sign-off
fields:
  - [Scheme rate %, Number, Conditional, "-", Required unless every insurer has a rate]
  - [Minimum premium, Amount, "No", "-", ">= 0"]
  - [Commission %, Number, "No", "-", ">= 0"]
  - [Package TSI limit, Amount, "No", "-", ">= 0"]
  - [Computation basis, Long text, "Yes", "-", As agreed with the insurers]
  - [Effective from, Date, "Yes", "-", "-"]
  - [Package start / Package end, Date, "Yes (end)", "-", End after effective date]
  - [Anniversary, Date, "No", "-", "-"]
  - [Signed package slip, Attachment, "Yes", Document type PKG_SLIP_SIGNED, Platform file rules]
notifications:
  - "ManCom members on submission."
audit:
  - "Changes to the pack and the submission recorded."
acceptance:
  - Submission without a signed package slip is refused and names the missing slip.
  - A complete pack moves the request to FOR_MANCOM and notifies ManCom.
```

```fr
id: FR-PM-041
title: Sign off package requirements (ManCom)
brd: [BRPM.015 (p.25-26)]
actor: ManCom member
priority: Must have
fit: NEW
screens: Package Request page (Requirements & Sign-off tab)
api: POST .../signoff; workflow action return
description: A ManCom member reviews the requirements pack and signs off in BIBS. The sign-off locks the quotation slips and forwards the request to MBS automatically. A physically signed sheet can be uploaded as MANCOM_SIGNOFF; without an upload, BIBS attaches a generated sign-off record. The sign-off reference is MC-<request number>.
preconditions:
  - "The request is in FOR_MANCOM; the user has PKG_MANCOM_SIGNOFF and did not submit the requirements."
main_flow:
  - The ManCom member opens the request and reviews the pack.
  - The member optionally uploads the signed sign-off sheet.
  - The member clicks **Sign Off and Send to MBS**.
  - BIBS records the sign-off, locks the slips and moves the request to WITH_MBS.
alternate_flows:
  - Return. The member returns the pack to TSU with a reason (REQUIREMENTS_PREP); BIBS records a RETURNED sign-off.
rules:
  - [R1, "MBS cannot receive the requirements before the sign-off.", Fixed, "-"]
  - [R2, "One member's sign-off completes the step (quorum PQ07).", Configurable, Workflow stage owner (PQ07)]
  - [R3, "SLA 72 hours (default).", Configurable, Parameter PKG_SLA_MANCOM]
validations:
  - [Signer submitted the requirements, ManCom signs off requirements submitted by someone else, PKG_FOUR_EYES]
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
fields_screen: Sign-off dialog
fields:
  - [Comment, Text, "No", "-", "-"]
  - [Signed sign-off sheet, Attachment, "No", Document type MANCOM_SIGNOFF, Platform file rules]
notifications:
  - "MBS on sign-off; TSU on return."
audit:
  - "Sign-off or return recorded with user, time, comment and document."
acceptance:
  - The requirements reach MBS only after the ManCom sign-off.
  - After sign-off, the QS of every round can no longer be edited or resent.
  - A request without an uploaded sheet carries a generated MANCOM_SIGNOFF document.
```

```fr
id: FR-PM-042
title: Set up the package (MBS)
brd: [BRPM.015 (p.25-26)]
actor: MBS
priority: Must have
fit: NEW
screens: Package Request page (Set-up tab); Package Version
api: POST .../setup; POST .../return-incomplete
description: MBS receives only signed-off requirements. MBS checks that they are complete and sets up the package, which creates a draft package version from the proposed terms (rate scheme, dates, coverages, insurers and insurer terms), with the request number and ManCom reference as its origin. For a NEW request MBS gives the risk code and name of the new product. MBS then completes and submits the version for validation (FR-PM-043).
preconditions:
  - "The request is in WITH_MBS; the user has PRODUCT_MAINTAIN."
main_flow:
  - MBS opens the request in the With MBS tab and reviews the pack.
  - MBS clicks **Set Up Package Version**, enters the risk code and name (NEW) and a change summary.
  - BIBS creates the draft version and links it on the Set-up tab; the request moves to FOR_VALIDATION.
  - MBS opens the version, checks it and submits it for validation.
alternate_flows:
  - Incomplete requirements. MBS clicks **Return Incomplete** with a reason; the request returns to REQUIREMENTS_PREP.
  - A version returned by the validator comes back to WITH_MBS; MBS corrects the same draft and sets it up again.
  - A package already has a version being set up. BIBS refuses a second draft.
rules:
  - [R1, "One draft or for-validation version per product at a time.", Fixed, "-"]
  - [R2, "A new product created by set-up stays pending until its first version is validated.", Fixed, "-"]
  - [R3, "Computation uses the rating methods of the platform (property, motor, generic); a new formula needs development (PQ12).", Fixed, "-"]
  - [R4, "SLA 48 hours (default).", Configurable, Parameter PKG_SLA_MBS_SETUP]
validations:
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
  - [A version is already being set up, "Product <code> already has a version being set up", VERSION_IN_PROGRESS]
  - [Risk code breaks the line's convention, "Risk code <code> does not follow the naming convention of <line>", PRODUCT_CODE_PATTERN]
fields_screen: Set Up Package Version dialog
fields:
  - [Risk code of the new package, Text, Conditional, "-", Required for NEW; line pattern]
  - [Product name, Text, Conditional, "-", Required for NEW]
  - [Change summary, Text, "Yes", "-", Up to 500 characters]
notifications:
  - "Validators (PRODUCT_VALIDATE) when the version is submitted; TSU when requirements are returned."
audit:
  - "Set-up, return and the created version recorded with request number and ManCom reference."
acceptance:
  - MBS cannot set up a package before ManCom sign-off.
  - Set-up creates a draft version whose terms equal the signed-off proposed terms.
  - Incomplete requirements returned by MBS reach TSU with the reason.
```

```fr
id: FR-PM-043
title: Validate and release a package version
brd: [PMADD06 (p.5), BRPM.015 (p.25-26)]
actor: TSU Head or Business Administrator (PRODUCT_VALIDATE)
priority: Must have
fit: CHANGE
screens: Validation Queue; Package Version (Validation Checklist)
api: POST /api/v1/catalog/products/{code}/versions/{n}/submit | validate | return; GET /api/v1/catalog/validation-queue
description:
  - A package version is not ready until it is validated. MBS submits the draft; BIBS checks completeness. The validator, who is neither the maker nor the submitter, confirms the checklist, reviews a test premium computed on a sample item, and releases the version or returns it with a reason.
  - On release the previous version ends the day before the new effective date, the product's commercial values are updated from the version, the request moves to RELEASED, Marketing is notified and the advisory is drafted (FR-PM-044).
preconditions:
  - "The version is FOR_VALIDATION."
main_flow:
  - The validator opens the version from the Validation Queue.
  - The validator ticks each checklist item and reviews the test premium.
  - The validator clicks **Validate and Release**.
  - BIBS releases the version, records the checklist and publishes the release to Product Maintenance.
alternate_flows:
  - Return. The validator clicks **Return to MBS** with a reason; the version goes back to DRAFT and the request to WITH_MBS.
  - Submission refused. The draft is incomplete; BIBS names the missing item and it stays DRAFT.
rules:
  - [R1, "Checklist: hierarchy complete (cover type and basic coverage); every panel insurer has terms for each included coverage; rates and minimum premiums match the signed-off terms; dates and package term are correct; test premium reviewed (final list PQ09).", Configurable, Checklist (PQ09)]
  - [R2, "The validator is never the maker or submitter of the version.", Fixed, "-"]
  - [R3, "The effective date is today or later and after the latest released version.", Fixed, "-"]
  - [R4, "Only RELEASED versions are sold.", Fixed, "-"]
  - [R5, "SLA 24 hours (default).", Configurable, Workflow stage SLA]
validations:
  - [Hierarchy incomplete, A package needs its cover type (PMADD01), PACKAGE_HIERARCHY_INCOMPLETE]
  - [Insurer terms missing, "Insurer <code> has no terms for coverage <coverage>", PACKAGE_INSURER_TERMS_MISSING]
  - [No rate, Enter the package rate or a rate for every insurer, PACKAGE_RATE_MISSING]
  - [No effective date, Enter the effective date, PACKAGE_DATES_INVALID]
  - [Validator is maker or submitter, A package version is validated by someone other than its maker and submitter, MAKER_CHECKER_VIOLATION]
  - [Checklist not complete, Validate and Release stays disabled until every item is ticked, "-"]
fields_screen: Validation Checklist
fields:
  - [Checklist items, Check boxes, "Yes", Checklist, All ticked]
  - [Return reason, Text, Conditional, "-", "Required for Return (message: Enter the reason of the return, REASON_REQUIRED)"]
notifications:
  - "Marketing (quotation users) on release; MBS on return."
audit:
  - "Submission, validation with the confirmed checklist, return with reason; release recorded on the product history."
acceptance:
  - The MBS user who set up the version cannot validate it.
  - A released version is used by new quotations from its effective date; before release it is not.
  - A returned version is editable again and the request is back with MBS.
```

```fr
id: FR-PM-044
title: Draft and send package advisories
brd: [BRPM.016 (p.26-27), BRPM.017 (p.27-28)]
actor: TSU TL / TSU Officer / Operations (PKG_ADVISORY)
priority: Must have
fit: NEW
screens: Package Request page (Advisories tab)
api: GET .../requests/{id}/advisories; PUT /advisories/{aid}; POST /advisories/{aid}/send
description: On release BIBS drafts the advisory automatically, of type PACKAGE_READY for NEW and REACTIVATE, RENEWAL for RENEW, PACKAGE_UPDATED otherwise, and RETIREMENT for a retired package. The user checks the recipients, subject and text and sends it. Sending is blocked until the supporting documents are on the request. Recipients receive an in-app notification, and e-mail addresses given on the advisory receive the protected advisory PDF.
preconditions:
  - "An advisory draft exists; the user has PKG_ADVISORY."
main_flow:
  - The user opens the Advisories tab; the draft shows its type, recipient groups and the document check.
  - The user edits the subject, text, groups and optional e-mail addresses.
  - The user clicks **Send**. BIBS stores the advisory PDF, notifies each group and e-mails the addresses.
alternate_flows:
  - Documents missing. BIBS blocks the send and names the missing documents.
rules:
  - [R1, "Required documents: signed package slip (PKG_SLIP_SIGNED) and ManCom sign-off (MANCOM_SIGNOFF); for a retirement only the ManCom sign-off.", Fixed, "-"]
  - [R2, "Recipient groups: Marketing, TSU, MBS, Processing, Operations; default Marketing, TSU, MBS, Operations.", Configurable, LOV PKG_ADVISORY_GROUP; parameter PKG_ADVISORY_GROUPS]
  - [R3, "Group to users: Marketing = quotation users, TSU = PKG_NEGOTIATE, MBS = PRODUCT_MAINTAIN, Processing = account processing, Operations = OPS_VIEW (PQ12).", Fixed, "-"]
validations:
  - [Documents missing, "Attach the supporting documents before sending: <documents>", ADVISORY_DOCUMENTS_MISSING]
  - [Subject or text blank, Enter the subject and the text, "-"]
fields_screen: Advisory
fields:
  - [Recipient groups, Multi-select, "Yes", LOV PKG_ADVISORY_GROUP, At least one]
  - [E-mail to (optional), Text, "No", "-", Valid e-mail addresses]
  - [Subject, Text, "Yes", "-", Up to 200 characters]
  - [Text, Long text, "Yes", Template PKG_ADVISORY / PKG_RENEWAL_ADVISORY, "-"]
notifications:
  - "In-app notification to each group's users; protected e-mail to the given addresses."
audit:
  - "Draft, edit and send recorded; the sent advisory PDF is kept as PKG_ADVISORY."
acceptance:
  - An advisory cannot be sent while the signed package slip is missing.
  - A sent advisory reaches the users of each selected group in BIBS and lists the supporting documents.
  - The advisory PDF is stored on the request.
```

```fr
id: FR-PM-045
title: Retire a package
brd: [BRPM.011 (p.21), BRPM.006 (p.17)]
actor: Requester, approvers, ManCom, MBS
priority: Must have
fit: CHANGE
screens: New Package Request (type RETIRE); Package Request page (Set-up tab)
api: POST .../requests/{id}/retire
description: A package is removed from sale through a RETIRE request, which follows the approvals and ManCom sign-off without negotiation. At the MBS step, MBS retires the product instead of setting up a version. The product and all its versions stay readable; nothing is deleted. Active incentive criteria on the product are flagged for review.
preconditions:
  - "A RETIRE request is in WITH_MBS."
main_flow:
  - MBS clicks **Retire Package**.
  - BIBS sets the product to RETIRED, closes the request as RETIRED and drafts the retirement advisory.
rules:
  - [R1, "Only a RETIRE request retires a package; other types cannot.", Fixed, "-"]
  - [R2, "A retired product cannot be sold to new business.", Fixed, "-"]
validations:
  - [Retire on another request type, Only a RETIRE request retires the package, PKG_REQUEST_TYPE_MISMATCH]
  - [Set-up on a RETIRE request, A RETIRE request retires the package instead, PKG_REQUEST_TYPE_MISMATCH]
  - [Quotation on a retired product, "Product <code> is RETIRED and cannot be sold", PRODUCT_NOT_SELLABLE]
notifications:
  - "Retirement advisory; incentive review alert INCENTIVE_PRODUCT_INACTIVE to the incentive maintainers."
audit:
  - "Retirement recorded on the request and product history."
acceptance:
  - After retirement, a new quotation on the product is refused.
  - The retired product and its versions stay searchable for users with PRODUCT_ARCHIVE_VIEW.
```

## Versions and rate schemes

```fr
id: FR-PM-050
title: Keep package terms as versions
brd: [BRPM.007 (p.17-18)]
actor: MBS; System
priority: Must have
fit: CHANGE
screens: Product page (Versions tab); Package Version
api: GET | POST /api/v1/catalog/products/{code}/versions; GET | PUT .../versions/{n}
description: The commercial terms of a package (rate scheme, coverages, insurers, insurer terms, dates) are held in numbered versions with an effective date. A package is never edited in place. MBS creates a new draft version, which is validated and released while the current version keeps selling. The Versions tab shows the timeline and compares two versions.
preconditions:
  - "The product is packaged."
main_flow:
  - MBS opens the product and clicks **New Version**; BIBS copies the version in force, effective tomorrow.
  - MBS edits the draft and submits it for validation (FR-PM-043).
alternate_flows:
  - A version created from a package request is set up through FR-PM-042.
rules:
  - [R1, "Version states: Draft, For validation, Released, Superseded, Expired (section 5.3).", Fixed, "-"]
  - [R2, "One released version is in force per product; the previous one ends the day before the next takes effect.", Fixed, "-"]
  - [R3, "Packages existing at go-live received version 1, effective 01-Jan-2020, validated by SYSTEM.", Fixed, "-"]
validations:
  - [Direct edit of a versioned field, "The rate, minimum premium, commission and TSI limit of package <code> change through a new version (Versions tab)", PRODUCT_FIELD_VERSIONED]
  - [Second draft, "Product <code> already has a version being set up", VERSION_IN_PROGRESS]
fields_screen: Package Version, Rate Scheme and Dates
fields:
  - [Package Rate %, Number, Conditional, "-", Required unless every insurer has a rate]
  - [Minimum Premium, Amount, "Yes", "-", ">= 0"]
  - [Commission %, Number, "Yes", "-", ">= 0"]
  - [Package TSI Limit, Amount, "No", "-", ">= 0"]
  - [Effective From, Date, "Yes", "-", Today or later; after the latest released version]
  - [Package Start / Package End / Anniversary, Date, "No", "-", End after effective date]
  - [Change Summary, Text, "No", "-", "-"]
  - [ManCom Sign-off Reference, Text, "No", "-", "-"]
  - [Computation Basis, Text, "No", "-", As agreed with the insurers]
notifications:
  - "None until submission."
audit:
  - "Each version keeps its maker, submitter, validator, dates, checklist and source request."
acceptance:
  - Changing the rate of a released package creates version n+1; version n stays in force until n+1 takes effect.
  - The Versions tab shows every version with status and effective dates.
```

```fr
id: FR-PM-051
title: Price new business on the latest approved rate scheme
brd: [BRPM.007 (p.17-18)]
actor: System; Marketing (quotation user); Business Administrator (exception approval)
priority: Must have
fit: CHANGE
screens: Quotation (rate scheme panel, Request Rate Exception); Premium Calculator; My Approvals
api: POST /api/v1/catalog/rating/quote; POST /api/v1/catalog/rate-scheme-exceptions
description:
  - A new-business quotation or account on a package is priced on the version in force on the transaction date, whatever the period start, so an outdated rate cannot be selected. The version number is stored on the quotation, the account and the invoice.
  - An item rate different from the scheme (or panel insurer) rate, or the use of a version that is not current, needs an approved rate-scheme exception. Without it, submission is refused. Renewals and endorsements use the version of the original account (BRD-6 and BRD-1 contracts).
preconditions:
  - "The product is a package with a released version."
main_flow:
  - The user creates a quotation; BIBS prices it on the current version and shows "Priced on version n".
  - The user submits the quotation; BIBS checks the scheme.
  - The quotation, the accounts created from it and the invoice carry the version number.
alternate_flows:
  - Non-current rate. The user clicks **Request Rate Exception** with the reason; a PRODUCT_AUTHORIZE holder decides it in My Approvals. With the approved exception the quotation is submitted and the exception reference is stored.
  - Version changed while the quotation was a draft. Submission is refused; the user reprices on the current version.
rules:
  - [R1, "New business uses the version in force on the transaction date.", Fixed, "-"]
  - [R2, "A deviating item rate or a non-current version needs an approved exception, found by the transaction reference.", Fixed, "-"]
  - [R3, "Exceptions expire after 30 days (default).", Fixed, "-"]
  - [R4, "Packages migrated as version 1 keep manual item rates until BDOI answers PQ10.", Configurable, Version flag manual rate allowed (PQ10)]
  - [R5, "Statutory rates (DST, VAT, premium tax, LGT) keep their own effective dates (PQ10).", Fixed, "-"]
validations:
  - [Deviating rate without exception, "Package <code> is priced on the current rate scheme: rate <n>% needs an approved rate exception", RATE_SCHEME_NOT_CURRENT]
  - [Product expired or retired, "Product <code> is <status> and cannot be sold", PRODUCT_NOT_SELLABLE]
  - [No version in force, "Package <code> has no released version in force on <date>", PRODUCT_NOT_SELLABLE]
fields_screen: Request Rate Exception
fields:
  - [Requested rate or version, Number, "Yes", "-", Different from the current scheme]
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - "Authorisers of rate exceptions in My Approvals; requester on decision."
audit:
  - "Exception request and decision; the version used by each transaction."
acceptance:
  - A quotation with a period start before the new version's effective date is priced on the new version.
  - Submission with a manual rate different from the scheme is refused without an approved exception and accepted with one.
  - The quotation, account and invoice show the version number that priced them.
```

```fr
id: FR-PM-052
title: Archive expired packages
brd: [BRPM.006 (p.17)]
actor: System; MBS / TSU Head / Business Administrator / Auditor (PRODUCT_ARCHIVE_VIEW)
priority: Must have
fit: CHANGE
screens: Products (Expired and Retired filters); Product page
api: GET /api/v1/catalog/products?lifecycle=EXPIRED|RETIRED
description: When a package end date passes and no newer version is released, the daily job sets the version to EXPIRED and the product to EXPIRED. Expired and retired packages are never deleted. They remain searchable with all versions, terms, documents and communications, are tagged "Expired" or "Retired", and are visible only to users with PRODUCT_ARCHIVE_VIEW. An expired package returns to sale only through a REACTIVATE request.
preconditions:
  - "None."
main_flow:
  - The daily job PACKAGE_VERSION_LIFECYCLE expires packages whose end date passed.
  - An authorised user filters the Products list by Expired and opens the package and its history.
rules:
  - [R1, "Products and versions are never physically deleted.", Fixed, "-"]
  - [R2, "Expired and retired products are visible only with PRODUCT_ARCHIVE_VIEW.", Fixed, "-"]
  - [R3, "Reactivation only through a REACTIVATE request that releases a new version.", Fixed, "-"]
validations:
  - [User without archive permission filters expired products, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "PKG_NEGOTIATE and PRODUCT_MAINTAIN holders when a package expires."
audit:
  - "Expiry recorded on the product and version history."
acceptance:
  - A package past its end date without a renewal is tagged Expired and cannot be sold.
  - Its versions, documents and request history remain retrievable by authorised users.
  - A Marketing AO does not see expired packages.
```

## Expiry and renewal

```fr
id: FR-PM-060
title: Monitor package expiry and anniversary dates
brd: [BRPM.017 (p.27-28)]
actor: TSU Officer; MBS; System
priority: Must have
fit: NEW
screens: Package Expiry (tabs Expiring, Renewal in Progress, Expired); Product Maintenance Home
api: GET /api/v1/product-maintenance/expiry
description: The Package Expiry screen lists released packages by end date, with days left, anniversary date and the open renewal request and its stage. The daily job PACKAGE_EXPIRY_MONITOR raises the alert PACKAGE_EXPIRING at the notice period and again at the reminder days, and can draft the renewal request automatically.
preconditions:
  - "The user has PKG_NEGOTIATE, PRODUCT_MAINTAIN or PKG_REPORT_VIEW."
main_flow:
  - The job runs daily (01:00 PHT) and raises PACKAGE_EXPIRING once per package, version and threshold.
  - The TSU Officer opens Package Expiry, sets "Packages ending within (days)" and reviews the list.
  - The officer exports the list with **Generate Expiry List** for a date range.
rules:
  - [R1, "First alert 60 days before the end date (default).", Configurable, Parameter PACKAGE_EXPIRY_NOTICE_DAYS]
  - [R2, "Reminders at 30 and 7 days (default).", Configurable, Parameter PACKAGE_EXPIRY_REMINDER_DAYS]
  - [R3, "Automatic drafting of RENEW requests off by default.", Configurable, Parameter PACKAGE_RENEWAL_AUTODRAFT]
  - [R4, "Job time 01:00 PHT.", Configurable, Job schedule (System Administrator)]
validations: []
fields_screen: Package Expiry
fields:
  - [Packages ending within (days), Number, "Yes", "-", "1-365"]
  - [Generate Expiry List (from / to), Date range, "Yes", "-", To after from]
notifications:
  - "PACKAGE_EXPIRING to PKG_NEGOTIATE and PRODUCT_MAINTAIN holders at each threshold."
audit:
  - "Job runs and alerts are logged."
acceptance:
  - A package ending in 45 days appears in the Expiring tab with 45 days left.
  - The alert is raised once at 60, 30 and 7 days, never twice for the same threshold.
```

```fr
id: FR-PM-061
title: Generate renewal requests
brd: [BRPM.017 (p.27-28)]
actor: TSU Officer
priority: Must have
fit: NEW
screens: Package Expiry (bulk Generate Renewal Request); Package Request page
api: POST /api/v1/product-maintenance/expiry/renewal-requests
description: The TSU Officer selects expiring packages and clicks **Generate Renewal Request**. BIBS drafts one RENEW request per package, pre-filled from the current version, with the next term's dates (start = old end + 1 day, same length). The request follows the normal workflow; negotiation is optional. MBS accepts or returns the renewal documentation at the WITH_MBS step, and the renewal advisory is FR-PM-044.
preconditions:
  - "The packages are released and have no open RENEW or REACTIVATE request."
main_flow:
  - The officer ticks the packages and clicks **Generate Renewal Request**.
  - BIBS drafts the requests and shows their numbers.
  - The officer edits the dates, terms and rates as needed and submits each request.
rules:
  - [R1, "One open renewal request per package.", Fixed, "-"]
  - [R2, "Next term: start = previous end + 1 day, same length.", Fixed, "-"]
validations:
  - [No package selected, Select the packages to renew, PKG_RENEWAL_NONE]
notifications:
  - "As FR-PM-020 on submission."
audit:
  - "Draft creation recorded with the source package and version."
acceptance:
  - A generated renewal request has the terms of the current version and the next term's dates.
  - A package with an open renewal request cannot get a second one.
```

## Monitoring, reports and notifications

```fr
id: FR-PM-070
title: Product Maintenance home and dashboard
brd: [BRPM.019 (p.29)]
actor: Marketing, TSU, MBS, validators
priority: Must have
fit: CHANGE
screens: Product Maintenance Home
api: GET /api/v1/product-maintenance/counts
description: The home page shows tiles by stage with SLA status (red past SLA, amber near SLA), packages expiring in 30, 60 and 90 days, versions for validation, advisories pending and comparative outputs of the week. Each tile opens its filtered list, which can be exported to Excel. Counts follow the user's access.
preconditions:
  - "The user has PRODUCT_VIEW or PKG_REPORT_VIEW."
main_flow:
  - The user opens Product Maintenance Home.
  - The user clicks a tile; the filtered list opens.
  - The user exports the list.
rules:
  - [R1, "Amber from 80% of the stage SLA; red past the SLA.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Exports are audited."
acceptance:
  - A request past its stage SLA is counted in a red tile.
  - Clicking "Expiring in 30 days" opens the Package Expiry list filtered to 30 days.
```

```fr
id: FR-PM-071
title: Package reports
brd: [BRPM.018 (p.28-29), BRPM.017 (p.27-28), BRPM.006 (p.17)]
actor: Marketing, TSU, MBS (PKG_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Reports (category New Business)
api: Report service, codes PM-PKG-STATUS, PM-PKG-EXPIRY, PM-VERSION-HISTORY
description: Three reports cover package creation status, expiry and version history (layouts in section 6). The Package Status Update Report runs for one request or for all, with filters. Reports export to PDF, Excel, ODS and CSV, can be saved as variants, and every run is logged.
preconditions:
  - "The user has PKG_REPORT_VIEW."
main_flow:
  - The user selects the report and sets the parameters.
  - BIBS runs the report and shows the result.
  - The user exports it or saves the parameters as a variant.
rules:
  - [R1, "BRPM.018 is built as a package report distinct from the BRD-1 Placement Update Report (name and columns to confirm, PQ15).", Configurable, Report definition (PQ15)]
validations:
  - [Days not a number, Enter a whole number of days, "-"]
notifications:
  - "None."
audit:
  - "Each run and export logged with user, parameters and time."
acceptance:
  - The Package Status Update Report for one request number shows only that request.
  - The report exports to PDF and Excel with the same rows.
  - A user without PKG_REPORT_VIEW cannot run the reports.
```

```fr
id: FR-PM-072
title: Notify users and publish product master changes
brd: [BRPM.022 (p.31)]
actor: System
priority: Must have
fit: CHANGE
screens: Notifications (bell); E-mails tab
api: Workflow and messaging (internal); ProductMasterFeed port
description: BIBS notifies users on every stage entry, return, SLA breach, release, expiry and advisory. Released and expired package versions are also handed to the product master feed, which is the point where other BDOI tracking and reporting systems will receive product changes. The feed's transport is parked until BDOI names the systems (PQ16); today it logs each change.
preconditions:
  - "None."
main_flow:
  - An event occurs (stage change, release, expiry).
  - BIBS notifies the users concerned in the app and, where set in their preferences, by e-mail.
  - For release and expiry, BIBS passes the change to the product master feed.
rules:
  - [R1, "Users set their notification preferences (in-app / e-mail) per event type.", Configurable, Notification preferences]
  - [R2, "Product master feed: no transport until PQ16 is answered.", Fixed, "-"]
validations: []
notifications:
  - "This FR is the notification service."
audit:
  - "Notifications and feed calls are logged."
acceptance:
  - The TSU TL receives a notification when a request enters FOR_TSU_REVIEW.
  - A release writes a product master feed log entry with product, version and effective date.
```

```fr
id: FR-PM-073
title: Enforce validations, approvals and SLAs
brd: [BRPM.021 (p.30-31)]
actor: System
priority: Must have
fit: CHANGE
screens: Package Request page (workflow panel)
api: Workflow PM_PACKAGE_REQUEST
description: Each stage has an owner permission and an SLA; each business action checks its data before the transition. A request or document cannot move, be sent or be finalised until its checks and approvals are complete (section 5). Pending actions appear in the owners' queues, and the SLA check alerts them when the SLA passes.
preconditions:
  - "None."
main_flow:
  - A user starts an action from the workflow panel.
  - BIBS checks the permission, the four-eyes rule and the data of the action.
  - BIBS performs the transition, records it and notifies the next owners.
rules:
  - [R1, "Only the transitions of section 5.2 are possible.", Fixed, "-"]
  - [R2, "SLA hours per stage (defaults in section 5.1).", Configurable, Parameters PKG_SLA_*]
validations:
  - [Action not allowed in the stage, "The action <action> is not allowed in stage <stage>", "-"]
notifications:
  - "Stage entry and SLA breach to the stage owners."
audit:
  - "Every transition with user, time, comment and reason."
acceptance:
  - A request cannot be sent to MBS without the ManCom sign-off, even through the API.
  - A request past its stage SLA raises an SLA alert to the stage owners.
```

## Incentive criteria

```fr
id: FR-PM-080
title: Maintain incentive criteria on the products matrix
brd: [PMADD07 (p.5-6), PMADD08 (p.6-7)]
actor: Incentive Maintenance user (MBS, INCENTIVE_CRITERIA_MAINTAIN); authoriser (PRODUCT_AUTHORIZE)
priority: Must have
fit: CHANGE
screens: Incentive Criteria
api: GET | POST /api/v1/catalog/incentive-criteria; PUT /{id}; POST /{id}/deactivate; GET /{id}/history
description:
  - The Incentive Maintenance user defines criteria such as CPC2 with type, value basis, value, rule parameters, effective dates and the products they apply to, picked from the active products matrix (product, optional cover type, segment, channel and insurer). Each new or changed criterion is authorised by another user.
  - An active criterion is not edited. An amendment creates a successor that takes effect when authorised, and the old row is end-dated then. Deactivation end-dates the criterion. History is kept.
preconditions:
  - "The user has INCENTIVE_CRITERIA_MAINTAIN."
main_flow:
  - The user clicks **New Criterion** and enters the attributes.
  - The user picks the applicable products from the products matrix.
  - The user saves; the criterion is pending authorisation.
  - The authoriser authorises it; it becomes active from its effective date.
alternate_flows:
  - Amend an active criterion. BIBS creates a pending successor; on authorisation the old row is end-dated the day before.
  - Deactivate. The user sets the end date; the criterion becomes inactive.
rules:
  - [R1, "Only active products (and active insurers) on the effective date can be selected.", Fixed, "-"]
  - [R2, "No overlapping periods for the same code.", Fixed, "-"]
  - [R3, "Incentive types to be supplied by BDOI (PQ04); today OTHERS and MIGRATED.", Configurable, LOV INCENTIVE_TYPE]
  - [R4, "Maker-checker on every change.", Fixed, "-"]
validations:
  - [No product selected, Select at least one product of the products matrix, INCENTIVE_PRODUCTS_REQUIRED]
  - [Product not active, "Product <code> is not an active product of the matrix", INCENTIVE_PRODUCT_NOT_ACTIVE]
  - [Value missing, Enter the value of the incentive criterion, INCENTIVE_VALUE_REQUIRED]
  - [Period overlaps, "Criterion <code> already covers part of this period", INCENTIVE_PERIOD_OVERLAP]
  - [Rule parameters not valid JSON, The rule parameters must be valid JSON, INCENTIVE_RULE_PARAMS_INVALID]
  - [Direct edit of an active row, An active criterion is changed by amending it (successor), INCENTIVE_ACTIVE_LOCKED]
fields_screen: Incentive Criterion
fields:
  - [Code, Text, "Yes", "-", "For example CPC2; unique per period"]
  - [Name, Text, "Yes", "-", Up to 120 characters]
  - [Incentive type, List, "Yes", LOV INCENTIVE_TYPE, "-"]
  - [Value basis, List, "Yes", Rate / Fixed amount / Rule, "-"]
  - [Value, Number, Conditional, "-", Required for Rate and Fixed amount]
  - [Effective from, Date, "Yes", "-", "-"]
  - [Effective to, Date, "No", "-", On or after effective from]
  - [Rule parameters (JSON), Text, Conditional, "-", Valid JSON; required for Rule]
  - [Description, Text, "No", "-", "-"]
  - [Products matrix, Picker, "Yes", "Active products; optional cover type, segment, channel, insurer", At least one]
notifications:
  - "Authorisers in My Approvals; the maintainer on decision."
audit:
  - "Create, amend, authorise, deactivate with before and after values; successors linked."
acceptance:
  - A criterion on an inactive product cannot be saved.
  - An amended active criterion stays active until its successor is authorised, with no gap.
  - The history shows every earlier version of the criterion.
```

```fr
id: FR-PM-081
title: Apply incentive criteria and review them when products end
brd: [PMADD07 (p.5-6), PMADD08 (p.6-7)]
actor: System; Incentive Maintenance user
priority: Must have
fit: CHANGE
screens: Invoice (booking); Incentive Criteria
api: IncentiveCriteriaService.matching (internal)
description: At booking, BIBS matches the active criteria against the invoice facts (product, cover type, segment, channel, lead insurer, booking date) and stores the matched codes on the invoice; the BRD-1 incentive flag is set when at least one criterion matches. When a product expires or is retired, its active criteria are flagged with the alert INCENTIVE_PRODUCT_INACTIVE and never deleted. The former booking incentive rules were copied once into the criteria and are read-only.
preconditions:
  - "Active criteria exist."
main_flow:
  - An invoice is booked.
  - BIBS finds the matching criteria and stores their codes on the invoice.
rules:
  - [R1, "Endorsements inherit the criteria codes and version of the original invoice.", Fixed, "-"]
  - [R2, "Computation and payout of incentives are outside BRD-3 (Q33); Operations schemes reference the criteria codes (OQ39).", Fixed, "-"]
validations:
  - [Change to a former booking incentive rule, The booking incentive rules are read-only; use Incentive Criteria, INCENTIVE_RULES_FROZEN]
notifications:
  - "INCENTIVE_PRODUCT_INACTIVE to the incentive maintainers."
audit:
  - "Matched codes stored on the invoice."
acceptance:
  - An invoice on a product covered by CPC2 carries CPC2 and the incentive flag.
  - Retiring the product raises INCENTIVE_PRODUCT_INACTIVE for CPC2.
```

# Workflow and status model

## Stages

Figure 2 shows the workflow PM_PACKAGE_REQUEST. Solid arrows are the main path, dashed arrows are returns and optional paths, and dotted arrows close a request.

![Workflow PM_PACKAGE_REQUEST: stages and actions](figures/brd03_workflow.dot)

<!-- table: widths=3.3,4.1,1.5,4.3,4.4 caption="Stages, owners and SLA" size=8 -->
| Stage | Owner (permission) | SLA hours (default) | SLA parameter | Business actions |
|---|---|---|---|---|
| DRAFT | Requester (PKG_REQUEST) | - | - | Save, submit, void |
| FOR_MKT_APPROVAL | Marketing TL / TH / UH (PKG_REQUEST_APPROVE) | 24 | PKG_SLA_MKT_APPROVAL | Approve, return, void |
| FOR_TSU_REVIEW | TSU TL (PKG_TSU_RECOMMEND) | 24 | PKG_SLA_TSU_REVIEW | Edit, recommend, return, void |
| FOR_TSU_APPROVAL | TSU TH (PKG_TSU_APPROVE) | 24 | PKG_SLA_TSU_APPROVAL | Approve, approve without negotiation, return, void |
| NEGOTIATION | TSU (PKG_NEGOTIATE) | 120 | PKG_SLA_NEGOTIATION | QS, responses, revise QS, terms final, not proceeded |
| TERMS_REVIEW | TSU TH (PKG_TSU_APPROVE) | 24 | stage setting | Release to Marketing or proceed to requirements |
| FOR_MKT_REVIEW | Marketing (PKG_REQUEST) | 48 | stage setting | Accept terms, request changes, not proceeded |
| REQUIREMENTS_PREP | TSU (PKG_NEGOTIATE) | 48 | stage setting | Complete and submit requirements |
| FOR_MANCOM | ManCom (PKG_MANCOM_SIGNOFF) | 72 | PKG_SLA_MANCOM | Sign off, return |
| WITH_MBS | MBS (PRODUCT_MAINTAIN) | 48 | PKG_SLA_MBS_SETUP | Set up, return incomplete, retire |
| FOR_VALIDATION | Validator (PRODUCT_VALIDATE) | 24 | stage setting | Validate and release, return (on the version) |
| RELEASED | - | terminal | - | Advisory drafted; Marketing notified |
| RETIRED | - | terminal | - | Retirement advisory |
| NOT_PROCEEDED | - | terminal | - | - |
| VOIDED | - | terminal | - | - |

## Transitions

<!-- table: widths=3.5,3.6,3.5,3.6,3.4 caption="Transitions of PM_PACKAGE_REQUEST" size=8 -->
| From | Action | To | Permission | Reason list |
|---|---|---|---|---|
| DRAFT | submit | FOR_MKT_APPROVAL | PKG_REQUEST | - |
| FOR_MKT_APPROVAL | approve | FOR_TSU_REVIEW | PKG_REQUEST_APPROVE | - |
| FOR_TSU_REVIEW | recommend | FOR_TSU_APPROVAL | PKG_TSU_RECOMMEND | - |
| FOR_TSU_APPROVAL | approve | NEGOTIATION | PKG_TSU_APPROVE | - |
| FOR_TSU_APPROVAL | approve_no_negotiation | FOR_MANCOM | PKG_TSU_APPROVE | - |
| FOR_MKT_APPROVAL, FOR_TSU_REVIEW, FOR_TSU_APPROVAL | return | DRAFT | stage owner | RETURN_REASON |
| DRAFT to FOR_TSU_APPROVAL | void | VOIDED | requester or stage owner | VOID_REASON |
| NEGOTIATION | revise_qs | NEGOTIATION | PKG_NEGOTIATE | - |
| NEGOTIATION | terms_final | TERMS_REVIEW | PKG_NEGOTIATE | - |
| NEGOTIATION | not_proceeded | NOT_PROCEEDED | PKG_NEGOTIATE, PKG_TSU_APPROVE | PKG_NOT_PROCEEDED_REASON |
| TERMS_REVIEW | release_to_marketing | FOR_MKT_REVIEW | PKG_TSU_APPROVE | - |
| TERMS_REVIEW | skip_marketing_review | REQUIREMENTS_PREP | PKG_TSU_APPROVE | - |
| FOR_MKT_REVIEW | accept_terms | REQUIREMENTS_PREP | PKG_REQUEST | - |
| FOR_MKT_REVIEW | request_changes | NEGOTIATION | PKG_REQUEST | RETURN_REASON |
| FOR_MKT_REVIEW | not_proceeded | NOT_PROCEEDED | PKG_REQUEST | PKG_NOT_PROCEEDED_REASON |
| REQUIREMENTS_PREP | submit_requirements | FOR_MANCOM | PKG_NEGOTIATE | - |
| FOR_MANCOM | signoff | WITH_MBS | PKG_MANCOM_SIGNOFF | - |
| FOR_MANCOM | return | REQUIREMENTS_PREP | PKG_MANCOM_SIGNOFF | RETURN_REASON |
| WITH_MBS | setup | FOR_VALIDATION | PRODUCT_MAINTAIN | - |
| WITH_MBS | retire | RETIRED | PRODUCT_MAINTAIN | - |
| WITH_MBS | return_incomplete | REQUIREMENTS_PREP | PRODUCT_MAINTAIN | RETURN_REASON |
| FOR_VALIDATION | version_released (system) | RELEASED | PRODUCT_VALIDATE | - |
| FOR_VALIDATION | version_returned (system) | WITH_MBS | PRODUCT_VALIDATE | - |

## Package version states

![Package version states (catalogue)](figures/brd03_version_states.dot){width=9}

<!-- table: widths=3.2,6.6,7 caption="Version states" -->
| State | Meaning | Used for |
|---|---|---|
| DRAFT | Being set up by MBS; editable | Nothing; not rated |
| FOR_VALIDATION | Submitted; waiting for the validator | Nothing; not rated |
| RELEASED | Validated and in force from its effective date | New business, renewals, endorsements |
| SUPERSEDED | Ended by a newer released version | Renewals (RENEWAL purpose) and endorsements of accounts sold on it |
| EXPIRED | Package end date passed with no newer version | Reference only; archived and searchable (BRPM.006) |

## SLA and escalation

- SLA hours are held per stage and start when the request enters the stage. The defaults in section 5.1 are placeholders until BDOI answers PQ08. A changed PKG_SLA_* parameter is copied into the stage by the daily job.
- The SLA check marks a request amber from 80% of the stage SLA and red past it. Red requests raise an SLA alert to the stage owners and appear in the red tiles of the Product Maintenance home.
- Escalation beyond the stage owners (for example to the unit head) is not in the BRD and is not built.

# Reports and documents

## Reports

<!-- table: widths=3.4,4.2,6,3 caption="Product Maintenance reports" -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| PM-PKG-STATUS | Package Status Update Report | Status of package requests, one or all, with stage age, end date, rates and the last action | BRPM.018 |
| PM-PKG-EXPIRY | Package Expiry Report | Released packages by end date with their renewal status | BRPM.017 |
| PM-VERSION-HISTORY | Package Version History | Every version of the packaged products with validator, dates and change summary | BRPM.006, 007 |

All three are in the report category New Business, need PKG_REPORT_VIEW, export to PDF, XLSX, ODS and CSV, and support saved variants.

### Package Status Update Report (PM-PKG-STATUS)

Parameters: Request No. (individual report); Status (all or one stage); Request Type; Line Code; Insurer Code; Package Ends Within (days). Layout: landscape, one row per request, sorted by request number, with the parameters printed in the report header.

<!-- table: widths=3.6,3,10 caption="PM-PKG-STATUS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Request No. | Text | PKR-yyyy-n |
| Type | Text | Request type |
| Client / Programme | Text | Client name (client-specific) or "Generic" |
| Package | Text | Package or programme name |
| Product | Text | Risk code |
| Version | Number | Resulting or base version |
| Stage | Text | Current stage |
| Days in Stage | Number | Days since stage entry |
| Package End | Date | Proposed or current package end date |
| Rate % | Percent | Scheme rate of the proposed terms |
| Insurers | Text | Chosen or target insurers |
| Last Action | Text | Last workflow action |
| On / By | Date / Text | Date and user of the last action |

### Package Expiry Report (PM-PKG-EXPIRY)

Parameter: Ending Within (days), default 90. Layout: portrait, sorted by package end date.

<!-- table: widths=3.6,3,10 caption="PM-PKG-EXPIRY columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Product | Text | Risk code |
| Package | Text | Product name |
| Version | Number | Version in force |
| Package End | Date | End date of the version |
| Days Left | Number | Days from today to the end date |
| Anniversary | Date | Anniversary date, if any |
| Renewal Request | Text | Open RENEW / REACTIVATE request, or "None" |
| Renewal Stage | Text | Stage of that request |

### Package Version History (PM-VERSION-HISTORY)

Parameter: Product Code (optional). Layout: landscape, grouped by product, versions in ascending order.

<!-- table: widths=3.6,3,10 caption="PM-VERSION-HISTORY columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Product | Text | Risk code |
| Version | Number | Version number |
| Status | Text | Version state |
| Effective From / Effective To | Date | Period in force |
| Package End | Date | Package end date |
| Submitted By / Validated By / Validated On | Text / Date | Checkpoint users and date (PMADD06) |
| Source Request | Text | PKR number that produced the version |
| Change Summary | Text | Summary entered at set-up |

## Documents

Documents are generated from versioned templates (FR-PM-013). The layouts below are the draft layouts delivered with the build; BDOI's own layouts replace them when provided (Q03).

<!-- table: widths=3.6,3.4,10 caption="Product Maintenance documents" size=8.5 -->
| Template | Output | Sections |
|---|---|---|
| PKG_REQUEST_FORM | PDF | Header (request no., type, scope, date, requester); client or programme; line, cover type, product; reason; requested terms by section; coverages table (coverage, included, limit, deductible); requested rate scheme and dates; target insurers; approvals with names and dates |
| PKG_QUOTATION_SLIP | PDF, one per insurer | BDOI letterhead; QS no. and round; insurer; package and line; requested cover by section; coverages table; requested rate and minimum premium; period; reply due date; contact |
| PKG_COMPARATIVE | PDF and XLSX; MASTER or CLIENT | Title; request and round; one column per insurer; rows per selected field (outcome, rate, minimum premium, coverages, deductibles, conditions, valid until, remarks); lowest rate marked; generated by, date, template version |
| PKG_SLIP | PDF | Package name and code; insurers with role and share; rate scheme; coverages and insurer terms; clauses; package dates; signature blocks |
| PKG_ADVISORY | PDF, e-mail and in-app | Advisory type; package and version; effective date; what changed; list of supporting documents; instructions to units |
| PKG_RENEWAL_ADVISORY | PDF, e-mail and in-app | As PKG_ADVISORY, with the previous and new term dates |

# Interfaces and integration

Figure 4 shows the interfaces of Product Maintenance. Package requests talk to the catalogue only through its published services and events. The catalogue is read by new business, Operations and Renewal.

![Interfaces of Product Maintenance (dashed = parked)](figures/brd03_integration.dot)

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Client master (CRM) | In | Client look-up on client-specific requests | BRPM.008 | BUILT |
| Catalogue set-up | Out | Draft version from the proposed terms at MBS set-up; retirement | BRPM.015 | BUILT |
| Catalogue events | In | Version released, returned, product expired | PMADD06, BRPM.017 | BUILT |
| E-mail outbox | Out | Protected QS, comparative outputs and advisories; send log | BRPM.012, 016, 020 | BUILT |
| Notifications and alerts | Out | Stage entry, SLA, release, expiry, incentive review | BRPM.021, 022 | BUILT |
| New business (BRD-1) | Out | Scheme version and rating for quotations, accounts and invoices; incentive criteria codes | BRPM.007; PMADD07 | BUILT |
| Operations (BRD-2) | Out | Version for endorsement re-rating; criteria codes for commission schemes (OQ39) | BRPM.007 | BUILT |
| Renewal (BRD-6) | Out | Version of the expiring account for Renew As Is | BRPM.007 | BUILT |
| Other BDOI systems | Out | Product master changes (ProductMasterFeed); the port logs each change | BRPM.022 | PARKED |
| Insurer portals | Out | Not requested; insurers are reached by e-mail | BRPM.012 | OUT |

> [!PARKED] Parked seam
> BRPM.022 asks for data "synchronized with BDOI systems". The systems, format and timing are not in the BRD (PQ16). BIBS hands every release and expiry to the ProductMasterFeed port, which logs it. Adding a transport is a configuration of a new adapter, with no change to the workflow.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.33)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Marketing 485 max / 145 concurrent; MBS 5 / 5; TSU 13 / 13 | Within the BRD-1 sizing (150 concurrent + 20% a year); the Marketing figures equal BRD-1's (PQ18) | FIT |
| Volumes | Not stated | Assumed tens of package requests a month; no special sizing | OPEN |
| Response time | Under 5 seconds for every role | Online p95 under 3 seconds; documents and e-mails generated asynchronously | FIT |
| Peak | 15th and 30th of the month; 08:00-17:30 | Expiry monitor and version lifecycle run at 01:00 | FIT |
| Availability | 99.9%; use 07:00-18:30; maintenance per bank standard | Same deployment as BRD-1 (window 07:00-22:00 governs, OQ44) | FIT |
| Recovery | RTO 4 hours, RPO 24 hours | Platform backup and recovery; one BIBS-wide NFR set is being agreed (XQ08) | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Security | Authorised users only; protected documents | Role-based access, four-eyes rules, protected e-mails (FR-PM-002, 004) | FIT |
| Audit | All actions logged | Append-only audit and workflow history (FR-PM-005) | FIT |
| Retention | Not stated | BRD-1 rule (5 years online, 15 years archive); product versions never purged (PQ18) | OPEN |

# Configuration items owned by the System Administrator

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.4,8.0 caption="Product Maintenance parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| PKG_REQUEST_PREFIX | PKR- | Prefix of package request numbers; the year is appended (PKR-yyyy-n) |
| PKG_QS_PREFIX | PQS- | Prefix of quotation slip numbers (PQS-yyyy-n) |
| PKG_QS_REPLY_DAYS | 5 | Days insurers have to reply to a QS (1-60) |
| PACKAGE_EXPIRY_NOTICE_DAYS | 60 | Days before the package end date of the first expiry alert (1-365) |
| PACKAGE_EXPIRY_REMINDER_DAYS | 30,7 | Days of the further expiry reminders |
| PACKAGE_RENEWAL_AUTODRAFT | false | The expiry monitor drafts RENEW requests at the notice period |
| PKG_ADVISORY_GROUPS | MARKETING, TSU, MBS, OPERATIONS | Default recipient groups of an advisory |
| PKG_SLA_MKT_APPROVAL | 24 | SLA hours of the Marketing approval |
| PKG_SLA_TSU_REVIEW | 24 | SLA hours of the TSU TL review |
| PKG_SLA_TSU_APPROVAL | 24 | SLA hours of the TSU Head approval |
| PKG_SLA_NEGOTIATION | 120 | SLA hours of the negotiation stage |
| PKG_SLA_MANCOM | 72 | SLA hours of the ManCom sign-off |
| PKG_SLA_MBS_SETUP | 48 | SLA hours of the MBS set-up |
| Job schedule package-expiry-cron | 01:00 PHT daily | Time of PACKAGE_EXPIRY_MONITOR and PACKAGE_VERSION_LIFECYCLE |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| PKG_REQUEST_TYPE | New package; Amend package terms; Update package details; Renew package; Retire package; Reactivate expired package |
| PKG_REQUEST_REASON | Client requirement; New programme for a market segment; Market competitiveness; Change in insurer terms or rates; Loss experience; Regulatory change; Package expiry or anniversary; Others (see comment) |
| PKG_RESPONSE_OUTCOME | Pending; Accepted as requested; Approved with changes; Counter-proposal; Declined; No response |
| PKG_ADVISORY_GROUP | Marketing; Technical Support Unit; Marketing Business Services; Processing; Operations |
| PKG_NOT_PROCEEDED_REASON | Declined by the insurers; Negotiated terms not accepted; Client withdrew the request; Business decision; Others (see comment) |
| INCENTIVE_TYPE | Others (see description); Migrated (from the booking incentive rules). Types to be supplied by BDOI (PQ04) |
| COVERAGE_KIND | Section; Coverage; Peril; Extension |
| CLAUSE_KIND | Warranty; Clause; Exclusion; Deductible wording |
| DOCUMENT_TYPE (Product Maintenance values) | PKG_REQUEST_FORM; PKG_QUOTATION_SLIP; PKG_INSURER_RESPONSE; PKG_COMPARATIVE; PKG_SLIP_SIGNED; MANCOM_SIGNOFF; PKG_ADVISORY |
| RETURN_REASON, VOID_REASON | Shared platform lists |

## Masters and rules maintained by the business

<!-- table: widths=4.4,4.6,7.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Product lines, cover types and subtypes, code patterns | MBS (PRODUCT_AUTHORIZE) | FR-PM-010 |
| Coverages / perils | MBS (PRODUCT_AUTHORIZE) | FR-PM-010 |
| Clause library | MBS (PRODUCT_AUTHORIZE) | FR-PM-011 |
| Field rules (presence, list, range, pattern) | MBS (PRODUCT_AUTHORIZE) | FR-PM-012 |
| Document templates | System Administrator | FR-PM-013 |
| Package versions and insurer terms | MBS (validated by PRODUCT_VALIDATE) | FR-PM-014, 043, 050 |
| Rate-scheme exceptions | Requester (PRODUCT_AUTHORIZE) | FR-PM-051 |
| Incentive criteria | Incentive Maintenance user (PRODUCT_AUTHORIZE) | FR-PM-080 |
| Roles and permissions | Business Administrator via access request (approver) | FR-PM-003 |
| Validation checklist | Change request until PQ09 is answered | FR-PM-043 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-PM-01 | The final main BRD (pp.10-35) and the addendum (pp.1-9) are the baseline; the earlier version (pp.36-61) is used only to trace changes | R1-R3 |
| A-PM-02 | A package is a catalogue product flagged as packaged, generic for a segment or client-specific; each package has its own risk code | PQ01 |
| A-PM-03 | Any one of Marketing TL, TH or UH approves a request | PQ08 |
| A-PM-04 | One ManCom member's sign-off completes the ManCom step | PQ07 |
| A-PM-05 | "Deletion" in BRPM.011 means retirement; records are never deleted | PQ13 |
| A-PM-06 | Maintenance of non-package products is out of scope in this phase | PQ19 |
| A-PM-07 | BRPM.023 does not exist in either BRD version; no requirement is missing | PQ20 |
| A-PM-08 | Package request volume is low (tens a month) | PQ18 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-PM-01 | BDOI provides the product matrix content: subtypes, coverages per line, naming convention | FR-PM-010 (Q01, Q02, PQ02) |
| D-PM-02 | BDOI provides the document layouts and the password convention | FR-PM-004, 013 (Q03, Q07, PQ21) |
| D-PM-03 | BDOI defines CPC2 and the other incentive criteria | FR-PM-080 (PQ04) |
| D-PM-04 | BDOI names the systems that receive product master changes | FR-PM-072 (PQ16) |
| D-PM-05 | BRD-6 Renewal passes the expiring account's version on Renew As Is | FR-PM-051 (PQ11) |
| D-PM-06 | The e-mail relay of the BIBS environment is available for insurer e-mails | FR-PM-030, 044 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-3 (status from the cross-BRD decisions, R6)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| PQ02 | Subline / type / subtype values and coverage / peril list per line; naming convention for product codes | FR-PM-010 | OPEN |
| PQ03 | Which attributes vary by insurer in a package; co-insurance shares | FR-PM-014 | OPEN |
| PQ04 | Definition of CPC2 and other criteria; required attributes; who is the Incentive Maintenance user | FR-PM-080, 081 | OPEN |
| PQ05 | Full list of negotiation outcomes; maximum rounds; apply PMADD03/04 to non-package PRFs | FR-PM-031, 032 | OPEN |
| PQ06 | Fields of client views; client layout; who generates client outputs | FR-PM-035 | OPEN |
| PQ07 | ManCom membership, quorum, in-system versus signed sheet, SLA | FR-PM-041 | OPEN |
| PQ08 | Approval chain by segment or amount; QS co-officer; SLA of each stage | FR-PM-021, 030, 073 | OPEN |
| PQ09 | Validator role (TSU Head, Business Administrator or either); checklist content | FR-PM-043 | OPEN |
| PQ10 | Scope of a rate scheme (statutory rates?); approver of non-current rates; manual item rates on packages | FR-PM-051 | PARTIAL |
| PQ11 | Meaning of package expiry / anniversary; notice period; automatic renewal drafts | FR-PM-060, 061 | PARTIAL |
| PQ12 | Content of the requirements pack; formulas beyond the three rating methods; advisory recipients and channel | FR-PM-040, 042, 044 | OPEN |
| PQ14 | Mandatory fields of the Package Request Form; client for generic programmes | FR-PM-020 | OPEN |
| PQ15 | Name and columns of the BRPM.018 report | FR-PM-071 | OPEN |
| PQ16 | Systems that receive product master changes; format and timing | FR-PM-072 | OPEN |
| PQ17 | Final role-to-action matrix; approval of role changes | FR-PM-002, 003 | ANSWERED |
| PQ18 | Volumes, growth, availability window and retention | Section 8 | OPEN |
| PQ21 | Which documents leave BDOI and the password convention | FR-PM-004 | PARTIAL |

PQ01, PQ13, PQ19 and PQ20 are covered by assumptions A-PM-02, A-PM-05, A-PM-06 and A-PM-07; BDOI confirms them with the sign-off of this document.

# Traceability

Every BRD-3 requirement is met by at least one FR. The screen and API columns name the main entry points.

<!-- table: widths=2.2,3.4,5.2,5.8 caption="BRD ID to FR, screen and API" size=8 -->
| BRD ID | FR | Screen | API |
|---|---|---|---|
| BRPM.001 | FR-PM-001 | Login | /api/v1/auth/login |
| BRPM.002 | FR-PM-002 | All; User Access Matrix | Permission checks; /nbadmin/access-matrix |
| BRPM.003 | FR-PM-012 | Product page (Field Rules) | /catalog/field-rules |
| BRPM.004 | FR-PM-012 | Product page (Field Rules) | /catalog/field-rules |
| BRPM.005 | FR-PM-013 | Package Request page (downloads) | .../form.pdf, quotation-slip.pdf, package-slip.pdf |
| BRPM.006 | FR-PM-052, FR-PM-045, FR-PM-071 | Products (Expired / Retired) | /catalog/products?lifecycle= |
| BRPM.007 | FR-PM-050, FR-PM-051 | Package Version; Quotation; Premium Calculator | /catalog/products/{code}/versions; /catalog/rating/quote; /catalog/rate-scheme-exceptions |
| BRPM.008 | FR-PM-020, FR-PM-021 | New Package Request; Package Requests | /product-maintenance/requests; .../submit; .../approve |
| BRPM.009 | FR-PM-022, FR-PM-023 | TSU Workbench; Package Request page | .../recommend; .../tsu-approve |
| BRPM.010 | FR-PM-032, FR-PM-033, FR-PM-036 | Package Request page (Negotiation) | .../rounds; .../terms-final |
| BRPM.011 | FR-PM-020, FR-PM-024, FR-PM-045 | Package Requests; TSU Workbench | /product-maintenance/requests; .../prefill; .../retire |
| BRPM.012 | FR-PM-030 | Package Request page (Negotiation) | .../rounds/{n}/quotation-slip/submit, approve; .../resend |
| BRPM.013 | FR-PM-031, FR-PM-034, FR-PM-036 | Package Request page (Negotiation, Comparative) | .../responses/{rid}; .../release-to-marketing; .../accept-terms |
| BRPM.014 | FR-PM-034 | Package Request page (Comparative) | .../comparatives; .../comparatives/master |
| BRPM.015 | FR-PM-040, 041, 042, 043 | Requirements & Sign-off; Set-up; Validation Queue | .../submit-requirements; .../signoff; .../setup; /catalog/.../validate |
| BRPM.016 | FR-PM-044 | Package Request page (Advisories) | /product-maintenance/advisories/{aid}/send |
| BRPM.017 | FR-PM-060, FR-PM-061, FR-PM-044 | Package Expiry | /product-maintenance/expiry; .../renewal-requests |
| BRPM.018 | FR-PM-071 | Reports | Report PM-PKG-STATUS |
| BRPM.019 | FR-PM-070 | Product Maintenance Home | /product-maintenance/counts |
| BRPM.020 | FR-PM-004 | E-mails and Documents tabs | Messaging outbox |
| BRPM.021 | FR-PM-073, FR-PM-021 | Workflow panel | Workflow PM_PACKAGE_REQUEST |
| BRPM.022 | FR-PM-072 | Notifications | Notifications; ProductMasterFeed |
| BRPM.024 | FR-PM-005 | History tabs | Audit service |
| PMADD01 | FR-PM-010 | Products; Coverages & Clauses | /catalog/lines, cover-types, coverages |
| PMADD02 | FR-PM-011, FR-PM-014 | Package Version (Insurer Terms); Coverages & Clauses | /catalog/products/{code}/versions/{n}; /catalog/clauses |
| PMADD03 | FR-PM-035 | Package Request page (Comparative) | .../comparatives (client view) |
| PMADD04 | FR-PM-031, FR-PM-032 | Package Request page (Negotiation) | .../responses; .../rounds |
| PMADD05 | FR-PM-002, FR-PM-003 | User Access Matrix; Access Requests | /nbadmin/access-matrix/by-action; /nbadmin/access-requests |
| PMADD06 | FR-PM-043 | Validation Queue; Package Version | /catalog/.../submit, validate, return |
| PMADD07 | FR-PM-080, FR-PM-081 | Incentive Criteria | /catalog/incentive-criteria |
| PMADD08 | FR-PM-080, FR-PM-081 | Incentive Criteria | /catalog/incentive-criteria |

API paths start with `/api/v1`; "..." stands for `/api/v1/product-maintenance/requests/{id}`.

# Sign-off

By signing, BDOI confirms that this FRS describes the Product Maintenance functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: "Head, Marketing Business Services and System Support", organisation: BDOI}
  - {name: "", role: "Head, Technical Support Unit", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
