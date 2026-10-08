---
# Operations Runbook and Observability of BIBS (programme level, BRD-00).
# Build: python docs/deliverables/src/programme/operations/build_operations_pack.py
# The scheduled jobs catalogue and the application exception codes are expanded from ops_data.yaml and the platform
# reference data by the build script (markers op:jobs and op:exceptions).
title: Operations Runbook and Observability
subtitle: Monitoring, alerts, dashboards, scheduled jobs, routine operations, standard procedures and incident playbooks of BIBS in production
doc_type: Operations Runbook
doc_code: Operations
brd: BRD-00
name: Operations Runbook and Observability
doc_id: BIBS-OPS-RUN-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Operations Runbook and Observability
output: Operations/BIBS_Operations_BRD-00_Runbook_and_Observability_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT DevOps Lead
    reviewer: iorta TechNXT Solution Architect; iorta TechNXT Service Delivery Lead
    approver: Head, BDOI IT (pending)
    change: First issue. Observability, alert catalogue, dashboards, the scheduled jobs catalogue read from the job registry of BIBS, routine operations, standard operating procedures and incident playbooks, on the component baseline confirmed by BDOI on 8 October 2026 (PostgreSQL 16, Valkey 8, Apache Kafka 3.9, OpenJDK 21, Spring Boot 3.5, nginx, managed Kubernetes with the Gateway API)
distribution:
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Operations model and tools"}
  - {name: "Cloud and Digital Operations Engineering", role: Approver, organisation: BDO Unibank IT, purpose: "Platform monitoring, backups, routine operations, playbooks"}
  - {name: "BIBS Service Owner and L2 lead", role: Reviewers, organisation: BDOI, purpose: "Daily operations, jobs, application alerts"}
  - {name: "Information Security Officer", role: Reviewer, organisation: BDOI, purpose: "Secrets, certificates, security alerts, log retention"}
  - {name: "Head, Comptrollership; Head, Operations", role: Reviewers, organisation: BDOI, purpose: "Business-critical jobs and their deadlines"}
  - {name: Project and support team, role: Delivery, organisation: iorta TechNXT, purpose: "L3 support, DevOps, releases"}
---

# Introduction

## Purpose

This runbook tells the people who run BIBS in production what to watch, what alarms them and what to do. It covers the observability of the platform (health checks, metrics, logs and traces), the alert catalogue with thresholds and responders, the dashboards, the catalogue of scheduled jobs, the routine operations (backups and restore tests, certificate, key and secret rotation, user housekeeping, database maintenance, log retention), the standard operating procedures and the incident playbooks.

## Audience

BDO Cloud Operations, BDOI application support (L2), iorta TechNXT support and DevOps (L3), and the BIBS Service Owner. The support levels, severities and service levels are those of the Support Model and Support Guide (R1).

## Scope

BIBS production in AWS ap-southeast-1 and its warm standby in the DR region; Pre-Prod where a procedure is rehearsed. Disaster recovery and business continuity are in the DR and BCP plan (R2); the steps of the go-live are in the Go-Live, Cutover and Data Migration Plan (R3).

## Reference documents

<!-- table: widths=1,7.4,8.2 caption="Reference documents" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| R1 | Support Model and Support Guide v1.0 | Levels, severities, SLA, SLO and error budget, escalation |
| R2 | Disaster Recovery and Business Continuity Plan v1.0 | RTO, RPO, failover and failback |
| R3 | Go-Live, Cutover and Data Migration Plan v1.0 | Technical cutover, job hold over the cutover weekend |
| R4 | Programme Alignment - Drops, Integrations and Infrastructure v1.0 | Component baseline of 8 October 2026, Kubernetes sizing, environments, IER changes |
| R5 | FRS BRD-00 Core Replacement v1.0, chapter Non-functional requirements | Response time, availability, retention, backup |
| R6 | BRD Discrepancy and Clarification Register v1.2 (DCR-133 to DCR-138, DCR-224 to DCR-228) | Service and maintenance windows, RPO and RTO, monitoring stack, Valkey mode |
| R7 | Document storage proposal (approved 26 September 2026) | S3 buckets, malware scanning, Object Lock, replication |
| R8 | IER workbook v20 (BDOI IT) | Environments, managed services, Dynatrace |

# Production landscape

## Workloads and services

BIBS is one application (a modular monolith on OpenJDK 21 and Spring Boot 3.5) delivered as one backend image that runs as three workloads, plus the web tier. Every workload is sized and monitored on its own.

<!-- table: widths=3.2,6.2,2.8,4.4 caption="BIBS workloads in production" bold=first size=8.5 -->
| Workload | Serves | Pods | Scaling and availability |
|---|---|---|---|
| bibs-web | The screens and the user services; no scheduled jobs, no event consumers | 3 to 10 | Autoscaled on CPU (65 %) and memory (80 %); at least one pod always available; spread over three availability zones |
| bibs-jobs | Scheduled and batch jobs (chapter 6): month-end, remittance and renewal extraction, reports, housekeeping | 2 | Fixed; the Valkey job lock runs each job once whatever the number of pods; 120-second grace at stop |
| bibs-integration | Integration event relay to Kafka, event consumers, inbound bank, insurer and watchlist files, the integration path called through Apigee X | 2 | Fixed; at least one pod always available; spread over zones |
| bibs-frontend | nginx serving the React web client over TLS | 2 to 4 | Autoscaled on CPU (70 %) |

<!-- table: widths=4,5.6,7 caption="Managed services and platform components" bold=first size=8.5 -->
| Component | Production set-up | Role for BIBS |
|---|---|---|
| Amazon RDS for PostgreSQL 16 | Multi-AZ, encrypted, TLS required, separate schema owner and least-privilege application login | All business data, audit trail, ledger, outbox of integration events |
| Amazon ElastiCache for Valkey 8 | Cluster mode disabled; primary and replica in two zones; TLS and authentication | Reference-data cache, job locks, token deny-list, sign-in counters |
| Amazon MSK, Apache Kafka 3.9 | Three brokers, TLS with SCRAM authentication | Outbound integration events (ten topics, each with a dead-letter topic) |
| Amazon S3 | Buckets documents, reports, inbound (quarantine until scanned), migration; SSE-KMS with BDOI keys; versioning; Object Lock (governance) | Attachments, generated documents, report files, uploads |
| Amazon EKS with the Gateway API | Managed Kubernetes; a Gateway API controller in place of ingress-nginx; network policies deny by default | Runs the four workloads |
| AWS WAF, internal load balancer, Route 53 private zone | Two paths: users from the BDO network; BDO systems through Apigee X | Edge security |
| Secrets store and KMS | Secrets of each environment; BDOI-owned keys | Keys, passwords, certificates |

## Environments

Six environments carry BIBS: DEV, SIT, UAT, Pre-Prod (production-sized; performance test and dress rehearsals), PROD and DR (R4). This runbook applies to PROD and DR; the procedures are rehearsed in Pre-Prod.

# Monitoring and observability

## Principles

- Every alert has an owner, a threshold, a severity and a playbook or procedure. An alert that nobody acts on is removed or changed at the monthly service review.
- Users should not be the first to report an outage: the synthetic sign-in check and the error-rate alerts fire before.
- One correlation id follows a request through the web tier, the backend, the log lines and the integration events it publishes; unexpected errors show the user a reference that is also in the log line.
- Personal data is never written to logs or metrics; events carry identifiers, statuses and amounts only.

![Observability: sources, signals, platforms and response](figures/ops_observability.dot){width=16}

## Health checks

<!-- table: widths=4,6.6,6 caption="Health checks" bold=first size=8.5 -->
| Check | What it proves | Used by |
|---|---|---|
| Liveness check of each backend pod | The application process answers; a failing pod is restarted | Kubernetes |
| Readiness check of each backend pod | The pod can take requests; a pod that is starting or stopping receives no traffic | Kubernetes and the load balancer |
| Health of the web tier | nginx serves the web client | Kubernetes and the load balancer |
| Detailed health on the management port | Database, Valkey and mail connections (Valkey down shows here without taking the pods out of service) | Monitoring namespace only |
| Synthetic sign-in and Dashboard read, every minute, from the BDO network | The service as a user sees it; the availability SLO | Dynatrace |
| Synthetic integration check through Apigee X, every 5 minutes | The system path is open (connectivity check of the integration workload) | Dynatrace |

## Metrics

BIBS publishes its metrics on a management port that the load balancer never exposes and that only the monitoring namespace can reach.

<!-- table: widths=3.6,8,5 caption="Metrics" bold=first size=8.5 -->
| Group | Metrics | Source |
|---|---|---|
| Requests | Rate, latency percentiles and errors per screen service, per workload | BIBS (Spring Boot metrics) |
| JVM | Heap and non-heap memory, garbage collection pauses, threads | BIBS |
| Database pool | Active, idle and pending connections, wait time | BIBS |
| Business counters | Journals posted; failures of the sign-in security store (per store) | BIBS |
| Jobs and events | Last run and result of each scheduled job; outbox rows pending and failed; dead letters | BIBS screens and metrics |
| Kubernetes | Pod restarts, CPU and memory against limits, ephemeral storage, autoscaler replicas, node status | EKS, metrics server |
| Managed services | RDS CPU, memory, storage, connections, replica lag, latest restorable time; Valkey memory, evictions, connections; MSK disk, under-replicated partitions; S3 replication latency | CloudWatch |
| Edge | Load balancer 5xx and latency, WAF blocked requests | CloudWatch |

## Logs

- Each pod writes one line per event to standard output, with an ISO timestamp, the level, the thread, the logger and the message; line breaks inside messages are neutralised so a log line cannot be forged.
- The correlation id of the request is on every line of that request; an unexpected error writes its reference on the error line.
- Passwords, tokens and second-factor codes are never logged; personal data is not logged.
- Logs are shipped to the BDO log platform and the security events to the BDO SIEM. Retention follows the umbrella NFR: application, system and audit logs 5 years online and 15 years in archive (R5). The audit trail of BIBS itself is kept in the database, insert-only, and is not purged.

## Traces

Dynatrace traces each request from the web tier through the backend to the database, Valkey, Kafka and the calls through Apigee X, so L3 can see where the time of a slow request is spent. Sampling is 100 % of failed requests and 10 % of the others in production (proposal; OP-03).

<!-- landscape -->

# Alert catalogue

## Platform and service alerts

The thresholds below are the starting values; they are tuned after the performance test and during hypercare. Severity follows the Support Model (P1 to P4). Responder codes: CO = BDO Cloud Operations on-call; L2 = BDOI application support; L3 = iorta TechNXT on-call.

<!-- table: widths=1.4,4.2,6.6,1.3,2.6,3.4,4.2 caption="Platform and service alert catalogue" size=7.5 bold=first -->
| ID | Alert | Condition and threshold | Sev. | First responder | Escalate to | Playbook or procedure |
|---|---|---|---|---|---|---|
| AL-01 | Service down | Synthetic sign-in fails 3 times in a row (3 minutes), or no ready bibs-web pod | P1 | CO and L3 (paged) | L2 lead, Service Owner | PB-01 to PB-03, PB-07 |
| AL-02 | Server error rate high | 5xx answers above 2 % of user requests over 5 minutes (P2); above 10 % (P1) | P1 / P2 | L3 | CO | PB-07 |
| AL-03 | Screen latency high | 95th percentile above 2 s for 15 minutes in the service window (P3); above 5 s (P2) | P2 / P3 | L3 | CO | PB-07 |
| AL-04 | Pod restarts | More than 3 restarts of one workload in 15 minutes | P2 | L3 | CO | PB-07 |
| AL-05 | Autoscaler at maximum | bibs-web at 10 pods for 30 minutes | P3 | CO | L3 | PB-07 |
| AL-06 | JVM memory | Heap above 85 % after garbage collection for 10 minutes | P3 | L3 | - | PB-07 |
| AL-07 | Database pool saturated | Pending connections above 0 for 5 minutes, or active above 90 % of the pool | P2 | L3 | CO | PB-07 |
| AL-08 | Database unavailable | RDS instance not available, or failover event | P1 | CO | L3 | PB-01 |
| AL-09 | Database CPU | Above 80 % for 15 minutes | P3 | CO | L3 | PB-07 |
| AL-10 | Database storage | Free storage below 20 % (P3), below 10 % (P2) | P2 / P3 | CO | L3 | PB-06 |
| AL-11 | DR replica lag | Cross-region replica lag above 5 minutes (P3), above 15 minutes (P2: RPO at risk) | P2 / P3 | CO | Service Owner | DR plan |
| AL-12 | Backup | Latest restorable time older than 15 minutes, or a backup job failed | P2 | CO | Service Owner | SOP-11 |
| AL-13 | Long-running query | A query running longer than 5 minutes in the service window | P3 | L3 | CO | PB-07 |
| AL-14 | Sign-in security store unavailable | Any increase of the security store failure counter; application alert SECURITY_STORE_UNAVAILABLE | P1 | L3 and CO | L2 lead | PB-03 |
| AL-15 | Valkey unavailable or memory | Valkey not reachable (P1); memory above 80 % or evictions above 0 (P3) | P1 / P3 | CO | L3 | PB-03 |
| AL-16 | Kafka unavailable | Broker not reachable, or under-replicated partitions above 0 for 10 minutes | P2 | CO | L3 | PB-02 |
| AL-17 | Integration events delayed | Outbox rows pending older than 15 minutes, or more than 500 pending | P2 | L2 | L3, CO | PB-02 |
| AL-18 | Integration events failed | Any outbox row in FAILED, or a new dead letter | P3 | L2 | L3 | SOP-05 |
| AL-19 | E-mail delayed | Queued e-mail older than 15 minutes, or more than 50 failed in an hour | P2 | L2 | BDOI IT (CCM owner) | PB-05 |
| AL-20 | Job failure | Application alert JOB_FAILURE: class A job (P2; P1 when the business deadline is the next morning), class B (P3), class C (P4) | P1 to P4 | L2 | L3 | SOP-03; chapter 6 |
| AL-21 | Job missed | A class A job without a successful run 30 minutes after its scheduled time | P2 | L2 | L3 | SOP-03 |
| AL-22 | Single sign-on errors | More than 5 single sign-on refusals of the provider in 5 minutes | P1 | L2 | BDOI IT (EIAM owner), L3 | PB-04 |
| AL-23 | Sign-in failures | More than 50 failed sign-ins in 5 minutes, or more than 10 locked accounts in an hour | P2 | L2 | Information Security Officer | Support Guide 12.2 |
| AL-24 | Integration path errors | Calls through Apigee X to BIBS with errors above 5 % over 10 minutes | P2 | L2 | BDOI API team, L3 | PB-05 |
| AL-25 | File quarantined | Application alert FILE_QUARANTINED | P2 | L2 | Information Security Officer | Support Guide 12.5 |
| AL-26 | Document replication delayed | S3 replication latency above 15 minutes | P2 | CO | Service Owner | DR plan |
| AL-27 | Certificate expiry | Any certificate within 30 days of expiry (P3); within 10 days (P2: automatic renewal failed) | P2 / P3 | CO | L3 | SOP-08 |
| AL-28 | Node or disk pressure | Node not ready, or ephemeral storage of a pod above 80 % of its limit | P2 | CO | L3 | PB-06 |
| AL-29 | WAF blocking spike | Blocked requests more than 5 times the hourly average | P3 | CO | Information Security Officer | - |
| AL-30 | Financial integrity | Application alerts UNBALANCED_TB, MIG_CLEARING_NOT_ZERO, MIG_TRUEUP_BREAK, NEGATIVE_CASH_BALANCE (CRITICAL) | P1 | L2 | Comptrollership, L3 | Business alert table below |

## Application exception codes

BIBS raises business and control alerts from its exception codes (*Setup & Administration › Administration › Exception Codes*), evaluated by the ALERT_DAILY_CHECKS job, by the jobs themselves and at posting. They appear under *Home › Alerts* for the holders of the alert rights and are e-mailed to the recipients of the code. The thresholds are maintained on the same screen. L2 watches the CRITICAL and HIGH codes daily; the business owner acts on the alert.

<!-- op:exceptions -->

## Routing rules

- P1 alerts page the first responder and open a P1 ticket; the L2 lead is informed at once (Support Guide, major incident).
- P2 and P3 alerts open a ticket in the queue of the first responder; P4 alerts are reviewed in the daily operations check.
- An alert that clears by itself within 5 minutes is kept on the dashboard but does not open a ticket, except AL-01, AL-08 and AL-14.
- Alerts are not suppressed in the maintenance window; the release ticket lists the alerts expected during the deployment.

<!-- portrait -->

# Dashboards

<!-- table: widths=3.6,8.4,4.6 caption="Dashboards" bold=first size=8.5 -->
| Dashboard | Panels | Audience |
|---|---|---|
| Service overview | Availability this month and error budget left; synthetic check; requests, 95th percentile latency and error rate per workload; open P1 and P2 | Service Owner, L2, L3, Cloud Operations |
| Platform | Pods per workload and restarts; CPU and memory against limits; autoscaler; nodes; Gateway and WAF | Cloud Operations, L3 |
| Database | CPU, memory, connections, pool usage, slow queries, storage, replica lag, latest restorable time | Cloud Operations, L3 |
| Cache and events | Valkey memory and evictions; outbox pending and failed by topic; dead letters; Kafka under-replicated partitions | L2, L3, Cloud Operations |
| Batch | Last night's jobs by class with status and duration against the 06:00 deadline; job failures this week | L2, business process owners |
| Business operations | Receipts issued and payments applied per hour; remittance batches by stage; renewal candidates by bucket; e-mails sent and failed | L2, Heads of Operations and Renewal |
| Security | Sign-ins and failures, locked accounts, single sign-on errors, second-factor resets, privileged changes, quarantined files, legacy archive exports | Information Security Officer, L2 |
| SLO and service report | Monthly SLO results, error budget burn, SLA by severity | Service Owner, monthly service review |

# Scheduled jobs

## How jobs run

- Each job has a name, a schedule and a workload. Jobs of the workload *jobs* run on bibs-jobs; jobs that move data to or from another system run on bibs-integration. A job runs once per schedule whatever the number of pods: the other pod records the run as skipped because the job is locked (this is normal).
- When Valkey is unavailable, a job does not run without its lock: the run is recorded as failed with "Job lock unavailable" and the JOB_FAILURE alert is raised (playbook PB-03).
- Every run is recorded with its start, end, result and counts in *Setup & Administration › Administration › Scheduled Jobs*, where L2 can also start a job by hand. The jobs of a module that is switched off do not run.
- Schedules are Philippine time (PHT). They are set per environment; non-production environments move the night jobs into their working hours (DCR-226).
- A job that failed is rerun once the cause is fixed. Jobs that take dates (renewal extraction, period closes) also catch up the missed dates at their next run.

## Night batch timeline

<!-- table: widths=2.8,13.8 caption="Night batch on a business day (PHT)" bold=first size=8.5 -->
| Time | Jobs |
|---|---|
| 18:00 | ADJ_DAILY_REPORT |
| 20:00 | BOOKING_BATCH, REMITTANCE_EXTRACTION, ACSL_GL_SL_RECON |
| 21:30-23:00 | SBM_PROCESSING 21:30, SBM_EXPIRY_SCAN 22:00, CLX_DAILY_REFRESH 22:15, CLX_DAILY_FILES 22:30, CLX_PROMISE_CHECK 22:45, CLX_ESCALATION 23:00 (last day of the month: BROKING_BOOKS_CLOSE 23:00) |
| 00:05-00:30 | JOURNAL_AUTO_REVERSAL, UAM_EFFECTIVE_CHANGES 00:05; RNW_EXPIRY_SWEEP, UAM_DORMANT_USERS 00:15; DISB_CHECK_STALE 00:20 (1st of the month: BOOK_RATE_FROM_CLOSING 00:30) |
| 01:00-02:00 | RNW_EXTRACTION, SCR_WATCHLIST_INGEST, PACKAGE_EXPIRY_MONITOR, PACKAGE_VERSION_LIFECYCLE 01:00; RNW_REEVALUATE, SCR_PERIODIC_SCREENING 01:30; MIG_STAGING_PURGE 02:00 |
| 04:00-05:30 | MINIMAL_BALANCE_SWEEP 04:00; CLX_APPLICATION_FILE, CLX_MONTHLY_FILES 05:00; BCL_PREMIUM_RECHECK 05:30 |
| 06:00-07:00 | Letters and reminders: RNW_NRNS_LETTERS, EB_RENEWAL_ADVICE, BCL_FOLLOW_UP_DUE, BCL_AGEING_ALERTS, PASSWORD_EXPIRY_NOTICE 06:00; SBM_LETTER_DISPATCH, EB_FRANCHISE_EXPIRY 06:30; EB_ITEM_FOLLOWUP, SBM_HOLD_COVER_WATCH, SCR_INGEST_ERROR_DIGEST 07:00 |

The planned maintenance window (Sunday 00:00-04:00) overlaps the 00:05 to 02:00 jobs. A release in the window holds those jobs and runs them after the smoke test, in the order of the table (SOP-01).

<!-- landscape -->

## Catalogue

The catalogue lists every scheduled job of BIBS as configured for production. Class A jobs protect a business deadline or a financial position (a failure is P2, and P1 when it blocks the next business morning); class B jobs do the daily operational work (P3, rerun the same day); class C jobs are housekeeping (P4, rerun at the next run). The jobs of the insurer-company modules, which BIBS does not use, are not listed.

<!-- op:jobs caption="Scheduled jobs of BIBS in production" -->

<!-- portrait -->

# Routine operations

## Calendar

<!-- table: widths=2.6,9.4,4.6 caption="Routine operations calendar" bold=first size=8.5 -->
| When | Task | Owner |
|---|---|---|
| Daily 07:00 | Daily operations check (section 7.2) | L2, with CO for the platform lines |
| Daily | Review of P4 alerts and of the CRITICAL and HIGH application alerts | L2 |
| Weekly (Thursday) | Operations review after the CAB: incidents, problems, error budget, capacity, alerts to tune | Service Owner |
| Weekly | Database health review: slow queries, dead rows, index use, storage growth | L3 with CO |
| Monthly | Restore spot check (point-in-time restore of yesterday into Pre-Prod, BIBS started, counts compared) | CO |
| Monthly | Privileged access review; break-glass seal check; second-factor resets review | Information Security Officer, L2 |
| Monthly | Capacity review: CPU, memory, storage, Kafka disk, log volume against the 15 % yearly growth of the IER | CO, L3 |
| Monthly | Security advisories of the components; patch plan for the next window | L3 |
| Quarterly | Full restore test of production into an isolated instance (SOP-11) | CO |
| Quarterly | User access review by the department heads (User Access Reports) | Information Security Officer |
| Quarterly | Contact annex and on-call rosters checked by a call test | Service Owner |
| Twice a year | DR component drill (DR plan) | CO, L3 |
| Yearly | Regional failover drill (DR plan); key rotation of the signing and second-factor keys; component baseline review | CO, L3, Information Security Officer |

## Daily operations check

Recorded every day (date, checker, result, tickets raised):

1. Night batch: every class A job of the night succeeded and finished before 06:00; reruns started for any failure.
2. *Home › Alerts*: new CRITICAL and HIGH codes assigned to their owners.
3. *Integration Events*: no FAILED outbox row; no new dead letter; pending rows not older than 15 minutes.
4. E-mail queue: nothing queued for more than 15 minutes.
5. Dashboards: availability and latency of the previous day; error budget left; no open platform alert.
6. Backups: latest restorable time within 15 minutes; last night's snapshots present; DR replica lag below 5 minutes; S3 replication within 15 minutes.
7. Certificates: none within 30 days of expiry.
8. Security dashboard: no unusual sign-in failures, lockouts or legacy archive exports.

## Backups and restore tests

<!-- table: widths=3.6,8.4,4.6 caption="Backup scheme of production (proposal)" bold=first size=8.5 -->
| Item | Scheme | Retention |
|---|---|---|
| Database: continuous log archiving | RDS automated backups with point-in-time recovery to any second | 35 days |
| Database: snapshots | Every 4 hours (00:00, 04:00, 08:00, 12:00, 16:00, 20:00) by the BDO backup plan; copied to the DR region | 35 days |
| Database: monthly copy | Snapshot of the first day of each month, copied to the DR region | 7 years (umbrella backup NFR, DCR-136) |
| Database: before a release or a data correction | Manual snapshot | 90 days |
| Documents and reports (S3) | Versioning; cross-region replication with replication time control; Object Lock in governance mode on the documents bucket | Per the retention rules of each record class |
| Configuration and secrets | Kubernetes manifests and release packages in the BDO release store; secrets in the secrets store, replicated to the DR region | Current and previous two releases |

**Restore test (quarterly, SOP-11).** A production snapshot is restored into an isolated instance in Pre-Prod, BIBS is started against it with the masking applied for non-production use, and the row counts of the key records (clients, invoices, receipts, journals, users) are compared with production at the snapshot time. The test records the backup used, the recovery point, the duration (target: below 2 hours) and any issue. A failed test is a P2 incident.

## Certificate and key rotation

<!-- table: widths=3.8,5.2,7.6 caption="Certificates and keys" bold=first size=8.5 -->
| Item | Rotation | Procedure |
|---|---|---|
| Browser certificate (BDO domain, BDO corporate PKI) | At the validity set by the BDO PKI; renewal started 30 days before expiry | CO requests the certificate, installs it on the load balancer listener in the maintenance window or without downtime; AL-27 watches it |
| In-cluster certificates (private CA) | Every 90 days, automatically, 15 days before expiry, with a new key each time | The backend reloads the new certificate by itself; the web tier picks it up at a restart (SOP-08) |
| Database CA bundle | When AWS rotates the RDS certificate authority | CO updates the CA bundle of the cluster before the RDS certificate change; rolling restart |
| KMS keys of the buckets | Automatic yearly rotation, or BDOI's key policy | BDOI security staff; no BIBS action |
| Token signing key | Yearly, and at once on a suspected leak | SOP-09; every user's current access token is refused and renewed once from the refresh cookie, so users keep working |
| Second-factor encryption key | Yearly, and on a suspected leak | SOP-09: new key set with the previous key kept; each enrolled secret is re-encrypted at its next use; the previous key is removed when every enrolled user has signed in, or those users are reset |

## Secret rotation

<!-- table: widths=4.2,2.8,9.6 caption="Secrets of production" bold=first size=8.5 -->
| Secret | Rotation | How |
|---|---|---|
| Database logins (schema owner and application login) | Every 90 days | New password in the secrets store, then a rolling restart of the three backend workloads; the old password is revoked after the restart |
| Valkey authentication token | Every 90 days | ElastiCache token rotation keeps the old and the new token valid together; set the new token, rolling restart, then remove the old token |
| Kafka credentials (SCRAM) | Every 90 days | New credential on MSK, new secret, rolling restart of bibs-integration and bibs-jobs, old credential removed |
| Single sign-on client secret (EIAM) | 30 days before the expiry set in Entra ID (at most 24 months) | BDOI IT issues the new secret; secret updated; rolling restart of bibs-web; tested with one user per persona |
| Mail credentials (CCM), when CCM requires them | Every 90 days or the CCM policy | As the database logins |
| Break-glass administrator passwords | After each use and every 90 days | Changed by the Information Security Officer; new sealed envelope |
| Migration masking key (non-production) | Never during the migration cycles: a new key would break repeatable masking between trial migrations | Destroyed with the non-production staging data after hypercare exit |

Secrets are only in the secrets store of each environment; they are never written in a ticket, an e-mail, a document or a file. Production refuses to start with a missing, weak or development value.

## User housekeeping

- **Dormant users**: UAM_DORMANT_USERS deactivates users who have not signed in for the dormancy period, after a notice (daily, 00:15).
- **Leavers and movers**: through an approved user access request in BIBS, or provisioned from UIDM-ISC once that interface is live; disabled in EIAM by HR offboarding the same day.
- **Access reviews**: quarterly by the department heads from the User Access Reports; monthly for privileged roles and the break-glass accounts.
- **Sessions**: idle, expired, locked and disabled users' sessions are ended every 15 minutes by USER_SESSION_SWEEP; a session ends after 30 minutes without activity and after 8 hours in all.
- **Second-factor resets**: requested by one administrator and approved by another; reviewed monthly.
- **Migration roles**: removed at hypercare exit (Data Migration Handbook, data security).

## Database maintenance

<!-- table: widths=4,12.6 caption="Database maintenance" bold=first size=8.5 -->
| Task | Standard |
|---|---|
| Vacuum and statistics | PostgreSQL autovacuum on; dead rows and the last vacuum of the large tables reviewed weekly; statistics refreshed after the migration loads of the cutover and after bulk loads |
| Indexes | Unused and bloated indexes reviewed monthly by L3; changes go through a release |
| Slow queries | Queries above 2 seconds reviewed weekly from Performance Insights; AL-13 for queries above 5 minutes |
| Storage | Storage autoscaling on with a ceiling of twice the planned size (2,000 GB in the IER for production); AL-10 |
| Connections | Pool size per pod times pods below 80 % of the database connection limit at the maximum autoscaled size |
| Minor versions | Applied by CO in the maintenance window after SIT and UAT, at most one minor version behind |
| Audit trail and ledger | Insert-only; never deleted or updated. Archiving of old audit entries is a recorded change by the schema owner (DBA), not a routine task |
| Schema changes | Only through BIBS releases; backward compatible with the previous release |

## Log retention

<!-- table: widths=4,4.6,8 caption="Retention of logs and records" bold=first size=8.5 -->
| Record | Retention | Where |
|---|---|---|
| Application and system logs | 5 years online, 15 years in archive (umbrella NFR; exceptions after CRQ22) | BDO log platform; archive tier |
| Security events | As the BDO SIEM standard, at least the application log retention | BDO SIEM |
| Audit trail of BIBS | Not purged; archive by the retention rules per record type | BIBS database |
| Legacy archive access log | Not changeable; with the archive | BIBS database |
| Job run history and integration events | Delivered outbox rows 30 days; archived events 400 days; job runs as the audit trail | BIBS database (EVENT_HOUSEKEEPING) |
| Metrics | 13 months | Monitoring platform |

# Standard operating procedures

Each procedure is recorded in the ticket or change with the operator, the time and the result.

<!-- table: widths=1.4,3.6,9.4,2.2 caption="Standard operating procedures" bold=first size=8 -->
| ID | Procedure | Steps | Who |
|---|---|---|---|
| SOP-01 | Planned release | 1. Change approved; release notes sent 5 business days ahead. 2. Manual database snapshot. 3. Hold the 00:05-02:00 jobs if the window overlaps them (scale bibs-jobs to zero). 4. Rolling update of the four workloads with no pod down at any time; database changes apply at the start of the first new pod. 5. Smoke test by L2 and key users within 30 minutes. 6. Release the jobs and run the held ones in timeline order. 7. Close the change | L3, CO, L2 |
| SOP-02 | Back-out of a release | Redeploy the previous release package (rolling); no database restore, as the database changes are backward compatible; smoke test; problem record opened | L3, CO |
| SOP-03 | Rerun a failed job | Read the failed run in *Scheduled Jobs*; fix the cause (data, configuration, platform); start the job with Run now; check the result and counts; for class A jobs confirm with the business owner before the deadline | L2 (L3 for platform causes) |
| SOP-04 | Clear a cache | After a change made outside the screens (only by an approved data correction): *Caches*, clear the cache concerned, never all Valkey keys; the job locks and the token deny-list stay | L2 |
| SOP-05 | Resend integration events and dead letters | *Integration Events*: FAILED rows, read the error, fix the cause, Send Again; dead letters: Retry after the fix, or Discard with the reason in the incident | L2 (L3 when the cause is in BIBS) |
| SOP-06 | Unlock a user or reset a second factor | Unlock in *Users* (standard change); second-factor reset requested in *Second Factor* by one administrator and approved by another | L2 |
| SOP-07 | Restart a workload | Rolling restart of the workload; for bibs-jobs check that no class A job is running (the 120-second grace lets a job finish its unit of work); jobs interrupted are rerun | CO, L3 |
| SOP-08 | Renewed in-cluster certificate for the web tier | Rolling restart of bibs-frontend after cert-manager renewed its certificate (or the configuration reloader, when installed); check the certificate dates | CO |
| SOP-09 | Rotate the signing or the second-factor key | Normal change in the maintenance window; new key in the secrets store (second factor: previous key kept as previous key); rolling restart; sign-in test; for the second factor remove the previous key later (section 7.4) | CO, L3, Information Security Officer |
| SOP-10 | Scale the web workload ahead of a known peak | Raise the minimum pods of bibs-web (for example to 5 for month-end and the January renewal peak); lower it after | CO |
| SOP-11 | Restore test or point-in-time copy to Pre-Prod | Restore the snapshot or point in time into an isolated instance; apply masking for non-production use; start BIBS; compare counts; delete the copy after use (a dry run of a data correction uses the same steps) | CO, L3 |
| SOP-12 | Maintenance window | Announce 5 business days ahead (L1 notice to users); hold jobs as needed; open the bridge; work; smoke test; close the window and release the jobs | Service Owner, CO |

# Incident playbooks

Each playbook gives the signs, what users see, the first actions, the diagnosis, the recovery and the checks before the incident is closed. A P1 always follows the major incident steps of the Support Guide (bridge within 15 minutes, user notice within 30 minutes). A P1 that cannot be restored within 2 hours in the primary region goes to the DR assessment of the DR and BCP plan.

## PB-01 Database down

<!-- table: widths=3.2,13.4 caption="PB-01 Database down" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-08, AL-01; readiness of the backend pods fails; every screen action fails with the unexpected-error message |
| Impact | BIBS unavailable for all users; jobs fail; integration events wait (the outbox is in the database, so nothing is lost) |
| First actions | CO checks the RDS event log. A Multi-AZ failover completes by itself in about 1 to 2 minutes: the application reconnects without a restart; watch AL-01 clear |
| Diagnosis | Failover in progress, instance stopped, storage full (PB-06), parameter change, network or security group change, credentials rotated without restart |
| Recovery | Failover done: rolling restart only if pods keep failing to connect. Storage full: PB-06. Corruption or a wrong mass change: point-in-time recovery to a new instance to just before the event (DR plan, scenario 2). Region-level loss: DR failover |
| Checks | Synthetic check green; one transaction per module; jobs that failed during the outage rerun (SOP-03); outbox drained |
| Communication | L1 notices at start, every hour and at restore; RCA owed (P1) |

## PB-02 Kafka unavailable

<!-- table: widths=3.2,13.4 caption="PB-02 Kafka unavailable" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-16, AL-17; EVENT_OUTBOX_RELAY runs record send errors |
| Impact | Users and business transactions are not affected: transactions only write the outbox. Outbound events wait; they are retried with back-off and set to FAILED after 10 attempts. E-mails wait for the consumer, but MAIL_DISPATCH still sends queued e-mail every 2 minutes |
| First actions | CO checks MSK health, broker disk, the security group and the credentials |
| Diagnosis | Broker down or disk full, credentials rotated without restart, network policy, certificate |
| Recovery | Once Kafka answers, PENDING rows are sent by themselves; FAILED rows are resent from *Integration Events* (SOP-05) |
| Checks | Outbox pending back to normal; no FAILED row; the receiving systems confirm the events of the period |
| Severity | P2; P1 only if the outage stops a business deadline (for example the ECM archive of final records before an audit) |

## PB-03 Valkey unavailable

<!-- table: widths=3.2,13.4 caption="PB-03 Valkey unavailable" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-15; AL-14 and the application alert SECURITY_STORE_UNAVAILABLE; job runs FAILED with "Job lock unavailable" (AL-20) |
| Impact | Screens keep working but slower (the reference-data cache falls back to the database). Sign-ins are checked against the database session log; if that also cannot be read, sign-ins are refused with "The sign-in could not be checked. Try again in a moment." Scheduled jobs do not run while the lock store is down. Sign-out may fail on the server (the browser still signs the user out) |
| First actions | CO checks ElastiCache: failover to the replica normally completes within a minute. Nothing needs to be restarted in BIBS once Valkey answers |
| Diagnosis | Node failover, memory full (evictions), authentication token rotated without restart, network policy |
| Recovery | Valkey back: caches refill on use. Rerun the jobs that failed during the outage in timeline order (SOP-03). If the replica does not take over within 30 minutes, CO creates a new Valkey cluster with the same settings and updates its address (rolling restart): Valkey holds no data that must be restored, only caches, locks and sign-in counters. Sign-outs made before the outage are forgotten, so their access tokens stay usable until they expire (at most 8 hours); the Information Security Officer is informed |
| Checks | AL-14 and AL-15 clear; a sign-in and a sign-out succeed; all class A jobs of the night succeeded |

## PB-04 EIAM unavailable

<!-- table: widths=3.2,13.4 caption="PB-04 EIAM unavailable" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-22; users report that the single sign-on page does not open or returns an error; the synthetic sign-in fails while the rest of the service is healthy |
| Impact | New sign-ins fail. Users already signed in keep working until their session ends (30 minutes without activity, 8 hours in all). Break-glass administrators can sign in with their password and second factor |
| First actions | L2 informs BDOI IT (EIAM owner) and checks the Microsoft Entra ID service status; L1 tells users not to sign out |
| Diagnosis | Entra ID outage, expired client secret (AL-27 should have warned), changed redirect address, clock difference, network path to Entra ID |
| Recovery | Expired client secret: new secret, rolling restart of bibs-web. Entra ID outage: wait for the provider; after 1 hour in the service window the departments start their continuity procedures (DR and BCP plan, chapter 10) |
| Checks | Sign-in of one user per persona; AL-22 clear |
| Note | BIBS does not fall back to local passwords for business users: local passwords exist only for the break-glass administrators |

## PB-05 Integration failure

<!-- table: widths=3.2,13.4 caption="PB-05 Integration failure" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-24 (calls through Apigee X), AL-17 and AL-18 (outbound events), AL-19 (e-mail through CCM), application alerts of the module (for example OPS_FLOW_IN_FAILED, SBM_INTAKE_FAILED, SCR_INGEST_FAILED, CLX_FILE_NOT_PUBLISHED, CSF_SYNC_FAILED) |
| Impact | Depends on the interface (Integration Inventory): inbound files wait or are refused as a whole; outbound events and e-mails wait in their queues; no business transaction is lost in BIBS |
| First actions | L2 identifies the interface and checks the last good run; informs the owner of the other system; the connectivity check through Apigee shows whether the path is open |
| Diagnosis | Other system down; Apigee credentials or scopes; certificate; file layout changed by the other party; a record refused by business rules |
| Recovery | Path restored: queued items are sent by themselves; refused files are corrected by the sender and loaded again; failed events resent (SOP-05); a layout change of a bank or insurer is a normal change |
| Checks | Totals of the files and events of the outage window agreed with the other system's owner |

## PB-06 Disk full

<!-- table: widths=3.2,13.4 caption="PB-06 Disk or storage full" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-10 (database storage), AL-28 (node or pod ephemeral storage), MSK broker disk (AL-16) |
| Database | Storage autoscaling extends the volume up to its ceiling. Near the ceiling: CO raises the ceiling as an emergency change; L3 looks for the cause (unexpected growth of a table, large bulk load, migration staging not purged: MIG_STAGING_PURGE_OVERDUE) |
| Pods | A pod over its ephemeral-storage limit is evicted and replaced; repeated evictions point to large temporary files (report rendering, uploads): L3 investigates; the limit can be raised in the next release |
| Kafka | Retention of the topics checked; broker storage extended by CO |
| Checks | Free space back above 30 %; the growth explained in the problem record |

## PB-07 Performance degradation

<!-- table: widths=3.2,13.4 caption="PB-07 Performance degradation" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-03, AL-02, AL-05, AL-07, AL-09, AL-13; users report slow screens |
| First actions | L3 opens the service dashboard and the traces of the slow requests: is the time in the database, in a call to another system, in the JVM, or at the edge? |
| Diagnosis | A heavy report or export at peak time; a slow query (missing index, plan change after a large load); pool saturation; garbage collection pauses; autoscaler at maximum; a night job overrunning into the morning; a noisy neighbour on the nodes |
| Recovery | Scale bibs-web (SOP-10); stop the runaway report run and ask the user to schedule it; refresh the statistics of the table concerned; move an overrunning job; restart a pod with memory trouble (SOP-07); fix through a release when the cause is in BIBS |
| Checks | 95th percentile back below 2 s for 30 minutes; error budget impact recorded |

## PB-08 Night batch failure

<!-- table: widths=3.2,13.4 caption="PB-08 Night batch failure" bold=first size=8.5 -->
| Step | Action |
|---|---|
| Signs | AL-20, AL-21; the batch dashboard shows a class A job failed or late |
| First actions | L2 on call reads the run; if the cause is data or configuration, fixes it through the screens and reruns (SOP-03); if the cause is the platform or BIBS, pages L3 |
| Deadlines | Files for the bank and Collections by 06:00 (CLX_APPLICATION_FILE, CLX_DAILY_FILES); renewal extraction before the Renewal team starts at 08:00; remittance batches before Remittance starts at 08:00; month-end close jobs on the last day |
| Communication | When a deadline will be missed, L2 informs the business owner before 07:00 with the expected time |

# Decisions and open points for BDOI {-}

<!-- table: widths=1.5,8.6,4,2.5 caption="Decisions and open points" bold=first size=8.5 -->
| Ref | Point and our proposal | Owner | Needed by |
|---|---|---|---|
| OP-01 | Confirm Dynatrace (named in the IER) as the APM, with CloudWatch and the BDO log platform; name the SIEM and the paging tool (DCR-224) | Head, BDOI IT | 31 Jan 2027 |
| OP-02 | Confirm that BDO Cloud Operations runs the platform in production (AWS account, managed services, backups, DR) with iorta TechNXT DevOps on L3 for the application workloads | Head, BDOI IT | 30 Nov 2026 |
| OP-03 | Confirm trace sampling of 100 % of failed requests and 10 % of the others | Cloud and Digital Operations Engineering | 30 Jun 2027 |
| OP-04 | Confirm the backup scheme: point-in-time recovery 35 days, snapshots every 4 hours, monthly copies kept 7 years in the DR region (DCR-136) | Head, BDOI IT with Compliance | 31 Mar 2027 |
| OP-05 | Confirm the rotation periods of section 7.5 against the BDO password and key policy | Information Security Officer | 31 Mar 2027 |
| OP-06 | Confirm the log retention of 5 years online and 15 years in archive for application and system logs (CRQ22) | Compliance | 31 Mar 2027 |
| OP-07 | Agree the alert thresholds of chapter 4 as starting values, to be tuned after the performance test (November-December 2027) | BIBS Service Owner | 15 Nov 2027 |
| OP-08 | Accept that a Valkey cluster that cannot recover is re-created empty (PB-03), with sign-outs made before the outage forgotten for at most 8 hours | Information Security Officer | 30 Jun 2027 |

# Glossary {-}

```glossary
APM: Application performance monitoring (Dynatrace)
CO: BDO Cloud Operations (Cloud and Digital Operations Engineering)
Error budget: The share of the service window in which an objective may be missed in a month
Gateway API: Kubernetes standard for routing traffic into the cluster, successor of Ingress
MSK: Amazon Managed Streaming for Apache Kafka
Outbox: The list of integration events that BIBS records with each business transaction and then sends to Kafka
PITR: Point-in-time recovery
RDS: Amazon Relational Database Service
SIEM: Security information and event management
Valkey: Open-source in-memory data store (BSD licence) used by BIBS for caches, job locks and sign-in counters
```

# Sign-off {-}

```signoff
rows:
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Cloud and Digital Operations Engineering", organisation: BDO Unibank IT}
  - {name: "", role: "BIBS Service Owner", organisation: BDOI}
  - {name: "", role: "Information Security Officer", organisation: BDOI}
  - {name: "", role: DevOps Lead, organisation: iorta TechNXT}
```
