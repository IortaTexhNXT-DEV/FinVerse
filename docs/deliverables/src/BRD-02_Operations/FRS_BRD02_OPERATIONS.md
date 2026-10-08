---
# Source of the Functional Requirements Specification for BRD-2 Operations.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-02_Operations/FRS_BRD02_OPERATIONS.md
title: Operations
subtitle: BRD-2 Operations v1.01 (Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation, Commission Receivables) and the Operations Central Addendum 1 annexes of May 2026
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-02
name: Operations
doc_id: BIBS-FRS-BRD-02
version: "2.1"
date: 08 October 2026
status: Issued for BDOI business sign-off
header_title: FRS BRD-2 Operations
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-2 baseline
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Operations Head (pending)
    change: First issue for BDOI review; aligned with the Operations screens and the cross-BRD decisions
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Product Owner (pending)
    change: "BDOI drop plan and integration names of 26-Sep-2026: payment channels OBPCS, Old BOB, PMS and TFS on the payment-file handlers (FR-OP-015); reinsurance transactions through Remittance in Drop 1, reinsurance module in phase 2 (introduction of the Remittance section); Production Reconciliation tested in Drop 2"
  - version: "2.0"
    date: 28 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Business sign-off pack: navigation by persona, 64 screen specifications with screenshots, four walkthroughs, messages, notifications, document outputs, upload templates and bank payment file layouts, cross-BRD contract, sign-off and change control (chapters 12-20); proposed business rules and clarifications for confirmation extended with the screen presentation items (chapter 21); screen standards (appendix). Issued 28-Sep-2026. Chapters 1-11 unchanged in substance; FR, BRD and test IDs kept"
  - version: "2.1"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI business units (sign-off)
    change: "Re-based on the Operations BRD v1.01 (Addendum from workshop, 15-Apr-2026, signed 23 to 28-Apr-2026) and the five Operations Central Addendum 1 annexes of May 2026 (Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation, Collection of Commission Receivables); user-story view and storyboard index added. Every BRD page now cites the file Operations_WS Addendum (BRD v1.01 on pages 71-185, annexes on pages 1-66). Check pick-up (FR-OP-017) and the Cashiering BIR 2307 processing (FR-OP-026) removed from scope; the PR 2307 reversal is owned by Marketing Collection (FR-OP-113) and Disbursement only releases the certificates (FR-OP-121); minimal balances are cleared when the payment is processed (FR-OP-023); Commission Receivables follows system tagging, read-only review, rule-based exclusion, SLA incentives and invoice-level ageing (FR-OP-090 to 099); new IDs BRQID.007, RMTID.041 and MKTID.014-019 traced; clarifications CLR-OP-36 to CLR-OP-62 added"
distribution:
  - {name: "Shellah Marie C. Miranda, AVP, Product Owner", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Jose Melvin M. Jarin, Operations: Financial Transactions and Processing", role: Approver, organisation: BDOI, purpose: Review and sign-off for Operations}
  - {name: "Shirley Catapang and Perjelyn Joy Gutierrez, Operations: Financial Transactions and Processing", role: Business user, organisation: BDOI, purpose: "Review of the Cashiering, Remittance, Adjustment, Production Reconciliation and Commission Receivables screens and rows"}
  - {name: "Cashiering, Remittance, Adjustment, Production Reconciliation and Commission Receivables teams", role: Business user, organisation: BDOI, purpose: Review of the screens of their team}
  - {name: "Pia Grace M. Pinili, Collections and Marketing Support", role: Approver, organisation: BDOI, purpose: "Review and sign-off of the Marketing Collection items, including the PR 2307 reversal and the direct payment tagging (approver of BRD v1.01 and of the Commission Receivables annex)"}
  - {name: "Angel Lou R. Kabigting, Marketing Head Office, Admin and Collections; BBG", role: Business user, organisation: BDOI, purpose: "Review of the Marketing Collection items (reviewer of BRD v1.01)"}
  - {name: "Retail, Corporate and Commercial Marketing (Roderick Lim, Head - Corporate and Retail Marketing)", role: Business user, organisation: BDOI, purpose: Review of the Marketing Collection items}
  - {name: "Comptrollership (Rodrigo R. Dela Cruz)", role: Business user, organisation: BDOI, purpose: "Review of accounting events, the PR 2307 entries, BIR certificates and reports (reviewer of BRD v1.01)"}
  - {name: "Cashiering (Ronna Marie Go, Aimee Lynne Medina, Carolyn Pasia), Production Reconciliation and Adjustment (Cristina Moyon, Joy Ebuenga), Commission Collection (Michael Angelo A. Navarro)", role: Business user, organisation: BDOI, purpose: "Review of the screens of their team (reviewers of the May 2026 annexes)"}
  - {name: Information Technology Group, role: Reviewer, organisation: BDOI, purpose: "Review of interfaces, jobs, access and the Technical Specification"}
  - {name: "Dan Ace Cauton, Program Manager, and Zean C. Ibay, Business Analyst, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Operations business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and prepare user acceptance testing (UAT). The FRs describe the proposed behaviour of the Operations screens. Where the proposed rule or screen differs from the BRD text, or needs a decision of BDOI, the FR says so in a note and chapter 21 lists each such point for confirmation. Every FR cites the BRD requirement it meets and the BRD page. Chapters 12 to 19 give the business view of the system: the navigation, the specification of every screen with its screenshots, the walkthroughs, the messages, notifications, documents, uploads and the contract with the other BRDs; chapter 20 describes the sign-off. Chapter 22 restates every BRD requirement as a user story with the FRs, acceptance criteria and test cases that meet it, and chapter 23 indexes the walkthrough steps as a storyboard.

**Version 2.1.** This version re-bases the FRS on the Operations BRD v1.01 of 15 April 2026 and on the five topic annexes signed in May 2026 (references R1 and R2). FRs whose requirement was removed from the BRD keep their number and are marked "Removed from scope"; FRs changed by the new BRD say so in a note headed "Changed in v2.1".

## Scope

Operations takes over every invoice booked in New Business (BRD-1) and follows its money until the insurer is paid, the insurer's official receipt is recorded and every difference is closed. The FRS is organised by team.

<!-- table: widths=4.2,9.4,3.6 caption="Scope of this FRS" -->
| Area | In scope | BRD IDs |
|---|---|---|
| Access and invoice ledger | Log-in, role-based access, Operations home, the invoice ledger and the payment history of each invoice, Invoice 360, invoice locks, co-insurance shares, batch run reports, report access | BRQID.001-003, 006, 007; RMTID.026, 032, 040; ADJID.027; CSHID.017, 018 |
| Cashiering | AR and OR (create, cancel, reinstate), receipt series, payment files, PDC warehouse, matching and application by component, pre-booked payments, AR Insurance, commission ORs, unapplied dispositions, minimal balances cleared when the payment is processed, search, batch printing, reports | CSHID.001-008, 010-025 |
| Remittance | Extraction (scheduled, manual and ad hoc, with duplicate prevention), eligibility rules, batches, exclusion, approval, schedule (draft before submission) and payment request, insurer OR upload, early remittance incentive, special remittance processing, tracking, notifications, reports | RMTID.001-036, 039-041 |
| Adjustment / Cancellation | Financial, non-financial and internal endorsement requests; validation, approval, return to Marketing, recompute per insurer, posting, excess and AR Insurer, slips, minimal balance file, justification and over-adjustment controls, reports | ADJID.001, 003, 005, 006, 008, 009, 011-014, 016-024, 026-028 |
| Production Reconciliation | Register extraction and sending, insurer feedback upload, matching with a peso-equivalent tolerance, buckets, annotations and disposition, unbooked accounts, estimated items, check of the early incentive applied by the insurer, reports | PRCID.001-027, 029-039; RMTID.037 |
| Commission Receivables / Direct Payment | DP entries from system tagging, read-only review, rule-based exclusion, eligibility from the fully paid status, billing and Statement of Account outputs per insurer, insurer answers and account status, invoice-level ageing, collection and PR reversal, SLA-based incentive programmes, BIR certificates, yearly production, reports | CMRID.001-015 |
| Marketing Collection items | Send schedule, hold requests, special remittance requests, PR 2307 reversal (tagging, validation, report and routing to Disbursement), DP tagging, endorsement types, returns and slips, extension of cover, supporting documents, DP PR reversal | MKTID.001-019 |
| Disbursement queue | Payment requests from Operations, DV number and status; receipt and release of the BIR 2307 certificates sorted per insurer | DBMID.001; RMTID.034 |
| Interfaces and flow-in | Ports to Collection, Disbursement, Marketing, Claims, insurers and shared drive; flow-in feeds with runs, records and alerts | BRQID.004, 005 |

**Removed from scope by the new BRD version:** check pick-up (CSHID.009, Cashiering annex p.4-6; FR-OP-017) and the Cashiering processing and reversal of BIR 2307 (CSHID.026 and CSHID.027, deleted in BRD v1.01, e-mail record p.67-69 and p.186-188; FR-OP-026). **Moved:** RMTID.038 is BRQID.007, PRCID.028 is RMTID.041, ADJID.002, 004, 007, 010, 015 and 025 are MKTID.014 to MKTID.019, and RMTID.037 belongs to Production Reconciliation (annexes p.3, 16, 25, 33-38). The traceability chapter lists every ID with its status.

**Out of scope for this phase:**

- The Collection system, the Disbursement system and the Accounting (GL mapping) specification. BRD-4 Collections and BRD-5 Accounting, Disbursement and Accounting Controls answer these questions; until they are in place Operations uses the uploads and the Disbursement queue described in section 7 (OQ01, OQ02, OQ07).
- Electronic transfers (file transfer, system-to-system interfaces, shared drive folders) to insurers, BDO bank channels and the Marketing and Claims systems. Files are uploaded and sent by e-mail in this phase (OQ17, OQ22, OQ29).
- The payout of incentives to branches. Operations computes and posts them and hands the pass-on to Disbursement (CMRID.006, OQ39).

## References

<!-- table: widths=1.2,11.4,3.6 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Operations BRD: Cashiering, Remittance, Prod Recon, Adjustment / Cancellation, Collection of Commission Receivables (Direct Payment), re-issued as "Addendum from workshop": pages 71-185 of the file Operations_WS Addendum. It includes Addendum 1 of December 2025 (RMTID.002, ADJID.014) and deletes CSHID.026 and CSHID.027 | v1.01, 15-Apr-2026; signed 23 to 28-Apr-2026 (p.184-185) |
| R2 | Operations Central - Addendum 1, five signed topic annexes of the May 2026 workshops: Cashiering (p.1-8), Remittance (p.9-18), Production Reconciliation (p.19-28), Adjustment / Cancellation (p.29-40, with the justification annex of ADJID.023 on p.40) and Collection of Commission Receivables (p.41-66) | v1.0 / 1.01, 4 to 12-May-2026; signed 8 to 21-May-2026 |
| R2a | E-mail record of the sign-off of the PR 2307 changes (BDOI Operations, Product Owner, BA and ITG, 13-Apr to 4-May-2026), pages 67-70 and 186-189 of the same file | 4-May-2026 |
| R2b | Superseded baseline kept in the same file for reference: Addendum 1 of December 2025 and the Operations BRD v1.0 of July 2025, pages 190-321, including the scanned annex lists of reports and of the endorsement slip (pages 310-321) | v1.0, Jul-2025 and 18-Dec-2025 |
| R3 | BDOI Operations (BRD-2) requirements baseline, open questions OQ01-OQ50 | current |
| R7 | Cross-BRD decisions and answered questions | current |
| R8 | FRS BRD-1 New Business and FRS BRD-3 Product Maintenance (booking, catalogue, shared platform) | v2.0 |
| R9 | FRS BRD-4 Collections (direct payment and PR 2307 dispositions, collector worklists) | v2.0 |

Page references in this document ("p.107") are pages of the file Operations_WS Addendum (321 pages). Pages 71-185 hold the BRD v1.01 (R1) and are the main citation; "annex p.n" cites the May 2026 annexes (R2) on pages 1-66, which govern where they change a requirement. The report and endorsement slip lists of the BRD annex were not re-issued with v1.01; they are quoted from the scanned pages 310-321 of the original BRD (R2b). The approval sheet of R1 (p.184-185) names the signatories of this set; on the overall approval page of the annexes (p.66) Jose Melvin Jarin is marked on leave and Roderick Lim has not signed (CLR-OP-36).

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

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority** and the **screens** where the user performs it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by the System Administrator or the business owner in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check (for example a blank mandatory field); its message follows the same wording but has no business code. Text in angle brackets (`<invoice>`) is replaced by the value.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-2 test plan.


> [!NOTE]
> Values marked "default" (holding days, tolerances, SLA hours, list entries) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a change to the system.



# Business context and process overview

## Business context

BDOI collects premium from clients on behalf of insurers and remits it net of its commission. It also earns commission, service fees, profit share and incentives. Today Operations issues receipts by hand, extracts remittances manually, matches insurer reports in spreadsheets, computes endorsements manually and gathers commission receivables by e-mail (p.74). The BRD asks for one system that integrates Cashiering, Remittance, Production Reconciliation, Adjustment / Cancellation and the Collection of Commission Receivables, with scheduled extractions, automatic matching, validated accounting entries, tracking and reports (p.73-76).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.74-76)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Manual issuance of receipts; payment details not visible; fragmented application | ARs and ORs numbered from controlled series; payments matched at acceptance and applied by component; unapplied money kept in a workbench |
| 2 | Manual extraction; no tracking of remittance batches | Scheduled and manual extraction with eligibility checks; batches with approval, Disbursement request and insurer OR |
| 3 | Manual matching of booked accounts with insurer reports | Register sent to the insurer; insurer feedback uploaded and matched within a 1.00 tolerance; differences followed to closure |
| 4 | Redundant steps, manual computation, limited traceability for endorsements | Endorsement requests recomputed per insurer, approved, posted in batches, linked to the original account |
| 5 | Dependency on IT for reports | Reports generated by the users, with view and export rights |
| 6 | Manual gathering, sanitation and billing of commission receivables | DP entries taken from the system tagging and reviewed read-only; only fully paid accounts billed per insurer; answers tracked and commission receivables aged by invoice; commission collected with an OR |

## Process overview

Figure 1 shows the life of a booked invoice by team. Steps 1 to 8 follow the money of a paid invoice. Steps 9 to 12 run beside it: reconciliation of what was booked, changes to the invoice, direct payment accounts and minimal balances. Every step reads and writes the invoice ledger (FR-OP-004).

![Life of a booked invoice by team (BRD p.74-76)](figures/brd02_process_flow.dot)

<!-- table: widths=0.8,3.8,3.2,7.2,2.8 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Invoice booked | New Business | The booked invoice is copied into the invoice ledger with its components, DTIP, commission and flags | BRQID.007 |
| 2 | Payment received | Cashier | Payment by counter, bank file or PDC; an AR is issued from the branch series | CSHID.001, 008 |
| 3 | Match and apply | System | Matched to a booked invoice and applied by component; pre-booked, excess and unmatched money becomes unapplied | CSHID.020, 022 |
| 4 | Unapplied disposition | Cashier, TL | Apply to another invoice, refund, reclass, transfer; approvals and reversals | CSHID.024, 025 |
| 5 | Remittance extraction | System, processor | Paid, cleared and eligible premium grouped into batches per insurer and type | RMTID.001-023 |
| 6 | Batch review and approval | Processor, TL | Exclusions only; submit; four-eyes approval posts the remittance and issues the commission OR | RMTID.002, 009-011 |
| 7 | Disbursement pays | Disbursement | Payment request, DV number; the invoice becomes fully or partially remitted | RMTID.034; DBMID.001 |
| 8 | Insurer OR | Processor | Insurer's OR schedule uploaded; exception report | RMTID.012, 013, 016 |
| 9 | Production reconciliation | Recon handler | Register to the insurer, feedback matched, differences followed | PRCID.001-039 |
| 10 | Adjustment / cancellation | Marketing, Adjustment | Endorsement requests; re-application of payments; AR Insurer after remittance | ADJID.001-028 |
| 11 | Direct payment commission | Commission handler | DP entries from the tagging; fully paid accounts billed to the insurer, aged, collected with an OR, PR reversed | CMRID.001-015; MKTID.012 |
| 12 | Minimal balances | System, Adjustment | Minimal balances cleared when the payment is processed; balances in the file range written off | CSHID.016; ADJID.026 |

## Invoice statuses

The invoice ledger keeps two statuses per invoice and a set of flags. Every Operations screen shows them as status chips.

<!-- table: widths=2.6,6.4,7.6 caption="Payment status, remittance status and flags of an invoice" size=8.5 -->
| Item | Values | Set by |
|---|---|---|
| Payment status | UNPAID; PARTIALLY_PAID; PAID; NOT_APPLICABLE (direct payment and return invoices) | Derived from the premium receivable balances |
| Remittance status | UNPROCESSED; UNAPPLIED_PAYMENT; WITH_OUTSTANDING_BALANCE; REVIEW_IN_PROCESS; REQUESTED_FOR_HOLD; APPROVED; PARTIALLY_REMITTED; FULLY_REMITTED; NOT_APPLICABLE | Derived from the movements, and set by Remittance for the batch and hold states (RMTID.019) |
| Flags | HOLD; PENDING_NEG_ADJ; WRITTEN_OFF; CANCELLED; ESTIMATED | Remittance (hold), Adjustment (pending negative adjustment, cancelled, written off), Cashiering (minimal balance cleared), Production Reconciliation (estimated) |
| Lock | Owner module and reason (for example REMITTANCE while in a batch, ADJUSTMENT while a request is open) | The owner module; released by the owner (RMTID.040) |

# Personas and roles

## Personas

<!-- table: widths=3.4,3.4,8,3 caption="Personas and BIBS roles" size=8.5 -->
| Persona (BRD annex) | BIBS role | Responsibilities in Operations | BRD |
|---|---|---|---|
| Cashier, HO (11) and branches (5) | CASHIER | Receives payments, issues ARs (branch) and ORs (HO), uploads payment files, requests cancellations and reinstatements, assigns dispositions, prints | CSHID.001-008, 010-025 |
| Cashiering TL / TH | CASHIER_TL | Approves cancellations, reinstatements and dispositions; maintains receipt series | CSHID.001-006, 024 |
| Remittance Processor (4) | REMIT_PROCESSOR | Extracts, reviews and excludes, submits batches, uploads insurer ORs; views the payment history of the invoices | RMTID.001-036, 039-041; BRQID.007 |
| Remittance TL / TH | REMIT_TL | Approves batches and special remittances; maintains incentive rules; assigns work | RMTID.009-011; MKTID.009 |
| Production Reconciliation Handler (4) | RECON_HANDLER | Extracts and sends registers, uploads insurer feedback, reconciles, records annotations, flags estimated items | PRCID.001-027, 029-039; RMTID.037 |
| Adjustment Processor (6) | ADJUSTMENT | Validates, returns and posts endorsement requests; uploads batches and minimal balance files | ADJID.001, 003, 005, 006, 008, 009, 011-014, 016-024, 026-028 |
| Adjustment TL | ADJUSTMENT_TL | Approves endorsement requests, including extensions of cover with additional premium | MKTID.017; ADJID.005 |
| Commission Handler / Processor | COMMREC_HANDLER | DP entries from the tagging, read-only review, billing and SOA outputs, answers and account status, ageing, collection; BIR certificate submissions | CMRID.001-015 |
| Commission TL / TH | COMMREC_TL | Posts incentive runs; maintains incentive schemes | CMRID.003, 005, 006 |
| Marketing Collection (HO / branches) | MKT_COLLECTION | Hold and special remittance requests; the PR 2307 reversal from tagging to routing to Disbursement; endorsement requests, corrections of returned requests, slips and supporting documents | MKTID.001-019 |
| Marketing TL / UH | MKT_TL | Approves holds; same requests as Marketing Collection | MKTID.006 |
| Comptrollership | COMPTROLLERSHIP | Acknowledges BIR certificates; views journals and reports | CMRID.015 |
| Disbursement | DISBURSEMENT | Works the Disbursement queue: acknowledge, DV number, paid, return; receives and releases the BIR 2307 certificates | DBMID.001 |
| System Administrator | SYSADMIN | Interfaces (flow-in feeds), parameters and lists (section 9) | BRQID.004, 005 |
| Auditor | AUDITOR | Read access to Operations and reports | CSHID.011 |

The addendum states that user roles and authorisation "will be further defined in succeeding documentations" (Addendum 1 of December 2025, p.193, footnote 2). The roles above are the project's proposal until BDOI confirms the matrix (OQ48).

Segregation of duties is enforced by the system, whatever the role grants: a receipt cancellation, reinstatement or disposition is never approved by its requester; a remittance batch is never approved by its submitter; a hold and a special remittance are never approved by their requester; an endorsement request is never approved by whoever raised, submitted or validated it.

<!-- pagebreak -->

## Permissions

<!-- table: widths=4.8,11.8 caption="Operations permissions" size=8.5 -->
| Permission | Allows |
|---|---|
| OPS_VIEW | Operations home, invoice search and Invoice 360 (read only) |
| OPS_REPORT_VIEW / OPS_REPORT_EXPORT | View Operations reports on screen / download and print them (CSHID.017, 018) |
| CASH_RECEIPT | Receive payments and issue ARs and ORs; Receipts |
| CASH_CANCEL / CASH_REINSTATE | Request the cancellation / reinstatement of a receipt |
| CASH_APPROVE | Approve cancellations and reinstatements; Cashiering Setup |
| CASH_APPLY | Pre-booked payments, re-match, manual application |
| CASH_UPLOAD | Payment files, PDC warehouse, commission ORs |
| CASH_DISPOSITION / CASH_DISPOSITION_APPROVE | Assign and submit dispositions / approve them and their reversals |
| CASH_SERIES_MANAGE | Receipt series master |
| CASH_PRINT | Batch printing of ARs and ORs |
| CWT_TAG / CWT_PROCESS | Marketing 2307 tagging / 2307 validation, report, posting and routing (proposed for Marketing Collection, CLR-OP-40) |
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

The table below is the proposed role-to-permission matrix ("Y" = granted). In v2.1 the 2307 processing right moves from the Cashier to Marketing Collection (BRD v1.01, MKTID.013; CLR-OP-40). Every Operations role also has WORK_VIEW, REPORT_VIEW, ATTACHMENT_VIEW, CLIENT_VIEW and ACCOUNT_VIEW. Handler and team leader roles have BULK_PROCESS and ATTACHMENT_MANAGE; team leaders have WORK_ASSIGN.

<!-- landscape -->

<!-- table: widths=4.6,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1 caption="Role-to-permission matrix for Operations (proposal until OQ48)" size=7.5 -->
| Permission | Cash-ier | Cash TL | Remit | Remit TL | Recon | Adj. | Adj. TL | Comm. Rec. | Comm. TL | Mkt Coll. | Mkt TL | Comp-trol. | Disb. | Sys Admin | Audi-tor |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| OPS_VIEW, OPS_REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| OPS_REPORT_EXPORT | Y | Y | Y | Y | Y | Y | Y | Y | Y | | | | | | |
| CASH_RECEIPT, CASH_CANCEL, CASH_REINSTATE, CASH_APPLY, CASH_UPLOAD, CASH_DISPOSITION, CASH_PRINT | Y | Y | | | | | | | | | | | | | |
| CASH_APPROVE, CASH_DISPOSITION_APPROVE, CASH_SERIES_MANAGE | | Y | | | | | | | | | | | | | |
| CWT_TAG, CWT_PROCESS (proposed, CLR-OP-40), HOLD_REQUEST, SPECIAL_REMIT_REQUEST, ADJ_REQUEST | | | | | | | | | | Y | Y | | | | |
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

The Operations foundation serves every team: access, the Operations home, the invoice ledger and its views, locks, co-insurance shares, batch run reports and report access.

```fr
id: FR-OP-001
title: Log in to BIBS
brd: [BRQID.001 (p.79)]
actor: Operations users
priority: Must have
screens: Login
description: Operations users log in with their BIBS user ID and password. Operations uses the BIBS log-in of BRD-1; it adds no screen of its own.
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
  - [User ID or password wrong, Invalid user name or password, AUTHENTICATION_FAILED]
  - [Account locked, Invalid user name or password, AUTHENTICATION_FAILED]
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
brd: [BRQID.002 (p.79)]
actor: System
priority: Must have
screens: All Operations screens
description:
  - Every Operations screen, button and action requires a permission (section 3.2). Menus show only the screens that the user's roles allow; buttons for actions the user may not perform are hidden.
  - The Cashiering, Remittance and Commission Receivables screens are in the Finance group of the menu; Adjustment and Product Reconciliation are in Client & Policy; the Operations home, invoice search, Invoice 360, Disbursement queue, hand-offs, interfaces and report archive are in the Operations group.
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a screen or starts an action.
  - BIBS checks the user's permissions for that screen or action.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link shows "You do not have access to this screen. Contact your administrator if you need it.". A direct request is refused and logged.
  - Segregation of duties. An approval refused by a four-eyes rule is refused with its message even when the role has the permission (section 3.3).
rules:
  - [R1, Roles are granted permissions as in section 3.3 until BDOI confirms the matrix (OQ48)., Configurable, Role-permission change request (BRD-11)]
  - [R2, "Four-eyes rules (a requester never approves) cannot be switched off.", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
notifications:
  - "None."
audit:
  - Refused requests are logged with the user and time.
acceptance:
  - A Remittance Processor sees the Remittance screens and not the Cashiering Setup or the Disbursement queue.
  - A direct approval request by a user without the approval permission is refused and logged.
  - A Marketing Collection user sees Remittance Holds, Special Remittance and BIR 2307 tagging but cannot approve a hold.
```

```fr
id: FR-OP-003
title: Present the Operations home by role
brd: [BRQID.003 (p.80)]
actor: Operations users
priority: Must have
screens: Operations Home; Cashiering, Remittance, Adjustment, Reconciliation and Commission workbenches
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
  - [R2, "External application links are list values holding the name and the web address of the application.", Configurable, LOV OPS_EXTERNAL_LINK]
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
> BRQID.003 AC3 asks that users can customise or filter the sections. BIBS filters the sections by role; a personal pin or hide of sections is not proposed (chapter 21, CLR-OP-01).

```fr
id: FR-OP-004
title: Keep the invoice ledger of every booked invoice
brd: [BRQID.007 (annex p.3), MKTID.011 (p.138; annex p.63)]
actor: System
priority: Must have
screens: Invoice Search; Invoice 360 (Movements); Interfaces (replay)
description:
  - Each invoice booked in New Business, each endorsement invoice and each return invoice is copied into the Operations invoice ledger after the booking commits. The ledger line holds the ARN, invoice, policy, PN numbers, client, assured and payor, lead insurer and insurer shares, currency, booking, inception and expiry dates, risk code, product line, segment, AO and unit, the direct payment and 2% CWT flags, and the amounts per component.
  - "Components: premium receivable (PR) by BASIC, DST, PREMIUM_TAX_VAT, LGT, FST and OTHER; DTIP; COMMISSION; COMMISSION_VAT; WTAX; PR2307. The balance of a component is booked + adjusted - applied + reversed - remitted - written off."
  - Every Operations movement on the invoice is recorded as a ledger movement (BOOKED, APPLIED, UNAPPLIED, REMITTED, ADJUSTED, WRITE_OFF, DP_REVERSAL, CWT_RECLASS, MIN_BAL) with the module, source reference, AR / OR / batch number, value date and journal batch. The movement list is the payment and remittance history of the invoice.
  - "Payment history at invoice level (BRQID.007, an Operations-wide capability since the May 2026 annex): every Operations team sees, on Invoice 360 and from its own screens, the payment applications of the invoice with the payment date, the amount applied and the AR number, updated as soon as a payment is applied. The history is view-only: payments are applied and corrected in Cashiering only, never from the Remittance or other team screens."
  - The direct payment tag set in New Business at quotation or policy (BRNB.114) is copied to the ledger line, so Cashiering, Remittance and Commission Receivables treat the invoice as direct payment (MKTID.011).
preconditions:
  - The invoice is booked in New Business.
main_flow:
  - New Business books an invoice.
  - After the booking commits, BIBS copies it into the ledger with its components and BOOKED movements, payment status UNPAID and remittance status WITH_OUTSTANDING_BALANCE.
  - Each later Operations action posts its movement and updates the balances and statuses together with it.
alternate_flows:
  - The copy fails. The booking is kept; the failure is a failed record of the feed Booked invoices from booking and raises the alert Interface run failed. An administrator replays the invoice, account or company from Interfaces (Replay Booked Invoices).
  - A direct payment invoice carries its PR and DTIP for information with payment and remittance status NOT_APPLICABLE; booking posts no PR for it.
rules:
  - [R1, "BOOKED movements come only from the booking feed.", Fixed, "-"]
  - [R2, "A movement is unique on module, source reference, invoice, component and type; posting the same source twice has no effect.", Fixed, "-"]
  - [R3, "Payment status is derived from the PR balances (UNPAID, PARTIALLY_PAID, PAID).", Fixed, "-"]
  - [R4, "Keys (ARN, invoice, client, insurer) are stored as values, so the ledger can be replayed from booking at any time.", Fixed, "-"]
  - [R5, "The payment history is view-only outside Cashiering (BRQID.007); Remittance and the other teams cannot add or change a payment application.", Fixed, "-"]
validations:
  - [Movement without an amount, A movement needs at least one amount other than zero, MOVEMENT_WITHOUT_AMOUNT]
  - [BOOKED movement from a module, BOOKED movements come from the booking feed only, MOVEMENT_TYPE_RESERVED]
  - [Replay without scope, "Give an invoice number, an ARN or a company to replay", REPLAY_SCOPE]
notifications:
  - Alert Interface run failed (NT-05) to the System Administrator when a copy fails.
audit:
  - Movements and status changes are only added, never changed; each status change keeps the module and time.
acceptance:
  - A booked invoice appears in Invoice Search with its components equal to the booking, status UNPAID and WITH_OUTSTANDING_BALANCE.
  - A payment applied to the invoice shows as an APPLIED movement with the AR number, date and amount per component.
  - A Remittance Processor opening Invoice 360 of a paid invoice sees the payment history (payment date, amount applied, AR number) and has no action to change a payment application.
  - A direct payment account booked in New Business shows the DP chip and payment status NOT_APPLICABLE.
  - Replaying an invoice already in the ledger changes nothing.
```

```fr
id: FR-OP-005
title: View an invoice in Invoice 360
brd: [RMTID.026 (p.126), RMTID.032 (p.129), ADJID.024 (p.163)]
actor: All Operations users (OPS_VIEW)
priority: Must have
screens: Invoice Search; Invoice 360
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
brd: [RMTID.040 (p.132-133)]
actor: System; Remittance, Adjustment
priority: Must have
screens: Invoice 360 (lock pill); Remittance Batch; Adjustment request page
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
brd: [ADJID.027 (p.165-166)]
actor: System
priority: Must have
screens: Invoice 360 (header, shares); Adjustment request page (Recompute)
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
  - The change per insurer is kept on the request.
acceptance:
  - A 60/40 co-insured invoice shows both insurers on Invoice 360.
  - A premium decrease of 1,000.00 on it is split 600.00 / 400.00 on the request's insurer breakdown.
```

> [!NOTE] Open point
> Whether remittance is extracted and scheduled per insurer share for co-insured invoices is open (OQ50). Remittance batches today are per lead insurer.

```fr
id: FR-OP-008
title: Continue batch jobs past failed records and report the run
brd: [BRQID.006 (p.82-83)]
actor: System; users who upload files
priority: Must have
screens: Bulk Upload wizard (run summary); Payment Uploads; Interfaces (runs, records); Report CSH-BATCH-RUN
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
  - Alert Interface run failed (NT-05) when a feed run has failed records.
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
brd: [CSHID.017 (p.101), CSHID.018 (p.101)]
actor: Operations users
priority: Must have
screens: Reports (Report Centre); Report Archive
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

Cashiering receives premium and non-premium payments, issues ARs and Head Office ORs, applies payments to the invoice ledger by component, keeps unapplied payments with their dispositions and runs the BIR 2307 flow.

```fr
id: FR-OP-010
title: Maintain AR and OR series per branch
brd: [CSHID.006 (p.91-92), CSHID.015 (p.100)]
actor: Cashiering TL / TH (maintain); System (numbering)
priority: Must have
screens: Receipt Series
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
brd: [CSHID.001 (p.86-87)]
actor: Cashier
priority: Must have
screens: Receive Payment; Receipts; Receipt page
description:
  - The cashier records a premium payment (Bills Payment, OTC, Trade, CLPC, PDC, Direct Credit) or a non-premium payment (refund, other expenses, AR Insurance). BIBS issues an AR numbered from the branch series, in the currency received, converted at the BOOK rate.
  - "The Receive Payment screen has two panes: on the left the payor and the ARN, invoice, policy or PN look-up and the tender; on the right a live preview of the match and of the application by component (DST, VAT, LGT, FST, other, basic), the 98% badge for 2% CWT clients, the excess that will stay unapplied and the BOOK rate."
  - Each payment is matched and applied at once (FR-OP-018, FR-OP-019). Uploaded files and matured PDCs issue their ARs the same way (FR-OP-015, FR-OP-016).
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
brd: [CSHID.002 (p.87-88)]
actor: Cashier (Head Office)
priority: Must have
screens: Receipts (Issue Official Receipt); Receipt page
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
brd: [CSHID.001 (p.86-87), CSHID.002 (p.87-88), CSHID.003 (p.88-89), CSHID.012 (p.98)]
actor: Cashier (request); Cashiering TL / TH (approve)
priority: Must have
screens: Receipt page (Cancel); Receipts (Cancellations and Reinstatements tab)
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
brd: [CSHID.001 (p.86-87), CSHID.004 (p.89-90), CSHID.005 (p.90-91), CSHID.013 (p.98-99)]
actor: Cashier (request); Cashiering TL / TH (approve)
priority: "Must have (CSHID.013: not stated in the BRD)"
screens: Receipt page (Reinstate)
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
brd: [CSHID.008 (p.92-95)]
actor: Cashier
priority: Must have
screens: Payment Uploads (Bulk Upload wizard, run summary); Cashiering Setup (layouts)
description:
  - "The cashier uploads the payment files of the channels: Bills Payment (TXT FS01 from IT-DCO), Trade (TXT from CIB), CLPC (Excel), Direct Credit (TXT FS01/04) and the PDC list (Excel). Each row becomes a payment with its AR, is matched and applied (FR-OP-018), and the run summary shows applied, unapplied, pre-booked, excess and failed rows (FR-OP-008)."
  - "BDOI named the channel systems on 26-Sep-2026 (Drop 0 integrations): Bills Payment is the OBPCS file (Online Bills Payment Consolidation System); Direct Credit carries the Old BOB funds-transfer collections of statements of account (or a layout of its own); the PDC list comes from PMS (PDC Management System); Trade payments come from TFS (Trade Finance System) by manual upload. The purpose of AFTS (Automatic Fund Transfer System) is not yet explained (IQ10). Outgoing payments through CMS / New BOB belong to Disbursement (BRD-5)."
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
  - Automatic matching again. The automatic re-matching run (hourly) re-matches unapplied payments of the files (FR-OP-018).
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
brd: [CSHID.008 (p.92-95)]
actor: Cashier; System (maturity job)
priority: Must have
screens: PDC Warehouse
description:
  - Post-dated checks are warehoused by screen or by the PDC list upload, each with a system number PDCW-yyyy-n, the client, invoice or ARN, check number, bank and branch, maturity date, amount and market segment. the daily PDC maturity run (daily 08:30 PHT) turns each matured check into a payment with its AR, matched and applied like any payment.
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
  - [R2, "Maturity runs daily (the daily PDC maturity run).", Configurable, Job schedule]
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

### FR-OP-017 Queue checks for pick-up and print their ARs: removed from scope {-}

> [!NOTE] Removed from scope by the Cashiering annex (CSHID.009, annex p.4-6)
> The Cashiering annex of May 2026 (MOM 05052026, signed 8-May-2026) removes CSHID.009 "Facility to accept check for check pick up from Collection": the process is no longer part of BDOI's target operating model. FR-OP-017 keeps its number and is withdrawn: the Check Pick-up screen, the pick-up requests from Collections, the Check Pick-ups Due tile, the pick-up report and the test conditions TC-OP-017.1 and TC-OP-017.2 are no longer part of this set. Checks collected by Marketing Collection are received at the counter or through the payment files like any other check (FR-OP-011, FR-OP-015). The hand-off from BRD-4 Collections to Cashiering is withdrawn as well (CLR-OP-38).

```fr
id: FR-OP-018
title: Match payments at acceptance and handle pre-booked payments
brd: [CSHID.020 (p.103-104)]
actor: System; Cashier (pre-booked queue)
priority: Must have
screens: Receive Payment (preview); Pre-booked Payments; Payment Uploads (run summary)
description:
  - "Every payment except a warehoused PDC is matched at acceptance on the ARN, invoice, policy or PN number. The outcome is one category: APPLIED (booked invoice with outstanding PR; applied up to 100%, or 98% for 2% CWT clients); PREBOOKED (account found but not booked yet); UNAPPLIED_NO_MATCH (no account found); EXCESS (payment above the outstanding amount; the excess stays unapplied); CANCELLED_REFERENCE (invoice or slip cancelled)."
  - A pre-booked payment issues its AR and waits as an unapplied item of origin PREBOOKED. It is applied when the invoice is booked (ledger event) or by the pre-booked re-matching run (every 2 hours); processed items leave the pre-booked queue. Items that wait too long raise PREBOOKED_AGEING.
  - A pre-booked payment also opens the New Business payment gate of the account (evidence CASHIERING PRE:<id>), so placement does not wait for booking (FR-OP-132).
preconditions:
  - "None (automatic)."
main_flow:
  - A payment is received (counter, file or PDC).
  - BIBS looks up the references and decides the category.
  - BIBS applies, queues as pre-booked, or creates the unapplied item.
alternate_flows:
  - The cashier runs **Re-match Now** on a pre-booked item, or **Release** to move it to the Unapplied Payments workbench.
  - The automatic re-matching run (hourly) matches again unapplied NO_MATCH and PREBOOKED items with their references.
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
brd: [CSHID.022 (p.105)]
actor: System
priority: Must have
screens: Receive Payment (preview); Receipt page (Applications, Journal)
description:
  - "BIBS applies each payment to the premium receivable of the invoice component by component in the order: DST, premium tax / VAT, LGT, FST, other charges, basic premium. It never applies to DTIP. Each application posts OPS_PAYMENT_APPLY (Dr unapplied collections / Cr PR by component) and, when commission is realised on collection, the realised commission and VAT; it records an APPLIED movement APP:<id> on the ledger."
preconditions:
  - The payment matched a booked invoice with outstanding PR.
main_flow:
  - BIBS takes the outstanding balance of each component in hierarchy order.
  - BIBS applies the payment until it is used or the PR is cleared.
  - BIBS posts the application and updates the invoice balances and payment status.
alternate_flows:
  - The payment exceeds the PR. The excess becomes an unapplied item of origin EXCESS, unless it is within the minimal balance threshold (FR-OP-023).
  - The payment leaves a premium balance within the minimal balance threshold. BIBS clears it in the same posting (FR-OP-023).
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
brd: [CSHID.021 (p.104)]
actor: Cashier
priority: "Must have (not stated for this ID in the BRD)"
screens: Receive Payment (AR class AR Insurance); Receipts
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
brd: [CSHID.007 (p.92)]
actor: Cashier; System
priority: Must have
screens: Commission ORs; Interfaces (feed COLLECTION_COMMISSION_PAYMENT)
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
brd: [CSHID.024 (p.105-106), CSHID.025 (p.106-107)]
actor: Cashier (assign, submit); Cashiering TL (approve)
priority: Must have
screens: Unapplied Payments (tabs Unapplied, Monitoring, For Approval, For Reversal, Done); Unapplied Payment page
description:
  - Every unapplied balance is an item UNP-yyyy-n with its origin (no match, excess, cancelled reference, adjustment, cancellation, DP reinstatement, remittance return, re-application, pre-booked, other), amount, balance, currency, client and unit. The workbench tabs follow the stage of workflow OPS_DISPOSITION.
  - "The cashier assigns a disposition type from DISPOSITION_TYPE: Apply to other invoice; DST payment application; Refund; Reclass; Transfer to other marketing unit; Others. Each type has an action (APPLY, DST_APPLY, REFUND, RECLASS, TRANSFER, MANUAL) and says whether it needs approval."
  - "Effects: APPLY applies the amount through the hierarchy to another booked invoice; DST_APPLY applies to its DST only; REFUND sends a request to Disbursement and posts OPS_UNAPPLIED_REFUND when paid; RECLASS and TRANSFER post OPS_UNAPPLIED_RECLASS for the whole balance to another client or unit; MANUAL releases the balance settled outside BIBS."
  - A completed disposition can be marked for reversal with a reason and is reversed on approval; a refund is reversed only after Disbursement returned it. A balance left after a partial disposition goes back to the Unapplied tab.
  - Minimal excess amounts within the minimal balance threshold never become unapplied items; they are cleared when the payment is processed (FR-OP-023, CSHID.016).
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
title: Clear minimal balances when the payment is processed
brd: [CSHID.016 (p.100-101; annex p.4 and 6-7)]
actor: System; Cashiering TL (thresholds, safety-net run)
priority: Must have
screens: Receive Payment (preview); Receipt page (Applications, Journal); Cashiering Setup (Minimal Balance); Reports CSH-MINBAL-PREMIUM, CSH-MINBAL-EXCESS
description:
  - "Minimal balances are cleared by the system in the same transaction that processes the payment (acceptance at the counter, a payment file, a matured PDC, a re-application or a disposition that applies money), with no user action, approval or tagging (Cashiering annex, CSHID.016). When the application leaves a premium receivable balance within the minimal balance threshold, BIBS reverses it (OPS_MINIMAL_BALANCE_REVERSAL, ledger MIN_BAL), unless the balance equals the client's 2% creditable withholding tax, the DST charged or the entire premium. When the payment exceeds the receivable by an amount within the threshold, BIBS moves the excess to AP overages (OPS_EXCESS_TO_OVERAGES) instead of creating an unapplied item."
  - "Minimal balances therefore never appear in the Unapplied, Monitoring or For Approval tabs of Unapplied Payments and are never routed to a disposition. An amount above the threshold follows the normal excess and unapplied handling (FR-OP-019, FR-OP-022)."
  - "The thresholds are configurable (default PHP 10.00 for premium and for excess, OQ11), never fixed in the system. The daily run of the minimal balance rules (04:00 PHT, or **Run Sweep Now**) stays as a safety net: it clears only a balance that a later event left within the threshold (for example a decrease posted by Adjustment) and finds nothing to do after a normal payment."
  - Each invoice component is cleared once; an invoice already written off by the minimal balance file (FR-OP-059) is skipped, so no balance is reversed twice.
preconditions:
  - A payment is being processed, or the safety-net run starts.
main_flow:
  - BIBS applies the payment by component (FR-OP-019).
  - BIBS compares the remaining premium receivable and any excess with the thresholds and checks the three exceptions.
  - BIBS reverses the minimal premium balance or moves the minimal excess to AP overages, in the same posting as the payment.
  - The receipt shows the clearing line in its Applications and Journal tabs; the reports list it.
alternate_flows:
  - The remaining balance equals the 2% CWT, the DST charged or the whole premium. BIBS keeps it outstanding.
  - The balance or excess is above the threshold. BIBS keeps the balance outstanding or creates the unapplied item of origin EXCESS.
  - Safety-net run. BIBS clears the balances left within the threshold by other events and logs the run once.
rules:
  - [R1, "Premium threshold 10.00; reverse, except a balance equal to the 2% CWT, the DST charged or the entire premium.", Configurable, Minimal balance rules; parameter MIN_BALANCE_AUTO_MAX]
  - [R2, "Excess threshold 10.00; moved to AP overages at processing, never to Unapplied Payments.", Configurable, Minimal balance rules]
  - [R3, "COMMISSION rule inactive until BDOI confirms it (OQ11).", Configurable, Minimal balance rules]
  - [R4, "A component is cleared once; no second automatic reversal of the same transaction.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - Each clearing is recorded once with the receipt, the rule and threshold applied, the amount, the time and the system as actor, and as a ledger movement.
acceptance:
  - A payment of 9,995.00 on an invoice of 10,000.00 clears the remaining 5.00 in the same posting; no unapplied item and no disposition are created.
  - A payment of 10,008.00 on an invoice of 10,000.00 moves the excess of 8.00 to AP overages; the Unapplied tab shows nothing for it.
  - A payment of 10,050.00 on an invoice of 10,000.00 creates an unapplied item of 50.00 (above the threshold).
  - A remaining balance of 5.00 that equals the DST charged is not reversed.
  - Running the safety-net sweep after these payments changes nothing.
```

> [!NOTE] Changed in v2.1
> The Cashiering annex of May 2026 rewrites CSHID.016: minimal balances and minimal excess payments are cleared automatically when the transaction is processed and must never appear in the disposition or unapplied tabs ("Minimal balances appear in disposition or unapplied payment tabs" is a negative scenario). Version 2.0 cleared them in a daily sweep; the sweep is kept only as a safety net. The threshold value and the boundary with the minimal balance file of Adjustment (ADJID.026, 10.00 to 100.00) are confirmed through CLR-OP-39 (OQ11).

```fr
id: FR-OP-024
title: Search receipts and keep the receipt audit trail
brd: [CSHID.010 (p.96-97), CSHID.011 (p.97-98)]
actor: Cashier; Auditor
priority: Must have
screens: Receipts (search); Receipt page (History)
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
  - [R3, "Every audit record holds the seven items of the IT audit trail standard: user who started the activity, event type, module, from and to values, origin of the event (IP address, MAC address, terminal ID or equivalent), date and time, success or failure indicator (condition of the BRD v1.01 sign-off, p.67; CLR-OP-37). The same applies to every Operations audit record.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the receipt audit; searches are logged in the search log."
acceptance:
  - A search by partial assured name and date range returns the matching ARs.
  - The History tab of a cancelled AR shows the creation, the cancellation request and approval with users and times.
  - The audit record of a receipt cancellation shows the user, the event type, the module Cashiering, the status before and after, the origin of the event, the date and time and Success; a cancellation refused for lack of permission is recorded with Failure.
```

```fr
id: FR-OP-025
title: Print ARs and ORs in batch
brd: [CSHID.019 (p.102-103)]
actor: Cashier
priority: "Not stated in the BRD"
screens: Batch Print
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

### FR-OP-026 Process BIR 2307 reversals: removed from scope {-}

> [!NOTE] Removed from scope by BRD v1.01 (CSHID.026 and CSHID.027 deleted)
> BRD v1.01 of 15-Apr-2026 deletes CSHID.026 (Cashiering processing of the tagged BIR 2307 data) and CSHID.027 (Cashiering reversal by reference number, transaction report and routing to Disbursement); the sign-off e-mails record "the removal of cashiering from certificate handling" (p.67-69, p.186-188). FR-OP-026 keeps its number and is withdrawn. The tagging, the validation of the amounts, the validation of the batch against the certificates, the BIR 2307 transaction report, its posting and the routing to Disbursement are now done by Marketing Collection under MKTID.013 (FR-OP-113); Disbursement only receives and releases the certificates (DBMID.001, FR-OP-121). The Cashiering steps of v2.0 (CWT copy checklist, Validate and Post, Route to Disbursement and Settle in Cash by the Cashier) and the test conditions TC-OP-026.1 to 026.3 are withdrawn (CLR-OP-40).

```fr
id: FR-OP-027
title: Post the accounting entries of Cashiering
brd: [CSHID.012 (p.98), CSHID.013 (p.98-99), CSHID.014 (p.99)]
actor: System
priority: "Must have (CSHID.013: not stated in the BRD)"
screens: Receipt page (Journal)
description:
  - "Each Cashiering transaction is a business event whose GL rule is configured by Comptrollership: OPS_AR_RECEIPT (AR issued), OPS_AR_INSURANCE_RECEIPT, OPS_OR_ISSUE, OPS_PAYMENT_APPLY, OPS_RECEIPT_REINSTATE, OPS_EXCESS_TO_OVERAGES, OPS_MINIMAL_BALANCE_REVERSAL, OPS_UNAPPLIED_REFUND, OPS_UNAPPLIED_RECLASS. Section 5.3 lists the default entries."
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
brd: [CSHID.023 (p.105; p.314-316)]
actor: Cashier; Cashiering TL; Comptrollership
priority: Must have
screens: Reports (category Operations)
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

Remittance extracts what clients paid, groups it into batches per insurer and type, gets the batches approved, asks Disbursement to pay the insurer and records the insurer's OR.

**Reinsurance transactions (BDOI answer of 26-Sep-2026).** Remittance handles the reinsurance transactions in Drop 1; the reinsurance module is phase 2. A reinsurer or reinsurance broker is set up as a counterparty and its payable follows the extraction, batch, hold, approval and payment steps of this section, with the same accounting. Which reinsurance transactions are in scope and the "Remittance Addendum" named in the drop plan are not yet defined (IQ20); no treaty or cession processing is in Drop 1.

```fr
id: FR-OP-030
title: Extract remittances by schedule, insurer or invoice
brd: [RMTID.001 (p.112; annex p.12), RMTID.003 (p.113-114; annex p.13), RMTID.004 (p.114-115; annex p.14), RMTID.005 (p.115), RMTID.007 (p.116-117), RMTID.008 (p.116)]
actor: System (the scheduled remittance extraction); Remittance Processor
priority: Must have
screens: Extraction; DTIP Status; Remittance Batches
description:
  - "An extraction run REX-yyyy-n examines the invoices of an insurer whose remittance status is UNPROCESSED, WITH_OUTSTANDING_BALANCE or PARTIALLY_REMITTED. It is started by the scheduled job (daily off-peak, 20:00 PHT by default), manually for an insurer and remittance type, or for one invoice number. Each insurer is processed in its own transaction."
  - "Every invoice examined gets a tag: EXTRACTED; UNEXTRACTED_DUE (with its blocking reasons, FR-OP-031); UNEXTRACTED_NOT_DUE; RETURNED (extracted then returned by a user)."
  - "Extracted invoices are grouped into batches RMB-<insurer>-yyyy-n per insurer and remittance type (WITH_INCENTIVES, NORMAL_PHP, NORMAL_USD, SPECIAL), assigned to a processor and opened at stage REVIEW_IN_PROCESS (FR-OP-033). Each invoice in a batch is locked by REMITTANCE. The extract file is stored in the list of extracts, named by REMIT_FILE_PATTERN."
  - A processor searching an invoice during the day can queue it for the end-of-day run (RMTID.005); the evening run extracts the queued invoices once.
  - "Each extracted invoice keeps its extraction source: Scheduled, Manual (single invoice or insurer) or Special (ad hoc / special remittance, FR-OP-040). The source shows with the EXTRACTED tag on the run, the batch line and Invoice 360. An extracted invoice is never extracted again by a later run while it is in a batch, approved or remitted: uniqueness holds across runs, whatever the source (RMTID.003, RMTID.004)."
  - "Instalment payments (RMTID.003, annex p.13): an invoice paid by instalment is extracted for the instalments applied and posted, even while the account is still in process; the meaning of \"still in process\" is confirmed through CLR-OP-44."
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
  - [R3, "Schedule of the job.", Configurable, "the scheduled remittance extraction (schedule set by the System Administrator)"]
  - [R4, "Extract file naming.", Configurable, Parameter REMIT_FILE_PATTERN (OQ17)]
  - [R5, "An invoice extracted by any source (scheduled, manual, special) is not extracted again by a later run; the source is kept with the tag.", Fixed, "-"]
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
  - An invoice taken by a manual (ad hoc) extraction shows the source Manual on Invoice 360, and the next scheduled run does not extract it again.
```

> [!NOTE] Difference from the BRD
> RMTID.005 asks to "trigger extraction at the end of day if payment application is searched" (OQ18). BIBS lets the processor queue the searched invoice for the evening run. RMTID.001 asks to save extracts on a shared drive with a folder per type; BIBS stores them in the list of extracts, from which the user downloads them; a copy to a shared drive is added when BDOI specifies it (OQ17).

```fr
id: FR-OP-031
title: Apply the remittance eligibility rules
brd: [RMTID.006 (p.115), RMTID.014 (p.119-120), RMTID.015 (p.120), RMTID.017 (p.121-122), RMTID.018 (p.122), RMTID.020 (p.123-124), RMTID.022 (p.124-125), RMTID.028 (p.127), RMTID.031 (p.128-129), RMTID.035 (p.130)]
actor: System
priority: Must have
screens: Extraction (run tags); DTIP Status; Report REM-PAIDAR-OVER-DTIP; Report REM-EXCLUDED
description:
  - "Paid AR is the net applied PR of the invoice from applied and posted payments only (cash, check and every channel). The amount to remit is paid AR less the DTIP already remitted."
  - "An invoice is not extracted, and its tag (Due - Not Extracted) lists the reasons in words: On hold (hold flag); Pending financial endorsement (any financial endorsement or cancellation request on the invoice that is pending or unresolved, for example a cancellation, a decrease or an increase in TSI or a change of commission; RMTID.001 as revised by the Remittance annex); Pending negative adjustment (a negative adjustment request is pending); Written off; Check within the holding period (a payment is younger than 3 banking days or not cleared); Paid AR above DTIP (paid AR above the DTIP balance); Locked by another team (another team holds the lock). Cancelled, direct payment and return invoices are skipped."
  - Written-off invoices are excluded from every output but remain visible and marked in Invoice 360 and the tags.
  - "A completed or validated adjustment, including a negative adjustment that was posted, no longer blocks the invoice: it is extracted on its new balances, subject to the other rules."
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
  - [R5, "Any pending or unresolved financial endorsement blocks the extraction; posted adjustments do not.", Fixed, "-"]
  - [R6, "Instalment invoices are extracted for the instalments applied, even while the account is in process (meaning of in process to confirm, CLR-OP-44).", Configurable, "-"]
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
  - An invoice with a pending increase in TSI is not extracted and its tag reads Pending financial endorsement; once the endorsement is posted the next run extracts it.
```

> [!NOTE] Differences from the BRD
> RMTID.014: the expected result caps the remittance at the DTIP balance, while the acceptance criteria exclude the account (OQ19); BIBS excludes by default and caps when the parameter is set to CAP. RMTID.017: the start of the holding period (AR date or deposit date) and the source of "cleared" are open (OQ20); BIBS counts from the last applied value date.

```fr
id: FR-OP-032
title: Review a batch and exclude records (addendum)
brd: [RMTID.002 (p.113), RMTID.024 (p.125-126; annex p.15), RMTID.027 (p.127)]
actor: Remittance Processor
priority: Must have
screens: Remittance Batches (queues by stage); Remittance Batch (totals, lines, exclusions, preview)
description:
  - The Remittance Batches screen lists the batches by stage (queues) with insurer, type, processor, counts and totals; it filters by insurer and type and supports bulk submit and approve.
  - "The extracted accounts are listed with the details of the remittance schedule template (RMTID.024 as revised by the Remittance annex): insurer, policy number, invoice, assured, remittance type, batch number, extraction source and the amounts of the schedule. The settlement number named in the annex is shown when BDOI confirms what it is and where it comes from (CLR-OP-46)."
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
  - [R1, "Exclusion only; amounts cannot be changed.", Fixed, "-"]
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
  - The list of extracted accounts shows for each account the insurer, policy number, invoice, remittance type and batch number of the remittance schedule.
```

> [!NOTE] Addendum
> The original BRD allowed online editing of the extract and pushing it "with or without edit" (p.239). Addendum 1 of December 2025, merged into BRD v1.01 (RMTID.002, p.113), replaces it with exclusion only; BIBS follows it.

```fr
id: FR-OP-033
title: Process a batch - assign, submit, hold and return
brd: [RMTID.009 (p.117-118), RMTID.010 (p.117), RMTID.019 (p.122-123), RMTID.029 (p.127-128)]
actor: Remittance Processor; Remittance TL
priority: Must have
screens: Remittance Batch (workflow panel); Remittance Batches
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
brd: [RMTID.011 (p.118-119; annex p.14-15)]
actor: Remittance Processor
priority: Must have
screens: Remittance Batch (Documents)
description:
  - The remittance schedule (PDF and XLSX) and the payment request (PDF) are generated from the templates REMITTANCE_SCHEDULE and REMITTANCE_PAYMENT_REQUEST and stored on the batch. They show the batch number, insurer, remittance type and the payment details per invoice; the layouts follow Annex III (Normal, Special, With Incentives).
  - "Before submission (stage Review in Process) the processor can save, export or produce the remittance schedule of the batch as it stands; that copy is marked Draft and is not stored as the issued schedule (RMTID.011 as revised by the Remittance annex, CLR-OP-45). Printing the schedule and the payment request is available after submission."
preconditions:
  - Draft schedule - the batch is in review. Issued schedule and payment request - the batch is submitted or approved.
main_flow:
  - The processor opens the Documents tab of the batch.
  - Before submission, the processor saves or exports the draft schedule to check it.
  - After submission, the processor downloads or prints the schedule and the payment request.
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
  - A batch in review produces a schedule marked Draft with the current lines; after submission the schedule is issued without the Draft mark.
```

> [!NOTE] Changed in v2.1
> The Remittance annex of May 2026 (signed 8-May-2026) refines RMTID.001 (only eligible accounts; any pending or unresolved financial endorsement excludes the account, completed adjustments do not: FR-OP-031), RMTID.003 (no reprocessing, uniqueness across runs, instalment invoices included while in process: FR-OP-030, CLR-OP-44), RMTID.004 (ad hoc extractions tagged and excluded from later runs: FR-OP-030), RMTID.011 (save or export the schedule before submission: FR-OP-034, CLR-OP-45) and RMTID.024 (extracted accounts with the schedule details: FR-OP-032, CLR-OP-46). It also makes RMTID.038 the Operations-wide requirement BRQID.007 (FR-OP-004) and moves the early remittance incentive of PRCID.028 to Remittance as RMTID.041 (FR-OP-038).

```fr
id: FR-OP-035
title: Approve a batch, post the remittance and request payment
brd: [RMTID.010 (p.117), RMTID.019 (p.122-123)]
actor: Remittance TL
priority: Must have
screens: Remittance Batch (workflow panel)
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
  - [R2, "Posting, the payment request and the ORs happen once per batch; a repeat does not duplicate them.", Fixed, "-"]
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
brd: [RMTID.034 (p.130), RMTID.036 (p.130-131)]
actor: System; Remittance Processor
priority: Must have
screens: Remittance Batch (workflow panel, history); Invoice 360 (Remittances); Disbursement Queue
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
brd: [RMTID.012 (p.118-119), RMTID.013 (p.119), RMTID.016 (p.120-121)]
actor: Remittance Processor
priority: "Must have (RMTID.016: not stated in the BRD)"
screens: Insurer OR Upload (upload, history); Report REM-OR-EXCEPTION
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
  - [Bad date or amount, "Line <n>: <column> must be a date such as 28-Oct-2026 / an amount", FEED_VALUE_INVALID]
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
brd: [RMTID.041 (annex p.16-17), RMTID.023 (p.125)]
actor: Remittance TL (rules); System
priority: Must have
screens: Incentive Rules; Remittance Batch (incentive amounts)
description:
  - "An incentive rule holds the insurer, product line (blank = all), segment (blank = all), rate as a percent of basic premium, window in days and the start of the window (inception or booking), with effective dates. An invoice remitted within the window goes into a With Incentives batch; its incentive = rate x basic premium share, with VAT at the invoice's VAT to commission ratio. On approval BIBS posts OPS_REMIT_INCENTIVE (Dr due for disbursement / Cr incentive income, output VAT) and issues the incentive OR."
  - "Eligibility and computation (RMTID.041, previously PRCID.028): the incentive applies to qualified CLG / CBG motor and fire accounts remitted within the defined period (for example 30 days); it is 2% of the basic premium share, computed only when the remittance date and the insurer and remittance data are complete and valid. It is a separate, identifiable calculation: it never changes the base commission or the remitted values and is shown, posted and reported on its own lines."
  - The same rules are read by Production Reconciliation to check the incentive the insurer applied (FR-OP-078).
preconditions:
  - The user has REMIT_APPROVE to maintain rules.
main_flow:
  - The TL adds a rule for an insurer.
  - Each extraction checks whether an invoice is within the window of a rule.
  - The incentive is computed on the batch and posted on approval.
alternate_flows:
  - Remitted after the window. No incentive; the invoice goes to a Normal batch.
rules:
  - [R1, "Rates, windows and basis per insurer and product (proposed default: CLG and CBG motor and fire, 2%, 30 days; start of the window from BDOI, OQ23, CLR-OP-47).", Configurable, Incentive Rules]
  - [R2, "The incentive is a separate computation and a separate line; it is never merged with the commission or the remittance amounts.", Fixed, "-"]
  - [R3, "No incentive without a valid remittance date and complete insurer and remittance data.", Fixed, "-"]
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
  - A CBG motor invoice remitted 20 days after the start of the window is in a With Incentives batch with a 2% incentive shown on its own line; its commission and net due are unchanged.
  - The same invoice remitted after 40 days has no incentive.
  - An invoice of a product the rule does not cover (for example CBG marine) has no incentive.
```

```fr
id: FR-OP-039
title: Search remittance accounts and payment details
brd: [RMTID.025 (p.126)]
actor: Remittance Processor
priority: Must have
screens: DTIP Status; Remittance Batches
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
brd: [RMTID.030 (p.128), RMTID.033 (p.129-130)]
actor: Remittance TL; Remittance Processor
priority: Must have
screens: Special Remittance; Special Remittance Request
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
brd: [RMTID.039 (p.132; p.316-318)]
actor: Remittance Processor; Remittance TL
priority: Must have
screens: Reports (category Operations)
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

Adjustment processes endorsement and cancellation requests on booked invoices: financial, non-financial and internal, singly or in batches.

```fr
id: FR-OP-050
title: Raise an endorsement or cancellation request
brd: [ADJID.001 (p.152), MKTID.014 (annex p.33), ADJID.003 (p.153), MKTID.015 (annex p.33-34), ADJID.020 (p.161)]
actor: Marketing Collection / TL (ADJ_REQUEST); Adjustment Processor (ADJ_PROCESS)
priority: Must have
screens: New Request (wizard); Change Request; Endorsement Request page
description:
  - "The New Request wizard has three steps: choose the policy first - one or more booked invoices (up to 50; one request ENR-yyyy-n is raised per invoice), listed with the insurer policy number, ARN, assured and insurer and searched by policy, ARN, invoice or client; enter the request (the form adapts to the type); review the recompute (FR-OP-054) and save or submit. As soon as an invoice is chosen, and before anything is saved, BIBS shows its policy details: policy number, ARN, invoice, placement slip, client, insurer, product, period of cover and gross premium; they stay on top of the later steps. The ARN, invoice, policy, client, insurer, segment, AO, account and product are copied from the ledger and the account, so every request is linked to its policy and placement."
  - "Endorsement types are shown with their business labels (class, then the change): for example Financial – Change of Cover, Financial – Change of Premium Rate, Non-financial – Cover Extension, Non-financial – Assured Information, Internal Adjustment. Codes are never shown."
  - "The class comes from the endorsement type (list ENDORSEMENT_TYPE): financial (change of TSI, insured items, commission rate, premium rate or amount, extension of cover, change of cover, adjustment in charges, minimal balance), non-financial (descriptive changes, change of period cover, extension of period covered, extension of cover without premium, change of assured name or information) or internal adjustment."
  - "A financial request needs a request type of Annex V (list ENDORSEMENT_REQUEST_TYPE): Flat Cancellation; Flat Cancellation - Retain DST; Partial Cancellation; Increase / Decrease in TSI; Increase / Decrease of Premium Rate; VAT / Premium Tax exempt; Increase / Decrease of taxes; Change of Cover; Extension of Cover; Write-off; Decrease / Increase in Commission; Cancellation Reversal. A cancellation needs a reason (Annex V, 33 values). A non-financial request carries no request type, sum insured or amount."
  - The invoice is locked by Adjustment while a request is open (FR-OP-006); a request that reduces the invoice raises PENDING_NEG_ADJ at submission.
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
  - [Invoice in remittance, "Invoice <no> is locked by <team> (<reason>)", INVOICE_LOCKED]
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
brd: [ADJID.023 (p.162; annex p.37-38 and 40)]
actor: System; requester
priority: Must have
screens: New Request (duplicate warning)
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
brd: [MKTID.019 (annex p.38)]
actor: Requester; Adjustment Processor
priority: Must have
screens: Endorsement Request page (Documents)
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
brd: [ADJID.005 (p.154-155; annex p.34-35), MKTID.016 (annex p.35), MKTID.017 (annex p.35-36)]
actor: Adjustment Processor (validate, return); Adjustment TL (approve)
priority: Must have
screens: Adjustment Workbench (tabs per stage); Endorsement Request page (workflow panel); Posting Batches
description:
  - Requests follow workflow OPS_ENDORSEMENT (section 5). The processor validates a submitted request for approval or, when it has no financial effect, directly for posting. The TL approves it. Extension of cover with additional premium (FIN_EXTENSION) always needs approval.
  - A request can be returned from validation, approval or the posting batch with a reason from ADJ_RETURN_REASON; returned requests are excluded from posting, flagged and the requester is notified. Posted requests cannot be returned. The requester corrects and resubmits, or cancels.
  - "Marketing is the correction owner (ADJID.005 and MKTID.016 as revised by the Adjustment annex): a returned request is routed automatically to the Marketing Collection owner of the account with a notification, also when an Adjustment Processor raised it; Marketing views the reason, corrects the request and resubmits it for validation. The request page shows the original submission, each return (initiator, reason, time) and each resubmission, with the number of returns."
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
  - [R2, "Return reasons from the list; a reason is mandatory for every return (MKTID.016; ADJID.005 allows mandatory or configurable, CLR-OP-52).", Configurable, LOV ADJ_RETURN_REASON]
  - [R3, "A returned request goes to the Marketing owner of the account for correction.", Fixed, "-"]
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
  - A request raised by an Adjustment Processor and returned at validation appears in the work of the Marketing owner of the account, who corrects and resubmits it; the request page then shows one return and one resubmission.
```

> [!NOTE] Changed in v2.1
> The Adjustment annex of May 2026 (signed 21-May-2026) renumbers six endorsement requirements as Marketing activities: ADJID.002 is MKTID.014, ADJID.004 is MKTID.015, ADJID.007 is MKTID.016, ADJID.010 is MKTID.017, ADJID.015 is MKTID.018 and ADJID.025 is MKTID.019 (FR-OP-050, 052, 053, 060, 114). Only MKTID.016 changes in content: Marketing corrects and resubmits returned requests. The revision log also names ADJID.003 and a deleted duplicate MKTID that the table does not show (CLR-OP-51).

```fr
id: FR-OP-054
title: Recompute premium, commission and refund per insurer (addendum)
brd: [ADJID.014 (p.158-159; annex p.36-37)]
actor: System
priority: Must have
screens: New Request (Recompute step); Endorsement Request page (Recompute)
description:
  - BIBS recomputes the premium, commission, refund premium and sum insured by request type. Cancellations use booking's own posting preview, so the preview equals the posting. A TSI change is rated once per insurer share with the catalogue calculator in endorsement mode (remaining term pro-rata or short-period, each insurer at its commission rate, the lead with its branch LGT). Other types take the entered component changes (commission derived from the invoice rate when blank).
  - The before and after values per component and the change per insurer are stored on the request at every save, submission, validation and posting, so the original values are never overwritten.
  - "The recompute runs automatically when a financial endorsement is submitted and uses validated inputs only: effective date, sum insured (TSI), premium rate and policy term. When an input is missing or invalid, the request cannot be submitted (ADJID.014 as revised by the Adjustment annex). Every recompute is logged with its inputs, outputs, user and time."
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
  - [R3, "Before, Change and After are shown on one basis, the premium in force of the policy year: the invoice with the endorsements and returns of the same policy year (CLR-OP-35), so a flat cancellation of a policy with later endorsements takes the whole policy year back to zero and never shows a negative After.", Proposed, "-"]
validations: []
notifications:
  - "None."
audit:
  - Before / after per component and the insurer breakdown are kept for every stage.
acceptance:
  - A decrease in TSI of a 60/40 co-insured invoice shows the premium and commission change per insurer.
  - A commission increase without premium change issues a service invoice at posting.
  - The recompute of a flat cancellation equals the amounts posted by booking.
  - A flat cancellation of a policy with a later endorsement shows Before as the premium in force of the policy year and After as zero for every component.
  - A TSI change submitted without an effective date is refused and nothing is recomputed or stored.
```

```fr
id: FR-OP-055
title: Handle a TSI increase above the package limit
brd: [ADJID.008 (p.155-156)]
actor: System; Marketing AO (quotation)
priority: Must have
screens: New Request; Endorsement Request page (Quotation No.)
description: A TSI increase is checked against the package TSI limit of the product in the catalogue. When the increase takes the account above the limit, submission opens a hand-off QUOTATION_REQUIRED for Marketing (QUOTE_MAINTAIN) to prepare a quotation, and validation waits until the quotation number is linked on the request. The added insurer and the co-insurance shares follow the quotation; non-package shares are recomputed on the endorsed sum insured and remaining period (FR-OP-054).
preconditions:
  - The request is a TSI increase.
main_flow:
  - The user submits the TSI increase.
  - BIBS finds the limit exceeded and opens the hand-off to Marketing.
  - Marketing prepares the quotation; the user enters the **Quotation No.** on the request.
  - The processor validates the request.
rules:
  - [R1, "Limit from the maximum sum insured of the catalogue product; insurer capacity per product to be confirmed (OQ33).", Configurable, Catalogue]
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
brd: [ADJID.006 (p.155), ADJID.011 (p.157)]
actor: Adjustment Processor (ADJ_POST)
priority: Must have
screens: Posting Batches; Batch Request Upload; Endorsement Request page (Accounting)
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
  - [R2, "Posting happens once per request; a repeat does not duplicate it.", Fixed, "-"]
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
brd: [ADJID.009 (p.156-157), ADJID.012 (p.157-158), ADJID.013 (p.158)]
actor: System; Adjustment Processor
priority: Must have
screens: Endorsement Request page; Adjustment Workbench (Payments to Re-apply); Unapplied Payments
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
  - [R1, "Re-application happens once per request and invoice; a repeat does not duplicate it.", Fixed, "-"]
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
title: Control over-adjustment and capture justifications
brd: [ADJID.023 (p.162; annex p.37-38 and 40), ADJID.028 (p.166)]
actor: System; requester
priority: Must have
screens: New Request (previous adjustments, justification); Invoice 360 (adjustment totals)
description:
  - BIBS keeps the cumulative adjustments of each original invoice against its original premium, DTIP and commission. Previous adjustments of the invoice are shown on the request before it is submitted; duplicates are flagged or blocked by FR-OP-051.
  - "Justification and validation rules of the ADJID.023 annex (p.40): submission needs a justification, and raises the alert ADJ_OVER_BASELINE, when (a) a single adjustment exceeds the baseline limit per adjustment, (b) the request results in a negative balance, (c) the cumulative adjustments approach or exceed the original premium or the original DTIP, or (d) a duplicate is overridden. A standard adjustment within the limits needs no justification. When the cumulative adjustments reach the warning level (proposed 90% of the original premium) BIBS shows a warning before submission."
preconditions:
  - "None."
main_flow:
  - The user submits a request.
  - BIBS adds the request to the cumulative adjustments and compares with the baseline.
  - Above the baseline, BIBS asks for the justification and raises the alert.
rules:
  - [R1, "Cumulative baseline 100% of the original premium and of the original DTIP (default, OQ37).", Configurable, Parameter ADJ_BASELINE_PERCENT]
  - [R2, "Baseline limit per adjustment and warning level (proposed 90%): values from BDOI (CLR-OP-53).", Configurable, Parameters]
  - [R3, "Justification required for the four breaches of the annex; none for a standard adjustment.", Fixed, "-"]
validations:
  - [Above the baseline without justification, "Cumulative adjustments of <amount> exceed the baseline of <n>% of the original premium <amount>", ADJ_OVER_BASELINE]
notifications:
  - ADJ_OVER_BASELINE alert to the Adjustment TL.
audit:
  - The justification and the cumulative totals are recorded.
acceptance:
  - A second premium increase that takes the adjustments above 100% of the original premium needs a justification.
  - A decrease that would make the premium negative is refused without justification.
  - A request that takes the cumulative adjustments to 92% of the original premium shows the warning and can be submitted without justification.
  - A request whose cumulative decrease exceeds the original DTIP needs a justification.
```

```fr
id: FR-OP-059
title: Write off minimal balances from a file
brd: [ADJID.026 (p.164-165)]
actor: Adjustment Processor (ADJ_POST)
priority: Must have
screens: Minimal Balance File; Report ADJ-MINBAL-FILE
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
brd: [MKTID.018 (annex p.37), ADJID.018 (p.160-161)]
actor: Marketing; Adjustment Processor
priority: Must have
screens: Endorsement Request page (Endorsement Slip, Validation Slip)
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
title: Search requests by policy, view the policy transaction history and ageing
brd: [ADJID.021 (p.161-162; annex p.37), ADJID.022 (p.162), ADJID.024 (p.163)]
actor: Adjustment users; Marketing
priority: Must have
screens: Adjustment Workbench; Endorsement Request page (Policy, Policy Transactions, History); Invoice Search and Invoice 360 (Adjustments, Policy Transactions); Account page (Policy Transactions)
description:
  - "Every request shows the policy it is against: the insurer policy number (as the ledger holds it now, since it is often issued after the request is raised), the ARN, the invoice, the insurer, the product and the placement slip. The chain is request, invoice, account (ARN), placement slip, policy number."
  - "The Adjustment Workbench lists requests by stage tab with counts. Its columns are Request No. (with the date raised), Policy No. / ARN, Invoice / Placement Slip, Assured (with the client code), Insurer / Product (names, never codes), Type (business label, with the request type), Effective, Aging, Status and Flags. One search box finds a request by request, policy, ARN, invoice or client (also by insurer, product, placement slip or insurer endorsement reference). Aging reads in days (\"0 days\", \"1 day\", \"12 days\"), from the request date (submission, or creation) to the completion date in Philippine days, as on the other workbenches. The completion date is the posting date (ADJID.021 as revised by the Adjustment annex); an open request ages to today."
  - The request page shows the Policy No., ARN and invoice chips, the policy and placement (tab Policy, with links to the account and the invoice) and keeps the complete history with the old and new details, status and remarks. From an ARN the user opens Invoice 360 with the payment and remittance history and the requests of the invoice (auto-complete on ARN).
  - "Policy Transactions (on the request page, on Invoice 360 and on the account page, one table for all three): the original booking (invoice, premium, taxes, commission), then each endorsement, adjustment, cancellation and refund in date order, each with its date and effective date, type label, reference (invoice, request), premium, taxes and commission change, the position after it (gross premium and commission; open requests have none), its status and its accounting entries. A row expands to its GL journals: journal no., date, status and lines (account, debit, credit, party), with a link to the journal page for users who may view journals. The history reads the Operations invoice ledger, the booked invoices and the journals of the accounting engine; no second ledger is kept."
  - Every journal a posted request writes is kept with the request, including the journals of the re-application of the invoice's payments; the refund to the client of payments made in excess by a decrease or cancellation is its own row with those journals.
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
  - A request submitted 3 days ago shows ageing "3 days" in the workbench.
  - A request submitted on 01-Oct-2026 and posted on 06-Oct-2026 shows ageing "5 days" and keeps it after posting.
  - The History tab shows the before and after values of each change.
  - Searching an ARN lists its requests and opens Invoice 360.
  - Searching an insurer policy number lists the requests on that policy, with the policy number, ARN, invoice, insurer and product names and placement slip in the list.
  - The Policy Transactions of a flat-cancelled invoice show the original booking, then the cancellation (Financial – Change of Cover, Flat Cancellation) with the negative premium and tax change, a gross premium of zero after it, status Posted and its journals, whose lines balance.
```

```fr
id: FR-OP-062
title: Generate the Adjustment reports
brd: [ADJID.016 (p.159), ADJID.017 (p.160), ADJID.019 (p.161)]
actor: Adjustment users; Comptrollership
priority: Must have
screens: Reports (category Operations)
description:
  - "ADJ-DAILY: Adjustment and Daily Endorsement Report of the requests raised or posted in the period, by type and user. the daily endorsement report run (18:00 PHT) exports it as Excel for the business date, archives it and notifies the holders of ADJ_APPROVE."
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

Production Reconciliation sends each insurer a register of the accounts BDOI booked with it, takes in the insurer's answer, matches both sides and follows every difference to closure. The work is organised in cycles: one open cycle per insurer and production month (workflow OPS_RECON, section 5).

BDOI places Production Reconciliation in **Drop 2** (drop plan of 26-Sep-2026): it is specified with Operations and is tested and signed off in the Drop 2 SIT and UAT. The deliverables index of Drop 2 points to this chapter.

```fr
id: FR-OP-070
title: Schedule the automatic extraction of the production register
brd: [PRCID.001 (p.142)]
actor: Recon Handler (schedules); System (the scheduled register extraction)
priority: Must have
screens: Extract Schedules
description: Each insurer has a schedule with its frequency (monthly on a day of the month, or weekly on a weekday), whether the register is sent automatically, and the recipients. the scheduled register extraction (daily 09:00 PHT) extracts the booked accounts of every insurer due that day. A run date that falls on a holiday moves to the next working day of the head office calendar.
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
brd: [PRCID.005 (p.143), PRCID.011 (p.144), PRCID.012 (p.145), PRCID.013 (p.145), PRCID.020 (p.146), PRCID.034 (p.149)]
actor: Recon Handler
priority: Must have
screens: Production Extracts (New Extract); Reconciliation Cycle (Registers Sent); Report PRC-EXTRACT-LOG
description:
  - The handler extracts the register of an insurer for a booking period (PRX-yyyy-n). Each line records the ledger facts of the booked account, the last payment and the estimated flag; an account not on an earlier register of the cycle counts as new. The extract shows its file name, location in the list of extracts (folder PRODRECON/<insurer>), date and time, creator and row count.
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
brd: [PRCID.002 (p.142), PRCID.004 (p.142-143), PRCID.006 (p.143), PRCID.007 (p.143)]
actor: System
priority: Must have
screens: Production Extracts (download)
description:
  - "The register is an XLSX workbook in the Production Register layout of Annex IV #5: month of production, insurer, invoice number, booking, inception and expiry dates, policy number, endorsement number, PN number, assured name, insurance code, risk type, basic premium, gross commission, A/R client, gross premium, booked VAT, amount paid, date paid, OR number, adjustment type, remittance status."
  - The sheet is protected; only the Remarks and Incentive columns and 200 blank rows are editable by the insurer. The file is named by the naming pattern and protected by a password when it is e-mailed (FR-OP-073).
preconditions:
  - An extract exists.
main_flow:
  - BIBS creates the workbook from the extract lines.
  - BIBS names it and stores it in the list of extracts.
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
brd: [PRCID.003 (p.142; annex p.22), PRCID.008 (p.144)]
actor: Recon Handler (RECON_SEND)
priority: Must have
screens: Production Extracts (Send); Reconciliation Cycle (workflow panel)
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

> [!NOTE] Open point in v2.1
> The Production Reconciliation annex of May 2026 says "Cover letter generation (PRCID.003) is no longer included in this addendum" while printing PRCID.003 unchanged in its table (annex p.22). BRD v1.01 keeps PRCID.003 (p.142). The cover letter is kept as described until BDOI confirms whether PRCID.003 is removed from scope or only unchanged (CLR-OP-48); if it is removed, the cover letter text becomes optional and the register is still sent with its password e-mail.

```fr
id: FR-OP-074
title: Upload the insurer production report
brd: [PRCID.009 (p.144), PRCID.010 (p.144), PRCID.022 (p.146-147; annex p.23-24), PRCID.031 (p.148-149), PRCID.032 (p.149)]
actor: Recon Handler
priority: Must have
screens: Insurer Feedback (upload, attempts); Interfaces (feed INSURER_PRODUCTION)
description:
  - The handler uploads the insurer's production report or matched file (the returned register). BIBS validates the layout and each row and matches the file at once (FR-OP-075). Lines of accounts that were not in the original extract are kept apart as insurer-only lines (unbooked, FR-OP-076).
  - "As the Production Reconciliation annex requires (PRCID.022), BIBS keeps the technical match (the records correspond on policy number and reference / invoice number) apart from the reconciliation outcome (the comparison of the fields and the insurer's feedback, such as a variance): a pair is Matched only when no field differs beyond the tolerance and the insurer reports no variance; a record with a discrepancy is never tagged fully matched. Uploaded records are linked to the existing booked record, or kept as a new insurer-only record."
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
  - [R1, "Upload by the handler; automatic pick-up from a mailbox or file transfer folder is added when BDOI specifies the insurer channels (OQ29).", Fixed, "-"]
  - [R2, "Duplicate by file content (SHA-256).", Fixed, "-"]
validations:
  - [Duplicate file, "Upload refused: <earlier upload>", RECON_UPLOAD_DUPLICATE]
  - [Column missing, "The file is not in the production register layout: column '<column>' missing", RECON_FILE_LAYOUT]
  - [Row without keys, "Row <n> has neither invoice nor policy number", RECON_ROW_INCOMPLETE]
  - [Row without insurer or month, "Row <n> has no insurer or production month (yyyy-MM)", RECON_ROW_INCOMPLETE]
  - [Bad amount, "Row <n>: <column> '<value>' is not an amount", RECON_ROW_AMOUNT]
  - [Bad date, "Row <n>: <column> '<value>' is not a date", RECON_ROW_DATE]
  - [Unknown company, "Upload the production of <insurer> from Production Reconciliation - Insurer Feedback (company not known)", RECON_COMPANY_UNKNOWN]
notifications:
  - RECON_FEEDBACK_UPLOADED to the handlers.
audit:
  - Each attempt is kept with its flow-in run and records.
acceptance:
  - Uploading the returned register matches it and lists the insurer-only lines separately.
  - Uploading the same file again is refused and counted as a new attempt.
  - A returned line that pairs on the policy number but carries an insurer variance is Matched with Discrepancy, not Matched.
```

> [!NOTE] Difference from the BRD
> PRCID.009 asks to receive and upload the insurer report automatically. Insurer channels are not specified (OQ29); the handler uploads the file; a mailbox or file transfer pick-up is added later without a change to the matching (chapter 21, CLR-OP-03).

```fr
id: FR-OP-075
title: Match booked and insurer lines within tolerance
brd: [PRCID.024 (p.147), PRCID.025 (p.147), PRCID.026 (p.147; annex p.24-25), PRCID.027 (p.147-148), PRCID.030 (p.148)]
actor: System; Recon Handler (re-match)
priority: Must have
screens: Reconciliation Cycle (Items by bucket, side-by-side comparison)
description:
  - "BIBS pairs each insurer line with a booked line using the keys of RECON_MATCH_KEYS in order (default invoice number, then policy number). It then compares the fields: policy, reference / invoice and PN numbers and the policy period must be equal; the assured name is compared ignoring case and spacing; commission, basic premium and gross premium match when they differ by 1.00 peso equivalent or less, whichever side is higher."
  - "Foreign currency accounts (PRCID.026 as revised by the Production Reconciliation annex): the difference of an amount field of a USD (or other foreign currency) account is converted to pesos before the tolerance is applied, at the BOOK rate of the booking date of the invoice (proposed, CLR-OP-49); a difference above 1.00 peso equivalent is not matched. The comparison shows the difference in the account currency and in pesos."
  - "Status of each item: MATCHED; MATCHED_WITH_DISCREPANCY (the differing fields are listed); BDOI_ONLY (booked, not returned by the insurer); UNMATCHED_PREBOOKED; UNMATCHED_NO_BOOKING. Each status is a bucket tab with its count; a discrepancy row expands to a side-by-side comparison with the tolerance highlighted."
  - Matching runs on every upload, when a later invoice is booked (waiting insurer-only lines of the same insurer), and by **Match Again** or the scheduled run for every open cycle.
preconditions:
  - A cycle has extract lines and insurer lines.
main_flow:
  - BIBS pairs the lines by key.
  - BIBS compares the fields and tags each item.
  - The handler reviews the buckets.
rules:
  - [R1, "Tolerance 1.00 peso equivalent per amount field; foreign currency differences converted to pesos first.", Configurable, Parameter RECON_TOLERANCE]
  - [R4, "Rate of the peso conversion: BOOK rate of the booking date of the invoice (proposed, CLR-OP-49).", Configurable, Rate type BOOK]
  - [R2, "Match keys and order.", Configurable, Parameter RECON_MATCH_KEYS (OQ30)]
  - [R3, "Automatch schedule manual until BDOI gives the frequency.", Configurable, "Scheduled run (OQ30)"]
validations:
  - [Action on a closed cycle, "Cycle <no> is closed", RECON_CYCLE_CLOSED]
notifications:
  - "None."
audit:
  - Each item keeps its match method (AUTO or MANUAL), status history and differences.
acceptance:
  - A gross premium difference of 0.80 is MATCHED; a difference of 1.20 is MATCHED_WITH_DISCREPANCY listing gross premium.
  - On a USD account with a BOOK rate of 58.00, a difference of USD 0.01 (PHP 0.58) is MATCHED and a difference of USD 0.02 (PHP 1.16) is MATCHED_WITH_DISCREPANCY.
  - An account the insurer did not return is BDOI_ONLY.
  - Booking the invoice of a waiting insurer-only line matches it automatically.
```

> [!NOTE] Difference from the BRD
> PRCID.030 names four statuses and the Production Reconciliation annex names three for PRCID.022 (Matched, Matched with Discrepancy, Unmatched). BIBS keeps five: MATCHED, MATCHED_WITH_DISCREPANCY, and three kinds of Unmatched (BDOI_ONLY for booked accounts the insurer did not return, UNMATCHED_PREBOOKED and UNMATCHED_NO_BOOKING for insurer-only records), so that each can be followed (CLR-OP-18, CLR-OP-50). In v2.1 the tolerance is applied in peso equivalent (PRCID.026 as revised by the annex).

```fr
id: FR-OP-076
title: Follow unbooked and pre-booked insurer production
brd: [PRCID.019 (p.146), PRCID.023 (p.147), PRCID.033 (p.149)]
actor: System; Recon Handler
priority: Must have
screens: Unbooked Accounts; Report PRC-UNBOOKED
description: For each insurer-only line BIBS searches New Business for a pre-booked account (ARN) with the same reference. The line becomes UNMATCHED_PREBOOKED when one is found and UNMATCHED_NO_BOOKING otherwise. The Unbooked Accounts list shows the unmatched and duplicate lines with their unbooked status (OPEN, PREBOOKED, BOOKED, CLOSED), feedback and disposition, and is searchable and filterable. PRC-UNBOOKED lists the unbooked accounts and their status.
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
brd: [PRCID.014 (p.145), PRCID.015 (p.145; annex p.23), PRCID.016 (p.145), PRCID.021 (p.146)]
actor: Recon Handler
priority: Must have
screens: Reconciliation Cycle (Items, filters, review dialog, bulk disposition)
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
  - [R1, "Company-concerned and disposition lists (values from BDOI, OQ31; seed values delivered).", Configurable, LOV RECON_COMPANY_CONCERNED; LOV RECON_DISPOSITION]
  - [R2, "Annotations (company concerned, instruction or notation, feedback) are linked to the selected production register record and never change policy, invoice or financial data (PRCID.015 as revised by the annex).", Fixed, "-"]
validations:
  - [Pair across cycles or wrong sides, Pair a booked-only item with an insurer-only item of the same cycle, RECON_PAIR_INVALID]
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
  - Saving an instruction on an item leaves the policy number, invoice and amounts of the item and of the invoice unchanged, and the instruction shows on that item only.
```

> [!NOTE] Difference from the BRD
> PRCID.021 asks to filter by BDOI location. The items filter by AO, sales unit, segment and product line; the location is available on the report PRC-UNMATCHED-LOC.

```fr
id: FR-OP-078
title: Check the early remittance incentive applied by insurers
brd: [RMTID.041 (annex p.16-17)]
actor: Recon Handler
priority: Must have
screens: Reconciliation Cycle (Early Incentive); Report PRC-EARLY-INCENTIVE
description: For each booked account of the cycle, BIBS checks the remittance date against the insurer's incentive rate and window kept by Remittance (FR-OP-038), so that the handler can compare the incentive the insurer applied with the one Remittance computed. The account is ELIGIBLE when remitted within the window (30 days for CLG / CBG motor and fire at 2% in the BRD example), NOT_ELIGIBLE when remitted late, and NO_RULE when no rule covers it. The check reads the Remittance computation and never computes a second incentive.
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

> [!NOTE] Changed in v2.1
> The Production Reconciliation and Remittance annexes of May 2026 move PRCID.028 to Remittance as RMTID.041: Remittance validates the eligibility and computes the incentive (FR-OP-038). FR-OP-078 is kept as the reconciliation check of the incentive the insurer applied, traced to RMTID.041; BDOI confirms whether this check stays (CLR-OP-47).

```fr
id: FR-OP-079
title: Close a cycle and view its history
brd: [PRCID.029 (p.148)]
actor: Recon Handler
priority: Must have
screens: Reconciliation Cycles; Reconciliation Cycle (workflow panel, History)
description: The cycle keeps its counts per bucket and its history (timestamps, actions, users). It closes by itself when every item is matched or marked for closure; otherwise the handler closes it with a comment.
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
brd: [PRCID.017 (p.145-146), PRCID.018 (p.146), PRCID.035 (p.149), PRCID.036 (p.149-150), PRCID.037 (p.150), PRCID.038 (p.150), PRCID.039 (p.150)]
actor: Recon Handler; TL / TH
priority: Must have
screens: Reports (category Operations)
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

Commission Receivables handles direct payment (DP) accounts, where the client paid the insurer directly: BDOI bills the insurer for its commission, follows the answer, collects the commission and reverses the premium receivable. It also runs the incentive programmes and tracks BIR certificates. The Commission Receivables annex of May 2026 (signed 13-May-2026) makes the system tagging the single source of the DP entries, limits the review to a read-only view, allows exclusion by approved rules only, bills only fully paid accounts, computes incentives per invoice under each insurer's SLA and ages the receivables by invoice; FR-OP-090 to FR-OP-099 follow it.

```fr
id: FR-OP-090
title: Take in the DP entries from the system tagging
brd: [CMRID.001 (p.169; annex p.46)]
actor: System; Commission Handler
priority: Must have
screens: DP Lists (DP entries, branch submissions); DP Accounts
description:
  - "Every invoice tagged direct payment at quotation or policy (MKTID.011) whose DP tagging is confirmed by Marketing Collection (MKTID.012, the Collections disposition DP PR for reversal of BRD-4) flows into Commission Receivables as a DP account DPL-yyyy-n, with no file to upload: the system tagging is the single source of the DP entries (Commission Receivables annex, p.43). The DP entries of Head Office and of each branch are consolidated with de-duplication by invoice and become the basis of billing."
  - "The DP Lists screen shows the entries received per branch and period, so the handler sees which branches had DP entries. The upload of a DP list file named <Branch>_DP_<yyyyMMdd>, which CMRID.001 still describes, is kept only as a fallback for entries that do not come from the tagging, until BDOI confirms its withdrawal (CLR-OP-54); a file with the same content as an earlier one is refused."
preconditions:
  - The invoice is booked and tagged direct payment; for the fallback upload the user has COMMREC_PROCESS.
main_flow:
  - Marketing Collection confirms the DP tagging of an invoice.
  - BIBS creates the DP account of the invoice in Commission Receivables, once.
  - BIBS reviews the account (FR-OP-091).
alternate_flows:
  - Fallback upload. The handler uploads a DP list; BIBS checks the name and the layout and reads each row as a DP account.
  - The invoice is already a DP account. BIBS does not create a second one and shows it as a duplicate.
rules:
  - [R1, "The DP tag of the booking and the confirmation of Marketing Collection are the source of the DP entries.", Fixed, "-"]
  - [R2, "An invoice already active as a DP account is a duplicate.", Fixed, "-"]
  - [R3, "Fallback upload naming <Branch>_DP_<yyyyMMdd>, for example HO_DP_20260930.xlsx, until withdrawn (CLR-OP-54).", Fixed, "-"]
validations:
  - [Name not in the convention, "Name the DP list <Branch>_DP_<yyyyMMdd> (e.g. HO_DP_20260930.xlsx): <file>", DP_LIST_NAME]
  - [Date part not a date, "The date in <file> is not a date (yyyyMMdd)", DP_LIST_NAME]
  - [Same file again, "<file> was already taken in as <list no>", DP_LIST_DUPLICATE]
  - [No invoice column, "The DP list has no '<column>' column", DP_LIST_LAYOUT]
  - [Row without invoice, The row has no invoice number, DP_ROW_INCOMPLETE]
  - [Premium not an amount, "Premium '<value>' is not an amount", DP_ROW_PREMIUM]
  - [No invoice of the list booked, "No invoice of the list is booked: upload it from Commission Receivables", DP_LIST_COMPANY]
fields_screen: DP Lists (fallback upload)
fields:
  - [DP List File, File, "Yes", "-", Naming convention]
notifications:
  - "None."
audit:
  - Each DP account keeps its source (tagging or file), the branch, the period and the time it was received; a file keeps its name and fingerprint.
acceptance:
  - A direct payment invoice confirmed by Marketing Collection appears as a DP account without any upload.
  - The same invoice confirmed twice gives one DP account.
  - A fallback list named CEB_DP_20260930.xlsx is taken in and appears for branch CEB; a list named dp list.xlsx is refused with DP_LIST_NAME.
```

> [!NOTE] Changed in v2.1
> The annex introduction (p.43) replaces manual DP list uploads by the DP tagging of the upstream processes, while CMRID.001 is reprinted unchanged with the Head Office and branch DP lists (p.46). The tagging is the main source in v2.1 and the upload stays as a fallback until BDOI confirms (CLR-OP-54). BRD-4 Collections confirms the DP entries through its disposition DP PR for reversal (R9).

```fr
id: FR-OP-091
title: Review DP entries read-only, exclude by rule and confirm eligibility from the fully paid status
brd: [CMRID.002 (p.169-170; annex p.46-48), CMRID.003 (p.170; annex p.48-50), CMRID.007 (p.172; annex p.54-55), CMRID.008 (p.172-173; annex p.55-57), CMRID.013 (p.175; annex p.59-61)]
actor: System; Commission Handler
priority: Must have
screens: DP Accounts (tabs by status, account detail)
description:
  - "Read-only review (CMRID.002): each DP account is shown at invoice level with its production record, the history of its commission movements and adjustments (for example endorsements that changed the premium or the commission), the premium remittance status (fully paid or not fully paid), the commission status (paid or unpaid) and the key fields (policy number, invoice amount, insurer, branch). Everything is read-only: the review does not validate, reconcile, match, compute or correct anything. BIBS highlights what the user can see is inconsistent (missing data, a duplicate entry, a status that differs from what is expected, for example unpaid but expected paid) as flags on the account, without applying a validation rule."
  - "Rule-based exclusion (CMRID.003): BIBS excludes only the entries that meet an approved, configurable exclusion rule, per insurer agreement, SLA or business criterion. The rules delivered are Negative amount and Fully cancelled transaction; further rules are added from the annex of exclusion criteria that BDOI provides (CLR-OP-56). Undefined terms such as erroneous booking are not used, and there is no exclusion by the user's judgement. An excluded entry stays visible with its rule, reason and time, and is left out of billing, reports and incentive computation."
  - "Eligibility (CMRID.013): an account is eligible for billing only when the premium receivable data upstream confirm it fully paid (the payment status of the invoice ledger, fed by Cashiering and the Collections dispositions; CLR-OP-62). The handler does not confirm the payment by hand. The account shows whether it is eligible and the basis of the decision (the payment status and its date). A manual override of eligibility is not offered; if BDOI asks for one it needs a reason and an approval and is kept in the audit trail."
  - "Commission receivable (CMRID.007): BIBS computes the commission receivable of each eligible account (commission, VAT, withholding tax, net) from the ledger, without manual work."
  - "Account status (CMRID.008): an account is Valid when it is eligible and not excluded (it proceeds to billing and payment), Invalid when an exclusion rule or the insurer's rejection applies, and Returned when it is sent back for correction or review; Invalid and Returned need a predefined reason before the status is final (FR-OP-093). The v2.0 tags map to these statuses (DP for billing, Billed and Approved are Valid; Rejected is Invalid or Returned; Excluded is Invalid), as listed in CLR-OP-60."
preconditions:
  - The user has COMMREC_PROCESS to work the accounts; OPS_VIEW to view them.
main_flow:
  - BIBS takes the DP account (FR-OP-090) and applies the exclusion rules.
  - BIBS reads the payment status upstream and marks the account eligible or not eligible, with the basis.
  - BIBS computes the commission receivable of the eligible accounts and sets them Valid.
  - The handler reviews the accounts read-only and the highlighted inconsistencies.
alternate_flows:
  - An exclusion rule applies. The account is Invalid with the rule and reason and stays visible.
  - The account is not yet fully paid. It waits as not eligible and becomes eligible when the payment status changes.
  - The ledger changes (for example an endorsement). BIBS re-reads the account and its status.
rules:
  - [R1, "Only Valid accounts (eligible from the fully paid status and not excluded) are billed.", Fixed, "-"]
  - [R2, "Exclusion rules Negative amount and Fully cancelled; others from the annex of criteria (CLR-OP-56).", Configurable, LOV INCENTIVE_EXCLUSION_RULE (DP exclusion rules)]
  - [R3, "The review is read-only; no user exclusion or manual payment confirmation.", Fixed, "-"]
  - [R4, "Source of the fully paid status: the payment status of the invoice ledger (CLR-OP-62).", Fixed, "-"]
validations:
  - [Action on an account in another status, "Account <invoice> (<tag>) cannot be <action>", DP_ITEM_STATE]
  - [Account already in the target status, "Account <invoice> is already <tag>", DP_ITEM_STATE]
notifications:
  - "None."
audit:
  - Each exclusion keeps the rule, reason and time; each eligibility decision keeps its basis; each status change keeps user, time and reason.
acceptance:
  - A DP account shows its production record, commission history, remittance status and commission status and offers no field to change.
  - An account with a negative amount is Invalid with the rule Negative amount, stays visible and is not billed.
  - An account whose invoice is not fully paid is not eligible and shows the payment status as the basis; when the payment status becomes Paid the account becomes Valid without a user action.
  - The same invoice entered twice is highlighted as a duplicate.
```

> [!NOTE] Changed in v2.1
> CMRID.002, 003, 007, 008 and 013 are rewritten by the Commission Receivables annex. Version 2.0 validated each account against the ledger with a sanitation result (Valid, Duplicate, Invalid, Incomplete), let the handler exclude an account with a reason and asked the handler to confirm that the client paid in full. In v2.1 the review is read-only with highlighted inconsistencies, exclusion follows approved rules only, eligibility comes from the fully paid status upstream and the statuses are Valid, Invalid and Returned (CLR-OP-55, 56, 60, 62).

```fr
id: FR-OP-092
title: Bill the insurer for DP commission and produce the Statement of Account
brd: [CMRID.009 (p.173-174; annex p.57-58), CMRID.012 (p.174-175; annex p.59), CMRID.007 (p.172; annex p.54-55)]
actor: Commission Handler
priority: Must have
screens: DP Accounts (Prepare Billing); DP Billings; DP Billing (Send)
description:
  - "The handler prepares billings from the Valid accounts: one billing CRB-yyyy-n per insurer, assigned to the handler (workflow OPS_DP_BILLING, section 5). The billing workbook lists the accounts with premium, commission, VAT, withholding tax and net. **Send Billing to Insurer** e-mails it as a password-protected file with the password in a separate e-mail and logs the request with its billing date, which starts the ageing of the receivable (FR-OP-099)."
  - "Statement of Account (CMRID.007): besides the billing, BIBS produces per insurer a Statement of Account of the commission receivables (proposed layout: insurer, statement date, each invoice with policy, billing date, commission, VAT, withholding tax, net, amount collected and balance, and the totals; CLR-OP-59). Billings and statements are produced on demand or on a schedule set by the System Administrator, grouped per insurer, from the consolidated system data and the insurer data received."
  - "The answer is followed up through the insurer's SLA: by default BIBS sets a follow-up date 10 working days after sending on the head office calendar and reminds the handler when it passes (FR-OP-093); BDOI confirms whether this reminder stays now that the receivables are aged by invoice (CLR-OP-61)."
preconditions:
  - The user has COMMREC_PROCESS; accounts are Valid.
main_flow:
  - The handler clicks **Prepare Billing**; BIBS creates one billing per insurer.
  - The handler opens a billing and clicks **Send Billing to Insurer**.
  - BIBS sends it, tags the accounts BILLED and moves the billing to AWAITING_INSURER.
  - The handler produces the Statement of Account of the insurer when it is needed.
alternate_flows:
  - Cancel a billing before it is sent; its accounts return to Valid, ready for billing.
  - Scheduled run. BIBS prepares the billings and statements on the schedule; the handler checks and sends them.
rules:
  - [R1, "Follow-up date 10 working days after sending (default; to be confirmed, CLR-OP-61).", Configurable, Parameter CMR_FEEDBACK_WORKING_DAYS]
  - [R2, "Billing and Statement of Account layouts to be confirmed (OQ38, CLR-OP-59).", Configurable, Billing columns; document template]
  - [R3, "A billing and a statement hold the accounts of one insurer.", Fixed, "-"]
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
  - The billing keeps file, recipients, sent time, billing date and follow-up date; each statement keeps its date and content.
acceptance:
  - Valid accounts of two insurers produce two billings.
  - A sent billing shows its billing date and its follow-up date 10 working days later.
  - The Statement of Account of an insurer lists each billed invoice with commission, collected amount and balance, and its totals equal the billings of that insurer.
```

```fr
id: FR-OP-093
title: Record insurer answers and the status of each DP account
brd: [CMRID.008 (p.172-173; annex p.55-57), CMRID.009 (p.173-174; annex p.57-58), CMRID.012 (p.174-175; annex p.59)]
actor: Commission Handler; System (the daily feedback follow-up)
priority: Must have
screens: Insurer Responses (answers upload); DP Billing (answers); DP Billings
description:
  - The insurer approves or rejects each account. Answers are entered on the billing or uploaded (columns Billing No., Invoice No., Decision, Reason, Comment). A rejection needs a reason from DP_FEEDBACK_REASON before it is final. Approved accounts stay Valid and go to collection; rejected accounts become Invalid, or Returned when they are sent back for correction or review, and are returned to the Collection team through the COLLECTION_DP_RETURNED extract with the time and reason. A billing whose accounts were all rejected moves to RETURNED_TO_COLLECTION.
  - "The validation requests sent, the answers received and the response status are tracked per account and per billing (CMRID.008, CMRID.012); the history of each account shows every status change with the validation result, the insurer's answer and the reason."
  - The daily feedback follow-up raises the alert Insurer feedback overdue (NT-20) for billings without an answer after the follow-up date (FR-OP-092, CLR-OP-61).
preconditions:
  - The billing is AWAITING_INSURER.
main_flow:
  - The handler enters or uploads the answers.
  - BIBS sets the accounts Valid (approved) or Invalid / Returned (rejected, with the reason) and moves the billing.
alternate_flows:
  - No answer by the follow-up date. BIBS flags the billing overdue and notifies.
rules:
  - [R1, "Feedback reasons (values from BDOI, OQ40; Others until then); a reason is required before an Invalid or Returned status is final.", Configurable, LOV DP_FEEDBACK_REASON]
validations:
  - [Rejection without reason, "Give the insurer's reason for rejecting <invoice>", DP_REASON_REQUIRED]
  - [Billing not waiting, "Billing <no> is not waiting for the insurer", DP_BILLING_NOT_AWAITING]
  - [Invoice not on the billing, "Invoice <no> is not on billing <no>", DP_NOT_ON_BILLING]
  - [Unknown billing in the file, "Billing <no> does not exist", DP_BILLING_UNKNOWN]
  - [Decision not Approved or Rejected, "Decision '<value>' is not one of Approved or Rejected (or Yes / No)", DP_RESPONSE_DECISION]
  - [Column missing, "The insurer's answer has no '<column>' column", DP_RESPONSE_LAYOUT]
notifications:
  - DP_FEEDBACK_OVERDUE to the handlers and TLs.
audit:
  - Each answer keeps decision, reason, comment, user and time; returned accounts keep the return time; every status change is kept.
acceptance:
  - A rejection without a reason is refused.
  - An approved account stays Valid and a rejected account shows Invalid with the insurer's reason in its history.
  - A billing without answer after the follow-up date shows overdue and raises the alert.
  - Rejected accounts are in the COLLECTION_DP_RETURNED extract.
```

```fr
id: FR-OP-094
title: Collect DP commission and reverse the premium receivable
brd: [CMRID.010 (p.174; annex p.58), MKTID.012 (p.138-139; annex p.63-64)]
actor: Commission Handler; System
priority: Must have
screens: DP Billing (Collect); DP Accounts (Reverse, Reinstate)
description:
  - "When the insurer pays the commission of an approved billing, the handler records the collection with the bank account, collection date and BIR certificate number. BIBS posts OPS_DP_COMMISSION_COLLECT (cash and CWT against the commission receivable; realisation of commission and output VAT), requests the commission OR from Cashiering and, for each approved account, records the DP_REVERSAL of the premium receivable and DTIP and the APPLIED commission. The billing moves to COLLECTED and then CLOSED."
  - The reversal is made only for accounts tagged direct payment by Marketing (MKTID.011) and confirmed collected; an account that fails the tag check blocks and is notified.
  - A reversed account can be reinstated with a direct payment reason from REINSTATEMENT_REASON (CSHID.004 group b), then reversed again. The DP_CANCELLATION reason also sends the collected commission to Unapplied Payments.
  - "PR 2307 reversals flagged For Commission Receivable by Marketing Collection (an outstanding commission receivable on the same invoice or account, MKTID.010 and MKTID.013, FR-OP-113) are listed for the Commission Receivables team with the invoice, the 2307 reference and the commission outstanding, so the commission is recovered before the case is closed (CLR-OP-42)."
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
title: Compute and post SLA-based incentives per invoice
brd: [CMRID.005 (p.171-172; annex p.52-53), CMRID.006 (p.172; annex p.53-54), CMRID.003 (p.170; annex p.48-50)]
actor: Commission TL (INCENTIVE_MANAGE, COMMREC_APPROVE)
priority: Must have
screens: Incentive Schemes (tier editor); Incentive Runs (compute, lines, post, cancel)
description:
  - "An incentive programme (scheme) is defined by configurable parameters taken from the insurer's Service Level Agreement or programme set-up, never by a programme name built into the system (CMRID.005 and CMRID.006 as revised by the Commission Receivables annex): the insurer, the programme type (a list maintained by the business; the v2.0 names No Touch, Top Up and Motor Mania become entries of that list), the eligibility criteria (product, period, branch, marketing unit, minimum premium and the other SLA conditions), the structure (a rate with its multiplier, or a fixed amount or configured value per policy or invoice), the beneficiary and the effective dates."
  - "A run INR-yyyy-n evaluates each invoice of the period against the programme: the invoice must meet every eligibility criterion, be fully paid and be paid within the credit term of the SLA; the incentive is the SLA rate times the multiplier, or the fixed amount, for that invoice. An ineligible invoice gets no incentive. Production volume or a production target is never a criterion. Entries excluded by the exclusion rules (negative amount, fully cancelled, FR-OP-091) are left out and listed with their rule."
  - "Every computed incentive is logged and listed per invoice with the programme, criteria met, rate or amount and result, for review and audit. Posting (COMMREC_APPROVE) publishes OPS_INCENTIVE_ACCRUE per insurer. The pass-on of an incentive to branches (OPS_INCENTIVE_PASS_ON with a PASS_ON request to Disbursement) is no longer stated in the BRD and is kept only if BDOI confirms it (CLR-OP-58)."
preconditions:
  - The scheme has tiers.
main_flow:
  - The TL maintains the scheme and its tiers.
  - The TL computes a run for a period and reviews the lines and exclusions.
  - The TL posts the run.
alternate_flows:
  - Cancel a computed run.
rules:
  - [R1, "Programmes are inactive until BDOI gives the SLA rates, amounts, multipliers, credit terms and criteria (OQ39, CLR-OP-58).", Configurable, Incentive Schemes]
  - [R2, "Exclusion rules Negative amount and Fully cancelled (the v2.0 rule erroneous booking is renamed Fully cancelled; CLR-OP-56).", Configurable, LOV INCENTIVE_EXCLUSION_RULE]
  - [R3, "Eligibility needs fully paid status and payment within the credit term; no production target.", Fixed, "-"]
  - [R4, "Programme types are a configurable list; no programme is built into the system.", Configurable, Incentive Schemes]
validations:
  - [Scheme without tiers, "Scheme <name> has no tiers yet: add its targets and amounts first", INCENTIVE_SCHEME_EMPTY]
  - [Tier incomplete (tiered), Each tier needs a production target and a rate above zero, INCENTIVE_TIER_INVALID]
  - [Tier incomplete (fixed), Each tier needs a minimum basic premium and a fixed amount above zero, INCENTIVE_TIER_INVALID]
  - [Scheme dates, The scheme ends before it starts, INCENTIVE_SCHEME_DATES]
  - [Run period, The period ends before it starts, INCENTIVE_RUN_PERIOD]
  - [Run earned nothing, "Run <no> earned no incentive to post", INCENTIVE_RUN_NOTHING]
  - [Run already posted or cancelled, "Run <no> is <status> and cannot change", INCENTIVE_RUN_STATE]
fields_screen: Incentive Scheme
fields:
  - [Code / Name, Text, "Yes", "-", Unique code]
  - [Scheme Type, List, "Yes", Programme types (configurable list), "-"]
  - [Calculation, List, "Yes", Rate with multiplier / fixed amount per policy or invoice, "-"]
  - [Period, List, "Yes", "Monthly, quarterly, half-year, yearly", "-"]
  - [Beneficiary, List, "Yes", BDOI / Branch, "-"]
  - [Insurer Code, Look-up, "No", Insurer master, "-"]
  - [Segments / Product Lines, Multi-select, "No", Segments / product lines, "-"]
  - [Branches / Marketing units, Multi-select, "No", Branches / units, "-"]
  - [Credit term (days), Number, "No", SLA, "Paid within the term"]
  - [Effective From / To, Date, "Yes / No", "-", To >= From]
notifications:
  - INCENTIVE_EXCLUSION alert when lines are excluded.
audit:
  - Scheme changes, runs, exclusions and postings are recorded.
acceptance:
  - A programme paying a fixed 1,000.00 per motor policy of basic premium 50,000.00 or more, for branch CEB, pays 1,000.00 for a fully paid CEB policy of 60,000.00 paid within the credit term, and nothing for the same policy of another branch.
  - An invoice that meets the criteria but was paid after the credit term, or is not fully paid, gets no incentive.
  - A fully cancelled invoice is excluded from a run and listed with the rule Fully cancelled.
  - The run lists every computed incentive per invoice with its programme and rate or amount.
```

> [!NOTE] Changed in v2.1
> CMRID.005 changes from production-target incentives for Retail and Corporate (No Touch, Top Up, rates and multipliers from June to December) to per-invoice incentives under each insurer's SLA, and CMRID.006 replaces the Motor Mania plan by configurable programmes with criteria by product, period, branch and marketing unit; "incentive based on production volume or target" is a negative scenario. Version 2.0 applied the rate of the highest production target reached; in v2.1 the programme types are a list, eligibility needs fully paid status and payment within the credit term, and the branch pass-on is kept only on BDOI's confirmation (CLR-OP-58).

```fr
id: FR-OP-096
title: Submit BIR certificates to Comptrollership
brd: [CMRID.015 (p.176; annex p.61-62), CMRID.010 (p.174; annex p.58)]
actor: Commission Handler (submit); Comptrollership (acknowledge)
priority: Must have
screens: BIR Certificates; BIR Certificate (workflow panel, ORs, scanned copy)
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
brd: [RMTID.037 (p.131; annex p.16), CMRID.014 (p.175-176; annex p.61)]
actor: Production Reconciliation Handler (estimated items); Commission Handler (yearly production)
priority: Must have
screens: Estimated Items; Report CMR-PRODUCTION-YEARLY
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

> [!NOTE] Changed in v2.1
> The Remittance annex of May 2026 states that RMTID.037 (track estimated items in production reports) will be moved to Production Reconciliation. The estimated items are proposed to be flagged by the Production Reconciliation Handler, who works the production register where they show; the yearly production report stays with Commission Receivables (CMRID.014). The definition of an estimated item is still open (OQ27, CLR-OP-47).

```fr
id: FR-OP-098
title: Classify and report commission receivables
brd: [CMRID.004 (p.171; annex p.50-51)]
actor: Commission Handler; TL
priority: Must have
screens: Reports (category Operations)
description:
  - "Every commission receivable output (report, billing extract, Statement of Account) classifies each entry at invoice level from the system tagging and source data, without comparing or reclassifying: Direct Billed (Regular) or Direct Billed (Priority) for direct payment / direct bill entries, and Regular Commission for the non-direct bill accounts (CMRID.004 as revised by the Commission Receivables annex; the categories and the source of the Priority tag are confirmed through CLR-OP-57). The classification is the same in every output."
  - "CMR-COMMISSION-RECEIVABLE shows per insurer and invoice the commission amount, the classification, the remittance status, the net commission after partial remittances and the remaining commission balance. It is produced on demand or on a schedule and reflects the recorded values without recomputing them. The other Commission reports are CMR-DP-STATUS, CMR-INCENTIVE, CMR-FEEDBACK-SLA, CMR-BIR-CERT and the ageing of FR-OP-099 (section 6.5)."
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
  - A direct bill invoice tagged Priority shows Direct Billed (Priority) on the report and on the Statement of Account of its insurer.
  - After a partial remittance the report shows the net commission realised and the remaining balance of the invoice.
```

```fr
id: FR-OP-099
title: Age commission receivables by invoice
brd: [CMRID.011 (p.174; annex p.58-59)]
actor: Commission Handler; Commission TL
priority: Must have
screens: DP Billings (Ageing); Reports (CMR-AGEING, proposed)
description:
  - "BIBS ages the outstanding commission receivable of each billed invoice from its reference date: the billing date, or the Statement of Account date when the receivable was billed through a statement (CMRID.011 as revised by the Commission Receivables annex). The ageing is shown per invoice with the outstanding amount, the age in days and the ageing bucket (proposed 0-30, 31-60, 61-90, 91-180 and over 180 days, OQ43, CLR-OP-61), and per insurer as totals by bucket."
  - "The ageing is kept month by month, so the user can see how the receivable of an invoice aged over time. It drives the follow-up: the handler sorts and filters by age, insurer and branch to prioritise the collection. A collected or written-off receivable leaves the ageing on its collection date."
preconditions:
  - The user has COMMREC_PROCESS or OPS_REPORT_VIEW; the invoice is billed.
main_flow:
  - The handler opens the ageing view or runs the report for a date.
  - BIBS lists the outstanding receivables per invoice with reference date, age and bucket, and the totals per insurer.
  - The handler follows up the oldest receivables.
alternate_flows:
  - Partial collection. The outstanding balance after the collection is aged from the same reference date.
rules:
  - [R1, "Reference date: billing date, or Statement of Account date (CLR-OP-61).", Fixed, "-"]
  - [R2, "Ageing buckets (proposed 0-30, 31-60, 61-90, 91-180, over 180 days).", Configurable, Ageing buckets (OQ43)]
validations: []
notifications:
  - "None."
audit:
  - Each monthly ageing position is kept; report runs are archived.
acceptance:
  - An invoice billed on 01-Aug-2026 and not collected shows an age of 68 days and the bucket 61-90 on 08-Oct-2026.
  - After a partial collection the remaining balance keeps the age counted from the billing date.
  - The ageing of a collected invoice stops on its collection date and the invoice leaves the outstanding list.
```

> [!NOTE] Changed in v2.1
> CMRID.011 changes from a feedback timeline (flag delays beyond 10 working days) to invoice-level ageing of commission receivables from the billing or Statement of Account date; "timeline tracking instead of ageing logic" is a negative scenario of the annex. FR-OP-099 is added for the ageing; the 10-working-day follow-up of FR-OP-092 and FR-OP-093 is kept only as a reminder, subject to CLR-OP-61.

## Marketing Collection items

The MKTID requirements are activities of Marketing (Marketing Collection HO and branches, Marketing TL / UH) that feed Operations. BRD-4 Collections confirmed that MKTID.001-009 and MKTID.011 stay in Operations as described here; the 2307 and DP tagging (MKTID.010, 012, 013) are also described by the Collections dispositions (OQ45). BRD v1.01 makes Marketing Collection the owner of the whole PR 2307 reversal (MKTID.013), and the Adjustment annex of May 2026 renumbers six endorsement requirements as Marketing activities (MKTID.014 to MKTID.019).

<!-- table: widths=2.4,7,4,3.2 caption="Marketing items and where they are specified" size=8.5 -->
| BRD ID | Activity | FR | Status after BRD-4 |
|---|---|---|---|
| MKTID.001 | Request to send the remittance schedule to the insurer | FR-OP-110 | Stays in Operations |
| MKTID.002-007 | Hold requests: tag, remove, assign, create / cancel / extend, approve, reference numbers | FR-OP-111 | Stays in Operations |
| MKTID.008 | Endorsement slip for external endorsements (same text as MKTID.018, CLR-OP-51) | FR-OP-114 | Stays in Operations |
| MKTID.009 | Special remittance request | FR-OP-112 | Stays in Operations |
| MKTID.010, 013 | PR 2307 reversal: tagging, validation of the amounts and the CRU, report, posting and routing to Disbursement | FR-OP-113 | Marketing Collection in Operations; same flow as the Collections disposition "PR 2307 for reversal" |
| MKTID.011 | DP tag at quotation and policy | FR-OP-004 | Stays (booking flag) |
| MKTID.012 | DP premium receivable reversal after valid tagging | FR-OP-094 | Collections disposition "DP PR for reversal" |
| MKTID.014 (was ADJID.002) | Types of financial endorsement | FR-OP-050 | Marketing raises the request |
| MKTID.015 (was ADJID.004) | Types of non-financial endorsement | FR-OP-050 | Marketing raises the request |
| MKTID.016 (was ADJID.007) | Correct and resubmit requests returned by Operations | FR-OP-053 | Marketing is the correction owner |
| MKTID.017 (was ADJID.010) | Extension of cover with additional premium, subject to approval | FR-OP-053, FR-OP-054 | Marketing raises the request |
| MKTID.018 (was ADJID.015) | Endorsement slip for external endorsements | FR-OP-060, FR-OP-114 | Marketing generates the slip |
| MKTID.019 (was ADJID.025) | Supporting documents of endorsement requests | FR-OP-052 | Marketing attaches the documents |

```fr
id: FR-OP-110
title: Send the remittance schedule to the insurer
brd: [MKTID.001 (p.133-134)]
actor: Remittance Processor on the request of Marketing
priority: Must have
screens: Remittance Batch (Send via Email)
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
brd: [MKTID.002 (p.134), MKTID.003 (p.134-135), MKTID.004 (p.135), MKTID.005 (p.135-136), MKTID.006 (p.136), MKTID.007 (p.136-137), RMTID.021 (p.124)]
actor: Marketing Collection (request); Marketing TL / UH (approve); System (expiry)
priority: Must have
screens: Remittance Holds; Remittance Hold (workflow panel)
description:
  - Marketing raises a hold request HLD-yyyy-n on an invoice with a reason, hold-until date and remarks, singly or by file (invoiceNo, reasonCode, holdUntil, remarks). One live request per invoice. Submission makes the invoice REQUESTED_FOR_HOLD; approval by another user sets the HOLD flag, which excludes the invoice from extraction (FR-OP-031). The approver assigns the hold to an active remittance processor, who is notified.
  - An active hold is extended (new hold-until date, with approval), cancelled (with approval) or released by Marketing; release makes the invoice eligible again. the scheduled run (daily 08:15 PHT) releases holds whose date has passed and notifies holds that reach their date the next day. Holds from Collection arrive through the feed COLLECTION_HOLD.
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
brd: [MKTID.009 (p.137)]
actor: Marketing Collection; Marketing TL
priority: Must have
screens: Special Remittance (New request, upload)
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
title: Tag, validate and post PR 2307 reversals and route the certificates to Disbursement
brd: [MKTID.013 (p.107-108), MKTID.010 (p.137-138; annex p.62-63)]
actor: Marketing Collection (tag, validate, post and route); System (computations and checks)
priority: Must have
screens: BIR 2307 (Tag 2307; tags, reports and routing); Invoice 360 (PR 2307, history)
description:
  - "Marketing Collection owns the whole PR 2307 reversal (MKTID.013 of BRD v1.01; the Cashiering steps of CSHID.026 and 027 are deleted). Marketing tags the reversal of an invoice with the BIR 2307 certificate received from the client: certificate number and period and the certificate amount, singly or by file. BIBS generates and stores the reference CWT-yyyy-n at tagging."
  - "On tagging BIBS computes the allowable PR 2307 amount of the invoice from the withholding tax rules and the invoice details (by default 2% of the premium subject to creditable withholding tax, CLR-OP-43), retrieves the current outstanding premium receivable of the invoice and validates that the certificate amount exceeds neither the computed PR 2307 amount nor the outstanding PR balance. It then checks whether an outstanding commission receivable (CRU) exists on the same invoice, account or transaction (CLR-OP-42) and shows the validated reversal details: PR 2307 amount, PR balance and CRU status."
  - "Marketing groups validated tags of one insurer, checks the batch against the BIR 2307 / CWT certificates received and attaches the supporting documents (the certificates, CWT copies). **Validate and Post** generates the BIR 2307 transaction report (printable, per insurer), posts the reversal and zeroes the premium receivable of each invoice (proposed entry Dr DTIP / Cr PR, CLR-OP-41), and **Route to Disbursement** sends the report with the certificates sorted per insurer directly to Disbursement as a CWT2307 request, without going through Remittance."
  - "When an outstanding CRU exists, the reversal and the release continue, the tag is flagged For Commission Receivable and is listed for the Commission Receivables team, so the commission recovery is not missed; the PR is zeroed subject to that handling. When the payment of the invoice was already remitted to the insurer, the reversed amount goes to Unapplied Payments (origin Other) and the insurer's written confirmation that the excess will be refunded is a mandatory attachment (MKTID.010); it is not routed to Adjustment and no reinstatement request is raised from the tag."
  - "Every reversal is traceable to the invoice, the AR / OR that paid it, the BIR 2307 record (certificate, report and batch) and the CRU, on the tag, on Invoice 360 and in the history."
preconditions:
  - The user has the 2307 tagging and processing permissions of Marketing Collection; the invoice has a 2% CWT portion outstanding.
main_flow:
  - Marketing enters the invoice, certificate number, period and amount; BIBS issues the reference CWT-yyyy-n.
  - BIBS computes the allowable PR 2307 amount, reads the outstanding PR, validates the certificate amount and checks for an outstanding CRU.
  - Marketing selects validated tags of one insurer, checks them against the certificates and attaches the supporting documents.
  - Marketing clicks **Validate and Post**; BIBS generates the BIR 2307 transaction report, posts the reversal and zeroes the PR.
  - Marketing clicks **Route to Disbursement**; the report and the sorted certificates reach the Disbursement queue (FR-OP-120, FR-OP-121).
alternate_flows:
  - Certificate amount above the computed PR 2307 amount or above the outstanding PR. BIBS refuses the tag and shows both amounts.
  - Outstanding CRU. BIBS continues and flags the tag For Commission Receivable; the Commission Receivables team sees it (FR-OP-094).
  - Payment already remitted. The reversed amount goes to Unapplied Payments with the insurer's written confirmation attached.
  - Marketing cancels a tag before it is posted, with a reason.
  - A tag that fails a check stays with Marketing for correction; nothing is posted.
rules:
  - [R1, "One tag in process per invoice.", Fixed, "-"]
  - [R2, "A 2307 report and batch hold the certificates of one insurer.", Fixed, "-"]
  - [R3, "Certificate amount <= computed PR 2307 amount and <= outstanding PR balance.", Fixed, "-"]
  - [R4, "Withholding tax rate and base of the PR 2307 computation (default 2% of the premium subject to withholding).", Configurable, "Withholding tax rules (Comptrollership), CLR-OP-43"]
  - [R5, "Accounting entry of the reversal: proposed Dr DTIP / Cr PR at posting and no entry at release (CLR-OP-41).", Configurable, Accounting rules]
  - [R6, "A payment already remitted goes to Unapplied Payments only with the insurer's written confirmation of refund.", Fixed, "-"]
validations:
  - [Unknown invoice, "Unknown invoice <no>", CWT_INVOICE_UNKNOWN]
  - [Tag already in process, "Invoice <no> already has a 2307 tag in process", CWT_ALREADY_TAGGED]
  - [No 2% outstanding, "Invoice <no> has no 2% CWT portion outstanding", CWT_AMOUNT]
  - [Certificate path without number, A certificate tag needs the BIR 2307 certificate number, CWT_CERTIFICATE_REQUIRED]
  - [Certificate amount above the computed PR 2307 amount or the outstanding PR, "<ref>: the certificate amount <amount> is above the allowable PR 2307 amount <amount> or the outstanding premium receivable <amount>", "-"]
  - [Tags of several insurers, A 2307 batch holds the certificates of one insurer, CWT_BATCH_ONE_INSURER]
  - [No selection, Select the 2307 tags to validate, CWT_BATCH_EMPTY]
  - [Remitted payment without the insurer's confirmation, "Attach the insurer's written confirmation that the excess will be refunded", "-"]
  - [Batch already routed, "<batch> is <status>", CWT_BATCH_ROUTED]
fields_screen: Tag 2307
fields:
  - [Invoice No., Look-up, "Yes", Invoice ledger, 2% CWT outstanding]
  - [Certificate No., Text, "Yes", "-", "-"]
  - [Period From / Period To, Date, "Yes", "-", To >= From]
  - [Amount, Amount, "Yes", "-", "> 0, <= allowable PR 2307 amount and <= outstanding PR"]
  - [Supporting documents, File, Cond., "-", "BIR 2307 / CWT copies; insurer's written confirmation when the payment was remitted"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - Disbursement queue entry when the report is routed; the Commission Receivables team when a tag is flagged For Commission Receivable.
audit:
  - Each tag, check result (PR 2307 amount, PR balance, CRU), validation, posting, routing and cancellation is kept with user, time and reason.
acceptance:
  - A certificate tag receives a reference CWT-2026-n and shows the computed PR 2307 amount, the outstanding PR and the CRU status.
  - A certificate amount above the computed PR 2307 amount is refused; so is an amount above the outstanding PR.
  - Validating and posting two tags of one insurer generates one BIR 2307 transaction report, zeroes the PR of both invoices and routes the report directly to Disbursement.
  - A batch with tags of two insurers is refused.
  - A tag on an invoice with an outstanding commission receivable is posted and released, and is flagged For Commission Receivable.
  - A tag on an invoice whose payment was already remitted cannot be posted without the insurer's written confirmation; once posted, the amount is in Unapplied Payments.
  - A second tag on the same invoice is refused while the first is in process.
```

> [!NOTE] Changed in v2.1
> MKTID.013 of BRD v1.01 makes Marketing Collection own the whole PR 2307 reversal and adds the computed PR 2307 ceiling, the outstanding PR check, the CRU check, the batch validation against the certificates, the transaction report, its posting and the routing to Disbursement; MKTID.010 drops the reinstatement request and the routing to Adjustment and sends an already remitted payment to Unapplied Payments. Version 2.0 had the Cashier validate, post and route the batch (FR-OP-026, withdrawn). The screens shown in chapter 13 are those reviewed with v2.0, where these steps carry the Cashier's role; their owner moves to Marketing Collection with the proposed rules CLR-OP-40 to CLR-OP-43. BRD-4 Collections describes the same reversal as the disposition "PR 2307 for reversal" (R9); both sets follow MKTID.013.

```fr
id: FR-OP-114
title: Generate the endorsement slip for Marketing
brd: [MKTID.008 (p.177), MKTID.018 (annex p.37)]
actor: Marketing Collection / TL (ADJ_REQUEST)
priority: Must have
screens: Endorsement Request page (Endorsement Slip)
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

Until the Disbursement module of BRD-5 is delivered, Operations sends its payment requests to an in-app Disbursement queue.

```fr
id: FR-OP-120
title: Work payment requests in the Disbursement queue
brd: [DBMID.001 (p.109), RMTID.034 (p.130)]
actor: Disbursement (DISB_PROCESS)
priority: Must have
screens: Disbursement Queue
description:
  - "Payment requests DSQ-yyyy-n of type REMITTANCE (remittance batches), REFUND (unapplied refunds), CWT2307 (BIR 2307 reports and certificates of Marketing Collection) and PASS_ON (incentive pass-on) arrive with their payee, amount, source and documents. Disbursement acknowledges a request, enters the DV number, marks it paid, or returns it with a reason. Each status (SENT, ACKNOWLEDGED, DV_ASSIGNED, PAID, RETURNED, CANCELLED) is sent back to the source module (Disbursement status event): Remittance updates the invoices (FR-OP-036), Cashiering completes the refund. A CWT2307 request comes from Marketing Collection (FR-OP-113) and is released, not paid (FR-OP-121)."
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
title: Receive and release BIR 2307 certificates to insurers
brd: [DBMID.001 (p.109)]
actor: Disbursement
priority: Must have
screens: Disbursement Queue; BIR 2307 (reports and routing)
description: "Disbursement receives from Marketing Collection the processed BIR 2307 transaction report of one insurer as a CWT2307 request, with the certificates sorted per insurer and the supporting documents attached (FR-OP-113). Disbursement checks that the certificates are sorted and complete and the documents attached, and releases them to the insurer (**Release to Insurer**). Disbursement posts no accounting entry; the reversal was posted by Marketing Collection (CLR-OP-41)."
preconditions:
  - The 2307 report is WITH_DISBURSEMENT.
main_flow:
  - Disbursement opens the CWT2307 request in the queue and acknowledges it.
  - Disbursement checks the report, the certificates and the attachments.
  - Disbursement releases the certificates to the insurer; the report moves to RELEASED.
alternate_flows:
  - Certificates incomplete or a document missing. Disbursement returns the request to Marketing Collection with a reason.
rules:
  - [R1, "One report per insurer.", Fixed, "-"]
  - [R2, "No posting on release (proposed, CLR-OP-41).", Fixed, "-"]
validations:
  - [Report already routed or released, "<batch> is <status>", CWT_BATCH_ROUTED]
notifications:
  - OPS_DISBURSEMENT_STATUS to Marketing Collection.
audit:
  - Receipt, return and release are in the report history with user and time.
acceptance:
  - A routed 2307 report of one insurer appears in the Disbursement queue with its certificates and attachments.
  - Releasing it to the insurer changes no journal and sets the report to RELEASED.
  - A request returned for a missing certificate goes back to Marketing Collection with the reason.
```

> [!NOTE] Changed in v2.1
> DBMID.001 of BRD v1.01 receives the sorted certificates and supporting documents from Marketing Collection instead of Cashiering, and drops the Disbursement accounting entries and the check that the DTIP is reversed: Disbursement only releases the certificates. Version 2.0 posted Dr DTIP / Cr PR2307 on release; that entry is withdrawn with the proposed entry of CLR-OP-41.

> [!NOTE] Superseded by BRD-5
> The Disbursement module of BRD-5 implements the same gateway (OQ02 answered); the in-app queue is the default until it is delivered.

## Interfaces and flow-in

```fr
id: FR-OP-130
title: Integrate with other systems through ports
brd: [BRQID.004 (p.81)]
actor: System
priority: Must have
screens: Interfaces; Hand-offs and Extracts
description:
  - "Operations exchanges data with the systems it depends on: the Collection feeds (2307 tags, commission payments, holds, special remittances, DP lists, returned DP accounts, refunds), the Disbursement requests and statuses, the insurer files, the shared drive, and the Marketing and Claims feeds. Until each system is specified, the exchange is a manual upload, the in-app Disbursement queue or the list of extracts. Section 7 lists each interface and its scope."
  - Work that an upload or the queue cannot complete alone (an OR or an unapplied item requested while Cashiering is not in use, a quotation for a TSI increase) becomes an open hand-off for the responsible team on Hand-offs and Extracts; the team closes it with what was done.
preconditions:
  - "None."
main_flow:
  - A module sends or receives an exchange.
  - BIBS sends or receives the data (upload, queue, list of extracts).
  - Failures are recorded and alerted (FR-OP-131).
rules:
  - [R1, "A new transfer to another system does not change the Operations screens.", Fixed, "-"]
validations:
  - [Hand-off already closed, The hand-off is already closed, HANDOFF_CLOSED]
notifications:
  - Alert Interface run failed (NT-05) on failures.
audit:
  - Every exchange is a flow-in run with its records, or a queue entry.
acceptance:
  - A remittance approval creates a request in the Disbursement queue.
  - An extract for the shared drive is stored in the list of extracts and downloadable.
```

> [!NOTE] Superseded by BRD-4 and BRD-5
> BRQID.004 names Collection, Accounting, Disbursement, Marketing and Claims. BRD-4 makes Collections a BIBS module that provides the Collection data in the system; BRD-5 keeps the GL in BIBS and provides the Disbursement requests and statuses; Claims (BRD-7) provides the claims data. The Marketing feed is on hold (OQ45).

```fr
id: FR-OP-131
title: Fetch data through flow-in feeds with runs, logs and alerts
brd: [BRQID.005 (p.81)]
actor: System Administrator (FLOWIN_MANAGE); System
priority: Must have
screens: Interfaces (feeds, runs, records, upload)
description:
  - Each feed (section 7) has a partner system, direction, transport, schedule and active flag. Each run FIR-yyyy-n records the trigger, start and end, records read, accepted and failed, status and errors. Each record is accepted once by its reference and content, so a file uploaded twice creates no duplicates. A failed run or failed records raise the alert Interface run failed.
  - The administrator activates a feed, sets its schedule and uploads files for feeds with a handler, and reads the runs and records.
preconditions:
  - The user has FLOWIN_MANAGE.
main_flow:
  - The administrator uploads a file to a feed, or the schedule starts it.
  - BIBS validates and maps each record through the module's handler.
  - BIBS records the run and its records and alerts on failure.
rules:
  - [R1, "A record is accepted only once.", Fixed, "-"]
  - [R2, "Schedules per feed (time and frequency, UTC).", Configurable, Interfaces]
validations:
  - [Feed inactive, "Feed <code> is inactive", FLOW_IN_FEED_INACTIVE]
  - [Feed without handler, "No module processes uploads of feed <code> yet", FLOW_IN_NO_HANDLER]
  - [Record failed, "<message>", FLOW_IN_RECORD_FAILED]
fields_screen: Interfaces (feed settings)
fields:
  - ["Schedule", Text, "No", "-", "Valid schedule (time and frequency, UTC)"]
  - [Active, Check box, "Yes", "-", "-"]
  - [File, File, Cond., "-", For an upload]
notifications:
  - Alert Interface run failed (NT-05, in-app and e-mail) to the administrators.
audit:
  - Runs and records are kept with their outcome.
acceptance:
  - Uploading a hold file with one invalid row records a run with one failed record and raises the alert Interface run failed.
  - Uploading the same hold file again accepts no new record.
```

```fr
id: FR-OP-132
title: Confirm payments to the New Business payment gate
brd: [CSHID.020 (p.103-104)]
actor: System
priority: Must have
screens: New Business placement (payment gate); Pre-booked Payments
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
| OPS_CWT_2307 | PR 2307 reversal tag | TAGGED, VALIDATING, REPORT_POSTED, WITH_DISBURSEMENT, RELEASED / CANCELLED (proposed for v2.1, CLR-OP-40) | FR-OP-113, 121 |
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
| OPS_CWT_2307 | VALIDATING, REPORT_POSTED | Marketing Collection (proposed, CLR-OP-40) | 24 |
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

![Workflow OPS_CWT_2307 as proposed for v2.1 (MKTID.010, 013; DBMID.001)](figures/brd02_wf_cwt2307.dot){width=15}

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

Every Operations posting is a business event; Comptrollership configures the GL rule of each event (maker-checker), and no GL account is fixed in the system (OQ07). The entries below are the proposed defaults with the seed chart of accounts. All events carry the party, cost centre and business line, and are priced at the BOOK rate when not in pesos.

<!-- table: widths=5.6,3.8,7.2 caption="Operations accounting events and default entries" size=8 -->
| Event | Transaction | Default entry |
|---|---|---|
| OPS_AR_RECEIPT | AR issued (premium) | Dr bank / cash on hand / Cr unapplied collections (client) |
| OPS_PAYMENT_APPLY | Payment applied per component | Dr unapplied collections / Cr PR by component; commission realised on collection: Dr unrealised commission / Cr commission income, Dr deferred output VAT / Cr output VAT |
| OPS_CWT_REVERSAL (proposed, CLR-OP-41) | PR 2307 reversal posted by Marketing Collection | Dr DTIP / Cr PR by component; no entry when Disbursement releases the certificates |
| OPS_EXCESS_TO_OVERAGES | Minimal excess (<= threshold, default 10.00) cleared when the payment is processed | Dr unapplied collections / Cr AP overages |
| OPS_MINIMAL_BALANCE_REVERSAL | Minimal PR balance (<= threshold, default 10.00) cleared when the payment is processed | Dr minimal balance / write-off / Cr PR by component |
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
| OPS_INCENTIVE_ACCRUE | SLA incentive earned per invoice | Dr incentive receivable / Cr incentive income |
| OPS_INCENTIVE_PASS_ON | Pass-on of a programme to branches, when BDOI keeps it (CLR-OP-58) | Dr incentive income / Cr due to branches, then Disbursement |

# Reports and documents

All Operations reports are in the report category Operations, need OPS_REPORT_VIEW to view and OPS_REPORT_EXPORT to download or print, export to PDF, XLSX and CSV, and are archived when run (FR-OP-009). "Draft" marks a layout the BRD does not give; BDOI confirms it through OQ42.

## Cashiering reports

<!-- table: widths=4.4,5.6,6.6 caption="Cashiering reports (Annex II of the original BRD, p.314-316)" size=8 -->
| Code | Name | Content |
|---|---|---|
| CSH-APPLIED-PREM | Applied Premium Report | AR no., date, amount paid, invoice no., payor / client, risk code, count, total (Annex II #1) |
| CSH-APPLIED-COMM | Applied Commission Report | OR no., invoice no., applied invoices, insurer, basic commission, WTAX, EVAT, net commission, total applied (#2) |
| CSH-PDC-WAREHOUSE | Post-dated Checks Warehousing | AR date, AR no., maturity date, client, amount, bank code, branch, check no., market segment (#3) |
| CSH-MINBAL-EXCESS | Minimal Balance of Unapplied Payments (Excess Payments) | AR no., AR date, market unit, section unit, minimal amount, count, total (#4) |
| CSH-CANCELLED-OR | Cancelled Official Receipts | Date issued, OR no., assured, gross, VAT, WTAX, amount, total (#5) |
| CSH-CANCELLED-AR | Cancelled Acknowledgment Receipts | Date issued, reason, AR no., assured, gross, VAT, WTAX, amount, total (#6) |
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
| CSH-2307-TXN | BIR 2307 Transaction Report | Tags of a 2307 report per insurer with the certificate amount, computed PR 2307 amount, PR balance, CRU status and certificates; generated, posted and routed by Marketing Collection (MKTID.013) |

## Remittance reports

<!-- table: widths=4.4,5.6,6.6 caption="Remittance reports (Annex III of the original BRD, p.316-318)" size=8 -->
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

<!-- table: widths=4.4,5.6,6.6 caption="Production Reconciliation reports (Annex IV of the original BRD, p.318-319)" size=8 -->
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
| PRC-EARLY-INCENTIVE | Early Incentive Check | Account, rule, rate, window, remittance date, result (RMTID.041, previously PRCID.028) |

## Commission Receivables reports

<!-- table: widths=4.4,5.6,6.6 caption="Commission Receivables reports" size=8 -->
| Code | Name | Content |
|---|---|---|
| CMR-COMMISSION-RECEIVABLE | Commission Receivable - Direct Payment vs Regular | Per insurer and invoice: commission, classification Direct Billed (Regular), Direct Billed (Priority) or Regular Commission, remittance status, net after partial remittances, remaining balance (CMRID.004) |
| CMR-PRODUCTION-YEARLY | Production per Branch and Insurer - Yearly | Total production and commission per branch and insurer, estimated items (CMRID.014) |
| CMR-DP-STATUS | Direct Payment Accounts per Status | DP accounts by status (Valid, Invalid, Returned), with the eligibility basis and the exclusion rule (CMRID.003, 008, 013) |
| CMR-INCENTIVE | Incentive Runs | Runs ending in the period with the incentive per invoice and exclusions (CMRID.005, 006) |
| CMR-FEEDBACK-SLA | Insurer Feedback Timeline | Billings with sent date, follow-up date and answer date (CMRID.009, 012) |
| CMR-AGEING (proposed) | Commission Receivable Ageing | Outstanding commission receivable per invoice with billing or SOA date, age, bucket, and totals per insurer and branch (CMRID.011, FR-OP-099) |
| CMR-SOA (proposed) | Statement of Account per insurer | Invoices billed with commission, VAT, withholding tax, net, collected and balance (CMRID.007, FR-OP-092) |
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
| BIR 2307 transaction report | PDF | Tags of a report per insurer, generated and posted by Marketing Collection, with the certificates attached |
| Statement of Account (proposed) | PDF and XLSX | Commission receivables of an insurer (CLR-OP-59) |

# Interfaces and integration

Figure 12 shows the interfaces of Operations. The modules exchange data only through the invoice ledger and its events; until an external system is specified, the exchange with it is an upload, the in-app Disbursement queue or the list of extracts.

![Interfaces of Operations (dashed = on hold or replaced by a later BRD)](figures/brd02_integration.dot)

<!-- table: widths=4.4,1.8,5.6,2.6,2.6 caption="Interfaces and flow-in feeds" status=Scope size=8 -->
| Interface / feed | Direction | Content and trigger | BRD | Scope |
|---|---|---|---|---|
| OPS_INVOICE_FEED (booking) | In | Booked invoices, endorsements and returns after commit; replay | BRQID.007 | IN SCOPE |
| Booking posting and service invoice | Out | Endorsement and cancellation posting; service invoice issue and credit | ADJID.011, 014 | IN SCOPE |
| Catalogue | In | Endorsement rating, commission rates, package limits | ADJID.008, 014 | IN SCOPE |
| Placement payment gate | Out | Applications and pre-booked payments per ARN | CSHID.020 | IN SCOPE |
| Accounting engine | Out | Business events of section 5.3 | CSHID.012-014; ADJID.011 | IN SCOPE |
| Outgoing e-mail | Out | Protected register, schedule, billing; passwords separately | PRCID.007, 008; MKTID.001; CMRID.009 | IN SCOPE |
| INSURER_REMIT_OR | In (upload) | Insurer OR schedules | RMTID.012, 013 | IN SCOPE |
| INSURER_PRODUCTION | In (upload) | Insurer production reports | PRCID.009, 022 | IN SCOPE |
| INSURER_DP_RESPONSE | In (upload) | Insurer answers to DP billings | CMRID.009 | IN SCOPE |
| COLLECTION_CHECK_PICKUP | In (upload) | Checks for pick-up (removed from scope with CSHID.009) | - | OUT |
| COLLECTION_CWT2307 | In (upload) | BIR 2307 tags of Marketing Collection | MKTID.013 | SUPERSEDED |
| COLLECTION_COMMISSION_PAYMENT | In (upload) | Commission payment details | CSHID.007 | IN SCOPE |
| COLLECTION_HOLD | In (upload) | Hold requests | RMTID.021; MKTID.003 | IN SCOPE |
| COLLECTION_SPECIAL_REMIT | In (upload) | Special remittance requests | RMTID.030; MKTID.009 | IN SCOPE |
| COLLECTION_DP_LIST | In (upload) | DP lists of HO and branches (fallback to the DP tagging, CLR-OP-54) | CMRID.001 | SUPERSEDED |
| COLLECTION_DP_RETURNED, COLLECTION_REFUND | Out | Rejected DP accounts; refunds | CMRID.009; CSHID.024 | IN SCOPE |
| DISBURSEMENT_REQUEST / _STATUS | Out / In | Payment requests; DV and status (in-app queue) | RMTID.034; DBMID.001 | SUPERSEDED |
| Shared drive | Out | Extract files | RMTID.001; PRCID.005 | ON HOLD |
| OBPCS, Old BOB, PMS, TFS (BDOI channels, Drop 0) | In (upload) | Bills payment, SOA funds-transfer, PDC and Trade payment files on the handlers PAY_BILLS, PAY_DIRECT_CREDIT, PAY_PDC, PAY_TRADE | CSHID.008 | IN SCOPE |
| Marketing and Claims feeds | In | Marketing data; claims for special remittance | BRQID.004; MKTID.009 | ON HOLD |

SUPERSEDED means an upload or queue in Operations that BRD-4 Collections or BRD-5 Accounting and Disbursement replaces. ON HOLD means the transfer waits for BDOI's specification (OQ17, OQ45, OQ46). OUT means removed from scope by the new BRD version.

> [!PARKED] Transfers on hold
> Insurer file transfer and portal channels, BDO bank file transports (FS01 / FS04) and the shared drive are not specified in the BRD (OQ03, OQ17, OQ22, OQ29). Adding a transfer later changes none of the Operations screens.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4 caption="Non-functional requirements (BRD v1.01 p.178-183)" size=8.5 -->
| Topic | BRD value | BIBS target and approach |
|---|---|---|
| Users | Cashiering HO 11 + branches 5, TL / TH 5 + 5; Remittance 4; Prod Recon 4; Adjustment 6; Commission 5 + 4 + 2 | Within the BRD-1 sizing (145 concurrent); Operations adds fewer than 50 users |
| Volumes and growth | Not stated | Assumed within the BRD-1 sizing; to confirm (OQ44) |
| Response time | Under 5 seconds for every function; section navigation under 5 s; batch print of 50 documents under 10 s | Online p95 under 3 seconds; extraction, matching, uploads and batch print run as background jobs with progress |
| Peak | 15th and 30th (Cashiering, Remittance); 1st-2nd week (Prod Recon); Q4 (Adjustment, Commission); 07:30-18:00 | Remittance extraction at 20:00 PHT and the minimal balance safety-net run at 04:00 PHT, outside the peak hours; the other jobs in section 9.3 |
| Availability | 99.9%; use 07:00-18:30 (07:30-18:00 Adjustment / Commission); maintenance per bank standard | Same deployment as BRD-1 (window 07:00-22:00 governs, OQ44) |
| Recovery | RTO 4 hours, RPO 24 hours | Platform backup and recovery; one BIBS-wide NFR set being agreed (XQ08) |
| Retention | Application, system and audit logs and history 5 years online, 15 years archive; backup every 4 hours kept 7 years | BRD-1 retention framework with backup retention 7 years |
| Anonymisation | No | None |
| Devices | Same performance on mobile and desktop | Responsive screens |
| Security | Authorised users only; protected files | Role-based access, four-eyes rules, protected e-mails (FR-OP-002, 013, 035) |
| Audit | All actions logged; the IT audit trail standard set as a condition of the sign-off of BRD v1.01 (p.67, p.186): user who started the activity, event type (create, edit, delete and so on), module affected, from and to values, origin of the event (IP address, MAC address, terminal ID or equivalent), date and time, success or failure indicator | Audit trail, workflow history and ledger movements that no user can change (FR-OP-004, 024). Every audit record of an Operations action holds the seven items of the IT standard; refused and failed actions are recorded with the failure indicator (CLR-OP-37) |

# Configuration items owned by the System Administrator

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,2.6,7.8 caption="Operations parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| REMIT_CHECK_HOLD_DAYS | 3 | Banking days a check is held before its payment is remitted (RMTID.017) |
| REMIT_PAIDAR_OVER_DTIP_MODE | EXCLUDE | EXCLUDE or CAP an invoice whose paid AR exceeds the DTIP (RMTID.014, OQ19) |
| REMIT_FILE_PATTERN | (blank) | File naming of remittance extracts (OQ17) |
| RECON_TOLERANCE | 1.00 | Reconciliation tolerance per amount field, in peso equivalent (PRCID.026) |
| RECON_MATCH_KEYS | INVOICE_NO,POLICY_NO | Keys pairing insurer lines with booked lines (OQ30) |
| PRODRECON_FILE_PATTERN | (blank = <INSURER>_PRODREG_<yyyyMM>_<seq>) | File naming of the production register (PRCID.004) |
| MIN_BALANCE_AUTO_MAX | 10.00 | Threshold of the minimal balances cleared when the payment is processed (CSHID.016) |
| MIN_BALANCE_FILE_RANGE | 10.00-100.00 | Range of the minimal balance file (ADJID.026) |
| CWT_APPLICATION_PERCENT | 98 | Share of premium applied for 2% CWT clients (CSHID.020) |
| CMR_FEEDBACK_WORKING_DAYS | 10 | Working days to the follow-up of the insurer's DP answer (CMRID.009; CLR-OP-61) |
| CMR_DP_COLLECTION_BANK | (blank) | Proposed bank account of DP commission collections |
| DP_PR_REVERSAL_POSTING | false | Post the GL entry of the DP PR reversal (MKTID.012) |
| ADJ_BASELINE_PERCENT | 100 | Over-adjustment baseline against the original premium and DTIP (ADJID.023, 028) |
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
| RECON_COMPANY_CONCERNED, RECON_DISPOSITION | Seed values until BDOI supplies them (OQ31) |
| ENDORSEMENT_TYPE | Financial: 8 types; non-financial: 5 types; internal adjustment (MKTID.014, 015) |
| ENDORSEMENT_REQUEST_TYPE | The 12 request types of Annex V |
| CANCELLATION_REASON | The 33 reasons of Annex V (shared with BRD-1) |
| ADJ_RETURN_REASON | Not qualified for posting; Incomplete supporting documents; Incorrect request details; Incorrect amounts; Duplicate request; Others |
| INCENTIVE_EXCLUSION_RULE | Negative amount; Fully cancelled (proposed name of the v2.0 value Erroneous bookings, CLR-OP-56); further rules from BDOI's annex of exclusion criteria |
| OPS_EXTERNAL_LINK | Empty; links (name and web address) added by the administrator |

## Jobs

<!-- table: widths=5,3.4,8.2 caption="Operations jobs (times in PHT)" size=8.5 -->
| Job | Default | Purpose |
|---|---|---|
| OPS_INVOICE_FEED_REPLAY | Manual | Replay the invoice ledger from booking |
| PREBOOKED_REMATCH | Every 2 hours | Apply pre-booked payments of accounts now booked |
| PAYMENT_AUTOMATCH | Hourly | Match unapplied payments again |
| PDC_MATURITY | 08:30 (00:30 UTC) | Turn matured PDCs into payments |
| MINIMAL_BALANCE_SWEEP | 04:00 (20:00 UTC) | Safety net: clears balances left within the threshold by later events; minimal balances of a payment are cleared when it is processed (FR-OP-023) |
| REMITTANCE_EXTRACTION | 20:00 | Scheduled remittance extraction |
| HOLD_EXPIRY | 08:15 | Release expired holds; notify holds expiring the next day |
| PRODUCTION_EXTRACT | 09:00 | Extract registers of insurers due |
| RECON_AUTOMATCH | Manual | Match open cycles again (OQ30) |
| ADJ_DAILY_REPORT | 18:00 | Archive the daily endorsement report |
| DP_FEEDBACK_SLA | 09:30 | Flag insurer answers past the follow-up date (CLR-OP-61) |

## Masters and rules maintained by the business

<!-- table: widths=5,4.6,7 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Receipt series | Cashiering TL (another CASH_SERIES_MANAGE holder) | FR-OP-010 |
| Payment file layouts, minimal balance rules | Cashiering TL | FR-OP-015, 023 |
| Disposition type rules (action, approval) | System Administrator (change request until OQ15) | FR-OP-022 |
| Early remittance incentive rules | Remittance TL | FR-OP-038 |
| Extract schedules | Recon Handler | FR-OP-070 |
| Incentive programmes (SLA criteria, rates, amounts, multipliers, credit terms) and exclusion rules | Commission TL | FR-OP-091, 095 |
| Accounting rules of the Operations events | Comptrollership (maker-checker) | FR-OP-027 |
| Flow-in feeds (schedule, active) | System Administrator | FR-OP-131 |
| Roles and permissions | Business Administrator via access request (BRD-11) | FR-OP-002 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-OP-01 | The BRD v1.01 (pp.71-185) is the baseline; the May 2026 annexes (pp.1-66) govern where they change a requirement, and the e-mail record (pp.67-70, 186-189) documents the PR 2307 decision | R1, R2, R2a |
| A-OP-02 | Every Operations function starts from the invoice booked by BRD-1; Operations does not book | BRQID.007 |
| A-OP-03 | GL accounts stay in BIBS; Comptrollership configures the rules of the Operations events | OQ07 (BRD-5) |
| A-OP-04 | Cancellations, reinstatements, refunds, reclasses and transfers need a TL approval | OQ06, OQ15 |
| A-OP-05 | Cashiering is the single payment intake; New Business placement consumes its result | OQ12 |
| A-OP-06 | Paid AR above DTIP is excluded from extraction until BDOI decides | OQ19 |
| A-OP-07 | Remittance batches are per lead insurer for co-insured invoices | OQ50 |
| A-OP-08 | The MKTID activities are performed in BIBS by Marketing users as described in section 4.7, including the PR 2307 reversal (MKTID.013) and the endorsement activities MKTID.014-019 | OQ45 |
| A-OP-09 | Where BRD v1.01 and a May 2026 annex differ, the annex governs; where an annex states a change that its own table does not show (PRCID.003, the duplicate MKTID, CMRID.001), the BRD v1.01 text is kept until BDOI confirms | CLR-OP-48, 51, 54 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-OP-01 | BDOI provides the bank and channel file layouts (FS01, FS04, CLPC, PDC) | FR-OP-015 (OQ03, OQ04) |
| D-OP-02 | BDOI provides the BIR ATP series and formats | FR-OP-010 (OQ05) |
| D-OP-03 | Comptrollership provides the GL accounts and the BOOK rate source | FR-OP-027 (OQ07, OQ08) |
| D-OP-04 | BRD-4 Collections delivers the in-app Collection feed; BRD-5 delivers the Disbursement module | Section 7 (OQ01, OQ02) |
| D-OP-05 | BDOI gives the SLA incentive parameters of each insurer (criteria, rates, amounts, multipliers, credit terms) and the early remittance rates and window | FR-OP-038, 095 (OQ23, OQ39) |
| D-OP-08 | BDOI gives the annex of exclusion criteria of CMRID.003 | FR-OP-091, 095 (CLR-OP-56) |
| D-OP-09 | BRD-4 Collections provides the DP tagging confirmation, the fully paid status and the PR 2307 disposition in the same flow as this FRS | FR-OP-090, 091, 113 (R9) |
| D-OP-06 | BDOI gives the report layouts not in the annex | Section 6 (OQ42) |
| D-OP-07 | The e-mail relay of the BIBS environment is available for insurer e-mails | FR-OP-073, 092, 110 |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-2 (status from R3 and the cross-BRD decisions, R7)" status=Status size=8 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| OQ01 | Systems of BRQID.004 (Collection, Accounting, Disbursement, Marketing, Claims): interface, data, frequency | FR-OP-130 | PARTIAL |
| OQ02 | Disbursement: request content, DV numbers and statuses | FR-OP-120 | ANSWERED |
| OQ03 / OQ04 | Payment file layouts; file encryption | FR-OP-015 | OPEN |
| IQ09 / IQ10 / IQ14 | PMS, OBPCS, Old BOB, AFTS and TFS: layouts, direction, frequency and transport (programme alignment) | FR-OP-015 | OPEN |
| IQ20 | Reinsurance transactions handled by Remittance in Drop 1; the Cashiering and Remittance addenda named in the drop plan (programme alignment) | FR-OP-030 to 041 | OPEN |
| OQ05 | Receipt series and BIR ATP | FR-OP-010 | OPEN |
| OQ06 | Approvers of cancellations and reinstatements | FR-OP-013, 014 | OPEN |
| OQ07 | Chart of accounts and default entries | FR-OP-027; section 5.3 | PARTIAL |
| OQ08 | BOOK rate source and date | FR-OP-027 | OPEN |
| OQ09 | Mode-of-payment hierarchy "Check, Cash" | FR-OP-019 | OPEN |
| OQ10 | Source of the 2% CWT flag | FR-OP-018 | OPEN |
| OQ11 | Minimal balance thresholds and the boundary with the minimal balance file | FR-OP-023, 059 (CLR-OP-39) | OPEN |
| OQ12 | Pre-booked matching key | FR-OP-018 | PARTIAL |
| OQ13 | Check pick-up source | FR-OP-017 (removed from scope, CSHID.009) | CLOSED |
| OQ14 / OQ49 | Commission OR grouping | FR-OP-021 | OPEN |
| OQ15 | Disposition list and approvers | FR-OP-022 | PARTIAL |
| OQ16 | BIR 2307 routing and posting moment | FR-OP-113, 121: routing to Disbursement by Marketing answered by BRD v1.01; entries open (CLR-OP-41) | PARTIAL |
| OQ17 | Remittance type rules, schedule, naming, shared drive | FR-OP-030 | PARTIAL |
| OQ18 | End-of-day extraction trigger | FR-OP-030 | OPEN |
| OQ19 | Paid AR greater than DTIP | FR-OP-031 | OPEN |
| OQ20 | Holding period start and cleared status | FR-OP-031 | OPEN |
| OQ21 | Remittance approval and status model | FR-OP-033 | OPEN |
| OQ22 | Insurer OR layout and tolerance; MKTID.001 as a Marketing request | FR-OP-037, 110 | OPEN |
| OQ23 | Early remittance incentive rates and window; products answered by RMTID.041 (CLG / CBG motor and fire, 2%, e.g. 30 days) | FR-OP-038, 078 (CLR-OP-47) | PARTIAL |
| OQ24 | Hold roles, maximum and extensions | FR-OP-111 | OPEN |
| OQ25 | Special remittance approvers and conditions | FR-OP-112 | PARTIAL |
| OQ26 | Write-off ownership | FR-OP-059 | OPEN |
| OQ27 | Definition of estimated items (owner moved to Production Reconciliation) | FR-OP-097 | OPEN |
| OQ28 | Lock reasons (Comptrollership) | FR-OP-006 | OPEN |
| OQ29 | Recon frequency, template, naming, channel | FR-OP-070 to 074 | PARTIAL |
| OQ30 | Recon keys and automatch timing | FR-OP-075 | OPEN |
| OQ31 | Company-concerned and disposition lists | FR-OP-077 | OPEN |
| OQ32 | Endorsement ownership, numbering, documents; Marketing ownership answered by MKTID.014-019 | FR-OP-050, 052, 053 (CLR-OP-51) | PARTIAL |
| OQ33 | Package TSI limits and co-insurance | FR-OP-055, 007 | OPEN |
| OQ34 | Credit memo on commission decrease | FR-OP-054 | OPEN |
| OQ35 | Extension of cover: financial or non-financial | FR-OP-050, 053 | OPEN |
| OQ36 | Refund basis of partial cancellations | FR-OP-054 | OPEN |
| OQ37 | Over-adjustment baseline, limit per adjustment and warning level (ADJID.023 annex) | FR-OP-058 (CLR-OP-53) | OPEN |
| OQ38 | DP sources (system tagging per the annex) and billing and SOA formats | FR-OP-090, 092 (CLR-OP-54, 59) | PARTIAL |
| OQ39 | SLA incentive parameters and branch pass-on | FR-OP-095 (CLR-OP-58) | PARTIAL |
| OQ40 | Insurer feedback reasons; status names Valid, Invalid, Returned | FR-OP-091, 093 (CLR-OP-60) | OPEN |
| OQ41 | BIR certificates of insurers | FR-OP-096 | ANSWERED |
| OQ42 / OQ43 | Report layouts; ageing buckets (also of the commission receivables) | Section 6; FR-OP-099 (CLR-OP-61) | PARTIAL |
| OQ44 | NFR alignment | Section 8 | OPEN |
| OQ45 | Marketing scope | Section 4.7 | PARTIAL |
| OQ46 | Claims needs | FR-OP-112 | PARTIAL |
| OQ47 | Search log purpose and retention | FR-OP-024 | OPEN |
| OQ48 | Operations access matrix | Section 3 | PARTIAL |
| OQ50 | Co-insured remittance per insurer share | FR-OP-007 | OPEN |


# Traceability

Every requirement ID of the Operations BRD v1.01 and of the May 2026 annexes is listed with its page, the FRs that meet it, the screens and the test conditions of the test plan (TC-OP-nnn.n; the workbook lists each case). "p.n" is a page of BRD v1.01 in the file Operations_WS Addendum and "annex p.n" a page of the annexes. IDs moved by the annexes are listed under their old number with the new one; IDs removed from scope are listed with the FR withdrawn.

## General requirements (BRQID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: General requirements (BRQID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| BRQID.001 | p.79 | FR-OP-001 | - | TC-OP-001.1, 001.2 (3 cases) |
| BRQID.002 | p.79 | FR-OP-002 | Operations Home | TC-OP-002.1, 002.2, 002.3 (3 cases) |
| BRQID.003 | p.80 | FR-OP-003 | Operations Home, Cashiering Workbench, Remittance Workbench, Reconciliation Workbench and 1 more | TC-OP-003.1, 003.2, 003.3 (3 cases) |
| BRQID.004 | p.81 | FR-OP-130 | Hand-offs and Extracts, Incoming Requests, Interfaces | TC-OP-130.1, 130.2 (3 cases) |
| BRQID.005 | p.81 | FR-OP-131 | Interfaces | TC-OP-131.1, 131.2 (3 cases) |
| BRQID.006 | p.82-83 | FR-OP-008 | Payment Uploads, Interfaces | TC-OP-008.1, 008.2, 008.3 (4 cases) |
| BRQID.007 (new) | annex p.3 | FR-OP-004 | Invoice Search, Invoice 360, Interfaces, FFY Register | TC-OP-004.1, 004.2, 004.3, 004.4 (6 cases) |

## Cashiering (CSHID) and Disbursement (DBMID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Cashiering (CSHID) and Disbursement (DBMID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| CSHID.001 | p.86-87 | FR-OP-011, FR-OP-013, FR-OP-014 | Receive Payment, Receipt, Receipts | TC-OP-011.1, 011.2, 011.3, 013.1, 013.2, 013.3, 014.1, 014.2, 014.3 (13 cases) |
| CSHID.002 | p.87-88 | FR-OP-012, FR-OP-013 | Receipts, Receipt | TC-OP-012.1, 012.2, 013.1, 013.2, 013.3 (7 cases) |
| CSHID.003 | p.88-89 | FR-OP-013 | Receipts, Receipt | TC-OP-013.1, 013.2, 013.3 (4 cases) |
| CSHID.004 | p.89-90 | FR-OP-014 | Receipts, Receipt | TC-OP-014.1, 014.2, 014.3 (4 cases) |
| CSHID.005 | p.90-91 | FR-OP-014 | Receipts, Receipt | TC-OP-014.1, 014.2, 014.3 (4 cases) |
| CSHID.006 | p.91-92 | FR-OP-010 | Receipt Series | TC-OP-010.1, 010.2, 010.3 (5 cases) |
| CSHID.007 | p.92 | FR-OP-021 | Commission ORs | TC-OP-021.1, 021.2 (4 cases) |
| CSHID.008 | p.92-95 | FR-OP-015, FR-OP-016 | Payment Uploads, Cashiering Setup, PDC Warehouse | TC-OP-015.1, 015.2, 015.3, 016.1, 016.2, 016.3 (8 cases) |
| CSHID.009 (removed) | annex p.4-6 | FR-OP-017 removed from scope | - | - |
| CSHID.010 | p.96-97 | FR-OP-024 | Receipts, Receipt | TC-OP-024.1, 024.2, 024.3 (5 cases) |
| CSHID.011 | p.97-98 | FR-OP-024 | Receipts, Receipt | TC-OP-024.1, 024.2, 024.3 (5 cases) |
| CSHID.012 | p.98 | FR-OP-013, FR-OP-027 | Receipts, Receipt | TC-OP-013.1, 013.2, 013.3, 027.1, 027.2 (7 cases) |
| CSHID.013 | p.98-99 | FR-OP-014, FR-OP-027 | Receipts, Receipt | TC-OP-014.1, 014.2, 014.3, 027.1, 027.2 (7 cases) |
| CSHID.014 | p.99 | FR-OP-027 | Receipt | TC-OP-027.1, 027.2 (3 cases) |
| CSHID.015 | p.100 | FR-OP-010 | Receipt Series | TC-OP-010.1, 010.2, 010.3 (5 cases) |
| CSHID.016 | p.100-101; annex p.4 and 6-7 | FR-OP-023 | Unapplied Payment, Cashiering Setup | TC-OP-023.1, 023.2, 023.3 (5 cases) |
| CSHID.017 | p.101 | FR-OP-009 | Report Archive, Report Centre (Operations reports), Report | TC-OP-009.1, 009.2 (2 cases) |
| CSHID.018 | p.101 | FR-OP-009 | Report Archive, Report Centre (Operations reports), Report | TC-OP-009.1, 009.2 (2 cases) |
| CSHID.019 | p.102-103 | FR-OP-025 | Receipt, Batch Print | TC-OP-025.1, 025.2 (3 cases) |
| CSHID.020 | p.103-104 | FR-OP-018, FR-OP-132 | Receive Payment, Pre-booked Payments | TC-OP-018.1, 018.2, 018.3, 132.1, 132.2 (7 cases) |
| CSHID.021 | p.104 | FR-OP-020 | Receive Payment | TC-OP-020.1, 020.2 (2 cases) |
| CSHID.022 | p.105 | FR-OP-019 | Receive Payment | TC-OP-019.1, 019.2 (3 cases) |
| CSHID.023 | p.105; p.314-316 | FR-OP-028 | Report Centre (Operations reports) | TC-OP-028.1, 028.2 (3 cases) |
| CSHID.024 | p.105-106 | FR-OP-022 | Unapplied Payments, Unapplied Payment, Incoming Requests | TC-OP-022.1, 022.2, 022.3 (6 cases) |
| CSHID.025 | p.106-107 | FR-OP-022 | Unapplied Payments, Unapplied Payment, Incoming Requests | TC-OP-022.1, 022.2, 022.3 (6 cases) |
| CSHID.026 (removed) | p.67-69, 186-188 (deleted) | FR-OP-026 removed from scope | - | - |
| CSHID.027 (removed) | p.67-69, 186-188 (deleted) | FR-OP-026 removed from scope | - | - |
| DBMID.001 | p.109 | FR-OP-120, FR-OP-121 | Unapplied Payment, Disbursement Queue, BIR 2307 | TC-OP-120.1, 120.2, 121.1, 121.2 (6 cases) |

## Remittance (RMTID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Remittance (RMTID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| RMTID.001 | p.112; annex p.12 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.002 | p.113 | FR-OP-032 | Remittance Batches, Remittance Batch | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.003 | p.113-114; annex p.13 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.004 | p.114-115; annex p.14 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.005 | p.115 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.006 | p.115 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.007 | p.116-117 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.008 | p.116 | FR-OP-030 | Extraction, DTIP Status | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.009 | p.117-118 | FR-OP-033 | Remittance Batches, Remittance Batch | TC-OP-033.1, 033.2, 033.3 (4 cases) |
| RMTID.010 | p.117 | FR-OP-033, FR-OP-035 | Remittance Batches, Remittance Batch, Remittance Deductions, Remittance Deduction | TC-OP-033.1, 033.2, 033.3, 035.1, 035.2, 035.3 (7 cases) |
| RMTID.011 | p.118-119; annex p.14-15 | FR-OP-034 | Remittance Batch | TC-OP-034.1, 034.2, 034.3 (4 cases) |
| RMTID.012 | p.118-119 | FR-OP-037 | Insurer OR Upload | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.013 | p.119 | FR-OP-037 | Insurer OR Upload | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.014 | p.119-120 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.015 | p.120 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.016 | p.120-121 | FR-OP-037 | Insurer OR Upload | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.017 | p.121-122 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.018 | p.122 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.019 | p.122-123 | FR-OP-033, FR-OP-035 | Remittance Batches, Remittance Batch, Remittance Deductions, Remittance Deduction | TC-OP-033.1, 033.2, 033.3, 035.1, 035.2, 035.3 (7 cases) |
| RMTID.020 | p.123-124 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.021 | p.124 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| RMTID.022 | p.124-125 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.023 | p.125 | FR-OP-038 | Extraction, Remittance Batch, Incentive Rules | TC-OP-038.1, 038.2 (3 cases) |
| RMTID.024 | p.125-126; annex p.15 | FR-OP-032 | Remittance Batches, Remittance Batch | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.025 | p.126 | FR-OP-039 | DTIP Status | TC-OP-039.1, 039.2 (3 cases) |
| RMTID.026 | p.126 | FR-OP-005 | Invoice Search, Invoice 360 | TC-OP-005.1, 005.2, 005.3 (4 cases) |
| RMTID.027 | p.127 | FR-OP-032 | Remittance Batches, Remittance Batch | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.028 | p.127 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.029 | p.127-128 | FR-OP-033 | Remittance Batches, Remittance Batch | TC-OP-033.1, 033.2, 033.3 (4 cases) |
| RMTID.030 | p.128 | FR-OP-040 | Special Remittance, Special Remittance Request | TC-OP-040.1, 040.2 (3 cases) |
| RMTID.031 | p.128-129 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.032 | p.129 | FR-OP-005 | Invoice Search, Invoice 360 | TC-OP-005.1, 005.2, 005.3 (4 cases) |
| RMTID.033 | p.129-130 | FR-OP-040 | Special Remittance, Special Remittance Request | TC-OP-040.1, 040.2 (3 cases) |
| RMTID.034 | p.130 | FR-OP-036, FR-OP-120 | Notification Settings, Remittance Batches, Remittance Batch, Disbursement Queue and 1 more | TC-OP-036.1, 036.2, 120.1, 120.2 (6 cases) |
| RMTID.035 | p.130 | FR-OP-031 | Extraction | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.036 | p.130-131 | FR-OP-036 | Notification Settings, Remittance Batches, Remittance Batch, Disbursement Queue | TC-OP-036.1, 036.2 (3 cases) |
| RMTID.037 | p.131; annex p.16 | FR-OP-097 | Estimated Items | TC-OP-097.1, 097.2 (3 cases) |
| RMTID.038 (moved) | p.131-132; annex p.3, 16 | Moved to BRQID.007: FR-OP-004 | - | - |
| RMTID.039 | p.132; p.316-318 | FR-OP-041 | Report Centre (Operations reports) | TC-OP-041.1, 041.2 (3 cases) |
| RMTID.040 | p.132-133 | FR-OP-006 | Invoice 360, Endorsement Request | TC-OP-006.1, 006.2, 006.3 (3 cases) |
| RMTID.041 (new) | annex p.16-17 | FR-OP-038, FR-OP-078 | Extraction, Remittance Batch, Incentive Rules, Reconciliation Cycle | TC-OP-038.1, 038.2, 078.1, 078.2 (5 cases) |

## Marketing activities (MKTID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Marketing activities (MKTID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| MKTID.001 | p.133-134 | FR-OP-110 | Remittance Batch | TC-OP-110.1, 110.2 (3 cases) |
| MKTID.002 | p.134 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.003 | p.134-135 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.004 | p.135 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.005 | p.135-136 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.006 | p.136 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.007 | p.136-137 | FR-OP-111 | Remittance Holds, Remittance Hold | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.008 | p.177 | FR-OP-114 | Endorsement Request | TC-OP-114.1, 114.2 (2 cases) |
| MKTID.009 | p.137 | FR-OP-112 | Special Remittance, Special Remittance Request | TC-OP-112.1, 112.2 (3 cases) |
| MKTID.010 | p.137-138; annex p.62-63 | FR-OP-113 | BIR 2307 | TC-OP-113.1, 113.2, 113.3, 113.4 (8 cases) |
| MKTID.011 | p.138; annex p.63 | FR-OP-004 | Invoice Search, Invoice 360, Interfaces, FFY Register | TC-OP-004.1, 004.2, 004.3, 004.4 (6 cases) |
| MKTID.012 | p.138-139; annex p.63-64 | FR-OP-094 | DP Accounts, DP Billing | TC-OP-094.1, 094.2 (4 cases) |
| MKTID.013 | p.107-108 | FR-OP-113 | BIR 2307 | TC-OP-113.1, 113.2, 113.3, 113.4 (8 cases) |
| MKTID.014 (new) | annex p.33 | FR-OP-050 | Adjustment Workbench, New Endorsement Request, Batch Request Upload | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| MKTID.015 (new) | annex p.33-34 | FR-OP-050 | Adjustment Workbench, New Endorsement Request, Batch Request Upload | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| MKTID.016 (new) | annex p.35 | FR-OP-053 | Endorsement Request | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| MKTID.017 (new) | annex p.35-36 | FR-OP-053 | Endorsement Request | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| MKTID.018 (new) | annex p.37 | FR-OP-060, FR-OP-114 | Endorsement Request | TC-OP-060.1, 060.2, 114.1, 114.2 (5 cases) |
| MKTID.019 (new) | annex p.38 | FR-OP-052 | Endorsement Request | TC-OP-052.1, 052.2 (2 cases) |

## Production Reconciliation (PRCID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Production Reconciliation (PRCID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| PRCID.001 | p.142 | FR-OP-070 | Extract Schedules | TC-OP-070.1, 070.2 (2 cases) |
| PRCID.002 | p.142 | FR-OP-072 | Production Extracts | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.003 | p.142; annex p.22 | FR-OP-073 | Production Extracts | TC-OP-073.1, 073.2 (3 cases) |
| PRCID.004 | p.142-143 | FR-OP-072 | Production Extracts | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.005 | p.143 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.006 | p.143 | FR-OP-072 | Production Extracts | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.007 | p.143 | FR-OP-072 | Production Extracts | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.008 | p.144 | FR-OP-073 | Production Extracts | TC-OP-073.1, 073.2 (3 cases) |
| PRCID.009 | p.144 | FR-OP-074 | Insurer Feedback | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.010 | p.144 | FR-OP-074 | Insurer Feedback | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.011 | p.144 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.012 | p.145 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.013 | p.145 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.014 | p.145 | FR-OP-077 | Reconciliation Cycle | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.015 | p.145; annex p.23 | FR-OP-077 | Reconciliation Cycle | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.016 | p.145 | FR-OP-077 | Reconciliation Cycle | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.017 | p.145-146 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.018 | p.146 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.019 | p.146 | FR-OP-076 | Unbooked Accounts | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.020 | p.146 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.021 | p.146 | FR-OP-077 | Reconciliation Cycle | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.022 | p.146-147; annex p.23-24 | FR-OP-074 | Insurer Feedback | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.023 | p.147 | FR-OP-076 | Unbooked Accounts | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.024 | p.147 | FR-OP-075 | Reconciliation Cycles, Reconciliation Cycle, Insurer Feedback | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.025 | p.147 | FR-OP-075 | Reconciliation Cycles, Reconciliation Cycle, Insurer Feedback | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.026 | p.147; annex p.24-25 | FR-OP-075 | Reconciliation Cycles, Reconciliation Cycle, Insurer Feedback | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.027 | p.147-148 | FR-OP-075 | Reconciliation Cycles, Reconciliation Cycle, Insurer Feedback | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.028 (moved) | p.148; annex p.25-26 | Moved to RMTID.041: FR-OP-038, FR-OP-078 | - | - |
| PRCID.029 | p.148 | FR-OP-079 | Reconciliation Cycles, Reconciliation Cycle | TC-OP-079.1, 079.2 (2 cases) |
| PRCID.030 | p.148 | FR-OP-075 | Reconciliation Cycles, Reconciliation Cycle, Insurer Feedback | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.031 | p.148-149 | FR-OP-074 | Insurer Feedback | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.032 | p.149 | FR-OP-074 | Insurer Feedback | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.033 | p.149 | FR-OP-076 | Unbooked Accounts | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.034 | p.149 | FR-OP-071 | Production Extracts | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.035 | p.149 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.036 | p.149-150 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.037 | p.150 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.038 | p.150 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.039 | p.150 | FR-OP-080 | Report Centre (Operations reports) | TC-OP-080.1, 080.2 (3 cases) |

## Adjustment / Cancellation (ADJID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Adjustment / Cancellation (ADJID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| ADJID.001 | p.152 | FR-OP-050 | Adjustment Workbench, New Endorsement Request, Batch Request Upload | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.002 (moved) | p.152-153; annex p.33 | Moved to MKTID.014: FR-OP-050 | - | - |
| ADJID.003 | p.153 | FR-OP-050 | Adjustment Workbench, New Endorsement Request, Batch Request Upload | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.004 (moved) | p.153-154; annex p.33-34 | Moved to MKTID.015: FR-OP-050 | - | - |
| ADJID.005 | p.154-155; annex p.34-35 | FR-OP-053 | Endorsement Request | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| ADJID.006 | p.155 | FR-OP-056 | Endorsement Request, Posting Batches, Batch Request Upload | TC-OP-056.1, 056.2 (3 cases) |
| ADJID.007 (moved) | p.155; annex p.35 | Moved to MKTID.016: FR-OP-053 | - | - |
| ADJID.008 | p.155-156 | FR-OP-055 | New Endorsement Request, Endorsement Request | TC-OP-055.1, 055.2 (3 cases) |
| ADJID.009 | p.156-157 | FR-OP-057 | Endorsement Request | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.010 (moved) | p.157; annex p.35-36 | Moved to MKTID.017: FR-OP-053 | - | - |
| ADJID.011 | p.157 | FR-OP-056 | Endorsement Request, Posting Batches, Batch Request Upload | TC-OP-056.1, 056.2 (3 cases) |
| ADJID.012 | p.157-158 | FR-OP-057 | Endorsement Request | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.013 | p.158 | FR-OP-057 | Endorsement Request | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.014 | p.158-159; annex p.36-37 | FR-OP-054 | New Endorsement Request | TC-OP-054.1, 054.2, 054.3, 054.4 (5 cases) |
| ADJID.015 (moved) | p.159; annex p.37 | Moved to MKTID.018: FR-OP-060, FR-OP-114 | - | - |
| ADJID.016 | p.159 | FR-OP-062 | Report Centre (Operations reports) | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.017 | p.160 | FR-OP-062 | Report Centre (Operations reports) | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.018 | p.160-161 | FR-OP-060 | Endorsement Request | TC-OP-060.1, 060.2 (3 cases) |
| ADJID.019 | p.161 | FR-OP-062 | Report Centre (Operations reports) | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.020 | p.161 | FR-OP-050 | Adjustment Workbench, New Endorsement Request, Batch Request Upload | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.021 | p.161-162; annex p.37 | FR-OP-061 | Invoice 360, Adjustment Workbench, Endorsement Request | TC-OP-061.1, 061.2, 061.3 (6 cases) |
| ADJID.022 | p.162 | FR-OP-061 | Invoice 360, Adjustment Workbench, Endorsement Request | TC-OP-061.1, 061.2, 061.3 (6 cases) |
| ADJID.023 | p.162; annex p.37-38 and 40 | FR-OP-051, FR-OP-058 | New Endorsement Request | TC-OP-051.1, 051.2, 058.1, 058.2, 058.3 (7 cases) |
| ADJID.024 | p.163 | FR-OP-005, FR-OP-061 | Invoice Search, Invoice 360, Adjustment Workbench, Endorsement Request | TC-OP-005.1, 005.2, 005.3, 061.1, 061.2, 061.3 (10 cases) |
| ADJID.025 (moved) | p.163; annex p.38 | Moved to MKTID.019: FR-OP-052 | - | - |
| ADJID.026 | p.164-165 | FR-OP-059 | Minimal Balance File | TC-OP-059.1, 059.2 (3 cases) |
| ADJID.027 | p.165-166 | FR-OP-007 | Invoice 360 | TC-OP-007.1, 007.2 (4 cases) |
| ADJID.028 | p.166 | FR-OP-058 | New Endorsement Request | TC-OP-058.1, 058.2, 058.3 (5 cases) |

## Commission Receivables (CMRID)

<!-- table: widths=2.2,2.6,3.4,4.4,5.2 caption="Traceability: Commission Receivables (CMRID)" size=7.5 -->
| BRD ID | Page | FR | Screen | Test cases |
|---|---|---|---|---|
| CMRID.001 | p.169; annex p.46 | FR-OP-090 | DP Lists | TC-OP-090.1, 090.2 (4 cases) |
| CMRID.002 | p.169-170; annex p.46-48 | FR-OP-091 | DP Lists, DP Accounts | TC-OP-091.1, 091.2, 091.3 (5 cases) |
| CMRID.003 | p.170; annex p.48-50 | FR-OP-091, FR-OP-095 | DP Lists, DP Accounts, Incentive Schemes, Incentive Runs | TC-OP-091.1, 091.2, 091.3, 095.1, 095.2, 095.3 (10 cases) |
| CMRID.004 | p.171; annex p.50-51 | FR-OP-098 | Report Centre (Operations reports) | TC-OP-098.1, 098.2 (2 cases) |
| CMRID.005 | p.171-172; annex p.52-53 | FR-OP-095 | Incentive Schemes, Incentive Runs | TC-OP-095.1, 095.2, 095.3 (5 cases) |
| CMRID.006 | p.172; annex p.53-54 | FR-OP-095 | Incentive Schemes, Incentive Runs | TC-OP-095.1, 095.2, 095.3 (5 cases) |
| CMRID.007 | p.172; annex p.54-55 | FR-OP-091, FR-OP-092 | DP Lists, DP Accounts, DP Billings, DP Billing | TC-OP-091.1, 091.2, 091.3, 092.1, 092.2, 092.3 (9 cases) |
| CMRID.008 | p.172-173; annex p.55-57 | FR-OP-091, FR-OP-093 | DP Lists, DP Accounts, DP Billings, DP Billing and 1 more | TC-OP-091.1, 091.2, 091.3, 093.1, 093.2, 093.3 (9 cases) |
| CMRID.009 | p.173-174; annex p.57-58 | FR-OP-092, FR-OP-093 | DP Accounts, DP Billings, DP Billing, Insurer Responses | TC-OP-092.1, 092.2, 092.3, 093.1, 093.2, 093.3 (8 cases) |
| CMRID.010 | p.174; annex p.58 | FR-OP-094, FR-OP-096 | DP Accounts, DP Billing, BIR Certificates, BIR Certificate | TC-OP-094.1, 094.2, 096.1, 096.2 (7 cases) |
| CMRID.011 | p.174; annex p.58-59 | FR-OP-099 | DP Billings, Report Centre (Operations reports) | TC-OP-099.1, 099.2 (3 cases) |
| CMRID.012 | p.174-175; annex p.59 | FR-OP-092, FR-OP-093 | DP Accounts, DP Billings, DP Billing, Insurer Responses | TC-OP-092.1, 092.2, 092.3, 093.1, 093.2, 093.3 (8 cases) |
| CMRID.013 | p.175; annex p.59-61 | FR-OP-091 | DP Lists, DP Accounts | TC-OP-091.1, 091.2, 091.3 (5 cases) |
| CMRID.014 | p.175-176; annex p.61 | FR-OP-097 | Estimated Items | TC-OP-097.1, 097.2 (3 cases) |
| CMRID.015 | p.176; annex p.61-62 | FR-OP-096 | BIR Certificates, BIR Certificate | TC-OP-096.1, 096.2 (3 cases) |

## Coverage summary

<!-- table: widths=2.6,2.6,2.2,1.8,1.8,2.2,2.8 caption="BRD-2 requirement IDs covered (BRD v1.01 and the May 2026 annexes)" -->
| Family | IDs listed | In scope | of which new | Moved | Removed | In scope, traced to FRs and test cases |
|---|---|---|---|---|---|---|
| BRQID | 7 | 7 | 1 | 0 | 0 | 7 |
| CSHID | 27 | 24 | 0 | 0 | 3 | 24 |
| DBMID | 1 | 1 | 0 | 0 | 0 | 1 |
| RMTID | 41 | 40 | 1 | 1 | 0 | 40 |
| MKTID | 19 | 19 | 6 | 0 | 0 | 19 |
| PRCID | 39 | 38 | 0 | 1 | 0 | 38 |
| ADJID | 28 | 22 | 0 | 6 | 0 | 22 |
| CMRID | 15 | 15 | 0 | 0 | 0 | 15 |
| **Total** | **177** | **166** | **8** | **8** | **3** | **166** |

The non-functional requirements of the BRD (p.178-183) are traced in section 8; the audit trail standard set as a condition of the sign-off of BRD v1.01 (p.67, p.186) is traced to FR-OP-024 and CLR-OP-37.


# Navigation

This chapter shows how each Operations user reaches the screens. The sidebar shows a screen only when the user's role holds its permission (section 3.2), so each persona sees a different menu. The tables follow the proposed role grants.

## Screens of Operations

The 64 screens specified in chapter 13, with the menu path and the roles that can open them. A screen without its own menu entry (a record, a dialog, a report) is reached from the screen before it; its path ends with that screen.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screen-index
```

## Screen flow

How the screens link: from a list to its record, from a record action to the next screen, and from one team's work to the next. The walkthroughs of chapter 14 follow these links with real steps.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: flow
```

<!-- portrait -->

## Menu by persona

For each Operations persona, the SIT and UAT user of the seed data and the sidebar that user sees, section by section. The BRD column shows which BRD owns a section; entries of other BRDs are listed so the business unit sees the whole menu of its users.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: menus
```

## Common screen elements

Elements that behave the same on every Operations screen are described once here and not repeated in the screen specifications.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: common
```

<!-- landscape -->

# Screen specifications

One specification per screen, grouped by team: the Operations home and invoice ledger, Cashiering, Remittance, Adjustment, Production Reconciliation, Commission Receivables, and the interfaces and reports. Each gives:

- **Purpose**, **who can open it** (personas and the permission), **navigation** (menu path and the other ways in) and the related **FRs**;
- **screenshots** of the SIT environment with seed data; the numbered markers on the first screenshot match the **No.** column of the field table;
- the **field table**: section of the screen, label as shown, type, length or format, mandatory (Y, N or the condition), source list or master, default, the statuses in which the field can be changed, the validation and the message shown when it fails, word for word;
- the **actions table**: button, who sees it, when it is enabled, what happens, the resulting status and the notification sent;
- the **business rules** of the screen with their FRs, the **expected outcome** and the **test cases** of the test plan that run on the screen.

A message in angle brackets (`<invoice>`) is completed by the system with the value shown. "Workflow notice" means the in-app notification NT-01 of chapter 16.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: screens
```

<!-- portrait -->

# End-to-end walkthroughs

Four walkthroughs follow a case through the screens, persona by persona, with what the user does, what the user sees and the result of each step. They use seed data only and run in this order on the SIT environment: walkthrough B cancels the policy that walkthrough A books, pays and remits. They are the script of the SIT review sessions of the Start Here guide.

## WT-A A booked invoice from the client's payment to the insurer's official receipt

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-A
```

## WT-B Cancellation of a paid and remitted policy, from the request to the refund

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-B
```

## WT-C Direct payment commission and the reconciliation of the insurer's production

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-C
```

## WT-D Controls and the messages the user sees

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: walkthrough
id: WT-D
```

<!-- landscape -->

# Messages catalogue

Every message an Operations user can see, grouped by the screen or dialog that shows it, with its code, its type and what the user does. The texts are quoted exactly as the screens show them.

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

The in-app notifications, alerts and e-mails that Operations sends: what triggers each, who receives it and what it contains. Each user chooses the in-app and e-mail channels of the Operations events on Notification Settings (SCR-OP-06). E-mail attachments to insurers are password protected; the password follows in a separate e-mail.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: notifications
```

<!-- portrait -->

# Document outputs

The documents Operations generates, with the BDO Insure letterhead, the "Confidential" footer and page numbers on the PDF documents. For each: the template, the format, the screen that produces it, the password protection, where every field comes from and the first page as generated from seed data. The workbooks sent to insurers are shown with a selection of their columns.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: documents
```

# Upload screens

The bulk uploads of Operations run on the screen of the team that owns them: download the template, upload the filled file, review every row with its message, then process the valid rows. A file uploaded before is refused. Each upload type below has its template columns; the column checks (mandatory, number, date, Y/N) apply to every type, and the row checks listed with each type come on top. The insurer OR, hold, special remittance, DP list, insurer feedback and insurer response files are read on their own screens with the columns given in their screen specifications (chapter 13).

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: uploads
```

## Bank payment files

Payment Uploads (SCR-OP-15) reads the files of the bank channels. Each channel has its own layout; the columns below are the proposed layouts until BDOI confirms the layouts of the channels (open questions OQ03 and OQ04). Every row is matched to its invoice by the reference column and receipted; a row that does not match becomes an unapplied payment.

<!-- table: widths=3.4,9.6,4.6 caption="Columns of the bank payment files" size=8.5 -->
| File | Columns (mandatory in bold) | Reference used for the match |
|---|---|---|
| Bills Payment File | **Incremental #**, **Invoice #**, **Assured's name**, **Amount**, Late deposit (Y/N), Phone #, Branch code, **Date of payment**, Time of payment | Invoice # (invoice, ARN, policy or PN number) |
| Trade Payment File | **Transaction date**, Branch name, **Transaction description**, Debit, **Credit**, Running balance, Check no. | The invoice, ARN, policy or PN number in the Transaction description |
| CLPC Payment File | **Count**, **Date credit**, **Amount**, AR number, Invoice, **Assured**, PN number, Corp. dept, Risk code, Remarks | Invoice (invoice or ARN), then PN number |
| Direct Credit File | **Transaction date**, BP filename, **Transaction no**, **Paid amount**, Payment type, **Payor**, Account ref no, Assured, EBIX_RefNo, Logged by, Requestor | Account ref no (invoice, ARN, policy or PN number) |

<!-- landscape -->

# Cross-BRD dependencies and interface contract

Operations takes every booked invoice from New Business and Renewal, the requests of Collections and Claims, and the products and insurers of Product Maintenance; it hands its payments, journals, payment requests and deductions to the BRDs downstream. The contract below lists each exchange: the BRD or system, the direction, what is exchanged, when and how, and who owns the data. Section 7 describes the interfaces with the external systems; the technical detail is in the Technical Specification, reviewed by BDOI IT.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: contract
```

<!-- portrait -->

# Sign-off and change control

## What is signed

The business sign-off covers the release set BRD-02 Operations v2.1:

<!-- table: widths=6,11.6 caption="Documents of the release set" -->
| Document | Content |
|---|---|
| 00 Start Here | The map of the pack, the reading order per role, the steps up to closure and the dates |
| 01 Sign-off Pack Guide (deck) | Purpose, approach and steps with who does what, the module at a glance, caveats and impacts on other modules, entry and exit criteria, handover and change control |
| 02 This FRS v2.1 | Requirements (chapters 1-11), the business view of the system (chapters 12-19), sign-off (chapter 20), the proposed rules for confirmation (chapter 21), the user-story view (chapter 22), the storyboard index (chapter 23) and the screen standards (appendix) |
| 03 Sign-off workbook v2.1 | The screen standards, screens, fields, actions, rules, messages, notifications, menus, upload templates and contract of this FRS, one row each, with the BU review columns; the comments log, meeting minutes, version history and sign-off certificate |
| 04 and 05 Test plan v2.1 and its summary | The test cases traced to the FRs and to the screens of chapter 13 |

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: counts
```

## How the review is recorded

Each Operations team records its review in the sign-off workbook: Accept, Change requested or Comment on each row of the screen standards, screen catalogue, field register, business rules and messages, with the comment, the reviewer and the date. Questions, corrections and change requests go to the comments log of the workbook, where the project team answers them. The project team answers every Change requested row in the comments log before sign-off: either the row is corrected in the next version of the set, or the change is raised as a change request (below) and the row is signed as specified.

## What signing freezes

Signing this release set freezes, for Operations:

- the screens and their navigation (chapters 12 and 13), the fields with their order, labels, types, mandatory rules, lists and validations;
- the actions with their conditions and resulting statuses, and the business rules;
- the messages (chapter 15), the notifications (chapter 16) and the generated documents (chapter 17);
- the upload templates (chapter 18) and the interface contract with the other BRDs (chapter 19).

Configuration values marked "default" (receipt series, tolerances, thresholds, schedules, list entries, templates, incentive rules and schemes, section 9) are not frozen; the Business Administrator and the System Administrator change them in the system without a change request.

## Change after sign-off

A change to anything frozen is raised in the Change Management Register. The request states the screen, field, rule or message concerned, the reason and the business priority. The project team assesses it, including its effect on the other BRDs through the interface contract of chapter 19 (for example a change of the payment application hierarchy reaches the Collections worklists and the journals of Accounting), and the owners of every BRD it touches approve it. An approved change is delivered as a new version of this release set (v2.1, v2.2 and so on) with its own Start Here guide, and only the changed pages and rows are reviewed and signed again (delta sign-off).

## Proposed rules for confirmation

Chapter 21 lists the proposed business rules and screen behaviour that differ from the BRD or need a decision of BDOI. BDOI records its decision on each item with its review; a decision that changes a screen, field, rule or message is applied in the next version of this set.

<!-- pagebreak -->

## Signatures

By signing, BDOI confirms that this FRS and the sign-off workbook describe the Operations functions, screens and messages it expects in BIBS, accepts the assumptions in section 10.1 and records its decisions on the items of chapter 21. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request. The signatories are those of the approval sheet of the Operations BRD v1.01 (p.184-185).

```signoff
rows:
  - {name: "Shellah Marie C. Miranda", role: "AVP, Product Owner", organisation: BDOI}
  - {name: "Jose Melvin M. Jarin", role: "Operations: Financial Transactions and Processing", organisation: BDOI}
  - {name: "Shirley Catapang", role: "Operations: Financial Transactions and Processing (input provider)", organisation: BDOI}
  - {name: "Perjelyn Joy Gutierrez", role: "Operations: Financial Transactions and Processing (input provider)", organisation: BDOI}
  - {name: "Pia Grace M. Pinili", role: "Collections and Marketing Support (approver)", organisation: BDOI}
  - {name: "Angel Lou R. Kabigting", role: "Marketing Head Office, Admin and Collections; BBG (reviewer)", organisation: BDOI}
  - {name: "Rodrigo R. Dela Cruz", role: "Comptrollership (reviewer)", organisation: BDOI}
  - {name: "", role: "Information Technology Group (reviewer)", organisation: BDOI}
  - {name: "Dan Ace Cauton", role: "Program Manager, ESG - Business Project Services", organisation: BDO Unibank ESG}
  - {name: "Zean C. Ibay", role: "Business Analyst, ESG - Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

<!-- pagebreak -->

# Proposed business rules and clarifications for confirmation

The table lists each point where the proposed screen or rule differs from the BRD text, fills a gap the BRD leaves open, or needs a decision of BDOI. CLR-OP-36 to CLR-OP-62 come from the re-base on BRD v1.01 and the May 2026 annexes (version 2.1). None of them removes a BRD requirement; most are settled by an answer of BDOI that is applied as configuration. BDOI records its decision with the review of this set (section 20.5); a decision that changes a screen, field, rule or message is applied in the next version of the set, and an answer that only sets a value (a list, a parameter, a template) is applied as configuration.

<!-- table: widths=1.7,2.9,6.1,3.5,3.4 caption="Proposed business rules and clarifications for confirmation" size=8 -->
| Ref | Topic | Proposed rule or screen behaviour | Reason | Decision requested from BDOI |
|---|---|---|---|---|
| CLR-OP-01 | Operations sections (BRQID.003; FR-OP-003) | The Operations sections are filtered by role; a personal pin or hide of sections is not proposed. | The role filter shows each user only the sections of the user's work. | Confirm the role filter, or ask for personal pin and hide. |
| CLR-OP-02 | Lock while with Comptrollership (RMTID.040; FR-OP-006) | Remittance and Adjustment lock the invoice while they work on it; a Comptrollership lock is added when BDOI names the activity. | The Comptrollership activity that should lock the invoice is not named (OQ28). | Name the Comptrollership activity (OQ28). |
| CLR-OP-03 | Insurer production report (PRCID.009; FR-OP-074) | The handler uploads the insurer file; a mailbox or file transfer pick-up is added when the insurer channels are specified. | The insurer channels are not specified (OQ29). | Specify the insurer channels (OQ29), or confirm the upload. |
| CLR-OP-04 | Schedule to Marketing (MKTID.001; FR-OP-110) | The Remittance processor sends the approved schedule from the batch, once. | The BRD has Marketing initiate the request (OQ22). | Confirm that the processor sends the schedule. |
| CLR-OP-05 | Edit of the extracted file (RMTID.002; FR-OP-032) | Rows of the extract are excluded and restored with a reason; financial fields cannot be edited. | The December 2025 addendum, merged into BRD v1.01 (p.113), governs over the original BRD, which allowed online edits. | Confirm exclusion in place of edits. |
| CLR-OP-06 | Receipt cancellation and reinstatement (CSHID.001-005; FR-OP-014) | A cancellation or reinstatement is a request approved by a Cashiering TL / TH (four eyes) before it posts. | The BRD does not name an approver (OQ06). | Confirm the Cashiering TL / TH approval (OQ06). |
| CLR-OP-07 | Commission OR per batch (CSHID.007; FR-OP-021) | Remittance approval issues one commission OR per batch; the Collection upload issues one OR per insurer, certificate and payment. | The BRD allows one OR per batch or per payment (OQ49). | Confirm the OR per batch and per payment (OQ49). |
| CLR-OP-08 | Payment files (CSHID.008; FR-OP-015) | Operations users upload the bank files; each file is stored read-only with its fingerprint, a duplicate file is refused, and the layouts are configurable. | The BRD has IT run the Trade, CLPC and Direct Credit files on FS04 (OQ03, OQ04). | Confirm the upload by Operations users (OQ03, OQ04). |
| CLR-OP-09 | Minimal balances (CSHID.016, ADJID.026; FR-OP-023, 059) | Minimal balances are cleared when the payment is processed (CLR-OP-39); the safety-net run skips an invoice already written off by the minimal balance file, so a balance is never reversed twice. | The BRD reverses up to 10.00 and writes off 10.00-100.00 from a file, with 10.00 in both (OQ11). | Confirm the boundary of 10.00 (OQ11). |
| CLR-OP-10 | End-of-day extraction (RMTID.005; FR-OP-030) | A user queues the searched invoice for the evening extraction run. | The BRD text "if payment application is searched" is open to reading (OQ18). | Confirm the queue for the evening run (OQ18). |
| CLR-OP-11 | DTIP balance (RMTID.014; FR-OP-031) | A paid AR above the DTIP balance is excluded from the remittance by default (option Exclude); the option Cap remits the DTIP balance only. | The BRD's expected result and acceptance criteria differ (OQ19). | Choose Exclude or Cap (OQ19). |
| CLR-OP-12 | Insurer OR comparison (RMTID.016; FR-OP-037) | The insurer OR amount is compared exactly with the paid PR per invoice. | No tolerance is given (OQ22). | Give the tolerance, or confirm the exact comparison (OQ22). |
| CLR-OP-13 | Cleared checks (RMTID.017; FR-OP-031) | Checks are counted in banking days from the last applied value date on the branch calendar; the cleared status comes from the payment application. | The BRD asks for checks at least 3 banking days old and cleared (OQ20). | Confirm the counting (OQ20). |
| CLR-OP-14 | Invoice statuses (RMTID.019; FR-OP-033, 035) | The invoice keeps a payment status and a remittance status; the batch has its own workflow stages. | The BRD's eight statuses mix payment and remittance states (OQ21). | Confirm the two statuses (OQ21). |
| CLR-OP-15 | Claims condition of a special remittance (MKTID.009; FR-OP-112) | The claims condition is confirmed through the Claims feed when it is connected; until then the request carries a note. | The Claims feed is not yet specified (OQ46). | Specify the Claims feed (OQ46). |
| CLR-OP-16 | Insurer register columns (PRCID.002; FR-OP-072) | The Remarks and Incentive columns and 200 blank rows are editable, so the insurer can add unbooked production. | PRCID.022 asks the insurer to report unbooked production in the same file. | Confirm the editable columns. |
| CLR-OP-17 | Location filter (PRCID.014, 021; FR-OP-077) | Reconciliation items filter by AO, sales unit, segment and product line; the location filter is on the reports. | The invoice ledger groups items by sales organisation. | Confirm the filters. |
| CLR-OP-18 | Reconciliation statuses (PRCID.030; FR-OP-075) | A fifth status, BDOI Only, marks booked accounts that the insurer did not return (see also CLR-OP-50 for the three statuses of the annex). | The BRD's four statuses do not cover accounts missing from the insurer's file. | Confirm the fifth status. |
| CLR-OP-19 | Sum insured on the register (Annex IV #5; FR-OP-072) | The production register has no Sum Insured column. | The invoice ledger does not hold the sum insured. | Confirm the register without the sum insured, or ask for it from the account. |
| CLR-OP-20 | Several accounts in one request (ADJID.001; FR-OP-050) | One request per invoice; the wizard and the upload raise several requests at once. | Each invoice has its own approval and posting. | Confirm one request per invoice. |
| CLR-OP-21 | Non-financial endorsement (ADJID.003; FR-OP-050) | Booking records the non-financial endorsement with its description; the account data are not changed. | The account data belong to New Business (OQ32). | Confirm (OQ32). |
| CLR-OP-22 | Quotation for a TSI increase (ADJID.008; FR-OP-055) | A hand-off to Marketing; the quotation number is linked on the request by reference. | The quotation is prepared in New Business. | Confirm the hand-off to Marketing. |
| CLR-OP-23 | DP premium receivable reversal (MKTID.012; FR-OP-094) | The ledger reversal is always recorded; its GL entry is switched off by a parameter because booking posts no PR for DP invoices. | Booking of a DP invoice has no premium receivable to reverse (OQ07). | Confirm the ledger-only reversal (OQ07). |
| CLR-OP-24 | Incentive schemes (CMRID.005, 006; FR-OP-095) | Superseded by CLR-OP-58: the incentives follow each insurer's SLA per invoice; the programmes stay inactive until BDOI gives the SLA parameters. | The annex of May 2026 replaces the target-based schemes (OQ39). | See CLR-OP-58. |
| CLR-OP-25 | Collection and Disbursement systems (BRQID.004, MKTID.010, 013, CMRID.001, DBMID.001, RMTID.034; FR-OP-130, 120) | Uploads for the Collection data and an in-app Disbursement queue, until BRD-4 Collections and BRD-5 Disbursement replace them. | The Collection and Disbursement systems are replaced by BIBS modules (OQ01, OQ02, OQ45). | Confirm the uploads and the queue until then. |
| CLR-OP-26 | Match keys and automatch frequency (PRCID.024-027; FR-OP-075) | Lines are matched on the match keys of the reconciliation parameters in order (by default the invoice number, then the policy number); the automatch runs on every upload and when the handler clicks Match Again. | The keys and the frequency are not given (OQ30). | Give the match keys and the automatch frequency (OQ30). |
| CLR-OP-28 | Insurer in the references (screen standards) | Remittance batches, reconciliation cycles and their titles carry the insurer's short code in the reference (for example RMB-INS-MGIC-2026-000001, PRC-INS-MGIC-202609-000001); the insurer's name shows in the record facts and the documents. | The code in the reference tells the insurer at a glance in lists and file names. | Confirm the short code in the references and record titles, or ask for the name there. |
| CLR-OP-32 | Insurer's decision in the DP billing (DO-08) | The insurer answers each account in the Decision column with Approved or Rejected (Yes or No is read the same way) and gives the reason of a rejection; DP Responses reads these values. | Fixed values let the answer be read without interpretation. | Confirm the values, or give the insurers' wording. |
| CLR-OP-33 | Hold reasons (MKTID.003; FR-OP-111) | The hold reason list has one value, Others, with the comment, until BDOI gives its list. | The list is to be supplied by BDOI (OQ24). | Give the list of hold reasons (OQ24). |
| CLR-OP-34 | Insurer references in the generated workbooks (DO-07, DO-08) | The production register and the DP billing show the insurer's short code, the dates as yyyy-mm-dd and the remittance status as a code, for the insurer's own systems to read. | The files are read back by the insurers and by BIBS. | Confirm the layout for the insurers, or give the layout each insurer needs. |
| CLR-OP-35 | Basis of the recompute (ADJID.014; FR-OP-054) | Before, Change and After of a request are all taken on the premium in force of the policy year: the invoice with the endorsements and returns of the same policy year, as booking computes a cancellation. A flat cancellation of a policy with later endorsements therefore shows the whole policy year going to zero; a write-off or a non-financial request stays on the invoice alone. | The cancellation takes back the whole policy year, so a Before of the booking invoice alone would show negative After amounts. | Confirm the premium in force of the policy year as the basis. |
| CLR-OP-36 | Approval status of the May 2026 annexes (R2; references) | The set treats the five annexes as approved by their signatories (p.8, 18, 27, 39, 65) and BRD v1.01 as approved on its sheet (p.184-185). | On the overall approval page of the annexes (p.66) Jose Melvin Jarin is marked on leave and Roderick Lim has neither signed nor dated. | Confirm that the annexes are approved as issued, or have the two signatures added. |
| CLR-OP-37 | Audit trail standard (BRD v1.01 sign-off, p.67, p.186; NFR; FR-OP-024) | Every Operations audit record holds the seven items of the IT standard: user, event type, module, from and to values, origin of the event (IP address, MAC address, terminal ID or equivalent), date and time, success or failure indicator; refused actions are recorded with Failure. | BDOI Operations made its sign-off of BRD v1.01 conditional on this standard. | Confirm that the seven items and the recording of refused actions meet the IT audit standard; BDOI IT confirms the origin detail it needs. |
| CLR-OP-38 | Check pick-up removed (CSHID.009; FR-OP-017; cross-reference BRD-4 Collections) | The Check Pick-up screen, the pick-up requests from Collections, the Check Pick-ups Due tile and the pick-up report are withdrawn; checks picked up by Marketing Collection are received like any other check. The same removal applies to the hand-off of FRS BRD-4 Collections (FR-CL-032 and its walkthrough WT-A), which the Collections set handles. | The Cashiering annex (p.4-6, MOM 05052026) removes CSHID.009 because the process is no longer part of BDOI's target process. | Confirm the withdrawal in both sets, and whether the cancellation reason "Check not picked up" stays in the receipt cancellation reasons. |
| CLR-OP-39 | Minimal balances at processing (CSHID.016; FR-OP-023, 059) | Minimal balances and minimal excess payments are cleared in the posting of the payment, never routed to Unapplied Payments; the daily run stays as a safety net for balances left by later events. Threshold 10.00 for premium and excess until BDOI confirms; the Adjustment file keeps 10.00 to 100.00. | The annex requires clearing when the transaction is processed, a configurable threshold and no duplicate reversal; the BRD has a second rule of 10.00 to 100.00 in Adjustment (OQ11). | Confirm the threshold values and that a balance of exactly 10.00 is cleared at processing, not written off by the file. |
| CLR-OP-40 | Owner of the PR 2307 reversal (MKTID.013; CSHID.026, 027 deleted; FR-OP-026, 113, 121) | Marketing Collection tags, validates against the certificates, generates and posts the BIR 2307 transaction report and routes it to Disbursement; the Cashier's 2307 right moves to Marketing Collection. The cash payment of the 2% by a client is received by Cashiering as an ordinary payment applied to the withheld portion, without a 2307 tag (the Settle in Cash step of v2.0 is withdrawn). The screens of chapter 13 show the v2.0 layout and are updated with this change. | BRD v1.01 deletes the Cashiering steps and the e-mail record confirms "the removal of cashiering from certificate handling". | Confirm Marketing Collection as owner of every step and the handling of a 2% paid in cash. |
| CLR-OP-41 | PR 2307 accounting entries (MKTID.013; DBMID.001; FR-OP-113, 121; section 5.3) | One entry when Marketing Collection posts the reversal: Dr DTIP / Cr PR by component; no entry when Disbursement releases the certificates. The v2.0 two-step entries (Dr PR2307 / Cr PR, then Dr DTIP / Cr PR2307) are withdrawn. | Comptrollership (R. dela Cruz) recommended this entry in the gap discussion recorded by ITG (p.69-70, p.188-189); BRD v1.01 only says the PR is zeroed out and refers the entries to the Collections BRD (OQ16). | Comptrollership confirms the entry, together with the owners of BRD-4 and BRD-5. |
| CLR-OP-42 | Outstanding commission receivable (CRU) in the PR 2307 reversal (MKTID.010, 013; FR-OP-113, 094) | CRU means a commission receivable of BDOI still unpaid on the same invoice or account (a DP commission billed or not yet collected). When one exists, the reversal and release continue, the tag is flagged For Commission Receivable and listed for the Commission Receivables team. | The BRD names the CRU check but does not define the CRU or what the Commission Receivables team does with the flag. | Confirm the definition of the CRU and the follow-up by Commission Receivables. |
| CLR-OP-43 | Allowable PR 2307 amount and remitted payments (MKTID.010, 013; FR-OP-113) | The allowable PR 2307 amount is the client's withholding rate (2% by default) times the premium subject to creditable withholding tax of the invoice, from the withholding tax rules maintained by Comptrollership. When the payment was already remitted, the reversed amount goes to Unapplied Payments with the insurer's written confirmation of refund attached; no reinstatement request is raised and nothing is routed to Adjustment. | MKTID.013 asks for a computed PR 2307 amount "based on applicable withholding tax rules and invoice details" without giving them; MKTID.010 drops the reinstatement request and the Adjustment route. | Confirm the computation basis (premium, charges included or not) and the Unapplied route. |
| CLR-OP-44 | Instalment invoices in the extraction (RMTID.003; FR-OP-030, 031) | An invoice paid by instalment is extracted for the instalments applied and posted, even while the account is "still in process"; "in process" is read as an account with later instalments not yet due or not yet paid. | The Remittance annex adds the rule without defining "still in process" (open endorsement, partial payment or account not booked in full). | Define "still in process" for instalment accounts. |
| CLR-OP-45 | Remittance schedule before submission (RMTID.011; FR-OP-034) | Before submission the processor can save or export the schedule of the batch as it stands, marked Draft and not kept as the issued schedule; printing and the payment request stay after submission. | The annex allows saving or producing the schedule before submission; a schedule that can still change is marked so that it is not sent as final. | Confirm the Draft mark. |
| CLR-OP-46 | Columns of the extracted accounts (RMTID.024; FR-OP-032) | The extracted accounts show the remittance schedule details (insurer, policy number, invoice, assured, remittance type, batch number, amounts); the settlement number is added when its meaning and source are given. | The annex names a "settlement number" that the BRD does not define. | Say what the settlement number is and where it comes from. |
| CLR-OP-47 | Early remittance incentive and estimated items moved (RMTID.041, RMTID.037; FR-OP-038, 078, 097) | Remittance validates and computes the incentive for CLG / CBG motor and fire accounts (2%, window of 30 days from inception by default) as a separate line; Production Reconciliation keeps a read-only check of the incentive the insurer applied (FR-OP-078). Estimated items are flagged by the Production Reconciliation Handler. | The annexes move PRCID.028 to Remittance as RMTID.041 and RMTID.037 to Production Reconciliation; the start of the window and the definition of estimated items are open (OQ23, OQ27). | Confirm the start of the window, whether the reconciliation check of FR-OP-078 stays, and the definition of estimated items. |
| CLR-OP-48 | Cover letter (PRCID.003; FR-OP-073) | The register is sent with the cover letter as in v2.0 until BDOI confirms. | The Production Reconciliation annex says cover letter generation "is no longer included in this addendum" but prints PRCID.003 unchanged; BRD v1.01 keeps it. | Confirm whether PRCID.003 is removed from scope or only unchanged. |
| CLR-OP-49 | Peso-equivalent tolerance (PRCID.026; FR-OP-075) | The difference of a foreign currency account is converted to pesos at the BOOK rate of the booking date of the invoice before the 1.00 tolerance is applied. | The annex asks for the conversion without naming the rate or its date (OQ08). | Confirm the rate and the date of the conversion. |
| CLR-OP-50 | Reconciliation statuses (PRCID.022, 030; FR-OP-074, 075) | Matched and Matched with Discrepancy as in the annex; the annex status Unmatched is shown as three kinds: BDOI Only, Unmatched - Pre-booked and Unmatched - No Booking. A record with a variance reported by the insurer is never Matched. | The annex names three statuses; the three kinds of Unmatched let each be followed (CLR-OP-18). | Confirm the mapping of the five statuses to the three of the annex. |
| CLR-OP-51 | Renumbered Marketing activities (MKTID.008, 014-019; ADJID.003; FR-OP-050, 053, 060, 114) | The FRs are traced to MKTID.014-019; ADJID.003 stays an Adjustment requirement and MKTID.008 stays next to MKTID.018 until BDOI confirms which one was deleted as the duplicate. | The revision log of the Adjustment annex names ADJID.003 and "deleted duplicate MKTID", which its table does not show (OQ32). | Confirm whether ADJID.003 moved to Marketing and which MKTID was deleted. |
| CLR-OP-52 | Return of endorsement requests (ADJID.005, MKTID.016; FR-OP-053) | A reason from the list is mandatory for every return; the request goes to the Marketing owner of the account, who corrects and resubmits; the request page shows each return and resubmission. | ADJID.005 says the reason is optional in its expected result and "mandatory or configurable" in its criteria; MKTID.016 makes it mandatory. | Confirm the mandatory reason. |
| CLR-OP-53 | Justification and over-adjustment limits (ADJID.023 annex; FR-OP-058) | Justification is required when a single adjustment exceeds the limit per adjustment, the balance becomes negative, the cumulative adjustments approach or exceed the original premium or DTIP, or a duplicate is overridden; warning at 90% of the original premium; cumulative limit 100%. | The annex sets the rules but not the values of the limit per adjustment and of "approaching" (OQ37). | Give the limit per adjustment and the warning level. |
| CLR-OP-54 | Source of the DP entries (CMRID.001; FR-OP-090) | The DP tagging of the booking confirmed by Marketing Collection is the source of the DP entries; the DP list upload stays as a fallback until BDOI withdraws it. | The annex introduction eliminates manual file uploads, while CMRID.001 is reprinted with the Head Office and branch DP lists (OQ38). | Confirm the withdrawal of the DP list upload. |
| CLR-OP-55 | Read-only review of DP entries (CMRID.002; FR-OP-091) | The review shows the production record, commission history, remittance and commission status read-only and highlights visible inconsistencies as flags; the v2.0 ledger checks become such flags and no longer set a sanitation result. | CMRID.002 forbids validation, reconciliation, matching and computation in the review but allows highlighting inconsistencies. | Confirm the flags shown (missing data, duplicate, status discrepancy). |
| CLR-OP-56 | Exclusion rules (CMRID.003; FR-OP-091, 095) | Rules Negative amount and Fully cancelled (the v2.0 rule "erroneous booking" is renamed); the handler's manual exclusion with a reason is withdrawn; other rules are added from BDOI's annex of criteria. | CMRID.003 refers to an annex of exclusion criteria that is not in the document and forbids undefined terms and user-driven exclusion. | Provide the annex of exclusion criteria; confirm the withdrawal of the manual exclusion. |
| CLR-OP-57 | Classification of commission entries (CMRID.004; FR-OP-098) | Direct Billed (Regular) and Direct Billed (Priority) for direct payment / direct bill entries, Regular Commission for non-direct bill accounts; the Priority tag is proposed as a flag on the DP tagging at quotation or policy. | The requirement text names Priority / Direct Payment / Direct Bill against Regular Commission, while the expected result names Direct Billed (Regular) and Direct Billed (Priority); the source of the Priority tag is not given. | Confirm the categories and who sets the Priority tag. |
| CLR-OP-58 | SLA incentive programmes (CMRID.005, 006; FR-OP-095) | Programmes are configured from each insurer's SLA (criteria, rate or fixed amount, multiplier, credit term, product, period, branch, marketing unit); No Touch, Top Up and Motor Mania become entries of the programme type list; the branch pass-on is kept only if BDOI confirms it. | The annex replaces the production-target incentives and the Motor Mania plan; the SLA parameters are not given and the pass-on to branches is no longer stated (OQ39). | Give the SLA parameters per insurer; confirm whether incentives are passed on to branches. |
| CLR-OP-59 | Statement of Account (CMRID.007; FR-OP-092) | A Statement of Account per insurer (proposed layout in FR-OP-092) besides the billing; billings and statements on demand or on a schedule. | The annex asks for outputs usable for SOA preparation without a layout (OQ38). | Give the SOA layout and the schedule. |
| CLR-OP-60 | DP account statuses (CMRID.008; FR-OP-091, 093) | Valid (eligible, not excluded, approved), Invalid (excluded or rejected) and Returned (sent back for correction or review), with a predefined reason before Invalid or Returned is final; the v2.0 tags map to them. | The annex replaces the tags DP for billing and DP for confirmation by Valid / Invalid / Returned (OQ40). | Confirm the statuses and give the feedback reasons. |
| CLR-OP-61 | Ageing of commission receivables (CMRID.011; FR-OP-092, 093, 099) | Ageing per invoice from the billing date or the SOA date with buckets 0-30, 31-60, 61-90, 91-180 and over 180 days; the 10-working-day follow-up date is kept as a reminder that the handler can switch off. | The annex replaces the 10-working-day feedback timeline by ageing and rejects "timeline tracking instead of ageing logic"; CMRID.009 still asks for billing within the SLA (OQ43). | Confirm the buckets and the reference date, and whether the reminder stays. |
| CLR-OP-62 | Fully paid status for billing (CMRID.013; FR-OP-091) | An account is eligible for billing when the payment status of the invoice ledger is Paid (fed by Cashiering and the Collections dispositions of BRD-4); the handler no longer confirms the payment and no manual override is offered. | The annex requires system-driven eligibility from the PR tagging upstream and no manual inference. | Confirm the payment status as the source, with the BRD-4 Collections owners. |

# User-story view

Each BRD requirement of the Operations BRD v1.01 and of the May 2026 annexes, restated as a user story: "As <persona>, I need <capability>, so that <business outcome>." The BRD writes its requirements in the form "As <Persona>, I must be able to ..."; the capability keeps the BRD's wording, tidied, and the persona is the FRS persona of chapter 3 who works with the function. The acceptance criteria are those of the FRs (FR-OP-nnn ACn, numbered as in chapter 4) and the test cases are the test conditions of the test plan. Moved and removed requirements are listed with their new number or their status.

## General requirements (BRQID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: General requirements (BRQID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| BRQID.001 | As a Cashier, I need to access and log in to BIBS with my own credentials, so that only I can work with the Operations functions of my role. | FR-OP-001 | FR-OP-001 AC1-3 | TC-OP-001.1, 001.2 (3 cases) |
| BRQID.002 | As the System Administrator, I need BIBS to restrict every Operations screen and action to authorised users, so that only authorised personnel can reach the Operations data. | FR-OP-002 | FR-OP-002 AC1-3 | TC-OP-002.1, 002.2, 002.3 (3 cases) |
| BRQID.003 | As a Remittance Processor, I need to land on the Operations functions of my role after log-in and move between the sections I am allowed to use, so that I reach my work at once, with the functions, validations, reports and links of each section. | FR-OP-003 | FR-OP-003 AC1-3 | TC-OP-003.1, 003.2, 003.3 (3 cases) |
| BRQID.004 | As the System Administrator, I need BIBS to integrate with the Collection, Accounting, Disbursement, Marketing and Claims systems and modules to fetch or flow in data, so that Operations works on complete data without re-keying it. | FR-OP-130 | FR-OP-130 AC1-2 | TC-OP-130.1, 130.2 (3 cases) |
| BRQID.005 | As the System Administrator, I need BIBS to fetch the data that Operations needs from the other systems and modules, on schedule or in real time, so that the operational data are current when a team starts its work. | FR-OP-131 | FR-OP-131 AC1-2 | TC-OP-131.1, 131.2 (3 cases) |
| BRQID.006 | As the System Administrator, I need batch jobs to continue past records in error and log every failed record with its reason, so that one bad record does not stop a whole run and the failures can be corrected. | FR-OP-008 | FR-OP-008 AC1-3 | TC-OP-008.1, 008.2, 008.3 (4 cases) |
| BRQID.007 (new) | As a Remittance Processor, I need to view the payment history of each invoice, with the payment dates, the amounts applied and the AR numbers, so that every Operations team sees the same, traceable payment data at invoice level. | FR-OP-004 | FR-OP-004 AC1-5 | TC-OP-004.1, 004.2, 004.3, 004.4 (6 cases) |

## Cashiering (CSHID) and Disbursement (DBMID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Cashiering (CSHID) and Disbursement (DBMID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| CSHID.001 | As a Cashier, I need to create, cancel and reinstate Acknowledgement Receipts for premium and non-premium payments, so that every payment received is receipted, and corrected only through a controlled cancellation or reinstatement. | FR-OP-011, FR-OP-013, FR-OP-014 | FR-OP-011 AC1-3; FR-OP-013 AC1-3; FR-OP-014 AC1-3 | TC-OP-011.1, 011.2, 011.3, 013.1, 013.2, 013.3, 014.1, 014.2, 014.3 (13 cases) |
| CSHID.002 | As a Cashier, I need to create, cancel and reinstate Official Receipts for BDOI income (service fees, profit share, commissions, incentives and others), so that BDOI's income is receipted with the right BIR receipt. | FR-OP-012, FR-OP-013 | FR-OP-012 AC1-2; FR-OP-013 AC1-3 | TC-OP-012.1, 012.2, 013.1, 013.2, 013.3 (7 cases) |
| CSHID.003 | As a Cashier, I need to choose the reason for the cancellation of an AR or OR, so that every cancellation carries a reason that can be audited. | FR-OP-013 | FR-OP-013 AC1-3 | TC-OP-013.1, 013.2, 013.3 (4 cases) |
| CSHID.004 | As a Cashier, I need to choose the reason for a reinstatement, so that every reinstatement carries a reason from the agreed list. | FR-OP-014 | FR-OP-014 AC1-3 | TC-OP-014.1, 014.2, 014.3 (4 cases) |
| CSHID.005 | As a Cashier, I need to encode the information a reinstatement needs (for example the invoice number of a premium payment), so that the reinstated receipt is applied to the right invoice. | FR-OP-014 | FR-OP-014 AC1-3 | TC-OP-014.1, 014.2, 014.3 (4 cases) |
| CSHID.006 | As the Cashiering TL / TH, I need BIBS to assign AR and OR numbers per transaction from the series of the branch, with ORs issued by Head Office only, so that receipt numbers are controlled, unique and compliant with BIR. | FR-OP-010 | FR-OP-010 AC1-4 | TC-OP-010.1, 010.2, 010.3 (5 cases) |
| CSHID.007 | As a Cashier, I need to issue one OR per commission check or credited amount, or one OR for several payments of the same insurer or payee and certificate, so that commission payments are receipted as the insurers and BIR require. | FR-OP-021 | FR-OP-021 AC1-2 | TC-OP-021.1, 021.2 (4 cases) |
| CSHID.008 | As a Cashier, I need to upload the Bills Payment, Trade, CLPC, matured PDC and Direct Credit files for batch processing, so that bank and channel payments are receipted and applied without keying each one. | FR-OP-015, FR-OP-016 | FR-OP-015 AC1-3; FR-OP-016 AC1-3 | TC-OP-015.1, 015.2, 015.3, 016.1, 016.2, 016.3 (8 cases) |
| CSHID.009 | Removed from scope by the Cashiering annex (annex p.4-6). The former story was: as a Cashier, I need to accept checks tagged for pick-up from Collection. | - | - | - |
| CSHID.010 | As a Cashier, I need to search receipts by AR, OR, client, invoice, payor, assured, amount, date, policy and insurer, so that I find any receipt quickly. | FR-OP-024 | FR-OP-024 AC1-3 | TC-OP-024.1, 024.2, 024.3 (5 cases) |
| CSHID.011 | As an Auditor, I need BIBS to log every action on an AR with the AR number and the user name in the audit trail, so that every receipt action can be traced to the person who did it. | FR-OP-024 | FR-OP-024 AC1-3 | TC-OP-024.1, 024.2, 024.3 (5 cases) |
| CSHID.012 | As a Cashier, I need BIBS to post the accounting entries of a posted cancellation, so that the books reflect the cancelled receipt at once. | FR-OP-013, FR-OP-027 | FR-OP-013 AC1-3; FR-OP-027 AC1-2 | TC-OP-013.1, 013.2, 013.3, 027.1, 027.2 (7 cases) |
| CSHID.013 | As a Cashier, I need BIBS to post the accounting entries of a posted reinstatement, so that the books reflect the reinstated receipt at once. | FR-OP-014, FR-OP-027 | FR-OP-014 AC1-3; FR-OP-027 AC1-2 | TC-OP-014.1, 014.2, 014.3, 027.1, 027.2 (7 cases) |
| CSHID.014 | As a Cashier, I need BIBS to post the accounting entries of every AR and OR, so that receipts and the general ledger always agree. | FR-OP-027 | FR-OP-027 AC1-2 | TC-OP-027.1, 027.2 (3 cases) |
| CSHID.015 | As a Cashier, I need BIBS to check that the AR or OR series is not depleted before a transaction, so that no receipt is issued outside an authorised series. | FR-OP-010 | FR-OP-010 AC1-4 | TC-OP-010.1, 010.2, 010.3 (5 cases) |
| CSHID.016 | As a Cashier, I need BIBS to clear minimal premium balances and minimal excess payments automatically when the payment is processed, within a configurable threshold and except for the 2% CWT, the DST charged or the entire premium, so that trivial residuals never reach the unapplied or disposition work and need no manual action. | FR-OP-023 | FR-OP-023 AC1-5 | TC-OP-023.1, 023.2, 023.3 (5 cases) |
| CSHID.017 | As a Cashier, I need to generate, download or print the Cashiering reports I am authorised for, so that I can report on my work without asking IT. | FR-OP-009 | FR-OP-009 AC1-3 | TC-OP-009.1, 009.2 (2 cases) |
| CSHID.018 | As a Cashier, I need to view the reports generated for my role, so that reports are shared only with the users who may see them. | FR-OP-009 | FR-OP-009 AC1-3 | TC-OP-009.1, 009.2 (2 cases) |
| CSHID.019 | As a Cashier, I need to print ARs and ORs in batch, so that receipts of a day or a channel are printed in one run. | FR-OP-025 | FR-OP-025 AC1-2 | TC-OP-025.1, 025.2 (3 cases) |
| CSHID.020 | As a Cashier, I need BIBS to match each payment at acceptance, except an issued PDC, and apply it to booked accounts or keep it as pre-booked or unapplied, so that money is applied as soon as it is received. | FR-OP-018, FR-OP-132 | FR-OP-018 AC1-4; FR-OP-132 AC1 | TC-OP-018.1, 018.2, 018.3, 132.1, 132.2 (7 cases) |
| CSHID.021 | As a Cashier, I need to receive the non-premium payments of insurers as AR Insurance, apart from premium payments, so that insurer refunds and other payments are never mixed with client premium. | FR-OP-020 | FR-OP-020 AC1 | TC-OP-020.1, 020.2 (2 cases) |
| CSHID.022 | As a Cashier, I need BIBS to apply payments in the defined hierarchy of premium components, so that every payment settles the components in the agreed order. | FR-OP-019 | FR-OP-019 AC1-2 | TC-OP-019.1, 019.2 (3 cases) |
| CSHID.023 | As a Cashier, I need to generate the Cashiering reports listed in the annex, so that the team has the reports it uses today. | FR-OP-028 | FR-OP-028 AC1-2 | TC-OP-028.1, 028.2 (3 cases) |
| CSHID.024 | As a Cashier, I need to view and manage the dispositions of unapplied payments, with approval and processing of each disposition type, so that unapplied money is applied, refunded, reclassed or transferred under control. | FR-OP-022 | FR-OP-022 AC1-4 | TC-OP-022.1, 022.2, 022.3 (6 cases) |
| CSHID.025 | As a Cashier, I need to see unapplied payments in the Unapplied, Monitoring, For Approval and For Reversal tabs, so that I know at a glance what waits for a disposition, an approval or a reversal. | FR-OP-022 | FR-OP-022 AC1-4 | TC-OP-022.1, 022.2, 022.3 (6 cases) |
| CSHID.026 | Deleted in BRD v1.01 (e-mail record p.67-69, p.186-188). The former story was: as a Cashier, I need to process the BIR 2307 data tagged by Marketing. | - | - | - |
| CSHID.027 | Deleted in BRD v1.01 (e-mail record p.67-69, p.186-188). The former story was: as a Cashier, I need to reverse BIR 2307 transactions by reference number and route them to Disbursement. | - | - | - |
| DBMID.001 | As Disbursement, I need to receive the BIR 2307 report with the certificates sorted per insurer and the supporting documents from Marketing Collection, and release them to the insurers, so that the certificates reach each insurer complete, without any posting by Disbursement. | FR-OP-120, FR-OP-121 | FR-OP-120 AC1-2; FR-OP-121 AC1-3 | TC-OP-120.1, 120.2, 121.1, 121.2 (6 cases) |

## Remittance (RMTID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Remittance (RMTID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| RMTID.001 | As a Remittance Processor, I need to extract per insurer and per remittance type (With Incentives, Normal - Dollar, Normal - Peso) only the eligible accounts, excluding those with a pending or unresolved financial endorsement, so that only premium that can be remitted is put in a batch. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.002 | As a Remittance Processor, I need to open the extracted file online and exclude records, without changing any financial value, so that the batch is corrected by exclusion while the amounts stay as computed. | FR-OP-032 | FR-OP-032 AC1-5 | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.003 | As a Remittance Processor, I need BIBS to extract automatically on schedule, never reprocess an extracted invoice and include instalment invoices still in process, so that remittance runs without manual work and without double remittance. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.004 | As a Remittance Processor, I need to extract one invoice of an insurer manually, with the account excluded from later runs and tagged as taken ad hoc, so that urgent remittances are made without risk of remitting twice. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.005 | As a Remittance Processor, I need to search account and payment details per invoice or batch and have the searched invoice extracted at the end of the day, so that an invoice paid today can still be remitted today. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.006 | As a Remittance Processor, I need BIBS to extract only from applied and posted payments, whatever the channel, so that only money really applied is remitted. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.007 | As a Remittance Processor, I need the extract grouped by remittance type, processor, insurer and batch number, so that each processor works the batches of their insurers. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.008 | As a Remittance Processor, I need BIBS to give each remittance extract a unique reference or batch number, so that every batch can be traced. | FR-OP-030 | FR-OP-030 AC1-5 | TC-OP-030.1, 030.2, 030.3, 030.4 (6 cases) |
| RMTID.009 | As a Remittance Processor, I need extracted accounts transferred to Process Remittance with their status, so that I see where each account is in the remittance process. | FR-OP-033 | FR-OP-033 AC1-3 | TC-OP-033.1, 033.2, 033.3 (4 cases) |
| RMTID.010 | As a Remittance Processor, I need to submit accounts for remittance processing and approval, so that a second person approves the remittance before it is paid. | FR-OP-033, FR-OP-035 | FR-OP-033 AC1-3; FR-OP-035 AC1-3 | TC-OP-033.1, 033.2, 033.3, 035.1, 035.2, 035.3 (7 cases) |
| RMTID.011 | As a Remittance Processor, I need to save or export the remittance schedule before submission and print the schedule and payment request after it, so that I can check the schedule before I submit, and the insurer and Disbursement get the final documents. | FR-OP-034 | FR-OP-034 AC1-3 | TC-OP-034.1, 034.2, 034.3 (4 cases) |
| RMTID.012 | As a Remittance Processor, I need to receive from the insurer the populated remittance schedule with the OR of each client, so that the insurer's official receipts are known for every remitted invoice. | FR-OP-037 | FR-OP-037 AC1-3 | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.013 | As a Remittance Processor, I need to upload the insurer's schedule and update the OR date and number of each client, so that the OR details are recorded without keying. | FR-OP-037 | FR-OP-037 AC1-3 | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.014 | As a Remittance Processor, I need BIBS to check that the paid AR is not more than the outstanding DTIP of the remittance, so that no more than the insurer's due is remitted. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.015 | As a Remittance Processor, I need a list of the paid ARs higher than the DTIP balance, so that those accounts can be investigated. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.016 | As a Remittance Processor, I need an exception report comparing the insurer's OR amounts with the paid premium, so that differences with the insurer are found and followed. | FR-OP-037 | FR-OP-037 AC1-3 | TC-OP-037.1, 037.2 (4 cases) |
| RMTID.017 | As a Remittance Processor, I need BIBS to include checks only when they are at least 3 days old and cleared, so that uncleared checks are never remitted. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.018 | As a Remittance Processor, I need BIBS to validate the holding period and the clearance of check payments, so that check payments are remitted only when safe. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.019 | As a Remittance Processor, I need BIBS to validate the remittance status on submission, so that only accounts in a valid status are submitted. | FR-OP-033, FR-OP-035 | FR-OP-033 AC1-3; FR-OP-035 AC1-3 | TC-OP-033.1, 033.2, 033.3, 035.1, 035.2, 035.3 (7 cases) |
| RMTID.020 | As a Remittance Processor, I need BIBS to exclude accounts on hold or with a pending negative adjustment request, so that money that may change is not remitted. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.021 | As a Remittance Processor, I need an account processed in remittance once its holding dates have ended, so that held accounts are remitted as soon as the hold expires. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| RMTID.022 | As a Remittance Processor, I need BIBS to exclude written-off accounts, so that written-off balances are never remitted. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.023 | As the Remittance TL / TH, I need the additional incentive for early remittance computed automatically, so that BDOI earns the incentive on every qualifying remittance. | FR-OP-038 | FR-OP-038 AC1-3 | TC-OP-038.1, 038.2 (3 cases) |
| RMTID.024 | As a Remittance Processor, I need to view the extracted accounts with the details of the remittance schedule (policy, insurer, settlement number, remittance type, batch number), so that I check each account against the schedule the insurer will receive. | FR-OP-032 | FR-OP-032 AC1-5 | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.025 | As a Remittance Processor, I need to search accounts and payment details by invoice, batch, endorsement reference, policy, assured and other identifiers, so that I find any account in the remittance process. | FR-OP-039 | FR-OP-039 AC1-2 | TC-OP-039.1, 039.2 (3 cases) |
| RMTID.026 | As a Remittance Processor, I need to view the details of an account or invoice, so that I have the full picture of the invoice before acting. | FR-OP-005 | FR-OP-005 AC1-3 | TC-OP-005.1, 005.2, 005.3 (4 cases) |
| RMTID.027 | As a Remittance Processor, I need to view the accounts submitted from Process Remittance, so that I follow what was submitted. | FR-OP-032 | FR-OP-032 AC1-5 | TC-OP-032.1, 032.2, 032.3, 032.4 (5 cases) |
| RMTID.028 | As a Remittance Processor, I need BIBS to check the payment status before extraction or processing, so that only posted and cleared payments are processed. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.029 | As a Remittance Processor, I need to submit, hold and return invoices or batches, so that a batch can be stopped or sent back for correction. | FR-OP-033 | FR-OP-033 AC1-3 | TC-OP-033.1, 033.2, 033.3 (4 cases) |
| RMTID.030 | As a Remittance Processor, I need to view the special remittance requests, so that requests from Marketing are processed in Remittance. | FR-OP-040 | FR-OP-040 AC1-2 | TC-OP-040.1, 040.2 (3 cases) |
| RMTID.031 | As a Remittance Processor, I need BIBS to exclude accounts with an active hold during extraction, so that held accounts stay out of the batches. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.032 | As a Remittance Processor, I need to track the hold status and the remittance status of each account, so that both statuses are visible in one place. | FR-OP-005 | FR-OP-005 AC1-3 | TC-OP-005.1, 005.2, 005.3 (4 cases) |
| RMTID.033 | As a Remittance Processor, I need to be notified when the status of a special remittance changes, so that I act on returned, approved or held requests at once. | FR-OP-040 | FR-OP-040 AC1-2 | TC-OP-040.1, 040.2 (3 cases) |
| RMTID.034 | As a Remittance Processor, I need to be notified of remittance and disbursement status changes, so that I follow each batch to payment without asking Disbursement. | FR-OP-036, FR-OP-120 | FR-OP-036 AC1-2; FR-OP-120 AC1-2 | TC-OP-036.1, 036.2, 120.1, 120.2 (6 cases) |
| RMTID.035 | As a Remittance Processor, I need the Remittance Team notified of pending negative adjustments, so that a batch with a reduced invoice is corrected before it is paid. | FR-OP-031 | FR-OP-031 AC1-5 | TC-OP-031.1, 031.2, 031.3, 031.4 (7 cases) |
| RMTID.036 | As a Remittance Processor, I need the remittance status tracked across every stage, so that each invoice can be followed from extraction to disbursement. | FR-OP-036 | FR-OP-036 AC1-2 | TC-OP-036.1, 036.2 (3 cases) |
| RMTID.037 | As a Production Reconciliation Handler, I need estimated items tracked and flagged in the production reports, so that estimated production is never mistaken for final production. | FR-OP-097 | FR-OP-097 AC1-2 | TC-OP-097.1, 097.2 (3 cases) |
| RMTID.038 | Moved to BRQID.007: see that row. | - | - | - |
| RMTID.039 | As a Remittance Processor, I need to generate the Remittance reports listed in the annex, so that the team has the reports it uses today. | FR-OP-041 | FR-OP-041 AC1-2 | TC-OP-041.1, 041.2 (3 cases) |
| RMTID.040 | As a Remittance Processor, I need BIBS to coordinate invoice adjustments, remittance exemptions and accounting entries in the right sequence, so that no two teams change the same invoice at the same time. | FR-OP-006 | FR-OP-006 AC1-3 | TC-OP-006.1, 006.2, 006.3 (3 cases) |
| RMTID.041 (new) | As a Remittance Processor, I need BIBS to validate the eligibility of qualified CLG / CBG motor and fire accounts and compute the early remittance incentive as a separate calculation, so that the incentive is applied only when earned and never merged with the commission or remittance values. | FR-OP-038, FR-OP-078 | FR-OP-038 AC1-3; FR-OP-078 AC1-2 | TC-OP-038.1, 038.2, 078.1, 078.2 (5 cases) |

## Marketing activities (MKTID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Marketing activities (MKTID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| MKTID.001 | As Marketing Collection, I need to request that the remittance schedule be sent to the insurer, with the client details, so that the insurer receives the schedule it needs. | FR-OP-110 | FR-OP-110 AC1 | TC-OP-110.1, 110.2 (3 cases) |
| MKTID.002 | As Marketing Collection, I need to remove the On Hold tag of an account so that it is included in the extraction, so that an account is remitted once the reason for the hold is gone. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.003 | As Marketing Collection, I need to tag an account On Hold so that it is excluded from the extraction, so that premium is not remitted while an issue is open. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.004 | As Marketing Collection, I need approved hold requests assigned to the remittance processors, so that every hold is followed by a named processor. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.005 | As Marketing Collection, I need to create, cancel and extend hold requests, so that holds match the real situation of the account. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.006 | As the Marketing TL / UH, I need to approve hold requests and their cancellations, so that no account is held without approval. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.007 | As Marketing Collection, I need a unique reference number for every hold request, so that every hold can be traced. | FR-OP-111 | FR-OP-111 AC1-4 | TC-OP-111.1, 111.2, 111.3, 111.4 (6 cases) |
| MKTID.008 | As Marketing Collection, I need to generate the endorsement slip of an external endorsement, so that the endorsement is documented for the insurer and the client. | FR-OP-114 | FR-OP-114 AC1 | TC-OP-114.1, 114.2 (2 cases) |
| MKTID.009 | As Marketing Collection, I need to request a special remittance for accounts that meet specific conditions (claims, renewal, instalment due, immediate OR), so that urgent remittances are processed outside the schedule. | FR-OP-112 | FR-OP-112 AC1-2 | TC-OP-112.1, 112.2 (3 cases) |
| MKTID.010 | As Marketing Collection, I need to tag and track BIR 2307 certificates, check whether the payment was already remitted and whether a commission receivable is outstanding, and log every action, so that the PR 2307 reversal is correct, and a remitted payment or an unpaid commission is handled, not missed. | FR-OP-113 | FR-OP-113 AC1-7 | TC-OP-113.1, 113.2, 113.3, 113.4 (8 cases) |
| MKTID.011 | As Marketing Collection, I need to tag an account as direct payment during quotation and conversion to policy, so that the invoice and the payment modules treat it as direct payment. | FR-OP-004 | FR-OP-004 AC1-5 | TC-OP-004.1, 004.2, 004.3, 004.4 (6 cases) |
| MKTID.012 | As Marketing Collection, I need the DP premium receivable reversed only after a valid DP tag and the collection of the commission, so that premium and commission reversals are correct without manual files. | FR-OP-094 | FR-OP-094 AC1-3 | TC-OP-094.1, 094.2 (4 cases) |
| MKTID.013 | As Marketing Collection, I need to tag PR 2307 reversals, submit the certificates received from clients, have BIBS validate the amounts against the computed PR 2307 amount, the outstanding PR and the CRU, and post and route the BIR 2307 report to Disbursement, so that the PR is zeroed without over-reversal and every reversal is traceable to the invoice, AR / OR, certificate and CRU. | FR-OP-113 | FR-OP-113 AC1-7 | TC-OP-113.1, 113.2, 113.3, 113.4 (8 cases) |
| MKTID.014 (new) | As Marketing Collection, I need to select the type of financial endorsement from the list, so that the request asks only for the inputs of that type. | FR-OP-050 | FR-OP-050 AC1-4 | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| MKTID.015 (new) | As Marketing Collection, I need to select the type of non-financial endorsement from the list, so that descriptive and period changes are recorded without financial impact. | FR-OP-050 | FR-OP-050 AC1-4 | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| MKTID.016 (new) | As Marketing Collection, I need to receive the requests returned by Operations with their reason, correct them and resubmit them, so that problem requests are fixed by their owner and never posted as they are. | FR-OP-053 | FR-OP-053 AC1-4 | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| MKTID.017 (new) | As Marketing Collection, I need to request an extension of cover with additional premium, subject to approval, so that the additional premium is computed and approved before the endorsement is final. | FR-OP-053 | FR-OP-053 AC1-4 | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| MKTID.018 (new) | As Marketing Collection, I need to generate the endorsement slip of an external endorsement, so that the endorsement is documented for the insurer and the client. | FR-OP-060, FR-OP-114 | FR-OP-060 AC1-2; FR-OP-114 AC1 | TC-OP-060.1, 060.2, 114.1, 114.2 (5 cases) |
| MKTID.019 (new) | As Marketing Collection, I need to attach the supporting documents of an endorsement request, so that the request carries its evidence for validation. | FR-OP-052 | FR-OP-052 AC1 | TC-OP-052.1, 052.2 (2 cases) |

## Production Reconciliation (PRCID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Production Reconciliation (PRCID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| PRCID.001 | As a Production Reconciliation Handler, I need booked accounts extracted per insurer automatically on the defined frequency, on the next working day after a holiday, so that every insurer's register is ready without manual work. | FR-OP-070 | FR-OP-070 AC1 | TC-OP-070.1, 070.2 (2 cases) |
| PRCID.002 | As a Production Reconciliation Handler, I need the extracted file locked except for the insurer's remarks column, so that the insurer can answer without changing BDOI's data. | FR-OP-072 | FR-OP-072 AC1-2 | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.003 | As a Production Reconciliation Handler, I need a cover letter generated for each extract, so that the insurer receives the register with a standard letter. | FR-OP-073 | FR-OP-073 AC1-2 | TC-OP-073.1, 073.2 (3 cases) |
| PRCID.004 | As a Production Reconciliation Handler, I need a file name assigned to each extract by the standard naming convention, so that registers are filed and found consistently. | FR-OP-072 | FR-OP-072 AC1-2 | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.005 | As a Production Reconciliation Handler, I need to see the file location, date and time of each extract, so that I know which register was produced when. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.006 | As a Production Reconciliation Handler, I need the file generated in the agreed format or template, so that the insurer can read it as agreed. | FR-OP-072 | FR-OP-072 AC1-2 | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.007 | As a Production Reconciliation Handler, I need to protect the generated file with a password, so that production data travel securely. | FR-OP-072 | FR-OP-072 AC1-2 | TC-OP-072.1, 072.2 (3 cases) |
| PRCID.008 | As a Production Reconciliation Handler, I need to send the extract to the insurer with the sent time recorded, so that I can prove when the register was sent. | FR-OP-073 | FR-OP-073 AC1-2 | TC-OP-073.1, 073.2 (3 cases) |
| PRCID.009 | As a Production Reconciliation Handler, I need to receive and upload the insurer's production report, so that the insurer's answer enters the reconciliation without re-keying. | FR-OP-074 | FR-OP-074 AC1-3 | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.010 | As a Production Reconciliation Handler, I need duplicate uploads blocked, so that the same answer is never reconciled twice. | FR-OP-074 | FR-OP-074 AC1-3 | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.011 | As a Production Reconciliation Handler, I need to extract the production register manually for a booking period, so that I can reconcile any period on request. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.012 | As a Production Reconciliation Handler, I need to view the accounts of the extracted and the uploaded files, so that I see both sides of the reconciliation. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.013 | As a Production Reconciliation Handler, I need to filter and display accounts by production month and insurer, so that I work one insurer and month at a time. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.014 | As a Production Reconciliation Handler, I need to filter accounts by AO / AB, booked and pre-booked, so that I can follow up differences with the right account officer. | FR-OP-077 | FR-OP-077 AC1-3 | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.015 | As a Production Reconciliation Handler, I need to record the company concerned and an instruction or notation on a production register record, without changing any policy, invoice or financial data, so that the follow-up of each difference is recorded on the right record. | FR-OP-077 | FR-OP-077 AC1-3 | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.016 | As a Production Reconciliation Handler, I need to select the company concerned from the list, with Others, so that the follow-up names the right company. | FR-OP-077 | FR-OP-077 AC1-3 | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.017 | As a Production Reconciliation Handler, I need to generate the production register with the insurer's feedback, so that the insurer's remarks are reported per account. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.018 | As a Production Reconciliation Handler, I need to generate the production register with the insurer's and Marketing's feedback, so that both answers are reported together. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.019 | As a Production Reconciliation Handler, I need a list of the unbooked accounts with their status, so that insurer production that BDOI did not book is followed to booking. | FR-OP-076 | FR-OP-076 AC1-2 | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.020 | As a Production Reconciliation Handler, I need to view the production register of manually extracted accounts, so that manual extracts are checked like scheduled ones. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.021 | As a Production Reconciliation Handler, I need to filter by booking date, insurer, BDOI location, market segment and product type, so that I narrow the reconciliation to what I am working on. | FR-OP-077 | FR-OP-077 AC1-3 | TC-OP-077.1, 077.2, 077.3 (5 cases) |
| PRCID.022 | As a Production Reconciliation Handler, I need to upload the insurer's matching results and have BIBS keep the technical match apart from the reconciliation outcome (Matched, Matched with Discrepancy, Unmatched), so that no record with a discrepancy is reported as matched and insurer-only records are recognised. | FR-OP-074 | FR-OP-074 AC1-3 | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.023 | As a Production Reconciliation Handler, I need accounts matched from booked to pre-booked by reference number, so that pre-booked production is matched when it is booked. | FR-OP-076 | FR-OP-076 AC1-2 | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.024 | As a Production Reconciliation Handler, I need pre-booked or unbooked accounts matched automatically on booking or upload, so that matches are made without waiting for me. | FR-OP-075 | FR-OP-075 AC1-4 | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.025 | As a Production Reconciliation Handler, I need accounts of an uploaded file automatically matched by status at the set time, so that open items are matched again regularly. | FR-OP-075 | FR-OP-075 AC1-4 | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.026 | As a Production Reconciliation Handler, I need accounts with a difference of 1.00 peso equivalent or less treated as matched, with foreign currency differences converted to pesos first, so that minor differences, including those from currency conversion, are handled consistently. | FR-OP-075 | FR-OP-075 AC1-4 | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.027 | As a Production Reconciliation Handler, I need an account tagged matched once the policy number and the reference or invoice number agree, so that matching follows clear criteria. | FR-OP-075 | FR-OP-075 AC1-4 | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.028 | Moved to RMTID.041: see that row. | - | - | - |
| PRCID.029 | As a Production Reconciliation Handler, I need to view the history of an account and a cycle, so that every reconciliation step can be traced. | FR-OP-079 | FR-OP-079 AC1-2 | TC-OP-079.1, 079.2 (2 cases) |
| PRCID.030 | As a Production Reconciliation Handler, I need to tag and track the status of each account (matched, matched with discrepancies, unmatched pre-booked, unmatched no booking), so that every account is followed to closure. | FR-OP-075 | FR-OP-075 AC1-4 | TC-OP-075.1, 075.2, 075.3 (4 cases) |
| PRCID.031 | As a Production Reconciliation Handler, I need to monitor the status of each upload (successful or unsuccessful), so that failed uploads are retried. | FR-OP-074 | FR-OP-074 AC1-3 | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.032 | As a Production Reconciliation Handler, I need the number of upload attempts and their status recorded, so that upload problems with an insurer are visible. | FR-OP-074 | FR-OP-074 AC1-3 | TC-OP-074.1, 074.2 (4 cases) |
| PRCID.033 | As a Production Reconciliation Handler, I need a repository of unmatched and duplicate accounts, so that problem accounts are kept in one place until solved. | FR-OP-076 | FR-OP-076 AC1-2 | TC-OP-076.1, 076.2 (3 cases) |
| PRCID.034 | As a Production Reconciliation Handler, I need a report of the item count and extraction time of each extract, so that extractions can be checked for completeness. | FR-OP-071 | FR-OP-071 AC1-2 | TC-OP-071.1, 071.2 (3 cases) |
| PRCID.035 | As a Production Reconciliation Handler, I need the Production Reconciliation Summary report, so that management sees the reconciliation position per insurer. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.036 | As a Production Reconciliation Handler, I need the summary of unmatched accounts per location, so that locations follow up their own differences. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.037 | As a Production Reconciliation Handler, I need the summary of unmatched accounts per Marketing AO / AB, so that account officers follow up their own differences. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.038 | As a Production Reconciliation Handler, I need a report of the unmatched accounts with feedback and disposition, so that the follow-up of each difference is reported. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |
| PRCID.039 | As a Production Reconciliation Handler, I need the summary per disposition, so that the outcome of the reconciliation is reported by disposition. | FR-OP-080 | FR-OP-080 AC1-2 | TC-OP-080.1, 080.2 (3 cases) |

## Adjustment / Cancellation (ADJID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Adjustment / Cancellation (ADJID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| ADJID.001 | As an Adjustment Processor, I need to perform single or multiple financial, non-financial and internal endorsement transactions on booked accounts, so that every change to a booked account is processed in BIBS. | FR-OP-050 | FR-OP-050 AC1-4 | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.002 | Moved to MKTID.014: see that row. | - | - | - |
| ADJID.003 | As an Adjustment Processor, I need to perform single or multiple non-financial endorsement transactions on booked accounts, so that descriptive changes are recorded and linked to the account. | FR-OP-050 | FR-OP-050 AC1-4 | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.004 | Moved to MKTID.015: see that row. | - | - | - |
| ADJID.005 | As an Adjustment Processor, I need to return transactions that do not qualify before batch posting, with the reason, to Marketing for correction, so that only valid requests are posted and the history of each return is kept. | FR-OP-053 | FR-OP-053 AC1-4 | TC-OP-053.1, 053.2, 053.3, 053.4 (6 cases) |
| ADJID.006 | As an Adjustment Processor, I need to post cancellations and adjustments in batches, so that many requests are posted in one action. | FR-OP-056 | FR-OP-056 AC1-2 | TC-OP-056.1, 056.2 (3 cases) |
| ADJID.007 | Moved to MKTID.016: see that row. | - | - | - |
| ADJID.008 | As an Adjustment Processor, I need BIBS to check an increase of TSI against the package limits and ask for the quotation, so that an increase beyond the package is priced before it is endorsed. | FR-OP-055 | FR-OP-055 AC1-2 | TC-OP-055.1, 055.2 (3 cases) |
| ADJID.009 | As an Adjustment Processor, I need on a decrease of TSI, to apply the excess payment to another invoice or refund it, so that the client's money is never left unapplied after a decrease. | FR-OP-057 | FR-OP-057 AC1-2 | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.010 | Moved to MKTID.017: see that row. | - | - | - |
| ADJID.011 | As an Adjustment Processor, I need BIBS to create the accounting entries of each financial endorsement type, so that the books follow every endorsement. | FR-OP-056 | FR-OP-056 AC1-2 | TC-OP-056.1, 056.2 (3 cases) |
| ADJID.012 | As an Adjustment Processor, I need BIBS to set up the excess payment resulting from a cancellation or adjustment, so that the excess is ready for refund or re-application. | FR-OP-057 | FR-OP-057 AC1-2 | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.013 | As an Adjustment Processor, I need BIBS to reverse the payments applied to an account on the latest premium and route any excess for Marketing disposition, so that payments always match the premium in force. | FR-OP-057 | FR-OP-057 AC1-2 | TC-OP-057.1, 057.2 (3 cases) |
| ADJID.014 | As an Adjustment Processor, I need BIBS to recompute premium, commission, refund premium and sum insured per insurer automatically on submission, from validated inputs, with a service invoice when income or commission changes, so that endorsements are computed correctly and consistently with the posting. | FR-OP-054 | FR-OP-054 AC1-5 | TC-OP-054.1, 054.2, 054.3, 054.4 (5 cases) |
| ADJID.015 | Moved to MKTID.018: see that row. | - | - | - |
| ADJID.016 | As an Adjustment Processor, I need the Adjustment and Daily Endorsement reports, so that daily activity is tracked. | FR-OP-062 | FR-OP-062 AC1-2 | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.017 | As an Adjustment Processor, I need a Validation List of the posted transactions with their GL entries and remarks, so that financial audits are supported. | FR-OP-062 | FR-OP-062 AC1-2 | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.018 | As an Adjustment Processor, I need to generate the validation slip of a validated request, so that the validation is documented. | FR-OP-060 | FR-OP-060 AC1-2 | TC-OP-060.1, 060.2 (3 cases) |
| ADJID.019 | As an Adjustment Processor, I need the Adjustment report, so that adjustments are summarised for management. | FR-OP-062 | FR-OP-062 AC1-2 | TC-OP-062.1, 062.2 (3 cases) |
| ADJID.020 | As an Adjustment Processor, I need endorsement transactions linked to the original account reference number, so that every endorsement can be traced to its account. | FR-OP-050 | FR-OP-050 AC1-4 | TC-OP-050.1, 050.2, 050.3, 050.4 (7 cases) |
| ADJID.021 | As an Adjustment Processor, I need to view the ageing of transactions from the request date to the completion (posting) date, so that slow requests are followed up. | FR-OP-061 | FR-OP-061 AC1-6 | TC-OP-061.1, 061.2, 061.3 (6 cases) |
| ADJID.022 | As an Adjustment Processor, I need to record and view the endorsement transactions of an account, so that the transaction history is always available. | FR-OP-061 | FR-OP-061 AC1-6 | TC-OP-061.1, 061.2, 061.3 (6 cases) |
| ADJID.023 | As an Adjustment Processor, I need BIBS to detect duplicate requests by reference number, reason and type and to require a justification when an adjustment breaks the limits of the justification annex, so that no adjustment is processed twice or above the limits without an explanation. | FR-OP-051, FR-OP-058 | FR-OP-051 AC1; FR-OP-058 AC1-4 | TC-OP-051.1, 051.2, 058.1, 058.2, 058.3 (7 cases) |
| ADJID.024 | As an Adjustment Processor, I need to search by Account Reference Number and pull up the policy, payment and remittance details, so that I see the whole account before processing a request. | FR-OP-005, FR-OP-061 | FR-OP-005 AC1-3; FR-OP-061 AC1-6 | TC-OP-005.1, 005.2, 005.3, 061.1, 061.2, 061.3 (10 cases) |
| ADJID.025 | Moved to MKTID.019: see that row. | - | - | - |
| ADJID.026 | As an Adjustment Processor, I need to process a file of accounts with minimal balances from 10.00 to 100.00, so that small balances are written off or credited in one run. | FR-OP-059 | FR-OP-059 AC1-2 | TC-OP-059.1, 059.2 (3 cases) |
| ADJID.027 | As an Adjustment Processor, I need adjustment amounts allocated automatically by insurer share, so that co-insured accounts are adjusted per insurer correctly. | FR-OP-007 | FR-OP-007 AC1-2 | TC-OP-007.1, 007.2 (4 cases) |
| ADJID.028 | As an Adjustment Processor, I need a baseline for allowable adjustments, tracking of cumulative adjustments and a prompt above the baseline or on a negative balance, so that over-adjustment is prevented. | FR-OP-058 | FR-OP-058 AC1-4 | TC-OP-058.1, 058.2, 058.3 (5 cases) |

## Commission Receivables (CMRID)

<!-- table: widths=2.1,7.6,2.4,2.9,2.6 caption="User stories: Commission Receivables (CMRID)" size=7.5 -->
| BRD ID | User story | FR IDs | Acceptance criteria (FR AC numbers) | Test cases |
|---|---|---|---|---|
| CMRID.001 | As a Commission Handler / Processor, I need the DP entries of Head Office and the branches taken in from the DP tagging (the DP lists as fallback) and consolidated, so that billing starts from one complete list of direct payment accounts. | FR-OP-090 | FR-OP-090 AC1-3 | TC-OP-090.1, 090.2 (4 cases) |
| CMRID.002 | As a Commission Handler / Processor, I need to view each DP entry read-only at invoice level with its production record, commission history, remittance status and commission status, so that I can check the entries without the system changing or reconciling them. | FR-OP-091 | FR-OP-091 AC1-4 | TC-OP-091.1, 091.2, 091.3 (5 cases) |
| CMRID.003 | As a Commission Handler / Processor, I need entries excluded only by approved, configurable rules, kept visible with the rule, reason and time, so that billing, reports and incentives use only valid entries, without subjective exclusion. | FR-OP-091, FR-OP-095 | FR-OP-091 AC1-4; FR-OP-095 AC1-4 | TC-OP-091.1, 091.2, 091.3, 095.1, 095.2, 095.3 (10 cases) |
| CMRID.004 | As a Commission Handler / Processor, I need commission receivable outputs that classify each entry as Direct Billed (Regular), Direct Billed (Priority) or Regular Commission, with the net commission after partial remittance and the remaining balance, so that billing and reporting use one consistent classification. | FR-OP-098 | FR-OP-098 AC1-3 | TC-OP-098.1, 098.2 (2 cases) |
| CMRID.005 | As the Commission TL / TH, I need incentive eligibility and amounts computed per invoice from each insurer's SLA criteria, rates and multipliers, so that incentives follow the insurer agreements and never production targets. | FR-OP-095 | FR-OP-095 AC1-4 | TC-OP-095.1, 095.2, 095.3 (5 cases) |
| CMRID.006 | As the Commission TL / TH, I need several incentive programmes defined by configurable parameters (product, period, branch, marketing unit, minimum premium), so that new programmes are set up without changing the system. | FR-OP-095 | FR-OP-095 AC1-4 | TC-OP-095.1, 095.2, 095.3 (5 cases) |
| CMRID.007 | As a Commission Handler / Processor, I need commission receivables consolidated and computed automatically, with outputs for billing and the Statement of Account, so that billing needs no manual consolidation. | FR-OP-091, FR-OP-092 | FR-OP-091 AC1-4; FR-OP-092 AC1-3 | TC-OP-091.1, 091.2, 091.3, 092.1, 092.2, 092.3 (9 cases) |
| CMRID.008 | As a Commission Handler / Processor, I need the status of each DP account updated from the validation and the insurer's answer (Valid, Invalid / Returned) with standard reasons and an audit trail, so that every account's position and history are clear. | FR-OP-091, FR-OP-093 | FR-OP-091 AC1-4; FR-OP-093 AC1-4 | TC-OP-091.1, 091.2, 091.3, 093.1, 093.2, 093.3 (9 cases) |
| CMRID.009 | As a Commission Handler / Processor, I need validated accounts sorted by insurer and assigned to handlers for billing, with the insurer's approval or rejection recorded, so that each insurer is billed within the SLA and its answer is followed. | FR-OP-092, FR-OP-093 | FR-OP-092 AC1-3; FR-OP-093 AC1-4 | TC-OP-092.1, 092.2, 092.3, 093.1, 093.2, 093.3 (8 cases) |
| CMRID.010 | As a Commission Handler / Processor, I need OR numbers and amounts, BIR certificates, withholding tax and insurer feedback tracked, so that the collection of each commission is fully documented. | FR-OP-094, FR-OP-096 | FR-OP-094 AC1-3; FR-OP-096 AC1-2 | TC-OP-094.1, 094.2, 096.1, 096.2 (7 cases) |
| CMRID.011 | As a Commission Handler / Processor, I need commission receivables aged per invoice from the billing or Statement of Account date, so that aged receivables are prioritised for collection. | FR-OP-099 | FR-OP-099 AC1-3 | TC-OP-099.1, 099.2 (3 cases) |
| CMRID.012 | As a Commission Handler / Processor, I need validation requests sent to insurers and their responses tracked automatically, so that no validation is missed or delayed. | FR-OP-092, FR-OP-093 | FR-OP-092 AC1-3; FR-OP-093 AC1-4 | TC-OP-092.1, 092.2, 092.3, 093.1, 093.2, 093.3 (8 cases) |
| CMRID.013 | As a Commission Handler / Processor, I need only DP accounts confirmed fully paid by the upstream PR data allowed to proceed to billing, with the basis of each decision, so that BDOI never bills commission on unpaid accounts. | FR-OP-091 | FR-OP-091 AC1-4 | TC-OP-091.1, 091.2, 091.3 (5 cases) |
| CMRID.014 | As a Commission Handler / Processor, I need production reports per branch and insurer with consolidated yearly data, so that production is analysed by branch and insurer. | FR-OP-097 | FR-OP-097 AC1-2 | TC-OP-097.1, 097.2 (3 cases) |
| CMRID.015 | As a Commission Handler / Processor, I need to tag BIR withholding tax certificates to the ORs and submit them to Comptrollership for acknowledgement, so that certificates are tracked and acknowledged without paper transmittals. | FR-OP-096 | FR-OP-096 AC1-2 | TC-OP-096.1, 096.2 (3 cases) |


# Storyboard index

Each step of the four walkthroughs of chapter 14 is a frame of the storyboard: the persona, the screen, what the user does, what the user sees and the result, the screenshot of the step in chapter 14, the FRs it shows and the slide of the step in the 07 UX Screen Deck of this set. The walkthroughs run in this order on the SIT environment with seed data. The screens are those reviewed with v2.0; where version 2.1 changes a step (for example the direct payment billing of WT-C, FR-OP-091 to FR-OP-093), the FR column names the FR that states the change.

## WT-A A booked invoice from the client's payment to the insurer's official receipt

<!-- table: widths=1.4,2.4,2.8,3.6,3.8,1.6,2.4,1.2 caption="Storyboard of walkthrough WT-A" size=7 -->
| Frame | Persona | Screen | Action | What the user sees / outcome | Screenshot | FR | UX deck slide |
|---|---|---|---|---|---|---|---|
| WT-A.1 | Processing (New Business, BRD-1) | SCR-NB-29 Book Account (New Business) | Opens the account from Ready to Book, checks the invoice and the journal preview and clicks Book Account | The Booked Invoice with its invoice number; the invoice is copied into the Operations ledger. Result: Booked | - | FR-OP-004 | - |
| WT-A.2 | Cashier | SCR-OP-02 Invoice Search | Searches the ARN on Invoice Search | The new invoice with its gross premium, outstanding premium, Unpaid and With Outstanding Balance. Result: In the ledger | - | FR-OP-004, FR-OP-005 | - |
| WT-A.3 | Cashier | SCR-OP-08 Receive Payment | Opens Receive Payment, enters the ARN, the payor, the amount paid and the payment date, and checks the preview | Application Preview: Booked, the invoice with each component and the amount applied to it, Excess to Unapplied 0.00. Result: Preview | - | FR-OP-011, FR-OP-018, FR-OP-019 | - |
| WT-A.4 | Cashier | SCR-OP-10 Receipt | Clicks Issue AR and Apply | Message: <AR> issued and applied to 1 invoice(s); the receipt opens with its application by component. Result: AR issued and applied | - | FR-OP-011, FR-OP-019, FR-OP-027 | - |
| WT-A.5 | Remittance Processor | SCR-OP-24 Extraction | Enters the invoice number on Extraction and clicks Run Extraction, then opens the run | The run tags the invoice Extracted with its paid AR and the batch number. Result: Extracted into a batch | - | FR-OP-030, FR-OP-031 | - |
| WT-A.6 | Remittance Processor | SCR-OP-26 Remittance Batch | Opens the batch, checks the totals strip and the account, clicks Preview and Submit and submits the batch | The batch is For Approval; the history shows the submission. Result: For approval | - | FR-OP-032, FR-OP-033 | - |
| WT-A.7 | Remittance TL / TH | SCR-OP-26 Remittance Batch | Opens the batch from For Approval and clicks Approve and Push | The batch is Approved - with Disbursement with its payment request and the commission OR. Result: Approved | - | FR-OP-035 | - |
| WT-A.8 | Disbursement | SCR-OP-36 Disbursement Queue | Acknowledges the payment request and assigns the DV number | The request is on the DV Assigned tab with its DV number. Result: DV assigned | - | FR-OP-036, FR-OP-120 | - |
| WT-A.9 | Remittance Processor | SCR-OP-27 Insurer OR Upload | Uploads the insurer's schedule with its OR number and opens the upload run | Records Read 1, OR Matches Paid PR 1; the account shows the insurer OR as Matched. Result: Insurer OR received | - | FR-OP-037 | - |
| WT-A.10 | Remittance Processor | SCR-OP-03 Invoice 360 | Opens Invoice 360 of the invoice, Remittances tab | Paid and Fully Remitted; the remittance batch of the invoice. Result: Closed | - | FR-OP-005, FR-OP-036 | - |

## WT-B Cancellation of a paid and remitted policy, from the request to the refund

<!-- table: widths=1.4,2.4,2.8,3.6,3.8,1.6,2.4,1.2 caption="Storyboard of walkthrough WT-B" size=7 -->
| Frame | Persona | Screen | Action | What the user sees / outcome | Screenshot | FR | UX deck slide |
|---|---|---|---|---|---|---|---|
| WT-B.1 | Marketing Collection | SCR-OP-38 New Endorsement Request | Opens New Request, searches the policy number and ticks the invoice | The booked invoice with its policy number, ARN, assured, insurer, payment and lock; the policy details of the invoice. Result: Policy chosen | - | FR-OP-050 | - |
| WT-B.2 | Marketing Collection | SCR-OP-38 New Endorsement Request | Chooses Financial – Change of Cover, Flat Cancellation, the reason, the effective date and the description, and clicks Recompute | Recompute & Submit: the premium and commission change, before and after per component and per insurer. Result: Recomputed | - | FR-OP-050, FR-OP-054 | - |
| WT-B.3 | Marketing Collection | SCR-OP-39 Endorsement Request | Clicks Submit for Validation | The request opens For Validation with the Policy No., ARN and Invoice chips; the invoice is locked by Adjustment. Result: For validation | - | FR-OP-050, FR-OP-006 | - |
| WT-B.4 | Adjustment Processor | SCR-OP-39 Endorsement Request | Opens the request from the workbench and clicks Validate | The request is For Approval. Result: Validated | - | FR-OP-053 | - |
| WT-B.5 | Adjustment TL | SCR-OP-39 Endorsement Request | Opens the request and clicks Approve | The request is For Posting. Result: Approved | - | FR-OP-053 | - |
| WT-B.6 | Adjustment Processor | SCR-OP-40 Posting Batches | Ticks the request on Posting Batches and clicks Post Selected | The batch result: the request Posted. Result: Posted | - | FR-OP-056 | - |
| WT-B.7 | Adjustment Processor | SCR-OP-39 Endorsement Request | Opens the Policy Transactions tab of the request and the journal of the last transaction | The original booking, the cancellation with its premium, taxes and commission change and gross premium 0.00 after it, the refund of the excess payment to the client, and the journal lines of the re-applied payment. Result: Policy history | - | FR-OP-057, FR-OP-061 | - |
| WT-B.8 | Cashier | SCR-OP-12 Unapplied Payment | Opens the unapplied payment left by the re-application, assigns a refund to the client and submits it | The disposition is For Approval. Result: Refund for approval | - | FR-OP-022 | - |
| WT-B.9 | Cashiering TL / TH | SCR-OP-12 Unapplied Payment | Opens the unapplied payment and clicks Approve | The disposition is Completed and the balance 0.00; the refund is sent to Disbursement. Result: Refund approved | - | FR-OP-022 | - |
| WT-B.10 | Disbursement | SCR-OP-36 Disbursement Queue | Opens the Disbursement Queue | The refund request to the client in To Acknowledge. Result: Refund with Disbursement | - | FR-OP-120 | - |

## WT-C Direct payment commission and the reconciliation of the insurer's production

<!-- table: widths=1.4,2.4,2.8,3.6,3.8,1.6,2.4,1.2 caption="Storyboard of walkthrough WT-C" size=7 -->
| Frame | Persona | Screen | Action | What the user sees / outcome | Screenshot | FR | UX deck slide |
|---|---|---|---|---|---|---|---|
| WT-C.1 | Commission Handler / Processor | SCR-OP-54 DP Billing | Opens the billing, clicks Record Answers, marks the account Approved and saves | The billing is Approved by Insurer - to Collect. Result: Insurer approved | - | FR-OP-093 | - |
| WT-C.2 | Commission Handler / Processor | SCR-OP-54 DP Billing | Clicks Record Collection with the collection date and the bank account | The billing is Closed - PR Reversed with the commission OR. Result: Collected | - | FR-OP-094 | - |
| WT-C.3 | Commission Handler / Processor | SCR-OP-03 Invoice 360 | Opens Invoice 360 of the direct payment invoice, Movements tab | The premium receivable reversal and the commission applied. Result: PR reversed | - | FR-OP-094, FR-OP-005 | - |
| WT-C.4 | Production Reconciliation Handler | SCR-OP-45 Reconciliation Cycle | Opens the cycle, the With Discrepancy item, and records the company concerned, the disposition and the feedback | The item review with the BDOI and insurer values side by side and the gross premium difference. Result: Feedback recorded | - | FR-OP-077 | - |
| WT-C.5 | Production Reconciliation Handler | SCR-OP-45 Reconciliation Cycle | Selects the insurer-only item and sets its disposition | The item Unmatched No Booking with its disposition. Result: Disposition set | - | FR-OP-077, FR-OP-076 | - |
| WT-C.6 | Production Reconciliation Handler | SCR-OP-48 Unbooked Accounts | Opens Unbooked Accounts | The policy the insurer reported, not booked by BDOI, with its disposition. Result: Followed | - | FR-OP-076 | - |

## WT-D Controls and the messages the user sees

<!-- table: widths=1.4,2.4,2.8,3.6,3.8,1.6,2.4,1.2 caption="Storyboard of walkthrough WT-D" size=7 -->
| Frame | Persona | Screen | Action | What the user sees / outcome | Screenshot | FR | UX deck slide |
|---|---|---|---|---|---|---|---|
| WT-D.1 | Marketing TL / UH | SCR-OP-29 Remittance Hold | Marketing Collection raises a hold on the invoice with its reason and hold-until date; the Marketing Team Leader opens it and clicks Approve hold | The hold is On Hold; the invoice is flagged On Hold and stays out of the extraction. Result: Hold approved | - | FR-OP-111 | - |
| WT-D.2 | Marketing Collection | SCR-OP-38 New Endorsement Request | Searches on New Request the invoice that is in the remittance batch for approval | The invoice reads Locked by Remittance and cannot be selected; a request on it is refused with the message Invoice <invoice> is locked by Remittance. Result: Refused | - | FR-OP-006, FR-OP-050 | - |
| WT-D.3 | Cashier | SCR-OP-10 Receipt | Opens a receipt, clicks Cancel Receipt, chooses the reason and submits the cancellation | The receipt shows the cancellation For Approval in its workflow header. Result: Cancellation requested | - | FR-OP-013 | - |
| WT-D.4 | Cashiering TL / TH | SCR-OP-10 Receipt | Opens the receipt and clicks Approve and Post | The receipt is Cancelled; its unapplied balance is closed. Result: Cancelled | - | FR-OP-013 | - |
| WT-D.5 | Remittance TL / TH | SCR-OP-26 Remittance Batch | Submits the batch in review for approval, then tries to approve it | Message: Batch <batch> must be approved by another user. Result: Refused | - | FR-OP-035 | - |


# Appendix: Screen standards

The Operations screens follow the screen standards of BIBS. They are the same standards in every FRS and are listed here so that BDOI can agree the look of the screens once and check the screenshots against it; they are not repeated in each screen specification.

<!-- table: widths=4.2,13.4 caption="Screen standards of BIBS" -->
| Area | Standard |
|---|---|
| Record pages | Back arrow and breadcrumb, title, then the record header: name, reference chips, status pill, flags (for example On Hold or Pending Negative Adjustment on an invoice) and the key facts. Page actions on the right in one order: secondary, primary, and the destructive action last and apart. |
| Workflow header (step bar) | Every record with a workflow shows a stepper under the record header: the stages of the main path in their order, passed stages ticked in blue, the current stage highlighted, the stages ahead in grey with their number. A returned or on-hold stage shows in amber after the stage it came from; a rejected, cancelled or voided record shows that stage in red and the path ends there; the last stage turns green when it is reached. Under the stepper one row gives Current Stage, Since, Due (with the Overdue pill) and Assigned To, with the actions of the stage on the right. The history is a table (Stage, From Stage, Action, By, Date and Time, Remarks, Duration in Stage), newest first. |
| Messages (notice standard) | One standard for errors, warnings, information and success: a white notice with a thin bar and an icon in the colour of its kind (red, amber, blue, green), a bold short title, then the business message with one bullet per missing item. No codes, internal references or technical terms in what the user reads. Field errors appear under the field; a long form lists its errors at the top with a link to the first field. Only an unexpected system error offers Retry and a reference for support behind Details. Special instructions are one short information notice. |
| Tables, not highlight boxes | Records and lists of records are rows of a titled table (for example the application of a receipt: Invoice, Component, Amount Applied); key data is a label and value grid. A coloured box carries a short message only, never a record. |
| Tables | Header row in BDO blue that stays in view; rows alternate white and Background Blue, with a light hover and a stronger blue for the selected row; a list scrolls inside its card, the card as high as its rows and never higher than the window; text left, amounts right, dates in one format; one value per cell with at most one muted line under it; a dash for an empty value; the kind of a mixed list of records in a Type column; a due date on one line, in red with the warning icon once past due; ages and durations in words (for example 3 days); "Showing x to y of n results" with page numbers. A table fits the page: related values share a column (one under the other) rather than scrolling sideways. |
| Row actions | The actions on a record of a list are in one row action menu at the end of the row (the three-dot button), never buttons or links in the row; a destructive action is listed last, in red, and asks for confirmation. |
| Status labels | Statuses are outlined pills (a border and text in the colour of the state group on a light tint) of one size and never wrap. The full label is shown (for example Review in Process, With Outstanding Balance); an agreed short form, with the full label in the tooltip, only for a label longer than 21 characters. |
| Labels and names | List values are shown by their label, never by their code; products by name with the code as a second line; insurers by name; users by their names, never by their user ID. |
| Dates, amounts and rates | One date picker; dates entered and shown as dd-MMM-yyyy, times as dd-MMM-yyyy HH:mm (Philippine time). Amounts with two decimals and thousand separators, negatives in brackets, the currency in the column header or before the amount. Rates as percentages with two to four decimals, as keyed. |
| Period cell | A period (period of cover, FFY, production month range, hold period) is shown in lists on two lines, the start date and "to" the end date, each date kept whole; an open end reads "to open". In a sentence or a label and value grid it is one line: "20-Oct-2026 to 20-Oct-2027". |
| Forms | Labels above the fields, fields of one height on one line, required fields marked, the error under the field, and the actions in one place (Cancel, then the main action). Format hints only; other guidance is in the tooltip of the label. |
| Uploads | Download Template next to the upload; a drop zone with the accepted types and maximum size; every row checked before anything is saved; Rows Read, Valid and Rejected with the rows; valid rows are processed and the rejected rows are returned in the error file (the template layout with an Error column and the wrong cells highlighted); Upload Corrected File keeps the link to the first upload; a file uploaded before is refused. |
| Confirmations and reasons | Every approve, authorise, post, release, cancel, void or deactivate asks for confirmation in a dialog that names the record and the effect. Every reject, return, cancel and void needs a reason (from its list where there is one) before it can be confirmed. Destructive actions are confirmed with the red button. The reason is kept in the history and sent with the notification. |
| Notifications | The bell shows the unread count and opens the panel grouped by day (Today, Yesterday, then the date), each notice with its title, one-line summary, record reference and time; Mark Read, Mark All Read and View All; the Notifications page lists every notice with filters. |
| Documents | Generated documents carry the BDO Insure letterhead, the document name and reference, and a business footer with "Confidential" and page x of y. |

The project team checks each screen against these standards before UAT and records the result in the screen readiness checklist. A screen found not to follow a standard during the review is recorded as Change requested in the sign-off workbook and corrected before UAT without a change request. The presentation choices that BDOI is asked to confirm are items of the clarifications chapter.
