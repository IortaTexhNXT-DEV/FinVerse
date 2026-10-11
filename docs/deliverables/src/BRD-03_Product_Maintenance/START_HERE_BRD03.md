---
# Source of "00 Start Here - Guide to the BRD-03 Sign-off Pack" (Word), release set v2.1 of BRD-3 Product Maintenance.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-03_Product_Maintenance/START_HERE_BRD03.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-03 Product Maintenance Sign-off Pack, release set v2.1
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-03
name: Product Maintenance
doc_id: BIBS-SH-BRD-03
version: "2.1"
date: 8 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-3 Product Maintenance
h1_page_break: false
control:
  - version: "2.0"
    date: 27 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-3 Product Maintenance business sign-off pack
  - version: "2.1"
    date: 8 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: "Re-based on the Product Maintenance BRD and Addendum (unchanged) and the Drop 0 decisions; sign-in follows BRD-11; packages in force at go-live and automatic renewal drafting as clarifications; user-story view and storyboard index added to the FRS"
distribution:
  - {name: "Product Owner, Marketing Business System", role: Approver, organisation: BDOI, purpose: Sign-off}
  - {name: Marketing Business Services and System Support (MBS), role: Business owner of the set-up, organisation: BDOI, purpose: Review and sign-off; configuration inputs}
  - {name: "Technical Support Unit (TSU) and Marketing", role: Reviewers, organisation: BDOI, purpose: Review of their screens and rows}
  - {name: "Comptrollership and BDOI IT", role: Reviewers, organisation: BDOI, purpose: "Rates, commission and incentives; interfaces and access"}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability and sign-off coordination}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# What this pack is for

This pack shows the proposed BRD-3 Product Maintenance of BIBS, screen by screen, so that each BDOI business unit can confirm what it will get and sign it off. Product Maintenance is the first set of Drop 0 (Setup and Data Migration): it holds the products, package versions, rates and incentive criteria that New Business, Renewal, Employee Benefits, Submitted Policies, Operations and Accounting use, and the data migration loads its masters at go-live. Signing freezes the content, the screens and the navigation of Product Maintenance; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The pack at a glance

The files are numbered so that they sort in reading order in the folder BRD-03_Product_Maintenance of Drop 0.

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

SIT users of the seed data for the review: ao and mkttl (Marketing), tsu, tsulead and tsuhead (TSU), mbs (MBS), mancom (ManCom), badmin (Business Administration) and approver (New Business Approver). The passwords are sent separately to the named reviewers.

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
| Wed 4-Nov-2026 | 09:30-12:00 | Kick-off; the package request from the form to the ManCom sign-off (walkthrough A steps 1-14) | Marketing, TSU, MBS |
| Thu 5-Nov-2026 | 09:30-12:00 | Set-up, versions and validation, insurers and commission, advisory, pricing and the rate exception (walkthrough A steps 15-24); returns and messages (walkthrough B) | MBS, TSU, Business Administration, Marketing |
| Fri 6-Nov-2026 | 09:30-11:30 | Catalogue screens, rates and taxes, incentive criteria, package expiry; the configuration input templates and the package map | MBS, Comptrollership, Data Migration working group, BDOI IT |

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

The workbook of configuration input templates lists what the business provides to set up Product Maintenance and to load it at go-live: the product catalogue, coverages and rules, clauses, rate tables, insurers and commission rates, the packages in force, the package map, the document templates, the lists of values and the incentive criteria (FRS chapter 19). The owners start the templates during the review; the final content follows the data migration mocks and is due at the map freeze. The workbook opens on a Start here sheet with the templates in the order they are filled in and a link to each; every template sheet carries its own guide above the columns (mandatory, format, allowed values, what to enter), drop-downs and one example row, and questions are raised in the workbook on the sheet Questions and comments.

# Change control after sign-off

Signing freezes the Product Maintenance screens, fields, navigation, actions, business rules, messages, notifications, documents, configuration input templates and the cross-BRD contract as specified. The configuration content itself (products, versions, rates, clauses, templates, list values and parameters) is maintained by the business under maker-checker and is not frozen. A change to anything frozen is raised in the Change Management Register with the screen, field, rule or message concerned, assessed with its mandays and its effect on the other BRDs, approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages are signed again.
