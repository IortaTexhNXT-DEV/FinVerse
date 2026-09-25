# iNXT BrokerVerse - BDOI Sanction Screening and Risk Profiling (BRD-10) Build Design

Status: **proposal for review**. This design extends [`BROKING_ARCHITECTURE.md`](BROKING_ARCHITECTURE.md) and the Developer Guide, which stay binding. It changes them only through the contract changes in section 9.

Requirements baseline: [`BDOI_SANC_BRD_SPEC.md`](../requirements/BDOI_SANC_BRD_SPEC.md). It has 37 requirement IDs (SNSRP-101 to SNSRP-903) and questions SQ01-SQ20. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `SNSRP-301`.

## 1. Design principles

1. **One compliance module on platform services.** The new module `screening` owns the watchlist, the matching engine, the risk rules, screening cases and STRs. It reuses the platform for everything else:
   - `workflow` for case stages, queues, SLA and assignment;
   - maker-checker and `approval` for four eyes;
   - `lov` for dispositions;
   - `attachment` for evidence;
   - `messaging` for notices;
   - `system` jobs and `alert` for monitoring;
   - `report` for reports;
   - `audit` for the trail.

   Nothing is duplicated.
2. **The client master stays in `crm`.** Screening never writes `crm_client` directly. It calls the new `crm.service.ClientRiskService` to set the risk rating and the tags `PEP` / `WATCHLIST_REVIEW`. The KYC review cycle, the client banner and the Client 360 view then pick up the result with no further change.
3. **Configuration is versioned data, never code.** The following are rows of a versioned configuration set, approved by a Compliance Checker and active from an effective date:
   - matching criteria;
   - risk categories and rules;
   - approval, assignment and SLA matrices;
   - validation rules;
   - review and STR templates.

   An active version is immutable. A change is a new draft (SNSRP-101-109). A case keeps the version it started with (SNSRP-104 "applies to new reviews only").
4. **Screening is event-driven and idempotent.** Four triggers each create a `scr_screening_run`:
   - a client is registered (`ClientRegistered`);
   - an account is submitted (`WorkCaseTransitioned` NB_ACCOUNT -> SUBMITTED);
   - a list entry changes (delta screening);
   - the periodic batch window.

   Re-running a run for the same client, entry version and rule version produces no second match.
5. **Screening informs, it does not block (default).** The BRD asks for tags, cases and notifications, not for a stop on business. A potential gate on account submission and placement is built behind the parameter `SCR_BLOCK_ON_OPEN_MATCH` (default `false`, SQ07).
6. **No money.** Screening posts no journal, creates no open item and publishes no accounting event.
7. **External sources are ports.** The following get a seam and a manual default; the real transports are parked (section 10):
   - the list feeds (AML advisory, NLDS-PEP);
   - the AMLC STR file destination;
   - the active-policy definition.

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `screening` (new, package `com.iortatechnxt.brokerverse.screening`, tables `scr_*`) | Versioned configuration (criteria, risk rules, matrices, SLA, validation, templates), watchlist sources, entries and ingestion, name keys and matching engine, screening runs and matches, client risk profile history, screening cases (workflow `SCR_CASE`), reviews, documents, committee votes, STRs and extraction, SLA monitor, reports | SNSRP-101-109, 201-204, 301-304, 401-405, 501-502, 601-602, 701-706, 801-802, 901-903 | crm (clients, `ClientRiskService`, `ClientRecordsProvider`), account (read), catalog (sales organisation, unit head), workflow, approval, lov, attachment, messaging, bulk, report, alert, system, audit, security, nbadmin (retention port), organization | V1050-V1055 (V1950-V1952) |
| `crm` (built) | + `ClientRiskService` (rating + tags + audit + review date); + events `ClientRegistered`, `ClientIdentityChanged` | SNSRP-302, 304, 602 | unchanged | none needed (code only) |
| `workflow` (built) | + `WorkflowService.overrideDue`; + a reason on `WorkAssignmentService.assign` | SNSRP-108, 404 | unchanged | none (history has `reason_code`) |
| `attachment` (built) | + named naming patterns in `DocumentNamingService` | SNSRP-601 | unchanged | none |
| `report` (built) | + `ReportCategory.COMPLIANCE`, `ReportMetadata.compliance(...)` | SNSRP-901 | unchanged | none |

Inner layout of `screening` (sub-packages are owned by one build agent each, section 13):

| Sub-package | Content |
|---|---|
| `screening.config` | `ConfigVersion`, typed rule entities, `ConfigVersionService` (draft, submit, approve, reject, diff, activation), resolvers `ActiveConfig` (as of a date) |
| `screening.watchlist` | Sources, entries, aliases, changes (maker-checker), `WatchlistFeed` port and file adapter, `SCR_WATCHLIST` bulk handler, ingestion runs and errors, jobs `SCR_WATCHLIST_INGEST`, `SCR_INGEST_ERROR_DIGEST` |
| `screening.matching` | `NameNormaliser`, `NameKeys` (tokens, Double Metaphone), `NameScorer` (exact, phonetic, Jaro-Winkler; pure functions), `ScreeningEngine`, runs, matches, suppressions, triggers (event listeners, job `SCR_PERIODIC_SCREENING`) |
| `screening.risk` | `RiskRuleEvaluator`, `ClientRiskProfile` history, manual override (SNSRP-304) |
| `screening.cases` | `ScreeningCase`, `ScreeningCaseService` (open, route, submit, approve, return, escalate, committee), `CaseRouter` (approval / assignment matrices), `CaseValidator` (SNSRP-701), reviews and answers, documents, events, `ScreeningApprovalSource`, SLA monitor job `SCR_SLA_MONITOR` |
| `screening.str` | STR, transactions, prefill, completeness, extraction, `StrFileSink` port |
| `screening.report` | The eight reports of section 11 |
| `screening.common` | Shared enums, `ScreeningPermissions`, the case query service |

### 2.1 Dependency graph (arrows = "depends on")

```
screening ──► crm (ClientService read, ClientRiskService, ClientRecordsProvider beans, events)
          ──► account (read: client and sales stamp of a submitted account)
          ──► catalog (SalesOrganisationService: marketing unit, unit head)
          ──► workflow, approval, lov, attachment, messaging, bulk, report, alert, system, audit, security
          ──► nbadmin (implements RetentionCandidateProvider)
crm, account, catalog, workflow ... do NOT depend on screening
```

The two triggers come from events that screening listens to. crm publishes `ClientRegistered` (new, crm-owned type). workflow already publishes `WorkCaseTransitioned`. So no built module gains a dependency on `screening`, and ArchUnit stays cycle-free.

## 3. Flyway allocation

Allocated range: **schema V1050-V1059, demo V1950-V1959** (Developer Guide: V1000-V1899 for later BRDs, ten versions each; demo V1900-V1999 in the same order). Six schema versions are used and four are kept free.

| Version | Owner (wave) | Content |
|---|---|---|
| `V1050__screening_foundation.sql` | S0 | Roles and grants (section 6), `sec_permission_action` rows (area `SCREENING`), LOV types and seeds (section 8), `sys_parameter` rows, alert codes, notification events, workflow `SCR_CASE` (`wf_stage`, `wf_transition`), retention rules `SCREENING_CASE` and `WATCHLIST_ENTRY` in `nba_retention_rule` (5 / 5 years) |
| `V1051__screening_config.sql` | S1-A | `scr_config_version`, `scr_match_rule`, `scr_risk_category`, `scr_risk_rule`, `scr_approval_route`, `scr_assignment_rule`, `scr_sla_rule`, `scr_validation_rule`, `scr_template`, `scr_template_field`, `scr_str_layout`, `scr_str_layout_column` |
| `V1052__screening_watchlist.sql` | S1-A | `scr_watchlist_source`, `scr_watchlist_entry`, `scr_watchlist_alias`, `scr_watchlist_change`, `scr_ingestion_run`, `scr_ingestion_error`, `scr_name_key` |
| `V1053__screening_matching.sql` | S1-B | `scr_screening_run`, `scr_match`, `scr_match_suppression`, `scr_client_risk_profile` |
| `V1054__screening_cases.sql` | S1-C | `scr_case`, `scr_case_event` (insert-only trigger), `scr_case_review`, `scr_case_answer`, `scr_case_document`, `scr_committee_vote` |
| `V1055__screening_str.sql` | S1-C | `scr_str`, `scr_str_transaction`, `scr_str_extraction` |
| `V1056-V1059` | - | Kept free (follow-ups; the AMLC layout seed once SQ09 is answered) |
| `db/demo/V1950__demo_screening_users_config.sql` | S1-A | Demo users (section 6.2), one ACTIVE version of every configuration type (thresholds, three risk categories, routes, SLA, KYC / transaction / EDD / STR templates) and one PENDING draft for `compchk` |
| `db/demo/V1951__demo_screening_watchlist.sql` | S1-B | Two sources, 25 **fictitious** list entries (clearly invented names; never real listed persons), one failed ingestion run with errors, one completed screening run with potential matches against the demo clients (V981) |
| `db/demo/V1952__demo_screening_cases.sql` | S1-C | Cases in every stage (new, investigation, returned, unit head approval, compliance review, committee, STR preparation, closed), one committee vote pending, one STR approved and one extracted |

Rules:
- Foreign keys only to platform tables (users, branches, workflow, LOV, attachments). Client, account and list references are ids or codes without foreign keys to V8xx tables, as in Operations and Collections.
- `V1050` runs after `V801` (crm LOV, workflow `NB_CLIENT`), `V790` (retention rules) and `V755` (`sec_permission_action`) on a fresh database; nothing in it depends on V1000+.

## 4. Entities (key fields)

Every table has `company_id` (where business data), the audit columns (`created_at/by`, `updated_at/by`) and `version`.

### 4.1 Configuration (`screening.config`)

| Table | Key fields | Notes |
|---|---|---|
| `scr_config_version` | `config_type` (MATCH_CRITERIA, RISK_RULES, APPROVAL_MATRIX, ASSIGNMENT_MATRIX, SLA_MATRIX, VALIDATION_RULES, TEMPLATE, STR_LAYOUT), `version_no`, `status` (DRAFT, PENDING, ACTIVE, SUPERSEDED, REJECTED), `effective_from`, `submitted_by/at`, `decided_by/at`, `decision_reason`, `diff` (text: before / after lines against the active version) | Unique (company, type, version_no); at most one ACTIVE per type and effective date. `AuthorizableEntity` semantics; the maker never decides (SNSRP-109) |
| `scr_match_rule` | `version_id`, `list_type`, `subject_type` (INDIVIDUAL, ENTITY), `algorithm` (EXACT, PHONETIC, FUZZY), `threshold` numeric(5,4), `fields` (NAME, ALIAS, BIRTH_DATE, NATIONALITY, ID), `min_score_for_case` | SNSRP-101 |
| `scr_risk_category` | `version_id`, `code`, `name`, `tier`, `kyc_risk_rating` (LOV code), `tags` (CODE list), `case_type`, `requires_edd` | SNSRP-102 |
| `scr_risk_rule` | `version_id`, `priority`, `category_code`, `condition_attr` (MATCH_LIST_TYPE, MATCH_STATUS, PEP, NATIONALITY, OCCUPATION, SOURCE_OF_FUNDS, CLIENT_TYPE, MARKET_SEGMENT), `operator` (EQ, IN, NOT_IN), `values` | First matching rule by priority wins; the rule id goes to the profile history |
| `scr_approval_route` | `version_id`, `from_stage`, `case_type`, `risk_category`, `marketing_unit`, `disposition`, `to_stage`, `approver_kind` (UNIT_HEAD, ROLE, USER), `approver_value`, `order` | SNSRP-103, 703 (escalation matrix = routes from COMPLIANCE_REVIEW) |
| `scr_assignment_rule` | `version_id`, `case_type`, `trigger`, `risk_category`, `marketing_unit`, `client_type`, `team_role`, `user`, `balancing` (ROUND_ROBIN, LEAST_OPEN, NONE) | SNSRP-106; evaluated when the case is created only |
| `scr_sla_rule` | `version_id`, `stage`, `case_type`, `risk_category`, `sla_hours`, `reminder_lead_hours`, `escalate_to_role`, `calendar` (CALENDAR, WORKING) | SNSRP-108 |
| `scr_validation_rule` | `version_id`, `stage`, `case_type`, `rule` (TEMPLATE_COMPLETE, DOCUMENT_TYPES_PRESENT, DISPOSITION_ALLOWED, RECOMMENDATION_PRESENT, STR_FLAG_CONSISTENT), `parameters`, `blocking` | SNSRP-701, 802 |
| `scr_template` / `scr_template_field` | template: `code`, `template_type` (KYC_REVIEW, TRANSACTION_REVIEW, EDD, STR), `version_no`, `status`, `effective_from`; field: `section`, `code`, `label`, `data_type` (TEXT, LONG_TEXT, NUMBER, AMOUNT, DATE, LOV, CHECKBOX, ATTACHMENT), `lov_type`, `mandatory`, `help`, `order`, `prefill_source` (STR only) | SNSRP-104, 105, 501 |
| `scr_str_layout` / `_column` | `format` (CSV, FIXED, XLSX, XML), `delimiter`, `encoding`; column: `order`, `field_code`, `header`, `length`, `pad`, `code_map` | SNSRP-105, 706; the AMLC content is parked (SQ09) |

### 4.2 Watchlist (`screening.watchlist`)

| Table | Key fields | Notes |
|---|---|---|
| `scr_watchlist_source` | `code`, `name`, `list_type` (SANCTION, PEP, INTERNAL, ADVERSE_MEDIA), `transport` (FILE, API, MANUAL), `cron`, `file_layout`, `active` | Seeds `AML_ADVISORY`, `NLDS_PEP`, `INTERNAL` |
| `scr_watchlist_entry` | `source_id`, `external_ref`, `entity_type`, `primary_name`, `first_name`, `last_name`, `birth_date`, `nationality`, `id_numbers`, `listed_on`, `delisted_on`, `status` (DRAFT, PENDING, ACTIVE, INACTIVE), `effective_from`, `entry_version`, `remarks` | Unique (source, external_ref); screening reads ACTIVE only (SNSRP-203) |
| `scr_watchlist_alias` | `entry_id`, `alias_name`, `alias_type` (AKA, FKA, SPELLING) | |
| `scr_watchlist_change` | `entry_id`, `change_type` (ADD, UPDATE, DEACTIVATE), `before_values`, `after_values`, `maker_remarks`, `status` (PENDING, APPROVED, REJECTED), `decided_by/at`, `decision_remarks` | Insert-only after decision; SNSRP-203 / 204 |
| `scr_ingestion_run` | `source_id`, `trigger` (SCHEDULED, MANUAL_UPLOAD, API), `file_attachment_id`, `received`, `added`, `updated`, `delisted`, `failed`, `status` (RUNNING, SUCCESS, PARTIAL, FAILED), `error`, `job_run_id` | SNSRP-201 |
| `scr_ingestion_error` | `run_id`, `line_no`, `raw_record`, `reason` | SNSRP-202 |
| `scr_name_key` | `subject_kind` (CLIENT, ENTRY, ALIAS), `subject_id`, `key_type` (TOKEN, PHONETIC, EXACT), `key_value` | Index (key_type, key_value); blocking for matching. Client keys are rebuilt on `ClientRegistered` / `ClientIdentityChanged` |

### 4.3 Matching and risk (`screening.matching`, `screening.risk`)

| Table | Key fields | Notes |
|---|---|---|
| `scr_screening_run` | `trigger` (CLIENT_REGISTERED, CLIENT_CHANGED, ACCOUNT_SUBMITTED, LIST_CHANGE, PERIODIC, MANUAL), `scope`, `config_version_id`, `clients_screened`, `entries_screened`, `matches`, `cases_opened`, `status`, `started/ended_at`, `job_run_id` | SNSRP-602 "all activities logged" |
| `scr_match` | `run_id`, `client_id`, `entry_id`, `entry_version`, `score`, `algorithm`, `matched_fields`, `status` (POTENTIAL, TRUE_MATCH, FALSE_POSITIVE), `case_id`, `decided_by/at` | Unique (client, entry, entry_version, config version): idempotent |
| `scr_match_suppression` | `client_id`, `entry_id`, `entry_version`, `reason`, `evidence_case_id` | SNSRP-304 false positive; lifted when the entry version changes (SQ12) |
| `scr_client_risk_profile` | `client_id`, `category_code`, `kyc_risk_rating`, `tags`, `source` (RULE, MANUAL), `rule_id`, `match_id`, `justification`, `evidence_case_id`, `effective_at`, `by` | History, insert-only |

### 4.4 Cases and STR (`screening.cases`, `screening.str`)

| Table | Key fields | Notes |
|---|---|---|
| `scr_case` | `case_no` (`SCR-yyyy-nnnnnn`), `client_id`, `client_code`, `client_name`, `trigger`, `case_type` (NAME_MATCH, PEP, HIGH_RISK, EDD, MONITOR, ACCOUNT_APPLICATION), `risk_category`, `active_policy`, `marketing_unit`, `unit_head`, `stage`, `assignee`, `disposition`, `recommendation`, `str_required`, `due_at`, `reminded_at`, `breached`, `escalated_to`, `config_version_ids`, `work_case_id` | One open case per client and case type |
| `scr_case_event` | `case_id`, `event` (CREATED, ASSIGNED, REASSIGNED, SUBMITTED, VALIDATED, VALIDATION_FAILED, APPROVED, DISAPPROVED, RETURNED, ESCALATED, COMMITTEE_VOTE, CLOSED, REMINDER, BREACH, DOCUMENT_ADDED, RISK_TAG_CHANGED), `from_stage`, `to_stage`, `from_value`, `to_value`, `reason_code`, `remarks`, `actor`, `occurred_at` | Insert-only (trigger as in V26). Case timeline and SNSRP-903 |
| `scr_case_review` / `scr_case_answer` | review: `case_id`, `template_id` (version), `status` (DRAFT, SUBMITTED), `submitted_by/at`; answer: `field_code`, `value_text`, `value_number`, `value_date`, `attachment_id` | SNSRP-501 |
| `scr_case_document` | `case_id`, `attachment_id`, `form_type`, `document_type`, `date_received`, `source`, `sequence`, `nominated_name` | SNSRP-601 |
| `scr_committee_vote` | `case_id`, `member`, `decision` (APPROVE_STR, NO_STR, RETURN), `remarks`, `voted_at` | SNSRP-704 |
| `scr_str` | `case_id`, `str_no`, `template_id`, `subject_snapshot`, `narrative`, `reason_codes`, `status` (DRAFT, FOR_APPROVAL, APPROVED, EXTRACTED, FILED), `extraction_id`, `amlc_reference`, `filed_on` | SNSRP-705 |
| `scr_str_transaction` | `str_id`, `reference` (invoice / receipt / policy), `txn_date`, `amount`, `currency`, `type`, `description` | Prefilled from read services, editable |
| `scr_str_extraction` | `period_from/to`, `layout_id`, `count`, `report_run_id` / file, `status`, `extracted_by/at` | SNSRP-706 |

## 5. Accounting events and GL entries

None. Screening creates no `BusinessEvent`, no journal and no open item. No `acc_event_type` rows are added.

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in S0)

| Permission | Use | Area / action class (`sec_permission_action`) |
|---|---|---|
| `SCR_VIEW` | View cases, clients' screening tab, lists | SCREENING / VIEW |
| `SCR_CONFIG_MAINTAIN` | Draft and submit configuration versions (SNSRP-101-108) | SCREENING / CREATE, AMEND |
| `SCR_CONFIG_APPROVE` | Approve / reject configuration versions (SNSRP-109) | SCREENING / APPROVE |
| `SCR_LIST_MAINTAIN` | Add / change / deactivate list entries, upload list files (SNSRP-203) | SCREENING / CREATE, AMEND |
| `SCR_LIST_APPROVE` | Approve / reject list changes (SNSRP-204) | SCREENING / APPROVE |
| `SCR_INVESTIGATE` | Work investigation stage, reviews, documents, dispositions (SNSRP-501, 502, 601) | SCREENING / AMEND |
| `SCR_RISK_TAG` | Manual risk-tag update with justification (SNSRP-304) | SCREENING / AMEND |
| `SCR_CASE_ASSIGN` | Re-assign cases (SNSRP-404) | SCREENING / AMEND |
| `SCR_CASE_APPROVE` | Unit Head approval stage (SNSRP-702) | SCREENING / APPROVE |
| `SCR_COMPLIANCE_REVIEW` | BU escalation review, STR preparation (SNSRP-703, 705) | SCREENING / APPROVE |
| `SCR_COMMITTEE` | AML Committee vote (SNSRP-704) | SCREENING / APPROVE |
| `SCR_STR_EXTRACT` | STR extraction and filing reference (SNSRP-706) | SCREENING / CREATE |
| `SCR_REPORT_VIEW` | Compliance reports and export (SNSRP-901) | SCREENING / VIEW |
| `SCR_AUDIT_VIEW` | Screening audit log report (SNSRP-903) | SCREENING / VIEW |

### 6.2 Roles (V1050) and demo users (V1950, password `Brokerverse@2026`)

| Role | Persona (BRD) | Permissions | Demo user |
|---|---|---|---|
| `COMPLIANCE_OFFICER` (new) | Compliance Officer (maker; BU escalation; STR) | SCR_VIEW, SCR_CONFIG_MAINTAIN, SCR_LIST_MAINTAIN, SCR_COMPLIANCE_REVIEW, SCR_STR_EXTRACT, SCR_CASE_ASSIGN, SCR_REPORT_VIEW, SCR_AUDIT_VIEW, CLIENT_VIEW, ATTACHMENT_VIEW, REPORT_VIEW, WORK_VIEW | `compoff` |
| `COMPLIANCE_CHECKER` (new) | Compliance Officer (Checker) | SCR_VIEW, SCR_CONFIG_APPROVE, SCR_LIST_APPROVE, SCR_REPORT_VIEW, CLIENT_VIEW, REPORT_VIEW | `compchk` |
| `UNIT_COMPLIANCE_COORD` (new) | Unit Compliance (User) Coordinator | SCR_VIEW, SCR_CASE_ASSIGN, SCR_REPORT_VIEW, CLIENT_VIEW, WORK_VIEW, REPORT_VIEW | `ucc` |
| `SCR_INVESTIGATOR` (new) | Investigator | SCR_VIEW, SCR_INVESTIGATE, SCR_RISK_TAG, CLIENT_VIEW, ACCOUNT_VIEW, ATTACHMENT_VIEW, ATTACHMENT_MANAGE, WORK_VIEW | `investigator` |
| `SCR_APPROVER` (new) | Approver / Unit Head | SCR_VIEW, SCR_CASE_APPROVE, SCR_CASE_ASSIGN, CLIENT_VIEW, WORK_VIEW | `scrapprover` |
| `AML_COMMITTEE` (new) | AML Committee Member | SCR_VIEW, SCR_COMMITTEE, CLIENT_VIEW, ATTACHMENT_VIEW | `amlcom1`, `amlcom2` |
| `OPS_LEAD` or existing TL roles | Operations Lead (SLA monitoring, reports) | + SCR_VIEW, SCR_REPORT_VIEW (grant to the role BDOI names, SQ19) | - |
| `AUDITOR` (exists) | Auditor | + SCR_VIEW, SCR_REPORT_VIEW, SCR_AUDIT_VIEW | `auditor` |
| `SYSADMIN` (exists) | System Admin | + SCR_VIEW (read), jobs | `admin` |

The 287 investigators may be existing Marketing Account Officers (SQ19). If so, BDOI grants `SCR_INVESTIGATOR` in addition to `MKT_AO`; users may hold several roles (BRD-11 NFR 13.1 c-d).

## 7. Workflows

### `SCR_CASE`: screening case (seeded in V1050)

| Stage | Owner permission | SLA default (hours, overridden by `scr_sla_rule`) | Transitions |
|---|---|---|---|
| `NEW` (initial) | system | - | `route` (system) -> INVESTIGATION (assignee by `scr_assignment_rule`) |
| `INVESTIGATION` | SCR_INVESTIGATE | 72 | `submit` (validation SNSRP-701, disposition from `SCR_DISPOSITION` parent INVESTIGATION) -> UNIT_HEAD_APPROVAL (approver by `scr_approval_route`) or CLOSED (disposition "false positive - no approval needed", if a route says so); `request_info` -> self (comment) |
| `RETURNED` | SCR_INVESTIGATE | 24 | `resubmit` -> UNIT_HEAD_APPROVAL / COMPLIANCE_REVIEW (back to the stage that returned it) |
| `UNIT_HEAD_APPROVAL` | SCR_CASE_APPROVE | 24 | `approve` -> by route (COMPLIANCE_REVIEW or CLOSED); `disapprove` (rationale, `RETURN_REASON`) -> RETURNED |
| `COMPLIANCE_REVIEW` | SCR_COMPLIANCE_REVIEW | 24 | `escalate_committee` -> AML_COMMITTEE; `prepare_str` -> STR_PREPARATION; `close` -> CLOSED; `return_for_rework` (reason) -> RETURNED |
| `AML_COMMITTEE` | SCR_COMMITTEE | 72 | votes; `finalise` (system, rule `SCR_COMMITTEE_RULE`) -> STR_PREPARATION (approve STR) / CLOSED (no STR) / COMPLIANCE_REVIEW (return) |
| `STR_PREPARATION` | SCR_COMPLIANCE_REVIEW | 48 | `str_ready` -> STR_EXTRACTION |
| `STR_EXTRACTION` | SCR_STR_EXTRACT | 120 | `filed` (AMLC reference) -> CLOSED |
| `CLOSED` (terminal) | - | - | `reopen` (SCR_COMPLIANCE_REVIEW, reason) -> INVESTIGATION |

Stage entry mirrors `scr_case.stage` through `WorkCaseTransitioned` (the same pattern as crm and account). The previous owner loses edit rights on submit, because edit rights come from the stage owner and the assignee (SNSRP-401).

Configuration versions and list changes do **not** use a workflow. They use maker-checker (DRAFT -> PENDING -> ACTIVE / REJECTED) and appear in My Approvals through `ScreeningApprovalSource`.

## 8. Jobs, parameters, LOVs, alerts, notifications

| Job (`ManagedJob`) | Cron property (default) | Work |
|---|---|---|
| `SCR_WATCHLIST_INGEST` | `brokerverse.jobs.scr-watchlist-ingest-cron` (`0 0 1 * * *`, before business hours) | Pull each active source through its `WatchlistFeed` adapter; build the run log; changed entries -> delta screening |
| `SCR_PERIODIC_SCREENING` | `brokerverse.jobs.scr-periodic-screening-cron` (`0 30 1 * * *`) | Delta screening of in-scope clients against entries changed since the last run; the full rescreen is monthly (`SCR_FULL_RESCREEN_DAY`) |
| `SCR_SLA_MONITOR` | `brokerverse.jobs.scr-sla-monitor-cron` (`0 0 * * * *`, hourly) | Reminders, breach flags, escalation notices, missing-document reminders (SNSRP-405, 802) |
| `SCR_INGEST_ERROR_DIGEST` | `brokerverse.jobs.scr-ingest-error-digest-cron` (`0 0 7 * * MON-FRI`) | E-mail of failed ingestion records to the recipients (SNSRP-202) |

| Parameter (`sys_parameter`, category SCREENING) | Default | Use |
|---|---|---|
| `SCR_SCREENING_SCOPE` | `PROSPECT,CONFIRMED` (CODE_LIST) | Client statuses in the periodic run (SQ11) |
| `SCR_FULL_RESCREEN_DAY` | 1 | Day of month of the full rescreen |
| `SCR_BLOCK_ON_OPEN_MATCH` | false | Optional gate (SQ07) |
| `SCR_COMMITTEE_RULE` | MAJORITY | ANY / MAJORITY / ALL (SQ15) |
| `SCR_COMMITTEE_SIZE` | 5 | Members for MAJORITY / ALL (BRD: 5 members) |
| `SCR_INGEST_ALERT_RECIPIENTS` | empty (e-mail list) | SNSRP-202 recipients (SQ01) |
| `SCR_CASE_SEQUENCE_PREFIX` | `SCR` | Case number prefix |

LOV types (V1050; seeds "to confirm", SQ06):
- `SCR_DISPOSITION` (`parentCode` = stage). INVESTIGATION: FALSE_POSITIVE, TRUE_MATCH_REVIEW, POSSIBLE_MATCH_EDD, NEED_MORE_INFO. UNIT_HEAD_APPROVAL: CONCUR, NOT_CONCUR. COMPLIANCE_REVIEW: CLOSE_NO_ACTION, ESCALATE_COMMITTEE, FOR_STR, RETURN. AML_COMMITTEE: APPROVE_STR, NO_STR, RETURN.
- `SCR_CASE_TYPE`.
- `SCR_LIST_TYPE`.
- `SCR_REASSIGN_REASON`: WORKLOAD, ABSENCE, CONFLICT_OF_INTEREST, OTHERS.
- `SCR_STR_REASON` (empty until SQ09).
- `SCR_FORM_TYPE`: KYC_REVIEW, TRANSACTION_REVIEW, EDD, STR.
- `SCR_DOCUMENT_TYPE`, parent `DOCUMENT_TYPE` codes.

Alert codes (`alt_exception_code`, module SCREENING):
- `SCR_INGEST_FAILED` (a run FAILED or PARTIAL);
- `SCR_SLA_BREACH`;
- `SCR_NO_ACTIVE_CONFIG` (a trigger fired without an ACTIVE matching version);
- `SCR_OPEN_TRUE_MATCH_AGEING` (threshold days).

Notification events:
- `SCR_CASE_ASSIGNED`;
- `SCR_CASE_FOR_APPROVAL`;
- `SCR_CASE_RETURNED`;
- `SCR_COMMITTEE_REVIEW`;
- `SCR_SLA_REMINDER`;
- `SCR_SLA_ESCALATION`;
- `SCR_DOCUMENT_REMINDER`;
- `SCR_NO_POLICY_HIT` (UCC and Investigator for clients without an active policy, SNSRP-303);
- `SCR_CONFIG_TO_APPROVE`;
- `SCR_LIST_CHANGE_TO_APPROVE`.

## 9. Impact on built modules (contract changes)

| Module | Change | Contract | Owner / wave | Notes |
|---|---|---|---|---|
| `crm` | New service `crm.service.ClientRiskService` | `ClientRiskProfile applyRiskProfile(Long clientId, RiskProfileChange change)`, where `RiskProfileChange(String riskRating, Set<String> addTags, Set<String> removeTags, String source, String reason, String reference)`. It sets `Client.riskRating` (new domain method `Client.applyRiskRating(code)`, which does not touch the other profile fields). It adds or ends tags through `ClientNotesService` (history kept), recomputes `kycReviewDue` with `KycReviewPolicy.nextReview` when the rating goes up, and writes `AuditTrailService` UPDATE "Risk rating X -> Y (source, reference)". It validates the codes against `KYC_RISK_RATING` / `CLIENT_TAG` | crm owner, in S0 | Screening never writes crm tables |
| `crm` | Events | `crm.service.ClientRegistered(Long companyId, Long clientId, String code, ClientType type)`, published by `ClientService.create` / `createProspect` and the `CLIENT_CREATE` bulk handler on insert. `crm.service.ClientIdentityChanged(Long companyId, Long clientId)`, published by `ClientService.update` when the name, birth date, nationality, TIN or ID changes | crm owner, in S0 | Published inside the transaction; screening listens with `@TransactionalEventListener(AFTER_COMMIT)` and runs asynchronously |
| `crm` (frontend) | Client page tab | `ClientDetailPage` gets a "Screening" tab that renders `features/screening/ClientScreeningTab` (risk profile history, matches, cases). The tab shows only with `SCR_VIEW` | crm frontend owner with S1-C | Import from `features/screening`; no backend change |
| `workflow` | Due-time override | `WorkflowService.overrideDue(Long caseId, Instant dueAt, String reason)`: sets `WorkCase.dueAt` and writes a history row (action `sla_override`) | S0 | Needed because the SLA matrix is configurable and dated (SNSRP-108); `wf_stage.sla_hours` stays the default |
| `workflow` | Re-assignment reason | Overload `WorkAssignmentService.assign(Long caseId, String assignee, List<String> eligibleUsers, String reasonCode, String comment)`; the history row stores `reason_code` / `comment` (columns exist in `WorkCaseHistory`) | S0 | The existing signature delegates with nulls; no behaviour change for other modules |
| `attachment` | Named naming patterns | `DocumentNamingService.nominate(NamingPattern pattern, NamingFacts facts)`, with `NamingPattern.DEFAULT` (today's `<REFERENCE>_<DOCTYPE>_<n>`) and `NamingPattern.SCREENING` (`<FORM_TYPE>_<CLIENT_NAME>_<DATE_RECEIVED yyyyMMdd>_<DOCTYPE>_<n>`) | S0 | The existing method keeps its signature |
| `report` | Category | `ReportCategory.COMPLIANCE("Compliance")`; `ReportMetadata.compliance(code, name)` with view `SCR_REPORT_VIEW` and export `SCR_REPORT_VIEW` | S0 | Additive |
| `security` | Permissions | 14 constants in `Permission.java` (section 6.1) | S0 | Additive |
| `nbadmin` | Retention | Rows `SCREENING_CASE` (CLOSED) and `WATCHLIST_ENTRY` (INACTIVE) in `nba_retention_rule` (5 / 5 years, action REVIEW); `screening` implements `RetentionCandidateProvider` | S1-C | Additive; archive / purge stays parked |
| `account` | None | Screening listens to `WorkCaseTransitioned` (workflow `NB_ACCOUNT`, to-stage `SUBMITTED`) and reads the account's client and sales stamp through the existing account read service | - | No change; if a dedicated event is preferred later, account adds `AccountSubmitted` |
| `catalog` | Uses the unit head contract of COLLECTIONS_DESIGN section 9 | `SalesOrganisationService.unitHead(companyId, unitCode)` (added by Collections C1-A) resolves the approver when the route says UNIT_HEAD | - | If it is not merged when S1-C starts, the route uses ROLE `SCR_APPROVER` + marketing unit filter |
| `approval` | Source | `screening.service.ScreeningApprovalSource` implements `PendingApprovalSource` (config versions, list changes, cases at UNIT_HEAD_APPROVAL for the chosen approver) | S1-A / S1-C | Additive |
| Operations, Collections, Accounting modules (`opsledger`, `cashiering`, `remittance`, `adjustment`, `prodrecon`, `commission`, `collections`, `disbursement`, `payrequest`, `acsl`, `frbs`), GL (`journal`, `accounting`), `catalog` / `productmaint`, NB modules other than crm | None | - | - | STR transaction details are read through existing read services only (invoice / receipt lookups); no contract change |

Optional gate (only if SQ07 says "block"): the port `crm.service.ClientComplianceGate` (`Optional<String> blockReason(Long clientId)`) is declared in crm and implemented by screening. `AccountService.submit` and placement call it when `SCR_BLOCK_ON_OPEN_MATCH` is true. This is **not** built until BDOI answers.

## 10. Integrations to park (seam only)

| Item | Question | Seam |
|---|---|---|
| Sanctions list source ("AML" advisory; UN / AMLC / OFAC lists) | SQ01 | Port `WatchlistFeed` (`fetch(source) -> WatchlistBatch`); default `FileDropWatchlistFeed` reads the uploaded file (bulk handler `SCR_WATCHLIST`, CSV / XLSX template); API adapters later |
| NLDS (BDO Negative List Database System) PEP validation | SQ01, Q16 | Same port, source `NLDS_PEP`, transport FILE; no direct NLDS query |
| AMLC portal (STR filing) | out of scope (p.8 "Via Portal") | The filing reference is recorded by hand |
| AMLC STR file format | SQ09 | `scr_str_layout` configurable; seed a placeholder layout with the case fields; the real layout comes in V1056 when supplied |
| Designated STR folder path | SQ16 | Port `StrFileSink` (`deliver(file, name)`); default = report archive (download); shared-folder / SFTP adapter later |
| BDO CIF / bank-side screening results | Q16, SQ11 | Parameter `SCR_SCREENING_SCOPE`; a bank-client exemption rule can be added as a risk rule on `bankClient` |
| Adverse media | SQ20 | List type ADVERSE_MEDIA with MANUAL transport only |
| Active policy definition | SQ10 | Port `ActivePolicyQuery` (`boolean hasActivePolicy(Long clientId, LocalDate asOf)`); default reads `ClientRecordsProvider` records of kind Account with status POLICY_ISSUED / BOOKED; booking may implement it later with policy periods |

## 11. Reports and screens

### 11.1 Reports (`screening.report`, category Compliance)

`SCR-HIGH-RISK-CLIENTS`, `SCR-CASE-STATUS`, `SCR-SLA-BREACHES`, `SCR-SANCTIONED-NAMES`, `SCR-PEP-CLIENTS`, `SCR-INGEST-ERRORS`, `SCR-STR-REGISTER`, `SCR-AUDIT-LOG`. Parameters and sources are in spec section 7. Heavy queries are SQL aggregates over `scr_case`, `scr_match` and `scr_client_risk_profile`, with PDF / XLSX / CSV export by the report framework (SNSRP-901, 903).

### 11.2 Screens

The navigation follows the BDOI groups of `docs/design/BDO_UX_GUIDELINES.md` section 3. The screen patterns and shared components are those of section 7 there.

**Group Client & Policy, new section "Sanction Screening"**, after Client Management (`features/screening/module.ts`, id `screening`, home `/screening`, permission SCR_VIEW):

| Screen | Route | Permission | Pattern / BRD |
|---|---|---|---|
| Screening home | `/screening` | SCR_VIEW | Kpi tiles: open cases per stage, SLA due today / breached, potential matches not yet cased, last list run status (SNSRP-402, 405) |
| Cases | `/screening/cases` | SCR_VIEW | Work list: status tabs (My Cases, Team, For Approval, Committee, STR, Closed), `WorklistToolbar` search (case no., client), filters (date created, marketing unit, unit head, case type, risk category, disposition, assignee, SLA), bulk re-assign (SNSRP-402-404) |
| Case | `/screening/cases/:id` | SCR_VIEW | Record details: `RecordSummary` (case no., client, status pill, risk flags, SLA badge). Tabs: Matches, Review (template form), Documents (`Attachments` + metadata dialog), Decisions (approvals, committee votes), STR, Timeline (`scr_case_event`). Actions by permission and stage via `WorkflowPanel` (SNSRP-401, 501-502, 601, 701-705) |
| Matches | `/screening/matches` | SCR_VIEW | Potential matches not yet decided, with score and fields; "Open case" / "Mark false positive" (SNSRP-301, 304) |
| High-risk clients | `/screening/high-risk` | SCR_VIEW | List + export of high-risk and PEP clients (p.8 report 1) |
| STR | `/screening/str` | SCR_COMPLIANCE_REVIEW | STR register and extraction dialog ("Extract Approved STRs" with period), filing reference (SNSRP-705, 706) |

**Group Setup & Administration, new section "Compliance Setup"** (`features/screening/setupModule.ts`, id `screening-setup`):

| Screen | Route | Permission | BRD |
|---|---|---|---|
| Configuration versions | `/screening-setup/config` | SCR_CONFIG_MAINTAIN / SCR_CONFIG_APPROVE (`alsoPermissions`) | Tabs per config type; a draft editor per type; submit; diff view; approve / reject (SNSRP-101-109) |
| Templates | `/screening-setup/templates` | SCR_CONFIG_MAINTAIN | Field designer (sections, fields, mandatory, LOV), preview; versions (SNSRP-104, 105) |
| Watchlist | `/screening-setup/watchlist` | SCR_VIEW | Entries by source; add / change / deactivate (maker); pending changes with before / after (checker) (SNSRP-203, 204) |
| List sources and runs | `/screening-setup/sources` | SCR_LIST_MAINTAIN | Sources, schedule, "Upload list file", run log with counts and errors (SNSRP-201, 202) |
| Dispositions | Lists of Values `?type=SCR_DISPOSITION` | LOV_MANAGE | SNSRP-107 (existing screen) |

Reports appear in the Reports group through the report catalogue (category Compliance).

Help: `features/screening/help.ts` (`SCREENING_HELP`), one entry per non-hidden route, registered in `HELP_SECTIONS` (Developer Guide 10.7).

## 12. NFR design notes

- **Performance.** Matching uses blocking keys (`scr_name_key`, indexed) before scoring. A daily delta covers 10-300 entries against the client base. The full monthly rescreen runs in the batch window (00:00-04:00 is the maintenance window, so jobs run from 01:00 with a guard against 00:00-01:00). Screens meet the 3-5 s target through paged server queries.
- **Retention.** 5 years online and 5 years archive for cases, documents and list entries (retention rules, section 9). `scr_case_event` and `scr_watchlist_change` are insert-only.
- **Security.** Four eyes on configuration and list changes. The maker is excluded from approval in both the service and the inbox source.
- **Name data.** Watchlist data is loaded only from BDOI-supplied files. The demo data uses invented names.

## 13. Build-wave plan

Prerequisites: none beyond the built crm, workflow, lov, attachment and report modules. The unit-head lookup of Collections C1-A is used when present (section 9).

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **S0** (1 agent, short; can be the same agent as U0 of BRD-11, see note) | Screening foundation | Permissions, V1050, the crm / workflow / attachment / report contract changes of section 9, nav registration (stub home), `help.ts`, crons in `application.yml`, Developer Guide range row | `security/domain/Permission.java`, `crm/service/ClientRiskService.java` (new), `crm/service/ClientRegistered.java`, `ClientIdentityChanged.java` (new), `crm/service/ClientService.java` (publish only), `crm/service/ClientBulkHandler.java` (publish only), `crm/domain/Client.java` (`applyRiskRating` only), `workflow/service/WorkflowService.java` (`overrideDue`), `workflow/service/WorkAssignmentService.java` (overload), `attachment/service/DocumentNamingService.java`, `report/core/ReportCategory.java`, `report/core/ReportMetadata.java`, `V1050`, `navigation/modules.ts`, `help/helpContent.ts`, `features/screening/module.ts`, `setupModule.ts`, `help.ts`, `application.yml`, `docs/operations/CONFIGURATION.md` | `mvn verify` green; existing crm / workflow tests unchanged |
| **S1-A** | Configuration and watchlist | `screening.config`, `screening.watchlist`, approval source (config + list parts), bulk handler, ingestion jobs, setup screens, demo V1950 | `screening/config/**`, `screening/watchlist/**`, `V1051`, `V1052`, `db/demo/V1950`, `features/screening/setup/**` | Draft -> approve -> active with diff; file upload -> run log -> entries PENDING -> approved -> ACTIVE |
| **S1-B** | Matching and risk | `screening.matching`, `screening.risk`, event listeners, periodic job, matches screen, demo V1951 | `screening/matching/**`, `screening/risk/**`, `V1053`, `db/demo/V1951`, `features/screening/matches/**` | New demo client with a listed name -> run -> match -> risk tag on the client (rating, tag, review date) |
| **S1-C** | Cases, STR, reports | `screening.cases`, `screening.str`, `screening.report`, SLA monitor, case screens, crm client tab, retention provider, demo V1952 | `screening/cases/**`, `screening/str/**`, `screening/report/**`, `V1054`, `V1055`, `db/demo/V1952`, `features/screening/cases/**`, `features/screening/str/**`, `features/crm/ClientDetailPage.tsx` (tab registration only) | Case -> review -> submit -> unit head -> compliance -> committee -> STR -> extraction -> filed; SLA reminder and breach |
| **S2** | Integration and hardening | E2E and `ApiSmokeIT` entries, module guide `docs/modules/SCREENING.md`, fit/gap refresh | tests + docs | Full `mvn verify` / `npm run verify` |

S1-B starts on S1-A's `ActiveConfig` interface (committed first in S1-A as a stub returning seeded defaults). S1-C starts on the S1-B `ScreeningEngine` result types (the same approach).

Rules for parallel work:
- One Flyway file set per agent: S0 V1050; S1-A V1051 / V1052 / V1950; S1-B V1053 / V1951; S1-C V1054 / V1055 / V1952.
- `screening.common` belongs to S1-A. S1-B and S1-C ask S1-A for changes.
- Shared files (the Permission enum, nav, help registry, `application.yml`, `ReportCategory`) are edited **only in S0**.
- No agent edits `crm/**`, `workflow/**` or `attachment/**` outside S0. The single exception is the crm client-page tab registration in S1-C, coordinated with the crm owner.
- Each agent writes its own `*ApiIT` smoke class.
- Note: BRD-11 (User Access Maintenance) also edits `Permission.java`, the nav and the help registry in its foundation wave U0. Run S0 and U0 as **one foundation agent**, or run them one after the other; never at the same time.

## 14. What depends on information BDOI has not given (build the seam, park the content)

| Item | Question | Built now | Parked |
|---|---|---|---|
| Thresholds, risk categories, matrices, SLA | SQ02-SQ04, SQ08 | Configurable tables and screens; demo values only | Production values entered by Compliance at go-live |
| Templates and dispositions | SQ05, SQ06 | Template designer; LOV seeds marked "to confirm" | Field lists |
| STR layout | SQ09 | Configurable layout, placeholder | AMLC format |
| List transports | SQ01 | File upload and file-drop job | API / NLDS connectors |
| Blocking gate | SQ07 | Parameter, off | Gate calls in account / placement |

## 15. Risks

1. **Matching quality.** Too many false positives on common Filipino names (e.g. "Santos", "Reyes"). Mitigation: blocking on several tokens plus birth date where available, thresholds per list type, false-positive suppression per entry version, and the "potential matches" screen before a case is opened (a threshold `min_score_for_case`).
2. **Unknown list formats.** Mitigation: a configurable file layout per source and a manual maker-checker entry path.
3. **Contract changes in crm and workflow.** Mitigation: small, additive, done in one S0 change set with tests; existing signatures stay.
4. **Regulatory content in the demo.** Mitigation: invented names only, flagged in the demo migration header.
5. **Investigator population.** 287 users may be MKT_AO holders. Mitigation: role grants are additive and the case list is scoped by team.

## 16. S0 foundation: as built

What the S0 wave built (together with U0 of BRD-11, one foundation agent, because both edit the shared files), and
where it differs from or details the sections above. S1-A, S1-B and S1-C build on this and compile only against it.

- **Migration `V1050__screening_foundation.sql`.**
  - Roles `COMPLIANCE_OFFICER`, `COMPLIANCE_CHECKER`, `UNIT_COMPLIANCE_COORD`, `SCR_INVESTIGATOR`, `SCR_APPROVER`,
    `AML_COMMITTEE` with the grants of section 6.2; `AUDITOR` + SCR_VIEW, SCR_REPORT_VIEW, SCR_AUDIT_VIEW; `SYSADMIN` +
    SCR_VIEW. The Operations Lead grant waits for SQ19 (no role is named yet). Demo users come with V1950 (S1-A).
  - `sec_permission_action`: the 14 `SCR_*` permissions in the area `SCREENING` with the action classes of 6.1.
  - LOV types `SCR_DISPOSITION` (parent = stage), `SCR_CASE_TYPE`, `SCR_LIST_TYPE`, `SCR_REASSIGN_REASON`,
    `SCR_STR_REASON` (no values until SQ09), `SCR_FORM_TYPE`, `SCR_DOCUMENT_TYPE` (parent = `DOCUMENT_TYPE` code).
    LOV codes are unique per type, so the committee "return" disposition is `COMMITTEE_RETURN` (parent
    `AML_COMMITTEE`); `RETURN` stays the Compliance review one. All dispositions are marked "(to confirm)" (SQ06).
  - Parameters of section 8 (category SCREENING). `SCR_INGEST_ALERT_RECIPIENTS` is a STRING (comma separated e-mail
    addresses; CODE_LIST does not accept addresses). `SCR_BLOCK_ON_OPEN_MATCH` = false.
  - Alert codes and notification events of section 8 (event sort orders 500-590).
  - Workflow `SCR_CASE` of section 7. Action codes (the unique key is stage + action):
    `NEW.route`; `INVESTIGATION.submit | close_no_approval | request_info` (self); `RETURNED.resubmit` (to
    UNIT_HEAD_APPROVAL) and `RETURNED.resubmit_to_compliance` (to COMPLIANCE_REVIEW); `UNIT_HEAD_APPROVAL.approve` (to
    COMPLIANCE_REVIEW), `approve_close` (to CLOSED), `disapprove` (RETURN_REASON); `COMPLIANCE_REVIEW.escalate_committee
    | prepare_str | close | return_for_rework` (RETURN_REASON); `AML_COMMITTEE.finalise_str | finalise_no_str |
    finalise_return`; `STR_PREPARATION.str_ready`; `STR_EXTRACTION.filed`; `CLOSED.reopen` (RETURN_REASON). No action
    is generic: every stage change goes through the case service (S1-C), which picks the action from the route.
  - Retention rules `SCREENING_CASE` (CLOSED) and `WATCHLIST_ENTRY` (INACTIVE), 5 / 5 years, action REVIEW; the review
    run shows "no provider" until S1-C implements `RetentionCandidateProvider`.
- **Permissions.** 14 constants `SCR_*` in `Permission.java` (section 6.1).
- **crm contract (section 9).**
  - `crm.service.ClientRiskService.applyRiskProfile(Long clientId, RiskProfileChange change)` returns
    `crm.service.ClientRiskProfile` (clientId, clientCode, previousRating, riskRating, activeTags, tagsAdded,
    tagsRemoved, kycReviewDue; `changed()`). `RiskProfileChange(riskRating, addTags, removeTags, source, reason,
    reference)`: source `ClientRiskService.SOURCE_RULE` ("RULE") or `SOURCE_MANUAL` ("MANUAL", reason mandatory,
    `RISK_JUSTIFICATION_REQUIRED`); the rating is validated against `KYC_RISK_RATING`; a null rating keeps it. Tags go
    through `ClientNotesService` (history and banner as for a manual tag); adding an active tag or ending an absent one
    is ignored. When the rating becomes HIGH and the client has a review cycle (verified KYC), `kycReviewDue` moves to
    the earlier of the current date and `KycReviewPolicy.nextReview(HIGH, verifiedOn)`; it never moves later. Audited
    "Risk rating X -> Y; tags added [...] (source, reference: reason)". `ClientRiskService.activeTags(clientId)` reads
    the active tags. **Naming:** S1-B names its history entity differently (e.g. `RiskProfileEntry` on
    `scr_client_risk_profile`) to avoid confusion with the crm record.
  - `Client.applyRiskRating(String code, LocalDate reviewDue)` (domain; refuses an inactive client).
  - Events `crm.service.ClientRegistered(companyId, clientId, code, type)` from `ClientService.create` /
    `createProspect` (the `CLIENT_CREATE` bulk handler, quotation and account intake all create through it, so no
    change was needed in `ClientBulkHandler`) and `crm.service.ClientIdentityChanged(companyId, clientId)` from
    `ClientService.update` when last / first / middle / corporate name, birth date, nationality, TIN, ID type or ID
    number changes. Both are published inside the transaction; screening listens with
    `@TransactionalEventListener(AFTER_COMMIT)`.
- **workflow contract.** `WorkflowService.overrideDue(Long caseId, Instant dueAt, String reason)` (null clears the SLA;
  refused on a closed case; history action `WorkflowService.SLA_OVERRIDE` = "sla_override", from = to = current stage,
  comment = reason; audited). `WorkAssignmentService.assign(Long caseId, String assignee, List<String> eligibleUsers,
  String reasonCode, String comment)`: with a reason or comment it writes a history row (action
  `WorkAssignmentService.REASSIGN` = "reassign", reason code, comment "previous -> assignee: comment"); the
  three-argument form delegates with nulls and writes no history row (unchanged behaviour). The caller validates the
  reason against `SCR_REASSIGN_REASON`.
- **attachment contract.** `DocumentNamingService.nominate(NamingPattern pattern, NamingFacts facts)`;
  `NamingPattern.DEFAULT` (the existing syntax, unchanged) and `NamingPattern.SCREENING`
  (`<FORM_TYPE>_<CLIENT_NAME>_<yyyyMMdd>_<DOCTYPE>_<n>.<ext>`, e.g. `KYC-REVIEW_DELA-CRUZ-JUAN_20260915_VALID-ID_1.jpg`;
  no date gives `NODATE`); `NamingFacts(reference, formType, clientName, dateReceived, documentType, sequence,
  originalName)` and `NamingFacts.of(reference, documentType, sequence, originalName)`. The existing four-argument
  method is unchanged. The upload options of `DocumentService` still use DEFAULT: S1-C calls `nominate(SCREENING, ...)`
  itself and stores the result in `scr_case_document.nominated_name`.
- **report contract.** `ReportCategory.COMPLIANCE("Compliance")`; `ReportMetadata.compliance(code, title,
  description, parameters)` (view and export SCR_REPORT_VIEW, archived).
- **Navigation.** Group Client & Policy, section **Sanction Screening** after Client Management
  (`features/screening/module.ts`, module id `screening`, help id `screening`): `/screening` "Screening Home"
  (SCR_VIEW), a landing screen until S1-C adds the tiles. Group Setup & Administration, section **Compliance Setup**
  after Broking Setup (`features/screening/setupModule.ts`, module id `screening-setup`, help id `screening-setup`):
  `/screening-setup/config` "Configuration Versions" (SCR_CONFIG_MAINTAIN, also SCR_CONFIG_APPROVE,
  SCR_LIST_MAINTAIN, SCR_LIST_APPROVE), a landing screen until S1-A adds the editor. Both help sections are in
  `features/screening/help.ts` (`SCREENING_HELP`, `SCREENING_SETUP_HELP`), registered in `HELP_SECTIONS` in sidebar
  order. S1-A / S1-B / S1-C add their routes to these two modules and their help entries to these two sections.
- **Jobs.** Crons in `application.yml` and `docs/operations/CONFIGURATION.md`, in UTC for the PHT times of section 8:
  `brokerverse.jobs.scr-watchlist-ingest-cron` `0 0 17 * * *` (01:00 PHT), `scr-periodic-screening-cron`
  `0 30 17 * * *` (01:30 PHT), `scr-sla-monitor-cron` `0 0 * * * *`, `scr-ingest-error-digest-cron`
  `0 0 23 * * SUN-THU` (07:00 PHT Monday to Friday). Each job reads its cron with
  `@Value("${brokerverse.jobs.<property>:-}")`.
- **Flyway left to the build waves.** As section 3: S1-A V1051, V1052, demo V1950; S1-B V1053, demo V1951; S1-C V1054,
  V1055, demo V1952; V1056-V1059 and V1953-V1959 free.
- **Parked in S0.** The Operations Lead grants (SQ19), the committee rule and size values (SQ15), the disposition list
  (SQ06), the STR reasons (SQ09) and the ingestion alert recipients (SQ01) are placeholders, as listed in section 14.
  The optional gate `ClientComplianceGate` is not built (SQ07).

## 17. S1-A configuration and watchlist: as built

What wave S1-A built on the S0 foundation, and where it details or differs from sections 3-11. S1-B and S1-C build on the
contracts listed here.

- **Packages.** `screening.config` (`domain`, `service`, `api`), `screening.watchlist` (`domain`, `service`, `api`),
  `screening.common` (`service.ScreeningPermissions`: permission names and `@PreAuthorize` expressions;
  `api.DecisionRequest`: checker remarks). `screening/package-info.java` documents the ownership of the sub-packages.
- **Migrations.** `V1051__screening_config.sql` (the twelve tables of 4.1), `V1052__screening_watchlist.sql` (the seven
  tables of 4.2, the insert-only trigger of `scr_watchlist_change` once decided, the three BRD sources `AML_ADVISORY`
  (SANCTION, file, full file), `NLDS_PEP` (PEP, file, delta file) and `INTERNAL` (manual)), demo
  `V1950__demo_screening_users_config.sql` (users compoff, compchk, compdual (maker and checker, for the four-eyes
  refusals), ucc, investigator, investigator2, scrapprover, amlcom1, amlcom2; version 1 of every configuration type for
  company FVI, ACTIVE since 2026-01-01; one PENDING SLA_MATRIX v2 made by compoff).
- **Details of 4.1.** `scr_config_version.scope` holds the template type of a TEMPLATE version (templates are versioned
  per type); it is '' for the other types. `change_note` and `base_version_id` (the version compared with) are added;
  `diff` stores the change lines as JSON (`item`, `attribute`, `before`, `after`). Partial unique indexes enforce one
  DRAFT or PENDING version per type and scope and one ACTIVE version per type, scope and effective date. Rule tables use
  `sort_order`, `rule_values`, `trigger_code`, `user_name` and `rule_kind` for the reserved words of 4.1.
- **Life cycle.** New Draft opens the existing draft or copies the latest approved version; a PENDING version blocks a
  new draft. Save validates the rows (FRS messages of FR-SS-011 to 017). Submit refuses an effective date before today,
  an empty draft and a draft identical to the version in force, and stores the difference. Approve makes the version
  ACTIVE from its effective date (a past date moves to today); `ConfigDecisionService.supersedeDue` marks the previous
  version SUPERSEDED once the new one is in force (on approval and whenever the version list is read). Reject needs a
  reason; Discard keeps the draft as REJECTED "withdrawn by maker". The maker (creator or submitter) never decides.
- **Watchlists are platform reference data** (no `company_id`): the list is the same for every company.
- **Ingestion (4.2, SNSRP-201/202).** One run per file (`WLR-yyyy-nnnnnn`). The SCR_WATCHLIST template columns are
  Reference, Entity Type, Primary Name, First Name, Last Name, Aliases (semicolons), Birth Date, Nationality, ID Numbers,
  Listed On, List Type (blank = the source's) and Remarks. A reference already on the list is updated or left unchanged,
  never duplicated; a full-file source delists the ACTIVE entries missing from the file. **A scheduled run of the official
  feed applies its changes at once** (changes recorded APPROVED by SYSTEM, FR-SS-020); **a file uploaded by a Compliance
  Officer stages them as PENDING changes** (new entries PENDING) that a checker approves one by one or all at once from
  the run (the exit criterion "file upload -> run log -> entries PENDING -> approved -> ACTIVE"). A FAILED or PARTIAL run
  raises `SCR_INGEST_FAILED` and notifies the SCR_LIST_MAINTAIN holders.
- **Transport seam (section 10).** Port `watchlist.service.WatchlistFeed` (`pending(source)`); the default
  `StagedFileWatchlistFeed` reads the files staged on the source (attachment entity type `ScreeningListSource`, staged
  with `POST /screening/watchlist/sources/{code}/stage` or the attachments API) that no run has read. An active FILE source
  without a new file gets a FAILED run "No list file was received". SFTP, e-mail advisory and API / NLDS adapters are
  parked (SQ01).
- **Jobs.** `SCR_WATCHLIST_INGEST` (`WatchlistIngestJob`, one transaction per file) and `SCR_INGEST_ERROR_DIGEST`
  (`IngestErrorDigestJob`: the failed records not yet sent, e-mailed with a CSV attachment to the valid addresses of
  `SCR_INGEST_ALERT_RECIPIENTS`; no record = no e-mail; no valid recipient = alert "no recipients"). Crons of S0.
- **Approval inbox.** `ConfigApprovalSource` (versions, SCR_CONFIG_APPROVE) and `WatchlistApprovalSource` (list
  changes, SCR_LIST_APPROVE) implement the configuration and list parts of `ScreeningApprovalSource`; S1-C adds the case
  part as its own source.
- **API.** `/api/v1/screening/config`: `GET versions?companyId&type`, `GET versions/{id}` (rows and changes),
  `GET versions/{id}/changes`, `POST drafts`, `PUT versions/{id}`, `POST versions/{id}/submit|withdraw|approve|reject`.
  `/api/v1/screening/watchlist`: `GET|POST entries`, `GET|PUT entries/{id}`, `POST entries/{id}/deactivate`,
  `GET changes?status`, `GET changes/{id}`, `POST changes/{id}/approve|reject`, `GET sources`, `PUT sources/{code}`,
  `GET template`, `POST sources/{code}/upload|stage`, `GET runs?source`, `GET runs/{id}`, `POST runs/{id}/approve`.
- **Screens** (`features/screening/setup`, routes in `setupModule.ts`, help in `SCREENING_SETUP_HELP`):
  `/screening-setup/config` Configuration Versions, `/screening-setup/templates` Templates (field designer and preview),
  `/screening-setup/watchlist` Watchlist, `/screening-setup/sources` List Sources and Runs.

### 17.1 Contracts for S1-B and S1-C

- `screening.config.service.ActiveConfig` (implemented by `VersionedActiveConfig`): `activeVersion(companyId, type,
  scope, asOf)`; by version id `matchCriteria`, `riskRules`, `approvalMatrix`, `assignmentMatrix`, `slaMatrix`,
  `validationRules`, `template`, `strLayout`; and the same "in force on a date" as default methods taking
  `(companyId, asOf)` (`template(companyId, TemplateType, asOf)`). Snapshot records: `ConfigVersionRef`, `MatchCriteria`
  (`rulesFor(listType, subjectType)`), `RiskRules` (rules sorted by priority, `category(code)`, `Rule.test(values)`),
  `ApprovalMatrix` (`from(stage)`), `AssignmentMatrix`, `SlaMatrix` (`ruleFor(stage, caseType, riskCategory)`, most
  specific row), `ValidationRules` (`rulesFor(stage, caseType)`), `ReviewTemplate`, `StrLayout`. Empty = no version in
  force: the caller raises `SCR_NO_ACTIVE_CONFIG`.
- `screening.watchlist.service.WatchlistDirectory`: `active(listTypes)`, `entries(ids)`, `entry(id)` returning
  `ListedEntry` (names, aliases, birth date, nationality, IDs, `entryVersion`, status).
- Event `screening.watchlist.service.WatchlistEntriesChanged(entryIds, cause)`, published inside the transaction that
  applied approved or feed changes; S1-B rebuilds the name keys of the entries and runs the delta screening with
  `@TransactionalEventListener(AFTER_COMMIT)`. S1-A never writes `scr_name_key`.
- The sources are seeded by V1052: V1951 inserts entries only (or uses `on conflict do nothing` for sources).

### 17.2 Parked or deferred in S1-A

- Risk rules test one attribute each (design 4.1); a combination such as "SANCTION and TRUE_MATCH" is expressed by the
  evaluation point (S1-B evaluates the rules on the match status it decides) until BDOI confirms the categories (SQ03).
- List transports other than upload / staged file (SQ01); the AMLC STR layout (SQ09, placeholder layout in V1950).
- The notice to Compliance of each run with new entries (FR-SS-020 notifications) is the approval notice of uploaded
  runs only; a notice for applied feed runs waits for the recipients of SQ01.
- The validation of the e-mail addresses when SCR_INGEST_ALERT_RECIPIENTS is saved belongs to the parameter screen
  (platform); the digest skips invalid addresses.
