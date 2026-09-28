---
# Source of "00 Start Here - Guide to the BRD-11 Sign-off Pack" (Word), release set v2.0 of BRD-11 User Access Maintenance.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-11_User_Access_Maintenance/START_HERE_BRD11.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-11 User Access Maintenance Sign-off Pack, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-11
name: User Access Maintenance
doc_id: BIBS-SH-BRD-11
version: "2.0"
date: 27 September 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-11 User Access Maintenance
h1_page_break: false
control:
  - version: "2.0"
    date: 27 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-11 User Access Maintenance business sign-off pack
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: "Business Administration (process owner of user access)", role: Business owner of the process, organisation: BDOI, purpose: Review and sign-off; configuration inputs}
  - {name: "BDOI Information Security", role: Signatory, organisation: BDOI, purpose: "Role matrix, menus by persona, separation of duties, password and session values"}
  - {name: "BDOI IT", role: Signatory, organisation: BDOI, purpose: "Sign-in and identity integration assumptions (EIAM, UIDM-ISC); System Administrator functions"}
  - {name: "Unit heads of the other BRDs", role: Reviewers, organisation: BDOI, purpose: The menus of their personas}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-11 User Access Maintenance of BIBS, screen by screen, so that BDOI can confirm what it will get and sign it off. User Access Maintenance is a set of Drop 0 (Setup and Data Migration): it holds the users, the group profiles and the permissions that every other BRD checks, and it decides the menu each persona sees. The pack therefore shows the menu of every persona, and BDOI Information Security signs the role matrix and the separation-of-duties rules with the business owner, while BDOI IT signs the sign-in and identity integration assumptions. Signing freezes the content, the screens and the navigation of User Access Maintenance; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-11_User_Access_Maintenance of Drop 0.

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

SIT users of the seed data for the review: requestor (Requestor), uamapprover (Approver), secapprover (Second Approver), badmin (Business Administrator), admin (System Administrator), auditor (Auditor), infosec (Information Security Officer) and approver (New Business Approver); the users of the other personas are named in FRS chapter 12. The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: ../signoff/signoff_pack.py
source: pack/pack.yaml
render: guide-steps
```

**How to review.** Open the FRS chapters of your area or the same screens on SIT. In the sign-off workbook, set BU review to Accept, Change requested or Comment on each row you review (sheets Screen standards, Screen catalogue, Field register, Business rules, Messages, and the Menu sheet of each persona you own), write the change in BU comment, and add your name and the date. Questions go to the Comments log sheet. One workbook per unit is enough.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Units |
|---|---|---|---|
| Mon 9-Nov-2026 | 09:30-12:00 | Kick-off; a new user from the request to the first sign-in with the menu of the persona (walkthrough A); a change, a revocation and a group-profile change (walkthrough B) | Business Administration, Information Security, unit heads |
| Tue 10-Nov-2026 | 09:30-12:00 | Refused cases and messages (walkthrough C); the reports, the matrix and the insurer roles hidden from BDOI (walkthrough D); sign-in, passwords and sessions; the configuration input templates | Information Security, BDOI IT, Business Administration, Data Migration working group |

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

# Configuration inputs

The workbook of configuration input templates lists what the business provides to set up User Access Maintenance: the users and their group profiles (loaded with the bulk request), the role-to-permission matrix (implemented by group-profile requests), the approvers, approval rules and limits, the separation-of-duties rules, the password, session and sign-in values and the lists of values (FRS chapter 19). The owners start the templates during the review; the users and the role matrix are due before the first data migration mock, and the parameters agreed with Information Security before UAT. The workbook opens on a Start here sheet with the templates in the order they are filled in and a link to each; every template sheet carries its own guide above the columns (mandatory, format, allowed values, what to enter), drop-downs and one example row, and questions are raised in the workbook on the sheet Questions and comments.

# Change control after sign-off

Signing freezes the User Access Maintenance screens, fields, navigation, actions, separation-of-duties and other business rules, messages, notifications, reports, templates and the cross-BRD contract as specified. The configuration itself (the group profiles and their permissions, the users, the approvers, the list values and the parameters) is maintained through requests and audited changes and is not frozen; the menus follow the profiles. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs, approved by the owners of every BRD it touches and, for a separation-of-duties or sign-in change, by BDOI Information Security, and delivered as a new version of the set; only the affected pages are signed again.
