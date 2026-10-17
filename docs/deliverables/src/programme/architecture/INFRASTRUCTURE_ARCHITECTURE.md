---
# Infrastructure Architecture of BIBS (BRD-00, Programme/Architecture).
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py
title: Infrastructure Architecture
subtitle: Hosting, network zones, sizing, data services, high availability, backup, monitoring and the component baseline of BIBS
doc_type: Infrastructure Architecture
doc_code: Architecture
brd: BRD-00
name: Infrastructure Architecture
doc_id: BIBS-ARC-03
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 3 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_Infrastructure_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT DevOps Engineer; iorta TechNXT Tech Lead
    approver: BDO IT Cloud and Digital Operations Engineering (pending)
    change: First issue, aligned with the IER workbook v20 and the component baseline of 8 October 2026
distribution:
  - {name: "Cloud and Digital Operations Engineering (IER owner)", role: Approver, organisation: BDO Unibank IT, purpose: "Hosting, sizing, IER restatement"}
  - {name: "Enterprise Architecture", role: Reviewer, organisation: BDOI IT, purpose: "Infrastructure baseline"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Zones, trust boundaries, encryption, keys"}
  - {name: "Network team", role: Reviewer, organisation: BDO Unibank IT, purpose: "Connectivity, private routes, address ranges"}
  - {name: "IT Operations and DBA", role: Reviewer, organisation: BDOI IT, purpose: "Backups, DR, monitoring"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Environments and operations support"}
---

# Introduction

## Purpose and audience

This document describes the infrastructure that runs BIBS, for BDO IT Cloud and Digital Operations Engineering (owner of the IER workbook), the network team, the Information Security Office, the DBAs and IT Operations. It states the hosting model, the network zones and trust boundaries, the sizing of each environment derived from the IER workbook, the data services and their high availability and backups, disaster recovery, monitoring, and the component baseline with licences and support horizons.

## Scope

All six environments (DEV, SIT, UAT, Pre-Prod, PROD, DR) on AWS, the EKS clusters at node level, PostgreSQL 16, Valkey 8, Apache Kafka 3.9 (KRaft), Amazon S3, keys and secrets, connectivity to the BDO network and Apigee X, and the monitoring stack. Kubernetes resources inside the clusters are in the Deployment Architecture.

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | IER workbook v20: HW and SW Requirements, Kubernetes sheets (DEV, SIT, UAT, PROD Y1-Y5), environment sheets, assumptions, VDI requirements | Sizing, hours, growth, recovery objectives |
| S2 | Programme Alignment pack v1.0, chapter 6 (IER against BIBS) and questions IQ25 to IQ35 | Changes to the IER, open questions |
| S3 | Document storage decision record (approved 26-Sep-2026) | Buckets, keys, Object Lock, replication, malware scanning |
| S4 | Application architecture option decision (26-Sep-2026) | Edge, encryption in transit, network policies |
| S5 | Client decisions of 8-Oct-2026 | Component baseline (Valkey, Kafka 3.9, Gateway API) |
| S6 | BIBS deployment guide, runbook, platform cache and events architecture | Database roles, backups, Valkey and Kafka behaviour |
| S7 | Umbrella Core Replacement BRD p.42-46 and the hosting appendix | Users, volumes, retention, backup, data residency |

## Conventions

Open points of this document are numbered IA-01 to IA-10 (chapter 12). Sizes are vCPU and GiB; storage in GB as in the IER.

# Hosting model

BIBS runs in BDOI's AWS organisation in the Asia Pacific (Singapore) region, ap-southeast-1, on managed services (ADR-18). Philippine data stays in the approved regions; access is restricted to personnel in the Philippines (hosting appendix).

<!-- table: widths=3.6,6.4,7.6 caption="Hosting model" bold=first size=8.5 -->
| Layer | Service | Notes |
|---|---|---|
| Containers | Amazon EKS, managed node groups across three availability zones | Kubernetes version supported by EKS; upgraded within its 14-month standard support |
| Relational data | Amazon RDS for PostgreSQL 16 | Single-AZ in DEV, SIT, UAT; Multi-AZ in Pre-Prod and PROD; cross-region read replica for DR |
| Cache and shared state | Amazon ElastiCache for Valkey 8 | Cluster mode disabled; primary and replica in Pre-Prod and PROD |
| Event streaming | Amazon MSK, Apache Kafka 3.9 in KRaft mode | 2 brokers in DEV, SIT, UAT; 3 brokers in Pre-Prod, PROD and DR |
| Files | Amazon S3, four buckets per environment | SSE-KMS, Object Lock, GuardDuty Malware Protection, cross-region replication from PROD |
| Keys and secrets | AWS KMS (customer-managed keys owned by BDOI), AWS Secrets Manager, AWS Private CA | One key per environment and bucket class; multi-Region keys for DR |
| Edge | Route 53 private zone, AWS WAF, internal Application Load Balancer through the Gateway API | No public exposure |
| Images | Amazon ECR with image scanning and replication to the DR region | Immutable tags |
| Management | Bastion through AWS Systems Manager (no inbound ports), pipeline runner, VDI for the project team | 15 VDI users (IER) |
| Monitoring | Dynatrace (BDO standard of the IER cluster sheets), Amazon CloudWatch, AWS CloudTrail | Tool choice IQ30 |

**Account structure (proposed, IA-01).** One AWS account for non-production (DEV, SIT, UAT), one for Pre-Prod and one for production, with the DR region in the production account; each account with its own KMS keys and Secrets Manager, so that production keys and data are never reachable from a test environment.

**Responsibilities.** AWS operates the managed services; BDO IT Cloud and Digital Operations provisions and operates the accounts, networks, clusters and data services (infrastructure as code, IA-02); iorta TechNXT provides the application, its manifests, the sizing and the runbooks, and supports operations.

# Environments

<!-- arch:environments -->

The 12 x 5 hours of the IER apply to the EKS nodes and RDS instances of the non-production environments; MSK and ElastiCache cannot be stopped and run 24 x 7. Extended hours are booked for batch and month-end test cycles, trial migrations (weekend loads) and the dress rehearsal; the night jobs are moved into the working window by configuration (IQ27, DA-07). Training uses a separate database on the UAT cluster with seed data (IA-09).

# Network zones and trust boundaries

![Network zones and trust boundaries](figures/ia_network.dot){width=16}

<!-- table: widths=3.4,6.8,7.4 caption="Network zones" bold=first size=8.5 -->
| Zone | Contains | Controls |
|---|---|---|
| Z1 BDO corporate network | User workstations, VDI, BDO on-premises systems (bank channels, LMS, PMS, CCM, file server) | BDO perimeter; Direct Connect or VPN to AWS |
| Z2 Apigee X | BDO API proxies in Google Cloud | OAuth 2.0 client credentials, quotas, spike arrest, analytics; private interconnect to AWS (HA VPN or Interconnect and Direct Connect through the BDO backbone) |
| Z3 Edge subnets | Route 53 private zone, internal load balancer with AWS WAF | HTTPS 443 only; source ranges limited to Z1 and Z2 |
| Z4 Application subnets | EKS worker nodes, namespace bibs | Network policies deny by default; security groups for pods; restricted Pod Security; IRSA |
| Z5 Data subnets | RDS, ElastiCache, MSK | Security groups admit Z4 only; no route out; TLS required |
| Z6 Private access to AWS services | S3 gateway; PrivateLink interfaces for KMS, STS, Secrets Manager, ECR and CloudWatch Logs | Bucket policies deny non-TLS requests and access from outside the VPC |
| Z7 Management | Bastion through SSM, pipeline runner | No inbound ports; session logging; MFA |

<!-- table: widths=2,5,10.6 caption="Trust boundaries" bold=first size=8.5 -->
| Boundary | Between | Controls at the boundary |
|---|---|---|
| TB1 | Users (Z1) and the edge (Z3) | TLS 1.2+ with the BDO certificate; AWS WAF managed rules and rate rule; EIAM sign-in, TOTP for privileged users; 15-minute access tokens |
| TB2 | Apigee X (Z2) and BIBS | Private route only; WAF admits the integration prefix only from the Apigee ranges; BIBS validates the Apigee token (issuer, audience, scopes per API); outbound calls to BDO systems also through Apigee |
| TB3 | Edge (Z3) and the pods (Z4) | HTTPS to the pods with certificates from AWS Private CA; network policy admits only the load balancer subnets |
| TB4 | Pods (Z4) and data services (Z5) | TLS verify-full to RDS, TLS and AUTH to Valkey, SASL_SSL to MSK; least-privilege database login; credentials from Secrets Manager |
| TB5 | Pods (Z4) and AWS services (Z6) | IAM role per workload (IRSA), least privilege per bucket and key; KMS key policies held by BDOI security |
| TB6 | Pods (Z4) and BDO on-premises systems (Z1) | Through Apigee X (APIs) or the CCM relay (SMTP 587 with STARTTLS); files through the S3 buckets with malware scanning |

There is no inbound route from the internet to any zone and no outbound internet access from the pods: images come from ECR and AWS services through PrivateLink. Subnet ranges, the Direct Connect or VPN route and the Apigee source ranges are BDO network values (Deployment Architecture, chapter 7).

# Compute and storage sizing

## IER figures and the proposed BIBS sizing

The IER sized about ten microservices. BIBS runs four workloads from two images (ADR-01, ADR-15), so the Kubernetes rows are restated; the data services keep the IER sizes adjusted to the component baseline. Growth is 15 % a year over 60 months (IER).

<!-- table: widths=2.4,7.2,8 caption="IER requests against the proposed sizing" bold=first size=8 -->
| Item | IER workbook v20 | Proposed for BIBS |
|---|---|---|
| EKS worker nodes PROD Y1 | 3 x m5.4xlarge (16 vCPU, 64 GiB) for ten bv-* services; Y5 6 nodes | 3 to 6 x m5.2xlarge (8 vCPU, 32 GiB), one or more per zone; Y5 4 to 8 nodes (DA-05) |
| EKS worker nodes UAT | 2 x m5.4xlarge | 3 x m5.xlarge, one per zone |
| EKS worker nodes DEV, SIT | 2 nodes each | 2 x m5.xlarge (SIT up to 3) |
| Persistent volumes | EFS "Total PV" of 21,800 GB (DEV) to 87,200 GB (PROD Y5) | None: pods are stateless, files are in S3; the EFS totals came from a formula error (maximum replicas x CPU limit) |
| PostgreSQL PROD | 16 vCPU, 128 GB, 2,000 GB, Multi-AZ | Same; storage autoscaling on; re-sized once Trial migration 2 measures the migrated volume without file content (IA-04) |
| PostgreSQL Pre-Prod / UAT / SIT / DEV | 8, 64, 1,000 Multi-AZ / 8, 32, 500 / 4, 16, 200 / 4, 16, 200 | Same |
| Cache and streaming PROD | ElastiCache for Redis 7 (cluster) + MSK 3 brokers (Kafka 3.6), 8 vCPU, 32 GB, 1,000 GB | ElastiCache for Valkey 8, cache.r7g.large primary and replica, cluster mode disabled; MSK Kafka 3.9 KRaft, 3 x kafka.m5.large, 100 GB per broker |
| DR | Cross-region read replica 16 vCPU, 128 GB, 2,000 GB; cache and streaming 3 brokers | Read replica as IER; Valkey and MSK created at failover; EKS standby with 2 nodes |
| Bastion and runner | 4 vCPU, 16 GB, 200 GB, RHEL 9 | Same (SSM, kubectl, kustomize) |
| VDI | 15 users: 8 developers (24 months), 4 testers (18), 3 production support (60) | Same; software list Temurin 21, Maven 3.9, Node 22 (Node 24 from April 2027), no Angular, no JDK 17 (IQ28) |

<!-- arch:sizing caption="Proposed Kubernetes sizing per environment" -->

<!-- arch:data_services caption="Data services per environment" -->

## How the sizes were derived

- **Users and load.** 429 concurrent sessions at peak (SA-02), screens p95 under 2 seconds; the peak hours are 08:00-10:00 and 15:00-17:00 and the month-end (BRD-6, BRD-3). bibs-web scales from 3 to 10 pods of 1 to 2 vCPU; the performance test on Pre-Prod (November - December 2027) confirms the figures.
- **Volumes.** 21,200 new-business and 25,800 renewal transactions a month, 144,400 CSF interactions a year, 15 % growth a year. Kafka carries ten topics of three partitions; at these volumes 100 GB per broker holds more than the 7-day retention.
- **Database.** One database for all modules; file content is in S3, so the database holds records, journals, ledger entries, audit trail and file metadata only.
- **Files.** The S3 volume is set by the existing document volumes copied before go-live and the yearly growth; the estimate is made from the legacy file inventory (IA-05).

# Data services

## PostgreSQL 16: high availability and backups

<!-- table: widths=4,13.6 caption="PostgreSQL 16 on Amazon RDS" bold=first size=8.5 -->
| Aspect | Set-up |
|---|---|
| Availability | Multi-AZ in Pre-Prod and PROD: synchronous standby in a second zone, automatic failover in about one to two minutes; the application reconnects through its pool |
| Encryption | At rest with the BDOI customer-managed KMS key of the environment; in transit required (force-SSL parameter on, clients verify-full with the RDS CA bundle) |
| Logins and roles | Schema owner login (migrations at start-up only); runtime group role without login holding row access only; application login, member of the runtime role, used by the connection pools. The runtime login cannot change the schema, truncate tables or disable the immutability triggers of the audit trail and the ledger |
| Point-in-time recovery | Automated backups with continuous transaction-log archiving, retention 35 days; restore to any second within the window |
| Snapshots | AWS Backup plan: every 4 hours kept 35 days; daily kept 35 days; monthly kept 7 years in a vault with Vault Lock, copied to the DR region; a manual snapshot before every PROD release |
| DR replica | Cross-region read replica in the DR region (asynchronous, lag normally seconds); promoted at failover |
| Restore tests | Quarterly restore of the latest snapshot into Pre-Prod with reconciliation of ledger totals; yearly DR failover test |
| Monitoring | Performance Insights; CloudWatch alarms on CPU, free memory, free storage, connections, replica lag and deadlocks |
| Maintenance | Minor versions applied in the 00:00-04:00 window after Pre-Prod; the major version reviewed at the annual technology review (PostgreSQL 16 community support until 09-Nov-2028) |

The backup policy meets the umbrella BRD (every 4 hours, kept 5 years) and the register proposal of 7 years; the retention of the business records themselves follows the retention rules of BIBS (5 years online, 15 offline).

## Valkey 8

<!-- table: widths=4,13.6 caption="Valkey 8 on Amazon ElastiCache" bold=first size=8.5 -->
| Aspect | Set-up |
|---|---|
| Use | Reference-data cache (lists of values, parameters, role permissions, data scope, product versions, organisation units), cluster-wide job locks with fencing tokens, token deny-list after sign-out, sign-in counters and rate limits |
| Topology | Cluster mode disabled (one shard), primary and replica in two zones with automatic failover in Pre-Prod and PROD; single node in DEV and SIT |
| Security | In-transit encryption and AUTH token (or an RBAC user); at-rest encryption with the BDOI KMS key; security group admits the EKS subnets only |
| Data loss tolerance | No persistence needed: caches refill from the database, locks expire within their 2-minute lease, the deny-list falls back to the session log in PostgreSQL |
| Eviction | volatile-lru (proposed): every key carries a time to live (caches, locks, deny-list, counters) |
| Key prefix | Set per environment by configuration, so a shared cache can never mix environments |

## Apache Kafka 3.9 (KRaft)

<!-- table: widths=4,13.6 caption="Apache Kafka 3.9 on Amazon MSK" bold=first size=8.5 -->
| Aspect | Set-up |
|---|---|
| Mode | KRaft (no ZooKeeper), Kafka 3.9 |
| Brokers | 3 x kafka.m5.large across three zones in Pre-Prod, PROD and DR; 2 brokers in DEV, SIT and UAT |
| Topics | Ten topics bibs.<domain>.<event>.v1 and their dead-letter topics, created by the application with 3 partitions; replication factor 3 and minimum in-sync replicas 2 (replication factor 2 on two brokers); automatic topic creation off on brokers and clients |
| Retention | 7 days on the event topics and 30 days on the dead-letter topics (proposed, IA-06); the outbox and the event archive in PostgreSQL are the long-term record |
| Security | SASL_SSL with SCRAM-SHA-512 (credentials in Secrets Manager) or IAM authentication; plaintext listeners disabled; encryption at rest with the BDOI KMS key |
| Producer and consumers | Producer with acknowledgements from all in-sync replicas and idempotency; consumer groups bibs-mail-dispatch, bibs-event-archive and bibs-dead-letter-recorder, plus the ECM archive connector |
| Monitoring | Under-replicated partitions, offline partitions, consumer lag per group, broker disk use |

## Object and file storage (Amazon S3)

<!-- table: widths=4,6.6,7 caption="S3 buckets per environment" bold=first size=8.5 -->
| Bucket | Content | Lifecycle and protection |
|---|---|---|
| bibs-<env>-documents | Attachments, generated documents (slips, invoices, ORs, SOAs, vouchers, letters, STRs), Word and PDF renditions | Versioning; Standard, then Standard-IA after 90 days, Glacier Instant Retrieval after 1 year; Object Lock in governance mode with legal hold; deletion by the BIBS retention rules |
| bibs-<env>-reports | Report runs, scheduled files, batch ZIP files | Expiry per the report archive setting (default 400 days) |
| bibs-<env>-inbound | Bulk uploads, bank and insurer files, watchlist feeds | Incoming and quarantine prefixes; GuardDuty Malware Protection; kept 90 days |
| bibs-<env>-migration | Migration extracts and staging files | Expiry after 5 days; migration role only |

All buckets: public access blocked at account and bucket level; SSE-KMS with the BDOI key of the bucket class and bucket keys; bucket policies deny unencrypted uploads, non-TLS access and other regions; object keys carry no personal data (company, record type, year, month, random identifier); CloudTrail data events and server access logs kept for audit. PROD documents and reports buckets replicate to the DR region with replication time control (15 minutes). Amazon EFS is not used.

# High availability and disaster recovery

![High availability and disaster recovery](figures/ia_dr.dot){width=16}

<!-- table: widths=3,5,5.6,4 caption="Availability and recovery per component" bold=first size=8 -->
| Component | Within the region | DR region | Recovery point |
|---|---|---|---|
| bibs-web, bibs-frontend | 3 zones, at least 2 pods, autoscaling | Standby at 1 pod each; scaled to PROD at failover | Stateless |
| bibs-jobs, bibs-integration | 2 pods across zones; job lock | 0 pods until failover (so jobs and consumers never run twice) | Stateless |
| PostgreSQL | Multi-AZ synchronous standby | Cross-region read replica, promoted | Seconds of replica lag; at most 15 minutes |
| Valkey | Primary and replica, automatic failover | Created at failover | Not needed (caches refill) |
| Kafka | 3 brokers, replication factor 3 | Created at failover | Undelivered events resent from the outbox |
| S3 | 11 nines durability, versioning | Replica buckets, replication time control | 15 minutes |
| Images, secrets, keys | ECR, Secrets Manager, KMS | ECR replication, secret replicas, multi-Region keys | Current |

**Failover plan (targets).** The RTO of 4 hours is met by the following steps, rehearsed in the yearly DR test:

<!-- table: widths=2,9.6,6 caption="DR failover steps and target times" bold=first size=8.5 -->
| Target time | Step | Owner |
|---|---|---|
| T + 0:30 | Incident assessed; failover decided by the DR owner | BDOI IT Operations |
| T + 0:45 | RDS replica promoted to primary in the DR region | DBA |
| T + 1:30 | ElastiCache for Valkey and MSK created from the infrastructure templates; secrets updated with their addresses | BDO IT Cloud |
| T + 2:00 | DR overlay applied: workloads scaled to the PROD sizing; jobs and integration started | Release team |
| T + 2:15 | Route 53 record of the BIBS host name switched to the DR load balancer; Apigee targets switched | BDO network and API teams |
| T + 3:00 | Smoke test and business verification by key users; outbox resends pending events | iorta TechNXT support, BDOI key users |
| T + 4:00 | RTO limit | DR owner |

The DR region and its cross-border basis must be approved by BDOI before the DR environment is provisioned (IA-03, IQ26, DSQ01).

# Monitoring

![Monitoring and alerting](figures/ia_monitoring.dot){width=15}

- **Metrics.** Each backend pod exposes Prometheus metrics on the management port 9090, reachable only from the monitoring namespace: JVM, HTTP latency and status by operation, connection pool, journals posted, security store failures. The IER cluster sheets place Dynatrace in every cluster; Dynatrace scrapes the Prometheus metrics and adds APM traces, host and Kubernetes monitoring. CloudWatch holds the managed-service metrics.
- **Logs.** One line per event on the console with ISO timestamps and the correlation identifier; line breaks neutralised against log forging. Container logs go to CloudWatch Logs (Fluent Bit) and on to the BDO log platform and SIEM; load balancer, WAF and CloudTrail logs too. Retention: 90 days searchable, then archived for one year (IA-07).
- **In the application.** The Scheduled Jobs monitor (run history, skipped and failed runs), the Integration Events screen (outbox, archive, dead letters), exception codes and alerts with notifications, the audit trail.

<!-- table: widths=4.4,7.4,3,2.8 caption="Alert catalogue (proposed thresholds)" bold=first size=8 -->
| Alert | Condition | Severity | Routed to |
|---|---|---|---|
| Service unavailable | Any served workload with no ready pod, or load balancer 5xx above 2 % for 5 minutes | Critical | IT Operations, on call |
| Slow screens | p95 latency of user operations above 2 seconds for 10 minutes | High | IT Operations, iorta support |
| Security store failure | Any increase of the security store failure counter (deny-list, session log or rate counters unreadable) | Critical | IT Operations, Security |
| Job failure | JOB_FAILURE alert raised by BIBS, or a job skipped on every pod longer than its run time | High | Job owner unit, iorta support |
| Outbox backlog | Pending events older than 15 minutes, or any failed outbox row | High | iorta support |
| Dead letters | A new dead letter | Medium | iorta support |
| Database | CPU above 80 % for 15 minutes; free storage under 20 %; replica lag above 5 minutes (RPO risk) | High | DBA |
| Valkey | Memory above 80 %; failover event | Medium | IT Operations |
| Kafka | Under-replicated partitions above 0 for 5 minutes; consumer lag above 1,000 records | High | IT Operations |
| Capacity | bibs-web at its maximum replicas for 15 minutes; node CPU above 80 % | Medium | IT Operations |
| Certificates | Any certificate expiring in less than 14 days | High | Release team |
| Malware | A quarantined file (FILE_QUARANTINED) | High | Security, uploader |
| Backups | A failed AWS Backup job or snapshot copy | High | DBA |

# Security infrastructure

<!-- table: widths=4,13.6 caption="Security services" bold=first size=8.5 -->
| Service | Use |
|---|---|
| AWS KMS | Customer-managed keys created and administered by BDOI security in BDOI's account: per environment for RDS, ElastiCache, MSK and Secrets Manager; per environment and bucket class for S3; multi-Region keys for DR; rotation per BDOI policy (annual by default); imported key material possible without a change in BIBS |
| AWS Secrets Manager | Database logins, signing and encryption keys of the application, Valkey and Kafka credentials, SMTP and OIDC client secrets; rotation policy per secret |
| AWS Private CA | In-cluster TLS certificates through cert-manager |
| IAM and IRSA | One role per backend workload; bucket- and key-level least privilege; no long-lived keys |
| AWS WAF | Managed rule groups, rate rule, Apigee-only rule (Deployment Architecture, chapter 5) |
| Amazon GuardDuty | Malware Protection for S3 on the inbound bucket and the upload prefix of the documents bucket; threat detection for the accounts and EKS |
| AWS CloudTrail | Management and S3 data events, kept for audit |
| CrowdStrike Falcon | Host protection (EDR) on the worker nodes (IER cluster sheets) |

# Component baseline

<!-- arch:baseline caption="Component baseline with licences and support horizon (8 October 2026)" -->

<!-- arch:components_more -->

The baseline is reviewed every year and before go-live (first review 31-Jan-2027, second in October 2027 before the ORR): each support date is checked against the upstream or AWS pages, components that reach their support end within the next twelve months are planned for upgrade, and this table is re-issued. Every component is under a licence that allows commercial use without a fee; none is source-available (ADR-04).

# Responsibilities

<!-- table: widths=5.4,2.4,2.4,2.4,2.4,2.6 caption="Infrastructure responsibilities (R responsible, A accountable, C consulted, I informed)" bold=first size=8 -->
| Activity | BDO IT Cloud and Digital Operations | BDO network | BDOI Information Security | BDOI IT Operations and DBA | iorta TechNXT |
|---|---|---|---|---|---|
| AWS accounts, VPCs, subnets | A, R | C | C | I | C |
| Direct Connect, VPN, Apigee private route | C | A, R | C | I | I |
| EKS clusters and add-ons | A, R | I | C | I | C |
| RDS, ElastiCache, MSK, S3 provisioning | A, R | I | C | C | C |
| KMS keys and key policies | C | I | A, R | I | I |
| Backups and restore tests | C | I | I | A, R | C |
| DR test | R | R | C | A | R |
| Monitoring and alert routing | R | I | C | A | C |
| Sizing and annual technology review | C | I | C | C | R (A: BDOI Enterprise Architecture) |

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| IA-01 | AWS account structure | Three accounts: non-production (DEV, SIT, UAT), Pre-Prod, production with DR; separate keys and secrets per account | BDO IT Cloud | 30-Nov-2026 |
| IA-02 | Infrastructure as code | Terraform modules owned by BDO IT Cloud for accounts, networks, EKS, RDS, ElastiCache, MSK, S3, KMS; iorta TechNXT reviews the BIBS-specific values | BDO IT Cloud | 18-Dec-2026 |
| IA-03 | DR region | Name the DR region and approve its cross-border basis; warm standby as in chapter 7; yearly DR test (IQ26, DSQ01) | BDOI Risk and Compliance, BDO IT Cloud | 16-Oct-2026 |
| IA-04 | PROD database size | Keep the IER 2,000 GB with storage autoscaling; re-size after Trial migration 2 measures the volume without file content | DBA | 31-Jul-2027 |
| IA-05 | S3 volume estimate | Estimate from the legacy file inventory before the copy of the existing files; add the yearly growth | BDOI IT with iorta TechNXT | 31-Mar-2027 |
| IA-06 | Kafka retention | 7 days on event topics, 30 days on dead-letter topics | BDOI IT Operations | 30-Nov-2026 |
| IA-07 | Log retention | 90 days searchable in CloudWatch, one year archived, security events in the BDO SIEM per BDO policy | Information Security Office | 30-Nov-2026 |
| IA-08 | Monitoring tool | Dynatrace for APM, hosts and Kubernetes (scraping the BIBS Prometheus metrics), CloudWatch for managed services and logs (IQ30) | BDO IT Cloud | 30-Nov-2026 |
| IA-09 | Training environment | A separate database with seed data on the UAT cluster, refreshed per training wave | BDOI Training lead | 30-Jun-2027 |
| IA-10 | IER restatement | BDO IT updates the IER: Kubernetes sheets with the sizing of chapter 5, Pre-Prod and DR sheets, ElastiCache for Valkey and MSK 3.9 rows, no EFS, S3 and KMS rows (IQ25, IQ33, IQ34) | BDO IT Cloud and Digital Operations Engineering | 30-Nov-2026 |

# Glossary {-}

```glossary
AZ: Availability zone
CRR: S3 cross-region replication
DR: Disaster recovery
EKS: Amazon Elastic Kubernetes Service
IER: Infrastructure Estimation and Recommendation workbook
KMS: AWS Key Management Service
KRaft: Kafka Raft metadata mode
MSK: Amazon Managed Streaming for Apache Kafka
PITR: Point-in-time recovery
RDS: Amazon Relational Database Service
RTC: S3 replication time control
SIEM: Security information and event management
SSM: AWS Systems Manager
VDI: Virtual desktop infrastructure
```
