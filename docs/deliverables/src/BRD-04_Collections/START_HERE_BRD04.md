---
# Source of "00 Start Here - Guide to the BRD-04 Sign-off Pack" (Word), release set v2.0 of BRD-4 Collections.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-04_Collections/START_HERE_BRD04.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-04 Collections Sign-off Pack, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-04
name: Collections
doc_id: BIBS-SH-BRD-04
version: "2.0"
date: 1 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-4 Collections
h1_page_break: false
control:
  - version: "2.0"
    date: 1 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-4 Collections business sign-off pack (replaces the release note of the set)
distribution:
  - {name: "Shellah Marie C. Miranda, AVP, Product Owner", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: "Jose Melvin Jarin, AVP, Product Owner; Marketing heads (Combank and Corbank, Retail Marketing); Head - Comptrollership", role: Approvers, organisation: BDOI, purpose: Sign-off of the addendum functions and of their units' rows}
  - {name: "John Benedict S. Santos, VP and Head, BDOI Operations", role: Reviewer, organisation: BDOI, purpose: "Review of the process with Operations and of the exports"}
  - {name: "Collections and Marketing Support, HO Marketing Support BBG and Marketing; Operations FT&P", role: Reviewers, organisation: BDOI, purpose: Review of the screens and rows of their units}
  - {name: "Collection handlers and team leads, Marketing AOs, handlers and team leads, unapplied payment handlers, Cashiering and Commission Receivables", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: Information Technology Group, role: Reviewer, organisation: BDOI, purpose: "Scheduled runs, files, access; the Technical Specification"}
  - {name: "Dan Ace Cauton and Zean C. Ibay, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-4 Collections of BIBS, screen by screen, so that the Collections and Marketing units can confirm what they will get and sign it off: the PR Worklist and the collection account, assignment, collector dispositions and efforts, promises to pay, installment plans and statements of account, escalations, the collector side of the unapplied payments with the requests to Cashiering, the Collections files and the set-up. Signing freezes the content, the screens and the navigation of Collections; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-04_Collections of Drop 1.

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

SIT users of the seed data for the review: clxhandler (Collection Handler), clxtl (Collection Team Lead), clxuh (Section Head), ao (Marketing Account Officer), mkttl (Marketing Team Lead), mkthandler (Marketing Handler), upphandler (Unapplied Payment Handler), proc (Processing), cashier (Cashiering), disb (Disbursement), comptrol (Comptrollership), badmin (Business Administrator), admin (System Administrator) and auditor. The passwords are sent separately to the named reviewers.

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
| Mon 23-Nov-2026 | 09:30-12:00 | Kick-off; the PR Worklist and the collection account, assignment, efforts and dispositions with the hand-off to Cashiering (walkthrough A, walkthrough E steps 1-2) | Collections and Marketing Support, Marketing, Cashiering |
| Tue 24-Nov-2026 | 09:30-12:00 | Promises to pay, escalations and escalation rules; installment plans and statements of account (walkthroughs B and D, walkthrough E step 3) | Collections and Marketing Support, Marketing heads |
| Wed 25-Nov-2026 | 09:30-12:00 | Unapplied payments and the requests to Cashiering; Collections files, set-up, the upload template and the commission receivable rows (walkthrough C, walkthrough E step 4) | Unapplied payment handlers, Cashiering, Commission Receivables, Comptrollership, BDOI IT |

# Who signs what

The set holds business content only: the screens and fields, the columns of the lists, files and the template that the user sees or fills in, the validations, business rules, messages, notifications, documents, walkthroughs and files. The business users sign that content. Technical content (the interfaces between systems, data storage, security set-up) is not part of the set: it is in the Technical Specification, a separate document reviewed by BDOI IT. The roles are those of the approval sheets of the Collections BRD: iorta TechNXT and the Business Analyst of Business Project Services prepare the set, Collections and Marketing Support with Marketing and Operations: Financial Transactions and Processing provide the input, the Operations Head and BDOI IT review it, and the Program Manager of Business Project Services, the Product Owners, the Marketing heads and the Head of Comptrollership approve it.

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

Signing freezes the Collections screens, fields, navigation, actions, business rules, messages, notifications, documents and files, the upload template and the cross-BRD contract as specified. Configuration values marked "default" (threshold, aging, edit-lock time, export cap, list entries, disposition attributes, escalation and assignment rules, the statement template) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs (Collections reads the Operations ledger and hands work to Cashiering, Commission Receivables and Adjustment), approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
