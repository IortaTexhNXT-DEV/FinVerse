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
| `crm` | W1 core, W2 full | Clients: prospect / confirmed, KYC, tags, special instructions, dedupe, contacts, 360 view (BRNB.030/032/046-049/090/091/099/101/110) | party, lov, workflow | V800-V809 (V981) |
| `catalog` | W2 | Product lines, cover types, BDOI risk codes, insurer panel and branches, commission and taxes, minimum-field matrix, TSU routing rules, sales organisation, premium calculator (Appendix A) | party, lov, dimension | V810-V819 (V982) |
| `account` | W2 | Account (ARN), risk items, account contacts, duplicate rules, FFY, direct payment, multi-year, account lifecycle (BRNB.025/050-054/066/102/109/113/114) | crm, catalog, workflow, bulk | V820-V829 (V983) |
| `quotation` | W3 | Package quotations: request intake, versions, approval, send, acceptance, conversion to accounts, bulk quotations | account, crm, catalog, workflow, docgen, messaging, bulk | V830-V839 (V984) |
| `nonpackage` | W3 | PRF, TSU queue, Quotation Slip, insurer responses, comparative table, Proposal Slip (BRNB.005-018) | account, crm, catalog, workflow, docgen, messaging | V840-V849 (V985) |
| `placement` | W3 | Payment gate, CLPC billing file and payment matching, placement slip, send, hold cover, returns, cancel / reactivate placement (BRNB.033/034/062/067-072/103) | account, catalog, workflow, docgen, messaging, bulk | V850-V859 (V986) |
| `issuance` | W3 | E-policy receipt, policy number update, Insurance Advice, encrypted e-policy dispatch, dispatch report (BRNB.035/060/070/073-078/095/104/105) | account, workflow, docgen, messaging | V860-V869 (V987) |
| `booking` | W3 | Individual / batch / direct / multi-year booking, accounting events, service invoice, endorsements, cancellation, incentive flag, cost center (BRNB.027/036/038/061/076/081/094/100/107/108/111/112) | account, catalog, accounting, subledger, workflow, docgen, messaging | V870-V879 (V988) |
| `nbreport` | W4 | NB operational reports and the NB dashboard (BRNB.011/012/075/078 ...) | all broking modules | V880-V889 (V989) |
| `nbadmin` | W2 | User access requests with approval, retention policy job, session policy, multi-tab session (BRNB.082/085/106) | security, system, workflow | V790-V799 |

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
| TSU routing thresholds | Q04 | `cat_tsu_rule` seeds (non-package, fleet of 5, TSI above 50M) until BDOI confirms |
| Nominated document naming | Q23 | `<REFERENCE>_<DOCTYPE>_<n>` (`DocumentNamingService.SYNTAX`) |
| Sales organisation and cost centers | Q34, Q41 | `cat_sales_unit` / `cat_sales_officer` tables; demo units only |
| Motor OD factors and BI / PD tables | Q35 | `cat_rate` MOTOR_OD_* and `cat_motor_limit` hold sample values |
| Premium tax base | Q43 | `cat_rate` PREMIUM_TAX per line (PROPERTY 12%) |
| Retention of accounts | nbadmin | `RetentionCandidateProvider` for ACCOUNT not implemented (port not on this branch) |

## 7. Catalog (`catalog`, W2)

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

## 8. Accounts (`account`, W2)

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
