---
# Support Model and Support Guide of BIBS (programme level, BRD-00).
# Build: python docs/deliverables/src/programme/operations/build_operations_pack.py
title: Support Model and Support Guide
subtitle: L1, L2 and L3 support of BIBS in production - service levels, processes, escalation and the L1 troubleshooting guide
doc_type: Support Model
doc_code: Support
brd: BRD-00
name: Support Model and Support Guide
doc_id: BIBS-OPS-SUP-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Support Model and Support Guide
output: Operations/BIBS_Support_BRD-00_Support_Model_and_Support_Guide_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Service Delivery Lead
    reviewer: iorta TechNXT Project Manager; iorta TechNXT Solution Architect
    approver: Head, BDOI IT (pending)
    change: First issue. Support levels, service hours, severity, SLA and SLO, processes, escalation, tools, reporting and the L1 troubleshooting guide, from the umbrella FRS BRD-00, the discrepancy register (DCR-133 to DCR-138), the Data Migration Handbook Part D and the operations documentation of BIBS
distribution:
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Support model, L1 and L2 staffing, tools"}
  - {name: "BIBS Product Owner (Marketing Business System)", role: Approver, organisation: BDOI, purpose: "Service levels and business priorities"}
  - {name: "Program Manager, Business Project Services", role: Reviewer, organisation: BDO Unibank ESG, purpose: "Handover from the programme to support"}
  - {name: "BDO service desk lead", role: Reviewer, organisation: BDO Unibank IT, purpose: "L1 procedures, ITSM tool, hours"}
  - {name: "Cloud and Digital Operations Engineering", role: Reviewer, organisation: BDO Unibank IT, purpose: "Infrastructure resolver group, on-call"}
  - {name: "Heads of Operations, Comptrollership, Marketing; Compliance", role: Reviewers, organisation: BDOI, purpose: "Severity examples and business escalation"}
  - {name: "Information Security Officer", role: Reviewer, organisation: BDOI, purpose: "Security incidents, access requests"}
  - {name: Project and support team, role: Delivery, organisation: iorta TechNXT, purpose: "L3 support, releases, knowledge base"}
---

# Introduction

## Purpose

This document sets out how BIBS (BDOI Broker System, on iNXT BrokerVerse) is supported in production from the end of hypercare: who does what at each support level, the service hours, how incidents are classified and how fast they are answered and resolved, the service level objectives of the platform, the incident, problem, change, release and knowledge processes, the escalation matrix, the tools and the service reporting. Its second part is the L1 troubleshooting guide that the BDOI service desk uses on the first call.

It applies from the hypercare exit, planned for 2 February 2028 after the January 2028 month-end close (Hypercare, Warranty and Sign-off Plan). During hypercare the same levels work with shorter targets and the command centre of the cutover.

## Audience

<!-- table: widths=4.6,12 caption="Audience" bold=first -->
| Reader | Uses this document for |
|---|---|
| BDOI service desk (L1) | Logging and classifying tickets, the first fix from chapter 12, when to escalate |
| BDOI application support (L2) | Triage, configuration and data checks, job reruns, problem records, contact with the business |
| iorta TechNXT support (L3) | Service levels, on-call, fixes, releases, root-cause analysis |
| BDO Cloud Operations | Infrastructure incidents, platform changes and on-call |
| BDOI business process owners | Severity of business impact, decisions, service reports |
| Head, BDOI IT and the BIBS Product Owner | Approval of the model, the service levels and the tools |

## Scope

In scope: BIBS in production in AWS ap-southeast-1, all modules of Drops 0, 1 and 2 that go live together in January 2028, the integrations listed in the Integration Inventory, the migrated legacy data and the Legacy Inquiry archive. Out of scope: the legacy systems (EBIX, QPS, ISYS, CMS) after the freeze, supported by BDOI IT until their decommissioning; the BDO systems on the other side of each interface (EIAM, UIDM-ISC, Apigee X, CCM, ECM, bank channels, EGL, EDP), supported by their owners; the ReInsurance module, which is phase 2.

## Reference documents

<!-- table: widths=1,7,8.6 caption="Reference documents" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| R1 | FRS BRD-00 Core Replacement v1.0 (umbrella), chapter Non-functional requirements | Users, response time, availability, retention, backup |
| R2 | BRD Discrepancy and Clarification Register v1.2, items DCR-133 to DCR-140, DCR-166, DCR-224, DCR-225 | Service window, availability, RPO and RTO, response time, maintenance window, monitoring stack |
| R3 | Programme Alignment - Drops, Integrations and Infrastructure v1.0 and the Integration Inventory | Integrations and their owners, environments, component baseline of 8 October 2026 |
| R4 | Data Migration Handbook BRD-13 v2.1, Part D | Command centre, hypercare roster, hypercare exit criteria |
| R5 | Operations Runbook and Observability v1.0 | Monitoring, alert catalogue, scheduled jobs, standard procedures |
| R6 | Disaster Recovery and Business Continuity Plan v1.0 | Recovery objectives, failover, manual workarounds |
| R7 | Hypercare, Warranty and Sign-off Plan v1.0 | Hypercare targets, handover to this model, warranty |
| R8 | FRS of BRD-1 to BRD-13 and the user guides of the sign-off sets | Expected behaviour of each screen, messages, roles |
| R9 | Change Management Register v1.0 | Change requests against the signed FRS |

# Support model

## Three support levels

BIBS is supported on three levels. BDOI runs the first two; iorta TechNXT is the third. BDO Cloud Operations and the owners of the other BDO systems are resolver groups that L2 and L3 call on (Figure 1).

![Support levels of BIBS and the resolver groups around them](figures/sup_tiers.dot){width=15}

<!-- table: widths=2.4,3.4,6.6,4.2 caption="Support levels" bold=first size=8.5 -->
| Level | Who | Does | Does not |
|---|---|---|---|
| L1 | BDOI service desk (BDO Unibank IT service desk, BIBS queue) | Takes every call, e-mail and portal request; logs the ticket with the minimum content (section 5.2); proposes the severity; solves with the L1 guide (chapter 12): sign-in, second factor, access, uploads, reports, periods, how-to; raises access requests; keeps the user informed; closes the ticket | Change configuration, run jobs, correct data, decide on business rules |
| L2 | BDOI application support (BIBS team in BDOI IT, with one key user per department) | Triage and severity; checks configuration, master data, workflow and approval queues, scheduled jobs, alerts and integration events on the BIBS screens; reruns jobs; clears caches; applies standard changes; finds workarounds; owns problem records; reproduces in UAT; works with BDO Cloud Operations and the interface owners | Change the application, run a data correction script, release |
| L3 | iorta TechNXT support (application support engineers, developers, DevOps) | Faults in BIBS: analysis from logs and traces, fixes, emergency and planned releases, data corrections through an approved change, performance, root-cause analysis, knowledge articles for L1 and L2 | Change BDOI business configuration without a ticket and BDOI's written approval |
| Resolver | BDO Cloud Operations (Cloud and Digital Operations Engineering) | AWS account, EKS, RDS, ElastiCache for Valkey, MSK, S3, network, WAF and Gateway, certificates of the BDO PKI, backups and restores, DR failover | Application changes |
| Resolver | BDOI IT system owners | EIAM (Entra ID), UIDM-ISC, Apigee X, CCM, ECM, M365, bank channels (CMS / New BOB, OBPCS, Old BOB, PMS), EGL, EDP, HL-LOAS, LMS, LFS SaaS, Bridger Insight XG | BIBS changes |

## Roles

<!-- table: widths=4.4,3,9.2 caption="Named roles of the support organisation" bold=first size=8.5 -->
| Role | Organisation | Responsibility |
|---|---|---|
| BIBS Service Owner | BDOI IT | Accountable for the BIBS service; chairs the monthly service review; approves the service report |
| BIBS Product Owner | BDOI (Marketing Business System) | Business priority of problems and changes; chairs the BIBS Change Advisory Board (CAB) |
| L1 team lead | BDO service desk | L1 staffing, quality of tickets, use of the L1 guide, first-contact resolution |
| L2 lead | BDOI IT | Triage quality, problem records, standard changes, contact with BDO Cloud Operations and the interface owners |
| Key users (super users) | Each BDOI department | Answer how-to questions in their team; confirm business impact and the fix; test releases in UAT |
| Information Security Officer | BDOI | Security incidents, privileged access, access reviews, legal holds |
| Data Protection Officer | BDOI | Personal data breaches (notification to the National Privacy Commission within 72 hours of knowledge) |
| iorta TechNXT Support Manager | iorta TechNXT | L3 service levels, on-call roster, root-cause analysis reports, monthly service report |
| iorta TechNXT L3 on-call engineer | iorta TechNXT | First L3 responder for P1 and paged alerts |
| Cloud Operations on-call | BDO Unibank IT | First responder for platform alerts and DR |

## RACI

R = responsible, A = accountable, C = consulted, I = informed.

<!-- table: widths=5.2,1.2,1.2,1.2,1.4,1.4,1.4,1.4 caption="RACI of support activities. SO = BIBS Service Owner; PO = BIBS Product Owner; CO = BDO Cloud Operations; BU = business process owner" size=8 -->
| Activity | L1 | L2 | L3 | CO | SO | PO | BU |
|---|---|---|---|---|---|---|---|
| Log, classify and keep users informed | R | C | I | - | A | - | I |
| Triage and confirm severity | C | R | C | C | A | I | C |
| Restore service (application) | I | R | R | C | A | I | I |
| Restore service (infrastructure) | I | C | C | R | A | I | I |
| Data correction | I | R | R | - | A | C | A |
| Problem management and RCA | I | R | R | C | A | C | I |
| Standard change | - | R | I | I | A | I | I |
| Normal change and release | I | C | R | C | A | A | C |
| Emergency change | I | C | R | C | A | C | I |
| Knowledge base | C | R | R | C | A | I | I |
| Monthly service report and review | I | C | R | C | A | C | I |
| Access requests (BRD-11) | R | C | - | - | I | - | A |

# Service hours

The service window of BIBS is the window in which users work and in which the availability objective is measured. It is one window for all modules (DCR-133, proposal for confirmation).

<!-- table: widths=4.2,5.2,7.2 caption="Service hours (Philippine time, PHT)" bold=first size=8.5 -->
| Service | Hours | Remarks |
|---|---|---|
| BIBS service window | 06:00-22:00 Monday to Saturday, and 06:00-22:00 on a Sunday or holiday that is a month-end closing day | Availability measured here (section 4.3) |
| Batch window | 22:00-06:00 every day | Night jobs (Operations Runbook, scheduled jobs catalogue); the service stays available to users |
| Planned maintenance window | Sunday 00:00-04:00, at most twice a month; announced 5 business days ahead | Releases, patching, DR drills; not counted as downtime. No maintenance in the month-end freeze (last 2 and first 3 business days of a month) |
| L1 BDOI service desk | 06:00-22:00 Monday to Saturday and month-end Sundays; P1 outside these hours through the BDO IT 24 x 7 hotline | Telephone, e-mail and ITSM portal |
| L2 BDOI application support | 07:00-20:00 Monday to Saturday; on call 20:00-07:00 and Sundays for P1 and for failures of class A jobs | Two analysts per shift proposed; one on call |
| L3 iorta TechNXT | 08:00-18:00 Monday to Friday (business days); 24 x 7 on call for P1; P2 also 07:00-22:00 on Saturdays and month-end Sundays | On-call engineer reached by page and telephone |
| BDO Cloud Operations | 24 x 7 | BDO standard |

Times of service levels count service hours of the level that holds the ticket, except P1, which counts elapsed time.

# Severity and service levels

## Severity matrix

The severity follows the business impact and the urgency. L1 proposes it when logging the ticket; L2 confirms or changes it at triage and records why.

<!-- table: widths=2.2,5.2,9.2 caption="Severity levels with BIBS examples" bold=first size=8.5 -->
| Severity | Definition | BIBS examples |
|---|---|---|
| P1 Critical | BIBS or a core process is down for all users or for all branches, financial data is wrong at scale, or a security or personal data breach is suspected | Nobody can sign in (EIAM or BIBS); receipts cannot be issued in any branch; the remittance extraction fails on a remittance due date; journals post wrong amounts; the month-end close is blocked on its last day; the platform is unavailable; personal data exposed |
| P2 High | A core process is down for one department or branch, or seriously degraded, with no workaround; a class A job failed | One branch cannot issue receipts; payment uploads of one bank channel are refused; the renewal extraction failed (RNW_EXTRACTION); the watchlist ingestion failed; the For Application To Invoice file was not produced by 06:00; screens slower than 5 seconds for many users |
| P3 Medium | A function fails or gives a wrong result and a workaround exists, or few users are affected | One report column wrong; an upload refuses one valid row; a letter layout problem; one user cannot see a menu that the role should give |
| P4 Low | Question, cosmetic issue, documentation, or request for a change | Wording on a screen; how-to question; new report request (goes to change management) |

<!-- table: widths=3.6,4.3,4.3,4.3 caption="Impact and urgency to severity" bold=first size=8.5 -->
| Impact / urgency | Urgent (deadline today, money or compliance at risk) | Normal | Low |
|---|---|---|---|
| All users or all branches | P1 | P1 | P2 |
| One department, branch or process | P1 | P2 | P3 |
| One or a few users | P2 | P3 | P4 |

Security events are at least P2. A suspected personal data breach is P1 and also follows the BDOI data breach procedure (section 5.6).

## Service levels (SLA)

The targets below are the proposal of iorta TechNXT for the BIBS support agreement. They become binding when BDOI signs the agreement.

<!-- table: widths=1.6,2.6,2.6,3,3.2,3.6 caption="Response and resolution targets after hypercare" bold=first size=8.5 -->
| Severity | L1 logs and answers the user | L2 / L3 response (engaged on the ticket) | Updates to the user | Restore service or workaround | Permanent resolution |
|---|---|---|---|---|---|
| P1 | 10 minutes | 15 minutes (L3 on-call within 30 minutes) | Every 60 minutes | 4 hours elapsed | Root cause in 5 business days; fix in an emergency release or the next planned release |
| P2 | 15 minutes | 30 minutes | Every 4 service hours | 8 service hours | Next planned release, at most 20 business days |
| P3 | 30 minutes | 4 service hours | Every 2 business days | 5 business days | Planned release agreed with the Product Owner |
| P4 | 1 business day | 2 business days | Weekly | Not applicable | By agreement or as a change request |

Clock rules: the clock starts when the ticket holds the minimum content; it stops while the ticket waits for BDOI (status Awaiting user) or for a third party outside BIBS that is not under the support agreement (status Awaiting third party), and ends when service is restored or a workaround is given. A ticket meets its SLA when both the response and the restore targets are met.

## Service level objectives (SLO) and error budget

The SLOs measure the platform, not the tickets. They take the non-functional targets of the umbrella FRS (R1) and the register proposals (DCR-134, DCR-137) that BDOI is asked to confirm.

<!-- table: widths=3.4,6,3.4,3.8 caption="Service level objectives" bold=first size=8.5 -->
| SLO | Measure | Objective | Measured by |
|---|---|---|---|
| Availability | Minutes in the service window in which the synthetic sign-in and a read of the Dashboard succeed, divided by the minutes of the service window, per calendar month; planned maintenance excluded | 99.9 % | Synthetic check every minute (Dynatrace) |
| Screen latency | Share of screen requests and single-record actions answered within 2 seconds, per hour of the service window | 95 % within 2 s; 99 % within 5 s | Request metrics of bibs-web |
| Reports | Interactive reports answered within 20 seconds; longer reports run as jobs with progress | 90 % within 20 s | Report run log |
| Error rate | Share of user requests that end in a server error, per day | Below 0.5 % | Request metrics; error log |
| Night batch | Class A jobs finished before 06:00 | 99 % of business days | Scheduled Jobs history |
| Integration events | Events delivered to Kafka within 5 minutes of the business transaction | 99 % | Outbox age |
| E-mail | E-mails handed to CCM within 15 minutes of being queued | 99 % | Mail queue age |
| Sign-in | Sign-ins through EIAM completed without a BIBS error | 99.5 % | Sign-in audit and SSO error count |

**Error budget.** An objective of 99.9 % leaves 0.1 % of the service window as budget. With 16 hours a day on about 26 days, the month has about 24,960 service minutes, so the budget is about **25 minutes of unavailability a month**. The latency budget is 5 % of screen requests above 2 seconds.

<!-- table: widths=4.6,12 caption="Error budget policy" bold=first size=8.5 -->
| Budget used in the month | Action |
|---|---|
| Up to 50 % | Normal change and release calendar |
| 50 % to 100 % | Only fixes and low-risk changes in the next window; root cause of each incident that used the budget reviewed at the weekly operations review |
| Above 100 % | Change freeze for anything but fixes and security patches until the actions of the root-cause analyses are done; the Service Owner informs the Head of BDOI IT and the BIBS Product Owner |

## Service level reporting

SLA and SLO results are reported monthly (chapter 11). A missed P1 or P2 target is explained in the report with its cause and action.

# Incident management

## Process

Figure 2 shows how a ticket moves through incident, problem, change and release management.

![Incident, problem, change and release](figures/sup_incident.dot){width=13.5}

## Minimum content of a ticket

L1 does not pass a ticket to L2 without:

- the user's name, department and branch, and the role used (never the password or a second-factor code);
- the screen (menu path) and the record number (client, quotation, account, invoice, receipt, remittance batch, journal, request);
- the date and time to the minute and what the user did, what happened and what was expected;
- the message shown, word for word, and the **reference** shown under an unexpected error ("The system could not complete the request. Try again; if it continues, contact support with the reference below.");
- a screenshot with the client's personal data covered;
- how many users or branches are affected and whether work can continue.

## Ticket statuses

<!-- table: widths=3.4,9.4,3.8 caption="Ticket statuses" bold=first size=8.5 -->
| Status | Meaning | Set by |
|---|---|---|
| New | Logged by L1 with the minimum content | L1 |
| Assigned | Severity confirmed, owner named, first response sent | L2 |
| In progress | Under analysis or being fixed | L2 or L3 |
| Awaiting user | Waiting for information, a test or a decision from BDOI; the clock stops | L2 or L3 |
| Awaiting third party | Waiting for a BDO system owner or AWS; the clock stops for L3 | L2 |
| Workaround given | Service restored; the permanent fix is tracked on a problem record | L2 or L3 |
| Resolved | Fix or answer delivered | L2 or L3 |
| Closed | User confirmed, or no reply within 5 business days of Resolved | L1 or the ITSM tool |
| Reopened | The user reports within 5 business days that the issue remains | L1 |

## Triage steps at L2

1. Confirm severity and impact with the key user of the department.
2. Read the message and the reference; check *Home › Alerts* and *Setup & Administration › Administration › Scheduled Jobs* for a related alert or failed job.
3. Check configuration before suspecting BIBS: the role and data scope of the user (*Users*, *User Access Matrix*), the system parameter or list of values concerned and its history in *Audit Trail*, the workflow stage and the approval queue of the record.
4. For an interface: *Integration Events* (outbox and dead letters), the latest upload or file run, and the status of the other system with its owner.
5. Reproduce in UAT with the same role and comparable data when possible.
6. Classify the cause: user or training, configuration, master or transaction data, BIBS fault, infrastructure, third party.
7. Resolve at L2 (standard change, job rerun, cache clear, data fix through the screens) or pass to L3 with the evidence; for infrastructure open a ticket to BDO Cloud Operations.

## Major incident

A P1 is a major incident. The L2 lead opens a bridge within 15 minutes with the L3 on-call engineer and, when the platform is concerned, the Cloud Operations on-call. The iorta TechNXT Support Manager is incident manager until service is restored. L1 sends the first user notice within 30 minutes and an update every hour (templates in the knowledge base). A P1 that cannot be restored within 2 hours in the primary region is assessed for disaster recovery (Disaster Recovery and Business Continuity Plan, chapter 7).

## Security incidents and personal data breaches

- Every suspected security incident (account misuse, unusual export from Legacy Inquiry, malware found in an upload, a token reuse that the user does not explain) is logged as at least P2 and assigned to the Information Security Officer, with L2 and L3 preserving the evidence (audit trail, sign-in history, logs).
- A suspected personal data breach is P1. The Data Protection Officer decides on notification; the National Privacy Commission is notified within 72 hours of knowledge where required. iorta TechNXT supplies the facts from the logs and the audit trail within 24 hours of the request.

# Problem management

- A problem record is opened for every P1, for every P2 whose cause is not known when service is restored, and for every cause behind three or more incidents in a calendar month.
- L2 owns the record; L3 writes the root-cause analysis (timeline, impact, cause, why it was not caught, actions with owners and dates) within 5 business days of restore for a P1 and 10 business days for a P2.
- When the workaround is known, a known error (symptom, cause, workaround) is published in the knowledge base for L1 and L2.
- The problem closes when the permanent fix is released and no incident of the same cause occurs for 30 days.
- Open problems are reviewed at the weekly operations review and in the monthly service review.

# Change and release management

## Types of change

<!-- table: widths=2.4,8.2,6 caption="Types of change" bold=first size=8.5 -->
| Type | Examples | Approval |
|---|---|---|
| Standard | Unlocking a user; a second-factor reset (two administrators); a user access request approved in BIBS or through UIDM-ISC; a list of values or system parameter that the business maintains with maker-checker; a job rerun; a cache clear | Pre-approved; done by the business administrator or L2; recorded in the ticket and in the BIBS audit trail |
| Normal | A release; a change of an accounting rule, workflow, package or business parameter with effect on postings; a data correction; an infrastructure change; a change of an interface | Change request with impact, test evidence and back-out; approved by the BIBS CAB |
| Emergency | Fix for a P1 or a P2 that cannot wait; urgent security patch | Approved by the BIBS Service Owner and the BIBS Product Owner (or deputies) by telephone or e-mail; reviewed at the next CAB |

The BIBS CAB meets weekly on Thursday: BIBS Product Owner (chair), BIBS Service Owner, L2 lead, BDO Cloud Operations, Information Security Officer, iorta TechNXT Support Manager, and the business owner of each change on the agenda. Changes to the signed FRS follow the Change Management Register (R9).

## Data corrections

A correction of business data that the screens cannot make is a normal change. It needs the ticket, the business owner's written approval of the before and after values, a dry run on a copy of production in Pre-Prod (masked if people without production access use it), a backup point taken just before, and a second person who checks the result. Financial records are never edited: corrections are reversals and new postings through BIBS functions.

## Releases

<!-- table: widths=3.4,13.2 caption="Release standard" bold=first size=8.5 -->
| Item | Standard |
|---|---|
| Planned release | Monthly, when there are fixes or approved changes; in the maintenance window of Sunday 00:00-04:00 |
| Emergency release | For P1 or P2 fixes and urgent security patches, at the time agreed by the Service Owner |
| Path | SIT, then UAT (business regression by the key users), then Pre-Prod (rehearsal of the deployment with production-like data), then production. The same release package moves through every environment |
| Release notes | Fixes, changes, new parameters, the jobs affected, the back-out plan; sent 5 business days ahead |
| Freeze | No release in the last 2 and first 3 business days of a month, from 15 December to 5 January, and in the 5 business days before a BIR filing deadline that BDOI names, except emergency releases |
| Before | Database backup point; previous release package kept ready |
| After | Smoke test by L2 and the key users (sign-in, one transaction per module, a report, the job monitor), within 30 minutes of the end of the deployment |
| Back-out | Redeploy the previous release package. Database changes of a release are backward compatible with the previous release, so the back-out needs no database restore |

## Patching

<!-- table: widths=4,12.6 caption="Patching standard" bold=first size=8.5 -->
| Component | Standard |
|---|---|
| BIBS platform components (OpenJDK 21, Spring Boot 3.5, React, nginx) and libraries | Security advisories reviewed monthly; critical and high vulnerabilities fixed within 14 days by an emergency or the next planned release; component baseline reviewed every year (Programme Alignment, component baseline) |
| Container base images | Refreshed with each planned release and at least every quarter; scanned before release |
| Managed services (RDS PostgreSQL 16, ElastiCache for Valkey 8, MSK Kafka 3.9, EKS) | Minor versions in the maintenance window by BDO Cloud Operations, after SIT and UAT; major upgrades as projects |
| Certificates | Renewal alarm 30 days before expiry; in-cluster certificates renewed automatically 15 days before expiry |

# Knowledge management

- The knowledge base lives in the BDOI ITSM tool and is shared by L1, L2 and L3. Its starting set at hypercare exit is chapter 12 of this document, the user guides of the sign-off sets, the Operations Runbook procedures and the known errors of hypercare.
- Each article names the screen by its menu path, the role, the BIBS release it applies to, the date reviewed and the owner.
- Every resolved ticket is checked for an article (new, updated or none needed); L2 approves articles for L1.
- Articles are reviewed at every planned release and at least twice a year.

# Escalation matrix

Functional escalation moves a ticket to the next level; hierarchical escalation brings in management when a target is at risk.

<!-- table: widths=5,5.6,6 caption="Escalation matrix" bold=first size=8.5 -->
| Trigger | BDOI side | iorta TechNXT and BDO Cloud Operations side |
|---|---|---|
| P1 logged | L1 informs the L2 lead at once; L2 lead informs the BIBS Service Owner | L3 on-call and the Support Manager engaged; Cloud Operations on-call paged for platform alerts |
| P1 not restored in 1 hour | Service Owner informs the Head of BDOI IT and the BIBS Product Owner | Support Manager engages the engineering lead; hourly bridge updates |
| P1 not restored in 2 hours | Head of BDOI IT informs the business heads concerned; DR assessment (DR plan) | iorta TechNXT head of delivery informed |
| P1 not restored in 4 hours | Business continuity procedures of the department in force (DR plan, chapter 10) | iorta TechNXT account executive informed; RCA report owed |
| P2 without response in 30 minutes | L1 calls the L2 lead | L2 lead assigns; Support Manager informed if L3 is needed |
| P2 not restored in 8 service hours | Service Owner informed | Support Manager sends a plan with dates |
| Disagreement on severity or closure | L2 lead, then the Service Owner | Support Manager, then the account executive |
| Three or more incidents of one cause in a month | Monthly service review | Problem record and RCA |

The names and telephone numbers of each role are kept in the contact annex of the support agreement and in the ITSM tool, and checked every quarter.

# Tools

<!-- table: widths=4,7.4,5.2 caption="Support tools" bold=first size=8.5 -->
| Tool | Use | Owner |
|---|---|---|
| BDOI ITSM tool (BDO service management platform) | Tickets, problems, changes, knowledge base, SLA clocks and reports | BDOI IT |
| BIBS screens for support (Setup & Administration) | *Users*, *Roles & Permissions*, *Second Factor*, *Audit Trail*, *System Parameters*, *Exception Codes*, *Scheduled Jobs*, *Integration Events*, *Caches*, *Legal Holds*, *Application Info*; *Home › Alerts*; *User Access* requests | L2 (system administrator roles) |
| Dynatrace (APM named in the IER) | Synthetic checks, traces, service dashboards, paging | BDO Cloud Operations |
| Metrics and logs (Prometheus metrics of BIBS, CloudWatch, the BDO log platform and SIEM) | Metrics, alert rules, log search by correlation id and error reference | BDO Cloud Operations; read access for L2 and L3 |
| AWS console (read-only for L3) | Managed service status, backups, replication | BDO Cloud Operations |
| Bridge and chat channel (M365) | Major incidents, release calls | BDOI IT |
| UAT and Pre-Prod environments | Reproduction, release tests, data correction dry runs | L2 and L3 |

The ITSM tool and the paging route are named by BDOI IT (open point OP-02).

# Service reporting

## Monthly service report

iorta TechNXT sends the report by the 10th business day of the month; the BIBS Service Owner reviews it with the Product Owner, the L2 lead, BDO Cloud Operations and iorta TechNXT in the monthly service review. It shows:

- tickets opened, resolved and open by severity, category, module and department; tickets that missed a target and why;
- availability against 99.9 %, minutes of planned maintenance, error budget used;
- screen latency, error rate, night batch and integration SLOs;
- P1 and P2 incidents with the status of their RCA; problems and known errors opened and closed;
- changes and releases, failed or backed-out changes;
- backup and restore tests, DR drills, certificate renewals, security events and access reviews;
- trends, risks and recommendations, including training needs seen in the tickets.

## KPIs

<!-- table: widths=4,8.6,4 caption="Support KPIs (proposed targets)" bold=first size=8.5 -->
| KPI | Definition | Target |
|---|---|---|
| Response SLA | Tickets answered within target / tickets | 95 % (P1 100 %) |
| Restore SLA | Tickets restored within target / tickets | 90 % (P1 100 %) |
| First-contact resolution at L1 | Tickets resolved by L1 without escalation / tickets logged by L1 | 40 % from month 3 after hypercare |
| Reopen rate | Tickets reopened / tickets resolved | Below 5 % |
| Backlog age | Open P3 tickets older than 30 days | 0 |
| RCA on time | P1 RCA delivered within 5 business days | 100 % |
| Change success | Changes without back-out or incident / changes | 95 % |
| Availability | SLO of chapter 4 | 99.9 % |
| Restore tests | Planned restore tests done and passed | 100 % |

During the first three months after hypercare the operations review is weekly (Thursday, after the CAB).

# Part B - L1 troubleshooting guide {-}

# L1 troubleshooting guide

## How to use this guide

The L1 analyst finds the user's symptom in the tables below, makes the checks in order and gives the answer. If the checks do not solve the problem within 30 minutes, or the problem affects more than one user, the analyst escalates to L2 with the minimum content of section 5.2. The messages are quoted as BIBS shows them; words in angle brackets are filled in by BIBS.

<!-- table: widths=3.6,5.2,7.8 caption="Personas and their usual calls" bold=first size=8.5 -->
| Persona | Works in | Usual calls |
|---|---|---|
| Cashier | Cashiering, branches | Sign-in, receipt series, payment uploads, period closed |
| Remittance officer | Operations - Remittance | Remittance batch stuck in a stage, report not received, extraction not run |
| Collections officer | Operations - Collections | Worklist not refreshed, Collections files not published, item locked by another user |
| Account Officer and Marketing staff | Retail and Corporate Marketing | Access to menus, quotation or account workflow stuck, e-mail to client not received |
| Renewal processor | Renewal processing team | Renewal not extracted, letters not sent, exception bucket |
| Accountant | Comptrollership | Period closed, approval above limit, maker-checker, GL-SL difference |
| Business administrator | Each department | User access requests, lists of values, parameters |
| System administrator | BDOI IT | Locks, second-factor resets, jobs, integration events |

## Sign-in

<!-- table: widths=4.4,6.4,5.8 caption="Sign-in" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| "Invalid user name or password" | The same message is shown for a wrong password, a locked account and a deactivated account. In *Users*, check whether the account is locked (failed attempts) or deactivated (dormant user, leaver) | Locked: unlock (standard change) and tell the user to sign in through the BDOI single sign-on. Deactivated: a user access request is needed; route to the department's business administrator. Neither: the user uses the wrong user name, or the EIAM password; send to the BDO password self-service |
| Single sign-on page of EIAM opens but BIBS answers "Single sign-on is not available" or "The sign-in link is not valid" | Ask whether other users of the same branch can sign in; check *Home › Alerts* | One user: retry in a new browser window; if it remains, escalate to L2 (account not linked to the EIAM identity). Many users: P1 to L2 (EIAM or BIBS sign-in service) |
| "Too many sign-in attempts. Wait a minute and try again." | Several attempts from the same address within a minute | Ask the user to wait one minute. Many users at once behind one address: escalate to L2 |
| "The sign-in could not be checked. Try again in a moment." | Ask whether others are affected | One user: retry after a minute. Several users: P1 to L2 at once (the sign-in security store is unavailable; alert SECURITY_STORE_UNAVAILABLE) |
| "Your session has ended. Sign in again." | Session ended after 30 minutes without activity, after 8 hours, or because the same session was used from a copied browser profile | Sign in again. If it happens repeatedly within minutes, escalate to L2 (possible copied cookie; security check) |
| Second factor: "The code does not match. Check the time of your phone and retry" | The phone clock must be set automatically | Set the phone time to automatic and retry. Wrong codes count towards the lock |
| Second factor: phone lost or replaced | Only break-glass and privileged users with local sign-in use the BIBS second factor | The user signs in with a recovery code meanwhile. A system administrator requests the reset in *Second Factor*; a second administrator approves it; the user enrols again at the next sign-in |

## Access

<!-- table: widths=4.4,6.4,5.8 caption="Access" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| A menu or screen is missing | The menu shows only what the user's roles allow. Check the user's roles in *Users* or the *User Access Matrix* report | The user asks the department's business administrator to raise a user access request (BRD-11); it is approved and applied in BIBS, or provisioned through UIDM-ISC once live. Same role as a colleague who sees it: escalate to L2 |
| "You are not permitted to perform this action" | The action needs a permission that the role does not give, or the user is the maker of the record | Access request as above, or another user performs the action (maker-checker) |
| "You do not have access to the data of this company. Ask your administrator to extend your data access." | The user's data scope (companies and branches) does not include the company or branch of the record | Access request for the data scope; never shared credentials |
| New joiner cannot sign in on the first day | The user exists in EIAM but not yet in BIBS, or the access request has a future effective date | Check the request status; requests with an effective date are applied at 00:05 by UAM_EFFECTIVE_CHANGES. Escalate to L2 if the request is approved and past its date |
| User deactivated after leave | Users without a sign-in for the dormancy period are deactivated after a notice | Reactivation through a user access request |

## Workflow stuck

<!-- table: widths=4.4,6.4,5.8 caption="Workflow and approvals" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| "'<action>' is not allowed while <reference> is in stage <name>" | The record is in another stage than the user thinks; open the record's workflow history | Explain the stage and who acts next (the history shows the user's name); refresh the screen |
| "You are not allowed to '<label>'" | The action belongs to another role or approval level | Route to the right approver; access request if the user should have it |
| An item waits in an approver's queue for days | *My Approvals* of the approver; the item may be above the approver's authorisation limit, or the approver is on leave | Ask the approver or the department head to act or reassign; ageing items raise PENDING_APPROVAL_AGEING after 2 days |
| Maker-checker refusal, for example "A journal cannot be authorized by the user who submitted it" | The user tries to approve their own work | Another user with the approval right approves |
| "Journal total <amount> exceeds your authorization limit <limit>", or the item is not in the approver's inbox | The approval inbox hides items above the viewer's limit | A higher-limit approver approves |
| "The record was changed by another user. Please reload and try again." | Two users edited the same record | Reload the record and repeat the change |
| "<editing by> is editing <invoice no>" (Collections) | Another collector holds the item | Wait or ask the colleague; the lock ends when the colleague leaves the item |
| "Batch <batch no> is <stage>: lines can no longer change" (Remittance) | The batch has passed the editable stage | Return the batch through its workflow if allowed, otherwise escalate to L2 |

## Upload errors

<!-- table: widths=4.4,6.4,5.8 caption="Uploads (bulk uploads, payment uploads, journal and chart uploads)" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| "The file does not follow the current template; missing column(s): <missing>" | The file was made from an old template or columns were renamed | Download the current template from the upload screen and copy the data into it |
| "This file was already uploaded as <job no> (<file name>)" | The same file content was uploaded before | Open the earlier upload; if it was wrong, reverse it through the screen and upload the corrected file |
| "Upload an Excel (.xlsx), OpenDocument (.ods), CSV (.csv) or text (.txt) file" | Wrong file type, for example a password-protected or old Excel file | Save as Excel workbook or CSV and upload again |
| "The file has no header row" or "The file has no data rows" | Empty file or the header is not on the first row | Use the template as it is; do not add title rows above the header |
| "The file has <size> rows; the maximum is <max>" | File too large for one upload | Split the file |
| "The text file has no tab, pipe, semicolon or comma separated header line" | Bank or insurer text file in another layout | Check the layout set up for the channel (*Bank Statement Layouts*); escalate to L2 if the bank changed its layout |
| Upload accepted but some rows rejected | The rejection report lists each row with its reason | The user corrects the rows and uploads only them again |
| File cannot be opened after upload; status pending or quarantined | Every upload is scanned for malware; a file is downloadable only after a clean result (within 5 minutes) | Wait 5 minutes. Quarantined: do not send the file again; P2 to L2 and the Information Security Officer |

## Report not received

<!-- table: widths=4.4,6.4,5.8 caption="Reports and e-mails" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| A scheduled report did not arrive by e-mail | *My Subscriptions*: last run and result. A failed run is retried once, then the owner is notified | Run the report by hand from the Report Centre meanwhile; escalate to L2 if the run failed twice |
| Subscription suspended | The owner lost the report permission, so the subscription stops | Access request, then reactivate the subscription |
| "This e-mail domain is not allowed for reports" | External recipients are allowed only for shareable reports and allowed domains | Send to BDOI addresses; the Product Owner decides on new domains |
| "<file name> is available from <available from>" | The file is produced by a night job and not ready yet | Try again after the time shown |
| "Select between 1 and 30 reports" | A report batch has too many reports | Split the batch |
| E-mail to a client or insurer not received (quotation, slip, invoice, letter) | E-mails go through CCM; check whether other e-mails are late | One e-mail: check the address on the record and the spam folder. Many: P2 to L2 (mail queue or CCM) |
| Collections or renewal file not on the share by 06:00 | *Home › Alerts* (CLX_FILE_NOT_PUBLISHED) | P2 to L2 |

## Period closed

<!-- table: widths=4.4,6.4,5.8 caption="Accounting periods" bold=first size=8.5 -->
| Symptom or message | Checks | Answer or escalation |
|---|---|---|
| "Period <name> is <status> and does not accept this posting" | The posting or value date is in a closed or future period (*Financial Periods*) | Use a date in the open period. If the posting must be in the closed period, Comptrollership decides; L1 never asks for a period to be reopened |
| "No accounting period is defined for <date>" | The date is outside the fiscal years set up | Check the date; if correct, escalate to L2 for Comptrollership (period set-up) |
| "The <module> books of <period> are closed" | The broking books are cut off at month end while the GL stays open for adjustments | Post with a date in the new month; adjustments of the closed month go to Comptrollership |
| Month-end close does not complete | *Period-End & Year-End* checklist lists the blocking items; alert GL_CLOSE_FAILED | P1 on the last day of the month, otherwise P2, to L2 with Comptrollership |

## Unexpected error

When BIBS shows "The system could not complete the request. Try again; if it continues, contact support with the reference below.", L1 records the reference and the time, asks the user to try once more, and escalates to L2 if it happens again. Several users with the same error within 15 minutes is a P1 candidate.

# Decisions and open points for BDOI {-}

<!-- table: widths=1.5,8.4,4.2,2.6 caption="Decisions and open points" bold=first size=8.5 -->
| Ref | Point and our proposal | Owner | Needed by |
|---|---|---|---|
| OP-01 | Confirm the three levels: L1 BDO service desk with a BIBS queue, L2 BDOI application support, L3 iorta TechNXT, with BDO Cloud Operations as infrastructure resolver | Head, BDOI IT | 30 Nov 2026 |
| OP-02 | Name the ITSM tool, the paging route and the bridge channel used for BIBS | Head, BDOI IT | 31 Mar 2027 |
| OP-03 | Confirm the service window 06:00-22:00 Monday to Saturday plus month-end Sundays (DCR-133) and the maintenance window Sunday 00:00-04:00 (DCR-138) | BIBS Product Owner with the BRD owners | 16 Oct 2026 |
| OP-04 | Confirm the availability objective of 99.9 % in the service window (DCR-134) and the screen latency objective of 95 % within 2 seconds (DCR-137) | BIBS Product Owner | 16 Oct 2026 |
| OP-05 | Confirm the service levels of chapter 4 for the support agreement, and the L2 staffing (two analysts per shift, one on call) | Head, BDOI IT | 30 Jun 2027 |
| OP-06 | Confirm that L1 handles the second-factor and unlock requests of the break-glass and privileged users through L2 only | Information Security Officer | 30 Jun 2027 |
| OP-07 | Name the L2 key users per department and their back-ups; they are trained in the UAT window (Aug-Dec 2027) | Heads of department | 30 Jun 2027 |
| OP-08 | Confirm the release freeze periods (month-end, 15 December to 5 January, BIR deadlines) | Head, Comptrollership | 30 Jun 2027 |

# Glossary {-}

```glossary
CAB: Change Advisory Board of BIBS
EIAM: Enterprise Identity Access Management (Microsoft Entra ID)
ITSM: IT service management tool (tickets, problems, changes, knowledge)
L1, L2, L3: Support levels 1, 2 and 3
PHT: Philippine time (UTC+8)
RCA: Root-cause analysis
SLA: Service level agreement (targets for tickets)
SLO: Service level objective (targets for the platform)
UIDM-ISC: User ID Maintenance - Identity Security Cloud (identity governance)
Error budget: The share of the service window in which an objective may be missed in a month
```

# Sign-off {-}

```signoff
rows:
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "BIBS Service Owner", organisation: BDOI}
  - {name: "", role: "Cloud and Digital Operations Engineering", organisation: BDO Unibank IT}
  - {name: "", role: Support Manager, organisation: iorta TechNXT}
```
