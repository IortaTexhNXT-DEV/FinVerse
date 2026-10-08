---
# Knowledge Transfer Plan of the BIBS programme (BRD-00). Built by build_delivery_adoption.py.
title: Knowledge Transfer Plan
subtitle: Audiences, topics, sessions, materials, schedule and readiness assessment for BDOI application support, BDO IT operations and power users
doc_type: Knowledge Transfer Plan
doc_code: Adoption
brd: BRD-00
name: Knowledge Transfer Plan
doc_id: BIBS-KT-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: BIBS Knowledge Transfer Plan
h1_page_break: false
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect; iorta TechNXT change and training lead
    approver: Head, BDOI IT (pending)
    change: First issue
distribution:
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Support model and hand-over"}
  - {name: "BDO IT operations (ITIO-SRE, ITSD-AMS)", role: Reviewer, organisation: BDO Unibank, purpose: "Operations topics and readiness"}
  - {name: "BDOI application support lead", role: Reviewer, organisation: BDOI, purpose: "Application support topics and readiness"}
  - {name: "BIBS Product Owner", role: Reviewer, organisation: BDOI, purpose: "Power users and key users"}
  - {name: "Head, BDOI Information Security", role: Reviewer, organisation: BDOI, purpose: "Security operations topics"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivers the sessions"}
---

# Purpose, audience and scope

## Purpose

This plan transfers the knowledge BDOI needs to run and support BIBS from the hypercare exit on 2-Feb-2028: who learns what, in which sessions, with which materials, on which dates, and how readiness is measured before each step of the hand-over.

## Audience

BDOI IT and the BDOI application support lead (approval and delivery of the support model), BDO IT operations (ITIO-SRE and ITSD-AMS), BDOI Information Security, the BIBS Product Owner and the key users, and the iorta TechNXT project team that delivers the sessions.

## Scope

The application and its configuration, the operation of the platform on managed Kubernetes in the BDOI environments, the interfaces with BDO systems, the data migration functions used after go-live (legacy inquiry, run-off, true-ups) and the support processes. End-user training is planned in the Change Management and Training Framework; this plan covers the support and operations teams and the power users who back them.

# Support model after the hand-over

<!-- table: widths=2.4,4.2,6.4,3.6 caption="Support levels from 2-Feb-2028" size=8.5 -->
| Level | Team | Handles | Target |
|---|---|---|---|
| L1 | BDOI service desk (BDOI IT) | Logs every ticket; sign-in and access questions; password and unlock through the access process; routes to L2 | Logged and routed within 30 minutes in business hours |
| L2 | BDOI application support | How-to questions, configuration (lists, parameters, templates, rules), user access requests, data corrections through the business screens, job reruns, first analysis of errors | Severity 1 response 1 hour; Severity 2 4 hours |
| L3 | iorta TechNXT product support | Fixes and patch releases, root cause of application errors, security patches | Per the support agreement |
| Platform | BDO IT operations | Kubernetes, database, cache, messaging, document store, network, backups, DR, monitoring | Per the BDO operations standards |
| Power users | Key users of each unit | First help to colleagues; check of release notes; UAT of patches | Same day |

# Approach

Knowledge is transferred by doing, in four phases tied to the test and cut-over calendar, so that the BDOI teams practise on the same environments and events as the project team.

![Phases of the knowledge transfer](figures/kt_phases.dot){width=12}

<!-- table: widths=3,3,10.6 caption="Phases" size=8.5 -->
| Phase | Window | What happens |
|---|---|---|
| 1 Foundation | 7-Jun to 16-Jul-2027 | Classroom sessions and guided walkthroughs on SIT; reading of the runbook, the operations and configuration guides and the FRS; labs |
| 2 Shadowing | 2-Aug to 12-Nov-2027 | BDOI teams sit with the iorta team during UAT releases, Trial migrations 3 and 4, incident triage and month-end cycles; the iorta team does the work and explains it |
| 3 Reverse shadowing | 15-Nov to 31-Dec-2027 | BDOI teams do the work (deployments to UAT and Pre-Prod, dress rehearsal tasks, ticket handling, job monitoring) and the iorta team checks it |
| 4 Supported operation | 3-Jan to 2-Feb-2028 | Hypercare: BDOI runs L1 and L2 and the platform; the iorta team is on call for L3 and coaching |

# Audiences

<!-- table: widths=3.4,2.2,5.4,5.6 caption="Audiences of the knowledge transfer" size=8.5 -->
| Audience | People (proposed) | Role after go-live | Entry profile |
|---|---|---|---|
| BDOI application support | 6 (2 L1 service desk, 4 L2 functional and data analysts) | L1 and L2 support of BIBS | Knows the BDOI broking processes or the legacy systems; reads business reports; trained on the BIBS personas |
| BDO IT operations | 6 (2 platform and Kubernetes, 1 database, 1 monitoring and service desk tooling, 2 security operations) | Run the platform and DR | Operates managed Kubernetes, PostgreSQL and the BDO monitoring tools |
| BDOI IT integration owners | 3 | Own the interfaces with BDO systems and their schedules | Knows the BDO systems on the integration list |
| Power users (key users) | 2 per unit and persona group, about 40 | First help and UAT of patches | UAT testers of the drops |

# Topics

## BDOI application support

<!-- table: widths=1.2,4.4,6.6,1.8,2.6 caption="Topics for BDOI application support" size=8 -->
| ID | Topic | Content | Days | Trainer |
|---|---|---|---|---|
| AS-01 | BIBS business overview | The broking cycle from client onboarding to booking, cashiering, remittance, collections, accounting, renewal, claims; the BRDs and drops; personas and menus | 1 | iorta Business Analyst lead |
| AS-02 | User access administration | Access requests, group profiles, approvals, separation of duties, locked and dormant users, EIAM sign-in and UIDM-ISC provisioning (BRD-11) | 0.5 | iorta Business Analyst lead |
| AS-03 | Business configuration | Lists of values, system parameters, document templates, renewal rules, escalation rules, products and packages, accounting rules; maker-checker and audit trail | 1 | iorta Business Analyst lead |
| AS-04 | Workflow, approvals and notifications | My Work, My Approvals, workflow history, notices and e-mails through CCM, outbound message status | 0.5 | iorta Business Analyst lead |
| AS-05 | Scheduled jobs and period ends | Job schedule and batch window (booking batch, remittance extraction, collections refresh, GL-SL reconciliation, books close, package expiry, watchlist load); rerun rules; month-end and year-end | 1 | iorta Solution Architect |
| AS-06 | Interfaces and file exchanges | Payment uploads of the bank channels, disbursement outputs, insurer files, EGL and EDP feeds; what to check when a file fails | 1 | iorta Solution Architect |
| AS-07 | Data corrections | Corrections only through the business screens (reversals, correction entries, re-applications); what is never corrected directly | 0.5 | iorta Business Analyst lead |
| AS-08 | Reports | Report Centre, report packs, exports, report archive | 0.5 | iorta Business Analyst lead |
| AS-09 | Incident triage | Messages and their meaning, the error catalogue, log search in the BDO monitoring tools, severity rules, escalation to L3 | 1 | iorta QA lead |
| AS-10 | Data migration after go-live | Legacy inquiry and its access log, legacy invoices in Cashiering, run-off and decommissioning, true-ups | 0.5 | iorta Migration Lead |
| AS-11 | Releases and patches | Release notes per persona, acceptance of a patch in UAT, regression checklist | 0.5 | iorta Project Manager |

## BDO IT operations

<!-- table: widths=1.2,4.4,6.6,1.8,2.6 caption="Topics for BDO IT operations" size=8 -->
| ID | Topic | Content | Days | Trainer |
|---|---|---|---|---|
| OP-01 | Architecture and component baseline | One modular application and one web client on managed Kubernetes; PostgreSQL 16, Valkey 8, Apache Kafka 3.9, S3 document store, nginx web tier, Kubernetes Gateway API, Apigee X for integrations | 1 | iorta Solution Architect |
| OP-02 | Deployment and configuration | Environment configuration, release deployment, smoke test, rollback, data structure changes at start-up | 1 | iorta infrastructure team |
| OP-03 | Monitoring and alerting | Health checks, metrics, logs, alerts and their runbook actions; job failure mails | 1 | iorta infrastructure team |
| OP-04 | Backup, restore and DR | Backups and point-in-time restore, document store replication, DR switch-over (RPO 15 minutes, RTO 4 hours), DR drill | 1 | iorta infrastructure team with BDO IT |
| OP-05 | Security operations | Secrets and key rotation (sign-in tokens, MFA encryption, migration masking), certificates, EIAM and SCIM settings, break-glass administrator, vulnerability patching | 1 | iorta Solution Architect with Information Security |
| OP-06 | Capacity and performance | Sizing for 1,344 named and 429 concurrent users, scaling, results of the performance test | 0.5 | iorta QA lead |
| OP-07 | Batch window and schedules | Night jobs and their times in PHT, dependencies, extended hours for month-end | 0.5 | iorta Solution Architect |

## Power users

<!-- table: widths=1.2,4.4,6.6,1.8,2.6 caption="Topics for power users" size=8 -->
| ID | Topic | Content | Days | Trainer |
|---|---|---|---|---|
| PU-01 | Learning path of the persona | The learning path of the Change Management and Training Framework, at key-user depth | 1 to 3 | iorta change and training lead |
| PU-02 | Configuration owned by the unit | Lists, templates and rules the unit owns, through maker-checker | 0.5 | iorta Business Analyst lead |
| PU-03 | First-line help | Help Center, quick-reference guides, messages, how to log a good ticket | 0.5 | iorta change and training lead |
| PU-04 | Patch acceptance | Reading release notes, re-testing the changed steps | 0.5 | iorta QA lead |

# Sessions and schedule

<!-- table: widths=3.2,6,4,3.4 caption="Session schedule" size=8.5 -->
| Dates | Sessions | Audience | Environment |
|---|---|---|---|
| 7 to 18-Jun-2027 | AS-01 to AS-04; OP-01, OP-02 | Application support; operations | SIT |
| 21-Jun to 2-Jul-2027 | AS-05 to AS-08; OP-03, OP-07 | Application support; operations | SIT |
| 5 to 16-Jul-2027 | AS-09 to AS-11; OP-04 to OP-06 (OP-06 repeated after the performance test); readiness check 1 | All | SIT |
| 19 to 30-Jul-2027 | PU-01 to PU-04 with key user training | Power users | Training environment |
| 2-Aug to 12-Nov-2027 | Shadowing: UAT releases every two weeks, Trial migrations 3 and 4, two month-end cycles, incident triage twice a week; readiness check 2 on 12-Nov-2027 | Application support; operations; integration owners | UAT |
| 15-Nov to 31-Dec-2027 | Reverse shadowing: dress rehearsal tasks, deployments to Pre-Prod, DR drill (proposed 6-Dec-2027), ticket handling; readiness check 3 on 17-Dec-2027 | Application support; operations | UAT, Pre-Prod |
| 3-Jan to 2-Feb-2028 | Supported operation in hypercare; daily triage at 10:00 and 16:00; hand-over on 2-Feb-2028 | All | Production |

# Materials

<!-- table: widths=4.4,6.4,3,2.8 caption="Materials" size=8.5 -->
| Material | Content | Owner | Ready by |
|---|---|---|---|
| Operations runbook | Start, stop, deploy, rollback, scale, backups, restore, DR, alerts and their actions, job rerun rules | iorta infrastructure team | 4-Jun-2027 |
| Deployment and configuration guides | Environment settings, secrets, Gateway API routes, Apigee X proxies, job schedules | iorta Solution Architect | 4-Jun-2027 |
| Technical Specification per sign-off set | Interfaces, data, jobs, access, as reviewed by BDOI IT | iorta Solution Architect | With each set |
| Security baseline | Sign-in, MFA, sessions, roles, data scope, logging, patching | iorta Solution Architect | 4-Jun-2027 |
| FRS and sign-off sets | Business rules, screens, messages, walkthroughs | iorta Business Analyst lead | Signed versions |
| User Manual, quick-reference guides, Help Center | What users do, step by step | iorta change and training lead | 30-Sep-2027 |
| Knowledge base | Articles per frequent question and incident, written during SIT and UAT; at least 60 articles by go-live | iorta QA lead with BDOI application support | 17-Dec-2027 |
| Recorded sessions | Every classroom session recorded and indexed by topic | iorta change and training lead | One week after each session |
| Labs | Practical exercises on SIT and UAT (deploy, restore, rotate a secret, rerun a job, correct a posting through the screens) | iorta infrastructure team; iorta Business Analyst lead | 4-Jun-2027 |

# Readiness assessment

Readiness is measured three times before go-live and once at the hand-over. Each check is scored green, amber or red; the steering committee sees the result. A red result at check 3 is a go-live risk raised to the go / no-go board.

<!-- table: widths=3,7.2,6.4 caption="Readiness criteria" size=8.5 -->
| Audience | Criteria | Green when |
|---|---|---|
| Application support | Knowledge test per topic; practical tasks done alone (configure a list value with maker-checker, process an access request, rerun a failed job, trace a posting from invoice to journal, triage five incidents) | Test score 80 percent or more; all tasks done unaided by check 3; 90 percent of tickets in reverse shadowing routed and resolved correctly |
| BDO IT operations | Deploy a release to UAT and roll it back; restore the database to a point in time in Pre-Prod; rotate a secret; run the DR drill; act on five alerts from the runbook | All tasks done unaided by check 3; DR drill within RTO 4 hours and RPO 15 minutes |
| Integration owners | Run and check each interface of their system; handle a failed file | All interfaces exercised in UAT |
| Power users | Learning path assessment; coach two colleagues; accept one patch in UAT | Assessment 80 percent or more |
| Hand-over (2-Feb-2028) | Checks 1 to 3 green; hypercare exit criteria met; knowledge base complete; open tickets handed over with owners | Signed hand-over record |

# Responsibilities

<!-- da:raci Support,Deployment -->

# Risks

<!-- table: widths=1.2,5.6,2.4,7.4 caption="Risks of the knowledge transfer" size=8.5 -->
| ID | Risk | Rating | Mitigation |
|---|---|---|---|
| KT-1 | Support and operations staff named late (DEP-18) | High | Names by 31-May-2027; sessions recorded for late joiners |
| KT-2 | Staff pulled into UAT or year-end work during shadowing | Medium | Shadowing slots fixed in the UAT calendar; back-up per role |
| KT-3 | BDO monitoring and log tools not connected before reverse shadowing | Medium | Connection checked at readiness check 2 |
| KT-4 | Knowledge held by one person in BDOI or iorta TechNXT (R-23) | Medium | Two people per topic; knowledge base articles |

# References

- Project Plan, Delivery Methodology, RACI Matrix and Risk Register, version 1.0.
- BIBS deployment guide, configuration guide and runbook; security baseline; quality gates.
- Data Migration Handbook (cut-over, hypercare roster, true-ups).
- IER Workbook v20 and the programme alignment pack (environments, RPO and RTO).
- Change Management and Training Framework and the User Manual Framework, version 1.0.

# Decisions and open points for BDOI

<!-- table: widths=1.2,8.4,3.4,2,1.6 caption="Decisions and open points" size=8.5 -->
| No. | Decision or open point (our proposal) | Owner | Needed by | Ref. |
|---|---|---|---|---|
| KT-01 | Confirm the support model: L1 BDOI service desk, L2 BDOI application support, L3 iorta TechNXT, platform BDO IT operations | Head, BDOI IT | 31-Mar-2027 | A-14 |
| KT-02 | Name the application support team (6 people proposed) and the operations staff (6 proposed) | Head, BDOI IT; BDO IT operations | 31-May-2027 | DEP-18 |
| KT-03 | Give access for the BDOI teams to SIT and UAT and to the BDO monitoring tools from 7-Jun-2027 | BDOI IT | 31-May-2027 | - |
| KT-04 | Confirm the DR drill date (proposed 6-Dec-2027) and the region | BDOI IT; Information Security | 30-Sep-2027 | IQ26 |
| KT-05 | Confirm the ticketing tool and the severity definitions for production | Head, BDOI IT | 30-Sep-2027 | - |
