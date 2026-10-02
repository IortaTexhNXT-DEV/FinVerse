---
# Source of the Functional Requirements Specification for BRD-5 Accounting, Disbursement and ACSL (release set v2.0).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-05_Accounting_Disbursement_ACSL/FRS_BRD05_ACCOUNTING.md
title: Accounting, Disbursement and ACSL
subtitle: BRD-5 Accounting (FRBS), Disbursement, Marketing Refund and Cash-Advance Requests, Accounting Controls and Subsidiary Ledger (ACSL), with Addendum 1 and the Workshop Addendum
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-05
name: Accounting Disbursement ACSL
doc_id: BIBS-FRS-BRD-05
version: "2.0"
date: 2 October 2026
status: Issued for BDOI business sign-off
header_title: FRS BRD-5 Accounting, Disbursement and ACSL
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-5 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue for BDOI review in two volumes with a cover note (Volume 1 FRBS and administration; Volume 2 Disbursement, Payment Requests and ACSL)
  - version: "2.0"
    date: 2 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Business sign-off pack: the two volumes and the cover note in one document, chapters 1-11 combined by topic with FR, BRD and test IDs kept; navigation by persona, 51 screen specifications with screenshots, five walkthroughs, messages, notifications, document outputs, upload templates, cross-BRD contract, sign-off and change control (chapters 12-20); proposed business rules and clarifications for confirmation of both volumes, extended with the screen presentation items (chapter 21); screen standards (appendix). Issued 2-Oct-2026"
distribution:
  - {name: "Ronald Allan E. De Leon, VP, Head - Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Iris S. Marquez, Product Owner - Comptrollership - FRBS and ACSL", role: Approver, organisation: BDOI, purpose: "Review and sign-off for FRBS and ACSL"}
  - {name: "Rodrigo R. Dela Cruz, Product Owner - Comptrollership - Disbursement", role: Approver, organisation: BDOI, purpose: Review and sign-off for Disbursement}
  - {name: "Perjelyn Joy R. Gutierrez, Product Owner - Comptrollership and Operations", role: Approver, organisation: BDOI, purpose: "Review and sign-off for the Operations hand-offs"}
  - {name: "Shellah Marie C. Miranda, AVP, Product Owner - Marketing, Processing and Collections", role: Approver, organisation: BDOI, purpose: "Review and sign-off for the Marketing refund and cash-advance requests"}
  - {name: "Roderick L. Lim, FVP, Head - Institutional Banking, SM and BDO Accounts", role: Approver, organisation: BDOI, purpose: Review and sign-off for Marketing}
  - {name: "Freddie A. Atanque and Jennifer T. Lugtu, Product Owners - Comptrollership - ACSL", role: Reviewer, organisation: BDOI, purpose: "Review of the ACSL, Disbursement and Accounting screens and rows"}
  - {name: "Ma. Fides Rivera, Marketing Business Services and System Support (MBS)", role: Input provider, organisation: BDOI, purpose: "Review of the non-functional requirements and the Marketing request screens"}
  - {name: "Information Technology Group", role: Reviewer, organisation: BDOI, purpose: "Review of interfaces, jobs, access and the Technical Specification"}
  - {name: "Dan Ace R. Cauton, Program Manager, Roda Lyn D. Gallardo and Zean C. Ibay, Business Analysts, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the requirements of BRD-5 for BDO Insurance and Reinsurance Brokers, Inc. (BDOI): Accounting (the Financial Reporting and Budget Section, FRBS, the GL team of Comptrollership), Disbursement, the Marketing refund and cash-advance requests, Accounting Controls and Subsidiary Ledger (ACSL), and the business and system administration of the BRD. It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page. The FRs describe the proposed behaviour of the screens. Where the proposed rule or screen differs from the BRD text, or needs a decision of BDOI, the FR says so in a note and chapter 21 lists each such point for confirmation. Chapters 12 to 19 give the business view of the system: the navigation, the specification of every screen with its screenshots, the walkthroughs, the messages, notifications, documents, uploads and the contract with the other BRDs; chapter 20 describes the sign-off.

## Scope

BRD-5 is one BRD with four source documents: Accounting (FRBS), Disbursement, Marketing refund and cash-advance requests (Payment Requests) and ACSL, plus business and system administration. The FRS is organised by unit.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access | Access from BDO devices, log-in with a profile, idle and log-out warnings for every unit of the BRD | FRBS 1.1.0-1.1.3; DIS, MKT, ACSL 1.1.0-1.1.3; BASAU 1.1.0-1.1.3 |
| Chart of accounts and rates | Chart set-up, upload, account numbering, search by short code, monthly revaluation rate | FRBS 2.2.0, 2.3.0-2.3.5, 3.6.0 |
| Reports on demand | Report selection, date range, column filters, batches of reports, print options | FRBS 2.4.0-2.4.10 |
| Posting and manual entries | Posting to GL and sub-ledger, assignment, validation, negative-balance control, bulk posting, return, accrual auto-reversal | FRBS 2.5.0-2.5.10, 2.8.0-2.8.5, 2.9.0, 3.1.0, 3.6.0b |
| Closing | Scheduled month-end GL close, broking-books cut-off, year-end close and verification, USD revaluation | FRBS 2.6.0-2.7.1, 3.4.0-3.5.0 |
| Bank reconciliation | Bank file upload, check-number matching, unmatched report | FRBS 3.3.0-3.3.3 |
| Service fee | Service-fee runs, payout, release and liquidation tags | FRBS 2.10.0-2.10.2 |
| Cost centres and incentive income | Cost-centre derivation rules; early incentives as Other Income | FRBS 3.1.1, 3.1.2 |
| Report pack | Appendix A report families - end of day, GARD, subsidiaries, schedules and aging, Mancom, service fee, government | FRBS 3.2.0; Appendix A |
| Business and system administration | Lists of values with approval, user and role management through requests, approvals and returns | BASAU 2.2.0-2.6.3 |
| Payees | Payee master, classes, modes of payment, payee requests, deletion rules, migration | DIS 2.2.0-2.2.8 |
| Request intake | System-triggered requests, classification, payee matching, e-mailed requests, uploads, the workbench | DIS 2.4.x-2.6.x, 3.25.x |
| Processing | Disbursement voucher, proforma entry, seven payment modes, ATD, MC / DD, CT / TT, expense allocation by cost centre, employee master | DIS 2.7.x, 3.30.x |
| Review, approval, cancellation | Review, return, approval and posting, rejection, cancellation before and after approval, regularisation | DIS 2.9.0, 2.13.0-2.21.0, 3.27.0 |
| Status and tagging | Instrument statuses, status edits, uploads of bank files, stale and negotiated checks, OR / AR and CWT tags, BIR 2307 | DIS 2.8.x, 2.10.x-2.12.0, 2.22.0, 3.26.x, 3.27.1 |
| End of day and funding | End of day, DCTF, checks, forms, confirmations, account funding, check series, bank accounts | DIS 2.7.12, 2.16.x, 2.17.x, 2.23.x, 2.24.x |
| Disbursement reports | On-demand, end-of-day and real-time reports, fall-out, CPC2 report | DIS 2.3.x, 3.28.x, 3.29.0 |
| Incentives and invoice family | CPC2 and the early-incentive service invoice; one invoice number for related transactions | DIS 3.27.2, 3.29.1, 3.29.2 |
| Payment Requests | Refund Request Form, cash-advance RFP, validation, review, approval, HR approval, check cancellation, CA / SA, status, liquidation | MKT 1.2.0-2.26.0; Appendix D |
| ACSL | Input files, insurer SOA upload and reconciliation, GL-SL reconciliation, reports, investigation, AR refund application, payment reversal, correction entries, remittance deduction, invoice family | ACSL 2.2.0-2.16.0, 2.9.1, 2.9.2 |

**Out of scope:** payroll outputs of Appendix A VII (HDMF, SSS, PhilHealth, 1601-C, 1604-C), because BIBS has no payroll (AQ06); the log-in to BDO Business Online Banking (DIS 2.17.1), which is outside BIBS and marked OUT in the BRD baseline. BDO Business Online Banking, eFPS / eBIRForms filing, the transmission of GARD reports to BDO Unibank and the bank channels (TPD / ACA, branch processing) are external channels; BIBS produces the files, forms and reports and records the results.

## Structure of this FRS

BRD-5 has 277 requirement IDs. Version 1.0 was issued in two volumes with a cover note; this version 2.0 combines them into one document so that the business sign-off set has one FRS with the screens of every unit. The FR numbers of both volumes are kept: FR-AC for Accounting and administration (FRBS 60, BASAU 25), FR-DS for Disbursement (DIS 111), FR-PQ for Payment Requests (MKT 36) and FR-AS for ACSL (ACSL 45). Chapters 1 to 11 keep the content of the volumes, combined by topic; chapter 4 has one section per area.

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Accounting, Disbursement and ACSL BRD (main BRD), pages 40-153 of the BRD-5 pack; signed scan pp.154-267 | v1.0, 23-Jul-2025; approved Jul to Oct 2025 |
| R2 | Addendum 1 "Accounting, Disbursement and ACSL - Addendum", pages 28-39 (signed copy pp.16-27) | v1.0, 18-Dec-2025; signed 13-Jan-2026 |
| R3 | Addendum 2 "Financial Reporting, Disbursement and ACSL - Addendum (Workshop)", pages 1-15 (scanned) | v1.0, 10-Apr-2026; signed 8 to 15-Apr-2026 |
| R4 | BDOI Accounting, Disbursement and ACSL (BRD-5) requirements baseline | current |
| R6 | Cross-BRD decisions and answered questions | current |
| R8 | BRD-2 Operations FRS (receipts, remittance, commission, remittance deductions) | v2.0 |
| R9 | BRD-4 Collections FRS (collector requests to Cashiering) | v2.0 |
| R10 | BRD-11 User Access Maintenance FRS (users, roles and access requests) | v2.0 |

Page references ("p.51") are pages of the BRD-5 PDF. "Add.1" is Addendum 1 (pp.28-39) and "Add.2" is the Workshop Addendum (pp.1-15). The BRD numbers the access rows of each unit "BRD 1.1.0-1.1.3"; this FRS writes them FRBS, DIS, MKT, ACSL and BASAU 1.1.0-1.1.3. The second row numbered FRBS 3.6.0 (item q, p.66) is written FRBS 3.6.0b. Duplicate numbers renamed by Addendum 1 are used in their new form (DIS 2.17.4, DIS 2.24.2, ACSL 2.5.5); the second row printed "DIS 3.30.1" (headcount report) is written DIS 3.30.2.

## Definitions and acronyms

```glossary
ACA: Automatic credit arrangement processed by BDO TPD from the DCTF
ACSL: Accounting Controls and Subsidiary Ledger unit of Comptrollership
AO: Account Officer (Marketing)
AQnn: Open question on BRD-5 raised by the project team (section 10.3)
AR: Acknowledgement Receipt
ATC: Alphanumeric tax code of the BIR
ATD: Authority to Debit the BDOIR main account
BASAU: Requirement ID prefix of the business and system administration rows of BRD-5
BIR: Bureau of Internal Revenue
BOB: BDO Business Online Banking
BOOK rate: Exchange rate at which USD Operations documents are booked
Broking books: The postings of the broking and Operations modules (booking, receipts, remittance, adjustment, commission, disbursement), closed by the system at month end
CA / SA: Current or savings account of a client at BDO, used for refunds
CLOSING rate: Month-end exchange rate used to revalue USD balances; the BRD's monthly revaluation rate
Cost centre: Responsibility centre of an expense or income line (dimension of the journal line)
CPC2: Incentive earned by BDOI on packaged Fire and Motor products, agreed with the insurers, on top of the regular commission
CT / TT: Credit Ticket / Telegraphic Transfer
CTA: Credit to account
CWT: Creditable withholding tax (BIR Form 2307)
DCTF: Direct Credit Transaction File sent to TPD for ACA processing
DTIP: Due to insurer (premium payable)
DV: Disbursement voucher (DV-yyyy-n)
EOD: End of day of Disbursement
EWT: Expanded withholding tax withheld at payment
FRBS: Financial Reporting and Budget Section (the GL team of Comptrollership); also the requirement ID prefix of the Accounting rows
GARD: BDO Unibank reporting format for subsidiaries (Appendix A II)
GL: General ledger
GL books: The general ledger closed by FRBS on a chosen date after the broking books
Instrument: The payment document of an approved DV (check, ATD, CTA extraction, MC / DD, CT / TT, online banking)
Mancom: Management committee reports (Appendix A V)
MC / DD: Manager's check / demand draft
OR: Official Receipt
Payee request: A request to create or change a payee (from a refund request, from Disbursement or from a payment request whose payee is not found)
Proforma entry: The accounting entry proposed by the rules before approval
RFP: Request for Payment (cash advance); RFP-yyyy-n
Root invoice: The original invoice number shared by an invoice's endorsements and cancellations
RRF: Refund Request Form; RRF-yyyy-n
SAWT: Summary Alphalist of Withholding Taxes
Schedule engine: Configurable account-schedule report of BIBS (report GL-SCHEDULE)
Service fee: Referrers' share of fully paid commission (Appendix A VI, p.144)
Short code: Unique short key of a GL account used for entry and search
SL: Sub-ledger (open items per party)
SOA: Statement of account sent by an insurer
TPD: BDO Transaction Processing Department
UPP: Unapplied payment
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority** and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the business or the System Administrator in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check; its message has no business code. Text in angle brackets (`<account>`) is replaced by the value.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**, the basis of the BRD-5 test cases.

Every BRD-5 row carries the priority **Must have** in the BRD. Where a function waits for BDOI data, its rules name the question it waits for (for example AQ24); the FR describes the proposed default until the answer.

> [!NOTE]
> The real chart of accounts, the accounting rules, the report layouts and several rates are BDOI data that have not been given (AQ01-AQ07, AQ20, AQ26). BIBS holds them as configuration with seed values, so a BDOI answer does not need a change to the system.

> [!NOTE]
> Bank channel layouts, form layouts, the payee migration file, the real accounting entries and several lists are BDOI data not given yet (AQ02, AQ09-AQ19, AQ21-AQ25). BIBS holds them as configuration or draft templates, so an answer does not need a change to the system.

# Business context and process overview

## Business context

### Accounting (FRBS)

BDOI is a broker. Its money is of two kinds: trust money (premium collected for insurers and remitted net of commission) and its own income (commission, service fee, profit share, incentives). BRD-1 and BRD-2 cover the front of the cycle - booking, receipts, application, remittance, adjustments and commission receivables. BRD-5 covers the back office of Comptrollership. In BIBS the general ledger is internal: every module posts through the accounting engine, and FRBS closes, reconciles and reports on the same ledger.

<!-- table: widths=1,8,8 caption="Current and envisioned Accounting process (BRD p.44-46)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Input reports and the closing of the broking books depend on IT support | FRBS runs every report on demand; the broking books close automatically at month end |
| 2 | Output reports are prepared by hand | The report pack (Appendix A) is generated from the ledger and exported to Excel and PDF |
| 3 | Reconciliation is manual | Bank files are uploaded and matched automatically on check number and amount |
| 4 | The system cannot prevent unbalanced transactions | Every journal is validated before submission and posting; unbalanced journals are refused |
| 5 | Some transactions need manual entries | Every financial event of BIBS posts through rules; manual entries are assigned, reviewed and posted in the system |

### Disbursement, Payment Requests and ACSL

Disbursement pays what the other units of BDOI request - remittances to insurers, client refunds, suppliers, government agencies, employees, service fees and incentive pass-ons. Marketing raises client refunds and employee cash advances. ACSL controls the sub-ledgers - it reconciles the insurer statements and the GL with the sub-ledgers, investigates accounts and corrects wrong postings. In BIBS all three work inside the application: a business module asks for a payment through the Operations disbursement gateway, the Disbursement module pays it and reports the status back, and every approval posts to the BIBS ledger.

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.45-48)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Some requests arrive on paper or by e-mail | Requests arrive from the BIBS modules, by upload or by encoding; each gets a request number |
| 2 | Not all payment methods are in the system | The seven modes of payment are in BIBS with their instrument statuses |
| 3 | Disbursement and CWT status tagging are manual | Statuses are set by print, extraction, uploads and jobs; users can still edit them with approval |
| 4 | Communication and approvals happen outside the system | Review and approval are workflows with four eyes; the source unit is notified |
| 5 | Payee maintenance covers only credit to account and check | The payee master covers every class and mode of payment |
| 6 | Most Disbursement reports are manual | End-of-day and real-time reports are generated |
| 7 | Marketing refund and cash-advance requests are e-mailed and signed on paper | Requests are raised, validated, reviewed and approved in BIBS and routed to Disbursement automatically |
| 8 | ACSL input reports need IT; corrections are posted by a separate unit; cancellations get a new invoice number | Reports are on demand; corrections post on approval; related transactions are linked by the root invoice number |

## Process overview

### Accounting (FRBS)

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

### Disbursement, Payment Requests and ACSL

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Request | Source module; Marketing; Disbursement | A payment request arrives from a module or Payment Requests, by upload or by encoding; it is classified and its payee matched | DIS 2.4-2.6, 3.25; MKT 2.24.0 |
| 2 | Disbursement voucher | Processor | The DV is created with its proforma entry, mode, paying account and allocation | DIS 2.7.x |
| 3 | Review | Team Leader | The TL reviews and submits for approval, or returns | DIS 2.13.0-2.15.0 |
| 4 | Approval and posting | Approver | The approver approves (single or bulk); the entry posts and the instrument is created | DIS 2.19.0 |
| 5 | End of day | TL / Approver | Checks, DCTF, forms and reports of the day; confirmations to payees | DIS 2.16.x, 2.7.12 |
| 6 | Instrument follow-up | Processor; System | Released, e-mailed, debited, credited, negotiated, stale | DIS 2.8.x, 3.26.x |
| 7 | Tagging | Processor | OR / AR and CWT tags; BIR 2307 | DIS 2.10.0-2.12.0 |
| 8 | Status to the source | System | The source module is told of every DV stage and payment | DIS 3.25.0; MKT 1.20.0 |
| 9 | ACSL control | ACSL | SOA and GL-SL reconciliation; cases; correction entries | ACSL 2.x |

![Payment request to payment, with the Payment Request and ACSL flows](figures/brd05_v2_process_flow.dot)

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
| Disbursement Processor | DISBURSEMENT | Encodes and uploads requests, processes DVs, tags instruments, OR / AR and CWT | DIS 2.4-2.12 |
| Disbursement Team Leader | DISB_TL | As the processor, plus review, payee maintenance, funding request and verification, end of day, status-edit approval | DIS 2.13-2.17 |
| Disbursement Approver | DISB_APPROVER | Approves, rejects and cancels approved DVs; authorises payees; approves funding; maintains bank accounts and check series | DIS 2.17-2.24 |
| Marketing Processor (AO) | PRQ_PROCESSOR, MKT_AO | Raises refund, cash-advance and check-cancellation requests | MKT 1.2-1.14 |
| Marketing Reviewer | PRQ_REVIEWER, MKT_TL | Assigns, reviews and endorses requests | MKT 1.9, 1.15 |
| Marketing Approver | PRQ_APPROVER | Approves or returns requests | MKT 1.16 |
| Human Resources | HR_APPROVER | Approves cash advances after Marketing | MKT 1.16.2 |
| ACSL Processor | ACSL_PROCESSOR | Uploads SOAs, investigates, applies refunds, requests reversals, prepares corrections | ACSL 2.2-2.9 |
| ACSL Team Leader | ACSL_TL | Assigns cases and corrections, reviews corrections, confirms remittance deductions | ACSL 2.7-2.10, 2.9.2 |
| ACSL Head (Approver) | ACSL_HEAD | Approves corrections | ACSL 2.11-2.12 |
| Cashier | CASHIER (Operations) | Validates refunds of cancelled policies; approves payment reversals | MKT 1.11.0; ACSL 2.6.1 |
| Comptrollership administrator | FIN_ADMIN, BUSINESS_ADMIN | Employee and cost-centre master; liquidation accounts | DIS 3.30.1 |

The role grants are the project's reading of the scanned role matrix (p.181): only the Section Head inputs the revaluation rate; only the Team Lead closes the month and the year; Processors make manual entries and monitor the service fee. BDOI confirms the matrix through AQ28. Several roles per user are allowed (decision D5 of R6). The Disbursement, Payment Request and ACSL grants follow the matrices of pp.216-217, 227 and 240.

## Permissions

<!-- table: widths=4.6,2.6,9.6 caption="Permissions" -->
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
| EMPLOYEE_MAINTAIN | AMEND | Employee and cost-centre master (DIS 3.30.1) |
| LOV_MANAGE | AMEND | Lists of values (BASAU 2.2.x) |
| ACCESS_REQUEST, ACCESS_APPROVE | CREATE / APPROVE | User and role requests and their decisions (BASAU 2.3.x-2.6.x) |
| DISB_VIEW | VIEW | Disbursement screens (read) |
| DISB_PROCESS | CREATE / AMEND | Encode requests, process DVs, instrument actions, cancel In Process |
| DISB_UPLOAD | CREATE | Request, status and payee-migration uploads |
| DISB_TAG | AMEND | OR / AR and CWT tags; record certificates received |
| DISB_REVIEW | APPROVE | Review, return, submit for approval, cancel For review |
| DISB_APPROVE | APPROVE | Approve, reject, return; cancel approved DVs |
| DISB_STATUS_APPROVE | APPROVE | Approve instrument status edits |
| DISB_PAYEE_MAINTAIN / DISB_PAYEE_AUTHORIZE / DISB_PAYEE_VIEW_FULL | CREATE, AMEND / APPROVE / VIEW | Payee master; full account numbers |
| DISB_EOD | AMEND | End of day and confirmations |
| DISB_FUNDING_REQUEST / VERIFY / APPROVE | CREATE / APPROVE / APPROVE | Account funding |
| DISB_REPORT_VIEW / DISB_REPORT_EXPORT | VIEW | Disbursement reports |
| MASTER_MAINTAIN | AMEND | Bank accounts and check series |
| EMPLOYEE_MAINTAIN | AMEND | Employee and cost-centre master |
| PRQ_VIEW / PRQ_CREATE / PRQ_ASSIGN / PRQ_REVIEW / PRQ_APPROVE / PRQ_HR_APPROVE | VIEW / CREATE / AMEND / APPROVE | Payment Requests |
| ACSL_VIEW / ACSL_UPLOAD / ACSL_PROCESS / ACSL_APPLY / ACSL_ASSIGN / ACSL_REVIEW / ACSL_APPROVE | VIEW / CREATE / AMEND / APPROVE | ACSL |
| ACSL_REPORT_VIEW / ACSL_REPORT_EXPORT | VIEW | ACSL reports |
| REMIT_DEDUCTION_CONFIRM | APPROVE | Confirm a remittance deduction |

## Permissions matrix

### Accounting and administration

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

ACCESS_APPROVE is held by the access approvers named by BDOI; ACCOUNTING_RULE_MANAGE and EMPLOYEE_MAINTAIN by the Comptrollership administrators. The table shows the proposed grants.

### Disbursement, Payment Requests and ACSL

<!-- table: widths=5.2,1.14,1.14,1.14,1.14,1.14,1.14,1.14,1.14,1.14,1.14 caption="Role-to-permission matrix (proposal until AQ28)" size=7.5 -->
| Permission | Disb. Proc. | Disb. TL | Disb. Appr. | MKT Proc. | MKT Rev. | MKT Appr. | HR | ACSL Proc. | ACSL TL | ACSL Head |
|---|---|---|---|---|---|---|---|---|---|---|
| DISB_VIEW | Y | Y | Y | | | | | | | |
| DISB_PROCESS | Y | Y | | | | | | | | |
| DISB_UPLOAD, DISB_TAG | Y | Y | | | | | | | | |
| DISB_REVIEW | | Y | | | | | | | | |
| DISB_APPROVE | | | Y | | | | | | | |
| DISB_STATUS_APPROVE | | Y | | | | | | | | |
| DISB_PAYEE_MAINTAIN | | Y | | | | | | | | |
| DISB_PAYEE_AUTHORIZE | | | Y | | | | | | | |
| DISB_PAYEE_VIEW_FULL | | Y | Y | | | | | | | |
| DISB_EOD | | Y | Y | | | | | | | |
| DISB_FUNDING_REQUEST, _VERIFY | | Y | | | | | | | | |
| DISB_FUNDING_APPROVE | | | Y | | | | | | | |
| MASTER_MAINTAIN (banks, checks) | | | Y | | | | | | | |
| DISB_REPORT_VIEW, _EXPORT | Y | Y | Y | | | | | | | |
| PRQ_VIEW | | | | Y | Y | Y | Y | | | |
| PRQ_CREATE | | | | Y | Y | | | | | |
| PRQ_ASSIGN, PRQ_REVIEW | | | | | Y | | | | | |
| PRQ_APPROVE | | | | | | Y | | | | |
| PRQ_HR_APPROVE | | | | | | | Y | | | |
| ACSL_VIEW, ACSL_REPORT_VIEW | | | | | | | | Y | Y | Y |
| ACSL_UPLOAD, _PROCESS, _APPLY, _REPORT_EXPORT | | | | | | | | Y | Y | |
| ACSL_ASSIGN, ACSL_REVIEW, REMIT_DEDUCTION_CONFIRM | | | | | | | | | Y | |
| ACSL_APPROVE | | | | | | | | | | Y |

MKT_AO holds PRQ_CREATE and PRQ_VIEW; MKT_TL holds PRQ_VIEW, PRQ_ASSIGN and PRQ_REVIEW. COMPTROLLERSHIP, SYSADMIN and AUDITOR have read access (DISB_VIEW, ACSL_VIEW and the report views; SYSADMIN and AUDITOR also PRQ_VIEW).

Segregation of duties enforced by the system: a journal is never posted by the user who submitted it; a submitted journal is never assigned to its submitter; a service-fee run is never approved by its preparer; an access request is never decided by its submitter; a record is never authorised by its maker; a DV is never checked by its processor or approved by its processor or checker; a funding request is verified by a user other than the maker and approved by two further users; a payee is never authorised by its maker; a status edit is never approved by its requester; a Payment Request is never endorsed or approved by the user who raised or last moved it; a liquidation is checked by someone other than the employee; an ACSL correction is reviewed by someone other than its preparer and approved by someone other than its maker.

# Functional requirements

## Access and session

```fr
id: FR-AC-001
title: Access Accounting and administration with a user profile
brd: [FRBS 1.1.0 (p.50), FRBS 1.1.1 (p.50), BASAU 1.1.0 (p.127), BASAU 1.1.1 (p.127)]
actor: FRBS users; Business and System Administrators
priority: Must have
screens: Login; Home; menu groups Finance, Planning and Closing, Setup and Administration
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
  - [User ID or password wrong, Invalid user name or password, AUTHENTICATION_FAILED]
  - [Account locked (the same message as a wrong password; the lock shows to the System Administrator on Users), Invalid user name or password, AUTHENTICATION_FAILED]
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
screens: All screens (session dialog)
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
screens: Setup > Currencies & Rates (card Monthly revaluation rates)
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
screens: General Ledger > Chart of Accounts
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
  - [R3, "BDOI's own chart is loaded at go-live by upload (FR-AC-012); the seed chart is a placeholder (AQ01).", Configurable, Chart of accounts]
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
screens: General Ledger > Chart Upload (Upload Chart, Upload History)
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
screens: General Ledger > Chart of Accounts (Numbering Schemes)
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
screens: Chart of Accounts (search); journal lines (account entry)
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
screens: Report Centre; Report; Accounting Reports > Report Pack
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
screens: Report (column filter row)
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
screens: Report Centre > Report Batch
description: A report batch runs several reports with shared parameters and returns one ZIP of files in a chosen format, or one merged PDF for printing. Each report checks its own permission; a report that fails is listed and the others are produced (status COMPLETED, PARTIAL or FAILED). Only the creator sees a batch.
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
screens: Report (Export dialog); Report Batch
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
screens: Journals; Account Inquiry; Party Statement; Accounting Engine (event log)
description: Every BIBS module that has a financial effect publishes a business event (booking, receipt, remittance, adjustment, commission, disbursement, service fee, correction). The accounting engine creates the journal lines from the rules maintained by Comptrollership, posts them together with the event, and records or matches the open items of the parties in the sub-ledger. GL accounts are never chosen in code.
preconditions:
  - "An active accounting rule exists for the event type."
main_flow:
  - A module publishes an event with its amounts and account roles.
  - The engine resolves the rule, fills the cost centres (FR-AC-054) and creates balanced lines.
  - The engine posts the journal and the sub-ledger items.
alternate_flows:
  - No rule or missing account role. The event is recorded FAILED in the event log for re-processing; the alert of the module is raised.
  - Broking books closed. Events of the broking modules for a closed month are refused (FR-AC-042).
rules:
  - [R1, "Posting rules are configuration maintained by Comptrollership; the real rules replace the seed rules (AQ02, OQ07).", Configurable, Accounting rules]
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
screens: General Ledger > Journals (Assign, Assigned to me); Journal
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
screens: General Ledger > New Journal; Edit Journal
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
screens: Journals (REVERSAL journals); Scheduled Jobs
description: Every day at 00:05 Manila the job JOURNAL_AUTO_REVERSAL posts, for each posted manual journal whose "reverse on" date has come, a REVERSAL system journal that mirrors it, linked to the original. Each reversal posts in its own transaction.
preconditions:
  - "A posted manual journal has a reverse-on date of today or earlier and is not reversed."
main_flow:
  - The job selects the journals due for reversal.
  - BIBS posts the reversal journal with the key JV:<batch>:AUTOREV.
  - The original shows its reversal.
rules:
  - [R1, "One reversal per journal; a repeat does not create a second one.", Fixed, "-"]
  - [R2, "Schedule daily 00:05 Manila.", Configurable, Job schedule (System Administrator)]
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
screens: New Journal; Journal; confirmation dialog
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
screens: Confirm posting dialog (Journals)
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
screens: Journals (bulk posting); Journal (Post, Return to Maker); My Approvals
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
screens: Edit Journal; Journal (Reverse)
description: Draft and returned journals are edited by their maker. A posted journal is never edited; it is corrected by reversal and a new entry, or through an ACSL correction entry (FR-AS-021).
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
screens: Planning & Closing > GL Close & Cut-Off (Month-End Close)
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
screens: Planning & Closing > Period-End & Year-End (Year-End panel)
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
screens: Planning & Closing > GL Close & Cut-Off (Broking Books Cut-Off)
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
  - [R1, "Close time BROKING_CLOSE_TIME = 23:00 Manila on the last day of the month.", Configurable, Parameter BROKING_CLOSE_TIME]
  - [R2, "The broking source modules are listed in BROKING_SOURCE_MODULES.", Configurable, Parameter BROKING_SOURCE_MODULES]
  - [R3, "The pending list shows the items that the broking modules report as pending for the month.", Fixed, "-"]
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
> The agreed close time and whether a closed month may be reopened are open (AQ04); 23:00 and reopen-with-reason are the proposed defaults (chapter 12, CLR-AC-07).

```fr
id: FR-AC-043
title: Revalue USD balances at the revaluation rate
brd: [FRBS 3.5.0 (p.65)]
actor: GL Team Lead (PERIOD_END_RUN)
priority: Must have
screens: Planning & Closing > FX Revaluation
description: At month end the TL previews and runs the FX revaluation. BIBS revalues the balances of the accounts flagged "revaluation required" (USD receivables, payables, cash and bank) per account, branch and currency at the CLOSING rate of FR-AC-010, posts the difference to the unrealised FX gain or loss, and reverses it automatically on the first day of the next period. Open items are revalued for information. The register report GL-FXREV lists the revaluation.
preconditions:
  - "The CLOSING rate of the month end exists."
main_flow:
  - The TL opens FX Revaluation, chooses the period and previews.
  - The TL runs the revaluation; BIBS posts the entries and their reversal.
rules:
  - [R1, "Revaluation is once per period, with automatic reversal.", Fixed, "-"]
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
screens: Receivables > Bank Statements; Bank Reconciliation; Setup > Bank Statement Layouts
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
screens: Report Centre; Bank Reconciliation
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
screens: Accounting Reports > Service Fee Runs; Service Fee Run; Service Fee Rates
description:
  - The service fee is the referrers' share of the commission that BDOI has fully collected - 2.5% or 1% of the commission net of the insurer's withholding tax, per market segment (p.144). FRBS computes a run for a period - BIBS takes the invoices whose payment status became paid in the period, are not cancelled, carry commission and are in no live run, and applies the rule of their segment in force on the day paid. It groups them into one line per service-fee segment, sales unit and currency. The payee of a line is the unit's recipient, else the unit; the cost centre is the recipient's, else the unit's, else the invoice's, else the cost-centre rules.
  - The run is submitted and approved by another user. Approval posts the accrual (FRBS_SERVICE_FEE_ACCRUE, expense against service fee payable) per line and sends each payout to Disbursement as a request of type SERVICE_FEE (FR-DS-020).
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
  - [R1, "Proposed rules - CBG 2.5% (segments CBG, RETAIL); IBG 1% (COMBANK, CORBANK, INSTITUTIONAL); base = commission less the insurer's withholding tax. To be confirmed (AQ20).", Configurable, Service Fee Rates]
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
screens: Service Fee Run (lines - Release, Liquidate)
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
screens: Setup > Cost-Centre Rules; Accounting Engine (event log); Alerts
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
screens: Accounting rules; Chart of Accounts
description: The early-remittance incentive posts through its own event component, separate from commission and premium, to the incentive income account (seed 4130), so the commission accounts are not touched. Comptrollership maps the component to an Other Income account of the BDOI chart. The source remittance batch is kept on the entry. CPC2 incentives use their own account (seed 4131, FR-DS-090).
preconditions:
  - "The Other Income account exists in the chart."
main_flow:
  - A remittance batch with an early incentive is approved (BRD-2).
  - The engine posts the incentive component to the incentive income account.
rules:
  - [R1, "The incentive account is chosen by the rule, not by code.", Configurable, Accounting rules]
  - [R2, "Moving the seed account 4130 under Other Income (4700) is a chart change for FRBS, not done in the seed data (AQ01).", Configurable, Chart of accounts]
validations: []
notifications:
  - "None."
audit:
  - "The journal carries the source batch reference."
acceptance:
  - The early incentive of a remittance batch posts to the incentive income account and not to commission income.
```

> [!NOTE] Related functions
> The automatic service invoice with 2% withholding for early incentives (DIS 3.29.1) and the CPC2 incentive (DIS 3.29.2) are part of Remittance and described in section 4.11 (FR-DS-090, FR-DS-091).

## Report pack

```fr
id: FR-AC-060
title: Generate the FRBS report pack
brd: [FRBS 3.2.0 (p.63); Appendix A (p.142-144)]
actor: FRBS users (FRBS_REPORT_VIEW / EXPORT)
priority: Must have
screens: Accounting Reports > Report Pack
description:
  - The Report Pack screen lists the reports of Appendix A by group - I End of Day, II GARD (bank format), III Subsidiaries Accounting, IV Schedules and Aging, V Performance / Mancom, VI Service Fee, VII Government - with 58 entries (section 6.2). Each entry opens its report runner and exports at once to Excel or PDF; the reports also run in report batches (FR-AC-022).
  - End-of-day reports are the statement of condition, income statement, trial balance, journal entries (day book) and subsidiary ledger; the cash receipts and cash disbursements books are day-book variants and BIR books (FR-AC-063).
preconditions:
  - "The user has FRBS_REPORT_VIEW."
main_flow:
  - The user opens Report Pack and chooses a group and a report.
  - The user enters the parameters and runs or exports it.
rules:
  - [R1, "Layouts of the GARD, subsidiaries and Mancom reports are drafts until BDOI provides them (AQ05).", Configurable, Schedule definitions; report pack]
  - [R2, "Reports flagged Word requested are issued in PDF and Excel (chapter 12, CLR-AC-10).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Every run and export is archived."
acceptance:
  - The Report Pack lists the seven Appendix A groups with their reports.
  - The GARD Schedule of Operating Expenses exports to Excel and PDF.
```

```fr
id: FR-AC-061
title: Run and maintain configurable account schedules
brd: [FRBS 3.2.0 (p.63); Appendix A II-IV (p.142-143)]
actor: GL team (run); GL Team Lead (MASTER_MAINTAIN, definitions)
priority: Must have
screens: Accounting Reports > Account Schedules (run, commentary, definitions editor)
description:
  - Most Appendix A schedules are definitions of one engine, the report GL-SCHEDULE. A definition selects the postable accounts by code prefix or report group, groups the posted ledger by account, party, party and document, cost centre, branch or business line, in base currency or in one currency, and shows the chosen figures - opening, debits, credits, movement, closing, a comparative (previous month or year), the variance and variance %. Optional ageing spreads the balance over up to eight buckets, first in first out. An optional commentary column prints the month's comment per row.
  - 28 definitions are provided (GARD-*, SUBS-*, SCH-*), all marked TO_CONFIRM. FRBS adds or changes definitions without a release and writes the monthly commentary.
preconditions:
  - "The user has FRBS_REPORT_VIEW (run), FRBS_REPORT_EXPORT (commentary) or MASTER_MAINTAIN (definitions)."
main_flow:
  - The user opens Account Schedules and chooses a schedule and the as-of date.
  - BIBS runs the definition and shows the rows.
  - The user writes the commentary of the month and exports.
alternate_flows:
  - New schedule. The TL creates a definition with its selector, grouping, figures and ageing.
rules:
  - [R1, "Account selectors point at the seed chart until BDOI's chart is uploaded (AQ01).", Configurable, Schedule definitions]
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
  - Every provided definition runs and exports to Excel and PDF.
  - The aged schedule of premium receivable splits each balance into its buckets and the buckets add up to the balance.
```

```fr
id: FR-AC-062
title: Produce the Mancom, production, GAP, cash-flow and expense reports
brd: [FRBS 3.2.0 (p.63); Appendix A IV-V (p.143-144)]
actor: FRBS users
priority: Must have
screens: Accounting Reports > Report Pack (groups IV, V, VI)
description:
  - The reports that need broking data are part of the FRBS module - the market performance summary (premium and commission per segment and location, month, year to date, previous year to date, growth), branch production (detailed and summary), expenses per cost centre and account, the GAP report (open sub-ledger receivables and payables by time to maturity, gap and cumulative gap), the cash-flow report (opening cash and bank, receipts and payments per journal type, closing) and the service-fee summary and detail.
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
screens: Tax & Statutory > BIR Forms & Books; Tax Returns; Certificates Received
description:
  - FRBS chooses a period and opens or exports each output - the VAT return 2550-Q with the relief lists, 1601-EQ and the quarterly, monthly and annual alphalists (QAP, MAP, 1604-E) from the withholding-tax worksheet, the SAWT from the register of certificates received (FR-DS-057), the 0619-F, 1603, 1702-Q (year to date) and 1702 worksheets from ledger movements of account prefixes and rates from parameters, the BIR books of accounts (general, purchase and sales journals, cash receipts and disbursements books, general ledger per account class with the balance forward) and the Insurance Commission Broker's Annual Statement of Business Operations.
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
screens: Administration > Lists of Values; My Approvals
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
screens: Administration > Access Requests; Users; Roles & Permissions; User Access Matrix
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
screens: My Approvals; Access Requests
description: The approver sees the pending LOV and user-management requests in My Approvals with a count, and is notified of new ones. The approver approves or declines a request with remarks, or returns it to the requester with remarks; the requester corrects and resubmits it. Several requests can be approved at once through bulk approval - each is decided on its own; a new-user request is approved on its own because its temporary password is shown once.
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

Section 4.1 covers Disbursement (FR-DS), section 4.2 Payment Requests of Marketing (FR-PQ) and section 4.3 ACSL (FR-AS).

## Disbursement

### Access

```fr
id: FR-DS-001
title: Access Disbursement, Payment Requests and ACSL with a user profile
brd: [DIS 1.1.0 (p.68), DIS 1.1.1 (p.69), DIS 1.1.2 (p.69), DIS 1.1.3 (p.69), MKT 1.1.0 (p.105), MKT 1.1.1 (p.105), MKT 1.1.2 (p.105), MKT 1.1.3 (p.105), ACSL 1.1.0 (p.114), ACSL 1.1.1 (p.115), ACSL 1.1.2 (p.115), ACSL 1.1.3 (p.115)]
actor: Disbursement, Marketing and ACSL users
priority: Must have
screens: Login; menu group Finance (Disbursement, Refund & Cash Advance Requests, ACSL)
description: Users reach BIBS from any BDO-issued device and log in with their own profile; the menu shows only the screens their roles allow. BIBS warns after 15 minutes of inactivity (SESSION_IDLE_WARNING_MINUTES) and 30 minutes before the forced log-out (SESSION_EXPIRY_WARNING_MINUTES). The rules are those of, FR-AC-001 and FR-AC-002.
preconditions:
  - The user has an active account with a Disbursement, Payment Request or ACSL role.
main_flow:
  - The user logs in with the user ID and password.
  - BIBS opens the home page with the Finance menu of the user's roles.
  - When inactive, the user receives the warnings before the time-outs.
rules:
  - [R1, "Log-in, lock-out (3 attempts) and session rules of BRD-1.", Configurable, Session parameters]
validations:
  - [User ID or password wrong, Invalid user name or password, AUTHENTICATION_FAILED]
notifications:
  - "On-screen warnings only."
audit:
  - Every log-in attempt is recorded with user, time and source address.
acceptance:
  - A Disbursement Processor sees the Disbursement Workbench and not the ACSL screens.
  - A Marketing AO sees Requests Home and New Refund Request only.
  - The inactivity warning appears after 15 minutes.
```

### Payee maintenance



```fr
id: FR-DS-010
title: Maintain payees with authorisation
brd: [DIS 2.2.0 (p.69), DIS 2.2.3 (p.70), DIS 2.2.6 (p.71), DIS 2.2.7 (p.71)]
actor: Disbursement Team Leader (maintain); Disbursement Approver (authorise)
priority: Must have
screens: Payees (tabs Active, For Authorisation, Drafts, Inactive, Payee Requests); Payee
description:
  - Disbursement keeps a payee master - payee code, class, name, address, e-mail, TIN, default and allowed modes of payment, currency, default cost centre and remarks, with one or more bank accounts (bank, branch, account number, account name, currency, mode, primary, active). Taxes (TIN, ATC, VAT) come from the party tax profile.
  - A new payee is saved as a draft and submitted; the Approver authorises it and it becomes ACTIVE. A change to an active payee, a deactivation and a reactivation are authorised the same way. Account numbers are masked in lists unless the user holds DISB_PAYEE_VIEW_FULL.
preconditions:
  - "The maker has DISB_PAYEE_MAINTAIN; the authoriser DISB_PAYEE_AUTHORIZE."
main_flow:
  - The TL clicks **New Payee**, enters the details and the accounts, and saves the draft.
  - The TL submits the payee for authorisation.
  - The Approver authorises it; the payee is ACTIVE and usable on DVs.
alternate_flows:
  - Return. The Approver returns the payee to the maker with a reason.
  - Change. The TL amends an active payee; the change waits for authorisation.
rules:
  - [R1, "A payee is never authorised by its maker.", Fixed, "-"]
  - [R2, "At least one mode of payment; the default mode is one of the allowed modes.", Fixed, "-"]
  - [R3, "Only ACTIVE payees are used on DVs.", Fixed, "-"]
validations:
  - [Code or company missing, Company and payee code are required, PAYEE_CODE]
  - [No mode, Select at least one mode of payment, PAYEE_MODE]
  - [Default mode not allowed, The default mode of payment must be one of the allowed modes, PAYEE_MODE]
  - [Payee not editable, "Payee <code> is <stage>", PAYEE_NOT_EDITABLE]
  - [Authoriser is the maker, A payee cannot be authorised by the user who maintained it, MAKER_CHECKER_VIOLATION]
fields_screen: New Payee
fields:
  - [Payee Code, Text, "Yes", "-", Unique; up to 30 characters]
  - [Payee Class, List, "Yes", LOV PAYEE_CLASS, "-"]
  - [Name, Text, "Yes", "-", Up to 250 characters]
  - [Address / E-mail / TIN, Text, "No", "-", Valid e-mail]
  - [Default Mode, List, "Yes", Modes of payment, One of the allowed modes]
  - [Allowed Modes, Multi-select, "Yes", "CTA, ATD, MC_DD, CREDIT_TICKET, TT, ONLINE_BANKING, CHECK", At least one]
  - [Currency, List, "Yes", Currencies, 3-letter code]
  - [Cost Centre, List, "No", Cost centres, Default for expense lines]
  - ["Account - bank, branch, number, name, currency, mode, primary", Table, Conditional, "-", Required for CTA / TT / online banking]
notifications:
  - "Payees for authorisation appear in the Approver's For Authorisation tab."
audit:
  - "Every change, submission, authorisation and return is recorded with user and time."
acceptance:
  - A payee created by disbtl is usable only after disbappr authorises it.
  - disbtl cannot authorise the payee he created.
  - A Processor sees the account number masked.
```

```fr
id: FR-DS-011
title: Classify payees and choose the modes of payment
brd: [DIS 2.2.2 (p.70), DIS 2.2.5 (p.70)]
actor: Disbursement Team Leader
priority: Must have
screens: Payee
description: Each payee has a class - Supplier, Insurer, Employee, Client, Government agency or Others - and the modes of payment it may be paid by - Credit to Account, Debit BDOIR Main Account (ATD), Manager's Check or Demand Draft, Credit Ticket, Telegraphic Transfer, Online Banking and Check. The class drives the automatic classification of requests (FR-DS-021); the modes limit the DV (FR-DS-033).
preconditions:
  - "The user maintains the payee (FR-DS-010)."
main_flow:
  - The TL chooses the class and the allowed and default modes.
rules:
  - [R1, "Payee classes Supplier, Insurer, Employee, Client, Government agency, Others.", Configurable, LOV PAYEE_CLASS]
  - [R2, "Party types EMPLOYEE, GOVERNMENT and OTHER_PAYEE exist for payees that are not clients or insurers.", Fixed, "-"]
validations:
  - [Mode not allowed on a DV, "Mode <mode> is not allowed for payee <code>", DV_MODE]
notifications:
  - "None."
audit:
  - "As FR-DS-010."
acceptance:
  - A supplier payee with modes Check and CTA cannot be paid by ATD.
```

```fr
id: FR-DS-012
title: Receive payee maintenance requests
brd: [DIS 2.2.1 (p.69)]
actor: Disbursement Team Leader
priority: Must have
screens: Payees (Payee Requests)
description: Payee requests come from refund requests (payee data of the RRF), from Disbursement itself, and from system requests whose payee was not found (NO_MATCH, FR-DS-022). Each shows its source, the payee data given and its status. The TL creates or completes the payee from the request; when the payee is authorised the request is DONE and a request waiting for that payee resumes.
preconditions:
  - "The user has DISB_PAYEE_MAINTAIN."
main_flow:
  - The TL opens Payee Requests and a request.
  - The TL creates the payee from it and submits it.
  - On authorisation the request is DONE.
alternate_flows:
  - Close. A request not needed is closed.
rules:
  - [R1, "Sources RRF, DISBURSEMENT, NO_MATCH.", Fixed, "-"]
validations:
  - [Request closed, "The payee request is <status>", PAYEE_REQUEST_CLOSED]
notifications:
  - "Alert DISB_PAYEE_NO_MATCH for NO_MATCH requests."
audit:
  - "The request keeps its source, its content and the payee created."
acceptance:
  - A remittance request for an insurer without payee creates a NO_MATCH payee request; authorising the payee resumes the request.
```

```fr
id: FR-DS-013
title: Delete or deactivate payees
brd: [DIS 2.2.4 (p.70)]
actor: Disbursement Team Leader; Approver
priority: Must have
screens: Payee (Delete, Request Deactivation)
description: A draft payee that was never used can be deleted. A payee that is active or has been used is deactivated instead; the deactivation is authorised by the Approver and the payee can be reactivated the same way. Inactive payees are not offered on new DVs.
preconditions:
  - "The user has DISB_PAYEE_MAINTAIN."
main_flow:
  - The TL deletes a draft payee, or requests the deactivation of an active one.
  - The Approver authorises the deactivation.
rules:
  - [R1, "Only a draft payee never used is deleted.", Fixed, "-"]
validations:
  - [Delete a used or active payee, Only a draft payee never used can be deleted; deactivate it instead, PAYEE_IN_USE]
notifications:
  - "None."
audit:
  - "Deletion and deactivation are recorded."
acceptance:
  - Deleting a payee paid last month is refused with PAYEE_IN_USE.
```

> [!NOTE] Difference from the BRD
> DIS 2.2.4 asks to delete payee details. BIBS keeps every payee that was used, for audit, and deactivates it (AQ11).

```fr
id: FR-DS-014
title: View all payees and migrate the existing payees
brd: [DIS 2.2.8 (p.71; Add.1 p.31-32; Add.2 p.11-12)]
actor: Disbursement users; Disbursement Team Leader (migration)
priority: Must have
screens: Payees; Disbursement Uploads (Payee Migration); Disbursement Reports (Payee report)
description: The Payees screen is the consolidated list of maintained payees - name, address, account number (masked unless permitted), mode of payment, disbursement type - active and inactive by tab. The payees of the current system are loaded once by the migration upload (payee code, name, class, address, e-mail, TIN, currency, allowed and default modes, bank, account number); each row is validated and the reconciliation lists loaded and refused rows. Payee data flows to the DV at processing, and every maintenance is logged.
preconditions:
  - "The user has DISB_VIEW (list); DISB_UPLOAD (migration)."
main_flow:
  - The TL downloads the migration template and fills it from the current system.
  - The TL uploads it; BIBS validates the rows and creates the payees.
  - Users view the payees by tab and export the payee report.
rules:
  - [R1, "Migrated payees carry the source MIGRATION.", Fixed, "-"]
  - [R2, "The migration file of the current system is to be provided (AQ11).", Configurable, Bulk handler DISB_PAYEE_MIGRATION]
validations:
  - [Row error, "<reason per row>", "-"]
notifications:
  - "None."
audit:
  - "The upload keeps its rows and outcomes; payee changes are audited."
acceptance:
  - A migration file of 100 payees with 2 invalid rows creates 98 payees and reports 2 rows with their reasons.
  - A user without DISB_PAYEE_VIEW_FULL sees masked account numbers.
```

### Request intake

```fr
id: FR-DS-020
title: Receive system-triggered payment requests
brd: [DIS 2.6.0 (p.75), DIS 2.6.2 (p.76), DIS 3.25.0 (p.95)]
actor: System; Disbursement Processor
priority: Must have
screens: Disbursement Workbench (System Requests, No Payee)
description:
  - BIBS modules ask for payments through the Operations disbursement gateway - remittance batches, cashiering refunds and 2307 releases, commission pass-ons, Payment Requests refunds and cash advances, service-fee payouts. Each request (DSR-yyyy-n) carries the RFP number, payee, disbursement type, amount, currency, attachments, the root invoice and the accounting references to settle.
  - When the payee is maintained, BIBS creates the DV at once (FR-DS-030). Refund and remittance requests go straight to the Approver (DISB_AUTO_APPROVER_ROUTING); others start In Process for the Processor.
  - When the payee is not maintained, the request waits under No Payee with a NO_MATCH payee request and the alert DISB_PAYEE_NO_MATCH, and resumes when the payee is authorised. With DISB_NO_PAYEE_ACTION = RETURN it is returned to its source at once.
  - Every DV stage and payment is reported back to the source module.
preconditions:
  - "The source module is configured to use the gateway."
main_flow:
  - A module sends a payment request.
  - BIBS records it and matches its payee.
  - BIBS creates the DV and routes it.
alternate_flows:
  - Duplicate. A second request with the same source reference is refused.
  - No payee. The request waits or is returned (parameter).
rules:
  - [R1, "Types routed straight to the approver - REFUND, REMITTANCE.", Configurable, Parameter DISB_AUTO_APPROVER_ROUTING]
  - [R2, "No-payee behaviour HOLD (default) or RETURN.", Configurable, Parameter DISB_NO_PAYEE_ACTION]
  - [R3, "A request has a positive amount.", Fixed, "-"]
validations:
  - [Amount not positive, A payment request needs a positive amount, DISB_AMOUNT]
  - [Duplicate request, "Request <ref> was already received", DISB_REQUEST_DUPLICATE]
  - [Payee not maintained, "Maintain payee <code> before creating the voucher", DISB_PAYEE_NOT_MAINTAINED]
  - [Wrong request status, "Request <no> is <status> and cannot be <action>", DISB_REQUEST_STATUS]
notifications:
  - "Alert DISB_PAYEE_NO_MATCH; the source module is notified of the status."
audit:
  - "The request keeps its source, its content and its history."
acceptance:
  - An approved remittance batch creates a remittance DV directly For Approval.
  - A refund request for a client without payee waits under No Payee and resumes when the payee is authorised.
```

> [!NOTE] Difference from the BRD
> DIS 3.25.0 asks to auto-reject requests without a maintained payee. The proposed default keeps them waiting so the payee can be added without the source re-sending; the parameter DISB_NO_PAYEE_ACTION = RETURN gives the BRD behaviour (AQ11, AQ12).

```fr
id: FR-DS-021
title: Classify requests by disbursement type
brd: [DIS 3.25.1 (p.96)]
actor: System
priority: Must have
screens: Disbursement Workbench; DV
description: Each request carries a disbursement type from its source or payee class - Remittance, Refund, Payment to supplier, Payment to government agencies, Payment to other bank units, Employee-related, Cash advance, Service fee, Incentive pass-on, BIR 2307 release, Other, and Re-issue of a stale check. The type drives the accounting rule of the DV, the reports and the end-of-day files.
preconditions:
  - "A request is received."
main_flow:
  - BIBS sets the type of the request.
rules:
  - [R1, "Types of the list DISBURSEMENT_TYPE (codes of the gateway).", Configurable, LOV DISBURSEMENT_TYPE]
validations: []
notifications:
  - "None."
audit:
  - "The type is stored on the request and the DV."
acceptance:
  - A request from a government payee is classified Payment to government agencies and appears in DSB-EOD-OTHER.
```

```fr
id: FR-DS-022
title: Match the payee and report requests without payee
brd: [DIS 3.25.2 (p.96)]
actor: System
priority: Must have
screens: Disbursement Workbench (No Payee); Disbursement Reports
description: BIBS matches the payee of a request to the payee master by party code first, then by name when only one payee has that name. Requests without a match are listed in the report of unmatched payees and wait under No Payee (FR-DS-020).
preconditions:
  - "A request is received."
main_flow:
  - BIBS looks up the payee.
  - Without a match, BIBS creates the NO_MATCH payee request and lists it.
rules:
  - [R1, "Match by party code, then by a unique name.", Fixed, "-"]
validations: []
notifications:
  - "Alert DISB_PAYEE_NO_MATCH."
audit:
  - "The match result is stored on the request."
acceptance:
  - The no-match report lists the request with its source, payee code and amount.
```

```fr
id: FR-DS-023
title: Encode requests received by e-mail
brd: [DIS 2.6.1 (p.76)]
actor: Disbursement Processor
priority: Must have
screens: Encode Payment Request
description: The Processor encodes a request received by e-mail - disbursement type, payee (the payee's details fill in from the master), currency, amount, purpose, RFP number, root invoice, expense account and cost centre - and attaches the documents. The request becomes a DV through the template (FR-DS-030) and flows to the checker.
preconditions:
  - "The user has DISB_PROCESS; the payee is maintained."
main_flow:
  - The Processor opens Encode Payment Request and fills it.
  - The Processor attaches the e-mail and documents and saves.
  - BIBS creates the request and its DV In Process.
rules:
  - [R1, "Types for encoding - Remittance, Refund, Payment to supplier, Employee-related, Other.", Configurable, LOV DISBURSEMENT_TYPE]
validations:
  - [Amount not positive, A payment request needs a positive amount, DISB_AMOUNT]
  - [Payee not maintained, "Maintain payee <code> before creating the voucher", DISB_PAYEE_NOT_MAINTAINED]
fields_screen: Encode Payment Request
fields:
  - [Disbursement Type, List, "Yes", LOV DISBURSEMENT_TYPE, "-"]
  - [Payee Code / Name, Look-up, "Yes", Payee master, Active payee]
  - [Currency, List, "Yes", Currencies, 3-letter code]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Purpose, Text, "Yes", "-", Up to 500 characters]
  - [RFP No. / Root Invoice, Text, "No", "-", "-"]
  - [Expense Account / Cost Centre, Look-up, "No", Chart; cost centres, "-"]
  - [Supporting Documents, Attachment, "No", "-", Platform file types]
notifications:
  - "None."
audit:
  - "The request records its source ENCODED and the user."
acceptance:
  - An encoded supplier request of 25,000.00 creates a DV In Process with the supplier's details.
```

```fr
id: FR-DS-024
title: Upload requests and view the fall-out
brd: [DIS 2.5.0 (p.75), DIS 2.5.1 (p.75)]
actor: Disbursement Processor / Team Leader (DISB_UPLOAD)
priority: Must have
screens: Disbursement Uploads (Payment Requests)
description: The user uploads an XLSX, ODS or CSV file of requests - RFP no., disbursement type, amount, currency, payee code and name, purpose, root invoice, expense account, cost centre. Each row is validated; valid rows become requests with their DVs in the processing list; the fall-out lists every failed row with its reason.
preconditions:
  - "The user has DISB_UPLOAD."
main_flow:
  - The user downloads the template, fills it and uploads it.
  - BIBS validates the rows and shows the valid and failed counts.
  - The user commits; the valid rows join the workbench.
rules:
  - [R1, "Upload columns are to be confirmed (AQ12).", Configurable, Bulk handler DISB_REQUESTS]
validations:
  - [Row error, "<reason per row>", "-"]
notifications:
  - "None."
audit:
  - "The upload keeps its file, rows and outcomes."
acceptance:
  - An upload of 20 rows with 3 unknown payees creates 17 requests and lists 3 fall-out rows with the reason.
```

```fr
id: FR-DS-025
title: Work the Disbursement Workbench
brd: [DIS 2.4.0 (p.73), DIS 2.4.1 (p.73), DIS 2.4.2 (p.74), DIS 2.4.3 (p.74), DIS 2.4.4 (p.74), DIS 2.7.1 (p.77), DIS 2.7.2 (p.78), DIS 2.7.3 (p.78)]
actor: Disbursement users
priority: Must have
screens: Disbursement Workbench; Disbursement Voucher (tabs Details, Entry, Instrument, OR / AR and CWT, Documents, E-mails)
description:
  - The workbench lists the requests and DVs by tab - System Requests, No Payee, In Process, For Review, For Approval, Approved, Cancelled / Rejected - with filters on the date received, and filter and sort on every column. Opening a DV shows its request, payee details from the master (name, currency, mode, taxes for suppliers), the attached documents (view and download), the entry, the instrument and the history.
  - A DV being worked by one user is claimed by that user; others see it read only.
preconditions:
  - "The user has DISB_VIEW."
main_flow:
  - The user opens the workbench and a tab, and filters by the received date.
  - The user opens a DV to view or process it.
rules:
  - [R1, "A DV in process belongs to the user who claimed it.", Fixed, "-"]
validations: []
fields_screen: Disbursement Workbench (filters)
fields:
  - [Received From / To, Date, "No", "-", To >= From]
  - [Column filters, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - "Read only; claims are recorded on the work case."
acceptance:
  - Filtering on the received date lists only the requests of that range.
  - The DV shows the payee's TIN and ATC for a supplier.
```

### Processing

```fr
id: FR-DS-030
title: Create the disbursement voucher
brd: [DIS 2.7.5 (p.79), DIS 2.7.4 (p.78)]
actor: System; Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Details)
description: BIBS creates the DV for a request automatically (system requests) or from the template of an encoded request, numbers it DV-yyyy-n and attaches it to the request. The Processor completes the terms - mode of payment, paying bank account, payee account, withholding tax (EWT), purpose, value date, cost centre and expense account. Missing or wrong fields are flagged and the DV cannot be submitted until they are corrected.
preconditions:
  - "The request has a maintained payee."
main_flow:
  - BIBS creates the DV and its proforma entry (FR-DS-031).
  - The Processor fills or corrects the terms and saves.
rules:
  - [R1, "The paying account is active and in the DV currency; the payee account is active.", Fixed, "-"]
  - [R2, "EWT is zero or more and below the gross amount.", Fixed, "-"]
  - [R3, "DV number format DV-<yyyy>-n (AQ13).", Configurable, Document numbering]
validations:
  - [Mandatory term missing, "Complete DV <no>: <missing fields>", DV_INCOMPLETE]
  - [No paying account, Select the paying bank account, DV_INCOMPLETE]
  - [Paying account currency differs, "The paying account must be in <currency>", DV_BANK_CURRENCY]
  - [Payee account inactive, Select an active account of the payee, DV_PAYEE_ACCOUNT]
  - [EWT out of range, The withholding tax must be at least zero and below the gross amount, DV_EWT]
  - [DV not editable, "DV <no> is <stage> and can no longer be changed", DV_NOT_EDITABLE]
fields_screen: Disbursement Voucher, Details
fields:
  - [Mode of Payment, List, "Yes", Allowed modes of the payee, "-"]
  - [Paying Account, List, "Yes", Active BDOIR bank accounts, DV currency]
  - [Payee Account, List, Conditional, Active accounts of the payee, Required for CTA / TT / online banking]
  - [Withholding Tax, Amount, "No", "-", ">= 0 and < gross"]
  - [Purpose, Text, "Yes", "-", "-"]
  - [Value Date, Date, "Yes", "-", Open period]
  - [Cost Centre / Expense Account, Look-up, Conditional, Cost centres; chart, Required for expense types]
notifications:
  - "None."
audit:
  - "DV creation and every change are recorded."
acceptance:
  - An encoded request gets DV-2026-n with its terms pre-filled from the payee.
  - Submitting a DV without paying account is refused with DV_INCOMPLETE.
```

```fr
id: FR-DS-031
title: Create and edit the proforma entry
brd: [DIS 2.7.6 (p.80)]
actor: System; Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Entry)
description: BIBS creates the proforma entry of the DV from the accounting rule of its type (section 5.5). The Processor may edit the lines - account, side, party, cost centre, amount - before approval; each edited line is marked Edited and the Approver sees that the entry was changed. Each line is checked for posting eligibility and the entry must balance. "Rebuild from rule" discards the edits.
preconditions:
  - "The DV is In Process or returned."
main_flow:
  - The Processor opens the Entry tab.
  - The Processor edits a line and saves.
  - BIBS validates the lines and the balance.
alternate_flows:
  - Reset. The Processor creates the entry again from the rule.
rules:
  - [R1, "Lines start from the rule; edits are marked and shown to the Approver.", Fixed, "-"]
  - [R2, "Which lines may be edited and whether an edited entry needs extra approval is open (AQ13).", Fixed, "-"]
validations:
  - [Entry without lines, The proforma entry has no line, DV_ENTRY_EMPTY]
  - [Invalid or unbalanced lines, "<errors>", DV_ENTRY_INVALID]
fields_screen: Entry line
fields:
  - [Side, List, "Yes", "Debit, Credit", "-"]
  - [Account, Look-up, "Yes", Chart of accounts, Postable; eligible]
  - [Party, Look-up, Conditional, Parties, Required for control accounts]
  - [Cost Centre, List, Conditional, Cost centres, Required for cost-centre accounts]
  - [Amount, Amount, "Yes", "-", "> 0"]
notifications:
  - "None."
audit:
  - "Edited lines keep their origin (Rule, Edited, Allocation) and the user."
acceptance:
  - Changing the expense account of a supplier DV marks the line Edited and the Approver sees it.
  - An unbalanced edited entry cannot be saved.
```

```fr
id: FR-DS-032
title: Allocate expenses by cost centre
brd: [DIS 2.7.10 (p.82), DIS 3.30.0 (Add.2 p.9)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Entry - Apply Allocation)
description: For supplier and employee expenses, the Processor attaches an allocation - lines of account, cost centre and amount - that makes up the expense lines of the proforma. The payee's default cost centre fills lines without one; the cost-centre rules (FR-AC-054) apply at posting. The allocation is visible in review and logged.
preconditions:
  - "The DV is In Process."
main_flow:
  - The Processor enters or pastes the allocation lines and clicks **Apply Allocation**.
  - BIBS replaces the expense lines with the allocation lines (origin Allocation).
rules:
  - [R1, "The allocation adds up to the gross amount.", Fixed, "-"]
validations:
  - [Allocation total differs, "The allocation must add up to the gross amount <amount>", DV_ALLOCATION_TOTAL]
fields_screen: Allocation line
fields:
  - [Expense Account, Look-up, "Yes", Chart, Postable]
  - [Cost Centre, List, "Yes", Cost centres, Active]
  - [Amount, Amount, "Yes", "-", "> 0"]
notifications:
  - "None."
audit:
  - "Allocation lines are recorded with origin Allocation."
acceptance:
  - A 30,000.00 rent DV allocated 20,000.00 to HO and 10,000.00 to Cebu posts two expense lines with those cost centres.
```

```fr
id: FR-DS-033
title: Process the payment by one of the seven modes
brd: [DIS 2.7.0 (p.77)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Instrument)
description: After approval each DV has one instrument of its mode - Check (printed from the paying account's cheque book), ATD, Credit to Account (extracted in the DCTF), Manager's Check / Demand Draft, Credit Ticket, Telegraphic Transfer or Online Banking. The instrument follows the life cycle of its mode (section 5.2); its number is the check number or a number of the mode's series.
preconditions:
  - "The DV is approved."
main_flow:
  - The Processor opens the Instrument tab and performs the next step of the mode (print, e-mail, release, confirm).
  - BIBS moves the instrument to the next status and reports it to the source.
rules:
  - [R1, "One instrument per approved DV.", Fixed, "-"]
  - [R2, "Transitions per mode as in section 5.2.", Fixed, "-"]
validations:
  - [DV not approved, The DV has no instrument until it is approved, DV_NOT_APPROVED]
  - [Status not allowed, "<mode> <instrument> is <status> and cannot become <status>", INSTRUMENT_STATUS]
  - [Mode without form, "<mode> has no printed form; it is processed by file", DISB_NO_FORM]
notifications:
  - "The source module is notified of the instrument status."
audit:
  - "Every status change is an instrument event with source USER, SYSTEM, UPLOAD or JOB."
acceptance:
  - A check DV prints check 000124 from the paying account's series and becomes Printed.
```

```fr
id: FR-DS-034
title: Process an Authority to Debit
brd: [DIS 2.7.7 (p.81)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Instrument - Print, E-mail, Debited)
description: For ATD, BIBS generates the ATD from the template DSB_ATD (status Printed), the Processor e-mails it to the processing branch with the debit instruction and the requester in copy (status Emailed), and records the branch's confirmation (status Debited).
preconditions:
  - "The DV is approved with mode ATD; the branch e-mail is in BRANCH_EMAIL."
main_flow:
  - The Processor prints the ATD.
  - The Processor e-mails it to the branch.
  - On the branch confirmation the Processor marks it Debited.
rules:
  - [R1, "Branch mailboxes are the list BRANCH_EMAIL (AQ09).", Configurable, LOV BRANCH_EMAIL]
validations:
  - [E-mail of a non-ATD instrument, Only an authority to debit is e-mailed, DISB_NOT_ATD]
notifications:
  - "The ATD e-mail goes to the branch with the requester in copy."
audit:
  - "The e-mail is logged on the DV (E-mails tab)."
acceptance:
  - An ATD printed, e-mailed and confirmed shows Printed, Emailed and Debited in its history.
```

```fr
id: FR-DS-035
title: Process Manager's Checks, Demand Drafts, Credit Tickets and Telegraphic Transfers
brd: [DIS 2.7.8 (p.81), DIS 2.7.9 (p.82)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Instrument)
description: For MC / DD, BIBS generates the form (template DSB_MC_DD) for sign-off and transaction at the branch (Printed); the Processor records the issued MC / DD received from the branch (Received) and its release to the payee (Released). For Credit Ticket and TT, BIBS generates the form (DSB_CREDIT_TICKET, DSB_TT) for sign-off (Printed) and the Processor records the branch's validation (Debited).
preconditions:
  - "The DV is approved with the mode."
main_flow:
  - The Processor prints the form.
  - The Processor records the branch result.
rules:
  - [R1, "Form layouts are drafts until AQ14.", Configurable, Document templates]
validations:
  - [Status not allowed, "<mode> <instrument> is <status> and cannot become <status>", INSTRUMENT_STATUS]
notifications:
  - "The source module is notified of Released or Debited."
audit:
  - "Every status is an instrument event."
acceptance:
  - A manager's check goes Printed, Received, Released; a credit ticket goes Printed, Debited.
```

> [!NOTE] BRD text
> DIS 2.7.9 is titled "Demand Draft / Manager's Check" but describes Credit Ticket / TT steps; this FR covers both.

```fr
id: FR-DS-036
title: Submit the DV for review
brd: [DIS 2.7.11 (p.82)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Submit)
description: The Processor submits a complete DV; it moves to For Review (workflow DISB_VOUCHER) with the confirmation of its totals.
preconditions:
  - "The DV is In Process and complete."
main_flow:
  - The Processor clicks **Submit** and confirms.
  - The DV is For Review in the TL's queue.
rules:
  - [R1, "Only a complete DV is submitted.", Fixed, "-"]
validations:
  - [Incomplete DV, "Complete DV <no>: <missing fields>", DV_INCOMPLETE]
notifications:
  - "The TLs see the DV in For Review."
audit:
  - "The transition is in the workflow history."
acceptance:
  - A submitted DV appears in the For Review tab.
```

```fr
id: FR-DS-037
title: Maintain the employee and cost-centre master and report the headcount
brd: [DIS 3.30.1 (Add.2 p.9-10), DIS 3.30.2 (Add.2 p.10-11)]
actor: Comptrollership administrator (EMPLOYEE_MAINTAIN)
priority: Must have
screens: Setup > Employees; Report Centre (Headcount per Cost Centre)
description: The employee master holds the employee number (unique), name, position, unit and branch, cost centre, hiring and separation dates and status. It gives the default cost centre of employee payees and the headcount per cost centre report, filtered by date and unit and exported to Excel and PDF.
preconditions:
  - "The user has EMPLOYEE_MAINTAIN."
main_flow:
  - The administrator adds or edits an employee.
  - Users run the headcount report.
rules:
  - [R1, "The employee number is unique; the separation date is not before the hiring date.", Fixed, "-"]
  - [R2, "Whether a cost centre is per employee or per unit is open (AQ26).", Configurable, Employee master]
validations:
  - [Cost centre missing, "Enter the employee's cost centre", COST_CENTER_REQUIRED]
  - [Separation before hiring, The separation date is before the hiring date, SEPARATION_BEFORE_HIRING]
  - [Branch of another company, The branch belongs to another company, BRANCH_OF_OTHER_COMPANY]
fields_screen: Employee
fields:
  - [Employee No., Text, "Yes", "-", Unique]
  - [Name / Position, Text, "Yes", "-", "-"]
  - [Branch / Unit, List, "Yes", Branches; units, Active]
  - [Cost Centre, List, "Yes", Cost centres, Active]
  - [Hired / Separated, Date, "Yes / No", "-", Separation >= hiring]
notifications:
  - "None."
audit:
  - "Every change is audited."
acceptance:
  - A second employee with the same number is refused.
  - The headcount report of 30 September counts the active employees per cost centre.
```

### Review, approval and cancellation

```fr
id: FR-DS-040
title: Review, return and submit DVs for approval
brd: [DIS 2.13.0 (p.87), DIS 2.14.0 (p.88), DIS 2.15.0 (p.88)]
actor: Disbursement Team Leader (DISB_REVIEW); Approver (return)
priority: Must have
screens: Disbursement Workbench (For Review); Disbursement Voucher
description: The TL reviews the DVs For Review - details, entry (edited lines marked), documents - and submits them for approval, or returns them to the Processor with a reason from the list and special instructions. The Approver may also return a DV.
preconditions:
  - "The DV is For Review; the TL is not its processor."
main_flow:
  - The TL opens a DV For Review and checks it.
  - The TL clicks **Submit for Approval**.
alternate_flows:
  - Return. The TL returns it with a reason (DISB_RETURN_REASON) and remarks; it is In Process again.
rules:
  - [R1, "The checker is never the processor.", Fixed, "-"]
validations:
  - [Checker is the processor, The processor cannot check their own voucher, DV_FOUR_EYES]
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "The Approvers see the DV For Approval; the Processor sees a returned DV with its reason."
audit:
  - "Review, return and remarks are in the workflow history."
acceptance:
  - A DV returned with "Incomplete supporting documents" is back In Process with the reason visible.
```

```fr
id: FR-DS-041
title: Approve and post one or several DVs
brd: [DIS 2.19.0 (p.91)]
actor: Disbursement Approver (DISB_APPROVE)
priority: Must have
screens: Disbursement Workbench (For Approval - bulk); Disbursement Voucher (Approve)
description: The Approver approves a single DV or selects several. Approval posts the DV entry - through the accounting rule, or the edited lines as given - creates the instrument, and reports the new status to the source (DV assigned). Each DV of a bulk approval is posted in its own transaction with a result per DV. A posting failure keeps the DV For Approval with posting status FAILED and the error.
preconditions:
  - "The DV is For Approval; the Approver is neither its processor nor its checker."
main_flow:
  - The Approver selects the DVs and clicks **Approve**, with remarks.
  - BIBS posts each DV and creates its instrument.
  - BIBS lists the results.
rules:
  - [R1, "The approver of a DV is never its processor or checker.", Fixed, "-"]
  - [R2, "Posting at approval (AQ13 may move it to release).", Fixed, "-"]
validations:
  - [Approver is processor or checker, The approver of a voucher cannot be its processor or checker, DV_FOUR_EYES]
  - [Not for approval, "DV <no> is <stage>", DV_NOT_FOR_APPROVAL]
  - [Posting failed, "DV <no> could not be posted: <reason>", DV_POSTING_FAILED]
  - [Entry empty, "DV <no> has no entry", DV_ENTRY_EMPTY]
notifications:
  - "The source module is notified of the status."
audit:
  - "Approval, remarks, the journal number and the posting status are on the DV."
acceptance:
  - Approving a refund DV posts Dr refund payable / Cr bank and creates its instrument.
  - Approving 5 DVs of which one has no rule approves 4 and leaves one For Approval with DV_POSTING_FAILED.
```

```fr
id: FR-DS-042
title: Reject a DV
brd: [DIS 2.21.0 (p.92)]
actor: Disbursement Approver
priority: Must have
screens: Disbursement Voucher (Reject)
description: The Approver rejects a DV For Approval with a reason and remarks. The DV is REJECTED (final) and the request is returned to its source, which is notified.
preconditions:
  - "The DV is For Approval."
main_flow:
  - The Approver clicks **Reject**, chooses the reason and writes remarks.
  - BIBS rejects the DV and returns the request.
rules:
  - [R1, "A rejected DV is final.", Fixed, "-"]
validations:
  - [Reject without reason, "Select a reason for 'reject'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "The source module is notified (RETURNED)."
audit:
  - "Reason and remarks are in the history."
acceptance:
  - A rejected refund DV sends the refund request back to its preparer in Payment Requests.
```

```fr
id: FR-DS-043
title: Cancel a DV In Process or For review
brd: [DIS 2.9.0 (p.85), DIS 2.18.0 (p.91)]
actor: Disbursement Processor (In Process); Team Leader (For review)
priority: Must have
screens: Disbursement Workbench (search); Disbursement Voucher (Cancel)
description: On request of the source unit, a DV In Process (Processor) or For review (TL) is cancelled with a reason from DISB_CANCEL_REASON and remarks. The user finds the DV by payee, client, amount or reference. The DV is CANCELLED and the request goes back to its source.
preconditions:
  - "The DV is In Process or For Review."
main_flow:
  - The user searches and opens the DV.
  - The user clicks **Cancel**, chooses the reason and writes remarks.
rules:
  - [R1, "Cancellation reasons of the list DISB_CANCEL_REASON (to confirm, AQ15).", Configurable, LOV DISB_CANCEL_REASON]
validations:
  - [Cancel without reason, "Select a reason for 'cancel'", WORKFLOW_REASON_REQUIRED]
  - [Request already in a voucher, "Cancel or reject the voucher of <request>", DISB_REQUEST_IN_VOUCHER]
notifications:
  - "The source module is notified (CANCELLED)."
audit:
  - "Reason and remarks are recorded."
acceptance:
  - A duplicate supplier DV In Process is cancelled with reason "Duplicate request or DV".
```

```fr
id: FR-DS-044
title: Cancel an approved DV and regularise the accounting
brd: [DIS 2.20.0 (p.92)]
actor: Disbursement Approver
priority: Must have
screens: Disbursement Voucher (Cancel approved DV)
description: On request, the Approver cancels an approved DV with a reason. BIBS reverses the DV journal (DV:<no>:CANCEL; posting status REVERSED, or REVERSAL_FAILED with the error), cancels the instrument when it is not final, returns the request to its source and publishes CANCELLED. The source restores its records - Payment Requests reopens the refund; for a remittance DV, Remittance reverses the batch postings with the opposite sign (remittance, incentive, CPC2 and each deduction, which gets its amount back), puts the invoices back in review with the remittance lock, records the cancelled DV and returns the batch to Review in process for its next send cycle (re-approval sends a new request under <batch>/R<n>). When another team has locked one of the invoices since, the batch is left as it is and the remittance processors are notified. Open check-cancellation hand-offs that name the DV are closed.
preconditions:
  - "The DV is APPROVED; its instrument is not final (negotiated, credited, debited)."
main_flow:
  - The Approver opens the DV and clicks **Cancel approved DV** with the reason.
  - BIBS reverses the entry, cancels the instrument and notifies the source.
alternate_flows:
  - Reversal failure. The DV shows REVERSAL_FAILED; it is listed in DSB-UNREGULARIZED and DISB_UNREGULARIZED is raised.
rules:
  - [R1, "Cancellation after release (check void versus stale) is open (AQ15).", Fixed, "-"]
validations:
  - [Instrument final, "<mode> <instrument> is <status> and cannot be cancelled", INSTRUMENT_FINAL]
  - [Cancel without reason, "Select a reason for 'cancel'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "The source module is notified (CANCELLED); alert DISB_UNREGULARIZED when not regularised."
audit:
  - "The reversal journal is linked to the DV."
acceptance:
  - Cancelling an approved refund DV posts the reversal and reopens the refund request.
  - A negotiated check's DV cannot be cancelled.
```

```fr
id: FR-DS-045
title: Regularise the accounting of every DV and list what is not regularised
brd: [DIS 3.27.0 (p.99; Add.1 p.31)]
actor: System; Disbursement users
priority: Must have
screens: Disbursement Workbench (Unregularised); Disbursement Reports
description: Every DV state change with an accounting effect posts in the same flow - approval, cancellation, negotiated and stale checks, re-issue. Lines stay editable only while the DV is not posted (Addendum 1). The report and the workbench list the DVs not regularised - posting FAILED or REVERSAL_FAILED, and approved DVs without their OR / AR tag.
preconditions:
  - "None."
main_flow:
  - A DV changes stage.
  - BIBS posts the entry, or records the failure.
  - Users review the unregularised list and act.
rules:
  - [R1, "Posted entries are never edited; unposted ones are.", Fixed, "-"]
  - [R2, "Every approved DV counts as unregularised until tagged, until AQ17 says which DVs need an OR / AR.", Fixed, "-"]
validations:
  - [Edit a posted DV, "DV <no> is <stage>; return it first", DV_NOT_EDITABLE]
notifications:
  - "Alert DISB_UNREGULARIZED."
audit:
  - "Posting status and errors are on the DV."
acceptance:
  - A DV whose approval posting failed appears in DSB-UNREGULARIZED with the error.
```

### Instrument statuses and tagging

```fr
id: FR-DS-050
title: Tag instrument statuses
brd: [DIS 2.8.0 (p.83), DIS 2.8.1 (p.83), DIS 2.8.2 (p.84), DIS 2.8.3 (p.84), DIS 2.8.4 (p.85)]
actor: Disbursement Processor
priority: Must have
screens: Disbursement Voucher (Instrument)
description: The Processor tags the statuses that need a person - a check Released when the payee receives it (release date, received by); an ATD Emailed and Debited after the branch confirms; a Credit Ticket or TT Debited after the branch confirms; an MC / DD Received from the branch and Released to the payee.
preconditions:
  - "The DV is approved and the instrument is in the previous status."
main_flow:
  - The Processor opens the instrument and records the step with its date.
rules:
  - [R1, "Transitions per mode as in section 5.2.", Fixed, "-"]
validations:
  - [Status not allowed, "<mode> <instrument> is <status> and cannot become <status>", INSTRUMENT_STATUS]
fields_screen: Instrument step
fields:
  - [Date, Date, "Yes", "-", Not in the future]
  - [Released to / Received by, Text, Conditional, "-", Release of a check or MC / DD]
notifications:
  - "The source module is notified of paid statuses."
audit:
  - "Each tag is an instrument event (USER)."
acceptance:
  - A check released on 2 October shows Released with the date and the person who received it.
```

```fr
id: FR-DS-051
title: Edit an instrument status with approval
brd: [DIS 2.8.5 (p.85)]
actor: Disbursement Processor (request); Team Leader (DISB_STATUS_APPROVE)
priority: Must have
screens: Disbursement Voucher (Instrument - Request Status Edit); status edits list
description: When a status was tagged wrongly, the Processor requests another status of the same mode with a reason. The TL approves it (the status changes) or rejects it. The requester never approves their own edit.
preconditions:
  - "No other edit of the instrument is pending."
main_flow:
  - The Processor requests the new status with the reason.
  - The TL approves; BIBS applies the status.
alternate_flows:
  - Reject. The TL rejects with a reason; the status stays.
rules:
  - [R1, "Workflow DISB_STATUS_EDIT - REQUESTED, APPLIED, REJECTED.", Fixed, "-"]
validations:
  - [Not a status of the mode, "<status> is not another status of a <mode>", STATUS_EDIT_INVALID]
  - [Edit pending, A status edit of this instrument already waits for approval, STATUS_EDIT_PENDING]
  - [Instrument changed meanwhile, "The instrument is now <status>; request the edit again", STATUS_EDIT_STALE]
  - [Approver is the requester, A status edit cannot be approved by its requestor, MAKER_CHECKER_VIOLATION]
fields_screen: Request Status Edit
fields:
  - [New Status, List, "Yes", Statuses of the mode, Another status]
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - "The TL sees pending edits."
audit:
  - "The edit and its decision are recorded."
acceptance:
  - A check wrongly tagged Released is set back to Printed after the TL approves the edit.
```

```fr
id: FR-DS-052
title: Tag statuses automatically
brd: [DIS 3.26.0 (p.96), DIS 3.26.3 (p.97), DIS 3.26.5 (p.98), DIS 3.26.6 (p.98), DIS 3.26.7 (p.99)]
actor: System
priority: Must have
screens: Disbursement Voucher (Instrument - status history)
description: BIBS sets statuses without a person where it can - an ATD, Credit Ticket, TT or MC / DD is Printed when its form is generated; an online-banking payment is Approved when the DV is approved and Debited when the BOB approval report is uploaded (by voucher reference and amount); a credit to account is Extracted at end of day and Credited by the upload of FR-DS-053; checks are Negotiated or Stale (FR-DS-053, FR-DS-054).
preconditions:
  - "The DV is approved."
main_flow:
  - The triggering event occurs (form generated, EOD, upload, job).
  - BIBS moves the instrument and records the source SYSTEM, UPLOAD or JOB.
rules:
  - [R1, "The BOB approval report is uploaded until a BOB interface exists (AQ09).", Fixed, "-"]
validations:
  - [Upload row with unknown DV, "No DV <no>", DV_NOT_FOUND]
  - [Amount differs, "Amount <amount> differs from <amount> of <instrument>", INSTRUMENT_AMOUNT]
  - [Mode differs, "DV <no> is paid by <mode>, not <mode>", INSTRUMENT_MODE]
notifications:
  - "The source module is notified of paid statuses."
audit:
  - "Each automatic status keeps its source and file."
acceptance:
  - Printing an ATD sets it Printed; uploading the BOB report sets an online-banking payment Debited.
```

```fr
id: FR-DS-053
title: Upload deposited-checks and credited-accounts files
brd: [DIS 2.22.0 (p.93), DIS 3.26.1 (p.97), DIS 3.26.4 (p.98)]
actor: Disbursement Processor / Team Leader (DISB_UPLOAD)
priority: Must have
screens: Disbursement Uploads (Negotiated Checks, Credited Accounts, BOB Approvals)
description: The user uploads the bank's files - deposited checks (check no., amount, date deposited), credited accounts (reference, amount, account no.) and BOB approvals (voucher reference, amount, BOB reference). Each row is matched to its instrument; a negotiated check is tagged Negotiated and posts its clearing entry (FR-DS-055); a credited account is tagged Credited.
preconditions:
  - "The user has DISB_UPLOAD."
main_flow:
  - The user uploads the file.
  - BIBS validates each row and updates the instruments.
  - The upload report lists the updated and refused rows.
rules:
  - [R1, "The uploads take a minimal CSV until the bank layouts are given (AQ09).", Configurable, Bulk handlers]
validations:
  - [Check not found, "No printed or released check <no> (<n> found)", CHECK_NOT_FOUND]
  - [Amount differs, "Amount <amount> differs from <amount> of <instrument>", INSTRUMENT_AMOUNT]
notifications:
  - "The source module is notified of paid statuses."
audit:
  - "The upload keeps its file and outcomes; instrument events carry the file."
acceptance:
  - A deposited-checks file with check 000124 for 15,000.00 tags it Negotiated.
  - A row whose amount differs is refused with INSTRUMENT_AMOUNT.
```

```fr
id: FR-DS-054
title: Stale checks after 180 days and re-issue them
brd: [DIS 3.26.2 (p.97)]
actor: System (job DISB_CHECK_STALE); Disbursement Processor (re-issue)
priority: Must have
screens: Disbursement Voucher (Instrument - Re-issue); Disbursement Reports
description: Every day at 00:20 the job tags Stale every check still Printed or Released DISB_STALE_DAYS (180) days after its print date, posts the stale entry to Miscellaneous Liability - stale checks (FR-DS-055) and raises DISB_CHECK_STALE. A stale check can be re-issued - a STALE_REISSUE request creates a new DV for the payee.
preconditions:
  - "The check is Printed or Released."
main_flow:
  - The job finds the checks older than 180 days.
  - BIBS tags them Stale and posts the entry.
alternate_flows:
  - Re-issue. The Processor re-issues a stale check; a new DV follows the normal flow.
rules:
  - [R1, "Stale after DISB_STALE_DAYS = 180 days from the print date (30-720).", Configurable, Parameter DISB_STALE_DAYS]
validations:
  - [Re-issue of a check not stale, Only a stale check is re-issued, CHECK_NOT_STALE]
notifications:
  - "Alert DISB_CHECK_STALE."
audit:
  - "The stale tag is an instrument event (JOB)."
acceptance:
  - A check printed on 1 April and not negotiated is Stale on 28 September.
```

```fr
id: FR-DS-055
title: Post the entries of negotiated and stale checks
brd: [DIS 3.27.1 (p.100)]
actor: System
priority: Must have
screens: Disbursement Voucher (Instrument, Entry)
description: With DISB_CHECK_CLEARING = ON, an approved check DV credits checks outstanding (account 2241). When the check is negotiated BIBS posts checks outstanding against the bank; when it is stale, checks outstanding against Miscellaneous Liability - stale checks of the payee. A re-issue posts the stale liability against the paying account. The entries flow to FRBS through the ledger.
preconditions:
  - "The check is negotiated or stale."
main_flow:
  - The upload or the job changes the check status.
  - BIBS posts the event with its seed rule (section 5.5, rows 8-10).
rules:
  - [R1, "Clearing model on or off.", Configurable, "Parameters DISB_CHECK_CLEARING, DISB_CHECK_CLEARING_ACCOUNT (2241)"]
  - [R2, "Stale-check accounting and re-issue are to be confirmed (AQ02, AQ14).", Configurable, Accounting rules]
validations: []
notifications:
  - "None."
audit:
  - "The journals are linked to the instrument (CHK:<id>:NEG / STALE)."
acceptance:
  - Negotiating a 15,000.00 check posts Dr 2241 / Cr bank 15,000.00.
```

```fr
id: FR-DS-056
title: Tag the Official Receipt or Acknowledgement Receipt
brd: [DIS 2.10.0 (p.86), DIS 2.10.1 (p.86), DIS 2.10.2 (p.86)]
actor: Disbursement Processor (DISB_TAG)
priority: Must have
screens: Disbursement Voucher (OR / AR and CWT)
description: When the payee's OR or AR arrives, the Processor finds the DV and tags the OR / AR number, its date, the date received, the amount and remarks. For remittance DVs the insurer OR uploaded in Remittance (BRD-2) is shown and does not need to be keyed again. A tagged DV leaves the unregularised list.
preconditions:
  - "The DV is approved."
main_flow:
  - The Processor searches the DV and opens OR / AR and CWT.
  - The Processor enters the receipt and saves.
rules:
  - [R1, "Only approved DVs are tagged.", Fixed, "-"]
  - [R2, "Which DVs need an OR / AR back is open (AQ17).", Fixed, "-"]
validations:
  - [DV not approved, "Only an approved DV is tagged; DV <no> is <stage>", DV_NOT_APPROVED]
fields_screen: OR / AR tag
fields:
  - [OR / AR No., Text, "Yes", "-", Up to 40 characters]
  - [Receipt Date, Date, "Yes", "-", "-"]
  - [Received On, Date, "Yes", "-", "-"]
  - [Amount, Amount, "No", "-", ">= 0"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - "The tag is kept with user and time."
acceptance:
  - A supplier's OR 12345 tagged on its DV removes the DV from the unregularised list.
```

```fr
id: FR-DS-057
title: Tag creditable withholding tax received or released
brd: [DIS 2.11.0 (p.86), DIS 2.11.1 (p.86), DIS 2.11.2 (p.87)]
actor: Disbursement Processor (DISB_TAG)
priority: Must have
screens: Disbursement Voucher (OR / AR and CWT); Tax & Statutory > Certificates Received
description: The Processor tags the CWT of a DV - Received for the certificates an insurer issues on commission and incentives, Released for the BIR 2307 BDOI gives a supplier - with the certificate number, period covered, date received or released and amount. Insurer certificates are recorded in one register (Certificates Received), which posts TAX_CWT_CERT_RECEIVED (AR-BIR on commission or incentives to AR-BIR on hand) and feeds the SAWT and the CWT report.
preconditions:
  - "The DV is approved (tag); the user has DISB_TAG or TAX_MANAGE (register)."
main_flow:
  - The Processor opens the DV's CWT tag and enters the certificate.
  - For an insurer certificate, the Processor records it in Certificates Received with its income lines.
rules:
  - [R1, "Directions RECEIVED and RELEASED.", Fixed, "-"]
  - [R2, "One register of received certificates for Disbursement and Commission (CMRID.015).", Fixed, "-"]
validations:
  - [Period reversed, The period covered ends before it starts, CWT_PERIOD]
  - [Certificate incomplete, Give the certificate number and the withholding agent, CERTIFICATE_INCOMPLETE]
  - [No income line, Give at least one income payment with the tax withheld, CERTIFICATE_LINES]
  - [Date received in the future, "Give the date received, not in the future", CERTIFICATE_RECEIVED_ON]
fields_screen: CWT tag
fields:
  - [Direction, Option, "Yes", "Received, Released", "-"]
  - [Certificate No., Text, "Yes", "-", "-"]
  - [Period From / To, Date, "Yes", "-", To >= From]
  - [Received / Released On, Date, "Yes", "-", "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
notifications:
  - "None."
audit:
  - "Tags and certificates are recorded; a cancelled certificate is reversed (CRT:<id>:CANCEL)."
acceptance:
  - Recording an insurer 2307 of 2,000.00 on commission posts Dr AR-BIR on hand / Cr AR-BIR on commission 2,000.00.
```

```fr
id: FR-DS-058
title: Generate BIR Form 2307 for suppliers
brd: [DIS 2.12.0 (p.87)]
actor: Disbursement Processor; Tax users
priority: Must have
screens: Tax & Statutory > BIR Form 2307
description: BIBS fills BIR Form 2307 per payee and quarter from the withholding on the payments and saves it as PDF, one or in batch.
preconditions:
  - "Withholding was recorded on payments of the quarter."
main_flow:
  - The user chooses the quarter and generates the certificates.
  - The user downloads the PDFs.
rules:
  - [R1, "Form 2307 covers a calendar quarter.", Fixed, "-"]
validations:
  - [Not a quarter, "Form 2307 covers a calendar quarter, not <period>", NOT_A_QUARTER]
  - [Nothing to issue, "No payee of <period> needs a new certificate", NO_CERTIFICATES]
notifications:
  - "None."
audit:
  - "Issued certificates are registered."
acceptance:
  - The 2307 of a supplier for Q3 shows the income and tax withheld of the quarter.
```

### End of day, funding and masters

```fr
id: FR-DS-060
title: Run the end of day of Disbursement
brd: [DIS 2.16.0 (p.88), DIS 2.16.3 (p.89), DIS 2.16.4 (p.89), DIS 2.16.5 (p.89)]
actor: Disbursement Team Leader / Approver (DISB_EOD)
priority: Must have
screens: Disbursement End of Day (Run End of Day, Runs, outputs)
description: For a business date, the end of day freezes the approved DVs and produces the outputs - the DCTF credit file (FR-DS-061), the check print batch (FR-DS-062), the ATD, MC / DD, Credit Ticket and TT forms, the vouchers and the end-of-day reports (FR-DS-081). Each output is downloadable from the run (EOD-yyyy-n).
preconditions:
  - "The user has DISB_EOD; the date has no run."
main_flow:
  - The user chooses the business date and clicks **Run End of Day**.
  - BIBS produces the outputs and lists them with their counts.
rules:
  - [R1, "One run per business date.", Fixed, "-"]
validations:
  - [Date already run, "The end of day of <date> was already processed", EOD_ALREADY_RUN]
notifications:
  - "None; confirmations follow (FR-DS-063)."
audit:
  - "The run keeps its outputs, counts and user."
acceptance:
  - The end of day of 30 September lists the DCTF, the check batch, the forms and six reports.
  - A second run of 30 September is refused.
```

```fr
id: FR-DS-061
title: Produce the Direct Credit Transaction File
brd: [DIS 2.16.1 (p.88)]
actor: System (end of day); Disbursement Team Leader
priority: Must have
screens: Disbursement End of Day (DCTF output)
description: The end of day writes the DCTF of the credit-to-account DVs - a header with the date (MMddyyyy) and the file name, and one 89-character detail per payment - 12-digit account number, 30-character payee name, 12 blanks, 20-character system reference, amount 000000000000.00, upper case (Appendix B, p.147). Each included payment becomes Extracted. The user forwards the file to TPD for ACA processing.
preconditions:
  - "Credit-to-account DVs are approved for the date."
main_flow:
  - The end of day writes the file.
  - The user downloads it and sends it to TPD.
rules:
  - [R1, "No trailer or totals until TPD's full specification is given (AQ09).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The file is kept with the run."
acceptance:
  - A CTA of 12,500.00 to account 001234567890 appears as one 89-character line with amount 000000012500.00.
```

```fr
id: FR-DS-062
title: Print checks and vouchers
brd: [DIS 2.16.2 (p.88), DIS 2.16.6 (p.89)]
actor: Disbursement Processor / Team Leader
priority: Must have
screens: Disbursement End of Day (check batch, vouchers); Disbursement Voucher (Document)
description: The end of day produces the check print batch - each check takes the next leaf of the paying account's cheque book and uses the template DSB_CHECK - and the vouchers (DSB_VOUCHER). Vouchers print one at a time or in batch with the print options of the report platform.
preconditions:
  - "Check DVs are approved; the paying account has an active cheque book."
main_flow:
  - The user downloads the check batch and prints it.
  - The user prints the vouchers.
rules:
  - [R1, "Check and voucher layouts and signatories are drafts until AQ14.", Configurable, Document templates]
  - [R2, "CHECK_SERIES_LOW is raised when DISB_CHECK_SERIES_WARNING (20) leaves remain.", Configurable, Parameter DISB_CHECK_SERIES_WARNING]
validations: []
notifications:
  - "Alert CHECK_SERIES_LOW."
audit:
  - "Printed check numbers are recorded on the instruments."
acceptance:
  - Three check DVs print checks 000125 to 000127 in order.
```

```fr
id: FR-DS-063
title: E-mail the payment confirmations and the remittance schedule
brd: [DIS 2.7.12 (p.83)]
actor: System; Disbursement Team Leader
priority: Must have
screens: Disbursement End of Day (Confirm); Disbursement Voucher (E-mails)
description: After the end of day, BIBS e-mails to each payee of the day the payment advice (template DSB_PAYMENT_ADVICE). The confirmations of a run are sent once; the e-mails are logged on the DVs.
preconditions:
  - "The run is completed and not confirmed."
main_flow:
  - The user clicks **Confirm** (or the scheduler runs it).
  - BIBS sends the advices and marks the run CONFIRMED.
rules:
  - [R1, "One confirmation per run.", Fixed, "-"]
  - [R2, "The remittance schedule is not attached to the insurer's advice (chapter 12, CLR-DS-15).", Fixed, "-"]
validations:
  - [Already sent, "The confirmations of <run> were already sent", EOD_CONFIRMED]
notifications:
  - "Each payee receives its payment advice."
audit:
  - "Each e-mail is logged."
acceptance:
  - Confirming the run of 30 September sends one advice per payee paid that day.
```

```fr
id: FR-DS-064
title: Fund the main BDOIR account with a verifier and two approvers
brd: [DIS 2.17.0 (p.90), DIS 2.17.1 (p.90), DIS 2.17.2 (p.90), DIS 2.17.3 (p.90), DIS 2.17.4 (p.90; Add.1 p.33)]
actor: Disbursement Team Leader (maker, verifier); Disbursement Approvers
priority: Must have
screens: Account Funding; Funding Request
description: A TL creates a funding request (FND-yyyy-n) - source and target BDOIR accounts, amount, purpose - and submits it. Another TL verifies it, and two approvers approve it in turn; either approver can decline it with remarks, and each step can return it to the maker. When the second approval is given, BIBS posts the transfer (DISB_FUND_TRANSFER). The transfer itself is done in BDO Business Online Banking; its reference is recorded on the request. The log-in to BOB is outside BIBS.
preconditions:
  - "The maker has DISB_FUNDING_REQUEST."
main_flow:
  - The TL creates and submits the request.
  - Another TL verifies it.
  - Approver 1 and approver 2 approve it.
  - BIBS posts the transfer.
alternate_flows:
  - Decline or return with remarks at any approval step.
rules:
  - [R1, "Verifier differs from the maker; each approver differs from the maker, the verifier and the other approver.", Fixed, "-"]
  - [R2, "Accounts, limits and approver order are to be confirmed (AQ10).", Configurable, Bank accounts]
validations:
  - [Amount not positive, The amount must be positive, FUNDING_AMOUNT]
  - [Same account, The source and target accounts must differ, FUNDING_SAME_ACCOUNT]
  - [Currencies differ, "Both accounts must be in <currency>", FUNDING_CURRENCY]
  - [Maker verifies, The maker cannot verify the funding request, FUNDING_FOUR_EYES]
  - [Maker or verifier approves, The maker or verifier cannot approve the funding request, FUNDING_FOUR_EYES]
  - [Same approver twice, The second approval must be given by another approver, FUNDING_FOUR_EYES]
  - [Wrong stage, "Funding request <no> is <stage>", FUNDING_NOT_EDITABLE]
fields_screen: New Funding Request
fields:
  - [Source Account / Target Account, List, "Yes", BDOIR bank accounts, Different; same currency]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Purpose / Remarks, Text, "Yes / No", "-", "-"]
  - [BOB Reference, Text, "No", "-", "-"]
notifications:
  - "Each step's owners see the request in their tab."
audit:
  - "Maker, verifier, approvers, remarks and the journal are recorded."
acceptance:
  - disbtl creates, disbtl2 verifies, disbappr and disbappr2 approve; the transfer posts.
  - disbappr cannot give both approvals.
```

```fr
id: FR-DS-070
title: Maintain the check series
brd: [DIS 2.23.0 (p.93), DIS 2.23.1 (p.93), DIS 2.23.2 (p.93)]
actor: Disbursement Approver (MASTER_MAINTAIN)
priority: Must have
screens: Bank Accounts and Checks (cheque books)
description: Each paying account has cheque books with first, last and next check numbers and a status. The Approver adds the beginning series and may correct its range only before the first check is printed; the correction keeps the previous range.
preconditions:
  - "The user has MASTER_MAINTAIN."
main_flow:
  - The Approver adds a cheque book with its first and last numbers.
  - The Approver corrects the range before any check is used.
rules:
  - [R1, "A range is edited only before its first leaf is used.", Fixed, "-"]
validations:
  - [Range edited after use, Only an active cheque book with no leaf used can be edited, CHEQUE_BOOK_IN_USE]
  - [Range invalid, "Cheque range <first>-<last> is invalid", INVALID_CHEQUE_RANGE]
  - [Range overlaps, Cheque range overlaps another book of the account, CHEQUE_RANGE_OVERLAP]
fields_screen: Cheque book
fields:
  - [First Check No. / Last Check No., Number, "Yes", "-", First <= Last]
notifications:
  - "Alert CHECK_SERIES_LOW."
audit:
  - "Edits keep the user, time and previous range."
acceptance:
  - A new series 000100-000199 is used from 000100.
```

```fr
id: FR-DS-071
title: Maintain the BDOIR bank accounts and their status
brd: [DIS 2.24.0 (p.93), DIS 2.24.1 (p.93), DIS 2.24.2 (p.94; Add.1 p.33)]
actor: Disbursement Approver
priority: Must have
screens: Bank Accounts and Checks
description: The Approver maintains the BDOIR bank accounts (GL account, currency, branch, notification format) and tags them active or inactive; a status change waits for authorisation. Inactive accounts are not offered on new DVs.
preconditions:
  - "The user has MASTER_MAINTAIN."
main_flow:
  - The Approver adds an account or requests a status change.
  - Another user authorises it.
rules:
  - [R1, "An account is usable when authorised and ACTIVE.", Fixed, "-"]
validations:
  - [Authoriser is the maker, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
notifications:
  - "Pending changes appear for authorisation."
audit:
  - "Every change is audited."
acceptance:
  - An inactive account cannot be chosen as the paying account.
```

### Disbursement reports

```fr
id: FR-DS-080
title: Run Disbursement reports on demand
brd: [DIS 2.3.0 (p.71), DIS 2.3.1 (p.72), DIS 2.3.2 (p.72), DIS 2.3.3 (p.72), DIS 2.3.4 (p.72), DIS 2.3.5 (p.72), DIS 2.3.6 (p.73), DIS 2.3.7 (p.73), DIS 2.3.8 (p.73), DIS 2.3.9 (p.73)]
actor: Disbursement users (DISB_REPORT_VIEW / EXPORT)
priority: Must have
screens: Disbursement Reports; Report Centre
description: The Disbursement reports (section 6.1) run at any time for a date or period; the user views the details, copies them, exports to XLSX, ODS or PDF, saves the file, previews and prints with print options (paper, orientation, fit to width), as in, FR-AC-020 to FR-AC-023.
preconditions:
  - "The user has DISB_REPORT_VIEW."
main_flow:
  - The user selects a report and the period.
  - The user views, exports or prints it.
rules:
  - [R1, "Report category Disbursement; archived.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Runs and exports are archived."
acceptance:
  - The masterlist of September exports to XLSX and prints landscape.
```

```fr
id: FR-DS-081
title: Generate the end-of-day reports
brd: [DIS 3.28.0 (p.100), DIS 3.28.2 (p.101)]
actor: System (end of day)
priority: Must have
screens: Disbursement End of Day (outputs); Disbursement Reports
description: The end of day generates for the date the Remittance, Refund, Summary, Payment to supplier, Employee-related and Other disbursement reports (Appendix B fields) with the DCTF, and archives them with the run.
preconditions:
  - "The end of day of the date has run."
main_flow:
  - The end of day generates the reports.
  - Users download them from the run.
rules:
  - [R1, "Layouts follow Appendix B; withholding on remittance shows 2% and 15%.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The reports are archived with the run."
acceptance:
  - The Remittance EOD report of 30 September lists every remittance DV approved that day.
```

```fr
id: FR-DS-082
title: Run the real-time Disbursement reports
brd: [DIS 3.28.3 (p.101)]
actor: Disbursement users
priority: Must have
screens: Disbursement Reports
description: For any date range - the masterlist of all disbursements; unreleased checks aged current-30 to 151-180 days with subtotals; CWT / BIR 2307 on commission (AR-BIR on commission and on incentives against the certificates, per payee and insurer, with variances); Authority to Debit; Miscellaneous Liability stale checks aged to 181 days and over; and the cash flow (amount per savings account, checks, ATD and CTA in process and for crediting, inter-office).
preconditions:
  - "The user has DISB_REPORT_VIEW."
main_flow:
  - The user runs the report for the range.
rules:
  - [R1, "Ageing buckets as Appendix B (30-day steps).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Runs are archived."
acceptance:
  - The unreleased-checks report puts a check printed 45 days ago in 31-60.
```

```fr
id: FR-DS-083
title: Report payees and upload fall-outs
brd: [DIS 3.28.1 (p.100), DIS 3.28.4 (p.102)]
actor: Disbursement users
priority: Must have
screens: Disbursement Reports
description: The payee report lists the payees with name, address, account number (masked unless permitted), mode of payment, disbursement type and source. The fall-out report lists, for a period, every refused row of the request uploads with its reason.
preconditions:
  - "The user has DISB_REPORT_VIEW."
main_flow:
  - The user runs the report for the period.
rules:
  - [R1, "Account numbers are masked without DISB_PAYEE_VIEW_FULL.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Runs are archived."
acceptance:
  - The fall-out report of September lists the 3 refused rows of an upload with their reasons.
```

### Incentives and the invoice family

```fr
id: FR-DS-090
title: Compute CPC2 incentives per remittance
brd: [DIS 3.29.2 (Add.2 p.8-9)]
actor: System (remittance extraction and approval)
priority: Must have
screens: Remittance batch (CPC2 column and totals, Settlement tab); remittance payment request and schedule
description:
  - When a remittance batch is extracted, BIBS looks up for each line the active incentive criteria of code CPC2 maintained by TSU on the products matrix (BRD-3 PMADD07-08) - risk code, segment and insurer, effective on the booking date, with a rate and an optional minimum gross premium. For a qualifying line it computes CPC2 as the rate on the basic premium remitted, with output VAT at the invoice's commission VAT ratio, per remittance and not cumulative.
  - CPC2 and its VAT are deducted from the amount payable to the insurer and shown apart from commission on the line, the batch totals, the payment request and the schedule. On approval BIBS posts OPS_REMIT_CPC2 per batch (reference RMB:<batch>:CPC2; amounts GROSS, CPC2_INCOME, OUTPUT_VAT) to CPC2 incentive income. Each line keeps the criterion code and rate for audit.
preconditions:
  - "An active CPC2 criterion with a rate exists for the product, segment and insurer."
main_flow:
  - The remittance processor extracts the batch.
  - BIBS computes CPC2 on each qualifying line.
  - On approval BIBS posts OPS_REMIT_CPC2.
rules:
  - [R1, "CPC2 applies to the lines matched by an active CPC2 criterion with a RATE basis.", Configurable, Incentive criteria (code CPC2)]
  - [R2, "Optional minimum gross premium per criterion.", Configurable, "Criterion rule parameter minimumPremium"]
  - [R3, "Base (basic premium remitted), VAT treatment and fixed-amount or rule criteria are to be confirmed (AQ24, OQ39, PQ04).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Each line keeps the CPC2 code and rate; the journal is linked to the batch."
acceptance:
  - A packaged Motor line with a basic premium remitted of 100,000.00 and a CPC2 rate of 2% shows CPC2 2,000.00 plus VAT, and the batch payable is reduced by that amount.
  - Approving the batch posts OPS_REMIT_CPC2 with the CPC2 income.
```

```fr
id: FR-DS-091
title: Issue the service invoice of the early incentive automatically
brd: [DIS 3.29.1 (Add.2 p.7-8)]
actor: System (remittance approval, booking)
priority: Must have
screens: Remittance batch (Settlement tab - SI); Service invoices; Service invoice types (trigger ON_INCENTIVE)
description: When a remittance batch with an early incentive is approved, BIBS issues once per batch a booking service invoice of type EARLY_INCENTIVE to the insurer - incentive, its VAT and withholding tax at EARLY_INCENTIVE_WTAX_RATE (2%) of the incentive, summed per line - and links it on the batch. The incentive OR carries the same withholding per line and names the service invoice. A manual issue of a service invoice type with trigger ON_INCENTIVE is refused.
preconditions:
  - "The batch has a qualified early incentive (BRD-2)."
main_flow:
  - The remittance approver approves the batch.
  - BIBS issues the service invoice and links it to the batch.
  - The incentive OR names the service invoice.
rules:
  - [R1, "Withholding tax rate 2%.", Configurable, Parameter EARLY_INCENTIVE_WTAX_RATE]
  - [R2, "One early-incentive service invoice per batch; a re-sent batch does not issue a second one.", Fixed, "-"]
  - [R3, "The entry of the insurer's 2% withholding (Dr 1611 / Cr 2211) waits for AQ25; until then the incentive is deducted in full from the remittance.", Fixed, "-"]
validations:
  - [Manual issue of an automatic type, "Service invoices of type <type> are issued automatically with the early remittance incentive", SERVICE_INVOICE_AUTOMATIC_ONLY]
notifications:
  - "None."
audit:
  - "The service invoice number is kept on the batch; the invoice keeps its batch reference."
acceptance:
  - An early incentive of 5,000.00 produces one service invoice with 100.00 withholding tax, linked to its batch.
  - Issuing an EARLY_INCENTIVE service invoice by hand is refused with SERVICE_INVOICE_AUTOMATIC_ONLY.
```

```fr
id: FR-DS-092
title: Report CPC2 incentives
brd: [DIS 3.29.0 (Add.2 p.7)]
actor: Disbursement users
priority: Must have
screens: Disbursement Reports (CPC2 report)
description: Disbursement users run the CPC2 report for a period. It lists, per remittance batch line, the user, product and role, the CPC2 code and rate, the basic premium remitted, the CPC2 amount and its VAT, with totals that agree with the CPC2 income posted in the ledger (FR-DS-090). The report exports to Excel and PDF and every run is logged. Its layout is confirmed with the CPC2 definition (AQ24).
preconditions:
  - "The user holds the Disbursement report permission."
main_flow:
  - The user opens Disbursement Reports, chooses the CPC2 report and enters the period.
  - BIBS lists the CPC2 lines of the remittance batches approved in the period, with the breakdown and totals.
  - The user exports the report to Excel or PDF.
rules:
  - [R1, "Layout to be agreed with the CPC2 definition (AQ24).", Configurable, Report layout]
  - [R2, "The totals agree with the CPC2 income posted for the period.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Every run and export is archived."
acceptance:
  - The CPC2 report of September lists every CPC2 line of the September batches and its total equals the CPC2 income posted in September.
```

```fr
id: FR-DS-093
title: Link related transactions to one invoice number
brd: [DIS 3.27.2 (p.100)]
actor: System; Operations, Disbursement and ACSL users
priority: Must have
screens: Invoice Search; Invoice 360 (Invoice Family tab); booking invoice (Root Invoice chip); ACSL Case; Disbursement Voucher
description: Booking sets the root invoice number of each invoice - the invoice itself for an original booking, the original booking for an endorsement or cancellation - and the ledger carries it. The Invoice 360 Invoice Family tab lists all invoices of the root with the family totals; remittance payment requests carry the root invoice when the batch has one family. BIR invoice numbers stay unique; the root links them.
preconditions:
  - "None."
main_flow:
  - An endorsement or cancellation is booked.
  - Booking sets its root invoice number.
  - Users see the family on the invoice and on ACSL cases.
rules:
  - [R1, "Endorsements and cancellations get their own invoice numbers, linked to the root (AQ29).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The root is stored on the booked invoice and the ledger invoice."
acceptance:
  - The family of an invoice lists its endorsement and its cancellation with the family totals.
```

<!-- pagebreak -->

## Payment Requests (Marketing)

 Access (MKT 1.1.0-1.1.3) is FR-DS-001.

```fr
id: FR-PQ-001
title: Receive refund and cash-advance requests in Requests Home
brd: [MKT 1.2.0 (p.105), MKT 1.3.0 (p.106), MKT 1.4.0 (p.106), MKT 1.5.0 (p.106), MKT 1.6.0 (p.106)]
actor: Marketing Processor, Reviewer and Approver; HR
priority: Must have
screens: Requests Home (tabs by stage with counts); Request
description: Marketing AOs raise refund requests to clients (RRF-yyyy-n) and employees raise cash-advance requests (RFP-yyyy-n) in BIBS. Requests Home lists them by stage with counts, searches by request number, payee, reference or DV number, and filters by kind and date. The user selects one request, or several for a bulk endorse or approve with a result per request. The request page shows the header, the accounts (RRF lines), the documents, the validations, the Disbursement status and the history.
preconditions:
  - "The user has PRQ_VIEW."
main_flow:
  - The user opens Requests Home and a tab.
  - The user searches or filters, and opens a request or selects several.
rules:
  - [R1, "Tabs follow the stages of section 5.6.", Fixed, "-"]
validations: []
fields_screen: Requests Home (filters)
fields:
  - [Search, Text, "No", "-", "Request no., payee, reference, DV no."]
  - [Kind, List, "No", "Refund, Cash advance, Check cancellation", "-"]
  - [Date From / To, Date, "No", "-", To >= From]
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - The For Review tab of the reviewer shows the count and the requests waiting for him.
  - Selecting 3 requests and clicking Endorse shows one result per request.
```

```fr
id: FR-PQ-002
title: Access the unapplied payment reports
brd: [MKT 1.7.0 (p.106), MKT 1.7.1 (p.106), MKT 1.7.2 (p.107), MKT 1.7.3 (p.107)]
actor: Marketing Processor, Reviewer and Approver
priority: Must have
screens: Report Centre (Operations reports)
description: The Marketing roles hold OPS_REPORT_VIEW and run the Operations reports of Cashiering on unapplied payments for a date range, download them as XLSX, save the file and print it (FR-AC-020 to FR-AC-023). A report of unapplied payments restricted to the Marketing user's segment is not proposed; its layout waits for AQ18 (chapter 12, CLR-PQ-01).
preconditions:
  - "The user holds OPS_REPORT_VIEW."
main_flow:
  - The user opens the Report Centre, chooses the report and the date range.
  - The user views, downloads or prints it.
rules:
  - [R1, "Report access by permission; no segment filter.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Runs and exports are archived."
acceptance:
  - A Marketing processor exports the unapplied-payment report of September to XLSX.
```

> [!NOTE] Difference from the BRD
> MKT 1.7.0 expects an unapplied-payment report for Marketing. Marketing uses the Cashiering reports without a segment filter; a Marketing-specific report is added if BDOI gives its layout (AQ18).

```fr
id: FR-PQ-003
title: Fill in the Refund Request Form
brd: [MKT 1.10.0 (p.107; Appendix D)]
actor: Marketing Processor (AO)
priority: Must have
screens: New Refund Request; Request (Refund)
description:
  - The AO fills in the RRF - segment, reference, requesting unit, purpose, currency, mode of payment (Credit to account, Check, ATD, Inter-office, Manager's check, Demand draft), the client's account number and name for credit to account, and one line per account (1 to 50) - AR no., client code, assured name, invoice no., amount, refund reason, branch / unit, categories A and B, account name.
  - All lines of a request belong to one client. The payee is the client. The RRF prints as PDF from the template of Appendix D.
preconditions:
  - "The user has PRQ_CREATE; the ARs are in the ledger."
main_flow:
  - The AO opens New Refund Request and fills the header and the lines.
  - The AO saves the draft (RRF-yyyy-n, DRAFT).
alternate_flows:
  - Edit. A request in Draft or Preparing is changed and saved again.
rules:
  - [R1, "One client per refund request.", Fixed, "-"]
  - [R2, "1 to 50 accounts per request.", Fixed, "-"]
  - [R3, "Refund reasons Cancelled policy, Overpayment, Double payment, Premium decrease (endorsement), Others.", Configurable, LOV REFUND_REASON]
  - [R4, "Categories A and B hold 'Others' until AQ18.", Configurable, "LOVs RRF_CATEGORY_A, RRF_CATEGORY_B"]
validations:
  - [No or too many lines, "A refund request has between 1 and 50 accounts", PRQ_LINES_REQUIRED]
  - [Line incomplete, Every line needs the AR number and the assured name, PRQ_LINE_INCOMPLETE]
  - [Several clients, All accounts of a refund request belong to one client (AQ18), PRQ_ONE_CLIENT]
  - [Invoice not in the ledger, "Invoice <no> is not in the ledger", PRQ_INVOICE_UNKNOWN]
  - [Amount not positive, The amount must be positive with at most two decimals, PRQ_AMOUNT_INVALID]
  - [Currency wrong, Currency must be a 3-letter code, PRQ_CURRENCY_INVALID]
  - [Request not editable, "<request> is <stage> for this action", PRQ_WRONG_STAGE]
fields_screen: New Refund Request
fields:
  - [Segment / Reference / Requesting Unit, Text, "No", "-", "Up to 40 / 80 / 60 characters"]
  - [Purpose, Text, "No", "-", Up to 500 characters]
  - [Currency, List, "No", Currencies, 3-letter code]
  - [Mode of Payment, List, "Yes", LOV PRQ_PAYMENT_MODE, "-"]
  - [Account No. / Account Name, Text, Conditional, "-", Credit to account - 10 to 16 digits]
  - [AR No., Text, "Yes", "-", Unique among live requests]
  - [Client Code / Assured Name, Text, "Yes", Client, One client]
  - [Invoice No., Text, "No", Ledger, Exists]
  - [Amount, Amount, "Yes", "-", "> 0, 2 decimals"]
  - [Reason, List, "Yes", LOV REFUND_REASON, "-"]
  - [Branch / Unit; Category A; Category B, Text / List, "No", "LOVs RRF_CATEGORY_A, _B", "-"]
notifications:
  - "None."
audit:
  - "The request keeps its trail (created, changed) with user and time."
acceptance:
  - An RRF with lines of two clients is refused with PRQ_ONE_CLIENT.
  - An RRF of 51 lines is refused.
```

```fr
id: FR-PQ-004
title: Fill in the Request for Payment of a cash advance
brd: [MKT 1.10.0 (p.107; Appendix D)]
actor: Employee / Marketing Processor
priority: Must have
screens: New Cash Advance; Request (Cash advance)
description: The employee fills in the RFP - segment, reference, requesting unit, RFP type (Cash advance, Petty cash, Others), purpose, currency, employee number and name, mode of payment, account number and name, and amount. The payee is the employee. The RFP prints as PDF from the template of Appendix D.
preconditions:
  - "The user has PRQ_CREATE."
main_flow:
  - The employee opens New Cash Advance, fills it and saves (RFP-yyyy-n, DRAFT).
rules:
  - [R1, "RFP types Cash advance, Petty cash, Others.", Configurable, LOV PRQ_RFP_TYPE]
validations:
  - [Purpose missing, Give the purpose of the cash advance (Appendix D RFP), PRQ_PURPOSE_REQUIRED]
  - [Payee missing, Give the payee code and name, PRQ_PAYEE_REQUIRED]
  - [Amount not positive, The amount must be positive with at most two decimals, PRQ_AMOUNT_INVALID]
fields_screen: New Cash Advance
fields:
  - [RFP Type, List, "No", LOV PRQ_RFP_TYPE, "-"]
  - [Purpose, Text, "Yes", "-", Up to 500 characters]
  - [Employee No. / Name, Text, "Yes", Employee master, "-"]
  - [Mode of Payment, List, "Yes", LOV PRQ_PAYMENT_MODE, "-"]
  - [Account No. / Name, Text, Conditional, "-", Credit to account]
  - [Amount, Amount, "Yes", "-", "> 0, 2 decimals"]
notifications:
  - "None."
audit:
  - "As FR-PQ-003."
acceptance:
  - A cash advance without purpose is refused with PRQ_PURPOSE_REQUIRED.
```

```fr
id: FR-PQ-005
title: Assign, re-assign and return requests
brd: [MKT 1.8.0 (p.107), MKT 1.9.0 (p.107)]
actor: Marketing Reviewer (PRQ_ASSIGN); any handler (return)
priority: Must have
screens: Request (Assign, Return)
description: The reviewer assigns a refund request to a preparer, or re-assigns it; the request moves to Preparing and appears in the preparer's work. Any handler can return a request to the previous handler with a reason and remarks (preparer, reviewer or requester, depending on the stage - section 5.6).
preconditions:
  - "The request is in a stage where the action is allowed."
main_flow:
  - The reviewer clicks **Assign**, chooses the user and writes a comment.
  - The request is Preparing for that user.
alternate_flows:
  - Return. The handler returns the request with a reason (RETURN_REASON).
rules:
  - [R1, "Returns go one step back (section 5.6).", Fixed, "-"]
validations:
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
  - [Wrong stage, "<request> is <stage> for this action", PRQ_WRONG_STAGE]
fields_screen: Assign
fields:
  - [User, Look-up, "Yes", Users with PRQ_CREATE, "-"]
  - [Comment, Text, "No", "-", Up to 500 characters]
notifications:
  - "The assignee and the returned-to handler are notified (PRQ_REQUEST_STATUS)."
audit:
  - "Assignments and returns are in the trail."
acceptance:
  - A request re-assigned from mktao to mktao2 appears in mktao2's work.
```

```fr
id: FR-PQ-006
title: Validate the refund of a cancelled policy with ACSL and Cashiering
brd: [MKT 1.11.0 (p.108), ACSL 2.5.5 (p.117; Add.1 p.34)]
actor: Marketing Processor; ACSL Processor; Cashier
priority: Must have
screens: Request (Validations); ACSL Cases; Cashiering tasks
description:
  - When a refund line has the reason Cancelled policy, the preparer sends the request for validation. BIBS opens, per line, a validation for ACSL - a case of type Account analysis request that checks the cancelled premium and whether the insurer returned the remitted premium - and one for Cashiering - a task RVL-yyyy-n that confirms the payment was reinstated to unapplied and gives the new AR number.
  - When all validations are confirmed the request goes For review; when one is rejected it returns to the preparer. When a validating module is not installed the validation is handed over and its result is entered by hand.
preconditions:
  - "The request has a Cancelled policy line; it is Draft or Preparing."
main_flow:
  - The preparer clicks **Send for Validation**.
  - ACSL and Cashiering record their results.
  - BIBS moves the request For review (all confirmed) or back to Preparing.
rules:
  - [R1, "One ACSL and one Cashiering validation per cancelled-policy line.", Fixed, "-"]
  - [R2, "The validation stage has a 48-hour SLA.", Configurable, Workflow PRQ_REFUND]
validations:
  - [Result already given, "This validation was already <status>", PRQ_VALIDATION_DONE]
fields_screen: Validation result
fields:
  - [Confirmed, Option, "Yes", "Confirmed, Rejected", "-"]
  - [New AR No., Text, Conditional, "-", Cashiering confirmation]
  - [Remarks, Text, "No", "-", Up to 500 characters]
notifications:
  - "ACSL and Cashiering see the case or task; the preparer is notified of the result."
audit:
  - "Each validation keeps its validator, result and time."
acceptance:
  - A refund of a cancelled policy goes For review only after ACSL confirms and Cashiering gives the new AR number.
```

```fr
id: FR-PQ-007
title: Upload and view supporting documents
brd: [MKT 1.12.0 (p.108), MKT 1.13.0 (p.108), MKT 2.22.0 (p.111)]
actor: Marketing users
priority: Must have
screens: Request (Documents)
description: Users attach supporting documents to the request with a document type; they select one or several, view them, download one file or a ZIP of the selected files, and save them. The documents are linked to the request and their references travel with the payment request to Disbursement, where the DV shows them.
preconditions:
  - "The user has ATTACHMENT_MANAGE."
main_flow:
  - The user uploads a file with its type.
  - The user selects files and views or downloads them.
rules:
  - [R1, "Accepted types - pdf, png, jpg, jpeg, xlsx, docx, csv, ods, odt, xls, doc, msg, eml; up to 10 MB each.", Configurable, "Attachment settings"]
validations:
  - [Type not allowed, "Only these file types are allowed: <list>", ATTACHMENT_TYPE_NOT_ALLOWED]
notifications:
  - "None."
audit:
  - "Files are stored with a SHA-256 checksum; every action is logged."
acceptance:
  - A PDF and an XLSX attached to an RRF appear on the refund DV.
```

> [!NOTE] Difference from the BRD
> MKT 1.12.0 lists .txt files; the platform does not accept .txt attachments. MKT 1.13.0 asks for a side-by-side preview; files open one at a time in the viewer.

```fr
id: FR-PQ-008
title: Submit, review and endorse requests
brd: [MKT 1.14.0 (p.109), MKT 1.15.0 (p.109)]
actor: Marketing Processor (submit); Reviewer (endorse)
priority: Must have
screens: Request (Submit, Endorse); Requests Home (bulk endorse)
description: The preparer submits the request for review. The reviewer checks it and endorses it to the approver, or returns it to the preparer. The reviewer is never the user who raised or last moved the request.
preconditions:
  - "The request is Draft or Preparing (submit); For review (endorse)."
main_flow:
  - The preparer clicks **Submit for Review**.
  - The reviewer clicks **Endorse** with a comment.
rules:
  - [R1, "Four eyes - no endorsement or approval by the requester or the previous actor.", Fixed, "-"]
validations:
  - [Same user, "You raised or already moved <request>: another user decides", PRQ_FOUR_EYES]
  - [Wrong stage, "<request> is <stage> for this action", PRQ_WRONG_STAGE]
  - [AR already refunded, "AR <no> is already refunded by request <no> (MKT 2.23.0)", PRQ_DUPLICATE_AR]
notifications:
  - "The next handler is notified (PRQ_REQUEST_STATUS)."
audit:
  - "Every action is in the trail."
acceptance:
  - mktao cannot endorse the request he submitted.
```

```fr
id: FR-PQ-009
title: Approve or decline refunds, cash advances and check cancellations
brd: [MKT 1.16.0 (p.109), MKT 1.16.1 (p.109), MKT 1.16.2 (p.109), MKT 1.16.3 (p.109)]
actor: Marketing Approver; HR (cash advances)
priority: Must have
screens: Request (Approve, Return); Requests Home (bulk approve)
description: The approver approves a request with remarks, or returns it to the reviewer with a reason; a request is declined by returning or cancelling it with a reason. A refund goes to Disbursement on approval. A cash advance goes to HR, whose approval sends it to Disbursement; HR can return it to the Marketing approver. An approved check cancellation is sent to the Disbursement approvers (FR-PQ-011).
preconditions:
  - "The request is For approval (HR approval for HR)."
main_flow:
  - The approver opens the request and clicks **Approve** with remarks.
  - BIBS moves the request to its next stage (section 5.6).
alternate_flows:
  - Return. The approver returns it with a reason (RETURN_REASON).
rules:
  - [R1, "Four eyes as FR-PQ-008.", Fixed, "-"]
  - [R2, "The approver of each kind and amount is to be confirmed (AQ18).", Configurable, Workflow permissions]
validations:
  - [Same user, "You raised or already moved <request>: another user decides", PRQ_FOUR_EYES]
  - [Wrong stage, "<request> is <stage> for this action", PRQ_WRONG_STAGE]
notifications:
  - "The requester is notified (PRQ_REQUEST_STATUS)."
audit:
  - "Approvals and remarks are in the trail."
acceptance:
  - An approved cash advance is For HR approval; after HR approves it is Sent to Disbursement.
```

```fr
id: FR-PQ-010
title: Cancel a request before approval
brd: [MKT 1.17.0 (p.110)]
actor: Requester, preparer or reviewer
priority: Must have
screens: Request (Cancel)
description: Before approval a request can be cancelled with a reason (VOID_REASON) and remarks - by the requester in Draft, by the preparer or reviewer in Preparing and For review. Cancelling releases the AR numbers of its lines. A check cancellation is withdrawn the same way.
preconditions:
  - "The request is not yet approved."
main_flow:
  - The user clicks **Cancel**, chooses the reason and writes remarks.
rules:
  - [R1, "Approved requests are cancelled only through Disbursement.", Fixed, "-"]
validations:
  - [Reason missing, "Select a reason for 'cancel'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "None."
audit:
  - "The reason is in the trail."
acceptance:
  - A cancelled RRF frees its AR for a new request.
```

```fr
id: FR-PQ-011
title: Request the cancellation of a disbursed check
brd: [MKT 1.19.0 (p.110)]
actor: Marketing Processor; Reviewer; Approver
priority: Must have
screens: Cancel a Check; Request (Check cancellation)
description: For a refund or cash advance paid by check, manager's check or demand draft, the user raises a check-cancellation request (CCR-yyyy-n) with the paid request, the check number, a reason and remarks. It is reviewed and approved like the other requests and then sent to the Disbursement approvers as the hand-off DV_CANCELLATION; the approver cancels the DV (FR-DS-044), which closes the hand-off. One live cancellation per paid request.
preconditions:
  - "The target request is disbursed by check with a DV."
main_flow:
  - The user opens Cancel a Check, chooses the paid request and the reason.
  - Reviewer and approver act (section 5.6).
  - BIBS sends the hand-off to Disbursement.
rules:
  - [R1, "Reasons of DISB_CANCEL_REASON.", Configurable, LOV DISB_CANCEL_REASON]
validations:
  - [Unknown target, "Request <no> is not a refund or cash advance", PRQ_TARGET_UNKNOWN]
  - [Not paid by check, "<request> was not paid by check", PRQ_TARGET_NOT_CHECK]
  - [No DV yet, "<request> has no disbursement voucher yet", PRQ_TARGET_NOT_DISBURSED]
  - [Cancellation pending, "A cancellation of the check of <request> is already in progress", PRQ_CANCELLATION_PENDING]
fields_screen: Cancel a Check
fields:
  - [Paid Request, Look-up, "Yes", Disbursed requests, Paid by check]
  - [Check No., Text, "No", Instrument, "-"]
  - [Reason, List, "Yes", LOV DISB_CANCEL_REASON, "-"]
  - [Remarks, Text, "No", "-", Up to 500 characters]
notifications:
  - "The Disbursement approvers receive the hand-off."
audit:
  - "The request and the DV cancellation are linked."
acceptance:
  - A check cancellation of a CTA refund is refused with PRQ_TARGET_NOT_CHECK.
```

```fr
id: FR-PQ-012
title: Track the status of requests and extract them
brd: [MKT 1.18.0 (p.110), MKT 1.18.1 (p.110), MKT 2.26.0 (p.112)]
actor: Marketing users
priority: Must have
screens: Requests Home; Request (History, Disbursement); Report Centre
description: Each request shows its stage, its trail and, once sent, the DV number and Disbursement status. For a date range, the status report lists requests with their current stage and the register lists all requests with their details; both export to XLSX or ODS and print.
preconditions:
  - "The user has PRQ_VIEW."
main_flow:
  - The user runs the report for the range and exports or prints it.
rules:
  - [R1, "Report category Payment Requests.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Runs are archived."
acceptance:
  - The status report of September lists each request with its stage and DV number.
```

```fr
id: FR-PQ-013
title: Receive the disbursement confirmation
brd: [MKT 1.20.0 (p.111)]
actor: System
priority: Must have
screens: Request (Disbursement tab)
description: Disbursement reports every DV status back. When the refund or cash advance is paid the request becomes Disbursed and the requester is notified. When Disbursement returns, rejects or cancels it, the request goes back to the preparer (refund) or requester (cash advance) and can be resent under a new reference (RRF.../n).
preconditions:
  - "The request was sent to Disbursement."
main_flow:
  - Disbursement pays the DV.
  - BIBS moves the request to Disbursed and notifies the requester.
rules:
  - [R1, "Paid statuses move the request to Disbursed; returned or cancelled move it back.", Fixed, "-"]
validations: []
notifications:
  - "PRQ_REQUEST_STATUS to the requester."
audit:
  - "The Disbursement status is in the trail."
acceptance:
  - A refund whose check is released shows Disbursed with the DV number.
```

```fr
id: FR-PQ-014
title: Prevent duplicate refunds by AR number
brd: [MKT 2.23.0 (p.111)]
actor: System
priority: Must have
screens: New Refund Request
description: An AR number can be on one live request only. The same AR twice on a request, or an AR already on another live request, is refused. Cancelling a request frees its ARs.
preconditions:
  - "None."
main_flow:
  - The user saves or submits a refund request.
  - BIBS checks the AR numbers.
rules:
  - [R1, "Unique live AR number.", Fixed, "-"]
validations:
  - [AR twice on the request, "AR <no> appears twice on the request (MKT 2.23.0)", PRQ_DUPLICATE_AR]
  - [AR on another request, "AR <no> is already refunded by request <no> (MKT 2.23.0)", PRQ_DUPLICATE_AR]
notifications:
  - "None."
audit:
  - "-"
acceptance:
  - A second RRF for AR 2026-00123 is refused while the first is live.
```

```fr
id: FR-PQ-015
title: Send approved requests to Disbursement and to HR
brd: [MKT 2.24.0 (p.111)]
actor: System
priority: Must have
screens: Request (Disbursement tab)
description: On final approval a refund is sent to Disbursement as a payment request of type Refund; a cash advance goes to HR first and is sent on HR approval as type Cash advance. The payment request carries the request number, payee, amount, mode, account and documents. The Disbursement flow of FR-DS-020 follows.
preconditions:
  - "The request is approved (and HR-approved for a cash advance)."
main_flow:
  - BIBS sends the request through the gateway.
  - The request is Sent to Disbursement.
rules:
  - [R1, "Refund and cash-advance DVs go straight to the Disbursement approver (DISB_AUTO_APPROVER_ROUTING contains REFUND).", Configurable, Parameter DISB_AUTO_APPROVER_ROUTING]
validations: []
notifications:
  - "None."
audit:
  - "The DSR reference is on the request."
acceptance:
  - An approved refund appears in the Disbursement workbench with its RRF number.
```

```fr
id: FR-PQ-016
title: Record the client's payout account on approval
brd: [MKT 2.25.0 (p.112; Add.1 p.34-35), MKT 2.25.1 (p.112)]
actor: System
priority: Must have
screens: Client (Payout accounts); New Refund Request (payout fields)
description: On approval of a refund, BIBS adds the client's payout details to the client record - for credit to account the payee name and BDO account number, for check the payee name. The same details are not recorded twice. The form offers the recorded accounts of the client.
preconditions:
  - "The refund is approved."
main_flow:
  - The approver approves the refund.
  - BIBS records the payout account unless it exists.
rules:
  - [R1, "BDO account number of 10 to 16 digits until AQ19.", Configurable, Client payout rule]
validations:
  - [Account number wrong, A credit to account needs the BDO account number (10 to 16 digits AQ19), PAYOUT_ACCOUNT_INVALID]
  - [Details missing, Give the payout mode and the account or check payee name, PAYOUT_INCOMPLETE]
notifications:
  - "None."
audit:
  - "The client record keeps who recorded the account and from which request."
acceptance:
  - The second refund of the same client to the same account does not add a second payout account.
```

```fr
id: FR-PQ-017
title: Liquidate a cash advance
brd: [MKT 1.10.0 (Appendix D)]
actor: Employee; Checker (Marketing / Comptrollership); Comptrollership administrator (accounts)
priority: Must have
screens: Request (Liquidation); Liquidation Accounts
description: After a cash advance is disbursed the employee records the liquidation - job level, cost centre, remarks and up to 60 fieldwork days with the date, particulars and expenses (per diem, representation, transport, lodging, others). The checker returns it or posts it (event PRQ_CA_LIQUIDATION) - the expenses by category against the advance, with the excess returned (cash returned) or the shortage payable to the employee. The account of each expense role is set on the Liquidation Accounts screen.
preconditions:
  - "The cash advance is Disbursed."
main_flow:
  - The employee enters the fieldwork days and submits.
  - The checker posts the liquidation.
alternate_flows:
  - Return. The checker returns it to the employee.
rules:
  - [R1, "The checker is not the employee.", Fixed, "-"]
  - [R2, "Expense-role accounts are configuration (AQ02).", Configurable, Liquidation Accounts]
validations:
  - [Not disbursed, Only a disbursed cash advance is liquidated, PRQ_NOT_LIQUIDABLE]
  - [No day, Enter at least one fieldwork day before submitting, PRQ_LIQUIDATION_EMPTY]
  - [Day incomplete, Every fieldwork day needs its date and particulars, PRQ_LIQUIDATION_LINE]
  - [Too many days, A liquidation has up to 60 fieldwork days, PRQ_LIQUIDATION_LINES]
  - [Expense negative, Expenses are zero or positive with at most two decimals, PRQ_AMOUNT_INVALID]
  - [Account not set, "Comptrollership has not set the account of <role> for liquidations (AQ02)", PRQ_LIQUIDATION_ACCOUNT_MISSING]
  - [Liquidation locked, "<liquidation> is <status>", PRQ_LIQUIDATION_LOCKED]
  - [Checker is the employee, A liquidation is checked by someone other than the employee, PRQ_FOUR_EYES]
fields_screen: Liquidation (fieldwork day)
fields:
  - [Date, Date, "Yes", "-", "-"]
  - [Particulars, Text, "Yes", "-", Up to 250 characters]
  - ["Per Diem, Representation, Transport, Lodging, Others", Amount, "No", "-", ">= 0, 2 decimals"]
notifications:
  - "None."
audit:
  - "The liquidation keeps its trail and journal number."
acceptance:
  - Posting a liquidation of 4,500.00 against an advance of 5,000.00 posts the expenses and 500.00 cash returned against the advance of 5,000.00.
```

<!-- pagebreak -->

## ACSL

 Access (ACSL 1.1.0-1.1.3) is FR-DS-001.

### Input files and reports

```fr
id: FR-AS-001
title: Generate the input files and run ACSL reports on demand
brd: [ACSL 2.2.0 (p.115), ACSL 2.3.0 (p.115), ACSL 2.3.1 (p.116), ACSL 2.3.2 (p.116), ACSL 2.3.3 (p.116), ACSL 2.3.4 (p.116), ACSL 2.3.5 (p.116), ACSL 2.3.6 (p.116), ACSL 2.14.2 (p.123)]
actor: ACSL users (ACSL_REPORT_VIEW / EXPORT)
priority: Must have
screens: Report Centre (category ACSL)
description: ACSL runs its reports at any time without IT - the list of all booked accounts with their financial details for a period and GL account (the input file of the investigations, named "List of all booked accounts_<GL account name>_<period>"), the SOA reconciliation, the SOA upload log and the GL-SL reconciliation. The user views the list, selects one or several reports, views the details, and downloads, prints and saves them singly or as a batch (FR-AC-020 to FR-AC-023).
preconditions:
  - "The user has ACSL_REPORT_VIEW."
main_flow:
  - The user chooses the reports and the period.
  - The user views, downloads, prints or saves them.
rules:
  - [R1, "Report category ACSL; runs are archived.", Fixed, "-"]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Runs and exports are archived."
acceptance:
  - The booked-accounts list of September for premium receivable exports to XLSX with the file name of the BRD.
```

### Insurer SOA reconciliation

```fr
id: FR-AS-002
title: Upload insurer statements of account
brd: [ACSL 2.2.1 (p.115), ACSL 2.4.0 (p.117; Add.1 p.33-34)]
actor: ACSL Processor (ACSL_UPLOAD)
priority: Must have
screens: Insurer SOA Reconciliation (Upload SOA; uploads list; upload log)
description:
  - The processor uploads the SOA of an insurer for a covered period (CSV, XLSX, ODS or TXT, up to 10 MB and 20,000 rows). BIBS reads it with the insurer's layout, or the standard layout (Invoice No, Policy No, Assured, Inception Date, Expiry Date, Gross Premium, Balance, Payments), numbers the upload SOA-yyyy-n and reconciles it (FR-AS-003).
  - The upload log proves each row was loaded - rows read, loaded and failed, with the reason per failed row (report ACSL-SOA-UPLOAD-LOG). The same file cannot be uploaded twice for an insurer.
preconditions:
  - "The user has ACSL_UPLOAD; a layout exists for the insurer or the standard layout."
main_flow:
  - The processor chooses the insurer and the period and uploads the file.
  - BIBS checks the layout, loads the rows and writes the log.
  - BIBS reconciles the upload.
rules:
  - [R1, "Maximum rows per upload ACSL_SOA_MAX_ROWS = 20000.", Configurable, Parameter ACSL_SOA_MAX_ROWS]
  - [R2, "Column headers per insurer; standard layout '*' until AQ21.", Configurable, SOA layouts]
validations:
  - [Insurer or period missing, Give the insurer and the covered period of the statement, ACSL_SOA_PERIOD]
  - [Period reversed, The covered period ends before it starts, ACSL_SOA_PERIOD]
  - [File too large, The file must be at most 10 MB, ACSL_SOA_TOO_LARGE]
  - [Too many rows, "An SOA upload has at most <max> rows", ACSL_SOA_TOO_LARGE]
  - [Duplicate file, "This file was already uploaded for <insurer> as <upload>", ACSL_SOA_DUPLICATE]
  - [Headers missing, "The file does not follow the layout '<name>'; missing column(s) <list>", ACSL_SOA_LAYOUT_MISMATCH]
  - [No layout, No SOA layout is configured, ACSL_SOA_NO_LAYOUT]
fields_screen: Upload SOA
fields:
  - [Insurer, Look-up, "Yes", Insurers, "-"]
  - [Period From / To, Date, "Yes", "-", To >= From]
  - [File, File, "Yes", "-", "csv, xlsx, ods, txt; <= 10 MB"]
notifications:
  - "None."
audit:
  - "The upload keeps its file checksum (SHA-256), counts and user."
acceptance:
  - An SOA of 1,200 rows with 3 rows without invoice number loads 1,197 rows and the log lists the 3 with their reason.
  - Uploading the same file again is refused with ACSL_SOA_DUPLICATE.
```

```fr
id: FR-AS-003
title: Reconcile the SOA against the booked transactions by invoice number
brd: [ACSL 2.13.0 (p.120), ACSL 2.13.1 (p.121), ACSL 2.14.0 (p.121), ACSL 2.14.1 (p.122)]
actor: System; ACSL Processor
priority: Must have
screens: Insurer SOA Reconciliation (upload - Results, Reconcile Again, Report)
description: For each SOA line BIBS finds the invoice in the ledger and reports its status - Outstanding, For remittance, Remitted (batch and date), Cancelled (with the cancellation reference), Direct billed (indicator), with the 2307 amount, the SOA balance and the variances of premium and outstanding balance. Lines without invoice in BIBS are Not found. The reconciliation report is named "<insurer>_<from>_<to>" and exports to XLSX, ODS or PDF. The processor can reconcile again after the ledger changes.
preconditions:
  - "The upload is loaded."
main_flow:
  - BIBS reconciles the lines.
  - The processor reviews the results by bucket and downloads the report.
alternate_flows:
  - Reconcile again. The processor re-runs the reconciliation.
rules:
  - [R1, "Matching key is the invoice number.", Fixed, "-"]
  - [R2, "Buckets Outstanding, For remittance, Remitted, Cancelled, Direct billed; Not found.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Each run keeps its results and time."
acceptance:
  - An SOA line whose invoice was remitted in batch RB-2026-12 shows Remitted with that batch and date.
  - An SOA line with an unknown invoice is Not found.
```

```fr
id: FR-AS-004
title: Reconcile GL and SL balances by GL code
brd: [ACSL 2.13.2 (p.121)]
actor: System (job ACSL_GL_SL_RECON); ACSL users
priority: Must have
screens: GL-SL Reconciliation (runs, rows, control accounts)
description: For each control account set up, BIBS compares the GL balance with its sub-ledger - the party ledger of the Operations components, or the open items of given document types - and shows the difference. The job runs every day at 20:00 and users run it on demand; a difference raises ACSL_GLSL_DIFFERENCE.
preconditions:
  - "Control accounts are set up (account, source, components or document types, currency)."
main_flow:
  - The job or the user runs the reconciliation for a date.
  - BIBS lists per account the GL balance, the SL balance and the difference.
rules:
  - [R1, "Sources PARTY_LEDGER or OPEN_ITEMS.", Fixed, "-"]
  - [R2, "Schedule daily at 20:00 Manila.", Configurable, Job ACSL_GL_SL_RECON]
validations:
  - [Setting incomplete, "Give the sub-ledger, and the components (Operations ledger) or document types (open items)", ACSL_GLSL_SETTING]
fields_screen: Control account
fields:
  - [GL Account, Look-up, "Yes", Chart, Control account]
  - [Source, Option, "Yes", "Party ledger, Open items", "-"]
  - [Components / Document Types, Text, Conditional, "-", Per source]
  - [Currency, List, "No", Currencies, "-"]
notifications:
  - "Alert ACSL_GLSL_DIFFERENCE."
audit:
  - "Runs and rows are kept."
acceptance:
  - A premium receivable GL of 1,000,000.00 against an SL of 998,500.00 shows a difference of 1,500.00 and raises the alert.
```

> [!NOTE] Difference from the BRD
> ACSL 2.13.2 includes the period-end check. The reconciliation runs nightly and on demand; a check that stops the period close on a GL-SL difference is not proposed (chapter 12, CLR-AS-04).

```fr
id: FR-AS-005
title: Produce aging and schedule reports per account family
brd: [ACSL 2.14.3 (p.124), ACSL 2.14.4 (p.125)]
actor: ACSL users
priority: Must have
screens: Report Centre (category ACSL)
description: ACSL users run the aging report by posting date and the schedule for the covered period of each account family - AR insurer's refund, AP refund from insurer, commission receivable, payable to insurance company and premium receivable - in PHP and USD. Each report ages the open items per party in the agreed ageing slots and shows the GL balance and the SL-GL difference, which agrees with the GL-SL reconciliation (FR-AS-004). The ageing slots and layouts are confirmed by BDOI (OQ43).
preconditions:
  - "The user holds the ACSL report permission."
main_flow:
  - The user opens the Report Centre, category ACSL, chooses the account family and enters the period and currency.
  - BIBS lists the open items per party in the ageing slots, with the totals, the GL balance and the SL-GL difference.
  - The user exports the report to Excel or PDF.
rules:
  - [R1, "Ageing slots and layouts to be confirmed (OQ43); eight slots are proposed.", Configurable, Ageing slots]
validations:
  - [Mandatory parameter missing, "Parameter <name> is required", MISSING_PARAMETER]
notifications:
  - "None."
audit:
  - "Every run and export is archived."
acceptance:
  - The premium receivable aging report of September ages the open items per party, and its SL-GL difference equals that of the GL-SL reconciliation for the same date.
```

### Investigation and cases

```fr
id: FR-AS-010
title: Investigate accounts and receive account analysis requests
brd: [ACSL 2.5.0 (p.117), ACSL 2.5.5 (p.117; Add.1 p.34), ACSL 2.5.1 (p.117), ACSL 2.5.2 (p.118), ACSL 2.5.3 (p.118)]
actor: ACSL Processor; ACSL Team Leader (assign)
priority: Must have
screens: ACSL Cases (tabs by stage); ACSL Case; Invoice Search; Invoice 360 (Invoice Family)
description:
  - ACSL works in cases (ACS-yyyy-n) of type Investigation, Account analysis request, Correction entry, AR refund payment application or Sub-ledger payment reversal. Cases come from ACSL users, from other units, and from Payment Requests (validation of a refund of a cancelled policy, FR-PQ-006).
  - The TL assigns a case; the processor searches the transaction in Invoice Search by invoice number, policy number, assured name, inception date range or account officer, selects it and sees it with all related transactions of the invoice family (regular booking, endorsements, cancellations, adjustments) and their collections, remittances and cases. The processor records the findings.
preconditions:
  - "The user has ACSL_PROCESS (ACSL_ASSIGN to assign)."
main_flow:
  - A case is received (RECEIVED).
  - The TL assigns it (ASSIGNED); the processor starts the investigation (INVESTIGATING).
  - The processor searches and opens the transaction and records the findings.
alternate_flows:
  - Send back. The processor sends the case back for re-assignment with a reason.
rules:
  - [R1, "Case types of the list ACSL_CASE_TYPE.", Configurable, LOV ACSL_CASE_TYPE]
  - [R2, "Investigation SLA 72 hours.", Configurable, Workflow ACSL_CASE]
validations:
  - [Type or subject missing, Give the case type and subject, ACSL_CASE_INCOMPLETE]
  - [Invoice unknown, "Invoice <no> is not in the ledger", ACSL_INVOICE_UNKNOWN]
  - [Wrong stage, "<case> is <stage>, not <stage>", ACSL_WRONG_STAGE]
fields_screen: New Case
fields:
  - [Type, List, "Yes", LOV ACSL_CASE_TYPE, "-"]
  - [Invoice No. / AR No., Text, "No", Ledger, Exists]
  - [Subject, Text, "Yes", "-", Up to 250 characters]
  - [Details, Text, "No", "-", Up to 2000 characters]
notifications:
  - "The ACSL team is notified of new cases; the assignee of assignments."
audit:
  - "The case keeps its trail and findings."
acceptance:
  - Searching by assured name finds the invoice; the case shows its endorsement and cancellation.
```

```fr
id: FR-AS-011
title: Give the result of the investigation to the requester
brd: [ACSL 2.5.4 (p.118)]
actor: ACSL Processor
priority: Must have
screens: ACSL Case (Provide Result)
description: The processor gives the result - Confirmed, Rejected or No action - with remarks. The case is RESULT_PROVIDED and the requester (user or module) receives it; a Payment Requests validation moves its request (FR-PQ-006).
preconditions:
  - "The case is INVESTIGATING."
main_flow:
  - The processor clicks **Provide Result**, chooses the outcome and writes remarks.
rules:
  - [R1, "Outcomes CONFIRMED, REJECTED, NO_ACTION.", Fixed, "-"]
validations:
  - [Outcome missing, "Give the result: confirmed, rejected or no action", ACSL_OUTCOME_INVALID]
notifications:
  - "The requester is notified."
audit:
  - "The result is in the trail."
acceptance:
  - Confirming the analysis request of a cancelled-policy refund marks the ACSL validation of the RRF Confirmed.
```

```fr
id: FR-AS-012
title: Apply AR refunds and request sub-ledger payment reversals
brd: [ACSL 2.6.0 (p.118), ACSL 2.6.1 (p.118)]
actor: ACSL Processor (ACSL_APPLY); Cashier (approval)
priority: Must have
screens: ACSL Case (Request Payment Reversal); Cashiering (payment reversals)
description: From a case, the processor requests the reversal of a payment application in the sub-ledger - receipt number, amount and reason - to apply an AR refund or correct an application. Cashiering records it (PRV-yyyy-n), a second cashiering user approves it, the applications are reversed and the money goes back to unapplied (Dr receivable / Cr unapplied); the result returns to the case.
preconditions:
  - "The case has its invoice; the receipt is applied to it."
main_flow:
  - The processor enters the receipt, amount and reason and requests the reversal.
  - Cashiering approves it.
  - The case shows the result.
rules:
  - [R1, "The reversal is approved by a cashiering user who is not the requester.", Fixed, "-"]
validations:
  - [Invoice or receipt missing, "A payment reversal needs the case's invoice and the receipt", ACSL_REVERSAL_INCOMPLETE]
  - [Approver is the requester, The requester cannot approve the payment reversal, MAKER_CHECKER_VIOLATION]
fields_screen: Request Payment Reversal
fields:
  - [Receipt No., Text, "Yes", Receipts of the invoice, "-"]
  - [Amount, Amount, "No", "-", "> 0"]
  - [Reason, Text, "Yes", "-", Up to 250 characters]
notifications:
  - "Cashiering sees the request; the case is updated with the result."
audit:
  - "The request and its outcome are linked to the case."
acceptance:
  - A reversal of 3,000.00 on OR 555 approved by a second cashier puts 3,000.00 back to unapplied and updates the case.
```

```fr
id: FR-AS-013
title: Coordinate short or over payments with the Account Officer
brd: [ACSL 2.6.2 (p.119)]
actor: ACSL Processor
priority: Must have
screens: ACSL Case (Message the AO)
description: For a short or over payment, the processor sends a message from the case to the Account Officer of the invoice; the AO receives it as a notification and the message is kept on the case.
preconditions:
  - "The case's invoice has an Account Officer."
main_flow:
  - The processor writes the message and sends it.
rules:
  - [R1, "The AO is the account officer of the invoice.", Fixed, "-"]
validations:
  - [No AO or empty message, "The case's invoice has no Account Officer, or the message is empty", ACSL_NO_ACCOUNT_OFFICER]
fields_screen: Message the AO
fields:
  - [Message, Text, "Yes", "-", Up to 1000 characters]
notifications:
  - "The AO receives the message."
audit:
  - "The message is in the case trail."
acceptance:
  - The AO of invoice 2026-0456 receives the message about a short payment of 150.00.
```

### Correction entries

```fr
id: FR-AS-020
title: Assign and re-assign correction entries
brd: [ACSL 2.7.0 (p.119), ACSL 2.8.0 (p.119)]
actor: ACSL Team Leader (ACSL_ASSIGN)
priority: Must have
screens: Correction Entries (To assign); Correction
description: A correction (COR-yyyy-n) is raised from a case (raise correction) or directly. The TL assigns it to a preparer, and can re-assign it while it is a draft; it moves to Draft in the preparer's work.
preconditions:
  - "The correction is To assign or Draft."
main_flow:
  - The TL clicks **Assign**, chooses the preparer and writes a comment.
rules:
  - [R1, "Draft SLA 48 hours.", Configurable, Workflow ACSL_CORRECTION]
validations:
  - [Wrong stage, "<correction> is <stage>, not <stage>", ACSL_WRONG_STAGE]
fields_screen: Assign
fields:
  - [User, Look-up, "Yes", Users with ACSL_PROCESS, "-"]
  - [Comment, Text, "No", "-", Up to 500 characters]
notifications:
  - "The preparer is notified."
audit:
  - "Assignments are in the trail."
acceptance:
  - A correction re-assigned to acslproc2 appears in her Draft tab.
```

```fr
id: FR-AS-021
title: Prepare a correction entry and route it for review
brd: [ACSL 2.9.0 (p.119), ACSL 2.9.1 (Add.2 p.12-13)]
actor: ACSL Processor
priority: Must have
screens: Correction (Lines, Propose from journal, Submit)
description:
  - The preparer enters the kind (Posting to a wrong GL account, Wrong amount, Reclassification, Other), the invoice, the original journal and the description, then the lines - account, side, amount, party for control accounts, invoice and ledger component, cost centre, business line and narration (up to 200 lines).
  - For a wrong GL account, "Propose" reads the original journal line and prepares the reversal of that line and the re-post to the right account (and party or component), both linked to the original invoice and batch. The original and the correction stay visible in the invoice family. The preparer submits a balanced correction for review.
preconditions:
  - "The correction is Draft and assigned to the user."
main_flow:
  - The preparer enters the header and lines, or proposes them from the journal.
  - The preparer submits it.
alternate_flows:
  - Cancel. The preparer cancels the draft with a reason.
rules:
  - [R1, "Kinds of the list ACSL_CORRECTION_KIND.", Configurable, LOV ACSL_CORRECTION_KIND]
  - [R2, "At least two lines, balanced, up to 200 lines.", Fixed, "-"]
validations:
  - [Description missing, Describe the correction, ACSL_DESCRIPTION_REQUIRED]
  - [Line without account, Every line needs a GL account, ACSL_LINE_INVALID]
  - [Side or amount wrong, Every line needs a side and a positive amount with two decimals, ACSL_LINE_INVALID]
  - [Account a heading, "Account <code> is a heading", ACSL_ACCOUNT_NOT_POSTABLE]
  - [Party missing, "Control account <code> needs the sub-ledger party", ACSL_PARTY_REQUIRED]
  - [Component without invoice, "A ledger component (<list>) needs the invoice it corrects", ACSL_COMPONENT_INVALID]
  - [Unbalanced, "A correction needs at least two lines and equal debits (<dr>) and credits (<cr>)", ACSL_CORRECTION_UNBALANCED]
  - [Too many lines, A correction has at most 200 lines, ACSL_TOO_MANY_LINES]
  - [Journal line unknown, "Journal <batch> has no line <n>", ACSL_LINE_UNKNOWN]
fields_screen: Correction line
fields:
  - [Account, Look-up, "Yes", Chart, Postable]
  - [Side, List, "Yes", "Debit, Credit", "-"]
  - [Amount, Amount, "Yes", "-", "> 0, 2 decimals"]
  - [Party, Look-up, Conditional, Parties, Control accounts]
  - [Invoice / Component, Text / List, Conditional, Ledger, Component needs the invoice]
  - [Cost Centre / Business Line / Narration, Text, "No", "-", "-"]
notifications:
  - "The reviewers see the correction For review."
audit:
  - "Lines keep their origin (Reversal, Repost) and the original batch and line."
acceptance:
  - A premium posted to 4110 instead of 4120 is corrected by a proposal that reverses 4110 and re-posts to 4120, both linked to the invoice.
```

```fr
id: FR-AS-022
title: Review and endorse the correction
brd: [ACSL 2.10.0 (p.119)]
actor: ACSL Team Leader (ACSL_REVIEW)
priority: Must have
screens: Correction (Endorse, Return)
description: The reviewer checks the correction and endorses it for approval with a comment, or returns it to the preparer with a reason. The reviewer is not the preparer.
preconditions:
  - "The correction is For review."
main_flow:
  - The reviewer clicks **Endorse** with a comment.
rules:
  - [R1, "The reviewer is never the preparer.", Fixed, "-"]
validations:
  - [Reviewer is the preparer, A correction is reviewed by someone other than its preparer, ACSL_FOUR_EYES]
notifications:
  - "The approvers see it For approval."
audit:
  - "Comments are in the trail."
acceptance:
  - acsltl endorses the correction of acslproc; acslproc cannot endorse his own.
```

```fr
id: FR-AS-023
title: Approve, decline or return the correction with a comment
brd: [ACSL 2.11.0 (p.120), ACSL 2.11.1 (p.120), ACSL 2.11.2 (p.120), ACSL 2.12.0 (p.120), ACSL 2.12.1 (p.120), ACSL 2.12.2 (p.120)]
actor: ACSL Head (ACSL_APPROVE)
priority: Must have
screens: Correction (Approve, Return)
description: The Head approves the correction with a comment (it posts, FR-AS-024), or returns it to the preparer with a reason and comment; a declined correction is returned and then cancelled by the preparer. Comments are saved in the trail.
preconditions:
  - "The correction is For approval."
main_flow:
  - The Head writes a comment and clicks **Approve**.
alternate_flows:
  - Return. The Head returns it with a reason (RETURN_REASON) and comment.
rules:
  - [R1, "The approver is not the maker.", Fixed, "-"]
validations:
  - [Approver is the maker, The approver of a correction is not its maker, ACSL_FOUR_EYES]
  - [Return without reason, "Select a reason for 'return'", WORKFLOW_REASON_REQUIRED]
fields_screen: Approve / Return
fields:
  - [Comment, Text, "No", "-", Up to 500 characters]
  - [Reason, List, Conditional, LOV RETURN_REASON, Return]
notifications:
  - "The preparer is notified."
audit:
  - "Decisions and comments are in the trail."
acceptance:
  - A returned correction is Draft again with the Head's comment.
```

```fr
id: FR-AS-024
title: Post corrections automatically on approval
brd: [ACSL 2.15.0 (p.125)]
actor: System
priority: Must have
screens: Correction (Journal); Journal
description: On approval BIBS posts a system journal ACS:<no> in the journal type of the corrected batch, records and matches the open items of the lines with a party, and moves the invoice components of the Operations ledger. The journal number is shown on the correction.
preconditions:
  - "The correction is approved."
main_flow:
  - BIBS posts the journal and updates the sub-ledgers.
rules:
  - [R1, "Posting in the open period of the approval date.", Fixed, "-"]
validations:
  - [Account unknown, "Account <code> is not in the chart", ACSL_ACCOUNT_UNKNOWN]
  - [Party unknown, "Party <code> of a correction line is not maintained", ACSL_PARTY_UNKNOWN]
notifications:
  - "None."
audit:
  - "The journal is linked to the correction, case and invoice."
acceptance:
  - An approved correction shows its journal ACS:COR-2026-5 and the invoice balance changes.
```

```fr
id: FR-AS-025
title: Deduct from the remittance on insurer confirmation
brd: [ACSL 2.9.2 (Add.2 p.13)]
actor: ACSL Processor (ACSL_PROCESS); ACSL Team Leader (REMIT_DEDUCTION_CONFIRM); System (remittance approval)
priority: Must have
screens: Remittance Deductions (work list); Remittance Deduction (record, batches, insurer confirmation documents); Remittance batch (Settlement tab)
description:
  - When an insurer confirms, with supporting documents, that an amount may be deducted from BDOI's remittance, the processor records the deduction (RDN-yyyy-n) - insurer, currency, source (AR insurer's refund, Over-remittance to the insurer, Other amount confirmed by the insurer), source reference, invoice, amount, the insurer's confirmation reference and date, remarks - attaches the documents and submits it. Another user confirms it.
  - When the next remittance batch of that insurer and currency is approved, BIBS consumes the confirmed deductions oldest first, capped at the amount payable (net due less the incentives), posts each part as OPS_REMIT_DEDUCTION (reference RMB:<batch>:<deduction no>) and records the application. The payment request is for the amount due after deductions; when the deductions take everything the batch is settled without a payment request. A deduction stays Confirmed while batches consume it (remaining amount shown) and becomes Applied when every batch that used it has received the insurer OR; a cancelled DV gives the amount back (FR-DS-044).
preconditions:
  - "The insurer's confirmation reference, date and documents are available."
main_flow:
  - The processor records the deduction and submits it (DRAFT to FOR_CONFIRMATION).
  - The TL confirms it (CONFIRMED).
  - The next approved batch of the insurer consumes it; it is APPLIED once the insurer ORs of its batches are in.
alternate_flows:
  - Return. The TL returns it to the processor with a reason.
  - Cancel. A draft deduction is cancelled with a reason.
rules:
  - [R1, "Submission needs the insurer's confirmation reference and date.", Fixed, "-"]
  - [R2, "The confirmer is not the preparer.", Fixed, "-"]
  - [R3, "Consumption is capped at the amount payable of the batch; the remainder waits for the next batch.", Fixed, "-"]
  - [R4, "Sources AR_INSURER_REFUND, OVER_REMITTANCE, OTHER (to confirm, AQ23).", Configurable, LOV REMIT_DEDUCTION_SOURCE]
validations:
  - [Amount not positive, A deduction needs a positive amount, REMIT_DEDUCTION_AMOUNT]
  - [Changed after submission, "Deduction <no> can only change as a draft", REMIT_DEDUCTION_STAGE]
  - [No insurer confirmation, "Deduction <no> needs the insurer's confirmation reference and date before submission", REMIT_DEDUCTION_UNCONFIRMED]
  - [Confirmer is the preparer, "Deduction <no> must be confirmed by another user", REMIT_DEDUCTION_FOUR_EYES]
fields_screen: Remittance Deduction
fields:
  - [Insurer, Look-up, "Yes", Insurers, "-"]
  - [Currency, List, "Yes", Currencies, 3-letter code]
  - [Source, List, "Yes", LOV REMIT_DEDUCTION_SOURCE, "-"]
  - [Source Reference, Text, "Yes", "-", Up to 60 characters]
  - [Invoice No., Text, "No", Ledger, "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Confirmation Ref. / Date, Text / Date, "Yes at submission", "-", Up to 100 characters]
  - [Remarks, Text, "No", "-", Up to 1000 characters]
notifications:
  - "The confirmers see deductions For confirmation."
audit:
  - "The deduction keeps its documents, confirmation, applications per batch and journals."
acceptance:
  - A confirmed deduction of 10,000.00 reduces the next remittance of the insurer by 10,000.00 and posts OPS_REMIT_DEDUCTION.
  - A deduction of 50,000.00 against a batch payable of 30,000.00 consumes 30,000.00, settles the batch without a payment request and keeps 20,000.00 remaining.
  - acsl cannot confirm the deduction he recorded.
```

```fr
id: FR-AS-026
title: Track related transactions per invoice and insurer
brd: [ACSL 2.16.0 (p.126; Add.1 p.34)]
actor: ACSL users
priority: Must have
screens: Invoice 360 (Invoice Family); ACSL Case
description: As FR-DS-093, ACSL sees for an invoice and its insurer the whole family - original, endorsements, cancellations, adjustments, collections, remittances, corrections and cases - through the root invoice number.
preconditions:
  - "None."
main_flow:
  - The user opens the invoice family from a case or Invoice 360.
rules:
  - [R1, "Endorsements and cancellations keep their own BIR invoice numbers (AQ29).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "-"
acceptance:
  - The family of an invoice cancelled and rebooked shows both with their cases.
```

# Workflows, statuses and accounting events


Solid arrows in the figures are the main path, dashed arrows are returns, dotted arrows are system actions.

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

Every posting of BRD-5 is a business event with components and account roles; the accounts come from the rules maintained by Comptrollership. The entries below are the seed rules; the real ones are BDOI's (AQ02, OQ07).

<!-- table: widths=0.8,4.2,4.4,5.4,1.8 caption="Accounting events and seed entries" size=8 -->
| # | Transaction | Event (source reference) | Seed entry | Note |
|---|---|---|---|---|
| 1 | DV approved - remittance to insurer | DISB_VOUCHER REMITTANCE (DV:<no>) | Dr 2211 due to insurer for disbursement / Cr paying bank, or 2241 checks outstanding for checks | - |
| 2 | DV approved - refund to client | DISB_VOUCHER REFUND | Dr 2216 refund payable / Cr paying account | - |
| 3 | DV approved - refund received from insurer | DISB_VOUCHER REFUND_FROM_INSURER | Dr 2217 AP refund from insurer / Cr paying account | - |
| 4 | DV approved - supplier, government, other bank unit | DISB_VOUCHER SUPPLIER / GOVERNMENT / OTHER_BANK_UNIT | Dr payable of the request / Cr paying account; Cr EWT payable when withheld | - |
| 5 | DV approved - employee, cash advance | DISB_VOUCHER EMPLOYEE / CASH_ADVANCE | Dr 1604 advances to employees or the expense (cost centre) / Cr paying account | - |
| 6 | DV approved - service fee, pass-on | DISB_VOUCHER SERVICE_FEE / PASS_ON | Dr 2250 / 2230 / Cr paying account | - |
| 7 | Proforma edited | DISB_VOUCHER (edited lines) | The edited lines, validated and balanced | - |
| 8 | Check negotiated | DISB_CHECK_NEGOTIATED (CHK:<id>:NEG) | Dr 2241 checks outstanding / Cr bank | - |
| 9 | Check staled | DISB_CHECK_STALE (CHK:<id>:STALE) | Dr 2241 / Cr 2240 Miscellaneous Liability - stale checks | - |
| 10 | Re-issue of a stale check | DISB_VOUCHER STALE_REISSUE | Dr 2240 / Cr paying account | - |
| 11 | Approved DV cancelled | DISB_VOUCHER negative (DV:<no>:CANCEL) | Reversal of rows 1-6 | - |
| 12 | Funding of the main account | DISB_FUND_TRANSFER (FND:<no>) | Dr target bank / Cr source bank | - |
| 13 | Insurer certificate received | TAX_CWT_CERT_RECEIVED (CRT:<id>) | Dr 1612 AR-BIR on hand / Cr 1610 AR-BIR on commission or 1611 on incentives | - |
| 14 | CPC2 incentive per remittance batch | OPS_REMIT_CPC2 (RMB:<batch>:CPC2) | Dr 2211 / Cr 4131 CPC2 incentive income, Cr 2504 output VAT | Base to be confirmed (AQ24) |
| 15 | Early incentive with service invoice | OPS_REMIT_INCENTIVE + service invoice EARLY_INCENTIVE | Dr 2211 / Cr 4130 incentive income (Other Income) + output VAT | The 2% WTAX entry (Dr 1611 / Cr 2211) to be confirmed (AQ25) |
| 16 | Remittance deduction on insurer confirmation | OPS_REMIT_DEDUCTION (RMB:<batch>:<deduction no>) | Dr 2211 / Cr 1225 AR insurer's refund | - |
| 17 | ACSL correction | System journal ADJUSTMENT (ACS:<no>) | Reversal of the wrong line and re-post to the right account | - |
| 18 | Service-fee accrual | FRBS_SERVICE_FEE_ACCRUE (<run>:<line>) | Dr 5614 service fee expense (cost centre) / Cr 2250 service fee payable | - |
| 19 | Cash-advance liquidation | PRQ_CA_LIQUIDATION (LIQ:<no>) | Dr expenses by category / Cr 1604; excess or shortage | - |
| 20 | Accrual auto-reversal | System journal REVERSAL (JV:<no>:AUTOREV) | Mirror of the accrual | - |

## Disbursement voucher (DISB_VOUCHER)

![Workflow DISB_VOUCHER (DIS 2.6.x-2.21.0)](figures/brd05_v2_dv_states.dot){width=12}

<!-- table: widths=3.2,3.8,3.6,6 caption="Stages of a DV" status=Stage size=8.5 -->
| Stage | Owner (permission) | SLA | Next |
|---|---|---|---|
| IN_PROCESS | Processor (DISB_PROCESS) | 24 h | Submit (FOR_REVIEW); route to the approver (refund, remittance); cancel (DISB_CANCEL_REASON) |
| FOR_REVIEW | Team Leader (DISB_REVIEW) | 24 h | Submit for approval; return (DISB_RETURN_REASON); cancel |
| FOR_APPROVAL | Approver (DISB_APPROVE) | 24 h | Approve (posting, instrument); return; reject (DISB_RETURN_REASON) |
| APPROVED | Processor (instrument) | - | Cancel approved DV (reversal DV:<no>:CANCEL) |
| REJECTED, CANCELLED | - | - | Final; the request goes back to its source |

A request without a maintained payee waits as NO_PAYEE (FR-DS-020). Refund and remittance DVs are routed straight to FOR_APPROVAL (DISB_AUTO_APPROVER_ROUTING). The posting status of a DV is NOT_POSTED, POSTED, FAILED, REVERSED or REVERSAL_FAILED.

## Payment instruments

![Instrument statuses per mode of payment (DIS 2.7.0, 2.8.x, 3.26.x)](figures/brd05_v2_instruments.dot){width=16}

<!-- table: widths=3.4,6.4,6.8 caption="Instrument statuses and what sets them" size=8.5 -->
| Mode | Statuses | Set by |
|---|---|---|
| Check | PENDING, PRINTED, RELEASED, NEGOTIATED; STALE | Print; release tag; deposited-checks upload; job DISB_CHECK_STALE (180 days) |
| Authority to Debit | PENDING, PRINTED, EMAILED, DEBITED | Print; e-mail to the branch; branch confirmation |
| Credit to account | PENDING, EXTRACTED, CREDITED | End of day (DCTF); credited-accounts upload |
| Manager's check / demand draft | PENDING, PRINTED, RECEIVED, RELEASED | Print; received from the branch; release to the payee |
| Credit ticket / TT | PENDING, PRINTED, DEBITED | Print; branch validation |
| Online banking | APPROVED, DEBITED | DV approval; BOB approval upload |

Every status except a final one (negotiated, credited, debited, released MC / DD) can become CANCELLED when the DV is cancelled. A wrong status is corrected through a status edit (workflow DISB_STATUS_EDIT - REQUESTED, APPLIED, REJECTED; FR-DS-051).

## End of day

The end of day of a business date (EOD-yyyy-n) produces the outputs DCTF, CHECKS, ATD, MC_DD, CREDIT_TICKET, TT, VOUCHERS and REPORT. After the run the confirmations are sent once (FR-DS-063). A date is run once.

## Account funding (DISB_FUNDING)

![Workflow DISB_FUNDING (DIS 2.17.x)](figures/brd05_v2_funding.dot){width=14}

The SLA of each stage is 8 hours. Returns and declines need a reason (RETURN_REASON); a cancellation needs VOID_REASON.

## Payee (DISB_PAYEE)

![Workflow DISB_PAYEE (DIS 2.2.x)](figures/brd05_v2_payee.dot){width=11}

## Payment Requests

![Workflow PRQ_REFUND (MKT 1.8.0-1.20.0)](figures/brd05_v2_prq_refund.dot){width=15}

A DRAFT refund can also be submitted, or sent for validation, without assignment. A refund returned by Disbursement goes back to PREPARING.

![Workflows PRQ_CASH_ADVANCE and PRQ_CHECK_CANCEL (MKT 1.16.2, 1.16.3, 1.19.0, 2.24.0)](figures/brd05_v2_prq_cash_advance.dot){width=14}

<!-- table: widths=3.8,3.2,9.6 caption="Payment Request stages" status=Stage size=8.5 -->
| Stage | Owner (permission) | Meaning |
|---|---|---|
| DRAFT | Requester (PRQ_CREATE) | Being prepared; editable |
| PREPARING | Assigned preparer (PRQ_CREATE) | Assigned, or returned by the reviewer or Disbursement (refunds) |
| FOR_VALIDATION | ACSL and Cashiering (48 h) | Refund of a cancelled policy being validated |
| FOR_REVIEW | Reviewer (PRQ_REVIEW) | Waiting for endorsement |
| FOR_APPROVAL | Approver (PRQ_APPROVE) | Waiting for Marketing approval |
| HR_APPROVAL | HR (PRQ_HR_APPROVE) | Cash advance waiting for HR |
| SENT_TO_DISBURSEMENT | - | Payment request sent (DSR number) |
| DISBURSED | - | Paid; cash advance ready for liquidation |
| REQUESTED, SENT | Requester; - | Check cancellation prepared; sent to Disbursement |
| CANCELLED | - | Cancelled or withdrawn before approval |

A liquidation is DRAFT, SUBMITTED or POSTED (FR-PQ-017). A validation is OPEN, DEFERRED (handed over), CONFIRMED or REJECTED.

## ACSL cases and corrections

![Workflow ACSL_CASE (ACSL 2.5.x-2.6.x)](figures/brd05_v2_acsl_case.dot){width=11}

![Workflow ACSL_CORRECTION (ACSL 2.7.0-2.15.0)](figures/brd05_v2_acsl_correction.dot){width=12}

The investigation SLA is 72 hours; a correction draft 48 hours; review and approval 24 hours each.

## Remittance deduction (REM_DEDUCTION)

![Workflow REM_DEDUCTION (ACSL 2.9.2)](figures/brd05_v2_deduction.dot){width=12}

The system action *apply* runs when every batch that used the deduction has received the insurer OR. A cancelled remittance DV gives the consumed amount back to the deduction.

## Accounting events of Disbursement, Payment Requests and ACSL

The events and seed entries are listed in, section 5.5 (rows 1-19). The events of this volume are DISB_VOUCHER (by disbursement type), DISB_CHECK_NEGOTIATED, DISB_CHECK_STALE, DISB_FUND_TRANSFER, TAX_CWT_CERT_RECEIVED, OPS_REMIT_CPC2, OPS_REMIT_INCENTIVE, OPS_REMIT_DEDUCTION, the ACSL correction journals (ACS:<no>) and PRQ_CA_LIQUIDATION. The real entries are BDOI data (AQ02).

# Reports and documents



## Accounting reports

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
| ORG-HEADCOUNT-CC | Headcount per Cost Centre | Employees per cost centre (FR-DS-037) | DIS 3.30.2 |

All reports need their view permission, export to PDF, XLSX, ODS and CSV, are archived, and run in report batches.

<!-- pagebreak -->

## Report pack (Appendix A)

The table is the catalogue of the Report Pack screen (58 entries). "Word" marks the reports BDOI asked for in Word, issued in PDF and Excel (chapter 12, CLR-AC-10). Layouts marked TO_CONFIRM wait for AQ05.

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

The Accounting functions produce no customer documents; the service-fee payout advice, the disbursement vouchers and the bank forms are produced by Disbursement.

<!-- table: widths=4.4,6.6 caption="Documents produced" size=8.5 -->
| Template | Document |
|---|---|
| DSB_VOUCHER | Disbursement voucher |
| DSB_CHECK | Check |
| DSB_ATD, DSB_ATD_EMAIL | Authority to Debit and its e-mail to the branch |
| DSB_MC_DD, DSB_CREDIT_TICKET, DSB_TT | Bank forms |
| DSB_PAYMENT_ADVICE | Payment advice e-mailed to the payee |
| DCTF | Direct Credit Transaction File (text) |
| PRQ_RRF, PRQ_RFP, PRQ_LIQUIDATION | Refund Request Form, Request for Payment, cash-advance liquidation (Appendix D) |
| BIR Form 2307 | Certificate of creditable tax withheld |
| EARLY_INCENTIVE service invoice | Service invoice of the early incentive (booking) |

## Disbursement, Payment Request and ACSL reports

<!-- table: widths=4.4,5,5.4 caption="Reports" size=8.5 -->
| Code | Name | BRD |
|---|---|---|
| DSB-MASTERLIST | Masterlist of Disbursements | DIS 2.3.1, 3.28.3 |
| DSB-UNRELEASED-CHECKS | Unreleased Checks (aged to 180 days) | DIS 2.3.2, 3.28.3 |
| DSB-CWT-COMMISSION | CWT / BIR 2307 on Commission | DIS 2.3.3, 3.28.3 |
| DSB-ATD | Authority to Debit | DIS 2.3.4, 3.28.3 |
| DSB-ML-STALE | Miscellaneous Liability - Stale Checks | DIS 2.3.5, 3.28.3 |
| DSB-CASH-FLOW | Disbursement Cash Flow | DIS 2.3.6, 3.28.3 |
| DSB-PAYEE | Payee Report | DIS 2.3.7, 3.28.1 |
| DSB-UPLOAD-FALLOUT | Request Upload Fall-out | DIS 2.3.8, 3.28.4 |
| DSB-PAYEE-NOMATCH | Payees Not Matched | DIS 3.25.2 |
| DSB-UNREGULARIZED | Unregularised Transactions | DIS 2.3.9, 3.27.0 |
| DSB-EOD-REMIT, -REFUND, -SUPPLIER, -EMPLOYEE, -OTHER, -SUMMARY | End-of-day reports | DIS 3.28.0, 3.28.2 |
| DSB-CPC2-INCENTIVE | CPC2 incentive report | DIS 3.29.0 |
| PRQ-STATUS | Request status | MKT 1.18.0 |
| PRQ-REGISTER | Request register | MKT 1.18.1 |
| ACSL-BOOKED-FIN-DETAILS | List of all booked accounts with financial details | ACSL 2.2.0, 2.14.2 |
| ACSL-SOA-RECON | SOA reconciliation | ACSL 2.14.1 |
| ACSL-SOA-UPLOAD-LOG | SOA upload log | ACSL 2.4.0 |
| ACSL-GL-SL-RECON | GL-SL reconciliation | ACSL 2.13.2 |
| ACSL aging and schedule reports | Five account families, PHP and USD | ACSL 2.14.3, 2.14.4 |

Reports run in the Report Centre with the options (view, export to XLSX, ODS or PDF, print, archive, batches).

# Interfaces and integration

Figure 6 shows the interfaces of Accounting. The general ledger is inside BIBS: every module posts through the accounting engine, so there is no accounting export (OQ01).

![Interfaces of Accounting (dashed = external or on hold)](figures/brd05_v1_integration.dot){width=11.5}

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Scope size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| Business modules (booking, Operations, Collections, Disbursement, Payment Requests, ACSL, FRBS) | In | Business events posted by the accounting engine | FRBS 3.1.0 | IN SCOPE |
| Disbursement | Out / In | Service-fee payouts (SERVICE_FEE) and their status (PAID, RETURNED, CANCELLED) | FRBS 2.10.x | IN SCOPE |
| Bank files | In | Cash-in-bank statements (XLSX, ODS, CSV) per layout | FRBS 3.3.1 | IN SCOPE |
| Report archive and batches | Out | Report files, ZIP and merged PDF | FRBS 2.4.x | IN SCOPE |
| BIR (eFPS, eBIRForms, CAS) | Out | Worksheets and loose-leaf books; no electronic filing | FRBS 3.2.0 | ON HOLD |
| BDO Unibank (GARD) | Out | Reports exported in PDF and Excel; submission channel not defined | FRBS 3.2.0 | ON HOLD |
| Payroll | In | HDMF, SSS, PhilHealth, 1601-C, 1604-C | Appendix A VII | OUT |
| Operations disbursement gateway | In / Out | Payment requests of remittance, cashiering, commission, FRBS and Payment Requests; DV status back | DIS 2.6.0, 3.25.0; MKT 1.20.0 | IN SCOPE |
| Refund validation | Out / In | ACSL case and Cashiering task per cancelled-policy line; results back | MKT 1.11.0; ACSL 2.5.5 | IN SCOPE |
| Payment reversal | Out / In | ACSL request to Cashiering (PRV-); result back | ACSL 2.6.1 | IN SCOPE |
| DV cancellation hand-off | Out | Approved check cancellation to the Disbursement approvers (DV_CANCELLATION); part on hold until BDOI answers AQ15 | MKT 1.19.0 | IN SCOPE |
| General ledger | Out | DV, check, funding, correction, liquidation, CPC2 and deduction postings | DIS 2.19.0; ACSL 2.15.0 | IN SCOPE |
| TPD (ACA) | Out | DCTF text file downloaded and sent by the user | DIS 2.16.1 | IN SCOPE |
| Bank branches | Out / In | ATD e-mail and bank forms; confirmations recorded by the user | DIS 2.7.7-2.7.9 | IN SCOPE |
| Bank files | In | Deposited checks, credited accounts, BOB approvals (CSV uploads); part on hold until BDOI answers AQ09 | DIS 2.22.0, 3.26.x | IN SCOPE |
| BDO Business Online Banking | Out | Funding and online payments done in BOB; reference recorded | DIS 2.17.1 | OUT |
| Insurer SOA | In | SOA file upload per insurer and period; part on hold until BDOI answers AQ21 | ACSL 2.2.1, 2.4.0 | IN SCOPE |
| E-mail | Out | Payment advice, ATD, notifications | DIS 2.7.12 | IN SCOPE |


# Non-functional requirements

<!-- table: widths=3,5.6,5.4 caption="Non-functional requirements (BRD p.133-139; Add.1 p.37)" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Users | Accounting 5 (GL officer, TL, TH); Business / System Admin 7 | Within the BRD-1 sizing (145 concurrent) |
| Volumes | Manual adjustments about 100 a year; 6 GL closings | Small; no special tuning |
| Response time | Screen load 5-10 s, refresh 5 s, field display 2 s, save 5 s; reports 10-20 s first load, 10 s next | Online p95 under 3 s; heavy reports in batches |
| Peaks | Month end and year end; 10:00-15:00 | Close and reversal jobs outside the peak |
| Devices | Same performance on mobile and desktop | Responsive screens |
| Availability (Add.1) | 100%; use 07:00-18:00 Monday to Saturday; downtime under 24 hours; maintenance 19:00-07:00 | Same deployment as BRD-1; 100% is not a measurable SLA; one BIBS-wide NFR set is being agreed (AQ27, XQ08) |
| Retention (Add.1) | Reports and vouchers 5 years online, 5 years archive; daily backup kept 5 years | Retention rules of BRD-1 with a document class for vouchers and generated reports |
| Security and audit | Authorised users only; maker-checker | Role-based access, four-eyes rules and audit on every change (section 3) |
| Users | Disbursement 8; ACSL 6; Marketing 394 named, 40 concurrent | Within the BRD-1 sizing (145 concurrent) |
| Volumes | Remittance DVs 198 a year (+20%), refunds 246, supplier payments 293, government 9, other bank units 31, employee-related 56, CWT tagging 50; corrections 400; Marketing RFP / refund 302 | Small; no special tuning |
| Response time | Screen load 5-10 s, refresh 5 s, field display 2 s, save 5 s (2 s for some Disbursement and ACSL saves); reports 10-20 s first load; upload or download 3 s per file | Online p95 under 3 s; end of day, reports and SOA reconciliation run as jobs or batches |
| Peaks | Month end and year end; 08:00-12:00 Disbursement; 10:00-15:00 ACSL and Marketing | End of day after the peak; GL-SL reconciliation at 20:00; stale-check job at 00:20 |
| Devices | Same performance on mobile and desktop | Responsive screens |
| Availability (Add.1) | 100%; 07:00-18:00 Monday to Saturday; downtime under 24 hours; maintenance 19:00-07:00; BCP under 3 days | Same deployment as BRD-1; 100% is not a measurable SLA; one BIBS-wide NFR set is being agreed (AQ27) |
| Retention (Add.1) | Reports and vouchers 5 years online, 5 years archive; daily backup kept 5 years | Retention rules of BRD-1 with a document class for vouchers and generated reports |
| Security and audit | Authorised users; maker-checker | Role-based access; four-eyes rules of section 3.3; masked account numbers; audit of every change |


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
| DISB_STALE_DAYS | 180 | Days after printing when an unnegotiated check is stale (30-720) |
| DISB_CHECK_CLEARING | ON | Checks credit checks outstanding until negotiated |
| DISB_CHECK_CLEARING_ACCOUNT | 2241 | Checks outstanding account |
| DISB_AUTO_APPROVER_ROUTING | REFUND,REMITTANCE | Disbursement types whose DV goes straight to the approver |
| DISB_NO_PAYEE_ACTION | HOLD | HOLD keeps a request without payee waiting; RETURN returns it to its source |
| DISB_CHECK_SERIES_WARNING | 20 | Remaining checks that raise CHECK_SERIES_LOW |
| EARLY_INCENTIVE_WTAX_RATE | 2 | Withholding tax rate (%) of the early-incentive service invoice |
| ACSL_SOA_MAX_ROWS | 20000 | Maximum rows of an SOA upload |


## Jobs

<!-- table: widths=5,4.2,7.4 caption="Accounting jobs (Manila time)" size=8.5 -->
| Job | Schedule | Purpose |
|---|---|---|
| JOURNAL_AUTO_REVERSAL | Daily 00:05 | Reverse accruals on their reverse-on date |
| GL_PERIOD_CLOSE | Every 15 minutes | Run the scheduled GL closes that are due |
| BROKING_BOOKS_CLOSE | Last day of the month 23:00 | Close the broking books |
| BOOK_RATE_FROM_CLOSING | First day of the month 00:30 | Copy the revaluation rates of the month that ended as the new month's BOOK rates |
| DISB_CHECK_STALE | Daily 00:20 | Stale the checks older than DISB_STALE_DAYS and post the entry |
| DISB_EOD_CONFIRMATION | After each end-of-day run (no fixed schedule) | Send the payment advices |
| DISB_EOD_REPORTS | After each end-of-day run (no fixed schedule) | Generate the end-of-day reports |
| ACSL_GL_SL_RECON | Daily 20:00 | GL-SL reconciliation of the control accounts |


## Lists of values

<!-- table: widths=5,11.6 caption="Lists of values" size=8.5 -->
| List | Values provided |
|---|---|
| SERVICE_FEE_SEGMENT | CBG; IBG (Institutional Banking Group); Others (to confirm, AQ20) |
| RETURN_REASON, VOID_REASON | Shared platform lists (journal return, run cancellation) |
| Lists of Payment Requests, Disbursement and ACSL | PAYEE_CLASS, DISBURSEMENT_TYPE, DISB_CANCEL_REASON, DISB_RETURN_REASON, BRANCH_EMAIL, REFUND_REASON, RRF_CATEGORY_A / B, PRQ_PAYMENT_MODE, PRQ_RFP_TYPE, ACSL_CASE_TYPE, ACSL_CORRECTION_KIND, REMIT_DEDUCTION_SOURCE |
| PAYEE_CLASS | Supplier, Insurer, Employee, Client, Government agency, Others |
| DISBURSEMENT_TYPE | Remittance; Refund; Payment to supplier; Payment to government agencies; Payment to other bank units; Employee-related request; Cash advance; Service fee; Incentive pass-on; BIR 2307 release; Other disbursement requests; Re-issue of a stale check |
| DISB_CANCEL_REASON | Requested by the requesting unit; Wrong payee, account or amount; Duplicate request or DV; Check spoiled or lost; Others (AQ15) |
| DISB_RETURN_REASON | Payee not maintained; Incomplete supporting documents; Incorrect accounting entry; Others |
| BRANCH_EMAIL | Branch mailboxes for the ATD (AQ09) |
| REFUND_REASON | Cancelled policy; Overpayment; Double payment; Premium decrease (endorsement); Others |
| RRF_CATEGORY_A, RRF_CATEGORY_B | Others (to confirm, AQ18) |
| PRQ_PAYMENT_MODE | CTA, CHECK, ATD, INTER_OFFICE, MANAGERS_CHECK, DEMAND_DRAFT |
| PRQ_RFP_TYPE | Cash advance, Petty cash, Others |
| ACSL_CASE_TYPE | Investigation; Account analysis request; Correction entry; AR refund payment application; Sub-ledger payment reversal |
| ACSL_CORRECTION_KIND | Posting to a wrong GL account; Wrong amount; Reclassification; Other correction |
| REMIT_DEDUCTION_SOURCE | AR insurer's refund; Over-remittance to the insurer; Other amount confirmed by the insurer (AQ23) |
| RETURN_REASON, VOID_REASON | Shared platform lists |


## Alerts

<!-- table: widths=5,2.4,9.2 caption="Accounting alerts" size=8.5 -->
| Code | Severity | When |
|---|---|---|
| COST_CENTER_MISSING | Medium | A posting to a cost-centre-required account found no rule |
| YEAR_END_CLOSE_DUE | High | The previous year is open 15 days before the deadline |
| GL_CLOSE_FAILED | High | A scheduled GL close or the broking-books close did not complete |
| DISB_PAYEE_NO_MATCH | Medium | A system request names a payee that is not maintained |
| DISB_UNREGULARIZED | High | A cancelled approved DV is not regularised after 2 days |
| DISB_CHECK_STALE | Low | A check reached DISB_STALE_DAYS |
| CHECK_SERIES_LOW | Medium | A cheque book reached its warning threshold |
| ACSL_GLSL_DIFFERENCE | High | The GL-SL reconciliation found a difference of 1.00 or more |


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
| Payees and their accounts | Disbursement TL (Approver) | FR-DS-010-014 |
| BDOIR bank accounts, cheque books | Disbursement Approver (another user) | FR-DS-070, 071 |
| Employees and cost centres | Comptrollership administrator | FR-DS-037 |
| Accounting rules of the disbursement types | Comptrollership (maker-checker) |, FR-AC-030 |
| Document templates (voucher, check, forms, RRF, RFP) | System Administrator | FR-DS-062, FR-PQ-003 |
| Liquidation accounts | Comptrollership administrator | FR-PQ-017 |
| SOA layouts per insurer | System Administrator (AQ21) | FR-AS-002 |
| GL-SL control accounts | ACSL TL | FR-AS-004 |
| CPC2 incentive criteria | TSU (BRD-3 Product Maintenance) | FR-DS-090 |
| Lists of values | Business Administrator (approver) |, FR-AC-070 |


# Assumptions, dependencies and open questions



## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-AC-01 | The general ledger of BDOI is kept in BIBS; there is no accounting interface to another system | OQ01 |
| A-AC-02 | BDOI's chart and accounting rules are loaded as configuration at go-live; the seed chart is a placeholder | AQ01, AQ02 |
| A-AC-03 | The monthly revaluation rate is the month-end CLOSING rate and becomes the next month's BOOK rate | AQ03, OQ08 |
| A-AC-04 | "Broking books" are the postings of the broking modules; "GL books" the whole ledger closed by FRBS | AQ04 |
| A-AC-05 | The service fee is 2.5% (CBG) or 1% (IBG) of fully paid commission net of the insurer's withholding tax | AQ20 |
| A-AC-06 | Payroll outputs are prepared outside BIBS | AQ06 |
| A-DS-01 | Bank channels stay manual or file-based; BIBS has no system-to-system link to the banks | AQ09 |
| A-DS-02 | Posting happens at DV approval, not at release | AQ13 |
| A-DS-03 | A payee used on a DV is never deleted; it is deactivated | AQ11 |
| A-DS-04 | Endorsements and cancellations keep their own BIR invoice numbers linked to a root invoice | AQ29 |
| A-PQ-01 | All accounts of one refund request belong to one client | AQ18 |
| A-PQ-02 | The cash-advance liquidation of Appendix D is in scope | AQ18 |
| A-AS-01 | Insurer SOAs carry BDOI's invoice number as matching key | AQ21 |
| A-AS-02 | CPC2 is computed on the basic premium remitted and deducted from the remittance | AQ24 |


## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-AC-01 | BDOI provides its chart, numbering convention and accounting rules | FR-AC-011-013, 030 (AQ01, AQ02) |
| D-AC-02 | BDOI provides the GARD, subsidiaries and Mancom layouts and segment budgets | FR-AC-060-062 (AQ05) |
| D-AC-03 | BDOI provides the bank file layouts | FR-AC-050 (AQ08) |
| D-AC-04 | BDOI confirms the service-fee rates and recipients | FR-AC-052 (AQ20) |
| D-AC-05 | BDOI gives the cost-centre rules | FR-AC-054 (AQ26) |
| D-AC-06 | BDOI confirms PDF and Excel for the reports asked in Word | FR-AC-060 (CLR-AC-10) |
| D-AC-07 | The broking modules report their pending items to the cut-off | FR-AC-042 |
| D-AC-08 | BDOI confirms Return, Resubmit and bulk approval on the administration screens | FR-AC-072 (CLR-AC-14) |
| D-DS-01 | BDOI gives the DCTF trailer, the credited-accounts, deposited-checks and BOB report layouts | FR-DS-052, 053, 061 (AQ09) |
| D-DS-02 | BDOI gives the check, voucher and bank-form layouts and signatories | FR-DS-035, 062 (AQ14) |
| D-DS-03 | BDOI gives the payee migration file | FR-DS-014 (AQ11) |
| D-DS-04 | BDOI gives the real accounting entries of each disbursement type | FR-DS-041, 055 (AQ02) |
| D-DS-05 | BDOI confirms the CPC2 base, VAT and criteria, and the accounting of the insurer's 2% on early incentives | FR-DS-090, 091 (AQ24, AQ25) |
| D-DS-06 | BDOI confirms the CPC2 report layout | FR-DS-092 (AQ24) |
| D-PQ-01 | BDOI gives the mandatory RRF / RFP fields, approval chains and the HR approver | FR-PQ-003, 004, 009 (AQ18) |
| D-AS-01 | BDOI gives the insurer SOA layouts | FR-AS-002 (AQ21) |
| D-AS-02 | BDOI confirms the ageing slots of the ACSL aging and schedule reports | FR-AS-005 (OQ43) |


## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions" status=Status size=8.5 -->
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
| AQ09 | Bank channel per mode; full DCTF specification; credited-accounts, deposited-checks and BOB report layouts | FR-DS-052, 053, 061 | OPEN |
| AQ10 | Main and source accounts of the funding; approver order; limits | FR-DS-064 | OPEN |
| AQ11 | Payee migration file; classes; several accounts; delete or deactivate | FR-DS-010-014 | OPEN |
| AQ12 | System request sources; RFP number format; upload columns | FR-DS-020, 024 | OPEN |
| AQ13 | DV number format; editable proforma lines; posting at approval or release; EWT at DV or invoice | FR-DS-030, 031, 041 | OPEN |
| AQ14 | Check, voucher and form layouts; signatories; stale-check accounting and re-issue | FR-DS-035, 054, 055, 062 | OPEN |
| AQ15 | Approver of status edits; cancellation after release; regularisation per source | FR-DS-044, 051; FR-PQ-011 | OPEN |
| AQ16 | CWT received and released; relation to the client 2307 flow; period covered | FR-DS-057 | OPEN |
| AQ17 | Which DVs need an OR / AR back | FR-DS-045, 056 | OPEN |
| AQ18 | RRF / RFP mandatory fields; approval chains; HR approver; liquidation scope; unapplied payment report | FR-PQ-002-004, 009, 017 | OPEN |
| AQ19 | CA / SA definition and validation | FR-PQ-016 | OPEN |
| AQ21 | Insurer SOA layouts; matching key; direct-billed indicator | FR-AS-002, 003 | OPEN |
| AQ22 | Accounts ACSL may correct; effect on sub-ledgers; approvers | FR-AS-021 | OPEN |
| AQ23 | What is deducted from the remittance; spanning batches; entries | FR-AS-025 | OPEN |
| AQ24 | CPC2 percentage, base, VAT and WTAX (with OQ39, PQ04) | FR-DS-090, 092 | OPEN |
| AQ25 | Early-incentive SI series, recipient and timing; accounting of the 2% | FR-DS-091 | OPEN |
| AQ29 | Invoice number of endorsements and cancellations | FR-DS-093, FR-AS-026 | PARTIAL |
AQ05 and AQ28 are partly answered by the Report List of BRD-12 and the role sections of BRD-6 to BRD-12 (R6).

AQ29 is applied as proposed (new numbers linked to a root invoice) until BDOI decides otherwise.

<!-- pagebreak -->

# Traceability

Every BRD-5 requirement is met by at least one FR, except the login to BDO Business Online Banking (DIS 2.17.1), which is outside BIBS. The test cases are listed by test condition (TC-AC-nnn.n, TC-DS-nnn.n, TC-PQ-nnn.n, TC-AS-nnn.n); the test plan workbook lists each case.


## FRBS (Accounting)

<!-- table: widths=2.2,2.2,2.6,4.6,5.2 caption="FRBS requirement IDs to FR, screen and test cases" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| FRBS 1.1.0 | p.50 | FR-AC-001 | Login; menu | TC-AC-001.1, 001.2 (4 cases) |
| FRBS 1.1.1 | p.50 | FR-AC-001 | Login; menu | TC-AC-001.1, 001.2 (4 cases) |
| FRBS 1.1.2 | p.50 | FR-AC-002 | Session dialog | TC-AC-002.1, 002.2 (4 cases) |
| FRBS 1.1.3 | p.50 | FR-AC-002 | Session dialog | TC-AC-002.1, 002.2 (4 cases) |
| FRBS 2.2.0 | p.51 | FR-AC-010 | Currencies & Rates | TC-AC-010.1, 010.2, 010.3, 010.4 (5 cases) |
| FRBS 2.3.0 | p.51 | FR-AC-011 | Chart of Accounts | TC-AC-011.1, 011.2, 011.3, 011.4 (7 cases) |
| FRBS 2.3.1 | p.51 | FR-AC-012 | Chart Upload | TC-AC-012.1, 012.2, 012.3 (7 cases) |
| FRBS 2.3.2 | p.52 | FR-AC-013 | Chart of Accounts (Numbering) | TC-AC-013.1, 013.2, 013.3 (5 cases) |
| FRBS 2.3.3 | p.52 | FR-AC-014 | Chart of Accounts; New Journal | TC-AC-014.1, 014.2 (4 cases) |
| FRBS 2.3.4 | p.53 | FR-AC-011 | Chart of Accounts | TC-AC-011.1, 011.2, 011.3, 011.4 (7 cases) |
| FRBS 2.3.5 | p.53 | FR-AC-011 | Chart of Accounts | TC-AC-011.1, 011.2, 011.3, 011.4 (7 cases) |
| FRBS 3.6.0 | p.65 | FR-AC-010, FR-AC-011 | Currencies & Rates; Chart of Accounts | TC-AC-010.1, 010.2, 010.3, 010.4, 011.1, 011.2, 011.3, 011.4 (12 cases) |
| FRBS 2.4.0 | p.53 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.1 | p.53 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.2 | p.53 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.3 | p.54 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.4 | p.54 | FR-AC-021 | Report (column filters) | TC-AC-021.1, 021.2 (2 cases) |
| FRBS 2.4.5 | p.54 | FR-AC-022 | Report Batch | TC-AC-022.1, 022.2, 022.3 (5 cases) |
| FRBS 2.4.6 | p.54 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.7 | p.54 | FR-AC-022 | Report Batch | TC-AC-022.1, 022.2, 022.3 (5 cases) |
| FRBS 2.4.8 | p.55 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 2.4.9 | p.55 | FR-AC-023 | Report (print options) | TC-AC-023.1, 023.2 (3 cases) |
| FRBS 2.4.10 | p.55 | FR-AC-020 | Report Centre; Report Pack | TC-AC-020.1, 020.2, 020.3, 020.4 (4 cases) |
| FRBS 3.2.0 | p.63 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | Report Pack; Account Schedules; BIR Forms & Books | TC-AC-060.1, 060.2, 061.1, 061.2, 061.3, 062.1, 062.2, 063.1, 063.2, 063.3 (22 cases) |
| FRBS 2.5.0 | p.55 | FR-AC-030 | Journals; event log | TC-AC-030.1, 030.2, 030.3 (5 cases) |
| FRBS 2.5.1 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | TC-AC-031.1, 031.2 (4 cases) |
| FRBS 2.5.2 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | TC-AC-031.1, 031.2 (4 cases) |
| FRBS 2.5.3 | p.56 | FR-AC-031 | Journals (Assign, Assigned to me) | TC-AC-031.1, 031.2 (4 cases) |
| FRBS 2.5.4 | p.56 | FR-AC-034 | New Journal; Journal | TC-AC-034.1, 034.2, 034.3 (6 cases) |
| FRBS 2.5.5 | p.57 | FR-AC-034 | New Journal; Journal | TC-AC-034.1, 034.2, 034.3 (6 cases) |
| FRBS 2.5.6 | p.57 | FR-AC-036 | Journals; My Approvals | TC-AC-036.1, 036.2, 036.3 (6 cases) |
| FRBS 2.5.7 | p.57 | FR-AC-036 | Journals; My Approvals | TC-AC-036.1, 036.2, 036.3 (6 cases) |
| FRBS 2.5.8 | p.57 | FR-AC-036 | Journals; My Approvals | TC-AC-036.1, 036.2, 036.3 (6 cases) |
| FRBS 2.5.9 | p.57 | FR-AC-036 | Journals; My Approvals | TC-AC-036.1, 036.2, 036.3 (6 cases) |
| FRBS 2.5.10 | p.58 | FR-AC-035 | Confirm posting dialog | TC-AC-035.1, 035.2 (3 cases) |
| FRBS 2.8.0 | p.59 | FR-AC-032 | New Journal | TC-AC-032.1, 032.2, 032.3, 032.4 (5 cases) |
| FRBS 2.8.1 | p.60 | FR-AC-032, FR-AC-033 | New Journal; Journals (REVERSAL) | TC-AC-032.1, 032.2, 032.3, 032.4, 033.1, 033.2 (8 cases) |
| FRBS 2.8.2 | p.60 | FR-AC-032 | New Journal | TC-AC-032.1, 032.2, 032.3, 032.4 (5 cases) |
| FRBS 2.8.3 | p.60 | FR-AC-035 | Confirm posting dialog | TC-AC-035.1, 035.2 (3 cases) |
| FRBS 2.8.4 | p.61 | FR-AC-034 | New Journal; Journal | TC-AC-034.1, 034.2, 034.3 (6 cases) |
| FRBS 2.8.5 | p.61 | FR-AC-032 | New Journal | TC-AC-032.1, 032.2, 032.3, 032.4 (5 cases) |
| FRBS 2.9.0 | p.61 | FR-AC-037 | Edit Journal | TC-AC-037.1, 037.2 (3 cases) |
| FRBS 3.1.0 | p.62 | FR-AC-030 | Journals; event log | TC-AC-030.1, 030.2, 030.3 (5 cases) |
| FRBS 3.6.0b | p.66 | FR-AC-034 | New Journal; Journal | TC-AC-034.1, 034.2, 034.3 (6 cases) |
| FRBS 2.6.0 | p.58; Add.1 p.35 | FR-AC-040 | GL Close & Cut-Off | TC-AC-040.1, 040.2, 040.3, 040.4 (7 cases) |
| FRBS 2.6.1 | p.58 | FR-AC-040 | GL Close & Cut-Off | TC-AC-040.1, 040.2, 040.3, 040.4 (7 cases) |
| FRBS 2.7.0 | p.59 | FR-AC-041 | Period-End & Year-End | TC-AC-041.1, 041.2, 041.3 (5 cases) |
| FRBS 2.7.1 | p.59 | FR-AC-041 | Period-End & Year-End | TC-AC-041.1, 041.2, 041.3 (5 cases) |
| FRBS 3.4.0 | p.64 | FR-AC-042 | GL Close & Cut-Off | TC-AC-042.1, 042.2, 042.3 (4 cases) |
| FRBS 3.4.1 | p.65 | FR-AC-042 | GL Close & Cut-Off | TC-AC-042.1, 042.2, 042.3 (4 cases) |
| FRBS 3.5.0 | p.65 | FR-AC-043 | FX Revaluation | TC-AC-043.1, 043.2 (4 cases) |
| FRBS 3.3.0 | p.63 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | TC-AC-050.1, 050.2, 050.3, 050.4 (6 cases) |
| FRBS 3.3.1 | p.63 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | TC-AC-050.1, 050.2, 050.3, 050.4 (6 cases) |
| FRBS 3.3.2 | p.64 | FR-AC-050 | Bank Statements; Bank Reconciliation; Bank Statement Layouts | TC-AC-050.1, 050.2, 050.3, 050.4 (6 cases) |
| FRBS 3.3.3 | p.64 | FR-AC-051 | Report Centre | TC-AC-051.1, 051.2 (2 cases) |
| FRBS 2.10.0 | p.61 | FR-AC-052 | Service Fee Runs; Service Fee Rates | TC-AC-052.1, 052.2, 052.3, 052.4 (10 cases) |
| FRBS 2.10.1 | p.62 | FR-AC-053 | Service Fee Run (lines) | TC-AC-053.1, 053.2, 053.3 (6 cases) |
| FRBS 2.10.2 | p.62 | FR-AC-053 | Service Fee Run (lines) | TC-AC-053.1, 053.2, 053.3 (6 cases) |
| FRBS 3.1.1 | Add.2 p.5-6 | FR-AC-054 | Cost-Centre Rules | TC-AC-054.1, 054.2, 054.3 (3 cases) |
| FRBS 3.1.2 | Add.2 p.6 | FR-AC-055 | Accounting rules | TC-AC-055.1, 055.2 (3 cases) |

## Business and system administration (BASAU)

<!-- table: widths=2.2,2.2,2.6,4.6,5.2 caption="BASAU requirement IDs to FR, screen and test cases" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| BASAU 1.1.0 | p.127 | FR-AC-001 | Login; menu | TC-AC-001.1, 001.2 (4 cases) |
| BASAU 1.1.1 | p.127 | FR-AC-001 | Login; menu | TC-AC-001.1, 001.2 (4 cases) |
| BASAU 1.1.2 | p.128 | FR-AC-002 | Session dialog | TC-AC-002.1, 002.2 (4 cases) |
| BASAU 1.1.3 | p.128 | FR-AC-002 | Session dialog | TC-AC-002.1, 002.2 (4 cases) |
| BASAU 2.2.0 | p.128 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.2.1 | p.128 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.2.2 | p.128 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.2.3 | p.128 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.2.4 | p.128 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.2.5 | p.129 | FR-AC-070 | Lists of Values; My Approvals | TC-AC-070.1, 070.2, 070.3, 070.4 (5 cases) |
| BASAU 2.3.0 | p.129 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.3.1 | p.129 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.3.2 | p.129 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.3.3 | p.129 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.4.0 | p.130 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.4.1 | p.130 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.4.2 | p.130 | FR-AC-071 | Access Requests; Users; Roles | TC-AC-071.1, 071.2, 071.3 (8 cases) |
| BASAU 2.5.0 | p.130 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.5.1 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.5.2 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.5.3 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.6.0 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.6.1 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.6.2 | p.131 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |
| BASAU 2.6.3 | p.132 | FR-AC-072 | My Approvals; Access Requests | TC-AC-072.1, 072.2, 072.3, 072.4 (8 cases) |

## Disbursement (DIS)

<!-- table: widths=2.2,2.6,2.6,4.4,5.2 caption="DIS requirement IDs to FR, screen and test cases" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| DIS 1.1.0 | p.68 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| DIS 1.1.1 | p.69 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| DIS 1.1.2 | p.69 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| DIS 1.1.3 | p.69 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| DIS 2.2.0 | p.69 | FR-DS-010 | Payees | TC-DS-010.1, 010.2, 010.3, 010.4 (9 cases) |
| DIS 2.2.1 | p.69 | FR-DS-012 | Payees | TC-DS-012.1, 012.2 (2 cases) |
| DIS 2.2.2 | p.70 | FR-DS-011 | Payee | TC-DS-011.1, 011.2 (2 cases) |
| DIS 2.2.3 | p.70 | FR-DS-010 | Payees | TC-DS-010.1, 010.2, 010.3, 010.4 (9 cases) |
| DIS 2.2.4 | p.70 | FR-DS-013 | Payee | TC-DS-013.1, 013.2 (3 cases) |
| DIS 2.2.5 | p.70 | FR-DS-011 | Payee | TC-DS-011.1, 011.2 (2 cases) |
| DIS 2.2.6 | p.71 | FR-DS-010 | Payees | TC-DS-010.1, 010.2, 010.3, 010.4 (9 cases) |
| DIS 2.2.7 | p.71 | FR-DS-010 | Payees | TC-DS-010.1, 010.2, 010.3, 010.4 (9 cases) |
| DIS 2.2.8 | p.71; Add.1 p.31-32; Add.2 p.11-12 | FR-DS-014 | Payees | TC-DS-014.1, 014.2 (3 cases) |
| DIS 2.3.0 | p.71 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.1 | p.72 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.2 | p.72 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.3 | p.72 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.4 | p.72 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.5 | p.72 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.6 | p.73 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.7 | p.73 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.8 | p.73 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 2.3.9 | p.73 | FR-DS-080 | Disbursement Reports | TC-DS-080.1, 080.2 (3 cases) |
| DIS 3.28.0 | p.100 | FR-DS-081 | Disbursement End of Day | TC-DS-081.1, 081.2 (2 cases) |
| DIS 3.28.1 | p.100 | FR-DS-083 | Disbursement Reports | TC-DS-083.1 (2 cases) |
| DIS 3.28.2 | p.101 | FR-DS-081 | Disbursement End of Day | TC-DS-081.1, 081.2 (2 cases) |
| DIS 3.28.3 | p.101 | FR-DS-082 | Disbursement Reports | TC-DS-082.1, 082.2 (4 cases) |
| DIS 3.28.4 | p.102 | FR-DS-083 | Disbursement Reports | TC-DS-083.1 (2 cases) |
| DIS 3.29.0 | Add.2 p.7 | FR-DS-092 | - | TC-DS-092.1, 092.2 (2 cases) |
| DIS 3.30.2 | Add.2 p.10-11 | FR-DS-037 | Setup > Employees | TC-DS-037.1, 037.2 (6 cases) |
| DIS 2.4.0 | p.73 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.4.1 | p.73 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.4.2 | p.74 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.4.3 | p.74 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.4.4 | p.74 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.5.0 | p.75 | FR-DS-024 | Disbursement Uploads | TC-DS-024.1, 024.2 (3 cases) |
| DIS 2.5.1 | p.75 | FR-DS-024 | Disbursement Uploads | TC-DS-024.1, 024.2 (3 cases) |
| DIS 2.6.0 | p.75 | FR-DS-020 | Disbursement Workbench | TC-DS-020.1, 020.2, 020.3 (5 cases) |
| DIS 2.6.1 | p.76 | FR-DS-023 | Encode Payment Request | TC-DS-023.1, 023.2 (3 cases) |
| DIS 2.6.2 | p.76 | FR-DS-020 | Disbursement Workbench | TC-DS-020.1, 020.2, 020.3 (5 cases) |
| DIS 3.25.0 | p.95 | FR-DS-020 | Disbursement Workbench | TC-DS-020.1, 020.2, 020.3 (5 cases) |
| DIS 3.25.1 | p.96 | FR-DS-021 | Disbursement Workbench | TC-DS-021.1, 021.2 (2 cases) |
| DIS 3.25.2 | p.96 | FR-DS-022 | Disbursement Workbench | TC-DS-022.1, 022.2 (3 cases) |
| DIS 2.7.0 | p.77 | FR-DS-033 | Disbursement Voucher | TC-DS-033.1, 033.2 (4 cases) |
| DIS 2.7.1 | p.77 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.7.2 | p.78 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.7.3 | p.78 | FR-DS-025 | Disbursement Workbench | TC-DS-025.1, 025.2, 025.3 (3 cases) |
| DIS 2.7.4 | p.78 | FR-DS-030 | Disbursement Voucher | TC-DS-030.1, 030.2, 030.3 (6 cases) |
| DIS 2.7.5 | p.79 | FR-DS-030 | Disbursement Voucher | TC-DS-030.1, 030.2, 030.3 (6 cases) |
| DIS 2.7.6 | p.80 | FR-DS-031 | Disbursement Voucher | TC-DS-031.1, 031.2 (4 cases) |
| DIS 2.7.7 | p.81 | FR-DS-034 | Disbursement Voucher | TC-DS-034.1, 034.2 (2 cases) |
| DIS 2.7.8 | p.81 | FR-DS-035 | Disbursement Voucher | TC-DS-035.1, 035.2 (2 cases) |
| DIS 2.7.9 | p.82 | FR-DS-035 | Disbursement Voucher | TC-DS-035.1, 035.2 (2 cases) |
| DIS 2.7.10 | p.82 | FR-DS-032 | Disbursement Voucher | TC-DS-032.1 (2 cases) |
| DIS 2.7.11 | p.82 | FR-DS-036 | Disbursement Voucher | TC-DS-036.1, 036.2 (2 cases) |
| DIS 2.7.12 | p.83 | FR-DS-063 | Disbursement End of Day | TC-DS-063.1, 063.2 (2 cases) |
| DIS 3.30.0 | Add.2 p.9 | FR-DS-032 | Disbursement Voucher | TC-DS-032.1 (2 cases) |
| DIS 3.30.1 | Add.2 p.9-10 | FR-DS-037 | Setup > Employees | TC-DS-037.1, 037.2 (6 cases) |
| DIS 2.8.0 | p.83 | FR-DS-050 | Disbursement Voucher | TC-DS-050.1, 050.2 (2 cases) |
| DIS 2.8.1 | p.83 | FR-DS-050 | Disbursement Voucher | TC-DS-050.1, 050.2 (2 cases) |
| DIS 2.8.2 | p.84 | FR-DS-050 | Disbursement Voucher | TC-DS-050.1, 050.2 (2 cases) |
| DIS 2.8.3 | p.84 | FR-DS-050 | Disbursement Voucher | TC-DS-050.1, 050.2 (2 cases) |
| DIS 2.8.4 | p.85 | FR-DS-050 | Disbursement Voucher | TC-DS-050.1, 050.2 (2 cases) |
| DIS 2.8.5 | p.85 | FR-DS-051 | Disbursement Voucher | TC-DS-051.1, 051.2 (6 cases) |
| DIS 2.9.0 | p.85 | FR-DS-043 | Disbursement Workbench | TC-DS-043.1, 043.2 (3 cases) |
| DIS 2.22.0 | p.93 | FR-DS-053 | Disbursement Uploads | TC-DS-053.1, 053.2 (4 cases) |
| DIS 3.26.0 | p.96 | FR-DS-052 | Disbursement Voucher | TC-DS-052.1, 052.2 (3 cases) |
| DIS 3.26.1 | p.97 | FR-DS-053 | Disbursement Uploads | TC-DS-053.1, 053.2 (4 cases) |
| DIS 3.26.2 | p.97 | FR-DS-054 | Disbursement Voucher | TC-DS-054.1, 054.2 (3 cases) |
| DIS 3.26.3 | p.97 | FR-DS-052 | Disbursement Voucher | TC-DS-052.1, 052.2 (3 cases) |
| DIS 3.26.4 | p.98 | FR-DS-053 | Disbursement Uploads | TC-DS-053.1, 053.2 (4 cases) |
| DIS 3.26.5 | p.98 | FR-DS-052 | Disbursement Voucher | TC-DS-052.1, 052.2 (3 cases) |
| DIS 3.26.6 | p.98 | FR-DS-052 | Disbursement Voucher | TC-DS-052.1, 052.2 (3 cases) |
| DIS 3.26.7 | p.99 | FR-DS-052 | Disbursement Voucher | TC-DS-052.1, 052.2 (3 cases) |
| DIS 2.10.0 | p.86 | FR-DS-056 | Disbursement Voucher | TC-DS-056.1, 056.2 (3 cases) |
| DIS 2.10.1 | p.86 | FR-DS-056 | Disbursement Voucher | TC-DS-056.1, 056.2 (3 cases) |
| DIS 2.10.2 | p.86 | FR-DS-056 | Disbursement Voucher | TC-DS-056.1, 056.2 (3 cases) |
| DIS 2.11.0 | p.86 | FR-DS-057 | Disbursement Voucher | TC-DS-057.1, 057.2 (6 cases) |
| DIS 2.11.1 | p.86 | FR-DS-057 | Disbursement Voucher | TC-DS-057.1, 057.2 (6 cases) |
| DIS 2.11.2 | p.87 | FR-DS-057 | Disbursement Voucher | TC-DS-057.1, 057.2 (6 cases) |
| DIS 2.12.0 | p.87 | FR-DS-058 | Tax & Statutory > BIR Form 2307 | TC-DS-058.1, 058.2 (3 cases) |
| DIS 2.13.0 | p.87 | FR-DS-040 | Disbursement Workbench | TC-DS-040.1, 040.2 (4 cases) |
| DIS 2.14.0 | p.88 | FR-DS-040 | Disbursement Workbench | TC-DS-040.1, 040.2 (4 cases) |
| DIS 2.15.0 | p.88 | FR-DS-040 | Disbursement Workbench | TC-DS-040.1, 040.2 (4 cases) |
| DIS 2.16.0 | p.88 | FR-DS-060 | Disbursement End of Day | TC-DS-060.1, 060.2 (2 cases) |
| DIS 2.16.1 | p.88 | FR-DS-061 | Disbursement End of Day | TC-DS-061.1, 061.2 (2 cases) |
| DIS 2.16.2 | p.88 | FR-DS-062 | Disbursement End of Day | TC-DS-062.1, 062.2 (2 cases) |
| DIS 2.16.3 | p.89 | FR-DS-060 | Disbursement End of Day | TC-DS-060.1, 060.2 (2 cases) |
| DIS 2.16.4 | p.89 | FR-DS-060 | Disbursement End of Day | TC-DS-060.1, 060.2 (2 cases) |
| DIS 2.16.5 | p.89 | FR-DS-060 | Disbursement End of Day | TC-DS-060.1, 060.2 (2 cases) |
| DIS 2.16.6 | p.89 | FR-DS-062 | Disbursement End of Day | TC-DS-062.1, 062.2 (2 cases) |
| DIS 2.17.0 | p.90 | FR-DS-064 | Account Funding | TC-DS-064.1, 064.2, 064.3 (7 cases) |
| DIS 2.17.1 | p.90 | FR-DS-064 | Account Funding | TC-DS-064.1, 064.2, 064.3 (7 cases) |
| DIS 2.17.2 | p.90 | FR-DS-064 | Account Funding | TC-DS-064.1, 064.2, 064.3 (7 cases) |
| DIS 2.17.3 | p.90 | FR-DS-064 | Account Funding | TC-DS-064.1, 064.2, 064.3 (7 cases) |
| DIS 2.17.4 | p.90; Add.1 p.33 | FR-DS-064 | Account Funding | TC-DS-064.1, 064.2, 064.3 (7 cases) |
| DIS 2.18.0 | p.91 | FR-DS-043 | Disbursement Workbench | TC-DS-043.1, 043.2 (3 cases) |
| DIS 2.19.0 | p.91 | FR-DS-041 | Disbursement Workbench | TC-DS-041.1, 041.2, 041.3 (5 cases) |
| DIS 2.20.0 | p.92 | FR-DS-044 | Disbursement Voucher | TC-DS-044.1, 044.2, 044.3 (4 cases) |
| DIS 2.21.0 | p.92 | FR-DS-042 | Disbursement Voucher | TC-DS-042.1, 042.2 (2 cases) |
| DIS 2.23.0 | p.93 | FR-DS-070 | Bank Accounts and Checks | TC-DS-070.1, 070.2 (4 cases) |
| DIS 2.23.1 | p.93 | FR-DS-070 | Bank Accounts and Checks | TC-DS-070.1, 070.2 (4 cases) |
| DIS 2.23.2 | p.93 | FR-DS-070 | Bank Accounts and Checks | TC-DS-070.1, 070.2 (4 cases) |
| DIS 2.24.0 | p.93 | FR-DS-071 | Bank Accounts and Checks | TC-DS-071.1, 071.2 (2 cases) |
| DIS 2.24.1 | p.93 | FR-DS-071 | Bank Accounts and Checks | TC-DS-071.1, 071.2 (2 cases) |
| DIS 2.24.2 | p.94; Add.1 p.33 | FR-DS-071 | Bank Accounts and Checks | TC-DS-071.1, 071.2 (2 cases) |
| DIS 3.27.0 | p.99; Add.1 p.31 | FR-DS-045 | Disbursement Workbench | TC-DS-045.1, 045.2 (2 cases) |
| DIS 3.27.1 | p.100 | FR-DS-055 | Disbursement Voucher | TC-DS-055.1, 055.2 (3 cases) |
| DIS 3.27.2 | p.100 | FR-DS-093 | Invoice Search | TC-DS-093.1, 093.2 (2 cases) |
| DIS 3.29.1 | Add.2 p.7-8 | FR-DS-091 | Remittance batch | TC-DS-091.1, 091.2 (3 cases) |
| DIS 3.29.2 | Add.2 p.8-9 | FR-DS-090 | Remittance batch | TC-DS-090.1, 090.2, 090.3 (3 cases) |

## Payment Requests (MKT)

<!-- table: widths=2.2,2.6,2.6,4.4,5.2 caption="MKT requirement IDs to FR, screen and test cases" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| MKT 1.1.0 | p.105 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.1.1 | p.105 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.1.2 | p.105 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.1.3 | p.105 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.2.0 | p.105 | FR-PQ-001 | Requests Home | TC-PQ-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.3.0 | p.106 | FR-PQ-001 | Requests Home | TC-PQ-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.4.0 | p.106 | FR-PQ-001 | Requests Home | TC-PQ-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.5.0 | p.106 | FR-PQ-001 | Requests Home | TC-PQ-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.6.0 | p.106 | FR-PQ-001 | Requests Home | TC-PQ-001.1, 001.2, 001.3 (4 cases) |
| MKT 1.7.0 | p.106 | FR-PQ-002 | Report Centre | TC-PQ-002.1, 002.2 (2 cases) |
| MKT 1.7.1 | p.106 | FR-PQ-002 | Report Centre | TC-PQ-002.1, 002.2 (2 cases) |
| MKT 1.7.2 | p.107 | FR-PQ-002 | Report Centre | TC-PQ-002.1, 002.2 (2 cases) |
| MKT 1.7.3 | p.107 | FR-PQ-002 | Report Centre | TC-PQ-002.1, 002.2 (2 cases) |
| MKT 1.8.0 | p.107 | FR-PQ-005 | Request | TC-PQ-005.1, 005.2 (4 cases) |
| MKT 1.9.0 | p.107 | FR-PQ-005 | Request | TC-PQ-005.1, 005.2 (4 cases) |
| MKT 1.10.0 | p.107 | FR-PQ-003, FR-PQ-004, FR-PQ-017 | New Refund Request; New Cash Advance; Request | TC-PQ-003.1, 003.2, 003.3, 004.1, 004.2, 017.1, 017.2, 017.3 (22 cases) |
| MKT 1.11.0 | p.108 | FR-PQ-006 | Request | TC-PQ-006.1, 006.2, 006.3 (4 cases) |
| MKT 1.12.0 | p.108 | FR-PQ-007 | Request | TC-PQ-007.1, 007.2 (2 cases) |
| MKT 1.13.0 | p.108 | FR-PQ-007 | Request | TC-PQ-007.1, 007.2 (2 cases) |
| MKT 1.14.0 | p.109 | FR-PQ-008 | Request | TC-PQ-008.1, 008.2, 008.3 (4 cases) |
| MKT 1.15.0 | p.109 | FR-PQ-008 | Request | TC-PQ-008.1, 008.2, 008.3 (4 cases) |
| MKT 1.16.0 | p.109 | FR-PQ-009 | Request | TC-PQ-009.1, 009.2, 009.3 (5 cases) |
| MKT 1.16.1 | p.109 | FR-PQ-009 | Request | TC-PQ-009.1, 009.2, 009.3 (5 cases) |
| MKT 1.16.2 | p.109 | FR-PQ-009 | Request | TC-PQ-009.1, 009.2, 009.3 (5 cases) |
| MKT 1.16.3 | p.109 | FR-PQ-009 | Request | TC-PQ-009.1, 009.2, 009.3 (5 cases) |
| MKT 1.17.0 | p.110 | FR-PQ-010 | Request | TC-PQ-010.1, 010.2 (3 cases) |
| MKT 1.18.0 | p.110 | FR-PQ-012 | Requests Home | TC-PQ-012.1, 012.2 (3 cases) |
| MKT 1.18.1 | p.110 | FR-PQ-012 | Requests Home | TC-PQ-012.1, 012.2 (3 cases) |
| MKT 1.19.0 | p.110 | FR-PQ-011 | Cancel a Check | TC-PQ-011.1, 011.2 (5 cases) |
| MKT 1.20.0 | p.111 | FR-PQ-013 | Request | TC-PQ-013.1, 013.2 (3 cases) |
| MKT 2.22.0 | p.111 | FR-PQ-007 | Request | TC-PQ-007.1, 007.2 (2 cases) |
| MKT 2.23.0 | p.111 | FR-PQ-014 | New Refund Request | TC-PQ-014.1, 014.2 (3 cases) |
| MKT 2.24.0 | p.111 | FR-PQ-015 | Request | TC-PQ-015.1, 015.2 (2 cases) |
| MKT 2.25.0 | p.112; Add.1 p.34-35 | FR-PQ-016 | Client | TC-PQ-016.1, 016.2 (3 cases) |
| MKT 2.25.1 | p.112 | FR-PQ-016 | Client | TC-PQ-016.1, 016.2 (3 cases) |
| MKT 2.26.0 | p.112 | FR-PQ-012 | Requests Home | TC-PQ-012.1, 012.2 (3 cases) |

## ACSL

<!-- table: widths=2.2,2.6,2.6,4.4,5.2 caption="ACSL requirement IDs to FR, screen and test cases" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| ACSL 1.1.0 | p.114 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| ACSL 1.1.1 | p.115 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| ACSL 1.1.2 | p.115 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| ACSL 1.1.3 | p.115 | FR-DS-001 | Login | TC-DS-001.1, 001.2, 001.3 (4 cases) |
| ACSL 2.2.0 | p.115 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.2.1 | p.115 | FR-AS-002 | Insurer SOA Reconciliation | TC-AS-002.1, 002.2, 002.3 (7 cases) |
| ACSL 2.3.0 | p.115 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.1 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.2 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.3 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.4 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.5 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.3.6 | p.116 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.4.0 | p.117; Add.1 p.33-34 | FR-AS-002 | Insurer SOA Reconciliation | TC-AS-002.1, 002.2, 002.3 (7 cases) |
| ACSL 2.13.0 | p.120 | FR-AS-003 | Insurer SOA Reconciliation | TC-AS-003.1, 003.2, 003.3 (3 cases) |
| ACSL 2.13.1 | p.121 | FR-AS-003 | Insurer SOA Reconciliation | TC-AS-003.1, 003.2, 003.3 (3 cases) |
| ACSL 2.13.2 | p.121 | FR-AS-004 | GL-SL Reconciliation | TC-AS-004.1, 004.2 (2 cases) |
| ACSL 2.14.0 | p.121 | FR-AS-003 | Insurer SOA Reconciliation | TC-AS-003.1, 003.2, 003.3 (3 cases) |
| ACSL 2.14.1 | p.122 | FR-AS-003 | Insurer SOA Reconciliation | TC-AS-003.1, 003.2, 003.3 (3 cases) |
| ACSL 2.14.2 | p.123 | FR-AS-001 | Report Centre | TC-AS-001.1, 001.2 (3 cases) |
| ACSL 2.14.3 | p.124 | FR-AS-005 | Report Centre | TC-AS-005.1, 005.2 (2 cases) |
| ACSL 2.14.4 | p.125 | FR-AS-005 | Report Centre | TC-AS-005.1, 005.2 (2 cases) |
| ACSL 2.5.0 | p.117 | FR-AS-010 | ACSL Cases | TC-AS-010.1, 010.2, 010.3 (5 cases) |
| ACSL 2.5.5 | p.117; Add.1 p.34 | FR-PQ-006, FR-AS-010 | Request; ACSL Cases | TC-PQ-006.1, 006.2, 006.3, TC-AS-010.1, TC-AS-010.2, TC-AS-010.3 (9 cases) |
| ACSL 2.5.1 | p.117 | FR-AS-010 | ACSL Cases | TC-AS-010.1, 010.2, 010.3 (5 cases) |
| ACSL 2.5.2 | p.118 | FR-AS-010 | ACSL Cases | TC-AS-010.1, 010.2, 010.3 (5 cases) |
| ACSL 2.5.3 | p.118 | FR-AS-010 | ACSL Cases | TC-AS-010.1, 010.2, 010.3 (5 cases) |
| ACSL 2.5.4 | p.118 | FR-AS-011 | ACSL Case | TC-AS-011.1, 011.2 (2 cases) |
| ACSL 2.6.0 | p.118 | FR-AS-012 | ACSL Case | TC-AS-012.1, 012.2 (3 cases) |
| ACSL 2.6.1 | p.118 | FR-AS-012 | ACSL Case | TC-AS-012.1, 012.2 (3 cases) |
| ACSL 2.6.2 | p.119 | FR-AS-013 | ACSL Case | TC-AS-013.1, 013.2 (2 cases) |
| ACSL 2.7.0 | p.119 | FR-AS-020 | Correction Entries | TC-AS-020.1, 020.2 (2 cases) |
| ACSL 2.8.0 | p.119 | FR-AS-020 | Correction Entries | TC-AS-020.1, 020.2 (2 cases) |
| ACSL 2.9.0 | p.119 | FR-AS-021 | Correction | TC-AS-021.1, 021.2, 021.3, 021.4 (11 cases) |
| ACSL 2.9.1 | Add.2 p.12-13 | FR-AS-021 | Correction | TC-AS-021.1, 021.2, 021.3, 021.4 (11 cases) |
| ACSL 2.9.2 | Add.2 p.13 | FR-AS-025 | Remittance Deductions | TC-AS-025.1, 025.2, 025.3 (7 cases) |
| ACSL 2.10.0 | p.119 | FR-AS-022 | Correction | TC-AS-022.1, 022.2 (3 cases) |
| ACSL 2.11.0 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.11.1 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.11.2 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.12.0 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.12.1 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.12.2 | p.120 | FR-AS-023 | Correction | TC-AS-023.1, 023.2 (3 cases) |
| ACSL 2.15.0 | p.125 | FR-AS-024 | Correction | TC-AS-024.1, 024.2 (3 cases) |
| ACSL 2.16.0 | p.126; Add.1 p.34 | FR-AS-026 | Invoice 360 | TC-AS-026.1, 026.2 (2 cases) |

## Coverage summary

<!-- table: widths=6,1.9,1.9,1.9 caption="Coverage summary" size=8.5 -->
| Group | BRD IDs | Covered | Out |
|---|---|---|---|
| FRBS (Accounting) | 60 | 60 | 0 |
| Business and system administration (BASAU) | 25 | 25 | 0 |
| Disbursement (DIS) | 111 | 111 | 1 |
| Payment Requests (MKT) | 36 | 36 | 0 |
| ACSL | 45 | 45 | 0 |
| **Total** | **277** | **277** | **1** |

# Navigation

This chapter shows how each user of Accounting, Disbursement, the Marketing requests and ACSL reaches the screens. The sidebar shows a screen only when the user's role holds its permission (section 3.2), so each persona sees a different menu. The tables follow the proposed role grants.

## Screens of Accounting, Disbursement and ACSL

The 51 screens specified in chapter 13, with the menu path and the roles that can open them. A screen without its own menu entry (a record, a dialog, a report) is reached from the screen before it; its path ends with that screen. The administration screens of BASAU (Lists of Values, Access Requests, Users, Roles & Permissions) are the screens of the BRD-1 and BRD-11 sets and are specified there.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screen-index
```

## Screen flow

How the screens link: from a list to its record, from a record action to the next screen, and from one unit's work to the next. The walkthroughs of chapter 14 follow these links with real steps.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: flow
```

<!-- portrait -->

## Menu by persona

For each persona, the SIT and UAT user of the seed data and the sidebar that user sees, section by section. The BRD column shows which BRD owns a section; entries of other BRDs are listed so the business unit sees the whole menu of its users.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: menus
```

## Common screen elements

Elements that behave the same on every screen of this set are described once here and not repeated in the screen specifications.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: common
```

<!-- landscape -->

# Screen specifications

One specification per screen, grouped by area: the general ledger, the chart and the rates; closing and bank reconciliation; reports, the service fee and the BIR outputs; Disbursement; the Marketing refund and cash-advance requests; ACSL. Each gives:

- **Purpose**, **who can open it** (personas and the permission), **navigation** (menu path and the other ways in) and the related **FRs**;
- the **screenshots** taken with seed data; the first carries numbered callouts that match the **Fields** table;
- the **Fields** table: section, label, type, length or format, mandatory, source list, default, the statuses in which the field can be changed, the validation and the message shown when it fails;
- the **Actions** table: button, who sees it, when it is enabled, what happens, the resulting status and the notification sent;
- the **business rules**, the **expected outcome** and the **test cases** of the test plan that exercise the screen.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screens
```

<!-- portrait -->

# End-to-end walkthroughs

Five walkthroughs follow a case through the screens, persona by persona, with what the user does, what the user sees and the result of each step. They use seed data only and run in this order on the SIT environment. They are the script of the SIT review sessions of the Start Here guide.

## WT-A A manual accrual from the voucher to its posting and automatic reversal

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-A
```

## WT-B A client refund from the Refund Request Form to the payment

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-B
```

## WT-C An insurer's statement of account reconciled and a wrong posting corrected

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-C
```

## WT-D Month end: the revaluation rate, the revaluation, the GL close and the service fee

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-D
```

## WT-E Controls and the messages the user sees

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-E
```

<!-- landscape -->

# Messages catalogue

Every message a user of this set can see, grouped by the screen or dialog that shows it, with its code, its type and what the user does. The texts are quoted exactly as the screens show them.

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

The in-app notifications, alerts and e-mails of Accounting, Disbursement, the Marketing requests and ACSL: what triggers each, who receives it and what it contains. Each user chooses the in-app and e-mail channels of the events on Notification Settings. Payment confirmations to payees are e-mailed from the end of day.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: notifications
```

<!-- portrait -->

# Document outputs

The documents this set generates, with the BDO Insure letterhead, the "Confidential" footer and page numbers on the PDF documents. For each: the template, the format, the screen that produces it, the password protection, where every field comes from and the first page as generated from seed data. The files for the bank and the reports are shown with a selection of their columns.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: documents
```

# Upload screens

The uploads of this set run on the screen of the unit that owns them: download the template, upload the filled file, review every row with its message, then process the valid rows. A file uploaded before is refused. Each upload type below has its template columns; the column checks (mandatory, number, date, Y/N) apply to every type, and the row checks listed with each type come on top. The bank statements and the insurer statements of account are read on their own screens with the layouts given in their screen specifications (chapter 13).

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: uploads
```

<!-- landscape -->

# Cross-BRD dependencies and interface contract

Accounting posts the journals of every module; Disbursement pays what the other BRDs request and reports the payment back; the Marketing requests come from the client records of New Business; ACSL reads the invoice ledger of Operations and corrects the postings of every module. The contract below lists each exchange: the BRD or system, the direction, what is exchanged, when and how, and who owns the data. Chapter 7 describes the interfaces with the external systems; the technical detail is in the Technical Specification, reviewed by BDOI IT.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: contract
```

<!-- portrait -->

# Sign-off and change control

## What is signed

The business sign-off covers the release set BRD-05 Accounting, Disbursement and ACSL v2.0:

<!-- table: widths=6,11.6 caption="Documents of the release set" -->
| Document | Content |
|---|---|
| 00 Start Here | The map of the pack, the reading order per role, the steps up to closure, who signs what and the dates |
| 01 Sign-off Pack Guide (deck) | Purpose, approach and steps with who does what, the module at a glance, caveats and impacts on other modules, entry and exit criteria, handover and change control |
| 02 This FRS v2.0 | Requirements (chapters 1-11), the business view of the system (chapters 12-19), sign-off (chapter 20), the proposed rules for confirmation (chapter 21) and the screen standards (appendix) |
| 03 Sign-off workbook v2.0 | The screen standards, screens, fields, actions, rules, messages, notifications, menus, upload templates and contract of this FRS, one row each, with the BU review columns; the comments log, meeting minutes, version history and sign-off certificate |
| 04 and 05 Test plan v2.0 and its summary | The test cases traced to the FRs and to the screens of chapter 13 |
| 07 to 09 UX screen documents | The UX Screen Deck, the UX screen register and the image package for the BDOI UX Design team, with the screens as specified in this FRS |

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: counts
```

## How the review is recorded

Each unit records its review in the sign-off workbook: Accept, Change requested or Comment on each row of the screen standards, screen catalogue, field register, business rules and messages, with the comment, the reviewer and the date. Questions, corrections and change requests go to the comments log of the workbook, where the project team answers them. The project team answers every Change requested row in the comments log before sign-off: either the row is corrected in the next version of the set, or the change is raised as a change request (below) and the row is signed as specified.

## What signing freezes

Signing this release set freezes, for Accounting, Disbursement, the Marketing requests and ACSL:

- the screens and their navigation (chapters 12 and 13), the fields with their order, labels, types, mandatory rules, lists and validations;
- the actions with their conditions and resulting statuses, and the business rules;
- the messages (chapter 15), the notifications (chapter 16) and the generated documents (chapter 17);
- the upload templates (chapter 18) and the interface contract with the other BRDs (chapter 19).

Configuration values marked "default" (accounting rules and their accounts, cost-centre rules, rates, parameters, schedules, list entries, templates and layouts, section 9) are not frozen; the Comptrollership administrators, the Business Administrator and the System Administrator change them in the system without a change request.

## Change after sign-off

A change to anything frozen is raised in the Change Management Register. The request states the screen, field, rule or message concerned, the reason and the business priority. The project team assesses it, including its effect on the other BRDs through the interface contract of chapter 19 (for example a change of the disbursement types reaches the remittance and refund requests of Operations and the Marketing requests), and the owners of every BRD it touches approve it. An approved change is delivered as a new version of this release set (v2.1, v2.2 and so on) with its own Start Here guide, and only the changed pages and rows are reviewed and signed again (delta sign-off).

## Proposed rules for confirmation

Chapter 21 lists the proposed business rules and screen behaviour that differ from the BRD or need a decision of BDOI. BDOI records its decision on each item with its review; a decision that changes a screen, field, rule or message is applied in the next version of this set.

<!-- pagebreak -->

## Signatures

By signing, BDOI confirms that this FRS and the sign-off workbook describe the Accounting, Disbursement, Marketing request and ACSL functions, screens and messages it expects in BIBS, accepts the assumptions in section 10.1 and records its decisions on the items of chapter 21. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request. The signatories are those of the approval sheets of the BRD-5 documents.

```signoff
rows:
  - {name: "Roda Lyn D. Gallardo", role: "Business Analyst, ESG - Business Project Services (prepared by)", organisation: BDO Unibank ESG}
  - {name: "Ma. Fides Rivera", role: "MBS Team (input provider)", organisation: BDOI}
  - {name: "Dan Ace R. Cauton", role: "Program Manager, ESG - Business Project Services (reviewer)", organisation: BDO Unibank ESG}
  - {name: "Freddie A. Atanque", role: "Product Owner - Comptrollership - ACSL (reviewer)", organisation: BDOI}
  - {name: "Jennifer T. Lugtu", role: "Product Owner - Comptrollership - ACSL (reviewer)", organisation: BDOI}
  - {name: "", role: "Information Technology Group (reviewer)", organisation: BDOI}
  - {name: "Rodrigo R. Dela Cruz", role: "Product Owner - Comptrollership - Disbursement", organisation: BDOI}
  - {name: "Iris S. Marquez", role: "Product Owner - Comptrollership - FRBS and ACSL", organisation: BDOI}
  - {name: "Perjelyn Joy R. Gutierrez", role: "Product Owner - Comptrollership and Operations", organisation: BDOI}
  - {name: "Shellah Marie C. Miranda", role: "AVP, Product Owner - Marketing, Processing and Collections", organisation: BDOI}
  - {name: "Ronald Allan E. De Leon", role: "VP, Head - Comptrollership", organisation: BDOI}
  - {name: "Roderick L. Lim", role: "FVP, Head - Institutional Banking, SM and BDO Accounts", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

<!-- pagebreak -->

# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, fills a gap the BRD leaves open, or needs a decision of BDOI. None of them removes a BRD requirement; most are settled by an answer of BDOI that is applied as configuration. BDOI records its decision with the review of this set (section 20.5); a decision that changes a screen, field, rule or message is applied in the next version of the FRS.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-AC-01 | Revaluation rate (FRBS 2.2.0; FR-AC-010) | The monthly revaluation rate is the CLOSING rate of the month end; a job copies it as the BOOK rate of the next month (OPS_BOOK_RATE_SOURCE). | The BRD asks for a monthly input without saying which rate is used for booking (AQ03, OQ08). | Confirm the CLOSING and BOOK rates (AQ03, OQ08). |
| CLR-AC-02 | Account number (FRBS 2.3.2; FR-AC-013) | A number is proposed from the numbering scheme of the parent when the code is left blank; a user may still key the code. | BDOI's numbering convention is not given (AQ01). | Give the numbering convention (AQ01). |
| CLR-AC-03 | Negative balances (FRBS 2.5.4, 2.8.4; FR-AC-034) | Each account has a policy ALLOW, WARN or BLOCK; only BLOCK accounts refuse the journal. | The BRD does not name the accounts that may never be negative (AQ30). | Name the accounts that may never be negative (AQ30). |
| CLR-AC-04 | Return of a posting request (FRBS 2.5.7; FR-AC-036) | The journal action is labelled "Return to Maker"; a returned journal is edited and resubmitted. | The BRD says "return"; the label says to whom the journal goes. | Confirm the label. |
| CLR-AC-05 | GL close (FRBS 2.6.0 Add.1; FR-AC-040) | FRBS schedules the close (proposal: 2nd banking day, 17:00); the job runs the checklist and closes, or records the failure. | The preferred date and time are not given (AQ04). | Give the date and time of the close (AQ04). |
| CLR-AC-06 | Reversal of a manual journal (FRBS 2.8.1; FR-AC-032, 033) | The reversal posts in the GL; manual journals record no sub-ledger open items, so there is none to reverse. | The BRD reverses in the GL and the sub-ledger. | Confirm the reversal in the GL only. |
| CLR-AC-07 | Broking-books close (FRBS 3.4.0-3.4.1; FR-AC-042) | The broking books close on the last day of the month at 23:00 Manila; the list of pending broking items shows what the broking modules report; the TL may reopen the books with a reason. | The close time and whether a closed month may be reopened are open (AQ04). | Confirm the close time and the reopening (AQ04). |
| CLR-AC-08 | Revaluation frequency (FRBS 3.5.0; FR-AC-043) | Revaluation runs per period at the CLOSING rate with automatic reversal; daily revaluation is added only if BDOI confirms it. | The BRD says "previous date versus current date", which reads as daily (AQ03). | Choose monthly or daily revaluation (AQ03). |
| CLR-AC-09 | Early incentives (FRBS 3.1.2; FR-AC-055) | The incentive component posts to its own account (4130); placing 4130 under Other Income is a change to BDOI's chart. | The seed chart does not place 4130 under Other Income (AQ01). | Confirm the account of the early incentives in BDOI's chart (AQ01). |
| CLR-AC-10 | Board schedules in Word (FRBS 3.2.0; FR-AC-060) | Schedules export to Excel, PDF, ODS and CSV; the GARD, subsidiaries and Mancom schedules flagged "Word requested" are issued in PDF and Excel. | The report platform issues the listed formats; Word output would be an addition. | Confirm PDF and Excel, or ask for Word output as a change. |
| CLR-AC-11 | Appendix A reports (FRBS 3.2.0; FR-AC-060 to 063) | 28 schedule definitions and 58 report-pack entries have draft layouts (TO_CONFIRM); lease, DTA, ECL and placement data are not held in BIBS; payroll outputs are out of scope. | BDOI has not given the layouts (AQ05, AQ06). | Give the layouts and confirm the data out of scope (AQ05, AQ06). |
| CLR-AC-12 | BIR books and forms (FRBS 3.2.0 VII; FR-AC-063) | Worksheets and loose-leaf books; they are not eFPS, DAT or CAS files. | Electronic filing is not specified (AQ07). | Confirm the worksheets (AQ07). |
| CLR-AC-13 | Role permissions (BASAU 2.3.2; FR-AC-071) | A change of role permissions is an access request approved by a second user (MODIFY_ROLE_PERMISSIONS). | Four eyes on access changes (PQ17). | Confirm the approval (PQ17). |
| CLR-AC-14 | Return and bulk approval of requests (BASAU 2.4.1, 2.5.3, 2.6.x; FR-AC-072) | The approver returns an access request with remarks; the requester edits and resubmits it; My Approvals approves several items at once. Journals have their own bulk posting. | The BRD asks to return requests and to select several requests. | Confirm Return, Resubmit and bulk approval. |
| CLR-DS-01 | Request without a maintained payee (DIS 3.25.0; FR-DS-020) | By default the request waits as No Payee, a payee request is raised and the alert DISB_PAYEE_NO_MATCH is sent; it resumes when the payee is authorised. DISB_NO_PAYEE_ACTION = RETURN returns it at once, as the BRD says. | Waiting avoids the source sending the request again (AQ11, AQ12). | Choose HOLD or RETURN (AQ11, AQ12). |
| CLR-DS-02 | Delete payee details (DIS 2.2.4; FR-DS-013) | Only a draft payee never used is deleted; any other payee is deactivated with authorisation. | A payee used on a voucher must stay for audit (AQ11). | Confirm deactivation in place of deletion. |
| CLR-DS-03 | Editable proforma entry (DIS 2.7.6, 3.27.0; FR-DS-031) | Lines are editable until approval; edited lines are marked Edited and shown to the approver. | The BRD does not say who may edit and until when (AQ13). | Confirm the edit until approval (AQ13). |
| CLR-DS-04 | Check leaves and layout (DIS 2.8.1, 2.16.2; FR-DS-050) | A check takes the next leaf of the paying account's cheque book when printed; the layout is a draft template until BDOI gives it. | The check layout is not given (AQ14). | Give the check layout (AQ14). |
| CLR-DS-05 | DCTF file (DIS 2.16.1; FR-DS-061) | Header and 89-character details as Appendix B; no trailer or totals until BDOI gives them. | The trailer is not specified (AQ09). | Give the trailer and totals (AQ09). |
| CLR-DS-06 | Bank file uploads (DIS 2.22.0, 3.26.4, 3.26.7; FR-DS-053) | The uploads take a minimal CSV (DV or check number, amount, date) until the bank layouts are given. | The bank layouts are not given (AQ09). | Give the bank layouts (AQ09). |
| CLR-DS-07 | Cancel an approved DV (DIS 2.20.0; FR-DS-044) | The DV journal is reversed and the request goes back to its source; for a remittance DV, Remittance reverses the batch postings, gives the deductions back and returns the batch to review; it is sent again under a new cycle. | The BRD asks to regularise the source data without the steps (AQ15). | Confirm the regularisation (AQ15). |
| CLR-DS-08 | Account funding (DIS 2.17.x; FR-DS-064) | A BIBS workflow with four eyes; the BOB transaction itself is done in BOB and its reference recorded. | BOB is outside BIBS (AQ10). | Confirm the funding workflow (AQ10). |
| CLR-DS-09 | OR / AR tagging (DIS 2.10.x; FR-DS-056) | Every approved DV counts as unregularised until tagged. | Which DVs need an OR / AR back is open (AQ17). | Name the DV types that need an OR / AR (AQ17). |
| CLR-DS-10 | CWT tagging (DIS 2.11.x; FR-DS-057) | The tag records the certificate on the DV; the insurer certificates on commission are kept in one register in Tax (Certificates Received). | One register avoids keeping the same certificate twice (AQ16). | Confirm the register (AQ16). |
| CLR-DS-11 | One invoice number for related transactions (DIS 3.27.2, ACSL 2.16.0; FR-DS-093) | Endorsements and cancellations keep their own BIR invoice number and carry the root invoice number; booking sets the root invoice number and Invoice 360 shows the family. | Each BIR invoice needs its own number (AQ29). | Confirm the root invoice number (AQ29). |
| CLR-DS-12 | CPC2 incentive (DIS 3.29.2; FR-DS-090) | CPC2 is computed from the TSU CPC2 criteria (rate on the basic premium remitted, output VAT), deducted from the remittance and posted as income. | The base, VAT treatment, fixed-amount and rule criteria are open (AQ24, OQ39, PQ04). | Confirm the base, VAT and criteria (AQ24, OQ39, PQ04). |
| CLR-DS-13 | Early-incentive service invoice (DIS 3.29.1; FR-DS-091) | One service invoice per batch with 2% withholding; until AQ25 is answered the incentive is deducted in full from the remittance and the insurer's 2% is not posted. | The accounting of the insurer's 2% withholding is open (AQ25). | Give the accounting of the 2% withholding (AQ25). |
| CLR-DS-14 | CPC2 report (DIS 3.29.0; FR-DS-092) | A CPC2 report in Disbursement Reports lists the CPC2 lines of a period with the breakdown and totals that agree with the ledger. | The layout depends on the CPC2 definition (AQ24). | Give the CPC2 report layout (AQ24). |
| CLR-DS-15 | Insurer's payment advice (FR-DS-063) | The payment advice of the day is e-mailed to each payee; the remittance schedule is not attached to the insurer's advice. | The BRD does not ask for the schedule with the advice; the insurer receives it from Remittance. | Confirm, or ask for the schedule to be attached. |
| CLR-PQ-01 | Unapplied payment report for Marketing (MKT 1.7.0; FR-PQ-002) | Marketing runs the Cashiering reports (OPS_REPORT_VIEW) without a segment filter. | A Marketing report per segment needs a layout (AQ18). | Confirm the Cashiering reports, or give the layout of a Marketing report (AQ18). |
| CLR-PQ-02 | Attachments (MKT 1.12.0, 1.13.0; FR-PQ-007) | .txt is not an accepted attachment type; files open one at a time in the viewer. | The accepted types are those of the document store for all BIBS modules. | Confirm the attachment types and the viewer. |
| CLR-PQ-03 | Details of the forms (MKT 1.10.0; FR-PQ-003, 004, 017) | The fields of Appendix D are used; all accounts of one refund request belong to one client. | The BRD list of fields is empty (AQ18). | Give the mandatory fields (AQ18). |
| CLR-PQ-04 | Cancellation of a disbursed check (MKT 1.19.0, 1.16.3; FR-PQ-011) | After approval the request is handed to the Disbursement approvers (hand-off DV_CANCELLATION); the approver cancels the DV, which closes the hand-off. | The BRD routes the request to Disbursement without the step (AQ15). | Confirm the hand-off (AQ15). |
| CLR-PQ-05 | CA / SA on the client record (MKT 2.25.0; FR-PQ-016) | Recorded on approval without duplicates; the BDO account number is checked as 10 to 16 digits. | The account number format is not given (AQ19). | Give the account number format (AQ19). |
| CLR-PQ-06 | Cash-advance liquidation (Appendix D; FR-PQ-017) | Liquidation, return of the excess and posting of the expenses are proposed as in FR-PQ-017. | The scope of the liquidation form is to be confirmed (AQ18). | Confirm the scope of the liquidation (AQ18). |
| CLR-AS-01 | SOA files (ACSL 2.4.0; FR-AS-002) | The SOA file is read in one request (up to ACSL_SOA_MAX_ROWS rows) and reconciled once loaded; layouts per insurer are configuration. | The insurer SOA layouts are not given (AQ21). | Give the SOA layouts (AQ21). |
| CLR-AS-02 | Remittance deduction (ACSL 2.9.2; FR-AS-025) | The deduction stays Confirmed while batches consume it and is Applied when every batch that used it has the insurer OR. | The sources of deductions and deductions spanning batches are open (AQ23). | Give the sources and confirm the spanning rule (AQ23). |
| CLR-AS-03 | Aging and schedule reports (ACSL 2.14.3, 2.14.4; FR-AS-005) | The ACSL aging and schedule reports age the open items per party in eight slots, in PHP and USD, with the GL balance and the SL-GL difference. | The ageing slots and layouts are not given (OQ43). | Confirm the slots and layouts (OQ43). |
| CLR-AS-04 | GL-SL reconciliation at period end (ACSL 2.13.2; FR-AS-004) | The reconciliation runs nightly and on demand; a difference raises an alert but does not stop the period close. | The BRD includes a period-end check without saying whether it stops the close. | Confirm the alert, or ask for the close to be stopped. |
| CLR-AC-15 | Bank reconciliation by the GL team (FRBS 3.3.0; FR-AC-050) | The bank statements, the statement layouts and the bank reconciliation are opened with the bank reconciliation permission, held today by the Accountant and the Finance Manager roles; the GL roles do not hold it. | The FR names the GL Officer as the actor, and the role matrix is to be confirmed (AQ28). | Confirm who reconciles the bank accounts, and the grant of the bank reconciliation permission to the GL Officer and the GL Team Lead. |

# Appendix: Screen standards

The screens of this set follow the screen standards of BIBS. They are the same standards in every FRS and are listed here so that BDOI can agree the look of the screens once and check the screenshots against it; they are not repeated in each screen specification.

<!-- table: widths=4.2,13.4 caption="Screen standards of BIBS" -->
| Area | Standard |
|---|---|
| Record pages | Back arrow and breadcrumb, title, then the record header: name, reference chips, status pill, flags (for example Entry Edited on a disbursement voucher) and the key facts. Page actions on the right in one order: secondary, primary, and the destructive action last and apart. |
| Workflow header (step bar) | Every record with a workflow shows a stepper under the record header: the stages of the main path in their order, passed stages ticked in blue, the current stage highlighted, the stages ahead in grey with their number. A returned or on-hold stage shows in amber after the stage it came from; a rejected, cancelled or voided record shows that stage in red and the path ends there; the last stage turns green when it is reached. Under the stepper one row gives Current Stage, Since, Due (with the Overdue pill) and Assigned To, with the actions of the stage on the right. The history is a table (Stage, From Stage, Action, By, Date and Time, Remarks, Duration in Stage), newest first. |
| Messages (notice standard) | One standard for errors, warnings, information and success: a white notice with a thin bar and an icon in the colour of its kind (red, amber, blue, green), a bold short title, then the business message with one bullet per missing item. No codes, internal references or technical terms in what the user reads. Field errors appear under the field; a long form lists its errors at the top with a link to the first field. Only an unexpected system error offers Retry and a reference for support behind Details. Special instructions are one short information notice. |
| Tables, not highlight boxes | Records and lists of records are rows of a titled table (for example the lines of an accounting entry: Side, Account, Party, Cost Centre, Amount); key data is a label and value grid. A coloured box carries a short message only, never a record. |
| Tables | Header row in BDO blue that stays in view; rows alternate white and Background Blue, with a light hover and a stronger blue for the selected row; a list scrolls inside its card, the card as high as its rows and never higher than the window; text left, amounts right, dates in one format; one value per cell with at most one muted line under it; a dash for an empty value; the kind of a mixed list of records in a Type column; a due date on one line, in red with the warning icon once past due; ages and durations in words (for example 3 days); "Showing x to y of n results" with page numbers. A table fits the page: related values share a column (one under the other) rather than scrolling sideways. |
| Row actions | The actions on a record of a list are in one row action menu at the end of the row (the three-dot button), never buttons or links in the row; a destructive action is listed last, in red, and asks for confirmation. |
| Status labels | Statuses are outlined pills (a border and text in the colour of the state group on a light tint) of one size and never wrap. The full label is shown (for example For Approval, Sent for Payment); an agreed short form, with the full label in the tooltip, only for a label longer than 21 characters. |
| Labels and names | List values are shown by their label, never by their code; products by name with the code as a second line; insurers by name; users by their names, never by their user ID. |
| Dates, amounts and rates | One date picker; dates entered and shown as dd-MMM-yyyy, times as dd-MMM-yyyy HH:mm (Philippine time). Amounts with two decimals and thousand separators, negatives in brackets, the currency in the column header or before the amount. Rates as percentages with two to four decimals, as keyed; exchange rates with six decimals. |
| Period cell | A period (period of cover, a statement period, the fully paid period of a service-fee run, an effective period) is shown in lists on two lines, the start date and "to" the end date, each date kept whole; an open end reads "to open". In a sentence or a label and value grid it is one line: "01-Sep-2026 to 30-Sep-2026". |
| Forms | Labels above the fields, fields of one height on one line, required fields marked, the error under the field, and the actions in one place (Cancel, then the main action). Format hints only; other guidance is in the tooltip of the label. |
| Uploads | Download Template next to the upload; a drop zone with the accepted types and maximum size; every row checked before anything is saved; Rows Read, Valid and Rejected with the rows; valid rows are processed and the rejected rows are returned in the error file (the template layout with an Error column and the wrong cells highlighted); Upload Corrected File keeps the link to the first upload; a file uploaded before is refused. |
| Confirmations and reasons | Every approve, authorise, post, release, cancel, void or deactivate asks for confirmation in a dialog that names the record and the effect. Every reject, return, cancel and void needs a reason (from its list where there is one) before it can be confirmed. Destructive actions are confirmed with the red button. The reason is kept in the history and sent with the notification. |
| Notifications | The bell shows the unread count and opens the panel grouped by day (Today, Yesterday, then the date), each notice with its title, one-line summary, record reference and time; Mark Read, Mark All Read and View All; the Notifications page lists every notice with filters. |
| Documents | Generated documents carry the BDO Insure letterhead, the document name and reference, and a business footer with "Confidential" and page x of y. |

The project team checks each screen against these standards before UAT and records the result in the screen readiness checklist. A screen found not to follow a standard during the review is recorded as Change requested in the sign-off workbook and corrected before UAT without a change request. The presentation choices that BDOI is asked to confirm are items of the clarifications chapter.
