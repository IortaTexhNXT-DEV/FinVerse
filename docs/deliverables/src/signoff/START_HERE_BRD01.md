---
# Source of "00 Start Here - Guide to the BRD-01 Sign-off Pack" (Word), release set v2.0 of BRD-1 New Business.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/signoff/START_HERE_BRD01.md
# The map, the reading order and the steps come from signoff/brd01/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-01 New Business Sign-off Pack, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-01
name: New Business
doc_id: BIBS-SH-BRD-01
version: "2.0"
date: 27 September 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-1 New Business
h1_page_break: false
control:
  - version: "2.0"
    date: 27 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-1 New Business business sign-off pack (replaces the release note of the set)
distribution:
  - {name: "Product Owner, BDOI", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Marketing, TSU, Processing, Comptrollership", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: "Compliance and BDOI IT", role: Reviewers, organisation: BDOI, purpose: "KYC and screening; interfaces and access"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows BRD-1 New Business as built in BIBS on 27-Sep-2026, screen by screen, so that each BDOI business unit can confirm what it will get and sign it off. Signing freezes the content, the screens and the navigation of New Business; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-01_New_Business of Drop 1.

```pack
plugin: ./signoff_pack.py
source: ./brd01/pack.yaml
render: guide-map
```

# Where to start

```pack
plugin: ./signoff_pack.py
source: ./brd01/pack.yaml
render: guide-reading
```

SIT users of the seed data for the review: ao and mkttl (Marketing), tsu (TSU), proc, proctl and epol (Processing), badmin (Business Administration). The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ./signoff_pack.py
source: ./brd01/pack.yaml
render: guide-steps
```

**How to review.** Open the FRS chapters of your area or the same screens on SIT. In the sign-off workbook, set BU review to Accept, Change requested or Comment on each row you review (sheets Screen standards, Screen catalogue, Field register, Business rules, Messages), write the change in BU comment, and add your name and the date. Questions go to the Comments log sheet. One workbook per unit is enough.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Units |
|---|---|---|---|
| Thu 1-Oct-2026 | 09:30-12:00 | Kick-off; client onboarding, KYC and package quotation (walkthrough A steps 1-9) | Marketing, MBS |
| Fri 2-Oct-2026 | 09:30-12:00 | Non-package placement: PRF, quotation slip, insurer responses, proposal slip (walkthrough B) | TSU, Marketing |
| Mon 5-Oct-2026 | 09:30-12:30 | Validation, payment, placement, issuance and booking; returns and messages (walkthrough A steps 10-17, walkthrough C) | Processing, Comptrollership |
| Tue 6-Oct-2026 | 09:30-11:30 | Bulk uploads with the error file, reports and dashboard, administration screens | MBS, Business Administration, Compliance, BDOI IT |

# Change control after sign-off

Signing freezes the New Business screens, fields, navigation, actions, business rules, messages, notifications, documents, upload templates and the cross-BRD contract as specified. Configuration values marked "default" (SLA hours, thresholds, list entries, templates) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs, approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
