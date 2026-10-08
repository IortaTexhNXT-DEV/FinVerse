---
# Architecture Decision Records of BIBS (BRD-00, Programme/Architecture); the ADR Log workbook is generated with it.
# Build: python docs/deliverables/src/programme/architecture/build_architecture_pack.py (the records come from adr.yaml)
title: Architecture Decision Records
subtitle: The architecture decisions of BIBS with their context, alternatives and consequences
doc_type: Architecture Decision Records
doc_code: Architecture
brd: BRD-00
name: Architecture Decision Records
doc_id: BIBS-ARC-06
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: Architecture set, document 6 of 6
h1_page_break: false
output: Architecture/BIBS_Architecture_BRD-00_Architecture_Decision_Records_v1.0.docx
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Solution Architect
    reviewer: iorta TechNXT Tech Lead; iorta TechNXT Project Manager
    approver: BDOI Enterprise Architecture (pending)
    change: First issue - ADR-01 to ADR-20, including the decisions of 26 September and 8 October 2026
distribution:
  - {name: "Enterprise Architecture", role: Approver, organisation: BDOI IT, purpose: "Architecture decisions"}
  - {name: "Information Security Office", role: Reviewer, organisation: BDOI IT, purpose: "Security decisions ADR-08 to ADR-10, ADR-13, ADR-14, ADR-17"}
  - {name: "Cloud and Digital Operations Engineering", role: Reviewer, organisation: BDO Unibank IT, purpose: "Platform decisions ADR-03 to ADR-06, ADR-15, ADR-18"}
  - {name: "Application Development and Integration", role: Reviewer, organisation: BDOI IT, purpose: "Integration decisions ADR-05, ADR-10, ADR-16"}
  - {name: "BIBS Product Owner", role: Informed, organisation: BDOI, purpose: "Scope decision ADR-11"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Decisions in force for delivery and support"}
---

# Introduction

## Purpose and audience

This document records the architecture decisions of BIBS, the BDOI Broker System on iNXT BrokerVerse: for each decision the context, the decision, the alternatives considered and why they were not chosen, the consequences, the status and the date. It is written for BDOI IT Enterprise Architecture, the Information Security Office and the platform teams, and for whoever supports BIBS over its life, so that the reasons for the architecture stay known after the delivery team has moved on. The ADR Log workbook (BIBS_Architecture_BRD-00_ADR_Log_v1.0.xlsx) holds the same decisions one per row.

## Scope

Decisions that shape the structure, technology, deployment, security or scope of BIBS and are costly to reverse. Functional decisions of a BRD are recorded in its FRS and in the discrepancy and change registers.

## Sources

<!-- table: widths=1,7,9.6 caption="Sources" bold=first -->
| Ref | Document | Used for |
|---|---|---|
| S1 | Client decisions of 26 September 2026 (architecture option, edge and encryption, document storage, ECM) | ADR-01, ADR-09, ADR-12, ADR-15 to ADR-18 |
| S2 | Client decisions of 8 October 2026 (component baseline, Valkey, Gateway API, broker-only scope) | ADR-02 to ADR-07, ADR-11 |
| S3 | Application architecture option decision; document storage decision record; platform cache and events architecture; company and branch data scope architecture; user access architecture | Context and decisions |
| S4 | Quality gates; security controls; suppression files of the scanners | ADR-13, ADR-14 |
| S5 | IER workbook v20; Programme Alignment pack v1.0 | Context of the infrastructure decisions |

# How decisions are recorded

## Template and statuses

Each record has: identifier and title; status; date; who decided; area; context (the forces and facts); decision; alternatives considered and why not; consequences (benefits, costs and risks); sources.

<!-- table: widths=2.6,15 caption="Statuses" bold=first size=8.5 -->
| Status | Meaning |
|---|---|
| Proposed | Recommended by the project; BDOI decides. The open item is named in "Decided by" |
| Accepted | Decided by BDOI or, for a platform principle, by the project and presented to BDOI in this document; in force |
| Superseded | Replaced by a later record that names it; kept for the history |

## Lifecycle

1. Anyone in the programme may raise a decision; the iorta TechNXT Solution Architect writes it as a Proposed record with the alternatives.
2. The record is reviewed with BDOI Enterprise Architecture and the teams it affects (security, cloud, integration) in the fortnightly architecture review.
3. BDOI Enterprise Architecture accepts it (or the BDOI owner named in the record for business-scope decisions); the date and decider are recorded.
4. A decision is never edited after acceptance; a change is a new record that supersedes it.
5. The log is reviewed at each annual technology review and before go-live (ORR / PRR, December 2027).

# Decision log

<!-- arch:adr_index -->

{{adr_count}} decisions: {{adr_accepted}} accepted and {{adr_proposed}} proposed for BDOI decision (ADR-10 SCIM provisioning and ADR-14 CVE exception policy).

# Decision records

<!-- arch:adrs -->

# Decisions and open points for BDOI

<!-- table: widths=1.2,3,8.4,2.8,2.2 caption="Decisions and open points for BDOI" bold=first size=8 -->
| ID | Topic | Our proposal | Owner | Needed by |
|---|---|---|---|---|
| AD-01 | Acceptance of the log | BDOI Enterprise Architecture accepts ADR-01 to ADR-20 as the architecture baseline of BIBS v1.0 | BDOI Enterprise Architecture | 30-Nov-2026 |
| AD-02 | ADR-10 SCIM provisioning | Option (a): accounts from UIDM-ISC through SCIM 2.0; roles and data scope through BIBS access requests (IQ05, SA-05) | BDOI IT Identity Governance | 16-Oct-2026 |
| AD-03 | ADR-14 CVE exception policy | Accept the policy; the BDOI Information Security Officer approves accepted findings for production releases (SA-10) | Information Security Office | 31-Dec-2026 |
| AD-04 | ADR-09 protocol details | OIDC against Entra ID; user name claim UPN; MFA by Entra conditional access; BIBS inactivity time-out kept at 30 minutes (IQ04) | BDOI IT Identity and Access Management | 16-Oct-2026 |
| AD-05 | ADR-18 DR region | Name the DR region and approve the cross-border basis (IQ26, DSQ01, IA-03) | BDOI Risk and Compliance | 16-Oct-2026 |
| AD-06 | ADR-02 support route | Decide at the first technology review between Spring Boot 4.x before UAT and commercial support of 3.5 (SA-07) | BDOI Enterprise Architecture | 31-Jan-2027 |
| AD-07 | Architecture review forum | A fortnightly architecture review chaired by BDOI Enterprise Architecture until go-live, then quarterly | BDOI Enterprise Architecture | 30-Oct-2026 |

# Glossary {-}

```glossary
ADR: Architecture decision record
CVE: Common Vulnerabilities and Exposures identifier
DOA: Delegation of Authority
EIAM: Enterprise Identity Access Management (Microsoft Entra ID)
IRSA: IAM roles for service accounts
KRaft: Kafka Raft metadata mode
NLB: Network Load Balancer
PKCE: Proof Key for Code Exchange (OAuth 2.0)
SBOM: Software bill of materials
SCIM: System for Cross-domain Identity Management
TOTP: Time-based one-time password
WAF: Web application firewall
```
