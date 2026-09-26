---
# Cutover Runbook of the BRD-13 Data Migration. The task tables are built from dm_cutover.yaml, which also
# produces the Cutover Task Plan workbook. Build: python docs/deliverables/src/migration/build_migration_pack.py
title: Cutover Runbook
subtitle: BRD-13 Data Migration - production cutover from T-30 to hypercare exit
doc_type: Runbook
doc_code: Migration
brd: BRD-13
name: Cutover Runbook
doc_id: BIBS-DMC-BRD-13
version: "1.0"
date: 26 September 2026
status: Issued for BDOI review
header_title: Cutover Runbook BRD-13
output: Migration/BIBS_Migration_BRD-13_Cutover_Runbook_v1.0.docx
h1_page_break: false
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Program Manager (pending)
    change: First issue; dates are relative to T until BDOI confirms the go-live date (DMQ25); re-issued after the dress rehearsal with named people and measured timings
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Owner of the cutover; chair of the go / no-go board"}
  - {name: "Go / no-go board members", role: Approver, organisation: BDOI, purpose: "Checkpoints GNG-1, GNG-2, GNG-3"}
  - {name: "BDOI IT (legacy EBIX, QPS, ISYS, CMS)", role: Task owner, organisation: BDOI, purpose: "Freeze, extracts, read-only, rollback"}
  - {name: "Comptrollership; Operations; Marketing; Compliance", role: Task owner, organisation: BDOI, purpose: "Accounts, reconciliation, verification, communication"}
  - {name: Project team, role: Task owner, organisation: iorta TechNXT, purpose: "Loads, reconciliation, environment, hypercare"}
---

# Purpose and use

This runbook is the plan of the production cutover of BIBS: from 30 days before go-live (T) to the end of hypercare. It lists every task with its owner, start, duration, predecessors and the evidence that proves it is done; the go / no-go checkpoints with their criteria; the freeze windows; the rollback procedure; the communication plan; the hypercare roster and exit criteria; and the decommissioning checklists of the legacy systems.

- **T** is the go-live date: the first business day (a Monday) of a month, after a legacy month-end close (proposal, DMQ25, register DCR-200). Days are calendar days; times are Philippine time (PHT).
- The same tasks are in the Cutover Task Plan workbook (`BIBS_Migration_BRD-13_Cutover_Task_Plan_v1.0.xlsx`), where names, actual times and status are recorded, and in the Migration Console as the plan of kind PRODUCTION (FR-DM-120). The console is the record during the cutover; the workbook is the planning and review copy.
- The plan was rehearsed in three mocks and a dress rehearsal (Strategy chapter 10). This version gives planned durations; version 1.1, issued after the dress rehearsal, gives the measured durations and the named people.

# Cutover organisation

## Command centre

- **Location and channels.** A command centre room at BDOI head office, a standing bridge line and a chat channel for the cutover team, open from T-3 17:00 to T+5.
- **Status calls.** T-3 to T: every 4 hours and at each checkpoint. T+1 to T+5: 08:00 and 17:00. Afterwards daily at 17:00 until hypercare exit.
- **Decisions.** Task owners report completion with the evidence in the console. The Data Migration Lead decides within the plan; anything that changes the plan, the scope or a checkpoint goes to the go / no-go board.
- **Escalation.** Task owner to the Data Migration Lead (15 minutes), to the iorta Project Manager and the Program Manager (30 minutes), to the go / no-go board (1 hour, or immediately for a stop condition).

**Stop conditions** (the board meets at once): a rejected final extract that cannot be re-sent within 2 hours; a load failure that leaves a financial object incomplete; Migration Clearing not 0.00 after the reconciliation window; a legacy posting after the freeze; loss of the production environment or of the snapshot.

## Roles

<!-- dm:roles -->

# Timeline

<!-- dm:phases -->

![The cutover weekend (T = Monday go-live; times PHT)](figures/dm_cutover_weekend.dot){width=16}

## Freeze windows

<!-- dm:freeze -->

# Go / no-go checkpoints

Each checkpoint is recorded in the Migration Console with the value of every criterion at the time of the decision (FR-DM-121). A criterion that is not met blocks GO unless the board records a waiver with its reason (only for criteria marked as waivable in the console; the financial and clearing criteria of GNG-3 are never waived).

<!-- dm:checkpoints -->

<!-- landscape -->

# Task list

Columns: **When** is the day relative to T and the planned start time; **Owner** is the accountable role (codes in section 2.2); **Hours** is the planned elapsed time; **After** lists the tasks that must be complete first; **Verification** is the evidence recorded in the console.

## Phase A - Readiness

<!-- dm:tasks phase=A -->

## Phase B - Pre-load and daily deltas

<!-- dm:tasks phase=B -->

## Phase C - Legacy freeze and final extracts

<!-- dm:tasks phase=C -->

## Phase D - Production load

<!-- dm:tasks phase=D -->

## Phase E - Reconciliation, verification and go / no-go

<!-- dm:tasks phase=E -->

## Phase F - Go-live

<!-- dm:tasks phase=F -->

## Phase G - Hypercare

<!-- dm:tasks phase=G -->

<!-- portrait -->

# Rollback

The rollback returns BDOI to legacy with no loss of data. It is available until the point of no return, the end-of-day review on T at 18:00 (DMQ32). The rollback point is the production database snapshot taken at T-2 02:00 (task CT-037), before any open item, header or trial balance is loaded; reference data and clients pre-loaded before it stay loaded and are harmless because legacy stays the system of record until go-live.

<!-- dm:rollback -->

After the point of no return, there is no technical rollback. A defect is fixed forward in BIBS through the normal business functions (correction, reversal, adjustment), with the change visible in the Prod Recon legacy change report where it touches a legacy invoice.

# Communication plan

<!-- dm:communication -->

# Hypercare

Hypercare runs from go-live to the first month-end close with the legacy control accounts (about T+30). The command centre stays open to T+5, then the daily 17:00 call continues until exit.

**Daily checks** (task CT-065): Migration Clearing 0.00 per branch and currency; legacy control accounts against their sub-ledgers (ACSL, context LEGACY); automatch runs and the unapplied items they left; payment file results; exception queues (rejected receipts, remittance exclusions, failed postings); failed jobs and alerts; open defects by severity; user tickets by department.

## Roster

<!-- dm:roster -->

## Exit criteria

<!-- dm:hypercare-exit -->

# Decommissioning checklists

A legacy system is decommissioned only when every item of its checklist is met and signed (FR-DM-123; DMQ27). The legacy context in BIBS closes separately, when the legacy positions have run off.

<!-- dm:decommissioning -->

# Sign-off {-}

By signing, BDOI approves this runbook as the plan of the production cutover. The go-live date, the named people and the measured durations are added in version 1.1 after the dress rehearsal.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services (chair, go / no-go board)", organisation: BDO Unibank ESG}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
