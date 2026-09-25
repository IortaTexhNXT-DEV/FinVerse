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
