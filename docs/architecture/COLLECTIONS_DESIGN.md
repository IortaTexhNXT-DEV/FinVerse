# iNXT BrokerVerse - BDOI Collections (BRD-4, CLXN) Build Design

Status: **proposal for review**. This design extends `docs/architecture/BROKING_ARCHITECTURE.md` and `docs/architecture/OPERATIONS_DESIGN.md`, which stay binding, and the Developer Guide. It does not change them, except for the contract changes listed in section 9.

Requirements baseline: [`BDOI_CLXN_BRD_SPEC.md`](../requirements/BDOI_CLXN_BRD_SPEC.md): 64 requirement IDs, of which BRCLXN.061-064 are draft, and questions CQ01-CQ25. Every class, migration and screen cites its BRD ID in Javadoc or a comment, for example `BRCLXN.049`.

## 1. Design principles

1. **Collections is a BrokerVerse module, not an interface.** The CMS BRD describes a separate system fed from EBIX / QPS. In BIBS, the booked invoice and its PR balance by component already live in `opsledger`, so Collections reads the ledger directly. It never keeps a second copy of the money.
2. **Collections monitors; it does not post money.**
   - Collections owns the follow-up of receivables:
     - the worklist;
     - assignment;
     - efforts, promises and installments;
     - dispositions and escalation;
     - billing statements.
   - It posts **no journal and no ledger movement**.
   - Every money effect happens in the module that already owns it:
     - Cashiering: application, unapplied items, 2307, pick-up;
     - Commission: DP reversal, commission receivable, incentives;
     - Adjustment: cancellation.
   - This keeps one place per GL rule (OPERATIONS_DESIGN principle 4).
3. **Hand-offs go through `opsledger` ports.**
   - Collections depends on `opsledger` and on the platform / BRD-1 modules only; it never calls `cashiering` or `commission` directly.
   - It **implements** the existing `CollectionFeed` port, so Cashiering and Commission receive Collections dispositions through the feeds they were already designed to consume.
   - It **calls** two new ports that Cashiering implements (unapplied items).
   - The graph stays acyclic, and the Cashiering, Commission and Collections agents can work in parallel.
4. **What changes because Collections is in-app.** The `COLLECTION_*` feeds keep their codes, idempotency and run log, but their transport becomes `IN_APP`. Uploads remain possible as a fallback, and nothing is simulated.
5. **Signed scope first.**
   - BRCLXN.001-060 are baselined.
   - BRCLXN.061-064 (draft, CQ01) are designed as Commission extensions and built only after BDOI confirms them.
6. **Exports are expensive** (Operations Head caveat, p.93). Lists are paged on the server. Exports run as asynchronous jobs with a row cap and a separate permission.

## 2. Modules

| Module | Purpose | BRD IDs | Depends on | Flyway (demo) |
|---|---|---|---|---|
| `collections` (new, package `com.iortatechnxt.brokerverse.collections`, tables `clx_*`) | Collection worklist per invoice (refresh from the ledger), assignment and reassignment, collection efforts, PR collector dispositions (category, owner, Operations action), promises, installment plans, escalation rules and cases, billing statements (SOA), collector dispositions on unapplied payments and application requests, the in-app `CollectionFeed` adapter (outbox / inbox), scheduled files and reports, field-level change log, Collections home | BRCLXN.001-058, 060 | opsledger (query, ports, events), account, booking (read), issuance (read), catalog (sales organisation), crm (client), workflow, messaging, bulk, report, docgen, lov, alert, system, organization | V1000-V1005 (V1900-V1901), see §3 decision |
| `commission` (Operations, being built) | + CR billing gate on PR confirmation; + receivable type (regular / incentive); + incentive campaigns and service-invoice billing; + commission refunds; + collectible portion on mixed payments | BRCLXN.059, 061-064 | unchanged (opsledger, catalog, booking `ServiceInvoiceService`) | commission range V785-V789 (V995) |
| `cashiering` (Operations, being built) | + implements `UnappliedDirectory` and `UnappliedDispositionRequests`; + pulls `COLLECTION_CWT2307` / `COLLECTION_CHECK_PICKUP` from `CollectionFeed` | BRCLXN.030-036, 040 (execution side) | unchanged | cashiering range V764-V769 (V991) |
| `opsledger` (built) | + two ports and a default method on `CollectionFeed`; + event `CollectionFeedReady` | - | unchanged | none (code only) |

### 2.1 What belongs where (decision)

| Capability | Collections | Cashiering | Commission | Remittance / Adjustment |
|---|---|---|---|---|
| Outstanding PR list, threshold, filters, net PR breakdown (A) | **Owns** | - | - | - |
| Payment intake, AR / OR, matching, application by component, PDC, check pick-up queue and AR printing | Reads status only | **Owns** (CSHID.001-022) | - | - |
| Unapplied payment item, its Cashiering tabs, disposition workflow `OPS_DISPOSITION`, refunds, reclass, transfers, minimal balance sweep | Collector disposition + "request application" (BRCLXN.030-048), read-only list | **Owns** (CSHID.016/024/025) | - | - |
| "For application to invoice" | Validates the invoice, records the request, writes the audit text file (041/042) | Executes the application (creates the disposition APPLY_TO_OTHER_INVOICE) | - | - |
| BIR 2307: tagging "PR 2307 for reversal" (MKTID.010/013), monthly and weekly file | **Owns** the tag (disposition) and the files | Validation vs CWT copies, 2307 report, PR2307 postings, route to Disbursement (CSHID.026/027, DBMID.001) | - | - |
| DP: "DP PR for reversal" tag, monthly and weekly files | **Owns** the tag and files | - | DP list intake, validation, billing, PR reversal (CMRID, MKTID.012) | - |
| Check pick-up request (p.40) | Disposition with action CHECK_PICKUP | Pick-up queue, AR at pick-up (CSHID.009) | - | - |
| CR billing gate, collectible portion, refunds, incentive campaigns (059, 061-064) | Shows status on the item | - | **Owns** | Adjustment publishes commission deltas (built) |
| Holds, special remittance, send schedule, endorsement slip (MKTID.001-009) | Links only (deep link from the item) | - | - | **Stay as built** (remittance `HoldService`, `SpecialRemittanceService`; adjustment slip) |
| Installments, promises, escalation, reassignment, SOA (049-058) | **Owns** | - | - | - |

### 2.2 `CollectionFeed` and the `COLLECTION_*` feeds: replacement plan

The port stays (`opsledger/service/port/CollectionFeed.java`). Collections declares `InAppCollectionFeed` (`@Service`), which replaces the default `ManualCollectionFeed` through `@ConditionalOnMissingBean`:
- `transport()` returns `IN_APP`.
- `pending(companyId, feedCode)` returns the PENDING rows of `clx_outbox` for the feed.
- `send(companyId, feedCode, items)` writes `clx_inbox` rows and a flow-in run.
- The new `acknowledge(...)` marks outbox rows TAKEN (see section 9).

| Feed (V761) | Direction | Before | After (Collections in BIBS) | Producer / consumer |
|---|---|---|---|---|
| `COLLECTION_CWT2307` | IN to Operations | Marketing upload / Cashiering tagging screen | **IN_APP**. The outbox item is created when a PR disposition with action CWT2307_REVERSAL reaches owner OPERATIONS (certificate received), or at once for the cash path. The key is `CWT:<invoice>:<disposition id>`; the fields are the p.59 set | Collections -> Cashiering (`csh_cwt_tag`) |
| `COLLECTION_DP_LIST` | IN | HO / branch files (CMRID.001) | **IN_APP**. The outbox item is created on the disposition "DP PR for reversal"; the monthly file is the audit copy. The key is `DP:<invoice>:<disposition id>` | Collections -> Commission (`cmr_dp_item`) |
| `COLLECTION_CHECK_PICKUP` | IN | upload | **IN_APP**. Created from a disposition with action CHECK_PICKUP (pick-up date, address, contact) | Collections -> Cashiering (`csh_pickup_request`) |
| `COLLECTION_DP_RETURNED` | OUT from Operations | CSV in the extract repository | **IN_APP**. `send()` creates `clx_inbox` rows. Each row reopens the item with the disposition "DP returned by insurer", the reason and a timestamp, and notifies the handler (CMRID.009) | Commission -> Collections |
| `COLLECTION_REFUND` | OUT | CSV | **IN_APP**. `send()` records the refund on the collector's unapplied history (BRCLXN.040) | Cashiering -> Collections |
| `COLLECTION_HOLD`, `COLLECTION_SPECIAL_REMIT` | IN | upload | **Unchanged** (MANUAL_UPLOAD). The CLXN BRD has no holds or special remittances; the remittance screens stay the Marketing intake | Marketing -> Remittance |
| `COLLECTION_COMMISSION_PAYMENT` | IN | upload | **Unchanged** (commission payment details come from CRU, not Collections) | CRU -> Cashiering |

`MarketingFeed` remains without an adapter. The Marketing Diary (ISYS) is a separate question (CQ20).

### 2.3 Dependency graph (arrows = "depends on")

```
      booking  account  issuance  catalog  crm           (BRD-1, read)
          \       |        |        /      /
           +---- opsledger (ledger, ports, events) ----+
          /           |            \                    \
   cashiering     commission     remittance/adjustment   collections (new)
   implements:    consumes:                              implements CollectionFeed (IN_APP)
   UnappliedDirectory, COLLECTION_DP_LIST                calls UnappliedDirectory,
   UnappliedDispositionRequests;  sends DP_RETURNED      UnappliedDispositionRequests
   consumes COLLECTION_CWT2307 / CHECK_PICKUP
```

## 3. Flyway allocation

> **Allocation decision (integration, 2026-09-25):** V890–V899 went to BRD-5 Accounting / Disbursement, which needs the whole
> block for four modules. Collections uses **schema V1000–V1009** and **demo V1900–V1909** (Developer Guide range table).
> Mapping of the files below: V890→V1000, V891→V1001, V892→V1002, V893→V1003, V894→V1004, V895→V1005 (V1006–V1009 kept
> free); demo V1900–V1901 unchanged. Everywhere else in this document, read the V89x numbers through this mapping.


The Developer Guide ranges and the free versions were checked in `backend/src/main/resources/db/{migration,demo}`:
- Schema versions used: V1-V99, V100-V749, V750-V754, V760-V762, V770-V771, V780, V790, V800-V880.
- Demo versions used: V900-V992 and V994, with V993 / V995 held by Operations.
- Product Maintenance claims V755, V813-V817, V821, V831, V871 and V996-V997, and holds V818-V819 for its follow-ups.

**Proposal: schema V890-V895 (V896-V899 kept for follow-ups); demo V1900-V1901.**

| Version | Owner (wave) | Content |
|---|---|---|
| `V890__collections_foundation.sql` | C0 | Grants of the Collections permissions to new and existing roles, LOV types, `sys_parameter` rows, alert codes, notification events, workflow `CLX_ESCALATION` (`wf_stage`, `wf_transition`), and `update ops_flow_in_feed set transport = 'IN_APP'` for `COLLECTION_CWT2307`, `COLLECTION_DP_LIST`, `COLLECTION_CHECK_PICKUP`, `COLLECTION_DP_RETURNED`, `COLLECTION_REFUND` |
| `V891__collections_worklist.sql` | C1-A | `clx_item`, `clx_item_balance`, `clx_assignment`, `clx_assignment_rule`, `clx_disposition`, `clx_effort`, `clx_field_change`, `clx_outbox`, `clx_inbox` |
| `V892__collections_promises_installments.sql` | C1-B | `clx_installment_plan`, `clx_installment`, `clx_promise`, `clx_billing_statement`, `clx_billing_statement_line` |
| `V893__collections_escalation.sql` | C1-B | `clx_escalation_rule`, `clx_escalation`, `clx_escalation_item` |
| `V894__collections_unapplied.sql` | C1-C | `clx_unapplied_disposition`, `clx_application_request` |
| `V895__collections_files.sql` | C1-A | `clx_scheduled_file` (report code, period, file id, available_from), document template rows (SOA) |
| `V896-V899` | - | Kept free |
| `db/demo/V1900__demo_collections.sql` | C1-A | Demo assignment rules, handlers (`clxhandler`, `clxtl`, `clxuh`), items for the booked demo invoices, dispositions of every kind |
| `db/demo/V1901__demo_collections_plans.sql` | C1-B | Demo installment plan (quarterly), promises (one kept, one broken), an escalation, one SOA |

Why this range is safe:
- V890-V899 is the free tail of the "broking business modules" range (V800-V899), where Collections belongs.
- V890 runs after V761 (`ops_flow_in_feed`, `ops_invoice`) and after V870 (booking) on a fresh database.
- The rule "no foreign keys to V8xx" holds anyway: invoice no., ARN, client code and usernames are plain values, as in Operations.
- Foreign keys go only to V1-V754 tables (users, branches, workflow, LOV, attachments).

Why the demo is at V1900:
- The V900-V999 demo block is nearly full: V993 / V995 are held by Operations, V996 / V997 by Product Maintenance, and V998 / V999 are the last two free versions.
- The demo must run after the Operations demo (V990-V995), because it needs demo invoices in the ledger.
- Proposed convention for the Developer Guide: **"V1900-V1999: demo data of modules added after BRD-3 (`db/demo` only), sub-range per BRD: V1900-V1909 Collections."** V1000-V1899 stay free for future schema ranges of later BRDs (Accounting, Disbursement, ACSL). Every demo version is then above every schema version, so a demo migration never runs before its tables.
- V998 / V999 are left to whoever needs a last V9xx slot.

Coordination: the Accounting / Disbursement analysis may also propose V890s. The owner of the Developer Guide range table decides, and the fallback for Collections is V1000-V1009 (schema) with the same demo block.

## 4. Entities (key fields)

Money is `numeric(19,2)`. Every table has `company_id`, the audit columns and `version`.

### 4.1 Worklist

- `clx_item`: one row per ledger invoice that has ever been listed (BRCLXN.001, 022).
  - Keys: `invoice_no` (unique per company), `arn`, `policy_no`, `policy_year`, `client_code`, `assured_name`.
  - Classification copied from the ledger: `insurer_code`, `segment`, `sales_unit`, `unit_head_username` (resolved), `ao_username`, `branch_id`, `booking_date`, `inception_date`, `expiry_date`, `currency`, `dp_flag`, `cwt_flag`, `invoice_category` (REGULAR / DIRECT_BILL).
  - Figures: `net_outstanding` (snapshot), `outstanding_pr2307`, `aging_days`, `aging_bracket`.
  - `status`:
    - OPEN;
    - COMPLETED (zero or below the threshold);
    - EXCLUDED_CANCELLED (BRCLXN.010);
    - CREDIT (negative, not type C, CQ04).
  - Dates: `listed_on`, `completed_on`, `last_refreshed_at`.
  - Current pointers: `current_handler`, `current_disposition_id`, `category` (A / B / C), `tagging_owner` (MARKETING / OPERATIONS), `last_effort_at`.
  - Flags: `promise_status`, `escalation_level`, `installment_overdue`.
  - Soft lock: `editing_by`, `editing_since` (NFR record lock).
  - Indexes: (company, status, segment, sales_unit), (company, current_handler, status), (company, client_code), `arn`, `aging_days`.
- `clx_item_balance`: `item_id`, component (from `LedgerComponent`), booked, adjusted, applied, written_off, outstanding. This is the snapshot of the net PR breakdown (BRCLXN.046); the live figures are read from the ledger on the account page.
- `clx_assignment`: `item_id`, `handler_username`, `kind` (PERMANENT / TEMPORARY), `valid_from`, `valid_to`, `reason`, `assigned_by`, `reverted_at`, `bulk_ref` (BRCLXN.052).
- `clx_assignment_rule`: `priority`, criteria (segment, sales_unit, client_code, amount_from / to, aging_from / to), `handler_username`, `active`.
- `clx_disposition`: `item_id`, `disposition_code` (LOV `CLX_PR_DISPOSITION`), `category`, `tagging_owner`, `ops_action` (NONE / CWT2307_REVERSAL / DP_REVERSAL / CHECK_PICKUP / CANCEL_REQUEST), `remarks`, `effective_on`, payload JSON (pick-up date / address, certificate no., etc.), `outbox_id`, `superseded_by`. The table is append-only (BRCLXN.021-023).
- `clx_effort`: `item_id`, `effort_code` (LOV `CLX_EFFORT_CODE`), `effort_at`, `channel`, `contact_person`, `remarks`, `bulk_ref`.
- `clx_field_change`: `entity`, `entity_id`, `field`, `old_value`, `new_value`, `username`, `changed_at`, `source_ip`, `bulk_ref` (BRCLXN.043, NFR audit).
- `clx_outbox`: `feed_code`, `idempotency_key` (unique per feed), `fields` JSON, `status` (PENDING / TAKEN / CANCELLED), `taken_at`, `taken_run_no`, `source_disposition_id`.
- `clx_inbox`: `feed_code`, `idempotency_key`, `fields` JSON, `received_at`, `item_id`, `processed_at`.

### 4.2 Promises, installments and billing

- `clx_installment_plan`: `item_id` (or `arn` for multi-year), `frequency` (LOV `CLX_BILLING_FREQUENCY`), `source` (POLICY_YEARS / GENERATED / MANUAL), `first_due`, `count`, `total`, `status` (BRCLXN.053/058, CQ15).
- `clx_installment`: `plan_id`, `seq`, `due_date`, `amount`, `invoice_no` (for multi-year policy-year invoices), `paid_amount` (allocated from APPLIED movements, oldest due first), `status` (NOT_DUE / DUE / OVERDUE / PAID / PARTIAL), `overdue_since`.
- `clx_promise`: `item_id`, `installment_id` (nullable), `promised_date`, `promised_amount`, `recorded_by`, `status` (OPEN / KEPT / PARTIALLY_KEPT / BROKEN / CANCELLED), `evaluated_at`, `actual_paid`, `actual_date` (BRCLXN.055).
- `clx_billing_statement`: `soa_no` (`SOA-<yyyy>`), `client_code`, `arn`, `cycle_from`, `cycle_to`, `due_date`, `total`, `document_id` (docgen), `generated_by`, `sent_at`; and `clx_billing_statement_line` (invoice / installment, coverage period, amount, paid, balance) (BRCLXN.058/060).

### 4.3 Escalation

- `clx_escalation_rule`: `code`, `name`, `basis` (AGING_FROM_BOOKING / AGING_FROM_INCEPTION / NO_COMMITMENT_BY_DAY / BROKEN_PROMISES_COUNT / INSTALLMENT_OVERDUE_DAYS / AMOUNT_OVER), `threshold`, filters (segment, unit, product line, amount range), `target_level` (TL / UH / SECTION_HEAD / USER), `target_username`, `notify`, `active`, effective dates. Maker-checker through `MASTER_AUTHORIZE` (BRCLXN.049).
- `clx_escalation`: case number `ESC-<yyyy>`, `kind` (AUTO / MANUAL), `rule_code`, `raised_by`, `target_username`, `level`, `reason`, `workflow_case_id`, `status` mirrored from the workflow.
- `clx_escalation_item`: one row per invoice of the escalation (BRCLXN.050 AC 1).

### 4.4 Unapplied payments (collector side)

- `clx_unapplied_disposition`: `unapplied_ref` (Cashiering key), `disposition_code` (LOV `CLX_UPP_DISPOSITION`), `invoice_no` (mandatory when the LOV attribute `requires_invoice` is set, validated by `CLX_INVOICE_NO_PATTERN` and ledger existence), `remarks`, `request_id`. Append-only (BRCLXN.031/033/040/047/048).
- `clx_application_request`: `unapplied_ref`, `invoice_no`, `amount` (optional, default the full unapplied balance), `requested_by`, `requested_at`, `cashiering_ref` (from the port), `status` (SENT / ACCEPTED / REJECTED / APPLIED), `file_run_no` (041 audit file) (BRCLXN.030/032).

Records of Cashiering's unapplied items are **not copied**. The list (034-036) reads them through `UnappliedDirectory` and joins the collector disposition by `unapplied_ref`.

## 5. Accounting events and GL entries

Collections posts **no** accounting event. The table shows where the money effects of Collections actions are posted. The demo rules follow OPERATIONS_DESIGN section 5, and the real accounts come from Comptrollership (OQ07).

| # | Trigger in Collections | Posted by | Event | Default entry |
|---|---|---|---|---|
| 1 | "For application to invoice" accepted by Cashiering | cashiering | `OPS_PAYMENT_APPLY` (existing #2) | Dr 2205 Unapplied / Cr 1210.x PR by component |
| 2 | Disposition "PR 2307 for reversal", certificate validated | cashiering | `OPS_CWT_RECLASS` / `OPS_CWT_DTIP_OFFSET` (existing #3/#4) | Dr 1211 PR2307 / Cr 1210.x; then Dr 2210 DTIP / Cr 1211 |
| 3 | Disposition "DP PR for reversal", validated and collection confirmed | commission | `OPS_DP_PR_REVERSAL` (existing #22) | Dr 2210 DTIP / Cr 1210.x |
| 4 | Partial direct-to-insurer payment confirmed (BRCLXN.064, draft) | commission | `OPS_DP_PR_REVERSAL` with the partial amount | As 3, for the confirmed portion only |
| 5 | Incentive campaign billed (BRCLXN.061/062, draft) | commission via booking `ServiceInvoiceService` | `OPS_INCENTIVE_BILL` (new) | Dr 1230 Incentive Receivable / Cr 4130 Incentive Income (Other Income), Cr 2504 Output VAT; WTAX on collection: Dr bank + Dr 1602 CWT / Cr 1230 (`OPS_OR_ISSUE` class INCENTIVE) |
| 6 | Commission refund from a negative adjustment on a billed / collected line (BRCLXN.063, draft) | commission | `OPS_COMMISSION_REFUND` (new) | Dr 4101 Commission Income (or 2220 if unrealized) and Dr 2504 / Cr 2217 Commission Refund Payable - Insurer (new demo account); paid through Disbursement |
| 7 | Installment plan, SOA, promise, escalation, disposition without Operations action | - | none | Monitoring only (BRCLXN.060: billing creates no receivable) |

Sub-ledger: incentive receivables are open items of the insurer party (type INCENTIVE), apart from commission receivables (BRCLXN.061), and commission refunds are credit items of the insurer (BRCLXN.063).

## 6. Security

### 6.1 Permissions (added to `security.domain.Permission` in C0, granted in V890)

| Permission | Used for |
|---|---|
| `CLX_VIEW` | Collections home, worklist, account page (read-only), completed collections |
| `CLX_WORK` | Efforts, promises, PR dispositions, remarks on own / assigned items; soft lock |
| `CLX_BULK_UPDATE` | Bulk update in the grid and the upload handler (BRCLXN.051) |
| `CLX_ESCALATE` | Manual escalation (050) |
| `CLX_ESCALATION_HANDLE` | Act on escalations (TL / UH / Section Head) |
| `CLX_ASSIGN` | Reassignment and assignment rules (052) |
| `CLX_UNAPPLIED_WORK` | Collector disposition and application request on unapplied payments (030-033) |
| `CLX_BILLING` | Installment plans and SOA generation (053/058) |
| `CLX_SETUP` | Threshold, escalation rules, billing frequencies, invoice pattern, Collections LOVs (007, 017, 037, 049) |
| `CLX_EXPORT` | Export of lists and download of files (caveat p.93) |
| `CLX_REPORT_VIEW` | Scheduled files and Collections reports (view) |
| `CLX_AUDIT_VIEW` | Audit log view / extract (044) |

### 6.2 Roles (V890) and demo users (V1900, password `Brokerverse@2026`)

| Role | Persona (p.43-46) | Key permissions | Demo user |
|---|---|---|---|
| `MKT_AO` (exists) | Marketing AO | CLX_VIEW, CLX_WORK, CLX_ESCALATE, CLX_UNAPPLIED_WORK, CLX_REPORT_VIEW, CLX_EXPORT | existing AO users |
| `MKT_TL` (exists) | Marketing Team Lead | + CLX_ESCALATION_HANDLE, CLX_ASSIGN, CLX_BULK_UPDATE, CLX_BILLING | `mkttl` (exists) |
| `MKT_HANDLER` (new) | Marketing Handler | as MKT_AO + CLX_BULK_UPDATE | `mkthandler` |
| `MKT_COLLECTION` (exists, Operations) | Collection Handler | CLX_VIEW, CLX_WORK, CLX_BULK_UPDATE, CLX_ESCALATE, CLX_UNAPPLIED_WORK, CLX_BILLING, CLX_REPORT_VIEW, CLX_EXPORT (+ existing CWT_TAG, HOLD_REQUEST, SPECIAL_REMIT_REQUEST, ADJ_REQUEST) | `mktcoll` (exists), `clxhandler` |
| `CLX_TL` (new) | Collection Team Lead | + CLX_ESCALATION_HANDLE, CLX_ASSIGN | `clxtl` |
| `MKT_SECTION_HEAD` (new) | Section Head Corporate / Retail | + CLX_SETUP (Corporate per p.44; Retail without setup), CLX_AUDIT_VIEW | `clxuh` |
| `UNAPPLIED_HANDLER` (new, CQ10) | Unapplied Payment Handler | CLX_VIEW, CLX_UNAPPLIED_WORK, CLX_REPORT_VIEW | - |
| `PROCESSOR` (exists) | Processing Unit | CLX_VIEW, CLX_WORK (limited to the disposition "no / missing policy number" by LOV attribute `allowed_roles`) | existing |
| `CASHIER`, `CASHIER_TL` (exist) | Operations Cashiering | CLX_VIEW, CLX_REPORT_VIEW, CLX_EXPORT | existing |
| `DISBURSEMENT`, `COMPTROLLERSHIP` (exist), `ACSL` (new, view) | BDO Insure viewers | CLX_VIEW, CLX_REPORT_VIEW (+ CLX_EXPORT for Disbursement) | existing |
| `APP_SUPPORT` (new) / `BUSINESS_ADMIN` (exists) / `DCO` (new) | Application support, Admin, DCO | CLX_SETUP, CLX_AUDIT_VIEW, `FLOWIN_MANAGE` (sync view), job run view | - |

The final matrix is open (OQ48). The single-role NFR is CQ23.

## 7. Workflows (`wf_stage` / `wf_transition`, seeded in V890)

`CLX_ESCALATION` (BRCLXN.049/050/055):
```
RAISED(system | CLX_ESCALATE) --route(system: rule target / chosen)--> WITH_TL(CLX_ESCALATION_HANDLE)
WITH_TL --acknowledge--> IN_ACTION --resolve(reason)--> RESOLVED [terminal]
WITH_TL/IN_ACTION --escalate_further--> WITH_UH(CLX_ESCALATION_HANDLE) --acknowledge--> IN_ACTION
WITH_TL/WITH_UH --return_to_handler(instruction)--> RETURNED --resubmit--> WITH_TL
any open stage --auto_close(system: item COMPLETED)--> RESOLVED
```
- SLA per stage from the rule (hours); `WorkSlaAlertCheck` raises `CLX_ESCALATION_OVERDUE`.
- Notifications `CLX_ESCALATED` go to the target and the handler.

Reassignment, dispositions and promises have no approval (the BRD asks for none). They are audited in `clx_field_change` and on the timeline.

## 8. Jobs, parameters, LOVs, alerts

| ManagedJob | Default cron (UTC; PHT = UTC+8) | Purpose | BRD |
|---|---|---|---|
| `CLX_DAILY_REFRESH` | `0 15 14 * * *` (22:15 PHT, after the 22:00 EOD) | Incremental refresh of `clx_item` from the ledger: new, updated, completed, excluded; unit head, aging, bracket; installment allocation and overdue flags; temporary-assignment revert; default assignment by rule | 001-015, 046, 052, 053 |
| `CLX_PROMISE_CHECK` | `0 45 14 * * *` | Evaluate promises due yesterday or earlier | 055 |
| `CLX_ESCALATION` | `0 0 15 * * *` | Apply the escalation rules; one case per account and rule (idempotent key rule + item + period) | 049 |
| `CLX_DAILY_FILES` | `0 30 14 * * *` | Outstanding PR List, Full Production Report | 045 |
| `CLX_APPLICATION_FILE` | `0 0 21 * * *` (05:00 PHT) | "For Application To Invoice" text file of the previous day's requests, through `FileDropPort` (FS04) | 041/042 |
| `CLX_WEEKLY_FILES` | `0 30 14 * * FRI` | Weekly DP PR / PR 2307 for reversal per unit per branch; available Monday 08:00 | 028/029 |
| `CLX_MONTHLY_FILES` | `0 0 21 * * *` (runs daily at 05:00 PHT; generates only on the first working day) | Monthly DP PR / PR 2307 for reversal for the previous month | 024-027 |

A balance listener on `InvoiceMovementPosted` (after commit, new transaction) refreshes the balance snapshot of one item between runs (BRCLXN.015).

Parameters (`sys_parameter`):

| Parameter | Default |
|---|---|
| `CLX_MIN_BALANCE_THRESHOLD` | 10.00 (to confirm, CQ03) |
| `CLX_AGING_BASIS` | BOOKING |
| `CLX_AGING_BRACKETS` | 0-30,31-45,46-60,61-90,91-120,121+ |
| `CLX_INVOICE_NO_PATTERN` | `^(I\d{8}\|BI-.+)$` |
| `CLX_PROMISE_GRACE_DAYS` | 0 |
| `CLX_EXPORT_MAX_ROWS` | 50000 |
| `CLX_EDIT_LOCK_MINUTES` | 15 |
| `LOGIN_MAX_FAILED_ATTEMPTS` (platform, NFR) | 3 |

LOV types:
- `CLX_PR_DISPOSITION`, with attributes `category`, `tagging_owner`, `ops_action`, `allowed_roles`;
- `CLX_UPP_DISPOSITION`, with attributes `requires_invoice`, `cashiering_action`;
- `CLX_EFFORT_CODE`;
- `CLX_BILLING_FREQUENCY`;
- `CLX_ESCALATION_REASON`.

The values are empty or placeholders until BDOI sends them (CQ08). The seeds that the process implies are marked "(to confirm)":
- "DP PR for reversal";
- "PR 2307 for reversal";
- "For check pick-up";
- "For application to invoice";
- "Cancel account";
- "Coordinate further";
- "Request bank AO assistance".

Alerts (`alt_exception_code`):
- `CLX_REFRESH_FAILED`;
- `CLX_ESCALATION_OVERDUE`;
- `CLX_FILE_NOT_PUBLISHED`;
- `CLX_OUTBOX_STALE` (PENDING for more than 24 h).

Notification events:
- `CLX_ESCALATED`;
- `CLX_REASSIGNED`;
- `CLX_PROMISE_BROKEN`;
- `CLX_DP_RETURNED`;
- `CLX_FILE_READY`.

## 9. Impact on built and in-progress modules (contract changes)

| Module (state) | Change | Concrete contract | Owner / wave |
|---|---|---|---|
| `opsledger` (built) | `CollectionFeed`: add `default void acknowledge(Long companyId, String feedCode, Collection<String> keys) {}`. `ManualCollectionFeed` keeps the no-op | Backward compatible; the consumers call it after `FlowInContext.accept` succeeds | C0 |
| `opsledger` (built) | New port `UnappliedDirectory` | `Page<UnappliedView> open(Long companyId, UnappliedFilter f, Pageable p)`; `Optional<UnappliedView> find(String unappliedRef)`; `List<UnappliedEvent> history(String unappliedRef)`. `UnappliedView` = ref, payment date, payment file name, transaction no., amount, balance, type, payor, bank code, check no., reference, matched client / invoice, Cashiering tab, disposition status. Default adapter `EmptyUnappliedDirectory` | C0 declares, O1-A implements |
| `opsledger` (built) | New port `UnappliedDispositionRequests` | `DispositionTicket request(DispositionRequest r)` with `r = (companyId, unappliedRef, action APPLY_TO_INVOICE / REFUND / RECLASS / TRANSFER, invoiceNo, amount, requestedBy, source "COLLECTIONS", sourceRef)`. Idempotent on sourceRef. Default `HandoffDispositionRequests` = open hand-off for team `CASH_DISPOSITION` (the same pattern as `HandoffUnappliedSink`) | C0 declares, O1-A implements |
| `opsledger` (built) | New event `CollectionFeedReady(companyId, feedCode)` in `OpsLedgerEvents`, published by Collections after commit | Consumers may pull at once instead of waiting for their schedule | C0 |
| `opsledger` data (V761) | `ops_flow_in_feed.transport` set to `IN_APP` for five feeds (V890 update statement) | No code change; the Interfaces screen shows the new transport | C0 |
| `cashiering` (being built, O1-A) | Implement `UnappliedDirectory` and `UnappliedDispositionRequests`. A request creates or updates a `csh_disposition` (source COLLECTIONS, type from `cashiering_action`) in MONITORING, so the approval rules of `OPS_DISPOSITION` stay. Add `csh_disposition.source` and `source_ref` columns (V764-V769) | Needed for BRCLXN.030-036 | O1-A |
| `cashiering` (being built) | `COLLECTION_CWT2307` and `COLLECTION_CHECK_PICKUP` consumers: a flow-in pull job (every 15 min + on `CollectionFeedReady`) calls `collectionFeed.pending`, accepts by key, then `acknowledge`. The Marketing "BIR 2307 tagging" screen (MKTID.013) becomes a Cashiering **correction** screen under `CWT_TAG`; tags normally arrive from Collections | Replaces the planned upload path; `CWT_TAGS` bulk handler kept as fallback | O1-A |
| `commission` (being built, O1-D) | `COLLECTION_DP_LIST` consumer (as above); `COLLECTION_DP_RETURNED` via `collectionFeed.send` (already planned). The CMRID.001 branch-file naming check applies only to the upload fallback | OQ38 answer | O1-D |
| `commission` (being built) | BRCLXN.059 gate: `PremiumConfirmation` on `cmr_dp_item` / CR line (source LEDGER_PAID / DP_CONFIRMED, date); CR SOA generation refuses unconfirmed lines | Signed requirement | O1-D |
| `commission` (being built) | BRCLXN.061-064 (draft, after CQ01): receivable type, campaigns on `cmr_incentive_scheme` (criteria ref to `cat_incentive_criteria`, window basis, exclusions), billing via `ServiceInvoiceService`, `cmr_commission_refund`, partial DP confirmation; events `OPS_INCENTIVE_BILL`, `OPS_COMMISSION_REFUND` | In the commission range V785-V789 | O1-D or a later C2-D |
| `remittance` (built) | None. Holds and special remittances stay. The Collections account page deep-links to `/remittance/holds?invoice=` | - | - |
| `adjustment` (built) | None required. Commission deltas are already posted as `ADJUSTED` movements (`CommissionAdjuster`), which Commission reads for BRCLXN.063. Collections shows `PENDING_NEG_ADJ` on the item | - | - |
| Disbursement queue (built, `opsledger` `DisbursementQueueService`) | None. Refunds (Cashiering) and commission refunds (draft 063) use `DisbursementGateway` as designed | - | - |
| GL / accounting | Two new event types (draft 061-063) with demo rules; no change for the signed scope | Comptrollership rule sign-off | O1-D |
| `catalog` (built, PM extending) | Unit Head on the sales unit: `cat_sales_unit.head_username` + `SalesOrganisationService.unitHead(companyId, unitCode)` | Migration `V819__catalog_sales_unit_head.sql`: V818-V819 are held by Product Maintenance, so agree with the PM owner. Fallback: `clx_unit_head` in V891 | **Deferred to C1-A** (P1-A is changing `catalog` during C0; section 14) |
| `security` (built) | Lockout threshold from a parameter (NFR 3 attempts); Collections permissions | `AppUser.MAX_FAILED_ATTEMPTS` becomes `SecurityProperties.maxFailedAttempts` | C0 |
| `report` (built) | Scheduled file generation with availability: `ReportArchiveService.archiveGenerated(code, params, file, availableFrom)` | Used by the Collections file jobs | C0 |

## 10. Integrations to park (seam only)

| Item | BRD | Seam built now | Question |
|---|---|---|---|
| Legacy EBIX / QPS open items and disposition history at go-live | BRCLXN.013-015, 022 | One-time bulk handler `CLX_LEGACY_ITEMS` (invoice no., balance, disposition, remarks, handler) | CQ07 |
| BDOI file server FS04 (application-to-invoice file, report drops) | BRCLXN.041/042, NFR 15.06-15.08 | `FileDropPort` to the in-system extract repository | OQ17, CQ12 |
| ISYS Marketing Diary | p.40-42 | Read-only panel fed by `MarketingFeed` (adapter absent) or a Collections form | CQ20 |
| E-mail of SOAs to clients | BRCLXN.058 | `messaging` outbox with a protected PDF | CQ18 |
| SSO / Windows credentials | NFR 2.08-2.09 | Platform JWT login (BRD-1 Q42) | Q42 |
| Automated Payment Processing (legacy APS) | BRCLXN.036/041 | Replaced by Cashiering; the text file is kept for audit | CQ12 |

## 11. Reports and screens

Reports (`ReportDefinition`, category "Collections"; view and export permissions; exports asynchronous):
- `CLX-OUTSTANDING-PR`, `CLX-FULL-PRODUCTION` (daily);
- `CLX-DP-FOR-REVERSAL`, `CLX-PR2307-FOR-REVERSAL` (monthly, weekly per unit / branch);
- `CLX-APPLICATION-TO-INVOICE` (daily text);
- `CLX-UNAPPLIED-LIST`, `CLX-COMPLETED-COLLECTIONS`, `CLX-INVOICES-WITH-DISPOSITION`;
- `CLX-ESCALATIONS`, `CLX-BROKEN-PROMISES`, `CLX-INSTALLMENTS-DUE`, `CLX-REASSIGNMENTS`, `CLX-AUDIT-LOG`.

The fields are those of spec section 7. The legacy-only fields are mapped in CQ22 and shown blank until then.

Screens: group **Finance**, section **Collections**, listed first, before Cashiering. The route is `/collections` and the frontend folder is `frontend/src/features/collections`. The screens follow `docs/design/BDO_UX_GUIDELINES.md`: tokens only, `DataTable` with saved quick filters, a record header with a reference chip and status, then tabs.
- **Collections home** (dashboard, CQ21). Tiles:
  - my open accounts;
  - due / overdue installments;
  - promises due today;
  - broken promises;
  - escalations with me;
  - unapplied awaiting my disposition;
  - DP returned by insurer;
  - files ready.

  There is also an aging-bracket bar chart per segment.
- **PR Worklist.**
  - Filters: segment, unit, UH, handler, AO, status, aging bracket, category, disposition, amount, promise status, escalation.
  - Group by client / ARN, with totals.
  - Bulk actions: disposition, effort, promise, remarks, escalate, reassign (BRCLXN.001-012, 050-052).
- **Collection account** (`/collections/items/:invoiceNo`):
  - header chips: payment, remittance, hold, DP, CWT, lock, owner (Marketing / Operations);
  - tabs:
    - Summary: net PR breakdown (046);
    - Installments & Promises (053/055);
    - Payments (054);
    - Timeline (057);
    - Policy & Co-insurance (056);
    - Dispositions & Efforts (016-023);
    - Escalations;
    - Billing Statements (058/060);
    - Marketing Diary (CQ20);
    - History (field changes, 043);
  - "Open Invoice 360" link;
  - soft lock banner "<user> is editing".
- **Client view**: all items of a client, with totals (003).
- **Unapplied Payments** (collector view): list (034-036), disposition drawer with a conditional invoice field (047/048), "Request Application", history (040).
- **Escalations**: inbox per level, with the workflow panel.
- **Assignments**: rules editor, reassign by criteria with a preview of the affected items, temporary assignments with an end date (052).
- **Billing Statements**: generate by cycle, list, download or send (058).
- **Files**: scheduled files per report and period, with "available from", download under `CLX_EXPORT` (024-029, 041, 045).
- **Collections Setup**: threshold, aging, invoice pattern, escalation rules (maker-checker), billing frequencies; links to the LOV maintenance of the Collections LOV types.
- **Sync log**: a link to *Operations -> Interfaces* (flow-in runs of the `COLLECTION_*` feeds) and to the job run history of `CLX_DAILY_REFRESH` (the "EBIX/QPS synchronization view" of p.45-46).

## 12. Build-wave plan

Prerequisites:
- `opsledger` is built;
- Cashiering (O1-A) and Commission (O1-D) are in progress.

C1-A and C1-B can start as soon as C0 is merged. C1-C needs O1-A to implement the two ports; it builds against the default adapters until then.

| Wave | Agent | Scope | Files owned | Exit criteria |
|---|---|---|---|---|
| **C0** (1 agent, short) | Collections foundation | Port additions in `opsledger` (section 9), `CollectionFeedReady` event, Collections permissions in `Permission.java`, V890 (grants, LOVs, parameters, workflow, alert codes, notification events, feed transport update), lockout parameter, report archive "generated" method, catalog unit head (with the PM owner), nav registration `features/collections/module.ts` with a stub home and `help.ts`, crons in `application.yml`, Developer Guide range rows (V890-V899, V1900-V1999) | `opsledger/service/port/**` (new files + `CollectionFeed.java`), `opsledger/service/adapter/OpsPortDefaults.java`, `OpsLedgerEvents.java`, `security/domain/Permission.java`, `security/**` (lockout), `report/core/ReportArchiveService.java`, `catalog/**` (sales unit head only), `V890`, `V819` (agreed), `navigation/modules.ts`, `help/helpContent.ts`, `application.yml`, `docs/development/DEVELOPER_GUIDE.md` | Compiles with default adapters; `mvn verify` green |
| **C1-A** | Collections core | Worklist + refresh job + balance listener, assignment and rules, PR dispositions, efforts, field-change log, soft lock, `InAppCollectionFeed` (outbox / inbox), scheduled files and the daily / weekly / monthly reports, home, worklist, account page (tabs Summary, Payments, Timeline, Policy, Dispositions, History), client view, files, setup, demo V1900 | `collections/{worklist,disposition,feed,files,audit,home}/**`, `V891`, `V895`, `db/demo/V1900`, `features/collections/**` except the C1-B / C1-C folders | Refresh from demo bookings; dispositions reach the Cashiering / Commission outboxes; files published with availability |
| **C1-B** | Collections plans and escalation | Installment plans (from policy-year invoices or generated), allocation, promises and `CLX_PROMISE_CHECK`, escalation rules / cases / workflow / job, manual and bulk escalation, bulk update handler, SOA (docgen template), demo V1901 | `collections/{installment,promise,escalation,billing,bulk}/**`, `V892`, `V893`, `db/demo/V1901`, `features/collections/{plans,escalations,billing}/**` | Broken promise -> escalation -> TL; SOA per cycle for a 3-year demo account |
| **C1-C** | Collections unapplied (after O1-A merges, or against the defaults) | Collector view, dispositions, application request, application text file, history; cashiering-side port implementation reviewed with O1-A | `collections/unapplied/**`, `V894`, `features/collections/unapplied/**` | Request -> Cashiering disposition -> applied -> history |
| **C1-D** | Commission extensions (in the commission agent's scope) | BRCLXN.059 gate (signed); after CQ01: 061-064 | `commission/**`, commission range | CR SOA blocked without confirmation |
| **C2** | Integration and hardening | E2E: booking -> item -> promise broken -> escalation -> payment applied -> item completed; 2307 disposition -> Cashiering tag; DP disposition -> Commission DP item -> returned -> Collections inbox; module guide `docs/modules/COLLECTIONS.md`; fit/gap refresh | tests + docs | Full `mvn verify` / `npm run verify` |

Rules for parallel work:
- One Flyway file set per agent: C0 V890; C1-A V891 / V895 / V1900; C1-B V892 / V893 / V1901; C1-C V894.
- Sub-packages are owned by one agent. `collections/common` (item query service, change recorder) belongs to C1-A. C1-B and C1-C use it read-only and ask C1-A for changes.
- Shared files (Permission enum, nav, help registry, `application.yml`, `ops_flow_in_feed` rows) are edited **only in C0**.
- Each agent writes its own `*ApiIT` smoke class.
- No agent edits `cashiering/**` or `commission/**` except their owners, who implement the ports and consumers listed in section 9.

## 13. Risks

1. **Cashiering / Commission timing.** C1-C and the feed consumers depend on O1-A / O1-D. Mitigation: ports with default hand-off adapters, and outbox items stay PENDING until a consumer takes them (alert `CLX_OUTBOX_STALE`).
2. **Installment source unknown** (CQ15). Mitigation: plans from policy-year invoices work today. Generated and manual plans are monitoring-only and do not change booking or the GL.
3. **Draft scope** (061-064). Mitigation: nothing is built before CQ01. The gate for 059 is signed and small.
4. **Export load** (caveat p.93). Mitigation: asynchronous exports, a row cap, a separate permission, and files generated off-peak.
5. **Legacy data** (EBIX history, invoice format). Mitigation: a migration handler and a pattern parameter accepting both formats (CQ07, CQ13).
6. **Flyway overlap** with the other pending BRD analyses in V890s. Mitigation: coordinate in the Developer Guide before C0, with the fallback of section 3.

## 14. C0 foundation: as built

What the C0 wave built (together with the BRD-5 foundation A0, because both edit the same shared files), and where it
differs from or details the sections above. C1-A, C1-B and C1-C build on this and compile only against it.

- **Migration.** `V1000__collections_foundation.sql` (the V890 of section 3, read through the allocation note):
  - roles `MKT_HANDLER`, `CLX_TL`, `MKT_SECTION_HEAD`, `UNAPPLIED_HANDLER`, `APP_SUPPORT`, `DCO` and the grants of
    section 6.2. Every new role also has WORK_VIEW, ATTACHMENT_VIEW, REPORT_VIEW, CLIENT_VIEW, ACCOUNT_VIEW and OPS_VIEW
    (the account page links to Invoice 360); MKT_AO receives OPS_VIEW for the same reason. The "`ACSL` (new, view)" role
    of 6.2 is not created: BRD-5 created `ACSL_PROCESSOR`, `ACSL_TL` and `ACSL_HEAD` (V890), and they receive CLX_VIEW
    and CLX_REPORT_VIEW. SYSADMIN and AUDITOR have read access; `sec_permission_action` classifies the `CLX_*`
    permissions in the area `COLLECTIONS`;
  - LOV types and the seeds of section 8, all marked "(to confirm)" (CQ08), plus `DP_RETURNED` ("DP returned by insurer",
    used by the inbox) and `NO_POLICY_NUMBER` (Processing Unit). Billing frequencies ANNUAL / SEMI_ANNUAL / QUARTERLY /
    MONTHLY; escalation reasons NO_COMMITMENT, BROKEN_PROMISE, AGING, INSTALLMENT_OVERDUE, CTE_REFUSED, OTHERS;
  - **LOV attributes.** `lov_value` has no attribute columns, so the attributes of sections 4.1 / 4.4 live in
    `clx_lov_attribute (type_code, code, attribute, value)`, keyed to `lov_value`. Attributes: `category`,
    `tagging_owner`, `ops_action`, `allowed_roles` (CLX_PR_DISPOSITION) and `requires_invoice`, `cashiering_action`
    (CLX_UPP_DISPOSITION; values = `UnappliedDispositionRequests.Action` or NONE). `category` (A / B / C) is not seeded
    until BDOI gives it (CQ08). C1-A maps the table (entity in `collections/common`) and maintains it on Collections
    Setup; C1-C reads the UPP rows;
  - workflow `CLX_ESCALATION` (section 7). `RAISED` has no owner; `route` / `route_to_head` are the system routing
    actions; `auto_close` exists from every open stage; `escalate_further` needs a `CLX_ESCALATION_REASON`,
    `return_to_handler` a `RETURN_REASON`; `acknowledge` and `return_to_handler` are generic (WorkflowPanel);
  - parameters of section 8 (`CLX_AGING_BRACKETS` is a CODE_LIST) and `LOGIN_MAX_FAILED_ATTEMPTS` = 3;
  - alert codes and notification events of section 8;
  - the five feeds of section 2.2 set to transport `IN_APP`. Their upload handlers stay (fallback), and
    `ManualCollectionFeed` stays the bean until C1-A declares `InAppCollectionFeed`;
  - `report_run.available_from` and the action `GENERATE` (scheduled files).
- **Security.** Permissions `CLX_*` in `Permission.java`. The lockout threshold is the parameter
  `LOGIN_MAX_FAILED_ATTEMPTS` (seeded 3, CQ23 decides whether it applies to all BIBS users), with
  `brokerverse.security.max-failed-attempts` (default 5) when the parameter is missing; `AppUser.MAX_FAILED_ATTEMPTS`
  is gone (`recordFailedLogin(int)`).
- **Report archive.** `ReportArchiveService.archiveGenerated(code, echo, rowCount, file, availableFrom)` archives a file
  produced by a job; `ReportService.generate(code, params, format, availableFrom)` runs, renders and archives a
  registered report in one call (no user permission check: the caller is a `ManagedJob`). The download of a generated
  file before `availableFrom` is refused with `REPORT_FILE_NOT_AVAILABLE`; the Report Archive screen shows "Available
  from". `ReportMetadata.collections(...)` gives the Collections category, view permission CLX_REPORT_VIEW and export
  permission CLX_EXPORT (archived).
- **Ports (section 9).** Built as specified: `CollectionFeed.acknowledge` (default no-op), `UnappliedDirectory` (default
  `EmptyUnappliedDirectory`), `UnappliedDispositionRequests` (default `HandoffDispositionRequests`, hand-off port
  `UnappliedDispositionRequests`, team `CASH_DISPOSITION`; `status(source, sourceRef)` added so C1-C can poll), and the
  events `CollectionFeedReady` and `UnappliedDispositionChanged` (published by cashiering when it decides a
  request: ACCEPTED, REJECTED or APPLIED). Cashiering is merged, so the cashiering adapters of the two ports are an open
  contract ask (C1-C with the cashiering owner, section 12).
- **Deferred to C1-A: the catalog unit head.** `cat_sales_unit.head_username` and
  `SalesOrganisationService.unitHead(companyId, unitCode)` (section 9, V819) are not built in C0 because the Product
  Maintenance wave P1-A is changing `catalog/**` at the same time. C1-A adds them after P1-A merges (agree the
  migration number with the PM owner: V818 / V819 are held by Product Maintenance), or falls back to `clx_unit_head`
  in V1001.
- **Navigation.** Group **Finance**, section **Collections**, first (before Cashiering), as section 11 decides; the
  home is `/collections` (`features/collections`, module id `collections`, help id `collections`, permission
  CLX_VIEW), a landing screen until C1-A adds the home tiles. Every other Collections route goes under `/collections`
  and into `features/collections/module.ts` and `help.ts` (owned by C1-A; C1-B / C1-C add their screens there).
- **Jobs.** The crons of section 8 are in `application.yml` (`brokerverse.jobs.clx-*-cron`) and
  `docs/operations/CONFIGURATION.md`; each job reads its cron with `@Value("${brokerverse.jobs.<property>:-}")`.
- **Flyway left to the build waves.** V1001-V1005 as in section 3 (C1-A V1001 / V1005 / demo V1900, C1-B V1002 / V1003
  / demo V1901, C1-C V1004); V1006-V1009 free.

### C1-A as built

Collections core (wave C1-A): worklist, assignment, dispositions, efforts, change log, soft lock, the in-app
`CollectionFeed`, files and reports, and the screens Home, PR Worklist, Collection account, Client view,
Assignments, Files and Setup. It builds on section 14 and differs from sections 3-11 where noted.

- **Flyway.** `V1001__collections_worklist.sql` (`clx_item`, `clx_item_balance`, `clx_assignment`,
  `clx_assignment_rule`, `clx_disposition`, `clx_effort`, `clx_field_change`, `clx_outbox`, `clx_inbox`),
  `V1005__collections_files.sql` (`clx_scheduled_file`; the SOA template is left to C1-B's billing),
  `V819__catalog_sales_unit_head.sql` (the catalog Unit Head, V819 being free after Product Maintenance merged)
  and `db/demo/V1900__demo_collections.sql` (users `clxhandler` MKT_COLLECTION, `clxtl` CLX_TL, `clxuh`
  MKT_SECTION_HEAD, `mkthandler` MKT_HANDLER; Unit Heads; three assignment rules). The demo items, dispositions,
  reassignment and files are made at start-up by `collections.demo.CollectionsDemoData` (order 97) as those users,
  because the demo invoices reach the ledger only when the booking and Operations runners have run.
- **Packages.** `collections.common` (item entity and repository, LOV attributes `LovAttributes`, change recorder
  `ChangeRecorder`, parameters `ClxSettings`, the read / edit contract `CollectionItems` and the home port
  `CollectionsWorkCountSource`), `worklist` (refresh, job, balance listener, assignment, rules, edit lock, account
  views), `disposition` (PR dispositions `PrDispositionService`, efforts, timeline), `feed` (outbox, inbox,
  `InAppCollectionFeed`, stale check), `files` (publication, jobs), `report`, `home`, `setup`, `demo`. The PR
  disposition entity is `PrDisposition` (`clx_disposition`) because Cashiering already has a `Disposition` bean.
- **Refresh rules** (`ItemSnapshots`): the total to collect is the six PR components plus PR2307. Above
  `CLX_MIN_BALANCE_THRESHOLD` the item is OPEN; at or below it COMPLETED (a new invoice is not listed); an invoice
  without a client receivable (payment status NOT_APPLICABLE: direct payment, return invoices) is never listed; a
  negative total is EXCLUDED_CANCELLED for cancellations / return kinds and CREDIT otherwise (CQ04). The balance
  listener refreshes listed items after each committed movement or flag change; new invoices wait for
  `CLX_DAILY_REFRESH` (or "Refresh from Ledger" on the account, `POST /items/{no}/refresh`). The Unit Head is the head
  of the invoice's sales unit, else of its department or region (`SalesOrganisationService.unitHead`, CQ05).
- **Assignment.** New open items without a handler go to the first matching active rule (`clx_assignment_rule`:
  segment, unit, client, amount and aging ranges, priority), else to their AO when the AO holds CLX_WORK.
  Reassignment (selection or criteria with a preview) is PERMANENT or TEMPORARY with an end date; the daily refresh
  returns an ended temporary assignment to the previous handler unless a later assignment replaced it.
  `CLX_REASSIGNED` notifies the new and previous handlers. Bulk references `CLXRA-<yyyy>-nnnnnn`.
- **Dispositions.** Only active `CLX_PR_DISPOSITION` values; `allowed_roles` restricts a value to roles; several
  accounts need CLX_BULK_UPDATE and share a bulk reference `CLXBU-<yyyy>-nnnnnn`. Hand-offs (fields in the layout the
  consumer already reads):
  - CHECK_PICKUP -> `COLLECTION_CHECK_PICKUP` (`PickupService.details` fields), key `PU:<invoice>:<id>`; needs the
    pick-up date (today or later), address and amount;
  - CWT2307_REVERSAL -> `COLLECTION_CWT2307` (`CWT_TAGS` columns), key `CWT:<invoice>:<id>`, queued when the owner is
    OPERATIONS or the path is CASH; a CERTIFICATE path needs the certificate number;
  - DP_REVERSAL -> `COLLECTION_DP_LIST` (DP list columns, branch code of the invoicing branch), key `DP:<invoice>:<id>`;
  - CANCEL_REQUEST: no feed (the cancellation is raised in Adjustment).
  A later disposition supersedes the current one and withdraws its PENDING hand-off (CANCELLED).
- **CollectionFeed (IN_APP).** `pending` delivers the PENDING outbox rows once and marks them TAKEN (the in-app
  consumers take every item of the call and log it in their own flow-in run, and do not acknowledge yet);
  `acknowledge` also marks rows TAKEN; `send` runs
  one flow-in run (trigger EVENT) whose records become `clx_inbox` rows. `COLLECTION_DP_RETURNED` reopens the item,
  records the disposition `DP_RETURNED` with the reason and time, and notifies the handler (`CLX_DP_RETURNED`);
  `COLLECTION_REFUND` rows are kept for C1-C. Queuing publishes `CollectionFeedReady` after commit.
  `CLX_OUTBOX_STALE` (an `AlertCheck`) flags items pending more than a day.
- **Edit lock.** Opening an account as a CLX_WORK user takes the lock for `CLX_EDIT_LOCK_MINUTES`; changes by
  others are refused with `CLX_ITEM_LOCKED` "<user> is editing"; system updates (refresh, inbox) ignore it.
- **Change log.** Every field change of items, rules, parameters, LOV attributes and Unit Heads is a
  `clx_field_change` row with from / to, user, time, source IP and bulk reference; the account's History tab and
  the `CLX-AUDIT-LOG` report (view and export CLX_AUDIT_VIEW) read it.
- **Files.** `CLX_DAILY_FILES` publishes the Outstanding PR List and the Full Production Report (month to date),
  available at once; `CLX_WEEKLY_FILES` the DP PR and PR 2307 for reversal of the Saturday-Friday week, one file per
  sales unit and invoicing branch with tagged accounts, available the next Monday 08:00 PHT; `CLX_MONTHLY_FILES`
  (run daily at 05:00 PHT) the two files of the previous month on the first working day of the head office
  calendar, available from 08:00. Each file is an XLSX GENERATE run of the report archive recorded once per report,
  period and scope; users with CLX_EXPORT are notified (`CLX_FILE_READY`); a failure is recorded and raises
  `CLX_FILE_NOT_PUBLISHED`. Exports of the Outstanding PR List run on request, refused above `CLX_EXPORT_MAX_ROWS`.
- **Reports** (`ReportMetadata.collections`): `CLX-OUTSTANDING-PR`, `CLX-FULL-PRODUCTION`, `CLX-DP-FOR-REVERSAL`,
  `CLX-PR2307-FOR-REVERSAL`, `CLX-COMPLETED-COLLECTIONS`, `CLX-INVOICES-WITH-DISPOSITION`, `CLX-REASSIGNMENTS`,
  `CLX-AUDIT-LOG`. The p.59-61 fields BrokerVerse holds are filled; legacy-only fields wait for CQ22.
- **API** (`/api/v1/collections`): `home`; `worklist` (+ `/totals`); `items/{no}` with `payments`, `policy`,
  `assignments`, `history`, `dispositions`, `efforts`, `handoffs`, `timeline`, `lock` (POST / DELETE), `refresh`,
  `details` (PUT); `dispositions`, `efforts`, `disposition-rules`; `clients/{code}`; `handlers`; `assignment-rules`
  (+ `/{id}`, `/{id}/active`); `reassignments` (+ `/preview`); `files`, `files/generate`, `exports`; `refresh`;
  `setup`, `setup/parameters/{key}`, `setup/lov-attributes`, `setup/unit-heads/{unit}`.
- **Contracts for C1-B / C1-C.** `CollectionItems` (`find`, `require`, `of`, `ofClient`, `requireEditable`,
  `requireForUpdate`), the item flags `CollectionItem.flagPromise / flagEscalation / flagInstallmentOverdue`,
  `ChangeRecorder.record`, `LovAttributes` (`of`, `ofType`, `prRule`), `ClxSettings`, `WorklistFilter` (with its
  promise / escalation work filters) and the port `CollectionsWorkCountSource` for the home tiles. Screens are added
  to `features/collections/module.ts` after the PR Worklist and before Assignments.
- **Parked / open.** Cashiering has no pull of `COLLECTION_CWT2307` (its upload stays the path): those outbox items
  stay PENDING and `CLX_OUTBOX_STALE` reports them (contract ask to the cashiering owner, with `acknowledge` after
  each accepted record for all consumers). Delivery date on the policy tab (CQ17), CTE and the
  Marketing Diary (CQ20), the legacy EBIX migration handler (CQ07), the real disposition, effort and category values
  (CQ08), the threshold value (CQ03) and the data scope per user (CQ06: every CLX_VIEW user sees every account,
  with filters) stay as designed seams.

### C1-B as built

Wave C1-B (Collections plans and escalation) built BRCLXN.049-051, 053-055 and 058 / 060 in
`collections/{installment,promise,escalation,billing,bulk}` and `collections.demo`, without reading the worklist of wave
C1-A: an invoice is a collection account when it is a client receivable of the ledger (not direct payment, not
cancelled) with a premium balance above `CLX_MIN_BALANCE_THRESHOLD`. The worklist item (`clx_item`) is referenced by its
plain keys (invoice no., ARN, client code); balances are read from `opsledger` (`InvoiceLedgerQueryService`, and one
read-only SQL over `ops_invoice` / `ops_invoice_component` in `escalation.service.EscalationCandidates`).

| BR ID | As built |
|---|---|
| BRCLXN.053 | `clx_installment_plan` / `clx_installment` (V1002). A plan is `POLICY_YEARS` (every BOOKING policy-year invoice of an ARN, booked in the ledger or still `SCHEDULED` in booking, split in the cycles of the frequency within its coverage year; the gross premium of a booked year, the premium total of a scheduled one), `GENERATED` (the outstanding of one invoice in N equal installments from a first due date) or `MANUAL` (entered installments that must add up to the outstanding). One live plan per invoice and per policy-year account (partial unique indexes). Installment status NOT_DUE / DUE / OVERDUE / PARTIAL / PAID, `overdue_since`; the plan completes when every installment is paid |
| BRCLXN.054 | `PlanAllocation`: per invoice of the plan, the settled amount (installments total less the ledger outstanding, so payments, reversals, 2307 reclass, DP reversal and write-offs all count) is allocated oldest due first; policy years booked since the plan was made are linked to their invoice first. Run by `CLX_PROMISE_CHECK` for every live plan, on "Refresh Allocation" and before an SOA |
| BRCLXN.055 | `clx_promise` (V1002): invoice, optional installment, `promised_on` (day of the promise, not in the future), `promised_date`, amount (default the whole outstanding, never above it). A new promise on the same invoice evaluates the expired one or supersedes the running one. `CLX_PROMISE_CHECK` evaluates promises whose date plus `CLX_PROMISE_GRACE_DAYS` is before the business date: KEPT when the payments applied (APPLIED less UNAPPLIED on the PR components, value date from `promised_on` to the deadline) reach the amount or nothing is left to collect, PARTIALLY_KEPT when some was paid, else BROKEN (rule to confirm, CQ16). A broken promise notifies the recorder and the AO (`CLX_PROMISE_BROKEN`) and publishes `PromiseBroken` |
| BRCLXN.049 | `clx_escalation_rule` (V1003, maker-checker: `CLX_SETUP` maintains, `MASTER_AUTHORIZE` authorizes, inbox source `EscalationApprovalSource`), bases AGING_FROM_BOOKING / AGING_FROM_INCEPTION / NO_COMMITMENT_BY_DAY (no OPEN promise) / BROKEN_PROMISES_COUNT / INSTALLMENT_OVERDUE_DAYS / AMOUNT_OVER, filters segment / sales unit / product line / outstanding range, target TL / UH / SECTION_HEAD / USER, reason, SLA hours, notify, effective dates. Job `CLX_ESCALATION` (`EscalationJob`, cron `clx-escalation-cron`) raises one case per rule, invoice and month (`dedup_key`, and none while one of the rule is open), then closes open cases whose invoices are collected (`auto_close`). `EscalationEngine` also listens to `PromiseBroken` and applies the BROKEN_PROMISES_COUNT rules at once: **a broken promise escalates to the team lead** (demo rule `CLX-BROKEN-PROMISE` → `mkttl`). `EscalationOverdueCheck` (alert job) raises `CLX_ESCALATION_OVERDUE` past the SLA of the case |
| BRCLXN.050 | `clx_escalation` / `clx_escalation_item` (V1003), workflow `CLX_ESCALATION` (V1000): a case starts in RAISED and is routed at once (`route` to WITH_TL for TL / USER, `route_to_head` to WITH_UH for UH / SECTION_HEAD; system action for rules, user action for manual escalations), assigned to the designated user when there is one, else it waits in the stage queue and every `CLX_ESCALATION_HANDLE` holder is notified (`CLX_ESCALATED`, with the AOs of the invoices). Manual escalation groups the selected invoices by ARN, one case per account (`POST /api/v1/collections/bulk/escalate`, `CLX_ESCALATE`). Business actions `escalate_further` (reason `CLX_ESCALATION_REASON`; the case moves to the unit / section head queue), `resolve` (resolution required), `resubmit`; `acknowledge` and `return_to_handler` run from the workflow panel. The stage is mirrored by an `@EventListener` on `WorkCaseTransitioned` |
| BRCLXN.051 | Bulk update handler `CLX_BULK_UPDATE` (`CollectionsBulkUpdateHandler`, permission `CLX_BULK_UPDATE`): per row a promise, an escalation (Escalate = Y, level, user, reason) and, through the port `WorklistUpdates`, the disposition, effort and remarks; each row validated and committed on its own, the upload number is the `bulk_ref` of every record. In-grid bulk actions `POST /api/v1/collections/bulk/{escalate,promises}` return one outcome per invoice (`ItemResult`) |
| BRCLXN.058 / 060 | `clx_billing_statement` / `_line` / `clx_billing_document` (V1002), `SOA-<yyyy>` numbers, one live SOA per plan and cycle: the cycle's installment (CURRENT) and every earlier unpaid one (ARREARS) with the allocation of the day; PDF from the docgen template `CLX_SOA` (seeded in V1002, version recorded); billing run for the cycles due in a period; e-mail through the messaging outbox with a password-protected PDF (`SoaDispatch`); cancel to bill the cycle again. An SOA creates no receivable and no CR billing |

- **Reports** (`escalation.report.PlanAndEscalationReports`, `ReportMetadata.collections`): `CLX-ESCALATIONS`,
  `CLX-BROKEN-PROMISES`, `CLX-INSTALLMENTS-DUE` (draft layouts, CQ22).
- **API** (`/api/v1/collections`): `plans` (list, detail, `by-account`, `installments/due`, `policy-years`, `generated`,
  `manual`, `{id}/refresh`, `{id}/cancel`), `promises` (list, `by-invoice/{no}`, record, `{id}/cancel`), `escalations`
  (list, detail, `by-invoice/{no}`, `{id}/actions/{action}`), `escalation-rules` (list, create, change, authorize,
  deactivate, `{id}/matches`), `billing/statements` (list, detail, `by-plan/{id}`, generate, `generate-due`,
  `{id}/document`, `{id}/recipient`, `{id}/send`, `{id}/cancel`), `bulk/escalate`, `bulk/promises`.
- **Screens** (`features/collections/{plans,escalations,billing}`, exported as `PLAN_SCREENS`, `ESCALATION_SCREENS`,
  `BILLING_SCREENS` with help entries `PLAN_HELP`, `ESCALATION_HELP`, `BILLING_HELP`, to be registered in
  `features/collections/module.ts` / `help.ts` by the C1-A owner): Installment Plans (+ record with Installments,
  Statements of Account and Promises tabs), Installments Due, Promises to Pay, Escalations (+ record with the workflow
  panel), Escalation Rules, Billing Statements (+ statement record). StatusBadge tones added for WITH_TL, WITH_UH,
  RETURNED, DUE, PARTIAL, PARTIALLY_KEPT (review), IN_ACTION, NOT_DUE (in process), RESOLVED, KEPT (done), OVERDUE,
  BROKEN (exception).
- **Demo.** `db/demo/V1901` seeds the process rules (broken promise → `mkttl`, 45 days from booking → TL, 60th day
  from inception → UH, installment overdue 15 days → TL) and one rule pending authorization (maker `badmin`). The
  storyline needs booked invoices, so it runs after the Operations demo as `collections.demo.CollectionsPlansDemoData`
  (order 110): a three-year PAR01 account for CL-2026-000005 issued (`ao`, `proc`) and booked (`proc`), its annual
  policy-year plan with **an SOA for each of its three billing cycles** (the first e-mailed), a quarterly plan for
  ARN-2026-940002, a kept promise on ARN-2026-940004, a running one on the quarterly plan and a broken one on the
  three-year account that escalated it to `mkttl`, who acknowledged it; the job escalated the overdue installment and
  `mktcoll` escalated ARN-2026-940004 by hand.
- **Contracts for the other waves.** Event `promise.service.PromiseBroken(companyId, promiseId, invoiceNo)`; port
  `bulk.service.WorklistUpdates` (validate / apply the disposition, effort and remarks of an invoice with a bulk
  reference; default `PendingWorklistUpdates` refuses them) for C1-A to implement with `clx_disposition`,
  `clx_effort` and `clx_field_change`; read services `PromiseService.forInvoice`, `EscalationService.forInvoice`,
  `InstallmentPlanService.forAccount`, `BillingStatementService.forPlan` for the account page tabs (Installments &
  Promises, Escalations, Billing Statements); `LedgerBalances` (outstanding, threshold, payments in a window).
- **Parked (seam only).** Resolving the team lead / unit head of an account from the sales organisation (CQ14: without
  a designated user a case waits in the stage queue); the promise rule, grace days and partial payments (CQ16);
  installment terms from the quotation or the account (CQ15: plans are built in Collections); SOA layout, recipient,
  numbering and e-mail (CQ18: template `CLX_SOA` and a reviewed e-mail); the account-page tabs, home tiles and the
  installment refresh inside `CLX_DAILY_REFRESH` belong to C1-A (the promise check allocates the plans meanwhile).
- **Flyway.** V1002 (plans, installments, promises, statements, SOA template), V1003 (rules, escalations), demo V1901.
  `CLX_SOA` is seeded here, not in V1005.

### C1-C as built

Wave C1-C built the collector side of unapplied payments (BRCLXN.030-042, 047/048) in `collections.unapplied` and,
because Cashiering is merged and owns the unapplied items, the Cashiering adapters of the four Operations ports it
needs, additively in `cashiering`.

| BR ID | As built |
|---|---|
| BRCLXN.034-036 | `UnappliedWorklistService` reads Cashiering's open items through `UnappliedDirectory` (never copied): payment date and age, payment file (the upload batch reference), transaction no., amount, balance, payment type, payor, bank, check no., payor reference, matched client / invoice, sales unit, the Cashiering tab (UNAPPLIED, MONITORING, FOR_APPROVAL, FOR_REVERSAL, DONE; the "processing stage", CQ11) and the status of the Cashiering disposition or collector request. The account fields of the matched invoice (assured, PR balance, inception, segment, sales unit, unit head, AO, insurer, invoice category, handler) come from the collection item, else the invoice ledger. Filters: text, client, unit, tab, payment dates and age run in Cashiering; market segment and collector disposition (`NONE` = none yet) run in Collections over at most 2,000 items |
| BRCLXN.031/033, 037-040 | `clx_unapplied_disposition` (V1004), append-only: only active `CLX_UPP_DISPOSITION` values; the attribute `cashiering_action` (APPLY_TO_INVOICE / REFUND / RECLASS / TRANSFER / NONE) decides whether a request is sent. The history (`GET /unapplied/{ref}/history`) merges Cashiering's events (`UnappliedDirectory.history`: intake, collector requests and decisions, dispositions, applied / refunded / reclassified / transferred / released, withdrawn, reversed, closed) with the collector dispositions; it stays after the payment is applied or refunded. Each disposition is also a `clx_field_change` row |
| BRCLXN.047/048 | `requires_invoice` makes the invoice mandatory; it must match `CLX_INVOICE_NO_PATTERN` (EBIX `I########` or BrokerVerse `BI-...`, CQ13) and exist in the invoice ledger of the company (`CLX_INVOICE_REQUIRED`, `CLX_INVOICE_FORMAT`, `CLX_INVOICE_UNKNOWN`); the form checks the same pattern before sending |
| BRCLXN.030/032 | `clx_application_request` (V1004): the request sent through `UnappliedDispositionRequests` with the key `CLX-UPP-<disposition id>` and the p.60 payment fields as a snapshot. Status SENT / DEFERRED (hand-off) / ACCEPTED / REJECTED / APPLIED, updated by `UnappliedDispositionChanged` (the requester is notified) or by "Check Status" (`POST /unapplied/requests/{id}/refresh`, a poll of the port). Role `UNAPPLIED_HANDLER` (V1000, CQ10) holds `CLX_UNAPPLIED_WORK`; demo user `upphandler` |
| BRCLXN.041/042 | Job `CLX_APPLICATION_FILE` (`ApplicationFileJob`, cron `clx-application-file-cron`, 05:00 PHT) writes, per company, the pipe-delimited text file `FOR_APPLICATION_TO_INVOICE_<yyyyMMdd>_<time>.txt` of the application requests made up to the end of the previous day and not yet listed (payment date, payment file, transaction no., paid amount, currency, payment type, payor, reference no., assured, invoice no., user ID, unapplied reference, request key) through `FileDropPort` into folder `FS04/CLX_APPLICATION_TO_INVOICE` (in-system extract repository until OQ17); each request records the file name. Manual run `POST /unapplied/application-file` (CLX_SETUP or CLX_EXPORT). Report `CLX-APPLICATION-TO-INVOICE` lists the same requests for a period |

- **Cashiering adapters** (V1006 `csh_collector_request`, `csh_refund_validation`, `csh_payment_reversal`; no existing
  cashiering table changed):
  - `CashieringUnappliedDirectory` implements `UnappliedDirectory` (open items with a balance, by SQL over
    `csh_unapplied`, `csh_payment`, `csh_receipt`); `UnappliedHistory` builds the history.
  - `CollectorRequestService` implements `UnappliedDispositionRequests`: a request is refused at once (REJECTED) when
    the item is unknown, has no balance, the amount is above the balance or an application has no invoice; otherwise it
    is queued (`CRQ-<yyyy>`, SUBMITTED) for `CASH_DISPOSITION`, idempotent on (source, source reference). On the
    Cashiering screen **Incoming Requests** (`/cashiering/requests`) a cashier accepts it - a disposition of
    `OPS_DISPOSITION` is assigned with the default type of the action (APPLY_OTHER_INVOICE, REFUND, RECLASS,
    TRANSFER_UNIT; the type must carry out the action) and the fields the collector cannot give (reclass client,
    transfer unit, payee), optionally submitted at once - or rejects it with a reason. `CollectorRequestTracker`
    publishes `UnappliedDispositionChanged` ACCEPTED on acceptance, APPLIED when the disposition is executed
    (application, refund through `DisbursementGateway`, reclass, transfer), REJECTED on rejection or when the cashier
    withdraws the disposition; the approval rules of the disposition types stay.
  - `CashieringRefundValidationSource` implements `RefundValidationSource` for validator CASHIERING (MKT 1.11.0): a
    task `RVL-<yyyy>` for `CASH_DISPOSITION`; the cashier confirms it with the unapplied item that holds the returned
    premium (its AR is the new AR number unless another is given) or rejects it; the answer is
    `RefundValidationCompleted`, so payrequest's validation no longer waits for a manual entry.
  - `CashieringPaymentReversals` implements `PaymentReversalRequester` (ACSL 2.6.0-2.6.1): the request `PRV-<yyyy>` is
    SUBMITTED for `CASH_APPROVE`; the approver (not the requester) reverses the receipt's active applications on the
    invoice newest first with the application engine (negative `OPS_PAYMENT_APPLY` and an UNAPPLIED ledger movement),
    applies back what exceeded the requested amount, and puts the money reversed in a new unapplied item; the decision
    is `PaymentReversalCompleted`.
- **Contract changes forced by the real beans.** `CollectionsAndDisbursementPortsIT` now tests the default adapters
  directly; `RefundValidationIT` confirms the CASHIERING validation through the adapter; `AcslCorrectionIT` expects a
  SUBMITTED reversal with a `PRV-` reference. The hand-off defaults stay for a deployment without cashiering.
- **API** (`/api/v1/collections/unapplied`): list (`q`, `clientCode`, `salesUnit`, `tab`, `paidFrom`, `paidTo`,
  `ageMin`, `ageMax`, `segment`, `disposition`), `disposition-rules`, `requests` (+ `/{id}/refresh`),
  `application-file`, `{ref}`, `{ref}/history`, `{ref}/dispositions` (POST). Cashiering (`/api/v1/cashiering`):
  `requests/counts`, `collector-requests` (+ `/{id}/accept`, `/{id}/reject`), `unapplied/{id}/collector-requests`,
  `refund-validations` (+ `/{id}/candidates`, `/{id}/confirm`, `/{id}/reject`), `payment-reversals`
  (+ `/{id}/approve`, `/{id}/reject`).
- **Screens.** `features/collections/unapplied` exports `UNAPPLIED_SCREENS` and `UNAPPLIED_HELP` (Unapplied Payments,
  Requests to Cashiering, and the hidden record `/collections/unapplied/:ref` with the tabs Payment & Account,
  Collector Dispositions, Requests to Cashiering, History) for the Collections owner to register; Cashiering gains
  **Incoming Requests** (tabs Collector Requests, Refund Validations, Payment Reversals, each To Do / Decided).
  StatusBadge tones added: APPLIED (done), UNAPPLIED and FOR_REVERSAL (review), DEFERRED and MONITORING (in process).
- **Home and reports.** `UnappliedWorkCounts` adds the tiles "Unapplied Awaiting Disposition" and "My Requests in
  Cashiering" for `CLX_UNAPPLIED_WORK` users. Reports `CLX-APPLICATION-TO-INVOICE` and `CLX-UNAPPLIED-DISPOSITIONS`
  (`ReportMetadata.collections`).
- **Demo** (Java runners, no new demo migration): `cashiering.demo.UnappliedDemoPayments` (order 125) receives four
  unmatched payments as `cashier`; `collections.demo.UnappliedDemoData` (126) creates `upphandler` (role
  UNAPPLIED_HANDLER) and, as the collectors, asks to apply Grace Villanueva's payment to an open invoice outside the
  installment plans, notes "Coordinate further" on Juan Dela Cruz's, asks for the refund of Mega Traders Inc.'s, and
  writes the day's application file; `cashiering.demo.CollectorRequestDemoData` (127) accepts and processes the
  application as `cashier`. The refund request stays queued; Liza Manalo's payment has no disposition.
- **Flyway.** V1004 (Collections), V1006 (Cashiering adapters; V1007-V1009 free).
- **Parked (seam only).** FS04 transport (OQ17: in-system extract repository) and the file layout (CQ12); the
  "Processing Stage", "Business Origin", "Client Code Match" and "System Generated Remarks" definitions (CQ11: the
  Cashiering tab and status are shown); the booker name and names in place of usernames for UH / AO (BRCLXN.036);
  who the Unapplied Payment Handler is (CQ10: role and demo user only); the real disposition values (CQ08); a REFUND
  disposition still goes to Disbursement directly, not through a payrequest RRF (OQ15/OQ16).
