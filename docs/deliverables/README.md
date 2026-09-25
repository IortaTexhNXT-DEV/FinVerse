# BIBS client deliverables: plan and status

The client pack for BDO Insurance and Reinsurance Brokers (BDOI). Every document uses the BDO Insure template:
- logo;
- Header Blue / CTA Blue / Yellow;
- document control and version history;
- the "Powered by iorta TechNXT" line.

**Formats.** Word, Excel and PowerPoint are the editable masters, and they are the only files kept in
`docs/deliverables/out/`. PDFs are produced from the masters when a version is issued to BDOI (`--keep-pdf`) and are not
committed, so a change is always made once, in the master.

Word, Excel and PowerPoint files are produced by the generators in `tools/deliverables/` from sources kept in the repository,
so each document can be regenerated after every build.

**Timing**
- **A.** Starts now: the source is the BRDs and the designs. It is refreshed with build references at the end.
- **B.** After the end-to-end build: the source is the running platform, its code and test results.

| # | Deliverable | Format | Timing | Source |
|---|---|---|---|---|
| 1 | Functional Requirements Specification (FRS), one per BRD | Word | A | BRD, spec, design |
| 2 | BRD discrepancy, conflict, impact and clarification register | Excel | A | specs (observations and questions), cross-BRD decisions |
| 3 | Test plan per BRD: test conditions, scenarios, positive and negative test cases | Excel + Word summary | A | FRS |
| 4 | Bill of materials (infrastructure and technology), coding and quality standards | Word + Excel | A | `pom.xml`, `package.json`, deployment files, hosting appendix |
| 5 | Fit-gap per module | Excel | B | specs and traceability, updated after build |
| 6 | Menu, screens, fields and roles workbook for client concurrence | Excel | B | `navigation/modules.ts`, screens, permissions, roles |
| 7 | End-to-end deck by persona: actor, role, usage, expected outcome, observation, screenshots | PowerPoint | B | demo walkthrough and screenshots |
| 8 | Persona-based end-to-end test execution and results | Excel + Word | B | automated and manual runs |
| 9 | Professional BDO theme on every screen and document; plain, specific wording | all | A + B | UX guidelines, writing standard |
| 10 | Every function works: CRUD, upload and download (with templates), document prints, schedules, reports; output checked | test evidence | B | test runs and output samples |
| 11 | Data dictionary | Excel | B | database schema, entity Javadoc |
| 12 | Technical and deployment architecture, with diagrams | Word | A (refreshed in B) | code, deployment files, hosting appendix |
| 13 | Persona-based manual end-to-end test: where each persona gets stuck | Excel + findings report | B | manual run with data entry |
| 14 | Persona-based menus: each user sees only their screens | verified in the platform | B | permissions audit and tests |
| 15 | Notifications and workflows per role | test evidence | B | workflow and notification tests |
| 16 | Configuration over code: rules, validations and definitions maintained from front-end masters by the System Administrator | gap list and fixes | B | code review of hard-coded rules |
| 17 | Reports in Excel and PDF; document schedules in Word and PDF | platform change and tests | B | report and docgen modules. Built: `DocxReportRenderer` (layout of the PDF, print options honoured) and DOCX in the API, batches, archive and scheduled files; Word on documents and schedules (`ReportMetadata.asDocument`: GL-SCHEDULE, GL-BVA, FRBS-MANCOM-MARKET, RI-SOA, FIN-AP-VOUCHER); every composed business document downloadable as Word (`doc_rendition`, Word copy offer after each PDF download); templates to and from Word; BDO Insure logo, Header Blue tables, "Confidential" footer and page x of y on PDF, Word and Excel |
| 18 | Screen-by-screen and field-by-field alignment review | findings and fixes | B | screenshots and review |
| 19 | API catalogue | Excel + OpenAPI file | B | OpenAPI specification |
| 20 | Upload and download templates in one folder | files | B | bulk handlers, report layouts |
| 21 | Requirements traceability matrix: BRD, FRS, design, code, test, result | Excel | A (refreshed in B) | all of the above |

## Added to the pack (recommended)

| # | Deliverable | Format | Timing |
|---|---|---|---|
| 22 | User manual per persona | Word | B |
| 23 | System administration and configuration guide (masters, parameters, roles, jobs) | Word | B |
| 24 | Operations and production support runbook (L1 / L2 / L3, incident playbooks, logs, jobs, restart and recovery) | Word | B |
| 25 | Installation and deployment guide (environments, configuration, secrets, releases) | Word | B |
| 26 | Security and data-protection controls mapping (hosting appendix, masking of non-production data, access, audit) | Word + Excel | A + B |
| 27 | Code quality report (static analysis, coverage, dependency and licence scan) | Excel | B |
| 28 | Performance and volume test plan and results (against the NFRs of each BRD) | Word + Excel | B |
| 29 | Data migration approach (legacy EBIX, ISYS, QPS and Excel sources, reconciliation) | Word | A |
| 30 | UAT plan and sign-off forms per BRD | Word | B |
| 31 | Release notes and open-questions log for BDOI | Word + Excel | B |

## More recommended items

| # | Deliverable | Format | Timing |
|---|---|---|---|
| 32 | Document review and sign-off tracker: document, version, the BDOI department and approver (for each BRD), review comments with responses, approval date | Excel | A (from now on) |
| 33 | Business process maps per department (to-be swimlanes: department, role, system step, hand-off), for business-user sign-off alongside the FRS | Word | A |
| 34 | Field registry: one list of every screen field (label, type, mandatory, LOV, validation, FRS reference). It drives the screens, the FRS field tables and the workbook of item 6, with an automated check that they agree | Excel + automated test | B |
| 35 | Error and message catalogue: every business rule code, the message the user sees, the cause, and what support does about it | Excel | B |
| 36 | Configuration catalogue: every parameter, LOV, rule, matrix, template and job schedule, with its owner, default and screen (supports item 16) | Excel | B |
| 37 | NFR compliance matrix: each NFR of each BRD, how BIBS meets it, and the evidence (performance, security scan, DR test, backup restore) | Excel | B |
| 38 | Observability and alert catalogue: dashboards, metrics, log fields (correlation id), and alerts routed to PagerDuty with their runbook step | Word + Excel | B |
| 39 | Training material per department (quick-reference cards and walkthroughs), derived from the user manuals | Word + PowerPoint | B |
| 40 | Accessibility and browser support statement (WCAG 2.1 AA checks on the main screens) | Word | B |
| 41 | Business process deck: As-Is, Envisioned (BIBS), gaps and best practice per business area, plus the holistic enterprise view (current, envisioned, best practice) | PowerPoint | A (refreshed in B) |

## FRS with annotated screenshots (FRS v1.1)

- **FRS v1.0 (text):** issued first, so each BDOI department can review the requirements now.
- **FRS v1.1 (after the build and the screen-alignment pass):** each process area gains:
  - annotated screenshots, where numbered markers on the fields tie to the rows of the FR field table;
  - a persona flow strip (screen by screen, with the action and the result);
  - the validations and messages shown on the screen;
  - cross-module impacts.
- **How the screenshots are made:** by the capture tool (`tools/screenshots`), which reads the field positions from the page, so they
  can be regenerated after every change.
- **Precondition:** the field registry (item 34) must agree with the screens and the FRS. Screen labels, order, mandatory markers
  and messages must match the FRS before the screenshots are taken.

## Governance: from BRD to production

1. FRS v1.0 per BRD → review by the owning BDOI department → comments answered in the sign-off tracker (item 32) → FRS approved.
2. Discrepancy register (item 2) resolved with BDOI; answers flow back into the FRS and the designs.
3. Build aligned to the approved FRS, plus the screen and field alignment pass (items 17, 18 and 34), then FRS v1.1 with annotated screenshots.
4. Test cases derived from the approved FRS (item 3), traced in the RTM (item 21), then SIT results (item 8) and persona end-to-end tests (items 12 and 13).
5. NFR evidence (item 37), architecture conformance to the client BOM (item 4, including Redis and Kafka), and the code-quality report (item 27).
6. UAT per department with sign-off (item 30). Production readiness means the runbook, observability, alerts and error catalogue are in place (items 24, 35 and 38).

## Writing standard (binding for every document)

- Write as the project team writes to the client: specific and factual, with BRD IDs, screen names, field names and values.
- No filler words ("seamless", "robust", "comprehensive", "leverage", "cutting-edge"), no rhetorical questions, no marketing tone.
- Short sentences in the active voice. Use tables for anything that has more than two attributes.
- Every requirement, test and finding cites its source (BRD ID and page, or file and line).
- Use BDOI's own terms (ARN, PRF, TSU, DP, FFY, CLPC, RA) exactly as the BRDs use them. Define each acronym once in the glossary.

## Toolkit and status

Toolkit: [`tools/deliverables/`](../../tools/deliverables/README.md) (Word, Excel and PowerPoint builders, PDF and
page previews). Sources: `docs/deliverables/src/`; outputs: `docs/deliverables/out/`, named
`BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>`.

| # | Document | Status |
|---|---|---|
| 1 | FRS BRD-3 Product Maintenance (reference FRS for the other BRDs) | Issued v1.0 for BDOI review: `out/FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx` and `.pdf` |
