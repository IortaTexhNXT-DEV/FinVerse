# iNXT BrokerVerse - BDOI Operations (BRD-2) Build Design

Status: **proposal for review**. It extends `docs/architecture/BROKING_ARCHITECTURE.md` (binding) and the
Developer Guide, and it does not change them. Requirements baseline: [`BDOI_OPS_BRD_SPEC.md`](../requirements/BDOI_OPS_BRD_SPEC.md) (169 requirement IDs,
fit/gap and questions OQ01-OQ50). Every class, migration and screen cites its BRD ID in Javadoc or a comment,
for example `CSHID.020`.

## 1. Design principles

1. **Trust money is kept separate from BDOI income.**
   - An **AR** acknowledges premium that BDOI collects on behalf of an insurer. It is posted to trust or
     receivable accounts.
   - An **OR** is a BIR receipt for BDOI's own income: commission, service fee, profit share, incentives or
     others. It is issued by Head Office only (CSHID.006).
   - The two are different documents with different series, even though one entity class holds both.
2. **One invoice ledger is the spine of Operations.** The booked invoice from BRD-1 `booking` is copied once
   into `opsledger`. That copy holds:
   - the premium receivable (PR) by component: DST, premium tax / VAT, LGT, other charges, basic;
   - DTIP, commission, VAT, WTAX and CWT;
   - the insurer shares.

   Every Operations movement is recorded against this ledger: application, reversal, remittance, adjustment,
   write-off and direct-payment (DP) reversal. Cashiering, Remittance, Prod Recon, Adjustment and Commission
   all read balances, statuses, flags and locks from it. None of them reads another sibling's tables.
3. **Sibling modules never call each other.** The five business modules depend on `opsledger` and on the
   platform and BRD-1 modules only. Where one of them needs another's action (issue an OR, re-apply payments,
   create an unapplied item, push to Disbursement), it calls a **port declared in `opsledger`** that the owner
   module implements. This keeps the graph free of cycles and lets four agents build in parallel.
4. **GL accounts are never chosen in code.** Every posting is a `BusinessEvent`, and the rules are configured by
   Comptrollership (ADJID.011 note). Demo rules are shipped only for the demo chart.
5. **Parked means seam, not fake.** Collection, Disbursement, Marketing, Claims, insurer channels, bank files and
   the shared drive each get a port. The default adapter is a manual upload, an in-app work queue or an
   internal module. No integration is simulated.
6. **The addendum overrides the main BRD.**
   - The remittance extract grid allows exclusion only (RMTID.002).
   - An adjustment that changes income produces a service invoice (ADJID.014).

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `opsledger` | Invoice ledger (components, shares, movements), invoice flags and locks, remittance / hold / DP status read model, invoice 360 view, flow-in framework (inbound / outbound feed runs), Operations ports and events, Operations dashboard counts API | BRQID.004/005, RMTID.026/032/038/040, ADJID.027, MKTID.011 (+ read side of all) | booking, account, catalog, crm, subledger, accounting, workflow, messaging, bulk, system, alert, lov | V760-V763 (V990) |
| `cashiering` | Receipt series; AR and OR (create, cancel, reinstate); payment intake (files, OTC, PDC warehouse, check pick-up); matching engine and component application; unapplied payments and dispositions; minimal-balance sweep; AR Insurance; BIR 2307 intake and reversal; batch printing; Cashiering reports | CSHID.001-027, MKTID.010/013, DBMID.001 | opsledger (+ platform, account, organization, currency, docgen, attachment) | V764-V769 (V991) |
| `remittance` | Extraction (scheduled / manual), exclusion grid, batches and Process Remittance workflow, remittance schedule and payment request, push to Disbursement, insurer OR upload and exception report, holds, special remittance, early-remittance incentive, Remittance reports | RMTID.001-025/027-031/033-036/039, MKTID.001-007/009 | opsledger (+ platform, organization, docgen, messaging) | V770-V774 (V992) |
| `prodrecon` | Production register extraction per insurer, locked-column file, cover letter, send, insurer upload, duplicate block, matching with tolerance, buckets, feedback / disposition, unbooked repository, recon reports, early-incentive validation | PRCID.001-039 | opsledger (+ platform, messaging, docgen, account read) | V775-V779 (V993) |
| `adjustment` | Endorsement requests (financial / non-financial / internal) on booked invoices, single and batch, return, approval, recompute per insurer, posting through booking, excess and AR Insurer set-up, service invoice link, endorsement slip, validation list and slip, duplicate check, minimal-balance / write-off file, over-adjustment control, reports | ADJID.001-026/028, MKTID.008 | opsledger, booking, catalog, account (+ platform, docgen, attachment) | V780-V784 (V994) |
| `commission` | DP list intake and consolidation, DP validation and sanitation, commission receivable, insurer billing and feedback SLA, DP PR reversal, incentive schemes (No Touch, Top Up, Motor Mania), BIR certificate submission to Comptrollership, production / commission reports, estimated items | CMRID.001-015, MKTID.012, RMTID.037 | opsledger, catalog (+ platform, messaging, docgen) | V785-V789 (V995) |

Changes to platform modules, made once in wave O0 by the owner of `opsledger`:
- `bulk`: a TXT fixed-width / delimited reader, a file SHA-256 duplicate block, handler outcome categories and "reprocess failed rows" (BRQID.006, CSHID.008, PRCID.010).
- `report`: a view vs export permission split and a generated-report archive `report_run` (CSHID.017/018).
- `accounting`: an optional `exchangeRate` on `BusinessEvent`, passed to the journal lines (the Comptrollership BOOK rate, 2 decimals, CSHID.012-014).
- `currency`: `RateType.BOOK`.
- `messaging`: notification preferences per user and event (RMTID.034).
- `security`: all Operations permissions.

### 2.1 Dependency graph (arrows = "depends on")

```
                 booking   account   catalog   crm          (BRD-1, W2/W3)
                     \        |         /       /
                      +------ opsledger -------+ ---> accounting, subledger, workflow, bulk, messaging,
                     /    /      |      \     \          docgen, lov, alert, system, organization, currency
            cashiering remittance prodrecon adjustment commission
                                             |    \
                                          booking  catalog   (posting & recompute contracts)
```

Ports declared in `opsledger.service.port` and implemented by their owners:

| Port | Implemented by | Called by | Purpose |
|---|---|---|---|
| `ReceiptIssuer` | cashiering | remittance (commission OR per settlement batch, CSHID.007), commission (DP commission OR), remittance incentive OR | Issue an HO OR for a given payee and lines; returns OR number |
| `PaymentReapplier` | cashiering | adjustment (ADJID.009/012/013) | Un-apply / re-apply payments of an invoice after a premium change; returns the excess |
| `UnappliedSink` | cashiering | adjustment, commission (DP reinstatement), remittance (return of excluded paid AR if needed) | Create an unapplied item with a source and disposition hint |
| `DisbursementGateway` | default adapter in opsledger (in-app Disbursement queue); later the real Disbursement system | remittance, cashiering (2307 report), commission (Motor Mania pass-on) | Push a payment request or report; receive DV no. and status callbacks |
| `CollectionFeed` | default adapter = upload / screens | cashiering (check pick-up, 2307 tags), remittance (hold, special remittance), commission (DP lists, returned accounts) | Inbound and outbound Collection data |
| `InsurerFileInbox` | default = manual upload | prodrecon, remittance, commission | Insurer reports and responses |
| `FileDropPort` | default = in-system extract repository | remittance (shared drive folders per type), prodrecon | Shared-drive export (parked) |
| `NegativeAdjustmentPending` (event) | published by adjustment | remittance (exclude + notify, RMTID.020/035) | Via the `PENDING_NEG_ADJ` flag, plus a Spring event for the notification |

Events published by `opsledger`, so that siblings react without depending on each other:
- `InvoiceBooked`: cashiering re-matches pre-booked payments; prodrecon auto-matches.
- `InvoiceMovementPosted`
- `InvoiceFlagChanged`
- `InvoiceLocked` / `InvoiceUnlocked`

### 2.2 Contract asks to BRD-1 modules (being built now)

Operations cannot start before `booking` exposes the items below. Agree them with the booking agent before
wave O1.

| Owner | Contract | Needed for |
|---|---|---|
| `booking` | Domain event `InvoiceBooked(invoiceNo, arn, endorsementNo?, clientCode, insurerCode(s) + shares, currency, bookingDate, inception/expiry, riskCode, segment, AO/unit, costCenter, components {BASIC, DST, PREMIUM_TAX/VAT, LGT, FST, OTHER}, commission, vatOnCommission, wtaxRate, dp flag, cwt2Percent flag)` after commit, plus a query `BookingQueryService.invoice(invoiceNo)` for replay | opsledger feed; CSHID.020/022; RMTID; PRCID; CMRID |
| `booking` | Accounting convention for booking: Dr PR by component (client) / Cr DTIP (insurer) and Dr Commission receivable / Cr Unrealized commission and deferred output VAT (see section 5). If booking realises commission at booking time, flip the parameter `OPS_COMMISSION_REALIZATION` | Section 5 |
| `booking` | `EndorsementPostingService.post(EndorsementPosting)` for financial endorsements and cancellations (flat, flat retain DST, partial), returning the journal and the new invoice / endorsement no. Also `ServiceInvoiceService.issue(...)` and a credit variant | ADJID.001/011/014 (BRNB.076/081/100) |
| `account` | `AccountQueryService.requireByArn`, pre-booked account lookup by ARN / PN no.; DP flag (BRNB.114) | CSHID.020, PRCID.023, MKTID.011 |
| `catalog` | `PremiumCalculator` with endorsement mode (pro-rata / short-period), commission rates per insurer × product, package TSI limits, insurer panel and branches | ADJID.008/014, CMRID.007 |
| `placement` | Payment gate: CLPC payment matching (BRNB.067/068) should consume cashiering's application result through a port (`PaymentConfirmationSource`) once Operations is live. It should not become a second payment intake | CSHID.008/020, OQ12 |
| `quotation` | Expose the DP flag at quotation (MKTID.011); link a "quotation required" endorsement (ADJID.008) | MKTID.011, ADJID.008 |

## 3. Flyway allocation

Constraints: V1-V99 platform, V100-V749 insurer and finance modules, V750-V754 used (W1), V790-V799 `nbadmin`,
V800-V889 NB modules, V900-V999 demo (NB demo V980-V989). Flyway runs with `out-of-order: true`.

**Proposal: Operations schema V760-V789, demo V990-V995.**

| Range | Module | First migrations |
|---|---|---|
| V755-V759 | kept free for broking foundation extras | - |
| V760-V763 | `opsledger`, plus Operations permissions / roles / LOV types (shared seed) and platform extensions (report_run, bulk file hash, notification preferences, RateType BOOK) | `V760__ops_foundation.sql`, `V761__ops_invoice_ledger.sql` |
| V764-V769 | `cashiering` | `V764__cashiering.sql` |
| V770-V774 | `remittance` | `V770__remittance.sql` |
| V775-V779 | `prodrecon` | `V775__prodrecon.sql` |
| V780-V784 | `adjustment` | `V780__adjustment.sql` |
| V785-V789 | `commission` | `V785__commission.sql` |
| V990-V995 | demo: users and rules (V990), then one per module (V991-V995) | after NB demo V980-V989 |

Rules that make this safe:
- **No foreign keys from V76x-V78x tables to V8xx tables.** On a fresh database, V760 runs before `booking`
  (V870). Operations stores ARN, invoice no., client code, insurer code and account id as plain values,
  exactly as the architecture already does between `account` and `quotation`. Foreign keys to V1-V754 tables
  are fine: parties, branches, workflow, LOV, attachments, `acc_event_type`.
- Demo data V990+ runs after the NB demo (V988 booking), so it can seed invoices for booked demo accounts.
- The Developer Guide range table gets one new row: "V760-V789 Operations (BRD-2)".

Alternatives rejected:
- V890-V899 has only 10 versions for 6 modules.
- V1000+ for the schema would force the Operations demo above V1000 too, because demo must follow the
  schema. That breaks the "demo = V900-V999" convention.

## 4. Entities (key fields)

Money is `numeric(19,2)`, rates are `numeric(19,8)`, and the BOOK rate is stored with 2 decimals.

### 4.1 `opsledger`

- `ops_invoice` holds the booked invoice, with one row per invoice or endorsement invoice:
  - identifiers: invoice_no (unique), arn, endorsement_no, policy_no, pn_nos, client_code, assured_name,
    payor_name;
  - classification: insurer_code (lead), currency, booking_date, inception, expiry, risk_code, product_line,
    segment, ao_username, unit, branch_id, cost_center;
  - flags: dp_flag, cwt_flag;
  - statuses: remittance_status (enum from RMTID.019), payment_status (UNPAID / PARTIAL / PAID), hold_flag,
    pending_neg_adj, written_off, cancelled, estimated;
  - lock_owner, lock_reason, locked_at (RMTID.040);
  - version.
- `ops_invoice_component`: invoice_id, component (BASIC / DST / PREMIUM_TAX / VAT / LGT / FST / OTHER /
  COMMISSION / COMMISSION_VAT / WTAX / DTIP / PR2307), booked, applied, reversed, remitted, adjusted, balance.
- `ops_invoice_share`: invoice_id, insurer_code, share_pct, lead flag (ADJID.027).
- `ops_invoice_movement`: invoice_id, movement_type (BOOKED / APPLIED / UNAPPLIED / REMITTED / ADJUSTED /
  WRITE_OFF / DP_REVERSAL / CWT_RECLASS / MIN_BAL), component, amount, source_module, source_ref, ar_no /
  or_no / batch_no, value_date, journal_batch_no. This is the history of RMTID.038 and ADJID.024.
- `ops_invoice_adjustment_total`: cumulative adjustments against the original premium and DTIP (ADJID.028).
- `ops_flow_in_feed` (code, partner system, direction, transport, cron, active) and `ops_flow_in_run` (feed,
  trigger, started / ended, read / ok / failed, status, error file). `ops_flow_in_record` holds an idempotency
  key and a payload hash (BRQID.005).
- `ops_disbursement_request`: the default gateway queue. It holds type (REMITTANCE / REFUND / CWT2307 /
  PASS_ON), source ref, payee, amount, status (SENT / ACKNOWLEDGED / DV_ASSIGNED / PAID / RETURNED), dv_no and
  dates.

### 4.2 `cashiering`

- `csh_receipt_series`: branch_id, kind (AR / OR), ATP no., prefix, from_no, to_no, next_no, warn_at, active.
  Maker-checker (CSHID.006/015).
- `csh_receipt`: receipt_no, kind (AR / OR), class (PREMIUM / NON_PREMIUM / COMMISSION / SERVICE_FEE /
  PROFIT_SHARE / INCENTIVE / OTHERS), branch_id, receipt_date, payor / party code, assured, currency,
  book_rate, amount, base_amount, vat, wtax, net, mode (CASH / CHECK / BILLS_PAYMENT / TRADE / CLPC / PDC /
  DIRECT_CREDIT / ADA / CREDIT_TO_ACCOUNT), check fields (no., bank, date, clearing status and date),
  certificate_ref, source (UPLOAD job / OTC / PDC / PICKUP / SETTLEMENT batch), status (ISSUED / CANCELLED /
  REINSTATED), printed_count, journal_batch_no.
- `csh_receipt_line`: for an OR, one line per invoice or commission item (insurer, certificate, basic
  commission, VAT, WTAX, net). This supports consolidation checks (CSHID.007).
- `csh_receipt_action`: action (CANCEL / REINSTATE_FULL / REINSTATE_PARTIAL), transaction_no (`RIN-<yyyy>`),
  reason_code, reason_text, encoded fields (CSHID.005), amount, status, approved_by.
- `csh_payment`: one per received payment, from a file row, OTC or PDC. Fields: batch_id, channel, raw
  reference fields (invoice #, AR #, PN #, account ref no, EBIX ref), amount, currency, value date / time,
  late_deposit, match_category (APPLIED / PREBOOKED / UNAPPLIED_NO_MATCH / EXCESS / CANCELLED_REFERENCE),
  matched invoice, receipt_id.
- `csh_payment_batch`: handler, file name, sha256, bulk_job_id, counts per category (BRQID.006).
- `csh_application`: payment / receipt, invoice_no, component, amount, sequence (hierarchy), applied_at,
  reversed_at, reversal_ref.
- `csh_prebooked`: payment_id, key (ARN / invoice / PN), first_seen, last_rematch, resolved_at (CSHID.020).
- `csh_unapplied`: source (PAYMENT / EXCESS / ADJUSTMENT / CANCELLATION / DP_REINSTATE), amount, balance,
  currency, client, unit, tab (UNAPPLIED / MONITORING / FOR_APPROVAL / FOR_REVERSAL / CLOSED).
- `csh_disposition`: unapplied_id, type (LOV `DISPOSITION_TYPE`), target invoice / unit, amount, status,
  requested_by, approved_by, reason.
- `csh_pdc_item`: warehouse no. `PDCW-<yyyy>`, upload batch, client, invoice, check no., bank code, branch,
  maturity date, amount, status (WAREHOUSED / MATURED / APPLIED / RETURNED / REPLACED / PULLED_OUT),
  receipt_id (CSHID.008 item 4).
- `csh_pickup_request`: collection ref, account, pick-up date, requestor, status, AR no. (CSHID.009).
- `csh_cwt_tag` (Marketing reference `CWT-<yyyy>`, invoice, 2% amount, path CASH / CERTIFICATE, certificate
  no. / period, status) and `csh_cwt_batch` (validation vs CWT copies, report, routed_at) (CSHID.026/027,
  MKTID.010/013).
- `csh_minimal_balance_rule`: kind, max amount, exclusions, action.
- `csh_print_log` and `csh_search_log`.

### 4.3 `remittance`

- `rem_extraction_run`: trigger (SCHEDULED / MANUAL_INVOICE / EOD_QUEUE / SPECIAL), insurer, type, started,
  counts, file id.
- `rem_extraction_tag`: invoice_no, run, tag (EXTRACTED / UNEXTRACTED_NOT_DUE / UNEXTRACTED_DUE / RETURNED),
  exclusion reasons (ON_HOLD, PENDING_NEG_ADJ, WRITTEN_OFF, CHECK_HOLDING, PAID_AR_OVER_DTIP, NOT_POSTED).
- `rem_batch`: batch_no `RMB-<insurer>-<yyyy>`, insurer, type (WITH_INCENTIVES / NORMAL_PHP / NORMAL_USD /
  SPECIAL), processor, stage, totals (paid AR, realized commission, VAT, WTAX, DTIP, incentive, net due), DV
  no., disbursement status, schedule and payment request documents.
- `rem_batch_line`: invoice_no, endorsement no., paid AR, commission, VAT, WTAX, DTIP, incentive, net due,
  excluded, exclusion reason, excluded_by / at, restored_by / at, insurer_or_no, insurer_or_date,
  insurer_or_amount, exception status.
- `rem_hold_request`: `HLD-<yyyy>`, invoice(s), reason, hold_until, extensions, status, assigned processor.
- `rem_special_request`: `SPR-<yyyy>`, invoice, condition (claims / renewal / installment / immediate OR),
  status, requestor, approvals, pushed_at.
- `rem_incentive_rule`: insurer, product line / segment, rate, window days, basis (INCEPTION / BOOKING), from
  / to.

### 4.4 `prodrecon`

- `prc_schedule`: insurer, frequency, next run (holiday roll).
- `prc_extract`: insurer, production month or booking range, trigger, file name, file id, password-protected,
  row count, created_at, sent_at, cover-letter message id.
- `prc_extract_line`: a snapshot of the booked invoice fields in Annex IV #5.
- `prc_upload`: insurer, period, sha256, attempt no., status, bulk_job_id.
- `prc_insurer_line`: insurer-side fields plus remarks and an `in_original_extract` flag.
- `prc_match`: extract_line / insurer_line / prebooked ARN, status (MATCHED / MATCHED_WITH_DISCREPANCY /
  UNMATCHED_PREBOOKED / UNMATCHED_NO_BOOKING), discrepancy fields JSON, company concerned (LOV), instruction /
  notation, insurer feedback, marketing feedback, disposition (LOV), for_closure.

### 4.5 `adjustment`

- `adj_request`: request_no `ENR-<yyyy>`, class (FINANCIAL / NON_FINANCIAL / INTERNAL), request type (Annex V
  list), endorsement type (ADJID.002/004), reason (Annex V cancellation reasons), invoice_no, arn, policy_no,
  endorsement_ref (insurer), effective date, requesting AO, segment, remarks, stage, duplicate_override_reason,
  before / after snapshot JSON, recompute result per insurer, service invoice no., endorsement slip no.
  `ES-<yyyy>`.
- `adj_request_share`: insurer, share %, premium, commission and refund deltas.
- `adj_posting_batch`: validation batch no. `VB-<yyyy>`, lines, returned lines with reasons, posted_at.
- `adj_min_balance_upload`: file, lines, action, result (ADJID.026).

### 4.6 `commission`

- `cmr_dp_list`: branch / HO, period, file name, sha256, received_at.
- `cmr_dp_item`: invoice, policy, insurer, premium, commission, VAT, WTAX, tag (DP_FOR_CONFIRMATION /
  DP_FOR_BILLING / BILLED / APPROVED / REJECTED / COLLECTED / PR_REVERSED), sanitation result, validation
  flags, feedback reason (LOV `DP_FEEDBACK_REASON`), OR no.
- `cmr_billing`: insurer, handler, billing no. `CRB-<yyyy>`, file, sent_at, SLA due (10 working days),
  response upload, status.
- `cmr_incentive_scheme`: type (TARGET_TIERED / FIXED_PER_POLICY / EARLY_REMITTANCE), period type, tiers
  (target, rate, multiplier), fixed amounts and minimum premiums, beneficiary (BDOI / BRANCH), effective
  dates. Seeded empty (OQ39).
- `cmr_incentive_run`: scheme, period, computed lines, exclusions, status.
- `cmr_certificate_submission`: OR links, certificate (form, period, amount), scans, status (SUBMITTED /
  ACKNOWLEDGED / REJECTED), reason (CMRID.015).

## 5. Accounting events and default GL entries

The events below are seeded in `acc_event_type` by each module. **Demo rules only** (V990): the accounts in
brackets are the demo chart plus new demo accounts. The real accounts come from Comptrollership (OQ07). All
events carry `partyCode`, `costCenter` and `businessLine` (risk line), and use the BOOK rate when not in PHP
(CSHID.012-014). Source references are idempotent keys.

Demo accounts added in V990 (proposed):
- 1210 Premium Receivable - Clients, with subaccounts by component: DST, premium tax / VAT, LGT, other, basic;
- 1211 PR - CWT 2307 (PR2307);
- 1220 Commission Receivable - Insurers;
- 1225 AR Insurer;
- 1230 Incentive Receivable;
- 2210 Due to Insurers (DTIP);
- 2211 Due to Insurers - for Disbursement;
- 2215 AP Overages;
- 2216 AP Refund Payable - Clients;
- 2220 Unrealized Commission;
- 2221 Deferred Output VAT;
- 2230 Due to Branches - Incentive pass-on;
- 4101 Commission Income;
- 4110 Service Fee Income;
- 4120 Profit Share Income;
- 4130 Incentive Income;
- 6510 Minimal Balance / Write-off.

Existing accounts used: 1101 / 1111 (cash / bank), 2205 Unapplied Collections, 1602 CWT, 2504 Output VAT.

| # | Transaction | Event (source ref) | Default entry |
|---|---|---|---|
| 0 | Booking (BRD-1, for reference) | `BROKER_BOOKING` (booking) | Dr 1210.x PR by component (client) / Cr 2210 DTIP (insurer). Dr 1220 Commission receivable / Cr 2220 Unrealized commission, Cr 2221 Deferred output VAT |
| 1 | AR issued (any premium channel) | `OPS_AR_RECEIPT` (`AR:<no>`) | Dr 1111 bank / 1101 cash (`@BANK`) / Cr 2205 Unapplied collections (client) |
| 2 | Payment applied to a booked invoice, per component in the hierarchy (CSHID.022) | `OPS_PAYMENT_APPLY` (`APP:<id>`) | Dr 2205 / Cr 1210.DST, .VAT, .LGT, .OTH, .BASIC. Commission realized pro rata (if `OPS_COMMISSION_REALIZATION=ON_COLLECTION`): Dr 2220 / Cr 4101 and Dr 2221 / Cr 2504 |
| 3 | 2% CWT account paid at 98% | part of 2 + `OPS_CWT_RECLASS` (`CWT:<tag>`) when tagged (CSHID.027) | Dr 1211 PR2307 / Cr 1210.x (2% portion) |
| 4 | 2307 certificate forwarded to the insurer (DBMID.001) | `OPS_CWT_DTIP_OFFSET` (`CWT:<tag>:DTIP`) | Dr 2210 DTIP / Cr 1211 PR2307 |
| 5 | Unapplied or excess stays in 2205 | none | Already in 2205 after event 1 (BRQID.006: JEs for applied and unapplied) |
| 6 | Excess or unapplied <= PHP 10 (summary 5.f) | `OPS_EXCESS_TO_OVERAGES` (`MINX:<id>`) | Dr 2205 / Cr 2215 AP overages |
| 7 | PR minimal balance <= PHP 10, not equal to CWT, DST or whole premium (CSHID.016) | `OPS_MINIMAL_BALANCE_REVERSAL` (`MINP:<invoice>:<comp>`) | Dr 6510 / Cr 1210.x (OQ11: target account) |
| 8 | Disposition refund / reclass / transfer (CSHID.024) | `OPS_UNAPPLIED_REFUND` / `OPS_UNAPPLIED_RECLASS` | Refund: Dr 2205 / Cr 2216 (to Disbursement). Reclass / transfer: Dr 2205 (old unit) / Cr 2205 (new unit / client) |
| 9 | AR or OR cancelled (CSHID.012) | the original events with negative amounts (`...:CANCEL:<n>`) | Reverses 1 (and 2 for every application of the receipt, putting the invoices back to outstanding) |
| 10 | Reinstatement full / partial (CSHID.013) | `OPS_RECEIPT_REINSTATE` (`RIN:<no>`) plus re-application | Dr bank / Cr 2205 for the reinstated amount; then event 2 |
| 11 | Insurer non-premium payment (CSHID.021) | `OPS_AR_INSURANCE_RECEIPT` (`AR:<no>`) | Dr bank / Cr 1225 AR Insurer (insurer) |
| 12 | Remittance batch approved and pushed to Disbursement (RMTID) | `OPS_REMITTANCE` (`RMB:<batch>`) per invoice line | Dr 2210 DTIP (paid AR part) + Dr 1602 CWT (WTAX withheld by insurer on commission) / Cr 1220 Commission receivable (commission + VAT) / Cr 2211 Due to insurer - for disbursement (net due). Disbursement pays Dr 2211 / Cr bank |
| 13 | With-incentive batch (RMTID.023) | `OPS_REMIT_INCENTIVE` (`RMB:<batch>:INC`) | Dr 2211 / Cr 4130 Incentive income (+ output VAT). Incentive OR issued through `ReceiptIssuer` |
| 14 | Commission OR per settlement batch (CSHID.007) | `OPS_OR_ISSUE` class COMMISSION (`OR:<no>`) | No cash. Deferred VAT made due if not done in 2: Dr 2221 / Cr 2504 (per rule) |
| 15 | Service fee / profit share / other OR (CSHID.002) | `OPS_OR_ISSUE` (`OR:<no>`, `@INCOME` by OR type) | Dr bank + Dr 1602 CWT / Cr 4110 / 4120 / 4130 / other + Cr 2504 output VAT |
| 16 | Cancellation, not paid (ADJID) | booking `EndorsementPostingService` (reversal of 0) | Dr 2210, Dr 2220, Dr 2221 / Cr 1210.x, Cr 1220 (DST kept for "retain DST") |
| 17 | Cancellation or decrease, paid but not remitted | 16 + `PaymentReapplier` (negative 2) | Dr 1210.x / Cr 2205, giving an unapplied item for disposition (ADJID.009/012/013). Realized commission reversed: Dr 4101 / Cr 2220 |
| 18 | Cancellation or decrease, already remitted | 16/17 + `OPS_AR_INSURER_SETUP` (`ADJ:<req>:ARI`) | Dr 1225 AR Insurer / Cr 2210 DTIP (negative DTIP reclassified); the commission retained is reversed against 1220 per rule (OQ07) |
| 19 | Positive financial endorsement | booking posting (as 0, new invoice / endorsement no.) | as 0; service invoice |
| 20 | Service invoice / credit on an income change (ADJID.014 addendum) | booking `ServiceInvoiceService` | BIR document only; the GL effect is in 16-19 |
| 21 | Write-off 10-100 file (ADJID.026) | `OPS_WRITE_OFF` (`WO:<invoice>`) | Debit balance: Dr 6510 / Cr 1210.x. Credit balance ("credit"): Dr 2205 / Cr 4190 other income (rule) |
| 22 | DP PR reversal after confirmed collection (MKTID.012) | `OPS_DP_PR_REVERSAL` (`DP:<invoice>`) | Dr 2210 DTIP / Cr 1210.x (client paid the insurer directly) |
| 23 | DP commission collected (CMRID) | `OPS_OR_ISSUE` class COMMISSION + `OPS_DP_COMMISSION_COLLECT` | Dr bank + Dr 1602 CWT / Cr 1220; realization Dr 2220 / Cr 4101, Dr 2221 / Cr 2504 |
| 24 | DP reinstatement (CSHID.004 b) | `OPS_DP_REINSTATE` | Reverse of 22 |
| 25 | Target incentive earned from insurer (CMRID.005) | `OPS_INCENTIVE_ACCRUE` (`INC:<run>`) | Dr 1230 / Cr 4130 |
| 26 | Motor Mania pass-on to branches (CMRID.006) | `OPS_INCENTIVE_PASS_ON` (`INCP:<run>`) | Dr 4130 (or a pass-on expense) / Cr 2230 Due to branches, then Disbursement |

The sub-ledger records:
- client PR as DEBIT open items per invoice;
- DTIP, AR Insurer and commission receivable as open items of the insurer party;
- unapplied balances as CREDIT items of the client.

Every event records its `ops_invoice_movement` in the same transaction.

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in O0; granted in V760)

| Permission | Used for |
|---|---|
| `OPS_VIEW` | Operations home and read-only invoice 360 |
| `OPS_REPORT_VIEW`, `OPS_REPORT_EXPORT` | View vs download / print of Operations reports (CSHID.017/018) |
| `CASH_RECEIPT` | Create AR / OR, OTC payment entry |
| `CASH_CANCEL`, `CASH_REINSTATE` | Request cancellation / reinstatement |
| `CASH_APPROVE` | Approve cancel / reinstate / dispositions (TL / TH) |
| `CASH_APPLY` | Manual application, re-match, pre-booked re-run |
| `CASH_UPLOAD` | Payment-file bulk handlers, PDC upload |
| `CASH_DISPOSITION`, `CASH_DISPOSITION_APPROVE` | Unapplied workbench |
| `CASH_SERIES_MANAGE` | Receipt series master (authorised with `MASTER_AUTHORIZE`) |
| `CASH_PRINT` | Batch printing |
| `CWT_TAG` | Marketing 2307 tagging and reinstatement requests |
| `CWT_PROCESS` | Cashiering 2307 validation and report |
| `DISB_PROCESS` | Disbursement queue: acknowledge, DV no., release (default adapter) |
| `REMIT_EXTRACT`, `REMIT_PROCESS`, `REMIT_EXCLUDE`, `REMIT_APPROVE`, `REMIT_OR_UPLOAD` | Remittance |
| `HOLD_REQUEST`, `HOLD_APPROVE` | Marketing hold requests |
| `SPECIAL_REMIT_REQUEST`, `SPECIAL_REMIT_APPROVE` | Special remittance |
| `RECON_PROCESS`, `RECON_SEND` | Production reconciliation |
| `ADJ_REQUEST` | Marketing endorsement request and endorsement slip (ADJID.015 / MKTID.008) |
| `ADJ_PROCESS`, `ADJ_APPROVE`, `ADJ_POST` | Adjustment processor, TL approval, batch posting |
| `COMMREC_PROCESS`, `COMMREC_APPROVE`, `INCENTIVE_MANAGE` | Commission receivables, incentive schemes |
| `BIR_CERT_SUBMIT`, `BIR_CERT_ACK` | CMRID.015 (Operations / Comptrollership) |
| `FLOWIN_MANAGE` | Feed configuration and re-runs (IT) |

### 6.2 Roles (V760) and demo users (V990, password `Brokerverse@2026`)

| Role | Persona (BRD annex) | Key permissions | Demo user |
|---|---|---|---|
| `CASHIER` | Cashiering HO (11) / branch (5) | CASH_RECEIPT, CASH_CANCEL, CASH_REINSTATE, CASH_APPLY, CASH_UPLOAD, CASH_DISPOSITION, CASH_PRINT, CWT_PROCESS | `cashier`, `cashbr` (branch: AR only) |
| `CASHIER_TL` | Cashiering TL / TH | + CASH_APPROVE, CASH_DISPOSITION_APPROVE, CASH_SERIES_MANAGE | `cashtl` |
| `REMIT_PROCESSOR` | Remittance processor (4) | REMIT_EXTRACT, REMIT_PROCESS, REMIT_EXCLUDE, REMIT_OR_UPLOAD | `remit` |
| `REMIT_TL` | Remittance TL / TH | + REMIT_APPROVE, SPECIAL_REMIT_APPROVE, WORK_ASSIGN | `remittl` |
| `RECON_HANDLER` | Prod Recon handler (4) | RECON_PROCESS, RECON_SEND | `recon` |
| `ADJUSTMENT` (exists, BRD-1) | Adjustment processor (6) | + ADJ_PROCESS, ADJ_POST | `adjust` (exists) |
| `ADJUSTMENT_TL` | Adjustment TL | + ADJ_APPROVE | `adjtl` |
| `COMMREC_HANDLER` | Commission handler / processor | COMMREC_PROCESS, BIR_CERT_SUBMIT | `commrec` |
| `COMMREC_TL` | Commission TL / TH | + COMMREC_APPROVE, INCENTIVE_MANAGE | `commtl` |
| `MKT_COLLECTION` | Marketing Collection (HO / branches) | CWT_TAG, HOLD_REQUEST, SPECIAL_REMIT_REQUEST, ADJ_REQUEST | `mktcoll` |
| `MKT_TL` (exists) | Marketing TL / UH | + HOLD_APPROVE, ADJ_REQUEST | `mkttl` (exists) |
| `COMPTROLLERSHIP` | Comptrollership | BIR_CERT_ACK, OPS_REPORT_VIEW (+ existing FIN roles for accounting rules) | `comptrol` |
| `DISBURSEMENT` | Disbursement | DISB_PROCESS | `disb` |

Every Operations role also gets OPS_VIEW, WORK_VIEW and OPS_REPORT_VIEW. OPS_REPORT_EXPORT goes to TL, TH and
handlers. The final access matrix is open (OQ48).

## 7. Workflows (seeded by each module; `wf_stage` / `wf_transition`)

In the diagrams, `(perm)` is the permission of a transition. Stage owner permissions drive the queues and the
notifications.

`OPS_RECEIPT_ACTION` (cashiering; cancel / reinstate, CSHID.001-005; approval assumed, OQ06):
```
REQUESTED(CASH_CANCEL|CASH_REINSTATE) --submit--> FOR_APPROVAL(CASH_APPROVE) --approve--> POSTED [terminal]
FOR_APPROVAL --return(reason)--> REQUESTED ; REQUESTED --withdraw--> WITHDRAWN [terminal]
```

`OPS_DISPOSITION` (cashiering; CSHID.024/025; tabs = stage groups):
```
UNAPPLIED --assign_disposition(CASH_DISPOSITION)--> MONITORING
MONITORING --submit(CASH_DISPOSITION)--> FOR_APPROVAL(CASH_DISPOSITION_APPROVE) --approve--> IN_PROCESS --complete(system)--> COMPLETED
MONITORING --complete(system; types without approval)--> COMPLETED
FOR_APPROVAL --return(reason)--> MONITORING ; MONITORING --update--> MONITORING
COMPLETED --mark_reversal(reason, CASH_DISPOSITION)--> FOR_REVERSAL --approve_reversal(CASH_DISPOSITION_APPROVE)--> UNAPPLIED
```

`OPS_CWT_2307` (cashiering; CSHID.026/027, MKTID.010/013, DBMID.001):
```
TAGGED(CWT_TAG) --receive(CWT_PROCESS)--> VALIDATING --validate--> REPORT_POSTED --route--> WITH_DISBURSEMENT(DISB_PROCESS)
  --release_to_insurer--> RELEASED [terminal]
VALIDATING --return(reason)--> TAGGED
```

`OPS_REMITTANCE` (remittance batch; RMTID.009-011/019/029/036):
```
REVIEW_IN_PROCESS(REMIT_PROCESS) --submit--> FOR_APPROVAL(REMIT_APPROVE) --approve--> APPROVED(pushed to Disbursement)
  --dv_assigned(system)--> PARTIALLY_REMITTED | FULLY_REMITTED --insurer_or_uploaded(REMIT_OR_UPLOAD)--> OR_RECEIVED [terminal]
REVIEW_IN_PROCESS --hold(reason)--> ON_HOLD --release--> REVIEW_IN_PROCESS
REVIEW_IN_PROCESS/FOR_APPROVAL --return(reason)--> RETURNED [terminal; lines re-tagged RETURNED and re-extractable]
```

`OPS_HOLD` (remittance; MKTID.002-007, RMTID.021):
```
DRAFT(HOLD_REQUEST) --submit--> FOR_APPROVAL(HOLD_APPROVE) --approve--> ACTIVE --assign(HOLD_APPROVE)--> ACTIVE
ACTIVE --extend(HOLD_REQUEST)--> FOR_APPROVAL ; ACTIVE --request_cancel--> CANCEL_FOR_APPROVAL --approve--> RELEASED
ACTIVE --expire(system, RMTID.021) / release(HOLD_REQUEST)--> RELEASED [terminal]
```

`OPS_SPECIAL_REMIT` (remittance; MKTID.009, RMTID.030/033):
```
REQUESTED(SPECIAL_REMIT_REQUEST) --validate(system checks)--> FOR_APPROVAL(SPECIAL_REMIT_APPROVE) --approve--> IN_PROCESS_REMITTANCE
  --pushed(system)--> PUSHED_TO_DISBURSEMENT [terminal] ; FOR_APPROVAL --reject(reason)--> REJECTED [terminal]
```

`OPS_RECON` (prodrecon; one case per insurer and production month):
```
EXTRACTED(RECON_PROCESS) --send(RECON_SEND)--> SENT_TO_INSURER --feedback_uploaded--> RECONCILING
  --close(RECON_PROCESS)--> CLOSED ; RECONCILING --for_closure(insurer confirmed)--> CLOSED
SENT_TO_INSURER --resend--> SENT_TO_INSURER
```

`OPS_ENDORSEMENT` (adjustment; ADJID.001-010/015/018):
```
DRAFT(ADJ_REQUEST) --submit--> FOR_VALIDATION(ADJ_PROCESS) --validate--> FOR_APPROVAL(ADJ_APPROVE)*
  --approve--> FOR_POSTING(ADJ_POST) --post(batch)--> POSTED [terminal]
FOR_VALIDATION/FOR_APPROVAL/FOR_POSTING --return(reason, ADJID.005/007)--> RETURNED --resubmit--> FOR_VALIDATION
DRAFT/RETURNED --cancel--> CANCELLED [terminal]
* types without approval go FOR_VALIDATION --validate--> FOR_POSTING; extension with additional premium always needs approval (ADJID.010)
Guards: invoice not locked by remittance (ADJID.001); negative types set PENDING_NEG_ADJ from submit to post / cancel.
```

`OPS_DP_BILLING` (commission; CMRID.008-013):
```
DP_FOR_CONFIRMATION(COMMREC_PROCESS) --confirm--> DP_FOR_BILLING --bill(send to insurer)--> AWAITING_INSURER [SLA 10 working days]
  --insurer_approved--> APPROVED --collected(system: OR issued)--> COLLECTED --pr_reversed(system)--> CLOSED
AWAITING_INSURER --insurer_rejected(reason)--> RETURNED_TO_COLLECTION [terminal]
```

`OPS_BIR_CERT` (commission; CMRID.015):
```
SUBMITTED(BIR_CERT_SUBMIT) --acknowledge(BIR_CERT_ACK)--> ACKNOWLEDGED ; SUBMITTED --reject(reason)--> REJECTED --resubmit--> SUBMITTED
```

## 8. Integrations to park (seam only)

| Item | BRD | Seam built now | Question |
|---|---|---|---|
| Collection system: check pick-up, 2307 tags, hold data, special remittance, DP lists, returned DP accounts, refunds | BRQID.004, CSHID.009/026, RMTID.021/030, CMRID.001/009 | `CollectionFeed` port; screens and uploads for Marketing Collection | OQ01, OQ45 |
| Disbursement system: remittance payment requests, refunds, 2307 report, pass-on; DV no. and status back | RMTID.002/019/034, DBMID.001 | `DisbursementGateway` with an in-app Disbursement queue (`ops_disbursement_request`) | OQ02 |
| Accounting system (if BDOI GL stays external) | BRQID.004 | Journals in the BrokerVerse GL; export port `AccountingExport` (not built) | OQ01 |
| Marketing / Claims systems | BRQID.004, MKTID.009 | `MarketingFeed` / `ClaimsFeed` ports, no adapter | OQ46 |
| Bank / collection files: Bills Payment FS01, Trade, CLPC, Direct Credit, PDC (PMS) | CSHID.008 | Bulk handlers with configurable layouts (layout table per handler); files uploaded by users | OQ03, OQ04 |
| Insurer channels: remittance schedule out, OR schedule in, production report in / out, DP billing and responses | RMTID.012, PRCID.008/009, CMRID.009/012 | E-mail through messaging with protected files; inbound by upload (`InsurerFileInbox`) | OQ22, OQ29, OQ38 |
| Shared drive folders (remittance types, DP lists) | RMTID.001, CMRID.001 | In-system extract repository; `FileDropPort` | OQ17, OQ38 |
| Comptrollership exchange-rate table | CSHID.012-014 | Rate type BOOK maintained in the currency master (upload) | OQ08 |
| Bank clearing status for checks | RMTID.017/018 | Cleared = bank reconciliation match or manual clear with reason | OQ20 |

## 9. Bulk handlers (`bulk.service.BulkImportHandler`)

| Code | Module | Permission | Purpose |
|---|---|---|---|
| `PAY_BILLS`, `PAY_TRADE`, `PAY_CLPC`, `PAY_DIRECT_CREDIT` | cashiering | CASH_UPLOAD | Payment files: rows create payments, then the matching engine runs; AR issued for all rows; run report with applied, unapplied, prebooked, failed (CSHID.008, BRQID.006) |
| `PAY_PDC` | cashiering | CASH_UPLOAD | Daily PDC list into the warehouse (PDCW numbers) |
| `COMMISSION_PAYMENT` | cashiering | CASH_UPLOAD | Commission payment details from Collection, feeding CSHID.007 ORs |
| `CWT_TAGS` | cashiering | CWT_TAG | Bulk 2307 tagging by Marketing |
| `REMIT_INSURER_OR` | remittance | REMIT_OR_UPLOAD | Insurer-populated remittance schedule with OR date / no.; exception report (RMTID.012/013/016) |
| `HOLD_ACCOUNTS` | remittance | HOLD_REQUEST | Bulk hold tagging |
| `RECON_INSURER_REPORT` | prodrecon | RECON_PROCESS | Insurer production report / matched file; duplicate block; auto-match (PRCID.009/010/022) |
| `ADJ_BATCH` | adjustment | ADJ_POST | Batch cancellation / adjustment requests (ADJID.006) |
| `MINIMAL_BALANCE_FILE` | adjustment | ADJ_POST | 10.00-100.00 write-off / credit file (ADJID.026) |
| `DP_LIST` | commission | COMMREC_PROCESS | DP lists from HO and branches, with naming convention check (CMRID.001) |
| `DP_INSURER_RESPONSE` | commission | COMMREC_PROCESS | Insurer approval / rejection with reasons (CMRID.009) |

## 10. Jobs, alerts and parameters

| ManagedJob | Module | Default cron (UTC) | BRD |
|---|---|---|---|
| `OPS_INVOICE_FEED_REPLAY` | opsledger | manual | Rebuild or replay the invoice ledger from booking |
| `PREBOOKED_REMATCH` | cashiering | every 2 h, plus manual | CSHID.020 |
| `PAYMENT_AUTOMATCH` | cashiering | after each upload + hourly | CSHID.008 item 3 |
| `PDC_MATURITY` | cashiering | daily 00:30 | CSHID.008 item 4e |
| `MINIMAL_BALANCE_SWEEP` | cashiering | daily 20:00 | CSHID.016 |
| `REMITTANCE_EXTRACTION` | remittance | off-peak daily 12:00 (20:00 PHT) | RMTID.001/003/005 |
| `HOLD_EXPIRY` | remittance | daily 00:15 | RMTID.021 |
| `PRODUCTION_EXTRACT` | prodrecon | per insurer schedule, holiday roll | PRCID.001 |
| `RECON_AUTOMATCH` | prodrecon | manual until frequency known | PRCID.025 |
| `ADJ_DAILY_REPORT` | adjustment | daily 10:00 (18:00 PHT) | ADJID.016 |
| `DP_FEEDBACK_SLA` | commission | daily | CMRID.011 |

Alerts (`alt_exception_code`):
- `RECEIPT_SERIES_LOW`
- `OPS_FLOW_IN_FAILED`
- `PREBOOKED_AGEING`
- `REMIT_EXTRACTION_FAILED`
- `REMIT_PAIDAR_OVER_DTIP`
- `DP_FEEDBACK_OVERDUE`
- `INCENTIVE_EXCLUSION`
- `RECON_UPLOAD_DUPLICATE`
- `ADJ_OVER_BASELINE`

Parameters (`sys_parameter`):

| Parameter | Default |
|---|---|
| `REMIT_CHECK_HOLD_DAYS` | 3 |
| `REMIT_PAIDAR_OVER_DTIP_MODE` | EXCLUDE (OQ19) |
| `RECON_TOLERANCE` | 1.00 |
| `MIN_BALANCE_AUTO_MAX` | 10.00 |
| `MIN_BALANCE_FILE_RANGE` | 10.00-100.00 |
| `CMR_FEEDBACK_WORKING_DAYS` | 10 |
| `OPS_COMMISSION_REALIZATION` | ON_COLLECTION |
| `CWT_APPLICATION_PERCENT` | 98 |
| `PRODRECON_FILE_PATTERN` | - |
| `REMIT_FILE_PATTERN` | - |
| `OPS_BOOK_RATE_TYPE` | BOOK |

LOV types (V760):
- Receipts and payments: `OR_TYPE`, `AR_CLASS`, `RECEIPT_CANCEL_REASON`, `REINSTATEMENT_REASON`, `DISPOSITION_TYPE`
- Remittance: `REMITTANCE_TYPE`, `REMIT_EXCLUSION_REASON`, `REMIT_RETURN_REASON`, `HOLD_REASON`, `SPECIAL_REMIT_CONDITION`
- Production reconciliation: `RECON_COMPANY_CONCERNED`, `RECON_DISPOSITION`
- Adjustment: `ENDORSEMENT_TYPE`, `ENDORSEMENT_REQUEST_TYPE`, `CANCELLATION_REASON`, `ENDORSEMENT_DOC_TYPE`
- Commission: `DP_FEEDBACK_REASON`, `INCENTIVE_EXCLUSION_RULE`
- General: `OPS_EXTERNAL_LINK`

The lists from the BRD are seeded exactly as written (`ops_data.py`).

## 11. Reports (ReportDefinition per report; category "Operations")

| Module | Codes |
|---|---|
| cashiering (Annex II) | `CSH-APPLIED-PREM`, `CSH-APPLIED-COMM`, `CSH-PDC-WAREHOUSE`, `CSH-MINBAL-EXCESS`, `CSH-CANCELLED-OR`, `CSH-CANCELLED-AR`, `CSH-CHECK-PICKUP`, `CSH-PRIORITY-POSTED`, `CSH-UNAPPLIED-COMM-MANCOM`, `CSH-UNAPPLIED-COMM-YTD`, `CSH-MINBAL-PREMIUM`, `CSH-MINBAL-COMMISSION`, `CSH-DAILY-CASH-REC`, `CSH-ADVANCE-PAYMENT`, `CSH-PAYMENT-REVERSAL`, `CSH-DIRECT-PAYMENT`, `CSH-REINSTATEMENT-MON`, `CSH-REAPPLICATION`, `CSH-CERT-OF-PAYMENT` (also as a PDF document), `CSH-REINSTATEMENT`, `CSH-CWT`; plus `CSH-AR-OUTSTANDING` (per client per invoice with ageing, KC 4.c), `CSH-BATCH-RUN` (BRQID.006), `CSH-2307-TXN` (CSHID.027) |
| remittance (Annex III) | `REM-TRACKER`, `REM-SPECIAL-REGISTER`, `REM-SCHEDULE-NORMAL`, `REM-SCHEDULE-SPECIAL`, `REM-DTIP-SUMMARY`, `REM-DTIP-DETAIL`, `REM-SCHEDULE-INCENTIVE`, `REM-REMITTED-BATCH`; plus `REM-PAIDAR-OVER-DTIP`, `REM-OR-EXCEPTION`, `REM-EXCLUDED`, `REM-HOLD` |
| prodrecon (Annex IV) | `PRC-SUMMARY`, `PRC-UNMATCHED-LOC`, `PRC-UNMATCHED-AO`, `PRC-DISPOSITION`, `PRC-REGISTER` (variants plain / insurer / marketing / both feedback), `PRC-UNBOOKED`, `PRC-EXTRACT-LOG`, `PRC-UNMATCHED-FEEDBACK`, `PRC-EARLY-INCENTIVE` |
| adjustment | `ADJ-DAILY`, `ADJ-VALIDATION-LIST`, `ADJ-REGISTER`, `ADJ-AGING`, `ADJ-MINBAL-FILE`; documents: endorsement slip, validation slip |
| commission | `CMR-COMMISSION-RECEIVABLE`, `CMR-PRODUCTION-YEARLY`, `CMR-DP-STATUS`, `CMR-INCENTIVE`, `CMR-FEEDBACK-SLA`, `CMR-BIR-CERT` |

Report layouts that are not specified in the annex are built with the obvious columns and flagged "layout to
confirm" (OQ42).

## 12. Screens (sidebar section **Operations**, after the NB sections)

Every record page follows the broking pattern: a header with the reference chip and status, then the
`WorkflowPanel`, then tabs Details / Documents / History, then the permitted actions. Lists use `DataTable`
with saved quick filters, empty and loading states, and toasts.

- **Operations home** (BRQID.003):
  - one card per section with live counts: payments to match, pre-booked ageing, unapplied by tab, batches
    for approval, holds expiring, recon cycles open, endorsements for posting, DP awaiting insurer (SLA red
    / amber);
  - pinned sections;
  - external links (LOV).
- **Cashiering**:
  - **Receive payment (OTC)**: a two-pane form. Payor and invoice lookup (ARN, invoice, policy, PN) sit on the
    left. On the right, a live application preview by component (DST -> VAT -> LGT -> Other -> Basic), a 98%
    CWT badge, the excess shown as unapplied, and a currency with BOOK rate chip.
  - **Payment uploads**: `BulkUploadWizard` per file type, then a run summary with five coloured tiles
    (applied / unapplied / prebooked / excess / failed), download of the report and "reprocess failed".
  - **Receipts**: a search with the ten criteria (CSHID.010) and a receipt page. Actions: Print, Cancel
    (reason dialog), Reinstate (full / partial form with the encoded fields). Tabs: Applications, Journal,
    History.
  - **Unapplied Payments** workbench with the tabs Unapplied / Monitoring / For Approval / For Reversal
    (CSHID.025). Actions are bulk-selectable. The disposition drawer shows the type-specific fields.
  - **Pre-booked** queue (ageing, re-run now).
  - **PDC Warehouse** (maturity calendar view).
  - **Check Pick-up queue** (filter by date, "Print ARs" batch).
  - **Batch Print** (filter by location / insurer, preview list, progress, retry failures).
  - **BIR 2307**: Marketing tagging, Cashiering validation with a CWT-copy checklist, report and route.
  - **Receipt Series** master with a remaining-count gauge.
- **Remittance**:
  - **Extraction workbench**: insurer / type pickers, run now or schedule, and run results with tags.
  - **Batch page**: a grid with exclusion checkboxes. Amounts are read-only (addendum). An "Exclusions" side
    panel shows the reason, user and restore action. The totals strip shows paid AR, commission, VAT, WTAX,
    DTIP, incentive and net due. A "Preview submission" dialog comes before submit. Documents: schedule and
    payment request.
  - **Insurer OR upload** with the exception report.
  - **Holds** and **Special Remittance** (Marketing and Remittance views).
  - **DTIP status**.
- **Production Reconciliation**:
  - **Cycles** board per insurer / month, as stage columns.
  - **Reconciliation workbench**: status buckets as tabs. A discrepancy row expands to a side-by-side
    field diff with the tolerance highlighted. Company-concerned and disposition dropdowns are edited inline.
  - **Uploads** history with attempts.
  - **Schedules**.
- **Adjustments**:
  - **New request** wizard: select invoices (multi), pick the type (the form adapts), recompute preview per
    insurer with before / after columns and the service-invoice impact, attachments, duplicate warning with an
    override reason.
  - **Request page** with slip and validation slip.
  - **Posting batches** (review, return, post).
  - **Minimal balance file**.
- **Commission Receivables**:
  - **DP intake** board: branch submission tracker, per-branch status for the period.
  - **Validation and sanitation** list with the rule results.
  - **Billing** per insurer with an SLA countdown.
  - **Insurer responses**.
  - **Incentive schemes** (tier editor) and runs.
  - **BIR certificate submissions** (Comptrollership acknowledge / reject).
- **Invoice 360** (opsledger), reachable from everywhere by ARN or invoice no. (ADJID.024, RMTID.026):
  - a component table with booked / applied / remitted / adjusted / balance;
  - status chips: payment, remittance, hold, lock, DP, written-off;
  - a movement timeline, receipts, batches and adjustments.

Frontend folders:
- `features/operations` (home + invoice 360), owned by O0;
- `features/cashiering`, `features/remittance`, `features/prodrecon`, `features/adjustment`,
  `features/commission`, each owning its `help.ts`.

## 13. Build-wave plan

Prerequisite: BRD-1 `booking` delivers the contract of section 2.2. Operations can start in parallel with the
end of BRD-1 W3 if the event record is agreed first.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **O0** (1 agent, short) | Ops foundation | `opsledger` complete (ledger, feed from booking, flags / locks, invoice 360 API, flow-in framework, ports with default adapters, Disbursement queue, events). The platform extensions: bulk TXT / hash / outcomes / reprocess, report view / export split and archive, BusinessEvent rate, RateType BOOK, notification preferences. **All** Operations permissions, roles and LOV types (V760). Operations nav section + home + invoice 360 UI. Demo users (V990). Developer Guide range row. Pre-registered help sections, crons in `application.yml`, CONFIGURATION.md entries for every O1 job | `opsledger/**`, `V760-V763`, `V990`, `security/domain/Permission.java`, `bulk/**`, `report/**` (small), `accounting/service/BusinessEvent.java`, `currency/domain/RateType.java`, `messaging/**` (small), `frontend/src/features/operations/**`, `navigation/modules.ts`, `help/helpContent.ts`, `application.yml`, docs | Invoice ledger built from demo bookings; ports compile with default adapters; `mvn verify` green |
| **O1-A** | Cashiering | Everything in `cashiering` (the largest area: receipts, series, uploads, matching, application, unapplied / dispositions, PDC, pick-up, 2307, minimal balance, printing, 24 reports). Implements `ReceiptIssuer`, `PaymentReapplier`, `UnappliedSink` | `cashiering/**`, `V764-V769`, `V991`, `features/cashiering/**` | AR / OR end to end with postings; upload -> match -> apply -> report |
| **O1-B** | Remittance | `remittance` including holds, special remittance and incentives; uses `ReceiptIssuer` (a stub bean from O0 until A merges) | `remittance/**`, `V770-V774`, `V992`, `features/remittance/**` | Extraction -> exclusion -> approve -> Disbursement queue -> insurer OR upload |
| **O1-C** | Adjustment | `adjustment`; uses the booking posting / service-invoice contracts and `PaymentReapplier` | `adjustment/**`, `V780-V784`, `V994`, `features/adjustment/**` | Financial / non-financial / internal requests, batch post, slips, validation list |
| **O1-D** | Prod recon + Commission | `prodrecon` and `commission` (two small, independent modules; one agent) | `prodrecon/**`, `commission/**`, `V775-V779`, `V785-V789`, `V993`, `V995`, `features/prodrecon/**`, `features/commission/**` | Recon cycle with matching; DP billing to collection, PR reversal, incentive engine (empty rules) |
| **O2** (1 agent) | Integration and hardening | Cross-module E2E test (booking -> AR -> application -> remittance -> insurer OR -> recon -> cancellation after remittance -> AR Insurer -> DP commission OR), Operations dashboard polish, demo storyline data, module guide `docs/modules/OPERATIONS.md`, fit/gap workbook refresh | tests + docs + demo | Full `mvn verify` / `npm run verify`; walkthrough |

Rules for parallel work:
- One Flyway range per agent.
- No edits to other agents' packages.
- Shared files (Permission enum, nav, help registry, `application.yml`) are edited **only in O0**.
- Each module has its own `*ApiIT` smoke class instead of editing the shared `ApiSmokeIT`, which avoids merge
  conflicts.
- Port stubs (default adapters) are `@ConditionalOnMissingBean`, so each O1 agent's tests run without the
  others.

## 14. What depends on information BDOI has not given (build the seam, park the content)

| Topic | Requirements | Built now | Parked content | Q |
|---|---|---|---|---|
| External systems (Collection, Disbursement, Marketing, Claims, Accounting) | BRQID.004/005, CSHID.009/026/027, DBMID.001, RMTID.030/034, CMRID.001/009 | Ports, flow-in framework, in-app queues and uploads | Transports and payloads | OQ01, OQ02, OQ45, OQ46 |
| Payment file layouts | CSHID.008 | Configurable layout table and five handlers with the BRD field lists | Exact record layouts, encryption | OQ03, OQ04 |
| Receipt series / BIR ATP | CSHID.001/002/006/015 | Series master | Formats and ATP ranges | OQ05 |
| GL accounts and default entries | CSHID.012-016, ADJID.011, MKTID.012, remittance | Event types and demo rules | Comptrollership rules | OQ07 |
| Exchange-rate table | CSHID.012-014 | Rate type BOOK, 2 decimals | Source and upload of rates | OQ08 |
| Minimal balance rules | CSHID.016, ADJID.026 | Rule table and parameters | Final thresholds and targets | OQ11 |
| Remittance schedule, naming, shared drive | RMTID.001/003/005 | Scheduler and repository | Schedule, names, folders | OQ17, OQ18 |
| Paid AR > DTIP behaviour | RMTID.014 | Parameter CAP / EXCLUDE | Decision | OQ19 |
| Early remittance incentive rates | RMTID.023, PRCID.028 | Rule table | Rates | OQ23 |
| Recon frequency, template, naming, keys, automatch timing | PRCID.001-009/022-027 | Schedules, templates, parameters | Values | OQ29, OQ30 |
| Company concerned / disposition lists | PRCID.016/038/039 | LOVs (empty) | Values | OQ31 |
| Package TSI limits, co-insurance model | ADJID.008/027 | Uses the catalog limits and share lines | Limits | OQ33 |
| Incentive schemes (No Touch, Top Up, Motor Mania) | CMRID.003/005/006 | Scheme engine, empty rules | Targets, tiers, amounts | OQ39 |
| DP list sources and billing formats | CMRID.001/009 | Upload handlers, naming check | Folders, layouts | OQ38 |
| Report layouts without fields | CSHID.023 (reports 9-18, 20-21), RMTID.039 (#8), CMRID.004/014 | Draft layouts | Final layouts | OQ42 |
| Access matrix | all | Proposed roles | Final matrix | OQ48 |

## 15. What depends on other BRDs or BRD-1 modules

| Dependency | Requirements affected | Note |
|---|---|---|
| BRD-1 `booking`: invoice event, components, commission / VAT / WTAX, service invoice, endorsement posting | Nearly all (opsledger feed), ADJID.001/011/014, CSHID.020/022 | Blocking; agree section 2.2 before O1 |
| BRD-1 `account`: ARN, pre-booked lookup, DP flag (BRNB.114) | CSHID.020, PRCID.023, MKTID.011 | Available in W2 |
| BRD-1 `catalog`: calculator endorsement mode, commission rates, package limits, insurer branches | ADJID.008/014, CMRID.007 | Endorsement / short-period mode may need an addition |
| BRD-1 `placement`: CLPC payment matching overlaps cashiering intake | CSHID.008/020 | Decide on a single payment intake (OQ12) |
| BRD-1 `quotation`: "quotation required" on TSI increase; DP flag at quotation | ADJID.008, MKTID.011 | Link by reference only |
| Collection / Marketing Collection BRD (not received) | MKTID.001-013, CSHID.009/026, RMTID.021/030, CMRID.001/009 | Screens in Operations until then (OQ45) |
| Disbursement / Accounting BRD (not received) | RMTID.002/011/019/034, DBMID.001, CSHID.026/027 | In-app Disbursement queue until then (OQ02) |
| Claims BRD | BRQID.004, MKTID.009 (claims special remittance) | Port only (OQ46) |
| Renewal BRD | Special remittance "renewal" condition | Condition value only |

## 16. Risks

1. **The booking contract is late or different.** Mitigation: O0 builds `opsledger` against a published
   event record, plus a replay job. The feed is the only place that knows the booking shape.
2. **The accounting convention is unconfirmed** (commission realization, 2% CWT, AR Insurer). Mitigation:
   everything is an event with configurable rules, plus the parameter `OPS_COMMISSION_REALIZATION`.
   Comptrollership signs off the rule set before UAT.
3. **Scope creep through the MKTID items.** Mitigation: the Marketing Collection screens are minimal intake
   forms behind the `CollectionFeed` port.
4. **Performance at mid-month and month-end peaks (15th / 30th)**, with a response time under 5 s.
   Mitigation: matching, extraction and printing run as asynchronous jobs; `ops_invoice` has indexes on
   invoice_no, arn, insurer, status and flags; searches use trigram indexes.
