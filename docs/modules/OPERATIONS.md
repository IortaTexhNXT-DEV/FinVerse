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
