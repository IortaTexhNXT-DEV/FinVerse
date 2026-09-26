# Claims Handling (BRD-7)

Guide of the Claims Handling module of iNXT BrokerVerse (`brokerclaims`): BDOI's case file for a
client's claim against its insurers. It covers the cover lookup, the premium check and the claims
authorization code, the claim record with its locations and insurer claims, the status, settlement
and closure, follow-up and diary, and the Claims Handling reports. The build design is
[`docs/architecture/CLAIMS_BROKING_DESIGN.md`](../architecture/CLAIMS_BROKING_DESIGN.md). Its
sections 17 to 19 record what each wave built, and section 14 gives the CL2 integration wave. The
requirements and their as-built status per BRCLM ID are in
[`docs/requirements/BDOI_CLM_BRD_SPEC.md`](../requirements/BDOI_CLM_BRD_SPEC.md). The functional
specification is [`FRS_BRD07_CLAIMS.md`](../deliverables/src/frs/FRS_BRD07_CLAIMS.md), with
requirements FR-CM-001 to FR-CM-066.

The insurer-side Claims module (`claims`, `docs/modules/CLAIMS.md`) is a different module. It
models an insurer's own claims with reserves in the general ledger. BDOI roles never see it.

## 1. Purpose

BDOI is a broker. The insurer decides and pays a claim; BDOI records it, coordinates it and follows
it up for the client. Before BrokerVerse this work was kept in spreadsheets and e-mail. The module:

- finds the **cover** (account ARN and policy year) of a loss, read-only, with the policy number,
  cover version at the loss date, Marketing team, AO, branch and the premium position of the year;
- **records the claim** (`BCL-<yyyy>-nnnnnn`) with its loss, claimant, catastrophe tag, several
  insured locations and several insurers, each with its own claim number, reserve, settlement and
  adjuster;
- **checks the premium**. A claim on an unpaid cover is saved, flagged and alerted. The **claims
  authorization code** (`CAC-<yyyy>-nnnnnn`) is generated only when the premium of the policy year
  is paid. A direct-payment cover follows parameter `BCL_AUTH_DP_POLICY`;
- runs the **BDOI status list** (18 statuses in four phases) under a role / unit **status access
  matrix**. The requested **type of settlement** closes a claim permanently; two statuses close it
  temporarily. A closed claim is **reopened** with a reason;
- keeps the **follow-up date**, the **next action plan**, a **diary** per claim and handler, and the
  **ages** (overall and this stage), with daily reminders and alerts;
- asks for the **claims special remittance**. A claim "With BDOI - For Premium Remittance" confirms
  the Marketing Collections request through the claims feed, and the handler is told when the
  premium is fully remitted;
- answers management, Marketing and Renewal with twelve **Claims Handling reports** and the loss
  experience of a cover.

Claims Handling posts **no accounting entry** and keeps no reserve of its own (design section 6).
The insurer's reserve and settlement are the insurer's figures, kept for monitoring and loss
experience.

## 2. Personas and roles

| Persona (BRD) | Role | SIT/UAT user(s) | Unit (handler register) | What they do |
|---|---|---|---|---|
| Claims Officer / Assistant | `CLM_OFFICER` | `clmofficer`, `clmofficer2`, `clmbranch` | Motor HO, Non-Motor HO, Cebu | Look up covers, record claims, authorization code, the newly filed and temporary closure statuses (matrix), action plan, diary, insurer claim numbers and updates, loss advice, location references, bulk uploads, reports |
| Claims Team Lead | `CLM_TL` | `clmtl` | Motor HO | As the officer, plus every status, permanent closure, claimant override, settlement, adjuster, follow-up override, reserve, reassignment |
| Claims Team Head | `CLM_TH` | `clmth` | Non-Motor HO | As the Team Lead, plus reopen (CLQ06) |
| Claims Unit Head | `CLM_UH` | `clmuh` | - | Claims Setup (status and settlement attributes, status access matrix, handler register, Claims lists), reopen, reassignment, reports and the data extract |
| Claims / Risk user | `CLM_RISK` | `clmrisk` | - | Read claims and covers, claims-prone locations and loss reports, data extract |
| Marketing Account Officer | `MKT_AO` (+ `BCL_REPORT_VIEW`) | `ao`, `ao2` | - | Runs the Claims Handling reports on screen (no export), loss experience of an account |
| Marketing Team Lead | `MKT_TL` (+ `BCL_REPORT_VIEW`, `BCL_REPORT_EXPORT`) | `mkttl` | - | As the AO, plus export (CLQ15) |
| System Administrator, Auditor | `SYSADMIN`, `AUDITOR` (+ `BCL_VIEW`, `BCL_REPORT_VIEW`) | `admin`, `auditor` | - | Read access; the administrator runs the jobs |

The SIT/UAT password is held in the seed configuration. The 18 `BCL_*` permissions and the grants per role
are in design section 7. The claims roles also have `WORK_VIEW`, `ATTACHMENT_VIEW`, `REPORT_VIEW`,
`CLIENT_VIEW`, `ACCOUNT_VIEW` and `OPS_VIEW`. The officer, TL and TH also have `ATTACHMENT_MANAGE`
and `BULK_PROCESS`.

The permissions of each role and the menu it sees are pinned by the persona check (client
requirement 14), suite **BRD-7** of
[`frontend/src/navigation/personaMenus.json`](../../frontend/src/navigation/personaMenus.json). It
is tested by `navigation/personaMenus.test.ts` (menu) and `security/PersonaMenusIT` (database
grants, one read per screen, Marketing reaches the Claims reports only). The Marketing roles are
pinned on their `BCL_*` grants only (`permissionScope`).

## 3. Screens

Group **Claims & Insurance**, section **Claims Handling**, listed first
(`features/brokerclaims/module.ts`):

| Screen | Route | Permission | What it shows |
|---|---|---|---|
| Claims Home | `/claims-handling` | BCL_VIEW | Tiles: my open claims, follow-ups due today and overdue, my diary due, temporarily closed, unpaid premium, awaiting premium remittance. Open claims by status and phase; ageing buckets |
| Claims Worklist | `/claims-handling/worklist` | BCL_VIEW | Tabs My Claims, Open, Temporarily Closed, Closed, Follow-ups Due, All; search by claim number, insurer claim number, ARN, policy number or assured; tile filters; bulk Reassign (WORK_ASSIGN) |
| Record Claim | `/claims-handling/new` | BCL_RECORD | Cover search, then the cover card with the premium check, the locations picker, the loss, the insurers proposed from the invoice shares, and a confirmation |
| Claim | `/claims-handling/:id` | BCL_VIEW | Summary card (claim number, status pill, flags *Unpaid premium*, *Awaiting premium remittance*, *Newer cover version*, *Multi-location*, *Multi-insurer*, *CAT*), status panel, workflow panel, actions, and the tabs Details, Locations, Insurers & Updates, Reserve & Settlement, Documents, Diary and History |
| Cover Lookup | `/claims-handling/covers` | BCL_COVER_VIEW | Read-only cover: account, policy years, items and locations, endorsements, invoices with payment and remittance status, claims of the cover, insurer location references |
| My Diary | `/claims-handling/diary` | BCL_VIEW | The user's diary entries across claims, due today and overdue |
| Insurer Location References | `/claims-handling/location-refs` | BCL_LOCATION_REF_MAINTAIN | Search, add (end-dates the open reference), upload |
| Claims Reports | `/claims-handling/reports` | BCL_REPORT_VIEW or BCL_DATA_EXTRACT | The Report Centre filtered to category **Claims Handling** |
| Claims Setup | `/claims-handling/setup` | BCL_SETUP | Status Attributes, Settlement Types, Status Access Matrix (maker-checker), Claims Handler Register, Claims Lists (through the list-of-values API with owner permission `BCL_SETUP`) |

Claim actions (`record/ClaimActions.tsx`, `status/ClaimStatusActions.tsx`): Generate Authorization
Code, Send Loss Advice, Refresh Cover Data, Use Latest Version, Change Status, Set Settlement,
Override Follow-up Date, Assign Adjuster, Reopen. When the status awaits the premium remittance and
an invoice of the cover is not fully remitted, the claim shows the **Request Special Remittance**
link.

Outside the module:
- **Account page, Claims tab** (`features/accounts/AccountDetailPage.tsx`, panel
  `features/brokerclaims/account/AccountClaimsPanel.tsx`, wave CL2). It is shown with `BCL_VIEW`.
  It is a read-only list of the account's claims: claim number linked to the claim record, policy
  year, date of loss, status, phase, currency, paid and outstanding, with a one-line summary. It
  reads `GET /api/v1/broker-claims/experience?arn=`.
- The **client 360** view lists the client's claims through `BrokerClaimClientRecords`.

Help: `features/brokerclaims/help.ts` (`BROKER_CLAIMS_HELP`), one entry per menu screen.

## 4. Flows

The whole path runs through the HTTP API in `api/ClaimsEndToEndApiIT`. An account is booked, the
claim is blocked on the unpaid cover, cashiering applies the payment, the claim is authorised,
remitted specially and worked through the matrix, then settled, reopened and settled again, and the
reports are checked after each step.

### 4.1 Cover, recording and premium (BRCLM.001-004, 007-009, 016, 039; FR-CM-010-016)

1. The officer searches the cover by ARN, policy number or assured, with at least 3 characters
   (`BCL_SEARCH_TOO_SHORT`). There is no branch or portfolio restriction.
2. Record Claim takes the cover (ARN + **policy year**, 1 = first year of the term), the loss
   (loss date not in the future, reported date not before the loss date), the nature of loss,
   claim type and description, the locations of that cover only, and the insurers. The insurers
   are proposed from the first live invoice's shares, else the lead insurer at 100 %.
3. A loss date outside the policy year is refused (`BCL_LOSS_OUTSIDE_COVER`) unless confirmed. The
   claim gets its number, the handler (the recorder), the unit and branch from the handler register
   and `sec_user.home_branch_id`, and the first status (`NEW_INCOMPLETE_DOCS` by default). It also
   gets a workflow case `BCL_CLAIM` in stage NEW and an audit entry. The account officer is told
   (`BCL_CLAIM_ASSIGNED`).
4. The **premium check** reads the ledger invoices of the ARN and policy year (cancelled
   excluded): none = NO_INVOICE, any UNPAID = UNPAID, any PARTIALLY_PAID = PARTIALLY_PAID, all
   direct payment = DIRECT_PAYMENT, else PAID. A blocking result raises the alert
   `BCL_UNPAID_PREMIUM_CLAIM:<claim id>`.
5. Cashiering posts the payment. `InvoiceMovementPosted` re-checks the open claims of the invoice's
   cover after commit. When the check turns PAID, the handler is told ("Premium of claim ... is
   paid"). The daily job `BCL_PREMIUM_RECHECK` is the safety net.
6. **Generate Authorization Code** (`BCL_AUTHORIZE`) issues `CAC-<yyyy>-nnnnnn` once, only when
   the check is PAID, or DIRECT_PAYMENT under `BCL_AUTH_DP_POLICY` (ALLOW; CONFIRM = an attachment
   of the claim as the insurer's payment evidence; BLOCK).

### 4.2 Locations, insurers and insurer communication (BRCLM.037, 041-043; FR-CM-020-024)

- Locations are linked from the cover's location items only. Each shows the insurer location
  reference valid on the loss date (`bcl_location_ref`; a new reference end-dates the open one).
- Each insurer line has its share, insurer claim number, date reported to the insurer, reserve
  (history in `bcl_reserve_change`, no journal), settled amount and adjuster. A number already on
  the claim is refused. A number found on another claim of the same insurer needs confirmation and
  raises `BCL_INSURER_CLAIM_NO_REUSED`.
- Insurer updates are insert-only (date, source, reference, remarks, attachments). They are
  allowed on closed claims.
- The loss advice is generated per insurer from template `BCL_LOSS_ADVICE` as a PDF on the company
  letterhead. It is e-mailed through messaging and stored as document type `CLAIM_REPORT` on the
  claim, the account and the client.

### 4.3 Status, settlement, closure and reopen (BRCLM.005, 010-015, 035; FR-CM-040-045)

1. **Change Status** lists the statuses the matrix gives one of the user's roles in the user's unit
   of the handler register (`bcl_status_access`; a null unit means any unit). By default officers
   may set the four newly filed and temporary closure statuses, and TL / TH all 18. A user outside
   the register gets `BCL_UNIT_NOT_SET`. A status outside the matrix gets `BCL_STATUS_NOT_ALLOWED`.
   Once the claim has left NEW, a newly filed status is refused (`BCL_STATUS_BACK_TO_NEW`).
2. Each change writes `bcl_status_history` with the days spent in the previous status, moves the
   workflow stage to the phase, resets "age this stage" and recomputes the next follow-up date (the
   status attribute `follow_up_days`, else `BCL_FOLLOW_UP_DAYS`) unless it was overridden. It tells
   the account officer (`BCL_STATUS_CHANGED`) and publishes `ClaimStatusChanged`.
3. Statuses 17 and 18 (phase TEMP_CLOSED) close the claim temporarily; the claim keeps ageing. Any
   in-progress status resumes it.
4. **Set Settlement** (`BCL_SETTLEMENT_UPDATE`) records the requested type of settlement. A type
   with `requires_settlement_amount` needs the amount and the date settled. A type with
   `closes_claim` also needs `BCL_CLOSE` and closes the claim permanently (phase CLOSED, closure
   date today, status kept). `SETTLED_LOA_REPAIR_SCHEDULE` and `SETTLED_LOA_UNDER_REPAIR` do not
   close until CLQ05 is answered.
5. **Reopen** (`BCL_REOPEN`, reason from `BCL_REOPEN_REASON`) moves a closed claim back to
   IN_PROGRESS. The settlement is cleared on the claim and kept in the timeline, and the handler is
   told.

### 4.4 Claims special remittance (BRCLM status 11, OQ46, MKTID.009; FR-CM-046)

1. The handler sets **With BDOI - For Premium Remittance** (`BDOI_PREMIUM_REMITTANCE`, attribute
   `awaiting_premium_remittance`). The claim shows the unremitted invoices of the cover and the
   Request Special Remittance link.
2. Marketing Collections requests a special remittance with condition **CLAIMS** for the invoice.
   The remittance module asks the claims feed (`ClaimsFeed`, implemented by `InAppClaimsFeed`, feed
   `CLAIMS_SPECIAL_REMIT`), which confirms it ("claim confirmed by the Claims system"). Without such
   a claim, the request is refused (`SPECIAL_REMIT_NOT_ELIGIBLE`, "the Claims system has no claim
   for it").
3. The remittance approver approves the request, which makes a batch. The batch is submitted and
   approved. The Disbursement DV marks the invoice FULLY_REMITTED.
4. `RemittanceStatusChanged` to FULLY_REMITTED notifies the handlers of the claims in an awaiting
   status (`BCL_PREMIUM_REMITTED`, "Premium of claim ... remitted"). The claim's *Awaiting premium
   remittance* flag and link drop once no invoice of the cover is left unremitted (wave CL2). The
   home tile and the worklist filter keep counting the claim while it stays in the status: they are
   the handler's to-do list until the status is changed.

### 4.5 Follow-up, diary and ageing (BRCLM.019-022, 025, 027, 034; FR-CM-050-055)

- The next follow-up date is computed at each status change. A TL / TH may override it with a
  reason (`BCL_FOLLOW_UP_OVERRIDE`). The next action plan has up to 2,000 characters and every
  version is kept.
- A diary entry is a call, e-mail, meeting, note or follow-up, with a due date and an assignee (the
  assignee is notified). The assignee or the author marks it done.
- Age overall = (closure date or as-of date) - reported date; age this stage = as-of - status since.
  Both are in calendar days.

## 5. Jobs

| Job | Cron property (UTC; PHT time) | Work |
|---|---|---|
| `BCL_PREMIUM_RECHECK` | `brokerverse.jobs.bcl-premium-recheck-cron` `0 30 21 * * *` (05:30) | Re-runs the premium check of open claims without an authorization code |
| `BCL_FOLLOW_UP_DUE` | `brokerverse.jobs.bcl-follow-up-due-cron` `0 0 22 * * *` (06:00) | Notice `BCL_FOLLOW_UP_DUE` for follow-ups and diary entries due on the business date; alert `BCL_FOLLOW_UP_OVERDUE` once per claim for past dates |
| `BCL_AGEING_ALERTS` | `brokerverse.jobs.bcl-ageing-alerts-cron` `0 0 22 * * *` (06:00) | Alert `BCL_CLAIM_PAST_DUE` once per outstanding claim older than `BCL_PAST_DUE_DAYS`, notice to the handler |

Jobs run on one instance at a time (job lock). They can be run on demand from Administration > Job
Monitor (`POST /api/v1/system/jobs/{name}/run`). The crons are documented in
`docs/operations/CONFIGURATION.md`.

## 6. Parameters and lists of values

| Parameter (category CLAIMS_HANDLING) | Delivered value | Use |
|---|---|---|
| `BCL_DEFAULT_CURRENCY` | PHP | Claim currency when the cover has none |
| `BCL_FOLLOW_UP_DAYS` | 7 | Days to the next follow-up when the status has no `follow_up_days` (CLQ07) |
| `BCL_AGEING_BUCKETS` | 30,60,90,180 | Age brackets of the home and `BCL-AGEING` |
| `BCL_PAST_DUE_DAYS` | 90 | Past due threshold (report and alert) |
| `BCL_PRONE_MIN_CLAIMS` / `BCL_PRONE_YEARS` | 3 / 3 | Claims-prone location flag (CLQ16) |
| `BCL_AUTH_DP_POLICY` | CONFIRM | Authorization on a direct-payment cover: ALLOW / CONFIRM / BLOCK |

Lists of values, maintained by the Unit Head through Claims Setup > Claims Lists (owner permission
`BCL_SETUP`, maker-checker): `BCL_CLAIM_STATUS` (18) and `BCL_SETTLEMENT_TYPE` (10), both with
behaviour attributes in `bcl_lov_attribute`. The status attributes are `phase`, `waiting_on`,
`follow_up_days` and `awaiting_premium_remittance`. The settlement type attributes are `outcome`,
`closes_claim` and `requires_settlement_amount`. Attribute changes go through maker-checker in
`bcl_attribute_change`. The other lists are `BCL_ADJUSTER` (25), `BCL_CATASTROPHE` (7),
`BCL_LOSS_NATURE` and `BCL_CLAIM_TYPE` (provisional, CLQ12), `BCL_UNIT` (7), `BCL_UPDATE_SOURCE`,
`BCL_DIARY_TYPE`, `BCL_DOCUMENT_TYPE`, `BCL_REOPEN_REASON` and `BCL_OVERRIDE_REASON`.

Masters: the status access matrix `bcl_status_access` (maker-checker) and the claims handler
register `bcl_handler` (user, unit, team; changes audited).

Alerts (`alt_exception_code`, module CLAIMS_HANDLING): `BCL_UNPAID_PREMIUM_CLAIM`,
`BCL_FOLLOW_UP_OVERDUE`, `BCL_CLAIM_PAST_DUE` and `BCL_INSURER_CLAIM_NO_REUSED`. Notification
events: `BCL_CLAIM_ASSIGNED`, `BCL_STATUS_CHANGED`, `BCL_FOLLOW_UP_DUE`, `BCL_PREMIUM_REMITTED`
and `BCL_NEWER_COVER_VERSION`. Numbers: `BCL-<yyyy>` and `CAC-<yyyy>`. Template:
`BCL_LOSS_ADVICE`.

## 7. Reports (category Claims Handling; view BCL_REPORT_VIEW, export BCL_REPORT_EXPORT)

| Code | Content |
|---|---|
| `BCL-OUTSTANDING` | Every claim of phase NEW, IN_PROGRESS or TEMP_CLOSED as of the date, with the p.42 columns and insurer claim numbers |
| `BCL-OUTSTANDING-PAST-DUE` | As above, age overall above `days` (default `BCL_PAST_DUE_DAYS`) |
| `BCL-SETTLED` | Claims with a SETTLED outcome settled in the date range |
| `BCL-AGEING` | Outstanding claims with age overall and bracket, totals per bracket and insurer |
| `BCL-AGEING-STATUS` | By status, then insurer; age this stage and overall |
| `BCL-LOSS-EXPERIENCE` | Per claim and insurer line: paid, O/S = max(reserve - paid, 0) while open, total; by client > ARN > policy year |
| `BCL-LOSS-RATIO` | Losses over the signed gross premium of the cover and policy year x 100; grouping client, line or insurer (CLQ20) |
| `BCL-PENDING-ACTIONS` | Follow-ups due or overdue and open diary entries due, per handler |
| `BCL-PRONE-LOCATIONS` | Count, paid and O/S per location key, city and province; claims-prone flag; catastrophe filter; drill-down |
| `BCL-INSURER-CLAIMS` | One row per insurer claim number with the BDOI claim, insurer, share, reserve and settled amount |
| `BCL-ACTIVITY-LOG` | Status history, field changes, diary, insurer updates, reserve amendments and location reference changes, with user and time |
| `BCL-DATA-EXTRACT` | Flat extract, one row per claim x insurer line x location (`BCL_DATA_EXTRACT` for view and export) |

Each report runs on screen and exports to Excel, PDF and CSV, and every run is archived. Saved
variants work for every code; the shared variants "Outstanding by insurer" and "Past due 90 days"
are delivered. Marketing (`MKT_AO`, `MKT_TL`) sees every report except the data extract; only the
Team Lead may export (CLQ15).

## 8. Integrations and contracts

| Contract | Kind | Used by |
|---|---|---|
| `opsledger.service.port.ClaimsFeed` implemented by `brokerclaims.feed.service.InAppClaimsFeed` | Port (feed `CLAIMS_SPECIAL_REMIT`) | Remittance special requests with condition CLAIMS |
| `OpsLedgerEvents.InvoiceMovementPosted`, `InvoiceFlagChanged`, `RemittanceStatusChanged`, `OpsInvoiceBooked` | Events consumed after commit (`claim.service.OperationsListener`) | Premium re-check, remitted notice, newer cover version notice |
| `brokerclaims.service.ClaimExperienceQueryService.summary(arn, policyYear)` | Read API | Account Claims tab, Marketing, Renewal (With Claim Y/N, count, status) |
| `brokerclaims.domain.ClaimStatusChanged`, `claim.service.ClaimRecorded` | Spring events inside the transaction | Status engine, notifications |
| `ClientRecordsProvider` (`BrokerClaimClientRecords`) | Provider | Client 360 |
| `RetentionCandidateProvider` (`BrokerClaimRetentionProvider`, record type `BROKER_CLAIM`) | Provider | Records retention (10 years online + 5 archive) |
| `report.ClaimActivitySource` | Extension point | Activity log contributors (`InsurerActivitySource`) |
| Bulk handlers `BCL_INSURER_UPDATE`, `BCL_INSURER_CLAIM_NO`, `BCL_LOCATION_REF` | Bulk upload | Insurer bordereaux and references |

API root: `/api/v1/broker-claims` (every claim call takes `companyId`). The endpoints are listed in
design sections 18 and 19.

## 9. Parked items

| Item | Question | Built now |
|---|---|---|
| Meaning and use of the claims authorization code, overrides | CLQ01 | Premium check, code generation, direct-payment parameter |
| Location lists per endorsement (cover version per location) | CLQ02 | Version = endorsements of the year; location snapshot |
| BDOI status matrix, units, follow-up days | CLQ04, CLQ07 | Default matrix, 7 days, maintainable |
| Settlement status list; the two "LOA - repair" types closing the claim | CLQ05 | 10 types with attributes; the two do not close |
| Closure and reopen rights | CLQ06 | Officer temporary; TL / TH permanent; TH / UH reopen |
| Reserve, loss experience and loss ratio bases | CLQ08, CLQ09, CLQ20 | Reserve per insurer line; O/S = reserve - paid; signed gross premium |
| Claim proceeds through BDOI (receipt and payout) | CLQ10 | None; no accounting entry |
| Insurer claims mailbox, adjuster directory | CLQ11, CLQ22 | Placement mailboxes proposed; adjusters as a list |
| Loss nature and claim type values | CLQ12 | Provisional seeds |
| Legacy claims migration (`source = MIGRATED`, V1025 held) | CLQ14 | Refused by recording (`BCL_SOURCE_NOT_ALLOWED`) |
| Marketing grants and portfolio restriction | CLQ15 | AO view, TL view and export, no restriction |
| Hazard / geocoding data for claims-prone areas | CLQ16 | Location key, city, province |
| Insurer channels (portal, API, SFTP bordereaux) | CLQ17 | E-mail loss advice, manual and bulk updates |
| Shared drive for report files | CLQ21 | In-app report archive |
| Notification events and wording to the client / insurer | CLQ22 | In-app notices to handlers and the AO |
| Total-loss indicator for Renewal | CLQ28 | Not defined |
| The remittance special form reads `?invoiceNo=&condition=CLAIMS` | UI ask to the remittance owner | The claim links to the form; the invoice is typed |
| Auto-resolve of `BCL_UNPAID_PREMIUM_CLAIM` when the premium becomes paid | Platform (no auto-resolve in `alert`) | The alert stays open until resolved on the Alerts screen |

## 10. Troubleshooting (production support)

| Symptom / error code | Cause | Fix |
|---|---|---|
| `BCL_SEARCH_TOO_SHORT` on Cover Lookup or Record Claim | Fewer than 3 characters | Type at least 3 characters of the ARN, policy number or assured |
| "Cover not found: <ARN>" | The ARN is wrong, or belongs to another company than the one selected | Check the company selector and the ARN on the account page |
| `BCL_LOSS_OUTSIDE_COVER` | Loss date outside the chosen policy year | Pick the right policy year, or confirm the recording outside the period |
| `BCL_LOSS_DATE_FUTURE`, `BCL_REPORTED_DATE_RANGE` | Loss date after today; reported date before the loss date or after today | Correct the dates; the reported date is later changed with Correct Reported Date (reason required) |
| `BCL_SOURCE_NOT_ALLOWED` | Source MIGRATED chosen | Migrated claims come only from the legacy migration (CLQ14) |
| Claim flagged *Unpaid premium*; `BCL_PREMIUM_UNPAID` on Generate Authorization Code | An invoice of the ARN and policy year is UNPAID or PARTIALLY_PAID | Check the invoices on Cover Lookup; once cashiering applies the payment the check turns PAID (after commit), or use Premium Check on the claim; the daily `BCL_PREMIUM_RECHECK` also catches it |
| Premium paid but the claim still says unpaid | The after-commit re-check failed (the log shows "Claims of invoice ... not brought in step") | Premium Check on the claim, or run `BCL_PREMIUM_RECHECK` from the Job Monitor |
| `BCL_DP_EVIDENCE_REQUIRED` / `BCL_ATTACHMENT_NOT_ON_CLAIM` | Direct-payment cover under `BCL_AUTH_DP_POLICY` = CONFIRM without evidence | Upload the insurer's payment evidence on the Documents tab of the claim and select it |
| `BCL_ALREADY_AUTHORIZED` | The claim already has its code | Nothing to do; the code is on the summary card |
| `BCL_UNIT_NOT_SET` | The user is not in the claims handler register | The Unit Head adds the user and unit on Claims Setup > Claims Handler Register |
| `BCL_STATUS_NOT_ALLOWED` ("You are not allowed to set the status ...") | No active matrix row for the user's roles and unit | The Unit Head adds the row on Claims Setup > Status Access Matrix; another user authorises it |
| `BCL_STATUS_BACK_TO_NEW` | A newly filed status on a claim that has left NEW | Choose an in-progress status (by design there is no way back to NEW) |
| `BCL_CLAIM_CLOSED` on a status or reported date change | The claim is permanently closed | Reopen it first (Team Head / Unit Head, with a reason) |
| `BCL_CLAIM_NOT_CLOSED` on Reopen | The claim is not permanently closed | A temporarily closed claim is resumed by setting an in-progress status |
| HTTP 403 on Set Settlement with a closing type | The user has BCL_SETTLEMENT_UPDATE but not BCL_CLOSE | A Team Lead or Team Head sets it |
| `BCL_SETTLEMENT_AMOUNT_REQUIRED`, `BCL_SETTLED_DATE_FUTURE` | The type needs the amount and date settled; the date is in the future | Enter both, date today or earlier |
| `BCL_INSURER_CLAIM_NO_DUPLICATE` / `BCL_INSURER_CLAIM_NO_REUSED` | The number is already on this claim / on another claim of the same insurer | Correct the number, or confirm the reuse (the alert records it) |
| `BCL_LOCATION_NOT_ON_COVER`, `BCL_LOCATION_LINKED` | The item is not a location of the claim's cover / already linked | Pick from the cover's locations |
| `BCL_EFFECTIVE_DATE` on a location reference | The new reference starts on or before the open one | Enter an effective date after the one shown |
| `BCL_RECIPIENT_REQUIRED`, `BCL_LOSS_ADVICE_INSURER` on Send Loss Advice | No e-mail address for an insurer, or the insurer is not on the claim | Enter the insurer's claims address (CLQ11) |
| Special remittance refused: "the Claims system has no claim for it" (`SPECIAL_REMIT_NOT_ELIGIBLE`) | No open claim of the invoice's cover and policy year is in a status with `awaiting_premium_remittance`, or its status was set before the feed window | The handler sets "With BDOI - For Premium Remittance" on the claim, then Marketing Collections requests again |
| Special remittance refused with `CHECK_HOLDING` | The last payment of the invoice is within `REMIT_CHECK_HOLD_DAYS` | Wait for the holding period (Remittance) |
| No "Premium ... remitted" notice | The invoice is only PARTIALLY_REMITTED, the claim was not in an awaiting status, or the after-commit listener failed | Check the invoice's remittance status on Invoice 360; the claim shows the unremitted invoices |
| *Awaiting premium remittance* tile still counts a remitted claim | The tile and worklist filter count claims in the status (to-do list) | The handler changes the status after the remitted notice |
| No follow-up reminders or past-due alerts | `BCL_FOLLOW_UP_DUE` / `BCL_AGEING_ALERTS` not running | Job Monitor: check the last runs, run now; see `JOB_FAILURE` alerts |
| `BCL_DIARY_NOT_YOURS`, `BCL_DIARY_DONE` | Only the assignee or author completes an entry; it is already done | Ask the assignee; nothing to do |
| `BCL_FOLLOW_UP_PAST` | Override date before today | Enter today or a later date |
| `BCL_ATTRIBUTES_UNCHANGED`, `BCL_MATRIX_ROW_EXISTS` | Setup change identical to the current values / duplicate matrix row | Change a value; edit the existing row |
| "The as-of date cannot be in the future" / "<to> must not be before <from>" on a report | Report parameters | Correct the dates |
| Marketing user cannot export a Claims report | `MKT_AO` has view only (CLQ15) | The Team Lead exports, or grant BCL_REPORT_EXPORT through a User Access request |
| Claims Handling missing from the menu | The user lacks BCL_VIEW (Marketing sees only Claims Reports) | Grant the role through a User Access request |

### 10.1 Where to look

- Tables: `bcl_claim` (cover snapshot, loss, progress), `bcl_claim_location`, `bcl_insurer_claim`,
  `bcl_reserve_change`, `bcl_insurer_update`, `bcl_location_ref`, `bcl_status_history`,
  `bcl_claim_event` (field changes), `bcl_diary_entry`, `bcl_lov_attribute`,
  `bcl_attribute_change`, `bcl_status_access`, `bcl_handler`.
- Workflow `BCL_CLAIM` (stage = phase), entity type `BrokerClaim`. The History tab and the Claims
  Activity Log show every change with user and time. The audit trail has the old and new values.
- Alerts screen, module CLAIMS_HANDLING. Notifications: `msg_notification` with entity type
  `BrokerClaim`.
- Seed data for SIT and UAT (seed profile): the Claims seed data runner of `brokerclaims` and its
  storyline `BrokerClaimsSeedStory`. They create a motor claim with an adjuster and a follow-up due, a typhoon claim with an offer to
  the claimant and an overridden follow-up, a temporarily closed direct-payment claim, an
  insurer-reported theft claim settled on the LOA and closed, a fire claim closed within the
  deductible and reopened, a claim awaiting the premium remittance, and a newly filed liability
  claim. The Insurer Location References come from V1921.
