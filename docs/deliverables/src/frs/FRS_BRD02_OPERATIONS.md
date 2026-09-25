---
# Source of the Functional Requirements Specification for BRD-2 Operations.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD02_OPERATIONS.md
title: Operations
subtitle: BRD-2 Operations (Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation, Commission Receivables) and Addendum 1
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-02
name: Operations
doc_id: BIBS-FRS-BRD-02
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-2 Operations
output: FRS/BIBS_FRS_BRD-02_Operations_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-2 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Operations Head (pending)
    change: First issue for BDOI review; aligned with the as-built Operations modules and the cross-BRD decisions
distribution:
  - {name: "Head, Operations Services", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Cashiering Head Office and Branches", role: Business user, organisation: BDOI, purpose: Review of the Cashiering FRs}
  - {name: "Financial Transactions and Processing (Remittance)", role: Business user, organisation: BDOI, purpose: Review of the Remittance FRs}
  - {name: "Transaction Recon / Analysis and MIS", role: Business user, organisation: BDOI, purpose: Review of the Production Reconciliation FRs}
  - {name: "Commission Collection and Adjustments", role: Business user, organisation: BDOI, purpose: Review of the Adjustment and Commission Receivables FRs}
  - {name: "Retail, Corporate and Commercial Marketing", role: Business user, organisation: BDOI, purpose: Review of the Marketing Collection items}
  - {name: Comptrollership, role: Business user, organisation: BDOI, purpose: "Review of accounting events, BIR certificates and reports"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Operations business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and prepare user acceptance testing (UAT). The Operations modules are built, so this FRS describes the behaviour of the delivered screens and services. Where the built behaviour differs from the BRD text, the FR describes the built behaviour and records the difference in a note with its reference (section 1.6 lists them). Every FR cites the BRD requirement it meets and the BRD page.

## Scope

Operations takes over every invoice booked in New Business (BRD-1) and follows its money until the insurer is paid, the insurer's official receipt is recorded and every difference is closed. The FRS is organised by team.

<!-- table: widths=4.2,9.4,3.6 caption="Scope of this FRS" -->
| Area | In scope | BRD IDs |
|---|---|---|
| Access and invoice ledger | Log-in, role-based access, Operations home, the invoice ledger, Invoice 360, invoice locks, co-insurance shares, batch run reports, report access | BRQID.001-003, 006; RMTID.026, 032, 038, 040; ADJID.027; CSHID.017, 018 |
| Cashiering | AR and OR (create, cancel, reinstate), receipt series, payment files, PDC warehouse, check pick-up, matching and application by component, pre-booked payments, AR Insurance, commission ORs, unapplied dispositions, minimal balances, search, batch printing, BIR 2307, reports | CSHID.001-027 |
| Remittance | Extraction, eligibility rules, batches, exclusion (addendum), approval, schedule and payment request, insurer OR upload, early remittance incentive, special remittance processing, tracking, notifications, reports | RMTID.001-036, 038-040 |
| Adjustment / Cancellation | Financial, non-financial and internal endorsement requests; validation, approval, recompute per insurer (addendum), posting, excess and AR Insurer, slips, minimal balance file, over-adjustment control, reports | ADJID.001-028 |
| Production Reconciliation | Register extraction and sending, insurer feedback upload, matching with tolerance, buckets, feedback and disposition, unbooked accounts, early incentive validation, reports | PRCID.001-039 |
| Commission Receivables / Direct Payment | DP lists, validation and sanitation, billing per insurer, insurer answers and SLA, collection and PR reversal, incentive schemes, BIR certificates, estimated items, reports | CMRID.001-015; RMTID.037 |
| Marketing Collection items | Send schedule, hold requests, special remittance requests, BIR 2307 tagging, DP tagging, endorsement slip, DP PR reversal | MKTID.001-013 |
| Disbursement queue | Payment requests from Operations, DV number and status, BIR 2307 report | DBMID.001; RMTID.034 |
| Interfaces and flow-in | Ports to Collection, Disbursement, Marketing, Claims, insurers and shared drive; flow-in feeds with runs, records and alerts | BRQID.004, 005 |

**Out of scope for this phase:**

- The Collection system, the Disbursement system and the Accounting (GL mapping) specification. BRD-4 Collections and BRD-5 Accounting, Disbursement and Accounting Controls answer these questions and design the replacements; Operations keeps the seams described in section 7 (OQ01, OQ02, OQ07).
- External transports (SFTP, APIs, shared drive folders) to insurers, BDO bank channels and the Marketing and Claims systems. Files are uploaded and sent by e-mail in this phase (OQ17, OQ22, OQ29).
- The payout of incentives to branches. Operations computes and posts them and hands the pass-on to Disbursement (CMRID.006, OQ39).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Operations BRD: Cashiering, Remittance, Prod Recon, Adjustment / Cancellation, Collection of Commission Receivables (Direct Payment), pages 8-132 of the BRD-2 pack | v1.0, Jul-2025; signed Jul to Sep-2025 | `docs/source-documents/Operations.pdf` |
| R2 | Operations Addendum 1 (RMTID.002, ADJID.014), pages 1-7 of the BRD-2 pack | v1.0, 18-Dec-2025; signed Jan-2026 | same file |
| R3 | BDOI Operations (BRD-2) requirements baseline and fit/gap, open questions OQ01-OQ50 | current | `docs/requirements/BDOI_OPS_BRD_SPEC.md` |
| R4 | Operations requirements traceability (status, screens, endpoints, tests per BR ID) | current | `docs/requirements/BDOI_OPS_TRACEABILITY.md` |
| R5 | Operations build design | current | `docs/architecture/OPERATIONS_DESIGN.md` |
| R6 | Operations module guide (as built) | current | `docs/modules/OPERATIONS.md` |
| R7 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R8 | FRS BRD-1 New Business and FRS BRD-3 Product Maintenance (booking, catalogue, shared platform) | v1.0 | `docs/deliverables/out/FRS/` |

Page references in this document ("p.23") are pages of the BRD-2 PDF (R1, R2). The annex pages 121-132 are scanned; the report and endorsement slip lists of the annex are quoted from those pages.

## Definitions and acronyms

```glossary
AB: Account Broker
AO: Account Officer (Marketing)
AR: Acknowledgement Receipt, issued by Cashiering for premium collected on behalf of an insurer (in Prod Recon documents AR also means Accounts Receivable)
AR Insurer: Receivable from an insurer for premium already remitted and later reduced by a cancellation or decrease
AR Insurance: Non-premium payment of an insurer (refund, other expenses) received by Cashiering (CSHID.021)
ARN: Account Reference Number of a New Business account
ATP: BIR Authority to Print of a receipt series
BIR: Bureau of Internal Revenue
BOOK rate: Comptrollership exchange rate, 2 decimals, used by every Operations posting (CSHID.012)
BRD: Business Requirements Document
CLG: Consumer Lending Group
CLPC: Consumer Lending Processing Center
CWT: Creditable withholding tax; the 2% withheld by some clients on premium, evidenced by BIR Form 2307
Disposition: The decision on an unapplied payment (apply, refund, reclass, transfer, others)
DP: Direct Payment; the client pays the premium directly to the insurer and BDOI collects its commission from the insurer
DST: Documentary Stamp Tax
DTIP: Due To Insurer Provider; premium payable to the insurer
DV: Disbursement voucher number assigned by Disbursement
FR: Functional requirement of this document (FR-OP-nnn)
HO: Head Office
Invoice 360: The Operations view of one invoice with its components, movements, locks and the records of every Operations team
LGT: Local Government Tax
LOV: List of values maintained by the System Administrator
OQnn: Open question on BRD-2 (section 10.3)
OR: Official Receipt, a BIR receipt for BDOI income; issued by Head Office only
OTC: Over the counter
PDC: Post-dated check
PN: Promissory Note
PR: Premium receivable from the client, held by component (DST, premium tax / VAT, LGT, FST, other charges, basic premium)
PR2307: The 2% CWT portion of the premium receivable waiting for the client's BIR 2307
Pre-booked: A payment matched to an account that is not booked yet
Production register: The list of accounts BDOI booked with an insurer for a period, sent for reconciliation
UAT: User acceptance testing
Unbooked account: Insurer production that BDOI has not booked
VAT: Value Added Tax
WTAX: Withholding tax
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R3), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the System Administrator or the business owner in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check (for example a blank mandatory field); its message follows the same wording but has no business code. Text in angle brackets (`<invoice>`) is replaced by the value.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-2 test plan.

API paths start with `/api/v1`. In the header tables "..." stands for the module path given in the section introduction.

> [!NOTE]
> Values marked "default" (holding days, tolerances, SLA hours, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R3)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Works with the platform built for BRD-1 |
| CONFIGURE | Needs set-up only (parameters, templates, rules) |
| CHANGE | Extends or re-purposes an existing capability |
| NEW | A capability that did not exist before BRD-2 |

## Differences between the built behaviour and the BRD

The table lists where the delivered behaviour differs from the BRD text or fills a gap the BRD leaves open. Each difference is also a note in its FR. None of them removes a BRD requirement; most wait for an answer from BDOI and are configuration.

<!-- table: widths=2.2,5.6,6.6,2.4 caption="Recorded differences (built behaviour against the BRD)" size=8.5 -->
| BRD ID | BRD says | BIBS does | Ref. |
|---|---|---|---|
| BRQID.003 | Users customise or filter the Operations sections | Sections are filtered by role; personal pin or hide of sections is not built | FR-OP-003 |
| RMTID.040 | Lock while the invoice is "still with Comptrollership" | Remittance and Adjustment locks are built; no Comptrollership lock until BDOI names the activity | OQ28 |
| PRCID.009 | Receive and upload the insurer report automatically | The handler uploads the file; mailbox / SFTP pick-up is a parked seam | OQ29 |
| MKTID.001 | Marketing initiates the request to send the schedule | The Remittance processor sends the approved schedule from the batch, once | OQ22 |
| RMTID.002 | Main BRD: edit the extracted file online and push it with or without edits | The addendum governs: rows are excluded and restored with a reason; financial fields cannot be edited | Addendum p.4 |
| CSHID.001-005 | Cancellation and reinstatement without a stated approver | A cancellation or reinstatement is a request approved by a Cashiering TL / TH (four eyes) before it posts | OQ06 |
| CSHID.007 | One OR per remittance batch, or per payment unless consolidated | Remittance approval issues one commission OR per batch; the Collection upload issues one OR per insurer, certificate and payment | OQ49 |
| CSHID.008 | Files "encrypted and cannot be edited"; Trade, CLPC and Direct Credit run by IT (FS04) | Operations users upload the files; each file is stored read-only with its SHA-256 and a duplicate file is refused; layouts are configurable | OQ03, OQ04 |
| CSHID.016 / ADJID.026 | PR <= 10.00 reversed; file 10.00-100.00 written off (10.00 in both) | The sweep skips an invoice already written off by the file, so a balance is never reversed twice | OQ11 |
| RMTID.005 | Trigger extraction at end of day "if payment application is searched" | A user queues the searched invoice for the evening extraction run | OQ18 |
| RMTID.014 | Expected result: remit the DTIP balance; acceptance criteria: flag and exclude | Excluded by default (`EXCLUDE`); `CAP` remits the DTIP balance only | OQ19 |
| RMTID.016 | Compare insurer OR amount with paid PR | Exact comparison per invoice; no tolerance until BDOI gives one | OQ22 |
| RMTID.017 | Checks at least 3 banking days old and cleared | Counted in banking days from the last applied value date on the branch calendar; the cleared status comes from the payment application | OQ20 |
| RMTID.019 | Eight statuses that mix payment and remittance states | The invoice keeps a payment status and a remittance status; the batch has its own workflow stages | OQ21 |
| MKTID.009 | Claims condition validated | The claims condition is confirmed through the Claims feed when it is connected; until then the request carries a note | OQ46 |
| PRCID.002 | Only the Remarks column is editable | Remarks and Incentive columns and 200 blank rows are editable, so the insurer can add unbooked production | PRCID.022 |
| PRCID.014 / 021 | Filter by BDOI location | The items filter by AO, sales unit, segment and product line; the location filter is on the reports only | R4 |
| PRCID.030 | Four statuses | A fifth status BDOI_ONLY marks booked accounts the insurer did not return | R6 5.1 |
| Annex IV #5 | Sum Insured on the production register | The column is left out: the invoice ledger does not hold the sum insured | R6 5.7 |
| ADJID.001 | Multiple accounts in one transaction | One request per invoice; the wizard and the upload raise several requests at once | R6 4.1 |
| ADJID.003 | Marketing updates the account through a non-financial endorsement | Booking records the non-financial endorsement with its description; the account data are not changed | OQ32 |
| ADJID.008 | Prompt the AO to prepare a quotation | A hand-off to Marketing; the quotation number is linked on the request by reference | R6 4.1 |
| MKTID.012 | Auto-reverse the DP premium receivable | The ledger reversal is always recorded; its GL entry is off (`DP_PR_REVERSAL_POSTING`) because booking posts no PR for DP invoices | OQ07 |
| CMRID.005 / 006 | No Touch, Top Up and Motor Mania incentives | The scheme engine is built; the schemes stay inactive without tiers until BDOI gives targets and amounts | OQ39 |
| BRQID.004, CSHID.009, MKTID.010 / 013, CMRID.001, DBMID.001, RMTID.034 | Integration with Collection and Disbursement | Built as ports with uploads and an in-app Disbursement queue; BRD-4 and BRD-5 now design the replacements | OQ01, OQ02, OQ45 |

# Business context and process overview

## Business context

BDOI collects premium from clients on behalf of insurers and remits it net of its commission. It also earns commission, service fees, profit share and incentives. Today Operations issues receipts by hand, extracts remittances manually, matches insurer reports in spreadsheets, computes endorsements manually and gathers commission receivables by e-mail (p.11). The BRD asks for one system that integrates Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation and the Collection of Commission Receivables, with scheduled extractions, automatic matching, validated accounting entries, tracking and reports (p.10-13).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.11-13)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Manual issuance of receipts; payment details not visible; fragmented application | ARs and ORs numbered from controlled series; payments matched at acceptance and applied by component; unapplied money kept in a workbench |
| 2 | Manual extraction; no tracking of remittance batches | Scheduled and manual extraction with eligibility checks; batches with approval, Disbursement request and insurer OR |
| 3 | Manual matching of booked accounts with insurer reports | Register sent to the insurer; insurer feedback uploaded and matched within a 1.00 tolerance; differences followed to closure |
| 4 | Redundant steps, manual computation, limited traceability for endorsements | Endorsement requests recomputed per insurer, approved, posted in batches, linked to the original account |
| 5 | Dependency on IT for reports | Reports generated by the users, with view and export rights |
| 6 | Manual gathering, sanitation and billing of commission receivables | DP lists validated and billed per insurer; answers tracked against a 10-working-day SLA; commission collected with an OR |

## Process overview

Figure 1 shows the life of a booked invoice by team. Steps 1 to 8 follow the money of a paid invoice. Steps 9 to 12 run beside it: reconciliation of what was booked, changes to the invoice, direct payment accounts and minimal balances. Every step reads and writes the invoice ledger (FR-OP-004).

![Life of a booked invoice by team (BRD p.11-13)](figures/brd02_process_flow.dot)

<!-- table: widths=0.8,3.8,3.2,7.2,2.8 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Invoice booked | New Business | The booked invoice is copied into the invoice ledger with its components, DTIP, commission and flags | RMTID.038 |
| 2 | Payment received | Cashier | Payment by counter, bank file, PDC or check pick-up; an AR is issued from the branch series | CSHID.001, 008, 009 |
| 3 | Match and apply | System | Matched to a booked invoice and applied by component; pre-booked, excess and unmatched money becomes unapplied | CSHID.020, 022 |
| 4 | Unapplied disposition | Cashier, TL | Apply to another invoice, refund, reclass, transfer; approvals and reversals | CSHID.024, 025 |
| 5 | Remittance extraction | System, processor | Paid, cleared and eligible premium grouped into batches per insurer and type | RMTID.001-023 |
| 6 | Batch review and approval | Processor, TL | Exclusions only; submit; four-eyes approval posts the remittance and issues the commission OR | RMTID.002, 009-011 |
| 7 | Disbursement pays | Disbursement | Payment request, DV number; the invoice becomes fully or partially remitted | RMTID.034; DBMID.001 |
| 8 | Insurer OR | Processor | Insurer's OR schedule uploaded; exception report | RMTID.012, 013, 016 |
| 9 | Production reconciliation | Recon handler | Register to the insurer, feedback matched, differences followed | PRCID.001-039 |
| 10 | Adjustment / cancellation | Marketing, Adjustment | Endorsement requests; re-application of payments; AR Insurer after remittance | ADJID.001-028 |
| 11 | Direct payment commission | Commission handler | DP accounts billed to the insurer, collected with an OR, PR reversed | CMRID.001-015; MKTID.012 |
| 12 | Minimal balances | Cashier, Adjustment | Small balances reversed or written off | CSHID.016; ADJID.026 |

## Invoice statuses

The invoice ledger keeps two statuses per invoice and a set of flags. Every Operations screen shows them as status chips.

<!-- table: widths=2.6,6.4,7.6 caption="Payment status, remittance status and flags of an invoice" size=8.5 -->
| Item | Values | Set by |
|---|---|---|
| Payment status | UNPAID; PARTIALLY_PAID; PAID; NOT_APPLICABLE (direct payment and return invoices) | Derived from the premium receivable balances |
| Remittance status | UNPROCESSED; UNAPPLIED_PAYMENT; WITH_OUTSTANDING_BALANCE; REVIEW_IN_PROCESS; REQUESTED_FOR_HOLD; APPROVED; PARTIALLY_REMITTED; FULLY_REMITTED; NOT_APPLICABLE | Derived from the movements, and set by Remittance for the batch and hold states (RMTID.019) |
| Flags | HOLD; PENDING_NEG_ADJ; WRITTEN_OFF; CANCELLED; ESTIMATED | Remittance (hold), Adjustment (pending negative adjustment, cancelled, written off), Cashiering (written off by sweep), Commission (estimated) |
| Lock | Owner module and reason (for example REMITTANCE while in a batch, ADJUSTMENT while a request is open) | The owner module; released by the owner (RMTID.040) |

# Personas and roles

## Personas

<!-- table: widths=3.4,3.4,8,3 caption="Personas and BIBS roles" size=8.5 -->
| Persona (BRD annex) | BIBS role | Responsibilities in Operations | BRD |
|---|---|---|---|
| Cashier, HO (11) and branches (5) | CASHIER | Receives payments, issues ARs (branch) and ORs (HO), uploads payment files, requests cancellations and reinstatements, assigns dispositions, prints, validates BIR 2307 | CSHID.001-027 |
| Cashiering TL / TH | CASHIER_TL | Approves cancellations, reinstatements and dispositions; maintains receipt series | CSHID.001-006, 024 |
| Remittance Processor (4) | REMIT_PROCESSOR | Extracts, reviews and excludes, submits batches, uploads insurer ORs | RMTID.001-040 |
| Remittance TL / TH | REMIT_TL | Approves batches and special remittances; maintains incentive rules; assigns work | RMTID.009-011; MKTID.009 |
| Production Reconciliation Handler (4) | RECON_HANDLER | Extracts and sends registers, uploads insurer feedback, reconciles | PRCID.001-039 |
| Adjustment Processor (6) | ADJUSTMENT | Validates, returns and posts endorsement requests; uploads batches and minimal balance files | ADJID.001-028 |
| Adjustment TL | ADJUSTMENT_TL | Approves endorsement requests | ADJID.010 |
| Commission Handler / Processor | COMMREC_HANDLER | DP lists, validation, billing, answers, collection; BIR certificate submissions | CMRID.001-015 |
| Commission TL / TH | COMMREC_TL | Posts incentive runs; maintains incentive schemes | CMRID.003, 005, 006 |
| Marketing Collection (HO / branches) | MKT_COLLECTION | Hold and special remittance requests, BIR 2307 tagging, endorsement requests | MKTID.001-013 |
| Marketing TL / UH | MKT_TL | Approves holds; same requests as Marketing Collection | MKTID.006 |
| Comptrollership | COMPTROLLERSHIP | Acknowledges BIR certificates; views journals and reports | CMRID.015 |
| Disbursement | DISBURSEMENT | Works the Disbursement queue: acknowledge, DV number, paid, return, release 2307 | DBMID.001 |
| System Administrator | SYSADMIN | Interfaces (flow-in feeds), parameters and lists (section 9) | BRQID.004, 005 |
| Auditor | AUDITOR | Read access to Operations and reports | CSHID.011 |

The addendum states that user roles and authorisation "will be further defined in succeeding documentations" (p.4, footnote 2). The roles above are the project's proposal until BDOI confirms the matrix (OQ48).

Segregation of duties is enforced by the system, whatever the role grants: a receipt cancellation, reinstatement or disposition is never approved by its requester; a remittance batch is never approved by its submitter; a hold and a special remittance are never approved by their requester; an endorsement request is never approved by whoever raised, submitted or validated it.

<!-- pagebreak -->

## Permissions

<!-- table: widths=4.8,11.8 caption="Operations permissions" size=8.5 -->
| Permission | Allows |
|---|---|
| OPS_VIEW | Operations home, invoice search and Invoice 360 (read only) |
| OPS_REPORT_VIEW / OPS_REPORT_EXPORT | View Operations reports on screen / download and print them (CSHID.017, 018) |
| CASH_RECEIPT | Receive payments and issue ARs and ORs; Receipts, Check Pick-up |
| CASH_CANCEL / CASH_REINSTATE | Request the cancellation / reinstatement of a receipt |
| CASH_APPROVE | Approve cancellations and reinstatements; Cashiering Setup |
| CASH_APPLY | Pre-booked payments, re-match, manual application |
| CASH_UPLOAD | Payment files, PDC warehouse, commission ORs |
| CASH_DISPOSITION / CASH_DISPOSITION_APPROVE | Assign and submit dispositions / approve them and their reversals |
| CASH_SERIES_MANAGE | Receipt series master |
| CASH_PRINT | Batch printing of ARs and ORs |
| CWT_TAG / CWT_PROCESS | Marketing 2307 tagging / Cashiering 2307 validation and report |
| REMIT_EXTRACT, REMIT_PROCESS, REMIT_EXCLUDE, REMIT_OR_UPLOAD | Extraction, batch processing, exclusion, insurer OR upload |
| REMIT_APPROVE | Approve remittance batches; incentive rules |
| HOLD_REQUEST / HOLD_APPROVE | Create, extend, cancel and release holds / approve them |
| SPECIAL_REMIT_REQUEST / SPECIAL_REMIT_APPROVE | Request / approve special remittances |
| RECON_PROCESS / RECON_SEND | Reconcile, upload, extract / send registers to insurers |
| ADJ_REQUEST, ADJ_PROCESS, ADJ_APPROVE, ADJ_POST | Raise, validate, approve and post endorsement requests |
| COMMREC_PROCESS / COMMREC_APPROVE / INCENTIVE_MANAGE | Direct payment processing / post incentive runs / maintain incentive schemes and runs |
| BIR_CERT_SUBMIT / BIR_CERT_ACK | Submit BIR certificates / acknowledge or reject them (Comptrollership) |
| DISB_PROCESS | Disbursement queue |
| FLOWIN_MANAGE | Interfaces: feed schedules, uploads, replay of the ledger |

## Permissions matrix

The table below is the role-to-permission matrix delivered with the build (migration V760; "Y" = granted). Every Operations role also has WORK_VIEW, REPORT_VIEW, ATTACHMENT_VIEW, CLIENT_VIEW and ACCOUNT_VIEW. Handler and team leader roles have BULK_PROCESS and ATTACHMENT_MANAGE; team leaders have WORK_ASSIGN.

<!-- landscape -->

<!-- table: widths=4.6,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1 caption="Role-to-permission matrix for Operations (proposal until OQ48)" size=7.5 -->
| Permission | Cash-ier | Cash TL | Remit | Remit TL | Recon | Adj. | Adj. TL | Comm. Rec. | Comm. TL | Mkt Coll. | Mkt TL | Comp-trol. | Disb. | Sys Admin | Audi-tor |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| OPS_VIEW, OPS_REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| OPS_REPORT_EXPORT | Y | Y | Y | Y | Y | Y | Y | Y | Y | | | | | | |
| CASH_RECEIPT, CASH_CANCEL, CASH_REINSTATE, CASH_APPLY, CASH_UPLOAD, CASH_DISPOSITION, CASH_PRINT, CWT_PROCESS | Y | Y | | | | | | | | | | | | | |
| CASH_APPROVE, CASH_DISPOSITION_APPROVE, CASH_SERIES_MANAGE | | Y | | | | | | | | | | | | | |
| CWT_TAG, HOLD_REQUEST, SPECIAL_REMIT_REQUEST, ADJ_REQUEST | | | | | | | | | | Y | Y | | | | |
| HOLD_APPROVE | | | | | | | | | | | Y | | | | |
| REMIT_EXTRACT, REMIT_PROCESS, REMIT_EXCLUDE, REMIT_OR_UPLOAD | | | Y | Y | | | | | | | | | | | |
| REMIT_APPROVE, SPECIAL_REMIT_APPROVE | | | | Y | | | | | | | | | | | |
| RECON_PROCESS, RECON_SEND | | | | | Y | | | | | | | | | | |
| ADJ_PROCESS, ADJ_POST | | | | | | Y | Y | | | | | | | | |
| ADJ_APPROVE | | | | | | | Y | | | | | | | | |
| COMMREC_PROCESS, BIR_CERT_SUBMIT | | | | | | | | Y | Y | | | | | | |
| COMMREC_APPROVE, INCENTIVE_MANAGE | | | | | | | | | Y | | | | | | |
| BIR_CERT_ACK, JOURNAL_VIEW | | | | | | | | | | | | Y | | | |
| DISB_PROCESS | | | | | | | | | | | | | Y | | |
| FLOWIN_MANAGE | | | | | | | | | | | | | | Y | |

<!-- portrait -->


# Functional requirements

## Access, invoice ledger and Invoice 360

The foundation module `opsledger` serves every team: access, the Operations home, the invoice ledger and its views, locks, co-insurance shares, batch run reports and report access. API paths of this section are under `/api/v1/ops`.

```fr
id: FR-OP-001
title: Log in to BIBS
brd: [BRQID.001 (p.16)]
actor: Operations users
priority: Must have
fit: FIT
screens: Login
api: POST /api/v1/auth/login
description: Operations users log in with their BIBS user ID and password. Operations uses the platform log-in built for BRD-1; it adds no screen of its own.
preconditions:
  - The user has an active BIBS account with at least one Operations role (section 3).
main_flow:
  - The user enters the user ID and password on the Login screen.
  - BIBS checks the credentials and the account status.
  - BIBS opens the home page with the menu of the user's roles.
alternate_flows:
  - Wrong credentials. BIBS refuses the log-in and counts the failed attempt.
  - Locked account. After the configured number of failed attempts the account locks; the System Administrator unlocks it.
  - Idle session. The session ends after the configured idle time; the user logs in again.
rules:
  - [R1, "Log-in, lock-out and session rules follow the session policy of BRD-1 (BRNB.040).", Configurable, Session policy (System Administrator)]
  - [R2, "BDO single sign-on / Active Directory is not part of this phase (BRD-1 Q42).", Fixed, "-"]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - Every successful and failed log-in is recorded with user, time and source address.
acceptance:
  - A user with valid credentials and an Operations role logs in and sees the Operations menu sections of the role.
  - A user with wrong credentials is refused and no session is created.
  - The account locks after the configured number of failed attempts.
```

```fr
id: FR-OP-002
title: Restrict each action to authorised roles
brd: [BRQID.002 (p.16)]
actor: System
priority: Must have
fit: FIT
screens: All Operations screens
api: Every endpoint checks its permission
description:
  - Every Operations screen, button and API call requires a permission (section 3.2). Menus show only the screens that the user's roles allow; buttons for actions the user may not perform are hidden.
  - The Cashiering, Remittance and Commission Receivables screens are in the Finance group of the menu; Adjustment and Product Reconciliation are in Client & Policy; the Operations home, invoice search, Invoice 360, Disbursement queue, hand-offs, interfaces and report archive are in the Operations group.
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a screen or starts an action.
  - BIBS checks the user's permissions for that screen or action.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link returns "You do not have access to this page". An API call is refused (HTTP 403) and logged.
  - Segregation of duties. An approval refused by a four-eyes rule is refused with its message even when the role has the permission (section 3.3).
rules:
  - [R1, Roles are granted permissions as in section 3.3 until BDOI confirms the matrix (OQ48)., Configurable, Role-permission change request (BRD-11)]
  - [R2, "Four-eyes rules (a requester never approves) cannot be switched off.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - Refused API calls are logged with user, endpoint and time.
acceptance:
  - A Remittance Processor sees the Remittance screens and not the Cashiering Setup or the Disbursement queue.
  - A direct call to an approval endpoint by a user without the approval permission is refused and logged.
  - A Marketing Collection user sees Remittance Holds, Special Remittance and BIR 2307 tagging but cannot approve a hold.
```

```fr
id: FR-OP-003
title: Present the Operations home by role
brd: [BRQID.003 (p.17)]
actor: Operations users
priority: Must have
fit: CHANGE
screens: Operations Home; Cashiering, Remittance, Adjustment, Reconciliation and Commission workbenches
api: GET /api/v1/ops/home
description:
  - After log-in the Operations Home shows one card per team section the user works in (Cashiering, Remittance, Production Reconciliation, Adjustment, Commission Receivables, Disbursement, Interfaces). Each card shows live work tiles with counts and opens the team's workbench. Sections that the user's roles do not cover are not shown.
  - The home also has an invoice search box and a list of links to the applications Operations works with, maintained in the list OPS_EXTERNAL_LINK.
  - "Examples of tiles: Unapplied Payments; Cancellations and Reinstatements for Approval; Dispositions for Approval; Batches in Review; Batches for Approval; Awaiting Insurer OR; Holds for Approval; Special Remittances for Approval; Endorsements for Validation, Approval and Posting; Returned to Requester; Registers to Send; Awaiting Insurer Feedback; Cycles Reconciling; Unbooked Insurer Production; DP Accounts to Confirm; Billings Awaiting Insurer; Insurer Feedback Overdue; BIR Certificates to Acknowledge; Payment Requests to Process; Locked Invoices; Pending Negative Adjustments."
preconditions:
  - The user has OPS_VIEW.
main_flow:
  - The user logs in and opens Operations Home.
  - BIBS collects the counts of each team the user's roles cover and shows one card per team.
  - The user clicks a tile; BIBS opens the team screen filtered on that queue.
  - The user switches between sections through the cards or the menu.
alternate_flows:
  - No Operations section for the role. The home shows "No Operations section is assigned to your role".
rules:
  - [R1, "A card is shown only for the teams whose permissions the user holds.", Fixed, "-"]
  - [R2, "External application links are list values labelled Name|https://url.", Configurable, LOV OPS_EXTERNAL_LINK]
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - A Cashier sees the Cashiering card with its tiles immediately after log-in and does not see the Remittance card.
  - Clicking "Batches for Approval" opens Remittance Batches filtered on the For Approval stage.
  - The home loads within 5 seconds (section 8).
```

> [!NOTE] Difference from the BRD
> BRQID.003 AC3 asks that users can customise or filter the sections. BIBS filters the sections by role; a personal pin or hide of sections is not built. The request can be raised as a change if BDOI needs it.

```fr
id: FR-OP-004
title: Keep the invoice ledger of every booked invoice
brd: [RMTID.038 (p.68-69), MKTID.011 (p.75)]
actor: System
priority: Must have
fit: NEW
screens: Invoice Search; Invoice 360 (Movements); Interfaces (replay)
api: GET /api/v1/ops/invoices; GET .../invoices/{no}/movements; POST .../invoices/replay
description:
  - Each invoice booked in New Business, each endorsement invoice and each return invoice is copied into the Operations invoice ledger after the booking commits. The ledger line holds the ARN, invoice, policy, PN numbers, client, assured and payor, lead insurer and insurer shares, currency, booking, inception and expiry dates, risk code, product line, segment, AO and unit, the direct payment and 2% CWT flags, and the amounts per component.
  - "Components: premium receivable (PR) by BASIC, DST, PREMIUM_TAX_VAT, LGT, FST and OTHER; DTIP; COMMISSION; COMMISSION_VAT; WTAX; PR2307. The balance of a component is booked + adjusted - applied + reversed - remitted - written off."
  - Every Operations movement on the invoice is recorded as a ledger movement (BOOKED, APPLIED, UNAPPLIED, REMITTED, ADJUSTED, WRITE_OFF, DP_REVERSAL, CWT_RECLASS, MIN_BAL) with the module, source reference, AR / OR / batch number, value date and journal batch. The movement list is the payment and remittance history of the invoice.
  - The direct payment tag set in New Business at quotation or policy (BRNB.114) is copied to the ledger line, so Cashiering, Remittance and Commission Receivables treat the invoice as direct payment (MKTID.011).
preconditions:
  - The invoice is booked in New Business.
main_flow:
  - New Business books an invoice.
  - After the booking commits, BIBS copies it into the ledger with its components and BOOKED movements, payment status UNPAID and remittance status WITH_OUTSTANDING_BALANCE.
  - Each later Operations action posts its movement and updates the balances and statuses in the same transaction.
alternate_flows:
  - The copy fails. The booking is kept; the failure is a failed record of flow-in feed OPS_INVOICE_FEED and raises the alert OPS_FLOW_IN_FAILED. An administrator replays the invoice, account or company from Interfaces (job OPS_INVOICE_FEED_REPLAY).
  - A direct payment invoice carries its PR and DTIP for information with payment and remittance status NOT_APPLICABLE; booking posts no PR for it.
rules:
  - [R1, "BOOKED movements come only from the booking feed.", Fixed, "-"]
  - [R2, "A movement is unique on module, source reference, invoice, component and type; posting the same source twice has no effect.", Fixed, "-"]
  - [R3, "Payment status is derived from the PR balances (UNPAID, PARTIALLY_PAID, PAID).", Fixed, "-"]
  - [R4, "Keys (ARN, invoice, client, insurer) are stored as values, so the ledger can be replayed from booking at any time.", Fixed, "-"]
validations:
  - [Movement without an amount, A movement needs at least one amount other than zero, MOVEMENT_WITHOUT_AMOUNT]
  - [BOOKED movement from a module, BOOKED movements come from the booking feed only, MOVEMENT_TYPE_RESERVED]
  - [Replay without scope, "Give an invoice number, an ARN or a company to replay", REPLAY_SCOPE]
notifications:
  - OPS_FLOW_IN_FAILED to the System Administrator when a copy fails.
audit:
  - Movements and status changes are append-only; each status change keeps the module and time.
acceptance:
  - A booked invoice appears in Invoice Search with its components equal to the booking, status UNPAID and WITH_OUTSTANDING_BALANCE.
  - A payment applied to the invoice shows as an APPLIED movement with the AR number, date and amount per component.
  - A direct payment account booked in New Business shows the DP chip and payment status NOT_APPLICABLE.
  - Replaying an invoice already in the ledger changes nothing.
```

```fr
id: FR-OP-005
title: View an invoice in Invoice 360
brd: [RMTID.026 (p.63), RMTID.032 (p.66), ADJID.024 (p.100)]
actor: All Operations users (OPS_VIEW)
priority: Must have
fit: NEW
screens: Invoice Search; Invoice 360
api: GET /api/v1/ops/invoices; GET .../invoices/{no}; GET .../invoices/{no}/family; GET .../invoices/{no}/history; GET .../accounts/{arn}/invoices
description:
  - Invoice Search finds invoices by invoice number, ARN, policy, client or assured, with filters on insurer and booking dates. The search is partial and not case-sensitive and suggests matches while the user types an ARN.
  - "Invoice 360 shows the header with reference chips (ARN, invoice, endorsement) and the status pills: payment status, remittance status, hold, lock, direct payment, written off, pending negative adjustment. Below it: the component table (booked, applied, remitted, adjusted, balance), the movement timeline, the family of the invoice (original, endorsements, return invoices) and one tab per team: Receipts, Remittances, Adjustments, Reconciliation, Commission; then Documents and History."
  - The hold status and the remittance status of the account are visible with their history and time stamps (RMTID.032).
preconditions:
  - The user has OPS_VIEW.
main_flow:
  - The user searches by ARN, invoice or policy number.
  - The user opens an invoice from the results.
  - BIBS shows Invoice 360 with the current balances, statuses and the records of every team.
alternate_flows:
  - Nothing found. The list shows an empty state; the user changes the criteria.
rules:
  - [R1, "Each team tab lists only the records of that team with their count; an empty tab says so.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - Searching ARN-2026-940001 lists its booking and endorsement invoices; opening one shows its receipts, remittance batch and insurer OR.
  - An invoice on hold shows the hold pill and the hold request in the Remittances tab with its dates.
  - A partial search on the assured name returns the matching invoices regardless of case.
```

```fr
id: FR-OP-006
title: Lock invoices while a team works on them
brd: [RMTID.040 (p.69-70)]
actor: System; Remittance, Adjustment
priority: Must have
fit: NEW
screens: Invoice 360 (lock pill); Remittance Batch; Adjustment request page
api: Ledger service (internal)
description:
  - A module locks an invoice while it works on it, with the owner module and a reason. Remittance locks each invoice in a batch until the DV number is received or the line is excluded or returned; Adjustment locks the invoice while a request is open. Other modules may still apply payments to a locked invoice; any other action is refused with INVOICE_LOCKED.
  - Status chips on every Operations screen show the lock, "In Remittance", "Pending Adjustment" and hold, so the other teams see why an invoice cannot be changed.
preconditions:
  - "None."
main_flow:
  - A team starts work on an invoice (extraction into a batch, an endorsement request).
  - BIBS locks the invoice with the owner and reason and notifies the users working on it (OPS_INVOICE_LOCKED).
  - Another team tries an action on the invoice; BIBS refuses it and names the owner.
  - The owner finishes; BIBS releases the lock.
alternate_flows:
  - Payment application. Cashiering applies a payment to a locked invoice; the application is allowed.
rules:
  - [R1, "While locked, other modules may post only APPLIED movements.", Fixed, "-"]
  - [R2, "Only the owner module releases its lock.", Fixed, "-"]
  - [R3, "Lock reasons are free text until BDOI lists them (OQ28).", Configurable, "-"]
validations:
  - [Action on a locked invoice, "Invoice <invoice> is locked by <owner> (<reason>)", INVOICE_LOCKED]
notifications:
  - OPS_INVOICE_LOCKED when an invoice a user works on is locked or released.
audit:
  - Lock and release are recorded as status changes with owner, reason and time.
acceptance:
  - An endorsement request on an invoice that is in a remittance batch is refused with INVOICE_LOCKED naming REMITTANCE.
  - A payment applied to an invoice locked by Adjustment is accepted.
  - Excluding the invoice from the batch releases the lock.
```

> [!NOTE] Difference from the BRD
> RMTID.040 also names a lock "while still with Comptrollership". BDOI has not listed which Comptrollership activity holds an invoice (OQ28); no Comptrollership lock is set in this phase.

```fr
id: FR-OP-007
title: Split invoice amounts by insurer share (co-insurance)
brd: [ADJID.027 (p.102-103)]
actor: System
priority: Must have
fit: NEW
screens: Invoice 360 (header, shares); Adjustment request page (Recompute)
api: GET /api/v1/ops/invoices/{no}; GET /api/v1/adjustment/requests/{id}/recompute
description:
  - A co-insured invoice stays one invoice with its insurer shares (insurer, share percent, lead). Payment is allocated in Cashiering on the invoice; adjustments are split by the configured shares and logged per insurer (FR-OP-054).
preconditions:
  - The invoice was booked with insurer shares.
main_flow:
  - BIBS copies the shares from the booking to the ledger line.
  - An adjustment of the invoice is recomputed per insurer share and stored per insurer on the request.
  - The posting records the change per insurer.
rules:
  - [R1, "Adjustments follow the original shares of the invoice; they are never allocated 100% to the lead insurer when shares exist.", Fixed, "-"]
  - [R2, "The commission party of a direct payment account is the lead insurer.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - The change per insurer is kept on the request (adj_request_share).
acceptance:
  - A 60/40 co-insured invoice shows both insurers on Invoice 360.
  - A premium decrease of 1,000.00 on it is split 600.00 / 400.00 on the request's insurer breakdown.
```

> [!NOTE] Open point
> Whether remittance is extracted and scheduled per insurer share for co-insured invoices is open (OQ50). Remittance batches today are per lead insurer.

```fr
id: FR-OP-008
title: Continue batch jobs past failed records and report the run
brd: [BRQID.006 (p.19-20)]
actor: System; users who upload files
priority: Must have
fit: CHANGE
screens: Bulk Upload wizard (run summary); Payment Uploads; Interfaces (runs, records); Report CSH-BATCH-RUN
api: /api/v1/bulk; /api/v1/ops/flow-in/runs, .../runs/{id}/records
description:
  - Every upload and scheduled job processes each record in its own transaction. A failed record is stored with its error and the run continues. At the end the run shows counts per outcome and lets the user download the report and reprocess the failed rows.
  - Payment files report the categories applied, unapplied, pre-booked, excess and failed; other handlers report their own outcomes (for example WRITE_OFF / CREDIT for the minimal balance file).
preconditions:
  - "The user has the permission of the upload (for example CASH_UPLOAD)."
main_flow:
  - The user uploads a file, or a job starts.
  - BIBS validates and processes each record; valid records are committed.
  - BIBS shows the run summary with one tile per outcome and the list of failed records with their messages.
  - The user corrects and reprocesses the failed rows.
alternate_flows:
  - Duplicate file. A file identical to an earlier upload of the same handler is refused (BULK_DUPLICATE_FILE).
rules:
  - [R1, "A failed record never stops the run.", Fixed, "-"]
  - [R2, "Outcome categories are defined per handler.", Fixed, "-"]
validations:
  - [Same file uploaded again, This file was already uploaded, BULK_DUPLICATE_FILE]
notifications:
  - OPS_FLOW_IN_FAILED when a feed run has failed records.
audit:
  - The run, its counts and every record outcome are kept (bulk job and rows, flow-in runs and records).
acceptance:
  - A payment file with 10 rows, one with an unknown invoice, commits 9 rows and reports 1 unapplied or failed row with its reason.
  - The run report CSH-BATCH-RUN lists successful, unsuccessful, applied, unapplied and failed records.
  - Reprocessing a corrected failed row applies it without reprocessing the others.
```

```fr
id: FR-OP-009
title: Generate, view and export Operations reports
brd: [CSHID.017 (p.38), CSHID.018 (p.38)]
actor: Operations users
priority: Must have
fit: "FIT (CSHID.017), CHANGE (CSHID.018)"
screens: Reports (Report Centre); Report Archive
api: /api/v1/reports
description:
  - Operations reports are in the report category Operations. A user with OPS_REPORT_VIEW generates a report with its parameters and views it on screen with its metadata (name, parameters, date, creator). Downloading (PDF, XLSX, CSV) and printing need OPS_REPORT_EXPORT.
  - Every run of an archived report is kept in the Report Archive with its parameters, creator, time and file.
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user opens Reports, chooses an Operations report and enters the parameters.
  - BIBS generates the report and shows it.
  - A user with OPS_REPORT_EXPORT downloads or prints it.
alternate_flows:
  - View-only user. The download and print buttons are not shown; a direct download is refused.
rules:
  - [R1, "View and export are separate permissions per report.", Configurable, Role grants]
validations:
  - [Download without export permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - Each generation and download is recorded in the report archive with user and time.
acceptance:
  - An Auditor views CSH-APPLIED-PREM on screen but cannot download it.
  - A Cashier downloads the same report as Excel.
  - The Report Archive lists the run with its parameters and creator.
```

## Cashiering

Cashiering receives premium and non-premium payments, issues ARs and Head Office ORs, applies payments to the invoice ledger by component, keeps unapplied payments with their dispositions and runs the BIR 2307 flow. API paths of this section are under `/api/v1/cashiering`.

```fr
id: FR-OP-010
title: Maintain AR and OR series per branch
brd: [CSHID.006 (p.28-29), CSHID.015 (p.37)]
actor: Cashiering TL / TH (maintain); System (numbering)
priority: Must have
fit: "CHANGE (CSHID.006), NEW (CSHID.015)"
screens: Receipt Series
api: GET / POST .../series; PUT .../series/{id}; POST .../series/{id}/authorize, /deactivate
description:
  - A receipt series is kept per kind (AR or OR) and branch, with prefix, first and last number, BIR ATP number and the warning level. ARs are numbered from the series of the branch of the transaction; ORs only from Head Office series. A number is taken when the receipt is posted, under a row lock, so two receipts never get the same number.
  - Before each receipt BIBS checks that the series has numbers left; a depleted series stops the transaction. When the remaining numbers reach the warning level, BIBS raises the alert RECEIPT_SERIES_LOW. The Receipt Series screen shows a remaining-count gauge.
preconditions:
  - The user has CASH_SERIES_MANAGE.
main_flow:
  - The TL adds a series with kind, branch, prefix, range, ATP number and warning level.
  - Another user with CASH_SERIES_MANAGE authorises it; it becomes usable.
  - Each AR or OR takes the next number of the usable series.
alternate_flows:
  - The TL extends the last number of a series; it cannot go below the numbers already issued.
  - The TL deactivates a series; the next usable series of the branch is used.
rules:
  - [R1, "OR series belong to Head Office only.", Fixed, "-"]
  - [R2, "Series are maker-checker.", Fixed, "-"]
  - [R3, "Series format and ATP ranges follow BDOI's BIR registration (OQ05).", Configurable, Receipt Series]
validations:
  - [OR series for a branch other than HO, Official receipt series belong to Head Office only (CSHID.006), OR_HEAD_OFFICE_ONLY]
  - [Last number below first, The last number must not be below the first number, RECEIPT_SERIES_RANGE]
  - [Last number below issued numbers, The last number cannot be below the numbers already issued, RECEIPT_SERIES_RANGE]
  - [No series left, "No authorized <kind> series with numbers left for this branch (CSHID.015)", RECEIPT_SERIES_DEPLETED]
  - [Maker authorises own series, The requester cannot approve, MAKER_CHECKER_VIOLATION]
fields_screen: Receipt Series
fields:
  - [Kind, List, "Yes", AR / OR, OR only for Head Office]
  - [Branch, List, "Yes", Branches, Branch of the company]
  - [Prefix, Text, "Yes", "-", "For example AR-HO-, AR-CEB-, OR-HO-"]
  - [From No. / To No., Number, "Yes", "-", To >= From]
  - [BIR ATP No., Text, "No", "-", "-"]
  - [Warn When Remaining At, Number, "No", "-", ">= 0"]
notifications:
  - RECEIPT_SERIES_LOW alert to the Cashiering TL when a series reaches its warning level.
audit:
  - Series changes and authorisations are audited with before and after values.
acceptance:
  - A branch AR takes the next number of the branch series; an OR takes the next Head Office OR number.
  - An OR from a branch series is refused with OR_HEAD_OFFICE_ONLY.
  - When the last number is used, the next receipt is refused with RECEIPT_SERIES_DEPLETED.
  - No two receipts carry the same number.
```

```fr
id: FR-OP-011
title: Receive a payment and issue an Acknowledgement Receipt
brd: [CSHID.001 (p.23-24)]
actor: Cashier
priority: Must have
fit: CHANGE
screens: Receive Payment; Receipts; Receipt page
api: POST .../payments/preview; POST .../payments; POST .../receipts/ar; GET .../receipts/{id}/pdf
description:
  - The cashier records a premium payment (Bills Payment, OTC, Trade, CLPC, PDC, Direct Credit) or a non-premium payment (refund, other expenses, AR Insurance). BIBS issues an AR numbered from the branch series, in the currency received, converted at the BOOK rate.
  - "The Receive Payment screen has two panes: on the left the payor and the ARN, invoice, policy or PN look-up and the tender; on the right a live preview of the match and of the application by component (DST, VAT, LGT, FST, other, basic), the 98% badge for 2% CWT clients, the excess that will stay unapplied and the BOOK rate."
  - Each payment is matched and applied at once (FR-OP-018, FR-OP-019). Uploaded files, matured PDCs and check pick-ups issue their ARs the same way (FR-OP-015 to FR-OP-017).
preconditions:
  - The user has CASH_RECEIPT; the branch has a usable AR series.
main_flow:
  - The cashier opens Receive Payment and enters the payor, reference, amount, currency, mode and check details.
  - BIBS shows the preview of what the payment will match and apply.
  - The cashier saves. BIBS issues the AR, posts OPS_AR_RECEIPT, applies the payment and shows the receipt page.
  - The cashier prints the AR.
alternate_flows:
  - Non-premium payment of an insurer (AR class AR_INSURANCE). See FR-OP-020.
  - No match. The AR is issued and the money becomes an unapplied item (FR-OP-022).
rules:
  - [R1, "AR classes and channels are the list AR_CLASS (premium: Bills Payment, OTC, Trade, CLPC, PDC, Direct Credit; non-premium: Refund, Other expenses, AR Insurance).", Configurable, LOV AR_CLASS]
  - [R2, "A receipt is in the currency received and posted at the BOOK rate with 2 decimals (CSHID.012).", Fixed, "-"]
  - [R3, "The AR number is taken from the series of the branch of the user at posting.", Fixed, "-"]
validations:
  - [Amount zero or negative, A receipt needs an amount above zero, RECEIPT_AMOUNT]
  - [No AR series left, "No authorized AR series with numbers left for this branch (CSHID.015)", RECEIPT_SERIES_DEPLETED]
  - [Mandatory field blank, "<Field> is required", "-"]
fields_screen: Receive Payment
fields:
  - [AR Class, List, "Yes", LOV AR_CLASS, "-"]
  - ["ARN, Invoice, Policy or PN No.", Look-up, "No", Invoice ledger / accounts, Used for matching]
  - [Payor / Client Code, Look-up, "No", Client master, "-"]
  - [Payor Name, Text, "Yes", "-", "-"]
  - [Assured Name, Text, "No", "-", "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Currency, List, "Yes", Currencies, BOOK rate needed when not PHP]
  - [Mode of Payment, List, "Yes", "Cash, check, bills payment, trade, CLPC, PDC, direct credit, ADA, credit to account", "-"]
  - [Check No. / Bank, Text / List, Cond., Banks, Required for a check]
  - [Payment Date, Date, "Yes", "-", Not in the future]
notifications:
  - "None."
audit:
  - Receipt creation is audited with the AR number, user and time (CSHID.011).
acceptance:
  - An OTC payment on a booked invoice issues an AR with the next branch number and applies the payment; the invoice becomes PAID when fully covered.
  - A USD payment shows the BOOK rate and posts at that rate.
  - The AR prints with the number, payor, amount and invoice.
```

```fr
id: FR-OP-012
title: Issue an Official Receipt for BDOI income
brd: [CSHID.002 (p.24-25)]
actor: Cashier (Head Office)
priority: Must have
fit: CHANGE
screens: Receipts (Issue Official Receipt); Receipt page
api: POST .../receipts/or
description:
  - Head Office issues ORs for Service Fee Income (risk management fee, consultancy fee), Insurance Profit Share, Commissions (with VAT and withholding tax), Incentives and Others (discount). An OR has lines with gross amount, VAT and withholding tax, and the 2307 certificate reference when tax is withheld. Posting OPS_OR_ISSUE credits the income account of the OR type and output VAT.
  - Commission ORs for remittance batches and direct payment collections are issued by the system (FR-OP-021, FR-OP-035, FR-OP-094).
preconditions:
  - The user has CASH_RECEIPT at Head Office; a Head Office OR series is usable.
main_flow:
  - The cashier clicks **Issue Official Receipt** and chooses the OR type.
  - The cashier enters the payor, currency, mode, amounts and description.
  - BIBS checks the Head Office rule and the series, issues the OR and posts it.
alternate_flows:
  - Issued from a branch. BIBS refuses the OR.
rules:
  - [R1, "OR types: Service Fee, Profit Share, Commission, Incentive, Others.", Configurable, LOV OR_TYPE]
  - [R2, "ORs are issued by Head Office only.", Fixed, "-"]
validations:
  - [OR from a branch, Official receipts are issued by Head Office only (CSHID.006), OR_HEAD_OFFICE_ONLY]
  - [No line, An official receipt needs a line, OR_WITHOUT_LINES]
  - [Company without Head Office, The company has no Head Office branch for official receipts, NO_HEAD_OFFICE]
  - [Amount zero, A receipt needs an amount above zero, RECEIPT_AMOUNT]
fields_screen: Issue Official Receipt
fields:
  - [OR Type, List, "Yes", LOV OR_TYPE, "-"]
  - [Payor Name / Payor Code, Text / Look-up, "Yes", Client or insurer master, "-"]
  - [Currency, List, "Yes", Currencies, "-"]
  - [Mode of Payment / Check No., List / Text, "Yes", "-", Check No. for checks]
  - [Gross Amount, Amount, "Yes", "-", "> 0"]
  - [VAT, Amount, "No", "-", ">= 0"]
  - [Withholding Tax, Amount, "No", "-", ">= 0"]
  - [2307 Certificate Ref., Text, "No", "-", "-"]
  - [Description, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - OR issuance is audited with OR number, user and time.
acceptance:
  - A service fee OR of 11,200.00 with 1,200.00 VAT posts the service fee income and output VAT.
  - A branch cashier cannot issue an OR.
```

```fr
id: FR-OP-013
title: Cancel an AR or OR
brd: [CSHID.001 (p.23-24), CSHID.002 (p.24-25), CSHID.003 (p.25-26), CSHID.012 (p.35)]
actor: Cashier (request); Cashiering TL / TH (approve)
priority: Must have
fit: CHANGE
screens: Receipt page (Cancel); Receipts (Cancellations and Reinstatements tab)
api: POST .../receipts/{id}/cancel; GET .../receipt-actions; POST .../receipt-actions/{id}/approve, /resubmit
description:
  - The cashier requests the cancellation of an AR or OR with a reason from the list RECEIPT_CANCEL_REASON. The request gets its own transaction number CAN-yyyy-n and goes to a Cashiering TL / TH for approval (workflow OPS_RECEIPT_ACTION, section 5).
  - On approval BIBS reverses every active application of the receipt (the invoices return to outstanding), re-posts the receipt events with negative amounts at the original BOOK rate, closes the unapplied balance of the receipt and marks it CANCELLED.
preconditions:
  - The user has CASH_CANCEL; the receipt is ISSUED or REINSTATED and has no request waiting.
main_flow:
  - The cashier opens the receipt and clicks **Cancel**.
  - The cashier chooses the reason group and reason and writes remarks ("Others" needs the text).
  - The cashier submits; the request moves to For approval and the TLs are notified.
  - The TL clicks **Approve and Post**. BIBS posts the reversal and cancels the receipt.
alternate_flows:
  - The TL returns the request with a reason; the cashier corrects and resubmits or withdraws it.
  - An unapplied item of the receipt already has a disposition in process. The cancellation is refused until the disposition is withdrawn.
rules:
  - [R1, "Reason groups PREMIUM, COMMISSION, PDC and GENERAL with the BRD values (section 9.2).", Configurable, LOV RECEIPT_CANCEL_REASON]
  - [R2, "The requester never approves.", Fixed, "-"]
  - [R3, "A cancellation re-posts the original events with negative amounts (source ...:CANCEL:n).", Fixed, "-"]
validations:
  - [No reason, Select a reason, RECEIPT_REASON_REQUIRED]
  - [Others without text, "Specify the reason when 'Others' is selected", RECEIPT_REASON_TEXT_REQUIRED]
  - [Receipt already cancelled, "Receipt <no> is already cancelled", RECEIPT_ALREADY_CANCELLED]
  - [Request already waiting, "Receipt <no> already has a request waiting for approval", RECEIPT_ACTION_PENDING]
  - [Unapplied item in disposition, "Unapplied item <ref> of the receipt is <stage>", RECEIPT_HAS_DISPOSITION]
  - [Approver is the requester, "The requester cannot approve <transaction>", MAKER_CHECKER_VIOLATION]
  - [Request not for approval, "<transaction> is <stage>", RECEIPT_ACTION_NOT_FOR_APPROVAL]
fields_screen: Cancel receipt
fields:
  - [Reason Group, List, "Yes", "PREMIUM, COMMISSION, PDC, GENERAL", "-"]
  - [Reason, List, "Yes", LOV RECEIPT_CANCEL_REASON of the group, Active value]
  - [Remarks, Text, Cond., "-", Required for Others]
notifications:
  - CASH_APPROVAL_REQUEST to the approvers on submission; the requester on return or posting.
audit:
  - Request, approval and posting are recorded with the transaction number, AR / OR number, user and time.
acceptance:
  - A cancelled AR puts the invoice it paid back to outstanding and reverses the AR posting.
  - The cashier who requested the cancellation cannot approve it.
  - A cancellation without a reason is refused.
```

> [!NOTE] Difference from the BRD
> The BRD does not state who approves cancellations and reinstatements (OQ06). BIBS requires the approval of a Cashiering TL / TH before posting, so no receipt is reversed by one person.

```fr
id: FR-OP-014
title: Reinstate a cancelled receipt
brd: [CSHID.001 (p.23-24), CSHID.004 (p.26-27), CSHID.005 (p.27-28), CSHID.013 (p.35-36)]
actor: Cashier (request); Cashiering TL / TH (approve)
priority: "Must have (CSHID.013: not stated in the BRD)"
fit: NEW
screens: Receipt page (Reinstate)
api: POST .../receipts/{id}/reinstate; POST .../receipt-actions/{id}/approve
description:
  - A cancelled receipt is reinstated in full or in part as a separate transaction RIN-yyyy-n, with a reason from REINSTATEMENT_REASON and the encoded fields of the reason group. On approval BIBS restores the receipt (status REINSTATED), posts OPS_RECEIPT_REINSTATE for the reinstated amount, applies it to the encoded invoice and keeps the rest unapplied.
  - "Premium reinstatement fields: invoice number, AR number, assured / payor, amount reinstated, Account Officer, Unit Head, Team Leader. Direct payment reinstatement fields: invoice number, OR number, assured / payor, amount reinstated. The fields default from the original receipt."
  - When the receipt was cancelled as a bounced or returned check, the encoded fields are not required (BRD note).
preconditions:
  - The user has CASH_REINSTATE; the receipt is CANCELLED.
main_flow:
  - The cashier opens the cancelled receipt and clicks **Reinstate**.
  - The cashier chooses full or partial, the reason group and reason, and completes the fields.
  - The request goes to the TL, who approves; BIBS posts and re-applies.
alternate_flows:
  - Direct payment reinstatement of a reversed DP account is done in Commission Receivables (FR-OP-094).
rules:
  - [R1, "Reason groups PREMIUM and DIRECT_PAYMENT with the BRD values (section 9.2).", Configurable, LOV REINSTATEMENT_REASON]
  - [R2, "Premium reasons apply to ARs.", Fixed, "-"]
  - [R3, "The reinstated amount is above zero and at most the receipt amount.", Fixed, "-"]
validations:
  - [Receipt not cancelled, Only a cancelled receipt can be reinstated, RECEIPT_NOT_CANCELLED]
  - [Amount out of range, "The reinstated amount must be above zero and at most <amount>", REINSTATEMENT_AMOUNT]
  - [Fields of the group missing, "Enter the <fields> (CSHID.005)", REINSTATEMENT_FIELDS_REQUIRED]
  - [Premium reason on an OR, Premium reinstatement reasons apply to ARs, REINSTATEMENT_REASON_GROUP]
  - [No reason, Select a reason, RECEIPT_REASON_REQUIRED]
fields_screen: Reinstate receipt
fields:
  - [Reinstatement, Option, "Yes", Full / Partial, "-"]
  - [Reason Group, List, "Yes", PREMIUM / DIRECT_PAYMENT, "-"]
  - [Reason, List, "Yes", LOV REINSTATEMENT_REASON, Active value]
  - [Amount to Reinstate, Amount, Cond., "-", Required for partial]
  - [Invoice No. / AR or OR No. / Assured or Payor, Text, Cond., Original receipt, Required unless bounced check]
  - [Account Officer / Unit Head / Team Leader, Look-up, Cond., Sales organisation, Premium group]
  - [Remarks, Text, Cond., "-", Required for Others]
notifications:
  - CASH_APPROVAL_REQUEST to the approvers.
audit:
  - The reinstatement transaction number, reason, fields and posting are recorded.
acceptance:
  - A partial reinstatement of 5,000.00 of a 10,000.00 AR applies 5,000.00 to the encoded invoice and posts OPS_RECEIPT_REINSTATE.
  - A reinstatement without the Unit Head of a premium reason is refused and names the missing field.
  - A bounced-check receipt is reinstated without the encoded fields.
```

```fr
id: FR-OP-015
title: Upload payment files for batch processing
brd: [CSHID.008 (p.29-32)]
actor: Cashier
priority: Must have
fit: CHANGE
screens: Payment Uploads (Bulk Upload wizard, run summary); Cashiering Setup (layouts)
api: /api/v1/bulk (handlers PAY_BILLS, PAY_TRADE, PAY_CLPC, PAY_DIRECT_CREDIT, PAY_PDC); GET / PUT .../layouts/{code}
description:
  - "The cashier uploads the payment files of the channels: Bills Payment (TXT FS01 from IT-DCO), Trade (TXT from CIB), CLPC (Excel), Direct Credit (TXT FS01/04) and the PDC list (Excel). Each row becomes a payment with its AR, is matched and applied (FR-OP-018), and the run summary shows applied, unapplied, pre-booked, excess and failed rows (FR-OP-008)."
  - Each uploaded file is stored read-only with its SHA-256; the same file cannot be uploaded twice. Unmatched payments go to the unapplied premium bucket.
  - The layout of each handler is kept in Cashiering Setup (AUTO for Excel / CSV / detected TXT, DELIMITED with a separator, or FIXED_WIDTH as Header:start:length). Until BDOI gives the bank layouts, the delimited layouts use the BRD field names (section 9.2).
preconditions:
  - The user has CASH_UPLOAD.
main_flow:
  - The cashier opens Payment Uploads and chooses the file type.
  - The cashier uploads the file; BIBS validates the layout and each row.
  - BIBS issues the ARs, matches and applies each payment and shows the run summary.
  - The cashier downloads the run report and reprocesses failed rows.
alternate_flows:
  - Automatic matching again. The job PAYMENT_AUTOMATCH (hourly) re-matches unapplied payments of the files (FR-OP-018).
rules:
  - [R1, "Files are stored read-only with their SHA-256; a duplicate file is refused.", Fixed, "-"]
  - [R2, "Layouts per handler are maintained in Cashiering Setup (OQ03, OQ04).", Configurable, Cashiering Setup]
  - [R3, "An AR is issued for every uploaded payment.", Fixed, "-"]
validations:
  - [Duplicate file, This file was already uploaded, BULK_DUPLICATE_FILE]
  - [Delimited layout without a one-character separator, A delimited layout needs a one-character separator, PAYMENT_LAYOUT_DELIMITER]
  - [Fixed-width field written wrongly, "Write each field as Header:start:length, not '<part>'", PAYMENT_LAYOUT_FIELDS]
  - [Unknown layout kind, "Unknown layout kind <kind>", PAYMENT_LAYOUT_KIND]
fields_screen: Cashiering Setup (payment file layout)
fields:
  - [File Kind, List, "Yes", AUTO / DELIMITED / FIXED_WIDTH, "-"]
  - [Separator, Text, Cond., "-", One character; for DELIMITED]
  - [Fields, Text, Cond., "-", "Header:start:length separated by ; (start is 1-based); for FIXED_WIDTH"]
notifications:
  - "None; the run summary is shown to the uploader."
audit:
  - Each file (name, hash, uploader, time) and each row outcome are kept.
acceptance:
  - A Bills Payment file of 20 rows issues 20 ARs; rows with a booked invoice are applied and the others are unapplied.
  - Uploading the same file again is refused.
  - A change of the PAY_BILLS layout to FIXED_WIDTH is used by the next upload without a release.
```

> [!NOTE] Difference from the BRD
> CSHID.008 states that Trade, Direct Credit and CLPC files are prepared by the business units and run by IT as a batch (FS04), and that the files are "encrypted and cannot be edited". In BIBS the Cashiers upload the files, and integrity is kept by read-only storage with a hash and the duplicate block (OQ03, OQ04).

```fr
id: FR-OP-016
title: Warehouse post-dated checks
brd: [CSHID.008 (p.29-32)]
actor: Cashier; System (maturity job)
priority: Must have
fit: CHANGE
screens: PDC Warehouse
api: GET / POST .../pdc; POST .../pdc/{id}/release; POST .../pdc/mature; bulk PAY_PDC
description:
  - Post-dated checks are warehoused by screen or by the PDC list upload, each with a system number PDCW-yyyy-n, the client, invoice or ARN, check number, bank and branch, maturity date, amount and market segment. The job PDC_MATURITY (daily 08:30 PHT) turns each matured check into a payment with its AR, matched and applied like any payment.
  - Before maturity a check leaves the warehouse as returned, replaced or pulled out, with a reason. The warehouse is viewed by maturity month.
preconditions:
  - The user has CASH_UPLOAD.
main_flow:
  - The cashier adds a check or uploads the daily PDC list.
  - BIBS stores the check as WAREHOUSED with its PDCW number.
  - On the maturity date the job creates the payment, issues the AR and applies it; the check becomes APPLIED.
alternate_flows:
  - The cashier releases a check before maturity as returned, replaced or pulled out.
  - The cashier matures checks on demand (**Mature Now**).
rules:
  - [R1, "A check (bank and number) is warehoused once.", Fixed, "-"]
  - [R2, "Maturity runs daily (job PDC_MATURITY).", Configurable, Job schedule]
validations:
  - [Check already warehoused, "Check <bank> <no> is already warehoused", PDC_DUPLICATE]
  - [Amount zero, A post-dated check needs an amount above zero, PDC_AMOUNT]
  - [Release with another outcome, "A check leaves the warehouse as returned, replaced or pulled out", PDC_RELEASE_STATUS]
  - [Check not in the warehouse, "Check <PDCW no> is <status>, not in the warehouse", PDC_NOT_WAREHOUSED]
fields_screen: Add post-dated check
fields:
  - ["ARN, Invoice, Policy or PN No.", Text, "Yes", "-", "-"]
  - [Client Code / Payor Name, Look-up / Text, "Yes", Client master, "-"]
  - [Check No. / Bank / Bank Branch, Text / List, "Yes", Banks, "-"]
  - [Maturity Date, Date, "Yes", "-", "-"]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Market Segment, List, "No", Segments, "-"]
notifications:
  - "None."
audit:
  - Warehousing, release and maturity are recorded with the PDCW number, user and time.
acceptance:
  - A PDC with maturity today is applied by the job, with its AR, and shows APPLIED.
  - A check warehoused twice is refused.
  - The PDC Warehousing report lists the warehoused checks with AR date, AR number, maturity, client, amount, bank, branch, check number and segment.
```

```fr
id: FR-OP-017
title: Queue checks for pick-up and print their ARs
brd: [CSHID.009 (p.32-33)]
actor: Cashier; Collection (source)
priority: Must have
fit: NEW
screens: Check Pick-up
api: GET / POST .../pickups; POST .../pickups/import, /pickups/print; POST .../pickups/{id}/cancel; feed COLLECTION_CHECK_PICKUP
description:
  - Checks tagged "for check pick-up" by Collection are queued in Cashiering. They arrive through the flow-in feed COLLECTION_CHECK_PICKUP (upload now) or are entered by hand. The cashier filters the queue by pick-up date; only requests due for printing are shown. **Print ARs** issues and prints the ARs of the selected checks in one batch, each with its own AR number.
preconditions:
  - The user has CASH_RECEIPT.
main_flow:
  - Pick-up requests reach the queue from the feed or by entry.
  - The cashier filters by pick-up date and selects the checks.
  - The cashier clicks **Print ARs**. BIBS issues the ARs in one transaction and prints them.
alternate_flows:
  - The cashier cancels a request that will not be picked up.
rules:
  - [R1, "A collection reference is queued once.", Fixed, "-"]
  - [R2, "Only requests due on the selected dates are printed.", Fixed, "-"]
validations:
  - [Reference already queued, "Collection reference <ref> is already queued", PICKUP_DUPLICATE]
  - [Amount zero, The check amount must be above zero, PICKUP_AMOUNT]
  - [None of the selection due, None of the selected requests is due for printing, PICKUP_NOTHING_DUE]
  - [Request not queued, "Pick-up request <ref> is <status>", PICKUP_NOT_QUEUED]
  - [Feed record incomplete, "Pick-up record <key> is incomplete: <detail>", PICKUP_RECORD_INVALID]
fields_screen: Check Pick-up (filter)
fields:
  - [Pick-up From / Pick-up To, Date, "No", "-", From <= To]
notifications:
  - "None."
audit:
  - Each request and its AR number are logged; the report CSH-CHECK-PICKUP lists them.
acceptance:
  - Two requests due today print two ARs with consecutive numbers.
  - A request due next week is not printed today.
```

> [!NOTE] Superseded source
> The source of pick-up requests is the Collection system (OQ01, OQ13). BRD-4 Collections turns it into an in-app Collections disposition; the Cashiering queue and **Print ARs** stay as built.

```fr
id: FR-OP-018
title: Match payments at acceptance and handle pre-booked payments
brd: [CSHID.020 (p.40-41)]
actor: System; Cashier (pre-booked queue)
priority: Must have
fit: CHANGE
screens: Receive Payment (preview); Pre-booked Payments; Payment Uploads (run summary)
api: POST .../payments/preview; POST .../payments; GET .../prebooked; POST .../prebooked/{id}/rematch, /release; POST .../matching/run
description:
  - "Every payment except a warehoused PDC is matched at acceptance on the ARN, invoice, policy or PN number. The outcome is one category: APPLIED (booked invoice with outstanding PR; applied up to 100%, or 98% for 2% CWT clients); PREBOOKED (account found but not booked yet); UNAPPLIED_NO_MATCH (no account found); EXCESS (payment above the outstanding amount; the excess stays unapplied); CANCELLED_REFERENCE (invoice or slip cancelled)."
  - A pre-booked payment issues its AR and waits as an unapplied item of origin PREBOOKED. It is applied when the invoice is booked (ledger event) or by the job PREBOOKED_REMATCH (every 2 hours); processed items leave the pre-booked queue. Items that wait too long raise PREBOOKED_AGEING.
  - A pre-booked payment also opens the New Business payment gate of the account (evidence CASHIERING PRE:<id>), so placement does not wait for booking (FR-OP-132).
preconditions:
  - "None (automatic)."
main_flow:
  - A payment is received (counter, file, PDC, pick-up).
  - BIBS looks up the references and decides the category.
  - BIBS applies, queues as pre-booked, or creates the unapplied item.
alternate_flows:
  - The cashier runs **Re-match Now** on a pre-booked item, or **Release** to move it to the Unapplied Payments workbench.
  - The job PAYMENT_AUTOMATCH (hourly) matches again unapplied NO_MATCH and PREBOOKED items with their references.
rules:
  - [R1, "Application of 2% CWT clients is limited to 98% of the premium; the 2% waits for the BIR 2307.", Configurable, Parameter CWT_APPLICATION_PERCENT (98)]
  - [R2, "Several outstanding invoices of one reference are paid oldest first.", Fixed, "-"]
  - [R3, "Pre-booked ageing threshold 5 days (default).", Configurable, Alert PREBOOKED_AGEING]
  - [R4, "The match key of pre-booked payments is ARN, invoice, policy or PN until BDOI confirms it (OQ12).", Configurable, "-"]
validations:
  - [Pre-booked item already processed, "Pre-booked payment of <ARN> is <status>", PREBOOKED_NOT_OPEN]
notifications:
  - PREBOOKED_AGEING alert to the Cashiers when an item passes the threshold.
audit:
  - The category, matched invoice and application are recorded on the payment.
acceptance:
  - A payment of 10,000.00 on a booked invoice of 10,000.00 is APPLIED in full.
  - For a 2% CWT client the same payment applies 9,800.00 and leaves 200.00 unapplied.
  - A payment by ARN before booking is PREBOOKED and is applied automatically when the invoice is booked.
  - A payment quoting a cancelled invoice becomes unapplied with origin CANCELLED_REFERENCE.
```

```fr
id: FR-OP-019
title: Apply payments by premium component hierarchy
brd: [CSHID.022 (p.42)]
actor: System
priority: Must have
fit: NEW
screens: Receive Payment (preview); Receipt page (Applications, Journal)
api: POST .../payments/preview; POST .../payments
description:
  - "BIBS applies each payment to the premium receivable of the invoice component by component in the order: DST, premium tax / VAT, LGT, FST, other charges, basic premium. It never applies to DTIP. Each application posts OPS_PAYMENT_APPLY (Dr unapplied collections / Cr PR by component) and, when commission is realised on collection, the realised commission and VAT; it records an APPLIED movement APP:<id> on the ledger."
preconditions:
  - The payment matched a booked invoice with outstanding PR.
main_flow:
  - BIBS takes the outstanding balance of each component in hierarchy order.
  - BIBS applies the payment until it is used or the PR is cleared.
  - BIBS posts the application and updates the invoice balances and payment status.
alternate_flows:
  - The payment exceeds the PR. The excess becomes an unapplied item of origin EXCESS.
rules:
  - [R1, "Hierarchy DST > premium tax / VAT > LGT > FST > other > basic.", Fixed, "-"]
  - [R2, "Commission is realised pro rata on collection when OPS_COMMISSION_REALIZATION = ON_COLLECTION.", Configurable, Parameter OPS_COMMISSION_REALIZATION]
validations:
  - [Application reversed twice, "Application <id> is already reversed", APPLICATION_ALREADY_REVERSED]
notifications:
  - "None."
audit:
  - Each application (component, amount, sequence) is kept with its receipt and journal.
acceptance:
  - A partial payment of 1,500.00 on an invoice with DST 1,000.00, VAT 1,200.00 and basic 10,000.00 applies 1,000.00 to DST and 500.00 to VAT.
  - The receipt's Journal tab shows the OPS_PAYMENT_APPLY entry balanced.
```

> [!NOTE] Open point
> The BRD also names a mode-of-payment hierarchy "Check, Cash". Its meaning is not defined (OQ09); BIBS applies each payment separately in the component order.

```fr
id: FR-OP-020
title: Receive non-premium payments of insurers (AR Insurance)
brd: [CSHID.021 (p.41)]
actor: Cashier
priority: "Must have (not stated for this ID in the BRD)"
fit: NEW
screens: Receive Payment (AR class AR Insurance); Receipts
api: POST .../receipts/ar (class AR_INSURANCE)
description: A payment from an insurer that is not premium (refund of remitted premium, other expenses, AR Insurance) is received with the AR class of the non-premium group and the paying insurer. BIBS posts OPS_AR_INSURANCE_RECEIPT (Dr bank / Cr AR Insurer of the insurer) the same business day and does not route it to client premium or to remittance.
preconditions:
  - The user has CASH_RECEIPT.
main_flow:
  - The cashier chooses AR class AR Insurance (or Refund / Other expenses) and the insurer as payor.
  - BIBS issues the AR and posts it to AR Insurer.
alternate_flows:
  - No insurer entered. BIBS refuses the AR.
rules:
  - [R1, "Non-premium ARs never enter payment matching or remittance.", Fixed, "-"]
validations:
  - [Non-premium AR without insurer, "A non-premium AR needs the paying insurer's code", AR_INSURANCE_PAYOR_REQUIRED]
notifications:
  - "None."
audit:
  - As FR-OP-011.
acceptance:
  - An insurer refund received as AR Insurance posts Dr bank / Cr AR Insurer and does not change any client invoice.
```

```fr
id: FR-OP-021
title: Issue commission ORs from commission payment details
brd: [CSHID.007 (p.29)]
actor: Cashier; System
priority: Must have
fit: NEW
screens: Commission ORs; Interfaces (feed COLLECTION_COMMISSION_PAYMENT)
api: GET .../commission-payments; POST .../commission-payments/issue; bulk COMMISSION_PAYMENT
description:
  - Commission payment details uploaded from Collection (bulk COMMISSION_PAYMENT or feed COLLECTION_COMMISSION_PAYMENT) are staged as lines. **Issue ORs** issues one OR per insurer, certificate and payment; lines of one group that name different payees are rejected. The OR is a cash receipt that credits the commission receivable and makes the deferred VAT due (OPS_OR_ISSUE).
  - When a remittance batch is approved, Remittance issues one commission OR per batch, with one line per invoice (FR-OP-035). Direct payment collections issue their OR the same way (FR-OP-094).
preconditions:
  - The user has CASH_UPLOAD; a Head Office OR series is usable.
main_flow:
  - The cashier uploads the commission payment details.
  - BIBS stages the lines and groups them by insurer, certificate and payment.
  - The cashier clicks **Issue ORs**; BIBS issues one OR per group.
alternate_flows:
  - A group with different payees is rejected and stays staged with the reason.
rules:
  - [R1, "Consolidation only for the same insurer / payee and the same certificate.", Fixed, "-"]
  - [R2, "Grouping rule to confirm (one OR per batch or per payment, OQ49).", Configurable, "-"]
validations:
  - [Feed record incomplete, "<errors>", FLOW_IN_RECORD_INVALID]
  - [Unknown company in the feed, "Unknown company <code>", FLOW_IN_COMPANY_UNKNOWN]
notifications:
  - "None."
audit:
  - Staged lines keep the upload job and row; each OR keeps its lines.
acceptance:
  - Three payment lines of one insurer and certificate are issued as one OR with three lines.
  - A remittance batch of five invoices, once approved, carries one commission OR number.
```

> [!NOTE] Difference from the BRD
> CSHID.007 says both "one OR per remittance batch number" and "OR per individual payment unless consolidation criteria are met" (OQ49). BIBS issues one OR per remittance batch for remittance, and one OR per insurer, certificate and payment for the Collection details.

```fr
id: FR-OP-022
title: Manage unapplied payments and their dispositions
brd: [CSHID.024 (p.42-43), CSHID.025 (p.43-44)]
actor: Cashier (assign, submit); Cashiering TL (approve)
priority: Must have
fit: NEW
screens: Unapplied Payments (tabs Unapplied, Monitoring, For Approval, For Reversal, Done); Unapplied Payment page
api: GET .../unapplied; GET .../unapplied/{id}; POST / PUT .../unapplied/{id}/disposition; POST .../unapplied/{id}/submit, /approve, /withdraw, /reversal, /reversal/approve; POST .../unapplied/bulk/submit, /bulk/approve
description:
  - Every unapplied balance is an item UNP-yyyy-n with its origin (no match, excess, cancelled reference, adjustment, cancellation, DP reinstatement, remittance return, re-application, pre-booked, other), amount, balance, currency, client and unit. The workbench tabs follow the stage of workflow OPS_DISPOSITION.
  - "The cashier assigns a disposition type from DISPOSITION_TYPE: Apply to other invoice; DST payment application; Refund; Reclass; Transfer to other marketing unit; Others. Each type has an action (APPLY, DST_APPLY, REFUND, RECLASS, TRANSFER, MANUAL) and says whether it needs approval."
  - "Effects: APPLY applies the amount through the hierarchy to another booked invoice; DST_APPLY applies to its DST only; REFUND sends a request to Disbursement and posts OPS_UNAPPLIED_REFUND when paid; RECLASS and TRANSFER post OPS_UNAPPLIED_RECLASS for the whole balance to another client or unit; MANUAL releases the balance settled outside BIBS."
  - A completed disposition can be marked for reversal with a reason and is reversed on approval; a refund is reversed only after Disbursement returned it. A balance left after a partial disposition goes back to the Unapplied tab.
preconditions:
  - The user has CASH_DISPOSITION.
main_flow:
  - The cashier opens the Unapplied tab and selects an item.
  - The cashier assigns a disposition with the amount and target; the item moves to Monitoring.
  - For a type that needs approval the cashier submits; the item moves to For Approval.
  - The TL approves; BIBS executes the disposition and the item moves to Done.
alternate_flows:
  - Type without approval. The cashier clicks **Process Disposition**; BIBS executes it at once.
  - The TL returns the disposition to the cashier with a reason (Monitoring).
  - The cashier withdraws a disposition in Monitoring; the item returns to Unapplied.
  - Reversal. The cashier marks a completed disposition for reversal; the TL approves or rejects the reversal.
  - Submit and approve are also done in bulk on a selection.
rules:
  - [R1, "Refund, reclass, transfer and others need approval; apply and DST application do not (default, OQ15).", Configurable, Disposition type rules]
  - [R2, "The approver is never the requester.", Fixed, "-"]
  - [R3, "Reclass and transfer move the whole balance.", Fixed, "-"]
validations:
  - [Amount above the balance, "The amount must be above zero and at most the balance <balance>", DISPOSITION_AMOUNT]
  - [Target missing, "Enter <target>", DISPOSITION_TARGET_REQUIRED]
  - [Target invoice unknown, "Unknown invoice <no>", DISPOSITION_INVOICE_UNKNOWN]
  - [Target takes no client payment, "Invoice <no> takes no client payment", DISPOSITION_INVOICE_NOT_RECEIVABLE]
  - [Nothing outstanding on the target, "Invoice <no> has nothing outstanding to apply to", DISPOSITION_NOTHING_TO_APPLY]
  - [Reclass of part of the balance, "A reclass or transfer moves the whole balance <balance>", DISPOSITION_WHOLE_BALANCE]
  - [Type without processing rule, "Disposition type <type> has no processing rule (OQ15)", DISPOSITION_TYPE_NOT_CONFIGURED]
  - [Approver is the requester, The requester cannot approve the disposition, MAKER_CHECKER_VIOLATION]
  - [Reversal without reason, Enter the reason for the reversal, REVERSAL_REASON_REQUIRED]
  - [Refund reversed before return, A refund is reversed only after Disbursement returned the request, REFUND_NOT_RETURNED]
  - [Action in the wrong tab, "<ref> is <stage>, not <expected>", UNAPPLIED_WRONG_STAGE]
fields_screen: Disposition
fields:
  - [Disposition Type, List, "Yes", LOV DISPOSITION_TYPE, Type with a processing rule]
  - [Amount, Amount, "Yes", "-", "> 0 and <= balance; whole balance for reclass / transfer"]
  - [Target invoice / client / unit, Look-up, Cond., Ledger / client master / units, By type]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - CASH_APPROVAL_REQUEST to the approvers on submission.
audit:
  - Every disposition, approval, return and reversal is kept in the item history with user, time and reason.
acceptance:
  - An excess of 500.00 applied to another booked invoice of the client clears 500.00 of that invoice in hierarchy order.
  - A refund needs TL approval and creates a Disbursement request; the item is Done when Disbursement pays it.
  - The cashier who submitted a refund cannot approve it.
  - The tabs show each item in the tab of its stage.
```

```fr
id: FR-OP-023
title: Reverse minimal balances automatically
brd: [CSHID.016 (p.37-38)]
actor: System (job MINIMAL_BALANCE_SWEEP); Cashiering TL (run now)
priority: Must have
fit: NEW
screens: Cashiering Setup (Minimal Balance, Run Sweep Now); Reports CSH-MINBAL-PREMIUM, CSH-MINBAL-EXCESS
api: GET .../minimal-balance/rules; POST .../minimal-balance/sweep
description:
  - "The sweep (daily 04:00 PHT, or **Run Sweep Now**) applies the rules of the minimal balance table: premium receivable balances of PHP 10.00 or less are reversed (OPS_MINIMAL_BALANCE_REVERSAL, ledger MIN_BAL), unless the balance equals the client's 2% CWT, the DST charged or the whole premium; excess and unapplied payments of PHP 10.00 or less go to AP overages (OPS_EXCESS_TO_OVERAGES)."
  - Each invoice component is reversed once; an invoice already written off is skipped.
preconditions:
  - "None (job)."
main_flow:
  - The job reads the active rules.
  - For each invoice with a PR balance at or below the limit, BIBS checks the exclusions and reverses the balance.
  - For each unapplied item at or below the limit, BIBS moves it to AP overages and closes it.
  - BIBS logs the sweep once.
rules:
  - [R1, "PREMIUM rule 10.00, REVERSE, excluding CWT, DST and whole premium.", Configurable, Minimal balance rules; parameter MIN_BALANCE_AUTO_MAX]
  - [R2, "EXCESS rule 10.00 to AP overages.", Configurable, Minimal balance rules]
  - [R3, "COMMISSION rule inactive until BDOI confirms it (OQ11).", Configurable, Minimal balance rules]
validations: []
notifications:
  - "None."
audit:
  - Each reversal is recorded once in the sweep log and as a ledger movement.
acceptance:
  - A PR balance of 5.00 is reversed by the sweep; running the sweep again changes nothing.
  - A PR balance of 5.00 that equals the DST charged is not reversed.
  - An unapplied excess of 8.00 moves to AP overages.
```

> [!NOTE] Difference from the BRD
> The BRD has three minimal balance rules that overlap at 10.00 (CSHID.016, Cashiering summary 5.f, ADJID.026; OQ11). BIBS keeps them as configurable rules and never reverses an invoice twice: the sweep skips invoices written off by the file of FR-OP-059.

```fr
id: FR-OP-024
title: Search receipts and keep the receipt audit trail
brd: [CSHID.010 (p.33-34), CSHID.011 (p.34-35)]
actor: Cashier; Auditor
priority: Must have
fit: "CHANGE (CSHID.010), FIT (CSHID.011)"
screens: Receipts (search); Receipt page (History)
api: GET .../receipts; GET .../receipts/{id}
description:
  - Receipts are searched by AR number, OR number, client number, invoice number, payor, assured, amount, date of issuance, policy number and insurer name, singly or combined, with kind and status filters. Names match partially; results are paged. Each search is logged with user, criteria and time.
  - Every action on a receipt (create, print, cancel, reinstate, apply, approve) records the AR / OR number, username, time, action and module in the immutable audit trail; the receipt's History tab shows it.
preconditions:
  - The user has CASH_RECEIPT.
main_flow:
  - The user enters one or more criteria.
  - BIBS lists the matching receipts.
  - The user opens a receipt and reads its History.
rules:
  - [R1, "Search logs are kept; retention to be confirmed (OQ47).", Configurable, "-"]
  - [R2, "Audit rows cannot be changed or deleted.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the receipt audit; searches are logged in the search log."
acceptance:
  - A search by partial assured name and date range returns the matching ARs.
  - The History tab of a cancelled AR shows the creation, the cancellation request and approval with users and times.
```

```fr
id: FR-OP-025
title: Print ARs and ORs in batch
brd: [CSHID.019 (p.39-40)]
actor: Cashier
priority: "Not stated in the BRD"
fit: NEW
screens: Batch Print
api: GET / POST .../print-batches; GET .../print-batches/{id}; POST .../print-batches/{id}/retry; GET .../print-batches/{id}/file
description:
  - The cashier selects receipts by filter (kind, date range, insurer, BDOI location) or from a list, previews the list and prints them as one merged PDF (batch PRB-yyyy-n). A copy of each receipt is kept. The batch shows progress and lists failures, which the cashier retries or skips.
preconditions:
  - The user has CASH_PRINT.
main_flow:
  - The cashier sets the filter and previews the receipts.
  - The cashier starts the batch; BIBS renders the receipts into one PDF.
  - The cashier downloads or prints the file.
alternate_flows:
  - Failures. The cashier clicks **Retry** for the failed receipts.
rules:
  - [R1, "At most 500 receipts per batch.", Fixed, "-"]
validations:
  - [Selection empty or above 500, "Select between 1 and 500 receipts to print", PRINT_SELECTION]
  - [Retry without failures, "<batch> has no failures", PRINT_NOTHING_FAILED]
fields_screen: Batch Print (filter)
fields:
  - [Kind, List, "No", AR / OR, "-"]
  - [Date From / Date To, Date, "No", "-", From <= To]
  - [Insurer Code, Look-up, "No", Insurer master, "-"]
notifications:
  - "None."
audit:
  - Each batch is logged with user, time and count (print log).
acceptance:
  - A batch of 50 ARs of one branch is produced as one PDF within 10 seconds (section 8).
  - A failed receipt is listed and printed on retry.
```

```fr
id: FR-OP-026
title: Process BIR 2307 reversals
brd: [CSHID.026 (p.44), CSHID.027 (p.44-45)]
actor: Marketing Collection (tag); Cashier (validate, route); Disbursement (release)
priority: Must have
fit: NEW
screens: BIR 2307 (tags, batches)
api: GET / POST .../cwt; GET .../cwt/expected; POST .../cwt/{id}/receive, /checklist, /settle-cash; GET / POST .../cwt/batches; POST .../cwt/batches/{id}/route, /release
description:
  - Marketing tags the 2307 of an invoice with the reference CWT-yyyy-n (FR-OP-113). Cashiering receives the tag and ticks the CWT-copy checklist. For the certificate path, the cashier validates a selection of tags of one insurer into a batch CWB-yyyy-n; BIBS generates the BIR 2307 transaction report and posts OPS_CWT_RECLASS (Dr PR2307 / Cr PR by component), which zeroes the PR.
  - The batch is routed to Disbursement directly, not through Remittance (type CWT2307). When Disbursement pays it, the batch is released to the insurer and BIBS posts OPS_CWT_DTIP_OFFSET (Dr DTIP / Cr PR2307).
  - For the cash path the client pays the 2% in cash; the cashier clicks **Settle in Cash**, an AR is issued and applied to the withheld 2%.
preconditions:
  - The user has CWT_PROCESS; the invoice has a 2% CWT portion outstanding.
main_flow:
  - Cashiering receives a tagged 2307 (workflow OPS_CWT_2307, section 5).
  - The cashier checks the CWT copy and ticks the checklist.
  - The cashier selects tags of one insurer and validates them; BIBS creates the batch, the report and the reclass entries.
  - The cashier routes the batch to Disbursement.
  - Disbursement pays and releases it to the insurer; BIBS posts the DTIP offset.
alternate_flows:
  - The cashier returns a tag to Marketing with a reason (for example the CWT copy is missing).
  - Cash path. **Settle in Cash** issues the AR and closes the tag as SETTLED_CASH.
rules:
  - [R1, "A 2307 batch holds the certificates of one insurer.", Fixed, "-"]
  - [R2, "A tag is validated only when its CWT copy was received.", Fixed, "-"]
  - [R3, "When the DTIP offset is posted (validation or release) is to be confirmed (OQ16); BIBS posts it on release.", Configurable, "-"]
validations:
  - [Tags of several insurers, A 2307 batch holds the certificates of one insurer (CSHID.027), CWT_BATCH_ONE_INSURER]
  - [CWT copy not received, "The CWT copy of <ref> was not received", CWT_COPY_MISSING]
  - [No selection, Select the 2307 tags to validate, CWT_BATCH_EMPTY]
  - [Tag settled in cash in a batch, "<ref> is settled in cash", CWT_CASH_IN_BATCH]
  - [Settle in cash a certificate tag, "<ref> is a certificate tag", CWT_NOT_CASH]
  - [PR below the 2307 amount, "<ref>: the invoice has less premium outstanding than the 2307 amount", CWT_AMOUNT_MISMATCH]
  - [Batch already routed, "<batch> is <status>", CWT_BATCH_ROUTED]
  - [Tag in another stage, "<ref> is <stage>", CWT_WRONG_STAGE]
notifications:
  - Disbursement queue entry when the batch is routed.
audit:
  - Each tag and batch keeps its workflow history; the report CSH-2307-TXN is kept with the batch.
acceptance:
  - Validating two tags of one insurer posts Dr PR2307 / Cr PR and zeroes the 2% PR of both invoices.
  - A batch with tags of two insurers is refused.
  - After Disbursement pays the batch, release posts Dr DTIP / Cr PR2307.
```

> [!NOTE] Superseded source
> The Marketing tagging is replaced in BRD-4 by the Collections disposition "PR 2307 for reversal" (OQ45); the Disbursement step moves to the Disbursement module of BRD-5 (OQ02). The Cashiering validation, report and postings stay as built.

```fr
id: FR-OP-027
title: Post the accounting entries of Cashiering
brd: [CSHID.012 (p.35), CSHID.013 (p.35-36), CSHID.014 (p.36)]
actor: System
priority: "Must have (CSHID.013: not stated in the BRD)"
fit: "CHANGE (CSHID.012, 014), NEW (CSHID.013)"
screens: Receipt page (Journal)
api: Accounting engine (business events)
description:
  - "Each Cashiering transaction is a business event whose GL rule is configured by Comptrollership: OPS_AR_RECEIPT (AR issued), OPS_AR_INSURANCE_RECEIPT, OPS_OR_ISSUE, OPS_PAYMENT_APPLY, OPS_RECEIPT_REINSTATE, OPS_CWT_RECLASS, OPS_CWT_DTIP_OFFSET, OPS_EXCESS_TO_OVERAGES, OPS_MINIMAL_BALANCE_REVERSAL, OPS_UNAPPLIED_REFUND, OPS_UNAPPLIED_RECLASS. Section 5.3 lists the default entries."
  - A cancellation re-posts the original events with negative amounts; a reinstatement posts OPS_RECEIPT_REINSTATE and a new application. Amounts in foreign currency are converted to pesos at the Comptrollership BOOK rate with 2 decimals.
preconditions:
  - The GL rules of the events are configured (OQ07).
main_flow:
  - A Cashiering transaction is posted.
  - BIBS raises the business event with its amounts, party, cost centre and business line.
  - The accounting engine creates and posts the balanced journal; the journal batch is linked on the receipt and the ledger movement.
rules:
  - [R1, "GL accounts are never chosen in code; Comptrollership configures the rules (maker-checker).", Configurable, Accounting rules]
  - [R2, "The bank and cash-on-hand accounts of the @BANK role come from the parameters CASH_BANK_ACCOUNT and CASH_ON_HAND_ACCOUNT.", Configurable, Parameters]
  - [R3, "BOOK rate with 2 decimals, maintained on Currency Rates until its source is agreed (OQ08).", Configurable, Rate type BOOK]
validations: []
notifications:
  - "None."
audit:
  - Journals are immutable; a correction is a reversing event.
acceptance:
  - Issuing, applying and cancelling an AR produce three balanced journals; the cancellation nets the first two to zero.
  - A USD AR posts pesos at the BOOK rate of the receipt date.
```

```fr
id: FR-OP-028
title: Generate the Cashiering reports
brd: [CSHID.023 (p.42 and p.125-127)]
actor: Cashier; Cashiering TL; Comptrollership
priority: Must have
fit: NEW
screens: Reports (category Operations)
api: /api/v1/reports (CSH-*)
description: BIBS provides the 21 Cashiering reports of Annex II plus the outstanding AR report, the batch run report and the BIR 2307 transaction report (24 reports, section 6.1). Reports whose fields the Annex does not give carry a draft layout note until BDOI confirms them (OQ42); ageing buckets are to be confirmed (OQ43).
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user chooses the report and its parameters.
  - BIBS generates it; the user views, downloads or prints it (FR-OP-009).
rules:
  - [R1, "Layouts of Annex II reports 9-18, 20 and 21 are drafts (OQ42).", Configurable, Report layouts]
validations: []
notifications:
  - "None."
audit:
  - Runs are archived (FR-OP-009).
acceptance:
  - CSH-APPLIED-PREM lists AR no., date, amount paid, invoice, payor, risk code, count and total for the chosen dates, in PDF and Excel.
  - CSH-CANCELLED-AR lists the cancelled ARs with their reason.
```


## Remittance

Remittance extracts what clients paid, groups it into batches per insurer and type, gets the batches approved, asks Disbursement to pay the insurer and records the insurer's OR. API paths of this section are under `/api/v1/remittance`.

```fr
id: FR-OP-030
title: Extract remittances by schedule, insurer or invoice
brd: [RMTID.001 (p.49), RMTID.003 (p.50-51), RMTID.004 (p.51-52), RMTID.005 (p.52), RMTID.007 (p.52-53), RMTID.008 (p.53)]
actor: System (job REMITTANCE_EXTRACTION); Remittance Processor
priority: Must have
fit: "NEW (RMTID.001, 004, 005, 007), CHANGE (RMTID.003), CONFIGURE (RMTID.008)"
screens: Extraction; DTIP Status; Remittance Batches
api: GET / POST .../runs; GET .../runs/{id}; GET .../runs/{id}/tags; GET / POST .../eod-requests
description:
  - "An extraction run REX-yyyy-n examines the invoices of an insurer whose remittance status is UNPROCESSED, WITH_OUTSTANDING_BALANCE or PARTIALLY_REMITTED. It is started by the scheduled job (daily off-peak, 20:00 PHT by default), manually for an insurer and remittance type, or for one invoice number. Each insurer is processed in its own transaction."
  - "Every invoice examined gets a tag: EXTRACTED; UNEXTRACTED_DUE (with its blocking reasons, FR-OP-031); UNEXTRACTED_NOT_DUE; RETURNED (extracted then returned by a user)."
  - "Extracted invoices are grouped into batches RMB-<insurer>-yyyy-n per insurer and remittance type (WITH_INCENTIVES, NORMAL_PHP, NORMAL_USD, SPECIAL), assigned to a processor and opened at stage REVIEW_IN_PROCESS (FR-OP-033). Each invoice in a batch is locked by REMITTANCE. The extract file is stored in the extract repository, named by REMIT_FILE_PATTERN."
  - A processor searching an invoice during the day can queue it for the end-of-day run (RMTID.005); the evening run extracts the queued invoices once.
preconditions:
  - The user has REMIT_EXTRACT for a manual run.
main_flow:
  - The job or the processor starts a run for one or all insurers.
  - BIBS checks each invoice against the eligibility rules and tags it.
  - BIBS groups the extracted invoices into batches, locks them and writes the extract file.
  - BIBS notifies the processors (REMIT_EXTRACTION_DONE).
alternate_flows:
  - Single invoice. The processor enters the invoice and insurer; BIBS extracts it or says why it cannot.
  - End-of-day request. The processor queues an invoice; the evening run takes it.
  - A run fails for one insurer. The other insurers continue; BIBS raises REMIT_EXTRACTION_FAILED.
rules:
  - [R1, "The remittance type is WITH_INCENTIVES when an early remittance rule covers the invoice (FR-OP-038), otherwise NORMAL_PHP or NORMAL_USD by currency.", Configurable, "Incentive rules; classification to confirm (OQ17)"]
  - [R2, "Batch numbers RMB-<insurer>-yyyy-n are unique and never reused.", Fixed, "-"]
  - [R3, "Schedule of the job.", Configurable, "Job REMITTANCE_EXTRACTION (brokerverse.jobs.remittance-extraction-cron)"]
  - [R4, "Extract file naming.", Configurable, Parameter REMIT_FILE_PATTERN (OQ17)]
validations:
  - [Invoice of another insurer, "Invoice <no> is not an invoice of <insurer>", REMIT_INVOICE_INSURER]
  - [Invoice not extractable, "Invoice <no> cannot be extracted: remittance status <status>", REMIT_INVOICE_NOT_EXTRACTABLE]
  - [Invoice not eligible now, "Invoice <no> cannot be remitted now: <reasons>", SPECIAL_REMIT_NOT_ELIGIBLE]
fields_screen: Extraction (manual run)
fields:
  - [Insurer Code, Look-up, "Yes", Insurer master, Active insurer]
  - [Remittance Type, List, "No", LOV REMITTANCE_TYPE, Blank = all]
  - [Invoice No., Text, "No", Invoice ledger, For a single-invoice extraction]
notifications:
  - REMIT_EXTRACTION_DONE to the holders of REMIT_PROCESS; REMIT_EXTRACTION_FAILED alert on failure.
audit:
  - Each run keeps its trigger, counts, file and the tag of every invoice examined.
acceptance:
  - The scheduled run extracts a paid and cleared invoice of INS-MGIC into a batch RMB-INS-MGIC-2026-n with tag EXTRACTED and locks it.
  - An invoice on hold is tagged UNEXTRACTED_DUE with reason ON_HOLD.
  - A manual extraction of an invoice of another insurer is refused with REMIT_INVOICE_INSURER.
  - An invoice queued for the end of day is extracted by the evening run, once.
```

> [!NOTE] Difference from the BRD
> RMTID.005 asks to "trigger extraction at the end of day if payment application is searched" (OQ18). BIBS lets the processor queue the searched invoice for the evening run. RMTID.001 asks to save extracts on a shared drive with a folder per type; BIBS stores them in the in-system extract repository behind the FileDropPort seam (OQ17).

```fr
id: FR-OP-031
title: Apply the remittance eligibility rules
brd: [RMTID.006 (p.52), RMTID.014 (p.56-57), RMTID.015 (p.57), RMTID.017 (p.58-59), RMTID.018 (p.59), RMTID.020 (p.60-61), RMTID.022 (p.61-62), RMTID.028 (p.64), RMTID.031 (p.65-66), RMTID.035 (p.67)]
actor: System
priority: Must have
fit: "NEW (RMTID.006, 014, 015, 020, 022, 028, 031, 035), CHANGE (RMTID.017, 018)"
screens: Extraction (run tags); DTIP Status; Report REM-PAIDAR-OVER-DTIP; Report REM-EXCLUDED
api: GET .../runs/{id}/tags; GET .../dtip
description:
  - "Paid AR is the net applied PR of the invoice from applied and posted payments only (cash, check and every channel). The amount to remit is paid AR less the DTIP already remitted."
  - "An invoice is not extracted, and its tag lists the reason, when: ON_HOLD (hold flag); PENDING_NEG_ADJ (a negative adjustment request is pending); WRITTEN_OFF; CHECK_HOLDING (a payment is younger than 3 banking days or not cleared); PAID_AR_OVER_DTIP (paid AR above the DTIP balance); NOT_POSTED; OTHERS (another team holds the lock). Cancelled, direct payment and return invoices are skipped."
  - Written-off invoices are excluded from every output but remain visible and marked in Invoice 360 and the tags.
  - When a negative adjustment is requested on an invoice, Adjustment raises PENDING_NEG_ADJ and Remittance notifies the Remittance Team with the batch the invoice is in (RMTID.035).
preconditions:
  - "None (applied by every extraction and again at submission and approval)."
main_flow:
  - BIBS reads the ledger balances, flags and payments of the invoice.
  - BIBS evaluates each rule and records the reasons.
  - BIBS extracts the invoice only when no rule blocks it.
alternate_flows:
  - Paid AR above DTIP with mode CAP. BIBS extracts the invoice for the DTIP balance only and records the difference.
rules:
  - [R1, "Holding period 3 banking days on the branch calendar, counted from the last applied value date.", Configurable, Parameter REMIT_CHECK_HOLD_DAYS (3)]
  - [R2, "Paid AR above DTIP excluded by default; CAP remits the DTIP balance.", Configurable, Parameter REMIT_PAIDAR_OVER_DTIP_MODE (EXCLUDE)]
  - [R3, "Negative DTIP is never extracted.", Fixed, "-"]
  - [R4, "The rules are checked again at submission and approval (FR-OP-033, FR-OP-035).", Fixed, "-"]
validations: []
notifications:
  - REMIT_PAIDAR_OVER_DTIP alert per invoice; REMIT_NEG_ADJ_PENDING to the Remittance Team.
audit:
  - Each run keeps the rule results per invoice.
acceptance:
  - An invoice paid by check two banking days ago is UNEXTRACTED_DUE with CHECK_HOLDING and is extracted on the third banking day.
  - An invoice with paid AR 10,500.00 and DTIP 10,000.00 is excluded and listed on REM-PAIDAR-OVER-DTIP with invoice, insurer, paid AR and DTIP.
  - A written-off invoice is never in a batch and shows WRITTEN_OFF on Invoice 360.
  - Submitting a negative adjustment on an invoice in a batch notifies the Remittance Team.
```

> [!NOTE] Differences from the BRD
> RMTID.014: the expected result caps the remittance at the DTIP balance, while the acceptance criteria exclude the account (OQ19); BIBS excludes by default and caps when the parameter is set to CAP. RMTID.017: the start of the holding period (AR date or deposit date) and the source of "cleared" are open (OQ20); BIBS counts from the last applied value date.

```fr
id: FR-OP-032
title: Review a batch and exclude records (addendum)
brd: [RMTID.002 (p.50 and addendum p.4-5), RMTID.024 (p.62-63), RMTID.027 (p.64)]
actor: Remittance Processor
priority: Must have
fit: "NEW (RMTID.002, 024), CHANGE (RMTID.027)"
screens: Remittance Batches (queues by stage); Remittance Batch (totals, lines, exclusions, preview)
api: GET .../batches; GET .../batches/{id}; GET .../batches/{id}/preview; POST .../batches/{id}/exclude, /restore
description:
  - The Remittance Batches screen lists the batches by stage (queues) with insurer, type, processor, counts and totals; it filters by insurer and type and supports bulk submit and approve.
  - "The batch page shows a read-only totals strip (paid AR, commission, VAT, WTAX, DTIP, incentive, net due, payable), the lines, the exclusions panel, the workflow panel and the documents. Financial values cannot be edited (addendum)."
  - The processor marks lines for exclusion with a reason; the invoice is unlocked and can be taken by a later run. An exclusion can be restored while the batch is editable. **Preview submission** shows the resulting dataset before submission. Only non-excluded lines are posted and pushed to Disbursement; every exclusion and restore is kept.
preconditions:
  - The user has REMIT_EXCLUDE; the batch is in REVIEW_IN_PROCESS or ON_HOLD.
main_flow:
  - The processor opens the batch and reviews the lines.
  - The processor selects lines, clicks **Exclude Accounts**, chooses a reason and writes a comment.
  - BIBS excludes the lines, unlocks the invoices and recomputes the totals.
  - The processor previews the submission.
alternate_flows:
  - Restore. The processor restores an excluded line; BIBS locks the invoice again.
  - The invoice can no longer be restored (taken by another batch, changed status). BIBS refuses the restore.
rules:
  - [R1, "Exclusion only; no endpoint changes amounts.", Fixed, "-"]
  - [R2, "Exclusion reasons from the list.", Configurable, LOV REMIT_EXCLUSION_REASON]
  - [R3, "Exclusion and restore only while the batch can change.", Fixed, "-"]
validations:
  - [Line already excluded, "Invoice <no> is already excluded", REMIT_LINE_ALREADY_EXCLUDED]
  - [Restore of a line not excluded, "Invoice <no> is not excluded", REMIT_LINE_NOT_EXCLUDED]
  - [Restore no longer possible, "Invoice <no> can no longer be restored (remittance status <status>)", REMIT_RESTORE_NOT_ELIGIBLE]
  - [Batch no longer editable, "Batch <no> is <stage>: lines can no longer change", REMIT_BATCH_STAGE]
  - [Exclusion without permission, You are not permitted to perform this action, ACCESS_DENIED]
fields_screen: Exclude Accounts
fields:
  - [Reason, List, "Yes", LOV REMIT_EXCLUSION_REASON, "-"]
  - [Comment, Text, Cond., "-", Required for Others]
notifications:
  - "None."
audit:
  - Each exclusion and restore keeps user, time, reason and comment on the line.
acceptance:
  - The processor cannot change the paid AR or commission of a line.
  - Excluding a line removes it from the totals and the preview and unlocks the invoice.
  - A restored line is back in the totals.
  - Only the remaining lines are posted on approval.
```

> [!NOTE] Addendum
> The main BRD (p.50) allowed online editing of the extract and pushing it "with or without edit". Addendum 1 (p.4-5) replaces it with exclusion only; BIBS follows the addendum.

```fr
id: FR-OP-033
title: Process a batch - assign, submit, hold and return
brd: [RMTID.009 (p.53-54), RMTID.010 (p.54), RMTID.019 (p.59-60), RMTID.029 (p.64-65)]
actor: Remittance Processor; Remittance TL
priority: Must have
fit: "CHANGE (RMTID.009, 010, 029), NEW (RMTID.019)"
screens: Remittance Batch (workflow panel); Remittance Batches
api: POST .../batches/{id}/submit, /return, /assign
description:
  - Each batch is a case of workflow OPS_REMITTANCE (section 5), starting at REVIEW_IN_PROCESS with its processor. The TL re-assigns it to another processor.
  - "**Submit for approval** checks every included line again: still locked by Remittance, and not cancelled, on hold, written off or flagged PENDING_NEG_ADJ, and the batch not empty. A batch can be put on hold with a reason and released, or returned with a reason; a return unlocks the lines and tags them RETURNED so a later run can take them again."
  - "The invoice keeps its remittance status (REVIEW_IN_PROCESS, APPROVED, PARTIALLY_REMITTED, FULLY_REMITTED, REQUESTED_FOR_HOLD, UNPROCESSED, UNAPPLIED_PAYMENT, WITH_OUTSTANDING_BALANCE); the batch has its own stage."
preconditions:
  - The user has REMIT_PROCESS.
main_flow:
  - The processor reviews the batch (FR-OP-032).
  - The processor clicks **Submit for approval**.
  - BIBS validates the lines and moves the batch to FOR_APPROVAL; the approvers are notified.
alternate_flows:
  - Hold. The processor holds the batch with a reason; **Release batch** returns it to review.
  - Return. The processor or TL returns the batch with a reason; the lines are unlocked and tagged RETURNED.
  - Re-assign. The TL assigns the batch to another active processor.
  - A line became invalid. Submission is refused and lists the problems.
rules:
  - [R1, "No double submission; the workflow accepts submit only from REVIEW_IN_PROCESS.", Fixed, "-"]
  - [R2, "Return and hold reasons from the lists.", Configurable, LOV REMIT_RETURN_REASON; LOV HOLD_REASON]
validations:
  - [Batch without lines, "Batch <no> has no invoice left to remit", REMIT_BATCH_EMPTY]
  - [Line no longer valid, "Batch <no> cannot go on: <problems>", REMIT_SUBMISSION_INVALID]
  - [Return after approval, "Batch <no> can no longer return", REMIT_BATCH_STAGE]
  - [Return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - REMIT_BATCH_FOR_APPROVAL to the approvers on submission; REMIT_BATCH_DECIDED to the processor on return.
audit:
  - Each transition is in the batch history with user, time, reason and comment.
acceptance:
  - A batch with one line placed on hold after extraction cannot be submitted; the message names the invoice and HOLD.
  - A returned batch releases its invoices; the next run extracts them again.
  - The TL re-assigns a batch; the new processor sees it in the queue.
```

> [!NOTE] Difference from the BRD
> RMTID.019 lists eight statuses that mix payment states (Unapplied Payment, With Outstanding Balance) and remittance states (OQ21). BIBS keeps the payment status and the remittance status of the invoice separately and gives the batch its own stages.

```fr
id: FR-OP-034
title: Produce the remittance schedule and payment request
brd: [RMTID.011 (p.54-55)]
actor: Remittance Processor
priority: Must have
fit: CHANGE
screens: Remittance Batch (Documents)
api: GET .../batches/{id}/documents/{kind}
description: The remittance schedule (PDF and XLSX) and the payment request (PDF) are generated from the templates REMITTANCE_SCHEDULE and REMITTANCE_PAYMENT_REQUEST and stored on the batch. They show the batch number, insurer, remittance type and the payment details per invoice; the layouts follow Annex III (Normal, Special, With Incentives).
preconditions:
  - The batch is submitted or approved.
main_flow:
  - The processor opens the Documents tab of the batch.
  - The processor downloads or prints the schedule and the payment request.
rules:
  - [R1, "Schedule layouts are drafts until BDOI confirms them (Mall Assurance columns of the Normal schedule, OQ42).", Configurable, Document templates]
  - [R2, "A document is generated from stored values, so a regenerated file shows the same content.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - Document generation is recorded on the batch.
acceptance:
  - The schedule of an approved batch lists each invoice with paid AR, realised commission, VAT, WTAX, DTIP and net due, and the totals equal the batch totals.
  - The payment request shows the payable amount, insurer and approver.
```

```fr
id: FR-OP-035
title: Approve a batch, post the remittance and request payment
brd: [RMTID.010 (p.54), RMTID.019 (p.59-60)]
actor: Remittance TL
priority: Must have
fit: CHANGE
screens: Remittance Batch (workflow panel)
api: POST .../batches/{id}/approve
description:
  - "The TL approves the submitted batch (**Approve and push to Disbursement**). In the approval transaction BIBS: re-checks every line; posts OPS_REMITTANCE per line (Dr DTIP, Dr CWT / Cr commission receivable, Cr due to insurer for disbursement); records REMITTED movements on DTIP, commission, VAT and WTAX and sets the invoices APPROVED; for a With Incentives batch posts OPS_REMIT_INCENTIVE; sends a payment request of type REMITTANCE for the payable to Disbursement; and issues the commission OR (and the incentive OR) through Cashiering."
  - "Commission, VAT and WTAX are realised pro rata on the cumulative DTIP remitted; the remittance that clears the DTIP takes the remaining balances. Net due = paid AR + WTAX - commission - VAT; payable = net due - incentive with VAT."
preconditions:
  - The batch is FOR_APPROVAL; the user has REMIT_APPROVE and did not submit it.
main_flow:
  - The TL reviews the batch and its preview.
  - The TL approves.
  - BIBS posts, sends the payment request and issues the ORs; the batch moves to APPROVED.
alternate_flows:
  - Send back. The TL sends the batch back to the processor with a reason (REVIEW_IN_PROCESS).
  - Return. The TL returns the batch (RETURNED).
rules:
  - [R1, "The approver is never the submitter.", Fixed, "-"]
  - [R2, "Posting, payment request and ORs are idempotent on the batch.", Fixed, "-"]
validations:
  - [Approver is the submitter, "Batch <no> must be approved by another user", REMIT_FOUR_EYES]
  - [Line no longer valid, "Batch <no> cannot go on: <problems>", REMIT_SUBMISSION_INVALID]
notifications:
  - REMIT_BATCH_DECIDED to the processor; the Disbursement queue receives the request.
audit:
  - The approval, journals, payment request number and OR numbers are recorded on the batch.
acceptance:
  - The processor who submitted a batch cannot approve it.
  - Approval posts one balanced OPS_REMITTANCE journal per line and one payment request for the payable.
  - The batch shows the commission OR number issued by Cashiering.
```

```fr
id: FR-OP-036
title: Follow Disbursement and track remittance status
brd: [RMTID.034 (p.67), RMTID.036 (p.67-68)]
actor: System; Remittance Processor
priority: Must have
fit: CHANGE
screens: Remittance Batch (workflow panel, history); Invoice 360 (Remittances); Disbursement Queue
api: GET .../batches/{id}; GET /api/v1/ops/invoices/{no}; /api/v1/ops/disbursements
description:
  - When Disbursement assigns the DV number, BIBS makes each invoice FULLY_REMITTED (no DTIP left) or PARTIALLY_REMITTED, releases the lock and moves the batch to FULLY_REMITTED or PARTIALLY_REMITTED. When Disbursement returns the request, the processors are notified.
  - The stage timeline of the batch and of the invoice shows every stage from extraction to disbursement with time stamps; users set their notification preferences per event (in-app, e-mail) on Notification Settings.
preconditions:
  - The batch is APPROVED.
main_flow:
  - Disbursement acknowledges, assigns the DV and pays the request (FR-OP-120).
  - BIBS updates the invoices and the batch and notifies the users.
alternate_flows:
  - Disbursement returns the request. The processors are notified; re-sending is manual (OQ02).
rules:
  - [R1, "Users may opt out of a notification event.", Configurable, Notification Settings]
validations: []
notifications:
  - OPS_DISBURSEMENT_STATUS on each status change of the payment request.
audit:
  - Each status change is kept with its time on the batch, the invoice and the request.
acceptance:
  - A DV assigned for the full payable makes the batch FULLY_REMITTED and unlocks its invoices.
  - A user who switched off OPS_DISBURSEMENT_STATUS e-mails still sees the in-app notice.
```

```fr
id: FR-OP-037
title: Upload the insurer OR schedule and report exceptions
brd: [RMTID.012 (p.55-56), RMTID.013 (p.56), RMTID.016 (p.57-58)]
actor: Remittance Processor
priority: "Must have (RMTID.016: not stated in the BRD)"
fit: "NEW (RMTID.012, 013), CHANGE (RMTID.016)"
screens: Insurer OR Upload (upload, history); Report REM-OR-EXCEPTION
api: POST .../insurer-or/upload; GET .../insurer-or/runs; GET .../insurer-or/runs/{id}; feed INSURER_REMIT_OR
description:
  - The insurer returns the remittance schedule with its OR number, date and amount per client invoice. The processor uploads it (columns batchNo, invoiceNo, orNo, orDate, orAmount); the upload runs through the feed INSURER_REMIT_OR, one record per invoice.
  - Each valid row sets the insurer OR on the batch line and the invoice. The line is MATCHED when the OR amount equals the paid AR, AMOUNT_MISMATCH otherwise. The batch reaches OR_RECEIVED when every included line has an OR.
  - The exception report REM-OR-EXCEPTION lists, per upload, the reference, OR amount, paid PR, status and reason, with a summary; it opens from the upload history.
preconditions:
  - The user has REMIT_OR_UPLOAD; the batch is approved.
main_flow:
  - The processor uploads the insurer schedule.
  - BIBS validates each row and updates the lines.
  - BIBS shows the counts (records read, matches, amount mismatches, refused).
  - The processor opens the exception report.
alternate_flows:
  - Rows refused (unknown batch, excluded line, duplicate OR) are listed with their reason; the other rows are kept.
rules:
  - [R1, "The OR amount is compared with the paid AR of the invoice, exact amount (tolerance to be given, OQ22).", Configurable, "-"]
  - [R2, "An OR number is recorded once per client.", Fixed, "-"]
validations:
  - [Batch not approved, "Batch <no> is <stage>: not yet approved", REMIT_OR_BATCH_STAGE]
  - [Line excluded, "Invoice <no> was excluded from <batch>", REMIT_OR_LINE_EXCLUDED]
  - [OR already recorded, "OR <no> was already recorded for client <client>", REMIT_OR_DUPLICATE]
  - [Missing column, "The file must have the columns <columns>", FEED_FILE_COLUMNS]
  - [Missing value, "Line <n>: <column> is missing", FEED_VALUE_MISSING]
  - [Bad date or amount, "Line <n>: <column> must be YYYY-MM-DD / an amount", FEED_VALUE_INVALID]
  - [Empty file, The file has no lines, FEED_FILE_EMPTY]
notifications:
  - "None."
audit:
  - Each upload is a flow-in run with its records; the OR and its upload are kept on the line.
acceptance:
  - An upload with the OR of every line moves the batch to OR_RECEIVED.
  - An OR amount 100.00 below the paid AR is AMOUNT_MISMATCH and listed on REM-OR-EXCEPTION.
  - A row without OR number is refused with its line number.
```

```fr
id: FR-OP-038
title: Apply the early remittance incentive
brd: [RMTID.023 (p.62)]
actor: Remittance TL (rules); System
priority: Must have
fit: NEW
screens: Incentive Rules; Remittance Batch (incentive amounts)
api: GET / POST .../incentive-rules; PUT .../incentive-rules/{id}
description:
  - "An incentive rule holds the insurer, product line (blank = all), segment (blank = all), rate as a percent of basic premium, window in days and the start of the window (inception or booking), with effective dates. An invoice remitted within the window goes into a With Incentives batch; its incentive = rate x basic premium share, with VAT at the invoice's VAT to commission ratio. On approval BIBS posts OPS_REMIT_INCENTIVE (Dr due for disbursement / Cr incentive income, output VAT) and issues the incentive OR."
  - The same rules are read by Production Reconciliation to validate early incentives (FR-OP-078).
preconditions:
  - The user has REMIT_APPROVE to maintain rules.
main_flow:
  - The TL adds a rule for an insurer.
  - Each extraction checks whether an invoice is within the window of a rule.
  - The incentive is computed on the batch and posted on approval.
alternate_flows:
  - Remitted after the window. No incentive; the invoice goes to a Normal batch.
rules:
  - [R1, "Rates, windows and basis per insurer (demo INS-MGIC Property CBG 2%, 30 days from inception; values from BDOI, OQ23).", Configurable, Incentive Rules]
validations:
  - [End before start, The rule cannot end before it starts, INCENTIVE_RULE_PERIOD]
  - [No company, Choose the company of the rule, COMPANY_REQUIRED]
fields_screen: Incentive Rules
fields:
  - [Insurer Code, Look-up, "Yes", Insurer master, "-"]
  - [Product Line (blank = all), List, "No", Product lines, "-"]
  - [Segment (blank = all), List, "No", Segments, "-"]
  - [Rate (% of basic premium), Number, "Yes", "-", "> 0"]
  - [Window (days), Number, "Yes", "-", "> 0"]
  - [Window Starts From, List, "Yes", Inception / Booking, "-"]
  - [Effective From / Effective To, Date, "Yes / No", "-", To >= From]
  - [Description, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - Rule changes are audited.
acceptance:
  - A CBG property invoice of INS-MGIC remitted 20 days after inception is in a With Incentives batch with a 2% incentive.
  - The same invoice remitted after 40 days has no incentive.
```

```fr
id: FR-OP-039
title: Search remittance accounts and payment details
brd: [RMTID.025 (p.63)]
actor: Remittance Processor
priority: Must have
fit: NEW
screens: DTIP Status; Remittance Batches
api: GET .../dtip; GET .../accounts; GET .../batches
description: DTIP Status shows the DTIP position of each invoice with its remittance status and the last extraction tag and reasons. It searches by invoice number, remittance batch number, endorsement reference, policy number and assured name, partially and without regard to case or format, and filters by insurer and remittance status. From a result the user opens Invoice 360 (FR-OP-005).
preconditions:
  - The user has REMIT_PROCESS.
main_flow:
  - The processor enters a search value.
  - BIBS lists the matching invoices with DTIP, paid AR, remitted and tag.
  - The processor opens an invoice.
rules: []
validations: []
fields_screen: DTIP Status (filters)
fields:
  - [Search, Text, "No", "-", "Invoice, batch, endorsement reference, policy or assured"]
  - [Insurer Code, Look-up, "No", Insurer master, "-"]
  - [Remittance Status, List, "No", Remittance statuses, "-"]
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - Searching "dela cruz" finds the invoices of assured "DELA CRUZ, JUAN".
  - Searching a batch number lists the invoices of the batch.
```

```fr
id: FR-OP-040
title: Process special remittance requests in Remittance
brd: [RMTID.030 (p.65), RMTID.033 (p.66-67)]
actor: Remittance TL; Remittance Processor
priority: Must have
fit: "NEW (RMTID.030), CONFIGURE (RMTID.033)"
screens: Special Remittance; Special Remittance Request
api: GET .../special; GET .../special/{id}; POST .../special/{id}/approve, /reject; feed COLLECTION_SPECIAL_REMIT
description:
  - Special remittance requests raised by Marketing (FR-OP-112) or received from Collection through the feed COLLECTION_SPECIAL_REMIT are listed with their condition, invoice, status and history. The TL approves or rejects them. Approval creates a SPECIAL batch at once, without a processor, so that another user approves the batch; the request follows the batch to PUSHED_TO_DISBURSEMENT or RETURNED.
  - Every status change is notified to the requester with the request number and new status.
preconditions:
  - The user has SPECIAL_REMIT_APPROVE (decide) or REMIT_PROCESS (view).
main_flow:
  - The TL opens a request FOR_APPROVAL.
  - The TL approves; BIBS creates the SPECIAL batch and extracts the invoice into it.
  - The batch goes through FR-OP-033 and FR-OP-035; the request becomes PUSHED_TO_DISBURSEMENT.
alternate_flows:
  - Reject with a reason (REJECTED).
  - The batch is returned; the request becomes RETURNED.
rules:
  - [R1, "The approver is never the requester.", Fixed, "-"]
  - [R2, "Stage notifications are configured per stage.", Configurable, Workflow OPS_SPECIAL_REMIT]
validations:
  - [Approver is the requester, "Request <no> must be approved by another user", SPECIAL_REMIT_FOUR_EYES]
notifications:
  - The requester on approval, rejection, push and return.
audit:
  - The request history keeps each decision with user, time and reason.
acceptance:
  - Approving a special remittance creates a SPECIAL batch containing the invoice.
  - The requester receives a notice with the request number when the batch is pushed to Disbursement.
```

```fr
id: FR-OP-041
title: Generate the Remittance reports
brd: [RMTID.039 (p.69 and p.127-129)]
actor: Remittance Processor; Remittance TL
priority: Must have
fit: NEW
screens: Reports (category Operations)
api: /api/v1/reports (REM-*)
description: BIBS provides the 8 Remittance reports of Annex III and four more (REM-PAIDAR-OVER-DTIP, REM-OR-EXCEPTION, REM-EXCLUDED, REM-HOLD), section 6.2. Layouts the Annex does not detail are drafts (OQ42).
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user chooses the report and its parameters and generates it.
rules:
  - [R1, "Layout of report 8 (List of Remitted Accounts with Batch Number) is a draft (OQ42).", Configurable, Report layouts]
validations: []
notifications:
  - "None."
audit:
  - Runs are archived (FR-OP-009).
acceptance:
  - REM-TRACKER lists insurer, remit type, batch number, accounts extracted, net due, incentive and due date.
  - "REM-DTIP-DETAIL lists the outstanding DTIP per invoice with the ageing columns of Annex III #6."
```

## Adjustment / Cancellation

Adjustment processes endorsement and cancellation requests on booked invoices: financial, non-financial and internal, singly or in batches. API paths of this section are under `/api/v1/adjustment`.

```fr
id: FR-OP-050
title: Raise an endorsement or cancellation request
brd: [ADJID.001 (p.89), ADJID.002 (p.89-90), ADJID.003 (p.90), ADJID.004 (p.90-91), ADJID.020 (p.98)]
actor: Marketing Collection / TL (ADJ_REQUEST); Adjustment Processor (ADJ_PROCESS)
priority: Must have
fit: "CHANGE (ADJID.001, 003), NEW (ADJID.002, 004), FIT (ADJID.020)"
screens: New Request (wizard); Change Request; Endorsement Request page
api: POST .../requests/preview; POST .../requests; PUT .../requests/{id}; POST .../requests/{id}/submit
description:
  - "The New Request wizard has three steps: choose one or more booked invoices (up to 50; one request ENR-yyyy-n is raised per invoice); enter the request (the form adapts to the type); review the recompute (FR-OP-054) and save or submit. The ARN, invoice, policy, client, insurer, segment and AO are copied from the ledger, so every request is linked to the original account reference."
  - "The class comes from the endorsement type (list ENDORSEMENT_TYPE): financial (change of TSI, insured items, commission rate, premium rate or amount, extension of cover, change of cover, adjustment in charges, minimal balance), non-financial (descriptive changes, change of period cover, extension of period covered, extension of cover without premium, change of assured name or information) or internal adjustment."
  - "A financial request needs a request type of Annex V (list ENDORSEMENT_REQUEST_TYPE): Flat Cancellation; Flat Cancellation - Retain DST; Partial Cancellation; Increase / Decrease in TSI; Increase / Decrease of Premium Rate; VAT / Premium Tax exempt; Increase / Decrease of taxes; Change of Cover; Extension of Cover; Write-off; Decrease / Increase in Commission; Cancellation Reversal. A cancellation needs a reason (Annex V, 33 values). A non-financial request carries no request type, sum insured or amount."
  - The invoice is locked by ADJUSTMENT while a request is open (FR-OP-006); a request that reduces the invoice raises PENDING_NEG_ADJ at submission.
preconditions:
  - The user has ADJ_REQUEST or ADJ_PROCESS; the invoice is booked.
main_flow:
  - The user opens **New Request** and chooses the invoices.
  - The user chooses the endorsement type, request type, reason, effective date and description, and enters the changes.
  - BIBS shows the recompute preview.
  - The user saves the draft or submits; submitted requests move to FOR_VALIDATION.
alternate_flows:
  - The invoice is in a remittance batch (locked). BIBS refuses the request.
  - A cancellation while another financial request is open on the invoice (for example a TSI change with a partial cancellation). BIBS refuses it.
  - Batch upload of requests (ADJ_BATCH) raises one request per row (FR-OP-056).
rules:
  - [R1, "One request per invoice; the wizard raises several at once.", Fixed, "-"]
  - [R2, "Effective date within the cover of the invoice.", Fixed, "-"]
  - [R3, "Change of period keeps the term; extension moves inception and expiry.", Fixed, "-"]
  - [R4, "No request on a return invoice, a cancelled or written-off invoice.", Fixed, "-"]
  - [R5, "Type lists as the BRD (section 9.2).", Configurable, LOV ENDORSEMENT_TYPE; ENDORSEMENT_REQUEST_TYPE; CANCELLATION_REASON]
validations:
  - [No invoice, Select at least one invoice, ADJ_INVOICES_REQUIRED]
  - [Too many invoices, "Select between 1 and 50 invoices", ADJ_INVOICES_REQUIRED]
  - [Header incomplete, "Enter the endorsement type, effective date and description", ADJ_REQUEST_INCOMPLETE]
  - [Financial without request type, Select the request type of a financial endorsement, ADJ_REQUEST_TYPE_REQUIRED]
  - [Non-financial with amounts, "A non-financial endorsement carries no request type, sum insured or amount change", ADJ_NON_FINANCIAL_WITH_AMOUNTS]
  - [Cancellation without reason, Select the reason for cancellation, ADJ_CANCELLATION_REASON_REQUIRED]
  - [TSI change without amount, Enter the increase or decrease of the total sum insured, ADJ_TSI_CHANGE_REQUIRED]
  - [Other type without amounts, "Enter the premium, charges or commission change", ADJ_AMOUNTS_REQUIRED]
  - [Effective date outside cover, "The effective date must be within the cover <inception> to <expiry>", ADJ_EFFECTIVE_DATE_OUTSIDE_TERM]
  - [Invalid new period, "Enter a valid new period (a change of period keeps the term of <inception> to <expiry>)", ADJ_PERIOD_INVALID]
  - [Return invoice, "<invoice> is a return invoice: raise the request on the original", ADJ_RETURN_INVOICE]
  - [Invoice cancelled or written off, "<invoice> is cancelled or written off", ADJ_INVOICE_CLOSED]
  - [Cancellation with another open financial request, "Request <no> (<type>) is still open on <invoice>: a cancellation cannot be combined with another financial change", ADJ_INCOMPATIBLE_REQUEST]
  - [Invoice in remittance, "Invoice <no> is locked by REMITTANCE (<reason>)", INVOICE_LOCKED]
  - [Type without class, "Endorsement type <code> has no class (FINANCIAL / NON_FINANCIAL)", ADJ_TYPE_WITHOUT_CLASS]
  - [Change after submission, "<request> is <stage> and can no longer be changed", ADJ_REQUEST_NOT_EDITABLE]
fields_screen: New Request (request step)
fields:
  - [Endorsement Type, List, "Yes", LOV ENDORSEMENT_TYPE, Class from the parent]
  - [Request Type, List, Cond., LOV ENDORSEMENT_REQUEST_TYPE, Required for financial]
  - [Reason for Cancellation, List, Cond., LOV CANCELLATION_REASON, Required for cancellations]
  - [Effective Date, Date, "Yes", "-", Within the cover]
  - [Insurer Endorsement Ref., Text, "No", "-", "-"]
  - [Description, Text, "Yes", "-", "-"]
  - [Sum Insured Change, Amount, Cond., "-", Signed; required for TSI change]
  - [Premium Rate (%), Number, "No", "-", Blank = product rate]
  - [Refund / Premium Basis, List, Cond., Pro-rata / Short-period, Cancellations and TSI changes]
  - [New Inception / New Expiry, Date, Cond., "-", Period change and extension]
  - [Premium / charges / commission change, Amount, Cond., "-", Other financial types]
  - [Additional / Other Instructions, Text, "No", "-", "-"]
notifications:
  - ADJ_REQUEST_STATUS to the Adjustment team when submitted.
audit:
  - Create, change, submit and cancel are recorded with before and after values.
acceptance:
  - A flat cancellation of a booked invoice is raised with a reason and submitted; the invoice shows PENDING_NEG_ADJ and the lock.
  - A request on an invoice in a remittance batch is refused with INVOICE_LOCKED.
  - A non-financial change of assured information is raised without amounts.
  - Choosing three invoices raises three requests.
```

> [!NOTE] Differences from the BRD
> ADJID.001 allows "multiple accounts" in one transaction; BIBS raises one request per invoice so that each is validated, posted and traced on its own (R6 4.1). ADJID.003 / 004 say Marketing updates the account with a non-financial endorsement; booking records the non-financial endorsement with its description, and the account data are not changed until the ownership is agreed (OQ32).

```fr
id: FR-OP-051
title: Detect duplicate endorsement requests
brd: [ADJID.023 (p.99)]
actor: System; requester
priority: Must have
fit: NEW
screens: New Request (duplicate warning)
api: POST .../requests
description: A request is a possible duplicate when another request that is not cancelled has the same invoice, request type, reason and endorsement reference. BIBS warns and names the earlier requests; the user cancels or proceeds with a justification, which is kept on the request and in the audit trail.
preconditions:
  - "None."
main_flow:
  - The user saves or submits a request.
  - BIBS finds a possible duplicate and refuses without a justification.
  - The user gives the justification and saves again.
rules:
  - [R1, "Duplicate keys - invoice, request type, reason, endorsement reference.", Fixed, "-"]
validations:
  - [Possible duplicate, "Possible duplicate of <requests> (same invoice, request type, reason and endorsement reference): give the justification to proceed", ADJ_DUPLICATE_REQUEST]
notifications:
  - "None."
audit:
  - The override justification is recorded.
acceptance:
  - A second flat cancellation of the same invoice with the same reason is refused until a justification is given.
```

```fr
id: FR-OP-052
title: Attach supporting documents to endorsement requests
brd: [ADJID.025 (p.100)]
actor: Requester; Adjustment Processor
priority: Must have
fit: FIT
screens: Endorsement Request page (Documents)
api: /api/v1/attachments (entity EndorsementRequest)
description: Supporting documents are attached to the request with a document type from ENDORSEMENT_DOC_TYPE. The platform checks file type, size and integrity (checksum) and allows several files.
preconditions:
  - The user has ATTACHMENT_MANAGE.
main_flow:
  - The user opens the Documents tab and uploads files with their type.
  - BIBS stores and lists them with uploader and time.
rules:
  - [R1, "The document list is blank in the BRD; only 'Other supporting document' until BDOI lists them (OQ32).", Configurable, LOV ENDORSEMENT_DOC_TYPE]
validations:
  - [File type or size not allowed, "The file type or size is not allowed", "-"]
notifications:
  - "None."
audit:
  - Uploads and deletions are audited.
acceptance:
  - A PDF endorsement copy uploaded to a request is listed and opens from the Documents tab.
```

```fr
id: FR-OP-053
title: Validate, approve or return endorsement requests
brd: [ADJID.005 (p.91-92), ADJID.007 (p.92), ADJID.010 (p.94)]
actor: Adjustment Processor (validate, return); Adjustment TL (approve)
priority: Must have
fit: "NEW (ADJID.005, 010), CHANGE (ADJID.007)"
screens: Adjustment Workbench (tabs per stage); Endorsement Request page (workflow panel); Posting Batches
api: POST .../requests/{id}/validate, /approve, /resubmit; POST .../batches/return
description:
  - Requests follow workflow OPS_ENDORSEMENT (section 5). The processor validates a submitted request for approval or, when it has no financial effect, directly for posting. The TL approves it. Extension of cover with additional premium (FIN_EXTENSION) always needs approval.
  - A request can be returned from validation, approval or the posting batch with a reason from ADJ_RETURN_REASON; returned requests are excluded from posting, flagged and the requester is notified. Posted requests cannot be returned. The requester corrects and resubmits, or cancels.
preconditions:
  - The request is in the stage of the action; the user has its permission.
main_flow:
  - The processor opens the request FOR_VALIDATION and checks it.
  - The processor clicks **Validate for approval** (or **Validate for posting**).
  - The TL clicks **Approve**; the request moves to FOR_POSTING.
alternate_flows:
  - Return with a reason; the requester is notified (RETURNED).
  - Several requests are returned from Posting Batches in one action, each with its reason.
rules:
  - [R1, "The approver never raised, submitted or validated the request.", Fixed, "-"]
  - [R2, "Return reasons from the list.", Configurable, LOV ADJ_RETURN_REASON]
validations:
  - [Approver involved earlier, A request is approved by someone who did not raise or validate it, ADJ_FOUR_EYES]
  - [Return without reason, Select the return reason, ADJ_RETURN_REASON_REQUIRED]
  - [Action in the wrong stage, "<request> is <stage>, not <expected>", ADJ_WRONG_STAGE]
fields_screen: Return request
fields:
  - [Reason, List, "Yes", LOV ADJ_RETURN_REASON, "-"]
  - [Comment, Text, Cond., "-", Required for Others]
notifications:
  - ADJ_REQUEST_STATUS to the team of the next stage, and to the requester on return, posting and cancellation.
audit:
  - Each transition is in the request history with reason and comment.
acceptance:
  - The processor who validated a request cannot approve it.
  - An extension of cover with additional premium cannot skip the approval.
  - A request returned from the posting batch is not posted and the requester sees the reason.
```

```fr
id: FR-OP-054
title: Recompute premium, commission and refund per insurer (addendum)
brd: [ADJID.014 (p.95-96 and addendum p.5)]
actor: System
priority: Must have
fit: CHANGE
screens: New Request (Recompute step); Endorsement Request page (Recompute)
api: POST .../requests/preview; GET .../requests/{id}/recompute
description:
  - BIBS recomputes the premium, commission, refund premium and sum insured by request type. Cancellations use booking's own posting preview, so the preview equals the posting. A TSI change is rated once per insurer share with the catalogue calculator in endorsement mode (remaining term pro-rata or short-period, each insurer at its commission rate, the lead with its branch LGT). Other types take the entered component changes (commission derived from the invoice rate when blank).
  - The before and after values per component and the change per insurer are stored on the request at every save, submission, validation and posting, so the original values are never overwritten.
  - When the change affects BDOI income or commission, posting issues a service invoice, or credits the insurer's service invoice for a decrease, through booking (addendum).
preconditions:
  - The request has its type and changes.
main_flow:
  - The user enters the request.
  - BIBS shows the before / after table and the breakdown per insurer with the service invoice, payment and remittance effects.
  - BIBS stores the result on the request.
alternate_flows:
  - An insurer has no commission configuration. The recompute says which insurer is missing.
rules:
  - [R1, "Refund basis pro-rata or short-period, chosen per request (OQ36).", Configurable, Request field]
  - [R2, "Service invoice on an income or commission change; credit of the insurer's service invoice on a decrease (OQ34).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - Before / after per component and the insurer breakdown are kept for every stage.
acceptance:
  - A decrease in TSI of a 60/40 co-insured invoice shows the premium and commission change per insurer.
  - A commission increase without premium change issues a service invoice at posting.
  - The recompute of a flat cancellation equals the amounts posted by booking.
```

```fr
id: FR-OP-055
title: Handle a TSI increase above the package limit
brd: [ADJID.008 (p.92-93)]
actor: System; Marketing AO (quotation)
priority: Must have
fit: NEW
screens: New Request; Endorsement Request page (Quotation No.)
api: POST .../requests/{id}/quotation
description: A TSI increase is checked against the package TSI limit of the product in the catalogue. When the increase takes the account above the limit, submission opens a hand-off QUOTATION_REQUIRED for Marketing (QUOTE_MAINTAIN) to prepare a quotation, and validation waits until the quotation number is linked on the request. The added insurer and the co-insurance shares follow the quotation; non-package shares are recomputed on the endorsed sum insured and remaining period (FR-OP-054).
preconditions:
  - The request is a TSI increase.
main_flow:
  - The user submits the TSI increase.
  - BIBS finds the limit exceeded and opens the hand-off to Marketing.
  - Marketing prepares the quotation; the user enters the **Quotation No.** on the request.
  - The processor validates the request.
rules:
  - [R1, "Limit from the catalogue product (max_sum_insured); insurer capacity per product to be confirmed (OQ33).", Configurable, Catalogue]
validations:
  - [Validation before the quotation, "The TSI increase exceeds the package limit: link the quotation prepared by Marketing first (ADJID.008)", ADJ_QUOTATION_REQUIRED]
  - [Link without number, Enter the quotation number, ADJ_QUOTATION_REF_REQUIRED]
  - [Link on a request that needs none, "<request> needs no quotation link", ADJ_QUOTATION_NOT_REQUIRED]
notifications:
  - The hand-off appears to Marketing on Hand-offs and Extracts.
audit:
  - The linked quotation number is recorded.
acceptance:
  - A TSI increase above the package limit cannot be validated before the quotation number is linked.
  - A TSI increase within the limit needs no quotation.
```

> [!NOTE] Difference from the BRD
> ADJID.008 asks to prompt the AO to prepare a quotation. The quotation module has no endorsement quotation call, so BIBS opens a hand-off and links the quotation by its number.

```fr
id: FR-OP-056
title: Post requests singly or in batches
brd: [ADJID.006 (p.92), ADJID.011 (p.94)]
actor: Adjustment Processor (ADJ_POST)
priority: Must have
fit: CHANGE
screens: Posting Batches; Batch Request Upload; Endorsement Request page (Accounting)
api: POST .../batches; GET .../batches; GET .../batches/{no}; POST .../requests/{id}/post; GET .../requests/{id}/journal; bulk ADJ_BATCH
description:
  - The processor selects requests FOR_POSTING and posts them as a validation batch VB-yyyy-n; each request is posted in its own transaction and a failure does not stop the others. The batch result shows posted, payments to re-apply and failed requests. Requests can also be uploaded in a batch file (ADJ_BATCH); re-uploading the same request number does not create duplicates.
  - "Posting: financial endorsements and cancellations are posted through booking's endorsement posting with source ADJ:<request> (endorsement invoice EN-yyyy-n or return invoice, journal and open items; the default entries per type are Comptrollership rules); non-financial endorsements are recorded by booking without GL; internal requests without financial effect post nothing. A decrease or cancellation records ADJUSTED movements on the original invoice; cancellations set the CANCELLED flag."
preconditions:
  - The requests are FOR_POSTING.
main_flow:
  - The processor opens Posting Batches, selects requests and clicks **Post selected**.
  - BIBS posts each request and shows the batch result.
  - The Accounting tab of each request shows its journal.
alternate_flows:
  - A request fails. It stays FOR_POSTING with the message; the others are posted.
  - Return selected requests before posting (FR-OP-053).
rules:
  - [R1, "GL entries per endorsement type are Comptrollership rules (OQ07).", Configurable, Accounting rules]
  - [R2, "Posting is idempotent on the request.", Fixed, "-"]
validations:
  - [Nothing selected, Select the requests to post, ADJ_BATCH_EMPTY]
  - [Posting failed, "<message of the failed request>", ADJ_POSTING_FAILED]
notifications:
  - ADJ_REQUEST_STATUS to the requester on posting.
audit:
  - The batch, each request result and the journal references are kept.
acceptance:
  - A batch of three requests posts all three; a fourth with a locked invoice fails without stopping the others.
  - The Validation List (FR-OP-062) shows the GL lines of the posted requests.
```

```fr
id: FR-OP-057
title: Set up excess payments, re-apply payments and AR Insurer
brd: [ADJID.009 (p.93-94), ADJID.012 (p.94-95), ADJID.013 (p.95)]
actor: System; Adjustment Processor
priority: Must have
fit: NEW
screens: Endorsement Request page; Adjustment Workbench (Payments to Re-apply); Unapplied Payments
api: POST .../requests/{id}/post, /reapply
description:
  - After a decrease or cancellation of a paid invoice, BIBS releases the lock and asks Cashiering to re-apply the payments against the reduced premium. Cashiering reverses the applications above the new premium and applies the money again in hierarchy order; the excess becomes one unapplied item (origin REAPPLY) for disposition by Marketing and Cashiering (apply to another invoice or refund, FR-OP-022). Realised commission is reversed with the applications.
  - When the invoice was already remitted and its DTIP becomes negative, BIBS sets up the amount (at most the remitted DTIP) as AR Insurer per insurer share (OPS_AR_INSURER_SETUP) and keeps PENDING_NEG_ADJ so that Remittance offsets or excludes the invoice.
  - When Cashiering is not available the request moves to AWAITING_REAPPLICATION ("Payments to Re-apply") and **Re-apply Payments** retries later.
preconditions:
  - A decrease or cancellation is posted on a paid or remitted invoice.
main_flow:
  - BIBS posts the request (FR-OP-056).
  - BIBS calls Cashiering to re-apply the payments of the invoice.
  - BIBS records the excess and its unapplied item on the request.
  - For a remitted invoice, BIBS sets up the AR Insurer.
alternate_flows:
  - Re-application unavailable. The request waits in AWAITING_REAPPLICATION.
rules:
  - [R1, "Re-application is idempotent per request and invoice.", Fixed, "-"]
  - [R2, "AR Insurer never exceeds the DTIP remitted.", Fixed, "-"]
validations:
  - [Re-application unavailable, "Payments can only be re-applied once Cashiering is available; the request stays awaiting re-application", PAYMENT_REAPPLIER_UNAVAILABLE]
notifications:
  - The unapplied item appears in the Unapplied Payments workbench.
audit:
  - The excess, unapplied reference and AR Insurer journal are kept on the request.
acceptance:
  - Cancelling a fully paid, not remitted invoice of 10,000.00 creates an unapplied item of 10,000.00.
  - Cancelling a remitted invoice sets up AR Insurer equal to the remitted DTIP and keeps PENDING_NEG_ADJ.
```

```fr
id: FR-OP-058
title: Control over-adjustment
brd: [ADJID.028 (p.103)]
actor: System; requester
priority: "Not stated in the BRD"
fit: NEW
screens: New Request (baseline justification); Invoice 360 (adjustment totals)
api: POST .../requests/{id}/submit
description: BIBS keeps the cumulative adjustments of each original invoice against its original premium, DTIP and commission. When a request would take the cumulative adjustments above the baseline percent of the original premium, or below zero, submission needs a justification and raises the alert ADJ_OVER_BASELINE. Previous adjustments are shown before the request is submitted; duplicates are blocked by FR-OP-051.
preconditions:
  - "None."
main_flow:
  - The user submits a request.
  - BIBS adds the request to the cumulative adjustments and compares with the baseline.
  - Above the baseline, BIBS asks for the justification and raises the alert.
rules:
  - [R1, "Baseline 100% of the original premium (default, OQ37).", Configurable, Parameter ADJ_BASELINE_PERCENT]
validations:
  - [Above the baseline without justification, "Cumulative adjustments of <amount> exceed the baseline of <n>% of the original premium <amount>", ADJ_OVER_BASELINE]
notifications:
  - ADJ_OVER_BASELINE alert to the Adjustment TL.
audit:
  - The justification and the cumulative totals are recorded.
acceptance:
  - A second premium increase that takes the adjustments above 100% of the original premium needs a justification.
  - A decrease that would make the premium negative is refused without justification.
```

```fr
id: FR-OP-059
title: Write off minimal balances from a file
brd: [ADJID.026 (p.101-102)]
actor: Adjustment Processor (ADJ_POST)
priority: Must have
fit: NEW
screens: Minimal Balance File; Report ADJ-MINBAL-FILE
api: bulk MINIMAL_BALANCE_FILE; GET .../write-offs
description: The processor uploads a file of invoices with minimal balances (columns Invoice No and Balance). A row is processed when the invoice exists, its PR balance equals the file, lies within 10.00-100.00, was not written off and no other module locks it. A debit balance is written off and a credit balance credited (outcomes WRITE_OFF / CREDIT) with the event OPS_WRITE_OFF, a WRITE_OFF movement and the WRITTEN_OFF flag, once per invoice. The run lists the rows not processed with their reason; ADJ-MINBAL-FILE summarises the file.
preconditions:
  - The user has ADJ_POST.
main_flow:
  - The processor uploads the file.
  - BIBS checks each row and writes off or credits the balance.
  - BIBS shows the run summary and the written-off list.
alternate_flows:
  - Duplicate file. Refused (BULK_DUPLICATE_FILE).
rules:
  - [R1, "Range 10.00-100.00 (OQ11).", Configurable, Parameter MIN_BALANCE_FILE_RANGE]
  - [R2, "An invoice is written off once.", Fixed, "-"]
validations:
  - [Balance outside range, "<invoice> has a balance of <amount>, outside the file range", ADJ_BALANCE_OUT_OF_RANGE]
  - [No PR balance, "<invoice> has no premium receivable balance", ADJ_NOTHING_TO_WRITE_OFF]
  - [Invoice locked, "Invoice <no> is locked by <owner>", INVOICE_LOCKED]
notifications:
  - "None."
audit:
  - Each processed invoice is recorded once with the file.
acceptance:
  - A balance of 50.00 in the file is written off and Remittance then refuses the invoice as WRITTEN_OFF.
  - A balance of 150.00 is refused with ADJ_BALANCE_OUT_OF_RANGE.
```

```fr
id: FR-OP-060
title: Generate the endorsement slip and the validation slip
brd: [ADJID.015 (p.96), ADJID.018 (p.97-98)]
actor: Marketing; Adjustment Processor
priority: Must have
fit: NEW
screens: Endorsement Request page (Endorsement Slip, Validation Slip)
api: GET .../requests/{id}/endorsement-slip; GET .../requests/{id}/validation-slip
description:
  - "The endorsement slip (template ENDORSEMENT_SLIP) is a PDF with the Annex V fields: date, insurer and co-insurers, endorsement request number, invoice number, request type, reason for cancellation, effective date, additional instructions, assured, risk code and description, policy number, period of cover, Marketing AO, market segment, total sum insured and its change, premium rate, payment status (outstanding, fully or partially paid with amount, date and remaining AR), remittance status, approved by. It is numbered once ES-yyyy-n and is not produced for internal adjustments."
  - The validation slip (template VALIDATION_SLIP) summarises a validated request with the validation status, before / after, insurer breakdown and GL entries; it is not produced before validation.
preconditions:
  - Endorsement slip - the request is external (not internal). Validation slip - the request passed validation.
main_flow:
  - The user opens the request and clicks **Endorsement Slip** or **Validation Slip**.
  - BIBS generates the PDF for download or print.
rules:
  - [R1, "Slip number assigned once and kept.", Fixed, "-"]
validations:
  - [Slip for an internal adjustment, Internal adjustments have no endorsement slip (ADJID.015), ADJ_NO_SLIP_FOR_INTERNAL]
  - [Validation slip before validation, "<request> has not passed validation yet", ADJ_NOT_VALIDATED]
notifications:
  - "None."
audit:
  - Slip generation is recorded with its number.
acceptance:
  - The endorsement slip of a flat cancellation shows the Annex V fields and a number ES-2026-n; regenerating it keeps the number.
  - An internal adjustment has no endorsement slip.
```

```fr
id: FR-OP-061
title: Search requests, view history and ageing
brd: [ADJID.021 (p.98-99), ADJID.022 (p.99), ADJID.024 (p.100)]
actor: Adjustment users; Marketing
priority: Must have
fit: CHANGE
screens: Adjustment Workbench; Endorsement Request page (History); Invoice Search and Invoice 360 (Adjustments)
api: GET .../requests; GET .../requests/{id}; GET .../invoices/{no}/requests; GET /api/v1/ops/invoices/{no}
description:
  - The Adjustment Workbench lists requests by stage tab with counts, searches by request, invoice, ARN, policy or assured, and shows the ageing of each request from submission (or creation) to completion in Philippine days, with the flags.
  - The request page keeps the complete history with the old and new details, status and remarks. From an ARN the user opens Invoice 360 with the payment and remittance history and the requests of the invoice (auto-complete on ARN).
preconditions:
  - The user has an ADJ_* permission or OPS_VIEW.
main_flow:
  - The user searches or opens a stage tab.
  - The user opens a request and its History tab.
rules: []
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - A request submitted 3 days ago shows ageing 3 in the workbench.
  - The History tab shows the before and after values of each change.
  - Searching an ARN lists its requests and opens Invoice 360.
```

```fr
id: FR-OP-062
title: Generate the Adjustment reports
brd: [ADJID.016 (p.96), ADJID.017 (p.97), ADJID.019 (p.98)]
actor: Adjustment users; Comptrollership
priority: Must have
fit: NEW
screens: Reports (category Operations)
api: /api/v1/reports (ADJ-*); job ADJ_DAILY_REPORT
description:
  - "ADJ-DAILY: Adjustment and Daily Endorsement Report of the requests raised or posted in the period, by type and user. The job ADJ_DAILY_REPORT (18:00 PHT) exports it as Excel for the business date, archives it and notifies the holders of ADJ_APPROVE."
  - "ADJ-VALIDATION-LIST: posted requests with one row per GL line - type of cancellation / adjustment, invoice, endorsement reference and request number, segment, requesting AO, policy, assured, insurer, GL code, debit / credit, reason, validation date, validation batch number."
  - "ADJ-REGISTER: Adjustment Report filtered by date, account, segment, AO and risk type. ADJ-AGING: ageing with buckets (ADJID.021)."
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user chooses the report and parameters and generates it.
rules:
  - [R1, "Layouts not given by the BRD are drafts (OQ42).", Configurable, Report layouts]
validations: []
notifications:
  - The daily report notification to the Adjustment TL.
audit:
  - Runs are archived.
acceptance:
  - The Validation List of a day matches the journals of the requests posted that day.
  - The daily report is archived every business day at 18:00.
```


## Production Reconciliation

Production Reconciliation sends each insurer a register of the accounts BDOI booked with it, takes in the insurer's answer, matches both sides and follows every difference to closure. The work is organised in cycles: one open cycle per insurer and production month (workflow OPS_RECON, section 5). API paths of this section are under `/api/v1/prodrecon`.

```fr
id: FR-OP-070
title: Schedule the automatic extraction of the production register
brd: [PRCID.001 (p.79)]
actor: Recon Handler (schedules); System (job PRODUCTION_EXTRACT)
priority: Must have
fit: CHANGE
screens: Extract Schedules
api: GET / POST .../schedules; PUT .../schedules/{id}
description: Each insurer has a schedule with its frequency (monthly on a day of the month, or weekly on a weekday), whether the register is sent automatically, and the recipients. The job PRODUCTION_EXTRACT (daily 09:00 PHT) extracts the booked accounts of every insurer due that day. A run date that falls on a holiday moves to the next working day of the head office calendar.
preconditions:
  - The user has RECON_PROCESS to maintain schedules.
main_flow:
  - The handler adds a schedule for an insurer.
  - On the run date the job extracts the register and opens or updates the cycle of the month.
  - When automatic sending is on, the register is sent (FR-OP-073).
rules:
  - [R1, "Frequency per insurer (values from BDOI, OQ29).", Configurable, Extract Schedules]
  - [R2, "Holidays roll to the next working day.", Fixed, "-"]
validations:
  - [No company or insurer, Choose the company and the insurer of the schedule, RECON_SCHEDULE_INSURER]
fields_screen: Extract Schedule
fields:
  - [Insurer Code, Look-up, "Yes", Insurer master, "-"]
  - [Frequency, List, "Yes", Monthly / Weekly, "-"]
  - [Run Day, Number, "Yes", "-", "Day of month 1-28 or weekday"]
  - [Recipients, Text, "No", "-", E-mail addresses; blank = insurer's e-mail]
notifications:
  - "None."
audit:
  - Schedule changes are audited.
acceptance:
  - A monthly schedule on day 5 extracts on 5 October; when 5 October is a holiday it extracts on the next working day.
```

```fr
id: FR-OP-071
title: Extract the production register manually and view it
brd: [PRCID.005 (p.80), PRCID.011 (p.81), PRCID.012 (p.82), PRCID.013 (p.82), PRCID.020 (p.83), PRCID.034 (p.86)]
actor: Recon Handler
priority: Must have
fit: NEW
screens: Production Extracts (New Extract); Reconciliation Cycle (Registers Sent); Report PRC-EXTRACT-LOG
api: POST / GET .../extracts; GET .../extracts/{id}/lines; GET .../extracts/{id}/file; GET .../cycles/{id}/extracts
description:
  - The handler extracts the register of an insurer for a booking period (PRX-yyyy-n). Each line records the ledger facts of the booked account, the last payment and the estimated flag; an account not on an earlier register of the cycle counts as new. The extract shows its file name, location in the extract repository (folder PRODRECON/<insurer>), date and time, creator and row count.
  - Extracts and uploaded insurer lines are viewed per cycle, filtered by production month and insurer. PRC-EXTRACT-LOG reports the item count and extraction time of each extract.
preconditions:
  - The user has RECON_PROCESS.
main_flow:
  - The handler clicks **New Extract** and enters the insurer and booking period (or production month).
  - BIBS extracts the booked accounts of the period and stores the file.
  - The handler opens the extract lines or downloads the file.
rules:
  - [R1, "Only accounts booked in the selected period are extracted.", Fixed, "-"]
validations:
  - [Period end before start, The booking period ends before it starts, RECON_EXTRACT_PERIOD]
  - [Nothing booked, "No invoice was booked with <insurer> from <from> to <to>", RECON_NOTHING_TO_EXTRACT]
fields_screen: New Extract
fields:
  - [Insurer Code, Look-up, "Yes", Insurer master, "-"]
  - [Production Month or booking period, Month / Dates, "Yes", "-", From <= To]
notifications:
  - "None."
audit:
  - Each extract is kept with its file, creator, time and count.
acceptance:
  - A manual extract of INS-MGIC for September lists only accounts booked in September.
  - The extract shows its file name, date, time and row count, and PRC-EXTRACT-LOG shows the same count.
```

```fr
id: FR-OP-072
title: Produce the register file in the agreed layout
brd: [PRCID.002 (p.79), PRCID.004 (p.79-80), PRCID.006 (p.80), PRCID.007 (p.80)]
actor: System
priority: Must have
fit: "CHANGE (PRCID.002), CONFIGURE (PRCID.004, 006), FIT (PRCID.007)"
screens: Production Extracts (download)
api: GET .../extracts/{id}/file
description:
  - "The register is an XLSX workbook in the Production Register layout of Annex IV #5: month of production, insurer, invoice number, booking, inception and expiry dates, policy number, endorsement number, PN number, assured name, insurance code, risk type, basic premium, gross commission, A/R client, gross premium, booked VAT, amount paid, date paid, OR number, adjustment type, remittance status."
  - The sheet is protected; only the Remarks and Incentive columns and 200 blank rows are editable by the insurer. The file is named by the naming pattern and protected by a password when it is e-mailed (FR-OP-073).
preconditions:
  - An extract exists.
main_flow:
  - BIBS builds the workbook from the extract lines.
  - BIBS names it and stores it in the extract repository.
rules:
  - [R1, "File name pattern, default <INSURER>_PRODREG_<yyyyMM>_<seq>.", Configurable, Parameter PRODRECON_FILE_PATTERN]
  - [R2, "Template per insurer when needed (OQ29).", Configurable, Document templates]
  - [R3, "Password convention to be confirmed (BRD-1 Q07); a password per e-mail until then.", Configurable, Messaging settings]
validations: []
notifications:
  - "None."
audit:
  - The file and its name are kept with the extract.
acceptance:
  - In the downloaded register every column except Remarks and Incentive is locked.
  - Two extracts of the same insurer and month get different file names.
```

> [!NOTE] Differences from the BRD
> PRCID.002 keeps only the insurer's Remarks column editable. BIBS also leaves the Incentive column and 200 blank rows editable, so that the insurer can report its early incentives and add production that BDOI did not book (PRCID.022). The Sum Insured column of Annex IV #5 is left out: the invoice ledger does not hold the sum insured.

```fr
id: FR-OP-073
title: Send the register to the insurer with a cover letter
brd: [PRCID.003 (p.79), PRCID.008 (p.81)]
actor: Recon Handler (RECON_SEND)
priority: Must have
fit: "CONFIGURE (PRCID.003), FIT (PRCID.008)"
screens: Production Extracts (Send); Reconciliation Cycle (workflow panel)
api: POST .../extracts/{id}/send
description: The handler sends the register by e-mail with the cover letter template PRODRECON_COVER_LETTER (insurer name, extract number, booking period, production month, row count, instructions to return the remarks). The attachment is password protected and the password is sent in a separate e-mail. When no recipient is given, the insurer's placement e-mail from the catalogue is used. BIBS stores the sent time and recipients on the extract and moves the cycle to SENT_TO_INSURER.
preconditions:
  - The user has RECON_SEND; the cycle is EXTRACTED, SENT_TO_INSURER or RECONCILING.
main_flow:
  - The handler clicks **Send to Insurer** and confirms the recipients.
  - BIBS e-mails the protected register and the password.
  - BIBS records the sent time and moves the cycle.
alternate_flows:
  - Resend. The handler resends a register; the new sent time is recorded.
rules:
  - [R1, "Cover letter text is a template (texts from BDOI, OQ29).", Configurable, Document template PRODRECON_COVER_LETTER]
validations:
  - [No recipient, "Enter the insurer's e-mail address: <insurer> has no placement e-mail in the catalog", RECON_NO_RECIPIENT]
fields_screen: Send to Insurer
fields:
  - [To, Text, "Yes", Insurer e-mail, Valid e-mail]
  - [Cc, Text, "No", "-", Valid e-mail]
notifications:
  - E-mails to the insurer; the send log keeps them.
audit:
  - Sent time and recipients are stored on the extract.
acceptance:
  - The insurer receives the register as a protected file and the password in a second e-mail.
  - The extract shows the sent date and time.
```

```fr
id: FR-OP-074
title: Upload the insurer production report
brd: [PRCID.009 (p.81), PRCID.010 (p.81), PRCID.022 (p.83-84), PRCID.031 (p.85-86), PRCID.032 (p.86)]
actor: Recon Handler
priority: Must have
fit: "CHANGE (PRCID.009, 010, 032), NEW (PRCID.022), FIT (PRCID.031)"
screens: Insurer Feedback (upload, attempts); Interfaces (feed INSURER_PRODUCTION)
api: POST / GET .../uploads; feed INSURER_PRODUCTION
description:
  - The handler uploads the insurer's production report or matched file (the returned register). BIBS validates the layout and each row and matches the file at once (FR-OP-075). Lines of accounts that were not in the original extract are kept apart as insurer-only lines (unbooked, FR-OP-076).
  - A file with the same content as an earlier upload is refused (DUPLICATE_BLOCKED) and raises RECON_UPLOAD_DUPLICATE. Every attempt is recorded with its number, user, time, row counts and status (successful / unsuccessful).
preconditions:
  - The user has RECON_PROCESS.
main_flow:
  - The handler uploads the file on Insurer Feedback.
  - BIBS checks for a duplicate, reads the rows and matches them in the cycle of the insurer and month.
  - BIBS shows the attempt with its status and counts; the cycle moves to RECONCILING.
alternate_flows:
  - Duplicate file. Refused; the attempt is recorded.
  - Layout wrong. Refused with the missing column.
rules:
  - [R1, "Upload by the handler; automatic pick-up from mailbox or SFTP is parked (InsurerFileInbox).", Fixed, "-"]
  - [R2, "Duplicate by file content (SHA-256).", Fixed, "-"]
validations:
  - [Duplicate file, "Upload refused: <earlier upload>", RECON_UPLOAD_DUPLICATE]
  - [Column missing, "The file is not in the production register layout: column '<column>' missing", RECON_FILE_LAYOUT]
  - [Row without keys, "Row <n> has neither invoice nor policy number", RECON_ROW_INCOMPLETE]
  - [Row without insurer or month, "Row <n> has no insurer or production month (yyyy-MM)", RECON_ROW_INCOMPLETE]
  - [Bad amount, "Row <n>: <column> '<value>' is not an amount", RECON_ROW_AMOUNT]
  - [Bad date, "Row <n>: <column> '<value>' is not a date", RECON_ROW_DATE]
  - [Unknown company, "Upload the production of <insurer> from Production Reconciliation - Uploads (company not known)", RECON_COMPANY_UNKNOWN]
notifications:
  - RECON_FEEDBACK_UPLOADED to the handlers.
audit:
  - Each attempt is kept with its flow-in run and records.
acceptance:
  - Uploading the returned register matches it and lists the insurer-only lines separately.
  - Uploading the same file again is refused and counted as a new attempt.
```

> [!NOTE] Difference from the BRD
> PRCID.009 asks to receive and upload the insurer report automatically. Insurer channels are not specified (OQ29); the handler uploads the file, and the InsurerFileInbox seam takes a mailbox or SFTP later without a change to the matching.

```fr
id: FR-OP-075
title: Match booked and insurer lines within tolerance
brd: [PRCID.024 (p.84), PRCID.025 (p.84), PRCID.026 (p.84), PRCID.027 (p.84-85), PRCID.030 (p.85)]
actor: System; Recon Handler (re-match)
priority: Must have
fit: NEW
screens: Reconciliation Cycle (Items by bucket, side-by-side comparison)
api: POST .../cycles/{id}/automatch; GET .../cycles/{id}/items; GET .../settings; job RECON_AUTOMATCH
description:
  - "BIBS pairs each insurer line with a booked line using the keys of RECON_MATCH_KEYS in order (default invoice number, then policy number). It then compares the fields: policy, reference / invoice and PN numbers and the policy period must be equal; the assured name is compared ignoring case and spacing; commission, basic premium and gross premium match when they differ by 1.00 or less, whichever side is higher."
  - "Status of each item: MATCHED; MATCHED_WITH_DISCREPANCY (the differing fields are listed); BDOI_ONLY (booked, not returned by the insurer); UNMATCHED_PREBOOKED; UNMATCHED_NO_BOOKING. Each status is a bucket tab with its count; a discrepancy row expands to a side-by-side comparison with the tolerance highlighted."
  - Matching runs on every upload, when a later invoice is booked (waiting insurer-only lines of the same insurer), and by **Match Again** or the job RECON_AUTOMATCH for every open cycle.
preconditions:
  - A cycle has extract lines and insurer lines.
main_flow:
  - BIBS pairs the lines by key.
  - BIBS compares the fields and tags each item.
  - The handler reviews the buckets.
rules:
  - [R1, "Tolerance 1.00 per amount field.", Configurable, Parameter RECON_TOLERANCE]
  - [R2, "Match keys and order.", Configurable, Parameter RECON_MATCH_KEYS (OQ30)]
  - [R3, "Automatch schedule manual until BDOI gives the frequency.", Configurable, Job RECON_AUTOMATCH (OQ30)]
validations:
  - [Action on a closed cycle, "Cycle <no> is closed", RECON_CYCLE_CLOSED]
notifications:
  - "None."
audit:
  - Each item keeps its match method (AUTO or MANUAL), status history and differences.
acceptance:
  - A gross premium difference of 0.80 is MATCHED; a difference of 1.20 is MATCHED_WITH_DISCREPANCY listing gross premium.
  - An account the insurer did not return is BDOI_ONLY.
  - Booking the invoice of a waiting insurer-only line matches it automatically.
```

> [!NOTE] Difference from the BRD
> PRCID.030 names four statuses. BIBS adds BDOI_ONLY for booked accounts the insurer did not return, so that they can be followed like the other differences.

```fr
id: FR-OP-076
title: Follow unbooked and pre-booked insurer production
brd: [PRCID.019 (p.83), PRCID.023 (p.84), PRCID.033 (p.86)]
actor: System; Recon Handler
priority: Must have
fit: NEW
screens: Unbooked Accounts; Report PRC-UNBOOKED
api: GET .../unbooked
description: For each insurer-only line BIBS searches New Business for a pre-booked account (ARN) with the same reference. The line becomes UNMATCHED_PREBOOKED when one is found and UNMATCHED_NO_BOOKING otherwise. The Unbooked Accounts repository lists the unmatched and duplicate lines with their unbooked status (OPEN, PREBOOKED, BOOKED, CLOSED), feedback and disposition, and is searchable and filterable. PRC-UNBOOKED lists the unbooked accounts and their status.
preconditions:
  - "None."
main_flow:
  - An insurer upload creates insurer-only lines.
  - BIBS looks up pre-booked accounts and tags each line.
  - When the account is booked, BIBS matches it (FR-OP-075) and the unbooked status becomes BOOKED.
rules: []
validations: []
notifications:
  - "None."
audit:
  - Status changes are kept per item.
acceptance:
  - An insurer policy whose ARN is pre-booked shows UNMATCHED_PREBOOKED with the ARN.
  - After booking, the line leaves the open list and shows BOOKED.
```

```fr
id: FR-OP-077
title: Review items, record feedback and pair manually
brd: [PRCID.014 (p.82), PRCID.015 (p.82), PRCID.016 (p.82), PRCID.021 (p.83)]
actor: Recon Handler
priority: Must have
fit: "NEW (PRCID.014, 015, 021), CONFIGURE (PRCID.016)"
screens: Reconciliation Cycle (Items, filters, review dialog, bulk disposition)
api: GET .../cycles/{id}/items; PUT .../items/{id}/feedback; POST .../items/feedback; POST .../items/{id}/pair, /split
description:
  - The items filter by status bucket, text, AO / AB, sales unit, market segment and product line (booked and pre-booked). The handler records on an item the company concerned (list with Others), an instruction or notation, the insurer feedback, the Marketing feedback, the disposition and the for-closure mark, singly or in bulk.
  - A BDOI-only item can be paired manually with an insurer-only item of the same cycle, and a wrong pairing can be split back into its two sides.
preconditions:
  - The user has RECON_PROCESS; the cycle is not closed.
main_flow:
  - The handler filters the items.
  - The handler opens an item and records the feedback and disposition.
  - BIBS saves them with user and time.
alternate_flows:
  - Bulk. The handler selects items and sets the disposition for all.
  - Manual pair or split.
rules:
  - [R1, "Company-concerned and disposition lists (values from BDOI, OQ31; demo values delivered).", Configurable, LOV RECON_COMPANY_CONCERNED; LOV RECON_DISPOSITION]
validations:
  - [Pair across cycles or wrong sides, Pair a BDOI-only item with an insurer-only item of the same cycle, RECON_PAIR_INVALID]
  - [Split of an unpaired item, Only a paired item can be split, RECON_SPLIT_INVALID]
fields_screen: Item review
fields:
  - [Company Concerned, List, "No", LOV RECON_COMPANY_CONCERNED, Others needs the instruction]
  - [Instruction, Text, "No", "-", "-"]
  - [Insurer Feedback, Text, "No", "-", "-"]
  - [Marketing Feedback, Text, "No", "-", "-"]
  - [Disposition, List, "No", LOV RECON_DISPOSITION, "-"]
  - [For closure, Check box, "No", "-", Insurer confirmed closure]
notifications:
  - "None."
audit:
  - Feedback changes are audited with before and after values.
acceptance:
  - Filtering by AO lists only that AO's items.
  - The disposition set in bulk on five items appears on each item and on PRC-DISPOSITION.
```

> [!NOTE] Difference from the BRD
> PRCID.021 asks to filter by BDOI location. The items filter by AO, sales unit, segment and product line; the location is available on the report PRC-UNMATCHED-LOC.

```fr
id: FR-OP-078
title: Validate early remittance incentives claimed by insurers
brd: [PRCID.028 (p.85)]
actor: Recon Handler
priority: Must have
fit: NEW
screens: Reconciliation Cycle (Early Incentive); Report PRC-EARLY-INCENTIVE
api: GET .../cycles/{id}/early-incentive
description: For each booked account of the cycle, BIBS checks the remittance date against the insurer's incentive rate and window kept by Remittance (FR-OP-038). The account is ELIGIBLE when remitted within the window (30 days from inception for CLG / CBG motor and fire at 2% in the BRD example), NOT_ELIGIBLE when remitted late, and NO_RULE when no rule covers it.
preconditions:
  - The cycle has booked accounts.
main_flow:
  - The handler opens the Early Incentive tab.
  - BIBS lists each account with rule, rate, window, remittance date and result.
rules:
  - [R1, "Rates and windows come from the Remittance incentive rules (OQ23).", Configurable, Incentive Rules]
validations: []
notifications:
  - "None."
audit:
  - "None (read only)."
acceptance:
  - An account remitted 20 days after inception under a 30-day rule is ELIGIBLE at the rule's rate.
  - An account of an insurer without rule is NO_RULE.
```

```fr
id: FR-OP-079
title: Close a cycle and view its history
brd: [PRCID.029 (p.85)]
actor: Recon Handler
priority: Must have
fit: FIT
screens: Reconciliation Cycles; Reconciliation Cycle (workflow panel, History)
api: GET .../cycles; GET .../cycles/{id}; POST .../cycles/{id}/close
description: The cycle keeps its counts per bucket and its history (timestamps, actions, users). It closes by itself (for_closure) when every item is matched or marked for closure; otherwise the handler closes it with a comment.
preconditions:
  - The cycle is RECONCILING.
main_flow:
  - The handler resolves the items.
  - BIBS closes the cycle when all items are matched or for closure, or the handler clicks **Close Cycle**.
rules:
  - [R1, "One open cycle per insurer and production month.", Fixed, "-"]
validations:
  - [Action on a closed cycle, "Cycle <no> is closed", RECON_CYCLE_CLOSED]
notifications:
  - "None."
audit:
  - The History tab shows the workflow history and the audit trail.
acceptance:
  - Marking the last open item for closure closes the cycle.
  - The History tab lists the extract, send, upload and closure with users and times.
```

```fr
id: FR-OP-080
title: Generate the Production Reconciliation reports
brd: [PRCID.017 (p.82-83), PRCID.018 (p.83), PRCID.035 (p.86), PRCID.036 (p.86-87), PRCID.037 (p.87), PRCID.038 (p.87), PRCID.039 (p.87)]
actor: Recon Handler; TL / TH
priority: Must have
fit: NEW
screens: Reports (category Operations)
api: /api/v1/reports (PRC-*)
description: "BIBS provides PRC-REGISTER (variants: plain, with insurer feedback, with Marketing feedback, with both), PRC-SUMMARY (matched, matched with discrepancies and unmatched items and amounts per insurer), PRC-UNMATCHED-LOC (per location), PRC-UNMATCHED-AO (per Marketing AO / AB, booked and pre-booked), PRC-UNMATCHED-FEEDBACK (unmatched accounts with feedback and disposition) and PRC-DISPOSITION (summary per disposition), in the layouts of Annex IV (section 6.4)."
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user chooses the report, insurer and month and generates it.
rules: []
validations: []
notifications:
  - "None."
audit:
  - Runs are archived.
acceptance:
  - PRC-SUMMARY of a cycle equals the bucket counts and amounts of the cycle.
  - PRC-REGISTER with insurer feedback shows the insurer remarks per account.
```

## Commission Receivables / Direct Payment

Commission Receivables handles direct payment (DP) accounts, where the client paid the insurer directly: BDOI bills the insurer for its commission, follows the answer, collects the commission and reverses the premium receivable. It also runs the incentive programmes and tracks BIR certificates. API paths of this section are under `/api/v1/commission`.

```fr
id: FR-OP-090
title: Take in the DP lists of Head Office and branches
brd: [CMRID.001 (p.106)]
actor: Commission Handler
priority: Must have
fit: NEW
screens: DP Lists (upload, pull, branch submissions)
api: GET / POST .../dp/lists; POST .../dp/lists/pull; GET .../dp/submissions; feed COLLECTION_DP_LIST
description: Marketing Collection of Head Office and each branch submits its DP list. The handler uploads it, or pulls the lists waiting in the Collection feed. The file name must follow <Branch>_DP_<yyyyMMdd>; a file with the same content is refused. The branch submission tracker shows per branch and period which lists arrived. The lists are consolidated with de-duplication by invoice and become the basis of billing.
preconditions:
  - The user has COMMREC_PROCESS.
main_flow:
  - The handler uploads a list or clicks **Pull**.
  - BIBS checks the name and the layout and reads each row as a DP account DPL-yyyy-n.
  - BIBS validates each account (FR-OP-091).
rules:
  - [R1, "Naming <Branch>_DP_<yyyyMMdd>, for example HO_DP_20260930.xlsx.", Fixed, "-"]
  - [R2, "An invoice already active on another list is a duplicate.", Fixed, "-"]
validations:
  - [Name not in the convention, "Name the DP list <Branch>_DP_<yyyyMMdd> (e.g. HO_DP_20260930.xlsx): <file>", DP_LIST_NAME]
  - [Date part not a date, "The date in <file> is not a date (yyyyMMdd)", DP_LIST_NAME]
  - [Same file again, "<file> was already taken in as <list no>", DP_LIST_DUPLICATE]
  - [No invoice column, "The DP list has no '<column>' column", DP_LIST_LAYOUT]
  - [Row without invoice, The row has no invoice number, DP_ROW_INCOMPLETE]
  - [Premium not an amount, "Premium '<value>' is not an amount", DP_ROW_PREMIUM]
  - [No invoice of the list booked, "No invoice of the list is booked: upload it from Commission Receivables", DP_LIST_COMPANY]
fields_screen: DP Lists (upload)
fields:
  - [DP List File, File, "Yes", "-", Naming convention]
notifications:
  - "None."
audit:
  - Each list keeps file name, hash, branch, period and received time.
acceptance:
  - A list named CEB_DP_20260930.xlsx is taken in and appears in the tracker for branch CEB.
  - A list named dp list.xlsx is refused with DP_LIST_NAME.
```

> [!NOTE] Superseded source
> CMRID.001 names shared folders of Head Office and branches. BRD-4 Collections replaces the DP list by the Collections disposition "DP PR for reversal" (OQ38); the upload stays as built until then.

```fr
id: FR-OP-091
title: Validate, sanitise and tag DP accounts
brd: [CMRID.002 (p.106-107), CMRID.007 (p.109), CMRID.008 (p.109-110), CMRID.013 (p.112)]
actor: System; Commission Handler
priority: Must have
fit: NEW
screens: DP Accounts (tabs by tag, account detail with rule results)
api: GET .../dp/items; GET .../dp/items/counts; POST .../dp/items/confirm, /exclude, /{id}/revalidate
description:
  - "Each DP account is checked against the invoice ledger: invoice booked; tagged direct payment; not cancelled or written off; booked with the listed insurer; listed premium equal to the booked gross premium within 1.00; no pending negative adjustment; commission still open; policy number present. The result of every rule is kept on the account. The sanitation result is VALID, DUPLICATE, INVALID or INCOMPLETE."
  - "BIBS computes the commission receivable of each account (commission, VAT, withholding tax, net) from the ledger. Valid accounts are tagged DP for confirmation; after the handler confirms that the client paid the insurer in full they become DP for billing. Tags: DP_FOR_CONFIRMATION, DP_FOR_BILLING, BILLED, APPROVED, REJECTED, COLLECTED, PR_REVERSED, EXCLUDED."
preconditions:
  - The user has COMMREC_PROCESS.
main_flow:
  - BIBS validates the accounts of a list.
  - The handler reviews the DP for confirmation tab and the rule results.
  - The handler confirms the fully paid accounts; they move to DP for billing.
alternate_flows:
  - The handler excludes an account with a reason; it stays on record as EXCLUDED.
  - The handler re-validates an account after the ledger changed.
rules:
  - [R1, "Only confirmed, fully paid, valid accounts are billed.", Fixed, "-"]
  - [R2, "Premium tolerance 1.00 between the list and the booking.", Fixed, "-"]
validations:
  - [Action on an account in another tag, "Account <invoice> (<tag>) cannot be <action>", DP_ITEM_STATE]
  - [Account already in the target tag, "Account <invoice> is already <tag>", DP_ITEM_STATE]
notifications:
  - "None."
audit:
  - Rule results, confirmations, exclusions and their reasons are kept per account.
acceptance:
  - An account whose invoice is not tagged direct payment is INVALID with the failed rule shown.
  - The same invoice on two branch lists is DUPLICATE on the second.
  - Confirming a valid account moves it to DP for billing.
```

```fr
id: FR-OP-092
title: Bill the insurer for DP commission
brd: [CMRID.009 (p.110-111), CMRID.012 (p.111-112)]
actor: Commission Handler
priority: Must have
fit: "NEW (CMRID.009), CHANGE (CMRID.012)"
screens: DP Accounts (Prepare Billing); DP Billings; DP Billing (Send)
api: POST / GET .../dp/billings; GET .../dp/billings/{id}; GET .../dp/billings/{id}/items, /file; POST .../dp/billings/{id}/send, /cancel
description: "The handler prepares billings from the accounts for billing: one billing CRB-yyyy-n per insurer, assigned to the handler (workflow OPS_DP_BILLING, section 5). The billing workbook lists the accounts with premium, commission, VAT, withholding tax and net. **Send Billing to Insurer** e-mails it as a password-protected file with the password in a separate e-mail, logs the request and sets the answer due date 10 working days later on the head office calendar."
preconditions:
  - The user has COMMREC_PROCESS; accounts are DP for billing.
main_flow:
  - The handler clicks **Prepare Billing**; BIBS creates one billing per insurer.
  - The handler opens a billing and clicks **Send Billing to Insurer**.
  - BIBS sends it, tags the accounts BILLED and moves the billing to AWAITING_INSURER.
alternate_flows:
  - Cancel a billing before it is sent; its accounts return to DP for billing.
rules:
  - [R1, "Answer due in 10 working days.", Configurable, Parameter CMR_FEEDBACK_WORKING_DAYS]
  - [R2, "Billing layout to be confirmed (OQ38).", Configurable, Billing columns]
validations:
  - [Nothing to bill, No account is confirmed for billing, DP_NOTHING_TO_BILL]
  - [Account not confirmed, "Account <invoice> is not confirmed for billing (<tag>)", DP_ITEM_STATE]
  - [Already sent, "Billing <no> was already sent", DP_BILLING_SENT]
  - [No insurer e-mail, "Enter the e-mail address of <insurer>", DP_NO_RECIPIENT]
  - [Account removed while on billing, "Account <invoice> is on a billing: cancel the billing first", DP_ITEM_BILLED]
fields_screen: Send Billing
fields:
  - [To, Text, "Yes", Insurer e-mail, Valid e-mail]
  - [Cc, Text, "No", "-", Valid e-mail]
notifications:
  - E-mails to the insurer with the protected billing and the password.
audit:
  - The billing keeps file, recipients, sent time and due date.
acceptance:
  - Accounts of two insurers produce two billings.
  - A sent billing shows its due date 10 working days later.
```

```fr
id: FR-OP-093
title: Record insurer answers and flag late feedback
brd: [CMRID.008 (p.109-110), CMRID.009 (p.110-111), CMRID.011 (p.111)]
actor: Commission Handler; System (job DP_FEEDBACK_SLA)
priority: Must have
fit: "NEW (CMRID.008, 009), CHANGE (CMRID.011)"
screens: Insurer Responses (answers upload); DP Billing (answers); DP Billings (SLA)
api: POST .../dp/billings/{id}/answers; POST .../dp/responses; feed INSURER_DP_RESPONSE; job DP_FEEDBACK_SLA
description:
  - The insurer approves or rejects each account. Answers are entered on the billing or uploaded (columns Billing No., Invoice No., Decision, Reason, Comment). A rejection needs a reason from DP_FEEDBACK_REASON. Approved accounts go to collection; rejected accounts are returned to the Collection team through the COLLECTION_DP_RETURNED extract with the time and reason. A billing whose accounts were all rejected moves to RETURNED_TO_COLLECTION.
  - The job DP_FEEDBACK_SLA (daily) raises DP_FEEDBACK_OVERDUE for billings without an answer after the due date; the billing list shows the SLA countdown.
preconditions:
  - The billing is AWAITING_INSURER.
main_flow:
  - The handler enters or uploads the answers.
  - BIBS tags the accounts APPROVED or REJECTED and moves the billing.
alternate_flows:
  - No answer in time. BIBS flags the billing overdue and notifies.
rules:
  - [R1, "Feedback reasons (values from BDOI, OQ40; Others until then).", Configurable, LOV DP_FEEDBACK_REASON]
validations:
  - [Rejection without reason, "Give the insurer's reason for rejecting <invoice>", DP_REASON_REQUIRED]
  - [Billing not waiting, "Billing <no> is not waiting for the insurer", DP_BILLING_NOT_AWAITING]
  - [Invoice not on the billing, "Invoice <no> is not on billing <no>", DP_NOT_ON_BILLING]
  - [Unknown billing in the file, "Billing <no> does not exist", DP_BILLING_UNKNOWN]
  - [Decision not APPROVED or REJECTED, "Decision '<value>' is neither APPROVED nor REJECTED", DP_RESPONSE_DECISION]
  - [Column missing, "The insurer's answer has no '<column>' column", DP_RESPONSE_LAYOUT]
notifications:
  - DP_FEEDBACK_OVERDUE to the handlers and TLs.
audit:
  - Each answer keeps decision, reason, comment, user and time; returned accounts keep the return time.
acceptance:
  - A rejection without a reason is refused.
  - A billing sent 11 working days ago without answer shows overdue and raises the alert.
  - Rejected accounts are in the COLLECTION_DP_RETURNED extract.
```

```fr
id: FR-OP-094
title: Collect DP commission and reverse the premium receivable
brd: [CMRID.010 (p.111), MKTID.012 (p.75-76)]
actor: Commission Handler; System
priority: Must have
fit: NEW
screens: DP Billing (Collect); DP Accounts (Reverse, Reinstate)
api: POST .../dp/billings/{id}/collect; POST .../dp/items/{id}/reverse, /reinstate
description:
  - "When the insurer pays the commission of an approved billing, the handler records the collection with the bank account, collection date and BIR certificate number. BIBS posts OPS_DP_COMMISSION_COLLECT (cash and CWT against the commission receivable; realisation of commission and output VAT), requests the commission OR from Cashiering and, for each approved account, records the DP_REVERSAL of the premium receivable and DTIP and the APPLIED commission. The billing moves to COLLECTED and then CLOSED."
  - The reversal is made only for accounts tagged direct payment by Marketing (MKTID.011) and confirmed collected; an account that fails the tag check blocks and is notified.
  - A reversed account can be reinstated with a direct payment reason from REINSTATEMENT_REASON (CSHID.004 group b), then reversed again. The DP_CANCELLATION reason also sends the collected commission to Unapplied Payments.
preconditions:
  - The billing is APPROVED; the user has COMMREC_PROCESS.
main_flow:
  - The handler clicks **Collect** and enters the collection details.
  - BIBS posts the collection, requests the OR and reverses the PR of each approved account.
  - The billing shows the OR number and status.
alternate_flows:
  - Reinstate an account with a reason.
rules:
  - [R1, "The GL entry of the PR reversal is posted only when DP_PR_REVERSAL_POSTING is on; the ledger reversal is always recorded (OQ07).", Configurable, Parameter DP_PR_REVERSAL_POSTING (false)]
  - [R2, "Proposed collection bank account.", Configurable, Parameter CMR_DP_COLLECTION_BANK]
validations:
  - [Nothing approved, "Billing <no> has no approved account to collect", DP_BILLING_NOT_APPROVED]
  - [No bank account, Choose the bank account the commission was received in, DP_BANK_REQUIRED]
  - [Account not tagged DP, "Invoice <no> is not tagged direct payment by Marketing: its premium receivable cannot be reversed (MKTID.012)", DP_TAG_INVALID]
  - [Reinstate without reason, Choose a direct payment reinstatement reason, DP_REINSTATE_REASON]
fields_screen: Collect
fields:
  - [Bank Account, List, "Yes", GL bank accounts, Default CMR_DP_COLLECTION_BANK]
  - [Collection Date, Date, "Yes", "-", Not in the future]
  - [BIR Certificate No., Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - The collection journal, OR number and every reversal and reinstatement are recorded.
acceptance:
  - Collecting an approved billing posts the collection, shows the commission OR and records DP_REVERSAL on each account.
  - An account not tagged direct payment is not reversed and the message names it.
  - A reinstated account can be reversed again.
```

> [!NOTE] Difference from the BRD
> MKTID.012 asks to reverse the DP premium receivable automatically. Booking posts no premium receivable for direct payment invoices, so the GL entry of the reversal is off until the DP accounting is agreed (OQ07); the reversal is recorded in the invoice ledger in all cases.

```fr
id: FR-OP-095
title: Compute and post incentives
brd: [CMRID.003 (p.107), CMRID.005 (p.108-109), CMRID.006 (p.109)]
actor: Commission TL (INCENTIVE_MANAGE, COMMREC_APPROVE)
priority: Must have
fit: NEW
screens: Incentive Schemes (tier editor); Incentive Runs (compute, lines, post, cancel)
api: GET / POST .../incentives/schemes; GET / PUT .../incentives/schemes/{id}; GET / POST .../incentives/runs; POST .../incentives/runs/{id}/post, /cancel
description:
  - "A scheme has a type (No Touch, Top Up, Motor Mania, Other), a calculation (TARGET_TIERED or FIXED_PER_POLICY), a period (for example June to December, quarterly, yearly), beneficiary (BDOI or branch), insurer, segments, product lines, effective dates and tiers. Target tiers hold a production target, rate and multiplier; fixed tiers hold a minimum basic premium and a fixed amount per policy."
  - A run INR-yyyy-n computes a scheme on a booking period. TARGET_TIERED applies the rate of the highest target reached times the multiplier to every eligible invoice; FIXED_PER_POLICY pays each policy the amount of the highest minimum premium it meets (Motor Mania). Negative amounts and erroneous bookings (cancelled or written off) are excluded when the exclusion rule is active and raise INCENTIVE_EXCLUSION.
  - Posting (COMMREC_APPROVE) publishes OPS_INCENTIVE_ACCRUE per insurer and, for branch schemes, OPS_INCENTIVE_PASS_ON per sales unit with a PASS_ON request to Disbursement.
preconditions:
  - The scheme has tiers.
main_flow:
  - The TL maintains the scheme and its tiers.
  - The TL computes a run for a period and reviews the lines and exclusions.
  - The TL posts the run.
alternate_flows:
  - Cancel a computed run.
rules:
  - [R1, "Schemes are inactive without tiers until BDOI gives targets, rates and amounts (OQ39).", Configurable, Incentive Schemes]
  - [R2, "Exclusion rules NEGATIVE_AMOUNT and ERRONEOUS_BOOKING.", Configurable, LOV INCENTIVE_EXCLUSION_RULE]
validations:
  - [Scheme without tiers, "Scheme <name> has no tiers yet (targets and amounts from BDOI, OQ39)", INCENTIVE_SCHEME_EMPTY]
  - [Tier incomplete (tiered), Each tier needs a production target and a rate above zero, INCENTIVE_TIER_INVALID]
  - [Tier incomplete (fixed), Each tier needs a minimum basic premium and a fixed amount above zero, INCENTIVE_TIER_INVALID]
  - [Scheme dates, The scheme ends before it starts, INCENTIVE_SCHEME_DATES]
  - [Run period, The period ends before it starts, INCENTIVE_RUN_PERIOD]
  - [Run earned nothing, "Run <no> earned no incentive to post", INCENTIVE_RUN_NOTHING]
  - [Run already posted or cancelled, "Run <no> is <status> and cannot change", INCENTIVE_RUN_STATE]
fields_screen: Incentive Scheme
fields:
  - [Code / Name, Text, "Yes", "-", Unique code]
  - [Scheme Type, List, "Yes", "No Touch, Top Up, Motor Mania, Other", "-"]
  - [Calculation, List, "Yes", TARGET_TIERED / FIXED_PER_POLICY, "-"]
  - [Period, List, "Yes", "Monthly, quarterly, half-year, yearly", "-"]
  - [Beneficiary, List, "Yes", BDOI / Branch, "-"]
  - [Insurer Code, Look-up, "No", Insurer master, "-"]
  - [Segments / Product Lines, Multi-select, "No", Segments / product lines, "-"]
  - [Effective From / To, Date, "Yes / No", "-", To >= From]
notifications:
  - INCENTIVE_EXCLUSION alert when lines are excluded.
audit:
  - Scheme changes, runs, exclusions and postings are recorded.
acceptance:
  - A Motor Mania scheme with tiers 10,000 / 500 and 50,000 / 1,000 pays 1,000.00 for a policy of basic premium 60,000.00 and nothing below 10,000.00.
  - A cancelled invoice is excluded from a run and listed with the reason.
```

```fr
id: FR-OP-096
title: Submit BIR certificates to Comptrollership
brd: [CMRID.015 (p.113), CMRID.010 (p.111)]
actor: Commission Handler (submit); Comptrollership (acknowledge)
priority: Must have
fit: NEW
screens: BIR Certificates; BIR Certificate (workflow panel, ORs, scanned copy)
api: GET / POST .../certificates; GET / PUT .../certificates/{id}; GET .../certificates/receipts; POST .../certificates/{id}/acknowledge, /reject
description: The handler records a withholding tax certificate received from an insurer (BIR form, certificate number, period, tax withheld, scanned copy) and tags it to the ORs issued by the Cashier that it covers. The submission BCS-yyyy-n goes to Comptrollership (workflow OPS_BIR_CERT, section 5), which acknowledges it or rejects it with a reason; a rejected submission is corrected and resubmitted. Submission and acknowledgment dates are kept.
preconditions:
  - The user has BIR_CERT_SUBMIT.
main_flow:
  - The handler adds the certificate, attaches the scan and selects the ORs.
  - The handler submits; Comptrollership is notified.
  - Comptrollership acknowledges.
alternate_flows:
  - Reject with a reason; the handler resubmits.
rules:
  - [R1, "A certificate covers at least one OR.", Fixed, "-"]
validations:
  - [No OR tagged, Tag the certificate to at least one official receipt (CMRID.015), CERT_NO_OR]
  - [Tax zero, The tax withheld must be above zero, CERT_AMOUNT]
  - [Period, The certificate period ends before it starts, CERT_PERIOD]
  - [No insurer, Choose the company and the insurer of the certificate, CERT_INSURER_REQUIRED]
  - [Reject without reason, Give the reason of the rejection, CERT_REASON_REQUIRED]
  - [Resubmit a submission not rejected, Only a rejected submission can be resubmitted, CERT_NOT_REJECTED]
fields_screen: BIR Certificate
fields:
  - [Insurer Code, Look-up, "Yes", Insurer master, "-"]
  - [BIR Form, List, "Yes", "2307, others", "-"]
  - [Certificate No., Text, "Yes", "-", "-"]
  - [Period From / To, Date, "Yes", "-", To >= From]
  - [Tax Withheld, Amount, "Yes", "-", "> 0"]
  - [Official receipts, Multi-select, "Yes", ORs of the insurer, At least one]
notifications:
  - The approvers of BIR_CERT_ACK on submission; the handler on rejection.
audit:
  - Each submission keeps its history with dates and reasons.
acceptance:
  - A certificate without an OR cannot be submitted.
  - Comptrollership rejects a certificate with a reason; the handler resubmits it and it is acknowledged.
```

```fr
id: FR-OP-097
title: Track estimated items and yearly production
brd: [RMTID.037 (p.68), CMRID.014 (p.112-113)]
actor: Commission Handler
priority: Must have
fit: NEW
screens: Estimated Items; Report CMR-PRODUCTION-YEARLY
api: GET / POST .../estimated; /api/v1/reports (CMR-PRODUCTION-YEARLY)
description: The handler flags invoices as estimated items with a reason, or clears the flag; the ledger's ESTIMATED flag shows separately with its label on the production register and the yearly production report. CMR-PRODUCTION-YEARLY consolidates production per branch and insurer for the year, with total production, estimated items and performance against targets.
preconditions:
  - The user has COMMREC_PROCESS.
main_flow:
  - The handler enters the invoice and reason and flags it.
  - The reports show the item as estimated.
rules:
  - [R1, "Definition of estimated items to be confirmed (OQ27).", Configurable, "-"]
validations: []
fields_screen: Estimated Items
fields:
  - [Invoice No., Text, "Yes", Invoice ledger, Booked invoice]
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - Flag changes are recorded as ledger status changes.
acceptance:
  - A flagged invoice appears as estimated on CMR-PRODUCTION-YEARLY and on the production register.
  - The yearly report includes every branch and insurer with production.
```

```fr
id: FR-OP-098
title: Report commission receivables
brd: [CMRID.004 (p.108)]
actor: Commission Handler; TL
priority: Must have
fit: NEW
screens: Reports (category Operations)
api: /api/v1/reports (CMR-*)
description: CMR-COMMISSION-RECEIVABLE shows commission booked, collected and outstanding per insurer, separating DP / direct bill from regular commissions, with partial remittances and net commission. The other Commission reports are CMR-DP-STATUS, CMR-INCENTIVE, CMR-FEEDBACK-SLA and CMR-BIR-CERT (section 6.5).
preconditions:
  - The user has OPS_REPORT_VIEW.
main_flow:
  - The user generates the report for a period.
rules:
  - [R1, "Final layout to be confirmed (OQ42).", Configurable, Report layouts]
validations: []
notifications:
  - "None."
audit:
  - Runs are archived.
acceptance:
  - A DP account collected and a regular invoice remitted appear in separate groups with their net commission.
```

## Marketing Collection items

The MKTID requirements are activities of Marketing (Marketing Collection HO and branches, Marketing TL / UH) that feed Operations. BRD-4 Collections confirmed that MKTID.001-009 and MKTID.011 stay in Operations as built; the 2307 and DP tagging (MKTID.010, 012, 013) move to the Collections dispositions (OQ45).

<!-- table: widths=2.4,7,4,3.2 caption="Marketing items and where they are specified" size=8.5 -->
| BRD ID | Activity | FR | Status after BRD-4 |
|---|---|---|---|
| MKTID.001 | Request to send the remittance schedule to the insurer | FR-OP-110 | Stays in Operations |
| MKTID.002-007 | Hold requests: tag, remove, assign, create / cancel / extend, approve, reference numbers | FR-OP-111 | Stays in Operations |
| MKTID.008 | Endorsement slip for external endorsements | FR-OP-114 | Stays in Operations |
| MKTID.009 | Special remittance request | FR-OP-112 | Stays in Operations |
| MKTID.010, 013 | BIR 2307 tagging and reinstatement requests | FR-OP-113 | Collections disposition "PR 2307 for reversal" |
| MKTID.011 | DP tag at quotation and policy | FR-OP-004 | Stays (booking flag) |
| MKTID.012 | DP premium receivable reversal after valid tagging | FR-OP-094 | Collections disposition "DP PR for reversal" |

```fr
id: FR-OP-110
title: Send the remittance schedule to the insurer
brd: [MKTID.001 (p.70-71)]
actor: Remittance Processor on the request of Marketing
priority: Must have
fit: CHANGE
screens: Remittance Batch (Send via Email)
api: POST /api/v1/remittance/batches/{id}/send-schedule
description: Once a batch is approved, the remittance schedule with the client details (client, amount, dates) is e-mailed to the insurer as a password-protected Excel file, with the password in a separate e-mail. The send is confirmed on the batch and allowed once per batch.
preconditions:
  - The batch is approved; the user has REMIT_PROCESS.
main_flow:
  - The processor clicks **Send via Email** and confirms To, subject and message.
  - BIBS sends the protected schedule and records the send.
rules:
  - [R1, "One send per batch.", Fixed, "-"]
validations:
  - [Batch not approved, "The schedule of <batch> can be sent once the batch is approved", REMIT_SCHEDULE_NOT_APPROVED]
  - [Already sent, "The schedule of <batch> was already sent", REMIT_SCHEDULE_ALREADY_SENT]
fields_screen: Send via Email
fields:
  - [To, Text, "Yes", Insurer e-mail, Valid e-mail]
  - [Subject, Text, "Yes", "-", "-"]
  - [Message, Text, "No", "-", "-"]
notifications:
  - E-mails to the insurer.
audit:
  - The send is logged on the batch and in the messaging log.
acceptance:
  - The schedule of an approved batch is sent once; a second send is refused.
```

> [!NOTE] Difference from the BRD
> MKTID.001 describes a Marketing request to send the schedule. The request is made outside BIBS (why the schedule goes through Marketing is OQ22); the Remittance processor sends it from the batch.

```fr
id: FR-OP-111
title: Request, approve, extend, cancel and release remittance holds
brd: [MKTID.002 (p.71), MKTID.003 (p.71-72), MKTID.004 (p.72), MKTID.005 (p.72-73), MKTID.006 (p.73), MKTID.007 (p.73-74), RMTID.021 (p.61)]
actor: Marketing Collection (request); Marketing TL / UH (approve); System (expiry)
priority: Must have
fit: "NEW (MKTID.002, 003, 005, RMTID.021), CHANGE (MKTID.004, 006), CONFIGURE (MKTID.007)"
screens: Remittance Holds; Remittance Hold (workflow panel)
api: GET / POST /api/v1/remittance/holds; GET / PUT .../holds/{id}; POST .../holds/{id}/submit, /cancel, /decision, /extend, /extension-decision, /request-cancel, /cancel-decision, /release, /assign; POST .../holds/upload; feed COLLECTION_HOLD; job HOLD_EXPIRY
description:
  - Marketing raises a hold request HLD-yyyy-n on an invoice with a reason, hold-until date and remarks, singly or by file (invoiceNo, reasonCode, holdUntil, remarks). One live request per invoice. Submission makes the invoice REQUESTED_FOR_HOLD; approval by another user sets the HOLD flag, which excludes the invoice from extraction (FR-OP-031). The approver assigns the hold to an active remittance processor, who is notified.
  - An active hold is extended (new hold-until date, with approval), cancelled (with approval) or released by Marketing; release makes the invoice eligible again. The job HOLD_EXPIRY (daily 08:15 PHT) releases holds whose date has passed and notifies holds that reach their date the next day. Holds from Collection arrive through the feed COLLECTION_HOLD.
preconditions:
  - The user has HOLD_REQUEST (request) or HOLD_APPROVE (decide, assign).
main_flow:
  - Marketing creates the hold request and submits it.
  - The Marketing TL approves it; the invoice is on hold.
  - The TL assigns the hold to a remittance processor.
  - On the hold-until date the job releases the hold.
alternate_flows:
  - Reject. The hold is rejected; the invoice returns to its previous status.
  - Extension. Marketing requests an extension; the TL approves or rejects it.
  - Cancellation. Marketing requests the cancellation; the TL approves (released) or rejects (still active).
  - Release. Marketing releases the hold before its date.
rules:
  - [R1, "Only invoices UNPROCESSED, WITH_OUTSTANDING_BALANCE or PARTIALLY_REMITTED, not direct payment, can be held.", Fixed, "-"]
  - [R2, "The approver is never the requester.", Fixed, "-"]
  - [R3, "Hold reasons (values from BDOI, OQ24); no maximum hold period.", Configurable, LOV HOLD_REASON]
validations:
  - [Invoice not holdable, "Invoice <no> cannot be held: remittance status <status>", HOLD_INVOICE_NOT_HOLDABLE]
  - [Hold already open, "Invoice <no> already has hold <no>", HOLD_DUPLICATE]
  - [Date not in the future, The hold-until date must be in the future, HOLD_DATE]
  - [Extension not later, "The extension must be later than <date>", HOLD_EXTENSION_DATE]
  - [Approver is the requester, "Hold <no> must be approved by another user", HOLD_FOUR_EYES]
  - [Assignee not a processor, "<user> is not an active remittance processor", HOLD_ASSIGNEE_NOT_ELIGIBLE]
  - [Action in another stage, "Hold <no> is <stage>, not <expected>", HOLD_STAGE]
fields_screen: Hold request
fields:
  - [Invoice No., Look-up, "Yes", Invoice ledger, Holdable invoice]
  - [Reason, List, "Yes", LOV HOLD_REASON, "-"]
  - [Hold Until, Date, "Yes", "-", In the future]
  - [Remarks, Text, "No", "-", "-"]
  - [Remittance Processor (User ID), Look-up, Cond., Users with REMIT_PROCESS, On assignment]
notifications:
  - The approvers on submission; the requester on decision; the processor on assignment; HOLD_EXPIRING the day before the hold date.
audit:
  - The hold history keeps every action, date and user; REM-HOLD reports the holds.
acceptance:
  - An approved hold excludes the invoice from the next extraction with reason ON_HOLD.
  - The requester cannot approve their own hold.
  - A hold whose date passed is released by the job and the invoice is extracted by the next run.
  - Hold numbers HLD-2026-n are unique and searchable.
```

```fr
id: FR-OP-112
title: Request a special remittance
brd: [MKTID.009 (p.74)]
actor: Marketing Collection; Marketing TL
priority: Must have
fit: NEW
screens: Special Remittance (New request, upload)
api: POST /api/v1/remittance/special; POST .../special/upload; feed COLLECTION_SPECIAL_REMIT
description: Marketing requests a special remittance SPR-yyyy-n for an invoice with a condition (Claims, Renewal, Installment due, Immediate OR issuance) and remarks, singly or by file (invoiceNo, conditionCode, remarks). BIBS validates the request on creation - the invoice is UNPROCESSED or PARTIALLY_REMITTED with paid AR applied, the checks are cleared (holding period) and it is not on hold - and sends it for approval. Once approved, the request is processed in Remittance (FR-OP-040).
preconditions:
  - The user has SPECIAL_REMIT_REQUEST.
main_flow:
  - Marketing enters the invoice, condition and remarks.
  - BIBS validates the eligibility and records the validation note.
  - The request moves to FOR_APPROVAL; the approvers are notified.
alternate_flows:
  - Not eligible. BIBS refuses the request with the reason.
rules:
  - [R1, "Conditions from the list.", Configurable, LOV SPECIAL_REMIT_CONDITION]
  - [R2, "Claims condition confirmed through the Claims feed CLAIMS_SPECIAL_REMIT when connected; otherwise a note (OQ46).", Configurable, "-"]
validations:
  - [Invoice not eligible, "Invoice <no> cannot be remitted specially: <reason>", SPECIAL_REMIT_NOT_ELIGIBLE]
  - [Request already open, "Invoice <no> already has request <no>", SPECIAL_REMIT_DUPLICATE]
fields_screen: Special remittance request
fields:
  - [Invoice No., Look-up, "Yes", Invoice ledger, Eligible invoice]
  - [Condition, List, "Yes", LOV SPECIAL_REMIT_CONDITION, "-"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - REMIT_BATCH_FOR_APPROVAL to the holders of SPECIAL_REMIT_APPROVE.
audit:
  - The request and its validation note are recorded.
acceptance:
  - A request on an invoice paid by a check deposited yesterday is refused with the holding reason.
  - A valid request moves to For approval with the note "Validated - paid AR ... applied and cleared".
```

```fr
id: FR-OP-113
title: Tag BIR 2307 reversals and reinstatement requests
brd: [MKTID.010 (p.74-75), MKTID.013 (p.46)]
actor: Marketing Collection (CWT_TAG)
priority: Must have
fit: NEW
screens: BIR 2307 (Tag 2307); bulk CWT_TAGS
api: POST /api/v1/cashiering/cwt; bulk CWT_TAGS; feed COLLECTION_CWT2307
description: Marketing tags the 2307 of an invoice with a reference CWT-yyyy-n generated at tagging, the path (certificate or cash), the certificate number and period and the amount, singly or by file. The tag flows to Cashiering by its reference (FR-OP-026). Marketing also submits reinstatement requests with the reason and payment details (FR-OP-014); when the premium was already remitted, the case goes to Adjustment with the insurer's written confirmation of refund as a mandatory attachment.
preconditions:
  - The user has CWT_TAG; the invoice has a 2% CWT portion outstanding.
main_flow:
  - Marketing enters the invoice, path, certificate and amount.
  - BIBS checks the invoice and issues the reference.
  - The tag appears in the Cashiering 2307 queue.
alternate_flows:
  - Marketing cancels a tag before Cashiering receives it.
rules:
  - [R1, "One tag in process per invoice.", Fixed, "-"]
validations:
  - [Unknown invoice, "Unknown invoice <no>", CWT_INVOICE_UNKNOWN]
  - [Tag already in process, "Invoice <no> already has a 2307 tag in process", CWT_ALREADY_TAGGED]
  - [No 2% outstanding, "Invoice <no> has no 2% CWT portion outstanding", CWT_AMOUNT]
  - [Certificate path without number, A certificate tag needs the BIR 2307 certificate number, CWT_CERTIFICATE_REQUIRED]
fields_screen: Tag 2307
fields:
  - [Invoice No., Look-up, "Yes", Invoice ledger, 2% CWT outstanding]
  - [Path, Option, "Yes", Certificate / Cash, "-"]
  - [Certificate No., Text, Cond., "-", Required for certificate]
  - [Period From / Period To, Date, Cond., "-", Certificate path]
  - [Amount, Amount, "Yes", "-", "> 0"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - The tag history keeps every action with user, time and reason.
acceptance:
  - A certificate tag receives a reference CWT-2026-n that Cashiering finds by that reference.
  - A second tag on the same invoice is refused while the first is in process.
```

```fr
id: FR-OP-114
title: Generate the endorsement slip for Marketing
brd: [MKTID.008 (p.114)]
actor: Marketing Collection / TL (ADJ_REQUEST)
priority: Must have
fit: NEW
screens: Endorsement Request page (Endorsement Slip)
api: GET /api/v1/adjustment/requests/{id}/endorsement-slip
description: Marketing generates the endorsement slip of its external endorsement requests with the function of FR-OP-060 (same fields, same number ES-yyyy-n). No slip is produced for internal adjustments.
preconditions:
  - The request is external.
main_flow:
  - The Marketing user opens the request and downloads or prints the slip.
rules: []
validations:
  - [Internal adjustment, Internal adjustments have no endorsement slip (ADJID.015), ADJ_NO_SLIP_FOR_INTERNAL]
notifications:
  - "None."
audit:
  - As FR-OP-060.
acceptance:
  - A Marketing user prints the slip of a partial cancellation request; the slip number is the one Adjustment sees.
```

## Disbursement queue

Until the Disbursement module of BRD-5 is delivered, Operations sends its payment requests to an in-app Disbursement queue. API paths are under `/api/v1/ops/disbursements`.

```fr
id: FR-OP-120
title: Work payment requests in the Disbursement queue
brd: [DBMID.001 (p.46), RMTID.034 (p.67)]
actor: Disbursement (DISB_PROCESS)
priority: Must have
fit: NEW
screens: Disbursement Queue
api: GET .../; GET .../{id}; POST .../{id}/acknowledge, /dv, /paid, /return, /cancel
description:
  - "Payment requests DSQ-yyyy-n of type REMITTANCE (remittance batches), REFUND (unapplied refunds), CWT2307 (2307 batches) and PASS_ON (incentive pass-on) arrive with their payee, amount, source and documents. Disbursement acknowledges a request, enters the DV number, marks it paid, or returns it with a reason. Each status (SENT, ACKNOWLEDGED, DV_ASSIGNED, PAID, RETURNED, CANCELLED) is sent back to the source module (event DisbursementStatusChanged): Remittance updates the invoices (FR-OP-036), Cashiering completes the refund or releases the 2307 batch."
preconditions:
  - The user has DISB_PROCESS.
main_flow:
  - Disbursement opens the queue and a request.
  - Disbursement acknowledges it, then enters the DV number, then marks it paid.
  - BIBS notifies the source module and its users at each step.
alternate_flows:
  - Return with a reason; the source team is notified.
rules:
  - [R1, "A request moves only forward through its statuses.", Fixed, "-"]
validations:
  - [Amount not positive, A payment request needs a positive amount, DISBURSEMENT_AMOUNT]
  - [Action not allowed in the status, "Payment request <no> is <status> and cannot be <action>", DISBURSEMENT_STATUS]
  - [Too many references, Too many references on one payment request, DISBURSEMENT_REFERENCES]
fields_screen: Disbursement Queue (DV)
fields:
  - [DV No., Text, "Yes", "-", "-"]
  - [Reason, Text, Cond., "-", Required to return]
notifications:
  - OPS_DISBURSEMENT_STATUS to the users of the source module.
audit:
  - Each status change is kept with user and time.
acceptance:
  - Entering the DV number of a remittance request makes its invoices FULLY_REMITTED.
  - A paid request cannot be returned.
```

```fr
id: FR-OP-121
title: Release BIR 2307 certificates to insurers
brd: [DBMID.001 (p.46)]
actor: Disbursement
priority: Must have
fit: NEW
screens: Disbursement Queue; BIR 2307 (batches)
api: POST /api/v1/cashiering/cwt/batches/{id}/route, /release
description: Disbursement receives the BIR 2307 transaction report and the certificates sorted per insurer as a CWT2307 request. When it is paid, the batch is released to the insurer (**Release to Insurer**) and BIBS posts Dr DTIP / Cr PR2307, which reverses the DTIP of the 2% and completes the zeroing of the PR (FR-OP-026).
preconditions:
  - The 2307 batch is WITH_DISBURSEMENT.
main_flow:
  - Disbursement processes the request as in FR-OP-120.
  - On payment the batch is released and the DTIP offset is posted.
rules:
  - [R1, "One batch per insurer.", Fixed, "-"]
validations:
  - [Batch already routed or released, "<batch> is <status>", CWT_BATCH_ROUTED]
notifications:
  - OPS_DISBURSEMENT_STATUS to Cashiering.
audit:
  - Release and posting are in the batch history.
acceptance:
  - Releasing a paid 2307 batch posts the DTIP offset for each tag of the batch.
```

> [!NOTE] Superseded by BRD-5
> The Disbursement module of BRD-5 implements the same gateway (OQ02 answered); the in-app queue is the default until it is delivered.

## Interfaces and flow-in

```fr
id: FR-OP-130
title: Integrate with other systems through ports
brd: [BRQID.004 (p.18)]
actor: System
priority: Must have
fit: NEW
screens: Interfaces; Hand-offs and Extracts
api: /api/v1/ops/flow-in; /api/v1/ops/extracts; /api/v1/ops/handoffs
description:
  - "Operations exchanges data with the systems it depends on through ports with a default adapter: CollectionFeed (check pick-up, 2307 tags, commission payments, holds, special remittances, DP lists, returned DP accounts, refunds), DisbursementGateway (payment requests and statuses), InsurerFileInbox (insurer files), FileDropPort (shared drive), MarketingFeed and ClaimsFeed. The defaults are manual uploads, the in-app Disbursement queue and the in-system extract repository; no integration is simulated. Section 7 lists each interface and its status."
  - Work that a default adapter cannot complete alone (an OR or an unapplied item requested while Cashiering is not installed, a quotation for a TSI increase) becomes an open hand-off for the responsible team on Hand-offs and Extracts; the team closes it with what was done.
preconditions:
  - "None."
main_flow:
  - A module calls a port.
  - The adapter sends or receives the data (upload, queue, repository).
  - Failures are recorded and alerted (FR-OP-131).
rules:
  - [R1, "Transports are adapters; a new transport does not change the modules.", Fixed, "-"]
validations:
  - [Hand-off already closed, The hand-off is already closed, HANDOFF_CLOSED]
notifications:
  - OPS_FLOW_IN_FAILED on failures.
audit:
  - Every exchange is a flow-in run with its records, or a queue entry.
acceptance:
  - A remittance approval creates a request in the Disbursement queue.
  - An extract for the shared drive is stored in the extract repository and downloadable.
```

> [!NOTE] Superseded by BRD-4 and BRD-5
> BRQID.004 names Collection, Accounting, Disbursement, Marketing and Claims. BRD-4 makes Collections a BIBS module that implements CollectionFeed in-app; BRD-5 keeps the GL in BIBS and implements DisbursementGateway; Claims (BRD-7) implements ClaimsFeed. Marketing remains parked (OQ45).

```fr
id: FR-OP-131
title: Fetch data through flow-in feeds with runs, logs and alerts
brd: [BRQID.005 (p.18)]
actor: System Administrator (FLOWIN_MANAGE); System
priority: Must have
fit: NEW
screens: Interfaces (feeds, runs, records, upload)
api: GET .../flow-in/feeds; PUT .../flow-in/feeds/{code}; POST .../flow-in/feeds/{code}/upload; GET .../flow-in/runs; GET .../flow-in/runs/{id}/records
description:
  - Each feed (section 7) has a partner system, direction, transport, schedule and active flag. Each run FIR-yyyy-n records the trigger, start and end, records read, accepted and failed, status and errors. Each record is accepted once by its idempotency key and payload hash, so a file uploaded twice creates no duplicates. A failed run or failed records raise the alert OPS_FLOW_IN_FAILED.
  - The administrator activates a feed, sets its schedule and uploads files for feeds with a handler, and reads the runs and records.
preconditions:
  - The user has FLOWIN_MANAGE.
main_flow:
  - The administrator uploads a file to a feed, or the schedule starts it.
  - BIBS validates and maps each record through the module's handler.
  - BIBS records the run and its records and alerts on failure.
rules:
  - [R1, "One record per idempotency key.", Fixed, "-"]
  - [R2, "Schedules per feed (Spring cron, UTC).", Configurable, Interfaces]
validations:
  - [Feed inactive, "Feed <code> is inactive", FLOW_IN_FEED_INACTIVE]
  - [Feed without handler, "No module processes uploads of feed <code> yet", FLOW_IN_NO_HANDLER]
  - [Record failed, "<message>", FLOW_IN_RECORD_FAILED]
fields_screen: Interfaces (feed settings)
fields:
  - ["Schedule (Spring cron, UTC)", Text, "No", "-", Valid cron]
  - [Active, Check box, "Yes", "-", "-"]
  - [File, File, Cond., "-", For an upload]
notifications:
  - OPS_FLOW_IN_FAILED (in-app and e-mail) to the administrators.
audit:
  - Runs and records are kept with their outcome.
acceptance:
  - Uploading a hold file with one invalid row records a run with one failed record and raises OPS_FLOW_IN_FAILED.
  - Uploading the same hold file again accepts no new record.
```

```fr
id: FR-OP-132
title: Confirm payments to the New Business payment gate
brd: [CSHID.020 (p.40-41)]
actor: System
priority: Must have
fit: CHANGE
screens: New Business placement (payment gate); Pre-booked Payments
api: placement PaymentConfirmationSource (source CASHIERING)
description: Cashiering is the single payment intake. The placement payment sweep of New Business reads the Cashiering applications (APP:<id>) and the payments waiting in the pre-booked queue (PRE:<id>) of each account, so a payment received before booking opens the payment gate. Cashiering does not call placement.
preconditions:
  - "None."
main_flow:
  - A payment is applied or queued as pre-booked.
  - The placement sweep reads it and marks the placement paid.
rules:
  - [R1, "Each application or pre-booked payment is one confirmation.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - The placement keeps the evidence reference.
acceptance:
  - A payment by ARN before booking opens the payment gate with evidence CASHIERING PRE:<id>.
```


# Workflows and accounting events

## Workflows

Operations uses ten workflows of the BIBS workflow engine. Each record page shows its workflow panel with the current stage, owner, SLA and the actions the user may take. Stage owners are notified on stage entry; the SLA hours are placeholders until BDOI states them.

<!-- table: widths=3.6,3.4,7.4,2.4 caption="Operations workflows" size=8.5 -->
| Workflow | Record | Stages (initial to terminal) | FR |
|---|---|---|---|
| OPS_RECEIPT_ACTION | Receipt cancellation or reinstatement | REQUESTED, FOR_APPROVAL, POSTED / WITHDRAWN | FR-OP-013, 014 |
| OPS_DISPOSITION | Unapplied payment | UNAPPLIED, MONITORING, FOR_APPROVAL, IN_PROCESS, COMPLETED, FOR_REVERSAL, CLOSED | FR-OP-022 |
| OPS_CWT_2307 | BIR 2307 tag | TAGGED, VALIDATING, REPORT_POSTED, WITH_DISBURSEMENT, RELEASED / SETTLED_CASH / CANCELLED | FR-OP-026, 113, 121 |
| OPS_REMITTANCE | Remittance batch | REVIEW_IN_PROCESS, ON_HOLD, FOR_APPROVAL, APPROVED, FULLY / PARTIALLY_REMITTED, OR_RECEIVED / RETURNED | FR-OP-033 to 037 |
| OPS_HOLD | Remittance hold | DRAFT, FOR_APPROVAL, ACTIVE, EXTENSION_FOR_APPROVAL, CANCEL_FOR_APPROVAL, RELEASED / REJECTED / CANCELLED | FR-OP-111 |
| OPS_SPECIAL_REMIT | Special remittance | REQUESTED, FOR_APPROVAL, IN_PROCESS_REMITTANCE, PUSHED_TO_DISBURSEMENT / REJECTED / RETURNED | FR-OP-040, 112 |
| OPS_RECON | Reconciliation cycle | EXTRACTED, SENT_TO_INSURER, RECONCILING, CLOSED | FR-OP-070 to 079 |
| OPS_ENDORSEMENT | Endorsement request | DRAFT, FOR_VALIDATION, FOR_APPROVAL, FOR_POSTING, AWAITING_REAPPLICATION, POSTED / RETURNED / CANCELLED | FR-OP-050 to 057 |
| OPS_DP_BILLING | DP commission billing | DP_FOR_BILLING, AWAITING_INSURER, APPROVED, COLLECTED, CLOSED / RETURNED_TO_COLLECTION / CANCELLED | FR-OP-092 to 094 |
| OPS_BIR_CERT | BIR certificate submission | SUBMITTED, ACKNOWLEDGED / REJECTED | FR-OP-096 |

<!-- table: widths=3.6,4.2,5.4,1.6 caption="Stage owners and SLA hours (default)" size=8 -->
| Workflow | Stage | Owner (permission) | SLA h |
|---|---|---|---|
| OPS_RECEIPT_ACTION | REQUESTED | Cashier (CASH_CANCEL / CASH_REINSTATE) | 8 |
| OPS_RECEIPT_ACTION | FOR_APPROVAL | Cashiering TL / TH (CASH_APPROVE) | 8 |
| OPS_DISPOSITION | UNAPPLIED | Cashier (CASH_DISPOSITION) | 48 |
| OPS_DISPOSITION | MONITORING | Cashier (CASH_DISPOSITION) | 72 |
| OPS_DISPOSITION | FOR_APPROVAL, FOR_REVERSAL | Cashiering TL (CASH_DISPOSITION_APPROVE) | 24 |
| OPS_CWT_2307 | TAGGED | Marketing Collection (CWT_TAG) | 48 |
| OPS_CWT_2307 | VALIDATING, REPORT_POSTED | Cashier (CWT_PROCESS) | 24 |
| OPS_CWT_2307 | WITH_DISBURSEMENT | Disbursement (DISB_PROCESS) | 72 |
| OPS_REMITTANCE | REVIEW_IN_PROCESS | Remittance Processor (REMIT_PROCESS) | 24 |
| OPS_REMITTANCE | FOR_APPROVAL | Remittance TL (REMIT_APPROVE) | 24 |
| OPS_REMITTANCE | FULLY / PARTIALLY_REMITTED | Remittance Processor (REMIT_OR_UPLOAD) | - |
| OPS_HOLD | FOR_APPROVAL, EXTENSION_, CANCEL_FOR_APPROVAL | Marketing TL / UH (HOLD_APPROVE) | 24 |
| OPS_SPECIAL_REMIT | FOR_APPROVAL | Remittance TL (SPECIAL_REMIT_APPROVE) | 24 |
| OPS_SPECIAL_REMIT | IN_PROCESS_REMITTANCE | Remittance Processor (REMIT_PROCESS) | 24 |
| OPS_RECON | EXTRACTED | Recon Handler (RECON_SEND) | 48 |
| OPS_ENDORSEMENT | FOR_VALIDATION | Adjustment Processor (ADJ_PROCESS) | 24 |
| OPS_ENDORSEMENT | FOR_APPROVAL | Adjustment TL (ADJ_APPROVE) | 24 |
| OPS_ENDORSEMENT | FOR_POSTING | Adjustment Processor (ADJ_POST) | 24 |
| OPS_ENDORSEMENT | AWAITING_REAPPLICATION, RETURNED | ADJ_POST / requester (ADJ_REQUEST) | 48 |
| OPS_DP_BILLING | DP_FOR_BILLING | Commission Handler (COMMREC_PROCESS) | 48 |
| OPS_BIR_CERT | SUBMITTED | Comptrollership (BIR_CERT_ACK) | 72 |
| OPS_BIR_CERT | REJECTED | Commission Handler (BIR_CERT_SUBMIT) | 48 |

## Workflow diagrams

In the figures, solid arrows are the main path, dashed arrows are returns and optional paths, and dotted arrows close a record without completing it. The second line of a stage is its owner.

### Receipt cancellation and reinstatement (OPS_RECEIPT_ACTION)

![Workflow OPS_RECEIPT_ACTION (CSHID.001-005)](figures/brd02_wf_receipt_action.dot){width=13}

### Unapplied payment disposition (OPS_DISPOSITION)

![Workflow OPS_DISPOSITION (CSHID.024, 025)](figures/brd02_wf_disposition.dot){width=15}

### BIR 2307 (OPS_CWT_2307)

![Workflow OPS_CWT_2307 (CSHID.026, 027; MKTID.010, 013; DBMID.001)](figures/brd02_wf_cwt2307.dot){width=15}

### Remittance batch (OPS_REMITTANCE)

![Workflow OPS_REMITTANCE (RMTID.009-011, 019, 029, 036)](figures/brd02_wf_remittance.dot)

### Remittance hold (OPS_HOLD)

![Workflow OPS_HOLD (MKTID.002-007, RMTID.021)](figures/brd02_wf_hold.dot)

### Special remittance (OPS_SPECIAL_REMIT)

![Workflow OPS_SPECIAL_REMIT (MKTID.009, RMTID.030, 033)](figures/brd02_wf_special.dot){width=14}

### Reconciliation cycle (OPS_RECON)

![Workflow OPS_RECON (PRCID.001-039)](figures/brd02_wf_recon.dot){width=14}

### Endorsement request (OPS_ENDORSEMENT)

![Workflow OPS_ENDORSEMENT (ADJID.001-010)](figures/brd02_wf_endorsement.dot)

### Direct payment billing (OPS_DP_BILLING)

![Workflow OPS_DP_BILLING (CMRID.008-013)](figures/brd02_wf_dp_billing.dot){width=15}

### BIR certificate submission (OPS_BIR_CERT)

![Workflow OPS_BIR_CERT (CMRID.015)](figures/brd02_wf_bir_cert.dot){width=14}

## Accounting events

Every Operations posting is a business event; Comptrollership configures the GL rule of each event (maker-checker), and no GL account is chosen in code (OQ07). The entries below are the defaults of the build design with the demo chart of accounts. All events carry the party, cost centre and business line, and are priced at the BOOK rate when not in pesos.

<!-- table: widths=5.6,3.8,7.2 caption="Operations accounting events and default entries" size=8 -->
| Event | Transaction | Default entry |
|---|---|---|
| OPS_AR_RECEIPT | AR issued (premium) | Dr bank / cash on hand / Cr unapplied collections (client) |
| OPS_PAYMENT_APPLY | Payment applied per component | Dr unapplied collections / Cr PR by component; commission realised on collection: Dr unrealised commission / Cr commission income, Dr deferred output VAT / Cr output VAT |
| OPS_CWT_RECLASS | 2307 validated | Dr PR2307 / Cr PR by component |
| OPS_CWT_DTIP_OFFSET | 2307 released to the insurer | Dr DTIP / Cr PR2307 |
| OPS_EXCESS_TO_OVERAGES | Excess or unapplied <= 10.00 | Dr unapplied collections / Cr AP overages |
| OPS_MINIMAL_BALANCE_REVERSAL | PR <= 10.00 | Dr minimal balance / write-off / Cr PR by component |
| OPS_UNAPPLIED_REFUND / OPS_UNAPPLIED_RECLASS | Refund / reclass or transfer | Dr unapplied collections / Cr refund payable; Dr unapplied (old) / Cr unapplied (new client or unit) |
| (reversal of the above) | AR or OR cancelled | Original events with negative amounts |
| OPS_RECEIPT_REINSTATE | Reinstatement | Dr bank / Cr unapplied collections, then OPS_PAYMENT_APPLY |
| OPS_AR_INSURANCE_RECEIPT | Insurer non-premium payment | Dr bank / Cr AR Insurer (insurer) |
| OPS_OR_ISSUE | OR (service fee, profit share, commission, incentive, others) | Dr bank, Dr CWT / Cr income by OR type, Cr output VAT; commission ORs make the deferred VAT due |
| OPS_REMITTANCE | Remittance batch approved, per invoice | Dr DTIP, Dr CWT / Cr commission receivable, Cr due to insurer for disbursement |
| OPS_REMIT_INCENTIVE | With Incentives batch | Dr due for disbursement / Cr incentive income, Cr output VAT |
| Booking endorsement posting | Financial endorsement or cancellation | Reversal or addition of the booking entry (booking's rules) |
| OPS_AR_INSURER_SETUP | Decrease after remittance | Dr AR Insurer / Cr DTIP (insurer) |
| OPS_ADJ_COMMISSION | Commission change without premium change | Dr commission receivable / Cr unrealised commission, Cr deferred output VAT (negative reverses) |
| OPS_WRITE_OFF | Minimal balance file | Debit balance: Dr write-off / Cr PR; credit balance: Dr PR / Cr other income |
| OPS_DP_PR_REVERSAL / OPS_DP_REINSTATE | DP premium receivable reversal / reinstatement | Dr DTIP / Cr PR by component (reinstatement reverses); posted only when DP_PR_REVERSAL_POSTING is on |
| OPS_DP_COMMISSION_COLLECT | DP commission collected | Dr bank, Dr CWT / Cr commission receivable; realisation of commission and output VAT |
| OPS_INCENTIVE_ACCRUE | Target incentive earned | Dr incentive receivable / Cr incentive income |
| OPS_INCENTIVE_PASS_ON | Motor Mania pass-on to branches | Dr incentive income / Cr due to branches, then Disbursement |

# Reports and documents

All Operations reports are in the report category Operations, need OPS_REPORT_VIEW to view and OPS_REPORT_EXPORT to download or print, export to PDF, XLSX and CSV, and are archived when run (FR-OP-009). "Draft" marks a layout the BRD does not give; BDOI confirms it through OQ42.

## Cashiering reports

<!-- table: widths=4.4,5.6,6.6 caption="Cashiering reports (Annex II, p.125-127)" size=8 -->
| Code | Name | Content |
|---|---|---|
| CSH-APPLIED-PREM | Applied Premium Report | AR no., date, amount paid, invoice no., payor / client, risk code, count, total (Annex II #1) |
| CSH-APPLIED-COMM | Applied Commission Report | OR no., invoice no., applied invoices, insurer, basic commission, WTAX, EVAT, net commission, total applied (#2) |
| CSH-PDC-WAREHOUSE | Post-dated Checks Warehousing | AR date, AR no., maturity date, client, amount, bank code, branch, check no., market segment (#3) |
| CSH-MINBAL-EXCESS | Minimal Balance of Unapplied Payments (Excess Payments) | AR no., AR date, market unit, section unit, minimal amount, count, total (#4) |
| CSH-CANCELLED-OR | Cancelled Official Receipts | Date issued, OR no., assured, gross, VAT, WTAX, amount, total (#5) |
| CSH-CANCELLED-AR | Cancelled Acknowledgment Receipts | Date issued, reason, AR no., assured, gross, VAT, WTAX, amount, total (#6) |
| CSH-CHECK-PICKUP | Check Pick-Up Requests | Date and time of request, AR no., date issued, amount, assured, collections handler / requestor, count, total (#7) |
| CSH-PRIORITY-POSTED | Priority Posted Accounts | OR no., payor, amount paid, date paid, encoder, branch, status, invoice, policy, risk code, corporate department (#8) |
| CSH-UNAPPLIED-COMM-MANCOM | Unapplied Commission Receivable Extract for Mancom | Draft (#9) |
| CSH-UNAPPLIED-COMM-YTD | Unapplied Commission Receivable YTD Balance | Draft (#10) |
| CSH-MINBAL-PREMIUM / CSH-MINBAL-COMMISSION | Premium / Commission Minimal Balance | Draft (#11, #12) |
| CSH-DAILY-CASH-REC | Daily Cash Reconciliation | Draft (#13) |
| CSH-ADVANCE-PAYMENT | Advance Payment Transactions (Auto-Credit) | Draft (#14) |
| CSH-PAYMENT-REVERSAL | Payment Reversals | Draft (#15) |
| CSH-DIRECT-PAYMENT | Direct Payment Accounts | Draft (#16) |
| CSH-REINSTATEMENT-MON / CSH-REINSTATEMENT | Reinstatement Monitoring / Reinstatements | Draft (#17, #20) |
| CSH-REAPPLICATION | Re-Application | Draft (#18) |
| CSH-CERT-OF-PAYMENT | Certification of Payment | AR no., policy no., requesting market unit, date issued, count (#19); also a PDF certificate |
| CSH-CWT | CWT | Draft (#21) |
| CSH-AR-OUTSTANDING | AR Outstanding with Ageing | Outstanding premium per client and invoice with ageing buckets (OQ43) |
| CSH-BATCH-RUN | Payment Batch Run Report | Successful, unsuccessful, applied, unapplied and failed records of a run (BRQID.006) |
| CSH-2307-TXN | BIR 2307 Transaction Report | Tags of a 2307 batch per insurer with amounts and certificates (CSHID.027) |

## Remittance reports

<!-- table: widths=4.4,5.6,6.6 caption="Remittance reports (Annex III, p.127-129)" size=8 -->
| Code | Name | Content |
|---|---|---|
| REM-TRACKER | Remittance Tracker | Insurer, remit type, batch no., accounts extracted, net due, incentive amount, due date (#1) |
| REM-SPECIAL-REGISTER | Special Remittance Register | Invoice, policy, insurer, assured, reason, processor, requestor, segment, date posted, date pushed to Disbursement, processed / checking / approval age (#2) |
| REM-SCHEDULE-NORMAL | Remittance Schedule Normal | Invoice, policy, endorsement, with COI, assured, risk code, record status, dates, OR delivery, paid AR, realised commission and VAT, WTAX, DTIP, net due, OR date / no. / amount; Mall Assurance columns draft (#3) |
| REM-SCHEDULE-SPECIAL | Remittance Schedule Special | As Normal without Mall Assurance (#4) |
| REM-DTIP-SUMMARY | DTIP Status Report - Summary | Insurer, no. of accounts, amount, remittance status (#5) |
| REM-DTIP-DETAIL | DTIP Status Report - Detailed | Invoice, assured, inception, expiry, insurer, risk code, product, basic premium, AR paid, for remittance, commission and CAT collected, last remittance and payment, ageing, outstanding DTIP and RA, uncollected commission and VAT (#6) |
| REM-SCHEDULE-INCENTIVE | Remittance Schedule with Incentives | As Normal with date of incentive expiry, net due before incentives, basic premium (#7) |
| REM-REMITTED-BATCH | List of Remitted Accounts with Batch Number | Draft (#8) |
| REM-PAIDAR-OVER-DTIP | Paid AR Higher than DTIP Balance | Invoice, insurer, paid AR, DTIP, excluded (RMTID.015) |
| REM-OR-EXCEPTION | OR vs Paid PR Exception Report | Reference, OR amount, paid PR, status, reason, summary per upload (RMTID.016) |
| REM-EXCLUDED | Accounts Excluded from Remittance | Invoices excluded by hold, pending negative adjustment and the other rules (RMTID.020) |
| REM-HOLD | Remittance Hold Register | Holds with status, dates and processor (MKTID.002-007) |

## Adjustment reports

<!-- table: widths=4.4,5.6,6.6 caption="Adjustment reports" size=8 -->
| Code | Name | Content |
|---|---|---|
| ADJ-DAILY | Adjustment and Daily Endorsement Report | Requests raised or posted in the period by type and user; exported daily (ADJID.016) |
| ADJ-VALIDATION-LIST | Validation List | One row per GL line of posted requests with the BRD fields (ADJID.017) |
| ADJ-REGISTER | Adjustment Report | Type, amount, reason by account, segment, AO and risk type (ADJID.019) |
| ADJ-AGING | Transaction Aging | Requests with ageing buckets from request to completion (ADJID.021) |
| ADJ-MINBAL-FILE | Minimal Balance Write-off Summary | Invoices written off or credited per file (ADJID.026) |

## Production Reconciliation reports

<!-- table: widths=4.4,5.6,6.6 caption="Production Reconciliation reports (Annex IV, p.129-130)" size=8 -->
| Code | Name | Content |
|---|---|---|
| PRC-SUMMARY | Production Reconciliation Summary | Insurer, matched items and amount, matched with discrepancies items and amount, unmatched items and amount, remarks (#1) |
| PRC-UNMATCHED-LOC | Unmatched Accounts per Location | Insurer, disposition, location, items, amount (#2) |
| PRC-UNMATCHED-AO | Unmatched Accounts per Marketing AO / AB | Insurer, disposition, items, amount; booked and pre-booked (#3) |
| PRC-DISPOSITION | Summary per Disposition | Disposition, insurer, Marketing AO / AB, items, amount (#4) |
| PRC-REGISTER | Production Register | Annex IV #5 columns; variants plain, insurer feedback, Marketing feedback, both (PRCID.017, 018, 020) |
| PRC-UNBOOKED | Unbooked Accounts and Status | Insurer-only lines with their status (PRCID.019, 033) |
| PRC-EXTRACT-LOG | Production Register Extraction Log | Item count and extraction time per extract (PRCID.034) |
| PRC-UNMATCHED-FEEDBACK | Unmatched Accounts with Feedback and Disposition | Company concerned, instruction, feedback, disposition (PRCID.038) |
| PRC-EARLY-INCENTIVE | Early Incentive Validation | Account, rule, rate, window, remittance date, result (PRCID.028) |

## Commission Receivables reports

<!-- table: widths=4.4,5.6,6.6 caption="Commission Receivables reports" size=8 -->
| Code | Name | Content |
|---|---|---|
| CMR-COMMISSION-RECEIVABLE | Commission Receivable - Direct Payment vs Regular | Commission booked, collected and outstanding per insurer; DP / direct bill against regular; net after partial remittances (CMRID.004) |
| CMR-PRODUCTION-YEARLY | Production per Branch and Insurer - Yearly | Total production and commission per branch and insurer, estimated items (CMRID.014) |
| CMR-DP-STATUS | Direct Payment Accounts per Status | DP accounts by tag (CMRID.008) |
| CMR-INCENTIVE | Incentive Runs | Runs ending in the period with the incentive per invoice and exclusions (CMRID.005, 006) |
| CMR-FEEDBACK-SLA | Insurer Feedback Timeline | Billings with sent date, due date and answer date (CMRID.011) |
| CMR-BIR-CERT | BIR Certificate Submissions | Certificates with their ORs and status (CMRID.010, 015) |

## Documents

<!-- table: widths=4.6,3.2,8.8 caption="Operations documents" size=8.5 -->
| Document | Output | Content |
|---|---|---|
| Acknowledgement Receipt / Official Receipt | PDF | Receipt number, date, payor, amount in words and figures, mode, invoice lines; OR with gross, VAT and WTAX (draft layout, OQ42) |
| Certification of Payment | PDF | AR, policy, requesting unit, date |
| REMITTANCE_SCHEDULE | PDF and XLSX | Batch, insurer, type, lines and totals (Annex III #3, #4, #7) |
| REMITTANCE_PAYMENT_REQUEST | PDF | Payable, insurer, batch, approver |
| Production register | XLSX, protected | Annex IV #5 columns; Remarks and Incentive editable |
| PRODRECON_COVER_LETTER | E-mail | Insurer, extract, period, row count, instructions |
| DP billing workbook | XLSX, protected | Accounts with premium, commission, VAT, WTAX, net |
| ENDORSEMENT_SLIP | PDF | Annex V fields, number ES-yyyy-n |
| VALIDATION_SLIP | PDF | Validation status, before / after, insurer breakdown, GL entries |
| BIR 2307 transaction report | PDF | Tags of a batch per insurer |

# Interfaces and integration

Figure 12 shows the interfaces of Operations. The modules exchange data only through the invoice ledger, its events and its ports; each external system is behind a port whose default adapter is an upload, the in-app Disbursement queue or the extract repository.

![Interfaces of Operations (dashed = parked or replaced by a later BRD)](figures/brd02_integration.dot)

<!-- table: widths=4.4,1.8,5.6,2.6,2.6 caption="Interfaces and flow-in feeds" status=Status size=8 -->
| Interface / feed | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| OPS_INVOICE_FEED (booking) | In | Booked invoices, endorsements and returns after commit; replay | RMTID.038 | BUILT |
| Booking posting and service invoice | Out | Endorsement and cancellation posting; service invoice issue and credit | ADJID.011, 014 | BUILT |
| Catalogue | In | Endorsement rating, commission rates, package limits | ADJID.008, 014 | BUILT |
| Placement payment gate | Out | Applications and pre-booked payments per ARN | CSHID.020 | BUILT |
| Accounting engine | Out | Business events of section 5.3 | CSHID.012-014; ADJID.011 | BUILT |
| E-mail outbox | Out | Protected register, schedule, billing; passwords separately | PRCID.007, 008; MKTID.001; CMRID.009 | BUILT |
| INSURER_REMIT_OR | In (upload) | Insurer OR schedules | RMTID.012, 013 | BUILT |
| INSURER_PRODUCTION | In (upload) | Insurer production reports | PRCID.009, 022 | BUILT |
| INSURER_DP_RESPONSE | In (upload) | Insurer answers to DP billings | CMRID.009 | BUILT |
| COLLECTION_CHECK_PICKUP | In (upload) | Checks for pick-up | CSHID.009 | SUPERSEDED |
| COLLECTION_CWT2307 | In (upload) | BIR 2307 tags | CSHID.026; MKTID.013 | SUPERSEDED |
| COLLECTION_COMMISSION_PAYMENT | In (upload) | Commission payment details | CSHID.007 | BUILT |
| COLLECTION_HOLD | In (upload) | Hold requests | RMTID.021; MKTID.003 | BUILT |
| COLLECTION_SPECIAL_REMIT | In (upload) | Special remittance requests | RMTID.030; MKTID.009 | BUILT |
| COLLECTION_DP_LIST | In (upload) | DP lists of HO and branches | CMRID.001 | SUPERSEDED |
| COLLECTION_DP_RETURNED, COLLECTION_REFUND | Out | Rejected DP accounts; refunds | CMRID.009; CSHID.024 | BUILT |
| DISBURSEMENT_REQUEST / _STATUS | Out / In | Payment requests; DV and status (in-app queue) | RMTID.034; DBMID.001 | SUPERSEDED |
| Shared drive (FileDropPort) | Out | Extract files | RMTID.001; PRCID.005 | PARKED |
| MarketingFeed, ClaimsFeed | In | Marketing data; claims for special remittance | BRQID.004; MKTID.009 | PARKED |

SUPERSEDED means built as a seam in Operations, with the replacement designed in BRD-4 Collections or BRD-5 Accounting and Disbursement (R4). PARKED means the transport waits for BDOI's specification (OQ17, OQ45, OQ46).

> [!PARKED] Parked transports
> Insurer SFTP / portal channels, BDO bank file transports (FS01 / FS04) and the shared drive are not specified in the BRD (OQ03, OQ17, OQ22, OQ29). Adding a transport is a new adapter of the existing port, with no change to the modules.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD annex p.115-120)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Cashiering HO 11 + branches 5, TL / TH 5 + 5; Remittance 4; Prod Recon 4; Adjustment 6; Commission 5 + 4 + 2 | Within the BRD-1 sizing (145 concurrent); Operations adds fewer than 50 users | FIT |
| Volumes and growth | Not stated | Assumed within the BRD-1 sizing; to confirm (OQ44) | OPEN |
| Response time | Under 5 seconds for every function; section navigation under 5 s; batch print of 50 documents under 10 s | Online p95 under 3 seconds; extraction, matching, uploads and batch print run as background jobs with progress | FIT |
| Peak | 15th and 30th (Cashiering, Remittance); 1st-2nd week (Prod Recon); Q4 (Adjustment, Commission); 07:30-18:00 | Remittance extraction at 20:00 PHT and the minimal balance sweep at 04:00 PHT, outside the peak hours; the other jobs in section 9.3 | FIT |
| Availability | 99.9%; use 07:00-18:30 (07:30-18:00 Adjustment / Commission); maintenance per bank standard | Same deployment as BRD-1 (window 07:00-22:00 governs, OQ44) | FIT |
| Recovery | RTO 4 hours, RPO 24 hours | Platform backup and recovery; one BIBS-wide NFR set being agreed (XQ08) | FIT |
| Retention | Application, database, audit logs and history 5 years online, 15 years archive; backup every 4 hours kept 7 years | BRD-1 retention framework with backup retention 7 years | OPEN |
| Anonymisation | No | None | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Security | Authorised users only; protected files | Role-based access, four-eyes rules, protected e-mails (FR-OP-002, 013, 035) | FIT |
| Audit | All actions logged | Append-only audit, workflow history, ledger movements (FR-OP-004, 024) | FIT |

# Configuration items owned by the System Administrator

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.6,7.8 caption="Operations parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| REMIT_CHECK_HOLD_DAYS | 3 | Banking days a check is held before its payment is remitted (RMTID.017) |
| REMIT_PAIDAR_OVER_DTIP_MODE | EXCLUDE | EXCLUDE or CAP an invoice whose paid AR exceeds the DTIP (RMTID.014, OQ19) |
| REMIT_FILE_PATTERN | (blank) | File naming of remittance extracts (OQ17) |
| RECON_TOLERANCE | 1.00 | Reconciliation tolerance per amount field (PRCID.026) |
| RECON_MATCH_KEYS | INVOICE_NO,POLICY_NO | Keys pairing insurer lines with booked lines (OQ30) |
| PRODRECON_FILE_PATTERN | (blank = <INSURER>_PRODREG_<yyyyMM>_<seq>) | File naming of the production register (PRCID.004) |
| MIN_BALANCE_AUTO_MAX | 10.00 | Minimal balances reversed automatically (CSHID.016) |
| MIN_BALANCE_FILE_RANGE | 10.00-100.00 | Range of the minimal balance file (ADJID.026) |
| CWT_APPLICATION_PERCENT | 98 | Share of premium applied for 2% CWT clients (CSHID.020) |
| CMR_FEEDBACK_WORKING_DAYS | 10 | Working days for the insurer's DP answer (CMRID.011) |
| CMR_DP_COLLECTION_BANK | (blank) | Proposed bank account of DP commission collections |
| DP_PR_REVERSAL_POSTING | false | Post the GL entry of the DP PR reversal (MKTID.012) |
| ADJ_BASELINE_PERCENT | 100 | Over-adjustment baseline (ADJID.028) |
| OPS_BOOK_RATE_TYPE | BOOK | Exchange rate type of Operations postings (CSHID.012) |
| OPS_COMMISSION_REALIZATION | ON_COLLECTION | When commission is realised (seeded by booking) |
| CASH_BANK_ACCOUNT / CASH_ON_HAND_ACCOUNT | (blank) | GL accounts of the @BANK role (OQ07) |

## Lists of values

<!-- table: widths=5,11.6 caption="Lists of values" size=8 -->
| List | Values delivered |
|---|---|
| AR_CLASS | Premium: Bills Payment; OTC; Trade; CLPC; PDC; Direct Credit. Non-premium: Refund; Other expenses; AR Insurance |
| OR_TYPE | Service Fee Income; Insurance Profit Share; Commissions (with VAT / withholding tax); Incentives; Others (discount) |
| RECEIPT_CANCEL_REASON | Premium: Discrepancy in check amount; No signature; Incorrect payee; Check not picked-up; Incorrect check details provided by the client or Insurer; Others. Commission: Incorrect payment details; Negative VAT; Discount not applied. PDC: retrieval / replacement - for replacement; pull out. General: Bounced or returned check; Error in issuance details; Error printing; Check not picked up; Double issuance; Others |
| REINSTATEMENT_REASON | Premium: Due to cancellation; Mis-application of payment; Withholding tax adjustment; Others. Direct payment: Due to double reversal; Due to wrong details of OR; Withholding tax adjustment; Due to cancellation; Others |
| DISPOSITION_TYPE | Apply to other invoice; DST payment application; Refund; Reclass; Transfer to other marketing unit; Others (customizable) |
| REMITTANCE_TYPE | With Incentives; Normal - Dollar; Normal - Peso |
| REMIT_EXCLUSION_REASON | Account on hold; Pending negative adjustment; Written off; Check within the holding period or not cleared; Paid AR greater than DTIP; Payment not yet applied and posted; Others |
| REMIT_RETURN_REASON, HOLD_REASON, DP_FEEDBACK_REASON, ENDORSEMENT_DOC_TYPE | Others only, until BDOI supplies the values (OQ24, OQ32, OQ40) |
| SPECIAL_REMIT_CONDITION | Claims; Renewal; Installment due; Immediate OR issuance |
| RECON_COMPANY_CONCERNED, RECON_DISPOSITION | Demo values until BDOI supplies them (OQ31) |
| ENDORSEMENT_TYPE | Financial: 8 types; non-financial: 5 types; internal adjustment (ADJID.002, 004) |
| ENDORSEMENT_REQUEST_TYPE | The 12 request types of Annex V |
| CANCELLATION_REASON | The 33 reasons of Annex V (shared with BRD-1) |
| ADJ_RETURN_REASON | Not qualified for posting; Incomplete supporting documents; Incorrect request details; Incorrect amounts; Duplicate request; Others |
| INCENTIVE_EXCLUSION_RULE | Negative production amounts; Erroneous bookings |
| OPS_EXTERNAL_LINK | Empty; links "Name|https://url" added by the administrator |

## Jobs

<!-- table: widths=5,3.4,8.2 caption="Operations jobs (times in PHT)" size=8.5 -->
| Job | Default | Purpose |
|---|---|---|
| OPS_INVOICE_FEED_REPLAY | Manual | Rebuild or replay the invoice ledger from booking |
| PREBOOKED_REMATCH | Every 2 hours | Apply pre-booked payments of accounts now booked |
| PAYMENT_AUTOMATCH | Hourly | Match unapplied payments again |
| PDC_MATURITY | 08:30 (00:30 UTC) | Turn matured PDCs into payments |
| MINIMAL_BALANCE_SWEEP | 04:00 (20:00 UTC) | Minimal balance reversal and overages |
| REMITTANCE_EXTRACTION | 20:00 | Scheduled remittance extraction |
| HOLD_EXPIRY | 08:15 | Release expired holds; notify holds expiring the next day |
| PRODUCTION_EXTRACT | 09:00 | Extract registers of insurers due |
| RECON_AUTOMATCH | Manual | Match open cycles again (OQ30) |
| ADJ_DAILY_REPORT | 18:00 | Archive the daily endorsement report |
| DP_FEEDBACK_SLA | 09:30 | Flag overdue insurer answers |

## Masters and rules maintained by the business

<!-- table: widths=5,4.6,7 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Receipt series | Cashiering TL (another CASH_SERIES_MANAGE holder) | FR-OP-010 |
| Payment file layouts, minimal balance rules | Cashiering TL | FR-OP-015, 023 |
| Disposition type rules (action, approval) | System Administrator (change request until OQ15) | FR-OP-022 |
| Early remittance incentive rules | Remittance TL | FR-OP-038 |
| Extract schedules | Recon Handler | FR-OP-070 |
| Incentive schemes and tiers | Commission TL | FR-OP-095 |
| Accounting rules of the Operations events | Comptrollership (maker-checker) | FR-OP-027 |
| Flow-in feeds (schedule, active) | System Administrator | FR-OP-131 |
| Roles and permissions | Business Administrator via access request (BRD-11) | FR-OP-002 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-OP-01 | The main BRD (pp.8-132) and Addendum 1 (pp.1-7) are the baseline; the addendum governs RMTID.002 and ADJID.014 | R1, R2 |
| A-OP-02 | Every Operations function starts from the invoice booked by BRD-1; Operations does not book | RMTID.038 |
| A-OP-03 | GL accounts stay in BIBS; Comptrollership configures the rules of the Operations events | OQ07 (BRD-5) |
| A-OP-04 | Cancellations, reinstatements, refunds, reclasses and transfers need a TL approval | OQ06, OQ15 |
| A-OP-05 | Cashiering is the single payment intake; New Business placement consumes its result | OQ12 |
| A-OP-06 | Paid AR above DTIP is excluded from extraction until BDOI decides | OQ19 |
| A-OP-07 | Remittance batches are per lead insurer for co-insured invoices | OQ50 |
| A-OP-08 | The MKTID activities are performed in BIBS by Marketing users as described in section 4.7 | OQ45 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-OP-01 | BDOI provides the bank and channel file layouts (FS01, FS04, CLPC, PDC) | FR-OP-015 (OQ03, OQ04) |
| D-OP-02 | BDOI provides the BIR ATP series and formats | FR-OP-010 (OQ05) |
| D-OP-03 | Comptrollership provides the GL accounts and the BOOK rate source | FR-OP-027 (OQ07, OQ08) |
| D-OP-04 | BRD-4 Collections delivers the in-app Collection feed; BRD-5 delivers the Disbursement module | Section 7 (OQ01, OQ02) |
| D-OP-05 | BDOI gives the incentive schemes (targets, tiers, amounts) and the early remittance rates | FR-OP-038, 095 (OQ23, OQ39) |
| D-OP-06 | BDOI gives the report layouts not in the annex | Section 6 (OQ42) |
| D-OP-07 | The e-mail relay of the BIBS environment is available for insurer e-mails | FR-OP-073, 092, 110 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-2 (status from R3 and the cross-BRD decisions, R7)" status=Status size=8 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| OQ01 | Systems of BRQID.004 (Collection, Accounting, Disbursement, Marketing, Claims): interface, data, frequency | FR-OP-130 | PARTIAL |
| OQ02 | Disbursement: payload, DV numbers and statuses | FR-OP-120 | ANSWERED |
| OQ03 / OQ04 | Payment file layouts; file encryption | FR-OP-015 | OPEN |
| OQ05 | Receipt series and BIR ATP | FR-OP-010 | OPEN |
| OQ06 | Approvers of cancellations and reinstatements | FR-OP-013, 014 | OPEN |
| OQ07 | Chart of accounts and default entries | FR-OP-027; section 5.3 | PARTIAL |
| OQ08 | BOOK rate source and date | FR-OP-027 | OPEN |
| OQ09 | Mode-of-payment hierarchy "Check, Cash" | FR-OP-019 | OPEN |
| OQ10 | Source of the 2% CWT flag | FR-OP-018 | OPEN |
| OQ11 | Minimal balance rules and targets | FR-OP-023, 059 | OPEN |
| OQ12 | Pre-booked matching key | FR-OP-018 | PARTIAL |
| OQ13 | Check pick-up source | FR-OP-017 | OPEN |
| OQ14 / OQ49 | Commission OR grouping | FR-OP-021 | OPEN |
| OQ15 | Disposition list and approvers | FR-OP-022 | PARTIAL |
| OQ16 | BIR 2307 routing and posting moment | FR-OP-026 | OPEN |
| OQ17 | Remittance type rules, schedule, naming, shared drive | FR-OP-030 | PARTIAL |
| OQ18 | End-of-day extraction trigger | FR-OP-030 | OPEN |
| OQ19 | Paid AR greater than DTIP | FR-OP-031 | OPEN |
| OQ20 | Holding period start and cleared status | FR-OP-031 | OPEN |
| OQ21 | Remittance approval and status model | FR-OP-033 | OPEN |
| OQ22 | Insurer OR layout and tolerance; MKTID.001 as a Marketing request | FR-OP-037, 110 | OPEN |
| OQ23 | Early remittance incentive rates and window | FR-OP-038, 078 | OPEN |
| OQ24 | Hold roles, maximum and extensions | FR-OP-111 | OPEN |
| OQ25 | Special remittance approvers and conditions | FR-OP-112 | PARTIAL |
| OQ26 | Write-off ownership | FR-OP-059 | OPEN |
| OQ27 | Definition of estimated items | FR-OP-097 | OPEN |
| OQ28 | Lock reasons (Comptrollership) | FR-OP-006 | OPEN |
| OQ29 | Recon frequency, template, naming, channel | FR-OP-070 to 074 | PARTIAL |
| OQ30 | Recon keys and automatch timing | FR-OP-075 | OPEN |
| OQ31 | Company-concerned and disposition lists | FR-OP-077 | OPEN |
| OQ32 | Endorsement ownership, numbering, documents | FR-OP-050, 052 | PARTIAL |
| OQ33 | Package TSI limits and co-insurance | FR-OP-055, 007 | OPEN |
| OQ34 | Credit memo on commission decrease | FR-OP-054 | OPEN |
| OQ35 | Extension of cover: financial or non-financial | FR-OP-050, 053 | OPEN |
| OQ36 | Refund basis of partial cancellations | FR-OP-054 | OPEN |
| OQ37 | Over-adjustment baseline | FR-OP-058 | OPEN |
| OQ38 | DP list sources and billing formats | FR-OP-090, 092 | PARTIAL |
| OQ39 | Incentive schemes | FR-OP-095 | PARTIAL |
| OQ40 | Insurer feedback reasons; SLA start | FR-OP-093 | OPEN |
| OQ41 | BIR certificates of insurers | FR-OP-096 | ANSWERED |
| OQ42 / OQ43 | Report layouts; ageing buckets | Section 6 | PARTIAL |
| OQ44 | NFR alignment | Section 8 | OPEN |
| OQ45 | Marketing scope | Section 4.7 | PARTIAL |
| OQ46 | Claims needs | FR-OP-112 | PARTIAL |
| OQ47 | Search log purpose and retention | FR-OP-024 | OPEN |
| OQ48 | Operations access matrix | Section 3 | PARTIAL |
| OQ50 | Co-insured remittance per insurer share | FR-OP-007 | OPEN |


# Traceability

Every BRD-2 requirement ID (169) is met by at least one FR. The page is the page of the BRD-2 PDF ("add." = Addendum 1). The build status comes from the requirements traceability (R4): BUILT = built and tested; SEAM = built and tested, with a value, layout or integration BDOI has not given kept as a seam; SUPERSEDED = built as a seam, with the replacement designed in BRD-4 or BRD-5. API paths start with `/api/v1`.

## General requirements (BRQID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: General requirements (BRQID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| BRQID.001 | p.16 | FR-OP-001 | /login | /auth | BUILT |
| BRQID.002 | p.16 | FR-OP-002 | /login | /auth | BUILT |
| BRQID.003 | p.17 | FR-OP-003 | /operations, section workbenches | GET /ops/home | BUILT |
| BRQID.004 | p.18 | FR-OP-130 | /operations/interfaces | /ops/flow-in | SUPERSEDED |
| BRQID.005 | p.18 | FR-OP-131 | /operations/interfaces | /ops/flow-in/feeds, runs, records | SEAM |
| BRQID.006 | p.19-20 | FR-OP-008 | /bulk, /operations/interfaces | /bulk | BUILT |

## Cashiering (CSHID) and Disbursement (DBMID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Cashiering (CSHID) and Disbursement (DBMID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| CSHID.001 | p.23-24 | FR-OP-011, FR-OP-013, FR-OP-014 | /cashiering/receive, /cashiering/receipts/:id | POST /cashiering/receipts/ar, /receipts/{id}/cancel, /reinstate | SEAM |
| CSHID.002 | p.24-25 | FR-OP-012, FR-OP-013 | /cashiering/receipts | POST /cashiering/receipts/or | SEAM |
| CSHID.003 | p.25-26 | FR-OP-013 | /cashiering/receipts/:id | POST /cashiering/receipts/{id}/cancel, /reinstate, /receipt-actions/{id}/approve | SEAM |
| CSHID.004 | p.26-27 | FR-OP-014 | /cashiering/receipts/:id | POST /cashiering/receipts/{id}/cancel, /reinstate, /receipt-actions/{id}/approve | SEAM |
| CSHID.005 | p.27-28 | FR-OP-014 | /cashiering/receipts/:id | POST /cashiering/receipts/{id}/cancel, /reinstate, /receipt-actions/{id}/approve | SEAM |
| CSHID.006 | p.28-29 | FR-OP-010 | /cashiering/series | /cashiering/series | SEAM |
| CSHID.007 | p.29 | FR-OP-021 | /cashiering/commission-ors, /operations/interfaces | GET /cashiering/commission-payments, POST /commission-payments/issue; feed COLLECTION_C... | SEAM |
| CSHID.008 | p.29-32 | FR-OP-015, FR-OP-016 | /cashiering/uploads, /cashiering/pdc | bulk PAY_BILLS / PAY_TRADE / PAY_CLPC / PAY_DIRECT_CREDIT / PAY_PDC; /cashiering/pdc | SEAM |
| CSHID.009 | p.32-33 | FR-OP-017 | /cashiering/pickups | /cashiering/pickups; feed COLLECTION_CHECK_PICKUP | SUPERSEDED |
| CSHID.010 | p.33-34 | FR-OP-024 | /cashiering/receipts | GET /cashiering/receipts | BUILT |
| CSHID.011 | p.34-35 | FR-OP-024 | /cashiering/receipts/:id (History) | GET /cashiering/receipts/{id} | BUILT |
| CSHID.012 | p.35 | FR-OP-013, FR-OP-027 | /cashiering/receipts/:id (Journal) | /cashiering/receipts/* | SEAM |
| CSHID.013 | p.35-36 | FR-OP-014, FR-OP-027 | /cashiering/receipts/:id (Journal) | /cashiering/receipts/* | SEAM |
| CSHID.014 | p.36 | FR-OP-027 | /cashiering/receipts/:id (Journal) | /cashiering/receipts/* | SEAM |
| CSHID.015 | p.37 | FR-OP-010 | /cashiering/series | /cashiering/series | BUILT |
| CSHID.016 | p.37-38 | FR-OP-023 | /cashiering/setup | GET /cashiering/minimal-balance/rules, POST /minimal-balance/sweep | SEAM |
| CSHID.017 | p.38 | FR-OP-009 | /reports (Report Centre) | /reports | BUILT |
| CSHID.018 | p.38 | FR-OP-009 | /reports (Report Centre) | /reports | BUILT |
| CSHID.019 | p.39-40 | FR-OP-025 | /cashiering/print | /cashiering/print-batches | BUILT |
| CSHID.020 | p.40-41 | FR-OP-018, FR-OP-132 | /cashiering/receive, /cashiering/prebooked | POST /cashiering/payments, /payments/preview, GET /prebooked, POST /matching/run | SEAM |
| CSHID.021 | p.41 | FR-OP-020 | /cashiering/receipts | POST /cashiering/receipts/ar (AR_INSURANCE) | BUILT |
| CSHID.022 | p.42 | FR-OP-019 | /cashiering/receive | POST /cashiering/payments/preview, /payments | BUILT |
| CSHID.023 | p.42,125-127 | FR-OP-028 | /reports (Report Centre) | /reports (CSH-*) | SEAM |
| CSHID.024 | p.42-43 | FR-OP-022 | /cashiering/unapplied, /cashiering/unapplied/:id | /cashiering/unapplied/* | SEAM |
| CSHID.025 | p.43-44 | FR-OP-022 | /cashiering/unapplied, /cashiering/unapplied/:id | /cashiering/unapplied/* | SEAM |
| CSHID.026 | p.44 | FR-OP-026 | /cashiering/cwt | /cashiering/cwt/*; feed COLLECTION_CWT2307 | SEAM |
| CSHID.027 | p.44-45 | FR-OP-026 | /cashiering/cwt | /cashiering/cwt/*; feed COLLECTION_CWT2307 | SEAM |
| DBMID.001 | p.46 | FR-OP-120, FR-OP-121 | /cashiering/cwt, /operations/disbursements | POST /cashiering/cwt/batches/{id}/route, /release; /ops/disbursements | SUPERSEDED |

## Remittance (RMTID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Remittance (RMTID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| RMTID.001 | p.49 | FR-OP-030 | /remittance/extraction | GET/POST /remittance/runs, GET /runs/{id}/tags | SEAM |
| RMTID.002 | p.50; add. p.4-5 | FR-OP-032 | /remittance/batches/:id | POST /remittance/batches/{id}/exclude, /restore | BUILT |
| RMTID.003 | p.50-51 | FR-OP-030 | /remittance/extraction | GET/POST /remittance/runs, GET /runs/{id}/tags | SEAM |
| RMTID.004 | p.51-52 | FR-OP-030 | /remittance/extraction, /remittance/dtip | POST /remittance/runs, GET/POST /eod-requests | SEAM |
| RMTID.005 | p.52 | FR-OP-030 | /remittance/extraction, /remittance/dtip | POST /remittance/runs, GET/POST /eod-requests | SEAM |
| RMTID.006 | p.52 | FR-OP-031 | /remittance/batches/:id | GET /remittance/batches/{id} | BUILT |
| RMTID.007 | p.52-53 | FR-OP-030 | /remittance/extraction | GET/POST /remittance/runs, GET /runs/{id}/tags | SEAM |
| RMTID.008 | p.53 | FR-OP-030 | /remittance/extraction | GET/POST /remittance/runs, GET /runs/{id}/tags | SEAM |
| RMTID.009 | p.53-54 | FR-OP-033 | /remittance/batches, /remittance/batches/:id | POST /remittance/batches/{id}/submit, /approve, /return, /assign | BUILT |
| RMTID.010 | p.54 | FR-OP-033, FR-OP-035 | /remittance/batches, /remittance/batches/:id | POST /remittance/batches/{id}/submit, /approve, /return, /assign | BUILT |
| RMTID.011 | p.54-55 | FR-OP-034 | /remittance/batches/:id (Documents) | GET /remittance/batches/{id}/documents/{kind} | SEAM |
| RMTID.012 | p.55-56 | FR-OP-037 | /remittance/insurer-or | POST /remittance/insurer-or/upload, GET /insurer-or/runs; feed INSURER_REMIT_OR | SEAM |
| RMTID.013 | p.56 | FR-OP-037 | /remittance/insurer-or | POST /remittance/insurer-or/upload, GET /insurer-or/runs; feed INSURER_REMIT_OR | SEAM |
| RMTID.014 | p.56-57 | FR-OP-031 | /remittance/extraction | /remittance/runs; report REM-PAIDAR-OVER-DTIP | SEAM |
| RMTID.015 | p.57 | FR-OP-031 | /remittance/extraction | /remittance/runs; report REM-PAIDAR-OVER-DTIP | SEAM |
| RMTID.016 | p.57-58 | FR-OP-037 | /remittance/insurer-or | POST /remittance/insurer-or/upload, GET /insurer-or/runs; feed INSURER_REMIT_OR | SEAM |
| RMTID.017 | p.58-59 | FR-OP-031 | /remittance/extraction | GET /remittance/runs/{id}/tags | SEAM |
| RMTID.018 | p.59 | FR-OP-031 | /remittance/extraction | GET /remittance/runs/{id}/tags | SEAM |
| RMTID.019 | p.59-60 | FR-OP-033, FR-OP-035 | /remittance/batches, /remittance/batches/:id | POST /remittance/batches/{id}/submit, /approve, /return, /assign | BUILT |
| RMTID.020 | p.60-61 | FR-OP-031 | /remittance/extraction, /remittance/holds | /remittance/runs, /holds | BUILT |
| RMTID.021 | p.61 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; job HOLD_EXPIRY | SEAM |
| RMTID.022 | p.61-62 | FR-OP-031 | /remittance/extraction | /remittance/runs | BUILT |
| RMTID.023 | p.62 | FR-OP-038 | /remittance/incentive-rules | GET/POST /remittance/incentive-rules, PUT /incentive-rules/{id} | SEAM |
| RMTID.024 | p.62-63 | FR-OP-032 | /remittance/dtip, /remittance/batches | GET /remittance/dtip, /batches, /runs/{id}/tags | BUILT |
| RMTID.025 | p.63 | FR-OP-039 | /remittance/dtip, /remittance/batches | GET /remittance/dtip, /batches, /runs/{id}/tags | BUILT |
| RMTID.026 | p.63 | FR-OP-005 | /operations/invoices, /operations/invoices/:no | GET /ops/invoices, /invoices/{no} | BUILT |
| RMTID.027 | p.64 | FR-OP-032 | /remittance/dtip, /remittance/batches | GET /remittance/dtip, /batches, /runs/{id}/tags | BUILT |
| RMTID.028 | p.64 | FR-OP-031 | /remittance/batches/:id | GET /remittance/batches/{id} | BUILT |
| RMTID.029 | p.64-65 | FR-OP-033 | /remittance/batches, /remittance/batches/:id | POST /remittance/batches/{id}/submit, /approve, /return, /assign | BUILT |
| RMTID.030 | p.65 | FR-OP-040 | /remittance/special, /remittance/special/:id | /remittance/special/*; feed COLLECTION_SPECIAL_REMIT | SEAM |
| RMTID.031 | p.65-66 | FR-OP-031 | /remittance/extraction, /remittance/holds | /remittance/runs, /holds | BUILT |
| RMTID.032 | p.66 | FR-OP-005 | /operations/invoices, /operations/invoices/:no | GET /ops/invoices, /invoices/{no} | BUILT |
| RMTID.033 | p.66-67 | FR-OP-040 | /remittance/special, /remittance/special/:id | /remittance/special/*; feed COLLECTION_SPECIAL_REMIT | SEAM |
| RMTID.034 | p.67 | FR-OP-036, FR-OP-120 | /remittance/batches/:id, /operations/disbursements | /ops/disbursements/* | SUPERSEDED |
| RMTID.035 | p.67 | FR-OP-031 | /remittance/extraction, /remittance/holds | /remittance/runs, /holds | BUILT |
| RMTID.036 | p.67-68 | FR-OP-036 | /remittance/batches/:id, /operations/invoices/:no | GET /remittance/batches/{id}, /ops/invoices/{no} | BUILT |
| RMTID.037 | p.68 | FR-OP-097 | /commission/estimated | GET/POST /commission/estimated | SEAM |
| RMTID.038 | p.68-69 | FR-OP-004 | /operations/invoices, /operations/invoices/:no | GET /ops/invoices, /invoices/{no} | BUILT |
| RMTID.039 | p.69,127-129 | FR-OP-041 | /reports (Report Centre) | /reports (REM-*) | SEAM |
| RMTID.040 | p.69-70 | FR-OP-006 | /operations/invoices/:no | GET /ops/invoices/{no} | SEAM |

## Marketing activities (MKTID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Marketing activities (MKTID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| MKTID.001 | p.70-71 | FR-OP-110 | /remittance/batches/:id | POST /remittance/batches/{id}/send-schedule | BUILT |
| MKTID.002 | p.71 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.003 | p.71-72 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.004 | p.72 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.005 | p.72-73 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.006 | p.73 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.007 | p.73-74 | FR-OP-111 | /remittance/holds, /remittance/holds/:id | /remittance/holds/*; feed COLLECTION_HOLD | SEAM |
| MKTID.008 | p.114 | FR-OP-114 | /adjustment/requests/:id | GET /adjustment/requests/{id}/endorsement-slip | BUILT |
| MKTID.009 | p.74 | FR-OP-112 | /remittance/special, /remittance/special/:id | /remittance/special/* | SEAM |
| MKTID.010 | p.74-75 | FR-OP-113 | /cashiering/cwt | POST /cashiering/cwt; bulk CWT_TAGS; feed COLLECTION_CWT2307 | SUPERSEDED |
| MKTID.011 | p.75 | FR-OP-004 | New Business account (direct payment tag), /operations/invoices/:no | GET /ops/invoices/{no} | BUILT |
| MKTID.012 | p.75-76 | FR-OP-094 | /commission/dp/items | POST /commission/dp/items/{id}/reverse, /reinstate | SUPERSEDED |
| MKTID.013 | p.46 | FR-OP-113 | /cashiering/cwt | POST /cashiering/cwt; bulk CWT_TAGS; feed COLLECTION_CWT2307 | SUPERSEDED |

## Production Reconciliation (PRCID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Production Reconciliation (PRCID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| PRCID.001 | p.79 | FR-OP-070 | /prodrecon/schedules | GET/POST /prodrecon/schedules, PUT /schedules/{id}; job PRODUCTION_EXTRACT | SEAM |
| PRCID.002 | p.79 | FR-OP-072 | /prodrecon/extracts | GET /prodrecon/extracts/{id}/file | SEAM |
| PRCID.003 | p.79 | FR-OP-073 | /prodrecon/extracts | POST /prodrecon/extracts/{id}/send | SEAM |
| PRCID.004 | p.79-80 | FR-OP-072 | /prodrecon/extracts | POST /prodrecon/extracts/{id}/send | SEAM |
| PRCID.005 | p.80 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.006 | p.80 | FR-OP-072 | /prodrecon/extracts | GET /prodrecon/extracts/{id}/file | SEAM |
| PRCID.007 | p.80 | FR-OP-072 | /prodrecon/extracts | POST /prodrecon/extracts/{id}/send | SEAM |
| PRCID.008 | p.81 | FR-OP-073 | /prodrecon/extracts | POST /prodrecon/extracts/{id}/send | SEAM |
| PRCID.009 | p.81 | FR-OP-074 | /prodrecon/uploads | POST/GET /prodrecon/uploads; feed INSURER_PRODUCTION | SEAM |
| PRCID.010 | p.81 | FR-OP-074 | /prodrecon/uploads | POST/GET /prodrecon/uploads; feed INSURER_PRODUCTION | SEAM |
| PRCID.011 | p.81 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.012 | p.82 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.013 | p.82 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.014 | p.82 | FR-OP-077 | /prodrecon/cycles/:id | GET /prodrecon/cycles/{id}/items, POST /items/{id}/pair, /split | BUILT |
| PRCID.015 | p.82 | FR-OP-077 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |
| PRCID.016 | p.82 | FR-OP-077 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |
| PRCID.017 | p.82-83 | FR-OP-080 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |
| PRCID.018 | p.83 | FR-OP-080 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |
| PRCID.019 | p.83 | FR-OP-076 | /prodrecon/unbooked | GET /prodrecon/unbooked | BUILT |
| PRCID.020 | p.83 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.021 | p.83 | FR-OP-077 | /prodrecon/cycles/:id | GET /prodrecon/cycles/{id}/items, POST /items/{id}/pair, /split | BUILT |
| PRCID.022 | p.83-84 | FR-OP-074 | /prodrecon/uploads | POST/GET /prodrecon/uploads; feed INSURER_PRODUCTION | SEAM |
| PRCID.023 | p.84 | FR-OP-076 | /prodrecon/unbooked | GET /prodrecon/unbooked | BUILT |
| PRCID.024 | p.84 | FR-OP-075 | /prodrecon/cycles/:id | POST /prodrecon/cycles/{id}/automatch; job RECON_AUTOMATCH | SEAM |
| PRCID.025 | p.84 | FR-OP-075 | /prodrecon/cycles/:id | POST /prodrecon/cycles/{id}/automatch; job RECON_AUTOMATCH | SEAM |
| PRCID.026 | p.84 | FR-OP-075 | /prodrecon/cycles/:id | POST /prodrecon/cycles/{id}/automatch; job RECON_AUTOMATCH | SEAM |
| PRCID.027 | p.84-85 | FR-OP-075 | /prodrecon/cycles/:id | POST /prodrecon/cycles/{id}/automatch; job RECON_AUTOMATCH | SEAM |
| PRCID.028 | p.85 | FR-OP-078 | /prodrecon/cycles/:id (Early Incentive) | GET /prodrecon/cycles/{id}/early-incentive | SEAM |
| PRCID.029 | p.85 | FR-OP-079 | /prodrecon/cycles/:id (History) | GET /prodrecon/cycles/{id} | BUILT |
| PRCID.030 | p.85 | FR-OP-075 | /prodrecon/cycles/:id | POST /prodrecon/cycles/{id}/automatch; job RECON_AUTOMATCH | SEAM |
| PRCID.031 | p.85-86 | FR-OP-074 | /prodrecon/uploads | POST/GET /prodrecon/uploads; feed INSURER_PRODUCTION | SEAM |
| PRCID.032 | p.86 | FR-OP-074 | /prodrecon/uploads | POST/GET /prodrecon/uploads; feed INSURER_PRODUCTION | SEAM |
| PRCID.033 | p.86 | FR-OP-076 | /prodrecon/unbooked | GET /prodrecon/unbooked | BUILT |
| PRCID.034 | p.86 | FR-OP-071 | /prodrecon/extracts | POST/GET /prodrecon/extracts, GET /extracts/{id}/lines | BUILT |
| PRCID.035 | p.86 | FR-OP-080 | /reports (Report Centre) | /reports (PRC-*) | BUILT |
| PRCID.036 | p.86-87 | FR-OP-080 | /reports (Report Centre) | /reports (PRC-*) | BUILT |
| PRCID.037 | p.87 | FR-OP-080 | /reports (Report Centre) | /reports (PRC-*) | BUILT |
| PRCID.038 | p.87 | FR-OP-080 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |
| PRCID.039 | p.87 | FR-OP-080 | /prodrecon/cycles/:id | PUT /prodrecon/items/{id}/feedback, POST /items/feedback | SEAM |

## Adjustment / Cancellation (ADJID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Adjustment / Cancellation (ADJID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| ADJID.001 | p.89 | FR-OP-050 | /adjustment/new, /adjustment | POST /adjustment/requests/preview, /requests | BUILT |
| ADJID.002 | p.89-90 | FR-OP-050 | /adjustment/new, /adjustment | POST /adjustment/requests/preview, /requests | BUILT |
| ADJID.003 | p.90 | FR-OP-050 | /adjustment/requests/:id | POST /adjustment/requests/{id}/post | SEAM |
| ADJID.004 | p.90-91 | FR-OP-050 | /adjustment/new, /adjustment | POST /adjustment/requests/preview, /requests | BUILT |
| ADJID.005 | p.91-92 | FR-OP-053 | /adjustment/requests/:id, /adjustment/batches | POST /adjustment/batches/return | BUILT |
| ADJID.006 | p.92 | FR-OP-056 | /adjustment/batches, /adjustment/upload | POST /adjustment/batches; bulk ADJ_BATCH | BUILT |
| ADJID.007 | p.92 | FR-OP-053 | /adjustment/requests/:id, /adjustment/batches | POST /adjustment/batches/return | BUILT |
| ADJID.008 | p.92-93 | FR-OP-055 | /adjustment/new | POST /adjustment/requests/{id}/quotation | SEAM |
| ADJID.009 | p.93-94 | FR-OP-057 | /adjustment/requests/:id | POST /adjustment/requests/{id}/post, /reapply | BUILT |
| ADJID.010 | p.94 | FR-OP-053 | /adjustment/requests/:id | POST /adjustment/requests/{id}/approve | BUILT |
| ADJID.011 | p.94 | FR-OP-056 | /adjustment/requests/:id (Accounting) | GET /adjustment/requests/{id}/journal | SEAM |
| ADJID.012 | p.94-95 | FR-OP-057 | /adjustment/requests/:id | POST /adjustment/requests/{id}/post, /reapply | BUILT |
| ADJID.013 | p.95 | FR-OP-057 | /adjustment/requests/:id | POST /adjustment/requests/{id}/post, /reapply | BUILT |
| ADJID.014 | p.95-96; add. p.5 | FR-OP-054 | /adjustment/requests/:id (Recompute) | GET /adjustment/requests/{id}/recompute | SEAM |
| ADJID.015 | p.96 | FR-OP-060 | /adjustment/requests/:id | GET /adjustment/requests/{id}/endorsement-slip | BUILT |
| ADJID.016 | p.96 | FR-OP-062 | /reports (Report Centre) | /reports (ADJ-*); job ADJ_DAILY_REPORT | BUILT |
| ADJID.017 | p.97 | FR-OP-062 | /reports (Report Centre) | /reports (ADJ-*); job ADJ_DAILY_REPORT | BUILT |
| ADJID.018 | p.97-98 | FR-OP-060 | /adjustment/requests/:id | GET /adjustment/requests/{id}/validation-slip | BUILT |
| ADJID.019 | p.98 | FR-OP-062 | /reports (Report Centre) | /reports (ADJ-*); job ADJ_DAILY_REPORT | BUILT |
| ADJID.020 | p.98 | FR-OP-050 | /adjustment, /operations/invoices/:no | GET /adjustment/invoices/{no}/requests, /ops/invoices/{no} | BUILT |
| ADJID.021 | p.98-99 | FR-OP-061 | /reports (Report Centre) | /reports (ADJ-*); job ADJ_DAILY_REPORT | BUILT |
| ADJID.022 | p.99 | FR-OP-061 | /adjustment/requests/:id (History) | GET /adjustment/requests/{id} | BUILT |
| ADJID.023 | p.99 | FR-OP-051 | /adjustment/new | POST /adjustment/requests | BUILT |
| ADJID.024 | p.100 | FR-OP-005, FR-OP-061 | /adjustment, /operations/invoices/:no | GET /adjustment/invoices/{no}/requests, /ops/invoices/{no} | BUILT |
| ADJID.025 | p.100 | FR-OP-052 | /adjustment/requests/:id (Documents) | /attachments | SEAM |
| ADJID.026 | p.101-102 | FR-OP-059 | /adjustment/minimal-balance | bulk MINIMAL_BALANCE_FILE; GET /adjustment/write-offs | SEAM |
| ADJID.027 | p.102-103 | FR-OP-007 | /operations/invoices/:no, /adjustment/requests/:id (Recompute) | GET /ops/invoices/{no}, /adjustment/requests/{id}/recompute | SEAM |
| ADJID.028 | p.103 | FR-OP-058 | /adjustment/new | POST /adjustment/requests/{id}/submit | SEAM |

## Commission Receivables (CMRID)

<!-- table: widths=2.2,1.8,2.9,4.4,4.7,2.6 caption="Traceability: Commission Receivables (CMRID)" status=Status size=7.5 -->
| BRD ID | Page | FR | Screen | API / job | Status |
|---|---|---|---|---|---|
| CMRID.001 | p.106 | FR-OP-090 | /commission/dp/lists | GET/POST /commission/dp/lists, POST /dp/lists/pull; feed COLLECTION_DP_LIST | SUPERSEDED |
| CMRID.002 | p.106-107 | FR-OP-091 | /commission/dp/items | GET /commission/dp/items, POST /dp/items/confirm, /exclude, /{id}/revalidate | BUILT |
| CMRID.003 | p.107 | FR-OP-095 | /commission/incentives/schemes, /commission/incentives/runs | /commission/incentives/* | SEAM |
| CMRID.004 | p.108 | FR-OP-098 | /reports (Report Centre) | /reports (CMR-*) | SEAM |
| CMRID.005 | p.108-109 | FR-OP-095 | /commission/incentives/schemes, /commission/incentives/runs | /commission/incentives/* | SEAM |
| CMRID.006 | p.109 | FR-OP-095 | /commission/incentives/schemes, /commission/incentives/runs | /commission/incentives/* | SEAM |
| CMRID.007 | p.109 | FR-OP-091 | /commission/dp/items | GET /commission/dp/items, POST /dp/items/confirm, /exclude, /{id}/revalidate | BUILT |
| CMRID.008 | p.109-110 | FR-OP-091, FR-OP-093 | /commission/dp/items | GET /commission/dp/items, POST /dp/items/confirm, /exclude, /{id}/revalidate | BUILT |
| CMRID.009 | p.110-111 | FR-OP-092, FR-OP-093 | /commission/dp/billings/:id, /commission/dp/responses | POST /commission/dp/billings/{id}/send, /answers, POST /dp/responses; feed INSURER_DP_R... | SEAM |
| CMRID.010 | p.111 | FR-OP-094, FR-OP-096 | /commission/dp/billings/:id | POST /commission/dp/billings/{id}/collect | BUILT |
| CMRID.011 | p.111 | FR-OP-093 | /commission/dp/billings | GET /commission/dp/billings; job DP_FEEDBACK_SLA | BUILT |
| CMRID.012 | p.111-112 | FR-OP-092 | /commission/dp/billings/:id, /commission/dp/responses | POST /commission/dp/billings/{id}/send, /answers, POST /dp/responses; feed INSURER_DP_R... | SEAM |
| CMRID.013 | p.112 | FR-OP-091 | /commission/dp/items | GET /commission/dp/items, POST /dp/items/confirm, /exclude, /{id}/revalidate | BUILT |
| CMRID.014 | p.112-113 | FR-OP-097 | /commission/estimated, /reports (Report Centre) | /commission/estimated; /reports (CMR-PRODUCTION-YEARLY) | SEAM |
| CMRID.015 | p.113 | FR-OP-096 | /commission/certificates, /commission/certificates/:id | /commission/certificates/* | BUILT |

## Coverage summary

<!-- table: widths=4,2,2,2,2.4,2 caption="BRD-2 requirement IDs covered" -->
| Family | IDs | BUILT | SEAM | SUPERSEDED | Covered |
|---|---|---|---|---|---|
| BRQID | 6 | 4 | 1 | 1 | 6 |
| CSHID | 27 | 8 | 18 | 1 | 27 |
| DBMID | 1 | 0 | 0 | 1 | 1 |
| RMTID | 40 | 18 | 21 | 1 | 40 |
| MKTID | 13 | 3 | 7 | 3 | 13 |
| PRCID | 39 | 15 | 24 | 0 | 39 |
| ADJID | 28 | 20 | 8 | 0 | 28 |
| CMRID | 15 | 7 | 7 | 1 | 15 |
| **Total** | **169** | **75** | **86** | **8** | **169** |

The non-functional requirements of the BRD annex (p.115-120) are traced in section 8.

# Sign-off

By signing, BDOI confirms that this FRS describes the Operations functions it expects in BIBS, and accepts the assumptions in section 10.1 and the differences recorded in section 1.6. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Head, Operations Services", organisation: BDOI}
  - {name: "", role: "Head, Cashiering", organisation: BDOI}
  - {name: "", role: "Head, Financial Transactions and Processing", organisation: BDOI}
  - {name: "", role: "Head, Commission Collection and Adjustments", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Retail and Corporate Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
