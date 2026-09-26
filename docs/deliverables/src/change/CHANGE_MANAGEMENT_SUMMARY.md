---
# Word summary of the Change Management Register (BRD-00, programme level).
# Build: python docs/deliverables/src/change/build_change_register.py (expands the <!-- cr:... --> tables and the
# {{...}} figures from change_register.yaml, then builds this document with tools/deliverables/bdoi_docx.py).
title: Change Management Register - Summary
subtitle: Deviations from the BRDs that go through change control, with effort in man-days
doc_type: Change Register
doc_code: Change_Management
brd: BRD-00
name: Change Management Summary
doc_id: BIBS-CMR-BRD-00-S
version: "1.0"
date: 26 September 2026
status: Issued for BDOI review
header_title: Change Management Register - Summary
h1_page_break: false
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Business Analyst lead
    reviewer: iorta TechNXT Solution Architect
    approver: BIBS Product Owner (pending)
    change: First issue with the register v1.0 (status as of 26 September 2026)
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Chair of the Change Control Board"}
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Plan, drops and effort"}
  - {name: "Owners of BRD-1 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Decision owners of the CRs of their BRD"}
  - {name: "BDOI IT (integration owners)", role: Reviewer, organisation: BDOI, purpose: "Interface and infrastructure CRs"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Impact analysis, scheduling, FRS revisions"}
---

# Purpose and scope

## Purpose

> [!NOTE] Status of this document
> {{statement}} The register records where BIBS differs from the BRDs and what it takes to close each difference. An approved change reaches the FRS only through an FRS revision that cites the CR ID.

The BRDs were written before BIBS and, in several places, for the legacy systems (EBIX, QPS, ISYS) that BIBS replaces. The FRS of each BRD records where the built or designed behaviour differs from the BRD text. BDOI's decisions of 26 September 2026 (single go-live, no Employee Benefits portal, documents in S3, reinsurance through Remittance, package remapping at sanitation, January to May 2028 renewals in BIBS, year-end option A) add more. This document and its workbook bring all of these into one master list, so that each deviation is decided once by the Change Control Board (CCB), with its effort known.

## What the register contains

The workbook *BIBS_Change_Register_BRD-00_Change_Management_v{{version}}.xlsx* has one row per deviation: {{total_crs}} change requests (CR-0001 to {{last_id}} in the workbook), each with the drop, the BRD and its requirement IDs and pages, the FRS FR IDs, the module, the deviation type, the BRD requirement, the as-built or proposed behaviour, the reason, the business impact, the affected departments, the effort in man-days (Analysis, Build, Test, Documentation), the priority, the CR status, the BDOI decision owner, the register and question references, and the target drop or release.

<!-- table: widths=3.6,12.9 caption="Sheets of the workbook" bold=first -->
| Sheet | Content |
|---|---|
| Cover, Document control | Title, version, the statement above, baseline, version history, review and approval, distribution |
| How to read | Reading guide, the nine deviation types, CR statuses, priorities, size classes and every column |
| Register | The {{total_crs}} CRs; filters, drop-down lists, frozen headers; printed on A3 landscape, two pages across |
| Summary | Counts and man-days by drop, BRD, type, status and priority, recalculated from the Register |
| Effort basis | Team roles, the man-day, what is included and excluded, size classes, man-days by activity |
| Change control | The eight-step process with roles and times; the proposed CCB membership and its rules |
| CR form | The change request form to fill, sign and file for each CR |

## Sources

Deviations were taken from these sources and merged where two sources record the same point; the Source column of the register keeps every reference.

- The FRS of each BRD: "Differences between the built behaviour and the BRD" (FRS BRD-2 and BRD-4 section 1.6, BRD-5 volumes 1 and 2 section 1.7), "Built behaviour that differs from the BRD" (FRS BRD-1 section 10.4), the recorded differences of FRS BRD-7, BRD-8 and BRD-9, the scope exclusions of every FRS and the out-of-scope IDs of FRS BRD-6, and the gaps of the umbrella FRS BRD-00 (section 5).
- The requirement specs (fit classes CHANGE, NEW and OUT; "Built with parked item" and "Not built" rows of the as-built tables) and the designs (as-built sections and parked lists).
- The BRD discrepancy and clarification register v1.2 (conflicts between BRDs, BRD against platform, NFR items, DCR-210 to DCR-243).
- *docs/deliverables/PENDING_EDITS.md*, the programme alignment pack (drop and integration mismatches, IER), the architecture option and document storage decisions, and the cross-BRD decisions D1 to D10.
- BDOI's answers of 26 September 2026 (A1 to A6 and DMQ36 to DMQ39).

## What is not in the register

- Open questions that do not change a requirement (they stay in the discrepancy register until answered).
- The baseline build of the designed BRDs (Renewal, Employee Benefits, Customer Servicing Facility, Submitted Policies, Data Migration, the rest of Claims): building what the FRS specifies is not a change. Only the delta caused by a deviation is estimated.
- Document-quality items of the BRD pack (typing, numbering, names).
- Scope that a BRD itself puts out (for example New Business OOS-1 and OOS-2), except where a BDOI addendum removed a requirement that the main BRD still carries.


# The change-control process

## Steps and roles

Every deviation in the register, and every new change request, follows the same eight steps. The register is the log; the CR form is the record of each decision.

<!-- cr:process -->

## Change Control Board

The CCB decides every CR. The owner of the BRD concerned is the decision owner and attends for the CRs of that BRD.

<!-- cr:ccb -->

Rules of the board:

- Quorum: the chair (or a named delegate), the Program Manager and the decision owner of each CR on the agenda.
- The CCB meets every two weeks from October 2026 to go-live, and ad hoc for High priority items that block a drop.
- A CR above 25 man-days, or one that moves a drop or the go-live date, needs the Program Manager's approval.
- Rejected CRs keep their row with the reason; the BRD text stands.

## How the statuses are used in this issue

- **Implemented (as built, for acceptance)** - {{impl_crs}} CRs. The behaviour is built (or decided and documented) and differs from the BRD text. No work remains; the CCB records the acceptance, or raises a new CR if it wants the BRD text built instead (the "BRD as written" column gives the indicative effort).
- **For CCB review** - {{ccb_crs}} CRs. Impact analysis is complete and the item can be decided now, most of them on a proposal or a BDOI decision already given.
- **Draft** - {{draft_crs}} CRs. The impact analysis is done but the item needs a BDOI answer (values, layouts, interfaces or a yes / no on scope) before the CCB can decide.

No row is Approved or Rejected yet: the CCB has not met. BDOI's decisions of 26 September 2026 are recorded as "For CCB review" so that the board records them formally.


# Headline figures

## Totals

<!-- table: widths=9,3.5 caption="Headline figures" bold=first -->
| Figure | Value |
|---|---|
| Change requests in the register | {{total_crs}} |
| Open (Draft or For CCB review) | {{open_crs}} |
| Implemented as built, for acceptance | {{impl_crs}} |
| High priority (all / open) | {{high_crs}} / {{high_open}} |
| CRs with no remaining effort (0 man-days) | {{zero_crs}} |
| CRs of size L (11-25 man-days) / XL (over 25) | {{l_crs}} / {{xl_crs}} |
| Man-days to close all open deviations | {{total_md}} |
| of which Analysis / Build / Test / Documentation | {{analysis_md}} / {{build_md}} / {{test_md}} / {{doc_md}} |

The {{total_md}} man-days are the work still needed to close the deviations as proposed. Not all of it is additional to the BRD scope: part of it (for example the umbrella gaps, the S3 store and the EIAM sign-in) is work the programme needs anyway; the register makes it visible and puts it under change control. Integration dependencies ({{int_md}} man-days) and items parked pending a BDOI answer ({{park_md}} man-days) make up {{int_park_pct}} percent of the total, so most of the effort is released only by BDOI answers and interface specifications. {{top_drop}} carries the most effort ({{top_drop_md}} man-days).

## By drop

<!-- cr:by_drop -->

## By deviation type

<!-- cr:by_type -->

## By CR status

<!-- cr:by_status -->

## By BRD

<!-- cr:by_brd -->


# Top 20 deviations by effort and impact


<!-- cr:top20 -->


# Decisions needed from BDOI

The CRs below cannot be closed without a BDOI decision. The dates are the latest that keep the drop plan: build-shaping items by 16 October 2026 (the date of the programme alignment), the rest before FRS sign-off on 30 November 2026, and the interface specifications by 31 January 2027 for the Drop 2 build.

<!-- cr:decisions -->

# Estimation basis

<!-- cr:roles -->

<!-- cr:assumptions -->

Size classes: **S** 1 to 3 man-days, **M** 4 to 10, **L** 11 to 25, **XL** over 25; **0** means built, acceptance only. The Register shows the class and the chosen number, for example "L (13)".

# Sign-off

By signing, BDOI accepts this register as the list of deviations under change control as of {{status_as_of}}, the change-control process and the CCB membership proposed in chapter 2. It does not approve any single CR: each CR is decided by the CCB. The FRS baseline and the platform build are not changed by this document.

```signoff
rows:
  - {name: "", role: "BIBS Product Owner (chair of the CCB)", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Chief Compliance Officer", organisation: BDOI}
  - {name: "", role: "BDOI IT (integration owner)", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
