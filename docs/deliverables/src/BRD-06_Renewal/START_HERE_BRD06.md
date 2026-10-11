---
# Source of "00 Start Here - Guide to the BRD-06 Sign-off Pack" (Word), release set v2.0 of BRD-6 Renewal.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-06_Renewal/START_HERE_BRD06.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-06 Renewal Sign-off Pack, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-06
name: Renewal
doc_id: BIBS-SH-BRD-06
version: "2.0"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-6 Renewal
h1_page_break: false
control:
  - version: "2.0"
    date: 8 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: "First issue with the BRD-6 Renewal business sign-off pack, on the Renewal BRD issued by BDOI on 8-Oct-2026 with the Renewal Addendum (Walkthrough) of May 2026 (replaces the FRS and test plan v1.1)"
distribution:
  - {name: "Shellah Marie C. Miranda, AVP, Product Owner", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: "Jose Melvin Jarin, AVP, Product Owner; Mark Joseph C. Makalintal, SAVP, Unit Head - Combank and Corbank; Edmundante F. Ramirez, VP, Head - Retail Marketing; Roderick Lim, FVP, Head - Corporate and Retail Marketing", role: Approvers, organisation: BDOI, purpose: Sign-off of the addendum functions and of their units' rows}
  - {name: "Rhommel Mark Galler, Marketing Business Services and System Support (MBS)", role: Reviewer, organisation: BDOI, purpose: "Review of the set-up, lists of values, templates and menus"}
  - {name: "Marketing Team Leaders and Account Officers, Processing Team Leaders and Officers, Contact Center and LAMD (the input providers of the approval sheets)", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: Information Technology Group, role: Reviewer, organisation: BDOI, purpose: "Scheduled runs, uploads, protected e-mails, access; the Technical Specification"}
  - {name: "Dan Ace R. Cauton, Edward Kenneth Cadena and Zean C. Ibay, ESG - Business Project Services", role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-6 Renewal of BIBS, screen by screen, so that the Marketing, Processing, Contact Center and LAMD units can confirm what they will get and sign it off: the expiry list and its extraction, the checks and the Clean, Review and Exception classification, assignment and transfers, the disposition and the Team Leader's review, processing and the insurer's answer, the Renewal Advice and the closing letters, the client's acceptance up to placement and booking, the follow-ups, the LAMD reports and the Renewal set-up. It follows the Renewal BRD as issued by BDOI on 8-Oct-2026, including the Renewal Addendum (Walkthrough) approved in May 2026. Signing freezes the content, the screens and the navigation of Renewal; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-06_Renewal of Drop 1.

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

SIT users of the seed data for the review: mkttl (Marketing Team Leader, CBG Metro Team 1), ao (Marketing Account Officer, CBG Metro Team 1), rnwtl (Renewal Team Leader, Corporate Marketing Team 1), ao2 (Marketing Account Officer, Corporate Marketing Team 1), proctl (Processing Team Leader), proc (Processing Officer), contactc (Contact Center), lamd (LAMD), badmin (Business Administrator), admin (System Administrator) and auditor. The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-steps
```

**How to review.** Open the FRS chapters of your unit or the same screens on SIT. In the sign-off workbook, set BU review to Accept, Change requested or Comment on each row you review (sheets Screen standards, Screen catalogue, Field register, Business rules, Messages), write the change in BU comment, and add your name and the date. Questions go to the Comments log sheet. One workbook per unit is enough. Chapter 21 of the FRS lists the proposed rules and clarifications on which BDOI is asked to decide; the appendices of the FRS give the user-story view of every BRD requirement and the storyboard index of the walkthroughs.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Units |
|---|---|---|---|
| Wed 4-Nov-2026 | 09:30-12:00 | Kick-off; the expiry list, the checks and the classification, assignment, the disposition and the Team Leader's post (walkthrough A, walkthrough E) | Marketing Team Leaders and Account Officers, MBS |
| Thu 5-Nov-2026 | 09:30-12:00 | Processing, insurer batches and answers, the Renewal Advice; Not for Renewal and the closing letters (walkthroughs B and D) | Processing Team Leaders and Officers, Marketing |
| Fri 6-Nov-2026 | 09:30-12:00 | Contact Center follow-up, acceptance, hold cover and placement; LAMD reports; Renewal Setup and go-live (walkthrough C) | Contact Center, LAMD, MBS, BDOI IT |

# Who signs what

The set holds business content only: the screens and fields, the columns of the lists, files and templates that the user sees or fills in, the validations, business rules, messages, notifications, documents and walkthroughs. The business users sign that content. Technical content (the interfaces between systems, data storage, security set-up) is not part of the set: it is in the Technical Specification, a separate document reviewed by BDOI IT. The roles are those of the approval sheets of the Renewal BRD and its addenda: iorta TechNXT and the Business Analysts of Business Project Services prepare the set, the Marketing and Processing users with MBS provide the input, MBS, the Program Manager and the BA Unit Head of Business Project Services and BDOI IT review it, and the Product Owners, the Combank and Corbank Unit Head and the Marketing heads approve it. The approval sheets mark the Comptrollership approval as not required: a renewal books through the New Business booking.

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

Signing freezes the Renewal screens, fields, navigation, actions, business rules, messages, notifications, documents, the upload templates and the cross-BRD contract as specified. Configuration values marked "default" (lead days, the TSI threshold, notice, reminder, waiting and ageing days, hold cover durations, list entries, check settings, classification rules, the decision matrix and the templates) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs (Renewal reads the booked accounts and hands the renewal account to New Business placement and booking), approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
