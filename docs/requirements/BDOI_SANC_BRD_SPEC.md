# BDOI Sanction Screening and Risk Profiling (BRD-10) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI / BDOIR), Philippines. Platform: iNXT BrokerVerse (BIBS - BDOI Broker System).

Status: **for BDOI concurrence.** The review workbook will be consolidated with the other BRDs. Build design: [`SANCTION_SCREENING_DESIGN.md`](../architecture/SANCTION_SCREENING_DESIGN.md).

File references in this document are relative to `backend/src/main/java/com/iortatechnxt/brokerverse/` (backend) and `frontend/src/` (frontend), unless they start with `docs/` or `backend/src/main/resources/`.

## 1. Source documents

Source: `docs/source-documents/Sanction Screening and Risk Profiling BRD.pdf` (29 pages, text layer on pp.1-27; pp.28-29 are scans; the process diagrams on pp.6-8 were read as images).

| Pages | Content |
|---|---|
| 1 | Cover: "Sanction Screening and Risk Profiling Business Requirements Document (BRD) Template", prepared by ESG-BPS |
| 2-5 | I. Executive summary; II. Business objective: compliance / regulatory goal (RA 9160 AMLA, BSP Circular 1182 s.2023, BSP CL-2023-030, BSP M-2025-017 on adverse media), benefits, penalty table (AMLA, AMLC, IC CL 2019-65), benefit table with quantified effort savings |
| 6-7 | III. Current process: onboarding name screening, regular name screening, PEP name screening (swimlanes Compliance Officer, Investigator, Unit Head, Compliance, AML Committee; Marketing Account Officer, Compliance Coordinator, NLDS validator, Team / Unit Head); reports and notifications of today |
| 8 | IV. Envisioned journey: to-be swimlanes for onboarding and regular screening (Compliance Officer, System, Investigator, Approver, Compliance, AML Committee; "File STR to AMLC" stays "Via Portal"); nine key capabilities; reports and notifications to be produced |
| 9 | V. Business key capabilities 1-9 |
| 10-21 | Table of business requirements: BRID SNSRP-101 to SNSRP-903 (37 IDs), all "Must have" |
| 22-24 | Usage requirements: users, volumes, growth and response time per role and process; peak demand |
| 24-25 | Availability; data retention and archiving; anonymisation |
| 26-29 | Approval sheet: prepared 10-Apr-2026 (ESG-BPS), input 13-16 Apr 2026, reviewed 10-12 Apr 2026, approved 16-17 Apr 2026 (Product Owner; Unit Heads Claims and Risk Management, Combank and Corbank; Heads of Retail Marketing). BDOI CCO signed as BU representative; Unit Head Processing "for regularization - on leave" |

## 2. Business context

BDOIR screens clients by hand today. The AML unit sends sanctioned names by e-mail advisory. Compliance consolidates them and publishes them on SharePoint. Investigators then match the names against new and existing clients in BDOI systems and tag the client risk profile. They also fill review templates (KYC, transaction), send them by e-mail for review and approval, escalate to the BU and the AML Committee, and file Suspicious Transaction Reports (STR) on the AMLC portal. PEP lists are prepared by Marketing Account Officers every quarter. A Compliance Coordinator sends them for Negative List Database System (NLDS) validation, which only one person can access. There is no Enhanced Due Diligence (EDD) process for PEPs (p.7).

The BRD asks for one in-system workflow:
- configurable criteria, templates, matrices and lists of values;
- a sanctions / PEP list that is updated regularly;
- automated matching of the list against BDOI records, automatic risk tagging and case creation;
- case work: review templates, KYC uploads, dispositions and routing to approvers;
- escalation to Compliance and the AML Committee;
- STR preparation and extraction in the AMLC format;
- notifications, SLA monitoring, reports and immutable audit logs.

The expected gains are 64 hours a month for Compliance, 10 minutes per case for name screening, and an end-to-end cycle cut from 4 hours to 1 hour (pp.3-5).

BrokerVerse has the building blocks. The client master has KYC documents, a risk rating (`KYC_RISK_RATING`: LOW / STANDARD / HIGH) and the tags `PEP` and `WATCHLIST_REVIEW`. It also has an exact-key duplicate check, KYC review cycles by risk rating, workflow cases with SLA, queues and assignment, maker-checker, the approval inbox, notifications, attachments, reports and an insert-only audit trail. It has **no watchlist, no name-matching engine, no screening case, no review-template engine and no STR**. Sanction screening is therefore mostly new, built as one new module (`screening`) on those platform services.

**Scope boundary.**
- Filing the STR on the AMLC portal stays manual: the to-be diagram keeps "File STR to AMLC" in a "Via Portal" lane outside the in-system workflow (p.8). BIBS prepares, approves and extracts the STR, and records the filing reference.
- The BRD does not ask to block quotations, placement or booking on a hit. Any blocking gate is a proposal, off by default (SQ07).

## 3. Fit/gap summary

### As built (wave S2)

All build waves (S0, S1-A, S1-B, S1-C) are merged; the last column of the section 5 tables gives
the status of each row and where it is built. Paths are under
`backend/src/main/java/com/iortatechnxt/brokerverse/screening/` unless stated; the design sections
16 to 19 describe each wave and the module guide is
[`docs/modules/SANCTION_SCREENING.md`](../modules/SANCTION_SCREENING.md).

| Status | Meaning | Rows |
|---|---|---|
| Built | Works as the acceptance criteria say | 19 |
| Built with parked item | Works; one element waits for BDOI (question in brackets) or is a named follow-up | 18 |
| Not built | - | 0 |
| **Total** | | **37** |

The parked elements are the production values and lists (SQ02, SQ03, SQ05, SQ06, SQ08, SQ15), the
AMLC STR format and reasons (SQ09), the list transports and recipients (SQ01), the active-policy
definition (SQ10), and four follow-ups: working-hour SLA calendars, accent-insensitive case search,
a guard against deleting case documents through the generic attachment API, and asynchronous
screening after commit. End-to-end evidence: `api/ScreeningEndToEndApiIT` (registration to the
filed STR through the HTTP API, SLA reminder and breach) and `security/PersonaMenusIT` with
`frontend/src/navigation/personaMenus.test.ts` (menus per role).

The fit columns below are the analysis before the build and are kept for traceability.

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 1 |
| CONFIGURE | Set-up only | 2 |
| CHANGE | Extend an existing capability | 20 |
| NEW | New build | 14 |
| OUT | Out of scope per BRD | 0 |
| **Total** | | **37** |

### Rows per capability and fit

| Cap. | Name (p.9) | Rows | FIT | CONFIGURE | CHANGE | NEW | Size S/M/L |
|---|---|---|---|---|---|---|---|
| 1 | In-system criteria, templates, matrix and LOV configuration | 9 | 0 | 1 | 5 | 3 | 2/7/0 |
| 2 | List of sanctioned names regularly updated | 4 | 0 | 0 | 1 | 3 | 2/1/1 |
| 3 | Automated matching, risk tagging and case creation | 4 | 0 | 0 | 2 | 2 | 1/1/2 |
| 4 | View / extract high-risk clients and cases (case management) | 5 | 0 | 0 | 5 | 0 | 4/1/0 |
| 5 | Review templates accomplished in the system | 2 | 0 | 0 | 1 | 1 | 1/1/0 |
| 6 | KYC documents uploaded; matching triggers | 2 | 0 | 0 | 1 | 1 | 1/1/0 |
| 7 | In-system routing for review and approval; STR | 6 | 0 | 0 | 2 | 4 | 3/3/0 |
| 8 | System-generated notifications | 2 | 0 | 1 | 1 | 0 | 2/0/0 |
| 9 | Reports on demand and audit | 3 | 1 | 0 | 2 | 0 | 2/1/0 |
| | **Total** | **37** | **1** | **2** | **20** | **14** | **18/16/3** |

SNSRP-602 carries capability number 6 in the table but sits between SNSRP-304 and SNSRP-401 and describes a matching trigger (capability 3). It is counted under 6 as printed.

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Client master (`crm`) | Extend | Screening writes the risk rating and the `PEP` / `WATCHLIST_REVIEW` tags through a new crm service method. crm publishes a `ClientRegistered` event. The risk rating keeps driving the KYC review cycle (`crm/service/KycReviewPolicy.java`) |
| Workflow (`workflow`) | Re-use + extend | Workflow `SCR_CASE`, with stages, owners and return reasons. Assignment and re-assignment need a reason. The configurable SLA matrix overrides the seeded `wf_stage.sla_hours`, and a reminder fires before the breach |
| Maker-checker (`common.domain.AuthorizableEntity`, `approval`) | Re-use | Configuration versions and watchlist changes are drafts approved by a Compliance Checker; they appear in My Approvals |
| Lists of values (`lov`) | Configure | Dispositions per case stage (`parentCode` = stage), case types, list types, STR reasons |
| Attachments (`attachment`) | Extend | KYC and evidence uploads on the case, with the BRD naming convention `<Form Type>_<Client Name>_<Date Received>_<Document Type>_<sequence>` |
| Bulk (`bulk`) | Re-use | Watchlist file intake handler (fallback transport of the list feed) |
| Jobs / alerts (`system`, `alert`) | Re-use | Ingestion, periodic screening and SLA monitor jobs. The alerts `SCR_INGEST_FAILED` and `SCR_SLA_BREACH` |
| Messaging (`messaging`) | Re-use | New-case, return, reminder and escalation notices. E-mail report of failed ingestion rows |
| Reports (`report`) | Extend | New category **Compliance**; 8 reports |
| Audit (`audit`) | Re-use | Insert-only `audit_log` (V26); a case timeline and a configuration diff table keep the structured before / after values |
| Security | Extend | 14 new permissions; 6 new roles |
| Accounting | None | Screening posts no journal and creates no open item |

### Target modules

| Module | Rows (primary) | Also touched (secondary) |
|---|---|---|
| `screening` (new) | 36 | - |
| `platform (audit)` | 1 (SNSRP-902) | - |
| `crm` (extension) | - | SNSRP-302, 304, 602 |
| `workflow` (extension) | - | SNSRP-108, 404 |
| `attachment` (extension) | - | SNSRP-601 |

## 4. Screening flow (for concurrence)

| Step | Owner | What happens | BRD |
|---|---|---|---|
| Configure | Compliance Officer (maker), Compliance Checker | Matching criteria and thresholds, risk categories and rules, approval matrix, assignment matrix, dispositions per stage, SLA matrix, review and STR templates. Each change is a versioned draft, active on approval with an effective date | SNSRP-101-109 |
| List intake | System, Compliance Officer | Sanctions and NLDS-PEP lists come in by feed or file on a schedule, and every run is logged. Rows that fail are reported to recipients. Manual additions and changes go through maker-checker | SNSRP-201-204 |
| Screening | System | Triggers: a new client is created, an account is applied for, the scheduled batch window, and a list update. Name matching runs with exact, phonetic and fuzzy rules, and each match result is recorded | SNSRP-301, 602 |
| Risk tagging | System, Investigator | Risk rules tag the client (risk rating and tags). The Investigator can override a tag with justification and evidence | SNSRP-302, 304 |
| Case creation | System | A case opens for a high-risk tag, a PEP or an account application. Only clients with an active policy need KYC Review / EDD. For clients without an active policy, the Unit Compliance Coordinator (UCC) and the Investigator are notified | SNSRP-303 |
| Investigation | Investigator | Fills the guided KYC / transaction review template, uploads documents, selects a disposition and submits. The case becomes read-only to them | SNSRP-501, 502, 601 |
| Validation and approval | System, Approver (Unit Head) | The case is checked against configured criteria, then routed by the approval matrix. The Approver approves, or disapproves with a rationale and returns it | SNSRP-701, 702 |
| BU escalation | Compliance Officer | Reviews the BU escalation. Routes it by the escalation matrix (e.g. to the AML Committee) or returns it for rework | SNSRP-703 |
| Committee | AML Committee member | Views the full case and records an online decision. The case follows the governance workflow | SNSRP-704 |
| STR | Compliance Officer | STR prefilled from the case, completeness check. The Committee-approved STR list is extracted in the AMLC format and saved to the designated folder. Filing on the AMLC portal stays manual | SNSRP-705, 706 |
| Monitoring | UCC, Operations Lead, Compliance | Case lists and search, re-assignment, SLA reminders and escalation, notifications, reports, audit log report | SNSRP-401-405, 801-802, 901-903 |

## 5. Requirements and fit/gap

Persona abbreviations: CO = Compliance Officer; CC = Compliance Checker; UCC = Unit Compliance (User) Coordinator; INV = Investigator; APR = Approver; AMLC-M = AML Committee Member; SYS = System. Every row is "Must have" in the BRD.

### Capability 1. In-system criteria, templates, matrix and LOV configuration

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-101<br><sub>p.10</sub> | 1 | CO | Configure name-matching criteria (exact / phonetic / fuzzy thresholds) so screening quality is controlled | Saving versions the change with user, timestamp and before / after values. A checker approves a Draft, which then becomes Active with an effective date | **NEW** | Only exact normalised keys for duplicates (`crm/service/DuplicateCheckService.java`, `crm/domain/DuplicateKeys.java`); no configurable matching | Versioned configuration `scr_config_version` (type MATCH_CRITERIA) with rows `scr_match_rule`: algorithm (EXACT, PHONETIC Double Metaphone, FUZZY Jaro-Winkler), threshold, fields compared (name, alias, birth date, nationality, ID), list types. Before / after diff kept on the version. Maker-checker per SNSRP-109 | `screening` | M | SQ02 | **Built**: config/service/ConfigVersionService.java, MatchCriteria.java; V1051; screen `/screening-setup/config` |
| SNSRP-102<br><sub>p.10</sub> | 1 | CO | Define risk profile categories so clients can be auto risk tagged | When the rules are activated, matched entities get the correct risk tag, and audit records are created | **CHANGE** | Risk rating LOV `KYC_RISK_RATING` (LOW / STANDARD / HIGH, `backend/src/main/resources/db/migration/V801__crm_onboarding_kyc_and_notes.sql`) and client tags `PEP`, `WATCHLIST_REVIEW`; rating drives the KYC review cycle (`crm/service/KycReviewPolicy.java`) | Risk categories `scr_risk_category` (tier, maps to a `KYC_RISK_RATING` code and optional client tags, case type, "requires EDD") and rules `scr_risk_rule` (condition on match list type / status, PEP, nationality, occupation, source of funds, client type; priority). Versioned like SNSRP-101. Categories may add values to `KYC_RISK_RATING` through the LOV screen | `screening` | M | SQ03 | **Built with parked item**: config/service/RiskRules.java, risk/service/RiskProfiler.java; categories are demo values (SQ03) |
| SNSRP-103<br><sub>p.10</sub> | 1 | CO | Maintain an Approval Matrix so cases route to the right approvers | Once the matrix is published, a submitted case routes to the intended reviewer / approver by rule. Every routing event is logged | **CHANGE** | Workflow transitions with owner permissions and queues (`workflow/service/WorkflowService.java`, `WorkAssignmentService.java`); no rule-based choice of approver | `scr_approval_route` rows (case type, risk category, marketing unit, disposition -> approver stage, approver user or role, order). The router picks the approver when the case is submitted and records it in the case timeline (`scr_case_event`) | `screening` | M | SQ04 | **Built**: config/service/ApprovalMatrix.java, cases/service/CaseRouter.java |
| SNSRP-104<br><sub>p.10-11</sub> | 1 | CO | Maintain review templates so a standard format is followed across reviewers | Templates are saved with version, user and timestamp. An update applies to new reviews only | **NEW** | No dynamic form or template engine (docgen templates are output documents: `backend/src/main/resources/db/migration/V754__document_templates.sql`) | `scr_template` (code, type KYC_REVIEW / TRANSACTION_REVIEW / EDD / STR, version, status) + `scr_template_field` (section, code, label, data type, mandatory, LOV type, help, order). A review stores the template version it was started with | `screening` | M | SQ05 | **Built with parked item**: config/service/ReviewTemplate.java, cases/service/CaseReviewService.java; screen `/screening-setup/templates`; field lists SQ05 |
| SNSRP-105<br><sub>p.11</sub> | 1 | CO | Maintain STR reporting templates so submissions are consistent, compliant and easy to maintain when formats change | Templates are saved with version, user and timestamp. An update applies to new STRs only | **NEW** | Document templates exist for letters only (`docgen`) | STR template = `scr_template` type STR (fields and mapping to case data) plus an extraction layout (`scr_str_layout`: column order, codes, format) versioned the same way. The AMLC layout itself is parked (SQ09) | `screening` | M | SQ09 | **Built with parked item**: config/service/StrLayout.java, str/service/StrFileWriter.java; AMLC layout SQ09 |
| SNSRP-106<br><sub>p.11</sub> | 1 | CO | Configure a case assignment matrix by scenario so cases route automatically to the right team or user | A case is assigned by the configured scenario when it is created. When the rules change, existing assignments stay and new cases follow the new rules | **CHANGE** | Team queues by stage owner permission, claim and assign (`workflow/service/WorkAssignmentService.java`) | `scr_assignment_rule` (scenario = case type, trigger, risk category, marketing unit, client type -> team role or user, load balancing ROUND_ROBIN / LEAST_OPEN). It is evaluated only when the case is created | `screening` | M | SQ04 | **Built**: config/service/AssignmentMatrix.java, cases/service/CaseAssigner.java |
| SNSRP-107<br><sub>p.11-12</sub> | 1 | CO | Configure a disposition list of values per case stage so only valid options are available in each stage | Only the dispositions configured for a stage can be selected in that stage | **CONFIGURE** | Generic LOV with maker-checker, effective dates and a parent code (`lov/domain/LovValue.java`, `lov/service/LovService.java`) | LOV type `SCR_DISPOSITION` with `parentCode` = case stage. Seeds are to be confirmed (SQ06). The case screen filters by the current stage | `screening` (LOV seed) | S | SQ06 | **Built with parked item**: LOV `SCR_DISPOSITION` (V1050), cases/service/CaseValidator.java; values to confirm SQ06 |
| SNSRP-108<br><sub>p.12</sub> | 1 | CO | Configure an SLA matrix per case stage so turnaround times are defined, monitored and enforced | The matrix is saved with thresholds, escalation rules and effective dates and applied by stage. A breach is flagged and escalated or alerted. SLA changes are versioned with before / after values | **CHANGE** | `wf_stage.sla_hours` (seeded, static) sets `WorkCase.dueAt`. The daily `WorkSlaAlertCheck` raises overdue alerts (`workflow/service/WorkSlaAlertCheck.java`) | `scr_sla_rule` (stage, case type, risk category, SLA hours, reminder lead hours, escalation target role, effective from) in the versioned configuration. The screening SLA service sets the case due time and overrides the workflow due time through the new `WorkflowService.overrideDue(caseId, dueAt)` | `screening` + `workflow` | M | SQ08 | **Built with parked item**: config/service/SlaMatrix.java, cases/service/CaseSla.java; calendar hours, values SQ08 |
| SNSRP-109<br><sub>p.12</sub> | 1 | CC | Approve / reject a drafted configuration so four-eyes control is enforced | Approval makes the configuration Active. Rejection records the reason | **CHANGE** | Maker-checker pattern and the universal inbox (`common.domain.AuthorizableEntity`, `approval/service/MasterRecordApprovals.java`, `approval/service/PendingApprovalSource.java`) | `scr_config_version` status DRAFT -> PENDING -> ACTIVE / REJECTED (reason mandatory). The previous version becomes SUPERSEDED on its effective date. A `ScreeningApprovalSource` adds the item to My Approvals. The maker cannot approve | `screening` | S |  | **Built**: config/service/ConfigDecisionService.java, ConfigApprovalSource.java |

### Capability 2. List of sanctioned names regularly updated

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-201<br><sub>p.12</sub> | 2 | SYS | Receive the sanctions / NLDS-PEP list in real time or on an agreed schedule from the source(s), so screening data stays current | When ingestion runs on the configured schedule, the result is logged: success / failure, volumes, reason for failure, source | **NEW** | Job framework with run history (`system/service/ManagedJob.java`, `JobRunService.java`); bulk file intake (`bulk`) | Watchlist sources `scr_watchlist_source` (code, list type SANCTION / PEP / INTERNAL / ADVERSE_MEDIA, transport FILE / API / MANUAL, cron). Port `WatchlistFeed` with a default file-drop / upload adapter (bulk handler `SCR_WATCHLIST`). Job `SCR_WATCHLIST_INGEST`. Run log `scr_ingestion_run` (source, trigger, received, added, updated, delisted, failed, status, error) + `scr_ingestion_error`. Real-time API sources are parked (SQ01) | `screening` | L | SQ01 | **Built with parked item**: watchlist/service/WatchlistIngestionService.java, WatchlistIngestJob.java, port WatchlistFeed (StagedFileWatchlistFeed); transports SQ01 |
| SNSRP-202<br><sub>p.13</sub> | 2 | CO | Know the records that failed ingestion into the core system | For each record that failed ingestion, an alert (it may be a report) goes to the identified recipients on an agreed schedule. It lists the failed records and where they came from | **NEW** | Alert framework (`alert/service/AlertService.java`); e-mail dispatch (`messaging/service/MailDispatcher.java`); report archive | Alert `SCR_INGEST_FAILED` per run, and report `SCR-INGEST-ERRORS` (source, run, line, reason). It is e-mailed on the agreed schedule (job `SCR_INGEST_ERROR_DIGEST`) to the recipients in parameter `SCR_INGEST_ALERT_RECIPIENTS` | `screening` | S | SQ01 | **Built with parked item**: watchlist/service/IngestErrorDigestJob.java, alert SCR_INGEST_FAILED; recipients SQ01 |
| SNSRP-203<br><sub>p.13</sub> | 2 | CO | Manually add, update and edit sanctioned-name and PEP records so regulatory updates, internal findings or urgent risk actions are captured | With compliance maker permission, an added or changed record is saved as Draft / Pending Approval and does not affect active screening. Mandatory fields are validated. Before / after values, maker user ID, timestamp and remarks are captured | **NEW** | None | `scr_watchlist_entry` (source, list type, entity type, names, aliases `scr_watchlist_alias`, birth date, nationality, ID numbers, listing / delisting dates, remarks) as an `AuthorizableEntity`. A pending change is stored as a change record `scr_watchlist_change` (before / after); screening reads ACTIVE rows only | `screening` | M |  | **Built**: watchlist/service/WatchlistService.java; screen `/screening-setup/watchlist` |
| SNSRP-204<br><sub>p.13</sub> | 2 | CC | Review, approve or reject sanctioned-name / PEP changes submitted by a maker so list updates are governed and auditable | The checker sees the full details, before / after values and maker remarks. Approval makes the record Active with an effective date, available for screening. Rejection leaves the record unchanged and logs the remarks. Checker user ID and timestamp are recorded | **CHANGE** | Maker-checker and inbox (`approval/service/MasterRecordApprovals.java`) | Approve / reject on `scr_watchlist_change`. Approval applies the change to the entry and triggers a delta screening of the entry (SNSRP-301). Source `ScreeningApprovalSource` | `screening` | S |  | **Built**: watchlist/service/WatchlistDecisionService.java, WatchlistApprovalSource.java |

### Capability 3. Automated matching, risk tagging and case creation

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-301<br><sub>p.14</sub> | 3 | SYS | Match sanction names by configurable criteria so true matches are identified | When a candidate pair meets the criteria, a match result is recorded | **NEW** | Exact duplicate keys only (`crm/domain/DuplicateKeys.java`) | Matching engine: name normalisation, token and phonetic blocking keys (`scr_name_key` for clients and list entries), then scoring (exact / Double Metaphone / Jaro-Winkler) with the active thresholds. Results go to `scr_screening_run` and `scr_match` (client, entry, score, algorithm, matched fields, status POTENTIAL / TRUE_MATCH / FALSE_POSITIVE). Pure-function scorer with JUnit tests | `screening` | L | SQ02 | **Built with parked item**: matching/service/NameNormaliser.java, NameKeys.java, NameScorer.java, PairMatcher.java, ScreeningEngine.java; weights SQ02 |
| SNSRP-302<br><sub>p.14</sub> | 3 | SYS | Auto tag the client risk profile by rule so identified risk profiles trigger reviews | Given a risk-profiling category rule, a qualifying match tags the client automatically | **CHANGE** | Client risk rating and tags exist; set manually only (`crm/domain/Client.java` `riskRating`, `crm/service/ClientNotesService.java`) | Rule evaluation after each match or run. Screening calls the new `crm.service.ClientRiskService.applyRiskProfile(clientId, rating, tags, source, reason)`, which writes the rating and tags and audits them. The history goes to `scr_client_risk_profile`. The new rating re-schedules the KYC review (`KycReviewPolicy`) | `screening` + `crm` | M | SQ03 | **Built**: risk/service/RiskRuleEvaluator.java, RiskProfiler.java; crm/service/ClientRiskService.java |
| SNSRP-303<br><sub>p.14</sub> | 3 | SYS | Automatically create cases for PEP and / or high-risk clients so reviews begin promptly | A High-risk tag opens a new case, assigned by the routing rules. Only clients with an active policy need KYC Review / EDD. For clients without an active policy, the UCC and the Investigator are notified. A case is triggered automatically when the client applies for an account | **NEW** | Workflow cases (`workflow/service/WorkflowService.java`); account submission publishes `WorkCaseTransitioned` | `ScreeningCaseService.open(...)`: case `scr_case` (SCR-yyyy-nnnnnn, client, trigger, case type, risk category, active-policy flag, marketing unit) + workflow `SCR_CASE`. The active policy comes from the port `ActivePolicyQuery` (default: client records of kind Account in POLICY_ISSUED / BOOKED; SQ10). Without an active policy the case type is MONITOR and a notice goes to the UCC and the Investigator. It listens to the account workflow submit transition for the "applied for an account" trigger | `screening` | L | SQ10, SQ11 | **Built with parked item**: cases/service/CaseTriggers.java, CaseOpeningService.java; active policy by port ActivePolicyQuery (SQ10) |
| SNSRP-304<br><sub>p.14</sub> | 3 | INV | Manually update the risk-profile tagging with justification | For a validated false positive, the Investigator updates the tagging and attaches evidence. The tagging and evidence are saved and take effect at once | **CHANGE** | Risk rating editable through client update, no justification or evidence (`crm/service/ClientService.java`) | "Update risk tag" action on the case / client (permission SCR_RISK_TAG): new rating and tags, mandatory justification and at least one evidence attachment. The match is set to FALSE_POSITIVE and the client is suppressed for that list entry until the entry changes. Written through `ClientRiskService`; history in `scr_client_risk_profile` (source MANUAL) | `screening` + `crm` | S | SQ12 | **Built**: risk/service/RiskOverrideService.java, matching/service/MatchDecisionService.java, cases/service/CaseMatchService.java |

### Capability 4. View / extract high-risk clients and cases (case management)

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-401<br><sub>p.15</sub> | 4 | SYS | Manage case status so the lifecycle is controlled | On Submit, the state follows the next workflow step. The previously assigned user's edit rights are locked. All transitions are logged | **CHANGE** | Workflow stages, transitions and history (`workflow/domain/WorkCaseHistory.java`) | Workflow `SCR_CASE` (NEW -> INVESTIGATION -> UNIT_HEAD_APPROVAL -> COMPLIANCE_REVIEW -> AML_COMMITTEE -> STR_PREPARATION -> CLOSED; RETURNED). Edit rights come from the stage owner and the assignee only. The case timeline `scr_case_event` mirrors every transition with the user and remarks | `screening` | M |  | **Built**: cases/service/CaseMover.java, CaseStageMirror.java, CaseAccess.java |
| SNSRP-402<br><sub>p.15-16</sub> | 4 | INV / UCC / CO | View a list of all cases and open each case so compliance activities can be reviewed, tracked and managed | The list follows the filters applied (date created, Marketing Unit, etc.) | **CHANGE** | Queue lists per stage (`workflow/service/WorkQueueService.java`) | Case list `GET /screening/cases` (status tabs, filters: created date, marketing unit, unit head, case type, risk category, stage, assignee, SLA state). Scoped by role: the Investigator sees own and team cases; UCC, CO and Auditor see all | `screening` | S |  | **Built**: cases/service/CaseQueries.java, CaseSearch.java; screen `/screening/cases` |
| SNSRP-403<br><sub>p.16</sub> | 4 | INV / UCC / CO | Search cases by relevant criteria to find and review specific cases quickly | A user with view rights searches by case ID, customer name, status, date range or assigned user, sees the matching cases, can refine the criteria and opens a case | **CHANGE** | Same | Search on the same endpoint (case no., client name / code, status, date range, assignee); the filters stay in the URL | `screening` | S |  | **Built with parked item**: cases/service/CaseSearch.java; accent-insensitive search not built |
| SNSRP-404<br><sub>p.16</sub> | 4 | INV | Re-assign a case or approval request to another eligible reviewer / approver (workload, absence, conflict of interest) | For a request in Pending Review or Pending Approval, only eligible users by role and access can be selected. Ownership changes without changing submitted data or status history. The reason, previous and new assignee, user and time go to the audit log. Only the new assignee can act | **CHANGE** | `WorkAssignmentService.assign(caseId, assignee, eligibleUsers)` without a reason | Mandatory reason (LOV `SCR_REASSIGN_REASON`: WORKLOAD, ABSENCE, CONFLICT_OF_INTEREST, OTHERS); eligibility = holders of the stage permission minus the client's own account officer (conflict). A platform change adds an optional reason to `assign` and the history (section 9 of the design) | `screening` + `workflow` | S | SQ13 | **Built**: cases/service/CaseAssignmentService.java |
| SNSRP-405<br><sub>p.17</sub> | 4 | SYS | SLA timer per stage so overdue items are visible and escalated | When the remaining-time threshold is reached, reminders are sent. When the SLA is breached, the case is flagged and escalated | **CHANGE** | Daily overdue alert only (`workflow/service/WorkSlaAlertCheck.java`) | Hourly job `SCR_SLA_MONITOR`: reminder at due minus lead hours (SNSRP-108), a breach flag on the case, escalation notice to the configured role, alert `SCR_SLA_BREACH`. The SLA badge shows on lists | `screening` | S | SQ08 | **Built with parked item**: cases/service/CaseSla.java, SlaMonitor.java, SlaMonitorJob.java; calendar hours |

### Capability 5. Review templates accomplished in the system

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-501<br><sub>p.17</sub> | 5 | INV | Guided KYC / Transaction review templates so assessments are consistent | When the review opens, mandatory fields are marked. An incomplete review cannot be submitted, and each missing field is flagged | **NEW** | None | Review form rendered from `scr_template_field` (version fixed at start). Answers are stored in `scr_case_review` / `scr_case_answer`. Validation runs on the server and the form shows it field by field. Save as draft is allowed | `screening` | M | SQ05 | **Built with parked item**: cases/service/CaseReviewService.java, ReviewValues.java; field lists SQ05 |
| SNSRP-502<br><sub>p.17</sub> | 5 | INV | Select a disposition from the given list so the case advances | With all required fields complete, Submit routes the case by the Approval Matrix, and the case becomes read-only to the Investigator | **CHANGE** | Workflow actions with reason codes | Disposition from `SCR_DISPOSITION` for the stage (SNSRP-107) + recommendation text; submit = validation (SNSRP-701) + router (SNSRP-103) + transition `submit` | `screening` | S |  | **Built**: cases/service/CaseSubmissionService.java, CaseValidator.java |

### Capability 6. KYC documents uploaded; matching triggers

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-601<br><sub>p.17</sub> | 6 | INV | Upload KYC or other supporting documents with metadata so evidence is traceable | The file is stored with type / date / source and shown on the case timeline. The name follows `<Form Type>_<Client Name>_<Date Received>_<Document Type>_<sequence number>` | **CHANGE** | Attachments with checksum, type checks and audit (`attachment/service/AttachmentService.java`); KYC documents on the client (`crm/service/KycDocumentService.java`); naming `<REFERENCE>_<DOCTYPE>_<n>` (`attachment/service/DocumentNamingService.java`, syntax parked Q23) | Case documents are attachments of entity type `ScreeningCase`, with the metadata `scr_case_document` (form type, document type, date received, source). A new naming pattern `SCREENING` in `DocumentNamingService` produces the BRD syntax. KYC-type documents are also registered on the client (`KycDocumentService`) | `screening` + `attachment` | S | SQ14 | **Built with parked item**: cases/service/CaseDocumentService.java, attachment DocumentNamingService (SCREENING); delete guard on the generic attachment API not built |
| SNSRP-602<br><sub>p.14-15</sub> | 6 (prints 6; trigger of 3) | SYS | Trigger client matching when a new client is created or during a defined period | Once a client is created, matching starts on its own with no user action, on the new client only, with the latest rules and data. When a defined period starts (e.g. a batch window), all eligible clients in scope are processed and no ineligible client is included. Every activity is logged and traceable | **NEW** | Client creation opens the onboarding case; no event for other modules (`crm/service/ClientService.java`) | crm publishes `ClientRegistered` after commit. Screening runs an asynchronous single-client screening. Job `SCR_PERIODIC_SCREENING` (cron = the batch window, parameter `SCR_SCREENING_SCOPE`: client statuses, e.g. PROSPECT and CONFIRMED; excludes INACTIVE). Delta screening of all clients against the entries changed since the last run. Each run is logged in `scr_screening_run` | `screening` + `crm` | M | SQ11 | **Built with parked item**: matching/service/ScreeningTriggers.java, BatchScreening.java, PeriodicScreeningJob.java; runs after commit in the request thread |

### Capability 7. In-system routing for review and approval; STR

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-701<br><sub>p.17</sub> | 7 | SYS | Validate dispositioned cases before approval against the set criteria so only valid cases are routed for approval | When the user submits a dispositioned case, it is checked against the configured criteria. Its status is updated and it routes to the right approver by the criteria. Every validation result is logged for audit | **NEW** | None | Validation rules `scr_validation_rule` (versioned: mandatory template complete, required document types by case type, disposition allowed at the stage, recommendation present, STR flag consistent with the disposition). Each result is written to `scr_case_event` (VALIDATED / VALIDATION_FAILED with messages) | `screening` | S | SQ06 | **Built**: cases/service/CaseValidator.java, config/service/ValidationRules.java |
| SNSRP-702<br><sub>p.18</sub> | 7 | APR | Approve / disapprove recommendations with comments so decisions are documented | On approval, the case moves to the next workflow step. On disapproval, the approver records a rationale and the case returns to the requestor, who can resubmit it | **CHANGE** | Workflow transition with note / return reason (`workflow/service/TransitionNote.java`) | Transitions `approve` / `disapprove` (rationale mandatory, returns to INVESTIGATION as RETURNED) / `resubmit`. Approvals also show in My Approvals (`ScreeningApprovalSource`) | `screening` | S |  | **Built**: cases/service/CaseApprovalService.java (unit head) |
| SNSRP-703<br><sub>p.18</sub> | 7 | CO | Review BU escalations so policy adherence is ensured | The CO records an outcome, the case routes by the escalation matrix (e.g. to the AML Committee), and the audit trail gets the outcome, user, time and remarks. If items need correction, the CO adds remarks and the case returns to the Investigator: status Returned, reasons captured and logged | **CHANGE** | Workflow return actions | Stage COMPLIANCE_REVIEW: outcomes from `SCR_DISPOSITION` (parent COMPLIANCE_REVIEW). The escalation route comes from `scr_approval_route` (stage COMPLIANCE_REVIEW -> AML_COMMITTEE / STR_PREPARATION / CLOSED). `return_for_rework` needs a reason | `screening` | S |  | **Built**: cases/service/CaseApprovalService.java (Compliance outcome) |
| SNSRP-704<br><sub>p.18-19</sub> | 7 | AMLC-M | Review cases that need a committee decision so governance is met | The member views the complete case (details, investigation findings, documents, prior comments) and records a decision online. When the decision is final, the case routes by the governance workflow and its status is updated. The member's decision, remarks, user ID, date and time are audited | **NEW** | None (the workflow has one assignee per stage) | Committee stage with `scr_committee_vote` (member, decision, remarks, at). Finalisation rule from parameter `SCR_COMMITTEE_RULE` (ANY / MAJORITY / ALL, default MAJORITY, SQ15). The last required vote triggers the system transition (STR_PREPARATION or CLOSED) | `screening` | M | SQ15 | **Built with parked item**: cases/service/CommitteeService.java, CommitteeRule.java; rule and size SQ15 |
| SNSRP-705<br><sub>p.19</sub> | 7 | CO | STR forms prepopulated from case data so filing is efficient | For a case that needs an STR, opening the STR prefills the party details, narrative and attachments, and they stay editable. The completeness check shows the gaps | **NEW** | None | `scr_str` (case, template version, subject party snapshot, transaction details `scr_str_transaction`, narrative, reason codes, status DRAFT / FOR_APPROVAL / APPROVED / EXTRACTED / FILED, AMLC reference, filing date). The fields are prefilled from the client, the case review and the account / invoice data (read-only services). Completeness is checked against the STR template | `screening` | M | SQ09 | **Built with parked item**: str/service/StrService.java, StrPrefill.java; invoice / receipt prefill behind StrTransactionSource, reasons SQ09 |
| SNSRP-706<br><sub>p.19-20</sub> | 7 | CO | Extract the list of AML Committee-approved STR cases in the AMLC reporting format | Only Committee-approved STR cases are selected. The STR list is produced in the AMLC format and saved on its own to the designated folder path. The extraction is audited | **NEW** | Report archive (`report/core/ReportArchiveService.java`) | `scr_str_extraction` (batch, period, count, file). The file is built by `scr_str_layout` (SNSRP-105) and archived (`ReportArchiveService.archiveGenerated`). Port `StrFileSink` with a default archive adapter; the shared-folder adapter is parked (SQ16). After filing on the portal, the CO records the AMLC reference (FILED) | `screening` | M | SQ09, SQ16 | **Built with parked item**: str/service/StrExtractionService.java, StrFilingService.java, ArchiveStrFileSink.java; AMLC XLSX / XML format SQ09 |

### Capability 8. System-generated notifications

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-801<br><sub>p.20</sub> | 8 | INV / UCC / CO / APR | Notifications for new cases assigned so action is timely | When a case is created or assigned, the assignee gets a notification and opens the case directly from it. The notification event (recipient, type, date and time) is logged | **CONFIGURE** | In-app notifications with link, preferences and event log (`messaging/service/NotificationService.java`, `messaging/domain/NotificationEvent.java`); assignment notices (`WorkAssignmentService`) | Notification events `SCR_CASE_ASSIGNED`, `SCR_CASE_RETURNED`, `SCR_CASE_FOR_APPROVAL`, `SCR_COMMITTEE_REVIEW`; link `/screening/cases/{id}` | `screening` | S |  | **Built with parked item**: cases/service/CaseNotifier.java; assignment announced by the platform assignment notice (no separate SCR_CASE_ASSIGNED) |
| SNSRP-802<br><sub>p.20</sub> | 8 | UCC / INV | SLA / document reminders so reviews stay on track | If documents are missing and the turnaround criteria are not met, reminders are sent | **CHANGE** | Daily overdue alert only | `SCR_SLA_MONITOR` (SNSRP-405) also reminds about missing required documents (validation rule of SNSRP-701 run in advisory mode). Notice `SCR_DOCUMENT_REMINDER` to the assignee and the UCC | `screening` | S |  | **Built**: cases/service/SlaMonitor.java (SCR_SLA_REMINDER, SCR_DOCUMENT_REMINDER) |

### Capability 9. Reports on demand and audit

| BR ID | Cap. | Persona | Requirement | Acceptance criteria (BRD) | Fit | Current capability | Proposed solution | Module | Size | Q | As built (S2) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SNSRP-901<br><sub>p.20</sub> | 9 | CO / UCC | Operational and compliance reports | With filters (date, Marketing Unit / Unit Head, disposition, case status), the report is generated and exported to CSV / XLSX / PDF | **CHANGE** | Report framework with automatic PDF / XLSX / CSV export (`report/core/ReportDefinition.java`, `TabularReportBuilder.java`) | New category COMPLIANCE and the reports of section 7. Common parameters: date range, marketing unit, unit head, disposition, case status | `screening` | M | SQ17 | **Built**: report/CaseReports.java, ClientReports.java, ListReports.java, StrRegisterReport.java |
| SNSRP-902<br><sub>p.21</sub> | 9 | SYS | Immutable audit logs of every system and user action, for complete, reliable and tamper-proof evidence | Every action writes an immutable audit entry that no user can alter or delete. Audit records are kept per regulatory and internal retention policies | **FIT** | `audit_log` is insert-only in the database (`backend/src/main/resources/db/migration/V26__audit_log_immutable.sql`); `AuditTrailService` on every change | Every screening service writes `AuditTrailService.record`. The structured before / after values are in `scr_config_version.diff`, `scr_watchlist_change` and `scr_case_event`, which are insert-only (trigger, same pattern as V26). Retention: 5 years online + 5 years archive (NFR) | `platform (audit)` | S | SQ18 | **Built**: `audit_log` (V26); insert-only `scr_case_event`, `scr_committee_vote` (V1054), `scr_watchlist_change` (V1052), `scr_client_risk_profile` (V1053) |
| SNSRP-903<br><sub>p.21</sub> | 9 | CO | Access the audit log report as needed | With filters (date, Marketing Unit / Unit Head, disposition, status), the report updates and exports to CSV / XLSX / PDF | **CHANGE** | `CTL-AUDIT` (date range, user, entity type) on Administration > Audit Trail | `SCR-AUDIT-LOG` over `scr_case_event` + `audit_log` for entity types `ScreeningCase`, `ScreeningConfig`, `WatchlistEntry`, with the case filters of SNSRP-901 | `screening` | S |  | **Built**: report/CaseReports.java (SCR-AUDIT-LOG, SCR_AUDIT_VIEW) |

## 6. Lists and parameters from the BRD

| Item | Source | Content in the BRD | Treatment |
|---|---|---|---|
| Matching methods | SNSRP-101 | exact / phonetic / fuzzy thresholds | `scr_match_rule`; values to be set by Compliance (SQ02) |
| List sources | pp.6-8, SNSRP-201 | "AML" e-mail advisory; NLDS (PEP, corporate only per p.7 note "Applicable for Corporate only"); sanctions / NLDS-PEP list | `scr_watchlist_source` seeds: `AML_ADVISORY` (SANCTION, FILE), `NLDS_PEP` (PEP, FILE), `INTERNAL` (INTERNAL, MANUAL). Transports parked (SQ01) |
| Review templates | p.6-8, SNSRP-104 / 501 | KYC, Transaction ("i.e. KYC, Transaction"); EDD named in the benefits (p.4-5) | Template types KYC_REVIEW, TRANSACTION_REVIEW, EDD, STR; field lists to be supplied (SQ05) |
| Case personas and stages | pp.6-8 | Compliance Officer, Investigator, Approver / Unit Head, Compliance (BU escalation), AML Committee, Compliance (STR) | Workflow `SCR_CASE` (design section 7) |
| File naming | SNSRP-601 | `<Form Type>_<Client Name>_<Date Received>_<Document Type>_<sequence number>` | `DocumentNamingService` pattern `SCREENING` |
| Report filters | SNSRP-402 / 901 / 903 | Date created, Marketing Unit / Unit Head, Disposition, Case Status | Common report parameters |

## 7. Reports

| Code | Report | BRD source | Parameters |
|---|---|---|---|
| `SCR-HIGH-RISK-CLIENTS` | Extract list of high-risk clients (with PEP and sanction flags, open case) | p.8 "Extract list of High-risk Clients"; capability 4 | as-of date, risk category, marketing unit, unit head, client type |
| `SCR-CASE-STATUS` | Status monitoring report (cases by stage, assignee, age, SLA state) | p.8 "Status Monitoring reports"; SNSRP-901 | date range, marketing unit / unit head, disposition, case status, case type |
| `SCR-SLA-BREACHES` | SLA reminders, breaches and escalations | SNSRP-405 / 108 | date range, stage, team |
| `SCR-SANCTIONED-NAMES` | List of sanctioned names (active entries, by source / list type, added / delisted in period) | p.6 current report "List of Sanctioned Names" | source, list type, status, date range |
| `SCR-PEP-CLIENTS` | List of approved PEP clients | p.7 current report "List of approved PEP clients" | as-of date, marketing unit |
| `SCR-INGEST-ERRORS` | Unsuccessful ingestion records | SNSRP-202 | run, source, date range |
| `SCR-STR-REGISTER` | STR register (drafted, approved, extracted, filed with AMLC reference) | SNSRP-705 / 706 | date range, status |
| `SCR-AUDIT-LOG` | Audit report (screening, configuration, list, case events) | p.8 "Audit Reports"; SNSRP-902 / 903 | date range, marketing unit / unit head, disposition, status, user |

## 8. Non-functional requirements (pp.22-25)

| Topic | BRD | Approach | Fit |
|---|---|---|---|
| Users (max / concurrent) | Compliance Officer 2 / 2 (config), CO Checker 1 / 1, Investigator 287 / 287 (risk-tag updates, KYC uploads) and 120 / 120 (case review), Operations Lead 1 / 1, Unit Head 8 / 8, CO 2 / 2 (BU escalation, STR), AML Committee 5 / 5, CO / Ops Lead 3 / 3 (reports), Auditor 2 / 2, System Admin 3 / 1 | Fewer than 300 named users, within the BRD-1 sizing (145 concurrent). 287 "concurrent" investigators is the named population, not a load; confirm (SQ19) | CONFIGURE |
| Volumes | Config and approvals 10 / month each; list intake and matching min. 10 / max. 300 per day; case creation, review, uploads, approvals, escalations 1-15 / week; STR 1-5 / week; reports 10 / month; audit reports 20 / year; user enrolments 5-10 / year. Growth 20% a year | Delta screening per list change; the full periodic screening runs in the batch window | FIT |
| Response time | 3-5 seconds per screen load; not applicable to system jobs | p95 < 3 s online; matching and extraction are asynchronous | FIT |
| Peak demand | End of month / year-end; 08:00-17:00 daily | Batch windows outside 08:00-17:00 | CONFIGURE |
| Devices | Mobile and desktop users expect the same speed | Responsive UI | FIT |
| Availability | 99.9%; used 08:00-18:00 Mon-Fri; downtime at most 45 minutes a month (planned maintenance only); maintenance 00:00-04:00; BCP threshold under 3 days | Same deployment as BRD-1; adds a fifth set of service hours to the alignment question (OQ44 / CQ25 / AQ27) | CONFIGURE |
| Retention | Transaction records / KYC documents / supporting documents: 5 years online, 5 years archive, backup daily, accessibility daily, backup retention 5 years | Retention rules `SCREENING_CASE`, `WATCHLIST_ENTRY` in `nba_retention_rule` (5 / 5); a provider implements `nbadmin.service.RetentionCandidateProvider`. Archive and purge stay parked as for BRD-1 | CHANGE |
| Anonymisation | None over time | None | FIT |
| Regulatory | RA 9160 AMLA and amendments, BSP Circular 1182 (2023), BSP CL-2023-030, BSP M-2025-017 (adverse media), IC CL 2019-65 (penalties) | Controls: maker-checker, immutable audit, retention. Adverse media screening is not a BR row (SQ20) | FIT |

## 9. Questions answered by this BRD

| Earlier Q | Topic | Answer from BRD-10 | Design impact |
|---|---|---|---|
| Q21 (BRD-1) | KYC review frequency by risk rating | Partly. Compliance defines the risk profile categories and matrix in the system (SNSRP-102, p.8). High-risk and PEP clients with an **active policy** need a KYC review / EDD case (SNSRP-303). The review frequency per rating is still not given | Risk categories map to `KYC_RISK_RATING`; `KYC_REVIEW_MONTHS_HIGH_RISK` stays a parameter. A high-risk tag now opens a screening case in addition to the periodic `KYC_REVIEW_DUE` list |
| Q18 (BRD-1) | Enforcement of tags | Partly. `PEP` and high-risk tags are set by rules and trigger reviews, not blocks (SNSRP-302 / 303) | Screening sets `PEP` / `WATCHLIST_REVIEW`. The banner keeps "warn only". The block stays a proposal behind a parameter (SQ07) |
| Q23 (BRD-1) | File-naming syntax | For screening documents: `<Form Type>_<Client Name>_<Date Received>_<Document Type>_<sequence number>` (SNSRP-601). The general syntax of other modules stays open | `DocumentNamingService` gets a second, named pattern; `BRNB.026` default unchanged |
| Q39 (BRD-1) | Retention per record type | For screening transaction records, KYC and supporting documents: 5 years online, 5 years archive, daily backup, 5-year backup retention (p.25). Other record types stay open | Two retention rules added (5 / 5) |
| Q16 (BRD-1) | BDO KYC standard / CIF | Not answered. The BRD names BDO's NLDS as the PEP validation source (p.7) | NLDS is a parked source (SQ01) |
| OQ48 / AQ28 / PQ17 | Role matrices | Partly, for this area: the personas are Compliance Officer, Compliance Checker, Unit Compliance Coordinator, Investigator, Approver (Unit Head), AML Committee Member, Operations Lead, Auditor, System Admin (pp.10-24) | Six new roles (design section 6). Who the 287 investigators are is SQ19 |
| OQ44 / CQ25 / AQ27 / PQ18 | NFR alignment | Not resolved: this BRD adds 08:00-18:00 Mon-Fri, a 00:00-04:00 maintenance window, at most 45 min downtime a month, and 5 + 5 years retention | Listed for the consolidated NFR decision |
| **Operations parked items** OQ01, OQ02, OQ07, OQ45 | Collection / Disbursement / Accounting systems, GL entries, Marketing scope | **None answered.** Screening has no money flow and no interface to Collection, Disbursement or Accounting | No change to `opsledger`, `cashiering`, `remittance`, `commission`, `collections`, `disbursement`, `payrequest`, `acsl`, `frbs`, `journal` |

## 10. Open questions for BDOI (new)

| Q# | Topic | Question | Related |
|---|---|---|---|
| SQ01 | List sources and transport | Which lists are screened: the "AML" advisory (from the BDO AML unit? AMLC / UN Security Council consolidated list?), NLDS-PEP, OFAC, others? For each: format, frequency ("real time or agreed schedule"), and transport (e-mail file, SFTP, API, direct NLDS access). Who in BDOI may upload a list file? Who receives the SNSRP-202 alert, and on what schedule? | SNSRP-201-202 |
| SQ02 | Matching thresholds | Initial thresholds per method (exact / phonetic / fuzzy) and the fields compared (name only, or name + birth date / nationality / ID)? Is a hit on aliases the same as on the primary name? Are corporate names screened with the same rules? | SNSRP-101, 301 |
| SQ03 | Risk categories | The defined Risk Profile Category memo (pp.6-7): categories, criteria and the resulting rating; which categories require EDD; does "High-risk" equal KYC_RISK_RATING HIGH? | SNSRP-102, 302 |
| SQ04 | Approval and assignment matrices | Approval and assignment rules: by marketing unit, segment (Combank, Corbank, Retail), risk category, case type? Who is the "Approver": the Unit Head of the client's marketing unit? | SNSRP-103, 106 |
| SQ05 | Templates | Field lists of the KYC review, transaction review and EDD templates, and of the STR form | SNSRP-104, 501, 705 |
| SQ06 | Dispositions and validation | Dispositions per stage (Investigator, Approver, Compliance, AML Committee) and the validation criteria before approval (SNSRP-701) | SNSRP-107, 701 |
| SQ07 | Business gate | While a potential or true match is open, must BIBS block or warn on quotation, account submission, placement or booking for the client? (The BRD is silent; default: warn by banner only.) | SNSRP-303 |
| SQ08 | SLA matrix | SLA hours per stage, reminder lead time and escalation targets; working hours or calendar hours; the holiday calendar | SNSRP-108, 405, 802 |
| SQ09 | STR format | The AMLC prescribed format for the STR extraction (file type, fields, codes); are covered transactions (CTR) in scope? Who approves the STR before extraction (AML Committee only)? | SNSRP-105, 705, 706 |
| SQ10 | Active policy | Definition of "client with an active policy": booked account with policy period covering today? Includes renewals in progress and endorsements? | SNSRP-303 |
| SQ11 | Screening scope and window | Which clients are screened in the periodic run (prospects, confirmed, inactive, bank clients already screened by BDO)? Batch window and frequency; also screen other parties (beneficial owners, authorised signatories, beneficiaries, insurers) which BIBS does not hold today? | SNSRP-602 |
| SQ12 | False-positive suppression | After a validated false positive, is the client exempt from the same entry until the entry changes, or re-screened each run? | SNSRP-304 |
| SQ13 | Re-assignment rights | SNSRP-404 persona is Investigator: may investigators re-assign their own cases, or only the UCC / team lead? | SNSRP-404 |
| SQ14 | Form Type | Meaning of "Form Type" in the naming convention (review template type? KYC form code?) | SNSRP-601 |
| SQ15 | AML Committee | Decision rule (any member, majority, all, quorum) and whether members vote separately | SNSRP-704 |
| SQ16 | Designated folder | Where the STR file must be saved ("designated folder path": shared drive, SFTP)? Is the in-system archive with download acceptable? | SNSRP-706 |
| SQ17 | Report layouts | Columns of the status monitoring and audit reports; content of "List of High-risk Profiles" | SNSRP-901, 903 |
| SQ18 | Audit retention | "Retained per regulatory and internal retention policies": 5 + 5 years as the data table, or longer (AMLA record keeping 5 years from account closure)? | SNSRP-902 |
| SQ19 | Investigators | Who are the 287 investigators (Marketing Account Officers, branch staff)? Do they already have BIBS roles? "287 concurrent" as a load target? | NFR |
| SQ20 | Adverse media | BSP M-2025-017 (adverse / negative media) is cited: is adverse-media screening in scope (list type ADVERSE_MEDIA as a manual source only)? | p.3 |

## 11. Observations on the BRD pack

- SNSRP-602 is printed with capability 6 but sits between SNSRP-304 and SNSRP-401. It describes a matching trigger (capability 3), and there is no SNSRP-3xx for it. Recorded as printed.
- Capability 6 ("KYC documents can be uploaded") has one functional ID (SNSRP-601). Capability 3 has SNSRP-301 to 304 and no ID for the scheduled trigger other than 602.
- The persona of SNSRP-404 (re-assignment) is "Investigator", but the acceptance criteria speak of reviewers and approvers, which suggests a coordinator role (SQ13).
- The AML Committee is plural in the process, but the criteria of SNSRP-704 describe one member's decision; the decision rule is not given (SQ15).
- The current-state PEP process notes "Applicable for Corporate only" under the NLDS validation step (p.7). It is unclear whether PEP screening of individuals uses another source (SQ01).
- The to-be diagram keeps "File STR to AMLC" in a "Via Portal" lane, so portal filing stays outside BIBS; SNSRP-706 only requires the extract and a saved file.
- The usage table gives 287 investigators as both maximum and concurrent users. Concurrency equal to the named population is unusual (SQ19).
- The usage table lists "System Admin - Manage user access and roles" (5-10 enrolments a year); this is the User Access Maintenance BRD (BRD-11) and is covered there.
- The benefits cite EDD, but no requirement ID defines an EDD form beyond "KYC Review / Enhanced Due Diligence" in SNSRP-303. EDD is treated as a review template type.
- The approval sheet shows the Unit Head - Processing as "for regularization - on leave" (p.29), without a signature.
- The BRD refers to BDOIR, while the other BRDs use BDOI.
- The penalty table (pp.3-4) cites "[legaldex.com], [anqacompliance.com]" as sources inside the AMLC column; it is background, not a requirement.
