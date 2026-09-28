---
# Source of "00 Start Here - Guide to the BRD-02 Sign-off Pack" (Word), release set v2.0 of BRD-2 Operations.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-02_Operations/START_HERE_BRD02.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-02 Operations Sign-off Pack, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-02
name: Operations
doc_id: BIBS-SH-BRD-02
version: "2.0"
date: 28 September 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-2 Operations
h1_page_break: false
control:
  - version: "2.0"
    date: 28 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-2 Operations business sign-off pack (replaces the release note of the set)
distribution:
  - {name: "Shellah Marie C. Miranda, AVP, Product Owner", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: "Jose Melvin M. Jarin, Operations: Financial Transactions and Processing", role: Approver, organisation: BDOI, purpose: Review and sign-off for Operations}
  - {name: "Shirley Catapang and Perjelyn Joy Gutierrez, Operations: FT&P", role: Reviewers, organisation: BDOI, purpose: Review of the screens and rows of their teams}
  - {name: "Cashiering, Remittance, Adjustment, Production Reconciliation, Commission Receivables, Marketing Collection, Comptrollership and Disbursement", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: Information Technology Group, role: Reviewer, organisation: BDOI, purpose: "Interfaces, jobs, access; the Technical Specification"}
  - {name: "Dan Ace Cauton and Zean C. Ibay, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-2 Operations of BIBS, screen by screen, so that each Operations team can confirm what it will get and sign it off: the invoice ledger and Invoice 360, Cashiering, Remittance, Adjustment, Production Reconciliation and Commission Receivables, with the Disbursement queue, the interfaces and the Operations reports. Signing freezes the content, the screens and the navigation of Operations; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-02_Operations of Drop 1.

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

SIT users of the seed data for the review: cashier, cashbr and cashtl (Cashiering), remit and remittl (Remittance), adjust and adjtl (Adjustment), recon (Production Reconciliation), commrec and commtl (Commission Receivables), mktcoll and mkttl (Marketing Collection), acsl and acsltl (ACSL), comptrol (Comptrollership), disb (Disbursement), admin (System Administrator) and auditor. The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-steps
```

**How to review.** Open the FRS chapters of your team or the same screens on SIT. In the sign-off workbook, set BU review to Accept, Change requested or Comment on each row you review (sheets Screen standards, Screen catalogue, Field register, Business rules, Messages), write the change in BU comment, and add your name and the date. Questions go to the Comments log sheet. One workbook per team is enough. Chapter 21 of the FRS lists the proposed rules and clarifications on which BDOI is asked to decide.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Teams |
|---|---|---|---|
| Mon 16-Nov-2026 | 09:30-12:00 | Kick-off; the invoice ledger and Invoice 360; Cashiering: receipts, application, unapplied payments and dispositions (walkthrough A steps 1-4, walkthrough D steps 3-4) | Cashiering, Operations FT&P |
| Tue 17-Nov-2026 | 09:30-12:00 | Remittance: extraction, batches, approval, Disbursement queue, insurer ORs, holds and special remittance (walkthrough A steps 5-10, walkthrough D steps 1, 2 and 5) | Remittance, Marketing Collection, Disbursement |
| Wed 18-Nov-2026 | 09:30-12:00 | Adjustment: endorsement and cancellation requests, posting, Policy Transactions, refund (walkthrough B) | Adjustment, Marketing Collection, Cashiering |
| Thu 19-Nov-2026 | 09:30-12:00 | Production Reconciliation and Commission Receivables: registers, feedback, DP billing and collection, incentives, BIR certificates (walkthrough C) | Production Reconciliation, Commission Receivables, Comptrollership |
| Fri 20-Nov-2026 | 09:30-11:30 | Uploads with the error file, reports, interfaces and the accounting of Operations | Operations FT&P, Comptrollership, BDOI IT |

# Who signs what

The set holds business content only: the screens and fields, the columns of the lists, reports and templates that the user sees or fills in, the validations, business rules, messages, notifications, documents, walkthroughs and reports. The business users sign that content. Technical content (the interfaces between systems, data storage, security set-up) is not part of the set: it is in the Technical Specification, a separate document reviewed by BDOI IT. The roles are those of the approval sheet of the Operations BRD: iorta TechNXT and the Business Analyst of Business Project Services prepare the set, Operations: Financial Transactions and Processing provides the input, BDOI IT reviews the interfaces, the user access and the Technical Specification, and the Program Manager of Business Project Services, Operations: Financial Transactions and Processing and the Product Owner approve it.

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

Signing freezes the Operations screens, fields, navigation, actions, business rules, messages, notifications, documents, upload templates and the cross-BRD contract as specified. Configuration values marked "default" (receipt series, tolerances, thresholds, schedules, list entries, templates, incentive rules) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs (Operations feeds Collections, Accounting, Disbursement and ACSL), approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
