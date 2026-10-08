---
# Hypercare, Warranty and Sign-off Plan of BIBS (programme level, BRD-00).
# Build: python docs/deliverables/src/programme/operations/build_operations_pack.py
# The hypercare roster and the exit criteria of the cutover plan are expanded from the BRD-13 pack/cutover.yaml.
title: Hypercare, Warranty and Sign-off Plan
subtitle: Hypercare after the January 2028 go-live, exit criteria, issue targets, handover to steady-state support, warranty and the acceptance certificates
doc_type: Hypercare Plan
doc_code: Hypercare
brd: BRD-00
name: Hypercare Warranty and Sign-off Plan
doc_id: BIBS-GL-HYP-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Hypercare, Warranty and Sign-off Plan
output: Go_Live/BIBS_Hypercare_BRD-00_Hypercare_Warranty_and_Sign-off_Plan_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Service Delivery Lead
    approver: BDOI Program Manager (pending)
    change: First issue. Hypercare from go-live to the January 2028 month-end close, daily stand-up and triage, issue targets, exit criteria, handover to the support model, a 12-month warranty and the certificates C1 UAT acceptance, C2 go-live acceptance and C3 hypercare exit
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Hypercare, certificates C2 and C3"}
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Handover to steady-state support, warranty"}
  - {name: "BIBS Product Owner; owners of BRD-1 to BRD-13", role: Approvers, organisation: BDOI, purpose: "UAT acceptance per drop (C1)"}
  - {name: "Heads of Operations, Comptrollership, Marketing; Head of the Renewal processing team", role: Reviewers, organisation: BDOI, purpose: "Business checks of hypercare"}
  - {name: "BIBS Service Owner", role: Reviewer, organisation: BDOI, purpose: "Handover and support"}
  - {name: Project and support team, role: Delivery, organisation: iorta TechNXT, purpose: "Hypercare, fixes, warranty"}
---

# Introduction

## Purpose

This plan covers the time from UAT acceptance to steady-state support: how BIBS is accepted for go-live, how the project team supports it closely after the go-live of January 2028 (hypercare), when hypercare ends, how support passes to the support model, what the warranty covers, and the certificates that BDOI signs at each step.

## Audience

The go / no-go board and the Program Manager; the business owners of the BRDs, who sign the UAT acceptance; the BIBS Service Owner, BDOI application support and the BDO service desk, who take over support; the iorta TechNXT project and support teams.

## Reference documents

<!-- table: widths=1,7.4,8.2 caption="Reference documents" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| R1 | Go-Live, Cutover and Data Migration Plan v1.0 | Go-live sequence, GNG-3, point of no return |
| R2 | Data Migration Handbook BRD-13 v2.1, Part D | Hypercare roster, daily checks, exit criteria, true-ups |
| R3 | Support Model and Support Guide v1.0 | Severities, steady-state service levels, L1 guide, handover target |
| R4 | Operations Runbook and Observability v1.0 | Daily operations check, alerts, jobs |
| R5 | Production Readiness Checklist v1.0 | Criteria carried into hypercare |
| R6 | Test plans of BRD-1 to BRD-13 (release set files 04 and 05) and the UAT plans | UAT scope and results per drop |
| R7 | Change Management Register v1.0 | Changes after acceptance |

# From UAT to steady state

Figure 1 shows the path from UAT acceptance to steady-state support and the three certificates.

![From UAT acceptance to steady-state support](figures/hc_phases.dot){width=14}

<!-- table: widths=3.4,4.2,9 caption="Milestones" bold=first size=8.5 -->
| Milestone | Date | Evidence |
|---|---|---|
| UAT acceptance Drop 2 | By 30 Nov 2027 | Certificate C1 (Drop 2) |
| UAT acceptance Drop 0 and Drop 1 (end to end, on migrated data) | By 15 Dec 2027 | Certificate C1 (Drop 0 and Drop 1) |
| Go / no-go GNG-3 | Sun 2 Jan 2028, 18:00 | Decision recorded with every criterion (R1) |
| Go-live | Mon 3 Jan 2028, 08:00 | Go-live notice |
| Point of no return | Mon 3 Jan 2028, 18:00 | Board decision (CT-069) |
| Go-live acceptance | Week-1 review, Mon 10 Jan 2028 (T+7) | Certificate C2 |
| Hypercare mid-point review | Mon 17 Jan 2028 (T+14) | Criteria status (CT-079) |
| Hypercare exit | Wed 2 Feb 2028 (T+30), after the January month-end close | Certificate C3 |
| End of warranty | Tue 2 Jan 2029 | Warranty closure note; annual maintenance from 3 Jan 2029 |

# Hypercare

## Duration

Hypercare runs from go-live, Monday 3 January 2028, to the first month-end close with the legacy control accounts, planned for Wednesday 2 February 2028 (T+30), in two stages:

<!-- table: widths=3.4,3.6,9.6 caption="Stages of hypercare" bold=first size=8.5 -->
| Stage | Days | Set-up |
|---|---|---|
| Intensive | T to T+5 (3 to 8 Jan 2028) | Command centre open 07:00-20:00; infrastructure on call 24 x 7; floor support in every department and the main branches; status calls 08:00 and 17:00 |
| Stabilise | T+6 to T+30 (9 Jan to 2 Feb 2028) | Daily stand-up and twice-daily triage; daily 17:00 call; support levels of the support model working beside the project team |

**Extension.** If the exit criteria are not met at T+30, hypercare continues to the February 2028 month-end close (exit decision on Friday 3 March 2028, T+60) at no extra charge to BDOI. A second extension is decided by the Steering Committee with a recovery plan.

## Organisation and roster

The roster of the cutover plan applies (R2). The names and telephone numbers are fixed at T-29 (CT-003).

<!-- op:hc_roster -->

Ticket routing during hypercare: users call the BDOI service desk (L1) or the floor support of their department; L1 logs every call; L2 and the iorta TechNXT application team triage together in the command centre; fixes follow section 3.5.

## Daily rhythm

<!-- table: widths=2.2,4.2,7.6,2.6 caption="Daily rhythm of hypercare (business days)" bold=first size=8.5 -->
| Time | Meeting or task | Content | Lead |
|---|---|---|---|
| 07:00 | Daily operations check | Night batch, alerts, backups, replication, certificates (R4, section 7.2) | iorta TechNXT infrastructure team |
| 07:30 | Daily hypercare check (CT-071) | Migration Clearing 0.00 per branch and currency; legacy control accounts against their sub-ledgers; automatch results; exception queues; URGENT January renewals without an RA or insurer request; failed jobs | iorta TechNXT Migration Lead |
| 08:30 | Daily stand-up (15 minutes) | Overnight results; open P1 and P2; today's risks (deadlines, month-end, insurer due dates); help needed from the business | iorta TechNXT Project Manager |
| 10:00 and 16:00 | Triage (CT-072) | New issues: severity, category, owner, target; fixes ready for release; workarounds to publish to L1 | iorta TechNXT QA lead with Operations, Comptrollership, Renewal and L2 |
| 12:00 (T to T+5) | Midday check | Queues, Migration Clearing, legacy posting check | iorta TechNXT Migration Lead |
| 17:00 | Status call and daily hypercare report | Issues opened and closed by severity and department; checks; decisions needed | iorta TechNXT Project Manager |
| Weekly | Board review (T+7, T+14, T+21, T+30) | Issues, volumes, performance, renewal queue, user feedback, exit criteria | Program Manager |

## Issue management

Every call and finding is an issue in the hypercare log in the ITSM tool, with: severity (P1 to P4 as in the support model), category, module, department and branch, the user, the record number, the evidence and the owner. Categories:

<!-- table: widths=3.6,8,5 caption="Issue categories in hypercare" bold=first size=8.5 -->
| Category | Meaning | Handled by |
|---|---|---|
| Application fault | BIBS does not behave as the signed FRS and the accepted test cases say | iorta TechNXT (fix and release) |
| Migrated data | A migrated record is wrong or missing | Data Migration Lead and data owner decide; correction through the BIBS functions or a data correction |
| Configuration | A parameter, list, rule, workflow or role is set wrongly | BDOI business administrator or L2 (standard change) |
| Access | A user lacks a role or a data scope | User access request |
| How-to and training | The user needs guidance | Floor support and L1; knowledge article |
| Change request | A new or different requirement | Change Management Register; not a hypercare fix |
| Interface or platform | A BDO system, AWS or the network | BDOI IT or BDO Cloud Operations |

## Issue resolution targets in hypercare

The targets are shorter than in steady state because the project team is on hand.

<!-- table: widths=1.6,3,3,3.6,5.4 caption="Issue targets in hypercare" bold=first size=8.5 -->
| Severity | Response | Workaround | Fix in production | Updates |
|---|---|---|---|---|
| P1 | 15 minutes (24 x 7 in the intensive stage) | 2 hours | Hotfix within 24 hours, approved by the go / no-go board | Every hour on the bridge |
| P2 | 30 minutes in service hours | 4 service hours | 3 business days, in a hotfix or the next hypercare release | At each triage |
| P3 | 4 service hours | 2 business days | 10 business days, in a hypercare release | Daily report |
| P4 | 1 business day | - | Planned with the Product Owner after hypercare | Weekly |

## Releases in hypercare

- From T-7 to T+14 production releases are frozen; only a hotfix of a Critical (P1) issue approved by the go / no-go board is deployed (R2, freeze windows).
- The first planned hypercare release is in the window of Sunday 23 January 2028, 00:00-04:00 (T+20), with the P2 and P3 fixes accepted by the key users in UAT.
- No release from 27 January to 3 February 2028 (January close), except a hotfix approved by the board.
- Every fix goes through UAT with the key user who raised the issue, then Pre-Prod, then production (Support Model, releases).

## Hypercare reporting

The daily hypercare report (17:00) and the weekly board review show: tickets opened, closed and open by severity, category and department; P1 and P2 with status; the daily checks; the night batch; availability and response times against the SLOs; the URGENT January renewals with an RA sent or an insurer request; true-up 1 status (due T+18, 21 Jan 2028); and the status of each exit criterion.

# Exit criteria

Hypercare ends when every criterion below is met, or waived by the go / no-go board with a dated plan. The cutover plan sets the business and migration criteria; this plan adds the support handover criteria.

<!-- op:hc_exit -->

<!-- table: widths=1.4,8.2,7 caption="Support handover criteria" bold=first size=8.5 -->
| Ref | Criterion | Evidence |
|---|---|---|
| HX-09 | No open P1 or P2 issue; every P3 has an agreed fix date and a workaround known to L1 | Hypercare log |
| HX-10 | New tickets below 40 a business day for 5 consecutive business days, with no rising trend (about 3 per 100 named users) | Ticket report |
| HX-11 | SLOs met for 10 consecutive business days: availability 99.9 %, 95 % of screen requests within 2 seconds, night batch class A finished before 06:00 | SLO dashboard |
| HX-12 | Known errors and workarounds of hypercare published in the knowledge base; L1 guide updated | Knowledge base index |
| HX-13 | L1 analysts and L2 analysts handled tickets alone for the last 5 business days (project team in support only) | Ticket assignment report |
| HX-14 | Monitoring and alert routing working; first monthly restore spot check passed | Alert test; restore record |
| HX-15 | Configuration baseline of production recorded; environment documentation current (versions, release, addresses, backups, secrets inventory without values) | Baseline export; environment sheet |
| HX-16 | Migration roles removed; non-production staging data of the migration purged | User Access report; purge log |
| HX-17 | Contacts and escalation matrix of the support model confirmed by a call test | Call test record |
| HX-18 | Open P3 and P4 issues and approved change requests moved to the support backlog and the Change Management Register with their history | Transfer list |

# Handover to steady-state support

## Steps

<!-- table: widths=1.4,3,8.6,3.6 caption="Handover steps" bold=first size=8.5 -->
| Step | When | Action | Owner |
|---|---|---|---|
| H1 | T+14 | Mid-point review against the exit criteria; gaps planned | Program Manager |
| H2 | T+15 to T+25 | L1 and L2 take every ticket first; the project team supports from the second line only | BIBS Service Owner |
| H3 | T+20 | Knowledge transfer sessions on the fixes and known errors of hypercare; runbook updates | iorta TechNXT Support Manager |
| H4 | T+28 | Transfer list of open issues and change requests prepared | iorta TechNXT Project Manager |
| H5 | T+30 | Handover meeting: exit criteria evidence, transfer list, contacts; certificate C3 submitted | Program Manager; BIBS Service Owner |
| H6 | T+30 | Command centre closed; daily 17:00 call ends; monthly service review starts in March 2028 | Program Manager |

## What changes at handover

<!-- table: widths=4,6.2,6.4 caption="Hypercare and steady state" bold=first size=8.5 -->
| Item | Hypercare | Steady state (support model) |
|---|---|---|
| First contact | L1 and the floor support | L1 BDOI service desk |
| Triage | Twice a day in the command centre | L2 at logging; weekly operations review |
| Targets | Section 3.5 | Support Model chapter 4 |
| Releases | Hotfixes and one hypercare release | Monthly planned release; emergency releases |
| Reporting | Daily report and weekly board review | Monthly service report and review |
| Governance | Go / no-go board | BIBS Change Advisory Board; Service Owner |

# Warranty

## Period

The warranty runs for **12 months from go-live: 3 January 2028 to 2 January 2029** (proposal, OP-03). Hypercare is inside the warranty. From 3 January 2029 support continues under the annual maintenance and support agreement.

## Scope

<!-- table: widths=8.3,8.3 caption="Warranty scope" size=8.5 -->
| Covered at no charge | Not covered (handled as noted) |
|---|---|
| Correction of any non-conformance of BIBS with the signed FRS, as changed by approved change requests, and with the accepted UAT test cases, reported during the warranty | New or changed requirements: Change Management Register |
| Correction of data loaded wrongly by the migration functions of BIBS, where the extract was right | Errors in the legacy data or the extracts (BDOI data stewards) |
| Security vulnerabilities of the BIBS application and its components rated Critical or High | BDO systems on the other side of an interface; AWS managed services; the network |
| Performance below the SLOs at the volumes of the performance test, when the cause is in BIBS | Volumes or users above those tested, until a capacity change is agreed |
| The fix of a regression caused by a warranty fix | Configuration changes made in production without iorta TechNXT's involvement |
| Updates of the user guides, the runbook and the knowledge articles affected by a fix | Use of BIBS contrary to the user guides; third-party products not delivered by iorta TechNXT |

## How a warranty issue is handled

A warranty issue is a ticket of category Application fault (or Migrated data, when BIBS loaded the data wrongly). It follows the support model: the service levels of the support model apply after hypercare, the fix is delivered in a planned or emergency release, and the monthly service report lists the warranty issues opened and closed. A disagreement on whether an issue is a non-conformance or a change goes to the BIBS Change Advisory Board, then to the Program Manager and the iorta TechNXT account executive.

# Sign-off certificates

Each certificate is submitted by iorta TechNXT with its evidence. BDOI signs it, or lists in writing the criteria not met with reasons, within 5 business days of submission. A certificate with open items may be signed with those items listed and dated.

## C1 UAT acceptance certificate (one per drop)

```keyvalues
Certificate: C1 - UAT acceptance
Drop: "Drop 0 and Drop 1 (end to end) / Drop 2 - one certificate each"
BRDs covered: "Drop 0: BRD-03, BRD-11, BRD-13; Drop 1: BRD-01, BRD-02, BRD-04, BRD-05, BRD-06, BRD-09, BRD-10, BRD-12; Drop 2: BRD-07, BRD-08"
UAT period: "Aug-Dec 2027 (Drop 0 and Drop 1); Oct-Nov 2027 (Drop 2)"
Due: "15 Dec 2027 (Drop 0 and Drop 1); 30 Nov 2027 (Drop 2)"
Release accepted: The release package tested in UAT and recorded in the certificate
```

<!-- table: widths=1.2,9.6,2.6,3.2 caption="C1 acceptance criteria" bold=first size=8.5 -->
| # | Criterion | Met (Yes / No) | Evidence reference |
|---|---|---|---|
| 1 | Every UAT test case of the drop executed; at least 98 % passed; every failed case has an issue | | UAT results per BRD |
| 2 | No open Critical or High issue; Medium issues with fix dates and workarounds | | Issue log |
| 3 | End-to-end scenarios across the drops passed on migrated data (Mock 3 and Mock 4) | | Scenario results |
| 4 | Reports, letters and files of the drop checked by their business owners | | Checklists |
| 5 | Interfaces of the drop tested end to end with the other systems' owners | | Integration test records |
| 6 | User guides of the drop reviewed by the key users | | Review sign-off |
| 7 | Change requests raised in UAT recorded in the Change Management Register with a decision | | Register extract |

```signoff
rows:
  - {name: "", role: "Business owner of each BRD of the drop", organisation: BDOI}
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

## C2 Go-live acceptance certificate

```keyvalues
Certificate: C2 - Go-live acceptance
Go-live: Monday 3 January 2028, 08:00 PHT
Point of no return: Monday 3 January 2028, 18:00 PHT - continue in BIBS
Signed at: Week-1 review, Monday 10 January 2028 (T+7)
```

<!-- table: widths=1.2,9.6,2.6,3.2 caption="C2 acceptance criteria" bold=first size=8.5 -->
| # | Criterion | Met (Yes / No) | Evidence reference |
|---|---|---|---|
| 1 | GNG-3 passed with every criterion recorded; no waiver of a financial or clearing criterion | | Go / No-Go report |
| 2 | Point of no return passed: no Critical issue stopping receipts, payment application, remittance or booking; Migration Clearing 0.00; no legacy posting since the freeze; every branch transacted in BIBS | | Board decision CT-069 |
| 3 | First BIBS end-of-day runs, payment uploads, automatch, remittance extraction and Collections refresh completed (CT-067, CT-070, CT-073, CT-074) | | Run logs |
| 4 | Daily hypercare checks clean since go-live, or each break explained | | Daily hypercare reports |
| 5 | URGENT January renewals worked in the day-1 queue | | Renewal report |
| 6 | Staging data and extract files of the production load purged within 5 days of sign-off | | Purge log (CT-075) |

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services (chair of the go / no-go board)", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```

## C3 Hypercare exit and handover certificate

```keyvalues
Certificate: C3 - Hypercare exit and handover to support
Hypercare period: "3 January 2028 to 2 February 2028 (extended to the February close: yes / no)"
Warranty: 3 January 2028 to 2 January 2029
Support from: The day after signature, under the Support Model and Support Guide
```

<!-- table: widths=1.6,9.2,2.6,3.2 caption="C3 exit criteria" bold=first size=8.5 -->
| Ref | Criterion | Met (Yes / No / Waived) | Evidence reference |
|---|---|---|---|
| HX-01 to HX-08 | Exit criteria of the cutover plan (chapter 4) | | |
| HX-09 to HX-18 | Support handover criteria (chapter 4) | | |
| - | Open items transferred with owners and dates (attached list) | | |

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "BIBS Service Owner", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
  - {name: "", role: Support Manager, organisation: iorta TechNXT}
```

# Decisions and open points for BDOI {-}

<!-- table: widths=1.5,8.6,4,2.5 caption="Decisions and open points" bold=first size=8.5 -->
| Ref | Point and our proposal | Owner | Needed by |
|---|---|---|---|
| OP-01 | Confirm hypercare from go-live to the January 2028 month-end close (planned exit 2 Feb 2028), with extension to the February close | Program Manager | 30 Jun 2027 |
| OP-02 | Confirm the handover ticket level of fewer than 40 new tickets a business day for 5 consecutive business days (HX-10) | BIBS Service Owner | 30 Nov 2027 |
| OP-03 | Confirm the 12-month warranty from go-live and its scope, in the support agreement | Head, BDOI IT | 30 Jun 2027 |
| OP-04 | Confirm the UAT pass rate of 98 % and the other C1 criteria per drop | BIBS Product Owner with the BRD owners | 30 Jun 2027 |
| OP-05 | Confirm the signatories of C1 per drop (the approvers of each BRD's approval sheet), C2 and C3 | Program Manager | 30 Jun 2027 |
| OP-06 | Confirm the hypercare release window of Sunday 23 January 2028 and the release freeze over the January close | Head, BDOI IT | 30 Nov 2027 |

# Glossary {-}

```glossary
C1, C2, C3: UAT acceptance, go-live acceptance and hypercare exit certificates
Hotfix: An emergency release of a fix for a P1 issue
Hypercare: Close support by the project team from go-live to the first month-end close
Key user: The department's trained user who tests fixes and helps colleagues
Warranty: The period in which iorta TechNXT corrects non-conformances of BIBS at no charge
```

# Sign-off {-}

```signoff
rows:
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "BIBS Service Owner", organisation: BDOI}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
