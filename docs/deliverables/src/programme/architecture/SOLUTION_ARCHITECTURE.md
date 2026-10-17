---
# Solution Architecture of BIBS (BRD-00, Programme/Architecture).
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py (expands the <!-- arch:... -->
# tables from architecture_data.yaml and the API figures {{...}} from the backend, then builds the Word file).
title: Solution Architecture
subtitle: Business and application architecture of BIBS, the BDOI Broker System on iNXT BrokerVerse
doc_type: Solution Architecture
doc_code: Architecture
brd: BRD-00
name: Solution Architecture
doc_id: BIBS-ARC-01
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 1 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_Solution_Architecture_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Tech Lead; iorta TechNXT Project Manager
    approver: BDOI Enterprise Architecture (pending)
    change: First issue, on the component baseline and the client decisions of 8 October 2026 (Valkey, Gateway API, broker-only scope)
distribution:
  - {name: "Enterprise Architecture", role: Approver, organisation: BDOI IT, purpose: "Architecture sign-off"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Security architecture, data scope, identity"}
  - {name: "Application Development and Integration", role: Reviewer, organisation: BDOI IT, purpose: "Interfaces, configuration ownership"}
  - {name: "Cloud and Digital Operations Engineering", role: Reviewer, organisation: BDO Unibank IT, purpose: "Hosting, IER alignment"}
  - {name: "BIBS Product Owner", role: Informed, organisation: BDOI, purpose: "Business capability map"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Architecture baseline for delivery and support"}
---

# Introduction

## Purpose and audience

This document describes the solution architecture of BIBS, the BDOI Broker System on iNXT BrokerVerse, for BDOI IT Enterprise Architecture, the Information Security Office and the application and integration teams. It explains how the business of BDO Insurance and Reinsurance Brokers, Inc. (BDOI) maps to the application, how the application is structured, how business rules are kept out of the program logic, and how the quality targets are met. It is the parent of the five other documents of the architecture set:

<!-- table: widths=1,5,11.6 caption="The architecture document set" bold=first -->
| # | Document | What it covers |
|---|---|---|
| 1 | Solution Architecture (this document) | Business and application architecture, bounded contexts, ports and connectors, events, API-first, cloud native, configurable rules, multi-entity readiness, quality attributes |
| 2 | Deployment Architecture | Runtime components, containers, Kubernetes topology per environment, Gateway API routing, scaling, configuration and secrets, release pipeline and promotion |
| 3 | Infrastructure Architecture | Hosting, network zones and trust boundaries, sizing, PostgreSQL high availability and backups, Valkey, Kafka, object storage, monitoring, environments, component baseline |
| 4 | Integration Architecture | Catalogue of every interface, patterns, security, error handling, reconciliation and ownership |
| 5 | API Specification and API Catalogue (Excel) | API standards, the OpenAPI description and the catalogue of all {{api_total}} operations |
| 6 | Architecture Decision Records and ADR Log (Excel) | {{adr_count}} decisions with context, alternatives and consequences |

## Scope

In scope: the BIBS application for BDOI as an insurance broker - the thirteen BRDs grouped in BDOI's drops (Drop 0: BRD-03, 11, 13; Drop 1: BRD-01, 02, 04, 05, 06, 09, 10, 12; Drop 2: BRD-07, 08), the platform services they share, and the interfaces with BDO systems. All modules go live together in January 2028.

Out of scope: the ReInsurance BRD (phase 2) and every insurer-company function. BIBS is broker-only: insurer underwriting, insurer claims, reinsurance treaty and cession accounting, actuarial reserves, group consolidation and insurer taxes are not part of BIBS (client decision of 8 October 2026, ADR-11).

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | BDOI BRDs 00 to 13 (latest versions of 8 October 2026) and the umbrella Core Replacement BRD (p.42-46) | Business capabilities, users, volumes, response times, retention |
| S2 | BDOI drop plan, programme timeline and the Programme Alignment pack v1.0 (26 September 2026) | Drops, integration inventory INT-01 to INT-20, questions IQ01 to IQ35 |
| S3 | BDOI IER workbook v20 | Recovery objectives, environments, sizing, growth |
| S4 | Application architecture option decision (confirmed by BDOI on 26 September 2026) | Modular monolith, workloads, edge and encryption |
| S5 | Document storage decision record (approved 26 September 2026) | S3 file store, ECM archive |
| S6 | Client decisions of 8 October 2026 | Component baseline, Valkey, Gateway API, broker-only scope |
| S7 | BIBS platform documentation: platform cache and events, company and branch data scope, user access, configuration reference, deployment guide, runbook, security controls, quality gates, developer guide | How the platform works |
| S8 | FRS of each BRD and the BIBS feature list (716 features against OOTB and best practice) | Functional baseline |

## Conventions

Open points of this document are numbered SA-01 to SA-11 (chapter 14). IQ numbers refer to the questions of the Programme Alignment pack; DCR and CRQ numbers to the discrepancy register and the umbrella BRD questions; ADR numbers to the Architecture Decision Records. Dates are written dd-MMM-yyyy in tables.

# Business architecture

## BDOI's business in one view

BDOI is an insurance broker. Insurers issue the policies; BDOI finds the cover for its clients - bank borrowers, retail and corporate clients of BDO - places it with insurers, collects the premium and remits it, earns commission and service fees, and services the client through renewal and claims. The core business flow is:

1. Marketing receives a quotation request (by e-mail, from the home loan system or by bulk upload) and quotes a package product, or raises a Proposal Request (PRF) that TSU prices with insurers for a non-package risk.
2. Marketing onboards the client (prospect, then KYC-verified client, screened against sanction and PEP lists) and creates one or more accounts. The account is the risk record, identified by one Account Reference Number (ARN) from quotation to invoice.
3. Processing checks the payment or the client's confirmation, places the account with the insurer (placement slip, hold cover) and handles insurer returns.
4. Processing receives the e-policy, records the policy number, issues the Insurance Advice for mortgaged accounts and sends the e-policy to the client, encrypted.
5. Processing books the account: the broker accounting entry, the service invoice, cost centre and incentive flag. Endorsements and cancellations follow.
6. Operations receives the premium (cashiering), applies it to invoices, remits it to the insurers net of commission, adjusts and reconciles production with the insurers; Collections follows up what is unpaid.
7. Accounting runs disbursements, the accounting control and sub-ledger (ACSL), the general ledger, BIR returns and the management reports.
8. Renewal extracts expiring policies 140 days ahead and runs them through sanitation, disposition and insurer approval; Claims handling, Employee Benefits and the Customer Servicing Facility serve clients after placement.

## Business capabilities and where they live

<!-- table: widths=4.4,2,1.6,4.6,5 caption="Business capability map" bold=first size=8 -->
| Capability | BRD | Drop | Bounded context | Main outcomes |
|---|---|---|---|---|
| Product and package maintenance | BRD-03 | Drop 0 | BC02 Product catalogue and maintenance | Versioned packages, rate and commission tables, approvals |
| User access maintenance | BRD-11 | Drop 0 | BC14 User access and identity | Access requests under four eyes, roles, data scope, sign-in |
| Data migration | BRD-13 | Drop 0 | BC15 Data migration | Legacy data loaded, reconciled and signed off |
| New business: client, quotation, placement, issuance, booking | BRD-01 | Drop 1 | BC01 Client management, BC03 New business broking | ARN from quotation to invoice; booked invoices |
| Operations: cashiering, remittance, adjustment, commission, production reconciliation | BRD-02 | Drop 1 | BC06 Operations | Receipts applied, insurers paid, production reconciled |
| Collections | BRD-04 | Drop 1 | BC07 Collections | Worklists, escalations, collection files |
| Accounting, disbursement, ACSL | BRD-05 | Drop 1 | BC08, BC09 Finance core | Payments, sub-ledger control, GL, BIR returns, reports |
| Renewal | BRD-06 | Drop 1 | BC04 Renewal | Expiring policies renewed or released |
| Customer servicing | BRD-09 | Drop 1 | BC12 Customer servicing | Contact Center cases and service requests |
| Sanction screening and risk profiling | BRD-10 | Drop 1 | BC13 Sanction screening | Screened clients, cases, STRs |
| Submitted policies | BRD-12 | Drop 1 | BC05 Submitted policies | Collateral policies approved and tracked |
| Claims handling | BRD-07 | Drop 2 | BC10 Claims handling | Claim files followed to settlement |
| Employee benefits | BRD-08 | Drop 2 | BC11 Employee benefits | EB programmes and yearly cycles |

## Users and usage

<!-- arch:nfr caption="Non-functional targets used across the architecture set" -->

The umbrella BRD counts 1,344 named users and 429 concurrent sessions by adding the rows of every BRD; Marketing users appear in several rows, so 429 is an upper bound. The architecture and the performance test use 429 concurrent sessions as the peak case until BDOI decides which figure governs (SA-02, IQ32).

# Application architecture

## Architecture at a glance

BIBS is one web application with one backend. The backend is a modular monolith (ADR-01): one Spring Boot 3.5 application on Java 21 with one package per bounded context, one PostgreSQL 16 database in which a business record, its journal, ledger and sub-ledger rows and its audit entry commit together, Valkey 8 for the reference-data cache and cluster-wide job locks, Apache Kafka 3.9 for integration events and Amazon S3 for every file. The web client is a React and TypeScript single-page application served by nginx. Users reach BIBS through the BDO network; BDO systems reach it through Apigee X.

![BIBS system context](figures/sa_context.dot){width=16}

## Architecture principles

<!-- table: widths=1,4.6,12 caption="Architecture principles" bold=first size=8.5 -->
| # | Principle | What it means in BIBS |
|---|---|---|
| P1 | One transaction per business fact | The business record, its accounting, open items, audit entry and outgoing event commit or roll back together (ADR-01, ADR-05, ADR-20) |
| P2 | Bounded contexts with enforced boundaries | One package per context; no cycles between modules; call-backs through ports owned by the caller; rules checked at every compile |
| P3 | Rules are data | Parameters, lists of values, rule tables, workflow definitions, templates and accounting rules are maintained on screens under maker-checker (ADR-19) |
| P4 | Secure by default | Every operation names its permission; the company and branch are checked against the user's data scope; the start is refused with weak or missing secrets or a plaintext connection |
| P5 | API-first | Every screen action is an operation of the backend with a typed contract; systems use versioned operations through Apigee X |
| P6 | Stateless, horizontally scaled | No session state in the pods; one image runs as three workloads (ADR-15); scheduled work runs once in the cluster under a lock |
| P7 | Managed platform services | PostgreSQL, Valkey, Kafka, object storage and keys are managed AWS services (ADR-18) |
| P8 | Traceable | Every request carries a correlation identifier into the logs, the audit trail and the events it publishes |
| P9 | Client-neutral platform | No client name, currency or logo in the program logic; the BDOI identity comes from data and the theme pack |
| P10 | Open-source baseline | Every component under a licence that allows commercial use without a fee (ADR-04) |

## Logical components

<!-- table: widths=3.6,7,7 caption="Logical components" bold=first size=8.5 -->
| Component | Responsibility | Technology |
|---|---|---|
| Web client | Screens for every persona: work queues, tree tables with row action menus, uploads, reports, approvals; theme pack with the BDOI identity | React 19, TypeScript (strict), Vite 8, served by nginx 1.30 |
| Backend - business contexts | The fifteen business bounded contexts of chapter 4 | Spring Boot 3.5 on Java 21 |
| Backend - platform services | Companies and branches, parameters, lists of values, workflow, bulk upload, messaging, document templates, attachments and files, audit, alerts, approvals, jobs, reports | Same application |
| Backend - integration and shared state | Outbox and Kafka topics, consumers, system integration operations for Apigee X, cache and job locks | Spring for Apache Kafka, Spring Data Redis client for Valkey |
| Relational store | All business data, metadata of files, outbox, audit trail | PostgreSQL 16 (Amazon RDS) |
| Cache and shared state | Reference-data cache, job locks, token deny-list, counters | Valkey 8 (Amazon ElastiCache) |
| Event streaming | Integration events with dead-letter topics | Apache Kafka 3.9 KRaft (Amazon MSK) |
| File store | Documents, attachments, reports, inbound files, migration extracts | Amazon S3 with SSE-KMS |

<!-- arch:baseline -->

# Domain model and bounded contexts

## Bounded contexts

The domain is cut into seventeen bounded contexts (domain-driven design): fifteen business contexts, each owned by one BRD, and two platform contexts. Each context is one or more backend modules (top-level packages); a module belongs to exactly one context. The table lists them; the API Catalogue lists every operation by context.

<!-- arch:contexts -->

## Context map

![Bounded contexts and their dependencies](figures/sa_context_map.dot){width=16.5}

Relationships between contexts follow three rules:

- **Upstream and downstream.** A downstream context calls the services of an upstream context (New business broking uses Client management and the Product catalogue; Operations reads the invoice ledger fed by Booking). An upstream context never calls a downstream one.
- **Ports for call-backs.** When an upstream context needs something from a downstream one, it declares an interface (a port) that the downstream context implements - for example the period-close guard of the finance core implemented by Journals, or the renewal hand-off of Submitted policies implemented by Renewal.
- **Events for facts.** Facts that other contexts react to inside the same transaction are in-process events (the Operations ledger feed from Booking); facts that leave the transaction are integration events on Kafka (chapter 6).

## Shared language

<!-- table: widths=3.4,14.2 caption="Key terms of the shared language" bold=first size=8.5 -->
| Term | Meaning in BIBS |
|---|---|
| Client | A prospect or KYC-verified client of BDOI (Client management); the accounting party is created from it |
| Account (ARN) | The risk record placed with an insurer, from quotation to invoice; one ARN per account |
| Package, package version | A product of the catalogue with a validity period; quotations and renewals use the version in force |
| Placement | The account placed with one or more insurers by slip; hold covers bridge the gap to the policy |
| Booking, booked invoice | The broker's accounting entry and invoice of a placed account or endorsement |
| Operations invoice ledger | The ledger of booked invoices that Cashiering, Remittance, Adjustment, Commission and Collections read and update |
| Business event (accounting) | A fact of an operational module (receipt, payment, booking) that the accounting engine turns into a journal |
| Integration event | A fact published on Kafka for asynchronous work and other systems |
| Data scope | The companies and branches a user may act for |

## Operations per bounded context

<!-- arch:context_api -->

# Clean and hexagonal architecture as applied

## Layers inside a module

Every module has the same inner layout. Dependencies point inward: the domain depends on nothing above it; services depend on the domain; the REST controllers depend on the services.

<!-- table: widths=2.4,7.6,7.6 caption="Layers of a module" bold=first size=8.5 -->
| Layer | Contains | Rules |
|---|---|---|
| api | REST controllers and the request and response records (Java records with bean validation) | Every operation names its permission; controllers return records, never persistent entities; no business logic |
| service | Transactional use cases, business rules, ports (interfaces) that other modules or external systems implement | Services never depend on controllers; background work is a managed job with a run history and a failure alert |
| domain | Entities that enforce their invariants, value records, enumerations, repositories | Never depends on service or api; money as BigDecimal with scale 2; maker-checker masters authorize through the entity |

![Inside a module: ports and connectors](figures/sa_hexagonal.dot){width=15}

## Rules checked at every compile

The architecture rules are executable: the pipeline fails when one is broken (ArchUnit, run with every compile and test cycle).

<!-- table: widths=6,11.6 caption="Architecture rules enforced at every compile" bold=first size=8.5 -->
| Rule | Why |
|---|---|
| No dependency cycles between modules | Each context can be understood, tested and, if ever needed, deployed on its own |
| The domain never depends on service or api | Business invariants stay independent of transport and framework edges |
| Services never depend on controllers; controllers live in api packages | Clear inbound side; one place for permissions and validation |
| Services live in service packages | Predictable layout for support teams |
| No scheduled methods; background work is a managed job | Every job is visible in the Scheduled Jobs monitor with history and a failure alert, and runs once in the cluster |
| Business dates come from the business clock (Asia/Manila by configuration); the clock is injected; no time-zone constants | "Today", cut-offs and periods follow the business zone; time can be fixed in tests |
| Seed data only in seed packages with the seed profile | SIT and UAT seed data can never load in production |
| Every controller operation that takes a company is checked against the data scope | No operation can bypass the company and branch check (ADR-08) |
| No client name or currency code in platform logic (client neutrality check of backend and web client) | The platform stays configurable for any company, branch or country |

Quality gates of the same pipeline add size and complexity limits (method 60 lines, file 600 lines, cognitive complexity 15, no copy-pasted blocks over 120 tokens), static analysis and coverage of at least 80 % of lines and 65 % of branches (Deployment Architecture, chapter 8).

## Ports and connectors

Outbound needs of a module are ports - interfaces owned by the module. The connector that implements a port is chosen by configuration, so an interface can start as a file exchange and move to an API without a change in the module. Examples:

<!-- table: widths=4.2,3.4,5,5 caption="Ports of the business modules (examples)" bold=first size=8.5 -->
| Port (business name) | Owned by | Connector in production | Purpose |
|---|---|---|---|
| File store | Platform (file storage) | Amazon S3 with SSE-KMS; local store on developer machines | Every document, attachment, report and inbound file |
| Bank channel and disbursement gateway | Disbursement, Operations | CMS / New BOB payment file (INT-07) | Outward payments and their status |
| Insurer file inbox | Operations | Inbound bucket, insurer channel (INT-19) | Insurer production, SOA and remittance files |
| File drop | Operations, Collections | Reports bucket and the BDOI file server (INT-24) | Scheduled files for BDOI units |
| Quotation request source | Package quotations | HL-LOAS through Apigee X (INT-04) | Quotation requests from source systems |
| Payment confirmation source | Placement | Confirmed payment reports | Opens the payment gate of an account |
| Watchlist feed | Sanction screening | List files (INT-21) | AML advisory and PEP lists |
| STR file sink | Sanction screening | STR file for the AMLC portal (INT-22) | Suspicious transaction reports |
| Mail transport | Messaging | SMTP relay of CCM (INT-13) | Outbound e-mail |
| Mail house gateway | Submitted policies, Renewal | Print batch for the mail house (INT-25) | Printed letters |
| Masking provider | Data migration | Keyed masking | Personal data masked outside production |
| Period-close guard, account usage checker | Finance core | Implemented by Journals and the Ledger | Call-backs inside the finance core without cycles |

## Workloads by runtime role

The same backend image runs as three workloads chosen by one variable (ADR-15): bibs-web serves the screens and user operations, bibs-jobs runs the scheduled and batch jobs, and bibs-integration runs the outbox relay, the Kafka consumers, the inbound files and the system integration operations. The modules do not change; the runtime role only switches the scheduler, the consumers and the HTTP routes on or off. The Deployment Architecture sizes each workload.

# Event-driven architecture

## Two kinds of events

- **In-process events** carry facts inside one transaction: a booking feeds the Operations invoice ledger, a workflow stage change mirrors to the record, an operational fact becomes an accounting business event that the accounting engine posts at once (ADR-20). They are synchronous and roll back with the transaction.
- **Integration events** carry facts out of the transaction, to asynchronous work (e-mail delivery, the event archive, the ECM archive) and to other systems. They go through the transactional outbox to Kafka (ADR-05).

## Transactional outbox to Kafka

![Event flow: transactional outbox to Kafka](figures/sa_outbox.dot){width=15}

- **Publishing.** The module publishes an integration event in its own transaction; the event becomes a row of the outbox table that commits or rolls back with the business change. Where a module publishes no event (client, receipt), an entity-change listener writes the outbox row just before the commit.
- **Relay.** After the commit the relay of bibs-integration is woken; a job also runs every minute for anything left and for due retries. One relay runs at a time in the cluster (job lock); rows are read in identifier order, so events of one key reach their partition in publication order. The producer waits for all in-sync replicas and uses producer idempotency. A row the broker does not acknowledge waits with a doubling back-off (from 30 seconds, at most 1 hour) and is marked failed after 10 attempts; the support screen puts it back in the queue.
- **Envelope.** Record key = business key; headers event identifier, event type and correlation identifier; value = event identifier, type, schema version, time, company, key, correlation identifier, source and the payload. Payloads carry identifiers, status and amounts, never personal data (TIN, contact details, birth date) or e-mail content.
- **Versioning.** A breaking payload change is a new topic version (.v2) published next to .v1 until every consumer has moved; added fields keep the version.

<!-- arch:topics caption="Kafka topics (each with a dead-letter topic .dlt)" -->

## Consumers, idempotency and dead letters

<!-- table: widths=3.6,6,8 caption="Consumer groups" bold=first size=8.5 -->
| Consumer group | Does | Duplicate and failure handling |
|---|---|---|
| bibs-mail-dispatch | Sends the queued e-mail of a notification event through CCM | Only messages still queued are sent, so a redelivered event sends nothing twice; a failed SMTP attempt stays queued for the mail dispatch job |
| bibs-event-archive | Stores every event envelope for traceability | Unique event identifier: a redelivery is ignored |
| bibs-dead-letter-recorder | Stores records of every dead-letter topic with the original topic, consumer group and error | Unique position; never dead-letters itself |
| ECM archive connector | Archives final records of the "archive to ECM" classes and stores the ECM reference (INT-12) | Checks the ECM reference before sending again |

Every consumer retries a failing record three times, two seconds apart, then sends it unchanged with the error in its headers to the dead-letter topic; a malformed envelope goes there at once. The Integration Events screen (System Administrator) lists outbox rows, archived events and dead letters, and offers retry and discard. Delivery is at least once; deduplication on the event identifier makes every consumer safe to replay.

## Behaviour under failure

<!-- table: widths=5,12.6 caption="What happens when a platform service fails" bold=first size=8.5 -->
| Situation | Effect |
|---|---|
| Kafka unreachable | Business transactions are not affected (they only write the outbox). Rows wait and are sent with back-off once Kafka is back; the mail dispatch job still sends queued e-mail |
| A consumer keeps failing | After the retries the record is in the dead-letter topic and on the support screen for retry or discard |
| Valkey unreachable | Caches read from the database; the token deny-list falls back to the session log in PostgreSQL; a job is not run without its lock and is recorded failed with an alert |
| A pod dies during a job | The lock lease expires within 2 minutes; the next run proceeds |
| DR failover | Kafka and Valkey are created in the DR region; events not yet delivered are resent from the outbox |

# API-first

Every screen of the web client calls typed operations of the backend; nothing is done in the browser that the backend does not check again. The same operations are open to automation and testing, and the system integration operations are a separate, versioned interface published through Apigee X.

- **User interface** - {{api_user}} operations under the prefix api/v1, plus {{api_admin}} administration operations under api/v1/admin, signed in with the BIBS access token and checked per permission and data scope.
- **System integration** - operations under the prefix integration/v1, served only by bibs-integration, accepting only Apigee X tokens with the scopes configured for each API ({{api_integration}} operation today, the connectivity check; the interfaces of the Integration Architecture are added there).
- **Contract** - an OpenAPI 3 description generated from the operations (served on developer environments only; switched off in SIT, UAT and production) and exported with each release (AP-01), and the API Catalogue of {{api_total}} operations in {{api_modules}} modules, generated from the same request mappings.
- **Standards** - versioning in the path, RFC 7807 problem details with a stable error code, page and size pagination, idempotency by business reference and record version, correlation identifier (API Specification, chapter 5).

# Cloud-native design

<!-- table: widths=4,13.6 caption="Cloud-native properties" bold=first size=8.5 -->
| Property | How BIBS meets it |
|---|---|
| One artefact, many environments | Each image is packaged once and promoted by digest; only the configuration of the environment changes (Deployment Architecture, chapter 8) |
| Configuration in the environment | All settings are environment variables; secrets come from AWS Secrets Manager; the defaults of the package are for developer machines only |
| Stateless processes | No session in the pods: short-lived access token, refresh cookie bound to a session in PostgreSQL, deny-list in Valkey; files in S3; any pod can serve any request |
| Disposability | Startup, readiness and liveness probes; graceful shutdown with a pre-stop pause so the load balancer drains a pod first; jobs finish their unit of work within a 120-second grace period |
| Horizontal scaling | Horizontal Pod Autoscaler on bibs-web and bibs-frontend; fixed, separately sized bibs-jobs and bibs-integration; cluster autoscaling of nodes |
| Backing services as attached resources | PostgreSQL, Valkey, Kafka, S3 and the SMTP relay are reached by address and credentials from the environment, with TLS |
| Schema with the release | Versioned schema migrations applied at start-up by the schema owner login; the application runs with a least-privilege login |
| Observability | Prometheus metrics on a separate management port, structured console logs with correlation identifiers, health indicators of every backing service |
| Least privilege | Non-root containers with read-only file systems and no capabilities; one IAM role per workload (IRSA); network policies deny by default |
| Immutable, scanned images | Pinned base images by digest; Trivy scan before push; SBOM per image |

# Configurable business rules

## Where business rules are configured

BIBS keeps business rules out of the program logic (ADR-19). A rule a business owner may need to change lives in data, is maintained on a screen by the role that owns it, and is audited; masters are authorized by a second user (maker-checker). A missing value stops the action with the code PARAMETER_NOT_SET instead of falling back to a coded value.

<!-- table: widths=3.6,6.6,4,3.4 caption="Configuration layers" bold=first size=8 -->
| Layer | What is configured | Where and by whom | Control |
|---|---|---|---|
| System parameters | More than 200 parameters in 25 categories (security, broking, renewal, operations, collections, EB, screening, migration and others): limits, tolerances, days, switches, sender addresses, link validity | Administration › System Parameters; System Administrator; business administrators per category | Value type and range checked; security parameters need a second approval; every change audited |
| Lists of values | 134 list types with 854 values: reasons, statuses, document types, segments, channels; effectivity dates and parent-child lists | Administration › Lists of Values; the owner permission of each list | Maker-checker; the maker never authorizes |
| Rule tables | Accounting rules and cost-centre rules; TSU routing, minimum-field matrix, document rules, rate tables and rate-scheme exceptions; payment gate rules; auto-book and incentive rules; disposition and minimal-balance rules; collection assignment and escalation rules; renewal bucket and decision rules; submitted-policy rules and limits; screening match, risk, assignment, SLA and validation rules; EB threshold rules; service fee rules; retention rules; separation-of-duties rules; migration mapping, survivorship and masking rules | The setup screens of each module; the module's administrator role | Maker-checker; effectivity dates; simulator for accounting rules |
| Workflow configuration | 42 workflows with 292 stages and 459 transitions: stage owner permission, SLA hours, allowed actions, permission per action, reason list | Workflow definitions delivered with each module and maintained as data | Changes are versioned with the release that carries them and audited |
| Exception codes and alerts | About 80 exception codes with severity, amount and day thresholds, per module | Administration › Exception Codes | Audited |
| Notifications | 90 notification events with default channels (in-app, e-mail) and user preferences | Administration › Notifications; each user for preferences | Audited |
| Document templates | 62 versioned templates (slips, advices, letters, invoices) with placeholders and effectivity | Administration › Document Templates | Versioned; effectivity dates |
| Record classes and retention | Retention per record type (years online and in archive), legal hold, archive-to-ECM classes | Administration › Retention; the DOA roles for legal hold | Changes by the DOA approvers only; audited |
| Job schedules | Time of every scheduled job (for example 20:00 booking batch and remittance extraction, 22:15 to 23:00 collection runs, 05:00 application file) | Deployment settings per environment (UTC cron); run now from the Scheduled Jobs monitor | Release record; non-production times moved into working hours by configuration |
| Client profile and theme | Legal name, short name, group name, document logo, head office code, base currency, default bank account; system name, logo and colours of the theme pack | Setup › Companies (client profile); theme pack chosen at packaging time | Maker-checker on the company |

## What stays in the program logic

Technical limits, scales and algorithm constants are not business rules and stay in the program logic after review: field lengths and list sizes, rate scales and percent bounds, date-range limits of files and screens (for example a payment notification covers at most 31 days), the client-matching scores of data migration, and the default values that the platform seeds into parameters. The client neutrality check of the pipeline refuses any client name or currency code in platform logic of the backend and the web client.

## How configuration reaches production

Configuration values are part of the release record. For each drop the configuration inputs workbook lists every parameter, list, rule table, event and template with its owner, its route into production (seeded with the release, loaded by template, or entered on screen in production) and the sign-off. Values entered in UAT are not copied to production by database copy; they follow the same route.

# Multi-company, multi-product, multi-channel and multi-country readiness

<!-- table: widths=3,7.4,7.2 caption="Readiness for more companies, branches, products, channels and countries" bold=first size=8 -->
| Dimension | How BIBS supports it | Limits and open points |
|---|---|---|
| Multi-company | Every business record carries its company; masters, numbering series, chart of accounts, periods and reports are per company; consolidated reporting reads across companies in one database | New company: set-up order company, branches, currencies, chart of accounts, dimensions, periods, parties, accounting rules, users (runbook) |
| Multi-branch | Branches (office master) per company with holiday calendars; branch on every transaction where the BRD needs it; reports by branch and unit | Whether branch scope filters company-wide reports (SA-04) |
| Data scope | Users act only for the companies and branches of their scope; checked centrally (ADR-08) | Default "all companies" for every user until an administrator narrows it |
| Multi-product | Product lines, cover types, BDOI risk products and package versions with validity periods; rate, commission and tax tables; minimum-field matrix and TSU routing per product - all data | A new line of business needs its accounting rules and document templates configured |
| Multi-channel | Web client for users; system integration interface through Apigee X; file channels for banks, loans, insurers and BDOI units; e-mail through CCM; in-app notifications | SMS is not required by any BRD; it would be a further messaging channel (IN-05) |
| Multi-currency | Currency master, rate types and dated exchange rates; base and transaction currency on every accounting line; rounding differences corrected deterministically; base currency per company | Revaluation rules per currency configured by Accounting |
| Taxes | Tax codes per company with rates and dates (VAT, EWT by ATC, DST); tax forms as master data (frequency, due rule, accounts); BIR certificates and returns of the broker | Other countries' forms need their own form definitions and reports |
| Time zone and calendar | Business zone setting (Asia/Manila) for "today", cut-offs, periods and job dates; timestamps in UTC; holiday calendars per branch | One business zone per deployment |
| Languages | English, the language of the BRDs; dates and amounts formatted in one place for the locale (dd-MMM-yyyy, thousands separators); user-facing texts kept in one place per module | A second screen language is a change request (SA-06) |
| Client identity | Client profile and theme pack (system name, logos, colours); no client name in platform logic | - |

# Quality attributes

<!-- table: widths=2.6,4.4,10.6 caption="Quality attributes and how they are met" bold=first size=8 -->
| Attribute | Target | How it is met |
|---|---|---|
| Performance | Screens p95 under 2 s; every role under 5 s; reports per BRD | Reference-data cache on Valkey; daily balances for trial balances and statements; paged lists; reports and extracts run as jobs with results in S3; downloads by presigned link, not through the pods; performance test at 429 concurrent sessions on Pre-Prod (Nov - Dec 2027) |
| Scalability | 15 % growth a year over 60 months | Horizontal scaling of bibs-web (3 to 10 pods in PROD); separate sizing of jobs and integration; cluster autoscaling; RDS vertical scaling and storage autoscaling |
| Availability | 99.9 % in service hours | Three availability zones; at least two pods per user-facing workload with disruption budgets; RDS Multi-AZ; Valkey primary and replica; three Kafka brokers; rolling updates with no unavailable pod |
| Recoverability | RPO 15 minutes, RTO 4 hours | Point-in-time recovery, 4-hourly snapshots, cross-region read replica and S3 replication; outbox resends events; DR runbook and yearly failover test (Infrastructure Architecture, chapter 7) |
| Security | BDO security standards; BRD-11 | SSO with EIAM, TOTP for privileged users, 15-minute access tokens, permissions on every operation, data scope, maker-checker and separation of duties, TLS on every hop, KMS encryption with BDOI keys, WAF, network policies, security gates in the pipeline |
| Auditability | Every change traceable | Insert-only audit trail protected by database triggers, written in the same transaction; access change log; immutable ledger with reversal-only corrections; correlation identifier from request to event |
| Data integrity | Ledgers agree to the centavo | One transaction per business fact; balanced-journal invariant; optimistic locking (409 on a stale update); gapless document numbers; SHA-256 on every file |
| Maintainability | Support by BDOI IT and iorta TechNXT for 10+ years | Same layout in every module; architecture rules and complexity limits enforced; Javadoc on public types; configuration instead of code changes |
| Operability | Supportable without the delivery team | Job monitor with run history, Integration Events screen, exception codes and alerts, health indicators, runbook per module |
| Portability | Managed Kubernetes and standard services | Standard Kubernetes resources and Gateway API; PostgreSQL, Valkey and Kafka protocols; S3 interface |
| Usability and accessibility | BDO UX guidelines; WCAG | One design system and theme; accessibility rules checked by the web client linter (jsx-a11y strict) |

# Security architecture summary

Security is part of every chapter; the essentials for the Information Security Office:

- **Identity.** Single sign-on with EIAM by OpenID Connect (SAML 2.0 supported); identities linked to existing active BIBS users only; break-glass administrators with a local password and TOTP (ADR-09). Accounts provisioned from UIDM-ISC (ADR-10, proposed).
- **Authorization.** Fine-grained permissions on every operation ({{api_permissions}} permission codes in use), roles as permission bundles granted through access requests under four eyes; company and branch data scope (ADR-08); maker-checker and authorization limits; separation-of-duties rules.
- **Sessions.** Access token of 15 minutes (security parameter, 5 to 60), refresh cookie rotated on every renewal and bound to the session, inactivity time-out and an absolute end after 8 hours; sign-out ends the session at once; tokens refused when the session cannot be checked.
- **Data protection.** TLS 1.2 or higher on every hop; RDS, S3, Valkey, MSK and backups encrypted with BDOI-owned KMS keys; personal data masked outside production; no personal data in object keys or event payloads.
- **Assurance.** Security gates in every pipeline run (ADR-13) with time-boxed exceptions (ADR-14); penetration test on Pre-Prod in November - December 2027 (IQ35).

# Roles and responsibilities

<!-- table: widths=5.6,2,2,2,2,2,2 caption="Architecture responsibilities (R responsible, A accountable, C consulted, I informed)" bold=first size=8 -->
| Activity | BDOI Enterprise Architecture | BDOI IT Security | BDOI IT Cloud | BDOI business owners | iorta TechNXT Solution Architect | iorta TechNXT Tech Lead |
|---|---|---|---|---|---|---|
| Architecture decisions (ADR) | A | C | C | I | R | C |
| Component baseline and annual technology review | A | C | C | I | R | R |
| Security architecture and exceptions | C | A | C | I | R | R |
| Bounded contexts and module boundaries | I | I | I | C | A | R |
| Business rules as configuration (values) | I | I | I | A | C | R |
| Interface specifications | C | C | C | C | A | R |
| Infrastructure provisioning | C | C | A | I | C | R |
| Release approval to production | C | C | R | A | C | R |

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| SA-01 | IER restatement | BDOI IT restates the IER architecture and Kubernetes sheets with the BIBS architecture of this set (four workloads, managed services, no microservices) and adds the Pre-Prod and DR sheets (IQ25, IQ33) | BDOI IT Cloud and Digital Operations Engineering | 30-Nov-2026 |
| SA-02 | Sizing basis | 429 concurrent sessions as the peak case of the performance test and of the PROD sizing; 1,344 named users (CRQ21, IQ32) | BDOI Program Manager | 30-Nov-2026 |
| SA-03 | Response-time targets | Screens p95 under 2 seconds and every role under 5 seconds; reports and batch at the value of each BRD | BRD owners | 30-Nov-2026 |
| SA-04 | Data scope business options | (1) any holder of user maintenance with an approved request may narrow a scope; (2) branch scope filters only requests that name a branch; (3) a restricted user's home branch is always in the scope; (4) a scope change carries the risk flag and needs a second approval | BRD-11 owner, Information Security Office | 30-Nov-2026 |
| SA-05 | UIDM-ISC provisioning | Option (a) of ADR-10: accounts from UIDM-ISC through SCIM 2.0, roles and data scope as BIBS requests (IQ05) | BDOI IT Identity Governance | 16-Oct-2026 |
| SA-06 | Screen language | English only; no second language at go-live | BIBS Product Owner | 30-Nov-2026 |
| SA-07 | Spring Boot 3.5 support | Decide at the first technology review (31-Jan-2027) between the move to Spring Boot 4.x before UAT and commercial support of 3.5, on the upstream support dates then published | BDOI Enterprise Architecture | 31-Jan-2027 |
| SA-08 | Node 22 end of life (30-Apr-2027) | Move the web client pipeline to Node 24 LTS by 31-Mar-2027 (no production impact: Node does not run in production) | iorta TechNXT Tech Lead; informed BDOI EA | 31-Mar-2027 |
| SA-09 | Retention exceptions | Platform rule 5 years online and 15 offline; BRD-7 10 / 15; BRD-5, BRD-10, BRD-12 5 / 5 (CRQ22) | BRD owners, Compliance | 30-Nov-2026 |
| SA-10 | Reviewer of CVE exceptions | The BDOI Information Security Officer approves every accepted finding before a production release (ADR-14) | Information Security Office | 31-Dec-2026 |
| SA-11 | Availability target | 99.9 % in service hours 06:00-22:00 Monday to Saturday; maintenance window 00:00-04:00 | BDOI IT Operations | 30-Nov-2026 |

# Glossary {-}

```glossary
ABAC: Attribute-based access control (company and branch data scope)
ACSL: Accounting Control and Sub-Ledger (BRD-5)
ADR: Architecture decision record
ARN: Account Reference Number
BC: Bounded context
CCM: Centralized Communications Management (e-mail sending)
DOA: Delegation of Authority
EIAM: Enterprise Identity Access Management (Microsoft Entra ID)
IER: Infrastructure Estimation and Recommendation workbook
IRSA: IAM roles for service accounts (Amazon EKS)
KRaft: Kafka Raft metadata mode (no ZooKeeper)
MSK: Amazon Managed Streaming for Apache Kafka
OIDC: OpenID Connect
PRF: Proposal Request Form
RPO / RTO: Recovery point objective / recovery time objective
SCIM: System for Cross-domain Identity Management
TOTP: Time-based one-time password (authenticator app)
TSU: Technical Support Unit
UIDM-ISC: User ID Maintenance - Identity Security Cloud (identity governance)
```
