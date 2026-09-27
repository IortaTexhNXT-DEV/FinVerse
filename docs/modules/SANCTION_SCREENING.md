# Sanction Screening (BRD-10)

Guide of the Sanction Screening module of iNXT BrokerVerse (`screening`): screening configuration,
watchlists, name matching, risk profiling, screening cases, suspicious transaction reports (STR)
and the compliance reports. The build design is
[`docs/architecture/SANCTION_SCREENING_DESIGN.md`](../architecture/SANCTION_SCREENING_DESIGN.md)
(sections 16 to 19 record what each wave built); the requirements and their as-built status per
SNSRP ID are in [`docs/requirements/BDOI_SANC_BRD_SPEC.md`](../requirements/BDOI_SANC_BRD_SPEC.md);
the functional specification is
[`FRS_BRD10_SANCTION_SCREENING.md`](../deliverables/src/frs/FRS_BRD10_SANCTION_SCREENING.md).

## 1. Purpose

BDOI must screen every client against sanction lists, politically exposed persons (PEP) and its
own internal list (RA 9160 AMLA, BSP Circular 1182, BSP CL-2023-030). Before BrokerVerse this was a
manual search on spreadsheets and e-mail. The module:

- keeps the screening rules as dated **configuration versions** under maker-checker (matching
  criteria, risk categories and rules, approval / assignment / SLA matrices, validation rules,
  review and STR templates, STR layout);
- keeps the **watchlists** (AML advisory sanctions, NLDS PEP, internal list) up to date from list
  files, with every change approved by a second Compliance Officer;
- **screens** a client when it is registered, when its identity changes, when an account
  application is submitted and whenever the list changes, plus a daily delta and a monthly full
  rescreen;
- turns a hit into a **match**, a **risk profile** on the client (rating, tags, KYC review date)
  and a **case** that runs investigation -> unit head -> Compliance -> AML Committee -> STR ->
  extraction -> filing, with SLA reminders and escalation;
- answers the regulator and the auditors with eight **compliance reports** and a complete audit
  trail.

Screening creates no accounting entry. Filing the STR on the AMLC portal stays manual (the BRD
"Via Portal" lane); BrokerVerse prepares, approves and extracts the STR and records the AMLC
reference.

## 2. Personas and roles

| Persona (BRD) | Role | SIT/UAT user(s) | What they do |
|---|---|---|---|
| Compliance Officer (maker) | `COMPLIANCE_OFFICER` | `compoff` | Drafts configuration versions, maintains the watchlists and uploads list files, reviews escalated cases (close, return, refer to the committee, require an STR), prepares, extracts and files STRs, re-assigns cases, runs the reports and the audit log |
| Compliance Officer (checker) | `COMPLIANCE_CHECKER` | `compchk` | Approves or rejects configuration versions and list changes (never their own) |
| Unit Compliance Coordinator | `UNIT_COMPLIANCE_COORD` | `ucc` | Watches the queues and SLA, re-assigns cases with a reason |
| Investigator | `SCR_INVESTIGATOR` | `investigator`, `investigator2` | Works the investigation: review template, KYC documents, confirm or clear matches, manual risk tag, disposition and submission |
| Approver / Unit Head | `SCR_APPROVER` | `scrapprover` | Concurs or does not concur with the investigator (never on their own investigation) |
| AML Committee member | `AML_COMMITTEE` | `amlcom1` to `amlcom5` | Votes on escalated cases (APPROVE_STR, NO_STR, COMMITTEE_RETURN) |
| Auditor | `AUDITOR` (+ SCR_VIEW, SCR_REPORT_VIEW, SCR_AUDIT_VIEW) | `auditor` | Reads cases, the STR register and the audit log |
| System Administrator | `SYSADMIN` (+ SCR_VIEW) | `admin` | Runs the jobs from the job monitor |

Four-eyes SIT/UAT users: `compdual` (maker and checker, to show the refusal of self-approval) and
`scrdual` (investigator and approver). The password is held in the seed configuration.

The permissions of each role and the menu it sees are pinned by the persona check (client
requirement 14): [`frontend/src/navigation/personaMenus.json`](../../frontend/src/navigation/personaMenus.json),
tested by `navigation/personaMenus.test.ts` (menu) and `security/PersonaMenusIT` (database grants
and one read per screen).

## 3. Screens

Group **Client & Policy**, section **Sanction Screening** (`features/screening/module.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| Screening Home | `/screening` | SCR_VIEW | Tiles: open cases per stage, due today, breached, potential matches not yet in a case, last list run |
| Cases | `/screening/cases` | SCR_VIEW | Work list with tabs My Cases / Team / For Approval / Committee / STR / Closed / All, search by case number or client, filters (stage, case type, risk category, marketing unit, unit head, disposition, assignee, SLA state, created dates), bulk re-assign |
| Screening Case | `/screening/cases/:id` | SCR_VIEW | Summary with SLA badge, workflow panel with the stage actions, tabs Matches, Review, Documents, Decisions (approvals and votes), STR, Timeline |
| Matches | `/screening/matches` | SCR_VIEW | Potential / true / false-positive matches with the side-by-side comparison, Open Case, Mark False Positive (with evidence) |
| Screening Runs | `/screening/runs` | SCR_VIEW | Log of every screening run with its matches |
| High-risk Clients | `/screening/high-risk` | SCR_VIEW | High-risk and PEP clients with export |
| STR | `/screening/str` | SCR_COMPLIANCE_REVIEW, SCR_STR_EXTRACT or SCR_AUDIT_VIEW | STR register and extractions ("Extract Approved STRs", download, filing reference) |

Group **Setup & Administration**, section **Compliance Setup** (`features/screening/setupModule.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| Configuration Versions | `/screening-setup/config` | SCR_CONFIG_MAINTAIN, SCR_CONFIG_APPROVE, SCR_LIST_MAINTAIN, SCR_LIST_APPROVE | A tab per configuration type; draft editor, submit, difference with the version in force, approve / reject |
| Templates | `/screening-setup/templates` | SCR_CONFIG_MAINTAIN, SCR_CONFIG_APPROVE | Review and STR template designer (sections, fields, mandatory, list of values) with preview |
| Watchlist | `/screening-setup/watchlist` | SCR_VIEW, SCR_LIST_MAINTAIN, SCR_LIST_APPROVE | Entries by source; add / change / deactivate (maker); pending changes with before / after (checker) |
| List Sources and Runs | `/screening-setup/sources` | SCR_LIST_MAINTAIN, SCR_LIST_APPROVE | Sources and schedules, list file template, Upload List File, run log with counts and failed records |

The client record has a **Screening** tab (SCR_VIEW) with the client's cases, matches and
risk-profile history. The dispositions are maintained in Lists of Values (type `SCR_DISPOSITION`,
parent = stage). The reports are in the Report Centre, category **Compliance**. Help:
`features/screening/help.ts` (`SCREENING_HELP`, `SCREENING_SETUP_HELP`).

## 4. Flows

### 4.1 Configuration (SNSRP-101 to 109)

1. The Compliance Officer opens **New Draft** on a configuration type: the existing draft, or a copy
   of the version in force. A pending version blocks a new draft (`SCR_CONFIG_PENDING`).
2. Save validates the rows (thresholds between 0 and 1, the case threshold at least the matching
   threshold, SLA hours, routes, template fields...).
3. Submit with an effective date (today or later) and a change note; the difference with the
   version in force is stored. An empty draft or one identical to the version in force is refused.
4. The checker approves (the version becomes ACTIVE from its effective date; the previous one is
   SUPERSEDED once the new one is in force) or rejects with a reason. The maker never decides.

Every case records the six version ids in force when it was opened, so a later change never
alters a running case.

### 4.2 Watchlists (SNSRP-201 to 204)

- **Manual change:** the maker adds, changes or deactivates an entry; the change is PENDING until a
  checker approves it; approved changes of ACTIVE entries trigger the delta screening at once.
- **List file:** the maker uploads a file in the source's layout (template on the Sources screen);
  the run (`WLR-yyyy-nnnnnn`) records added / changed / unchanged / delisted / failed counts; the new
  and changed entries are staged PENDING and the checker approves them one by one or all at once
  from the run.
- **Scheduled feed:** job `SCR_WATCHLIST_INGEST` reads the files staged on each active source
  (`WatchlistFeed` port) and applies them at once (recorded as approved by SYSTEM). A full-file
  source delists the entries missing from the file. A failed or partial run raises
  `SCR_INGEST_FAILED` and notifies the list makers; the failed records are e-mailed by
  `SCR_INGEST_ERROR_DIGEST`.

### 4.3 Screening, matches and risk (SNSRP-301 to 304, 602)

1. Triggers, each after the business transaction commits (a screening failure never undoes the
   business save): client registered, client identity changed (name, birth date, nationality, TIN,
   ID), account application submitted, watchlist entries changed; plus the periodic job and
   "Screen Now" on the client (SCR_INVESTIGATE).
2. Names are normalised (accents, titles, company forms, particles "de la" / "dela"); candidates
   share a phonetic or exact blocking key; each MATCH_CRITERIA rule scores EXACT, PHONETIC (Double
   Metaphone) or FUZZY (Jaro-Winkler), adjusted by birth date, nationality and ID number.
3. A score at or above the rule's threshold is a **match** (POTENTIAL); at or above the case
   threshold it also opens a case. The same client, entry version and criteria version are never
   recorded twice; a pair cleared as a false positive is not raised again until the entry changes.
4. The RISK_RULES version assigns a risk category: the client's rating is raised (never lowered),
   the category's tags are added (e.g. `WATCHLIST_REVIEW`, `PEP`), and a HIGH rating brings the
   KYC review date forward. Each change is a row of the client's risk-profile history.
5. The investigator confirms a match (TRUE_MATCH, rules evaluated again) or marks it a false
   positive with a justification and evidence; "Update Risk Tag" changes the profile by hand
   (SCR_RISK_TAG, justification and evidence mandatory).

### 4.4 Case (SNSRP-401 to 405, 501, 502, 601, 701 to 706)

```
NEW -> INVESTIGATION -> UNIT_HEAD_APPROVAL -> COMPLIANCE_REVIEW -> AML_COMMITTEE -> STR_PREPARATION -> STR_EXTRACTION -> CLOSED
            ^   |               |                  |    |               |
            |   +-> CLOSED      +-> RETURNED <-----+    +-> CLOSED      +-> COMPLIANCE_REVIEW (committee return)
            +----- RETURNED (resubmit to the stage that returned it)
```

| Step | Who | Endpoint (`/api/v1/screening/cases/{id}`) | Rules |
|---|---|---|---|
| Open | System | - | One open case per client and case type (a new match joins it); template by case type (KYC_REVIEW, EDD, TRANSACTION_REVIEW); investigator by the assignment matrix (ROUND_ROBIN, LEAST_OPEN or the stage queue); SLA of the stage |
| Re-assign | UCC, Compliance, unit head | `POST reassign` | Reason from `SCR_REASSIGN_REASON`; the new assignee must be eligible for the stage |
| Review | Investigator | `PUT review`, `POST documents` | Mandatory template fields; documents with form type, document type, date received and source, named `<FORM>_<CLIENT>_<yyyyMMdd>_<DOCTYPE>_<n>` |
| Submit | Investigator | `POST submit` | VALIDATION_RULES (mandatory fields and documents) must pass; the route of the APPROVAL_MATRIX picks the approver; NEED_MORE_INFO returns the case to the account officer |
| Unit head | Approver | `POST decision` | CONCUR / NOT_CONCUR (rationale); never the case's investigator |
| Compliance | Compliance Officer | `POST outcome` | CLOSE_NO_ACTION, RETURN (reason), ESCALATE_COMMITTEE, FOR_STR |
| Committee | AML Committee | `POST votes` | One vote per member and round; `SCR_COMMITTEE_RULE` (ANY / MAJORITY / ALL) over `SCR_COMMITTEE_SIZE`; a tie opens a new round |
| STR | Compliance Officer | `POST /cases/{id}/str`, `PUT /str/{id}`, `POST /str/{id}/ready` | Prefilled subject and transactions; reason codes, mandatory STR fields and at least one transaction |
| Extraction | Compliance Officer | `POST /str/extractions`, `GET /str/extractions/{id}/file` | Committee-approved STRs of a period to one file in the STR layout (CSV or fixed width), archived with its SHA-256; a re-extraction needs a reason |
| Filing | Compliance Officer | `POST /str/{id}/filing` | Unique AMLC reference, filing date not before the extraction nor in the future; closes the case |
| Re-open | Compliance Officer | `POST reopen` | Reason; back to COMPLIANCE_REVIEW |

Every step writes the case timeline (`scr_case_event`, insert-only) and the audit trail. Pending
unit-head and committee decisions appear in **My Approvals**.

The whole path is exercised through the HTTP API by `api/ScreeningEndToEndApiIT` (registration to
filed STR, the client's Screening tab and the reports; the SLA reminder and breach through the job
monitor).

## 5. Jobs

| Job | Cron property (UTC; PHT time) | Work |
|---|---|---|
| `SCR_WATCHLIST_INGEST` | `brokerverse.jobs.scr-watchlist-ingest-cron` `0 0 17 * * *` (01:00) | Reads the staged list files of each active source, one transaction per file |
| `SCR_PERIODIC_SCREENING` | `brokerverse.jobs.scr-periodic-screening-cron` `0 30 17 * * *` (01:30) | Delta screening of the in-scope clients against the entries changed since the last run; full rescreen on day `SCR_FULL_RESCREEN_DAY` |
| `SCR_SLA_MONITOR` | `brokerverse.jobs.scr-sla-monitor-cron` `0 0 * * * *` (hourly) | One reminder at remind-at, breach flag + alert `SCR_SLA_BREACH` + escalation notice, daily missing-document reminder |
| `SCR_INGEST_ERROR_DIGEST` | `brokerverse.jobs.scr-ingest-error-digest-cron` `0 0 23 * * SUN-THU` (07:00 Mon-Fri) | E-mails the failed list records (CSV) to `SCR_INGEST_ALERT_RECIPIENTS` |

Jobs run on one instance at a time (job lock) and can be run on demand from Administration > Job
Monitor (`POST /api/v1/system/jobs/{name}/run`).

## 6. Parameters and lists of values

| Parameter (category SCREENING) | Delivered value | Use |
|---|---|---|
| `SCR_SCREENING_SCOPE` | `PROSPECT,CONFIRMED` | Client statuses screened by the periodic job (SQ11) |
| `SCR_FULL_RESCREEN_DAY` | 1 | Day of the month of the full rescreen |
| `SCR_BLOCK_ON_OPEN_MATCH` | false | Reserved for the optional blocking gate (not built, SQ07) |
| `SCR_COMMITTEE_RULE` | MAJORITY | ANY / MAJORITY / ALL (SQ15) |
| `SCR_COMMITTEE_SIZE` | 5 | Committee members counted by MAJORITY / ALL |
| `SCR_INGEST_ALERT_RECIPIENTS` | empty | Comma-separated e-mail addresses of the ingestion digest (SQ01) |
| `SCR_CASE_SEQUENCE_PREFIX` | SCR | Case number prefix |

Lists of values: `SCR_DISPOSITION` (parent = stage; "to confirm", SQ06), `SCR_CASE_TYPE`,
`SCR_LIST_TYPE`, `SCR_REASSIGN_REASON`, `SCR_STR_REASON` (empty until SQ09; seed codes
RSN01-03), `SCR_FORM_TYPE`, `SCR_DOCUMENT_TYPE`.

Configuration versions (Compliance Setup): MATCH_CRITERIA, RISK_RULES, APPROVAL_MATRIX,
ASSIGNMENT_MATRIX, SLA_MATRIX, VALIDATION_RULES, TEMPLATE (per template type), STR_LAYOUT. The seed
company FVI has version 1 of each (V1950), with seed values only.

## 7. Reports (category Compliance, view and export SCR_REPORT_VIEW)

| Code | Content |
|---|---|
| `SCR-HIGH-RISK-CLIENTS` | High-risk clients with rating, category, tags, last case |
| `SCR-PEP-CLIENTS` | Clients tagged PEP |
| `SCR-SANCTIONED-NAMES` | Clients with a true match on a sanction list |
| `SCR-CASE-STATUS` | Cases by stage, type, assignee and age |
| `SCR-SLA-BREACHES` | Cases past their SLA with the escalation |
| `SCR-INGEST-ERRORS` | Failed list records by run |
| `SCR-STR-REGISTER` | STRs with status, extraction and AMLC reference (also Word) |
| `SCR-AUDIT-LOG` | Screening audit log (SCR_AUDIT_VIEW) |

Each runs on screen and exports to PDF, Excel and CSV; every run is archived.

## 8. Integrations and ports

| Port | Default | Replace when |
|---|---|---|
| `screening.watchlist.service.WatchlistFeed` | `StagedFileWatchlistFeed`: files staged on the source (attachment or `POST /watchlist/sources/{code}/stage`) | BDOI gives the SFTP / e-mail / API / NLDS transport (SQ01) |
| `screening.str.service.StrTransactionSource` | Transactions from the client's accounts | Finance exposes invoices and receipts |
| `screening.str.service.StrFileSink` | Archive in the report archive (SCR-STR-REGISTER) | An AMLC drop folder is agreed |
| `screening.cases.service.ActivePolicyQuery` | Client records of kind Account with status POLICY_ISSUED / BOOKED | Booking provides policy periods (SQ10) |
| `screening.matching.service.MatchCaseOpener` | `CaseOpeningService` | - |

Events consumed: `crm.service.ClientRegistered`, `ClientIdentityChanged`,
`account.service.AccountStatusChanged` (SUBMITTED), `WatchlistEntriesChanged`. Event published:
`ScreeningCompleted` (cases are opened from it). Contracts used from crm:
`ClientRiskService.applyRiskProfile`.

## 9. Parked items

| Item | Question | Built now |
|---|---|---|
| Production thresholds, categories, matrices, SLA hours | SQ02-SQ04, SQ08 | Configurable versions; seed values |
| Review and STR template field lists, dispositions | SQ05, SQ06 | Template designer; LOV seeds "to confirm" |
| AMLC STR layout (XLSX / XML) and reason codes | SQ09 | CSV and fixed-width layouts; `SCR_STR_FORMAT_PARKED` for the others |
| List transports (SFTP, e-mail advisory, API, NLDS) | SQ01 | Upload and staged files |
| Blocking gate on quotation / placement | SQ07 | Parameter only (off) |
| Operations Lead grants | SQ19 | None granted |
| Other parties (beneficial owners, signatories) | SQ11 | Clients only |
| Weights of birth date, nationality and ID | SQ02 | Code constants |
| Working-hour SLA calendars | - | Calendar hours |
| Asynchronous screening after commit | - | Runs in the request thread after commit |
| Guard against deleting case documents through the generic attachment API | - | Not built |
| `SCR_CASE_ASSIGNED` notice | - | The platform assignment notice is used |
| Seed unit heads with SCR_CASE_APPROVE | - | Seed cases use the approvers' queue |

## 10. Troubleshooting (production support)

| Symptom / error code | Cause | Fix |
|---|---|---|
| No match and no case for a client that is on the list; alert `SCR_NO_ACTIVE_CONFIG` | No ACTIVE MATCH_CRITERIA version for the company on the day | Compliance approves a MATCH_CRITERIA version (Configuration Versions); then "Screen Now" on the client or wait for the periodic job |
| Match recorded but no case opened | The score is below the rule's case threshold (`min_score_for_case`), or the client already has an open case of that type (the match joined it) | Check the match score on Matches; open the case by hand with **Open Case**, or adjust the threshold in a new MATCH_CRITERIA version |
| Match recorded but the rating did not change | No RISK_RULES version in force, the category's rating is not higher than the client's, or its tags are not valid `CLIENT_TAG` codes | Approve a RISK_RULES version; correct the category; use Update Risk Tag with evidence |
| A new list entry does not screen the clients | The change is still PENDING (maker-checker) | The checker approves it on Watchlist or on the run page |
| `SCR_INGEST_FAILED` alert; run FAILED "No list file was received" | The scheduled job found no new file for an active FILE source | Stage or upload the file on List Sources and Runs; re-run `SCR_WATCHLIST_INGEST` from the job monitor |
| Run PARTIAL with failed records | Rows not matching the layout (name missing, bad date) | Read the failed records on the run; fix and re-upload; the digest job e-mails them daily |
| Digest job message "no recipients" (`SCR_INGEST_DIGEST_NO_RECIPIENTS`) | `SCR_INGEST_ALERT_RECIPIENTS` empty or invalid | Enter valid e-mail addresses in System Parameters |
| `SCR_CONFIG_PENDING` on New Draft | A version of that type is waiting for approval | The checker decides it, or the maker withdraws it |
| `SCR_CONFIG_UNCHANGED` / `SCR_CONFIG_EMPTY` on submit | The draft equals the version in force / has no rows | Change the draft before submitting |
| `SCR_CONFIG_MAKER_APPROVES` / `SCR_LIST_MAKER_APPROVES` | The maker tried to approve their own change | Another Compliance Officer (checker) approves |
| `SCR_VALIDATION_FAILED` on Submit | Mandatory review fields or required documents missing (VALIDATION_RULES) | Complete the Review tab and upload the listed document types; the refusal is logged on the timeline |
| `SCR_CASE_NOT_YOURS` / `SCR_CASE_ASSIGNED_ELSEWHERE` | The case moved on or was re-assigned | Refresh; the UCC re-assigns it if needed |
| `SCR_APPROVER_IS_INVESTIGATOR` | The unit head investigated the case | Re-assign the approval to another approver |
| `SCR_ASSIGNEE_NOT_ELIGIBLE` | The chosen user lacks the stage permission | Pick a user from Eligible assignees, or grant the role through a User Access request |
| `SCR_ALREADY_VOTED` | The member already voted in this round | Nothing to do; a new round starts only on a tie |
| Committee case stays in AML_COMMITTEE | Not enough votes for `SCR_COMMITTEE_RULE` over `SCR_COMMITTEE_SIZE` | Remaining members vote; check that the size matches the number of AML_COMMITTEE users |
| `SCR_STR_INCOMPLETE` on Mark Ready | Reason codes, mandatory STR fields or transactions missing (the gaps are listed) | Complete the STR tab |
| `SCR_NOTHING_TO_EXTRACT` | No committee-approved STR not yet extracted in the period | Check the period (committee decision date, Manila days) and the STR status |
| `SCR_STR_FORMAT_PARKED` | The STR_LAYOUT version uses XLSX or XML | Use CSV or FIXED until SQ09 is answered |
| `SCR_AMLC_REFERENCE_USED` / `SCR_FILING_BEFORE_EXTRACTION` | Duplicate AMLC reference / filing date before the extraction | Enter the reference and date given by the AMLC portal |
| No SLA reminders or breaches | `SCR_SLA_MONITOR` not running or failing | Job Monitor: check the last runs; run it now; see `JOB_FAILURE` alerts |
| Screening seems slow when saving a client | Screening runs after commit in the same request (design 18.2) | Expected for large lists; the client save itself is already committed |

## 10.1 Where to look

- Tables: `scr_config_version` (+ rule tables), `scr_watchlist_*`, `scr_screening_run`, `scr_match`,
  `scr_match_suppression`, `scr_client_risk_profile`, `scr_case`, `scr_case_event`,
  `scr_committee_vote`, `scr_str*`.
- Alerts: `SCR_INGEST_FAILED`, `SCR_SLA_BREACH`, `SCR_NO_ACTIVE_CONFIG`,
  `SCR_OPEN_TRUE_MATCH_AGEING` (Alerts screen, module SCREENING).
- Audit trail entity types: `ScreeningCase`, `ScreeningMatch`, configuration versions and watchlist
  changes; the case timeline holds every stage change with its user and remarks.
