---
# Source of the Functional Requirements Specification for BRD-5, Volume 2 (Disbursement, Payment Requests, ACSL).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD05_ACCOUNTING_VOL2.md
title: Disbursement, Payment Requests and ACSL - Volume 2
subtitle: BRD-5 Accounting, Disbursement, Accounting Controls and Subsidiary Ledger - Volume 2 - Disbursement, Marketing Refund and Cash-Advance Requests, ACSL
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-05
name: Accounting Disbursement ACSL Vol2
doc_id: BIBS-FRS-BRD-05-V2
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-5 Vol. 2 - Disbursement, Requests, ACSL
output: FRS/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx
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
    change: First issue for BDOI review; aligned with the as-built Disbursement (A1-DSB), Payment Requests and ACSL (A1-PRQ) modules and the remittance and booking changes (A1-OPSX)
distribution:
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Disbursement Section (Processors, Team Leaders, Approvers)", role: Business owner, organisation: BDOI, purpose: Review of the Disbursement FRs}
  - {name: "Accounting Controls and Subsidiary Ledger (ACSL)", role: Business owner, organisation: BDOI, purpose: Review of the ACSL FRs}
  - {name: "Marketing (AOs, reviewers, approvers)", role: Business owner, organisation: BDOI, purpose: Review of the Payment Request FRs}
  - {name: "Human Resources", role: Business user, organisation: BDOI, purpose: Review of the cash-advance approval}
  - {name: "Operations (Cashiering, Remittance)", role: Business user, organisation: BDOI, purpose: "Review of the refund validation, remittance and deduction FRs"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
---

# Introduction

## Purpose

This Functional Requirements Specification (FRS) states how BIBS (BDOI Broker System, on iNXT BrokerVerse) meets the Disbursement, Marketing refund and cash-advance request, and Accounting Controls and Subsidiary Ledger (ACSL) requirements of BRD-5 for BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It turns each BRD requirement into functional requirements with actors, flows, rules, validations, screens, fields, notifications, audit and acceptance criteria.

BDOI uses this document to confirm that the system behaves as the business expects. The project team uses it to test and to prepare user acceptance testing (UAT). Every functional requirement (FR) cites the BRD requirement it meets and the BRD page.

Disbursement, Payment Requests and ACSL are built. Where the delivered behaviour differs from the BRD text, the FR describes the delivered behaviour and records the difference in a note; section 1.7 lists all differences in one table. The remittance and booking changes of the wave A1-OPSX (CPC2, the early-incentive service invoice, remittance deductions, restoring a remittance batch after a DV cancellation, the root invoice in booking and the invoice family) are built; the parts that wait for BDOI data are marked as parked in the FR.

## Scope

<!-- table: widths=4,9,4 caption="Scope of Volume 2" -->
| Area | In scope | Source |
|---|---|---|
| Access | Access, log-in and session warnings of the Disbursement, Marketing and ACSL users | DIS, MKT, ACSL 1.1.0-1.1.3 |
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

**Out of scope for this volume:** FRBS (Accounting) and business / system administration (Volume 1). The log-in to BDO Business Online Banking (DIS 2.17.1) is outside BIBS and marked OUT in the BRD baseline. Bank channels (BOB, TPD / ACA, branch processing) are external; BIBS produces the files and forms and records the results.

## Volumes of this FRS

BRD-5 has 277 requirement IDs and is issued in two volumes with a shared cover note (`BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx`).

<!-- table: widths=3.4,6.8,4.2,2.2 caption="Volumes of the BRD-5 FRS" -->
| Document | Content | BRD IDs | FR prefix |
|---|---|---|---|
| Cover note | Why two volumes; how to read them; common references; sign-off route | - | - |
| Volume 1 | FRBS (Accounting), General Ledger platform, report pack, service fee, business and system administration | FRBS 60, BASAU 25 (85) | FR-AC |
| Volume 2 (this document) | Disbursement, Payment Requests, ACSL | DIS 111, MKT 36, ACSL 45 (192) | FR-DS, FR-PQ, FR-AS |

The accounting events of BRD-5 and their seed entries, the chart and the posting rules are described in Volume 1 (section 5.5); this volume refers to them.

## References

<!-- table: widths=1.2,7.4,3.6,5.4 caption="Reference documents" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Accounting, Disbursement and ACSL BRD (main BRD), pages 40-153; signed scan pp.154-267 | v1.0, 23-Jul-2025; approved Jul to Oct 2025 | `docs/source-documents/Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip` |
| R2 | Addendum 1, pages 28-39 (signed copy pp.16-27) | v1.0, 18-Dec-2025; signed 13-Jan-2026 | same file |
| R3 | Addendum 2 (Workshop), pages 1-15 (scanned) | v1.0, 10-Apr-2026; signed 8 to 15-Apr-2026 | same file |
| R4 | BDOI Accounting, Disbursement and ACSL (BRD-5) requirements baseline and fit/gap | current | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| R5 | Accounting, Disbursement and ACSL build design, including section 17 and the as-built notes A1-GL, A1-PRQ, A1-DSB, A1-FRBS, A1-OPSX | current | `docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |
| R7 | BRD-5 FRS Volume 1 (FRBS and Accounting) | v1.0 | `docs/deliverables/out/Drop-1_Transactional/FRS/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx` |
| R8 | BRD-2 Operations FRS (remittance, cashiering, commission) | v1.0 | `docs/deliverables/out/Drop-1_Transactional/FRS/BIBS_FRS_BRD-02_Operations_v1.0.docx` |
| R9 | BRD-4 Collections FRS (collector requests to Cashiering) | v1.0 | `docs/deliverables/out/Drop-1_Transactional/FRS/BIBS_FRS_BRD-04_Collections_v1.0.docx` |

Page references ("p.69") are pages of the BRD-5 PDF; "Add.1" is Addendum 1 (pp.28-39) and "Add.2" the Workshop Addendum (pp.1-15). The BRD numbers the Disbursement access rows "BRD 1.1.0-1.1.3"; this FRS writes them DIS 1.1.0-1.1.3. Duplicate numbers renamed by Addendum 1 are used in their new form (DIS 2.17.4, DIS 2.24.2, ACSL 2.5.5); the second row printed "DIS 3.30.1" (headcount report) is written DIS 3.30.2.

## Definitions and acronyms

```glossary
ACA: Automatic credit arrangement processed by BDO TPD from the DCTF
ACSL: Accounting Controls and Subsidiary Ledger unit of Comptrollership
AO: Account Officer (Marketing)
AQnn: Open question on BRD-5 raised by the project team (section 10.3)
AR: Acknowledgement Receipt
ATD: Authority to Debit the BDOIR main account
BOB: BDO Business Online Banking
CA / SA: Current or savings account of a client at BDO, used for refunds
CPC2: Incentive earned by BDOI on packaged Fire and Motor products, agreed with the insurers, on top of the regular commission
CT / TT: Credit Ticket / Telegraphic Transfer
CTA: Credit to account
CWT: Creditable withholding tax (BIR Form 2307)
DCTF: Direct Credit Transaction File sent to TPD for ACA processing
DTIP: Due to insurer (premium payable)
DV: Disbursement voucher (DV-yyyy-n)
EOD: End of day of Disbursement
EWT: Expanded withholding tax withheld at payment
Instrument: The payment document of an approved DV (check, ATD, CTA extraction, MC / DD, CT / TT, online banking)
MC / DD: Manager's check / demand draft
OR: Official Receipt
Payee request: A request to create or change a payee (from a refund request, from Disbursement or from a payment request whose payee is not found)
Proforma entry: The accounting entry of a DV proposed by the rules and editable before approval
RFP: Request for Payment (cash advance); RFP-yyyy-n
Root invoice: The original invoice number shared by an invoice's endorsements and cancellations
RRF: Refund Request Form; RRF-yyyy-n
SOA: Statement of account sent by an insurer
TPD: BDO Transaction Processing Department
UPP: Unapplied payment
```

## How to read the functional requirements

Each FR in section 4 has the same parts as in Volume 1: a header table (BRD trace with page, actor, priority, fit class of the baseline R4, screens, API), description, preconditions, main flow, alternate and exception flows, business rules (*Configurable* or *Fixed*), validations with the message and its code ("-" for a screen or platform check), screens and fields, notifications, audit and numbered acceptance criteria. Text in angle brackets (`<DV>`) is replaced by the value. Every BRD-5 row carries the priority **Must have**. API paths start with `/api/v1`; "..." in a header table stands for the module path given in the section introduction.

Where a built function waits for BDOI data, the Fit column adds "parked" with the question it waits for (for example AQ24); the FR describes the delivered behaviour until the answer.

> [!NOTE]
> Bank channel layouts, form layouts, the payee migration file, the real accounting entries and several lists are BDOI data not given yet (AQ02, AQ09-AQ19, AQ21-AQ25). BIBS holds them as configuration or draft templates, so an answer does not need a new build.

<!-- table: widths=2.6,14 caption="Fit classes (from the requirements baseline, R4)" status=Class -->
| Class | Meaning |
|---|---|
| FIT | Worked with the platform before BRD-5 |
| CONFIGURE | Needed set-up only |
| CHANGE | Extended an existing capability |
| NEW | A capability that did not exist before BRD-5 |
| OUT | Out of scope per the BRD |

## Differences between the built behaviour and the BRD

<!-- table: widths=2.2,5.4,6.8,2.2 caption="Recorded differences (built behaviour against the BRD)" size=8.5 -->
| BRD ID | BRD says | BIBS does | Ref. |
|---|---|---|---|
| DIS 3.25.0 | Auto-reject a request whose payee is not maintained and notify the requester | By default the request waits as No Payee, a payee request is raised and the alert DISB_PAYEE_NO_MATCH is sent; it resumes when the payee is authorised. DISB_NO_PAYEE_ACTION = RETURN returns it at once as the BRD says | AQ11, AQ12 |
| DIS 2.2.4 | Delete payee details | Only a draft payee never used is deleted; any other payee is deactivated with authorisation | AQ11 |
| DIS 2.7.6, 3.27.0 | Proforma entry editable | Lines are editable until approval; edited lines are marked Edited and shown to the approver | AQ13 |
| DIS 2.8.1, 2.16.2 | Check statuses; checks printed at EOD | A check takes the next leaf of the paying account's cheque book when printed; the layout is a draft template until AQ14 | AQ14 |
| DIS 2.16.1 | DCTF with header and details | Header and 89-character details as Appendix B; no trailer or totals until AQ09 | AQ09 |
| DIS 2.22.0, 3.26.4, 3.26.7 | Upload bank files (deposited checks, credited accounts, BOB approvals) | The uploads take a minimal CSV (DV or check number, amount, date) until the bank layouts are given | AQ09 |
| DIS 2.20.0 | Cancel an approved DV and regularise the source data | The DV journal is reversed and the request goes back to its source; for a remittance DV, Remittance reverses the batch postings, gives the deductions back and returns the batch to review; it is sent again under a new cycle | AQ15 |
| DIS 2.17.x | Funding through BOB with a verifier and two approvers | Built as a BIBS workflow with four eyes; the BOB transaction itself is done in BOB and its reference recorded | AQ10 |
| DIS 2.10.x | OR / AR tagging on DVs | Every approved DV counts as unregularised until tagged; which DVs need an OR / AR back is open | AQ17 |
| DIS 2.11.x | CWT tagging | The tag records the certificate on the DV; the insurer certificates on commission are kept in one register in Tax (Certificates Received) | AQ16 |
| DIS 3.27.2, ACSL 2.16.0 | One invoice number for related transactions | Endorsements and cancellations keep their own BIR invoice number and carry the root invoice number; booking sets the root invoice number and Invoice 360 shows the family | AQ29 |
| DIS 3.29.2 | CPC2 per remittance, deducted and posted as income | Built from the TSU CPC2 criteria (rate on the basic premium remitted, output VAT); base, VAT treatment, fixed-amount and rule criteria wait for BDOI | AQ24, OQ39, PQ04 |
| DIS 3.29.1 | Service invoice of the early incentive issued automatically with 2% withholding | Built once per batch; the accounting of the insurer's 2% withholding is parked and the incentive is still deducted in full from the remittance | AQ25 |
| DIS 3.29.0 | CPC2 report | Not built; the CPC2 amounts are on the remittance batch and its schedule | Gap G4 |
| MKT 1.7.0 | Unapplied payment report for Marketing | Marketing runs the Cashiering reports (OPS_REPORT_VIEW); no Marketing report with a segment filter | AQ18 |
| MKT 1.12.0, 1.13.0 | Upload .txt files; preview documents side by side | .txt is not an accepted attachment type; files open one at a time in the viewer | - |
| MKT 1.10.0 | Populate the details of the forms (list empty) | The fields of Appendix D are used; all accounts of one refund request belong to one client | AQ18 |
| MKT 1.19.0, 1.16.3 | Cancellation of a disbursed check routed to Disbursement | After approval the request is handed to the Disbursement approvers (hand-off DV_CANCELLATION); the approver cancels the DV, which closes the hand-off | AQ15 |
| MKT 2.25.0 | CA / SA on the client record | Recorded on approval without duplicates; the BDO account number is checked as 10 to 16 digits until AQ19 | AQ19 |
| Appendix D | Cash-advance liquidation form | Built (liquidation, return, post) although its scope is to be confirmed | AQ18 |
| ACSL 2.4.0 | Upload SOA files that trigger reconciliation | The SOA file is read in one request (up to ACSL_SOA_MAX_ROWS rows) and reconciled once loaded; layouts per insurer are configuration | AQ21 |
| ACSL 2.14.3, 2.14.4 | Aging and schedule reports per account family | Not built as ACSL reports; the premium receivable schedules with ageing are delivered as account schedules (Volume 1) | Gap G3 |
| ACSL 2.9.2 | Remittance deduction on insurer confirmation | Built in Remittance; the deduction stays Confirmed while batches consume it and is Applied when every batch that used it has the insurer OR; sources and spanning batches wait for BDOI | AQ23 |
| ACSL 2.13.2 | GL-SL reconciliation also at period end | Nightly and on demand; the period-end check is not built | - |

# Business context and process overview

## Business context

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

The grants follow the role matrices read from the scanned pages (pp.216-217, 227, 240); BDOI confirms them through AQ28.

## Permissions

<!-- table: widths=5,2.6,9 caption="Volume 2 permissions" -->
| Permission | Action class | Allows |
|---|---|---|
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

Segregation of duties enforced by the system: a DV is never checked by its processor or approved by its processor or checker; a funding request is verified by a user other than the maker and approved by two further users; a payee is never authorised by its maker; a status edit is never approved by its requester; a Payment Request is never endorsed or approved by the user who raised or last moved it; a liquidation is checked by someone other than the employee; an ACSL correction is reviewed by someone other than its preparer and approved by someone other than its maker.

# Functional requirements

Section 4.1 covers Disbursement (FR-DS), section 4.2 Payment Requests of Marketing (FR-PQ) and section 4.3 ACSL (FR-AS).

## Disbursement

### Access

```fr
id: FR-DS-001
title: Access Disbursement, Payment Requests and ACSL with a user profile
brd: [DIS 1.1.0 (p.68), DIS 1.1.1 (p.69), DIS 1.1.2 (p.69), DIS 1.1.3 (p.69), MKT 1.1.0 (p.105), MKT 1.1.1 (p.105), MKT 1.1.2 (p.105), MKT 1.1.3 (p.105), ACSL 1.1.0 (p.114), ACSL 1.1.1 (p.115), ACSL 1.1.2 (p.115), ACSL 1.1.3 (p.115)]
actor: Disbursement, Marketing and ACSL users
priority: Must have
fit: "FIT (1.1.0, 1.1.1), CONFIGURE (1.1.2, 1.1.3)"
screens: Login; menu group Finance (Disbursement, Refund & Cash Advance Requests, ACSL)
api: POST /api/v1/auth/login
description: Users reach BIBS from any BDO-issued device and log in with their own profile; the menu shows only the screens their roles allow. BIBS warns after 15 minutes of inactivity (SESSION_IDLE_WARNING_MINUTES) and 30 minutes before the forced log-out (SESSION_EXPIRY_WARNING_MINUTES). The rules are those of Volume 1, FR-AC-001 and FR-AC-002.
preconditions:
  - The user has an active account with a Disbursement, Payment Request or ACSL role.
main_flow:
  - The user logs in with the user ID and password.
  - BIBS opens the home page with the Finance menu of the user's roles.
  - When inactive, the user receives the warnings before the time-outs.
rules:
  - [R1, "Log-in, lock-out (3 attempts) and session rules of BRD-1.", Configurable, Session parameters]
validations:
  - [User ID or password wrong, Invalid user ID or password, "-"]
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

API paths of this section are under `/api/v1/disbursement` unless stated.

```fr
id: FR-DS-010
title: Maintain payees with authorisation
brd: [DIS 2.2.0 (p.69), DIS 2.2.3 (p.70), DIS 2.2.6 (p.71), DIS 2.2.7 (p.71)]
actor: Disbursement Team Leader (maintain); Disbursement Approver (authorise)
priority: Must have
fit: NEW
screens: Payees (tabs Active, For Authorisation, Drafts, Inactive, Payee Requests); Payee
api: "GET/POST .../payees; PUT .../payees/{id}; POST .../payees/{id}/submit, /authorize, /deactivate, /reactivate; .../payees/{id}/accounts"
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
fit: "CHANGE (2.2.2), NEW (2.2.5)"
screens: Payee
api: .../payees
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
fit: NEW
screens: Payees (Payee Requests)
api: "GET .../payee-requests; POST .../payee-requests/{id}/close"
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
  - "The request keeps its source, payload and the payee created."
acceptance:
  - A remittance request for an insurer without payee creates a NO_MATCH payee request; authorising the payee resumes the request.
```

```fr
id: FR-DS-013
title: Delete or deactivate payees
brd: [DIS 2.2.4 (p.70)]
actor: Disbursement Team Leader; Approver
priority: Must have
fit: NEW
screens: Payee (Delete, Request Deactivation)
api: "DELETE .../payees/{id}; POST .../payees/{id}/deactivate, /reactivate"
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
fit: NEW
screens: Payees; Disbursement Uploads (Payee Migration); Disbursement Reports (Payee report)
api: ".../payees?status=; bulk handler DISB_PAYEE_MIGRATION; report DSB-PAYEE"
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
fit: "NEW (2.6.0, 2.6.2), CHANGE (3.25.0)"
screens: Disbursement Workbench (System Requests, No Payee)
api: "Operations port DisbursementGateway; GET .../requests; POST .../requests/{id}/voucher, /return"
description:
  - BIBS modules ask for payments through the Operations disbursement gateway - remittance batches, cashiering refunds and 2307 releases, commission pass-ons, Payment Requests refunds and cash advances, service-fee payouts. Each request (DSR-yyyy-n) carries the RFP number, payee, disbursement type, amount, currency, attachments, the root invoice and the accounting references to settle.
  - When the payee is maintained, BIBS creates the DV at once (FR-DS-030). Refund and remittance requests go straight to the Approver (DISB_AUTO_APPROVER_ROUTING); others start In Process for the Processor.
  - When the payee is not maintained, the request waits under No Payee with a NO_MATCH payee request and the alert DISB_PAYEE_NO_MATCH, and resumes when the payee is authorised. With DISB_NO_PAYEE_ACTION = RETURN it is returned to its source at once.
  - Every DV stage and payment is reported back to the source module (DisbursementStatusChanged).
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
  - [R1, "Types built straight to the approver - REFUND, REMITTANCE.", Configurable, Parameter DISB_AUTO_APPROVER_ROUTING]
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
  - "The request keeps its source, payload and history."
acceptance:
  - An approved remittance batch creates a remittance DV directly For Approval.
  - A refund request for a client without payee waits under No Payee and resumes when the payee is authorised.
```

> [!NOTE] Difference from the BRD
> DIS 3.25.0 asks to auto-reject requests without a maintained payee. The delivered default keeps them waiting so the payee can be added without the source re-sending; the parameter DISB_NO_PAYEE_ACTION = RETURN gives the BRD behaviour (AQ11, AQ12).

```fr
id: FR-DS-021
title: Classify requests by disbursement type
brd: [DIS 3.25.1 (p.96)]
actor: System
priority: Must have
fit: NEW
screens: Disbursement Workbench; DV
api: "-"
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
fit: NEW
screens: Disbursement Workbench (No Payee); Disbursement Reports
api: Report DSB-PAYEE-NOMATCH
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
fit: NEW
screens: Encode Payment Request
api: "POST .../requests (encode)"
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
fit: "CHANGE (2.5.0), FIT (2.5.1)"
screens: Disbursement Uploads (Payment Requests)
api: Bulk handler DISB_REQUESTS
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
fit: "NEW; FIT (2.4.4); CHANGE (2.4.2, 2.7.2)"
screens: Disbursement Workbench; Disbursement Voucher (tabs Details, Entry, Instrument, OR / AR and CWT, Documents, E-mails)
api: "GET .../summary; GET .../requests; GET .../vouchers?stage=&receivedFrom=&receivedTo="
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
fit: NEW
screens: Disbursement Voucher (Details)
api: "POST .../requests/{id}/voucher; PUT .../vouchers/{id}/terms"
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
title: Build and edit the proforma entry
brd: [DIS 2.7.6 (p.80)]
actor: System; Disbursement Processor
priority: Must have
fit: NEW
screens: Disbursement Voucher (Entry)
api: "GET/PUT .../vouchers/{id}/proforma; POST .../vouchers/{id}/proforma/reset"
description: BIBS builds the proforma entry of the DV from the accounting rule of its type (Volume 1, section 5.5). The Processor may edit the lines - account, side, party, cost centre, amount - before approval; each edited line is marked Edited and the Approver sees that the entry was changed. Each line is checked for posting eligibility and the entry must balance. "Rebuild from rule" discards the edits.
preconditions:
  - "The DV is In Process or returned."
main_flow:
  - The Processor opens the Entry tab.
  - The Processor edits a line and saves.
  - BIBS validates the lines and the balance.
alternate_flows:
  - Reset. The Processor rebuilds the entry from the rule.
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
fit: CHANGE
screens: Disbursement Voucher (Entry - Apply Allocation)
api: POST .../vouchers/{id}/allocation
description: For supplier and employee expenses, the Processor attaches an allocation - lines of account, cost centre and amount - that builds the expense lines of the proforma. The payee's default cost centre fills lines without one; the cost-centre rules of Volume 1 (FR-AC-054) apply at posting. The allocation is visible in review and logged.
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
fit: NEW
screens: Disbursement Voucher (Instrument)
api: ".../vouchers/{id}/instrument/print, /document, /release, /email, /debited, /received"
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
fit: NEW
screens: Disbursement Voucher (Instrument - Print, E-mail, Debited)
api: ".../vouchers/{id}/instrument/print, /email, /debited"
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
fit: NEW
screens: Disbursement Voucher (Instrument)
api: ".../vouchers/{id}/instrument/print, /received, /release, /debited"
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
fit: NEW
screens: Disbursement Voucher (Submit)
api: POST .../vouchers/{id}/submit
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
fit: NEW
screens: Setup > Employees; Report Centre (Headcount per Cost Centre)
api: "GET/POST/PUT /api/v1/organization/employees; report ORG-HEADCOUNT-CC"
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
fit: NEW
screens: Disbursement Workbench (For Review); Disbursement Voucher
api: "POST .../vouchers/{id}/submit-for-approval, /{id}/route; return from the workflow panel"
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
fit: NEW
screens: Disbursement Workbench (For Approval - bulk); Disbursement Voucher (Approve)
api: "POST .../vouchers/{id}/approve; POST .../vouchers/approve (bulk)"
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
  - "The source module is notified (DisbursementStatusChanged)."
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
fit: NEW
screens: Disbursement Voucher (Reject)
api: "POST .../vouchers/{id}/reject"
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
fit: NEW
screens: Disbursement Workbench (search); Disbursement Voucher (Cancel)
api: "POST .../vouchers/{id}/cancel"
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
fit: NEW
screens: Disbursement Voucher (Cancel approved DV)
api: "POST .../vouchers/{id}/cancel (approved DV)"
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
fit: NEW
screens: Disbursement Workbench (Unregularised); Disbursement Reports
api: Report DSB-UNREGULARIZED
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
fit: NEW
screens: Disbursement Voucher (Instrument)
api: ".../vouchers/{id}/instrument/release, /email, /debited, /received"
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
fit: NEW
screens: Disbursement Voucher (Instrument - Request Status Edit); status edits list
api: ".../vouchers/{id}/instrument/status-edits; .../status-edits/{id}/approve"
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
fit: NEW
screens: Disbursement Voucher (Instrument - status history)
api: "Instrument service; bulk handler DISB_BOB_APPROVED"
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
fit: NEW
screens: Disbursement Uploads (Negotiated Checks, Credited Accounts, BOB Approvals)
api: "Bulk handlers DISB_CHECKS_NEGOTIATED, DISB_CTA_CREDITED, DISB_BOB_APPROVED"
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
fit: NEW
screens: Disbursement Voucher (Instrument - Re-issue); Disbursement Reports
api: "Job DISB_CHECK_STALE; POST .../vouchers/{id}/instrument/reissue"
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
fit: NEW
screens: Disbursement Voucher (Instrument, Entry)
api: "Events DISB_CHECK_NEGOTIATED, DISB_CHECK_STALE"
description: With DISB_CHECK_CLEARING = ON, an approved check DV credits checks outstanding (account 2241). When the check is negotiated BIBS posts checks outstanding against the bank; when it is stale, checks outstanding against Miscellaneous Liability - stale checks of the payee. A re-issue posts the stale liability against the paying account. The entries flow to FRBS through the ledger.
preconditions:
  - "The check is negotiated or stale."
main_flow:
  - The upload or the job changes the check status.
  - BIBS posts the event with its seed rule (Volume 1, section 5.5, rows 8-10).
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
fit: NEW
screens: Disbursement Voucher (OR / AR and CWT)
api: POST .../vouchers/{id}/tags/receipt
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
fit: NEW
screens: Disbursement Voucher (OR / AR and CWT); Tax & Statutory > Certificates Received
api: "POST .../vouchers/{id}/tags/cwt; /api/v1/tax/received-certificates"
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
fit: FIT
screens: Tax & Statutory > BIR Form 2307
api: /api/v1/tax/2307
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
fit: NEW
screens: Disbursement End of Day (Run End of Day, Runs, outputs)
api: "POST .../eod/runs; GET .../eod/outputs/{id}"
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
fit: CHANGE
screens: Disbursement End of Day (DCTF output)
api: "EOD output DCTF; notification format DCTF"
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
fit: CHANGE
screens: Disbursement End of Day (check batch, vouchers); Disbursement Voucher (Document)
api: ".../eod/outputs/{id}; GET .../vouchers/{id}/document"
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
fit: CHANGE
screens: Disbursement End of Day (Confirm); Disbursement Voucher (E-mails)
api: "POST .../eod/runs/{id}/confirm; job DISB_EOD_CONFIRMATION"
description: After the end of day, BIBS e-mails to each payee of the day the payment advice (template DSB_PAYMENT_ADVICE). The confirmations of a run are sent once; the e-mails are logged on the DVs.
preconditions:
  - "The run is completed and not confirmed."
main_flow:
  - The user clicks **Confirm** (or the scheduler runs it).
  - BIBS sends the advices and marks the run CONFIRMED.
rules:
  - [R1, "One confirmation per run.", Fixed, "-"]
  - [R2, "Attaching the remittance schedule to the insurer's advice is parked.", Fixed, "-"]
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
fit: "NEW; OUT (2.17.1)"
screens: Account Funding; Funding Request
api: "POST .../funding, /{id}/submit, /{id}/verify, /{id}/approve; decline, return and cancel from the workflow panel"
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
fit: "FIT (2.23.0, 2.23.1), CHANGE (2.23.2)"
screens: Bank Accounts and Checks (cheque books)
api: "POST .../banks/{id}/cheque-books; PUT .../cheque-books/{id}"
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
fit: "FIT (2.24.0, 2.24.1), CHANGE (2.24.2)"
screens: Bank Accounts and Checks
api: ".../banks; POST .../banks/{id}/status, /authorize"
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
fit: "NEW (2.3.0); FIT (2.3.1-2.3.8); CHANGE (2.3.9)"
screens: Disbursement Reports; Report Centre
api: "Reports with DSB codes (section 6.1)"
description: The Disbursement reports (section 6.1) run at any time for a date or period; the user views the details, copies them, exports to XLSX, ODS or PDF, saves the file, previews and prints with print options (paper, orientation, fit to width), as in Volume 1, FR-AC-020 to FR-AC-023.
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
fit: NEW
screens: Disbursement End of Day (outputs); Disbursement Reports
api: "Reports DSB-EOD-REMIT, DSB-EOD-REFUND, DSB-EOD-SUMMARY, DSB-EOD-SUPPLIER, DSB-EOD-EMPLOYEE, DSB-EOD-OTHER; job DISB_EOD_REPORTS"
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
fit: NEW
screens: Disbursement Reports
api: "Reports DSB-MASTERLIST, DSB-UNRELEASED-CHECKS, DSB-CWT-COMMISSION, DSB-ATD, DSB-ML-STALE, DSB-CASH-FLOW"
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
fit: "NEW (3.28.1), CHANGE (3.28.4)"
screens: Disbursement Reports
api: "Reports DSB-PAYEE, DSB-UPLOAD-FALLOUT, DSB-PAYEE-NOMATCH"
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
fit: "CHANGE; built, CPC2 base and criteria parked (AQ24, OQ39, PQ04)"
screens: Remittance batch (CPC2 column and totals, Settlement tab); remittance payment request and schedule
api: "Event OPS_REMIT_CPC2; GET /api/v1/remittance/batches/{id} (line cpc2Code, cpc2Rate, cpc2, cpc2Vat)"
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
fit: "CHANGE; built, accounting of the 2% withholding parked (AQ25)"
screens: Remittance batch (Settlement tab - SI); Service invoices; Service invoice types (trigger ON_INCENTIVE)
api: "Booking service invoice type EARLY_INCENTIVE (trigger ON_INCENTIVE)"
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
  - [R3, "The entry of the insurer's 2% withholding (Dr 1611 / Cr 2211) is parked (AQ25); the incentive is still deducted in full from the remittance.", Fixed, "-"]
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
fit: "NEW; not built (Gap G4)"
screens: "-"
api: "Report DSB-CPC2-INCENTIVE (not built)"
description: The BRD asks for a CPC2 report by user, product and role with the calculation breakdown, consistent with the accounting, exportable and logged. The report is not built. The CPC2 of each line (code, rate, amount, VAT) is on the remittance batch, its payment request and schedule, and the income is in the ledger (FR-DS-090).
preconditions:
  - "-"
main_flow:
  - Not built.
rules:
  - [R1, "Layout to be agreed with the CPC2 definition (AQ24).", Fixed, "-"]
validations: []
notifications:
  - "None."
audit:
  - "-"
acceptance:
  - "Gap G4: to be accepted when built."
```

```fr
id: FR-DS-093
title: Link related transactions to one invoice number
brd: [DIS 3.27.2 (p.100)]
actor: System; Operations, Disbursement and ACSL users
priority: Must have
fit: CHANGE
screens: Invoice Search; Invoice 360 (Invoice Family tab); booking invoice (Root Invoice chip); ACSL Case; Disbursement Voucher
api: "GET /api/v1/ops/invoices/{no}/family; GET /api/v1/ops/invoices"
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

API paths of this section are under `/api/v1/payment-requests` unless stated. Access (MKT 1.1.0-1.1.3) is FR-DS-001.

```fr
id: FR-PQ-001
title: Receive refund and cash-advance requests in Requests Home
brd: [MKT 1.2.0 (p.105), MKT 1.3.0 (p.106), MKT 1.4.0 (p.106), MKT 1.5.0 (p.106), MKT 1.6.0 (p.106)]
actor: Marketing Processor, Reviewer and Approver; HR
priority: Must have
fit: "NEW; FIT (1.6.0)"
screens: Requests Home (tabs by stage with counts); Request
api: "GET .../requests; GET .../requests/counts; GET .../requests/{id}"
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
fit: "CHANGE (1.7.0), FIT (1.7.1-1.7.3)"
screens: Report Centre (Operations reports)
api: "Report Centre; e.g. CSH-MINBAL-EXCESS"
description: The Marketing roles hold OPS_REPORT_VIEW and run the Operations reports of Cashiering on unapplied payments for a date range, download them as XLSX, save the file and print it (Volume 1, FR-AC-020 to FR-AC-023). A report of unapplied payments restricted to the Marketing user's segment is not built; its layout waits for AQ18.
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
> MKT 1.7.0 expects an unapplied-payment report for Marketing. Marketing uses the Cashiering reports without a segment filter; a Marketing-specific report is not built (AQ18).

```fr
id: FR-PQ-003
title: Fill in the Refund Request Form
brd: [MKT 1.10.0 (p.107; Appendix D)]
actor: Marketing Processor (AO)
priority: Must have
fit: "NEW; built, form fields and approvers parked (AQ18)"
screens: New Refund Request; Request (Refund)
api: "POST .../requests/refunds; PUT .../requests/{id}/refund; GET .../requests/{id}/form"
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
fit: "NEW; built, form fields and approvers parked (AQ18)"
screens: New Cash Advance; Request (Cash advance)
api: "POST .../requests/cash-advances; PUT .../requests/{id}/cash-advance"
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
fit: "NEW (1.8.0), CHANGE (1.9.0)"
screens: Request (Assign, Return)
api: "POST .../requests/{id}/assign; return from the workflow panel"
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
fit: NEW
screens: Request (Validations); ACSL Cases; Cashiering tasks
api: "POST .../requests/{id}/submit (for validation); GET .../requests/{id}/validations; POST .../requests/{id}/validations/{validationId}/result"
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
fit: "FIT (1.12.0), CHANGE (1.13.0, 2.22.0)"
screens: Request (Documents)
api: "/api/v1/attachments (entity PAYMENT_REQUEST)"
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
fit: NEW
screens: Request (Submit, Endorse); Requests Home (bulk endorse)
api: "POST .../requests/{id}/submit; POST .../requests/{id}/endorse"
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
fit: NEW
screens: Request (Approve, Return); Requests Home (bulk approve)
api: "POST .../requests/{id}/approve; return and cancel from the workflow panel"
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
fit: NEW
screens: Request (Cancel)
api: Cancel from the workflow panel
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
fit: "NEW; built, hand-off to Disbursement parked (AQ15)"
screens: Cancel a Check; Request (Check cancellation)
api: "POST .../requests/check-cancellations"
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
fit: NEW
screens: Requests Home; Request (History, Disbursement); Report Centre
api: "Reports PRQ-STATUS, PRQ-REGISTER"
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
fit: NEW
screens: Request (Disbursement tab)
api: "Event DisbursementStatusChanged"
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
fit: NEW
screens: New Refund Request
api: "-"
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
fit: NEW
screens: Request (Disbursement tab)
api: "Operations port DisbursementGateway"
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
fit: "CHANGE; built, account-number format parked (AQ19)"
screens: Client (Payout accounts); New Refund Request (payout fields)
api: "GET .../payout-accounts"
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
fit: "NEW; built, scope and accounts parked (AQ18, AQ02)"
screens: Request (Liquidation); Liquidation Accounts
api: "PUT .../requests/{id}/liquidation; POST .../liquidation/submit, /return, /post; GET/PUT .../liquidation-accounts"
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

API paths of this section are under `/api/v1/acsl` unless stated. Access (ACSL 1.1.0-1.1.3) is FR-DS-001.

### Input files and reports

```fr
id: FR-AS-001
title: Generate the input files and run ACSL reports on demand
brd: [ACSL 2.2.0 (p.115), ACSL 2.3.0 (p.115), ACSL 2.3.1 (p.116), ACSL 2.3.2 (p.116), ACSL 2.3.3 (p.116), ACSL 2.3.4 (p.116), ACSL 2.3.5 (p.116), ACSL 2.3.6 (p.116), ACSL 2.14.2 (p.123)]
actor: ACSL users (ACSL_REPORT_VIEW / EXPORT)
priority: Must have
fit: "CHANGE (2.2.0, 2.3.2, 2.3.4-2.3.6); FIT (2.3.0, 2.3.1, 2.3.3); NEW (2.14.2)"
screens: Report Centre (category ACSL)
api: "Reports ACSL-BOOKED-FIN-DETAILS, ACSL-SOA-RECON, ACSL-SOA-UPLOAD-LOG, ACSL-GL-SL-RECON"
description: ACSL runs its reports at any time without IT - the list of all booked accounts with their financial details for a period and GL account (the input file of the investigations, named "List of all booked accounts_<GL account name>_<period>"), the SOA reconciliation, the SOA upload log and the GL-SL reconciliation. The user views the list, selects one or several reports, views the details, and downloads, prints and saves them singly or as a batch (Volume 1, FR-AC-020 to FR-AC-023).
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
fit: "CHANGE (2.2.1), NEW (2.4.0); built, insurer layouts parked (AQ21)"
screens: Insurer SOA Reconciliation (Upload SOA; uploads list; upload log)
api: "POST .../soa-uploads; GET .../soa-uploads/{id}/log; GET .../soa-layouts"
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
fit: NEW
screens: Insurer SOA Reconciliation (upload - Results, Reconcile Again, Report)
api: "GET .../soa-uploads/{id}/results; POST .../soa-uploads/{id}/reconcile; GET .../soa-uploads/{id}/report"
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
fit: "CHANGE; built, control accounts parked (OQ07)"
screens: GL-SL Reconciliation (runs, rows, control accounts)
api: "POST .../gl-sl/runs; GET .../gl-sl/runs/{id}/rows; GET/PUT .../gl-sl/controls; report ACSL-GL-SL-RECON"
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
> ACSL 2.13.2 includes the period-end check. The reconciliation runs nightly and on demand; a check that stops the period close on a GL-SL difference is not built.

```fr
id: FR-AS-005
title: Produce aging and schedule reports per account family
brd: [ACSL 2.14.3 (p.124), ACSL 2.14.4 (p.125)]
actor: ACSL users
priority: Must have
fit: "CHANGE (2.14.3), NEW (2.14.4); not built (Gap G3)"
screens: Report Centre
api: "ACSL aging and schedule reports (not built)"
description: The BRD asks for aging reports by posting date and schedules for the covered period of AR insurer's refund, AP refund from insurer, commission receivable, payable to insurance company and premium receivable, in PHP and USD, with the GL balance and the SL-GL difference. These ACSL reports are not built; they need eight ageing slots in the ledger ageing (OQ43). Until then the account schedules of Volume 1 (SCH-PR with ageing, and the other schedules) and FR-AS-004 cover the balances and the difference.
preconditions:
  - "-"
main_flow:
  - Not built.
rules:
  - [R1, "Ageing slots and layouts to be confirmed (OQ43).", Configurable, Ageing slots]
validations: []
notifications:
  - "None."
audit:
  - "-"
acceptance:
  - "Gap G3: to be accepted when built."
```

### Investigation and cases

```fr
id: FR-AS-010
title: Investigate accounts and receive account analysis requests
brd: [ACSL 2.5.0 (p.117), ACSL 2.5.5 (p.117; Add.1 p.34), ACSL 2.5.1 (p.117), ACSL 2.5.2 (p.118), ACSL 2.5.3 (p.118)]
actor: ACSL Processor; ACSL Team Leader (assign)
priority: Must have
fit: "CHANGE; FIT (2.5.2); NEW (2.5.5)"
screens: ACSL Cases (tabs by stage); ACSL Case; Invoice Search; Invoice 360 (Invoice Family)
api: "GET/POST .../cases; POST .../cases/{id}/assign; PUT .../cases/{id}/findings; GET .../invoices/{invoiceNo}/cases; GET /api/v1/ops/invoices?assured=&inceptionFrom=&inceptionTo=&ao=; GET /api/v1/ops/invoices/{no}/family"
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
fit: NEW
screens: ACSL Case (Provide Result)
api: "POST .../cases/{id}/result"
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
fit: CHANGE
screens: ACSL Case (Request Payment Reversal); Cashiering (payment reversals)
api: "POST .../cases/{id}/payment-reversal; Operations port PaymentReversalRequester"
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
fit: CHANGE
screens: ACSL Case (Message the AO)
api: "POST .../cases/{id}/message-ao"
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
fit: CHANGE
screens: Correction Entries (To assign); Correction
api: "POST .../corrections/{id}/assign"
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
fit: "CHANGE; built, the journal link corrects_batch_id parked (AQ22)"
screens: Correction (Lines, Propose from journal, Submit)
api: "POST .../corrections/{id}/propose; PUT .../corrections/{id}/lines; GET .../corrections/{id}/original-lines; POST .../corrections/{id}/submit"
description:
  - The preparer enters the kind (Posting to a wrong GL account, Wrong amount, Reclassification, Other), the invoice, the original journal and the description, then the lines - account, side, amount, party for control accounts, invoice and ledger component, cost centre, business line and narration (up to 200 lines).
  - For a wrong GL account, "Propose" reads the original journal line and builds the reversal of that line and the re-post to the right account (and party or component), both linked to the original invoice and batch. The original and the correction stay visible in the invoice family. The preparer submits a balanced correction for review.
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
fit: CHANGE
screens: Correction (Endorse, Return)
api: "POST .../corrections/{id}/endorse; return from the workflow panel"
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
fit: "FIT (2.11.0-2.11.2, 2.12.1, 2.12.2), CHANGE (2.12.0)"
screens: Correction (Approve, Return)
api: "POST .../corrections/{id}/approve; return from the workflow panel"
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
fit: FIT
screens: Correction (Journal); Journal
api: "-"
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
fit: "CHANGE; built, deduction sources and spanning batches parked (AQ23)"
screens: Remittance Deductions (work list); Remittance Deduction (record, batches, insurer confirmation documents); Remittance batch (Settlement tab)
api: "/api/v1/remittance/deductions (create, PUT /{id}, /{id}/submit, /{id}/confirm, /{id}/applications, by-batch/{batchId}, pending); cancel and return from the workflow panel"
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
fit: CHANGE
screens: Invoice 360 (Invoice Family); ACSL Case
api: "GET /api/v1/ops/invoices/{no}/family; GET .../invoices/{invoiceNo}/cases"
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

## Accounting events of this volume

The events and seed entries are listed in Volume 1, section 5.5 (rows 1-19). The events of this volume are DISB_VOUCHER (by disbursement type), DISB_CHECK_NEGOTIATED, DISB_CHECK_STALE, DISB_FUND_TRANSFER, TAX_CWT_CERT_RECEIVED, OPS_REMIT_CPC2, OPS_REMIT_INCENTIVE, OPS_REMIT_DEDUCTION, the ACSL correction journals (ACS:<no>) and PRQ_CA_LIQUIDATION. The real entries are BDOI data (AQ02).

# Reports and documents

## Reports

<!-- table: widths=4.4,5,5.4,1.8 caption="Reports of Volume 2" status=Status size=8.5 -->
| Code | Name | BRD | Status |
|---|---|---|---|
| DSB-MASTERLIST | Masterlist of Disbursements | DIS 2.3.1, 3.28.3 | Built |
| DSB-UNRELEASED-CHECKS | Unreleased Checks (aged to 180 days) | DIS 2.3.2, 3.28.3 | Built |
| DSB-CWT-COMMISSION | CWT / BIR 2307 on Commission | DIS 2.3.3, 3.28.3 | Built |
| DSB-ATD | Authority to Debit | DIS 2.3.4, 3.28.3 | Built |
| DSB-ML-STALE | Miscellaneous Liability - Stale Checks | DIS 2.3.5, 3.28.3 | Built |
| DSB-CASH-FLOW | Disbursement Cash Flow | DIS 2.3.6, 3.28.3 | Built |
| DSB-PAYEE | Payee Report | DIS 2.3.7, 3.28.1 | Built |
| DSB-UPLOAD-FALLOUT | Request Upload Fall-out | DIS 2.3.8, 3.28.4 | Built |
| DSB-PAYEE-NOMATCH | Payees Not Matched | DIS 3.25.2 | Built |
| DSB-UNREGULARIZED | Unregularised Transactions | DIS 2.3.9, 3.27.0 | Built |
| DSB-EOD-REMIT, -REFUND, -SUPPLIER, -EMPLOYEE, -OTHER, -SUMMARY | End-of-day reports | DIS 3.28.0, 3.28.2 | Built |
| DSB-CPC2-INCENTIVE | CPC2 incentive report | DIS 3.29.0 | Not built (G4) |
| PRQ-STATUS | Request status | MKT 1.18.0 | Built |
| PRQ-REGISTER | Request register | MKT 1.18.1 | Built |
| ACSL-BOOKED-FIN-DETAILS | List of all booked accounts with financial details | ACSL 2.2.0, 2.14.2 | Built |
| ACSL-SOA-RECON | SOA reconciliation | ACSL 2.14.1 | Built |
| ACSL-SOA-UPLOAD-LOG | SOA upload log | ACSL 2.4.0 | Built |
| ACSL-GL-SL-RECON | GL-SL reconciliation | ACSL 2.13.2 | Built |
| ACSL aging and schedule reports | Five account families, PHP and USD | ACSL 2.14.3, 2.14.4 | Not built (G3) |

Reports run in the Report Centre with the options of Volume 1 (view, export to XLSX, ODS or PDF, print, archive, batches).

## Documents

<!-- table: widths=4.4,6.6,5.6 caption="Documents produced" size=8.5 -->
| Template | Document | Status |
|---|---|---|
| DSB_VOUCHER | Disbursement voucher | Draft layout (AQ14) |
| DSB_CHECK | Check | Draft layout per bank (AQ14) |
| DSB_ATD, DSB_ATD_EMAIL | Authority to Debit and its e-mail to the branch | Draft layout (AQ14) |
| DSB_MC_DD, DSB_CREDIT_TICKET, DSB_TT | Bank forms | Draft layouts (AQ14) |
| DSB_PAYMENT_ADVICE | Payment advice e-mailed to the payee | Built |
| DCTF | Direct Credit Transaction File (text) | Header and details (AQ09) |
| PRQ_RRF, PRQ_RFP, PRQ_LIQUIDATION | Refund Request Form, Request for Payment, cash-advance liquidation (Appendix D) | Draft layouts (AQ18) |
| BIR Form 2307 | Certificate of creditable tax withheld | Built |
| EARLY_INCENTIVE service invoice | Service invoice of the early incentive (booking) | Built |

# Interfaces and integration

![Interfaces of Disbursement, Payment Requests and ACSL (dashed = external)](figures/brd05_v2_integration.dot){width=13}

<!-- table: widths=3.8,2,7.2,2.4,2 caption="Interfaces" status=Status size=8.5 -->
| Interface | Direction | Content and trigger | BRD | Status |
|---|---|---|---|---|
| Operations disbursement gateway | In / Out | Payment requests of remittance, cashiering, commission, FRBS and Payment Requests; DV status back (DisbursementStatusChanged) | DIS 2.6.0, 3.25.0; MKT 1.20.0 | BUILT |
| Refund validation | Out / In | ACSL case and Cashiering task per cancelled-policy line; results back | MKT 1.11.0; ACSL 2.5.5 | BUILT |
| Payment reversal | Out / In | ACSL request to Cashiering (PRV-); result back | ACSL 2.6.1 | BUILT |
| DV cancellation hand-off | Out | Approved check cancellation to the Disbursement approvers (DV_CANCELLATION) | MKT 1.19.0 | BUILT, PARKED (AQ15) |
| General ledger | Out | DV, check, funding, correction, liquidation, CPC2 and deduction postings | DIS 2.19.0; ACSL 2.15.0 | BUILT |
| TPD (ACA) | Out | DCTF text file downloaded and sent by the user | DIS 2.16.1 | BUILT (file) |
| Bank branches | Out / In | ATD e-mail and bank forms; confirmations recorded by the user | DIS 2.7.7-2.7.9 | BUILT (manual) |
| Bank files | In | Deposited checks, credited accounts, BOB approvals (CSV uploads) | DIS 2.22.0, 3.26.x | BUILT, PARKED (AQ09) |
| BDO Business Online Banking | Out | Funding and online payments done in BOB; reference recorded | DIS 2.17.1 | OUT |
| Insurer SOA | In | SOA file upload per insurer and period | ACSL 2.2.1, 2.4.0 | BUILT, PARKED (AQ21) |
| E-mail | Out | Payment advice, ATD, notifications | DIS 2.7.12 | BUILT |

# Non-functional requirements

<!-- table: widths=3,5.8,5.4,2.4 caption="Non-functional requirements (BRD p.133-139; Add.1 p.37)" size=8.5 -->
| Topic | BRD value | BIBS target and approach | Status |
|---|---|---|---|
| Users | Disbursement 8; ACSL 6; Marketing 394 named, 40 concurrent | Within the BRD-1 sizing (145 concurrent) | FIT |
| Volumes | Remittance DVs 198 a year (+20%), refunds 246, supplier payments 293, government 9, other bank units 31, employee-related 56, CWT tagging 50; corrections 400; Marketing RFP / refund 302 | Small; no special tuning | FIT |
| Response time | Screen load 5-10 s, refresh 5 s, field display 2 s, save 5 s (2 s for some Disbursement and ACSL saves); reports 10-20 s first load; upload or download 3 s per file | Online p95 under 3 s; end of day, reports and SOA reconciliation run as jobs or batches | FIT |
| Peaks | Month end and year end; 08:00-12:00 Disbursement; 10:00-15:00 ACSL and Marketing | End of day after the peak; GL-SL reconciliation at 20:00; stale-check job at 00:20 | FIT |
| Devices | Same performance on mobile and desktop | Responsive screens | FIT |
| Availability (Add.1) | 100%; 07:00-18:00 Monday to Saturday; downtime under 24 hours; maintenance 19:00-07:00; BCP under 3 days | Same deployment as BRD-1; 100% is not a measurable SLA; one BIBS-wide NFR set is being agreed (AQ27) | OPEN |
| Retention (Add.1) | Reports and vouchers 5 years online, 5 years archive; daily backup kept 5 years | Retention rules of BRD-1 with a document class for vouchers and generated reports | FIT |
| Security and audit | Authorised users; maker-checker | Role-based access; four-eyes rules of section 3.3; masked account numbers; audit of every change | FIT |

# Configuration items owned by the business and the System Administrator

## Parameters

<!-- table: widths=6,3,7.6 caption="Parameters of Volume 2" size=8.5 -->
| Parameter | Default | Meaning |
|---|---|---|
| DISB_STALE_DAYS | 180 | Days after printing when an unnegotiated check is stale (30-720) |
| DISB_CHECK_CLEARING | ON | Checks credit checks outstanding until negotiated |
| DISB_CHECK_CLEARING_ACCOUNT | 2241 | Checks outstanding account |
| DISB_AUTO_APPROVER_ROUTING | REFUND,REMITTANCE | Disbursement types whose DV goes straight to the approver |
| DISB_NO_PAYEE_ACTION | HOLD | HOLD keeps a request without payee waiting; RETURN returns it to its source |
| DISB_CHECK_SERIES_WARNING | 20 | Remaining checks that raise CHECK_SERIES_LOW |
| EARLY_INCENTIVE_WTAX_RATE | 2 | Withholding tax rate (%) of the early-incentive service invoice |
| ACSL_SOA_MAX_ROWS | 20000 | Maximum rows of an SOA upload |

## Jobs

<!-- table: widths=5,4.2,7.4 caption="Jobs of Volume 2 (Manila time)" size=8.5 -->
| Job | Schedule | Purpose |
|---|---|---|
| DISB_CHECK_STALE | Daily 00:20 | Stale the checks older than DISB_STALE_DAYS and post the entry |
| DISB_EOD_CONFIRMATION | After each end-of-day run (no fixed schedule) | Send the payment advices |
| DISB_EOD_REPORTS | After each end-of-day run (no fixed schedule) | Generate the end-of-day reports |
| ACSL_GL_SL_RECON | Daily 20:00 | GL-SL reconciliation of the control accounts |

## Lists of values

<!-- table: widths=5,11.6 caption="Lists of values of Volume 2" size=8.5 -->
| List | Values delivered |
|---|---|
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

<!-- table: widths=5,2.4,9.2 caption="Alerts of Volume 2" size=8.5 -->
| Code | Severity | When |
|---|---|---|
| DISB_PAYEE_NO_MATCH | Medium | A system request names a payee that is not maintained |
| DISB_UNREGULARIZED | High | A cancelled approved DV is not regularised after 2 days |
| DISB_CHECK_STALE | Low | A check reached DISB_STALE_DAYS |
| CHECK_SERIES_LOW | Medium | A cheque book reached its warning threshold |
| ACSL_GLSL_DIFFERENCE | High | The GL-SL reconciliation found a difference of 1.00 or more |

## Masters and rules maintained by the business

<!-- table: widths=5,5,6.6 caption="Masters and rules" size=8.5 -->
| Item | Maintained by (authorised by) | FR |
|---|---|---|
| Payees and their accounts | Disbursement TL (Approver) | FR-DS-010-014 |
| BDOIR bank accounts, cheque books | Disbursement Approver (another user) | FR-DS-070, 071 |
| Employees and cost centres | Comptrollership administrator | FR-DS-037 |
| Accounting rules of the disbursement types | Comptrollership (maker-checker) | Volume 1, FR-AC-030 |
| Document templates (voucher, check, forms, RRF, RFP) | System Administrator | FR-DS-062, FR-PQ-003 |
| Liquidation accounts | Comptrollership administrator | FR-PQ-017 |
| SOA layouts per insurer | System Administrator (AQ21) | FR-AS-002 |
| GL-SL control accounts | ACSL TL | FR-AS-004 |
| CPC2 incentive criteria | TSU (BRD-3 Product Maintenance) | FR-DS-090 |
| Lists of values | Business Administrator (approver) | Volume 1, FR-AC-070 |

# Assumptions, dependencies and open questions

## Assumptions

<!-- table: widths=1.8,11,3.8 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| A-DS-01 | Bank channels stay manual or file-based; BIBS has no bank API | AQ09 |
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
| D-DS-01 | BDOI gives the DCTF trailer, the credited-accounts, deposited-checks and BOB report layouts | FR-DS-052, 053, 061 (AQ09) |
| D-DS-02 | BDOI gives the check, voucher and bank-form layouts and signatories | FR-DS-035, 062 (AQ14) |
| D-DS-03 | BDOI gives the payee migration file | FR-DS-014 (AQ11) |
| D-DS-04 | BDOI gives the real accounting entries of each disbursement type | FR-DS-041, 055 (AQ02) |
| D-DS-05 | BDOI confirms the CPC2 base, VAT and criteria, and the accounting of the insurer's 2% on early incentives | FR-DS-090, 091 (AQ24, AQ25) |
| D-DS-06 | The CPC2 report is built | FR-DS-092 (Gap G4) |
| D-PQ-01 | BDOI gives the mandatory RRF / RFP fields, approval chains and the HR approver | FR-PQ-003, 004, 009 (AQ18) |
| D-AS-01 | BDOI gives the insurer SOA layouts | FR-AS-002 (AQ21) |
| D-AS-02 | The ledger ageing supports eight slots for the ACSL aging and schedule reports | FR-AS-005 (Gap G3, OQ43) |

## Open questions

<!-- table: widths=1.4,10.1,2.8,2.4 caption="Open questions of Volume 2" status=Status size=8.5 -->
| ID | Question | Affects | Status |
|---|---|---|---|
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
| AQ27 | NFR set | Section 8 | OPEN |
| AQ28 | Role matrices | Section 3 | PARTIAL |
| AQ29 | Invoice number of endorsements and cancellations | FR-DS-093, FR-AS-026 | PARTIAL |

AQ29 is applied as proposed (new numbers linked to a root invoice) until BDOI decides otherwise.

<!-- pagebreak -->

# Traceability

Every Volume 2 requirement is met by at least one FR. The Build column gives the delivery state: **Built**; **Built, parked** (built; configuration or content waits for BDOI, see the FR); **Not built** with its gap (G3 ACSL aging and schedule reports, G4 CPC2 report); **Out** (outside BIBS per the BRD baseline).

## Disbursement (DIS)

<!-- table: widths=2.2,2.6,2.6,4.4,4.6,2 caption="DIS requirement IDs to FR, screen and build status" size=7.5 -->
| BRD ID | Page | FR | Screen | API | Build |
|---|---|---|---|---|---|
| DIS 1.1.0 | p.68 | FR-DS-001 | Login | POST /auth/login | Built |
| DIS 1.1.1 | p.69 | FR-DS-001 | Login | POST /auth/login | Built |
| DIS 1.1.2 | p.69 | FR-DS-001 | Login | POST /auth/login | Built |
| DIS 1.1.3 | p.69 | FR-DS-001 | Login | POST /auth/login | Built |
| DIS 2.2.0 | p.69 | FR-DS-010 | Payees | GET/POST .../payees | Built |
| DIS 2.2.1 | p.69 | FR-DS-012 | Payees | GET .../payee-requests | Built |
| DIS 2.2.2 | p.70 | FR-DS-011 | Payee | .../payees | Built |
| DIS 2.2.3 | p.70 | FR-DS-010 | Payees | GET/POST .../payees | Built |
| DIS 2.2.4 | p.70 | FR-DS-013 | Payee | DELETE .../payees/{id} | Built |
| DIS 2.2.5 | p.70 | FR-DS-011 | Payee | .../payees | Built |
| DIS 2.2.6 | p.71 | FR-DS-010 | Payees | GET/POST .../payees | Built |
| DIS 2.2.7 | p.71 | FR-DS-010 | Payees | GET/POST .../payees | Built |
| DIS 2.2.8 | p.71; Add.1 p.31-32; Add.2 p.11-12 | FR-DS-014 | Payees | .../payees?status= | Built, parked |
| DIS 2.3.0 | p.71 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.1 | p.72 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.2 | p.72 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.3 | p.72 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.4 | p.72 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.5 | p.72 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.6 | p.73 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.7 | p.73 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.8 | p.73 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 2.3.9 | p.73 | FR-DS-080 | Disbursement Reports | Reports with DSB codes | Built |
| DIS 3.28.0 | p.100 | FR-DS-081 | Disbursement End of Day | Reports DSB-EOD-REMIT, DSB-EOD-REFUND | Built |
| DIS 3.28.1 | p.100 | FR-DS-083 | Disbursement Reports | Reports DSB-PAYEE, DSB-UPLOAD-FALLOUT | Built |
| DIS 3.28.2 | p.101 | FR-DS-081 | Disbursement End of Day | Reports DSB-EOD-REMIT, DSB-EOD-REFUND | Built |
| DIS 3.28.3 | p.101 | FR-DS-082 | Disbursement Reports | Reports DSB-MASTERLIST | Built |
| DIS 3.28.4 | p.102 | FR-DS-083 | Disbursement Reports | Reports DSB-PAYEE, DSB-UPLOAD-FALLOUT | Built |
| DIS 3.29.0 | Add.2 p.7 | FR-DS-092 | - | Report DSB-CPC2-INCENTIVE | Not built (G4) |
| DIS 3.30.2 | Add.2 p.10-11 | FR-DS-037 | Setup > Employees | GET/POST/PUT /organization/employees | Built |
| DIS 2.4.0 | p.73 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.4.1 | p.73 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.4.2 | p.74 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.4.3 | p.74 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.4.4 | p.74 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.5.0 | p.75 | FR-DS-024 | Disbursement Uploads | Bulk handler DISB_REQUESTS | Built |
| DIS 2.5.1 | p.75 | FR-DS-024 | Disbursement Uploads | Bulk handler DISB_REQUESTS | Built |
| DIS 2.6.0 | p.75 | FR-DS-020 | Disbursement Workbench | Operations port DisbursementGateway | Built |
| DIS 2.6.1 | p.76 | FR-DS-023 | Encode Payment Request | POST .../requests | Built |
| DIS 2.6.2 | p.76 | FR-DS-020 | Disbursement Workbench | Operations port DisbursementGateway | Built |
| DIS 3.25.0 | p.95 | FR-DS-020 | Disbursement Workbench | Operations port DisbursementGateway | Built |
| DIS 3.25.1 | p.96 | FR-DS-021 | Disbursement Workbench | - | Built |
| DIS 3.25.2 | p.96 | FR-DS-022 | Disbursement Workbench | Report DSB-PAYEE-NOMATCH | Built |
| DIS 2.7.0 | p.77 | FR-DS-033 | Disbursement Voucher | .../vouchers/{id}/instrument/print | Built |
| DIS 2.7.1 | p.77 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.7.2 | p.78 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.7.3 | p.78 | FR-DS-025 | Disbursement Workbench | GET .../summary | Built |
| DIS 2.7.4 | p.78 | FR-DS-030 | Disbursement Voucher | POST .../requests/{id}/voucher | Built |
| DIS 2.7.5 | p.79 | FR-DS-030 | Disbursement Voucher | POST .../requests/{id}/voucher | Built |
| DIS 2.7.6 | p.80 | FR-DS-031 | Disbursement Voucher | GET/PUT .../vouchers/{id}/proforma | Built |
| DIS 2.7.7 | p.81 | FR-DS-034 | Disbursement Voucher | .../vouchers/{id}/instrument/print, /email | Built |
| DIS 2.7.8 | p.81 | FR-DS-035 | Disbursement Voucher | .../vouchers/{id}/instrument/print | Built, parked |
| DIS 2.7.9 | p.82 | FR-DS-035 | Disbursement Voucher | .../vouchers/{id}/instrument/print | Built, parked |
| DIS 2.7.10 | p.82 | FR-DS-032 | Disbursement Voucher | POST .../vouchers/{id}/allocation | Built |
| DIS 2.7.11 | p.82 | FR-DS-036 | Disbursement Voucher | POST .../vouchers/{id}/submit | Built |
| DIS 2.7.12 | p.83 | FR-DS-063 | Disbursement End of Day | POST .../eod/runs/{id}/confirm | Built |
| DIS 3.30.0 | Add.2 p.9 | FR-DS-032 | Disbursement Voucher | POST .../vouchers/{id}/allocation | Built |
| DIS 3.30.1 | Add.2 p.9-10 | FR-DS-037 | Setup > Employees | GET/POST/PUT /organization/employees | Built |
| DIS 2.8.0 | p.83 | FR-DS-050 | Disbursement Voucher | .../vouchers/{id}/instrument/release | Built |
| DIS 2.8.1 | p.83 | FR-DS-050 | Disbursement Voucher | .../vouchers/{id}/instrument/release | Built |
| DIS 2.8.2 | p.84 | FR-DS-050 | Disbursement Voucher | .../vouchers/{id}/instrument/release | Built |
| DIS 2.8.3 | p.84 | FR-DS-050 | Disbursement Voucher | .../vouchers/{id}/instrument/release | Built |
| DIS 2.8.4 | p.85 | FR-DS-050 | Disbursement Voucher | .../vouchers/{id}/instrument/release | Built |
| DIS 2.8.5 | p.85 | FR-DS-051 | Disbursement Voucher | .../vouchers/{id}/instrument/status-edits | Built |
| DIS 2.9.0 | p.85 | FR-DS-043 | Disbursement Workbench | POST .../vouchers/{id}/cancel | Built |
| DIS 2.22.0 | p.93 | FR-DS-053 | Disbursement Uploads | Bulk handlers DISB_CHECKS_NEGOTIATED | Built, parked |
| DIS 3.26.0 | p.96 | FR-DS-052 | Disbursement Voucher | Instrument service | Built |
| DIS 3.26.1 | p.97 | FR-DS-053 | Disbursement Uploads | Bulk handlers DISB_CHECKS_NEGOTIATED | Built |
| DIS 3.26.2 | p.97 | FR-DS-054 | Disbursement Voucher | Job DISB_CHECK_STALE | Built |
| DIS 3.26.3 | p.97 | FR-DS-052 | Disbursement Voucher | Instrument service | Built |
| DIS 3.26.4 | p.98 | FR-DS-053 | Disbursement Uploads | Bulk handlers DISB_CHECKS_NEGOTIATED | Built, parked |
| DIS 3.26.5 | p.98 | FR-DS-052 | Disbursement Voucher | Instrument service | Built |
| DIS 3.26.6 | p.98 | FR-DS-052 | Disbursement Voucher | Instrument service | Built |
| DIS 3.26.7 | p.99 | FR-DS-052 | Disbursement Voucher | Instrument service | Built, parked |
| DIS 2.10.0 | p.86 | FR-DS-056 | Disbursement Voucher | POST .../vouchers/{id}/tags/receipt | Built |
| DIS 2.10.1 | p.86 | FR-DS-056 | Disbursement Voucher | POST .../vouchers/{id}/tags/receipt | Built |
| DIS 2.10.2 | p.86 | FR-DS-056 | Disbursement Voucher | POST .../vouchers/{id}/tags/receipt | Built |
| DIS 2.11.0 | p.86 | FR-DS-057 | Disbursement Voucher | POST .../vouchers/{id}/tags/cwt | Built |
| DIS 2.11.1 | p.86 | FR-DS-057 | Disbursement Voucher | POST .../vouchers/{id}/tags/cwt | Built |
| DIS 2.11.2 | p.87 | FR-DS-057 | Disbursement Voucher | POST .../vouchers/{id}/tags/cwt | Built |
| DIS 2.12.0 | p.87 | FR-DS-058 | Tax & Statutory > BIR Form 2307 | /tax/2307 | Built |
| DIS 2.13.0 | p.87 | FR-DS-040 | Disbursement Workbench | POST .../vouchers/{id}/submit-for-approval | Built |
| DIS 2.14.0 | p.88 | FR-DS-040 | Disbursement Workbench | POST .../vouchers/{id}/submit-for-approval | Built |
| DIS 2.15.0 | p.88 | FR-DS-040 | Disbursement Workbench | POST .../vouchers/{id}/submit-for-approval | Built |
| DIS 2.16.0 | p.88 | FR-DS-060 | Disbursement End of Day | POST .../eod/runs | Built |
| DIS 2.16.1 | p.88 | FR-DS-061 | Disbursement End of Day | EOD output DCTF | Built, parked |
| DIS 2.16.2 | p.88 | FR-DS-062 | Disbursement End of Day | .../eod/outputs/{id} | Built, parked |
| DIS 2.16.3 | p.89 | FR-DS-060 | Disbursement End of Day | POST .../eod/runs | Built |
| DIS 2.16.4 | p.89 | FR-DS-060 | Disbursement End of Day | POST .../eod/runs | Built |
| DIS 2.16.5 | p.89 | FR-DS-060 | Disbursement End of Day | POST .../eod/runs | Built |
| DIS 2.16.6 | p.89 | FR-DS-062 | Disbursement End of Day | .../eod/outputs/{id} | Built, parked |
| DIS 2.17.0 | p.90 | FR-DS-064 | Account Funding | POST .../funding, /{id}/submit | Built |
| DIS 2.17.1 | p.90 | FR-DS-064 | Account Funding | POST .../funding, /{id}/submit | Out (external) |
| DIS 2.17.2 | p.90 | FR-DS-064 | Account Funding | POST .../funding, /{id}/submit | Built |
| DIS 2.17.3 | p.90 | FR-DS-064 | Account Funding | POST .../funding, /{id}/submit | Built |
| DIS 2.17.4 | p.90; Add.1 p.33 | FR-DS-064 | Account Funding | POST .../funding, /{id}/submit | Built |
| DIS 2.18.0 | p.91 | FR-DS-043 | Disbursement Workbench | POST .../vouchers/{id}/cancel | Built |
| DIS 2.19.0 | p.91 | FR-DS-041 | Disbursement Workbench | POST .../vouchers/{id}/approve | Built |
| DIS 2.20.0 | p.92 | FR-DS-044 | Disbursement Voucher | POST .../vouchers/{id}/cancel | Built |
| DIS 2.21.0 | p.92 | FR-DS-042 | Disbursement Voucher | POST .../vouchers/{id}/reject | Built |
| DIS 2.23.0 | p.93 | FR-DS-070 | Bank Accounts and Checks | POST .../banks/{id}/cheque-books | Built |
| DIS 2.23.1 | p.93 | FR-DS-070 | Bank Accounts and Checks | POST .../banks/{id}/cheque-books | Built |
| DIS 2.23.2 | p.93 | FR-DS-070 | Bank Accounts and Checks | POST .../banks/{id}/cheque-books | Built |
| DIS 2.24.0 | p.93 | FR-DS-071 | Bank Accounts and Checks | .../banks | Built |
| DIS 2.24.1 | p.93 | FR-DS-071 | Bank Accounts and Checks | .../banks | Built |
| DIS 2.24.2 | p.94; Add.1 p.33 | FR-DS-071 | Bank Accounts and Checks | .../banks | Built |
| DIS 3.27.0 | p.99; Add.1 p.31 | FR-DS-045 | Disbursement Workbench | Report DSB-UNREGULARIZED | Built |
| DIS 3.27.1 | p.100 | FR-DS-055 | Disbursement Voucher | Events DISB_CHECK_NEGOTIATED | Built, parked |
| DIS 3.27.2 | p.100 | FR-DS-093 | Invoice Search | GET /ops/invoices/{no}/family | Built |
| DIS 3.29.1 | Add.2 p.7-8 | FR-DS-091 | Remittance batch | Booking service invoice type EARLY_INCENTIVE | Built, parked |
| DIS 3.29.2 | Add.2 p.8-9 | FR-DS-090 | Remittance batch | Event OPS_REMIT_CPC2 | Built, parked |

## Payment Requests (MKT)

<!-- table: widths=2.2,2.6,2.6,4.4,4.6,2 caption="MKT requirement IDs to FR, screen and build status" size=7.5 -->
| BRD ID | Page | FR | Screen | API | Build |
|---|---|---|---|---|---|
| MKT 1.1.0 | p.105 | FR-DS-001 | Login | POST /auth/login | Built |
| MKT 1.1.1 | p.105 | FR-DS-001 | Login | POST /auth/login | Built |
| MKT 1.1.2 | p.105 | FR-DS-001 | Login | POST /auth/login | Built |
| MKT 1.1.3 | p.105 | FR-DS-001 | Login | POST /auth/login | Built |
| MKT 1.2.0 | p.105 | FR-PQ-001 | Requests Home | GET .../requests | Built |
| MKT 1.3.0 | p.106 | FR-PQ-001 | Requests Home | GET .../requests | Built |
| MKT 1.4.0 | p.106 | FR-PQ-001 | Requests Home | GET .../requests | Built |
| MKT 1.5.0 | p.106 | FR-PQ-001 | Requests Home | GET .../requests | Built |
| MKT 1.6.0 | p.106 | FR-PQ-001 | Requests Home | GET .../requests | Built |
| MKT 1.7.0 | p.106 | FR-PQ-002 | Report Centre | Report Centre | Built, parked |
| MKT 1.7.1 | p.106 | FR-PQ-002 | Report Centre | Report Centre | Built |
| MKT 1.7.2 | p.107 | FR-PQ-002 | Report Centre | Report Centre | Built |
| MKT 1.7.3 | p.107 | FR-PQ-002 | Report Centre | Report Centre | Built |
| MKT 1.8.0 | p.107 | FR-PQ-005 | Request | POST .../requests/{id}/assign | Built |
| MKT 1.9.0 | p.107 | FR-PQ-005 | Request | POST .../requests/{id}/assign | Built |
| MKT 1.10.0 | p.107 | FR-PQ-003, FR-PQ-004, FR-PQ-017 | New Refund Request; New Cash Advance; Request | POST .../requests/refunds; POST .../requests/cash-advances; PUT .../requests/{id}/liquidation | Built, parked |
| MKT 1.11.0 | p.108 | FR-PQ-006 | Request | POST .../requests/{id}/submit | Built |
| MKT 1.12.0 | p.108 | FR-PQ-007 | Request | /attachments | Built |
| MKT 1.13.0 | p.108 | FR-PQ-007 | Request | /attachments | Built |
| MKT 1.14.0 | p.109 | FR-PQ-008 | Request | POST .../requests/{id}/submit | Built |
| MKT 1.15.0 | p.109 | FR-PQ-008 | Request | POST .../requests/{id}/submit | Built |
| MKT 1.16.0 | p.109 | FR-PQ-009 | Request | POST .../requests/{id}/approve | Built |
| MKT 1.16.1 | p.109 | FR-PQ-009 | Request | POST .../requests/{id}/approve | Built |
| MKT 1.16.2 | p.109 | FR-PQ-009 | Request | POST .../requests/{id}/approve | Built |
| MKT 1.16.3 | p.109 | FR-PQ-009 | Request | POST .../requests/{id}/approve | Built, parked |
| MKT 1.17.0 | p.110 | FR-PQ-010 | Request | Cancel from the workflow panel | Built |
| MKT 1.18.0 | p.110 | FR-PQ-012 | Requests Home | Reports PRQ-STATUS, PRQ-REGISTER | Built |
| MKT 1.18.1 | p.110 | FR-PQ-012 | Requests Home | Reports PRQ-STATUS, PRQ-REGISTER | Built |
| MKT 1.19.0 | p.110 | FR-PQ-011 | Cancel a Check | POST .../requests/check-cancellations | Built, parked |
| MKT 1.20.0 | p.111 | FR-PQ-013 | Request | Event DisbursementStatusChanged | Built |
| MKT 2.22.0 | p.111 | FR-PQ-007 | Request | /attachments | Built |
| MKT 2.23.0 | p.111 | FR-PQ-014 | New Refund Request | - | Built |
| MKT 2.24.0 | p.111 | FR-PQ-015 | Request | Operations port DisbursementGateway | Built |
| MKT 2.25.0 | p.112; Add.1 p.34-35 | FR-PQ-016 | Client | GET .../payout-accounts | Built, parked |
| MKT 2.25.1 | p.112 | FR-PQ-016 | Client | GET .../payout-accounts | Built, parked |
| MKT 2.26.0 | p.112 | FR-PQ-012 | Requests Home | Reports PRQ-STATUS, PRQ-REGISTER | Built |

## ACSL

<!-- table: widths=2.2,2.6,2.6,4.4,4.6,2 caption="ACSL requirement IDs to FR, screen and build status" size=7.5 -->
| BRD ID | Page | FR | Screen | API | Build |
|---|---|---|---|---|---|
| ACSL 1.1.0 | p.114 | FR-DS-001 | Login | POST /auth/login | Built |
| ACSL 1.1.1 | p.115 | FR-DS-001 | Login | POST /auth/login | Built |
| ACSL 1.1.2 | p.115 | FR-DS-001 | Login | POST /auth/login | Built |
| ACSL 1.1.3 | p.115 | FR-DS-001 | Login | POST /auth/login | Built |
| ACSL 2.2.0 | p.115 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.2.1 | p.115 | FR-AS-002 | Insurer SOA Reconciliation | POST .../soa-uploads | Built, parked |
| ACSL 2.3.0 | p.115 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.1 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.2 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.3 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.4 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.5 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.3.6 | p.116 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.4.0 | p.117; Add.1 p.33-34 | FR-AS-002 | Insurer SOA Reconciliation | POST .../soa-uploads | Built, parked |
| ACSL 2.13.0 | p.120 | FR-AS-003 | Insurer SOA Reconciliation | GET .../soa-uploads/{id}/results | Built |
| ACSL 2.13.1 | p.121 | FR-AS-003 | Insurer SOA Reconciliation | GET .../soa-uploads/{id}/results | Built |
| ACSL 2.13.2 | p.121 | FR-AS-004 | GL-SL Reconciliation | POST .../gl-sl/runs | Built, parked |
| ACSL 2.14.0 | p.121 | FR-AS-003 | Insurer SOA Reconciliation | GET .../soa-uploads/{id}/results | Built |
| ACSL 2.14.1 | p.122 | FR-AS-003 | Insurer SOA Reconciliation | GET .../soa-uploads/{id}/results | Built |
| ACSL 2.14.2 | p.123 | FR-AS-001 | Report Centre | Reports ACSL-BOOKED-FIN-DETAILS | Built |
| ACSL 2.14.3 | p.124 | FR-AS-005 | Report Centre | ACSL aging and schedule reports | Not built (G3) |
| ACSL 2.14.4 | p.125 | FR-AS-005 | Report Centre | ACSL aging and schedule reports | Not built (G3) |
| ACSL 2.5.0 | p.117 | FR-AS-010 | ACSL Cases | GET/POST .../cases | Built |
| ACSL 2.5.5 | p.117; Add.1 p.34 | FR-PQ-006, FR-AS-010 | Request; ACSL Cases | POST .../requests/{id}/submit; GET/POST .../cases | Built |
| ACSL 2.5.1 | p.117 | FR-AS-010 | ACSL Cases | GET/POST .../cases | Built |
| ACSL 2.5.2 | p.118 | FR-AS-010 | ACSL Cases | GET/POST .../cases | Built |
| ACSL 2.5.3 | p.118 | FR-AS-010 | ACSL Cases | GET/POST .../cases | Built |
| ACSL 2.5.4 | p.118 | FR-AS-011 | ACSL Case | POST .../cases/{id}/result | Built |
| ACSL 2.6.0 | p.118 | FR-AS-012 | ACSL Case | POST .../cases/{id}/payment-reversal | Built |
| ACSL 2.6.1 | p.118 | FR-AS-012 | ACSL Case | POST .../cases/{id}/payment-reversal | Built |
| ACSL 2.6.2 | p.119 | FR-AS-013 | ACSL Case | POST .../cases/{id}/message-ao | Built |
| ACSL 2.7.0 | p.119 | FR-AS-020 | Correction Entries | POST .../corrections/{id}/assign | Built |
| ACSL 2.8.0 | p.119 | FR-AS-020 | Correction Entries | POST .../corrections/{id}/assign | Built |
| ACSL 2.9.0 | p.119 | FR-AS-021 | Correction | POST .../corrections/{id}/propose | Built |
| ACSL 2.9.1 | Add.2 p.12-13 | FR-AS-021 | Correction | POST .../corrections/{id}/propose | Built, parked |
| ACSL 2.9.2 | Add.2 p.13 | FR-AS-025 | Remittance Deductions | /remittance/deductions | Built, parked |
| ACSL 2.10.0 | p.119 | FR-AS-022 | Correction | POST .../corrections/{id}/endorse | Built |
| ACSL 2.11.0 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.11.1 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.11.2 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.12.0 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.12.1 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.12.2 | p.120 | FR-AS-023 | Correction | POST .../corrections/{id}/approve | Built |
| ACSL 2.15.0 | p.125 | FR-AS-024 | Correction | - | Built |
| ACSL 2.16.0 | p.126; Add.1 p.34 | FR-AS-026 | Invoice 360 | GET /ops/invoices/{no}/family | Built |

API paths start with `/api/v1`; "..." stands for the module path of the FR.

## Coverage summary

<!-- table: widths=5,1.9,1.9,1.9,2.2,1.9,1.9 caption="Coverage summary of Volume 2" size=8.5 -->
| Group | BRD IDs | Covered | Built | Built, parked | Not built | Out |
|---|---|---|---|---|---|---|
| Disbursement (DIS) | 111 | 111 | 97 | 12 | 1 | 1 |
| Payment Requests (MKT) | 36 | 36 | 30 | 6 | 0 | 0 |
| ACSL | 45 | 45 | 38 | 5 | 2 | 0 |
| **Total** | **192** | **192** | **165** | **23** | **3** | **1** |


# Sign-off

By signing, BDOI confirms that this volume describes the Disbursement, Payment Request and ACSL functions it expects in BIBS, accepts the recorded differences in section 1.7 and the assumptions in section 10.1. Open questions in section 10.3 stay open; their answers are applied as configuration or through a change request. Volume 1 is signed separately by the Accounting and administration owners.

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Disbursement Section", organisation: BDOI}
  - {name: "", role: "Head, Accounting Controls and Subsidiary Ledger", organisation: BDOI}
  - {name: "", role: "Head, Marketing", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
