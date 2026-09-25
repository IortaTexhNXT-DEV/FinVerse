---
# Source of the Functional Requirements Specification for BRD-7 Claims.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD07_CLAIMS.md
title: Claims
subtitle: BRD-7 Motor and Non-Motor Claims Logging, Renumbering Addendum and Workshop Addendum
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-07
name: Claims
doc_id: BIBS-FRS-BRD-07
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-7 Claims
output: FRS/BIBS_FRS_BRD-07_Claims_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-7 baseline and the Claims build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the cross-BRD decisions D3 and D4
distribution:
  - {name: "Product Owner, Claims", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "SAVP Unit Head, Claims, Risk Management and Analytics", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Claims Team Heads and Team Leads (Motor, Non-Motor)", role: Business user, organisation: BDOI, purpose: Review of the handling and closure steps}
  - {name: "Retail, Corporate and Commercial Marketing", role: Business user, organisation: BDOI, purpose: Review of the notice of loss and the loss information access}
  - {name: Operations (Remittance), role: Business user, organisation: BDOI, purpose: Review of the claims special remittance}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Claims business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as the business expects. The project team uses it to build, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

The Claims module is designed and not yet built. This FRS is written from the BRD, the requirements baseline (R4) and the build design (R5). Section 1.5 explains what that means for the screen, API and message-code entries.

## Scope

BDOI is a broker. The insurer accepts, evaluates and pays a claim; BDOI files and follows the claim on behalf of the client (p.24-26). The scope is **claims logging and monitoring**: the claim case file on a cover, the premium check, the insurer's claim numbers, reserve and settlement as reported by the insurer, the BDOI claim statuses, follow-up, closure, ageing and the claims reports.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access and audit | Role-based access to every Claims function, claim history, log-in and session rules | BRCLM.002, 005, 006, 011, 015, 018, 019, 021, 024; NFR p.32-36 |
| Cover and premium | Read-only cover lookup, cover number and version, policy number, Marketing unit and branch, premium check and claims authorization code | BRCLM.001-004, 007-009, 016, 039 |
| Locations and insurers | Several insured locations and several insurer claim numbers per claim, insurer-reported claims and updates, insurer location references, loss advice to the insurer | BRCLM.037, 041-043; p.24-25 |
| Claim details | Claimant override, adjuster / appraiser, insurer reserve, catastrophe codes | BRCLM.006, 017, 018, 023, 024, 036 |
| Status, settlement and closure | 18 claim statuses restricted by role and unit, 10 settlement types, temporary and permanent closure, reopen, claims special remittance | BRCLM.005, 010-015, 035 |
| Follow-up and ageing | Next follow-up date, next action plan, diary, age overall and per status, pending actions, Claims home | BRCLM.019-022, 025, 027, 034 |
| Reports and analytics | Ageing, outstanding, settled, loss experience, loss ratio, claims-prone locations, insurer claim numbers, data extract, Marketing access | BRCLM.026, 028-033, 038, 040 |

**Out of scope for this phase:**

- The insurer-side claims function of the platform (reserving, claim payments, recoveries, posting). BDOI records the insurer's reserve and settlement as information and posts nothing (Q44, R6).
- Claim money through BDOI (receipt of proceeds, payment to a claimant). The BRD has none: the insurer pays the assured or issues an LOA to the repair shop (p.24-26). The flow is described and parked (CLQ10, section 7).
- Insurer portals or system-to-system feeds for claims. Insurers are reached by e-mail; their updates are recorded or uploaded (CLQ17).
- Migration of open and historical EBIX / ISYS claims. The seam exists; scope and format wait for CLQ14.

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Claims - Addendum (Workshop), pages 1-9 of the BRD-7 pack (BRCLM.037-043) | v1.0, 08-Apr-2026; signed 10 to 16-Apr-2026 | `docs/source-documents/Claims (CLM).PDF` |
| R2 | Claims - Addendum (renumbering FRID-001-036 to BRCLM.001-036), pages 10-18 | v1.0, 17-Dec-2025; approved 17 to 22-Dec-2025 | same file |
| R3 | Motor and Non-Motor Claims Logging BRD, pages 19-45 (process, stakeholders, FRID table, NFR, capacity, retention, report list) | v1, 22-Jan-2025; signed 5 to 6-Mar-2025 | same file |
| R4 | BDOI Claims (BRD-7) requirements baseline and fit/gap | current | `docs/requirements/BDOI_CLM_BRD_SPEC.md` |
| R5 | Claims build design (module `brokerclaims`) | proposal for review | `docs/architecture/CLAIMS_BROKING_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions (BRD-6 to BRD-12) | binding | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R7 | BDOI Report List as of 27-Apr-2026 (Claims reports, p.39-42) | 27-Apr-2026 | `docs/source-documents/Report List as of APR-27-2026.pdf` |
| R8 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |

Page references in this document ("p.13") are pages of the BRD-7 PDF. BRCLM.001-036 appear twice with the same text, in the renumbering addendum (p.13-16) and as FRID-001-036 in the original BRD (p.29-31); FRs cite both, for example "BRCLM.001 (p.13; p.29)".

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing)
ARN: Account Reference Number; the BIBS account that is the cover of a claim
BRCLM: Requirement ID prefix of the Claims BRD (BRCLM.001-043)
CAC: Claims authorization code, numbered CAC-yyyy-nnnnnn (BRCLM.001; meaning to confirm, CLQ01)
CASA: Accredited repair shop or dealer for motor repairs (p.24)
CLQnn: Open question on BRD-7 raised by the project team (section 10.3)
CNR: Document in status "With Claimant - For Submission of CNR"; not defined in the BRD (CLQ12)
Cover: The account (ARN) and policy year a claim is made under; the EBIX "Cover Number" (CLQ02)
Cover version: The number of booked endorsements of the policy year effective on or before the loss date
CRF: Claims Reporting Form, completed by Marketing for Non-Motor claims (p.25)
EBIX / ISYS: Legacy core system / legacy claims reporting source
FR: Functional requirement of this document (FR-CL-nnn)
Insurer claim line: One insurer's part of a claim, with its claim number, share, reserve, settled amount and adjuster (BRCLM.043)
LOA: Letter of Authority issued by the insurer to the repair shop (p.24)
LOV: List of values maintained in BIBS
O/S: Outstanding loss amount (insurer reserve less paid)
Phase: One of NEW, IN_PROGRESS, TEMP_CLOSED, CLOSED; each claim status belongs to one phase
PLA: Preliminary Loss Advice (Motor, p.24)
TH: Team Head
TL: Team Lead
UAT: User acceptance testing
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R4), the **screens** and the **API**.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained in BIBS (parameter, list of values, matrix or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. A "-" marks a screen check (for example a blank mandatory field).
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-7 test plan.

> [!NOTE] Designed, not built
> Screen names, permissions, parameters and lists come from the build design (R5). API paths and business message codes are fixed when the module is built: the API entry reads "To be assigned at build" and the Code column reads "To be assigned at build". Codes quoted in this document exist in the platform today (for example ACCESS_DENIED). Values marked "default" are placeholders that BDOI confirms through the open questions in section 10.3; they are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the platform built for BRD-1 and BRD-2 |
| CONFIGURE | Needs set-up only (parameters, lists, rules) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-7 |

# Business context and process overview

## Business context

Claims are filed manually today (p.24-26). For **Motor** claims the client, AO or branch sends a notice of accident or loss; Marketing prepares a Preliminary Loss Advice (PLA) with the assured, policy or reference number, date and location of the loss, the nature of the loss and the initial loss reserve if known. The claim handler validates the PLA, sends it to the insurer and chases the client for documents. The insurer either issues a Letter of Authority (LOA) to an accredited repair shop, or makes a cash offer that the assured signs and the handler returns. For **Non-Motor** claims Marketing completes a Claims Reporting Form (CRF), Claims sends a formal loss advice to the insurer, an adjuster may inspect the site, and the claim closes after the insurer pays the insured.

The BRD asks for a claims facility with claim authorization tied to premium payment, cover visibility, date management, configurable statuses restricted by role and unit, claimant override, settlement types, adjusters, follow-up dates, action plans, a diary, the insurer reserve, ageing and reports (p.23, p.27). The workshop addendum of April 2026 adds several locations and several insurers per claim, insurer-reported updates, insurer location references, claims-prone location analysis and Marketing access to loss information (p.5-7).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.23-27)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | The notice of loss arrives by e-mail or call; Marketing prepares the PLA or CRF | The Claims Officer records the claim on the client's cover in BIBS; the claim number is BCL-yyyy-nnnnnn |
| 2 | Policy details are looked up in EBIX and re-keyed | The cover, policy number, version, locations, Marketing unit and branch come from the BIBS account, read-only |
| 3 | Premium payment is checked by hand | BIBS reads the invoice ledger; the authorization code is disabled while premium is unpaid |
| 4 | The loss advice goes to the insurer by e-mail from a mailbox | The loss advice is generated from the claim and e-mailed with a send log; insurer claim numbers are recorded per insurer |
| 5 | Claim status and follow-up are tracked in spreadsheets | Status, adjuster, reserve, next follow-up date, action plan and diary are on the claim, with history |
| 6 | Closure is tagged informally | Temporary and permanent closure are distinct; permanent closure needs a closing settlement type and a TL / TH |
| 7 | Claims reports are extracted from ISYS | Ageing, outstanding, settled, loss experience and loss ratio reports run in BIBS and export to Excel |

## Process overview

The table lists the steps of a claim, and Figure 1 shows them by actor. Section 5 describes the phases and the workflow BCL_CLAIM that run under these steps.

<!-- table: widths=0.8,3.8,3.4,7.2,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Notice of loss | Client, AO, branch | The PLA or CRF data reach Claims (by e-mail or call) or the insurer reports the claim first | p.24-25; BRCLM.041 |
| 2 | Record claim | Claims Officer | Claim on a cover (ARN, policy year, version) with reported date, loss date, nature, locations and insurers | BRCLM.003, 004, 037, 043 |
| 3 | Premium check | System | Invoices of the cover read from the ledger; unpaid premium disables the authorization code | BRCLM.001 |
| 4 | Loss advice | Claims Officer | Loss advice e-mailed to each insurer; insurer claim numbers recorded as they arrive | p.24-25; BRCLM.041, 043 |
| 5 | Handling | Claims Officer, TL, TH | Status within the role / unit matrix, adjuster, insurer reserve, insurer updates | BRCLM.010-013, 017-018, 023-024 |
| 6 | Follow-up | Claims Officer | Next follow-up date, next action plan, diary entries | BRCLM.019-022 |
| 7 | Premium remittance | Claims, Remittance | Status "With BDOI - For Premium Remittance"; the special remittance confirms the claim in BIBS | BRCLM.010; OQ46 |
| 8 | Settlement | TL, TH | Requested type of settlement, settlement amount and date | BRCLM.014, 015 |
| 9 | Temporary closure | Claims Officer | Status 17 or 18; the claim stays outstanding and can resume | BRCLM.035 |
| 10 | Permanent closure | TL, TH | Closing settlement type; reopen only with a reason | BRCLM.005, 035 |
| 11 | Ageing and reports | Claims, Unit Head | Age overall and per status, pending actions, claims reports | BRCLM.025-034 |
| 12 | Loss analytics | Risk, Marketing | Claims-prone locations, loss experience for renewal and pricing | BRCLM.038, 040 |

![Claim lifecycle by actor (BRCLM.001-043, BRD p.24-27)](figures/brd07_process_flow.dot)

## Motor and Non-Motor claims

Motor and Non-Motor claims use the same claim record and the same statuses. They differ in the documents and in the settlement path, which BIBS records through the status and the settlement type.

<!-- table: widths=3,7,7 caption="Motor and Non-Motor handling (BRD p.24-26)" -->
| Aspect | Motor | Non-Motor |
|---|---|---|
| Notice document | Preliminary Loss Advice (PLA), prepared by Marketing | Claims Reporting Form (CRF), completed by Marketing |
| Adjuster | Rarely; the insurer evaluates the estimate | Assigned by the insurer depending on the nature of loss; may inspect the site |
| Settlement | LOA to an accredited repair shop, or cash offer signed by the assured | Offer reviewed and possibly contested by Claims; signed by the insured |
| Typical settlement types | Settled - LOA Issued (and its repair variants); Settled - Signed Release Papers Returned | Settled; Settled - Directly Filed; Closed - Denied; Closed - within Deductible |
| Handling unit | Motor Head Office team | Non-Motor team (Head Office, North Luzon, Mindanao) |

# Personas and roles

## Personas

<!-- table: widths=3.4,3.1,8.1,3 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in Claims | BRD |
|---|---|---|---|
| Claims Officer / Claims Assistant | CLM_OFFICER | Records claims with cover and version; maintains the reported date; records insurer claim numbers and updates; encodes the action plan; tags temporary closures | p.28; BRCLM.003, 004, 021, 035 |
| Team Lead | CLM_TL | Updates status, settlement type and adjuster; overrides claimant and follow-up date; amends the insurer reserve; closes claims | p.28; BRCLM.005, 006, 011, 015, 018, 019, 024 |
| Team Head | CLM_TH | As the Team Lead; also reopens permanently closed claims | p.28; as above; CLQ06 |
| Unit Head | CLM_UH | Maintains statuses, settlement types, adjusters and catastrophe codes; sets the status matrix; grants access to cover numbers and to report data | p.28; BRCLM.002, 010, 012, 014, 017, 033, 036 |
| Claims / Risk user | CLM_RISK | Analyses claims-prone locations and loss patterns; uses the data extract | BRCLM.038 |
| Marketing user | MKT_AO, MKT_TL | Views loss information through the claims reports; exports as the role allows | BRCLM.040 |
| System Administrator | SYSADMIN | Parameters, roles, users; read access to Claims | NFR p.33 |
| Auditor | AUDITOR | Read access to claims and reports | NFR p.33-34 |

The BRD has no persona column for BRCLM.001-036; the persona comes from the stakeholder matrix (p.28) and the target process (p.27). The roles above are the project's proposal until BDOI confirms the matrix (CLQ04, CLQ06, CLQ15; OQ48).

## Permissions

<!-- table: widths=5.8,10.8 caption="Claims permissions (build design R5, section 7.1)" -->
| Permission | Allows |
|---|---|
| BCL_VIEW | Claims home, worklist and claim record (read) |
| BCL_COVER_VIEW | Cover Lookup: read-only account, endorsements, invoices and claims of the cover |
| BCL_RECORD | Record a claim; edit loss details, locations and insurer claim numbers; record insurer updates and diary entries |
| BCL_AUTHORIZE | Generate the claims authorization code when premium is paid |
| BCL_STATUS_UPDATE | Change the claim status within the status matrix; correct the reported date; resume a temporary closure |
| BCL_CLOSE | Permanent closure through a closing settlement type |
| BCL_REOPEN | Reopen a permanently closed claim, with a reason |
| BCL_CLAIMANT_OVERRIDE | Override or input the claimant's name |
| BCL_SETTLEMENT_UPDATE | Set or change the requested type of settlement, settlement amount and date |
| BCL_ADJUSTER_ASSIGN | Set or change the adjuster / appraiser |
| BCL_FOLLOW_UP_OVERRIDE | Override the next follow-up date |
| BCL_ACTION_PLAN | Encode the next action plan summary |
| BCL_RESERVE_AMEND | Amend the insurer reserve |
| BCL_LOCATION_REF_MAINTAIN | Maintain insurer location references (screen and upload) |
| BCL_SETUP | Status and settlement attributes, status access matrix, claims handler register, Claims lists of values |
| BCL_REPORT_VIEW | Run the Claims Handling reports on screen |
| BCL_REPORT_EXPORT | Download and print the Claims Handling reports |
| BCL_DATA_EXTRACT | Flat data extract for analytics |

## Permissions matrix

The table below is the proposed role-to-action matrix ("Y" = granted). Reassignment of claims between handlers uses the existing WORK_ASSIGN permission, granted to CLM_TL, CLM_TH and CLM_UH.

<!-- table: widths=4.9,1.35,1.35,1.35,1.35,1.35,1.35,1.35,1.35,1.35 caption="Role-to-action matrix for Claims (proposal until CLQ04, CLQ06, CLQ15)" size=8 -->
| Permission | CLM Officer | CLM TL | CLM TH | CLM UH | CLM Risk | MKT AO | MKT TL | Sys Admin | Auditor |
|---|---|---|---|---|---|---|---|---|---|
| BCL_VIEW | Y | Y | Y | Y | Y | | | Y | Y |
| BCL_COVER_VIEW | Y | Y | Y | Y | Y | | | | |
| BCL_RECORD | Y | Y | Y | | | | | | |
| BCL_AUTHORIZE | Y | Y | Y | | | | | | |
| BCL_STATUS_UPDATE | Y (matrix) | Y | Y | | | | | | |
| BCL_CLOSE | | Y | Y | | | | | | |
| BCL_REOPEN | | | Y | Y | | | | | |
| BCL_CLAIMANT_OVERRIDE | | Y | Y | | | | | | |
| BCL_SETTLEMENT_UPDATE | | Y | Y | | | | | | |
| BCL_ADJUSTER_ASSIGN | | Y | Y | | | | | | |
| BCL_FOLLOW_UP_OVERRIDE | | Y | Y | | | | | | |
| BCL_ACTION_PLAN | Y | Y | Y | | | | | | |
| BCL_RESERVE_AMEND | | Y | Y | | | | | | |
| BCL_LOCATION_REF_MAINTAIN | Y | Y | Y | | | | | | |
| BCL_SETUP | | | | Y | | | | | |
| BCL_REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| BCL_REPORT_EXPORT | Y | Y | Y | Y | Y | | Y | | |
| BCL_DATA_EXTRACT | | | | Y | Y | | | | |

The officer's status rights are limited by the status access matrix (FR-CL-041): by default officers may set the newly filed and temporary closure statuses, and TL / TH may set every status. Marketing never receives a claim maintenance permission (BRCLM.040).

# Functional requirements

## Access, security and audit

```fr
id: FR-CL-001
title: Log in to BIBS with lockout and session rules
brd: [NFR 1.01-2.03 (p.32-33), NFR 10.01-10.06 (p.35)]
actor: All Claims and Marketing users
priority: Must have
fit: CHANGE
screens: Login
api: POST /api/v1/auth/login (existing)
description:
  - Claims users log in with their BIBS user ID and password on the platform log-in built for BRD-1. The BRD asks for Windows credentials (NFR 1.01); directory sign-in is built as a parked port and local sign-in stays until BDO supplies the interface (decision D6, R6).
  - The account locks after three invalid attempts, and an idle session ends after the configured time. The Claims NFR asks for 15 minutes (NFR 10.01), which differs from other BRDs (CLQ24).
preconditions:
  - The user has an active BIBS account with at least one Claims role.
main_flow:
  - The user enters the user ID and password on the Login screen (password masked).
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the Claims Handling menu of the user's roles.
alternate_flows:
  - Wrong credentials. BIBS refuses the log-in with a plain message and counts the failed attempt.
  - Locked account. After three failed attempts the account locks; the System Administrator unlocks it.
  - Idle session. The session ends after the idle timeout; the user logs in again.
rules:
  - [R1, "Lockout after 3 invalid attempts for every BIBS user (decision D5).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R2, "Idle timeout per the BIBS session policy; the Claims value of 15 minutes is confirmed under CLQ24.", Configurable, Session policy (System Administrator)]
  - [R3, "Directory sign-in (Windows ID) through the parked DirectoryAuthenticator port; local sign-in until BDO supplies the interface (UQ04).", Configurable, Parameter AUTH_MODE]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded with user, time and source address; the log is visible to the System Administrator only.
acceptance:
  - A user with valid credentials and a Claims role logs in and sees the Claims Handling menu section.
  - The fourth attempt after three wrong passwords is refused because the account is locked.
  - An idle session ends after the configured timeout.
```

```fr
id: FR-CL-002
title: Restrict each Claims action to authorised roles
brd: [BRCLM.002 (p.13; p.29), BRCLM.005 (p.13; p.29), BRCLM.006 (p.13; p.29), BRCLM.011 (p.14; p.29), BRCLM.015 (p.14; p.30), BRCLM.018 (p.15; p.30), BRCLM.019 (p.15; p.30), BRCLM.021 (p.15; p.30), BRCLM.024 (p.16; p.30)]
actor: System; Unit Head (grants through User Access)
priority: High (BRD p.21)
fit: NEW
screens: All Claims Handling screens
api: Every Claims endpoint checks its permission
description:
  - Nine BRD requirements ask that a user "have the necessary permissions" before an action (p.28). Each Claims screen, button and API call requires one of the permissions of section 3.2. Menus show only the screens the user's roles allow; buttons for actions the user may not perform are hidden.
  - The permission grants of section 3.3 are changed through User Access Maintenance requests, which another user approves (BRD-11). The Unit Head requests the changes for the Claims roles.
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a claim or starts an action.
  - BIBS checks the user's permission for that action and, for a status change, the status matrix (FR-CL-041).
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The action is not offered; a direct call is refused (HTTP 403) and logged.
  - Claims record of another company. BIBS returns "not found" and does not reveal the record.
rules:
  - [R1, "Roles are granted the permissions of section 3.3 until BDOI confirms the matrix (OQ48).", Configurable, User Access Maintenance request]
  - [R2, "Marketing roles receive report permissions only, never claim maintenance (BRCLM.040).", Fixed, "-"]
  - [R3, "Every endpoint checks the claim's company, so object references cannot be manipulated (NFR access control, p.33).", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - Refused calls are logged with user, endpoint and time.
acceptance:
  - A Claims Officer does not see the Close action; a direct close call is refused with ACCESS_DENIED.
  - A Team Lead overrides the claimant name; a Claims Officer cannot.
  - A Marketing AO opens the Claims reports and does not see the Claims worklist.
```

```fr
id: FR-CL-003
title: Keep a complete claim history and audit trail
brd: [BRCLM.041 (p.6), BRCLM.042 (p.6), NFR 4.01-4.03 (p.33-34)]
actor: System; any user with BCL_VIEW (read)
priority: Must have
fit: FIT
screens: Claim record (History tab); Claims Activity Log report
api: Audit service (existing); history to be assigned at build
description:
  - Every change on a claim is kept twice. The claim keeps its own history for each item (status, reserve, claimant, adjuster, follow-up date, action plan, cover version, authorization code, insurer updates, location references), and each change writes an audit entry with before and after values.
  - The History tab shows the timeline of the claim with user and time. The Claims Activity Log report lists the same entries across claims (FR-CL-066).
preconditions:
  - "None."
main_flow:
  - A user changes a claim.
  - BIBS writes the history row and the audit entry in the same transaction.
  - The History tab shows the change in time order.
rules:
  - [R1, "History and audit rows are append-only; no user can change or delete them.", Fixed, "-"]
  - [R2, "Claims data: 10 years online, 15 years archive, purge after 15 years (p.41); the audit-log archive of 16 years is confirmed under CLQ24.", Configurable, Retention rule for record type BrokerClaim]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - For a closed claim, the History tab lists every status change, the settlement and the closure with user and time.
  - A reserve amendment shows the previous and new amounts and the reason.
  - No screen or API allows history rows to be edited or deleted.
```

## Cover, policy and premium

```fr
id: FR-CL-010
title: Look up a cover (read-only)
brd: [BRCLM.002 (p.13; p.29), BRCLM.003 (p.14; p.29)]
actor: Claims Officer / Assistant, TL, TH, Unit Head, Claims / Risk user
priority: High (BRD p.21)
fit: CHANGE
screens: Cover Lookup
api: To be assigned at build
description:
  - Claims users check the policy coverages of any cover without account maintenance rights. The Cover Lookup shows the account header (client, product and line, insurer, period, currency, sum insured), the risk items and locations, the endorsements of each policy year, the invoices with payment and remittance status, the claims of the cover and the insurer location references.
  - All covers are visible to the Claims roles, with no portfolio or branch restriction (BRCLM.003), until BDOI decides otherwise for branch users (CLQ27).
preconditions:
  - The user has BCL_COVER_VIEW.
main_flow:
  - The user opens Cover Lookup and searches by ARN, policy number or assured name.
  - BIBS lists the matching accounts.
  - The user opens a cover; BIBS shows its data read-only.
  - From the cover the user starts **Record Claim** (FR-CL-011).
alternate_flows:
  - No match. BIBS shows "No cover found for <criteria>".
rules:
  - [R1, "The cover is the BIBS account (ARN) and policy year; the EBIX Cover Number maps to it (CLQ02).", Fixed, "-"]
  - [R2, "Cover data is view-only for Claims users (BRCLM.039 AC3).", Fixed, "-"]
validations:
  - [Search text shorter than 3 characters, Enter at least 3 characters, "-"]
fields_screen: Cover Lookup (search)
fields:
  - [Search by, List, "Yes", ARN / Policy No. / Assured, "-"]
  - [Search text, Text, "Yes", "-", At least 3 characters]
  - [Policy year, List, "No", Policy years of the account, "-"]
notifications:
  - "None."
audit:
  - "Opening a cover is logged as a view with user and time."
acceptance:
  - A Claims Officer finds a cover by policy number and sees its endorsements and invoice payment status.
  - The Cover Lookup offers no edit action on the account.
  - A user without BCL_COVER_VIEW cannot open Cover Lookup.
```

```fr
id: FR-CL-011
title: Record a claim on a cover
brd: [BRCLM.003 (p.14; p.29), BRCLM.009 (p.13; p.29), BRCLM.041 (p.6)]
actor: Claims Officer / Assistant (BCL_RECORD)
priority: High (BRD p.21); BRCLM.041 Must have
fit: NEW
screens: Record Claim; Claim record
api: To be assigned at build
description:
  - The Claims Officer records the claim against a cover. The cover number (ARN and policy year) and the cover version are required (BRCLM.003). The record holds the loss data of the PLA or CRF (p.24-25) and becomes one claim reference, whatever the number of locations and insurers.
  - The claim currency defaults to the cover currency, else to Philippine Peso (BRCLM.009). The claim source is BDOI notice, or insurer-reported when the insurer notifies the loss first (BRCLM.041).
  - BIBS takes a snapshot of the cover (policy number, version, period, sum insured, lead insurer, Marketing unit, AO and branch) so that reports keep the values at recording.
preconditions:
  - The user has BCL_RECORD.
  - The cover exists in BIBS.
main_flow:
  - The officer clicks **Record Claim** and searches the cover.
  - BIBS shows the cover card with policy number, version, period, premium check (FR-CL-016), Marketing unit, AO and branch.
  - The officer picks the insured locations of the claim (FR-CL-020).
  - The officer enters the loss details and the reported date.
  - BIBS proposes the insurers from the invoice shares of the cover; the officer confirms them (FR-CL-021).
  - The officer saves. BIBS assigns the claim number, sets the first status and phase NEW, and computes the next follow-up date.
alternate_flows:
  - Loss date outside the cover period. BIBS warns and asks for confirmation; recording on an expired or cancelled cover waits for CLQ02.
  - Insurer-reported claim. The officer sets the source to insurer-reported and enters the insurer's claim number at recording.
  - Unpaid premium. The claim is saved and flagged; only the authorization code is blocked (FR-CL-016).
rules:
  - [R1, "Claim numbers are BCL-<yyyy>-nnnnnn (format to confirm, CLQ13).", Configurable, Document number series BCL]
  - [R2, "Cover number, policy year and cover version are mandatory.", Fixed, "-"]
  - [R3, "Currency defaults to the cover currency, else PHP.", Configurable, Parameter BCL_DEFAULT_CURRENCY]
  - [R4, "The claim number never changes; one claim reference per incident.", Fixed, "-"]
  - [R5, "The claimant defaults to the assured (FR-CL-030).", Fixed, "-"]
validations:
  - [Cover not selected, Select the cover of the claim, "-"]
  - [Loss date blank or after today, Enter a loss date that is not in the future, "-"]
  - [Reported date before the loss date or after today, The reported date must be between the loss date and today, To be assigned at build]
  - [Nature of loss not selected, Select the nature of loss, "-"]
  - [Loss date outside the cover period, "The loss date is outside the cover period <from> to <to>. Confirm to continue", To be assigned at build]
fields_screen: Record Claim
fields:
  - [Cover (ARN / policy no.), Look-up, "Yes", Accounts, Existing account]
  - [Policy year, List, "Yes", Policy years of the cover, "-"]
  - [Cover version, Display, "Yes", Endorsements of the policy year, Computed at the loss date]
  - [Source, Option, "Yes", BDOI notice / Insurer-reported, "-"]
  - [Loss date, Date, "Yes", "-", Not after today]
  - [Reported date, Date, "Yes", "-", Between loss date and today]
  - [Nature of loss, List, "Yes", LOV BCL_LOSS_NATURE, "-"]
  - [Claim type, List, "Yes", LOV BCL_CLAIM_TYPE, "-"]
  - [Loss description, Long text, "Yes", "-", Up to 2000 characters]
  - [Place of loss (motor), Text, "No", "-", Up to 200 characters]
  - [Claim amount, Amount, "No", "-", ">= 0"]
  - [Deductible, Amount, "No", "-", ">= 0"]
  - [Initial loss reserve, Amount, "No", "-", ">= 0"]
  - [Currency, List, "Yes", Currency master, Default cover currency or PHP]
  - [Catastrophe code, List, "No", LOV BCL_CATASTROPHE, FR-CL-033]
notifications:
  - "The claim is placed in the handler's My Claims queue; the AO of the account is notified in the app (BCL_CLAIM_ASSIGNED)."
audit:
  - "Creation with the full cover snapshot and loss data."
acceptance:
  - A claim cannot be saved without cover number, policy year and cover version.
  - A new claim on a USD cover has currency USD; on a cover without currency it has PHP.
  - A recorded claim has a BCL number, phase NEW and a next follow-up date.
  - An insurer-reported claim shows the source "Insurer-reported" and the insurer claim number.
```

```fr
id: FR-CL-012
title: Maintain the reported date
brd: [BRCLM.004 (p.13; p.29)]
actor: Claims Officer / Assistant (at recording); BCL_STATUS_UPDATE holder (correction)
priority: High (BRD p.21)
fit: NEW
screens: Record Claim; Claim record (Details tab)
api: To be assigned at build
description: The reported date is the only operational date users maintain on a claim. It is entered at recording and is the start of every claim age (FR-CL-053). Other dates (status changes, closure, updates) are system time stamps. A correction of the reported date on an open claim needs BCL_STATUS_UPDATE and a reason, and is kept in the history.
preconditions:
  - "The claim is open (phase NEW, IN_PROGRESS or TEMP_CLOSED) for a correction."
main_flow:
  - The user clicks **Correct Reported Date** on the Details tab.
  - The user enters the new date and the reason.
  - BIBS saves the date, recomputes the ages and records the change.
rules:
  - [R1, "The reported date is between the loss date and today.", Fixed, "-"]
  - [R2, "Whether the date is the report to BDOI or to the insurer is confirmed under CLQ03; default = report to BDOI.", Configurable, Field label and help text]
validations:
  - [Reason blank on correction, Enter the reason for the change, "-"]
  - [Date outside the allowed range, The reported date must be between the loss date and today, To be assigned at build]
  - [Correction on a closed claim, The reported date of a closed claim cannot change, To be assigned at build]
fields_screen: Correct Reported Date
fields:
  - [Reported date, Date, "Yes", "-", Between loss date and today]
  - [Reason, List, "Yes", LOV BCL_OVERRIDE_REASON, "-"]
  - [Remark, Text, "No", "-", Up to 500 characters]
notifications:
  - "None."
audit:
  - "Old and new date, reason, user and time."
acceptance:
  - A reported date later than today is refused.
  - A corrected reported date changes the age overall of the claim on the next report run.
  - A Claims Officer without BCL_STATUS_UPDATE cannot correct the date after recording.
```

```fr
id: FR-CL-013
title: Show the policy number without re-keying
brd: [BRCLM.007 (p.13; p.29), BRCLM.008 (p.13; p.29)]
actor: System
priority: High (BRD p.21)
fit: FIT
screens: Record Claim (cover card); Claim record (summary); invoice screens of BRD-1 and BRD-2
api: None (read from the account)
description:
  - The BRD asks to get the policy number from EBIX and upload it to Claims, and to show it on the invoice because the insurer asks for it when a claim is reported. In BIBS the policy number is recorded on the account by issuance, one per policy year, and printed on every invoice. The claim copies the number of its policy year; nobody uploads or re-keys it.
preconditions:
  - The policy of the policy year has been issued in BIBS.
main_flow:
  - The officer selects the cover and policy year.
  - BIBS copies the policy number into the claim snapshot and shows it on the summary card.
alternate_flows:
  - Policy not yet issued. The claim shows "Policy number pending"; it is filled when the policy is issued and the snapshot is refreshed.
  - Legacy policy. Covers and claims migrated from EBIX / ISYS carry their legacy numbers (CLQ14).
rules:
  - [R1, "The account is the master of the policy number; Claims keeps a copy and never edits it.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The snapshot records the policy number used."
acceptance:
  - A claim on an issued cover shows the policy number of the policy year without any entry by the user.
  - The invoice of the cover shows the same policy number.
```

> [!NOTE] Difference from the BRD
> BRCLM.007 describes an upload from EBIX. In BIBS the policy number is already on the account, so no upload is built. Legacy numbers enter through the migration (CLQ14).

```fr
id: FR-CL-014
title: Show the Marketing team, AO and branch of the policy
brd: [BRCLM.016 (p.14; p.30)]
actor: Claims users
priority: High (BRD p.21)
fit: CHANGE
screens: Record Claim (cover card); Claim record (summary card); Claims reports
api: None (snapshot)
description: The claim shows the Marketing team / unit and the account officer of the policy, and the BDOI branch, taken from the sales stamp of the account and the invoicing branch at recording. Every Claims report carries the Marketing Team and Account Officer columns (p.42-43). The user can refresh the snapshot when the account's sales stamp has changed.
preconditions:
  - "The claim is recorded."
main_flow:
  - BIBS copies region, department, team, account officer, cost centre and invoicing branch into the claim.
  - The summary card shows team, AO and branch.
  - The user clicks **Refresh Cover Data** to reload the current values.
rules:
  - [R1, "Reports use the values stored on the claim.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "A refresh records the old and new values."
acceptance:
  - A new claim shows the Marketing team and AO of the account.
  - The Outstanding Claims report lists the Marketing Team and Account Officer of each claim.
```

```fr
id: FR-CL-015
title: Show the cover version and flag a newer version
brd: [BRCLM.039 (p.5-6), BRCLM.003 (p.14; p.29)]
actor: Claims user; System
priority: Must have
fit: CHANGE
screens: Claim record (summary card, flag "Newer cover version")
api: To be assigned at build
description:
  - The claim displays the cover number and version used, as "Cover v<n> (<endorsement no.>)". The version is the number of booked endorsements of the policy year effective on or before the loss date. When the policy year has a later endorsement, the claim shows the flag **Newer cover version**; the user reviews the latest terms and may switch the claim to the latest version.
  - Policy data stays view-only for Claims users (AC3).
preconditions:
  - "The claim is recorded."
main_flow:
  - BIBS computes the version at recording and on each read.
  - When an endorsement of the policy year is booked, BIBS sets the flag and notifies the handler.
  - The handler opens the cover panel and reviews the endorsement.
  - The handler clicks **Use Latest Version**; BIBS updates the version and records the change.
rules:
  - [R1, "Version = count of booked endorsements of the policy year effective on or before the loss date.", Fixed, "-"]
  - [R2, "Location lists are not versioned per endorsement until CLQ02 is answered.", Fixed, "-"]
validations: []
notifications:
  - "BCL_NEWER_COVER_VERSION to the handler when an endorsement of the cover is booked."
audit:
  - "Version change with old and new version, user and time."
acceptance:
  - A claim shows the cover number and the version used.
  - After an endorsement of the policy year is booked, the claim shows "Newer cover version".
  - The cover panel of the claim has no edit action.
```

```fr
id: FR-CL-016
title: Check premium payment and control the claims authorization code
brd: [BRCLM.001 (p.13; p.29), NFR 15.05 (p.39)]
actor: System; Claims Officer, TL, TH (BCL_AUTHORIZE)
priority: High (BRD p.21)
fit: NEW
screens: Record Claim (cover card); Claim record (flag "Unpaid premium", action Generate Authorization Code)
api: To be assigned at build
description:
  - BIBS identifies unpaid invoices of the cover. It reads the invoices of the ARN and policy year (originals, endorsements and cancellations; cancelled invoices excluded) from the invoice ledger with their payment status. The result is PAID, UNPAID, PARTIALLY_PAID, DIRECT_PAYMENT or NO_INVOICE, with the list of unpaid invoices and their balances.
  - The claims authorization code (CAC-yyyy-nnnnnn) can be generated only when the result is PAID. For direct-payment accounts (premium paid to the insurer) the rule is set by a parameter. The button is disabled otherwise, and the claim shows the flag **Unpaid premium**.
  - The check re-runs when a payment is applied to an invoice of the cover and in a daily job, so a claim becomes authorisable as soon as the premium is paid.
preconditions:
  - "The claim is recorded."
main_flow:
  - BIBS runs the premium check when the claim is recorded and shows the result on the cover card.
  - The officer opens the claim; the Generate Authorization Code button is enabled only when the result is PAID.
  - The officer clicks **Generate Authorization Code**; BIBS issues the code and records who and when.
alternate_flows:
  - Unpaid or partly paid premium. The button is disabled; the claim lists the unpaid invoices and raises the alert BCL_UNPAID_PREMIUM_CLAIM.
  - Payment received later. The check re-runs on the payment event; the handler is notified and the button is enabled.
  - Direct payment. With the default setting the officer attaches the insurer's payment evidence before the code is issued.
rules:
  - [R1, "Code only when the premium check is PAID.", Fixed, "-"]
  - [R2, "Direct-payment accounts: ALLOW, CONFIRM (evidence attached) or BLOCK; default CONFIRM.", Configurable, Parameter BCL_AUTH_DP_POLICY]
  - [R3, "Cancelled invoices are excluded from the check.", Fixed, "-"]
  - [R4, "The meaning and use of the code, and any TL / TH override, are confirmed under CLQ01.", Configurable, Change request after CLQ01]
validations:
  - [Code requested while premium is unpaid, "The premium of <ARN> <policy year> is not fully paid. The authorization code cannot be generated", To be assigned at build]
  - [Direct payment without evidence, Attach the insurer's payment evidence first, To be assigned at build]
notifications:
  - "BCL_UNPAID_PREMIUM_CLAIM alert to the handler when a claim is recorded on a cover with unpaid premium."
  - "The handler is notified when the premium of the cover becomes fully paid."
audit:
  - "Each check (time, result) and the code generation (user, time) are recorded."
acceptance:
  - On a cover with one unpaid endorsement invoice, the claim shows "Unpaid premium" with the invoice and its balance, and the code cannot be generated.
  - After the invoice is paid in Cashiering, the claim shows PAID and the code can be generated.
  - A cancelled unpaid invoice does not block the code.
  - The premium check answers within 5 seconds (NFR 15.05).
```

## Locations, insurers and insurer communication

```fr
id: FR-CL-020
title: Link several insured locations to one claim
brd: [BRCLM.037 (p.5)]
actor: Claims user (BCL_RECORD)
priority: Must have
fit: NEW
screens: Record Claim (locations picker); Claim record (Locations tab)
api: To be assigned at build
description: A claim can reference more than one insured location of its cover. Locations are chosen only from the location list of the cover (risk items of kind location); BIBS stores each with its address, city, province and normalised location key. The claim keeps one claim number whatever the number of locations. Each linked location shows its insurer location references (FR-CL-023).
preconditions:
  - The cover has at least one insured location.
main_flow:
  - On Record Claim, or on the Locations tab, the user opens the location picker.
  - BIBS lists the locations of the cover.
  - The user ticks one or more locations and adds a description of the damage per location.
  - BIBS links them to the claim; the flag **Multi-location** is shown when there are two or more.
alternate_flows:
  - Motor or non-location cover. The picker is not shown; the place of loss is entered as text.
  - Remove a location. Allowed while the claim is open; the removal is recorded.
rules:
  - [R1, "Only locations of the claim's cover may be linked.", Fixed, "-"]
  - [R2, "A location is linked once per claim.", Fixed, "-"]
  - [R3, "One claim reference regardless of the number of locations (AC4).", Fixed, "-"]
validations:
  - [Location not on the cover, "Location <item> is not on the cover <ARN>", To be assigned at build]
  - [Location already linked, "Location <item> is already on this claim", To be assigned at build]
fields_screen: Locations tab
fields:
  - [Location, Multi-select, "Yes", Locations of the cover, Location item of the cover]
  - [Damage description, Text, "No", "-", Up to 500 characters]
notifications:
  - "None."
audit:
  - "Link and removal with user and time."
acceptance:
  - A claim on a property cover with three locations can link two of them and keeps one claim number.
  - A location of another account cannot be linked.
  - Each linked location is viewable on the Locations tab with its address.
```

```fr
id: FR-CL-021
title: Record several insurer claim numbers under one claim
brd: [BRCLM.043 (p.7)]
actor: Claims user (BCL_RECORD)
priority: Must have
fit: NEW
screens: Claim record (Insurers & Updates tab); Claims worklist (search)
api: To be assigned at build
description:
  - A claim incident can involve several insurers, each issuing its own claim number. The claim holds one insurer claim line per insurer, with the insurer, its share, the insurer claim number, the date reported to the insurer, the reserve, the settled amount and the adjuster. BIBS proposes the insurers and shares from the invoice shares of the cover.
  - Insurer claim numbers are visible on the claim with the insurer name, searchable from the worklist and listed in the Insurer Claim Numbers report (FR-CL-066). Insurer claim numbers can also be loaded by upload.
preconditions:
  - The claim is open.
main_flow:
  - The user opens Insurers & Updates and clicks **Add Insurer Claim Number** on an insurer line.
  - The user enters the number and the date reported to the insurer.
  - BIBS checks for duplicates and saves; the flag **Multi-insurer** shows when two or more insurers have lines.
alternate_flows:
  - The same insurer number exists on another claim. BIBS warns, shows the other claim and asks for confirmation; the alert BCL_INSURER_CLAIM_NO_REUSED is raised.
  - Upload. The user uploads a file of claim numbers (claim no., insurer, insurer claim no., date reported); BIBS validates every row before it applies the file.
rules:
  - [R1, "The combination claim, insurer and insurer claim number is unique (AC6).", Fixed, "-"]
  - [R2, "Each insurer claim number is linked to one insurer (AC2).", Fixed, "-"]
  - [R3, "Shares default from the invoice shares and may be edited; whether one incident can span several ARNs is confirmed under CLQ19.", Configurable, Insurer claim line]
validations:
  - [Duplicate number for the same insurer and claim, "Insurer claim number <no.> is already recorded for <insurer> on this claim", To be assigned at build]
  - [Insurer not selected, Select the insurer, "-"]
  - [Date reported to insurer after today, The date cannot be in the future, "-"]
fields_screen: Insurer claim line
fields:
  - [Insurer, List, "Yes", Insurers of the cover, Insurer on the invoice shares or added with reason]
  - [Share %, Number, "No", Invoice shares, "0-100"]
  - [Insurer claim number, Text, "No", "-", "Up to 60 characters; unique per insurer and claim"]
  - [Date reported to insurer, Date, "No", "-", Not after today]
  - [Adjuster, List, "No", LOV BCL_ADJUSTER, FR-CL-031]
notifications:
  - "None."
audit:
  - "Every insurer line and number change with user and time."
acceptance:
  - A claim with two insurers holds two insurer claim numbers and remains one claim.
  - Entering the same number twice for the same insurer on the same claim is refused.
  - Searching the worklist by an insurer claim number opens the BDOI claim.
```

```fr
id: FR-CL-022
title: Record insurer-reported claims and insurer updates
brd: [BRCLM.041 (p.6)]
actor: Claims user (BCL_RECORD)
priority: Must have
fit: NEW
screens: Claim record (Insurers & Updates tab, timeline); Bulk Upload (BCL_INSURER_UPDATE)
api: To be assigned at build
description:
  - Insurer communications are recorded on the claim so that they are central and auditable. A claim first reported by the insurer is recorded with source "Insurer-reported" (FR-CL-011). Each insurer update captures the date, the source (e-mail, letter, portal, call, file), the insurer's reference, remarks and attachments, and can be linked to an insurer claim line.
  - Updates are insert-only and form part of the claim timeline. Insurer bordereaux can be loaded in bulk.
preconditions:
  - The claim exists.
main_flow:
  - The user clicks **Record Insurer Update**.
  - The user enters the date, source, reference and remarks, and attaches the insurer's document.
  - BIBS saves the update and shows it in the timeline.
alternate_flows:
  - Bulk upload. The user uploads the file (claim no. or insurer and insurer claim no., update date, source, reference, remarks); BIBS validates every row, matches the claim and applies the file only when all rows are valid.
  - Wrong update. It cannot be edited; the user records a correcting update that refers to it.
rules:
  - [R1, "Insurer updates are never edited or deleted.", Fixed, "-"]
  - [R2, "Sources: e-mail, letter, portal, call, file (to confirm, CLQ17).", Configurable, LOV BCL_UPDATE_SOURCE]
validations:
  - [Update date blank or in the future, Enter an update date that is not in the future, "-"]
  - [Remarks blank, Enter the remarks, "-"]
  - [Upload row matches no claim, "Row <n>: no claim found for <insurer> <insurer claim no.>", To be assigned at build]
fields_screen: Record Insurer Update
fields:
  - [Insurer claim line, List, "No", Insurer lines of the claim, "-"]
  - [Update date, Date, "Yes", "-", Not after today]
  - [Source, List, "Yes", LOV BCL_UPDATE_SOURCE, "-"]
  - [Insurer reference, Text, "No", "-", Up to 100 characters]
  - [Remarks, Long text, "Yes", "-", Up to 2000 characters]
  - [Attachments, Attachment, "No", Document types BCL_DOCUMENT_TYPE, Allowed file types of the platform]
notifications:
  - "The handler is notified of updates loaded by upload."
audit:
  - "Each update is recorded with user and time; uploads are recorded with the file and its row results."
acceptance:
  - An insurer update with date, source and remarks appears in the claim timeline.
  - An update cannot be edited after saving.
  - A file of 50 updates with one unmatched row is refused as a whole and the row is named.
```

```fr
id: FR-CL-023
title: Maintain insurer location references
brd: [BRCLM.042 (p.6)]
actor: Claims user (BCL_LOCATION_REF_MAINTAIN)
priority: Must have
fit: NEW
screens: Insurer Location References; Cover Lookup; Claim record (Locations tab); Bulk Upload (BCL_LOCATION_REF)
api: To be assigned at build
description: For each insured location of a cover and each insurer, BIBS holds the location reference the insurer uses, with effective dates. Both the internal location and the insurer references are shown on every claim location. A change closes the current reference and opens a new one, so the mapping history is kept and auditable. References are maintained on screen or loaded by upload.
preconditions:
  - The cover has insured locations.
main_flow:
  - The user opens Insurer Location References and searches by ARN.
  - The user selects a location and an insurer and enters the insurer's reference and the effective date.
  - BIBS closes the previous reference of that location and insurer and saves the new one.
rules:
  - [R1, "One current reference per location and insurer.", Fixed, "-"]
  - [R2, "A change end-dates the previous reference; nothing is deleted.", Fixed, "-"]
  - [R3, "Where the references come from (policy schedule, placement, the insurer at claim time) is confirmed under CLQ18.", Configurable, Change request after CLQ18]
validations:
  - [Reference blank, Enter the insurer location reference, "-"]
  - [Effective date before the current reference's start, The effective date must be after <date>, To be assigned at build]
fields_screen: Insurer Location Reference
fields:
  - [Cover (ARN), Look-up, "Yes", Accounts, Existing account]
  - [Location, List, "Yes", Locations of the cover, "-"]
  - [Insurer, List, "Yes", Insurers of the cover, "-"]
  - [Insurer location reference, Text, "Yes", "-", Up to 60 characters]
  - [Effective from, Date, "Yes", "-", After the current reference's start]
notifications:
  - "None."
audit:
  - "Each reference with its effective dates, user and time; the Claims Activity Log lists the changes."
acceptance:
  - A claim location shows the internal address and the reference of each insurer.
  - Changing a reference keeps the old one with its end date.
```

```fr
id: FR-CL-024
title: Send the loss advice to the insurer
brd: ["Process p.24-25 (PLA, formal loss advice)", NFR 15.14 (p.40), BRCLM.041 (p.6)]
actor: Claims Officer
priority: High (BRD p.21)
fit: NEW
screens: Claim record (action Send Loss Advice; Documents tab; E-mails)
api: To be assigned at build
description:
  - The loss advice (PLA for Motor, formal loss advice for Non-Motor) is generated from the claim with the fields of p.24-25 (assured, policy or reference number, date and location of loss, nature of loss, initial loss reserve, assigned adjuster) and the insurer claim numbers known so far. It is e-mailed to the claims e-mail of each insurer with a send log.
  - The generated advice is stored with the claim as document type CLAIM_REPORT, linked to the claim, the account and the client, so the contact centre can retrieve it (decision D3; BRCSF-009).
preconditions:
  - The claim has at least one insurer line.
main_flow:
  - The officer clicks **Send Loss Advice** and selects the insurers.
  - BIBS generates the advice from template BCL_LOSS_ADVICE and shows the preview.
  - The officer confirms; BIBS sends one e-mail per insurer and logs each send.
alternate_flows:
  - Delivery failure. The e-mail shows the failed status; the officer resends it.
  - Insurer without a claims e-mail. BIBS asks for the recipient address.
rules:
  - [R1, "The template layout is a draft until BDOI provides its layout (CLQ22).", Configurable, Document template BCL_LOSS_ADVICE]
  - [R2, "Each generated advice is stored as CLAIM_REPORT linked to the claim, account and client.", Fixed, "-"]
validations:
  - [No insurer selected, Select at least one insurer, "-"]
  - [Recipient address invalid, Enter a valid e-mail address, EMAIL_ADDRESS_INVALID]
notifications:
  - "The insurer receives the loss advice by e-mail."
audit:
  - "Each e-mail with recipient, time, subject and attachment name."
acceptance:
  - The loss advice of a Non-Motor claim shows the assigned adjuster and the insurer claim numbers.
  - The sent advice appears on the Documents tab as a claims report and in the client's documents.
```

## Claim details

```fr
id: FR-CL-030
title: Override or input the claimant's name
brd: [BRCLM.006 (p.13; p.29)]
actor: Team Lead, Team Head (BCL_CLAIMANT_OVERRIDE)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Details tab, action Override Claimant)
api: To be assigned at build
description: The claimant defaults to the assured of the cover. A TL or TH may override it or input another name (for example a third-party claimant) with a reason. The claim shows the flag "Claimant overridden". Whether a claimant record with contact and payee details is needed is confirmed under CLQ25.
preconditions:
  - "The claim is open."
main_flow:
  - The TL clicks **Override Claimant**.
  - The TL enters the claimant name and the reason.
  - BIBS saves the name, sets the flag and records the change.
rules:
  - [R1, "Default claimant = assured name of the cover.", Fixed, "-"]
validations:
  - [Name blank, Enter the claimant's name, "-"]
  - [Reason blank, Enter the reason for the change, "-"]
fields_screen: Override Claimant
fields:
  - [Claimant name, Text, "Yes", "-", Up to 200 characters]
  - [Reason, List, "Yes", LOV BCL_OVERRIDE_REASON, "-"]
notifications:
  - "None."
audit:
  - "Old and new name, reason, user and time."
acceptance:
  - A new claim shows the assured as claimant.
  - A Team Lead overrides the claimant with a reason; the claim shows "Claimant overridden".
  - A Claims Officer does not see the override action.
```

```fr
id: FR-CL-031
title: Maintain adjusters / appraisers and assign them to claims
brd: [BRCLM.017 (p.14-15; p.30), BRCLM.018 (p.15; p.30)]
actor: Unit Head (list); Team Lead, Team Head (assignment)
priority: High (BRD p.21)
fit: "CONFIGURE (BRCLM.017), NEW (BRCLM.018)"
screens: Broking Setup, Lists of values (BCL_ADJUSTER); Claim record (action Assign Adjuster)
api: To be assigned at build
description:
  - The adjuster / appraiser list is a list of values seeded with the 25 companies of the BRD (section 9.2). The Claims Unit Head maintains it, with maker-checker and effective dates.
  - A TL or TH sets or changes the adjuster of a claim, or of one insurer line when insurers appoint different adjusters. Each change is kept in the history.
preconditions:
  - "The user has BCL_SETUP (list) or BCL_ADJUSTER_ASSIGN (claim)."
main_flow:
  - The TL clicks **Assign Adjuster** on the claim or on an insurer line.
  - The TL selects the adjuster from the list.
  - BIBS saves the assignment and records the change.
alternate_flows:
  - New adjuster company. The Unit Head adds it to the list; another authoriser approves it; it becomes selectable.
rules:
  - [R1, "Adjusters are list values with effective dates; an ended value stays on the claims that used it.", Configurable, LOV BCL_ADJUSTER]
  - [R2, "The Unit Head maintains the Claims lists without the global list-maintenance right (list owner permission BCL_SETUP).", Configurable, List type owner permission]
validations:
  - [Adjuster not selected, Select the adjuster, "-"]
fields_screen: Assign Adjuster
fields:
  - [Applies to, Option, "Yes", Whole claim / Insurer line, "-"]
  - [Adjuster / appraiser, List, "Yes", LOV BCL_ADJUSTER, Effective value]
notifications:
  - "None."
audit:
  - "List changes with maker and checker; assignments with old and new adjuster, user and time."
acceptance:
  - The adjuster list offers the 25 companies of the BRD.
  - A Team Lead assigns an adjuster; a Claims Officer cannot.
  - An adjuster added by the Unit Head is selectable only after another user authorises it.
```

```fr
id: FR-CL-032
title: Record and amend the insurer reserve
brd: [BRCLM.023 (p.16; p.30), BRCLM.024 (p.16; p.30)]
actor: Claims Officer (initial reserve at recording); Team Lead, Team Head (amend)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Reserve & Settlement tab, action Amend Reserve)
api: To be assigned at build
description:
  - The insurer reserve is the insurer's own estimate of the loss, recorded by BDOI as information for monitoring and loss experience. It is held per insurer claim line; the initial loss reserve of the PLA is the starting value. BDOI keeps no reserve of its own and posts no journal (Q44).
  - Only a TL or TH amends the reserve, with a reason. Every amendment is kept with the previous and new amount.
preconditions:
  - "The claim is open and has an insurer line."
main_flow:
  - The TL clicks **Amend Reserve** on an insurer line.
  - The TL enters the new amount and the reason.
  - BIBS saves the amount and adds a row to the reserve history.
rules:
  - [R1, "Reserve per insurer line; the claim total is the sum of the lines.", Fixed, "-"]
  - [R2, "No accounting entry is produced by a reserve change.", Fixed, "-"]
  - [R3, "Whether amendments need approval is confirmed under CLQ08; none by default.", Configurable, Change request after CLQ08]
validations:
  - [Amount negative, The reserve cannot be negative, "-"]
  - [Reason blank, Enter the reason for the change, "-"]
fields_screen: Amend Reserve
fields:
  - [Insurer line, Display, "Yes", Insurer lines of the claim, "-"]
  - [Current reserve, Display, "-", "-", "-"]
  - [New reserve, Amount, "Yes", "-", ">= 0"]
  - [Reason, Text, "Yes", "-", Up to 500 characters]
notifications:
  - "None."
audit:
  - "Previous amount, new amount, reason, user and time."
acceptance:
  - A Team Lead amends the reserve; the history shows the previous and new amounts.
  - A Claims Officer cannot amend the reserve after recording.
  - A reserve change produces no journal entry.
```

```fr
id: FR-CL-033
title: Maintain catastrophe codes and tag claims
brd: [BRCLM.036 (p.16; p.31)]
actor: Unit Head (list); Claims user (tag)
priority: High (BRD p.21)
fit: CONFIGURE
screens: Broking Setup, Lists of values (BCL_CATASTROPHE); Record Claim; Claim record (Details tab)
api: To be assigned at build
description: The catastrophe codes of the BRD (Typhoon, Earthquake, Flood, Volcanic Eruption, Landslide, Fire, Others such as pandemic, El Nino or terrorism) are a list of values the Unit Head maintains. A claim can carry one code and a free-text event name (for example the typhoon's name). The claims-prone location report filters on the code (FR-CL-063).
preconditions:
  - "None."
main_flow:
  - The user selects the catastrophe code on the claim and, where useful, enters the event name.
  - BIBS saves the tag and shows the flag **CAT** on the summary card.
rules:
  - [R1, "Codes are list values with maker-checker.", Configurable, LOV BCL_CATASTROPHE]
validations:
  - [Event name without a code, Select the catastrophe code of the event, "-"]
fields_screen: Claim Details (catastrophe)
fields:
  - [Catastrophe code, List, "No", LOV BCL_CATASTROPHE, Effective value]
  - [Event name, Text, "No", "-", Up to 100 characters]
notifications:
  - "None."
audit:
  - "Tag changes recorded."
acceptance:
  - A claim tagged Typhoon with the event name shows the CAT flag.
  - The Unit Head adds a new code; it is selectable after authorisation.
```

## Status, settlement and closure

```fr
id: FR-CL-040
title: Maintain the claim status values
brd: [BRCLM.010 (p.13-14; p.29)]
actor: Unit Head (BCL_SETUP)
priority: High (BRD p.21)
fit: NEW
screens: Broking Setup, Lists of values (BCL_CLAIM_STATUS); Claims Setup (status attributes)
api: To be assigned at build
description:
  - The 18 claim statuses of the BRD (section 5.2) are values of a list maintained by the Unit Head, with maker-checker and effective dates. Each status carries attributes on the Claims Setup screen - the phase it belongs to (NEW, IN_PROGRESS, TEMP_CLOSED), the party the claim waits on, the default follow-up days and whether the claim awaits premium remittance.
  - The phase drives the system (ageing, closure, reports); the status is the BDOI label. A new status is usable once it has a phase.
preconditions:
  - "The user has BCL_SETUP."
main_flow:
  - The Unit Head adds or edits a status value.
  - The Unit Head sets its attributes on Claims Setup.
  - Another authoriser approves the value; it becomes selectable within the status matrix (FR-CL-041).
rules:
  - [R1, "Each status belongs to exactly one phase; CLOSED is reached only through a settlement type (FR-CL-045).", Fixed, "-"]
  - [R2, "Status values, waiting party and follow-up days are maintained by the business.", Configurable, "LOV BCL_CLAIM_STATUS, status attributes"]
  - [R3, "A status in use is end-dated, never deleted.", Fixed, "-"]
validations:
  - [Status without a phase, Set the phase of the status, To be assigned at build]
  - [Follow-up days not a whole number, Enter a whole number of days, "-"]
fields_screen: Status attributes
fields:
  - [Status, Display, "Yes", LOV BCL_CLAIM_STATUS, "-"]
  - [Phase, List, "Yes", NEW / IN_PROGRESS / TEMP_CLOSED, "-"]
  - [Waiting on, List, "Yes", Insurer / Claimant / Assured / Adjuster / BDOI, "-"]
  - [Follow-up days, Number, "No", "-", "1-365; blank = BCL_FOLLOW_UP_DAYS"]
  - [Awaiting premium remittance, Check box, "No", "-", "-"]
notifications:
  - "Pending values appear in the authoriser's My Approvals."
audit:
  - "Value and attribute changes with maker, checker and before / after values."
acceptance:
  - The status list offers the 18 BRD values on a new database.
  - A status added by the Unit Head is selectable only after authorisation and after its phase is set.
```

```fr
id: FR-CL-041
title: Restrict status selection by role and unit
brd: [BRCLM.012 (p.14; p.29), BRCLM.013 (p.14; p.29)]
actor: Unit Head (matrix); System (enforcement)
priority: High (BRD p.21)
fit: NEW
screens: Claims Setup (Status Access Matrix; Claims Handler Register); Claim record (Change Status)
api: To be assigned at build
description:
  - The status access matrix lists, for each status, the roles and units allowed to select it. The claims handler register records each handler's unit (Motor HO, Non-Motor HO or a branch) and team. When a user changes a status, the drop-down shows only the statuses allowed for one of the user's roles and the user's unit, and the service refuses any other status even when called directly.
  - The matrix is data maintained by the Unit Head with maker-checker. The delivered default gives TL and TH every status and officers the newly filed and temporary closure statuses (CLQ04).
preconditions:
  - "The user has BCL_SETUP (matrix) or BCL_STATUS_UPDATE (selection)."
main_flow:
  - The Unit Head maintains matrix rows (status, role, unit or any unit).
  - A handler opens **Change Status**; BIBS lists the allowed statuses.
  - The handler selects one; BIBS checks the matrix again on save.
rules:
  - [R1, "A status is selectable if a row matches one of the user's roles and the user's unit (or any unit).", Fixed, "-"]
  - [R2, "Matrix rows and the handler register are business data.", Configurable, "Status Access Matrix, Claims Handler Register"]
  - [R3, "Claims units: Motor HO, Non-Motor HO, branches Angeles, Cebu, CDO, Davao, GenSan (to confirm, CLQ04).", Configurable, LOV BCL_UNIT]
validations:
  - [Status not allowed for the user, "You are not allowed to set the status <status>", To be assigned at build]
  - [User not in the handler register, Your claims unit is not set. Contact the Unit Head, To be assigned at build]
fields_screen: Status Access Matrix row
fields:
  - [Status, List, "Yes", LOV BCL_CLAIM_STATUS, "-"]
  - [Role, List, "Yes", Claims roles, "-"]
  - [Unit, List, "No", LOV BCL_UNIT, Blank = any unit]
notifications:
  - "Pending matrix rows appear in My Approvals."
audit:
  - "Matrix and register changes with maker and checker."
acceptance:
  - A Claims Officer of Motor HO sees only the statuses the matrix gives the officer role.
  - A direct API call setting a status outside the matrix is refused.
  - A matrix row takes effect only after another user authorises it.
```

```fr
id: FR-CL-042
title: Change the claim status
brd: [BRCLM.011 (p.14; p.29), BRCLM.027 (p.16; p.31)]
actor: Team Lead, Team Head; Claims Officer within the matrix (BCL_STATUS_UPDATE)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (action Change Status; History tab)
api: To be assigned at build
description:
  - A permitted user changes the claim status with a remark. BIBS records the change in the status history (from, to, time, user, remark, days in the previous status), resets the age in the current status, recomputes the next follow-up date unless it was overridden, and moves the claim to the phase of the new status.
  - A status flagged "awaiting premium remittance" (status 11) lists the unremitted invoices of the cover with a link to the special remittance request (FR-CL-046).
preconditions:
  - The claim is not CLOSED.
  - The status is allowed by the matrix (FR-CL-041).
main_flow:
  - The handler clicks **Change Status**, selects the status and writes a remark.
  - BIBS checks the permission and the matrix and saves.
  - BIBS updates the phase, the status history, the age and the next follow-up date.
alternate_flows:
  - Status of phase TEMP_CLOSED. The claim becomes temporarily closed (FR-CL-045).
  - Closed claim. The action is not offered; the claim must be reopened first.
rules:
  - [R1, "Only BCL_STATUS_UPDATE holders change the status; the first status is set at recording.", Fixed, "-"]
  - [R2, "Each change writes one status history row.", Fixed, "-"]
  - [R3, "Allowed status sequences, if any, are confirmed under CLQ04; none are enforced by default.", Configurable, Change request after CLQ04]
validations:
  - [Status not selected, Select the new status, "-"]
  - [Change on a closed claim, "Claim <no.> is closed. Reopen it before changing the status", To be assigned at build]
fields_screen: Change Status
fields:
  - [New status, List, "Yes", Allowed statuses (matrix), "-"]
  - [Remark, Text, "No", "-", Up to 500 characters]
notifications:
  - "BCL_STATUS_CHANGED to the account officer in the app; e-mail to client, AO or insurer only for the events BDOI names (CLQ22)."
audit:
  - "Status history row and audit entry."
acceptance:
  - A status change resets "age this stage" to zero and keeps the age overall.
  - The status history shows the days spent in each earlier status.
  - A status update is saved within 1 minute (NFR 15.06).
```

```fr
id: FR-CL-043
title: Maintain the requested types of settlement
brd: [BRCLM.014 (p.14; p.30)]
actor: Unit Head (BCL_SETUP)
priority: High (BRD p.21)
fit: NEW
screens: Broking Setup, Lists of values (BCL_SETTLEMENT_TYPE); Claims Setup (settlement attributes)
api: To be assigned at build
description: The ten settlement types of the BRD (section 5.3) are values of a list maintained by the Unit Head. Each type carries its outcome (SETTLED or CLOSED_WITHOUT_PAYMENT), whether it closes the claim, and whether it needs a settlement amount and date. The BRD values are outcomes rather than requested types; the overview (p.23) and the stakeholder matrix (p.28) also name "Settlement Status" values without an ID (CLQ05).
preconditions:
  - "The user has BCL_SETUP."
main_flow:
  - The Unit Head adds or edits a settlement type and sets its attributes.
  - Another authoriser approves it; it becomes selectable.
rules:
  - [R1, "Types, outcome and flags are maintained by the business.", Configurable, "LOV BCL_SETTLEMENT_TYPE, settlement attributes"]
  - [R2, "Types with outcome SETTLED need a settlement amount and date.", Configurable, Attribute requires_settlement_amount]
validations:
  - [Outcome not set, Set the outcome of the settlement type, To be assigned at build]
fields_screen: Settlement attributes
fields:
  - [Settlement type, Display, "Yes", LOV BCL_SETTLEMENT_TYPE, "-"]
  - [Outcome, List, "Yes", SETTLED / CLOSED_WITHOUT_PAYMENT, "-"]
  - [Closes the claim, Check box, "Yes", "-", "-"]
  - [Requires settlement amount, Check box, "Yes", "-", "-"]
notifications:
  - "Pending values appear in My Approvals."
audit:
  - "Changes with maker, checker and before / after values."
acceptance:
  - The settlement type list offers the 10 BRD values with their outcome.
  - Changing "Settled - LOA Issued - Vehicle Under Repair" to not close the claim takes effect without a new build.
```

```fr
id: FR-CL-044
title: Set the requested type of settlement
brd: [BRCLM.015 (p.14; p.30), BRCLM.029 (p.16; p.31)]
actor: Team Lead, Team Head (BCL_SETTLEMENT_UPDATE)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Reserve & Settlement tab, action Set Settlement)
api: To be assigned at build
description:
  - A TL or TH sets or changes the requested type of settlement of the claim. For a settled outcome they enter the settlement amount (per insurer line where the insurers settle separately) and the date settled; these values feed the Settled Claims, Loss Experience and Loss Ratio reports.
  - When the type closes the claim, the action also needs BCL_CLOSE and closes the claim permanently (FR-CL-045).
preconditions:
  - "The claim is not CLOSED."
main_flow:
  - The TL clicks **Set Settlement** and selects the type.
  - For a settled outcome the TL enters the settlement amount and the date settled.
  - BIBS saves the settlement and, for a closing type, closes the claim.
alternate_flows:
  - Change of settlement type. Allowed while the claim is open; the earlier type stays in the history.
  - Several payments per claim or per insurer. Recorded per insurer line; partial payments are confirmed under CLQ09.
rules:
  - [R1, "Settlement amount and date are required for types with requires_settlement_amount.", Configurable, Settlement attributes]
  - [R2, "Settlement figures are the insurer's; BDOI posts nothing.", Fixed, "-"]
validations:
  - [Type not selected, Select the type of settlement, "-"]
  - [Amount or date missing for a settled type, Enter the settlement amount and the date settled, To be assigned at build]
  - [Date settled after today, The date settled cannot be in the future, "-"]
  - [Closing type without BCL_CLOSE, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: Set Settlement
fields:
  - [Requested type of settlement, List, "Yes", LOV BCL_SETTLEMENT_TYPE, Effective value]
  - [Settlement amount, Amount, Cond., "-", "Required when the type needs it; >= 0"]
  - [Settled amount per insurer, Amount, "No", Insurer lines, ">= 0"]
  - [Date settled, Date, Cond., "-", Required when the type needs it; not after today]
  - [Remark, Text, "No", "-", Up to 500 characters]
notifications:
  - "BCL_STATUS_CHANGED to the account officer when the claim closes."
audit:
  - "Type, amounts and date with user and time."
acceptance:
  - Setting "Settled" without an amount is refused.
  - A Team Lead sets "Settled - Signed Release Papers Returned" with amount and date; the claim appears in the Settled Claims report.
  - A Claims Officer does not see the Set Settlement action.
```

```fr
id: FR-CL-045
title: Close a claim temporarily or permanently and reopen it
brd: [BRCLM.005 (p.13; p.29), BRCLM.035 (p.16; p.31)]
actor: Claims Officer (temporary closure); Team Lead, Team Head (permanent closure); Team Head, Unit Head (reopen)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Change Status, Set Settlement, Reopen); Claims worklist (tabs Temporarily Closed, Closed)
api: To be assigned at build
description:
  - "**Temporary closure** uses the statuses of phase TEMP_CLOSED: 17 \"Temporary Closed Claim - Non-submission of Documents\" and 18 \"Temporary Closed Claim - with Offer\". The claim stays outstanding and ageing continues; any permitted user resumes it by setting an in-progress status."
  - "**Permanent closure** happens only through a settlement type that closes the claim, by a TL or TH with BCL_CLOSE. The claim becomes CLOSED with the closure date; afterwards it accepts only diary entries, insurer updates and reopen."
  - "**Reopen** of a permanently closed claim needs BCL_REOPEN and a reason. The settlement type is kept in the history and cleared on the claim."
preconditions:
  - "The claim is open (closure) or CLOSED (reopen)."
main_flow:
  - The officer sets status 17 or 18; BIBS marks the claim temporarily closed.
  - The TL sets a closing settlement type (FR-CL-044); BIBS closes the claim permanently.
  - To reopen, the TH clicks **Reopen**, selects the reason and confirms; BIBS returns the claim to IN_PROGRESS.
rules:
  - [R1, "Officers tag temporary closures; TL / TH close permanently; TH / UH reopen (to confirm, CLQ06).", Configurable, Roles and status matrix]
  - [R2, "A permanent closure needs the settlement amount and date required by the type.", Fixed, "-"]
  - [R3, "Temporarily closed claims stay in the outstanding reports.", Configurable, Change request after CLQ06]
validations:
  - [Permanent closure without BCL_CLOSE, You are not permitted to perform this action, ACCESS_DENIED]
  - [Reopen without reason, "Select a reason for 'reopen'", WORKFLOW_REASON_REQUIRED]
  - [Change on a closed claim, "Claim <no.> is closed. Reopen it before changing it", To be assigned at build]
fields_screen: Reopen Claim
fields:
  - [Reason, List, "Yes", LOV BCL_REOPEN_REASON, "-"]
  - [Remark, Text, "No", "-", Up to 500 characters]
notifications:
  - "The handler is notified when a claim is reopened."
audit:
  - "Closure kind, closure date, reopen reason, user and time."
acceptance:
  - A claim in status 18 appears in the Temporarily Closed tab and in the Outstanding Claims report.
  - A claim closed with "Closed - Denied" appears in the Closed tab and no longer ages.
  - A Team Lead cannot reopen a closed claim; a Team Head reopens it with a reason.
```

```fr
id: FR-CL-046
title: Request the claims special remittance for claims awaiting premium remittance
brd: [BRCLM.010 (p.13-14; p.29), BRCLM.001 (p.13; p.29)]
actor: Claims Officer; Remittance (Operations)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (flag "Awaiting premium remittance", link Request Special Remittance); Special Remittance request (BRD-2)
api: Port ClaimsFeed, feed CLAIMS_SPECIAL_REMIT (to be implemented at build)
description:
  - Status 11 "With BDOI - For Premium Remittance" means the insurer waits for BDOI to remit the premium before it settles. BIBS lists the invoices of the cover that are not fully remitted, and links to the special remittance request of Operations with the invoice and the claims condition pre-filled.
  - Claims publishes the claims of that status to the Operations feed CLAIMS_SPECIAL_REMIT. The special remittance screen uses the feed to confirm the claims condition in BIBS, instead of a manual note (OQ46). When the premium is fully remitted, the handler is notified.
preconditions:
  - The claim is in a status flagged "awaiting premium remittance".
main_flow:
  - The handler sets status 11 (FR-CL-042).
  - BIBS shows the flag and the unremitted invoices.
  - The handler clicks **Request Special Remittance**; the Operations screen opens with the invoice and condition CLAIMS.
  - Remittance confirms the claims condition from the feed and processes the remittance.
  - When the invoice is fully remitted, BIBS notifies the handler.
alternate_flows:
  - Claims condition without an eligible claim. Operations refuses the claims special remittance for that invoice (behaviour change agreed with the Remittance owner, R5 section 12.2).
rules:
  - [R1, "Feed item = one not-fully-remitted invoice of each open claim in a status flagged awaiting premium remittance.", Fixed, "-"]
  - [R2, "Approvers and SLA of the special remittance stay with Operations (OQ25).", Configurable, Operations settings]
validations: []
notifications:
  - "BCL_PREMIUM_REMITTED to the handler when the invoice becomes fully remitted."
audit:
  - "Feed reads are logged by Operations; the claim history shows the status."
acceptance:
  - A claim set to status 11 appears in the CLAIMS_SPECIAL_REMIT feed with its unremitted invoice.
  - A claims special remittance for an invoice without such a claim is refused by Operations.
  - The handler is notified when the invoice is fully remitted.
```

## Follow-up, diary and ageing

```fr
id: FR-CL-050
title: Compute and override the next follow-up date
brd: [BRCLM.019 (p.15; p.30)]
actor: System; Team Lead, Team Head (BCL_FOLLOW_UP_OVERRIDE)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (summary card; action Override Follow-up Date); Claims worklist (Follow-ups Due)
api: To be assigned at build
description: BIBS sets the next follow-up date at recording and at each status change - the status's follow-up days, else the default parameter, from the change date. A TL or TH may override the date with a reason; the claim shows "Follow-up overridden" and later status changes keep the overridden date until it passes.
preconditions:
  - "The claim is open."
main_flow:
  - BIBS computes the date on each status change.
  - The TL clicks **Override Follow-up Date**, enters the date and the reason.
  - BIBS saves it and sets the flag.
rules:
  - [R1, "Default follow-up = 7 days, or the follow-up days of the status.", Configurable, "Parameter BCL_FOLLOW_UP_DAYS, status attributes"]
  - [R2, "An override is kept until its date passes or the TL changes it.", Fixed, "-"]
validations:
  - [Date in the past, The follow-up date cannot be before today, "-"]
  - [Reason blank, Enter the reason for the change, "-"]
fields_screen: Override Follow-up Date
fields:
  - [Next follow-up date, Date, "Yes", "-", Today or later]
  - [Reason, List, "Yes", LOV BCL_OVERRIDE_REASON, "-"]
notifications:
  - "BCL_FOLLOW_UP_DUE to the handler on the follow-up date (FR-CL-054)."
audit:
  - "Old and new date, reason, user and time."
acceptance:
  - A status with 5 follow-up days set on 1 March gives a next follow-up date of 6 March.
  - A Team Lead overrides the date with a reason; a Claims Officer cannot.
```

```fr
id: FR-CL-051
title: Encode the next action plan summary
brd: [BRCLM.020 (p.15; p.30), BRCLM.021 (p.15; p.30)]
actor: Claims Officer, TL, TH (BCL_ACTION_PLAN)
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Details tab, Next Action Plan); Claims worklist; Outstanding reports
api: To be assigned at build
description: Each claim has a Next Action Plan Summary field of up to 2,000 characters. Permitted users update it; every version is kept in the claim history. The current text is shown in the worklist and printed as "Follow Ups / Remarks" in the outstanding reports (p.42).
preconditions:
  - "The claim is open."
main_flow:
  - The officer edits the Next Action Plan Summary and saves.
  - BIBS keeps the previous text in the history.
rules:
  - [R1, "Every saved version is kept.", Fixed, "-"]
validations:
  - [Text longer than 2000 characters, The action plan can have up to 2000 characters, "-"]
fields_screen: Next Action Plan
fields:
  - [Next action plan summary, Long text, "No", "-", Up to 2000 characters]
notifications:
  - "None."
audit:
  - "Each version with user and time."
acceptance:
  - The current action plan appears in the Follow Ups / Remarks column of the Outstanding Claims report.
  - The history shows the earlier versions of the text.
```

```fr
id: FR-CL-052
title: Keep a diary per claim and per handler
brd: [BRCLM.022 (p.15; p.30), NFR 15.08 (p.39)]
actor: Claims handlers
priority: High (BRD p.21)
fit: NEW
screens: Claim record (Diary tab); My Diary
api: To be assigned at build
description: Handlers log and plan activities on a claim. A diary entry has a type (call, e-mail, meeting, note, follow-up), date, optional due date and assignee, text and attachments, and is marked done when completed. My Diary lists the user's open entries across claims by due date. Due entries are notified each morning (FR-CL-054).
preconditions:
  - "The user has BCL_RECORD."
main_flow:
  - The handler adds a diary entry on the claim.
  - The handler sets a due date and assignee for a planned activity.
  - The assignee marks the entry done when completed.
alternate_flows:
  - Entry on a closed claim. Allowed; closed claims accept diary entries.
rules:
  - [R1, "Diary types: call, e-mail, meeting, note, follow-up.", Configurable, LOV BCL_DIARY_TYPE]
  - [R2, "Entries are not deleted; a wrong entry is marked done with a remark.", Fixed, "-"]
validations:
  - [Text blank, Enter the diary text, "-"]
  - [Due date before the entry date, The due date must be on or after the entry date, "-"]
fields_screen: Diary entry
fields:
  - [Type, List, "Yes", LOV BCL_DIARY_TYPE, "-"]
  - [Date, Date, "Yes", "-", Default today]
  - [Due date, Date, "No", "-", On or after the date]
  - [Assignee, List, "No", Claims handlers, Default the user]
  - [Text, Long text, "Yes", "-", Up to 2000 characters]
notifications:
  - "The assignee is notified when an entry is assigned to another user."
audit:
  - "Entries and completion with user and time."
acceptance:
  - A diary entry assigned to another handler appears in that handler's My Diary.
  - A diary entry can be added to a closed claim.
```

```fr
id: FR-CL-053
title: Compute the age of claims overall and per status
brd: [BRCLM.025 (p.16; p.30), BRCLM.027 (p.16; p.31)]
actor: System
priority: High (BRD p.21)
fit: NEW
screens: Claim record (summary card); Claims worklist; ageing reports
api: To be assigned at build
description:
  - "Age overall is the number of calendar days from the reported date to today, or to the closure date for a closed claim (BRCLM.025)."
  - "Age this stage is the number of days since the current status was set. The status history gives the days spent in every earlier status (BRCLM.027)."
  - Ages are computed when read, so they are always current, and shown on the claim, in the worklist and in the reports.
preconditions:
  - "None."
main_flow:
  - The user opens a claim or runs a report.
  - BIBS computes age overall and age this stage as of today (or the report's as-of date).
rules:
  - [R1, "Calendar days; the reported date is day zero.", Fixed, "-"]
  - [R2, "Temporarily closed claims keep ageing (to confirm, CLQ06).", Configurable, Change request after CLQ06]
  - [R3, "Ageing buckets 0-30, 31-60, 61-90, 91-180, 181+ days (Report List).", Configurable, Parameter BCL_AGEING_BUCKETS]
validations: []
notifications:
  - "None."
audit:
  - "None (computed values)."
acceptance:
  - A claim reported on 1 March and open on 31 March has age overall 30.
  - A claim closed on 15 March keeps age overall 14 after closure.
  - A claim that spent 5 days in status 3 and is 2 days in status 8 shows age this stage 2 and history 5 days for status 3.
```

```fr
id: FR-CL-054
title: Track pending actions and remind handlers
brd: [BRCLM.034 (p.16; p.31), BRCLM.019 (p.15; p.30), BRCLM.022 (p.15; p.30)]
actor: System; Claims handlers
priority: High (BRD p.21)
fit: NEW
screens: Claims worklist (tab Follow-ups Due); Claims home; Pending Actions report
api: To be assigned at build
description: A pending action is a claim whose next follow-up date is reached or a diary entry that is due and not done. The daily job notifies each handler of the claims and entries due today and raises an alert for past dates. Pending actions are listed in the Follow-ups Due tab and in the Pending Actions report per handler (FR-CL-061).
preconditions:
  - "None."
main_flow:
  - The job BCL_FOLLOW_UP_DUE runs at 06:00 Manila time.
  - BIBS notifies each handler of the follow-ups and diary entries due today.
  - BIBS raises BCL_FOLLOW_UP_OVERDUE for past-due items.
  - The handler opens Follow-ups Due and works the claims.
rules:
  - [R1, "Job time 06:00 Manila.", Configurable, Job schedule bcl-follow-up-due-cron]
  - [R2, "One overdue alert per claim until it is cleared.", Fixed, "-"]
validations: []
notifications:
  - "BCL_FOLLOW_UP_DUE to the handler; BCL_FOLLOW_UP_OVERDUE alert for past dates."
audit:
  - "Job runs and alerts are logged."
acceptance:
  - A claim with next follow-up date today appears in Follow-ups Due and the handler is notified at 06:00.
  - A claim two days past its follow-up date raises one overdue alert.
```

```fr
id: FR-CL-055
title: Claims home and worklist
brd: [NFR 15.03 (p.39), BRCLM.034 (p.16; p.31), BRCLM.043 (p.7)]
actor: Claims users (BCL_VIEW); TL, TH, UH (reassign)
priority: Must have
fit: NEW
screens: Claims Home; Claims Worklist
api: To be assigned at build
description:
  - Claims Home shows the user's open claims, follow-ups due today and overdue, claims by status and phase, ageing buckets, claims on unpaid covers and claims awaiting premium remittance. Each tile opens its filtered list.
  - The worklist has the tabs My Claims, Open, Temporarily Closed, Closed and Follow-ups Due, with search by claim number, insurer claim number, ARN, policy number and assured. TL, TH and UH reassign claims between handlers, one by one or in bulk.
preconditions:
  - The user has BCL_VIEW.
main_flow:
  - The user opens Claims Home and clicks a tile.
  - The worklist opens filtered; the user searches or sorts.
  - The TL selects claims and clicks **Reassign**, choosing the new handler.
rules:
  - [R1, "Reassignment needs WORK_ASSIGN.", Fixed, "-"]
validations:
  - [Reassign without a handler, Select the new handler, "-"]
notifications:
  - "BCL_CLAIM_ASSIGNED to the new handler."
audit:
  - "Reassignment with old and new handler, user and time."
acceptance:
  - The Claims Home loads within 5 seconds (NFR 15.03).
  - A search by an insurer claim number returns the BDOI claim.
  - A Team Lead reassigns ten claims in one action; the new handler is notified.
```

## Reports and analytics

```fr
id: FR-CL-060
title: Claims ageing reports
brd: [BRCLM.026 (p.16; p.30), BRCLM.028 (p.16; p.31), "Report list (p.43)"]
actor: Claims users, Unit Head (BCL_REPORT_VIEW)
priority: High (BRD p.21)
fit: NEW
screens: Reports (category Claims Handling)
api: Report service /api/v1/reports; codes BCL-AGEING, BCL-AGEING-STATUS
description: The Claims Aging Report lists outstanding claims with the age overall from the reported date and the ageing bucket, with totals per bucket and insurer (BCL-AGEING). The Claims Aging Report per Status groups the outstanding claims by status, then insurer, with age this stage and age overall (BCL-AGEING-STATUS). Both carry the columns of the outstanding claims list (section 6.1).
preconditions:
  - The user has BCL_REPORT_VIEW.
main_flow:
  - The user selects the report and sets the parameters (as-of date, branch, unit, handler, insurer, product line, Marketing team, AO).
  - BIBS runs the report and shows the result.
  - A user with BCL_REPORT_EXPORT downloads it in Excel, PDF or CSV.
rules:
  - [R1, "Buckets 0-30, 31-60, 61-90, 91-180, 181+ days (default).", Configurable, Parameter BCL_AGEING_BUCKETS]
  - [R2, "File name <Report>_<date of extraction> (Report List).", Fixed, "-"]
validations:
  - [As-of date in the future, The as-of date cannot be in the future, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each run and export with user, parameters and time."
acceptance:
  - A claim 45 days old appears in bucket 31-60 of BCL-AGEING.
  - BCL-AGEING-STATUS groups claims by status and shows age this stage.
  - A report is generated within 5 minutes (NFR 15.09-15.13).
```

```fr
id: FR-CL-061
title: Outstanding, past due, settled and pending actions reports
brd: [BRCLM.029 (p.16; p.31), BRCLM.031 (p.16; p.31), BRCLM.034 (p.16; p.31), "Report list (p.42-43)"]
actor: Claims users, Unit Head (BCL_REPORT_VIEW)
priority: High (BRD p.21)
fit: NEW
screens: Reports (category Claims Handling)
api: Report service /api/v1/reports; codes BCL-OUTSTANDING, BCL-OUTSTANDING-PAST-DUE, BCL-SETTLED, BCL-PENDING-ACTIONS
description:
  - The List of all Outstanding Claims covers every claim in phase NEW, IN_PROGRESS or TEMP_CLOSED as of the date. The 90 Days Past Due variant keeps the claims whose age overall exceeds the threshold.
  - The List of all Settled Claims covers claims permanently closed with a settled outcome in the date range, with date settled and settlement amount.
  - The Pending Actions report lists the claims with follow-up due or overdue and the open diary entries due, per handler.
  - Columns are those of the BRD report list (section 6.1). Insurer claim numbers are added to every list.
preconditions:
  - The user has BCL_REPORT_VIEW.
main_flow:
  - The user selects the report and sets the parameters.
  - BIBS runs it and shows the result; export as FR-CL-060.
rules:
  - [R1, "Past due threshold 90 days (default).", Configurable, Parameter BCL_PAST_DUE_DAYS]
  - [R2, "Daily job BCL_AGEING_ALERTS raises BCL_CLAIM_PAST_DUE for outstanding claims older than the threshold.", Configurable, Job schedule bcl-ageing-alerts-cron]
  - [R3, "Definitions of Claim Amount, Settlement Amount and Date Settled are confirmed under CLQ09.", Configurable, Report definition]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "BCL_CLAIM_PAST_DUE alert to the handler when a claim passes the threshold."
audit:
  - "Each run and export with user, parameters and time."
acceptance:
  - A temporarily closed claim appears in the Outstanding Claims list.
  - A claim open for 95 days appears in the 90 Days Past Due variant; a claim open for 80 days does not.
  - A claim closed "Closed - Denied" does not appear in the Settled Claims list.
```

```fr
id: FR-CL-062
title: Loss experience and loss ratio reports
brd: [BRCLM.030 (p.16; p.31), BRCLM.032 (p.16; p.31), "Report list (p.43-44)"]
actor: Claims users, Unit Head, Marketing (BCL_REPORT_VIEW)
priority: High (BRD p.21)
fit: NEW
screens: Reports (category Claims Handling); account page (Claims tab, loss summary)
api: Report service /api/v1/reports; codes BCL-LOSS-EXPERIENCE, BCL-LOSS-RATIO
description:
  - "The Loss Experience report lists, per claim and insurer line: insured, claimant, policy number, date of loss, nature and type of loss, deductible, amount of loss paid, outstanding and total, insurer and status, grouped by client, account and policy year. Paid = settled amount; outstanding = insurer reserve less paid while the claim is open, never below zero."
  - "The Loss Ratio report divides the losses (paid plus outstanding) by the premium of the same cover and policy year and multiplies by 100, grouped by client, product line or insurer. The premium is the signed gross premium of the ledger invoices of that policy year (originals, endorsements and cancellations)."
  - The same loss figures are served to Renewal (decision D4) and to the account page through one read service.
preconditions:
  - The user has BCL_REPORT_VIEW.
main_flow:
  - The user selects the report, the grouping and the period.
  - BIBS computes the figures and shows the result; export as FR-CL-060.
rules:
  - [R1, "O/S = max(reserve - paid, 0) while open (to confirm, CLQ08).", Configurable, Report definition]
  - [R2, "Premium basis, period basis and grouping are confirmed under CLQ20; default = signed gross premium per policy year.", Configurable, Report definition]
validations:
  - [Grouping not selected, Select the grouping, "-"]
notifications:
  - "None."
audit:
  - "Each run and export with user, parameters and time."
acceptance:
  - A claim with reserve 100,000 and paid 60,000 shows O/S 40,000 and total 100,000.
  - A cover with premium 50,000 and losses 25,000 shows a loss ratio of 50.00%.
  - Renewal shows the same claim count and totals for the account as the Loss Experience report.
```

```fr
id: FR-CL-063
title: Identify claims-prone locations
brd: [BRCLM.038 (p.5)]
actor: Claims / Risk user (BCL_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Reports (Claims by Location / Claims-prone Locations)
api: Report service /api/v1/reports; code BCL-PRONE-LOCATIONS
description: The report shows, per location key, city and province, the number of claims, paid and outstanding amounts over a period, and flags a location "claims-prone" when it has at least the threshold number of claims in the look-back years. The user filters by catastrophe code, product line and period, and drills down to the claims of a location. It builds on the claims and loss data of FR-CL-062 (AC3).
preconditions:
  - The user has BCL_REPORT_VIEW.
main_flow:
  - The user runs the report for a period and optional catastrophe code.
  - BIBS lists locations with counts, amounts and the claims-prone flag.
  - The user opens a location to see its claims.
rules:
  - [R1, "Claims-prone = at least 3 claims in 3 years (default).", Configurable, "Parameters BCL_PRONE_MIN_CLAIMS, BCL_PRONE_YEARS"]
  - [R2, "Granularity is the location key, city and province; barangay, region or hazard data wait for CLQ16.", Fixed, "-"]
validations:
  - [Period end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each run and export logged."
acceptance:
  - A location with 3 claims in the last 3 years is flagged claims-prone.
  - Filtering by Flood lists only the claims tagged Flood.
  - The drill-down lists the claims of the location with their numbers.
```

```fr
id: FR-CL-064
title: Give authorised users the data for their own analysis
brd: [BRCLM.033 (p.16; p.31)]
actor: Unit Head, Claims / Risk user (BCL_DATA_EXTRACT)
priority: High (BRD p.21)
fit: CHANGE
screens: Reports (Claims Data Extract); saved report variants
api: Report service /api/v1/reports; code BCL-DATA-EXTRACT
description: The Claims Data Extract gives one row per claim, insurer line and location with every claim field, for analysis outside BIBS. Access needs BCL_DATA_EXTRACT, which the Unit Head requests for the users who need it. Users save their parameter sets as report variants for every Claims report.
preconditions:
  - The user has BCL_DATA_EXTRACT.
main_flow:
  - The user runs the extract with a date range and filters.
  - The user downloads it in Excel or CSV.
  - The user saves the parameters as a variant.
rules:
  - [R1, "A dynamic report builder is not provided; saved variants and the extract cover the need (Q40).", Fixed, "-"]
validations:
  - [Date range end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each extract run and download with user and parameters."
acceptance:
  - A claim with two insurers and three locations gives six rows in the extract.
  - A Claims Officer without BCL_DATA_EXTRACT cannot run the extract.
```

```fr
id: FR-CL-065
title: Give Marketing controlled access to loss information
brd: [BRCLM.040 (p.6)]
actor: Marketing AO, Marketing TL
priority: Must have
fit: CHANGE
screens: Reports (Claims Handling); account page (Claims tab, loss summary)
api: Report service /api/v1/reports; loss summary to be assigned at build
description: Marketing users view loss experience through the existing claims reports, for renewal, pricing and client discussions. They receive BCL_REPORT_VIEW; export is granted per role (proposal - Marketing TL only). The account page shows a loss summary of the account (claim count, open claims, paid, outstanding). Marketing never receives a claim maintenance permission.
preconditions:
  - "The user has BCL_REPORT_VIEW."
main_flow:
  - The Marketing user opens the Loss Experience report for a client.
  - BIBS shows the loss figures.
  - A Marketing TL downloads the report.
rules:
  - [R1, "View for MKT_AO and MKT_TL; export for MKT_TL (to confirm, CLQ15).", Configurable, User Access Maintenance request]
  - [R2, "Whether Marketing sees all accounts or only its own portfolio is confirmed under CLQ15; all accounts by default.", Configurable, Change request after CLQ15]
validations:
  - [Export without BCL_REPORT_EXPORT, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - "Report runs and exports by Marketing users are logged."
acceptance:
  - A Marketing AO views the Loss Experience report and cannot download it.
  - A Marketing TL downloads the report in Excel.
  - A Marketing AO cannot open a claim record.
```

```fr
id: FR-CL-066
title: Insurer claim number register and claims activity log
brd: [BRCLM.043 (p.7), BRCLM.041 (p.6), BRCLM.042 (p.6), NFR 15.08 (p.39)]
actor: Claims users, Unit Head (BCL_REPORT_VIEW)
priority: Must have
fit: NEW
screens: Reports (category Claims Handling)
api: Report service /api/v1/reports; codes BCL-INSURER-CLAIMS, BCL-ACTIVITY-LOG
description: The Insurer Claim Numbers report lists one row per insurer claim number with the BDOI claim, insurer, share, reserve and settled amount (BRCLM.043 AC5). The Claims Activity Log lists status changes, field changes, insurer updates, location reference changes and diary entries with user and time, so insurer communications and mappings are auditable (BRCLM.041 AC3, 042 AC3).
preconditions:
  - The user has BCL_REPORT_VIEW.
main_flow:
  - The user selects the report and the period, insurer or handler.
  - BIBS shows the result; export as FR-CL-060.
rules:
  - [R1, "Both reports read the history tables; nothing can be edited from them.", Fixed, "-"]
validations:
  - [Period end before start, The end date must be on or after the start date, INVALID_REPORT_PARAMETERS]
notifications:
  - "None."
audit:
  - "Each run and export logged."
acceptance:
  - The Insurer Claim Numbers report lists both numbers of a two-insurer claim under the same BDOI claim number.
  - The Claims Activity Log lists a location reference change with the old and new reference.
```

# Workflow and status model

## Phases

Figure 2 shows the phases of a claim. The 18 BDOI statuses are values inside a phase (section 5.2); the phase decides what the system does. The workflow BCL_CLAIM mirrors the phase: it gives the handler's My Work queue, assignment and case history. Its transitions are called only by the Claims services after their own checks.

![Claim phases and workflow BCL_CLAIM](figures/brd07_phases.dot){width=10}

<!-- table: widths=3,6.6,4.4,2.6 caption="Phases of a claim" -->
| Phase | Meaning | Reached by | Ages |
|---|---|---|---|
| NEW | Newly filed; with or without complete documents | Recording (statuses 1-2) | Yes |
| IN_PROGRESS | Being handled; waits on the insurer, adjuster, claimant, assured or BDOI | Status 3-16 | Yes |
| TEMP_CLOSED | Temporarily closed; can resume | Status 17-18 | Yes (CLQ06) |
| CLOSED | Permanently closed, settled or closed without payment | Closing settlement type (BCL_CLOSE) | No |

<!-- table: widths=3.4,2.2,3,4.6,3.4 caption="Transitions of BCL_CLAIM" size=8 -->
| From | Action | To | Permission | Reason list |
|---|---|---|---|---|
| NEW | progress | IN_PROGRESS | BCL_STATUS_UPDATE (matrix) | - |
| NEW, IN_PROGRESS | temp_close | TEMP_CLOSED | BCL_STATUS_UPDATE (matrix) | - |
| TEMP_CLOSED | resume | IN_PROGRESS | BCL_STATUS_UPDATE (matrix) | - |
| NEW, IN_PROGRESS, TEMP_CLOSED | close | CLOSED | BCL_SETTLEMENT_UPDATE + BCL_CLOSE | - |
| CLOSED | reopen | IN_PROGRESS | BCL_REOPEN | BCL_REOPEN_REASON |

## Claim statuses

<!-- table: widths=0.8,8.8,3,4 caption="Claim status values (BRCLM.010, p.13-14) and proposed attributes" size=8.5 -->
| # | Status | Phase | Waiting on |
|---|---|---|---|
| 1 | Newly Filed Claim - with complete documents | NEW | Insurer |
| 2 | Newly Filed Claim - without or incomplete documents | NEW | Claimant |
| 3 | For Adjuster's Review and Evaluation | IN_PROGRESS | Adjuster |
| 4 | For Claimant's Acceptance of Offer | IN_PROGRESS | Claimant |
| 5 | For Claimant's Submission of Documents | IN_PROGRESS | Claimant |
| 6 | For Insurer's Issuance of Check | IN_PROGRESS | Insurer |
| 7 | For Insurer's Returning of Release Papers | IN_PROGRESS | Insurer |
| 8 | For Insurer's Review and Evaluation | IN_PROGRESS | Insurer |
| 9 | With Adjuster - For Issuance of Settlement Offer | IN_PROGRESS | Adjuster |
| 10 | With Assured - For Schedule of Meeting | IN_PROGRESS | Assured |
| 11 | With BDOI - For Premium Remittance | IN_PROGRESS | BDOI (awaiting premium remittance) |
| 12 | With BDOI - For Transmittal of Settlement Check | IN_PROGRESS | BDOI |
| 13 | With BDOI - Under Review / Discussion | IN_PROGRESS | BDOI |
| 14 | With Claimant - For Pull-out of Salvaged Items | IN_PROGRESS | Claimant |
| 15 | With Claimant - For Submission of CNR | IN_PROGRESS | Claimant |
| 16 | With Insurer - For Issuance of LOA | IN_PROGRESS | Insurer |
| 17 | Temporary Closed Claim - Non-submission of Documents | TEMP_CLOSED | Claimant |
| 18 | Temporary Closed Claim - with Offer | TEMP_CLOSED | Claimant |

The phase and the waiting party are the project's proposal; BDOI confirms them with the status matrix (CLQ04). "CNR" is not defined in the BRD (CLQ12). Whether "complete documents" is decided by a document checklist per claim type is open (CLQ26).

## Settlement types

<!-- table: widths=8.2,4.6,3.8 caption="Requested type of settlement (BRCLM.014, p.14) and proposed attributes" size=8.5 -->
| Settlement type | Outcome | Closes the claim |
|---|---|---|
| Closed - Cancelled | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - Denied | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - within Deductible | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - without Payment | CLOSED_WITHOUT_PAYMENT | Yes |
| Settled | SETTLED | Yes |
| Settled - Directly Filed | SETTLED | Yes |
| Settled - LOA Issued | SETTLED | Yes (repair monitored offline, p.24) |
| Settled - LOA Issued - For Schedule of Repair | SETTLED | To confirm (CLQ05) |
| Settled - LOA Issued - Vehicle Under Repair | SETTLED | To confirm (CLQ05) |
| Settled - Signed Release Papers Returned | SETTLED | Yes |

## Service levels and escalation

- The BRD sets response times, not handling SLAs: claims booking and premium validation 5 seconds, status and settlement updates 1 minute, activity log and reports 5 minutes (p.39-40).
- The NEW phase has a stage SLA of 24 hours in the workflow (default), so that newly filed claims are picked up. Other phases have no SLA; the follow-up date and the 90-day past due alert (FR-CL-054, FR-CL-061) drive the monitoring.
- Escalation beyond the handler (to the TL or UH) is not in the BRD and is not designed. Supervisors see overdue follow-ups on Claims Home.

# Reports and documents

## Reports

<!-- table: widths=4.2,4.8,5.4,2.2 caption="Claims Handling reports (category Claims Handling)" size=8.5 -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| BCL-OUTSTANDING | List of all Outstanding Claims | Open and temporarily closed claims as of a date | BRCLM.031 |
| BCL-OUTSTANDING-PAST-DUE | Outstanding Claims 90 Days Past Due | Outstanding claims older than the threshold | BRCLM.031 |
| BCL-SETTLED | List of all Settled Claims | Claims settled in a date range | BRCLM.029 |
| BCL-AGEING | Claims Aging Report | Age overall and bucket of outstanding claims | BRCLM.025, 026 |
| BCL-AGEING-STATUS | Claims Aging Report per Status | Age per status of outstanding claims | BRCLM.027, 028 |
| BCL-LOSS-EXPERIENCE | Loss Experience | Paid, outstanding and total loss per claim | BRCLM.030 |
| BCL-LOSS-RATIO | Loss Ratio | Losses over premium per cover and year | BRCLM.032 |
| BCL-PENDING-ACTIONS | Pending Actions | Follow-ups and diary entries due, per handler | BRCLM.034 |
| BCL-PRONE-LOCATIONS | Claims by Location / Claims-prone Locations | Claim counts and amounts per location | BRCLM.038 |
| BCL-INSURER-CLAIMS | Insurer Claim Numbers | Every insurer claim number with its BDOI claim | BRCLM.043 |
| BCL-ACTIVITY-LOG | Claims Activity Log | Status, field, insurer and location changes | BRCLM.041, 042 |
| BCL-DATA-EXTRACT | Claims Data Extract | One row per claim, insurer line and location | BRCLM.033 |

All reports need BCL_REPORT_VIEW to run and BCL_REPORT_EXPORT to download (the extract needs BCL_DATA_EXTRACT). They export to XLSX (the BRD format), PDF and CSV, support saved variants, and name their files `<Report>_<date of extraction>`. Common parameters: branch, unit, handler, insurer, product line, Marketing team, AO, loss or reported date range and as-of date.

### Outstanding claims lists (BCL-OUTSTANDING, BCL-OUTSTANDING-PAST-DUE, BCL-AGEING, BCL-AGEING-STATUS)

Layout: landscape, one row per claim, sorted by age overall (descending); the ageing reports add the bucket or group by status. Columns from the BRD report list (p.42-43), plus the insurer claim numbers.

<!-- table: widths=3.8,2.6,10.2 caption="Columns of the outstanding claims lists" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Claim Number | Text | BCL-yyyy-nnnnnn |
| Name of Claimant | Text | Claimant (default the assured) |
| Assured's Name | Text | Assured of the cover |
| Nature / Type of Loss / Accident | Text | Nature of loss and claim type |
| Date of Loss | Date | Loss date |
| Date Reported | Date | Reported date |
| Claim Amount | Amount | Claim amount |
| Deductible | Amount | Deductible |
| Claim Status | Text | Current BDOI status |
| Insurance Company | Text | Insurers of the claim, with their claim numbers |
| Claim Type | Text | Claim type |
| Follow Ups / Remarks | Text | Next action plan summary |
| Age this Stage | Number | Days in the current status |
| Age Overall | Number | Days since the reported date |
| Next Follow Up | Date | Next follow-up date |
| Claim Handler | Text | Handler |
| Marketing Team | Text | Marketing team of the policy |
| Account Officer | Text | AO of the policy |

### Settled claims list (BCL-SETTLED)

Parameters: date settled from / to. Layout: landscape, sorted by date settled. Columns as the outstanding list without Claim Type, Follow Ups / Remarks, Age this Stage and Next Follow Up, and with **Date Settled** (date) and **Settlement Amount** (amount) after Deductible (p.42).

### Loss experience and loss ratio (BCL-LOSS-EXPERIENCE, BCL-LOSS-RATIO)

<!-- table: widths=3.8,2.6,10.2 caption="Columns of the loss reports (p.43-44)" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Insured | Text | Assured of the cover |
| Claimant | Text | Claimant |
| Policy No. | Text | Policy number of the policy year |
| Date of Loss | Date | Loss date |
| Nature of Loss / Type of Loss | Text | Nature of loss and claim type |
| Deductible | Amount | Deductible |
| Amount of Loss - Paid | Amount | Settled amount |
| Amount of Loss - O/S | Amount | Reserve less paid while open, not below zero |
| Amount of Loss - Total | Amount | Paid plus O/S |
| Insurer Name | Text | Insurer of the line |
| Status | Text | Current BDOI status |
| Premiums (loss ratio only) | Amount | Signed gross premium of the cover and policy year |
| Loss Ratio % (loss ratio only) | Percent | Total loss / premium x 100 |

## Documents

<!-- table: widths=3.6,3.4,9.6 caption="Claims documents" size=8.5 -->
| Template / type | Output | Content |
|---|---|---|
| BCL_LOSS_ADVICE | PDF by e-mail, stored as CLAIM_REPORT | Assured; policy or reference number; date and location of loss; nature of loss and description; initial loss reserve; assigned adjuster; insurer claim numbers; BDOI claim number and handler (p.24-25). Draft layout until BDOI provides its own (CLQ22) |
| CLAIM_REPORT | Attachment type | Claims reports linked to the claim, the account (ARN) and the client, so the contact centre retrieves them (decision D3; BRCSF-009). Which claim documents count as claims reports is open (XQ05) |
| BCL_DOCUMENT_TYPE | Attachment types on the claim | PLA; CRF; estimate; offer; signed offer; LOA; release papers; others (to confirm, CLQ26) |

Attachments on a claim are named `<CLAIM NO>_<DOCTYPE>_<n>` by the platform naming service.

# Interfaces and integration

Figure 3 shows the interfaces of the Claims module. Claims reads the BRD-1 and BRD-2 modules through their public services and events; no other module writes claims. Renewal and the contact centre read claims through one read service and the document store.

![Interfaces of Claims (dashed = parked)](figures/brd07_integration.dot)

> [!PARKED] Parked seams
> The migration of EBIX / ISYS claims keeps a source value "migrated" and a legacy reference on the claim (CLQ14). Claim money through BDOI would use the existing Cashiering and Disbursement paths with a new receipt type and payee class (CLQ10). Neither is simulated.

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Accounts (BRD-1) | In | Cover snapshot: client, product, period, insurer, sum insured, sales stamp, locations | BRCLM.002, 003, 016, 037 | DESIGNED |
| Endorsements (BRD-1 booking) | In | Cover version; newer endorsement event | BRCLM.039 | DESIGNED |
| Invoice ledger (BRD-2) | In | Invoices of the cover with payment and remittance status; payment and remittance events | BRCLM.001 | DESIGNED |
| Special remittance (BRD-2) | Out | Feed CLAIMS_SPECIAL_REMIT through the port ClaimsFeed | BRCLM.010; OQ46 | DESIGNED |
| E-mail outbox | Out | Loss advice to insurers; send log | p.24-25 | DESIGNED |
| Documents | Out | Claims reports as CLAIM_REPORT, linked to claim, account and client | BRCSF-009; D3 | DESIGNED |
| Client 360 view (CRM) | Out | Claims of the client | BRCLM.040 | DESIGNED |
| Renewal (BRD-6) | Out | Loss experience per account and policy year (decision D4) | BRCLM.030, 040 | DESIGNED |
| Notifications and alerts | Out | Assignment, status, follow-up, premium, newer version, past due | NFR 15.14 | DESIGNED |
| EBIX / ISYS claims | In | Migration of open and historical claims | p.23; BRCLM.007 | PARKED |
| Claim proceeds through BDOI | Out | Cashiering receipt and Disbursement payout, only if CLQ10 is answered yes | BRCLM.010 status 12 | PARKED |
| Insurer channels (portal, API, bordereaux) | In | Manual recording and upload cover the need today | BRCLM.041, 043 | PARKED |
| Shared drive for report files | Out | Report archive in BIBS; drop to a drive only if BDOI confirms | NFR 15.09-15.13 | PARKED |


# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.32-41)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Authentication | Windows credentials; masked password; plain log-on errors; lockout after 3 attempts; password change every 90 days | Platform log-in (FR-CL-001); directory sign-in as the parked port of decision D6 | CHANGE |
| Passwords | BDO password standard; minimum 8 (admin 12); history of 8; minimum age 1 day | Platform password policy | FIT |
| Access control | Custom roles; protection against direct object reference manipulation; user administration | Roles and permissions (FR-CL-002); company check on every claim | FIT |
| Audit logging | Access attempts, privileged use, admin changes, customer record access and updates; timestamp, user, source IP | Claim history and platform audit (FR-CL-003) | FIT |
| Sessions | Idle timeout 15 minutes; random session IDs; renewed on log-in | Session policy parameter; value to align (CLQ24) | CONFIGURE |
| Users | HO Claims 28 (15 concurrent); branch Claims 9 (5); HO Marketing 10 (5); support, IT, DCO 10 each (2) | Well within the BRD-1 sizing | FIT |
| Volumes | Claims booking 55 a day; status and settlement updates 1,096 a day; activity log 1,206 a day; notifications 50 a week; 10% growth | Indexed claim tables; reports as SQL aggregates | FIT |
| Response time | Screens, dashboard, booking, premium validation 5 s; status updates 1 minute; activity log and reports 5 minutes | Online p95 under 3 seconds; reports synchronous under 5 minutes | FIT |
| Operating hours | Monday-Friday 06:00-20:00 | Same deployment as BRD-1 (07:00-22:00); the earlier start is aligned under CLQ24 | CONFIGURE |
| Locations | Head Office Makati and Ortigas; branches Angeles, Cebu, CDO, Davao, GenSan | BDO network, web | FIT |
| Recovery | RTO 4 hours; RPO 4 hours; DR server | Platform HA and DR; one BIBS-wide NFR set is being agreed (XQ08) | CONFIGURE |
| Scalability | No downtime when scaling the application; database scaling up to 120 minutes downtime | Container scaling; DBA procedure | FIT |
| Retention | 10 years online, 15 years archive, purge after 15 years; audit logs archive 16 years; backup every 4 hours, kept 5 years | Retention rule for record type BrokerClaim; the 16-year audit archive is aligned under CLQ24 | CONFIGURE |
| Interface | Follow the existing BDO Insurance UI | BDO UX guidelines (R8) | FIT |

# Configuration items

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.4,8.0 caption="Claims parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| BCL_DEFAULT_CURRENCY | PHP | Claim currency when the cover has none (BRCLM.009) |
| BCL_FOLLOW_UP_DAYS | 7 | Default days to the next follow-up date |
| BCL_AGEING_BUCKETS | 30,60,90,180 | Ageing buckets 0-30, 31-60, 61-90, 91-180, 181+ days |
| BCL_PAST_DUE_DAYS | 90 | Threshold of the past due report and alert |
| BCL_PRONE_MIN_CLAIMS | 3 | Claims for a claims-prone location |
| BCL_PRONE_YEARS | 3 | Look-back years for claims-prone locations |
| BCL_AUTH_DP_POLICY | CONFIRM | Authorization code on direct-payment accounts: ALLOW, CONFIRM, BLOCK |
| LOGIN_MAX_FAILED_ATTEMPTS | 3 | Failed log-ins before lockout (platform, decision D5) |
| Job schedule bcl-follow-up-due-cron | 06:00 Manila daily | Follow-up and diary reminders |
| Job schedule bcl-premium-recheck-cron | 05:30 Manila daily | Re-check of premium on open, unauthorised claims |
| Job schedule bcl-ageing-alerts-cron | 06:00 Manila daily | Past due alerts |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| BCL_CLAIM_STATUS | The 18 statuses of section 5.2 |
| BCL_SETTLEMENT_TYPE | The 10 types of section 5.3 |
| BCL_ADJUSTER | Gemini Adjustment Company; Chartered Adjusters, Inc.; BA International Adjusters & Surveyors Company, Inc.; Total Claims Specialist, Inc.; Unified Adjusters and Surveyors (FAR East), Inc.; Top Brass Insurance Adjusters & Surveyors Co., Inc.; Adjustment Standard Corporation (ASCOR); CARES Adjusters & Surveyors, Inc.; Plaridel Adjusters and Appraisers, Inc.; Crawford & Company Philippines, Inc.; Manila Adjusters & Surveyors Company (MASCO); Tan-Gatue Adjustment Company, Inc.; Technical Inspection Group Adjustment & Surveyors Corp.; Esteban Adjusters and Valuers, Inc.; Interclaim Adjustment Co., Inc.; Pacific International Loss Adjusters Co., Inc.; Senon Insurance Adjusters & Appraisers; Audemus Adjustment Corporation; PALM Property Adjusters; McLarens Philippines; Universal Adjuster Appraisers Co., Inc.; Eagle Prosperity Adjustment and Surveyor Corp.; DFM Adjustment Co., Inc.; Optimum Claims Solutions Insurance Adjustment, Inc.; TreborAsia Insurance Adjustment Services (p.14-15) |
| BCL_CATASTROPHE | Typhoon; Earthquake; Flood; Volcanic Eruption; Landslide; Fire; Others (i.e. Pandemic, El Nino, Terrorism) (p.16) |
| BCL_LOSS_NATURE, BCL_CLAIM_TYPE | Provisional: Motor own damage; Motor third party; Motor theft; Fire; Property; Engineering; Marine; Liability; Personal accident; Others (CLQ12) |
| BCL_UNIT | Motor HO; Non-Motor HO; branches Angeles, Cebu, CDO, Davao, GenSan (CLQ04) |
| BCL_UPDATE_SOURCE | E-mail; Letter; Portal; Call; File (CLQ17) |
| BCL_DIARY_TYPE | Call; E-mail; Meeting; Note; Follow-up |
| BCL_DOCUMENT_TYPE | PLA; CRF; Estimate; Offer; Signed offer; LOA; Release papers; Others (CLQ26) |
| BCL_REOPEN_REASON, BCL_OVERRIDE_REASON | Delivered with generic values; BDOI supplies its own |
| DOCUMENT_TYPE (Claims value) | CLAIM_REPORT (decision D3) |

## Masters and rules maintained by the business

<!-- table: widths=4.8,5.4,6.4 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Claim statuses and their attributes | Unit Head (list authoriser) | FR-CL-040 |
| Status access matrix | Unit Head (authoriser) | FR-CL-041 |
| Claims handler register (units, teams) | Unit Head | FR-CL-041 |
| Settlement types and their attributes | Unit Head (list authoriser) | FR-CL-043 |
| Adjusters / appraisers, catastrophe codes, other Claims lists | Unit Head (list authoriser) | FR-CL-031, 033 |
| Insurer location references | Claims users (BCL_LOCATION_REF_MAINTAIN) | FR-CL-023 |
| Loss advice template | System Administrator | FR-CL-024 |
| Roles and permissions | Unit Head via User Access request (approver) | FR-CL-002 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-CL-01 | The workshop addendum (p.1-9), the renumbering addendum (p.10-18) and the original BRD (p.19-45) together are the baseline; BRCLM.001-036 have the text of FRID-001-036 | R1-R3 |
| A-CL-02 | The EBIX "Cover Number" is the BIBS account (ARN) and policy year; the version is the endorsement sequence | CLQ02 |
| A-CL-03 | BDOI keeps no reserve and receives no claim money; the insurer's reserve and settlement are information | Q44; CLQ10 |
| A-CL-04 | The claims facility is a new BIBS module; the BRD's "enhance the existing claims module in eBIX" (p.23) does not apply | p.23 |
| A-CL-05 | The workshop addendum's seven requirements are in scope, although the addendum says it does not change the scope (p.4) | p.4 |
| A-CL-06 | Claims Officers tag temporary closures; TL / TH close permanently | CLQ06 |
| A-CL-07 | The capacity table of p.39-40 is the requirement, although it is headed "sample requirements" | p.39 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-CL-01 | BDOI gives the status matrix, the claims units and the follow-up days per status | FR-CL-040, 041, 050 (CLQ04, CLQ07) |
| D-CL-02 | BDOI defines the claims authorization code | FR-CL-016 (CLQ01) |
| D-CL-03 | The Remittance owner agrees the feed-based claims condition | FR-CL-046 |
| D-CL-04 | The attachment access classes (EB work item P3) include CLAIM_REPORT so the contact centre can list it | FR-CL-024 (XQ04) |
| D-CL-05 | BDOI provides the loss advice layout and the notification events | FR-CL-024 (CLQ22) |
| D-CL-06 | BDOI decides the migration of EBIX / ISYS claims | Historical analysis in FR-CL-062, 063 (CLQ14) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-7 (status from the cross-BRD decisions, R6)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| CLQ01 | Meaning of the claims authorization code; which unpaid invoices block it; overrides | FR-CL-016 | OPEN |
| CLQ02 | Cover number and version; location versioning; claims on expired or cancelled covers | FR-CL-010, 011, 015, 020 | OPEN |
| CLQ03 | Meaning of "maintain the Reported Date only"; who may correct it | FR-CL-012, 053 | OPEN |
| CLQ04 | Status matrix by role and unit; claims units; follow-up days per status; allowed sequences | FR-CL-040, 041, 042 | OPEN |
| CLQ05 | Settlement types versus settlement status; which types close the claim | FR-CL-043, 044 | OPEN |
| CLQ06 | Closure and reopen rights; ageing of temporarily closed claims | FR-CL-045, 053 | OPEN |
| CLQ07 | Follow-up date computation; diary features | FR-CL-050, 052 | OPEN |
| CLQ08 | Reserve per claim or per insurer; O/S definition; approval of amendments | FR-CL-032, 062 | OPEN |
| CLQ09 | Definitions of claim amount, settlement amount, date settled; partial payments | FR-CL-044, 061 | OPEN |
| CLQ10 | Claim money through BDOI; data of the settlement cheque transmittal | Section 7 | OPEN |
| CLQ11 | Adjuster contacts; accreditation per insurer | FR-CL-031 | OPEN |
| CLQ12 | Claim type and nature of loss values; meaning of CNR | FR-CL-011; section 5.2 | OPEN |
| CLQ13 | Claim number format; legacy numbers | FR-CL-011 | OPEN |
| CLQ14 | Migration of EBIX / ISYS claims | Section 7 | OPEN |
| CLQ15 | Marketing roles, scope and export rights on loss information | FR-CL-065 | OPEN |
| CLQ16 | Granularity and threshold of claims-prone areas; hazard data | FR-CL-063 | OPEN |
| CLQ17 | Channels and sources of insurer updates; bulk upload | FR-CL-022 | OPEN |
| CLQ18 | Source of insurer location references | FR-CL-023 | OPEN |
| CLQ19 | Multi-insurer incidents: co-insurance, several policies, several ARNs | FR-CL-021 | OPEN |
| CLQ20 | Loss ratio premium basis, period basis and grouping | FR-CL-062 | OPEN |
| CLQ21 | BIBS replacing ISYS for claims reporting; buckets; shared drive | FR-CL-060, 061 | OPEN |
| CLQ22 | Notification events and templates to client, AO and insurer | FR-CL-024, 042 | OPEN |
| CLQ23 | Who records the notice of loss (AO or Claims) | FR-CL-011 | OPEN |
| CLQ24 | NFR alignment: hours, session timeout, audit archive, RPO | Section 8 | OPEN |
| CLQ25 | Third-party claimants and claimant contact details | FR-CL-030 | OPEN |
| CLQ26 | Document checklist per claim type | Section 5.2 | OPEN |
| CLQ27 | Branch users see only their branch's claims or all | FR-CL-010 | OPEN |
| CLQ28 | Total-loss indicator for Renewal (XQ10) | FR-CL-043 | OPEN |
| OQ46 | What Operations needs from Claims (claims special remittance) | FR-CL-046 | PARTIAL |
| Q44 | Insurer-only functions not required for BDOI | FR-CL-032 | ANSWERED |
| Q42 | BDO single sign-on / Windows ID | FR-CL-001 | ANSWERED |

<!-- pagebreak -->

## Differences between the design and the BRD

<!-- table: widths=3,7,6.6 caption="Recorded differences" size=8.5 -->
| BRD item | BRD text | FRS and design |
|---|---|---|
| p.23, assumption 1 | Enhance the existing claims module (in eBIX) | New BIBS module; the platform's insurer-side claims module does not fit a broker (R5 section 2) |
| BRCLM.007 | Get the policy number from EBIX and upload it to Claims | The policy number is on the BIBS account and is copied; no upload (FR-CL-013) |
| BRCLM.014 | "Requested Type of Settlement" values | The values are outcomes; each carries whether it closes the claim (FR-CL-043; CLQ05) |
| BRCLM.005 / BRCLM.035 | Officers tag closures; TL / TH close | Officers set temporary closures; TL / TH close permanently (FR-CL-045; CLQ06) |
| p.28 "Settlement Type Status values" | Maintained by the Unit Head; no ID, no values | Not built as a separate list until CLQ05 is answered |
| NFR 1.01 | Windows credentials | Directory sign-in built as a parked port (decision D6) |

# Traceability

Every BRD-7 requirement is met by at least one FR. The screen column names the main entry point; the last column names the section of the build design (R5) that specifies it. The non-functional requirements (p.32-41) are traced in section 8; the loss advice process (p.24-25) in FR-CL-024.

<!-- table: widths=2.2,3.6,5.6,5.2 caption="BRD ID to FR, screen and design" size=8 -->
| BRD ID | FR | Screen | Design (R5) |
|---|---|---|---|
| BRCLM.001 | FR-CL-016, FR-CL-046 | Record Claim; Claim record | 3.1, 8.3 |
| BRCLM.002 | FR-CL-010, FR-CL-002 | Cover Lookup | 7.1, 11 |
| BRCLM.003 | FR-CL-010, FR-CL-011, FR-CL-015 | Cover Lookup; Record Claim | 3.1, 5.1 |
| BRCLM.004 | FR-CL-012 | Record Claim; Claim record | 5.1 |
| BRCLM.005 | FR-CL-045, FR-CL-002 | Claim record (Set Settlement) | 7.1, 8.1 |
| BRCLM.006 | FR-CL-030, FR-CL-002 | Claim record (Override Claimant) | 5.1, 7.1 |
| BRCLM.007 | FR-CL-013 | Claim record (summary) | 3.1 |
| BRCLM.008 | FR-CL-013 | Invoice screens (BRD-1, BRD-2) | 3.1 |
| BRCLM.009 | FR-CL-011 | Record Claim | 5.1, 9.3 |
| BRCLM.010 | FR-CL-040, FR-CL-046 | Lists of values; Claims Setup | 5.3, 8.1 |
| BRCLM.011 | FR-CL-042, FR-CL-002 | Claim record (Change Status) | 8.1 |
| BRCLM.012 | FR-CL-041 | Claims Setup (Status Access Matrix) | 5.3, 8.1 |
| BRCLM.013 | FR-CL-041 | Claim record (Change Status) | 8.1 |
| BRCLM.014 | FR-CL-043 | Lists of values; Claims Setup | 5.3, 8.1 |
| BRCLM.015 | FR-CL-044, FR-CL-002 | Claim record (Set Settlement) | 8.1 |
| BRCLM.016 | FR-CL-014 | Claim record (summary) | 3.1, 5.1 |
| BRCLM.017 | FR-CL-031 | Lists of values (BCL_ADJUSTER) | 9.3 |
| BRCLM.018 | FR-CL-031, FR-CL-002 | Claim record (Assign Adjuster) | 5.1, 7.1 |
| BRCLM.019 | FR-CL-050, FR-CL-054, FR-CL-002 | Claim record; Follow-ups Due | 8.1, 9.1 |
| BRCLM.020 | FR-CL-051 | Claim record (Details) | 5.1 |
| BRCLM.021 | FR-CL-051, FR-CL-002 | Claim record (Details) | 7.1 |
| BRCLM.022 | FR-CL-052, FR-CL-054 | Claim record (Diary); My Diary | 5.2, 9.1 |
| BRCLM.023 | FR-CL-032 | Claim record (Reserve & Settlement) | 5.2 |
| BRCLM.024 | FR-CL-032, FR-CL-002 | Claim record (Amend Reserve) | 7.1 |
| BRCLM.025 | FR-CL-053 | Claim record; worklist | 10 |
| BRCLM.026 | FR-CL-060 | Reports (BCL-AGEING) | 10 |
| BRCLM.027 | FR-CL-053, FR-CL-042 | Claim record (History) | 5.2, 10 |
| BRCLM.028 | FR-CL-060 | Reports (BCL-AGEING-STATUS) | 10 |
| BRCLM.029 | FR-CL-061, FR-CL-044 | Reports (BCL-SETTLED) | 10 |
| BRCLM.030 | FR-CL-062 | Reports (BCL-LOSS-EXPERIENCE) | 10 |
| BRCLM.031 | FR-CL-061 | Reports (BCL-OUTSTANDING) | 10 |
| BRCLM.032 | FR-CL-062 | Reports (BCL-LOSS-RATIO) | 10 |
| BRCLM.033 | FR-CL-064 | Reports (BCL-DATA-EXTRACT) | 10 |
| BRCLM.034 | FR-CL-054, FR-CL-061, FR-CL-055 | Follow-ups Due; Claims Home | 9.1, 10 |
| BRCLM.035 | FR-CL-045 | Claim record; worklist tabs | 8.1 |
| BRCLM.036 | FR-CL-033 | Lists of values (BCL_CATASTROPHE) | 9.3 |
| BRCLM.037 | FR-CL-020 | Record Claim; Locations tab | 5.2 |
| BRCLM.038 | FR-CL-063 | Reports (BCL-PRONE-LOCATIONS) | 10 |
| BRCLM.039 | FR-CL-015 | Claim record (summary) | 3.1 |
| BRCLM.040 | FR-CL-065 | Reports; account page | 7.2, 12.2 |
| BRCLM.041 | FR-CL-022, FR-CL-011, FR-CL-003, FR-CL-066 | Insurers & Updates tab | 5.2, 9.5 |
| BRCLM.042 | FR-CL-023, FR-CL-003, FR-CL-066 | Insurer Location References | 5.2, 9.5 |
| BRCLM.043 | FR-CL-021, FR-CL-066, FR-CL-055 | Insurers & Updates tab; worklist | 5.2, 9.5 |


# Sign-off

By signing, BDOI confirms that this FRS describes the Claims functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Claims", organisation: BDOI}
  - {name: "", role: "SAVP Unit Head, Claims, Risk Management and Analytics", organisation: BDOI}
  - {name: "", role: "Team Head, Motor Claims", organisation: BDOI}
  - {name: "", role: "Team Head, Non-Motor Claims", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
