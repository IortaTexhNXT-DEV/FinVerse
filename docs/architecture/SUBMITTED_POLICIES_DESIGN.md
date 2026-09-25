# iNXT BrokerVerse - BDOI Submitted Policies (BRD-12) Build Design

Status: **proposal for review**. It extends `docs/architecture/BROKING_ARCHITECTURE.md` (binding), `OPERATIONS_DESIGN.md`,
`COLLECTIONS_DESIGN.md` and `ACCOUNTING_DISBURSEMENT_DESIGN.md`, and it does not change them. Requirements baseline:
[`BDOI_SP_BRD_SPEC.md`](../requirements/BDOI_SP_BRD_SPEC.md) (33 requirement IDs BRIDSP-01 to 33, questions SQ01-SQ25).
Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRIDSP-12`.

## 1. Design principles

1. **A submitted policy is not an account.** BDOI did not place it, so it has no ARN, no invoice and no GL effect. It
   lives in its own masterlist (`sbm_policy`). It becomes an account only when the client agrees to renew with BDOI;
   from then on BRD-1 (account → placement → issuance → booking) runs unchanged, and the masterlist only follows it.
2. **Rules are data.** Sanitation, matching, classification, disposition, limits, letters and approval matrices are
   tables with maker-checker (BRIDSP-08). The seeds are the BRD lists; the buckets are an LOV. The code implements the
   engine and the steps, never the business values.
3. **Every run is replayable and explains itself.** A processing run records one result per policy and step (outcome,
   bucket, reason code, rule id and version). Fallout reports and the history of a policy read those rows.
4. **Reuse the BRD-1 and Operations platform.** Intake = `bulk` handlers and flow-in runs; extraction = the `issuance`
   extraction port; documents = `docgen`; letters and notifications = `messaging`; approvals = `workflow` + approval
   inbox; hold cover = `placement`; unapplied payments = `cashiering` via the `opsledger` ports; reports = `report`.
5. **Parked means seam, not fake.** LFS, HLS, CIU, SPI, LAMD, IBG / Leasing, the mail house (COG), OCR and a qualified
   e-signature each get a port whose default adapter is an upload, a manual step or a stamped signature.
6. **Renewal is a hand-off.** The renewal leg is shared with the Renewal BRD (analysed in parallel). `submitted`
   calls a port `RenewalHandOff`; its default adapter (in `submitted`) creates the renewal account and the letters. When a
   `renewal` module exists it implements the port and the default adapter steps aside (`@ConditionalOnMissingBean`).

## 2. Modules

| Module | State | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|---|
| `submitted` | **new** | Sources and intake runs, document extraction and confirmation, the Submitted Masterlist and its history, LAMD loan snapshot, rules and processing runs (sanitation, matching, classification, disposition, limits), buckets and tags, IAAF and reviews, TOR, approval matrices, expiry scan, renewal hand-off, letters and print batches, handling-fee records and tagger, No Touch billing lists, migration, Submitted Policies reports, dashboard counts | BRIDSP-01-25, 28-31, 33 | account, catalog, crm (read), booking (event), placement (port), opsledger (ports), bulk, workflow, docgen, messaging, attachment, lov, alert, system, report, audit, approval, organization | V1070-V1079 (V1970-V1972) |
| `placement` | built, **changed** | Hold cover re-assignment; unbooked hold-cover alert facts | BRIDSP-32, 24 | as today | V850-V859 (owner's range) |
| `account` | built, **changed** | Origin SUBMITTED_POLICY, business type RENEWAL and `renewal_of_ref` on the account | BRIDSP-26/27 | as today | V820-V829 |
| `booking` | built, **changed** | Business type taken from the account, carried on the invoice flags and `InvoiceBooked` | BRIDSP-27 | as today | V870-V879 |
| `issuance` | built, **changed** | `PolicyDataExtractor` document kind and field set; `OcrEngine` port | BRIDSP-02 | as today | V860-V869 |
| `opsledger` | built, **changed** | `UnappliedDispositionRequests.Action.RECOGNIZE_INCOME` with an income type | BRIDSP-31 | as today | V760-V763 |
| `cashiering` | being built, **changed** | Disposition type `HANDLING_FEE`, event `SBM_HANDLING_FEE`, OR through `ReceiptIssuer` | BRIDSP-31 | as today | V764-V769 |
| `collections` | being built, **changed** | LOV value `HANDLING_FEE` in `CLX_UPP_DISPOSITION`; the collector view shows the tag and its source | BRIDSP-31 | as today | V1000-V1009 |
| `report` | built, **changed** | `ReportCategory.SUBMITTED_POLICIES` and `ReportMetadata.submitted(...)` factory (view / export split, archived) | BRIDSP-20/21/28 | as today | - |
| `security` | built, **changed** | Submitted Policies permissions (enum) | all | as today | granted in V1070 |

The module is one top-level package `com.iortatechnxt.brokerverse.submitted` with the usual `domain` / `service` / `api`
layout and sub-packages per area (`intake`, `masterlist`, `processing`, `review`, `renewal`, `fee`, `report`), so that
four agents can build in parallel (section 14).

### 2.1 Dependency graph (arrows = "depends on")

```
           account   catalog   crm   booking(event)   placement(HoldCoverService)
               \        |       |        |               /
                +------------- submitted ---------------+ ---> bulk, workflow, docgen, messaging, attachment,
                               |                                lov, alert, system, report, audit, approval
                               v
                  opsledger ports (UnappliedDirectory, UnappliedDispositionRequests, ReceiptIssuer)
                               ^
                           cashiering (implements)            issuance (PolicyDataExtractor port)
```

No built module depends on `submitted`. `booking` publishes `InvoiceBooked` and `account` publishes
`AccountStatusChanged`; `submitted` listens (after commit) to follow the renewal account.

### 2.2 Ports

| Port | Declared in | Implemented by | Called by | Purpose |
|---|---|---|---|---|
| `SubmittedSourceFeed` | `submitted.service.port` | default: none (uploads); later LFS / HLS / CIU / SPI / LAMD adapters | intake jobs | Scheduled pull of a source file / API (SQ01) |
| `PolicyDataExtractor` (extended) | `issuance.service` | `PdfTextPolicyDataExtractor` (text PDFs); `OcrEngine` adapter later | submitted extraction | Field proposals from a document (BRIDSP-02) |
| `OcrEngine` (new) | `issuance.service` | default: `NoOcrEngine` (returns "not readable" → manual entry) | extractor | Scanned / printed documents (Q24, SQ03) |
| `RenewalHandOff` | `submitted.service.port` | default `SubmittedRenewalHandOff` (in `submitted`); later the `renewal` module | expiry scan, "Renew with BDOI" action | Start the renewal of a masterlist record (BRIDSP-23/25/26) |
| `MailHouseGateway` | `submitted.service.port` | default: print batch in the extract repository | letter dispatch | Snail mail through COG (SQ09) |
| `SignatureProvider` | `submitted.service.port` | default `StampedSignature` (name, position, time, hash) | IAAF / TOR approval | Qualified e-signature later (SQ07) |

## 3. Business flows

### 3.1 Intake and masterlist (BRIDSP-01/02/03/04/33)

1. A **source file** (upload or `SubmittedSourceFeed`) runs through its bulk handler. Each valid row upserts
   `sbm_policy` on the natural key (company, segment, business type, PN or policy no., expiry date). New rows get
   `SBM-yyyy-nnnnnn`, status RECEIVED; changed rows write `sbm_policy_history`. The file hash blocks duplicates.
2. A **document** (policy copy, scanned or PDF) is attached to a new or existing record. `SbmExtractionService` calls
   the extractor and stores the proposal in `sbm_extraction` (field, value, confidence). The confirm screen shows the
   proposal next to the record; *Confirm* writes the fields and the status VALIDATED. *Reject* keeps the file with a
   reason. Nothing is saved to the masterlist before confirmation.
3. **Manual entry** by a Marketing user or Policy Reviewer creates the record directly (status VALIDATED).
4. **Migration** (`SBM_MIGRATION` handler) loads the legacy Excel masterlists with `migrated = true`, `legacy_ref`, the
   original `date_received` and the mapped status (`sbm_status_map`). Failures go to the bulk error log with
   `SBM_MIG_*` reason codes.
5. Every commit of 1-4 queues a **processing run** for the affected records (BRIDSP-09).

### 3.2 Processing run (BRIDSP-08-16)

`SbmProcessingService.run(trigger, scope)` executes the steps in order, set-based per step:

| Step | Input | Output | BRD |
|---|---|---|---|
| SANITATION | record fields | normalised fields; duplicates (same PN or same serial / motor number) → bucket FOR_REVIEW; incomplete → FALLOUT | 08, 09, RL #151 |
| MATCHING | `sbm_lamd_loan` | PN matched / unmatched, loan status (ACTIVE, OPEN_MARKET, FULLY_PAID, REMEDIAL), amortised flag; PN = serial / motor / assured checks | 09, 13 |
| CLASSIFICATION | matching result | INFORCED / SUBMITTED | 11 |
| DISPOSITION | classification, loan status, tags, FFY / employee / No Touch lists | bucket (LOV `SBM_BUCKET`), renewal tag, RA template (GENERIC / FFY), exclusion reason | 12-15 |
| LIMITS | insurer, product, TSI, vehicle age | `sbm_limit_check`; `insurer_approval_required` → TOR task | 16 |

Rules: `sbm_rule` rows of the active rule set, highest priority first; the first match with `stop = true` ends the
step. A manual tag (BRIDSP-03) is an override: the step records the rule result but keeps the manual value.
Each result row stores the rule id and rule-set version. A record with no matching rule in a step is FALLOUT with
reason `SBM_NO_RULE`. On bucket change the record's work case moves (`SBM_POLICY` workflow) and a notification is sent.

### 3.3 Policy review and IAAF (BRIDSP-05/06/07)

Non-CBG Corporate & Branches and CBG Fire records with documents enter the review queue. The reviewer records a review
(`sbm_iaaf_review`: adequacy ADEQUATE / WITH_FINDINGS, findings, remarks). With findings the record goes back to the
bank counterpart (e-mail from the record) and a new review follows. When adequate, the reviewer generates the IAAF
(one per policy, `IAAF-yyyy-nnnnnn`), which is routed by `sbm_approval_matrix` (document IAAF, segment, TSI band →
levels). The last approval stamps the signatures (`SignatureProvider`), renders the PDF and moves the IAAF to
ISSUED; *Send to bank counterpart* e-mails it. Then the record is monitored for expiry (conversion opportunity) or a
reminder letter is sent (p.7 "with opportunity for renewal?").

### 3.4 Limits and TOR (BRIDSP-16-19)

A LIMITS breach creates a TOR task for the handling Marketing user. The TOR (`TOR-yyyy-nnnnnn`) lists the breaches and
the proposed terms, and it is routed by the TSU approval matrix (`sbm_approval_matrix` document TOR, levels granted to
`TOR_APPROVE`, held by the TSU roles). On approval the signed PDF is attached, the AO is notified and the TOR becomes
RELEASED when the AO opens or downloads it.

### 3.5 Renewal (BRIDSP-22-27, 32)

1. `SBM_EXPIRY_SCAN` (daily) selects FOR_RENEWAL records whose expiry is within the lead days of their segment
   (`SBM_RENEWAL_LEAD_DAYS` per segment, e.g. CBG Fire 150, CBG Motor 120) and hands them to `RenewalHandOff`.
2. Default hand-off:
   - assigns the insurer (rules `sbm_insurer_rule`: by vehicle type, "different from the expiring insurer"), computes the
     premium with `catalog.PremiumCalculator`, and creates the **renewal account** through
     `AccountService.createDraft(NewAccount.renewal(...))` (origin SUBMITTED_POLICY, business type RENEWAL,
     `renewal_of_ref` = SBM number, risk items copied);
   - requests the **hold cover** through `HoldCoverService.request` (30 days). If the insurer has not accepted within
     `SBM_INSURER_ACCEPT_DAYS` (default 5) an alert asks the handler to follow up or re-assign
     (`HoldCoverService.reassign`, BRIDSP-32);
   - once accepted, queues the **letters** (`sbm_letter`) due by `sbm_letter_rule`: RA (generic or FFY) at
     `SBM_RA_DAYS_BEFORE_EXPIRY` (90), reminder letters, SFU for mortgaged accounts, NAL for unrenewed accounts.
3. The client accepts (payment or confirmation): the BRD-1 path runs (payment gate → placement slip → issuance →
   booking). `AccountStatusChanged` and `InvoiceBooked` update the masterlist conversion status (PROCESS_PLACEMENT,
   PLACED, BOOKED).
4. The client declines or the expiry passes without renewal: NRNS / NAL letter, status NOT_RENEWED, reason.
5. Non-CBG Retail: the MAO starts the renewal by hand ("Renew with BDOI"); the quotation is a normal BRD-1 quotation.

### 3.6 Handling fee (BRIDSP-31)

1. Handling-fee records (`sbm_handling_fee`) are created for CBG Motor submitted accounts that BDOI bills (bulk upload
   or from a processing rule, SQ13), with PN, location reference, amount and billing date.
2. `SBM_HANDLING_FEE_TAGGER` (every 30 minutes, plus the `CashieringUnappliedCreated` event if Cashiering publishes it)
   reads open unapplied items through `UnappliedDirectory`, and matches CLPC items on the PN and OTC items on the
   location reference against BILLED records.
3. A match sends `UnappliedDispositionRequests.request(action RECOGNIZE_INCOME, incomeType HANDLING_FEE, source
   SUBMITTED, sourceRef HF:<id>)`. The handling-fee record becomes TAGGED; Cashiering records the disposition, posts
   `SBM_HANDLING_FEE` and issues the OR (`ReceiptIssuer`, OR type HANDLING_FEE); the ticket status turns APPLIED.
4. Unmatched or ambiguous items stay for the UPP handler in the Collections unapplied view, where the Handling Fee
   disposition is also available by hand (p.4 flow).

### 3.7 No Touch billing (Report List #164, OQ39)

`SBM-NO-TOUCH` exports the No Touch bucket per insurer with the premium and fee columns blank. The insurer returns the
validated list; the upload (`SBM_NO_TOUCH_RETURN`) fills basic premium, gross service fee, VAT and WTax and produces
the billing statement. Issuing the service-fee bill uses `booking.ServiceInvoiceService.issue` with a new SI type
`SERVICE_FEE_NO_TOUCH` (seeded by `submitted` in V1075, since the SI type table is data), posting `SBM_NO_TOUCH_FEE`.

## 4. Entities (key fields)

All tables have the V1 audit columns, `company_id`, and identity keys. No foreign keys to V8xx tables (plain ARN,
invoice no., insurer and client codes), as in Operations.

### 4.1 Intake and masterlist

- `sbm_source`: code (LFS_INSURANCE, HLS_INSURANCE, CIU, SPI, LOAN_BOOKING, LAMD, IBG_LEASING_DOC, IA_MASTERLIST,
  MANUAL, MIGRATION), segment, business type, bulk handler code, format, active.
- `sbm_intake_run`: run no. `SBI-yyyy-nnnnnn`, source, bulk job id or flow-in run id, file name, SHA-256, counts
  (received, created, updated, duplicate, failed), started / finished.
- `sbm_policy` (the masterlist): number `SBM-yyyy-nnnnnn`; segment (CBG_MOTOR, CBG_FIRE, NONCBG_CORPORATE,
  NONCBG_RETAIL); business type NB / RB; source; intake run; date received; renewal month; PN no.; loan application no.;
  CIF; value date; maturity date; referring branch; originating unit; borrower; assured (client name); mailing address;
  telephone, mobile, e-mail; insurer code; policy no.; inception; expiry; coverage days; amount insured; total premium;
  risk details (unit description, serial no., motor no., colour, plate no. / property location, occupancy); mortgagee;
  classification (INFORCED / SUBMITTED); loan status; amortised; bucket; renewal tag (RENEWABLE / NON_RENEWABLE) with
  source (RULE / MANUAL), reason, tagged by / at; RA template; opportunity tag; adequacy status; handler; conversion
  status; remarks; status (RECEIVED, VALIDATED, CLASSIFIED, IN_REVIEW, FOR_RENEWAL, RENEWAL_IN_PROGRESS, PLACED,
  BOOKED, NOT_RENEWED, EXCLUDED, CLOSED); renewal ARN; booked invoice no. and date; `insurer_approval_required`;
  migrated; legacy ref; version.
  Unique (company, segment, business type, pn_no / policy_no, expiry). Indexes on PN, policy no., expiry, bucket,
  status, handler, segment.
- `sbm_policy_history`: policy, field, old value, new value, source (INTAKE, EXTRACTION, MANUAL, RUN, RENEWAL,
  BOOKING), user, time (BRIDSP-29).
- `sbm_extraction`: policy (nullable before matching), attachment id, extractor, status (PROPOSED, CONFIRMED, REJECTED,
  FAILED), fields (jsonb: field → value, confidence), confirmed by / at, reject reason.
- `sbm_lamd_loan`: snapshot date, PN no., borrower, loan status, amortised, maturity date, originating unit, balance.
  Unique (company, snapshot date, PN).
- `sbm_status_map`: legacy status → status, bucket (migration).
- `sbm_user_scope`: username, segment, own-accounts-only flag (BRIDSP-28).

### 4.2 Rules and processing

- `sbm_rule_set` (AuthorizableEntity): code, step, segment, business type, version, effective from / to, status.
- `sbm_rule`: rule set, priority, conditions (jsonb list of {field, operator, value}), outcome (bucket, tag,
  classification, RA template, flag), reason code (LOV `SBM_REASON`), stop.
- `sbm_run`: run no. `SBR-yyyy-nnnnnn`, trigger (INTAKE, SCHEDULED, MANUAL), scope, steps, counts per outcome, job run id.
- `sbm_run_result`: run, policy, step, outcome (PASSED, BUCKETED, FALLOUT, OVERRIDDEN), bucket, reason code, rule id,
  rule-set version, message.
- `sbm_limit_rule`: insurer, line / product, max TSI, max vehicle age, other attribute and limit, active.
- `sbm_limit_check`: policy, rule, attribute, limit, value, breached, run.

### 4.3 Review, IAAF and TOR

- `sbm_iaaf`: number `IAAF-yyyy-nnnnnn`, policy (unique), status (DRAFT, FOR_APPROVAL, RETURNED, APPROVED, ISSUED,
  CANCELLED), current level, template version, PDF attachment id, sent to, sent at.
- `sbm_iaaf_link`: IAAF, related policy, relation (PREVIOUS_TERM, SAME_BORROWER, OTHER).
- `sbm_iaaf_review`: IAAF, review no., review date, reviewer, adequacy, findings, remarks.
- `sbm_tor`: number `TOR-yyyy-nnnnnn`, policy or ARN, breaches (jsonb), proposed terms, status (DRAFT, FOR_APPROVAL,
  RETURNED, APPROVED, RELEASED, CANCELLED), AO username, PDF attachment id, released at.
- `sbm_approval_matrix`: document (IAAF / TOR), segment, TSI from / to, level, permission, named approver (optional),
  signatory title.
- `sbm_signature`: document type and id, level, signer, position, signed at, method (STAMPED / ESIG), hash.

### 4.4 Renewal, letters, handling fee, No Touch

- `sbm_renewal`: policy, hand-off (DEFAULT / RENEWAL_MODULE), insurer assigned, premium, ARN, hold-cover reference,
  insurer accepted at, re-assign count, outcome (RENEWED, DECLINED, EXPIRED, IN_PROGRESS), decline reason.
- `sbm_insurer_rule`: segment, vehicle type / occupancy, insurer, priority, exclude-expiring-insurer flag.
- `sbm_letter_rule`: letter type, segment, bucket, days relative to expiry, channel, template code, active.
- `sbm_letter`: number `SBL-yyyy-nnnnnn`, policy, type (RA_GENERIC, RA_FFY, NRNS, NAL, SFU, REMINDER, RENEWAL_NOTICE,
  RENEWAL_PROPOSAL), channel (EMAIL, PRINT, BANK_COUNTERPART), status (QUEUED, GENERATED, SENT, PRINTED, FAILED),
  message id, print batch, template version.
- `sbm_print_batch`: batch no., letter type, count, merged PDF (extract repository), handed to (COG), date.
- `sbm_handling_fee`: policy, PN no., location reference, amount, currency, billing date, status (BILLED, TAGGED,
  APPLIED, CANCELLED), unapplied ref, disposition ticket, OR no.
- `sbm_no_touch_batch` / `_line`: insurer, period, policy, fields of RL #164, returned values, SI no.

## 5. Accounting events and GL entries

Masterlist, review, TOR and letters post nothing. Money appears only in the handling fee, the No Touch service fee and
the renewal booking. Demo rules only (V1970); Comptrollership configures the real accounts (OQ07, SQ13, SQ14).

| # | Transaction | Event (source ref) | Posted by | Default entry (demo chart) |
|---|---|---|---|---|
| 1 | Handling-fee payment recognised from an unapplied item | `SBM_HANDLING_FEE` (`HF:<id>`) | cashiering (disposition RECOGNIZE_INCOME) | Dr 2205 Unapplied Collections (client) / Cr 4115 Handling Fee Income (new demo account), Cr 2504 Output VAT |
| 2 | OR for the handling fee | `OPS_OR_ISSUE` class HANDLING_FEE (`OR:<no>`) | cashiering via `ReceiptIssuer` | BIR document; no further GL line (income already in 1) |
| 3 | No Touch service fee billed to the insurer | `SBM_NO_TOUCH_FEE` (`NT:<batch>:<insurer>`) | submitted via booking `ServiceInvoiceService` | Dr 1236 Service Fee Receivable - Insurers (new demo) / Cr 4110 Service Fee Income, Cr 2504 Output VAT |
| 4 | No Touch fee collected | `OPS_OR_ISSUE` class SERVICE_FEE (existing #15) | cashiering | Dr bank + Dr 1602 CWT (WTax as billed) / Cr 1236 |
| 5 | Renewal of a submitted policy booked | `BROKER_BOOKING` (existing) with business type RENEWAL | booking | As BRD-1 booking; no new rule |

Sub-ledger: the service-fee receivable is an open item of the insurer party (type SERVICE_FEE); the handling fee
closes the client's unapplied credit item.

## 6. Security

### 6.1 Permissions (enum `security.domain.Permission`, granted in V1070)

| Permission | Used for |
|---|---|
| `SBM_VIEW` | Masterlist, record, dashboards (within the user's scope) |
| `SBM_MAINTAIN` | Manual entry and edit, renewal tag, handler, remarks, document upload, confirm extraction |
| `SBM_INTAKE` | Source uploads, intake runs, LAMD snapshot upload |
| `SBM_PROCESS` | Start processing runs, resolve fallout, manual disposition, renewal hand-off, insurer re-assignment |
| `SBM_RULE_MAINTAIN`, `SBM_RULE_APPROVE` | Rule sets, limit rules, insurer rules, letter rules, approval matrices (maker-checker) |
| `IAAF_PREPARE`, `IAAF_APPROVE` | Reviews and IAAF; approval levels |
| `TOR_PREPARE`, `TOR_APPROVE` | TOR; TSU approval levels |
| `SBM_LETTER_SEND` | Generate, send and print letters; print batches |
| `SBM_HANDLING_FEE` | Handling-fee records, tagger results, manual tag |
| `SBM_MIGRATE` | Migration handler |
| `SBM_EXPORT` | Masterlist extract |
| `SBM_REPORT_VIEW`, `SBM_REPORT_EXPORT` | Submitted Policies reports (view vs export, archived) |

### 6.2 Roles (V1070) and demo users (V1970, password `Brokerverse@2026`)

| Role | Persona (BRD) | Permissions | Demo user |
|---|---|---|---|
| `SBM_HANDLER` | Submitted Handler (CBG) | VIEW, MAINTAIN, INTAKE, IAAF_PREPARE, TOR_PREPARE, REPORT_VIEW / EXPORT, EXPORT | `sbmhandler` |
| `SBM_CHECKER` | Submitted Checker | VIEW, IAAF_APPROVE (level 1), REPORT_VIEW | `sbmchecker` |
| `SBM_SANITATION` | Sanitation Handler | VIEW, MAINTAIN, INTAKE, PROCESS, LETTER_SEND, REPORT_VIEW / EXPORT, EXPORT | `sanitation` |
| `SBM_TL` | Team Lead | all SBM permissions except RULE_APPROVE; WORK_ASSIGN | `sbmtl` |
| `SBM_POLICY_REVIEWER` | Non-CBG Corporate Policy Review Officer | VIEW, MAINTAIN, IAAF_PREPARE, REPORT_VIEW | `polreview` |
| `SBM_RULE_ADMIN` | Business administrator of the rules | RULE_MAINTAIN (maker) | `badmin` (existing, role added) |
| `SBM_UPP_HANDLER` | Admin Team UPP handler | SBM_VIEW, SBM_HANDLING_FEE, plus the Collections unapplied permissions | `upphandler` |
| existing `MKT_AO` | Marketing AO / MAO | + SBM_VIEW (own scope), SBM_MAINTAIN, TOR_PREPARE | `ao` |
| existing `MKT_TL` | NB TL | + SBM_VIEW, IAAF_APPROVE (level 2), SBM_RULE_APPROVE | `mkttl` |
| existing `TSU` | TSU | + TOR_APPROVE | `tsu`, `tsulead` |
| existing `PROCESSOR`, booking users | Placement / Booking users | + SBM_VIEW | `proc` |

Scope: `SbmScopeService` filters every query by `sbm_user_scope` (segment list; own accounts only for AOs, matched on
`handler_username` or the renewal account's AO). Reports apply the same scope.

## 7. Workflows (`wf_stage` / `wf_transition`, seeded in V1070)

- `SBM_POLICY`: RECEIVED → VALIDATED → CLASSIFIED → {FOR_RENEWAL, FOR_MANUAL_DISPOSITION, IN_REVIEW, EXCLUDED} →
  RENEWAL_IN_PROGRESS → {PLACED → BOOKED, NOT_RENEWED}; CLOSED. System transitions are driven by the run and by the
  account / booking events; user transitions: `dispose` (manual disposition, reason), `exclude`, `reinstate`, `renew`,
  `close`.
- `SBM_IAAF`: DRAFT → FOR_APPROVAL (level n, looping while levels remain) → APPROVED → ISSUED; `return` to DRAFT with a
  reason; `cancel`.
- `SBM_TOR`: DRAFT → FOR_APPROVAL (level n) → APPROVED → RELEASED; `return`; `cancel`.

Approval inbox: `SubmittedApprovalSource` (IAAF and TOR levels, maker excluded, rule sets pending approval).

## 8. Jobs, parameters, LOVs, alerts, notifications, templates

| Job (`ManagedJob`) | Cron (default, PHT) | Purpose | BRD |
|---|---|---|---|
| `SBM_INTAKE_PULL` | manual (`-`) until a feed exists | Pull sources through `SubmittedSourceFeed` | 01 |
| `SBM_PROCESSING` | 21:30 daily | Processing run of RECEIVED / VALIDATED / changed records | 09 |
| `SBM_EXPIRY_SCAN` | 22:00 daily | Renewal hand-off by lead days | 23 |
| `SBM_LETTER_DISPATCH` | 06:30 daily | Letters due; print batches | 22 |
| `SBM_HOLD_COVER_WATCH` | 07:00 daily | Insurer not accepted within n days; hold cover of unbooked accounts | 24, 32 |
| `SBM_HANDLING_FEE_TAGGER` | every 30 min | Tag handling-fee payments | 31 |

Crons: `brokerverse.jobs.sbm-*-cron` with environment variables, documented in `docs/operations/CONFIGURATION.md`.

Parameters (category SUBMITTED): `SBM_RENEWAL_LEAD_DAYS` (CODE_LIST per segment), `SBM_RA_DAYS_BEFORE_EXPIRY` (90),
`SBM_MASTERLIST_DAYS_FROM_EXTRACTION` (30), `SBM_INSURER_ACCEPT_DAYS` (5), `SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS` (5),
`SBM_REVIEW_SLA_DAYS`.

LOV types: `SBM_BUCKET` (attribute `renewal_action`: RENEW / MANUAL / EXCLUDE), `SBM_REASON`, `SBM_NON_RENEWAL_REASON`,
`SBM_CONVERSION_STATUS`, `SBM_LOAN_STATUS`, `SBM_SEGMENT`, `SBM_LETTER_TYPE`, `SBM_DECLINE_REASON`,
`SBM_IAAF_FINDING`.

Alert codes: `SBM_FALLOUT` (run with fallout), `SBM_INTAKE_FAILED`, `SBM_INSURER_NOT_ACCEPTED`,
`SBM_HOLD_COVER_UNBOOKED`, `SBM_IAAF_SLA`, `SBM_TOR_SLA`, `SBM_LETTER_FAILED`.

Notification events (preference catalogue, BRIDSP-24): SBM_NEW_SUBMISSION, SBM_MANUAL_VALIDATION, SBM_IAAF_PENDING,
SBM_TOR_PENDING, SBM_TOR_RELEASED, SBM_BUCKET_CHANGED, SBM_FALLOUT, SBM_EXPIRY_NEAR, SBM_RENEWAL_STARTED,
SBM_PLACEMENT_READY, SBM_PLACEMENT_SENT, SBM_HOLD_COVER_UNBOOKED.

Templates (`docgen`): `SBM_IAAF`, `SBM_TOR`, `SBM_RA_GENERIC`, `SBM_RA_FFY`, `SBM_NRNS`, `SBM_NAL`, `SBM_SFU`,
`SBM_REMINDER`, `SBM_RENEWAL_NOTICE`, `SBM_RENEWAL_PROPOSAL`, `SBM_NO_TOUCH_BILLING`; placeholders only until BDOI
supplies the layouts (SQ07-SQ09).

Bulk handlers: `SBM_LFS_INSURANCE`, `SBM_HLS_INSURANCE`, `SBM_CIU`, `SBM_SPI`, `SBM_LOAN_BOOKING`, `SBM_LAMD`,
`SBM_IA_MASTERLIST`, `SBM_MIGRATION`, `SBM_HANDLING_FEE_BILLING`, `SBM_NO_TOUCH_RETURN`, `SBM_RULES` (rule import).

## 9. Impact on built and in-progress modules (contract changes)

| Module (state) | Change | Concrete contract | Owner / wave |
|---|---|---|---|
| `account` (built) | Origin and business type | `acc_account.business_type` (NEW_BUSINESS / RENEWAL, default NEW_BUSINESS), `origin` value `SUBMITTED_POLICY`, `renewal_of_ref varchar(40)`; `NewAccount.renewal(companyId, clientRef, product, segment, insurer, period, items, renewalOfRef, aoUsername)`; `Account.getBusinessType()`; `AccountStatusChanged` unchanged. Migration `V822__account_business_type.sql` | account owner, S0 |
| `booking` (built) | Business type from the account | `InvoiceBuilder` passes `account.getBusinessType()` instead of `BusinessType.NEW_BUSINESS` (line 129); `InvoiceBooked` gains `BusinessType businessType` (factory `of(...)` fills it; callers of the canonical constructor updated in the same change) | booking owner, S0 |
| `placement` (built) | Hold-cover re-assignment | `HoldCoverService.reassign(String arn, String newInsurerCode, String reasonCode)` returning the new `HoldCover`; `HoldCoverStatus.REASSIGNED`; `AccountLifecycleService.changeInsurer(arn, insurerCode, reason)` in `account`; `PlacementQueryService.holdCovers(arn)` (history). V851 | placement owner, S0 |
| `issuance` (built) | Extraction for submitted policies | `PolicyDataExtractor.extract(ExtractionRequest(kind, attachmentId, insurerCode))` returning `ExtractionProposal(Map<String, ExtractedValue>)`; kinds `EPOLICY` (today's behaviour) and `SUBMITTED_POLICY`; `iss_extraction_pattern.kind` column (V861); port `OcrEngine` with default `NoOcrEngine` | issuance owner, S0 |
| `opsledger` (built) | Income disposition | `UnappliedDispositionRequests.Action.RECOGNIZE_INCOME`; `DispositionRequest` gains `String incomeType` (null for the other actions; a secondary constructor keeps the 9-argument form) | opsledger owner, S0 |
| `cashiering` (being built) | Handling-fee disposition | Disposition type `HANDLING_FEE` (action INCOME, no approval unless BDOI decides otherwise, SQ13) in `csh_disposition_type_rule`; the executor posts `SBM_HANDLING_FEE` and issues the OR through its own `ReceiptIssuer` (OR type HANDLING_FEE); `UnappliedView` exposes channel, PN / reference and location reference. Optional event `UnappliedItemCreated(ref, channel, reference)` after commit | cashiering owner (O1-A); event type row seeded by `submitted` in V1070 |
| `collections` (being built) | UPP disposition value | `CLX_UPP_DISPOSITION` value `HANDLING_FEE` (attribute requires_invoice = false); the unapplied list shows "Handling fee (auto)" when the ticket source is SUBMITTED | collections owner (C1) |
| `report` (built) | Category and factory | `ReportCategory.SUBMITTED_POLICIES("Submitted Policies")`; `ReportMetadata.submitted(code, title, description, params)` = view `SBM_REPORT_VIEW`, export `SBM_REPORT_EXPORT`, archived | submitted, S0 (small platform change) |
| `security` (built) | Permissions | Enum values of section 6.1 | submitted, S0 |
| `messaging` (built) | Preference catalogue | Register the SBM notification events (data) | submitted, S0 (V1070) |
| `nbadmin` (built) | Retention | Row in `nba_retention_rule` for record type SUBMITTED_POLICY (5 years online) and a `RetentionCandidateProvider` in `submitted` | submitted, S1-A |
| `catalog` / `productmaint` (built) | None required | Limits read through `RiskProduct.exceedsPackageLimit`; insurer acceptance limits live in `sbm_limit_rule` until Product Maintenance takes them over (SQ08) | - |
| GL / accounting | Event types | `SBM_HANDLING_FEE`, `SBM_NO_TOUCH_FEE` in `acc_event_type` (V1070); demo accounts 4115, 1236 and rules (V1970) | submitted |
| `frbs` (being built) | Service-fee pack | `FRBS-SERVICE-FEE` should include handling-fee (4115) and No Touch service-fee (4110, SI type SERVICE_FEE_NO_TOUCH) income; no contract change, a column / filter | frbs owner (A1-FRBS) |
| `nbreport` (built) | None | NB reports already include renewal accounts once `business_type` exists (`NB-BOOKED-REG` gains a business-type column in a later change) | nbreport owner |
| Renewal BRD (future `renewal`) | Hand-off | Implements `RenewalHandOff` if it owns the renewal of submitted policies (SQ10) | Renewal design |

## 10. Integrations to park (seam only)

| Item | BRD | Seam built now | Question |
|---|---|---|---|
| LFS, HLS, CIU, SPI, Loan Booking Report, LAMD, IA masterlist feeds | BRIDSP-01/13, p.4-5 | Bulk upload handlers; `SubmittedSourceFeed` port, job manual | SQ01, Q11 |
| OCR of scanned / printed policies | BRIDSP-02 | `OcrEngine` port, `NoOcrEngine` default (manual entry) | SQ03, Q24 |
| Mail house (COG) for snail-mail RA letters | p.5, BRIDSP-22 | Print batches (merged PDF + control list) in the extract repository | SQ09 |
| Qualified e-signature of IAAF / TOR | BRIDSP-07/18 | `SignatureProvider`, default stamped signature | SQ07 |
| Shared drive (Drive H:\) and SharePoint UPP list | p.4-5 | Not replicated; masterlist and reports in BIBS; `FileDropPort` if an export is still required | OQ17 |
| ISYS reference / ARF | p.5 | Not fed; the ARN is the reference | SQ20 |
| Insurer channels for proposals / hold cover | p.5 | E-mail through `placement` (existing rule, Q06) | Q06 |

## 11. Reports (ReportDefinition per report; category "Submitted Policies")

| Code | Report | BRD / Report List | Notes |
|---|---|---|---|
| `SBM-MASTERLIST` | Submitted Masterlist Report / Extract | BRIDSP-04/28, RL #151 | Fields of RL #151 per segment; migrated records included; scope-filtered |
| `SBM-DOC-FALLOUT` | Document Processing Fallout | BRIDSP-10, RL #152 | Extraction and intake failures |
| `SBM-PROCESS-FALLOUT` | Fallout: sanitation, matching, disposition, classification | BRIDSP-10/20, RL #153 | Per run or period, reason code |
| `SBM-NON-RENEWAL` | Non-renewal Report | BRIDSP-14, RL #154 | Exclusions with reason codes |
| `SBM-MIGRATION-ERRORS` | Migration Error Log | BRIDSP-33, RL #155 | Reads the bulk rows of `SBM_MIGRATION` jobs |
| `SBM-SANITATION` | Sanitation Report (RMEL certification) | BRIDSP-20, RL #156 | Counts per the RL #156 grouping |
| `SBM-DISPOSITION` | Disposition Report | BRIDSP-20, RL #157 | |
| `SBM-CLASSIFICATION` | Account Classification Report | BRIDSP-20, RL #158 | |
| `SBM-RENEWABLE` | Renewable Accounts / Policies | BRIDSP-25, RL #159 | Grouped by bucket and expiry month |
| `SBM-IAAF` | IAAF Tracking / Review | BRIDSP-05-07, RL #160 | Mapping, review history, approvals and audit |
| `SBM-TOR` | TOR Status & Approval | BRIDSP-17-19, RL #161 | Pending / Approved / Released, AO |
| `SBM-HANDLING-FEE` | Handling Fee Payment Classification | BRIDSP-31, RL #162 | CLPC / OTC, status, OR |
| `SBM-CONVERSION` | Submitted Policies Conversion (with persistency and accounts for review) | RL #163 | From the renewal outcome |
| `SBM-NO-TOUCH` | Submitted Policies No Touch | RL #164 | Export blank fee columns; re-upload builds the billing statement |
| `SBM-PR-CONVERSION` | Policy Review Conversion | RL #133 | |
| `SBM-PR-MONITORING` | Policy Review Monitoring (ageing) | RL #134 | SQ24 for the ageing events |
| `SBM-PERSISTENCY` | Renewal Persistency - submitted accounts | RL #135 | The general persistency report belongs to the Renewal BRD |
| `SBM-PENETRATION` | Penetration Report (CBG Motor per channel) | RL #138 | Needs `SBM_LOAN_BOOKING` and `SBM_IA_MASTERLIST` uploads |
| `SBM-HOLD-COVER-GAP` | Hold Cover with Gap | RL #138 remark | Reads `plc_hold_cover` of renewal accounts |
| `SBM-LETTERS` | Letters register | BRIDSP-22/24 | Sent, printed, failed |

All reports: PDF / XLSX / ODS / CSV / XML (framework), saved variants, archive (BRIDSP-21). Layouts without fields in the
Report List are built with the obvious columns and flagged "layout to confirm" (SQ25). Heavy reports use SQL aggregates
(`SbmReportJdbc`, the `NbReportJdbc` pattern).

## 12. Screens (group **Client & Policy**, section **Submitted Policies**)

Placed after Placement & Booking and before Product Reconciliation, where the UX guidelines keep "Renewal (later BRD)";
route `/submitted`, frontend folder `frontend/src/features/submitted`, help `SUBMITTED_HELP`. Screens follow
`docs/design/BDO_UX_GUIDELINES.md`: work lists with status tabs, `WorklistToolbar`, `RecordSummary`, `StatusBadge`
pills, flag chips (Renewable, FFY, No Touch, Migrated, Insurer approval), tokens only.

- **Submitted Policies home**: tiles (received today, awaiting validation, fallout of the last run, for renewal this
  month, IAAF / TOR pending, insurer not accepted, hold cover unbooked, handling fees tagged today).
- **Masterlist** (work list): tabs All | For Validation | Classified | For Renewal | Manual Disposition | Non-Renewal |
  Fallout; filters (segment, NB / RB, bucket, expiry month, insurer, handler, conversion status, migrated); bulk actions
  Assign Handler, Tag Renewable / Non-Renewable, Run Processing, Renew with BDOI, Export.
- **Policy record** (`/submitted/policies/:id`): summary card, `WorkflowPanel`, tabs Details | Loan & Matching | Rule
  Results | Review & IAAF | TOR | Renewal | Letters | Documents | History.
- **Upload & Intake**: source uploads (bulk wizard per handler), intake runs, LAMD snapshots.
- **Extraction Review**: proposals side by side with confirm / reject.
- **Processing Runs**: list, run detail with counts and fallout.
- **Policy Reviews / IAAF**: review queue, IAAF list and page with approvals.
- **TOR**: list and page.
- **Renewal Work List**: For Renewal | Hold Cover Pending | Insurer Not Accepted | Letters Due | Converted | Not Renewed;
  action Re-assign Insurer (BRIDSP-32).
- **Letters & Print Batches**.
- **Handling Fees**: records, tagger results, unmatched items (link to the Collections unapplied view).
- **No Touch Billing**: export, upload of returns, billing statements.
- **Submitted Policies Setup** (`SBM_RULE_MAINTAIN`): rule sets (maker-checker), limit rules, insurer rules, letter
  rules, approval matrices, sources, status map, user scope; links to the LOV maintenance of the SBM LOV types.
- **Reports**: the SBM reports in the Report Centre (category Submitted Policies), also linked from the group Reports.

## 13. Flyway plan (schema V1070-V1079, demo V1970-V1979)

The allocation fits in one block of ten; seven versions are used and three stay free.

| Version | Wave | Content |
|---|---|---|
| `V1070__sbm_foundation.sql` | S0 | Permission grants and roles (6.2), LOV types and seeds, workflows `SBM_POLICY` / `SBM_IAAF` / `SBM_TOR`, parameters, alert codes, notification events, event types `SBM_HANDLING_FEE` / `SBM_NO_TOUCH_FEE`, document templates (placeholders), jobs registry rows |
| `V1071__sbm_masterlist.sql` | S1-A | `sbm_source`, `sbm_intake_run`, `sbm_policy`, `sbm_policy_history`, `sbm_extraction`, `sbm_lamd_loan`, `sbm_status_map`, `sbm_user_scope` |
| `V1072__sbm_rules_processing.sql` | S1-B | `sbm_rule_set`, `sbm_rule`, `sbm_run`, `sbm_run_result`, `sbm_limit_rule`, `sbm_limit_check`; seed rule sets from the BRD lists (inactive where SQ05 is open) |
| `V1073__sbm_review_iaaf_tor.sql` | S1-C | `sbm_iaaf`, `sbm_iaaf_link`, `sbm_iaaf_review`, `sbm_tor`, `sbm_approval_matrix`, `sbm_signature` |
| `V1074__sbm_renewal_letters.sql` | S1-D | `sbm_renewal`, `sbm_insurer_rule`, `sbm_letter_rule`, `sbm_letter`, `sbm_print_batch` |
| `V1075__sbm_handling_fee_no_touch.sql` | S1-D | `sbm_handling_fee`, `sbm_no_touch_batch`, `sbm_no_touch_line`; SI type `SERVICE_FEE_NO_TOUCH` row in `bkg_si_type` (data only, no DDL on booking tables) |
| `V1076__sbm_reports_retention.sql` | S2 | Report archive settings, retention rule row, report metadata seeds if any |
| V1077-V1079 | - | Free |
| `V1970__demo_sbm_users_rules.sql` | S2 | Demo users, demo accounts 4115 / 1236 and rules, active demo rule sets, limit / insurer / letter rules, approval matrices |
| `V1971__demo_sbm_masterlist.sql` | S2 | ~60 masterlist records over the four segments (NB / RB, migrated ones), LAMD snapshot, one processing run with fallout |
| `V1972__demo_sbm_review_renewal.sql` | S2 | IAAFs at each stage, a TOR released, letters and a print batch, handling-fee records; the renewal accounts are created by a Java demo runner after the NB demo (as `BookingDemoData`) |

Rules: no foreign keys from V107x tables to V8xx tables (plain codes and ARNs); the owner-range changes (V822, V851,
V861 and the cashiering / collections seeds) are made by those owners in their own ranges.

## 14. Build-wave plan

Prerequisites: Collections C1 and Operations O1-A (cashiering) expose `UnappliedDirectory` /
`UnappliedDispositionRequests` implementations; BRD-5 report platform changes merged.

| Wave | Agent | Scope | Files owned | Done when |
|---|---|---|---|---|
| **S0** (one agent, sequential) | Foundation | `submitted` package skeleton, permissions, `ReportCategory` / factory, V1070; contract PRs to account (V822), booking, placement (V851), issuance (V861), opsledger port change; `RenewalHandOff`, `SubmittedSourceFeed`, `MailHouseGateway`, `SignatureProvider` interfaces | `submitted/package-info.java`, `submitted/service/port/**`, `security/domain/Permission.java`, `report/core/ReportCategory.java`, `ReportMetadata.java`, `account/**` (business type only), `booking/service/InvoiceBuilder.java`, `InvoiceBooked.java`, `placement/service/HoldCoverService.java`, `issuance/service/PolicyDataExtractor.java`, `opsledger/service/port/UnappliedDispositionRequests.java`, V1070, V822, V851, V861 | Build green; existing ITs pass; ports compile with default adapters |
| **S1-A** (parallel) | Intake & masterlist | Sources, bulk handlers, intake runs, extraction review, manual entry, masterlist, history, scope, migration, retention provider | `submitted/{domain,service,api}/intake/**`, `.../masterlist/**`, V1071, `features/submitted/{masterlist,intake,extraction}/**` | Upload → record; document → confirm; migration with error log |
| **S1-B** (parallel) | Rules & processing | Rule engine, steps, runs, results, limits, buckets, fallout, SBM_PROCESSING job | `submitted/.../processing/**`, V1072, `features/submitted/{runs,setup/rules}/**` | Seeded rules bucket the demo list; fallout rows with reasons |
| **S1-C** (parallel) | Review, IAAF, TOR | Reviews, IAAF, TOR, approval matrix, signatures, approval source, notifications | `submitted/.../review/**`, V1073, `features/submitted/{iaaf,tor}/**` | IAAF and TOR through two levels with the inbox; PDFs |
| **S1-D** (parallel) | Renewal, letters, fees | Expiry scan, default `RenewalHandOff`, insurer rules, hold-cover watch, letters and print batches, handling-fee tagger, No Touch billing | `submitted/.../renewal/**`, `.../fee/**`, V1074-V1075, `features/submitted/{renewal,letters,fees,notouch}/**` | Record → renewal account → hold cover → letters → booked status; handling fee tagged and applied via cashiering |
| **S2** (one agent) | Reports, home, demo, docs | 20 reports, home and counts, help, demo V1970-V1972 and runner, traceability, module guide `docs/modules/SUBMITTED_POLICIES.md`, Developer Guide range row | `submitted/report/**`, `features/submitted/{home,reports}/**`, `help.ts`, V1076, V1970-V1972, docs | Every report runs and exports in tests; ApiSmokeIT; screenshots |

Parallel-work rules:
- Only S0 edits files outside `submitted/**` and `features/submitted/**`; S1 agents ask S0 (or the owner) for any change
  outside.
- Each S1 agent owns its sub-packages, its migration and its screens; shared domain types (`SbmPolicy`, enums) are
  created in S0 and changed only by S1-A, by agreement.
- `features/submitted/module.ts` and `help.ts` are created in S0 with placeholder routes; S2 finalises them.
- No agent edits another module's migration or range; demo data waits for S2.

## 15. What depends on information BDOI has not given (build the seam, park the content)

| Item | BRD | Built | Parked content | Q |
|---|---|---|---|---|
| Source layouts and transports | 01 | Handlers with the RL #151 columns | Real layouts, feeds | SQ01, SQ02 |
| OCR | 02 | Text PDF extraction | OCR engine | SQ03 |
| Bucket rules | 11-15 | Engine + seeded rules | Precedence and conflicts | SQ04, SQ05 |
| LAMD | 13 | Snapshot upload | Feed and field meaning | SQ06 |
| IAAF / TOR matrices and templates | 05-07, 16-19 | Matrix tables, placeholder templates, stamped signature | Levels, signatories, layouts, e-signature | SQ07, SQ08 |
| Letters | 22 | Letter engine, rules, print batches | Templates, lead days, COG | SQ09 |
| Handling fee / No Touch accounting | 31, RL #164 | Events and demo rules | Accounts, rates, OR / SI rules | SQ13, SQ14, OQ07 |
| Renewal ownership | 23-26 | Default hand-off | Renewal BRD boundary | SQ10 |

## 16. Risks

1. **Overlap with the Renewal BRD.** Both may design expiry lists, RA letters and renewal status. Mitigation: the
   `RenewalHandOff` port and a single letter engine; agree ownership before S1-D.
2. **Bucket conflicts** (SQ05) could make the processing results disputed. Mitigation: rules are data, results record
   the rule version, and manual overrides are audited.
3. **Scanned documents** without OCR mean manual entry for most Non-CBG documents. Mitigation: side-by-side entry
   screen; OCR port ready.
4. **Handling-fee tagging depends on two modules being built** (cashiering, collections). Mitigation: the tagger works
   against the ports; with the default adapters the request is DEFERRED to the hand-off queue.
5. **2-second response NFR.** Mitigation: indexed masterlist queries, asynchronous runs and exports.
