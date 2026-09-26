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
version: "1.2"
date: 26 September 2026
status: Issued for BDOI review
header_title: Cutover Runbook BRD-13
output: Migration/BIBS_Migration_BRD-13_Cutover_Runbook_v1.2.docx
h1_page_break: false
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Program Manager (pending)
    change: First issue; dates are relative to T until BDOI confirms the go-live date (DMQ25); re-issued after the dress rehearsal with named people and measured timings
  - version: "1.1"
    date: 26 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Program Manager (pending)
    change: "Calendar of the BDOI timeline (go-live January 2028, proposed Monday 3 January 2028) with the relative days mapped to dates; tasks moved off the year-end holidays (GNG-2 and the last business day to T-5, completion drives to T-12, client load to T-11); carried RMEL cohorts of January-May 2028 with the completeness check and the catch-up extraction; default names of the decommissioning criteria. Named people and measured durations follow in version 1.2 after the dress rehearsal"
  - version: "1.2"
    date: 26 Sep 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect
    approver: BDOI Program Manager (pending)
    change: "BDOI answers of 26-Sep-2026. Year-end cut-over option A (DMQ39, recommended, awaiting Comptrollership confirmation) - December soft close, provisional GL opening, legacy GL restricted to FY2027 adjustments, true-up 1 (T+15 to T+18) and the final true-up with the legacy GL lock and the closure (T+60 to T+120, new phase H). January-May 2028 expiries processed in BIBS after go-live (DMQ37) - Renewal team readiness, RA-sent file with maker-checker review of rejects (DMQ38), go-live extraction at T 04:00 and the day-1 priority queue; the carried cohorts are removed. Tasks renumbered CT-001 to CT-095. Named people and measured durations follow in version 1.3 after the dress rehearsal"
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Owner of the cutover; chair of the go / no-go board"}
  - {name: "Go / no-go board members", role: Approver, organisation: BDOI, purpose: "Checkpoints GNG-1, GNG-2, GNG-3"}
  - {name: "BDOI IT (legacy EBIX, QPS, ISYS, CMS)", role: Task owner, organisation: BDOI, purpose: "Freeze, extracts, read-only, rollback"}
  - {name: "Comptrollership; Operations; Marketing; Compliance", role: Task owner, organisation: BDOI, purpose: "Accounts, reconciliation, verification, communication"}
  - {name: Project team, role: Task owner, organisation: iorta TechNXT, purpose: "Loads, reconciliation, environment, hypercare"}
---

# Purpose and use

This runbook is the plan of the production cutover of BIBS: from 30 days before go-live (T) to the end of hypercare. It lists every task with its owner, start, duration, predecessors and the evidence that proves it is done; the go / no-go checkpoints with their criteria; the freeze windows; the rollback procedure; the communication plan; the hypercare roster and exit criteria; and the decommissioning checklists of the legacy systems.

- **T** is the go-live date. The BDOI timeline sets it in January 2028 (DMQ25). The recommendation, awaiting Comptrollership confirmation, is **Monday 3 January 2028** at the year-end boundary (DMQ39 option A, register DCR-242): legacy processes to 31 December 2027 and closes FY2027, and BIBS opens with the open items at 31 December and a provisional GL opening. Days are calendar days; times are Philippine time (PHT). Section 3.1 maps the relative days to dates; tasks that would fall on the year-end holidays are placed on the nearest working day.
- The same tasks are in the Cutover Task Plan workbook (`BIBS_Migration_BRD-13_Cutover_Task_Plan_v1.2.xlsx`), where names, actual times and status are recorded, and in the Migration Console as the plan of kind PRODUCTION (FR-DM-120). The console is the record during the cutover; the workbook is the planning and review copy.
- The plan is rehearsed in four mocks (April, July, August and October 2027) and a dress rehearsal (November 2027) (Strategy chapter 10). This version gives planned durations; version 1.3, issued after the dress rehearsal, gives the measured durations and the named people.
- The early renewal release of the concept paper of 6 September 2026 is superseded by the single January 2028 go-live (register DCR-240). The renewals of the January-May 2028 expiries are processed in BIBS after go-live (DMQ37): no renewal is carried from legacy. The Renewal processing team is made ready for the January expiries (CT-013), compiles the renewal advices already sent from its Excel trackers (CT-033) and reviews the rejected rows with a maker and a checker (CT-055); BIBS extracts every expiry to 31 May 2028 at T 04:00 (CT-062), and the team works the day-1 priority queue, January expiries first (CT-066).
- The FY2027 closing and audit adjustments that Comptrollership posts in the legacy GL after the freeze reach BIBS as true-ups: true-up 1 after the legacy year-end close (CT-080 to CT-084) and the final true-up after the audited financial statements, followed by the legacy GL lock and the closure (phase H, CT-087 to CT-095).

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

## Calendar

<!-- dm:calendar -->

![The cutover weekend (T = Monday 3 January 2028, recommended; times PHT)](figures/dm_cutover_weekend.dot){width=16}

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

## Phase H - Final true-up and legacy GL close

<!-- dm:tasks phase=H -->

<!-- portrait -->

# Rollback

The rollback returns BDOI to legacy with no loss of data. It is available until the point of no return, the end-of-day review on T at 18:00 (DMQ32). The rollback point is the production database snapshot taken at T-2 02:00 (task CT-040), before any open item, header or trial balance is loaded; reference data and clients pre-loaded before it stay loaded and are harmless because legacy stays the system of record until go-live.

<!-- dm:rollback -->

After the point of no return, there is no technical rollback. A defect is fixed forward in BIBS through the normal business functions (correction, reversal, adjustment), with the change visible in the Prod Recon legacy change report where it touches a legacy invoice. The true-ups come after the point of no return; a true-up that is wrong is corrected by the next true-up, never by editing a posted journal.

# Year-end cut-over and FY2027 true-ups

The cut-over sits on the year-end boundary (DMQ39, option A; recommended, awaiting Comptrollership confirmation at M6). It changes three things in the plan:

- **Before the freeze.** Comptrollership completes a December soft close by 20 December 2027 (CT-011), so the preliminary December trial balance is close to final, and names the users who may post FY2027 adjustments in the legacy GL after the freeze (CT-034).
- **At the freeze.** The legacy business modules become read-only, and the legacy GL stays open only for FY2027 closing and audit adjustments by those users, in FY2027 periods (CT-036). The preliminary December trial balance is signed as the provisional opening (CT-039) and loaded as balance-sheet opening journals dated 1 January 2028, with the FY2027 result in retained earnings (CT-048). FY2028 P&L starts at zero in BIBS.
- **After go-live.** Each FY2027 adjustment reaches BIBS as an opening-balance adjustment journal (type OPENING, value date 1 January 2028) in a true-up, prepared by the Comptrollership GL lead and approved by the Head of Comptrollership: true-up 1 after the legacy year-end close (CT-080 to CT-084, before the January close) and the final true-up after the audited financial statements (CT-088 to CT-093). Each true-up is reconciled to the legacy trial balance with the cut-off checks of the Reconciliation Approach before it is signed. The legacy GL is then locked (CT-094) and the true-ups are closed (CT-095). The FY2027 BIR annual returns and the FY2027 audit use legacy (CT-092); FY2028 uses BIBS.

Options B (go-live after the first-quarter close in April 2028, with a year-to-date P&L migration) and C (two books in parallel for the first quarter of 2028) were considered and are not recommended (Strategy section 11.1). If BDOI wants more assurance in January, Comptrollership compares the key BIBS reports with the opening position instead of keeping two books.

# Communication plan

<!-- dm:communication -->

# Hypercare

Hypercare runs from go-live to the first month-end close with the legacy control accounts (about T+30). The command centre stays open to T+5, then the daily 17:00 call continues until exit.

**Daily checks** (task CT-071): Migration Clearing 0.00 per branch and currency; legacy control accounts against their sub-ledgers (ACSL, context LEGACY); automatch runs and the unapplied items they left; URGENT January renewals without an RA sent or an insurer request; payment file results; exception queues (rejected receipts, remittance exclusions, failed postings); failed jobs and alerts; open defects by severity; user tickets by department.

## Roster

<!-- dm:roster -->

## Exit criteria

<!-- dm:hypercare-exit -->

# Decommissioning checklists

A legacy system is decommissioned only when every item of its checklist is met and signed (FR-DM-123; DMQ27). The legacy context in BIBS closes separately, when the legacy positions have run off. The words before the dash are the default criterion names that the Migration Console and its messages use.

<!-- dm:decommissioning -->

# Sign-off {-}

By signing, BDOI approves this runbook as the plan of the production cutover. The confirmed go-live date and the Comptrollership confirmation of the year-end option A (M6), the named people and the measured durations are added in version 1.3 after the dress rehearsal.

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services (chair, go / no-go board)", organisation: BDO Unibank ESG}
  - {name: "", role: "Data Migration Lead", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
