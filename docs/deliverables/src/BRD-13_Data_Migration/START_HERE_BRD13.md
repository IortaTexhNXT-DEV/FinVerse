---
# Source of "00 Start Here - Guide to the BRD-13 Data Migration sign-off set" (Word), release set v2.0.
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-13_Data_Migration/START_HERE_BRD13.md
# The map, the reading order and the steps come from pack/guide.yaml (the same source as the guide deck).
title: Start Here
subtitle: Guide to the BRD-13 Data Migration sign-off set, release set v2.0
doc_type: Start Here Guide
doc_code: StartHere
brd: BRD-13
name: Data Migration
doc_id: BIBS-SH-BRD-13
version: "2.0"
date: 5 October 2026
status: Issued for BDOI business sign-off
header_title: Start Here BRD-13 Data Migration
h1_page_break: false
control:
  - version: "2.0"
    date: 5 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the BRD-13 Data Migration business sign-off set (one handbook, one workbook and the test plan in place of the separate migration documents)
distribution:
  - {name: "Program Manager, Business Project Services", role: Business owner and approver, organisation: BDO Unibank ESG, purpose: Sign-off}
  - {name: Data Migration Lead, role: Reviewer and signatory, organisation: BDOI, purpose: "Object decisions, trial migrations, cut-over plan"}
  - {name: "Data owners: Marketing Business System, Retail and Corporate Marketing, Operations, Disbursement, Compliance and Analytics", role: Reviewers and signatories, organisation: BDOI, purpose: Review of their objects, templates, code maps and reconciliation}
  - {name: "Comptrollership", role: Reviewer and signatory, organisation: BDOI, purpose: "Legacy accounts, GL opening, reconciliation, opening-balance adjustments, year-end option"}
  - {name: Renewal processing team, role: Reviewer and signatory, organisation: BDOI, purpose: "Renewal advices already sent, day-1 renewal queue"}
  - {name: BDOI IT, role: Reviewer and signatory, organisation: BDOI, purpose: "Extracts, transfer, freeze, read-only access, decommissioning"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests, the migration runs"}
---

# What this set is for

This set describes the proposed migration of the BDOI legacy data (EBIX, QPS, ISYS, CMS and the Excel trackers) into BIBS and the Migration Console that runs it, so that the business owner, each data owner, Comptrollership, the Renewal processing team and BDOI IT can confirm what moves, how it is checked and how the cut-over is run, and sign it off. Signing fixes the decisions per data object, the load templates, the screens, rules and messages of the Migration Console, the reconciliation and the sign-off gates, and the cut-over plan; a later change goes through the Change Management Register with its mandays. The screenshots use fictitious seed data only.

# The set at a glance

The files are numbered so that they sort in reading order in the folder BRD-13_Data_Migration of Drop 0.

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: guide-map
```

# Where to start

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: guide-reading
```

SIT users of the seed data for the review: miglead (Data Migration Lead), migsteward (Data Steward), migowner (data owner), migops (Migration Operator), migrecon (reconciliation approver), miggonogo (go / no-go board), comptrol (Comptrollership), legacyaudit and legacyrev (Legacy Inquiry and its access log), cashier, cashtl and topmgmt (Unapplied to Income, Legacy PR 2307 Reversal), commrec and commtl (DP PR Legacy Reversal). The passwords are sent separately to the named reviewers.

# Steps up to closure

```pack
plugin: build_dm_pack.py
source: pack/pack.yaml
render: guide-steps
```

**How to review.** Read the Handbook part of your role, or follow the same screens on SIT during the sessions. In the Migration Workbook, set BU review to Accept, Change requested or Comment on each row of your objects (sheets Object catalogue, Data owners, Extract planning, Code maps, Validation rules, Cut-over tasks, Reconciliation sign-off; on the load template sheets, listed in load order on the sheet Load templates, the BU review rows above each column) and on the screen sheets, write the change in BU comment, and add your name and the date. On the sheet Proposed rules, record the BDOI decision on each proposed rule. Questions go to the Comments log sheet, with the load template and the column when they concern one. One workbook per unit is enough.

<!-- table: widths=3.6,2.4,7.2,4.4 caption="SIT walkthrough sessions" -->
| Date | Time | Session | Units |
|---|---|---|---|
| Wed 7-Oct-2026 | 09:30-12:00 | Kick-off with the guide deck; the approach, the objects and their decisions; Migration Home, Data Objects, Code Maps, Layouts and Rules, and the load templates | Business owner, data owners, Data Migration Lead, BDOI IT |
| Thu 8-Oct-2026 | 09:30-12:30 | One object from extract to acceptance: Extracts, Batches and a batch, Client Matching, Reconciliation, Sign-off; rejected rows and the resubmission of the RA-sent file | Data owners and stewards, Renewal processing team, BDOI IT |
| Fri 9-Oct-2026 | 09:30-12:30 | Financial objects and the cut-over: legacy invoices on Invoice 360, the legacy batches, Opening-Balance Adjustments, Migration Clearing, Cutover and go / no-go, Run-off and Decommissioning, Legacy Inquiry | Comptrollership, Operations, Cashiering, Commission, Compliance, BDOI IT |

<!-- table: widths=4.2,12.4 caption="Key dates" -->
| Date | What |
|---|---|
| Mon 5-Oct-2026 | Set issued (release set v2.0) |
| 7 to 9-Oct-2026 | Kick-off and SIT walkthroughs |
| Fri 16-Oct-2026 | Decisions on the proposed rules marked M1 |
| Wed 21-Oct-2026 | Reviewed workbooks returned to the project team |
| Mon 26-Oct-2026 | Q&A session on the consolidated comments log |
| Wed 28-Oct-2026 | Release set v2.1 with the agreed corrections, when needed |
| Fri 30-Oct-2026 | Sign-off meeting (M2): objects decided, layouts frozen, owners named |

# Change control after sign-off

Signing fixes the decisions per data object, the load templates and the control file, the code map sets, the validation rules, the screens, fields, actions, rules and messages of the Migration Console, the markers of migrated data, the legacy batches, the reconciliation per object, the sign-off gates, the cut-over plan and the cross-BRD contract as specified. Settings marked "default" (thresholds, tolerances, retention days, dates of the cut-over calendar) and the open decisions with their dates are not fixed. A change to anything fixed is raised in the Change Management Register with the object, layout, screen, rule or message concerned, assessed with its mandays and its effect on the other BRDs, approved by the owners of every BRD it touches, and delivered as a new version of the set; only the affected pages and layouts are signed again.
