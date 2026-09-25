# Operations (BRD-2)

Guide of the Operations modules of iNXT BrokerVerse: the shared invoice ledger (`opsledger`), and
cashiering, remittance, product reconciliation, adjustment and commission receivables. The build
design is [`docs/architecture/OPERATIONS_DESIGN.md`](../architecture/OPERATIONS_DESIGN.md); the
requirements are [`docs/requirements/BDOI_OPS_BRD_SPEC.md`](../requirements/BDOI_OPS_BRD_SPEC.md)
and their status per BR ID is in
[`docs/requirements/BDOI_OPS_TRACEABILITY.md`](../requirements/BDOI_OPS_TRACEABILITY.md).
Section 0 is the overview of the whole; sections 1 to 6 describe each module; section 7 is the
integration of the modules (wave O2) and section 8 the consolidated list of parked items.

## 0. Overview

### 0.1 What Operations does

Operations takes over every booked invoice from New Business and follows its money until the
insurer has been paid and the file is closed.

| Team | Module | Screens (sidebar) | What it does |
|---|---|---|---|
| All Operations teams | `opsledger` | *Operations*: home, invoice search, Invoice 360, Disbursement queue, hand-offs, interfaces, report archive, notification settings | One ledger line per invoice with its premium receivable by component, DTIP, commission, flags, lock and statuses; the ports to the systems BDOI has not specified yet |
| Cashiering | `cashiering` | *Finance / Cashiering* | Receives payments (counter, bank files, PDC, pick-up), issues ARs and ORs, applies money to the invoices by component, keeps unapplied money until it is disposed of, runs the BIR 2307 flow |
| Remittance | `remittance` | *Finance / Remittance* | Extracts what clients paid, groups it into batches per insurer, gets them approved, asks Disbursement to pay the insurer and records the insurer's OR; holds and special remittances |
| Product reconciliation | `prodrecon` | *Client & Policy / Product Reconciliation* | Sends each insurer the register of what BDOI booked, takes the insurer's answer and follows every difference |
| Adjustment | `adjustment` | *Client & Policy / Adjustment* | Endorsement and cancellation requests on booked invoices, with validation, approval, posting, re-application of payments and the AR Insurer after remittance |
| Commission receivables | `commission` | *Finance / Commission Receivables* | Bills insurers for the commission on direct payment accounts, collects it with an OR, reverses the premium receivable; incentive schemes and BIR certificates |

The modules never call each other. They share the ledger (`InvoiceLedgerService`), the events of
`OpsLedgerEvents` and the ports of `opsledger.service.port`, which the owning module implements
(section 1.3 and 7.2).

### 0.2 The life of an invoice (end to end)

The flow below is the one `OperationsEndToEndIT` walks through with the demo users (section 7.3).
Each arrow is a real service call; the ledger movement it leaves is in brackets.

```mermaid
flowchart LR
    NB["Booking (New Business)<br/>InvoiceBooked"] -->|feed after commit| L[("Invoice ledger<br/>ops_invoice<br/>[BOOKED]")]
    subgraph CASH[Cashiering]
      P["Payment<br/>OTC / files / PDC / pick-up"] --> AR[AR issued<br/>OPS_AR_RECEIPT]
      AR --> M{Match}
      M -->|booked| APP["Applied per component<br/>OPS_PAYMENT_APPLY [APPLIED]"]
      M -->|not booked yet| PRE[Pre-booked queue]
      M -->|no match / excess| UN[Unapplied item]
      UN --> DSP[Disposition<br/>refund / apply / reclass]
    end
    PRE -.->|PaymentConfirmationSource<br/>PRE: / APP:| GATE[Placement payment gate]
    L --> P
    APP --> X
    subgraph REM[Remittance]
      X["Extraction<br/>tags, batch per insurer"] --> B["Batch<br/>exclude / restore"]
      B -->|submit, four-eyes approve<br/>OPS_REMITTANCE [REMITTED]| APPR[Approved]
      APPR --> OR1[Commission OR<br/>ReceiptIssuer]
      INSOR[Insurer OR upload<br/>INSURER_REMIT_OR] --> ORR[OR received]
    end
    APPR -->|DisbursementGateway| DQ[Disbursement queue]
    DQ -->|DV assigned| FR[Fully / partially remitted<br/>invoice unlocked]
    FR --> INSOR
    DSP -->|refund| DQ
    subgraph REC[Production reconciliation]
      RX[Register extract and send] --> RU[Insurer upload<br/>INSURER_PRODUCTION] --> RM["Match: matched / discrepancy /<br/>BDOI only / insurer only"]
      RM -.->|EarlyIncentiveRules| EI[Early incentive check]
    end
    L --> RX
    subgraph ADJ[Adjustment]
      RQ[Request] --> VAL[Validate / approve] --> PST["Post<br/>EndorsementPostingService [ADJUSTED]"]
      PST -->|after remittance| ARI[AR Insurer<br/>OPS_AR_INSURER_SETUP<br/>PENDING_NEG_ADJ]
      PST -->|paid invoice| RA[PaymentReapplier]
    end
    FR --> RQ
    RA --> UN
    subgraph CMR[Commission receivables]
      DPL["DP list<br/>COLLECTION_DP_LIST"] --> BIL[Billing to insurer] --> ANS[Insurer answer] --> COL["Collection<br/>OPS_DP_COMMISSION_COLLECT<br/>[DP_REVERSAL]"]
      COL --> OR2[Commission OR<br/>ReceiptIssuer]
    end
    L --> DPL
    L --> WO["Minimal balance file<br/>OPS_WRITE_OFF [WRITE_OFF]"]
```

In words:

1. **Booking.** `InvoiceBooked` reaches the ledger after the booking commits (`InvoiceLedgerFeed`):
   components booked, remittance status `WITH_OUTSTANDING_BALANCE`, the booking's open items in
   the subledger.
2. **Payment.** Cashiering issues the AR and applies the money in hierarchy order (DST, premium tax
   / VAT, LGT, FST, other charges, basic); the invoice becomes `PAID`. A payment for an account
   that is not booked yet waits in the pre-booked queue and opens the placement payment gate.
3. **Remittance.** Extraction puts the paid invoice in a batch and locks it; exclusion unlocks it
   and restore takes it back; submission and four-eyes approval post the remittance (DTIP,
   commission, VAT and withholding tax remitted), issue the commission OR and send the payment
   request to Disbursement. The DV makes the invoice `FULLY_REMITTED` and releases the lock; the
   insurer's OR schedule closes the batch.
4. **Reconciliation.** The register goes to the insurer; the insurer's file is matched, and the
   early incentive of each remitted invoice is checked against remittance's rules.
5. **Cancellation after remittance.** The adjustment posts the return on the original invoice,
   sets up the AR Insurer for the remitted DTIP, keeps `PENDING_NEG_ADJ` for remittance and has
   cashiering re-apply the payments: the money becomes an unapplied item to refund.
6. **Direct payment.** The DP account is listed, billed, approved by the insurer and collected: the
   premium receivable is reversed and cashiering issues the commission OR.
7. **Minimal balance.** A small balance is written off from the minimal balance file; remittance
   then refuses the written-off invoice.

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
| `EarlyIncentiveRules` | `Optional<Terms> termsFor(Long companyId, Subject)` | no rule | remittance (`RemittanceEarlyIncentiveRules`, O2) |
| `UnappliedDirectory` (BRD-4) | `Page<UnappliedView> open(Long companyId, UnappliedFilter, Pageable)`; `Optional<UnappliedView> find(String unappliedRef)`; `List<UnappliedEvent> history(String unappliedRef)` | `EmptyUnappliedDirectory` | cashiering (COLLECTIONS_DESIGN 9) |
| `UnappliedDispositionRequests` (BRD-4) | `DispositionTicket request(DispositionRequest)`; `Optional<DispositionTicket> status(String source, String sourceRef)` | `HandoffDispositionRequests`: `DEFERRED` + hand-off for team `CASH_DISPOSITION` | cashiering |
| `RefundValidationSource` (BRD-5) | `String validator()`; `ValidationTicket open(ValidationRequest)`; callers use `RefundValidations.open` | `HandoffRefundValidationSource` (`ANY`): `DEFERRED` + hand-off for `ACSL_PROCESS` / `CASH_DISPOSITION` | acsl (ACSL), cashiering (CASHIERING) |
| `PaymentReversalRequester` (BRD-5) | `ReversalTicket request(ReversalRequest)` | `HandoffPaymentReversalRequester`: `DEFERRED` + hand-off for `CASH_APPLY` | cashiering |
| `InvoiceCorrectionSink` (BRD-5) | `CorrectionResult record(CorrectionRequest)` | `LedgerInvoiceCorrectionSink`: signed `CORRECTION` movements | opsledger itself |

`CollectionFeed` also has `default void acknowledge(Long companyId, String feedCode, Collection<String> keys)`
(no-op for the manual transport; the in-app adapter of Collections marks its outbox rows TAKEN). The
`DisbursementGateway` request (`DisbursementRequest.Spec`) and the event `DisbursementStatusChanged` carry the BRD-5
fields (RFP number, payee class, disbursement type, documents, root invoice, accounting references, straight-to-approval,
DV and instrument statuses) and the status CANCELLED; the BRD-2 constructors are kept
(ACCOUNTING_DISBURSEMENT_DESIGN section 17).

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

**Planned consumers from later BRDs** (designed, not built; every item is **to be agreed with the Operations owner**,
and no contract above changes until it is; see `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md`):
- **Claims** (`brokerclaims`, BRD-7) implements `ClaimsFeed` with `InAppClaimsFeed`, feed `CLAIMS_SPECIAL_REMIT`
  (CLAIMS_BROKING_DESIGN section 3.1). `SpecialRemittanceService.claimsNote` would then refuse a CLAIMS-condition
  request without an eligible claim, where today it only adds a note (section 3.5).
- **Submitted Policies** (`submitted`, BRD-12) needs `UnappliedDispositionRequests.Action.RECOGNIZE_INCOME` with an
  income type (`DispositionRequest.incomeType`) and the cashiering disposition type HANDLING_FEE
  (SUBMITTED_POLICIES_DESIGN section 9).
- **Renewal** (`renewal`, BRD-6) needs a business-type column on `ops_invoice`, copied from `InvoiceBooked` by
  `InvoiceLedgerFeed` (RENEWAL_DESIGN section 13; ACSL "account status Renewal" needs it too).
- **Customer Servicing Facility** (`csf`, BRD-9) needs `InvoiceLedgerQueryService.paymentsOfClient(companyId,
  clientCode, from)` (CUSTOMER_SERVICING_DESIGN section 11).

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
| `DisbursementStatusChanged` | companyId, requestNo, type, sourceModule, sourceRef, status, dvNo, reason, dvStatus, instrumentStatus |
| `CollectionFeedReady` | companyId, feedCode (published by Collections after commit) |
| `UnappliedDispositionChanged` | companyId, unappliedRef, source, sourceRef, status (ACCEPTED / REJECTED / APPLIED), cashieringRef, message (published by cashiering) |
| `RefundValidationCompleted` | companyId, validator, sourceModule, sourceRef, confirmed, newArNo, remarks (published by acsl / cashiering) |
| `PaymentReversalCompleted` | companyId, sourceModule, sourceRef, approved, reference, remarks (published by cashiering) |

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
| `COLLECTION_CWT2307`, `COLLECTION_COMMISSION_PAYMENT` (`CollectionFlowInFeeds`, O2) | flow-in handlers | 2307 tags and commission payment details from Collection, with the checks of the `CWT_TAGS` / `COMMISSION_PAYMENT` uploads and a `Company` column |

### 2.12 Demo

`V991` adds:

- GL 4190 and the demo rules of the 11 events;
- the series `AR-HO-`, `AR-CEB-` and `OR-HO-`;
- two check pick-up requests;
- the bank and cash-on-hand parameters.

At start-up (demo profile, order 91, before remittance) `CashieringDemoData` posts, as `cashier`,
with payments valued a week back so remittance can extract them:

- the booking and endorsement invoices of `ARN-2026-940001` paid in full and a partial payment of
  `ARN-2026-940004`;
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
- `RemittanceDemoData` (profile `demo`, order 92) asks for a hold on ARN-2026-940004, remits the
  paid invoice of ARN-2026-940001 through approval, DV and insurer OR, and runs a manual
  extraction whose batch waits for review (section 7.4).
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

`V994` adds GL 4190 and the demo rules of the three events. At start-up (demo profile, order 93,
after cashiering and remittance) `AdjustmentDemoData` leaves a request at every stage: on
`ARN-2026-940002` a draft, a request waiting for validation, a returned request and a premium rate
increase waiting for approval; a flat cancellation of `ARN-2026-940004` ready for the posting
batch; an internal adjustment of `ARN-2026-940001` posted (section 7.4).

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
  insurer's rate and window from the `opsledger.service.port.EarlyIncentiveRules` port, which
  remittance implements with its `rem_incentive_rule` rules (section 7.1). An invoice no rule
  covers is `NO_RULE`; the rates themselves are parked (OQ23).

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
| PRCID.028 | Built: the `EarlyIncentiveRules` port, read from remittance's incentive rules, and the report. The rates are parked (OQ23). |
| PRCID.029 | Built: workflow history and audit trail. |
| PRCID.035, 036, 037 | Built: reports. |

### 5.7 Parked

| Item | Question | Seam |
|---|---|---|
| Extract frequency, template, naming, password per insurer | OQ29 | `prc_schedule`, `PRODRECON_FILE_PATTERN`, `PRODRECON_COVER_LETTER` |
| Match keys and automatch time | OQ30 | `RECON_MATCH_KEYS`, `RECON_TOLERANCE`, `recon-automatch-cron` |
| Company concerned and disposition lists | OQ31 | LOVs `RECON_COMPANY_CONCERNED`, `RECON_DISPOSITION` (demo values in V993) |
| Early incentive rates and windows | OQ23 | `rem_incentive_rule` read through `opsledger.service.port.EarlyIncentiveRules` |
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

## 7. Integration of the modules (wave O2)

### 7.1 What changed in the integration wave

| Change | Where | Why |
|---|---|---|
| `EarlyIncentiveRules` moved from `prodrecon.service.port` to `opsledger.service.port`; its "no rule" default moved to `OpsPortDefaults`; remittance implements it (`RemittanceEarlyIncentiveRules`, rule `rem_incentive_rule` covering the insurer, product line and segment on the booking date) | `opsledger`, `remittance`, `prodrecon` | PRCID.028 / RMTID.023: production reconciliation validates the early incentive against the rates remittance keeps. The port sits with the other Operations ports so that the sibling modules stay independent (design section 2.1) |
| Flow-in handlers for `COLLECTION_CWT2307` and `COLLECTION_COMMISSION_PAYMENT` (`CollectionFlowInFeeds`): each record runs the checks and commit of the `CWT_TAGS` / `COMMISSION_PAYMENT` bulk handlers, once per idempotency key; the files carry the bulk columns plus `Company` | `cashiering` | Every inbound feed by upload now has its handler, so *Operations → Interfaces* accepts all of them (CSHID.007/026, MKTID.013) |
| Adjustment's `ADJUSTED` movement of a return invoice carries the booking **journal batch** (it carried the return invoice number in the journal field) | `adjustment` (`LedgerEffects`, `AdjustmentPostingService`) | Bug found by the end-to-end test: Invoice 360 and the journal checks follow the movement's journal batch |
| Home tile labels in Title Case, minor words lowercase ("Batches in Review", "Paid, Not Yet Extracted") | every `OpsWorkCountSource` | BDO UX guideline; guarded by `OperationsSeamsIT` |
| Invoice 360: `RecordSummary` with reference chips, payment and remittance pills and flag tags; one tab per module (Receipts, Remittances, Adjustments, Reconciliation, Commission) with its record count and an empty state | `features/operations` | BDO UX guideline section 7 |
| Demo runners in storyline order, each signed in as the demo user whose job it is (`opsledger.demo.DemoUsers`) | all Operations `demo` packages | Real data on every Operations screen after start-up (section 7.4) |

### 7.2 Ports and their beans

| Port | Bean with all Operations modules installed | Default (kept for parked integrations and for a module's own tests) |
|---|---|---|
| `ReceiptIssuer` | `CashieringReceiptIssuer` | `HandoffReceiptIssuer` |
| `UnappliedSink` | `CashieringUnappliedSink` | `HandoffUnappliedSink` |
| `PaymentReapplier` | `CashieringPaymentReapplier` | `LedgerPaymentReapplier` |
| `EarlyIncentiveRules` | `RemittanceEarlyIncentiveRules` | no rule |
| `DisbursementGateway` | `QueueDisbursementGateway` (default) | parked: Disbursement module of BRD-5 (OQ02 answered) |
| `CollectionFeed` | `ManualCollectionFeed` (default) | parked: Collections module of BRD-4 (OQ01 answered) |
| `InsurerFileInbox` | `ManualInsurerFileInbox` (default) | parked: insurer channels |
| `FileDropPort` | `RepositoryFileDrop` (default) | parked: shared drive / FS04 (OQ17) |
| `MarketingFeed`, `ClaimsFeed` | none | parked (OQ45, OQ46) |
| `placement.service.PaymentConfirmationSource` | `CashieringPaymentConfirmationSource` (`CASHIERING`) next to placement's own report source | - |

`FlowInAndPortsIT` asserts this table. Inbound flow-in feeds and their handlers:

| Feed | Handler | Module |
|---|---|---|
| `COLLECTION_CHECK_PICKUP` | `PickupFlowInHandler` | cashiering |
| `COLLECTION_CWT2307` | `CollectionFlowInFeeds.Cwt2307Feed` | cashiering |
| `COLLECTION_COMMISSION_PAYMENT` | `CollectionFlowInFeeds.CommissionPaymentFeed` | cashiering |
| `COLLECTION_HOLD` | `RemittanceFeedHandlers.HoldFeed` | remittance |
| `COLLECTION_SPECIAL_REMIT` | `RemittanceFeedHandlers.SpecialRemitFeed` | remittance |
| `INSURER_REMIT_OR` | `RemittanceFeedHandlers.InsurerOrFeed` | remittance |
| `INSURER_PRODUCTION` | `InsurerProductionHandler` | prodrecon |
| `COLLECTION_DP_LIST` | `DpListHandler` | commission |
| `INSURER_DP_RESPONSE` | `DpResponseHandler` | commission |

`OPS_INVOICE_FEED` is fed in-app by booking; `COLLECTION_DP_RETURNED`, `COLLECTION_REFUND`,
`DISBURSEMENT_REQUEST` and `DISBURSEMENT_STATUS` are outbound or in-app and have no upload.

### 7.3 End-to-end test

`opsintegration.OperationsEndToEndIT` runs the flow of section 0.2 through the real services as
the demo users, without mocks (`OpsJourney` and `OpsJourneyLater` hold the steps). At each step it
checks the ledger (components, movements, flags, payment and remittance status, lock), the journals
(every accounting event of the step is posted and balanced, every journal a movement points to
exists and balances) and the open items (the booking's subledger items, the ledger balances).

| Step | Users | Checks |
|---|---|---|
| 1 Booking | proc | `UNPAID`, `WITH_OUTSTANDING_BALANCE`, PR = DTIP = gross, `BOOKED` movements, client premium, DTIP and commission open items |
| 2 OTC payment | cashier | AR `OPS_AR_RECEIPT`, application `OPS_PAYMENT_APPLY`, `APPLIED` per component equal to booked, DTIP untouched, `PAID` |
| 3 Payment gate | ao, proc, cashier | payment by ARN before booking goes pre-booked; the placement sweep opens the gate with evidence `CASHIERING` / `PRE:<id>` |
| 4 Extraction | remit | tag `EXTRACTED`, batch in review, lock `REMITTANCE`, paid AR on the line |
| 5 Exclusion, restore, approval | remit, remittl | exclusion unlocks, restore locks, approval refused to the submitter, `OPS_REMITTANCE`, DTIP and commission remitted, commission OR issued by cashiering |
| 6 Disbursement | disb | request amount = payable, DV assigned and paid, batch and invoice `FULLY_REMITTED`, lock released |
| 7 Insurer OR | remit | `INSURER_REMIT_OR` run, line `MATCHED`, batch `OR_RECEIVED` |
| 8 Reconciliation | recon, remittl | register extracted, sent, answered; `MATCHED` and a gross premium discrepancy; early incentive `ELIGIBLE` at the rate of a remittance rule |
| 9 Cancellation after remittance | mktcoll, adjust, adjtl, cashier, cashtl | AR Insurer = remitted DTIP (`OPS_AR_INSURER_SETUP`), `CANCELLED`, `PENDING_NEG_ADJ`, applications reversed, unapplied item refunded through the Disbursement queue |
| 10 Direct payment | proc, commrec | DP list through `COLLECTION_DP_LIST`, billing, approval, collection `OPS_DP_COMMISSION_COLLECT`, `DP_REVERSAL`, commission OR issued by cashiering |
| 11 Minimal balance | cashier, adjust, remit | PHP 50 left by a payment, `MINIMAL_BALANCE_FILE` upload, `OPS_WRITE_OFF`, `WRITTEN_OFF`, extraction tag `UNEXTRACTED_DUE` / `WRITTEN_OFF` |

`OperationsSeamsIT` covers the early incentive port, the two new Collection feeds and the home
tiles (count, Title Case label, link to a screen). All data is created by the tests with unique
keys, so they pass in either order.

### 7.4 Demo storyline (`--spring.profiles.active=demo`, fresh database)

| Order | Runner | Signed in as | What it leaves on the screens |
|---|---|---|---|
| 80 | `booking.demo.BookingDemoData` (New Business) | - | four booked accounts, a positive endorsement of ARN-2026-940001, a partial cancellation of ARN-2026-940002 |
| 90 | `OpsLedgerDemoReplay` | admin | the ledger of every booked invoice (replay run on *Interfaces*) |
| 91 | `CashieringDemoData` | cashier, mktcoll | ARN-2026-940001 booking and endorsement invoices paid (value date a week back), ARN-2026-940004 partly paid, an unmatched payment with a refund for approval, a pre-booked payment (ARN-2026-940005), an excess with a cancellation request, a PDC, a service fee OR, a BIR 2307 tag |
| 92 | `RemittanceDemoData` | mktcoll, remit, remittl, disb | a hold for approval on ARN-2026-940004; the batch of ARN-2026-940001 approved, paid by DV-DEMO-0001 and closed by the insurer's OR; a manual extraction whose batch (the endorsement) waits for review |
| 93 | `AdjustmentDemoData` | mktcoll, adjust, adjtl | on ARN-2026-940002 a draft, a request for validation, a returned request and a rate increase for approval; a flat cancellation of ARN-2026-940004 for posting; an internal adjustment of ARN-2026-940001 posted |
| 94 | `ProdReconDemoData` | recon | the INS-MGIC September register extracted and sent, the insurer's answer uploaded: a match, a premium discrepancy and an unbooked policy |
| 96 | `CommissionDemoData` | commrec | the direct payment account ARN-2026-940003 listed, confirmed, billed and sent to INS-MGIC |

Each runner checks its own marker and does nothing on a restart; a step that fails is logged and
skipped. `OperationsDemoDataIT` starts the demo profile on its own database and checks the
storyline, the user of each step, the Operations home counts and that running the runners again
changes nothing.

## 8. Parked items (consolidated)

Everything here is built as a seam (port, configuration table, parameter or manual upload); no
integration is faked. "Superseded" means a later BRD answered the question and designs the
replacement; it is not built in Operations.

| Item | Question | Seam in Operations | Module(s) | Status |
|---|---|---|---|---|
| Collection system interface (check pick-up, 2307 tags, commission payments, holds, special remittance, DP lists, refunds) | OQ01, OQ13, OQ38, OQ45 | `CollectionFeed` (CSV in the extract repository), `COLLECTION_*` flow-in feeds by upload | opsledger, cashiering, remittance, commission | **Superseded by BRD-4**: Collections is a BrokerVerse module; `CollectionFeed` gets its in-app adapter there |
| Marketing activities MKTID.010/012/013 (2307 and DP PR tagging) | OQ45 | 2307 tagging screen and `CWT_TAGS` / `COLLECTION_CWT2307`; DP list | cashiering, commission | **Superseded by BRD-4** (Collections dispositions); MKTID.001-009/011 stay in Operations as built |
| Disbursement system, DV numbers and statuses, re-sending returned requests | OQ02 | `DisbursementGateway` and the in-app queue | opsledger, remittance, cashiering, commission | **Superseded by BRD-5**: Disbursement module implements the gateway |
| GL accounts of every Operations event, bank / cash accounts, subledger settlement of DTIP and PR open items | OQ07 | event types with demo rules; `CASH_BANK_ACCOUNT` / `CASH_ON_HAND_ACCOUNT`; ledger movements | all | **Partly superseded by BRD-5** (GL kept in BIBS; accounts still to be given) |
| Marketing and Claims feeds | OQ45, OQ46 | `MarketingFeed`, `ClaimsFeed` without adapters; special remittance note | opsledger, remittance | parked (Claims BRD) |
| Insurer channels (SFTP / API) | OQ22, OQ29, OQ38 | `InsurerFileInbox`, manual upload | opsledger, remittance, prodrecon, commission | parked |
| Shared drive | OQ17 | `FileDropPort` to the extract repository | opsledger, remittance, prodrecon | parked (FS04 named by BRD-4) |
| BOOK rate source | OQ08 | `RateType.BOOK` kept by hand | opsledger | parked (proposal in BRD-5) |
| Lock reasons and who lifts them | OQ28 | free-text reason, owner module unlocks | opsledger | parked |
| Bank / channel payment file layouts | OQ03, OQ04 | `csh_payment_file_layout` per handler | cashiering | parked |
| BIR ATP and receipt series | OQ05 | series master with ATP number | cashiering | parked |
| Approvers of cancellations, reinstatements, dispositions | OQ06, OQ15 | workflows and `requires_approval` per disposition type | cashiering | parked |
| Minimal balance limits and targets | OQ11 | `csh_minimal_balance_rule`, `MIN_BALANCE_FILE_RANGE` | cashiering, adjustment | parked |
| Payments before booking | OQ12 | pre-booked queue, `PREBOOKED_REMATCH` | cashiering | parked |
| Commission OR grouping | OQ14 | one OR per staged payment line | cashiering | parked |
| 2307 routing with Disbursement | OQ16 | `OPS_CWT_2307`, gateway type CWT2307 | cashiering | parked |
| Remittance schedule, file naming, EOD trigger | OQ17, OQ18 | cron property, `REMIT_FILE_PATTERN`, `rem_eod_request` | remittance | parked |
| Paid AR above DTIP | OQ19 | `REMIT_PAIDAR_OVER_DTIP_MODE` | remittance | parked |
| Holding period start and "cleared" source | OQ20 | banking days from the last applied value date | remittance | parked |
| Early incentive rates and window | OQ23 | `rem_incentive_rule` (read by prodrecon through `EarlyIncentiveRules`) | remittance, prodrecon | parked (rates) |
| Hold roles, maximum and extensions | OQ24 | `HOLD_REQUEST` / `HOLD_APPROVE`, no maximum | remittance | parked |
| Extract frequency, template, naming, password per insurer; match keys | OQ29, OQ30 | `prc_schedule`, `PRODRECON_FILE_PATTERN`, `RECON_MATCH_KEYS`, `RECON_TOLERANCE` | prodrecon | parked |
| Company-concerned and disposition lists; open LOVs | OQ24, OQ31, OQ32, OQ40 | LOV types, demo or "Others" values | all | parked |
| Endorsement numbering, documents, account update by non-financial endorsements | OQ32 | `ENR-`/`ES-` numbers, `ENDORSEMENT_DOC_TYPE`, recorded by booking | adjustment | parked |
| Package TSI limits, co-insurance | OQ33 | catalog `max_sum_insured`, insurer shares | adjustment | parked |
| Credit memo on commission decrease; refund basis | OQ34, OQ36 | `ServiceInvoiceService.credit`; basis per request | adjustment | parked |
| Over-adjustment baseline | OQ37 | `ADJ_BASELINE_PERCENT` | adjustment | parked |
| DP list folders; billing and answer formats | OQ38, OQ40 | `COLLECTION_DP_LIST` upload, `DpBillingSender` columns, `INSURER_DP_RESPONSE` layout | commission | parked (DP list source superseded by BRD-4) |
| Incentive targets and amounts | OQ39 | inactive demo schemes | commission | parked |
| OR issuance for DP collections; certificates | OQ41 | `ReceiptIssuer` (cashiering OR); `cmr_certificate` | commission | answered by BRD-5 (one received-certificate register) |
| Report layouts and ageing buckets | OQ42, OQ43 | draft layouts | all | parked |
| Search log retention | OQ47 | `csh_search_log` kept | cashiering | parked |
| Access matrix per role | OQ48 | grants of V760 | all | parked (partly answered by BRD-4/5) |
| Direct payment PR accounting | OQ07 | `DP_PR_REVERSAL_POSTING` off; ledger movement always | commission | parked |
