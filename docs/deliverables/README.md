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
| 28 | Performance and volume test plan and results (against the NFRs of each BRD; the peak case is 429 concurrent sessions, the sum of the user rows of the Core Replacement umbrella BRD p.42, until BDOI answers DCR-166 / CRQ21) | Word + Excel | B |
| 29 | Data migration approach (legacy EBIX, ISYS, QPS and Excel sources, reconciliation). Sources: the BRD-13 spec [`BDOI_DM_BRD_SPEC.md`](../requirements/BDOI_DM_BRD_SPEC.md), the design [`DATA_MIGRATION_DESIGN.md`](../architecture/DATA_MIGRATION_DESIGN.md), the FRS [`FRS_BRD13_DATA_MIGRATION.md`](src/frs/FRS_BRD13_DATA_MIGRATION.md) and the migration document set in [`src/migration/`](src/migration/); the umbrella BRD's client-migration volumes (BRD-00 p.43) | Word | A |
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

## UAT readiness programme (after the end-to-end build)

The platform goes to BDOI UAT only when every item below is complete and evidenced. Each step is run through the
screens with data keyed field by field, not only through automated tests. Every result is recorded against the
requirement it proves.

**1. Users and access set up from the front end.** The System Administrator creates each persona's user on the
User Access screens through a request and its approval. No user is seeded by script for this run.
- Each persona signs in and sees only its own menus and screens (items 14 and 6).
- A screen the persona is not granted is refused, even when it is opened by URL.

**2. Persona end-to-end runs per BRD and FRS.** Each persona keys its own transactions, and the work moves to the
next persona through the workflow: approvals, returns, hand-offs between departments and notifications (items 7, 8,
13 and 15). Every FR, acceptance criterion and test case of the test plans (item 3) is executed on screen. Every
field-level validation is tried with valid, invalid and boundary values.

**3. Cross-module continuity (the "disconnect" check).** One record is followed through every module it touches.
Amounts, statuses and references must agree at each hand-off:
- quotation → placement → issuance → booking → invoice → Collections → Cashiering receipt → remittance to the insurer
  → commission → Accounting (GL entries, sub-ledgers) → FRBS schedules and reports;
- claims, renewals, Employee Benefits, Customer Servicing, Sanction Screening, User Access and Submitted Policies, each
  with its link into Operations and Accounting;
- a tie-out at period end: sub-ledger to GL, Operations totals to Accounting totals, report totals to the ledger.

**4. Batch processes and scheduled jobs.** Every job named in the BRDs and FRS is run and its effect checked:
- when it runs (Philippine time), rerunning safely and the lock against a double run;
- what happens on failure (alert, e-mail to the job-failure recipients), and catching up after downtime;
- month-end and year-end close rehearsed on test data.

**5. Documents, schedules and reports.** Every print, schedule and report named in the BRDs is generated with the
BDOI theme and checked against the source data:
- reports in Excel and PDF; documents and schedules in Word and PDF (item 17);
- upload and download templates tested from the templates folder (item 20).

**6. Requirement master list with results.** One workbook lists every requirement of all the BRDs, BRD-00 to BRD-13 (the RTM, item 21).
For each requirement it shows:
- the FR, the screen and the field;
- the test case, the result on screen, the evidence (screenshot) and the defect, if any;
- a readiness status.

Its summary is the UAT readiness statement per module and for the platform.

**7. Code standard report** (item 27), module by module and screen by screen. It shows:
- static analysis (SonarQube rules, PMD, SpotBugs, Checkstyle), duplication, code smells and coverage;
- dependency and licence scan;
- a hard-coding audit: no business rule, list or threshold held in code instead of configuration (item 16);
- consistent naming and error codes (item 35), so the production support team can trace a defect from the screen
  message to the code.

**8. Performance and volume** (item 28). BDOI is a volume business (retail and transactions), so the tests cover:
- load tests at the BRD volumes and peak periods (month end, renewal season, bulk uploads), with 429 concurrent
  sessions as the peak case until DCR-166 is answered (the register proposes sizing for 150 concurrent plus 20% a year);
- screen response times against the NFRs, batch run times, report generation under load;
- database growth and archiving.

**9. Security** (item 26):
- role and data-access tests (every endpoint against every role);
- OWASP Top 10 checks and a dynamic scan;
- password, lock-out and session rules;
- audit trail completeness;
- masking of non-production data;
- secrets handling;
- dependency vulnerabilities.

**10. Resilience and operations:**
- backup and restore test, and the disaster recovery switch-over (hosting appendix);
- Redis and Kafka outages handled without losing a transaction;
- monitoring and alerts (item 38), and the runbook (item 24).

**11. Also in scope** (added to the client's list):
- a data migration dry run with reconciliation of migrated balances (item 29): mock runs and the dress rehearsal of
  the Data Migration design, with the legacy clearing account at 0.00;
- browser and screen-size checks and the accessibility statement (item 40);
- concurrency (two users on the same record);
- e-mail and notification delivery;
- time-zone and holiday handling;
- a defect triage and retest cycle with severity rules;
- UAT entry and exit criteria and the sign-off forms (item 30).

**Exit.** Every requirement has been tested and passed, or has an agreed disposition with BDOI; no open Critical or High
defect remains. The readiness statement is then issued for BDOI to approve the move into UAT.

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
| 1 | FRS, one per BRD (BRD-5 in two volumes with a cover note) | v1.0 issued for BDOI review: `out/FRS/`, 16 Word files: the BRD-00 Core Replacement umbrella, BRD-1 to BRD-13 (BRD-13 Data Migration added) and the BRD-5 cover note. The Claims FRS numbers its requirements FR-CM-nnn (renumbered from FR-CL-nnn, which Collections keeps; DCR-188) |
| 2 | Discrepancy and clarification register | v1.1: `out/Registers/BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.1.xlsx`: 209 items (BRD-00 Core Replacement DCR-163 to DCR-188, BRD-13 Data Migration DCR-189 to DCR-209 added) and 410 questions (CRQ01-CRQ25, DMQ01-DMQ35, XQ12-XQ13 added) |
| 3 | Test plans, one per FRS volume | v1.0: `out/TestPlans/`, 13 workbooks and 13 Word summaries (BRD-1 to BRD-12); builder `src/testplans/build_test_plan.py`. The Claims plan uses case IDs keyed on FR-CM. Not yet written: BRD-13 Data Migration (and the cross-cutting FR-CR requirements of BRD-00) |
| 17 | Reports in Excel and PDF; documents and schedules in Word and PDF | Built in the platform (`ExportFormat.DOCX`, document renditions); see Developer Guide 6.1-6.2 |
| 41 | Business process deck | v1.0: `out/Decks/BIBS_Deck_BRD-00_Business_Process_AsIs_Envisioned_BestPractice_v1.0.pptx` |
| 29 | Data migration (BRD-13) | v1.0: Strategy and Approach (47 pages), Data Requirements Workbook (31 data objects, 27 extract layouts, 335 fields, 46 data-quality rules), Cutover Runbook and Task Plan (75 tasks, T-30 to T+30), Reconciliation Approach and Sign-off, in `out/Migration/`; source `src/migration/` |
| 20 | Upload and download templates | Migration extract templates: `out/Migration/templates/` (27 layouts and the control file). Platform upload and download templates follow after the build |
| 3 | Test plan BRD-13 Data Migration | v1.0: `out/TestPlans/BIBS_TestPlan_BRD-13_Data_Migration_v1.0.xlsx` and summary (138 cases) |

### Carried into FRS v1.1

The test plans record every FRS statement that cannot be tested as written in the FRS Findings sheet of each
workbook (about 100 findings). Besides those, v1.1 applies these corrections across all FRS documents:

- quote the platform's log-in, lock-out and no-access messages as built, and the codes of reused platform messages
  (bulk and attachment file checks, duplicate client, e-mail address, report parameters);
- write each BRD trace as a block list, one quoted reference per item, so that commas inside a reference do not split
  it (BRD-4 FR-CL-033, 084, 091-094; BRD-6 FR-RN-090, 103; the page references of BRD-11);
- BRD-5 Volume 1 gap G1 (no Word output) is closed: schedules and documents are produced in Word;
- BRD-2: the menu section is "Production Reconciliation" (the platform label is corrected with the alignment pass);
- BRD-5 Volume 1: Lists of Values and Access Requests are under Setup & Administration > Broking Setup.
- BRD-13: the seven test-plan findings (FR-DM-013, 021, 051, 061, 091, 110, 123); the design gains sub-layouts R04B, P01S,
  F01S and F01C and the `<LAYOUT>_<SOURCE>_...` file-name rule, and drops the demo account codes that clash with Accounting.
