# Operations (BRD-2)

Guide of the Operations modules of iNXT BrokerVerse: the shared invoice ledger (`opsledger`), and
cashiering, remittance, product reconciliation, adjustment and commission receivables. The build
design is [`docs/architecture/OPERATIONS_DESIGN.md`](../architecture/OPERATIONS_DESIGN.md); the
requirements are [`docs/requirements/BDOI_OPS_BRD_SPEC.md`](../requirements/BDOI_OPS_BRD_SPEC.md).
Each module adds its own section below the foundation.

## 1. Foundation (`opsledger` and platform extensions)

### 1.1 What the foundation provides

| Area | What exists | Where |
|---|---|---|
| Invoice ledger | One `ops_invoice` per booked invoice or endorsement, with parties, classification, insurer shares, component balances, flags, lock and statuses; every change is an `ops_invoice_movement` or an `ops_invoice_status_change` | `opsledger.domain`, `InvoiceLedgerService`, `InvoiceLedgerQueryService` |
| Feed from booking | `booking.service.InvoiceBooked` is recorded after commit, in its own transaction (`InvoiceLedgerFeed`); a failure is a failed record of flow-in feed `OPS_INVOICE_FEED`, raises `OPS_FLOW_IN_FAILED` and does not undo the booking | `InvoiceLedgerFeed`, `InvoiceLedgerWriter` |
| Replay | Rebuilds the ledger from `BookingQueryService` (invoice, account or company); idempotent per invoice. Job `OPS_INVOICE_FEED_REPLAY` (manual by default), endpoint `POST /api/v1/ops/invoices/replay`. The demo profile replays at start-up (`OpsLedgerDemoReplay`, after `BookingDemoData`) | `InvoiceFeedReplayService`, `InvoiceFeedReplayJob` |
| Invoice 360 | Header, components with outstanding balances, movements, related items of the Operations modules (ports), documents, workflow and history | `Invoice360Service`, `GET /api/v1/ops/invoices/{no}` |
| Operations home | Work tiles per team, filled by `OpsWorkCountSource` beans, plus ledger counts | `OperationsHomeService`, `GET /api/v1/ops/home` |
| Flow-in framework | Feeds, runs and records with idempotency keys; manual upload is the default transport | `FlowInService`, `FlowInHandler`, `/api/v1/ops/flow-in` |
| Disbursement queue | In-app queue until the Disbursement BRD (OQ02): SENT, ACKNOWLEDGED, DV_ASSIGNED, PAID, RETURNED | `DisbursementQueueService`, `/api/v1/ops/disbursements` |
| Extract repository | Files that would go to the shared drive (OQ17), stored with SHA-256 | `ExtractRepositoryService`, `GET /api/v1/ops/extracts` |
| Hand-offs | Work a default port adapter cannot do alone (receipt, unapplied) becomes an open hand-off for the team | `HandoffService`, `/api/v1/ops/handoffs` |
| Book rate | `RateType.BOOK` (2 decimals, `RateType.normalize`); `BookRates.rate(companyId, currency, date)` and `BookRates.price(BusinessEvent)` | `currency`, `opsledger.service.BookRates` |
| Bulk | TXT input (`TextLayout`), file hash with `BULK_DUPLICATE_FILE`, outcome categories, reprocess of failed rows | `bulk.service` |
| Reports | View / export permissions per report, archive of every run of an archived report (`report_run`) | `report.core` |
| Notifications | Preferences per user and event (in-app, e-mail) | `messaging.service.NotificationPreferenceService` |

### 1.2 Invoice ledger rules

- **Keys are plain values.** ARN, invoice number, client code and insurer code are stored as text;
  there are no foreign keys to the V8xx tables.
- **Components** (`LedgerComponent`): `BASIC`, `DST`, `PREMIUM_TAX_VAT`, `LGT`, `FST`, `OTHER` (the
  premium receivable, PR), `DTIP` (due to insurer), `COMMISSION`, `COMMISSION_VAT`, `WTAX`,
  `PR2307` (2% CWT portion). Payments apply in `LedgerComponent.applicationHierarchy()`: DST,
  premium tax / VAT, LGT, FST, other, basic.
- **Balance** per component = booked + adjusted − applied + reversed − remitted − written off
  (a database check keeps it).
- **Movements** (`MovementType`): `BOOKED`, `APPLIED`, `UNAPPLIED`, `REMITTED`, `ADJUSTED`,
  `WRITE_OFF`, `DP_REVERSAL`, `CWT_RECLASS`, `MIN_BAL`. Amounts are signed; a negative amount undoes
  a movement. A movement is unique on (source module, source reference, invoice, component,
  type), so posting the same request twice is a no-op.
- **Payment status** is derived from the PR balances: `UNPAID`, `PARTIALLY_PAID`, `PAID`;
  `NOT_APPLICABLE` for direct-payment and return invoices.
- **Remittance status**: `UNPROCESSED`, `UNAPPLIED_PAYMENT`, `WITH_OUTSTANDING_BALANCE`,
  `REVIEW_IN_PROCESS`, `REQUESTED_FOR_HOLD`, `APPROVED`, `PARTIALLY_REMITTED`, `FULLY_REMITTED`,
  `NOT_APPLICABLE`. The derived values follow the movements; the others are set by remittance with
  `InvoiceLedgerService.setRemittanceStatus`.
- **Flags** (`InvoiceFlag`): `HOLD`, `PENDING_NEG_ADJ`, `WRITTEN_OFF`, `CANCELLED`, `ESTIMATED`.
- **Lock.** A module locks an invoice with `lock(invoiceNo, owner, reason)`. While it is locked,
  other modules may only post `APPLIED` movements; anything else fails with `INVOICE_LOCKED`.
- **Direct payment** invoices carry their PR and DTIP amounts for information with status
  `NOT_APPLICABLE`; booking does not post a PR for them.
- **Adjustment totals** (`ops_invoice_adjustment_total`) keep the original and adjusted premium,
  DTIP and commission per original invoice; `ADJ_OVER_BASELINE` is raised when the adjusted premium
  goes above the original.

### 1.3 Ports and default adapters

All ports are in `opsledger.service.port`. The defaults are declared in `OpsPortDefaults` with
`@ConditionalOnMissingBean`. An Operations module replaces a default by declaring a component-scanned
`@Service` that implements the port. Its tests then run alone, and the default serves every other
module's tests.

| Port | Signature | Default | Replaced by |
|---|---|---|---|
| `ReceiptIssuer` | `IssuedReceipt issueOfficialReceipt(ReceiptRequest)` | `HandoffReceiptIssuer`: `DEFERRED` + hand-off for team `CASH_RECEIPT` | cashiering |
| `UnappliedSink` | `UnappliedHandle create(UnappliedRequest)` | `HandoffUnappliedSink`: `DEFERRED` + hand-off for team `CASH_DISPOSITION` | cashiering |
| `PaymentReapplier` | `ReapplyResult reapply(ReapplyRequest)` | `LedgerPaymentReapplier`: `PAYMENT_REAPPLIER_UNAVAILABLE` | cashiering |
| `DisbursementGateway` | `DisbursementTicket send(Long companyId, DisbursementRequest.Spec)`; `Optional<DisbursementTicket> status(String sourceModule, String sourceRef)` | `QueueDisbursementGateway` (in-app queue) | Disbursement BRD |
| `CollectionFeed` | `String transport()`; `List<FeedItem> pending(Long companyId, String feedCode)`; `String send(Long companyId, String feedCode, List<FeedItem>)` | `ManualCollectionFeed`: CSV in the extract repository, folder `COLLECTION/<feed>` | Collection interface (OQ01) |
| `InsurerFileInbox` | `String transport()`; `List<InboxFile> pending(Long companyId, String insurerCode, String fileType)` | `ManualInsurerFileInbox` (manual upload, empty inbox) | insurer channels |
| `FileDropPort` | `DroppedFile drop(Long companyId, ExtractFile.Location, DropContent, ExtractFile.Origin)` | `RepositoryFileDrop` (extract repository) | shared drive (OQ17) |
| `MarketingFeed` | `List<FeedItem> fetch(Long companyId, String feedCode, LocalDate since)` | none (optional bean) | Marketing interface (OQ45) |
| `ClaimsFeed` | same as `MarketingFeed` | none (optional bean) | Claims BRD (OQ46) |

Extension points read by the foundation (any number of beans):
- `InvoiceRelatedItems`: `Section section()` and `List<RelatedItem> itemsFor(String invoiceNo)`.
  These are the tabs of Invoice 360. Sections: `RECEIPTS`, `REMITTANCES`, `ADJUSTMENTS`,
  `RECONCILIATION`, `COMMISSION`, `DOCUMENTS`.
- `OpsWorkCountSource`: `List<WorkCount> counts(Long companyId)`. These are the tiles of the
  Operations home. Sections: `CASHIERING`, `REMITTANCE`, `PRODRECON`, `ADJUSTMENT`, `COMMISSION`,
  `DISBURSEMENT`, `INTERFACES`.
- `FlowInHandler`: `String feedCode()` and `void handle(FlowInFile, FlowInContext)`. A feed with a
  handler accepts uploads. Inside the handler, call `context.accept(idempotencyKey, payload, work)`
  once per record. A key that was already accepted is skipped as a duplicate.

### 1.4 Payment confirmation to placement (plan for cashiering)

Placement already sweeps confirmed payments through `placement.service.PaymentConfirmationSource`
(job `PAYMENT_CONFIRMATION_SWEEP`, BROKING_ARCHITECTURE section 13). Cashiering provides the adapter.
There is no second payment intake.

- Add a `@Component` that implements `PaymentConfirmationSource` with `sourceCode()` = `"CASHIERING"`.
- `confirmedFor(companyId, arns)` returns one `ConfirmedPayment(arn, "APP:<applicationId>", amount,
  paidOn, description)` per receipt application of those accounts that is not cancelled. The
  application id in the reference keeps each confirmation unique for the sweep.
- Placement's sweep marks the placement paid. Cashiering does not call placement.

### 1.5 Flow-in feeds (V761)

`OPS_INVOICE_FEED` (booking to ledger), `COLLECTION_CHECK_PICKUP`, `COLLECTION_CWT2307`,
`COLLECTION_COMMISSION_PAYMENT`, `COLLECTION_HOLD`, `COLLECTION_SPECIAL_REMIT`,
`COLLECTION_DP_LIST`, `COLLECTION_DP_RETURNED`, `COLLECTION_REFUND`, `DISBURSEMENT_REQUEST`,
`DISBURSEMENT_STATUS`, `INSURER_REMIT_OR`, `INSURER_PRODUCTION`, `INSURER_DP_RESPONSE`.
Administrators with `FLOWIN_MANAGE` set the schedule and activation, upload files and read runs
and records on *Operations → Interfaces*.

### 1.6 Events (`opsledger.service.OpsLedgerEvents`, published in the posting transaction)

| Event | Fields |
|---|---|
| `OpsInvoiceBooked` | companyId, invoiceNo, arn, kind, clientCode, insurerCode, policyNo, directPayment, source |
| `InvoiceMovementPosted` | companyId, invoiceNo, type, sourceModule, sourceRef, amounts per component |
| `InvoiceFlagChanged` | companyId, invoiceNo, flag, value, module, reason |
| `RemittanceStatusChanged` | companyId, invoiceNo, from, to, module |
| `InvoiceLocked` / `InvoiceUnlocked` | companyId, invoiceNo, owner (+ reason) |
| `NegativeAdjustmentPending` | companyId, invoiceNo, requestNo, pending, requestedBy |
| `DisbursementStatusChanged` | companyId, requestNo, type, sourceModule, sourceRef, status, dvNo, reason |

Listen with `@TransactionalEventListener(phase = AFTER_COMMIT)` and do the work in a new
transaction.

### 1.7 Reference data (V760, V762)

- **Roles:** `CASHIER`, `CASHIER_TL`, `REMIT_PROCESSOR`, `REMIT_TL`, `RECON_HANDLER`,
  `ADJUSTMENT_TL`, `COMMREC_HANDLER`, `COMMREC_TL`, `MKT_COLLECTION`, `COMPTROLLERSHIP`,
  `DISBURSEMENT`.
- **Demo users** (V990, password `Brokerverse@2026`): `cashier`, `cashbr` (Cebu branch), `cashtl`,
  `remit`, `remittl`, `recon`, `adjtl`, `commrec`, `commtl`, `mktcoll`, `comptrol`, `disb`.
- **LOV types**, parameters and alert codes: OPERATIONS_DESIGN section 10. The lists the BRD
  gives are seeded as written. `RECON_COMPANY_CONCERNED`, `RECON_DISPOSITION` and
  `OPS_EXTERNAL_LINK` are empty. `HOLD_REASON`, `REMIT_RETURN_REASON`, `ENDORSEMENT_DOC_TYPE` and
  `DP_FEEDBACK_REASON` hold only "Others" until BDOI supplies values (OQ24, OQ31, OQ32, OQ40).
- **Notification events** (`msg_notification_event`): `OPS_INVOICE_LOCKED`, `OPS_FLOW_IN_FAILED`,
  `OPS_DISBURSEMENT_STATUS`, `REMIT_EXTRACTION_DONE`, `REMIT_BATCH_FOR_APPROVAL`,
  `REMIT_BATCH_DECIDED`, `REMIT_NEG_ADJ_PENDING`, `HOLD_EXPIRING`, `CASH_APPROVAL_REQUEST`,
  `RECON_FEEDBACK_UPLOADED`, `ADJ_REQUEST_STATUS`, `DP_FEEDBACK_OVERDUE`. Send with
  `NotificationService.notifyUser(username, notice, eventCode)` or
  `notifyPermission(permission, notice, eventCode)`, which respect the user's preference.
- **Demo GL** (V990): 1211, 1225, 1230, 2211, 2215, 2216, 2230, 4110, 4120, 4130, 6510. BOOK rates
  are the SPOT rates rounded to 2 decimals.

### 1.8 Jobs

| Property (`brokerverse.jobs.*`) | Default (UTC) | Job |
|---|---|---|
| `ops-invoice-feed-replay-cron` | `-` (manual) | `OPS_INVOICE_FEED_REPLAY` (built) |
| `prebooked-rematch-cron` | `0 0 */2 * * *` | `PREBOOKED_REMATCH` |
| `payment-automatch-cron` | `0 30 * * * *` | `PAYMENT_AUTOMATCH` |
| `pdc-maturity-cron` | `0 30 0 * * *` | `PDC_MATURITY` |
| `minimal-balance-sweep-cron` | `0 0 20 * * *` | `MINIMAL_BALANCE_SWEEP` |
| `remittance-extraction-cron` | `0 0 12 * * *` | `REMITTANCE_EXTRACTION` |
| `hold-expiry-cron` | `0 15 0 * * *` | `HOLD_EXPIRY` |
| `production-extract-cron` | `0 0 1 * * *` | `PRODUCTION_EXTRACT` |
| `recon-automatch-cron` | `-` (manual) | `RECON_AUTOMATCH` |
| `adj-daily-report-cron` | `0 0 10 * * *` | `ADJ_DAILY_REPORT` |
| `dp-feedback-sla-cron` | `0 30 1 * * *` | `DP_FEEDBACK_SLA` |

The Operations modules implement `ManagedJob` and read their cron with
`@Value("${brokerverse.jobs.<property>:-}")`.

### 1.9 Screens

| Route | Screen | Permission |
|---|---|---|
| `/operations` | Operations home (work tiles per team) | `OPS_VIEW` |
| `/operations/invoices` | Invoice search | `OPS_VIEW` |
| `/operations/invoices/:no` | Invoice 360 (hidden, opened from search) | `OPS_VIEW` |
| `/operations/disbursements` | Disbursement queue | `DISB_PROCESS` |
| `/operations/handoffs` | Hand-offs | `OPS_VIEW` |
| `/operations/interfaces` | Interfaces (flow-in) | `FLOWIN_MANAGE` |
| `/operations/report-archive` | Report archive | `OPS_REPORT_VIEW` |
| `/operations/notifications` | Notification settings | `OPS_VIEW` |

The modules follow BDOI's prototype. Product reconciliation (`/prodrecon`) and adjustment
(`/adjustment`) are in *Client & Policy*. Cashiering (`/cashiering`), remittance (`/remittance`) and
commission receivables (`/commission`) are in *Finance*. The foundation registered each of them as a
stub home page with a help section, so a module fills in only its own feature folder.

### 1.10 Parked (seam only)

| Item | Question | Seam |
|---|---|---|
| Collection system interface | OQ01 | `CollectionFeed` (CSV in the extract repository), `COLLECTION_*` feeds by manual upload |
| Disbursement system | OQ02 | `DisbursementGateway` and the in-app queue |
| Marketing and Claims feeds | OQ45, OQ46 | `MarketingFeed`, `ClaimsFeed` without adapters |
| Accounting interface / GL mapping | OQ07 | demo GL only; production configures the Accounting Rules |
| Insurer channels (SFTP / API) | - | `InsurerFileInbox`, manual upload |
| Shared drive | OQ17 | `FileDropPort` to the in-system extract repository |
| Source of the BOOK rate | OQ08 | `RateType.BOOK` is kept by hand on *Currency Rates* (demo = SPOT rounded) |
| Lock reasons and who may lift them | OQ28 | free-text reason, owner module unlocks |
| Open LOVs | OQ24, OQ31, OQ32, OQ40 | LOV types exist, empty or with "Others" only |
| Access matrix per role | OQ48 | grants in V760 follow OPERATIONS_DESIGN section 6 |
| Direct payment PR | - | booking posts no PR; the ledger keeps the amounts for information (commission uses them) |

## 5. Product reconciliation (`prodrecon`)

Production Reconciliation sends each insurer a register of the accounts BDOI booked for it, takes in
the insurer's answer, matches both sides and follows every difference to closure. Requirements
PRCID.001-039. Migrations: V775 (schema, workflow, parameters, template) and V993 (demo LOVs and
schedules).

### 5.1 Model

- **Schedule** (`prc_schedule`): one per insurer. It sets the frequency (monthly on a day of the
  month, or weekly on a weekday), whether the register is sent automatically, and the recipients.
  The next run date moves to the next working day on the head office calendar.
- **Cycle** (`prc_cycle`): one open cycle per company, insurer and production month (unique index on
  the open cycles). It is a work case of the workflow `OPS_RECON`:
  - `EXTRACTED` to `SENT_TO_INSURER` (`send`, `resend`, `RECON_SEND`);
  - `feedback_uploaded` to `RECONCILING`;
  - `close` or the system action `for_closure` to `CLOSED`.

  The stage is mirrored on the cycle, which also keeps the counts per bucket.
- **Extract** (`prc_extract` / `prc_extract_line`): a register of the accounts booked in a period,
  either scheduled or manual. It is stored in the extract repository (`ops_extract_file`, folder
  `PRODRECON/<insurer>`). Each line records the ledger facts, the last payment and the estimated flag.
  An account that was not on an earlier register of the cycle counts as new.
- **Upload** (`prc_upload`): each insurer feedback file, with its attempt number, row counts,
  status and flow-in run. A file with the same content as an earlier one is `DUPLICATE_BLOCKED`
  (PRCID.010/032).
- **Item** (`prc_item`): one reconciliation line holding the BDOI side (`bdoi_*`) and the insurer
  side (`ins_*`).
  - Status: `MATCHED`, `MATCHED_WITH_DISCREPANCY`, `BDOI_ONLY`, `UNMATCHED_PREBOOKED` or
    `UNMATCHED_NO_BOOKING`.
  - Match method: `AUTO` or `MANUAL`.
  - Feedback: company concerned, instruction, insurer feedback, marketing feedback, disposition and
    for-closure.
  - Unbooked status for insurer-only lines: `OPEN`, `PREBOOKED`, `BOOKED` or `CLOSED`.

### 5.2 Rules

- **Register** (`ProductionRegisterWorkbook`): an XLSX workbook in the `RegisterLayout` columns. The
  sheet is protected, and only the Remarks and Incentive columns plus 200 blank rows can be edited by
  the insurer (PRCID.002/006). The file is named from `PRODRECON_FILE_PATTERN`; the default is
  `<INSURER>_PRODREG_<yyyyMM>_<seq>` (PRCID.004).
- **Sending** (`ReconSendService`): the `PRODRECON_COVER_LETTER` template is sent through the
  messaging e-mail with a password-protected attachment. When no recipients are given, the insurer's
  e-mail addresses are used. The sent time and the recipients are stored on the extract
  (PRCID.003/007/008).
- **Matching** (`ReconMatcher`):
  - Pairing uses the keys of `RECON_MATCH_KEYS`, in order (default `INVOICE_NO,POLICY_NO`).
  - Amount fields (commission, basic premium, gross premium) match when they differ by no more than
    `RECON_TOLERANCE` (1.00).
  - Policy, reference and PN numbers and the period must be equal. Names are compared ignoring case
    and spacing.
  - An item with any difference is `MATCHED_WITH_DISCREPANCY` and lists the differing fields
    (PRCID.026/027).
- **Insurer-only lines**: the Booking module is searched for a pre-booked account (ARN) with the same
  reference. The line becomes `UNMATCHED_PREBOOKED` if one is found and `UNMATCHED_NO_BOOKING`
  otherwise (PRCID.023).
- **Booking** of a later invoice matches the waiting insurer-only line of the same insurer
  (`ReconBookingListener`, after commit). `RECON_AUTOMATCH` matches every open cycle again
  (PRCID.024/025).
- **Manual matching**: a BDOI-only item can be paired with an insurer-only line, and a wrong pairing
  can be split back into its two sides (PRCID.014).
- **Closing**: a cycle closes by itself (`for_closure`) when every item is matched or marked for
  closure. Otherwise the user closes it with a comment.
- **Early incentive** (PRCID.028): each booked account's remittance date is checked against the
  insurer's rate and window from the `EarlyIncentiveRules` port. The default adapter returns no rule,
  so the result is `NO_RULE` until remittance supplies the rates (OQ23).

### 5.3 API (`/api/v1/prodrecon`)

| Method and path | Purpose | Permission |
|---|---|---|
| `GET /cycles`, `GET /cycles/{id}` | Cycles by insurer, month and stage | `RECON_PROCESS` or `RECON_SEND` |
| `GET /cycles/{id}/items` | Items by bucket, search, AO, sales unit, segment, product line | read |
| `POST /cycles/{id}/automatch`, `POST /cycles/{id}/close` | Match again, close | `RECON_PROCESS` |
| `GET /cycles/{id}/early-incentive` | Early incentive validation | read |
| `PUT /items/{id}/feedback`, `POST /items/feedback` | Feedback on one or many items | `RECON_PROCESS` |
| `POST /items/{id}/pair`, `POST /items/{id}/split` | Manual matching | `RECON_PROCESS` |
| `GET /unbooked` | Unbooked repository | read |
| `POST /extracts`, `GET /extracts`, `GET /cycles/{id}/extracts` | Manual extract, extract log | `RECON_PROCESS` / read |
| `GET /extracts/{id}/lines`, `GET /extracts/{id}/file` | Register lines and file | read |
| `POST /extracts/{id}/send` | Send to the insurer | `RECON_SEND` |
| `POST /uploads` (multipart), `GET /uploads` | Insurer feedback upload and history | `RECON_PROCESS` / read |
| `GET/POST /schedules`, `PUT /schedules/{id}` | Extract schedules | read / `RECON_PROCESS` |
| `GET /settings` | Tolerance and match keys | read |

Flow-in feed: `INSURER_PRODUCTION` (`InsurerProductionHandler`). Jobs: `PRODUCTION_EXTRACT`
(`production-extract-cron`) and `RECON_AUTOMATCH` (`recon-automatch-cron`).

### 5.4 Reports (`ReportMetadata.operations`)

| Code | Report | BR |
|---|---|---|
| `PRC-SUMMARY` | Production reconciliation summary per insurer and month | PRCID.035 |
| `PRC-UNMATCHED-LOC` | Unmatched accounts per BDOI location | PRCID.036 |
| `PRC-UNMATCHED-AO` | Unmatched accounts per marketing AO / AB (booked and pre-booked) | PRCID.037 |
| `PRC-DISPOSITION` | Summary per disposition | PRCID.039 |
| `PRC-REGISTER` | Production register; variants plain, with insurer feedback, with marketing feedback, both | PRCID.017/018/020 |
| `PRC-UNBOOKED` | Unbooked accounts and status | PRCID.019/033 |
| `PRC-EXTRACT-LOG` | Item count and extraction time per extract | PRCID.034 |
| `PRC-UNMATCHED-FEEDBACK` | Unmatched accounts with feedback and disposition | PRCID.038 |
| `PRC-EARLY-INCENTIVE` | Early incentive validation | PRCID.028 |

### 5.5 Screens (*Client & Policy* / Product Reconciliation)

| Route | Screen |
|---|---|
| `/prodrecon` | Reconciliation Workbench (tiles from `ProdReconWorkCounts`) |
| `/prodrecon/cycles` | Reconciliation Cycles (tabs by stage) |
| `/prodrecon/cycles/:id` | Cycle record page: summary, `WorkflowPanel` (`ReconCycle`), Items by bucket with a side-by-side comparison, feedback, pair / split, bulk disposition and filters, Registers Sent, Early Incentive, Documents |
| `/prodrecon/extracts` | Production Extracts (New Extract, download, send) |
| `/prodrecon/uploads` | Insurer Feedback (upload, attempts) |
| `/prodrecon/unbooked` | Unbooked Accounts |
| `/prodrecon/schedules` | Extract Schedules |

The module implements `OpsWorkCountSource` with these tiles: registers to send, cycles awaiting
feedback, cycles reconciling, and unbooked accounts. It also implements `InvoiceRelatedItems` with
the reconciliation items of an invoice.

### 5.6 Fit/gap status

| BR | Status |
|---|---|
| PRCID.001 | Built: schedules and `PRODUCTION_EXTRACT`, with holidays rolled. Frequency values are parked (OQ29). |
| PRCID.002, 006 | Built: protected workbook with only Remarks and Incentive editable. The per-insurer template is parked (OQ29). |
| PRCID.003, 004, 007, 008 | Built: cover letter template, naming parameter, protected e-mail, and the sent time and recipients. The password convention and final texts are parked (OQ29). |
| PRCID.005, 011, 012, 013, 020, 034 | Built: extract repository, manual extract, extract lines, filters, extract log. |
| PRCID.009, 010, 022, 031, 032 | Built: `INSURER_PRODUCTION` upload with separation of accounts not in the original extract, duplicate block, attempts. Insurer channels are parked (upload only). |
| PRCID.014, 021 | Built on the items: AO, sales unit, segment and product line. The reports filter by insurer and month; a location filter on the items is not built. |
| PRCID.015, 016, 017, 018, 038, 039 | Built: feedback fields and register variants. The company-concerned and disposition lists are demo values (OQ31). |
| PRCID.019, 023, 033 | Built: unbooked repository with the pre-booked lookup. |
| PRCID.024, 025, 026, 027, 030 | Built: match on booking, `RECON_AUTOMATCH`, tolerance, criteria, statuses. The keys and the timing are parameters (OQ30). |
| PRCID.028 | Seam: the `EarlyIncentiveRules` port and the report. The rates are parked with remittance (OQ23). |
| PRCID.029 | Built: workflow history and audit trail. |
| PRCID.035, 036, 037 | Built: reports. |

### 5.7 Parked

| Item | Question | Seam |
|---|---|---|
| Extract frequency, template, naming, password per insurer | OQ29 | `prc_schedule`, `PRODRECON_FILE_PATTERN`, `PRODRECON_COVER_LETTER` |
| Match keys and automatch time | OQ30 | `RECON_MATCH_KEYS`, `RECON_TOLERANCE`, `recon-automatch-cron` |
| Company concerned and disposition lists | OQ31 | LOVs `RECON_COMPANY_CONCERNED`, `RECON_DISPOSITION` (demo values in V993) |
| Early incentive rates and windows | OQ23 | `prodrecon.service.port.EarlyIncentiveRules` (default: no rule) |
| Insurer channels (SFTP / API) and shared drive | OQ17 | Manual upload to `INSURER_PRODUCTION`; `FileDropPort` to the extract repository |
| Sum insured on the register | - | Not in the Operations ledger; column left out |

## 6. Commission receivables (`commission`)

Commission Receivables handles direct payment accounts, where the client paid the insurer directly.
It bills the insurer for BDOI's commission, follows the insurer's answer, collects the commission
and reverses the premium receivable. It also runs the incentive programmes and tracks BIR
certificates. Requirements CMRID.001-015 and MKTID.012. Migrations: V785 (schema, workflows, event
types, parameters) and V995 (demo accounting rules, collection bank, inactive demo schemes).

### 6.1 Model

- **DP list** (`cmr_dp_list`): a list of direct payment accounts from a branch, Head Office or the
  collection feed. The file name must follow the `<Branch>_DP_<yyyyMMdd>` convention, and a file
  with the same content is refused (CMRID.001).
- **DP account** (`cmr_dp_item`): one row of a list.
  - What it holds: the submitted values, the ledger amounts (premium, commission, VAT, withholding
    tax, net), the result of each validation rule, the insurer answer, the collection and the
    reversal count.
  - Tag: `DP_FOR_CONFIRMATION`, `DP_FOR_BILLING`, `BILLED`, `APPROVED`, `REJECTED`, `COLLECTED`,
    `PR_REVERSED` or `EXCLUDED`.
  - Sanitation: `VALID`, `DUPLICATE`, `INVALID` or `INCOMPLETE`.
- **Billing** (`cmr_billing`): the confirmed accounts of one insurer. It is a work case of
  `OPS_DP_BILLING`:
  - `DP_FOR_BILLING` to `AWAITING_INSURER` (`bill`);
  - then `APPROVED` or `RETURNED_TO_COLLECTION`;
  - then `COLLECTED` and `CLOSED`;
  - or `CANCELLED` before it is sent.

  The billing holds the file, the recipients, the answer due date, the OR and the OR status.
  "DP for confirmation" is a tag on the account, not a stage of the billing.
- **Incentive scheme** (`cmr_incentive_scheme` / `cmr_incentive_tier`): the scheme type (No Touch,
  Top Up, Motor Mania, Other), calculation (`TARGET_TIERED` or `FIXED_PER_POLICY`), period,
  beneficiary (BDOI or branch), insurer, segments, product lines and tiers.
- **Incentive run** (`cmr_incentive_run` / `_line`): a scheme computed on a booking period. It is
  `COMPUTED`, then `POSTED` or `CANCELLED`, and keeps its totals and one line per invoice with the
  exclusion reason.
- **Certificate submission** (`cmr_certificate` / `cmr_certificate_or`): a BIR certificate tagged
  to the ORs it covers. It is a work case of `OPS_BIR_CERT`: `SUBMITTED`, then `ACKNOWLEDGED` or
  `REJECTED`; a rejected submission can be resubmitted.

### 6.2 Rules

- **Validation** (`DpValidator`, CMRID.002/008/013): an account passes when the invoice is booked,
  tagged direct payment, not cancelled, of the listed insurer and premium, has no pending negative
  adjustment, and still has commission open. The same invoice already active on another list is a
  `DUPLICATE`. The rule results are kept per account.
- **Confirmation and billing** (CMRID.009/013): the user confirms that the client paid the insurer
  in full, then prepares the billing, one per insurer. The billing workbook is built through
  `DocumentComposer` and sent by protected e-mail. The answer is due `CMR_FEEDBACK_WORKING_DAYS`
  (10) working days later on the head office calendar. `DP_FEEDBACK_SLA` raises
  `DP_FEEDBACK_OVERDUE` for overdue billings (CMRID.011).
- **Answers** (CMRID.009/012): answers are entered on screen or uploaded through the
  `INSURER_DP_RESPONSE` feed (columns Billing No., Invoice No., Decision, Reason, Comment).
  - A rejection needs a `DP_FEEDBACK_REASON`.
  - Rejected accounts go back to the collection team through the `COLLECTION_DP_RETURNED` extract of
    the `CollectionFeed` port.
  - A billing whose accounts were all rejected moves to `RETURNED_TO_COLLECTION`.
- **Collection** (CMRID.010, MKTID.012):
  - `OPS_DP_COMMISSION_COLLECT` posts cash to the bank (`CMR_DP_COLLECTION_BANK` or the one
    entered), the withholding tax to CWT, and realises the commission receivable and output VAT.
  - For each approved account, the ledger records a `DP_REVERSAL` of the premium receivable and DTIP
    and an `APPLIED` commission movement through `InvoiceLedgerService.post`.
  - The OR is requested from `ReceiptIssuer`. With the default hand-off it is `DEFERRED`.
- **PR reversal posting**: booking posts no PR for direct payment invoices. The GL events
  `OPS_DP_PR_REVERSAL` and `OPS_DP_REINSTATE` are therefore published only when
  `DP_PR_REVERSAL_POSTING` is true. The ledger movement is always recorded.
- **Reinstatement** (CSHID.004 b): a reversed account can be reinstated with a `DP_*` reason of
  `REINSTATEMENT_REASON`, then reversed again. `DP_CANCELLATION` also sends the collected commission
  to `UnappliedSink`.
- **Incentives** (CMRID.003/005/006):
  - `IncentiveEngine` applies the rate of the highest production target reached (times the
    multiplier) to every eligible invoice. For Motor Mania, it pays each policy the fixed amount of
    the highest minimum basic premium the policy meets.
  - Negative amounts and erroneous bookings (cancelled or written off) are excluded when active in
    `INCENTIVE_EXCLUSION_RULE`, and raise `INCENTIVE_EXCLUSION`.
  - Posting (`COMMREC_APPROVE`) publishes `OPS_INCENTIVE_ACCRUE` per insurer. For branch schemes it
    also publishes `OPS_INCENTIVE_PASS_ON` per sales unit, with a `PASS_ON` request to
    `DisbursementGateway`.
- **Certificates** (CMRID.015): a certificate must cover at least one OR. Comptrollership
  (`BIR_CERT_ACK`) acknowledges it or rejects it with a reason.
- **Estimated items** (CMRID.014): the ledger's `ESTIMATED` flag is set or cleared through
  `InvoiceLedgerService`. It shows on the production register and on the yearly production report.
- **Co-insurance**: the commission party of a direct payment account is the lead insurer.

### 6.3 API (`/api/v1/commission`)

| Method and path | Purpose | Permission |
|---|---|---|
| `GET /dp/lists`, `POST /dp/lists` (multipart), `POST /dp/lists/pull` | Lists, upload, pull from the collection feed | `COMMREC_PROCESS` (read also `COMMREC_APPROVE`) |
| `GET /dp/submissions` | Branch submission tracker | read |
| `GET /dp/items`, `GET /dp/items/counts` | Accounts by tag, sanitation, insurer, list, text | read |
| `POST /dp/items/confirm`, `/exclude`, `/{id}/revalidate`, `/{id}/reverse`, `/{id}/reinstate` | Account actions | `COMMREC_PROCESS` |
| `POST /dp/billings`, `GET /dp/billings`, `GET /dp/billings/{id}`, `GET /dp/billings/{id}/items` | Prepare and list billings | `COMMREC_PROCESS` / read |
| `POST /dp/billings/{id}/send`, `/cancel`, `/answers`, `/collect`; `GET /dp/billings/{id}/file` | Billing actions | `COMMREC_PROCESS` / read |
| `POST /dp/responses` (multipart) | Insurer answers file | `COMMREC_PROCESS` |
| `GET/POST /incentives/schemes`, `GET/PUT /incentives/schemes/{id}` | Schemes and tiers | read / `INCENTIVE_MANAGE` |
| `GET/POST /incentives/runs`, `GET /incentives/runs/{id}[/lines]`, `POST /incentives/runs/{id}/cancel` | Runs | read / `INCENTIVE_MANAGE` |
| `POST /incentives/runs/{id}/post` | Post a run | `COMMREC_APPROVE` |
| `GET/POST /certificates`, `GET/PUT /certificates/{id}`, `GET /certificates/receipts` | Certificate submissions | `BIR_CERT_SUBMIT` (read also `BIR_CERT_ACK`) |
| `POST /certificates/{id}/acknowledge`, `/reject` | Comptrollership decision | `BIR_CERT_ACK` |
| `GET/POST /estimated` | Estimated items | read / `COMMREC_PROCESS` |
| `GET /settings` | Feedback days, collection bank, PR reversal posting | read |

Flow-in feeds: `COLLECTION_DP_LIST` (`DpListHandler`) and `INSURER_DP_RESPONSE`
(`DpResponseHandler`). Job: `DP_FEEDBACK_SLA` (`dp-feedback-sla-cron`). Event types:
`OPS_DP_COMMISSION_COLLECT`, `OPS_DP_PR_REVERSAL`, `OPS_DP_REINSTATE`, `OPS_INCENTIVE_ACCRUE` and
`OPS_INCENTIVE_PASS_ON`.

### 6.4 Reports (`ReportMetadata.operations`)

| Code | Report | BR |
|---|---|---|
| `CMR-COMMISSION-RECEIVABLE` | Commission booked, collected and outstanding, DP / direct bill against regular, per insurer | CMRID.004 |
| `CMR-PRODUCTION-YEARLY` | Yearly production per branch and insurer, with estimated items | CMRID.014 |
| `CMR-DP-STATUS` | Direct payment accounts by status | CMRID.008 |
| `CMR-INCENTIVE` | Incentive runs and their invoices | CMRID.005/006 |
| `CMR-FEEDBACK-SLA` | Insurer feedback timeliness per billing | CMRID.011 |
| `CMR-BIR-CERT` | BIR certificate submissions and their ORs | CMRID.010/015 |

### 6.5 Screens (*Finance* / Commission Receivables)

| Route | Screen |
|---|---|
| `/commission` | Commission Workbench (tiles from `CommissionWorkCounts`) |
| `/commission/dp/lists` | DP Lists (upload, pull, branch submissions) |
| `/commission/dp/items` | DP Accounts (tabs by tag, confirm, exclude, prepare billing, account detail with rules, reinstate / reverse) |
| `/commission/dp/billings` | DP Billings (tabs by stage) |
| `/commission/dp/billings/:id` | Billing record page: summary, `WorkflowPanel` (`DpBilling`), send, answers, collection, cancel, accounts, documents |
| `/commission/dp/responses` | Insurer Responses (awaiting billings, answers upload) |
| `/commission/incentives/schemes` | Incentive Schemes (tier editor) |
| `/commission/incentives/runs` | Incentive Runs (compute, review lines, post, cancel) |
| `/commission/certificates` | BIR Certificates |
| `/commission/certificates/:id` | Certificate record page: `WorkflowPanel` (`BirCertificate`), acknowledge, reject, resubmit, ORs, scanned copy |
| `/commission/estimated` | Estimated Items |

The module implements `OpsWorkCountSource` with these tiles: accounts to confirm, accounts for
billing, billings awaiting the insurer, overdue feedback, billings to collect, and certificates to
acknowledge. It also implements `InvoiceRelatedItems` with the direct payment accounts of an
invoice.

### 6.6 Fit/gap status

| BR | Status |
|---|---|
| CMRID.001 | Built: list upload with the naming check, duplicate block and branch tracker. The folders and the Collection system are parked (OQ38): pull through `CollectionFeed`, upload now. |
| CMRID.002, 007, 008, 013 | Built: validation rules on the ledger, computed amounts, tags and sanitation. |
| CMRID.003, 005, 006 | Built: scheme engine, exclusions and posting. The demo schemes have no tiers until BDOI gives targets and amounts (OQ39). |
| CMRID.004, 014 | Built: reports. Final layouts are parked (OQ42); the estimated-item definition is parked (OQ27). |
| CMRID.009, 012 | Built: billing per insurer with its handler, protected e-mail, answers on screen or by file. The billing and answer formats are parked (OQ38/OQ40). |
| CMRID.010 | Built: OR request (`ReceiptIssuer`), withholding tax on the collection, certificates and feedback. |
| CMRID.011 | Built: working-day due date and `DP_FEEDBACK_SLA`. |
| CMRID.015 | Built: certificate submission tagged to ORs with the Comptrollership workflow. |
| MKTID.012 | Built: PR reversal and reinstatement in the ledger. The GL posting is behind `DP_PR_REVERSAL_POSTING` until the direct payment PR accounting is agreed (OQ07). |

### 6.7 Parked

| Item | Question | Seam |
|---|---|---|
| DP list folders and Collection system transport | OQ38 | `COLLECTION_DP_LIST` upload, `CollectionFeed` pull |
| Billing and insurer answer formats, insurer channels | OQ38, OQ40 | `DpBillingSender` columns, `INSURER_DP_RESPONSE` layout |
| Incentive targets, tiers and amounts | OQ39 | Inactive demo schemes without tiers |
| OR issuance for DP collections | OQ41 | `ReceiptIssuer` (default: deferred hand-off) |
| GL accounts of the DP collection and PR reversal | OQ07 | Demo rules in V995, `DP_PR_REVERSAL_POSTING` off |
| Branch pass-on payment | OQ02 | `DisbursementGateway` `PASS_ON` request |
| Co-insurance split of DP commission | - | Lead insurer is the commission party |
