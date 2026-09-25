# BIBS client deliverables: plan and status

The client pack for BDO Insurance and Reinsurance Brokers (BDOI). Every document uses the BDO Insure template:
- logo;
- Header Blue / CTA Blue / Yellow;
- document control and version history;
- the "Powered by iorta TechNXT" line.

Word, Excel and PowerPoint files are produced by the generators in `tools/deliverables/` from sources kept in the repository,
so each document can be regenerated after every build.

**Timing**
- **A.** Starts now: the source is the BRDs and the designs. It is refreshed with build references at the end.
- **B.** After the end-to-end build: the source is the running platform, its code and test results.

| # | Deliverable | Format | Timing | Source |
|---|---|---|---|---|
| 1 | Functional Requirements Specification (FRS), one per BRD | Word + PDF | A | BRD, spec, design |
| 2 | BRD discrepancy, conflict, impact and clarification register | Excel | A | specs (observations and questions), cross-BRD decisions |
| 3 | Test plan per BRD: test conditions, scenarios, positive and negative test cases | Excel + Word summary | A | FRS |
| 4 | Bill of materials (infrastructure and technology), coding and quality standards | Word + Excel | A | `pom.xml`, `package.json`, deployment files, hosting appendix |
| 5 | Fit-gap per module | Excel | B | specs and traceability, updated after build |
| 6 | Menu, screens, fields and roles workbook for client concurrence | Excel | B | `navigation/modules.ts`, screens, permissions, roles |
| 7 | End-to-end deck by persona: actor, role, usage, expected outcome, observation, screenshots | PowerPoint + PDF | B | demo walkthrough and screenshots |
| 8 | Persona-based end-to-end test execution and results | Excel + Word report | B | automated and manual runs |
| 9 | Professional BDO theme on every screen and document; plain, specific wording | all | A + B | UX guidelines, writing standard |
| 10 | Every function works: CRUD, upload and download (with templates), document prints, schedules, reports; output checked | test evidence | B | test runs and output samples |
| 11 | Data dictionary | Excel | B | database schema, entity Javadoc |
| 12 | Technical and deployment architecture, with diagrams | Word + PDF | A (refreshed in B) | code, deployment files, hosting appendix |
| 13 | Persona-based manual end-to-end test: where each persona gets stuck | Excel + findings report | B | manual run with data entry |
| 14 | Persona-based menus: each user sees only their screens | verified in the platform | B | permissions audit and tests |
| 15 | Notifications and workflows per role | test evidence | B | workflow and notification tests |
| 16 | Configuration over code: rules, validations and definitions maintained from front-end masters by the System Administrator | gap list and fixes | B | code review of hard-coded rules |
| 17 | Reports in Excel and PDF; document schedules in Word and PDF | platform change and tests | B | report and docgen modules |
| 18 | Screen-by-screen and field-by-field alignment review | findings and fixes | B | screenshots and review |
| 19 | API catalogue | Excel + OpenAPI | B | OpenAPI specification |
| 20 | Upload and download templates in one folder | files | B | bulk handlers, report layouts |
| 21 | Requirements traceability matrix: BRD, FRS, design, code, test, result | Excel | A (refreshed in B) | all of the above |

## Added to the pack (recommended)

| # | Deliverable | Format | Timing |
|---|---|---|---|
| 22 | User manual per persona | Word + PDF | B |
| 23 | System administration and configuration guide (masters, parameters, roles, jobs) | Word + PDF | B |
| 24 | Operations and production support runbook (L1 / L2 / L3, incident playbooks, logs, jobs, restart and recovery) | Word | B |
| 25 | Installation and deployment guide (environments, configuration, secrets, releases) | Word | B |
| 26 | Security and data-protection controls mapping (hosting appendix, masking of non-production data, access, audit) | Word + Excel | A + B |
| 27 | Code quality report (static analysis, coverage, dependency and licence scan) | PDF + Excel | B |
| 28 | Performance and volume test plan and results (against the NFRs of each BRD) | Word + Excel | B |
| 29 | Data migration approach (legacy EBIX, ISYS, QPS and Excel sources, reconciliation) | Word | A |
| 30 | UAT plan and sign-off forms per BRD | Word | B |
| 31 | Release notes and open-questions log for BDOI | Word + Excel | B |

## Writing standard (binding for every document)

- Write as the project team writes to the client: specific and factual, with BRD IDs, screen names, field names and values.
- No filler words ("seamless", "robust", "comprehensive", "leverage", "cutting-edge"), no rhetorical questions, no marketing tone.
- Short sentences in the active voice. Use tables for anything that has more than two attributes.
- Every requirement, test and finding cites its source (BRD ID and page, or file and line).
- Use BDOI's own terms (ARN, PRF, TSU, DP, FFY, CLPC, RA) exactly as the BRDs use them. Define each acronym once in the glossary.
