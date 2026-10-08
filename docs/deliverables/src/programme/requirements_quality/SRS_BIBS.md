---
# System Requirements Specification of BIBS (BRD-00, programme level).
# Build: python docs/deliverables/src/programme/requirements_quality/build_requirements_quality.py --only srs
# Lines <!-- rq:<name> --> are tables built from the FRS, the test plans, the NFR catalogue and the integration
# inventory; double-brace tokens are figures and NFR targets (build_requirements_quality.py, placeholders and figures).
title: System Requirements Specification
subtitle: BIBS - BDOI Broker System on iNXT BrokerVerse, all BRDs of phase 1
doc_type: System Requirements Specification
doc_code: SRS
brd: BRD-00
name: System Requirements Specification
doc_id: BIBS-SRS-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: SRS - BIBS System Requirements Specification
h1_page_break: true
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Business Analyst lead
    reviewer: iorta TechNXT Solution Architect; iorta TechNXT QA lead
    approver: BIBS Product Owner (pending)
    change: First issue, from the BRD versions of 8 October 2026, the FRS of every BRD, the test plans, the programme alignment pack and the IER workbook v20
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Programme requirements baseline"}
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Scope, drops and acceptance approach"}
  - {name: "Product owners of BRD-1 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Module summaries and open points of their BRD"}
  - {name: "BDOI IT (integration owners)", role: Reviewer, organisation: BDOI, purpose: "External interfaces, constraints"}
  - {name: "BDOI Information Security; Data Protection Officer; Compliance Officer", role: Reviewers, organisation: BDOI, purpose: "Security, privacy and compliance requirements"}
  - {name: "Head, Comptrollership", role: Reviewer, organisation: BDOI, purpose: "BIR and Insurance Commission requirements"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Specification, test and acceptance"}
---

# Introduction

## Purpose

This System Requirements Specification (SRS) states, in one document, what BIBS - the BDOI Broker System on iNXT BrokerVerse - must do for BDO Insurance and Reinsurance Brokers, Inc. (BDOI). It is the programme-level view above the Functional Requirements Specifications (FRS) of the thirteen BRDs: it gives the system context, the users, the functional scope of each module with a reference to its FRS, the external interfaces, the data and compliance requirements, the constraints and the way BDOI accepts the system.

The SRS does not repeat the FRS. Each functional requirement, with its rules, fields, messages and acceptance criteria, lives in the FRS of its BRD; where this document and an FRS describe the same behaviour, the FRS governs. The non-functional requirements are summarised here and specified in full in the companion document *Non-Functional Requirements* v{{version}} and its register.

## Audience

<!-- table: widths=4.6,12 caption="Readers and what they use the SRS for" -->
| Reader | Use |
|---|---|
| BIBS Product Owner and the product owners of each BRD | Confirm the scope of phase 1 and the summary of their module; decide the open points of chapter 13 |
| Program Manager, Business Project Services | Scope per drop, acceptance approach, dependencies |
| BDOI IT and BDO Cloud and Digital Operations Engineering | System context, interfaces, constraints and the non-functional baseline |
| BDOI Information Security, Data Protection Officer, Compliance Officer | Security, privacy and regulatory requirements (chapters 7, 8 and 11) |
| iorta TechNXT project team | One reference for specification, development, test and acceptance |

## Scope

BIBS is the enterprise-wide insurance broking system of BDOI. Phase 1 covers the twelve function BRDs (BRD-1 to BRD-12), the Data Migration BRD (BRD-13) and the cross-cutting requirements of the Core Replacement umbrella BRD (BRD-00). All modules go live together in January 2028 (BDOI answer A1 of 26 September 2026); the proposed go-live date is Monday 3 January 2028.

<!-- table: widths=4,12.6 caption="In and out of scope of phase 1" -->
| Area | Scope |
|---|---|
| In scope | Client onboarding, quotation and proposal, package products, placement, issuance and booking, renewal, submitted policies, customer servicing, sanction screening and risk profiling, cashiering, remittance (including the reinsurance transactions that pass through Remittance), adjustment and cancellation, production reconciliation, commission receivables, collections, accounting, disbursement and ACSL, claims logging and monitoring, employee benefits without the partner portal, user access maintenance, data migration and the cross-cutting platform services |
| Phase 2 | The reinsurance module (ReInsurance BRD, CORE-15) |
| Not part of BIBS | Insurer-company functions: insurer underwriting, insurer claims processing, reinsurance treaty and cession accounting, actuarial reserves, group consolidation and insurer taxes. BDOI is a broker: the insurers issue the policies, pay the claims and keep their own books |
| Excluded by BDOI answers | Employee Benefits partner portal (no portal feature on the drop plan); documents in ECM (every BIBS document is held in the S3 document store, answer A4); early renewal release (superseded by the single go-live) |

## Document hierarchy

<!-- table: widths=4.4,6.6,5.6 caption="Where each kind of requirement is specified" -->
| Document | Holds | Governs |
|---|---|---|
| BRDs of BDOI (versions of 8 October 2026) | Business requirements with IDs and pages | The business need |
| This SRS | Context, scope, users, interfaces, data, constraints, compliance, acceptance | Programme-level requirements |
| FRS of each BRD (Data Migration Handbook for BRD-13) | Functional requirements FR-xx-nnn, screens, workflows, reports, messages, user-story view | Functional behaviour |
| Non-Functional Requirements v{{version}} and register | {{nfr_total}} measurable NFRs with verification, owner and test type | Quality attributes |
| Requirements Traceability Matrix v{{version}} | BRD requirement to FR, test cases, screens and drop | Coverage |
| Test plans of each BRD and the Test Strategy v{{version}} | Test conditions, cases, levels, environments, criteria | Verification |
| Technical Specification (reviewed by BDOI IT) | Technical design, interfaces in detail, deployment | Technical realisation |

## References

<!-- table: widths=1.2,10.4,5 caption="Reference documents" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | BDOI Core Replacement BRD (umbrella) and the function BRDs BRD-1 to BRD-12, Data Migration BRD | Versions held on 8 October 2026 |
| R2 | FRS of BRD-00 to BRD-12 and the Data Migration Handbook (BRD-13) | Versions listed in chapter 4 |
| R3 | Test plans of BRD-1 to BRD-13 | Versions listed in chapter 4 |
| R4 | BIBS Non-Functional Requirements and NFR register | v{{version}}, 8 October 2026 |
| R5 | BIBS Requirements Traceability Matrix | v{{version}}, 8 October 2026 |
| R6 | BIBS Test Strategy | v{{version}}, 8 October 2026 |
| R7 | Programme alignment pack: drops, integrations, infrastructure, and the integration inventory | v1.0, 26 September 2026 |
| R8 | BDOI drop plan and programme timeline; BDOI answers A1 to A6 | 26 September 2026 |
| R9 | IER workbook v20 (BDO Cloud and Digital Operations Engineering) | v20 |
| R10 | BRD discrepancy and clarification register | v1.2 |
| R11 | Change management register | v1.0 |
| R12 | BDOI feature list against the OOTB edition and broking best practice (716 features) | v1.0 |
| R13 | BDO UX guidelines and BDOI UX design | Current |
| R14 | Client decisions on the component baseline (PostgreSQL 16, Valkey 8, Kafka 3.9, OpenJDK 21, Spring Boot 3.5, Node 22, nginx, managed Kubernetes, Gateway API) | 8 October 2026 |

## Definitions and acronyms

```glossary
AMLA: Anti-Money Laundering Act (RA 9160, as amended)
AMLC: Anti-Money Laundering Council
ACSL: Accounting Controls and Subsidiary Ledger
BDOI: BDO Insurance and Reinsurance Brokers, Inc.
BIBS: BDOI Broker System, on iNXT BrokerVerse
BIR: Bureau of Internal Revenue
BRD: Business Requirements Document of BDOI
CCB: Change Control Board
CCM: Centralized Communications Management (BDO e-mail sending)
DPA: Data Privacy Act of 2012 (RA 10173)
DPO: Data Protection Officer
EIAM: Enterprise Identity Access Management on Microsoft Entra ID
EOPT: Ease of Paying Taxes Act (RA 11976)
FR: Functional requirement of an FRS (FR-xx-nnn)
FRS: Functional Requirements Specification
IC: Insurance Commission; Insurance Code (RA 10607)
IER: Infrastructure Estimation and Recommendation workbook of BDO IT
KYC: Know your customer
NFR: Non-functional requirement (NFR-xxx-nn)
NFT: Non-functional testing (performance, volume, resilience, security)
ORR / PRR: Operational and production readiness review
PEP: Politically exposed person
PHT: Philippine time (Asia/Manila)
RTM: Requirements Traceability Matrix
SIT: System integration test
STR: Suspicious Transaction Report
TSU: Technical Support Unit
UAT: User acceptance test
UIDM-ISC: User ID Maintenance - Identity Security Cloud (identity governance)
WCAG: Web Content Accessibility Guidelines
```

# System context

## Business context

BDOI is a non-life insurance and reinsurance broker of the BDO group. It places the risks of retail, corporate and bank clients with insurers, collects premiums and remits them to the insurers, earns commission, services clients and follows their claims. Today the work runs on several legacy systems (QPS, EBIX and others), spreadsheets and e-mail; the umbrella BRD asks for one integrated platform from client onboarding to accounting, with automated workflows, dashboards for every role, system reports and audit logs (umbrella BRD p.4-5).

BIBS replaces these systems. It is one application with a module per BRD, one set of users and roles, one client and account base, one invoice ledger and one general ledger.

## System context diagram

Figure 1 shows BIBS with its users and the BDO and external systems it exchanges data with. Every system-to-system interface passes the Apigee X gateway; users sign in through EIAM.

![System context of BIBS: identity and gateway (first row), systems behind Apigee X (second row and EGL, EDP, CARMS), e-mail and content services, and sources entered by hand or migrated once (last row)](figures/srs_context.dot){width=16.5}

## Modules and drops

BDOI splits requirements, development, test and sign-off into three drops; the platform is developed end to end and all modules go live together. Figure 2 places each BRD in its drop.

![BIBS modules by BDOI drop](figures/srs_modules.dot){width=16}

<!-- table: widths=2.6,5.4,3.4,5.2 caption="Drops, BRDs and windows (BDOI drop plan and timeline)" size=8.5 -->
| Drop | BRDs | Requirements | SIT and UAT |
|---|---|---|---|
| Drop 0 - Setup and Data Migration | BRD-11, BRD-03, BRD-13 | Sep to Nov 2026 (with Drop 1) | Setup tested with Drop 1; trial migrations Apr to Oct 2027 |
| Drop 1 - Transactional | BRD-01, 02, 04, 05, 06, 09, 10, 12 | Sep to Nov 2026 | SIT 4 Jan to 30 Jul 2027; UAT 2 Aug to 17 Dec 2027 |
| Drop 2 - Independent | BRD-07, BRD-08; Production Reconciliation (BRD-02); Marketing Collection extraction (BRD-04) | Dec 2026 to Feb 2027 | SIT 1 Jul to 30 Sep 2027; UAT 1 Oct to 30 Nov 2027 |
| Programme | BRD-00 cross-cutting services | With Drop 1 | Performance and penetration test 1 Nov to 15 Dec 2027; ORR / PRR Dec 2027; go-live 3 Jan 2028 |

# Users and personas

## User population

The umbrella BRD counts 1,344 named and 429 concurrent users (p.42). The function BRDs give the users per module; the table groups them by persona family. The FRS of each BRD names the personas, their BIBS roles and permissions.

<!-- table: widths=4.2,5.4,2.4,4.6 caption="Persona families, BRDs and user numbers (named / concurrent, from the BRDs)" size=8 -->
| Persona family | Personas | BRDs | Users (named / concurrent) |
|---|---|---|---|
| Marketing | Account Officer (AO), Team Lead, Team Head, Unit Head; EB desk | BRD-1, 3, 4, 6, 8 | 485 / 145 (BRD-1); EB 13 |
| Technical Support Unit and MBS | TSU Officer, Team Lead, Head; Marketing Business Services | BRD-1, 3 | TSU 13 / 13; MBS 5 / 5 |
| Processing | Processing Officer, Processing TL, e-policy sender, Submitted Policies checker and handler | BRD-1, 6, 8, 12 | 82 (BRD-1); Renewal 73 / 22; SP 38 / 21 |
| Operations | Cashiering, Remittance, Production Reconciliation, Adjustment, Commission users and their TL / TH | BRD-2 | About 50 named |
| Collections | Collection Handler, Team Lead, Section Head, Unapplied Payment Handler | BRD-4 | 130 / 56 |
| Finance | FRBS GL officers, Disbursement, ACSL, Comptrollership | BRD-5, 2 | Accounting 5, Disbursement 8, ACSL 6 |
| Claims | Claims and Risk users, Team Lead, Team Head, Unit Head; Marketing view | BRD-7 | 47 / 25 |
| Customer servicing | Contact Center personnel, supervisors, management | BRD-9 | 24 concurrent |
| Compliance | Compliance Officer, Investigator, Unit Head, AML Committee | BRD-10 | Under 300 named |
| Administration and control | Business Administrator, System Administrator, UAM requestor and approver, Information Security, Auditor | BRD-11, all | Requestors 14 / 5, approvers 8 / 3, administrators 7 |

## Access principles

- Every persona sees only the menus and screens of its roles (FR-CR-001) and starts on a role home page with its work (FR-CR-010).
- Every action is checked against the user's permissions and every record against the user's companies and branches (data scope); a refused access is logged (NFR-SEC-04).
- Makers never approve their own work; approvals follow the configured matrices of each workflow (NFR-CFG-03).
- BDO users sign in through EIAM; only the named break-glass administrators keep local passwords with a second factor (NFR-SEC-01, NFR-SEC-02).
- No insurer, client or partner user signs in to BIBS in phase 1: the Employee Benefits portal is not part of the drop plan.

# Functional requirements

## Approach

The functional requirements are specified in the FRS of each BRD. Each FR carries the BRD requirement IDs and pages it meets, the actor, the priority, the screens, the main and alternate flows, the business rules (each marked Configurable or Fixed with the place where it is maintained), the validations with their messages, the fields and the acceptance criteria. The {{frs_count}} FRS hold {{fr_total}} FRs. Of their {{all_rules}} business rules, {{cfg_rules}} ({{cfg_pct}}%) are configurable by the business or the System Administrator; the others are fixed by the BRD or by regulation.

<!-- rq:frs_summary -->

FRS versions move as BDOI re-issues the BRDs; the traceability matrix states the FRS version of every row. FRS issued at v2.0 move to v2.1 and FRS-only BRDs to the next minor version with the re-base on the BRD versions of 8 October 2026.

## Cross-cutting services (BRD-00)

The umbrella FRS specifies the services that no function BRD owns: persona menus and the role home page (FR-CR-001, 010, 011), notifications (FR-CR-020), the audit trail and the master-data change log (FR-CR-030, 031), standard, tailored and scheduled reports (FR-CR-040 to 043), document generation from templates (FR-CR-050), lists of values, insurer records, MIS fields and products (FR-CR-060 to 063), sign-in and access requests (FR-CR-070), document protection (FR-CR-071), workflow routing (FR-CR-080), the insurer invoice batch (FR-CR-081), migrated clients during coexistence (FR-CR-082), bulk and individual processing (FR-CR-083), the Invoice Master List (FR-CR-090), multi-currency (FR-CR-091), the BDO brand and design system (FR-CR-092) and the SOA and service invoice at booking (FR-CR-093).

## Module summaries

<!-- table: widths=3.2,10.4,3 caption="What each module does (summary of the FRS scope chapters)" size=8 -->
| Module | Summary | FRS |
|---|---|---|
| BRD-01 New Business | Client onboarding and KYC, tags and lifecycle rules; quotation and proposal (package and non-package through the TSU); Proposal Request Form, quotation slip and insurer comparatives; placement with the EOPT details of the client, hold cover, issuance and e-policy; booking with GL entry and service invoice; bulk processing; New Business reports | FR-NB |
| BRD-02 Operations | Invoice ledger and Invoice 360; Cashiering (AR and OR series, payment files, PDC warehouse, application by premium component, unapplied payments); Remittance to insurers (extraction, eligibility, approval, schedule, insurer OR, early remittance incentive, special remittance); Adjustment and Cancellation; Production Reconciliation; Commission Receivables for direct payment; BIR 2307 certificates | FR-OP |
| BRD-03 Product Maintenance | Package products: product structure and clause library, package request, insurer negotiation and comparatives, ManCom sign-off, set-up and validation, versions and rate schemes, expiry and renewal, incentive criteria | FR-PM |
| BRD-04 Collections | Premium-receivable worklist per invoice, assignment, collector dispositions, promises to pay, installments, escalation, billing statements, collector side of unapplied payments, scheduled reversal files and daily reports | FR-CL |
| BRD-05 Accounting, Disbursement and ACSL | Chart of accounts, posting and manual entries, closing and revaluation, bank reconciliation, service fee, report pack and BIR outputs; disbursement vouchers, instruments and BIR 2307 for suppliers; Marketing payment requests; ACSL reconciliation; business and system administration requests | FR-AC, FR-DS, FR-PQ, FR-AS |
| BRD-06 Renewal | Extraction of expiring accounts, governance rules and buckets, Marketing disposition and review, Processing and insurer round-trip, Renewal Advice and other letters, acceptance and progression to placement and booking, renewal of submitted policies | FR-RN |
| BRD-07 Claims | Claims logging and monitoring for the client: cover lookup and premium check, locations and insurer claim numbers, insurer reserve and settlement as reported, statuses, follow-up and ageing, claims special remittance, reports | FR-CM |
| BRD-08 Employee Benefits | Group benefit programmes (HMO, group life, group personal accident): renewal start, broker on record, franchise and TOR, proposals and comparative sign-off, processing, billing and roster servicing, reports; without the partner portal | FR-EB |
| BRD-09 Customer Servicing Facility | Servicing workspace for the Contact Center: search and view of client, account, invoice and payment; contact-detail updates with audit; resend of renewal advices and e-policies; document upload; CSF reports | FR-CSF |
| BRD-10 Sanction Screening | Sanctions and PEP list intake, name matching, risk-profile tagging, cases with SLA and escalation, investigation, Unit Head and AML Committee approval, STR in the AMLC format, audit | FR-SS |
| BRD-11 User Access Maintenance | Sign-in and session rules, user access requests (enrol, modify, deactivate, reactivate, bulk) with approval, group profiles, segregation-of-duties rules, access reports and logs | FR-UA |
| BRD-12 Submitted Policies | Intake of policies that bank borrowers submit, masterlist, sanitation and classification rules, policy review (IAAF), limits and TOR, renewal hand-off, handling fee, monitoring | FR-SP |
| BRD-13 Data Migration | Migration Console: data object register, extracts and batches, validation, client matching, code maps, loads, reconciliation L1 to L5, legacy invoices after go-live, legacy inquiry, cut-over and run-off | FR-DM |

# User-story approach

Each FRS carries an appendix *User-story view* with one row per BRD requirement of the BRD version it is based on: the BRD ID, the user story, the FRs that meet it, their acceptance criteria and the test conditions of the test plan. A second appendix, *Storyboard index*, gives one row per walkthrough frame: persona, screen, action, outcome, screenshot and FR.

- **Wording.** "As <persona>, I need <capability>, so that <business outcome>." Where the BRD writes its requirements as stories, its wording is kept with tidied grammar; otherwise the story is taken from the FR actor, title and purpose. Personas are the persona names of the FRS.
- **Acceptance.** The acceptance criteria of a story are the numbered acceptance criteria of its FRs (for example FR-NB-004 AC1-3). Each acceptance criterion maps to at least one test condition, and each condition has at least one positive and one negative case where the FR allows.
- **Status today.** {{story_frs}} of the {{frs_count}} FRS carry the appendix ({{stories_total}} story rows); the FRS of the other BRDs add it at their re-issue on the BRD versions of 8 October 2026.

![Traceability chain from the BRD requirement to the drop](figures/rtm_chain.dot){width=14}

# External interfaces

## User interface

- Browser application on BDO-issued desktops, laptops, tablets and phones; no plug-in or local install (NFR-RSP-01 to 03).
- BDO brand and the BDO UX guidelines on every screen: tables or tree tables with row action menus, labels not codes, user names not log-ins, dates dd-MMM-yyyy, formatted amounts (NFR-USE-03, NFR-USE-04).
- WCAG 2.2 level AA (NFR-ACC-01 to 04).

## System interfaces

All external touch-points are owned by BDOI IT. Every system-to-system interface passes the Apigee X gateway with its own credentials (NFR-SEC-09, NFR-INT-01); sign-in uses EIAM by OpenID Connect or SAML 2.0, and user provisioning from UIDM-ISC uses SCIM 2.0 (planned). The {{integrations}} interfaces of the integration inventory are:

<!-- rq:interfaces -->

## Documents, e-mail and files

- Business documents (slips, advices, receipts, statements of account, vouchers, letters) are generated from templates the Business Administrator maintains; documents that leave BDOI are password-protected PDF or Excel files (FR-CR-050, FR-CR-071, NFR-SEC-10).
- Outbound e-mail goes through CCM with its delivery status (NFR-INT-05).
- Every document and attachment is held in the S3 document store (answer A4), encrypted with BDOI keys, versioned and replicated to the DR region (NFR-SEC-06, NFR-REC-03).
- File interfaces (PDC list, bills payment, bank and insurer files) are validated per layout and reconciled to control totals (NFR-INT-04).

# Data requirements

## Data domains

<!-- table: widths=3.4,8.2,5 caption="Data domains of BIBS and their owners" size=8.5 -->
| Domain | Content | Owner (BDOI) |
|---|---|---|
| Client and account | Clients (individual and corporate) with EOPT details (taxpayer name, TIN, registered address), KYC and risk profile, accounts, tags, contacts | Marketing; Compliance for the risk profile |
| Product and insurer | Lines, covers and clauses, packages and versions, rate schemes, insurers, insurer contacts and terms, commission rates | MBS, TSU |
| Placement and policy | Quotations, proposals, placements, policies and endorsements as issued by the insurers, e-policies, submitted policies | Processing |
| Money | Invoice ledger, receipts, payments and their application, unapplied payments, remittances, commission receivables, disbursement vouchers | Operations; Comptrollership |
| Accounting | Chart of accounts, journals, sub-ledgers, periods, revaluation rates, BIR worksheets and books | Comptrollership |
| Claims and employee benefits | Claim case files with locations, insurer claim numbers, statuses; EB programmes, rosters and billing | Claims; EB desk |
| Compliance | Sanctions and PEP list entries, screening cases, STRs | Compliance |
| Access and audit | Users, roles, permissions, data scope, access requests, audit trail, change log | Information Security; Business Administration |
| Configuration | Lists of values, parameters, rule tables, workflows, templates, job schedules | Business and System Administrators |

## Data rules

- **One record of each client** across modules; duplicates are matched at onboarding and at migration (FR-DM-031 to 033).
- **Configuration as data.** Every business rule value is a parameter, list of values, rule table or template with an effective date and a second-user approval (NFR-CFG-01, NFR-CFG-02).
- **Data scope.** Records carry their company and branch; users act only within their scope.
- **Integrity of money and ledger.** Ledger entries are never changed: corrections are reversals; document numbers are gapless; journals balance.
- **Base currency** PHP from the company record; other currencies with daily and month-end rates (NFR-LOC-03).
- **Time.** The business date follows Asia/Manila; time stamps are kept in UTC (NFR-LOC-02).
- **Masking.** Personal data is masked in every non-production environment (NFR-PRV-01).

## Volumes, migration and retention

- Volumes and growth follow NFR-CAP-02 and NFR-CAP-03: {{nfr:NFR-CAP-02}}.
- Migration (BRD-13) moves reference data, clients, accounts, open items and the legacy invoices BIBS still needs, reconciled at levels L1 to L5, in four trial migrations, a dress rehearsal and the production load of the cut-over (Data Migration Handbook).
- Retention is set per record type (NFR-RET-01): {{nfr:NFR-RET-01}}. Archive and purge run only after BDOI's retention decision (Q39).

# Non-functional requirements

The non-functional requirements are specified in the companion document *Non-Functional Requirements* v{{version}}: {{nfr_total}} NFRs in 18 categories, each with an ID, a measurable target, the verification method, the owner and the test type ({{nfr_aligned}} aligned with the sources, {{nfr_decision}} waiting for a BDOI decision between conflicting BRD values, {{nfr_proposed}} proposed by iorta TechNXT). The headline targets are:

<!-- rq:nfr_headline -->

# Constraints

<!-- table: widths=3.6,9.6,3.4 caption="Constraints on the solution" size=8.5 -->
| Constraint | Statement | Source |
|---|---|---|
| Single go-live | All modules go live together in January 2028; proposed T = Monday 3 January 2028, legacy freeze after the end of day of 31 December 2027 | BDOI answer A1; project plan |
| Broker scope | BIBS records the insurer's policy, premium, reserve and settlement as information; it does not underwrite, pay claims or keep insurer books | BRD-7 scope; client instruction of 8 October 2026 |
| Component baseline | PostgreSQL 16, Valkey 8 (in place of Redis), Apache Kafka 3.9, OpenJDK 21 LTS, Spring Boot 3.5, React, with Node 22 LTS used only to compile the web screens, nginx web tier, managed Kubernetes with the Gateway API (in place of ingress-nginx) | Client decisions of 8 October 2026 |
| Application structure | One modular application with a module per business area and separate web, jobs and integration workloads | Architecture option decision |
| Hosting | AWS ap-southeast-1; production access only by named personnel in the Philippines; DR region named by BDOI IT after the data-residency check | Hosting appendix; IER; IQ26 |
| Documents | Every document and attachment in the S3 document store; no document storage in ECM | BDOI answer A4 |
| Identity | Sign-in through EIAM; provisioning from UIDM-ISC; integrations through Apigee X | Drop plan integrations |
| Environments | DEV, SIT, UAT, Pre-Prod, PROD and DR; non-production runs 12 x 5, production 24 x 7 | IER assumptions 3 and 4 |
| Devices | Browser only; same functions on mobile and desktop | BRD usage requirements |
| Reinsurance | Reinsurance module in phase 2; reinsurance transactions through Remittance in phase 1 | BDOI answer A3 |

# Assumptions and dependencies

## Assumptions

<!-- table: widths=1.6,11.4,3.6 caption="Assumptions" size=8.5 -->
| ID | Assumption | Related |
|---|---|---|
| SA-01 | The function BRDs and their FRS govern the business processes; the umbrella BRD adds the cross-cutting requirements | FRS BRD-00 A-CR-01 |
| SA-02 | The BRD versions of 8 October 2026 are the baseline of phase 1; later changes pass the change-control process | Change management register |
| SA-03 | The insurers keep issuing policies, invoices and claim decisions in their own systems; BIBS exchanges files or messages with them | Integration INT-19 |
| SA-04 | Users work on BDO-issued devices on the BDO network | BRD usage requirements |
| SA-05 | The BIBS-wide NFR values of the register apply until BDOI decides otherwise (XQ08) | NFR document |
| SA-06 | Non-production environments use seed data or masked migrated data only | NFR-PRV-01 |

## Dependencies on BDOI

<!-- table: widths=1.6,8.4,3.6,3 caption="Dependencies (project plan DEP items)" size=8 -->
| ID | Dependency | Provided by | Needed by |
|---|---|---|---|
| DEP-04 | Sign-off of the Drop 0 sets and the Drop 1 FRS | Product owners | 30 Nov 2026 |
| DEP-06 | SIT environment on managed Kubernetes with PostgreSQL 16, Valkey 8, Kafka 3.9, document store and Gateway API | BDOI IT | 4 Jan 2027 |
| DEP-07 | EIAM sign-in specification and application registration | BDO IT EIAM team | 31 Jan 2027 |
| DEP-08 | UIDM-ISC option and SCIM provisioning connector | BDO IT IGA team | 31 Jan 2027 |
| DEP-10 | Drop 2 integration specifications: EGL, EDP, CARMS, insurer exchanges | BDOI IT | 31 Jan 2027 |
| DEP-11 | Bank channel layouts and transport: CMS / New BOB, OBPCS, Old BOB, AFTS, PMS | BDOI IT | 26 Feb 2027 |
| DEP-13 | Apigee X proxies, products and credentials for SIT, UAT and production | BDO IT API team | 31 Mar 2027 |
| DEP-15 | DR region and data residency | BDOI IT; Information Security | 31 Mar 2027 |
| DEP-20 | UAT testers named per BRD and released for the UAT windows | Business owners | 16 Jul 2027 |
| DEP-23 | Pre-Prod at production size | BDOI IT | 1 Oct 2027 |

# Compliance requirements

BIBS supports BDOI's obligations; it does not replace BDOI's compliance processes or filings. The table maps each law or regulation to the BIBS requirements that support it.

<!-- table: widths=3.2,6.2,4.4,2.8 caption="Regulatory requirements and the BIBS requirements that support them" size=8 -->
| Law or regulation | Obligation BIBS supports | BIBS requirement | BDOI owner |
|---|---|---|---|
| Insurance Code (RA 10607) and Insurance Commission | Premiums collected for insurers tracked per invoice until remitted and the insurer's official receipt recorded; placement records with slips and insurer terms; Insurance Commission Broker's Annual Statement of Business Operations | FR-OP-030 to 041 (Remittance), FR-OP-110 to 112, FR-NB-053 to 055, FR-AC-063; audit trail NFR-AUD-01 | Head, Operations; Head, Comptrollership |
| BIR - books, returns and certificates | Loose-leaf books of accounts (general journal, sales, purchase, cash receipts and disbursements, general ledger); worksheets of 2550-Q, 1601-EQ, 0619-F, 1603, 1702-Q, 1702; alphalists (QAP, MAP, 1604-E) and SAWT; BIR Form 2307 received, released and issued; receipt series with the BIR Authority to Print | FR-AC-063 (outputs are worksheets and loose-leaf books, not eFPS, DAT or CAS files), FR-OP-010 to 014, FR-OP-096, FR-OP-113, FR-OP-121, FR-DS-058 | Head, Comptrollership |
| BIR - Ease of Paying Taxes (RA 11976) | EOPT details of the client (taxpayer name, TIN, registered address) on the placement sent to the insurer; service invoices numbered, produced and sent from BIBS | FR-NB-087, FR-NB-117, FR-CR-093; transmission to BIR CAS / e-invoicing is not in phase 1 (CLR-NB-33) | Head, Comptrollership |
| AMLA (RA 9160, as amended); AMLC; BSP Circular 1182 (2023), BSP CL-2023-030, BSP M-2025-017; IC CL 2019-65 | Screening of clients against sanctions and PEP lists; risk profiling; case investigation and approval; STR prepared and extracted in the AMLC format (filing on the AMLC portal outside BIBS); records kept at least 5 years | FR-SS-010 to 072, FR-SS-091; NFR-RET-05 | Compliance Officer |
| Data Privacy Act 2012 (RA 10173) | Lawful, proportionate processing with security measures; masked non-production data; access control and data scope; sensitive personal information of EB members; data-subject access and correction; breach detection; retention | NFR-SEC-04, NFR-SEC-06, NFR-PRV-01 to 06, NFR-AUD-01, NFR-RET-01; FR-CSF-040 | Data Protection Officer |
| BSP MORB and Circular 808 (IT risk management), BDO standards | Four eyes on access changes, least privilege, segregation of duties, dormant-user deactivation, audit trail no user can change | FRS BRD-11 (FR-UA-006, 053), NFR-SEC-03, NFR-AUD-02 | BDOI Information Security |

# Acceptance approach

## Acceptance steps

<!-- table: widths=3.4,7.6,3.4,2.2 caption="How BDOI accepts BIBS" size=8 -->
| Step | What is accepted | Signed by | When |
|---|---|---|---|
| FRS sign-off | FRS of each BRD, with its clarifications decided or parked under change control | Product owner of the BRD | Drop 0 and 1 by 30 Nov 2026 (BRD-11 1 Dec 2026); Drop 2 by 26 Feb 2027 |
| Test plan approval | Test conditions and cases of each BRD; coverage of every in-scope BRD ID in the traceability matrix | Business owners; iorta QA lead | Drop 1 by 18 Dec 2026; Drop 2 by 30 Jun 2027 |
| SIT exit | All Must-have cases passed; no open Severity 1 or 2 incident; Severity 3 with an agreed workaround | iorta QA lead; BIBS Product Owner | Drop 1 30 Jul 2027; Drop 2 30 Sep 2027 |
| UAT sign-off | Business scenarios of each BRD on migrated, masked data; UAT sign-off certificate per BRD | Product owner of each BRD; BIBS Product Owner | Drop 2 by 30 Nov 2027; Drop 1 by 17 Dec 2027 |
| NFR acceptance | Evidence of each NFR (test report, inspection record or analysis) against its target | BDOI acceptor named per NFR | By 15 Dec 2027 (NFT); ORR / PRR Dec 2027 |
| Migration acceptance | Reconciliation L1 to L5 and business verification of each data object; dress rehearsal report | Data Migration Lead; data owners | Dress rehearsal 26 Nov 2027; go / no-go 2 Jan 2028 |
| Go-live | Go / no-go decision on T-1 | Go / no-go board | 2 Jan 2028 |

## Requirements coverage

The traceability matrix v{{version}} traces {{brd_ids}} BRD requirement IDs: {{covered}} are covered by FRs and test cases (usage requirements by the NFR catalogue), {{fr_only}} by FRs without a test case yet (umbrella FRs), {{clr_ids}} wait for a BDOI decision on a proposed rule, {{out_ids}} are out of scope, replaced or removed, and {{gap}} have no FR named. Coverage of the in-scope IDs is {{coverage_pct}}%. The test plans hold {{cases}} test cases.

<!-- rq:coverage -->

Rule for UAT entry: every in-scope BRD ID of the drop is covered by at least one test case, and every FR has at least one positive and one negative case.

## Change control

A change to an approved requirement goes through the change-control process of the change management register: impact analysis, effort, decision by the Change Control Board, FRS revision citing the CR ID, new or changed test cases and an updated traceability row.

# Decisions and open points for BDOI

<!-- table: widths=0.9,9.0,3.6,1.9,1.4 caption="Decisions requested from BDOI" size=8 status=Status -->
| No. | Decision or open point | Owner (BDOI) | Needed by | Status |
|---|---|---|---|---|
| 1 | Accept this SRS as the programme-level baseline, with the FRS governing functional behaviour | BIBS Product Owner | 23 Oct 2026 | OPEN |
| 2 | Decide the BIBS-wide NFR values that conflict between BRDs (response time, availability, service and maintenance windows, backup and record retention) - see the NFR document | BIBS Product Owner with the BRD owners | 30 Nov 2026 | OPEN |
| 3 | Confirm EIAM single sign-on in place of the EUA directory sign-in of BRD-11 (IQ04) and the split of access requests between UIDM-ISC and BIBS (IQ05) | BDOI Information Security; BDOI IT | 31 Jan 2027 | OPEN |
| 4 | Name the DR region after the data-residency check (IQ26) | BDOI IT | 31 Mar 2027 | OPEN |
| 5 | Confirm that transmission of service invoices to BIR CAS / e-invoicing is outside phase 1 (CLR-NB-33) | Head, Comptrollership | 30 Nov 2026 | OPEN |
| 6 | Decide the umbrella rows waiting for a proposed rule: claim cheques held by BDOI (CRQ03), CSF case resolution (CRQ04), emerging capabilities (CRQ10), ALeA e-mail encoding (CRQ20) | BIBS Product Owner | 30 Nov 2026 | OPEN |
| 7 | Scope and drop of Customer Servicing Case Management (CSF addendum) | Product Owner, CSF; Change Control Board | 30 Oct 2026 | OPEN |
| 8 | Accept WCAG 2.2 level AA as the accessibility level of BIBS | BIBS Product Owner | 30 Nov 2026 | OPEN |
| 9 | Plan the privacy impact assessment of BIBS by the Data Protection Officer before UAT | Data Protection Officer | 30 Jun 2027 | OPEN |
| 10 | Confirm the re-issue of the FRS of BRD-06 to BRD-10 and BRD-12 with the user-story view and storyboard index | BIBS Product Owner | 13 Nov 2026 | OPEN |

# Sign-off {-}

By signing, BDOI accepts this System Requirements Specification as the programme-level requirements baseline of BIBS phase 1. The FRS of each BRD remain the specification of the functional behaviour and are signed separately.

```signoff
rows:
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Head, BDOI Information Security", organisation: BDOI}
  - {name: "", role: "Project Manager", organisation: iorta TechNXT}
```
