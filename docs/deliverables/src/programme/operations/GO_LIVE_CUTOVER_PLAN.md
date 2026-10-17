---
# Go-Live, Cutover and Data Migration Plan of BIBS (programme level, BRD-00).
# Build: python docs/deliverables/src/programme/operations/build_operations_pack.py
# The cutover tasks, roles, freeze windows, checkpoints, rollback and communication tables are expanded from the
# production cutover plan of the BRD-13 Data Migration Handbook (BRD-13 pack/cutover.yaml), and the technical cutover
# tasks from golive_data.yaml, so the go-live plan and the handbook are one plan.
title: Go-Live, Cutover and Data Migration Plan
subtitle: Governance, go / no-go criteria, mock migrations and dress rehearsals, cutover runbook, rollback and communication for the January 2028 go-live of BIBS
doc_type: Go-Live Plan
doc_code: GoLive
brd: BRD-00
name: Go-Live Cutover and Data Migration Plan
doc_id: BIBS-GL-CUT-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Go-Live, Cutover and Data Migration Plan
output: Go_Live/BIBS_GoLive_BRD-00_Cutover_and_Data_Migration_Plan_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Migration Lead; iorta TechNXT Solution Architect
    approver: BDOI Program Manager (pending)
    change: First issue. One go-live plan on the BDOI drop plan and timeline (go-live January 2028, all modules together), based on the production cutover plan of the Data Migration Handbook BRD-13 v2.1 (Part D) with the technical cutover, the readiness gates and the programme governance added
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Owner of the cutover; chair of the go / no-go board"}
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Technical cutover, readiness reviews"}
  - {name: "Data Migration Lead", role: Approver, organisation: BDOI, purpose: "Migration cycles and the cutover tasks"}
  - {name: "Heads of Operations, Comptrollership, Retail and Corporate Marketing; Compliance", role: Approvers, organisation: BDOI, purpose: "Business readiness, freeze, go / no-go"}
  - {name: "Head of the Renewal processing team", role: Reviewer, organisation: BDOI, purpose: "Day-1 renewal queue"}
  - {name: "Cloud and Digital Operations Engineering", role: Reviewer, organisation: BDO Unibank IT, purpose: "Production and DR environments, backups"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Rehearsals, cutover, hypercare"}
---

# Introduction

## Purpose

This plan brings BIBS (BDOI Broker System, on iNXT BrokerVerse) into production in January 2028. It sets the governance and the go / no-go criteria, the sequence from the legacy freeze to production, the mock migrations and dress rehearsals on the way, the cutover runbook hour by hour with owners, the rollback plan, the data migration steps (templates, mapping, cleansing, validation, upload and reconciliation) and the communication plan.

The data migration itself is specified in the Data Migration Handbook of BRD-13 (R1): strategy (Part A), the Migration Console (Part B), reconciliation and sign-off gates (Part C) and the production cutover runbook (Part D). This plan does not repeat it: it places it in the programme, adds the technical cutover and the readiness gates, and reproduces the cutover tasks from the same source so that there is one plan.

## Audience

The go / no-go board and the Program Manager; the BDOI Data Migration Lead, data owners and business heads; BDOI IT and BDO Cloud Operations; the iorta TechNXT project, migration and infrastructure teams.

## Reference documents

<!-- table: widths=1,7.6,8 caption="Reference documents" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| R1 | Data Migration Handbook BRD-13 v2.1 (release set file 02) | Migration strategy, Migration Console, reconciliation, cutover runbook Part D |
| R2 | Migration Workbook BRD-13 v2.1 (release set file 03) | Load templates, object catalogue, code maps, cutover tasks for review |
| R3 | BDOI drop plan and programme timeline (received 26 September 2026) and BDOI answers A1 to A6 | Drops, test windows, single go-live in January 2028 |
| R4 | Programme Alignment - Drops, Integrations and Infrastructure v1.0 | Timeline commitments, environments, integrations per drop |
| R5 | Production Readiness Checklist v1.0 (Excel) | ORR, PRR and go / no-go evidence |
| R6 | Hypercare, Warranty and Sign-off Plan v1.0 | Hypercare after go-live, acceptance certificates |
| R7 | Operations Runbook and Observability v1.0; Disaster Recovery and Business Continuity Plan v1.0 | Monitoring, jobs, backups, DR drill before go-live |
| R8 | Test plans of BRD-1 to BRD-13 (release set files 04 and 05) | SIT and UAT of each drop |

# Go-live approach

## One go-live in January 2028

- **All modules go live together in January 2028** (BDOI answer A1 of 26 September 2026). The drops split the requirements, delivery, test and sign-off workload; the platform goes to UAT complete and end to end. There is no early go-live: the early renewal concept paper is superseded (DCR-240).
- **Recommended date: Monday 3 January 2028**, at the year-end boundary (DMQ39 option A, awaiting the confirmation of the Head of Comptrollership at M6, 1 October 2027). Legacy processes to 31 December 2027 and closes FY2027; BIBS opens with the open items at 31 December and a provisional GL opening; FY2027 adjustments reach BIBS as true-ups until 2 May 2028.
- **Big bang for open items, pre-load for master data.** Reference data and clients are loaded two weeks before go-live and kept current by daily deltas; open items, policy headers, the RA-sent file and the provisional GL opening are loaded once, after the legacy freeze, over the New Year weekend.
- **No parallel run.** Each open item is processed in one system only; the trial migrations, the dress rehearsals, the measured go / no-go criteria, the rollback point and the daily hypercare reconciliation replace a parallel run (R1, Cutover strategy).

## What goes live

<!-- table: widths=3.2,7.4,6 caption="Scope of the go-live by drop" bold=first size=8.5 -->
| Drop | Modules (BRD) | Integrations live at go-live |
|---|---|---|
| Drop 0 - Setup and Data Migration | Product Maintenance (BRD-03), User Access Maintenance (BRD-11), Data Migration (BRD-13); set-up of access, authorization, workflow and data management | EIAM (single sign-on), UIDM-ISC (planned), CCM e-mail, M365, bank and PDC file layouts |
| Drop 1 - Transactional | New Business (BRD-01), Operations (BRD-02: cashiering, remittance, adjustment, commission, production reconciliation), Collections (BRD-04), Accounting, Disbursement and ACSL (BRD-05), Renewal (BRD-06), Customer Servicing Facility (BRD-09), Sanction Screening (BRD-10), Submitted Policies (BRD-12) | CMS / New BOB, OBPCS, Old BOB, PMS, HL-LOAS, LMS, LFS SaaS |
| Drop 2 - Independent | Claims (BRD-07), Employee Benefits without portal (BRD-08), production reconciliation, marketing collection extraction, other reports | EDP, EGL, insurer channels, Bridger Insight XG (manual) |
| Not in the go-live | ReInsurance module (phase 2; Remittance handles the reinsurance transactions in Drop 1) | - |

# Governance

## Bodies

<!-- table: widths=3.4,5.4,4.2,3.6 caption="Go-live governance" bold=first size=8.5 -->
| Body | Members | Decides | Meets |
|---|---|---|---|
| Programme Steering Committee | BDOI executive sponsor (chair), Program Manager, Head of BDOI IT, business heads, iorta TechNXT account executive and Project Manager | Go-live date and year-end option, scope changes, escalated risks | Monthly; weekly from 1 November 2027 |
| Go / no-go board | Program Manager (chair); Heads of Operations, Comptrollership, Marketing and Compliance; Head of BDOI IT; iorta TechNXT Project Manager | GNG-1, GNG-2, GNG-3, the point of no return, rollback, waivers | At each checkpoint; at once on a stop condition |
| Readiness reviews | ORR: BIBS Service Owner (chair), BDO Cloud Operations, Information Security Officer, iorta TechNXT; PRR: Head of BDOI IT (chair), BIBS Product Owner, Program Manager, iorta TechNXT | Operational and production readiness (R5) | ORR 15 Dec 2027; PRR 17 Dec 2027 |
| Cutover command centre | Data Migration Lead, iorta TechNXT Project Manager and Migration Lead, task owners on duty | Execution within the plan; escalation to the board | T-3 17:00 to T+5; daily 17:00 to hypercare exit |
| BIBS Change Advisory Board | BIBS Product Owner (chair), Service Owner, Cloud Operations, Information Security Officer, iorta TechNXT | Production changes; from T-7 only hotfixes of Critical issues approved by the go / no-go board | Weekly; daily in the release freeze |

## RACI

<!-- table: widths=5.4,1.3,1.3,1.3,1.3,1.3,1.3,1.3,1.3,1.3 caption="RACI of the go-live. SC = Steering Committee; GNG = go / no-go board; PM = BDOI Program Manager; DML = Data Migration Lead; BU = business heads and data owners; BIT = BDOI IT; CLO = BDO Cloud Operations; IPM = iorta TechNXT Project Manager; ITM = iorta TechNXT migration and infrastructure team" size=8 -->
| Activity | SC | GNG | PM | DML | BU | BIT | CLO | IPM | ITM |
|---|---|---|---|---|---|---|---|---|---|
| Go-live date and year-end option | A | C | R | C | C | C | I | C | I |
| Readiness reviews (ORR, PRR) | I | I | A | C | C | R | R | R | C |
| Mock migrations and dress rehearsals | I | I | A | R | C | R | C | R | R |
| Production and DR environments | I | I | I | I | - | A | R | C | R |
| Go / no-go decisions | I | A | R | C | C | C | C | C | C |
| Legacy freeze | I | I | A | R | C | R | - | I | I |
| Production load and reconciliation | I | I | I | A | C | C | I | R | R |
| Business validation (G6) | I | I | I | R | A | - | - | I | C |
| Technical cutover | I | I | I | I | - | A | R | R | R |
| Rollback | I | A | R | C | C | R | R | R | R |
| Communication | I | I | A | C | C | C | - | R | - |

# Go / no-go criteria

## Gates

Five gates lead to the go-live. Each gate is recorded with the value of every criterion at the time of the decision. A criterion that is not met blocks GO unless the board records a waiver with its reason; the financial and clearing criteria of GNG-3 are never waived (R1, Part D).

<!-- table: widths=2,3.4,3.2,8 caption="Readiness and go / no-go gates" bold=first size=8.5 -->
| Gate | When | Decides | Criteria |
|---|---|---|---|
| ORR | 15 Dec 2027 | Operations ready | The Operational, Security, Performance and Support criteria of the Production Readiness Checklist (R5): monitoring and alerts live, backups and restore test passed, DR drill passed, penetration test without open Critical or High finding, performance test at 429 concurrent sessions met, support organisation trained |
| PRR | 17 Dec 2027 | Production ready | Every ORR criterion met or waived; UAT acceptance certificates of all drops signed; production configuration baseline approved; compliance criteria met (BIR receipt series, retention, privacy impact assessment) |
| GNG-1 | T-15, Sun 19 Dec 2027 | Start the pre-load | As below |
| GNG-2 | T-5, Wed 29 Dec 2027 | Enter the legacy freeze | As below |
| GNG-3 | T-1, Sun 2 Jan 2028 18:00 | Go live | As below |

## Criteria of the checkpoints

The criteria are those of the production cutover plan (R1, Part D).

<!-- op:cut_checkpoints -->

## Technical criteria added at GNG-3

- Production release identical to the one accepted in UAT and rehearsed in Pre-Prod; no release since T-7 other than an approved hotfix.
- Monitoring green: synthetic sign-in, alert routing to L1, L2, L3 and Cloud Operations tested (TC-07); dashboards live.
- Backups after the load present; DR replica in sync (lag below 1 minute); document replication within 15 minutes.
- Certificates valid for more than 30 days; production secrets in the secrets store only; break-glass accounts sealed.
- Integrations live at go-live answering their connectivity checks through Apigee X; CCM e-mail tested.

# Cutover sequence

## From legacy freeze to production

Figure 1 shows the eight steps of the cutover with the gates of the BRD-13 handbook (G1 decision to G7 go-live) and the go / no-go checkpoints.

![Cutover sequence from legacy freeze to production](figures/gl_sequence.dot){width=14.5}

<!-- table: widths=2.8,3,7.2,3.6 caption="Cutover sequence" bold=first size=8.5 -->
| Step | When | What happens | Evidence and gate |
|---|---|---|---|
| 1 Legacy freeze | T-3 22:00 (Fri 31 Dec 2027) | Last legacy EOD; EBIX, QPS and CMS business modules read-only; batch jobs that post disabled; legacy GL open only for FY2027 adjustments by named Comptrollership users (CT-035, CT-036) | Freeze test refused in each module; GNG-2 passed |
| 2 Extract | T-3 22:15 to T-2 03:00 | Final client delta; final extracts of policy headers, open invoices, unapplied payments, collection state, remittance items, PDCs, receipt series, preliminary December trial balance, RA-sent file, with control files (CT-037, CT-038) | Every file in the intake; SHA-256 recorded |
| 3 Validate | T-2 03:00 to 09:00 | Intake checks against the control files; data-quality rules; code maps; resolution of issues (CT-041, CT-042, CT-044) | G3 per object; 0 financial errors or approved exclusions |
| 4 Migrate | T-2 02:00 to 18:00 | Rollback point snapshot; loads of headers, open items, UPP, collection state, remittance items, PDCs, provisional GL opening, RA-sent file, receipt series (CT-040, CT-043 to CT-050) | G4 per batch; run logs |
| 5 Reconcile | T-2 18:00 to T-1 04:00 | L1 to L4 of every financial object; L5 Migration Clearing per branch and currency; breaks explained and approved (CT-051, CT-052) | G5 per object; Migration Clearing 0.00 |
| 6 Business validation | T-1 06:00 to 13:00 | Full sanction screening of migrated clients; sample checks on screen by the data owners; RA-sent rejects corrected; business smoke test (CT-053 to CT-056) | G6 per Day-1 object; smoke test checklist |
| 7 Go / no-go | T-1 13:00 to 18:00 | Jobs, alerts and integrations checked; go / no-go pack; decision GNG-3 (CT-057 to CT-059) | GNG-3 = G7 recorded with every criterion |
| 8 Production | T (Mon 3 Jan 2028) | Renewal go-live extraction 04:00; logins 06:00; open for business 08:00; point of no return 18:00 (CT-061 to CT-069) | Go-live notice; first transactions in every branch |

# Mock migrations and dress rehearsals

## Schedule

The migration is run five times before it counts: four trial migrations (the mock migrations) in the SIT and UAT migration windows of the BDOI timeline, and the dress rehearsal in the full migration and cutover window, with a reserve slot (R1, Migration cycles). They sit inside the test windows of the drops (Figure 2).

![Mock migrations, dress rehearsals and drop test windows](figures/gl_cycles.dot){width=15.5}

<!-- table: widths=2.8,2.6,3.6,3.8,3.8 caption="Mock migrations and dress rehearsals" bold=first size=8 -->
| Cycle | When and where | Scope | Entry criteria | Exit criteria |
|---|---|---|---|---|
| Mock 1 (Trial migration 1) | 19-30 Apr 2027, SIT | Masked full extracts; reference data and clients first, then policy headers and the RA-sent file | SIT with the Migration Console and Renewal by 9 Apr 2027; layouts frozen; draft code maps; extracts received (9 Apr 2027) | Objects loaded; L1-L4 run; timings per object; profiling report to the owners |
| Mock 2 (Trial migration 2) | 5-16 Jul 2027, SIT | All objects | Mock 1 exit met; code maps approved (18 Jun 2027); legacy accounts and rules set up on SIT | L1-L5 reconciled; Migration Clearing 0.00; no open Critical migration issue |
| Mock 3 (Trial migration 3, UAT load) | 2-13 Aug 2027, UAT | All objects, refreshed extracts | Mock 2 exit met; UAT readiness statement | Business owners verify samples; the end-to-end UAT of Drop 1 (Aug-Dec 2027) runs on migrated data |
| Mock 4 (Trial migration 4, UAT refresh) | 4-15 Oct 2027, UAT | All objects; go-live renewal extraction; test true-up | Mock 3 exit met; cutover date, year-end option and fallback agreed (M6, 1 Oct 2027) | Run as a timed cutover; renewal extraction check balanced; test true-up reconciled |
| Dress rehearsal 1 | 15-26 Nov 2027, Pre-Prod (production-sized) | Full volume after a legacy EOD; the technical cutover tasks TC-03 to TC-17 in their order; the job hold; the rollback (snapshot restore) | Mock 4 exit met; Pre-Prod ready by 1 Oct 2027; cutover plan frozen | Whole cutover within the window with at least 20 % margin; rollback rehearsed within 6 hours; go / no-go criteria measured |
| Dress rehearsal 2 (reserve) | 6-10 Dec 2027, Pre-Prod | The steps whose exit criteria rehearsal 1 missed | Actions of rehearsal 1 done | Missed criteria met; plan updated with measured durations and named people |

## What every rehearsal measures

- the elapsed time of every task against the plan, so the cutover window and its 20 % margin are proved on full volume (planning rate about 50,000 rows an hour per partition);
- the error and rejection rates per object against the thresholds (master data at most 0.5 % rejected or waived; financial objects 0 errors or approved exclusions);
- the reconciliation results L1 to L5 and the time to explain the breaks;
- the go / no-go criteria as measured values, so the board sees at GNG-3 the same report it saw in the rehearsals;
- for the dress rehearsals also the technical cutover, the job hold and release, the monitoring and the rollback.

## Drop test windows

<!-- table: widths=3.6,4.2,8.8 caption="SIT and UAT windows of the drops (BDOI timeline)" bold=first size=8.5 -->
| Stream | Window | Link to the migration |
|---|---|---|
| SIT Drop 0 and Drop 1 | Jan-Jul 2027 | Mock 1 gives migrated clients and reference data to the Renewal tests; Mock 2 completes SIT migration |
| SIT Drop 2 | Jul-Sep 2027 | Uses the Mock 2 data where an object of Drop 2 needs it |
| UAT Drop 0 and Drop 1, end to end | Aug-Dec 2027 | Runs on the Mock 3 load, refreshed by Mock 4 |
| UAT Drop 2 | Oct-Nov 2027 | Runs on the Mock 4 data |
| Performance and penetration test | Nov-Dec 2027 | On Pre-Prod with the dress-rehearsal volume |
| ORR / PRR | Dec 2027-Jan 2028 | Evidence pack by 15 Dec 2027 |

# Data migration steps

The steps below are those of every object in every cycle (R1, Part A, Migration approach). The Migration Console records each step; the Migration Workbook (R2) holds the review copy.

<!-- table: widths=2.8,7.4,3.4,3 caption="Data migration steps and where they are specified" bold=first size=8 -->
| Step | What happens | Who | Specified in R1 |
|---|---|---|---|
| Download templates | The Migration Console exports the load templates from Layouts and Rules: one template per layout with the column names as header row, a workbook per object, one workbook of all templates with the column guide, the control-file template and the filling rules. The Migration Workbook lists every layout on its own sheet in load order (sheet Load templates) | BDOI IT downloads; iorta TechNXT maintains the layouts | Part A, Extract templates |
| Legacy mapping | Legacy codes are mapped to BIBS codes in versioned code map sets (Map, Default, Create or Reject); the data steward prepares and the business owner approves each version (gate G2). Legacy packages are remapped at Renewal sanitation, not at upload (DMQ36) | Data stewards; data owners | Part A, Mapping and code maps |
| Cleansing | BDOI fixes wrong or missing business data in legacy before the next extract; format differences are handled in the extract; duplicate clients are matched by BIBS and doubtful pairs decided by the steward; what cannot be fixed is waived with a plan | Data stewards; BDOI IT; iorta TechNXT | Part A, Cleansing: who fixes what |
| Upload | Files are uploaded on the Extracts screen of the Migration Console, or through the secure file drop that BDOI IT names, never by e-mail; outside production personal data is masked at intake; a file that fails its control totals is rejected as a whole | BDOI IT; iorta Migration Operator | Part A, Secure transfer and staging |
| Validation | Every staged row is checked against the data-quality rules of its layout; clients are matched and deduplicated; batches load only within the thresholds (gate G3) | BIBS; data stewards | Part A, Validation; Client matching |
| Load | Approved batches (gate G4) load through the BIBS services in chunks; a rerun skips what is already loaded; before sign-off a batch can be rolled back | iorta Migration Operator | Part A, Load through the BIBS services |
| Reconciliation | L1 counts, L2 amounts, L3 hash totals, L4 fields, L5 GL (Migration Clearing 0.00 per branch and currency); breaks explained and approved (gate G5); business verification on screen and acceptance (gate G6) | Reconciliation approvers; data owners | Part C |

# Cutover runbook

## Organisation

The command centre is a room at BDOI head office with a standing bridge line and a chat channel, open from T-3 17:00 to T+5. Status calls run every 4 hours from T-3 to T and at each checkpoint, then at 08:00 and 17:00 to T+5, then daily at 17:00 to hypercare exit. Escalation runs from the task owner to the Data Migration Lead (15 minutes), to the iorta TechNXT Project Manager and the Program Manager (30 minutes), to the go / no-go board (1 hour, or at once for a stop condition) (R1, Part D).

**Stop conditions** (the board meets at once): a rejected final extract that cannot be re-sent within 2 hours; a load failure that leaves a financial object incomplete; Migration Clearing not 0.00 after the reconciliation window; a legacy posting after the freeze; loss of the production environment or of the snapshot.

<!-- op:cut_roles -->

The technical tasks of this plan add one role: **CLO** - BDO Cloud Operations (production and DR environments, secrets, backups).

## Phases

<!-- table: widths=1.2,6,9.4 caption="Cutover phases (T = Monday 3 January 2028)" bold=first size=8.5 -->
| Phase | Name | Days |
|---|---|---|
| A | Readiness | T-30 to T-15 (4 to 19 Dec 2027) |
| B | Pre-load and daily deltas | T-14 to T-4 (20 to 30 Dec 2027) |
| C | Legacy freeze and final extracts | T-3 (Friday 31 Dec 2027) |
| D | Production load | T-2 (Saturday 1 Jan 2028) |
| E | Reconciliation, verification and go / no-go | T-1 (Sunday 2 Jan 2028) |
| F | Go-live | T (Monday 3 Jan 2028) |
| G | Hypercare and true-up 1 | T+1 to T+30 (4 Jan to 2 Feb 2028) |
| H | Final true-up and legacy GL close | T+31 to T+120 (Feb to 2 May 2028) |

The full task list of phases A to H (CT-001 to CT-095) is in the handbook (R1, Part D) and on the sheet Cut-over tasks of the Migration Workbook (R2). The weekend tasks are reproduced below as the hour-by-hour template; the columns Actual start / end and Done by are filled in during the rehearsals and the cutover (the Migration Console holds the record).

<!-- landscape -->

## Hour-by-hour template: freeze to go-live

<!-- op:cut_hours phases=C,D,E,F template=yes caption="Cutover weekend, hour by hour (T-3 to T)" -->

## Technical cutover tasks

<!-- op:tech_cutover template=yes -->

## Freeze windows

<!-- op:cut_freeze -->

<!-- portrait -->

# Rollback plan

## Rollback point and decision

The rollback returns BDOI to legacy with no loss of data. The rollback point is the full backup (snapshot) of production taken at T-2 02:00 (CT-040), before any open item, header or trial balance is loaded; reference data and clients pre-loaded before it stay loaded and are harmless because legacy stays the system of record until go-live. The rollback is available until the point of no return, the end-of-day review on T at 18:00 (DMQ32). It is decided by the go / no-go board at GNG-3 (NO-GO), or before T 18:00 when a failure is judged unrecoverable. The rollback was rehearsed in dress rehearsal 1 and must complete within 6 hours (CT-060), so that legacy opens for business on Monday at 08:00.

## Steps

<!-- op:cut_rollback -->

## After the point of no return

There is no technical rollback after T 18:00. An issue is corrected forward in BIBS through the normal business functions (correction, reversal, adjustment), with the change visible in the Prod Recon legacy change report where it touches a legacy invoice. A true-up that is wrong is corrected by the next true-up, never by editing a posted journal. A platform outage after go-live follows the DR and BCP plan (R7).

# Communication plan

<!-- op:cut_comms -->

Programme-level messages added by this plan:

<!-- table: widths=0.8,6.4,4,2.6,2.8 caption="Programme messages" size=8 -->
| # | Message | Audience | When | Owner |
|---|---|---|---|---|
| P1 | Go-live date confirmed and the year-end option | Steering Committee, business heads, BDOI IT | 1 Oct 2027 (M6) | Program Manager |
| P2 | Result of the ORR and PRR with open waivers | Steering Committee | 17 Dec 2027 | Head, BDOI IT |
| P3 | Each go / no-go decision with the criteria values | Steering Committee; command centre | GNG-1, GNG-2, GNG-3 | Program Manager |
| P4 | Support channels after go-live: service desk number, ITSM portal, floor support per department | All users | T-1 19:00 with message 8 | BIBS Service Owner |

# Risks of the go-live

<!-- table: widths=0.8,6.2,1.6,8 caption="Main go-live risks" size=8 -->
| # | Risk | Impact | Mitigation |
|---|---|---|---|
| 1 | Cutover over the year-end holidays (24 December 2027 to 1 January 2028) | High | Year-end option confirmed at M6; tasks moved off holidays in the calendar; roster confirmed at T-29 |
| 2 | Migration Clearing not 0.00 at GNG-3 | High | Reconciled in every mock from Mock 2; trial balance extracted after the same EOD as the detail; break explanations agreed before GNG-3 |
| 3 | Load time beyond the window | Medium | Pre-load of reference data and clients; dress rehearsal at full volume with 20 % margin; reserve rehearsal |
| 4 | Late UAT acceptance of a drop | High | UAT exit criteria tracked weekly from September 2027; open issues triaged; Drop 2 accepted by 30 Nov 2027, two weeks before the PRR |
| 5 | Performance or penetration findings in December | High | Early security scan in SIT (June 2027); performance test on Pre-Prod from 1 Nov 2027; findings closed by 10 Dec 2027 |
| 6 | DR region not approved in time for the pre-go-live drill | Medium | Decision needed by 31 Mar 2027 (DR plan OP-01); otherwise the drill is a restore into a second environment in the primary region and the regional drill moves after go-live |
| 7 | Scheduled jobs acting on migrated data during the load | Medium | Job hold from T-3 21:30 to T 03:30 (TC-12, TC-15); one-off runs decided at GNG-3 |
| 8 | Tight lead time for the January 2028 renewals | High | Day-1 priority queue; go-live extraction at T 04:00; staffing plan signed at T-20 |

# Decisions and open points for BDOI {-}

<!-- table: widths=1.5,8.6,4,2.5 caption="Decisions and open points" bold=first size=8.5 -->
| Ref | Point and our proposal | Owner | Needed by |
|---|---|---|---|
| OP-01 | Confirm the go-live date Monday 3 January 2028 and the year-end option A (DMQ25, DMQ39, DCR-242) | Head, Comptrollership; Steering Committee | 1 Oct 2027 (M6) |
| OP-02 | Confirm the governance bodies and their members, and name the BDOI executive sponsor who chairs the Steering Committee | Program Manager | 30 Nov 2026 |
| OP-03 | Confirm the ORR on 15 Dec 2027 and the PRR on 17 Dec 2027 as gates before GNG-1 | Head, BDOI IT | 30 Jun 2027 |
| OP-04 | Confirm the job hold over the load window (bibs-jobs scaled to zero from T-3 21:30 to T 03:30) and the deferral of the 1 January runs (KYC_REVIEW_DUE, MIG_RUNOFF_SNAPSHOT) to 3 January | Data Migration Lead with the Head of BDOI IT | 15 Oct 2027 (after Mock 4) |
| OP-05 | Confirm that the January 2028 BOOK rates are set from the legacy December revaluation by Comptrollership (BIBS has no December close to copy them from) | Head, Comptrollership | 15 Nov 2027 |
| OP-06 | Confirm the dates of dress rehearsal 1 (15-26 Nov 2027) and of the reserve (6-10 Dec 2027) with the performance test window | Program Manager | 30 Jun 2027 |
| OP-07 | The open decisions of the migration itself (DMQ01 to DMQ35, CLR-DM-01 to CLR-DM-20) stay in the handbook with their dates | Data Migration Lead | As in R1 |

# Glossary {-}

```glossary
DML: BDOI Data Migration Lead
G1 to G7: Sign-off gates of each migrated object (decision, mapping, validation, load approval, reconciliation, acceptance, go-live)
GNG-1, GNG-2, GNG-3: Go / no-go checkpoints of the cutover
Mock migration: A trial migration run on masked full extracts, timed as the real one
ORR: Operational readiness review
PRR: Production readiness review
RA: Renewal advice
T: Go-live day (recommended Monday 3 January 2028)
UPP: Unapplied premium payment
```

# Sign-off {-}

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: Data Migration Lead, organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
