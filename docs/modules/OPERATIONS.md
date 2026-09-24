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

## 3. Remittance (`remittance`)

Remittance of paid premiums to the insurers, from extraction through the insurer's OR. It also covers
the Marketing hold and special remittance requests (RMTID.001-025/027-031/033-036/039,
MKTID.001-007/009). The code is in package `com.iortatechnxt.brokerverse.remittance`, with Flyway
`V770__remittance.sql` and `V771__remittance_workflows_and_events.sql`. The demo is
`V992__demo_remittance.sql` plus `remittance.demo.RemittanceDemoData`, and the screens are in
`frontend/src/features/remittance`.

### 3.1 Extraction

- **Eligibility** (`RemittanceRules`, which is pure, and `ExtractionWork`). An invoice is examined
  when its remittance status is UNPROCESSED, WITH_OUTSTANDING_BALANCE or PARTIALLY_REMITTED.
  - Cancelled invoices (flag `CANCELLED`), direct payment invoices and return invoices (gross
    premium not positive) are skipped. Positions are read from the original invoice, where amounts
    due are booked plus the adjustment's ADJUSTED movements (`ADJ:<req>`).
  - Paid AR is the net applied PR amount of the ledger. Only applied and posted payments count
    (RMTID.006/028).
  - The amount to remit is paid AR less the DTIP already remitted.
  - Blocking reasons:
    - `ON_HOLD`: the HOLD flag (RMTID.020/031).
    - `PENDING_NEG_ADJ`: a pending negative adjustment.
    - `WRITTEN_OFF` (RMTID.022).
    - `CHECK_HOLDING` (RMTID.017/018): a payment is younger than `REMIT_CHECK_HOLD_DAYS` banking days,
      counted on the branch working-day calendar from the last applied value date.
    - `PAID_AR_OVER_DTIP` (RMTID.014): paid AR is above the DTIP balance. By default the invoice is
      excluded; with `REMIT_PAIDAR_OVER_DTIP_MODE` = `CAP`, only the DTIP balance is remitted (OQ19).
    - `OTHERS`: another team holds the invoice's lock.
- **Tags** (`rem_extraction_tag`, RMTID.003/024): every invoice examined in a run gets one of
  EXTRACTED, UNEXTRACTED_DUE (with its reasons), UNEXTRACTED_NOT_DUE or RETURNED.
- **Runs** (`rem_extraction_run`, `REX-<yyyy>`). A run is started in one of these ways:
  - scheduled, by job `REMITTANCE_EXTRACTION` (cron `brokerverse.jobs.remittance-extraction-cron`);
  - manually for an insurer, or for a single invoice (RMTID.004);
  - from the end-of-day queue: a search for an invoice can queue it for the evening run (RMTID.005,
    `rem_eod_request`, OQ18).
  Each insurer is processed in its own transaction. A failure raises alert `REMIT_EXTRACTION_FAILED`.
- **Batches** (RMTID.001/007/008/009). Lines are grouped per insurer and remittance type into batch
  `RMB-<insurer>-<yyyy>`, and each batch starts an `OPS_REMITTANCE` case at REVIEW_IN_PROCESS.
  - The remittance types are WITH_INCENTIVES, NORMAL_PHP, NORMAL_USD and SPECIAL. An invoice goes to
    WITH_INCENTIVES when an early-remittance rule (`rem_incentive_rule`, RMTID.023, OQ23) matches its
    insurer, product line and segment within the window from inception or booking.
  - The extract file is written through `FileDropPort`, named by `REMIT_FILE_PATTERN` (OQ17).
  - While in a batch, the invoice is locked by `REMITTANCE`.
- **Amounts** (RMTID.023, OPERATIONS_DESIGN §5 row 12):
  - Commission, VAT and WTAX are realised pro rata on the cumulative DTIP remitted. The remittance
    that clears the DTIP takes whatever balances remain.
  - Net due = paid AR + WTAX − commission − VAT.
  - Incentive = rate × the basic premium share, and its VAT follows the invoice's VAT/commission
    ratio. Payable = net due − incentive with VAT.

### 3.2 Batch processing (RMTID.002/009-011/019/024/027/029)

- **Batch page** (`/remittance/batches/:id`):
  - a read-only totals strip: Paid AR, commission, VAT, WTAX, DTIP, incentive, net due and payable;
  - the lines;
  - exclusions with a reason (`REMIT_EXCLUSION_REASON`), and restore. Both are allowed only while the
    batch is editable, and an exclusion unlocks the invoice (RMTID.002 addendum);
  - a preview of the schedule;
  - the workflow panel;
  - re-assignment to another processor.
- **Workflow `OPS_REMITTANCE`**:
  - submit (RMTID.019 guard: every included line still valid and the batch not empty);
  - hold (`HOLD_REASON`) and release;
  - return (`REMIT_RETURN_REASON`), which unlocks the lines and tags them RETURNED;
  - approve, with four-eyes (`REMIT_FOUR_EYES`), and send back. Submit and approve re-check every
    included line: still locked by remittance, and not cancelled, on hold, written off or flagged
    `PENDING_NEG_ADJ`;
  - after approval, the system actions dv_full, dv_partial and or_received.
- **Posting on approval** (`BatchPosting`, in the approval transaction):
  - event `OPS_REMITTANCE` per line, with source ref `RMB:<batch>:<invoice>`: Dr DTIP, Dr CWT /
    Cr commission receivable, Cr due for disbursement;
  - the ledger movement REMITTED on DTIP, commission, VAT and WTAX, and remittance status APPROVED;
  - for a With Incentives batch, event `OPS_REMIT_INCENTIVE` (`RMB:<batch>:INC`): Dr due for
    disbursement / Cr incentive income, Cr output VAT (§5 row 13);
  - a `DisbursementGateway` request of type `REMITTANCE` for the payable;
  - the commission OR (CSHID.007) and the incentive OR through `ReceiptIssuer`.
- **Documents** (RMTID.011). The remittance schedule (PDF and XLSX) and the payment request PDF come
  from doc templates `REMITTANCE_SCHEDULE` and `REMITTANCE_PAYMENT_REQUEST` and are stored on the
  batch (`rem_batch_document`).
  - *Send schedule* (MKTID.001) e-mails the schedule to the insurer as a password-protected Excel file, with the password in a separate e-mail, once per batch.
- **Disbursement feedback** (`DisbursementFeedback`, after commit) reacts to `DisbursementStatusChanged`:
  - DV_ASSIGNED makes each invoice FULLY_REMITTED or PARTIALLY_REMITTED, releases the lock and moves
    the batch on (RMTID.034/036);
  - RETURNED notifies the processors.
  - `NegativeAdjustmentPending` notifies the Remittance Team, with the batch the invoice is in
    (RMTID.035).

### 3.3 Insurer OR (RMTID.012/013/016)

- The insurer's populated schedule is uploaded as a CSV on `/remittance/insurer-or`. It runs through
  flow-in feed `INSURER_REMIT_OR` (`FlowInService`, one record per idempotency key).
- Each row sets the insurer OR number, date and amount on its batch line. The line is MATCHED when the
  OR amount equals the paid AR, and AMOUNT_MISMATCH otherwise.
- Duplicates and excluded lines are rejected per record.
- The batch reaches OR_RECEIVED when every included line has an OR.
- Exception report: `REM-OR-EXCEPTION`, reachable from the upload history.

### 3.4 Holds (MKTID.002-007, RMTID.021/031)

- Request `HLD-<yyyy>` (`rem_hold_request`, one live request per invoice), workflow `OPS_HOLD`:
  - draft, then submit: the invoice becomes REQUESTED_FOR_HOLD;
  - approve with four-eyes (`HOLD_APPROVE`): the HOLD flag is set and the status restored. Reject is
    the alternative;
  - extend, with extension approval;
  - request cancel, with cancel approval;
  - release;
  - assign to a processor (MKTID.004).
- Job `HOLD_EXPIRY` (cron `brokerverse.jobs.hold-expiry-cron`) runs daily:
  - it releases holds whose date has passed, so the invoice becomes eligible again (RMTID.021);
  - it notifies `HOLD_EXPIRING` for holds that reach their date the next day.
- Holds from Collection come in by upload through feed `COLLECTION_HOLD` (source COLLECTION_FEED).

### 3.5 Special remittance (MKTID.009, RMTID.030/033)

- Request `SPR-<yyyy>` (`rem_special_request`), with a condition from LOV `SPECIAL_REMIT_CONDITION`.
  It is validated on creation: the invoice must be paid, cleared and not on hold.
  - The claims condition is confirmed through `ClaimsFeed` feed `CLAIMS_SPECIAL_REMIT` when that port
    is connected. Without it, the request carries a note (OQ46).
- Workflow `OPS_SPECIAL_REMIT`:
  - validate;
  - approve, with four-eyes. Approval creates a SPECIAL batch at once, with no processor so that a
    different approver approves the batch;
  - or reject with a reason;
  - then pushed or returned, following the batch.
- Requests from Collection come in through feed `COLLECTION_SPECIAL_REMIT`.
- Status changes are notified to the requester (RMTID.033).

### 3.6 Search, tracking and reports

- **Search**:
  - `/remittance/dtip`: DTIP status per invoice with the last tag and its reasons. Search by invoice,
    batch, endorsement reference, policy or assured (RMTID.025).
  - `/remittance/batches`: the batch queues by stage (RMTID.027).
  - The Invoice 360 tab (`InvoiceRelatedItems`) lists an invoice's batches, holds and specials
    (RMTID.036).
- **Reports**, all through `ReportMetadata.operations` (RMTID.039):
  - `REM-TRACKER`
  - `REM-SPECIAL-REGISTER`
  - `REM-SCHEDULE-NORMAL`, `REM-SCHEDULE-SPECIAL`, `REM-SCHEDULE-INCENTIVE`
  - `REM-DTIP-SUMMARY`, `REM-DTIP-DETAIL`
  - `REM-REMITTED-BATCH`
  - `REM-PAIDAR-OVER-DTIP` (RMTID.015)
  - `REM-OR-EXCEPTION`
  - `REM-EXCLUDED`
  - `REM-HOLD`

  Layouts that the Annex does not detail are drafts (OQ42).
- **Operations home**: `RemittanceWorkCounts` implements `OpsWorkCountSource`, with batches in review
  and for approval, holds and specials for approval. `RemittanceApprovalSource` implements
  `PendingApprovalSource`.

### 3.7 API (`/api/v1/remittance`)

| Resource | Endpoints |
|---|---|
| Extraction | `GET/POST runs`, `GET runs/{id}`, `GET runs/{id}/tags`, `GET/POST eod-requests`, `GET accounts`, `GET dtip` |
| Incentive rules | `GET/POST incentive-rules`, `PUT incentive-rules/{id}` |
| Insurer OR | `POST insurer-or/upload`, `GET insurer-or/runs`, `GET insurer-or/runs/{id}` |
| Batches | `GET batches`, `GET batches/{id}`, `GET batches/{id}/preview`, `POST batches/{id}/exclude`, `restore`, `submit`, `approve`, `return`, `assign`, `GET batches/{id}/documents/{kind}`, `POST batches/{id}/send-schedule` |
| Holds | `GET/POST holds`, `GET/PUT holds/{id}`, `POST holds/{id}/submit`, `cancel`, `decision`, `extend`, `extension-decision`, `request-cancel`, `cancel-decision`, `release`, `assign`, `POST holds/upload` |
| Special | `GET/POST special`, `GET special/{id}`, `POST special/{id}/approve`, `reject`, `POST special/upload` |

### 3.8 Screens

| Route | Screen | Permission |
|---|---|---|
| `/remittance` | Remittance home | `REMIT_PROCESS` |
| `/remittance/extraction` | Extraction runs, manual and single-invoice extraction, tags | `REMIT_EXTRACT` |
| `/remittance/batches` | Batch queues with bulk submit / approve | `REMIT_PROCESS` |
| `/remittance/batches/:id` | Batch (hidden): totals, lines, exclusions, workflow, documents | `REMIT_PROCESS` |
| `/remittance/insurer-or` | Insurer OR upload and history | `REMIT_OR_UPLOAD` |
| `/remittance/holds`, `/remittance/holds/:id` | Hold requests | `HOLD_REQUEST` |
| `/remittance/special`, `/remittance/special/:id` | Special remittance requests | `SPECIAL_REMIT_REQUEST` |
| `/remittance/dtip` | DTIP status search | `REMIT_PROCESS` |
| `/remittance/incentive-rules` | Early-remittance incentive rules | `REMIT_APPROVE` |

### 3.9 Demo

- V992 seeds:
  - the demo accounting rules for `OPS_REMITTANCE`: 2210 / 1602 against 1220 / 2211;
  - the rules for `OPS_REMIT_INCENTIVE`: 2211 against 4130 / 2504;
  - an incentive rule: INS-MGIC, PROPERTY, CBG, 2%, 30 days from inception.
- `RemittanceDemoData` (profile `demo`) puts a hold on invoice ARN-2026-940004 and runs a manual
  extraction, so the batches and tags have content.
- Demo users are `remit` and `remittl`.

### 3.10 Parked (seam only)

| Item | Question | Seam |
|---|---|---|
| Remittance type rules, batch schedule, file naming, shared drive | OQ17 | type from currency and incentive rule; cron property; `REMIT_FILE_PATTERN`; `FileDropPort` |
| End-of-day extraction trigger | OQ18 | `rem_eod_request` queue processed by the evening run |
| Paid AR above DTIP | OQ19 | `REMIT_PAIDAR_OVER_DTIP_MODE` EXCLUDE (default) / CAP |
| Holding period start and "cleared" source | OQ20 | banking days from the last applied value date |
| Insurer OR layout, tolerance, insurer channels | OQ22 | CSV upload, exact match, e-mail only |
| Incentive rates and window | OQ23 | `rem_incentive_rule` maintained on screen |
| Hold roles, maximum and extension rules | OQ24 | permissions `HOLD_REQUEST` / `HOLD_APPROVE`, no maximum |
| Report layouts, Mall Assurance columns of the Normal schedule | OQ42 | draft layouts |
| Marketing and Claims feeds | OQ45, OQ46 | `COLLECTION_HOLD` / `COLLECTION_SPECIAL_REMIT` uploads, `ClaimsFeed` `CLAIMS_SPECIAL_REMIT` |
| Disbursement system, re-sending a returned payment request | OQ02 | `DisbursementGateway` queue; returned requests are notified only |
| GL accounts | OQ07 | demo rules only |
| DTIP open-item settlement in the subledger | OQ07 | the ledger REMITTED movement; GL through the event |

### 3.11 Fit/gap status

| BR ID | Status | Where |
|---|---|---|
| RMTID.001 / 003 / 007 / 008 | Built (schedule and naming OQ17) | `REMITTANCE_EXTRACTION`, batches per insurer and type, extract file |
| RMTID.002 | Built | exclusion and restore with reason, read-only totals |
| RMTID.004 / 005 | Built (EOD meaning OQ18) | manual single-invoice extraction, end-of-day queue |
| RMTID.006 / 028 | Built | paid AR from applied and posted payments |
| RMTID.009 / 010 / 019 / 029 | Built | `OPS_REMITTANCE`, submit guard, hold / return, re-assign |
| RMTID.011 / MKTID.001 | Built | schedule and payment request documents, send schedule |
| RMTID.012 / 013 / 016 | Built (layout OQ22) | `INSURER_REMIT_OR`, `REM-OR-EXCEPTION` |
| RMTID.014 / 015 | Built (mode OQ19) | over-DTIP rule, `REM-PAIDAR-OVER-DTIP` |
| RMTID.017 / 018 | Built (start and clearing OQ20) | check holding period on banking days |
| RMTID.020 / 031 / 035 | Built | hold and negative adjustment exclusions, notification |
| RMTID.021 / MKTID.002-007 | Built (roles OQ24) | `OPS_HOLD`, `HOLD_EXPIRY`, `COLLECTION_HOLD` |
| RMTID.022 | Built | written-off exclusion |
| RMTID.023 | Built (rates OQ23) | incentive rules, `OPS_REMIT_INCENTIVE`, incentive OR |
| RMTID.024 / 025 / 027 | Built | tags, DTIP search, batch queues |
| RMTID.030 / 033 / MKTID.009 | Built (Claims OQ46) | `OPS_SPECIAL_REMIT`, special batches, `COLLECTION_SPECIAL_REMIT` |
| RMTID.034 / 036 | Built (Disbursement OQ02) | Disbursement feedback, tracker, Invoice 360 tab |
| RMTID.039 | Built (layouts OQ42) | 12 `REM-*` reports |

## 4. Adjustment (`adjustment`)

Endorsement and cancellation requests on booked invoices of the ledger (ADJID.001-026/028,
MKTID.008). Package `com.iortatechnxt.brokerverse.adjustment`, Flyway `V780__adjustment.sql`,
demo `V994__demo_adjustment.sql` and `adjustment.demo.AdjustmentDemoData`, screens in
`frontend/src/features/adjustment`.

### 4.1 Requests

- **One request per invoice** (`adj_request`, number `ENR-<yyyy>`), raised singly, for several
  invoices at once (the wizard raises one request per invoice) or by upload (`ADJ_BATCH`). The ARN,
  invoice, policy, client, insurer, segment and AO are copied from the ledger (ADJID.020).
- **Class** comes from the parent of the `ENDORSEMENT_TYPE` value (`FIN_` financial, `NF_`
  non-financial, `INT_` internal). A financial request needs an Annex V request type
  (`ENDORSEMENT_REQUEST_TYPE`); a non-financial one carries no request type, sum insured or amount.
- **Computation** follows the request type:

  | Request type | Computation | Inputs |
  |---|---|---|
  | `FLAT_CANCELLATION`, `FLAT_CANCELLATION_RETAIN_DST`, `PARTIAL_CANCELLATION` | cancellation | reason (`CANCELLATION_REASON`), basis pro-rata / short-period |
  | `TSI_CHANGE` | sum insured | TSI change (signed), rate (blank = product rate), basis |
  | `WRITE_OFF` | write-off of the outstanding premium receivable | - |
  | any other (rate, taxes, cover, extension, commission, cancellation reversal) | amounts | premium component changes and / or commission (derived from the invoice rate when blank) |
  | none (non-financial, internal without type) | no financial effect | - |

- **Checks** (`RequestRules`, `RequestChecks`, `RequestConflicts`): effective date within the
  invoice's cover; valid new period for `NF_PERIOD_CHANGE` (same term) and `NF_PERIOD_EXTENSION`;
  the invoice is not a return invoice, not cancelled or written off and not locked by another
  module (`INVOICE_LOCKED`, e.g. in a remittance batch); no cancellation together with another
  open financial request on the invoice (`ADJ_INCOMPATIBLE_REQUEST`, e.g. TSI change with a partial
  cancellation).
- **Duplicates** (ADJID.023): same invoice, request type, reason and endorsement reference, not
  cancelled. Raising one is refused (`ADJ_DUPLICATE_REQUEST`) unless a justification is given; the
  justification is kept on the request and in the audit trail.
- **Over-adjustment** (ADJID.028): the ledger's cumulative adjustments of the original invoice
  plus this request, against the parameter `ADJ_BASELINE_PERCENT` (100) of the original premium, or
  below zero. Submitting such a request needs a justification and raises `ADJ_OVER_BASELINE`.
- **Quotation required** (ADJID.008): a TSI increase that takes the account above the package
  limit (`cat_product.max_sum_insured`) opens a hand-off `QUOTATION_REQUIRED` for `QUOTE_MAINTAIN`
  at submission; validation waits until the quotation number is linked on the request (link by
  reference only).

### 4.2 Workflow `OPS_ENDORSEMENT` (V780)

```
DRAFT(ADJ_REQUEST) --submit--> FOR_VALIDATION(ADJ_PROCESS) --validate--> FOR_APPROVAL(ADJ_APPROVE) --approve--> FOR_POSTING(ADJ_POST)
FOR_VALIDATION --validate_for_posting--> FOR_POSTING            (no financial effect; FIN_EXTENSION always needs approval)
FOR_POSTING --post--> POSTED ; FOR_POSTING --post_pending--> AWAITING_REAPPLICATION(ADJ_POST) --reapply--> POSTED
FOR_VALIDATION/FOR_APPROVAL/FOR_POSTING --return(ADJ_RETURN_REASON)--> RETURNED(ADJ_REQUEST) --resubmit--> FOR_VALIDATION
DRAFT/RETURNED --cancel--> CANCELLED
```

- Submit, resubmit, validate, approve, post and re-apply are business actions of the module;
  return and cancel are generic (workflow panel). `RequestStageListener` mirrors the stage, keeps
  the return reason and comment, and releases the invoice on a cancellation.
- Approval is four eyes: refused to the requester, the submitter and the validator
  (`ADJ_FOUR_EYES`).
- Notifications (`ADJ_REQUEST_STATUS`): the team of the next stage, and the requester on return,
  posting and cancellation.
- Aging (ADJID.021) runs from submission (or creation) to completion, in Philippine days.

### 4.3 Hold on the invoice

- The invoice is **locked** (owner `ADJUSTMENT`) when a request is raised and unlocked when its
  last open request is posted or cancelled. Remittance cannot extract a locked invoice; cashiering
  can still apply payments.
- A request that reduces the invoice raises **`PENDING_NEG_ADJ`** and publishes
  `NegativeAdjustmentPending(pending = true)` when submitted; the flag is cleared (event with
  `pending = false`) when the last reducing request is posted or cancelled, **except** while the
  payments wait for re-application or when an AR Insurer was set up (the invoice was remitted):
  the flag then stays for remittance to offset or exclude the invoice.

### 4.4 Recompute and posting

- **Recompute** (`RecomputeService`, `PremiumDeltas`): cancellations use booking's own preview
  (`EndorsementPostingService.preview`), so the preview equals the posting; a TSI change is rated
  once per insurer share with the catalog calculator in endorsement mode (`RatingService` /
  `PremiumCalculator`, `endorsement = true`, remaining term pro-rata or short-period, each insurer
  at its commission rate, the lead with its branch LGT); amounts are split by the insurer shares.
  The before / after per component and the change per insurer are stored on the request
  (`adj_request_component`, `adj_request_share`) at every save, submission, validation and posting.
- **Posting** (`AdjustmentPostingService`, one transaction per request; `PostingBatchService`
  posts a selection as batch `VB-<yyyy>`, a failure does not stop the others):
  1. financial endorsements and cancellations through **`EndorsementPostingService.post`** with
     the source reference `ADJ:<request>`: booking books the endorsement (`EN-<yyyy>`) or return
     invoice, its journal and open items, issues or credits the service invoice; non-financial
     endorsements are recorded by booking without GL; internal requests without financial effect
     post nothing;
  2. a **decrease or cancellation** posts an `ADJUSTED` movement with the return amounts
     (premium by component, DTIP, commission, VAT, withholding tax) on the **original invoice**
     (`ADJ:<request>`). The return invoice reaches the ledger through the feed and is **offset**
     (`ADJ:<request>:OFFSET`, `ReturnInvoiceOffset`) so the ledger counts the return once, on the
     original. A positive endorsement is a new collectible invoice; the original is unchanged.
     Cancellations set the `CANCELLED` flag;
  3. **after remittance** (row 18): when the DTIP of the original becomes negative and was
     remitted, the amount (at most the remitted DTIP) is set up as **AR Insurer**: event
     `OPS_AR_INSURER_SETUP` per insurer share (`ADJ:<request>:ARI:<insurer>`) and an `ADJUSTED`
     DTIP movement reclassifying the negative DTIP (`ADJ:<request>:ARI`);
  4. **paid invoice** (row 17): the lock is released and the payments are re-applied through the
     cashiering port **`PaymentReapplier.reapply`** (`ADJ:<request>`); the excess and its unapplied
     item are recorded on the request. While cashiering is not installed the default port refuses
     (`PAYMENT_REAPPLIER_UNAVAILABLE`): the request moves to **AWAITING_REAPPLICATION** ("Payments
     to Re-apply") and *Re-apply Payments* retries it later;
  5. a **commission change alone** posts `OPS_ADJ_COMMISSION` per insurer share, an `ADJUSTED`
     commission movement, and issues a service invoice (`INSURER_COMMISSION_ENDT`) or credits the
     insurer's service invoice through booking's **`ServiceInvoiceService`** (ADJID.014);
  6. a **write-off** request clears the premium receivable like the minimal balance file.

### 4.5 Accounting events (V780; demo rules V994, OQ07)

| Event | Source reference | Demo entry |
|---|---|---|
| booking `BROKER_BOOKING` (rows 16, 19) | booking's own, request `ADJ:<request>` | reversal or addition of the booking entry |
| `OPS_AR_INSURER_SETUP` (row 18) | `ADJ:<request>:ARI:<insurer>` | Dr 1225 AR Insurer / Cr 2210 DTIP (insurer) |
| `OPS_WRITE_OFF` (row 21) | `WO:<invoice>` | debit balance: Dr 6510 / Cr 1210.x (client); credit balance: Dr 1210.x / Cr 4190 Other income |
| `OPS_ADJ_COMMISSION` (ADJID.014) | `ADJ:<request>:COM:<insurer>` | Dr 1220 / Cr 2220, Cr 2221 (negative amounts reverse) |

Every event carries the invoice's product line and cost center and is priced at the BOOK rate.

### 4.6 Minimal balance file (ADJID.026)

Bulk handler `MINIMAL_BALANCE_FILE` (`ADJ_POST`, duplicate files refused): columns *Invoice No*
and *Balance*. A row is processed when the invoice exists, its premium receivable balance equals
the file and lies within `MIN_BALANCE_FILE_RANGE` (10.00-100.00), it was not written off and no
other module locks it. A debit balance is written off, a credit balance credited (outcome
categories `WRITE_OFF` / `CREDIT`), with a `WRITE_OFF` movement `WO:<invoice>`, the `WRITTEN_OFF`
flag and an `adj_min_balance_item` (once per invoice).

### 4.7 API (`/api/v1/adjustment`)

| Endpoint | Permission |
|---|---|
| `GET /requests?companyId=&stage=&q=&page=&size=`, `GET /requests/counts`, `GET /requests/{id}`, `GET /requests/{id}/recompute`, `GET /requests/{id}/journal`, `GET /invoices/{no}/requests` | any `ADJ_*` or `OPS_VIEW` |
| `POST /requests/preview`, `POST /requests`, `PUT /requests/{id}`, `POST /requests/{id}/submit`, `/resubmit`, `/quotation` | `ADJ_REQUEST` or `ADJ_PROCESS` |
| `POST /requests/{id}/validate` | `ADJ_PROCESS` |
| `POST /requests/{id}/approve` | `ADJ_APPROVE` |
| `POST /requests/{id}/post` (batch of one), `/reapply`, `POST /batches` | `ADJ_POST` |
| `POST /batches/return` | `ADJ_PROCESS`, `ADJ_APPROVE` or `ADJ_POST` (the workflow checks the stage) |
| `GET /requests/{id}/endorsement-slip`, `/validation-slip` (PDF) | any `ADJ_*` |
| `GET /batches`, `GET /batches/{no}`, `GET /write-offs` | any `ADJ_*` or `OPS_VIEW` |

### 4.8 Screens (group *Client & Policy*, section *Adjustment*)

| Route | Screen | Permission |
|---|---|---|
| `/adjustment` | Adjustment Workbench: tabs per stage with counts, search, aging, flags (`?stage=` from the Operations home tiles) | `ADJ_PROCESS` (+ `ADJ_REQUEST`, `ADJ_APPROVE`, `ADJ_POST`) |
| `/adjustment/new` | New request wizard: invoices (multi), request (form adapts to the type), recompute before / after and per insurer with the service invoice, payment and remittance effects, duplicate and baseline justifications; save or submit | `ADJ_REQUEST` (+ `ADJ_PROCESS`) |
| `/adjustment/requests/:id` (hidden) | Request page: summary, workflow panel with the business actions, tabs Details / Recompute / Accounting / Documents / History, endorsement slip and validation slip | any `ADJ_*` or `OPS_VIEW` |
| `/adjustment/requests/:id/edit` (hidden) | Change of a draft or returned request | `ADJ_REQUEST` (+ `ADJ_PROCESS`) |
| `/adjustment/batches` | Posting Batches: ready for posting (return selected, post selected) and posted batches with their outcome | `ADJ_POST` |
| `/adjustment/upload` | Batch Request Upload (`ADJ_BATCH`) | `ADJ_POST` |
| `/adjustment/minimal-balance` | Minimal Balance File (`MINIMAL_BALANCE_FILE`) and the balances processed | `ADJ_POST` |

Supporting documents (ADJID.025) are attachments of entity type `EndorsementRequest`; the document
list `ENDORSEMENT_DOC_TYPE` is still open (OQ32).

### 4.9 Documents, reports and job

- **Endorsement slip** (ADJID.015, MKTID.008): PDF with the Annex V fields (date, insurer and
  co-insurers, request and invoice numbers, request type, reason, effective date, instructions,
  assured, risk code and description, policy, period, AO, segment, sum insured and change, rate,
  payment and remittance status, approver), numbered once `ES-<yyyy>`, template
  `ENDORSEMENT_SLIP`; not for internal adjustments.
- **Validation slip** (ADJID.018): PDF of a validated request with the validation, before / after,
  insurer breakdown and GL entries (template `VALIDATION_SLIP`).
- **Reports** (category Operations, `OPS_REPORT_VIEW` / `OPS_REPORT_EXPORT`, archived):

  | Code | Report | BRD |
  |---|---|---|
  | `ADJ-DAILY` | Adjustment and Daily Endorsement Report: requests raised or posted in the period, by type and user | ADJID.016 |
  | `ADJ-VALIDATION-LIST` | Validation List: posted requests with one row per GL line and the BRD fields | ADJID.017 |
  | `ADJ-REGISTER` | Adjustment Report by account, segment, AO and risk type | ADJID.019 |
  | `ADJ-AGING` | Transaction aging with buckets | ADJID.021 |
  | `ADJ-MINBAL-FILE` | Minimal balance write-off summary per file | ADJID.026 |

  The layouts not given by the BRD are drafts to confirm (OQ42).
- **Job `ADJ_DAILY_REPORT`** (`brokerverse.jobs.adj-daily-report-cron`, default `0 0 10 * * *`
  UTC = 18:00 PHT): exports `ADJ-DAILY` of the business date as Excel for every company (archived)
  and notifies the holders of `ADJ_APPROVE`.

### 4.10 Contracts for the other modules

| Contract | Kind | Use |
|---|---|---|
| `OpsLedgerEvents.NegativeAdjustmentPending` with the `PENDING_NEG_ADJ` flag | event published | remittance excludes the invoice and notifies (RMTID.020/035) |
| `PaymentReapplier.reapply(ReapplyRequest(invoiceNo, "ADJUSTMENT", "ADJ:<request>", date, reason))` | port called | cashiering reverses the applications above the new premium and returns the excess. The invoice is unlocked before the call, and its PR balance is already reduced (negative when overpaid). Return `ReapplyResult.none` when nothing is applied; throw `PAYMENT_REAPPLIER_UNAVAILABLE` only when it cannot work |
| `ADJUSTED` movements `ADJ:<request>` on the original invoice, `ADJ:<request>:OFFSET` on the return invoice, `ADJ:<request>:ARI` for the AR Insurer | ledger convention | remittance and cashiering read balances from the original invoice; a return invoice of an adjustment always nets to zero |
| `InvoiceRelatedItems` (section `ADJUSTMENTS`) | SPI implemented | invoice 360 tab: requests raised on the invoice or that booked it |
| `OpsWorkCountSource` (section `ADJUSTMENT`) | SPI implemented | Operations home tiles: for validation, for approval, for posting, returned, payments to re-apply |

### 4.11 Demo

`V994` adds GL 4190 and the demo rules of the three events. At start-up (demo profile, after the
booking demo and the ledger replay) `AdjustmentDemoData` raises, as `mktcoll`, a change of the
assured's information on the invoice of `ARN-2026-940003` (waiting for validation) and a flat
cancellation of `ARN-2026-940004`, validated by `adjust` and approved by `adjtl`, ready for the
posting batch.

### 4.12 Parked (seam only)

| Item | Question | Seam |
|---|---|---|
| GL accounts of the Adjustment events and of commission realised at booking | OQ07 | event types with demo rules; production configures the Accounting Rules |
| Payment re-application | cashiering (O1-A) | `PaymentReapplier` port; requests wait in AWAITING_REAPPLICATION until it is available |
| Over-adjustment baseline value | OQ37 | parameter `ADJ_BASELINE_PERCENT` (100) |
| Minimal balance range, targets and approval | OQ11 | parameter `MIN_BALANCE_FILE_RANGE`, event `OPS_WRITE_OFF` |
| Refund basis of partial cancellations and decreases | OQ36 | pro-rata or short-period chosen per request |
| Credit memo or negative service invoice on a commission decrease | OQ34 | credit of the insurer's service invoice through `ServiceInvoiceService.credit` |
| Endorsement request number format and document list | OQ32 | `ENR-<yyyy>`, `ES-<yyyy>`; `ENDORSEMENT_DOC_TYPE` holds only "Others" |
| "Quotation required" link to a quotation | ADJID.008 | hand-off `QUOTATION_REQUIRED` to Marketing and the quotation number linked by reference; `QuotationService` has no endorsement quotation call |
| Update of the account's risk or assured data by a non-financial endorsement | OQ32 | recorded by booking as a non-financial endorsement with its description; the account is not changed |
| Package TSI limits and co-insurance model | OQ33 | catalog `max_sum_insured` and the invoice's insurer shares |
| Report layouts | OQ42 | draft layouts |

### 4.13 Fit/gap status

| BR ID | Status | Where |
|---|---|---|
| ADJID.001 | Built | requests on booked invoices, single / multiple / upload, lock and remittance-queue guard, incompatible types |
| ADJID.002 / 004 | Built | `ENDORSEMENT_TYPE` classes, form by request type, period checks |
| ADJID.003 | Built (account data update parked, OQ32) | non-financial posting recorded by booking |
| ADJID.005 / 007 | Built | return with reason from validation, approval or posting batch; requester notified |
| ADJID.006 | Built | posting batches `VB-<yyyy>`, `ADJ_BATCH` upload |
| ADJID.008 | Built (quotation link by reference) | recompute per insurer, package limit, hand-off |
| ADJID.009 / 012 / 013 | Built (seam until cashiering) | `PaymentReapplier`, excess recorded, AWAITING_REAPPLICATION |
| ADJID.010 | Built | approval stage, `FIN_EXTENSION` always approved |
| ADJID.011 | Built (GL rules OQ07) | booking `EndorsementPostingService.post`, own events |
| ADJID.014 | Built | recompute before / after and per insurer; service invoice issue / credit |
| ADJID.015 / MKTID.008 | Built | endorsement slip `ES-<yyyy>` |
| ADJID.016 | Built | `ADJ-DAILY` and job `ADJ_DAILY_REPORT` |
| ADJID.017 | Built | `ADJ-VALIDATION-LIST` |
| ADJID.018 | Built | validation slip |
| ADJID.019 | Built | `ADJ-REGISTER` |
| ADJID.020 | Built | ARN and invoice on every request, invoice 360 tab |
| ADJID.021 | Built | aging on lists and `ADJ-AGING` |
| ADJID.022 | Built | before / after, trail, workflow history, audit |
| ADJID.023 | Built | duplicate check with justification |
| ADJID.024 | Built | invoice search and invoice 360 (opsledger) with the requests of the invoice |
| ADJID.025 | Built (document list OQ32) | attachments of the request |
| ADJID.026 | Built (range and targets OQ11) | `MINIMAL_BALANCE_FILE`, `ADJ-MINBAL-FILE` |
| ADJID.028 | Built (baseline OQ37) | cumulative control with justification, `ADJ_OVER_BASELINE` |
