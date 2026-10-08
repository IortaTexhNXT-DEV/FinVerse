---
# Test Strategy of BIBS (BRD-00, programme level).
# Build: python docs/deliverables/src/programme/requirements_quality/build_requirements_quality.py --only ts
# <!-- rq:... --> tables come from the test plans and the NFR catalogue; {{...}} tokens are counts and NFR targets.
title: Test Strategy
subtitle: Test levels, environments, data, criteria, incident management and schedule for BIBS phase 1
doc_type: Test Strategy
doc_code: TestStrategy
brd: BRD-00
name: Test Strategy
doc_id: BIBS-TST-BRD-00
version: "1.0"
date: 8 October 2026
status: Issued for BDOI review
header_title: Test Strategy - BIBS
h1_page_break: true
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT QA lead
    reviewer: iorta TechNXT Solution Architect; iorta TechNXT Project Manager
    approver: BIBS Product Owner (pending)
    change: First issue, aligned with the BDOI programme timeline, the project plan, the test plans of BRD-1 to BRD-13 and the NFR catalogue v1.0
distribution:
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Test approach, criteria, UAT organisation"}
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Schedule per drop and sign-off"}
  - {name: "Product owners of BRD-1 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "UAT testers, scenarios and sign-off certificates"}
  - {name: "Head, BDOI IT", role: Reviewer, organisation: BDOI, purpose: "Environments, integrations, NFT and DR"}
  - {name: "BDOI Information Security; Data Protection Officer", role: Reviewers, organisation: BDOI, purpose: "Security testing, penetration test, masked data"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Test planning, execution and reporting"}
---

# Introduction

## Purpose

This Test Strategy sets how BIBS is tested from the first line of code to the go-live check: the test levels and types, the environments, the test data, the entry and exit criteria, the handling of test incidents, the tools, the coverage targets, the roles and the schedule of each drop. It applies to every BRD of phase 1, to the data migration and to the non-functional requirements.

The test plans of each BRD (Excel workbook and Word summary) hold the test conditions, cases, steps and expected results; this strategy does not repeat them. Together they hold {{cases}} test cases over {{conds}} test conditions, {{pos_cases}} positive and {{neg_cases}} negative.

## Audience

<!-- table: widths=4.6,12 caption="Readers" -->
| Reader | Use |
|---|---|
| BIBS Product Owner and BRD product owners | UAT organisation, testers, criteria and sign-off |
| BDOI IT, BDO Cloud and Digital Operations Engineering | Environments, integrations, non-functional tests and DR drill |
| BDOI Information Security, Data Protection Officer | Security tests, penetration test, masked test data |
| iorta TechNXT project team | Test levels, criteria, tools and responsibilities |

## Scope

<!-- table: widths=4,12.6 caption="What is tested" -->
| Area | Scope |
|---|---|
| Functional | Every FR of the FRS of BRD-00 to BRD-12 and of the Data Migration Handbook (BRD-13); every in-scope BRD requirement ID through the traceability matrix |
| Integrations | The interfaces of the integration inventory through Apigee X, EIAM and UIDM-ISC, with partner simulators until each partner system is available |
| Data migration | Trial migrations 1 to 4, the dress rehearsal and the production load: validation, load, reconciliation L1 to L5 and business verification |
| Non-functional | The {{nfr_total}} NFRs of the NFR catalogue, each at the stage named in the register |
| Out of scope | The insurer-company functions that BIBS does not include; the reinsurance module (phase 2); the internal testing of BDO systems on the other side of an interface |

## References

<!-- table: widths=1.2,11,4.4 caption="Reference documents" -->
| Ref. | Document | Version |
|---|---|---|
| R1 | FRS of BRD-00 to BRD-12; Data Migration Handbook (BRD-13) | Versions in the traceability matrix |
| R2 | Test plans of BRD-1 to BRD-13 (workbook and summary) | Versions in chapter 5 |
| R3 | BIBS System Requirements Specification | v{{version}} |
| R4 | BIBS Non-Functional Requirements and NFR register | v{{version}} |
| R5 | BIBS Requirements Traceability Matrix | v{{version}} |
| R6 | BIBS project plan (timeline, milestones, test windows, dependencies) | v1.0 |
| R7 | Programme alignment pack; IER workbook v20 | v1.0; v20 |
| R8 | Quality gates of the platform (static analysis, coverage, dependency and supply-chain checks) | Current |

# Test approach

## Principles

1. **Requirement-based.** Every test case traces to an FR and its BRD requirement IDs; every in-scope BRD ID has at least one case before UAT entry (traceability matrix).
2. **Positive and negative.** Every FR has at least one case where the action succeeds and one where BIBS refuses it; every message of the messages catalogue has a case.
3. **Early and continuous.** Unit testing, static analysis and security scans run on every change in the CI pipeline; a change that fails a gate is not released into SIT.
4. **Risk first.** Money, ledger, compliance and access functions (Cashiering, Remittance, Accounting, Sanction Screening, User Access) are tested first and most deeply; their regression set runs on every release candidate.
5. **Business owns acceptance.** BDOI business users run UAT on their own scenarios; iorta TechNXT supports, fixes and re-tests.
6. **Realistic data, never exposed.** SIT uses seed data; UAT runs on migrated, masked data; non-functional tests run on production-sized volumes. No unmasked personal data leaves production.
7. **Production-like for non-functional testing.** Performance, resilience and DR are tested on the production-sized Pre-Prod and the DR region.

## Flow of testing

![Test levels and environments](figures/ts_levels.dot){width=15.5}

# Test levels

<!-- table: widths=2.6,4.6,2.6,2.4,4.4 caption="Test levels" size=7.5 -->
| Level | Objective and scope | Performed by | Environment | Evidence |
|---|---|---|---|---|
| Unit testing | Each class and component behaves as specified in isolation: rules, calculations, validations, permission checks; web components and their states | iorta developers | Developer machine; CI pipeline | Unit testing and coverage reports of the CI pipeline |
| Integration testing | Modules work together and with PostgreSQL 16, Valkey 8 and Kafka 3.9: postings, workflows, events, jobs, data scope | iorta developers | CI pipeline; DEV | CI reports |
| Service interface (API) testing | Each service interface answers to its contract: inputs, outputs, errors, security; partner interfaces against simulators and then the partner systems through Apigee X | iorta QA team | DEV; SIT | Contract test results; interface test log |
| System integration test (SIT) | The test plan of each BRD: every condition and case, end-to-end flows across modules (onboarding to booking to cashiering to remittance to the GL), interfaces, jobs, reports and documents, screens and messages | iorta QA team | SIT | Test execution in the test management tool; SIT exit report per drop |
| Data migration testing | Each trial migration: extract validation, load, reconciliation L1 to L5, business verification on screen, timings | iorta Migration Lead with BDOI data owners | SIT (trials 1, 2); UAT (trials 3, 4); Pre-Prod (dress rehearsal) | Reconciliation reports and sign-off forms of each object |
| User acceptance test (UAT) | BDOI business users confirm that BIBS supports their processes end to end on migrated data; usability and learnability (NFR-USE-05) | BDOI UAT testers, supported by iorta | UAT | UAT scenarios run; UAT sign-off certificate per BRD |
| Non-functional testing (NFT) | Performance, volume, stress, soak, resilience, DAST and penetration test against the NFR targets | iorta Performance Test Lead and Security Lead; independent penetration testers | Pre-Prod | NFT report; security test report |
| Operational acceptance and DR | Backups, restores, monitoring and alerts, releases and rollback, DR failover; the readiness review | iorta DevOps and Platform Lead with BDOI IT | Pre-Prod; DR | DR drill report; ORR / PRR evidence pack |
| Go-live check | Smoke check of each module on production at T, before users start | iorta QA team with key users | Production | Go-live check list signed by the command centre |

# Test types

<!-- table: widths=3.2,6.8,3.4,3.2 caption="Test types and where they apply" size=7.5 -->
| Test type | What it checks | Levels | NFRs |
|---|---|---|---|
| Positive | The action succeeds with valid data and the expected result, status and postings | SIT, UAT | - |
| Negative | BIBS refuses invalid data or an invalid action with the message of the catalogue | SIT, UAT | NFR-SUP-03 |
| Boundary | Limits, thresholds and dates at, below and above the value | Unit, SIT | NFR-CAP-04, NFR-LOC-02 |
| Security-access | A persona without the permission, or outside its company and branch, cannot see or act | SIT, penetration test | NFR-SEC-04 |
| Workflow | Stages, approvals, returns, SLA timers and escalations of each workflow | SIT, UAT | NFR-CFG-03 |
| Report-output and document | Report columns, totals, filters and formats; documents and their protection | SIT, UAT | NFR-SEC-10, NFR-PRF-06 |
| Upload-download | Templates, row results, rejected rows, file limits | SIT | NFR-PRF-05 |
| Screen and message | Each screen of the sign-off set against its specification; each message word for word | SIT | NFR-USE-04 |
| Regression | The cases of the affected FRs and the risk-based regression set on every release candidate; automated regression testing of the main flows | SIT, UAT | - |
| Performance: load, peak, stress, soak | Response times and resource use at the design load, the peak, beyond the peak and over 8 hours | NFT | NFR-PRF, NFR-CAP, NFR-SCL |
| Volume | Year-5 data and jobs: reports, files, night window | NFT | NFR-PRF-03, 04, 07 |
| Security: SAST, dependencies, images, secrets | Code and supply chain on every change | CI pipeline | NFR-SEC-07 |
| Security: DAST and penetration test | The running application from outside | SIT (early scan), NFT | NFR-SEC-08 |
| Resilience | Loss of a pod, node, zone, cache, event stream or partner system | NFT | NFR-AVL-04, 05, NFR-INT-02, 06 |
| Disaster recovery | Failover to the DR region within the RTO and RPO | DR drill | NFR-REC-01, 02, 04 |
| Accessibility | WCAG 2.2 AA by scan and manual keyboard and screen-reader checks | SIT | NFR-ACC-01 to 04 |
| Responsive and cross-browser | Phone, tablet and desktop widths; supported browsers | SIT | NFR-RSP-01 to 03 |
| Usability | Click counts of the main tasks; first-attempt success and SUS score | SIT, UAT | NFR-USE-01, 05 |
| Configuration | Changes of lists, parameters, rules and templates take effect from their date after approval | SIT | NFR-CFG-01 to 04 |
| Data migration reconciliation | Counts, amounts and balances L1 to L5 per object; Migration Clearing at 0.00 | Trial migrations | NFR-PRV-01, 05 |

# Test plans and coverage

## Test plans per BRD

Each BRD has a test plan with scenarios, test conditions (TC-xx-nnn.c), test cases (TC-xx-nnn.c-nn), test data sets, the roles-and-access matrix and, for the issued sign-off sets, one case per screen and per message group.

<!-- rq:test_plans -->

## Coverage targets

<!-- table: widths=4.4,6.6,3.2,2.4 caption="Coverage targets" size=8 -->
| Coverage | Target | Measured by | When |
|---|---|---|---|
| BRD requirements | 100% of the in-scope BRD requirement IDs of a drop traced to at least one test case | Traceability matrix | Test plan approval; UAT entry |
| Functional requirements | Every FR with at least one positive and one negative case; every condition with a case | Test plan check | Test plan approval |
| Code coverage on new code | Line coverage above 90% on new and changed code | SonarQube new-code gate | Every change |
| Code coverage overall | At least 80% of lines and 65% of branches | CI coverage gate | Every change |
| Test execution | 100% of the cases of the drop run; 100% of Must-have cases passed | Test management tool | SIT exit |
| Automated regression | The positive main flow of every Must-have FR in the automated regression set | Regression suite report | SIT exit of each drop |
| NFRs | Evidence for every NFR at its stage | NFR register | ORR / PRR |

Today the traceability matrix v{{version}} covers {{covered}} of the {{in_scope_ids}} in-scope BRD requirement IDs ({{coverage_pct}}%); {{fr_only}} rows are traced to umbrella FRs whose cases sit in the function test plans, and {{clr_ids}} wait for a BDOI decision.

# Test environments

## Environment set

<!-- table: widths=2.2,4.6,3.6,2.6,3.6 caption="Test environments (IER workbook v20; project plan)" size=7.5 -->
| Environment | Purpose | Data | Ready by | Hours and size |
|---|---|---|---|---|
| DEV | Development, unit and integration testing | Seed data | In use | 12 x 5; backend 1-2 pods |
| SIT | System integration test, service interface tests, trial migrations 1 and 2, early security scan | Seed data; masked migration loads | 4 Jan 2027 (DEP-06) | 12 x 5 plus extended hours for batch and trial migrations; backend 2-3 pods |
| UAT | User acceptance test, trial migrations 3 and 4 | Masked migrated data (trial migration 3) | 19 Jul 2027 (DEP-21) | 12 x 5 plus UAT month-end runs; backend 2-4 pods |
| Training | Key-user and end-user training | Seed and masked data, training users | 19 Jul 2027 (DEP-21) | 12 x 5 |
| Pre-Prod | NFT, penetration test, dress rehearsal, release and rollback rehearsal | Production-sized synthetic and masked data | 1 Oct 2027 (DEP-23) | Production size; backend 3-6 pods |
| DR | DR drill and failover | Replica of Pre-Prod or production | 1 Nov 2027 (DEP-25) | Standby; scaled at failover |
| Production | Go-live check only; no testing with business data | Production | 1 Nov 2027 (DEP-25) | 24 x 7 |

## Environment rules

- Each environment runs the component baseline of 8 October 2026 (PostgreSQL 16, Valkey 8, Kafka 3.9, OpenJDK 21, Spring Boot 3.5, nginx, managed Kubernetes with the Gateway API) at the same versions as production.
- Releases reach SIT, UAT and Pre-Prod through the same pipeline and release notes; no change is made by hand in an environment.
- Scheduled jobs of non-production environments run inside the 12 x 5 hours; batch, month-end and trial-migration cycles get extended hours booked one week ahead with BDOI IT.
- Partner systems that are not ready are replaced by simulators that follow the agreed contract; each simulator is switched to the real system as soon as it is available, and the interface cases are re-run.
- Access to SIT and UAT is by named SIT and UAT users per persona; access to Pre-Prod and DR follows production rules.

# Non-functional testing

## Performance and volume

- **Workload model.** From NFR-CAP-02 and the peaks of NFR-CAP-05: transaction mix per persona, hourly rates and think times, approved by the BIBS Product Owner before the runs.
- **Scenarios.** Load: 60 minutes at the design load (150 concurrent users, year 1). Peak: 60 minutes at 429 concurrent sessions. Stress: steps of 10% above the peak until a target breaks. Soak: 8 hours at the design load. Volume: reports, files and the night window on year-5 data.
- **Tool.** Apache JMeter 5.6 (IER VDI list); results with the server metrics of the monitoring platform.
- **Baseline.** A first load run in SIT in June 2027 finds design issues early; the formal NFT runs on Pre-Prod from 1 November to 15 December 2027.

<!-- rq:nft_targets -->

## Security

- Every change: SAST, dependency, container image and secret scans of the CI pipeline (NFR-SEC-07).
- Each release candidate in SIT and Pre-Prod: DAST scan (OWASP ZAP proposed); no open high or critical finding.
- Early security scan in SIT in June 2027; independent penetration test on Pre-Prod between 1 November and 15 December 2027 by a provider BDOI Information Security names (DEP-19); every critical and high finding fixed and re-tested before go-live.
- Security-access cases of every test plan (each protected action against personas without the permission and outside the data scope).

## Resilience and DR

- Resilience: stop a backend pod, drain a node, fail over the database, lose an availability zone, stop Valkey and Kafka for 15 minutes, switch a partner simulator off - each under load, with no lost transaction (NFR-AVL-04, 05; NFR-INT-02, 06).
- DR drill in December 2027: failover to the DR region and back, measuring the RPO ({{nfr:NFR-REC-01}}) and the RTO ({{nfr:NFR-REC-02}}).

## Accessibility, responsive and usability

- Accessibility scan of every screen and a manual keyboard and screen-reader pass of the main journey of each persona in SIT (NFR-ACC-01 to 04).
- Every screen family at 360, 768, 1280 and 1920 pixels and on the supported browsers (NFR-RSP-01 to 03).
- Click counts of the top tasks in SIT; first-attempt success and the SUS survey in UAT (NFR-USE-01, 05).

# Test data management

<!-- table: widths=2.8,6.8,3.6,3.4 caption="Test data by environment" size=8 -->
| Data | Content and rules | Environments | Owner |
|---|---|---|---|
| Seed data | Reference data, products, clients, accounts and transactions created for testing (profile seed); named data sets TD-xx of each test plan; SIT and UAT users per persona | DEV, SIT, Training | iorta QA lead |
| Masked migrated data | Extracts from the legacy systems loaded through the Migration Console with names, TIN, addresses, contact details and bank account numbers masked; the masking key is held outside the environment | SIT (trials 1, 2), UAT (trials 3, 4) | iorta Migration Lead; BDOI Data Migration Lead |
| Production-sized data | Year-5 volumes generated from the profile of the masked data for NFT | Pre-Prod | iorta Performance Test Lead |
| Configuration values | Organisation, chart of accounts, accounting rules, bank accounts and lists from the Drop 0 configuration inputs (D2, D3, D5) | SIT, UAT | Configuration owners (BDOI) |

Rules:

- No unmasked personal data in any non-production environment; each load is checked by the masking verification before testers get access (NFR-PRV-01).
- Migration staging data is purged within 5 days of the batch sign-off (NFR-PRV-05).
- Each test case names its data set; a data set that a run consumes is restored from its snapshot before the next run.
- UAT data is refreshed only at a trial migration; the refresh dates are in the schedule (chapter 14).
- Test evidence that holds data is stored in the test management tool with access for the project and the UAT testers only.

# Entry and exit criteria

<!-- table: widths=2.6,7,7 caption="Entry and exit criteria per level" size=7.5 -->
| Level | Entry | Exit |
|---|---|---|
| SIT (per drop) | Test plans approved (Drop 1 18 Dec 2026; Drop 2 30 Jun 2027); SIT environment and configuration set-up ready; release notes of the release candidate delivered; smoke test passed | 100% of the cases run; all Must-have cases passed; no open Severity 1 or 2 incident; Severity 3 incidents with a workaround agreed by the BDOI owner; regression passed on the last release candidate; SIT exit report issued |
| UAT (per drop) | SIT exit; UAT readiness statement (Drop 1 30 Jul 2027; Drop 2 30 Sep 2027); UAT scenarios and sign-off forms issued (16 Jul 2027); testers named and trained; trial migration 3 loaded and verified | Every UAT scenario passed or accepted with a workaround; no open Severity 1 or 2 incident; UAT sign-off certificate per BRD |
| Data migration (per trial) | Extracts and control files received; code maps of the trial approved; Migration Console ready | All objects of the trial loaded; L1 to L5 reconciled; Migration Clearing 0.00; timings recorded |
| NFT | Pre-Prod at production size (1 Oct 2027); workload model approved; release candidate passed SIT regression | Every NFT target of chapter 7 met, or a deviation accepted by the NFR acceptor; no open critical or high security finding |
| DR drill | DR environment ready; DR runbook reviewed | RPO and RTO met; failback done; report signed |
| Go-live check | Go decision on T-1 | Smoke check of every module passed; command centre opens the system to users |

Suspension: testing of a drop stops when a Severity 1 incident blocks more than 20% of the planned cases or the environment is unavailable for more than one business day; it resumes when the cause is fixed and the smoke test passes.

# Test incident management

## Life cycle

![Life cycle of a test incident](figures/ts_incident.dot){width=14}

## Severity

<!-- table: widths=2.6,8.2,3,2.8 caption="Severity of a test incident and the fix target during testing" size=8 -->
| Severity | Definition | Examples | Fix target in test |
|---|---|---|---|
| Severity 1 - Critical | A function or module cannot be used, data is lost or corrupted, money or the GL is wrong, or security is breached; no workaround; testing is blocked | Receipt posts the wrong amount; a persona sees another company's records | Fix or workaround within 1 business day |
| Severity 2 - High | A main function fails or gives a wrong result; a workaround exists but is costly or risky | Remittance schedule misses eligible invoices; a report total is wrong | Within 3 business days |
| Severity 3 - Medium | A function works with a reasonable workaround; a secondary function fails | A filter does not work; an e-mail template shows a wrong field | In the next release candidate (within 10 business days) |
| Severity 4 - Low | Wording, layout or cosmetic point with no effect on the result | Label differs from the specification; alignment of a column | Planned release |

The tester proposes the severity; the daily triage (iorta QA lead with the BDOI owner of the BRD during UAT) confirms it and sets the fix priority. A point that changes a requirement is not a test incident: it becomes a change request for the Change Control Board, and the FRS and test cases follow its decision. Production incidents after go-live follow the priorities P1 to P4 of the production support approach.

## Recording and reporting

- Each incident records the case ID, the release candidate, the steps, the expected and actual result, the evidence, the severity, the BRD and FR, the environment and the owner.
- Daily during SIT and UAT: triage of new incidents and of incidents due for re-test.
- Weekly: open incidents by severity and age, fix rate, re-test pass rate, cases run and passed per BRD, coverage of the BRD IDs.

# Automation and regression

- **In the CI pipeline, on every change:** unit testing, integration testing, coverage, static analysis and security scans (quality gates). A change that fails a gate is not merged.
- **Service interface checks:** contract checks of each interface with Postman collections (IER VDI list) run on every release candidate in SIT.
- **Automated regression testing of the screens:** the positive main flow of every Must-have FR in Katalon (IER VDI list), run on every release candidate in SIT and UAT; the suite grows with each drop.
- **Risk-based manual regression:** the cases of the FRs a release changes, plus the money, ledger, compliance and access sets.
- **Smoke set:** sign-in, one transaction per module, one report, one job - on every deployment to SIT, UAT, Pre-Prod and production.

# Tools

<!-- table: widths=4.2,7,5.4 caption="Test tools" size=8 -->
| Purpose | Tool | Source |
|---|---|---|
| Test management, execution and incidents | Xray or TestRail (BDOI chooses) | IER VDI list |
| Test plans and traceability | Test plan workbooks per BRD; traceability matrix | Client pack |
| Unit and integration testing, coverage | Java and TypeScript unit testing frameworks of the CI pipeline; integration testing against PostgreSQL 16; coverage reports | Quality gates |
| Static analysis and quality gate | Checkstyle, PMD, SpotBugs, ArchUnit, ESLint with SonarJS, SonarQube | Quality gates |
| Security scans | CodeQL, OWASP dependency-check, npm audit, Trivy, gitleaks, CycloneDX bill of materials; OWASP ZAP for DAST (proposed) | Quality gates |
| Service interface testing | Postman | IER VDI list |
| Automated regression of screens | Katalon | IER VDI list |
| Performance and volume | Apache JMeter 5.6 | IER VDI list |
| Monitoring during NFT | Dynatrace or Prometheus and Grafana (NFR-OBS-01) | IER |
| Accessibility | Accessibility checker of the browser developer tools; NVDA screen reader | Proposed |

# Roles and responsibilities

## Roles

<!-- table: widths=4.4,12.2 caption="Test roles" size=8.5 -->
| Role | Responsibilities |
|---|---|
| iorta QA lead (test manager) | Test strategy and plans, SIT execution, triage, reporting, SIT exit report, UAT support |
| iorta test analysts | Test cases and data, execution, incident logging, re-test, regression |
| iorta Performance Test Lead | Workload model, NFT scripts and runs, NFT report |
| iorta Security Lead | Security gates, DAST, coordination of the penetration test, security report |
| iorta Development Lead and developers | Unit and integration testing, coverage, fixes and release notes |
| iorta DevOps and Platform Lead | Environments with BDOI IT, pipeline, resilience and DR tests |
| iorta Migration Lead | Trial migrations, reconciliation, masking verification |
| iorta Business Analyst lead | Traceability, UAT scenarios with the business, change requests |
| BIBS Product Owner | Approves the strategy, the workload model and the UAT readiness; signs the UAT summary |
| BRD product owners and UAT testers (BDOI) | Approve test plans; run UAT; sign the UAT certificate of their BRD |
| BDOI IT | Environments, integrations, partner test systems, DR region |
| BDOI Information Security | Security acceptance; names the penetration test provider |
| BDOI Data Migration Lead and data owners | Extracts, code maps, business verification of migrated data |
| Data Protection Officer | Accepts the masking rules and the privacy tests |

## RACI

<!-- table: widths=4.2,1.5,1.5,1.5,1.5,1.5,1.6,1.6,1.6 caption="RACI (R responsible, A accountable, C consulted, I informed)" size=7.5 -->
| Activity | QA lead | Perf. / Sec. leads | Dev. lead | Migration lead | BIBS PO | BRD owners and testers | BDOI IT | BDOI InfoSec |
|---|---|---|---|---|---|---|---|---|
| Test strategy and test plans | R | C | C | C | A | C | I | I |
| Test environments | C | C | I | I | I | I | A | C |
| Test data (seed and masked) | R | C | I | R | I | I | C | A |
| Unit and integration testing | I | I | A | I | I | I | I | I |
| SIT execution and exit report | A | C | C | C | I | I | C | I |
| Incident triage and fixes | R | C | R | C | I | C | C | I |
| UAT scenarios and readiness | R | I | I | C | A | R | I | I |
| UAT execution and sign-off | C | I | I | C | A | R | I | I |
| Trial migrations and reconciliation | C | I | I | R | I | R | C | I |
| Performance and volume test | C | R | C | I | A | I | C | I |
| Security tests and penetration test | C | R | C | I | I | I | C | A |
| DR drill | I | C | I | I | I | I | A | C |
| Go-live check | R | I | C | C | A | C | C | I |

# Schedule per drop

<!-- table: widths=3.8,2.4,3.6,6.8 caption="Test windows (project plan; BDOI programme timeline)" size=8 -->
| Window | Environment | Dates | Gate |
|---|---|---|---|
| Drop 1 test plans approved | - | 18 Dec 2026 | Test plans of BRD-01, 02, 04, 05, 06, 09, 10, 12 and the Drop 0 sets |
| Drop 1 SIT (with Drop 0 setup) | SIT | 4 Jan to 30 Jul 2027 | SIT exit report; UAT readiness statement 30 Jul 2027 |
| Trial migration 1 | SIT | 19 to 30 Apr 2027 | Profiling report; timings |
| Early security scan and SIT load baseline | SIT | Jun 2027 | Findings triaged before UAT |
| Drop 2 test plans approved | - | 30 Jun 2027 | Test plans of BRD-07, BRD-08 |
| Drop 2 SIT | SIT | 1 Jul to 30 Sep 2027 | SIT exit; UAT readiness statement 30 Sep 2027 |
| Trial migration 2 | SIT | 5 to 16 Jul 2027 | L1 to L5 reconciled; Migration Clearing 0.00 |
| Drop 1 UAT (end to end) | UAT | 2 Aug to 17 Dec 2027 | UAT sign-off certificate per BRD |
| Trial migration 3 (UAT load) | UAT | 2 to 13 Aug 2027 | Business verification on screen |
| Trial migration 4 | UAT | 4 to 15 Oct 2027 | Timed as a cut-over |
| Drop 2 UAT | UAT | 1 Oct to 30 Nov 2027 | Sign-off certificates of BRD-07 and BRD-08 |
| Performance and penetration test | Pre-Prod | 1 Nov to 15 Dec 2027 | NFT and security reports; 429 concurrent users within the targets; no open critical or high finding |
| Dress rehearsal | Pre-Prod | 15 to 26 Nov 2027 | Full volume within the window with 20% margin; rollback rehearsed |
| DR drill | Pre-Prod and DR | Dec 2027 | RPO and RTO met |
| ORR / PRR | - | Dec 2027 (evidence by 15 Dec 2027) | ORR / PRR approved |
| Go / no-go and go-live check | Production | 2 and 3 Jan 2028 | Go decision; smoke check passed |
| Hypercare regression | Production and UAT | 4 Jan to 2 Feb 2028 | Every hypercare fix re-tested in UAT before release |

# Reporting and sign-off

<!-- table: widths=4.4,5.8,3.2,3.2 caption="Test reports and who signs them" size=8 -->
| Report | Content | Prepared by | Signed by |
|---|---|---|---|
| Weekly test report | Cases run and passed per BRD, incidents by severity and age, coverage, risks | iorta QA lead | - (to the steering committee) |
| SIT exit report (per drop) | Results against the exit criteria, open incidents with workarounds, coverage | iorta QA lead | BIBS Product Owner |
| UAT readiness statement (per drop) | SIT exit, data loaded, testers trained, scenarios issued | iorta QA lead | BIBS Product Owner |
| UAT sign-off certificate (per BRD) | Scenarios run, open points accepted | iorta QA lead | Product owner of the BRD |
| NFT report | Results against each NFR target, deviations | iorta Performance Test Lead | BIBS Product Owner; Head, BDOI IT |
| Security test report | DAST and penetration test findings and re-tests | iorta Security Lead | BDOI Information Security |
| Migration reconciliation and dress rehearsal reports | Reconciliation L1 to L5, timings, rollback | iorta Migration Lead | BDOI Data Migration Lead |
| DR drill report | RPO, RTO, issues | iorta DevOps and Platform Lead | Head, BDOI IT |

# Risks

<!-- table: widths=5.4,2,9.2 caption="Test risks and mitigations" size=8 -->
| Risk | Impact | Mitigation |
|---|---|---|
| Integration specifications and partner test systems arrive late (DEP-07, 08, 10, 11, 13) | High | Simulators on the agreed contracts; interface cases re-run when the partner is available; dates tracked in the project plan |
| Environments late (SIT 4 Jan 2027, Pre-Prod 1 Oct 2027) | High | Environment readiness checklist one month ahead; DEV used for early SIT cycles |
| FRS re-issued on the BRD versions of 8 October 2026 change the test cases | Medium | Test plans re-based with the FRS; the traceability matrix regenerated from the FRS at each issue |
| UAT testers not released (DEP-20) | High | Testers named by 16 Jul 2027; UAT calendar per BRD agreed with the owners |
| NFR values not decided (XQ08) | Medium | NFT runs against the catalogue values; results reported against both values where BDOI has not decided |
| Penetration test findings late | Medium | Early scan in SIT in June 2027; provider named by 31 May 2027 (DEP-19) |
| Masked data incomplete or masking fails | High | Masking verification before tester access; DPO accepts the rules before trial migration 1 |

# Decisions and open points for BDOI

<!-- table: widths=0.9,9,3.6,2,1.4 caption="Decisions requested from BDOI on testing" size=8 status=Status -->
| No. | Decision | Owner (BDOI) | Needed by | Status |
|---|---|---|---|---|
| 1 | Approve the severity definitions, the entry and exit criteria and the suspension rule of this strategy | BIBS Product Owner | 30 Nov 2026 | OPEN |
| 2 | Choose the test management tool (Xray or TestRail) and grant the UAT testers access | BDOI IT | 15 Dec 2026 | OPEN |
| 3 | Switch on the new-code coverage gate (above 90%) in the BDO SonarQube instance | BDOI IT | 15 Dec 2026 | OPEN |
| 4 | Name the penetration test provider and approve the early scan in SIT (DEP-19, IQ35) | BDOI Information Security | 31 May 2027 | OPEN |
| 5 | Accept the masking rules of the test data before trial migration 1 | Data Protection Officer | 31 Mar 2027 | OPEN |
| 6 | Approve the NFT workload model (transaction mix and rates per persona) | BIBS Product Owner | 30 Sep 2027 | OPEN |
| 7 | Name at least two UAT testers per business unit and release them for the UAT windows (DEP-20) | Product owners of the BRDs | 16 Jul 2027 | OPEN |
| 8 | Partner test systems and test accounts for every interface of Drop 0 and Drop 1 in SIT | BDOI IT | 31 Mar 2027 | OPEN |
| 9 | Date of the DR drill in December 2027 and the DR region (IQ26) | BDOI IT | 31 Mar 2027 | OPEN |

# Sign-off {-}

By signing, BDOI approves this Test Strategy as the basis of the test plans, the test windows of each drop and the acceptance of BIBS phase 1.

```signoff
rows:
  - {name: "", role: "BIBS Product Owner", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: "Head, BDOI IT", organisation: BDOI}
  - {name: "", role: "Head, BDOI Information Security", organisation: BDOI}
  - {name: "", role: "QA lead", organisation: iorta TechNXT}
```
