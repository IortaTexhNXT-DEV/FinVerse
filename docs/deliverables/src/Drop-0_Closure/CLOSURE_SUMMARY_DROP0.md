---
# Source of "02 Drop 0 Closure Summary" (Word), Drop 0 closure set v2.0, for the BDOI steering committee.
# Build: python tools/deliverables/drop_closure.py (writes 01 the configuration inputs workbook and this document).
# The tables are ```pack blocks rendered by tools/deliverables/drop_closure.py from drop0.yaml, the clarification
# chapters of the three sign-off sets and the BRD-13 catalogue.
title: Drop 0 Closure Summary
subtitle: "Setup and Data Migration: sign-off sets, decisions, configuration inputs and the move to Drop 1"
doc_type: Closure Summary for the Steering Committee
doc_code: ClosureSummary
brd: Drop 0
name: Drop 0 Closure Summary
doc_id: BIBS-CS-DROP-0
version: "2.0"
date: 5 October 2026
status: Issued for the BDOI steering committee
header_title: Drop 0 Closure Summary
h1_page_break: true
control:
  - version: "2.0"
    date: 5 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Business Analysis
    approver: ""
    change: First issue with the three Drop 0 sign-off sets v2.0 and the Drop 0 configuration inputs workbook
distribution:
  - {name: "Program Manager, Business Project Services", role: Chair and approver, organisation: BDO Unibank ESG, purpose: Steering committee decision}
  - {name: "Product Owner, Marketing Business System", role: Business owner of BRD-03 and BRD-11, organisation: BDOI, purpose: Decisions and sign-off}
  - {name: "Head, Comptrollership", role: Owner of the finance set-up and the year-end option, organisation: BDOI, purpose: Decisions and configuration inputs}
  - {name: "Head, BDOI Information Security", role: Owner of the security settings, organisation: BDOI, purpose: Decisions and configuration inputs}
  - {name: "Head, BDOI IT", role: "Owner of extracts, sign-in and integrations", organisation: BDOI, purpose: Decisions and configuration inputs}
  - {name: Data Migration Lead, role: Owner of the migration objects, organisation: BDOI, purpose: Decisions}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Sessions, answers, revisions, change requests"}
---

# Purpose of this summary

This summary asks the BDOI steering committee to close Drop 0 (Setup and Data Migration) and to open Drop 1. It brings together, in one place, what the three Drop 0 sign-off sets ask the business to confirm, the number of proposed rules and clarifications that are still open in each set, the decisions BDOI takes to close the drop, the configuration inputs BDOI provides before go-live and when, the entry and exit criteria, the risks and dependencies, and the change control that applies once the sets are signed.

The companion workbook **01_BIBS_Drop-0_Configuration_Inputs_v2.0.xlsx** lists every configuration input with its BDOI owner, its due date relative to go-live and its one route into BIBS. Go-live is proposed for Monday 3 January 2028 (T), at the year-end boundary, subject to the confirmation of the year-end option by Comptrollership (decision DEC-01).

# Scope of Drop 0

Drop 0 prepares the platform on which every other drop runs and moves the legacy data into it. In the BDOI drop plan it covers:

- 0.1 Accessibility and Login, 0.2 Authorization and 0.3 User Maintenance: BRD-11 User Access Maintenance;
- 0.4 Data Management: the GL accounts and reference tables of Comptrollership (the finance set-up of BRD-5 used by Drop 1);
- 0.5 Workflow: the approval stages of the Drop 0 functions;
- 0.6 Product Maintenance: BRD-03 Product Maintenance;
- the Data Migration stream: BRD-13 Data Migration;
- the Drop 0 integrations: EIAM, UIDM-ISC, LMS, HL-LOAS, PMS, CMS / New BOB, OBPCS, AFTS, Old BOB, ECM, CCM, M365 and TFS.

Requirements and mapping run from September to November 2026; the migration is prepared from November 2026 to March 2027, tested in SIT from April to July 2027 and in UAT from August to October 2027; the full migration and the cut-over run from November 2027 to January 2028.

# The three sign-off sets

Each set is issued as one release set in its own folder of Drop 0 and is signed as one unit by its business owner. Signing a set freezes its screens, fields, rules, messages, notifications and interface contract as specified.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: sets
```

# Proposed rules and clarifications for confirmation

Each set closes with a chapter of proposed business rules and clarifications for confirmation: the points where the proposed rule differs from the BRD text or where BIBS needs a BDOI decision to complete the rule. The FRS of BRD-03 and BRD-11 also carry their open questions; the Data Migration Handbook carries its open decisions with the date each is needed by. The counts below are taken from those chapters.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: clarifications
```

Every proposed rule is decided with the review of its set (Agree, Agree with change, Disagree, Need more information), or accepted as the working assumption with its decision date. A decision that only sets a value (a list, a parameter, a template) is applied as configuration through the workbook; a decision that changes a screen, field, rule or message is applied in version 2.1 of the set.

# Consolidated decisions BDOI takes

The decisions below gather the items of the three clarification chapters that shape the other drops, the cut-over or the configuration. The column From names the items of the sets they answer.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: decisions
```

# Configuration inputs

The workbook has one register row per configuration input, grouped in twelve areas: organisation; chart of accounts and accounting; banks and bank accounts; document numbering; lists of values; system and security parameters; notifications and documents; products and packages; insurers and partners; users, roles and approvals; module rules and set-up; data migration reference data. Each row has one route:

- **Template**: the owner fills the template sheet named in the row; the rows are entered or uploaded on the named screen and authorised by a second user. The workbook holds the templates CI-01 to CI-10 of BRD-03 and CI-01 to CI-06 of BRD-11, taken from the same sources as their sets, and the fifteen Drop 0 templates D0-01 to D0-15 for the company, branches, holidays, dimensions, employees, chart of accounts, categories and numbering, accounting rules, legacy accounts, tax codes, bank accounts, check books, petty cash, statement layouts and approval limits.
- **Screen**: the owner enters the item directly on the named screen and records the decision in the register.
- **Migration object**: the item is extracted from the legacy systems and loaded by the named object of the Migration Workbook of BRD-13. It is not typed in the configuration workbook; the sheet Migration cross-reference names the object of every such item, so that no item reaches BIBS twice.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: inputs
```

## Timeline

The due dates are set against the programme milestones and counted back from go-live. The platform set-up that Drop 1 SIT uses is due first; the production values are due before the dress rehearsal, and the reference values freeze with the code maps seven days before go-live.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: timeline
```

# Entry and exit criteria

## Entry criteria for closing Drop 0

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: close_entry
```

## Exit criteria: Drop 0 is closed when

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: close_exit
```

## Entry criteria for Drop 1

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: drop1_entry
```

# Risks and dependencies

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: risks
```

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: dependencies
```

# Change control

- **Before signature.** A question on a set is answered in the comments log of its sign-off workbook without changing the text; a correction is made once, in version 2.1 of the set, with the changed rows marked.
- **After signature.** Nothing in a signed set changes without a change request in the Change Management Register. The request states its effect on the other BRDs through the cross-BRD interface contract of the set and its mandays, is approved by the owners of every BRD it touches, and is delivered as a new version of the set (2.1, 2.2) in which only the changed pages are signed again.
- **Configuration inputs.** A value in the workbook is changed by the owner of the row until the due date, and after it by a change request. The reference values and code maps freeze seven days before go-live (D7); a change after the freeze is a change request with a new code map version.
- **Open items.** A question still open at signature stays open on its stated working assumption until BDOI answers it; the answer is applied as configuration or through a change request.

# Closure sign-off

By signing, the steering committee closes Drop 0 on the basis of the three signed sets, records the decisions of chapter 5 with their owners and dates, accepts the configuration inputs timeline of chapter 6 and opens Drop 1.

```pack
plugin: ../../../../tools/deliverables/drop_closure.py
render: signoff
```
