# iNXT BrokerVerse - Broking (BDOI New Business) Architecture

This document is binding for every change to the broking modules. It extends the
[Developer Guide](../development/DEVELOPER_GUIDE.md), whose rules apply unchanged: module layout,
ArchUnit, quality gates, tests and help entries.

Requirements baseline: [`docs/requirements/BDOI_NB_BRD_SPEC.md`](../requirements/BDOI_NB_BRD_SPEC.md)
(BRD-1 New Business). Each class, migration and screen that implements a requirement cites its BR ID
in Javadoc or in a comment, for example `BRNB.051`.

## 1. Business model in one paragraph

BDOI is an **insurance broker**. Insurers issue the policies.

1. Marketing receives a quotation request by e-mail, from a source system (HLS) or by bulk upload.
2. Marketing quotes a package product, or raises a Proposal Request (PRF) that the TSU prices with
   insurers for a non-package risk.
3. Marketing onboards the client (prospect, then KYC-verified client) and creates one or more
   **accounts**. The account is the risk record. It is identified by one Account Reference Number
   (ARN) from quotation to invoice.
4. Processing checks payment or client confirmation, places the account with the insurer
   (placement slip, hold cover) and handles insurer returns.
5. Processing receives the e-policy and updates the policy number. Insurance Advice is generated for
   mortgaged accounts, and the e-policy is sent to the client encrypted.
6. Processing books the account: GL entry, service invoice, cost center and incentive flag.
   Endorsements and cancellations follow.

## 2. Modules

Each module is a top-level package under `com.iortatechnxt.brokerverse`, with the usual
`domain` / `service` / `api` layout. An arrow means "depends on". Cycles are forbidden.

| Module | Owner wave | Purpose | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `lov` | W1 | Lists of values: type, code, label, effectivity, maker-checker (BRNB.083) | common, audit, approval | V750 (V980) |
| `workflow` | W1 | Stage/transition engine, work cases, status history, queues, assignment, SLA (BRNB.022/080/096/115) | security, messaging | V751 (V980) |
| `bulk` | W1 | Bulk upload framework: template, parse XLSX/CSV/ODS, validate, partial commit, error report (BRNB.024/025/039) | system | V752 |
| `messaging` | W1 | Outbound e-mail outbox, send log, document protection (password / encryption), in-app notifications (BRNB.013/015/035) | security, system | V753 |
| `docgen` | W1 | Versioned document templates with `{{placeholders}}`; branded PDF / XLSX composition (BRNB.004) | common | V754 |
| `crm` | W1 core, W2 full | Clients: prospect / confirmed, KYC, tags, special instructions, dedupe, contacts, 360 view (BRNB.030/032/046-049/090/091/099/101/110) | party, lov, workflow, messaging, attachment, bulk, report, nbadmin (retention port) | V800-V809 (V981) |
| `catalog` | W2 | Product lines, cover types, BDOI risk codes, insurer panel and branches, commission and taxes, minimum-field matrix, TSU routing rules, sales organisation, premium calculator (Appendix A) | party, lov, dimension | V810-V819 (V982) |
| `account` | W2 | Account (ARN), risk items, account contacts, duplicate rules, FFY, direct payment, multi-year, account lifecycle (BRNB.025/050-054/066/102/109/113/114) | crm, catalog, workflow, bulk | V820-V829 (V983) |
| `quotation` | W3 | Package quotations: request intake, versions, approval, send, acceptance, conversion to accounts, bulk quotations | account, crm, catalog, workflow, docgen, messaging, bulk | V830-V839 (V984) |
| `nonpackage` | W3 | PRF, TSU queue, Quotation Slip, insurer responses, comparative table, Proposal Slip (BRNB.005-018) | account, crm, catalog, workflow, docgen, messaging | V840-V849 (V985) |
| `placement` | W3 | Payment gate, CLPC billing file and payment matching, placement slip, send, hold cover, returns, cancel / reactivate placement (BRNB.033/034/062/067-072/103) | account, catalog, workflow, docgen, messaging, bulk | V850-V859 (V986) |
| `issuance` | W3 | E-policy receipt, policy number update, Insurance Advice, encrypted e-policy dispatch, dispatch report (BRNB.035/060/070/073-078/095/104/105) | account, workflow, docgen, messaging | V860-V869 (V987) |
| `booking` | W3 | Individual / batch / direct / multi-year booking, accounting events, service invoice, endorsements, cancellation, incentive flag, cost center (BRNB.027/036/038/061/076/081/094/100/107/108/111/112) | account, catalog, accounting, subledger, workflow, docgen, messaging | V870-V879 (V988) |
| `nbreport` | W4 | NB operational reports and the NB dashboard (BRNB.011/012/075/078 ...) | all broking modules | V880-V889 (V989) |
| `nbadmin` | W2 | User access requests with approval, access matrix, retention policy job, session policy, multi-tab session (BRNB.040/082/083/085/106) | security, system, approval, messaging, docgen | V790-V799 |

Business modules never call "upward". The rules for callbacks between modules are:

- `account` knows nothing about quotations or placement. It stores their references as plain values.
- `quotation` and `nonpackage` create accounts through `AccountService`.
- `placement`, `issuance` and `booking` move the account through `AccountLifecycleService`.

## 3. Shared contracts (W1)

### 3.1 Workflow (`workflow`)

- Definitions are data: `wf_stage` (workflow, stage, name, owner permission, SLA hours, terminal) and
  `wf_transition` (workflow, from stage, action, to stage, permission, generic flag, reason
  required). They are seeded in `V751`. A module that needs a new transition adds it in its own
  migration.
- A **work case** (`wf_case`) tracks one business record, identified by entity type, entity id and
  business reference (ARN, PRF number, ...). It holds the current stage, assignee, queue permission,
  stage entry time and deep link. Every change writes a `wf_case_history` row with from / to stage,
  action, reason code, comment, actor (a user or `SYSTEM`) and timestamp. This implements
  BRNB.022 and BRNB.115.
- `WorkflowService`:
  - `start(StartCase)`
  - `transition(entityType, entityId, action, TransitionNote)`: user action; checks the transition
    exists from the current stage and that the user holds its permission.
  - `systemTransition(...)`: automated step.
  - `assign(caseId, username)` and `claim(caseId)`.
  - `caseOf(entityType, entityId)` and `history(...)`.
  - `queue(QueueQuery, Pageable)` and `counts(...)`.
- **Generic** transitions have no side effect beyond the stage change, for example "return to
  Marketing" with a reason. Any user may run them through `POST /api/v1/workflow/cases/{id}/actions/{action}`.
  Business actions (book, send slip, ...) are called only by the owning module's endpoint, which
  calls `transition` inside its own transaction.
- On entry to a stage, a notification goes to holders of the stage's owner permission, or to the
  assignee. See 3.4.
- Workflows seeded in V751 are listed in section 4.

### 3.2 Lists of values (`lov`)

- `lov_type` holds the code, name and whether business users may maintain it. `lov_value` holds
  type, code, label, sort order, parent code, and effective from / to. `lov_value` is maker-checker
  (`AuthorizableEntity`).
- Use `LovService.requireValid(type, code, onDate)` in validations, and
  `LovService.activeValues(type, onDate)` for UI lists. The front end uses `<LovSelect type="...">`.
- Every coded business field the BRD calls an "LOV" is an LOV type, never an enum, unless it
  drives code paths (statuses stay enums). Types are seeded in V750; modules add their own types in
  their own migrations.

### 3.3 Bulk processing (`bulk`)

- A module implements `bulk.service.BulkImportHandler` with these members:
  - `code()`, `title()`, `permission()`, `columns()`
  - `sanitize(row)`: optional; trims, upper-cases and normalises.
  - `validate(row, context)`
  - `commit(row, context)`: returns the created reference.
- The framework does the rest:
  - template download (XLSX with an instructions sheet);
  - upload of XLSX / CSV / ODS, with header checks against the template;
  - row validation;
  - review of the job;
  - commit of valid rows, each in its own transaction, recording failures;
  - summary and error report download (XLSX).
- Jobs and rows are kept in `bulk_job` and `bulk_row` for audit. The UI is the generic
  `/bulk/:handler` wizard.

### 3.4 Messaging (`messaging`)

- `MessageService.queueEmail(OutboundEmail)` takes recipients, subject, body, attachments, an
  optional `Protection` and the related entity. With protection, PDF attachments are encrypted
  (AES) with a password and XLSX files are password-protected. If `deliverPasswordSeparately` is
  set, a second e-mail carrying only the password is queued (BRNB.035).
- Messages are dispatched by the `MAIL_DISPATCH` managed job, and immediately after the commit.
- The transport is the `MailTransport` port. By default it is SMTP when `brokerverse.mail.enabled`
  is set; otherwise messages are recorded as `SENT (simulated)`, so demos and tests never send
  e-mail.
- Every attempt is logged with recipient, time, subject and attachment SHA-256 (BRNB.008 send log).
  The outcome (sent or failed, with the reason) is readable per entity, for the dispatch reports.
- `NotificationService.notifyUser(...)` and `notifyPermission(...)` create in-app notifications,
  shown by the bell in the header.
- Password conventions are pluggable through `DocumentPasswordPolicy`. The default is a generated
  12-character password. The BDOI convention is parked (Q07).

### 3.5 Documents (`docgen`)

- `doc_template` holds code, version, title, body with `{{placeholders}}`, effective date and active
  flag. Templates are versioned: a new version supersedes the old one, and the version used is stored
  on the generated record (BRNB.004).
- `DocumentComposer.pdf(DocumentSpec)` renders a BDOI-branded PDF. The spec contains the title,
  reference, key-value sections, tables, merged template text and a signature block.
  `DocumentComposer.xlsx(SheetSpec)` renders spreadsheets such as placement slips and billing files.

### 3.6 Numbers

Numbers come from `DocumentNumberService` with these prefixes:

| Number | Prefix |
|---|---|
| Account Reference Number | `ARN-<yyyy>` |
| Prospect code | `PR-<yyyy>` |
| Client code | `CL-<yyyy>` |
| Proposal Request (marketing reference) | `PRF-<yyyy>` |
| Quotation Slip | `QS-<yyyy>` |
| Proposal Slip | `PS-<yyyy>` |
| Placement slip | `PL-<yyyy>` |
| Insurance Advice | `IA-<yyyy>` |
| Service invoice | `SI-<branch>-<yyyy>` |
| Bulk job | `BLK-<yyyy>` |

The ARN is generated once, at quotation or PRF creation (or at direct account creation). It is
carried unchanged on every downstream record, report and interface (BRNB.102).

### 3.7 Security

Permissions, added to `security.domain.Permission` in W1 and granted in V750:

| Permission | Used for |
|---|---|
| `CLIENT_VIEW`, `CLIENT_MAINTAIN`, `CLIENT_APPROVE` | Clients, KYC verification, prospect conversion |
| `QUOTE_VIEW`, `QUOTE_MAINTAIN`, `QUOTE_APPROVE` | Package quotations |
| `PROPOSAL_REQUEST`, `PROPOSAL_APPROVE` | PRF by Marketing; TL / TH / UH approval |
| `TSU_PROCESS`, `TSU_APPROVE` | QS / PS preparation and approval |
| `ACCOUNT_VIEW`, `ACCOUNT_MAINTAIN`, `ACCOUNT_PROCESS` | Accounts (Marketing), processing actions |
| `BULK_PROCESS` | Bulk uploads (handlers add their own permission) |
| `PLACEMENT_MANAGE`, `BILLING_MANAGE` | Placement, hold cover, returns; CLPC billing, payment matching |
| `EPOLICY_MANAGE`, `EPOLICY_SEND` | E-policy receipt and Insurance Advice; E-policy Sender |
| `BOOKING_PROCESS`, `BOOKING_ADJUST` | Booking; Adjustment (cancel booking, negative endorsements) |
| `WORK_VIEW`, `WORK_ASSIGN` | My Work queues; assign / re-assign workload (Approver) |
| `LOV_MANAGE` | Maintain LOVs (authorised with `MASTER_AUTHORIZE`) |
| `ACCESS_REQUEST`, `ACCESS_APPROVE` | Profile creation / modification requests |
| `MESSAGE_VIEW` | Outbound message log |

BDOI roles, seeded in V750 from the BRD personas:

| Role | Persona |
|---|---|
| `MKT_AO` | Marketing Account Officer |
| `MKT_TL` | Marketing TL / TH / UH approver |
| `TSU` | Technical Support Unit |
| `PROCESSOR` | Processing |
| `PROCESSING_TL` | Processing team leader |
| `NB_APPROVER` | Approver (pending requests, workload assignment) |
| `EPOLICY_SENDER` | E-policy Sender |
| `ADJUSTMENT` | Adjustment |
| `BUSINESS_ADMIN` | Business Administrator |

The existing `SYSADMIN` role is the System Administrator.

Demo users (V980; password `Brokerverse@2026`):

| User | Role |
|---|---|
| `ao` | MKT_AO |
| `ao2` | MKT_AO (second maker for four-eyes demos) |
| `mkttl` | MKT_TL |
| `tsu`, `tsulead` | TSU |
| `proc` | PROCESSOR |
| `proctl` | PROCESSING_TL |
| `approver` | NB_APPROVER |
| `epol` | EPOLICY_SENDER |
| `adjust` | ADJUSTMENT |
| `badmin` | BUSINESS_ADMIN |

## 4. Workflows (seeded in V751)

### `NB_QUOTATION`: package quotation (BRNB.020-022, 041-045)

```
DRAFT --submit--> FOR_REVIEW --approve--> APPROVED --send--> SENT_TO_CLIENT --accept--> ACCEPTED --create_accounts--> CONVERTED
  ^                  |return(reason)                              |decline--> NOT_PROCEEDED
  +------------------+
DRAFT/FOR_REVIEW --void(reason)--> VOIDED
```

### `NB_PROPOSAL`: non-package PRF (BRNB.005-017)

```
DRAFT --submit--> FOR_MKT_APPROVAL --approve--> WITH_TSU --prepare_qs--> QS_PREPARATION --submit_qs--> QS_FOR_APPROVAL
  --approve_qs--> QS_SENT --terms_complete--> TERMS_RECEIVED --submit_ps--> PS_FOR_APPROVAL --approve_ps--> PS_RELEASED
  --send_to_client--> SENT_TO_CLIENT --accept--> ACCEPTED --create_accounts--> CONVERTED
returns: FOR_MKT_APPROVAL/WITH_TSU --return--> DRAFT; QS_FOR_APPROVAL --return--> QS_PREPARATION; PS_FOR_APPROVAL --return--> TERMS_RECEIVED
SENT_TO_CLIENT --decline--> NOT_PROCEEDED; DRAFT..WITH_TSU --void--> VOIDED
```

### `NB_ACCOUNT`: account from creation to booking (BRNB.022/033/034/062/069/094/111/115)

```
DRAFT --submit--> SUBMITTED --validate--> AWAITING_PAYMENT --payment_confirmed--> READY_FOR_PLACEMENT
SUBMITTED --return--> RETURNED_TO_MARKETING --resubmit--> SUBMITTED
READY_FOR_PLACEMENT --place--> PLACED --policy_received--> POLICY_ISSUED --book--> BOOKED
PLACED --insurer_return--> RETURNED_BY_INSURER --resubmit--> READY_FOR_PLACEMENT
RETURNED_BY_INSURER --return--> RETURNED_TO_MARKETING
READY_FOR_PLACEMENT/PLACED/RETURNED_BY_INSURER --cancel_placement--> PLACEMENT_CANCELLED --reactivate--> READY_FOR_PLACEMENT
SUBMITTED --direct_booking--> POLICY_ISSUED            (policy already issued; policy document mandatory, BRNB.111)
BOOKED --cancel--> CANCELLED                           (post-issuance cancellation, BRNB.094)
DRAFT/SUBMITTED/RETURNED_TO_MARKETING --void--> VOIDED (internal reversal / soft delete, BRNB.019/094)
```

- The two gates are `validate` and `payment_confirmed`.
- CBG Fire and Motor accounts must be paid. Other Lines need client confirmation (BRD 2.3.1).
- Direct-payment accounts skip the payment gate (BRNB.114).

### `NB_CLIENT`: client onboarding (BRNB.090/101, seeded in V801)

```
PROSPECT --submit_kyc--> KYC_REVIEW --verify_kyc--> KYC_VERIFIED --confirm--> CONFIRMED --review_kyc--> CONFIRMED
KYC_REVIEW --return(reason)--> PROSPECT
PROSPECT/KYC_REVIEW/KYC_VERIFIED/CONFIRMED --deactivate(reason)--> INACTIVE
```

- `submit_kyc` (CLIENT_MAINTAIN) and `confirm` need the mandatory KYC documents and the minimum
  client information; `verify_kyc` / `review_kyc` (CLIENT_APPROVE) are refused to the client's
  creator and to the user who submitted the KYC (four eyes).
- `confirm` issues the client code and opens the client's party; `return` is the only generic
  action (run from the `WorkflowPanel`).

## 5. Front end

- Broking sections come first in the sidebar:
  - **My Work** (queues and notifications)
  - **Clients**
  - **Quotations & Proposals**
  - **Accounts & Placement**
  - **Issuance & Booking**
  - **Bulk Processing**
  - **Products & Insurers** (catalog)
  - **Broking Setup**

  The insurer and finance sections follow.
- Shared broking components (W1, `components/broking`):
  - `StageTimeline`: status history.
  - `WorkflowPanel`: current stage, SLA, assignee and generic actions with a reason dialog.
  - `LovSelect`
  - `BulkUploadWizard`
  - `SendEmailDialog`: recipients, subject, body, protection toggle.
  - `ReferenceChip`: ARN with copy.
  - `InstructionsBanner`: client and account special instructions (W2 `crm`).
- Every broking record page shows, top to bottom:
  1. a header with the reference and status badge;
  2. the `WorkflowPanel`;
  3. tabs: Details, Documents, History;
  4. the actions allowed for the current user's permissions.

## 6. Parked (waiting for later BRDs or BDOI answers)

Build the seam (port, configuration or placeholder) and document it in the module guide. Do not
build the external interface itself.

| Item | Question | Seam |
|---|---|---|
| HLS inbound interface | Q11 | `quotation` request intake port + bulk handler |
| CLPC transport (SFTP) | Q28 | billing file generation and payment report import work by upload / download |
| Insurer channels (SFTP / API) | Q06 | `placement` sends by e-mail; `cat_insurer.placement_channel` accepts only EMAIL (`PLACEMENT_CHANNEL_PARKED`) |
| Shared mailbox reading (requests, e-policies) | Q12, Q31 | manual upload with the e-mail attached |
| OCR / data extraction from documents | Q24 | `issuance` extraction port with a manual review screen |
| BDO SSO / Active Directory | Q42 | existing JWT login |
| Product matrix content | Q01, Q02 | minimum-field, document and TSU rule tables configurable (`catalog`); seeds are defaults |
| Incentive rules | Q33 | rule table empty |
| BDO KYC standard fields | Q16 | configurable KYC checklist |
| Data ownership register | Q37 | not started |
| Dynamic report builder | Q40 | saved report variants only |
| Override requests (Renewal) | OOS-1 | not built |
| KYC review frequency by risk rating | Q21 | parameters `KYC_REVIEW_MONTHS`, `KYC_REVIEW_MONTHS_HIGH_RISK` |
| Duplicate keys and precedence | Q17 | hard keys TIN, ID, name + birth date; soft keys e-mail, mobile, corporate name |
| Client tags / instruction types and enforcement (block or warn) | Q18 | LOVs `CLIENT_TAG`, `INSTRUCTION_TYPE`; banner only (warn) |
| BDO CIF integration (source of truth for bank clients) | Q16 | `bank_client` flag and CIF number captured manually |
| Retention periods per record type; archive vs purge | Q39 | rule table `nba_retention_rule` and monthly counts; no archive / purge (DBA storage and backup decision) |
| Forced password change at first sign-in | - | temporary password shown once to the approver; user changes it on My Profile |
| TSU routing thresholds | Q04 | `cat_tsu_rule` seeds (non-package, fleet of 5, TSI above 50M) until BDOI confirms |
| Nominated document naming | Q23 | `<REFERENCE>_<DOCTYPE>_<n>` (`DocumentNamingService.SYNTAX`) |
| Sales organisation and cost centers | Q34, Q41 | `cat_sales_unit` / `cat_sales_officer` tables; demo units only |
| Motor OD factors and BI / PD tables | Q35 | `cat_rate` MOTOR_OD_* and `cat_motor_limit` hold sample values |
| Premium tax base | Q43 | `cat_rate` PREMIUM_TAX per line (PROPERTY 12%) |
| Retention of accounts | nbadmin | `RetentionCandidateProvider` for ACCOUNT not implemented (port not on this branch) |

## 7. CRM (`crm`): clients

Requirements: BRNB.029, 030, 032, 046-049, 065, 090, 091, 099, 101, 110 (legacy BRD 1.1.5, 1.1.6,
2.1.5-2.1.7, 1.3.3). Migrations V800 (core), V801; demo V981.

- **Client master.** `Client` holds individuals and corporates with identity (TIN, ID type and
  number), contact and address, market segment, BDO bank client flag and CIF, and a KYC profile
  (`ClientProfile`: nationality, civil status, occupation, source of funds, risk rating; LOVs
  `NATIONALITY`, `CIVIL_STATUS`, `SOURCE_OF_FUNDS`, `KYC_RISK_RATING`). `ClientRules` checks the TIN
  format `000-000-000-000`, e-mail, mobile `09xxxxxxxxx` / `+639xxxxxxxxx`, birth date in the past
  and the minimum age of an individual (`CLIENT_MIN_AGE`, 18). A prospect needs only the type and
  the name (BRNB.029); `ClientCompleteness` flags incomplete clients against the parameters
  `CLIENT_MIN_FIELDS_INDIVIDUAL` / `CLIENT_MIN_FIELDS_CORPORATE` (field codes BIRTH_DATE, TIN, ID,
  CONTACT, ADDRESS, MARKET_SEGMENT, NATIONALITY, SOURCE_OF_FUNDS, OCCUPATION).
- **Onboarding** (`ClientOnboardingService`, workflow `NB_CLIENT`, section 4). KYC documents are
  attachments of entity type `Client` registered with their `DOCUMENT_TYPE` in `crm_kyc_document`
  (`KycDocumentService`). The mandatory documents per client type are the LOVs
  `KYC_DOCS_INDIVIDUAL` / `KYC_DOCS_CORPORATE` (codes are `DOCUMENT_TYPE` codes), maintained on the
  Lists of Values screen. Confirmation issues `CL-<yyyy>-nnnnnn` (the `PR-` code is kept) and opens
  the client's party (`PartyService.createAuthorizedBySystem`, type INDIVIDUAL_CLIENT or
  CORPORATE_CLIENT, code = client code) in the same transaction. Verification sets the next review
  date from the risk rating. Deactivation needs a `CLIENT_DEACTIVATION_REASON`.
- **Duplicates** (`DuplicateCheckService`, BRNB.032). Normalised keys are stored on the client
  (`ClientKeys`: ID, mobile, name, corporate name). Hard keys (TIN, ID type + number, last + first
  name + birth date) block create, update and confirmation with `CLIENT_DUPLICATE` listing the
  existing codes; the blocked attempt is audited in its own transaction. E-mail, mobile and
  corporate name only warn. `GET /crm/clients/duplicates` feeds the live warnings of the form.
- **Tags and special instructions** (`ClientNotesService`, BRNB.091): tags from `CLIENT_TAG`,
  instructions with an `INSTRUCTION_TYPE`, text and effective dates; every change is written to
  `crm_client_note_history` (who, when, from / to) and audited.
- **Client 360** (`Client360Service`, BRNB.099): records of every `ClientRecordsProvider` bean and
  warnings for missing linkages (confirmed without party, party missing or inactive, incomplete
  information, KYC expired). History = audit trail under the prospect and client codes.
- **KYC review** (BRNB.110): monthly job `KYC_REVIEW_DUE` expires overdue KYC and notifies the
  `CLIENT_MAINTAIN` holders with the count of non-bank clients due (window `KYC_DUE_WINDOW_DAYS`).
  Report `NB-KYC-DUE` (category Control & Audit) gives download and print.
- **Bulk** handler `CLIENT_CREATE` (BRNB.047/065, permission CLIENT_MAINTAIN): upsert by prospect or
  client code (blank cells keep values), else by a single hard duplicate key, else a new prospect.
- **Retention**: `ClientRetentionProvider` implements `nbadmin.service.RetentionCandidateProvider`
  for record type `CLIENT` (last activity = last update).

API (`/api/v1/crm`): `GET clients` (search), `GET/PUT clients/{id}`, `POST clients`,
`GET clients/duplicates`, `GET clients/{id}/kyc-checklist`, `POST clients/{id}/kyc-documents`
(multipart), `POST clients/{id}/submit-kyc | verify-kyc | confirm | deactivate`,
`GET clients/{id}/instructions` (banner), `GET clients/{id}/notes`, `POST clients/{id}/tags`,
`POST clients/{id}/tags/{code}/remove`, `POST clients/{id}/instructions`,
`PUT clients/{id}/instructions/{iid}`, `POST clients/{id}/instructions/{iid}/end`,
`GET clients/{id}/records`, `GET clients/{id}/history`, `GET kyc-reviews`. The W1 lookup endpoints
(`clients/lookup`, `clients/{id}/summary`) are unchanged.

Contracts for other modules:

- `ClientService`: W1 methods unchanged (`createProspect` now also validates, blocks hard
  duplicates and opens the onboarding case); new `create(companyId, ClientDetails, ClientProfile)`
  and `update(id, ClientDetails, ClientProfile)`.
- `ClientRecordsProvider` (port, W1): implement it to list your records on the client page.
- Frontend: `components/broking/InstructionsBanner` (`clientId`) shows tags and instructions in
  force; `components/broking/bannerKey` is its query key; `api/clients.ts` (`clientsApi`) is the
  full client API.

Screens (section Clients): Clients, New Client, KYC Reviews Due, client page (header with codes and
badges, actions by permission, `WorkflowPanel`, tabs Details / KYC & Documents / Tags &
Instructions / Linked Records / History).

Demo (V981): 12 clients of company FVI: six confirmed (with authorized parties
`CL-2026-000001..006`; `CL-2026-000002` has an expired KYC), one prospect in KYC review
(`PR-2026-000007`, for `mkttl` to verify), one verified prospect (`PR-2026-000008`, to confirm),
three prospects, one dormant since 2019 (`PR-2026-000011`) and one inactive client since 2020
(`PR-2026-000012`) for the retention review. Tags and instructions on `CL-2026-000001/2/3/5/6`.

## 8. Broking administration (`nbadmin`)

Requirements: BRNB.040, 079, 082, 083, 085, 086, 089, 106 (legacy BRD 3.3, 3.4). Migration V790.

- **Lists of Values** screen (`/broking-setup/lists?type=`) on the W1 `lov` API: add, change,
  deactivate (LOV_MANAGE) and authorize (MASTER_AUTHORIZE, not the maker).
- **User access requests** (`AccessRequestService`, BRNB.085): CREATE_USER, MODIFY_ROLES,
  DISABLE_USER, ENABLE_USER with a justification (ACCESS_REQUEST); decided by ACCESS_APPROVE, never
  the requester. Approval applies the change through `UserAdminService` (`AccessChangeApplier`); a
  created user gets a 14-character temporary password returned once to the approver and never
  stored in clear. Pending requests appear in My Approvals (`AccessRequestApprovalSource`). Requester
  and approvers are notified.
- **User Access Matrix** (BRD 3.3.4): roles x permissions with enabled users per role, read-only,
  Excel export (`GET /nbadmin/access-matrix/export`, audited).
- **Session policy** (BRNB.040): `GET /system/session-policy` returns the inactivity timeout, the
  warning lead (`SESSION_TIMEOUT_MINUTES` minus `SESSION_IDLE_WARNING_MINUTES`, i.e. a warning after
  15 minutes of inactivity) and `expiryWarningMinutes` (`SESSION_EXPIRY_WARNING_MINUTES`, 30): the
  web client warns before the absolute sign-out at the token expiry.
- **Multi-tab session** (BRNB.082, frontend `session/tabSync.ts`): the token stays in each tab's
  sessionStorage; a new tab asks the open tabs for it over the `BroadcastChannel`
  `brokerverse.session` (request / share handshake); sign-in, sign-out and activity are broadcast,
  so signing out in one tab signs out all and activity in one tab keeps all signed in.
- **Data retention** (BRNB.106): rules in `nba_retention_rule` (record type, statuses, years online /
  archive, REVIEW or ARCHIVE; seeded 5 / 15 years), results in `nba_retention_run`. Port
  `nbadmin.service.RetentionCandidateProvider` (record type, `countEligible`, `eligible`) is
  implemented by `crm` for CLIENT; QUOTATION, PROPOSAL and ACCOUNT rules show "module not yet
  reporting" until those modules implement it. Monthly job `RETENTION_REVIEW`. **Parked:** physical
  archive and purge wait for the DBA's storage and backup decision; nothing is ever deleted.
- **Audit log report** (BRNB.086/089): the existing report `CTL-AUDIT` (date range, user, entity
  type; PDF / Excel / CSV) is offered as download buttons on Administration > Audit Trail.

API (`/api/v1/nbadmin`): `GET/POST access-requests`, `GET access-requests/{id}`,
`POST access-requests/{id}/approve | reject`, `GET users`, `GET roles`, `GET access-matrix`,
`GET access-matrix/export`, `GET retention/rules`, `PUT retention/rules/{id}`,
`GET retention/rules/{id}/eligible`, `POST retention/review`.

Screens (section Broking Setup): Lists of Values, Access Requests, User Access Matrix, Data
Retention. A screen may declare `alsoPermissions` (navigation) so the checker of a maker screen
(e.g. the Approver on Access Requests) can open it.

## 9. Catalog (`catalog`, W2)

What BDOI sells and with whom. Every table is a maker-checker master (`AuthorizableEntity`): new and
changed rows wait in My Approvals (`CatalogApprovalSource`). They are authorized or deactivated through
`POST /api/v1/catalog/records/{kind}/{id}/authorize|deactivate`, and rating and accounts use only
ACTIVE rows. Rates are effective-dated, and the row in force on the period start applies.

| Table (V810-V812) | Content | Seeds |
|---|---|---|
| `cat_product_line` | line, risk item kind (VEHICLE / PROPERTY_LOCATION / PERSON / GENERIC), rating method (PROPERTY / MOTOR / GENERIC) | 12 Annex I lines |
| `cat_cover_type` | cover types per line | Annex I |
| `cat_product` | features: package, fleet, market segments, mortgage, direct payment, multi-year and maximum term, FFY, payment gate (PAID / CLIENT_CONFIRMATION), default rate, commission and minimum premium, package TSI limit, TSU involvement | Annex I codes, MTR / PAR packages (TSI limits PAR 20M, MTR 5M) |
| `cat_field_rule` | minimum-field matrix: scope ALL (`*`) / LINE / PRODUCT, target ACCOUNT / ITEM, field key (`a\|b` = one of) | defaults per line |
| `cat_document_rule` | documents required before submission | MOTOR → IDF, PERSONAL_ACCIDENT → VALID_ID |
| `cat_insurer`, `cat_insurer_branch` | insurer panel: party (type INSURER), accreditation, placement channel and e-mails, credit days; branches with LGT rate | demo V982 |
| `cat_commission_rate` | commission per insurer × product (product blank = all products), effective-dated | demo V982 |
| `cat_rate` | DST, premium tax, VAT on premium, fire service tax, VAT on commission, motor OD factors; line blank = all lines | DST 12.5, VAT 12 (PROPERTY 0), PTX PROPERTY 12, FST PROPERTY 2, OD 90 / 81 |
| `cat_short_period_rate` | % of annual premium by months covered | 1-12 months: 20 … 100 |
| `cat_motor_limit` | BI / PD limit premiums | sample tables |
| `cat_tsu_rule` | TSU routing criteria: product class, line, fleet units, locations, TSI above, endorsement type, priority | NON_PACKAGE, FLEET_5, TSI_50M |
| `cat_sales_unit`, `cat_sales_officer` | region → department → team with cost center (inherited), officers per team | demo V982 |

Contracts for other modules:

- `ProductCatalogService.requireProduct(code)` and `requireUsableProduct(code)` (ACTIVE only), plus
  `products(ProductFilter)`.
- `ProductRuleService.effectiveFieldRules(product)`, `requiredDocuments(product)` and
  `missingFields(product, FieldPresence)`: field errors keyed by field path.
- `InsurerService.requireUsableInsurer(companyId, partyCode)`, `requireUsableBranch(...)` and
  `panel(companyId)`: the active insurers, each with its active branches.
- `RateResolver`:
  - `rate(RateCode, lineCode, date)`
  - `commission(companyId, insurerCode, productCode, date)`: the product row first, then the insurer-wide row.
  - `shortPeriodPercent(months, date)`
  - `motorLimitPremium(coverage, limit, date)`
- `RatingService.rate(RatingQuery)`: resolves the rates and rates the risk. Also over HTTP:
  `POST /api/v1/catalog/rating/quote`.
- `PremiumCalculator` is pure. It rounds DST to centavos and then up to the next 0.50. The
  minimum premium does not apply to endorsements or pro-rata periods.
- Endorsements: pass `endorsement = true` and
  `PremiumRequest.Period.remainingTerm(basis, effective, expiry, shortPeriodPercent)`. The basis
  is pro-rata days over the year that starts on the effective date, or the short-period table. A
  negative sum insured gives a return premium, and the minimum premium does not apply.
- `TsuRoutingService.evaluate(product, TsuFacts)` returns a `TsuDecision`. The product setting
  ALWAYS / NEVER is checked first. A package above its TSI limit then gives `PACKAGE_TSI_LIMIT`.
  Otherwise the first matching rule by priority applies.
- `SalesOrganisationService.assignmentOf(companyId, username)` returns the region, department,
  team and cost center.

API: `/api/v1/catalog/**`. The endpoints are `lines`, `cover-types`, `products[/{code}]`,
`field-rules`, `document-rules`, `tsu-rules`, `insurers[/{id}]` (with `/branches` and
`/commissions`), `rates/taxes`, `rates/short-period`, `rates/motor-limits`,
`sales-organisation[/assignment|/units|/officers]` and `rating/quote`. Read needs any of
MASTER_VIEW, ACCOUNT_VIEW, QUOTE_VIEW or TSU_PROCESS. Maintain needs MASTER_MAINTAIN, and
authorize needs MASTER_AUTHORIZE.

Screens (sidebar **Products & Insurers**):

- Products: list with filters, plus field, document and TSU rules tabs.
- Product detail: features, field matrix and required documents.
- Insurers: list, and a profile with branches / LGT and commission rates.
- Rates & Taxes.
- Sales Organisation.
- Premium Calculator.

## 10. Accounts (`account`, W2)

An account (ARN `ARN-yyyy-nnnnnn`) is one client, one product and one or more risk items:

- `VEHICLE`: plate, conduction sticker, engine and chassis numbers, stored in a normalised form for
  duplicate checks.
- `PROPERTY_LOCATION`: address, occupancy, construction, and the insured items inside.
- `PERSON`
- `GENERIC`

Tables (V820):

- `acc_account`: premium breakdown, FFY, contact, sales stamp, TSU clearance and lifecycle columns.
- `acc_account_pn` and `acc_account_policy`
- `acc_risk_item` and `acc_risk_item_detail`

LOVs (V820): VEHICLE_BODY_TYPE, CONSTRUCTION_CLASS, OCCUPANCY, MORTGAGEE_BANK and
FFY_CANCEL_REASON.

The `acc_account.status` column mirrors the `NB_ACCOUNT` work case (`AccountStatusListener`), and
every change publishes `AccountStatusChanged(accountId, arn, from, to, …)`.

Rules:

- **Draft**:
  - The account is created by `AccountService.createDraft(NewAccount)`, for a confirmed client or a prospect.
  - The product must be usable and allowed for the market segment.
  - Direct payment, FFY and multi-year are accepted only when the product allows them.
  - The account is priced on every save, stamped with the creator's sales units and cost center,
    and given its work case.
- **Duplicates** (`DuplicateCheckService`):
  - A vehicle identifier or location key already on a live account (not closed or voided) is
    refused with `DUPLICATE_ACCOUNT`, and the message names the existing ARN(s).
  - Exception: CTPL may share a vehicle with a non-CTPL account.
  - Endorsements get the findings back instead of an error.
- **Submit / resubmit** (Marketing) require:
  - every mandatory field of the matrix (`ACCOUNT_INCOMPLETE`, errors keyed by field path);
  - every required document (`MISSING_DOCUMENTS`);
  - a rated premium (`PREMIUM_NOT_RATED`).
- **Validate** (Processing):
  - The client must be confirmed.
  - TSU must have cleared the account when a rule or the package limit requires it (`TSU_CLEARANCE_REQUIRED`).
  - A direct-payment account then moves on with the system action `payment_confirmed`.
- **Direct booking** requires an attached POLICY_COPY or EPOLICY.
- **Tags** (`AccountTaggingService`): FFY tag or cancel with a reason, the payment arrangement
  (`VIA_BDOI` or `DIRECT_TO_INSURER`, giving the `directPayment` flag), and look-up by vehicle.
- **Bulk handlers**:
  - `ACCOUNT_CREATE` takes the parameters product, segment and submit. It uses a client by code,
    matches a client by name and birth date, or creates a new prospect.
  - `ACCOUNT_UPDATE`
  - `FFY_TAGGING`

Contracts for other modules:

- `AccountService.createDraft(NewAccount)`: for quotation and nonpackage. `NewAccount.direct()` is
  for direct creation.
- `AccountQueryService`, read-only:
  - `get(id)` and `requireByArn(arn)`: with items and numbers loaded.
  - `search(AccountSearch, Pageable)`
  - `byClient(clientId)`
  - `check(id)`
  - `preBooked(companyId, reference)`: live, not yet booked accounts found by ARN, policy number or
    PN number. This serves Cashiering CSHID.020 and Prod Recon PRCID.023.
- `Account.isDirectPayment()`: the direct payment flag (BRNB.114).
- `AccountLifecycleService`: placement, issuance and booking move the account through it. It takes
  the ARN and runs a user transition when a user is signed in, otherwise a system transition.
  - `markPaymentConfirmed`
  - `recordPlacement`
  - `recordInsurerReturn`
  - `resubmitPlacement`
  - `cancelPlacement`
  - `reactivate`
  - `recordHoldCover`
  - `recordPolicy`
  - `recordBooking`: validates the cost center.
  - `recordCancellation`
- `AccountClientRecords` implements the crm `ClientRecordsProvider`, which gives the accounts in
  the client 360 view.

API: `/api/v1/accounts`:

- `GET` search, with text / pn / vehicle / location / product / line / insurer / status / ffy /
  directPayment / officer / mine / includeVoided and the period.
- `GET` `/{id}`, `/by-arn/{arn}` and `/{id}/check`.
- `POST` create and `PUT /{id}`.
- `POST /{id}/submit`, `/resubmit`, `/validate`, `/direct-booking` and `/tsu-clearance`.
- `PUT /{id}/ffy` and `POST /{id}/ffy/cancel`.
- `PUT /{id}/payment-arrangement`.

Documents (`attachment`, V27):

- Document type (list DOCUMENT_TYPE).
- Multi-file upload: `POST /api/v1/attachments/batch`.
- Names inherited, or nominated as `<REFERENCE>_<DOCTYPE>_<n>.<ext>`.
- One file linked to several records (`POST /attachments/{id}/links`), and unlinking.
- ZIP download of chosen files (`GET /attachments/zip?ids=`), up to 50 files.
- MSG, EML and legacy Office types added.
- `DocumentService.documentTypesOf(target)` feeds the document rules.

Screens (sidebar **Accounts & Placement**):

- Accounts: search panel, quick filters (My drafts, Returned to me, Awaiting payment, FFY, Direct
  payment), status badges and ARN chips.
- New Account: a six-step wizard with autosave every 30 seconds and a duplicate fall-out that links
  to the existing ARN.
- Account detail: header, `WorkflowPanel` with Submit / Resubmit / Validate / Direct booking, and
  tabs Details / Risk items / Premium / Documents / E-mails / History.
- FFY Register.
- Direct Payment.
- `/bulk/ACCOUNT_CREATE`: the bulk page with product, segment and submit parameters.
- The shared `<Attachments>` component gains a document type choice, multi-file upload, selection
  with ZIP download, and a linked-file marker. Its existing props are unchanged.

## 13. Placement (`placement`, W3)

Placement takes a validated account from *awaiting payment* to *placed with the insurer*. It
never changes the account itself: every state change goes through `AccountLifecycleService`.

Tables (V850):

- `plc_payment_gate_rule` and `plc_payment_evidence`
- `plc_billing_batch` / `_item` and `plc_payment_report` / `_line`
- `plc_slip` / `_account` / `_file`
- `plc_hold_cover` and `plc_insurer_return`

Parameters (category PLACEMENT): `HOLD_COVER_DAYS` (30) and `HOLD_COVER_ALERT_DAYS` (5). LOV
`CLIENT_CONFIRMATION_CHANNEL`; document types `PAYMENT_CONFIRMATION` and `HOLD_COVER`; return
reason `INSURER_UNDERWRITING`.

Rules:

- **Payment gate** (BRD 2.3.1, BRNB.068/114, `PaymentGateService`):
  - The rule comes from `plc_payment_gate_rule` by segment and line, highest priority first.
    Seeds: CBG Property and CBG Motor need `PAYMENT_MATCHED`; everything else needs a
    `CLIENT_CONFIRMATION` (channel, remarks, optional supporting document).
  - Direct-payment accounts skip the gate. `/direct-payment` releases an account tagged after
    validation.
  - Every decision is kept as evidence (kind, source, reference). Opening the gate calls
    `markPaymentConfirmed`.
  - Placement is not a payment intake: no receipts and no cash entries.
- **CLPC billing** (BRNB.067, `BillingService`):
  - Candidates are CBG Property accounts awaiting payment that are not already on an unpaid
    billing item.
  - A batch `BILL-yyyy-nnnnnn` holds PN no., loan application no., booking date, borrower,
    originating unit, premium, ARN, BDOI location and amortised Y/N.
  - The file downloads as .xlsx or .ods.
- **Payment reports** (BRNB.067/068, `PaymentReportService`), uploaded as .xlsx, .csv or .ods:
  - `CLPC` reports answer a billing batch and match by PN or loan application no.
  - `REFERENCE` reports (other segments) match by ARN.
  - Lines are MATCHED, UNMATCHED or AMBIGUOUS, and the review resolves a line by hand.
  - Confirm opens the gate of each matched paid line (through the sweep below); the report is then
    APPLIED.
- **Payment confirmation port** `PaymentConfirmationSource`:
  - Methods: `sourceCode()` and `confirmedFor(companyId, arns)`, returning `ConfirmedPayment`.
  - Operations Cashiering will add its receipt applications as another implementation.
  - Today's implementation is `ReportPaymentSource` (source `PAYMENT_REPORT`, reference
    `<reportNo>#<rowNo>`).
  - The job `PAYMENT_CONFIRMATION_SWEEP` (hourly, or `POST /placement/gate/sweep`) asks every
    source about the accounts awaiting payment. It applies each confirmation once per source and
    reference.
- **Placement slip** (BRNB.069, `PlacementSlipService`):
  - One slip `PL-yyyy-nnnnnn` per insurer branch, as PDF and XLSX from the `PLACEMENT_SLIP`
    template.
  - It is generated only when every account meets the prerequisites (`SlipPrerequisites`). Each
    unmet one has a code: `NOT_READY_FOR_PLACEMENT`, `PAYMENT_NOT_CONFIRMED` (unless direct
    payment), `DOCUMENTS_MISSING`, `TSU_NOT_CLEARED`, `INSURER_NOT_USABLE`.
  - Regenerating after a return creates the next version (`PL-2026-000001 v2`) and marks the old
    one SUPERSEDED.
- **Send** (BRNB.071):
  - E-mail to the insurer branch mailbox from `catalog`, with the attachments protected by
    `messaging`.
  - The first send calls `recordPlacement` for each account; later sends are resends.
  - The send log is the messaging outbox (entity `PlacementSlip`).
- **Hold cover** (BRNB.072/103, `HoldCoverService`):
  - Request (template `HOLD_COVER_REQUEST`, `HOLD_COVER_DAYS`), then the insurer's confirmation
    (insurer, reference, date, expiry) or decline. Each is mirrored through `recordHoldCover`.
  - Job `HOLD_COVER_EXPIRY` (daily) notifies the holders of `PLACEMENT_MANAGE` once per hold cover
    within `HOLD_COVER_ALERT_DAYS`. It expires those past their expiry while the policy is awaited.
- **Insurer returns** (BRNB.033/034, `InsurerReturnService`):
  - Reason (list RETURN_REASON) and remarks through `recordInsurerReturn`.
  - Resubmission through `resubmitPlacement`.
  - The return is resolved when the account leaves RETURNED_BY_INSURER.
- **Cancel / reactivate** (BRNB.062, BRD 2.1.16, `PlacementBatchService`): one or several
  accounts with a comment, through `cancelPlacement` / `reactivate`. Each account gets its own
  result.
- **Booking hand-off**: the workbench action *For Booking* opens `/booking?arns=<ARN,…>`.
  Placement never calls booking.

Contracts for other modules:

- `PlacementQueryService.slipsFor(arn)`, `currentSlip(arn)` and `holdCover(arn)`.
- `PaymentConfirmationSource` (port, above).
- `PaymentGateService.applyPayment(sourceCode, ConfirmedPayment)`.

API `/api/v1/placement`. Viewing needs `ACCOUNT_VIEW`, `PLACEMENT_MANAGE` or `BILLING_MANAGE`;
actions need `PLACEMENT_MANAGE`; billing needs `BILLING_MANAGE`.

- `GET /workbench?tab=&q=`, `/workbench/counts`, `/readiness?arns=` and `/accounts/{arn}`.
- `POST /accounts/{arn}/insurer-return`, `/accounts/{arn}/resubmit`, `/accounts/cancel`,
  `/accounts/reactivate`, `/accounts/{arn}/hold-cover`, `/hold-cover/confirm` and
  `/hold-cover/decline`.
- `/slips`: `GET` list and `/{id}`; `POST /generate`, `/{id}/regenerate`, `/{id}/send` and `/send`
  (bulk); `GET /{id}/email-draft` and `/{id}/files/{pdf|xlsx}`.
- `/billing`: `GET /candidates`, `/batches`, `/batches/{id}` and `/batches/{id}/file?format=`;
  `POST /batches`; `/reports` upload, list and get, `/lines/{lineId}/match`, `/confirm` and
  `/discard`.
- `/gate`: `GET /rules` and `/{arn}`; `POST /{arn}/client-confirmation`, `/{arn}/direct-payment`
  and `/sweep`.

Screens (sidebar **Placement & Booking**):

- Placement Workbench:
  - Tiles: Awaiting payment, Ready for placement, Placed, Returned by insurer, Hold cover expiring.
  - Tabs, and the bulk actions For Placement, Send Slips, For Booking, Cancel Placement and
    Reactivate.
- Placement Slips.
- CLPC Billing, with the payment report review.
- Account placement page: gate, slip, hold cover and returns.
- The Placement tab on the account detail page (`PlacementPanel`, mounted with a marked comment).

Demo data (V986):

- Accounts `ARN-2026-910001` to `910008`.
- Billing batch `BILL-2026-900001` (closed, with the matched CLPC report `PMT-2026-900001`) and
  `900002`.
- A confirmed ARN report and one in review.
- Slips `PL-2026-900001` to `900005`, sent.
- A confirmed hold cover and one about to expire.
- One account returned by the insurer.

Parked:

- CLPC SFTP transport (Q28): download and upload only.
- Insurer SFTP / API channels (Q06): a non-EMAIL channel is refused with
  `PLACEMENT_CHANNEL_PARKED`.

## 14. Issuance (`issuance`, W3)

Issuance takes a placed account to *policy issued* and sends the policy documents out. The policy
number reaches the account only through `AccountLifecycleService.recordPolicy`.

Tables (V860): `iss_extraction_pattern`, `iss_document_trigger`, `iss_epolicy`, `iss_upload_batch`
/ `_item` and `iss_insurance_advice`. Parameters (category ISSUANCE): `IA_TRIGGER`
(`ON_POLICY_ISSUE`, `ON_PLACEMENT` or `MANUAL`) and `EPOLICY_PASSWORD_HINT`. LOV
`EPOLICY_REJECT_REASON`; document type `INSURANCE_ADVICE`.

Rules:

- **E-policy receipt** (BRNB.073, `EpolicyService`):
  - A PDF is matched to the account by ARN, policy number or PN (`AccountQueryService.preBooked`),
    or chosen by hand. It is stored as document type `EPOLICY`.
  - Bulk upload (`EpolicyUploadService`) matches each file name. A review lets the user fix or
    skip items before confirming.
- **Extraction** (BRNB.074/104):
  - Port `PolicyDataExtractor`. The default `PdfTextPolicyDataExtractor` reads the PDF text
    (OpenPDF) and applies the `iss_extraction_pattern` regexes per insurer (default row `*`) for the
    policy number, the period from / to and the premium.
  - The review shows the extracted values next to the account's. Confirm calls `recordPolicy`.
  - Multi-year accounts take several policy numbers (BRNB.112).
  - Reject keeps the file with a reason.
- **Document triggers** (BRNB.105): `iss_document_trigger` maps a document type to an action.
  Seed: `EPOLICY` → `EXTRACTION_REVIEW`. Auto-booking belongs to booking, which listens for
  POLICY_ISSUED; issuance never calls booking.
- **Insurance Advice** (BRNB.060/070/095):
  - `IA-yyyy-nnnnnn`, for mortgaged accounts only (`IA_NOT_MORTGAGED`), from the
    `INSURANCE_ADVICE` template.
  - Generated automatically, before commit, on the status change chosen by `IA_TRIGGER`, or one or
    several at a time by hand.
  - Register with view, download and protected send (BRNB.035).
- **E-policy dispatch** (BRNB.077/035):
  - One or a batch, to the client contact. The attachment is encrypted and the password goes in a
    separate e-mail (standard password policy; the BDOI convention is parked, Q07).
  - The dispatch report is the messaging log filtered on the purposes `EPOLICY` and
    `INSURANCE_ADVICE`.

Contracts for other modules:

- `IssuanceQueryService.policyFor(arn)`: policy numbers, e-policy and advice.
- `PolicyDataExtractor` (port, above).
- `IssuanceClientRecords` implements `ClientRecordsProvider`: the Insurance Advices in the client
  360 view.

API `/api/v1/issuance`. Viewing needs `ACCOUNT_VIEW`, `EPOLICY_MANAGE` or `EPOLICY_SEND`; receipt
and extraction need `EPOLICY_MANAGE`; dispatch needs `EPOLICY_SEND`.

- `GET /workbench?tab=&q=`, `/workbench/counts`, `/policies/{arn}` and `/triggers`.
- `POST /epolicies` (multipart), `GET /epolicies/{id}`, `POST /epolicies/{id}/extract`,
  `/confirm` and `/reject`.
- `POST /epolicy-uploads` (multipart), `GET /epolicy-uploads/{id}`,
  `POST /epolicy-uploads/{id}/items/{itemId}`, `/confirm` and `/discard`.
- `/insurance-advice`: `GET` list, `/{id}` and `/{id}/file`; `POST /generate` and `/send`.
- `/dispatch`: `GET /{epolicyId}/draft` and `/log`; `POST /{epolicyId}` and `/batch`.

Screens (sidebar **Policy Issuance**):

- Issuance Workbench, with the tabs Placed – awaiting policy, Policy received – review, Ready to
  dispatch and IA to generate.
- E-policy Upload: single, or bulk with match review.
- Extraction Review.
- Insurance Advice register.
- Dispatch, with the dispatch report.
- The Policy tab on the account detail page (`PolicyPanel`).

Demo data (V987): three accounts POLICY_ISSUED with confirmed e-policies, one e-policy in review,
`IA-2026-900001` and `900002` for the mortgaged accounts, and one account dispatched (messaging
log).

Parked:

- Reading e-policies from a shared mailbox or SFTP (Q12, Q31): manual upload.
- OCR (Q24): text PDFs only, through the extraction port.
