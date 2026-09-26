---
# Source of the Functional Requirements Specification for BRD-4 Collections.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD04_COLLECTIONS.md
title: Collections
subtitle: BRD-4 Collections (Collection Management System BRD, renumbering addendum and Workshop Addendum)
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-04
name: Collections
doc_id: BIBS-FRS-BRD-04
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-4 Collections
output: FRS/BIBS_FRS_BRD-04_Collections_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-4 baseline and the build design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Operations Head (pending)
    change: First issue for BDOI review; aligned with the as-built Collections module (waves C0, C1-A, C1-B, C1-C) and the cross-BRD decisions
distribution:
  - {name: "VP and Head, BDOI Operations", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Section Heads, Corporate and Retail Marketing", role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Collection Team Leads and Collection Handlers", role: Business user, organisation: BDOI, purpose: "Review of the worklist, disposition, promise and escalation FRs"}
  - {name: "Marketing Account Officers and Marketing Handlers", role: Business user, organisation: BDOI, purpose: Review of the collector FRs}
  - {name: "Operations Cashiering", role: Business user, organisation: BDOI, purpose: Review of the unapplied-payment and hand-off FRs}
  - {name: "Commission Receivables Unit (CRU)", role: Business user, organisation: BDOI, purpose: "Review of the DP list and commission receivable FRs"}
  - {name: "Comptrollership, Disbursement and ACSL", role: Viewer, organisation: BDOI, purpose: Review of the read access and reports}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Collections business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

Collections is built. Where the delivered behaviour differs from the BRD text, the FR describes the delivered behaviour and records the difference in a note; section 1.6 lists all differences in one table.

## Scope

The scope is the follow-up of **premium receivables (PR)** and **unapplied payments** by Marketing: the collection worklist per invoice, assignment, collection efforts, collector dispositions, promises to pay, installments, escalation, billing statements, the collector side of unapplied payments, and the scheduled files that hand accounts to Operations. It also covers the commission receivable (CR) billing gate of the signed addendum and records the four draft rows on commission and incentives.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Access and audit | Role-based access, encoded-by and updated-by, field-level audit log and its extraction, record lock | BRCLXN.019-020, 043-044; NFR 3, 4, 9 |
| Outstanding PR worklist | List at invoice level, threshold, exclusions, totals, filters, net PR breakdown, daily refresh, history of completed accounts | BRCLXN.001-015, 021-023, 046 |
| Collector work | PR collector dispositions and their list, collection efforts, bulk update, reassignment | BRCLXN.016-018, 051, 052 |
| Installments and promises | Installment plans, payment history, promises to pay and broken promises, policy and co-insurance view, transaction history | BRCLXN.053-057 |
| Escalation | Escalation rules, automatic and manual escalation, escalation workflow | BRCLXN.049, 050, 055 |
| Billing statements | Statements of account (SOA) per billing cycle; billing is monitoring only | BRCLXN.058, 060 |
| Unapplied payments | Unapplied list, filters, disposition list, collector disposition, request application, invoice validation, history, daily application file | BRCLXN.030-042, 047, 048 |
| Scheduled files and reports | Monthly and weekly "DP PR for reversal" and "PR 2307 for reversal" files, daily PR reports, Collections reports | BRCLXN.024-029, 045; p.59-61 |
| Commission receivable | CR billing only after the premium is confirmed paid; draft rows on incentives, refunds and mixed payments | BRCLXN.059; 061-064 (draft) |

**Out of scope for this phase:**

- Posting of money. Collections posts no journal and no ledger movement. Payments, BIR 2307 reversals, direct payment (DP) reversals and cancellations are posted by Cashiering, Commission and Adjustment (BRD-2).
- Holds, special remittances, the send-schedule request and the endorsement slip (MKTID.001-009). The CLXN BRD does not cover them; they stay in Operations as built (OQ45).
- The Marketing Diary kept in ISYS (p.40-42). There is no FR ID for it; the panel is parked until BDOI answers CQ20.
- Transport of the daily application file to the BDOI file server FS04. The file is written to the in-system extract repository until OQ17 is answered.
- BRCLXN.061-064 as built functions. They are in the unsigned draft addendum only (CQ01). Section 4.10 specifies them from the design so that BDOI can confirm them; they are not built.

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Collection Management System (CMS) BRD, pages 35-61 of the BRD-4 pack (signed printout pp.66-93) | v1, 17-Jan-2025 to 10-Mar-2025; signed Feb to Mar 2025 | `docs/source-documents/Collections (CLXN) BRD.pdf` |
| R2 | Renumbering addendum (FRID-001-048 to BRCLXN.001-048), pages 23-34 | 17-Dec-2025; approved 19 to 22-Dec-2025 | same file |
| R3 | Collections Addendum (Workshop), signed version, pages 13-22 (BRCLXN.049-060) | v1.0, 10-Apr-2026; approved 16 to 17-Apr-2026 | same file |
| R4 | Collections Addendum (Workshop), draft version, pages 1-12 (BRCLXN.049-064; 061-064 only here) | v1.0 draft, 05 to 10-Apr-2026 | same file |
| R5 | BDOI Collections (BRD-4) requirements baseline and fit/gap | current | `docs/requirements/BDOI_CLXN_BRD_SPEC.md` |
| R6 | Collections build design, including section 14 and the as-built notes of waves C1-A, C1-B and C1-C | current | `docs/architecture/COLLECTIONS_DESIGN.md` |
| R7 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R8 | BRD-2 Operations FRS (Cashiering, Commission Receivables, Remittance) | v1.0 | `docs/deliverables/out/Drop-1_Transactional/FRS/BIBS_FRS_BRD-02_Operations_v1.0.docx` |
| R9 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |

Page references in this document ("p.26") are pages of the BRD-4 PDF. For BRCLXN.001-048 the first page is the renumbering addendum (R2, the governing wording) and the second page is the FRID table of the CMS BRD (R1), for example "p.26 / 47". For BRCLXN.049-060 the page is the signed addendum (R3); for BRCLXN.061-064 the page is the draft (R4).

<!-- pagebreak -->

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing)
AR: Acknowledgement Receipt issued by Cashiering for a payment
ARN: Account reference number of a booked account (one ARN, one or more invoices)
BRCLXN: Requirement ID prefix of the Collections BRD (BRCLXN.001-064)
CMS: Collection Management System, the name of the application in the CMS BRD (R1)
Collection account: The collection record of one booked invoice in the worklist ("collection item" in the build)
Collection Handler: Marketing staff who follow up premium receivables for an assigned market segment
CQnn: Open question on BRD-4 raised by the project team (section 10.3)
CR: Commission receivable, billed to insurers by the Commission Receivables Unit
CRU: Commission Receivables Unit (Operations)
CTE: Credit term extension requested from the insurer
CWT: Creditable withholding tax (2%) withheld by the client and evidenced by BIR Form 2307
DP: Direct payment, a premium the client paid directly to the insurer
DTIP: Due to insurer (premium payable to the insurer)
EBIX / QPS: Legacy booking and accounting systems named in the CMS BRD
FRID: Requirement ID of the CMS BRD before renumbering (FRID-001-048)
FS04: BDOI file server named in BRCLXN.041-042
Hand-off: An item Collections places in its outbox for Cashiering or Commission (check pick-up, BIR 2307 tag, DP list)
LOV: List of values maintained by an authorised user
PR: Premium receivable (outstanding premium of a booked invoice)
PR2307: The 2% of the premium receivable held open until the client's BIR 2307 certificate is received
SOA: Statement of account (billing statement per billing cycle)
Tagging owner: Whether the next action on an account is Marketing's or Operations' (p.41)
UH: Unit Head (Marketing)
UPP: Unapplied payment, a payment Cashiering received but could not apply to an invoice
```

## How to read the functional requirements

Each FR in section 4 has the same parts:

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R5), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**.
- **Business rules**. *Configurable* rules are maintained by an authorised user in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check (for example a blank mandatory field); its message follows the same wording but has no business code. Text in angle brackets (`<invoice>`) is replaced by the value.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-4 test plan.

API paths start with `/api/v1/collections` unless another path is given; "..." in a header table stands for that prefix.

The CMS BRD states that "unless otherwise stated, all requirements are considered high priority and committed for this phase" (p.37). BRCLXN.001-048 therefore carry the priority **High**. The signed addendum gives **Must have** for BRCLXN.049 and 051-060 and no priority for BRCLXN.050.

> [!NOTE]
> Values marked "to confirm" (threshold, disposition values, effort codes, escalation defaults) are placeholders that BDOI confirms through the open questions in section 10.3. They are configuration, so a changed answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R5)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Worked with the platform before BRD-4 |
| CONFIGURE | Needed set-up only (parameters, lists, rules) |
| CHANGE | Extended an existing capability |
| NEW | A capability that did not exist before BRD-4 |

## Differences between the built behaviour and the BRD

The table lists where the delivered behaviour differs from the BRD text or fills a gap the BRD leaves open. Each difference is also a note or rule in its FR. None of them removes a BRD requirement; most wait for an answer from BDOI and are configuration.

<!-- table: widths=2.2,5.4,6.8,2.2 caption="Recorded differences (built behaviour against the BRD)" size=8.5 -->
| BRD ID | BRD says | BIBS does | Ref. |
|---|---|---|---|
| BRCLXN.013-015 | Daily batch extracts all data from EBIX after the 22:00 EOD | EBIX is replaced by BIBS booking. Job CLX_DAILY_REFRESH reads the BIBS invoice ledger at 22:15; the balance of a listed account is also refreshed after each payment. Legacy EBIX open items are a one-time migration (not built) | CQ07 |
| BRCLXN.010 | Exclude negative balances of transaction type C | Negatives of cancellations and return invoices are never listed (EXCLUDED_CANCELLED); other negatives are kept as CREDIT on a Credit Balances tab | CQ04 |
| BRCLXN.012 | Show only the accounts of the selected segment and UH | Server-side filters. There is no default data scope per user: every CLX_VIEW user sees every account and filters | CQ06 |
| BRCLXN.011 | Filter by Unit Head | The Unit Head is the head of the invoice's sales unit (else of its department or region), maintained on Collections Setup | CQ05 |
| BRCLXN.024, 026 | Monthly batch "generates the file" | The file is also a hand-off: a DP PR disposition goes at once to the Commission DP list, a PR 2307 disposition to Cashiering's 2307 intake. The monthly file is the audit copy | OQ16, OQ38 |
| BRCLXN.026 | 2307 accounts reach Operations for reversal | Cashiering does not yet pull the COLLECTION_CWT2307 hand-offs; they stay pending and the alert CLX_OUTBOX_STALE reports them. Cashiering's 2307 upload remains the path | R6 C1-A |
| BRCLXN.030, 032 | Request application of payment | The request goes to Cashiering's Incoming Requests; a cashier accepts it and applies the payment through the Operations disposition workflow and its approvals | OQ15 |
| BRCLXN.036 | UH, AO and bank officer shown as names; booker name; Processing Stage, Business Origin, Client Code Match, System Remarks | User names are shown for UH and AO; booker and bank officer names, Business Origin and System Remarks are not shown; "Processing Stage" is the Cashiering tab | CQ11 |
| BRCLXN.041-042 | Text file loaded to FS04 before 06:00 | The pipe-delimited file is written at 05:00 to the in-system repository folder FS04/CLX_APPLICATION_TO_INVOICE; the transport to FS04 is parked | OQ17, CQ12 |
| BRCLXN.047 | Invoice number is "I" and 8 digits | The pattern CLX_INVOICE_NO_PATTERN accepts the EBIX format and the BIBS format (BI-...), and the invoice must exist in the BIBS ledger | CQ13 |
| BRCLXN.051 | Bulk update of several invoices | The worklist updates several accounts with a disposition or an effort (with remarks) and reassigns them; promises and escalations are recorded in bulk on their screens. The upload CLX_BULK_UPDATE records promises and escalations; its disposition, effort and remarks columns are refused (CLX_WORKLIST_UNAVAILABLE) | Gap G1 |
| BRCLXN.053 | Installment schedule per account | Plans are made in Collections (policy years, generated or manual); installment terms do not come from the quotation or the account | CQ15 |
| BRCLXN.055 | Detect broken promises per installment | A promise is KEPT, PARTIALLY_KEPT or BROKEN by the payments applied between the day of the promise and the promised date plus CLX_PROMISE_GRACE_DAYS (0) | CQ16 |
| BRCLXN.056 | Delivery date and receipt date | The first AR date is shown as the receipt date; the delivery date is blank | CQ17 |
| BRCLXN.059 | CR billing needs Premium Receivable confirmation | Commission bills a DP account only after the CRU reviewer confirms it "fully paid to the insurer" (CMRID.013); there is no separate automatic PR confirmation record | OQ38 |
| BRCLXN.060 | Aging from billing statements, invoice dates and policy periods | CLX_AGING_BASIS takes BOOKING or INCEPTION; aging from the SOA due date is not built | CQ14 |
| BRCLXN.061-064 | Draft requirements on commission and incentives | Specified, not built, until BDOI confirms the draft (CQ01) | CQ01 |
| NFR 3.04 | A user may not hold several roles | Several roles per user are allowed (decision D5) | R7 D5 |
| NFR 1.04, 2.02 | Lock-out after 3 invalid attempts | Built for all BIBS users (LOGIN_MAX_FAILED_ATTEMPTS = 3) | R7 D5 |
| p.93 caveat | Exports may slow the system | Exports run in the background, capped at CLX_EXPORT_MAX_ROWS, under their own permission CLX_EXPORT | FR-CL-084 |

# Business context and process overview

## Business context

BDOI Marketing collects premium receivables from clients through Account Officers, Marketing Handlers and dedicated Collection Handlers, with Team Leads, Unit Heads and Section Heads. Today the work runs on EBIX and QPS, on a "Marketing Diary" in ISYS, on e-mail and on Excel lists (p.40-41). The CMS BRD asks for one application in which collectors record their dispositions on premium receivables and unapplied payments, and the workshop addendum adds escalation, bulk update, reassignment, installments, promises, billing statements and the commission receivable gate (p.13-20).

In BIBS the booked invoice and its PR balance by component already live in the Operations invoice ledger. Collections is therefore a BIBS module that reads the ledger and hands work to Cashiering and Commission inside the application. It never keeps a second copy of the money.

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.40-42)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | The AO records the client's payment instruction in the Marketing Diary (ISYS) | The Marketing Diary stays in ISYS until CQ20 is answered; collection efforts and remarks are kept in BIBS |
| 2 | The Collection Handler takes the outstanding-PR list for the segment from EBIX | BIBS lists every invoice with PR above the threshold each night and assigns it to a handler by rule |
| 3 | The handler follows up new accounts within 10-15 days and secures a commitment within 60 days | The handler logs efforts and promises to pay; escalation rules flag aging and broken promises |
| 4 | Commitments outside the credit term go to the AO and the insurer (CTE), then to the UH | Accounts are escalated automatically or by hand to the TL or UH through the escalation workflow |
| 5 | On the 60th day the handler sets a disposition (cancel, coordinate further, bank AO help) and a category A / B / C | The handler records a collector disposition from the maintained list; its category and tagging owner follow the list |
| 6 | Deposit, bills payment and direct payment are followed up with Cashiering and insurers | The handler sees payments and unapplied payments in BIBS and asks Cashiering to apply a payment |
| 7 | 2% CWT stays a Marketing action until the BIR 2307 is collected | The disposition "PR 2307 for reversal" becomes an Operations action and is handed to Cashiering |
| 8 | Operations extracts the Direct Payment Report every month | The disposition "DP PR for reversal" goes to the Commission DP list at once; monthly and weekly files are produced for audit |

## Process overview

The table lists the steps of the collection cycle, and Figure 1 shows them by actor.

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Booking | Processing (BRD-1) | The invoice is booked; its PR by component, DTIP, commission and co-insurance shares go to the invoice ledger | - |
| 2 | Nightly refresh | System | New invoices with PR above the threshold become collection accounts; balances, aging and status are refreshed; paid accounts complete | BRCLXN.001-015, 046 |
| 3 | Assignment | System, Team Lead | New accounts go to a handler by rule, else to their AO; the TL reassigns permanently or temporarily | BRCLXN.052 |
| 4 | Follow-up | Collection Handler, AO | Efforts, remarks, category, promises to pay, installment plans and billing statements | BRCLXN.016-023, 053-058 |
| 5 | Escalation | System, handler, TL, UH | Rules raise escalations for aging, missing commitments, broken promises and overdue installments; users escalate by hand | BRCLXN.049, 050, 055 |
| 6 | Hand-off to Operations | Handler, then Cashiering / Commission | Check pick-up and BIR 2307 tags to Cashiering; DP accounts to the Commission DP list; cancellations raised in Adjustment | BRCLXN.024-029 |
| 7 | Unapplied payments | Collection Handler, Unapplied Payment Handler, then Cashiering | Collector disposition; a request to apply the payment to a valid invoice; daily application file | BRCLXN.030-042, 047, 048 |
| 8 | Files and reports | System | Daily PR reports, weekly and monthly reversal files, Collections reports | BRCLXN.024-029, 045 |
| 9 | Commission receivable | CRU (Commission) | CR billing only for DP accounts confirmed fully paid to the insurer | BRCLXN.059 |

![Collection cycle by actor (BRCLXN.001-060)](figures/brd04_process_flow.dot)

## Collection account statuses

Each invoice that has ever been listed keeps one collection account. The refresh sets its status from the ledger; users never set it.

<!-- table: widths=4.4,6.2,6 caption="Statuses of a collection account" status=Status -->
| Status | Meaning | Set when |
|---|---|---|
| OPEN | Premium to collect above the threshold | The total of the six PR components plus PR2307 is above CLX_MIN_BALANCE_THRESHOLD |
| COMPLETED | Paid, or at or below the threshold | The total falls to or below the threshold; the account reopens when the balance comes back |
| EXCLUDED_CANCELLED | Negative balance of a cancellation or return invoice | The total is negative on a cancellation or return invoice (BRCLXN.010) |
| CREDIT | Negative balance of any other invoice | The total is negative on another invoice (CQ04) |

An invoice without a client receivable (payment status NOT_APPLICABLE: direct payment and return invoices) is never listed.

# Personas and roles

## Personas

<!-- table: widths=3.2,4.4,7,2.4 caption="Personas and BIBS roles" size=8.5 -->
| Persona | BIBS role | Responsibilities in Collections | BRD |
|---|---|---|---|
| Marketing AO | MKT_AO | Follows up own accounts; efforts, dispositions, promises, manual escalation; unapplied dispositions | p.43 |
| Marketing Team Lead | MKT_TL | As the AO, plus bulk update, assignment, billing statements and acting on escalations | p.43; BRCLXN.049-052 |
| Marketing Handler | MKT_HANDLER | As the AO, plus bulk update | p.43 |
| Collection Handler | MKT_COLLECTION | Works the assigned accounts, unapplied payments, installment plans and billing statements | p.43; BRCLXN.030-031 |
| Collection Team Lead | CLX_TL | As the handler, plus assignment and acting on escalations | BRCLXN.049, 052, 055 |
| Section Head (Corporate / Retail) | MKT_SECTION_HEAD | As the TL, plus Collections set-up (threshold, disposition options) and the audit log | p.44; BRCLXN.007, 017 |
| Unapplied Payment Handler | UNAPPLIED_HANDLER | Dispositions and application requests on unapplied payments | BRCLXN.032-033 (CQ10) |
| Processing Unit | PROCESSOR | Disposition "no / missing policy number" only | p.44 |
| Operations Cashiering | CASHIER, CASHIER_TL | Read access and exports; executes the application requests on its Incoming Requests screen | p.44 |
| Disbursement, Comptrollership, ACSL | DISBURSEMENT, COMPTROLLERSHIP, ACSL_PROCESSOR, ACSL_TL, ACSL_HEAD | Read access and reports (Disbursement also exports) | p.45 |
| Application Support, Business Administrator, DCO | APP_SUPPORT, BUSINESS_ADMIN, DCO | Collections set-up, audit log, interface (sync) view and job runs | p.45-46 |
| System Administrator, Auditor | SYSADMIN, AUDITOR | Read access, set-up (SYSADMIN) and audit log | NFR 4 |

Every new Collections role also holds WORK_VIEW, ATTACHMENT_VIEW, REPORT_VIEW, CLIENT_VIEW, ACCOUNT_VIEW and OPS_VIEW, because the collection account links to Invoice 360. The roles are the project's proposal until BDOI confirms the access matrix (OQ48). Several roles per user are allowed (decision D5 of R7).

<!-- pagebreak -->

## Permissions

<!-- table: widths=4.6,2.6,9.6 caption="Collections permissions and action classes" -->
| Permission | Action class | Allows |
|---|---|---|
| CLX_VIEW | VIEW | Collections Home, PR Worklist, collection account, client view, completed accounts (read only) |
| CLX_WORK | CREATE / AMEND | Efforts, promises, PR dispositions and remarks; takes the edit lock of an account |
| CLX_BULK_UPDATE | AMEND | Update several accounts at once, in the grid or by upload (BRCLXN.051) |
| CLX_ESCALATE | CREATE | Manual escalation (BRCLXN.050) |
| CLX_ESCALATION_HANDLE | APPROVE | Acknowledge, escalate further, return or resolve an escalation |
| CLX_ASSIGN | AMEND | Assignment rules and reassignment (BRCLXN.052) |
| CLX_UNAPPLIED_WORK | CREATE | Collector disposition and application request on unapplied payments (BRCLXN.030-033) |
| CLX_BILLING | CREATE | Installment plans and billing statements (BRCLXN.053, 058) |
| CLX_SETUP | AMEND | Threshold and other parameters, disposition rules, Unit Heads, escalation rules (BRCLXN.007, 017, 037, 049) |
| CLX_EXPORT | VIEW | Download of scheduled files and exports (caveat p.93) |
| CLX_REPORT_VIEW | VIEW | Collections reports and the Files screen |
| CLX_AUDIT_VIEW | VIEW | Collections audit log (BRCLXN.044) |

## Permissions matrix

The table below is the role-to-permission matrix delivered with the build ("Y" = granted). The System Administrator changes it through role-permission change requests; the User Access Matrix screen shows it by permission and by action class.

<!-- table: widths=4.2,1.23,1.23,1.23,1.23,1.23,1.23,1.23,1.23,1.23,1.23,1.23 caption="Role-to-permission matrix for Collections (proposal until OQ48)" size=7.5 -->
| Permission | MKT AO | MKT TL | MKT Hdlr | Coll. Hdlr | CLX TL | Sect. Head | UPP Hdlr | Proc. | Cash-ier | Disb. / Compt. / ACSL | Admin / DCO |
|---|---|---|---|---|---|---|---|---|---|---|---|
| CLX_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| CLX_WORK | Y | Y | Y | Y | Y | Y | | Y | | | |
| CLX_BULK_UPDATE | | Y | Y | Y | Y | Y | | | | | |
| CLX_ESCALATE | Y | Y | Y | Y | Y | Y | | | | | |
| CLX_ESCALATION_HANDLE | | Y | | | Y | Y | | | | | |
| CLX_ASSIGN | | Y | | | Y | Y | | | | | |
| CLX_UNAPPLIED_WORK | Y | Y | Y | Y | Y | Y | Y | | | | |
| CLX_BILLING | | Y | | Y | Y | Y | | | | | |
| CLX_SETUP | | | | | | Y | | | | | Y |
| CLX_EXPORT | Y | Y | Y | Y | Y | Y | | | Y | Disb. | |
| CLX_REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | | Y | Y | |
| CLX_AUDIT_VIEW | | | | | | Y | | | | | Y |

"Admin / DCO" is APP_SUPPORT, BUSINESS_ADMIN and DCO; they also hold FLOWIN_MANAGE and SYSTEM_MONITOR for the interface and job views (APP_SUPPORT also LOV_MANAGE). SYSADMIN holds CLX_VIEW, CLX_SETUP, CLX_AUDIT_VIEW and CLX_REPORT_VIEW; AUDITOR holds CLX_VIEW, CLX_REPORT_VIEW and CLX_AUDIT_VIEW.

Segregation of duties enforced by the system: the maker of an escalation rule never authorises it; a disposition reserved to roles (attribute `allowed_roles`) is refused for other roles; updating several accounts at once needs CLX_BULK_UPDATE whatever other permission the user holds.

# Functional requirements

## Access, audit and record lock

```fr
id: FR-CL-001
title: Restrict each Collections action to authorised roles
brd: [Stakeholder functions (p.43-46), NFR 3.01-3.05 (p.52)]
actor: System
priority: High
fit: CHANGE
screens: All Collections screens; User Access Matrix
api: Every endpoint checks its permission; GET /api/v1/nbadmin/access-matrix/by-action
description:
  - Every Collections screen, button and API call requires a permission (section 3.2). The permissions carry an action class (VIEW, CREATE, AMEND, APPROVE) in the area COLLECTIONS, so the User Access Matrix shows who may view, work, bulk update, assign, escalate and set up.
  - Menus show only the screens the user's roles allow. Collections is a section of the Finance group, listed before Cashiering.
  - Users log in with the platform log-in of BRD-1. After three consecutive failed attempts the account locks (NFR 1.04, 2.02); the lock-out applies to all BIBS users.
preconditions:
  - "The user is logged in."
main_flow:
  - The user opens a Collections screen or starts an action.
  - BIBS checks the user's permissions for that screen or action.
  - BIBS shows the screen or performs the action.
alternate_flows:
  - No permission. The screen is not in the menu; a direct link returns "You do not have access to this page"; an API call is refused (HTTP 403) and logged.
  - Reserved disposition. A disposition value reserved to other roles is refused even when the user holds CLX_WORK (FR-CL-031).
rules:
  - [R1, "Roles are granted permissions as in section 3.3 until BDOI confirms the matrix (OQ48).", Configurable, Role-permission change request]
  - [R2, "A user may hold several roles. The BRD rule of one role per user (NFR 3.04) is not built (decision D5).", Fixed, "-"]
  - [R3, "Lock-out after LOGIN_MAX_FAILED_ATTEMPTS consecutive failed log-ins (default 3).", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
  - [R4, "BDO single sign-on with Windows credentials (NFR 1.01) is not part of this phase (Q42).", Fixed, "-"]
validations:
  - [Action without permission, You are not permitted to perform this action, ACCESS_DENIED]
  - [Account locked after failed log-ins, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - Refused API calls are logged with user, endpoint and time; every log-in attempt is logged with its source address.
acceptance:
  - A Disbursement user sees the PR Worklist read only and cannot record a disposition.
  - A Marketing AO does not see Collections Setup or Assignments.
  - The User Access Matrix lists the CLX_* permissions under the area COLLECTIONS with their action classes.
  - A user is locked after three consecutive wrong passwords.
```

```fr
id: FR-CL-002
title: Show who encoded and who updated each record
brd: [BRCLXN.019 (p.28 / 48), BRCLXN.020 (p.28 / 48)]
actor: System
priority: High
fit: FIT
screens: Collection account (Dispositions & Efforts, History); Promises to Pay; Installment Plans; Escalations; Unapplied Payment
api: "GET .../items/{invoiceNo}/dispositions, /efforts, /history"
description: Every Collections record (disposition, effort, promise, plan, escalation, reassignment, unapplied disposition, application request) carries the user ID and time of creation and of the last update. The screens show them as "Recorded by" or "By" with the date.
preconditions:
  - "None."
main_flow:
  - A user saves a Collections record.
  - BIBS stores the user ID and the time with the record.
  - The lists and tabs of the record show the user and time.
rules:
  - [R1, "The user ID is taken from the session; it cannot be entered or changed.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "The user and time are part of the record; field changes are also in the audit log (FR-CL-003)."
acceptance:
  - A disposition recorded by clxhandler shows clxhandler and the time on the Dispositions & Efforts tab.
  - A promise changed by another user shows that user as the last updater.
```

```fr
id: FR-CL-003
title: Keep a field-level audit log of Collections changes
brd: [BRCLXN.043 (p.32 / 49), NFR 4.03 (p.52)]
actor: System
priority: High
fit: CHANGE
screens: Collection account (History); Collections Setup
api: GET .../items/{invoiceNo}/history
description:
  - Every change a user makes to a Collections record is written as one audit row per field, with the field, the value before ("From"), the value after ("To"), the user ID, the time, the source IP address and, for a bulk change, the bulk reference. This covers accounts (handler, disposition, category, tagging owner, remarks), assignment rules, parameters, disposition rules, Unit Heads and unapplied dispositions.
  - The History tab of the collection account shows the field changes of that account in time order.
preconditions:
  - "None."
main_flow:
  - A user changes a Collections record.
  - BIBS writes the change rows in the same transaction as the change.
  - The History tab and the audit log report (FR-CL-004) show the rows.
alternate_flows:
  - Bulk change. Every account of a bulk update gets its own rows with the shared bulk reference (CLXBU-yyyy-n).
rules:
  - [R1, "Audit rows are append-only; no user can change or delete them.", Fixed, "-"]
  - [R2, "Changes made by the nightly refresh and by the inbox are system changes and carry the user SYSTEM.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - Changing the category of an account from A to B writes one row with From A, To B, the user, the time and the source IP.
  - Changing CLX_MIN_BALANCE_THRESHOLD on Collections Setup writes a row with the old and new value.
  - No screen or API allows an audit row to be edited or deleted.
```

```fr
id: FR-CL-004
title: View and extract the Collections audit log
brd: [BRCLXN.044 (p.32 / 49), NFR 4.05 (p.52)]
actor: Section Head; Application Support; Business Administrator; DCO; Auditor
priority: High
fit: CONFIGURE
screens: Report Centre (Collections Audit Log)
api: Report CLX-AUDIT-LOG
description: Authorised users run the report Collections Audit Log for a period on screen and export it to Excel, PDF, ODS or CSV. It lists every field change of FR-CL-003.
preconditions:
  - "The user has CLX_AUDIT_VIEW."
main_flow:
  - The user opens the Report Centre and selects Collections Audit Log.
  - The user enters the company and the period.
  - BIBS shows the rows; the user exports them.
rules:
  - [R1, "Viewing and exporting the audit log need CLX_AUDIT_VIEW.", Fixed, "-"]
validations:
  - [Period end before start, The end date must be on or after the start date, "-"]
fields_screen: Collections Audit Log (parameters)
fields:
  - [Company, List, "Yes", Companies of the user, "-"]
  - [From / To, Date, "Yes", "-", "Default: start of month to today"]
notifications:
  - "None."
audit:
  - "The run and each export are recorded in the report archive with user, time and parameters."
acceptance:
  - A Section Head exports the audit log of a month to Excel with the columns Changed On, User ID, Source IP, Record, Reference, Field, From, To and Bulk Reference.
  - A Collection Handler cannot open the report.
```

```fr
id: FR-CL-005
title: Lock a collection account while a user edits it
brd: [NFR 9.03 (p.53)]
actor: Collection user; System
priority: High
fit: CHANGE
screens: Collection account
api: "POST / DELETE .../items/{invoiceNo}/lock"
description: When a user with CLX_WORK opens a collection account, BIBS takes an edit lock on it for CLX_EDIT_LOCK_MINUTES. Another user who tries to change the account while the lock is held is refused with "<user> is editing <invoice>" and sees a banner on the account. The lock is released when the holder leaves the account or when it expires.
preconditions:
  - "The user has CLX_WORK."
main_flow:
  - The user opens a collection account.
  - BIBS records the user and time as the lock holder.
  - The user records efforts, dispositions or remarks.
  - The user leaves the account; BIBS releases the lock.
alternate_flows:
  - Another user holds the lock. The account opens read only with the banner "<user> is editing"; changes are refused.
  - The lock expires. After CLX_EDIT_LOCK_MINUTES the next user takes the lock.
rules:
  - [R1, "Lock time CLX_EDIT_LOCK_MINUTES, default 15 (1-240).", Configurable, Parameter CLX_EDIT_LOCK_MINUTES]
  - [R2, "The nightly refresh and the inbox update a locked account; the lock applies to users only.", Fixed, "-"]
validations:
  - [Change while another user holds the lock, "<user> is editing <invoice>", CLX_ITEM_LOCKED]
notifications:
  - "None."
audit:
  - "The lock holder and time are stored on the account."
acceptance:
  - While clxhandler has an account open, mkttl cannot record a disposition on it and sees "clxhandler is editing".
  - After 15 minutes without release, mkttl can take the lock.
```

## Outstanding PR worklist

```fr
id: FR-CL-010
title: Generate the outstanding PR list at invoice level
brd: [BRCLXN.001 (p.26 / 47), BRCLXN.002 (p.26 / 47), BRCLXN.004 (p.26 / 47)]
actor: Collection user
priority: High
fit: "CHANGE (001), FIT (002, 004)"
screens: PR Worklist; Collection account
api: "GET .../worklist; GET .../items/{invoiceNo}"
description:
  - BIBS keeps one collection account per booked invoice with outstanding premium receivable. The PR Worklist lists them, one row per invoice, with client, assured, invoice, policy, ARN, booking and inception dates, aging and aging bracket, insurer, segment, sales unit, Unit Head, AO, handler, net outstanding, PR2307, current disposition, category, tagging owner, promise and escalation flags.
  - The outstanding PR comes from the Operations invoice ledger (booked + adjusted - applied + reversed - remitted - written off, per component). Collections reads it and never recalculates it.
  - Tabs group the accounts by status - Open Accounts, Credit Balances, Completed Collections, Excluded, All - with the count of each.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user opens the PR Worklist.
  - BIBS shows the Open Accounts tab, sorted by aging.
  - The user filters, searches or groups the list (FR-CL-014, FR-CL-015).
  - The user opens an account to see its details.
rules:
  - [R1, "One collection account per invoice; the key is the invoice number within the company.", Fixed, "-"]
  - [R2, "The list is at invoice level (BRCLXN.001); FRID-001 said cover-number level. The totals per account and client are FR-CL-014 (CQ02).", Fixed, "-"]
  - [R3, "Aging counts from the booking or inception date (CLX_AGING_BASIS) in the brackets of CLX_AGING_BRACKETS.", Configurable, Parameters CLX_AGING_BASIS and CLX_AGING_BRACKETS]
  - [R4, "Lists are paged on the server (at most 200 rows per page).", Fixed, "-"]
validations: []
fields_screen: PR Worklist (filters)
fields:
  - [Status tab, Tabs, "Yes", "Open Accounts, Credit Balances, Completed Collections, Excluded, All", "-"]
  - [Search, Text, "No", "-", "Invoice, policy, ARN, client or assured"]
notifications:
  - "None."
audit:
  - "Read only; the exports of FR-CL-084 are recorded in the report archive."
acceptance:
  - An invoice booked today with a premium of 25,000.00 appears on the Open Accounts tab after the nightly refresh, with its net outstanding of 25,000.00.
  - After a payment of 10,000.00 is applied, the account shows 15,000.00 outstanding.
  - A user with CLX_VIEW only can open the list and an account but has no work actions.
```

```fr
id: FR-CL-011
title: Apply the minimal balance threshold
brd: [BRCLXN.005 (p.26 / 47), BRCLXN.008 (p.26 / 47), BRCLXN.009 (p.26 / 47)]
actor: System
priority: High
fit: CHANGE
screens: PR Worklist
api: Job CLX_DAILY_REFRESH
description: An invoice enters the worklist as OPEN when its total to collect (the six PR components plus PR2307) is above CLX_MIN_BALANCE_THRESHOLD. At or below the threshold, a new invoice is not listed and a listed account moves to COMPLETED. A completed account reopens when its balance comes back above the threshold (for example after a payment reversal).
preconditions:
  - "The threshold parameter exists (default 10.00)."
main_flow:
  - The refresh reads the total to collect of each invoice.
  - BIBS compares it with CLX_MIN_BALANCE_THRESHOLD.
  - BIBS lists the invoice as OPEN, completes it, or leaves it out.
rules:
  - [R1, "Included when the total to collect is above the threshold (strictly greater).", Fixed, "-"]
  - [R2, "Excluded when the total is zero or at or below the threshold; the account is not deleted (FR-CL-018).", Fixed, "-"]
  - [R3, "Threshold CLX_MIN_BALANCE_THRESHOLD, default 10.00, in the invoice currency; the value is to be confirmed (CQ03).", Configurable, Parameter CLX_MIN_BALANCE_THRESHOLD]
  - [R4, "The Collections threshold is separate from the Cashiering minimal-balance write-off rule (MIN_BALANCE_AUTO_MAX, OQ11).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Status changes by the refresh are system changes on the account timeline."
acceptance:
  - With the threshold at 10.00, an invoice with 10.01 outstanding is listed and one with 10.00 is not.
  - A listed account paid down to 8.00 moves to Completed Collections at the next refresh.
```

```fr
id: FR-CL-012
title: Maintain the threshold and the Collections parameters
brd: [BRCLXN.006 (p.26 / 47), BRCLXN.007 (p.26 / 47)]
actor: Section Head (Corporate); Application Support; Business Administrator; DCO; System Administrator
priority: High
fit: CONFIGURE
screens: Collections Setup (Parameters)
api: "GET .../setup; PUT .../setup/parameters/{key}"
description: The minimal balance threshold and the other Collections parameters are stored in BIBS and changed by authorised users on Collections Setup. A change takes effect at the next refresh or immediately for the checks that read it (invoice pattern, lock time, export cap).
preconditions:
  - "The user has CLX_SETUP."
main_flow:
  - The user opens Collections Setup, card Parameters.
  - The user changes a value and saves.
  - BIBS validates the value against the parameter's type and limits and stores it.
alternate_flows:
  - Refresh now. The user starts a refresh of the worklist on demand (POST .../refresh) to apply a new threshold at once.
rules:
  - [R1, "Only the Collections parameters of section 9.1 can be changed on this screen.", Fixed, "-"]
  - [R2, "Aging brackets must be ascending ranges from-to or from+.", Fixed, "-"]
  - [R3, "The aging basis is BOOKING or INCEPTION.", Fixed, "-"]
validations:
  - [Parameter not of Collections, "<key> is not a Collections parameter", CLX_PARAMETER_NOT_COLLECTIONS]
  - [Aging basis not BOOKING or INCEPTION, The aging basis is BOOKING or INCEPTION, CLX_AGING_BASIS]
  - [Bracket ends before it starts, "Aging bracket '<text>' ends before it starts", CLX_AGING_BRACKETS_INVALID]
  - [Brackets not ascending, "Aging brackets must be in ascending order: <value>", CLX_AGING_BRACKETS_INVALID]
  - [Value outside its limits, "<parameter> must be between <min> and <max>", "-"]
fields_screen: Collections Setup, Parameters
fields:
  - [CLX_MIN_BALANCE_THRESHOLD, Amount, "Yes", "-", "0-100,000"]
  - [CLX_AGING_BASIS, List, "Yes", "BOOKING, INCEPTION", "-"]
  - [CLX_AGING_BRACKETS, Text, "Yes", "-", "Ranges such as 0-30,31-45,121+"]
  - [CLX_INVOICE_NO_PATTERN, Text, "Yes", "-", Regular expression]
  - [CLX_PROMISE_GRACE_DAYS, Number, "Yes", "-", "0-30"]
  - [CLX_EXPORT_MAX_ROWS, Number, "Yes", "-", "100-1,000,000"]
  - [CLX_EDIT_LOCK_MINUTES, Number, "Yes", "-", "1-240"]
notifications:
  - "None."
audit:
  - "Each change is a field change row with the old and new value (FR-CL-003)."
acceptance:
  - A Section Head changes the threshold to 100.00; after "Refresh now" accounts with 100.00 or less move to Completed Collections.
  - A Collection Handler does not see Collections Setup.
  - The change appears in the Collections Audit Log with From 10.00 and To 100.00.
```

```fr
id: FR-CL-013
title: Exclude cancelled negative balances and keep other credits apart
brd: [BRCLXN.010 (p.27 / 47)]
actor: System
priority: High
fit: CHANGE
screens: PR Worklist (Excluded, Credit Balances tabs); Collections Home
api: Job CLX_DAILY_REFRESH
description: An invoice with a negative total to collect is never an open account. When it is a cancellation or return invoice (the BRD's transaction type C), its account is EXCLUDED_CANCELLED. Any other negative total (for example an overpayment) is CREDIT and appears on the Credit Balances tab and tile, so that the handler can follow it up with Cashiering.
preconditions:
  - "None."
main_flow:
  - The refresh reads a negative total to collect.
  - BIBS sets EXCLUDED_CANCELLED for a cancellation or return invoice, else CREDIT.
rules:
  - [R1, "A negative cancellation or return invoice is never listed as OPEN.", Fixed, "-"]
  - [R2, "The treatment of other negatives (CREDIT view) waits for BDOI's answer to CQ04.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Status changes are on the account timeline."
acceptance:
  - The negative return invoice of a cancelled policy does not appear on the Open Accounts tab.
  - An overpaid invoice with -500.00 appears on the Credit Balances tab.
```

> [!NOTE] Difference from the BRD
> BRCLXN.010 asks only to exclude cancelled negatives. BIBS also keeps other negatives visible as CREDIT so they are not lost (CQ04).

```fr
id: FR-CL-014
title: Total the PR balance by account and by client
brd: [BRCLXN.003 (p.26 / 47)]
actor: Collection user
priority: High
fit: CHANGE
screens: PR Worklist (By Account, By Client); Client View
api: "GET .../worklist/totals; GET .../clients/{clientCode}"
description: The worklist adds up the PR balances across the invoices of an account (ARN) and of a client. "By Account" and "By Client" show one row per group with the number of invoices, the outstanding and the PR2307; the Client View shows every collection account of one client with the totals.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user switches the worklist grouping to By Account or By Client.
  - BIBS shows the groups with their totals, at most 200 groups per page.
  - The user opens a client to see the Client View.
rules:
  - [R1, "Totals are for information; the threshold is applied per invoice (CQ02).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - A client with three open invoices of 5,000.00, 7,000.00 and 3,000.00 shows 15,000.00 in By Client and in the Client View.
```

```fr
id: FR-CL-015
title: Filter the worklist by market segment, Unit Head and other criteria
brd: [BRCLXN.011 (p.27 / 47), BRCLXN.012 (p.27 / 47)]
actor: Collection user; Section Head (Unit Heads)
priority: High
fit: CHANGE
screens: PR Worklist; Collections Setup (Unit Heads)
api: "GET .../worklist?segment=&salesUnit=&unitHead=...; PUT .../setup/unit-heads/{unitCode}"
description:
  - The worklist filters on the server by market segment, sales unit, Unit Head, handler, AO, aging bracket, category, disposition, outstanding range, promise status and client. Only the accounts that match are returned and totalled.
  - The Unit Head of an account is the head of the invoice's sales unit, else the head of its department or region, from the sales organisation. Authorised users maintain the Unit Head of each sales unit on Collections Setup.
preconditions:
  - "The user has CLX_VIEW (filters); CLX_SETUP (Unit Heads)."
main_flow:
  - The user chooses the filters and applies them.
  - BIBS returns the matching accounts and their totals.
  - The user saves the filter set as a quick filter for later use.
rules:
  - [R1, "Every CLX_VIEW user sees every account and narrows it with the filters; a default scope per user is not built (CQ06).", Fixed, "-"]
  - [R2, "Unit Head resolution: sales unit head, else department head, else region head.", Configurable, Collections Setup (Unit Heads)]
validations: []
fields_screen: PR Worklist, filters
fields:
  - [Market Segment, List, "No", Market segments, "-"]
  - [Sales Unit, List, "No", Sales units, "-"]
  - [Unit Head, Text, "No", User names, "-"]
  - [Handler / Account Officer, Text, "No", User names, "-"]
  - [Aging Bracket, List, "No", CLX_AGING_BRACKETS, "-"]
  - [Category (A/B/C), List, "No", "A, B, C", "-"]
  - [Disposition, List, "No", LOV CLX_PR_DISPOSITION, "-"]
  - [Outstanding From / To, Amount, "No", "-", From <= To]
  - [Promise Status, List, "No", "Open, Kept, Partially kept, Broken", "-"]
  - [Client Code, Text, "No", Client master, "-"]
notifications:
  - "None."
audit:
  - "A change of a Unit Head is a field change row (FR-CL-003)."
acceptance:
  - Filtering on segment RETAIL and Unit Head clxuh returns only the accounts of that segment whose sales unit is headed by clxuh.
  - Changing the Unit Head of a sales unit changes the Unit Head of its accounts at the next refresh.
```

> [!NOTE] Difference from the BRD
> BRCLXN.012 can be read as a data restriction per user. BIBS applies the selected filters only; whether handlers, AOs and UHs should see only their own accounts is open (CQ06). BRD-9 answered it for CSF agents only (all accounts).

```fr
id: FR-CL-016
title: Show the net PR and its breakdown
brd: [BRCLXN.046 (p.33 / 50)]
actor: Collection user
priority: High
fit: NEW
screens: Collection account (Summary)
api: GET .../items/{invoiceNo}
description: The Summary tab of a collection account shows the net outstanding premium and how it is made up - booked premium, endorsements and adjustments, cancellations, payments applied, written off or reversed, and the net outstanding - in total and per component (basic premium, DST, VAT, LGT, other charges, PR2307). The figures are read live from the invoice ledger; the snapshot of the last refresh is kept on the account for the lists.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user opens a collection account.
  - BIBS shows the header figures (net outstanding, PR2307) and the Net PR Breakdown by Component.
  - The user opens Invoice 360 from the account for the full ledger view.
rules:
  - [R1, "Net outstanding = premium due + endorsements / adjustments - payments applied - written off / reversed.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - For an invoice of 50,000.00 with a negative endorsement of 5,000.00 and a payment of 20,000.00, the Summary shows 25,000.00 net outstanding and the three movements.
  - The component rows add up to the net outstanding.
```

```fr
id: FR-CL-017
title: Refresh the worklist every day from the invoice ledger
brd: [BRCLXN.013 (p.27 / 47), BRCLXN.014 (p.27 / 47), BRCLXN.015 (p.27 / 47; edit p.78)]
actor: System; Application Support / DCO (run log)
priority: High
fit: CHANGE
screens: PR Worklist; Collection account ("Refresh from Ledger"); Operations > Interfaces and job runs (sync view)
api: "Job CLX_DAILY_REFRESH; POST .../refresh; POST .../items/{invoiceNo}/refresh"
description:
  - The job CLX_DAILY_REFRESH runs every night at 22:15 (Manila), after the 22:00 end-of-day. It creates accounts for new invoices above the threshold, updates the balances, aging, bracket, Unit Head and flags of the listed accounts, completes accounts at or below the threshold, sets excluded and credit statuses, assigns new accounts by rule (FR-CL-020) and ends expired temporary assignments.
  - Between runs, the balance of a listed account is refreshed after each committed ledger movement or flag change (for example a payment applied), so the worklist shows same-day payments. New invoices wait for the nightly run or "Refresh from Ledger".
  - The job run history (start, end, counts, result) is the "EBIX/QPS synchronization view" of Application Support and DCO (p.45-46).
preconditions:
  - "The invoice ledger holds the booked invoices."
main_flow:
  - At 22:15 the job reads the ledger invoices of each company.
  - BIBS creates, updates, completes or excludes the collection accounts.
  - BIBS records the run with the counts of new, updated and completed accounts.
alternate_flows:
  - Run failure. The run is recorded as failed and the alert CLX_REFRESH_FAILED is raised; the next run repeats the refresh.
  - Refresh on demand. A CLX_SETUP user refreshes the whole worklist; a CLX_WORK user refreshes one account with "Refresh from Ledger".
rules:
  - [R1, "EBIX is replaced by the BIBS invoice ledger; the BRD's extraction becomes an in-app refresh.", Fixed, "-"]
  - [R2, "Schedule 22:15 Manila daily (cron 0 15 14 * * * UTC).", Configurable, Job schedule clx-daily-refresh-cron]
  - [R3, "The refresh is idempotent: running it twice gives the same result.", Fixed, "-"]
validations: []
notifications:
  - "Alert CLX_REFRESH_FAILED to the alert recipients when a run fails."
audit:
  - "Each run is in the job run history with trigger (schedule or user), times and counts."
acceptance:
  - An invoice booked during the day is on the worklist the next morning.
  - A payment applied at 10:00 reduces the account's outstanding before 10:05.
  - A failed run raises CLX_REFRESH_FAILED and is visible in the job run history.
```

> [!NOTE] Difference from the BRD
> The BRD (p.27, FRID-015 as edited by hand on p.78) describes a nightly extraction from EBIX. In BIBS the booking is internal, so the refresh reads the BIBS ledger. Open EBIX items, their disposition history and unapplied items at go-live need a one-time migration; its scope and format are open (CQ07) and the migration handler is not built.

```fr
id: FR-CL-018
title: Keep the history of accounts and dispositions
brd: [BRCLXN.021 (p.28 / 48), BRCLXN.022 (p.28 / 48), BRCLXN.023 (p.28 / 48)]
actor: System
priority: High
fit: "NEW (021, 022), CHANGE (023)"
screens: PR Worklist (Completed Collections); Collection account (Timeline, Dispositions & Efforts, History)
api: "GET .../items/{invoiceNo}/timeline, /dispositions, /history"
description: Collection accounts are never deleted. When the PR becomes zero or falls below the threshold, the account moves to COMPLETED and keeps its dispositions, efforts, promises, escalations, assignments and field changes. Dispositions are append-only - a new disposition supersedes the previous one, which stays in the history. Each balance change is noted on the account's Timeline.
preconditions:
  - "None."
main_flow:
  - The PR of an account falls to zero.
  - The refresh completes the account.
  - Users still open the account from Completed Collections and see its whole history.
rules:
  - [R1, "No delete of accounts, dispositions, efforts or field changes.", Fixed, "-"]
  - [R2, "The current disposition is a pointer to the latest one; earlier dispositions stay.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Every change remains in the history and the audit log."
acceptance:
  - A fully paid account appears under Completed Collections with its three earlier dispositions.
  - Recording a new disposition leaves the previous one on the Dispositions & Efforts tab, marked superseded.
```

## Assignment

```fr
id: FR-CL-020
title: Assign new accounts by rule
brd: [BRCLXN.052 (p.17)]
actor: System; Collection Team Lead (rules)
priority: Must have
fit: NEW
screens: Assignments (Default Assignment Rules)
api: "GET/POST .../assignment-rules; PUT .../assignment-rules/{id}; POST .../assignment-rules/{id}/active"
description: Authorised users keep default assignment rules. Each rule has a priority, criteria (market segment, sales unit, client, outstanding range, aging range) and the handler. The nightly refresh gives each new open account without a handler to the first active rule that matches, else to its AO when the AO holds CLX_WORK.
preconditions:
  - "The user has CLX_ASSIGN (rules)."
main_flow:
  - The TL adds a rule with its priority, criteria and handler.
  - The refresh tries the active rules in priority order for each new account.
  - BIBS assigns the account permanently to the handler of the first matching rule.
alternate_flows:
  - No rule matches. The account goes to its AO, or stays unassigned (Unassigned Accounts tile) when the AO cannot work collections.
  - The TL deactivates a rule; it is no longer tried.
rules:
  - [R1, "Rules are tried in ascending priority; the first match wins.", Configurable, Assignments (rules)]
  - [R2, "The handler of a rule must be an active user holding CLX_WORK.", Fixed, "-"]
validations:
  - [Rule without name, Give the rule a name, CLX_RULE_NAME]
  - [Range ends before it starts, A range must start before it ends, CLX_RULE_RANGE]
  - [Handler not eligible, "<user> is not an active collection handler", CLX_HANDLER_NOT_ELIGIBLE]
fields_screen: Default Assignment Rule
fields:
  - [Name, Text, "Yes", "-", "-"]
  - [Priority, Number, "Yes", "-", Whole number]
  - [Market Segment, List, "No", Market segments, "-"]
  - [Sales Unit, List, "No", Sales units, "-"]
  - [Client Code, Text, "No", Client master, "-"]
  - [Outstanding From / To, Amount, "No", "-", From <= To]
  - [Aging From / To (days), Number, "No", "-", From <= To]
  - [Handler, List, "Yes", Users with CLX_WORK, Active]
notifications:
  - CLX_REASSIGNED to the handler who receives accounts.
audit:
  - "Rule changes and assignments are field change rows; each assignment is on the account's history."
acceptance:
  - With a rule "segment RETAIL to clxhandler", a new Retail account is assigned to clxhandler at the next refresh.
  - A new account without a matching rule goes to its AO.
```

```fr
id: FR-CL-021
title: Reassign accounts permanently or temporarily
brd: [BRCLXN.052 (p.17)]
actor: Collection Team Lead; Marketing Team Lead; Section Head
priority: Must have
fit: NEW
screens: Assignments (Reassign by Criteria); PR Worklist (Reassign)
api: "POST .../reassignments/preview; POST .../reassignments; GET .../items/{invoiceNo}/assignments"
description:
  - A TL moves accounts to another handler to balance the workload, either by selecting them in the worklist or by criteria (client, sales unit, segment, aging, amount, current handler) after a preview of the affected accounts. The reassignment is PERMANENT, or TEMPORARY until an end date.
  - When a temporary assignment ends, the nightly refresh returns the account to the previous handler, unless a later assignment replaced it. Every reassignment keeps the previous handler, the reason and who made it, so accountability is not lost.
preconditions:
  - "The user has CLX_ASSIGN."
main_flow:
  - The TL selects accounts or enters criteria and previews the matching accounts.
  - The TL chooses the new handler, the kind (Permanent or Temporary with an end date) and the reason.
  - BIBS reassigns the accounts; several accounts share a bulk reference CLXRA-yyyy-n.
  - BIBS notifies the new and the previous handlers.
alternate_flows:
  - End of a temporary assignment. The refresh returns the account to the previous handler and notifies both.
rules:
  - [R1, "At most 2,000 accounts in one reassignment.", Fixed, "-"]
  - [R2, "A temporary reassignment needs an end date after today.", Fixed, "-"]
  - [R3, "The new handler must hold CLX_WORK.", Fixed, "-"]
validations:
  - [No account selected or matched, No account matches the selection, CLX_REASSIGN_EMPTY]
  - [Reason blank, Give the reason of the reassignment, CLX_REASSIGN_REASON]
  - [Kind not given, A reassignment is PERMANENT or TEMPORARY, CLX_REASSIGN_KIND]
  - [Temporary without a future end date, A temporary reassignment needs an end date after today, CLX_REASSIGN_END_DATE]
  - [New handler not eligible, "<user> is not an active collection handler", CLX_HANDLER_NOT_ELIGIBLE]
fields_screen: Reassign Accounts
fields:
  - [New Handler, List, "Yes", Users with CLX_WORK, Active]
  - [Kind, Option, "Yes", "Permanent, Temporary", "-"]
  - [Until, Date, Conditional, "-", Required for Temporary; after today]
  - [Reason, Text, "Yes", "-", "-"]
notifications:
  - CLX_REASSIGNED to the new and the previous handlers.
audit:
  - "Each reassignment is kept on the account (Assignments) and in the report Collection Reassignments (CLX-REASSIGNMENTS)."
acceptance:
  - A TL reassigns 40 accounts of one sales unit to mkthandler until the end of the month; after that date they return to their previous handler.
  - The history of an account shows the previous handler, the new handler, the kind, the dates, the reason and the TL.
  - A reassignment without a reason is refused.
```

## Collector dispositions and efforts

```fr
id: FR-CL-030
title: Maintain the PR collector disposition list and its rules
brd: [BRCLXN.016 (p.27 / 47), BRCLXN.017 (p.27 / 47), BRCLXN.018 (p.28 / 48)]
actor: Section Head; Application Support; Business Administrator (LOV maker-checker)
priority: High
fit: "CONFIGURE (016, 017), FIT (018)"
screens: Lists of Values (CLX_PR_DISPOSITION); Collections Setup (Disposition Rules)
api: "/api/v1/lov (type CLX_PR_DISPOSITION); GET/PUT .../setup/lov-attributes"
description:
  - The PR collector dispositions are the list of values CLX_PR_DISPOSITION. Authorised users add values and deactivate them on the LOV screen with maker-checker and effective dates. Deactivated values cannot be selected.
  - Each value has rules kept on Collections Setup - the category (A, B or C), the tagging owner (Marketing or Operations), the Operations action (none, BIR 2307 reversal, DP reversal, check pick-up, cancellation request) and the roles allowed to use it.
preconditions:
  - "The user has LOV_MANAGE (values) or CLX_SETUP (rules)."
main_flow:
  - The user adds a value with its code, label and effective date; another user approves it.
  - The user sets its rules on Collections Setup, card Disposition Rules.
  - The value appears in the Record Disposition dialog for the roles allowed.
alternate_flows:
  - Deactivate. The user sets an end date; after it the value is no longer offered and is refused on save, while old dispositions keep it.
rules:
  - [R1, "Values delivered, all to be confirmed by BDOI (CQ08): DP PR for reversal; PR 2307 for reversal; For check pick-up; Cancel account; Coordinate further; Request bank AO assistance; No / missing policy number; DP returned by insurer.", Configurable, LOV CLX_PR_DISPOSITION]
  - [R2, "Only active values within their effective dates can be selected.", Fixed, "-"]
  - [R3, "The category A / B / C of each value is not seeded until BDOI gives it (CQ08).", Configurable, Collections Setup (Disposition Rules)]
  - [R4, "No / missing policy number is reserved to PROCESSOR, MKT_COLLECTION, CLX_TL and MKT_SECTION_HEAD.", Configurable, "Disposition Rules (allowed_roles)"]
validations:
  - [Unknown value, "<code> is not a value of <type>", CLX_LOV_VALUE_UNKNOWN]
  - [Unknown attribute, "<attribute> is not an attribute of <type>", CLX_LOV_ATTRIBUTE_UNKNOWN]
  - [Invalid attribute value, "'<value>' is not a valid <attribute>", CLX_LOV_ATTRIBUTE_VALUE]
fields_screen: Collections Setup, Disposition Rules
fields:
  - [Disposition, List, "Yes", LOV CLX_PR_DISPOSITION, Existing value]
  - [Category, List, "No", "A, B, C", "-"]
  - [Tagging Owner, List, "Yes", "MARKETING, OPERATIONS", "-"]
  - [Operations Action, List, "Yes", "NONE, CWT2307_REVERSAL, DP_REVERSAL, CHECK_PICKUP, CANCEL_REQUEST", "-"]
  - [Allowed Roles, Multi-select, "No", Roles, Blank = every role with CLX_WORK]
notifications:
  - "Pending LOV changes appear in My Approvals."
audit:
  - "LOV changes are audited by the LOV module; rule changes are field change rows (FR-CL-003)."
acceptance:
  - A value added and approved on the LOV screen is offered in Record Disposition.
  - A value ended yesterday is not offered today and a direct call with it is refused.
  - A Collection Handler without an allowed role cannot choose "No / missing policy number" when the value is reserved.
```

```fr
id: FR-CL-031
title: Record a PR collector disposition
brd: [BRCLXN.016 (p.27 / 47), BRCLXN.018 (p.28 / 48), BRCLXN.021 (p.28 / 48); current process (p.40-41)]
actor: Collection Handler; Marketing AO / Handler / TL; Processing Unit (restricted value)
priority: High
fit: NEW
screens: PR Worklist (Record Disposition); Collection account (Dispositions & Efforts)
api: "POST .../dispositions; GET .../items/{invoiceNo}/dispositions, /handoffs"
description:
  - The handler records the outcome of the follow-up of one or several accounts as a disposition from the list, with remarks. The disposition sets the account's current disposition, category and tagging owner. When the value has an Operations action, BIBS hands the account to Operations (FR-CL-032) and asks for the details that hand-off needs.
  - A later disposition supersedes the current one; a hand-off of the superseded disposition that Operations has not taken yet is withdrawn.
preconditions:
  - "The user has CLX_WORK; several accounts at once need CLX_BULK_UPDATE."
  - "The account is OPEN and not locked by another user."
main_flow:
  - The handler selects one or more accounts and clicks **Record Disposition**.
  - The handler chooses the disposition; BIBS shows what it hands to Operations and the detail fields it needs.
  - The handler enters the details and remarks and confirms.
  - BIBS records the disposition on each account, updates category and tagging owner, and queues the hand-off.
alternate_flows:
  - Several accounts. All accounts get the same disposition and share a bulk reference CLXBU-yyyy-n; each account is validated on its own.
  - Cancellation request. For "Cancel account" no feed is sent; the handler raises the cancellation endorsement in Adjustment (BRD-2).
rules:
  - [R1, "At most 500 accounts in one action.", Fixed, "-"]
  - [R2, "Only active values; values with allowed roles only for those roles.", Configurable, Disposition Rules]
  - [R3, "Dispositions are append-only (FR-CL-018).", Fixed, "-"]
validations:
  - [No account selected, Select at least one account, CLX_NO_ACCOUNT]
  - [More than 500 accounts, Select at most 500 accounts at once, CLX_TOO_MANY_ACCOUNTS]
  - [Several accounts without the bulk permission, Updating several accounts at once needs CLX_BULK_UPDATE, CLX_BULK_NOT_ALLOWED]
  - [Account not open, "<invoice> is <status>", CLX_ITEM_CLOSED]
  - [Account of another company, "<invoice> belongs to another company", CLX_ITEM_OTHER_COMPANY]
  - [Value reserved to other roles, "Disposition <code> is reserved to <roles>", CLX_DISPOSITION_NOT_ALLOWED]
  - [Account locked by another user, "<user> is editing <invoice>", CLX_ITEM_LOCKED]
fields_screen: Record Disposition
fields:
  - [Disposition, List, "Yes", LOV CLX_PR_DISPOSITION, Active value allowed for the user]
  - [Detail fields, "Text / Date / Amount", Conditional, By Operations action (FR-CL-032), Required fields of the hand-off]
  - [Remarks, Long text, "No", "-", "-"]
notifications:
  - "None to Marketing; the hand-off appears in the Operations queue (FR-CL-032)."
audit:
  - "Each disposition is kept with user, time and bulk reference; the changed fields are field change rows."
acceptance:
  - A handler records "Coordinate further" on one account; it becomes the current disposition with tagging owner Marketing.
  - A handler with CLX_BULK_UPDATE records the same disposition on 20 accounts; all share one bulk reference.
  - A handler without CLX_BULK_UPDATE who selects two accounts is refused with CLX_BULK_NOT_ALLOWED.
```

```fr
id: FR-CL-032
title: Hand dispositions over to Cashiering and Commission
brd: [BRCLXN.024 (p.28 / 48), BRCLXN.026 (p.29 / 48); current process (p.40-41); MKTID.010 / 012 / 013 (BRD-2)]
actor: System; Cashiering; Commission Receivables Unit
priority: High
fit: NEW
screens: Collection account (Hand-offs to Operations); Cashiering > Check Pick-up; Commission > DP Lists; Collections Home (DP Returned by Insurer)
api: "GET .../items/{invoiceNo}/handoffs; CollectionFeed (in-app transport) for COLLECTION_CHECK_PICKUP, COLLECTION_CWT2307, COLLECTION_DP_LIST, COLLECTION_DP_RETURNED, COLLECTION_REFUND"
description:
  - A disposition with an Operations action places an item in the Collections outbox for the module that owns the money effect. Cashiering and Commission take the items through the Operations CollectionFeed port inside BIBS; the item keeps its status (PENDING, TAKEN, CANCELLED).
  - "Check pick-up: an item for the Cashiering pick-up queue with the pick-up date, address, contact person, check amount, number and bank."
  - "BIR 2307 reversal: an item for Cashiering's 2307 intake. It is queued when the tagging owner is Operations or when the path is CASH; the CERTIFICATE path needs the certificate number."
  - "DP reversal: an item for the Commission DP list with the DP list columns and the invoicing branch."
  - When an insurer returns a DP account, Commission sends it back; BIBS reopens the account, records the disposition "DP returned by insurer" with the reason and notifies the handler.
preconditions:
  - "A disposition with an Operations action was recorded (FR-CL-031)."
main_flow:
  - BIBS creates the outbox item with an idempotent key (PU, CWT or DP, invoice and disposition).
  - BIBS publishes that the feed has pending items.
  - The consuming module takes the item and processes it with its own rules.
  - The account's Hand-offs to Operations panel shows the status of each item.
alternate_flows:
  - Superseded. A later disposition withdraws a PENDING item (CANCELLED).
  - Not taken. An item pending for more than a day raises CLX_OUTBOX_STALE.
  - DP returned. The Commission return reopens the account (FR-CL-018) with the disposition DP_RETURNED.
rules:
  - [R1, "Collections posts no journal and no ledger movement; Cashiering and Commission post the effects.", Fixed, "-"]
  - [R2, "The pick-up date is today or later; the check amount is required.", Fixed, "-"]
  - [R3, "The 2307 path is CASH or CERTIFICATE.", Fixed, "-"]
  - [R4, "Stale threshold 1 day.", Configurable, Exception code CLX_OUTBOX_STALE]
validations:
  - [Pick-up date in the past, Give a pick-up date from today on for the check pick-up, CLX_PICKUP_DATE]
  - [Check amount missing, Give the check amount, CLX_PICKUP_AMOUNT]
  - [2307 path not CASH or CERTIFICATE, The 2307 path is CASH or CERTIFICATE, CLX_CWT_PATH]
  - [Required detail missing, "Give <detail>", CLX_DISPOSITION_DETAIL]
  - [Detail not a date or an amount, "'<value>' is not a date (yyyy-mm-dd) / an amount", CLX_DISPOSITION_DETAIL]
fields_screen: Record Disposition (detail fields)
fields:
  - [Pick-up Date, Date, Conditional, "-", "Check pick-up; today or later"]
  - [Pick-up Address, Text, Conditional, "-", Check pick-up]
  - [Contact Person, Text, "No", "-", "-"]
  - [Check Amount, Amount, Conditional, "-", "Check pick-up; > 0"]
  - [Check No. / Check Bank, Text, "No", "-", "-"]
  - [Path, List, Conditional, "CASH, CERTIFICATE", BIR 2307 reversal]
  - [Certificate No., Text, Conditional, "-", Required for CERTIFICATE]
  - [Certificate Period From / To, Date, "No", "-", From <= To]
  - [2% Amount, Amount, "No", "-", ">= 0"]
notifications:
  - CLX_DP_RETURNED to the handler when an insurer returns a DP account.
  - Alert CLX_OUTBOX_STALE when an item is not taken within a day.
audit:
  - "Outbox and inbox items keep their key, fields, status and times; the consuming module logs the take in its flow-in run."
acceptance:
  - A "For check pick-up" disposition creates a request in Cashiering's Check Pick-up queue with the date and address given.
  - A "DP PR for reversal" disposition appears on the Commission DP list.
  - A DP account returned by the insurer is open again in the handler's worklist with the disposition "DP returned by insurer".
```

> [!WARNING] Open item
> Cashiering does not yet take the COLLECTION_CWT2307 items from the Collections outbox; its 2307 upload stays the path. The items stay PENDING and CLX_OUTBOX_STALE reports them until the Cashiering owner adds the pull (R6, C1-A).

```fr
id: FR-CL-033
title: Log collection efforts, remarks and the tagging category
brd: ["Stakeholder functions - manage collection effort transactions (p.43-46)", "Report fields Last Collection Effort Date, Code, Remarks (p.59)", "Categories A, B, C (p.41 / 61)"]
actor: Collection Handler; Marketing AO / Handler / TL
priority: High
fit: NEW
screens: PR Worklist (Log Effort, Update Remarks and Category); Collection account (Dispositions & Efforts)
api: "POST .../efforts; PUT .../items/{invoiceNo}/details"
description: The handler logs each collection effort (call, e-mail, visit, SOA sent, others) with its date and time, channel, contact person and remarks. The latest effort's date, code and remarks appear on the worklist and in the reversal reports. The handler also keeps the account's remarks and its tagging category A, B or C.
preconditions:
  - "The user has CLX_WORK; several accounts at once need CLX_BULK_UPDATE."
main_flow:
  - The handler selects accounts and clicks **Log Effort**.
  - The handler chooses the effort code and enters the time, channel, contact person and remarks.
  - BIBS records the effort on each account and updates its last effort.
alternate_flows:
  - Remarks and category. The handler updates the remarks and the category with **Update Remarks and Category**.
rules:
  - [R1, "Effort codes delivered, to be confirmed (CQ08): Phone call; E-mail; Client visit; Statement of account sent; Others (see remarks).", Configurable, LOV CLX_EFFORT_CODE]
  - [R2, "An effort cannot be dated in the future.", Fixed, "-"]
  - [R3, "The category is A, B or C.", Fixed, "-"]
validations:
  - [Effort dated in the future, An effort cannot be in the future, CLX_EFFORT_FUTURE]
  - [Category not A B or C, "The tagging category is A, B or C", CLX_CATEGORY]
  - [Several accounts without the bulk permission, Updating several accounts at once needs CLX_BULK_UPDATE, CLX_BULK_NOT_ALLOWED]
fields_screen: Log Collection Effort
fields:
  - [Effort, List, "Yes", LOV CLX_EFFORT_CODE, Active value]
  - [Date and time, Date-time, "Yes", "-", Not in the future]
  - [Channel, Text, "No", "-", "-"]
  - [Contact Person, Text, "No", "-", "-"]
  - [Remarks, Long text, "No", "-", "-"]
notifications:
  - "None."
audit:
  - "Efforts are append-only and kept with user and time; category and remarks changes are field change rows."
acceptance:
  - A logged call appears on the Dispositions & Efforts tab and as the last effort on the worklist row.
  - An effort dated tomorrow is refused.
```

> [!NOTE] Capabilities without an FR ID
> The Marketing Diary (p.40-42) and the credit term extension (CTE, p.38-41) appear only in the process diagrams. The CTE is recorded as an effort with remarks until BDOI decides whether it is tracked in BIBS; the Marketing Diary panel waits for CQ20.

```fr
id: FR-CL-034
title: Update several accounts at once
brd: [BRCLXN.051 (p.17)]
actor: Collection user with CLX_BULK_UPDATE
priority: Must have
fit: NEW
screens: PR Worklist (bulk actions); Promises to Pay; Escalations; Bulk Uploads (Collections Bulk Update)
api: "POST .../dispositions, /efforts (several invoices); POST .../bulk/promises; POST .../bulk/escalate; bulk handler CLX_BULK_UPDATE"
description:
  - Authorised users update several invoices at once. In the PR Worklist the selected accounts get the same disposition or effort (each with remarks) or are reassigned; on Promises to Pay and Escalations several invoices get the same promise or escalation. Each account is validated and saved on its own and the result lists the outcome per invoice.
  - The upload template "Collections Bulk Update" takes one row per invoice with a promise (date, amount, day of the promise), an escalation (Escalate = Y, level, user, reason) and worklist fields (disposition, effort code, remarks). Each row is validated and committed on its own; the upload number is the bulk reference of every record.
preconditions:
  - "The user has CLX_BULK_UPDATE (and CLX_ESCALATE for escalations)."
main_flow:
  - The user selects accounts in the worklist, or downloads the template and fills it.
  - The user runs the action, or uploads the file.
  - BIBS validates each account or row and applies the valid ones.
  - BIBS shows the outcome per invoice; the upload has a row report with the errors.
alternate_flows:
  - Partial success. Invalid rows are rejected with their reasons; valid rows are saved.
rules:
  - [R1, "At most 500 invoices in one grid action.", Fixed, "-"]
  - [R2, "A row needs a promise, Escalate = Y or a worklist field.", Fixed, "-"]
  - [R3, "Before and after values of every affected record are logged with the bulk reference (FR-CL-003).", Fixed, "-"]
validations:
  - [Row with nothing to do, "Fill a promise, Escalate = Y or a worklist field", "-"]
  - [Promise amount without date, A promise amount needs the promise date, "-"]
  - [Invoice not in the ledger, "Invoice <no> is not in the ledger", "-"]
  - [Nothing to collect, "Invoice <no> has nothing to collect", CLX_NOTHING_TO_COLLECT]
  - [Escalation without permission, You are not allowed to escalate accounts (CLX_ESCALATE), "-"]
  - [USER escalation without user, "Name the user in Escalate To for a USER escalation", "-"]
  - ["Disposition, effort or remarks column filled in the upload", "Dispositions, efforts and remarks are updated through the collection worklist, which is not available", CLX_WORKLIST_UNAVAILABLE]
fields_screen: Collections Bulk Update (template)
fields:
  - [Invoice No, Text, "Yes", Invoice ledger, Collection account]
  - [Promise Date / Promise Amount / Promised On, Date / Amount / Date, "No", "-", Amount needs the date]
  - [Escalate, Yes/No, "No", "-", "Default N"]
  - [Escalation Level, List, "No", "TL, UH, SECTION_HEAD, USER", "Default TL"]
  - [Escalate To, Text, Conditional, Users with CLX_ESCALATION_HANDLE, Required for USER]
  - [Escalation Reason, List, "No", LOV CLX_ESCALATION_REASON, "-"]
  - [Disposition / Effort Code / Remarks, List / List / Text, "No", "LOV CLX_PR_DISPOSITION, CLX_EFFORT_CODE", Refused in the upload today (gap G1)]
notifications:
  - "As for the single actions (CLX_ESCALATED, CLX_REASSIGNED)."
audit:
  - "Each record carries the bulk reference; the upload keeps its row report."
acceptance:
  - A TL logs the same effort on 30 accounts in one action; the result lists 30 successes and each account shows the effort.
  - An upload with 10 promise rows and 1 row for a paid invoice saves 10 promises and reports 1 error.
  - Every account of a bulk action shows the bulk reference in its History.
```

> [!WARNING] Gap G1 (built behaviour)
> The upload template accepts the disposition, effort and remarks columns, but the worklist has not implemented the port the upload uses for them, so those columns are refused with CLX_WORKLIST_UNAVAILABLE. Promises and escalations upload correctly, and the worklist bulk actions cover dispositions and efforts with their remarks. The fix is a small change in the Collections module (implement `WorklistUpdates`).

## Installments, promises and account views

```fr
id: FR-CL-040
title: Create installment plans for multi-year and installment accounts
brd: [BRCLXN.053 (p.17-18)]
actor: Collection Handler; Collection / Marketing Team Lead; Section Head
priority: Must have
fit: NEW
screens: Installment Plans (New Installment Plan); Installment Plan; Installments Due
api: "POST .../plans/policy-years, /generated, /manual; GET .../plans, /plans/{id}, /plans/installments/due; POST .../plans/{id}/cancel"
description:
  - An installment plan gives the due dates and amounts an account is followed up on. It has one of three bases. POLICY_YEARS takes every policy-year invoice of a multi-year account (booked, or still scheduled in booking) and splits each year into the cycles of the billing frequency within its coverage year. GENERATED splits the outstanding of one invoice into N equal installments from a first due date. MANUAL takes installments entered by the user, which must add up to the outstanding.
  - Each installment is NOT_DUE, DUE, OVERDUE, PARTIAL or PAID. An installment past its due date and not fully paid is flagged overdue, can escalate the account (FR-CL-051) and appears on Installments Due. The plan completes when every installment is paid.
  - A plan is monitoring only. It changes neither the booking nor the GL (BRCLXN.060).
preconditions:
  - "The user has CLX_BILLING."
  - "The account or invoice has premium to collect (not paid directly to the insurer, not cancelled)."
main_flow:
  - The user clicks **New Installment Plan** and chooses the plan basis.
  - The user enters the ARN (policy years) or the invoice, the billing frequency and, for a generated plan, the first due date and the number of installments.
  - BIBS builds the installments and numbers the plan IPL-yyyy-n.
  - The plan appears in Installment Plans; its installments appear on Installments Due as they fall due.
alternate_flows:
  - Cancel. The user cancels an active plan (for example to change the frequency) and creates a new one.
  - Manual plan. The user enters each installment's due date and amount.
rules:
  - [R1, "One live plan per invoice and per policy-year account.", Fixed, "-"]
  - [R2, "Billing frequencies Annual, Semi-annual, Quarterly, Monthly.", Configurable, LOV CLX_BILLING_FREQUENCY]
  - [R3, "At most 120 installments in one plan.", Fixed, "-"]
  - [R4, "Installment terms are entered in Collections; taking them from the quotation or the account waits for CQ15.", Fixed, "-"]
validations:
  - [Account without a booked policy year, "Account <ARN> has no booked policy year", CLX_PLAN_NOT_BOOKED]
  - [Invoice with nothing outstanding, "Invoice <no> has no outstanding premium to schedule", CLX_PLAN_NOTHING_DUE]
  - [Invoice paid directly or cancelled, "Invoice <no> is paid directly to the insurer / cancelled and has no premium to collect", CLX_INVOICE_NOT_COLLECTIBLE]
  - [Invoice not in the ledger, "Invoice <no> is not in the ledger", CLX_INVOICE_UNKNOWN]
  - [Live plan exists, "Plan <no> already schedules this account; cancel it first", CLX_PLAN_EXISTS]
  - [No installment entered, Enter at least one installment, CLX_PLAN_EMPTY]
  - [Manual amounts or dates invalid, Installment amounts must be positive and due dates increasing, CLX_PLAN_ENTRY_INVALID]
  - [Manual total differs from the outstanding, "The installments add up to <sum> but the outstanding premium is <amount>", CLX_PLAN_TOTAL_MISMATCH]
  - [Frequency without a cycle length, "Billing frequency <code> has no cycle length defined", CLX_FREQUENCY_UNSUPPORTED]
  - [Cancel a plan that is not active, "Plan <no> is <status> and cannot be cancelled", CLX_PLAN_NOT_ACTIVE]
fields_screen: New Installment Plan
fields:
  - [Plan Basis, Option, "Yes", "Policy years, Generated, Manual", "-"]
  - [Account Reference No. (ARN), Text, Conditional, Accounts, Required for Policy years]
  - [Invoice No., Text, Conditional, Invoice ledger, Required for Generated and Manual]
  - [Billing Frequency, List, Conditional, LOV CLX_BILLING_FREQUENCY, Policy years and Generated]
  - [First Due Date, Date, Conditional, "-", Generated]
  - [Number of Installments, Number, Conditional, "-", "Generated; 1-120"]
  - ["Installments (due date, amount)", Table, Conditional, "-", "Manual; dates increasing, amounts > 0, total = outstanding"]
notifications:
  - "None."
audit:
  - "Creation and cancellation are recorded with user and time on the plan."
acceptance:
  - A three-year account booked with annual policy-year invoices and a quarterly frequency gets a plan of 12 installments, four per coverage year.
  - A generated plan of 4 installments on an outstanding of 40,000.00 has four installments of 10,000.00.
  - A second live plan on the same invoice is refused with CLX_PLAN_EXISTS.
  - An unpaid installment past its due date is flagged overdue and listed on Installments Due.
```

```fr
id: FR-CL-041
title: Allocate payments to installments and show the payment history
brd: [BRCLXN.054 (p.18)]
actor: Collection user; System
priority: Must have
fit: CHANGE
screens: Collection account (Payments); Installment Plan (Installments); Installment Plans (Refresh Allocation)
api: "GET .../items/{invoiceNo}/payments; POST .../plans/{id}/refresh; job CLX_PROMISE_CHECK"
description:
  - The Payments tab of a collection account lists every payment movement of the invoice in date order - payments applied, reversals, 2307 reclassification, DP reversal and write-offs - with the AR / OR reference, and the summary of booked, paid and outstanding amounts.
  - For accounts with a plan, BIBS allocates the settled amount of each invoice (installments total less the ledger outstanding) to the installments, oldest due first. The allocation runs every night with the promise check, on **Refresh Allocation** and before a billing statement is generated.
preconditions:
  - "The user has CLX_VIEW (history); CLX_BILLING (Refresh Allocation)."
main_flow:
  - The user opens the Payments tab of an account.
  - BIBS lists the payment movements from the ledger with their dates and references.
  - For a planned account, the user opens the plan to see the paid amount and status per installment.
rules:
  - [R1, "Allocation is oldest due installment first.", Fixed, "-"]
  - [R2, "Every settlement counts: payments, reversals, 2307 reclass, DP reversal and write-offs.", Fixed, "-"]
  - [R3, "Policy years booked after the plan was made are linked to their invoice before allocation.", Fixed, "-"]
validations:
  - [Unknown billing cycle, "Plan <no> has no billing cycle <n>", CLX_CYCLE_UNKNOWN]
notifications:
  - "None."
audit:
  - "Read only; allocations are recalculated from the ledger each time."
acceptance:
  - A payment of 15,000.00 on a plan with installments of 10,000.00 marks the first installment PAID and the second PARTIAL (5,000.00).
  - The Payments tab lists the payment with its AR number and date.
```

```fr
id: FR-CL-042
title: Record and evaluate promises to pay
brd: [BRCLXN.055 (p.18), BRCLXN.053 (p.17-18)]
actor: Collection user; System (promise check)
priority: Must have
fit: NEW
screens: Promises to Pay (Record Promise, Withdraw Promise); Installment Plan (Promises)
api: "POST .../promises; GET .../promises, /promises/by-invoice/{no}; POST .../promises/{id}/cancel; POST .../bulk/promises; job CLX_PROMISE_CHECK"
description:
  - The handler records the client's promise to pay an invoice, or one installment of it - the day the promise was made, the promised payment date and the amount (blank = the whole outstanding). A new promise on the same invoice replaces the running one.
  - Every night the job CLX_PROMISE_CHECK (22:45) checks the promises whose promised date plus CLX_PROMISE_GRACE_DAYS has passed. It compares the payments applied between the day of the promise and that deadline with the promised amount - KEPT when the amount was paid or nothing is left to collect, PARTIALLY_KEPT when part was paid, else BROKEN. A broken promise notifies the recorder and the AO and triggers the broken-promise escalation rules (FR-CL-051).
preconditions:
  - "The user has CLX_WORK; several invoices need CLX_BULK_UPDATE."
main_flow:
  - The handler clicks **Record Promise** and enters the invoice (or chooses an installment), the day of the promise, the promised date and the amount.
  - BIBS records the promise as OPEN.
  - After the promised date plus the grace days, the promise check sets KEPT, PARTIALLY_KEPT or BROKEN.
alternate_flows:
  - Withdraw. The handler selects open promises and clicks **Withdraw Promise**; they become CANCELLED.
  - New promise. A new promise on the same invoice evaluates an expired one or supersedes the running one.
rules:
  - [R1, "The day of the promise cannot be in the future; the promised date cannot be before it.", Fixed, "-"]
  - [R2, "The promised amount is positive and at most the outstanding.", Fixed, "-"]
  - [R3, "Grace days CLX_PROMISE_GRACE_DAYS, default 0 (0-30); the kept / broken rule is to be confirmed (CQ16).", Configurable, Parameter CLX_PROMISE_GRACE_DAYS]
  - [R4, "Schedule 22:45 Manila daily.", Configurable, Job schedule clx-promise-check-cron]
validations:
  - [Promised date missing or day of promise in the future, Enter the promised date; the day of the promise cannot be in the future, CLX_PROMISE_DATES]
  - [Promised date before the day of the promise, The promised date cannot be before the day of the promise, CLX_PROMISE_DATES]
  - [Amount not positive, The promised amount must be positive, CLX_PROMISE_AMOUNT]
  - [Amount above the outstanding, "The promised amount <amount> is above the outstanding <outstanding>", CLX_PROMISE_OVER_BALANCE]
  - [Nothing to collect, "Invoice <no> has nothing to collect", CLX_PROMISE_NOTHING_DUE]
  - [Installment of another invoice, "The installment does not bill invoice <no>", CLX_PROMISE_INSTALLMENT]
  - [Withdraw a closed promise, "The promise of <invoice> is already <status>", CLX_PROMISE_CLOSED]
fields_screen: Record Promise to Pay
fields:
  - [Invoice No. / Invoice Nos., Text, "Yes", Collection accounts, Several need CLX_BULK_UPDATE]
  - [Installment, List, "No", Installments of the plan, Of the same invoice]
  - [Promised On, Date, "Yes", "-", Not in the future; default today]
  - [Promised Payment Date, Date, "Yes", "-", On or after Promised On]
  - [Promised Amount, Amount, "No", "-", "> 0 and <= outstanding; blank = outstanding"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - CLX_PROMISE_BROKEN to the recorder and the AO when a promise is broken.
audit:
  - "Each promise keeps the recorder, the evaluation time, the amount paid in time and its final status."
acceptance:
  - A promise of 10,000.00 by 15 October with 10,000.00 applied on 14 October is KEPT after the check of 15 October.
  - A promise with 4,000.00 applied in time is PARTIALLY_KEPT.
  - A promise with nothing applied is BROKEN, the AO is notified and the account is escalated to the team lead by the broken-promise rule.
```

```fr
id: FR-CL-043
title: View policy, account, invoice and co-insurance information
brd: [BRCLXN.056 (p.18-19)]
actor: Collection user
priority: Must have
fit: CHANGE
screens: Collection account (Policy & Co-insurance, Summary); Invoice 360 (link)
api: GET .../items/{invoiceNo}/policy
description: The Policy & Co-insurance tab shows, read only, the policy number and status, the account (ARN) and client, the booking date, the first AR date (shown as the receipt date), the invoice family (the original invoice with its endorsements and cancellations) and the co-insurance shares with lead insurer, share % and amounts. The Summary tab shows the invoice figures. The **Open Invoice 360** link opens the Operations view of the invoice.
preconditions:
  - "The user has CLX_VIEW; Invoice 360 needs OPS_VIEW (granted to every Collections role)."
main_flow:
  - The user opens the Policy & Co-insurance tab of an account.
  - BIBS assembles the policy, account, invoice family and share data from the ledger, the account and the issuance records.
rules:
  - [R1, "The tab is read only; changes are made in the owning modules.", Fixed, "-"]
  - [R2, "Delivery date (e-policy dispatch or physical delivery) waits for CQ17 and is shown blank.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - A co-insured invoice shows each insurer, the lead flag and the share % adding up to 100.
  - An endorsed invoice shows the original and the endorsement invoice in Invoice Family.
```

> [!NOTE] Difference from the BRD
> BRCLXN.056 names a delivery date and a receipt date. BIBS shows the first AR date as the receipt date and leaves the delivery date blank until BDOI says which date is meant (CQ17).

```fr
id: FR-CL-044
title: View the complete transaction history of an account
brd: [BRCLXN.057 (p.19)]
actor: Collection user
priority: Must have
fit: CHANGE
screens: Collection account (Timeline)
api: GET .../items/{invoiceNo}/timeline
description: The Timeline tab merges in date order the ledger movements of the invoice (applied, adjusted, written off, DP reversal, 2307 reclass) and the Collections actions (assignments, dispositions, efforts, hand-offs, items received from Operations). Each entry shows its date, source, description, amount and user. The history is kept for audit and never deleted.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user opens the Timeline tab.
  - BIBS shows the merged entries, newest first, with their source (Ledger, Assignment, Disposition, Effort, Hand-off, From Operations).
rules:
  - [R1, "Entries are never edited or deleted.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - After a payment, a disposition and a reassignment, the Timeline shows the three entries in date order with their sources.
```

## Escalation

```fr
id: FR-CL-050
title: Maintain escalation rules
brd: [BRCLXN.049 (p.16)]
actor: Collection Team Lead (maker, with CLX_SETUP); authoriser with MASTER_AUTHORIZE
priority: Must have
fit: NEW
screens: Escalation Rules
api: "GET/POST .../escalation-rules; PUT .../escalation-rules/{id}; POST .../{id}/authorize, /{id}/deactivate; GET .../{id}/matches"
description:
  - An escalation rule says when an account is escalated automatically and to whom. It has a code, name, basis and threshold, optional filters (segment, sales unit, product line, outstanding range), the target level (TL, UH, Section Head or a named user), the reason, the SLA in hours, whether to notify, and effective dates.
  - Rules are maker-checker - a new or changed rule waits for authorisation by another user with MASTER_AUTHORIZE and appears in My Approvals. **Preview** lists the accounts the rule would escalate today.
preconditions:
  - "The maker has CLX_SETUP; the authoriser has MASTER_AUTHORIZE and is not the maker."
main_flow:
  - The maker clicks **New Rule**, enters the rule and saves.
  - BIBS stores it as pending authorisation.
  - The authoriser authorises it; the job and the broken-promise check use it from then on.
alternate_flows:
  - Deactivate. The maker deactivates a rule instead of deleting it.
  - Preview. The maker checks the accounts a rule matches before authorisation.
rules:
  - [R1, "Bases - aging from booking, aging from inception, no commitment by day N (no open promise), broken promises count, installment overdue days, amount over.", Fixed, "-"]
  - [R2, "Delivered rules, to be confirmed (CQ14) - broken promise to the team lead; 45 days from booking to the TL; 60th day from inception to the UH; installment overdue 15 days to the TL.", Configurable, Escalation Rules]
  - [R3, "The maker never authorises the rule.", Fixed, "-"]
validations:
  - [Company or code missing, Enter the company and the rule code, CLX_RULE_CODE]
  - [Threshold or SLA not positive, The threshold and the SLA hours must be positive, CLX_RULE_INVALID]
  - [USER target without user, Name the user who receives the escalations of this rule, CLX_RULE_TARGET]
  - [End before start, The rule cannot end before it becomes effective, CLX_RULE_DATES]
  - [Amount range reversed, The amount range ends below its start, CLX_RULE_AMOUNTS]
  - [Authoriser is the maker, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
fields_screen: Escalation Rule
fields:
  - [Code / Name, Text, "Yes", "-", Unique code]
  - [Basis, List, "Yes", Bases of R1, "-"]
  - [Threshold, Number / Amount, "Yes", "-", "> 0 (days, count or amount)"]
  - [Segment / Sales Unit / Product Line, List, "No", Masters, "-"]
  - [Outstanding From / To, Amount, "No", "-", From <= To]
  - [Escalate To, List, "Yes", "TL, UH, SECTION_HEAD, USER", "-"]
  - [User, List, Conditional, Users with CLX_ESCALATION_HANDLE, Required for USER]
  - [Reason, List, "Yes", LOV CLX_ESCALATION_REASON, "-"]
  - [SLA (Hours), Number, "Yes", "-", "> 0"]
  - [Effective From / To, Date, "Yes / No", "-", To >= From]
notifications:
  - "Pending rules appear in My Approvals of the authorisers."
audit:
  - "Create, change, authorise and deactivate are recorded with user, time and before / after values."
acceptance:
  - A rule "aging from booking 45 days to the TL" is not applied until another user authorises it.
  - The maker cannot authorise their own rule.
  - Preview of the authorised rule lists the accounts over 45 days old.
```

```fr
id: FR-CL-051
title: Escalate accounts automatically
brd: [BRCLXN.049 (p.16), BRCLXN.055 (p.18)]
actor: System
priority: Must have
fit: NEW
screens: Escalations; Escalation; Collections Home
api: "Job CLX_ESCALATION; event PromiseBroken"
description:
  - The job CLX_ESCALATION runs every night at 23:00, after the promise check. For each active rule it finds the accounts that meet the rule and raises one escalation per rule, invoice and month, unless an escalation of that rule is still open for the account. It then closes open escalations whose invoices are fully collected.
  - Broken-promise rules also run at once when a promise is broken, so a broken promise reaches the team lead the same night.
  - An escalation is numbered ESC-yyyy-n, kind AUTO, with the rule, reason and target, and starts the workflow CLX_ESCALATION (section 5.1).
preconditions:
  - "At least one authorised, effective rule."
main_flow:
  - The job evaluates each rule against the open accounts.
  - BIBS raises an escalation for each new match and routes it to the TL or UH stage (or to the named user).
  - BIBS notifies the target (or every holder of CLX_ESCALATION_HANDLE when there is no named user) and the AOs of the invoices.
alternate_flows:
  - Account collected. An open escalation whose invoices are collected closes automatically (auto_close).
  - Past the SLA. The alert job raises CLX_ESCALATION_OVERDUE for an escalation that stays in a stage beyond the SLA of its rule.
rules:
  - [R1, "One escalation per rule, invoice and month (idempotent key), none while one of the rule is open.", Fixed, "-"]
  - [R2, "Schedule 23:00 Manila daily.", Configurable, Job schedule clx-escalation-cron]
  - [R3, "Without a designated user the escalation waits in the stage queue; resolving the TL or UH of an account from the sales organisation is parked (CQ14).", Fixed, "-"]
validations: []
notifications:
  - CLX_ESCALATED to the target (or the stage queue) and the AOs.
  - Alert CLX_ESCALATION_OVERDUE past the SLA.
audit:
  - "Escalations keep the rule, the reason, the kind AUTO and every workflow step."
acceptance:
  - An account 46 days from booking is escalated to the TL by the 45-day rule, once in the month.
  - A broken promise raises an escalation to the team lead the same night.
  - An escalated account that is paid in full is closed automatically at the next run.
```

```fr
id: FR-CL-052
title: Escalate accounts manually
brd: [BRCLXN.050 (p.16)]
actor: Collection user with CLX_ESCALATE
priority: Not stated in the BRD
fit: NEW
screens: Escalations (Escalate Accounts); PR Worklist
api: POST .../bulk/escalate
description: A collection user escalates one or more invoices to the team lead, the unit / section head or a designated user, whatever their automated status. BIBS groups the selected invoices by account (ARN) and raises one escalation per account, kind MANUAL, with the reason and remarks. The escalation follows the same workflow as an automatic one.
preconditions:
  - "The user has CLX_ESCALATE."
main_flow:
  - The user clicks **Escalate Accounts** and enters the invoice numbers.
  - The user chooses the level or a designated user, the reason and remarks.
  - BIBS raises one escalation per account and routes it.
  - BIBS returns the outcome per invoice.
alternate_flows:
  - Invoice with nothing to collect. That invoice is reported as refused; the others are escalated.
rules:
  - [R1, "At most 500 invoices in one action.", Fixed, "-"]
  - [R2, "The invoices of one account are escalated together.", Fixed, "-"]
  - [R3, "The designated user must hold CLX_ESCALATION_HANDLE.", Fixed, "-"]
validations:
  - [Level or user missing, "Choose the level, and the user for a designated authority", CLX_ESCALATION_TARGET]
  - [User cannot handle escalations, "<user> does not handle escalations (CLX_ESCALATION_HANDLE)", CLX_ESCALATION_TARGET]
  - [Invoice with nothing to collect, "Invoice <no> has nothing to collect", CLX_NOTHING_TO_COLLECT]
fields_screen: Escalate Accounts
fields:
  - [Invoice Nos., Text list, "Yes", Collection accounts, "1-500 invoices"]
  - [Escalate To, List, "Yes", "Team lead, Unit / section head, User", "-"]
  - [User, List, Conditional, Users with CLX_ESCALATION_HANDLE, Required for User]
  - [Reason, List, "Yes", LOV CLX_ESCALATION_REASON, "-"]
  - [Remarks, Long text, "No", "-", "-"]
notifications:
  - CLX_ESCALATED to the target and the AOs.
audit:
  - "The escalation keeps the user who raised it, kind MANUAL and every workflow step; it is listed per account and in report CLX-ESCALATIONS."
acceptance:
  - Escalating five invoices of two accounts raises two escalations.
  - A user without CLX_ESCALATE does not see Escalate Accounts.
  - An account already escalated by a rule can still be escalated by hand.
```

```fr
id: FR-CL-053
title: Act on an escalation
brd: [BRCLXN.049 (p.16), BRCLXN.050 (p.16)]
actor: Team Lead, Unit Head, Section Head (CLX_ESCALATION_HANDLE); handler (resubmit)
priority: Must have
fit: NEW
screens: Escalations (inbox per stage); Escalation (workflow panel)
api: "GET .../escalations, /escalations/{id}, /escalations/by-invoice/{no}; POST .../escalations/{id}/actions/{action}"
description: The receiving level works its escalations from the Escalations inbox. It acknowledges an escalation (IN_ACTION), escalates it further to the unit / section head with a reason, returns it to the handler with an instruction, or resolves it with the resolution. A returned escalation is resubmitted by the handler once the instruction is done.
preconditions:
  - "The user has CLX_ESCALATION_HANDLE (resubmit - CLX_WORK or CLX_ESCALATE)."
main_flow:
  - The TL opens an escalation in WITH_TL and clicks **Acknowledge**.
  - The TL works the account and clicks **Resolve**, entering the resolution.
  - BIBS closes the escalation as RESOLVED and records it on the account.
alternate_flows:
  - Escalate further. The TL escalates to WITH_UH with a reason from CLX_ESCALATION_REASON.
  - Return to handler. The TL or UH returns it with a reason (RETURN_REASON); the handler resubmits it to the TL.
rules:
  - [R1, "Stages and actions are those of workflow CLX_ESCALATION (section 5.1).", Fixed, "-"]
  - [R2, "Stage SLA from the rule; default 24 hours at WITH_TL and WITH_UH, 72 at IN_ACTION, 48 at RETURNED.", Configurable, Escalation rule SLA / workflow stage]
validations:
  - [Resolve without resolution, Describe how the escalation was resolved, CLX_RESOLUTION_REQUIRED]
  - [Unknown action, "'<action>' is not an escalation action", CLX_ESCALATION_ACTION]
  - [Escalate further or return without reason, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
fields_screen: Escalation (actions)
fields:
  - [Reason, List, Conditional, "LOV CLX_ESCALATION_REASON / RETURN_REASON", Escalate further and return]
  - [Resolution, Long text, Conditional, "-", Resolve]
notifications:
  - CLX_ESCALATED to the unit / section head on escalate further; the handler on return.
audit:
  - "Every action is in the workflow history with user, time, reason and comment."
acceptance:
  - A TL acknowledges and resolves an escalation with a resolution; it closes as RESOLVED.
  - A TL cannot resolve without a resolution.
  - A returned escalation shows in the handler's Escalations and can be resubmitted.
```

## Billing statements

```fr
id: FR-CL-060
title: Generate billing statements per billing cycle
brd: [BRCLXN.058 (p.19)]
actor: Collection Handler; Team Lead (CLX_BILLING)
priority: Must have
fit: NEW
screens: Billing Statements (Billing Run); Installment Plan (Statements of Account); Statement of Account
api: "POST .../billing/statements (one cycle); POST .../billing/statements/generate-due; GET .../billing/statements, /{id}, /by-plan/{planId}, /{id}/document"
description:
  - BIBS generates a statement of account (SOA) for a billing cycle of a live installment plan. The SOA lists the installment of the cycle (CURRENT) with its coverage period and every earlier installment still unpaid (ARREARS), the payments allocated on the day (FR-CL-041) and the amount due. It is numbered SOA-yyyy-n and rendered to PDF from the template CLX_SOA, whose version is recorded.
  - A billing run generates the SOAs of every cycle of the live plans falling due in a period and not billed yet; a single cycle is billed from its plan.
preconditions:
  - "The user has CLX_BILLING; the plan is ACTIVE."
main_flow:
  - The user opens Billing Statements and clicks **Billing Run**, entering the due period.
  - BIBS refreshes the allocation, then generates one SOA per plan cycle due in the period.
  - The user opens a statement and downloads the PDF.
alternate_flows:
  - One cycle. From an installment plan the user generates the SOA of one cycle.
rules:
  - [R1, "One live SOA per plan and billing cycle.", Fixed, "-"]
  - [R2, "Amounts follow the installment of the policy period; multi-year and installment policies are supported.", Fixed, "-"]
  - [R3, "Layout, recipient and numbering are drafts until BDOI answers CQ18.", Configurable, Document template CLX_SOA]
validations:
  - [Cycle already billed, "Cycle <n> of <plan> is billed by <SOA>", CLX_SOA_EXISTS]
  - [Plan cancelled, "Plan <no> is cancelled", CLX_PLAN_NOT_ACTIVE]
  - [Period end before start, The period ends before it starts, CLX_SOA_PERIOD]
fields_screen: Billing Run
fields:
  - [Due From / Due To, Date, "Yes", "-", To >= From]
notifications:
  - "None until sent (FR-CL-061)."
audit:
  - "Each SOA keeps the generating user, time and template version."
acceptance:
  - For the three-year demo account with an annual plan, BIBS produces an SOA for each of its three billing cycles.
  - A second SOA for a billed cycle is refused until the first is cancelled.
  - The SOA shows the coverage period of the installment and any unpaid earlier installment as arrears.
```

```fr
id: FR-CL-061
title: Send or cancel a billing statement
brd: [BRCLXN.058 (p.19)]
actor: Collection Handler; Team Lead (CLX_BILLING)
priority: Must have
fit: NEW
screens: Statement of Account (Send Statement of Account, Cancel Statement of Account, E-mails Sent)
api: "PUT .../billing/statements/{id}/recipient; POST .../{id}/send, /{id}/cancel"
description: The user sends a generated SOA by e-mail. The PDF is password-protected and the password goes in a separate e-mail; the send log is kept on the statement (status SENT). A statement can be cancelled so the cycle can be billed again.
preconditions:
  - "The user has CLX_BILLING; the statement is GENERATED or SENT."
main_flow:
  - The user opens a statement and clicks **Send Statement of Account**.
  - The user enters or confirms the recipients.
  - BIBS e-mails the protected PDF and the password separately and logs both.
alternate_flows:
  - Cancel. The user cancels the statement with a reason; the cycle can be billed again.
rules:
  - [R1, "SOAs leave BIBS as password-protected PDF.", Fixed, "-"]
  - [R2, "Recipients (client, bank) are to be confirmed (CQ18).", Configurable, Statement recipient]
validations:
  - [No recipient, Enter at least one recipient, CLX_SOA_RECIPIENT]
  - [Statement cancelled, "Statement <SOA> is cancelled", CLX_SOA_CANCELLED]
fields_screen: Send Statement of Account
fields:
  - [Recipients, E-mail list, "Yes", Client contacts, Valid e-mail addresses]
notifications:
  - "The recipient receives the protected SOA and, separately, its password."
audit:
  - "Each e-mail is logged with recipient, time and attachment; cancellation with user and reason."
acceptance:
  - Sending an SOA sends two e-mails (document and password) and sets the statement to SENT.
  - A cancelled statement cannot be sent.
```

```fr
id: FR-CL-062
title: Keep billing as monitoring only
brd: [BRCLXN.060 (p.20)]
actor: System; Collection user / Team Lead
priority: Must have
fit: NEW
screens: Billing Statements; PR Worklist; Collection account
api: "-"
description: Billing statements, invoice dates and policy periods are visible to support aging, monitoring and escalation. Generating an SOA creates no receivable, no collection account and no commission receivable billing; the receivable remains the booked invoice in the ledger. Aging counts from the booking or inception date (CLX_AGING_BASIS).
preconditions:
  - "None."
main_flow:
  - A user generates an SOA (FR-CL-060).
  - BIBS stores the statement and its lines only.
  - The worklist and aging are unchanged.
rules:
  - [R1, "An SOA never creates a receivable or a CR billing.", Fixed, "-"]
  - [R2, "Aging basis BOOKING or INCEPTION; aging from the SOA due date is not built (CQ14).", Configurable, Parameter CLX_AGING_BASIS]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - Generating an SOA of 10,000.00 changes no ledger balance and adds no worklist account.
  - The SOA list shows invoice references, due dates and amounts per account.
```

> [!NOTE] Difference from the BRD
> BRCLXN.060 links aging to billing statements. BIBS ages accounts from the booking or inception date; the SOA due date as an aging basis waits for CQ14.

## Unapplied payments

Cashiering owns the unapplied payment, its tabs, its disposition workflow OPS_DISPOSITION and the money effects (BRD-2). Collections adds the collector side: the list as of today, the collector disposition and the request to Cashiering. Collections reads the unapplied items from Cashiering and never copies them. API paths of this section are under `/api/v1/collections/unapplied`.

```fr
id: FR-CL-070
title: View the list of unapplied payments as of today
brd: [BRCLXN.034 (p.30 / 48), BRCLXN.036 (p.31 / 49)]
actor: Collection Handler; Unapplied Payment Handler; Marketing AO; Cashiering (read)
priority: High
fit: NEW
screens: Unapplied Payments; Unapplied Payment (Payment & Account)
api: "GET .../unapplied; GET .../unapplied/{ref}"
description:
  - The Unapplied Payments screen lists the payments Cashiering could not apply, with an open balance as of today, read live from Cashiering. Each row shows the payment date and age (today less the payment date), the payment file (upload batch reference), transaction no., amount and unapplied balance, payment type, payor, bank code, check no., payor reference, the matched client or invoice, the Cashiering tab ("processing stage") and the status of the Cashiering disposition or collector request.
  - For a matched invoice the row also shows the assured, PR balance, inception date, segment, sales unit, Unit Head, AO, insurer, invoice category (Regular or Direct Bill) and the collection handler, from the collection account or else from the invoice ledger. The invoice number is shown in its BIBS format.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user opens Unapplied Payments.
  - BIBS shows the tab Awaiting Disposition with the open payments.
  - The user opens a payment to see its payment and account details, dispositions, requests and history.
rules:
  - [R1, "Only payments with an unapplied balance above zero are listed.", Fixed, "-"]
  - [R2, "Cashiering tabs - Awaiting Disposition (UNAPPLIED), In Cashiering (MONITORING), For Approval, For Reversal, All Open.", Fixed, "-"]
validations: []
fields_screen: Unapplied Payments (columns)
fields:
  - [Payment Date / Age, Date / Number, "-", Cashiering, Age in days]
  - [Payment File / Transaction No., Text, "-", Cashiering upload, "-"]
  - [Paid / Unapplied Amount, Amount, "-", Cashiering, "-"]
  - [Payment Type / Payor / Bank / Check No. / Reference, Text, "-", Cashiering, "-"]
  - [Client / Invoice matched, Text, "-", Cashiering match, "-"]
  - [Assured / PR Balance / Inception / Segment / Unit Head / AO / Insurer / Invoice Category, Mixed, "-", Collection account or ledger, "-"]
  - [Cashiering Tab / Status, Text, "-", Cashiering, "-"]
  - [Collector Disposition, Text, "-", Latest collector disposition, "-"]
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - A payment received today and not matched appears on Awaiting Disposition with age 0.
  - A payment matched to an invoice shows the invoice's assured, AO and PR balance.
  - A payment fully applied by Cashiering is no longer listed.
```

> [!NOTE] Difference from the BRD
> BRCLXN.036 asks for the Unit Head, AO and bank officer as names, the booker name, "Business Origin", "Client Code Match" and "System Generated Remarks". BIBS shows the UH and AO user names and the Cashiering tab as the processing stage; the other fields wait for their definitions (CQ11, CQ22).

```fr
id: FR-CL-071
title: Filter unapplied payments by market segment and disposition status
brd: [BRCLXN.035 (p.30 / 48)]
actor: Collection user
priority: High
fit: NEW
screens: Unapplied Payments (filters)
api: "GET .../unapplied?q=&clientCode=&salesUnit=&tab=&paidFrom=&paidTo=&ageMin=&ageMax=&segment=&disposition="
description: The user filters the unapplied list by market segment, collector disposition (including "none yet"), Cashiering tab, client, sales unit, payment dates and age, and searches by reference, payor, transaction, check or invoice. Text, client, unit, tab, date and age filters run in Cashiering; segment and disposition run in Collections.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user sets the filters.
  - BIBS returns the matching payments.
rules:
  - [R1, "The segment and disposition filters are applied to at most 2,000 payments at a time; narrow the other filters when there are more.", Fixed, "-"]
validations: []
fields_screen: Unapplied Payments (filters)
fields:
  - [Search, Text, "No", "-", "Reference, payor, transaction, check or invoice"]
  - [Market Segment, List, "No", Market segments, "-"]
  - [Collector Disposition, List, "No", "LOV CLX_UPP_DISPOSITION, None yet", "-"]
  - [Paid From / To, Date, "No", "-", To >= From]
  - [Age Min / Max, Number, "No", "-", Min <= Max]
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - Filtering on segment RETAIL and disposition "None yet" lists only Retail payments without a collector disposition.
```

```fr
id: FR-CL-072
title: Maintain the unapplied payment disposition list
brd: [BRCLXN.037 (p.32 / 49), BRCLXN.038 (p.32 / 49), BRCLXN.039 (p.32 / 49)]
actor: Section Head; Application Support; Business Administrator
priority: High
fit: CONFIGURE
screens: Lists of Values (CLX_UPP_DISPOSITION); Collections Setup (Disposition Rules)
api: "/api/v1/lov (type CLX_UPP_DISPOSITION); GET/PUT .../setup/lov-attributes"
description: The collector dispositions of unapplied payments are the list of values CLX_UPP_DISPOSITION, stored in BIBS and maintained by authorised users (add, deactivate, effective dates, maker-checker). Each value has two rules - whether it requires an invoice number, and the Cashiering action it asks for (apply to invoice, refund, reclass, transfer, none). Deactivated values cannot be selected.
preconditions:
  - "The user has LOV_MANAGE (values) or CLX_SETUP (rules)."
main_flow:
  - The user adds a value and another user approves it.
  - The user sets its rules on Collections Setup.
  - The value is offered in the unapplied Record Disposition dialog.
rules:
  - [R1, "Values delivered, to be confirmed (CQ08) - For application to invoice (requires invoice; APPLY_TO_INVOICE); For refund (REFUND); For reclass (RECLASS); For transfer to other unit (TRANSFER); Coordinate further (NONE); Handling fee (auto) for Submitted Policies (BRD-12).", Configurable, LOV CLX_UPP_DISPOSITION]
  - [R2, "Only active values within their effective dates can be selected.", Fixed, "-"]
validations:
  - [Unknown value, "<code> is not a value of <type>", CLX_LOV_VALUE_UNKNOWN]
  - [Invalid rule value, "'<value>' is not a valid <attribute>", CLX_LOV_ATTRIBUTE_VALUE]
fields_screen: Collections Setup, Disposition Rules (unapplied)
fields:
  - [Disposition, List, "Yes", LOV CLX_UPP_DISPOSITION, Existing value]
  - [Requires Invoice, Check box, "Yes", "-", "-"]
  - [Cashiering Action, List, "Yes", "APPLY_TO_INVOICE, REFUND, RECLASS, TRANSFER, NONE", "-"]
notifications:
  - "Pending LOV changes appear in My Approvals."
audit:
  - "LOV changes are audited; rule changes are field change rows."
acceptance:
  - A deactivated unapplied disposition is not offered and is refused on save.
  - A new value with Cashiering action NONE records a disposition without a request.
```

```fr
id: FR-CL-073
title: Record a collector disposition on an unapplied payment
brd: [BRCLXN.031 (p.29 / 48), BRCLXN.033 (p.29 / 48)]
actor: Collection Handler; Unapplied Payment Handler; Marketing AO / Handler / TL
priority: High
fit: NEW
screens: Unapplied Payments (Record Disposition); Unapplied Payment (Collector Dispositions)
api: "POST .../unapplied/{ref}/dispositions; GET .../unapplied/disposition-rules"
description: The handler documents what should happen to an unapplied payment by recording a collector disposition with remarks. When the value has a Cashiering action, the disposition also sends a request to Cashiering (FR-CL-074). Dispositions are append-only and kept after the payment is applied or refunded.
preconditions:
  - "The user has CLX_UNAPPLIED_WORK."
  - "The payment has an unapplied balance."
main_flow:
  - The handler selects a payment and clicks **Record Disposition**.
  - The handler chooses the disposition, enters the invoice number when required, the amount (optional) and remarks.
  - BIBS records the disposition and, for a Cashiering action, sends the request.
rules:
  - [R1, "Only active CLX_UPP_DISPOSITION values.", Fixed, "-"]
  - [R2, "The amount is above zero and at most the unapplied balance; blank = the whole balance.", Fixed, "-"]
validations:
  - [Payment without balance, "<ref> has no unapplied balance left", CLX_UNAPPLIED_NO_BALANCE]
  - [Amount out of range, "The amount must be above zero and at most the balance <balance>", CLX_UNAPPLIED_AMOUNT]
  - [Invoice checks, See FR-CL-075, "CLX_INVOICE_REQUIRED, CLX_INVOICE_FORMAT, CLX_INVOICE_UNKNOWN"]
fields_screen: Record Disposition (unapplied)
fields:
  - [Disposition, List, "Yes", LOV CLX_UPP_DISPOSITION, Active value]
  - [Invoice No., Text, Conditional, Invoice ledger, "Required when the value requires an invoice (FR-CL-075)"]
  - [Amount, Amount, "No", "-", "> 0 and <= unapplied balance"]
  - [Remarks, Long text, "No", "-", "-"]
notifications:
  - "None for a disposition without Cashiering action."
audit:
  - "Each disposition is kept with user and time and written as a field change row."
acceptance:
  - A handler records "Coordinate further" on Juan Dela Cruz's payment; it shows on the Collector Dispositions tab and no request is sent.
  - A disposition on a payment already fully applied is refused.
```

```fr
id: FR-CL-074
title: Request Cashiering to apply, refund, reclass or transfer a payment
brd: [BRCLXN.030 (p.29 / 48), BRCLXN.032 (p.29 / 48)]
actor: Collection Handler; Unapplied Payment Handler; Cashier (accepts)
priority: High
fit: NEW
screens: Unapplied Payments (Request Application); Requests to Cashiering; Cashiering > Incoming Requests (Collector Requests)
api: "POST .../unapplied/{ref}/dispositions; GET .../unapplied/requests; POST .../unapplied/requests/{id}/refresh; POST /api/v1/cashiering/collector-requests/{id}/accept, /reject"
description:
  - A disposition whose value carries a Cashiering action sends a request to Cashiering with the key CLX-UPP-<disposition> and a snapshot of the payment fields. Cashiering refuses it at once when the payment is unknown, has no balance, the amount is above the balance or an application has no invoice; otherwise it queues it (CRQ-yyyy-n) on its Incoming Requests screen.
  - A cashier accepts the request - BIBS assigns a disposition of the Operations workflow OPS_DISPOSITION with the matching type (apply to other invoice, refund, reclass, transfer unit) and the fields the collector cannot give - or rejects it with a reason. The approvals of the disposition type still apply. The request moves to ACCEPTED, then APPLIED when the disposition is executed, or REJECTED.
  - The requester is notified of each decision; Requests to Cashiering lists every request with its status and Cashiering reference.
preconditions:
  - "The user has CLX_UNAPPLIED_WORK; the value has a Cashiering action."
main_flow:
  - The handler records "For application to invoice" with a valid invoice (FR-CL-075).
  - BIBS sends the request; its status is SENT.
  - A cashier accepts it on Incoming Requests and completes the disposition; its approver approves it.
  - Cashiering applies the payment; the request becomes APPLIED and the handler is notified.
alternate_flows:
  - Rejected. The cashier rejects the request with a reason; the handler sees REJECTED and the reason.
  - Hand-off. Without the Cashiering adapter the request is DEFERRED as a hand-off to the team CASH_DISPOSITION.
  - Check status. The handler selects open requests and clicks **Check Status** to ask Cashiering again.
rules:
  - [R1, "One request per disposition; the key makes it idempotent.", Fixed, "-"]
  - [R2, "Refunds follow the Cashiering refund path to Disbursement; a Payment Requests RRF is not raised (OQ15).", Fixed, "-"]
validations:
  - [Cashiering rejects, "Rejected by Cashiering - <reason>", "-"]
  - [Reject without reason (cashier), Enter the reason for rejecting, REJECT_REASON_REQUIRED]
  - [Request already decided (cashier), "<request> is already <status>", COLLECTOR_REQUEST_DECIDED]
  - [Disposition type does not match the action (cashier), "Disposition type <type> does not carry out a <action> request", COLLECTOR_REQUEST_TYPE_MISMATCH]
notifications:
  - The requester is notified when Cashiering accepts, rejects or executes the request.
audit:
  - "The request keeps its status history, Cashiering reference and decision note; Cashiering keeps its own disposition history."
acceptance:
  - A request to apply Grace Villanueva's payment to an open invoice is accepted and executed by the cashier; the request shows APPLIED and the invoice's outstanding falls by the amount.
  - A rejected request shows REJECTED with the cashier's reason.
  - A refund request stays SENT until a cashier acts on it.
```

```fr
id: FR-CL-075
title: Validate the invoice of "for application to invoice"
brd: [BRCLXN.047 (p.33 / 50), BRCLXN.048 (p.33 / 50)]
actor: System
priority: High
fit: NEW
screens: Record Disposition (unapplied)
api: POST .../unapplied/{ref}/dispositions
description: When the chosen disposition requires an invoice ("For application to invoice"), the invoice number is mandatory, must match the pattern CLX_INVOICE_NO_PATTERN and must exist in the invoice ledger of the company. The form checks the pattern before sending and the server checks all three; the disposition cannot be submitted otherwise.
preconditions:
  - "The disposition value has requires_invoice = true."
main_flow:
  - The handler enters the invoice number.
  - The form checks the pattern.
  - On save, BIBS checks presence, pattern and existence and records the disposition.
rules:
  - [R1, "Pattern default ^(I\\d{8}|BI-.+)$ - the EBIX format I and 8 digits, or the BIBS format BI-...; whether EBIX numbers are still accepted after go-live is open (CQ13).", Configurable, Parameter CLX_INVOICE_NO_PATTERN]
  - [R2, "The invoice must exist in the BIBS invoice ledger.", Fixed, "-"]
validations:
  - [Invoice blank, Enter the invoice number to apply the payment to, CLX_INVOICE_REQUIRED]
  - [Invoice not in the pattern, "Invoice number <no> does not have the expected format (<pattern>)", CLX_INVOICE_FORMAT]
  - [Invoice not in the ledger, "Invoice <no> is not in the invoice ledger", CLX_INVOICE_UNKNOWN]
notifications:
  - "None."
audit:
  - "Refusals are not stored; the accepted disposition is audited (FR-CL-073)."
acceptance:
  - A "For application to invoice" disposition without an invoice number is refused with CLX_INVOICE_REQUIRED.
  - The invoice number I1234567 (7 digits) is refused with CLX_INVOICE_FORMAT.
  - A well-formed invoice number that is not booked is refused with CLX_INVOICE_UNKNOWN.
```

> [!NOTE] Difference from the BRD
> BRCLXN.047 fixes the format "I" followed by 8 digits, the EBIX format. BIBS invoice numbers are BI-<branch>-<yyyy>-n, so the pattern is a parameter that accepts both, and the invoice must exist in BIBS.

```fr
id: FR-CL-076
title: Keep the history of unapplied payments and dispositions
brd: [BRCLXN.040 (p.32 / 49)]
actor: System; Collection user (read)
priority: High
fit: NEW
screens: Unapplied Payment (History, Collector Dispositions, Requests to Cashiering)
api: GET .../unapplied/{ref}/history
description: The history of an unapplied payment merges Cashiering's events (intake, collector requests and decisions, dispositions, applied, refunded, reclassified, transferred, released, withdrawn, reversed, closed) with the collector dispositions, in time order. It stays available after the payment is applied or refunded; refunds reported by Cashiering are also recorded on the collector side.
preconditions:
  - "The user has CLX_VIEW."
main_flow:
  - The user opens a payment, tab History.
  - BIBS shows the merged events with dates, users and notes.
rules:
  - [R1, "Collector dispositions and requests are never deleted.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - After a payment is applied through a collector request, its history shows the disposition, the request, the acceptance and the application.
```

```fr
id: FR-CL-077
title: Produce the daily "For Application To Invoice" file
brd: [BRCLXN.041 (p.32 / 49), BRCLXN.042 (p.32 / 49)]
actor: System; Section Head / user with CLX_EXPORT (manual run)
priority: High
fit: NEW
screens: Requests to Cashiering (File column); Report Centre (For Application To Invoice)
api: "Job CLX_APPLICATION_FILE; POST .../unapplied/application-file; report CLX-APPLICATION-TO-INVOICE"
description:
  - At 05:00 every day the job CLX_APPLICATION_FILE writes, per company, a pipe-delimited text file FOR_APPLICATION_TO_INVOICE_<yyyyMMdd>_<time>.txt of the application requests made up to the end of the previous day and not yet listed. The fields are payment date, payment file name, transaction no., paid amount, currency, payment type, payor, reference no., assured, invoice no., user ID of the disposition, unapplied reference and request key (p.60).
  - The file goes through the file-drop port into the folder FS04/CLX_APPLICATION_TO_INVOICE of the in-system repository; each request records the file name. The report For Application To Invoice lists the same requests for any period.
preconditions:
  - "Application requests exist."
main_flow:
  - At 05:00 the job selects the requests not yet listed.
  - BIBS writes the file and records it on each request.
  - Authorised users download the file or run the report.
alternate_flows:
  - Manual run. A user with CLX_SETUP or CLX_EXPORT writes the file on demand.
  - Failure. The alert CLX_FILE_NOT_PUBLISHED is raised.
rules:
  - [R1, "Schedule 05:00 Manila daily (before the BRD's 06:00).", Configurable, Job schedule clx-application-file-cron]
  - [R2, "A request is listed in one file only.", Fixed, "-"]
  - [R3, "Transport to the BDOI file server FS04 is parked; the file stays in the in-system repository (OQ17, CQ12).", Fixed, "-"]
validations: []
notifications:
  - "Alert CLX_FILE_NOT_PUBLISHED on failure."
audit:
  - "Each file is recorded with its name, time and the requests it lists."
acceptance:
  - A request made on Monday is in Tuesday's 05:00 file and not in Wednesday's.
  - The file has one pipe-delimited line per request with the user ID of the disposition.
```

> [!NOTE] Difference from the BRD
> In BIBS the application is done in-app by Cashiering (FR-CL-074), so the text file is kept for audit and for any legacy hand-off. Whether BDOI still needs it, and its layout, are open (CQ12).

## Scheduled files, reports and home

```fr
id: FR-CL-080
title: Produce the "DP PR for reversal" files
brd: [BRCLXN.024 (p.28 / 48), BRCLXN.025 (p.28 / 48)]
actor: System; Collection user, CRU (download)
priority: High
fit: "NEW (024), CHANGE (025)"
screens: Collections Files (Monthly); Report Centre (DP PR for Reversal)
api: "Job CLX_MONTHLY_FILES; report CLX-DP-FOR-REVERSAL; GET .../files"
description: The job CLX_MONTHLY_FILES runs every day at 05:00 and, on the first working day of the month (head office holiday calendar), generates the Excel file "DP PR for Reversal" of the accounts tagged with that disposition in the previous month. The file is available from 08:00 that day and users with CLX_EXPORT are notified. The accounts themselves reached the Commission DP list when they were tagged (FR-CL-032); the file is the monthly record.
preconditions:
  - "Accounts were tagged DP PR for reversal in the previous month."
main_flow:
  - On the first working day the job generates the file for the previous month.
  - BIBS archives it with "available from" 08:00 and notifies the CLX_EXPORT users.
  - Users download it from Collections Files.
alternate_flows:
  - Failure. The file is recorded as failed and CLX_FILE_NOT_PUBLISHED is raised; a CLX_SETUP user generates the files of a date again.
rules:
  - [R1, "Generated once per report, period and scope.", Fixed, "-"]
  - [R2, "Not downloadable before its availability time.", Fixed, "-"]
validations:
  - [Download before availability, "<file> is available from <time>", REPORT_FILE_NOT_AVAILABLE]
notifications:
  - CLX_FILE_READY to the CLX_EXPORT users.
audit:
  - "Each file is a GENERATE run of the report archive with its period, row count and availability."
acceptance:
  - On 1 October (a working day) the file of September's DP tags is available at 08:00.
  - When 1 October is a holiday, the file is generated on the next working day.
```

```fr
id: FR-CL-081
title: Produce the "PR 2307 for reversal" files
brd: [BRCLXN.026 (p.29 / 48), BRCLXN.027 (p.29 / 48)]
actor: System; Collection user, Cashiering (download)
priority: High
fit: "NEW (026), CHANGE (027)"
screens: Collections Files (Monthly); Report Centre (PR 2307 for Reversal)
api: "Job CLX_MONTHLY_FILES; report CLX-PR2307-FOR-REVERSAL"
description: On the first working day of the month the same job generates the Excel file "PR 2307 for Reversal" of the accounts tagged with that disposition in the previous month, available from 08:00. The columns are the p.59 fields BIBS holds, including the PR2307 amount and the difference between the premium balance and the PR2307.
preconditions:
  - "Accounts were tagged PR 2307 for reversal in the previous month."
main_flow:
  - As FR-CL-080, for the report CLX-PR2307-FOR-REVERSAL.
rules:
  - [R1, "As FR-CL-080.", Fixed, "-"]
validations:
  - [Download before availability, "<file> is available from <time>", REPORT_FILE_NOT_AVAILABLE]
notifications:
  - CLX_FILE_READY to the CLX_EXPORT users.
audit:
  - "As FR-CL-080."
acceptance:
  - The October file lists every account tagged PR 2307 for reversal in September with its PR2307 amount.
```

```fr
id: FR-CL-082
title: Produce the weekly reversal files per unit and branch
brd: [BRCLXN.028 (p.29 / 48), BRCLXN.029 (p.29 / 48)]
actor: System; Collection user (download)
priority: High
fit: "NEW (028), CHANGE (029)"
screens: Collections Files (Weekly)
api: "Job CLX_WEEKLY_FILES; reports CLX-DP-FOR-REVERSAL, CLX-PR2307-FOR-REVERSAL"
description: Every Friday at 22:30, after the end-of-day, the job CLX_WEEKLY_FILES generates the "DP PR for Reversal" and "PR 2307 for Reversal" Excel files of the Saturday-to-Friday week, one file per sales unit and invoicing branch that has tagged accounts. They are available from 08:00 on the following Monday.
preconditions:
  - "Accounts were tagged in the week."
main_flow:
  - On Friday the job generates the files per unit and branch.
  - BIBS archives them with "available from" Monday 08:00 and notifies the CLX_EXPORT users.
rules:
  - [R1, "Week = Saturday to Friday.", Fixed, "-"]
  - [R2, "Schedule Friday 22:30 Manila.", Configurable, Job schedule clx-weekly-files-cron]
  - [R3, "Which reports are weekly per unit per branch is partly answered - Report List #59 says daily or as needed with production-style columns (CQ09).", Fixed, "-"]
validations:
  - [Download before availability, "<file> is available from <time>", REPORT_FILE_NOT_AVAILABLE]
notifications:
  - CLX_FILE_READY to the CLX_EXPORT users.
audit:
  - "As FR-CL-080."
acceptance:
  - Accounts tagged between Saturday and Friday appear in that week's files, split by unit and branch, downloadable from Monday 08:00.
```

```fr
id: FR-CL-083
title: Produce the daily PR report
brd: [BRCLXN.045 (p.33 / 50)]
actor: System; Collection user (download)
priority: High
fit: CHANGE
screens: Collections Files (Daily); Report Centre (Outstanding PR List, Full Production Report)
api: "Job CLX_DAILY_FILES; reports CLX-OUTSTANDING-PR, CLX-FULL-PRODUCTION"
description: Every day at 22:30, after the end-of-day, the job CLX_DAILY_FILES generates the "Outstanding PR List" (open accounts with their outstanding premium) and the "Full Production Report" (accounts of the invoices booked from the start of the month, any status), available at once.
preconditions:
  - "None."
main_flow:
  - The job generates both files.
  - BIBS archives them and notifies the CLX_EXPORT users.
rules:
  - [R1, "Schedule 22:30 Manila daily.", Configurable, Job schedule clx-daily-files-cron]
  - [R2, "The p.60-61 fields BIBS holds are filled; legacy fields (cover number, QPS reference, EBIX invoice number) wait for CQ22.", Fixed, "-"]
validations: []
notifications:
  - CLX_FILE_READY to the CLX_EXPORT users.
audit:
  - "As FR-CL-080."
acceptance:
  - Each morning Collections Files shows the previous evening's Outstanding PR List and Full Production Report.
```

```fr
id: FR-CL-084
title: Export lists on request without slowing the system
brd: ["Stakeholder functions - export the disposition list and completed collections (p.43-46)", "Caveat of the Operations Head (p.93)"]
actor: Collection user with CLX_EXPORT
priority: High
fit: CHANGE
screens: PR Worklist (Export to Files); Collections Files (Exports); Report Centre (Collections reports)
api: "POST .../exports; GET .../files; Collections reports"
description: Users export the worklist with its filters as an Outstanding PR List file, and run and export the Collections reports (section 6.1). Exports of the worklist run in the background and appear under Collections Files, Exports; they are refused above CLX_EXPORT_MAX_ROWS. Downloading needs CLX_EXPORT.
preconditions:
  - "The user has CLX_EXPORT (download) and CLX_REPORT_VIEW (reports)."
main_flow:
  - The user filters the worklist and clicks **Export to Files**.
  - BIBS generates the file in the background and notifies the user.
  - The user downloads it from Collections Files.
rules:
  - [R1, "Export cap CLX_EXPORT_MAX_ROWS, default 50,000.", Configurable, Parameter CLX_EXPORT_MAX_ROWS]
  - [R2, "Exports are recorded in the report archive.", Fixed, "-"]
validations:
  - [Too many rows, "<rows> accounts exceed the export limit of <cap>; narrow the filters", CLX_EXPORT_TOO_LARGE]
notifications:
  - CLX_FILE_READY when the export is ready.
audit:
  - "Each export is an archived run with user, time, parameters and row count."
acceptance:
  - An export of 60,000 accounts is refused with CLX_EXPORT_TOO_LARGE; with a narrower filter it runs.
  - A user without CLX_EXPORT sees the files but cannot download them.
```

```fr
id: FR-CL-085
title: Show the Collections home
brd: ["Stakeholder functions - view your dashboard (p.43-46)", "Report Dashboard (p.61)"]
actor: Every Collections user
priority: High
fit: NEW
screens: Collections Home
api: GET .../home
description: Collections Home shows the user's work as tiles with counts and links - My Open Accounts, All Open Accounts, Unassigned Accounts, DP Returned by Insurer, Credit Balances, Files Ready This Week, and for unapplied-payment users Unapplied Awaiting Disposition and My Requests in Cashiering - and a chart of the open amounts per aging bracket, for all segments or one.
preconditions:
  - "The user has CLX_VIEW (or another Collections permission)."
main_flow:
  - The user opens Collections Home.
  - BIBS shows the tiles the user's permissions allow and the aging chart.
  - The user clicks a tile to open the filtered list.
rules:
  - [R1, "Dashboard content per role is to be confirmed (CQ21).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Read only."
acceptance:
  - A handler with 12 open accounts sees 12 on My Open Accounts; clicking it opens the worklist filtered to them.
  - A user without CLX_UNAPPLIED_WORK does not see the unapplied tiles.
```

> [!NOTE] Built content
> The design also proposed tiles for installments due, promises due, broken promises and escalations. These are reached from their own screens (Installments Due, Promises to Pay, Escalations); adding them to the home waits for CQ21.

## Commission receivable and incentives

BRCLXN.059 is signed. BRCLXN.061-064 are only in the unsigned draft of the workshop addendum (p.8-12) and their scope is confirmed through CQ01. The Commission Receivables Unit is the Operations Commission team, so these rows belong to the Commission module of BRD-2, not to Collections. FR-CL-091 to FR-CL-094 are specified from the design and are **not built**; they carry no error codes.

```fr
id: FR-CL-090
title: Bill commission receivable only after the premium is confirmed paid
brd: [BRCLXN.059 (p.19-20)]
actor: Commission Receivables Unit (reviewer)
priority: Must have
fit: CHANGE
screens: Commission > DP Accounts; DP Billings
api: "POST /api/v1/commission/dp/items/confirm; DP billing endpoints"
description:
  - Commission receivable is billed to the insurer on accounts the client paid directly to the insurer. An account enters Commission from the Collections DP list (FR-CL-032) or a DP list upload. The CRU reviewer confirms each account "fully paid to the insurer" (DP for billing) before it can be put on a billing; a billing is only made of confirmed accounts.
  - Tagging an account for reversal or generating a billing statement does not by itself create a CR billing (BRCLXN.060). Partially paid or adjusted accounts stay visible on DP Accounts with their tag until confirmed.
preconditions:
  - "The account is on the Commission DP list."
main_flow:
  - The reviewer opens DP Accounts and checks the insurer's payment evidence.
  - The reviewer confirms the account as fully paid to the insurer.
  - The CRU sorts the confirmed accounts by insurer into billings (CMRID.009).
alternate_flows:
  - Not confirmed. An account that is not confirmed cannot be billed.
rules:
  - [R1, "A CR billing contains only accounts confirmed fully paid to the insurer.", Fixed, "-"]
  - [R2, "An SOA or a disposition never creates a CR billing.", Fixed, "-"]
validations:
  - [Account not confirmed, "Account <invoice> is not confirmed for billing (<tag>)", DP_ITEM_STATE]
  - [Nothing confirmed, No account is confirmed for billing, DP_NOTHING_TO_BILL]
notifications:
  - "As in the BRD-2 Commission FRs."
audit:
  - "The confirmation is kept with the reviewer and time."
acceptance:
  - A DP account not yet confirmed cannot be added to a billing.
  - After confirmation, the account is billed to its insurer.
```

> [!NOTE] Difference from the BRD
> BRCLXN.059 asks for "Premium Receivable confirmation". BIBS enforces it through the CRU reviewer's confirmation on the DP account (CMRID.013); there is no separate automatic confirmation record from the ledger (OQ38).

```fr
id: FR-CL-091
title: Keep regular commission and incentives as separate receivables
brd: [BRCLXN.061 (p.8-9, draft)]
actor: System; Commission Receivables Unit
priority: Must have (draft)
fit: NEW (not built; CQ01)
screens: Commission > DP Accounts, Incentive Runs (to be extended)
api: to be assigned at build
description:
  - BIBS would manage regular commission and incentives as distinct receivable types, each with its own eligibility, billing and accounting. Regular commission is collectible only when the premium is fully paid or confirmed. Incentives are Other Income, generated only when eligibility is met, on an incentive receivable account apart from commission receivable. Negative endorsements recompute both.
  - Receivables would be traceable at policy, invoice, insurer and transaction level.
preconditions:
  - "BDOI confirms the draft row (CQ01) and the GL treatment (OQ07)."
main_flow:
  - A commission or incentive becomes due.
  - BIBS creates a receivable of the matching type with its own rule and account.
  - Billing and aging treat the two types separately.
rules:
  - [R1, "Incentives are Other Income, separate from commission (FRBS 3.1.2 of BRD-5).", Configurable, Accounting rules (Comptrollership)]
validations:
  - [Incentive billed before eligibility, The incentive is not eligible for billing, "-"]
notifications:
  - "To be defined with CQ01."
audit:
  - "Receivable type and source kept on every line."
acceptance:
  - A regular commission line and an incentive line of the same invoice appear as two receivables with different accounts.
```

```fr
id: FR-CL-092
title: Bill incentive campaigns automatically
brd: [BRCLXN.062 (p.9-10, draft)]
actor: User (Commission); System
priority: Must have (draft)
fit: NEW (not built; CQ01)
screens: Commission > Incentive Schemes (to be extended to campaigns)
api: to be assigned at build
description: Incentives would be configured as campaigns with criteria - product or policy type, payment timing (within X days from booking or inception), payment status and exclusions such as pending negative adjustments. A campaign run would identify the qualified invoices, exclude those with unresolved adjustments, cancellations or partial payments, and bill the insurer through a system-generated service invoice with the applicable withholding tax, keeping the campaign and criteria on each line. No manual tagging.
preconditions:
  - "BDOI confirms the draft row (CQ01) and its relation to the early-remittance incentive (CQ24)."
main_flow:
  - A user sets up a campaign with its criteria.
  - The campaign run selects the qualified invoices.
  - BIBS generates the service invoice per insurer.
rules:
  - [R1, "Criteria reference the incentive criteria of Product Maintenance (PMADD07-08).", Configurable, Incentive criteria]
validations:
  - [Invoice with a pending negative adjustment, The invoice has an unresolved adjustment and is excluded, "-"]
notifications:
  - "To be defined with CQ01."
audit:
  - "Each billed line keeps its campaign and criteria snapshot."
acceptance:
  - An invoice paid within the campaign window is billed; one with a pending negative adjustment is excluded.
```

```fr
id: FR-CL-093
title: Handle commission refunds from negative adjustments apart
brd: [BRCLXN.063 (p.10-11, draft)]
actor: System; Commission Receivables Unit
priority: Must have (draft)
fit: NEW (not built; CQ01)
screens: Commission (to be extended)
api: to be assigned at build
description: When a negative adjustment reduces the premium of an invoice whose commission is billed or collected, BIBS would recompute the commission on the final premium and create a commission refund linked to the original commission and the adjustment. Refunds would never be CR-collectible, would be excluded from the CR statement of account, and would be excluded from the CRU dashboards and aging.
preconditions:
  - "BDOI confirms the draft row (CQ01)."
main_flow:
  - Adjustment posts the commission delta (built in BRD-2).
  - Commission creates a refund line for a billed or collected commission.
rules:
  - [R1, "A refund is never a CR line.", Fixed, "-"]
validations: []
notifications:
  - "To be defined with CQ01."
audit:
  - "Each refund references the original commission line and the adjustment request."
acceptance:
  - A negative endorsement on a collected commission creates a refund, not a negative CR line.
```

```fr
id: FR-CL-094
title: Bill only the collectible commission on mixed payments
brd: [BRCLXN.064 (p.11-12, draft)]
actor: System; Commission Receivables Unit
priority: Must have (draft)
fit: NEW (not built; CQ01)
screens: Commission (to be extended)
api: to be assigned at build
description: When a client pays part of a premium to BDOI and part directly to the insurer, BIBS would compute the commission receivable only on the portion paid directly and confirmed, after PR confirmation. The statement of account, aging and escalation would include only confirmed collectible portions.
preconditions:
  - "BDOI confirms the draft row (CQ01) and how a partial direct payment is evidenced (CQ19)."
main_flow:
  - The partial direct payment is confirmed.
  - BIBS computes the collectible commission on the confirmed portion.
rules:
  - [R1, "Collectible CR = commission x confirmed direct portion / gross premium.", Configurable, To be confirmed (CQ19)]
validations: []
notifications:
  - "To be defined with CQ01."
audit:
  - "The confirmation and the portion are kept on the line."
acceptance:
  - For a premium of 100,000.00 with 40,000.00 paid directly and confirmed, the collectible commission is 40% of the commission.
```

# Workflow and status model

## Escalation workflow (CLX_ESCALATION)

Figure 2 shows the workflow CLX_ESCALATION. Solid arrows are the main path, dashed arrows are returns and further escalation, and dotted arrows are the automatic closure when the account is collected.

![Workflow CLX_ESCALATION: stages and actions (BRCLXN.049, 050)](figures/brd04_escalation_workflow.dot){width=12}

<!-- table: widths=3,4.6,2.2,6.8 caption="Stages, owners and SLA" size=8.5 -->
| Stage | Owner (permission) | SLA hours (default) | Business actions |
|---|---|---|---|
| RAISED | System (no owner) | - | Route to the TL, or to the unit / section head |
| WITH_TL | Team lead (CLX_ESCALATION_HANDLE) | 24 | Acknowledge, escalate further, return to handler |
| WITH_UH | Unit / section head (CLX_ESCALATION_HANDLE) | 24 | Acknowledge, return to handler |
| IN_ACTION | Receiving level (CLX_ESCALATION_HANDLE) | 72 | Resolve, escalate further |
| RETURNED | Handler (CLX_WORK) | 48 | Resubmit to the team lead |
| RESOLVED | - | terminal | - |

The rule's SLA hours replace the stage default for escalations raised by that rule.

<!-- table: widths=3,3.3,2.6,4.2,3.5 caption="Transitions of CLX_ESCALATION" size=8 -->
| From | Action | To | Permission | Reason list |
|---|---|---|---|---|
| RAISED | route | WITH_TL | CLX_ESCALATE (system for rules) | - |
| RAISED | route_to_head | WITH_UH | CLX_ESCALATE (system for rules) | - |
| WITH_TL, WITH_UH | acknowledge | IN_ACTION | CLX_ESCALATION_HANDLE | - |
| WITH_TL, IN_ACTION | escalate_further | WITH_UH | CLX_ESCALATION_HANDLE | CLX_ESCALATION_REASON |
| WITH_TL, WITH_UH | return_to_handler | RETURNED | CLX_ESCALATION_HANDLE | RETURN_REASON |
| IN_ACTION | resolve | RESOLVED | CLX_ESCALATION_HANDLE | resolution text |
| RETURNED | resubmit | WITH_TL | CLX_WORK or CLX_ESCALATE | - |
| any open stage | auto_close (system) | RESOLVED | - | - |

## Collection account statuses and hand-offs

Figure 3 shows how the refresh moves a collection account between statuses, and how a disposition with an Operations action becomes a hand-off.

![Collection account statuses and hand-offs to Operations](figures/brd04_item_states.dot){width=13}

<!-- table: widths=3,5.8,7.8 caption="Hand-off (outbox) statuses" status=Status size=8.5 -->
| Status | Meaning | Next |
|---|---|---|
| PENDING | Waiting for the consuming module | TAKEN when Cashiering or Commission takes it; CANCELLED when a later disposition supersedes it; CLX_OUTBOX_STALE after one day |
| TAKEN | Taken by the consuming module and logged in its flow-in run | - |
| CANCELLED | Withdrawn before it was taken | - |

## Promises, installments and billing statements

![Promise, installment and statement statuses (BRCLXN.053, 055, 058)](figures/brd04_plan_states.dot)

<!-- table: widths=3.2,4.2,9.2 caption="Statuses of plans, installments, promises and statements" size=8.5 -->
| Record | Statuses | Set by |
|---|---|---|
| Installment plan | ACTIVE, COMPLETED, CANCELLED | Creation; completion when every installment is paid; cancellation by a CLX_BILLING user |
| Installment | NOT_DUE, DUE, OVERDUE, PARTIAL, PAID | Allocation of the ledger payments (FR-CL-041) and the business date |
| Promise to pay | OPEN, KEPT, PARTIALLY_KEPT, BROKEN, CANCELLED | Recording; the nightly promise check; withdrawal or a newer promise |
| Billing statement | GENERATED, SENT, CANCELLED | Generation, e-mail, cancellation |

## Requests to Cashiering

![Collector request to Cashiering (BRCLXN.030-033)](figures/brd04_unapplied_request.dot){width=12}

<!-- table: widths=3,6.6,7 caption="Statuses of an application request" status=Status size=8.5 -->
| Status | Meaning | Set by |
|---|---|---|
| SENT | Queued for Cashiering (CRQ number) | The collector disposition |
| DEFERRED | Handed over to the team CASH_DISPOSITION (no Cashiering adapter) | The default adapter |
| ACCEPTED | A cashier accepted it and assigned an Operations disposition | Cashiering |
| APPLIED | The disposition was executed (applied, refunded, reclassified or transferred) | Cashiering |
| REJECTED | Refused at once by Cashiering's checks, rejected by a cashier, or the disposition was withdrawn | Cashiering |

# Reports and documents

## Reports

All Collections reports are in the report category Collections. Viewing needs CLX_REPORT_VIEW, exporting needs CLX_EXPORT (the audit log needs CLX_AUDIT_VIEW); they export to PDF, XLSX, ODS and CSV and are archived. The reports take the company and, where they cover a period, the dates; they filter by market segment, sales unit and invoicing branch. The layouts are those of p.59-61 with the fields BIBS holds; legacy-only fields wait for CQ22.

<!-- table: widths=4.6,3.6,5.6,2.8 caption="Collections reports" size=8.5 -->
| Code | Name | Content | BRD |
|---|---|---|---|
| CLX-OUTSTANDING-PR | Outstanding PR List | Open collection accounts with the outstanding premium | BRCLXN.001-012, 045 |
| CLX-FULL-PRODUCTION | Full Production Report | Accounts of the invoices booked in the period, any status | BRCLXN.045 |
| CLX-DP-FOR-REVERSAL | DP PR for Reversal | Accounts tagged "DP PR for reversal" in the period | BRCLXN.024-029 |
| CLX-PR2307-FOR-REVERSAL | PR 2307 for Reversal | Accounts tagged "PR 2307 for reversal" in the period | BRCLXN.026-029 |
| CLX-COMPLETED-COLLECTIONS | Completed Collections | Accounts completed (paid or below the threshold) in the period | BRCLXN.008, 022 |
| CLX-INVOICES-WITH-DISPOSITION | Invoices with Disposition | Open accounts with their current collector disposition | BRCLXN.016-021 |
| CLX-REASSIGNMENTS | Collection Reassignments | Assignments and reassignments in the period | BRCLXN.052 |
| CLX-ESCALATIONS | Escalated Accounts | Escalations raised in the period with target, reason and stage | BRCLXN.049, 050 |
| CLX-BROKEN-PROMISES | Broken Promises to Pay | Promises broken or partially kept, by promised date | BRCLXN.055 |
| CLX-INSTALLMENTS-DUE | Installments Due and Overdue | Unpaid installments of live plans falling due in the period | BRCLXN.053 |
| CLX-APPLICATION-TO-INVOICE | For Application To Invoice | Collector requests to apply unapplied payments, with the file that listed them | BRCLXN.041 |
| CLX-UNAPPLIED-DISPOSITIONS | Unapplied Payment Dispositions | Collector dispositions of unapplied payments with their Cashiering request | BRCLXN.031-040 |
| CLX-AUDIT-LOG | Collections Audit Log | Field changes with from / to values | BRCLXN.043, 044 |

### Account reports (CLX-OUTSTANDING-PR, CLX-FULL-PRODUCTION, CLX-COMPLETED-COLLECTIONS, CLX-INVOICES-WITH-DISPOSITION)

One row per invoice. The columns are shared by the four reports.

<!-- table: widths=4.6,2.6,9.4 caption="Columns of the account reports" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Client Code / Name of Assured | Text | Client of the invoice and the assured |
| Invoice No. / Policy No. / Account (ARN) | Text | Keys of the invoice |
| Booking Date / Inception / Expiry | Date | Invoice dates |
| Aging / Aging Bracket | Number / Text | Days from the aging basis; bracket of CLX_AGING_BRACKETS |
| Invoicing Branch / Insurer / Product Line | Text | Classification from the ledger |
| Currency | Text | Invoice currency |
| Total Premium / Outstanding Premium / PR2307 Amount | Amount | Gross premium, net outstanding, open PR2307 |
| Market Segment / Sales Unit / Unit Head / Account Officer / Collection Handler | Text | Organisation and people (user names) |
| Client Payment Status / Invoice Category | Text | Payment status of the ledger; Regular or Direct Bill |
| Disposition / Category (A/B/C) / Tagging Owner | Text | Current collector disposition and its attributes |
| Last Collection Effort Date / Code | Date / Text | Latest effort |
| Remarks / Status | Text | Account remarks and status |

### Reversal reports (CLX-DP-FOR-REVERSAL, CLX-PR2307-FOR-REVERSAL)

The account columns above, followed by:

<!-- table: widths=4.6,2.6,9.4 caption="Additional columns of the reversal reports" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Date Tagged / Tagged By / Tag Remarks | Date / Text | The disposition that tagged the account |
| Basic Commission / VAT on Commission | Amount | Commission figures of the invoice |
| WTAX Rate | Percent | Withholding tax rate on commission |
| Diff (Premium Balance less PR2307) | Amount | Premium balance net of the PR2307 (p.59) |

The p.59 fields that BIBS does not hold (booking category, corporate department, commission invoice no. / date / amount, CR team remarks, PR reversal date) are left out until CQ22 maps them.

### For Application To Invoice (CLX-APPLICATION-TO-INVOICE)

<!-- table: widths=4.6,2.6,9.4 caption="Columns of the application report and file" size=8.5 -->
| Column | Format | Content |
|---|---|---|
| Requested On | Date | Date of the collector request |
| Payment Date / Payment File Name / Transaction No. | Date / Text | Payment identification (p.60) |
| Paid Amount / Currency / Payment Type | Amount / Text | Payment figures |
| Payor / Reference No. / Name of Assured | Text | Payor and target account |
| Invoice No. | Text | Invoice to apply the payment to |
| User ID | Text | User of the disposition |
| Unapplied Reference / Status / Cashiering Reference / File | Text | Request tracking |

## Documents

<!-- table: widths=3.6,3.4,9.6 caption="Collections documents" size=8.5 -->
| Template / file | Output | Content |
|---|---|---|
| CLX_SOA | PDF, e-mailed password-protected | SOA number and date; client and account; billing cycle and due date; installments billed (current and arrears) with policy year, coverage period, amount, paid and balance; amount due; generated by; template version. Draft layout until CQ18 |
| FOR_APPLICATION_TO_INVOICE_<yyyyMMdd>_<time>.txt | Pipe-delimited text | One line per application request with the fields of the table above; folder FS04/CLX_APPLICATION_TO_INVOICE of the in-system repository |
| Collections Bulk Update | XLSX template | Columns of FR-CL-034 with descriptions and an example row |

# Interfaces and integration

Figure 6 shows the interfaces of Collections. Collections reads the Operations invoice ledger and talks to Cashiering and Commission only through the Operations ports, inside BIBS. It posts no accounting entry.

![Interfaces of Collections (dashed = parked)](figures/brd04_integration.dot){width=15}

<!-- table: widths=3.8,2.2,7,2.4,2.2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Invoice ledger (opsledger) | In | Invoices, PR by component, movements, flags, shares; nightly refresh and after each movement | BRCLXN.001-015, 046 | BUILT |
| Sales organisation (catalog) | In | Unit Head of each sales unit, department or region | BRCLXN.011 | BUILT |
| Account, issuance, client master | In | Policy, account and client data of the account page | BRCLXN.056 | BUILT |
| Cashiering: check pick-up | Out | COLLECTION_CHECK_PICKUP, pulled by the pick-up queue | p.40 | BUILT |
| Cashiering: BIR 2307 tags | Out | COLLECTION_CWT2307; Cashiering pull not yet built | BRCLXN.026 | PARTIAL |
| Commission: DP list | Out | COLLECTION_DP_LIST, pulled by the DP list intake | BRCLXN.024 | BUILT |
| Commission: DP returned | In | COLLECTION_DP_RETURNED reopens the account | CMRID.009 | BUILT |
| Cashiering: unapplied items | In | UnappliedDirectory (list, item, history) | BRCLXN.034-036, 040 | BUILT |
| Cashiering: collector requests | Out / In | UnappliedDispositionRequests; UnappliedDispositionChanged back | BRCLXN.030-033 | BUILT |
| Cashiering: refunds | In | COLLECTION_REFUND recorded on the collector side | BRCLXN.040 | BUILT |
| Messaging and alerts | Out | Notifications, SOA e-mails, alerts | BRCLXN.049-058 | BUILT |
| BDOI file server FS04 | Out | Daily application file; in-system repository until OQ17 | BRCLXN.041-042 | PARKED |
| ISYS Marketing Diary | In | Client payment instructions | p.40-42 | PARKED |
| Legacy EBIX / QPS open items | In | One-time migration of open PRs and dispositions | BRCLXN.013-015 | PARKED |

> [!PARKED] Parked seams
> The FS04 transport (OQ17, CQ12), the Marketing Diary (CQ20) and the legacy migration (CQ07) are seams without content. Each is a configuration of a new adapter or a one-time load, with no change to the Collections functions.

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.51-58)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Authentication | SSO with Windows credentials; masked password; friendly errors; all attempts logged; lock-out after 3 attempts (NFR 1-2) | Platform log-in of BRD-1; lock-out at 3 for all users (D5); SSO parked (Q42) | PARTIAL |
| Password rules | 8 / 12 characters, complexity, history 8, minimum age 1 day, change every 90 days (NFR 2) | Platform password policy of BRD-1 | FIT |
| Access control | RBAC, custom roles, user maintenance; no multiple roles; IDOR protection (NFR 3) | RBAC and user administration exist; several roles per user allowed (D5) | CHANGE |
| Audit logging | Log-in / log-out, admin and configuration changes, record access and updates; timestamp, user, source IP, resource; exportable (NFR 4) | Platform audit plus the Collections change log with source IP (FR-CL-003, 004) | FIT |
| UI / UX | BDOI UI; copy / paste; smart search; record lock "<Username> is editing" (NFR 9) | BDO UX guidelines; search on every list; edit lock (FR-CL-005) | FIT |
| Session | Idle timeout 15 minutes set by the administrator; one session per device (NFR 10) | Timeout parameter exists; single session per device open (UQ09 / XQ11) | PARTIAL |
| Batch security | Restricted execution; audit of origin, time and trigger (NFR 12) | ManagedJob run history with trigger and user; manual runs need CLX_SETUP | FIT |
| Operating hours and locations | 06:00-22:00 Monday to Friday and month-end weekends; head office and six provincial offices (NFR 17) | Web access from every BDOI location; jobs run after 22:00 | FIT |
| Users and volumes | 130 users, 56 concurrent; 1,200 dispositions, applications and handler updates a day (NFR 15.01-15.05) | Worklist on indexed accounts; server-side paging | FIT |
| Response time | Screens under 5 seconds; daily report under 3 minutes; weekly under 10; monthly under 15 (NFR 15) | Online p95 under 3 seconds; files generated off-peak in the background | FIT |
| Exports | Caveat p.93: exports may slow the system | Background exports, row cap, separate permission (FR-CL-084) | FIT |
| Availability and recovery | RTO 4 hours, RPO 4 hours; DR server (NFR 18) | Same deployment as BRD-1 and BRD-2; one BIBS-wide NFR set is being agreed (XQ08, CQ25) | OPEN |
| Retention | 5 years online, 15 years archive; backup every 4 hours, kept 5 years (NFR 16) | Retention framework of BRD-1; records never deleted in Collections | FIT |

# Configuration items owned by the business and the System Administrator

The items below are changed in BIBS without a release. Changes to parameters and lists are audited.

## Parameters

<!-- table: widths=6.2,3,7.4 caption="Collections parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| CLX_MIN_BALANCE_THRESHOLD | 10.00 | Total to collect above which an invoice is listed (to confirm, CQ03) |
| CLX_AGING_BASIS | BOOKING | Aging from BOOKING or INCEPTION |
| CLX_AGING_BRACKETS | 0-30, 31-45, 46-60, 61-90, 91-120, 121+ | Aging brackets in days |
| CLX_INVOICE_NO_PATTERN | EBIX I and 8 digits, or BIBS BI-... | Regular expression of the invoice number of "for application to invoice" (CQ13) |
| CLX_PROMISE_GRACE_DAYS | 0 | Days after the promised date before a promise is checked (0-30) |
| CLX_EXPORT_MAX_ROWS | 50000 | Row cap of a worklist export |
| CLX_EDIT_LOCK_MINUTES | 15 | Minutes after which the edit lock expires |
| LOGIN_MAX_FAILED_ATTEMPTS | 3 | Failed log-ins that lock a user (all users, D5) |

## Jobs

<!-- table: widths=5,4.2,7.4 caption="Collections jobs (Manila time)" size=8.5 -->
| Job | Schedule | Purpose |
|---|---|---|
| CLX_DAILY_REFRESH | Daily 22:15 | Refresh the worklist from the ledger; assignment; end of temporary assignments |
| CLX_DAILY_FILES | Daily 22:30 | Outstanding PR List and Full Production Report |
| CLX_PROMISE_CHECK | Daily 22:45 | Evaluate promises; allocate payments to installments |
| CLX_ESCALATION | Daily 23:00 | Apply the escalation rules; close collected escalations |
| CLX_WEEKLY_FILES | Friday 22:30 | Weekly reversal files per unit and branch |
| CLX_APPLICATION_FILE | Daily 05:00 | Daily "For Application To Invoice" text file |
| CLX_MONTHLY_FILES | Daily 05:00 (generates on the first working day) | Monthly reversal files of the previous month |

## Lists of values

<!-- table: widths=5,11.6 caption="Lists of values" size=8.5 -->
| List | Values delivered |
|---|---|
| CLX_PR_DISPOSITION | DP PR for reversal; PR 2307 for reversal; For check pick-up; Cancel account; Coordinate further; Request bank AO assistance; No / missing policy number; DP returned by insurer (all to confirm, CQ08) |
| CLX_UPP_DISPOSITION | For application to invoice; For refund; For reclass; For transfer to other unit; Coordinate further (to confirm, CQ08); Handling fee (BRD-12) |
| CLX_EFFORT_CODE | Phone call; E-mail; Client visit; Statement of account sent; Others (see remarks) (to confirm, CQ08) |
| CLX_BILLING_FREQUENCY | Annual; Semi-annual; Quarterly; Monthly |
| CLX_ESCALATION_REASON | No payment commitment; Broken promise to pay; Aging beyond the threshold; Installment overdue; Credit term extension refused by the insurer; Others (see remarks) |
| RETURN_REASON | Shared platform list (return of an escalation) |

## Alerts and notification events

<!-- table: widths=5,2.4,9.2 caption="Alerts and notifications" size=8.5 -->
| Code | Kind | When |
|---|---|---|
| CLX_REFRESH_FAILED | Alert (high) | The daily refresh failed |
| CLX_FILE_NOT_PUBLISHED | Alert (high) | A scheduled file was not generated |
| CLX_ESCALATION_OVERDUE | Alert (medium) | An escalation stayed in a stage beyond its SLA |
| CLX_OUTBOX_STALE | Alert (medium) | A hand-off is pending for more than one day |
| CLX_ESCALATED | Notification (in-app, e-mail) | An account was escalated to the user or the user's team |
| CLX_REASSIGNED | Notification (in-app) | Accounts were assigned to or taken from the user |
| CLX_PROMISE_BROKEN | Notification (in-app) | A promise of the user's account was broken |
| CLX_DP_RETURNED | Notification (in-app) | An insurer returned a DP account |
| CLX_FILE_READY | Notification (in-app) | A scheduled file or export is ready |

## Masters and rules maintained by the business

<!-- table: widths=5,5,6.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Disposition values (PR, unapplied), effort codes, frequencies, reasons | LOV_MANAGE users (LOV approver) | FR-CL-030, 033, 072 |
| Disposition rules (category, owner, action, allowed roles, requires invoice, Cashiering action) | CLX_SETUP users | FR-CL-030, 072 |
| Unit Head per sales unit | CLX_SETUP users | FR-CL-015 |
| Default assignment rules | CLX_ASSIGN users | FR-CL-020 |
| Escalation rules | CLX_SETUP users (MASTER_AUTHORIZE) | FR-CL-050 |
| SOA template CLX_SOA | System Administrator | FR-CL-060 |
| Roles and permissions | Business Administrator via access request (approver) | FR-CL-001 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-CL-01 | The renumbering addendum (p.23-34) is the governing wording of BRCLXN.001-048; the signed workshop addendum (p.13-22) governs BRCLXN.049-060 | R2, R3 |
| A-CL-02 | BRCLXN.061-064 are not approved; they are specified and not built until BDOI confirms them | CQ01 |
| A-CL-03 | EBIX and QPS are replaced by BIBS booking and the Operations ledger; the BRD's extraction is an in-app refresh | OQ01 |
| A-CL-04 | The list is kept at invoice level; account and client totals are for information | CQ02 |
| A-CL-05 | Every CLX_VIEW user sees every account and uses filters | CQ06 |
| A-CL-06 | "Transaction type C" (BRCLXN.010) means cancellation and return invoices | CQ04 |
| A-CL-07 | Collections posts no accounting entry; the money effects stay in Cashiering, Commission and Adjustment | R6 principle 2 |
| A-CL-08 | The Processing Unit's only Collections action is the disposition "No / missing policy number" | p.44 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-CL-01 | BDOI provides the disposition values, categories A / B / C, effort codes and the threshold value | FR-CL-011, 030, 033, 072 (CQ03, CQ08) |
| D-CL-02 | The Cashiering owner adds the pull of COLLECTION_CWT2307 | FR-CL-032 |
| D-CL-03 | The Collections owner implements the bulk upload port for dispositions, efforts and remarks (gap G1) | FR-CL-034 |
| D-CL-04 | BDOI provides the SOA layout, recipients and numbering | FR-CL-060, 061 (CQ18) |
| D-CL-05 | BDOI names the FS04 transport and confirms the application file layout | FR-CL-077 (OQ17, CQ12) |
| D-CL-06 | BDOI decides the EBIX migration scope and cut-off | FR-CL-017 (CQ07) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-4 (status from the cross-BRD decisions, R7)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| CQ01 | Are BRCLXN.061-064 (draft only) in scope, and who owns them? | FR-CL-091-094 | OPEN |
| CQ02 | Invoice level confirmed; do account or client totals decide the threshold? | FR-CL-010, 014 | OPEN |
| CQ03 | Value of the threshold; per currency; net of PR2307? | FR-CL-011 | OPEN |
| CQ04 | Treatment of negative balances that are not cancellations | FR-CL-013 | OPEN |
| CQ05 | Source of the Unit Head; bank officer and booker as names | FR-CL-015, 070 | OPEN |
| CQ06 | Data scope per user (own segment, unit, accounts) | FR-CL-015 | PARTIAL |
| CQ07 | Migration of EBIX open items and history at go-live | FR-CL-017 | OPEN |
| CQ08 | Values of the PR and unapplied dispositions, categories, effort codes | FR-CL-030, 033, 072 | OPEN |
| CQ09 | Which reports are weekly per unit per branch; what the "PR Report" is | FR-CL-082, 083 | PARTIAL |
| CQ10 | Who the Unapplied Payment Handler is | FR-CL-073, 074 | OPEN |
| CQ11 | Definitions of Processing Stage, Business Origin, Client Code Match, System Remarks | FR-CL-070 | OPEN |
| CQ12 | Is the daily application text file still needed, and its layout | FR-CL-077 | OPEN |
| CQ13 | Accept EBIX invoice numbers after go-live? | FR-CL-075 | OPEN |
| CQ14 | Default escalation rules, levels, targets and aging basis | FR-CL-050, 051, 062 | OPEN |
| CQ15 | Source of installment terms; booking per installment or in full | FR-CL-040 | OPEN |
| CQ16 | When a promise is kept; grace days; partial payment | FR-CL-042 | OPEN |
| CQ17 | Meaning of delivery date and receipt date | FR-CL-043 | OPEN |
| CQ18 | SOA layout, recipient, numbering, e-mail | FR-CL-060, 061 | OPEN |
| CQ19 | Evidence of a partial direct payment on a non-DP invoice | FR-CL-094 | OPEN |
| CQ20 | Marketing Diary in ISYS or in BIBS; CTE tracking | FR-CL-033 | OPEN |
| CQ21 | Dashboard content per role | FR-CL-085 | OPEN |
| CQ22 | Mapping of legacy report fields | Section 6.1 | OPEN |
| CQ23 | Single role, single session, lock-out at 3 | FR-CL-001 | ANSWERED |
| CQ24 | Relation of draft campaigns to the early-remittance incentive | FR-CL-092 | OPEN |
| CQ25 | NFR alignment (hours, RPO) across BRDs | Section 8 | OPEN |

CQ23 is answered by BRD-11 (decision D5), except the single session per device (UQ09).

# Traceability

Every BRD-4 requirement is met by at least one FR. The Build column gives the delivery state: **Built**; **Built, parked** (built with a parked seam or configuration waiting for BDOI); **Built, gap** (built with a recorded gap); **Draft, not built**.

<!-- table: widths=2.2,2.6,4.8,5,2 caption="BRD ID to FR, screen and build status" size=7.5 -->
| BRD ID | FR | Screen | API | Build |
|---|---|---|---|---|
| BRCLXN.001 | FR-CL-010 | PR Worklist | .../worklist | Built |
| BRCLXN.002 | FR-CL-010 | PR Worklist; Collection account | .../items/{no} | Built |
| BRCLXN.003 | FR-CL-014 | PR Worklist (By Account / Client); Client View | .../worklist/totals; .../clients/{code} | Built |
| BRCLXN.004 | FR-CL-010 | Collection account | .../items/{no} | Built |
| BRCLXN.005 | FR-CL-011 | PR Worklist | Job CLX_DAILY_REFRESH | Built, parked |
| BRCLXN.006 | FR-CL-012 | Collections Setup | .../setup/parameters/{key} | Built, parked |
| BRCLXN.007 | FR-CL-012 | Collections Setup | .../setup/parameters/{key} | Built |
| BRCLXN.008 | FR-CL-011, FR-CL-018 | PR Worklist (Completed) | Job CLX_DAILY_REFRESH | Built |
| BRCLXN.009 | FR-CL-011 | PR Worklist | Job CLX_DAILY_REFRESH | Built |
| BRCLXN.010 | FR-CL-013 | PR Worklist (Excluded, Credit Balances) | Job CLX_DAILY_REFRESH | Built |
| BRCLXN.011 | FR-CL-015 | PR Worklist; Collections Setup (Unit Heads) | .../worklist; .../setup/unit-heads | Built |
| BRCLXN.012 | FR-CL-015 | PR Worklist | .../worklist | Built, parked |
| BRCLXN.013 | FR-CL-017 | Job runs | Job CLX_DAILY_REFRESH | Built, parked |
| BRCLXN.014 | FR-CL-017 | Job runs | Job CLX_DAILY_REFRESH | Built |
| BRCLXN.015 | FR-CL-017 | Collection account (Refresh from Ledger) | .../items/{no}/refresh | Built |
| BRCLXN.016 | FR-CL-030, FR-CL-031 | Lists of Values; Record Disposition | /lov; .../dispositions | Built, parked |
| BRCLXN.017 | FR-CL-030 | Lists of Values; Collections Setup | /lov; .../setup/lov-attributes | Built |
| BRCLXN.018 | FR-CL-030, FR-CL-031 | Record Disposition | .../dispositions | Built |
| BRCLXN.019 | FR-CL-002 | Account tabs | .../items/{no}/dispositions | Built |
| BRCLXN.020 | FR-CL-002 | Account tabs | .../items/{no}/history | Built |
| BRCLXN.021 | FR-CL-018, FR-CL-031 | Dispositions & Efforts | .../items/{no}/dispositions | Built |
| BRCLXN.022 | FR-CL-018 | PR Worklist (Completed) | .../worklist?status=COMPLETED | Built |
| BRCLXN.023 | FR-CL-018 | Timeline | .../items/{no}/timeline | Built |
| BRCLXN.024 | FR-CL-080, FR-CL-032 | Collections Files; Commission DP Lists | Job CLX_MONTHLY_FILES; COLLECTION_DP_LIST | Built |
| BRCLXN.025 | FR-CL-080 | Collections Files | .../files | Built |
| BRCLXN.026 | FR-CL-081, FR-CL-032 | Collections Files | Job CLX_MONTHLY_FILES; COLLECTION_CWT2307 | Built, gap |
| BRCLXN.027 | FR-CL-081 | Collections Files | .../files | Built |
| BRCLXN.028 | FR-CL-082 | Collections Files (Weekly) | Job CLX_WEEKLY_FILES | Built, parked |
| BRCLXN.029 | FR-CL-082 | Collections Files (Weekly) | .../files | Built |
| BRCLXN.030 | FR-CL-074 | Unapplied Payments; Requests to Cashiering | .../unapplied/{ref}/dispositions | Built |
| BRCLXN.031 | FR-CL-073 | Unapplied Payment (Collector Dispositions) | .../unapplied/{ref}/dispositions | Built |
| BRCLXN.032 | FR-CL-074 | Unapplied Payments | .../unapplied/{ref}/dispositions | Built, parked |
| BRCLXN.033 | FR-CL-073 | Unapplied Payments | .../unapplied/{ref}/dispositions | Built, parked |
| BRCLXN.034 | FR-CL-070 | Unapplied Payments | .../unapplied | Built |
| BRCLXN.035 | FR-CL-071 | Unapplied Payments (filters) | .../unapplied?segment=&disposition= | Built |
| BRCLXN.036 | FR-CL-070 | Unapplied Payments | .../unapplied | Built, parked |
| BRCLXN.037 | FR-CL-072 | Lists of Values; Collections Setup | /lov; .../setup/lov-attributes | Built |
| BRCLXN.038 | FR-CL-072 | Record Disposition (unapplied) | .../unapplied/disposition-rules | Built |
| BRCLXN.039 | FR-CL-072 | Lists of Values | /lov | Built, parked |
| BRCLXN.040 | FR-CL-076 | Unapplied Payment (History) | .../unapplied/{ref}/history | Built |
| BRCLXN.041 | FR-CL-077 | Requests to Cashiering; Report Centre | Job CLX_APPLICATION_FILE | Built, parked |
| BRCLXN.042 | FR-CL-077 | - | FileDropPort (FS04) | Built, parked |
| BRCLXN.043 | FR-CL-003 | History tab | .../items/{no}/history | Built |
| BRCLXN.044 | FR-CL-004 | Report Centre | Report CLX-AUDIT-LOG | Built |
| BRCLXN.045 | FR-CL-083 | Collections Files (Daily) | Job CLX_DAILY_FILES | Built |
| BRCLXN.046 | FR-CL-016 | Collection account (Summary) | .../items/{no} | Built |
| BRCLXN.047 | FR-CL-075 | Record Disposition (unapplied) | .../unapplied/{ref}/dispositions | Built, parked |
| BRCLXN.048 | FR-CL-075 | Record Disposition (unapplied) | .../unapplied/{ref}/dispositions | Built |
| BRCLXN.049 | FR-CL-050, FR-CL-051, FR-CL-053 | Escalation Rules; Escalations | .../escalation-rules; Job CLX_ESCALATION | Built, parked |
| BRCLXN.050 | FR-CL-052, FR-CL-053 | Escalations (Escalate Accounts) | .../bulk/escalate | Built |
| BRCLXN.051 | FR-CL-034 | PR Worklist; Bulk Uploads | .../bulk/*; handler CLX_BULK_UPDATE | Built, gap |
| BRCLXN.052 | FR-CL-020, FR-CL-021 | Assignments | .../assignment-rules; .../reassignments | Built |
| BRCLXN.053 | FR-CL-040, FR-CL-042 | Installment Plans; Installments Due | .../plans | Built, parked |
| BRCLXN.054 | FR-CL-041 | Collection account (Payments); Installment Plan | .../items/{no}/payments | Built |
| BRCLXN.055 | FR-CL-042, FR-CL-051 | Promises to Pay | .../promises; Job CLX_PROMISE_CHECK | Built, parked |
| BRCLXN.056 | FR-CL-043 | Collection account (Policy & Co-insurance) | .../items/{no}/policy | Built, parked |
| BRCLXN.057 | FR-CL-044 | Collection account (Timeline) | .../items/{no}/timeline | Built |
| BRCLXN.058 | FR-CL-060, FR-CL-061 | Billing Statements | .../billing/statements | Built, parked |
| BRCLXN.059 | FR-CL-090 | Commission DP Accounts, DP Billings | /commission/dp/items/confirm | Built |
| BRCLXN.060 | FR-CL-062 | Billing Statements | - | Built, parked |
| BRCLXN.061 | FR-CL-091 | - | to be assigned at build | Draft, not built |
| BRCLXN.062 | FR-CL-092 | - | to be assigned at build | Draft, not built |
| BRCLXN.063 | FR-CL-093 | - | to be assigned at build | Draft, not built |
| BRCLXN.064 | FR-CL-094 | - | to be assigned at build | Draft, not built |

API paths start with `/api/v1`; "..." stands for `/api/v1/collections`.

<!-- table: widths=5,3,8.6 caption="Capabilities and NFRs without a BRCLXN ID" size=8 -->
| Source | FR / section | Note |
|---|---|---|
| Stakeholder functions (p.43-46), NFR 3 | FR-CL-001 | Role-based access |
| NFR 9.03 (record lock) | FR-CL-005 | Edit lock |
| Collection efforts, categories (p.41, 43-46, 59, 61) | FR-CL-033 | Efforts and categories |
| Exports and caveat (p.43-46, p.93) | FR-CL-084 | Background exports |
| Dashboard (p.43-46, p.61) | FR-CL-085 | Collections Home |
| Check pick-up (p.40) | FR-CL-032 | Hand-off to Cashiering |
| Marketing Diary, CTE (p.38-42) | FR-CL-033 note | Parked (CQ20) |
| NFR 1-18 (p.51-58) | Section 8 | NFR table |

<!-- table: widths=6,2.2,2.2,2.2,2.2,2.2 caption="Coverage summary" size=8.5 -->
| Group | BRD IDs | Covered | Built | Built with parked seam or gap | Draft, not built |
|---|---|---|---|---|---|
| A. Outstanding PR list (001-012, 046) | 13 | 13 | 10 | 3 | 0 |
| B. Daily refresh (013-015) | 3 | 3 | 2 | 1 | 0 |
| C. Dispositions, tracking, audit (016-023, 043-044) | 10 | 10 | 9 | 1 | 0 |
| D. Files and reports (024-029, 045) | 7 | 7 | 5 | 2 | 0 |
| E. Unapplied payments (030-042, 047-048) | 15 | 15 | 8 | 7 | 0 |
| F. Collection management (049-058, 060) | 11 | 11 | 4 | 7 | 0 |
| G. Commission receivable (059, 061-064) | 5 | 5 | 1 | 0 | 4 |
| **Total** | **64** | **64** | **39** | **21** | **4** |

# Sign-off

By signing, BDOI confirms that this FRS describes the Collections functions it expects in BIBS, accepts the recorded differences in section 1.6 and the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "VP and Head, BDOI Operations", organisation: BDOI}
  - {name: "", role: "Section Head, Corporate Marketing", organisation: BDOI}
  - {name: "", role: "Section Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Head, Commission Receivables Unit", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
