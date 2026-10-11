---
# Non-Functional Requirements of BIBS (BRD-00, programme level); companion of the NFR register workbook.
# Build: python docs/deliverables/src/programme/requirements_quality/build_requirements_quality.py --only nfr
# The NFR tables are built from nfr_catalogue.yaml (<!-- rq:nfr_<category> --> placeholders), so the Word document,
# the register and the targets quoted in the SRS and the Test Strategy are the same.
title: Non-Functional Requirements
subtitle: Measurable quality targets of BIBS with verification, owners and test types
doc_type: Non-Functional Requirements Catalogue
doc_code: NFR
brd: BRD-00
name: Non Functional Requirements
doc_id: BIBS-NFR-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: NFR - BIBS Non-Functional Requirements
h1_page_break: true
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT QA lead; iorta TechNXT Security Lead
    approver: BIBS Product Owner (pending)
    change: First issue; reconciles the non-functional chapters of the FRS, the NFR comparison of the discrepancy register v1.2 and the IER workbook v20
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Decisions on the conflicting BRD values (XQ08)"}
  - {name: "Head, BDOI IT", role: Approver, organisation: BDOI, purpose: "Availability, recovery, observability and operations targets"}
  - {name: "Cloud and Digital Operations Engineering (IER owner)", role: Reviewer, organisation: BDO Unibank IT, purpose: "Capacity, scalability, DR and the IER changes"}
  - {name: "BDOI Information Security; Data Protection Officer", role: Reviewers, organisation: BDOI, purpose: "Security and privacy targets"}
  - {name: "Product owners of BRD-1 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Targets that replace the values of their BRD"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Design, test and evidence of each NFR"}
---

# Introduction

## Purpose

This document is the catalogue of the non-functional requirements (NFRs) of BIBS. Each NFR has an ID, a measurable target, the way it is measured, the verification method, the test type, the stage where it is verified, the iorta TechNXT owner who is accountable for meeting it and the BDOI role that accepts the evidence. The register *BIBS_NFR_BRD-00_Non_Functional_Requirements_Register_v{{version}}.xlsx* holds the same {{nfr_total}} NFRs with every column, the reconciliation sheet and the counts.

The BRDs state their own non-functional values, and they differ: response times from 2 to 10 seconds, availability from 99.9% to 100%, recovery points from 15 minutes to 24 hours. BIBS is one platform and needs one set. The catalogue gives that set; the System Requirements Specification and the Test Strategy quote it word for word.

## Audience and use

<!-- table: widths=4.6,12 caption="Readers" -->
| Reader | Use |
|---|---|
| BIBS Product Owner and the BRD owners | Decide the NFRs with status DECISION (chapter 9); confirm the PROPOSED ones |
| BDOI IT and BDO Cloud and Digital Operations Engineering | Plan the infrastructure and the IER changes; accept availability, recovery and operations evidence |
| BDOI Information Security and the Data Protection Officer | Accept the security and privacy targets and their evidence |
| iorta TechNXT project team | Design to the targets; plan the non-functional tests (Test Strategy, chapter 7) |

## How the targets were set

The target of each NFR comes from the strongest source available, in this order:

1. a BDOI decision or answer (for example A4: documents in S3 only; the component baseline of 8 October 2026);
2. the IER workbook v20 of BDO Cloud and Digital Operations Engineering (for example RPO 15 minutes, RTO 4 hours, production 24 x 7, 60 months of use);
3. the BIBS-wide value proposed in the NFR comparison of the discrepancy register v1.2 (for example 99.9% availability in the service window);
4. the values of the BRDs, with their pages;
5. a value proposed by iorta TechNXT where no source gives one.

Each NFR has a status: **ALIGNED** - the sources agree or BDOI has answered ({{nfr_aligned}} NFRs); **DECISION** - the BRDs conflict and BDOI decides, our proposal applies meanwhile ({{nfr_decision}}); **PROPOSED** - our value, for BDOI to confirm ({{nfr_proposed}}).

## Summary

<!-- rq:nfr_categories -->

![Where the NFRs are verified](figures/nfr_flow.dot){width=15}

# Performance, capacity and scalability

## Performance

<!-- rq:nfr_prf -->

The response targets apply at the design load of NFR-CAP-01 with the transaction mix of NFR-CAP-02, measured from the BDO network. Reports and files that cannot meet NFR-PRF-03 run as background jobs with a progress indicator, so a user never waits on a screen for a long report.

## Capacity and volume

<!-- rq:nfr_cap -->

The design load grows 20% a year from 150 concurrent users: 150, 180, 216, 259 and 311 in years 1 to 5. The peak test at 429 concurrent sessions (the sum of the BRD rows of the umbrella BRD) covers year 5 with margin.

## Scalability

<!-- rq:nfr_scl -->

# Availability and recovery

## Availability

<!-- rq:nfr_avl -->

## Recovery, backup and DR

<!-- rq:nfr_rec -->

# Security, privacy and auditability

## Security

<!-- rq:nfr_sec -->

## Privacy

<!-- rq:nfr_prv -->

## Auditability

<!-- rq:nfr_aud -->

## Data retention

<!-- rq:nfr_ret -->

# Users, screens and language

## Accessibility

<!-- rq:nfr_acc -->

## Usability and low-click workflows

<!-- rq:nfr_use -->

## Responsive and mobile-first

<!-- rq:nfr_rsp -->

## Localisation

<!-- rq:nfr_loc -->

# Operation and change

## Maintainability

<!-- rq:nfr_mnt -->

## Configurability

<!-- rq:nfr_cfg -->

Today {{cfg_rules}} of the {{all_rules}} business rules of the FRS ({{cfg_pct}}%) are marked Configurable with the screen, list or parameter where they are maintained; the others are fixed by the BRD or by regulation and are listed as such in each FRS.

## Observability

<!-- rq:nfr_obs -->

## Supportability

<!-- rq:nfr_sup -->

## Interoperability

<!-- rq:nfr_int -->

# Reconciliation with the FRS and the IER workbook

The FRS of each BRD has a non-functional chapter that quotes the values of its BRD and states the BIBS approach. The table compares them with the register proposals, the IER workbook and this catalogue. Where the catalogue differs from an FRS, the FRS takes the catalogue value at its next issue once BDOI has decided; where it differs from the IER, BDO Cloud and Digital Operations Engineering updates the IER.

<!-- rq:nfr_reconciliation -->

Changes requested in the IER workbook v20 as a result:

- Sizing basis: 20% growth a year (the BRD value) in place of 15%, and 429 concurrent sessions as the peak (NFR-CAP-01, NFR-CAP-03).
- Component rows: Valkey 8 in place of ElastiCache for Redis 7, Kafka 3.9 in place of 3.6, the Kubernetes Gateway API in place of ingress-nginx; VDI software list with OpenJDK 21 and Node 22 (NFR-MNT-04).
- Kubernetes rows: the BIBS workloads and sizing of the programme alignment pack (section 6.4) in place of the ten services of the template (NFR-SCL-01).
- DR: one DR region named by BDOI IT, with database replica, document store replication and a standby cluster (NFR-REC-05).

# Verification and ownership

## Verification by test type

Each NFR is verified by test, analysis, inspection or walk-through, in the stage named in the register. The test types are those of the Test Strategy v{{version}}.

<!-- rq:nfr_verification -->

## Ownership

The owner is the iorta TechNXT role accountable for meeting the target and producing the evidence; the BDOI acceptor reviews the evidence and accepts it, or raises a finding.

<!-- rq:nfr_owners -->

<!-- table: widths=4.6,1.6,1.6,1.6,1.6,1.6,1.6,1.6 caption="RACI for the NFRs (iorta TechNXT roles, then BDOI; R responsible, A accountable, C consulted, I informed)" size=8 -->
| Activity | Solution Architect | Perf. Test Lead | Security Lead | QA lead | BIBS PO | BDOI IT | BDOI InfoSec |
|---|---|---|---|---|---|---|---|
| Agree the NFR targets | R | C | C | C | A | C | C |
| Design to the targets | A | C | R | I | I | C | C |
| Plan and run the non-functional tests | C | A | R | R | I | C | C |
| Accept performance and capacity evidence | R | R | I | C | A | C | I |
| Accept availability, recovery and operations evidence | R | I | I | C | I | A | C |
| Accept security and privacy evidence | C | I | R | C | I | C | A |
| Report NFRs monthly after go-live | R | I | R | I | I | A | C |

# Decisions and open points for BDOI

<!-- table: widths=0.9,8.6,3.6,2.2,1.4 caption="Decisions requested from BDOI on the NFRs" size=8 status=Status -->
| No. | Decision | Owner (BDOI) | Needed by | Status |
|---|---|---|---|---|
| 1 | Screen response p95 2.0 seconds for every module (NFR-PRF-01) in place of the BRD values of 2 to 10 seconds (DCR-137, DCR-165, CRQ21) | BIBS Product Owner with the BRD owners | 30 Nov 2026 | OPEN |
| 2 | Users: design load 150 concurrent plus 20% a year; peak test at 429 concurrent sessions (NFR-CAP-01; DCR-166, DCR-232) | BIBS Product Owner | 30 Nov 2026 | OPEN |
| 3 | Availability 99.9% of the service window for all modules (NFR-AVL-01), withdrawing 99.99% (BRD-8) and 100% (BRD-5) (DCR-134, DCR-057) | BIBS Product Owner; Head, Comptrollership | 30 Nov 2026 | OPEN |
| 4 | Service window 06:00-22:00 Monday to Saturday plus month-end weekends; maintenance window 00:00-04:00 (NFR-AVL-02, 03; DCR-133, DCR-138) | BIBS Product Owner; Head, BDOI IT | 30 Nov 2026 | OPEN |
| 5 | Backup retention 7 years (NFR-REC-03; DCR-136) | Head, BDOI IT | 30 Nov 2026 | OPEN |
| 6 | Retention per record type, the audit archive of 16 years and the purge decision (NFR-RET-01, 03; CRQ22, Q39, CLQ24) | BIBS Product Owner; Head, Comptrollership; Compliance Officer | 31 Jan 2027 | OPEN |
| 7 | Session sign-out at 30 minutes for every module (NFR-SEC-05; DCR-140) | BDOI Information Security | 30 Nov 2026 | OPEN |
| 8 | DR region and data residency (NFR-REC-05, NFR-PRV-02; DCR-225, IQ26) | BDOI IT; BDOI Information Security | 31 Mar 2027 | OPEN |
| 9 | Monitoring platform: Dynatrace or Prometheus and Grafana (NFR-OBS-01; IQ30) | BDOI IT | 31 Jan 2027 | OPEN |
| 10 | User provisioning split between UIDM-ISC and BIBS access requests (NFR-SEC-03; IQ05) | BDOI Information Security | 31 Jan 2027 | OPEN |
| 11 | CCM sending mode (NFR-INT-05; IQ11) | BDOI IT | 26 Feb 2027 | OPEN |
| 12 | Confirm the PROPOSED targets, including WCAG 2.2 AA (NFR-ACC-01), the low-click targets (NFR-USE-01), the code coverage above 90% on new code (NFR-MNT-02) and the remediation times (NFR-SEC-12) | BIBS Product Owner; BDOI Information Security | 30 Nov 2026 | OPEN |
| 13 | IER workbook updates of chapter 7 | Cloud and Digital Operations Engineering | 31 Dec 2026 | OPEN |

# Sign-off {-}

By signing, BDOI accepts the targets of this catalogue as the non-functional baseline of BIBS phase 1; the NFRs with status DECISION take the value BDOI records against items 1 to 11 of chapter 9.

```signoff
rows:
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Head, BDOI Information Security", organisation: BDOI}
  - {name: "", role: "Cloud and Digital Operations Engineering (IER owner)", organisation: BDO Unibank IT}
  - {name: "", role: "Solution Architect", organisation: iorta TechNXT}
```
