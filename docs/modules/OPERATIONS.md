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


## 2. Cashiering (`cashiering`)

Package `com.iortatechnxt.brokerverse.cashiering`, Flyway `V763`-`V764` (demo `V991`), screens in
`frontend/src/features/cashiering`. Cashiering receives premium and non-premium payments, issues
acknowledgement receipts (AR) and Head Office official receipts (OR), applies payments to the
invoice ledger component by component, keeps unapplied payments with their dispositions, and runs
the BIR 2307 reversal flow with Marketing and Disbursement (CSHID.001-027, MKTID.010/013, DBMID.001).

### 2.1 Receipts and series

- **Series** (`csh_receipt_series`, maker-checker): one per kind (AR / OR) and branch, with prefix,
  range, BIR ATP number and a warning level. ORs come only from Head Office series (CSHID.006).
  Every receipt takes the next number under a row lock. A depleted series refuses the receipt
  (`RECEIPT_SERIES_DEPLETED`, CSHID.015). At the warning level the `RECEIPT_SERIES_LOW` alert is
  raised.
- **AR** (`csh_receipt`, kind AR): the class comes from LOV `AR_CLASS` (premium or non-premium).
  The tender is the mode, check and source. The amount is converted at the BOOK rate. Posting:
  `OPS_AR_RECEIPT` (Dr bank or cash on hand / Cr unapplied collections), or `OPS_AR_INSURANCE_RECEIPT`
  for non-premium insurer receipts (AR Insurance, CSHID.021).
- **OR** (kind OR): the type comes from LOV `OR_TYPE` (service fee, profit share, commission,
  incentive, others), with lines of gross, VAT and withholding tax. Posting: `OPS_OR_ISSUE`.
  Commission ORs: the `COMMISSION_PAYMENT` upload stages the insurer payments, and *Issue ORs*
  issues one OR per payment (CSHID.007).
- **Cancellation and reinstatement** (`csh_receipt_action`, workflow `OPS_RECEIPT_ACTION`):
  - A request gets its own number (`CAN-<yyyy>` / `RIN-<yyyy>`) and a reason from LOV
    `RECEIPT_CANCEL_REASON` / `REINSTATEMENT_REASON`. "Others" needs the text.
  - A reinstatement also carries the encoded fields of its reason group (CSHID.003-005).
  - The checker approves and the posting follows (CSHID.012/013). The requester cannot approve.
  - A cancellation reverses every active application, reverses `OPS_AR_RECEIPT` and closes the
    unapplied balance of the receipt.
  - A reinstatement restores the receipt (status REINSTATED, `OPS_RECEIPT_REINSTATE`) for the full
    or partial amount. It applies the amount to the encoded invoice, and the rest stays unapplied.
    When the cancelled receipt was a bounced check, the encoded fields are not required.
- **Search** (CSHID.010): ten criteria (receipt no., client, invoice, policy, payor, assured,
  insurer, amount, dates, kind / status), paged. Each search is logged in `csh_search_log` (OQ47).
- **Printing**:
  - `ReceiptDocument` renders the AR / OR PDF and the certificate of payment (draft layout, OQ42).
  - *Batch Print* (`csh_print_batch`) prints a selection into one merged PDF. It records each line
    and retries failures (CSHID.019).
- **Audit** (CSHID.011): every action records the receipt number and the user in the audit trail.

### 2.2 Payment intake, matching and application

- **Intake** (`csh_payment`): over the counter (*Receive Payment*), the payment files, PDC
  maturity, check pick-up and the 2307 cash path. Each payment issues its AR and is matched at once
  (CSHID.008/020).
- **Matching** (`PaymentMatcher`) on ARN, invoice, policy or PN number. The outcome is one of:
  - booked invoice with outstanding PR (applied, oldest first);
  - cancelled invoice (unapplied, origin CANCELLED_REFERENCE);
  - account not booked yet (pre-booked queue);
  - no match (unapplied, NO_MATCH).
- **Application** (`ApplicationService`, `csh_application`): the pure `ApplicationPlanner`
  allocates by `LedgerComponent.applicationHierarchy()` (DST, VAT / premium tax, LGT, FST, other,
  basic), never DTIP (CSHID.022). Each application:
  - posts `OPS_PAYMENT_APPLY` (Dr unapplied collections / Cr PR by component, with the commission
    realised on collection per `OPS_COMMISSION_REALIZATION`);
  - posts the ledger movement `APPLIED` with reference `APP:<id>`.
- **2% CWT clients** are applied up to 98% of the premium (parameter `CWT_APPLICATION_PERCENT`). The 2% waits
  for the BIR 2307, and anything above it stays unapplied.
- **Excess** becomes an unapplied item (origin EXCESS) for disposition.
- **Preview** (`POST /payments/preview`): what a payment would match and apply, the 2% withheld, the
  excess and the BOOK rate. Nothing is saved.
- **Payment files** (`BulkImportHandler`, permission `CASH_UPLOAD`, duplicate files blocked):
  - handlers `PAY_BILLS`, `PAY_TRADE`, `PAY_CLPC`, `PAY_DIRECT_CREDIT` and `PAY_PDC`;
  - layouts in `csh_payment_file_layout`: AUTO (Excel / CSV / detected TXT), DELIMITED, or
    FIXED_WIDTH (`Header:start:length;...`);
  - they are changed on *Cashiering Setup* until BDOI confirms the bank layouts (OQ03/OQ04);
  - the run summary counts applied, unapplied, pre-booked, excess and failed rows.
- **Pre-booked** (`csh_prebooked`, OQ12): an account matched before it is booked. The AR is issued,
  and the money waits as an unapplied item (origin PREBOOKED). The item is applied by:
  - the `OpsInvoiceBooked` ledger event, or
  - the `PREBOOKED_REMATCH` job.

  Ageing items raise `PREBOOKED_AGEING`. *Release* moves an item to the unapplied workbench.
- **Automatch** (`PAYMENT_AUTOMATCH`): unapplied NO_MATCH / PREBOOKED items are matched again with
  their references, and are applied when the invoice is booked now.
- **PDC warehouse** (`csh_pdc_item`, `PDCW-<yyyy>`):
  - a check is warehoused by screen or by the `PAY_PDC` upload;
  - `PDC_MATURITY` turns a matured check into a payment with its AR;
  - before maturity a check can be returned, replaced or pulled out.
- **Check pick-up** (`csh_pickup_request`, CSHID.009): requests come from `CollectionFeed`
  (`PickupFlowInHandler`) or are entered by hand. *Print ARs* issues and prints the ARs of the
  selected checks in one batch.

### 2.3 Unapplied payments and dispositions (CSHID.024/025)

- `csh_unapplied` holds every unapplied balance with its origin: no match, excess, cancelled
  reference, adjustment, cancellation, DP reinstatement, remittance return, re-application,
  pre-booked or other.
- The stage mirrors workflow `OPS_DISPOSITION`. The tabs are Unapplied / Monitoring / For
  Approval / For Reversal / Done.
- Disposition types are in `csh_disposition_type_rule`, each with an action and whether it needs
  approval (OQ15):

  | Action | Effect |
  |---|---|
  | APPLY / DST_APPLY | apply to another invoice or its DST only |
  | REFUND | `DisbursementGateway` request, then `OPS_UNAPPLIED_REFUND` when paid |
  | RECLASS / TRANSFER | `OPS_UNAPPLIED_RECLASS` to another client or unit (whole balance) |
  | MANUAL | other |

- Four eyes: the approver is never the requester. Submit and approve also run in bulk.
- A completed disposition can be marked for reversal and reversed on approval. A refund is
  reversed only after Disbursement returned it.
- A balance left after a partial disposition goes back to the Unapplied tab.

### 2.4 Minimal balance (CSHID.016, OQ11)

The `MINIMAL_BALANCE_SWEEP` job, or *Run Sweep Now*, works from the rules in
`csh_minimal_balance_rule`:

- **Premium balances** up to PHP 10 are reversed (`OPS_MINIMAL_BALANCE_REVERSAL`, ledger
  `MIN_BAL`). A balance is skipped when:
  - it equals the client's 2% CWT, the DST or the whole premium;
  - the invoice is already written off (`WRITTEN_OFF`, e.g. by Adjustment's `MINIMAL_BALANCE_FILE`
    for PHP 10-100).
- **Excess and unapplied payments** up to PHP 10 go to AP overages (`OPS_EXCESS_TO_OVERAGES`).
- Every sweep is logged once in `csh_minimal_balance`.

### 2.5 BIR 2307 (CSHID.026/027, MKTID.010/013, DBMID.001)

`csh_cwt_tag` (workflow `OPS_CWT_2307`) and `csh_cwt_batch`:

1. Marketing tags the certificate (or the cash path) of the invoice, by screen or with the
   `CWT_TAGS` upload (`CWT_TAG`).
2. Cashiering receives the tag and ticks the CWT-copy checklist.
3. Cashiering validates a selection per insurer into a report batch. This posts `OPS_CWT_RECLASS`
   (Dr PR2307 / Cr PR by component).
4. The batch is routed to Disbursement (`DisbursementGateway`, type CWT2307).
5. When Disbursement pays it, the batch is released to the insurer. This posts
   `OPS_CWT_DTIP_OFFSET` (Dr DTIP / Cr PR2307).

On the cash path an AR is issued and applied to the withheld 2% instead (*Settle in Cash*).

### 2.6 Accounting events (V764; demo rules V991, OQ07)

All 11 events use journal type RECEIPT at the BOOK rate:

- `OPS_AR_RECEIPT`, `OPS_AR_INSURANCE_RECEIPT`, `OPS_OR_ISSUE`;
- `OPS_PAYMENT_APPLY`, `OPS_RECEIPT_REINSTATE`;
- `OPS_CWT_RECLASS`, `OPS_CWT_DTIP_OFFSET`;
- `OPS_EXCESS_TO_OVERAGES`, `OPS_MINIMAL_BALANCE_REVERSAL`;
- `OPS_UNAPPLIED_REFUND`, `OPS_UNAPPLIED_RECLASS`.

The `@BANK` role account comes from the system parameters `CASH_BANK_ACCOUNT` (check and electronic
modes) and `CASH_ON_HAND_ACCOUNT` (cash). They are empty in production until BDOI gives the GL
(OQ07). The demo sets them to 1111 / 1101 and adds GL 4190.

### 2.7 Jobs

| Job | Cron property | Default |
|---|---|---|
| `PREBOOKED_REMATCH` | `brokerverse.jobs.prebooked-rematch-cron` | every 2 hours |
| `PAYMENT_AUTOMATCH` | `brokerverse.jobs.payment-automatch-cron` | hourly at :30 |
| `PDC_MATURITY` | `brokerverse.jobs.pdc-maturity-cron` | 00:30 |
| `MINIMAL_BALANCE_SWEEP` | `brokerverse.jobs.minimal-balance-sweep-cron` | 20:00 |

Each item runs in its own transaction; a failure is logged and the run goes on.

### 2.8 API (`/api/v1/cashiering`)

| Area | Endpoints |
|---|---|
| Receipts | `GET /receipts` (search), `GET /receipts/{id}`, `POST /receipts/ar`, `POST /receipts/or`, `POST /receipts/{id}/cancel`, `POST /receipts/{id}/reinstate`, `GET /receipts/{id}/pdf`, `POST /receipts/{id}/certificate-of-payment` |
| Receipt actions | `GET /receipt-actions`, `POST /receipt-actions/{id}/approve`, `POST /receipt-actions/{id}/resubmit` |
| Payments | `POST /payments/preview`, `POST /payments`, `GET /payments`, `GET /prebooked`, `POST /prebooked/{id}/rematch`, `POST /prebooked/{id}/release`, `POST /matching/run` |
| Unapplied | `GET /unapplied`, `GET /unapplied/{id}`, `GET /unapplied/{id}/dispositions`, `GET /disposition-types`, `POST` / `PUT /unapplied/{id}/disposition`, `POST /unapplied/{id}/submit`, `/approve`, `/withdraw`, `/reversal`, `/reversal/approve`, `POST /unapplied/bulk/submit`, `/bulk/approve` |
| Checks | `GET` / `POST /pdc`, `POST /pdc/{id}/release`, `POST /pdc/mature`, `GET` / `POST /pickups`, `POST /pickups/import`, `POST /pickups/print`, `POST /pickups/{id}/cancel` |
| Printing | `GET` / `POST /print-batches`, `GET /print-batches/{id}`, `POST /print-batches/{id}/retry`, `GET /print-batches/{id}/file` |
| BIR 2307 | `GET` / `POST /cwt`, `GET /cwt/expected`, `POST /cwt/{id}/receive`, `/checklist`, `/settle-cash`, `GET` / `POST /cwt/batches`, `GET /cwt/batches/{id}/tags`, `POST /cwt/batches/{id}/route`, `/release` |
| Setup | `GET` / `POST /series`, `PUT /series/{id}`, `POST /series/{id}/authorize`, `/deactivate`, `GET /layouts`, `PUT /layouts/{code}`, `GET /minimal-balance/rules`, `POST /minimal-balance/sweep`, `GET /commission-payments`, `POST /commission-payments/issue` |

Permissions are those of V760: `CASH_RECEIPT`, `CASH_CANCEL`, `CASH_REINSTATE`, `CASH_APPROVE`,
`CASH_APPLY`, `CASH_UPLOAD`, `CASH_DISPOSITION`, `CASH_DISPOSITION_APPROVE`, `CASH_SERIES_MANAGE`,
`CASH_PRINT`, `CWT_TAG`, `CWT_PROCESS` and `DISB_PROCESS`.

### 2.9 Screens (group *Finance*, section *Cashiering*)

- Cashiering Workbench
- Receive Payment: two panes, with the live application preview by component, the 98% CWT badge,
  the excess to unapplied and the BOOK-rate chip.
- Receipts: search, the Cancellations and Reinstatements tab, and Issue Official Receipt.
- Receipt page: summary, `WorkflowPanel` of the open request, and the tabs Applications / Lines /
  Journal / History. Actions are Print, Cancel and Reinstate.
- Unapplied Payments: workbench with bulk actions, and the item page with the disposition form.
- Pre-booked Payments
- Payment Uploads: `BulkUploadWizard` per file type and the run summary tiles.
- PDC Warehouse: by maturity month.
- Check Pick-up
- Batch Print: preview, progress and retry.
- BIR 2307
- Commission ORs
- Receipt Series: remaining-count gauge.
- Cashiering Setup: layouts and minimal balance.

Each screen has its help entry in `features/cashiering/help.ts`.

### 2.10 Reports (CSHID.023, OQ42/OQ43)

The 24 reports are registered as `ReportMetadata.operations` and built with
`TabularReportBuilder`:

- `CSH-APPLIED-PREM`, `CSH-APPLIED-COMM`
- `CSH-PDC-WAREHOUSE`
- `CSH-MINBAL-EXCESS`, `CSH-MINBAL-PREMIUM`, `CSH-MINBAL-COMMISSION`
- `CSH-CANCELLED-OR`, `CSH-CANCELLED-AR`
- `CSH-CHECK-PICKUP`
- `CSH-PRIORITY-POSTED`
- `CSH-UNAPPLIED-COMM-MANCOM`, `CSH-UNAPPLIED-COMM-YTD`
- `CSH-DAILY-CASH-REC`
- `CSH-ADVANCE-PAYMENT`
- `CSH-PAYMENT-REVERSAL`
- `CSH-DIRECT-PAYMENT`
- `CSH-REINSTATEMENT-MON`, `CSH-REINSTATEMENT`
- `CSH-REAPPLICATION`
- `CSH-CERT-OF-PAYMENT`
- `CSH-CWT`
- `CSH-AR-OUTSTANDING`
- `CSH-BATCH-RUN`
- `CSH-2307-TXN`

Reports whose field list the BRD does not give carry a "draft layout (OQ42)" note. The access
rules for generating, downloading and view-only use are the platform report permissions
(CSHID.017/018).

### 2.11 Contracts for the other modules

| Contract | Kind | Use |
|---|---|---|
| `ReceiptIssuer.issueOfficialReceipt` | port implemented (`CashieringReceiptIssuer`) | remittance and commission get an OR number, idempotent on (module, source ref) |
| `UnappliedSink.create` | port implemented (`CashieringUnappliedSink`) | adjustment, remittance and commission put money into unapplied payments for disposition, idempotent on (module, source ref) |
| `PaymentReapplier.reapply(ReapplyRequest(invoiceNo, "ADJUSTMENT", "ADJ:<request>", date, reason))` | port implemented (`CashieringPaymentReapplier`) | reads the balances of the original invoice (already unlocked and reduced), reverses its active applications (negative `APPLIED`) and applies the money again in hierarchy order. The excess becomes one unapplied item (origin REAPPLY, net Dr 1210.x / Cr 2205), and realised commission is reversed with the applications. Returns `ReapplyResult.none` when nothing was applied. It never throws `PAYMENT_REAPPLIER_UNAVAILABLE`, and it is idempotent on (module, source ref, invoice) through `csh_reapplication` |
| `placement.service.PaymentConfirmationSource` | adapter (`CashieringPaymentConfirmationSource`, source `CASHIERING`) | returns `APP:<id>` for each active application and `PRE:<id>` for each payment waiting in the pre-booked queue of the ARN, so a payment received before booking opens the NB payment gate |
| `OpsWorkCountSource`, `InvoiceRelatedItems`, `PendingApprovalSource` | SPIs implemented | Operations home tiles (section `CASHIERING`), the invoice 360 section `RECEIPTS`, and the approvals inbox (series to authorize, receipt actions and dispositions) |
| `CollectionFeed` (`PickupFlowInHandler`) | flow-in handler | check pick-up requests from the Collection system (OQ01/OQ13) |

### 2.12 Demo

`V991` adds:

- GL 4190 and the demo rules of the 11 events;
- the series `AR-HO-`, `AR-CEB-` and `OR-HO-`;
- two check pick-up requests;
- the bank and cash-on-hand parameters.

At start-up (demo profile) `CashieringDemoData` posts, as `cashier`:

- a full and a partial payment;
- an unmatched payment with a refund disposition submitted for approval;
- a payment by ARN (`ARN-2026-940005`);
- a cancellation request;
- a warehoused PDC;
- a service-fee OR.

As `mktcoll` it tags a 2307. The demo users are `cashier`, `cashbr` (CEB), `cashtl` (team
leader, approvals), `mktcoll`, `disb` and `approver`.

### 2.13 Parked (seam only)

| Item | Question | Seam |
|---|---|---|
| Bank / channel file layouts (Bills Payment FS01, Trade, CLPC, Direct Credit) | OQ03, OQ04 | `csh_payment_file_layout` per handler (AUTO / DELIMITED / FIXED_WIDTH), changed on *Cashiering Setup* |
| BIR ATP and receipt series per branch | OQ05 | series master with ATP number and warning level (`RECEIPT_SERIES_LOW`) |
| Who approves cancellations, reinstatements and which dispositions | OQ06, OQ15 | workflow `OPS_RECEIPT_ACTION` (CASH_APPROVE) and `requires_approval` per disposition type |
| GL accounts of the Cashiering events, bank / cash-on-hand accounts | OQ07 | event types with demo rules; parameters `CASH_BANK_ACCOUNT` / `CASH_ON_HAND_ACCOUNT` empty in production |
| Minimal balance limits, exclusions and targets | OQ11 | `csh_minimal_balance_rule` (PHP 10, CWT / DST / whole premium excluded) |
| Payments before booking | OQ12 | pre-booked queue, `PREBOOKED_REMATCH`, `PREBOOKED_AGEING`, `PRE:<id>` to the NB payment gate |
| Collection system (check pick-up, refunds) | OQ01, OQ13 | `CollectionFeed` handler and manual entry; refunds through `DisbursementGateway` |
| Commission OR grouping (one per check or per payment) | OQ14 | one OR per staged payment line of `COMMISSION_PAYMENT` |
| 2307 routing and certificate handling with Disbursement | OQ16 | `OPS_CWT_2307` workflow, `DisbursementGateway` type CWT2307, release on `DisbursementStatusChanged` PAID |
| Report layouts and ageing buckets | OQ42, OQ43 | draft layouts; the pre-booked ageing threshold comes from the alert |
| Search log retention | OQ47 | `csh_search_log` kept |

### 2.14 Fit/gap status

| BR ID | Status | Where |
|---|---|---|
| CSHID.001 | Built | AR issue (OTC, uploads, PDC, pick-up), cancel and reinstate with approval |
| CSHID.002 | Built | Head Office OR per `OR_TYPE` with VAT / WTAX lines, cancel and reinstate |
| CSHID.003 / 004 / 005 | Built (lists OQ24) | reasons from LOV, encoded reinstatement fields by reason group |
| CSHID.006 / 015 | Built (ATP OQ05) | series per kind and branch, OR from Head Office only, depletion check, `RECEIPT_SERIES_LOW` |
| CSHID.007 | Built (grouping OQ14) | `COMMISSION_PAYMENT` upload, *Issue ORs* |
| CSHID.008 | Built (layouts OQ03/OQ04) | `PAY_BILLS`, `PAY_TRADE`, `PAY_CLPC`, `PAY_DIRECT_CREDIT`, `PAY_PDC`; PDC warehouse and `PDC_MATURITY` |
| CSHID.009 | Built (Collection feed OQ01/OQ13) | check pick-up queue, *Print ARs* |
| CSHID.010 | Built | receipt search with ten criteria, search log |
| CSHID.011 | Built | audit trail per receipt action |
| CSHID.012 / 013 / 014 | Built (GL OQ07) | `OPS_AR_RECEIPT`, `OPS_OR_ISSUE`, `OPS_RECEIPT_REINSTATE` and their reversals |
| CSHID.016 | Built (limits OQ11) | `MINIMAL_BALANCE_SWEEP`, rule table |
| CSHID.017 / 018 | Built | platform report access |
| CSHID.019 | Built | batch print with preview, progress and retry |
| CSHID.020 | Built (pre-booked OQ12) | matching at acceptance, pre-booked queue, automatch |
| CSHID.021 | Built | AR Insurance (`OPS_AR_INSURANCE_RECEIPT`), segregated from premium |
| CSHID.022 | Built | component hierarchy, 98% for CWT clients, excess to unapplied |
| CSHID.023 | Built (layouts OQ42/OQ43) | 24 Cashiering reports |
| CSHID.024 / 025 | Built (approvals OQ15) | unapplied workbench, dispositions, four eyes, reversal |
| CSHID.026 / 027, MKTID.010 / 013 | Built (routing OQ16) | 2307 tagging, validation, report, reclass, DTIP offset, cash path |
| DBMID.001 | Built (in-app Disbursement queue, OQ02) | route to Disbursement, release on PAID |
