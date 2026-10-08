---
# Delivery Methodology of the BIBS programme (BRD-00). Built by build_delivery_adoption.py.
title: Delivery Methodology
subtitle: Hybrid delivery by drops and iterations, definitions of ready and done, quality gates, configuration and release management, change control, documentation standards and governance
doc_type: Delivery Methodology
doc_code: Delivery
brd: BRD-00
name: Delivery Methodology
doc_id: BIBS-DM-BRD-00
version: "1.0"
date: 08 October 2026
status: Issued for BDOI review
header_title: BIBS Delivery Methodology
h1_page_break: false
control:
  - version: "1.0"
    date: 08 Oct 2026
    author: iorta TechNXT Project Manager
    reviewer: iorta TechNXT Solution Architect; iorta TechNXT QA lead
    approver: Program Manager, Business Project Services (pending)
    change: First issue
distribution:
  - {name: "Program Manager, Business Project Services", role: Approver, organisation: BDO Unibank ESG, purpose: "Approval of the delivery approach and governance"}
  - {name: "BIBS Product Owner", role: Approver, organisation: BDOI, purpose: "Ready and done, change control"}
  - {name: "Head, BDOI IT", role: Reviewer, organisation: BDOI, purpose: "Quality gates, release and configuration management"}
  - {name: "Head, BDOI Information Security", role: Reviewer, organisation: BDOI, purpose: "Security scans and patching"}
  - {name: "Business owners of BRD-01 to BRD-13", role: Reviewers, organisation: BDOI, purpose: "Ceremonies, sign-off and change control"}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Applies the methodology"}
---

# Purpose, audience and scope

## Purpose

This document states how the BIBS programme is delivered: how the BDOI drops and the iterations of the delivery team fit together, when a requirement is ready for development and when a change is done, the quality gates every change passes, how configuration and releases are managed, how requirement changes are controlled, which documentation standards apply and how the programme is governed. It is the agreement between BDOI and iorta TechNXT on the way of working; the dates are in the Project Plan.

## Audience

The Program Manager of Business Project Services and the BIBS Product Owner (approval), the business owners of each BRD and their key users (ceremonies, reviews, sign-off), BDOI IT and Information Security (quality, release, security), and the iorta TechNXT project team.

## Scope

All work of the programme from requirements to the hand-over to production support: Drop 0, Drop 1, Drop 2, the data migration stream, the integrations with BDO systems and the readiness activities. Production support after the hypercare follows the support model agreed in the Knowledge Transfer Plan.

# Delivery approach

BIBS is delivered in a **hybrid** way. The **plan-driven frame** comes from BDOI: three drops that split the requirements, test and sign-off workload, one end-to-end UAT, and one go-live of all modules together in January 2028. Inside that frame the delivery team works in **two-week iterations**: each iteration takes requirements that meet the definition of ready, delivers them through the quality gates, shows them to the key users and feeds the release that goes to SIT.

![Hybrid delivery: drops, iterations, releases and test stages](figures/dm_lifecycle.dot){width=15}

## Principles

- **Configure before changing.** A requirement is met by configuration (lists, parameters, rules, templates, workflow stages) where the platform allows it; a change to the application is the second choice and is traced to its FR.
- **One source of truth per item.** BRD (what BDOI needs) - FRS (how BIBS meets it) - test case - UAT result - release note; every link carries the BRD requirement ID and page.
- **Business content in the sign-off sets.** Sign-off sets hold business content only; technical detail goes to the Technical Specification reviewed by BDOI IT.
- **Seed data outside production.** SIT, UAT and training use seed data and masked extracts; production data never leaves production unmasked.
- **Small, frequent releases.** A release into SIT at the end of every iteration; regression on every release; nothing reaches UAT that has not passed SIT.
- **Decisions are recorded.** Every clarification, decision and change has an ID, an owner and a date (clarification chapters, decision logs, Change Management Register, RAID log).

## Drops and development waves

<!-- table: widths=2.4,4,3.4,6.8 caption="Drops, development waves and releases" size=8.5 -->
| Drop | BRDs | Development | Content of the releases |
|---|---|---|---|
| Drop 0 | BRD-03, BRD-11, BRD-13 (and GL accounts of BRD-05) | First wave, Nov - Dec 2026; Migration Console Nov 2026 - Mar 2027 | Setup, sign-in and user access, product maintenance, data management, workflow; migration functions |
| Drop 1 | BRD-01, 02, 04, 05, 06, 09, 10, 12 | First wave (upstream) Nov - Dec 2026; second wave (downstream) Jan - Feb 2027 | Client onboarding to booking, renewal, submitted policies; cashiering, remittance, adjustment, accounting, disbursement, commission receivables, reports |
| Drop 2 | BRD-07, BRD-08; Production Reconciliation; Marketing Collection extraction | Third wave, Mar - Apr 2027 | Claims, Employee Benefits without portal, other reports, EGL, EDP and insurer exchanges; CSF Case Management if approved |

# Iterations and ceremonies

<!-- table: widths=3.2,2.4,2.4,4.6,4 caption="Ceremonies of the delivery team" size=8.5 -->
| Ceremony | When | Length | Who | Output |
|---|---|---|---|---|
| Daily stand-up | Every working day, 09:15 PHT | 15 minutes | Delivery team; iorta Project Manager | Impediments raised; plan of the day |
| Refinement | Weekly, Wednesday | 60 minutes | Business Analyst lead, Solution Architect, QA lead, key user of the BRD when needed | Items meet the definition of ready |
| Iteration planning | First Monday of the iteration | 2 hours | Delivery team; BIBS Product Owner or delegate | Iteration goal and committed items |
| Iteration review | Last Thursday of the iteration | 60 to 90 minutes | Delivery team; key users and business owner of the BRDs shown | Feedback recorded; accepted items |
| Retrospective | Last Friday of the iteration | 45 minutes | Delivery team | Two improvement actions per iteration |
| Release planning | At each drop start and monthly | 2 hours | iorta Project Manager, Solution Architect, BIBS Product Owner, BDOI IT | Release calendar into SIT and UAT |
| Business working group | Weekly per BRD in requirements and UAT | 60 minutes | Business owner, key users, Business Analyst lead | Clarifications decided; UAT progress |

The iteration review is held on the SIT environment with seed data, never on a developer workstation. Key users see the screens they will test, which keeps the UAT short.

# Definition of ready and definition of done

## Definition of ready (an item may enter an iteration)

<!-- table: widths=1,6.6,9 caption="Definition of ready" size=8.5 -->
| No. | Criterion | Evidence |
|---|---|---|
| R1 | The item traces to a BRD requirement (ID and page) or to an approved CR | FRS traceability chapter; CR number |
| R2 | The FR has actor, main flow, alternate flows, rules, validations with the messages word for word, fields and acceptance criteria | FR block of the FRS |
| R3 | The screens are specified (fields, actions, statuses, messages) and placed in the screen register | Screen specification; UX screen register |
| R4 | Open clarifications are decided, or the working assumption is recorded with its decision date | Clarification chapter of the FRS |
| R5 | Test cases exist for every acceptance criterion | Test plan and its cases |
| R6 | Dependencies are available or simulated in SIT only with the agreement of BDOI IT (interfaces, configuration inputs, seed data) | Dependency list of the Project Plan |
| R7 | The effort is estimated and fits the iteration | Iteration plan |

## Definition of done

<!-- table: widths=2.4,7.2,7 caption="Definition of done at three levels" size=8.5 -->
| Level | Criteria | Evidence |
|---|---|---|
| Item | Code reviewed and approved by a second developer; every quality gate passed; tests of the acceptance criteria pass; screens follow the BDO UX guidelines (labels not codes, names not logins, dd-MMM-yyyy dates, formatted amounts, tables with row action menus); Help Center text updated; FRS updated where the behaviour changed | Review record; gate results; test results |
| Release | All items of the release done; regression passed in SIT; release notes per persona; deployment configuration versioned; SBOM produced; no open Severity 1 or 2 incident introduced by the release | Release notes; regression report; SBOM |
| Drop | All FRs of the drop delivered or deferred by an approved CR; SIT exit criteria met; UAT signed per BRD; user guides and quick-reference guides updated; training material updated; knowledge base updated | SIT exit report; UAT sign-off certificates; documentation versions |

# Quality gates

Every change passes the same gates in the continuous integration pipeline; a change that fails a gate is not accepted into the mainline. The gates are set in the project configuration and are the same on every developer machine and in the pipeline. No rule is weakened or suppressed without a written reason next to the exclusion.

![Quality gates of every change](figures/dm_quality_gates.dot){width=14}

<!-- table: widths=3.4,8,5.2 caption="Gates and thresholds" size=8.5 -->
| Gate | What is checked | Threshold |
|---|---|---|
| Code review | A second developer reviews every change against the FR, the coding standard and the security checklist | One approval; the author never approves his own change |
| Formatting | Java and TypeScript formatting | No difference allowed |
| Coding standard | Naming, size and complexity (Checkstyle; ESLint with the SonarSource rules, TypeScript strict) | 600 lines per file, 60 lines per method, 120 columns; cognitive complexity 15, cyclomatic 12 |
| Code smells and correctness | PMD, SpotBugs with security rules, compiler warnings as errors | No finding |
| Duplication | Copy-paste detection | Blocks of 120 tokens or more fail |
| Architecture | Layering and module boundaries (ArchUnit) | No module cycle; no access across bounded contexts outside their published services |
| Tests | Module and integration tests on PostgreSQL 16; web client tests | All pass |
| Coverage | Line and branch coverage of the server code | 80 percent lines, 65 percent branches |
| Accessibility | Accessibility lint of the web client | No finding |
| Code scanning | CodeQL on Java and TypeScript | No new high finding |
| Dependencies | OWASP dependency-check (server), audit of the web packages | Fails at CVSS 7.0 or higher, or on a high or critical advisory |
| Container images | Trivy on the server and web images | Fails on a high or critical vulnerability with a fix available |
| Secrets | gitleaks over the whole history | Any finding fails |
| Software bill of materials | CycloneDX SBOM of the server and the web client | Produced and kept with every release |

**Accepted findings.** A finding is fixed by upgrading the library, the base image or the package. An accepted finding is the exception: its record names the reason it does not apply, the reviewer, the review date and an expiry date at most six months ahead; after the expiry the gate fails again. Information Security sees every accepted finding in the release notes.

**Patching.** Security advisories of the components of the baseline (for example the Spring Framework 6.2 line under Spring Boot 3.5) are applied in a monthly patch release; a critical finding is patched within 7 days and a high finding within 30 days, with the regression run before the patch reaches UAT or production.

## Test levels and incident severities

<!-- table: widths=3.2,5.4,4,4 caption="Test levels" size=8.5 -->
| Level | Scope | Who | Environment |
|---|---|---|---|
| Developer tests | Module and integration tests of every change | iorta development team | Pipeline |
| SIT | Test cases of the test plan per BRD; end-to-end walkthroughs; regression per release | iorta QA | SIT |
| Integration test | Exchanges with EIAM, UIDM-ISC, Apigee X and the BDO systems | iorta QA with BDOI IT and the BDO IT teams | SIT, UAT |
| Trial migrations | Loads, reconciliation L1 to L5, timings | Data Migration Lead, iorta Migration Lead | SIT, UAT, Pre-Prod |
| UAT | Business scenarios end to end on migrated data | Key users and UAT testers | UAT |
| Usability tests | Task-based sessions with users per persona | iorta change and training lead with key users | UAT, training |
| Performance test | Peak of 429 concurrent users and the batch window | BDOI IT with iorta QA | Pre-Prod |
| Penetration test | Application and infrastructure | BDOI Information Security or its provider | Pre-Prod |

<!-- table: widths=2.2,8,3.2,3.2 caption="Incident severities and targets during SIT and UAT" size=8.5 -->
| Severity | Meaning | Response | Fix delivered |
|---|---|---|---|
| 1 Critical | A process stops for all users or data is wrong with no workaround | 2 hours | Next working day (patch release) |
| 2 High | A process stops for some users or a control fails; workaround hard | 1 working day | Within 5 working days |
| 3 Medium | Wrong behaviour with an acceptable workaround | 2 working days | Next release |
| 4 Low | Cosmetic or wording | 5 working days | Planned release |

# Configuration and release management

## Configuration items

<!-- table: widths=4,7.2,5.4 caption="Configuration items and where they are versioned" size=8.5 -->
| Item | How it is versioned | Change route |
|---|---|---|
| Application (server and web client) | One mainline; each release tagged with its number; release lines for patches of a released version | Iteration and patch releases |
| Container images | Packaged once per release and promoted unchanged from SIT to production; base images pinned by digest | Release |
| Deployment configuration per environment | Versioned Kubernetes configuration per environment (replicas, resources, Gateway API routes, settings) | Release with BDOI IT approval for Pre-Prod and production |
| Data structure changes | Versioned with the application, applied automatically at deployment, forward only | Release |
| Business configuration (lists, parameters, rules, templates, products) | Entered on the screens with maker-checker, or loaded through the configuration inputs workbook; every change in the audit trail | Configuration owner until the due date; then CR |
| Reference data and code maps of the migration | Versioned code maps in the Migration Console; frozen at T-7 (27-Dec-2027) | Data Migration Lead; CR after the freeze |
| Secrets and keys | Secret store of the environment; never in files or documents | BDOI IT |
| Documents | Version in the file name and on the cover; one current version per folder | Document owner; review and approval |

## Release flow

1. **Iteration release** every two weeks into SIT, with release notes listing the FRs, CRs and incidents it closes.
2. **UAT release** when SIT has passed the release; the UAT calendar fixes the release days so that testers are not interrupted (proposed Monday evenings).
3. **Pre-Prod and production release** through the BDOI change management: a change record approved by BDOI IT, the deployment by the iorta infrastructure team and BDO IT operations, a smoke test and a rollback plan for each release.
4. **Patch release** for a Severity 1 or 2 incident or a security finding, from the release line of the version in use, with the same gates and a reduced regression agreed with the QA lead.
5. **Release freeze.** From 10-Dec-2027 (proposed) only Severity 1 and 2 fixes enter the go-live release; after go-live, the first planned release follows the hypercare exit.

Release numbers are year.drop.iteration with a patch digit (for example 2027.1.06.1); the version shows on the Application Info screen and in the release notes.

# Requirement change control

## When change control applies

- Before signature, a question on a set is answered in its comments log; a correction is made once, in the next version of the set (for example 2.1), with the changed rows marked.
- After signature, nothing in a signed set changes without a change request (CR) in the Change Management Register: new BRD versions, scope changes (for example Customer Servicing Case Management), new or changed rules, screens, reports, interfaces and data objects.
- A configuration value is changed by its owner until its due date; after it, by a CR. Code maps and reference values freeze at T-7.

## Process

![Requirement change control](figures/dm_change_control.dot){width=14}

<!-- table: widths=2.6,7,4,3 caption="Steps of the change request process" size=8.5 -->
| Step | What happens | Who | Time |
|---|---|---|---|
| 1 Request | The requester raises a CR on the CR form, or the project team raises one from a recorded deviation | Requester; iorta Project Manager logs it | Same day |
| 2 Impact analysis | Scope, BRD and FRS references, affected modules, data, interfaces, tests, documents, drop; effort in man-days (analysis, development, test, documentation) | iorta Business Analyst and Solution Architect; business owner of the BRD | 5 working days (10 for L and XL) |
| 3 CCB review | Accept, reject, defer, or ask for more analysis | Change Control Board | Fortnightly; ad hoc for High priority |
| 4 Approval | CR form signed by the decision owner and the BIBS Product Owner; above 25 man-days or when a drop date moves, also the Program Manager | Decision owner; BIBS Product Owner; Program Manager | 5 working days after the CCB |
| 5 Schedule | Placed in a drop or release; plan, FRS change list and test plan updated | iorta Project Manager; Program Manager | Next planning cycle |
| 6 Implement | Configuration, development, FRS revision, test cases, guides | Delivery team | As scheduled |
| 7 Verify | SIT and regression; the owner checks the behaviour in SIT or UAT | iorta QA; business owner | Drop test window |
| 8 Close | Result and FRS version recorded; listed in the release notes | iorta Project Manager; BIBS Product Owner | At release |

CR sizes: S 1 to 3 man-days, M 4 to 10, L 11 to 25, XL above 25. The Change Control Board is chaired by the BIBS Product Owner with the Program Manager of Business Project Services, the owner of the BRD concerned, the Head of Comptrollership (postings, GL, tax), the Chief Compliance Officer (screening, KYC, access, retention), BDOI IT (interfaces and infrastructure) and, for iorta TechNXT, the Project Manager (secretary), the Solution Architect and the Business Analyst lead.

# Documentation standards

<!-- table: widths=3.6,13 caption="Documentation standards" size=8.5 -->
| Topic | Standard |
|---|---|
| Template | BDO Insure template for Word, Excel and PowerPoint: cover, document control, distribution list, table of contents, numbered chapters, "Confidential - BDOI" footer |
| File names | BIBS_<document type>_BRD-nn_<name>_v<version>, for example BIBS_FRS_BRD-03_Product_Maintenance_v2.0.docx; programme documents use BRD-00 |
| Folders | One release-set folder per BRD in the folder of its drop; programme documents by kind under Programme |
| Versions | x.0 for an issue for review or sign-off; x.1, x.2 for corrections; only the current version stays in a folder; every version has a row in the document control table |
| Writing | Specific and active ("the Team Lead approves"); BRD requirement IDs with pages; dates dd-MMM-yyyy; amounts formatted; no filler; terms of the glossary |
| Business sign-off sets | Business content only; every set carries a "Who signs what" matrix from the BRD approval sheet; technical content in the Technical Specification |
| Screens in documents | Screenshots of the SIT environment with seed data; screen IDs from the screen register; changed screens re-captured with each release and marked in the register |
| Figures | Drawn from versioned sources in BDO colours; legible at page width |
| Review and approval | Author, reviewer and approver named in the document control; BDOI comments in the comments log of the set |
| Storage | Masters (Word, Excel, PowerPoint) in the programme document store; PDFs produced at issue |

# Governance cadence

![Governance bodies and cadence](figures/dm_governance.dot){width=15}

<!-- table: widths=3.2,3.2,5,2.2,3 caption="Governance bodies" size=8.5 -->
| Body | Chair | Members | Cadence | Decides or produces |
|---|---|---|---|---|
| Steering committee | Program Manager, Business Project Services | BIBS Product Owner; heads of Operations, Comptrollership, Marketing, Compliance, BDOI IT, Information Security; iorta TechNXT engagement lead | Monthly; at each drop closure; go / no-go | Direction, milestone changes, drop closure, go-live, escalations |
| Programme meeting | iorta Project Manager | Program Manager, BIBS Product Owner, workstream leads | Weekly (Tuesday) | Status, dependencies due in four weeks, RAID review, actions |
| Change Control Board | BIBS Product Owner | See change control | Fortnightly | CR decisions |
| Design authority | Head, BDOI IT | iorta Solution Architect, Information Security, BDO IT EIAM, IGA and API teams | Fortnightly | Architecture, integration and security decisions |
| Business working group | Business owner of the BRD | Key users, iorta Business Analyst lead, QA lead | Weekly in requirements and UAT | Clarifications, UAT progress, sign-off readiness |
| Data migration working group | Data Migration Lead | Data owners and stewards, BDOI IT, iorta Migration Lead | Weekly | Extracts, code maps, trial results, gates |
| Change and training working group | BIBS Product Owner | Change champions, key users, iorta change and training lead | Fortnightly from April 2027, weekly from October 2027 | Communications, training, adoption KPIs |

## Status reporting

The weekly status report gives: milestones of the next eight weeks with their status (on track, at risk, late); dependencies due in the next four weeks; High risks and issues; CRs by status; SIT or UAT progress (cases run, passed, failed, blocked; incidents by severity and age); and the decisions needed from BDOI. The steering committee receives the monthly summary with the plan, the RAID heat map and the decisions.

<!-- table: widths=5,6.6,5 caption="Delivery indicators" size=8.5 -->
| Indicator | Measure | Target |
|---|---|---|
| Milestone adherence | Milestones met on their date | 90 percent; none on the critical path late |
| Sign-off cycle | Days from issue of a set to signature | 30 days or less |
| CR cycle time | Days from request to CCB decision | 15 working days or less |
| SIT pass rate | Cases passed at first run per release | 85 percent or more |
| Incident ageing | Open Severity 1 and 2 incidents older than their fix target | Zero |
| Gate health | Pipeline runs failing a gate, fixed the same day | 95 percent |

# Roles and responsibilities

The full RACI is in the workbook **BIBS_Delivery_BRD-00_RACI_Matrix_v1.0.xlsx**. The extract below covers governance, requirements, development and test.

<!-- da:raci Governance,Requirements,Development,Test -->

<!-- da:roles -->

# References

- Project Plan workbook and summary, version 1.0 (8-Oct-2026); RACI Matrix; Risk Register (RAID log).
- Change Management Register and summary, version 1.0 (CR process, CCB, sizes).
- Drop 0 closure summary, version 2.0 (change control after signature, configuration inputs).
- Quality gates of the BIBS platform; security baseline of BIBS; BDO UX guidelines and UX readiness checklist.
- Deliverables writing standard and toolkit conventions of the BIBS client pack.
- Client decisions of 8-Oct-2026 on the component baseline.

# Decisions and open points for BDOI

<!-- table: widths=1.2,8.4,3.4,2,1.6 caption="Decisions and open points" size=8.5 -->
| No. | Decision or open point (our proposal) | Owner | Needed by | Ref. |
|---|---|---|---|---|
| DM-01 | Approve the hybrid approach: two-week iterations inside the BDOI drops, with an iteration review for key users on SIT | Program Manager; BIBS Product Owner | 30-Oct-2026 | - |
| DM-02 | Approve the definitions of ready and done and the incident severities | BIBS Product Owner; BDOI IT | 30-Oct-2026 | - |
| DM-03 | Confirm the patch times for security findings: critical 7 days, high 30 days | BDOI Information Security | 30-Nov-2026 | R-05 |
| DM-04 | Confirm the BDOI change management route for Pre-Prod and production releases (change record, approvers, release windows) | BDOI IT | 31-Mar-2027 | - |
| DM-05 | Confirm the release freeze for the go-live release from 10-Dec-2027 | Steering committee | 1-Oct-2027 | - |
| DM-06 | Name the members of the design authority, including the BDO IT EIAM, IGA and API teams | Head, BDOI IT | 30-Nov-2026 | R-04 |
| DM-07 | Confirm the SonarQube instance and the container registry to use in the BDOI environments | BDOI IT | 31-Dec-2026 | IQ29 |
