---
# Source of the Functional Requirements Specification for BRD-5, Volume 1 (FRBS and Accounting).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD05_ACCOUNTING_VOL1.md
title: Accounting (FRBS) - Volume 1
subtitle: BRD-5 Accounting, Disbursement, Accounting Controls and Subsidiary Ledger - Volume 1 - Financial Reporting (FRBS), General Ledger and Business / System Administration
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-05
name: Accounting Disbursement ACSL Vol1
doc_id: BIBS-FRS-BRD-05-V1
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-5 Volume 1 - FRBS and Accounting
output: FRS/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-5 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue for BDOI review; aligned with the as-built GL platform (A1-GL), report pack and service fee (A1-FRBS) and the remittance and booking changes (A1-OPSX)
distribution:
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Financial Reporting and Budget Section (FRBS / GL team)", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Accounting Controls and Subsidiary Ledger (ACSL)", role: Business user, organisation: BDOI, purpose: "Review of the posting, closing and report FRs"}
  - {name: "Business Administrator and System Administrator", role: Business user, organisation: BDOI, purpose: Review of the administration FRs}
  - {name: "Marketing (Retail, CBG, IBG)", role: Business user, organisation: BDOI, purpose: Review of the service fee FRs}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Accounting requirements of BRD-5 for BDO Insurance and Reinsurance Brokers, Inc. (BDOI): the Financial Reporting and Budget Section (FRBS, the GL team) and the business and system administration of the BRD. It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

The Accounting functions are built. Where the delivered behaviour differs from the BRD text, the FR describes the delivered behaviour and records the difference in a note; section 1.7 lists all differences in one table. The remittance and booking changes of the wave A1-OPSX (CPC2, early-incentive service invoice, remittance deductions) are built and described in Volume 2.

## Scope

BRD-5 is one pack with four source documents: Accounting (FRBS), Disbursement, Marketing refund and cash-advance requests (Payment Requests) and Accounting Controls and Subsidiary Ledger (ACSL), plus business and system administration. This volume covers FRBS and administration.

<!-- table: widths=4,9,4 caption="Scope of Volume 1" -->
| Area | In scope | Source |
|---|---|---|
| Access | Access from BDO devices, log-in with a profile, idle and log-out warnings (Accounting and administration) | FRBS 1.1.0-1.1.3; BASAU 1.1.0-1.1.3 |
| Chart of accounts and rates | Chart set-up, upload, account numbering, search by short code, monthly revaluation rate | FRBS 2.2.0, 2.3.0-2.3.5, 3.6.0 |
| Reports on demand | Report selection, date range, column filters, batches of reports, print options | FRBS 2.4.0-2.4.10 |
| Posting and manual entries | Posting to GL and sub-ledger, assignment, validation, negative-balance control, bulk posting, return, accrual auto-reversal | FRBS 2.5.0-2.5.10, 2.8.0-2.8.5, 2.9.0, 3.1.0, 3.6.0b |
| Closing | Scheduled month-end GL close, broking-books cut-off, year-end close and verification, USD revaluation | FRBS 2.6.0-2.7.1, 3.4.0-3.5.0 |
| Bank reconciliation | Bank file upload, check-number matching, unmatched report | FRBS 3.3.0-3.3.3 |
| Service fee | Service-fee runs, payout, release and liquidation tags | FRBS 2.10.0-2.10.2 |
| Cost centres and incentive income | Cost-centre derivation rules; early incentives as Other Income | FRBS 3.1.1, 3.1.2 |
| Report pack | Appendix A report families - end of day, GARD, subsidiaries, schedules and aging, Mancom, service fee, government | FRBS 3.2.0; Appendix A |
| Business and system administration | Lists of values with approval, user and role management through requests, approvals and returns | BASAU 2.2.0-2.6.3 |

**Out of scope for this volume:** Disbursement, Payment Requests and ACSL (Volume 2). Payroll outputs of Appendix A VII (HDMF, SSS, PhilHealth, 1601-C, 1604-C) are out of scope because BIBS has no payroll (AQ06). BDO Business Online Banking, eFPS / eBIRForms filing and the transmission of GARD reports to BDO Unibank are external channels; BIBS produces the files and reports.

## Volumes of this FRS

BRD-5 has 277 requirement IDs. One document covering all of them at the level of detail of the reference FRS would exceed 200 pages, so the FRS is issued in two volumes with a shared cover note.

<!-- table: widths=3.4,6.8,4.2,2.2 caption="Volumes of the BRD-5 FRS" -->
| Document | Content | BRD IDs | FR prefix |
|---|---|---|---|
| Cover note | Why two volumes; how to read them; common references; sign-off route | - | - |
| Volume 1 (this document) | FRBS (Accounting), General Ledger platform, report pack, service fee, business and system administration | FRBS 60, BASAU 25 (85) | FR-AC |
| Volume 2 | Disbursement, Payment Requests (Marketing refund, cash advance, check cancellation), ACSL | DIS 111, MKT 36, ACSL 45 (192) | FR-DS, FR-PQ, FR-AS |

Each volume has its own traceability chapter. Cross-references to the other volume name the FR (for example "FR-DS-041, Volume 2").

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Accounting, Disbursement and ACSL BRD (main BRD), pages 40-153 of the BRD-5 pack; signed scan pp.154-267 | v1.0, 23-Jul-2025; approved Jul to Oct 2025 | `docs/source-documents/Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip` |
| R2 | Addendum 1 "Accounting, Disbursement and ACSL - Addendum", pages 28-39 (signed copy pp.16-27) | v1.0, 18-Dec-2025; signed 13-Jan-2026 | same file |
| R3 | Addendum 2 "Financial Reporting, Disbursement and ACSL - Addendum (Workshop)", pages 1-15 (scanned) | v1.0, 10-Apr-2026; signed 8 to 15-Apr-2026 | same file |
| R4 | BDOI Accounting, Disbursement and ACSL (BRD-5) requirements baseline and fit/gap | current | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| R5 | Accounting, Disbursement and ACSL build design, including section 17 and the as-built notes A1-GL, A1-PRQ, A1-DSB, A1-FRBS | current | `docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R7 | BRD-5 FRS Volume 2 (Disbursement, Payment Requests, ACSL) | v1.0 | `docs/deliverables/out/FRS/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx` |
| R8 | BRD-2 Operations FRS (receipts, remittance, commission) | v1.0 | `docs/deliverables/out/FRS/BIBS_FRS_BRD-02_Operations_v1.0.docx` |

Page references ("p.51") are pages of the BRD-5 PDF. "Add.1" is Addendum 1 (pp.28-39) and "Add.2" is the Workshop Addendum (pp.1-15). The BRD numbers the Accounting access rows "BRD 1.1.0-1.1.3"; this FRS writes them FRBS 1.1.0-1.1.3. The second row numbered FRBS 3.6.0 (item q, p.66) is written FRBS 3.6.0b.

## Definitions and acronyms

```glossary
ACSL: Accounting Controls and Subsidiary Ledger unit of Comptrollership
AQnn: Open question on BRD-5 raised by the project team (section 10.3)
ATC: Alphanumeric tax code of the BIR
BASAU: Requirement ID prefix of the business and system administration rows of BRD-5
BIR: Bureau of Internal Revenue
BOB: BDO Business Online Banking
BOOK rate: Exchange rate at which USD Operations documents are booked
Broking books: The postings of the broking and Operations modules (booking, receipts, remittance, adjustment, commission, disbursement), closed by the system at month end
CLOSING rate: Month-end exchange rate used to revalue USD balances; the BRD's monthly revaluation rate
Cost centre: Responsibility centre of an expense or income line (dimension of the journal line)
DV: Disbursement voucher (Volume 2)
FRBS: Financial Reporting and Budget Section (the GL team of Comptrollership); also the requirement ID prefix of the Accounting rows
GARD: BDO Unibank reporting format for subsidiaries (Appendix A II)
GL: General ledger
GL books: The general ledger closed by FRBS on a chosen date after the broking books
Mancom: Management committee reports (Appendix A V)
Proforma entry: The accounting entry proposed by the rules before approval
SAWT: Summary Alphalist of Withholding Taxes
Schedule engine: Configurable account-schedule report of BIBS (report GL-SCHEDULE)
Service fee: Referrers' share of fully paid commission (Appendix A VI, p.144)
Short code: Unique short key of a GL account used for entry and search
SL: Sub-ledger (open items per party)
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R4), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the business or the System Administrator in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check; its message has no business code. Text in angle brackets (`<account>`) is replaced by the value.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**, the basis of the BRD-5 test cases.

Every BRD-5 row carries the priority **Must have** in the BRD. API paths start with `/api/v1`.

> [!NOTE]
> The real chart of accounts, the accounting rules, the report layouts and several rates are BDOI data that have not been given (AQ01-AQ07, AQ20, AQ26). BIBS holds them as configuration with demo values, so a BDOI answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Worked with the finance platform before BRD-5 |
| CONFIGURE | Needed set-up only (parameters, rules, report definitions) |
| CHANGE | Extended an existing capability |
| NEW | A capability that did not exist before BRD-5 |
| OUT | Out of scope per the BRD |

## Differences between the built behaviour and the BRD

<!-- table: widths=2.2,5.4,6.8,2.2 caption="Recorded differences (built behaviour against the BRD)" size=8.5 -->
| BRD ID | BRD says | BIBS does | Ref. |
|---|---|---|---|
| FRBS 2.2.0 | Input the revaluation rate monthly | The monthly revaluation rate is the CLOSING rate of the month end; a job copies it as the BOOK rate of the next month (OPS_BOOK_RATE_SOURCE) | AQ03, OQ08 |
| FRBS 2.3.2 | The account number is generated by the system | A number is proposed from the numbering scheme of the parent when the code is left blank; a user may still key the code | AQ01 |
| FRBS 2.5.4, 2.8.4 | Prevent posting with a negative balance | Each account has a policy ALLOW, WARN or BLOCK; only BLOCK accounts refuse the journal; the accounts that may never be negative are to be named by BDOI | AQ30 |
| FRBS 2.5.7 | Return a posting request | The journal action "Reject" is labelled "Return to Maker"; a returned journal is edited and resubmitted | - |
| FRBS 2.6.0 (Add.1) | Close the previous month's GL books on a preferred date and time | FRBS schedules the close (proposal: 2nd banking day, 17:00); the job runs the checklist and closes, or records the failure | AQ04 |
| FRBS 2.8.1 | The reversal posts in the GL and the sub-ledger | The reversal posts in the GL; manual journals record no sub-ledger open items, so there is none to reverse | - |
| FRBS 3.4.0-3.4.1 | Close the broking books automatically | The broking books close on the last day of the month at 23:00 Manila; the list of pending broking items is empty until the broking modules report them | AQ04 |
| FRBS 3.5.0 | Revalue daily ("previous date versus current date") | Revaluation runs per period at the CLOSING rate with automatic reversal; daily revaluation only if BDOI confirms | AQ03 |
| FRBS 3.1.2 | Early incentives as Other Income | The incentive component posts to its own account; moving 4130 under Other Income is a chart change for FRBS (not done in the demo chart) | AQ01 |
| FRBS 3.2.0 | Reports and schedules; board schedules in Word | Schedules export to Excel, PDF, ODS and CSV; there is no Word renderer in the report platform, so the GARD, subsidiaries and Mancom schedules flagged "Word requested" are issued in PDF and Excel | Gap G1 |
| FRBS 3.2.0 | 138 Appendix A reports | 28 schedule definitions and 58 report-pack entries are delivered with draft layouts; lease, DTA, ECL and placement data are not in BIBS; payroll outputs are out of scope | AQ05, AQ06 |
| FRBS 3.2.0 (VII) | BIR books and forms | Worksheets and loose-leaf books; they are not eFPS, DAT or CAS files | AQ07 |
| BASAU 2.3.2 | Assign functionality to a role | A change of role permissions is an access request approved by a second user (MODIFY_ROLE_PERMISSIONS) | PQ17 |
| BASAU 2.4.1, 2.5.3, 2.6.x | Return requests; select several requests | The API returns an access request with remarks, lets the requester resubmit it, and approves inbox items in bulk. The Access Requests screen and My Approvals do not show Return, Resubmit or bulk approval yet; journals have their own bulk posting | Gap G2 |

# Business context and process overview

## Business context

BDOI is a broker. Its money is of two kinds: trust money (premium collected for insurers and remitted net of commission) and its own income (commission, service fee, profit share, incentives). BRD-1 and BRD-2 cover the front of the cycle - booking, receipts, application, remittance, adjustments and commission receivables. BRD-5 covers the back office of Comptrollership. In BIBS the general ledger is internal: every module posts through the accounting engine, and FRBS closes, reconciles and reports on the same ledger.

<!-- table: widths=1,8,8 caption="Current and envisioned Accounting process (BRD p.44-46)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Input reports and the closing of the broking books depend on IT support | FRBS runs every report on demand; the broking books close automatically at month end |
| 2 | Output reports are prepared by hand | The report pack (Appendix A) is generated from the ledger and exported to Excel and PDF |
| 3 | Reconciliation is manual | Bank files are uploaded and matched automatically on check number and amount |
| 4 | The system cannot prevent unbalanced transactions | Every journal is validated before submission and posting; unbalanced journals are refused |
| 5 | Some transactions need manual entries | Every financial event of BIBS posts through rules; manual entries are assigned, reviewed and posted in the system |

## Process overview

The table lists the monthly cycle of the GL team, and Figure 1 shows it by actor.

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Daily posting | System (all modules) | Business events post to the GL and sub-ledger through the accounting rules; cost centres are derived | FRBS 2.5.0, 3.1.0, 3.1.1 |
| 2 | Manual entries | GL Officer, then GL TL / Head | Manual, adjustment and accrual journals are prepared, assigned, validated, confirmed and posted | FRBS 2.5.1-2.5.10, 2.8.x, 2.9.0 |
| 3 | Accrual reversal | System | Accruals with a "reverse on" date are reversed on that date | FRBS 2.8.1 |
| 4 | Bank reconciliation | GL Officer | Bank files are uploaded; items are matched on check number and amount; unmatched items are reported | FRBS 3.3.x |
| 5 | Broking-books cut-off | System | On the last day of the month at 23:00 the broking modules stop posting into the month | FRBS 3.4.0-3.4.1 |
| 6 | Revaluation rate and revaluation | GL Head; GL TL | The month-end CLOSING rate is entered; USD balances are revalued | FRBS 2.2.0, 3.5.0 |
| 7 | Month-end GL close | GL TL (scheduled) | On the scheduled date the checklist runs and the previous month closes | FRBS 2.6.0-2.6.1 |
| 8 | Service fee | GL Officer, GL TL | The service fee of the month is computed, approved, paid through Disbursement and tagged | FRBS 2.10.x |
| 9 | Reports | GL team | End-of-day, GARD, subsidiaries, schedules, Mancom, service fee and government reports | FRBS 2.4.x, 3.2.0 |
| 10 | Year-end close | GL TL | The year closes on or before 15 April and is verified | FRBS 2.7.0-2.7.1 |

![Monthly accounting cycle by actor (FRBS 2.2.0-3.6.0)](figures/brd05_v1_process_flow.dot)

# Personas and roles

## Personas

<!-- table: widths=3.2,4,7.4,2.4 caption="Personas and BIBS roles" size=8.5 -->
| Persona | BIBS role | Responsibilities | BRD |
|---|---|---|---|
| GL Officer (FRBS Processor) | FRBS_PROCESSOR | Prepares manual entries; monitors the service fee; runs and exports the report pack and government reports | p.66-67 matrix (p.181) |
| GL Team Lead | FRBS_TL | Assigns and posts entries; schedules and runs the month-end and year-end closes; maintains and uploads the chart; approves the service fee | p.66-67 matrix |
| GL Team Head / Section Head | FRBS_HEAD | As the TL, plus the monthly revaluation rate, authorisation of masters and reversal of journals | p.66-67 matrix |
| Business Administrator | BUSINESS_ADMIN | Maintains lists of values; requests user and role changes | BASAU 2.2.x (p.128) |
| System Administrator | SYSADMIN | Manages users and role profiles through approved requests | BASAU 2.3.x (p.129) |
| Approver (administration) | Holder of ACCESS_APPROVE or MASTER_AUTHORIZE | Approves, declines or returns LOV and user-management requests | BASAU 2.5.x-2.6.x (p.130-131) |
| Comptrollership, Auditor | COMPTROLLERSHIP, AUDITOR | Read access to journals, reports and closing | - |

The role grants are the project's reading of the scanned role matrix (p.181): only the Section Head inputs the revaluation rate; only the Team Lead closes the month and the year; Processors make manual entries and monitor the service fee. BDOI confirms the matrix through AQ28. Several roles per user are allowed (decision D5 of R6).

## Permissions

<!-- table: widths=4.6,2.6,9.6 caption="Accounting and administration permissions" -->
| Permission | Action class | Allows |
|---|---|---|
| JOURNAL_VIEW | VIEW | Journals, account inquiry, party statements |
| JOURNAL_CREATE | CREATE / AMEND | Create, edit, submit and cancel manual, adjustment and accrual journals |
| JOURNAL_ASSIGN | AMEND | Assign or re-assign submitted journals to a poster (FRBS 2.5.1) |
| JOURNAL_AUTHORIZE | APPROVE | Post (approve) or return journals, in bulk (FRBS 2.5.6-2.5.7) |
| JOURNAL_REVERSE | APPROVE | Reverse a posted journal |
| REVALUATION_RATE_MAINTAIN | AMEND | Enter the monthly revaluation rate (FRBS 2.2.0) |
| COA_UPLOAD | CREATE | Upload the chart of accounts (FRBS 2.3.1) |
| MASTER_MAINTAIN / MASTER_AUTHORIZE | AMEND / APPROVE | Maintain and authorise masters (chart, schedule definitions, LOVs) |
| PERIOD_MANAGE, PERIOD_END_RUN | AMEND | Open and close periods, run revaluation and year-end |
| GL_CLOSE_SCHEDULE | AMEND | Schedule, withdraw or run the GL close; broking-books cut-off (FRBS 2.6.0, 3.4.x) |
| FRBS_REPORT_VIEW, FRBS_REPORT_EXPORT | VIEW | Report pack and account schedules; commentary |
| SERVICE_FEE_MANAGE, SERVICE_FEE_APPROVE, SERVICE_FEE_TAG | CREATE / APPROVE / AMEND | Service-fee runs, approval and tags (FRBS 2.10.x) |
| TAX_VIEW | VIEW | BIR forms, books and alphalists |
| ACCOUNTING_RULE_MANAGE | AMEND | Accounting rules and cost-centre rules (FRBS 3.1.1) |
| EMPLOYEE_MAINTAIN | AMEND | Employee and cost-centre master (DIS 3.30.1, Volume 2) |
| LOV_MANAGE | AMEND | Lists of values (BASAU 2.2.x) |
| ACCESS_REQUEST, ACCESS_APPROVE | CREATE / APPROVE | User and role requests and their decisions (BASAU 2.3.x-2.6.x) |

## Permissions matrix

<!-- table: widths=5.4,1.8,1.8,1.8,1.8,1.8,1.8,1.8 caption="Role-to-permission matrix for Accounting (proposal until AQ28)" size=8 -->
| Permission | GL Officer | GL TL | GL Head | Bus. Admin | Sys. Admin | Compt. | Auditor |
|---|---|---|---|---|---|---|---|
| JOURNAL_VIEW | Y | Y | Y | | | Y | |
| JOURNAL_CREATE | Y | Y | | | | | |
| JOURNAL_ASSIGN | | Y | Y | | | | |
| JOURNAL_AUTHORIZE | | Y | Y | | | | |
| JOURNAL_REVERSE | | | Y | | | | |
| REVALUATION_RATE_MAINTAIN | | | Y | | | | |
| COA_UPLOAD, MASTER_MAINTAIN | | Y | | | | | |
| MASTER_AUTHORIZE | | | Y | | | | |
| PERIOD_MANAGE, PERIOD_END_RUN, GL_CLOSE_SCHEDULE | | Y | Y | | | | |
| FRBS_REPORT_VIEW | Y | Y | Y | | | Y | Y |
| FRBS_REPORT_EXPORT, TAX_VIEW | Y | Y | Y | | | | |
| SERVICE_FEE_MANAGE, SERVICE_FEE_TAG | Y | Y | | | | | |
| SERVICE_FEE_APPROVE | | Y | Y | | | | |
| LOV_MANAGE | | | | Y | | | |
| ACCESS_REQUEST | | | | Y | Y | | |

ACCESS_APPROVE is held by the access approvers named by BDOI; ACCOUNTING_RULE_MANAGE and EMPLOYEE_MAINTAIN by the Comptrollership administrators. The table shows the delivered grants of V890 and V899.

Segregation of duties enforced by the system: a journal is never posted by the user who submitted it; a submitted journal is never assigned to its submitter; a service-fee run is never approved by its preparer; an access request is never decided by its submitter; a record is never authorised by its maker.

# Functional requirements

## Access and session

```fr
id: FR-AC-001
title: Access Accounting and administration with a user profile
brd: [FRBS 1.1.0 (p.50), FRBS 1.1.1 (p.50), BASAU 1.1.0 (p.127), BASAU 1.1.1 (p.127)]
actor: FRBS users; Business and System Administrators
priority: Must have
fit: FIT
screens: Login; Home; menu groups Finance, Planning and Closing, Setup and Administration
api: POST /api/v1/auth/login
description:
  - Users reach BIBS from any BDO-issued device with a browser; the screens are responsive. Each user logs in with their own profile, and the menu shows only the screens their roles allow (section 3).
preconditions:
  - The user has an active BIBS account with an Accounting or administration role.
main_flow:
  - The user opens BIBS and enters the user ID and password.
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the menu of the user's roles.
alternate_flows:
  - Wrong credentials. The log-in is refused and counted; after LOGIN_MAX_FAILED_ATTEMPTS the account locks.
rules:
  - [R1, "Log-in, lock-out and session rules are those of BRD-1 (BRNB.040).", Configurable, Session parameters]
  - [R2, "BDO single sign-on / Active Directory is not part of this phase (Q42).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded with user, time and source address.
acceptance:
  - A GL Officer logs in from a BDO laptop and a BDO mobile device and sees Journals, Report Pack and Service Fee Runs.
  - A GL Officer does not see Chart Upload or GL Close & Cut-Off.
```

```fr
id: FR-AC-002
title: Warn before the idle time-out and before the forced log-out
brd: [FRBS 1.1.2 (p.50), FRBS 1.1.3 (p.50), BASAU 1.1.2 (p.128), BASAU 1.1.3 (p.128)]
actor: System
priority: Must have
fit: CONFIGURE
screens: All screens (session dialog)
api: "-"
description: After 15 minutes without activity BIBS shows an inactivity warning; the user continues or is logged out. 30 minutes before the end of the session BIBS warns that the system will log the user out. Both times are parameters.
preconditions:
  - "The user is logged in."
main_flow:
  - The user is inactive for SESSION_IDLE_WARNING_MINUTES.
  - BIBS shows the inactivity warning with the choice to stay logged in.
  - Before the session ends, BIBS shows the log-out warning SESSION_EXPIRY_WARNING_MINUTES ahead.
rules:
  - [R1, "Inactivity warning after 15 minutes (SESSION_IDLE_WARNING_MINUTES).", Configurable, Parameter]
  - [R2, "Log-out warning 30 minutes before the forced log-out (SESSION_EXPIRY_WARNING_MINUTES).", Configurable, Parameter]
validations: []
notifications:
  - "On-screen warnings only."
audit:
  - "Log-outs by time-out are recorded."
acceptance:
  - After 15 minutes of inactivity the warning appears.
  - The log-out warning appears 30 minutes before the session ends.
```

## Chart of accounts and revaluation rate

```fr
id: FR-AC-010
title: Enter the monthly revaluation rate
brd: [FRBS 2.2.0 (p.51), FRBS 3.6.0 (p.65)]
actor: GL Team Head / Section Head
priority: Must have
fit: FIT
screens: Setup > Currencies & Rates (card Monthly revaluation rates)
api: "GET/POST /api/v1/currencies/revaluation-rates; POST .../revaluation-rates/{yyyy-MM}/copy-to-book"
description:
  - The Section Head enters, once a month, the revaluation rate of each foreign currency for the month end. BIBS keeps it as the CLOSING rate of that date; the FX revaluation (FR-AC-043) uses it.
  - When OPS_BOOK_RATE_SOURCE is CLOSING_PREV_MONTH, the job BOOK_RATE_FROM_CLOSING copies the rates of the month that ends as the BOOK rates of the next month, never overwriting a BOOK rate already entered. "Copy to BOOK" does it on demand.
preconditions:
  - "The user has REVALUATION_RATE_MAINTAIN."
main_flow:
  - The Section Head opens Currencies & Rates, card Monthly revaluation rates.
  - The Section Head enters the month and the rate per currency and saves.
  - BIBS stores the CLOSING rate at the month end.
  - At 00:30 on the first day of the next month the job copies it as that month's BOOK rate.
rules:
  - [R1, "The revaluation rate is the CLOSING rate of the month end.", Fixed, "-"]
  - [R2, "BOOK rate of month M = CLOSING rate of the end of M-1 when OPS_BOOK_RATE_SOURCE = CLOSING_PREV_MONTH; MANUAL keeps BOOK rates by hand (AQ03, OQ08).", Configurable, Parameter OPS_BOOK_RATE_SOURCE]
  - [R3, "Rates are positive; the currency must be active.", Fixed, "-"]
validations:
  - [Rate not positive, Exchange rate must be positive, INVALID_RATE]
  - [Inactive currency, "Currency <code> is not active", INACTIVE_CURRENCY]
fields_screen: Monthly revaluation rates
fields:
  - [Month, Month, "Yes", "-", yyyy-MM]
  - [Currency, List, "Yes", Active currencies, Not the base currency]
  - [Rate, Number, "Yes", "-", "> 0; up to 8 decimals"]
notifications:
  - "None."
audit:
  - "Rates are recorded with user and time; the copy job records its run."
acceptance:
  - The Section Head enters USD 58.1234 for August; it appears as the CLOSING rate of 31 August and as the BOOK rate from 1 September.
  - A GL Officer cannot enter a revaluation rate.
```

> [!NOTE] Difference from the BRD
> The BRD asks only for a monthly rate. BIBS also proposes it as the BOOK rate of the next month for Operations receipts, which BDOI confirms through AQ03 / OQ08. The parameter switches this off.

```fr
id: FR-AC-011
title: Set up, view and edit the chart of accounts
brd: [FRBS 2.3.0 (p.51), FRBS 2.3.4 (p.53), FRBS 2.3.5 (p.53), FRBS 3.6.0 (p.65)]
actor: GL Team Lead (maintain); GL Team Head (authorise)
priority: Must have
fit: FIT
screens: General Ledger > Chart of Accounts
api: "GET/POST /api/v1/coa/accounts; PUT .../accounts/{id}; authorisation through My Approvals"
description: FRBS keeps the chart of accounts in BIBS - levels (group, main, sub, micro), class, category, parent, postable flag, control account and sub-ledger type, currencies, cost-centre requirement, revaluation flag, negative-balance policy, short code, branch and role restrictions, freeze and close. New and changed accounts are maker-checker. Users view the details of any account.
preconditions:
  - "The maker has MASTER_MAINTAIN; the authoriser MASTER_AUTHORIZE."
main_flow:
  - The TL opens Chart of Accounts and creates or edits an account.
  - BIBS checks the hierarchy (class of the parent, allowed level) and saves it pending authorisation.
  - The Head authorises it; it becomes usable for postings.
rules:
  - [R1, "A child account has the class of its parent; sub and micro accounts need a parent.", Fixed, "-"]
  - [R2, "An account with postings cannot become a heading.", Fixed, "-"]
  - [R3, "BDOI's own chart is loaded at go-live by upload (FR-AC-012); the demo chart is a placeholder (AQ01).", Configurable, Chart of accounts]
validations:
  - [Parent class differs, "Child account class must match parent <code>", CLASS_MISMATCH]
  - [Level not allowed under the parent, "A <level> account cannot be placed under a <level>", INVALID_TIER]
  - [Sub or micro without parent, Sub and Micro GL accounts require a parent account, PARENT_REQUIRED]
  - [Parent with postings, "Account <code> already has postings and cannot become a heading", PARENT_HAS_POSTINGS]
  - [Authoriser is the maker, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
fields_screen: Chart of Accounts (account)
fields:
  - [Account code, Text, Conditional, "-", "Unique; proposed by numbering when blank (FR-AC-013)"]
  - [Name, Text, "Yes", "-", "-"]
  - [Short code, Text, "No", "-", Unique per company (FR-AC-014)]
  - [Parent, Look-up, Conditional, Chart, Required for sub and micro]
  - [Class / Level / Category, List, "Yes", Account classes and levels, Class of the parent]
  - ["Postable, Control account, Sub-ledger type", Check box / List, "No", "-", "-"]
  - ["Cost centre required, Revaluation required", Check box, "No", "-", "-"]
  - [Negative balance policy, List, "No", "ALLOW, WARN, BLOCK", "Default ALLOW (FR-AC-034)"]
notifications:
  - "Pending accounts appear in My Approvals."
audit:
  - "Every change is audited with before and after values."
acceptance:
  - A new account under 1210 with a different class is refused.
  - A new account is not usable in a journal until another user authorises it.
```

```fr
id: FR-AC-012
title: Upload the chart of accounts
brd: [FRBS 2.3.1 (p.51)]
actor: GL Team Lead (COA_UPLOAD); GL Team Head (authorise)
priority: Must have
fit: CHANGE
screens: General Ledger > Chart Upload (Upload Chart, Upload History)
api: "/api/v1/coa/uploads (template, upload, rows, commit, cancel, report)"
description: FRBS uploads an XLSX, CSV or ODS file of parent and child accounts. BIBS validates every row (parent, class, level, category, postable, control, sub-ledger type, currency, flags), shows the valid and invalid rows, and on commit creates the accounts pending authorisation, parents before children. A parent may be an existing account or an earlier row of the file. The error report lists every refused row with its reason.
preconditions:
  - "The user has COA_UPLOAD."
main_flow:
  - The TL downloads the template and fills it.
  - The TL uploads the file; BIBS validates each row and shows the counts.
  - The TL commits the valid rows; BIBS creates the accounts pending authorisation.
  - The Head authorises them.
alternate_flows:
  - Invalid rows. The TL downloads the row report, corrects the file and uploads again, or commits only the valid rows.
  - Cancel. The TL cancels an upload that was not committed.
rules:
  - [R1, "Every uploaded account is created PENDING_AUTHORIZATION.", Fixed, "-"]
  - [R2, "A blank code takes the next number of the parent's numbering scheme (FR-AC-013).", Fixed, "-"]
validations:
  - [Class of a row differs from its parent, "Child account class must match parent <code>", CLASS_MISMATCH]
  - [Short code already used, "Short code <code> is already used by account <account>", SHORT_CODE_TAKEN]
  - [No numbering scheme for a blank code, "Enter the account code: no numbering scheme under <parent>", NO_NUMBERING]
  - [File not readable or wrong type, Upload a .csv or .xlsx file, "-"]
fields_screen: Chart Upload
fields:
  - [Chart file, File, "Yes", "-", "XLSX, CSV or ODS"]
notifications:
  - "Pending accounts appear in My Approvals of the authorisers."
audit:
  - "The upload keeps its file, rows, outcomes and the user who committed it."
acceptance:
  - The sample file of 8 accounts under the groups 1900 and 2900 loads without errors and the accounts wait for authorisation.
  - A row whose parent is missing is reported with its reason and not created.
```

```fr
id: FR-AC-013
title: Generate account numbers and roll child accounts up to their parent
brd: [FRBS 2.3.2 (p.52)]
actor: GL Team Lead; System
priority: Must have
fit: CHANGE
screens: General Ledger > Chart of Accounts (Numbering Schemes)
api: "GET/PUT /api/v1/coa/numbering; GET /api/v1/coa/accounts/next-code"
description: FRBS defines a numbering scheme per parent account (separator none, "." or "-", and a width of 1 to 6 digits). When an account is created or uploaded with a blank code, BIBS gives it the next free number under its parent; numbers already used under the prefix are never reused. Every child is linked to its parent, and the statements and schedules roll child balances up to the parent.
preconditions:
  - "The user has MASTER_MAINTAIN."
main_flow:
  - The TL sets the numbering scheme of a parent.
  - The TL creates a child account with a blank code.
  - BIBS proposes and assigns the next number.
rules:
  - [R1, "Numbers are unique and never reused under a parent.", Fixed, "-"]
  - [R2, "Balances of child accounts roll up to every parent level in the statements.", Fixed, "-"]
validations:
  - [Invalid scheme, "Separator must be none, '.' or '-' and width 1 to 6 digits", INVALID_NUMBERING]
  - [Top-level account without code, Enter the account code of a top-level account, ACCOUNT_CODE_REQUIRED]
  - [No free number left, "No free <width>-digit number under <parent>", NUMBERING_EXHAUSTED]
fields_screen: Numbering scheme
fields:
  - [Parent account, Look-up, "Yes", Chart, Heading account]
  - [Separator, List, "Yes", "none, '.', '-'", "-"]
  - [Width, Number, "Yes", "-", "1-6"]
notifications:
  - "None."
audit:
  - "Scheme changes are audited."
acceptance:
  - With the scheme "." and width 2 under 1900, a new blank-coded child becomes 1900.01, the next 1900.02.
  - The balance of 1900.01 is included in 1900 on the trial balance.
```

> [!NOTE] Difference from the BRD
> FRBS 2.3.2 says the account number is system-generated. BIBS generates it when the code is blank and a scheme exists; FRBS may still key a code. BDOI's numbering convention is part of AQ01.

```fr
id: FR-AC-014
title: Search accounts by name, short code or number
brd: [FRBS 2.3.3 (p.52)]
actor: FRBS users
priority: Must have
fit: CHANGE
screens: Chart of Accounts (search); journal lines (account entry)
api: "GET /api/v1/coa/accounts?q=; GET /api/v1/coa/accounts/lookup?key="
description: Each account may have a short code, unique per company regardless of case. The chart search finds accounts by code, name or short code, and a journal line accepts the short code in place of the account code (FR-AC-032).
preconditions:
  - "The user has JOURNAL_VIEW or MASTER_VIEW."
main_flow:
  - The user types a name, short code or number in the search.
  - BIBS lists the matching accounts.
rules:
  - [R1, "Short codes are unique per company, case-insensitive.", Fixed, "-"]
validations:
  - [Short code already used, "Short code <code> is already used by account <account>", SHORT_CODE_TAKEN]
  - [Unknown key on a journal line, "Unknown GL account <key>", UNKNOWN_ACCOUNT]
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - Searching "PRPHP" returns the account whose short code is PRPHP.
  - A second account cannot take a short code already used.
```

## Reports on demand

```fr
id: FR-AC-020
title: Select, run, view and copy reports on demand
brd: [FRBS 2.4.0 (p.53), FRBS 2.4.1 (p.53), FRBS 2.4.2 (p.53), FRBS 2.4.3 (p.54), FRBS 2.4.6 (p.54), FRBS 2.4.8 (p.55), FRBS 2.4.10 (p.55)]
actor: FRBS users
priority: Must have
fit: FIT
screens: Report Centre; Report; Accounting Reports > Report Pack
api: "GET /api/v1/reports; POST /api/v1/reports/{code}/run; GET .../export?format="
description: FRBS users run any report they are allowed to see at any time, without IT. They choose the report from the catalogue by category, enter its parameters (date range or as-of date, company, branch), view the result on screen, preview the PDF and export to XLSX, ODS, PDF or CSV. The on-screen table can be selected and copied into Excel or Word.
preconditions:
  - "The user has the view permission of the report (for example FRBS_REPORT_VIEW)."
main_flow:
  - The user opens the Report Centre or the Report Pack and selects a report.
  - The user enters the parameters and clicks **Run**.
  - BIBS shows the result.
  - The user previews or exports it.
rules:
  - [R1, "Exports and runs are archived with parameters, user and time.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
  - [Invalid parameters, "<errors>", INVALID_REPORT_PARAMETERS]
fields_screen: Report parameters (typical)
fields:
  - [Company, List, "Yes", Companies of the user, "-"]
  - [From / To or As of, Date, "Yes", "-", "Defaults per report (for example start of month, today)"]
  - [Branch, List, "No", Branches, "-"]
notifications:
  - "None."
audit:
  - "Each run and export is in the report archive."
acceptance:
  - A GL Officer runs the trial balance for August and exports it to XLSX and ODS.
  - The on-screen result can be copied and pasted into Excel.
```

```fr
id: FR-AC-021
title: Filter every column of a report
brd: [FRBS 2.4.4 (p.54)]
actor: FRBS users
priority: Must have
fit: CHANGE
screens: Report (column filter row)
api: "GET .../export?filter=column:text"
description: The report viewer has a filter row under the column headings. The user filters the loaded rows by any column; an export applies the same filters to the detail rows and notes them on the file.
preconditions:
  - "A report result is on screen."
main_flow:
  - The user types a filter value in a column.
  - BIBS shows only the matching rows.
  - The user exports; the file holds the filtered rows and states the filter.
rules:
  - [R1, "Filters apply to detail rows; totals are recomputed on the filtered rows.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The filter is part of the archived parameters."
acceptance:
  - Filtering the Account column of a trial balance on "1210" shows only the 1210 accounts; the export holds the same rows.
```

```fr
id: FR-AC-022
title: Download or print several reports at once
brd: [FRBS 2.4.5 (p.54), FRBS 2.4.7 (p.54)]
actor: FRBS users
priority: Must have
fit: CHANGE
screens: Report Centre > Report Batch
api: "POST /api/v1/reports/batches; GET /api/v1/reports/batches, /{id}, /{id}/file"
description: A report batch runs several reports with shared parameters and returns one ZIP of files in a chosen format, or one merged PDF for printing. Each report checks its own permission; a report that fails is listed and the others are delivered (status COMPLETED, PARTIAL or FAILED). Only the creator sees a batch.
preconditions:
  - "The user may run each report of the batch."
main_flow:
  - The user opens Report Batch and selects the reports.
  - The user enters the shared parameters and the output (ZIP of XLSX / ODS / PDF / CSV, or merged PDF).
  - BIBS runs the reports and offers the file.
rules:
  - [R1, "A batch has at least one report and at most the platform maximum.", Fixed, "-"]
  - [R2, "Batches run in the request; each report in its own transaction.", Fixed, "-"]
validations:
  - [No report or too many, "Select between 1 and <max> reports", REPORT_BATCH_SIZE]
  - [No file produced, "Batch <no> produced no file", REPORT_BATCH_EMPTY]
notifications:
  - "None."
audit:
  - "The batch and each report run are archived."
acceptance:
  - Selecting the trial balance, the income statement and the statement of condition for August returns one ZIP with three files.
  - The merged PDF option returns one PDF with the three reports in order.
```

```fr
id: FR-AC-023
title: Set the print options of a report
brd: [FRBS 2.4.9 (p.55)]
actor: FRBS users
priority: Must have
fit: CHANGE
screens: Report (Export dialog); Report Batch
api: "GET .../export?format=PDF&paper=&orientation=&fitToWidth="
description: Before printing, the user chooses the paper size, the orientation and whether to fit the columns to the page width. The header and footer repeat on every page.
preconditions:
  - "A report result is on screen."
main_flow:
  - The user chooses PDF and the print options.
  - BIBS renders the PDF with those options.
rules:
  - [R1, "Paper A4, Letter or Legal; portrait or landscape; fit to width on or off.", Fixed, "-"]
validations: []
fields_screen: PDF print options
fields:
  - [Paper, List, "No", "A4, Letter, Legal", "-"]
  - [Orientation, List, "No", "Portrait, Landscape", "-"]
  - [Fit to width, Check box, "No", "-", "-"]
notifications:
  - "None."
audit:
  - "The options are recorded with the export."
acceptance:
  - A wide report printed landscape, fit to width, shows all its columns on one page width.
```

## Posting and manual entries

```fr
id: FR-AC-030
title: Post every financial transaction to the GL and the sub-ledger
brd: [FRBS 2.5.0 (p.55), FRBS 3.1.0 (p.62)]
actor: System
priority: Must have
fit: FIT
screens: Journals; Account Inquiry; Party Statement; Accounting Engine (event log)
api: Accounting engine (business events)
description: Every BIBS module that has a financial effect publishes a business event (booking, receipt, remittance, adjustment, commission, disbursement, service fee, correction). The accounting engine builds the journal lines from the rules maintained by Comptrollership, posts them in the same transaction, and records or matches the open items of the parties in the sub-ledger. GL accounts are never chosen in code.
preconditions:
  - "An active accounting rule exists for the event type."
main_flow:
  - A module publishes an event with its amounts and account roles.
  - The engine resolves the rule, fills the cost centres (FR-AC-054) and builds balanced lines.
  - The engine posts the journal and the sub-ledger items.
alternate_flows:
  - No rule or missing account role. The event is recorded FAILED in the event log for re-processing; the alert of the module is raised.
  - Broking books closed. Events of the broking modules for a closed month are refused (FR-AC-042).
rules:
  - [R1, "Posting rules are configuration maintained by Comptrollership; the real rules replace the demo rules (AQ02, OQ07).", Configurable, Accounting rules]
  - [R2, "A posted journal is immutable; corrections are reversals and new entries.", Fixed, "-"]
validations:
  - [No rule for the event, "No active accounting rule for event <type> (line of business <lob>, currency <ccy>)", NO_ACCOUNTING_RULE]
  - [Account role not supplied, "Event <type> must supply an account for role <role>", MISSING_ACCOUNT_ROLE]
  - [Event without amounts, "Event <type> has no amounts to account for", EMPTY_ACCOUNTING_EVENT]
notifications:
  - "Alerts of the publishing module on failure."
audit:
  - "The event log keeps each event, its source reference and its journal."
acceptance:
  - An approved remittance batch posts its journal and clears the insurer's open items.
  - An event without a rule is FAILED in the event log and can be re-processed after the rule is added.
```

```fr
id: FR-AC-031
title: Assign manual entries for posting and work the assigned list
brd: [FRBS 2.5.1 (p.56), FRBS 2.5.2 (p.56), FRBS 2.5.3 (p.56)]
actor: GL Team Lead / Head (assign); poster
priority: Must have
fit: "CHANGE (2.5.1), FIT (2.5.2, 2.5.3)"
screens: General Ledger > Journals (Assign, Assigned to me); Journal
api: "POST /api/v1/journals/assign; GET /api/v1/journals/assignees; GET /api/v1/journals?assignedTo="
description: A TL or Head assigns submitted journals to a poster, or re-assigns them. Each poster filters Journals on "Assigned to me", selects an entry and views its lines, dimensions, attachments and history before posting.
preconditions:
  - "The assigner has JOURNAL_ASSIGN; the assignee holds JOURNAL_AUTHORIZE."
main_flow:
  - The TL selects submitted journals and clicks **Assign**.
  - The TL chooses the poster.
  - The poster opens Journals, filter "Assigned to me", and opens an entry.
rules:
  - [R1, "The assignee must be allowed to authorise journals and must not be the submitter.", Fixed, "-"]
validations:
  - [Assignee cannot authorise, "<user> is not allowed to authorize journals", ASSIGNEE_NOT_AUTHORIZER]
  - [Assignee is the submitter, "<journal> cannot be assigned to the user who submitted it", ASSIGNEE_IS_MAKER]
fields_screen: Assign journals
fields:
  - [Assign to, List, "Yes", Users with JOURNAL_AUTHORIZE, Not the submitter]
notifications:
  - "The assignee sees the entries in Assigned to me."
audit:
  - "Assigned by, assigned to and time are stored on the journal."
acceptance:
  - A journal submitted by glofficer cannot be assigned to glofficer.
  - After assignment to gltl, the journal appears in gltl's Assigned to me.
```

```fr
id: FR-AC-032
title: Prepare manual entries
brd: [FRBS 2.8.0 (p.59), FRBS 2.8.1 (p.60), FRBS 2.8.2 (p.60), FRBS 2.8.5 (p.61)]
actor: GL Officer; GL Team Lead
priority: Must have
fit: "FIT (2.8.0, 2.8.2, 2.8.5), CHANGE (2.8.1)"
screens: General Ledger > New Journal; Edit Journal
api: "POST /api/v1/journals; PUT .../{id}; POST .../{id}/submit; journal upload /api/v1/journals/upload"
description: The GL Officer prepares a MANUAL, ADJUSTMENT or ACCRUAL journal - journal type, value date, reference, narration, optional "reverse on" date, and lines with the account (by code or short code; the name fills in), debit or credit, description, cost centre and other dimensions. The journal is saved as a draft and submitted for review and posting. Journals can also be uploaded from a file or generated from recurring templates.
preconditions:
  - "The user has JOURNAL_CREATE; the value date is in an open period."
main_flow:
  - The officer clicks **New Journal** and fills the header and the lines.
  - The officer saves the draft; BIBS numbers it.
  - The officer submits it; BIBS validates it (FR-AC-034) and asks for confirmation (FR-AC-035).
  - The journal becomes PENDING_APPROVAL.
alternate_flows:
  - Accrual with reversal. A "reverse on" date after the value date is set; the reversal posts automatically (FR-AC-033).
rules:
  - [R1, "Only MANUAL, ADJUSTMENT or ACCRUAL journals are entered manually.", Fixed, "-"]
  - [R2, "Only the maker changes a draft or returned journal.", Fixed, "-"]
  - [R3, "The reversal date must follow the value date and applies to manual journals only.", Fixed, "-"]
validations:
  - [Wrong journal type, "Only MANUAL, ADJUSTMENT or ACCRUAL journals can be entered manually", INVALID_JOURNAL_TYPE]
  - [Not the maker, "Only the maker (<user>) can change this journal", NOT_MAKER]
  - [Reversal date not after value date, "The reversal date must be after the value date <date>", REVERSAL_DATE_INVALID]
  - [Unknown account or short code, "Unknown GL account <key>", UNKNOWN_ACCOUNT]
fields_screen: New Journal
fields:
  - [Journal type, List, "Yes", "MANUAL, ADJUSTMENT, ACCRUAL", "-"]
  - [Value date, Date, "Yes", "-", Open period; back- and forward-dating limits]
  - [Reference, Text, "No", "-", "-"]
  - [Narration, Text, "Yes", "-", "-"]
  - [Reverse on, Date, "No", "-", "After the value date"]
  - [Line - account or short code, Look-up, "Yes", Chart of accounts, Postable; manual posting allowed]
  - [Line - debit / credit, Amount, "Yes", "-", One side per line; > 0]
  - [Line - description, Text, "No", "-", "-"]
  - [Line - cost centre and dimensions, List, Conditional, Dimensions, Required when the account requires a cost centre]
notifications:
  - "The journal appears in the approval inbox of the posters."
audit:
  - "Create, save, submit, cancel and every change are recorded with user and time."
acceptance:
  - An accrual entered with the short code of the account shows the account name and saves as a draft.
  - Submitting sends it to PENDING_APPROVAL after confirmation.
```

```fr
id: FR-AC-033
title: Reverse accruals automatically on their reversal date
brd: [FRBS 2.8.1 (p.60)]
actor: System
priority: Must have
fit: CHANGE
screens: Journals (REVERSAL journals); Scheduled Jobs
api: Job JOURNAL_AUTO_REVERSAL
description: Every day at 00:05 Manila the job JOURNAL_AUTO_REVERSAL posts, for each posted manual journal whose "reverse on" date has come, a REVERSAL system journal that mirrors it, linked to the original. Each reversal posts in its own transaction.
preconditions:
  - "A posted manual journal has a reverse-on date of today or earlier and is not reversed."
main_flow:
  - The job selects the journals due for reversal.
  - BIBS posts the reversal journal with the key JV:<batch>:AUTOREV.
  - The original shows its reversal.
rules:
  - [R1, "One reversal per journal (idempotent key).", Fixed, "-"]
  - [R2, "Schedule daily 00:05 Manila.", Configurable, Job schedule journal-auto-reversal-cron]
validations:
  - [Journal already reversed, "Journal <no> is already reversed", ALREADY_REVERSED]
  - [Reversal already pending, "A reversal of <no> already exists", REVERSAL_PENDING]
notifications:
  - "None; failures appear in the job run history."
audit:
  - "The reversal journal carries the source AUTOREV and the link to the original."
acceptance:
  - An accrual posted on 31 August with reverse-on 1 September is reversed by a system journal dated 1 September.
```

> [!NOTE] Difference from the BRD
> FRBS 2.8.1 asks for the reversal in the GL and the SL. Manual journals record no sub-ledger open items in BIBS, so only the GL is reversed.

```fr
id: FR-AC-034
title: Validate entries and refuse errors
brd: [FRBS 2.5.4 (p.56), FRBS 2.5.5 (p.57), FRBS 2.8.4 (p.61), FRBS 3.6.0b (p.66)]
actor: System
priority: Must have
fit: "CHANGE (2.5.4, 2.8.4, 3.6.0b), FIT (2.5.5)"
screens: New Journal; Journal; confirmation dialog
api: "POST .../journals/{id}/submit, /approve; GET .../journals/{id}/warnings"
description: BIBS validates every manual journal on submission and again on posting. It refuses a journal that has no debit or no credit line, is not balanced, posts to a non-postable, frozen or closed account, uses a currency or branch the account does not allow, is dated outside the permitted window, or would leave a BLOCK account with a negative balance. WARN accounts give a warning that the user sees before confirming. All errors are listed together.
preconditions:
  - "A journal is submitted or approved."
main_flow:
  - The user submits or approves a journal.
  - BIBS runs the checks and the negative-balance check on the balance after posting.
  - BIBS refuses the journal with every error, or shows the warnings for confirmation.
rules:
  - [R1, "Each account has a negative-balance policy ALLOW, WARN or BLOCK; the natural side comes from the account class.", Configurable, Chart of accounts (FR-AC-011)]
  - [R2, "The accounts that may never be negative are to be named by BDOI (AQ30).", Configurable, Chart of accounts]
validations:
  - [Unbalanced journal, "Journal is not balanced - debit <x> vs credit <y>", UNBALANCED_JOURNAL]
  - [Any line error, "<list of errors>", JOURNAL_INVALID]
  - [Negative balance on a BLOCK account, "Account <code> would have a negative (<side>) balance of <amount>", JOURNAL_INVALID]
  - [Negative balance on a WARN account, "Account <code> would have a negative (<side>) balance of <amount>", "- (warning)"]
  - [Value date outside the window, "Value date <date> is earlier / later than the permitted <n> back- / forward-dated days", JOURNAL_INVALID]
notifications:
  - "Messages on screen."
audit:
  - "Refusals are not posted; warnings accepted are recorded with the confirmation."
acceptance:
  - A journal with debits of 1,000.00 and credits of 900.00 is refused as unbalanced.
  - A journal that would make a BLOCK cash account negative is refused; on a WARN account it shows a warning and can be confirmed.
```

> [!NOTE] Difference from the BRD
> FRBS 2.5.4 asks to prevent posting with a negative balance on every account. BIBS lets FRBS choose per account (ALLOW, WARN, BLOCK), because many GL accounts legitimately change sides.

```fr
id: FR-AC-035
title: Confirm the transaction details before sending them on
brd: [FRBS 2.5.10 (p.58), FRBS 2.8.3 (p.60)]
actor: GL Officer; poster
priority: Must have
fit: CHANGE
screens: Confirm posting dialog (Journals)
api: "-"
description: Before a journal is submitted or posted, BIBS shows a confirmation dialog with the totals, the number of lines, the dates and any warnings. The user confirms or goes back.
preconditions:
  - "The user clicks Submit or Post."
main_flow:
  - BIBS shows the confirmation dialog.
  - The user confirms; BIBS submits or posts.
alternate_flows:
  - Cancel. The journal stays as it was.
rules:
  - [R1, "The dialog is shown for every submission and posting.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The confirmed action is audited."
acceptance:
  - Submitting a journal shows its debit and credit totals and line count before it is sent.
```

```fr
id: FR-AC-036
title: Post or return entries, one or several at a time, with remarks
brd: [FRBS 2.5.6 (p.57), FRBS 2.5.7 (p.57), FRBS 2.5.8 (p.57), FRBS 2.5.9 (p.57)]
actor: GL Team Lead / Head (JOURNAL_AUTHORIZE)
priority: Must have
fit: "CHANGE (2.5.6), FIT (2.5.7-2.5.9)"
screens: Journals (bulk posting); Journal (Post, Return to Maker); My Approvals
api: "POST /api/v1/journals/{id}/approve, /{id}/reject; POST /api/v1/journals/bulk-approve"
description: The poster posts a single journal or selects several and posts them together. Each journal is validated and posted in its own transaction with every control; the result lists the successes and the refusals. The poster returns a journal to its maker with remarks ("Return to Maker"); the maker corrects and resubmits it.
preconditions:
  - "The user has JOURNAL_AUTHORIZE and did not submit the journal."
main_flow:
  - The poster selects the journals and clicks **Post**.
  - BIBS validates and posts each journal; the results are listed.
alternate_flows:
  - Return. The poster enters the remarks and clicks **Return to Maker**; the journal is REJECTED and editable by its maker.
  - Limit. A journal above the poster's authorisation limit is refused.
rules:
  - [R1, "A journal is never posted by its submitter.", Fixed, "-"]
  - [R2, "Posting limits per user apply.", Configurable, User authorisation limits]
validations:
  - [Poster is the submitter, A journal cannot be authorized by the user who submitted it, MAKER_CHECKER_VIOLATION]
  - [Above the limit, "Journal total <amount> exceeds your authorization limit <limit>", AUTHORIZATION_LIMIT_EXCEEDED]
  - [Wrong status, "Cannot <action> journal <no> in status <status>", INVALID_JOURNAL_STATUS]
  - [Return without remarks, Enter the reason, "-"]
fields_screen: Return to Maker
fields:
  - [Remarks, Long text, "Yes", "-", "-"]
notifications:
  - "The maker sees a returned journal in the journal list with its remarks."
audit:
  - "Posting and return are recorded with user, time and remarks."
acceptance:
  - Posting 10 selected journals of which one is unbalanced posts 9 and reports the refused one.
  - A returned journal shows the remarks and can be edited and resubmitted by its maker.
```

```fr
id: FR-AC-037
title: Edit accounting entries
brd: [FRBS 2.9.0 (p.61)]
actor: GL Officer (maker)
priority: Must have
fit: FIT
screens: Edit Journal; Journal (Reverse)
api: "PUT /api/v1/journals/{id}; POST .../{id}/reverse"
description: Draft and returned journals are edited by their maker. A posted journal is never edited; it is corrected by reversal and a new entry, or through an ACSL correction entry (Volume 2, FR-AS-021).
preconditions:
  - "The journal is DRAFT or REJECTED (edit); POSTED (reverse, JOURNAL_REVERSE)."
main_flow:
  - The maker opens the journal and edits it.
  - The maker submits it again.
alternate_flows:
  - Posted. The Head reverses the journal with a reason; the maker prepares a new one.
rules:
  - [R1, "Posted journals are immutable.", Fixed, "-"]
validations:
  - [Edit a posted journal, "Cannot edit journal <no> in status POSTED", INVALID_JOURNAL_STATUS]
notifications:
  - "None."
audit:
  - "Every change and the reversal are audited."
acceptance:
  - A returned journal is edited and resubmitted by its maker.
  - A posted journal has no Edit action.
```

## Closing and revaluation

```fr
id: FR-AC-040
title: Schedule and run the month-end close of the GL books
brd: [FRBS 2.6.0 (p.58; Add.1 p.35), FRBS 2.6.1 (p.58)]
actor: GL Team Lead / Head (GL_CLOSE_SCHEDULE); System
priority: Must have
fit: CHANGE
screens: Planning & Closing > GL Close & Cut-Off (Month-End Close)
api: "GET/POST /api/v1/closing/close-schedules; GET .../close-schedules/proposal; POST .../close-schedules/{id}/cancel; POST /api/v1/closing/close-now"
description:
  - FRBS schedules the close of the previous month's GL books on a date and time of its choice in the current month (Addendum 1). BIBS proposes the 2nd banking day of the month at 17:00 Manila, skipping company holidays. Every 15 minutes the job GL_PERIOD_CLOSE runs the closes that are due - the period-end checklist, then the period close. A refused close is recorded on the schedule with the blocking items and raises GL_CLOSE_FAILED. FRBS can also close at once.
  - With CLOSE_ONLY_PREVIOUS_MONTH on, only the month before the close date may be closed.
preconditions:
  - "The user has GL_CLOSE_SCHEDULE; the period is open and has no scheduled close."
main_flow:
  - The TL opens GL Close & Cut-Off and clicks **Schedule Close**.
  - BIBS proposes the date and time; the TL confirms or changes them.
  - At the time, the job runs the checklist and closes the period.
alternate_flows:
  - Checklist fails. The close is FAILED with the blocking items; GL_CLOSE_FAILED is raised; the TL fixes the items and schedules again.
  - Withdraw. The TL withdraws a scheduled close.
rules:
  - [R1, "Only the previous month may be closed (CLOSE_ONLY_PREVIOUS_MONTH = true).", Configurable, Parameter CLOSE_ONLY_PREVIOUS_MONTH]
  - [R2, "Proposed close - 2nd banking day of the next month, 17:00 Manila.", Fixed, "-"]
  - [R3, "The checklist covers pending journals, reconciliations, FX revaluation and the trial balance.", Fixed, "-"]
validations:
  - [Not the previous month, "On <date> only the period of <month> may be closed, not <period>", CLOSE_ONLY_PREVIOUS_MONTH]
  - [Already scheduled, "<period> already has a scheduled close", CLOSE_ALREADY_SCHEDULED]
  - [Period not closable, "<period> is <status>", PERIOD_NOT_CLOSABLE]
  - [Checklist blocking items, "<blocking items>", GL_CLOSE_CHECKLIST]
  - [Schedule not active, "The scheduled close is already <status>", CLOSE_SCHEDULE_NOT_ACTIVE]
fields_screen: Schedule Close
fields:
  - [Period, List, "Yes", Open periods, Previous month]
  - [Close on, Date-time, "Yes", "-", "In the future; default 2nd banking day 17:00"]
notifications:
  - Alert GL_CLOSE_FAILED when a scheduled close fails.
audit:
  - "Schedule, withdrawal, run and result are recorded with user and time."
acceptance:
  - On 1 October, scheduling the close of August is refused; September is accepted.
  - A close with unposted journals fails and raises GL_CLOSE_FAILED with the list.
```

```fr
id: FR-AC-041
title: Close the year and verify that the nominal accounts are zero
brd: [FRBS 2.7.0 (p.59), FRBS 2.7.1 (p.59)]
actor: GL Team Lead
priority: Must have
fit: "FIT (2.7.0), CHANGE (2.7.1)"
screens: Planning & Closing > Period-End & Year-End (Year-End panel)
api: "GET /api/v1/closing/year-end/checklist, /preview; POST .../year-end/close, .../year-end/verify"
description: The TL closes the fiscal year by 15 April of the next year. The year-end close zeroes the income and expense accounts into retained earnings and opens the next year. BIBS then verifies the close - the nominal balance as of the year end and the trial-balance difference must both be zero - and stores and shows the result. The alert YEAR_END_CLOSE_DUE is raised 15 days before the deadline while the year is open.
preconditions:
  - "Every period of the year is closed; the user has PERIOD_END_RUN."
main_flow:
  - The TL runs the year-end checklist and previews the closing entries.
  - The TL closes the year.
  - BIBS posts the closing journal and verifies the result.
alternate_flows:
  - Verify again. After late adjustments the TL runs the verification again.
rules:
  - [R1, "Deadline 15 April (YEAR_END_CLOSE_DEADLINE = 04-15); alert 15 days before.", Configurable, Parameter YEAR_END_CLOSE_DEADLINE; alert YEAR_END_CLOSE_DUE]
  - [R2, "Periods of a closed year cannot be reopened.", Fixed, "-"]
validations:
  - [Open periods, "All periods of fiscal year <year> must be closed first", YEAR_PERIODS_OPEN]
  - [Checklist fails, "<failures>", YEAR_END_CHECKLIST_FAILED]
  - [Already closed, "Fiscal year <year> is already closed", YEAR_ALREADY_CLOSED]
  - [Verify a year not closed, The fiscal year has no year-end close to verify, YEAR_NOT_CLOSED]
notifications:
  - Alert YEAR_END_CLOSE_DUE before the deadline.
audit:
  - "The close, the closing journal and the verification (values, time) are stored."
acceptance:
  - After the close of 2025, the verification shows nominal balance 0.00 and trial-balance difference 0.00.
  - On 1 April 2026 with 2025 still open, YEAR_END_CLOSE_DUE is raised.
```

```fr
id: FR-AC-042
title: Close the broking books automatically at month end
brd: [FRBS 3.4.0 (p.64), FRBS 3.4.1 (p.65)]
actor: System; GL Team Lead (reopen)
priority: Must have
fit: CHANGE
screens: Planning & Closing > GL Close & Cut-Off (Broking Books Cut-Off)
api: "GET /api/v1/closing/broking-books, /broking-books/pending; POST .../broking-books/close, /broking-books/reopen"
description:
  - On the last day of each month at 23:00 Manila the job BROKING_BOOKS_CLOSE closes the broking books of the month. From then on the accounting engine refuses events of the broking source modules (booking, Operations, cashiering, remittance, adjustment, commission, disbursement) dated in that month, without any change to those modules. The GL itself stays open for FRBS adjustments until the GL close (FR-AC-040).
  - The screen shows the status of the cut-off and the pending broking items. A TL can close at once or reopen the books with a reason.
preconditions:
  - "The month is open."
main_flow:
  - At 23:00 on the last day the job closes the broking books.
  - A later broking event dated in the month is refused with BOOKS_CLOSED.
  - FRBS posts its adjustments and then closes the GL books.
alternate_flows:
  - Reopen. The TL reopens the broking books with a reason (for example a late remittance) and closes them again.
rules:
  - [R1, "Close time BROKING_CLOSE_TIME = 23:00 Manila on the last day of the month.", Configurable, Parameter BROKING_CLOSE_TIME; job broking-books-close-cron]
  - [R2, "The broking source modules are listed in BROKING_SOURCE_MODULES.", Configurable, Parameter BROKING_SOURCE_MODULES]
  - [R3, "The pending list is empty until the broking modules report their pending items (port BrokingCutoffCheck).", Fixed, "-"]
validations:
  - [Broking event in a closed month, "The broking books of <period> are closed", BOOKS_CLOSED]
  - [Reopen without reason, Enter the reason for reopening the books, REASON_REQUIRED]
fields_screen: Reopen broking books
fields:
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - Alert GL_CLOSE_FAILED if the automatic close fails.
audit:
  - "Close and reopen are recorded with user, time and reason."
acceptance:
  - After 23:00 on 30 September, a remittance posting dated 30 September is refused; a GL adjustment dated 30 September is accepted.
  - A reopened month accepts broking postings until it is closed again.
```

> [!NOTE] Difference from the BRD
> The agreed close time and whether a closed month may be reopened are open (AQ04); 23:00 and reopen-with-reason are the delivered defaults. The pending broking items are not listed yet because the broking modules do not implement the cut-off check.

```fr
id: FR-AC-043
title: Revalue USD balances at the revaluation rate
brd: [FRBS 3.5.0 (p.65)]
actor: GL Team Lead (PERIOD_END_RUN)
priority: Must have
fit: CONFIGURE
screens: Planning & Closing > FX Revaluation
api: "GET /api/v1/closing/fx-revaluations/preview; POST /api/v1/closing/fx-revaluations"
description: At month end the TL previews and runs the FX revaluation. BIBS revalues the balances of the accounts flagged "revaluation required" (USD receivables, payables, cash and bank) per account, branch and currency at the CLOSING rate of FR-AC-010, posts the difference to the unrealised FX gain or loss, and reverses it automatically on the first day of the next period. Open items are revalued for information. The register report GL-FXREV lists the revaluation.
preconditions:
  - "The CLOSING rate of the month end exists."
main_flow:
  - The TL opens FX Revaluation, chooses the period and previews.
  - The TL runs the revaluation; BIBS posts the entries and their reversal.
rules:
  - [R1, "Revaluation is per period, idempotent, with automatic reversal.", Fixed, "-"]
  - [R2, "The accounts revalued are those flagged revaluation required.", Configurable, Chart of accounts]
  - [R3, "Daily revaluation (BRD - previous date versus current date) only if BDOI confirms (AQ03).", Fixed, "-"]
validations:
  - [No CLOSING rate, "No CLOSING rate for <currencies>", RATE_NOT_FOUND]
  - [Period of another company, Period belongs to another company, PERIOD_OTHER_COMPANY]
notifications:
  - "None."
audit:
  - "The revaluation run, its journal and its reversal are recorded."
acceptance:
  - A USD receivable of 1,000.00 booked at 56.00 and revalued at 58.00 gives an unrealised gain of 2,000.00, reversed on the first day of the next month.
```

## Bank reconciliation

```fr
id: FR-AC-050
title: Upload bank files and reconcile automatically
brd: [FRBS 3.3.0 (p.63), FRBS 3.3.1 (p.63), FRBS 3.3.2 (p.64)]
actor: GL Officer; System
priority: Must have
fit: "FIT (3.3.0), CHANGE (3.3.1, 3.3.2)"
screens: Receivables > Bank Statements; Bank Reconciliation; Setup > Bank Statement Layouts
api: "POST /api/v1/receivables/bank-rec/statements/file; GET/POST .../bank-rec/layouts; POST .../bank-rec/auto-match; .../matches; .../reconciliations/{id}/finalize"
description:
  - FRBS keeps a statement layout per bank account (the columns of the bank's file). The GL Officer uploads the bank's cash-in-bank file (XLSX, ODS or CSV); BIBS maps it to the standard statement, imports it and runs the automatic matching at once.
  - The first matching pass pairs a book entry and a bank line that carry the same check number (in the reference or narration) and the same amount, on any date. The other rules then match the remaining lines on amount within a date window. The officer reviews the matches, matches or unmatches by hand, and finalises the reconciliation.
preconditions:
  - "The layout of the bank account exists; the user has the bank-reconciliation permission."
main_flow:
  - The officer opens Bank Statements and uploads the file for the bank account.
  - BIBS imports the lines and auto-matches them.
  - The officer reviews the workbench, adjusts matches and finalises.
alternate_flows:
  - Lines without a check number. They are matched by amount and date, or by hand (AQ08).
rules:
  - [R1, "Rule CHECK_NO_AND_AMOUNT runs first - same check number and same amount.", Fixed, "-"]
  - [R2, "Bank file layouts are configuration per bank account (AQ08).", Configurable, Bank Statement Layouts]
validations:
  - [Row with a bad date or amount, "Row <n> - invalid date / amount '<value>'", INVALID_STATEMENT]
  - [Layout without amount columns, Map either a signed amount column or both debit and credit columns, LAYOUT_AMOUNTS]
  - [Invalid date pattern in the layout, "Invalid date pattern <pattern>", LAYOUT_DATE_PATTERN]
  - [Line already reconciled, "Statement line <n> is already reconciled", LINE_ALREADY_MATCHED]
fields_screen: Bank Statement Layout
fields:
  - [Bank account (GL), List, "Yes", Bank accounts, "-"]
  - ["Column mapping (date, description, reference, debit and credit or signed amount, balance)", Text, "Yes", "-", Column names of the file]
  - [Date pattern, Text, "No", "-", Valid date pattern]
notifications:
  - "None."
audit:
  - "Imports, matches, unmatches and finalisation are recorded with user and time."
acceptance:
  - A bank file with check 000123 for 15,000.00 matches the book payment of check 000123 for 15,000.00 though the dates differ.
  - After finalisation the reconciliation statement shows no unexplained difference.
```

```fr
id: FR-AC-051
title: Report the unmatched transactions
brd: [FRBS 3.3.3 (p.64)]
actor: GL Officer
priority: Must have
fit: FIT
screens: Report Centre; Bank Reconciliation
api: "Reports FIN-BRS-UNREC-BOOK, FIN-BRS-UNREC-BANK, FIN-BRS-STMT"
description: The reports list the book entries not matched to the bank, the bank lines not matched to the books, and the bank reconciliation statement (balance per bank, per books, reconciling items). They export to Excel and PDF.
preconditions:
  - "A statement is imported."
main_flow:
  - The officer runs the unmatched reports for the bank account and date.
rules:
  - [R1, "Unmatched items are those without a match at the report date.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Report runs are archived."
acceptance:
  - A deposit in transit appears on the unmatched book report and on the reconciliation statement.
```

## Service fee

```fr
id: FR-AC-052
title: Compute, approve and pay the service fee
brd: [FRBS 2.10.0 (p.61); Appendix A VI (p.144)]
actor: GL Officer (compute, submit); GL Team Lead / Head (approve)
priority: Must have
fit: NEW
screens: Accounting Reports > Service Fee Runs; Service Fee Run; Service Fee Rates
api: "GET/POST /api/v1/frbs/service-fee/runs; .../runs/{id}/recompute, /submit, /approve; GET/POST .../rules, .../recipients"
description:
  - The service fee is the referrers' share of the commission that BDOI has fully collected - 2.5% or 1% of the commission net of the insurer's withholding tax, per market segment (p.144). FRBS computes a run for a period - BIBS takes the invoices whose payment status became paid in the period, are not cancelled, carry commission and are in no live run, and applies the rule of their segment in force on the day paid. It groups them into one line per service-fee segment, sales unit and currency. The payee of a line is the unit's recipient, else the unit; the cost centre is the recipient's, else the unit's, else the invoice's, else the cost-centre rules.
  - The run is submitted and approved by another user. Approval posts the accrual (FRBS_SERVICE_FEE_ACCRUE, expense against service fee payable) per line and sends each payout to Disbursement as a request of type SERVICE_FEE (Volume 2, FR-DS-020).
preconditions:
  - "Rules exist for the segments; the user has SERVICE_FEE_MANAGE (compute) or SERVICE_FEE_APPROVE (approve)."
main_flow:
  - The officer opens Service Fee Runs, enters the fully-paid period and clicks **Compute**.
  - BIBS computes the run (SFR-yyyy-n) with its lines and invoices.
  - The officer reviews it and submits it.
  - The TL approves it; BIBS posts the accruals and sends the payouts.
alternate_flows:
  - Recompute. A computed run is recomputed after rule changes.
  - Return. The approver returns the run with a reason; it is COMPUTED again.
  - Cancel. A computed run is cancelled; its invoices are freed.
  - Payout returned. A payout returned or cancelled by Disbursement puts the line back for "Send Again".
rules:
  - [R1, "Delivered rules - CBG 2.5% (segments CBG, RETAIL); IBG 1% (COMBANK, CORBANK, INSTITUTIONAL); base = commission less the insurer's withholding tax. To be confirmed (AQ20).", Configurable, Service Fee Rates]
  - [R2, "An invoice is in one live run only.", Fixed, "-"]
  - [R3, "Fee = (commission - insurer WTAX) x rate, 2 decimals; USD lines at the BOOK rate.", Fixed, "-"]
  - [R4, "The approver is neither the preparer nor the submitter.", Fixed, "-"]
validations:
  - [Period invalid, Give a period that ends on or before today and after it starts, SERVICE_FEE_PERIOD]
  - [Nothing to pay, "No invoice fully paid from <from> to <to> is left for a service fee", SERVICE_FEE_NOTHING]
  - [Run without invoices, "Run <no> has no invoice to pay a fee on", SERVICE_FEE_EMPTY]
  - [Approver is the preparer, The run is approved by someone other than its preparer, SERVICE_FEE_FOUR_EYES]
  - [Wrong stage, "Run <no> is <stage> and cannot be <action>", SERVICE_FEE_STAGE]
  - [Rule incomplete, "Give the segment, the rate and the start date", SERVICE_FEE_RULE]
  - [Rate not positive, The rate is a percentage above 0, SERVICE_FEE_RATE]
fields_screen: Service Fee Run (compute) and Service Fee Rates
fields:
  - [Fully Paid From / To, Date, "Yes", "-", "To <= today; From <= To"]
  - [Rule - Service-Fee Segment, List, "Yes", LOV SERVICE_FEE_SEGMENT, "-"]
  - [Rule - Market Segments, Multi-select, "Yes", Market segments, "-"]
  - [Rule - Rate (%), Number, "Yes", "-", "> 0"]
  - [Rule - Effective From / To, Date, "Yes / No", "-", To >= From]
  - ["Recipient - Sales Unit, Payee Code, Payee Name, Cost Centre", Text / List, "Yes", Sales units; payees, "-"]
notifications:
  - "The approvers see submitted runs in their work list."
audit:
  - "Compute, submit, approve, return, cancel, the accruals and the payouts are recorded on the run."
acceptance:
  - For a CBG invoice fully paid in August with commission 10,000.00 and insurer WTAX 200.00, the fee is 245.00.
  - The preparer cannot approve the run.
  - On approval, one accrual journal and one Disbursement request are created per line.
```

```fr
id: FR-AC-053
title: Record the liquidation report and tag released or liquidated
brd: [FRBS 2.10.1 (p.62), FRBS 2.10.2 (p.62)]
actor: GL Officer (SERVICE_FEE_TAG); System
priority: Must have
fit: NEW
screens: Service Fee Run (lines - Release, Liquidate)
api: "POST .../frbs/service-fee/runs/{id}/lines/{n}/release, /liquidate (multipart)"
description: When Disbursement pays a line (status PAID), BIBS tags it RELEASED on that date. The officer can also tag a line released with the credit date. When the unit sends its liquidation report, the officer uploads it on the line and tags the line LIQUIDATED with the liquidation date. The run moves to RELEASED when every paid line is released and to LIQUIDATED when every line is liquidated.
preconditions:
  - "The run is approved."
main_flow:
  - Disbursement pays the line; BIBS tags it released.
  - The officer uploads the unit's liquidation report and enters the liquidation date.
  - BIBS tags the line liquidated; when all are, the run is LIQUIDATED.
rules:
  - [R1, "The liquidation date is not before the release date and not in the future.", Fixed, "-"]
  - [R2, "The liquidation report is mandatory to liquidate.", Fixed, "-"]
validations:
  - [Date in the future, Give a date that is not in the future, SERVICE_FEE_DATES]
  - [Liquidation before release, "The liquidation date is before the release date <date>", SERVICE_FEE_DATES]
  - [No report attached, Attach the liquidation report of the unit, SERVICE_FEE_LIQUIDATION_REPORT]
  - [Wrong line status, "Line <n> is <status> and cannot be <action>", SERVICE_FEE_LINE_STATUS]
fields_screen: Liquidate line
fields:
  - [Released On, Date, Conditional, "-", Manual release; not in the future]
  - [Liquidated On, Date, "Yes", "-", ">= release date; not in the future"]
  - [Liquidation Report, File, "Yes", "-", Attachment]
notifications:
  - "None."
audit:
  - "Tags, dates and the attachment are kept on the line."
acceptance:
  - A line paid by Disbursement on 5 September shows Released on 5 September.
  - Liquidating without the report is refused.
```

## Cost centres and incentive income

```fr
id: FR-AC-054
title: Derive and enforce the cost centre of every entry
brd: [FRBS 3.1.1 (Add.2 p.5-6)]
actor: Comptrollership (rules); System
priority: Must have
fit: CHANGE
screens: Setup > Cost-Centre Rules; Accounting Engine (event log); Alerts
api: "GET/POST /api/v1/accounting/cost-center-rules; PUT .../{id}"
description: Accounts can require a cost centre. Comptrollership maintains cost-centre rules - source module, event type, sales unit, account officer - that give the cost centre of an entry. Before posting, the accounting engine fills a missing cost centre on a cost-centre-required line from the first matching rule. When none matches, the event is recorded FAILED with COST_CENTER_MISSING in the event register and the alert is raised, so the exception is logged and can be re-processed after the rule is added. Manual journals must carry the cost centre themselves.
preconditions:
  - "The user has ACCOUNTING_RULE_MANAGE or MASTER_MAINTAIN (rules)."
main_flow:
  - Comptrollership adds a rule with its criteria and cost centre.
  - An event posts to a cost-centre-required account without a cost centre.
  - The engine applies the rule and posts.
alternate_flows:
  - No rule. The event fails with COST_CENTER_MISSING and the alert is raised.
rules:
  - [R1, "The rules are tried in order; the first match gives the cost centre.", Configurable, Cost-Centre Rules]
  - [R2, "Which rules apply to which transactions is BDOI data (AQ26).", Configurable, Cost-Centre Rules]
validations:
  - [Rule without cost centre, Enter the cost centre of the rule, COST_CENTER_REQUIRED]
  - [Missing cost centre at posting, "<message naming the event and account>", COST_CENTER_MISSING]
fields_screen: Cost-Centre Rule
fields:
  - [Source module / Event type, List, "No", Modules; event types, "-"]
  - [Sales unit / Account officer, List, "No", Sales units; users, "-"]
  - [Cost centre, List, "Yes", Cost-centre dimension, Active]
  - [Priority, Number, "Yes", "-", "-"]
notifications:
  - Alert COST_CENTER_MISSING.
audit:
  - "Rule changes are audited; failed events stay in the register."
acceptance:
  - A service-fee accrual without a cost centre takes the cost centre of the matching rule.
  - A posting that no rule covers is not posted and raises COST_CENTER_MISSING.
```

```fr
id: FR-AC-055
title: Account for early incentives as Other Income
brd: [FRBS 3.1.2 (Add.2 p.6)]
actor: Comptrollership (rules); System
priority: Must have
fit: CONFIGURE
screens: Accounting rules; Chart of Accounts
api: Event OPS_REMIT_INCENTIVE (component INCENTIVE_INCOME)
description: The early-remittance incentive posts through its own event component, separate from commission and premium, to the incentive income account (demo 4130), so the commission accounts are not touched. Comptrollership maps the component to an Other Income account of the BDOI chart. The source remittance batch is kept on the entry. CPC2 incentives use their own account (demo 4131, Volume 2 FR-DS-090).
preconditions:
  - "The Other Income account exists in the chart."
main_flow:
  - A remittance batch with an early incentive is approved (BRD-2).
  - The engine posts the incentive component to the incentive income account.
rules:
  - [R1, "The incentive account is chosen by the rule, not by code.", Configurable, Accounting rules]
  - [R2, "Moving the demo account 4130 under Other Income (4700) is a chart change for FRBS, not done in the demo (AQ01).", Configurable, Chart of accounts]
validations: []
notifications:
  - "None."
audit:
  - "The journal carries the source batch reference."
acceptance:
  - The early incentive of a remittance batch posts to the incentive income account and not to commission income.
```

> [!NOTE] Related functions
> The automatic service invoice with 2% withholding for early incentives (DIS 3.29.1) and the CPC2 incentive (DIS 3.29.2) are built in Remittance and described in Volume 2 (FR-DS-090, FR-DS-091).

## Report pack

```fr
id: FR-AC-060
title: Generate the FRBS report pack
brd: [FRBS 3.2.0 (p.63); Appendix A (p.142-144)]
actor: FRBS users (FRBS_REPORT_VIEW / EXPORT)
priority: Must have
fit: CHANGE
screens: Accounting Reports > Report Pack
api: GET /api/v1/frbs/report-pack
description:
  - The Report Pack screen lists the reports of Appendix A by group - I End of Day, II GARD (bank format), III Subsidiaries Accounting, IV Schedules and Aging, V Performance / Mancom, VI Service Fee, VII Government - with 58 entries delivered (section 6.2). Each entry opens its report runner and exports at once to Excel or PDF; the reports also run in report batches (FR-AC-022).
  - End-of-day reports are the statement of condition, income statement, trial balance, journal entries (day book) and subsidiary ledger; the cash receipts and cash disbursements books are day-book variants and BIR books (FR-AC-063).
preconditions:
  - "The user has FRBS_REPORT_VIEW."
main_flow:
  - The user opens Report Pack and chooses a group and a report.
  - The user enters the parameters and runs or exports it.
rules:
  - [R1, "Layouts of the GARD, subsidiaries and Mancom reports are drafts until BDOI provides them (AQ05).", Configurable, Schedule definitions; report pack]
  - [R2, "Reports flagged Word requested are delivered in PDF and Excel; the report platform has no Word renderer.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Every run and export is archived."
acceptance:
  - The Report Pack lists the seven Appendix A groups with their reports.
  - The GARD Schedule of Operating Expenses exports to Excel and PDF.
```

> [!WARNING] Gap G1 - Word output
> BDOI asks for the board-deck schedules (GARD, subsidiaries, Mancom) in Word. The report platform renders PDF, Excel, ODS, CSV and XML only, so these reports are issued in PDF and Excel until a Word renderer is added to the report platform.

```fr
id: FR-AC-061
title: Run and maintain configurable account schedules
brd: [FRBS 3.2.0 (p.63); Appendix A II-IV (p.142-143)]
actor: GL team (run); GL Team Lead (MASTER_MAINTAIN, definitions)
priority: Must have
fit: CHANGE
screens: Accounting Reports > Account Schedules (run, commentary, definitions editor)
api: "GET/POST/PUT /api/v1/finreport/schedules; PUT .../{code}/comments; report GL-SCHEDULE"
description:
  - Most Appendix A schedules are definitions of one engine, the report GL-SCHEDULE. A definition selects the postable accounts by code prefix or report group, groups the posted ledger by account, party, party and document, cost centre, branch or business line, in base currency or in one currency, and shows the chosen figures - opening, debits, credits, movement, closing, a comparative (previous month or year), the variance and variance %. Optional ageing spreads the balance over up to eight buckets, first in first out. An optional commentary column prints the month's comment per row.
  - 28 definitions are delivered (GARD-*, SUBS-*, SCH-*), all marked TO_CONFIRM. FRBS adds or changes definitions without a release and writes the monthly commentary.
preconditions:
  - "The user has FRBS_REPORT_VIEW (run), FRBS_REPORT_EXPORT (commentary) or MASTER_MAINTAIN (definitions)."
main_flow:
  - The user opens Account Schedules and chooses a schedule and the as-of date.
  - BIBS runs the definition and shows the rows.
  - The user writes the commentary of the month and exports.
alternate_flows:
  - New schedule. The TL creates a definition with its selector, grouping, figures and ageing.
rules:
  - [R1, "Account selectors point at the demo chart until BDOI's chart is uploaded (AQ01).", Configurable, Schedule definitions]
  - [R2, "Only a balance schedule can be aged; up to 8 buckets.", Fixed, "-"]
validations:
  - [No accounts selected, List at least one account prefix or report group, SCHEDULE_ACCOUNTS]
  - [Definition incomplete, "Give the name, family, selector, grouping, side and basis", SCHEDULE_INCOMPLETE]
  - [Invalid code, "Use 2 to 40 upper-case letters, digits or dashes for the code", SCHEDULE_CODE]
  - [No figure chosen, Choose at least one figure, SCHEDULE_COLUMNS]
  - [Comparative without period, Choose a comparative period for the comparative figures, SCHEDULE_COMPARATIVE]
  - [Ageing on a movement schedule, Only a balance schedule can be aged, SCHEDULE_AGEING_BASIS]
  - [Comment on a schedule without commentary, "Schedule <code> has no commentary column", SCHEDULE_NO_COMMENTARY]
  - [Comment too long, "A comment has at most <n> characters", COMMENT_TOO_LONG]
fields_screen: Schedule definition
fields:
  - [Code / Title / Family, Text / List, "Yes", "GARD, SUBS, SCH", Code 2-40 characters]
  - [Accounts By, List, "Yes", "Code prefix, Report group", "-"]
  - [Accounts, Text, "Yes", "-", At least one]
  - [Grouping, List, "Yes", "Account, party, party and document, cost centre, branch, business line", "-"]
  - [Basis, List, "Yes", "Balance, Movement", "-"]
  - [Positive Side, List, "Yes", "Debit, Credit", "-"]
  - [Currency, Text, "No", "-", 3-letter code]
  - [Comparative, List, "No", "Previous month, previous year", "-"]
  - ["Figures, in column order", Multi-select, "Yes", "Opening, debits, credits, movement, closing, comparative, variance, variance %", Each once]
  - [Ageing Buckets, Text, "No", "-", Up to 8; balance schedules only]
notifications:
  - "None."
audit:
  - "Definition changes and comments are recorded with user and time."
acceptance:
  - Every delivered definition runs and exports to Excel and PDF.
  - The aged schedule of premium receivable splits each balance into its buckets and the buckets add up to the balance.
```

```fr
id: FR-AC-062
title: Produce the Mancom, production, GAP, cash-flow and expense reports
brd: [FRBS 3.2.0 (p.63); Appendix A IV-V (p.143-144)]
actor: FRBS users
priority: Must have
fit: NEW
screens: Accounting Reports > Report Pack (groups IV, V, VI)
api: "Reports FRBS-MANCOM-MARKET, FRBS-BRANCH-PRODUCTION, FRBS-BRANCH-PRODUCTION-SUM, FRBS-EXPENSE-GROUPING, FRBS-GAP, FRBS-CASH-FLOW, FRBS-SUSTAINABILITY-PROD, FRBS-SERVICE-FEE, FRBS-SERVICE-FEE-DETAIL"
description:
  - The reports that need broking data are built in the FRBS module - the market performance summary (premium and commission per segment and location, month, year to date, previous year to date, growth), branch production (detailed and summary), expenses per cost centre and account, the GAP report (open sub-ledger receivables and payables by time to maturity, gap and cumulative gap), the cash-flow report (opening cash and bank, receipts and payments per journal type, closing) and the service-fee summary and detail.
preconditions:
  - "The user has FRBS_REPORT_VIEW."
main_flow:
  - The user runs the report for the period and exports it.
rules:
  - [R1, "Sales units stand in for BDO branch codes in branch production until AQ05.", Configurable, Sales organisation]
  - [R2, "Segment budgets for the market performance summary are to be provided (AQ05).", Configurable, Budget]
validations: []
notifications:
  - "None."
audit:
  - "Report runs are archived."
acceptance:
  - The market performance summary of August shows the premium and commission of each segment with year to date and growth.
  - The GAP report totals match the open items of the sub-ledger.
```

```fr
id: FR-AC-063
title: Produce the BIR returns, alphalists, SAWT and books of accounts
brd: [FRBS 3.2.0 (p.63); Appendix A VII (p.144)]
actor: FRBS users (TAX_VIEW)
priority: Must have
fit: "FIT (2550-Q, 0619-E, 1601-EQ, QAP), NEW / CHANGE (others)"
screens: Tax & Statutory > BIR Forms & Books; Tax Returns; Certificates Received
api: "Reports TAX-VAT-2550Q, TAX-EWT-1601EQ, TAX-QAP, TAX-MAP, TAX-1604E, TAX-SAWT, TAX-0619F, TAX-1603, TAX-1702Q, TAX-1702, TAX-BOOK-GJ / PJ / SJ / CRB / CDB / SL, IC-BROKER-ASBO"
description:
  - FRBS chooses a period and opens or exports each output - the VAT return 2550-Q with the relief lists, 1601-EQ and the quarterly, monthly and annual alphalists (QAP, MAP, 1604-E) from the withholding-tax worksheet, the SAWT from the register of certificates received (Volume 2, FR-DS-057), the 0619-F, 1603, 1702-Q (year to date) and 1702 worksheets from ledger movements of account prefixes and rates from parameters, the BIR books of accounts (general, purchase and sales journals, cash receipts and disbursements books, general ledger per account class with the balance forward) and the Insurance Commission Broker's Annual Statement of Business Operations.
preconditions:
  - "The user has TAX_VIEW (granted to the FRBS roles)."
main_flow:
  - The user opens BIR Forms & Books, chooses the period and the output.
  - The user opens it on screen or exports it to Excel or PDF.
rules:
  - [R1, "Rates - income tax TAX_RCIT_RATE 25%, fringe benefit TAX_FBT_RATE 35%.", Configurable, Parameters]
  - [R2, "The outputs are worksheets and loose-leaf books, not eFPS, DAT or CAS files; the final withholding tax account of 0619-F is not mapped (AQ07).", Fixed, "-"]
  - [R3, "HDMF, SSS, PhilHealth, 1601-C and 1604-C are payroll outputs and out of scope (AQ06); 2550-M only if BDOI still needs it (AQ07).", Fixed, "-"]
validations:
  - [Form without lines, "Form <code> has no lines", FORM_NOT_DEFINED]
  - [Form prepared outside BIBS, This form is prepared outside BrokerVerse and has no worksheet, NO_WORKSHEET]
  - [Period end before start, Tax period end precedes its start, INVALID_TAX_PERIOD]
notifications:
  - "None."
audit:
  - "Returns filed through Tax Returns keep maker-checker; report runs are archived."
acceptance:
  - The SAWT of Q3 lists every insurer certificate recorded for the quarter.
  - The Cash Disbursements book of August lists the payment journals of the month.
```

## Business and system administration

```fr
id: FR-AC-070
title: Maintain lists of values with approval
brd: [BASAU 2.2.0 (p.128), BASAU 2.2.1 (p.128), BASAU 2.2.2 (p.128), BASAU 2.2.3 (p.128), BASAU 2.2.4 (p.128), BASAU 2.2.5 (p.129)]
actor: Business Administrator (LOV_MANAGE); approver (MASTER_AUTHORIZE)
priority: Must have
fit: FIT
screens: Administration > Lists of Values; My Approvals
api: "GET /api/v1/lov/types, /lov/{type}/values; POST/PUT .../values; approval through My Approvals"
description: The Business Administrator views every list of values, adds values, edits them and deactivates them by an effective-to date. Each value has an effective-from and effective-to date. Additions and changes wait for approval by another user and apply from their effective date. The lists of BRD-5 are in section 9.3.
preconditions:
  - "The user has LOV_MANAGE; the list is maintainable."
main_flow:
  - The administrator opens Lists of Values and a list.
  - The administrator adds or edits a value with its effective dates.
  - The approver approves it in My Approvals; it is usable from its effective date.
rules:
  - [R1, "Lists maintained by the system cannot be changed on the screen.", Fixed, "-"]
  - [R2, "Values are never deleted; they end on their effective-to date.", Fixed, "-"]
validations:
  - [Effective-to before effective-from, The effective-to date is before the effective-from date, LOV_EFFECTIVITY_INVALID]
  - [System list, "The list <type> is maintained by the system", LOV_NOT_MAINTAINABLE]
  - [Approver is the maker, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
fields_screen: List value
fields:
  - [Code, Text, "Yes", "-", Unique in the list]
  - [Label, Text, "Yes", "-", "-"]
  - [Sort order, Number, "No", "-", "-"]
  - [Effective from / to, Date, "Yes / No", "-", To >= From]
notifications:
  - "Pending values appear in My Approvals of the approvers."
audit:
  - "Every change and approval is audited."
acceptance:
  - A new refund reason is usable only after another user approves it.
  - A value ended yesterday is no longer offered today.
```

```fr
id: FR-AC-071
title: Manage users and role profiles through approved requests
brd: [BASAU 2.3.0 (p.129), BASAU 2.3.1 (p.129), BASAU 2.3.2 (p.129), BASAU 2.3.3 (p.129), BASAU 2.4.0 (p.130), BASAU 2.4.2 (p.130)]
actor: System Administrator / Business Administrator (ACCESS_REQUEST); approver (ACCESS_APPROVE)
priority: Must have
fit: "FIT; CHANGE (2.3.2)"
screens: Administration > Access Requests; Users; Roles & Permissions; User Access Matrix
api: "GET/POST /api/v1/nbadmin/access-requests (CREATE_USER, MODIFY_ROLES, DISABLE_USER, MODIFY_ROLE_PERMISSIONS)"
description: User management follows the access matrix and goes through requests - create a user with roles, change the roles of a user (the group profiles), disable a user, and change the permissions of a role. The administrator raises the request with its justification; an approver who is not the requester decides it; BIBS applies it. The request list shows every request with its status. Roles act as the group profiles; a user may hold several roles.
preconditions:
  - "The requester has ACCESS_REQUEST."
main_flow:
  - The administrator opens Access Requests and creates a request of the needed type.
  - BIBS validates it and waits for approval.
  - The approver approves it; BIBS applies it and notifies the requester.
rules:
  - [R1, "One pending request per subject.", Fixed, "-"]
  - [R2, "A role-permission change applies the difference between the role at approval and the request.", Fixed, "-"]
validations:
  - [User name invalid, "The user name has 3 to 50 letters, digits, dots, dashes or underscores", ACCESS_USERNAME]
  - [User exists, "User <user> already exists", ACCESS_USER_EXISTS]
  - [No role, Select at least one role, ACCESS_ROLES]
  - [Unknown role or permission, "Unknown role(s) / permission(s) <list>", "ACCESS_UNKNOWN_ROLE, ACCESS_UNKNOWN_PERMISSION"]
  - [Permission added and removed, "Permission(s) both added and removed - <list>", ACCESS_PERMISSION_CONFLICT]
  - [No change, "The request does not change the permissions of role <role>", ACCESS_NO_PERMISSION_CHANGE]
  - [Pending request exists, "A request for <subject> is already waiting for approval", ACCESS_REQUEST_PENDING]
fields_screen: Access Request
fields:
  - [Type, List, "Yes", "Create user, Modify roles, Disable user, Modify role permissions", "-"]
  - [User name / Full name, Text, Conditional, "-", Create user]
  - [Roles, Multi-select, Conditional, Roles, At least one]
  - [Role and permissions to add / remove, List / Multi-select, Conditional, Roles; permissions, Modify role permissions]
  - [Justification, Text, "Yes", "-", "-"]
notifications:
  - The approvers are notified of a new request; the requester of the decision.
audit:
  - "Request, decision and applied change are recorded."
acceptance:
  - A request to give glofficer the role FRBS_TL takes effect only after another user approves it.
  - The User Access Matrix shows the changed role.
```

```fr
id: FR-AC-072
title: Approve, decline or return administration requests
brd: [BASAU 2.4.1 (p.130), BASAU 2.5.0 (p.130), BASAU 2.5.1 (p.131), BASAU 2.5.2 (p.131), BASAU 2.5.3 (p.131), BASAU 2.6.0 (p.131), BASAU 2.6.1 (p.131), BASAU 2.6.2 (p.131), BASAU 2.6.3 (p.132)]
actor: Approver (ACCESS_APPROVE, MASTER_AUTHORIZE)
priority: Must have
fit: "FIT; CHANGE (2.4.1, 2.5.3, 2.6.0, 2.6.1)"
screens: My Approvals; Access Requests
api: "POST /api/v1/nbadmin/access-requests/{id}/approve, /reject, /return, /resubmit; POST /api/v1/approvals/bulk-approve; GET /api/v1/approvals/inbox, /counts"
description: The approver sees the pending LOV and user-management requests in My Approvals with a count, and is notified of new ones. The approver approves or declines a request with remarks, or returns it to the requester with remarks; the requester corrects and resubmits it. Several requests can be approved at once through the bulk approval API - each is decided on its own; a new-user request is approved on its own because its temporary password is shown once.
preconditions:
  - "The approver is not the requester."
main_flow:
  - The approver opens My Approvals and a request.
  - The approver enters remarks and approves, declines or returns it.
  - BIBS applies the decision and notifies the requester.
alternate_flows:
  - Return. The request becomes RETURNED; the requester resubmits it with a new justification.
  - Bulk. Several requests are approved together with a result per request.
rules:
  - [R1, "A request is never decided by its submitter.", Fixed, "-"]
  - [R2, "Remarks are mandatory to decline or return.", Fixed, "-"]
validations:
  - [Approver is the requester, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
  - [Decline without reason, Enter the reason of the rejection, ACCESS_REJECT_REASON]
  - [Return without remarks, Enter the remarks for the requester, ACCESS_RETURN_REASON]
  - [Already decided, "Request <no> is already <status>", ACCESS_REQUEST_DECIDED]
  - [Resubmit by another user, "Only <user> can resubmit this request", ACCESS_NOT_REQUESTER]
  - [New user in bulk, "Approve <ref> on its own - the new user's temporary password is shown once", ACCESS_APPROVE_INDIVIDUALLY]
fields_screen: Decision
fields:
  - [Remarks, Long text, Conditional, "-", Required to decline or return]
notifications:
  - The requester is notified of approval, decline or return; approvers of new requests.
audit:
  - "Every decision is recorded with user, time and remarks."
acceptance:
  - A returned request shows RETURNED with the remarks and is resubmitted by its requester.
  - The requester cannot approve their own request.
```

> [!NOTE] Gap G2
> Return, resubmit and bulk approval work through the API. The Access Requests screen and My Approvals do not show the Return, Resubmit and bulk-approve buttons yet; the screens offer approve and decline one at a time.

# Workflows, statuses and accounting events

## Manual journal

Figure 2 shows the statuses of a manual journal. Solid arrows are the main path, dashed arrows are returns, dotted arrows are system actions.

![Statuses of a manual journal (FRBS 2.5.x, 2.8.x)](figures/brd05_v1_journal_states.dot){width=12}

<!-- table: widths=3.4,6.6,6.6 caption="Journal statuses" status=Status size=8.5 -->
| Status | Meaning | Next |
|---|---|---|
| DRAFT | Being prepared by the maker | Submit (PENDING_APPROVAL) or cancel |
| PENDING_APPROVAL | Submitted; assigned or open to posters | Post (POSTED) or return (REJECTED) |
| REJECTED | Returned to the maker with remarks | Edit and submit again, or cancel |
| POSTED | In the ledger; immutable | Reverse (REVERSED), manually or on the reverse-on date |
| REVERSED | Cancelled in the ledger by a linked reversal journal | - |
| CANCELLED | Withdrawn before posting | - |

## GL close and broking-books cut-off

![Month-end sequence: broking-books cut-off, adjustments, GL close (FRBS 2.6.0, 3.4.0)](figures/brd05_v1_closing.dot){width=14}

<!-- table: widths=3.8,6.2,6.6 caption="Statuses of a scheduled close" status=Status size=8.5 -->
| Status | Meaning | Set by |
|---|---|---|
| SCHEDULED | Waiting for its date and time | The TL (schedule) |
| COMPLETED | Checklist passed; the period is closed | Job GL_PERIOD_CLOSE |
| FAILED | Checklist or close refused; blocking items recorded; GL_CLOSE_FAILED raised | Job GL_PERIOD_CLOSE |
| CANCELLED | Withdrawn before its time | The TL |

Accounting periods move FUTURE, OPEN, CLOSING, CLOSED and REOPENED; a period of a closed fiscal year cannot be reopened.

## Service-fee run (FRBS_SERVICE_FEE)

![Workflow FRBS_SERVICE_FEE (FRBS 2.10.x)](figures/brd05_v1_service_fee.dot){width=10}

<!-- table: widths=3.2,3.4,3.2,4.2,2.6 caption="Transitions of FRBS_SERVICE_FEE" size=8 -->
| From | Action | To | Permission | Reason list |
|---|---|---|---|---|
| COMPUTED | submit | FOR_APPROVAL | SERVICE_FEE_MANAGE | - |
| COMPUTED | cancel | CANCELLED | SERVICE_FEE_MANAGE | VOID_REASON |
| FOR_APPROVAL | approve | APPROVED | SERVICE_FEE_APPROVE (not the preparer) | - |
| FOR_APPROVAL | return | COMPUTED | SERVICE_FEE_APPROVE | RETURN_REASON |
| APPROVED | release (system when all paid lines are released) | RELEASED | SERVICE_FEE_TAG | - |
| RELEASED | liquidate (system when all lines are liquidated) | LIQUIDATED | SERVICE_FEE_TAG | - |

SLA hours: COMPUTED 48, FOR_APPROVAL 24.

## Access requests

![Access request statuses (BASAU 2.3.x-2.6.x)](figures/brd05_v1_access_request.dot){width=11}

<!-- table: widths=3.4,13.2 caption="Access request statuses" status=Status size=8.5 -->
| Status | Meaning |
|---|---|
| PENDING | Waiting for an approver other than the requester |
| APPROVED | Applied to the user or role |
| REJECTED | Declined with a reason |
| RETURNED | Returned with remarks; the requester corrects and resubmits it (back to PENDING) |

## Accounting events of BRD-5

Every posting of BRD-5 is a business event with components and account roles; the accounts come from the rules maintained by Comptrollership. The entries below are the demo rules; the real ones are BDOI's (AQ02, OQ07).

<!-- table: widths=0.8,4.2,4.4,5.4,1.8 caption="Accounting events and demo entries" size=8 -->
| # | Transaction | Event (source reference) | Demo entry | Build |
|---|---|---|---|---|
| 1 | DV approved - remittance to insurer | DISB_VOUCHER REMITTANCE (DV:<no>) | Dr 2211 due to insurer for disbursement / Cr paying bank, or 2241 checks outstanding for checks | Built |
| 2 | DV approved - refund to client | DISB_VOUCHER REFUND | Dr 2216 refund payable / Cr paying account | Built |
| 3 | DV approved - refund received from insurer | DISB_VOUCHER REFUND_FROM_INSURER | Dr 2217 AP refund from insurer / Cr paying account | Built |
| 4 | DV approved - supplier, government, other bank unit | DISB_VOUCHER SUPPLIER / GOVERNMENT / OTHER_BANK_UNIT | Dr payable of the request / Cr paying account; Cr EWT payable when withheld | Built |
| 5 | DV approved - employee, cash advance | DISB_VOUCHER EMPLOYEE / CASH_ADVANCE | Dr 1604 advances to employees or the expense (cost centre) / Cr paying account | Built |
| 6 | DV approved - service fee, pass-on | DISB_VOUCHER SERVICE_FEE / PASS_ON | Dr 2250 / 2230 / Cr paying account | Built |
| 7 | Proforma edited | DISB_VOUCHER (edited lines) | The edited lines, validated and balanced | Built |
| 8 | Check negotiated | DISB_CHECK_NEGOTIATED (CHK:<id>:NEG) | Dr 2241 checks outstanding / Cr bank | Built |
| 9 | Check staled | DISB_CHECK_STALE (CHK:<id>:STALE) | Dr 2241 / Cr 2240 Miscellaneous Liability - stale checks | Built |
| 10 | Re-issue of a stale check | DISB_VOUCHER STALE_REISSUE | Dr 2240 / Cr paying account | Built |
| 11 | Approved DV cancelled | DISB_VOUCHER negative (DV:<no>:CANCEL) | Reversal of rows 1-6 | Built |
| 12 | Funding of the main account | DISB_FUND_TRANSFER (FND:<no>) | Dr target bank / Cr source bank | Built |
| 13 | Insurer certificate received | TAX_CWT_CERT_RECEIVED (CRT:<id>) | Dr 1612 AR-BIR on hand / Cr 1610 AR-BIR on commission or 1611 on incentives | Built |
| 14 | CPC2 incentive per remittance batch | OPS_REMIT_CPC2 (RMB:<batch>:CPC2) | Dr 2211 / Cr 4131 CPC2 incentive income, Cr 2504 output VAT | Built; base parked (AQ24) |
| 15 | Early incentive with service invoice | OPS_REMIT_INCENTIVE + service invoice EARLY_INCENTIVE | Dr 2211 / Cr 4130 incentive income (Other Income) + output VAT | Built; the 2% WTAX entry (Dr 1611 / Cr 2211) parked (AQ25) |
| 16 | Remittance deduction on insurer confirmation | OPS_REMIT_DEDUCTION (RMB:<batch>:<deduction no>) | Dr 2211 / Cr 1225 AR insurer's refund | Built |
| 17 | ACSL correction | System journal ADJUSTMENT (ACS:<no>) | Reversal of the wrong line and re-post to the right account | Built |
| 18 | Service-fee accrual | FRBS_SERVICE_FEE_ACCRUE (<run>:<line>) | Dr 5614 service fee expense (cost centre) / Cr 2250 service fee payable | Built |
| 19 | Cash-advance liquidation | PRQ_CA_LIQUIDATION (LIQ:<no>) | Dr expenses by category / Cr 1604; excess or shortage | Built |
| 20 | Accrual auto-reversal | System journal REVERSAL (JV:<no>:AUTOREV) | Mirror of the accrual | Built |

# Reports and documents

## Reports of this volume

<!-- table: widths=4.8,4.6,5.2,2 caption="Accounting reports" size=8.5 -->
| Code | Name | Content | BRD |
|---|---|---|---|
| FIN-MIS-BS, FIN-MIS-IE | Statement of Condition, Income Statement | Configurable statement formats with comparatives | FRBS 3.2.0 (I) |
| FIN-TB-MAIN | Trial Balance | Opening, movement and closing per account | FRBS 3.2.0 (I) |
| FIN-GL-DAYBOOK, FIN-GL-SUBLEDGER-LC | Journal Entries, Subsidiary Ledger | Posted lines per day; sub-ledger per party | FRBS 3.2.0 (I) |
| GL-SCHEDULE | Account schedule | Any schedule definition (FR-AC-061) | FRBS 3.2.0 (II-IV) |
| GL-FXREV | Revaluation Report | FX revaluation register | FRBS 3.5.0 |
| FIN-BRS-STMT, FIN-BRS-UNREC-BOOK, FIN-BRS-UNREC-BANK | Bank reconciliation | Statement and unmatched items | FRBS 3.3.3 |
| FRBS-MANCOM-MARKET | Market Performance Summary | Premium and commission per segment and location; month, YTD, previous YTD, growth | FRBS 3.2.0 (V) |
| FRBS-BRANCH-PRODUCTION, -SUM | Branch Production | Detailed and summary production per branch (sales unit) | FRBS 3.2.0 (V) |
| FRBS-EXPENSE-GROUPING | Expense Allocation per Cost Centre | Expenses per cost centre and account | FRBS 3.2.0 (IV) |
| FRBS-GAP | GAP Report | Open receivables and payables by time to maturity; gap and cumulative gap | FRBS 3.2.0 (IV) |
| FRBS-CASH-FLOW | Cash Flow Report | Opening cash and bank, receipts and payments per journal type, closing | FRBS 3.2.0 (IV) |
| FRBS-SERVICE-FEE, -DETAIL | Service Fee Report | Summary per segment; detail per invoice | FRBS 2.10.0 |
| FRBS-SUSTAINABILITY-PROD | Sustainability Report | Yearly production per client type, region and line | Report List #169 |
| TAX-* , IC-BROKER-ASBO | Government outputs | Section 6.2 group VII | FRBS 3.2.0 (VII) |
| ORG-HEADCOUNT-CC | Headcount per Cost Centre | Employees per cost centre (Volume 2, FR-DS-037) | DIS 3.30.2 |

All reports need their view permission, export to PDF, XLSX, ODS and CSV, are archived, and run in report batches.

<!-- pagebreak -->

## Report pack delivered (Appendix A)

The table is the catalogue of the Report Pack screen (58 entries). "Word" marks the reports BDOI asked for in Word, delivered in PDF and Excel (Gap G1). Layouts marked TO_CONFIRM wait for AQ05.

<!-- table: widths=1.5,4.2,6.4,3,1.5 caption="Report pack entries by Appendix A group" size=7.5 -->
| Group | Report / definition | Title | Source | Word |
|---|---|---|---|---|
| I | FIN-MIS-BS | Statement of Condition | App. A I; list #1 | - |
| I | FIN-MIS-IE | Income Statement | App. A I; list #2 | - |
| I | FIN-TB-MAIN | Trial Balance | App. A I; list #3 | - |
| I | FIN-GL-DAYBOOK | Journal Entries | App. A I; list #4 | - |
| I | FIN-GL-SUBLEDGER-LC | Subsidiary Ledger | App. A I; list #5 | - |
| II | GARD-CASH | Schedule of Cash and Cash Equivalents | App. A II | Word |
| II | GARD-OPEX | Schedule of Operating Expenses | App. A II | Word |
| II | GARD-OTHER-INCOME | Schedule of Other Income | App. A II | Word |
| II | GARD-FX | Schedule of Foreign Exchange Gain / (Loss) | App. A II | Word |
| II | GARD-MISC-ASSETS | Schedule of Miscellaneous Assets | App. A II | Word |
| II | GARD-ACCRUED | Schedule of Accrued Expenses and Taxes Payable | App. A II | Word |
| II | GARD-VARIANCE-SIE | Variance Analysis SIE with Commentary | App. A II-45 | Word |
| III | SUBS-SIE-YOY | FS Analysis SIE - Year on Year | App. A III | Word |
| III | SUBS-SOC-MOM | FS Analysis SOC - Month on Month | App. A III | Word |
| III | GL-BVA | FS Analysis - Actual vs Budget | App. A III | Word |
| IV | SCH-COMMISSION-INCOME | Schedule of Commission Income | App. A IV; list #9 | - |
| IV | SCH-PR-PHP | Schedule of Receivable from Insurance Companies Clients - Peso | App. C; list #45 | - |
| IV | SCH-PR-USD | Schedule of Receivable from Insurance Companies Clients - USD | App. C; list #46 | - |
| IV | SCH-PR-2307 | Schedule of Premiums Receivable 2307 | App. A IV-32 | - |
| IV | SCH-COMM-RECEIVABLE | Schedule of Commission Receivables | App. A IV | - |
| IV | SCH-AR-BIR | Schedule of AR-BIR | App. A IV-6 to 8 | - |
| IV | SCH-AR-BIR-INSURER | Schedule of AR-BIR per Insurer | App. A IV | - |
| IV | SCH-AR-INSURER-REFUND | Schedule of AR Insurer's Refund | App. C | - |
| IV | SCH-AR-OFFICERS | Schedule of AR Officers and Employees | App. A IV | - |
| IV | SCH-PREPAID | Schedule of Prepaid Expenses | App. A IV | - |
| IV | SCH-FFE | Schedule of Furniture, Fixtures and Equipment | App. A IV | - |
| IV | SCH-PAY-INSURERS | Schedule of Payable to Insurance Companies | App. C | - |
| IV | SCH-AP-REFUND-INS | Schedule of A/P Refund from Insurer | App. C | - |
| IV | SCH-AP-OTHERS | Schedule of AP - Others Current | App. A IV-18/19 | - |
| IV | SCH-AP-OFFICERS | Schedule of AP - Officers and Employees | App. A IV | - |
| IV | SCH-STALE-CHECKS | Schedule of Stale Checks | App. B | - |
| IV | SCH-CHECKS-OUTSTANDING | Checks and Other Cash Items | App. A IV | - |
| IV | SCH-SERVICE-FEE-PAYABLE | Schedule of Service Fee Payable | App. A VI | - |
| IV | SCH-EXPENSE-CC | Schedule of Expenses per Cost Centre | App. A IV | - |
| IV | FRBS-EXPENSE-GROUPING | Expense Allocation per Cost Centre | App. A IV; list #10 | - |
| IV | FRBS-GAP | GAP Report | App. A IV | - |
| IV | FRBS-CASH-FLOW | Cash Flow Report | App. A IV | - |
| IV | FIN-BRS-STMT | SA / CA Reconciliation | App. A IV; list #11-12 | - |
| IV | GL-FXREV | Revaluation Report | App. A IV; list #14 | - |
| V | FRBS-MANCOM-MARKET | Market Performance Summary | App. A V; list #15 | Word |
| V | FRBS-BRANCH-PRODUCTION | Branch Production - Detailed | App. A V; list #16 | - |
| V | FRBS-BRANCH-PRODUCTION-SUM | Branch Production - Summary | App. A V; list #16 | - |
| V | FRBS-SUSTAINABILITY-PROD | Sustainability Report - Risk Management | List #169 | - |
| VI | FRBS-SERVICE-FEE | Service Fee Report - Summary | App. A VI; list #17 | - |
| VI | FRBS-SERVICE-FEE-DETAIL | Service Fee Report - Details | App. A VI; list #17 | - |
| VII | TAX-VAT-2550Q | VAT Return 2550-Q | App. A VII; list #29 | - |
| VII | TAX-EWT-1601EQ | Expanded Withholding Tax 1601-EQ | App. A VII | - |
| VII | TAX-QAP | Quarterly Alphalist of Payees | App. A VII | - |
| VII | TAX-MAP | Monthly Alphalist of Payees | App. A VII; list #28 | - |
| VII | TAX-1604E | Annual Information Return 1604-E | App. A VII | - |
| VII | TAX-SAWT | Summary Alphalist of Withholding Taxes | App. A VII; list #30 | - |
| VII | TAX-0619F | Final Withholding Tax Remittance 0619-F | App. A VII | - |
| VII | TAX-1603 | Fringe Benefit Tax 1603 | App. A VII | - |
| VII | TAX-1702Q | Quarterly Income Tax Return 1702-Q | App. A VII | - |
| VII | TAX-1702 | Annual Income Tax Return 1702 | App. A VII | - |
| VII | TAX-BOOK-GJ, -PJ, -SJ, -CRB, -CDB | Books of Accounts - General, Purchase, Sales, Cash Receipts, Cash Disbursements | App. A VII; list #18-22 | - |
| VII | TAX-BOOK-SL | Books of Accounts - General Ledger by Class | App. A VII; list #23-27 | - |
| VII | IC-BROKER-ASBO | Broker's Annual Statement of Business Operations | App. A VII; list #31 | - |

Appendix A reports not in the pack: the IFRS 16 lease, DTA, ECL / NUGL and placement schedules need data BIBS does not hold (AQ05); HDMF, SSS, PhilHealth, 1601-C and 1604-C are payroll outputs (AQ06, out of scope); 2550-M waits for AQ07. Any further GL schedule of groups II-IV is a new definition of the schedule engine, without a release.

## Documents

Volume 1 produces no customer documents. The service-fee payout advice, disbursement vouchers and bank forms are Volume 2 documents.

# Interfaces and integration

Figure 6 shows the interfaces of Accounting. The general ledger is inside BIBS: every module posts through the accounting engine, so there is no accounting export (OQ01).

![Interfaces of Accounting (dashed = external or parked)](figures/brd05_v1_integration.dot){width=11.5}

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Business modules (booking, Operations, Collections, Disbursement, Payment Requests, ACSL, FRBS) | In | Business events posted by the accounting engine | FRBS 3.1.0 | BUILT |
| Disbursement | Out / In | Service-fee payouts (SERVICE_FEE) and their status (PAID, RETURNED, CANCELLED) | FRBS 2.10.x | BUILT |
| Bank files | In | Cash-in-bank statements (XLSX, ODS, CSV) per layout | FRBS 3.3.1 | BUILT |
| Report archive and batches | Out | Report files, ZIP and merged PDF | FRBS 2.4.x | BUILT |
| BIR (eFPS, eBIRForms, CAS) | Out | Worksheets and loose-leaf books; no electronic filing | FRBS 3.2.0 | PARKED |
| BDO Unibank (GARD) | Out | Reports exported in PDF and Excel; submission channel not defined | FRBS 3.2.0 | PARKED |
| Payroll | In | HDMF, SSS, PhilHealth, 1601-C, 1604-C | Appendix A VII | OUT |

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.133-139; Add.1 p.37)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Accounting 5 (GL officer, TL, TH); Business / System Admin 7 | Within the BRD-1 sizing (145 concurrent) | FIT |
| Volumes | Manual adjustments about 100 a year; 6 GL closings | Small; no special tuning | FIT |
| Response time | Screen load 5-10 s, refresh 5 s, field display 2 s, save 5 s; reports 10-20 s first load, 10 s next | Online p95 under 3 s; heavy reports in batches | FIT |
| Peaks | Month end and year end; 10:00-15:00 | Close and reversal jobs outside the peak | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Availability (Add.1) | 100%; use 07:00-18:00 Monday to Saturday; downtime under 24 hours; maintenance 19:00-07:00 | Same deployment as BRD-1; 100% is not a measurable SLA; one BIBS-wide NFR set is being agreed (AQ27, XQ08) | OPEN |
| Retention (Add.1) | Reports and vouchers 5 years online, 5 years archive; daily backup kept 5 years | Retention rules of BRD-1 with a document class for vouchers and generated reports | FIT |
| Security and audit | Authorised users only; maker-checker | Role-based access, four-eyes rules and audit on every change (section 3) | FIT |

# Configuration items owned by the business and the System Administrator

## Parameters

<!-- table: widths=6.2,2.8,7.6 caption="Accounting parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| OPS_BOOK_RATE_SOURCE | CLOSING_PREV_MONTH | BOOK rate of a month = revaluation rate of the previous month end; MANUAL keeps BOOK rates by hand |
| CLOSE_ONLY_PREVIOUS_MONTH | true | Month-end close only for the previous month |
| YEAR_END_CLOSE_DEADLINE | 04-15 | Month and day by which the previous year is closed |
| BROKING_CLOSE_TIME | 23:00 | Time of the broking-books close on the last day of the month |
| BROKING_SOURCE_MODULES | booking, Operations, cashiering, remittance, adjustment, commission, disbursement | Modules cut off by the broking-books close |
| SESSION_IDLE_WARNING_MINUTES | 15 | Inactivity warning |
| SESSION_EXPIRY_WARNING_MINUTES | 30 | Warning before the forced log-out |
| TAX_RCIT_RATE / TAX_FBT_RATE | 25 / 35 | Income tax and fringe benefit tax rates of the BIR worksheets |
| AGEING_BUCKETS | platform default | BDOI buckets 30, 90, 180, 365, 730 once BDOI confirms (OQ43) |

## Jobs

<!-- table: widths=5,4.2,7.4 caption="Accounting jobs (Manila time)" size=8.5 -->
| Job | Schedule | Purpose |
|---|---|---|
| JOURNAL_AUTO_REVERSAL | Daily 00:05 | Reverse accruals on their reverse-on date |
| GL_PERIOD_CLOSE | Every 15 minutes | Run the scheduled GL closes that are due |
| BROKING_BOOKS_CLOSE | Last day of the month 23:00 | Close the broking books |
| BOOK_RATE_FROM_CLOSING | First day of the month 00:30 | Copy the revaluation rates of the month that ended as the new month's BOOK rates |

## Lists of values

<!-- table: widths=5,11.6 caption="Lists of values of Volume 1" size=8.5 -->
| List | Values delivered |
|---|---|
| SERVICE_FEE_SEGMENT | CBG; IBG (Institutional Banking Group); Others (to confirm, AQ20) |
| RETURN_REASON, VOID_REASON | Shared platform lists (journal return, run cancellation) |
| Volume 2 lists | PAYEE_CLASS, DISBURSEMENT_TYPE, DISB_CANCEL_REASON, DISB_RETURN_REASON, BRANCH_EMAIL, REFUND_REASON, RRF_CATEGORY_A / B, PRQ_PAYMENT_MODE, PRQ_RFP_TYPE, ACSL_CASE_TYPE, ACSL_CORRECTION_KIND, REMIT_DEDUCTION_SOURCE |

## Alerts

<!-- table: widths=5,2.4,9.2 caption="Accounting alerts" size=8.5 -->
| Code | Severity | When |
|---|---|---|
| COST_CENTER_MISSING | Medium | A posting to a cost-centre-required account found no rule |
| YEAR_END_CLOSE_DUE | High | The previous year is open 15 days before the deadline |
| GL_CLOSE_FAILED | High | A scheduled GL close or the broking-books close did not complete |

## Masters and rules maintained by the business

<!-- table: widths=5,5,6.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Chart of accounts, numbering schemes, short codes, negative-balance policy | GL Team Lead (GL Team Head) | FR-AC-011-014, 034 |
| Monthly revaluation rate | GL Team Head | FR-AC-010 |
| Accounting rules | Comptrollership (maker-checker) | FR-AC-030 |
| Cost-centre rules | Comptrollership | FR-AC-054 |
| Bank statement layouts | GL team | FR-AC-050 |
| Service-fee rules and recipients | GL Team Lead / Head | FR-AC-052 |
| Schedule definitions and commentary | GL Team Lead (MASTER_MAINTAIN); GL team | FR-AC-061 |
| Lists of values | Business Administrator (approver) | FR-AC-070 |
| Users, roles, permissions | System / Business Administrator (access approver) | FR-AC-071 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-AC-01 | The general ledger of BDOI is kept in BIBS; there is no accounting interface to another system | OQ01 |
| A-AC-02 | BDOI's chart and accounting rules are loaded as configuration at go-live; the demo chart is a placeholder | AQ01, AQ02 |
| A-AC-03 | The monthly revaluation rate is the month-end CLOSING rate and becomes the next month's BOOK rate | AQ03, OQ08 |
| A-AC-04 | "Broking books" are the postings of the broking modules; "GL books" the whole ledger closed by FRBS | AQ04 |
| A-AC-05 | The service fee is 2.5% (CBG) or 1% (IBG) of fully paid commission net of the insurer's withholding tax | AQ20 |
| A-AC-06 | Payroll outputs are prepared outside BIBS | AQ06 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-AC-01 | BDOI provides its chart, numbering convention and accounting rules | FR-AC-011-013, 030 (AQ01, AQ02) |
| D-AC-02 | BDOI provides the GARD, subsidiaries and Mancom layouts and segment budgets | FR-AC-060-062 (AQ05) |
| D-AC-03 | BDOI provides the bank file layouts | FR-AC-050 (AQ08) |
| D-AC-04 | BDOI confirms the service-fee rates and recipients | FR-AC-052 (AQ20) |
| D-AC-05 | BDOI gives the cost-centre rules | FR-AC-054 (AQ26) |
| D-AC-06 | The report platform owner adds a Word renderer | FR-AC-060 (Gap G1) |
| D-AC-07 | The broking modules report their pending items to the cut-off | FR-AC-042 |
| D-AC-08 | The administration screens add Return, Resubmit and bulk approval | FR-AC-072 (Gap G2) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions of Volume 1" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| AQ01 | BDOI's chart, its mapping, numbering, short codes; PHP and USD as separate accounts? | FR-AC-011-014 | OPEN |
| AQ02 | Default entries of every BRD-5 transaction | FR-AC-030; section 5.5 | OPEN |
| AQ03 | Source of the revaluation rate; BOOK rate; daily or monthly revaluation | FR-AC-010, 043 | OPEN |
| AQ04 | Broking books versus GL books; agreed close time; reopening; 15 April deadline | FR-AC-040-042 | OPEN |
| AQ05 | Layouts of GARD, subsidiaries, Mancom; non-GL data; segment budgets | FR-AC-060-062 | PARTIAL |
| AQ06 | Payroll reports out of scope or imported | FR-AC-063 | OPEN |
| AQ07 | 2550-M; formats of MAP, SAWT, 0619-F, 1603, 1702, 1604-E; BIR books format | FR-AC-063 | OPEN |
| AQ08 | Bank file layouts; matching without check numbers | FR-AC-050 | OPEN |
| AQ20 | Service-fee definition, rates, recipients, liquidation content | FR-AC-052, 053 | OPEN |
| AQ26 | Cost-centre rules; cost centre per employee or per unit | FR-AC-054 | OPEN |
| AQ27 | NFR set (availability, hours, retention) | Section 8 | OPEN |
| AQ28 | Role matrices | Section 3 | PARTIAL |
| AQ30 | Accounts that may never be negative; warning or block | FR-AC-034 | OPEN |
| AQ31 | GARD submission to BDO Unibank (file or print); reciprocal accounts | FR-AC-060 | OPEN |

AQ05 and AQ28 are partly answered by the Report List of BRD-12 and the role sections of BRD-6 to BRD-12 (R6).

# Traceability

Every Volume 1 requirement is met by at least one FR. The Build column gives the delivery state: **Built**; **Built, parked** (built; configuration or content waits for BDOI); **Built, gap** (built with a recorded gap, G1 or G2).

## FRBS (Accounting)

<!-- table: widths=2.2,2.2,2.6,4.6,4.8,1.9 caption="FRBS requirement IDs to FR, screen and build status" size=7.5 -->
| BRD ID | Page | FR | Screen | API | Build |
|---|---|---|---|---|---|
| FRBS 1.1.0 | p.50 | FR-AC-001 | Login; menu | auth/login | Built |
| FRBS 1.1.1 | p.50 | FR-AC-001 | Login; menu | auth/login | Built |
| FRBS 1.1.2 | p.50 | FR-AC-002 | Session dialog | Session parameters | Built |
| FRBS 1.1.3 | p.50 | FR-AC-002 | Session dialog | Session parameters | Built |
| FRBS 2.2.0 | p.51 | FR-AC-010 | Currencies & Rates | /currencies/revaluation-rates | Built |
| FRBS 2.3.0 | p.51 | FR-AC-011 | Chart of Accounts | /coa/accounts | Built, parked |
| FRBS 2.3.1 | p.51 | FR-AC-012 | Chart Upload | /coa/uploads | Built |
| FRBS 2.3.2 | p.52 | FR-AC-013 | Chart of Accounts (Numbering) | /coa/numbering | Built |
| FRBS 2.3.3 | p.52 | FR-AC-014 | Chart of Accounts; New Journal | /coa/accounts/lookup | Built |
| FRBS 2.3.4 | p.53 | FR-AC-011 | Chart of Accounts | /coa/accounts | Built |
| FRBS 2.3.5 | p.53 | FR-AC-011 | Chart of Accounts | /coa/accounts | Built |
| FRBS 3.6.0 | p.65 | FR-AC-010, FR-AC-011 | Currencies & Rates; Chart of Accounts | /currencies/revaluation-rates; /coa/accounts | Built |
| FRBS 2.4.0 | p.53 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.1 | p.53 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.2 | p.53 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.3 | p.54 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.4 | p.54 | FR-AC-021 | Report (column filters) | /reports/{code}/export?filter= | Built |
| FRBS 2.4.5 | p.54 | FR-AC-022 | Report Batch | /reports/batches | Built |
| FRBS 2.4.6 | p.54 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.7 | p.54 | FR-AC-022 | Report Batch | /reports/batches | Built |
| FRBS 2.4.8 | p.55 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 2.4.9 | p.55 | FR-AC-023 | Report (print options) | /reports/{code}/export | Built |
| FRBS 2.4.10 | p.55 | FR-AC-020 | Report Centre; Report Pack | /reports | Built |
| FRBS 3.2.0 | p.63 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | Report Pack; Account Schedules; BIR Forms & Books | /frbs/report-pack; /finreport/schedules; GL-SCHEDULE; FRBS-* reports; TAX-* reports | Built, gap |
| FRBS 2.5.0 | p.55 | FR-AC-030 | Journals; event log | Accounting engine | Built |
| FRBS 2.5.1 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | /journals/assign | Built |
| FRBS 2.5.2 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | /journals/assign | Built |
| FRBS 2.5.3 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | /journals/assign | Built |
| FRBS 2.5.4 | p.56 | FR-AC-034 | New Journal; Journal | /journals/{id}/submit, /warnings | Built, parked |
| FRBS 2.5.5 | p.57 | FR-AC-034 | New Journal; Journal | /journals/{id}/submit, /warnings | Built |
| FRBS 2.5.6 | p.57 | FR-AC-036 | Journals; My Approvals | /journals/bulk-approve; /{id}/reject | Built |
| FRBS 2.5.7 | p.57 | FR-AC-036 | Journals; My Approvals | /journals/bulk-approve; /{id}/reject | Built |
| FRBS 2.5.8 | p.57 | FR-AC-036 | Journals; My Approvals | /journals/bulk-approve; /{id}/reject | Built |
| FRBS 2.5.9 | p.57 | FR-AC-036 | Journals; My Approvals | /journals/bulk-approve; /{id}/reject | Built |
| FRBS 2.5.10 | p.58 | FR-AC-035 | Confirm posting dialog | - | Built |
| FRBS 2.8.0 | p.59 | FR-AC-032 | New Journal | /journals | Built |
| FRBS 2.8.1 | p.60 | FR-AC-032, FR-AC-033 | New Journal; Journals (REVERSAL) | /journals; Job JOURNAL_AUTO_REVERSAL | Built |
| FRBS 2.8.2 | p.60 | FR-AC-032 | New Journal | /journals | Built |
| FRBS 2.8.3 | p.60 | FR-AC-035 | Confirm posting dialog | - | Built |
| FRBS 2.8.4 | p.61 | FR-AC-034 | New Journal; Journal | /journals/{id}/submit, /warnings | Built, parked |
| FRBS 2.8.5 | p.61 | FR-AC-032 | New Journal | /journals | Built |
| FRBS 2.9.0 | p.61 | FR-AC-037 | Edit Journal | /journals/{id} | Built |
| FRBS 3.1.0 | p.62 | FR-AC-030 | Journals; event log | Accounting engine | Built |
| FRBS 3.6.0b | p.66 | FR-AC-034 | New Journal; Journal | /journals/{id}/submit, /warnings | Built, parked |
| FRBS 2.6.0 | p.58; Add.1 p.35 | FR-AC-040 | GL Close & Cut-Off | /closing/close-schedules | Built, parked |
| FRBS 2.6.1 | p.58 | FR-AC-040 | GL Close & Cut-Off | /closing/close-schedules | Built |
| FRBS 2.7.0 | p.59 | FR-AC-041 | Period-End & Year-End | /closing/year-end/* | Built |
| FRBS 2.7.1 | p.59 | FR-AC-041 | Period-End & Year-End | /closing/year-end/* | Built |
| FRBS 3.4.0 | p.64 | FR-AC-042 | GL Close & Cut-Off | /closing/broking-books | Built, parked |
| FRBS 3.4.1 | p.65 | FR-AC-042 | GL Close & Cut-Off | /closing/broking-books | Built, parked |
| FRBS 3.5.0 | p.65 | FR-AC-043 | FX Revaluation | /closing/fx-revaluations | Built |
| FRBS 3.3.0 | p.63 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | /receivables/bank-rec/* | Built |
| FRBS 3.3.1 | p.63 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | /receivables/bank-rec/* | Built, parked |
| FRBS 3.3.2 | p.64 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | /receivables/bank-rec/* | Built, parked |
| FRBS 3.3.3 | p.64 | FR-AC-051 | Report Centre | FIN-BRS-UNREC-BOOK / -BANK / FIN-BRS-STMT | Built |
| FRBS 2.10.0 | p.61 | FR-AC-052 | Service Fee Runs; Service Fee Rates | /frbs/service-fee/runs | Built, parked |
| FRBS 2.10.1 | p.62 | FR-AC-053 | Service Fee Run (lines) | /frbs/service-fee/runs/{id}/lines/* | Built, parked |
| FRBS 2.10.2 | p.62 | FR-AC-053 | Service Fee Run (lines) | /frbs/service-fee/runs/{id}/lines/* | Built, parked |
| FRBS 3.1.1 | Add.2 p.5-6 | FR-AC-054 | Cost-Centre Rules | /accounting/cost-center-rules | Built, parked |
| FRBS 3.1.2 | Add.2 p.6 | FR-AC-055 | Accounting rules | Event OPS_REMIT_INCENTIVE | Built, parked |

## Business and system administration (BASAU)

<!-- table: widths=2.2,2.2,2.6,4.6,4.8,1.9 caption="BASAU requirement IDs to FR, screen and build status" size=7.5 -->
| BRD ID | Page | FR | Screen | API | Build |
|---|---|---|---|---|---|
| BASAU 1.1.0 | p.127 | FR-AC-001 | Login; menu | auth/login | Built |
| BASAU 1.1.1 | p.127 | FR-AC-001 | Login; menu | auth/login | Built |
| BASAU 1.1.2 | p.128 | FR-AC-002 | Session dialog | Session parameters | Built |
| BASAU 1.1.3 | p.128 | FR-AC-002 | Session dialog | Session parameters | Built |
| BASAU 2.2.0 | p.128 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.2.1 | p.128 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.2.2 | p.128 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.2.3 | p.128 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.2.4 | p.128 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.2.5 | p.129 | FR-AC-070 | Lists of Values; My Approvals | /lov | Built |
| BASAU 2.3.0 | p.129 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.3.1 | p.129 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.3.2 | p.129 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.3.3 | p.129 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.4.0 | p.130 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.4.1 | p.130 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built, gap |
| BASAU 2.4.2 | p.130 | FR-AC-071 | Access Requests; Users; Roles | /nbadmin/access-requests | Built |
| BASAU 2.5.0 | p.130 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built |
| BASAU 2.5.1 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built |
| BASAU 2.5.2 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built |
| BASAU 2.5.3 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built, gap |
| BASAU 2.6.0 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built, gap |
| BASAU 2.6.1 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built, gap |
| BASAU 2.6.2 | p.131 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built |
| BASAU 2.6.3 | p.132 | FR-AC-072 | My Approvals; Access Requests | /nbadmin/access-requests/{id}/*; /approvals/bulk-approve | Built |

API paths start with `/api/v1`.

## Coverage summary

<!-- table: widths=6,2.2,2.2,2.2,2.2,2.2 caption="Coverage summary of Volume 1" size=8.5 -->
| Group | BRD IDs | Covered | Built | Built with parked seam | Built with gap |
|---|---|---|---|---|---|
| FRBS (Accounting) | 60 | 60 | 45 | 14 | 1 |
| Business and system administration (BASAU) | 25 | 25 | 21 | 0 | 4 |
| **Total** | **85** | **85** | **66** | **14** | **5** |


# Sign-off

By signing, BDOI confirms that this volume describes the Accounting (FRBS) and administration functions it expects in BIBS, accepts the recorded differences in section 1.7 and the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request. Volume 2 is signed separately by the Disbursement, Marketing and ACSL owners.

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Financial Reporting and Budget Section", organisation: BDOI}
  - {name: "", role: "Business Administrator", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
