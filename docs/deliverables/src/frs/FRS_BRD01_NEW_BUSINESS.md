---
# Source of the Functional Requirements Specification for BRD-1 New Business.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD01_NEW_BUSINESS.md
title: New Business
subtitle: BRD-1 New Business (Fire, Motor, Other Lines and Non-Package) and Workshop Addendum
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-01
name: New Business
doc_id: BIBS-FRS-BRD-01
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-1 New Business
output: FRS/BIBS_FRS_BRD-01_New_Business_v1.0.docx
control:
  - version: "0.9"
    date: 18 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Solution Architect
    approver: ""
    change: Internal draft from the BRD-1 baseline, the traceability record and the broking design
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Product Owner (pending)
    change: First issue for BDOI review; aligned with the as-built screens, codes and the cross-BRD decisions
distribution:
  - {name: "Product Owner, BDOI", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: Review of all FRs}
  - {name: "Retail, Commercial, Corporate and Institutional Marketing", role: Business user, organisation: BDOI, purpose: "Review of client, quotation, PRF and account FRs"}
  - {name: Technical Support Unit (TSU), role: Business user, organisation: BDOI, purpose: Review of the non-package and TSU routing FRs}
  - {name: Processing, role: Business user, organisation: BDOI, purpose: "Review of payment, placement, issuance and booking FRs"}
  - {name: Comptrollership, role: Business user, organisation: BDOI, purpose: Review of the booking and service invoice FRs}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the New Business business requirements of BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

New Business is built. Every FR therefore describes the behaviour of the delivered system, with the error codes, messages, lists of values, parameters, permissions and screen names of the build. Where the built behaviour differs from the BRD, the FR says so in a note and section 10.4 lists every difference.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

## Scope

The scope is the New Business process of BDOI as a broker: from the quotation or proposal request to the booked account with its service invoice, for Fire, Motor, Other Lines and non-package risks, in individual and bulk mode.

<!-- table: widths=4,9,4 caption="Scope of this FRS" -->
| Area | In scope | Source |
|---|---|---|
| Product lines and rules | Other Lines on the Fire and Motor workflow, product-specific fields, validation by product, intake templates, TSU routing | BRNB.001-004, 093, 098 |
| Common controls | Stages and status history, queues and assignment, void, document protection, approvals before sending, notifications, audit, documents, bulk uploads | BRNB.013-016, 019, 022, 026, 039, 055, 056, 080, 092, 094, 096, 097, 115 |
| Client onboarding and KYC | Search, create and update clients, duplicates, prospect and confirmed client, KYC, tags and instructions, client 360, KYC review list | BRNB.029, 030, 032, 046-049, 065, 090, 091, 099, 101, 110 |
| Quotation (package) | Requests by e-mail or source system, individual and bulk quotations, versions, approval, sending, acceptance, one ARN | BRNB.004, 020, 021, 023, 024, 028, 041-045, 063, 102 |
| Non-package (PRF) | Proposal Request Form, TSU queue, Quotation Slip, insurer responses, comparative table, Proposal Slip | BRNB.005-010, 017 |
| Account | Account creation and update, individual and bulk, premium computation, duplicate fall-out, contacts, returns, FFY, direct payment | BRNB.025, 033, 050-054, 058, 064, 066, 109, 113, 114 |
| Placement and hold cover | Placement slip, sending, hold cover request and confirmation, insurer returns, cancel and reactivate placement | BRNB.034, 059, 062, 069, 071, 072, 103; BRD 2.1.16 |
| Payment confirmation | CLPC billing file, payment reports and matching, payment gate per segment | BRNB.067, 068 |
| Issuance and e-policy | E-policy receipt, extraction and policy number update, document triggers, Insurance Advice, e-policy dispatch and report | BRNB.035, 060, 070, 073, 074, 077, 078, 095, 104, 105 |
| Booking | Individual, batch, automatic and direct booking, multi-year, endorsements, cancellation, service invoices, incentive flag, cost center | BRNB.027, 036, 038, 061, 076, 081, 100, 100b, 107, 108, 111, 112 |
| Reports and dashboard | NB dashboard, operational reports, print and download, saved variants | BRNB.011, 012, 018, 031, 037, 057, 075 |
| Administration | Log-in and session, tabs, lists of values, approvals, profiles and functions, access requests, audit log, retention | BRNB.040, 079, 082-089, 106 |

**Out of scope for this phase:**

- Override requests (BRD 1.1.12, 1.2.9, 2.1.14, 2.2.1.9 and 3.4.2.2.13). The addendum (p.83) makes them Renewal-only (OOS-1). BRD-6 Renewal delivers them.
- QPS-specific references: the QPS reference number and the QPS placement report (OOS-2, p.83). The Account Reference Number (ARN) and the Placement Summary report replace them.
- Maintenance of package products. This is BRD-3 Product Maintenance; this FRS uses the product catalogue that BRD-3 maintains.
- Renewal of accounts (BRD-6), post-issuance adjustments beyond the endorsements of BRNB.061 / 081 (BRD-2 Operations) and collection of premium (BRD-4 Collections).
- Computation and payout of incentives. BRNB.107 asks for the eligibility indicator only (Q33).

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | New Business BRD pack: Workshop Addendum (pp.1-22, repeated pp.23-44), Addendum for Other Lines and Non-Package (pp.45-90), Fire and Motor BRD ID consolidation (pp.91-129), Fire and Motor BRD V06162025 (pp.130-218) | Addendum signed 9 to 15-Apr-2026; Other Lines 16-Dec-2025; consolidation 21-Dec-2025 | `docs/source-documents/New Business (NB) BRD.pdf` |
| R2 | BDOI New Business (BRD-1) requirements baseline and fit/gap, including the open questions Q01-Q44 | current | `docs/requirements/BDOI_NB_BRD_SPEC.md` |
| R3 | New Business requirements traceability (built status, module, screen, API, test per BRD ID) | current | `docs/requirements/BDOI_NB_TRACEABILITY.md` |
| R4 | Broking (BDOI New Business) architecture | current | `docs/architecture/BROKING_ARCHITECTURE.md` |
| R5 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R6 | FRS BRD-3 Product Maintenance (package products, versions, incentive criteria) | 1.0, 25-Sep-2026 | `docs/deliverables/out/Drop-0_Setup_and_Data_Migration/FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.pdf` |
| R7 | BDO UX guidelines (brand, screen patterns) | current | `docs/design/BDO_UX_GUIDELINES.md` |

Page references in this document ("p.49") are pages of the BRD PDF (R1). The Workshop Addendum is cited by its first copy (pp.1-22). A reference to a legacy BRD step ("BRD 2.3.1") is given where no BRNB ID exists.

## Definitions and acronyms

```glossary
AO: Account Officer (Marketing)
ARN: Account Reference Number, ARN-yyyy-nnnnnn; one number per account from quotation to invoice (BRNB.102)
BRD: Business Requirements Document
BRNB: Requirement ID prefix of the New Business BRD (BRNB.001-BRNB.115)
CBG: Consumer Banking Group, a BDOI market segment
CLPC: Consumer Loans Process Center of BDO Unibank; receives the CBG Fire billing file and returns the payment report
CTPL: Compulsory Third Party Liability (products CTP01, CTP02)
DST: Documentary stamp tax
E-policy: The policy document the insurer issues in PDF
FFY: Free First Year; an account whose first-year premium is not paid by the client (BRNB.113)
FFYMI: Free First Year Motor Insurance
FR: Functional requirement of this document (FR-NB-nnn)
FST: Fire service tax
GL: General ledger
HLS: Home Loan System of BDO Unibank
IA: Insurance Advice, issued for mortgaged accounts (BRNB.070)
IDF: Insurance Declaration Form
KYC: Know your customer
LGT: Local government tax; its rate depends on the insurer branch
LOV: List of values maintained by the Business Administrator
MBS: Marketing Business Services and System Support
PN: Promissory note number of a loan account
PRF: Proposal Request Form for a non-package risk (BRNB.005)
PS: Proposal Slip, the approved insurer terms released to Marketing (BRNB.017)
PTX: Premium tax
QPS: Quotation Processing System, the current BDOI system
QS: Quotation Slip sent to insurers (BRNB.008)
Qnn: Open question on BRD-1 raised by the project team (section 10.3)
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

- A header table with the **BRD trace** (requirement ID and page), the **actor**, the BRD **priority**, the **fit** class of the baseline (R2), and the **screens** and **API** that implement it.
- **Description**, **preconditions**, **main flow** and **alternate and exception flows**. A description paragraph that starts with **Built behaviour** records a difference from the BRD.
- **Business rules**. *Configurable* rules are maintained by the Business Administrator, the System Administrator or the business owner in BIBS (parameter, list of values or master record, section 9). *Fixed* rules are part of the system and change only through a change request.
- **Validations and messages**: the check, the message the user sees and its code. The code is the one BIBS returns for a business rule. A "-" marks a screen or platform check (for example a blank mandatory field); its message follows the same wording but has no business code. Text in angle brackets (`<ARN>`) is filled in by BIBS.
- **Screens and fields**: label, type, whether mandatory ("Cond." = mandatory when the condition in the Validation column applies), the source list and the validation.
- **Notifications**, **audit** and numbered **acceptance criteria**. The acceptance criteria are the basis of the test cases of the BRD-1 test plan.

> [!NOTE]
> Values marked "default" (SLA hours, days, thresholds, list entries) are the values delivered with the build. They are configuration, so a changed BDOI answer to an open question in section 10.3 does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R2)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Worked with the platform before BRD-1 |
| CONFIGURE | Needed set-up only (parameters, templates, rules) |
| CHANGE | Extended or re-purposed an existing capability |
| NEW | A capability built for BRD-1 |
| OUT | Out of scope according to the BRD |

# Business context and process overview

## Business context

BDOI is an insurance broker: insurers issue the policies. Today most New Business work is manual, bulk processing exists for CBG Home only, status monitoring and reconciliation are done by hand, and all communication runs by e-mail (p.133). The BRD asks for bulk and individual processing across all segments, one system workflow, automation of most processing tasks, and reports for monitoring and reconciliation (p.132, 134). The Other Lines addendum extends this to every product line and adds non-package placement through the TSU (p.47-48). The April 2026 workshop addendum adds onboarding, tags, lifecycle rules, one reference number, hold cover, extraction, retention and booking details (p.3-19).

<!-- table: widths=1,8,8 caption="Current and envisioned process (BRD p.133-134, p.47)" -->
| # | Current process (before) | Envisioned process in BIBS (after) |
|---|---|---|
| 1 | Quotation requests arrive by e-mail; HLS requests are keyed in | Requests are captured in a request inbox or loaded from a source-system file; each gets a number |
| 2 | Quotations and proposals are prepared in QPS or Excel | Quotations are rated in BIBS with the Appendix A formulas, versioned, approved and sent protected |
| 3 | Non-package risks go to TSU by e-mail; comparative tables are built in Excel | Marketing raises a PRF; TSU sends quotation slips, keys in insurer terms, and BIBS compiles the comparative table |
| 4 | Clients are created without a standard onboarding step | Every client starts as a prospect and is confirmed after KYC verification |
| 5 | Accounts are tracked in spreadsheets; duplicates are found late | Each account has one ARN, a workflow stage and duplicate checks at creation |
| 6 | CBG Fire billing and payment matching are manual | BIBS produces the CLPC billing file and matches payment reports |
| 7 | Placement files, hold covers and e-policies go by e-mail without a log | Slips, hold cover requests, e-policies and advices are generated, protected and logged |
| 8 | Booking, GL entries and invoices are separate steps | Booking posts the GL entry and issues the service invoice in one step |
| 9 | Status reports are compiled by hand | Dashboards and reports read the workflow history directly |

## Process overview

The table lists the steps of the New Business process and Figure 1 shows them by actor. Each step is a stage of one of the four BIBS workflows (section 5).

<!-- table: widths=0.8,4,3.4,6.8,2.6 caption="Process steps" -->
| # | Step | Owner | What happens in BIBS | BRD |
|---|---|---|---|---|
| 1 | Request intake | Marketing AO | Request captured from e-mail or loaded from a source system; bulk requests by upload | BRNB.041, 023, 028 |
| 2 | Client onboarding | Marketing AO, Marketing TL | Prospect created; KYC documents uploaded, verified by a second user; client confirmed | BRNB.090, 101, 030 |
| 3a | Package quotation | Marketing AO, approver | Quotation rated, approved, sent protected; client acceptance recorded | BRNB.043, 020, 021, 045 |
| 3b | Non-package PRF | Marketing AO, Marketing TL, TSU | PRF approved; QS to insurers; responses and comparative table; Proposal Slip released | BRNB.005-010, 017 |
| 4 | Account creation | Marketing AO | One account per accepted risk group, with the quotation's ARN; submitted to Processing | BRNB.051, 102 |
| 5 | Validation | Processing | Client confirmed, TSU clearance where required; returned to Marketing when incomplete | BRNB.033, 096 |
| 6 | Payment confirmation | Processing, system | CBG Fire and Motor must be paid (CLPC billing, payment reports); Other Lines need client confirmation | BRNB.067, 068; BRD 2.3.1 |
| 7 | Placement | Processing | Placement slip per insurer branch, sent by e-mail; hold cover request and confirmation; insurer returns | BRNB.069, 071, 072, 103, 034 |
| 8 | Issuance | Processing, E-policy Sender | E-policy received and matched; policy number confirmed; Insurance Advice for mortgaged accounts; e-policy sent protected | BRNB.073, 074, 070, 077 |
| 9 | Booking | Processing, Adjustment | Invoice, GL entry and service invoice in one transaction; batch, automatic or direct booking; endorsements and cancellation | BRNB.027, 036, 076, 111, 081 |
| 10 | Monitoring | All | Dashboard, status and fall-out reports, production against target | BRNB.012, 075, 115 |

![New Business process by actor (BRNB.001, p.49; BRD p.134)](figures/brd01_process_flow.dot)

## Processing modes and product lines

Every step works in two modes (p.132): **individual** (one record on screen) and **bulk** (an Excel, ODS or CSV upload with a downloadable template, or a source-system file). The bulk uploads are listed in FR-NB-019.

Other Lines follow the Fire and Motor workflow with product-specific set-up (p.47). The product lines of the Annex (p.84-90) are held in the catalogue with their cover types: Property, Motor, Engineering, Liability, Crime, Property / Equipment Floater, Marine, Marine Hull, Aviation, Personal Accident, Electronic Equipment and Others. Package products (for example MTR and PAR codes) are maintained by BRD-3 Product Maintenance (R6).

# Personas and roles

## Personas

<!-- table: widths=3.2,3.3,8.1,3 caption="Personas and BIBS roles" -->
| Persona | BIBS role | Responsibilities in New Business | BRD |
|---|---|---|---|
| Marketing AO | MKT_AO | Captures requests; creates clients, quotations, PRFs and accounts; uploads bulk files; handles returned accounts | BRD 1.1, 2.1 (p.93-107) |
| Marketing TL / TH / UH | MKT_TL | As the AO, and approves quotations and PRFs, verifies KYC, assigns work | BRNB.005, 021, 090 |
| TSU | TSU | Receives PRFs; prepares and approves quotation and proposal slips (never both on the same slip); keys in insurer terms; clears accounts | BRNB.007-010, 017, 098 |
| Processing | PROCESSOR | Validates accounts; billing and payment matching; placement, hold cover, returns; e-policy receipt, Insurance Advice; booking | BRD 1.2, 1.3, 2.2 (p.101-120) |
| Processing TL | PROCESSING_TL | As Processing, and assigns work | BRNB.080 |
| Approver | NB_APPROVER | Approves quotations, access requests and master records; assigns work | BRNB.079, 080 (p.119-120) |
| E-policy Sender | EPOLICY_SENDER | Sends e-policies and Insurance Advices to clients | BRNB.077, 078 (p.118-119) |
| Adjustment | ADJUSTMENT | Cancels bookings; posts negative and non-financial endorsements | BRNB.081 (p.120) |
| Business Administrator | BUSINESS_ADMIN | Maintains lists of values and masters; submits access requests; views audit logs and messages | BRNB.082-086 (p.121-122) |
| System Administrator | SYSADMIN | Users, roles, parameters, jobs; submits access requests; maintains lists of values | BRNB.087-089 (p.122-127) |
| Auditor | AUDITOR | Read access to clients, quotations, accounts, work queues and messages | BRNB.016 |

The BRD persona footnote says that user roles "will be further defined in succeeding documentations" (p.49, footnote 2; p.4). BRD-11 User Access Maintenance governs the final matrix; the roles above are the build's matrix until BDOI confirms it.

## Permissions

<!-- table: widths=4.6,2.6,9.6 caption="New Business permissions and action classes" -->
| Permission | Action class | Allows |
|---|---|---|
| CLIENT_VIEW | VIEW | Clients, client 360, KYC Reviews Due |
| CLIENT_MAINTAIN | CREATE / AMEND | Create and update clients, KYC documents, tags and instructions, submit KYC, confirm |
| CLIENT_APPROVE | APPROVE | Verify KYC, periodic KYC review, return a KYC |
| QUOTE_VIEW | VIEW | Quotations and quotation requests |
| QUOTE_MAINTAIN | CREATE / AMEND | Capture requests; create, revise, send quotations; record acceptance; create accounts |
| QUOTE_APPROVE | APPROVE | Approve or return quotations |
| PROPOSAL_REQUEST | CREATE / AMEND | Create and submit PRFs; send the Proposal Slip to the client; acceptance |
| PROPOSAL_APPROVE | APPROVE | Marketing approval of a PRF |
| TSU_PROCESS | CREATE / AMEND | TSU queue, quotation slip, insurer responses, proposal slip, TSU clearance |
| TSU_APPROVE | APPROVE | Approval of quotation and proposal slips (not by the preparer) |
| ACCOUNT_VIEW | VIEW | Accounts, placement and issuance workbenches, NB reports |
| ACCOUNT_MAINTAIN | CREATE / AMEND | Create, update, submit and resubmit accounts; FFY and payment arrangement |
| ACCOUNT_PROCESS | AMEND | Validate, return and direct booking of accounts |
| PLACEMENT_MANAGE | AMEND | Payment gate actions, slips, hold cover, insurer returns, cancel and reactivate placement |
| BILLING_MANAGE | AMEND | CLPC billing batches and payment reports |
| EPOLICY_MANAGE | AMEND | E-policy receipt, extraction review, Insurance Advice generation |
| EPOLICY_SEND | CREATE | Send e-policies and Insurance Advices |
| BOOKING_PROCESS | CREATE | Booking, batch runs, service invoices |
| BOOKING_ADJUST | APPROVE | Endorsements, cancellations and service invoice credits |
| BULK_PROCESS | CREATE | Bulk upload centre (each upload also checks its own permission) |
| WORK_VIEW, WORK_ASSIGN | VIEW / AMEND | My Work queues and NB Dashboard; assign and re-assign work |
| LOV_MANAGE | CREATE / AMEND | Lists of values |
| MASTER_VIEW, MASTER_MAINTAIN, MASTER_AUTHORIZE | VIEW / AMEND / APPROVE | Catalogue masters, rates, templates, booking set-up; authorisation of masters and LOV values |
| ACCESS_REQUEST, ACCESS_APPROVE | CREATE / APPROVE | Access requests and their decision |
| AUDIT_VIEW, MESSAGE_VIEW, REPORT_VIEW | VIEW | Audit trail, outbound messages, report centre |
| ATTACHMENT_VIEW, ATTACHMENT_MANAGE | VIEW / AMEND | Documents tabs |

<!-- pagebreak -->

## Permissions matrix

The table below is the role-to-permission matrix delivered with the build ("Y" = granted, migration V750). Changes go through access requests of type "Modify role permissions" (FR-NB-135). The User Access Matrix screen shows it by permission and by action class and exports it to Excel.

<!-- table: widths=4.3,1.12,1.12,1.12,1.12,1.12,1.12,1.12,1.12,1.12,1.12,1.12 caption="Role-to-permission matrix for New Business (build V750)" size=7.5 -->
| Permission | MKT AO | MKT TL | TSU | Proc. | Proc. TL | NB Appr. | E-pol. Send. | Adjust. | Bus. Admin | Sys. Admin | Audit. |
|---|---|---|---|---|---|---|---|---|---|---|---|
| CLIENT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | | | Y |
| CLIENT_MAINTAIN | Y | Y | | | | | | | | | |
| CLIENT_APPROVE | | Y | | | | | | | | | |
| QUOTE_VIEW | Y | Y | Y | Y | Y | Y | | | | | Y |
| QUOTE_MAINTAIN | Y | Y | | | | | | | | | |
| QUOTE_APPROVE | | Y | | | | Y | | | | | |
| PROPOSAL_REQUEST | Y | Y | | | | | | | | | |
| PROPOSAL_APPROVE | | Y | | | | | | | | | |
| TSU_PROCESS | | | Y | | | | | | | | |
| TSU_APPROVE | | | Y | | | | | | | | |
| ACCOUNT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | | | Y |
| ACCOUNT_MAINTAIN | Y | Y | | Y | Y | | | | | | |
| ACCOUNT_PROCESS | | | | Y | Y | | | | | | |
| PLACEMENT_MANAGE | | | | Y | Y | | | | | | |
| BILLING_MANAGE | | | | Y | Y | | | | | | |
| EPOLICY_MANAGE | | | | Y | Y | | | | | | |
| EPOLICY_SEND | | | | | | | Y | | | | |
| BOOKING_PROCESS | | | | Y | Y | | | | | | |
| BOOKING_ADJUST | | | | | | | | Y | | | |
| BULK_PROCESS | Y | Y | | Y | Y | | | | | | |
| WORK_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y |
| WORK_ASSIGN | | Y | | | Y | Y | | | | | |
| LOV_MANAGE | | | | | | | | | Y | Y | |
| MASTER_VIEW | | | | | | Y | | | Y | | |
| MASTER_MAINTAIN | | | | | | | | | Y | | |
| MASTER_AUTHORIZE | | | | | | Y | | | | | |
| ACCESS_REQUEST | | | | | | | | | Y | Y | |
| ACCESS_APPROVE | | | | | | Y | | | | | |
| AUDIT_VIEW | | | | | | | | | Y | | |
| MESSAGE_VIEW | | | | | | | | | Y | Y | Y |
| REPORT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | Y | | |
| ATTACHMENT_VIEW | Y | Y | Y | Y | Y | Y | Y | Y | | | |
| ATTACHMENT_MANAGE | Y | Y | Y | Y | Y | | | Y | | | |

The System Administrator also holds the platform permissions for users, roles, parameters and jobs, and the Auditor the platform audit permissions; they are not listed here.

Segregation of duties is enforced by the system, whatever the role grants: a KYC is never verified by the client's creator or the user who submitted it; a quotation or PRF is never approved by its maker or submitter; a quotation slip or proposal slip is never approved by its preparer; an access request is never decided by its requester; a list value or master record is never authorised by its maker.

## BRD functions and BIBS permissions

BRNB.088 (p.122-127) lists the functions that the System Administrator assigns to profiles. Each function is a permission (or a pair) in BIBS, so assigning a function is granting its permission to a role.

<!-- table: widths=1.2,8.2,7.2 caption="BRD functions (BRD 3.4.2.2) and their permissions" size=8 -->
| # | BRD function | BIBS permission(s) |
|---|---|---|
| 1-2 | Receive quotation / proposal request; create quotation / proposal | QUOTE_MAINTAIN (package); PROPOSAL_REQUEST (non-package) |
| 3-4 | Search client; update client details | CLIENT_VIEW; CLIENT_MAINTAIN |
| 5-6 | Search accounts; update accounts | ACCOUNT_VIEW; ACCOUNT_MAINTAIN |
| 7-8 | Upload files / documents; view uploaded documents | ATTACHMENT_MANAGE; ATTACHMENT_VIEW |
| 9 | Access reports | REPORT_VIEW (each report also checks its own permission) |
| 10, 20 | Request / approve override | Not built: Renewal only (OOS-1) |
| 11 | Handle returned placement request | ACCOUNT_MAINTAIN (Marketing); PLACEMENT_MANAGE (Processing) |
| 12, 34 | Access generated Insurance Advice | ACCOUNT_VIEW (register); EPOLICY_SEND (send) |
| 13 | Approve requests | ACCESS_APPROVE, MASTER_AUTHORIZE, QUOTE_APPROVE |
| 14 | Assign workload to specific users | WORK_ASSIGN |
| 15-18 | Create bulk quotation; generate accounts for bulk; create clients; create accounts | BULK_PROCESS with QUOTE_MAINTAIN, ACCOUNT_MAINTAIN, CLIENT_MAINTAIN |
| 19-20 | Generate billing report (CBG Fire); validate payment (other segments) | BILLING_MANAGE; PLACEMENT_MANAGE (client confirmation) |
| 21, 23, 24 | Generate and send placement file / slip; request hold cover | PLACEMENT_MANAGE |
| 22, 25, 26 | Generate Insurance Advice; receive e-policy; update e-policy number | EPOLICY_MANAGE |
| 27 | Generate reports | REPORT_VIEW |
| 28 | Send e-policy to client | EPOLICY_SEND |
| 29-30 | Generate list of accounts (individual); confirm issuance or payment | QUOTE_MAINTAIN (create accounts); BILLING_MANAGE |
| 31-32 | Book accounts; cancel placement | BOOKING_PROCESS; PLACEMENT_MANAGE |
| 33 | Manually build accounts for bulk processing | BULK_PROCESS, QUOTE_MAINTAIN |
| 35-36 | Reactivate cancelled placed accounts; cancel booking | PLACEMENT_MANAGE or ACCOUNT_MAINTAIN; BOOKING_ADJUST |
| 37-39 | Maintain LOVs; maintain users; submit profile requests | LOV_MANAGE; ACCESS_REQUEST (user changes go through requests); ACCESS_REQUEST |

# Functional requirements

## Product lines and product rules

```fr
id: FR-NB-001
title: "Run every product line on one New Business workflow"
brd: [BRNB.001 (p.49), Annex (p.84-90)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "Products; Product page"
api: "GET /api/v1/catalog/lines; GET /api/v1/catalog/products"
description:
  - "BIBS holds every BDOI product in one catalogue keyed by the BDOI risk code (for example CAR00, CGL01, MTR08, PAR01) with its product line and cover type. Fire, Motor and Other Lines products all use the same quotation, account, placement, issuance and booking workflows; a product differs only by its set-up (risk item kind, rating method, field rules, required documents, payment gate, TSU routing)."
  - "The product line decides the risk item kind of an account (vehicle, property location, person or generic) and the rating method (property, motor or generic)."
preconditions:
  - "The product is authorised and active in the catalogue (BRD-3 FRS, R6)."
main_flow:
  - "The user selects a product on a quotation, PRF or account."
  - "BIBS checks that the product is usable and offered to the market segment."
  - "BIBS applies the product's line settings (risk item kind, rating, field rules, documents) and starts the standard workflow."
alternate_flows:
  - "Product not usable. BIBS refuses the selection."
rules:
  - [R1, "Product lines: Property, Motor, Engineering, Liability, Crime, Property / Equipment Floater, Marine, Marine Hull, Aviation, Personal Accident, Electronic Equipment, Others (Annex II).", Configurable, Product lines (catalogue)]
  - [R2, "Cover types per line as in Annex II (for example Motor: TPL only, Comprehensive).", Configurable, Cover types (catalogue)]
  - [R3, "Only ACTIVE (authorised) products, lines and cover types can be used.", Fixed, "-"]
  - [R4, "A product states the market segments it is offered to.", Configurable, Product master]
validations:
  - [Product not authorised or inactive, "Product <code> is not authorized or is inactive", PRODUCT_NOT_ACTIVE]
  - [Product not offered to the segment, "Product <code> is not offered to segment <segment>", SEGMENT_NOT_ALLOWED]
  - [Cover type of another line, "Cover type <code> does not belong to <line>", COVER_TYPE_INVALID]
fields_screen: "Product (catalogue, main attributes used by New Business)"
fields:
  - [Risk code, Text, "Yes", "-", Unique; follows the line's naming pattern]
  - [Product line / Cover type, List, "Yes", Product lines; cover types of the line, Active]
  - [Market segments, Multi-select, "Yes", LOV MARKET_SEGMENT, "-"]
  - [Package, Check box, "No", "-", Package products are maintained under BRD-3]
  - [Payment gate, List, "Yes", PAID / CLIENT_CONFIRMATION, See FR-NB-092]
  - [Direct payment / FFY / Multi-year (max term) / Mortgage, Check boxes, "No", "-", Maximum term 1-10 years]
  - [TSU involvement, List, "No", ALWAYS / NEVER / by rule, See FR-NB-005]
notifications:
  - "None."
audit:
  - "Product records are maker-checker masters; every change is audited (BRD-3)."
acceptance:
  - "A Liability product (for example CGL01) goes through quotation, account, placement, issuance and booking with the same stages as a Motor product."
  - "A product that is not active cannot be selected on a quotation or account."
  - "A product offered only to CBG cannot be used on a CORBANK account."
```

```fr
id: FR-NB-002
title: "Capture the product-specific fields of each line"
brd: [BRNB.002 (p.49), BRNB.093 (p.5)]
actor: "System; MBS / Business Administrator (field matrix)"
priority: "Must have"
fit: "CHANGE"
screens: "New Account (Risk Items step); New Quotation; Products (Field Rules tab)"
api: "GET /api/v1/catalog/field-rules; GET /api/v1/accounts/{id}/check"
description:
  - "An account holds one or more risk items. The item fields follow the line's risk item kind - vehicle (plate, conduction sticker, engine, chassis, make, model, year model, body type, colour, seats, sum insured), property location (address, city, province, occupancy, construction class, insured items with sums insured), person (name, birth date, relationship) or generic (description, sum insured)."
  - "A minimum-field matrix (field rules by scope ALL, LINE or PRODUCT and target ACCOUNT or ITEM) states which fields must be present before submission. It is held in the catalogue and maintained without IT; the delivered rows are defaults until BDOI provides the product matrix (Q01, Q02)."
preconditions:
  - "The product of the record is selected."
main_flow:
  - "The user enters the account and risk item details."
  - "On save, BIBS stores every field entered, even when mandatory fields are still blank (draft)."
  - "On submission, BIBS evaluates the field rules in scope and lists each missing field against its field."
rules:
  - [R1, "Rules apply from the widest scope to the narrowest: ALL (*), then the line, then the product.", Fixed, "-"]
  - [R2, "Default account rules: client, market segment, period from and to, currency, at least one risk item; every item needs a sum insured.", Configurable, Field rules]
  - [R3, "Default Motor item rules: plate number or conduction sticker, engine number, chassis number, make, model, year model.", Configurable, Field rules]
  - [R4, "Default Property item rules: full address, city, occupancy, construction class, insured items with sums insured.", Configurable, Field rules]
  - [R5, "Default Personal Accident item rules: insured person and birth date; other lines: a description of the risk.", Configurable, Field rules]
  - [R6, "A rule on 'a|b' is met when one of the fields is present.", Fixed, "-"]
validations:
  - [Mandatory field blank on submission, "Complete the mandatory fields of <product>: <field list>", ACCOUNT_INCOMPLETE]
  - [Rule for every product with a scope code other than '*', "Rules for every product use the scope code '*'", RULE_SCOPE_INVALID]
notifications:
  - "None."
audit:
  - "Field rule changes are audited and authorised by another user (MASTER_AUTHORIZE)."
acceptance:
  - "A Motor account without an engine number cannot be submitted, and the message names the engine number."
  - "A Motor item with a conduction sticker and no plate number meets the plate rule."
  - "A rule added by the Business Administrator for one product applies to the next submission without a release."
```

```fr
id: FR-NB-003
title: "Apply validation rules by product type"
brd: [BRNB.003 (p.49-50)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "New Account; Edit Account; New Quotation"
api: "GET /api/v1/accounts/{id}/check; POST /api/v1/accounts/{id}/submit"
description:
  - "When a quotation or account is saved, submitted or validated, BIBS applies the rules of its product - field presence (FR-NB-002), required documents per product (FR-NB-017), list values (LOV), value ranges and patterns defined as field rules (BRD-3 FRS FR-PM-012), duplicate rules (FR-NB-063), and the product's allowed features (segment, direct payment, FFY, multi-year term, mortgagee)."
  - "The Account page shows a check panel with every open finding before the user submits."
preconditions:
  - "The record has a product."
main_flow:
  - "The user saves or submits the record."
  - "BIBS evaluates the rules of the product and returns all findings together."
  - "The user corrects the fields named and submits again."
rules:
  - [R1, "Rules are evaluated on the server; the screen shows the same findings next to the fields.", Fixed, "-"]
  - [R2, "Product features are checked against the product: direct payment, FFY, multi-year term, mortgagee.", Configurable, Product master]
validations:
  - [Multi-year term above the product's maximum, "Product <code> allows a term of up to <n> year(s)", MULTI_YEAR_NOT_ALLOWED]
  - [Direct payment on a product that does not allow it, "Product <code> cannot be paid directly to the insurer", DIRECT_PAYMENT_NOT_ELIGIBLE]
  - [FFY on a product that is not FFY eligible, "Product <code> is not Free First Year eligible", FFY_NOT_ELIGIBLE]
  - [Mortgagee on a product without mortgage, "Product <code> carries no mortgagee", MORTGAGE_NOT_APPLICABLE]
  - [Period end not after start, The period end must be after the period start, ACCOUNT_PERIOD_INVALID]
  - [Value not in the list on the date, "'<code>' is not a valid value of <list> on <date>", LOV_VALUE_INVALID]
notifications:
  - "None."
audit:
  - "Refused submissions are not stored; the user sees the findings."
acceptance:
  - "A product without the multi-year feature refuses a 3-year term with MULTI_YEAR_NOT_ALLOWED."
  - "The check panel of a draft account lists all missing fields and documents at once."
```

```fr
id: FR-NB-004
title: "Use standard intake and shared document templates"
brd: [BRNB.004 (p.50; updated p.18)]
actor: "Marketing; System; Business Administrator (templates)"
priority: "Must have"
fit: "NEW"
screens: "Document Templates; New Quotation; New Proposal Request"
api: "GET /api/v1/doc-templates; POST /api/v1/doc-templates/{code}/versions"
description:
  - "Quotations and PRFs are captured on one standard intake: client or prospect, market segment, source channel, product, insurer and branch, period, validity, rating basis and risk items. The same minimum dataset applies to package and non-package risks; Marketing can still adjust terms before submission (workshop update, p.18)."
  - "Documents are generated from versioned templates with placeholders filled from the record. One template serves all lines. The template version used is stored on the quotation (intake version) and on each generated document."
preconditions:
  - "The templates of section 6.2 exist and are active."
main_flow:
  - "The user generates a document (quotation letter, quotation slip, proposal slip, placement slip, hold cover request, Insurance Advice, service invoice, e-policy e-mail)."
  - "BIBS merges the record's stored values into the current template version."
  - "BIBS stores the document with its template code and version."
alternate_flows:
  - "A new template version is saved by the Business Administrator. Documents generated before keep their version; new documents use the new one."
rules:
  - [R1, "Templates are versioned; a new version supersedes the old one from its effective date.", Fixed, "-"]
  - [R2, "Template layouts are the build's drafts until BDOI provides its layouts (Q03).", Configurable, Document Templates]
  - [R3, "Completeness is checked before quotation creation and submission (FR-NB-041).", Fixed, "-"]
validations:
  - [Template text blank, Enter the template text, TEMPLATE_BODY_REQUIRED]
fields_screen: "Document Templates (new version)"
fields:
  - [Code, Display, "-", "Template code (for example QUOTATION_LETTER)", "-"]
  - [Title, Text, "Yes", "-", "-"]
  - [Body, Long text, "Yes", "-", "Placeholders {{name}} from the record"]
  - [Effective from, Date, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Every template version is kept with user and time; each generated document records the template version."
acceptance:
  - "A quotation shows the intake template version it was captured with."
  - "A Fire and a Motor quotation letter are produced from the same template with their own item details."
  - "After a new template version, an earlier document still shows the version it was generated with."
```

```fr
id: FR-NB-005
title: "Route risks to TSU by rule"
brd: [BRNB.098 (p.7)]
actor: "System; TSU"
priority: "Must have"
fit: "NEW"
screens: "Products (TSU Rules tab); New Quotation (routing result); New Proposal Request; Account page (TSU Clearance)"
api: "GET /api/v1/catalog/tsu-rules; POST /api/v1/accounts/{id}/tsu-clearance"
description:
  - "TSU involvement is decided by rules, not by manual routing. A rule states a product class (package, non-package or any), line, minimum fleet units, minimum number of locations, a TSI threshold and an endorsement type, with a priority. The product setting ALWAYS or NEVER is checked first; a package above its package TSI limit always goes to TSU (PACKAGE_TSI_LIMIT); otherwise the first matching rule by priority applies."
  - "The result is shown on the quotation and PRF. A non-package risk goes to TSU through a PRF (FR-NB-050). An account whose rule requires TSU must have a TSU clearance before Processing validates it."
preconditions:
  - "TSU rules are active in the catalogue."
main_flow:
  - "The user enters the product, items, fleet size, locations and sums insured."
  - "BIBS evaluates the routing and shows whether TSU is required and why."
  - "For an account that needs TSU, the TSU officer records the clearance on the account page (**Record TSU Clearance**)."
alternate_flows:
  - "A package risk below every rule is quoted directly as a package quotation; a PRF for it is refused (PRF_NOT_NEEDED)."
rules:
  - [R1, "Delivered rules: NON_PACKAGE (every non-package risk), FLEET_5 (Motor fleet of 5 units or more), TSI_50M (TSI above PHP 50,000,000).", Configurable, TSU rules (thresholds Q04)]
  - [R2, "The product setting (ALWAYS, NEVER) overrides the rules.", Configurable, Product master]
  - [R3, "TSU clearance is recorded before the account is validated.", Fixed, "-"]
validations:
  - [Account needs TSU and has no clearance, "TSU must clear the account first: <reason>", TSU_CLEARANCE_REQUIRED]
  - [Clearance after validation, TSU clearance is given before the account is validated, TSU_CLEARANCE_CLOSED]
  - [PRF for a package risk that no rule sends to TSU, "No TSU rule applies to this package risk: quote it directly as a package quotation", PRF_NOT_NEEDED]
notifications:
  - "None; TSU sees accounts needing clearance in the account search."
audit:
  - "The routing result (rule and reason) is stamped on the quotation, PRF and account; the clearance is audited with user and time."
acceptance:
  - "A Motor quotation with 6 vehicles shows \"TSU required (FLEET_5)\"."
  - "An account that requires TSU cannot be validated before TSU records the clearance."
  - "A change to the TSI threshold applies to the next evaluation without a release."
```

## Common workflow, documents and controls

```fr
id: FR-NB-010
title: "Track the status of every record through defined stages"
brd: [BRNB.022 (p.60-61; updated p.18), BRNB.115 (p.18-19)]
actor: "System; all users"
priority: "Must have"
fit: "NEW"
screens: "Workflow panel and History tab of every client, quotation, PRF and account page; My Work; NB Dashboard"
api: "GET /api/v1/workflow/cases/by-record; GET /api/v1/workflow/definitions/{workflowCode}/stages"
description:
  - "Every client, quotation, PRF and account has a work case in one of four workflows (NB_CLIENT, NB_QUOTATION, NB_PROPOSAL, NB_ACCOUNT, section 5). The stages, their owners, SLA hours and the allowed transitions are data. A status changes only through an allowed transition, started by a user action or by the system (for example the payment sweep or e-policy confirmation)."
  - "Each change writes a history row with from and to stage, action, reason, comment, actor (user or SYSTEM) and time. The record page shows the current stage, its SLA due time and the assignee in the workflow panel, and the full history in a timeline."
  - "An account is \"stalled\" when it has not moved for NB_STALLED_DAYS days; stalled and overdue accounts are listed on the dashboard and in the Account Status Report (FR-NB-122)."
preconditions:
  - "None."
main_flow:
  - "A user performs a business action (for example Submit, Approve, Send, Validate)."
  - "BIBS checks that the transition exists from the current stage and that the user holds its permission."
  - "BIBS moves the case, writes the history row, sets the SLA due time of the new stage and notifies its owners."
alternate_flows:
  - "Action not allowed in the current stage. BIBS refuses it."
  - "Generic actions (return with a reason, decline, void) run from the workflow panel; business actions run only from their own screen."
rules:
  - [R1, "Only the transitions of section 5 are possible; there is no manual status edit.", Fixed, "-"]
  - [R2, "SLA hours per stage (defaults in section 5).", Configurable, Workflow stage definitions]
  - [R3, "Stalled = no stage movement for 5 days (default).", Configurable, Parameter NB_STALLED_DAYS]
  - [R4, "The account status field mirrors its work case and cannot be edited.", Fixed, "-"]
validations:
  - [Action not allowed in the stage, "'<action>' is not allowed while <reference> is in stage <stage>", WORKFLOW_TRANSITION_NOT_ALLOWED]
  - [User lacks the permission, "You are not allowed to '<action>'", WORKFLOW_ACTION_NOT_PERMITTED]
  - [Business action started from the panel, "'<action>' must be done from the record screen", WORKFLOW_ACTION_NOT_GENERIC]
  - [Reason missing, "Select a reason for '<action>'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "Stage entry: the holders of the stage's owner permission, or the assignee; the originator of the record when someone else changes it."
  - "SLA breach: alert WORK_SLA_BREACH from the daily alert check."
audit:
  - "The status history is insert-only; every row has the actor and time."
acceptance:
  - "An account's History tab shows every stage from Draft to Booked with user and time."
  - "An attempt to book an account in stage Placed is refused with WORKFLOW_TRANSITION_NOT_ALLOWED."
  - "An account without movement for 5 days is flagged stalled in the Account Status Report."
```

> [!NOTE] Status names of the BRD
> BRNB.022 lists Received, In Process, Sent to Client, Accepted, Rejected, Account Created, Submitted for Placement, Booked, Cancelled and Endorsed. They map to stages as follows: Received = request NEW or quotation DRAFT; In Process = DRAFT to APPROVED; Sent to Client = SENT_TO_CLIENT; Accepted = ACCEPTED; Rejected = NOT_PROCEEDED; Account Created = CONVERTED; Submitted for Placement = READY_FOR_PLACEMENT; Booked = BOOKED; Cancelled = CANCELLED or PLACEMENT_CANCELLED. "Endorsed" is not a stage: an endorsement is a booked invoice on a BOOKED account (FR-NB-115). BDOI confirms the stage list under Q10.

```fr
id: FR-NB-011
title: "Work queues, Processing trigger and workload assignment"
brd: [BRNB.096 (p.6), BRNB.080 (p.120), BRNB.097 (p.6-7)]
actor: "Marketing; Processing; Approver; TL"
priority: "Must have"
fit: "NEW"
screens: "My Work; Assign dialog"
api: "GET /api/v1/workflow/queue; GET /api/v1/workflow/counts; POST /api/v1/workflow/cases/{id}/claim; POST /api/v1/workflow/cases/{id}/assign"
description:
  - "A Marketing submission is the official trigger for Processing: when an account is submitted (FR-NB-064), its case enters the stage SUBMITTED, owned by ACCOUNT_PROCESS, and appears in the Processing queue with its status, submission date and originating unit. Processing does not act on e-mails."
  - "My Work lists the items of the user's queues (tabs Assigned to me, Team queue, All my queues) with reference, description, stage, originator, time in stage, due time and assignee. A user claims an item from the team queue. A TL or Approver (WORK_ASSIGN) assigns or re-assigns an item to a user who works the queue."
  - "New Business and Renewal are separate transactions: New Business records use the NB workflows and queues; renewals use the Renewal workflow (BRD-6)."
preconditions:
  - "The user has WORK_VIEW; assignment needs WORK_ASSIGN."
main_flow:
  - "The user opens My Work and filters by stage or searches by reference."
  - "The user claims an item, or a TL selects an item and clicks **Assign** and chooses the assignee."
  - "The assignee is notified and works the item from its record page."
alternate_flows:
  - "Claim of an item outside the user's queue. BIBS refuses it."
rules:
  - [R1, "An item can be assigned only to a user who holds the stage's owner permission.", Fixed, "-"]
  - [R2, "Processing actions start only from submitted accounts (stage SUBMITTED onwards).", Fixed, "-"]
  - [R3, "Assignment is manual (claim or assign); automatic round-robin allocation is not built.", Fixed, "-"]
validations:
  - [Claim outside the user's queue, This item is not in your queue, WORK_CLAIM_NOT_ALLOWED]
  - [Assignee does not work the queue, "<user> does not work this queue", WORK_ASSIGNEE_NOT_ELIGIBLE]
  - [Item already closed, The item is closed, WORK_CASE_CLOSED]
notifications:
  - "The assignee on assignment; the stage owners on stage entry."
audit:
  - "Claims and assignments are recorded on the work case with user and time."
acceptance:
  - "An account submitted by Marketing appears in the Processing team queue with its submission date and unit."
  - "A Processing TL re-assigns an item to another processor, who sees it under Assigned to me."
  - "A Marketing user cannot claim a Processing item."
```

```fr
id: FR-NB-012
title: "Void in-process records and distinguish reversal from cancellation"
brd: [BRNB.019 (p.58), BRNB.094 (p.5-6)]
actor: "Authorised user (maker or stage owner)"
priority: "Must have"
fit: "CHANGE"
screens: "Workflow panel (Void) of quotations, PRFs and accounts"
api: "POST /api/v1/workflow/cases/{id}/actions/void"
description:
  - "A record that is still in process can be voided with a reason from VOID_REASON and a comment: a quotation in DRAFT or FOR_REVIEW, a PRF in DRAFT, FOR_MKT_APPROVAL or WITH_TSU, an account in DRAFT, SUBMITTED or RETURNED_TO_MARKETING. Voiding is the BRD's \"deletion\": the record is removed from active lists and searches but kept, with its history, for audit."
  - "BIBS keeps two distinct lifecycle outcomes (BRNB.094). VOIDED is the internal reversal of a record before issuance, with no insurer impact. CANCELLED is the cancellation of a booked account after issuance, posted as a cancellation invoice (FR-NB-116). A placement can also be cancelled before issuance (PLACEMENT_CANCELLED, FR-NB-085)."
  - "**Built behaviour:** records are never physically deleted. A voided record is hidden from standard lists and reports and found with the filter Include voided."
preconditions:
  - "The record is in a stage that allows void; the user holds the transition's permission."
main_flow:
  - "The user clicks **Void** in the workflow panel."
  - "BIBS asks for confirmation, the reason and a comment."
  - "BIBS moves the record to VOIDED and records the reason."
alternate_flows:
  - "Record already finalised (for example APPROVED quotation, placed or booked account). The Void action is not offered; a direct call is refused."
rules:
  - [R1, "Void reasons: Duplicate record; Encoding error; Client withdrew the request; Others (see comment).", Configurable, LOV VOID_REASON]
  - [R2, "Void is allowed only in the stages listed above.", Fixed, "-"]
  - [R3, "Cancellation reasons (placement and booking): Client request; Non-payment of premium; Loan cancelled or paid off; Insurer request; Others.", Configurable, LOV CANCELLATION_REASON]
validations:
  - [Void without reason, "Select a reason for 'Void'", WORKFLOW_REASON_REQUIRED]
  - [Void of a finalised record, "'void' is not allowed while <reference> is in stage <stage>", WORKFLOW_TRANSITION_NOT_ALLOWED]
notifications:
  - "The originator when someone else voids the record."
audit:
  - "Void recorded with user, time, reason and comment; the record stays in the audit trail."
acceptance:
  - "A draft account voided with reason Duplicate record disappears from the Accounts list and appears with Include voided."
  - "A booked account has no Void action; it is cancelled through a cancellation (FR-NB-116)."
  - "The Account Status Report shows VOIDED and CANCELLED as different outcomes."
```

```fr
id: FR-NB-013
title: "Protect every document that leaves BIBS"
brd: [BRNB.013 (p.55-56), BRNB.035 (p.75-76)]
actor: "System; sending user"
priority: "Must have"
fit: "NEW"
screens: "Send via Email dialog of quotations, PRFs, slips, Insurance Advice and e-policy dispatch; Outbound Messages"
api: "Messaging outbox (internal); GET /api/v1/messages/by-record; POST /api/v1/messages/{id}/retry"
description:
  - "Every document e-mailed outside BDOI is protected - quotations, quotation slips, proposal slips, comparative tables, placement slips, hold cover requests, Insurance Advices, e-policies and service invoices. PDF files are encrypted (AES) with a password, Excel files are password protected, and the password is sent in a separate e-mail. Single and batch sending work the same way."
  - "Inside BIBS, documents open only for users with the view permission of their record (ATTACHMENT_VIEW and the record's permission). Insurers have no access to BIBS."
preconditions:
  - "The document is generated or attached on its record."
main_flow:
  - "The user sends one or several documents from the record."
  - "BIBS protects each attachment with a password from the password policy and queues the e-mail."
  - "BIBS queues a second e-mail with the password only."
  - "The dispatch job sends both and records the outcome."
alternate_flows:
  - "A file type that cannot be protected (neither PDF nor Excel) is refused."
  - "Delivery failure. The message shows FAILED with the reason; the user resends it."
rules:
  - [R1, "Protection cannot be switched off for documents sent outside BDOI.", Fixed, "-"]
  - [R2, "Password: a generated 12-character password per e-mail until BDOI states its convention (Q07).", Configurable, Password policy (parked Q07)]
  - [R3, "Mail is dispatched every 2 minutes and right after the commit; 3 delivery attempts before FAILED.", Configurable, Parameter MAIL_MAX_ATTEMPTS; job MAIL_DISPATCH]
validations:
  - [File cannot be protected, "<file> cannot be password protected (only PDF and Excel files can)", DOCUMENT_NOT_PROTECTABLE]
  - [No recipient, Enter at least one recipient, EMAIL_RECIPIENT_REQUIRED]
  - [Invalid address, "Invalid e-mail address: <address>", EMAIL_ADDRESS_INVALID]
  - [Resend of a message that did not fail, Only a failed message can be resent, MESSAGE_NOT_FAILED]
fields_screen: "Send via Email dialog"
fields:
  - [To / Cc, E-mail list, "Yes (To)", Contact of the record, Valid addresses]
  - [Subject / Body, Text, "Yes", Template, "-"]
  - [Password hint, Text, "No", Parameter EPOLICY_PASSWORD_HINT, Shown to the client]
notifications:
  - "The recipient receives the protected document and, separately, the password."
audit:
  - "Each message is logged with recipients, time, subject, attachment name and SHA-256 hash, and its outcome (sent or failed with the reason)."
acceptance:
  - "A quotation e-mailed to a client arrives as an encrypted PDF, and the password arrives in a second e-mail."
  - "A batch of 10 e-policies is sent with each file protected and each password e-mail logged."
  - "A user without view rights on an account cannot open its documents."
```

```fr
id: FR-NB-014
title: "Enforce validation and approval before a document is sent"
brd: [BRNB.014 (p.56), BRNB.005 (p.50-51)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "Quotation page; PRF page (Quotation Slip, Proposal Slip tabs)"
api: "POST /api/v1/quotations/{id}/approve; POST /api/v1/proposals/{id}/quotation-slip/approve; POST /api/v1/proposals/{id}/proposal-slip/approve"
description:
  - "A document leaves BIBS only after its checks and approval. A quotation is sent only in stage APPROVED; a quotation slip is sent only by its approval; a proposal slip reaches Marketing only by its approval; a placement slip is generated only when the placement prerequisites are met (FR-NB-080)."
  - "Approvals follow four-eyes: the approver is never the maker or submitter of a quotation or PRF, nor the preparer of a slip."
  - "**Built behaviour:** the BRD asks for multi-level approval (Marketing TL, then TH, then UH for PRFs). BIBS has one approval stage per document; the approver permission decides who approves. A chain by amount, product or segment waits for Q05."
preconditions:
  - "None."
main_flow:
  - "The maker submits the document."
  - "BIBS runs the document's checks and moves it to its approval stage."
  - "An approver who is not the maker approves it; only then is the send action offered."
rules:
  - [R1, "Send actions are hidden and refused until the approval stage is passed.", Fixed, "-"]
  - [R2, "Four-eyes cannot be switched off.", Fixed, "-"]
  - [R3, "One approval stage per document type (chain Q05).", Configurable, Workflow stage owner permission]
validations:
  - [Quotation not approved, "Quotation <number> must be approved before it is sent", QUOTATION_NOT_APPROVED]
  - [Quotation approved by its maker, A quotation is approved by someone other than its maker, QUOTATION_FOUR_EYES]
  - [PRF approved by its maker, A PRF is approved by someone other than its maker, PRF_FOUR_EYES]
  - [Slip approved by its preparer, The quotation slip is approved by another TSU officer, QS_FOUR_EYES]
  - [Proposal slip approved by its preparer, The proposal slip is approved by another TSU officer, PS_FOUR_EYES]
notifications:
  - "The approvers when a document enters its approval stage."
audit:
  - "Submit and approve recorded with user and time."
acceptance:
  - "A draft quotation cannot be sent, even through the API."
  - "The maker of a quotation cannot approve it."
  - "A quotation slip is e-mailed to insurers only after a second TSU officer approves it."
```

```fr
id: FR-NB-015
title: "Notify users and pass data to BDOI systems"
brd: [BRNB.015 (p.56)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "Notifications (bell); Notification preferences (My Profile)"
api: "GET /api/v1/notifications/unread-count; GET | PUT /api/v1/notifications/preferences/{eventCode}"
description:
  - "BIBS notifies users in the application on every stage entry (the owners of the new stage or the assignee), when someone other than the originator changes a record, on returns, on failed dispatches, on hold covers about to expire and when the monthly KYC list is ready. Users choose per event whether they also receive an e-mail."
  - "**Built behaviour:** the BRD also asks for data synchronised with BDOI tracking and reporting systems. BIBS publishes events for its own modules (Operations, Collections, Renewal); a feed to other BDOI systems waits for Q08, which names QPS and EBIX for client contact updates only (BRD-9)."
preconditions:
  - "None."
main_flow:
  - "An event occurs (stage change, return, failure, due date)."
  - "BIBS creates the in-app notification and, if the user's preference says so, queues an e-mail."
  - "The user opens the notification and goes to the record."
rules:
  - [R1, "Notification preferences (in-app / e-mail) per event type.", Configurable, Notification preferences]
  - [R2, "No external feed until Q08 is answered.", Fixed, "-"]
validations: []
notifications:
  - "This FR is the notification service."
audit:
  - "Notifications are stored with recipient, time and read status."
acceptance:
  - "The Processing team receives a notification when an account is submitted."
  - "A user who turns on e-mail for returns receives an e-mail when an account is returned to them."
```

```fr
id: FR-NB-016
title: "Keep a complete audit trail"
brd: [BRNB.016 (p.56)]
actor: "System; users with access (read)"
priority: "Must have"
fit: "FIT"
screens: "History tab of every record; Audit Trail"
api: "GET /api/v1/audit-logs"
description: "BIBS records every creation, edit, approval, return, void, sending, generation and upload for clients, quotations, PRFs, accounts, slips, e-policies, invoices, lists of values and access requests. Each row holds who, when, what and the before and after values. Users review the trail of a record on its History tab; the Business Administrator and Auditor use the Audit Trail screen (FR-NB-136)."
preconditions:
  - "None."
main_flow:
  - "A user or the system performs an action."
  - "BIBS writes the audit row in the same transaction as the change."
  - "The History tab shows the workflow history and the audit rows in time order."
rules:
  - [R1, "Audit rows are append-only; the database refuses updates and deletes.", Fixed, "-"]
  - [R2, "Retention: 5 years online, 15 years archive (p.82).", Configurable, Retention rules (FR-NB-137)]
validations: []
notifications:
  - "None."
audit:
  - "This FR is the audit."
acceptance:
  - "A changed client e-mail shows the old and new value, user and time on the client's History tab."
  - "No screen or API allows audit rows to be changed or deleted."
```

```fr
id: FR-NB-017
title: "Upload, name and link documents"
brd: [BRNB.026 (p.65-66), BRNB.055 (p.104-105)]
actor: "Marketing; Processing; System"
priority: "Must have"
fit: "CHANGE"
screens: "Documents tab of clients, quotations, PRFs and accounts; KYC & Documents tab"
api: "POST /api/v1/attachments/batch; POST /api/v1/attachments/{id}/links; DELETE /api/v1/attachments/{id}"
description:
  - "Users upload one or several files at once (up to 50) to a client or account, choose the document type, and either keep the source file name or let BIBS nominate a name with the syntax `<REFERENCE>_<DOCTYPE>_<n>.<ext>`. One file can be linked to several accounts (for example one IDF for a fleet). Users remove a file they uploaded when the record is still editable."
  - "Each file is checked for type, size, content signature and malware before it is stored. The required documents per product (document rules) are checked at submission."
preconditions:
  - "The user has ATTACHMENT_MANAGE and access to the record."
main_flow:
  - "The user clicks **Upload**, selects the files and the document type, and chooses Keep file name or Nominate name."
  - "BIBS validates each file and stores it with the record, the type and the name."
  - "The user links a file to other accounts when needed."
alternate_flows:
  - "One file of the batch fails a check. BIBS stores the valid files and lists the failed one with its reason."
rules:
  - [R1, "Allowed types: pdf, png, jpg / jpeg, xlsx, xls, docx, doc, csv, ods, odt, msg, eml.", Configurable, Allowed file types (list and size Q23)]
  - [R2, "Maximum file size 10 MB (default).", Configurable, Setting BROKERVERSE_ATTACHMENT_MAX_SIZE]
  - [R3, "Nominated names follow <REFERENCE>_<DOCTYPE>_<n>.<ext> until BDOI gives its syntax (Q23).", Configurable, Naming syntax (Q23)]
  - [R4, "Document types are the LOV DOCUMENT_TYPE (IDF, valid ID, birth certificate, account list, quotation, client acceptance e-mail, e-policy, official receipt, policy copy, KYC form, proof of address, SEC certificate, GIS, secretary's certificate, request e-mail, payment confirmation, hold cover, Insurance Advice, quotation slip, insurer response, comparative table, proposal slip, others).", Configurable, LOV DOCUMENT_TYPE]
  - [R5, "Required documents per product: Motor - IDF; Personal Accident - valid ID (defaults).", Configurable, Document rules (catalogue)]
validations:
  - [Empty file, The file is empty, ATTACHMENT_EMPTY]
  - [File too large, "The file exceeds the maximum size of <n> MB", ATTACHMENT_TOO_LARGE]
  - [Type not allowed, "Only these file types are allowed: <list>", ATTACHMENT_TYPE_NOT_ALLOWED]
  - [Content does not match the extension, "The file content does not match its .<type> type", ATTACHMENT_CONTENT_MISMATCH]
  - [Malware found, "The file was rejected by the malware scan: <detail>", ATTACHMENT_INFECTED]
  - [More than 50 files, Upload between 1 and 50 files at a time, DOCUMENT_FILE_COUNT]
  - [Nominate without a reference, A reference is needed to nominate the file names, DOCUMENT_REFERENCE_REQUIRED]
  - [Required document missing at submission, "Upload the mandatory documents: <types>", MISSING_DOCUMENTS]
fields_screen: "Upload documents"
fields:
  - [Files, File picker, "Yes", "-", "1-50 files; allowed types and size"]
  - [Document type, List, "Yes", LOV DOCUMENT_TYPE, Active value]
  - [File name, Option, "Yes", Keep source name / Nominate, "-"]
notifications:
  - "None."
audit:
  - "Upload, link, unlink and removal are audited with user, time, file name and hash."
acceptance:
  - "Five files uploaded in one step are stored with their type and linked to the account."
  - "An .exe file is refused with ATTACHMENT_TYPE_NOT_ALLOWED."
  - "A Motor account without an IDF cannot be submitted (MISSING_DOCUMENTS)."
  - "A file linked to three accounts appears on the Documents tab of each."
```

```fr
id: FR-NB-018
title: "View and download client and account documents"
brd: [BRNB.056 (p.105)]
actor: "Marketing; Processing"
priority: "Must have"
fit: "CHANGE"
screens: "Documents tab (client, account); KYC & Documents tab"
api: "GET /api/v1/attachments/{id}/content; GET /api/v1/attachments/zip?ids="
description: "The client page shows the client-level documents (KYC and others) and the account page the account-level documents (IDF, e-policy, policy copy and others), with type, name, size, uploader and date. Users open a document, download one, or select several and download them as one ZIP file, which the browser saves to the folder the user chooses."
preconditions:
  - "The user has ATTACHMENT_VIEW and access to the record."
main_flow:
  - "The user opens the Documents tab."
  - "The user selects one or more documents and clicks **Download** (one file) or **Download ZIP**."
rules:
  - [R1, "A ZIP holds up to 50 files.", Fixed, "-"]
  - [R2, "Every file is verified against its stored checksum before download.", Fixed, "-"]
validations:
  - [More than 50 selected, Select between 1 and 50 documents, DOCUMENT_FILE_COUNT]
  - [Stored file damaged, Stored file failed its checksum verification, ATTACHMENT_INTEGRITY_FAILURE]
notifications:
  - "None."
audit:
  - "Downloads are logged."
acceptance:
  - "A user downloads the ID and KYC form of a client as one ZIP."
  - "The account page shows the e-policy uploaded by Processing."
```

```fr
id: FR-NB-019
title: "Upload records in bulk with a standard template"
brd: [BRNB.039 (p.80-81), BRNB.064 (p.110), BRNB.024 (p.62-63)]
actor: "Marketing; Processing; System"
priority: "Must have"
fit: "NEW"
screens: "Bulk Uploads; Bulk Upload wizard (/bulk/<type>)"
api: "GET /api/v1/bulk/handlers/{code}/template; POST /api/v1/bulk/jobs; POST /api/v1/bulk/jobs/{id}/commit; GET /api/v1/bulk/jobs/{id}/report"
description:
  - "All bulk processing uses one upload wizard. The user downloads the current template (Excel with an instructions sheet), fills it, and uploads it as .xlsx, .ods, .csv or .txt. BIBS checks the header against the template, sanitises each row (trim, case, format), validates every row, and shows valid and invalid rows for review. The user commits; each valid row is processed in its own transaction, so one failure does not stop the others. A summary and an error report (Excel, one line per failed row with the reason) are available after commit."
  - "Delivered uploads - CLIENT_CREATE (bulk client creation and update), QUOTATION_CREATE (bulk quotations), QUOTATION_ACCEPTANCE (bulk quotation acceptance), QUOTATION_REQUEST (quotation requests), ACCOUNT_CREATE (bulk account creation), ACCOUNT_UPDATE (bulk account update), FFY_TAGGING (Free First Year tagging), BOOKING_UPLOAD (booking upload)."
preconditions:
  - "The user has BULK_PROCESS and the permission of the upload."
main_flow:
  - "The user opens Bulk Uploads, selects the upload type and downloads the template."
  - "The user uploads the completed file with the parameters of the type (for example product and market segment)."
  - "BIBS validates the file and lists each row as valid or invalid with the reason."
  - "The user clicks **Commit**; BIBS processes the valid rows and shows the counts of processed and failed rows."
  - "The user downloads the error report, corrects the failed rows and uploads them again."
alternate_flows:
  - "Wrong template. The file is refused and nothing is processed."
  - "Same file uploaded twice. BIBS refuses it and names the earlier upload."
  - "The user cancels the upload before commit."
rules:
  - [R1, "Only the current template is accepted (header check).", Fixed, "-"]
  - [R2, "Maximum 5,000 data rows per file (default).", Configurable, Parameter BULK_MAX_ROWS]
  - [R3, "Valid rows are committed; invalid rows are never committed.", Fixed, "-"]
  - [R4, "Sanitisation trims, upper-cases codes and normalises formats; BDOI's own criteria wait for Q14.", Configurable, Upload handler (Q14)]
validations:
  - [Wrong file type, "Upload an Excel (.xlsx), OpenDocument (.ods), CSV (.csv) or text (.txt) file", BULK_FILE_TYPE]
  - [Missing columns, "The file does not follow the current template; missing column(s): <columns>", BULK_TEMPLATE_MISMATCH]
  - [No data rows, The file has no data rows, BULK_FILE_EMPTY]
  - [Too many rows, "The file has <n> rows; the maximum is <max>", BULK_FILE_TOO_LARGE]
  - [Duplicate file, "This file was already uploaded as <job> (<file>)", BULK_DUPLICATE_FILE]
  - [Upload type not allowed, "You are not allowed to use '<upload>'", BULK_NOT_PERMITTED]
notifications:
  - "The uploader sees the job status in Bulk Uploads."
audit:
  - "Jobs (BLK-yyyy-n) and every row with its outcome are kept for audit."
acceptance:
  - "A file with 100 rows, 3 of them invalid, commits 97 rows and the error report lists the 3 with reasons."
  - "A file with a missing column is refused with BULK_TEMPLATE_MISMATCH and no row is processed."
  - "The template downloaded from the wizard is accepted unchanged."
```

```fr
id: FR-NB-020
title: "Authoritative source per data element"
brd: [BRNB.092 (p.5)]
actor: "Business unit; System"
priority: "Must have"
fit: "NEW"
screens: "-"
api: "-"
description:
  - "The BRD asks for one authoritative source per key data element (client, vehicle, property, address, policy, invoice), with primary and fallback sources, enforced as read-only or editable fields, and conflicts logged."
  - "**Built behaviour:** the data ownership register is not built; it waits for BDOI's list of sources (Q37). In the build, BIBS is the source for clients, accounts and invoices it creates; the policy number is owned by the account and set only from the insurer's e-policy (FR-NB-101); BRD-7 and BRD-9 confirm that the policy number flows from the policy system and that QPS and EBIX stay the systems of record for contact data during coexistence (R5)."
preconditions:
  - "BDOI answers Q37."
main_flow:
  - "Parked: no flow in this phase."
rules:
  - [R1, "The policy number of an account changes only through the e-policy confirmation.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Not applicable until built."
acceptance:
  - "The policy number field of an account is read-only on the account screens."
```

## Client onboarding and KYC

```fr
id: FR-NB-030
title: "Search clients and view their details"
brd: [BRNB.046 (p.97), BRNB.044 (p.96-97)]
actor: "Marketing; Processing"
priority: "Must have"
fit: "FIT"
screens: "Clients; Client page"
api: "GET /api/v1/crm/clients; GET /api/v1/crm/clients/{id}"
description: "Users search clients by one or several criteria together and open the client page with the complete details - identity, contact and address, segment and bank relationship, onboarding status, KYC documents, tags and instructions, quotations, confirmed proposals, linked records and history."
preconditions:
  - "The user has CLIENT_VIEW."
main_flow:
  - "The user opens Clients and enters one or more criteria."
  - "The user clicks **Apply Filters**; BIBS lists the matching clients with code, name, type, segment, bank flag, status, KYC status, KYC review date and contact."
  - "The user opens a client to view its full details."
rules:
  - [R1, "Criteria combine with AND; name matches on any part of the name.", Fixed, "-"]
  - [R2, "Inactive clients are listed only when the status filter asks for them.", Fixed, "-"]
validations: []
fields_screen: "Clients (search panel)"
fields:
  - [Prospect or Client Code, Text, "No", "-", "PR- or CL- code"]
  - [Name Contains, Text, "No", "-", "-"]
  - [TIN / ID Number / E-mail / Mobile, Text, "No", "-", "-"]
  - [Status, List, "No", "Prospect, KYC review, KYC verified, Confirmed, Inactive", "-"]
  - [KYC Status, List, "No", "Complete, missing, expired", "-"]
  - [Market Segment, List, "No", LOV MARKET_SEGMENT, "-"]
  - [BDO Bank Client, List, "No", "Yes / No", "-"]
notifications:
  - "None."
audit:
  - "Client views are not audited; changes are (FR-NB-016)."
acceptance:
  - "A search by mobile number and segment returns only the clients matching both."
  - "The client page shows the client's accounts on the Linked Records tab."
```

```fr
id: FR-NB-031
title: "Create a client (individual or corporate)"
brd: [BRNB.048 (p.98), BRNB.030 (p.69-71), BRNB.029 (p.68-69)]
actor: "Marketing AO / TL"
priority: "Must have"
fit: "CHANGE"
screens: "New Client; Client page"
api: "POST /api/v1/crm/clients; GET /api/v1/crm/clients/duplicates"
description:
  - "The AO creates an individual or corporate client. A new client is always a **prospect** with a prospect code PR-yyyy-nnnnnn. A prospect needs only the client type and the name (the minimum data of BRNB.029), so a quotation can start at once; the other information and the KYC documents are completed during onboarding (FR-NB-034)."
  - "BIBS validates formats (TIN, e-mail, mobile, birth date, minimum age) and list values, checks duplicates while the user types (FR-NB-033), and flags a client whose information is incomplete against the minimum-field parameters. The flag is shown on the client, and on every quotation and account of the client, until the information is complete."
  - "A client is saved as soon as it is valid; it can be completed later. Cancel leaves without saving."
preconditions:
  - "The user has CLIENT_MAINTAIN."
main_flow:
  - "The AO clicks **New Client** and selects the client type."
  - "The AO enters the identity, contact and address, segment and KYC profile fields."
  - "BIBS shows live duplicate warnings."
  - "The AO saves. BIBS validates, assigns the prospect code and starts the onboarding case (stage PROSPECT)."
  - "BIBS offers **Open Client** or **Create Another**."
alternate_flows:
  - "Hard duplicate found. BIBS refuses the save and names the existing client codes."
  - "Invalid field. BIBS names the first invalid field; the client is not saved."
rules:
  - [R1, "Prospect codes are PR-<yyyy>-nnnnnn; client codes CL-<yyyy>-nnnnnn are issued at confirmation.", Fixed, "-"]
  - [R2, "An individual needs last and first name; a corporate needs the registered name.", Fixed, "-"]
  - [R3, "Minimum information of an individual - birth date, TIN, ID, contact, address, market segment; of a corporate - TIN, contact, address, market segment.", Configurable, "Parameters CLIENT_MIN_FIELDS_INDIVIDUAL, CLIENT_MIN_FIELDS_CORPORATE"]
  - [R4, "An individual policyholder is at least 18 years old.", Configurable, Parameter CLIENT_MIN_AGE]
  - [R5, "BDO bank standard fields and CIF integration wait for Q16; the CIF number is captured manually.", Configurable, KYC profile fields (Q16)]
validations:
  - [Name missing, "Enter the <last name / first name / registered name>", CLIENT_NAME_REQUIRED]
  - [TIN format, Enter the TIN as 000-000-000-000, CLIENT_TIN_FORMAT]
  - [E-mail format, Enter a valid e-mail address, CLIENT_EMAIL_FORMAT]
  - [Mobile format, Enter the mobile number as 09xxxxxxxxx or +639xxxxxxxxx, CLIENT_MOBILE_FORMAT]
  - [Birth date not in the past, The birth date must be in the past, CLIENT_BIRTH_DATE]
  - [Individual under age, "An individual policyholder must be at least <n> years old", CLIENT_UNDER_AGE]
  - [List value not valid, "'<code>' is not a valid value of <list> on <date>", LOV_VALUE_INVALID]
  - [Hard duplicate, "This client already exists: <codes>", CLIENT_DUPLICATE]
  - [Company not chosen, Select the company of the client, COMPANY_REQUIRED]
fields_screen: "New Client"
fields:
  - [Client type, Option, "Yes", "Individual / Corporate", Fixed after confirmation]
  - [Last name / First name / Middle name / Suffix, Text, Cond., "-", Last and first name for individuals]
  - [Registered name / Nature of business, Text, Cond., "-", Registered name for corporates]
  - [Birth date, Date, "No", "-", "In the past; age >= CLIENT_MIN_AGE"]
  - [Nationality / Civil status, List, "No", "LOV NATIONALITY, CIVIL_STATUS", Individuals]
  - [Occupation, Text, "No", "-", "-"]
  - [TIN, Text, "No", "-", "000-000-000-000"]
  - [ID type / ID number, List / Text, "No", LOV ID_TYPE, "-"]
  - [E-mail / Mobile / Landline, Text, "No", "-", "E-mail format; mobile 09xxxxxxxxx or +639xxxxxxxxx"]
  - [Street address / City / Province / Postal code, Text, "No", "-", "-"]
  - [Market segment, List, "No", LOV MARKET_SEGMENT, "-"]
  - [BDO bank client / BDO CIF number, Check box / Text, "No", "-", "-"]
  - [Source of funds / KYC risk rating, List, "No", "LOV SOURCE_OF_FUNDS, KYC_RISK_RATING", "-"]
notifications:
  - "None on creation; the onboarding case appears in the AO's queue (stage PROSPECT)."
audit:
  - "Creation audited with all values; a blocked duplicate attempt is audited separately."
acceptance:
  - "An AO creates a prospect with only the type and name; it gets a PR code and the incomplete flag."
  - "A TIN entered as 123456789 is refused with CLIENT_TIN_FORMAT."
  - "A client with the same TIN as an existing client is refused and the message names the existing code."
```

```fr
id: FR-NB-032
title: "Update client details"
brd: [BRNB.049 (p.98-99), BRNB.030 (p.69-71)]
actor: "Marketing AO / TL"
priority: "Must have"
fit: "CHANGE"
screens: "Edit Client; Client page (KYC & Documents)"
api: "PUT /api/v1/crm/clients/{id}; POST /api/v1/crm/clients/{id}/kyc-documents"
description: "The AO edits a client with the same validations as creation (FR-NB-031) and uploads further documents. Changes are saved at once; Cancel discards them. The updated client moves on through onboarding with **Submit KYC** (FR-NB-034). The type of a confirmed client cannot change, and an inactive client cannot be changed."
preconditions:
  - "The user has CLIENT_MAINTAIN; the client is not inactive."
main_flow:
  - "The AO opens the client and clicks **Edit**."
  - "The AO changes the fields and uploads documents on the KYC & Documents tab."
  - "The AO saves; BIBS validates, re-checks duplicates and updates the completeness flag."
rules:
  - [R1, "Every change is kept with the old and new value.", Fixed, "-"]
validations:
  - [Inactive client, An inactive client cannot be changed, CLIENT_INACTIVE]
  - [Type change on a confirmed client, The client type of a confirmed client cannot be changed, CLIENT_TYPE_LOCKED]
  - [Formats and duplicates, As FR-NB-031, "CLIENT_TIN_FORMAT, CLIENT_DUPLICATE ..."]
notifications:
  - "None."
audit:
  - "Before and after values on the client's History tab."
acceptance:
  - "A changed address is visible at once and the History tab shows the old address."
  - "An inactive client has no Edit action."
```

```fr
id: FR-NB-033
title: "Prevent duplicate clients"
brd: [BRNB.032 (p.72-73), BRNB.030 (p.69-71)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "New Client; Edit Client (duplicate warnings)"
api: "GET /api/v1/crm/clients/duplicates"
description:
  - "BIBS stores normalised keys of each client and compares them before a client is created, changed or confirmed. Hard keys block the action - TIN; ID type and number; last name, first name and birth date. Soft keys only warn - e-mail, mobile and corporate name. The form shows matches while the user types, with the existing client codes."
  - "Duplicate accounts are covered by FR-NB-063; for endorsements, duplicates are allowed and flagged."
preconditions:
  - "None."
main_flow:
  - "The user enters identity data; BIBS lists possible matches."
  - "On save, BIBS blocks a hard match and names the existing codes."
alternate_flows:
  - "Soft match only. BIBS saves the client and keeps the warning visible."
rules:
  - [R1, "Hard keys - TIN; ID type + number; last + first name + birth date. Soft keys - e-mail, mobile, corporate name.", Configurable, "Duplicate keys (precedence Q17)"]
  - [R2, "Keys are compared normalised (case, spaces, punctuation).", Fixed, "-"]
validations:
  - [Hard duplicate, "This client already exists: <codes>", CLIENT_DUPLICATE]
notifications:
  - "None."
audit:
  - "Every blocked attempt is logged with user, time and the matching codes, even though the save is refused."
acceptance:
  - "A second client with the same last name, first name and birth date is refused and the attempt is in the audit trail."
  - "A client with the same mobile as another is saved with a warning."
```

```fr
id: FR-NB-034
title: "Onboard the client - KYC, verification and confirmation"
brd: [BRNB.090 (p.4), BRNB.101 (p.9-10), BRNB.030 (p.69-71), BRNB.029 (p.68-69)]
actor: "Marketing AO (maker); Marketing TL (verifier)"
priority: "Must have"
fit: "CHANGE"
screens: "Client page (workflow panel, KYC & Documents tab)"
api: "POST /api/v1/crm/clients/{id}/submit-kyc | verify-kyc | confirm | deactivate; GET .../kyc-checklist"
description:
  - "Onboarding is a defined step before New Business is booked. The client moves through the workflow NB_CLIENT - PROSPECT, KYC_REVIEW, KYC_VERIFIED, CONFIRMED (section 5.1). The AO uploads the mandatory KYC documents and submits the KYC; a verifier who is neither the client's creator nor the submitter verifies it; the client is then confirmed."
  - "Confirmation issues the client code CL-yyyy-nnnnnn and keeps the prospect code, so the conversion stays traceable; every screen and report shows both codes and the status (prospect or confirmed client)."
  - "A quotation can be made for a prospect. Accounts are created, validated and booked only for a confirmed client, so booking and issuance are blocked until the client is complete (BRNB.029 AC8)."
preconditions:
  - "The client is a prospect; the user has CLIENT_MAINTAIN (submit, confirm) or CLIENT_APPROVE (verify)."
main_flow:
  - "The AO uploads the mandatory KYC documents shown in the KYC checklist."
  - "The AO clicks **Submit the KYC for verification** (KYC_REVIEW)."
  - "The TL reviews the documents and clicks **Verify the KYC** (KYC_VERIFIED). BIBS sets the next KYC review date from the risk rating."
  - "The AO or TL clicks **Confirm the client**. BIBS issues the client code and opens the client's party record (CONFIRMED)."
alternate_flows:
  - "Return. The verifier returns the KYC with a reason; the client goes back to PROSPECT."
  - "Deactivate. A client in any stage is deactivated with a reason from CLIENT_DEACTIVATION_REASON (INACTIVE)."
  - "Periodic review. For a confirmed client, the verifier records the periodic KYC review (**review_kyc**), which sets the next review date."
rules:
  - [R1, "Mandatory KYC documents - individual - KYC form, valid ID; corporate - KYC form, SEC / DTI certificate, GIS, secretary's certificate.", Configurable, "LOV KYC_DOCS_INDIVIDUAL, KYC_DOCS_CORPORATE"]
  - [R2, "Submit and confirm need the minimum client information and the mandatory documents.", Fixed, "-"]
  - [R3, "The verifier is neither the client's creator nor the KYC submitter.", Fixed, "-"]
  - [R4, "Next review - 36 months (standard and low risk), 12 months (high risk).", Configurable, "Parameters KYC_REVIEW_MONTHS, KYC_REVIEW_MONTHS_HIGH_RISK (Q21)"]
  - [R5, "Deactivation reasons - Duplicate client record; Client request; Deceased / dissolved; No business relationship; Compliance decision; Others.", Configurable, LOV CLIENT_DEACTIVATION_REASON]
  - [R6, "SLA - PROSPECT 72 hours, KYC_REVIEW 24 hours, KYC_VERIFIED 24 hours (defaults).", Configurable, Workflow stage definitions]
validations:
  - [Documents missing, "Upload the mandatory KYC documents before you <step>: <documents>", KYC_DOCUMENTS_MISSING]
  - [Information incomplete, "Complete the client information before you <step>: <fields>", CLIENT_INFO_INCOMPLETE]
  - [Verifier is the maker, The KYC must be verified by a user other than its maker, KYC_FOUR_EYES]
  - [Confirm without verified KYC, "Client <code> has no verified KYC", KYC_NOT_VERIFIED]
  - [Confirm a client that is not a prospect, Only a prospect can be confirmed, CLIENT_NOT_PROSPECT]
  - [Account for an unconfirmed client, "Client <code> is not yet confirmed (onboarding and KYC incomplete)", CLIENT_NOT_CONFIRMED]
  - [Deactivate without reason, "Select a reason for 'Deactivate'", WORKFLOW_REASON_REQUIRED]
fields_screen: "KYC & Documents tab"
fields:
  - [Document type, List, "Yes", LOV DOCUMENT_TYPE, Mandatory types listed in the KYC checklist]
  - [File, File, "Yes", "-", Allowed types and size (FR-NB-017)]
notifications:
  - "The CLIENT_APPROVE holders when a KYC is submitted; the AO on return and verification."
audit:
  - "Each onboarding step with user, time and reason; the history is kept under the prospect and the client code."
acceptance:
  - "A prospect without a valid ID cannot be submitted for KYC; the message names the missing document."
  - "The AO who submitted the KYC cannot verify it."
  - "After confirmation, the client shows both PR and CL codes and the status Confirmed client."
  - "An account cannot be created from a quotation of an unconfirmed client (CLIENT_NOT_CONFIRMED)."
```

```fr
id: FR-NB-035
title: "Create and update clients in bulk"
brd: [BRNB.047 (p.97-98), BRNB.065 (p.110-111)]
actor: "Marketing; System"
priority: "Must have"
fit: "NEW"
screens: "Bulk Upload (Bulk client creation and update, /bulk/CLIENT_CREATE)"
api: "POST /api/v1/bulk/jobs (handler CLIENT_CREATE)"
description:
  - "Marketing uploads a list of clients on the standard template (FR-NB-019). For each row, BIBS updates the client found by prospect or client code (blank cells keep the current values), else the client found by a single hard duplicate key, else creates a new prospect. Every row is validated like the screen (formats, lists, duplicates), invalid rows are flagged with the reason and not saved."
  - "Bulk quotation and account uploads (FR-NB-046, FR-NB-065) use the same client look-up and create prospects for new names, which follow the onboarding of FR-NB-034."
  - "**Built behaviour:** a bulk row is saved directly when valid; there is no separate draft state for bulk rows. Review before commit takes the place of the BRD's save-draft and cancel."
preconditions:
  - "The user has BULK_PROCESS and CLIENT_MAINTAIN."
main_flow:
  - "The user downloads the template, fills it and uploads it."
  - "BIBS validates each row and shows the review."
  - "The user commits; BIBS creates or updates the clients and reports codes per row."
rules:
  - [R1, "Upsert order - prospect or client code; else one hard duplicate key; else new prospect.", Fixed, "-"]
  - [R2, "A row that matches several clients on hard keys is refused.", Fixed, "-"]
validations:
  - [Row errors, Same messages as FR-NB-031, "CLIENT_TIN_FORMAT, CLIENT_DUPLICATE, LOV_VALUE_INVALID ..."]
fields_screen: "CLIENT_CREATE template columns"
fields:
  - [Type, Text, "Yes", "INDIVIDUAL or CORPORATE", "-"]
  - [Last / First / Middle name; Registered name, Text, Cond., "-", By type]
  - [TIN; ID type; ID number, Text, "No", LOV ID_TYPE, "TIN 000-000-000-000"]
  - [E-mail; Mobile; Address; City; Province; Postal code, Text, "No", "-", Formats as FR-NB-031]
  - [Market segment; Nationality; Source of funds; Risk rating, Code, "No", Respective LOVs, Active code]
  - [CIF, Text, "No", "-", "-"]
notifications:
  - "None; the job result shows the outcome."
audit:
  - "Each created or updated client is audited with the bulk job number."
acceptance:
  - "An upload of 50 rows with 5 existing client codes updates those 5 and creates 45 prospects."
  - "A row with an invalid mobile is refused and listed in the error report."
```

```fr
id: FR-NB-036
title: "Maintain client and account tags and special instructions"
brd: [BRNB.091 (p.4)]
actor: "All users with CLIENT_MAINTAIN (maintain); all users (view)"
priority: "Must have"
fit: "NEW"
screens: "Client page (Tags & Instructions tab); instructions banner on client, quotation, PRF and account pages"
api: "POST /api/v1/crm/clients/{id}/tags; POST .../tags/{code}/remove; POST | PUT .../instructions; POST .../instructions/{iid}/end; GET .../instructions"
description:
  - "Users tag a client with controlled values (list CLIENT_TAG) and record special instructions with a type, text and effective dates. The tags and instructions in force are shown as a banner on every screen of the client's records (quotations, PRFs, accounts, placement, booking), so servicing, collections and processing follow them."
  - "**Built behaviour:** tags and instructions warn only; they do not block a process. Blocking, and the final list of tags and instruction types, wait for Q18. BRD-10 Sanction Screening sets PEP and WATCHLIST_REVIEW by rule."
preconditions:
  - "The user has CLIENT_MAINTAIN to change tags and instructions."
main_flow:
  - "The user opens Tags & Instructions and clicks **Add Tag** or **New Instruction**."
  - "The user selects the tag, or the instruction type, text and dates, and saves."
  - "BIBS shows the banner on the client's records while the item is in force."
alternate_flows:
  - "The user changes an instruction or ends it (**End**); an ended instruction cannot be changed."
rules:
  - [R1, "Tags - VIP client; BDO employee; Do not call; Watchlist review; Politically exposed person.", Configurable, LOV CLIENT_TAG]
  - [R2, "Instruction types - Billing and payment; Communication preference; Document delivery; Servicing / renewal; Collection; Others.", Configurable, LOV INSTRUCTION_TYPE]
  - [R3, "History of every tag and instruction is kept (who, when, from / to).", Fixed, "-"]
validations:
  - [Tag already set, "The client is already tagged <tag>", CLIENT_TAG_EXISTS]
  - [Instruction text blank, Enter the instruction, INSTRUCTION_TEXT_REQUIRED]
  - [End before start, The end date must not be before the start date, INSTRUCTION_DATES]
  - [Change of an ended instruction, An ended instruction cannot be changed, INSTRUCTION_ENDED]
fields_screen: "Instruction dialog"
fields:
  - [Type, List, "Yes", LOV INSTRUCTION_TYPE, Active value]
  - [Instruction, Long text, "Yes", "-", "-"]
  - [Effective from / Effective to, Date, "Yes (from)", "-", To on or after from]
notifications:
  - "None; the banner is the notice."
audit:
  - "Change history table on the tab and the audit trail, with user, time and from / to values."
acceptance:
  - "A billing instruction on a client appears as a banner on each of the client's accounts."
  - "Removing a tag keeps the earlier tag in the change history."
```

```fr
id: FR-NB-037
title: "Client 360 view and linkage checks"
brd: [BRNB.099 (p.8)]
actor: "Processing; Marketing"
priority: "Must have"
fit: "NEW"
screens: "Client page (Linked Records, Quotation, Confirmed Proposals, History tabs)"
api: "GET /api/v1/crm/clients/{id}/records; GET /api/v1/crm/clients/{id}/history"
description:
  - "The client page lists every record linked to the client - quotations, PRFs, accounts with their ARN, Insurance Advices and booked invoices - with reference, description, status and date, and flags missing linkages - a confirmed client without an active party, incomplete client information, or an expired KYC. Key identifier changes (codes, names, TIN, IDs) are in the History tab."
  - "Prospects are the leads of this phase; client-level special instructions are reused by Renewal and servicing."
  - "**Built behaviour:** lead management before the prospect (for example bank referrals) is not built; it waits for Q19."
preconditions:
  - "The user has CLIENT_VIEW."
main_flow:
  - "The user opens the client and the Linked Records tab."
  - "BIBS lists the linked records and any linkage warning."
  - "The user opens a linked record from the list."
rules:
  - [R1, "Each module lists its own records for the client (quotation, PRF, account, issuance, booking).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Not applicable (read-only view)."
acceptance:
  - "A client with two accounts and one booked invoice shows all three on Linked Records."
  - "A client whose KYC has expired shows the warning \"KYC expired\"."
```

```fr
id: FR-NB-038
title: "Monthly list of clients due for KYC review"
brd: [BRNB.110 (p.14)]
actor: "Marketing; System"
priority: "Must have"
fit: "NEW"
screens: "KYC Reviews Due; Report NB-KYC-DUE"
api: "GET /api/v1/crm/kyc-reviews; report NB-KYC-DUE"
description:
  - "A monthly job (KYC_REVIEW_DUE, first day of the month) expires the KYC of clients past their review date and notifies the CLIENT_MAINTAIN holders with the number of non-bank clients due. The KYC Reviews Due screen lists the clients due by a date, filtered by non-bank / bank / all, risk rating and market segment, and downloads or prints the list (Excel, PDF)."
  - "**Built behaviour:** the BRD's 'shared path' is replaced by the in-system list and the report, which the user downloads to any folder."
preconditions:
  - "The user has CLIENT_VIEW."
main_flow:
  - "The job runs and notifies the users."
  - "The user opens KYC Reviews Due and filters the list."
  - "The user downloads or prints it, and records each review on the client (FR-NB-034, periodic review)."
rules:
  - [R1, "Listed - clients whose review date falls within 30 days (default) or has passed.", Configurable, Parameter KYC_DUE_WINDOW_DAYS]
  - [R2, "Job schedule - 1st of the month, 10:00 PHT (default).", Configurable, Job KYC_REVIEW_DUE]
validations: []
fields_screen: "NB-KYC-DUE columns"
fields:
  - [Client Code / Client / Type / Segment, Text, "-", Client master, "-"]
  - [Bank Client / Risk Rating, Text, "-", Client master, "-"]
  - [Last Verified / Review Due, Date, "-", KYC profile, "-"]
  - [KYC, Text, "-", KYC status, "-"]
notifications:
  - "CLIENT_MAINTAIN holders when the monthly list is ready."
audit:
  - "Job runs and report exports are logged."
acceptance:
  - "On the 1st of the month, the AO receives a notification with the count of non-bank clients due."
  - "The list filtered on high risk shows only high-risk clients and exports to Excel with the same rows."
```

## Quotation

```fr
id: FR-NB-040
title: "Receive quotation and proposal requests"
brd: [BRNB.041 (p.94), BRNB.023 (p.61)]
actor: "Marketing AO; System"
priority: "Must have"
fit: "NEW"
screens: "Quotation Requests; Capture Quotation Request dialog; Bulk Upload (Quotation requests)"
api: "GET | POST /api/v1/quotation-requests; POST .../{id}/prospect; POST .../{id}/close"
description:
  - "Every request is logged in the Quotation Requests inbox with a number REQ-yyyy-nnnnnn, its channel (list SOURCE_CHANNEL), source reference, client or prospect, requested product, segment and cover. A request received by e-mail is captured by the AO with the e-mail attached (document type REQUEST_EMAIL). Requests from a source system are loaded as a file (upload QUOTATION_REQUEST) or through the source-system intake port."
  - "From the inbox the AO creates the prospect when the client is new, creates the quotation (the request becomes QUOTED) or closes the request with a reason."
  - "**Built behaviour:** HLS requests (BRNB.023) are loaded by upload of the HLS extract. The direct HLS interface, with its source signature check, waits for Q11; the intake port and the staging of requests are built. Reading a shared mailbox automatically waits for Q12."
preconditions:
  - "The user has QUOTE_MAINTAIN (capture) or QUOTE_VIEW (view)."
main_flow:
  - "The AO clicks **Capture Request**, enters the request details and attaches the e-mail."
  - "BIBS numbers the request (NEW) and lists it under To Quote."
  - "The AO clicks **Create Prospect** for a new client, then **Create Quotation**; the quotation wizard opens pre-filled."
alternate_flows:
  - "Same source reference of the same channel received again. BIBS refuses the duplicate."
  - "The AO closes a request that will not be quoted, with a reason."
rules:
  - [R1, "Request numbers REQ-<yyyy>-nnnnnn.", Configurable, Parameter QUOTATION_REQUEST_PREFIX]
  - [R2, "A source reference is unique per channel.", Fixed, "-"]
  - [R3, "Channels - E-mail; Home Loan System (HLS); Bulk upload; Branch referral; Walk-in / direct.", Configurable, LOV SOURCE_CHANNEL]
  - [R4, "Every request keeps its channel, so the source (for example HLS) is traceable.", Fixed, "-"]
validations:
  - [No client and no prospect name, Give the client code or the prospect's name, QUOTATION_REQUEST_CLIENT]
  - [Duplicate source reference, "Request <reference> of <channel> was already received", QUOTATION_REQUEST_DUPLICATE]
  - [Create prospect for a request that has a client, "Request <number> has a client", QUOTATION_REQUEST_HAS_CLIENT]
  - [Close without reason, Give the reason for closing, QUOTATION_REQUEST_REASON]
  - [Action on a closed request, "Request <number> is already <status>", QUOTATION_REQUEST_CLOSED]
fields_screen: "Capture Quotation Request"
fields:
  - [Channel, List, "Yes", LOV SOURCE_CHANNEL, "-"]
  - [Source reference, Text, "No", "-", Unique per channel]
  - [Existing client, Look-up, Cond., Client master, Or the prospect name]
  - [Prospect name / e-mail / mobile, Text, Cond., "-", Name when no client]
  - [Requested product, List, "No", Products, Active]
  - [Market segment, List, "No", LOV MARKET_SEGMENT, "-"]
  - [Requested cover, Text, "Yes", "-", "-"]
  - [Request e-mail, Attachment, "No", Document type REQUEST_EMAIL, msg / eml / pdf]
notifications:
  - "None; the request is in the inbox of Marketing."
audit:
  - "Capture, prospect creation, quotation and closure are audited; the intake job logs each source call."
acceptance:
  - "An e-mailed request captured with the e-mail attached gets a REQ number and appears under To Quote."
  - "A request file loaded from the HLS extract creates one request per row with channel HLS."
  - "The same HLS reference loaded twice is refused the second time."
```

```fr
id: FR-NB-041
title: "Create an individual quotation with its premium"
brd: [BRNB.043 (p.95-96), BRNB.029 (p.68-69), BRNB.102 (p.10), BRNB.004 (p.18)]
actor: "Marketing AO"
priority: "Must have"
fit: "CHANGE"
screens: "New Quotation (wizard); Quotation page"
api: "POST /api/v1/quotations; POST /api/v1/quotations/preview; PUT /api/v1/quotations/{id}"
description:
  - "The AO creates a quotation (for example IDF, FFYMI) in a wizard - client or prospect, product, risk items, premium, review. A client or prospect code is enough (BRNB.029); a quotation of a prospect with incomplete information is flagged. BIBS rates the risk with the Appendix A formulas (FR-NB-062) and shows the live premium breakdown."
  - "On first save the quotation gets its number (QT-yyyy-nnnnnn, shown as Proposal No.), its ARN (BRNB.102), version 1 and its work case (DRAFT). The TSU routing result is shown (FR-NB-005). For a package product, the quotation uses the package version in force (BRD-3); a non-standard item rate is asked through **Request Rate Exception** (BRD-3 FRS FR-PM-051)."
  - "Items carry a risk group; each accepted risk group later becomes one account (FR-NB-045)."
preconditions:
  - "The user has QUOTE_MAINTAIN; the client or prospect is active."
main_flow:
  - "The AO clicks **New Quotation** (or Create Quotation from a request or a client)."
  - "The AO selects the client or prospect, the segment, channel, product, insurer and branch, period, validity and rating basis."
  - "The AO adds the risk items with sums insured and risk groups; BIBS shows the premium."
  - "The AO clicks **Save Draft**; BIBS assigns the numbers and the ARN."
  - "The AO clicks **Submit for Review** (FR-NB-043)."
alternate_flows:
  - "Premium cannot be computed (sum insured or rate missing). Submission is refused."
  - "Validity date in the past. Submission is refused until it is changed."
rules:
  - [R1, "Quotation numbers QT-<yyyy>-nnnnnn; ARN ARN-<yyyy>-nnnnnn.", Configurable, Parameter QUOTATION_NUMBER_PREFIX (ARN format fixed)]
  - [R2, "Default validity 30 days from creation.", Configurable, Parameter QUOTATION_VALIDITY_DAYS]
  - [R3, "Submission needs at least one item, a rated premium and a validity that has not passed.", Fixed, "-"]
  - [R4, "The product of a quotation cannot change after the first save.", Fixed, "-"]
  - [R5, "The intake template version is stamped on the quotation.", Fixed, "-"]
validations:
  - [Client or product missing, Select the client and the product, QUOTATION_INCOMPLETE]
  - [Product not offered to segment, "Product <code> is not offered to segment <segment>", SEGMENT_NOT_ALLOWED]
  - [Direct payment not allowed, "Product <code> cannot be paid directly to the insurer", DIRECT_PAYMENT_NOT_ELIGIBLE]
  - [Period end not after start, The period end must be after the period start, QUOTATION_PERIOD_INVALID]
  - [Validity in the past, The validity date cannot be in the past, QUOTATION_VALIDITY_PAST]
  - [No items, Add at least one risk item, QUOTATION_NO_ITEMS]
  - [Premium not computable, "The premium cannot be computed: give each item a sum insured (and a rate when the product has no default rate)", QUOTATION_NOT_RATED]
  - [Validity passed at submission, "The validity date has passed: change it before submitting", QUOTATION_EXPIRED]
  - [Product changed, The product of a quotation cannot change, QUOTATION_PRODUCT_FIXED]
fields_screen: "New Quotation"
fields:
  - [Client or prospect, Look-up, "Yes", Client master, Active client or prospect]
  - [Market segment / Source channel, List, "Yes / No", "LOV MARKET_SEGMENT, SOURCE_CHANNEL", Segment allowed for the product]
  - [Product, List, "Yes", Products, Usable; fixed after save]
  - [Insurer / Insurer branch (LGT), List, "No", Insurer panel, Active insurer and branch]
  - [Period from / Period to, Date, "Yes", "-", To after from]
  - [Valid until, Date, "Yes", "-", "Default today + 30 days; not in the past"]
  - [Rating basis, Option, "Yes", "Annual / Pro-rata (days) / Short period (table)", "-"]
  - [Risk items, Table, "Yes", Item fields of the line (FR-NB-002), "Sum insured >= 0; risk group"]
  - [Remarks, Text, "No", "-", "-"]
notifications:
  - "None until submission."
audit:
  - "Creation and each save audited; the ARN assignment is logged."
acceptance:
  - "A quotation for a prospect is saved and shows the incomplete-client flag."
  - "A saved quotation has a QT number, an ARN and version 1."
  - "A quotation with an item without sum insured cannot be submitted (QUOTATION_NOT_RATED)."
```

```fr
id: FR-NB-042
title: "Edit a quotation before approval and keep its versions"
brd: [BRNB.020 (p.58-59)]
actor: "Marketing AO"
priority: "Must have"
fit: "CHANGE"
screens: "Edit Quotation; Quotation page (Versions tab, Compare versions)"
api: "PUT /api/v1/quotations/{id}; POST .../revise; GET .../versions; GET .../diff?from=&to="
description:
  - "A quotation in DRAFT is edited in place. At submission its version is frozen. To change an APPROVED or SENT_TO_CLIENT quotation, the AO clicks **Revise (new version)**; the quotation returns to DRAFT and the next change opens version n+1, which is validated and submitted for approval again."
  - "The Versions tab lists every version with its state, gross premium and submission; Compare versions shows changed terms, items added, removed or changed, and the premium difference."
preconditions:
  - "The quotation is in DRAFT (edit) or APPROVED / SENT_TO_CLIENT (revise)."
main_flow:
  - "The AO opens the quotation and clicks **Edit** (draft) or **Revise** (approved or sent)."
  - "The AO changes the fields; BIBS validates the mandatory fields on save."
  - "The AO submits the new version for review."
alternate_flows:
  - "Quotation in FOR_REVIEW, ACCEPTED or closed. Editing is refused."
rules:
  - [R1, "A submitted version is read-only.", Fixed, "-"]
  - [R2, "Revise is allowed from APPROVED and SENT_TO_CLIENT.", Fixed, "-"]
validations:
  - [Edit in a locked stage, "Quotation <number> is <status>", QUOTATION_NOT_EDITABLE]
  - [Change of a submitted version, "Version <n> was submitted and is read-only", QUOTATION_VERSION_FROZEN]
notifications:
  - "The approvers when the new version is submitted."
audit:
  - "Each version with user and time; the revise action in the history."
acceptance:
  - "Revising a sent quotation creates version 2; version 1 stays readable."
  - "Compare versions shows the changed sum insured and the premium difference."
```

```fr
id: FR-NB-043
title: "Approve or return a quotation"
brd: [BRNB.021 (p.59-60), BRNB.014 (p.56)]
actor: "Approver (Marketing TL / TH / UH, NB Approver)"
priority: "Must have"
fit: "FIT"
screens: "Quotations (For Review tab); Quotation page"
api: "POST /api/v1/quotations/{id}/approve; workflow action return"
description: "The approver views the full details and the version history of a quotation in FOR_REVIEW and approves it or returns it to the maker with a reason and comment. An approved quotation is locked and can be sent to the client (FR-NB-044). The approver is never the maker or the submitter."
preconditions:
  - "The quotation is in FOR_REVIEW; the user has QUOTE_APPROVE."
main_flow:
  - "The approver opens the quotation from the For Review tab."
  - "The approver reviews the details, premium and versions."
  - "The approver clicks **Approve** with an optional comment; the quotation becomes APPROVED."
alternate_flows:
  - "Return. The approver clicks **Return to maker** with a reason (RETURN_REASON); the quotation goes back to DRAFT."
  - "Void. The maker or approver voids it (FR-NB-012)."
rules:
  - [R1, "Approver is not the maker or submitter.", Fixed, "-"]
  - [R2, "SLA of FOR_REVIEW 8 hours (default).", Configurable, Workflow stage definitions]
  - [R3, "One approval stage; a multi-level chain waits for Q05.", Configurable, Permission QUOTE_APPROVE]
validations:
  - [Approver is the maker, A quotation is approved by someone other than its maker, QUOTATION_FOUR_EYES]
  - [Return without reason, "Select a reason for 'Return to maker'", WORKFLOW_REASON_REQUIRED]
notifications:
  - "The maker on approval or return."
audit:
  - "Approval and return with user, time, comment and reason."
acceptance:
  - "The maker cannot approve the quotation."
  - "An approved quotation cannot be edited except by Revise."
  - "A returned quotation is in DRAFT with the reason visible."
```

```fr
id: FR-NB-044
title: "Send quotations to clients, singly or in a batch"
brd: [BRNB.043 (p.95-96), BRNB.042 (p.94-95), BRNB.013 (p.55)]
actor: "Marketing AO"
priority: "Must have"
fit: "CHANGE"
screens: "Quotation page (Send); Quotations (Send via Email batch action)"
api: "POST /api/v1/quotations/{id}/send; POST /api/v1/quotations/batch-send; GET .../document.pdf | document.xlsx"
description:
  - "An APPROVED quotation is sent to the client by e-mail with the quotation PDF (templates QUOTATION_LETTER and QUOTATION_TERMS) and the Excel schedule, both password protected, and the password in a separate e-mail (FR-NB-013). The quotation becomes SENT_TO_CLIENT."
  - "In the Quotations list the AO selects several approved quotations and clicks **Send via Email**. BIBS sends one e-mail per client with all of that client's quotations, and one password e-mail."
  - "The AO downloads a quotation as PDF or Excel at any time; the browser saves it to the folder the user chooses."
preconditions:
  - "The quotation is APPROVED; the user has QUOTE_MAINTAIN."
main_flow:
  - "The AO clicks **Send** (or selects quotations and clicks **Send via Email**)."
  - "BIBS generates and protects the documents and queues the e-mails."
  - "The quotations move to SENT_TO_CLIENT."
alternate_flows:
  - "A selected quotation is not approved. The batch is refused and the problems are listed."
rules:
  - [R1, "Only APPROVED quotations are sent.", Fixed, "-"]
  - [R2, "Quotations whose validity ends within 7 days are listed as Expiring.", Configurable, Parameter QUOTATION_EXPIRING_DAYS]
validations:
  - [Nothing selected, Select the quotations to send, QUOTATION_BATCH_EMPTY]
  - [Batch with problems, "Cannot send: <problems>", QUOTATION_BATCH_INVALID]
  - [Not approved, "Quotation <number> must be approved before it is sent", QUOTATION_NOT_APPROVED]
fields_screen: "Send via Email (batch)"
fields:
  - [Password hint for the clients, Text, "No", "-", Printed in the e-mail]
notifications:
  - "The client receives the protected quotation and the password separately."
audit:
  - "Each e-mail logged with recipients, time and attachment hashes."
acceptance:
  - "Three approved quotations of two clients selected together produce two e-mails and two password e-mails."
  - "A draft quotation in the selection stops the batch with QUOTATION_BATCH_INVALID."
```

```fr
id: FR-NB-045
title: "Record client acceptance and create the accounts"
brd: [BRNB.045 (p.97), BRNB.024 (p.62-63), BRNB.044 (p.96-97)]
actor: "Marketing AO"
priority: "Must have"
fit: "NEW"
screens: "Quotation page (Record Acceptance, Create Accounts); Bulk Quotation Acceptance"
api: "POST /api/v1/quotations/{id}/accept; POST .../create-accounts; POST /api/v1/bulk/jobs (QUOTATION_ACCEPTANCE)"
description:
  - "When the client accepts by e-mail, the AO attaches the acceptance e-mail (document type CLIENT_ACCEPTANCE), records the acceptance with the accepted risk groups and a comment (ACCEPTED), and creates the accounts. BIBS creates one draft account per accepted risk group with the premium of the group, the insurer, the payment arrangement and the quotation's creator as account officer (CONVERTED). A quotation with one account passes its own ARN; with several, the accounts carry ARN-yyyy-nnnnnn-01, -02 and so on."
  - "Accepted quotations are received in bulk with the upload QUOTATION_ACCEPTANCE (ARN, risk groups, remarks), which can create the accounts at once; the uploaded list is kept as the acceptance evidence. This is also how Marketing builds accounts for bulk processing (BRNB.044)."
  - "A client who declines is recorded with **Client declined** (NOT_PROCEEDED)."
preconditions:
  - "The quotation is SENT_TO_CLIENT (accept) or ACCEPTED (create accounts); account creation needs a confirmed client (FR-NB-034)."
main_flow:
  - "The AO uploads the acceptance e-mail."
  - "The AO clicks **Record Acceptance**, ticks the accepted risk groups and saves."
  - "The AO clicks **Create Accounts**; BIBS creates the draft accounts and links them to the quotation."
alternate_flows:
  - "Client not yet confirmed. Account creation is refused until onboarding is complete."
  - "Bulk acceptance with the parameter createAccounts creates the accounts in the same job."
rules:
  - [R1, "One account per accepted risk group.", Fixed, "-"]
  - [R2, "Accounts carry the quotation's ARN (with -nn suffix when several).", Fixed, "-"]
validations:
  - [No acceptance e-mail, "Attach the client's acceptance e-mail (document type Client acceptance e-mail) first", ACCEPTANCE_EMAIL_REQUIRED]
  - [Unknown risk group, "The quotation offers risk groups <groups> only", ACCEPTANCE_GROUP_UNKNOWN]
  - [Create accounts before acceptance, "Record the client's acceptance first", QUOTATION_NOT_ACCEPTED]
  - [Client not confirmed, "Client <code> is not yet confirmed (onboarding and KYC incomplete)", CLIENT_NOT_CONFIRMED]
  - [Bulk risk groups badly written, "Risk groups are numbers separated by ';'", ACCEPTANCE_GROUP_INVALID]
fields_screen: "Record Acceptance"
fields:
  - [Accepted risk groups, Check boxes, "Yes", Risk groups of the quotation, At least one]
  - [Comment, Text, "No", "-", "-"]
notifications:
  - "None; the accounts appear in the AO's drafts."
audit:
  - "Acceptance with the groups, the evidence document and the created ARNs."
acceptance:
  - "A quotation with risk groups 1 and 2, of which the client accepts group 1, creates one account with the quotation's ARN."
  - "Accepting without the acceptance e-mail is refused."
  - "A bulk acceptance of 20 ARNs with createAccounts creates the accounts of the confirmed clients and lists the others as failed."
```

```fr
id: FR-NB-046
title: "Create quotations in bulk"
brd: [BRNB.028 (p.67-68), BRNB.063 (p.109-110), BRNB.042 (p.94-95)]
actor: "Marketing; System"
priority: "Must have"
fit: "NEW"
screens: "Bulk Quotations (/bulk/QUOTATION_CREATE); Quotations"
api: "POST /api/v1/bulk/jobs (QUOTATION_CREATE)"
description:
  - "Marketing uploads a list of request details (or the extract of a source system) with the parameters product and default market segment. Each valid row becomes one quotation with one risk item, rated, numbered and given an ARN. The client is found by code, or by name and birth date, or created as a prospect, so a quotation can be created without an existing client code (BRNB.063). The job reports each quotation number or the reason of failure; duplicate rows of the same file are refused."
  - "The quotations then follow the normal approval and are sent singly or in a batch (FR-NB-044)."
preconditions:
  - "The user has BULK_PROCESS and QUOTE_MAINTAIN."
main_flow:
  - "The user selects the product and segment and uploads the file."
  - "BIBS validates each row (client, item fields of the line, sum insured) and shows the review."
  - "The user commits; BIBS creates the quotations and lists their numbers."
rules:
  - [R1, "One quotation per row; the product is a parameter of the upload.", Fixed, "-"]
  - [R2, "A new name without a code creates a prospect (conflict BRNB.029 / 063 settled this way, Q13).", Fixed, "-"]
validations:
  - [Product not chosen, Choose the product of the upload, BULK_PRODUCT_REQUIRED]
  - [No client code and no name, Give the client code or the prospect's name, QUOTATION_CLIENT_REQUIRED]
  - [Row errors, Messages of FR-NB-041, "QUOTATION_NOT_RATED ..."]
fields_screen: "QUOTATION_CREATE template columns"
fields:
  - [Client code / Prospect name / E-mail / Mobile, Text, Cond., Client master, Code or name]
  - [Market segment; Insurer; Insurer branch, Code, "No", Lists, Default from the screen]
  - [Sum insured; Rate, Number, "Yes (sum)", "-", ">= 0"]
  - [Description (other lines), Text, Cond., "-", "-"]
  - [Plate; Engine; Chassis; Make; Model (Motor), Text, Cond., "-", Motor field rules]
  - [Address; City; Occupancy; Construction (Fire), Text, Cond., LOV OCCUPANCY, Property field rules]
  - [Insured person (Personal Accident), Text, Cond., "-", "-"]
notifications:
  - "None; the job result shows the outcome."
audit:
  - "Each quotation audited with the bulk job number."
acceptance:
  - "An upload of 30 HLS requests for product PAR01 creates 30 quotations, each with its own ARN."
  - "A row with a new name creates a prospect and a quotation for it."
```

```fr
id: FR-NB-047
title: "One Account Reference Number from quotation to invoice"
brd: [BRNB.102 (p.10), BRNB.006 (p.51-52)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "ARN chip on every quotation, PRF, account, slip, advice and invoice page; Account by ARN"
api: "GET /api/v1/accounts/by-arn/{arn}; GET /api/v1/quotations/by-arn/{arn}"
description:
  - "BIBS generates one ARN (ARN-yyyy-nnnnnn) at quotation or PRF creation, or at direct account creation, and carries it unchanged on the account, placement slip, hold cover, e-policy, Insurance Advice, invoice, service invoice, reports and interfaces. Several accounts from one quotation carry the ARN with a two-digit suffix. Users search and open any record by its ARN."
  - "The ARN replaces the QPS reference number (OOS-2). The PRF also keeps its marketing reference (FR-NB-050)."
preconditions:
  - "None."
main_flow:
  - "A quotation, PRF or direct account is created; BIBS assigns the next ARN from the yearly sequence."
  - "Every downstream record stores the ARN."
  - "A user enters an ARN in any search to find the record."
rules:
  - [R1, "ARN format ARN-<yyyy>-nnnnnn, or ARN-<yyyy>-nnnnnn-nn for split accounts; gap-free, never re-used, not editable.", Fixed, "-"]
  - [R2, "Format of the BDOI number waits for Q15; the ARN prefix is the build default.", Configurable, Numbering (Q15)]
validations:
  - [Invalid ARN given, "The ARN must read ARN-yyyy-nnnnnn (or ARN-yyyy-nnnnnn-nn)", ARN_INVALID]
  - [ARN already used, "An account already has ARN <arn>", ARN_IN_USE]
notifications:
  - "None."
audit:
  - "Number assignment logged."
acceptance:
  - "The ARN of a quotation appears unchanged on its account, placement slip and booked invoice."
  - "The Booked Accounts Register and the Placement Update Report can be filtered by that ARN."
```

## Non-package placement (PRF)

```fr
id: FR-NB-050
title: Create and submit a Proposal Request Form
brd: [BRNB.005 (p.50-51), BRNB.006 (p.51-52)]
actor: Marketing AO
priority: Must have
fit: NEW
screens: Proposal Requests; New Proposal Request; Edit Proposal Request
api: POST /api/v1/proposals; PUT /api/v1/proposals/{id}; POST .../{id}/submit; GET .../{id}/checklist
description:
  - For a risk that the TSU must price with insurers, the AO fills in a PRF - client or prospect, product line and risk code, market segment, period, risk details in free sections (heading and details), risk items with sums insured and risk groups, and the insurers requested - and attaches the documents of the product's checklist.
  - On first save BIBS assigns the marketing reference PRF-yyyy-nnnnnn from a gap-free yearly sequence, and an ARN. The number is shown on the PRF and in every list; it cannot be edited. The PRF is tracked in real time through the workflow NB_PROPOSAL (section 5.3).
  - Submission checks the header, the risk description, the mandatory documents and the TSU routing, and sends the PRF for Marketing approval (FR-NB-051).
preconditions:
  - The user has PROPOSAL_REQUEST.
main_flow:
  - The AO clicks **New Proposal Request** and completes the client, product and period.
  - The AO adds risk detail sections and risk items, selects the insurers requested and uploads the documents; the checklist shows Attached and Missing.
  - The AO clicks **Save Draft**; BIBS assigns the PRF number and the ARN.
  - The AO clicks **Submit for Approval**; the PRF moves to FOR_MKT_APPROVAL.
alternate_flows:
  - A required document or the risk description is missing. Submission is refused and names what is missing.
  - A package risk that no TSU rule sends to TSU. Submission is refused; the AO makes a package quotation instead.
rules:
  - [R1, "Marketing reference PRF-<yyyy>-nnnnnn; gap-free per year; one number per PRF; no manual entry.", Configurable, Parameter PROPOSAL_NUMBER_PREFIX (format Q15)]
  - [R2, "A PRF needs the TSU routing to require TSU - every non-package risk does (rule NON_PACKAGE).", Configurable, TSU rules (FR-NB-005)]
  - [R3, "Mandatory documents come from the product's document rules.", Configurable, Document rules]
  - [R4, "The product of a PRF cannot change after the first save.", Fixed, "-"]
  - [R5, "Insurers have no access to BIBS; they are reached only by e-mail.", Fixed, "-"]
validations:
  - [Client or product missing, Complete the PRF header, PRF_INCOMPLETE]
  - [No risk description, Describe the risk (sections or risk items) before submitting, PRF_INCOMPLETE]
  - [Documents missing, "Attach the mandatory documents: <types>", MISSING_DOCUMENTS]
  - [TSU not needed, "No TSU rule applies to this package risk: quote it directly as a package quotation", PRF_NOT_NEEDED]
  - [Segment not allowed, "Product <code> is not offered to segment <segment>", SEGMENT_NOT_ALLOWED]
  - [Period end not after start, The period end must be after the period start, PRF_PERIOD_INVALID]
  - [Product changed, The product of a PRF cannot change, PRF_PRODUCT_FIXED]
  - [Edit in a locked stage, "PRF <number> cannot be changed while <stage>", PRF_NOT_EDITABLE]
fields_screen: New Proposal Request
fields:
  - [Client or prospect, Look-up, "Yes", Client master, Active]
  - [Product line and risk code, List, "Yes", Products, Usable; fixed after save]
  - [Market segment, List, "Yes", LOV MARKET_SEGMENT, Allowed for the product]
  - [Period from / Period to, Date, "Yes", "-", To after from]
  - [Risk details, Sections, Cond., Heading and details, Sections or items required]
  - [Risk items, Table, Cond., Item fields of the line, "Sum insured, risk group"]
  - [Insurers requested, Multi-select, "No", Insurer panel, Active insurers]
  - [Documents, Attachment, Cond., Product document checklist, Mandatory types]
notifications:
  - The PROPOSAL_APPROVE holders on submission.
audit:
  - Number assignment, each save and the submission are audited.
acceptance:
  - A new PRF gets the next PRF number of the year and an ARN; no two PRFs share a number.
  - A PRF without its mandatory documents cannot be submitted.
  - The PRF number cannot be edited on any screen.
```

```fr
id: FR-NB-051
title: Approve a PRF (Marketing)
brd: [BRNB.005 (p.50-51), BRNB.014 (p.56)]
actor: Marketing TL / TH / UH
priority: Must have
fit: NEW
screens: Proposal Requests (For Approval tab); Proposal Request page
api: POST /api/v1/proposals/{id}/approve; workflow action return
description:
  - A Marketing approver reviews the submitted PRF and approves it, which sends it to the TSU (WITH_TSU), or returns it to the AO with a reason (DRAFT). The approver is never the maker or submitter.
  - "**Built behaviour:** the BRD's chain TL, then TH, then UH is one approval stage in the build; any holder of PROPOSAL_APPROVE approves. The chain waits for Q05."
preconditions:
  - The PRF is in FOR_MKT_APPROVAL; the user has PROPOSAL_APPROVE.
main_flow:
  - The approver opens the PRF and reviews it.
  - The approver clicks **Approve and send to TSU**.
  - BIBS moves the PRF to WITH_TSU and notifies the TSU queue.
alternate_flows:
  - Return to Account Officer with a reason (RETURN_REASON).
  - Void by the maker or approver (FR-NB-012).
rules:
  - [R1, "Approver is not the maker or submitter.", Fixed, "-"]
  - [R2, "SLA of FOR_MKT_APPROVAL 8 hours (default).", Configurable, Workflow stage definitions]
validations:
  - [Approver is the maker, A PRF is approved by someone other than its maker, PRF_FOUR_EYES]
  - [Return without reason, "Select a reason for 'Return to Account Officer'", WORKFLOW_REASON_REQUIRED]
notifications:
  - TSU_PROCESS holders on approval; the AO on return.
audit:
  - Approval and return with user, time, reason and comment.
acceptance:
  - The AO who submitted the PRF cannot approve it.
  - An approved PRF appears in the TSU Workbench.
```

```fr
id: FR-NB-052
title: Receive, amend, return or delete PRFs (TSU)
brd: [BRNB.007 (p.52-53)]
actor: TSU
priority: Must have
fit: NEW
screens: TSU Workbench; Proposal Request page
api: GET /api/v1/proposals; PUT /api/v1/proposals/{id}; POST /api/v1/workflow/cases/{id}/actions/{prepare_qs | return | void}
description: The TSU Workbench shows the TSU queue as tiles by stage (received, quotation slip in preparation, for approval, awaiting insurer terms, terms received, proposal slip for approval) and a list filtered by type and status. TSU opens a PRF, accepts it for quotation slip preparation, amends it, returns it to Marketing with a reason, or voids ("deletes") it while it is in process. A returned PRF shows the reason; the AO corrects it and resubmits. The AO sees every return in the PRF list and history.
preconditions:
  - The user has TSU_PROCESS.
main_flow:
  - TSU opens the TSU Workbench and a PRF in WITH_TSU.
  - TSU reviews and, where needed, updates the PRF.
  - TSU clicks **Accept and prepare quotation slip** (QS_PREPARATION).
alternate_flows:
  - Return to Marketing with a reason (DRAFT); the AO updates and resubmits.
  - Void with a reason while in WITH_TSU.
rules:
  - [R1, "Marketing edits a PRF in DRAFT; TSU edits it in WITH_TSU or QS_PREPARATION.", Fixed, "-"]
  - [R2, "SLA of WITH_TSU 8 hours; QS_PREPARATION 24 hours (defaults).", Configurable, Workflow stage definitions]
  - [R3, "Return reasons - Incomplete or incorrect details; Missing supporting documents; Declined by insurer; Additional insurer requirements; Rate or terms to be reviewed; Insurer underwriting review; Others.", Configurable, LOV RETURN_REASON]
validations:
  - [Edit in a locked stage, "PRF <number> cannot be changed while <stage>", PRF_NOT_EDITABLE]
  - [Return without reason, "Select a reason for 'Return to Marketing'", WORKFLOW_REASON_REQUIRED]
notifications:
  - TSU on receipt (stage entry); the AO on return; the AO when the PRF is changed by TSU.
audit:
  - Accept, update, return and void with user, time and reason.
acceptance:
  - A PRF approved by Marketing appears in the TSU Workbench within the Received tile.
  - A PRF returned by TSU is back in the AO's drafts with the reason.
  - TSU can filter the queue by stage and search by PRF number.
```

```fr
id: FR-NB-053
title: Prepare, approve and send the Quotation Slip
brd: [BRNB.008 (p.53-54)]
actor: TSU officer (prepare); second TSU officer (approve)
priority: Must have
fit: NEW
screens: Proposal Request page (Quotation Slip tab, Routing and insurers)
api: PUT /api/v1/proposals/{id}/insurers; POST .../quotation-slip/submit; POST .../quotation-slip/approve; GET .../quotation-slip.pdf
description:
  - The Quotation Slip is generated from the validated PRF data (risk details, items, sums insured, period) with the QUOTATION_SLIP template. TSU selects the panel insurers and can edit the slip content before submission. Submission numbers the slip QS-yyyy-nnnnnn, stamps the template version and sets the reply date. A second TSU officer reviews and approves it; BIBS then e-mails the protected slip to each insurer's placement address and opens a pending response per insurer.
  - The E-mails tab shows each send with its status (sent or failed); a failed e-mail is resent from there.
preconditions:
  - The PRF is in QS_PREPARATION (submit) or QS_FOR_APPROVAL (approve).
main_flow:
  - TSU selects the insurers and clicks **Save Selection**.
  - TSU reviews the slip PDF and clicks **Submit quotation slip** (QS_FOR_APPROVAL).
  - The approver clicks **Approve and send to insurers** (QS_SENT).
  - BIBS sends one protected e-mail per insurer and shows the send status.
alternate_flows:
  - Return for correction with a reason (QS_PREPARATION).
  - Insurer without a placement e-mail. Approval is refused until the insurer master has one.
rules:
  - [R1, "Slip numbers QS-<yyyy>-nnnnnn.", Configurable, Parameter QUOTATION_SLIP_PREFIX]
  - [R2, "Reply date = send date + 5 days (default).", Configurable, Parameter QUOTATION_SLIP_REPLY_DAYS]
  - [R3, "The approver is not the preparer.", Fixed, "-"]
  - [R4, "Insurers are reached by e-mail only (portal / API Q06).", Fixed, "-"]
validations:
  - [No insurer, Select at least one insurer of the panel for the quotation slip, QS_NO_INSURER]
  - [Insurer change after submission, Insurers are selected before the quotation slip is submitted, QS_CLOSED]
  - [Approver is the preparer, The quotation slip is approved by another TSU officer, QS_FOUR_EYES]
  - [Insurer without e-mail, "<insurer> has no placement e-mail address", INSURER_NO_EMAIL]
  - [Approve before preparation, No quotation slip has been prepared yet, QS_NOT_PREPARED]
fields_screen: Quotation Slip tab
fields:
  - [Insurers, Multi-select, "Yes", Insurer panel, Active insurers]
  - [Insurers reply by, Date, "No", "-", Default send date + 5 days]
  - [Comment, Text, "No", "-", Printed on the slip]
notifications:
  - TSU_APPROVE holders on submission; the preparer on approval or return.
audit:
  - Submit, approve and each e-mail with recipient, time, subject, body and attachment hash.
acceptance:
  - The preparer cannot approve their own slip.
  - An approved slip reaches each selected insurer as a protected PDF and the E-mails tab lists each send.
  - A failed send can be resent from the E-mails tab.
```

```fr
id: FR-NB-054
title: Key in insurer terms
brd: [BRNB.009 (p.54)]
actor: TSU
priority: Must have
fit: NEW
screens: Proposal Request page (Insurer Responses tab)
api: PUT /api/v1/proposals/{id}/responses/{rid}; POST .../responses/{rid}/document; POST .../responses/{rid}/recommend; GET .../responses/history; POST .../terms-complete
description:
  - For each insurer approached, TSU records the reply - terms received or declined to quote - with premium, rate, deductibles, conditions, validity and remarks, and attaches the insurer's document. Authorised users edit a response; each change raises the revision and keeps the previous values in the version history. TSU flags the recommended insurer.
  - When the terms are complete, TSU clicks **Insurer terms complete** (TERMS_RECEIVED). Responses still pending must be closed explicitly.
preconditions:
  - The quotation slip has been sent (QS_SENT).
main_flow:
  - TSU opens the response of an insurer and clicks **Terms** (or Declined).
  - TSU enters the terms, attaches the document and clicks **Save Terms**.
  - TSU flags the recommended insurer with **Recommend**.
  - TSU clicks **Insurer terms complete**.
alternate_flows:
  - Correction of saved terms. The history shows both revisions.
  - Some insurers have not answered. TSU records them as declined or closes them before completing.
rules:
  - [R1, "Response statuses - PENDING, RECEIVED (terms received), DECLINED (declined to quote).", Fixed, "-"]
  - [R2, "Received terms need a premium.", Fixed, "-"]
  - [R3, "Only received terms can be recommended.", Fixed, "-"]
  - [R4, "Terms complete needs at least one RECEIVED response and no PENDING one.", Fixed, "-"]
validations:
  - [Terms without premium, "Enter the premium quoted by <insurer>", RESPONSE_PREMIUM_REQUIRED]
  - [Recommend a response without terms, Only received terms can be recommended, RESPONSE_NOT_RECEIVED]
  - [Keying in before the slip is sent, Insurer terms are keyed in after the quotation slip is sent, RESPONSES_CLOSED]
  - [Complete with no terms, No insurer terms have been received, TERMS_NONE]
  - [Complete with pending responses, "<n> insurer(s) have not answered: close the request to proceed", TERMS_PENDING]
fields_screen: Insurer response
fields:
  - [Response, Option, "Yes", "Terms received / Declined to quote", "-"]
  - [Premium, Amount, Cond., "-", "Required for terms; >= 0"]
  - [Rate %, Number, "No", "-", ">= 0"]
  - [Valid until, Date, "No", "-", "-"]
  - [Deductibles / Conditions / Remarks, Long text, "No", "-", "-"]
  - [Response document, Attachment, "No", Document type INSURER_RESPONSE, File rules]
notifications:
  - None; the stage updates on the TSU Workbench.
audit:
  - Each revision with previous values, user and time in the version history.
acceptance:
  - TSU records terms for two insurers and a decline for a third; the comparative table shows the two offers.
  - A change of premium on a saved response keeps the earlier premium in the history.
```

```fr
id: FR-NB-055
title: Compile the comparative table
brd: [BRNB.010 (p.54-55), BRNB.009 (p.54)]
actor: System; TSU; Marketing (view)
priority: Must have
fit: NEW
screens: Proposal Request page (Comparative Table tab)
api: GET /api/v1/proposals/{id}/comparative; GET .../comparative.pdf | comparative.xlsx
description: BIBS compiles the insurer responses of the PRF into one comparative table - received terms first, cheapest first, then declines - with premium, rate, deductibles, conditions, validity and remarks per insurer, the lowest premium flagged and the recommended insurer marked. The table is always built from the current responses, so an amended response shows at once. It is exported as PDF and Excel, attached to the PRF (document type COMPARATIVE_TABLE) and sent protected with the proposal slip.
preconditions:
  - At least one response exists.
main_flow:
  - The user opens the Comparative Table tab; BIBS shows the table from the current responses.
  - The user downloads it as **PDF** or **Excel**.
rules:
  - [R1, "Order - received terms by premium ascending, then declined, then pending.", Fixed, "-"]
  - [R2, "Lowest premium and recommended insurer are flagged.", Fixed, "-"]
validations: []
notifications:
  - None.
audit:
  - Exports are logged with the file hash when sent.
acceptance:
  - A PRF with three responses shows the cheapest insurer first and flags it as lowest premium.
  - After a premium change, the table shows the new premium without any manual step.
  - The Excel export contains the same rows as the screen.
```

```fr
id: FR-NB-056
title: Generate, approve and release the Proposal Slip
brd: [BRNB.017 (p.57)]
actor: TSU officer (prepare); second TSU officer (approve)
priority: Must have
fit: NEW
screens: Proposal Request page (Proposal Slip tab, Archived versions)
api: POST /api/v1/proposals/{id}/proposal-slip/submit; POST .../proposal-slip/approve; GET .../proposal-slip.pdf
description: From the chosen insurer's received terms (the recommended one by default), TSU generates the Proposal Slip with the PROPOSAL_SLIP template. It keeps the content, terms and conditions of the quotation slip and adds the insurer's terms. Each generation is a new version PS-yyyy-nnnnnn vn, archived on the PRF as document type PROPOSAL_SLIP; earlier versions stay searchable and downloadable. After approval by a second TSU officer the slip is released to Marketing (PS_RELEASED).
preconditions:
  - The PRF is in TERMS_RECEIVED (submit) or PS_FOR_APPROVAL (approve).
main_flow:
  - TSU chooses the insurer of the proposal slip and clicks **Submit proposal slip**.
  - BIBS generates and archives the slip version (PS_FOR_APPROVAL).
  - The approver clicks **Approve and release to Marketing** (PS_RELEASED).
alternate_flows:
  - Return for correction with a reason (TERMS_RECEIVED); the next submission creates a new version.
rules:
  - [R1, "Proposal slip numbers PS-<yyyy>-nnnnnn with a version per generation.", Configurable, Parameter PROPOSAL_SLIP_PREFIX]
  - [R2, "Released to Marketing only after approval.", Fixed, "-"]
validations:
  - [No insurer chosen, Choose the insurer (or flag the recommended one) for the proposal slip, PS_INSURER_REQUIRED]
  - [Chosen insurer has no terms, "<insurer> has not quoted terms", PS_TERMS_MISSING]
  - [Approver is the preparer, The proposal slip is approved by another TSU officer, PS_FOUR_EYES]
  - [Approve before preparation, No proposal slip has been prepared yet, PS_NOT_PREPARED]
notifications:
  - TSU_APPROVE holders on submission; the AO on release.
audit:
  - Each version with template version, user and time.
acceptance:
  - A proposal slip cannot reach Marketing before a second TSU officer approves it.
  - After a return and a new submission, both versions are listed under Archived versions.
```

```fr
id: FR-NB-057
title: Send the proposal, record acceptance and create the accounts
brd: [BRNB.017 (p.57), BRNB.045 (p.97), BRNB.013 (p.55)]
actor: Marketing AO
priority: Must have
fit: NEW
screens: Proposal Request page; client page (Confirmed Proposals tab)
api: POST /api/v1/proposals/{id}/send; POST .../accept; POST .../create-accounts
description: The AO sends the released proposal slip and the comparative table to the client, password protected (SENT_TO_CLIENT). When the client accepts, the AO attaches the acceptance e-mail and records the accepted risk groups (ACCEPTED), then creates one draft account per risk group with the PRF's ARN, the chosen insurer and its quoted rate (CONVERTED). A client who declines is recorded with Client declined (NOT_PROCEEDED). Accepted proposals are listed on the client page under Confirmed Proposals.
preconditions:
  - The PRF is PS_RELEASED (send), SENT_TO_CLIENT (accept) or ACCEPTED (create accounts); account creation needs a confirmed client.
main_flow:
  - The AO clicks **Send to client**.
  - The AO uploads the acceptance e-mail and clicks **Record acceptance**.
  - The AO clicks **Create accounts**.
rules:
  - [R1, "Documents to the client are protected (FR-NB-013).", Fixed, "-"]
  - [R2, "Accounts carry the PRF's ARN (with -nn suffix when several).", Fixed, "-"]
validations:
  - [No acceptance e-mail, "Attach the client's acceptance e-mail first", ACCEPTANCE_EMAIL_REQUIRED]
  - [Unknown risk group, "The proposal covers risk groups <groups> only", ACCEPTANCE_GROUP_UNKNOWN]
  - [Create accounts before acceptance, Record the client's acceptance of the proposal first, PROPOSAL_NOT_ACCEPTED]
  - [Chosen terms missing, "No terms of <insurer> are recorded", PROPOSAL_TERMS_MISSING]
  - [Client not confirmed, "Client <code> is not yet confirmed (onboarding and KYC incomplete)", CLIENT_NOT_CONFIRMED]
notifications:
  - The client receives the protected documents and the password separately.
audit:
  - Send, acceptance and account creation audited with the created ARNs.
acceptance:
  - An accepted PRF creates its account with the chosen insurer and the quoted rate.
  - The client page lists the accepted PRF under Confirmed Proposals.
```

## Account

```fr
id: FR-NB-060
title: "Search accounts and view their details"
brd: [BRNB.050 (p.99)]
actor: "Marketing; Processing"
priority: "Must have"
fit: "CHANGE"
screens: "Accounts; Account page; Account by ARN"
api: "GET /api/v1/accounts; GET /api/v1/accounts/{id}; GET /api/v1/accounts/by-arn/{arn}"
description: "Users search accounts by one or several criteria together - ARN or client text, PN number, plate / conduction / engine / chassis number, location of risk, product, line, insurer, status, FFY, direct payment, account officer, own accounts and the period start - and open the account page with the complete details on the tabs Details, Risk Items, Premium, Placement, Policy, Documents, E-mails and History. Quick filters list My drafts, Returned to me, Awaiting payment, FFY and Direct payment."
preconditions:
  - "The user has ACCOUNT_VIEW."
main_flow:
  - "The user opens Accounts, enters criteria and clicks **Apply Filters**."
  - "BIBS lists ARN, client, product, insurer, period, gross premium, status, flags and officer."
  - "The user opens an account."
rules:
  - [R1, "Voided accounts are shown only with Include voided.", Fixed, "-"]
validations: []
fields_screen: "Accounts (search panel)"
fields:
  - [Text (ARN / client), Text, "No", "-", "-"]
  - [PN Number, Text, "No", "-", "-"]
  - [Plate / Conduction / Engine / Chassis, Text, "No", "-", Normalised match]
  - [Location of Risk, Text, "No", "-", "-"]
  - [Product / Insurer / Status, List, "No", Catalogue; workflow stages, "-"]
  - [Starts On or After / Starts On or Before, Date, "No", "-", "-"]
notifications:
  - "None."
audit:
  - "Not applicable (read-only)."
acceptance:
  - "A search by engine number finds the Motor account that insures the vehicle."
  - "The quick filter Returned to me lists the AO's accounts in RETURNED_TO_MARKETING."
```

```fr
id: FR-NB-061
title: "Create an account (individual)"
brd: [BRNB.051 (p.99-101), BRNB.029 (p.68-69)]
actor: "Marketing AO"
priority: "Must have"
fit: "CHANGE"
screens: "New Account (six-step wizard); Account page"
api: "POST /api/v1/accounts; PUT /api/v1/accounts/{id}; GET /api/v1/accounts/{id}/check"
description:
  - "Accounts are normally created from an accepted quotation or PRF (FR-NB-045, FR-NB-057). The AO can also create one directly in a six-step wizard - client; segment, product, source and insurer; period, term, currency, payment arrangement, mortgage and FFY; risk items; contact and rating; review and documents."
  - "Fields that take several values accept a list - PN numbers, insured items, locations of risk, vehicles. Amounts are in the account currency. BIBS computes the premium on every save (FR-NB-062), checks duplicates (FR-NB-063), links the account to the client, stamps the creator's sales units and cost center, and assigns the ARN and the workflow stage DRAFT."
  - "The draft is saved with **Save Draft** and automatically every 30 seconds while the user types. Cancel leaves without saving. **Submit to Processing** sends the account on (FR-NB-064)."
preconditions:
  - "The user has ACCOUNT_MAINTAIN; the client exists (a prospect is allowed for a draft; submission needs a confirmed client)."
main_flow:
  - "The AO clicks **New Account** and completes the steps."
  - "BIBS shows the premium and the duplicate findings as the AO goes."
  - "The AO uploads the documents (IDF, list of accounts, others) on the last step."
  - "The AO saves; BIBS assigns the ARN and the status DRAFT."
alternate_flows:
  - "Duplicate risk found. BIBS refuses the save and names the existing ARN (fall-out)."
  - "The AO leaves the wizard; the last autosaved draft is kept."
rules:
  - [R1, "Currency code per account; default PHP.", Fixed, "-"]
  - [R2, "Direct payment, FFY, multi-year and mortgagee only where the product allows them (FR-NB-003).", Configurable, Product master]
  - [R3, "The status is set by the workflow; it is never keyed in.", Fixed, "-"]
  - [R4, "Autosave every 30 seconds while the draft changes.", Fixed, "-"]
validations:
  - [Client missing, Select the client, ACCOUNT_CLIENT_REQUIRED]
  - [Product missing, Select the product, ACCOUNT_PRODUCT_REQUIRED]
  - [Duplicate risk, "Duplicate of existing account <ARN>: <findings>", DUPLICATE_ACCOUNT]
  - [Period end not after start, The period end must be after the period start, ACCOUNT_PERIOD_INVALID]
  - [Multi-year term below 2 years, A multi-year account needs a term of at least 2 years, ACCOUNT_TERM_INVALID]
  - [Feature not allowed, As FR-NB-003, "MULTI_YEAR_NOT_ALLOWED, FFY_NOT_ELIGIBLE ..."]
fields_screen: "New Account"
fields:
  - [Client, Look-up, "Yes", Client master, Confirmed client or prospect]
  - [Market segment / Product / Source channel, List, "Yes / Yes / No", "LOV MARKET_SEGMENT, products, LOV SOURCE_CHANNEL", Product offered to the segment]
  - [Insurer / Insurer branch, List, "No", Insurer panel, Active; branch gives the LGT rate]
  - [Period from / Period to, Date, "Yes", "-", To after from]
  - [Term (years), Number, Cond., "-", "Multi-year products: 2 to the product maximum"]
  - [Currency, Code, "Yes", "-", Default PHP]
  - [Payment, Option, "Yes", "Premium paid through BDOI / Direct payment to the insurer", Direct payment if the product allows]
  - [Mortgagee bank / Loan application no., List / Text, "No", LOV MORTGAGEE_BANK, Mortgage products]
  - [PN numbers, List of text, "No", "-", Several values]
  - [Free First Year from, Date, "No", "-", FFY products]
  - [Risk items, Table, "Yes", Item fields of the line (FR-NB-002), Sum insured per item]
  - [Contact person / E-mail / Mobile / Mailing address, Text, "No", Defaults from the client (FR-NB-066), Formats as FR-NB-031]
  - [Rating basis / Commission % override, Option / Number, "Yes / No", "Annual, Pro-rata (days), Short period (table)", "Commission 0-100"]
notifications:
  - "None until submission."
audit:
  - "Creation and every save (manual or autosave) audited with before and after values."
acceptance:
  - "An account with two PN numbers and three insured items is saved and priced."
  - "A draft left after typing is found under My drafts with the last autosaved values."
  - "A Motor account whose plate number is on a live account is refused and the message names the existing ARN."
```

```fr
id: FR-NB-062
title: "Compute the premium (Appendix A)"
brd: [BRNB.051 (p.99-101), Appendix A (p.216), BRNB.112 (p.15-16)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "Premium tab of quotations and accounts; Premium Calculator"
api: "POST /api/v1/catalog/rating/quote"
description:
  - "BIBS rates every quotation and account with the Appendix A formulas and the rates of the catalogue in force on the period start. Property - net premium = TSI x rate; DST 12.5% of net premium; premium tax, fire service tax and LGT (rate of the insurer branch) on net premium; gross premium = net premium + charges; commission = commission rate x net premium; VAT on commission 12%. Motor - OD / Theft coverage = TSI x OD factor (annual 90%, multi-year 81%); OD / Theft premium = coverage x rate; BI and PD premiums from the limit tables; basic premium = OD / Theft + BI + PD; DST 12.5%, VAT 12% and LGT on the basic premium. Other lines - TSI x rate with the taxes of the line."
  - "The period is rated annual, pro-rata by days (365 or 366) or by the short-period table. The minimum premium applies when the premium is below the product minimum, but not to endorsements or pro-rata periods. DST is rounded to centavos and then up to the next 0.50."
preconditions:
  - "Items have sums insured; a rate is on the item or is the product default."
main_flow:
  - "The user enters or changes items, period or rating basis."
  - "BIBS resolves the rates, computes the breakdown and shows it."
rules:
  - [R1, "Rates - DST 12.5; VAT 12 (Property 0); premium tax Property 12; FST Property 2; motor OD factors 90 / 81 (defaults).", Configurable, Rates & Taxes (Q35 for OD basis; Q43 for premium tax)]
  - [R2, "Short-period percentages by months covered 1-12.", Configurable, Rates & Taxes (short-period table)]
  - [R3, "Commission from the insurer x product rate, else the insurer-wide rate.", Configurable, Insurer commission rates]
  - [R4, "LGT rate from the insurer branch.", Configurable, Insurer branches]
  - [R5, "Minimum premium not applied to endorsements or pro-rata periods.", Fixed, "-"]
validations:
  - [No item, Enter at least one item to rate, RATING_NO_ITEMS]
  - [Sum insured negative, Enter a sum insured of zero or more for every item, RATING_SUM_INSURED_INVALID]
  - [Rate missing, "Enter the premium rate of <item>", RATING_RATE_REQUIRED]
  - [OD factor not set up, The motor OD/Theft coverage factor is not set up, RATING_OD_FACTOR_MISSING]
  - [Limit not in the table, "<coverage> limit <amount> is not in the limit table", MOTOR_LIMIT_UNKNOWN]
  - [Short-period rate missing, "No short-period rate is set up for <n> month(s)", SHORT_PERIOD_RATE_MISSING]
  - [Period invalid, The period end must be after the period start, RATING_PERIOD_INVALID]
notifications:
  - "None."
audit:
  - "The rated breakdown is stored with the quotation version and the account."
acceptance:
  - "A Property item of TSI 1,000,000 at 0.25% gives net premium 2,500.00, DST 312.50, premium tax 300.00, FST 50.00 plus LGT."
  - "A DST of 101.20 is rounded to 101.50."
  - "A pro-rata period of 180 days gives 180/365 of the annual premium without the minimum premium."
```

> [!QUESTION] Appendix A to confirm
> Appendix A (p.216) gives the Fire premium tax as 12% of net premium and two Motor OD / Theft bases (90% and 81% of TSI). The build applies them as written, as rates in the catalogue. BDOI confirms the premium tax basis (Q43) and the multi-year basis (Q35).

```fr
id: FR-NB-063
title: "Prevent duplicate accounts"
brd: [BRNB.032 (p.72-73), BRNB.051 (p.99-101), BRNB.066 (p.111-114)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "New Account; Edit Account; bulk account uploads"
api: "POST /api/v1/accounts; PUT /api/v1/accounts/{id}"
description:
  - "Before an account is saved, BIBS compares its risks with the live accounts of the company (every status except VOIDED and CANCELLED). Motor - the plate number, conduction sticker, engine number or chassis / serial number (normalised alphanumeric) of any item matches an item of another account. Fire and property - the same client, the same normalised location of risk and the same insured items. A match blocks the save, and the fall-out names the existing ARN and the matching identifier."
  - "A CTPL product (CTP01, CTP02) may share a vehicle with a Motor package account (MTR codes), as the BRD allows. For an endorsement the duplicate is allowed and returned as a flagged finding."
  - "**Built behaviour:** the BRD's example keys for accounts (e-mail, phone, ID number) are the client duplicate keys of FR-NB-033; accounts are compared on their risks, as BRNB.051 and 066 specify."
preconditions:
  - "None."
main_flow:
  - "The user saves an account, or a bulk row is processed."
  - "BIBS checks every item against the live accounts."
  - "When there is a match, BIBS refuses the account and lists the fall-out with the existing ARN."
rules:
  - [R1, "Live accounts = all statuses except VOIDED and CANCELLED.", Fixed, "-"]
  - [R2, "CTPL versus Motor package exception.", Fixed, "-"]
  - [R3, "Endorsements - duplicates flagged, not blocked.", Fixed, "-"]
  - [R4, "Fire location match - exact on the normalised address and city (Q22).", Fixed, "-"]
validations:
  - [Duplicate risk, "Duplicate of existing account <ARN>: <identifier> <value> (item <n>)", DUPLICATE_ACCOUNT]
notifications:
  - "None."
audit:
  - "Bulk duplicate fall-outs are kept with the bulk job and counted in report NB-STAGE-OUTCOME."
acceptance:
  - "A second Motor account with the same chassis number is refused and names the first ARN."
  - "A CTPL account for a vehicle insured under MTR08 is accepted."
  - "A Fire account for the same client, address and insured items as a live account is refused."
```

```fr
id: FR-NB-064
title: "Update, submit and validate an account"
brd: [BRNB.053 (p.102-103), BRNB.054 (p.103-104), BRNB.025 (p.63-65), BRNB.096 (p.6)]
actor: "Marketing AO (update, submit); Processing (update, validate)"
priority: "Must have"
fit: "CHANGE"
screens: "Edit Account; Account page (workflow panel, check panel)"
api: "PUT /api/v1/accounts/{id}; POST .../{id}/submit; POST .../{id}/validate"
description:
  - "Marketing edits an account in DRAFT or RETURNED_TO_MARKETING - details, multi-value fields, items and documents - and BIBS recomputes the premium. **Submit to Processing** checks the mandatory fields of the product (FR-NB-002), the required documents and the rated premium, and moves the account to SUBMITTED."
  - "Processing edits a submitted account (role-specific corrections) and clicks **Validate**. Validation requires a confirmed client and, where the TSU rule applies, a TSU clearance. The account moves to AWAITING_PAYMENT; a direct-payment account moves straight on to READY_FOR_PLACEMENT (FR-NB-092)."
  - "Cancel discards unsaved changes; a draft can be voided (FR-NB-012)."
preconditions:
  - "The user has ACCOUNT_MAINTAIN (Marketing) or ACCOUNT_PROCESS (Processing); the account is in an editable stage."
main_flow:
  - "The user opens the account and clicks **Edit**."
  - "The user changes the fields and saves; BIBS validates and prices."
  - "Marketing clicks **Submit to Processing**; Processing reviews and clicks **Validate**."
alternate_flows:
  - "Incomplete account. Submission is refused; the check panel lists every missing field and document."
  - "Processing returns the account to Marketing with a reason (FR-NB-067)."
rules:
  - [R1, "Editable stages - DRAFT and RETURNED_TO_MARKETING (Marketing); SUBMITTED (Processing).", Fixed, "-"]
  - [R2, "Submission gates - mandatory fields, required documents, rated premium.", Fixed, "-"]
  - [R3, "Validation gates - confirmed client, TSU clearance when required.", Fixed, "-"]
  - [R4, "SLA - DRAFT 24 hours, SUBMITTED 8 hours (defaults).", Configurable, Workflow stage definitions]
validations:
  - [Mandatory fields missing, "Complete the mandatory fields of <product>: <fields>", ACCOUNT_INCOMPLETE]
  - [Documents missing, "Upload the mandatory documents: <types>", MISSING_DOCUMENTS]
  - [Premium not rated, Compute the premium (sum insured and rate of every item) first, PREMIUM_NOT_RATED]
  - [Edit in a locked stage, "Account <ARN> is <status>", ACCOUNT_NOT_EDITABLE]
  - [Client not confirmed, "Client <code> is not yet confirmed (onboarding and KYC incomplete)", CLIENT_NOT_CONFIRMED]
  - [TSU clearance missing, "TSU must clear the account first: <reason>", TSU_CLEARANCE_REQUIRED]
notifications:
  - "Processing queue on submission; the AO on validation or return."
audit:
  - "Each update with before and after values; submit and validate in the status history."
acceptance:
  - "A submitted account can no longer be edited by Marketing."
  - "An account of a prospect cannot be validated."
  - "Processing corrects the plate number of a submitted account and validates it; the history shows the change."
```

```fr
id: FR-NB-065
title: "Create and update accounts in bulk"
brd: [BRNB.066 (p.111-114), BRNB.052 (p.101-102), BRNB.025 (p.63-65), BRNB.064 (p.110), BRNB.039 (p.80-81), BRNB.044 (p.96-97)]
actor: "Marketing; Processing; System"
priority: "Must have"
fit: "NEW"
screens: "Bulk Account Creation (/bulk/ACCOUNT_CREATE); Bulk Upload (ACCOUNT_UPDATE)"
api: "POST /api/v1/bulk/jobs (ACCOUNT_CREATE, ACCOUNT_UPDATE)"
description:
  - "ACCOUNT_CREATE takes a list of accounts from a source-system extract or a manual upload (.xlsx, .ods, .csv), with the parameters product, default segment and submit. For each row BIBS sanitises the values, finds the client by code or by name and birth date or creates a prospect, validates the fields and duplicates, prices the account, assigns the ARN (and associates the quotation reference given in the row), and saves it as a draft - or submits it when the parameter submit is set. Failed rows, including duplicate fall-outs with the existing ARN, are listed in the error report."
  - "ACCOUNT_UPDATE takes the ARN and the columns to change (segment, insurer and branch, mortgagee, loan application number, PN list, contact e-mail and mobile); blank cells keep the current value. Only editable accounts are updated."
  - "Documents for bulk accounts are uploaded on each account or linked to several accounts at once (FR-NB-017)."
preconditions:
  - "The user has BULK_PROCESS and ACCOUNT_MAINTAIN."
main_flow:
  - "The user selects the product and segment, uploads the file and reviews the rows."
  - "The user commits; BIBS creates or updates the accounts and lists ARNs and failures."
rules:
  - [R1, "Each row is one account; one row fails without stopping the others.", Fixed, "-"]
  - [R2, "Sanitisation - trim, upper-case codes, normalise identifiers; BDOI criteria wait for Q14.", Configurable, Upload handler (Q14)]
  - [R3, "Update only accounts in DRAFT, RETURNED_TO_MARKETING or SUBMITTED of the same company.", Fixed, "-"]
validations:
  - [Product not chosen, Choose the product of the upload, BULK_PRODUCT_REQUIRED]
  - [No client, Give the Client Code or the Client Name, BULK_CLIENT_REQUIRED]
  - [Name format, "Enter an individual's name as 'Last, First'", BULK_CLIENT_NAME_FORMAT]
  - [Account of another company, "Account <ARN> belongs to another company", ACCOUNT_OTHER_COMPANY]
  - [Account not editable, "Account <ARN> is <status>", ACCOUNT_NOT_EDITABLE]
  - [Duplicate risk, "Duplicate of existing account <ARN>: <findings>", DUPLICATE_ACCOUNT]
fields_screen: "ACCOUNT_CREATE template columns (main)"
fields:
  - [Client code / Client name, Text, Cond., Client master, "Code, or name as Last, First"]
  - [Segment; Insurer; Branch, Code, "No", Lists, Default from the screen]
  - [Mortgagee; Loan application no.; PN numbers (;), Text, "No", LOV MORTGAGEE_BANK, "-"]
  - [Quotation reference, Text, "No", "-", Associated with the account]
  - [Item columns of the line, Mixed, Cond., Field rules, "Sum insured, vehicle or location data"]
notifications:
  - "None; the job result shows the outcome."
audit:
  - "Each account audited with the bulk job number; failed rows kept with the job."
acceptance:
  - "An upload of 200 CBG Fire accounts creates the valid rows as drafts and lists duplicates with the existing ARN."
  - "With the parameter submit, the created accounts are in SUBMITTED."
  - "An update row with an ARN in PLACED is refused with ACCOUNT_NOT_EDITABLE."
```

```fr
id: FR-NB-066
title: "Account-level contact details"
brd: [BRNB.109 (p.14)]
actor: "Marketing AO"
priority: "Must have"
fit: "NEW"
screens: "New Account (contact step); Account page"
api: "POST /api/v1/accounts; PUT /api/v1/accounts/{id}"
description: "Each account has its own contact - contact person, e-mail, mobile and mailing address. When the account has no contact of its own, BIBS fills it from the client's contact. Communications about the account (e-policy dispatch, Insurance Advice, letters) use the account contact. Users view and update it on the account."
preconditions:
  - "The user has ACCOUNT_MAINTAIN to change it."
main_flow:
  - "The wizard pre-fills the contact from the client."
  - "The AO keeps or changes it and saves."
rules:
  - [R1, "Blank account contact = client contact.", Fixed, "-"]
validations:
  - [E-mail or mobile format, Enter a valid e-mail address / mobile number, "-"]
notifications:
  - "None."
audit:
  - "Contact changes with before and after values."
acceptance:
  - "An account created for a client with e-mail a@x.ph shows that e-mail as account contact."
  - "An e-policy is dispatched to the account contact e-mail when it differs from the client's."
```

```fr
id: FR-NB-067
title: "Handle accounts returned to Marketing"
brd: [BRNB.033 (p.73-74), BRNB.058 (p.106-107)]
actor: "Processing (return); Marketing AO (correct and resubmit)"
priority: "Must have"
fit: "NEW"
screens: "Accounts (Returned to me); My Work; Account page"
api: "POST /api/v1/workflow/cases/{id}/actions/return; POST /api/v1/accounts/{id}/resubmit"
description: "Processing returns an account to Marketing with a reason and a comment from the stages SUBMITTED, AWAITING_PAYMENT, READY_FOR_PLACEMENT or RETURNED_BY_INSURER. The account moves to RETURNED_TO_MARKETING and appears under Returned to me and in the AO's queue. The AO reads the reason, updates the account, adds comments and clicks **Resubmit to Processing**; the account goes back to SUBMITTED."
preconditions:
  - "The user holds the return permission of the stage (Processing) or ACCOUNT_MAINTAIN (resubmit)."
main_flow:
  - "Processing clicks **Return to Marketing** and selects the reason."
  - "The AO opens the account, corrects it and adds a comment."
  - "The AO clicks **Resubmit to Processing**."
rules:
  - [R1, "Resubmission runs the submission gates again.", Fixed, "-"]
  - [R2, "SLA of RETURNED_TO_MARKETING 24 hours (default).", Configurable, Workflow stage definitions]
validations:
  - [Return without reason, "Select a reason for 'Return to Marketing'", WORKFLOW_REASON_REQUIRED]
  - [Resubmit incomplete, As FR-NB-064, "ACCOUNT_INCOMPLETE, MISSING_DOCUMENTS"]
notifications:
  - "The AO (originator) on return; the Processing queue on resubmission."
audit:
  - "Return reason, comments and resubmission in the history."
acceptance:
  - "A returned account shows the reason on its page and in Returned to me."
  - "After resubmission the account is back in the Processing queue."
```

```fr
id: FR-NB-068
title: "Maintain the Free First Year register"
brd: [BRNB.113 (p.16-17)]
actor: "Marketing AO"
priority: "Must have"
fit: "NEW"
screens: "FFY Register; Account page (Tags card); Bulk Upload (Free First Year tagging)"
api: "PUT /api/v1/accounts/{id}/ffy; POST /api/v1/accounts/{id}/ffy/cancel; POST /api/v1/bulk/jobs (FFY_TAGGING)"
description:
  - "An eligible account is tagged Free First Year with its FFY start; BIBS sets the FFY end to one year less a day after the start. The tag is set on the account (**Tag Free First Year**) or by upload (FFY_TAGGING), which finds each account by its vehicle identifier and refuses a row that matches none or several accounts. Users edit the FFY start and cancel the tag with a reason; the account itself is not deleted and the cancelled tag stays visible."
  - "The FFY Register lists the FFY accounts with FFY start and end, status and officer, and is searchable, filterable and sortable. The tag is kept across account updates and is available to reports and to Renewal."
preconditions:
  - "The product is FFY eligible; the account is active."
main_flow:
  - "The AO opens the account and clicks **Tag Free First Year**, enters the start and saves."
  - "BIBS computes the end and lists the account in the FFY Register."
alternate_flows:
  - "Cancel FFY with a reason (FFY_CANCEL_REASON)."
  - "Bulk tagging - TAG or CANCEL per row."
rules:
  - [R1, "FFY end = start + 1 year - 1 day.", Fixed, "-"]
  - [R2, "Only FFY-eligible products and active accounts are tagged.", Fixed, "-"]
  - [R3, "Cancellation reasons - Auto loan cancelled; Dealer promo withdrawn; Tagged in error; Others.", Configurable, LOV FFY_CANCEL_REASON]
validations:
  - [Product not eligible, "Product <code> is not Free First Year eligible", FFY_NOT_ELIGIBLE]
  - [Start missing, Enter the Free First Year start, FFY_START_REQUIRED]
  - [Cancel an untagged account, "Account <ARN> is not tagged Free First Year", FFY_NOT_TAGGED]
  - [Account not active, "Account <ARN> is <status>", ACCOUNT_NOT_ACTIVE]
  - [Vehicle matches none or several, "No live account insures vehicle <id> / Several accounts insure vehicle <id>: <ARNs>", FFY_VEHICLE_NOT_UNIQUE]
  - [Bulk action invalid, Action must be TAG or CANCEL, FFY_ACTION_INVALID]
notifications:
  - "None."
audit:
  - "Tag, edit and cancel with user, date and type of change."
acceptance:
  - "An account tagged FFY from 1-Mar-2026 shows the end 28-Feb-2027 in the register."
  - "A cancelled FFY tag keeps its dates and shows the cancellation reason."
  - "An upload row whose engine number matches two accounts is refused and names both ARNs."
```

```fr
id: FR-NB-069
title: "Identify and tag direct-payment accounts"
brd: [BRNB.114 (p.17)]
actor: "Marketing AO; System"
priority: "Must have"
fit: "NEW"
screens: "New Account (Payment); Direct Payment; Account page (Tags card)"
api: "PUT /api/v1/accounts/{id}/payment-arrangement; POST /api/v1/placement/gate/{arn}/direct-payment"
description:
  - "The payment arrangement of an account is \"Premium paid through BDOI\" or \"Direct payment to the insurer\". Direct payment is offered only for products that allow it (configurable criterion). The tag drives the process - the account skips the payment gate, booking opens no client premium receivable, and the tag shows on billing, placement, booking and report views."
  - "Users change the arrangement until the payment gate is passed. The Direct Payment screen lists the direct-payment accounts, searchable and filterable by status."
  - "**Built behaviour:** the accounting variant for direct payment (commission receivable only) follows the build's booking rules; BDOI confirms it under Q29."
preconditions:
  - "The product allows direct payment."
main_flow:
  - "The AO selects Direct payment to the insurer on the account."
  - "On validation BIBS moves the account past the payment gate (FR-NB-092)."
alternate_flows:
  - "Tagged after validation. Processing releases the waiting account with **Direct payment** on the gate panel."
rules:
  - [R1, "Eligibility is a product setting.", Configurable, Product master]
  - [R2, "The arrangement is locked once the payment gate is passed.", Fixed, "-"]
validations:
  - [Product not eligible, "Product <code> cannot be paid directly to the insurer", DIRECT_PAYMENT_NOT_ELIGIBLE]
  - [Gate already passed, "The payment gate of <ARN> is passed", PAYMENT_ARRANGEMENT_LOCKED]
  - [Release of an account that is not direct payment, "Account <ARN> is not paid directly to the insurer", GATE_NOT_DIRECT_PAYMENT]
notifications:
  - "None."
audit:
  - "Every change of the arrangement with user, date and values."
acceptance:
  - "A direct-payment account goes from validation to Ready for placement without a payment."
  - "The booked invoice of a direct-payment account has no client premium receivable."
  - "The arrangement cannot change after the payment gate."
```

## Placement and hold cover

```fr
id: FR-NB-080
title: "Generate the placement slip when the prerequisites are met"
brd: [BRNB.069 (p.115-116; updated p.19)]
actor: "Processing; System"
priority: "Must have"
fit: "NEW"
screens: "Placement Workbench (For Placement); Generate Placement Slips dialog; Placement Slips; Account Placement"
api: "GET /api/v1/placement/readiness?arns=; POST /api/v1/placement/slips/generate; POST .../slips/{id}/regenerate; GET .../slips/{id}/files/{pdf|xlsx}"
description:
  - "Processing selects one or several accounts on the Placement Workbench and clicks **For Placement**. BIBS groups them by insurer branch and generates one placement slip PL-yyyy-nnnnnn per branch, as PDF and Excel from the PLACEMENT_SLIP template. A slip is generated only when every account meets the prerequisites - stage Ready for placement, payment or client confirmation recorded (unless direct payment), required documents present, TSU clearance given when required, and insurer and branch usable. The dialog shows each unmet prerequisite per account before generation."
  - "After an insurer return, **Regenerate** creates the next version of the slip (for example PL-2026-000001 v2) and marks the previous one SUPERSEDED."
preconditions:
  - "The user has PLACEMENT_MANAGE."
main_flow:
  - "Processing selects accounts in the Ready for placement tile and clicks **For Placement**."
  - "BIBS shows the prerequisites per account."
  - "Processing clicks **Generate Slips**; BIBS creates the slips (GENERATED) and lists them under To Send."
alternate_flows:
  - "An account does not meet a prerequisite. No slip is generated for the selection; the message lists each problem."
  - "Regenerate after a return."
rules:
  - [R1, "One slip per insurer branch; an account is on one current slip.", Fixed, "-"]
  - [R2, "Prerequisites - READY_FOR_PLACEMENT; payment confirmed or direct payment; documents complete; TSU cleared; insurer usable. Segment-specific prerequisites wait for Q26.", Fixed, "-"]
  - [R3, "Slip statuses - GENERATED, SENT, SUPERSEDED.", Fixed, "-"]
validations:
  - [No accounts selected, Select the accounts to place, SLIP_NO_ACCOUNTS]
  - [Prerequisites not met, "Placement prerequisites not met - <ARN>: <problem> | ...", SLIP_PREREQUISITES_UNMET]
  - [Account not ready, "The account is <status>; a slip needs Ready for placement", NOT_READY_FOR_PLACEMENT]
  - [Payment not recorded, The payment or client confirmation is not recorded, PAYMENT_NOT_CONFIRMED]
  - [Documents missing, "Missing documents: <types>", DOCUMENTS_MISSING]
  - [TSU clearance missing, "TSU clearance is required (<rule>) and not given", TSU_NOT_CLEARED]
  - [Insurer not set, Choose the insurer and branch on the account before placement, INSURER_NOT_SET]
  - [Regenerate a superseded slip, "Slip <number> was already replaced", SLIP_SUPERSEDED]
notifications:
  - "None; the slips appear under To Send."
audit:
  - "Generation and regeneration with the template version, user and time."
acceptance:
  - "An account awaiting payment cannot be put on a slip; the dialog names PAYMENT_NOT_CONFIRMED."
  - "Five accounts of two insurer branches produce two slips."
  - "Regenerating a slip after a return creates version 2 and marks version 1 superseded."
```

```fr
id: FR-NB-081
title: "Send the placement slip to the insurer"
brd: [BRNB.071 (p.116)]
actor: "Processing; System"
priority: "Must have"
fit: "NEW"
screens: "Placement Slips (Send, Send Slips); Placement Workbench (Send Slips)"
api: "POST /api/v1/placement/slips/{id}/send; POST /api/v1/placement/slips/send; GET .../slips/{id}/email-draft"
description:
  - "BIBS e-mails each slip to the placement mailbox of its insurer branch (from the insurer master), with the PDF and Excel protected and the password sent separately. The first send records the placement of each account (PLACED); later sends are resends, for example after a return or a failed delivery. The send log shows recipients, time and outcome per slip."
  - "**Built behaviour:** slips are sent by e-mail when the user clicks Send; SFTP and insurer API channels wait for Q06, and an insurer set up for them is refused."
preconditions:
  - "The slip is GENERATED or SENT; the user has PLACEMENT_MANAGE."
main_flow:
  - "Processing opens To Send, selects slips and clicks **Send Slips**."
  - "BIBS queues the protected e-mails and marks the slips SENT and the accounts PLACED."
alternate_flows:
  - "Resend a sent slip to the same or corrected recipients."
rules:
  - [R1, "Recipients from the insurer branch's placement addresses.", Configurable, Insurer master]
  - [R2, "Only the e-mail channel is available (Q06).", Fixed, "-"]
validations:
  - [Insurer on SFTP or API, "<insurer> is set up for <channel> placements; only e-mail is available (Q06)", PLACEMENT_CHANNEL_PARKED]
  - [Superseded slip, "Slip <number> v<n> was replaced by a new version", SLIP_SUPERSEDED]
notifications:
  - "Processing is notified of failed dispatches."
audit:
  - "Each send and resend logged with recipients, time, subject and attachment hash; counted in NB-PLC-SUMMARY."
acceptance:
  - "A sent slip moves its accounts to Placed with the slip reference and date."
  - "A failed e-mail is shown as failed and can be resent."
```

```fr
id: FR-NB-082
title: "Request a 30-day hold cover from the insurer"
brd: [BRNB.072 (p.116)]
actor: "Processing; System"
priority: "Must have"
fit: "NEW"
screens: "Account Placement (Hold Cover panel)"
api: "POST /api/v1/placement/accounts/{arn}/hold-cover"
description:
  - "For an account in placement (Ready for placement, Placed or Returned by insurer), Processing clicks **Request Hold Cover**. BIBS generates the request from the HOLD_COVER_REQUEST template with the start date and an expiry of 30 days, and e-mails it to the insurer branch (REQUESTED)."
  - "**Built behaviour:** the request is sent when the user clicks the button, not automatically for every placement; which accounts need an automatic request waits for Q27."
preconditions:
  - "The account is in placement and has an insurer; no open hold cover."
main_flow:
  - "Processing opens the account placement page and clicks **Request Hold Cover**."
  - "BIBS sends the request and shows the hold cover as Requested with its expiry."
rules:
  - [R1, "Hold cover period 30 days (default).", Configurable, Parameter HOLD_COVER_DAYS]
  - [R2, "One open hold cover per account.", Fixed, "-"]
validations:
  - [Account not in placement, "A hold cover is requested while the account is being placed, not <status>", HOLD_COVER_NOT_ALLOWED]
  - [Hold cover already open, "Account <ARN> already has a hold cover requested or confirmed", HOLD_COVER_OPEN]
  - [No insurer, Choose the insurer on the account first, INSURER_NOT_SET]
notifications:
  - "None on request; see FR-NB-083 for expiry alerts."
audit:
  - "Request logged with the e-mail and the dates."
acceptance:
  - "A hold cover requested on 1-Jun expires on 1-Jul and the e-mail is in the account's E-mails tab."
  - "A second request while one is open is refused."
```

```fr
id: FR-NB-083
title: "Record the insurer's hold cover confirmation"
brd: [BRNB.103 (p.10-11)]
actor: "Processing (Marketing views)"
priority: "Must have"
fit: "NEW"
screens: "Account Placement (Hold Cover panel); Placement Workbench (Hold cover expiring tile); Placement Update Report"
api: "POST /api/v1/placement/hold-cover/confirm; POST /api/v1/placement/hold-cover/decline"
description:
  - "When the insurer confirms, Processing records the confirmation - insurer, insurer reference, confirmation date and expiry (CONFIRMED) - or records a decline. The hold cover status is shown on the account, on the Placement Workbench and in the Placement Update Report, so Marketing sees it."
  - "A daily job alerts Processing about hold covers expiring within 5 days, once per hold cover, and marks EXPIRED those past their expiry while the policy is still awaited."
preconditions:
  - "The account has a hold cover REQUESTED."
main_flow:
  - "Processing clicks **Record Confirmation**, enters the reference and expiry and saves."
  - "BIBS stores the confirmation and shows the status Confirmed."
alternate_flows:
  - "**Record Decline** with the insurer reference."
rules:
  - [R1, "Statuses - REQUESTED, CONFIRMED, DECLINED, EXPIRED.", Fixed, "-"]
  - [R2, "Expiry alert 5 days before (default); job HOLD_COVER_EXPIRY daily at 08:30 PHT.", Configurable, Parameter HOLD_COVER_ALERT_DAYS]
  - [R3, "What happens at expiry without a policy waits for Q27.", Configurable, "-"]
validations:
  - [Reference missing, "Enter the insurer's reference", HOLD_COVER_REFERENCE]
  - [No open hold cover, "Account <ARN> has no open hold cover", HOLD_COVER_NONE]
  - [Expiry before start, "The hold cover cannot expire before it starts (<date>)", HOLD_COVER_DATES]
  - [Hold cover closed, "The hold cover of <ARN> is <status>", HOLD_COVER_CLOSED]
fields_screen: "Record Confirmation"
fields:
  - [Insurer reference, Text, "Yes", "-", "-"]
  - [Expires on, Date, "Yes", "-", On or after the start]
notifications:
  - "PLACEMENT_MANAGE holders when a hold cover is about to expire."
audit:
  - "Confirmation or decline with user and time."
acceptance:
  - "A confirmed hold cover shows its reference and expiry on the account and in the Placement Update Report."
  - "A hold cover expiring in 4 days raises one alert to Processing."
```

```fr
id: FR-NB-084
title: "Handle placements returned by the insurer"
brd: [BRNB.034 (p.74-75), BRNB.059 (p.107)]
actor: "Processing; Marketing AO / TL"
priority: "Must have"
fit: "NEW"
screens: "Placement Workbench (Returned by insurer tile); Account Placement (Insurer Returns); Record Insurer Return dialog"
api: "POST /api/v1/placement/accounts/{arn}/insurer-return; POST /api/v1/placement/accounts/{arn}/resubmit"
description:
  - "When the insurer returns a placement, Processing records the return with a reason and the insurer's remarks (RETURNED_BY_INSURER). Processing updates the account and clicks **Resubmit for Placement** (READY_FOR_PLACEMENT), then regenerates and resends the slip (FR-NB-080, 081). When Marketing must act, Processing returns the account to Marketing with a reason (RETURNED_TO_MARKETING); the AO updates and resubmits it (FR-NB-067) and it comes back to Processing."
  - "The return is resolved when the account leaves RETURNED_BY_INSURER; each return is kept with its resolution."
preconditions:
  - "The account is PLACED (record return) or RETURNED_BY_INSURER (resubmit or return)."
main_flow:
  - "Processing clicks **Record Insurer Return**, selects the reason and enters the remarks."
  - "Processing corrects the account and clicks **Resubmit for Placement**."
rules:
  - [R1, "Return reasons from RETURN_REASON, including Insurer underwriting review.", Configurable, LOV RETURN_REASON]
  - [R2, "SLA of RETURNED_BY_INSURER 8 hours (default).", Configurable, Workflow stage definitions]
validations:
  - [Reason missing, "Select a reason for 'Returned by insurer'", WORKFLOW_REASON_REQUIRED]
  - [Insurer differs from the account, "Account <ARN> is set up with insurer <code>", PLACEMENT_INSURER_MISMATCH]
  - [Wrong status, "Account <ARN> is <status>, not <expected>", ACCOUNT_STATUS_INVALID]
fields_screen: "Record Insurer Return"
fields:
  - [Reason, List, "Yes", LOV RETURN_REASON, Active value]
  - [Insurer remarks, Long text, "No", "-", "-"]
notifications:
  - "PLACEMENT_MANAGE holders on return; the AO when returned to Marketing."
audit:
  - "Return, remarks, resolution and resubmission in the account history."
acceptance:
  - "A returned account appears in the Returned by insurer tile with the insurer's remarks."
  - "After resubmission and a new slip, the account is Placed again and the return shows as resolved."
```

```fr
id: FR-NB-085
title: "Cancel placement of one or several accounts"
brd: [BRNB.062 (p.109)]
actor: "Processing"
priority: "Must have"
fit: "NEW"
screens: "Placement Workbench (Cancel Placement); Account Placement"
api: "POST /api/v1/placement/accounts/cancel"
description:
  - "Processing selects one or several accounts that are Ready for placement, Placed or Returned by insurer, clicks **Cancel Placement**, and enters a reason (CANCELLATION_REASON) and a comment. Each account is tagged Placement cancelled (PLACEMENT_CANCELLED); each gets its own result, so one failure does not stop the others."
  - "**Built behaviour:** the BRD says the account returns to the previous workflow. In BIBS a cancelled placement stays in the stage Placement cancelled, visible and reportable, until it is reactivated (FR-NB-086), which returns it to Ready for placement. This is the pre-issuance reversal of BRNB.094; it has no accounting."
preconditions:
  - "The user has PLACEMENT_MANAGE."
main_flow:
  - "Processing selects the accounts and clicks **Cancel Placement**."
  - "Processing selects the reason and enters the comment."
  - "BIBS cancels each account and shows the result per ARN."
rules:
  - [R1, "Cancellation reasons - Client request; Non-payment of premium; Loan cancelled or paid off; Insurer request; Others.", Configurable, LOV CANCELLATION_REASON]
validations:
  - [Nothing selected, Select at least one account, PLACEMENT_NO_ACCOUNTS]
  - [Reason missing, "Select a reason for 'Cancel placement'", WORKFLOW_REASON_REQUIRED]
  - [Account of another company, "Account <ARN> belongs to another company", ACCOUNT_OTHER_COMPANY]
notifications:
  - "The account officer of each cancelled account."
audit:
  - "Each cancellation with reason, comment, user and time."
acceptance:
  - "Cancelling three accounts, one of which is already booked, cancels two and reports the third as refused."
  - "A cancelled placement appears with status Placement cancelled in the Account Status Report."
```

```fr
id: FR-NB-086
title: "Reactivate cancelled placements"
brd: [BRD 2.1.16 (p.180), BRD 3.4.2.2.35 (p.208)]
actor: "Marketing; Processing"
priority: "Must have"
fit: "NEW"
screens: "Placement Workbench (Reactivate); Account page"
api: "POST /api/v1/placement/accounts/reactivate"
description: "The user selects one or several accounts in Placement cancelled and clicks **Reactivate** with a comment. Each account moves automatically to Ready for placement, the next workflow step. The requirement is in the role matrix of the BRD without a BRNB ID (Q25); it is built as specified in BRD 2.1.16."
preconditions:
  - "The user has ACCOUNT_MAINTAIN or PLACEMENT_MANAGE; the accounts are in PLACEMENT_CANCELLED."
main_flow:
  - "The user selects the cancelled accounts and clicks **Reactivate**."
  - "BIBS moves each account to READY_FOR_PLACEMENT and shows the result per ARN."
rules:
  - [R1, "Reactivation keeps the payment evidence; the account needs a new slip.", Fixed, "-"]
validations:
  - [Nothing selected, Select at least one account, PLACEMENT_NO_ACCOUNTS]
  - [Account not cancelled, "'reactivate' is not allowed while <ARN> is in stage <stage>", WORKFLOW_TRANSITION_NOT_ALLOWED]
notifications:
  - "Placement owners on stage entry."
audit:
  - "Reactivation with user, time and comment."
acceptance:
  - "A reactivated account is in Ready for placement and can be put on a new slip."
```

## Payment confirmation

```fr
id: FR-NB-090
title: "Generate the CLPC billing file (CBG Fire)"
brd: [BRNB.067 (p.114-115)]
actor: "Processing (BILLING_MANAGE)"
priority: "Must have"
fit: "NEW"
screens: "CLPC Billing (CBG Fire Awaiting Payment, Billing Batches)"
api: "GET /api/v1/placement/billing/candidates; POST .../billing/batches; GET .../billing/batches/{id}/file?format=XLSX|ODS"
description:
  - "BIBS lists the CBG Fire (CBG, Property) accounts awaiting payment that are not already on an unpaid billing item. Processing creates a billing batch BILL-yyyy-nnnnnn with all or selected accounts. The batch file holds PN number (loan account), loan application number, booking date (inception date), borrower (assured's name), originating unit (department code), premium, reference (ARN), BDOI location and amortised Y/N, and downloads as .xlsx or .ods."
  - "**Built behaviour:** the file is downloaded and forwarded to CLPC by the user; the automatic transfer to CLPC waits for Q28. The ARN replaces the QPS reference number (OOS-2)."
preconditions:
  - "CBG Fire accounts are in AWAITING_PAYMENT."
main_flow:
  - "Processing opens CLPC Billing and reviews the accounts awaiting payment."
  - "Processing clicks **Bill All** (or selects accounts and clicks **Bill Selected**); BIBS creates the batch (GENERATED)."
  - "Processing downloads the file (**Excel** or **ODS**) and sends it to CLPC."
rules:
  - [R1, "Candidates - segment CBG, line Property, stage AWAITING_PAYMENT, not on an unpaid billing item.", Fixed, "-"]
  - [R2, "Batch statuses - GENERATED, REPORT_RECEIVED, CLOSED.", Fixed, "-"]
  - [R3, "Three CLPC billing variants of BRD-12 (Report List #65-67) are planned as variants of NB-CLPC-BILLING.", Configurable, "-"]
validations:
  - [Nothing to bill, No CBG Fire account awaiting payment is left to bill, BILLING_NOTHING_TO_BILL]
  - [Selected accounts not candidates, "Not CBG Fire accounts awaiting payment, or already billed: <ARNs>", BILLING_NOT_CANDIDATE]
  - [Format not supported, Choose the billing file format XLSX or ODS, BILLING_FORMAT]
fields_screen: "Billing file columns"
fields:
  - [PN No. / Loan Application No., Text, "-", Account, "-"]
  - [Booking Date, Date, "-", Account inception, "-"]
  - [Borrower / Originating Unit / BDOI Location, Text, "-", "Client, sales unit, branch", "-"]
  - [Premium, Amount, "-", Gross premium, "-"]
  - [Reference (ARN) / Amortised, Text, "-", Account, Y or N]
notifications:
  - "None."
audit:
  - "Batch creation and each file download logged."
acceptance:
  - "A batch of 40 CBG Fire accounts downloads as .ods with the 40 rows and all columns."
  - "An account already billed and unpaid is not offered for a second batch."
```

```fr
id: FR-NB-091
title: "Match payment reports (CLPC and other segments)"
brd: [BRNB.067 (p.114-115), BRNB.068 (p.115)]
actor: "Processing; System"
priority: "Must have"
fit: "CHANGE"
screens: "CLPC Billing (Payment Reports); Payment Report page; Upload Payment Report dialog"
api: "POST /api/v1/placement/billing/reports; POST .../reports/{id}/lines/{lineId}/match; POST .../reports/{id}/confirm | discard"
description:
  - "Processing uploads a payment report (.xlsx, .csv, .ods). A CLPC report answers a billing batch and is matched on PN number or loan application number. A reference report (other segments) is matched on the ARN. Each line is MATCHED, UNMATCHED, AMBIGUOUS or UNPAID; the reviewer resolves a line by hand (**Match** to an account). **Confirm Matches** opens the payment gate of every matched paid account (READY_FOR_PLACEMENT) through the hourly sweep, and the report becomes CONFIRMED; unpaid accounts stay awaiting payment."
  - "The payment-confirmation port also takes receipts applied in Operations Cashiering (BRD-2), so an account paid at the cashier is released the same way."
preconditions:
  - "The user has BILLING_MANAGE; for CLPC the billing batch exists."
main_flow:
  - "Processing clicks **Upload Payment Report**, chooses the kind (CLPC or reference) and, for CLPC, the batch."
  - "BIBS parses and matches the lines and shows the review."
  - "Processing resolves unmatched or ambiguous lines and clicks **Confirm Matches**."
alternate_flows:
  - "**Discard** a wrong report before confirmation."
rules:
  - [R1, "Match keys - CLPC on PN or loan application number; reference reports on ARN.", Fixed, "-"]
  - [R2, "Each confirmation is applied once per source and reference.", Fixed, "-"]
  - [R3, "Sweep PAYMENT_CONFIRMATION_SWEEP hourly.", Configurable, Job schedule]
validations:
  - [Layout wrong, "The CLPC report needs a 'PN No.' or 'Loan Application No.' column / The payment report needs an 'ARN' or 'Reference' column", PAYMENT_REPORT_LAYOUT]
  - [Empty report, The payment report has no rows, PAYMENT_REPORT_EMPTY]
  - [Bad amount or date, "Row <n>: '<value>' is not an amount / is not a date (yyyy-mm-dd)", PAYMENT_REPORT_VALUE]
  - [Batch missing, Choose the CLPC billing batch the report answers, PAYMENT_REPORT_BATCH_REQUIRED]
  - [Batch closed, "Billing batch <number> is closed", BILLING_BATCH_CLOSED]
  - [Line already matched, "Row <n> is already matched to <ARN>", PAYMENT_LINE_MATCHED]
  - [Account not awaiting payment, "<ARN> is <status>, not awaiting payment", ACCOUNT_NOT_AWAITING_PAYMENT]
  - [Report closed, "Payment report <number> is <status>", PAYMENT_REPORT_CLOSED]
notifications:
  - "Placement owners when accounts enter Ready for placement."
audit:
  - "Report, lines, manual matches and confirmation kept as payment evidence (kind, source, reference)."
acceptance:
  - "A CLPC report with 38 paid and 2 unpaid lines releases the 38 accounts and leaves 2 awaiting payment."
  - "A reference report line with an unknown ARN is listed as UNMATCHED and can be matched by hand."
  - "The Matched and Unmatched Payments report shows each line with its result."
```

```fr
id: FR-NB-092
title: "Apply the payment gate per segment"
brd: [BRNB.068 (p.115), BRD 2.3.1 (p.188), BRNB.114 (p.17)]
actor: "System; Processing"
priority: "Must have"
fit: "CHANGE"
screens: "Account Placement (Payment Gate panel); Placement Workbench (Awaiting payment)"
api: "GET /api/v1/placement/gate/{arn}; POST .../gate/{arn}/client-confirmation; POST .../gate/{arn}/direct-payment; POST .../gate/sweep"
description:
  - "After validation an account waits at the payment gate. The gate rule is chosen by segment and line - CBG Fire and CBG Motor must be paid (a matched payment, FR-NB-091); all other accounts need the client's confirmation to proceed even without payment. Processing records the confirmation with its channel, remarks and an optional supporting document. Direct-payment accounts skip the gate."
  - "Every gate decision is kept as evidence and opens the gate (READY_FOR_PLACEMENT). Placement takes no payment: no receipt or cash entry is made here."
preconditions:
  - "The account is AWAITING_PAYMENT."
main_flow:
  - "Processing opens the account's Payment Gate panel."
  - "For a client-confirmation account, Processing records the confirmation."
  - "BIBS opens the gate and the account moves to Ready for placement."
rules:
  - [R1, "Gate rules - CBG + Property - payment matched; CBG + Motor - payment matched; all others - client confirmation (priority order).", Configurable, Payment gate rules]
  - [R2, "Confirmation channels - E-mail from the client; Signed confirmation form; Recorded call; Others.", Configurable, LOV CLIENT_CONFIRMATION_CHANNEL]
  - [R3, "SLA of AWAITING_PAYMENT 72 hours (default).", Configurable, Workflow stage definitions]
validations:
  - [Confirmation on a paid-only account, "Account <ARN> must be paid: match its payment from a payment report", GATE_REQUIRES_PAYMENT]
  - [Account not awaiting payment, "Account <ARN> is <status>, not awaiting payment", ACCOUNT_NOT_AWAITING_PAYMENT]
fields_screen: "Record client confirmation"
fields:
  - [Confirmation channel, List, "Yes", LOV CLIENT_CONFIRMATION_CHANNEL, Active]
  - [Remarks, Text, "No", "-", "-"]
  - [Supporting document, Attachment, "No", Document type PAYMENT_CONFIRMATION, File rules]
notifications:
  - "Placement owners on stage entry."
audit:
  - "Evidence with kind, source, reference, user and time on the gate panel."
acceptance:
  - "A CBG Motor account cannot pass the gate with a client confirmation."
  - "A CGL account passes the gate on a recorded client confirmation."
  - "A direct-payment account never waits at the gate."
```

## Issuance and e-policy

```fr
id: FR-NB-100
title: "Receive e-policies and store them with the account"
brd: [BRNB.073 (p.116-117)]
actor: "Processing (EPOLICY_MANAGE); System"
priority: "Must have"
fit: "NEW"
screens: "E-policy Upload (single, bulk); Issuance Workbench (Placed - awaiting policy)"
api: "POST /api/v1/issuance/epolicies; POST /api/v1/issuance/epolicy-uploads; POST .../epolicy-uploads/{id}/items/{itemId}; POST .../epolicy-uploads/{id}/confirm | discard"
description:
  - "Processing uploads the insurer's e-policy PDF, singly or in a bulk of up to 50 files. BIBS matches each file to its account by ARN, policy number or PN number found in the file name, or the user chooses the account. The file is stored as document type EPOLICY on the account (the designated folder), and the e-policy enters review (FR-NB-101). In a bulk upload, the user reviews the matches, fixes or skips items and confirms."
  - "**Built behaviour:** e-policies are received by upload. Reading an insurer mailbox or SFTP folder automatically waits for Q31 (Q12)."
preconditions:
  - "The account is placed (PLACED, or later for a replacement)."
main_flow:
  - "Processing opens E-policy Upload and selects Single or Bulk."
  - "Processing uploads the PDF(s) and, for a single file, the ARN."
  - "BIBS matches the file(s) and opens the extraction review."
alternate_flows:
  - "No account found. BIBS asks the user to choose the ARN."
rules:
  - [R1, "E-policies are PDF.", Fixed, "-"]
  - [R2, "Up to 50 files per bulk upload.", Fixed, "-"]
  - [R3, "E-policy statuses - RECEIVED, REVIEW, CONFIRMED, REJECTED.", Fixed, "-"]
validations:
  - [No account for the file, "No account found for <file>: choose the account (ARN)", EPOLICY_ACCOUNT_NOT_FOUND]
  - [Account not placed, "Account <ARN> is <status>; an e-policy is received once placed", EPOLICY_ACCOUNT_STATUS]
  - [Too many files, Upload between 1 and 50 e-policies at a time, EPOLICY_FILE_COUNT]
  - [Upload closed, "This upload is <status>", EPOLICY_UPLOAD_CLOSED]
fields_screen: "E-policy Upload (single)"
fields:
  - [E-policy PDF, File, "Yes", "-", PDF]
  - [Account (ARN), Look-up, "No", Accounts, Placed account]
  - [Policy number, Text, "No", "-", Used for matching]
notifications:
  - "None; the e-policy appears under Policy received - review."
audit:
  - "Upload, match and confirmation with user, time and file hash."
acceptance:
  - "A bulk upload of 10 e-policies named by ARN matches all 10 to their accounts."
  - "An e-policy for an account awaiting payment is refused."
```

```fr
id: FR-NB-101
title: "Extract policy details and update the policy number"
brd: [BRNB.074 (p.117), BRNB.104 (p.11)]
actor: "System; Processing (confirm)"
priority: "Must have"
fit: "NEW"
screens: "Extraction Review; Account page (Policy tab)"
api: "POST /api/v1/issuance/epolicies/{id}/extract; POST .../confirm; POST .../reject"
description:
  - "BIBS reads the text of the e-policy PDF and applies the extraction patterns of the insurer (default patterns when the insurer has none) to find the policy number, the period from and to, and the premium. The Extraction Review shows the extracted values next to the account's values. The user corrects them if needed and clicks **Confirm Policy**; BIBS then updates the policy number and issue date of the account, links the e-policy, and moves the account to POLICY_ISSUED. A multi-year account takes one policy number per year."
  - "The user can **Reject** the e-policy with a reason; the file is kept."
  - "**Built behaviour:** extraction reads text PDFs only; OCR of scanned documents waits for Q24 (a parked port)."
preconditions:
  - "An e-policy is in REVIEW."
main_flow:
  - "BIBS extracts the values when the e-policy is received (or the user clicks **Extract Again**)."
  - "The user checks the values and the issue date and clicks **Confirm Policy**."
  - "BIBS updates the account (POLICY_ISSUED) and logs the source document."
rules:
  - [R1, "User confirmation is required before the account is updated.", Fixed, "-"]
  - [R2, "Extraction patterns per insurer (regular expressions).", Configurable, Extraction patterns]
  - [R3, "Reject reasons - Belongs to another account; Not the issued policy; Details differ from the placement; Unreadable or damaged file; Others.", Configurable, LOV EPOLICY_REJECT_REASON]
validations:
  - [Already reviewed, "The e-policy <file> of <ARN> is already <status>", EPOLICY_REVIEWED]
  - [Wrong number of policy numbers, "Account <ARN> needs <n> policy number(s), one per year", POLICY_NUMBERS_MISMATCH]
  - [Insurer differs, "Account <ARN> is set up with insurer <code>", PLACEMENT_INSURER_MISMATCH]
fields_screen: "Confirm Policy"
fields:
  - [Policy number(s), Text, "Yes", Extracted, One per policy year]
  - [Issue date, Date, "Yes", "-", "-"]
  - [Period / Premium, Display, "-", Extracted and account values, Differences highlighted]
notifications:
  - "Booking owners on POLICY_ISSUED; auto-booking may queue the account (FR-NB-112)."
audit:
  - "The update records the source document, the extracted and confirmed values, user and time."
acceptance:
  - "A text e-policy fills the policy number in the review; after confirmation the account shows it and is Policy issued."
  - "A 3-year account cannot be confirmed with one policy number."
```

```fr
id: FR-NB-102
title: "Trigger processes from uploaded documents"
brd: [BRNB.105 (p.11-12)]
actor: "System"
priority: "Must have"
fit: "NEW"
screens: "Issuance Workbench; document triggers (read-only list)"
api: "GET /api/v1/issuance/triggers"
description: "BIBS evaluates each uploaded document against the document-trigger rules, which map a document type to an action. The delivered rule maps EPOLICY to the extraction review, which leads to the policy number update (FR-NB-101); policy issuance then triggers the Insurance Advice (FR-NB-103) and, where a rule matches, auto-booking (FR-NB-112). Each triggered process is linked to the document and the account, and logged with the document reference, the process and the time."
preconditions:
  - "None."
main_flow:
  - "A document is uploaded."
  - "BIBS finds the trigger of its type and starts the action."
rules:
  - [R1, "Delivered trigger - EPOLICY -> EXTRACTION_REVIEW.", Configurable, Document triggers (further documents Q24)]
validations: []
notifications:
  - "As the triggered process."
audit:
  - "Trigger events with document reference, process and time."
acceptance:
  - "Uploading an EPOLICY document to a placed account opens an extraction review task."
```

```fr
id: FR-NB-103
title: "Generate the Insurance Advice for mortgaged accounts"
brd: [BRNB.070 (p.116), BRNB.095 (p.6)]
actor: "System; Processing"
priority: "Must have"
fit: "NEW"
screens: "Insurance Advice (Generate Insurance Advice); Issuance Workbench (IA to generate)"
api: "POST /api/v1/issuance/insurance-advice/generate"
description:
  - "An Insurance Advice IA-yyyy-nnnnnn is generated from the INSURANCE_ADVICE template for accounts with a mortgagee bank only. It is generated automatically on the lifecycle event chosen by the parameter IA_TRIGGER - at policy issuance (default), at placement, or never (manual) - and Processing can generate one or several at a time by entering or selecting the ARNs."
  - "Generation is logged; an advice is never generated for an account without a mortgagee or before placement."
preconditions:
  - "The account has a mortgagee bank and is placed or later."
main_flow:
  - "The trigger event occurs (for example policy issued), or Processing clicks **Generate Insurance Advice** with the ARNs."
  - "BIBS generates the advice (GENERATED) and lists it in the register."
rules:
  - [R1, "Trigger - ON_POLICY_ISSUE (default), ON_PLACEMENT or MANUAL.", Configurable, "Parameter IA_TRIGGER (Q30)"]
  - [R2, "Only mortgaged accounts get an advice.", Fixed, "-"]
validations:
  - [No mortgagee, "Account <ARN> has no mortgagee bank: no Insurance Advice", IA_NOT_MORTGAGED]
  - [Not placed, "Account <ARN> is <status>; it must be placed first", IA_ACCOUNT_STATUS]
  - [Too many selected, "Select between 1 and 200 records", ISSUANCE_SELECTION]
notifications:
  - "None; the advice appears under IA to generate / the register."
audit:
  - "Generation with the trigger or user, template version and time."
acceptance:
  - "A mortgaged account gets its advice automatically when its policy is confirmed."
  - "An account without mortgagee is refused with IA_NOT_MORTGAGED."
```

```fr
id: FR-NB-104
title: "Access, download and send Insurance Advices"
brd: [BRNB.060 (p.107-108), BRNB.035 (p.75-76)]
actor: "Marketing; E-policy Sender"
priority: "Must have"
fit: "NEW"
screens: "Insurance Advice register"
api: "GET /api/v1/issuance/insurance-advice; GET .../{id}/file; POST .../insurance-advice/send"
description: "The register lists the generated advices with IA number, insured / proposal number, mortgagee, policy number, generation date, status and last sending, searchable and filterable. Users view an advice, download it as PDF (the browser saves it to the chosen folder), select several and send them to the intended recipients. Each advice is encrypted and its password sent separately (FR-NB-013)."
preconditions:
  - "The user has ACCOUNT_VIEW (view); EPOLICY_SEND (send)."
main_flow:
  - "The user searches the register and selects one or more advices."
  - "The user clicks **Send Selected**, enters To, Cc and the password hint, and clicks **Send via Email**."
  - "BIBS sends each advice protected and marks it SENT."
rules:
  - [R1, "Up to 50 advices per sending.", Fixed, "-"]
validations:
  - [Selection size, "Select between 1 and 50 Insurance Advices", IA_SELECTION]
  - [No recipient, Enter at least one recipient, EMAIL_RECIPIENT_REQUIRED]
notifications:
  - "The recipients receive the advice and the password separately."
audit:
  - "Sending logged per advice; included in the dispatch report (FR-NB-106)."
acceptance:
  - "A user searches by policy number and downloads the advice PDF."
  - "Three advices sent together arrive protected, each followed by its password e-mail."
```

```fr
id: FR-NB-105
title: "Send e-policies to clients"
brd: [BRNB.077 (p.118-119), BRNB.035 (p.75-76)]
actor: "E-policy Sender"
priority: "Must have"
fit: "NEW"
screens: "E-policy Dispatch (Ready to Dispatch, Send E-policies); Issuance Workbench (Ready to dispatch)"
api: "GET /api/v1/issuance/dispatch/{epolicyId}/draft; POST /api/v1/issuance/dispatch/{epolicyId}; POST /api/v1/issuance/dispatch/batch"
description: "The E-policy Sender sends confirmed e-policies to the clients, one or a batch. BIBS uses the account contact e-mail (FR-NB-066), the EPOLICY_EMAIL template with the password hint that tells the client how the password is built, attaches the encrypted e-policy, and sends the password in a separate e-mail."
preconditions:
  - "The e-policy is CONFIRMED; the user has EPOLICY_SEND."
main_flow:
  - "The sender opens Ready to Dispatch and selects the e-policies."
  - "The sender clicks **Send Selected** (or **Send** on one), checks the recipients and the password hint."
  - "BIBS sends the protected e-mails and records the outcome."
alternate_flows:
  - "Account without contact e-mail. The sender enters the recipient."
rules:
  - [R1, "Password hint text.", Configurable, Parameter EPOLICY_PASSWORD_HINT (convention Q07)]
  - [R2, "Up to 200 e-policies per batch.", Fixed, "-"]
validations:
  - [Policy data not confirmed, "Confirm the policy data of <ARN> before sending the e-policy", EPOLICY_NOT_CONFIRMED]
  - [No recipient, "Account <ARN> has no contact e-mail: enter the recipient", EPOLICY_NO_RECIPIENT]
notifications:
  - "The client receives the e-policy and the password separately."
audit:
  - "Each e-mail with recipients, time, attachment hash and outcome."
acceptance:
  - "A batch of 20 e-policies is sent, each protected, and the dispatch report lists 20 sent."
  - "An e-policy still in review cannot be sent."
```

```fr
id: FR-NB-106
title: "Report sent and failed e-policies"
brd: [BRNB.078 (p.119)]
actor: "E-policy Sender; Processing"
priority: "Must have"
fit: "NEW"
screens: "E-policy Dispatch (Dispatch Report); Report NB-DISPATCH"
api: "GET /api/v1/issuance/dispatch/log; report NB-DISPATCH"
description: "The Insurance Advice and E-policy Dispatch report lists, for a date range, the e-policies and advices sent and not sent, grouped by outcome (sent, failed, queued), with counts and the reason of each failure (from the messaging log)."
preconditions:
  - "The user has ACCOUNT_VIEW."
main_flow:
  - "The user selects the date range, the document (all, e-policy, Insurance Advice) and the outcome."
  - "BIBS shows the grouped list with counts and exports it."
rules:
  - [R1, "Outcome from the messaging log of purposes EPOLICY and INSURANCE_ADVICE.", Fixed, "-"]
validations: []
fields_screen: "NB-DISPATCH columns"
fields:
  - [Document / Reference, Text, "-", E-policy or advice; ARN, "-"]
  - [Queued On / Sent On, Date, "-", Messaging log, "-"]
  - [Recipients / Subject, Text, "-", Messaging log, "-"]
  - [Reason, Text, "-", Failure reason, "-"]
  - [Simulated / Sent By / Attempts, Text / Number, "-", Messaging log, "-"]
notifications:
  - "None."
audit:
  - "Report runs and exports logged."
acceptance:
  - "A failed dispatch appears under Failed with its reason."
  - "The counts per outcome match the lists."
```

## Booking

```fr
id: FR-NB-110
title: "Book an account with its GL entry and service invoice"
brd: [BRNB.027 (p.66-67), BRNB.061 (p.108-109), BRNB.108 (p.13-14)]
actor: "Processing (BOOKING_PROCESS); System"
priority: "Must have"
fit: "CHANGE"
screens: "Booking Workbench (Ready to Book); Pre-booking Confirmation (/booking/book/<ARN>); Booked Invoice"
api: "POST /api/v1/booking/preview; POST /api/v1/booking/book"
description:
  - "Processing books an account whose policy is issued (or that is flagged for direct booking). The Pre-booking Confirmation shows the invoice(s) with the premium breakdown and commission, and the journal preview. On **Book Account** BIBS, in one transaction - checks the account, the cost center and the insurer shares; builds the invoice BI-<branch>-yyyy-n on the booking date; posts the BROKER_BOOKING accounting event per insurer share (premium receivable from the client unless direct payment, premium payable to the insurer, commission receivable, VAT on commission, withholding tax); opens the open items; issues the service invoice when a type is triggered on booking (FR-NB-117); and moves the account to BOOKED."
  - "If any step fails, nothing is kept and the error is shown. Booking twice returns the invoice already booked."
preconditions:
  - "The account is POLICY_ISSUED; the user has BOOKING_PROCESS."
main_flow:
  - "Processing opens the account from Ready to Book."
  - "Processing checks the booking date and cost center and clicks **Update Preview**."
  - "Processing clicks **Book Account**; BIBS shows the booked invoice."
rules:
  - [R1, "Booking, GL entry and service invoice are one transaction.", Fixed, "-"]
  - [R2, "The booking date cannot be in the future.", Fixed, "-"]
  - [R3, "Commission realisation ON_COLLECTION (default) or ON_BOOKING.", Configurable, Parameter OPS_COMMISSION_REALIZATION]
  - [R4, "Withholding tax on commission 10% (default).", Configurable, Parameter BOOKING_WTAX_RATE]
  - [R5, "GL accounts of the event are set up in Accounting Rules; production values wait for OQ07.", Configurable, Accounting rules]
validations:
  - [Account not issued, "Account <ARN> is <status>: only issued policies are booked", ACCOUNT_NOT_BOOKABLE]
  - [Already booked, "Account <ARN> is already booked (BRNB.076)", DUPLICATE_BOOKING]
  - [Future booking date, "The booking date <date> is in the future", BOOKING_DATE_FUTURE]
  - [Cost center missing, "Enter the cost center of account <ARN> (BRNB.108)", COST_CENTER_REQUIRED]
  - [No insurer / period / premium, "Account <ARN> has no insurer / has no period of cover / has no rated premium", "ACCOUNT_INSURER_MISSING, ACCOUNT_PERIOD_MISSING, PREMIUM_NOT_RATED"]
  - [Company has no branch, The company has no branch to book invoices, BOOKING_BRANCH_MISSING]
fields_screen: "Pre-booking Confirmation"
fields:
  - [Booking date, Date, "Yes", Default today, Not in the future]
  - [Cost center, List, "Yes", Sales organisation (default from the account officer), Active cost center]
notifications:
  - "None; a failure is shown on screen."
audit:
  - "Invoice, journal and service invoice are linked to the ARN and recorded with user and time."
acceptance:
  - "Booking an issued account creates the invoice, the balanced journal and the service invoice, and the account shows Booked."
  - "If the journal cannot be posted, the account stays Policy issued and no invoice exists."
  - "Booking the same account twice returns the existing invoice."
```

```fr
id: FR-NB-111
title: "Book individually or in batch, on schedule or by upload"
brd: [BRNB.036 (p.77-78), BRNB.061 (p.108-109)]
actor: "Processing"
priority: "Must have"
fit: "CHANGE"
screens: "Booking Workbench (Ready to Book, Queued for Batch, Booked Today, Failed); Batch Runs; Upload Bookings"
api: "GET /api/v1/booking/workbench; POST .../queue; PUT .../queue/{id}; POST .../queue/{id}/remove; POST .../batch/confirm | batch/cancel | book-now; GET .../batch-runs"
description:
  - "Individual booking books one account at a time (FR-NB-110). For batch booking, Processing selects accounts and clicks **Add to Batch**; the Placement Workbench action For Booking hands accounts over the same way. Before the batch runs, Processing edits the booking date or cost center of an entry, removes an entry, cancels the batch or confirms the selected entries. The batch runs at the scheduled time (default 20:00 PHT) or at once with **Book Now**. Each account is booked in its own transaction; failures go to the Failed tab with the reason and do not stop the others."
  - "Upload Bookings (BOOKING_UPLOAD) takes a list of ARNs with booking date and cost center; each row is validated and booked. Booking uploads are separate from the account creation uploads."
preconditions:
  - "The accounts are POLICY_ISSUED."
main_flow:
  - "Processing selects accounts on Ready to Book and clicks **Add to Batch**."
  - "Processing reviews Queued for Batch, edits or removes entries and clicks **Confirm Batch**."
  - "The batch runs; the Batch Run shows booked and failed accounts with invoice numbers or reasons."
rules:
  - [R1, "One active queue entry per ARN.", Fixed, "-"]
  - [R2, "Batch schedule daily 20:00 PHT (default).", Configurable, Job BOOKING_BATCH schedule]
  - [R3, "Batch runs are numbered BB-<yyyy>-n with one result row per account.", Fixed, "-"]
validations:
  - [Nothing selected or queued, "Select at least one account to book / No queued account to book", BATCH_EMPTY]
  - [Future booking date, "The booking date <date> is in the future", BOOKING_DATE_FUTURE]
  - [Entry no longer queued, "Account <ARN> is no longer queued (<status>)", QUEUE_ENTRY_CLOSED]
notifications:
  - "Failed entries are visible on the Failed tab."
audit:
  - "Each batch run with its rows, user and time."
acceptance:
  - "Ten accounts queued and confirmed are booked by the evening run; one without cost center fails and the others are booked."
  - "An entry removed from the queue is not booked."
  - "An upload of 50 ARNs books the valid rows and lists the failures."
```

```fr
id: FR-NB-112
title: "Book automatically on defined criteria and never twice"
brd: [BRNB.076 (p.118)]
actor: "System"
priority: "Must have"
fit: "CHANGE"
screens: "Booking Setup (Auto-book Rules)"
api: "GET | POST /api/v1/booking/setup/auto-book-rules; PUT .../{id}"
description:
  - "When a policy is issued, BIBS checks the auto-book rules (product, market segment, enabled). When a rule matches, the account is queued with source AUTO and booked by the next batch run (FR-NB-111)."
  - "Duplicate booking is prevented by the booking key ARN + transaction number (NB, NB-Y2 ... for policy years, then endorsement numbers), which is unique in the database. Endorsements are separate transactions on the same ARN, so they are not duplicates."
  - "A non-financial endorsement (no premium change) is recorded with its EN number and description but books no invoice and posts no journal (FR-NB-115)."
preconditions:
  - "Auto-book rules are set up (none are delivered for production)."
main_flow:
  - "A policy is issued."
  - "BIBS evaluates the rules and queues the matching account."
rules:
  - [R1, "Auto-book rule - product and / or market segment; blank = any.", Configurable, Booking Setup (MASTER_MAINTAIN)]
  - [R2, "Booking key ARN + transaction number is unique.", Fixed, "-"]
validations:
  - [Second booking of the same transaction, "Account <ARN> is already booked (BRNB.076)", DUPLICATE_BOOKING]
fields_screen: "Auto-book rule"
fields:
  - [Product, List, "No", Products, Blank = any]
  - [Market segment, List, "No", LOV MARKET_SEGMENT, Blank = any]
  - [Description, Text, "Yes", "-", "-"]
  - [Enabled, Check box, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Rule changes audited; queued entries show source AUTO."
acceptance:
  - "With a rule for product CTP01, a CTP01 account is queued automatically when its policy is confirmed."
  - "A second booking call for the same ARN does not create a second invoice."
```

```fr
id: FR-NB-113
title: "Book directly when the policy is already issued"
brd: [BRNB.038 (p.79-80), BRNB.111 (p.15)]
actor: "Marketing; Processing"
priority: "Must have"
fit: "NEW"
screens: "Account page (Direct booking); Booking Workbench"
api: "POST /api/v1/accounts/{id}/direct-booking"
description:
  - "When the insurer has already issued the policy to the client, the account bypasses placement. From SUBMITTED or READY_FOR_PLACEMENT, the user clicks **Direct booking (policy already issued)**. BIBS requires at least one issued policy document on the account (policy copy or e-policy), moves the account to POLICY_ISSUED without placement data, and the account is booked like any other (FR-NB-110, individually, in batch or by upload). Documents stay linked to the booked account."
  - "Direct bookings are listed in the Booked Accounts Register and the Account Status Report, filterable by date, status and user."
preconditions:
  - "The user has ACCOUNT_PROCESS or PLACEMENT_MANAGE."
main_flow:
  - "The user uploads the issued policy as POLICY_COPY or EPOLICY."
  - "The user clicks **Direct booking**; BIBS checks the document and moves the account."
  - "Processing books the account."
rules:
  - [R1, "At least one POLICY_COPY or EPOLICY document is mandatory.", Fixed, "-"]
  - [R2, "Duplicate rules and booking key apply as for placed accounts.", Fixed, "-"]
validations:
  - [No policy document, "Attach the issued policy (policy copy or e-policy) before direct booking", POLICY_DOCUMENT_REQUIRED]
notifications:
  - "Booking owners on POLICY_ISSUED."
audit:
  - "The direct booking action and the document are in the account history."
acceptance:
  - "An account without a policy copy cannot be directly booked."
  - "A directly booked account has no placement slip and is booked with its invoice."
```

```fr
id: FR-NB-114
title: "Book multi-year policies"
brd: [BRNB.112 (p.15-16)]
actor: "Processing; System"
priority: "Must have"
fit: "CHANGE"
screens: "Booked Invoice (Invoices of the Account); Pre-booking Confirmation"
api: "POST /api/v1/booking/book; GET /api/v1/booking/accounts/{arn}/invoices"
description: "A multi-year account (for example a 5-year term) is identified as multi-year on the account and carries one policy number per year (FR-NB-101), all linked to its ARN. Booking creates one invoice per policy year - year 1 is booked at once, years 2 to n are stored SCHEDULED and booked by the batch job when their year starts. Each year has its own premium, commission and policy number, and the history shows every year."
preconditions:
  - "The product allows multi-year terms; the account has a term of 2 years or more and one policy number per year."
main_flow:
  - "Processing books the account; BIBS books year 1 and schedules the other years."
  - "The batch job books each scheduled year when it becomes due."
rules:
  - [R1, "One invoice per policy year, transaction numbers NB, NB-Y2 ... NB-Yn.", Fixed, "-"]
  - [R2, "Premium and commission basis of multi-year (upfront or yearly) waits for Q35.", Configurable, "-"]
validations:
  - [Year not due, "Policy year <n> of <ARN> is not due for booking", POLICY_YEAR_NOT_DUE]
  - [Policy numbers missing, "Account <ARN> needs <n> policy number(s), one per year", POLICY_NUMBERS_MISMATCH]
notifications:
  - "None."
audit:
  - "Each yearly invoice with its policy number and booking date."
acceptance:
  - "A 5-year account shows five invoices with five policy numbers, year 1 booked and years 2-5 scheduled."
  - "Year 2 is booked by the batch run on its start date."
```

```fr
id: FR-NB-115
title: "Post financial and non-financial endorsements"
brd: [BRNB.061 (p.108-109), BRNB.027 (p.66-67), BRNB.076 (p.118), BRNB.081 (p.120)]
actor: "Processing (positive, non-financial); Adjustment (negative)"
priority: "Must have"
fit: "CHANGE"
screens: "Endorsements; New Endorsement"
api: "POST /api/v1/booking/endorsements/preview; POST /api/v1/booking/endorsements; GET /api/v1/booking/endorsements"
description:
  - "An endorsement is recorded against a booked account with a number EN-yyyy-n. A financial endorsement - positive (additional premium) or negative (return premium) - is booked as an invoice with its effective date, booking date, description and the change of sum insured and premium. The premium is computed on the remaining term (pro-rata days or the short-period table) or entered, and the minimum premium does not apply. The preview shows the invoice and journal before posting. The posting uses the same accounting event as booking, with negative amounts for returns, and issues or credits the service invoice when an endorsement type is set up."
  - "A non-financial endorsement (for example a change of address or plate number) is recorded with its description only: no invoice and no journal. As the BRD splits the roles, Processing posts positive and non-financial endorsements (BOOKING_PROCESS) and Adjustment posts negative endorsements and cancellations (BOOKING_ADJUST). Endorsement requests with approval are the Adjustment module of BRD-2 Operations."
preconditions:
  - "The account is BOOKED."
main_flow:
  - "The user opens New Endorsement and enters the ARN, type, dates, description and change."
  - "The user checks the journal preview."
  - "The user clicks **Post Endorsement**; BIBS checks the permission of the type."
rules:
  - [R1, "Positive endorsements add premium; negative endorsements return premium.", Fixed, "-"]
  - [R2, "The return cannot exceed the premium in force of the policy year.", Fixed, "-"]
  - [R3, "Short-period percentages from the rate table.", Configurable, Rates & Taxes]
validations:
  - [Mandatory data missing, "Enter the account, type, effective date and description", ENDORSEMENT_INCOMPLETE]
  - [No change entered, Enter the change of the sum insured or the premium, ENDORSEMENT_CHANGE_REQUIRED]
  - [Sign does not match the type, "A positive endorsement must add premium / A negative endorsement or cancellation must return premium", ENDORSEMENT_SIGN_MISMATCH]
  - [Return too high, "The return premium exceeds the premium in force of policy year <n>", RETURN_EXCEEDS_PREMIUM]
  - [Effective date outside the term, "The effective date <date> is not within a booked policy year of <ARN>", ENDORSEMENT_DATE_OUTSIDE_TERM]
  - [Account not booked, "Account <ARN> is <status>: only booked accounts are endorsed", ACCOUNT_NOT_BOOKED]
fields_screen: "New Endorsement"
fields:
  - [ARN, Look-up, "Yes", Booked accounts, "-"]
  - [Type, Option, "Yes", "Positive / Negative / Non-financial", "Positive and non-financial - BOOKING_PROCESS; negative - BOOKING_ADJUST"]
  - [Effective date / Booking date, Date, "Yes", "-", "Effective within a booked year; booking not in the future"]
  - [Premium basis, Option, Cond., "Pro-rata / Short period", Financial endorsements]
  - [Sum insured change / Premium rate (%), Number, Cond., "-", "-"]
  - [Description, Text, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Endorsement invoice, journal and service invoice linked to the ARN and the original invoice."
acceptance:
  - "A positive endorsement of +500,000 sum insured on a Fire account posts an EN invoice with additional premium on the remaining days."
  - "A negative endorsement larger than the premium in force is refused."
```

```fr
id: FR-NB-116
title: "Cancel booked accounts"
brd: [BRNB.081 (p.120), BRNB.094 (p.5-6)]
actor: "Adjustment"
priority: "Must have"
fit: "CHANGE"
screens: "Booked Invoice (Cancel Booking dialog)"
api: "POST /api/v1/booking/endorsements (kind CANCELLATION)"
description: "Adjustment cancels a booked account after issuance - the true policy cancellation of BRNB.094. The cancellation is posted as a CANCELLATION invoice with a return basis - FLAT (reverses everything), FLAT_RETAIN_DST (keeps the documentary stamp tax) or PARTIAL (pro-rata on the unexpired days, or an amount entered) - a cancellation date, a reason and a description. BIBS reverses the commission and receivable entries, credits the service invoice, and moves the account to CANCELLED."
preconditions:
  - "The account is BOOKED; the user has BOOKING_ADJUST."
main_flow:
  - "Adjustment opens the booked invoice and clicks **Cancel Booking**."
  - "Adjustment chooses the return basis, date and reason and confirms."
  - "BIBS posts the cancellation and shows the account as Cancelled."
rules:
  - [R1, "Return bases - FLAT, FLAT_RETAIN_DST, PARTIAL.", Fixed, "-"]
  - [R2, "Reasons from CANCELLATION_REASON.", Configurable, LOV CANCELLATION_REASON]
validations:
  - [Kind on a non-cancellation, "A cancellation, and only a cancellation, has a kind", CANCELLATION_KIND_REQUIRED]
  - [Short-period rate missing, "No short-period rate is set up for <n> month(s)", SHORT_PERIOD_RATE_MISSING]
  - [Account not booked, "Account <ARN> is <status>: only booked accounts are endorsed", ACCOUNT_NOT_BOOKED]
fields_screen: "Cancel Booking"
fields:
  - [Return basis, Option, "Yes", "Flat / Flat, retain DST / Partial", "-"]
  - [Cancellation date, Date, "Yes", "-", Within the booked term]
  - [Reason, List, "Yes", LOV CANCELLATION_REASON, "-"]
  - [Description, Text, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Cancellation invoice, reversal journal and credit linked to the original invoice."
acceptance:
  - "A flat cancellation reverses the whole premium and commission and the account shows Cancelled."
  - "A partial cancellation returns the premium of the unexpired days."
```

```fr
id: FR-NB-117
title: "Generate and send service invoices"
brd: [BRNB.100 (p.8-9), BRNB.100b (p.9)]
actor: "Processing; System"
priority: "Must have"
fit: "CHANGE"
screens: "Service Invoices; Service Invoice page; Booking Setup (Service Invoice Types)"
api: "GET /api/v1/booking/service-invoices; POST .../service-invoices; POST .../{id}/resend; POST .../{id}/credit; GET .../{id}/pdf"
description:
  - "A service invoice SI-<branch>-yyyy-n is generated from its type - template, trigger, recipient and owner. Delivered types - INSURER_COMMISSION (commission invoice to the insurer, on booking, owner BOOKING_PROCESS), INSURER_COMMISSION_ENDT (on endorsement, owner BOOKING_ADJUST) and INTERNAL (manual). The invoice captures the details of its type (recipient, commission, VAT, withholding tax, net amount), stores the PDF with the template version, and shows its owner."
  - "The invoice is e-mailed to the insurer's billing address. The owner is notified when a dispatch fails, with the reason; **Send via Email** resends it. A credit is issued against an invoice, never above what is open."
preconditions:
  - "The invoice type is active."
main_flow:
  - "Booking or an endorsement triggers the type; BIBS issues and sends the invoice."
  - "The owner checks the dispatch status on Service Invoices."
alternate_flows:
  - "Dispatch failure. The owner is notified and resends."
  - "**Credit** with the commission and VAT to credit and a reason."
rules:
  - [R1, "Types, triggers (ON_BOOKING, ON_ENDORSEMENT, MANUAL), recipient and owner are set up per type.", Configurable, Booking Setup (Service Invoice Types)]
  - [R2, "BIR CAS / e-invoicing transmission is not built; numbering, PDF and dispatch are local.", Fixed, "-"]
validations:
  - [Type inactive, "Service invoice type <code> is inactive", SERVICE_INVOICE_TYPE_INACTIVE]
  - [Credit above open amount, "Service invoice <number> has <commission> commission and <vat> VAT left to credit", CREDIT_EXCEEDS_INVOICE]
  - [Credit of a credit, "<number> is itself a credit", CREDIT_OF_CREDIT]
fields_screen: "Service invoice type"
fields:
  - [Code / Name, Text, "Yes", "-", Unique code]
  - [Recipient, List, "Yes", "Insurer / Internal", "-"]
  - [Trigger, List, "Yes", "On booking / On endorsement / Manual", "-"]
  - [Owner team (permission) / Owner user, List, "Yes (one)", Permissions; users, "-"]
  - [Document template, List, "Yes", Document templates, Active]
notifications:
  - "The owner on a failed dispatch, with the reason."
audit:
  - "Issue, dispatch outcome, resend and credit with user and time; register NB-SI-REG."
acceptance:
  - "Booking an account issues an INSURER_COMMISSION invoice and e-mails it to the insurer."
  - "A failed dispatch notifies the owner and appears as FAILED with its reason in the register."
```

```fr
id: FR-NB-118
title: "Mark booked transactions with an incentive eligibility indicator"
brd: [BRNB.107 (p.12-13)]
actor: "System"
priority: "Must have"
fit: "NEW"
screens: "Booked Invoice; Booked Accounts Register; Incentive Criteria (BRD-3)"
api: "IncentiveCriteriaService (internal); GET /api/v1/catalog/incentive-criteria"
description:
  - "At booking, BIBS evaluates the active incentive criteria against the invoice facts (product, cover type, segment, channel, lead insurer, booking date), stores the matched criteria codes on the invoice and sets the incentive indicator when at least one criterion matches. The indicator is rule-based and cannot be changed by hand; it is visible on the invoice and in the Booked Accounts Register, and endorsements inherit it from the original invoice."
  - "**Built behaviour:** the booking incentive rules first built for BRD-1 are frozen; the criteria are maintained in Product Maintenance > Incentive Criteria (BRD-3, PMADD07 and 08), which took over this set-up. The criteria content waits for Q33; computation and payout are out of scope."
preconditions:
  - "Active incentive criteria exist."
main_flow:
  - "An account is booked."
  - "BIBS matches the criteria and stores the codes and the indicator."
rules:
  - [R1, "Indicator = at least one matching active criterion.", Fixed, "-"]
  - [R2, "Criteria are maintained with maker-checker in BRD-3.", Configurable, Incentive Criteria]
validations:
  - [Change of a frozen booking rule, "Incentive rules are maintained in Product Maintenance > Incentive Criteria", INCENTIVE_RULES_FROZEN]
notifications:
  - "None."
audit:
  - "Matched codes and the rule decision stored with the invoice."
acceptance:
  - "An invoice on a product covered by an active criterion shows Incentive = Yes and the criterion code."
  - "The indicator cannot be edited on the invoice."
```

```fr
id: FR-NB-119
title: "Capture the cost center and business type of every booking"
brd: [BRNB.108 (p.13-14), BRNB.097 (p.6-7)]
actor: "System; Processing"
priority: "Must have"
fit: "CONFIGURE"
screens: "Pre-booking Confirmation; Booking Workbench (Edit); Booked Invoice; Booked Accounts Register"
api: "POST /api/v1/booking/book; PUT /api/v1/booking/queue/{id}"
description:
  - "Every booked transaction carries a cost center from the standard sales organisation master (region, department, team; the cost center is inherited by the team). It defaults from the account officer's team, stamped on the account at creation, and can be changed before booking. Booking is refused without it. The cost center is posted as a dimension on the journal and shown on the invoice, in reports and in the Operations feed."
  - "Each booking also carries the business type (New Business or Renewal) on the accounting event and in the Booked Accounts Register."
  - "**Built behaviour:** the invoice is stamped New Business by the NB booking path. Taking the type from the account (work item BT0, decision D1 of R5) is planned with BRD-6 and is not yet built."
preconditions:
  - "The sales organisation and cost centers are set up."
main_flow:
  - "BIBS proposes the cost center of the account."
  - "Processing keeps or changes it and books."
rules:
  - [R1, "Cost center mandatory on every booking.", Fixed, "-"]
  - [R2, "Cost center source and default rule wait for Q34; the sales organisation holds seed units.", Configurable, Sales Organisation]
validations:
  - [Cost center missing, "Enter the cost center of account <ARN> (BRNB.108)", COST_CENTER_REQUIRED]
notifications:
  - "None."
audit:
  - "Capture and changes of the cost center recorded on the queue entry and the invoice."
acceptance:
  - "A booking without cost center is refused with COST_CENTER_REQUIRED."
  - "The Booked Accounts Register shows the cost center and the NB / Renewal column of each invoice."
```

## Reports and dashboard

```fr
id: FR-NB-120
title: "New Business dashboard"
brd: [BRNB.012 (p.55), BRNB.115 (p.18-19)]
actor: "Marketing; TSU; Processing; TLs and approvers"
priority: "Must have"
fit: "CHANGE"
screens: "NB Dashboard (landing page of the broking roles)"
api: "GET /api/v1/nb/dashboard?companyId=&asOf="
description:
  - "The NB Dashboard shows, as of a date - new requests; quotations sent this month; overdue work items (SLA breaches) per workflow; bookings of the month (count, premium, commission); requests, quotations and PRFs by status; accounts by stage; the year-to-date funnel quotation, sent, accepted, accounts, placed, policy issued, booked; ageing of open accounts by stage (under 1, 1-3, 3-7, over 7 days, overdue); production against target per team for the month."
  - "Every tile and bar opens its filtered list (accounts by status, quotations by tab, booked invoices, the Account Status Report), where the user filters further and exports to Excel. Data is read live from the workflow and booking tables and follows the user's access."
preconditions:
  - "The user has WORK_VIEW."
main_flow:
  - "The user signs in; the dashboard opens."
  - "The user clicks a tile or bar; the filtered list opens."
  - "The user exports the list."
rules:
  - [R1, "Overdue = past the stage SLA; stalled = no movement for NB_STALLED_DAYS.", Configurable, "Stage SLA; parameter NB_STALLED_DAYS"]
  - [R2, "Comparative tables are reached from the PRF list (FR-NB-055).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Exports from the lists are logged."
acceptance:
  - "An account submitted a minute ago is counted in Accounts by Stage when the dashboard is refreshed."
  - "Clicking the Placed bar opens the Accounts list filtered on Placed."
```

```fr
id: FR-NB-121
title: "Placement Update Report (individual and collective)"
brd: [BRNB.011 (p.55)]
actor: "Marketing; TSU; Processing"
priority: "Must have"
fit: "NEW"
screens: "New Business Reports; report NB-PLC-UPDATE"
api: "POST /api/v1/reports/NB-PLC-UPDATE/run; GET .../export?format="
description: "The Placement Update Report shows where each account stands with its insurer. Run for one ARN it is the individual report (regardless of dates); run for a period it is the collective report of every account whose stage changed in the period, grouped by insurer, optionally for one insurer or account officer. Access is restricted by the report permission (ACCOUNT_VIEW)."
preconditions:
  - "The user has REPORT_VIEW and ACCOUNT_VIEW."
main_flow:
  - "The user selects the report, the company and the period, and optionally the ARN, insurer or officer."
  - "BIBS runs the report; the user exports or prints it."
rules:
  - [R1, "Accounts from Ready for placement onwards are included.", Fixed, "-"]
validations: []
fields_screen: "NB-PLC-UPDATE columns (grouped by insurer)"
fields:
  - [ARN / Client / Product, Text, "-", Account, "-"]
  - [Stage / Since, Text / Date, "-", Work case, "-"]
  - [Placement Slip / Placed On, Text / Date, "-", Slip, "-"]
  - [Hold Cover / Hold Cover Ref., Text, "-", Hold cover, "-"]
  - [Policy Issued, Date, "-", Account, "-"]
  - [Last Return / Remarks, Text, "-", Insurer return, "-"]
  - [Officer, Text, "-", Account officer, "-"]
notifications:
  - "None."
audit:
  - "Runs and exports logged with user and parameters."
acceptance:
  - "The report for one ARN shows only that account."
  - "The collective report for June groups the accounts by insurer."
```

```fr
id: FR-NB-122
title: "Operational reports - stage outcomes, status, placement, billing, payments, production"
brd: [BRNB.075 (p.117-118), BRNB.115 (p.18-19), BRNB.022 (p.60-61)]
actor: "Marketing; Processing; TLs; approvers"
priority: "Must have"
fit: "NEW"
screens: "New Business Reports; Production Targets"
api: "POST /api/v1/reports/{code}/run; GET /api/v1/nb/targets; PUT /api/v1/nb/targets"
description:
  - "The reports of BRNB.075 are in the category New Business of the Report Centre, each with a date range - Successful and Fall-out Accounts per Stage (NB-STAGE-OUTCOME); Account Status Report (NB-ACC-STATUS) with stage age, SLA due and breach, stalled flag and an exceptions filter; Placement Summary (NB-PLC-SUMMARY) with slips sent, resent and failed per insurer and branch; CLPC Billing Report (NB-CLPC-BILLING); Matched and Unmatched Payments (NB-PAY-MATCH); Production Statistics (NB-PRODUCTION) per region, department, team or officer against target. Columns are in section 6.1."
  - "Targets per unit and period (bookings, premium, commission) are maintained on Production Targets and pro-rated by days to the report period."
  - "**Built behaviour:** the QPS Placement Report is replaced by the Placement Summary (OOS-2). The BRD's production 'per system' is read as per officer; the sales hierarchy and target values wait for Q41 (seed targets delivered)."
preconditions:
  - "The user holds the report's permission (section 6.1)."
main_flow:
  - "The user selects the report and the date range and runs it."
  - "The user downloads it (FR-NB-125) or prints it (FR-NB-124)."
rules:
  - [R1, "Stalled after NB_STALLED_DAYS days (default 5).", Configurable, Parameter NB_STALLED_DAYS]
  - [R2, "Duplicate fall-outs are the bulk rows refused as duplicates (screen duplicates are never created).", Fixed, "-"]
  - [R3, "Targets maintained by the holders of WORK_ASSIGN / MASTER_MAINTAIN.", Configurable, Production Targets]
validations:
  - [Target period invalid, The period end must not be before the period start, TARGET_PERIOD_INVALID]
  - [Negative target, Targets cannot be negative, TARGET_NEGATIVE]
notifications:
  - "None."
audit:
  - "Runs and exports logged; target changes audited."
acceptance:
  - "The Account Status Report with the exception filter Breach lists only accounts past their SLA."
  - "The Placement Summary shows the failed e-mails of each insurer."
  - "Production for a team shows bookings against the team's monthly target and the achievement percentage."
```

```fr
id: FR-NB-123
title: "Role-based reports and saved report variants"
brd: [BRNB.057 (p.106)]
actor: "Marketing; Processing; Approver"
priority: "Must have"
fit: "CHANGE"
screens: "New Business Reports; Report runner (/reports/<code>)"
api: "GET | POST /api/v1/nb/report-variants; DELETE .../report-variants/{id}"
description:
  - "Each user sees only the reports his roles allow (each report has its own permission). The user selects a date range and parameters, views the result, downloads it (.xlsx, .ods and the formats of FR-NB-125), saves it to the chosen folder, and prints it."
  - "The user saves the parameters of a report as a named variant, optionally shared with the other users who may run the report; only the owner deletes it."
  - "**Built behaviour:** 'create dynamic reports' is delivered as saved variants with column filters; an ad-hoc report builder is not built (Q40, which BRD-8 and BRD-12 answer as enough)."
preconditions:
  - "The user has REPORT_VIEW."
main_flow:
  - "The user opens New Business Reports and selects a report."
  - "The user sets the parameters and runs it."
  - "The user clicks **Save Variant**, names it and chooses Shared."
rules:
  - [R1, "A variant can be run only by users who may run its report.", Fixed, "-"]
validations:
  - [Delete by a non-owner, Only the user who saved a report variant may delete it, VARIANT_NOT_OWNER]
  - [Report not permitted, "Report <code> is not available to you", REPORT_NOT_AVAILABLE]
notifications:
  - "None."
audit:
  - "Variant creation and deletion audited."
acceptance:
  - "A Marketing AO does not see the CLPC Billing Report."
  - "A shared variant \"SLA breaches - all stages\" runs for another TL with the same parameters."
```

```fr
id: FR-NB-124
title: "Print reports"
brd: [BRNB.031 (p.71-72)]
actor: "Users with report access"
priority: "Must have"
fit: "CHANGE"
screens: "Report runner (Print)"
api: "GET /api/v1/reports/{code}/export?format=PDF"
description:
  - "Once a report is shown, the **Print** button opens the PDF in the browser's print preview. The PDF has the same rows, columns and filters as the screen, and a header block with the report name and ID, the user, the run date and time and a \"Filters:\" line. Pages are A4, portrait or landscape by report, in the BDO style. Only data the user may see is printed."
  - "**Built behaviour:** A4 only (no Letter). Summary reports (placement summary, stage outcome, production) and detail reports with group subtotals are separate reports; there is no per-report summary / detail switch."
preconditions:
  - "The report has been run."
main_flow:
  - "The user runs the report and clicks **Print**."
  - "The browser shows the preview; the user prints."
rules:
  - [R1, "Print is enabled only after a successful run.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Print (PDF export) logged with user and parameters."
acceptance:
  - "A printed Account Status Report shows the report name, user, date and the filters applied."
  - "The printout has the same number of rows as the screen."
```

```fr
id: FR-NB-125
title: "Download reports in several formats"
brd: [BRNB.037 (p.78-79)]
actor: "Users with report access"
priority: "Must have"
fit: "CHANGE"
screens: "Report runner (Download)"
api: "GET /api/v1/reports/{code}/export?format=PDF|XLSX|ODS|CSV|XML"
description: "Users download any report they may run, for the selected date range and parameters, as .xlsx, .ods, .csv, .xml or .pdf. The browser saves the file to the location the user chooses. Every download is logged."
preconditions:
  - "The user holds the report's permission."
main_flow:
  - "The user runs the report and selects the format."
  - "BIBS generates the file with the same rows and metadata."
rules:
  - [R1, "The XML export carries the metadata, columns and typed rows.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Each download logged with user, report, format and parameters."
acceptance:
  - "The ODS and XLSX files of the same run contain the same rows."
  - "A user without the report's permission cannot download it."
```

```fr
id: FR-NB-126
title: "Late Renewal Requests Report"
brd: [BRNB.018 (p.57)]
actor: "System; users with report access"
priority: "Must have"
fit: "CHANGE"
screens: "-"
api: "-"
description:
  - "The BRD asks for a scheduled report of renewals received after expiry, restricted by role."
  - "**Built behaviour:** not built. The report concerns renewals; BRD-6 Renewal has no such report and Report List #182 is a package report. The proposal is a variant of the Renewal listing (RNW-LISTING) once BDOI confirms under Q09 / RQ29."
preconditions:
  - "BDOI answers Q09."
main_flow:
  - "Parked: no flow in this phase."
rules: []
validations: []
notifications:
  - "None."
audit:
  - "Not applicable until built."
acceptance:
  - "Parked; acceptance criteria are written when Q09 is answered."
```

## Administration

```fr
id: FR-NB-130
title: "Log in with a role profile; inactivity and sign-out warnings"
brd: [BRNB.040 (p.93-94), BRNB.087 (p.122)]
actor: "All users; System Administrator"
priority: "Must have"
fit: "CONFIGURE"
screens: "Login; session warning dialog"
api: "POST /api/v1/auth/login; GET /api/v1/system/session-policy"
description:
  - "Users of every profile (Marketing, Processing, E-policy Sender, Approver, Adjustment, Business Administrator, System Administrator) log in with their BIBS user ID and password from any BDO-issued device or workstation through the browser. The menu shows the screens of their roles."
  - "The web client warns after 15 minutes of inactivity and signs the user out at the session timeout; it also warns 30 minutes before the system-triggered sign-out at the end of the session."
  - "**Built behaviour:** sign-in is local to BIBS. BRD-11 requires directory sign-in (BDO EUA / Windows ID, LDAP / AD or SSO); it is a parked port until BDO supplies the interface (decision D6, R5)."
preconditions:
  - "The user has an active BIBS account with at least one role."
main_flow:
  - "The user enters the user ID and password."
  - "BIBS checks the credentials and opens the landing page of the user's roles (NB Dashboard for broking roles)."
alternate_flows:
  - "Wrong credentials; the account locks after 3 failed attempts (decision D5)."
  - "Inactivity; the warning dialog offers to stay signed in."
rules:
  - [R1, "Session timeout 30 minutes; idle warning after 15 minutes; expiry warning 30 minutes before the absolute sign-out.", Configurable, "Parameters SESSION_TIMEOUT_MINUTES, SESSION_IDLE_WARNING_MINUTES, SESSION_EXPIRY_WARNING_MINUTES"]
  - [R2, "Lock-out after 3 failed attempts.", Configurable, Parameter LOGIN_MAX_FAILED_ATTEMPTS]
validations:
  - [Wrong user ID or password, Invalid user ID or password, "-"]
  - [Account locked, Your account is locked. Contact the System Administrator, "-"]
notifications:
  - "None."
audit:
  - "Successful and failed log-ins with user, time and source address."
acceptance:
  - "A Processing user logs in and sees the Processing screens only."
  - "After 15 minutes without activity the warning appears."
  - "The System Administrator logs in from a BDO workstation with the System Administrator profile."
```

```fr
id: FR-NB-131
title: "Open BIBS in several browser tabs"
brd: [BRNB.082 (p.121)]
actor: "All users (Business Administrator in the BRD)"
priority: "Must have"
fit: "CHANGE"
screens: "Every screen"
api: "-"
description: "A signed-in user opens BIBS in a new tab without signing in again. The new tab obtains the session from the open tabs; activity in one tab keeps all tabs signed in, and signing out in one tab signs out all tabs."
preconditions:
  - "The user is signed in in one tab."
main_flow:
  - "The user opens a BIBS link in a new tab."
  - "The new tab asks the open tabs for the session and opens the page."
rules:
  - [R1, "The session token stays in each tab's session storage; tabs share it over a browser channel.", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Not applicable."
acceptance:
  - "A user opens an account in a second tab without a log-in prompt."
  - "Signing out in one tab signs out the other tabs."
```

```fr
id: FR-NB-132
title: "Maintain lists of values with effectivity and approval"
brd: [BRNB.083 (p.121)]
actor: "Business Administrator (maintain); Approver (authorise)"
priority: "Must have"
fit: "NEW"
screens: "Lists of Values (Broking Setup)"
api: "GET /api/v1/lov/types; GET | POST /api/v1/lov/{type}/values; PUT /api/v1/lov/values/{id}; POST .../values/{id}/authorize | deactivate"
description:
  - "The Business Administrator views the lists of values, adds values, edits them and deactivates them, with effective-from and effective-to dates. Each new or changed value is saved for authorisation and becomes usable when another user with MASTER_AUTHORIZE authorises it. Screens and validations use only the values active on the date."
  - "**Built behaviour:** the BRD's 'delete LOVs' is deactivation; values are never deleted, so old records keep their labels."
preconditions:
  - "The user has LOV_MANAGE; the list is maintainable by business users."
main_flow:
  - "The user selects a list and clicks **New Value** (or **Edit**)."
  - "The user enters code, label, order, parent and effective dates and clicks **Save for Authorization**."
  - "The authoriser clicks **Authorize**; the value is active from its effective date."
alternate_flows:
  - "**Deactivate** a value; it stays on existing records."
rules:
  - [R1, "Maker-checker on every value.", Fixed, "-"]
  - [R2, "Lists marked system-maintained cannot be changed by business users.", Fixed, "-"]
validations:
  - [Effective-to before effective-from, The effective-to date is before the effective-from date, LOV_EFFECTIVITY_INVALID]
  - [List maintained by the system, "The list <type> is maintained by the system", LOV_NOT_MAINTAINABLE]
  - [Authorisation by the maker, A record cannot be authorized by the user who maintained it, MAKER_CHECKER_VIOLATION]
fields_screen: "List value"
fields:
  - [Code, Text, "Yes", "-", Unique in the list; fixed after save]
  - [Label, Text, "Yes", "-", Up to 200 characters]
  - [Order, Number, "No", "-", Whole number]
  - [Parent, Text, "No", Parent list value, "-"]
  - [Effective from / Effective to, Date, "Yes (from)", "-", To on or after from]
notifications:
  - "Authorisers see pending values in My Approvals (FR-NB-133)."
audit:
  - "Every change with maker, checker, before and after values."
acceptance:
  - "A new return reason is usable only after another user authorises it."
  - "A value effective from next month is not offered today."
```

```fr
id: FR-NB-133
title: "Approve pending requests in My Approvals"
brd: [BRNB.079 (p.119-120)]
actor: "Approver"
priority: "Must have"
fit: "FIT"
screens: "My Approvals"
api: "GET /api/v1/approvals/inbox"
description: "The approver sees in one inbox the requests pending his approval - list value changes, access (profile) requests, master records - and opens each to review its details. He approves or declines it with a comment, which is saved with the decision; approval applies the change and pushes the item to its next step. Quotation, PRF and slip approvals are done on their own screens (FR-NB-043, 051, 053, 056)."
preconditions:
  - "The user holds the approval permission of the source (MASTER_AUTHORIZE, ACCESS_APPROVE)."
main_flow:
  - "The approver opens My Approvals and a request."
  - "The approver reviews and clicks **Approve** (or **Reject**) with a comment."
rules:
  - [R1, "An approver never decides his own request.", Fixed, "-"]
  - [R2, "Override requests are out of scope (Renewal, OOS-1).", Fixed, "-"]
validations:
  - [Decision on own request, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
  - [Reject without reason, Enter the reason of the rejection, ACCESS_REJECT_REASON]
notifications:
  - "The requester on the decision."
audit:
  - "Decision, comment, user and time."
acceptance:
  - "A pending access request and a pending list value both appear in the approver's inbox."
  - "Rejecting without a reason is refused."
```

```fr
id: FR-NB-134
title: "Define profiles, assign functions and roles"
brd: [BRNB.088 (p.122-127), BRNB.084 (p.121)]
actor: "System Administrator; Business Administrator"
priority: "Must have"
fit: "CHANGE"
screens: "Roles & Permissions; Users; User Access Matrix"
api: "/api/v1/admin/roles; /api/v1/admin/users; GET /api/v1/nbadmin/access-matrix; GET .../access-matrix/export"
description:
  - "BIBS delivers the profiles of the BRD - Marketing User (MKT_AO), Marketing TL (MKT_TL), Processing (PROCESSOR, PROCESSING_TL), Approver (NB_APPROVER), Business Administrator, Adjustment, E-policy Sender - plus TSU. Each BRD function is a permission (section 3.4); a profile is a role with its permissions. Users are assigned roles per the agreed User Access Matrix; a user may hold several roles (decision D5)."
  - "The User Access Matrix screen shows roles against permissions (By Permission) and against areas and actions (By Action), with the enabled users per role, and exports to Excel."
  - "**Built behaviour:** role and user changes go through approved access requests (FR-NB-135); the direct role edit stays for the System Administrator as in BRD-11."
preconditions:
  - "The user has the administration permissions."
main_flow:
  - "The administrator opens the User Access Matrix to review the roles."
  - "Changes are requested through Access Requests and applied on approval."
rules:
  - [R1, "Delivered matrix as section 3.3 until BDOI confirms it (BRD-11).", Configurable, Roles and permissions]
validations:
  - [Unknown role, "Unknown role(s): <codes>", ACCESS_UNKNOWN_ROLE]
notifications:
  - "None."
audit:
  - "Role and user changes audited; the matrix export is audited."
acceptance:
  - "The matrix shows that only the Adjustment role holds BOOKING_ADJUST."
  - "The Excel export lists every role and permission shown on screen."
```

```fr
id: FR-NB-135
title: "Submit profile creation and modification requests for approval"
brd: [BRNB.085 (p.121-122)]
actor: "Business Administrator or System Administrator (requester); Approver"
priority: "Must have"
fit: "CHANGE"
screens: "Access Requests; New Access Request dialog; request detail"
api: "GET | POST /api/v1/nbadmin/access-requests; POST .../{id}/approve | reject"
description:
  - "The requester creates a request of type Create user (user name, full name, e-mail, home branch, roles), Modify roles, Disable user, Enable user or Modify role permissions (role, permissions to add and remove), with a justification, and submits it for approval. The approver (ACCESS_APPROVE), who is not the requester, approves and applies it or rejects it with a reason. A created user gets a temporary password shown once to the approver."
  - "BRD-11 extends the lifecycle (drafts, return, cancel, chosen approver, bulk); that extension belongs to the BRD-11 build."
preconditions:
  - "The user has ACCESS_REQUEST."
main_flow:
  - "The requester clicks **New Request**, selects the type and fills in the fields."
  - "The requester clicks **Submit for Approval**."
  - "The approver opens the request and clicks **Approve and Apply**."
alternate_flows:
  - "Reject with a reason."
rules:
  - [R1, "One pending request per user or role at a time.", Fixed, "-"]
  - [R2, "The approver is never the requester.", Fixed, "-"]
validations:
  - [User name format, "The user name has 3 to 50 letters, digits, dots, dashes or underscores", ACCESS_USERNAME]
  - [User exists, "User <name> already exists", ACCESS_USER_EXISTS]
  - [Unknown user, "User <name> does not exist", ACCESS_UNKNOWN_USER]
  - [Full name missing, Enter the full name of the new user, ACCESS_FULL_NAME]
  - [No role, Select at least one role, ACCESS_ROLES]
  - [Role missing, Select the role to change, ACCESS_ROLE]
  - [Permission added and removed, "Permission(s) both added and removed: <list>", ACCESS_PERMISSION_CONFLICT]
  - [No change, "The request does not change the permissions of role <role>", ACCESS_NO_PERMISSION_CHANGE]
  - [Request already pending, "A request for <subject> is already waiting for approval", ACCESS_REQUEST_PENDING]
  - [Decided by the requester, A request cannot be decided by the user who submitted it, ACCESS_FOUR_EYES]
  - [Already decided, "Request <number> is already <status>", ACCESS_REQUEST_DECIDED]
fields_screen: "New Access Request"
fields:
  - [Request, List, "Yes", "Create user, Modify roles, Disable user, Enable user, Modify role permissions", "-"]
  - [User name, Text, Cond., "-", 3-50 characters]
  - [Full name / E-mail / Home branch, Text / List, Cond., Branches, Create user]
  - [Roles, Multi-select, Cond., Roles, At least one]
  - [Role / Permissions to add / remove, List / Multi-select, Cond., Roles; permissions, Modify role permissions]
  - [Justification, Text, "Yes", "-", "-"]
notifications:
  - "Approvers on submission; the requester on the decision."
audit:
  - "Request, decision and applied change with users and times."
acceptance:
  - "A user created by request exists only after another user approves it."
  - "The requester cannot approve his own request."
```

```fr
id: FR-NB-136
title: "View and export the audit log report"
brd: [BRNB.086 (p.122), BRNB.089 (p.127)]
actor: "Business Administrator; System Administrator"
priority: "Must have"
fit: "FIT"
screens: "Audit Trail (Administration)"
api: "GET /api/v1/audit-logs; GET /api/v1/reports/CTL-AUDIT/export?format=PDF|XLSX|CSV"
description:
  - "The Audit Trail screen lists the audit rows by date range, user and entity type. The download buttons export the audit log report CTL-AUDIT as PDF, Excel or CSV, saved where the user chooses."
  - "BRNB.089 criterion 1 repeats \"assign cancel placement function\" (p.127); it is read as \"generate the audit log report\"."
preconditions:
  - "The user has AUDIT_VIEW (or the platform audit permission)."
main_flow:
  - "The user filters the audit trail and clicks **PDF**, **Excel** or **CSV**."
rules:
  - [R1, "The audit trail is read-only (FR-NB-016).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "Exports of the audit log are themselves logged."
acceptance:
  - "The Business Administrator exports one day's audit rows to Excel."
```

```fr
id: FR-NB-137
title: "Apply the retention policy"
brd: [BRNB.106 (p.12), Data retention (p.82)]
actor: "System; Business Administrator"
priority: "Must have"
fit: "NEW"
screens: "Data Retention (Broking Setup)"
api: "GET /api/v1/nbadmin/retention/rules; PUT .../rules/{id}; GET .../rules/{id}/eligible; POST .../retention/review"
description:
  - "Retention rules per record type and status (years online, years in archive, action REVIEW or ARCHIVE) cover clients (inactive, never-confirmed prospects), quotations and PRFs (not proceeded, voided) and accounts (voided, cancelled). A monthly job (RETENTION_REVIEW) counts the eligible records per rule; users list them from the rule. Records stay available until the retention period ends, and every run is logged."
  - "**Built behaviour:** the physical archive and purge are not built; they wait for the DBA's storage and backup decision (Q39). Nothing is deleted. Retention values for other record types come from later BRDs (R5)."
preconditions:
  - "The user has MASTER_VIEW (view) and MASTER_MAINTAIN (edit rules)."
main_flow:
  - "The job runs monthly and stores the counts."
  - "The user opens Data Retention, reviews the counts and lists the eligible records."
  - "The user edits a rule or clicks **Run Review Now**."
rules:
  - [R1, "Default 5 years online and 15 years archive for every rule (p.82).", Configurable, Retention rules]
  - [R2, "Backup every 4 hours with 5 years of backup retention is an infrastructure setting (NFR, section 8).", Fixed, "-"]
validations:
  - [Years invalid, Years online must be positive and years in archive not negative, RETENTION_YEARS]
  - [No status, Enter at least one status, RETENTION_STATUSES]
fields_screen: "Retention rule"
fields:
  - [Record type, Display, "-", "CLIENT, QUOTATION, PROPOSAL, ACCOUNT", "-"]
  - [Statuses, Text, "Yes", Statuses of the record type, At least one]
  - [Years online / Years in archive, Number, "Yes", "-", "> 0 / >= 0"]
  - [Action, List, "Yes", "REVIEW / ARCHIVE", "-"]
  - [Active, Check box, "Yes", "-", "-"]
notifications:
  - "None."
audit:
  - "Rule changes and every review run with date, time and counts."
acceptance:
  - "The review lists voided quotations older than 5 years."
  - "No record is deleted by a review run."
```

# Workflow and status model

The four New Business workflows are data (stages and transitions, migrations V751, V801 and V830). In the figures, solid arrows are the main path, dashed arrows are returns and alternative paths, and dotted arrows close a record. The second line of a stage is its owner.

## Client onboarding (NB_CLIENT)

![Workflow NB_CLIENT: client onboarding (BRNB.090, 101)](figures/brd01_client_workflow.dot){width=10}

<!-- table: widths=3.3,4.6,1.6,7.1 caption="NB_CLIENT stages" size=8.5 -->
| Stage | Owner (permission) | SLA hours | Actions |
|---|---|---|---|
| PROSPECT | Marketing AO (CLIENT_MAINTAIN) | 72 | submit_kyc; deactivate |
| KYC_REVIEW | Verifier (CLIENT_APPROVE) | 24 | verify_kyc; return (RETURN_REASON); deactivate |
| KYC_VERIFIED | Marketing (CLIENT_MAINTAIN) | 24 | confirm (CLIENT_MAINTAIN or CLIENT_APPROVE); deactivate |
| CONFIRMED | - | - | review_kyc (CLIENT_APPROVE, periodic review); deactivate |
| INACTIVE | - | terminal | - |

## Package quotation (NB_QUOTATION)

![Workflow NB_QUOTATION: package quotation (BRNB.020-022, 041-045)](figures/brd01_quotation_workflow.dot){width=13}

<!-- table: widths=3.3,4.6,1.6,7.1 caption="NB_QUOTATION stages" size=8.5 -->
| Stage | Owner (permission) | SLA hours | Actions |
|---|---|---|---|
| DRAFT | Marketing AO (QUOTE_MAINTAIN) | 24 | submit; void (VOID_REASON) |
| FOR_REVIEW | Approver (QUOTE_APPROVE) | 8 | approve; return (RETURN_REASON); void |
| APPROVED | Marketing (QUOTE_MAINTAIN) | 24 | send; revise (to DRAFT, new version) |
| SENT_TO_CLIENT | Marketing (QUOTE_MAINTAIN) | 168 | accept; decline; revise |
| ACCEPTED | Marketing (QUOTE_MAINTAIN) | 24 | create_accounts |
| CONVERTED, NOT_PROCEEDED, VOIDED | - | terminal | - |

## Non-package proposal (NB_PROPOSAL)

![Workflow NB_PROPOSAL: Proposal Request Form (BRNB.005-017)](figures/brd01_proposal_workflow.dot)

<!-- pagebreak -->

<!-- table: widths=3.4,4.6,1.6,7 caption="NB_PROPOSAL stages" size=8.5 -->
| Stage | Owner (permission) | SLA hours | Actions |
|---|---|---|---|
| DRAFT | Marketing AO (PROPOSAL_REQUEST) | 24 | submit; void |
| FOR_MKT_APPROVAL | Marketing TL / TH / UH (PROPOSAL_APPROVE) | 8 | approve; return; void |
| WITH_TSU | TSU (TSU_PROCESS) | 8 | prepare_qs; return; void |
| QS_PREPARATION | TSU (TSU_PROCESS) | 24 | submit_qs |
| QS_FOR_APPROVAL | TSU approver (TSU_APPROVE) | 8 | approve_qs; return |
| QS_SENT | TSU (TSU_PROCESS) | 72 | terms_complete |
| TERMS_RECEIVED | TSU (TSU_PROCESS) | 24 | submit_ps |
| PS_FOR_APPROVAL | TSU approver (TSU_APPROVE) | 8 | approve_ps; return |
| PS_RELEASED | Marketing (PROPOSAL_REQUEST) | 24 | send_to_client |
| SENT_TO_CLIENT | Marketing (PROPOSAL_REQUEST) | 168 | accept; decline |
| ACCEPTED | Marketing (PROPOSAL_REQUEST) | 24 | create_accounts |
| CONVERTED, NOT_PROCEEDED, VOIDED | - | terminal | - |

## Account (NB_ACCOUNT)

![Workflow NB_ACCOUNT: account from creation to booking (BRNB.022, 033, 034, 062, 069, 094, 111, 115)](figures/brd01_account_workflow.dot)

<!-- table: widths=4.8,4.3,1.4,6.1 caption="NB_ACCOUNT stages" size=8.5 -->
| Stage | Owner (permission) | SLA hours | Actions |
|---|---|---|---|
| DRAFT | Marketing (ACCOUNT_MAINTAIN) | 24 | submit; void |
| SUBMITTED | Processing (ACCOUNT_PROCESS) | 8 | validate; direct_booking; return; void |
| RETURNED_TO_MARKETING | Marketing (ACCOUNT_MAINTAIN) | 24 | resubmit; void |
| AWAITING_PAYMENT | Processing (BILLING_MANAGE) | 72 | payment_confirmed (gate); return |
| READY_FOR_PLACEMENT | Processing (PLACEMENT_MANAGE) | 8 | place; direct_booking; return; cancel_placement |
| PLACED | Processing (EPOLICY_MANAGE) | 72 | policy_received; insurer_return; cancel_placement |
| RETURNED_BY_INSURER | Processing (PLACEMENT_MANAGE) | 8 | resubmit; return; cancel_placement |
| PLACEMENT_CANCELLED | - | - | reactivate (ACCOUNT_MAINTAIN or PLACEMENT_MANAGE) |
| POLICY_ISSUED | Processing (BOOKING_PROCESS) | 24 | book |
| BOOKED | - | - | cancel (BOOKING_ADJUST, CANCELLATION_REASON); endorsements |
| CANCELLED, VOIDED | - | terminal | - |

## SLA, escalation and status reporting

- SLA hours are held per stage and start when a record enters the stage. The values above are the build's defaults until BDOI confirms the stage list and targets (Q10).
- The daily alert check raises WORK_SLA_BREACH for work items past their stage SLA. Overdue items are counted on the NB Dashboard and listed in the Account Status Report; stalled accounts (no movement for NB_STALLED_DAYS) are flagged there too.
- Escalation beyond the stage owners (for example to a unit head) is not in the BRD and is not built.
- Every transition writes an insert-only history row; the status reports of FR-NB-122 read that history.

# Reports and documents

## Reports

<!-- table: widths=3.1,4,6.2,3.5 caption="New Business reports (category New Business; KYC in Control & Audit)" size=8.5 -->
| Code | Name | Content | Permission |
|---|---|---|---|
| NB-PLC-UPDATE | Placement Update Report | Placement status per account, individual (ARN) or collective by insurer (BRNB.011) | ACCOUNT_VIEW |
| NB-ACC-STATUS | Account Status Report | Stage, stage age, SLA due and breach, stalled, assignee; exceptions filter (BRNB.075, 115) | ACCOUNT_VIEW |
| NB-STAGE-OUTCOME | Successful and Fall-out Accounts per Stage | Entered, successful, returned, voided, cancelled, open per stage; bulk rejects and duplicate fall-outs (BRNB.075) | ACCOUNT_VIEW |
| NB-PLC-SUMMARY | Placement Summary | Slips, accounts, sent, resent, failed and queued e-mails, superseded per insurer and branch (BRNB.075) | PLACEMENT_MANAGE |
| NB-CLPC-BILLING | CLPC Billing Report | CBG Fire accounts billed to CLPC with payment status (BRNB.067, 075) | BILLING_MANAGE |
| NB-PAY-MATCH | Matched and Unmatched Payments | Payment report lines by match result (BRNB.068, 075) | BILLING_MANAGE |
| NB-PRODUCTION | Production Statistics | Bookings, premium and commission against target by region, department, team or officer (BRNB.075) | WORK_ASSIGN |
| NB-BOOKED-REG | Booked Accounts Register | Bookings, endorsements and cancellations by product line (BRNB.027, 108) | ACCOUNT_VIEW |
| NB-SI-REG | Service Invoice Register | Service invoices and credits with dispatch outcome (BRNB.100, 100b) | BOOKING_PROCESS |
| NB-DISPATCH | Insurance Advice and E-policy Dispatch | Sent, failed and queued e-policies and advices with reasons (BRNB.078) | ACCOUNT_VIEW |
| NB-KYC-DUE | KYC Reviews Due | Clients due for KYC review (BRNB.110) | CLIENT_VIEW |
| CTL-AUDIT | Audit Trail report | Audit rows by date, user, entity (BRNB.086, 089) | AUDIT_VIEW |

Every report takes the company and a date range (default from the start of the month to today), exports to PDF, XLSX, ODS, CSV and XML, prints with the metadata header, and supports saved variants. The columns of NB-PLC-UPDATE, NB-KYC-DUE and NB-DISPATCH are in FR-NB-121, 038 and 106.

<!-- table: widths=3.2,4.2,9.2 caption="Parameters and columns of the other New Business reports" size=8 -->
| Code | Parameters | Columns |
|---|---|---|
| NB-ACC-STATUS | Status; Exceptions (all, breach, stalled); Account Officer | Grouped by stage - ARN, Client, Product, Officer, In Stage Since, Age (days), SLA Due, SLA Breached, Stalled, Assignee |
| NB-STAGE-OUTCOME | - | Grouped by section - Stage / Upload, Entered / Rows, Successful, Returned, Voided, Cancelled, Rejected, Duplicate Fall-outs, Open Now |
| NB-PLC-SUMMARY | - | Insurer, Branch, Slips, Accounts, Sent, Resent, Failed E-mails, Queued E-mails, Superseded |
| NB-CLPC-BILLING | Payment Status (all, billed, paid, unpaid) | Grouped by billing batch - ARN, PN No., Loan Application No., Borrower, Originating Unit, BDOI Location, Booking Date, Premium, Amortised, Payment |
| NB-PAY-MATCH | Match Result (all, matched, unmatched, ambiguous, unpaid) | Grouped by result - Payment Report, Kind, Report Status, Reference, Paid, Amount, Paid On, ARN, Manual Match, Applied, Message, Row |
| NB-PRODUCTION | Sales Unit Level | Unit, Name, Bookings, Target Bookings, Premium, Target Premium, Achievement, Commission, Target Commission |
| NB-BOOKED-REG | Insurer Code | Grouped by product line - Booking Date, Invoice No., ARN, Client, Insurer, Product, Policy No., Transaction, NB / Renewal, Basic Premium, Taxes & Charges, Gross Premium, Commission, VAT on Commission, Cost Center, Officer, Incentive, Direct Payment |
| NB-SI-REG | Dispatch (all, not sent, queued, sent, failed) | Grouped by invoice type - Service Invoice, Issue Date, Kind, Booked Invoice, ARN, Recipient, Commission, VAT, Withholding Tax, Net Amount, Dispatch, Failure Reason, Credit Of |

## Documents

Documents are generated from versioned templates (FR-NB-004). The layouts are the build's drafts; BDOI's layouts replace them when provided (Q03).

<!-- table: widths=3.8,3.2,9.6 caption="New Business documents" size=8.5 -->
| Template | Output | Content |
|---|---|---|
| QUOTATION_LETTER, QUOTATION_TERMS | PDF and Excel schedule, protected | Insurance quotation - client, product, insurer, period, validity, items with sums insured, premium breakdown, terms and conditions |
| QUOTATION_SLIP | PDF, one per insurer, protected | QS number, PRF and ARN, risk details and items, period, reply date, comment |
| Comparative table | PDF and Excel | One row per insurer - response, premium, rate, deductibles, conditions, validity, remarks; lowest premium and recommended flags |
| PROPOSAL_SLIP | PDF, versioned | PS number and version, chosen insurer, terms of the quotation slip and the insurer's terms |
| PLACEMENT_SLIP | PDF and Excel per insurer branch | Slip number and version, insurer branch, accounts with ARN, client, items, sums insured, premium, period |
| HOLD_COVER_REQUEST | PDF | ARN, insured, insurer, start and expiry of the hold cover |
| INSURANCE_ADVICE | PDF | IA number, mortgagee bank, insured, policy number, period, sums insured |
| EPOLICY_EMAIL | E-mail text | Covering e-mail of the e-policy with the password hint |
| SERVICE_INVOICE_NOTE | PDF | Service invoice number, recipient, booked invoice, commission, VAT, withholding tax, net amount |
| CLPC billing file | XLSX or ODS | Columns of FR-NB-090 |

# Interfaces and integration

Figure 6 shows the interfaces of New Business. The broking modules talk to each other through published services and events; external parties are reached by e-mail or by file.

![Interfaces of New Business (dashed = parked)](figures/brd01_integration.dot)

<!-- table: widths=3.9,2,7.1,2.6,2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| HLS and other source systems | In | Quotation requests; bulk quotations and accounts by upload of the extract; intake port | BRNB.023, 028, 064 | PARKED |
| Shared mailboxes | In | Requests and e-policies read automatically | BRNB.041, 073 | PARKED |
| E-mail outbox | Out | Protected quotations, slips, hold cover requests, advices, e-policies, service invoices; send log | BRNB.008, 013, 035, 071 | BUILT |
| Insurers | Out / In | Slips and requests by e-mail; replies keyed in; SFTP / API channels | BRNB.008, 071 | PARTIAL |
| CLPC | Out / In | Billing file download and payment report upload; SFTP transfer | BRNB.067 | PARTIAL |
| Operations (BRD-2) | Out / In | Booked invoices (InvoiceBooked), pre-booked look-up; cashier receipts as payment confirmations | BRNB.027, 068 | BUILT |
| Product Maintenance (BRD-3) | In | Products, package versions, rate exceptions, incentive criteria | BRNB.001, 107 | BUILT |
| Accounting engine | Out | BROKER_BOOKING events, journals, open items, cost center dimension | BRNB.027, 108 | BUILT |
| Other BDOI systems | Out | NB data feed (QPS / EBIX contact updates per BRD-9) | BRNB.015 | PARKED |
| BDO directory (EUA / AD / SSO) | In | Sign-in | BRNB.040 | PARKED |
| BIR CAS / e-invoicing | Out | Service invoice transmission | BRNB.100 | OUT |

> [!PARKED] Parked seams
> The parked interfaces are built as seams - a port, a staging table or a manual upload - so that adding the transport is a new adapter with no change to the workflows: `QuotationRequestSource` (HLS, Q11), manual capture with the e-mail attached (Q12, Q31), `PaymentConfirmationSource` and file upload (CLPC, Q28), the e-mail channel only (insurers, Q06), `DirectoryAuthenticator` (sign-in, D6), and the OCR port of the extraction (Q24).

# Non-functional requirements

<!-- table: widths=3,5.6,5.4,2.6 caption="Non-functional requirements (BRD p.82, p.210-212)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | 485 named (392 Marketing AO / TL, 82 Processing, 3 System Admin); up to 145 concurrent | Stateless application scaled horizontally; sized for 150 concurrent users plus 20% a year | FIT |
| Volumes | 21,200 transactions a month (quotation, client, account, uploads, booking); 1,300 endorsements a month; growth 20% a year | About 1,000 transactions a day; bulk uploads up to 5,000 rows per file, committed row by row | FIT |
| Response time | 10 seconds for every listed function | Online p95 under 3 seconds; bulk jobs, e-mails and batch booking run asynchronously | FIT |
| Peak hours | 08:00-10:00 and 15:00-17:00 | Batch booking at 20:00; KYC and retention jobs outside peaks | FIT |
| Availability | 07:00-22:00, Monday to Saturday; other items "follow existing QPS set-up" | Maintenance window outside service hours; one BIBS-wide NFR set is being agreed (XQ08) | OPEN |
| Devices | Any BDO-issued device or workstation; same performance on mobile and desktop | Browser application, responsive screens; no device restriction | FIT |
| Security | Outbound documents encrypted or password protected; insurers have no access | Protected PDF / Excel with separate passwords; no external user role; role-based access and four-eyes | FIT |
| Audit | All actions logged | Append-only audit and workflow history (FR-NB-016) | FIT |
| Retention | Application, database and audit logs and historical data: 5 years online, 15 years archive; backup every 4 hours kept 5 years (addendum p.82; the original BRD says "follow QPS") | Retention rules and monthly review (FR-NB-137); archive and purge wait for Q39; backups are an infrastructure setting | PARTIAL |

# Configuration items owned by the System Administrator and the Business Administrator

The items below are changed in BIBS without a release. Changes to parameters, lists and masters are audited; lists and masters are maker-checker.

## Parameters

<!-- table: widths=5.8,4.4,6.4 caption="New Business parameters" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| QUOTATION_NUMBER_PREFIX | QT | Prefix of quotation numbers (<prefix>-yyyy-nnnnnn, shown as Proposal No.) |
| QUOTATION_REQUEST_PREFIX | REQ | Prefix of quotation request numbers |
| QUOTATION_VALIDITY_DAYS | 30 | Default validity of a quotation |
| QUOTATION_EXPIRING_DAYS | 7 | Window of the Expiring filter |
| PROPOSAL_NUMBER_PREFIX | PRF | Prefix of the PRF marketing reference |
| QUOTATION_SLIP_PREFIX | QS | Prefix of quotation slip numbers |
| PROPOSAL_SLIP_PREFIX | PS | Prefix of proposal slip numbers |
| QUOTATION_SLIP_REPLY_DAYS | 5 | Days given to insurers to reply |
| CLIENT_MIN_AGE | 18 | Minimum age of an individual policyholder |
| CLIENT_MIN_FIELDS_INDIVIDUAL | BIRTH_DATE, TIN, ID, CONTACT, ADDRESS, MARKET_SEGMENT | Minimum information of an individual client |
| CLIENT_MIN_FIELDS_CORPORATE | TIN, CONTACT, ADDRESS, MARKET_SEGMENT | Minimum information of a corporate client |
| KYC_REVIEW_MONTHS / KYC_REVIEW_MONTHS_HIGH_RISK | 36 / 12 | Months between periodic KYC reviews |
| KYC_DUE_WINDOW_DAYS | 30 | Days ahead in which a KYC review is listed as due |
| HOLD_COVER_DAYS | 30 | Hold cover period requested from the insurer |
| HOLD_COVER_ALERT_DAYS | 5 | Days before expiry when Processing is alerted |
| IA_TRIGGER | ON_POLICY_ISSUE | When the Insurance Advice is generated: ON_POLICY_ISSUE, ON_PLACEMENT or MANUAL |
| EPOLICY_PASSWORD_HINT | Standard text (password sent separately) | Text telling the client how the e-policy password is built |
| OPS_COMMISSION_REALIZATION | ON_COLLECTION | Commission unrealised at booking and realised on collection, or realised at booking |
| BOOKING_WTAX_RATE | 10 | Withholding tax on commission (%) |
| BOOKING_CWT2_SEGMENTS | (empty) | Segments on the second creditable withholding rate |
| NB_STALLED_DAYS | 5 | Days without movement after which an account is stalled |
| BULK_MAX_ROWS | 5000 | Maximum data rows per bulk upload file |
| MAIL_FROM_ADDRESS / MAIL_MAX_ATTEMPTS | no-reply address / 3 | Sender of BIBS e-mails; delivery attempts before FAILED |
| SESSION_TIMEOUT_MINUTES / SESSION_IDLE_WARNING_MINUTES / SESSION_EXPIRY_WARNING_MINUTES | 30 / 15 / 30 | Session timeout and warnings |
| LOGIN_MAX_FAILED_ATTEMPTS | 3 | Failed log-ins before lock-out |

<!-- table: widths=5.4,4,7.2 caption="Scheduled jobs (times in PHT)" size=8.5 -->
| Job | Default schedule | Purpose |
|---|---|---|
| MAIL_DISPATCH | Every 2 minutes | Sends queued e-mails and records outcomes |
| PAYMENT_CONFIRMATION_SWEEP | Hourly | Opens the payment gate of confirmed payments |
| HOLD_COVER_EXPIRY | Daily 08:30 | Alerts and expires hold covers |
| BOOKING_BATCH | Daily 20:00 | Books the queue and scheduled multi-year invoices |
| KYC_REVIEW_DUE | 1st of the month, 10:00 | Expires overdue KYC and notifies the KYC list |
| RETENTION_REVIEW | 2nd of the month, 11:00 | Counts records eligible under the retention rules |
| QUOTATION_REQUEST_INTAKE | Manual (no schedule) | Reads the source-system intake port |
| Alert checks (WORK_SLA_BREACH) | Daily 09:30 | Work items past their SLA |

## Lists of values

<!-- table: widths=5.2,11.4 caption="Lists of values used by New Business" size=8.5 -->
| List | Values delivered |
|---|---|
| MARKET_SEGMENT | CBG; Commercial Banking; Corporate Banking; Retail Marketing; Institutional Banking / SM / BDO accounts |
| SOURCE_CHANNEL | E-mail; Home Loan System (HLS); Bulk upload; Branch referral; Walk-in / direct |
| RETURN_REASON | Incomplete or incorrect details; Missing supporting documents; Declined by insurer; Additional insurer requirements; Rate or terms to be reviewed; Insurer underwriting review; Others |
| VOID_REASON | Duplicate record; Encoding error; Client withdrew the request; Others |
| CANCELLATION_REASON | Client request; Non-payment of premium; Loan cancelled or paid off; Insurer request; Others |
| DOCUMENT_TYPE | IDF; Valid ID; Birth certificate; List of account details; Quotation / proposal; Client acceptance e-mail; Request e-mail; E-policy; Official receipt; Issued policy copy; KYC form; Proof of address; SEC / DTI certificate; GIS; Secretary's certificate; Payment / client confirmation; Hold cover confirmation; Insurance Advice; Quotation slip; Insurer response; Comparative table; Proposal slip; Others |
| KYC_DOCS_INDIVIDUAL / KYC_DOCS_CORPORATE | KYC form, valid ID / KYC form, SEC certificate, GIS, secretary's certificate |
| ID_TYPE, NATIONALITY, CIVIL_STATUS, SOURCE_OF_FUNDS | Identity and KYC profile values of the client master |
| KYC_RISK_RATING | Low; Standard; High (enhanced due diligence) |
| CLIENT_DEACTIVATION_REASON | Duplicate client record; Client request; Deceased / dissolved; No business relationship; Compliance decision; Others |
| CLIENT_TAG | VIP client; BDO employee; Do not call; Watchlist review; Politically exposed person |
| INSTRUCTION_TYPE | Billing and payment; Communication preference; Document delivery; Servicing / renewal; Collection; Others |
| VEHICLE_BODY_TYPE | Sedan; Hatchback; SUV / AUV; Pick-up; Van; Truck; Motorcycle |
| CONSTRUCTION_CLASS | Class 1 concrete to Class 4 light materials |
| OCCUPANCY | Dwelling; Condominium unit; Office; Retail / store; Warehouse; Factory / industrial |
| MORTGAGEE_BANK | BDO Home Loans; BDO Auto Loans; BDO Commercial Lending; BDO Leasing and Finance |
| FFY_CANCEL_REASON | Auto loan cancelled; Dealer promo withdrawn; Tagged in error; Others |
| CLIENT_CONFIRMATION_CHANNEL | E-mail from the client; Signed confirmation form; Recorded call; Others |
| EPOLICY_REJECT_REASON | Belongs to another account; Not the issued policy; Details differ from the placement; Unreadable or damaged file; Others |

## Masters and rules maintained by the business

<!-- table: widths=5,4.8,6.8 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Product lines, cover types, products | MBS under BRD-3 (PRODUCT_AUTHORIZE / MASTER_AUTHORIZE) | FR-NB-001 |
| Field rules and document rules | Business Administrator (MASTER_AUTHORIZE) | FR-NB-002, 003, 017 |
| TSU routing rules | Business Administrator (MASTER_AUTHORIZE) | FR-NB-005 |
| Insurers, branches (LGT), commission rates, placement e-mails | Business Administrator (MASTER_AUTHORIZE) | FR-NB-053, 062, 081 |
| Rates and taxes, short-period table, motor limits | Business Administrator (MASTER_AUTHORIZE) | FR-NB-062 |
| Sales organisation and cost centers; production targets | Business Administrator | FR-NB-119, 122 |
| Payment gate rules | Configuration (change request until Q26 / Q28 are answered) | FR-NB-092 |
| Document templates | Business Administrator | FR-NB-004 |
| Extraction patterns and document triggers | Configuration | FR-NB-101, 102 |
| Auto-book rules, service invoice types | Business Administrator (MASTER_MAINTAIN) | FR-NB-112, 117 |
| Incentive criteria | Incentive Maintenance user under BRD-3 | FR-NB-118 |
| Retention rules | Business Administrator | FR-NB-137 |
| Roles and permissions | Access requests (approver) | FR-NB-134, 135 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-NB-01 | The Workshop Addendum (Apr-2026) updates BRNB.004, 022 and 069 and adds BRNB.090-115; where it differs from the earlier documents it prevails | R1 |
| A-NB-02 | A quotation can be made for a prospect; a confirmed client is required from account creation on (BRNB.029 against BRNB.063) | Q13 |
| A-NB-03 | "Deletion" of in-process records is a void with a reason; nothing is physically deleted | BRNB.019 |
| A-NB-04 | One approval stage per document meets BRNB.005 and 014 until BDOI states the chain | Q05 |
| A-NB-05 | The unnumbered addendum row "send the generated service invoice" is BRNB.100b | p.9 |
| A-NB-06 | "Reactivate cancelled placed accounts" (BRD 2.1.16) is in scope although it has no BRNB ID | Q25 |
| A-NB-07 | BRNB.089 criterion 1 ("assign cancel placement function") is a copy error for "generate audit log report" | p.127 |
| A-NB-08 | The addendum's retention (5 years online, 15 archive) prevails over "follow QPS" | p.82, 212 |
| A-NB-09 | The ARN replaces the QPS reference number on the CLPC billing file and in reports | OOS-2 |

## Dependencies

<!-- table: widths=1.8,11,3.8 caption="Dependencies" size=8.5 -->
| ID | Dependency | Needed for |
|---|---|---|
| D-NB-01 | BDOI provides the product matrix files and minimum fields per line | FR-NB-001, 002 (Q01, Q02) |
| D-NB-02 | BDOI provides the document layouts and the password convention | FR-NB-004, 013 (Q03, Q07) |
| D-NB-03 | BDO provides the HLS, CLPC and insurer interface specifications | FR-NB-040, 090, 081 (Q11, Q28, Q06) |
| D-NB-04 | BDO provides the directory sign-in interface | FR-NB-130 (UQ04) |
| D-NB-05 | Finance provides the GL accounts of the booking event | FR-NB-110 (OQ07) |
| D-NB-06 | The e-mail relay of the BIBS environment is available | FR-NB-013 and every sending FR |
| D-NB-07 | BRD-6 delivers the business type work item BT0 | FR-NB-119 (D1) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions on BRD-1 (status from the cross-BRD decisions, R5)" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
| Q01, Q02 | Product matrix files; minimum mandatory fields per line and cover type | FR-NB-001, 002 | OPEN |
| Q03 | Sample layouts of the quotation, slips, placement slip, hold cover, advice, service invoice | FR-NB-004 | OPEN |
| Q04 | TSU involvement thresholds (fleet, locations, TSI, endorsements) | FR-NB-005 | OPEN |
| Q05 | Approval chain and limits for PRF and quotations | FR-NB-014, 043, 051 | OPEN |
| Q06 | Insurer channels (e-mail, SFTP, API) and branch codes | FR-NB-053, 081 | PARTIAL |
| Q07 | Password convention for outbound documents | FR-NB-013, 105 | PARTIAL |
| Q08 | BDOI systems that receive NB data | FR-NB-015 | PARTIAL |
| Q09 | Late renewal requests report in NB or Renewal | FR-NB-126 | OPEN |
| Q10 | Master list of stages and transitions | Section 5 | PARTIAL |
| Q11 | HLS interface specification | FR-NB-040 | PARTIAL |
| Q12, Q31 | Reading shared mailboxes for requests and e-policies | FR-NB-040, 100 | PARTIAL |
| Q13 | Client code at quotation (conflict BRNB.029 / 063) | FR-NB-041, 046 | OPEN |
| Q14 | Sanitation criteria for bulk lists | FR-NB-019, 065 | PARTIAL |
| Q15 | Format of the reference numbers (ARN, PRF) | FR-NB-047, 050 | PARTIAL |
| Q16, Q17 | BDO KYC standard fields and CIF; duplicate keys and precedence | FR-NB-031, 033 | OPEN |
| Q18 | Tags and instruction types; block or warn | FR-NB-036 | PARTIAL |
| Q19, Q20 | Lead management; event that confirms a client | FR-NB-034, 037 | OPEN |
| Q21 | KYC review frequency by risk rating | FR-NB-038 | PARTIAL |
| Q22 | Fire duplicate rule - exact or normalised address | FR-NB-063 | OPEN |
| Q23 | Maximum file size, file types, naming syntax | FR-NB-017 | PARTIAL |
| Q24 | Documents to extract; OCR | FR-NB-101, 102 | PARTIAL |
| Q25 | Reactivation of cancelled placements in scope | FR-NB-086 | OPEN |
| Q26 | Placement prerequisites per segment | FR-NB-080 | OPEN |
| Q27 | Accounts needing an automatic hold cover; expiry handling | FR-NB-082, 083 | PARTIAL |
| Q28 | CLPC file layouts, transport and schedule | FR-NB-090, 091 | PARTIAL |
| Q29 | Direct-payment accounting | FR-NB-069 | OPEN |
| Q30 | Insurance Advice trigger and recipient | FR-NB-103 | OPEN |
| Q32 | Booking entries and service invoice rules | FR-NB-110, 117 | OPEN |
| Q33 | Incentive qualification rules | FR-NB-118 | OPEN |
| Q34 | Cost center source and default | FR-NB-119 | OPEN |
| Q35 | Multi-year premium and commission basis; OD / Theft basis | FR-NB-062, 114 | OPEN |
| Q36 | FFY payer and renewal hand-off | FR-NB-068 | PARTIAL |
| Q37 | Authoritative source per data element | FR-NB-020 | PARTIAL |
| Q38 | Behaviour of the GRF / ARF request forms | FR-NB-011 | OPEN |
| Q39 | Retention per record type; archive or purge | FR-NB-137 | PARTIAL |
| Q40 | Scope of dynamic reports | FR-NB-123 | PARTIAL |
| Q41 | Sales hierarchy and targets | FR-NB-122 | OPEN |
| Q42 | Directory sign-in; session warnings | FR-NB-130 | ANSWERED |
| Q43 | Premium tax basis per line | FR-NB-062 | OPEN |
| Q44 | Insurer-only functions not required | Platform | ANSWERED |

## Built behaviour that differs from the BRD

The table lists every point where the delivered system does what the BRD asks in a different way, or not yet. Each is also stated in its FR.

<!-- table: widths=2.6,6.6,6,1.4 caption="Differences between the BRD and the built behaviour" size=8 -->
| BRD ID | BRD asks | BIBS does | FR |
|---|---|---|---|
| BRNB.005, 014 | Multi-level approval TL, TH, UH | One approval stage per document; the approver permission decides (Q05) | 014, 051 |
| BRNB.015 | Data synchronised with BDOI tracking systems | Internal events only; external feed parked (Q08) | 015 |
| BRNB.018 | Late Renewal Requests Report | Not built; proposed as a Renewal listing variant (Q09) | 126 |
| BRNB.019 | Delete in-process records | Void with a reason; records kept and hidden from standard lists | 012 |
| BRNB.022 | Status list incl. "Endorsed" | Stages of four workflows; endorsements are invoices on a booked account | 010 |
| BRNB.023 | Requests directly from HLS with source check | Upload of the HLS extract; direct interface parked (Q11) | 040 |
| BRNB.029 / 063 | Client code required / quotation without client code | Prospect code created automatically; confirmed client required from account creation | 041, 046 |
| BRNB.031 | Print on A4 or Letter; summary or detail | A4 only; summary and detail are separate reports | 124 |
| BRNB.032 | Account duplicates by e-mail, phone, ID | Clients checked on identity keys; accounts on vehicle and location keys | 033, 063 |
| BRNB.036 | Edit or delete accounts before batch booking | Edit booking date and cost center, remove from the queue, cancel the batch | 111 |
| BRNB.040, 087 | Log-in with BDO profile (directory) | Local sign-in; directory port parked (D6) | 130 |
| BRNB.041 | Requests received by e-mail | Captured manually with the e-mail attached (Q12) | 040 |
| BRNB.047, 065 | Save draft and cancel in bulk updates | Review before commit; valid rows saved directly | 035 |
| BRNB.057 | Create dynamic reports | Saved report variants; no ad-hoc builder (Q40) | 123 |
| BRNB.062 | Cancelled placement returns to the previous workflow | Stage Placement cancelled until reactivated to Ready for placement | 085 |
| BRNB.067 | Billing file forwarded to CLPC automatically; QPS reference | Download and upload by the user (Q28); ARN instead of QPS reference | 090 |
| BRNB.071 | Placement file sent automatically | Sent by e-mail on the user's action; SFTP / API parked (Q06) | 081 |
| BRNB.072 | Hold cover request sent automatically | Sent when Processing clicks Request Hold Cover (Q27) | 082 |
| BRNB.073 | E-policy received from insurers | Uploaded by Processing; mailbox / SFTP parked (Q31) | 100 |
| BRNB.075 | Production per system; QPS placement report | Per region, department, team, officer; Placement Summary | 122 |
| BRNB.080 | Assign and re-assign workload | Manual claim and assignment; no automatic allocation | 011 |
| BRNB.083 | Delete LOVs | Deactivate; values never deleted | 132 |
| BRNB.091 | Tags and instructions enforced | Banner (warn only); blocking parked (Q18) | 036 |
| BRNB.092 | Authoritative source per data element | Register not built (Q37) | 020 |
| BRNB.097 | NB / Renewal classified per transaction | NB booking stamps New Business; type from the account planned (BT0) | 119 |
| BRNB.099 | Leads management | Prospects only; leads parked (Q19) | 037 |
| BRNB.104 | Extraction from uploaded documents | Text PDFs only; OCR parked (Q24) | 101 |
| BRNB.106 | Retain, archive, purge | Rules and review counts; archive and purge parked (Q39) | 137 |
| BRNB.107 | Incentive indicator on booking | From BRD-3 incentive criteria; booking incentive rules frozen | 118 |
| BRNB.110 | List in a shared path | In-system list and report with notification | 038 |

# Traceability

Every BRD-1 requirement is met by at least one FR. The table lists the 119 rows of the requirements baseline (R2): BRNB.001-115, the unnumbered row BRNB.100b, BRD 2.1.16 and the two out-of-scope items. Status is the build status of the traceability record (R3): BUILT, CONFIGURED (built capability, BDOI content pending), PARKED (seam built, waiting for a BDOI answer) or OUT.

<!-- table: widths=2.4,2.6,4.6,5.2,2.2 caption="BRD ID to FR, page, main screen and status" status=Status size=8 -->
| BRD ID | Page | FR | Main screen | Status |
|---|---|---|---|---|
| BRNB.001 | p.49 | FR-NB-001 | Products | CONFIGURED |
| BRNB.002 | p.49 | FR-NB-002 | New Account (Risk Items step) | CONFIGURED |
| BRNB.003 | p.49-50 | FR-NB-003 | New Account | CONFIGURED |
| BRNB.004 | p.50; updated p.18 | FR-NB-004, FR-NB-041 | Document Templates | BUILT |
| BRNB.005 | p.50-51 | FR-NB-014, FR-NB-050, FR-NB-051 | Proposal Requests | BUILT |
| BRNB.006 | p.51-52 | FR-NB-047, FR-NB-050 | New Proposal Request; Proposal Requests | BUILT |
| BRNB.007 | p.52-53 | FR-NB-052 | TSU Workbench | BUILT |
| BRNB.008 | p.53-54 | FR-NB-053 | Proposal Request page (Quotation Slip) | BUILT |
| BRNB.009 | p.54 | FR-NB-054, FR-NB-055 | Proposal Request page (Insurer Responses) | BUILT |
| BRNB.010 | p.54-55 | FR-NB-055 | Proposal Request page (Comparative Table) | BUILT |
| BRNB.011 | p.55 | FR-NB-121 | New Business Reports | BUILT |
| BRNB.012 | p.55 | FR-NB-120 | NB Dashboard | BUILT |
| BRNB.013 | p.55-56 | FR-NB-013, FR-NB-044, FR-NB-057 | Send via Email dialog; Outbound Messages | BUILT |
| BRNB.014 | p.56 | FR-NB-014, FR-NB-043, FR-NB-051 | Quotation page | BUILT |
| BRNB.015 | p.56 | FR-NB-015 | Notifications (bell) | BUILT |
| BRNB.016 | p.56 | FR-NB-016 | History tab of every record | BUILT |
| BRNB.017 | p.57 | FR-NB-056, FR-NB-057 | Proposal Request page (Proposal Slip) | BUILT |
| BRNB.018 | p.57 | FR-NB-126 | - | PARKED |
| BRNB.019 | p.58 | FR-NB-012 | Workflow panel (Void) | BUILT |
| BRNB.020 | p.58-59 | FR-NB-042 | Edit Quotation | BUILT |
| BRNB.021 | p.59-60 | FR-NB-043 | Quotations (For Review tab) | BUILT |
| BRNB.022 | p.60-61; updated p.18 | FR-NB-010, FR-NB-122 | Workflow panel; History tab | BUILT |
| BRNB.023 | p.61 | FR-NB-040 | Quotation Requests | PARKED |
| BRNB.024 | p.62-63 | FR-NB-019, FR-NB-045 | Bulk Uploads | BUILT |
| BRNB.025 | p.63-65 | FR-NB-064, FR-NB-065 | Edit Account | BUILT |
| BRNB.026 | p.65-66 | FR-NB-017 | Documents tab | BUILT |
| BRNB.027 | p.66-67 | FR-NB-110, FR-NB-115 | Booking Workbench (Ready to Book) | BUILT |
| BRNB.028 | p.67-68 | FR-NB-046 | Bulk Quotations | BUILT |
| BRNB.029 | p.68-69 | FR-NB-031, FR-NB-034, FR-NB-041, FR-NB-061 | New Client; New Quotation | BUILT |
| BRNB.030 | p.69-71 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | New Client | BUILT |
| BRNB.031 | p.71-72 | FR-NB-124 | Report runner (Print) | BUILT |
| BRNB.032 | p.72-73 | FR-NB-033, FR-NB-063 | New Client | BUILT |
| BRNB.033 | p.73-74 | FR-NB-067 | Accounts (Returned to me) | BUILT |
| BRNB.034 | p.74-75 | FR-NB-084 | Placement Workbench; Account Placement | BUILT |
| BRNB.035 | p.75-76 | FR-NB-013, FR-NB-104, FR-NB-105 | Send via Email dialog; Outbound Messages | BUILT |
| BRNB.036 | p.77-78 | FR-NB-111 | Booking Workbench | BUILT |
| BRNB.037 | p.78-79 | FR-NB-125 | Report runner (Download) | BUILT |
| BRNB.038 | p.79-80 | FR-NB-113 | Account page (Direct booking) | BUILT |
| BRNB.039 | p.80-81 | FR-NB-019, FR-NB-065 | Bulk Uploads | BUILT |
| BRNB.040 | p.93-94 | FR-NB-130 | Login | CONFIGURED |
| BRNB.041 | p.94 | FR-NB-040 | Quotation Requests | BUILT |
| BRNB.042 | p.94-95 | FR-NB-044, FR-NB-046 | Quotation page (Send) | BUILT |
| BRNB.043 | p.95-96 | FR-NB-041, FR-NB-044 | New Quotation (wizard) | BUILT |
| BRNB.044 | p.96-97 | FR-NB-030, FR-NB-045, FR-NB-065 | Bulk Quotation Acceptance; Clients | BUILT |
| BRNB.045 | p.97 | FR-NB-045, FR-NB-057 | Quotation page (Record Acceptance) | BUILT |
| BRNB.046 | p.97 | FR-NB-030 | Clients | BUILT |
| BRNB.047 | p.97-98 | FR-NB-035 | Bulk Upload (CLIENT_CREATE) | BUILT |
| BRNB.048 | p.98 | FR-NB-031 | New Client | BUILT |
| BRNB.049 | p.98-99 | FR-NB-032 | Edit Client | BUILT |
| BRNB.050 | p.99 | FR-NB-060 | Accounts | BUILT |
| BRNB.051 | p.99-101 | FR-NB-061, FR-NB-062, FR-NB-063 | New Account (six-step wizard) | BUILT |
| BRNB.052 | p.101-102 | FR-NB-065 | Bulk Account Creation | BUILT |
| BRNB.053 | p.102-103 | FR-NB-064 | Edit Account | BUILT |
| BRNB.054 | p.103-104 | FR-NB-064 | Edit Account | BUILT |
| BRNB.055 | p.104-105 | FR-NB-017 | Documents tab | BUILT |
| BRNB.056 | p.105 | FR-NB-018 | Documents tab (client, account) | BUILT |
| BRNB.057 | p.106 | FR-NB-123 | New Business Reports | BUILT |
| BRNB.058 | p.106-107 | FR-NB-067 | Accounts (Returned to me) | BUILT |
| BRNB.059 | p.107 | FR-NB-084 | Placement Workbench; Account Placement | BUILT |
| BRNB.060 | p.107-108 | FR-NB-104 | Insurance Advice register | BUILT |
| BRNB.061 | p.108-109 | FR-NB-110, FR-NB-111, FR-NB-115 | Endorsements | BUILT |
| BRNB.062 | p.109 | FR-NB-085 | Placement Workbench (Cancel Placement) | BUILT |
| BRNB.063 | p.109-110 | FR-NB-046 | Bulk Quotations | BUILT |
| BRNB.064 | p.110 | FR-NB-019, FR-NB-065 | Bulk Uploads | BUILT |
| BRNB.065 | p.110-111 | FR-NB-035 | Bulk Upload (CLIENT_CREATE) | BUILT |
| BRNB.066 | p.111-114 | FR-NB-063, FR-NB-065 | Bulk Account Creation | BUILT |
| BRNB.067 | p.114-115 | FR-NB-090, FR-NB-091 | CLPC Billing | BUILT |
| BRNB.068 | p.115 | FR-NB-091, FR-NB-092 | Account Placement (Payment Gate panel) | BUILT |
| BRNB.069 | p.115-116; updated p.19 | FR-NB-080 | Placement Workbench (For Placement) | BUILT |
| BRNB.070 | p.116 | FR-NB-103 | Insurance Advice (Generate Insurance Advice) | BUILT |
| BRNB.071 | p.116 | FR-NB-081 | Placement Slips (Send, Send Slips) | BUILT |
| BRNB.072 | p.116 | FR-NB-082 | Account Placement (Hold Cover panel) | BUILT |
| BRNB.073 | p.116-117 | FR-NB-100 | E-policy Upload (single, bulk) | BUILT |
| BRNB.074 | p.117 | FR-NB-101 | Extraction Review | BUILT |
| BRNB.075 | p.117-118 | FR-NB-122 | New Business Reports | BUILT |
| BRNB.076 | p.118 | FR-NB-112, FR-NB-115 | Booking Setup (Auto-book Rules) | BUILT |
| BRNB.077 | p.118-119 | FR-NB-105 | E-policy Dispatch | BUILT |
| BRNB.078 | p.119 | FR-NB-106 | E-policy Dispatch (Dispatch Report) | BUILT |
| BRNB.079 | p.119-120 | FR-NB-133 | My Approvals | BUILT |
| BRNB.080 | p.120 | FR-NB-011 | My Work | BUILT |
| BRNB.081 | p.120 | FR-NB-115, FR-NB-116 | Booked Invoice (Cancel Booking) | BUILT |
| BRNB.082 | p.121 | FR-NB-131 | Every screen | BUILT |
| BRNB.083 | p.121 | FR-NB-132 | Lists of Values (Broking Setup) | BUILT |
| BRNB.084 | p.121 | FR-NB-134 | Roles & Permissions | CONFIGURED |
| BRNB.085 | p.121-122 | FR-NB-135 | Access Requests | BUILT |
| BRNB.086 | p.122 | FR-NB-136 | Audit Trail (Administration) | BUILT |
| BRNB.087 | p.122 | FR-NB-130 | Login | CONFIGURED |
| BRNB.088 | p.122-127 | FR-NB-134 | Roles & Permissions | CONFIGURED |
| BRNB.089 | p.127 | FR-NB-136 | Audit Trail (Administration) | BUILT |
| BRNB.090 | p.4 | FR-NB-034 | Client page (KYC & Documents) | BUILT |
| BRNB.091 | p.4 | FR-NB-036 | Client page (Tags & Instructions) | BUILT |
| BRNB.092 | p.5 | FR-NB-020 | - | PARKED |
| BRNB.093 | p.5 | FR-NB-002 | New Account (Risk Items step) | CONFIGURED |
| BRNB.094 | p.5-6 | FR-NB-012, FR-NB-116 | Workflow panel (Void) | BUILT |
| BRNB.095 | p.6 | FR-NB-103 | Insurance Advice (Generate Insurance Advice) | CONFIGURED |
| BRNB.096 | p.6 | FR-NB-011, FR-NB-064 | My Work | BUILT |
| BRNB.097 | p.6-7 | FR-NB-011, FR-NB-119 | My Work; Booked Accounts Register | BUILT |
| BRNB.098 | p.7 | FR-NB-005 | Products (TSU Rules tab) | CONFIGURED |
| BRNB.099 | p.8 | FR-NB-037 | Client page (Linked Records) | BUILT |
| BRNB.100 | p.8-9 | FR-NB-117 | Service Invoices | BUILT |
| BRNB.100b | p.9 | FR-NB-117 | Service Invoices | BUILT |
| BRNB.101 | p.9-10 | FR-NB-034 | Client page (KYC & Documents) | BUILT |
| BRNB.102 | p.10 | FR-NB-041, FR-NB-047 | ARN chip; Account by ARN | BUILT |
| BRNB.103 | p.10-11 | FR-NB-083 | Account Placement (Hold Cover panel) | BUILT |
| BRNB.104 | p.11 | FR-NB-101 | Extraction Review | BUILT |
| BRNB.105 | p.11-12 | FR-NB-102 | Issuance Workbench | BUILT |
| BRNB.106 | p.12 | FR-NB-137 | Data Retention (Broking Setup) | BUILT |
| BRNB.107 | p.12-13 | FR-NB-118 | Booked Invoice | CONFIGURED |
| BRNB.108 | p.13-14 | FR-NB-110, FR-NB-119 | Pre-booking Confirmation | BUILT |
| BRNB.109 | p.14 | FR-NB-066 | New Account (contact step) | BUILT |
| BRNB.110 | p.14 | FR-NB-038 | KYC Reviews Due | BUILT |
| BRNB.111 | p.15 | FR-NB-113 | Account page (Direct booking) | BUILT |
| BRNB.112 | p.15-16 | FR-NB-062, FR-NB-114 | Booked Invoice | BUILT |
| BRNB.113 | p.16-17 | FR-NB-068 | FFY Register | BUILT |
| BRNB.114 | p.17 | FR-NB-069, FR-NB-092 | New Account (Payment) | BUILT |
| BRNB.115 | p.18-19 | FR-NB-010, FR-NB-120, FR-NB-122 | Workflow panel; History tab | BUILT |
| BRD 2.1.16 | p.180 | FR-NB-086 | Placement Workbench (Reactivate) | BUILT |
| OOS-1 | p.83 | Section 1.2 (out of scope) | - | OUT |
| OOS-2 | p.83 | Section 1.2; FR-NB-047, 090 | - | OUT |

Page numbers are pages of the BRD PDF (R1). The traceability record R3 names the module, API and test class of each row.

# Sign-off

By signing, BDOI confirms that this FRS describes the New Business functions it expects in BIBS, accepts the assumptions in section 10.1 and notes the differences in section 10.4. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Product Owner", organisation: BDOI}
  - {name: "", role: "Head, Marketing Business Services and System Support", organisation: BDOI}
  - {name: "", role: "Unit Head, Processing", organisation: BDOI}
  - {name: "", role: "Head, Retail Marketing", organisation: BDOI}
  - {name: "", role: "Unit Head, Combank and Corbank", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
