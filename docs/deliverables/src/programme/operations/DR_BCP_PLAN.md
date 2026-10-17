---
# Disaster Recovery and Business Continuity Plan of BIBS (programme level, BRD-00).
# Build: python docs/deliverables/src/programme/operations/build_operations_pack.py
title: Disaster Recovery and Business Continuity Plan
subtitle: Recovery objectives, DR topology, backup and replication, failover and failback, DR drills and the business continuity procedures of BIBS
doc_type: DR and BCP Plan
doc_code: Continuity
brd: BRD-00
name: Disaster Recovery and Business Continuity Plan
doc_id: BIBS-OPS-DRP-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Disaster Recovery and Business Continuity Plan
output: Operations/BIBS_Continuity_BRD-00_Disaster_Recovery_and_Business_Continuity_Plan_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT DevOps Lead; iorta TechNXT Project Manager
    approver: Head, BDOI IT (pending)
    change: First issue. RTO 4 hours and RPO 15 minutes of the IER workbook v20 (register DCR-135, DCR-225), service tiers, warm-standby topology in a second AWS region, failover and failback, drill plan and the continuity procedures of the key business processes
distribution:
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "DR authority, plan approval"}
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Service tiers and business priorities"}
  - {name: "Cloud and Digital Operations Engineering", role: Approver, organisation: BDO Unibank IT, purpose: "DR topology, failover, backups, drills"}
  - {name: "Heads of Operations, Comptrollership, Marketing; Head of the Renewal processing team", role: Approvers, organisation: BDOI, purpose: "Continuity procedures of their processes"}
  - {name: "Compliance; Information Security Officer; Data Protection Officer", role: Reviewers, organisation: BDOI, purpose: "Data residency, ransomware scenario, regulatory notices"}
  - {name: "BDO Business Continuity Management", role: Reviewer, organisation: BDO Unibank, purpose: "Alignment with the group BCP"}
  - {name: Project and support team, role: Delivery, organisation: iorta TechNXT, purpose: "Recovery of the application, drills"}
---

# Introduction

## Purpose

This plan states how BIBS is recovered after an outage that the normal high availability of the platform does not absorb, and how BDOI keeps its key processes going while BIBS is not available. It fixes the recovery objectives per service tier and per scenario, the DR topology, the backup and replication scheme, the failover and failback procedures, the DR drills, the continuity procedures of cashiering, remittance, renewal letters and month-end, and the communication plan.

## Audience

The DR authority and the BIBS Service Owner (decisions), BDO Cloud Operations and iorta TechNXT (recovery), BDOI application support (L2) and the business process owners (continuity procedures), and BDO Business Continuity Management (alignment with the group plan).

## Scope

BIBS production in AWS ap-southeast-1 and its DR environment in the second region, all modules live in January 2028, the documents in S3 and the integrations. The legacy systems after the freeze, and the BDO systems on the other side of each interface (EIAM, Apigee X, CCM, bank channels, EGL, EDP), are covered by their own plans; this plan says what BDOI does with BIBS while one of them is down.

## Reference documents

<!-- table: widths=1,7.4,8.2 caption="Reference documents" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| R1 | IER workbook v20 (BDOI IT), sheets HW and SW Requirements and DR | RPO 15 minutes; DR standby with asynchronous replication |
| R2 | BRD Discrepancy and Clarification Register v1.2, DCR-133 to DCR-136, DCR-225, DCR-228 | Service window, availability, RPO and RTO, DR region, backup retention, Valkey mode |
| R3 | FRS BRD-00 Core Replacement v1.0, chapter Non-functional requirements | Availability, backup and retention targets; hosting in ap-southeast-1 with access from the Philippines |
| R4 | Programme Alignment - Drops, Integrations and Infrastructure v1.0 | Environments, Kubernetes sizing including DR, component baseline |
| R5 | Document storage proposal (approved 26 September 2026) | S3 replication with replication time control, Object Lock, BDOI keys |
| R6 | Operations Runbook and Observability v1.0 | Backups, restore tests, alerts, playbooks |
| R7 | Support Model and Support Guide v1.0 | Severities, major incident, escalation |
| R8 | Data Migration Handbook BRD-13 v2.1 | Rollback point of the cutover, legacy read-only access |

# What must be recovered

## Critical business processes

<!-- table: widths=3.6,3.4,2.4,3.4,3.8 caption="Critical business processes of BIBS" bold=first size=8.5 -->
| Process | Owner | Tolerable outage | Peak periods | Continuity procedure (chapter 10) |
|---|---|---|---|---|
| Cashiering: receipts, payment application, payment uploads, PDCs | Head, Operations (Cashiering) | 4 hours | Daily 09:00-15:00; month-end; January | BC-01 |
| Remittance to insurers | Head, Operations (Remittance) | 1 business day | Insurer remittance due dates; month-end | BC-02 |
| Booking and policy issuance | Heads of Retail and Corporate Marketing | 1 business day | January renewal peak | BC-05 |
| Renewal: extraction, renewal advices and letters, insurer requests | Head of the Renewal processing team | 1 business day (expiries within 7 days: 4 hours) | January to May; daily 01:00 extraction | BC-03 |
| Accounting: GL posting, month-end close, BIR books | Head, Comptrollership | 1 business day; 4 hours on the last and first business days of a month | Month-end; year-end; BIR deadlines | BC-04 |
| Disbursement: payment release to payees and insurers | Head, Comptrollership (Disbursement) | 1 business day | Month-end | BC-06 |
| Collections: worklist, files for the bank | Head, Operations (Collections) | 1 business day | Daily files by 06:00 | BC-07 |
| Sanction screening of new clients | Compliance | 1 business day | Client onboarding | BC-08 |
| Sign-in and user access | BDOI IT | 4 hours | Start of the business day | BC-09 |

## Service tiers

<!-- table: widths=2.4,7.6,6.6 caption="Service tiers" bold=first size=8.5 -->
| Tier | BIBS functions | Why |
|---|---|---|
| Tier 1 - Critical | Sign-in; cashiering (receipts, payment application, uploads); remittance; booking; GL posting and the period close; disbursement release; documents (download of stored files) | Money received and owed, books of account; a gap creates financial and regulatory exposure within hours |
| Tier 2 - Essential | Renewal; collections; adjustments and endorsements; submitted policies; claims handling; Employee Benefits; customer servicing; sanction screening; user access requests; e-mail and letters; inbound bank, insurer and watchlist files | Service to clients and insurers; can wait a business day with the continuity procedures |
| Tier 3 - Deferrable | Analytical reports and the Report Pack; extracts to EDP and EGL; ECM archive of final records; Legacy Inquiry; production reconciliation; housekeeping jobs | Derived or periodic; caught up after recovery |

# Recovery objectives

## RTO and RPO per service tier

The IER sets RPO 15 minutes and RTO 4 hours for the platform (R1); register item DCR-135 proposed the same values and DCR-225 accepted them, subject to the BRD owners' confirmation. BIBS is one application on one database, so the platform comes back as a whole; the tiers set the order of the checks and of the catch-up work after the platform is back.

<!-- table: widths=2.6,2.4,2.4,9.2 caption="Recovery objectives per tier (production)" bold=first size=8.5 -->
| Tier | RTO | RPO | Meaning |
|---|---|---|---|
| Tier 1 | 4 hours | 15 minutes | Platform back, Tier 1 functions verified by the key users, users signed in |
| Tier 2 | 8 hours | 15 minutes | Integrations re-established, the jobs missed during the outage caught up, queued e-mails and letters sent |
| Tier 3 | 24 hours | 15 minutes for data; next run for derived extracts | Reports, extracts and archives caught up |
| Non-production | 5 business days | 24 hours | Restored from the release packages and a masked copy when needed |

## Objectives per scenario

<!-- table: widths=4.2,2,2,8.4 caption="Recovery objectives per scenario" bold=first size=8.5 -->
| Scenario | RTO | RPO | How |
|---|---|---|---|
| Loss of a pod or a node | Minutes, automatic | 0 | Kubernetes replaces the pod; at least one pod of each workload always running |
| Loss of an availability zone | 15 minutes, automatic | 0 | Pods spread over three zones; RDS Multi-AZ synchronous standby; Valkey replica; MSK brokers in three zones |
| Database corruption or wrong mass change | 4 hours | To the point before the event (point-in-time recovery to the second) | Restore to a new instance before the event; transactions after that point re-keyed from source documents |
| Loss of the region | 4 hours | 15 minutes | Failover to the DR region (chapter 7) |
| Ransomware or destructive attack on the account | 24 hours | 4 hours (last clean snapshot) | Clean restore from the release packages and from snapshots in the locked backup vault of a separate account (OP-04) |
| Outage of a BDO system (EIAM, Apigee X, CCM, bank channels) | Owner's plan | - | BIBS stays up; continuity procedures of chapter 10 where the business is affected |

## Availability

The availability objective is 99.9 % of the service window (06:00-22:00 Monday to Saturday and month-end Sundays) per month, about 25 minutes of unavailability a month (Support Model, section 4.3). A disaster recovery spends the error budget of the month; it is reported in the monthly service report.

# DR topology

## Primary and DR

Figure 1 shows production in ap-southeast-1 across three availability zones and the warm standby in the DR region. The DR region is still to be approved by BDOI under the same cross-border basis as the primary region (IQ26, DCR-225, DSQ01); the hosting appendix places BIBS in ap-southeast-1 with access restricted to personnel in the Philippines.

![DR topology: production and warm standby](figures/dr_topology.dot){width=15}

<!-- table: widths=3.6,4.4,4.4,4.2 caption="Components in production and in DR" bold=first size=8.5 -->
| Component | Production | DR (warm standby) | Replication and data at failover |
|---|---|---|---|
| Kubernetes (EKS) | Four workloads; web 3-10 pods, jobs 2, integration 2, frontend 2-4 | Cluster running with the current release; backend at 0 pods, scaled to production size at failover | Release packages and configuration deployed to DR with every production release |
| Database (RDS PostgreSQL 16) | Multi-AZ, encrypted | Cross-region read replica | Asynchronous log shipping; lag alert at 5 minutes; promoted at failover |
| Valkey 8 | Primary and replica | None | Created at failover; caches refill on use; job locks start empty |
| Kafka 3.9 (MSK) | Three brokers | Created at failover (or a standby cluster, OP-05) | Not replicated: events not yet delivered are resent from the database outbox |
| Documents (S3) | Four buckets with versioning; Object Lock on documents | Replica buckets | Cross-region replication with replication time control (15 minutes) |
| Container images and secrets | Registry and secrets store | Replicated registry and secrets | Replicated continuously; KMS multi-Region keys or BDOI keys in DR |
| Edge | WAF, Gateway, internal load balancer | Pre-provisioned | Route 53 private zone record switched; Apigee X target switched by the BDOI API team |

## Data loss at failover

At a regional failover, transactions committed in the last minutes before the loss may not have reached the DR replica (the RPO allows up to 15 minutes). After the failover:

- L2 lists the last records per module in DR (receipts, applications, bookings, journals) with their times, and the key users compare them with their own records and source documents of the same period;
- missing transactions are keyed again in BIBS from the source documents; bank and insurer files of the period are uploaded again (BIBS refuses a file it already loaded);
- integration events written in the database are resent from the outbox; e-mails queued in the database are sent again by the mail job.

# Backup and replication

<!-- table: widths=3.6,8.6,4.4 caption="Backup and replication scheme" bold=first size=8.5 -->
| Item | Scheme | Retention |
|---|---|---|
| Database: continuous log archiving | Point-in-time recovery to any second | 35 days |
| Database: snapshots | Every 4 hours, copied to the DR region | 35 days |
| Database: monthly copy | First day of each month, copied to the DR region and to a locked backup vault in a separate AWS account | 7 years (umbrella NFR and DCR-136) |
| Database: before a release, a data correction and the cutover load | Manual snapshot (the rollback point of the cutover is taken at T-2 02:00) | 90 days; the cutover snapshot until true-ups close (2 May 2028) |
| DR replica | Cross-region read replica, continuous | Current |
| Documents and reports | Versioning; cross-region replication with replication time control; Object Lock governance on documents; legal holds | Per record class (5 / 15 years by default; 10 / 15 years for claims; AMLA periods for screening) |
| Release packages and configuration | Every production release kept in the BDO release store and deployed to DR | Current and two previous releases |
| Secrets and keys | Secrets store replicated to DR; KMS keys rotated yearly | Current and previous key until every secret is re-encrypted |

The backup jobs, the restore tests and their evidence are in the Operations Runbook (R6, section 7.3).

# Roles and the DR authority

<!-- table: widths=4,3.6,9 caption="DR roles" bold=first size=8.5 -->
| Role | Who | Responsibility |
|---|---|---|
| DR authority | Head, BDOI IT, with the BIBS Product Owner (deputies: BIBS Service Owner and the Head of Operations) | Declares the disaster, decides failover and failback |
| DR coordinator | BDO Cloud Operations lead on duty | Runs the failover and failback steps, keeps the timeline |
| Application recovery | iorta TechNXT L3 on-call and DevOps | Scales and checks BIBS in DR, outbox resend, job release, smoke test |
| Business verification | Key users of Cashiering, Remittance, Booking, Accounting | Tier 1 checks before the service opens |
| Continuity lead | BIBS Service Owner | Activates the continuity procedures, communication |
| Integration owners | BDOI IT (Apigee X, EIAM, CCM, bank channels) | Switch the targets of their systems to DR; confirm the interfaces |
| Communication | L1 service desk lead; Program Manager during hypercare | Notices to users, branches, insurers and bank channels |

<!-- table: widths=5.6,1.4,1.4,1.4,1.4,1.4,1.4,1.4 caption="RACI. DA = DR authority; DC = DR coordinator; L3 = iorta TechNXT; L2 = BDOI application support; BU = business owners; IO = integration owners; SO = BIBS Service Owner" size=8 -->
| Activity | DA | DC | L3 | L2 | BU | IO | SO |
|---|---|---|---|---|---|---|---|
| Assess the outage | I | R | R | C | I | C | A |
| Declare the disaster | A | C | C | I | C | I | R |
| Failover of the platform | I | A | R | C | - | C | I |
| Switch the integrations | I | C | C | C | - | R | A |
| Business verification | I | I | C | R | A | - | C |
| Continuity procedures | I | - | - | C | R | - | A |
| Failback | A | R | R | C | C | R | C |
| DR drill | A | R | R | C | C | C | R |

# Disaster declaration and failover

## When to fail over

A failover to the DR region is declared when the production region cannot give the Tier 1 service back within the RTO: AWS reports a regional impairment of EKS, RDS or the network; the database cannot be recovered in the region within 2 hours; or the security team requires the production account to be isolated. An outage of one availability zone, one managed service with a working failover, or a BDO system is not a disaster: the playbooks of the runbook apply.

## Failover steps

Figure 2 gives the timeline against the 4-hour RTO; the table gives every step.

![Regional failover against the 4-hour RTO](figures/dr_failover.dot){width=14}

<!-- table: widths=1.4,1.8,8.6,2.6,2.2 caption="Failover steps" bold=first size=8 -->
| Step | Time | Action | Owner | Evidence |
|---|---|---|---|---|
| FO-01 | T0 | P1 opened; bridge opened; first user notice (communication item 1) | L2 lead | Ticket, bridge log |
| FO-02 | T0 + 30 min | Assessment: scope of the outage, AWS status, expected recovery time in the region; replica lag at the time of the loss | DR coordinator, L3 | Assessment note |
| FO-03 | T0 + 60 min | Decision by the DR authority; continuity procedures activated (chapter 10); notice of DR to users and business heads | DR authority | Decision record |
| FO-04 | + 0:10 | Stop writes to production if it is still partly reachable (backend scaled to zero) so that the two regions never both take transactions | DR coordinator | Change record |
| FO-05 | + 0:30 | Promote the DR read replica to a standalone database; record its last transaction time (the recovery point) | DR coordinator | Promotion event; recovery point |
| FO-06 | + 0:40 | Confirm the S3 replica buckets and their latest replicated objects; secrets and keys available in DR | DR coordinator | Replication status |
| FO-07 | + 0:45 | Create Valkey and the Kafka cluster in DR from the stored templates; update their addresses in the DR configuration | DR coordinator | Service status |
| FO-08 | + 1:15 | Scale the BIBS workloads in DR to production size with the jobs held | L3 | Pods ready |
| FO-09 | + 1:30 | Switch the private DNS record of BIBS to DR; BDOI API team switches the Apigee X targets; certificates checked | DR coordinator, integration owners | DNS change; Apigee change |
| FO-10 | + 1:45 | Technical smoke test: sign-in through EIAM, a read and a test transaction reversed afterwards, a document download, the integration connectivity check | L3 | Smoke test record |
| FO-11 | + 1:50 | Integration events resent from the outbox; mail queue released | L3, L2 | Outbox drained |
| FO-12 | + 2:00 | Business verification of Tier 1 by the key users (receipt, payment application, remittance batch view, booking, GL posting view) | Key users | Checklist signed |
| FO-13 | + 2:15 | Service opened to users; notice (communication item 4) | DR authority | Notice |
| FO-14 | + 2:30 | Jobs released in the order of the night batch timeline; missed runs started by hand | L2, L3 | Job runs |
| FO-15 | Same day | Data loss check and re-keying (section 4.2); manual records of the continuity procedures keyed and reconciled | L2, business owners | Reconciliation signed by Comptrollership |

The timings after FO-03 add up to about 2 h 15 min from the decision, so the service is open within the 4-hour RTO when the decision is taken by T0 + 60 minutes and the failover itself is rehearsed in the drills.

# Failback

The return to the primary region is a planned change, done when AWS has restored the region and BDO Cloud Operations has restored the primary environment, at the latest 30 days after the failover.

<!-- table: widths=1.4,10.6,4.6 caption="Failback steps" bold=first size=8.5 -->
| Step | Action | Owner |
|---|---|---|
| FB-01 | Re-create the production environment in ap-southeast-1 from the current release; create a cross-region replica of the DR database in the primary region | DR coordinator |
| FB-02 | Re-establish S3 replication from the DR buckets to the primary buckets and wait until the backlog is replicated | DR coordinator |
| FB-03 | Agree the failback window with the DR authority (Sunday 00:00-04:00, outside the month-end freeze); notify users 5 business days ahead | DR authority |
| FB-04 | In the window: hold the jobs; stop the BIBS workloads in DR; wait until the replica has applied the last transaction; promote it in the primary region | DR coordinator, L3 |
| FB-05 | Start BIBS in the primary region; switch DNS and the Apigee X targets back; smoke test and Tier 1 verification | L3, integration owners, key users |
| FB-06 | Release the jobs; re-create the DR replica from the primary and return DR to warm standby; close the change | DR coordinator |
| FB-07 | If the failback fails before FB-05 ends: switch back to DR (still intact) and plan a new window | DR authority |

# DR drill plan

<!-- table: widths=3.8,3.4,5,4.4 caption="DR drill calendar" bold=first size=8.5 -->
| Drill | When | Scope | Success criteria |
|---|---|---|---|
| Pre-go-live failover drill | 29 Nov to 3 Dec 2027, after the first dress rehearsal | Pre-Prod failed over to the DR environment and failed back, with the dress-rehearsal data | RTO 4 hours and RPO 15 minutes met; steps timed; readiness item OPS-11 |
| Pre-go-live restore test | By 8 Dec 2027 | A production snapshot restored into an isolated instance; the restore time proved on a Pre-Prod snapshot with the full dress-rehearsal volume | Restore within 2 hours; counts equal (readiness OPS-10) |
| Tabletop exercise with the business | 6 Dec 2027, then yearly in March | Declaration, continuity procedures, communication | Roles and decisions confirmed; actions logged |
| Component drills | Twice a year (May and November, from 2028) | Point-in-time recovery to a new instance; Valkey re-created; Kafka cluster lost; availability-zone failure simulated in Pre-Prod | Each component back within its objective; playbooks corrected |
| Full regional failover of production | Yearly, first in September 2028, on a Sunday outside the month-end and renewal peaks | Production failed over to DR, run for at least 4 business hours, then failed back | RTO and RPO met; no data lost beyond the RPO; users able to work in DR |

**Evidence kept** for each drill: plan, timeline with actual times, recovery point and time achieved, issues and actions with owners, sign-off of the DR authority. Drill reports are attached to the monthly service report of the month and reviewed by BDO Business Continuity Management. A drill that misses its objectives is repeated within 3 months after the actions are done.

# Business continuity procedures

## When the procedures start

The BIBS Service Owner activates the continuity procedures when BIBS (or EIAM, or a bank channel that a process needs) is unavailable, or expected to be, for more than **1 hour within the service window**, or at once when a disaster is declared. The department heads run the procedures of their processes; L1 tells users that the procedures are active.

**Daily continuity pack.** Each department's continuity custodian receives every business day at 06:00 a set of reports by e-mail through BIBS report subscriptions (FR-CR-043), stored by BDOI in a controlled M365 folder and deleted after 7 days. The pack is what the teams work from while BIBS is down:

<!-- table: widths=4,6,6.6 caption="Daily continuity pack (06:00, by subscription)" bold=first size=8.5 -->
| Report | Custodian | Used for |
|---|---|---|
| Open invoices by branch and client, with the amount due | Cashiering | Accepting payments and quoting balances |
| Unapplied payments and post-dated checks due in 5 days | Cashiering | Deposits and maturing checks |
| Remittance due list by insurer and due date | Remittance | Urgent remittances |
| Renewals expiring within 30 days with bucket, RA status and insurer | Renewal processing team | Contacting insurers and clients |
| Collection worklist with promises due | Collections | Follow-up calls |
| Disbursements approved and not yet released | Disbursement | Urgent payments |
| Trial balance of the previous day (month-end days only) | Comptrollership | Close decisions |

## BC-01 Cashiering

1. Branches keep accepting payments in cash and checks. Each payment is written in the **contingency collection log** of the branch (date, time, payor, client and invoice numbers, amount, mode, check details, cashier) and the payor receives a pre-numbered acknowledgement from the branch's contingency series (OP-06: BDOI and Comptrollership confirm the form and its BIR treatment).
2. Deposits go to the bank as usual; the log is signed by the branch head at the end of the day.
3. Payments through the bank channels (bills payment, OBPCS, Old BOB, direct credit) continue at the bank; their files are uploaded when BIBS is back. BIBS refuses a file it already loaded.
4. After recovery, cashiers key the logged payments in BIBS on the same day, with the payment date of the log and the contingency number in the remarks; the official receipt is issued from BIBS and sent to the payor. The branch head reconciles the log with the receipts keyed and with the deposit.
5. If the outage lasts beyond the end of the business day, the deposits of the day are reconciled with the logs the next morning and Comptrollership is told of any difference.

## BC-02 Remittance

1. An outage shorter than one business day delays the extraction and the batches; nothing is paid twice because remittance batches are made only in BIBS.
2. If the outage crosses an insurer's remittance due date, Remittance takes the insurer's lines from the remittance due list of the continuity pack, prepares a manual remittance advice, and pays through the CMS / New BOB channel with the usual two approvals and the written approval of the Head of Comptrollership. Only items already paid and cleared are remitted.
3. After recovery, Remittance records the manual payment in BIBS against the same invoices as a remittance batch with the bank reference, so that the remittance status of each invoice is right and the item is not extracted again. Comptrollership checks the DTIP balances of the insurer.

## BC-03 Renewal letters and renewals

1. Renewal advices and letters that BIBS sends by e-mail wait in the queue and are sent when BIBS is back; the renewal extraction takes the missed dates at its next run.
2. For policies expiring within 7 days, the Renewal processing team works from the renewals list of the continuity pack: it calls or e-mails the insurer (from M365) to confirm terms or request a hold cover, and calls the client or Account Officer; each contact is noted in a contingency tracker with date, person and outcome.
3. After recovery, the team records the dispositions and the insurer answers in BIBS, and checks that no renewal advice is sent twice (the RA-sent status is updated before the letter jobs release).
4. In January to May 2028 (the go-live renewal window) the 7-day threshold becomes 14 days for the URGENT January expiries.

## BC-04 Month-end

1. On the last two and first three business days of a month, an outage of more than 4 hours is escalated at once to the Head of Comptrollership.
2. The broking books close and the GL close run when BIBS is back; Comptrollership decides whether the close calendar moves by the length of the outage and informs the business heads.
3. Postings that belong to the closing month are keyed with their own dates before the close runs; the period stays open until the catch-up of BC-01 to BC-02 is reconciled.
4. BIR filing deadlines that fall in the outage use the reports of the previous closed period and the daily trial balance of the continuity pack only with the Head of Comptrollership's approval; filings are amended if the catch-up changes the figures.

## BC-05 Booking and issuance

New bookings and issuance wait for BIBS. Urgent covers are bound with the insurer by the Account Officer through a written hold cover from the insurer; the booking is keyed when BIBS is back and the hold cover is attached.

## BC-06 Disbursement

Only urgent payments approved before the outage (on the continuity pack list) are released, through CMS / New BOB with two approvals and the Head of Comptrollership's written approval. After recovery the disbursement is recorded in BIBS against the same request so that it is not released twice.

## BC-07 Collections

Collectors work the promises due from the worklist of the continuity pack and record the outcome in a contingency tracker; dispositions are keyed after recovery. The For Application To Invoice and Outstanding PR files are produced by their jobs when BIBS is back; the bank is told of the delay by Operations.

## BC-08 Sanction screening

No client is onboarded and no policy bound for a new client while screening is unavailable, unless Compliance screens the client by hand in the screening service it uses (Bridger Insight XG) and records the result; the client is screened again in BIBS after recovery.

## BC-09 Sign-in (EIAM outage)

Users who are signed in keep working until their session ends; L1 asks them not to sign out. The break-glass administrators can sign in for urgent administration only. If the outage lasts beyond 1 hour in the service window, the departments start BC-01 to BC-08 as if BIBS were down.

## Catch-up and reconciliation

<!-- table: widths=1.4,10.6,4.6 caption="Catch-up order after recovery" bold=first size=8.5 -->
| Order | Catch-up | Signed off by |
|---|---|---|
| 1 | Data loss check after a failover (section 4.2) | BIBS Service Owner |
| 2 | Payments from the contingency logs and the bank channel files (BC-01) | Branch heads; Head, Operations |
| 3 | Manual remittances and disbursements recorded against their items (BC-02, BC-06) | Head, Comptrollership |
| 4 | Jobs missed during the outage, in the order of the night batch timeline | L2 lead |
| 5 | Renewal dispositions, collection dispositions, held bookings (BC-03, BC-05, BC-07) | Heads of department |
| 6 | Reconciliation of the period: receipts against deposits, remittances against bank, GL-SL reconciliation | Head, Comptrollership |

# Communication plan

<!-- table: widths=0.7,4,3.4,3,2.6,2.9 caption="Communication during an outage and a disaster" size=8 -->
| # | Message | Audience | Channel | When | Owner |
|---|---|---|---|---|---|
| 1 | BIBS unavailable; what is known; next update time | All users; department heads | E-mail, intranet banner, branch heads' phone tree | Within 30 minutes of the P1 | L1 lead |
| 2 | Continuity procedures active; which ones; where the continuity pack is | Department heads, branch heads, custodians | E-mail and phone tree | At activation | BIBS Service Owner |
| 3 | Hourly update | Same as 1 | E-mail | Every hour until restored | L1 lead |
| 4 | Service restored (or open in DR); what to re-key; the catch-up timetable | All users | E-mail, intranet | At restore | BIBS Service Owner |
| 5 | Delays in remittances, payments or files | Insurers, bank channels, corporate clients concerned | Letter or e-mail from Operations | When an outage passes 4 hours in the service window or crosses a due date | Head, Operations |
| 6 | Notice to the BDO group and regulators as required by the BDO incident reporting policy | BDO Business Continuity Management; regulators named by Compliance | Per policy | As the policy requires | Compliance |
| 7 | Incident report with root cause and actions | Head, BDOI IT; BIBS Product Owner; BDO BCM | Report | Within 5 business days | iorta TechNXT Support Manager with BDO Cloud Operations |

# Maintaining this plan

The plan is reviewed every year in March, after every drill and after every change of the topology, the service tiers or the BDO policy. The BIBS Service Owner owns it; changes are approved by the DR authority. Contact lists are checked every quarter.

# Decisions and open points for BDOI {-}

<!-- table: widths=1.5,8.6,4,2.5 caption="Decisions and open points" bold=first size=8.5 -->
| Ref | Point and our proposal | Owner | Needed by |
|---|---|---|---|
| OP-01 | Name the DR region and confirm that data residency allows it (IQ26, DCR-225, DSQ01); until then the plan assumes a second AWS region approved under the same cross-border basis | Compliance with BDOI IT | 31 Mar 2027 |
| OP-02 | Confirm RTO 4 hours and RPO 15 minutes for the whole platform (DCR-135) and the tier objectives of chapter 3 | BIBS Product Owner with the BRD owners | 16 Oct 2026 |
| OP-03 | Confirm the DR authority (Head, BDOI IT with the BIBS Product Owner) and the deputies | Head, BDOI IT | 30 Jun 2027 |
| OP-04 | Confirm the locked backup vault in a separate AWS account for the monthly copies (ransomware scenario) | Information Security Officer | 31 Mar 2027 |
| OP-05 | Choose between creating MSK at failover (lower cost, proposed) and a standby MSK cluster in DR (faster) | Cloud and Digital Operations Engineering | 31 Mar 2027 |
| OP-06 | Agree the contingency acknowledgement series of the branches and its BIR treatment (BC-01) | Head, Comptrollership | 30 Jun 2027 |
| OP-07 | Name the continuity custodians per department and confirm the daily continuity pack and its M365 folder | Department heads | 30 Sep 2027 |
| OP-08 | Confirm the dates of the pre-go-live failover drill (29 Nov to 3 Dec 2027) and of the first production failover drill (September 2028) | Head, BDOI IT | 30 Jun 2027 |
| OP-09 | Confirm the regulators and group functions to be told of a major BIBS outage | Compliance | 30 Sep 2027 |

# Glossary {-}

```glossary
BCM: Business continuity management (BDO group function)
DR: Disaster recovery
DTIP: Due to insurer premium
Failback: The planned return from the DR region to the primary region
Failover: The move of the service to the DR region
RPO: Recovery point objective - the most data, in time, that may be lost
RTO: Recovery time objective - the longest time until the service is back
Warm standby: DR environment kept running at minimum size with replicated data, scaled up at failover
```

# Sign-off {-}

```signoff
rows:
  - {name: "", role: "Head, BDOI IT (DR authority)", organisation: BDOI}
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "Head, Operations", organisation: BDOI}
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Cloud and Digital Operations Engineering", organisation: BDO Unibank IT}
  - {name: "", role: Solution Architect, organisation: iorta TechNXT}
```
