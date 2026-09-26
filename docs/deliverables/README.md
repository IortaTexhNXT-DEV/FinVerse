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
| 4 | Bill of materials (infrastructure and technology), coding and quality standards | Word + Excel | A | `pom.xml`, `package.json`, deployment files, hosting appendix; the BDOI IER workbook v20 and its comparison with BIBS (item 42, section 6); the architecture option [`ARCHITECTURE_OPTION_DECISION.md`](../architecture/ARCHITECTURE_OPTION_DECISION.md) (BIBS modular monolith with the IER enterprise elements, awaiting BDOI confirmation) |
| 5 | Fit-gap per module | Excel | B | specs and traceability, updated after build |
| 6 | Menu, screens, fields and roles workbook for client concurrence | Excel | B | `navigation/modules.ts`, screens, permissions, roles |
| 7 | End-to-end deck by persona: actor, role, usage, expected outcome, observation, screenshots | PowerPoint | B | seed-data walkthrough and screenshots |
| 8 | Persona-based end-to-end test execution and results | Excel + Word | B | automated and manual runs |
| 9 | Professional BDO theme on every screen and document; plain, specific wording | all | A + B | UX guidelines, writing standard |
| 10 | Every function works: CRUD, upload and download (with templates), document prints, schedules, reports; output checked | test evidence | B | test runs and output samples |
| 11 | Data dictionary | Excel | B | database schema, entity Javadoc |
| 12 | Technical and deployment architecture, with diagrams | Word | A (refreshed in B) | code, deployment files, hosting appendix; the IER-aligned diagrams and Kubernetes sizing of item 42; [`ARCHITECTURE_OPTION_DECISION.md`](../architecture/ARCHITECTURE_OPTION_DECISION.md); deployment view in [`ARCHITECTURE.md`](../architecture/ARCHITECTURE.md) |
| 13 | Persona-based manual end-to-end test: where each persona gets stuck | Excel + findings report | B | manual run with data entry |
| 14 | Persona-based menus: each user sees only their screens | verified in the platform | B | permissions audit and tests |
| 15 | Notifications and workflows per role | test evidence | B | workflow and notification tests |
| 16 | Configuration over code: rules, validations and definitions maintained from front-end masters by the System Administrator | gap list and fixes | B | code review of hard-coded rules |
| 1, 3 | BRD-1 New Business business sign-off pack (release set v2.0) | Drop 1 | FRS v2.0 with screen specifications, sign-off workbook v2.0, test plan v2.0 and release note in `out/Drop-1_Transactional/BRD-01_New_Business/`; source `src/signoff/brd01/`. Screenshots are captured on the SIT environment before issue |
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
| 26 | Security and data-protection controls mapping (hosting appendix, masking of non-production data, access, audit; EIAM and UIDM-ISC, S3 encryption with BDOI keys, Object Lock and legal hold from item 42 and [`DOCUMENT_STORAGE_DECISION.md`](../architecture/DOCUMENT_STORAGE_DECISION.md)) | Word + Excel | A + B |
| 27 | Code quality report (static analysis, coverage, dependency and licence scan) | Excel | B |
| 28 | Performance and volume test plan and results (against the NFRs of each BRD; the peak case is 429 concurrent sessions, the sum of the user rows of the Core Replacement umbrella BRD p.42, until BDOI answers DCR-166 / CRQ21; the environments and Kubernetes sizing follow the IER workbook as corrected in item 42, section 6.4, and the test runs on Pre-Prod in Nov - Dec 2027) | Word + Excel | B |
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
| 42 | Programme alignment: BDOI drops, timeline, integrations and infrastructure (Word), with the integration inventory (Excel) and the IER-aligned architecture diagrams. Analysis [`PROGRAMME_ALIGNMENT.md`](../architecture/PROGRAMME_ALIGNMENT.md); source `src/alignment/` | Word + Excel + PNG | A |

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

## Release and sign-off per BRD

Each BRD is released to BDOI as one **release set** and signed off as one unit by its business unit. The release set
lives in one folder, `out/<drop folder>/BRD-nn_<Name>/`, and holds the same documents for every BRD:

| Document | Content | Built by |
|---|---|---|
| Release note (Word, 2-3 pages) | What the set contains, how to review it, the SIT review sessions, the dates for comments and sign-off, change control after sign-off | `src/signoff/RELEASE_NOTE_<BRD>.md` |
| FRS v2.0 (Word) | The requirements of v1.x unchanged, plus: navigation (menu by persona, screen-flow diagram); one specification per screen (purpose, who opens it, navigation, screenshots with numbered callouts, field table, actions table, business rules, expected outcome, FR and test links); end-to-end walkthroughs; messages catalogue; notifications; generated documents; upload templates; cross-BRD interface contract; sign-off and change control | `src/frs/` with ```pack blocks read from `src/signoff/<brd>/` |
| Sign-off workbook (Excel) | Screen catalogue, field register, actions, business rules, messages, notifications, menu by persona, upload templates, cross-BRD contract and the sign-off sheet, with the BU review columns (Accept / Change requested / Comment, comment, reviewer, date) | `src/signoff/signoff_pack.py` |
| Test plan v2.0 (Excel and Word summary) | The cases of v1.x re-traced to FRS v2.0 and its screens, plus screen cases (one per screen) and message cases | `src/testplans/build_test_plan.py` |

The screen, field, action, message and menu rows are generated from the system as built (menus and role grants,
server and screen messages, upload templates) through `tools/deliverables/code_facts.py`, and a check refuses a screen
label, button, route, FR or test link that the code or the FRS does not have. Screenshots are taken on the SIT
environment with seed data by `tools/screenshots/capture_pack.cjs` from the manifest of the set.

**Proposed release order.** One BRD at a time, so each business unit reviews a complete set: New Business first as the
pattern for the others, then the BRDs it depends on and the ones that depend on it.

| Wave | BRD release sets | Business owner | Proposed window |
|---|---|---|---|
| 1 | BRD-1 New Business | Marketing, TSU, Processing | Review Oct 2026, sign-off by 30-Oct-2026 |
| 2 | BRD-3 Product Maintenance, BRD-11 User Access Maintenance | Product owner, user access administration | Nov 2026 |
| 3 | BRD-10 Sanction Screening, BRD-12 Submitted Policies, BRD-6 Renewal, BRD-9 Customer Servicing Facility | Compliance, Marketing, Processing | Nov 2026 |
| 4 | BRD-2 Operations, BRD-4 Collections, BRD-5 Accounting, Disbursement and ACSL | Operations, Collections, Comptrollership | Nov - Dec 2026 |
| 5 | BRD-13 Data Migration (after the objects of waves 1-4 are frozen) | Migration working group | Dec 2026 |
| 6 | BRD-7 Claims, BRD-8 Employee Benefits (Drop 2) | Claims, EB | Dec 2026 - Feb 2027 |

**Design freeze and change control.** Signing a release set freezes, for that BRD, its screens, fields, navigation,
messages, notifications and interface contract as specified. A later change is raised in the Change Management Register
(`out/Programme/Change_Management/`), assessed for its effect on the other BRDs through the cross-BRD contract of the
set, approved by the owners of every BRD it touches, and delivered as a new version of the release set (v2.1, v2.2...)
with its own release note. Nothing in a signed set changes without such a request.

**The as-built status rule stays.** The sets describe the system as built on their "Status as of" date. Where the build
differs from the BRD, the FRS says so in "Built behaviour that differs from the BRD" and the difference is resolved
through the register or a change request, not by editing the text. The final as-built refresh (below) re-issues every
set at build completion.

## Document status and the final as-built refresh

Every status marker in the documents reflects the date shown on the document and is **provisional until the platform
is complete**: Built, Built with parked item, Designed, Not built; the fit classes (FIT, CONFIGURE, CHANGE, NEW, OUT);
the "as built" sections of the designs; the automation references of the test plans; and every count. Each document's
cover or document-control table carries "Status as of <date>".

When the end-to-end build is finished, one **final as-built refresh** brings every document to the real state of the
platform: the specs, designs, FRS, test plans, register, alignment pack, migration pack, module guides and the RTM. Each
document's version is bumped and the superseded versions are deleted from `out/`, so only current documents remain and
nobody refers to an old one. The refresh is step 12 of the UAT readiness programme below and comes before the readiness
statement.

## UAT readiness programme (after the end-to-end build)

BDOI dates: UAT readiness statement for Drop 1 (end to end) by **30-Jul-2027** (UAT Aug - Dec 2027), for Drop 2 by
**30-Sep-2027** (UAT Oct - Nov 2027); UAT plans and sign-off forms (item 30) by 16-Jul-2027; UAT runs on migrated, masked
data from Mock 3 (Aug 2027). Source: item 42, section 4.

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

**12. Final as-built refresh of the document set** (section "Document status and the final as-built refresh"). Every
spec, design, FRS, test plan, register, alignment pack, migration pack, module guide and the RTM is updated to the
platform as built, re-issued with a new version and "Status as of" date, and the superseded versions are deleted. The
readiness statement cites only these versions.

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
page previews). Sources: `docs/deliverables/src/`; outputs: one release-set folder per BRD,
`docs/deliverables/out/<drop folder>/BRD-nn_<Name>/`, and programme-level items by kind under `out/Programme/<kind>/`;
files are named `BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>`.

### Deliverables by drop

BDOI keeps the BRDs, FRS, test plans and other collaterals grouped under its drops (answer A5 of 26-Sep-2026; drop plan
[`BDOI_DROP_PLAN.md`](../source-documents/BDOI_DROP_PLAN.md); map in
[`PROGRAMME_ALIGNMENT.md`](../architecture/PROGRAMME_ALIGNMENT.md) section 2.3). Inside a drop folder each BRD has its
release-set folder `BRD-nn_<Name>/` (section "Release and sign-off per BRD"). Each drop folder has an index `README.md`
that lists every document with its BRD, version and the BDOI dates of the drop.

| Drop folder | BRDs (primary drop) | BDOI dates | Index |
|---|---|---|---|
| `out/Drop-0_Setup_and_Data_Migration/` | BRD-3 Product Maintenance, BRD-11 User Access Maintenance, BRD-13 Data Migration (FRS, test plan, migration pack and templates) | Setup with the Drop 1 requirements; migration requirements Sep - Nov 2026, build Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, cut-over Nov 2027 - Jan 2028 | [README](out/Drop-0_Setup_and_Data_Migration/README.md) |
| `out/Drop-1_Transactional/` | BRD-1, BRD-2, BRD-4, BRD-5 (three documents), BRD-6, BRD-9, BRD-10 (proposed), BRD-12 | Requirements Sep - Nov 2026, build Nov 2026 - Feb 2027, SIT Jan - Jul 2027, UAT Aug - Dec 2027 | [README](out/Drop-1_Transactional/README.md) |
| `out/Drop-2_Independent/` | BRD-7 Claims, BRD-8 Employee Benefits | Requirements Dec 2026 - Feb 2027, build Mar - Apr 2027, SIT Jul - Sep 2027, UAT Oct - Nov 2027 | [README](out/Drop-2_Independent/README.md) |
| `out/Programme/` | BRD-00: umbrella FRS, register, process deck, alignment pack and IER diagrams; UAT readiness | Performance and penetration test Nov - Dec 2027, ORR / PRR Dec 2027 - Jan 2028, go-live January 2028 | [README](out/Programme/README.md) |

A BRD that spans drops lives in its primary drop, and the index of the other drop points to it (no copies): BRD-2
Production Reconciliation and BRD-4 Marketing Collection extraction (Drop 2), BRD-3 quotation and BRD-8 EB placement and
reports (Drop 1), BRD-1 client onboarding and BRD-5 GL accounts (Drop 0), BRD-13 legacy invoices (Drop 1), BRD-9 service
requests and BRD-10 Bridger Insight (Drop 2). The builders take the folder from the drop map in
`tools/deliverables/brand.py` (`BRD_DROP`, `BRD_NAMES`), so a rebuild lands in the BRD's release-set folder; `python
tools/deliverables/drop_index.py` regenerates the four indexes.

### Status

| # | Document | Drop | Status |
|---|---|---|---|
| 1 | FRS, one per BRD (BRD-5 in two volumes with a cover note) | Drop of each BRD (BRD-00: Programme) | v1.0 issued for BDOI review, BRD-2, BRD-6, BRD-11 and BRD-13 at v1.1 (programme alignment and BDOI answers of 26-Sep-2026): the release-set folder of each BRD, 16 Word files: the BRD-00 Core Replacement umbrella, BRD-1 to BRD-13 (BRD-13 Data Migration added) and the BRD-5 cover note. The Claims FRS numbers its requirements FR-CM-nnn (renumbered from FR-CL-nnn, which Collections keeps; DCR-188) |
| 2 | Discrepancy and clarification register | Programme | v1.2: `out/Programme/Registers/BIBS_Register_BRD-00_Discrepancies_and_Clarifications_v1.2.xlsx`: 239 items (programme alignment DCR-210 to DCR-235 and Data Migration v1.1 DCR-240 to DCR-243 added; DCR-236 to DCR-239 not used; DCR-135 answered by the IER) with a Drop column, and 453 questions (IQ01-IQ35 of the programme alignment and DSQ01-DSQ04 of the document storage decision added; DMQ25-DMQ26, DMQ36-DMQ39 answered) |
| 3 | Test plans, one per FRS volume | Drop of each BRD | v1.0 (BRD-6 and BRD-13 at v1.1): the release-set folder of each BRD, 14 workbooks and 14 Word summaries (BRD-1 to BRD-13, BRD-5 in two volumes); builder `src/testplans/build_test_plan.py`. The Claims plan uses case IDs keyed on FR-CM. Not yet written: the cross-cutting FR-CR requirements of BRD-00 |
| 3 | Test plan BRD-13 Data Migration | Drop 0 | v1.1: `out/Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/BIBS_TestPlan_BRD-13_Data_Migration_v1.1.xlsx` and summary (152 cases) |
| 17 | Reports in Excel and PDF; documents and schedules in Word and PDF | All (platform) | Built in the platform (`ExportFormat.DOCX`, document renditions); see Developer Guide 6.1-6.2 |
| 20 | Upload and download templates | Drop 0 (migration templates) | Migration extract templates: `out/Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/templates/` (27 layouts and the control file). Platform upload and download templates follow after the build |
| 29 | Data migration (BRD-13) | Drop 0 | v1.1 (BDOI timeline, go-live January 2028): Strategy and Approach (52 pages), Data Requirements Workbook (31 data objects, 27 extract layouts, 352 fields, 51 data-quality rules), Cutover Runbook and Task Plan (75 tasks, T-30 to T+30), Reconciliation Approach and Sign-off, in `out/Drop-0_Setup_and_Data_Migration/BRD-13_Data_Migration/`; FRS BRD-13 v1.1 (80 pages); source `src/migration/` |
| 41 | Business process deck | Programme | v1.0: `out/Programme/Decks/BIBS_Deck_BRD-00_Business_Process_AsIs_Envisioned_BestPractice_v1.0.pptx` |
| 42 | Programme alignment and integration inventory | Programme | v1.0: `out/Programme/Alignment/BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v1.0.docx`, `BIBS_Alignment_BRD-00_Integration_Inventory_v1.0.xlsx` (20 integrations) and the IER diagrams in `out/Programme/Alignment/IER/`; source `src/alignment/` |

Edits still owed to documents that the build teams are editing: [`PENDING_EDITS.md`](PENDING_EDITS.md).

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
  F01S and F01C and the `<LAYOUT>_<SOURCE>_...` file-name rule, and drops the seed account codes that clash with Accounting.
