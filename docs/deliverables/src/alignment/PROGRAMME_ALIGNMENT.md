---
# Programme alignment: BDOI drops, timeline, integrations and infrastructure against BIBS (BRD-00).
# Build: python docs/deliverables/src/alignment/build_alignment_pack.py (expands the <!-- al:... --> tables from
# alignment_data.yaml, builds this document with tools/deliverables/bdoi_docx.py and the Integration Inventory workbook).
title: Programme Alignment - Drops, Integrations and Infrastructure
subtitle: BDOI drop plan, timeline and IER workbook against BIBS
doc_type: Programme Alignment
doc_code: Alignment
brd: BRD-00
name: Drops Integrations Infrastructure
doc_id: BIBS-ALN-BRD-00
version: "1.0"
date: 26 September 2026
status: Issued for BDOI review
header_title: Programme Alignment - Drops, Integrations, Infrastructure
output: Alignment/BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v1.0.docx
control:
  - version: "1.0"
    date: 26 Sep 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Program Manager (pending)
    change: First issue, from the drop plan and timeline slides, the IER workbook v20, the early renewal concept paper and the BDOI answers of 26 September 2026
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Drops, timeline, cut-over"}
  - {name: "BDOI IT (integration owners)", role: Approver, organisation: BDOI, purpose: "Integration inventory and open questions IQ04-IQ19"}
  - {name: "Cloud and Digital Operations Engineering (IER owner)", role: Approver, organisation: BDO Unibank IT, purpose: "Infrastructure alignment and the IER changes"}
  - {name: "Product Owner, Marketing Business System", role: Reviewer, organisation: BDOI, purpose: "Drop mapping, renewal transition"}
  - {name: "Head, Comptrollership; Head, Operations", role: Reviewer, organisation: BDOI, purpose: "Drop 1 downstream, EGL, bank channels"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery plan, test and migration plan"}
---

# Introduction

## Purpose

BDOI has issued a drop plan, a programme timeline, an infrastructure estimation and recommendation (IER) workbook and a concept paper on early renewal processing. This document sets each of them against BIBS (BDOI Broker System, on iNXT BrokerVerse) and its FRS, so that the requirements, delivery, tests and infrastructure follow BDOI's drops and process. For each topic it states what fits, what does not, what we deliver, and what we need from BDOI and by when.

## Sources

<!-- table: widths=1,6,6 caption="Sources" bold=first -->
| Ref | Document | Content used |
|---|---|---|
| S1 | Drop plan slide ("BDOI Drop Plan - modules and integrations") and its transcription | Drops 0, 1 and 2, the BRD column, the integrations per drop |
| S2 | Programme timeline slide | Requirements, development, SIT, UAT, migration, ORR / PRR bars; go-live January 2028 |
| S3 | IER workbook v20 (FR-ITC-ENG007) | RPO / RTO, HW and SW requirements, VDI, architecture diagrams, environment and Kubernetes sheets, summary |
| S4 | Concept paper "Advance Implementation of Renewal Processing" V1.0, dated 06/09/2026 | Early renewal release by 15 August 2027 (superseded, see 1.3) |
| S5 | BDOI answers and integration list of 26 September 2026 | A1 to A6 below |
| S6 | Document storage proposal (26 September 2026) | S3 for file content, PostgreSQL for metadata |

## BDOI answers of 26 September 2026

<!-- table: widths=1,8,5 caption="BDOI answers of 26 September 2026" bold=first -->
| Ref | Answer | Effect on this document |
|---|---|---|
| A1 | All modules go live together in January 2028. There is no early go-live. Drops split the requirements, delivery, test and sign-off workload; the platform goes to UAT complete, end to end | The concept paper is recorded as superseded (chapter 3); no separate release |
| A2 | Integration names and meanings (EIAM, UIDM-ISC, ECM, CCM, M365, LMS, HL-LOAS, LFS SaaS, PMS, CMS / New BOB, OBPCS, Old BOB, TFS, EDP, EGL, Bridger Insight XG); all touch-points are owned by BDOI IT | Used in the integration inventory (chapter 5) |
| A3 | Remittance handles the reinsurance transactions in Drop 1; the reinsurance module is phase 2 | Drop item 1.D3 (IQ20) |
| A4 | Documents and attachments are stored in an S3 bucket only | Section 6.3 |
| A5 | BRDs, FRS, test plans and collaterals are grouped under the drops | Drop-to-document map, section 2.4 |
| A6 | BDOI supplies the diagrams for the "Architecture Diagram" sheets of the IER | Figures 3 and 4, delivered as PNG files |

## Conventions

The ReInsurance BRD is phase 2. Questions to BDOI are numbered IQ01 to IQ35 (chapter 7); register items DCR-210 to DCR-235 are added to the discrepancy register.

# Drop mapping

## The drops at a glance

Figure 1 shows each drop item with the BIBS BRD that specifies it and the integrations of the drop.

![Drop items and the BIBS BRDs](figures/al_drop_map.dot){width=15.5}

<!-- landscape -->

## Drop items against BRDs, FRS and BIBS

<!-- al:drop_items drop="Drop 0" caption="Drop 0 - Setup and Data Migration" -->

<!-- al:drop_items drop="Drop 1 upstream" caption="Drop 1 - Transactional, upstream (product inherent)" -->

<!-- al:drop_items drop="Drop 1 downstream" caption="Drop 1 - Transactional, downstream (product agnostic)" -->

<!-- al:drop_items drop="Drop 2" caption="Drop 2 - Independent" -->

<!-- portrait -->

## Gaps and mismatches in the drop plan

<!-- table: widths=0.8,7.4,5.6,2 caption="Gaps and mismatches in the drop plan" size=8.5 bold=first -->
| # | Finding | Proposal | Refs |
|---|---|---|---|
| M1 | **Reinsurance under Remittance (1.D3).** The slide names the Reinsurance BRD for Remittance; the ReInsurance BRD is phase 2. BDOI answered that Remittance handles the reinsurance transactions (A3), but which transactions and with which accounting is not written anywhere | BDOI names the transactions and supplies the "Remittance Addendum"; BIBS handles them in Remittance as insurer-like counterparties; the treaty and facultative module stays phase 2 | IQ20, DCR-210 |
| M2 | **Employee Benefits "no portal feature" (2.4).** BRD-8 (BRID-005, 005.01, 014) and FRS BRD-8 use an insurer and client-HR portal; the IER diagram shows an "EB Insurer Portal" tenant | No portal for EB; insurers and client HR send documents by e-mail and EB users upload them; FRS BRD-8 re-issued | IQ22, DCR-211 |
| M3 | **"Marketing Collection (extraction)" in Drop 2.** BRD-4 Collections is a BIBS module fed by the Operations ledger; no extraction is needed inside BIBS. The item may assume the legacy Collection Management System stays | BDOI confirms that BRD-4 in BIBS replaces the Collection Management System; the item then means the Collections daily files | IQ21, DCR-212 |
| M4 | **Claims and Production Reconciliation in Drop 2.** Both are specified. No conflict: they are tested in SIT Drop 2 (Jul - Sep 2027); Production Reconciliation is a chapter of the BRD-2 FRS, which is otherwise in Drop 1 | Split the BRD-2 test plan by chapter | - |
| M5 | **Data Management, Workflow, Accessibility & Login (Drop 0) have no BRD.** Data Management is umbrella capability CORE-17 (MIS fields and insurer management, proposed in FRS BRD-00); Workflow is platform; "Accessibility" can mean sign-in or WCAG accessibility | Use CORE-17, BRD-11 plus EIAM, and each BRD's workflows as the acceptance basis; the accessibility statement (deliverable 40) covers WCAG | IQ24, DCR-214 |
| M6 | **BRDs not on the slide.** BRD-10 Sanction Screening and the CSF service-request functions are in no drop | BRD-10 in Drop 1 with Client Onboarding; CSF service requests in Drop 2 | IQ23, DCR-213 |
| M7 | **Documents we do not have.** "Operations BRD (Cashiering Addendum)" and "(Remittance Addendum)" are named on the slide; the BIBS pack has only Operations Addendum 1 (RMTID.002, ADJID.014) | BDOI supplies them | IQ20, DCR-215 |
| M8 | **EB in Drop 1.** Placement & ePolicy and Reports (upstream) name Employee Benefits, while the EB module is Drop 2 | EB programmes are placed through the NB placement screens in Drop 1; EB servicing in Drop 2 | IQ22, DCR-235 |
| M9 | **One system in two drops.** HLS (Drop 0) and LOAS (Drop 2) are HL-LOAS | One interface specification, delivered in Drop 0 | IQ07, DCR-216 |
| M10 | **CMS means two things.** In the BIBS documents CMS is the Collection Management System (BRD-4); on the slide it is the Cash Management System (outward payments) | Use "CMS / New BOB" for the bank channel and "Collection Management System" in full | DCR-221 |

## Drop-to-document map

BDOI groups the BRDs, FRS, test plans and collaterals under the drops (A5). The folder restructure follows the column "Drop (folder)"; documents used by more than one drop stay in one folder and are cross-referenced. Programme documents sit in a Programme folder.

<!-- al:documents -->

# Early renewal concept paper

## What the paper proposed

The concept paper (S4), prepared by the Project Manager and reviewed by the BU Project Lead (signature dates 06/09/2026), proposed a pre-cut-over release of renewal processing by 15 August 2027, for policies expiring from January to May 2028. The date matches the renewal lead time: 140 days before 1 January 2028 is 14 August 2027. It included the renewal core (RMEL ingestion, sanitation, disposition, proposal, insurer approval, renewal advice, NAL and NRL), TSU Product / Package Maintenance and quotation, Submitted Policy and Free First Year renewal, and the early migration of the client master and renewal reference data. Placement, booking and the back-office functions were to wait for the January 2028 cut-over.

## Status: superseded

BDOI answered that every module goes live together in January 2028 and that there is no early go-live (A1). The paper is therefore recorded as superseded as an operating model. Nothing is prepared for a separate early release: no pre-cut-over production environment, no early client migration, and no legacy-to-BIBS client synchronisation for August to December 2027.

The capabilities the paper lists remain in scope for January 2028. The table below maps them to the FRS.

<!-- al:concept -->

## What remains from the paper

**Renewals around the cut-over (IQ02).** Legacy keeps extracting, disposing, placing and booking renewals until the freeze. At go-live (T), the RMEL cohorts expiring from T to T + 140 days are already in disposition in legacy; they are carried forward into BIBS Renewal with their disposition, handler and status (migration object P03), and renew on the new-business path pre-filled from the migrated header. Later cohorts are extracted by BIBS. Renewals expiring between the freeze and T must be placed in legacy before the freeze or held by hold covers.

**Package remapping (IQ03).** The paper's first risk remains: legacy package names must map to the BIBS packages, for the migrated headers (P01), the carried-forward cohorts (P03) and the package versions (R06). Options:

<!-- table: widths=2.2,5,3.6,3.6 caption="Package remapping options" size=8.5 bold=first -->
| Option | How | For | Against |
|---|---|---|---|
| 1. At upload | A versioned PACKAGE code map applied at migration intake; unmapped codes stop the batch | One decision per legacy package, signed at gate G2, reconciled | A package that needs a per-policy decision (split packages) cannot be mapped one to one |
| 2. In sanitation | A renewal check proposes the package per candidate; TSU confirms | Handles per-policy cases | Thousands of manual decisions; inconsistent results |
| **3. Both (recommended)** | Code map at intake for every one-to-one mapping; a renewal sanitation check sends candidates with an unmapped, retired or split package to the Review bucket for TSU; each TSU decision can add a code map version | Deterministic for the bulk, controlled for exceptions | Two places to explain in training |

Recommendation: option 3, owned by TSU (Product Maintenance) with the MBS data steward; the mapping rules are agreed before the first full extract (15 January 2027) and tested with sample RMEL files in Trial migration 1.

**The early renewal release.** Not planned (A1). Renewal follows the drops: it is delivered with Drop 1 (November 2026 to February 2027) and tested in SIT and UAT Drop 1.

# Timeline alignment

## BDOI timeline against the BIBS plan

The slide (S2) runs requirements and development in three periods of about two months: first setup and upstream, then downstream, then the independent modules. Read from the bars, the requirements of Drop 1 run from September to November 2026 (the transcription says September to October) and those of Drop 2 from December 2026 to February 2027. Drop 0 has no bar of its own; it is part of the first period.

The BDOI development windows cover the Drop 1 and Drop 2 BRDs, the integrations, the S3 document store and the changes that come out of the FRS sign-off.

<!-- al:timeline -->

## Go-live date

BDOI has fixed January 2028 but not the day (IQ01). The migration strategy takes T as the first business day of a month after a legacy month-end close. For January 2028 this gives **Monday 3 January 2028**: legacy freeze after the EOD of Wednesday 29 December 2027 (30 December, 31 December and 1 January are non-working days), final extracts and loads over the long weekend, go / no-go on Sunday 2 January. The legacy December and year-end close must then be complete in legacy before the final GL extract. The slide places the go-live marker at the end of January; a later date in January needs a short January period in the GL opening (DMQ18).

## What each drop needs from us, and from BDOI

<!-- table: widths=2.6,7,7 caption="What each drop needs, and by when" size=8.5 bold=first -->
| When | From iorta TechNXT | From BDOI |
|---|---|---|
| By 16 Oct 2026 | This document; register rows DCR-210 to DCR-235 | Priority 1 answers (IQ01, IQ04, IQ05, IQ10, IQ15, IQ16, IQ20, IQ22, IQ25, IQ26, IQ31) |
| By 30 Nov 2026 | FRS v1.1 of the Drop 0 and Drop 1 BRDs with BDOI comments answered | FRS sign-off, Drop 0 and Drop 1; migration object decisions (G1) |
| By 18 Dec 2026 | Test plans of Drop 0 and Drop 1 re-issued under the drops; SIT entry criteria | Test plan approval; SIT environment (EKS, RDS, ElastiCache, MSK, S3) |
| By 15 Jan 2027 | Interface specifications drafted from the IQ answers | First full migration extracts; interface layouts from BDOI IT |
| By 26 Feb 2027 | Drop 1 ready for SIT; FRS v1.1 of Drop 2 | FRS sign-off, Drop 2 |
| By 30 Apr 2027 | Drop 2 ready for SIT; Migration Console ready (31 Mar) | Integration test connections for EGL, EDP, insurer channels |
| Apr - Jul 2027 | Trial migration 1 and Trial migration 2 in SIT | Extracts per trial migration; data steward reviews |
| By 30 Jul 2027 | UAT readiness statement, UAT plans and sign-off forms; security scan in SIT | UAT users set up through EIAM and UIDM-ISC |
| Aug - Dec 2027 | UAT support on migrated data (Trial migration 3 in August, re-load in October) | UAT execution and sign-off per drop |
| By 1 Oct 2027 | Performance test plan and scripts | Pre-Prod environment, production-sized |
| Nov 2027 - Jan 2028 | Performance and penetration test fixes; dress rehearsal on Pre-Prod; ORR / PRR evidence by 15 Dec 2027 | Penetration test, ORR / PRR, go / no-go |

# Integration inventory

## Context

Figure 2 shows BIBS and the systems BDOI named. All external touch-points are owned by BDOI IT (A2). The meanings are BDOI's; "our understanding" is the project team's reading and is confirmed through the IQ questions. The Integration Inventory workbook holds the same rows with every column.

![BIBS integration context (all touch-points owned by BDOI IT)](figures/al_integration_context.dot){width=14}

<!-- landscape -->

## Inventory

<!-- al:integrations -->

<!-- al:integration_detail -->

<!-- portrait -->

## Interfaces named in the BRDs but not on the slide

<!-- al:other_interfaces -->

## Integration summary

- **Drop 0:** EIAM sign-in (OIDC), UIDM-ISC provisioning, CCM (configuration if SMTP), bank and PDC file layouts (configuration of the payment-file uploads), CMS / New BOB outward payment file, HL-LOAS quotation requests.
- **Drop 1:** LMS and LFS intakes with Submitted Policies and Renewal.
- **Drop 2:** EGL extract, EDP ingestion per SD 11, insurer channels, CARMS (unknown), Bridger evidence set-up.
- **Not in scope:** ECM storage (A4), ICBS (struck through).

# Infrastructure alignment

## What the IER says

The IER (S3) sets RPO 15 minutes and RTO 4 hours; RDS PostgreSQL 16 (Single-AZ in DEV, SIT, UAT; Multi-AZ in Pre-Prod and PROD, 2,000 GB in PROD), ElastiCache Redis 7 and Amazon MSK (Kafka 3.6, three brokers in Pre-Prod, PROD and DR), a GitLab runner and SSM bastion, S3 as document store and EFS as shared volume; a DR standby with asynchronous replication; 15 VDI users; 12 x 5 hours for non-production; 15 % growth a year over 60 months. It matches the BIBS technology baseline for the database and event streaming. For the cache, BIBS uses Valkey 8 (Amazon ElastiCache for Valkey) in place of Redis 7, and Kafka 3.9 in place of 3.6 (component baseline below).

It does not match BIBS on the application: the diagram in the "Architecture Diagram" sheet and the Kubernetes sheets describe ten microservices, three tenants, Aurora and MongoDB, Apigee, Istio and EventBridge. BIBS is one modular monolith whose business record, journal and ledger rows commit in one database transaction; splitting it would break that guarantee. The IER should describe the BIBS architecture (IQ25). Figures 3 and 4 are the diagrams for the two "Architecture Diagram" sheets (A6).

<!-- landscape -->

![BIBS application architecture (for the IER "Architecture Diagram" sheet)](figures/al_application_architecture.dot){width=24}

![BIBS infrastructure and deployment (for the IER "Architecture Diagram (Infra)" sheet)](figures/al_deployment.dot){width=24}

## IER against BIBS

<!-- al:infra -->

<!-- portrait -->

## BIBS component baseline

The client confirmed the component baseline on 8 October 2026. Valkey replaces Redis, whose releases from 7.4 on are no longer under the BSD licence; the Kubernetes Gateway API replaces ingress-nginx, which the Kubernetes project retired in March 2026.

<!-- table: widths=3.4,3.4,3.6,6.6 caption="BIBS component baseline" size=8.5 bold=first -->
| Component | Version baseline | Licence | Function |
|---|---|---|---|
| PostgreSQL | 16.x | PostgreSQL Licence | Relational data store |
| Valkey | 8.x | BSD-3-Clause | Cache, job locks, token deny-list, counters |
| Apache Kafka | 3.9 (KRaft) | Apache 2.0 | Event streaming, transactional outbox |
| OpenJDK (Eclipse Temurin) | 21 LTS | GPLv2 with Classpath Exception | Application runtime |
| Spring Boot | 3.5.x | Apache 2.0 | Application framework |
| React | 19.x | MIT | Web user interface |
| Node (JavaScript runtime) | 22 LTS | MIT | Compiles the web user interface for release (not a production runtime) |
| nginx (unprivileged image) | 1.30 | BSD-2-Clause | Serves the web user interface, proxies the API |
| Kubernetes (managed) | Provider-supported version | Apache 2.0 | Container orchestration |
| Gateway API controller | Gateway API v1 controller: the managed provider's controller or Envoy Gateway | Apache 2.0 | Ingress and traffic routing |

**Support horizon.** Each component stays within the support window its upstream project publishes: PostgreSQL 16 until 9 November 2028, Eclipse Temurin 21 until at least December 2029, Node 22 until 30 April 2027, each Kubernetes version on Amazon EKS for 14 months of standard support; the end dates of Valkey 8, Kafka 3.9, Spring Boot 3.5, nginx 1.30 and the gateway controller are to be confirmed at each annual technology review. The baseline is reviewed every year and before go-live, and a component that reaches its support end within the following twelve months is planned for upgrade.

## Documents in S3 only

BDOI's instruction (A4) is met by the document storage proposal (S6): option C, S3 for the file content and PostgreSQL for the metadata, 5-minute signed links issued after the BIBS permission check and audit, four buckets per environment (documents, reports, inbound quarantine, migration with 5-day expiry), SSE-KMS, IRSA and a VPC gateway, Object Lock in governance mode with legal hold, and cross-region replication for DR. Existing files are copied and verified before go-live. The table below lists the proposal and the changes in the IER.

<!-- al:s3 -->

## Kubernetes sizing of the BIBS workloads

The IER Kubernetes sheets size ten bv-* services. BIBS runs two deployments. The table below replaces those rows; the values are a starting point to be proved by the performance test at the peak case (429 concurrent sessions until DCR-166 is answered). Scheduled jobs run once per schedule whatever the number of pods, through the Valkey job lock. The cluster add-ons of the IER sheets (kube-system, monitoring and EDR agents) stay, with a Gateway API controller in place of ingress-nginx; the EFS controller is not needed.

<!-- al:k8s -->

## Environments, hours and disaster recovery

- **Six environments** are needed: DEV, SIT, UAT, Pre-Prod (dress rehearsal and performance test), PROD and DR. The IER has Kubernetes sheets for DEV, SIT, UAT and PROD only, and its Pre-Prod sheet is titled "UAT ENVIRONMENT" (IQ33).
- **12 x 5 non-production hours** do not cover the BIBS night jobs (EOD booking and remittance extraction at 20:00, Collections files from 22:15 to 23:00, renewal extraction and screening at 01:00, application file at 05:00). In non-production the schedules are moved into the working window by configuration, and extended hours are booked for batch and month-end test cycles, trial migrations (weekend loads) and the dress rehearsal. MSK and ElastiCache cannot be stopped, so only the EKS nodes and RDS instances follow the 12 x 5 hours (IQ27).
- **RPO 15 minutes and RTO 4 hours** answer register item DCR-135 (the register proposed the same values); RDS point-in-time recovery and the cross-region replica meet the RPO, and S3 replication with replication time control covers the documents.
- **DR region.** The HW sheet names a cross-region read replica; the diagram names ap-southeast-1 with a warm standby; the hosting appendix places BIBS in ap-southeast-1 with access restricted to personnel in the Philippines. The DR region and data residency must be confirmed (IQ26). At failover the backend is scaled up in DR, Valkey is re-created (caches refill; revoked tokens are lost until they expire, at most 8 hours) and MSK is recreated; events not yet delivered are resent from the database outbox.

## Changes needed

<!-- table: widths=2.4,11.6,3 caption="Changes needed" size=8.5 bold=first -->
| Area | Change | Owner |
|---|---|---|
| IER | Replace the architecture diagrams with Figures 3 and 4; restate the Kubernetes sheets with the sizing of section 6.4; add Pre-Prod and DR cluster sheets; retitle the Pre-Prod sheet; replace the template rows of the environment cost sheets; remove EFS; add the S3, KMS, VPC gateway, replication and malware-scanning rows; "RHEL 9.x" removed from the RDS rows; ElastiCache for Valkey 8 with "cluster mode disabled"; MSK Kafka 3.9; a Gateway API controller in place of ingress-nginx; VDI software list with Temurin 21, Maven 3.9 and Node 22 | BDOI IT with iorta TechNXT |
| BIBS | S3 document store and the copy of the existing files; EIAM OIDC sign-in; UIDM-ISC provisioning; bank channel files; EGL and EDP extracts; non-production schedules by configuration | iorta TechNXT |
| BIBS deployment | Helm chart or Kustomize overlays per environment (replicas, HPA, PDB, IRSA service account, topology spread); Kafka replication factor 2 on 2-broker MSK in DEV and SIT; pipeline on the BDO toolchain (IQ29) | iorta TechNXT |
| Documents | Bill of materials and deployment architecture (deliverables 4 and 12) from this chapter; security mapping (deliverable 26) with EIAM, UIDM-ISC and S3 encryption; DR runbook | iorta TechNXT |

# Open questions for BDOI

Priority 1 questions shape the scope and are needed by 16 October 2026; priority 2 by 30 November 2026; priority 3 before the delivery of the item concerned.

<!-- al:questions -->

# Glossary {-}

```glossary
BIBS: BDOI Broker System, on iNXT BrokerVerse
CCM: Centralized Communications Management (e-mail sending)
CMS / New BOB: Cash Management System, outward payments
DR: Disaster recovery
EDP: Enterprise Data Platform
EGL: Enterprise General Ledger
EIAM: Enterprise Identity Access Management (Entra ID)
EKS: Amazon Elastic Kubernetes Service
FFY: Free First Year
Gateway API: Kubernetes standard for routing traffic into the cluster, successor of Ingress
HL-LOAS: Home Loan System (Loan Origination and Admin)
IER: Infrastructure Estimation and Recommendation workbook
IRSA: IAM Roles for Service Accounts (EKS)
LFS SaaS: Loan Front-End System
LMS: Loans Management System
MSK: Amazon Managed Streaming for Apache Kafka
OBPCS: Online Bills Payment Consolidation System
Old BOB: Old Business Online Banking Collection System
ORR / PRR: Operational / production readiness review
PMS: PDC Management System
RMEL: The expiring-policy list extracted for renewal
RPO / RTO: Recovery point objective / recovery time objective
TFS: Trade Finance System
UIDM-ISC: User ID Maintenance - Identity Security Cloud (IGA)
Valkey: Open-source in-memory data store (BSD-3-Clause, Linux Foundation, continues Redis) used for the cache
```
