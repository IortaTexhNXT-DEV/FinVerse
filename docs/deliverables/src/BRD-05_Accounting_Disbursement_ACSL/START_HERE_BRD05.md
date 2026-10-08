---
# Source of "00 Start Here - Guide to the BRD-05 Sign-off Pack" (Word), release set v2.1 of BRD-5 Accounting,
# Disbursement and ACSL.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-05_Accounting_Disbursement_ACSL/START_HERE_BRD05.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-05 Accounting, Disbursement and ACSL Sign-off Pack, release set v2.1
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-05
name: Accounting Disbursement ACSL
doc_id: BIBS-SH-BRD-05
version: "2.1"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-5 Accounting, Disbursement and ACSL
h1_page_break: false
control:
  - version: "2.0"
    date: 2 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-5 Accounting, Disbursement and ACSL business sign-off pack (replaces the cover note of the two FRS volumes)
  - version: "2.1"
    date: 08 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: "Re-based on the WS Addendum v04152026 (minutes of 4-May-2026); user-story view and storyboard index added to the FRS; CPC2 by the incentive tag of the booked transaction; granularity of the incentive service invoice for confirmation"
distribution:
  - {name: "Ronald Allan E. De Leon, VP, Head - Comptrollership", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: "Iris S. Marquez (FRBS and ACSL), Rodrigo R. Dela Cruz (Disbursement), Perjelyn Joy R. Gutierrez (Comptrollership and Operations), Shellah Marie C. Miranda (Marketing, Processing and Collections), Roderick L. Lim (Institutional Banking, SM and BDO Accounts)", role: Approvers, organisation: BDOI, purpose: Sign-off of the rows of their units}
  - {name: "Freddie A. Atanque and Jennifer T. Lugtu, Product Owners - Comptrollership - ACSL", role: Reviewers, organisation: BDOI, purpose: "Review of the ACSL, Disbursement and Accounting screens and rows"}
  - {name: "Marketing Business Services and System Support (Ma. Fides Rivera, Ruby Evangelista, Darryl Castillo, Rhommel Mark Galler)", role: Input providers and reviewer, organisation: BDOI, purpose: "Review of the Marketing request screens and of the non-functional requirements"}
  - {name: "GL team, Disbursement, ACSL, Marketing requesters and approvers, Human Resources, Cashiering and Remittance", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: Information Technology Group, role: Reviewer, organisation: BDOI, purpose: "Scheduled runs, files, bank files and forms, access; the Technical Specification"}
  - {name: "Dan Ace R. Cauton, Roda Lyn D. Gallardo, Zean C. Ibay and Jane Tagle, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-5 Accounting, Disbursement and ACSL of BIBS, screen by screen, so that Comptrollership (the GL team of FRBS, Disbursement and ACSL), Marketing and Human Resources can confirm what they will get and sign it off: the journals, the chart of accounts and the rates, the month-end and year-end closing, the bank reconciliation, the report pack, the account schedules, the service fee and the BIR outputs; the payees, the payment requests and the disbursement vouchers with their entries and instruments, the end of day, the account funding, the bank accounts and checks; the Marketing refund, cash-advance and check-cancellation requests; the insurer statements of account, the GL-SL reconciliation, the ACSL cases and the correction entries. The two volumes of the FRS of version 1.0 are combined into one FRS. Signing freezes the content, the screens and the navigation of the set; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

**What changed in v2.1.** BDOI issued the BRD-5 pack again on 8 October 2026 with the minutes of the walkthrough of the Workshop Addendum with IT on 4 May 2026 in front of the unchanged BRD. This version applies them: the CPC2 incentive and its report follow the incentive tag stored on the booked transaction; the early-incentive service invoice is issued per remittance batch with one line per qualified transaction, and BDOI is asked to confirm the granularity and the trigger (CLR-DS-17); every BRD page cited moves by one page. The FRS now ends with a user-story view of every BRD requirement and a storyboard index of the walkthrough frames. Chapter 21 lists the new points for confirmation, CLR-DS-16 to CLR-DS-18.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-05_Accounting_Disbursement_ACSL of Drop 1.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-map
```

# Where to start

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-reading
```

SIT users of the seed data for the review: glofficer (GL Officer), gltl (GL Team Lead), glhead (GL Section Head), accountant (Accountant), fmanager (Comptrollership administrator), disb (Disbursement Processor), disbtl and disbtl2 (Disbursement Team Leaders), disbappr and disbappr2 (Disbursement Approvers), mktao (Marketing Processor), mktrev (Marketing Reviewer), mktappr (Marketing Approver), hrappr (Human Resources), acsl (ACSL Processor), acsltl (ACSL Team Leader), acslhead (ACSL Head), comptrol (Comptrollership), badmin (Business Administrator), admin (System Administrator) and auditor. The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-steps
```

**How to review.** Open the FRS chapters of your unit or the same screens on SIT. In the sign-off workbook, set BU review to Accept, Change requested or Comment on each row you review (sheets Screen standards, Screen catalogue, Field register, Business rules, Messages), write the change in BU comment, and add your name and the date. Questions go to the Comments log sheet. One workbook per unit is enough. Chapter 21 of the FRS lists the proposed rules and clarifications on which BDOI is asked to decide.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Units |
|---|---|---|---|
| Wed 25-Nov-2026 | 09:30-12:00 | Kick-off; the journals, the chart of accounts and the rates, the manual accrual from entry to posting (walkthrough A, walkthrough E step 1) | GL team, Comptrollership |
| Thu 26-Nov-2026 | 09:30-12:00 | Month end: the revaluation rate and the revaluation, the GL close and the broking books, the year-end close, the bank reconciliation; the report pack, the account schedules, the service fee and the BIR outputs (walkthrough D) | GL team, Comptrollership, BDOI IT |
| Fri 27-Nov-2026 | 09:30-12:00 | Disbursement: payees, the workbench, the voucher with its entry and instrument, the end of day, the fundings, the bank accounts and checks, the uploads and the reports (walkthrough B steps 5 to 7, walkthrough E step 4) | Disbursement, Operations (Remittance, Cashiering), BDOI IT |
| Mon 30-Nov-2026 | 09:30-12:00 | The Marketing refund, cash-advance and check-cancellation requests (walkthrough B steps 1 to 4 and 8, walkthrough E step 2) | Marketing, Human Resources, Disbursement |
| Tue 1-Dec-2026 | 09:30-12:00 | ACSL: the insurer statements of account, the GL-SL reconciliation, the cases and the correction entries; the remittance deductions with Operations (walkthrough C, walkthrough E step 3) | ACSL, Operations (Remittance) |

# Who signs what

The set holds business content only: the screens and fields, the columns of the lists, files and templates that the user sees or fills in, the validations, business rules, messages, notifications, documents, walkthroughs and files. The business users sign that content. Technical content (the interfaces between systems, data storage, security set-up) is not part of the set: it is in the Technical Specification, a separate document reviewed by BDOI IT. The roles are those of the approval sheets of the BRD-5 documents: iorta TechNXT and the Business Analysts of Business Project Services prepare the set, Marketing Business Services and System Support provide the input, the Program Manager of Business Project Services, the ACSL product owners, BDOI IT and MBS review it, and the product owners of Disbursement, FRBS and ACSL, Comptrollership and Operations, Marketing, the Head of Institutional Banking and the Head of Comptrollership approve it.

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: owners-matrix
```

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: owners-roles
```

The signatories sign the Sign-off certificate sheet of the sign-off workbook in the same order: prepared by, input provided by, reviewed by, approved by.

# Change control after sign-off

Signing freezes the screens, fields, navigation, actions, business rules, messages, notifications, documents and files, the upload templates and the cross-BRD contract of Accounting, Disbursement, the Marketing requests and ACSL as specified. Configuration values marked "default" (accounting and cost-centre rules, rates, parameters, check series, list entries, report and schedule layouts, form and file layouts, service-fee rates) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs (Disbursement pays the requests of Operations and Marketing and reports back, and ACSL corrects the postings of every module), approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
