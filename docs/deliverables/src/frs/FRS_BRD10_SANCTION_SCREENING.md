---
# Source of the Functional Requirements Specification for BRD-10 Sanction Screening and Risk Profiling.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD10_SANCTION_SCREENING.md
title: Sanction Screening and Risk Profiling
subtitle: BRD-10 Sanction Screening and Risk Profiling
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-10
name: Sanction Screening
doc_id: BIBS-FRS-BRD-10
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-10 Sanction Screening
output: FRS/BIBS_FRS_BRD-10_Sanction_Screening_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-10 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; specified from the BRD and the build design (not yet built); aligned with the cross-BRD decisions
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Chief Compliance Officer and Compliance unit, role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Unit Head, Claims and Risk Management", role: Approver, organisation: BDOI, purpose: Review of the risk-profiling and case steps}
  - {name: "Combank, Corbank and Retail Marketing", role: Business user, organisation: BDOI, purpose: Review of the investigation and approval steps}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Sanction Screening and Risk Profiling business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system will behave as Compliance expects. The project team uses it to build, test and prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

Sanction screening is designed but not yet built. The FRs are specified from the BRD (R1), the requirements baseline (R2) and the build design (R3). Message codes are assigned at build; this document gives the message text only.

## Scope

The scope is the screening of BDOI clients against sanctions and politically exposed person (PEP) lists, the tagging of the client risk profile, and the handling of the resulting cases up to the Suspicious Transaction Report (STR) for the Anti-Money Laundering Council (AMLC). The BRD calls the company BDOIR; this document uses BDOI, as the other BRDs do.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Configuration | Matching criteria, risk-profile categories and rules, approval, assignment and SLA matrices, review and STR templates, dispositions per stage, validation rules; all versioned and approved by a checker | SNSRP-101 to 109 |
| Sanctions and PEP list | Scheduled list intake with a run log, alerts on failed records, manual additions and changes under maker-checker | SNSRP-201 to 204 |
| Matching and risk tagging | Screening triggers, name matching, automatic risk tagging, automatic case creation, manual risk-tag update with evidence | SNSRP-301 to 304, 602 |
| Case management | Case lifecycle, case list, search, re-assignment, SLA timer and escalation, high-risk client list | SNSRP-401 to 405; p.8 |
| Investigation | Review templates, dispositions, KYC and supporting documents | SNSRP-501, 502, 601 |
| Approval and escalation | Validation before approval, Unit Head approval, BU escalation review, AML Committee decision | SNSRP-701 to 704 |
| STR | STR prefilled from the case, completeness check, extraction of committee-approved STRs in the AMLC format | SNSRP-705, 706 |
| Notifications, reports and audit | Assignment, SLA and document notifications, compliance reports, immutable audit log and audit report | SNSRP-801, 802, 901 to 903 |

**Out of scope for this phase:**

- Filing the STR on the AMLC portal. The to-be diagram keeps "File STR to AMLC" in a "Via Portal" lane outside the in-system workflow (p.8). BIBS prepares, approves and extracts the STR and records the filing reference.
- Blocking quotations, account submission, placement or booking while a match is open. The BRD asks for tags, cases and notifications, not for a stop on business. A blocking gate is a proposal behind a parameter, off by default (SQ07, section 10.3).
- Screening of parties that BIBS does not hold today (beneficial owners, authorised signatories, beneficiaries). BDOI confirms the scope under SQ11.
- Direct connection to BDO's Negative List Database System (NLDS) and to external list providers by API. Lists come in as files until BDOI names the sources and transports (SQ01, section 7).
- Adverse-media screening. BSP M-2025-017 is cited in the objectives (p.3) but no requirement asks for it (SQ20).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Sanction Screening and Risk Profiling Business Requirements Document, 29 pages | Prepared 10-Apr-2026; approved 16 to 17-Apr-2026 | `docs/source-documents/Sanction Screening and Risk Profiling BRD.pdf` |
| R2 | BDOI Sanction Screening and Risk Profiling (BRD-10) requirements baseline and fit/gap | current | `docs/requirements/BDOI_SANC_BRD_SPEC.md` |
| R3 | Sanction Screening and Risk Profiling build design | current (proposal for review) | `docs/architecture/SANCTION_SCREENING_DESIGN.md` |
| R4 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R5 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |
| R6 | BRD-1 New Business requirements baseline (client master, KYC review, shared platform) | current | `docs/requirements/BDOI_NB_BRD_SPEC.md` |
| R7 | FRS BRD-11 User Access Maintenance (roles, sign-in, session policy) | v1.0 | `docs/deliverables/out/Drop-0_Setup_and_Data_Migration/FRS/BIBS_FRS_BRD-11_User_Access_Maintenance_v1.0.pdf` |

Page references in this document ("p.14") are pages of the BRD-10 PDF (R1). The requirement table is printed as images on pp.10-21; it was read from the page images.

## Definitions and acronyms

```glossary
AMLA: Anti-Money Laundering Act of 2001 (RA 9160) and its amendments
AMLC: Anti-Money Laundering Council, which receives STRs through its portal
AML Committee: BDOI committee that decides escalated cases and approves STRs (five members, p.23)
Approver: The Unit Head who approves or disapproves the Investigator's recommendation (SNSRP-702)
BRD: Business Requirements Document
BSP: Bangko Sentral ng Pilipinas
BU: Business unit
Case: A screening case of one client, opened by a match, a risk tag or an account application; numbered SCR-yyyy-nnnnnn
CC: Compliance Officer (Checker); approves configuration and list changes
CO: Compliance Officer (maker); configures, maintains lists, reviews BU escalations and prepares STRs
Configuration version: One dated set of rules of one type (for example the matching criteria), approved by a checker and active from its effective date
Delta screening: Screening of the clients against the list entries changed since the last run
Disposition: The outcome a user selects at a case stage, from the list configured for that stage (SNSRP-107)
EDD: Enhanced Due Diligence
False positive: A potential match that the Investigator confirms is not the listed person
FR: Functional requirement of this document (FR-SS-nnn)
KYC: Know your customer
LOV: List of values maintained in BIBS
NLDS: BDO Negative List Database System, used today to validate PEP names (p.7)
PEP: Politically exposed person
Potential match: A client and list entry pair whose score meets the matching criteria, not yet decided
Risk category: A risk-profile tier (for example High-risk, PEP) with its client risk rating and tags (SNSRP-102)
SLA: Service level agreement; here the target hours of a case stage
SNSRP: Requirement ID prefix of the BRD (SNSRP-101 to SNSRP-903, 37 IDs)
SQnn: Open question on BRD-10 raised by the project team (section 10.3); cited elsewhere as "SANC SQnn" (R4, XQ07)
STR: Suspicious Transaction Report filed with the AMLC
UAT: User acceptance testing
UCC: Unit Compliance (User) Coordinator
Watchlist: The sanctions and PEP entries held in BIBS, by source
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority** (every BRD-10 requirement is "Must have"), the **fit** class of the baseline (R2), the **screens** that implement it and the **service** or job of the design (R3).
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by Compliance or the System Administrator in BIBS (configuration version, parameter or list of values, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check and the message the user sees. The message codes are assigned when the module is built; the column shows "To be assigned at build". A "-" marks a screen check (for example a blank mandatory field), which has no business code.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-10 test plan.

> [!NOTE]
> Thresholds, SLA hours, template fields, dispositions and routing rules shown as "default" or "seed" are placeholders until BDOI answers the open questions in section 10.3. They are configuration entered by Compliance, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R2)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works today with the BIBS platform |
| CONFIGURE | Needs set-up only (parameters, lists, templates) |
| CHANGE | Extends an existing platform capability |
| NEW | A capability that does not exist in BIBS before BRD-10 |

The 37 BRD requirements split into 1 FIT, 2 CONFIGURE, 20 CHANGE and 14 NEW (R2, section 3).

# Business context and process overview

## Business context

BDOI screens clients by hand today (p.6-7). The AML unit sends sanctioned names by e-mail advisory. Compliance consolidates them and publishes them on SharePoint. Investigators match the names against new and existing clients, tag the client risk profile by hand, fill review templates, and route them by e-mail for review and approval. PEP lists are prepared every quarter by Marketing Account Officers and validated in NLDS, to which one person has access. There is no Enhanced Due Diligence process for PEPs.

The BRD asks for one in-system workflow with configurable criteria and matrices, a list that is updated regularly, automated matching, risk tagging and case creation, review templates, routing to approvers, escalation to Compliance and the AML Committee, STR preparation and extraction, notifications, reports and immutable audit logs (p.8-9). BDOI expects to free 64 hours a month for Compliance, save 10 minutes per case for name screening and cut the case cycle from 4 hours to 1 hour (p.4-5).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.6-8)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | The risk-profile category is cascaded by memo | Compliance maintains risk categories and rules in BIBS; a checker approves each version |
| 2 | Sanctioned names arrive from the AML unit by e-mail advisory | List files are loaded on a schedule or uploaded; every run is logged and failed records are reported |
| 3 | Names are consolidated and published on SharePoint | The watchlist is held in BIBS by source; manual additions go through maker-checker |
| 4 | Investigators match names against BDOI systems by hand | BIBS matches every new client, every account application, every list change and the whole client base in a batch window |
| 5 | The client risk profile is tagged by hand | Risk rules set the client risk rating and the PEP / watchlist tags; the Investigator can override with evidence |
| 6 | Review templates are filled outside the system and e-mailed | The Investigator completes the review template on the case and uploads the KYC documents |
| 7 | Items are routed for review and approval by e-mail | BIBS validates the case and routes it by the approval matrix to the Unit Head, Compliance and the AML Committee |
| 8 | PEP lists are prepared quarterly by Marketing and validated in NLDS by one person | PEP entries are part of the watchlist; PEP clients get a case and, where required, an EDD review |
| 9 | The STR is prepared by hand and filed on the AMLC portal | BIBS prefills the STR from the case and extracts the committee-approved STRs in the AMLC format; filing on the portal stays manual |

## Process overview

The table lists the steps of the to-be process, and Figure 1 shows them by actor. Steps 5 to 12 are stages of the BIBS case workflow SCR_CASE (section 5).

<!-- table: widths=0.8,3.8,3.4,7,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Configure | Compliance Officer, Compliance Checker | Matching criteria, risk categories and rules, matrices, templates, dispositions and SLAs are drafted, approved and made active from a date | SNSRP-101 to 109 |
| 2 | List intake | System; Compliance Officer | Sources are loaded on a schedule or by upload; manual entries are approved by the checker | SNSRP-201 to 204 |
| 3 | Name matching | System | Clients are matched against the active list with exact, phonetic and fuzzy rules; each result is recorded | SNSRP-301, 602 |
| 4 | Risk tagging | System; Investigator | Risk rules tag the client; the Investigator overrides a validated false positive with evidence | SNSRP-302, 304 |
| 5 | Case creation | System | A case opens for a high-risk or PEP tag or an account application and is assigned by the assignment matrix | SNSRP-303, 106 |
| 6 | Investigation | Investigator | Completes the review template, uploads documents, selects a disposition and submits | SNSRP-501, 502, 601 |
| 7 | Validation and routing | System | Checks the case against the validation rules and routes it by the approval matrix | SNSRP-701, 103 |
| 8 | Unit Head approval | Approver | Approves, or disapproves with a rationale and returns the case | SNSRP-702 |
| 9 | BU escalation review | Compliance Officer | Records an outcome and routes by the escalation matrix, or returns for rework | SNSRP-703 |
| 10 | AML Committee | AML Committee members | View the full case and record their decisions; the decision rule finalises the case | SNSRP-704 |
| 11 | STR preparation | Compliance Officer | STR prefilled from the case; completeness check | SNSRP-705 |
| 12 | STR extraction and filing | Compliance Officer | Committee-approved STRs extracted in the AMLC format and saved; the AMLC reference is recorded after portal filing | SNSRP-706 |

![To-be screening process by actor (BRD p.8; SNSRP-101 to 706)](figures/brd10_process_flow.dot)

## Screening triggers and case types

Screening runs on four triggers. Each run is logged (FR-SS-030).

<!-- table: widths=3.6,6.4,4.4,2.6 caption="Screening triggers" -->
| Trigger | When | Clients screened | BRD |
|---|---|---|---|
| Client registered | A client or prospect is created, one at a time or by bulk upload | The new client only | SNSRP-602 |
| Client identity changed | The name, birth date, nationality, TIN or ID of a client changes | That client | SNSRP-602 |
| Account submitted | An account (policy application) of the client is submitted | That client | SNSRP-303 |
| List change | A list entry is added, changed or delisted (file run or approved manual change) | All in-scope clients against the changed entries | SNSRP-201, 204 |
| Batch window | The scheduled period starts (default 01:30 daily; full re-screen monthly) | All clients in scope of parameter SCR_SCREENING_SCOPE | SNSRP-602 |

A case has one of six types (LOV SCR_CASE_TYPE). The type comes from the risk category or the trigger.

<!-- table: widths=4,12.6 caption="Case types" -->
| Type | Opened when |
|---|---|
| NAME_MATCH | A potential match reaches the case threshold of its matching rule |
| PEP | The client is tagged PEP by a risk rule |
| HIGH_RISK | The client is tagged High-risk by a risk rule |
| EDD | The risk category requires Enhanced Due Diligence and the client has an active policy |
| MONITOR | A high-risk or PEP client has no active policy; the UCC and the Investigator are notified (SNSRP-303) |
| ACCOUNT_APPLICATION | The client applies for an account and the screening result requires a review |

# Personas and roles

## Personas

<!-- table: widths=3,4.6,6.6,3.4 caption="Personas and BIBS roles (design R3, section 6.2)" -->
| Persona | BIBS role | Responsibilities in Sanction Screening | BRD |
|---|---|---|---|
| Compliance Officer | COMPLIANCE_OFFICER | Drafts configuration; maintains list entries and uploads list files; reviews BU escalations; prepares and extracts STRs; re-assigns cases; runs reports and the audit report | SNSRP-101 to 108, 203, 703, 705, 706, 901, 903 |
| Compliance Officer (Checker) | COMPLIANCE_CHECKER | Approves or rejects configuration versions and list changes | SNSRP-109, 204 |
| Unit Compliance (User) Coordinator | UNIT_COMPLIANCE_COORD | Monitors cases, re-assigns cases, receives notices for clients without an active policy and document reminders; runs reports | SNSRP-303, 402, 403, 802, 901 |
| Investigator | SCR_INVESTIGATOR | Works the investigation stage: reviews, documents, dispositions; updates risk tags with evidence | SNSRP-304, 402 to 404, 501, 502, 601 |
| Approver (Unit Head) | SCR_APPROVER | Approves or disapproves recommendations; re-assigns approvals | SNSRP-702, 801 |
| AML Committee Member | AML_COMMITTEE | Views committee cases and records decisions | SNSRP-704 |
| Operations Lead | Existing team-lead role named by BDOI (SQ19) | Monitors SLAs and escalations; runs compliance reports | p.23-24 |
| Auditor | AUDITOR (exists) | Reads cases and runs the audit log report and case history | p.24 |
| System Administrator | SYSADMIN (exists) | Read access; jobs and parameters; user access through BRD-11 (R7) | p.24 |
| System | - | Ingestion, matching, tagging, case creation, validation, SLA monitoring, notifications | SNSRP-201, 301 to 303, 401, 405, 602, 701, 902 |

The 287 investigators of the usage table (p.22-23) may be existing Marketing Account Officers. If so, BDOI grants SCR_INVESTIGATOR in addition to their current role; a user may hold several roles (BRD-11, R4 decision D5). The roles above are the project's proposal until BDOI confirms them (SQ19, OQ48).

## Permissions

<!-- table: widths=4.8,2.8,9.4 caption="Sanction Screening permissions and action classes (area SCREENING)" -->
| Permission | Action class | Allows |
|---|---|---|
| SCR_VIEW | VIEW | Open the Sanction Screening screens, cases, the client Screening tab and the watchlist (read only) |
| SCR_CONFIG_MAINTAIN | CREATE / AMEND | Draft and submit configuration versions and templates |
| SCR_CONFIG_APPROVE | APPROVE | Approve or reject configuration versions |
| SCR_LIST_MAINTAIN | CREATE / AMEND | Add, change and deactivate list entries; upload list files; maintain list sources |
| SCR_LIST_APPROVE | APPROVE | Approve or reject list changes |
| SCR_INVESTIGATE | AMEND | Work the investigation stage: reviews, documents, dispositions |
| SCR_RISK_TAG | AMEND | Update a client's risk tag with justification and evidence |
| SCR_CASE_ASSIGN | AMEND | Re-assign cases and approvals |
| SCR_CASE_APPROVE | APPROVE | Unit Head approval stage |
| SCR_COMPLIANCE_REVIEW | APPROVE | BU escalation review and STR preparation |
| SCR_COMMITTEE | APPROVE | AML Committee decision |
| SCR_STR_EXTRACT | CREATE | STR extraction and filing reference |
| SCR_REPORT_VIEW | VIEW | Compliance reports and their export |
| SCR_AUDIT_VIEW | VIEW | Screening audit log report |

## Permissions matrix

The table below is the proposed role-to-action matrix ("Y" = granted). It is maintained through BRD-11 group-profile requests (R7) and shown on the User Access Matrix screen.

<!-- table: widths=4.4,1.36,1.36,1.36,1.36,1.36,1.36,1.36,1.36,1.36 caption="Role-to-action matrix for Sanction Screening (proposal until SQ19 / OQ48)" size=8 -->
| Permission | Comp. Officer | Comp. Checker | UCC | Investi-gator | Approver | AML Comm. | Ops Lead | Auditor | Sys Admin |
|---|---|---|---|---|---|---|---|---|---|
| SCR_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| SCR_CONFIG_MAINTAIN | Y | | | | | | | | |
| SCR_CONFIG_APPROVE | | Y | | | | | | | |
| SCR_LIST_MAINTAIN | Y | | | | | | | | |
| SCR_LIST_APPROVE | | Y | | | | | | | |
| SCR_INVESTIGATE | | | | Y | | | | | |
| SCR_RISK_TAG | | | | Y | | | | | |
| SCR_CASE_ASSIGN | Y | | Y | | Y | | | | |
| SCR_CASE_APPROVE | | | | | Y | | | | |
| SCR_COMPLIANCE_REVIEW | Y | | | | | | | | |
| SCR_COMMITTEE | | | | | | Y | | | |
| SCR_STR_EXTRACT | Y | | | | | | | | |
| SCR_REPORT_VIEW | Y | Y | Y | | | | Y | Y | |
| SCR_AUDIT_VIEW | Y | | | | | | | Y | |

Segregation of duties is enforced by the system, whatever the role grants: the maker of a configuration version or list change never approves it, the Investigator of a case never approves it, an AML Committee member votes once per case, and a re-assigned case can be acted on only by its new assignee.

# Functional requirements

## Access, security and audit

```fr
id: FR-SS-001
title: Restrict screening functions to authorised roles
brd: [SNSRP-101 (p.10), SNSRP-203 (p.13), SNSRP-403 (p.16)]
actor: System
priority: Must have
fit: CHANGE
screens: All Sanction Screening and Compliance Setup screens; User Access Matrix
api: Permission checks on every screening service (area SCREENING)
description:
  - The BRD grants each action to a persona ("given I have configuration permissions", "compliance maker permissions", "access permissions to view cases"). Every screening screen, button and service call requires one of the permissions of section 3.2. Menus show only the screens the user's roles allow, and buttons for actions the user may not perform are hidden.
  - Case data is scoped by role. An Investigator sees the cases assigned to him or her and to his or her team. The UCC, Compliance Officer, Auditor and AML Committee members see all cases; committee members act only on cases in the AML_COMMITTEE stage.
preconditions:
  - "The user is logged in (BRD-11, R7)."
main_flow:
  - The user opens a screen or starts an action.
  - BIBS checks the user's permissions and, for a case, the stage owner and the assignee.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link returns "You do not have access to this page". A service call is refused and logged.
  - Segregation of duties. An action refused by a four-eyes rule is refused with its message even when the role has the permission.
rules:
  - [R1, "Permissions and their action classes are those of section 3.2; roles are granted as in section 3.3 until BDOI confirms the matrix.", Configurable, BRD-11 group-profile request]
  - [R2, "Edit rights on a case come from the stage owner permission and the assignment; they end when the case leaves the stage.", Fixed, "-"]
  - [R3, "Four-eyes rules cannot be switched off.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
  - [Action on a case not assigned to the user, "Case <case no.> is assigned to <user>", To be assigned at build]
notifications:
  - "None."
audit:
  - Refused calls are logged with user, action and time.
acceptance:
  - An Investigator sees Screening Home, Cases and Matches, and does not see Compliance Setup.
  - A Compliance Checker can approve a configuration version and cannot edit it.
  - A direct call to the committee decision by a user without SCR_COMMITTEE is refused and logged.
```

## Configuration

```fr
id: FR-SS-010
title: Maintain versioned screening configuration
brd: [SNSRP-101 (p.10), SNSRP-102 (p.10), SNSRP-103 (p.10), SNSRP-104 (p.10-11), SNSRP-105 (p.11), SNSRP-106 (p.11), SNSRP-108 (p.12)]
actor: Compliance Officer (maker)
priority: Must have
fit: NEW
screens: Compliance Setup > Configuration Versions (one tab per type)
api: Configuration version service (design R3, section 4.1)
description:
  - Every screening rule set is a configuration version of one type - MATCH_CRITERIA, RISK_RULES, APPROVAL_MATRIX, ASSIGNMENT_MATRIX, SLA_MATRIX, VALIDATION_RULES, TEMPLATE and STR_LAYOUT. A version holds its rules, an effective date and a status (DRAFT, PENDING, ACTIVE, SUPERSEDED, REJECTED, section 5.3).
  - The Compliance Officer changes a rule set by creating a draft from the active version, editing it and submitting it. BIBS keeps the user, time and the before / after difference against the active version (the "versioned with user, timestamp, and before/after values" of SNSRP-101 and 108). An active version is never edited.
  - A case keeps the versions that were active when it was created or when its review started, so a change applies to new work only (SNSRP-104, 105, 106).
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN."
main_flow:
  - The Compliance Officer opens the tab of the configuration type and clicks **New Draft**.
  - BIBS copies the active version into a draft.
  - The Compliance Officer edits the rules (FR-SS-011 to FR-SS-018) and sets the effective date.
  - The Compliance Officer opens **Changes** to see the difference against the active version.
  - The Compliance Officer clicks **Submit for Approval**. BIBS checks the version and moves it to PENDING.
  - The Compliance Checker decides it (FR-SS-019).
alternate_flows:
  - The Compliance Officer discards a draft; it is kept as REJECTED with the reason "withdrawn by maker".
  - A draft of the same type already exists. BIBS opens the existing draft instead of creating a second one.
rules:
  - [R1, "At most one DRAFT or PENDING version per type, and one ACTIVE version per type for each effective date.", Fixed, "-"]
  - [R2, "The effective date is today or later.", Fixed, "-"]
  - [R3, "The difference is computed against the version active on the day of submission and stored with the version.", Fixed, "-"]
  - [R4, "A case records the version of each type it used; later versions do not change it.", Fixed, "-"]
validations:
  - [Effective date in the past, The effective date cannot be before today, To be assigned at build]
  - [Draft has no rule, Add at least one rule before submitting, To be assigned at build]
  - [Draft identical to the active version, The draft does not change the active configuration, To be assigned at build]
fields_screen: Configuration Version (header)
fields:
  - [Type, List, "Yes", "MATCH_CRITERIA, RISK_RULES, APPROVAL_MATRIX, ASSIGNMENT_MATRIX, SLA_MATRIX, VALIDATION_RULES, TEMPLATE, STR_LAYOUT", Fixed after creation]
  - [Version no., Number, "Yes", System, "Next number of the type"]
  - [Status, Text, "Yes", System, Read only]
  - [Effective from, Date, "Yes", "-", Today or later]
  - [Change note, Text, "Yes", "-", Up to 1000 characters]
  - [Changes, Table, "No", System, "Rule, attribute, before, after"]
notifications:
  - On submission, the holders of SCR_CONFIG_APPROVE are notified (event SCR_CONFIG_TO_APPROVE) and the version appears in My Approvals.
audit:
  - Create, each save, submit and discard are recorded with user and time; the difference is kept with the version.
acceptance:
  - A submitted matching-criteria draft shows the changed threshold with its before and after values.
  - An active version cannot be edited; a change needs a new draft.
  - A case opened before a new assignment matrix keeps its assignee when the new matrix becomes active.
```

```fr
id: FR-SS-011
title: Configure name-matching criteria
brd: [SNSRP-101 (p.10)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: NEW
screens: Configuration Versions (tab Matching Criteria)
api: Configuration type MATCH_CRITERIA
description:
  - The matching criteria control screening quality. For each list type (SANCTION, PEP, INTERNAL, ADVERSE_MEDIA) and subject type (individual or entity) the Compliance Officer sets the algorithms used - exact, phonetic (Double Metaphone) and fuzzy (Jaro-Winkler) - their thresholds, the fields compared and the minimum score that opens a case.
  - The BRD gives no values. The build delivers seed thresholds; Compliance enters the production values before go-live (SQ02).
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN; a draft of type MATCH_CRITERIA is open (FR-SS-010)."
main_flow:
  - The Compliance Officer adds or edits a matching rule for a list type and subject type.
  - The Compliance Officer selects the algorithm, the threshold, the fields compared and the case threshold.
  - The Compliance Officer saves the draft and submits it (FR-SS-010).
rules:
  - [R1, "Algorithms - EXACT (normalised name), PHONETIC (Double Metaphone keys), FUZZY (Jaro-Winkler similarity).", Fixed, "-"]
  - [R2, "Thresholds are between 0 and 1; a pair scoring at or above the threshold is a potential match.", Configurable, Configuration MATCH_CRITERIA]
  - [R3, "A potential match at or above the case threshold opens or joins a case (FR-SS-034); below it, it waits on the Matches screen.", Configurable, Configuration MATCH_CRITERIA]
  - [R4, "Names are normalised before scoring (case, accents, punctuation, honorifics, word order).", Fixed, "-"]
  - [R5, "A hit on an alias counts as a hit on the entry (to confirm, SQ02).", Configurable, Configuration MATCH_CRITERIA]
validations:
  - [Threshold outside 0-1, Enter a threshold between 0 and 1, "-"]
  - [Case threshold below the matching threshold, The case threshold must be at least the matching threshold, To be assigned at build]
  - [No field selected, Select at least the name field, "-"]
fields_screen: Matching rule
fields:
  - [List type, List, "Yes", LOV SCR_LIST_TYPE, "-"]
  - [Subject type, List, "Yes", Individual / Entity, "-"]
  - [Algorithm, List, "Yes", Exact / Phonetic / Fuzzy, "-"]
  - [Threshold, Number, "Yes", "-", "0 to 1, four decimals"]
  - [Fields compared, Multi-select, "Yes", "Name, Alias, Birth date, Nationality, ID", Name always included]
  - [Minimum score for a case, Number, "Yes", "-", "0 to 1; not below the threshold"]
notifications:
  - "As FR-SS-010."
audit:
  - "As FR-SS-010; the active version used is stored on each screening run."
acceptance:
  - After the checker approves a fuzzy threshold of 0.92 effective today, the next screening run uses 0.92 and records the version on the run.
  - A threshold of 1.5 cannot be saved.
```

```fr
id: FR-SS-012
title: Define risk-profile categories and tagging rules
brd: [SNSRP-102 (p.10)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: CHANGE
screens: Configuration Versions (tab Risk Rules)
api: Configuration type RISK_RULES
description:
  - Risk categories replace the memo on risk-profile categories (p.6). A category has a code, name and tier, the client risk rating it sets (list KYC_RISK_RATING - LOW, STANDARD, HIGH - of the client master), the client tags it adds (PEP, WATCHLIST_REVIEW), the case type it opens and whether it requires EDD.
  - Risk rules assign a category. A rule tests one attribute - match list type, match status, PEP, nationality, occupation, source of funds, client type or market segment - with an operator and values. Rules are evaluated by priority; the first rule that matches decides the category (FR-SS-033).
  - The client master already holds the risk rating and the tags and uses the rating for the KYC review cycle (BRD-1). Screening sets them through the client master; it does not add a second rating.
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN; a draft of type RISK_RULES is open."
main_flow:
  - The Compliance Officer adds or edits the categories.
  - The Compliance Officer adds or edits the rules with their priority.
  - The Compliance Officer submits the draft (FR-SS-010).
rules:
  - [R1, "The first matching rule by priority wins; its rule ID is kept on the client risk-profile history.", Fixed, "-"]
  - [R2, "A category's rating must exist in KYC_RISK_RATING; a new rating value is added through the Lists of Values screen first.", Configurable, LOV KYC_RISK_RATING]
  - [R3, "Categories that require EDD open an EDD case when the client has an active policy (FR-SS-034).", Configurable, Configuration RISK_RULES]
  - [R4, "Whether High-risk equals the rating HIGH, and the category list itself, are confirmed by BDOI (SQ03).", Configurable, Configuration RISK_RULES]
validations:
  - [Rating not in the list, "Risk rating <code> is not a value of KYC_RISK_RATING", To be assigned at build]
  - [Two rules with the same priority, Each rule needs its own priority, To be assigned at build]
  - [Rule points to an unknown category, "Category <code> is not defined in this version", To be assigned at build]
fields_screen: Risk category / Risk rule
fields:
  - [Category code / name, Text, "Yes", "-", Unique in the version]
  - [Tier, Number, "Yes", "-", "1 = highest"]
  - [Client risk rating, List, "Yes", LOV KYC_RISK_RATING, "-"]
  - [Client tags, Multi-select, "No", "LOV CLIENT_TAG (PEP, WATCHLIST_REVIEW)", "-"]
  - [Case type, List, "No", LOV SCR_CASE_TYPE, Blank = no case]
  - [Requires EDD, Check box, "No", "-", "-"]
  - [Rule priority, Number, "Yes", "-", Unique whole number]
  - [Attribute, List, "Yes", "Match list type, Match status, PEP, Nationality, Occupation, Source of funds, Client type, Market segment", "-"]
  - [Operator, List, "Yes", "Equals, In, Not in", "-"]
  - [Values, Multi-select, "Yes", LOV of the attribute, At least one]
notifications:
  - "As FR-SS-010."
audit:
  - "As FR-SS-010. Each tagging records the rule and version that caused it (FR-SS-033)."
acceptance:
  - With an active rule "match list type = SANCTION and status = TRUE_MATCH gives High-risk", a confirmed sanction match sets the client rating to the category's rating and writes an audit record.
  - A category with a rating that is not in KYC_RISK_RATING cannot be submitted.
```

```fr
id: FR-SS-013
title: Maintain the approval and escalation matrix
brd: [SNSRP-103 (p.10), SNSRP-703 (p.18)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: CHANGE
screens: Configuration Versions (tab Approval Matrix)
api: Configuration type APPROVAL_MATRIX; case router
description:
  - The approval matrix decides where a submitted case goes. A route has a from-stage, conditions (case type, risk category, marketing unit, disposition), a to-stage and the approver - the Unit Head of the client's marketing unit, a role or a named user - with an order.
  - Routes from COMPLIANCE_REVIEW form the escalation matrix of SNSRP-703 (for example to the AML Committee, to STR preparation or to closure).
  - Every routing decision is written to the case timeline with the route used.
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN; a draft of type APPROVAL_MATRIX is open."
main_flow:
  - The Compliance Officer adds or edits routes.
  - The Compliance Officer submits the draft (FR-SS-010).
  - When a case is submitted, BIBS takes the first route whose conditions match, in order, and sets the next stage and approver.
alternate_flows:
  - No route matches. BIBS sends the case to UNIT_HEAD_APPROVAL with the Unit Head of the client's marketing unit (default route) and records "default route".
  - The Unit Head cannot be resolved (unit without a head). BIBS assigns the case to the SCR_APPROVER queue of the marketing unit and alerts Compliance.
rules:
  - [R1, "Routes are evaluated in order; blank conditions match any value.", Fixed, "-"]
  - [R2, "The approver of a case is never its Investigator.", Fixed, "-"]
  - [R3, "Who the Approver is (Unit Head of the client's unit) and the matrix dimensions are confirmed by BDOI (SQ04).", Configurable, Configuration APPROVAL_MATRIX]
validations:
  - [Route without to-stage, Select the next stage, "-"]
  - [Approver kind USER without a user, Select the approving user, "-"]
  - [Route to a stage that cannot follow the from-stage, "Stage <to> cannot follow <from>", To be assigned at build]
fields_screen: Approval route
fields:
  - [Order, Number, "Yes", "-", Unique whole number]
  - [From stage, List, "Yes", "Stages of SCR_CASE (INVESTIGATION, UNIT_HEAD_APPROVAL, COMPLIANCE_REVIEW)", "-"]
  - [Case type / Risk category / Marketing unit / Disposition, List, "No", "LOV SCR_CASE_TYPE; risk categories; marketing units; LOV SCR_DISPOSITION", Blank = any]
  - [To stage, List, "Yes", Stages of SCR_CASE, Allowed transition (section 5.2)]
  - [Approver kind, List, Conditional, "Unit Head, Role, User", Required for an approval stage]
  - [Approver, List, Conditional, Roles / users, "Required for Role or User"]
notifications:
  - "As FR-SS-010."
audit:
  - "As FR-SS-010; every routing event is logged on the case timeline with route, stage and approver (SNSRP-103)."
acceptance:
  - With a published matrix, a submitted HIGH_RISK case of the Corbank unit goes to the approver the route names, and the timeline shows the route.
  - A COMPLIANCE_REVIEW outcome "Escalate to Committee" routes the case to AML_COMMITTEE.
```

```fr
id: FR-SS-014
title: Configure the case assignment matrix
brd: [SNSRP-106 (p.11)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: CHANGE
screens: Configuration Versions (tab Assignment Matrix)
api: Configuration type ASSIGNMENT_MATRIX
description:
  - The assignment matrix routes a new case to a team or a user by scenario. A scenario combines case type, trigger, risk category, marketing unit and client type. The target is a team role, with ROUND_ROBIN or LEAST_OPEN balancing, or a named user.
  - The matrix is applied only when a case is created. A new matrix does not move cases that are already assigned (SNSRP-106 AC2).
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN; a draft of type ASSIGNMENT_MATRIX is open."
main_flow:
  - The Compliance Officer adds or edits the scenarios and their targets.
  - The Compliance Officer submits the draft (FR-SS-010).
  - When a case is created, BIBS takes the first scenario that matches and assigns the case.
alternate_flows:
  - No scenario matches. The case goes to the SCR_INVESTIGATOR team queue, unassigned, and the UCC is notified.
rules:
  - [R1, "Balancing - ROUND_ROBIN (next user in turn), LEAST_OPEN (user with the fewest open cases), NONE (team queue).", Fixed, "-"]
  - [R2, "The client's own account officer is never assigned as Investigator (conflict of interest).", Fixed, "-"]
  - [R3, "Only users holding SCR_INVESTIGATE and enabled are assigned.", Fixed, "-"]
validations:
  - [Target neither role nor user, Select a team role or a user, "-"]
  - [User without SCR_INVESTIGATE, "User <user> cannot investigate cases", To be assigned at build]
fields_screen: Assignment rule
fields:
  - [Order, Number, "Yes", "-", Unique whole number]
  - [Case type / Trigger / Risk category / Marketing unit / Client type, List, "No", LOVs and masters, Blank = any]
  - [Team role, List, Conditional, Roles with SCR_INVESTIGATE, Required when no user]
  - [User, Look-up, Conditional, Users with SCR_INVESTIGATE, Required when no team role]
  - [Balancing, List, "Yes", "Round robin, Least open, None", "-"]
notifications:
  - "As FR-SS-010; the assignee is notified of each new case (FR-SS-080)."
audit:
  - "As FR-SS-010; the case timeline records the assignment and the scenario used."
acceptance:
  - A new PEP case of the Retail unit is assigned by the matching scenario when it is created.
  - After a new matrix becomes active, existing cases keep their assignees and new cases follow the new rules.
```

```fr
id: FR-SS-015
title: Configure the SLA matrix per case stage
brd: [SNSRP-108 (p.12)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: CHANGE
screens: Configuration Versions (tab SLA Matrix)
api: Configuration type SLA_MATRIX; job SCR_SLA_MONITOR
description:
  - The SLA matrix sets, per case stage, case type and risk category, the SLA hours, the reminder lead time, the escalation target role and whether hours are calendar or working hours. It overrides the default stage hours of the workflow (section 5.1).
  - When a case enters a stage, BIBS sets its due time from the matrix active on that date. The SLA monitor (FR-SS-044) sends reminders, flags breaches and escalates.
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN; a draft of type SLA_MATRIX is open."
main_flow:
  - The Compliance Officer sets the SLA rows.
  - The Compliance Officer submits the draft (FR-SS-010).
  - When the version becomes active, cases entering a stage get due times from it.
rules:
  - [R1, "The most specific row (stage + case type + risk category) applies; blank conditions match any value.", Fixed, "-"]
  - [R2, "Working hours use the BIBS holiday calendar and office hours 08:00-18:00 Monday to Friday (to confirm, SQ08).", Configurable, Configuration SLA_MATRIX; holiday calendar]
  - [R3, "A changed SLA applies to stage entries from its effective date; running due times do not move.", Fixed, "-"]
validations:
  - [SLA hours not positive, Enter the SLA in hours (greater than 0), "-"]
  - [Reminder lead not below the SLA, The reminder must fall before the SLA ends, To be assigned at build]
fields_screen: SLA rule
fields:
  - [Stage, List, "Yes", Stages of SCR_CASE, "-"]
  - [Case type / Risk category, List, "No", LOV SCR_CASE_TYPE; risk categories, Blank = any]
  - [SLA hours, Number, "Yes", "-", "> 0"]
  - [Reminder lead hours, Number, "Yes", "-", ">= 0 and below the SLA hours"]
  - [Escalate to role, List, "Yes", Roles, "-"]
  - [Calendar, List, "Yes", "Calendar hours, Working hours", "-"]
notifications:
  - "As FR-SS-010."
audit:
  - "As FR-SS-010 (thresholds, escalation and effective date with before / after values)."
acceptance:
  - A case entering INVESTIGATION after a 48-hour SLA becomes active is due 48 hours later (calendar) and is flagged when the time passes.
  - The SLA change shows its before and after values in the version history.
```

```fr
id: FR-SS-016
title: Maintain review templates
brd: [SNSRP-104 (p.10-11)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: NEW
screens: Compliance Setup > Templates
api: Configuration type TEMPLATE (template types KYC_REVIEW, TRANSACTION_REVIEW, EDD)
description:
  - Review templates give every reviewer the same format. A template has a code, a type (KYC review, transaction review, EDD), sections and fields. Each field has a label, data type (text, long text, number, amount, date, list, check box, attachment), mandatory flag, list type, help text and order.
  - The Compliance Officer designs the fields on the template designer and previews the form. A new template version is approved like any configuration and applies to reviews started after its effective date; a review in progress keeps the version it started with (SNSRP-104 AC2).
  - The field lists of the three templates are supplied by BDOI (SQ05). The build delivers seed templates.
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN."
main_flow:
  - The Compliance Officer opens Templates and creates a draft of a template.
  - The Compliance Officer adds sections and fields and previews the form.
  - The Compliance Officer submits the template version; the checker approves it (FR-SS-019).
rules:
  - [R1, "Template types - KYC_REVIEW, TRANSACTION_REVIEW, EDD, STR (STR in FR-SS-017).", Fixed, "-"]
  - [R2, "A review stores the template version it was started with.", Fixed, "-"]
  - [R3, "A field code is unique in the template and never re-used for another meaning.", Fixed, "-"]
validations:
  - [Field without label, Enter the field label, "-"]
  - [List field without list type, Select the list of values of the field, "-"]
  - [Template without a mandatory field, A review template needs at least one mandatory field, To be assigned at build]
fields_screen: Template field
fields:
  - [Section, Text, "Yes", "-", Up to 100 characters]
  - [Code, Text, "Yes", "-", "Unique; letters, digits, underscore"]
  - [Label, Text, "Yes", "-", Up to 200 characters]
  - [Data type, List, "Yes", "Text, Long text, Number, Amount, Date, List, Check box, Attachment", "-"]
  - [List type, List, Conditional, LOV types, Required for List]
  - [Mandatory, Check box, "Yes", "-", "-"]
  - [Help text, Text, "No", "-", Up to 500 characters]
  - [Order, Number, "Yes", "-", Whole number]
notifications:
  - "As FR-SS-010."
audit:
  - "Template versions are saved with version, user and time (SNSRP-104 AC1)."
acceptance:
  - A new KYC review template version, once approved, is used by reviews started after its effective date.
  - A review started on version 1 still shows version 1 after version 2 becomes active.
```

```fr
id: FR-SS-017
title: Maintain STR templates and the extraction layout
brd: [SNSRP-105 (p.11)]
actor: Compliance Officer (maker); Compliance Checker
priority: Must have
fit: NEW
screens: Compliance Setup > Templates (STR); Configuration Versions (tab STR Layout)
api: Configuration types TEMPLATE (type STR) and STR_LAYOUT
description:
  - The STR template holds the fields of the STR form and, for each field, the case data that prefills it (client, case review, account and invoice data). The STR layout defines the extraction file - format (CSV, fixed width, XLSX or XML), delimiter, encoding and the columns with their order, header, length, padding and code mapping.
  - Both are versioned. An update applies to new STRs only and does not change STRs already submitted (SNSRP-105 AC2).
  - The AMLC prescribed format is not in the BRD (SQ09). The build delivers a placeholder layout with the case fields; the AMLC layout is entered as a new version when BDOI supplies it.
preconditions:
  - "The user has SCR_CONFIG_MAINTAIN."
main_flow:
  - The Compliance Officer creates a draft of the STR template or of the STR layout.
  - The Compliance Officer maintains the fields and their prefill sources, or the file columns.
  - The Compliance Officer submits the version; the checker approves it.
rules:
  - [R1, "An STR records the template version it was created with; the extraction records the layout version.", Fixed, "-"]
  - [R2, "Every layout column maps to an STR field or a fixed value.", Fixed, "-"]
validations:
  - [Column without source, "Column <n> needs an STR field or a fixed value", To be assigned at build]
  - [Fixed-width column without length, Enter the column length, "-"]
fields_screen: STR layout column
fields:
  - [Order, Number, "Yes", "-", Whole number]
  - [STR field / fixed value, List / Text, "Yes", Fields of the STR template, One of the two]
  - [Header, Text, "No", "-", "-"]
  - [Length / padding, Number / List, Conditional, "Left, Right", Required for fixed width]
  - [Code mapping, Table, "No", "-", "BIBS code to AMLC code"]
notifications:
  - "As FR-SS-010."
audit:
  - "Saved with version, user and time (SNSRP-105 AC1)."
acceptance:
  - An STR created after a new STR template version uses the new fields; an STR already submitted keeps its fields.
  - The extraction file follows the column order of the active layout.
```

```fr
id: FR-SS-018
title: Configure dispositions per case stage
brd: [SNSRP-107 (p.11-12)]
actor: Compliance Officer; Business Administrator (Lists of Values)
priority: Must have
fit: CONFIGURE
screens: Lists of Values (type SCR_DISPOSITION)
api: LOV SCR_DISPOSITION (parent code = case stage)
description:
  - Dispositions are values of the list SCR_DISPOSITION, each with the case stage as its parent. The case screen offers only the dispositions of the current stage, so a user cannot pick an option that does not belong to the stage.
  - The list uses the existing Lists of Values screen, with maker-checker and effective dates. The delivered values are placeholders until BDOI confirms them (SQ06).
preconditions:
  - "The user has LOV_MANAGE (maker) or LOV approval rights (checker)."
main_flow:
  - The user opens Lists of Values, type SCR_DISPOSITION.
  - The user adds or end-dates a disposition and selects its stage.
  - A second user approves the change; it is available from its effective date.
rules:
  - [R1, "Only dispositions whose parent is the current stage are offered.", Fixed, "-"]
  - [R2, "Each disposition maps to a workflow action (for example FALSE_POSITIVE with submit).", Configurable, LOV SCR_DISPOSITION]
validations:
  - [Disposition without stage, Select the case stage of the disposition, "-"]
fields_screen: List value (SCR_DISPOSITION)
fields:
  - [Code, Text, "Yes", "-", Unique]
  - [Label, Text, "Yes", "-", "-"]
  - [Parent (stage), List, "Yes", "INVESTIGATION, UNIT_HEAD_APPROVAL, COMPLIANCE_REVIEW, AML_COMMITTEE", "-"]
  - [Effective from / to, Date, "Yes / No", "-", To on or after from]
notifications:
  - "Pending list changes appear in My Approvals."
audit:
  - "List changes audited with before and after values."
acceptance:
  - In UNIT_HEAD_APPROVAL the disposition list shows only CONCUR and NOT_CONCUR (delivered values).
  - An end-dated disposition is no longer offered and stays on the cases that used it.
```

```fr
id: FR-SS-019
title: Approve or reject a configuration version
brd: [SNSRP-109 (p.12)]
actor: Compliance Officer (Checker)
priority: Must have
fit: CHANGE
screens: My Approvals; Configuration Versions (Pending)
api: Configuration version service (approve, reject); approval inbox source
description: The checker reviews a pending version with its changes and approves or rejects it. Approval makes the version ACTIVE from its effective date and supersedes the previous version on that date. Rejection records the reason and leaves the active version in force. The maker cannot approve his or her own version.
preconditions:
  - "The version is PENDING; the user has SCR_CONFIG_APPROVE and is not its maker."
main_flow:
  - The checker opens the version from My Approvals.
  - The checker reviews the rules and the before / after changes.
  - The checker clicks **Approve**.
  - BIBS sets the version ACTIVE from its effective date; on that date the previous version becomes SUPERSEDED.
alternate_flows:
  - Reject. The checker clicks **Reject** and enters the reason; the version becomes REJECTED and the maker is notified.
rules:
  - [R1, "The maker never approves.", Fixed, "-"]
  - [R2, "A rejection needs a reason.", Fixed, "-"]
  - [R3, "If no ACTIVE matching version exists when a trigger fires, the run stops and alert SCR_NO_ACTIVE_CONFIG is raised.", Fixed, "-"]
validations:
  - [Checker is the maker, A configuration change is approved by someone other than its maker, To be assigned at build]
  - [Reject without reason, Enter the reason for the rejection, "-"]
notifications:
  - The maker is notified of approval or rejection.
audit:
  - "Decision recorded with checker, time and reason."
acceptance:
  - A draft approved by the checker is ACTIVE from its effective date.
  - A rejected draft keeps its reason and the active version is unchanged.
  - The maker of a version cannot approve it, even with SCR_CONFIG_APPROVE.
```

## Sanctions and PEP list

```fr
id: FR-SS-020
title: Receive the sanctions and PEP lists on schedule
brd: [SNSRP-201 (p.12)]
actor: System; Compliance Officer (upload)
priority: Must have
fit: NEW
screens: Compliance Setup > List Sources and Runs
api: Job SCR_WATCHLIST_INGEST; bulk handler SCR_WATCHLIST; port WatchlistFeed
description:
  - Each list source has a code, name, list type (SANCTION, PEP, INTERNAL, ADVERSE_MEDIA), transport (file, API, manual), schedule and file layout. The delivered sources are AML_ADVISORY (sanctions, file), NLDS_PEP (PEP, file) and INTERNAL (manual).
  - The ingestion job reads each active source on its schedule; the Compliance Officer can also upload a file with **Upload List File**. Each run adds new entries, updates changed entries and delists removed ones, and writes a run log - source, trigger, records received, added, updated, delisted and failed, status and reason for failure (SNSRP-201 AC).
  - Entries loaded from a source file are active at once, as the file is the official list; manual changes go through maker-checker (FR-SS-022). Real-time or API sources are parked until BDOI names them (SQ01).
preconditions:
  - "The source is active; for an upload the user has SCR_LIST_MAINTAIN."
main_flow:
  - At the scheduled time (default 01:00 daily) the job reads the file of each file source.
  - BIBS validates each record against the source layout.
  - BIBS adds, updates or delists the entries and rebuilds their name keys.
  - BIBS writes the run log and starts delta screening of the changed entries (FR-SS-030).
alternate_flows:
  - Some records fail. BIBS loads the valid records, logs each failed record with its line and reason, and marks the run PARTIAL (FR-SS-021).
  - The file is missing or unreadable. The run is FAILED with the reason; alert SCR_INGEST_FAILED is raised.
  - Manual upload. The Compliance Officer selects the source and the file; the run is logged with trigger MANUAL_UPLOAD.
rules:
  - [R1, "An entry is identified by source and external reference; a record already loaded is updated, not duplicated.", Fixed, "-"]
  - [R2, "An entry missing from a full-file source is delisted (status INACTIVE, delisting date set), never deleted.", Fixed, "-"]
  - [R3, "Schedules run outside 08:00-17:00 by default (peak hours, p.24).", Configurable, Source schedule; job cron]
  - [R4, "List sources, formats, frequency and transport are confirmed by BDOI (SQ01).", Configurable, List sources]
validations:
  - [Record without name, "Line <n>: name is missing", To be assigned at build]
  - [Invalid date, "Line <n>: <field> is not a valid date", To be assigned at build]
  - [File type not allowed, "The file type is not allowed for source <code>", To be assigned at build]
fields_screen: List source / Upload list file
fields:
  - [Code / Name, Text, "Yes", "-", Unique code]
  - [List type, List, "Yes", LOV SCR_LIST_TYPE, "-"]
  - [Transport, List, "Yes", "File, API, Manual", API parked]
  - [Schedule, Text, Conditional, "-", Required for File and API]
  - [File layout, List, Conditional, "CSV / XLSX template", Required for File]
  - [Active, Check box, "Yes", "-", "-"]
  - [File (upload), Attachment, "Yes", "-", "CSV or XLSX, up to the platform size limit"]
notifications:
  - "Compliance recipients are notified of each run with new entries (p.8, notifications on newly added sanctioned names, FR-SS-082)."
audit:
  - "Every run logged with counts, status and reason; the source file is kept as an attachment of the run."
acceptance:
  - A scheduled run of a file with 3 new and 1 changed entry logs received 4, added 3, updated 1 and status SUCCESS.
  - A run that cannot read its file is logged FAILED with the reason and raises SCR_INGEST_FAILED.
  - Uploading the same file twice adds no duplicate entries.
```

```fr
id: FR-SS-021
title: Report records that failed ingestion
brd: [SNSRP-202 (p.13)]
actor: System; Compliance Officer (recipient)
priority: Must have
fit: NEW
screens: List Sources and Runs (run detail); Reports (SCR-INGEST-ERRORS)
api: Job SCR_INGEST_ERROR_DIGEST; alert SCR_INGEST_FAILED
description: Every record that fails ingestion is kept with its source, run, line, raw record and reason. On the agreed schedule (default 07:00 Monday to Friday) BIBS e-mails the list of failed records since the last digest, with where they came from, to the recipients in parameter SCR_INGEST_ALERT_RECIPIENTS. The same list is the report SCR-INGEST-ERRORS. A FAILED or PARTIAL run also raises the in-app alert SCR_INGEST_FAILED.
preconditions:
  - "At least one run with failed records since the last digest."
main_flow:
  - The digest job collects the failed records since the last digest.
  - BIBS builds the report and e-mails it to the recipients.
  - The Compliance Officer opens the run detail, corrects the source file and uploads it again.
alternate_flows:
  - No failed records. No e-mail is sent.
  - No recipient configured. The digest is skipped and the alert shows "no recipients".
rules:
  - [R1, "Recipients and schedule are those BDOI agrees (SQ01).", Configurable, Parameter SCR_INGEST_ALERT_RECIPIENTS; job cron]
validations:
  - [Invalid e-mail in the recipient list, "<address> is not a valid e-mail address", "-"]
notifications:
  - "E-mail digest to the recipients; in-app alert SCR_INGEST_FAILED to Compliance Officers."
audit:
  - "Each digest logged with recipients and record count."
acceptance:
  - A run with 2 failed lines produces a digest listing both lines with source, run and reason.
  - The report SCR-INGEST-ERRORS shows the same records for the run date.
```

```fr
id: FR-SS-022
title: Add or change sanctioned-name and PEP records manually
brd: [SNSRP-203 (p.13)]
actor: Compliance Officer (maker)
priority: Must have
fit: NEW
screens: Compliance Setup > Watchlist
api: Watchlist entry and change (maker-checker)
description:
  - Compliance captures regulatory updates, internal findings or urgent risk actions by adding, changing or deactivating a list entry. An entry holds the source, list type, entity type, primary name, first and last name, aliases, birth date, nationality, ID numbers, listing and delisting dates and remarks.
  - A manual change is saved as a change record in PENDING with the before and after values, the maker and the remarks. It does not affect screening until the checker approves it (FR-SS-023).
preconditions:
  - "The user has SCR_LIST_MAINTAIN."
main_flow:
  - The Compliance Officer opens Watchlist and clicks **Add Entry**, or opens an entry and clicks **Change** or **Deactivate**.
  - The Compliance Officer enters the data and the remarks.
  - BIBS validates the mandatory fields and saves the change as PENDING.
alternate_flows:
  - A pending change already exists for the entry. BIBS refuses a second change until the first is decided.
rules:
  - [R1, "Screening reads ACTIVE entries only; DRAFT and PENDING changes have no effect.", Fixed, "-"]
  - [R2, "Manual entries use source INTERNAL unless the change corrects an entry of another source.", Fixed, "-"]
  - [R3, "Entries are never deleted; they are deactivated.", Fixed, "-"]
validations:
  - [Primary name blank, Enter the name of the listed person or entity, "-"]
  - [List type not selected, Select the list type, "-"]
  - [Remarks blank, Enter the reason for the change, "-"]
  - [Pending change exists, "Entry <ref> already has a change waiting for approval", To be assigned at build]
fields_screen: Watchlist entry
fields:
  - [Source, List, "Yes", List sources, "-"]
  - [List type, List, "Yes", LOV SCR_LIST_TYPE, "-"]
  - [Entity type, List, "Yes", "Individual, Entity", "-"]
  - [Primary name, Text, "Yes", "-", Up to 300 characters]
  - [First / last name, Text, "No", "-", For individuals]
  - [Aliases, Table, "No", "AKA, FKA, spelling", "-"]
  - [Birth date, Date, "No", "-", Not in the future]
  - [Nationality, List, "No", Countries, "-"]
  - [ID numbers, Text, "No", "-", "-"]
  - [Listed on / Delisted on, Date, "No", "-", Delisted on or after listed]
  - [Remarks, Long text, "Yes", "-", Up to 2000 characters]
notifications:
  - "The holders of SCR_LIST_APPROVE are notified (event SCR_LIST_CHANGE_TO_APPROVE); the change appears in My Approvals."
audit:
  - "Before and after values, maker, time and remarks are kept on the change record (SNSRP-203)."
acceptance:
  - A new manual entry is PENDING and is not used by screening until approved.
  - Saving without a name or remarks is refused and names the missing field.
```

```fr
id: FR-SS-023
title: Approve or reject list changes
brd: [SNSRP-204 (p.13)]
actor: Compliance Officer (Checker)
priority: Must have
fit: CHANGE
screens: My Approvals; Watchlist (Pending Changes)
api: Watchlist change approval; delta screening
description: The checker sees the full entry, the before and after values and the maker's remarks. Approval applies the change - the entry becomes ACTIVE (or INACTIVE for a deactivation) with its effective date - and starts delta screening of the entry. Rejection leaves the entry unchanged and logs the checker's remarks. The checker's user ID and time are recorded in both cases.
preconditions:
  - "The change is PENDING; the user has SCR_LIST_APPROVE and is not its maker."
main_flow:
  - The checker opens the change from My Approvals.
  - The checker compares the before and after values and reads the remarks.
  - The checker clicks **Approve**.
  - BIBS applies the change, sets the entry's effective date and screens all in-scope clients against it.
alternate_flows:
  - Reject. The checker enters the remarks and clicks **Reject**; the entry is unchanged.
rules:
  - [R1, "The maker never approves.", Fixed, "-"]
  - [R2, "A decided change is read-only.", Fixed, "-"]
validations:
  - [Checker is the maker, A list change is approved by someone other than its maker, To be assigned at build]
  - [Reject without remarks, Enter the remarks for the rejection, "-"]
notifications:
  - The maker is notified of the decision.
audit:
  - "Decision, checker and time recorded on the change record (insert-only)."
acceptance:
  - An approved new entry is ACTIVE and the next delta screening matches clients against it.
  - A rejected change leaves the entry as it was and shows the checker's remarks.
```

## Matching, risk tagging and case creation

```fr
id: FR-SS-030
title: Trigger client matching
brd: [SNSRP-602 (p.14-15), SNSRP-303 (p.14)]
actor: System
priority: Must have
fit: NEW
screens: Screening Home (last runs); client page (Screening tab)
api: Events client registered, client identity changed, account submitted; job SCR_PERIODIC_SCREENING
description:
  - Matching starts without user action. When a client is created, BIBS screens the new client only, with the active matching criteria and the active list. When a client's identity data changes or the client applies for an account, BIBS screens that client. When the batch window starts, BIBS screens every eligible client in scope against the entries changed since the last run, and once a month against the full list.
  - Eligible clients are those whose status is in parameter SCR_SCREENING_SCOPE (default prospects and confirmed clients); inactive clients are excluded (SNSRP-602 AC2).
  - Every run is logged - trigger, scope, configuration version, clients and entries screened, matches, cases opened, status, start and end time.
preconditions:
  - "An ACTIVE matching-criteria version exists."
main_flow:
  - A trigger fires (client created, identity changed, account submitted, list change or batch window).
  - BIBS starts a screening run with the active configuration.
  - BIBS matches the clients in scope (FR-SS-031) and applies the risk rules (FR-SS-033).
  - BIBS writes the run log.
alternate_flows:
  - No active matching version. The run is not started; alert SCR_NO_ACTIVE_CONFIG is raised.
  - A client created in bulk. Each created client is screened; the runs are grouped under the bulk job.
rules:
  - [R1, "Re-running the same client, entry version and configuration version produces no second match (idempotent).", Fixed, "-"]
  - [R2, "Client statuses in scope of the batch window.", Configurable, Parameter SCR_SCREENING_SCOPE (SQ11)]
  - [R3, "Batch window default 01:30 daily; full re-screen on day SCR_FULL_RESCREEN_DAY of the month.", Configurable, Job cron; parameter SCR_FULL_RESCREEN_DAY]
  - [R4, "Screening runs after the client or account transaction is saved and never delays it.", Fixed, "-"]
validations: []
notifications:
  - "None for the run itself; matches and cases notify as in FR-SS-034 and FR-SS-080."
audit:
  - "Each run is logged with its counts; each match with its run (SNSRP-602 'all activities are logged and traceable')."
acceptance:
  - Creating a new client starts a run for that client only, with no user action.
  - The batch run processes all prospects and confirmed clients and no inactive client.
  - The run log shows trigger, configuration version and counts.
```

```fr
id: FR-SS-031
title: Match client names against the list
brd: [SNSRP-301 (p.14)]
actor: System
priority: Must have
fit: NEW
screens: Matches; Case (Matches tab); client page (Screening tab)
api: Matching engine (name normaliser, name keys, scorer)
description:
  - BIBS normalises the client's name and the entry's names and aliases, finds candidate pairs through blocking keys (name tokens and phonetic keys) and scores each pair with the algorithms of the active criteria. When a pair meets the criteria, BIBS records a match - client, entry and entry version, score, algorithm, matched fields and status POTENTIAL.
  - Birth date, nationality and ID numbers, where held on both sides and selected in the criteria, raise or lower the score. A pair suppressed as a false positive for the same entry version is skipped (FR-SS-035).
preconditions:
  - "A screening run is in progress (FR-SS-030)."
main_flow:
  - BIBS builds the candidate pairs of the clients in scope.
  - BIBS scores each pair.
  - BIBS records a match for each pair that meets the threshold.
  - BIBS passes the matches to the risk rules (FR-SS-033) and case creation (FR-SS-034).
rules:
  - [R1, "Match statuses - POTENTIAL, TRUE_MATCH, FALSE_POSITIVE.", Fixed, "-"]
  - [R2, "One match per client, entry, entry version and configuration version.", Fixed, "-"]
  - [R3, "A changed entry (new entry version) is matched again, even after an earlier false positive.", Fixed, "-"]
validations: []
fields_screen: Matches (list)
fields:
  - [Client, Text, "-", Client master, Name and code]
  - [List entry, Text, "-", Watchlist, "Name, source, list type"]
  - [Score / algorithm, Number / Text, "-", "-", "Score 0 to 1"]
  - [Matched fields, Text, "-", "-", "For example name, birth date"]
  - [Status, Text, "-", "-", "POTENTIAL, TRUE_MATCH, FALSE_POSITIVE"]
  - [Case, Link, "-", "-", Case number when cased]
notifications:
  - "None."
audit:
  - "Each match is stored with run, score and fields; status changes are audited."
acceptance:
  - A client named "Juan Dela Cruz" matches a list entry "Juan de la Cruz" by the phonetic rule and a match is recorded with its score.
  - A pair below the threshold records no match.
  - A second run with the same versions records no duplicate match.
```

```fr
id: FR-SS-032
title: Review potential matches
brd: [SNSRP-301 (p.14), SNSRP-304 (p.14)]
actor: Investigator; Compliance Officer
priority: Must have
fit: NEW
screens: Matches
api: Match decision (open case, false positive)
description: Potential matches below the case threshold are listed on the Matches screen with their score and matched fields. The Investigator opens a case for a match, or marks it a false positive with justification and evidence (FR-SS-035). This keeps weak matches on common names from opening cases while leaving them visible.
preconditions:
  - "The user has SCR_INVESTIGATE."
main_flow:
  - The Investigator opens Matches and filters by score, list type or client.
  - The Investigator compares the client and the entry side by side.
  - The Investigator clicks **Open Case** or **Mark False Positive**.
alternate_flows:
  - The client already has an open case of the same type. **Open Case** adds the match to that case.
rules:
  - [R1, "One open case per client and case type.", Fixed, "-"]
validations:
  - [False positive without justification, Enter the justification and attach the evidence, "-"]
notifications:
  - "A case opened from the screen notifies its assignee (FR-SS-080)."
audit:
  - "Decision on each match recorded with user and time."
acceptance:
  - A potential match marked as false positive leaves the list and is not raised again for the same entry version.
  - Opening a case from a match for a client with an open case adds the match to that case.
```

```fr
id: FR-SS-033
title: Tag the client risk profile automatically
brd: [SNSRP-302 (p.14)]
actor: System
priority: Must have
fit: CHANGE
screens: Client page (Screening tab, risk-profile history); client banner
api: Risk rule evaluator; client risk service of the client master
description:
  - After each match or run, BIBS evaluates the active risk rules for the client. When a rule qualifies, BIBS sets the client's risk rating and adds the category's tags (PEP, WATCHLIST_REVIEW) in the client master, with the source "screening", the rule and the match as reference.
  - The client master then re-schedules the KYC review when the rating goes up (BRD-1 KYC review cycle). The client banner shows the tags, as today; it warns and does not block (SQ07).
  - Each change is written to the client risk-profile history (category, rating, tags, source RULE, rule, match, time).
preconditions:
  - "An ACTIVE risk-rules version exists."
main_flow:
  - A match is recorded or a run ends.
  - BIBS evaluates the rules by priority for each client concerned.
  - BIBS applies the category's rating and tags to the client and writes the history.
  - BIBS passes the result to case creation (FR-SS-034).
alternate_flows:
  - No rule qualifies. The client profile is unchanged.
  - The client already has the same rating and tags. No change is written.
rules:
  - [R1, "Screening changes the rating and tags only through the client master service; it never writes client records directly.", Fixed, "-"]
  - [R2, "A rule never lowers a rating set manually with evidence (FR-SS-035) for the same entry version.", Fixed, "-"]
  - [R3, "Blocking business on a tag is off (parameter SCR_BLOCK_ON_OPEN_MATCH = false, SQ07).", Configurable, Parameter SCR_BLOCK_ON_OPEN_MATCH]
validations: []
notifications:
  - "None for the tag; the case notifies (FR-SS-080)."
audit:
  - "Client audit - 'Risk rating X to Y (screening, rule, match)'; risk-profile history row (insert-only)."
acceptance:
  - A confirmed sanction match under an active High-risk rule sets the client rating and the WATCHLIST_REVIEW tag automatically.
  - The client's Screening tab shows the rule and match that caused the change.
  - The KYC review date of the client moves to the date of the higher rating.
```

```fr
id: FR-SS-034
title: Create cases automatically
brd: [SNSRP-303 (p.14)]
actor: System
priority: Must have
fit: NEW
screens: Cases; Case
api: Case service (open); port ActivePolicyQuery
description:
  - A case opens when a client is tagged High-risk or PEP, when a match reaches the case threshold, and when a client applies for an account and screening requires a review. The case gets a number SCR-yyyy-nnnnnn, the client, trigger, case type, risk category, active-policy flag, marketing unit and Unit Head, and is assigned by the assignment matrix (FR-SS-014).
  - Only clients with an active policy require a KYC review or EDD. For a client without an active policy, the case type is MONITOR and the UCC and the Investigator are notified (SNSRP-303 AC2).
  - The active policy of a client is read through a port. The default reads the client's accounts in status POLICY_ISSUED or BOOKED; BDOI confirms the definition (SQ10).
preconditions:
  - "Risk tagging or matching produced a result that requires a case."
main_flow:
  - BIBS checks for an open case of the same client and type.
  - BIBS determines the active-policy flag and the case type.
  - BIBS creates the case in stage NEW and routes it to INVESTIGATION with the assignee of the assignment matrix.
  - BIBS notifies the assignee.
alternate_flows:
  - An open case of the same client and type exists. The new match is added to it and the timeline records it.
  - No active policy. Case type MONITOR; notice SCR_NO_POLICY_HIT to the UCC and the Investigator.
rules:
  - [R1, "One open case per client and case type.", Fixed, "-"]
  - [R2, "Case numbers are SCR-<yyyy>-<nnnnnn>.", Configurable, Parameter SCR_CASE_SEQUENCE_PREFIX]
  - [R3, "Active policy - accounts in POLICY_ISSUED or BOOKED (default until SQ10).", Configurable, ActivePolicyQuery adapter]
  - [R4, "The case records the configuration versions in force at creation.", Fixed, "-"]
validations: []
notifications:
  - "SCR_CASE_ASSIGNED to the assignee; SCR_NO_POLICY_HIT to the UCC and the Investigator for a client without an active policy."
audit:
  - "Case creation, assignment and scenario recorded on the case timeline."
acceptance:
  - A High-risk tag on a client with a booked account opens a case assigned by the routing rules.
  - For a client without an active policy, a MONITOR case opens and the UCC and the Investigator are notified.
  - Submitting an account of a listed client opens an ACCOUNT_APPLICATION case automatically.
```

```fr
id: FR-SS-035
title: Update the risk-profile tag manually with justification
brd: [SNSRP-304 (p.14)]
actor: Investigator
priority: Must have
fit: CHANGE
screens: Case (action Update Risk Tag); Matches (Mark False Positive)
api: Client risk service (source MANUAL)
description:
  - For a validated false positive, the Investigator updates the client's risk rating and tags, enters a justification and attaches at least one evidence document. The change takes effect at once in the client master and is written to the risk-profile history with source MANUAL.
  - The match becomes FALSE_POSITIVE and the client is not matched again against that entry until the entry changes (suppression by entry version; SQ12).
preconditions:
  - "The user has SCR_RISK_TAG; the match or case is open."
main_flow:
  - The Investigator clicks **Update Risk Tag** on the case or **Mark False Positive** on the match.
  - The Investigator selects the new rating and tags and enters the justification.
  - The Investigator attaches the evidence.
  - BIBS saves the change, updates the client and suppresses the pair.
alternate_flows:
  - The entry changes later. The suppression lapses and the next run matches the client again.
rules:
  - [R1, "Justification and at least one evidence document are mandatory.", Fixed, "-"]
  - [R2, "Suppression is per client, entry and entry version (default until SQ12).", Fixed, "-"]
validations:
  - [Justification blank, Enter the justification of the change, "-"]
  - [No evidence attached, Attach at least one evidence document, To be assigned at build]
  - [Rating not in the list, Select a valid risk rating, "-"]
fields_screen: Update Risk Tag dialog
fields:
  - [New risk rating, List, "Yes", LOV KYC_RISK_RATING, "-"]
  - [Tags to add / remove, Multi-select, "No", LOV CLIENT_TAG, "-"]
  - [Justification, Long text, "Yes", "-", Up to 2000 characters]
  - [Evidence, Attachment, "Yes", "-", At least one file; naming as FR-SS-052]
notifications:
  - "None."
audit:
  - "Client audit and risk-profile history with user, time, justification and evidence reference."
acceptance:
  - The updated tag shows on the client immediately after saving.
  - The change cannot be saved without evidence.
  - The same entry version does not raise the client again in the next run.
```

## Case management

```fr
id: FR-SS-040
title: Control the case lifecycle
brd: [SNSRP-401 (p.15)]
actor: System
priority: Must have
fit: CHANGE
screens: Case (workflow panel, Timeline tab)
api: Workflow SCR_CASE
description:
  - Cases follow the workflow SCR_CASE (section 5). When a user submits, the case moves to the next stage of the workflow and the previous assignee's edit rights end. Every transition is written to the case timeline with user, time, disposition, reason and remarks.
  - Edit rights come only from the stage owner permission and the assignment, so a case is read-only to everyone else.
preconditions:
  - "The case exists."
main_flow:
  - The user starts an action from the workflow panel.
  - BIBS checks the permission, the assignment and the data of the action.
  - BIBS performs the transition, records it and notifies the next owner.
rules:
  - [R1, "Only the transitions of section 5.2 are possible.", Fixed, "-"]
  - [R2, "A closed case is re-opened only by SCR_COMPLIANCE_REVIEW with a reason.", Fixed, "-"]
validations:
  - [Action not allowed in the stage, "The action <action> is not allowed in stage <stage>", To be assigned at build]
  - [Edit by the previous assignee, "Case <case no.> is no longer assigned to you", To be assigned at build]
notifications:
  - "Stage entry to the next owner (FR-SS-080)."
audit:
  - "Every transition logged on the insert-only case timeline."
acceptance:
  - After the Investigator submits, the case is read-only to the Investigator.
  - The Timeline tab lists every transition with user and time.
```

```fr
id: FR-SS-041
title: View the list of cases and open a case
brd: [SNSRP-402 (p.15-16)]
actor: Investigator; UCC; Compliance Officer
priority: Must have
fit: CHANGE
screens: Cases; Case
api: GET /api/v1/screening/cases
description:
  - The Cases screen lists the cases the user may see, with tabs My Cases, Team, For Approval, Committee, STR and Closed. Filters are date created, marketing unit, Unit Head, case type, risk category, stage, disposition, assignee and SLA state. The list follows the filters and can be exported to Excel.
  - A row opens the case page - summary (case number, client, status, risk flags, SLA badge) and the tabs Matches, Review, Documents, Decisions, STR and Timeline.
preconditions:
  - "The user has SCR_VIEW."
main_flow:
  - The user opens Cases and chooses a tab.
  - The user applies filters.
  - The user opens a case.
rules:
  - [R1, "Investigators see their own and their team's cases; UCC, Compliance Officer, Auditor and AML Committee see all.", Fixed, "-"]
  - [R2, "Filters stay in the address so a filtered list can be shared.", Fixed, "-"]
validations:
  - [Date-to before date-from, The end date must be on or after the start date, "-"]
fields_screen: Cases (columns)
fields:
  - [Case no., Link, "-", "-", SCR-yyyy-nnnnnn]
  - [Client, Text, "-", Client master, Name and code]
  - [Case type / Risk category, Text, "-", "-", "-"]
  - [Marketing unit / Unit Head, Text, "-", Sales organisation, "-"]
  - [Stage / Assignee, Text, "-", "-", "-"]
  - [Created / Due, Date-time, "-", "-", "-"]
  - [SLA, Badge, "-", "-", "On time, Due soon, Breached"]
notifications:
  - "None."
audit:
  - "Exports audited."
acceptance:
  - Filtering by marketing unit and date created shows only the cases of that unit created in the period.
  - An Investigator does not see cases of another team.
```

```fr
id: FR-SS-042
title: Search cases
brd: [SNSRP-403 (p.16)]
actor: Investigator; UCC; Compliance Officer
priority: Must have
fit: CHANGE
screens: Cases (search toolbar)
api: GET /api/v1/screening/cases (search parameters)
description: A user with view rights searches by case number, client name or code, status, date range or assigned user, sees the matching cases, refines the criteria and opens a case from the results.
preconditions:
  - "The user has SCR_VIEW."
main_flow:
  - The user enters one or more criteria in the search toolbar.
  - BIBS lists the matching cases within the user's scope.
  - The user refines the criteria or opens a case.
rules:
  - [R1, "Client names are matched ignoring case and accents; partial text is allowed from 3 characters.", Fixed, "-"]
validations:
  - [Search text shorter than 3 characters, Enter at least 3 characters, "-"]
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - Searching a case number returns that case only.
  - Searching a client name fragment with a date range returns the matching cases created in the range.
```

```fr
id: FR-SS-043
title: Re-assign a case or approval
brd: [SNSRP-404 (p.16)]
actor: Investigator, UCC, Compliance Officer or Approver with SCR_CASE_ASSIGN
priority: Must have
fit: CHANGE
screens: Case (action Re-assign); Cases (bulk re-assign)
api: Workflow assignment with reason
description:
  - A case in a review or approval stage can be re-assigned for workload, absence or conflict of interest. Only eligible users - holders of the stage permission, enabled, and not the client's own account officer - can be selected. Ownership changes without changing the submitted data or the status history.
  - The reason, previous and new assignee, user and time are written to the timeline and the audit log. Only the new assignee can act on the case afterwards.
  - The BRD gives this right to the Investigator persona; whether Investigators may re-assign their own cases or only coordinators may is SQ13. The role matrix grants SCR_CASE_ASSIGN to the UCC, Compliance Officer and Approver until BDOI answers.
preconditions:
  - "The case is in INVESTIGATION, RETURNED, UNIT_HEAD_APPROVAL or COMPLIANCE_REVIEW; the user has SCR_CASE_ASSIGN."
main_flow:
  - The user clicks **Re-assign**.
  - BIBS lists the eligible users.
  - The user selects the new assignee and the reason and enters a comment.
  - BIBS changes the assignee and notifies both users.
rules:
  - [R1, "Reasons - WORKLOAD, ABSENCE, CONFLICT_OF_INTEREST, OTHERS (comment required for OTHERS).", Configurable, LOV SCR_REASSIGN_REASON]
  - [R2, "The case data, stage and history are not changed by a re-assignment.", Fixed, "-"]
validations:
  - [Reason not selected, Select the reason for the re-assignment, "-"]
  - [User not eligible, "<user> cannot take cases in stage <stage>", To be assigned at build]
  - [OTHERS without comment, Enter a comment for reason Others, "-"]
fields_screen: Re-assign dialog
fields:
  - [New assignee, List, "Yes", Eligible users, Not the current assignee]
  - [Reason, List, "Yes", LOV SCR_REASSIGN_REASON, "-"]
  - [Comment, Text, Conditional, "-", Required for OTHERS]
notifications:
  - "SCR_CASE_ASSIGNED to the new assignee; the previous assignee is informed."
audit:
  - "Reason, previous and new assignee, user and time in the timeline and the audit log."
acceptance:
  - The client's own account officer is not offered as the new assignee.
  - After re-assignment, the previous assignee cannot act on the case.
  - The timeline shows the reason and both users.
```

```fr
id: FR-SS-044
title: Monitor SLA per stage, remind and escalate
brd: [SNSRP-405 (p.17), SNSRP-108 (p.12)]
actor: System
priority: Must have
fit: CHANGE
screens: Screening Home (SLA tiles); Cases (SLA badge)
api: Job SCR_SLA_MONITOR (hourly); alert SCR_SLA_BREACH
description:
  - Every hour BIBS checks the due time of each open case. At the due time minus the reminder lead hours it sends a reminder to the assignee. When the SLA is breached it flags the case, sends an escalation notice to the role of the SLA matrix and raises alert SCR_SLA_BREACH.
  - The SLA badge shows on lists and on the case - on time, due soon (inside the reminder lead) and breached.
preconditions:
  - "The case is open and has a due time."
main_flow:
  - The SLA monitor runs.
  - For cases inside the reminder lead, BIBS sends one reminder.
  - For breached cases, BIBS sets the breach flag, notifies the escalation role and raises the alert.
rules:
  - [R1, "One reminder and one escalation per case and stage entry.", Fixed, "-"]
  - [R2, "Hours per stage from the active SLA matrix (FR-SS-015); defaults in section 5.1.", Configurable, Configuration SLA_MATRIX]
validations: []
notifications:
  - "SCR_SLA_REMINDER to the assignee; SCR_SLA_ESCALATION to the escalation role."
audit:
  - "Reminder and breach recorded on the case timeline (events REMINDER, BREACH)."
acceptance:
  - A case 2 hours before its due time with a 4-hour lead receives one reminder.
  - A breached case is flagged, escalated and counted in the breached tile.
```

```fr
id: FR-SS-045
title: Screening home and high-risk client list
brd: [SNSRP-402 (p.15-16), SNSRP-405 (p.17); p.8 capability 4 and report "Extract list of High-risk Clients"]
actor: Compliance Officer; UCC; Operations Lead; Investigator
priority: Must have
fit: CHANGE
screens: Screening Home; High-risk Clients
api: Report SCR-HIGH-RISK-CLIENTS
description:
  - Screening Home shows tiles - open cases per stage, SLA due today and breached, potential matches not yet cased and the status of the last list run. Each tile opens its filtered list.
  - High-risk Clients lists the clients with a high-risk category, PEP or watchlist tag, with rating, tags, open case and marketing unit, and exports the list (capability 4, "view / extract the list of high-risk clients' details and cases created").
preconditions:
  - "The user has SCR_VIEW."
main_flow:
  - The user opens Screening Home and clicks a tile.
  - The user opens High-risk Clients, filters and exports.
rules:
  - [R1, "Counts follow the user's case scope (FR-SS-041).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Exports audited."
acceptance:
  - The breached tile opens the case list filtered to breached cases.
  - The high-risk list exports to Excel with the same rows as on screen.
```

## Investigation

```fr
id: FR-SS-050
title: Complete the guided review template
brd: [SNSRP-501 (p.17)]
actor: Investigator
priority: Must have
fit: NEW
screens: Case (Review tab)
api: Case review and answers
description:
  - The Review tab renders the template of the case type (KYC review, transaction review or EDD) in the version active when the review started. Mandatory fields are marked. The Investigator can save a draft at any time.
  - On submission, an incomplete review is refused and each missing or invalid field is flagged next to the field.
preconditions:
  - "The case is in INVESTIGATION or RETURNED and assigned to the user; the user has SCR_INVESTIGATE."
main_flow:
  - The Investigator opens the Review tab; BIBS starts the review on the active template version.
  - The Investigator completes the fields and clicks **Save Draft** as needed.
  - The Investigator submits with the disposition (FR-SS-051).
alternate_flows:
  - Incomplete. BIBS keeps the case in the stage and flags each missing field.
rules:
  - [R1, "The template of the case type is used; EDD when the risk category requires EDD and the client has an active policy.", Fixed, "-"]
  - [R2, "Validation runs on the server; the form shows the result per field.", Fixed, "-"]
validations:
  - [Mandatory field blank, "<Field> is required", "-"]
  - [Value of the wrong type, "<Field> must be a <type>", "-"]
notifications:
  - "None."
audit:
  - "Review saves and submission recorded with user and time; answers kept with the template version."
acceptance:
  - When the review opens, mandatory fields are marked.
  - Submission with a blank mandatory field is refused and the field is flagged.
```

```fr
id: FR-SS-051
title: Select a disposition and submit the case
brd: [SNSRP-502 (p.17)]
actor: Investigator
priority: Must have
fit: CHANGE
screens: Case (workflow panel)
api: Workflow SCR_CASE action submit
description: With all required fields complete, the Investigator selects a disposition from the list of the stage (FR-SS-018), writes the recommendation and clicks **Submit**. BIBS validates the case (FR-SS-060), routes it by the approval matrix (FR-SS-013) and makes it read-only to the Investigator.
preconditions:
  - "The review is complete; the case is assigned to the user."
main_flow:
  - The Investigator selects the disposition and writes the recommendation.
  - The Investigator clicks **Submit**.
  - BIBS validates the case, routes it and notifies the approver.
alternate_flows:
  - Validation fails. The case stays in INVESTIGATION and the messages are shown (FR-SS-060).
  - Disposition NEED_MORE_INFO. The case stays with the Investigator; the timeline records the request for information.
rules:
  - [R1, "Only dispositions of the current stage can be selected.", Fixed, "-"]
  - [R2, "The recommendation is mandatory.", Fixed, "-"]
validations:
  - [Disposition not selected, Select the disposition, "-"]
  - [Recommendation blank, Write the recommendation, "-"]
fields_screen: Submit dialog
fields:
  - [Disposition, List, "Yes", LOV SCR_DISPOSITION (parent INVESTIGATION), "-"]
  - [Recommendation, Long text, "Yes", "-", Up to 4000 characters]
  - [STR required, Check box, "No", "-", Consistent with the disposition (FR-SS-060)]
notifications:
  - "SCR_CASE_FOR_APPROVAL to the approver."
audit:
  - "Submission with disposition, recommendation, route and approver on the timeline."
acceptance:
  - A submitted case goes to the approver of the matching route and is read-only to the Investigator.
  - Only INVESTIGATION dispositions are offered.
```

```fr
id: FR-SS-052
title: Upload KYC and supporting documents
brd: [SNSRP-601 (p.17)]
actor: Investigator
priority: Must have
fit: CHANGE
screens: Case (Documents tab)
api: Attachments of entity type ScreeningCase; naming pattern SCREENING
description:
  - The Investigator uploads KYC and other supporting documents with their metadata - form type, document type, date received and source. BIBS stores the file with a checksum, shows it on the case timeline and names it by the BRD convention "<Form Type>_<Client Name>_<Date Received>_<Document Type>_<sequence number>".
  - KYC-type documents are also registered on the client's KYC documents, so the client master shows them.
preconditions:
  - "The user has SCR_INVESTIGATE (and attachment rights); the case is open."
main_flow:
  - The Investigator clicks **Upload** on the Documents tab.
  - The Investigator selects the file and enters the metadata.
  - BIBS stores the file, names it and adds it to the timeline.
rules:
  - [R1, "Name - <Form Type>_<Client Name>_<Date Received yyyyMMdd>_<Document Type>_<n>.", Configurable, Naming pattern SCREENING]
  - [R2, "Form type from LOV SCR_FORM_TYPE (KYC_REVIEW, TRANSACTION_REVIEW, EDD, STR) until BDOI defines 'Form Type' (SQ14).", Configurable, LOV SCR_FORM_TYPE]
  - [R3, "File types and size follow the platform attachment rules.", Configurable, Allowed file types]
  - [R4, "Documents are never deleted from a submitted case.", Fixed, "-"]
validations:
  - [Metadata missing, "Enter the <field> of the document", "-"]
  - [Date received in the future, The date received cannot be in the future, "-"]
  - [File type not allowed, The file type is not allowed, "-"]
fields_screen: Upload document dialog
fields:
  - [File, Attachment, "Yes", "-", Allowed types and size]
  - [Form type, List, "Yes", LOV SCR_FORM_TYPE, "-"]
  - [Document type, List, "Yes", LOV SCR_DOCUMENT_TYPE, "-"]
  - [Date received, Date, "Yes", "-", Not in the future]
  - [Source, Text, "Yes", "-", "For example client, branch, Marketing"]
notifications:
  - "None."
audit:
  - "Upload recorded on the timeline (DOCUMENT_ADDED) and in the audit trail with the file hash."
acceptance:
  - An uploaded KYC form received 12-May-2026 for client "ABC Trading" is named KYC_REVIEW_ABC Trading_20260512_<type>_1 and shows on the timeline.
  - A document without date received cannot be uploaded.
```

## Validation, approval and escalation

```fr
id: FR-SS-060
title: Validate dispositioned cases before approval
brd: [SNSRP-701 (p.17)]
actor: System
priority: Must have
fit: NEW
screens: Case (validation messages; Timeline)
api: Configuration type VALIDATION_RULES; case validator
description:
  - When a dispositioned case is submitted, BIBS checks it against the active validation rules of its stage and type - template complete, required document types present, disposition allowed at the stage, recommendation present, STR flag consistent with the disposition. A blocking rule stops the submission; a non-blocking rule gives a warning.
  - Every validation result, passed or failed, is written to the timeline (VALIDATED or VALIDATION_FAILED with the messages).
  - The validation rules are a configuration type maintained like the others (FR-SS-010). The criteria come from BDOI (SQ06).
preconditions:
  - "A case is submitted (FR-SS-051, FR-SS-061, FR-SS-063)."
main_flow:
  - BIBS evaluates the rules of the stage and type.
  - All blocking rules pass. BIBS logs VALIDATED, updates the status and routes the case.
alternate_flows:
  - A blocking rule fails. BIBS logs VALIDATION_FAILED and shows the messages; the case stays.
rules:
  - [R1, "Rule types - TEMPLATE_COMPLETE, DOCUMENT_TYPES_PRESENT, DISPOSITION_ALLOWED, RECOMMENDATION_PRESENT, STR_FLAG_CONSISTENT.", Fixed, "-"]
  - [R2, "Rules, their parameters and blocking flag per stage and case type.", Configurable, Configuration VALIDATION_RULES]
validations:
  - [Required document missing, "Upload the <document type> before submitting", To be assigned at build]
  - [STR flag inconsistent, "Disposition <disposition> requires the STR flag", To be assigned at build]
fields_screen: Validation rule
fields:
  - [Stage, List, "Yes", Stages of SCR_CASE, "-"]
  - [Case type, List, "No", LOV SCR_CASE_TYPE, Blank = any]
  - [Rule, List, "Yes", Rule types (R1), "-"]
  - [Parameters, Text, Conditional, "-", "For example required document types"]
  - [Blocking, Check box, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Every validation outcome logged on the timeline (SNSRP-701)."
acceptance:
  - A case without the required KYC document cannot be submitted and the timeline records VALIDATION_FAILED.
  - A valid case is logged VALIDATED and routed to the approver.
```

```fr
id: FR-SS-061
title: Approve or disapprove the recommendation (Unit Head)
brd: [SNSRP-702 (p.18)]
actor: Approver (Unit Head)
priority: Must have
fit: CHANGE
screens: Cases (For Approval); Case; My Approvals
api: Workflow SCR_CASE actions approve, disapprove
description: The Approver reviews the case, the review, the documents and the recommendation. **Approve** (disposition CONCUR) moves the case to the next step of the approval matrix. **Disapprove** (NOT_CONCUR) needs a rationale; the case returns to the Investigator as RETURNED, and the Investigator can correct and resubmit it (FR-SS-062).
preconditions:
  - "The case is in UNIT_HEAD_APPROVAL and assigned to the user; the user has SCR_CASE_APPROVE and is not the Investigator."
main_flow:
  - The Approver opens the case from For Approval or My Approvals.
  - The Approver reviews it and enters a comment.
  - The Approver clicks **Approve**; BIBS routes the case (COMPLIANCE_REVIEW or CLOSED by the matrix).
alternate_flows:
  - Disapprove. The Approver selects a return reason and writes the rationale; the case goes to RETURNED and the Investigator is notified.
rules:
  - [R1, "The Approver is never the Investigator of the case.", Fixed, "-"]
  - [R2, "SLA 24 hours (default).", Configurable, Configuration SLA_MATRIX]
validations:
  - [Disapprove without rationale, Write the rationale for disapproving, "-"]
  - [Approver is the Investigator, A case is approved by someone other than its Investigator, To be assigned at build]
fields_screen: Decision dialog
fields:
  - [Decision, List, "Yes", LOV SCR_DISPOSITION (parent UNIT_HEAD_APPROVAL), "-"]
  - [Return reason, List, Conditional, LOV RETURN_REASON, Required to disapprove]
  - [Rationale / comment, Long text, Conditional, "-", Required to disapprove]
notifications:
  - "SCR_CASE_RETURNED to the Investigator on disapproval; stage entry to Compliance on approval."
audit:
  - "Decision, rationale, user and time on the timeline."
acceptance:
  - An approved case moves to the next workflow step.
  - A disapproved case is RETURNED with the rationale visible to the Investigator.
```

```fr
id: FR-SS-062
title: Correct and resubmit a returned case
brd: [SNSRP-702 (p.18), SNSRP-703 (p.18)]
actor: Investigator
priority: Must have
fit: CHANGE
screens: Cases (My Cases); Case
api: Workflow SCR_CASE action resubmit
description: A returned case shows the return reason and remarks. The Investigator corrects the review, documents or recommendation and resubmits. The case goes back to the stage that returned it - UNIT_HEAD_APPROVAL or COMPLIANCE_REVIEW - after the validation of FR-SS-060.
preconditions:
  - "The case is RETURNED and assigned to the user."
main_flow:
  - The Investigator opens the returned case and reads the reason.
  - The Investigator corrects the case and writes a response.
  - The Investigator clicks **Resubmit**.
rules:
  - [R1, "Each return and resubmission is a new round on the timeline; earlier rounds stay readable.", Fixed, "-"]
validations:
  - [Response blank, Write your response to the return, "-"]
notifications:
  - "Stage entry to the stage that returned the case."
audit:
  - "Resubmission with response recorded."
acceptance:
  - A case returned by Compliance goes back to COMPLIANCE_REVIEW on resubmission.
```

```fr
id: FR-SS-063
title: Review BU escalations (Compliance)
brd: [SNSRP-703 (p.18)]
actor: Compliance Officer
priority: Must have
fit: CHANGE
screens: Cases (Compliance Review); Case
api: Workflow SCR_CASE actions escalate_committee, prepare_str, close, return_for_rework
description: The Compliance Officer reviews an escalated case and records an outcome from the COMPLIANCE_REVIEW dispositions - close with no action, escalate to the AML Committee, for STR, or return. The case routes by the escalation matrix (FR-SS-013). If items need correction, the Compliance Officer adds remarks and returns the case to the Investigator; the status becomes RETURNED with the reasons captured.
preconditions:
  - "The case is in COMPLIANCE_REVIEW; the user has SCR_COMPLIANCE_REVIEW."
main_flow:
  - The Compliance Officer opens the case.
  - The Compliance Officer selects the outcome and writes remarks.
  - BIBS routes the case by the escalation matrix and records outcome, user, time and remarks.
alternate_flows:
  - Return for rework. The Compliance Officer selects the reason and writes the remarks; the case goes to RETURNED.
rules:
  - [R1, "Outcomes from LOV SCR_DISPOSITION parent COMPLIANCE_REVIEW (delivered - CLOSE_NO_ACTION, ESCALATE_COMMITTEE, FOR_STR, RETURN).", Configurable, LOV SCR_DISPOSITION]
  - [R2, "Remarks are mandatory for a return.", Fixed, "-"]
validations:
  - [Outcome not selected, Select the outcome, "-"]
  - [Return without remarks, Enter the items to correct, "-"]
notifications:
  - "SCR_COMMITTEE_REVIEW to the committee members on escalation; SCR_CASE_RETURNED to the Investigator on return."
audit:
  - "Outcome, user, time and remarks on the timeline (SNSRP-703)."
acceptance:
  - An outcome "Escalate to Committee" routes the case to AML_COMMITTEE.
  - A return sets the case to RETURNED with the remarks and logs the action.
```

```fr
id: FR-SS-064
title: Record the AML Committee decision
brd: [SNSRP-704 (p.18-19)]
actor: AML Committee Member
priority: Must have
fit: NEW
screens: Cases (Committee); Case (Decisions tab)
api: Committee vote; workflow SCR_CASE system action finalise
description:
  - A committee member views the complete case - details, investigation findings, documents and prior comments - and records a decision online - approve STR, no STR, or return - with remarks.
  - When the decision is final under the committee rule, BIBS routes the case - to STR_PREPARATION (approve STR), CLOSED (no STR) or COMPLIANCE_REVIEW (return) - and updates its status. The rule is parameter SCR_COMMITTEE_RULE - ANY, MAJORITY or ALL of SCR_COMMITTEE_SIZE members (default MAJORITY of 5; SQ15).
preconditions:
  - "The case is in AML_COMMITTEE; the user has SCR_COMMITTEE and has not voted on it."
main_flow:
  - The member opens the case from the Committee tab.
  - The member reviews the case and clicks **Record Decision**.
  - The member selects the decision and writes remarks.
  - BIBS records the vote and checks the rule; when it is met, BIBS finalises the case.
alternate_flows:
  - The rule is not yet met. The case stays in AML_COMMITTEE; the Decisions tab shows the votes so far.
  - Tie under MAJORITY with all members voted. The case returns to COMPLIANCE_REVIEW with the note "no majority".
rules:
  - [R1, "One vote per member per case round; a vote cannot be changed after submission.", Fixed, "-"]
  - [R2, "Decision rule and committee size.", Configurable, Parameters SCR_COMMITTEE_RULE and SCR_COMMITTEE_SIZE]
validations:
  - [Decision not selected, Select your decision, "-"]
  - [Member already voted, You have already recorded your decision on this case, To be assigned at build]
fields_screen: Record Decision dialog
fields:
  - [Decision, List, "Yes", "LOV SCR_DISPOSITION (parent AML_COMMITTEE) - APPROVE_STR, NO_STR, RETURN", "-"]
  - [Remarks, Long text, "Yes", "-", Up to 4000 characters]
notifications:
  - "The Compliance Officer is notified when the decision is final."
audit:
  - "Each member's decision, remarks, user ID, date and time on the timeline (SNSRP-704)."
acceptance:
  - A member sees the case details, findings, documents and prior comments before deciding.
  - With rule MAJORITY of 5, the third APPROVE_STR vote moves the case to STR_PREPARATION.
  - Each vote is recorded with user, date and time.
```

## Suspicious Transaction Report

```fr
id: FR-SS-070
title: Prepare the STR from case data
brd: [SNSRP-705 (p.19)]
actor: Compliance Officer
priority: Must have
fit: NEW
screens: Case (STR tab); STR
api: STR service (prefill, completeness)
description:
  - For a case that requires an STR, opening the STR tab creates the STR with number, template version and status DRAFT. BIBS prefills the subject party from the client, the narrative from the case review and recommendation, the transactions from the client's accounts, invoices and receipts, and lists the case documents as attachments. Every field stays editable.
  - The completeness check lists the gaps against the STR template. When it passes, **Mark Ready** moves the case to STR_EXTRACTION and the STR to FOR_APPROVAL; the STR becomes APPROVED when it belongs to a case whose committee decision was APPROVE_STR.
preconditions:
  - "The case is in STR_PREPARATION; the user has SCR_COMPLIANCE_REVIEW."
main_flow:
  - The Compliance Officer opens the STR tab; BIBS prefills the STR.
  - The Compliance Officer edits the party details, transactions and narrative and selects the reason codes.
  - The Compliance Officer clicks **Check Completeness**; BIBS highlights the gaps.
  - The Compliance Officer clicks **Mark Ready**.
alternate_flows:
  - Gaps remain. **Mark Ready** is refused and the gaps are listed.
rules:
  - [R1, "The prefilled subject is a snapshot; later client changes do not alter a submitted STR.", Fixed, "-"]
  - [R2, "Fields, reason codes and prefill sources follow the STR template (FR-SS-017); the AMLC field list is SQ09.", Configurable, Configuration TEMPLATE (STR)]
validations:
  - [Mandatory STR field blank, "<Field> is required for the STR", "-"]
  - [No transaction listed, Add at least one transaction, To be assigned at build]
  - [Transaction amount not positive, The amount must be greater than 0, "-"]
fields_screen: STR
fields:
  - [STR no., Text, "Yes", System, "-"]
  - [Subject party, Section, "Yes", Client master (snapshot), Editable]
  - [Transactions, Table, "Yes", "Accounts, invoices, receipts", "Reference, date, amount, currency, type, description"]
  - [Reason codes, Multi-select, "Yes", LOV SCR_STR_REASON, At least one]
  - [Narrative, Long text, "Yes", Case review and recommendation, Editable]
  - [Attachments, List, "No", Case documents, "-"]
notifications:
  - "None."
audit:
  - "STR creation, each save and status change recorded; the template version is kept."
acceptance:
  - Opening the STR of a case prefills the client's details, the narrative and the case documents.
  - The completeness check highlights each missing mandatory field.
```

```fr
id: FR-SS-071
title: Extract committee-approved STRs in the AMLC format
brd: [SNSRP-706 (p.19-20)]
actor: Compliance Officer
priority: Must have
fit: NEW
screens: STR (Extract Approved STRs)
api: STR extraction; port StrFileSink (default report archive)
description:
  - The Compliance Officer starts the extraction for a period. BIBS selects only the STRs approved by the AML Committee and not yet extracted, builds the file with the active STR layout (FR-SS-017) and saves it to the designated location. The extraction is recorded (batch, period, count, file, user, time) and the STRs become EXTRACTED.
  - Until BDOI names the designated folder (SQ16), the file is saved in the BIBS report archive and downloaded from the STR screen. A shared-folder or SFTP destination is a later adapter with no change to this function.
preconditions:
  - "The user has SCR_STR_EXTRACT; at least one APPROVED STR exists in the period."
main_flow:
  - The Compliance Officer clicks **Extract Approved STRs** and enters the period.
  - BIBS lists the STRs that will be extracted.
  - The Compliance Officer confirms.
  - BIBS generates the file, saves it and records the extraction.
alternate_flows:
  - No approved STR in the period. BIBS shows "No committee-approved STR to extract" and creates no file.
rules:
  - [R1, "Only STRs with an AML Committee APPROVE_STR decision are extracted.", Fixed, "-"]
  - [R2, "An STR is extracted once; a re-extraction needs a reason and is recorded as such.", Fixed, "-"]
  - [R3, "File format per the active STR layout; the AMLC format is SQ09.", Configurable, Configuration STR_LAYOUT]
validations:
  - [Period end before start, The end date must be on or after the start date, "-"]
  - [Nothing to extract, No committee-approved STR to extract, To be assigned at build]
fields_screen: Extract Approved STRs dialog
fields:
  - [Period from / to, Date, "Yes", "-", To on or after from]
  - [STRs, List, "-", System, "Committee-approved, not extracted"]
notifications:
  - "None."
audit:
  - "Extraction recorded with batch, period, count, file hash, user and time (SNSRP-706)."
acceptance:
  - An extraction for a month includes only the committee-approved STRs of that month.
  - The file is saved and downloadable, and the extraction appears in the audit trail.
  - An STR not approved by the committee is never in the file.
```

```fr
id: FR-SS-072
title: Record the AMLC filing reference
brd: [SNSRP-706 (p.19-20); p.8 "File STR to AMLC - Via Portal"]
actor: Compliance Officer
priority: Must have
fit: NEW
screens: STR register; Case (STR tab)
api: STR service (filed)
description: Filing on the AMLC portal stays manual (p.8). After filing, the Compliance Officer records the AMLC reference and the filing date on the STR. The STR becomes FILED and the case closes (action filed).
preconditions:
  - "The STR is EXTRACTED; the user has SCR_STR_EXTRACT."
main_flow:
  - The Compliance Officer opens the STR and clicks **Record Filing**.
  - The Compliance Officer enters the AMLC reference and the filing date.
  - BIBS sets the STR to FILED and closes the case.
rules:
  - [R1, "The AMLC reference is unique.", Fixed, "-"]
validations:
  - [Reference blank, Enter the AMLC reference, "-"]
  - [Filing date before extraction, The filing date cannot be before the extraction date, To be assigned at build]
notifications:
  - "None."
audit:
  - "Filing reference, date, user and time recorded."
acceptance:
  - Recording the reference closes the case and shows FILED in the STR register.
```

## Notifications

```fr
id: FR-SS-080
title: Notify users of new and assigned cases
brd: [SNSRP-801 (p.20)]
actor: System (recipients - Investigator, UCC, Compliance Officer, Approver)
priority: Must have
fit: CONFIGURE
screens: Notifications (bell); e-mail
api: Notification events SCR_CASE_ASSIGNED, SCR_CASE_FOR_APPROVAL, SCR_CASE_RETURNED, SCR_COMMITTEE_REVIEW
description: When a case is created or assigned, the assignee receives a notification that opens the case directly (link to the case page). Approvers, returned-case owners and committee members receive the matching events. Each notification event is logged with recipient, type, date and time.
preconditions:
  - "None."
main_flow:
  - A case is created, assigned, submitted for approval, returned or escalated to the committee.
  - BIBS sends the notification in the app and, where set in the user's preferences, by e-mail.
  - The user clicks the notification and the case opens.
rules:
  - [R1, "Users choose in-app or e-mail per event type.", Configurable, Notification preferences]
validations: []
notifications:
  - "This FR is the notification set."
audit:
  - "Notification events logged with recipient, type, date and time (SNSRP-801)."
acceptance:
  - The assignee of a new case receives a notification and opens the case from it.
  - The notification log shows recipient, type and time.
```

```fr
id: FR-SS-081
title: Send SLA and missing-document reminders
brd: [SNSRP-802 (p.20)]
actor: System (recipients - UCC, Investigator)
priority: Must have
fit: CHANGE
screens: Notifications; Case (Timeline)
api: Job SCR_SLA_MONITOR; event SCR_DOCUMENT_REMINDER
description: With each SLA check (FR-SS-044), BIBS also runs the document rules of FR-SS-060 in advisory mode. When required documents are missing and the case is inside its reminder lead or past its SLA, BIBS sends a reminder listing the missing documents to the assignee and the UCC.
preconditions:
  - "The case is open and a document rule applies to it."
main_flow:
  - The SLA monitor runs.
  - BIBS finds cases with missing documents inside the reminder lead or breached.
  - BIBS sends one reminder per case and stage entry.
rules:
  - [R1, "At most one document reminder per case per day.", Fixed, "-"]
validations: []
notifications:
  - "SCR_DOCUMENT_REMINDER to the assignee and the UCC."
audit:
  - "Reminder recorded on the timeline."
acceptance:
  - A case missing its KYC form one day before the SLA ends sends a reminder that names the KYC form.
```

```fr
id: FR-SS-082
title: Notify Compliance of newly added sanctioned names
brd: [p.8 "Notifications received - notifications on newly added sanctioned names"; SNSRP-201 (p.12)]
actor: System (recipients - Compliance Officers)
priority: Must have
fit: CHANGE
screens: Notifications; List Sources and Runs
api: Ingestion run completion
description: The to-be process lists "notifications on newly added sanctioned names" among the notifications to be received (p.8). When a run or an approved manual change adds entries, BIBS notifies the Compliance Officers with the source, the number of entries added and a link to the run.
preconditions:
  - "A run or approved change added at least one entry."
main_flow:
  - The run ends or the change is approved.
  - BIBS sends the notification with the counts and the link.
rules:
  - [R1, "One notification per run.", Fixed, "-"]
validations: []
notifications:
  - "This FR is the notification."
audit:
  - "Notification logged."
acceptance:
  - A run that adds 5 entries notifies the Compliance Officers with the count and the link to the run.
```

## Reports and audit

```fr
id: FR-SS-090
title: Operational and compliance reports
brd: [SNSRP-901 (p.20); p.6-8 reports produced]
actor: Compliance Officer; UCC; Operations Lead; Auditor
priority: Must have
fit: CHANGE
screens: Reports (category Compliance)
api: Report codes SCR-HIGH-RISK-CLIENTS, SCR-CASE-STATUS, SCR-SLA-BREACHES, SCR-SANCTIONED-NAMES, SCR-PEP-CLIENTS, SCR-INGEST-ERRORS, SCR-STR-REGISTER
description: Seven reports cover high-risk clients, case status, SLA breaches, sanctioned names, PEP clients, ingestion errors and the STR register (layouts in section 6). The common filters are date, marketing unit / Unit Head, disposition and case status. Every report exports to CSV, XLSX and PDF.
preconditions:
  - "The user has SCR_REPORT_VIEW."
main_flow:
  - The user selects the report and sets the filters.
  - BIBS generates the report.
  - The user exports it.
rules:
  - [R1, "Report columns are the project's proposal until BDOI confirms them (SQ17).", Configurable, Report definition]
validations:
  - [Date range invalid, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Each run and export logged with user, parameters and time."
acceptance:
  - The case status report filtered by marketing unit and case status lists only those cases.
  - Each report exports to CSV, XLSX and PDF with the same rows.
  - A user without SCR_REPORT_VIEW cannot run the reports.
```

```fr
id: FR-SS-091
title: Keep an immutable audit log
brd: [SNSRP-902 (p.21)]
actor: System
priority: Must have
fit: FIT
screens: Case (Timeline); Audit Trail
api: Audit trail service; insert-only case timeline, configuration differences and list changes
description: Every system and user action writes an audit entry that no user can alter or delete. The platform audit log is insert-only in the database. Screening adds structured, insert-only records - the case timeline, the configuration version differences and the list change records - which keep the before and after values. Records are retained 5 years online and 5 years in archive (p.25).
preconditions:
  - "None."
main_flow:
  - A user or the system performs an action.
  - BIBS writes the audit entry in the same transaction.
rules:
  - [R1, "Audit rows, timeline events and list change records are append-only; no user, including the System Administrator, can change or delete them.", Fixed, "-"]
  - [R2, "Retention - 5 years online and 5 years archive for cases, documents and list entries (retention rules SCREENING_CASE and WATCHLIST_ENTRY); longer AMLA retention is SQ18.", Configurable, Retention rules]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - No screen or service allows an audit row or timeline event to be edited or deleted.
  - A database update of a timeline row is refused.
```

```fr
id: FR-SS-092
title: Audit log report
brd: [SNSRP-903 (p.21); p.8 "Audit Reports"]
actor: Compliance Officer; Auditor
priority: Must have
fit: CHANGE
screens: Reports (SCR-AUDIT-LOG)
api: Report code SCR-AUDIT-LOG
description: The audit log report lists screening, configuration, list and case events with date, event, case or entry, from and to values, user and remarks. It has the case filters - date, marketing unit / Unit Head, disposition, status - and a user filter, updates when the filters change, and exports to CSV, XLSX and PDF.
preconditions:
  - "The user has SCR_AUDIT_VIEW."
main_flow:
  - The user opens the report and sets the filters.
  - BIBS generates the report.
  - The user exports it.
rules:
  - [R1, "Sources - case timeline, configuration differences, list changes and the platform audit log for screening entity types.", Fixed, "-"]
validations:
  - [Date range invalid, The end date must be on or after the start date, "-"]
notifications:
  - "None."
audit:
  - "Report runs logged."
acceptance:
  - Filtering by a disposition lists only the events of cases with that disposition.
  - The report exports to PDF and XLSX with the same rows.
```

# Workflow and status model

## Stages

Figure 2 shows the case workflow SCR_CASE. Solid arrows are the main path, dashed arrows are returns, and dotted arrows close a case without an STR.

![Workflow SCR_CASE: stages and actions](figures/brd10_case_workflow.dot){width=16}

<!-- table: widths=3.6,4.2,1.6,7.2 caption="Stages, owners and SLA" size=8 -->
| Stage | Owner (permission) | SLA hours (default) | Business actions |
|---|---|---|---|
| NEW | System | - | Route to INVESTIGATION with the assignee of the assignment matrix |
| INVESTIGATION | Investigator (SCR_INVESTIGATE) | 72 | Review, documents, risk tag, disposition, submit; request information |
| RETURNED | Investigator (SCR_INVESTIGATE) | 24 | Correct and resubmit |
| UNIT_HEAD_APPROVAL | Approver (SCR_CASE_APPROVE) | 24 | Approve, disapprove |
| COMPLIANCE_REVIEW | Compliance Officer (SCR_COMPLIANCE_REVIEW) | 24 | Escalate to committee, prepare STR, close, return for rework |
| AML_COMMITTEE | Committee members (SCR_COMMITTEE) | 72 | Record decisions; finalised by the committee rule |
| STR_PREPARATION | Compliance Officer (SCR_COMPLIANCE_REVIEW) | 48 | Prepare STR, check completeness, mark ready |
| STR_EXTRACTION | Compliance Officer (SCR_STR_EXTRACT) | 120 | Extract, record filing reference |
| CLOSED | - | terminal | Re-open with a reason |

The SLA hours are the workflow defaults. The SLA matrix (FR-SS-015) overrides them per stage, case type and risk category; BDOI supplies the values (SQ08).

## Transitions

<!-- table: widths=4,2.8,3.6,4.4,2.6 caption="Transitions of SCR_CASE" size=8 -->
| From | Action | To | Permission | Reason list |
|---|---|---|---|---|
| NEW | route (system) | INVESTIGATION | - | - |
| INVESTIGATION | submit | UNIT_HEAD_APPROVAL, or CLOSED when a route says so | SCR_INVESTIGATE | - |
| INVESTIGATION | request_info | INVESTIGATION | SCR_INVESTIGATE | - |
| RETURNED | resubmit | the stage that returned the case | SCR_INVESTIGATE | - |
| UNIT_HEAD_APPROVAL | approve | COMPLIANCE_REVIEW or CLOSED (by route) | SCR_CASE_APPROVE | - |
| UNIT_HEAD_APPROVAL | disapprove | RETURNED | SCR_CASE_APPROVE | RETURN_REASON |
| COMPLIANCE_REVIEW | escalate_committee | AML_COMMITTEE | SCR_COMPLIANCE_REVIEW | - |
| COMPLIANCE_REVIEW | prepare_str | STR_PREPARATION | SCR_COMPLIANCE_REVIEW | - |
| COMPLIANCE_REVIEW | close | CLOSED | SCR_COMPLIANCE_REVIEW | - |
| COMPLIANCE_REVIEW | return_for_rework | RETURNED | SCR_COMPLIANCE_REVIEW | RETURN_REASON |
| AML_COMMITTEE | finalise (system) | STR_PREPARATION / CLOSED / COMPLIANCE_REVIEW | SCR_COMMITTEE (votes) | - |
| STR_PREPARATION | str_ready | STR_EXTRACTION | SCR_COMPLIANCE_REVIEW | - |
| STR_EXTRACTION | filed | CLOSED | SCR_STR_EXTRACT | - |
| CLOSED | reopen | INVESTIGATION | SCR_COMPLIANCE_REVIEW | RETURN_REASON |
| Any open stage | re-assign | same stage | SCR_CASE_ASSIGN | SCR_REASSIGN_REASON |

## Configuration versions and list changes

Configuration versions and list changes do not use the case workflow. They follow maker-checker states (Figure 3) and appear in My Approvals.

![Maker-checker states of configuration versions and list changes (SNSRP-101 to 109, 203, 204)](figures/brd10_config_states.dot){width=17}

<!-- table: widths=3.2,6.6,7 caption="Configuration version states" -->
| State | Meaning | Used by screening |
|---|---|---|
| DRAFT | Being edited by the maker | No |
| PENDING | Submitted; waiting for the checker | No |
| ACTIVE | Approved; in force from its effective date | Yes, for new runs, cases and reviews |
| SUPERSEDED | Replaced by a newer active version | Only by cases and reviews started on it |
| REJECTED | Rejected with a reason, or withdrawn by the maker | No |

## STR status

![STR status (SNSRP-705, 706)](figures/brd10_str_states.dot){width=8}

<!-- table: widths=3.2,13.4 caption="STR statuses" -->
| Status | Meaning |
|---|---|
| DRAFT | Prefilled from the case; editable by the Compliance Officer |
| FOR_APPROVAL | Complete; marked ready |
| APPROVED | The case has an AML Committee APPROVE_STR decision; eligible for extraction |
| EXTRACTED | Included in an AMLC extraction file |
| FILED | Filed on the AMLC portal; AMLC reference and filing date recorded |

# Reports and documents

## Reports

<!-- table: widths=4.4,4.4,5.2,2.6 caption="Sanction Screening reports (category Compliance)" -->
| Code | Name | Purpose | BRD |
|---|---|---|---|
| SCR-HIGH-RISK-CLIENTS | High-risk Clients | High-risk, PEP and watchlist-tagged clients with their open cases | p.8; capability 4 |
| SCR-CASE-STATUS | Case Status Monitoring | Cases by stage, assignee, age and SLA state | p.8; SNSRP-901 |
| SCR-SLA-BREACHES | SLA Reminders and Breaches | Reminders, breaches and escalations | SNSRP-405, 108 |
| SCR-SANCTIONED-NAMES | List of Sanctioned Names | Active entries by source and list type; added and delisted in the period | p.6 |
| SCR-PEP-CLIENTS | List of Approved PEP Clients | Clients tagged PEP after review | p.7 |
| SCR-INGEST-ERRORS | Unsuccessful Ingestion Records | Failed records by run and source | SNSRP-202 |
| SCR-STR-REGISTER | STR Register | STRs drafted, approved, extracted and filed, with AMLC reference | SNSRP-705, 706 |
| SCR-AUDIT-LOG | Screening Audit Log | Screening, configuration, list and case events | p.8; SNSRP-902, 903 |

All reports need SCR_REPORT_VIEW (SCR-AUDIT-LOG needs SCR_AUDIT_VIEW), export to CSV, XLSX and PDF, and print the parameters, the user and the time generated in the header. The columns below are the project's proposal until BDOI confirms the layouts (SQ17).

### High-risk Clients (SCR-HIGH-RISK-CLIENTS)

Parameters: As-of Date; Risk Category; Marketing Unit; Unit Head; Client Type. Layout: landscape, sorted by risk tier then client name.

<!-- table: widths=3.6,3,10 caption="SCR-HIGH-RISK-CLIENTS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Client Code / Name | Text | Client master |
| Client Type | Text | Individual or corporate |
| Risk Category | Text | Category of the last tagging |
| Risk Rating | Text | KYC_RISK_RATING value |
| Tags | Text | PEP, WATCHLIST_REVIEW |
| Tagged On / Source | Date / Text | Date and source (rule or manual) of the last change |
| Open Case | Text | Case number and stage, or "None" |
| Active Policy | Text | Yes / No |
| Marketing Unit / Unit Head | Text | Sales organisation of the client |

### Case Status Monitoring (SCR-CASE-STATUS)

Parameters: Date Range (created); Marketing Unit / Unit Head; Disposition; Case Status; Case Type. Layout: landscape, grouped by stage.

<!-- table: widths=3.6,3,10 caption="SCR-CASE-STATUS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Case No. | Text | SCR-yyyy-nnnnnn |
| Client | Text | Name and code |
| Case Type / Risk Category | Text | - |
| Stage | Text | Current stage |
| Assignee | Text | Current assignee |
| Created | Date | Case creation date |
| Days in Stage | Number | Days since stage entry |
| Due | Date-time | Stage due time |
| SLA State | Text | On time, due soon, breached |
| Disposition | Text | Last disposition |
| Marketing Unit / Unit Head | Text | - |

### SLA Reminders and Breaches (SCR-SLA-BREACHES)

Parameters: Date Range; Stage; Team. Layout: portrait, sorted by date.

<!-- table: widths=3.6,3,10 caption="SCR-SLA-BREACHES columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Date / Time | Date-time | Time of the reminder, breach or escalation |
| Event | Text | Reminder, breach, escalation |
| Case No. | Text | - |
| Stage | Text | Stage at the event |
| Assignee | Text | - |
| SLA Hours / Due | Number / Date-time | From the SLA matrix |
| Escalated To | Text | Role or user notified |

### List of Sanctioned Names (SCR-SANCTIONED-NAMES)

Parameters: Source; List Type; Status; Date Range (listed or delisted). Layout: landscape, sorted by name.

<!-- table: widths=3.6,3,10 caption="SCR-SANCTIONED-NAMES columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Source / List Type | Text | - |
| External Reference | Text | Reference in the source list |
| Name | Text | Primary name |
| Aliases | Text | All aliases |
| Entity Type | Text | Individual or entity |
| Birth Date / Nationality | Date / Text | - |
| Listed On / Delisted On | Date | - |
| Status | Text | ACTIVE, INACTIVE |
| Last Change | Text | Run or manual change, with date |

### List of Approved PEP Clients (SCR-PEP-CLIENTS)

Parameters: As-of Date; Marketing Unit. Layout: portrait, sorted by client name.

<!-- table: widths=3.6,3,10 caption="SCR-PEP-CLIENTS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Client Code / Name | Text | - |
| PEP Since | Date | Date the PEP tag was set |
| Matched Entry | Text | PEP list entry and source |
| Case No. / Outcome | Text | Case that confirmed the PEP status |
| EDD Required / Last EDD | Text / Date | From the risk category and the last EDD review |
| Marketing Unit / Account Officer | Text | - |

### Unsuccessful Ingestion Records (SCR-INGEST-ERRORS)

Parameters: Run; Source; Date Range. Layout: landscape, sorted by run and line.

<!-- table: widths=3.6,3,10 caption="SCR-INGEST-ERRORS columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Run Date / Run No. | Date / Text | - |
| Source | Text | Source code and name |
| File | Text | File name |
| Line | Number | Line in the file |
| Record | Text | Raw record (truncated to 200 characters) |
| Reason | Text | Validation failure |

### STR Register (SCR-STR-REGISTER)

Parameters: Date Range; Status. Layout: landscape, sorted by STR number.

<!-- table: widths=3.6,3,10 caption="SCR-STR-REGISTER columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| STR No. / Case No. | Text | - |
| Subject | Text | Client name |
| Status | Text | DRAFT to FILED |
| Committee Decision / Date | Text / Date | APPROVE_STR and date |
| Extraction Batch / Date | Text / Date | - |
| AMLC Reference / Filed On | Text / Date | - |
| Prepared By | Text | - |

### Screening Audit Log (SCR-AUDIT-LOG)

Parameters: Date Range; Marketing Unit / Unit Head; Disposition; Status; User. Layout: landscape, sorted by date and time.

<!-- table: widths=3.6,3,10 caption="SCR-AUDIT-LOG columns" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Date / Time | Date-time | - |
| Area | Text | Case, configuration, watchlist, screening run |
| Reference | Text | Case no., version no., entry reference or run no. |
| Event | Text | For example ASSIGNED, APPROVED, ACTIVATED |
| From / To | Text | Before and after values |
| Reason / Remarks | Text | - |
| User | Text | User ID |

## Documents

Screening produces two outputs in addition to the reports. Both come from versioned configuration.

<!-- table: widths=3.6,3.4,10 caption="Sanction Screening outputs" size=8.5 -->
| Output | Format | Content |
|---|---|---|
| STR (on screen and PDF) | PDF | STR number; subject party; transactions; reason codes; narrative; attachments list; committee decision; prepared by and date; template version |
| AMLC extraction file | Per STR layout (CSV, fixed width, XLSX or XML) | One row per committee-approved STR, columns and codes of the active STR layout; placeholder layout until SQ09 |

# Interfaces and integration

Figure 5 shows the interfaces of Sanction Screening. Screening reads the client master and accounts and writes the client risk rating and tags only through the client master's service.

![Interfaces of Sanction Screening (dashed = parked)](figures/brd10_integration.dot){width=15}

<!-- table: widths=3.8,1.8,7.4,2.4,2.4 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| List files (AML advisory, NLDS-PEP) | In | CSV / XLSX by upload or file drop, on schedule | SNSRP-201 | DESIGNED |
| List feeds by API; direct NLDS query | In | Real-time or scheduled feed from list providers or BDO NLDS | SNSRP-201 | PARKED |
| Client master | In / Out | In - client registered and identity changed events, client data. Out - risk rating, PEP and watchlist tags, KYC review date | SNSRP-302, 304, 602 | DESIGNED |
| Accounts (BRD-1) | In | Account submitted event; active policy (accounts POLICY_ISSUED / BOOKED) | SNSRP-303 | DESIGNED |
| Attachments and client KYC documents | Out | Case documents, KYC documents registered on the client | SNSRP-601 | DESIGNED |
| Notifications, e-mail and alerts | Out | Case, SLA, document, ingestion and list notices | SNSRP-202, 801, 802 | DESIGNED |
| Report archive | Out | STR extraction file for download | SNSRP-706 | DESIGNED |
| Designated STR folder (shared drive / SFTP) | Out | Automatic saving of the extraction file | SNSRP-706 | PARKED |
| AMLC portal | Out | STR filing; manual, outside BIBS | p.8 | OUT |

> [!PARKED] Parked seams
> The list transports (SQ01), the designated STR folder (SQ16), the AMLC file format (SQ09) and the definition of an active policy (SQ10) are not in the BRD. BIBS builds each as a replaceable adapter with a working default - file upload, report archive, placeholder layout and booked accounts. Adding the final transport or format is configuration or a new adapter, with no change to the screening workflow.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.22-25)" status=Status size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Compliance Officer 2 / 2; Checker 1 / 1; Investigator 287 / 287 (risk tags, uploads) and 120 / 120 (case review); Operations Lead 1 / 1; Unit Head 8 / 8; CO BU escalation and STR 2 / 2; AML Committee 5 / 5; reports 3 / 3; Auditor 2 / 2; System Admin 3 / 1 | Under 300 named users, within the BIBS sizing. 287 "concurrent" investigators is read as the named population (SQ19) | CONFIGURE |
| Volumes | Configuration and approvals 10 a month; list intake and matching 10 to 300 a day; cases, reviews, uploads, approvals, escalations 1 to 15 a week; STR 1 to 5 a week; reports 10 a month; audit reports 20 a year; 20% growth a year | Delta screening per list change; full re-screen monthly in the batch window | FIT |
| Response time | 3 to 5 seconds per screen; not applicable to system jobs | Online p95 under 3 seconds; matching, ingestion and extraction run asynchronously | FIT |
| Peak | End of month and year-end; 08:00-17:00 daily | Jobs run from 01:00, outside the peak | FIT |
| Devices | Same speed on mobile and desktop | Responsive screens | FIT |
| Availability | 99.9%; use 08:00-18:00 Monday to Friday; downtime at most 45 minutes a month, planned only; maintenance 00:00-04:00; BCP threshold under 3 days | Same deployment as the rest of BIBS; the service-hours variants are consolidated in one BIBS-wide NFR set (XQ08) | CONFIGURE |
| Retention | Transaction records, KYC and supporting documents - 5 years online, 5 years archive, daily backup, daily accessibility, 5-year backup retention | Retention rules SCREENING_CASE and WATCHLIST_ENTRY (5 / 5); archive and purge follow the BIBS retention decision (Q39) | CHANGE |
| Anonymisation | None | None | FIT |
| Security | Maker-checker, immutable audit (SNSRP-109, 204, 902) | Role-based access, four-eyes rules, insert-only audit (FR-SS-001, 019, 023, 091) | FIT |
| Regulatory | RA 9160 AMLA, BSP Circular 1182 (2023), BSP CL-2023-030, BSP M-2025-017, IC CL 2019-65 | Controls above; adverse-media screening not in scope (SQ20) | FIT |

# Configuration items

The items below are changed in BIBS without a release. Screening rules are maintained by Compliance as configuration versions; parameters and lists by the System Administrator or the Business Administrator. All changes are audited.

## Parameters and jobs

<!-- table: widths=6.2,3.2,7.2 caption="Sanction Screening parameters and jobs" size=8.5 -->
| Parameter / job | Default | Meaning |
|---|---|---|
| SCR_SCREENING_SCOPE | PROSPECT, CONFIRMED | Client statuses screened in the batch window (SQ11) |
| SCR_FULL_RESCREEN_DAY | 1 | Day of the month of the full re-screen |
| SCR_BLOCK_ON_OPEN_MATCH | false | Optional block on account submission and placement while a match is open (SQ07) |
| SCR_COMMITTEE_RULE | MAJORITY | AML Committee decision rule - ANY, MAJORITY, ALL (SQ15) |
| SCR_COMMITTEE_SIZE | 5 | Committee members counted by MAJORITY and ALL |
| SCR_INGEST_ALERT_RECIPIENTS | empty | E-mail recipients of the failed-records digest (SQ01) |
| SCR_CASE_SEQUENCE_PREFIX | SCR | Prefix of case numbers (SCR-yyyy-nnnnnn) |
| Job SCR_WATCHLIST_INGEST | 01:00 daily | Reads the list sources |
| Job SCR_PERIODIC_SCREENING | 01:30 daily | Delta screening; full re-screen on SCR_FULL_RESCREEN_DAY |
| Job SCR_SLA_MONITOR | hourly | Reminders, breaches, escalations, document reminders |
| Job SCR_INGEST_ERROR_DIGEST | 07:00 Monday to Friday | E-mail of failed ingestion records |

## Lists of values

<!-- table: widths=5.4,11.2 caption="Lists of values (delivered values to be confirmed, SQ06)" size=8.5 -->
| List | Values delivered |
|---|---|
| SCR_DISPOSITION (parent = stage) | INVESTIGATION - False positive; True match, for review; Possible match, EDD; Need more information. UNIT_HEAD_APPROVAL - Concur; Not concur. COMPLIANCE_REVIEW - Close, no action; Escalate to committee; For STR; Return. AML_COMMITTEE - Approve STR; No STR; Return |
| SCR_CASE_TYPE | Name match; PEP; High-risk; EDD; Monitor (no active policy); Account application |
| SCR_LIST_TYPE | Sanction; PEP; Internal; Adverse media (manual only) |
| SCR_REASSIGN_REASON | Workload; Absence; Conflict of interest; Others |
| SCR_STR_REASON | Empty until BDOI supplies the AMLC reason codes (SQ09) |
| SCR_FORM_TYPE | KYC review; Transaction review; EDD; STR |
| SCR_DOCUMENT_TYPE | Screening document types under the platform DOCUMENT_TYPE list |
| KYC_RISK_RATING, CLIENT_TAG (client master) | LOW, STANDARD, HIGH; PEP, WATCHLIST_REVIEW (existing) |

## Configuration maintained by Compliance

<!-- table: widths=4.6,4.6,7.4 caption="Configuration and masters" size=8.5 -->
| Item | Maintained by (approved by) | FR |
|---|---|---|
| Matching criteria | Compliance Officer (Compliance Checker) | FR-SS-011 |
| Risk categories and rules | Compliance Officer (Compliance Checker) | FR-SS-012 |
| Approval and escalation matrix | Compliance Officer (Compliance Checker) | FR-SS-013 |
| Assignment matrix | Compliance Officer (Compliance Checker) | FR-SS-014 |
| SLA matrix | Compliance Officer (Compliance Checker) | FR-SS-015 |
| Review templates (KYC, transaction, EDD) | Compliance Officer (Compliance Checker) | FR-SS-016 |
| STR template and extraction layout | Compliance Officer (Compliance Checker) | FR-SS-017 |
| Validation rules | Compliance Officer (Compliance Checker) | FR-SS-060 |
| Dispositions per stage | Business Administrator (LOV approver) | FR-SS-018 |
| List sources and entries | Compliance Officer (Compliance Checker for entries) | FR-SS-020, 022, 023 |
| Roles and permissions | Business Administrator via BRD-11 group-profile request | FR-SS-001 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-SS-01 | The BRD approved on 16 to 17-Apr-2026 (29 pages) is the baseline | R1 |
| A-SS-02 | Filing on the AMLC portal stays outside BIBS; BIBS extracts the file and records the reference | p.8 |
| A-SS-03 | Screening informs and does not block business; a block is built only if BDOI asks | SQ07 |
| A-SS-04 | EDD is a review template type used by risk categories that require it | p.4-5; SNSRP-303 |
| A-SS-05 | SNSRP-602, printed under capability 6, is the matching trigger of capability 3 | R2 section 11 |
| A-SS-06 | The Approver is the Unit Head of the client's marketing unit | SQ04 |
| A-SS-07 | The AML Committee decides by majority of five members until BDOI states the rule | SQ15 |
| A-SS-08 | A client with an active policy is a client with an account in POLICY_ISSUED or BOOKED | SQ10 |
| A-SS-09 | BDOIR in the BRD and BDOI in the other BRDs are the same company | R2 section 11 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-SS-01 | BDOI supplies the list sources, file layouts and transport | FR-SS-020, 021 (SQ01) |
| D-SS-02 | Compliance supplies thresholds, risk categories, matrices, SLAs and dispositions before go-live | FR-SS-011 to 018 (SQ02 to SQ08) |
| D-SS-03 | BDOI supplies the template field lists and the AMLC STR format | FR-SS-016, 017, 070, 071 (SQ05, SQ09) |
| D-SS-04 | The client master publishes the client registered and identity changed events and offers the risk-profile service | FR-SS-030, 033 |
| D-SS-05 | The unit-head look-up of the sales organisation is available | FR-SS-013 |
| D-SS-06 | User access and the new roles are granted through BRD-11 | FR-SS-001 (R7) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-10 (R2 section 10; status from R4)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| SQ01 | Which lists are screened (AML advisory source, NLDS-PEP, UN / AMLC, OFAC); format, frequency, transport; who may upload; recipients and schedule of the failed-record alert | FR-SS-020, 021 | OPEN |
| SQ02 | Initial thresholds per method; fields compared; alias hits; corporate names | FR-SS-011, 031 | OPEN |
| SQ03 | Risk-profile categories, criteria and ratings; which require EDD; High-risk equals rating HIGH | FR-SS-012, 033 | OPEN |
| SQ04 | Dimensions of the approval and assignment matrices; who the Approver is | FR-SS-013, 014 | OPEN |
| SQ05 | Field lists of the KYC review, transaction review, EDD and STR templates | FR-SS-016, 050, 070 | OPEN |
| SQ06 | Dispositions per stage and validation criteria before approval | FR-SS-018, 060 | OPEN |
| SQ07 | Block or warn on quotation, account submission, placement or booking while a match is open | FR-SS-033 | OPEN |
| SQ08 | SLA hours per stage, reminder lead, escalation targets, working or calendar hours | FR-SS-015, 044 | OPEN |
| SQ09 | AMLC STR format; covered transactions in scope; who approves the STR | FR-SS-017, 070, 071 | OPEN |
| SQ10 | Definition of a client with an active policy | FR-SS-034 | OPEN |
| SQ11 | Clients in scope of the periodic run; batch window; other parties | FR-SS-030 | OPEN |
| SQ12 | False-positive suppression until the entry changes, or re-screen each run | FR-SS-035 | OPEN |
| SQ13 | Who may re-assign cases | FR-SS-043 | OPEN |
| SQ14 | Meaning of "Form Type" in the naming convention | FR-SS-052 | OPEN |
| SQ15 | AML Committee decision rule and voting | FR-SS-064 | OPEN |
| SQ16 | Designated folder for the STR file | FR-SS-071 | OPEN |
| SQ17 | Columns of the status monitoring, high-risk and audit reports | FR-SS-090, 092 | OPEN |
| SQ18 | Audit retention - 5 + 5 years or AMLA record keeping | FR-SS-091 | OPEN |
| SQ19 | Who the 287 investigators are; existing roles; load target | Section 3, 8 | OPEN |
| SQ20 | Adverse-media screening in scope | Section 1.2 | OPEN |

BRD-10 also answers questions raised on other BRDs, in part: Q18 (tags trigger reviews, not blocks), Q21 (risk categories in the system; KYC review or EDD for high-risk and PEP clients with an active policy), Q23 (naming syntax for screening documents) and Q39 (screening retention 5 + 5 years) (R4).

# Traceability

Every BRD-10 requirement is met by at least one FR. The screen column names the main entry point.

<!-- table: widths=2.4,1.6,4.2,5.4,2.8 caption="BRD ID to FR and screen" status=Fit size=8 -->
| BRD ID | Page | FR | Screen | Fit |
|---|---|---|---|---|
| SNSRP-101 | p.10 | FR-SS-010, FR-SS-011 | Configuration Versions (Matching Criteria) | NEW |
| SNSRP-102 | p.10 | FR-SS-010, FR-SS-012 | Configuration Versions (Risk Rules) | CHANGE |
| SNSRP-103 | p.10 | FR-SS-010, FR-SS-013 | Configuration Versions (Approval Matrix) | CHANGE |
| SNSRP-104 | p.10-11 | FR-SS-010, FR-SS-016 | Templates | NEW |
| SNSRP-105 | p.11 | FR-SS-010, FR-SS-017 | Templates (STR); STR Layout | NEW |
| SNSRP-106 | p.11 | FR-SS-010, FR-SS-014 | Configuration Versions (Assignment Matrix) | CHANGE |
| SNSRP-107 | p.11-12 | FR-SS-018 | Lists of Values (SCR_DISPOSITION) | CONFIGURE |
| SNSRP-108 | p.12 | FR-SS-010, FR-SS-015, FR-SS-044 | Configuration Versions (SLA Matrix) | CHANGE |
| SNSRP-109 | p.12 | FR-SS-019 | My Approvals; Configuration Versions | CHANGE |
| SNSRP-201 | p.12 | FR-SS-020, FR-SS-082 | List Sources and Runs | NEW |
| SNSRP-202 | p.13 | FR-SS-021 | List Sources and Runs; Reports | NEW |
| SNSRP-203 | p.13 | FR-SS-022, FR-SS-001 | Watchlist | NEW |
| SNSRP-204 | p.13 | FR-SS-023 | My Approvals; Watchlist | CHANGE |
| SNSRP-301 | p.14 | FR-SS-031, FR-SS-032 | Matches | NEW |
| SNSRP-302 | p.14 | FR-SS-033 | Client page (Screening tab) | CHANGE |
| SNSRP-303 | p.14 | FR-SS-034, FR-SS-030 | Cases | NEW |
| SNSRP-304 | p.14 | FR-SS-035, FR-SS-032 | Case; Matches | CHANGE |
| SNSRP-401 | p.15 | FR-SS-040 | Case (workflow panel, Timeline) | CHANGE |
| SNSRP-402 | p.15-16 | FR-SS-041, FR-SS-045 | Cases; Case; Screening Home | CHANGE |
| SNSRP-403 | p.16 | FR-SS-042, FR-SS-001 | Cases (search) | CHANGE |
| SNSRP-404 | p.16 | FR-SS-043 | Case (Re-assign) | CHANGE |
| SNSRP-405 | p.17 | FR-SS-044, FR-SS-045 | Screening Home; Cases | CHANGE |
| SNSRP-501 | p.17 | FR-SS-050 | Case (Review) | NEW |
| SNSRP-502 | p.17 | FR-SS-051 | Case (workflow panel) | CHANGE |
| SNSRP-601 | p.17 | FR-SS-052 | Case (Documents) | CHANGE |
| SNSRP-602 | p.14-15 | FR-SS-030 | Screening Home (runs); client page | NEW |
| SNSRP-701 | p.17 | FR-SS-060 | Case (validation messages) | NEW |
| SNSRP-702 | p.18 | FR-SS-061, FR-SS-062 | Cases (For Approval); Case | CHANGE |
| SNSRP-703 | p.18 | FR-SS-063, FR-SS-013, FR-SS-062 | Cases (Compliance Review); Case | CHANGE |
| SNSRP-704 | p.18-19 | FR-SS-064 | Cases (Committee); Case (Decisions) | NEW |
| SNSRP-705 | p.19 | FR-SS-070 | Case (STR) | NEW |
| SNSRP-706 | p.19-20 | FR-SS-071, FR-SS-072 | STR | NEW |
| SNSRP-801 | p.20 | FR-SS-080 | Notifications | CONFIGURE |
| SNSRP-802 | p.20 | FR-SS-081 | Notifications | CHANGE |
| SNSRP-901 | p.20 | FR-SS-090 | Reports | CHANGE |
| SNSRP-902 | p.21 | FR-SS-091 | Case (Timeline); Audit Trail | FIT |
| SNSRP-903 | p.21 | FR-SS-092 | Reports (SCR-AUDIT-LOG) | CHANGE |

The BRD's process sections add three items without a requirement ID, also covered: the high-risk client list (p.8, FR-SS-045), notifications on newly added sanctioned names (p.8, FR-SS-082) and the manual AMLC filing (p.8, FR-SS-072). The non-functional requirements of pp.22-25 are in section 8.

# Sign-off

By signing, BDOI confirms that this FRS describes the Sanction Screening and Risk Profiling functions it expects in BIBS, and accepts the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner, Marketing Business System", organisation: BDOI}
  - {name: "", role: Chief Compliance Officer, organisation: BDOI}
  - {name: "", role: "Unit Head, Claims and Risk Management", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
