---
# Source of "00 Start Here - Guide to the BRD-01 Sign-off Pack" (Word), release set v2.1 of BRD-1 New Business.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-01_New_Business/START_HERE_BRD01.md
# The map, the reading order and the steps come from signoff/brd01/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-01 New Business Sign-off Pack, release set v2.1
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-01
name: New Business
doc_id: BIBS-SH-BRD-01
version: "2.1"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-1 New Business
h1_page_break: false
control:
  - version: "2.0"
    date: 1 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-1 New Business business sign-off pack (replaces the release note of the set)
  - version: "2.1"
    date: 08-Oct-2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: "Re-based on New Business (NB) BRD 05132026 (Addendum after IT Walkthrough, approved 13-May-2026); user-story view and storyboard index added to the FRS; every file of the set re-issued at v2.1"
distribution:
  - {name: "Product Owner, BDOI", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner, organisation: BDOI, purpose: Review and sign-off}
  - {name: "Marketing, TSU, Processing, Comptrollership", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: "Compliance and BDOI IT", role: Reviewers, organisation: BDOI, purpose: "KYC and screening; interfaces and access"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-1 New Business of BIBS, screen by screen, so that each BDOI business unit can confirm what it will get and sign it off. Signing freezes the content, the screens and the navigation of New Business; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# What changed in v2.1

BDOI re-issued the New Business BRD on 13 May 2026 (New Business (NB) BRD 05132026). It adds, in front of the earlier pack, the Addendum after the IT walkthrough of 28 to 30 April 2026, which restates five requirements; every other page is unchanged and now comes 10 pages later. Release set v2.1 is re-based on it:

- **EOPT details on the placement (BRNB.100).** The placement slip sent to the insurer carries the client's taxpayer name, TIN and registered address, as in force at the final sending (new FR-NB-087).
- **Service invoices (BRNB.100.1).** The owner is notified of every sending, successful or failed (FR-NB-117).
- **Incentive indicator (BRNB.107).** Set only when the booked invoice is fully paid, after the applicable endorsements are considered, and evaluated again on endorsement and cancellation; the same rule applies to Renewal (FR-NB-118).
- **Bulk quotations (BRNB.042).** One or more quotations to one or more recipients with their supporting documents; a batch sends the valid quotations and lists the others (FR-NB-044, FR-NB-046).
- **Insurance Advice (BRNB.060).** Sent automatically to the recipient enrolled in the MIS set-up (new FR-NB-107).
- **For confirmation.** Seven new items in FRS chapter 21 (CLR-NB-44 to CLR-NB-50).
- **New appendices.** The FRS adds a user-story view (one row per BRD requirement with its FRs, acceptance criteria and test conditions) and a storyboard index of the walkthrough steps.

The changed screens are flagged Changed in this issue in the UX screen register; the changes are taken through at the Q&A session.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-01_New_Business of Drop 1.

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

SIT users of the seed data for the review: ao and mkttl (Marketing), tsu and tsulead (TSU), proc, proctl and epol (Processing), approver (New Business Approver), adjust (Adjustment), badmin (Business Administration), admin (System Administrator) and auditor. The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
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

# Who signs what

The set holds business content only: the screens and fields, the columns of the lists, reports and templates that the user sees or fills in, the validations, business rules, messages, notifications, documents, walkthroughs and reports. The business users sign that content. Technical content (the interfaces between systems, data storage, security set-up) is not part of the set: it is in the Technical Specification, a separate document reviewed by BDOI IT. The roles are those of the approval sheet of the BRD: iorta TechNXT prepares the set, the business units provide the input and review their parts, BDOI IT reviews the interfaces, the user access and the Technical Specification, the Program Manager of Business Project Services reviews the traceability and the completeness, and the Product Owner approves the whole set.

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

Signing freezes the New Business screens, fields, navigation, actions, business rules, messages, notifications, documents, upload templates and the cross-BRD contract as specified. Configuration values marked "default" (SLA hours, thresholds, list entries, templates) are not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs, approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
